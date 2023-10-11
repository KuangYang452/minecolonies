package com.minecolonies.coremod.colony.requestsystem.management.manager.wrapped;

import com.minecolonies.api.colony.requestsystem.token.IToken;
import com.minecolonies.coremod.colony.requestsystem.management.IStandardRequestManager;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

/**
 * 用于处理内部重新分配更改的类。在分配请求时，考虑给定的黑名单。
 */
public final class WrappedBlacklistAssignmentRequestManager extends AbstractWrappedRequestManager
{

    @NotNull
    private final Collection<IToken<?>> blackListedResolvers;

    public WrappedBlacklistAssignmentRequestManager(@NotNull final IStandardRequestManager wrappedManager, @NotNull final Collection<IToken<?>> blackListedResolvers)
    {
        super(wrappedManager);
        this.blackListedResolvers = blackListedResolvers;
    }

    /**
     * 用于将请求分配给解析器的方法。
     *
     * @param token 要分配的请求的令牌。
     * @throws IllegalArgumentException 当令牌未注册到请求或已分配给解析器时抛出。
     */
    @Override
    public void assignRequest(@NotNull final IToken<?> token) throws IllegalArgumentException
    {
        wrappedManager.getRequestHandler().assignRequest(wrappedManager.getRequestHandler().getRequest(token), blackListedResolvers);
    }
}
