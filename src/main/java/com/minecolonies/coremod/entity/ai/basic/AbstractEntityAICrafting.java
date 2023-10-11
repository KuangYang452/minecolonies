package com.minecolonies.coremod.entity.ai.basic;

import com.google.common.collect.ImmutableList;
import com.minecolonies.api.colony.buildings.modules.ICraftingBuildingModule;
import com.minecolonies.api.colony.requestsystem.request.IRequest;
import com.minecolonies.api.colony.requestsystem.request.RequestState;
import com.minecolonies.api.colony.requestsystem.requestable.Stack;
import com.minecolonies.api.colony.requestsystem.requestable.crafting.PublicCrafting;
import com.minecolonies.api.crafting.IRecipeStorage;
import com.minecolonies.api.crafting.ItemStorage;
import com.minecolonies.api.crafting.RecipeStorage;
import com.minecolonies.api.entity.ai.statemachine.AITarget;
import com.minecolonies.api.entity.ai.statemachine.states.IAIState;
import com.minecolonies.api.entity.citizen.VisibleCitizenStatus;
import com.minecolonies.api.entity.pathfinding.AbstractAdvancedPathNavigate;
import com.minecolonies.api.util.*;
import com.minecolonies.coremod.Network;
import com.minecolonies.coremod.colony.buildings.AbstractBuilding;
import com.minecolonies.coremod.colony.buildings.modules.CraftingWorkerBuildingModule;
import com.minecolonies.coremod.colony.jobs.AbstractJobCrafter;
import com.minecolonies.coremod.entity.citizen.EntityCitizen;
import com.minecolonies.coremod.network.messages.client.BlockParticleEffectMessage;
import com.minecolonies.coremod.network.messages.client.LocalizedParticleEffectMessage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Predicate;

import static com.minecolonies.api.entity.ai.statemachine.states.AIWorkerState.*;
import static com.minecolonies.api.util.constant.CitizenConstants.*;
import static com.minecolonies.api.util.constant.Constants.DEFAULT_SPEED;
import static net.minecraft.world.entity.animal.Sheep.ITEM_BY_DYE;

/**
 * 用于主要的制作人工智能的抽象类。
 */
public abstract class AbstractEntityAICrafting<J extends AbstractJobCrafter<?, J>, B extends AbstractBuilding> extends AbstractEntityAIInteract<J, B>
{
    /**
     * 工人延迟下一次打击的时间。
     */
    protected static final int HIT_DELAY = 10;

    /**
     * 增加此值以使产品制作进度慢得多。
     */
    public static final int PROGRESS_MULTIPLIER = 10;

    /**
     * 最大级别，应该影响工人速度。
     */
    protected static final int MAX_LEVEL = 50;

    /**
     * 需要击打产品的次数。
     */
    private static final int HITTING_TIME = 3;

    /**
     * 当前正在制作的请求；
     */
    public IRequest<? extends PublicCrafting> currentRequest;

    /**
     * 当前正在制作的配方。
     */
    protected IRecipeStorage currentRecipeStorage;

    /**
     * 玩家伤害来源。
     */
    private DamageSource playerDamageSource;

    /**
     * 制作成功的每个任务值多少动作。默认情况下，每个制作成功值为1个动作。在子类中覆盖此值以使制作配方值更高。
     *
     * @return 制作成功的每个任务值多少动作。
     */
    protected int getActionRewardForCraftingSuccess()
    {
        return 1;
    }

    /**
     * 初始化制作工作并添加所有任务。
     *
     * @param job 他所拥有的工作。
     */
    public AbstractEntityAICrafting(@NotNull final J job)
    {
        super(job);
        super.registerTargets(
          /*
           * 检查是否应执行任务。
           */
          new AITarget(IDLE, () -> START_WORKING, 1),
          new AITarget(START_WORKING, this::decide, STANDARD_DELAY),
          new AITarget(QUERY_ITEMS, this::queryItems, STANDARD_DELAY),
          new AITarget(GET_RECIPE, this::getRecipe, STANDARD_DELAY),
          new AITarget(CRAFT, this::craft, HIT_DELAY)
        );
        worker.setCanPickUpLoot(true);
    }

