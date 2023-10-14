package com.minecolonies.api.tileentities;

import com.ldtteam.structurize.blueprints.v1.Blueprint;
import com.ldtteam.structurize.management.StructureName;
import com.ldtteam.structurize.management.Structures;
import com.ldtteam.structurize.util.BlockInfo;
import com.ldtteam.structurize.util.PlacementSettings;
import com.minecolonies.api.blocks.AbstractBlockHut;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.IColonyManager;
import com.minecolonies.api.colony.IColonyView;
import com.minecolonies.api.colony.buildings.IBuilding;
import com.minecolonies.api.colony.buildings.IBuildingContainer;
import com.minecolonies.api.colony.buildings.views.IBuildingView;
import com.minecolonies.api.colony.permissions.Action;
import com.minecolonies.api.inventory.api.CombinedItemHandler;
import com.minecolonies.api.inventory.container.ContainerBuildingInventory;
import com.minecolonies.api.util.*;
import com.minecolonies.api.util.constant.Constants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import static com.minecolonies.api.util.constant.BuildingConstants.DEACTIVATED;
import static com.minecolonies.api.util.constant.NbtTagConstants.TAG_BUILDING_TYPE;

/**
 * 处理村庄建筑瓦砾的方块实体的类。
 */
@SuppressWarnings("PMD.ExcessiveImports")
public class TileEntityColonyBuilding extends AbstractTileEntityColonyBuilding implements ITickable
{
    /**
     * 用于存储村庄ID的NBTTag。
     */
    private static final String TAG_COLONY = "colony";
    private static final String TAG_MIRROR = "mirror";
    private static final String TAG_STYLE  = "style";

    /**
     * 村庄ID。
     */
    private int colonyId = 0;

    /**
     * 村庄。
     */
    private IColony colony;

    /**
     * 瓦砾所属的建筑。
     */
    private IBuilding building;

    /**
     * 检查建筑是否有镜像。
     */
    private boolean mirror;

    /**
     * 建筑的风格。
     */
    private String style = "";

    /**
     * 建筑位置的名称。
     */
    public ResourceLocation registryName;

    /**
     * 创建建筑的合并存储包装器。
     */
    private LazyOptional<CombinedItemHandler> combinedInv;

    /**
     * 用于反射创建新的方块实体的默认构造函数。不要使用。
     */
    public TileEntityColonyBuilding(final BlockPos pos, final BlockState state)
    {
        this(MinecoloniesTileEntities.BUILDING.get(), pos, state);
    }

    /**
     * 替代的重载构造函数。
     *
     * @param type 实体类型。
     */
    public TileEntityColonyBuilding(final BlockEntityType<? extends AbstractTileEntityColonyBuilding> type, final BlockPos pos, final BlockState state)
    {
        super(type, pos, state);
    }

    /**
     * 返回村庄ID。
     *
     * @return 村庄的ID。
     */
    @Override
    public int getColonyId()
    {
        return colonyId;
    }

    /**
     * 返回方块实体的村庄。
     *
     * @return 方块实体的村庄。
     */
    @Override
    public IColony getColony()
    {
        if (colony == null)
        {
            updateColonyReferences();
        }
        return colony;
    }

    /**
     * 从方块实体同步村庄的引用。
     */
    private void updateColonyReferences()
    {
        if (colony == null && getLevel() != null)
        {
            if (colonyId == 0)
            {
                colony = IColonyManager.getInstance().getColonyByPosFromWorld(getLevel(), this.getBlockPos());
            }
            else
            {
                colony = IColonyManager.getInstance().getColonyByWorld(colonyId, getLevel());
            }

            // 这很可能是一个预览建筑，请不要在这里打印日志。
            if (colony == null && !getLevel().isClientSide)
            {
                // 在服务器端打印日志
                //Log.getLogger().info(String.format("TileEntityColonyBuilding at %s:[%d,%d,%d] had colony.",getWorld().getWorldInfo().getWorldName(), pos.getX(), pos.getY(), pos.getZ()));
            }
        }

        if (building == null && colony != null)
        {
            building = colony.getBuildingManager().getBuilding(getPosition());
            if (building != null && (getLevel() == null || !getLevel().isClientSide))
            {
                building.setTileEntity(this);
            }
        }
    }

    /**
     * 返回方块实体的位置。
     *
     * @return 方块实体的坐标位置。
     */
    @Override
    public BlockPos getPosition()
    {
        return worldPosition;
    }

