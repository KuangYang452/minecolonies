package com.minecolonies.api.entity.ai.statemachine;

import com.minecolonies.api.entity.ai.statemachine.states.IAIState;
import com.minecolonies.api.entity.ai.statemachine.tickratestatemachine.TickingTransition;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/**
 * 一个简单的目标，AI 试图完成它。它具有状态匹配器，因此仅在匹配状态时执行。它具有测试函数，以进行更多检查，以确定是否需要执行。它还可以更改状态。
 */
public class AITarget extends TickingTransition<IAIState>
{
    /**
     * 构造一个目标。
     *
     * @param state     需要达到的状态
     * @param predicate 执行的断言
     * @param action    要应用的操作
     * @param tickRate  刷新率
     */
    public AITarget(
      @NotNull final IAIState state,
      @NotNull final BooleanSupplier predicate,
      @NotNull final Supplier<IAIState> action,
      final int tickRate)
    {
        super(state, predicate, action, tickRate);
    }

    /**
     * 构造一个目标。
     *
     * @param predicate 执行的断言
     * @param action    要应用的操作
     * @param tickRate  刷新率
     */
    protected AITarget(
      @NotNull final BooleanSupplier predicate,
      @NotNull final Supplier<IAIState> action,
      final int tickRate)
    {
        super(predicate, action, tickRate);
    }

    /**
     * 构造一个目标。
     *
     * @param predicateState 需要达到的状态 | null
     * @param state          要切换到的状态
     * @param tickRate       刷新率
     */
    public AITarget(@NotNull final IAIState predicateState, @Nullable final IAIState state, final int tickRate)
    {
        this(predicateState, () -> state, tickRate);
    }

    /**
     * 构造一个目标。
     *
     * @param state    需要达到的状态 | null
     * @param action   要应用的操作
     * @param tickRate 刷新率
     */
    public AITarget(@NotNull final IAIState state, @NotNull final Supplier<IAIState> action, final int tickRate)
    {
        this(state, () -> true, action, tickRate);
    }
}
