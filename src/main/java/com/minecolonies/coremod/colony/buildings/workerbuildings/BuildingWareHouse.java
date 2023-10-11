package com.minecolonies.coremod.colony.buildings.workerbuildings;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.ldtteam.blockui.views.BOWindow;
import com.minecolonies.api.colony.ICitizenData;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.IColonyView;
import com.minecolonies.api.colony.buildings.workerbuildings.IWareHouse;
import com.minecolonies.api.colony.requestsystem.resolver.IRequestResolver;
import com.minecolonies.api.tileentities.*;
import com.minecolonies.api.util.constant.TypeConstants;
import com.minecolonies.coremod.blocks.BlockMinecoloniesRack;
import com.minecolonies.coremod.client.gui.WindowHutMinPlaceholder;
import com.minecolonies.coremod.colony.buildings.AbstractBuilding;
import com.minecolonies.coremod.colony.buildings.modules.CourierAssignmentModule;
import com.minecolonies.coremod.colony.buildings.modules.WarehouseModule;
import com.minecolonies.coremod.colony.buildings.views.AbstractBuildingView;
import com.minecolonies.coremod.colony.requestsystem.resolvers.DeliveryRequestResolver;
import com.minecolonies.coremod.colony.requestsystem.resolvers.PickupRequestResolver;
import com.minecolonies.coremod.colony.requestsystem.resolvers.WarehouseConcreteRequestResolver;
import com.minecolonies.coremod.colony.requestsystem.resolvers.WarehouseRequestResolver;
import com.minecolonies.coremod.tileentities.TileEntityWareHouse;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.NotNull;

import static com.ldtteam.structurize.placement.handlers.placement.PlacementHandlers.handleTileEntityPlacement;

/**
 * 仓库建筑的类。
 */
public class BuildingWareHouse extends AbstractBuilding implements IWareHouse
{
    /**
     * 描述仓库的字符串。
     */
    private static final String WAREHOUSE = "warehouse";

    /**
     * 建筑的最大级别。
     */
    private static final int MAX_LEVEL = 5;

    /**
     * 最大存储升级。
     */
    public static final int MAX_STORAGE_UPGRADE = 3;

    /**
     * 实例化一个新的仓库建筑。
     *
     * @param c 城邦。
     * @param l 位置
     */
    public BuildingWareHouse(final IColony c, final BlockPos l)
    {
        super(c, l);
    }

    @Override
    public void requestRepair(final BlockPos builder)
    {
        // 确保修复时所有货架都设置为在仓库中。
        for (final BlockPos pos : containerList)
        {
            if (getColony().getWorld() != null)
            {
                final BlockEntity entity = getColony().getWorld().getBlockEntity(pos);
                if (entity instanceof TileEntityRack)
                {
                    ((AbstractTileEntityRack) entity).setInWarehouse(true);
                }
            }
        }

        super.requestRepair(builder);
    }

    @Override
    public boolean canAccessWareHouse(final ICitizenData citizenData)
    {
        return getFirstModuleOccurance(CourierAssignmentModule.class).hasAssignedCitizen(citizenData);
    }

    @NotNull
    @Override
    public String getSchematicName()
    {
        return WAREHOUSE;
    }

    /**
     * 返回属于城邦建筑的方块实体。
     *
     * @return 建筑的{@link TileEntityColonyBuilding}对象。
     */
    @Override
    public AbstractTileEntityWareHouse getTileEntity()
    {
        final AbstractTileEntityColonyBuilding entity = super.getTileEntity();
        return !(entity instanceof TileEntityWareHouse) ? null : (AbstractTileEntityWareHouse) entity;
    }

    @Override
    public boolean hasContainerPosition(final BlockPos inDimensionLocation)
    {
        return containerList.contains(inDimensionLocation) || getLocation().getInDimensionLocation().equals(inDimensionLocation);
    }

    @Override
    public int getMaxBuildingLevel()
    {
        return MAX_LEVEL;
    }

    @Override
    public void registerBlockPosition(@NotNull final Block block, @NotNull final BlockPos pos, @NotNull final Level world)
    {
        if (block instanceof BlockMinecoloniesRack)
        {
            final BlockEntity entity = world.getBlockEntity(pos);
            if (entity instanceof TileEntityRack)
            {
                ((AbstractTileEntityRack) entity).setInWarehouse(true);
                while (((TileEntityRack) entity).getUpgradeSize() < getFirstModuleOccurance(WarehouseModule.class).getStorageUpgrade())
                {
                    ((TileEntityRack) entity).upgradeRackSize();
                }
            }
        }
        super.registerBlockPosition(block, pos, world);
    }

    @Override
    public ImmutableCollection<IRequestResolver<?>> createResolvers()
    {
        final ImmutableCollection<IRequestResolver<?>> supers = super.createResolvers();
        final ImmutableList.Builder<IRequestResolver<?>> builder = ImmutableList.builder();

        builder.addAll(supers);
        builder.add(new WarehouseRequestResolver(getRequester().getLocation(),
          getColony().getRequestManager().getFactoryController().getNewInstance(TypeConstants.ITOKEN)),
          new WarehouseConcreteRequestResolver(getRequester().getLocation(),
          getColony().getRequestManager().getFactoryController().getNewInstance(TypeConstants.ITOKEN))
          );

        builder.add(new DeliveryRequestResolver(getRequester().getLocation(),
          getColony().getRequestManager().getFactoryController().getNewInstance(TypeConstants.ITOKEN)));
        builder.add(new PickupRequestResolver(getRequester().getLocation(),
          getColony().getRequestManager().getFactoryController().getNewInstance(TypeConstants.ITOKEN)));

        return builder.build();
    }

    /**
     * 升级所有容器，每个容器增加9个槽位。
     *
     * @param world 世界对象。
     */
    @Override
    public void upgradeContainers(final Level world)
    {
        if (getFirstModuleOccurance(WarehouseModule.class).getStorageUpgrade() < MAX_STORAGE_UPGRADE)
        {
            for (final BlockPos pos : getContainers())
            {
                final BlockEntity entity = world.getBlockEntity(pos);
                if (entity instanceof TileEntityRack && !(entity instanceof TileEntityColonyBuilding))
                {
                    ((AbstractTileEntityRack) entity).upgradeRackSize();
                }
            }
            getFirstModuleOccurance(WarehouseModule.class).incrementStorageUpgrade();
        }
        markDirty();
    }

    @Override
    public boolean canBeGathered()
    {
        return false;
    }

    /**
     * 仓库视图。
     */
    public static class View extends AbstractBuildingView
    {
        /**
         * 实例化仓库视图。
         *
         * @param c     要放置的城邦视图
         * @param l     位置
         */
        public View(final IColonyView c, final BlockPos l)
        {
            super(c, l);
        }

        @NotNull
        @Override
        public BOWindow getWindow()
        {
            return new WindowHutMinPlaceholder<>(this);
        }
    }
}
