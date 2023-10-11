package com.minecolonies.coremod.colony.workorders;

import com.ldtteam.structurize.management.StructureName;
import com.ldtteam.structurize.util.PlacementSettings;
import com.minecolonies.api.advancements.AdvancementTriggers;
import com.minecolonies.api.colony.ICitizenData;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.IColonyTagCapability;
import com.minecolonies.api.colony.workorders.IWorkManager;
import com.minecolonies.api.colony.workorders.IWorkOrder;
import com.minecolonies.api.util.LoadOnlyStructureHandler;
import com.minecolonies.api.util.Log;
import com.minecolonies.api.util.MessageUtils;
import com.minecolonies.coremod.colony.Colony;
import com.minecolonies.coremod.util.AdvancementUtils;
import com.minecolonies.coremod.util.ColonyUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.Tuple;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.minecolonies.api.util.constant.TranslationConstants.OUT_OF_COLONY;
import static com.minecolonies.api.colony.IColony.CLOSE_COLONY_CAP;

/**
 * 处理殖民地的工单。
 */
public class WorkManager implements IWorkManager
{
    private static final String                   TAG_WORK_ORDERS = "workOrders";
    // 每秒一次
    //private static final int    WORK_ORDER_FULFILL_INCREMENT = 1 * 20;
    /**
     * 工单所属的殖民地。
     */
    private final        Colony                   colony;
    @NotNull
    private final        Map<Integer, IWorkOrder> workOrders      = new LinkedHashMap<>();
    private              int                      topWorkOrderId  = 0;
    /**
     * 检查是否有更改。
     */
    private              boolean                  dirty           = false;

    /**
     * 构造函数，保存对殖民地的引用。
     *
     * @param c 工单管理器所属的殖民地。
     */
    public WorkManager(final Colony c)
    {
        colony = c;
    }

    /**
     * 从工单管理器中移除一个工单。
     *
     * @param order 要移除的工单 {@link IWorkOrder}。
     */
    @Override
    public void removeWorkOrder(@NotNull final IWorkOrder order)
    {
        removeWorkOrder(order.getID());
    }

    /**
     * 从工单管理器中移除一个工单。
     *
     * @param orderId 要移除的工单的ID。
     */
    @Override
    public void removeWorkOrder(final int orderId)
    {
        final IWorkOrder workOrder = workOrders.get(orderId);
        if (workOrder != null)
        {
            workOrders.remove(orderId);
            colony.removeWorkOrderInView(orderId);
            workOrder.onRemoved(colony);
            colony.markDirty();
        }
    }

    /**
     * 根据指定的ID和类型获取工单。
     *
     * @param id   工单的ID。
     * @param type 期望的工单类型的类。
     * @param <W>  要返回的工单类型。
     * @return 指定ID的工单，如果未找到或类型不兼容，则返回null。
     */
    @Override
    @Nullable
    public <W extends IWorkOrder> W getWorkOrder(final int id, @NotNull final Class<W> type)
    {
        final IWorkOrder workOrder = getWorkOrder(id);
        if (type.isInstance(workOrder))
        {
            return type.cast(workOrder);
        }

        return null;
    }

    /**
     * 根据ID获取工单。
     *
     * @param id 工单的ID。
     * @return 指定ID的工单，如果不存在则返回null。
     */
    @Override
    public IWorkOrder getWorkOrder(final int id)
    {
        return workOrders.get(id);
    }

    /**
     * 获取指定类型的未分配工单。
     *
     * @param type 要查找的工单类型的类。
     * @param <W>  要返回的工单类型。
     * @return 未分配的给定类型的工单，如果未找到则返回null。
     */
    @Override
    @Nullable
    public <W extends IWorkOrder> W getUnassignedWorkOrder(@NotNull final Class<W> type)
    {
        for (@NotNull final IWorkOrder o : workOrders.values())
        {
            if (!o.isClaimed() && type.isInstance(o))
            {
                return type.cast(o);
            }
        }

        return null;
    }

