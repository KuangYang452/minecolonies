package com.minecolonies.coremod.colony;

import com.google.common.collect.ImmutableList;
import com.minecolonies.api.blocks.ModBlocks;
import com.minecolonies.api.colony.ColonyState;
import com.minecolonies.api.colony.ICitizenData;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.IColonyTagCapability;
import com.minecolonies.api.colony.buildings.IBuilding;
import com.minecolonies.api.colony.managers.interfaces.*;
import com.minecolonies.api.colony.permissions.Action;
import com.minecolonies.api.colony.permissions.Rank;
import com.minecolonies.api.colony.requestsystem.manager.IRequestManager;
import com.minecolonies.api.colony.requestsystem.requester.IRequester;
import com.minecolonies.api.colony.workorders.IWorkManager;
import com.minecolonies.api.entity.ai.statemachine.tickratestatemachine.ITickRateStateMachine;
import com.minecolonies.api.entity.ai.statemachine.tickratestatemachine.TickRateStateMachine;
import com.minecolonies.api.entity.ai.statemachine.tickratestatemachine.TickingTransition;
import com.minecolonies.api.entity.citizen.AbstractEntityCitizen;
import com.minecolonies.api.research.IResearchManager;
import com.minecolonies.api.util.BlockPosUtil;
import com.minecolonies.api.util.Log;
import com.minecolonies.api.util.MessageUtils;
import com.minecolonies.api.util.WorldUtil;
import com.minecolonies.api.util.constant.Constants;
import com.minecolonies.api.util.constant.NbtTagConstants;
import com.minecolonies.api.util.constant.Suppression;
import com.minecolonies.coremod.MineColonies;
import com.minecolonies.coremod.Network;
import com.minecolonies.coremod.colony.managers.*;
import com.minecolonies.coremod.colony.permissions.Permissions;
import com.minecolonies.coremod.colony.pvp.AttackingPlayer;
import com.minecolonies.coremod.colony.requestsystem.management.manager.StandardRequestManager;
import com.minecolonies.coremod.colony.workorders.WorkManager;
import com.minecolonies.coremod.network.messages.client.colony.ColonyViewRemoveWorkOrderMessage;
import com.minecolonies.coremod.permissions.ColonyPermissionEventHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BannerPattern;
import net.minecraft.world.level.block.entity.BannerPatterns;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static com.minecolonies.api.colony.ColonyState.*;
import static com.minecolonies.api.entity.ai.statemachine.tickratestatemachine.TickRateConstants.MAX_TICKRATE;
import static com.minecolonies.api.util.constant.ColonyConstants.*;
import static com.minecolonies.api.util.constant.Constants.DEFAULT_STYLE;
import static com.minecolonies.api.util.constant.Constants.TICKS_SECOND;
import static com.minecolonies.api.util.constant.NbtTagConstants.*;
import static com.minecolonies.api.util.constant.TranslationConstants.*;
import static com.minecolonies.coremod.MineColonies.getConfig;

/**
 * 此类描述了一个殖民地，并包含了所有操作殖民地所需的数据和方法。
 */
@SuppressWarnings({Suppression.BIG_CLASS, Suppression.SPLIT_CLASS})
public class Colony implements IColony
{
    /**
     * 建筑的默认样式。
     */
    private String style = DEFAULT_STYLE;

    /**
     * 殖民地的ID。
     */
    private final int id;

    /**
     * 殖民地的维度。
     */
    private ResourceKey<Level> dimensionId;

    /**
     * 殖民地的加载的区块列表。
     */
    private Set<Long> loadedChunks = new HashSet<>();

    /**
     * 殖民地的加载的区块列表。
     */
    public Set<Long> ticketedChunks = new HashSet<>();

    private boolean ticketedChunksDirty = true;

    /**
     * 必须强制加载的区块列表。
     */
    private Set<Long> pendingChunks = new HashSet<>();

    /**
     * 等待卸载的区块列表，其票证已被移除。
     */
    private Set<Long> pendingToUnloadChunks = new HashSet<>();

    /**
     * 殖民地的路标列表。
     */
    private final Map<BlockPos, BlockState> wayPoints = new HashMap<>();

    /**
     * 殖民地的工作管理器（请求系统）。
     */
    private final WorkManager workManager = new WorkManager(this);

    /**
     * 殖民地的建筑管理器。
     */
    private final IRegisteredStructureManager buildingManager = new RegisteredStructureManager(this);

    /**
     * 殖民地的墓地管理器。
     */
    private final IGraveManager graveManager = new GraveManager(this);

    /**
     * 殖民地的居民管理器。
     */
    private final ICitizenManager citizenManager = new CitizenManager(this);

    /**
     * 殖民地的访客管理器。
     */
    private final IVisitorManager visitorManager = new VisitorManager(this);

    /**
     * 殖民地的蛮族管理器。
     */
    private final IRaiderManager raidManager = new RaidManager(this);

    /**
     * 殖民地的事件管理器。
     */
    private final IEventManager eventManager = new EventManager(this);

    /**
     * 殖民地的繁殖管理器。
     */
    private final IReproductionManager reproductionManager = new ReproductionManager(this);

    /**
     * 殖民地的事件描述管理器。
     */
    private final IEventDescriptionManager eventDescManager = new EventDescriptionManager(this);

    /**
     * 殖民地的包管理器。
     */
    private final IColonyPackageManager packageManager = new ColonyPackageManager(this);

    /**
     * 殖民地的进度管理器。
     */
    private final IProgressManager progressManager = new ProgressManager(this);

    /**
     * 玩家可以自由交互的位置。
     */
    private final Set<BlockPos> freePositions = new HashSet<>();

    /**
     * 玩家可以自由交互的方块。
     */
    private final Set<Block> freeBlocks = new HashSet<>();

    /**
     * 殖民地权限事件处理器。
     */
    private ColonyPermissionEventHandler eventHandler;

    /**
     * 此殖民地是否可以自动删除。
     */
    private boolean canColonyBeAutoDeleted = true;

    /**
     * 用于确定当前是白天还是黑夜的变量。
     */
    private boolean isDay = true;

    /**
     * 殖民地当前运行的世界。
     */
    @Nullable
    private Level world = null;

    /**
     * 殖民地的招聘模式。
     */
    private boolean manualHiring = false;

    /**
     * 殖民地的住房模式。
     */
    private boolean manualHousing = false;

    /**
     * 居民是否可以迁入。
     */
    private boolean moveIn = true;

