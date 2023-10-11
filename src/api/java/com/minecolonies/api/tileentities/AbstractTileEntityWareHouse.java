package com.minecolonies.api.tileentities;

import com.minecolonies.api.inventory.InventoryCitizen;
import com.minecolonies.api.util.Tuple;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Predicate;

public abstract class AbstractTileEntityWareHouse extends TileEntityColonyBuilding
{
    public AbstractTileEntityWareHouse(final BlockEntityType<? extends AbstractTileEntityWareHouse> warehouse, final BlockPos pos, final BlockState state)
    {
        super(warehouse, pos, state);
    }

    /**
     * 从仓库中获取第一个匹配的ItemStack的方法。
     *
     * @param itemStackSelectionPredicate 用于选择ItemStack的谓词。
     * @return 第一个匹配的ItemStack。
     */
    public abstract boolean hasMatchingItemStackInWarehouse(@NotNull Predicate<ItemStack> itemStackSelectionPredicate, int count);

    /**
     * 用于检查此仓库是否包含所请求的任何itemstacks的方法。
     *
     * @param itemStack 用于检查的堆栈。
     * @param count 最小计数。
     * @param ignoreNBT 是否应忽略nbt值。
     * @return 仓库是否包含堆栈时为true，否则为false。
     */
    public abstract boolean hasMatchingItemStackInWarehouse(@NotNull final ItemStack itemStack, final int count, final boolean ignoreNBT);

    /**
     * 用于检查此仓库是否包含所请求的任何itemstacks的方法。
     *
     * @param itemStack 用于检查的堆栈。
     * @param count 最小计数。
     * @param ignoreNBT 是否应忽略nbt值。
     * @param leftOver 保留在仓库中的剩余物品。
     * @return 仓库是否包含堆栈时为true，否则为false。
     */
    public abstract boolean hasMatchingItemStackInWarehouse(@NotNull final ItemStack itemStack, final int count, final boolean ignoreNBT, final int leftOver);

    /**
     * 用于检查此仓库是否包含所请求的任何itemstacks的方法。
     *
     * @param itemStack 用于检查的堆栈。
     * @param count 最小计数。
     * @param ignoreNBT 是否应忽略nbt值。
     * @param ignoreDamage 是否忽略伤害。
     * @param leftOver 保留在仓库中的剩余物品。
     * @return 仓库是否包含堆栈时为true，否则为false。
     */
    public abstract boolean hasMatchingItemStackInWarehouse(@NotNull final ItemStack itemStack, final int count, final boolean ignoreNBT, final boolean ignoreDamage, final int leftOver);

    /**
     * 用于检查此仓库是否包含所请求的任何itemstacks的方法。
     *
     * @param itemStackSelectionPredicate 用于检查的谓词。
     * @return 仓库是否包含堆栈时为true，否则为false。
     */
    @NotNull
    public abstract List<Tuple<ItemStack, BlockPos>> getMatchingItemStacksInWarehouse(@NotNull Predicate<ItemStack> itemStackSelectionPredicate);

    /**
     * 将市民的库存倒入仓库的方法。遍历所有物品并搜索适当的箱子进行倒入。
     *
     * @param inventoryCitizen 市民的库存
     */
    public abstract void dumpInventoryIntoWareHouse(@NotNull InventoryCitizen inventoryCitizen);
}