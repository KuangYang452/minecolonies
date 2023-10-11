package com.minecolonies.api.colony.interactionhandling;

import com.ldtteam.blockui.views.BOWindow;
import com.minecolonies.api.colony.ICitizenData;
import com.minecolonies.api.colony.ICitizenDataView;
import net.minecraft.world.entity.player.Player;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.util.INBTSerializable;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 用于处理各种GUI交互的响应处理程序。
 */
public interface IInteractionResponseHandler extends INBTSerializable<CompoundTag>
{
    /**
     * GUI向玩家提出的问题。这是交互的关键，充当ID。
     *
     * @return 文本问题。
     */
    Component getInquiry();

    /**
     * 获取所有可能响应的列表。
     *
     * @return 玩家可以给出的可能响应的列表。
     */
    List<Component> getPossibleResponses();

    /**
     * 获取响应后GUI可能的进一步交互。
     *
     * @param response 玩家提供给GUI的响应。
     * @return 如果存在，则返回ICitizenInquiry的实例，否则返回null。
     */
    @Nullable
    Component getResponseResult(final Component response);

    /**
     * 检查此交互是否为主要交互还是次要交互。
     *
     * @return 如果是主要交互，则返回true。
     */
    boolean isPrimary();

    /**
     * 获取此交互响应处理程序的优先级。
     *
     * @return 聊天优先级。
     */
    IChatPriority getPriority();

    /**
     * 检查此响应处理程序是否对玩家仍然可见。
     *
     * @param world 此居民所在的世界。
     * @return 如果仍然可见，则返回true。
     */
    boolean isVisible(final Level world);

    /**
     * 检查此响应处理程序是否仍然有效。
     *
     * @param colony 此居民所在的殖民地。
     * @return 如果仍然有效，则返回true，否则返回false。
     */
    boolean isValid(final ICitizenData colony);

    /**
     * 在可能的响应上触发的服务器端操作。
     *
     * @param response 玩家点击的字符串响应。
     * @param player   触发它的世界。
     * @param data     与之相关的居民数据。
     */
    void onServerResponseTriggered(final Component response, final Player player, final ICitizenData data);

    /**
     * 在可能的响应上触发的客户端端操作。
     *
     * @param response 玩家点击的字符串响应。
     * @param player   客户端端的世界。
     * @param data     分配给它的居民数据。
     * @param window   触发它的窗口。
     * @return 是否希望继续交互。
     */
    @OnlyIn(Dist.CLIENT)
    boolean onClientResponseTriggered(final Component response, final Player player, final ICitizenDataView data, final BOWindow window);

    /**
     * 移除特定的父交互。
     *
     * @param inquiry 父交互的问题。
     */
    void removeParent(Component inquiry);

    /**
     * 生成与此相关的所有子交互。
     *
     * @return 所有子交互。
     */
    List<IInteractionResponseHandler> genChildInteractions();

    /**
     * 用于反序列化的类型ID。
     *
     * @return 字符串类型。
     */
    String getType();

    /**
     * 打开交互时的回调，用于设置特定于交互的内容。
     */
    default void onWindowOpened(final BOWindow window, final ICitizenDataView dataView) {}

    /**
     * 获取要为此交互渲染的图标。
     *
     * @return 图标的资源位置。
     */
    default ResourceLocation getInteractionIcon()
    {
        return null;
    }
}