    /**
     * 殖民地的名称。
     */
    private String name = "ERROR(Wasn't placed by player)";

    /**
     * 殖民地的中心位置。
     */
    private BlockPos center;

    /**
     * 殖民地的权限对象。
     */
    @NotNull
    private Permissions permissions;

    /**
     * 分配给殖民地的请求管理器。
     */
    private IRequestManager requestManager;

    /**
     * 分配给殖民地的研究管理器。
     */
    private IResearchManager researchManager;

    /**
     * 殖民地本身的NBTTag复合标签。
     */
    private CompoundTag colonyTag;

    /**
     * 访问殖民地的玩家列表。
     */
    private final List<Player> visitingPlayers = new ArrayList<>();

    /**
     * 攻击殖民地的玩家列表。
     */
    private final List<AttackingPlayer> attackingPlayers = new ArrayList<>();

    /**
     * 殖民地的状态机。
     */
    private final ITickRateStateMachine<ColonyState> colonyStateMachine;

    /**
     * 殖民地是否已更改。
     */
    private boolean isActive = true;

    /**
     * 殖民地团队颜色。
     */
    private ChatFormatting colonyTeamColor = ChatFormatting.WHITE;

    /**
     * 殖民地旗帜，作为一组图案的列表。
     */
    private ListTag colonyFlag = new BannerPattern.Builder()
                                   .addPattern(BannerPatterns.BASE, DyeColor.WHITE)
                                   .toListTag();

    /**
     * 上次雇佣兵使用的时间。
     */
    private long mercenaryLastUse = 0;

    /**
     * 当殖民地未加载时收集的额外儿童时间数量。
     */
    private int additionalChildTime = 0;

    /**
     * 当殖民地未加载时要存储的额外儿童时间的最大数量。
     */
    private static final int maxAdditionalChildTime = 70000;

    /**
     * 殖民地是否有儿童的布尔值。
     */
    private boolean hasChilds = false;

    /**
     * 上次服务器在线的时间。
     */
    public long lastOnlineTime = 0;

    /**
     * 强制区块加载计时器。
     */
    private int forceLoadTimer = 0;

    /**
     * 殖民地的纹理风格。
     */
    private String textureStyle = "default";

    /**
     * 用于新创建的殖民地的构造函数。
     *
     * @param id 殖民地的ID。
     * @param w  殖民地所在的世界。
     * @param c  殖民地的中心（市政厅的位置）。
     */
    @SuppressWarnings("squid:S2637")
    Colony(final int id, @Nullable final Level w, final BlockPos c)
    {
        this(id, w);
        center = c;
        this.permissions = new Permissions(this);
        requestManager = new StandardRequestManager(this);
        researchManager = new ResearchManager(this);
    }

    /**
     * 基础构造函数。
     *
     * @param id    殖民地的当前ID。
     * @param world 殖民地所在的世界。
     */
    protected Colony(final int id, @Nullable final Level world)
    {
        this.id = id;
        if (world != null)
        {
            this.dimensionId = world.dimension();
            onWorldLoad(world);
            checkOrCreateTeam();
        }
        this.permissions = new Permissions(this);
        researchManager = new ResearchManager(this);
        colonyStateMachine = new TickRateStateMachine<>(INACTIVE, e -> {});

        colonyStateMachine.addTransition(new TickingTransition<>(INACTIVE, () -> true, this::updateState, UPDATE_STATE_INTERVAL));
        colonyStateMachine.addTransition(new TickingTransition<>(UNLOADED, () -> true, this::updateState, UPDATE_STATE_INTERVAL));
        colonyStateMachine.addTransition(new TickingTransition<>(ACTIVE, () -> true, this::updateState, UPDATE_STATE_INTERVAL));
        colonyStateMachine.addTransition(new TickingTransition<>(ACTIVE, () -> true, () -> {
            this.getCitizenManager().tickCitizenData();
            return null;
        }, TICKS_SECOND));

        colonyStateMachine.addTransition(new TickingTransition<>(ACTIVE, this::updateSubscribers, () -> ACTIVE, UPDATE_SUBSCRIBERS_INTERVAL));
        colonyStateMachine.addTransition(new TickingTransition<>(ACTIVE, this::tickRequests, () -> ACTIVE, UPDATE_RS_INTERVAL));
        colonyStateMachine.addTransition(new TickingTransition<>(ACTIVE, this::checkDayTime, () -> ACTIVE, UPDATE_DAYTIME_INTERVAL));
        colonyStateMachine.addTransition(new TickingTransition<>(ACTIVE, this::updateWayPoints, () -> ACTIVE, CHECK_WAYPOINT_EVERY));
        colonyStateMachine.addTransition(new TickingTransition<>(ACTIVE, this::worldTickSlow, () -> ACTIVE, MAX_TICKRATE));
        colonyStateMachine.addTransition(new TickingTransition<>(UNLOADED, this::worldTickUnloaded, () -> UNLOADED, MAX_TICKRATE));
    }
    /**
     * 更新殖民地的状态。
     *
     * @return 新的殖民地状态。
     */
    private ColonyState updateState()
    {
        if (world == null)
        {
            return INACTIVE;
        }
        packageManager.updateAwayTime();

        if (!packageManager.getCloseSubscribers().isEmpty() || (loadedChunks.size() > 40 && !packageManager.getImportantColonyPlayers().isEmpty()))
        {
            isActive = true;
            return ACTIVE;
        }

        if (!packageManager.getImportantColonyPlayers().isEmpty() || forceLoadTimer > 0)
        {
            isActive = true;
            return UNLOADED;
        }

        return INACTIVE;
    }

    /**
     * 更新现有的订阅者。
     *
     * @return false
     */
    private boolean updateSubscribers()
    {
        packageManager.updateSubscribers();
        return false;
    }

    /**
     * 执行请求管理器的刻。
     *
     * @return false
     */
    private boolean tickRequests()
    {
        if (getRequestManager() != null)
        {
            getRequestManager().tick();
        }
        return false;
    }

