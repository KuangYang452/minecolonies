package com.minecolonies.coremod.entity.ai.basic;

import com.minecolonies.api.entity.ai.citizen.builder.IBuilderUndestroyable;
import com.minecolonies.api.entity.pathfinding.PathResult;
import com.minecolonies.api.util.BlockPosUtil;
import com.minecolonies.api.util.InventoryUtils;
import com.minecolonies.api.util.ItemStackUtils;
import com.minecolonies.api.util.MathUtils;
import com.minecolonies.coremod.MineColonies;
import com.minecolonies.coremod.colony.buildings.AbstractBuilding;
import com.minecolonies.coremod.colony.jobs.AbstractJob;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.AirBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.minecolonies.api.research.util.ResearchConstants.BLOCK_BREAK_SPEED;

/**
 * 这是所有工人 AI 的基类。每个 AI 都会实现此类，并附带其工作类型。类中包含一些实用工具：
 * - AI 将在建筑箱中清空完整的库存。
 * - AI 将以延迟方式模拟挖掘方块。
 * - AI 将自动请求物品和工具（并从建筑箱中收集它们）。
 *
 * @param <J> 此 AI 必须执行的工作类型。
 */
public abstract class AbstractEntityAIInteract<J extends AbstractJob<?, J>, B extends AbstractBuilding> extends AbstractEntityAISkill<J, B>
{
    /**
     * 工作中的渲染元数据。
     */
    public static final String RENDER_META_WORKING = "working";

    /**
     * 实体每挖掘一个方块所获得的经验值数量。
     */
    public static final double XP_PER_BLOCK = 0.05D;

    /**
     * 如果我们高一级所需时间的百分比。
     */
    private static final double LEVEL_MODIFIER = 0.85D;

    /**
     * 建造者必须达到的最小范围，以便进行建造或清除操作。
     */
    private static final int MIN_WORKING_RANGE = 12;

    /**
     * 用于捡起物品的工人周围范围。
     */
    private static final int ITEM_PICKUP_RANGE = 3;

    /**
     * 等待的时钟滴答声，直到发现卡住。
     */
    private static final int STUCK_WAIT_TICKS = 20;

    /**
     * 工人拾取物品的水平范围。
     */
    public static final float RANGE_HORIZONTAL_PICKUP = 45.0F;

    /**
     * 工人捡取物品的垂直范围。
     */
    public static final float RANGE_VERTICAL_PICKUP = 3.0F;

    /**
     * 工作者静止不动的滴答数。
     */
    private int stillTicks = 0;

    /**
     * 用于存储路径索引，以检查工人是否仍在行走。
     */
    private int previousIndex = 0;

    /**
     * 所有需要收集的物品的位置。
     */
    @Nullable
    private List<BlockPos> items;

    /**
     * 当前随机位置的路径。
     */
    private PathResult pathResult;

    /**
     * 路径的备份因子。
     */
    protected int pathBackupFactor = 1;

    /**
     * 创建AI的抽象部分。始终使用此构造函数！
     *
     * @param job 要执行的工作
     */
    public AbstractEntityAIInteract(@NotNull final J job)
    {
        super(job);
        super.registerTargets(
          //no new targets for now
        );
    }

    /**
     * 模拟挖掘方块并产生粒子、物品掉落等效果。注意：由于模拟了延迟，必须调用两次。因此，请确保在此函数之前的代码路径可达第二次调用。同时，确保在返回 false 时立即退出更新函数。
     *
     * @param blockToMine 应该被挖掘的方块
     * @return 当完成时返回 true
     */
    protected final boolean mineBlock(@NotNull final BlockPos blockToMine)
    {
        return mineBlock(blockToMine, worker.blockPosition());
    }

