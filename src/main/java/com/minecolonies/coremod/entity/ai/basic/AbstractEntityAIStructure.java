package com.minecolonies.coremod.entity.ai.basic;

import com.google.common.collect.ImmutableList;
import com.ldtteam.structurize.blocks.schematic.BlockFluidSubstitution;
import com.ldtteam.structurize.blueprints.v1.Blueprint;
import com.ldtteam.structurize.placement.BlockPlacementResult;
import com.ldtteam.structurize.placement.StructurePhasePlacementResult;
import com.ldtteam.structurize.placement.StructurePlacer;
import com.ldtteam.structurize.placement.structure.IStructureHandler;
import com.ldtteam.structurize.util.BlockUtils;
import com.ldtteam.structurize.util.BlueprintPositionInfo;
import com.ldtteam.structurize.util.PlacementSettings;
import com.minecolonies.api.blocks.AbstractBlockHut;
import com.minecolonies.api.blocks.ModBlocks;
import com.minecolonies.api.colony.buildings.IBuilding;
import com.minecolonies.api.colony.requestsystem.request.IRequest;
import com.minecolonies.api.colony.requestsystem.requestable.IDeliverable;
import com.minecolonies.api.colony.requestsystem.requestable.Stack;
import com.minecolonies.api.crafting.ItemStorage;
import com.minecolonies.api.entity.ai.citizen.builder.IBuilderUndestroyable;
import com.minecolonies.api.entity.ai.statemachine.AIEventTarget;
import com.minecolonies.api.entity.ai.statemachine.AITarget;
import com.minecolonies.api.entity.ai.statemachine.states.AIBlockingEventType;
import com.minecolonies.api.entity.ai.statemachine.states.IAIState;
import com.minecolonies.api.entity.citizen.AbstractEntityCitizen;
import com.minecolonies.api.items.ModTags;
import com.minecolonies.api.util.*;
import com.minecolonies.api.util.constant.TypeConstants;
import com.minecolonies.coremod.MineColonies;
import com.minecolonies.coremod.colony.buildings.AbstractBuildingStructureBuilder;
import com.minecolonies.coremod.colony.buildings.modules.BuildingResourcesModule;
import com.minecolonies.coremod.colony.buildings.utils.BuilderBucket;
import com.minecolonies.coremod.colony.buildings.utils.BuildingBuilderResource;
import com.minecolonies.coremod.colony.jobs.AbstractJobStructure;
import com.minecolonies.coremod.entity.ai.util.BuildingStructureHandler;
import com.minecolonies.coremod.tileentities.TileEntityDecorationController;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.AirBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.util.TriPredicate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import static com.ldtteam.structurize.placement.AbstractBlueprintIterator.NULL_POS;
import static com.minecolonies.api.entity.ai.statemachine.states.AIWorkerState.*;
import static com.minecolonies.api.research.util.ResearchConstants.BLOCK_PLACE_SPEED;
import static com.minecolonies.api.util.constant.CitizenConstants.*;
import static com.minecolonies.api.util.constant.Constants.TICKS_SECOND;
import static com.minecolonies.coremod.colony.buildings.workerbuildings.BuildingMiner.FILL_BLOCK;
import static com.minecolonies.coremod.entity.ai.basic.AbstractEntityAIStructure.ItemCheckResult.*;
import static com.minecolonies.coremod.entity.ai.util.BuildingStructureHandler.Stage.*;

/**
 * 这个基础AI类用于需要建造整个结构的AI。这些结构必须作为示意图文件提供。
 * <p>
 * 一旦AI开始建造一个结构，只有在完成后才会恢复对其的控制权。
 * <p>
 * 如果AI重置，结构就会消失，因此只需重新开始建造，不会重置任何进度。
 *
 * @param <J> 此AI需要执行的工作类型。
 */
public abstract class AbstractEntityAIStructure<J extends AbstractJobStructure<?, J>, B extends AbstractBuildingStructureBuilder> extends AbstractEntityAIInteract<J, B>
{
    /**
     * 当前要建造的结构任务。
     */
    protected Tuple<StructurePlacer, BuildingStructureHandler<J, B>> structurePlacer;

    /**
     * 如果结构状态当前已达到极限而不是阻止放置。
     */
    protected boolean limitReached = false;

    /**
     * 不同的物品检查结果可能性。
     */
    public enum ItemCheckResult
    {
        FAIL,
        SUCCESS,
        RECALC
    }

