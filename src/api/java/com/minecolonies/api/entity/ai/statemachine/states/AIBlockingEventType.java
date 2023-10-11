package com.minecolonies.api.entity.ai.statemachine.states;

/**
 * 用于状态机事件的事件类型。
 */
public enum AIBlockingEventType implements IStateEventType
{
    /*###Priority NonStates###*/
    /**
     * AITargets的最高优先级状态，如果返回true，则停止本次AI执行。无论当前状态如何都会检查。
     */
    AI_BLOCKING,
    /**
     * 在尝试执行正常状态的AITarget之前检查的高优先级状态，如果返回true，将阻止进一步执行。无论当前状态如何都会检查。
     */
    STATE_BLOCKING,
    /**
     * 用于执行一个动作，然后在返回状态后自行消耗的较高优先级状态。在检查AI_BLOCKING_PRIO目标后立即检查，如果阻止了AITargets的进一步执行，则返回true。无论当前状态如何都会检查。
     */
    EVENT
}
