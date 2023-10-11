package com.minecolonies.api.colony.requestsystem.manager;

import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.requestsystem.data.IDataStoreManager;
import com.minecolonies.api.colony.requestsystem.factory.IFactoryController;
import com.minecolonies.api.colony.requestsystem.request.IRequest;
import com.minecolonies.api.colony.requestsystem.request.RequestState;
import com.minecolonies.api.colony.requestsystem.requestable.IRequestable;
import com.minecolonies.api.colony.requestsystem.requester.IRequester;
import com.minecolonies.api.colony.requestsystem.resolver.IRequestResolver;
import com.minecolonies.api.colony.requestsystem.resolver.IRequestResolverProvider;
import com.minecolonies.api.colony.requestsystem.resolver.player.IPlayerRequestResolver;
import com.minecolonies.api.colony.requestsystem.resolver.retrying.IRetryingRequestResolver;
import com.minecolonies.api.colony.requestsystem.token.IToken;
import com.minecolonies.api.tileentities.ITickable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.common.util.INBTSerializable;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.function.Predicate;

/**
 * 用于描述殖民地内请求管理器类的接口。扩展了INBTSerializable以便于从NBT中轻松读取和写入。
 */
public interface IRequestManager extends INBTSerializable<CompoundTag>, ITickable
{

    /**
     * 此管理器管理的殖民地。
     *
     * @return 此管理器管理的殖民地。
     */
    @NotNull
    IColony getColony();

    /**
     * 获取RequestManager的FactoryController的方法。
     *
     * @return 此RequestManager的FactoryController。
     */
    @NotNull
    IFactoryController getFactoryController();

    /**
     * 为给定对象创建一个请求的方法。
     *
     * @param requester 请求者。
     * @param object    被请求的对象。
     * @param <T>       请求的类型。
     * @return 代表请求的令牌。
     * @throws IllegalArgumentException 当此管理器无法为给定类型创建请求时抛出。
     */
    @NotNull
    <T extends IRequestable> IToken<?> createRequest(@NotNull IRequester requester, @NotNull T object) throws IllegalArgumentException;

    /**
     * 用于分配请求给解析器的方法。
     *
     * @param token 要分配的请求的令牌。
     * @throws IllegalArgumentException 当令牌未注册到请求或已分配给解析器时抛出。
     */
    @NotNull
    void assignRequest(@NotNull IToken<?> token) throws IllegalArgumentException;

    /**
     * 用于创建并立即分配请求的方法。
     *
     * @param requester 请求者。
     * @param object    可请求的对象。
     * @param <T>       可请求对象的类型。
     * @return 代表请求的令牌。
     * @throws IllegalArgumentException 当createRequest或assignRequest抛出IllegalArgumentException时抛出。
     */
    @NotNull
    default <T extends IRequestable> IToken<?> createAndAssignRequest(@NotNull IRequester requester, @NotNull T object) throws IllegalArgumentException
    {
        final IToken<?> token = createRequest(requester, object);
        assignRequest(token);
        return token;
    }

    /**
     * 重新分配给定请求的方法。
     *
     * @param token                  应重新分配的请求的令牌。
     * @param resolverTokenBlackList 黑名单。
     * @return 获得分配的解析器的令牌，如果找不到则返回null。
     * @throws IllegalArgumentException 当令牌未知于此管理器时抛出。
     */
    @Nullable
    IToken<?> reassignRequest(@NotNull IToken<?> token, @NotNull Collection<IToken<?>> resolverTokenBlackList) throws IllegalArgumentException;

    /**
     * 获取给定令牌的请求的方法。
     *
     * @param token 要获取请求的令牌。
     * @return 该令牌的给定类型的请求。
     * @throws IllegalArgumentException 当令牌不生成给定类型T的请求时抛出。
     */
    @Nullable
    IRequest<?> getRequestForToken(@NotNull final IToken<?> token) throws IllegalArgumentException;

    /**
     * 根据其令牌获取解析器的方法。
     *
     * @param token 令牌。
     * @return 注册到给定令牌的解析器。
     * @throws IllegalArgumentException 当令牌未知时抛出。
     */
    @NotNull
    IRequestResolver<?> getResolverForToken(@NotNull final IToken<?> token) throws IllegalArgumentException;

