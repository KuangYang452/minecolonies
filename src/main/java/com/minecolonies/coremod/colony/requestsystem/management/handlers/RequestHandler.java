package com.minecolonies.coremod.colony.requestsystem.management.handlers;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.reflect.TypeToken;
import com.minecolonies.api.colony.requestsystem.management.IRequestHandler;
import com.minecolonies.api.colony.requestsystem.manager.AssigningStrategy;
import com.minecolonies.api.colony.requestsystem.manager.IRequestManager;
import com.minecolonies.api.colony.requestsystem.manager.RequestMappingHandler;
import com.minecolonies.api.colony.requestsystem.request.IRequest;
import com.minecolonies.api.colony.requestsystem.request.RequestState;
import com.minecolonies.api.colony.requestsystem.requestable.IRequestable;
import com.minecolonies.api.colony.requestsystem.requester.IRequester;
import com.minecolonies.api.colony.requestsystem.resolver.IRequestResolver;
import com.minecolonies.api.colony.requestsystem.token.IToken;
import com.minecolonies.api.util.Log;
import com.minecolonies.coremod.colony.requestsystem.management.IStandardRequestManager;
import com.minecolonies.coremod.colony.requestsystem.management.manager.wrapped.WrappedBlacklistAssignmentRequestManager;
import com.minecolonies.coremod.colony.requestsystem.management.manager.wrapped.WrappedStaticStateRequestManager;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

import static com.minecolonies.api.util.constant.Suppression.RAWTYPES;
import static com.minecolonies.api.util.constant.Suppression.UNCHECKED;

/**
 * 用于处理请求系统内部工作的类，涉及到请求处理。
 */
public class RequestHandler implements IRequestHandler
{

    private final IStandardRequestManager manager;

    public RequestHandler(final IStandardRequestManager manager) {this.manager = manager;}

    @Override
    public IRequestManager getManager()
    {
        return manager;
    }

    @Override
    @SuppressWarnings(UNCHECKED)
    public <Request extends IRequestable> IRequest<Request> createRequest(final IRequester requester, final Request request)
    {
        final IToken<?> token = manager.getTokenHandler().generateNewToken();

        final IRequest<Request> constructedRequest = manager.getFactoryController()
                                                       .getNewInstance(TypeToken.of((Class<? extends IRequest<Request>>) RequestMappingHandler.getRequestableMappings()
                                                                                                                           .get(request.getClass())), request, token, requester);

        manager.getLogger().debug("为请求创建： " + request + "，令牌：" + token + " 和输出：" + constructedRequest);

        registerRequest(constructedRequest);

        return constructedRequest;
    }

    @Override
    public void registerRequest(final IRequest<?> request)
    {
        if (manager.getRequestIdentitiesDataStore().getIdentities().containsKey(request.getId()) ||
              manager.getRequestIdentitiesDataStore().getIdentities().containsValue(request))
        {
            throw new IllegalArgumentException("给定的请求已经在此管理器中已知");
        }

        manager.getLogger().debug("注册请求： " + request);

        manager.getRequestIdentitiesDataStore().getIdentities().put(request.getId(), request);
    }

    /**
     * 用于将给定请求分配给解析器的方法，不考虑任何黑名单。
     *
     * @param request 要分配的请求
     * @throws IllegalArgumentException 当请求已经被分配时
     */
    @Override
    public void assignRequest(final IRequest<?> request)
    {
        assignRequest(request, Collections.emptyList());
    }

    /**
     * 用于将给定请求分配给解析器的方法，考虑给定的解析器令牌黑名单。
     *
     * @param request                要分配的请求。
     * @param resolverTokenBlackList 包含解析器令牌的黑名单，当检查可能的解析器时，会跳过其中的解析器。
     * @return 分配请求的解析器的令牌，如果未找到则返回null。
     * @throws IllegalArgumentException 当请求在此管理器中未知时抛出。
     */
    @Override
    public IToken<?> assignRequest(final IRequest<?> request, final Collection<IToken<?>> resolverTokenBlackList)
    {
        switch (request.getStrategy())
        {
            case PRIORITY_BASED:
                return assignRequestDefault(request, resolverTokenBlackList);
            case FASTEST_FIRST:
            {
                Log.getLogger().warn("尚未实现最快优先策略。");
                return assignRequestDefault(request, resolverTokenBlackList);
            }
        }

        return null;
    }

