package com.minecolonies.api.entity.ai.statemachine.tickratestatemachine;

import com.minecolonies.api.entity.ai.statemachine.states.IState;
import com.minecolonies.api.entity.ai.statemachine.states.IStateEventType;
import com.minecolonies.api.entity.ai.statemachine.transitions.IStateMachineEvent;
import org.jetbrains.annotations.NotNull;

import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/**
 * 具有节拍率的状态机事件。
 */
public class TickingEvent<S extends IState> extends TickingTransition<S> implements IStateMachineEvent<S>
{
    /**
     * 此事件的类型
     */
    private final IStateEventType eventType;

    /**
     * 创建一个新的 TickingEvent
     *
     * @param eventType 事件的类型
     * @param condition 事件适用的条件
     * @param nextState 事件过渡到的状态
     * @param tickRate  检查事件的节拍率
     */
    protected TickingEvent(
      @NotNull final IStateEventType eventType,
      @NotNull final BooleanSupplier condition,
      @NotNull final Supplier<S> nextState,
      @NotNull final int tickRate)
    {
        super(condition, nextState, tickRate);
        this.eventType = eventType;
    }

    /**
     * 获取此事件的类型
     */
    @Override
    public IStateEventType getEventType()
    {
        return eventType;
    }
}
