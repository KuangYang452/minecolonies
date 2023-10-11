package com.minecolonies.coremod.colony.requestsystem.management.manager;

import com.google.common.collect.ImmutableList;
import com.google.common.reflect.TypeToken;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.requestsystem.StandardFactoryController;
import com.minecolonies.api.colony.requestsystem.data.*;
import com.minecolonies.api.colony.requestsystem.factory.IFactoryController;
import com.minecolonies.api.colony.requestsystem.management.*;
import com.minecolonies.api.colony.requestsystem.management.update.UpdateType;
import com.minecolonies.api.colony.requestsystem.request.IRequest;
import com.minecolonies.api.colony.requestsystem.request.RequestState;
import com.minecolonies.api.colony.requestsystem.requestable.IRequestable;
import com.minecolonies.api.colony.requestsystem.requester.IRequester;
import com.minecolonies.api.colony.requestsystem.resolver.IRequestResolver;
import com.minecolonies.api.colony.requestsystem.resolver.IRequestResolverProvider;
import com.minecolonies.api.colony.requestsystem.resolver.player.IPlayerRequestResolver;
import com.minecolonies.api.colony.requestsystem.resolver.retrying.IRetryingRequestResolver;
import com.minecolonies.api.colony.requestsystem.token.IToken;
import com.minecolonies.api.util.ItemStackUtils;
import com.minecolonies.api.util.constant.Constants;
import com.minecolonies.api.util.constant.TypeConstants;
import com.minecolonies.coremod.colony.requestsystem.management.IStandardRequestManager;
import com.minecolonies.coremod.colony.requestsystem.management.handlers.*;
import com.minecolonies.coremod.colony.requestsystem.management.manager.wrapped.WrappedStaticStateRequestManager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static com.minecolonies.api.util.constant.Suppression.BIG_CLASS;

/**
 * 请求系统的主要类。实现了IRequestManager接口的默认实现。
 * <p>
 * 使用
 */

@SuppressWarnings(BIG_CLASS)
public class StandardRequestManager implements IStandardRequestManager
{
    ////---------------------------NBTTags-------------------------\\\\
    private static final String NBT_DATASTORE                       = "DataStores";
    private static final String NBT_ID_REQUEST_IDENTITIES           = "RequestIdentitiesStoreId";
    private static final String NBT_ID_REQUEST_RESOLVER_IDENTITIES  = "RequestResolverIdentitiesStoreId";
    private static final String NBT_ID_PROVIDER_ASSIGNMENTS         = "ProviderAssignmentsStoreId";
    private static final String NBT_ID_REQUEST_RESOLVER_ASSIGNMENTS = "RequestResolverAssignmentsStoreId";
    private static final String NBT_ID_REQUESTABLE_TYPE_ASSIGNMENTS = "RequestableTypeAssignmentsStoreId";
    private static final String NBT_ID_PLAYER                       = "PlayerRequestResolverId";
    private static final String NBT_ID_RETRYING                     = "RetryingRequestResolverId";
    private static final String NBT_VERSION                         = "Version";
    ////---------------------------NBTTags-------------------------\\\\

    private IToken<?> requestIdentitiesDataStoreId;

    private IToken<?> requestResolverIdentitiesDataStoreId;

    private IToken<?> providerRequestResolverAssignmentDataStoreId;

    private IToken<?> requestResolverRequestAssignmentDataStoreId;

    private IToken<?> requestableTypeRequestResolverAssignmentDataStoreId;

    private IToken<?> playerRequestResolverId;

    private IToken<?> retryingRequestResolverId;

    private IDataStoreManager dataStoreManager;

    /**
     * 描述请求管理器本身是否已更改的变量。
     */
    private boolean dirty = true;

    /**
     * 管理器的群体。
     */
    @NotNull
    private final IColony colony;

    @NotNull
    private final Logger logger;

    @NotNull
    private final IUpdateHandler updateHandler = new UpdateHandler(this);

    @NotNull
    private final ITokenHandler tokenHandler = new TokenHandler(this);

    @NotNull
    private final IResolverHandler resolverHandler = new ResolverHandler(this);

    @NotNull
    private final IRequestHandler requestHandler = new RequestHandler(this);

    @NotNull
    private final IProviderHandler providerHandler = new ProviderHandler(this);

    private int version = -1;

    public StandardRequestManager(@NotNull final IColony colony)
    {
        this.colony = colony;
        this.logger = LogManager.getLogger(String.format("%s.requestsystem.%s", Constants.MOD_ID, colony.getID()));
        reset();
    }