    /**
     * 检查某个物品，并返回包含该物品的箱子位置。
     *
     * @param itemStackSelectionPredicate 用于搜索的物品栈。
     * @return 位置或null。
     */
    @Override
    @Nullable
    public BlockPos getPositionOfChestWithItemStack(@NotNull final Predicate<ItemStack> itemStackSelectionPredicate)
    {
        final Predicate<ItemStack> notEmptyPredicate = itemStackSelectionPredicate.and(ItemStackUtils.NOT_EMPTY_PREDICATE);
        @Nullable final IBuildingContainer theBuilding = getBuilding();

        if (theBuilding != null)
        {
            for (final BlockPos pos : theBuilding.getContainers())
            {
                if (WorldUtil.isBlockLoaded(level, pos))
                {
                    final BlockEntity entity = getLevel().getBlockEntity(pos);
                    if (entity instanceof AbstractTileEntityRack)
                    {
                        if (((AbstractTileEntityRack) entity).hasItemStack(notEmptyPredicate))
                        {
                            return pos;
                        }
                    }
                    else if (isInTileEntity(entity, notEmptyPredicate))
                    {
                        return pos;
                    }
                }
            }
        }
        return null;
    }

    /**
     * 设置方块实体的村庄。
     *
     * @param c 要设置为引用的村庄。
     */
    @Override
    public void setColony(final IColony c)
    {
        colony = c;
        colonyId = c.getID();
        setChanged();
    }

