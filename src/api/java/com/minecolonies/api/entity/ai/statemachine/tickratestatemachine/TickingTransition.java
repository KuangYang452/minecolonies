package com.minecolonies.api.entity.ai.statemachine.tickratestatemachine;

import com.minecolonies.api.entity.ai.statemachine.basestatemachine.BasicTransition;
import com.minecolonies.api.entity.ai.statemachine.states.IState;
import org.jetbrains.annotations.NotNull;

import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

import static com.minecolonies.api.entity.ai.statemachine.tickratestatemachine.TickRateConstants.MAX_TICKRATE;
import static com.minecolonies.api.entity.ai.statemachine.tickratestatemachine.TickRateConstants.MAX_TICKRATE_VARIANT;

/**
 * 使用滴答率逻辑进行过渡，允许定义一个预期的滴答率，在该滴答率下将检查此过渡。
 */
public class TickingTransition<S extends IState> extends BasicTransition<S> implements ITickingTransition<S>
{
    /**
     * 要调用目标的滴答率，例如 tickRate = 20 意味着每 20 个滴答调用一次函数。
     */
    private int tickRate;

    /**
     * 用于滴答的随机偏移，以使 AITarget 在服务器滴答上获得更分布均匀的激活。
     */
    private final int tickOffset;

    /**
     * 在创建 AITarget 时使用的变量，它会随着每个 AITarget 的创建而变化，影响下一个 AITarget。
     */
    @SuppressWarnings("PMD.AssignmentToNonFinalStatic")
    private static int tickOffsetVariant = 0;

    /**
     * 创建一个带有滴答率的新过渡。
     *
     * @param state     要应用过渡的状态
     * @param condition 在进入下一个状态之前检查的条件
     * @param nextState 这个过渡导向的下一个状态
     * @param tickRate  应该检查此过渡的期望滴答率。
     */
    public TickingTransition(
      @NotNull final S state,
      @NotNull final BooleanSupplier condition,
      @NotNull final Supplier<S> nextState,
      final int tickRate)
    {
        super(state, condition, nextState);

        // 限制滴答率
        this.tickRate = Math.min(tickRate, MAX_TICKRATE);
        this.tickRate = Math.max(this.tickRate, 1);

        // 计算偏移以避免后续重复计算
        this.tickOffset = tickOffsetVariant % this.tickRate;
        // 增加偏移变量以供下一个 AITarget 使用，并在一定点重置变量
        tickOffsetVariant++;
        if (tickOffsetVariant >= MAX_TICKRATE_VARIANT)
        {
            tickOffsetVariant = 0;
        }
    }

    /**
     * 创建一个带有滴答率的新过渡。
     *
     * @param condition 在进入下一个状态之前检查的条件
     * @param nextState 这个过渡导向的下一个状态
     * @param tickRate  应该检查此过渡的期望滴答率。
     */
    public TickingTransition(
      @NotNull final BooleanSupplier condition,
      @NotNull final Supplier<S> nextState,
      final int tickRate)
    {
        super(condition, nextState);

        // 限制滴答率
        this.tickRate = Math.min(tickRate, MAX_TICKRATE);
        this.tickRate = Math.max(this.tickRate, 1);

        // 计算偏移以避免后续重复计算
        this.tickOffset = tickOffsetVariant % this.tickRate;
        // 增加偏移变量以供下一个 AITarget 使用，并在一定点重置变量
        tickOffsetVariant++;
        if (tickOffsetVariant >= MAX_TICKRATE_VARIANT)
        {
            tickOffsetVariant = 0;
        }
    }

    /**
     * 返回 AITarget 的预期滴答率。
     *
     * @return 滴答率
     */
    @Override
    public int getTickRate()
    {
        return tickRate;
    }

    /**
     * 允许动态更改滴答率。
     *
     * @param tickRate AITarget 应该滴答的速率
     */
    @Override
    public void setTickRate(final int tickRate)
    {
        this.tickRate = tickRate;
    }

    /**
     * 返回预设的滴答偏移。
     *
     * @return 随机值
     */
    @Override
    public int getTickOffset()
    {
        return tickOffset;
    }
}