    /**
     * 每500刻钟调用一次，用于较慢的更新。
     *
     * @return false
     */
    private boolean worldTickSlow()
    {
        buildingManager.cleanUpBuildings(this);
        citizenManager.onColonyTick(this);
        visitorManager.onColonyTick(this);
        updateAttackingPlayers();
        eventManager.onColonyTick(this);
        buildingManager.onColonyTick(this);
        graveManager.onColonyTick(this);
        workManager.onColonyTick(this);
        reproductionManager.onColonyTick(this);

        final long currTime = System.currentTimeMillis();
        if (lastOnlineTime != 0)
        {
            final long pastTime = currTime - lastOnlineTime;
            if (pastTime > ONE_HOUR_IN_MILLIS)
            {
                for (final IBuilding building : buildingManager.getBuildings().values())
                {
                    building.processOfflineTime(pastTime / 1000);
                }
            }
        }
        lastOnlineTime = currTime;

        updateChildTime();
        updateChunkLoadTimer();
        return false;
    }

    /**
     * 检查是否可以卸载殖民地。
     * 更新块卸载计时器，并在计时器为0时释放块。
     */
    private void updateChunkLoadTimer()
    {
        if (getConfig().getServer().forceLoadColony.get())
        {
            for (final ServerPlayer sub : getPackageManager().getCloseSubscribers())
            {
                if (getPermissions().hasPermission(sub, Action.CAN_KEEP_COLONY_ACTIVE_WHILE_AWAY))
                {
                    this.forceLoadTimer = CHUNK_UNLOAD_DELAY;
                    pendingChunks.addAll(pendingToUnloadChunks);
                    for (final long pending : pendingChunks)
                    {
                        checkChunkAndRegisterTicket(pending, world.getChunk(ChunkPos.getX(pending), ChunkPos.getZ(pending)));
                    }

                    pendingToUnloadChunks.clear();
                    pendingChunks.clear();
                    return;
                }
            }

            if (this.forceLoadTimer > 0)
            {
                this.forceLoadTimer -= MAX_TICKRATE;
                if (this.forceLoadTimer <= 0)
                {
                    for (final long chunkPos : this.ticketedChunks)
                    {
                        final int chunkX = ChunkPos.getX(chunkPos);
                        final int chunkZ = ChunkPos.getZ(chunkPos);
                        if (world instanceof ServerLevel)
                        {
                            final ChunkPos pos = new ChunkPos(chunkX, chunkZ);
                            ((ServerChunkCache) world.getChunkSource()).removeRegionTicket(KEEP_LOADED_TYPE, pos, 2, pos);
                            pendingToUnloadChunks.add(chunkPos);
                        }
                    }
                    ticketedChunks.clear();
                    ticketedChunksDirty = true;
                }
            }
        }
    }

    /**
     * 检查块并注册一个票据。
     *
     * @param chunkPos 要检查的块位置
     */
    private void checkChunkAndRegisterTicket(final long chunkPos, final LevelChunk chunk)
    {
        if (forceLoadTimer > 0 && world instanceof ServerLevel)
        {
            if (!ticketedChunks.contains(chunkPos) && buildingManager.isWithinBuildingZone(chunk))
            {
                ticketedChunks.add(chunkPos);
                ticketedChunksDirty = true;
                ((ServerChunkCache) world.getChunkSource()).addRegionTicket(KEEP_LOADED_TYPE, chunk.getPos(), 2, chunk.getPos());
            }
        }
    }

    /**
     * 每500刻钟调用一次，用于较慢的更新。仅在殖民地未加载时调用。
     *
     * @return false
     */
    private boolean worldTickUnloaded()
    {
        updateChildTime();
        updateChunkLoadTimer();
        return false;
    }

    /**
     * 为孩子的生长增加500个额外的刻钟。
     */
    private void updateChildTime()
    {
        if (hasChilds && additionalChildTime < maxAdditionalChildTime)
        {
            additionalChildTime += MAX_TICKRATE;
        }
        else
        {
            additionalChildTime = 0;
        }
    }

    /**
     * 更新白天和黑夜检测。
     *
     * @return false
     */
    private boolean checkDayTime()
    {
        if (isDay && !WorldUtil.isDayTime(world))
        {
            isDay = false;
            eventManager.onNightFall();
            raidManager.onNightFall();
            if (!packageManager.getCloseSubscribers().isEmpty())
            {
                citizenManager.checkCitizensForHappiness();
            }

            citizenManager.updateCitizenSleep(false);
        }
        else if (!isDay && WorldUtil.isDayTime(world))
        {
            isDay = true;
            citizenManager.onWakeUp();
        }
        return false;
    }

    /**
     * 更新正在攻击的玩家。
     */
    public void updateAttackingPlayers()
    {
        final List<Player> visitors = new ArrayList<>(visitingPlayers);

        // 清理访问玩家。
        for (final Player player : visitors)
        {
            if (!packageManager.getCloseSubscribers().contains(player))
            {
                visitingPlayers.remove(player);
                attackingPlayers.remove(new AttackingPlayer(player));
            }
        }

        for (final AttackingPlayer player : attackingPlayers)
        {
            if (!player.getGuards().isEmpty())
            {
                player.refreshList(this);
                if (player.getGuards().isEmpty())
                {
                    MessageUtils.format(COLONY_DEFENDED_SUCCESS_MESSAGE, player.getPlayer().getName()).sendTo(this).forManagers();
                }
            }
        }
    }

    @Override
    public PlayerTeam getTeam()
    {
        // 此getter将在不存在时创建团队。未来可以采取不同的方式。
        return checkOrCreateTeam();
    }

    /**
     * 检查或创建团队。
     */
    private PlayerTeam checkOrCreateTeam()
    {
        if (this.world.getScoreboard().getPlayerTeam(getTeamName()) == null)
        {
            this.world.getScoreboard().addPlayerTeam(getTeamName());
            this.world.getScoreboard().getPlayerTeam(getTeamName()).setAllowFriendlyFire(false);
        }
        return this.world.getScoreboard().getPlayerTeam(getTeamName());
    }

    /**
     * 为pvp处理设置殖民地颜色。
     *
     * @param colonyColor 殖民地颜色。
     */
    public void setColonyColor(final ChatFormatting colonyColor)
    {
        if (this.world != null)
        {
            checkOrCreateTeam();
            this.colonyTeamColor = colonyColor;
            this.world.getScoreboard().getPlayerTeam(getTeamName()).setColor(colonyColor);
            this.world.getScoreboard().getPlayerTeam(getTeamName()).setPlayerPrefix(Component.literal(colonyColor.toString()));
        }
        this.markDirty();
    }

    /**
     * 设置用于装饰等的殖民地旗帜图案。
     *
     * @param colonyFlag 包含图案-颜色对的列表
     */
    @Override
    public void setColonyFlag(ListTag colonyFlag)
    {
        this.colonyFlag = colonyFlag;
        markDirty();
    }

