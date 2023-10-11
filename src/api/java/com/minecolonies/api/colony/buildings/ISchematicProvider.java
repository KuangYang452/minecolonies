package com.minecolonies.api.colony.buildings;

import com.ldtteam.structurize.blocks.interfaces.IBlueprintDataProvider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Tuple;
import net.minecraft.core.BlockPos;
import net.minecraftforge.common.util.INBTSerializable;

import java.util.Set;
public interface ISchematicProvider extends INBTSerializable<CompoundTag> {
    /**
     * 返回当前对象的 {@code BlockPos}，也用作ID。
     *
     * @return 当前对象的 {@code BlockPos}。
     */
    BlockPos getPosition();

    /**
     * 根据示意图设置建筑的角落。
     *
     * @param corner1 第一个角落。
     * @param corner2 第二个角落。
     */
    void setCorners(final BlockPos corner1, final BlockPos corner2);

    /**
     * 获取基于示意图的建筑的所有角落。
     * 这是最低角落 (x, y, z) 和最高角落 (x, y, z)。
     *
     * @return 角落的元组。
     */
    Tuple<BlockPos, BlockPos> getCorners();

    /**
     * 返回当前对象的 {@code BlockPos}，也用作ID。
     *
     * @return 当前对象的 {@code BlockPos}。
     */
    BlockPos getID();

    /**
     * 获取父建筑的位置。
     *
     * @return 父建筑的位置。
     */
    BlockPos getParent();

    /**
     * 是否有父建筑。
     *
     * @return 如果有父建筑则返回true。
     */
    boolean hasParent();

    /**
     * 设置父建筑的位置。
     * @param pos
     */
    void setParent(BlockPos pos);

    /**
     * 获取子建筑的位置集合。
     * @return
     */
    Set<BlockPos> getChildren();

    /**
     * 添加一个子建筑的位置。
     * @param pos
     */
    void addChild(BlockPos pos);

    /**
     * 移除一个子建筑的位置。
     * @param pos
     */
    void removeChild(BlockPos pos);

    /**
     * 返回当前建筑的旋转值。
     *
     * @return 旋转值的整数表示。
     */
    int getRotation();

    /**
     * 返回当前建筑的风格。
     *
     * @return 当前建筑风格的字符串表示。
     */
    String getStyle();

    /**
     * 设置建筑的风格。
     *
     * @param style 风格的字符串值。
     */
    void setStyle(String style);

    /**
     * 返回当前对象的级别。
     *
     * @return 当前对象的级别。
     */
    int getBuildingLevel();

    /**
     * 设置建筑的当前级别。
     *
     * @param level 建筑的级别。
     */
    void setBuildingLevel(int level);

    /**
     * 返回实例是否被标记为脏的状态。
     *
     * @return 如果脏则返回true，否则返回false。
     */
    boolean isDirty();

    /**
     * 将 {@code #dirty} 设置为false，表示实例已经是最新的。
     */
    void clearDirty();

    /**
     * 标记实例和建筑为脏的状态。
     */
    void markDirty();

    /**
     * 设置当前建筑是否镜像。
     */
    void setIsMirrored(final boolean isMirrored);

    /**
     * 返回当前建筑是否镜像。
     *
     * @return 镜像的布尔值。
     */
    boolean isMirrored();

    /**
     * 子类必须返回它们的结构名称。
     *
     * @return StructureProxy 的名称。
     */
    String getSchematicName();

    /**
     * 子类必须返回它们的最大建筑级别。
     *
     * @return 最大建筑级别。
     */
    int getMaxBuildingLevel();

    /**
     * 检查建筑是否已被拆解。
     *
     * @return 如果已拆解则返回true。
     */
    boolean isDeconstructed();

    /**
     * 设置建筑为已拆解状态。
     */
    void setDeconstructed();

    /**
     * 当旧示意图更新到新示意图时调用。
     *
     * @param oldSchematic 旧示意图
     * @param newSchematic 新示意图
     * @param blueprintDataProvider 蓝图数据提供者
     */
    void onUpgradeSchematicTo(final String oldSchematic, final String newSchematic, final IBlueprintDataProvider blueprintDataProvider);
}