    private void setup()
    {
        dataStoreManager = StandardFactoryController.getInstance().getNewInstance(TypeConstants.DATA_STORE_MANAGER);

        requestIdentitiesDataStoreId = registerDataStore(TypeConstants.REQUEST_IDENTITIES_DATA_STORE);
        requestResolverIdentitiesDataStoreId = registerDataStore(TypeConstants.REQUEST_RESOLVER_IDENTITIES_DATA_STORE);
        providerRequestResolverAssignmentDataStoreId = registerDataStore(TypeConstants.PROVIDER_REQUEST_RESOLVER_ASSIGNMENT_DATA_STORE);
        requestResolverRequestAssignmentDataStoreId = registerDataStore(TypeConstants.REQUEST_RESOLVER_REQUEST_ASSIGNMENT_DATA_STORE);
        requestableTypeRequestResolverAssignmentDataStoreId = registerDataStore(TypeConstants.REQUESTABLE_TYPE_REQUEST_RESOLVER_ASSIGNMENT_DATA_STORE);

        final IRequestResolver<?> playerRequestResolver = StandardFactoryController.getInstance().getNewInstance(TypeConstants.PLAYER_REQUEST_RESOLVER, this);
        final IRequestResolver<?> retryingRequestResolver = StandardFactoryController.getInstance().getNewInstance(TypeConstants.RETRYING_REQUEST_RESOLVER, this);

        getResolverHandler().registerResolver(playerRequestResolver);
        getResolverHandler().registerResolver(retryingRequestResolver);

        this.playerRequestResolverId = playerRequestResolver.getId();
        this.retryingRequestResolverId = retryingRequestResolver.getId();
    }

    private IToken<?> registerDataStore(TypeToken<? extends IDataStore> typeToken)
    {
        return dataStoreManager.get(StandardFactoryController.getInstance().getNewInstance(TypeConstants.ITOKEN), typeToken)
                 .getId();
    }

    /**
     * 由此管理器管理请求的群体。
     *
     * @return 由此管理器管理请求的群体。
     */
    @NotNull
    @Override
    public IColony getColony()
    {
        return colony;
    }

    /**
     * 用于获取RequestManager的FactoryController的方法。
     *
     * @return 该RequestManager的FactoryController。
     */
    @NotNull
    @Override
    public IFactoryController getFactoryController()
    {
        return StandardFactoryController.getInstance();
    }

    /**
     * 用于为给定对象创建请求的方法。
     *
     * @param requester 请求者。
     * @param object    正在请求的对象。
     * @return 表示请求的令牌。
     * @throws IllegalArgumentException 当此管理器无法为给定类型生成请求时抛出。
     */
    @NotNull
    @Override
    public <T extends IRequestable> IToken<?> createRequest(@NotNull final IRequester requester, @NotNull final T object)
    {
        final IRequest<T> request = getRequestHandler().createRequest(requester, object);
        markDirty();
        return request.getId();
    }

    /**
     * 标记请求管理器和群体为脏的方法。
     */
    @Override
    public void markDirty()
    {
        this.setDirty(true);
    }

    /**
     * 检查请求管理器是否脏。
     *
     * @return 如果是，则返回true。
     */
    @Override
    public boolean isDirty()
    {
        return dirty;
    }

    @Override
    public void setDirty(final boolean isDirty)
    {
        this.dirty = isDirty;

        if (this.isDirty())
        {
            colony.markDirty();
        }
    }

    /**
     * 用于分配请求给解析器的方法。
     *
     * @param token 要分配的请求的令牌。
     * @throws IllegalArgumentException 当令牌未注册到请求或已分配给解析器时抛出。
     */
    @Override
    public void assignRequest(@NotNull final IToken<?> token)
    {
        getRequestHandler().assignRequest(getRequestHandler().getRequest(token));
        markDirty();
    }

    /**
     * 用于创建并立即分配请求的方法。
     *
     * @param requester 请求者。
     * @param object    可请求的对象。
     * @return 表示请求的令牌。
     * @throws IllegalArgumentException 当createRequest或assignRequest抛出IllegalArgumentException时抛出。
     */
    @NotNull
    @Override
    public <T extends IRequestable> IToken<?> createAndAssignRequest(@NotNull final IRequester requester, @NotNull final T object)
    {
        final IToken<?> token = createRequest(requester, object);
        assignRequest(token);
        return token;
    }

    @Override
    @Nullable
    public IToken<?> reassignRequest(@NotNull final IToken<?> token, @NotNull final Collection<IToken<?>> resolverTokenBlackList)
    {
        final IRequest<?> request = getRequestHandler().getRequest(token);
        markDirty();
        return getRequestHandler().reassignRequest(request, resolverTokenBlackList);
    }