    /**
     * 定义建造者绝不应该触及的事物的断言。
     */
    protected TriPredicate<BlueprintPositionInfo, BlockPos, IStructureHandler> DONT_TOUCH_PREDICATE = (info, worldPos, handler) ->
    {
        final BlockState worldState = handler.getWorld().getBlockState(worldPos);

        return worldState.getBlock() instanceof IBuilderUndestroyable
                || worldState.getBlock() == Blocks.BEDROCK
                || (info.getBlockInfo().getState().getBlock() instanceof AbstractBlockHut && handler.getWorldPos().equals(worldPos)
                && worldState.getBlock() instanceof AbstractBlockHut);
    };

    /**
     * 建筑师开始建造的位置。
     */
    protected BlockPos workFrom;

    /**
     * 要挖掘的方块。
     */
    protected BlockPos blockToMine;

    /**
     * 最后拾取的物品列表中的项目ID。
     */
    private int pickUpCount = 0;

    /**
     * 创建此AI基类并设置重要事项。
     * <p>
     * 请始终使用此构造函数！
     *
     * @param job 使用此基类的AI的工作类。
     */
    protected AbstractEntityAIStructure(@NotNull final J job)
    {
        super(job);
        this.registerTargets(

                /*
                 * 拾取可能被丢弃的物品
                 */
                new AITarget(PICK_UP_RESIDUALS, this::pickUpResiduals, TICKS_SECOND),
                /*
                 * 检查是否应该执行任务
                 */
                new AIEventTarget(AIBlockingEventType.STATE_BLOCKING, this::checkIfCanceled, IDLE, 1),
                /*
                 * 选择适当的状态来执行下一步
                 */
                new AITarget(LOAD_STRUCTURE, this::loadRequirements, 5),
                /*
                 * 选择适当的状态来执行下一步
                 */
                new AITarget(START_BUILDING, this::startBuilding, 1),
                /*
                 * 选择适当的状态来执行下一步
                 */
                new AITarget(MINE_BLOCK, this::doMining, 10),
                /*
                 * 检查是否需要建造某物
                 */
                new AITarget(IDLE, this::isThereAStructureToBuild, () -> START_BUILDING, 100),
                /*
                 * 建造建筑的结构和基础
                 */
                new AITarget(BUILDING_STEP, this::structureStep, STANDARD_DELAY),
                /*
                 * 完成建筑并将控制权交还给AI
                 */
                new AITarget(COMPLETE_BUILD, this::completeBuild, STANDARD_DELAY),
                new AITarget(PICK_UP, this::pickUpMaterial, 5)
        );

    }

    /**
     * 状态：在继续工作之前拾取材料。
     *
     * @return 下一个状态。
     */
    public IAIState pickUpMaterial()
    {
        if (structurePlacer == null || !structurePlacer.getB().hasBluePrint())
        {
            return IDLE;
        }

        if (structurePlacer.getB().getStage() == null || structurePlacer.getB().getStage() == BuildingStructureHandler.Stage.CLEAR)
        {
            pickUpCount = 0;
            return START_WORKING;
        }

        final List<Tuple<Predicate<ItemStack>, Integer>> neededItemsList = new ArrayList<>();

        final BuilderBucket neededRessourcesMap = building.getRequiredResources();
        final BuildingResourcesModule module = building.getFirstModuleOccurance(BuildingResourcesModule.class);
        if (neededRessourcesMap != null)
        {
            for (final Map.Entry<String, Integer> entry : neededRessourcesMap.getResourceMap().entrySet())
            {
                final BuildingBuilderResource res = module.getResourceFromIdentifier(entry.getKey());
                if (res != null)
                {
                    int amount = entry.getValue();
                    neededItemsList.add(new Tuple<>(itemstack -> ItemStackUtils.compareItemStacksIgnoreStackSize(res.getItemStack(), itemstack, true, true), amount));
                }
            }
        }

        if (neededItemsList.size() <= pickUpCount || InventoryUtils.openSlotCount(worker.getInventoryCitizen()) <= MIN_OPEN_SLOTS)
        {
            building.checkOrRequestBucket(building.getRequiredResources(), worker.getCitizenData(), true);
            building.checkOrRequestBucket(building.getNextBucket(), worker.getCitizenData(), false);
            pickUpCount = 0;
            return START_WORKING;
        }

        needsCurrently = neededItemsList.get(pickUpCount);
        pickUpCount++;

        if (InventoryUtils.hasItemInProvider(building.getTileEntity(), needsCurrently.getA()))
        {
            return GATHERING_REQUIRED_MATERIALS;
        }

        return PICK_UP;
    }

