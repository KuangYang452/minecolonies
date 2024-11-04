package com.minecolonies.api.events;

import com.minecolonies.api.colony.ICitizenData;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.IColonyManager;
import com.minecolonies.api.colony.IColonyView;
import com.minecolonies.api.colony.buildings.IBuilding;
import com.minecolonies.core.colony.workorders.WorkOrderBuilding;
import net.minecraft.core.BlockPos;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jetbrains.annotations.NotNull;

/**
 * Event handler for all mod events.
 */
public interface IModEventHandler
{
    // Colony related events //

    /**
     * Event triggered just after the colony manager is loaded.
     *
     * @param manager the colony manager reference.
     */
    void managerLoaded(final @NotNull IColonyManager manager);

    /**
     * Event triggered just before the colony manager is unloaded.
     *
     * @param manager the colony manager reference.
     */
    void managerUnloaded(final @NotNull IColonyManager manager);

    /**
     * Event triggered just after a colony is being created.
     *
     * @param colony the colony in question.
     */
    void createColony(final @NotNull IColony colony);

    /**
     * Event triggered just before a colony is being deleted.
     *
     * @param colony the colony in question.
     */
    void deleteColony(final @NotNull IColony colony);

    /**
     * Event triggered just after a colony it's name is changed.
     *
     * @param colony the colony in question.
     */
    void colonyNameChanged(final @NotNull IColony colony);

    /**
     * Event triggered just after a colony it's team color is changed.
     *
     * @param colony the colony in question.
     */
    void colonyTeamColorChanged(final @NotNull IColony colony);

    /**
     * Event triggered just after a colony it's flag is changed.
     *
     * @param colony the colony in question.
     */
    void colonyFlagChanged(final @NotNull IColony colony);

    /**
     * Event triggered just after a building work order has been completed.
     *
     * @param building  the building which has been completed.
     * @param workOrder the work order belonging to this construction.
     */
    void buildingCompleted(final @NotNull IBuilding building, final @NotNull WorkOrderBuilding workOrder);

    /**
     * Event triggered just after a citizen is added to the colony.
     *
     * @param citizen the citizen which was added.
     * @param source  how the citizen was added to the colony.
     */
    void citizenAdded(final @NotNull ICitizenData citizen, final @NotNull CitizenAddedSource source);

    /**
     * Event triggered just after a citizen has died.
     *
     * @param citizen      the citizen which died.
     * @param damageSource how the citizen died.
     */
    void citizenDied(final @NotNull ICitizenData citizen, final @NotNull DamageSource damageSource);

    /**
     * Event triggered just before a citizen is removed from the colony.
     *
     * @param citizen the citizen which is being removed.
     * @param reason  why the citizen is being removed.
     */
    void citizenRemoved(final @NotNull ICitizenData citizen, final @NotNull Entity.RemovalReason reason);

    /**
     * Event triggered just before the colony manager is unloaded.
     *
     * @param colonyView the colony manager reference.
     */
    void colonyViewUpdated(final @NotNull IColonyView colonyView);

    // Internal events //

    /**
     * Event triggered just after a chunk is assigned its owning colony capability data.
     *
     * @param chunk the input chunk.
     */
    void chunkUpdated(final @NotNull LevelChunk chunk);

    /**
     * Event trigger for placing an entity in the world.
     *
     * @param level    the target level.
     * @param position the target position.
     * @param entity   the entity to place.
     */
    void placeEntity(final @NotNull Level level, final @NotNull BlockPos position, final @NotNull Entity entity);

    /**
     * Event triggered after all custom recipes have been reloaded.
     */
    void customRecipesLoaded();

    /**
     * How the citizen came into the colony.
     */
    enum CitizenAddedSource
    {
        /**
         * The citizen spawned as part of the {@link com.minecolonies.api.configuration.ServerConfiguration#initialCitizenAmount}.
         */
        INITIAL,
        /**
         * The citizen was born naturally.
         */
        BORN,
        /**
         * The citizen was hired from the tavern.
         */
        HIRED,
        /**
         * The citizen got resurrected from his grave.
         */
        RESURRECTED,
        /**
         * The citizen was spawned in using commands.
         */
        COMMANDS
    }
}