    /**
     * 获取指定类型的所有工单。
     *
     * @param type 要查找的工单类型的类。
     * @param <W>  要返回的工单类型。
     * @return 给定类型的所有工单的列表。
     */
    @Override
    public <W extends IWorkOrder> List<W> getWorkOrdersOfType(@NotNull final Class<W> type)
    {
        return workOrders.values().stream().filter(type::isInstance).map(type::cast).collect(Collectors.toList());
    }

    /**
     * 获取所有工单。
     *
     * @return 所有工单的列表。
     */
    @Override
    @NotNull
    public Map<Integer, IWorkOrder> getWorkOrders()
    {
        return workOrders;
    }

    /**
     * 当移除一个市民时，取消该市民领取的工单。
     *
     * @param citizen 要取消领取工单的市民。
     */
    @Override
    public void clearWorkForCitizen(@NotNull final ICitizenData citizen)
    {
        dirty = true;
        workOrders.values().stream().filter(o -> o != null && o.isClaimedBy(citizen)).forEach(IWorkOrder::clearClaimedBy);
    }

    /**
     * 保存工单管理器。
     *
     * @param compound 要保存到的CompoundTag。
     */
    @Override
    public void write(@NotNull final CompoundTag compound)
    {
        // 工单列表
        @NotNull final ListTag list = new ListTag();
        for (@NotNull final IWorkOrder o : workOrders.values())
        {
            @NotNull final CompoundTag orderCompound = new CompoundTag();
            o.write(orderCompound);
            list.add(orderCompound);
        }
        compound.put(TAG_WORK_ORDERS, list);
    }

