package com.minecolonies.coremod.entity.ai.citizen.concrete;

import com.minecolonies.api.colony.requestsystem.request.RequestState;
import com.minecolonies.api.entity.ai.statemachine.states.IAIState;
import com.minecolonies.api.util.InventoryUtils;
import com.minecolonies.api.util.ItemStackUtils;
import com.minecolonies.api.util.Tuple;
import com.minecolonies.coremod.colony.buildings.workerbuildings.BuildingConcreteMixer;
import com.minecolonies.coremod.colony.jobs.JobConcreteMixer;
import com.minecolonies.coremod.entity.ai.basic.AbstractEntityAICrafting;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ConcretePowderBlock;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraftforge.items.CapabilityItemHandler;
import org.jetbrains.annotations.NotNull;

import java.util.function.Predicate;

import static com.minecolonies.api.entity.ai.statemachine.states.AIWorkerState.*;
import static com.minecolonies.api.util.constant.Constants.STACKSIZE;

/**
 * 具体的石匠AI类。
 */
public class EntityAIConcreteMixer extends AbstractEntityAICrafting<JobConcreteMixer, BuildingConcreteMixer>
{
    /**
     * 用于检查库存中是否有混凝土粉末的谓词。
     */
    private static final Predicate<ItemStack> CONCRETE =
      stack -> !stack.isEmpty() 
            && stack.getItem() instanceof BlockItem 
            && ((BlockItem) stack.getItem()).getBlock() instanceof ConcretePowderBlock;

    /**
     * 混凝土石匠的构造函数。定义混凝土石匠执行的任务。
     *
     * @param job 要使用的混凝土石匠职业。
     */
    public EntityAIConcreteMixer(@NotNull final JobConcreteMixer job)
    {
        super(job);
    }

    @Override
    public Class<BuildingConcreteMixer> getExpectedBuildingClass()
    {
        return BuildingConcreteMixer.class;
    }

    @Override
    protected IAIState decide()
    {
        // 这只需在没有标记为交付的混凝土粉末上运行。
        // 我们需要一个“输出”库存来保护它们，以防止在这里处理。
        /*
        if (job.getTaskQueue().isEmpty())
        {
            final IAIState state = mixConcrete();
            if (state != CRAFT)
            {
                return state;
            }
            return START_WORKING;
        }
        */

        if (job.getCurrentTask() == null)
        {
            return START_WORKING;
        }

        if (walkTo == null && walkToBuilding())
        {
            return START_WORKING;
        }

        if (job.getActionsDone() > 0)
        {
            // 在继续之前等待卸载。
            return getState();
        }

        if (currentRequest != null && currentRecipeStorage != null)
        {
            return QUERY_ITEMS;
        }

        return GET_RECIPE;
    }

    @Override
    protected int getExtendedCount(final ItemStack primaryOutput)
    {
        return building.outputBlockCountInWorld(primaryOutput);
    }

    /**
     * 混合混凝土并开采它。
     *
     * @return 下一个状态。
     */
    private IAIState mixConcrete()
    {
        int slot = -1;

        if(currentRequest != null && currentRecipeStorage != null)
        {
            ItemStack inputStack = currentRecipeStorage.getCleanedInput().get(0).getItemStack();
            if(CONCRETE.test(inputStack))
            {
                slot = InventoryUtils.findFirstSlotInItemHandlerWith(worker.getInventoryCitizen(), s -> ItemStackUtils.compareItemStacksIgnoreStackSize(s, inputStack));
            }
            else
            {
                return START_WORKING;
            }
        }
        else
        { 
            slot = InventoryUtils.findFirstSlotInItemHandlerWith(worker.getInventoryCitizen(), CONCRETE);
        }

        if (slot != -1)
        {
            final ItemStack stack = worker.getInventoryCitizen().getStackInSlot(slot);
            final Block block = ((BlockItem) stack.getItem()).getBlock();
            final BlockPos posToPlace = building.getBlockToPlace();
            if (posToPlace != null)
            {
                if (walkToBlock(posToPlace))
                {
                    walkTo = posToPlace;
                    return START_WORKING;
                }
                walkTo = null; 
                if (InventoryUtils.attemptReduceStackInItemHandler(worker.getInventoryCitizen(), stack, 1))
                {
                    world.setBlock(posToPlace, block.defaultBlockState().updateShape(Direction.DOWN, block.defaultBlockState(), world, posToPlace, posToPlace), 0x03);
                }
                return START_WORKING;
            }
        }

        final BlockPos pos = building.getBlockToMine();
        if (pos != null)
        {
            if (walkToBlock(pos))
            {
                walkTo = pos;
                return START_WORKING;
            }
            walkTo = null;
            if (mineBlock(pos))
            {
                this.resetActionsDone();
                return CRAFT;
            }
            return START_WORKING;
        }

        if (InventoryUtils.hasItemInItemHandler(building.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY).orElseGet(null), CONCRETE))
        {
            needsCurrently = new Tuple<>(CONCRETE, STACKSIZE);
            return GATHERING_REQUIRED_MATERIALS;
        }
        else
        {
            incrementActionsDone();
        }

        return START_WORKING;
    }

    @Override
    protected IAIState craft()
    {
        if (currentRecipeStorage == null)
        {
            return START_WORKING;
        }

        if (currentRequest == null && job.getCurrentTask() != null)
        {
            return GET_RECIPE;
        }

        if (walkTo == null && walkToBuilding())
        {
            return getState();
        }

        currentRequest = job.getCurrentTask();

        if (currentRequest != null && (currentRequest.getState() == RequestState.CANCELLED || currentRequest.getState() == RequestState.FAILED))
        {
            currentRequest = null;
            incrementActionsDone(getActionRewardForCraftingSuccess());
            currentRecipeStorage = null;
            return START_WORKING;
        }

        final ItemStack concrete = currentRecipeStorage.getPrimaryOutput();
        if (concrete.getItem() instanceof BlockItem && ((BlockItem) concrete.getItem()).getBlock() instanceof ConcretePowderBlock)
        {
            return super.craft();
        }

        final IAIState mixState = mixConcrete();
        if (mixState == getState())
        {
            currentRequest.addDelivery(new ItemStack(concrete.getItem(), 1));
            job.setCraftCounter(job.getCraftCounter() + 1);
            if (job.getCraftCounter() >= job.getMaxCraftingCount())
            {
                incrementActionsDone(getActionRewardForCraftingSuccess());
                currentRecipeStorage = null;
                resetValues();

                if (inventoryNeedsDump())
                {
                    if (job.getMaxCraftingCount() == 0 && job.getCraftCounter() == 0 && currentRequest != null)
                    {
                        job.finishRequest(true);
                        worker.getCitizenExperienceHandler().addExperience(currentRequest.getRequest().getCount() / 2.0);
                    }
                }
            }
        }

        return mixState;
    }
}