    /**
     * 拾取建筑区域内的残留物品。
     *
     * @return 下一个状态。
     */
    protected IAIState pickUpResiduals()
    {
        if (structurePlacer != null && structurePlacer.getB().getStage() != null)
        {
            return IDLE;
        }

        if (getItemsForPickUp() == null)
        {
            fillItemsList();
        }

        if (getItemsForPickUp() != null && !getItemsForPickUp().isEmpty())
        {
            gatherItems();
            return getState();
        }

        resetGatheringItems();
        workFrom = null;
        structurePlacer = null;

        return IDLE;
    }

    /**
     * 完成逻辑。
     *
     * @return 完成后的最终状态。
     */
    protected IAIState completeBuild()
    {
        incrementActionsDoneAndDecSaturation();
        executeSpecificCompleteActions();
        worker.getCitizenExperienceHandler().addExperience(XP_EACH_BUILDING);

        return PICK_UP_RESIDUALS;
    }

    /**
     * 开始建造这个StructureIterator。
     * <p>
     * 将确定从哪里开始。
     *
     * @return 开始的新状态。
     */
    @NotNull
    protected IAIState startBuilding()
    {
        if (structurePlacer == null || !structurePlacer.getB().hasBluePrint())
        {
            return LOAD_STRUCTURE;
        }
        return BUILDING_STEP;
    }

    /**
     * 结构加载后的下一个状态。
     * @return 下一个状态。
     */
    public IAIState afterStructureLoading()
    {
        return START_BUILDING;
    }

    /**
     * 走到当前建设地点。
     * <p>
     * 计算并缓存要走到的位置。
     *
     * @param currentBlock 当前正在工作的方块。
     * @return 在走到工地时返回true。
     */
    public boolean walkToConstructionSite(final BlockPos currentBlock)
    {
        if (workFrom == null)
        {
            workFrom = getWorkingPosition(currentBlock);
        }

        //矿工不应搜索安全位置。只需从当前位置建造即可。
        return worker.isWorkerAtSiteWithMove(workFrom, STANDARD_WORKING_RANGE) || MathUtils.twoDimDistance(worker.blockPosition(), workFrom) < MIN_WORKING_RANGE;
    }

    /**
     * 检查需要作为装饰处理的方块。
     */
    protected static boolean isDecoItem(Block block)
    {
        return block.defaultBlockState().is(ModTags.decorationItems) || block instanceof BlockFluidSubstitution;
    }