    /**
     * 用于将给定请求分配给解析器的方法，考虑给定的解析器令牌黑名单。使用默认分配策略：{@link AssigningStrategy#PRIORITY_BASED}
     *
     * @param request                要分配的请求。
     * @param resolverTokenBlackList 包含解析器令牌的黑名单，当检查可能的解析器时，会跳过其中的解析器。
     * @return 分配请求的解析器的令牌，如果未找到则返回null。
     * @throws IllegalArgumentException 当请求在此管理器中未知时抛出。
     */
    @Override
    @SuppressWarnings(UNCHECKED)
    public IToken<?> assignRequestDefault(final IRequest<?> request, final Collection<IToken<?>> resolverTokenBlackList)
    {
        //检查请求是否已注册
        getRequest(request.getId());

        manager.getLogger().debug("开始为请求分配解析器搜索： " + request);

        request.setState(new WrappedStaticStateRequestManager(manager), RequestState.ASSIGNING);

        final Set<TypeToken<?>> requestTypes = request.getSuperClasses();

        final List<TypeToken<?>> typeIndexList = new LinkedList<>(requestTypes);

        final Set<IRequestResolver<?>> resolvers = requestTypes.stream()
                                                     .filter(typeToken -> manager.getRequestableTypeRequestResolverAssignmentDataStore().getAssignments().containsKey(typeToken))
                                                     .flatMap(type -> manager.getRequestableTypeRequestResolverAssignmentDataStore()
                                                                        .getAssignments()
                                                                        .get(type)
                                                                        .stream()
                                                                        .map(iToken -> manager.getResolverHandler().getResolver(iToken)))
                                                     .filter(iRequestResolver -> typeIndexList.contains(iRequestResolver.getRequestType()))
                                                     .sorted(Comparator.comparingInt((IRequestResolver<?> r) -> -1 * r.getPriority())
                                                               .thenComparingInt((IRequestResolver<?> r) -> typeIndexList.indexOf(r.getRequestType())))
                                                     .collect(Collectors.toCollection(LinkedHashSet::new));

        IRequestResolver previousResolver = null;
        int previousMetric = Integer.MAX_VALUE;
        @Nullable List<IToken<?>> attemptResult = null;
        for (@SuppressWarnings(RAWTYPES) final IRequestResolver resolver : resolvers)
        {
            //如果解析器在黑名单中，则跳过
            if (resolverTokenBlackList.contains(resolver.getId()) || manager.getResolverHandler().isBeingRemoved(resolver.getId()))
            {
                continue;
            }

            //如果初步检查失败，则跳过
            if (!resolver.canResolveRequest(manager, request))
            {
                continue;
            }

            if (previousResolver == null)
            {
                //如果尝试失败（即attemptResult == null），则跳过
                attemptResult = resolver.attemptResolveRequest(new WrappedBlacklistAssignmentRequestManager(manager, resolverTokenBlackList), request);
                if (attemptResult != null)
                {
                    previousResolver = resolver;
                    previousMetric = resolver.getSuitabilityMetric(request);
                }
                continue;
            }

            if (previousResolver.getClass().equals(resolver.getClass()))
            {
                final int currentResolverMetric = resolver.getSuitabilityMetric(request);
                if (currentResolverMetric < previousMetric)
                {
                    @Nullable List<IToken<?>> tempAttemptResolveRequest = resolver.attemptResolveRequest(new WrappedBlacklistAssignmentRequestManager(manager, resolverTokenBlackList), request);
                    if (tempAttemptResolveRequest != null)
                    {
                        previousResolver = resolver;
                        previousMetric = resolver.getSuitabilityMetric(request);
                        attemptResult = tempAttemptResolveRequest;
                    }
                }
            }
            else
            {
                break;
            }
        }

        if (previousResolver != null)
        {
            return resolve(request, previousResolver, resolverTokenBlackList, attemptResult);
        }

        return null;
    }

