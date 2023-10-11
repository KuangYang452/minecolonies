package com.minecolonies.api.colony.interactionhandling;

/**
 * 交互的不同优先级类型。
 */
public enum ChatPriority implements IChatPriority
{
    HIDDEN,
    CHITCHAT,
    PENDING,
    IMPORTANT,
    BLOCKING;

    @Override
    public int getPriority()
    {
        return this.ordinal();
    }
}
