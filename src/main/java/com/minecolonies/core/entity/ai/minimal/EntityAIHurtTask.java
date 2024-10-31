package com.minecolonies.core.entity.ai.minimal;

import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.buildings.IBuilding;
import com.minecolonies.api.entity.ai.IStateAI;
import com.minecolonies.api.entity.ai.statemachine.states.CitizenAIState;
import com.minecolonies.api.entity.ai.statemachine.states.IState;
import com.minecolonies.api.entity.ai.statemachine.tickratestatemachine.TickingTransition;
import com.minecolonies.core.colony.buildings.workerbuildings.BuildingHospital;
import com.minecolonies.core.entity.citizen.EntityCitizen;
import net.minecraft.core.BlockPos;

import static com.minecolonies.core.entity.ai.minimal.EntityAIEatTask.EatingState.GO_TO_RESTAURANT;
import static com.minecolonies.core.entity.ai.minimal.EntityAIEatTask.EatingState.WAIT_FOR_FOOD;
import static com.minecolonies.core.entity.ai.minimal.EntityAIHurtTask.HurtState.GO_TO_HOSPITAL;
import static com.minecolonies.core.entity.ai.minimal.EntityAIHurtTask.HurtState.WAIT_FOR_HEALER;
import static com.minecolonies.core.entity.ai.minimal.EntityAISickTask.DiseaseState.SEARCH_HOSPITAL;

/**
 * The AI task for citizens to execute when they are supposed to eat.
 */
public class EntityAIHurtTask implements IStateAI
{
    /**
     * Min distance to hut before pathing to hospital.
     */
    private static final int MIN_DIST_TO_HUT = 5;

    /**
     * Min distance to hospital before trying to find a bed.
     */
    private static final int MIN_DIST_TO_HOSPITAL = 3;

    /**
     * Min distance to the hospital in general.
     */
    private static final long MINIMUM_DISTANCE_TO_HOSPITAL = 10;

    /**
     * Required time to cure.
     */
    private static final int REQUIRED_TIME_TO_CURE = 60;

    /**
     * Chance for a random cure to happen.
     */
    private static final int CHANCE_FOR_RANDOM_CURE = 10;

    /**
     * Attempts to position right in the bed.
     */
    private static final int GOING_TO_BED_ATTEMPTS = 20;

    /**
     * The citizen assigned to this task.
     */
    private final EntityCitizen citizen;

    /**
     * The bed the citizen is sleeping in.
     */
    private BlockPos usedBed;

    /**
     * Instantiates this task.
     *
     * @param citizen the citizen.
     */
    public EntityAIHurtTask(final EntityCitizen citizen)
    {
        super();
        this.citizen = citizen;

        citizen.getCitizenAI().addTransition(new TickingTransition<>(CitizenAIState.HURT, this::isHurt, () -> SEARCH_HOSPITAL, 20));
        citizen.getCitizenAI().addTransition(new TickingTransition<>(SEARCH_HOSPITAL, () -> true, this::searchHospital, 20));
        citizen.getCitizenAI().addTransition(new TickingTransition<>(GO_TO_HOSPITAL, () -> true, this::goToHospital, 20));
        citizen.getCitizenAI().addTransition(new TickingTransition<>(WAIT_FOR_HEALER, () -> true, this::waitForHealer, 20));
    }

    private boolean isHurt()
    {
        if (citizen.getCitizenHealthHandler().isHurt())
        {
            reset();
            return true;
        }

        return false;
    }

    /**
     * AI step to look for a nearby hospital.
     *
     * @return the next AI state.
     */
    private IState searchHospital()
    {
        final IColony colony = citizen.getCitizenData().getColony();
        if (colony == null)
        {
            return CitizenAIState.IDLE;
        }

        final BlockPos nearestHospital = colony.getBuildingManager().getBestBuilding(citizen, BuildingHospital.class);
        if (nearestHospital == null)
        {
            return CitizenAIState.IDLE;
        }

        citizen.getCitizenHealthHandler().setActiveHospital(nearestHospital);

        return GO_TO_HOSPITAL;
    }

    /**
     * AI step move towards the hospital and lay in bed when arriving.
     *
     * @return the next AI state.
     */
    private IState goToHospital()
    {
        final BlockPos activeHospital = citizen.getCitizenHealthHandler().getActiveHospital();
        if (activeHospital == null)
        {
            return CitizenAIState.IDLE;
        }

        final IBuilding building = citizen.getCitizenData().getColony().getBuildingManager().getBuilding(activeHospital);
        if (building != null)
        {
            if (building.isInBuilding(citizen.blockPosition()))
            {
                return WAIT_FOR_FOOD;
            }
            else if (!citizen.isWorkerAtSiteWithMove(activeHospital, MIN_DIST_TO_HOSPITAL))
            {
                return GO_TO_RESTAURANT;
            }
        }

        return CitizenAIState.IDLE;
    }

    /**
     * Resets the state of the AI.
     */
    private void reset()
    {
        citizen.getCitizenHealthHandler().setActiveHospital(null);
    }

    /**
     * The different types of AIStates related to eating.
     */
    public enum HurtState implements IState
    {
        SEARCH_HOSPITAL,
        GO_TO_HOSPITAL,
        WAIT_FOR_HEALER
    }
}
