package com.minecolonies.api.colony.buildings;

import com.minecolonies.api.tileentities.AbstractTileEntityColonyBuilding;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.util.List;

public interface IBuildingContainer extends ISchematicProvider, ICapabilityProvider
{
    @Override
    void deserializeNBT(CompoundTag compound);

    @Override
    CompoundTag serializeNBT();

    /**
     * 获取建筑物的拾取优先级。
     *
     * @return 优先级，一个整数。
     */
    int getPickUpPriority();

    /**
     * 增加或减少当前的拾取优先级。
     *
     * @param value 要添加的新优先级。
     */
    void alterPickUpPriority(int value);

    /**
     * 添加一个新的容器位置到建筑物中。
     *
     * @param pos 要添加的位置。
     */
    void addContainerPosition(@NotNull BlockPos pos);

    /**
     * 从建筑物中移除一个容器。
     *
     * @param pos 要移除的位置。
     */
    void removeContainerPosition(BlockPos pos);

    /**
     * 获取所有属于建筑物的容器（包括小屋块）。
     *
     * @return 列表的副本，以避免当前修改异常。
     */
    List<BlockPos> getContainers();

    /**
     * 注册一个块状态和位置。我们抑制此警告，因为该参数将在重写此方法的子类中使用。
     *
     * @param blockState 要注册的块状态
     * @param pos        块状态的位置
     * @param world      要在其中注册的世界。
     */
    void registerBlockPosition(@NotNull BlockState blockState, @NotNull BlockPos pos, @NotNull Level world);

    /**
     * 注册一个块和位置。我们抑制此警告，因为该参数将在重写此方法的子类中使用。
     *
     * @param block 要注册的块
     * @param pos   块的位置
     * @param world 要在其中注册的世界。
     */
    @SuppressWarnings("squid:S1172")
    void registerBlockPosition(@NotNull Block block, @NotNull BlockPos pos, @NotNull Level world);

    /**
     * 返回属于殖民地建筑物的瓦片实体。
     *
     * @return 建筑物的 {@link AbstractTileEntityColonyBuilding} 对象。
     */
    AbstractTileEntityColonyBuilding getTileEntity();

    /**
     * 为建筑物设置瓦片实体。
     *
     * @param te 瓦片实体
     */
    void setTileEntity(AbstractTileEntityColonyBuilding te);
    /**
     * 检索所请求的能力在特定侧的可选处理程序。
     * 返回值可以对于多个侧是相同的。
     * 鼓励Mod开发者缓存此值，使用Optional的监听功能来
     * 通知是否丢失了所请求的能力。
     *
     * @param cap 要检查的能力
     * @param direction 要检查的侧面，
     *   可以为NULL。NULL定义为表示“内部”或“自身”
     * @return 所请求的能力的可选项，其中包含所请求的能力。
     */
    @Nonnull
    @Override
    <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, final Direction direction);
}