    /**
     * 从保存的数据中加载殖民地。
     *
     * @param compound 包含殖民地数据的NBT复合标签。
     * @param world    要加载的世界。
     * @return 加载的殖民地。
     */
    @Nullable
    public static Colony loadColony(@NotNull final CompoundTag compound, @Nullable final Level world)
    {
        try
        {
            final int id = compound.getInt(TAG_ID);
            @NotNull final Colony c = new Colony(id, world);
            c.name = compound.getString(TAG_NAME);
            c.center = BlockPosUtil.read(compound, TAG_CENTER);
            c.dimensionId = ResourceKey.create(Registry.DIMENSION_REGISTRY, new ResourceLocation(compound.getString(TAG_DIMENSION)));

            c.setRequestManager();
            c.read(compound);

            return c;
        }
        catch (final Exception e)
        {
            Log.getLogger().warn("加载殖民地时出现问题，请向管理员报告此问题", e);
        }
        return null;
    }

    /**
     * 在殖民地加载时设置请求管理器。
     */
    private void setRequestManager()
    {
        requestManager = new StandardRequestManager(this);
    }

    /**
     * 从保存的数据中读取殖民地。
     *
     * @param compound 要从中读取的复合标签。
     */
    /**
     * 从CompoundTag中读取数据。
     *
     * @param compound 包含数据的CompoundTag。
     */
    public void read(@NotNull final CompoundTag compound)
    {
        manualHiring = compound.getBoolean(TAG_MANUAL_HIRING);
        dimensionId = ResourceKey.create(Registry.DIMENSION_REGISTRY, new ResourceLocation(compound.getString(TAG_DIMENSION)));

        mercenaryLastUse = compound.getLong(TAG_MERCENARY_TIME);
        additionalChildTime = compound.getInt(TAG_CHILD_TIME);

        // 权限
        permissions.loadPermissions(compound);

        citizenManager.read(compound.getCompound(TAG_CITIZEN_MANAGER));
        visitorManager.read(compound);
        buildingManager.read(compound.getCompound(TAG_BUILDING_MANAGER));

        // 在市民和建筑加载后重新计算最大市民数量。
        citizenManager.calculateMaxCitizens();

        graveManager.read(compound.getCompound(TAG_GRAVE_MANAGER));

        if (compound.getAllKeys().contains(TAG_PROGRESS_MANAGER))
        {
            progressManager.read(compound);
        }

        eventManager.readFromNBT(compound);
        eventDescManager.deserializeNBT(compound.getCompound(NbtTagConstants.TAG_EVENT_DESC_MANAGER));

        if (compound.getAllKeys().contains(TAG_RESEARCH))
        {
            researchManager.readFromNBT(compound.getCompound(TAG_RESEARCH));
            // 现在已加载建筑、殖民者和研究，检查新的自动启动研究。
            // 这主要是为了向后兼容旧的存档，以便玩家不必手动启动更新前已解锁的新自动启动研究。
            researchManager.checkAutoStartResearch();
        }

        // 工作量
        workManager.read(compound.getCompound(TAG_WORK));

        wayPoints.clear();
        // 路点
        final ListTag wayPointTagList = compound.getList(TAG_WAYPOINT, Tag.TAG_COMPOUND);
        for (int i = 0; i < wayPointTagList.size(); ++i)
        {
            final CompoundTag blockAtPos = wayPointTagList.getCompound(i);
            final BlockPos pos = BlockPosUtil.read(blockAtPos, TAG_WAYPOINT);
            final BlockState state = NbtUtils.readBlockState(blockAtPos);
            wayPoints.put(pos, state);
        }

        // 空闲方块
        freeBlocks.clear();
        final ListTag freeBlockTagList = compound.getList(TAG_FREE_BLOCKS, Tag.TAG_STRING);
        for (int i = 0; i < freeBlockTagList.size(); ++i)
        {
            freeBlocks.add(ForgeRegistries.BLOCKS.getValue(new ResourceLocation(freeBlockTagList.getString(i))));
        }

        freePositions.clear();
        // 空闲位置
        final ListTag freePositionTagList = compound.getList(TAG_FREE_POSITIONS, Tag.TAG_COMPOUND);
        for (int i = 0; i < freePositionTagList.size(); ++i)
        {
            final CompoundTag blockTag = freePositionTagList.getCompound(i);
            final BlockPos block = BlockPosUtil.read(blockTag, TAG_FREE_POSITIONS);
            freePositions.add(block);
        }

        packageManager.setLastContactInHours(compound.getInt(TAG_ABANDONED));
        manualHousing = compound.getBoolean(TAG_MANUAL_HOUSING);

        if (compound.getAllKeys().contains(TAG_MOVE_IN))
        {
            moveIn = compound.getBoolean(TAG_MOVE_IN);
        }

        if (compound.getAllKeys().contains(TAG_STYLE))
        {
            this.style = compound.getString(TAG_STYLE);
        }

        raidManager.read(compound);

        if (compound.getAllKeys().contains(TAG_AUTO_DELETE))
        {
            this.canColonyBeAutoDeleted = compound.getBoolean(TAG_AUTO_DELETE);
        }
        else
        {
            this.canColonyBeAutoDeleted = true;
        }

        if (compound.getAllKeys().contains(TAG_TEAM_COLOR))
        {
            // 此读取可能在世界非空之前发生，因为Minecraft的能力操作顺序。
            // 因此，setColonyColor适当必须等到onWorldLoad触发。
            this.colonyTeamColor = ChatFormatting.values()[compound.getInt(TAG_TEAM_COLOR)];
        }

        if (compound.getAllKeys().contains(TAG_FLAG_PATTERNS))
        {
            this.setColonyFlag(compound.getList(TAG_FLAG_PATTERNS, Constants.TAG_COMPOUND));
        }

        this.requestManager.reset();
        if (compound.getAllKeys().contains(TAG_REQUESTMANAGER))
        {
            this.requestManager.deserializeNBT(compound.getCompound(TAG_REQUESTMANAGER));
        }
        this.lastOnlineTime = compound.getLong(TAG_LAST_ONLINE);
        if (compound.contains(TAG_COL_TEXT))
        {
            this.textureStyle = compound.getString(TAG_COL_TEXT);
        }
        this.colonyTag = compound;
    }

