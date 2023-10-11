package com.minecolonies.api.colony.requestsystem.request;

import net.minecraft.nbt.IntTag;
import net.minecraft.network.FriendlyByteBuf;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 用于描述请求状态的枚举。
 */
public enum RequestState
{
    /**
     * 未注册请求的默认状态。
     */
    CREATED,

    /**
     * 已注册但未解决的请求状态。
     */
    REPORTED,

    /**
     * 分配中的请求状态。
     */
    ASSIGNING,

    /**
     * 已分配但未开始的请求状态。
     */
    ASSIGNED,

    /**
     * 处于处理中的请求状态。
     */
    IN_PROGRESS,

    /**
     * 主要处理已完成的请求状态。后续处理仍需要确定。
     */
    RESOLVED,

    /**
     * 已解决但后续处理仍在进行中的请求状态。
     */
    FOLLOWUP_IN_PROGRESS,

    /**
     * 已完成的请求状态。
     */
    COMPLETED,

    /**
     * 玩家强制完成的请求状态。
     */
    OVERRULED,

    /**
     * 已取消的请求状态。
     */
    CANCELLED,

    /**
     * 表示请求已被请求者接收的状态。
     */
    RECEIVED,

    /**
     * 用于指示请求已被取消覆盖，但仍在进行中以确保正确清理的状态。
     */
    FINALIZING,

    /**
     * 用于指示真正失败的请求状态。
     */
    FAILED;

    /**
     * 用于读取和写入NBT的索引列表。
     */
    static final List<RequestState> indexList = new ArrayList<>();
    static
    {
        /*
         * 永远不要更改这个！它用于在不同的模组版本之间读取和写入NBT，所以必须保持不变。
         */
        Collections.addAll(indexList, RequestState.values());
    }
    /**
     * 这是一个空的构造函数，我不知道为什么，可能是Orion的原因 =D。
     */
    RequestState()
    {
    }

    /**
     * 从NBT反序列化RequestState的方法。
     *
     * @param nbt 要反序列化的NBT。
     * @return 存储在给定NBT中的RequestState。
     */
    public static RequestState deserialize(final IntTag nbt)
    {
        return indexList.get(nbt.getAsInt());
    }

    /**
     * 将状态序列化为NBT的方法。
     *
     * @return 状态的NBT表示。
     */
    public IntTag serialize()
    {
        return IntTag.valueOf(indexList.indexOf(this));
    }

    /**
     * 从数据包缓冲区反序列化RequestState的方法。
     *
     * @param buffer 要反序列化的缓冲区。
     * @return 存储在给定NBT中的RequestState。
     */
    public static RequestState deserialize(final FriendlyByteBuf buffer)
    {
        return indexList.get(buffer.readInt());
    }

    /**
     * 将状态序列化为数据包缓冲区的方法。
     *
     * @param buffer 要写入的缓冲区。
     */
    public void serialize(FriendlyByteBuf buffer)
    {
        buffer.writeInt(indexList.indexOf(this));
    }
}