    /**
     * 恢复工单管理器。
     *
     * @param compound 要从中读取的CompoundTag。
     */
    @Override
    public void read(@NotNull final CompoundTag compound)
    {
        workOrders.clear();
        // 工单列表
        final ListTag list = compound.getList(TAG_WORK_ORDERS, Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); ++i)
        {
            final CompoundTag orderCompound = list.getCompound(i);
            @Nullable final IWorkOrder o = AbstractWorkOrder.createFromNBT(orderCompound, this);
            if (o != null)
            {
                addWorkOrder(o, true);

                // 如果这个工单已被领取，并且领取它的市民不存在了
                // 则清除领取状态
                // 这只是一种异常情况的清理措施；在正常情况下不应该发生这种情况
                if (o.isClaimed() && colony.getBuildingManager().getBuilding(o.getClaimedBy()) == null)
                {
                    o.clearClaimedBy();
                }

                topWorkOrderId = Math.max(topWorkOrderId, o.getID());
            }
        }
    }

    /**
     * 添加工单到工单管理器。
     *
     * @param order          要添加的工单。
     * @param readingFromNbt 如果从NBT中读取。
     */
    @Override
    public void addWorkOrder(@NotNull final IWorkOrder order, final boolean readingFromNbt)
    {
        dirty = true;

        if (!(order instanceof WorkOrderMiner))
        {
            for (final IWorkOrder or : workOrders.values())
            {
                if (or.getLocation().equals(order.getLocation()) && or.getStructureName().equals(order.getStructureName()))
                {
                    Log.getLogger().warn("避免添加重复的工单");
                    removeWorkOrder(or);
                    break;
                }
            }
            if (!readingFromNbt && !isWorkOrderWithinColony(order))
            {
                MessageUtils.format(OUT_OF_COLONY, order.getDisplayName(), order.getLocation().getX(), order.getLocation().getZ()).sendTo(colony).forAllPlayers();
                return;
            }
        }

        if (order.getID() == 0)
        {
            topWorkOrderId++;
            order.setID(topWorkOrderId);
        }

        if (!readingFromNbt)
        {
            final StructureName structureName = new StructureName(order.getStructureName());
            if (order instanceof WorkOrderBuilding)
            {
                final int level = order.getTargetLevel();
                AdvancementUtils.TriggerAdvancementPlayersForColony(colony, player ->
                                                                              AdvancementTriggers.CREATE_BUILD_REQUEST.trigger(player, structureName, level));
            }
            else if (order instanceof WorkOrderDecoration)
            {
                AdvancementUtils.TriggerAdvancementPlayersForColony(colony, player ->
                                                                              AdvancementTriggers.CREATE_BUILD_REQUEST.trigger(player, structureName, 0));
            }
        }

        workOrders.put(order.getID(), order);
        order.onAdded(colony, readingFromNbt);
    }

    /**
     * 检查工单是否在殖民地内。
     *
     * @param order 要检查的工单。
     * @return 如果在殖民地内则返回true。
     */
    private boolean isWorkOrderWithinColony(final IWorkOrder order)
    {
        final Level world = colony.getWorld();
        final Tuple<BlockPos, BlockPos> corners
          = ColonyUtils.calculateCorners(order.getLocation(),
          world,
          new LoadOnlyStructureHandler(world, order.getLocation(), order.getStructureName(), new PlacementSettings(), true).getBluePrint(),
          order.getRotation(),
          order.isMirrored());

        Set<ChunkPos> chunks = new HashSet<>();
        final int minX = Math.min(corners.getA().getX(), corners.getB().getX()) + 1;
        final int maxX = Math.max(corners.getA().getX(), corners.getB().getX());

        final int minZ = Math.min(corners.getA().getZ(), corners.getB().getZ()) + 1;
        final int maxZ = Math.max(corners.getA().getZ(), corners.getB().getZ());

        for (int x = minX; x < maxX; x += 16)
        {
            for (int z = minZ; z < maxZ; z += 16)
            {
                final int chunkX = x >> 4;
                final int chunkZ = z >> 4;
                final ChunkPos pos = new ChunkPos(chunkX, chunkZ);
                if (!chunks.contains(pos))
                {
                    chunks.add(pos);
                    final IColonyTagCapability colonyCap = world.getChunk(pos.x, pos.z).getCapability(CLOSE_COLONY_CAP, null).orElseGet(null);
                    if (colonyCap == null || colonyCap.getOwningColony() != colony.getID())
                    {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    /**
     * 在殖民地刻度时处理更新。目前，只做周期性的工单清理。
     *
     * @param colony 正在刻度的殖民地。
     */
    @Override
    public void onColonyTick(@NotNull final IColony colony)
    {
        @NotNull final Iterator<IWorkOrder> iter = workOrders.values().iterator();
        while (iter.hasNext())
        {
            final IWorkOrder o = iter.next();
            if (!o.isValid(this.colony))
            {
                iter.remove();
                dirty = true;
            }
            else if (o.isDirty())
            {
                dirty = true;
                o.resetChange();
            }
        }
    }

    /**
     * 获取工单按优先级排序的列表。
     *
     * @param builder 希望领取工单的建筑位置。
     * @param type    需要的工单类型。
     * @param <W>     类型。
     * @return 工单列表。
     */
    @Override
    public <W extends IWorkOrder> List<W> getOrderedList(Class<W> type, BlockPos builder)
    {
        return getOrderedList(type::isInstance, builder)
          .stream()
          .map(m -> (W) m)
          .collect(Collectors.toList());
    }

    /**
     * 获取工单按优先级排序的列表。
     *
     * @param builder   希望领取工单的建筑位置。
     * @param predicate 一个检查每个工单的谓词。
     * @return 工单列表。
     */
    @Override
    public List<IWorkOrder> getOrderedList(@NotNull Predicate<IWorkOrder> predicate, final BlockPos builder)
    {
        return workOrders.values().stream()
          .filter(o -> (!o.isClaimed() || o.getClaimedBy().equals(builder)))
          .filter(predicate)
          .sorted(Comparator.comparingInt(IWorkOrder::getPriority).reversed())
          .collect(Collectors.toList());
    }

    /**
     * 检查是否有更改。
     *
     * @return 如果有更改则返回true。
     */
    @Override
    public boolean isDirty()
    {
        return dirty;
    }

    /**
     * 设置是否有更改。
     *
     * @param dirty 如果有更改则为true，否则为false。
     */
    @Override
    public void setDirty(final boolean dirty)
    {
        this.dirty = dirty;
    }

    @Override
    public IColony getColony()
    {
        return colony;
    }
}