    @Override
    public void setChanged()
    {
        super.setChanged();
        if (building != null)
        {
            building.markDirty();
        }
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket()
    {
        return  ClientboundBlockEntityDataPacket.create(this);
    }

    @NotNull
    @Override
    public CompoundTag getUpdateTag()
    {
        return saveWithId();
    }

    @Override
    public void handleUpdateTag(final CompoundTag tag)
    {
        this.load(tag);
    }

    @Override
    public void onDataPacket(final Connection net, final ClientboundBlockEntityDataPacket packet)
    {
        final CompoundTag compound = packet.getTag();
        colonyId = compound.getInt(TAG_COLONY);
        super.onDataPacket(net, packet);
    }

    @Override
    public void onLoad()
    {
        if (building != null)
        {
            building.setTileEntity(null);
        }
    }

    /**
     * 返回与方块实体关联的建筑。
     *
     * @return 与方块实体关联的{@link IBuildingContainer}。
     */
    @Override
    public IBuilding getBuilding()
    {
        if (building == null)
        {
            updateColonyReferences();
        }
        return building;
    }

    /**
     * 设置与方块实体关联的建筑。
     *
     * @param b 要与方块实体关联的{@link IBuilding}。
     */
    @Override
    public void setBuilding(final IBuilding b)
    {
        building = b;
    }

    @NotNull
    @Override
    public Component getDisplayName()
    {
        return getBlockState().getBlock().getName();
    }

    /**
     * 返回与方块实体关联的建筑的视图。
     *
     * @return 与方块实体关联的{@link IBuildingView}。
     */
    @Override
    public IBuildingView getBuildingView()
    {
        final IColonyView c = IColonyManager.getInstance().getColonyView(colonyId, level.dimension());
        return c == null ? null : c.getBuilding(getPosition());
    }

    @Override
    public void load(@NotNull final CompoundTag compound)
    {
        super.load(compound);
        if (compound.getAllKeys().contains(TAG_COLONY))
        {
            colonyId = compound.getInt(TAG_COLONY);
        }
        mirror = compound.getBoolean(TAG_MIRROR);
        style = compound.getString(TAG_STYLE);
        registryName = new ResourceLocation(compound.getString(TAG_BUILDING_TYPE));
        buildingPos = worldPosition;
        single = true;
    }

    @Override
    public void saveAdditional(@NotNull final CompoundTag compound)
    {
        super.saveAdditional(compound);
        compound.putInt(TAG_COLONY, colonyId);
        compound.putBoolean(TAG_MIRROR, mirror);
        compound.putString(TAG_STYLE, style);
        compound.putString(TAG_BUILDING_TYPE, registryName.toString());
    }

    @Override
    public void tick()
    {
        if (combinedInv != null)
        {
            combinedInv.invalidate();
            combinedInv = null;
        }
        if (!getLevel().isClientSide && colonyId == 0)
        {
            final IColony tempColony = IColonyManager.getInstance().getColonyByPosFromWorld(getLevel(), this.getPosition());
            if (tempColony != null)
            {
                colonyId = tempColony.getID();
            }
        }

        if (!getLevel().isClientSide && colonyId != 0 && colony == null)
        {
            updateColonyReferences();
        }
    }

    public boolean isUsableByPlayer(@NotNull final Player player)
    {
        return this.hasAccessPermission(player);
    }

    /**
     * 检查玩家是否有权限访问瓦砾。
     *
     * @param player 要检查权限的玩家。
     * @return 当玩家有权限或建筑不存在时返回true，否则返回false。
     */
    @Override
    public boolean hasAccessPermission(final Player player)
    {
        //TODO 每次打开GUI都会调用这个方法，是否合适？
        return building == null || building.getColony().getPermissions().hasPermission(player, Action.ACCESS_HUTS);
    }

    /**
     * 设置方块实体是否有镜像。
     *
     * @param mirror 如果有镜像则为true。
     */
    @Override
    public void setMirror(final boolean mirror)
    {
        this.mirror = mirror;
    }

    /**
     * 检查建筑是否有镜像。
     *
     * @return 如果有镜像则为true。
     */
    @Override
    public boolean isMirrored()
    {
        return mirror;
    }

    /**
     * 获取建筑的风格。
     *
     * @return 风格字符串。
     */
    @Override
    public String getStyle()
    {
        return this.style;
    }

    /**
     * 设置方块实体的风格。
     *
     * @param style 要设置的风格。
     */
    @Override
    public void setStyle(final String style)
    {
        this.style = style;
    }

    @Override
    public ResourceLocation getBuildingName()
    {
        return registryName;
    }

    @Override
    public boolean isMain()
    {
        return true;
    }

    @Override
    public void updateBlockState()
    {
        // 什么都不做
    }

    @Override
    public void setSingle(final boolean single)
    {
        // 什么都不做，这些方块实体总是单独的！
    }

    @NotNull
    @Override
    public <T> LazyOptional<T> getCapability(@NotNull final Capability<T> capability, @Nullable final Direction side)
    {
        if (!remove && capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY && getBuilding() != null)
        {
            if (combinedInv == null)
            {
                //添加额外的容器
                final Set<IItemHandlerModifiable> handlers = new LinkedHashSet<>();
                final Level world = colony.getWorld();
                if (world != null)
                {
                    for (final BlockPos pos : building.getContainers())
                    {
                        if (WorldUtil.isBlockLoaded(world, pos) && !pos.equals(this.worldPosition))
                        {
                            final BlockEntity te = world.getBlockEntity(pos);
                            if (te != null)
                            {
                                if (te instanceof AbstractTileEntityRack)
                                {
                                    handlers.add(((AbstractTileEntityRack) te).getInventory());
                                    ((AbstractTileEntityRack) te).setBuildingPos(this.getBlockPos());
                                }
                                else
                                {
                                    building.removeContainerPosition(pos);
                                }
                            }
                        }
                    }
                }
                handlers.add(this.getInventory());

                combinedInv = LazyOptional.of(() -> new CombinedItemHandler(building.getSchematicName(), handlers.toArray(new IItemHandlerModifiable[0])));
            }
            return (LazyOptional<T>) combinedInv;
        }
        return super.getCapability(capability, side);
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(final int id, @NotNull final Inventory inv, @NotNull final Player player)
    {
        return new ContainerBuildingInventory(id, inv, colonyId, getBlockPos());
    }

    /**
     * 重新激活该方块实体的瓦砾。
     * 加载原理图数据并正确设置风格。
     */
    public void reactivate()
    {
        final List<String> tags = new ArrayList<>(this.getPositionedTags().get(BlockPos.ZERO));
        tags.remove(DEACTIVATED);
        if (!tags.isEmpty())
        {
            this.setStyle(tags.get(0));
        }

        //todo 移除1.19世界生成村庄的重新扫描之后移除
        if (this.getSchematicName().contains("home"))
        {
            this.setSchematicName(this.getSchematicName().replace("home", "residence"));
            this.registryName = new ResourceLocation(Constants.MOD_ID, "residence");
        }
        if (this.getSchematicName().contains("citizen"))
        {
            this.setSchematicName(this.getSchematicName().replace("citizen", "residence"));
            this.registryName = new ResourceLocation(Constants.MOD_ID, "residence");
        }
        String structureName = new StructureName(Structures.SCHEMATICS_PREFIX, this.getStyle(), this.getSchematicName()).toString();

        final LoadOnlyStructureHandler structure = new LoadOnlyStructureHandler(level, this.getPosition(), structureName, new PlacementSettings(), true);
        final Blueprint blueprint = structure.getBluePrint();

        final BlockState structureState = structure.getBluePrint().getBlockInfoAsMap().get(structure.getBluePrint().getPrimaryBlockOffset()).getState();
        if (structureState != null)
        {
            if (!(structureState.getBlock() instanceof AbstractBlockHut) || !(level.getBlockState(this.getPosition()).getBlock() instanceof AbstractBlockHut))
            {
                Log.getLogger().error(String.format("Schematic %s doesn't have a correct Primary Offset", structureName.toString()));
                return;
            }
            final int structureRotation = structureState.getValue(AbstractBlockHut.FACING).get2DDataValue();
            final int worldRotation = level.getBlockState(this.getPosition()).getValue(AbstractBlockHut.FACING).get2DDataValue();

            final int rotation;
            if (structureRotation <= worldRotation)
            {
                rotation = worldRotation - structureRotation;
            }
            else
            {
                rotation = 4 + worldRotation - structureRotation;
            }

            blueprint.rotateWithMirror(BlockPosUtil.getRotationFromRotations(rotation), this.isMirrored() ? Mirror.FRONT_BACK : Mirror.NONE, level);
            final BlockInfo info = blueprint.getBlockInfoAsMap().getOrDefault(blueprint.getPrimaryBlockOffset(), null);
            if (info.getTileEntityData() != null)
            {
                this.readSchematicDataFromNBT(info.getTileEntityData());
            }
        }
    }
}
