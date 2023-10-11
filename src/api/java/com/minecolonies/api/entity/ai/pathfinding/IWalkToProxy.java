package com.minecolonies.api.entity.ai.pathfinding;

import net.minecraft.world.entity.Mob;
import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;

/**
 * 定义了 walkToProxy 的接口。
 */
public interface IWalkToProxy
{
    /**
     * 通过代理引导实体到特定位置。
     *
     * @param target 目标位置。
     * @param range  范围。
     * @return 如果到达则返回 true。
     */
    boolean walkToBlock(@NotNull final BlockPos target, final int range);

    /**
     * 通过代理将实体引导到特定位置。
     *
     * @param target 目标位置。
     * @param range  范围。
     * @param onMove 实体是否在移动？
     * @return 如果到达目标位置则返回true。
     */
    boolean walkToBlock(@NotNull final BlockPos target, final int range, final boolean onMove);

    /**
     * 根据实体获取路点列表。
     *
     * @return 路点集合。
     */
    Set<BlockPos> getWayPoints();

    /**
     * 检查是否在距离计算中应考虑Y轴级别。
     *
     * @return 如果是，则返回true。
     */
    boolean careAboutY();

    /**
     * 尝试获取到特定目标的专用代理。
     *
     * @param target         目标。
     * @param distanceToPath 到目标的距离。
     * @return 如果存在，则返回特殊代理点，否则返回 null。
     */
    @Nullable
    BlockPos getSpecializedProxy(final BlockPos target, final double distanceToPath);

    /**
     * 获取代理列表的方法。
     *
     * @return 列表的副本
     */
    List<BlockPos> getProxyList();

    /**
     * 向代理列表添加一个条目。
     *
     * @param pos 要添加的位置。
     */
    void addToProxyList(final BlockPos pos);

    /**
     * 用于检测生物实体是否在具有移动的位置。
     *
     * @param entity 要检查的实体。
     * @param x      x 值。
     * @param y      y 值。
     * @param z      z 值。
     * @param range  范围。
     * @return 如果是，则返回 true。
     */
    boolean isLivingAtSiteWithMove(final Mob entity, final int x, final int y, final int z, final int range);

    /**
     * 获取与代理相关联的实体的getter方法。
     *
     * @return 实体。
     */
    Mob getEntity();

    /**
     * 获取当前代理对象的方法。
     *
     * @return 当前代理对象。
     */
    BlockPos getCurrentProxy();

    /**
     * 重置代理的目标。
     */
    void reset();
}
