package com.minecolonies.api.colony.requestsystem.resolver.retrying;

import com.minecolonies.api.colony.requestsystem.manager.IRequestManager;
import com.minecolonies.api.colony.requestsystem.requestable.IRetryable;
import com.minecolonies.api.colony.requestsystem.resolver.IQueuedRequestResolver;
import com.minecolonies.api.colony.requestsystem.token.IToken;
import com.minecolonies.api.tileentities.ITickable;
import org.jetbrains.annotations.Nullable;

public interface IRetryingRequestResolver extends IQueuedRequestResolver<IRetryable>, ITickable
{
    /**
     * 更新与此解决程序关联的管理器数据，将其与一个管理器关联起来。
     *
     * @param manager 新的关联管理器。
     */
    void updateManager(IRequestManager manager);

    /**
     * 获取解析程序尝试的最大尝试次数的方法。
     *
     * @return 最大尝试次数。
     */
    int getMaximalTries();

    /**
     * 获取重试之间的最大刻数。
     *
     * @return 重试之间的最大刻数。
     */
    int getMaximalDelayBetweenRetriesInTicks();

    /**
     * 当前尝试重新分配的方法。
     *
     * @return 当前重新分配尝试。如果没有进行重新分配，则返回-1。
     */
    int getCurrentReassignmentAttempt();

    /**
     * 获取重新分配的指示的方法。
     *
     * @return 重新分配的指示。
     */
    default boolean isReassigning()
    {
        return getCurrentlyBeingReassignedRequest() != null;
    }

    /**
     * 获取当前正在重新分配的请求的令牌的方法。
     *
     * @return 当前正在重新分配的请求的令牌，如果未执行重新分配则返回null。
     */
    @Nullable
    IToken<?> getCurrentlyBeingReassignedRequest();
}
