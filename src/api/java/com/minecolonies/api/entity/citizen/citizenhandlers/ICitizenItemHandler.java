package com.minecolonies.api.entity.citizen.citizenhandlers;

import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionHand;
import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface ICitizenItemHandler
{
    /**
     * 市民将尝试拾取特定物品。
     *
     * @param ItemEntity 他想要拾取的物品。
     */
    void tryPickupItemEntity(@NotNull ItemEntity ItemEntity);

    /**
     * 移除当前持有的物品。
     */
    void removeHeldItem();

    /**
     * 设置当前持有的物品。
     *
     * @param hand 手的位置
     * @param slot 来自背包的槽位。
     */
    void setHeldItem(InteractionHand hand, int slot);

    /**
     * 设置主手当前持有的物品。
     *
     * @param slot 来自背包的槽位。
     */
    void setMainHeldItem(int slot);

    /**
     * 摆动实体手臂，创建声音和粒子效果。
     * <p>
     * 不会破坏方块。
     *
     * @param blockPos 方块位置。
     */
    void hitBlockWithToolInHand(@Nullable BlockPos blockPos);

    /**
     * 摆动实体手臂，创建声音和粒子效果。
     * <p>
     * 如果breakBlock为true，则会破坏方块（不同的声音和粒子效果），并损坏市民手中的工具。
     *
     * @param blockPos   方块位置。
     * @param breakBlock 是否要破坏此方块。
     */
    void hitBlockWithToolInHand(@Nullable BlockPos blockPos, boolean breakBlock);

    /**
     * 损坏当前持有的物品。
     *
     * @param hand   物品所在的手。
     * @param damage 损坏的数量。
     */
    void damageItemInHand(InteractionHand hand, int damage);

    /**
     * 拾取市民周围范围内的所有物品。
     */
    void pickupItems();

    /**
     * 摆动实体手臂，创建声音和粒子效果。
     * <p>
     * 这将破坏方块（不同的声音和粒子效果），并损坏市民手中的工具。
     *
     * @param blockPos 方块位置。
     */
    void breakBlockWithToolInHand(@Nullable BlockPos blockPos);

    /**
     * 处理实体掉落物品。
     *
     * @param itemstack 要掉落的物品。
     * @return 掉落的物品实体。
     */
    ItemEntity entityDropItem(@NotNull ItemStack itemstack);

    /**
     * 在受到打击后更新装甲损伤。
     *
     * @param damage 造成的伤害。
     */
    void updateArmorDamage(double damage);

    /**
     * 对装甲进行修复。
     *
     * @param localXp 要添加的经验值。
     * @return 剩余的经验值。
     */
    double applyMending(final double localXp);
}