    /**
     * 尝试使用给定的解析器解决给定请求的方法。
     * @param request 要完成的请求。
     * @param resolver 要使用的解析器。
     * @param resolverTokenBlackList 黑名单。
     * @return 解析器令牌。
     */
    private IToken<?> resolve(final IRequest<?> request, final IRequestResolver resolver, final Collection<IToken<?>> resolverTokenBlackList, @Nullable final List<IToken<?>> attemptResult)
    {
        //成功找到解析器，注册
        manager.getLogger().debug("成功完成请求解析器分配搜索： " + request);

        manager.getResolverHandler().addRequestToResolver(resolver, request);
        //TODO: 将此false更改为模拟。
        resolver.onRequestAssigned(manager, request, false);

        for (final IToken<?> childRequestToken :
          attemptResult)
        {
            final IRequest<?> childRequest = manager.getRequestHandler().getRequest(childRequestToken);

            childRequest.setParent(request.getId());
            request.addChild(childRequest.getId());
        }

        for (final IToken<?> childRequestToken :
          attemptResult)
        {
            final IRequest<?> childRequest = manager.getRequestHandler().getRequest(childRequestToken);

            if (!isAssigned(childRequestToken))
            {
                assignRequest(childRequest, resolverTokenBlackList);
            }
        }

        if (request.getState().ordinal() < RequestState.IN_PROGRESS.ordinal())
        {
            request.setState(new WrappedStaticStateRequestManager(manager), RequestState.IN_PROGRESS);
            if (!request.hasChildren())
            {
                resolveRequest(request);
            }
        }

        return resolver.getId();
    }

    /**
     * 方法用于重新分配请求给不在给定黑名单中的解析器。在内部取消请求而不通知请求方，并尝试重新分配。
     * 如果重新分配失败，它将被重新分配给原始解析器。
     *
     * @param request                正在重新分配的请求。
     * @param resolverTokenBlackList 不要分配请求的黑名单。
     * @return 分配请求的解析器的令牌，如果未找到则返回null。
     * @throws IllegalArgumentException 当发生错误时抛出。
     */
    @Override
    public IToken<?> reassignRequest(final IRequest<?> request, final Collection<IToken<?>> resolverTokenBlackList)
    {
        if (request.hasChildren())
        {
            throw new IllegalArgumentException("无法重新分配具有子项的请求。");
        }

        final IRequestResolver currentlyAssignedResolver = manager.getResolverForRequest(request.getId());
        currentlyAssignedResolver.onAssignedRequestBeingCancelled(new WrappedStaticStateRequestManager(manager), request);

        if (manager.getRequestResolverRequestAssignmentDataStore().getAssignments().containsKey(currentlyAssignedResolver.getId()))
        {
            manager.getRequestResolverRequestAssignmentDataStore().getAssignments().get(currentlyAssignedResolver.getId()).remove(request.getId());
            if (manager.getRequestResolverRequestAssignmentDataStore().getAssignments().get(currentlyAssignedResolver.getId()).isEmpty())
            {
                manager.getRequestResolverRequestAssignmentDataStore().getAssignments().remove(currentlyAssignedResolver.getId());
            }
        }

        currentlyAssignedResolver.onAssignedRequestCancelled(new WrappedStaticStateRequestManager(manager), request);

        manager.updateRequestState(request.getId(), RequestState.REPORTED);
        IToken<?> newAssignedResolverId = assignRequest(request, resolverTokenBlackList);

        return newAssignedResolverId;
    }

    /**
     * 方法用于检查给定请求令牌是否分配给解析器。
     *
     * @param token 要检查的请求令牌。
     * @return 当请求令牌已分配时返回true，否则返回false。
     */
    @Override
    public boolean isAssigned(final IToken<?> token)
    {
        return manager.getRequestResolverRequestAssignmentDataStore().getAssignmentForValue(token) != null;
    }

    @Override
    public void onRequestResolved(final IToken<?> token)
    {
        final IRequest<?> request = getRequest(token);
        final IRequestResolver resolver = manager.getResolverHandler().getResolverForRequest(token);

        //检索后续请求。
        final List<IRequest<?>> followupRequests = resolver.getFollowupRequestForCompletion(manager, request);

        request.setState(manager, RequestState.FOLLOWUP_IN_PROGRESS);

        //将后续请求分配给父级作为子项，以保持处理被暂停。
        if (followupRequests != null && !followupRequests.isEmpty())
        {
            followupRequests.forEach(followupRequest -> request.addChild(followupRequest.getId()));
            followupRequests.forEach(followupRequest -> followupRequest.setParent(request.getId()));
        }

        //如果需要，分配后续请求
        if (followupRequests != null && !followupRequests.isEmpty() &&
              followupRequests.stream().anyMatch(followupRequest -> !isAssigned(followupRequest.getId())))
        {
            followupRequests.stream()
              .filter(followupRequest -> !isAssigned(followupRequest.getId()))
              .forEach(this::assignRequest);
        }

        //所有后续请求立即解决或没有后续请求。
        if (!request.hasChildren())
        {
            manager.updateRequestState(request.getId(), RequestState.COMPLETED);
        }
    }

