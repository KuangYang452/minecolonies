package com.minecolonies.api.colony.requestsystem.requestable.deliveryman;

/**
 * 所有送货员请求的抽象类
 */
public abstract class AbstractDeliverymanRequestable implements IDeliverymanRequestable
{
    ////// --------------------------- NBTConstants --------------------------- \\\\\\
    protected static final String NBT_PRIORITY = "Priority";
    ////// --------------------------- NBTConstants --------------------------- \\\\\\

    private static final int MAX_BUILDING_PRIORITY     = 10;
    private static final int DEFAULT_DELIVERY_PRIORITY = 13;
    private static final int MAX_AGING_PRIORITY        = 14;
    private static final int PLAYER_ACTION_PRIORITY    = 15;

    protected int priority = 0;

    /**
     * 送货员请求的构造函数
     *
     * @param priority 请求的优先级。优先级越高，交付/取货越早
     */
    protected AbstractDeliverymanRequestable(final int priority)
    {
        this.priority = priority;
    }

    /**
     * 将优先级缩放到所需的内部值。这是为了使实际优先级不仅仅是1-10，而是1-100（x^2）。这将有效地使总是递增1的老化算法变得更慢。
     * 函数可以是任何东西 - 线性缩放器、二次、指数等等。随着时间的推移，找到最佳解决方案。
     */
    public static int scaledPriority(final int priority)
    {
        // 这个版本使增加变成了二次方增长
        // return (int) Math.pow(priority, 2);

        return priority;
    }

    /**
     * 获取建筑交互界面中允许设置的最大优先级。这是玩家可以使用的“正常”设置。
     *
     * @param returnScaled 如果要返回经过缩放的值，则为true
     * @return 经过缩放/未经缩放的优先级
     */
    public static int getMaxBuildingPriority(final boolean returnScaled)
    {
        return returnScaled ? scaledPriority(MAX_BUILDING_PRIORITY) : MAX_BUILDING_PRIORITY;
    }

    /**
     * 获取分配给交付的优先级。这会影响工匠的后续交付以及仓库的交付。
     *
     * @param returnScaled 如果值应该返回经过缩放的话，设置为true
     * @return 缩放/未缩放的优先级
     */
    public static int getDefaultDeliveryPriority(final boolean returnScaled)
    {
        return returnScaled ? scaledPriority(DEFAULT_DELIVERY_PRIORITY) : DEFAULT_DELIVERY_PRIORITY;
    }

    /**
     * 获取老化机制可以分配的最大优先级。之后，优先级就不会自然增加。
     *
     * @param returnScaled 如果值应该返回经过缩放的话，设置为true
     * @return 缩放/未缩放的优先级
     */
    public static int getMaxAgingPriority(final boolean returnScaled)
    {
        return returnScaled ? scaledPriority(MAX_AGING_PRIORITY) : MAX_AGING_PRIORITY;
    }

    /**
     * 获取分配给“立即行动请求”的优先级。请注意：这最终应该影响邮箱（Postbox）。
     *
     * @param returnScaled 如果要返回经过缩放的值，则为true
     * @return 缩放/未缩放的优先级
     */
    public static int getPlayerActionPriority(final boolean returnScaled)
    {
        return returnScaled ? scaledPriority(PLAYER_ACTION_PRIORITY) : PLAYER_ACTION_PRIORITY;
    }

    @Override
    public int getPriority()
    {
        return priority;
    }

    @Override
    public void incrementPriorityDueToAging()
    {
        // 老化机制设置的优先级实际上可以超过请求者可以选择的最大优先级。
        // 最坏的情况下，优先级队列会变成一个对于非常老的请求来说是FIFO队列，新的最大优先级请求需要等待。
        priority = Math.min(getMaxAgingPriority(true), priority + 1);
    }

    @Override
    public boolean equals(final Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (!(o instanceof AbstractDeliverymanRequestable))
        {
            return false;
        }
        return getPriority() == ((AbstractDeliverymanRequestable) o).getPriority();
    }

    @Override
    public int hashCode()
    {
        return getPriority();
    }
}
