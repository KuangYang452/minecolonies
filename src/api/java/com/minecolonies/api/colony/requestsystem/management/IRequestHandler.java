package com.minecolonies.api.colony.requestsystem.management;

import com.minecolonies.api.colony.requestsystem.manager.AssigningStrategy;
import com.minecolonies.api.colony.requestsystem.manager.IRequestManager;
import com.minecolonies.api.colony.requestsystem.request.IRequest;
import com.minecolonies.api.colony.requestsystem.requestable.IRequestable;
import com.minecolonies.api.colony.requestsystem.requester.IRequester;
import com.minecolonies.api.colony.requestsystem.token.IToken;

import java.util.Collection;

public interface IRequestHandler
{
    IRequestManager getManager();

    <Request extends IRequestable> IRequest<Request> createRequest(IRequester requester, Request request);

    void registerRequest(IRequest<?> request);

    /**
     * 用于将给定请求分配给解决器，不考虑任何黑名单。
     *
     * @param request 分配的请求
     * @throws IllegalArgumentException 当请求已经被分配时抛出
     */
    void assignRequest(IRequest<?> request);

    /**
     * 用于将给定请求分配给解决器，考虑给定的解决器令牌黑名单。
     *
     * @param request                分配的请求。
     * @param resolverTokenBlackList 在此黑名单中具有其令牌的每个解决器在检查可能的解决器时都将被跳过。
     * @return 已分配请求的解决器的令牌，如果未找到则返回null。
     * @throws IllegalArgumentException 当请求在此管理器中未知时抛出。
     */
    IToken<?> assignRequest(IRequest<?> request, Collection<IToken<?>> resolverTokenBlackList);

    /**
     * 用于将给定请求分配给解决器，考虑给定的解决器令牌黑名单。使用默认的分配策略：{@link AssigningStrategy#PRIORITY_BASED}
     *
     * @param request                分配的请求。
     * @param resolverTokenBlackList 在此黑名单中具有其令牌的每个解决器在检查可能的解决器时都将被跳过。
     * @return 已分配请求的解决器的令牌，如果未找到则返回null。
     * @throws IllegalArgumentException 当请求在此管理器中未知时抛出。
     */
    IToken<?> assignRequestDefault(IRequest<?> request, Collection<IToken<?>> resolverTokenBlackList);

    /**
     * 用于重新分配请求给不在给定黑名单中的解决器。在不通知请求者的情况下，取消请求并尝试重新分配。如果重新分配失败，则分配回原始解决器。
     *
     * @param request                正在重新分配的请求。
     * @param resolverTokenBlackList 不分配请求的黑名单。
     * @return 已分配请求的解决器的令牌，如果未找到则返回null。
     * @throws IllegalArgumentException 当发生错误时抛出。
     */
    IToken<?> reassignRequest(IRequest<?> request, Collection<IToken<?>> resolverTokenBlackList);

    /**
     * 用于检查给定请求令牌是否已分配给解决器。
     *
     * @param token 要检查的请求令牌。
     * @return 当请求令牌已分配时返回true，否则返回false。
     */
    boolean isAssigned(IToken<?> token);

    /**
     * 用于处理请求成功解决及其子请求的完成。
     *
     * @param token 完成解决的请求令牌。
     */
    void onRequestResolved(IToken<?> token);

    /**
     * 用于处理请求成功完成，其子请求和后续请求。
     *
     * @param token 成功完成的请求令牌。
     */
    void onRequestCompleted(IToken<?> token);

    /**
     * 用于处理被覆盖或取消的请求。首先取消所有子请求，然后处理清理请求的创建。
     *
     * @param token 被取消或覆盖的请求的令牌
     */
    void onRequestOverruled(IToken<?> token);

    /**
     * 用于处理被覆盖或取消的请求。首先取消所有子请求，然后处理清理请求的创建。
     *
     * @param token 被取消或覆盖的请求的令牌
     */
    void onRequestCancelled(IToken<?> token);

    void onChildRequestCancelled(IToken<?> token);

    void onRequestCancelledDirectly(IToken<?> token);

    void processDirectCancellationAndNotifyRequesterOf(IRequest<?> request);

    void processDirectCancellationOf(IRequest<?> request);

    /**
     * 用于解决请求。调用此方法时，给定的请求必须已分配。
     *
     * @param request 要解决的请求。
     * @throws IllegalArgumentException 当请求未知、未解决或无法解决时抛出。
     */
    void resolveRequest(IRequest<?> request);

    /**
     * 当给定管理器被其请求者通知收到给定任务时调用的方法。在此时，所有与解决器的通信都应被中止，因此需要处理覆盖和取消。
     *
     * @param token 请求的令牌。
     * @throws IllegalArgumentException 当令牌未知时抛出。
     */
    void cleanRequestData(IToken<?> token);

    /**
     * 用于从给定令牌获取已注册的请求。
     *
     * @param token 要查询的令牌
     * @return 请求。
     * @throws IllegalArgumentException 当令牌未知于给定管理器时抛出。
     */
    IRequest<?> getRequest(IToken<?> token);

    /**
     * 用于从给定令牌获取已注册的请求。
     *
     * @param token 要获取请求的令牌。
     * @return 请求，如果不存在具有该令牌的请求则返回null。
     */
    IRequest<?> getRequestOrNull(IToken<?> token);

    /**
     * 返回由给定请求者发出的所有请求。
     *
     * @param requester 相关的请求者。
     * @return 由给定请求者发出的请求实例的集合。
     */
    Collection<IRequest<?>> getRequestsMadeByRequester(IRequester requester);

    /**
     * 从管理器中移除特定请求者和所有已分配的请求。
     * @param requester 请求者。
     */
    void removeRequester(IRequester requester);
}
