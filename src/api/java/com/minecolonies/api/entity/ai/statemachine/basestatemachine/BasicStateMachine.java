package com.minecolonies.api.entity.ai.statemachine.basestatemachine;

import com.minecolonies.api.entity.ai.statemachine.states.IState;
import com.minecolonies.api.entity.ai.statemachine.states.IStateEventType;
import com.minecolonies.api.entity.ai.statemachine.transitions.IStateMachineEvent;
import com.minecolonies.api.entity.ai.statemachine.transitions.IStateMachineOneTimeEvent;
import com.minecolonies.api.entity.ai.statemachine.transitions.IStateMachineTransition;
import com.minecolonies.api.util.Log;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
/**
 * 基本的状态机类，可用于任何扩展了过渡接口的Transition类型。它包含当前状态和事件与过渡的哈希映射，这是使状态机工作所需的最小要求。
 */
public class BasicStateMachine<T extends IStateMachineTransition<S>, S extends IState> implements IStateMachine<T, S>
{
    /**
     * 过渡和事件的列表
     */
    @NotNull
    protected final Map<S, List<T>>               transitionMap;
    @NotNull
    protected final Map<IStateEventType, List<T>> eventTransitionMap;

    /**
     * 当前状态的过渡列表
     */
    protected List<T> currentStateTransitions;

    /**
     * 我们所处的当前状态
     */
    @NotNull
    private S state;

    /**
     * 我们开始的状态
     */
    @NotNull
    private final S initState;

    /**
     * 异常处理程序
     */
    @NotNull
    private final Consumer<RuntimeException> exceptionHandler;

    /**
     * 构造一个新的状态机
     *
     * @param initialState     初始状态。
     * @param exceptionHandler 异常处理程序。
     */
    protected BasicStateMachine(@NotNull final S initialState, @NotNull final Consumer<RuntimeException> exceptionHandler)
    {
        this.state = initialState;
        this.initState = initialState;
        this.exceptionHandler = exceptionHandler;
        this.transitionMap = new HashMap<>();
        currentStateTransitions = new ArrayList<>();
        this.transitionMap.put(initialState, currentStateTransitions);
        this.eventTransitionMap = new HashMap<>();
    }

    /**
     * 添加一个过渡
     *
     * @param transition 要添加的过渡
     */
    public void addTransition(final T transition)
    {
        if (transition.getState() != null)
        {
            transitionMap.computeIfAbsent(transition.getState(), k -> new ArrayList<>()).add(transition);
        }
        if (transition instanceof IStateMachineEvent)
        {
            eventTransitionMap.computeIfAbsent(((IStateMachineEvent<?>) transition).getEventType(), k -> new ArrayList<>()).add(transition);
        }
    }

    /**
     * 注销一个过渡
     */
    public void removeTransition(final T transition)
    {
        if (transition instanceof IStateMachineEvent)
        {
            eventTransitionMap.get(((IStateMachineEvent<?>) transition).getEventType()).removeIf(t -> t == transition);
        }
        else
        {
            transitionMap.get(transition.getState()).removeIf(t -> t == transition);
        }
    }

    /**
     * 更新状态机。
     */
    public void tick()
    {
        for (final List<T> transitions : eventTransitionMap.values())
        {
            for (final T transition : transitions)
            {
                if (checkTransition(transition))
                {
                    return;
                }
            }
        }

        for (final T transition : currentStateTransitions)
        {
            if (checkTransition(transition))
            {
                return;
            }
        }
    }

    /**
     * 检查过渡的条件
     *
     * @param transition 要检查的目标
     * @return 如果这个目标有效且我们应该停止执行此刻则返回true
     */
    public boolean checkTransition(@NotNull final T transition)
    {
        try
        {
            if (!transition.checkCondition())
            {
                return false;
            }
        }
        catch (final RuntimeException e)
        {
            Log.getLogger().warn("过渡 " + transition + " 的条件检查引发了异常:", e);
            this.onException(e);
            return false;
        }
        return transitionToNext(transition);
    }

    /**
     * 进行转换检查。应用过渡并更改状态。如果状态为null，执行更多的过渡并不改变状态。
     *
     * @param transition 我们正在查看的过渡
     * @return 如果转到新状态则返回true
     */
    public boolean transitionToNext(@NotNull final T transition)
    {
        final S newState;
        try
        {
            newState = transition.getNextState();
        }
        catch (final RuntimeException e)
        {
            Log.getLogger().warn("过渡 " + transition + " 的状态机引发了异常:", e);
            this.onException(e);
            return false;
        }

        if (newState != null)
        {
            if (transition instanceof IStateMachineOneTimeEvent && ((IStateMachineOneTimeEvent<?>) transition).shouldRemove())
            {
                removeTransition(transition);
            }

            if (newState != state)
            {
                currentStateTransitions = transitionMap.get(newState);

                if (currentStateTransitions == null || currentStateTransitions.isEmpty())
                {
                    // 到达Trap/Sink状态，无法离开。
                    onException(new RuntimeException("缺少状态 " + newState + " 的AI过渡"));
                    reset();
                    return true;
                }
            }

            state = newState;
            return true;
        }
        return false;
    }

    /**
     * 处理上层的异常。
     *
     * @param e 要处理的异常。
     */
    protected void onException(final RuntimeException e)
    {
        exceptionHandler.accept(e);
    }

    /**
     * 获取状态机的当前状态
     *
     * @return 当前的IAIState。
     */
    public final S getState()
    {
        return state;
    }

    /**
     * 重置状态机
     */
    public void reset()
    {
        state = initState;
        currentStateTransitions = transitionMap.get(initState);
    }
}
