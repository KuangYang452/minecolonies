package com.minecolonies.api.colony.requestsystem.resolver;

import com.google.common.collect.Lists;
import com.google.common.reflect.TypeToken;
import com.minecolonies.api.colony.requestsystem.manager.IRequestManager;
import com.minecolonies.api.colony.requestsystem.request.IRequest;
import com.minecolonies.api.colony.requestsystem.requestable.IRequestable;
import com.minecolonies.api.colony.requestsystem.requester.IRequester;
import com.minecolonies.api.colony.requestsystem.token.IToken;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Predicate;

/**
 * 用于解决请求的接口。在一个殖民地中，可以存在多个解决器，用于解决给定类型 R 的请求。首先检查具有最高优先级的解决器，然后是第二个，依此类推。
 *
 * 解决器本身负责存储它返回的请求的令牌。
 *
 * @param <R> 此解决器可以提供的请求类型。
 */
public interface IRequestResolver<R extends IRequestable> extends IRequester {

    /**
     * 用于确定此解决器可以解决的请求类型。
     *
     * @return 表示此解决器可以解决的请求类型的类。
     */
    TypeToken<? extends R> getRequestType();

    /**
     * 用于确定此请求解析器是否能够解决给定请求的类型。应快速且廉价地检查此解决器是否可能解决此请求。
     *
     * @param manager 要检查此解决器是否可以解决请求的管理器。
     * @param requestToCheck 要检查的请求。
     * @return 如果此解决器可以解决给定请求，则返回 true；否则返回 false。
     */
    boolean canResolveRequest(@NotNull IRequestManager manager, IRequest<? extends R> requestToCheck);

    /**
     * 用于尝试解决请求的方法。
     *
     * 当此尝试成功时，将返回所需请求的令牌列表。此列表可能为空。列表应指示在调用 @code{resolve(IRequest request)} 方法之前应该满足的所有子请求。
     *
     * 当此尝试不成功时，例如此解决器无法安排制作操作，则应返回 Null 对象。在这种情况下，管理器将尝试下一个解决器。
     *
     * 非常重要的是，返回的请求尚未被分配。否则，模拟和其他策略将失败！给定的管理器将自己处理分配！
     *
     * @param manager 尝试使用此解决器进行解决的管理器。
     * @param request 要解决的请求。
     * @return 如果尝试成功，则返回所需请求的令牌（允许空列表以指示无需求），如果尝试失败，则返回 null。
     */
    @Nullable
    List<IToken<?>> attemptResolveRequest(@NotNull IRequestManager manager, @NotNull IRequest<? extends R> request);

    /**
     * 用于解决给定请求的方法。在所有子请求都解决后立即调用。
     *
     * 解决器应通过给定的管理器更新状态。
     *
     * 当调用此方法时，对于此解决器，所有要求都需要满足。否则，它将抛出 RuntimeException。
     *
     * @param request 要解决的请求。
     * @param manager 正在解决此请求的管理器，正常情况下，这是殖民地管理器。
     * @throws RuntimeException 当解决器无法解决请求时抛出。这应该永远不会发生，因为首先应该调用 attemptResolve，此时所有要求应该对解决器可用。
     */
    @Nullable
    void resolveRequest(@NotNull IRequestManager manager, @NotNull IRequest<? extends R> request);

    /**
     * 由给定的管理器调用，以指示已将此请求分配给您。
     *
     * @param manager 系统管理器。
     * @param request 已分配的请求。
     * @param simulation 如果正在模拟，则为 true。
     */
    default void onRequestAssigned(@NotNull final IRequestManager manager, @NotNull final IRequest<? extends R> request, boolean simulation) {
        // 没有操作
    }

    /**
     * 表示已取消分配的请求。在更新图形之前调用。
     *
     * @param manager 指示取消操作的管理器。
     * @param request 已取消的请求。
     */
    void onAssignedRequestBeingCancelled(@NotNull IRequestManager manager, @NotNull IRequest<? extends R> request);

    /**
     * 表示已取消分配的请求。在更新图形后调用。
     *
     * @param manager 指示取消操作的管理器。
     * @param request 已取消的请求。
     */
    void onAssignedRequestCancelled(@NotNull IRequestManager manager, @NotNull IRequest<? extends R> request);

    /**
     * 由给定的管理器调用，以指示殖民地已更新其可用物品。
     *
     * @param manager 系统管理器。
     * @param shouldTriggerReassign 指示已分配的请求
     */
    default void onColonyUpdate(@NotNull final IRequestManager manager, @NotNull final Predicate<IRequest<?>> shouldTriggerReassign) {
        // 没有操作
    }

    @Nullable
    default List<IRequest<?>> getFollowupRequestForCompletion(@NotNull IRequestManager manager, @NotNull IRequest<? extends R> completedRequest) {
        return Lists.newArrayList();
    }

    /**
     * 检查此解决器对请求的适用性。
     * @param request 要检查的请求。
     * @return 适用性度量值（用于简单比较的整数）。
     */
    default int getSuitabilityMetric(@NotNull final IRequest<? extends R> request) {
        return 0;
    }

    /**
     * 此解决器的优先级。优先级越高，解决器越早调用。
     *
     * @return 此解决器的优先级。
     */
    int getPriority();
}