    /**
     * 方法用于处理成功解决请求时的情况。
     *
     * @param token 成功完成的请求的令牌。
     */
    @Override
    public void onRequestCompleted(final IToken<?> token)
    {
        final IRequest<?> request = getRequest(token);

        request.getRequester().onRequestedRequestComplete(manager, request);

        //检查请求是否有父级，并且是否需要解决父级。
        if (request.hasParent())
        {
            final IRequest<?> parentRequest = getRequest(request.getParent());

            manager.updateRequestState(request.getId(), RequestState.RECEIVED);
            parentRequest.removeChild(request.getId());

            request.setParent(null);

            if (!parentRequest.hasChildren())
            {
                //正常处理仍在运行，我们已经收到了所有依赖项，解决父级请求。
                if (parentRequest.getState() == RequestState.IN_PROGRESS)
                {
                    resolveRequest(parentRequest);
                }
                //后续处理正在运行，我们已完成了所有后续处理，完成父级请求。
                else if (parentRequest.getState() == RequestState.FOLLOWUP_IN_PROGRESS)
                {
                    manager.updateRequestState(parentRequest.getId(), RequestState.COMPLETED);
                }
            }
        }
    }

    /**
     * 方法用于处理已覆盖或取消的请求。首先取消所有子请求，然后处理清理请求的创建。
     *
     * @param token 已取消或覆盖的请求的令牌
     */
    @Override
    @SuppressWarnings(UNCHECKED)
    public void onRequestOverruled(final IToken<?> token)
    {
        final IRequest<?> request = getRequest(token);

        if (manager.getRequestResolverRequestAssignmentDataStore().getAssignmentForValue(token) == null)
        {
            manager.getRequestIdentitiesDataStore().getIdentities().remove(token);
            return;
        }

        //让我们首先取消所有子级，否则这将变成一个大混乱。
        if (request.hasChildren())
        {
            final ImmutableCollection<IToken<?>> currentChildren = request.getChildren();
            currentChildren.forEach(this::onRequestCancelledDirectly);
        }

        final IRequestResolver resolver = manager.getResolverHandler().getResolverForRequest(token);
        //通知解析器。
        resolver.onAssignedRequestBeingCancelled(manager, request);

        //这将通知所有人:D
        manager.updateRequestState(token, RequestState.COMPLETED);

        //取消完成
        resolver.onAssignedRequestCancelled(manager, request);
    }

    /**
     * 方法用于处理已覆盖或取消的请求。首先取消所有子请求，然后处理清理请求的创建。
     *
     * @param token 已取消或覆盖的请求的令牌
     */
    @Override
    public void onRequestCancelled(final IToken<?> token)
    {
        final IRequest<?> request = manager.getRequestHandler().getRequest(token);

        if (request == null)
        {
            return;
        }

        if (request.hasParent())
        {
            this.onChildRequestCancelled(token);
        }
        else
        {
            this.onRequestCancelledDirectly(token);
        }

        manager.markDirty();
    }

    @Override
    public void onChildRequestCancelled(final IToken<?> token)
    {
        final IRequest<?> request = manager.getRequestForToken(token);
        final IRequest<?> parent = manager.getRequestForToken(request.getParent());
        parent.resetDeliveries();
        parent.getChildren().forEach(this::onRequestCancelledDirectly);
        this.reassignRequest(parent, ImmutableList.of());
    }

    @Override
    public void onRequestCancelledDirectly(final IToken<?> token)
    {
        final IRequest<?> request = manager.getRequestForToken(token);
        if (request.hasChildren())
        {
            request.getChildren().forEach(this::onRequestCancelledDirectly);
        }

        processDirectCancellationAndNotifyRequesterOf(request);

        cleanRequestData(token);
    }

    @Override
    public void processDirectCancellationAndNotifyRequesterOf(final IRequest<?> request)
    {
        processDirectCancellationOf(request);
        request.getRequester().onRequestedRequestCancelled(manager, request);
    }

    @Override
    public void processDirectCancellationOf(final IRequest<?> request)
    {
        final boolean assigned = this.isAssigned(request.getId());
        IRequestResolver resolver = null;

        if (assigned)
        {
            resolver = manager.getResolverForRequest(request.getId());
            resolver.onAssignedRequestBeingCancelled(new WrappedStaticStateRequestManager(manager), request);

            if (manager.getRequestResolverRequestAssignmentDataStore().getAssignments().containsKey(resolver.getId()))
            {
                manager.getRequestResolverRequestAssignmentDataStore().getAssignments().get(resolver.getId()).remove(request.getId());
                if (manager.getRequestResolverRequestAssignmentDataStore().getAssignments().get(resolver.getId()).isEmpty())
                {
                    manager.getRequestResolverRequestAssignmentDataStore().getAssignments().remove(resolver.getId());
                }
            }
        }

                if (request.hasParent())
        {
            getRequest(request.getParent()).removeChild(request.getId());
        }
        request.setParent(null);
        request.setState(manager, RequestState.CANCELLED);

        if (assigned)
        {
            resolver.onAssignedRequestCancelled(new WrappedStaticStateRequestManager(manager), request);
        }
    }

