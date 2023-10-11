package com.minecolonies.api.entity.citizen.citizenhandlers;

import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.buildings.IBuilding;
import com.minecolonies.api.colony.buildings.IBuilding;
import org.jetbrains.annotations.Nullable;

public interface ICitizenColonyHandler
{
    /**
     * 计算该工人的建筑物。
     *
     * @return 建筑物或null（如果没有）。
     */
    @Nullable
    IBuilding getWorkBuilding();

    @Nullable
    IBuilding getHomeBuilding();

    /**
     * EntityCitizen的服务器特定更新。
     *
     * @param colonyID  殖民地ID。
     * @param citizenID 市民ID。
     */
    void registerWithColony(final int colonyID, final int citizenID);

    /**
     * 更新市民实体的客户端部分。
     */
    void updateColonyClient();

    /**
     * 获取工人每次执行的动作或行进x个方块时应减少其饱和度的数量。
     *
     * @return 描述它的double。
     */
    double getPerBuildingFoodCost();

    /**
     * 获取市民所属的殖民地。
     *
     * @return 市民所属的殖民地或null。
     */
    @Nullable
    IColony getColony();

    /**
     * 获取殖民地ID。
     *
     * @return 殖民地ID。
     */
    int getColonyId();

    /**
     * 设置殖民地ID。
     *
     * @param colonyId 新的殖民地ID。
     */
    void setColonyId(int colonyId);

    /**
     * 在实体被移除时执行的操作。
     */
    void onCitizenRemoved();
}
