package com.minecolonies.api.colony.buildings.modules;

import com.minecolonies.api.colony.buildings.IBuilding;

/**
 * 所有模块的抽象类。具有从建筑物中必须调用的所有必要方法的基本方法。
 */
public abstract class AbstractBuildingModule implements IBuildingModule
{
    /**
     * 模块是否脏的标志。
     */
    public boolean isDirty = false;

    /**
     * 此模块所属的建筑物。
     */
    protected IBuilding building;

    @Override
    public void markDirty()
    {
        this.isDirty = true;
        if (building != null)
        {
            building.markDirty();
        }
    }

    @Override
    public void clearDirty()
    {
        this.isDirty = false;
    }

    @Override
    public boolean checkDirty()
    {
        return this.isDirty;
    }

    @Override
    public IBuilding getBuilding()
    {
        return this.building;
    }

    @Override
    public IBuildingModule setBuilding(final IBuilding building)
    {
        this.building = building;
        return this;
    }
}