    @Nullable
    @Override
    public IRequest<?> getRequestForToken(@NotNull final IToken<?> token) throws IllegalArgumentException
    {
        final IRequest<?> internalRequest = getRequestHandler().getRequestOrNull(token);

        return internalRequest;
    }

    @NotNull
    @Override
    public IRequestResolver<?> getResolverForToken(@NotNull final IToken<?> token) throws IllegalArgumentException
    {
        return getResolverHandler().getResolver(token);
    }

    @Nullable
    @Override
    public IRequestResolver<?> getResolverForRequest(@NotNull final IToken<?> requestToken) throws IllegalArgumentException
    {
        final IRequest<?> request = getRequestHandler().getRequest(requestToken);

        return getResolverForToken(getResolverHandler().getResolverForRequest(request).getId());
    }

    /**
     * 用于更新给定请求的状态的方法。
     *
     * @param token 表示要更新的给定请求的令牌。
     * @param state 请求的新状态。
     * @throws IllegalArgumentException 当令牌对此管理器未知时抛出。
     */
    @Override
    public void updateRequestState(@NotNull final IToken<?> token, @NotNull final RequestState state)
    {
        final IRequest<?> request = getRequestHandler().getRequest(token);

        getLogger().debug("从：" + token + " 更新请求状态。原始状态：" + request.getState() + " 更新后状态：" + state);

        request.setState(new WrappedStaticStateRequestManager(this), state);
        markDirty();

        switch (request.getState())
        {
            case RESOLVED:
                getLogger().debug("请求已解决：" + token + "。确定后续请求...");
                getRequestHandler().onRequestResolved(token);
                return;
            case COMPLETED:
                getLogger().debug("请求已完成：" + token + "。通知父级和请求者...");
                getRequestHandler().onRequestCompleted(token);
                return;
            case OVERRULED:
                getLogger().debug("请求已被否决：" + token + "。通知父级、子级和请求者...");
                getRequestHandler().onRequestOverruled(token);
                return;
            case FAILED:
                getLogger().debug("请求失败：" + token + "。通知父级、子级和请求者...");
                getRequestHandler().onRequestCancelled(token);
                return;
            case CANCELLED:
                getLogger().debug("请求已取消：" + token + "。通知父级、子级和请求者...");
                getRequestHandler().onRequestCancelled(token);
                return;
            case RECEIVED:
                getLogger().debug("请求已接收：" + token + "。从系统中移除...");
                getRequestHandler().cleanRequestData(token);
                return;
            default:
        }
    }

    /**
     * 用于指示向此管理器添加新提供者的方法。
     *
     * @param provider 新提供者。
     */
    @Override
    public void onProviderAddedToColony(@NotNull final IRequestResolverProvider provider)
    {
        getProviderHandler().registerProvider(provider);
    }

    @Override
    public void overruleRequest(@NotNull final IToken<?> token, @Nullable final ItemStack stack)
    {
        final IRequest<?> request = getRequestHandler().getRequest(token);

        if (!ItemStackUtils.isEmpty(stack))
        {
            request.overrideCurrentDeliveries(ImmutableList.of(stack));
        }

        updateRequestState(token, RequestState.OVERRULED);
    }

    /**
     * 用于指示从群体中移除提供者的方法。
     *
     * @param provider 被移除的提供者。
     */
    @Override
    public void onProviderRemovedFromColony(@NotNull final IRequestResolverProvider provider) throws IllegalArgumentException
    {
        getProviderHandler().removeProvider(provider);
    }

    @Override
    public void onRequesterRemovedFromColony(@NotNull final IRequester requester) throws IllegalArgumentException
    {
        getRequestHandler().removeRequester(requester);
    }

    /**
     * 基于谓词重新分配请求的方法。
     *
     * @param shouldTriggerReassign 用于确定是否应重新分配请求的谓词。
     */
    @Override
    public void onColonyUpdate(@NotNull final Predicate<IRequest<?>> shouldTriggerReassign)
    {
        getResolverHandler().onColonyUpdate(shouldTriggerReassign);
    }

    /**
     * 获取玩家解析器。
     *
     * @return 玩家解析器对象。
     */
    @NotNull
    @Override
    public IPlayerRequestResolver getPlayerResolver()
    {
        return (IPlayerRequestResolver) getResolverHandler().getResolver(playerRequestResolverId);
    }

    @NotNull
    @Override
    public IRetryingRequestResolver getRetryingRequestResolver()
    {
        return (IRetryingRequestResolver) getResolverHandler().getResolver(retryingRequestResolverId);
    }