    /**
     * 执行实际的放置等操作的结构步骤。
     *
     * @return 前进到的下一个步骤。
     */
    protected IAIState structureStep()
    {
        if (structurePlacer.getB().getStage() == null)
        {
            return PICK_UP_RESIDUALS;
        }

        if (InventoryUtils.isItemHandlerFull(worker.getInventoryCitizen()))
        {
            return INVENTORY_FULL;
        }

        worker.getCitizenStatusHandler().setLatestStatus(Component.translatable("com.minecolonies.coremod.status.building"));

        checkForExtraBuildingActions();

        //一些事情要做！然后我们进入实际阶段！

        //填充workFrom，指示建筑师应该从哪里开始建造。
        //还确保我们在该位置。
        final BlockPos progress = getProgressPos() == null ? NULL_POS : getProgressPos().getA();
        final BlockPos worldPos = structurePlacer.getB().getProgressPosInWorld(progress);
        if (getProgressPos() != null)
        {
            structurePlacer.getB().setStage(getProgressPos().getB());
        }

        if (!progress.equals(NULL_POS) && !limitReached && (blockToMine == null ? !walkToConstructionSite(worldPos) : !walkToConstructionSite(blockToMine)))
        {
            return getState();
        }

        limitReached = false;

        final StructurePhasePlacementResult result;
        final StructurePlacer placer = structurePlacer.getA();
        switch (structurePlacer.getB().getStage())
        {
            case BUILD_SOLID:
                //结构

                result = placer.executeStructureStep(world,
                  null,
                  progress,
                  StructurePlacer.Operation.BLOCK_PLACEMENT,
                  () -> placer.getIterator()
                          .increment(DONT_TOUCH_PREDICATE.or((info, pos, handler) -> !info.getBlockInfo().getState().getMaterial().isSolid() || isDecoItem(info.getBlockInfo()
                                                                                                                                                  .getState()
                                                                                                                                                  .getBlock()))),
                  false);
                break;
            case CLEAR_WATER:

                //水
                result = placer.executeStructureStep(world, null, progress, StructurePlacer.Operation.WATER_REMOVAL,
                  () -> placer.getIterator().decrement((info, pos, handler) -> handler.getWorld().getBlockState(pos).getFluidState().isEmpty()), false);
                break;
            case CLEAR_NON_SOLIDS:
                // 清除空气
                result = placer.executeStructureStep(world,
                  null,
                  progress,
                  StructurePlacer.Operation.BLOCK_PLACEMENT,
                  () -> placer.getIterator()
                          .decrement(DONT_TOUCH_PREDICATE.or((info, pos, handler) -> !(info.getBlockInfo().getState().getBlock() instanceof AirBlock) || (handler.getWorld().isEmptyBlock(pos)))),
                  false);
                break;
            case DECORATE:

                // 非实体
                result = placer.executeStructureStep(world,
                  null,
                  progress,
                  StructurePlacer.Operation.BLOCK_PLACEMENT,
                  () -> placer.getIterator()
                          .increment(DONT_TOUCH_PREDICATE.or((info, pos, handler) -> info.getBlockInfo().getState().getMaterial().isSolid() && !isDecoItem(info.getBlockInfo()
                                                                                                                                                   .getState()
                                                                                                                                                   .getBlock()))),
                  false);
                break;
            case SPAWN:
                // 实体
                result = placer.executeStructureStep(world, null, progress, StructurePlacer.Operation.BLOCK_PLACEMENT,
                  () -> placer.getIterator().increment(DONT_TOUCH_PREDICATE.or((info, pos, handler) -> info.getEntities().length == 0)), true);
                break;
            case REMOVE_WATER:
                //水
                placer.getIterator().setRemoving();
                result = placer.executeStructureStep(world, null, progress, StructurePlacer.Operation.WATER_REMOVAL,
                  () -> placer.getIterator().decrement((info, pos, handler) -> info.getBlockInfo().getState().getFluidState().isEmpty()), false);
                break;
            case REMOVE:
                placer.getIterator().setRemoving();
                result = placer.executeStructureStep(world, null, progress, StructurePlacer.Operation.BLOCK_REMOVAL,
                  () -> placer.getIterator().decrement((info, pos, handler) -> handler.getWorld().getBlockState(pos).getBlock() instanceof AirBlock
                                                                                                         || info.getBlockInfo().getState().getBlock() instanceof AirBlock
                                                                                                         || !handler.getWorld().getBlockState(pos).getFluidState().isEmpty()
                                                                                                         || info.getBlockInfo().getState().getBlock()
                                                                                                              == com.ldtteam.structurize.blocks.ModBlocks.blockSolidSubstitution.get()
                                                                                                         || info.getBlockInfo().getState().getBlock()
                                                                                                              == com.ldtteam.structurize.blocks.ModBlocks.blockSubstitution.get()
                                                                                                         || info.getBlockInfo().getState().getBlock()
                                                                                                              == com.ldtteam.structurize.blocks.ModBlocks.blockSubstitution.get()
                                                                                                         || handler.getWorld().getBlockState(pos).getBlock() instanceof IBuilderUndestroyable),
                  true);
                break;
            case CLEAR:
            default:

                result = placer.executeStructureStep(world, null, progress, StructurePlacer.Operation.BLOCK_REMOVAL,
                  () -> placer.getIterator().decrement((info, pos, handler) -> handler.getWorld().getBlockState(pos).getBlock() instanceof IBuilderUndestroyable
                                                                                 || handler.getWorld().getBlockState(pos).getBlock() == Blocks.BEDROCK
                                                                                 || handler.getWorld().getBlockState(pos).getBlock() instanceof AirBlock
                                                                                 || info.getBlockInfo().getState().getBlock()
                                                                                      == com.ldtteam.structurize.blocks.ModBlocks.blockFluidSubstitution.get()
                                                                                 || !handler.getWorld().getBlockState(pos).getFluidState().isEmpty()), false);
                if (result.getBlockResult().getResult() == BlockPlacementResult.Result.FINISHED)
                {
                    building.checkOrRequestBucket(building.getRequiredResources(), worker.getCitizenData(), true);
                }
                break;
        }

        if (result.getBlockResult().getResult() == BlockPlacementResult.Result.FAIL)
        {
            Log.getLogger().error("Failed placement at: " + result.getBlockResult().getWorldPos().toShortString());
        }

        if (result.getBlockResult().getResult() == BlockPlacementResult.Result.FINISHED)
        {
            building.nextStage();
            if (!structurePlacer.getB().nextStage())
            {
                building.setProgressPos(null, null);
                return COMPLETE_BUILD;
            }
        }
        else if (result.getBlockResult().getResult() == BlockPlacementResult.Result.LIMIT_REACHED)
        {
            this.limitReached = true;
        }
        this.storeProgressPos(result.getIteratorPos(), structurePlacer.getB().getStage());

        if (result.getBlockResult().getResult() == BlockPlacementResult.Result.MISSING_ITEMS)
        {
            if (hasListOfResInInvOrRequest(this, result.getBlockResult().getRequiredItems(), result.getBlockResult().getRequiredItems().size() > 1) == RECALC)
            {
                job.getWorkOrder().setRequested(false);
                return LOAD_STRUCTURE;
            }
            return NEEDS_ITEM;
        }

        worker.swing(InteractionHand.MAIN_HAND);
        worker.queueSound(SoundEvents.BAMBOO_HIT, worker.blockPosition(), 10, 0, 0.5f, 0.1f);

        if (result.getBlockResult().getResult() == BlockPlacementResult.Result.BREAK_BLOCK)
        {
            blockToMine = result.getBlockResult().getWorldPos();
            return MINE_BLOCK;
        }
        else
        {
            blockToMine = null;
        }

        if (MineColonies.getConfig().getServer().builderBuildBlockDelay.get() > 0)
        {
            final double decrease = 1 - worker.getCitizenColonyHandler().getColony().getResearchManager().getResearchEffects().getEffectStrength(BLOCK_PLACE_SPEED);

            setDelay((int) (
              (MineColonies.getConfig().getServer().builderBuildBlockDelay.get() * PROGRESS_MULTIPLIER / (getPlaceSpeedLevel() / 2 + PROGRESS_MULTIPLIER))
                * decrease));
        }
        return getState();
    }

