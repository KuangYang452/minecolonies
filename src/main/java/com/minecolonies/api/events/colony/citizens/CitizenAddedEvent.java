package com.minecolonies.api.events.colony.citizens;

import com.minecolonies.api.colony.ICitizenData;
import com.minecolonies.api.events.IModEventHandler.CitizenAddedSource;

/**
 * Event for when a citizen was added to the colony.
 */
public class CitizenAddedEvent extends AbstractCitizenEvent
{
    /**
     * The way the citizen came into the colony.
     */
    private final CitizenAddedSource source;

    /**
     * Citizen added event.
     *
     * @param citizen the citizen related to the event.
     * @param source  the way the citizen came into the colony.
     */
    public CitizenAddedEvent(final ICitizenData citizen, final CitizenAddedSource source)
    {
        super(citizen);
        this.source = source;
    }

    /**
     * Get the way the citizen came into the colony.
     *
     * @return the enum value.
     */
    public CitizenAddedSource getSource()
    {
        return source;
    }
}
