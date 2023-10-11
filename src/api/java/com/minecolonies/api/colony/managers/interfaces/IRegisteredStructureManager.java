package com.minecolonies.api.colony.managers.interfaces;

import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.buildings.IBuilding;
import com.minecolonies.api.colony.buildings.IMysticalSite;
import com.minecolonies.api.colony.buildings.workerbuildings.ITownHall;
import com.minecolonies.api.colony.buildings.workerbuildings.IWareHouse;
import com.minecolonies.api.entity.citizen.AbstractEntityCitizen;
import com.minecolonies.api.tileentities.AbstractScarecrowTileEntity;
import com.minecolonies.api.tileentities.AbstractTileEntityColonyBuilding;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/**
 * 用于管理已注册结构的接口。
 * 建筑物、田地、装饰物等。
 */
public interface IRegisteredStructureManager
{
    /**
     * 从NBT中读取建筑物。
     *
     * @param compound 存储建筑物的复合标签。
     */
    void read(@NotNull final CompoundTag compound);

    /**
     * 将建筑物写入NBT。
     *
     * @param compound 存储建筑物的复合标签。
     */
    void write(@NotNull final CompoundTag compound);

    /**
     * 清除建筑物的isDirty标志。
     */
    void clearDirty();

    /**
     * 向订阅者发送建筑物的数据包。
     *
     * @param closeSubscribers 旧订阅者集合。
     * @param newSubscribers   新订阅者集合。
     */
    void sendPackets(Set<ServerPlayer> closeSubscribers, final Set<ServerPlayer> newSubscribers);

    /**
     * 在殖民地周期性更新时处理建筑物。
     *
     * @param colony 事件对象。
     */
    void onColonyTick(IColony colony);

    /**
     * 清理建筑物。
     *
     * @param colony 在世界周期性更新事件中的殖民地。
     */
    void cleanUpBuildings(final IColony colony);

    /**
     * 获取特定建筑物。
     *
     * @param pos 建筑物的坐标。
     * @return 建筑物。
     */
    IBuilding getBuilding(BlockPos pos);

    /**
     * 获取休闲场所的位置列表。
     * @return 位置列表。
     */
    List<BlockPos> getLeisureSites();

    /**
     * 获取满足条件的第一个建筑物的坐标。
     * @param predicate 用于匹配建筑物的条件。
     * @return 坐标或null。
     */
    @Nullable
    BlockPos getFirstBuildingMatching(final Predicate<IBuilding> predicate);

    /**
     * 注册新的休闲场所。
     * @param pos 位置坐标。
     */
    void addLeisureSite(BlockPos pos);

    /**
     * 移除休闲场所。
     * @param pos 位置坐标。
     */
    void removeLeisureSite(BlockPos pos);

    /**
     * 获取与位置最近的仓库。
     * @param pos 位置坐标。
     * @return 最近的仓库。
     */
    @Nullable
    IWareHouse getClosestWarehouseInColony(BlockPos pos);

    /**
     * 返回殖民地内的所有建筑物的映射。键为ID（坐标），值为建筑物对象。
     *
     * @return ID（坐标）为键，建筑物为值的映射。
     */
    @NotNull
    Map<BlockPos, IBuilding> getBuildings();

    /**
     * 获取殖民地的市政厅。
     *
     * @return 市政厅建筑物。
     */
    ITownHall getTownHall();

    /**
     * 获取已建造的神秘遗址中的最高级别。
     *
     * @return 所有神秘遗址中的最高级别，如果没有建造神秘遗址，则返回零。
     */
    int getMysticalSiteMaxBuildingLevel();

    /**
     * 检查殖民地是否已经放置了仓库。
     *
     * @return 如果是，则返回true。
     */
    boolean hasWarehouse();

    /**
     * 检查殖民地是否已经放置了神秘遗址。
     *
     * @return 如果是，则返回true。
     */
    boolean hasMysticalSite();

    /**
     * 检查殖民地是否已经放置了市政厅。
     *
     * @return 如果是，则返回true。
     */
    boolean hasTownHall();

    /**
     * 根据ID从殖民地中获取建筑物。建筑物将被转换为指定的类型。
     *
     * @param buildingId 建筑物的ID（坐标）。
     * @param type       建筑物的类型。
     * @param <B>        建筑物类。
     * @return 具有指定ID的建筑物。
     */
    @Nullable
    <B extends IBuilding> B getBuilding(final BlockPos buildingId, @NotNull final Class<B> type);

    /**
     * 获取不可修改版本的农田列表。
     *
     * @return 农田和它们的ID列表。
     */
    @NotNull
    List<BlockPos> getFields();