    /**
     * 获取影响放置速度的等级。
     * @return 等级。
     */
    public abstract int getPlaceSpeedLevel();

    @Override
    public IAIState getStateAfterPickUp()
    {
        return PICK_UP;
    }

    @Override
    public IAIState afterRequestPickUp()
    {
        return INVENTORY_FULL;
    }

    @Override
    public IAIState afterDump()
    {
        return PICK_UP;
    }

    /**
     * 用于采矿的单独步骤。
     * @return 下一个要前往的状态。
     */
    public IAIState doMining()
    {
        if (blockToMine == null || world.getBlockState(blockToMine).getBlock() instanceof AirBlock)
        {
            return BUILDING_STEP;
        }

        if (!mineBlock(blockToMine, getCurrentWorkingPosition()))
        {
            worker.swing(InteractionHand.MAIN_HAND);
            return getState();
        }
        worker.decreaseSaturationForContinuousAction();
        return BUILDING_STEP;
    }

    /**
     * 根据名称、旋转次数和位置加载结构。
     *
     * @param name        要获取的名称。
     * @param rotateTimes 旋转的次数。
     * @param position    要设置的位置。
     * @param isMirrored  结构是否镜像？
     * @param removal     是否是移除步骤。
     */
    public void loadStructure(@NotNull final String name, final int rotateTimes, final BlockPos position, final boolean isMirrored, final boolean removal)
    {
        final BuildingStructureHandler<J, B> structure;
        IBuilding colonyBuilding = worker.getCitizenColonyHandler().getColony().getBuildingManager().getBuilding(position);
        final BlockEntity entity = world.getBlockEntity(position);

        if (removal)
        {
            structure = new BuildingStructureHandler<>(world,
              position,
              name,
              new PlacementSettings(isMirrored ? Mirror.FRONT_BACK : Mirror.NONE, BlockPosUtil.getRotationFromRotations(rotateTimes)),
              this, new BuildingStructureHandler.Stage[] {REMOVE_WATER, REMOVE});
            building.setTotalStages(2);
        }
        else if ((colonyBuilding != null && (colonyBuilding.getBuildingLevel() > 0 || colonyBuilding.hasParent())) ||
                   (entity instanceof TileEntityDecorationController && ((TileEntityDecorationController) entity).getTier() > 0))
        {
            structure = new BuildingStructureHandler<>(world,
              position,
              name,
              new PlacementSettings(isMirrored ? Mirror.FRONT_BACK : Mirror.NONE, BlockPosUtil.getRotationFromRotations(rotateTimes)),
              this, new BuildingStructureHandler.Stage[] {BUILD_SOLID, CLEAR_WATER, CLEAR_NON_SOLIDS, DECORATE, SPAWN});
            building.setTotalStages(5);
        }
        else
        {
            structure = new BuildingStructureHandler<>(world,
              position,
              name,
              new PlacementSettings(isMirrored ? Mirror.FRONT_BACK : Mirror.NONE, BlockPosUtil.getRotationFromRotations(rotateTimes)),
              this, new BuildingStructureHandler.Stage[] {CLEAR, BUILD_SOLID, CLEAR_WATER, CLEAR_NON_SOLIDS, DECORATE, SPAWN});
            building.setTotalStages(6);
        }

        if (!structure.hasBluePrint())
        {
            handleSpecificCancelActions();
            Log.getLogger().warn("找不到名称为: " + name + " 的结构，中止加载过程");
            return;
        }
        final MutableComponent jobName = Component.translatable(worker.getCitizenData().getJob().getJobRegistryEntry().getTranslationKey().toLowerCase());
        job.setBlueprint(structure.getBluePrint());
        job.getBlueprint().rotateWithMirror(BlockPosUtil.getRotationFromRotations(rotateTimes), isMirrored ? Mirror.FRONT_BACK : Mirror.NONE, world);
        setStructurePlacer(structure);

        if (getProgressPos() != null)
        {
            structure.setStage(getProgressPos().getB());
        }
    }

