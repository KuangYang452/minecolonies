package com.minecolonies.api.eventbus;

import com.google.gson.reflect.TypeToken;
import com.minecolonies.api.util.Log;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Default implementation of the mod event bus.
 */
public class DefaultEventBus implements EventBus
{
    /**
     * The map of event handlers.
     */
    private final Map<TypeToken<?>, List<EventHandler<?>>> eventHandlersPerType = new HashMap<>();

    @Override
    public <T extends IModEvent> void subscribe(final @NotNull IModEventType<T> eventType, final @NotNull EventHandler<T> handler)
    {
        Log.getLogger().debug("Registering event handler for id {}.", eventType.getIdentifier());

        eventHandlersPerType.computeIfAbsent(eventType.getIdentifier(), (f) -> new ArrayList<>()).add(handler);
    }

    @Override
    public <T extends IModEvent> void post(final @NotNull IModEventType<T> eventType, final @NotNull T event)
    {
        final List<EventHandler<?>> eventHandlers = eventHandlersPerType.get(eventType.getIdentifier());
        if (eventHandlers == null)
        {
            return;
        }

        Log.getLogger().debug("Sending event '{}' for type '{}'. Sending to {} handlers.", event.getEventId(), eventType.getIdentifier(), eventHandlers.size());

        for (final EventHandler<?> handler : eventHandlers)
        {
            try
            {
                ((EventHandler<T>) handler).apply(event);
            }
            catch (Exception ex)
            {
                Log.getLogger().warn("Sending event '{}' for type '{}'. Error occurred in handler:", event.getEventId(), eventType.getIdentifier(), ex);
            }
        }
    }
}