    /**
     * 获取分配给殖民地的事件处理程序。
     *
     * @return 殖民地权限事件处理程序。
     */
    public ColonyPermissionEventHandler getEventHandler()
    {
        return eventHandler;
    }

    /**
     * 将殖民地写入保存数据。
     *
     * @param compound 要写入的CompoundTag。
     * @return 写入后的CompoundTag。
     */
    public CompoundTag write(@NotNull final CompoundTag compound)
    {
        // 核心属性
        compound.putInt(TAG_ID, id);
        compound.putString(TAG_DIMENSION, dimensionId.location().toString());

        // 基本数据
        compound.putString(TAG_NAME, name);
        BlockPosUtil.write(compound, TAG_CENTER, center);

        compound.putBoolean(TAG_MANUAL_HIRING, manualHiring);
        compound.putLong(TAG_MERCENARY_TIME, mercenaryLastUse);

        compound.putInt(TAG_CHILD_TIME, additionalChildTime);

        // 权限
        permissions.savePermissions(compound);

        final CompoundTag buildingCompound = new CompoundTag();
        buildingManager.write(buildingCompound);
        compound.put(TAG_BUILDING_MANAGER, buildingCompound);

        final CompoundTag citizenCompound = new CompoundTag();
        citizenManager.write(citizenCompound);
        compound.put(TAG_CITIZEN_MANAGER, citizenCompound);

        visitorManager.write(compound);

        final CompoundTag graveCompound = new CompoundTag();
        graveManager.write(graveCompound);
        compound.put(TAG_GRAVE_MANAGER, graveCompound);

        // 工作量
        @NotNull final CompoundTag workManagerCompound = new CompoundTag();
        workManager.write(workManagerCompound);
        compound.put(TAG_WORK, workManagerCompound);

        progressManager.write(compound);
        eventManager.writeToNBT(compound);
        compound.put(NbtTagConstants.TAG_EVENT_DESC_MANAGER, eventDescManager.serializeNBT());
        raidManager.write(compound);

        @NotNull final CompoundTag researchManagerCompound = new CompoundTag();
        researchManager.writeToNBT(researchManagerCompound);
        compound.put(TAG_RESEARCH, researchManagerCompound);

        // 路点
        @NotNull final ListTag wayPointTagList = new ListTag();
        for (@NotNull final Map.Entry<BlockPos, BlockState> entry : wayPoints.entrySet())
        {
            @NotNull final CompoundTag wayPointCompound = new CompoundTag();
            BlockPosUtil.write(wayPointCompound, TAG_WAYPOINT, entry.getKey());
            wayPointCompound.put(TAG_BLOCK, NbtUtils.writeBlockState(entry.getValue()));
            wayPointTagList.add(wayPointCompound);
        }
        compound.put(TAG_WAYPOINT, wayPointTagList);

        // 空闲方块
        @NotNull final ListTag freeBlocksTagList = new ListTag();
        for (@NotNull final Block block : freeBlocks)
        {
            freeBlocksTagList.add(StringTag.valueOf(ForgeRegistries.BLOCKS.getKey(block).toString()));
        }
        compound.put(TAG_FREE_BLOCKS, freeBlocksTagList);

        // 空闲位置
        @NotNull final ListTag freePositionsTagList = new ListTag();
        for (@NotNull final BlockPos pos : freePositions)
        {
            @NotNull final CompoundTag wayPointCompound = new CompoundTag();
            BlockPosUtil.write(wayPointCompound, TAG_FREE_POSITIONS, pos);
            freePositionsTagList.add(wayPointCompound);
        }
        compound.put(TAG_FREE_POSITIONS, freePositionsTagList);

        compound.putInt(TAG_ABANDONED, packageManager.getLastContactInHours());
        compound.putBoolean(TAG_MANUAL_HOUSING, manualHousing);
        compound.putBoolean(TAG_MOVE_IN, moveIn);
        compound.put(TAG_REQUESTMANAGER, getRequestManager().serializeNBT());
        compound.putString(TAG_STYLE, style);
        compound.putBoolean(TAG_AUTO_DELETE, canColonyBeAutoDeleted);
        compound.putInt(TAG_TEAM_COLOR, colonyTeamColor.ordinal());
        compound.put(TAG_FLAG_PATTERNS, colonyFlag);
        compound.putLong(TAG_LAST_ONLINE, lastOnlineTime);
        compound.putString(TAG_COL_TEXT, textureStyle);
        this.colonyTag = compound;

        isActive = false;
        return compound;
    }

    /**
     * 获取维度ID。
     *
     * @return 维度ID。
     */
    public ResourceKey<Level> getDimension()
    {
        return dimensionId;
    }

    @Override
    public boolean isRemote()
    {
        return false;
    }

    @Override
    public IResearchManager getResearchManager()
    {
        return this.researchManager;
    }

    /**
     * 当殖民地的世界加载时，与之关联。
     *
     * @param w 世界对象。
     */
    @Override
    public void onWorldLoad(@NotNull final Level w)
    {
        if (w.dimension() == dimensionId)
        {
            this.world = w;
            // 注册新的事件处理程序
            if (eventHandler == null)
            {
                eventHandler = new ColonyPermissionEventHandler(this);
                MinecraftForge.EVENT_BUS.register(eventHandler);
            }
            setColonyColor(this.colonyTeamColor);
        }
    }

    /**
     * 如果世界卸载，则取消关联世界。
     *
     * @param w 世界对象。
     */
    @Override
    public void onWorldUnload(@NotNull final Level w)
    {
        if (w != world)
        {
            /*
             * 如果事件世界不是殖民地世界，则忽略。这可能发生在与其他模组的交互中。
             * 只要我们确保在那个时刻什么都不做，这对于MineColonies不应该是问题。
             */
            return;
        }

        if (eventHandler != null)
        {
            MinecraftForge.EVENT_BUS.unregister(eventHandler);
        }
        world = null;
    }

    @Override
    public void onServerTick(@NotNull final TickEvent.ServerTickEvent event)
    {
    }
    /**
     * 获取殖民地的工作管理器。
     *
     * @return 殖民地的工作管理器。
     */
    @Override
    @NotNull
    public IWorkManager getWorkManager()
    {
        return workManager;
    }

    /**
     * 获取自由交互位置列表的副本。
     *
     * @return 自由交互位置列表。
     */
    public Set<BlockPos> getFreePositions()
    {
        return new HashSet<>(freePositions);
    }

