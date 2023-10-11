package com.minecolonies.api.colony.requestsystem.request;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.reflect.TypeToken;
import com.minecolonies.api.colony.IColonyView;
import com.minecolonies.api.colony.requestsystem.manager.AssigningStrategy;
import com.minecolonies.api.colony.requestsystem.manager.IRequestManager;
import com.minecolonies.api.colony.requestsystem.requestable.IRequestable;
import com.minecolonies.api.colony.requestsystem.requester.IRequester;
import com.minecolonies.api.colony.requestsystem.token.IToken;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * 用于表示对殖民地内部市场发出的请求的接口，请求类型为R，例如ItemStack、FluidStack等。
 *
 * @param <R> 请求类型，例如ItemStack、FluidStack等。
 */
public interface IRequest<R extends IRequestable>
{
    /**
     * 获取此请求的分配策略方法。
     *
     * @return 此请求的分配策略。
     */
    default AssigningStrategy getStrategy()
    {
        return AssigningStrategy.PRIORITY_BASED;
    }

    /**
     * 表示管理系统外部的请求唯一标识符的方法。
     *
     * @param <T> 泛型标记。
     * @return 表示管理系统外部的请求的标识符。
     */
    <T extends IToken<?>> T getId();

    /**
     * 用于确定此请求是哪种类型的请求的方法。只有用于此类型的RequestResolvers才会用于解析这个请求。
     *
     * @return 表示此请求类型的类。
     */
    @NotNull
    TypeToken<? extends R> getType();

    /**
     * 返回请求的当前状态。
     *
     * @return 当前状态。
     */
    @NotNull
    RequestState getState();

    /**
     * 设置此请求的当前状态的方法。不推荐从请求管理系统之外调用此方法。
     *
     * @param manager 请求管理器。
     * @param state   此请求的新状态。
     */
    void setState(@NotNull IRequestManager manager, @NotNull RequestState state);

    /**
     * 请求此请求的殖民地元素。
     *
     * @return 此请求的请求者。
     */
    @NotNull
    IRequester getRequester();

    /**
     * 返回实际请求的对象的方法。RequestResolver可以根据需要比较此对象。
     *
     * @return 实际请求的对象。
     */
    @NotNull
    R getRequest();

    /**
     * 返回此请求的结果。
     *
     * @return 此请求的结果，如果不可用则返回null。
     */
    @Nullable
    R getResult();

    /**
     * 设置请求的结果的方法。
     *
     * @param result 此请求的新结果。
     */
    void setResult(@NotNull R result);

    /**
     * 用于检查结果是否已设置的方法。
     *
     * @return 当结果已设置时返回true，否则返回false。
     */
    boolean hasResult();

    /**
     * 返回此请求的父级。如果设置了父级，则表示此请求是请求链的一部分。
     *
     * @param <T> 泛型标记。
     * @return 此请求的父级，如果没有父级则返回null。
     */
    @Nullable
    <T extends IToken<?>> T getParent();

    /**
     * 设置请求的父级的方法。
     *
     * @param <T> 泛型标记。
     * @param parent 新父级，如果要清除现有的父级则传入null。
     */
    <T extends IToken<?>> void setParent(@Nullable T parent);

    /**
     * 如果此请求有父级则返回true，否则返回false。
     *
     * @return 如果此请求有父级则返回true，否则返回false。
     */
    boolean hasParent();

    /**
     * 添加单个子请求的方法。
     *
     * @param <T> 泛型标记。
     * @param child 要添加的新子请求。
     */
    <T extends IToken<?>> void addChild(@NotNull T child);

    /**
     * 一次性添加多个子请求的方法。
     *
     * @param <T> 泛型标记。
     * @param children 要添加的多个子请求。
     */
    <T extends IToken<?>> void addChildren(@NotNull T... children);

    /**
     * 一次性添加多个子请求的方法。
     *
     * @param <T> 泛型标记。
     * @param children 要添加的多个子请求的集合。
     */
    <T extends IToken<?>> void addChildren(@NotNull Collection<T> children);

    /**
     * 移除单个子请求的方法。
     *
     * @param <T> 泛型标记。
     * @param child 要移除的子请求。
     */
    <T extends IToken<?>> void removeChild(@NotNull T child);

