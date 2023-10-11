package com.minecolonies.coremod.colony.interactionhandling;

import com.minecolonies.api.colony.ICitizen;
import com.minecolonies.api.colony.interactionhandling.IChatPriority;
import com.minecolonies.api.colony.interactionhandling.IInteractionResponseHandler;
import com.minecolonies.api.colony.interactionhandling.InteractionValidatorRegistry;
import com.minecolonies.api.colony.interactionhandling.ModInteractionResponseHandlers;
import com.minecolonies.api.util.Tuple;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Component;

import java.util.Collections;
import java.util.List;

/**
 * 服务器端交互响应处理程序。
 */
public class StandardInteraction extends ServerCitizenInteraction
{
    /**
     * 标准响应
     */
    public static final String INTERACTION_R_OKAY   = "com.minecolonies.coremod.gui.chat.okay";
    public static final String INTERACTION_R_IGNORE = "com.minecolonies.coremod.gui.chat.ignore";
    public static final String INTERACTION_R_REMIND = "com.minecolonies.coremod.gui.chat.remindmelater";
    public static final String INTERACTION_R_SKIP   = "com.minecolonies.coremod.gui.chat.skipchitchat";

    @SuppressWarnings("unchecked")
    private static final Tuple<Component, Component>[] tuples = (Tuple<Component, Component>[]) new Tuple[] {
      new Tuple<>(Component.translatable(INTERACTION_R_OKAY), null),
      new Tuple<>(Component.translatable(INTERACTION_R_IGNORE), null),
      new Tuple<>(Component.translatable(INTERACTION_R_REMIND), null),
      new Tuple<>(Component.translatable(INTERACTION_R_SKIP), null)};

    /**
     * 具有自定义验证器的服务器交互响应处理程序。
     *
     * @param inquiry   客户端查询。
     * @param validator 验证器的ID。
     * @param priority  交互优先级。
     */
    public StandardInteraction(
      final Component inquiry,
      final Component validator,
      final IChatPriority priority)
    {
        super(inquiry, true, priority, InteractionValidatorRegistry.getStandardInteractionValidatorPredicate(validator), validator, tuples);
    }

    /**
     * 服务器交互响应处理程序。
     *
     * @param inquiry  客户端查询。
     * @param priority 交互优先级。
     */
    public StandardInteraction(
      final Component inquiry,
      final IChatPriority priority)
    {
        super(inquiry, true, priority, InteractionValidatorRegistry.getStandardInteractionValidatorPredicate(inquiry), inquiry, tuples);
    }

    /**
     * 加载市民响应处理程序的方式。
     *
     * @param data 拥有此处理程序的市民。
     */
    public StandardInteraction(final ICitizen data)
    {
        super(data);
    }

    @Override
    public List<IInteractionResponseHandler> genChildInteractions()
    {
        return Collections.emptyList();
    }

    @Override
    public String getType()
    {
        return ModInteractionResponseHandlers.STANDARD.getPath();
    }
}