    /**
     * 设置结构放置者。
     * @param structure 放置者。
     */
    public void setStructurePlacer(final BuildingStructureHandler<J, B> structure)
    {
        structurePlacer = new Tuple<>(new StructurePlacer(structure), structure);
    }

    /**
     * 检查放置者的库存中是否有物品列表中的物品，并在找到后从列表中移除它们。
     *
     * @param placer   放置者。
     * @param itemList 要检查的物品列表。
     * @param force    是否强制插入。
     * @return 如果需要请求则返回true。
     */
    public static <J extends AbstractJobStructure<?, J>, B extends AbstractBuildingStructureBuilder> ItemCheckResult hasListOfResInInvOrRequest(
      @NotNull final AbstractEntityAIStructure<J, B> placer,
      final List<ItemStack> itemList,
      final boolean force)
    {
        final Map<ItemStorage, Integer> requestedMap = new HashMap<>();
        for (final ItemStack stack : itemList)
        {
            if (stack.getItem() instanceof BlockItem && isBlockFree(((BlockItem) stack.getItem()).getBlock().defaultBlockState()))
            {
                continue;
            }

            ItemStorage tempStorage = new ItemStorage(stack.copy());
            if (requestedMap.containsKey(tempStorage))
            {
                final int oldSize = requestedMap.get(tempStorage);
                tempStorage.setAmount(tempStorage.getAmount() + oldSize);
            }
            requestedMap.put(tempStorage, tempStorage.getAmount());
        }

        for (final ItemStorage stack : requestedMap.keySet())
        {
            if (!InventoryUtils.hasItemInItemHandler(placer.getInventory(), stack1 ->  ItemStackUtils.compareItemStacksIgnoreStackSize(stack.getItemStack(), stack1)) && !placer.building.hasResourceInBucket(stack.getItemStack()))
            {
                return RECALC;
            }
        }

        final List<ItemStack> foundStacks = InventoryUtils.filterItemHandler(placer.getWorker().getInventoryCitizen(),
          itemStack -> requestedMap.keySet().stream().anyMatch(storage -> ItemStackUtils.compareItemStacksIgnoreStackSize(storage.getItemStack(), itemStack)));

        final Map<ItemStorage, Integer> localMap = new HashMap<>();
        for (final ItemStack stack : foundStacks)
        {
            ItemStorage tempStorage = new ItemStorage(stack.copy());
            if (localMap.containsKey(tempStorage))
            {
                final int oldSize = localMap.get(tempStorage);
                tempStorage.setAmount(tempStorage.getAmount() + oldSize);
            }
            localMap.put(tempStorage, tempStorage.getAmount());
        }

        if (force)
        {
            for (final Map.Entry<ItemStorage, Integer> local : localMap.entrySet())
            {
                int req = requestedMap.getOrDefault(local.getKey(), 0);
                if (req != 0)
                {
                    if (local.getValue() >= req)
                    {
                        requestedMap.remove(local.getKey());
                    }
                    else
                    {
                        requestedMap.put(local.getKey(), req - local.getValue());

                    }
                }
            }
        }
        else
        {
            requestedMap.entrySet().removeIf(entry -> ItemStackUtils.isEmpty(entry.getKey().getItemStack()) || foundStacks.stream().anyMatch(target -> ItemStackUtils.compareItemStacksIgnoreStackSize(target, entry.getKey().getItemStack())));
        }

        for (final Map.Entry<ItemStorage, Integer> placedStack : requestedMap.entrySet())
        {
            final ItemStack stack = placedStack.getKey().getItemStack();
            if (ItemStackUtils.isEmpty(stack))
            {
                return FAIL;
            }

            final ImmutableList<IRequest<? extends IDeliverable>> requests = placer.building
                                                                               .getOpenRequestsOfTypeFiltered(
                                                                                 placer.getWorker().getCitizenData(),
                                                                                 TypeConstants.DELIVERABLE,
                                                                                 (IRequest<? extends IDeliverable> r) -> r.getRequest().matches(stack));

            final ImmutableList<IRequest<? extends IDeliverable>> completedRequests = placer.building
                                                                                        .getCompletedRequestsOfTypeFiltered(
                                                                                          placer.getWorker().getCitizenData(),
                                                                                          TypeConstants.DELIVERABLE,
                                                                                          (IRequest<? extends IDeliverable> r) -> r.getRequest().matches(stack));

            if (requests.isEmpty() && completedRequests.isEmpty())
            {
                final com.minecolonies.api.colony.requestsystem.requestable.Stack stackRequest = new Stack(stack, placer.getTotalAmount(stack).getCount(), 1);
                placer.getWorker().getCitizenData().createRequest(stackRequest);
                placer.registerBlockAsNeeded(stack);
                return FAIL;
            }
            else
            {
                for (final IRequest<? extends IDeliverable> request : requests)
                {
                    if (placer.worker.getCitizenJobHandler().getColonyJob().getAsyncRequests().contains(request.getId()))
                    {
                        placer.worker.getCitizenJobHandler().getColonyJob().markRequestSync(request.getId());
                    }
                }

                for (final IRequest<? extends IDeliverable> request : completedRequests)
                {
                    if (placer.worker.getCitizenJobHandler().getColonyJob().getAsyncRequests().contains(request.getId()))
                    {
                        placer.worker.getCitizenJobHandler().getColonyJob().markRequestSync(request.getId());
                    }
                }
            }
            return FAIL;
        }
        return SUCCESS;
    }

