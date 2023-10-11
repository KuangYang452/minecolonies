package com.minecolonies.api.colony;

import com.minecolonies.api.colony.managers.interfaces.*;
import com.minecolonies.api.colony.permissions.IPermissions;
import com.minecolonies.api.colony.requestsystem.manager.IRequestManager;
import com.minecolonies.api.colony.requestsystem.requester.IRequester;
import com.minecolonies.api.colony.workorders.IWorkManager;
import com.minecolonies.api.entity.citizen.AbstractEntityCitizen;
import com.minecolonies.api.research.IResearchManager;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.player.Player;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.BlockPos;
import net.minecraft.ChatFormatting;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.event.TickEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static com.minecolonies.api.util.constant.ColonyConstants.TEAM_COLONY_NAME;

/**
 * Colony和ColonyView的接口，必须实现以下方法。
 */
public interface IColony
{
    Capability<IColonyTagCapability> CLOSE_COLONY_CAP = CapabilityManager.get(new CapabilityToken<>() {});

    void onWorldLoad(@NotNull Level w);

    void onWorldUnload(@NotNull Level w);

    void onServerTick(@NotNull TickEvent.ServerTickEvent event);

    @NotNull
    IWorkManager getWorkManager();

    void onWorldTick(@NotNull TickEvent.LevelTickEvent event);

    /**
     * 返回殖民地的位置。
     *
     * @return 殖民地的位置。
     */
    BlockPos getCenter();

    /**
     * 返回殖民地的名称。
     *
     * @return 殖民地的名称。
     */
    String getName();

    void setName(String n);

    /**
     * 返回殖民地的权限。
     *
     * @return 殖民地的{@link IPermissions}。
     */
    IPermissions getPermissions();

    /**
     * 确定给定的区块坐标是否被视为在殖民地的边界内。
     *
     * @param w   要检查的世界。
     * @param pos 方块位置。
     * @return 如果在殖民地内，则为true，否则为false。
     */
    boolean isCoordInColony(Level w, BlockPos pos);

    /**
     * 返回到中心的平方(x, z)距离。
     *
     * @param pos 方块位置。
     * @return (x, z)方向的距离的平方。
     */
    long getDistanceSquared(BlockPos pos);

    /**
     * 返回殖民地是否有市政厅。
     *
     * @return 是否有市政厅。
     */
    boolean hasTownHall();

    /**
     * 返回此殖民地的唯一ID。
     *
     * @return 代表ID的整数。
     */
    int getID();

    /**
     * 检查殖民地是否有仓库。
     *
     * @return 如果有则为true。
     */
    boolean hasWarehouse();

    /**
     * 检查殖民地是否在特定级别或更高级别拥有建筑类型。
     *
     * @param building       基于原理图名称的建筑的字符串标识符。
     * @param level          级别要求。
     * @param singleBuilding 如果为true，则要求至少一个建筑满足最低要求。
     * @return 如果至少有一个至少满足目标级别的建筑，则为true。
     */
    boolean hasBuilding(final String building, final int level, final boolean singleBuilding);

    /**
     * 为所有殖民地（包括Colony和ColonyView）定义团队名称。
     *
     * @return 团队名称
     */
    default String getTeamName()
    {
        final String dim = getDimension().location().getPath();
        return TEAM_COLONY_NAME + "_" + (dim.length() > 10 ? dim.hashCode() : dim) + "_" + getID();
    }

    /**
     * 返回此殖民地的旗帜图案，作为ListTag。
     *
     * @return 旗帜图案的ListTag。
     */
    ListTag getColonyFlag();

    /**
     * 是否为殖民地的白天。
     *
     * @return 如果是白天则为true。
     */
    boolean isDay();

    /**
     * 获取殖民地的团队。
     *
     * @return 殖民地的团队。
     */
    PlayerTeam getTeam();

    /**
     * 获取玩家对殖民地的最后联系时间（以小时为单位）。
     *
     * @return 描述性值的整数。
     */
    int getLastContactInHours();

    /**
     * 获取殖民地所在的世界。
     *
     * @return 殖民地所在的世界。
     */
    Level getWorld();

    /**
     * 获取此殖民地的当前{@link IRequestManager}。如果当前殖民地不支持请求系统，则返回null。
     *
     * @return 此殖民地的{@link IRequestManager}，如果不支持则返回null。
     */
    @NotNull
    IRequestManager getRequestManager();

    /**
     * 调用以标记此殖民地为脏，并需要同步/保存。
     */
    void markDirty();

    /**
     * 用于检查是否可以通过自动清理删除殖民地。
     *
     * @return 如果可以则为true。
     */
    boolean canBeAutoDeleted();

    /**
     * 用于从给定位置获取{@link IRequester}的方法。始终是一个建筑物。
     *
     * @param pos 要获取其作为请求者的建筑物的位置。
     * @return 位置的{@link IRequester}，如果没有则为null。
     */
    @Nullable
    IRequester getRequesterBuildingForPosition(@NotNull final BlockPos pos);

    /**
     * 移除访问中的玩家。
     *
     * @param player 玩家。
     */
    void removeVisitingPlayer(final Player player);

    /**
     * 获取应该接收消息的殖民地中的玩家。
     *
     * @return 玩家列表。
     */
    @NotNull
    List<Player> getMessagePlayerEntities();