    /**
     * 获取给定请求的解析器的方法。
     *
     * @param requestToken 获取解析器的请求的令牌。
     * @return 如果请求尚未解析，则返回null；否则返回分配的解析器。
     * @throws IllegalArgumentException 当令牌未知时抛出。
     */
    @Nullable
    IRequestResolver<?> getResolverForRequest(@NotNull final IToken<?> requestToken) throws IllegalArgumentException;

    /**
     * 更新给定请求的状态的方法。
     *
     * @param token 代表要更新的请求的令牌。
     * @param state 请求的新状态。
     * @throws IllegalArgumentException 当令牌未知于此管理器时抛出。
     */
    @NotNull
    void updateRequestState(@NotNull IToken<?> token, @NotNull RequestState state) throws IllegalArgumentException;

    /**
     * 用于否决请求的方法。更新状态并设置交付物（如果适用）。
     *
     * @param token 要否决的请求的令牌。
     * @param stack 应视为交付的物品堆栈。如果无法交付，则为null。
     * @throws IllegalArgumentException 当令牌与请求不匹配时抛出。
     */
    void overruleRequest(@NotNull IToken<?> token, @Nullable ItemStack stack) throws IllegalArgumentException;

    /**
     * 用于通知此管理器殖民地已添加新提供者的方法。
     *
     * @param provider 新提供者。
     * @throws IllegalArgumentException 当已注册具有相同令牌的提供者时抛出。
     */
    void onProviderAddedToColony(@NotNull IRequestResolverProvider provider) throws IllegalArgumentException;

    /**
     * 用于通知此管理器殖民地已删除提供者的方法。
     *
     * @param requester 被移除的请求者。
     * @throws IllegalArgumentException 当未注册具有相同令牌的请求者时抛出。

     */
    void onRequesterRemovedFromColony(@NotNull final IRequester requester) throws IllegalArgumentException;

    /**
     * 用于通知此管理器殖民地已删除提供者的方法。
     *
     * @param provider 被移除的提供者。
     * @throws IllegalArgumentException 当未注册具有相同令牌的提供者时抛出。
     */
    void onProviderRemovedFromColony(@NotNull IRequestResolverProvider provider) throws IllegalArgumentException;

    /**
     * 用于通知殖民地已更新其可用物品的方法。
     *
     * @param shouldTriggerReassign 应分配的请求
     */
    void onColonyUpdate(@NotNull final Predicate<IRequest<?>> shouldTriggerReassign);

    /**
     * 获取玩家解析器。
     *
     * @return 玩家解析器对象。
     */
    @NotNull
    IPlayerRequestResolver getPlayerResolver();

    /**
     * 获取重试请求解析器。
     *
     * @return 重试请求解析器。
     */
    @NotNull
    IRetryingRequestResolver getRetryingRequestResolver();

    /**
     * 获取数据存储管理器。
     *
     * @return 数据存储管理器。
     */
    @NotNull
    IDataStoreManager getDataStoreManager();

    /**
     * 调用以重置RS。
     */
    void reset();

    /**
     * 检查是否脏并需要更新。
     *
     * @return 如果需要则返回true。
     */
    boolean isDirty();

    /**
     * 设置是否脏并需要更新。
     *
     * @param isDirty 如果需要则为true。
     */
    void setDirty(boolean isDirty);

    /**
     * 标记此管理器为脏。
     */
    void markDirty();

    /**
     * 获取记录器。
     *
     * @return 记录器。
     */
    Logger getLogger();

    /**
     * 将此请求管理器序列化为给定的FriendlyByteBuf。
     *
     * @param controller 控制器。
     * @param buffer     要序列化到的FriendlyByteBuf。
     */
    void serialize(final IFactoryController controller, final FriendlyByteBuf buffer);

    /**
     * 从给定的FriendlyByteBuf反序列化此请求管理器。
     *
     * @param controller 控制器。
     * @param buffer     要从中反序列化的FriendlyByteBuf。
     */
    void deserialize(final IFactoryController controller, final FriendlyByteBuf buffer);
}