    /**
     * 加载结构的所有要求。
     * @return 下一个要前往的状态。
     */
    public IAIState loadRequirements()
    {
        return START_WORKING;
    }

    /**
     * 遍历所有所需资源，并将其存储在建筑中。
     * @return 如果完成则返回true。
     */
    public boolean requestMaterials()
    {
        /*
         * 如果需要，可以进行覆盖。
         */
        return true;
    }

    /**
     * 将块注册为需要的块，如果可能的话。
     *
     * @param stack 块的堆栈。
     */
    public void registerBlockAsNeeded(final ItemStack stack)
    {
        /*
         * 如果需要的话，在子类中进行覆盖。
         */
    }

    /**
     * 如果需要，将进度位置存储在建筑中，供工人使用。
     *
     * @param blockPos 进度位置。
     * @param stage    当前阶段。
     */
    public void storeProgressPos(final BlockPos blockPos, final BuildingStructureHandler.Stage stage)
    {
        /*
         * 如果需要，可以进行覆盖。
         */
    }

    /**
     * 填充物品列表。
     */
    @Override
    public void fillItemsList()
    {
        worker.getCitizenStatusHandler().setLatestStatus(Component.translatable("com.minecolonies.coremod.status.gathering"));

        if (!structurePlacer.getB().hasBluePrint())
        {
            return;
        }
        final Blueprint blueprint = structurePlacer.getB().getBluePrint();

        final BlockPos leftCorner = structurePlacer.getB().getWorldPos().subtract(blueprint.getPrimaryBlockOffset());
        searchForItems(new AABB(leftCorner, leftCorner.offset(blueprint.getSizeX(), blueprint.getSizeY(), blueprint.getSizeZ())));
    }