    @Override
    protected void updateRenderMetaData()
    {
        worker.setRenderMetadata(getState() == CRAFT ? RENDER_META_WORKING : "");
    }

    /**
     * 决定要做什么的主要方法。
     *
     * @return 要转到的下一个状态。
     */
    protected IAIState decide()
    {
        worker.getCitizenData().setVisibleStatus(VisibleCitizenStatus.WORKING);
        if (job.getTaskQueue().isEmpty())
        {
            if (worker.getNavigation().isDone())
            {
                if (building.isInBuilding(worker.blockPosition()))
                {
                    worker.getNavigation().moveToRandomPos(10, DEFAULT_SPEED, building.getCorners(), AbstractAdvancedPathNavigate.RestrictionType.XYZ);
                }
                else
                {
                    walkToBuilding();
                }
            }
            return IDLE;
        }

        if (job.getCurrentTask() == null)
        {
            return IDLE;
        }

        if (walkToBuilding())
        {
            return START_WORKING;
        }

        if (job.getActionsDone() >= getActionsDoneUntilDumping())
        {
            // 等待转储后继续。
            return getState();
        }

        return getNextCraftingState();
    }

    /**
     * 获取下一个需要制作的状态，如果存在任务。
     *
     * @return 下一个状态
     */
    protected IAIState getNextCraftingState()
    {
        if (job.getCurrentTask() == null)
        {
            return getState();
        }

        if (currentRequest != null && currentRecipeStorage != null)
        {
            return QUERY_ITEMS;
        }

        return GET_RECIPE;
    }

    /**
     * 查询队列中第一个请求的IRecipeStorage。
     *
     * @return 要转到的下一个状态。
     */
    protected IAIState getRecipe()
    {
        final IRequest<? extends PublicCrafting> currentTask = job.getCurrentTask();

        if (currentTask == null)
        {
            return START_WORKING;
        }

        final ICraftingBuildingModule module = building.getCraftingModuleForRecipe(currentTask.getRequest().getRecipeID());
        if (module == null)
        {
            job.finishRequest(false);
            incrementActionsDone(getActionRewardForCraftingSuccess());
            return START_WORKING;
        }
        currentRecipeStorage = module.getFirstFulfillableRecipe(stack -> ItemStackUtils.compareItemStacksIgnoreStackSize(stack, currentTask.getRequest().getStack()), 1, false);
        if (currentRecipeStorage == null)
        {
            job.finishRequest(false);
            incrementActionsDone(getActionRewardForCraftingSuccess());
            return START_WORKING;
        }

        currentRequest = currentTask;
        job.setMaxCraftingCount(currentRequest.getRequest().getCount());
        final int currentCount = InventoryUtils.getItemCountInItemHandler(worker.getInventoryCitizen(), stack -> ItemStackUtils.compareItemStacksIgnoreStackSize(stack, currentRecipeStorage.getPrimaryOutput()));
        final int inProgressCount = getExtendedCount(currentRecipeStorage.getPrimaryOutput());

        final int countPerIteration = currentRecipeStorage.getPrimaryOutput().getCount();
        final int doneOpsCount = currentCount / countPerIteration;
        final int progressOpsCount = inProgressCount / countPerIteration;

        final int remainingOpsCount = currentRequest.getRequest().getCount() - doneOpsCount - progressOpsCount;

        final List<ItemStorage> input = currentRecipeStorage.getCleanedInput();
        for (final ItemStorage inputStorage : input)
        {
            final ItemStack container = inputStorage.getItem().getCraftingRemainingItem(inputStorage.getItemStack());
            final int remaining;
            if(!currentRecipeStorage.getCraftingToolsAndSecondaryOutputs().isEmpty() && ItemStackUtils.compareItemStackListIgnoreStackSize(currentRecipeStorage.getCraftingToolsAndSecondaryOutputs(), inputStorage.getItemStack(), false, true))
            {
                remaining = inputStorage.getAmount();
            }
            else if (!ItemStackUtils.isEmpty(container) && ItemStackUtils.compareItemStacksIgnoreStackSize(inputStorage.getItemStack(), container , false, true))
            {
                remaining = inputStorage.getAmount();
            }
            else
            {
                remaining = inputStorage.getAmount() * remainingOpsCount;
            }
            if (InventoryUtils.getCountFromBuilding(building, itemStack -> ItemStackUtils.compareItemStacksIgnoreStackSize(itemStack, inputStorage.getItemStack(), false, true))
                    + InventoryUtils.getItemCountInItemHandler(worker.getInventoryCitizen(), itemStack -> ItemStackUtils.compareItemStacksIgnoreStackSize(itemStack, inputStorage.getItemStack(), false, true))
                    + getExtendedCount(inputStorage.getItemStack())
                    < remaining)
            {
                job.finishRequest(false);
                incrementActionsDone(getActionRewardForCraftingSuccess());
                return START_WORKING;
            }
        }

        job.setCraftCounter(doneOpsCount);
        return QUERY_ITEMS;
    }

