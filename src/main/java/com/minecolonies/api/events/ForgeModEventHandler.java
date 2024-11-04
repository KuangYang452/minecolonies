package com.minecolonies.api.events;

import com.minecolonies.api.colony.ICitizenData;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.IColonyManager;
import com.minecolonies.api.colony.IColonyView;
import com.minecolonies.api.colony.buildings.IBuilding;
import com.minecolonies.api.colony.managers.events.ColonyManagerLoadedEvent;
import com.minecolonies.api.colony.managers.events.ColonyManagerUnloadedEvent;
import com.minecolonies.api.events.colony.*;
import com.minecolonies.api.events.colony.buildings.BuildingConstructionEvent;
import com.minecolonies.api.events.colony.citizens.CitizenAddedEvent;
import com.minecolonies.api.events.colony.citizens.CitizenDiedEvent;
import com.minecolonies.api.events.colony.citizens.CitizenRemovedEvent;
import com.minecolonies.api.util.Log;
import com.minecolonies.core.colony.workorders.WorkOrderBuilding;
import net.minecraft.core.BlockPos;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.BlockSnapshot;
import net.minecraftforge.event.level.BlockEvent.EntityPlaceEvent;
import net.minecraftforge.eventbus.api.Event;
import org.jetbrains.annotations.NotNull;

/**
 * Forge implementation for the mod event handler.
 */
public class ForgeModEventHandler implements IModEventHandler
{
    @Override
    public void managerLoaded(final @NotNull IColonyManager manager)
    {
        sendEvent(new ColonyManagerLoadedEvent(manager));
    }

    @Override
    public void managerUnloaded(final @NotNull IColonyManager manager)
    {
        sendEvent(new ColonyManagerUnloadedEvent(manager));
    }

    @Override
    public void createColony(final @NotNull IColony colony)
    {
        sendEvent(new ColonyCreatedEvent(colony));
    }

    @Override
    public void deleteColony(final @NotNull IColony colony)
    {
        sendEvent(new ColonyDeletedEvent(colony));
    }

    @Override
    public void colonyNameChanged(final @NotNull IColony colony)
    {
        sendEvent(new ColonyNameChangedEvent(colony));
    }

    @Override
    public void colonyTeamColorChanged(final @NotNull IColony colony)
    {
        sendEvent(new ColonyTeamColorChangedEvent(colony));
    }

    @Override
    public void colonyFlagChanged(final @NotNull IColony colony)
    {
        sendEvent(new ColonyFlagChangedEvent(colony));
    }

    @Override
    public void buildingCompleted(final @NotNull IBuilding building, final @NotNull WorkOrderBuilding workOrder)
    {
        sendEvent(new BuildingConstructionEvent(building, workOrder));
    }

    @Override
    public void citizenAdded(final @NotNull ICitizenData citizen, final @NotNull CitizenAddedSource source)
    {
        sendEvent((new CitizenAddedEvent(citizen, source)));
    }

    @Override
    public void citizenDied(final @NotNull ICitizenData citizen, final @NotNull DamageSource damageSource)
    {
        sendEvent(new CitizenDiedEvent(citizen, damageSource));
    }

    @Override
    public void citizenRemoved(final @NotNull ICitizenData citizen, final @NotNull Entity.RemovalReason reason)
    {
        sendEvent(new CitizenRemovedEvent(citizen, reason));
    }

    @Override
    public void colonyViewUpdated(final @NotNull IColonyView colonyView)
    {
        sendEvent(new ColonyViewUpdatedEvent(colonyView));
    }

    @Override
    public void chunkUpdated(final @NotNull LevelChunk chunk)
    {
        sendEvent(new ClientChunkUpdatedEvent(chunk));
    }

    @Override
    public void placeEntity(final @NotNull Level level, final @NotNull BlockPos position, final @NotNull Entity entity)
    {
        sendEvent(new EntityPlaceEvent(BlockSnapshot.create(level.dimension(), level, position), level.getBlockState(position.below()), entity));
    }

    @Override
    public void customRecipesLoaded()
    {
        sendEvent(new CustomRecipesReloadedEvent());
    }

    /**
     * Internal event send method.
     *
     * @param event the input event.
     */
    private void sendEvent(final Event event)
    {
        try
        {
            MinecraftForge.EVENT_BUS.post(event);
        }
        catch (final Exception e)
        {
            Log.getLogger().atError().withThrowable(e).log("Exception occurred during {} event", event.getClass().getName());
        }
    }
}