    @NotNull
    @Override
    public IDataStoreManager getDataStoreManager()
    {
        return dataStoreManager;
    }

    @Override
    public void reset()
    {
        this.reset(UpdateType.RESET);
    }

    private void reset(UpdateType type)
    {
        setup();

        version = -1;
        getUpdateHandler().handleUpdate(UpdateType.RESET);
    }

    /**
     * 用于将当前请求系统序列化为NBT的方法。
     *
     * @return 描述当前请求系统的NBTData
     */
    @Override
    public CompoundTag serializeNBT()
    {
        final CompoundTag systemCompound = new CompoundTag();
        systemCompound.putInt(NBT_VERSION, version);

        systemCompound.put(NBT_DATASTORE, getFactoryController().serialize(dataStoreManager));
        systemCompound.put(NBT_ID_REQUEST_IDENTITIES, getFactoryController().serialize(requestIdentitiesDataStoreId));
        systemCompound.put(NBT_ID_REQUEST_RESOLVER_IDENTITIES, getFactoryController().serialize(requestResolverIdentitiesDataStoreId));
        systemCompound.put(NBT_ID_PROVIDER_ASSIGNMENTS, getFactoryController().serialize(providerRequestResolverAssignmentDataStoreId));
        systemCompound.put(NBT_ID_REQUEST_RESOLVER_ASSIGNMENTS, getFactoryController().serialize(requestResolverRequestAssignmentDataStoreId));
        systemCompound.put(NBT_ID_REQUESTABLE_TYPE_ASSIGNMENTS, getFactoryController().serialize(requestableTypeRequestResolverAssignmentDataStoreId));

        systemCompound.put(NBT_ID_PLAYER, getFactoryController().serialize(playerRequestResolverId));
        systemCompound.put(NBT_ID_RETRYING, getFactoryController().serialize(retryingRequestResolverId));

        return systemCompound;
    }

    /**
     * 用于将给定nbt标签内的数据反序列化到此请求系统的方法。
     *
     * @param nbt 要反序列化的数据。
     */
    @Override
    public void deserializeNBT(final CompoundTag nbt)
    {
        executeDeserializationStepOrMarkForUpdate(nbt,
          NBT_VERSION,
          CompoundTag::getInt,
          v -> version = v);

        executeDeserializationStepOrMarkForUpdate(nbt,
          NBT_DATASTORE,
          CompoundTag::getCompound,
          c -> dataStoreManager = getFactoryController().deserialize(c));

        executeDeserializationStepOrMarkForUpdate(nbt,
          NBT_ID_REQUEST_IDENTITIES,
          CompoundTag::getCompound,
          c -> requestIdentitiesDataStoreId = getFactoryController().deserialize(c));
        executeDeserializationStepOrMarkForUpdate(nbt,
          NBT_ID_REQUEST_RESOLVER_IDENTITIES,
          CompoundTag::getCompound,
          c -> requestResolverIdentitiesDataStoreId = getFactoryController().deserialize(c));
        executeDeserializationStepOrMarkForUpdate(nbt,
          NBT_ID_PROVIDER_ASSIGNMENTS,
          CompoundTag::getCompound,
          c -> providerRequestResolverAssignmentDataStoreId = getFactoryController().deserialize(c));
        executeDeserializationStepOrMarkForUpdate(nbt,
          NBT_ID_REQUEST_RESOLVER_ASSIGNMENTS,
          CompoundTag::getCompound,
          c -> requestResolverRequestAssignmentDataStoreId = getFactoryController().deserialize(c));
        executeDeserializationStepOrMarkForUpdate(nbt,
          NBT_ID_REQUESTABLE_TYPE_ASSIGNMENTS,
          CompoundTag::getCompound,
          c -> requestableTypeRequestResolverAssignmentDataStoreId = getFactoryController().deserialize(c));

        executeDeserializationStepOrMarkForUpdate(nbt,
          NBT_ID_PLAYER,
          CompoundTag::getCompound,
          c -> playerRequestResolverId = getFactoryController().deserialize(c));

        executeDeserializationStepOrMarkForUpdate(nbt,
          NBT_ID_RETRYING,
          CompoundTag::getCompound,
          c -> retryingRequestResolverId = getFactoryController().deserialize(c));

        if (dataStoreManager == null)
        {
            reset();
        }

        updateIfRequired();
    }