    @NotNull
    default List<BlockPos> getWayPoints(@NotNull BlockPos position, @NotNull BlockPos target)
    {
        final List<BlockPos> tempWayPoints = new ArrayList<>();
        tempWayPoints.addAll(getWayPoints().keySet());
        tempWayPoints.addAll(getBuildingManager().getBuildings().keySet());

        final double maxX = Math.max(position.getX(), target.getX());
        final double maxZ = Math.max(position.getZ(), target.getZ());

        final double minX = Math.min(position.getX(), target.getX());
        final double minZ = Math.min(position.getZ(), target.getZ());

        final Iterator<BlockPos> iterator = tempWayPoints.iterator();
        while (iterator.hasNext())
        {
            final BlockPos p = iterator.next();
            final int x = p.getX();
            final int z = p.getZ();
            if (x < minX || x > maxX || z < minZ || z > maxZ)
            {
                iterator.remove();
            }
        }

        return tempWayPoints;
    }

    double getOverallHappiness();

    Map<BlockPos, BlockState> getWayPoints();

    String getStyle();

    void setStyle(String style);

    IRegisteredStructureManager getBuildingManager();

    ICitizenManager getCitizenManager();

    IGraveManager getGraveManager();

    /**
     * 获取访问者管理器
     *
     * @return 管理器
     */
    IVisitorManager getVisitorManager();

    IRaiderManager getRaiderManager();

    /**
     * 获取殖民地的事件管理器。
     *
     * @return 事件管理器。
     */
    IEventManager getEventManager();

    /**
     * 获取殖民地的繁殖管理器。
     *
     * @return 繁殖管理器。
     */
    IReproductionManager getReproductionManager();

    /**
     * 获取殖民地的事件描述管理器。
     *
     * @return 事件描述管理器。
     */
    IEventDescriptionManager getEventDescriptionManager();

    IColonyPackageManager getPackageManager();

    IProgressManager getProgressManager();

    /**
     * 添加访问中的玩家。
     *
     * @param player 玩家。
     */
    void addVisitingPlayer(final Player player);

    /**
     * 获取殖民地维度。
     *
     * @return 维度ID。
     */
    ResourceKey<Level> getDimension();

    /**
     * 检查殖民地是在服务器端还是客户端。
     *
     * @return 如果是服务器端则为true。
     */
    boolean isRemote();

    /**
     * 获取研究管理器。
     *
     * @return 研究管理器对象。
     */
    IResearchManager getResearchManager();

    /**
     * 保存雇佣兵使用的时间，以设置冷却时间。
     */
    void usedMercenaries();

    /**
     * 获取上次使用雇佣兵的时间。
     *
     * @return 雇佣兵使用时间。
     */
    long getMercenaryUseTime();

    CompoundTag getColonyTag();

    boolean isColonyUnderAttack();

    boolean isValidAttackingPlayer(Player entity);

    boolean isValidAttackingGuard(AbstractEntityCitizen entity);

    void setColonyColor(ChatFormatting color);

    void setColonyFlag(ListTag patterns);

    void setManualHousing(boolean manualHousing);

    void addWayPoint(BlockPos pos, BlockState newWayPointState);

    void addGuardToAttackers(AbstractEntityCitizen entityCitizen, Player followPlayer);

    void addFreePosition(BlockPos pos);

    void addFreeBlock(Block block);

    void removeFreePosition(BlockPos pos);

    void removeFreeBlock(Block block);

    void setCanBeAutoDeleted(boolean canBeDeleted);

    void setManualHiring(boolean manualHiring);

    CompoundTag write(CompoundTag colonyCompound);

    void read(CompoundTag compound);

    void setMoveIn(boolean newMoveIn);

    /**
     * 返回一组接收殖民地重要消息的玩家。
     *
     * @return 玩家集合。
     */
    @NotNull
    List<Player> getImportantMessageEntityPlayers();

    boolean isManualHiring();

    boolean isManualHousing();

    boolean canMoveIn();

    /**
     * 尝试使用给定数量的额外成长时间来供养儿童。
     *
     * @param amount 要使用的数量。
     * @return 如果已用尽则为true。
     */
    boolean useAdditionalChildTime(int amount);

    /**
     * 设置殖民地是否有子代。
     */
    void updateHasChilds();

    /**
     * 将加载的区块添加到殖民地列表中。
     *
     * @param chunkPos 要添加的区块位置。
     * @param chunk    区块。
     */
    void addLoadedChunk(long chunkPos, final LevelChunk chunk);

    /**
     * 从殖民地列表中移除区块。
     *
     * @param chunkPos 要移除的区块位置。
     */
    void removeLoadedChunk(long chunkPos);

    /**
     * 返回加载的区块数量。
     *
     * @return 区块数量。
     */
    int getLoadedChunkCount();

    /**
     * 返回殖民地的当前状态。
     *
     * @return 状态。
     */
    ColonyState getState();

    /**
     * 殖民地当前是否处于活动状态。
     *
     * @return 如果是则为true。
     */
    boolean isActive();

    /**
     * 获取殖民地通过票证加载的区块位置集合。
     *
     * @return 位置集合。
     */
    Set<Long> getTicketedChunks();

    /**
     * 设置殖民地的纹理样式。
     *
     * @param style 要设置的样式。
     */
    void setTextureStyle(String style);

    /**
     * 获取殖民地样式。
     *
     * @return 样式的字符串ID。
     */
    String getTextureStyleId();
}
