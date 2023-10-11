package com.minecolonies.coremod.tileentities;

import com.minecolonies.api.inventory.InventoryCitizen;
import com.minecolonies.api.tileentities.AbstractTileEntityRack;
import com.minecolonies.api.tileentities.AbstractTileEntityWareHouse;
import com.minecolonies.api.tileentities.MinecoloniesTileEntities;
import com.minecolonies.api.tileentities.TileEntityRack;
import com.minecolonies.api.util.*;
import com.minecolonies.coremod.colony.buildings.modules.WarehouseModule;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import static com.minecolonies.api.util.constant.Constants.TICKS_FIVE_MIN;
import static com.minecolonies.api.util.constant.TranslationConstants.*;
import static com.minecolonies.coremod.colony.buildings.workerbuildings.BuildingWareHouse.MAX_STORAGE_UPGRADE;
import static net.minecraftforge.items.CapabilityItemHandler.ITEM_HANDLER_CAPABILITY;

/**
 * 处理我们殖民地仓库的瓦砾实体的类。
 */
public class TileEntityWareHouse extends AbstractTileEntityWareHouse
{
    /**
     * 上次发送通知的时间。
     */
    private long lastNotification                   = 0;

    public TileEntityWareHouse(final BlockPos pos, final BlockState state)
    {
        super(MinecoloniesTileEntities.WAREHOUSE.get(), pos, state);
        inWarehouse = true;
    }

    @Override
    public boolean hasMatchingItemStackInWarehouse(@NotNull final Predicate<ItemStack> itemStackSelectionPredicate, int count)
    {
        final List<Tuple<ItemStack, BlockPos>> targetStacks = getMatchingItemStacksInWarehouse(itemStackSelectionPredicate);
        return targetStacks.stream().mapToInt(tuple -> ItemStackUtils.getSize(tuple.getA())).sum() >= count;
    }

    @Override
    public boolean hasMatchingItemStackInWarehouse(@NotNull final ItemStack itemStack, final int count, final boolean ignoreNBT)
    {
        return hasMatchingItemStackInWarehouse(itemStack, count, ignoreNBT, 0);
    }