    /**
     * 模拟挖掘方块并生成粒子、物品掉落等效果。注意：由于它模拟了延迟，必须调用两次。因此，请确保在调用此函数之前的代码路径可以再次执行。并且确保在此返回 false 时立即退出更新函数。
     *
     * @param blockToMine 要挖掘的方块
     * @param safeStand 我们要站立以执行挖掘的方块
     * @return 一旦完成，返回 true
     */
    protected boolean mineBlock(@NotNull final BlockPos blockToMine, @NotNull final BlockPos safeStand)
    {
        return mineBlock(blockToMine, safeStand, true, true, null);
    }

    /**
     * 模拟采矿过程，包括粒子、物品掉落等。注意：因为它模拟了延迟，必须调用两次。
     * 因此，请确保调用此函数之前的代码路径可达第二次，并确保在此返回 false 后立即退出更新函数。
     *
     * @param blockToMine      要采矿的方块
     * @param safeStand        我们要站在上面进行采矿的方块
     * @param damageTool       是否要损坏使用的工具
     * @param getDrops         是否要获取掉落物品
     * @param blockBreakAction 用于替代默认方块破坏操作的可运行对象，可以为 null
     * @return 一旦完成采矿，返回 true
     */
    protected final boolean mineBlock(
      @NotNull final BlockPos blockToMine,
      @NotNull final BlockPos safeStand,
      final boolean damageTool,
      final boolean getDrops,
      final Runnable blockBreakAction)
    {
        final BlockState curBlockState = world.getBlockState(blockToMine);
        @Nullable final Block curBlock = curBlockState.getBlock();
        if (curBlock instanceof AirBlock
              || curBlock instanceof IBuilderUndestroyable
              || curBlock == Blocks.BEDROCK)
        {
            if (curBlockState.getMaterial().isLiquid())
            {
                world.removeBlock(blockToMine, false);
            }
            //no need to mine block...
            return true;
        }

        if (checkMiningLocation(blockToMine, safeStand))
        {
            //we have to wait for delay
            return false;
        }

        final ItemStack tool = worker.getMainHandItem();

        if (getDrops)
        {
            //calculate fortune enchantment
            final int fortune = ItemStackUtils.getFortuneOf(tool);

            //create list for all item drops to be stored in
            List<ItemStack> localItems = new ArrayList<>();

            //Checks to see if the equipped tool has Silk Touch AND if the blocktoMine has a viable Item SilkTouch can get.
            if (!tool.isEmpty() && shouldSilkTouchBlock(curBlockState))
            {
                final ItemStack fakeTool = tool.copy();
                fakeTool.enchant(Enchantments.SILK_TOUCH, 1);
                localItems.addAll(BlockPosUtil.getBlockDrops(world, blockToMine, fortune, fakeTool, worker));
            }
            //If Silk Touch doesn't work, get blocks with Fortune value as normal.
            else
            {
                localItems.addAll(BlockPosUtil.getBlockDrops(world, blockToMine, fortune, tool, worker));
            }

            localItems = increaseBlockDrops(localItems);

            //add the drops to the citizen
            for (final ItemStack item : localItems)
            {
                InventoryUtils.transferItemStackIntoNextBestSlotInItemHandler(item, worker.getInventoryCitizen());
            }
        }

        triggerMinedBlock(curBlockState);

        if (blockBreakAction == null)
        {
            //Break the block
            worker.getCitizenItemHandler().breakBlockWithToolInHand(blockToMine);
        }
        else
        {
            blockBreakAction.run();
        }

        if (tool != ItemStack.EMPTY && damageTool)
        {
            tool.getItem().inventoryTick(tool, world, worker, worker.getCitizenInventoryHandler().findFirstSlotInInventoryWith(tool.getItem()), true);
        }
        worker.getCitizenExperienceHandler().addExperience(XP_PER_BLOCK);
        this.incrementActionsDone();
        return true;
    }

    /**
     * 检查是否应该使用丝触探针采集此特定方块。
     * @param curBlockState 要检查的方块状态。
     * @return 如果是，则返回true。
     */
    public boolean shouldSilkTouchBlock(final BlockState curBlockState)
    {
        return false;
    }