    /**
     * 方法用于解决请求。当调用此方法时，给定请求必须已分配。
     *
     * @param request 要解决的请求。
     * @throws IllegalArgumentException 当请求未知，未解决或无法解决时抛出。
     */
    @Override
    @SuppressWarnings(UNCHECKED)
    public void resolveRequest(final IRequest<?> request)
    {
        getRequest(request.getId());
        if (!isAssigned(request.getId()))
        {
            throw new IllegalArgumentException("给定请求未解决");
        }

        if (request.getState() != RequestState.IN_PROGRESS)
        {
            throw new IllegalArgumentException("给定请求不在正确的状态。要求：" + RequestState.IN_PROGRESS + " - 找到：" + request.getState());
        }

        if (request.hasChildren())
        {
            throw new IllegalArgumentException("无法解决具有打开子项的请求");
        }

        final IRequestResolver resolver = manager.getResolverHandler().getResolverForRequest(request);

        request.setState(new WrappedStaticStateRequestManager(manager), RequestState.IN_PROGRESS);
        resolver.resolveRequest(manager, request);
    }

    /**
     * 当给定的管理器被通知其请求方接收到给定任务时，调用此方法。此时所有与解析器的通信应中止，因此需要处理取消和取消。
     * 在调用此方法之前。
     *
     * @param token 请求的令牌。
     * @throws IllegalArgumentException 当令牌未知时抛出。
     */
    @Override
    public void cleanRequestData(final IToken<?> token)
    {
        manager.getLogger().debug("从管理器中删除 " + token + "，因为它已完成并且其包已被请求方接收到。");
        getRequest(token);

        if (isAssigned(token))
        {
            final IRequestResolver<?> resolver = manager.getResolverHandler().getResolverForRequest(token);
            manager.getRequestResolverRequestAssignmentDataStore().getAssignments().get(resolver.getId()).remove(token);
            if (manager.getRequestResolverRequestAssignmentDataStore().getAssignments().get(resolver.getId()).isEmpty())
            {
                manager.getRequestResolverRequestAssignmentDataStore().getAssignments().remove(resolver.getId());
            }
        }

        manager.getRequestIdentitiesDataStore().getIdentities().remove(token);
    }

    @Override
    public void removeRequester(final IRequester requester)
    {
        for (final IRequest<?> req : new ArrayList<>(manager.getRequestIdentitiesDataStore().getIdentities().values()))
        {
            if (req.getRequester().getId().equals(requester.getId()))
            {
                onRequestCancelled(req.getId());
            }
        }
    }

    /**
     * 用于获取注册请求的方法，从给定令牌获取。
     *
     * @param token 要查询的令牌
     * @throws IllegalArgumentException 当令牌对给定管理器是未知的时抛出。
     */
    @Override
    public IRequest<?> getRequest(final IToken<?> token)
    {
        if (!manager.getRequestIdentitiesDataStore().getIdentities().containsKey(token))
        {
            throw new IllegalArgumentException("给定令牌未注册为此管理器的请求");
        }

        return getRequestOrNull(token);
    }

    /**
     * 用于获取注册请求的方法，从给定令牌获取。
     *
     * @param token 要获取请求的令牌。
     * @return 请求或者当没有具有该令牌的请求时返回null。
     */
    @Override
    public IRequest<?> getRequestOrNull(final IToken<?> token)
    {
        manager.getLogger().debug("检索： " + token + " 的请求");

        return manager.getRequestIdentitiesDataStore().getIdentities().get(token);
    }

    /**
     * 返回由特定请求者发出的所有请求。
     *
     * @param requester 相关请求者。
     * @return 包含由给定请求者发出的请求实例的集合。
     */
    @Override
    public Collection<IRequest<?>> getRequestsMadeByRequester(final IRequester requester)
    {
        return manager.getRequestIdentitiesDataStore()
                 .getIdentities()
                 .values()
                 .stream()
                 .filter(iRequest -> iRequest.getRequester().getId().equals(requester.getId()))
                 .collect(Collectors.toList());
    }
}
