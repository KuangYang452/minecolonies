package com.minecolonies.api.colony;

import com.minecolonies.api.colony.interactionhandling.IInteractionResponseHandler;
import com.minecolonies.api.colony.requestsystem.requestable.IRequestable;
import com.minecolonies.api.colony.requestsystem.token.IToken;
import com.minecolonies.api.entity.citizen.AbstractCivilianEntity;
import com.minecolonies.api.entity.citizen.AbstractEntityCitizen;
import net.minecraft.world.entity.player.Player;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraftforge.common.util.INBTSerializable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * 殖民地中所有平民的数据，可以是市民/商人/访客等
 */
public interface ICivilianData extends ICitizen, INBTSerializable<CompoundTag>
{
    /**
     * 返回平民数据的实体实例。在需要时重新生成平民。
     *
     * @return 平民数据的 {@link AbstractEntityCitizen}。
     */

    /**
     * 设置平民数据的实体。
     *
     * @param civilian 平民数据的 {@link AbstractCivilianEntity} 实例。
     */
    void setEntity(@Nullable AbstractCivilianEntity civilian);

    /**
     * 标记实例为脏。
     */
    void markDirty();

    /**
     * 返回市民所属的殖民地。
     *
     * @return 市民所属的殖民地。
     */
    IColony getColony();

    /**
     * 初始化一个新的市民，当未从NBT读取时。
     */
    void initForNewCivilian();

    /**
     * 从市民数据初始化实体的值。
     */
    void initEntityValues();

    /**
     * 设置性别并生成新的姓名
     * @param isFemale 是否为女性
     */
    void setGenderAndGenerateName(@NotNull boolean isFemale);

    /**
     * 设置性别
     *
     * @param isFemale 是否为女性
     */
    void setGender(boolean isFemale);

    /**
     * 返回平民的纹理ID。
     *
     * @return 纹理ID。
     */
    int getTextureId();

    /**
     * 返回实例是否为脏。
     *
     * @return 当脏时为true，否则为false。
     */
    boolean isDirty();

    /**
     * 清除实例的脏标记。
     */
    void clearDirty();

    /**
     * 如有必要，更新 {@link AbstractCivilianEntity} 的实例。
     */
    void updateEntityIfNecessary();

    /**
     * 将市民数据写入字节缓冲以进行传输。
     *
     * @param buf 要写入的缓冲区。
     */
    void serializeViewNetworkData(@NotNull FriendlyByteBuf buf);

    /**
     * 获取饱和度。
     *
     * @param extraSaturation 要增加的额外饱和度
     */
    void increaseSaturation(double extraSaturation);

    /**
     * 获取饱和度。
     *
     * @param extraSaturation 要移除的饱和度
     */
    void decreaseSaturation(double extraSaturation);

    /**
     * 设置市民姓名。
     *
     * @param name 要设置的姓名。
     */
    void setName(String name);

    /**
     * 创建一个阻塞请求。
     *
     * @param requested 要创建的请求。
     * @param <R> 请求的类型
     * @return 请求的令牌。
     */
    <R extends IRequestable> IToken<?> createRequest(@NotNull R requested);

    /**
     * 创建一个异步请求。
     *
     * @param requested 要创建的请求。
     * @param <R> 请求的类型
     * @return 请求的令牌。
     */
    <R extends IRequestable> IToken<?> createRequestAsync(@NotNull R requested);

    /**
     * 当请求被取消时调用。
     *
     * @param token 要取消的令牌。
     */
    void onRequestCancelled(@NotNull IToken<?> token);

    /**
     * 检查请求是否是异步的。
     *
     * @param token 要检查的令牌。
     * @return 如果是异步则返回true。
     */
    boolean isRequestAsync(@NotNull IToken<?> token);

    /**
     * 在服务器端触发响应。
     *
     * @param key 组件的键。
     * @param response 触发的响应。
     * @param player 触发所在的玩家。
     */
    void onResponseTriggered(@NotNull Component key, @NotNull Component response, Player player);

    /**
     * 更新数据以更新值。
     */
    void tick();

    /**
     * 触发可能的交互。
     *
     * @param handler 新的处理器。
     */
    void triggerInteraction(@NotNull IInteractionResponseHandler handler);

    /**
     * 获取纹理后缀。
     *
     * @return 后缀。
     */
    String getTextureSuffix();

    /**
     * 设置纹理后缀。
     *
     * @param suffix 要设置的后缀。
     */
    void setSuffix(String suffix);

    /**
     * 获取实体。
     *
     * @return
     */
    Optional<? extends AbstractCivilianEntity> getEntity();
}
