package com.minecolonies.api.entity.ai.statemachine.tickratestatemachine;

/**
 * 用于限制 tick 率的转换和状态机的常量。
 */
public class TickRateConstants
{
    /**
     * 在 AITarget 创建时可以设置的最大延迟
     */
    public static final int MAX_TICKRATE = 500;

    /**
     * AI Tick 的随机偏移的最大值，以避免在同一个 tick 上激活。
     */
    public static final int MAX_TICKRATE_VARIANT = 50;
}