    @Override
    public void serialize(IFactoryController controller, FriendlyByteBuf buffer)
    {
        buffer.writeInt(version);
        controller.serialize(buffer, dataStoreManager);
        controller.serialize(buffer, requestIdentitiesDataStoreId);
        controller.serialize(buffer, requestResolverIdentitiesDataStoreId);
        controller.serialize(buffer, providerRequestResolverAssignmentDataStoreId);
        controller.serialize(buffer, requestResolverRequestAssignmentDataStoreId);
        controller.serialize(buffer, requestableTypeRequestResolverAssignmentDataStoreId);
        controller.serialize(buffer, playerRequestResolverId);
        controller.serialize(buffer, retryingRequestResolverId);
    }

    @Override
    public void deserialize(IFactoryController controller, FriendlyByteBuf buffer)
    {
        version = buffer.readInt();
        dataStoreManager = controller.deserialize(buffer);
        requestIdentitiesDataStoreId = controller.deserialize(buffer);
        requestResolverIdentitiesDataStoreId = controller.deserialize(buffer);
        providerRequestResolverAssignmentDataStoreId = controller.deserialize(buffer);
        requestResolverRequestAssignmentDataStoreId = controller.deserialize(buffer);
        requestableTypeRequestResolverAssignmentDataStoreId = controller.deserialize(buffer);
        playerRequestResolverId = controller.deserialize(buffer);
        retryingRequestResolverId = controller.deserialize(buffer);
    }

    private <T> void executeDeserializationStepOrMarkForUpdate(
      @NotNull final CompoundTag nbt,
      @NotNull final String key,
      @NotNull final BiFunction<CompoundTag, String, T> extractor,
      @NotNull final Consumer<T> valueConsumer)
    {
        if (!nbt.getAllKeys().contains(key))
        {
            markForUpdate();
            return;
        }

        T base;
        try
        {
            base = extractor.apply(nbt, key);
        }
        catch (Exception ex)
        {
            markForUpdate();
            return;
        }

        valueConsumer.accept(base);
    }

    private void markForUpdate()
    {
        version = -1;
    }

    @Override
    public Logger getLogger()
    {
        return logger;
    }

    @Override
    public void tick()
    {
        this.getRetryingRequestResolver().updateManager(this);
        this.getRetryingRequestResolver().tick();
    }

    @NotNull
    @Override
    public IRequestIdentitiesDataStore getRequestIdentitiesDataStore()
    {
        return dataStoreManager.get(requestIdentitiesDataStoreId, TypeConstants.REQUEST_IDENTITIES_DATA_STORE);
    }

    @NotNull
    @Override
    public IRequestResolverIdentitiesDataStore getRequestResolverIdentitiesDataStore()
    {
        return dataStoreManager.get(requestResolverIdentitiesDataStoreId, TypeConstants.REQUEST_RESOLVER_IDENTITIES_DATA_STORE);
    }

    @NotNull
    @Override
    public IProviderResolverAssignmentDataStore getProviderResolverAssignmentDataStore()
    {
        return dataStoreManager.get(providerRequestResolverAssignmentDataStoreId, TypeConstants.PROVIDER_REQUEST_RESOLVER_ASSIGNMENT_DATA_STORE);
    }

    @NotNull
    @Override
    public IRequestResolverRequestAssignmentDataStore getRequestResolverRequestAssignmentDataStore()
    {
        return dataStoreManager.get(requestResolverRequestAssignmentDataStoreId, TypeConstants.REQUEST_RESOLVER_REQUEST_ASSIGNMENT_DATA_STORE);
    }

    @NotNull
    @Override
    public IRequestableTypeRequestResolverAssignmentDataStore getRequestableTypeRequestResolverAssignmentDataStore()
    {
        return dataStoreManager.get(requestableTypeRequestResolverAssignmentDataStoreId, TypeConstants.REQUESTABLE_TYPE_REQUEST_RESOLVER_ASSIGNMENT_DATA_STORE);
    }

    @Override
    public IProviderHandler getProviderHandler()
    {
        return providerHandler;
    }

    @Override
    public IRequestHandler getRequestHandler()
    {
        return requestHandler;
    }

    @Override
    public IResolverHandler getResolverHandler()
    {
        return resolverHandler;
    }

    @Override
    public ITokenHandler getTokenHandler()
    {
        return tokenHandler;
    }

    @Override
    public IUpdateHandler getUpdateHandler()
    {
        return updateHandler;
    }

    private void updateIfRequired()
    {
        if (version < updateHandler.getCurrentVersion())
        {
            reset(UpdateType.DATA_LOAD);
        }
    }

    @Override
    public int getCurrentVersion()
    {
        return version;
    }

    @Override
    public void setCurrentVersion(final int currentVersion)
    {
        this.version = currentVersion;
    }
}
