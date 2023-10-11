package com.minecolonies.api.entity.ai.statemachine.basestatemachine;

import com.minecolonies.api.entity.ai.statemachine.states.IState;
import com.minecolonies.api.entity.ai.statemachine.transitions.IStateMachineTransition;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/**
 * 用于状态机的基本转换类。包括转换适用的状态、确定下一个状态的状态提供者以及必须为真的条件
 * 才能转换到下一个状态。
 */
public class BasicTransition<S extends IState> implements IStateMachineTransition<S>
{
    /**
     * 我们开始的状态
     */
    @Nullable
    private final S state;

    /**
     * 需要满足的过渡条件。
     */
    @NotNull
    private final BooleanSupplier condition;

    /**
     * 我们将要过渡到的下一个状态。
     */
    @NotNull
    private final Supplier<S> nextState;

    /**
     * 在条件 C 下从状态 A 创建到状态 B 的新过渡。
     *
     * @param state     状态 A
     * @param condition 条件 C
     * @param nextState 状态 B
     */
    public BasicTransition(@NotNull final S state, @NotNull final BooleanSupplier condition, @NotNull final Supplier<S> nextState)
    {
        this.state = state;
        this.condition = condition;
        this.nextState = nextState;
    }

    /**
     * 受保护的构造函数，允许子类没有状态。
     *
     * @param condition 条件。
     * @param nextState 要前往的下一个状态。
     */
    protected BasicTransition(@NotNull final BooleanSupplier condition, @NotNull final Supplier<S> nextState)
    {
        this.state = null;
        this.condition = condition;
        this.nextState = nextState;
    }

    /**
     * 返回应用此转换的状态。
     *
     * @return IAIState
     */
    public S getState()
    {
        return state;
    }

    /**
     * 计算下一个要进入的状态。
     *
     * @return 下一个AI状态
     */
    public S getNextState()
    {
        return nextState.get();
    }

    /**
     * 检查此转换的条件是否适用。
     */
    public boolean checkCondition()
    {
        return condition.getAsBoolean();
    }
}
