package com.minecolonies.api.colony.buildings.workerbuildings;

import com.minecolonies.api.colony.ICitizenData;
import com.minecolonies.api.colony.buildings.IBuilding;
import com.minecolonies.api.tileentities.AbstractTileEntityColonyBuilding;
import com.minecolonies.api.tileentities.AbstractTileEntityWareHouse;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.Level;

import java.util.Set;

public interface IWareHouse extends IBuilding
{
    /**
     * 检查送货员是否被允许访问仓库。
     *
     * @param buildingWorker 送货员的数据。
     * @return 如果可以访问，则为true。
     */
    boolean canAccessWareHouse(ICitizenData buildingWorker);

    /**
     * 升级所有容器的容量，每个容器增加9个槽位。
     *
     * @param world 世界对象。
     */
    void upgradeContainers(Level world);

    /**
     * 返回属于殖民地建筑的瓦片实体。
     *
     * @return 建筑物的{@link AbstractTileEntityColonyBuilding}对象。
     */
    @Override
    AbstractTileEntityWareHouse getTileEntity();

    /**
     * 检查容器位置是否属于仓库。
     *
     * @param inDimensionLocation 位置。
     * @return 如果是，则为true。
     */
    boolean hasContainerPosition(BlockPos inDimensionLocation);
}