    /**
     * 可能增加方块掉落物品。将由工作者重写。
     *
     * @param drops 掉落物品列表。
     * @return 附加掉落物品列表。
     */
    protected List<ItemStack> increaseBlockDrops(final List<ItemStack> drops)
    {
        return drops;
    }

    /**
     * 如果矿工希望针对每个被挖掘的方块执行特定操作，触发器。
     *
     * @param blockToMine 被挖掘的方块。
     */
    protected void triggerMinedBlock(@NotNull final BlockState blockToMine)
    {

    }

    /**
     * 检查正确的工具并等待适当的延迟。
     *
     * @param blockToMine 最终要挖掘的方块
     * @param safeStand   安全的站立位置（空方块！）
     * @return 如果应该等待，则返回 true
     */
    private boolean checkMiningLocation(@NotNull final BlockPos blockToMine, @NotNull final BlockPos safeStand)
    {
        final BlockState curBlock = world.getBlockState(blockToMine);

        if (!holdEfficientTool(curBlock, blockToMine))
        {
            //We are missing a tool to harvest this block...
            return true;
        }

        if (walkToBlock(safeStand) && MathUtils.twoDimDistance(worker.blockPosition(), safeStand) > MIN_WORKING_RANGE)
        {
            return true;
        }
        currentWorkingLocation = blockToMine;

        return hasNotDelayed(getBlockMiningDelay(curBlock, blockToMine));
    }

    /**
     * 计算挖掘这个方块所需的时间。
     *
     * @param state 方块状态
     * @param pos   坐标
     * @return 以游戏刻为单位的延迟
     */
    public int getBlockMiningDelay(@NotNull final BlockState state, @NotNull final BlockPos pos)
    {
        if (worker.getMainHandItem() == null)
        {
            return (int) state.getDestroySpeed(world, pos);
        }

        return MineColonies.getConfig().getServer().pvp_mode.get()
                 ? MineColonies.getConfig().getServer().blockMiningDelayModifier.get() / 2
                 : calculateWorkerMiningDelay(state, pos);
    }

    /**
     * 计算在特定位置的方块的工作者挖掘延迟。
     *
     * @param state 方块状态。
     * @param pos   位置。
     * @return 工作者的挖掘延迟。
     */
    private int calculateWorkerMiningDelay(@NotNull final BlockState state, @NotNull final BlockPos pos)
    {
        final double reduction = 1 - worker.getCitizenColonyHandler().getColony().getResearchManager().getResearchEffects().getEffectStrength(BLOCK_BREAK_SPEED);

        return (int) (((MineColonies.getConfig().getServer().blockMiningDelayModifier.get() * Math.pow(LEVEL_MODIFIER, getBreakSpeedLevel() / 2.0))
                         * (double) world.getBlockState(pos).getDestroySpeed(world, pos) / (double) (worker.getMainHandItem()
                                                                                                        .getItem()
                                                                                                        .getDestroySpeed(worker.getMainHandItem(), state)))
                        * reduction);
    }

    /**
     * 获取影响破坏速度的等级。
     * @return 等级。
     */
    public int getBreakSpeedLevel()
    {
        return getPrimarySkillLevel();
    }

    /**
     * 填充要收集的物品位置列表。
     */
    public void fillItemsList()
    {
        searchForItems(worker.getBoundingBox()
                         .expandTowards(RANGE_HORIZONTAL_PICKUP, RANGE_VERTICAL_PICKUP, RANGE_HORIZONTAL_PICKUP)
                         .expandTowards(-RANGE_HORIZONTAL_PICKUP, -RANGE_VERTICAL_PICKUP, -RANGE_HORIZONTAL_PICKUP));
    }

