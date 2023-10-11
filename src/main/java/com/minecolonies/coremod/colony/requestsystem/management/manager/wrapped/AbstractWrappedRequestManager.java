package com.minecolonies.coremod.colony.requestsystem.management.manager.wrapped;

import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.requestsystem.data.IDataStoreManager;
import com.minecolonies.api.colony.requestsystem.factory.IFactoryController;
import com.minecolonies.api.colony.requestsystem.manager.IRequestManager;
import com.minecolonies.api.colony.requestsystem.request.IRequest;
import com.minecolonies.api.colony.requestsystem.request.RequestState;
import com.minecolonies.api.colony.requestsystem.requestable.IRequestable;
import com.minecolonies.api.colony.requestsystem.requester.IRequester;
import com.minecolonies.api.colony.requestsystem.resolver.IRequestResolver;
import com.minecolonies.api.colony.requestsystem.resolver.IRequestResolverProvider;
import com.minecolonies.api.colony.requestsystem.resolver.player.IPlayerRequestResolver;
import com.minecolonies.api.colony.requestsystem.resolver.retrying.IRetryingRequestResolver;
import com.minecolonies.api.colony.requestsystem.token.IToken;
import com.minecolonies.coremod.colony.requestsystem.management.IStandardRequestManager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.function.Predicate;

/**
 * 包装类，用于管理请求的管理器。此类的子类在至少一个方法上具有自定义行为。
 */
public abstract class AbstractWrappedRequestManager implements IRequestManager
{
    @NotNull
    protected final IStandardRequestManager wrappedManager;

    public AbstractWrappedRequestManager(@NotNull final IStandardRequestManager wrappedManager)
    {
        this.wrappedManager = wrappedManager;
    }

    /**
     * 该管理器管理的殖民地的信息。
     *
     * @return 该管理器管理的殖民地。
     */
    @NotNull
    @Override
    public IColony getColony()
    {
        return wrappedManager.getColony();
    }

    /**
     * 获取RequestManager的FactoryController的方法。
     *
     * @return 该RequestManager的FactoryController。
     */
    @NotNull
    @Override
    public IFactoryController getFactoryController()
    {
        return wrappedManager.getFactoryController();
    }

    /**
     * 为给定对象创建请求的方法。
     *
     * @param requester 请求者。
     * @param object    被请求的对象。
     * @return 代表请求的令牌。
     * @throws IllegalArgumentException 当此管理器无法为给定类型生成请求时抛出。
     */
    @NotNull
    @Override
    public <T extends IRequestable> IToken<?> createRequest(@NotNull final IRequester requester, @NotNull final T object) throws IllegalArgumentException
    {
        return wrappedManager.createRequest(requester, object);
    }

    /**
     * 分配请求给解析器的方法。
     *
     * @param token 要分配的请求的令牌。
     * @throws IllegalArgumentException 当令牌未注册到请求或已分配给解析器时抛出。
     */
    @Override
    public void assignRequest(@NotNull final IToken<?> token) throws IllegalArgumentException
    {
        wrappedManager.assignRequest(token);
    }

    /**
     * 创建并立即分配请求的方法。
     *
     * @param requester 请求者。
     * @param object    请求的对象。
     * @return 代表请求的令牌。
     * @throws IllegalArgumentException 当createRequest或assignRequest抛出IllegalArgumentException时抛出。
     */
    @NotNull
    @Override
    public <T extends IRequestable> IToken<?> createAndAssignRequest(@NotNull final IRequester requester, @NotNull final T object) throws IllegalArgumentException
    {
        final IToken<?> token = createRequest(requester, object);
        assignRequest(token);
        return token;
    }

    @Override
    public IToken<?> reassignRequest(@NotNull final IToken<?> token, @NotNull final Collection<IToken<?>> resolverTokenBlackList) throws IllegalArgumentException
    {
        return wrappedManager.reassignRequest(token, resolverTokenBlackList);
    }

    /**
     * 获取给定令牌的请求的方法。
     *
     * @param token 要获取请求的令牌。
     * @return 该令牌的请求的类型。
     * @throws IllegalArgumentException 当没有该令牌的请求或令牌不产生给定类型T的请求时抛出。
     */
    @Nullable
    @Override
    public IRequest<?> getRequestForToken(@NotNull final IToken<?> token) throws IllegalArgumentException
    {
        return wrappedManager.getRequestHandler().getRequestOrNull(token);
    }