    /**
     * 获取可以重写的扩展计数。
     *
     * @param stack 要添加的堆栈。
     * @return 附加的数量（例如在熔炉中）。
     */
    protected int getExtendedCount(final ItemStack stack)
    {
        return 0;
    }

    @Override
    public IAIState getStateAfterPickUp()
    {
        return GET_RECIPE;
    }

    /**
     * 查询要放入库存以制作的所需物品。
     *
     * @return 要转到的下一个状态。
     */
    private IAIState queryItems()
    {
        if (currentRecipeStorage == null)
        {
            return START_WORKING;
        }

        return checkForItems(currentRecipeStorage);
    }

    /**
     * 检查所需配方的所有物品。
     *
     * @param storage 配方存储。
     * @return 要转到的下一个状态。
     */
    protected IAIState checkForItems(@NotNull final IRecipeStorage storage)
    {
        final int inProgressCount = getExtendedCount(currentRecipeStorage.getPrimaryOutput());
        final int countPerIteration = currentRecipeStorage.getPrimaryOutput().getCount();
        final int progressOpsCount = inProgressCount / Math.max(countPerIteration, 1);

        final List<ItemStorage> input = storage.getCleanedInput();
        for (final ItemStorage inputStorage : input)
        {
            final Predicate<ItemStack> predicate = stack -> !ItemStackUtils.isEmpty(stack) && new Stack(stack, false).matches(inputStorage.getItemStack());
            final int invCount = InventoryUtils.getItemCountInItemHandler(worker.getInventoryCitizen(), predicate);
            final ItemStack container = inputStorage.getItem().getCraftingRemainingItem(inputStorage.getItemStack());
            final int remaining;
            if(!currentRecipeStorage.getCraftingToolsAndSecondaryOutputs().isEmpty() && ItemStackUtils.compareItemStackListIgnoreStackSize(currentRecipeStorage.getCraftingToolsAndSecondaryOutputs(), inputStorage.getItemStack(), false, true))
            {
                remaining = inputStorage.getAmount();
            }
            else if(!ItemStackUtils.isEmpty(container) && ItemStackUtils.compareItemStacksIgnoreStackSize(inputStorage.getItemStack(), container , false, true))
            {
                remaining = inputStorage.getAmount();
            }
            else
            {
                remaining = inputStorage.getAmount() * Math.max(job.getMaxCraftingCount(), 1);
            }

            if (invCount <= 0 || invCount + ((job.getCraftCounter() + progressOpsCount) * inputStorage.getAmount())
                    < remaining)
            {
                if (InventoryUtils.hasItemInProvider(building, predicate))
                {
                    needsCurrently = new Tuple<>(predicate, remaining);
                    return GATHERING_REQUIRED_MATERIALS;
                }
                currentRecipeStorage = null;
                currentRequest = null;
                return GET_RECIPE;
            }
        }

        return CRAFT;
    }

