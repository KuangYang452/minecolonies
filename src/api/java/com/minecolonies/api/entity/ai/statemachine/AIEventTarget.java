package com.minecolonies.api.entity.ai.statemachine;

import com.minecolonies.api.entity.ai.statemachine.states.AIBlockingEventType;
import com.minecolonies.api.entity.ai.statemachine.states.IAIState;
import com.minecolonies.api.entity.ai.statemachine.states.IState;
import com.minecolonies.api.entity.ai.statemachine.tickratestatemachine.TickingEvent;
import com.minecolonies.api.entity.ai.statemachine.tickratestatemachine.TickingTransition;
import org.jetbrains.annotations.NotNull;

import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/**
 * 特殊的AI目标，用于进行先决状态检查和限制。它们始终在常规AITargets之前进行检查。
 */
public class AIEventTarget<S extends IState> extends TickingEvent<S>
{
    /**
     * 构造一个特殊的目标。
     *
     * @param eventType  AISpecial State（AI特殊状态）
     * @param predicate  在执行操作之前检查的布尔谓词
     * @param action     返回下一个eventType的操作供应商
     * @param tickRate   应调用此目标的tick速率
     */
    public AIEventTarget(
      @NotNull final AIBlockingEventType eventType,
      @NotNull final BooleanSupplier predicate,
      @NotNull final Supplier<S> action, final int tickRate)
    {
        super(eventType, predicate, action, tickRate);
    }

    public AIEventTarget(
      @NotNull final AIBlockingEventType eventType,
      @NotNull final BooleanSupplier predicate,
      @NotNull final S IAIState,
      final int tickRate)
    {
        super(eventType, predicate, () -> IAIState, tickRate);
    }

    public AIEventTarget(
      @NotNull final AIBlockingEventType eventType,
      @NotNull final Supplier<S> action,
      final int tickRate)
    {
        super(eventType, () -> true, action, tickRate);
    }
}
