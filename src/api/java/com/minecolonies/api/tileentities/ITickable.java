package com.minecolonies.api.tileentities;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 可以进行tick的接口。
 */
public interface ITickable
{
    /**
     * 使用参数来进行tick。
     * @param level 进行tick的世界。
     * @param state 它的状态。
     * @param pos 进行tick的位置。
     */
    default void tick(final Level level, final BlockState state, final BlockPos pos)
    {
        tick();
    }

    /**
     * 默认的无参数tick实现。
     */
    default void tick()
    {

    }
}