    /**
     * 一次性移除多个子请求的方法。
     *
     * @param <T> 泛型标记。
     * @param children 要移除的多个子请求。
     */
    <T extends IToken<?>> void removeChildren(@NotNull T... children);

    /**
     * 一次性移除多个子请求的方法。
     *
     * @param <T> 泛型标记。
     * @param children 要移除的多个子请求的集合。
     */
    <T extends IToken<?>> void removeChildren(@NotNull Collection<T> children);

    /**
     * 检查此请求是否有子请求的方法。
     *
     * @return 如果有子请求则返回true。
     */
    boolean hasChildren();

    /**
     * 获取此请求的子请求的方法。不可变集合。
     *
     * @return 此请求的子请求的不可变集合。
     */
    @NotNull
    ImmutableCollection<IToken<?>> getChildren();

    /**
     * 子请求状态更新时由子请求调用的方法，指示子请求的状态已更新。
     *
     * @param manager 导致子请求更新的管理器。
     * @param child   已更新的子请求。
     */
    void childStateUpdated(@NotNull IRequestManager manager, @NotNull IToken<?> child);

    /**
     * 用于指示此请求的结果是否可以交付的方法。
     *
     * @return 如果可以交付请求则返回true，否则返回false。
     */
    boolean canBeDelivered();

    /**
     * 获取用于交付的ItemStack的方法。
     *
     * @return Deliveryman携带的ItemStacks。{@link NonNullList#isEmpty()}表示无法交付。
     */
    @NotNull
    ImmutableList<ItemStack> getDeliveries();

    /**
     * 将此请求的交付设置为给定的物品堆栈。
     *
     * @param stacks 将用于交付的物品堆栈。
     */
    void overrideCurrentDeliveries(@NotNull final ImmutableList<ItemStack> stacks);

    /**
     * 将单个物品堆栈添加为此请求的交付的方法。
     *
     * @param stack 应将其视为新交付的物品堆栈。
     */
    void addDelivery(@NotNull final ItemStack stack);

    /**
     * 将一组物品堆栈添加为此请求的交付的方法。
     *
     * @param list 应将其视为新交付的物品堆栈的列表。
     */
    void addDelivery(@NotNull final List<ItemStack> list);

    /**
     * 获取用于在GUI中显示请求的ItemStack的方法。如果返回空列表，则不显示任何堆栈。如果返回包含多个堆栈的列表，则每秒切换一次堆栈，除非玩家按住Shift键。
     *
     * @return The text that describes this Request.
     */
    @NotNull
    Component getShortDisplayString();

    /**
     * 用于获取一个可显示给玩家的 {@link Component}，描述了玩家如何完成该请求。应该代表请求，以阐明玩家是否需要完成它，或者需要有关此请求的信息。
     *
     * @return 描述此请求的文本。
     */
    @NotNull
    Component getLongDisplayString();

    /**
     * 用于获取一个 ItemStack 列表，表示此请求的堆叠。此列表在GUI中用于显示请求的内容。如果返回一个空列表，则不会显示任何堆叠。
     * 如果返回包含多个堆叠的列表，则每秒钟会在堆叠之间切换，除非玩家按住Shift键。
     *
     * @return 代表此请求的 ItemStack 列表。
     */
    @NotNull
    List<ItemStack> getDisplayStacks();

    /**
     * 获取在{@link #getDisplayStacks()}返回空列表时显示的ResourceLocation的方法。
     *
     * @return 在没有DisplayStacks时显示的图像的ResourceLocation。
     */
    @NotNull
    ResourceLocation getDisplayIcon();

    /**
     * 获取特定类型的请求的方法。
     * @param tClass 类的Class对象。
     * @param <T> 类型。
     * @return 特定类型的请求。
     */
    @NotNull
    <T> Optional<T> getRequestOfType(final Class<T> tClass);

    /**
     * 获取此类型的所有超类（缓存值）的方法。
     * @return 不可变副本的集合。
     */
    Set<TypeToken<?>> getSuperClasses();

    /**
     * 获取请求的解析器工具提示的方法。
     * @param colony 可以在必要时获取信息的殖民地视图。
     * @return 字符串列表或空列表。
     */
    List<MutableComponent> getResolverToolTip(IColonyView colony);

    /**
     * 重置请求的交付的方法。
     */
    void resetDeliveries();
}