    @Override
    public boolean hasMatchingItemStackInWarehouse(@NotNull final ItemStack itemStack, final int count, final boolean ignoreNBT, final boolean ignoreDamage, final int leftOver)
    {
        int totalCountFound = 0 - leftOver;
        for (@NotNull final BlockPos pos : getBuilding().getContainers())
        {
            if (WorldUtil.isBlockLoaded(level, pos))
            {
                final BlockEntity entity = getLevel().getBlockEntity(pos);
                if (entity instanceof TileEntityRack && !((AbstractTileEntityRack) entity).isEmpty())
                {
                    totalCountFound += ((AbstractTileEntityRack) entity).getCount(itemStack, ignoreDamage, ignoreNBT);
                    if (totalCountFound >= count)
                    {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public boolean hasMatchingItemStackInWarehouse(@NotNull final ItemStack itemStack, final int count, final boolean ignoreNBT, final int leftOver)
    {
        return hasMatchingItemStackInWarehouse(itemStack, count, ignoreNBT, true, leftOver);
    }

    @Override
    @NotNull
    public List<Tuple<ItemStack, BlockPos>> getMatchingItemStacksInWarehouse(@NotNull final Predicate<ItemStack> itemStackSelectionPredicate)
    {
        List<Tuple<ItemStack, BlockPos>> found = new ArrayList<>();
        
        if (getBuilding() != null)
        {
            for (@NotNull final BlockPos pos : getBuilding().getContainers())
            {
                final BlockEntity entity = getLevel().getBlockEntity(pos);
                if (entity instanceof TileEntityRack && !((AbstractTileEntityRack) entity).isEmpty() && ((AbstractTileEntityRack) entity).getItemCount(itemStackSelectionPredicate) > 0)
                {
                    final TileEntityRack rack = (TileEntityRack) entity;
                    for (final ItemStack stack : (InventoryUtils.filterItemHandler(rack.getInventory(), itemStackSelectionPredicate)))
                    {
                        found.add(new Tuple<>(stack, pos));
                    }
                }

                if (entity instanceof ChestBlockEntity && InventoryUtils.hasItemInItemHandler(entity.getCapability(ITEM_HANDLER_CAPABILITY, null).orElseGet(null), itemStackSelectionPredicate))
                {
                    for (final ItemStack stack : InventoryUtils.filterItemHandler(entity.getCapability(ITEM_HANDLER_CAPABILITY, null).orElseGet(null), itemStackSelectionPredicate))
                    {
                        found.add(new Tuple<>(stack, pos));
                    }
                }
            }
        }
        return found;
    }

    @Override
    public void dumpInventoryIntoWareHouse(@NotNull final InventoryCitizen inventoryCitizen)
    {
        for (int i = 0; i < inventoryCitizen.getSlots(); i++)
        {
            final ItemStack stack = inventoryCitizen.getStackInSlot(i);
            if (ItemStackUtils.isEmpty(stack))
            {
                continue;
            }

            @Nullable final BlockEntity chest = getRackForStack(stack);
            if (chest == null)
            {
                if(level.getGameTime() - lastNotification > TICKS_FIVE_MIN)
                {
                    lastNotification = level.getGameTime();
                    if (getBuilding().getBuildingLevel() == getBuilding().getMaxBuildingLevel())
                    {
                        if (getBuilding().getFirstModuleOccurance(WarehouseModule.class).getStorageUpgrade() < MAX_STORAGE_UPGRADE)
                        {
                            MessageUtils.format(COM_MINECOLONIES_COREMOD_WAREHOUSE_FULL_LEVEL5_UPGRADE).sendTo(getColony()).forAllPlayers();
                        }
                        else
                        {
                            MessageUtils.format(COM_MINECOLONIES_COREMOD_WAREHOUSE_FULL_MAX_UPGRADE).sendTo(getColony()).forAllPlayers();
                        }
                    }
                    else
                    {
                        MessageUtils.format(COM_MINECOLONIES_COREMOD_WAREHOUSE_FULL).sendTo(getColony()).forAllPlayers();
                    }
                }
                return;
            }

            final int index = i;
            chest.getCapability(ITEM_HANDLER_CAPABILITY, null).ifPresent(handler -> InventoryUtils.transferItemStackIntoNextBestSlotInItemHandler(inventoryCitizen, index, handler));
        }
    }

    /**
     * 获取适合堆叠的瓦砾。
     * @param stack 要插入的堆叠。
     * @return 匹配的瓦砾。
     */
    public BlockEntity getRackForStack(final ItemStack stack)
    {
        BlockEntity rack = getPositionOfChestWithItemStack(stack);
        if (rack == null)
        {
            rack = getPositionOfChestWithSimilarItemStack(stack);
            if (rack == null)
            {
                rack = searchMostEmptyRack();
            }
        }
        return rack;
    }

    /**
     * 搜索带有堆叠物品的正确箱子。
     *
     * @param stack 要倾倒的堆叠物品。
     * @return 箱子的瓦砾实体。
     */
    @Nullable
    private BlockEntity getPositionOfChestWithItemStack(@NotNull final ItemStack stack)
    {
        for (@NotNull final BlockPos pos : getBuilding().getContainers())
        {
            if (WorldUtil.isBlockLoaded(level, pos))
            {
                final BlockEntity entity = getLevel().getBlockEntity(pos);
                if (entity instanceof AbstractTileEntityRack)
                {
                    if (((AbstractTileEntityRack) entity).getFreeSlots() > 0 && ((AbstractTileEntityRack) entity).hasItemStack(stack, 1, true))
                    {
                        return entity;
                    }
                }
            }
        }

        return null;
    }

    /**
     * 搜索具有类似物品堆叠的箱子。
     *
     * @param stack 堆叠物品。
     * @return 箱子的瓦砾实体。
     */
    @Nullable
    private BlockEntity getPositionOfChestWithSimilarItemStack(final ItemStack stack)
    {
        for (@NotNull final BlockPos pos : getBuilding().getContainers())
        {
            if (WorldUtil.isBlockLoaded(level, pos))
            {
                final BlockEntity entity = getLevel().getBlockEntity(pos);
                if (entity instanceof AbstractTileEntityRack)
                {
                    if (((AbstractTileEntityRack) entity).getFreeSlots() > 0 && ((AbstractTileEntityRack) entity).hasSimilarStack(stack))
                    {
                        return entity;
                    }
                }
            }
        }
        return null;
    }

    /**
     * 搜索物品最少的箱子。
     *
     * @return 该箱子的瓦砾实体。
     */
    @Nullable
    private BlockEntity searchMostEmptyRack()
    {
        int freeSlots = 0;
        BlockEntity emptiestChest = null;
        for (@NotNull final BlockPos pos : getBuilding().getContainers())
        {
            final BlockEntity entity = getLevel().getBlockEntity(pos);
            if (entity instanceof TileEntityRack)
            {
                if (((AbstractTileEntityRack) entity).isEmpty())
                {
                    return entity;
                }

                final int tempFreeSlots = ((AbstractTileEntityRack) entity).getFreeSlots();
                if (tempFreeSlots > freeSlots)
                {
                    freeSlots = tempFreeSlots;
                    emptiestChest = entity;
                }
            }
        }
        return emptiestChest;
    }
}
