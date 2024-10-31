package com.minecolonies.api.entity.citizen.citizenhandlers;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;

/**
 * Citizen health handler interface.
 */
public interface ICitizenHealthHandler
{
    /**
     * Check if the citizen is hurt and must be healed.
     *
     * @return true if so.
     */
    boolean isHurt();

    /**
     * Check if the colony has a hospital where the citizen may be healed in.
     *
     * @return true if the colony has a hospital.
     */
    boolean canBeHealed();

    /**
     * Check if the citizen is currently being healed at a hospital.
     *
     * @return the hospital building location.
     */
    BlockPos getActiveHospital();

    void setActiveHospital(final BlockPos hospitalPos);

    void heal();

    /**
     * Write the handler to NBT.
     *
     * @param compound the nbt to write it to.
     */
    void write(final CompoundTag compound);

    /**
     * Read the handler from NBT.
     *
     * @param compound the nbt to read it from.
     */
    void read(final CompoundTag compound);
}
