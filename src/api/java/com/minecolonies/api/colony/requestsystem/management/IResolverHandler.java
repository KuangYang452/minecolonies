package com.minecolonies.api.colony.requestsystem.management;

import com.google.common.annotations.VisibleForTesting;
import com.minecolonies.api.colony.requestsystem.manager.IRequestManager;
import com.minecolonies.api.colony.requestsystem.request.IRequest;
import com.minecolonies.api.colony.requestsystem.requestable.IRequestable;
import com.minecolonies.api.colony.requestsystem.resolver.IRequestResolver;
import com.minecolonies.api.colony.requestsystem.token.IToken;

import java.util.Collection;
import java.util.function.Predicate;

public interface IResolverHandler
{
    IRequestManager getManager();

    /**
     * 用于同时注册多个解析器的方法
     * <p>
     * 仅在内部使用。该方法修改了用于跟踪解析器的resolverBiMap。
     * </p>
     *
     * @param resolvers 要注册的解析器。
     * @return 注册时解析器的令牌。
     * @throws IllegalArgumentException 如果对任何给定的解析器调用registerResolver方法时抛出IllegalArgumentException，则会抛出IllegalArgumentException。
     */
    Collection<IToken<?>> registerResolvers(IRequestResolver<?>... resolvers);

    /**
     * 用于注册解析器的方法
     * <p>
     * 仅在内部使用。该方法修改了用于跟踪已注册的解析器的resolverBiMap。
     * </p>
     *
     * @param resolver 要注册的解析器
     * @return 新注册解析器的令牌
     * @throws IllegalArgumentException 如果要附加到解析器的令牌已经注册，或者解析器已经使用不同的令牌注册。
     */
    IToken<?> registerResolver(IRequestResolver<? extends IRequestable> resolver);

    /**
     * 用于同时注册多个解析器的方法
     * <p>
     * 仅在内部使用。该方法修改了用于跟踪解析器的resolverBiMap。
     * </p>
     *
     * @param resolvers 要注册的解析器。
     * @return 注册时解析器的令牌。
     * @throws IllegalArgumentException 如果对任何给定的解析器调用registerResolver方法时抛出IllegalArgumentException，则会抛出IllegalArgumentException。
     */
    Collection<IToken<?>> registerResolvers(Collection<IRequestResolver<?>> resolvers);

    /**
     * 用于移除已注册解析器的方法
     * <p>
     * 仅在内部使用。该方法修改了用于跟踪解析器的resolverBiMap。
     * </p>
     *
     * @param token 要移除的解析器的令牌。
     * @throws IllegalArgumentException 如果给定的解析器未注册，或者给定的解析器的令牌未注册到相同的解析器。
     */
    void removeResolver(IToken<?> token);

    /**
     * 用于移除已注册解析器的方法
     * <p>
     * 仅在内部使用。该方法修改了用于跟踪解析器的resolverBiMap。
     * </p>
     *
     * @param resolver 要移除的解析器
     * @throws IllegalArgumentException 如果给定的解析器未注册，或者给定的解析器的令牌未注册到相同的解析器。
     */
    void removeResolver(IRequestResolver<?> resolver);

    /**
     * 获取当前分配给解析器的所有请求的方法。
     *
     * @param resolver 要获取请求的解析器。
     * @return 带有请求令牌的集合。
     */
    Collection<IToken<?>> getRequestsAssignedToResolver(IRequestResolver<?> resolver);

    /**
     * 根据给定令牌获取解析器的方法。
     * <p>
     * 仅在内部使用。查询resolverBiMap以获取给定令牌的解析器。
     * </p>
     *
     * @param token 要查找的解析器的令牌。
     * @return 使用给定令牌注册的解析器。
     * @throws IllegalArgumentException 当给定令牌未注册到任何IRequestResolver时抛出。
     */
    IRequestResolver<? extends IRequestable> getResolver(IToken<?> token);

    void removeResolverInternal(IRequestResolver<?> resolver);

    /**
     * 用于移除多个已注册解析器的方法
     * <p>
     * 仅在内部使用。该方法修改了用于跟踪解析器的resolverBiMap。
     * </p>
     *
     * @param resolvers 要移除的解析器。
     * @throws IllegalArgumentException 如果对任何给定的解析器调用removeResolver方法时抛出IllegalArgumentException，则会抛出IllegalArgumentException。
     */
    void removeResolvers(IRequestResolver<?>... resolvers);

    /**
     * 用于移除多个已注册解析器的方法
     * <p>
     * 仅在内部使用。该方法修改了用于跟踪解析器的resolverBiMap。
     * </p>
     *
     * @param resolvers 要移除的解析器。
     * @throws IllegalArgumentException 如果对任何给定的解析器调用removeResolver方法时抛出IllegalArgumentException，则会抛出IllegalArgumentException。
     */
    void removeResolvers(Iterable<IRequestResolver<?>> resolvers);

    /**
     * 用于将请求添加到解析器的方法
     * <p>
     * 仅在内部使用。该方法修改了用于跟踪哪个解析器处理哪个请求的resolverRequestMap。
     * </p>
     *
     * @param resolver 要将请求添加到的解析器。
     * @param request  要添加到解析器的请求。
     */
    void addRequestToResolver(IRequestResolver<?> resolver, IRequest<?> request);

    /**
     * 用于从解析器中移除请求的方法
     * <p>
     * 仅在内部使用。该方法修改了用于跟踪哪个解析器处理哪个请求的resolverRequestMap。
     * </p>
     *
     * @param resolver 要从中移除给定请求的解析器。
     * @param request  要移除的请求。
     * @throws IllegalArgumentException 当解析器未知或给定请求未注册到给定解析器时抛出。
     */
    void removeRequestFromResolver(IRequestResolver<?> resolver, IRequest<?> request);

    /**
     * 从给定请求令牌获取解析器的方法。
     *
     * @param requestToken 请求令牌，需要请求分配的解析器。
     * @return 具有给定令牌的请求的解析器。
     * @throws IllegalArgumentException 当令牌未知或请求尚未分配时抛出。
     */
    IRequestResolver<? extends IRequestable> getResolverForRequest(IToken<?> requestToken);

    /**
     * 从给定请求获取解析器的方法。
     *
     * @param request 请求，需要请求分配的解析器。
     * @return 请求的解析器。
     * @throws IllegalArgumentException 当令牌未知或请求尚未分配时抛出。
     */
    IRequestResolver<? extends IRequestable> getResolverForRequest(IRequest<?> request);

    /**
     * 基于谓词重新分配请求的方法
     *
     * @param shouldTriggerReassign 用于确定是否应重新分配请求的谓词
     */
    void onColonyUpdate(Predicate<IRequest<?>> shouldTriggerReassign);

    /**
     * 检查解析器是否正在被移除中。
     * @param id 要检查的解析器的ID。
     * @return 如果是，则返回true。
     */
    boolean isBeingRemoved(IToken<?> id);

    /**
     * 处理与正在被移除的提供程序附加的单个解析器的移除的内部方法。
     *
     * @param assignedResolvers 正在被移除的解析器列表。
     * @param resolverToken 要移除的解析器的ID，必须是assignedResolvers列表的一部分。
     */
    @VisibleForTesting
    void processResolverForRemoval(final Collection<IToken<?>> assignedResolvers, final IToken<?> resolverToken);
}
