package com.minecolonies.api.entity.citizen.citizenhandlers;

import com.minecolonies.api.entity.ai.Status;
import net.minecraft.network.chat.Component;

public interface ICitizenStatusHandler
{
    /**
     * 获取市民的最新状态。
     *
     * @return 描述状态的长度为4的组件。
     */
    Component[] getLatestStatus();

    /**
     * 设置市民的最新状态并清除现有状态。
     *
     * @param status 要设置的新状态。
     */
    void setLatestStatus(Component... status);

    /**
     * 追加到现有的latestStatus列表中。如果已满，这将覆盖最旧的状态并将其他状态向下移动到数组中。
     *
     * @param status 要追加的最新状态。
     */
    void addLatestStatus(Component status);

    /**
     * 获取当前状态的getter。
     *
     * @return 状态。
     */
    Status getStatus();

    /**
     * 当前状态的setter。
     *
     * @param status 要设置的状态。
     */
    void setStatus(Status status);
}
