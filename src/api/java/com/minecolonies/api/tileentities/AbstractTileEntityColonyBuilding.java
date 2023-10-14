package com.minecolonies.api.tileentities;

import com.ldtteam.structurize.blocks.interfaces.IBlueprintDataProvider;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.buildings.IBuilding;
import com.minecolonies.api.colony.buildings.IBuildingContainer;
import com.minecolonies.api.colony.buildings.views.IBuildingView;
import com.minecolonies.api.util.InventoryFunctions;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Tuple;
import net.minecraft.core.BlockPos;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

public abstract class AbstractTileEntityColonyBuilding extends TileEntityRack implements IBlueprintDataProvider
{
    /**
     * TE数据的版本。
     */
    private static final String TAG_VERSION = "version";
    private static final int    VERSION     = 2;

    /**
     * 方案图的角落位置，相对于TE位置。
     */
    private BlockPos corner1             = BlockPos.ZERO;
    private BlockPos corner2 = BlockPos.ZERO;

    /**
     * TE的方案名称
     */
    private String schematicName = "";

    /**
     * 相对于TE位置的块位置和字符串标签的映射。
     */
    private Map<BlockPos, List<String>> tagPosMap = new HashMap<>();

    /**
     * 检查建筑是否可能有旧数据。
     */
    private int version = 0;

    public AbstractTileEntityColonyBuilding(final BlockEntityType<? extends AbstractTileEntityColonyBuilding> type, final BlockPos pos, final BlockState state)
    {
        super(type, pos, state);
    }

    /**
     * 查找第一个与{@code is}类型相同的@see ItemStack。它将从箱子中取出并放入工人的库存中。确保工人站在箱子旁边以
     * 不破坏沉浸感。还要确保有足够的库存空间存放堆栈。
     *
     * @param entity                      箱子或建筑物的tileEntity。
     * @param itemStackSelectionPredicate 物品堆叠的断言。
     * @return 如果找到了堆栈，则返回true。
     */
    public static boolean isInTileEntity(final ICapabilityProvider entity, @NotNull final Predicate<ItemStack> itemStackSelectionPredicate)
    {
        return InventoryFunctions.matchFirstInProvider(entity, itemStackSelectionPredicate);
    }

    /**
     * 返回殖民地的ID。
     *
     * @return 殖民地的ID。
     */
    public abstract int getColonyId();

    /**
     * 返回tile entity的殖民地。
     *
     * @return tile entity的殖民地。
     */
    public abstract IColony getColony();

    /**
     * 设置tile entity的殖民地。
     *
     * @param c 要设置的殖民地。
     */
    public abstract void setColony(IColony c);

    /**
     * 返回tile entity的位置。
     *
     * @return tile entity的块坐标。
     */
    public abstract BlockPos getPosition();

    /**
     * 检查特定物品并返回包含该物品的箱子的位置。
     *
     * @param itemStackSelectionPredicate 要搜索的堆栈。
     * @return 位置或null。
     */
    @Nullable
    public abstract BlockPos getPositionOfChestWithItemStack(@NotNull Predicate<ItemStack> itemStackSelectionPredicate);

    /**
     * 返回与tile entity相关联的建筑物。
     *
     * @return 与tile entity相关联的{@link IBuildingContainer}。
     */
    public abstract IBuilding getBuilding();

    /**
     * 设置与tile entity相关联的建筑物。
     *
     * @param b 要与tile entity相关联的{@link IBuildingContainer}。
     */
    public abstract void setBuilding(IBuilding b);

    /**
     * 返回与tile entity相关联的建筑物的视图。
     *
     * @return tile entity相关联的{@link IBuildingView}。
     */
    public abstract IBuildingView getBuildingView();

    /**
     * 检查玩家是否有权访问小屋。
     *
     * @param player 要检查权限的玩家。
     * @return 如果玩家有访问权限，或者建筑不存在，则返回true，否则返回false。
     */
    public abstract boolean hasAccessPermission(Player player);

    /**
     * 设置实体是否镜像。
     *
     * @param mirror 如果是镜像则为true。
     */
    public abstract void setMirror(boolean mirror);

    /**
     * 检查建筑是否镜像。
     *
     * @return 如果是镜像则为true。
     */
    public abstract boolean isMirrored();

    /**
     * 样式的获取器。
     *
     * @return 样式的字符串。
     */
    public abstract String getStyle();

    /**
     * 设置tileEntity的样式。
     *
     * @param style 要设置的样式。
     */
    public abstract void setStyle(String style);

    /**
     * 获取属于此{@link AbstractTileEntityColonyBuilding}的建筑名称。
     *
     * @return 建筑的名称。
     */
    public abstract ResourceLocation getBuildingName();

    @Override
    public String getSchematicName()
    {
        return schematicName;
    }

    @Override
    public void setSchematicName(final String name)
    {
        schematicName = name;
    }

    @Override
    public Map<BlockPos, List<String>> getPositionedTags()
    {
        return tagPosMap;
    }

    @Override
    public void setPositionedTags(final Map<BlockPos, List<String>> positionedTags)
    {
        tagPosMap = positionedTags;
        setChanged();
    }

    @Override
    public Tuple<BlockPos, BlockPos> getSchematicCorners()
    {
        return new Tuple<>(corner1, corner2);
    }

    @Override
    public void setSchematicCorners(final BlockPos pos1, final BlockPos pos2)
    {
        corner1 = pos1;
        corner2 = pos2;
    }

    @Override
    public void load(@NotNull final CompoundTag compound)
    {
        super.load(compound);
        readSchematicDataFromNBT(compound);
        this.version = compound.getInt(TAG_VERSION);
    }

    @Override
    public void readSchematicDataFromNBT(final CompoundTag originalCompound)
    {
        final String old = getSchematicName();
        IBlueprintDataProvider.super.readSchematicDataFromNBT(originalCompound);

        if (level == null || level.isClientSide || getColony() == null || getColony().getBuildingManager() == null)
        {
            return;
        }

        final IBuilding building = getColony().getBuildingManager().getBuilding(worldPosition);
        if (building != null)
        {
            building.onUpgradeSchematicTo(old, getSchematicName(), this);
        }
        this.version = VERSION;
    }

    @Override
    public void saveAdditional(@NotNull final CompoundTag compound)
    {
        super.saveAdditional(compound);
        writeSchematicDataToNBT(compound);
        compound.putInt(TAG_VERSION, this.version);
    }

    @Override
    public BlockPos getTilePos()
    {
        return worldPosition;
    }

    /**
     * 检查TE是否使用旧数据版本。
     * @return 如果是，则返回true。
     */
    public boolean isOutdated()
    {
        return version < VERSION;
    }
}