    /**
     * 实际的制作逻辑。
     *
     * @return 要转到的下一个状态。
     */
    protected IAIState craft()
    {
        if (currentRecipeStorage == null || job.getCurrentTask() == null)
        {
            return START_WORKING;
        }

        if (currentRequest == null && job.getCurrentTask() != null)
        {
            return GET_RECIPE;
        }

        if (walkToBuilding())
        {
            return getState();
        }

        job.setProgress(job.getProgress() + 1);

        worker.setItemInHand(InteractionHand.MAIN_HAND,
          currentRecipeStorage.getCleanedInput().get(worker.getRandom().nextInt(currentRecipeStorage.getCleanedInput().size())).getItemStack().copy());
        worker.setItemInHand(InteractionHand.OFF_HAND, currentRecipeStorage.getPrimaryOutput().copy());
        hitBlockWithToolInHand(building.getPosition());
        Network.getNetwork().sendToTrackingEntity(new LocalizedParticleEffectMessage(worker.getMainHandItem(), building.getPosition().above()), worker);

        currentRequest = job.getCurrentTask();

        if (currentRequest != null && (currentRequest.getState() == RequestState.CANCELLED || currentRequest.getState() == RequestState.FAILED))
        {
            currentRequest = null;
            incrementActionsDone(getActionRewardForCraftingSuccess());
            currentRecipeStorage = null;
            return START_WORKING;
        }

        if (job.getProgress() >= getRequiredProgressForMakingRawMaterial())
        {
            final IAIState check = checkForItems(currentRecipeStorage);
            if (check == CRAFT)
            {
                if (!currentRecipeStorage.fullfillRecipe(getLootContext(), ImmutableList.of(worker.getItemHandlerCitizen())))
                {
                    currentRequest = null;
                    incrementActionsDone(getActionRewardForCraftingSuccess());
                    job.finishRequest(false);
                    resetValues();
                    return START_WORKING;
                }

                currentRequest.addDelivery(currentRecipeStorage.getPrimaryOutput());
                job.setCraftCounter(job.getCraftCounter() + 1);

                if (job.getCraftCounter() >= job.getMaxCraftingCount())
                {
                    incrementActionsDone(getActionRewardForCraftingSuccess());
                    final ICraftingBuildingModule module = building.getCraftingModuleForRecipe(currentRecipeStorage.getToken());
                    if (module != null)
                    {
                        module.improveRecipe(currentRecipeStorage, job.getCraftCounter(), worker.getCitizenData());
                    }

                    currentRecipeStorage = null;
                    resetValues();

                    if (inventoryNeedsDump() && job.getMaxCraftingCount() == 0 && job.getProgress() == 0 && job.getCraftCounter() == 0 && currentRequest != null)
                    {
                        worker.getCitizenExperienceHandler().addExperience(currentRequest.getRequest().getCount() / 2.0);
                    }
                }
                else
                {
                    job.setProgress(0);
                    return GET_RECIPE;
                }
            }
            else
            {
                currentRequest = null;
                job.finishRequest(false);
                incrementActionsDoneAndDecSaturation();
                resetValues();
            }
            return START_WORKING;
        }

        return getState();
    }