    /**
     * 获取自由交互方块列表的副本。
     *
     * @return 自由交互方块列表。
     */
    public Set<Block> getFreeBlocks()
    {
        return new HashSet<>(freeBlocks);
    }

    /**
     * 添加一个新的自由交互位置。
     *
     * @param pos 要添加的位置。
     */
    public void addFreePosition(@NotNull final BlockPos pos)
    {
        freePositions.add(pos);
        markDirty();
    }

    /**
     * 添加一个新的自由交互方块。
     *
     * @param block 要添加的方块。
     */
    public void addFreeBlock(@NotNull final Block block)
    {
        freeBlocks.add(block);
        markDirty();
    }

    /**
     * 移除一个自由交互位置。
     *
     * @param pos 要移除的位置。
     */
    public void removeFreePosition(@NotNull final BlockPos pos)
    {
        freePositions.remove(pos);
        markDirty();
    }

    /**
     * 移除一个自由交互方块。
     *
     * @param block 要移除的方块。
     */
    public void removeFreeBlock(@NotNull final Block block)
    {
        freeBlocks.remove(block);
        markDirty();
    }

    /**
     * 任何与世界每个tick相关的逻辑都应在此处执行。注意：如果殖民地的世界没有加载，它不会有世界tick。在应该_始终_运行的逻辑上使用onServerTick。
     *
     * @param event {@link TickEvent.WorldTickEvent}
     */
    @Override
    public void onWorldTick(@NotNull final TickEvent.LevelTickEvent event)
    {
        if (event.level != getWorld())
        {
            /*
             * 如果事件的世界不是殖民地世界，则忽略。这可能发生在与其他模组的交互中。只要我们小心在那个时刻什么都不做，这对MineColonies不会成为问题。
             */
            return;
        }

        colonyStateMachine.tick();
    }

    /**
     * 随机计算殖民地是否应更新市民。通常在CLEANUP_TICK_INCREMENT时更新。
     *
     * @param world        世界。
     * @param averageTicks 平均刻度以进行更新。
     * @return 随机布尔值。
     */
    public static boolean shallUpdate(final Level world, final int averageTicks)
    {
        return world.getGameTime() % (world.random.nextInt(averageTicks * 2) + 1) == 0;
    }

    /**
     * 在worldTicks之后更新路径点。
     *
     * @return false
     */
    private boolean updateWayPoints()
    {
        if (!wayPoints.isEmpty() && world != null)
        {
            final int randomPos = world.random.nextInt(wayPoints.size());
            int count = 0;
            for (final Map.Entry<BlockPos, BlockState> entry : wayPoints.entrySet())
            {
                if (count++ == randomPos)
                {
                    if (WorldUtil.isBlockLoaded(world, entry.getKey()))
                    {
                        final Block worldBlock = world.getBlockState(entry.getKey()).getBlock();
                        if (
                                ((worldBlock != (entry.getValue().getBlock()) && entry.getValue().getBlock() != ModBlocks.blockWayPoint) && worldBlock != ModBlocks.blockConstructionTape)
                                        || (world.isEmptyBlock(entry.getKey().below()) && !entry.getValue().getMaterial().isSolid()))
                        {
                            wayPoints.remove(entry.getKey());
                            markDirty();
                        }
                    }
                    return false;
                }
            }
        }

        return false;
    }

    /**
     * 返回殖民地的中心。
     *
     * @return 殖民地中心的块坐标。
     */
    @Override
    public BlockPos getCenter()
    {
        return center;
    }

    @Override
    public String getName()
    {
        return name;
    }

    /**
     * 设置殖民地的名称。标记为脏。
     *
     * @param n 新名称。
     */
    @Override
    public void setName(final String n)
    {
        name = n;
        markDirty();
    }

    @NotNull
    @Override
    public Permissions getPermissions()
    {
        return permissions;
    }

    @Override
    public boolean isCoordInColony(@NotNull final Level w, @NotNull final BlockPos pos)
    {
        if (w.dimension() != this.dimensionId)
        {
            return false;
        }

        final LevelChunk chunk = w.getChunkAt(pos);
        final IColonyTagCapability cap = chunk.getCapability(CLOSE_COLONY_CAP, null).resolve().orElse(null);
        return cap != null && cap.getOwningColony() == this.getID();
    }

    @Override
    public long getDistanceSquared(@NotNull final BlockPos pos)
    {
        return BlockPosUtil.getDistanceSquared2D(center, pos);
    }

    @Override
    public boolean hasTownHall()
    {
        return buildingManager.hasTownHall();
    }

    /**
     * 返回殖民地的ID。
     *
     * @return 殖民地ID。
     */
    @Override
    public int getID()
    {
        return id;
    }

    @Override
    public boolean hasWarehouse()
    {
        return buildingManager.hasWarehouse();
    }

