package com.minecolonies.core.entity.citizen.citizenhandlers;

import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.buildings.ModBuildings;
import com.minecolonies.api.entity.citizen.AbstractEntityCitizen;
import com.minecolonies.api.entity.citizen.citizenhandlers.ICitizenHealthHandler;
import com.minecolonies.api.util.BlockPosUtil;
import com.minecolonies.core.colony.jobs.AbstractJobGuard;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;

import static com.minecolonies.api.util.constant.CitizenConstants.TAG_HOSPITAL;
import static com.minecolonies.api.util.constant.StatisticsConstants.CITIZENS_HEALED;

/**
 * Handler taking care of citizens their health.
 */
public class CitizenHealthHandler implements ICitizenHealthHandler
{
    /**
     * Health percentage at which citizens seek a doctor.
     */
    private static final double MIN_SEEK_DOCTOR_HEALTH = 0.5;
    private static final double MAX_SEEK_DOCTOR_HEALTH = 0.1;

    /**
     * The citizen assigned to this manager.
     */
    private final AbstractEntityCitizen citizen;

    /**
     * Position of the hospital the citizen might be sleeping at (or null if not sleeping at a hospital).
     */
    private BlockPos hospitalPos = null;

    /**
     * Constructor for the health handler.
     *
     * @param citizen the citizen owning the handler.
     */
    public CitizenHealthHandler(final AbstractEntityCitizen citizen)
    {
        this.citizen = citizen;
    }

    @Override
    public boolean isHurt()
    {
        if (citizen.getCitizenJobHandler() instanceof AbstractJobGuard)
        {
            return false;
        }

        return citizen.getHealth() / citizen.getMaxHealth() < MIN_SEEK_DOCTOR_HEALTH;
    }

    @Override
    public boolean canBeHealed()
    {
        final IColony colony = citizen.getCitizenColonyHandler().getColony();
        if (colony == null)
        {
            return false;
        }

        return colony.hasBuilding(ModBuildings.HOSPITAL_ID, 1, true);
    }

    @Override
    public BlockPos getActiveHospital()
    {
        return hospitalPos;
    }

    @Override
    public void setActiveHospital(final BlockPos hospitalPos)
    {
        this.hospitalPos = hospitalPos;
    }

    @Override
    public void heal()
    {
        citizen.setHealth(citizen.getMaxHealth());
        citizen.getCitizenData().getColony().getStatisticsManager().increment(CITIZENS_HEALED, citizen.getCitizenData().getColony().getDay());

        hospitalPos = null;
        citizen.markDirty(0);
    }

    @Override
    public void write(final CompoundTag compound)
    {
        if (hospitalPos != null)
        {
            BlockPosUtil.write(compound, TAG_HOSPITAL, hospitalPos);
        }
    }

    @Override
    public void read(final CompoundTag compound)
    {
        if (compound.contains(TAG_HOSPITAL))
        {
            hospitalPos = BlockPosUtil.read(compound, TAG_HOSPITAL);
        }
    }
}