    /**
     * 从令牌获取解析器的方法。
     *
     * @param token 令牌。
     * @return 使用给定令牌注册的解析器。
     * @throws IllegalArgumentException 当令牌未知时抛出。
     */
    @NotNull
    @Override
    public IRequestResolver<?> getResolverForToken(@NotNull final IToken<?> token) throws IllegalArgumentException
    {
        return wrappedManager.getResolverForToken(token);
    }

    /**
     * 为给定请求获取解析器的方法。
     *
     * @param requestToken 获取解析器的请求的令牌。
     * @return 如果请求尚未解析，则返回null，否则返回分配的解析器。
     * @throws IllegalArgumentException 当令牌未知时抛出。
     */
    @Nullable
    @Override
    public IRequestResolver<?> getResolverForRequest(@NotNull final IToken<?> requestToken) throws IllegalArgumentException
    {
        return wrappedManager.getResolverForRequest(requestToken);
    }

    /**
     * 更新给定请求的状态的方法。
     *
     * @param token 代表要更新的请求的令牌。
     * @param state 请求的新状态。
     * @throws IllegalArgumentException 当此管理器未知令牌时抛出。
     */
    @Override
    public void updateRequestState(@NotNull final IToken<?> token, @NotNull final RequestState state) throws IllegalArgumentException
    {
        wrappedManager.updateRequestState(token, state);
    }

    @Override
    public void overruleRequest(@NotNull final IToken<?> token, @Nullable final ItemStack stack) throws IllegalArgumentException
    {
        wrappedManager.overruleRequest(token, stack);
    }

    /**
     * 通知此管理器殖民地已添加新提供者的方法。
     *
     * @param provider 新提供者。
     * @throws IllegalArgumentException 当已经注册具有相同令牌的提供者时抛出。
     */
    @Override
    public void onProviderAddedToColony(@NotNull final IRequestResolverProvider provider) throws IllegalArgumentException
    {
        wrappedManager.onProviderAddedToColony(provider);
    }

    /**
     * 通知此管理器殖民地已从提供者中移除的方法。
     *
     * @param provider 被移除的提供者。
     * @throws IllegalArgumentException 当没有注册具有相同令牌的提供者时抛出。
     */
    @Override
    public void onProviderRemovedFromColony(@NotNull final IRequestResolverProvider provider) throws IllegalArgumentException
    {
        wrappedManager.onProviderRemovedFromColony(provider);
    }

    @Override
    public void onRequesterRemovedFromColony(@NotNull final IRequester requester) throws IllegalArgumentException
    {
        wrappedManager.onRequesterRemovedFromColony(requester);
    }

    @NotNull
    @Override
    public IPlayerRequestResolver getPlayerResolver()
    {
        return wrappedManager.getPlayerResolver();
    }

    @NotNull
    @Override
    public IRetryingRequestResolver getRetryingRequestResolver()
    {
        return wrappedManager.getRetryingRequestResolver();
    }

    @Override
    public CompoundTag serializeNBT()
    {
        return wrappedManager.serializeNBT();
    }

    @Override
    public void deserializeNBT(final CompoundTag nbt)
    {
        wrappedManager.deserializeNBT(nbt);
    }

    @Override
    public void serialize(IFactoryController controller, FriendlyByteBuf buffer)
    {
        wrappedManager.serialize(controller, buffer);
    }

    @Override
    public void deserialize(IFactoryController controller, FriendlyByteBuf buffer)
    {
        wrappedManager.deserialize(controller, buffer);
    }

    @Override
    public void tick()
    {
        wrappedManager.tick();
    }

    @NotNull
    @Override
    public IDataStoreManager getDataStoreManager()
    {
        return wrappedManager.getDataStoreManager();
    }

    @Override
    public void reset()
    {
        wrappedManager.reset();
    }

    @Override
    public boolean isDirty()
    {
        return wrappedManager.isDirty();
    }

    @Override
    public void setDirty(final boolean isDirty)
    {
        wrappedManager.setDirty(isDirty);
    }

    @Override
    public void markDirty()
    {
        wrappedManager.markDirty();
    }

    @Override
    public void onColonyUpdate(@NotNull final Predicate<IRequest<?>> shouldTriggerReassign)
    {
        throw new UnsupportedOperationException("包装请求管理器不能使用此方法！");
    }

    @Override
    public Logger getLogger()
    {
        return wrappedManager.getLogger();
    }
}