    /**
     * 搜索工人周围的所有物品，并将它们存储在物品列表中。
     *
     * @param boundingBox 搜索的区域。
     */
    public void searchForItems(final AABB boundingBox)
    {
        items = world.getEntitiesOfClass(ItemEntity.class, boundingBox)
                  .stream()
                  .filter(item -> item != null && item.isAlive() &&
                                    (!item.getPersistentData().getAllKeys().contains("PreventRemoteMovement") || !item.getPersistentData().getBoolean("PreventRemoteMovement")))
                  .map(BlockPosUtil::fromEntity)
                  .collect(Collectors.toList());
    }

    /**
     * 通过步行到达一个物品来收集它。
     */
    public void gatherItems()
    {
        worker.setCanPickUpLoot(true);
        if (worker.getNavigation().isDone() || worker.getNavigation().getPath() == null)
        {
            final BlockPos pos = getAndRemoveClosestItemPosition();
            worker.isWorkerAtSiteWithMove(pos, ITEM_PICKUP_RANGE);
            return;
        }

        final int currentIndex = worker.getNavigation().getPath().getNextNodeIndex();
        //We moved a bit, not stuck
        if (currentIndex != previousIndex)
        {
            stillTicks = 0;
            previousIndex = currentIndex;
            return;
        }

        stillTicks++;
        //Stuck for too long
        if (stillTicks > STUCK_WAIT_TICKS)
        {
            //Skip this item
            worker.getNavigation().stop();
            if (items != null && !items.isEmpty())
            {
                items.remove(0);
            }
        }
    }

    /**
     * 寻找最接近的物品并从列表中移除它。
     *
     * @return 最接近的物品
     */
    private BlockPos getAndRemoveClosestItemPosition()
    {
        int index = 0;
        double distance = Double.MAX_VALUE;

        for (int i = 0; i < items.size(); i++)
        {
            final double tempDistance = items.get(i).distSqr(worker.blockPosition());
            if (tempDistance < distance)
            {
                index = i;
                distance = tempDistance;
            }
        }

        return items.remove(index);
    }

    /**
     * 在市民周围搜索一个随机位置，以市民为锚点。
     * @param range 最大范围
     * @return 在找到位置之前返回 null。
     */
    protected BlockPos findRandomPositionToWalkTo(final int range)
    {
        return findRandomPositionToWalkTo(range, worker.blockPosition());
    }

    /**
     * 寻找一个随机位置进行移动。
     *
     * @param range 最大范围
     * @param pos 我们想在给定范围内找到周围随机位置的位置
     * @return 直到找到位置前返回null。
     */
    protected BlockPos findRandomPositionToWalkTo(final int range, final BlockPos pos)
    {
        if (pathResult == null)
        {
            pathBackupFactor = 1;
            pathResult = getRandomNavigationPath(range, pos);
        }
        else if ( pathResult.failedToReachDestination())
        {
            pathBackupFactor++;
            pathResult = getRandomNavigationPath(range * pathBackupFactor, pos);
        }

        if (pathResult.isPathReachingDestination())
        {
            final BlockPos resultPos = pathResult.getPath().getEndNode().asBlockPos();
            pathResult = null;
            return resultPos;
        }

        if (pathResult.isCancelled())
        {
            pathResult = null;
            return null;
        }

        if (pathBackupFactor > 10)
        {
            pathResult = null;
            return null;
        }

        return null;
    }

    /**
     * 获取一个用于查找特定位置的导航器。
     *
     * @param range 最大范围。
     * @param pos 要查找的位置
     * @return 导航器。
     */
    protected PathResult getRandomNavigationPath(final int range, final BlockPos pos)
    {
        if (pos == null || pos == worker.blockPosition())
        {
            return worker.getNavigation().moveToRandomPos(range, 1.0D);
        }
        else
        {
            return worker.getNavigation().moveToRandomPosAroundX(range, 1.0D, pos);
        }
    }

    /**
     * 重置收集物品为null。
     */
    public void resetGatheringItems()
    {
        items = null;
    }

    /**
     * 获取要采集的物品列表。
     *
     * @return 其副本。
     */
    @Nullable
    public List<BlockPos> getItemsForPickUp()
    {
        return items == null ? null : new ArrayList<>(items);
    }
}