    @Override
    public boolean hasBuilding(final String name, final int level, boolean singleBuilding)
    {
        int sum = 0;
        for (final IBuilding building : this.getBuildingManager().getBuildings().values())
        {
            if (building.getBuildingType().getRegistryName().getPath().equalsIgnoreCase(name))
            {
                if (singleBuilding)
                {
                    if (building.getBuildingLevel() >= level)
                    {
                        return true;
                    }
                }
                else
                {
                    sum += building.getBuildingLevel();
                    if (sum >= level)
                    {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public int getLastContactInHours()
    {
        return packageManager.getLastContactInHours();
    }

    /**
     * 返回殖民地所在的世界。
     *
     * @return 殖民地所在的世界。
     */
    @Nullable
    public Level getWorld()
    {
        return world;
    }

    @NotNull
    @Override
    public IRequestManager getRequestManager()
    {
        return requestManager;
    }

    /**
     * 标记实例为脏。
     */
    public void markDirty()
    {
        packageManager.setDirty();
        isActive = true;
    }

    @Override
    public boolean canBeAutoDeleted()
    {
        return canColonyBeAutoDeleted;
    }

    @Override
    public IRequester getRequesterBuildingForPosition(@NotNull final BlockPos pos)
    {
        return buildingManager.getBuilding(pos);
    }

    @Override
    @NotNull
    public List<Player> getMessagePlayerEntities()
    {
        List<Player> players = new ArrayList<>();

        for (ServerPlayer player : packageManager.getCloseSubscribers())
        {
            if (permissions.hasPermission(player, Action.RECEIVE_MESSAGES))
            {
                players.add(player);
            }
        }

        return players;
    }

    @Override
    @NotNull
    public List<Player> getImportantMessageEntityPlayers()
    {
        final Set<Player> playerList = new HashSet<>(getMessagePlayerEntities());

        for (final ServerPlayer player : packageManager.getImportantColonyPlayers())
        {
            if (permissions.hasPermission(player, Action.RECEIVE_MESSAGES_FAR_AWAY))
            {
                playerList.add(player);
            }
        }
        return new ArrayList<>(playerList);
    }

    /**
     * 获取是否需要手动分配工作。
     *
     * @return true或false。
     */
    public boolean isManualHiring()
    {
        return manualHiring;
    }

    /**
     * 设置工作分配手动或自动。
     *
     * @param manualHiring 如果手动分配工作则为true，否则为false。
     */
    public void setManualHiring(final boolean manualHiring)
    {
        this.manualHiring = manualHiring;
        progressManager.progressEmploymentModeChange();
        markDirty();
    }

    /**
     * 获取是否需要手动分配住房。
     *
     * @return true或false。
     */
    public boolean isManualHousing()
    {
        return manualHousing;
    }

    /**
     * 设置住房分配手动或自动。
     *
     * @param manualHousing 如果手动分配住房则为true，否则为false。
     */
    public void setManualHousing(final boolean manualHousing)
    {
        this.manualHousing = manualHousing;
        markDirty();
    }

    /**
     * 获取是否可以搬入住房。
     *
     * @return true或false。
     */
    public boolean canMoveIn()
    {
        return moveIn;
    }

    /**
     * 设置是否可以搬入市民。
     *
     * @param newMoveIn 如果可以搬入则为true，否则为false。
     */
    public void setMoveIn(final boolean newMoveIn)
    {
        this.moveIn = newMoveIn;
        markDirty();
    }

    /**
     * 向客户端发送已删除工作订单的消息。
     *
     * @param orderId 要删除的工作订单。
     */
    public void removeWorkOrderInView(final int orderId)
    {
        // 通知已删除的工作订单的订阅者
        for (final ServerPlayer player : packageManager.getCloseSubscribers())
        {
            Network.getNetwork().sendToPlayer(new ColonyViewRemoveWorkOrderMessage(this, orderId), player);
        }
    }

    /**
     * 向殖民地添加路径点。
     *
     * @param point 要添加的路径点。
     * @param block 路径点的方块。
     */
    public void addWayPoint(final BlockPos point, final BlockState block)
    {
        wayPoints.put(point, block);
        this.markDirty();
    }

    /**
     * 获取总体幸福度。
     *
     * @return 总体幸福度。
     */
    @Override
    public double getOverallHappiness()
    {
        if (citizenManager.getCitizens().size() <= 0)
        {
            return 5.5;
        }

        double happinessSum = 0;
        for (final ICitizenData citizen : citizenManager.getCitizens())
        {
            happinessSum += citizen.getCitizenHappinessHandler().getHappiness(citizen.getColony());
        }
        return happinessSum / citizenManager.getCitizens().size();
    }

    /**
     * 获取殖民地的所有路径点。
     *
     * @return 哈希映射的副本。
     */
    @Override
    public Map<BlockPos, BlockState> getWayPoints()
    {
        return new HashMap<>(wayPoints);
    }

    /**
     * 设置殖民地是否可以自动删除。
     *
     * @param canBeDeleted 如果殖民地可以自动删除则为true。
     */
    public void setCanBeAutoDeleted(final boolean canBeDeleted)
    {
        this.canColonyBeAutoDeleted = canBeDeleted;
        this.markDirty();
    }

    /**
     * 获取殖民地的默认风格。
     *
     * @return 默认风格字符串。
     */
    @Override
    public String getStyle()
    {
        return style;
    }

    /**
     * 设置殖民地的默认风格。
     *
     * @param style 默认字符串。
     */
    @Override
    public void setStyle(final String style)
    {
        this.style = style;
        this.markDirty();
    }

    /**
     * 获取殖民地的建筑管理器。
     *
     * @return 建筑管理器。
     */
    @Override
    public IRegisteredStructureManager getBuildingManager()
    {
        return buildingManager;
    }

    /**
     * 获取殖民地的坟墓管理器。
     *
     * @return 坟墓管理器。
     */
    @Override
    public IGraveManager getGraveManager()
    {
        return graveManager;
    }

    /**
     * 获取殖民地的市民管理器。
     *
     * @return 市民管理器。
     */
    @Override
    public ICitizenManager getCitizenManager()
    {
        return citizenManager;
    }

    /**
     * 获取殖民地的访客管理器。
     *
     * @return 访客管理器。
     */
    @Override
    public IVisitorManager getVisitorManager()
    {
        return visitorManager;
    }

    /**
     * 获取殖民地的入侵者管理器。
     *
     * @return 入侵者管理器。
     */
    @Override
    public IRaiderManager getRaiderManager()
    {
        return raidManager;
    }

    @Override
    public IEventManager getEventManager()
    {
        return eventManager;
    }

    @Override
    public IReproductionManager getReproductionManager()
    {
        return reproductionManager;
    }

    @Override
    public IEventDescriptionManager getEventDescriptionManager()
    {
        return eventDescManager;
    }

    /**
     * 获取殖民地的包裹管理器。
     *
     * @return 包裹管理器。
     */
    @Override
    public IColonyPackageManager getPackageManager()
    {
        return packageManager;
    }

    /**
     * 获取殖民地的进度管理器。
     *
     * @return 进度管理器。
     */
    @Override
    public IProgressManager getProgressManager()
    {
        return progressManager;
    }

    /**
     * 获取所有正在访问的玩家。
     *
     * @return 列表。
     */
    public ImmutableList<Player> getVisitingPlayers()
    {
        return ImmutableList.copyOf(visitingPlayers);
    }

    @Override
    public void addVisitingPlayer(final Player player)
    {
        final Rank rank = getPermissions().getRank(player);
        if (!rank.isColonyManager() && !visitingPlayers.contains(player) && MineColonies.getConfig().getServer().sendEnteringLeavingMessages.get())
        {
            visitingPlayers.add(player);
            if (!this.getImportantMessageEntityPlayers().contains(player))
            {
                MessageUtils.format(ENTERING_COLONY_MESSAGE, this.getName()).sendTo(player);
            }
            MessageUtils.format(ENTERING_COLONY_MESSAGE_NOTIFY, player.getName()).sendTo(this, true).forManagers();
        }
    }

    @Override
    public void removeVisitingPlayer(final Player player)
    {
        if (visitingPlayers.contains(player) && MineColonies.getConfig().getServer().sendEnteringLeavingMessages.get())
        {
            visitingPlayers.remove(player);
            if (!this.getImportantMessageEntityPlayers().contains(player))
            {
                MessageUtils.format(LEAVING_COLONY_MESSAGE, this.getName()).sendTo(player);
            }
            MessageUtils.format(LEAVING_COLONY_MESSAGE_NOTIFY, player.getName()).sendTo(this, true).forManagers();
        }
    }

    /**
     * 获取殖民地的NBT标签。
     *
     * @return 标签。
     */
    @Override
    public CompoundTag getColonyTag()
    {
        try
        {
            if (this.colonyTag == null || this.isActive)
            {
                this.write(new CompoundTag());
            }
        }
        catch (final Exception e)
        {
            Log.getLogger().warn("Something went wrong persisting colony: " + id, e);
        }
        return this.colonyTag;
    }

    /**
     * 检查玩家是否是试图入侵殖民地的波浪的一部分。
     *
     * @param player 要检查的玩家。
     * @return 如果是则为true。
     */
    public boolean isValidAttackingPlayer(final Player player)
    {
        if (packageManager.getLastContactInHours() > 1)
        {
            return false;
        }

        for (final AttackingPlayer attackingPlayer : attackingPlayers)
        {
            if (attackingPlayer.getPlayer().equals(player))
            {
                return attackingPlayer.isValidAttack(this);
            }
        }
        return false;
    }

    /**
     * 检查守卫的攻击是否有效。
     *
     * @param entity 守卫实体。
     * @return 如果有效则为true。
     */
    public boolean isValidAttackingGuard(final AbstractEntityCitizen entity)
    {
        if (packageManager.getLastContactInHours() > 1)
        {
            return false;
        }

        return AttackingPlayer.isValidAttack(entity, this);
    }

    /**
     * 向攻击守卫列表中添加守卫。
     *
     * @param IEntityCitizen 要添加的市民。
     */
    public void addGuardToAttackers(final AbstractEntityCitizen IEntityCitizen, final Player player)
    {
        if (player == null)
        {
            return;
        }

        for (final AttackingPlayer attackingPlayer : attackingPlayers)
        {
            if (attackingPlayer.getPlayer().equals(player))
            {
                if (attackingPlayer.addGuard(IEntityCitizen))
                {
                    MessageUtils.format(COLONY_ATTACK_GUARD_GROUP_SIZE_MESSAGE, attackingPlayer.getPlayer().getName(), attackingPlayer.getGuards().size())
                      .sendTo(this)
                      .forManagers();
                }
                return;
            }
        }

        for (final Player visitingPlayer : visitingPlayers)
        {
            if (visitingPlayer.equals(player))
            {
                final AttackingPlayer attackingPlayer = new AttackingPlayer(visitingPlayer);
                attackingPlayer.addGuard(IEntityCitizen);
                attackingPlayers.add(attackingPlayer);
                MessageUtils.format(COLONY_ATTACK_START_MESSAGE, visitingPlayer.getName()).sendTo(this).forManagers();
            }
        }
    }

    /**
     * 检查殖民地是否正在被另一个玩家攻击。
     *
     * @return 如果是则为true。
     */
    public boolean isColonyUnderAttack()
    {
        return !attackingPlayers.isEmpty();
    }

    /**
     * 获取殖民地团队颜色。
     *
     * @return ChatFormatting枚举颜色。
     */
    public ChatFormatting getTeamColonyColor()
    {
        return colonyTeamColor;
    }

    /**
     * 获取殖民地的旗帜模式。
     *
     * @return 模式-颜色对的列表
     */
    @Override
    public ListTag getColonyFlag() { return colonyFlag; }

    /**
     * 设置殖民地为活动状态。
     *
     * @param isActive 是否为活动状态。
     */
    public void setActive(final boolean isActive)
    {
        this.isActive = isActive;
    }

    /**
     * 保存使用佣兵的时间，以设置冷却时间。
     */
    @Override
    public void usedMercenaries()
    {
        mercenaryLastUse = world.getGameTime();
        markDirty();
    }

    /**
     * 获取上次使用佣兵的时间。
     */
    @Override
    public long getMercenaryUseTime()
    {
        return mercenaryLastUse;
    }

    @Override
    public boolean useAdditionalChildTime(final int amount)
    {
        if (additionalChildTime < amount)
        {
            return false;
        }
        else
        {
            additionalChildTime -= amount;
            return true;
        }
    }

    @Override
    public void updateHasChilds()
    {
        for (ICitizenData data : this.getCitizenManager().getCitizens())
        {
            if (data.isChild())
            {
                this.hasChilds = true;
                return;
            }
        }
        this.hasChilds = false;
    }

    @Override
    public void addLoadedChunk(final long chunkPos, final LevelChunk chunk)
    {
        if (world instanceof ServerLevel
              && getConfig().getServer().forceLoadColony.get())
        {
            if (this.forceLoadTimer > 0)
            {
                checkChunkAndRegisterTicket(chunkPos, chunk);
            }
            else
            {
                this.pendingChunks.add(chunkPos);
            }
        }
        this.loadedChunks.add(chunkPos);
    }

    @Override
    public void removeLoadedChunk(final long chunkPos)
    {
        loadedChunks.remove(chunkPos);
        pendingToUnloadChunks.remove(chunkPos);
    }

    @Override
    public int getLoadedChunkCount()
    {
        return loadedChunks.size();
    }

    @Override
    public ColonyState getState()
    {
        return colonyStateMachine.getState();
    }

    @Override
    public boolean isActive()
    {
        return colonyStateMachine.getState() != INACTIVE;
    }

    @Override
    public boolean isDay()
    {
        return isDay;
    }

    @Override
    public Set<Long> getTicketedChunks()
    {
        return ticketedChunks;
    }

    @Override
    public void setTextureStyle(final String style)
    {
        this.textureStyle = style;
        this.markDirty();
    }

    @Override
    public String getTextureStyleId()
    {
        return this.textureStyle;
    }

    /**
     * 检查是否需要更新视图的块票信息
     *
     * @return
     */
    public boolean isTicketedChunksDirty()
    {
        return ticketedChunksDirty;
    }
}