    /**
     * 从一个瓜地方块创建一个农田，并将其添加到殖民地中。
     *
     * @param tileEntity 包含库存的稻草人方块实体。
     * @param pos        农田的位置。
     * @param world      农田所在的世界。
     */
    void addNewField(final AbstractScarecrowTileEntity tileEntity, final BlockPos pos, final Level world);

    /**
     * 返回一个尚未被占用的农田。
     *
     * @param owner 农田的所有者ID。
     * @param world 农田所在的世界。
     * @return 如果有可用的农田，则返回一个农田，否则返回null。
     */
    @Nullable
    AbstractScarecrowTileEntity getFreeField(final int owner, final Level world);

    /**
     * 从殖民地中移除一个建筑物（当其被摧毁时）。
     *
     * @param subscribers 殖民地的订阅者集合。
     * @param building    要移除的建筑物。
     */
    void removeBuilding(@NotNull final IBuilding building, final Set<ServerPlayer> subscribers);

    /**
     * 标记建筑数据已更改。
     */
    void markBuildingsDirty();

    /**
     * 从一个方块实体创建一个建筑物，并将其添加到殖民地中。
     *
     * @param tileEntity 要创建建筑物的方块实体。
     * @param world      要添加到的世界。
     * @return 已创建并添加的建筑物。
     */
    @Nullable
    IBuilding addNewBuilding(@NotNull final AbstractTileEntityColonyBuilding tileEntity, final Level world);

    /**
     * 从农田列表中移除一个农田。
     *
     * @param pos 农田的位置ID。
     */
    void removeField(final BlockPos pos);

    /**
     * 为特定的居民计算合适的建筑物。
     *
     * @param citizen   居民。
     * @param building  建筑物类型。
     * @return 该建筑物的位置。
     */
    BlockPos getBestBuilding(final AbstractEntityCitizen citizen, final Class<? extends IBuilding> building);

    /**
     * 为特定位置计算合适的建筑物。
     *
     * @param pos       位置。
     * @param building  建筑物类型。
     * @return 该建筑物的位置。
     */
    BlockPos getBestBuilding(final BlockPos pos, final Class<? extends IBuilding> building);

    /**
     * 返回殖民地内随机匹配过滤条件的建筑物。
     *
     * @param filterPredicate 用于筛选的条件。
     * @return 随机的建筑物。如果没有找到匹配条件的建筑物，则返回null。
     */
    BlockPos getRandomBuilding(Predicate<IBuilding> filterPredicate);

    /**
     * 检查是否有警卫建筑物靠近给定的建筑物。
     *
     * @param building 要检查的建筑物。
     * @return 如果没有靠近的警卫塔，则返回false，否则返回true。
     */
    boolean hasGuardBuildingNear(IBuilding building);

    /**
     * 当某个警卫建筑物的等级发生变化时触发的事件。
     * @param guardBuilding 警卫建筑物。
     * @param newLevel 新的等级。
     */
    void guardBuildingChangedAt(IBuilding guardBuilding, int newLevel);

    /**
     * 设置市政厅建筑物。
     *
     * @param building 要设置的市政厅建筑物。
     */
    void setTownHall(@Nullable final ITownHall building);

    /**
     * 从BuildingManager中移除一个仓库。
     *
     * @param wareHouse 要移除的仓库。
     */
    void removeWareHouse(final IWareHouse wareHouse);

    /**
     * 获取此殖民地中的仓库列表。
     *
     * @return 仓库列表。
     */
    List<IWareHouse> getWareHouses();

    /**
     * 从BuildingManager中移除一个神秘遗址。
     *
     * @param mysticalSite 要移除的神秘遗址。
     */
    void removeMysticalSite(final IMysticalSite mysticalSite);

    /**
     * 获取此殖民地中的神秘遗址列表。
     *
     * @return 神秘遗址列表。
     */
    List<IMysticalSite> getMysticalSites();

    /**
     * 检查是否允许在新建建筑物的位置放置方块。
     *
     * @param block  要检查的方块。
     * @param pos    位置坐标。
     * @param player 尝试放置的玩家。
     * @return 如果允许放置，则返回true。
     */
    boolean canPlaceAt(Block block, BlockPos pos, Player player);

    /**
     * 检查区块位置是否在殖民地的建筑区域内。
     * @param chunk 要检查的区块。
     * @return 如果在建筑区域内，则返回true。
     */
    boolean isWithinBuildingZone(final LevelChunk chunk);

    /**
     * 获取带有空床位的房屋。
     * @return 房屋或null。
     */
    IBuilding getHouseWithSpareBed();

    /**
     * 当殖民地中的建筑物完成升级状态时执行的操作。
     *
     * @param building 已升级的建筑物。
     * @param level    新等级。
     */
    void onBuildingUpgradeComplete(@Nullable IBuilding building, int level);

    /**
     * 获取前往随机休闲场所的位置。
     * @return 位置坐标。
     */
    BlockPos getRandomLeisureSite();
}
