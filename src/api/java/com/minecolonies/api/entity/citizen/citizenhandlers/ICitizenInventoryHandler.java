package com.minecolonies.api.entity.citizen.citizenhandlers;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.item.Item;

public interface ICitizenInventoryHandler
{
    /**
     * 返回包含特定物品的库存中的第一个槽位。
     *
     * @param targetItem 物品。
     * @return 槽位。
     */
    int findFirstSlotInInventoryWith(Item targetItem);

    /**
     * 返回包含特定方块的库存中的第一个槽位。
     *
     * @param block 方块。
     * @return 槽位。
     */
    int findFirstSlotInInventoryWith(Block block);

    /**
     * 返回库存中特定方块的数量。
     *
     * @param block 方块。
     * @return 数量。
     */
    int getItemCountInInventory(Block block);

    /**
     * 返回库存中特定物品的数量。
     *
     * @param targetItem 物品。
     * @return 数量。
     */
    int getItemCountInInventory(Item targetItem);

    /**
     * 检查市民库存中是否有特定方块。
     *
     * @param block 方块。
     * @return 如果有则为true。
     */
    boolean hasItemInInventory(Block block);

    /**
     * 检查市民库存中是否有特定物品。
     *
     * @param item 物品。
     * @return 如果有则为true。
     */
    boolean hasItemInInventory(Item item);

    /**
     * 检查库存是否已满。
     *
     * @return 如果库存已满则为true。
     */
    boolean isInventoryFull();
}
