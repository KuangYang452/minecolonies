package com.minecolonies.coremod.blocks.huts;

import com.minecolonies.api.blocks.AbstractBlockHut;
import com.minecolonies.api.colony.buildings.ModBuildings;
import com.minecolonies.api.colony.buildings.registry.BuildingEntry;
import org.jetbrains.annotations.NotNull;

/**
 * Hut for the Planner. No different from {@link AbstractBlockHut}
 */
public class BlockHutPlanner extends AbstractBlockHut<BlockHutPlanner>
{
    public BlockHutPlanner()
    {
        // No different from Abstract parent
        super();
    }

    @NotNull
    @Override
    public String getHutName() { return "blockhutplanner"; }

    @Override
    public BuildingEntry getBuildingEntry() { return ModBuildings.planner.get(); }
}