    /**
     * 计算工作位置。
     * <p>
     * 从宽度和长度中取一个最小的距离。
     * <p>
     * 然后查找该距离处的地板级别，然后检查它是否包含两个空气级别。
     *
     * @param targetPosition 要工作的位置。
     * @return 从哪个位置开始工作的BlockPos。
     */
    @Override
    public BlockPos getWorkingPosition(final BlockPos targetPosition)
    {
        // 获取长度或宽度中较大的一个。
        final int length = structurePlacer.getB().getBluePrint().getSizeX();
        final int width = structurePlacer.getB().getBluePrint().getSizeZ();
        final int distance = Math.max(width, length) + MIN_ADDITIONAL_RANGE_TO_BUILD;

        return getWorkingPosition(distance, targetPosition, 0);
    }

    /**
     * 定义可免费建造的块。
     *
     * @param block 要检查是否免费的块。
     * @return true或false。
     */
    public static boolean isBlockFree(@Nullable final BlockState block)
    {
        return block == null
                || BlockUtils.isWater(block)
                || block.is(BlockTags.LEAVES)
                || block.getBlock() == ModBlocks.blockDecorationPlaceholder;
    }

    /**
     * 如果需要，子类可以重写此方法。
     *
     * @return 如果需要，返回true。
     */
    protected boolean isAlreadyCleared()
    {
        return false;
    }

    /**
     * 获取工人的当前工作位置。如果workFrom为null，则计算一个新位置。
     *
     * @return 当前的工作位置。
     */
    protected BlockPos getCurrentWorkingPosition()
    {
        return workFrom == null ? getWorkingPosition(structurePlacer.getB().getProgressPosInWorld(structurePlacer.getA().getIterator().getProgressPos())) : workFrom;
    }

    /**
     * 检查是否存在要建造的结构。
     *
     * @return 如果应该开始建造则返回true。
     */
    protected boolean isThereAStructureToBuild()
    {
        if (structurePlacer == null || !structurePlacer.getB().hasBluePrint())
        {
            worker.getCitizenStatusHandler().setLatestStatus(Component.translatable("com.minecolonies.coremod.status.waitingForBuild"));
            return false;
        }
        return true;
    }

    /**
     * 减少所需资源的数量。
     *
     * @param stack 已使用的堆栈。
     */
    public void reduceNeededResources(final ItemStack stack)
    {
        /*
         * 在这里没有需要做的事情。工人如果需要的话会覆盖这个方法。
         */
    }

    /**
     * 检查是否有额外的建筑选项与每个块相关。
     */
    public void checkForExtraBuildingActions()
    {
        /*
         * 如果需要，可以在工人中进行覆盖。
         */
    }

    /**
     * 处理结构取消的特定操作。
     */
    public void handleSpecificCancelActions()
    {
        /*
         * 子类必须覆盖此方法。
         */
    }

    /**
     * 检查实际需要多少特定的物品。
     *
     * @param stack 要检查的堆栈。
     * @return 包含正确数量的新堆栈。
     */
    @Nullable
    public ItemStack getTotalAmount(@Nullable final ItemStack stack)
    {
        return stack;
    }

    /**
     * 将currentStructure设置为null。
     */
    public void resetCurrentStructure()
    {
        workFrom = null;
        structurePlacer = null;
        building.setProgressPos(null, null);
    }

    /**
     * 获取AI的工人。
     *
     * @return EntityCitizen对象。
     */
    public AbstractEntityCitizen getWorker()
    {
        return this.worker;
    }

    /**
     * 获取当前结构的进度。
     *
     * @return 带有当前阶段的进度。
     */
    public abstract Tuple<BlockPos, BuildingStructureHandler.Stage> getProgressPos();

    /**
     * 检查是否应该覆盖特定情况下的固体替代块。
     *
     * @param worldBlock    世界块。
     * @param worldMetadata 世界元数据。
     * @return 如果应该覆盖则返回true。
     */
    public abstract boolean shallReplaceSolidSubstitutionBlock(final Block worldBlock, final BlockState worldMetadata);

    /**
     * 寻找一个方便的块来替代一个本应是固体的空间。
     *
     * @param ignored 要放置的位置。
     * @return 块。
     */
    public BlockState getSolidSubstitution(final BlockPos ignored)
    {
        return building.getSetting(FILL_BLOCK).getValue().getBlock().defaultBlockState();
    }

    /**
     * 执行加载结构时的特定操作。
     */
    protected abstract void executeSpecificCompleteActions();

    /**
     * 检查结构任务是否已取消。
     *
     * @return 如果重置为空闲状态则返回true。
     */
    protected abstract boolean checkIfCanceled();
}