    public void hitBlockWithToolInHand(@Nullable final BlockPos blockPos)
    {
        worker.getLookControl().setLookAt(blockPos.getX(), blockPos.getY(), blockPos.getZ(), FACING_DELTA_YAW, worker.getMaxHeadXRot());

        worker.swing(worker.getUsedItemHand());

        final BlockState blockState = worker.level.getBlockState(blockPos);
        final BlockPos vector = blockPos.subtract(worker.blockPosition());
        final Direction facing = Direction.getNearest(vector.getX(), vector.getY(), vector.getZ()).getOpposite();

        Network.getNetwork().sendToPosition(
          new BlockParticleEffectMessage(blockPos, blockState, facing.ordinal()),
          new PacketDistributor.TargetPoint(blockPos.getX(), blockPos.getY(), blockPos.getZ(), BLOCK_BREAK_PARTICLE_RANGE, worker.level.dimension()));

        job.playSound(blockPos, (EntityCitizen) worker);
    }

    /**
     * 重置所有值。
     */
    public void resetValues()
    {
        job.setMaxCraftingCount(0);
        job.setProgress(0);
        job.setCraftCounter(0);
        worker.setItemInHand(InteractionHand.MAIN_HAND, ItemStackUtils.EMPTY);
        worker.setItemInHand(InteractionHand.OFF_HAND, ItemStackUtils.EMPTY);
    }

    @Override
    public IAIState afterDump()
    {
        if (job.getMaxCraftingCount() == 0 && job.getProgress() == 0 && job.getCraftCounter() == 0 && currentRequest != null)
        {
            // 回退安全保护。通常，craft() 方法应该处理请求。
            if (currentRequest.getState() == RequestState.IN_PROGRESS)
            {
                job.finishRequest(true);
                worker.getCitizenExperienceHandler().addExperience(currentRequest.getRequest().getCount() / 2.0);
            }
            currentRequest = null;
        }

        resetValues();
        return super.afterDump();
    }

    @Override
    protected int getActionsDoneUntilDumping()
    {
        return 1;
    }

    /**
     * 获取执行配方所需的进度。
     *
     * @return 所需的打击次数。
     */
    private int getRequiredProgressForMakingRawMaterial()
    {
        final int jobModifier = worker.getCitizenData().getCitizenSkillHandler().getLevel(((CraftingWorkerBuildingModule) getModuleForJob()).getCraftSpeedSkill()) / 2;
        return PROGRESS_MULTIPLIER / Math.min(jobModifier + 1, MAX_LEVEL) * HITTING_TIME;
    }

    @Override
    public boolean isAfterDumpPickupAllowed()
    {
        return currentRequest == null;
    }

    /**
     * 获取用于制作的LootContextBuilder
     * @return 用于制作的LootContext
     */
    protected LootContext getLootContext()
    {
        return getLootContext(false);
    }

    /**
     * 获取用于制作的LootContextBuilder
     * @param includeKiller true表示包含基于击杀者的参数
     * @return 用于制作的LootContext
     */
    protected LootContext getLootContext(boolean includeKiller)
    {
        if(playerDamageSource == null)
        {
            FakePlayer fp = FakePlayerFactory.getMinecraft((ServerLevel) this.world);
            playerDamageSource = DamageSource.playerAttack(fp);
        }

        LootContext.Builder builder =  (new LootContext.Builder((ServerLevel) this.world))
        .withParameter(LootContextParams.ORIGIN, worker.position())
        .withParameter(LootContextParams.THIS_ENTITY, worker)
        .withParameter(LootContextParams.TOOL, worker.getMainHandItem())
        .withRandom(worker.getRandom())
        .withLuck((float) getEffectiveSkillLevel(getPrimarySkillLevel()));

        if(includeKiller)
        {
            builder = builder
                .withParameter(LootContextParams.DAMAGE_SOURCE, playerDamageSource)
                .withParameter(LootContextParams.KILLER_ENTITY, playerDamageSource.getEntity())
                .withParameter(LootContextParams.DIRECT_KILLER_ENTITY, playerDamageSource.getDirectEntity());
            }
        
        return builder.create(RecipeStorage.recipeLootParameters);
    }
}
