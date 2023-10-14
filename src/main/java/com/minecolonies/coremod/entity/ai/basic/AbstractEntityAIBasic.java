package com.minecolonies.coremod.entity.ai.basic;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.reflect.TypeToken;
import com.minecolonies.api.colony.ICitizenData;
import com.minecolonies.api.colony.buildings.IBuilding;
import com.minecolonies.api.colony.buildings.workerbuildings.IWareHouse;
import com.minecolonies.api.colony.interactionhandling.ChatPriority;
import com.minecolonies.api.colony.jobs.IJob;
import com.minecolonies.api.colony.requestsystem.location.ILocation;
import com.minecolonies.api.colony.requestsystem.request.IRequest;
import com.minecolonies.api.colony.requestsystem.request.RequestState;
import com.minecolonies.api.colony.requestsystem.requestable.IDeliverable;
import com.minecolonies.api.colony.requestsystem.requestable.RequestTag;
import com.minecolonies.api.colony.requestsystem.requestable.Stack;
import com.minecolonies.api.colony.requestsystem.requestable.Tool;
import com.minecolonies.api.colony.requestsystem.resolver.IRequestResolver;
import com.minecolonies.api.crafting.ItemStorage;
import com.minecolonies.api.entity.ai.pathfinding.IWalkToProxy;
import com.minecolonies.api.entity.ai.statemachine.AIEventTarget;
import com.minecolonies.api.entity.ai.statemachine.AITarget;
import com.minecolonies.api.entity.ai.statemachine.states.AIBlockingEventType;
import com.minecolonies.api.entity.ai.statemachine.states.AIWorkerState;
import com.minecolonies.api.entity.ai.statemachine.states.IAIState;
import com.minecolonies.api.inventory.InventoryCitizen;
import com.minecolonies.api.tileentities.TileEntityRack;
import com.minecolonies.api.util.*;
import com.minecolonies.api.util.constant.IToolType;
import com.minecolonies.api.util.constant.ToolType;
import com.minecolonies.api.util.constant.TypeConstants;
import com.minecolonies.api.util.constant.translation.RequestSystemTranslationConstants;
import com.minecolonies.coremod.colony.buildings.AbstractBuilding;
import com.minecolonies.coremod.colony.buildings.modules.WorkerBuildingModule;
import com.minecolonies.coremod.colony.interactionhandling.PosBasedInteraction;
import com.minecolonies.coremod.colony.interactionhandling.StandardInteraction;
import com.minecolonies.coremod.colony.jobs.AbstractJob;
import com.minecolonies.coremod.colony.jobs.JobDeliveryman;
import com.minecolonies.coremod.colony.requestsystem.resolvers.StationRequestResolver;
import com.minecolonies.coremod.entity.ai.citizen.deliveryman.EntityAIWorkDeliveryman;
import com.minecolonies.coremod.entity.pathfinding.EntityCitizenWalkToProxy;
import com.minecolonies.coremod.util.WorkerUtil;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.minecolonies.api.colony.requestsystem.requestable.deliveryman.AbstractDeliverymanRequestable.getMaxBuildingPriority;
import static com.minecolonies.api.colony.requestsystem.requestable.deliveryman.AbstractDeliverymanRequestable.scaledPriority;
import static com.minecolonies.api.entity.ai.statemachine.states.AIWorkerState.*;
import static com.minecolonies.api.util.constant.CitizenConstants.*;
import static com.minecolonies.api.util.constant.Constants.*;
import static com.minecolonies.api.util.constant.ToolLevelConstants.TOOL_LEVEL_WOOD_OR_GOLD;
import static com.minecolonies.api.util.constant.TranslationConstants.*;
import static com.minecolonies.coremod.entity.ai.basic.AbstractEntityAIInteract.RENDER_META_WORKING;
import static net.minecraftforge.items.CapabilityItemHandler.ITEM_HANDLER_CAPABILITY;

/**
 * 该类提供基本的AI功能。
 *
 * @param <J> 此AI必须执行的工作
 */
public abstract class AbstractEntityAIBasic<J extends AbstractJob<?, J>, B extends AbstractBuilding> extends AbstractAISkeleton<J>
{
    // /execute in minecraft:the_end run tp @a 1 150 10000
    /**
     * 每个终止动作后的标准延迟时间。
     */
    protected static final int STANDARD_DELAY = 5;

    /**
     * 每个终止动作后的标准延迟时间。
     */
    protected static final int REQUEST_DELAY = TICKS_20 * 3;

    /**
     * 可能的拾取尝试次数。
     */
    private static final int PICKUP_ATTEMPTS = 10;

    /**
     * AI当前正在工作或希望工作的方块。
     */
    @Nullable
    protected BlockPos currentWorkingLocation = null;

    /**
     * 下一个动作之前的滴答声数。
     */
    private int delay = 0;

    /**
     * 是否已等待一个滴答声。
     */
    private boolean hasDelayed = false;

    /**
     * 走路代理。
     */
    private IWalkToProxy proxy;

    /**
     * 这将逐渐禁用实体。
     */
    private int exceptionTimer = 1;

    /**
     * 他当前正在尝试卸下的槽位。
     */
    private int slotAt = 0;

    /**
     * 指示是否实际卸下了物品。
     */
    private boolean hasDumpedItems = false;

    /**
     * 步行延迟。
     */
    protected static final int WALK_DELAY = 20;

    /**
     * 他当前可能需要的东西。
     */
    protected Tuple<Predicate<ItemStack>, Integer> needsCurrently = null;

    /**
     * 工人应该走到的当前位置。
     */
    protected BlockPos walkTo = null;

    /**
     * 在卸下周期中已经保留的物品。
     */
    private final List<ItemStorage> alreadyKept = new ArrayList<>();

    /**
     * 计数拾取尝试次数。
     */
    private int pickUpCounter = 0;

    /**
     * 市民的建筑。
     */
    public final B building;

    /**
     * 最近的可用仓库
     */
    public final B warehouse;

    /**
     * 为每个AI设置一些重要的骨架内容。
     *
     * @param job 工作类
     */
    protected AbstractEntityAIBasic(@NotNull final J job)
    {
        super(job);
        if (!getExpectedBuildingClass().isInstance(worker.getCitizenData().getWorkBuilding()))
        {
            building = null;
            warehouse = null;
            worker.getCitizenData().setJob(null);
            Log.getLogger().error("市民：" + worker.getCitizenData().getId() + " 获得了与此建筑物不匹配的工作。中止", new Exception());
            return;
        }
        building = (B) worker.getCitizenData().getWorkBuilding();
        warehouse = (B) building.getColony().getBuildingManager().getClosestWarehouseInColony(building.getPosition());
        super.registerTargets(
          /*
           * 初始化安全检查和转换到IDLE
           */
          new AIEventTarget(AIBlockingEventType.AI_BLOCKING, this::initSafetyChecks, 1),
          new AITarget(INIT, this::getState, 1),
          /*
           * 更新胸带和名牌
           * 将在每次执行时执行
           * 并且不会停止执行
           */
          new AIEventTarget(AIBlockingEventType.AI_BLOCKING, () -> true, this::updateVisualState, 20),
          /*
           * 如果waitingForSomething返回true
           * 停止执行以等待它。
           * 这会保持当前状态
           * （返回null不会停止执行）
           */
          new AIEventTarget(AIBlockingEventType.AI_BLOCKING, this::waitingForSomething, this::getState, 1),

          /*
           * 只要需要，就倾倒库存。
           * 如果库存已经倾倒，则继续执行
           * 以解决状态。
           */
          new AIEventTarget(AIBlockingEventType.STATE_BLOCKING, this::inventoryNeedsDump, INVENTORY_FULL, 100),
          new AITarget(INVENTORY_FULL, this::dumpInventory, 10),
          new AITarget(ORGANIZE_INVENTORY, this::clearInventory,10),
          new AITarget(TRANSFER_TO_WAREHOUSE,this::unloadCargoAtWarehouse,10),
          /*
           * 检查是否需要任何物品。
           * 如果是，转换到NEEDS_ITEM。
           * 并等待新物品。
           */
          new AIEventTarget(AIBlockingEventType.AI_BLOCKING, () -> getState() != INVENTORY_FULL &&
                                                                     this.building.hasOpenSyncRequest(worker.getCitizenData()) || this.building
                                                                                                                                            .hasCitizenCompletedRequestsToPickup(
                                                                                                                                              worker.getCitizenData()),
            NEEDS_ITEM,
            20),

          new AIEventTarget(AIBlockingEventType.AI_BLOCKING, () -> building.hasCitizenCompletedRequests(worker.getCitizenData()) && this.cleanAsync(), NEEDS_ITEM, 200),

          new AITarget(NEEDS_ITEM, this::waitForRequests, 40),
          /*
           * 收集所需的物品。
           */
          new AITarget(GATHERING_REQUIRED_MATERIALS, this::getNeededItem, TICKS_SECOND),
          /*
           * 在此之前，放置与重新启动无关的任何非重新启动的AITargets
           * 重新启动AI、建筑等。
           */
          new AIEventTarget(AIBlockingEventType.STATE_BLOCKING, this::shouldRestart, this::restart, TICKS_SECOND),
          /*
           * 如果没有暂停，则重置。
           */
          new AITarget(PAUSED, () -> !this.isPaused(), () -> IDLE, TICKS_SECOND),
          /*
           * 如果工人暂停，不要工作
           */
          new AITarget(PAUSED, this::bePaused, 10),
          /*
           * 走到目标。
           */
          new AITarget(WALK_TO, this::walkToState, 10),
          /*
           * 以库存倾倒开始暂停
           */
          new AIEventTarget(AIBlockingEventType.AI_BLOCKING, this::isStartingPaused, INVENTORY_FULL, TICKS_SECOND)
        );
    }

    /**
     * 设置要走到的位置。
     * @param walkto 要走到的位置。
     */
    public void setWalkTo(final BlockPos walkto)
    {
        this.walkTo = walkto;
    }

    /**
     * 特殊的行走状态..
     * @return 一旦到达，就返回IDLE。
     */
    private IAIState walkToState()
    {
        if (walkToBlock(walkTo, DEFAULT_RANGE_FOR_DELAY))
        {
            return getState();
        }
        return IDLE;
    }
    /**
     * 从建筑物中获取材料。为此，如果尚未设置位置，则前往建筑物。然后检查具有所需材料的箱子并设置位置并返回。
     * <p>
     * 如果位置已经设置，则导航到该位置。到达后转移到库存并返回到开始工作状态。
     *
     * @return 转移到的下一个状态。
     */
    private IAIState getNeededItem()
    {
        worker.getCitizenStatusHandler().setLatestStatus(Component.translatable(COM_MINECOLONIES_COREMOD_STATUS_GATHERING));

        if (walkTo == null && walkToBuilding())
        {
            return getState();
        }

        if (needsCurrently == null)
        {
            return getStateAfterPickUp();
        }
        else
        {
            if (walkTo == null)
            {
                final BlockPos pos = building.getTileEntity().getPositionOfChestWithItemStack(needsCurrently.getA());
                if (pos == null)
                {
                    return getStateAfterPickUp();
                }
                walkTo = pos;
            }

            if (walkToBlock(walkTo) && pickUpCounter++ < PICKUP_ATTEMPTS)
            {
                return getState();
            }

            pickUpCounter = 0;

            if (!tryTransferFromPosToWorkerIfNeeded(walkTo, needsCurrently))
            {
                walkTo = null;
                return getState();
            }
        }

        walkTo = null;
        return getStateAfterPickUp();
    }

    /**
     * 拾取物品后要转换到的状态。
     *
     * @return 转移到的下一个状态。
     */
    public IAIState getStateAfterPickUp()
    {
        return START_WORKING;
    }

    /**
     * 可以被子类覆盖以返回工人期望的确切建筑类型。
     *
     * @return 与此AI的工作关联的建筑类型。
     */
    public abstract Class<B> getExpectedBuildingClass();

    /**
     * 可以被子类覆盖以返回确切的建筑类型。
     *
     * @param type 类型。
     * @return 与此AI的工人关联的建筑。
     */
    @NotNull
    @SuppressWarnings("unchecked")
    private B getOwnBuilding(@NotNull final Class<B> type)
    {
        if (type.isInstance(worker.getCitizenColonyHandler().getWorkBuilding()))
        {
            return (B) worker.getCitizenColonyHandler().getWorkBuilding();
        }
        throw new IllegalStateException("市民 " + worker.getCitizenData().getName() + " 意外失去了其建筑，类型不匹配");
    }

    @Override
    protected void onException(final RuntimeException e)
    {
        worker.getCitizenData().triggerInteraction(new StandardInteraction(Component.translatable(WORKER_AI_EXCEPTION), ChatPriority.BLOCKING));

        try
        {
            final int timeout = EXCEPTION_TIMEOUT * exceptionTimer;
            this.setDelay(FMLEnvironment.production ? timeout : EXCEPTION_TIMEOUT);
            // 现在等待更长时间
            exceptionTimer *= 2;
            if (worker != null)
            {
                final String name = this.worker.getName().getString();
                final BlockPos workerPosition = worker.blockPosition();
                final IJob<?> colonyJob = worker.getCitizenJobHandler().getColonyJob();
                final String jobName = colonyJob == null ? "null" : colonyJob.getJobRegistryEntry().getTranslationKey();
                Log.getLogger().error("暂停实体 " + name + "（" + jobName + "）在 " + workerPosition + "，因为出现错误 " + timeout + " 秒：");
            }
            else
            {
                Log.getLogger().error("暂停的实体为空，因为出现错误 " + timeout + " 秒：");
            }

            // 修复打印实际异常的问题
            e.printStackTrace();
        }
        catch (final RuntimeException exp)
        {
            Log.getLogger().error("嗯，报告崩溃了：");
            exp.printStackTrace();
            Log.getLogger().error("由AI异常引起：");
            e.printStackTrace();
        }
    }

    /**
     * 设置延迟的滴答声数。
     *
     * @param timeout 在此滴答声后等待的延迟时间。
     */
    public final void setDelay(final int timeout)
    {
        this.delay = timeout;
    }

    /**
     * 检查是否需要倾倒工人的库存。
     * <p>
     * 这还会询问实现AI是否需要基于自定义原因倾倒。{见 wantInventoryDumped}
     *
     * @return 如果需要倾倒库存，则为true。
     */
    protected boolean inventoryNeedsDump()
    {
        return getState() != INVENTORY_FULL && canBeInterrupted() &&
                 (worker.getCitizenInventoryHandler().isInventoryFull()
                    || job.getActionsDone() >= getActionsDoneUntilDumping()
                    || wantInventoryDumped())
                 && !(job instanceof JobDeliveryman);
    }

    /**
     * 计算AI在倾倒其库存之前应执行多少个操作。
     * <p>
     * 覆盖此项以更改值。
     *
     * @return 在物品倾倒之前执行的操作数。
     */
    protected int getActionsDoneUntilDumping()
    {
        return ACTIONS_UNTIL_DUMP;
    }

    /**
     * 必须被子类覆盖，以指定何时倾倒库存。始终在库存已满时倾倒。
     *
     * @return 如果现在需要倾倒库存，则为true
     */
    protected boolean wantInventoryDumped()
    {
        return false;
    }

    /**
     * 检查重要变量是否为null以防止崩溃。
     *
     * @return 如果一切准备就绪，则为IDLE，否则保持在INIT中
     */
    @Nullable
    private IAIState initSafetyChecks()
    {
        if (null == job || worker.getCitizenData() == null || building == null)
        {
            return INIT;
        }

        if (worker.getCitizenData().getJob() != job || building != worker.getCitizenData().getWorkBuilding())
        {
            worker.getCitizenData().setJob(null);
            return INIT;
        }

        if (getState() == INIT)
        {
            return IDLE;
        }

        return null;
    }

    /**
     * 更新工人的视觉状态。更新渲染元数据。更新名牌上的当前状态。
     *
     * @return null 以执行更多的目标。
     */
    private IAIState updateVisualState()
    {
        // 更新工人所处的当前状态。
        job.setNameTag(this.getState().toString());
        // 更新胸前的火把、种子等等。
        updateRenderMetaData();
        return null;
    }

    /**
     * 可以在实现中覆盖。
     * <p>
     * 在这里，AI可以检查是否需要重新渲染胸前的物品并进行渲染。
     */
    protected void updateRenderMetaData()
    {
        worker.setRenderMetadata(getState() == IDLE ? "" : RENDER_META_WORKING);
    }

    /**
     * 如果AI正在等待某些事情，则此方法将返回true。在这种情况下，不要执行更多的AI代码，直到它返回false。每滴答一次调用一次此方法以获得正确的延迟。工人在等待时会正确移动和动画。
     *
     * @return 如果我们必须等待某事，则返回true
     */
    private boolean waitingForSomething()
    {
        if (delay > 0)
        {
            if (delay % HIT_EVERY_X_TICKS == 0 && currentWorkingLocation != null && EntityUtils.isLivingAtSite(worker,
              currentWorkingLocation.getX(),
              currentWorkingLocation.getY(),
              currentWorkingLocation.getZ(),
              DEFAULT_RANGE_FOR_DELAY))
            {
                worker.getCitizenItemHandler().hitBlockWithToolInHand(currentWorkingLocation);
            }
            delay -= getTickRate();
            if (delay <= 0)
            {
                clearWorkTarget();
            }
            return true;
        }
        return false;
    }

    /**
     * 移除当前的工作块及其延迟。
     */
    private void clearWorkTarget()
    {
        this.currentWorkingLocation = null;
        this.delay = 0;
    }

    /**
     * 如果工人有打开的请求，它们的结果将被查询，直到它们全部完成为止，还会等待 DELAY_RECHECK。
     *
     * @return NEEDS_ITEM
     */
    @NotNull
    private IAIState waitForRequests()
    {
        delay = DELAY_RECHECK;
        updateWorkerStatusFromRequests();
        return lookForRequests();
    }

    private void updateWorkerStatusFromRequests()
    {
        if (!building.hasWorkerOpenRequests(worker.getCitizenData().getId()) && !building.hasCitizenCompletedRequests(worker.getCitizenData()))
        {
            worker.getCitizenStatusHandler().setLatestStatus();
            return;
        }

        Collection<IRequest<?>> requests = building.getCompletedRequests(worker.getCitizenData());
        if (requests.isEmpty())
        {
            requests = building.getOpenRequests(worker.getCitizenData().getId());
        }

        if (!requests.isEmpty())
        {
            worker.getCitizenStatusHandler()
              .setLatestStatus(Component.translatable("com.minecolonies.coremod.status.waiting"), requests.iterator().next().getShortDisplayString());
        }
    }

    /**
     * 查询当前需要的物品的实用方法。轮询此方法直到所有物品都到齐。
     *
     * @return 要进入的下一个状态。
     */
    @NotNull
    private IAIState lookForRequests()
    {
        if (!this.building.hasOpenSyncRequest(worker.getCitizenData())
              && !building.hasCitizenCompletedRequests(worker.getCitizenData()))
        {
            return afterRequestPickUp();
        }
        if (building.hasCitizenCompletedRequests(worker.getCitizenData()))
        {
            final Collection<IRequest<?>> completedRequests = building.getCompletedRequests(worker.getCitizenData());
            final List<IRequest<?>> deliverableRequests = new ArrayList<>();
            for (final IRequest<?> req : completedRequests)
            {
                if (!req.canBeDelivered())
                {
                    building.markRequestAsAccepted(worker.getCitizenData(), req.getId());
                }
                else
                {
                    deliverableRequests.add(req);
                }
            }

            if (!deliverableRequests.isEmpty())
            {
                final IRequest<?> firstDeliverableRequest = deliverableRequests.get(0);
                final IRequestResolver<?> resolver = building.getColony().getRequestManager().getResolverForRequest(firstDeliverableRequest.getId());
                final ILocation pickupLocation = resolver instanceof StationRequestResolver ? resolver.getLocation() : building.getLocation();

                if (walkToBlock(pickupLocation.getInDimensionLocation()) || !WorldUtil.isBlockLoaded(world, pickupLocation.getInDimensionLocation()))
                {
                    return NEEDS_ITEM;
                }
                final BlockEntity blockEntity = world.getBlockEntity(pickupLocation.getInDimensionLocation());
                if (blockEntity == null)
                {
                    return NEEDS_ITEM;
                }

                boolean async = false;
                if (worker.getCitizenData().isRequestAsync(firstDeliverableRequest.getId()))
                {
                    async = true;
                    job.getAsyncRequests().remove(firstDeliverableRequest.getId());
                }

                building.markRequestAsAccepted(worker.getCitizenData(), firstDeliverableRequest.getId());

                final List<IItemHandler> validHandlers = Lists.newArrayList();
                validHandlers.add(worker.getItemHandlerCitizen());
                validHandlers.addAll(InventoryUtils.getItemHandlersFromProvider(blockEntity));

                //Check if we either have the requested Items in our inventory or if they are in the building.
                if (InventoryUtils.areAllItemsInItemHandlerList(firstDeliverableRequest.getDeliveries(), validHandlers))
                {
                    final List<ItemStack> niceToHave = itemsNiceToHave();
                    final List<ItemStack> contained = InventoryUtils.getContainedFromItemHandler(firstDeliverableRequest.getDeliveries(), worker.getItemHandlerCitizen());

                    InventoryUtils.moveItemStacksWithPossibleSwap(
                      worker.getItemHandlerCitizen(),
                      InventoryUtils.getItemHandlersFromProvider(blockEntity),
                      firstDeliverableRequest.getDeliveries(),
                      itemStack ->
                        contained.stream().anyMatch(stack -> ItemStackUtils.compareItemStacksIgnoreStackSize(itemStack, stack)) ||
                          niceToHave.stream().anyMatch(stack -> ItemStackUtils.compareItemStacksIgnoreStackSize(itemStack, stack))
                    );
                    return NEEDS_ITEM;
                }
                else
                {
                    //Seems like somebody else picked up our stack.
                    //Lets try this again.
                    if (async)
                    {
                        worker.getCitizenData().createRequestAsync(firstDeliverableRequest.getRequest());
                    }
                    else
                    {
                        worker.getCitizenData().createRequest(firstDeliverableRequest.getRequest());
                    }
                }
            }
        }
        else
        {
            walkToBuilding();
        }

        return NEEDS_ITEM;
    }

    /**
     * 获取该工人的主要技能。
     * @return 主要技能。
     */
    protected int getPrimarySkillLevel()
    {
        return worker.getCitizenData().getCitizenSkillHandler().getLevel(getModuleForJob().getPrimarySkill());
    }

    /**
     * 获取该工人的次要技能。
     * @return 次要技能。
     */
    protected int getSecondarySkillLevel()
    {
        return worker.getCitizenData().getCitizenSkillHandler().getLevel(getModuleForJob().getSecondarySkill());
    }

    /**
     * 获取用于此AI的给定模块的实用方法。
     * @return 正确的模块。
     */
    protected WorkerBuildingModule getModuleForJob()
    {
        return building.getModuleMatching(WorkerBuildingModule.class, m -> m.getJobEntry() == job.getJobRegistryEntry());
    }

    /**
     * 对技能水平应用早期奖励曲线
     * 注意：这将调整范围从0-99到1-100
     * @param rawSkillLevel 要应用曲线的技能等级
     * @return 用于线性奖励函数的有效技能等级
     */
    protected int getEffectiveSkillLevel(int rawSkillLevel)
    {
        return (int)(((rawSkillLevel + 1) * 2) - Math.pow((rawSkillLevel + 1 ) / 10.0, 2));
    }

    /**
     * 清理异步请求
     *
     * @return false
     */
    private boolean cleanAsync()
    {
        final Collection<IRequest<?>> completedRequests = building.getCompletedRequests(worker.getCitizenData());

        for (IRequest<?> request : completedRequests)
        {
            if (worker.getCitizenData().isRequestAsync(request.getId()))
            {
                building.markRequestAsAccepted(worker.getCitizenData(), request.getId());
            }
        }

        return false;
    }

    /**
     * 检查是否允许在卸货后自动拾取。通常情况下，我们希望这样做，但如果工人当前正在制作/建筑，他将在它们的解析器中处理交付/卸货，因此我们可以在此期间禁用自动拾取。请注意，这只是效率问题。当dman在制作期间进行拾取时，这不会造成伤害，这只是一次浪费。因此，此标志仅用于卸货后的*自动*拾取。对于玩家触发的forcePickups和当库存已满时，它会*被忽略*。
     *
     * @return 如果当前允许卸货后的拾取，则返回true。
     */
    public boolean isAfterDumpPickupAllowed()
    {
        return true;
    }

    /**
     * 拾取请求后要执行的操作。
     *
     * @return 要进入的下一个状态。
     */
    public IAIState afterRequestPickUp()
    {
        return IDLE;
    }

    /**
     * 获取所需物品的总数。工人必须覆盖此方法，如果他们有更多信息。
     *
     * @param deliveredItemStack 所需的堆栈。
     * @return 默认情况下堆栈的大小。
     */
    public int getTotalRequiredAmount(final ItemStack deliveredItemStack)
    {
        return deliveredItemStack.getCount();
    }

    /**
     * 将工人步行到建筑物的箱子。如果此返回true，请立即返回。
     *
     * @return 如果工人在他的建筑物上，则返回false
     */
    protected final boolean walkToBuilding()
    {
        @Nullable final IBuilding ownBuilding = building;
        // 如果建筑物为空，则返回true以暂停工人
        return ownBuilding == null
                 || walkToBlock(ownBuilding.getPosition());
    }

    /**
     * 检查工人小屋中的所有架子以获取所需物品。
     * 如果找到，将其转移到工人库存中。
     *
     * @param is 所请求的物品类型（忽略数量）
     * @return 如果找到并转移了该类型的堆栈，则返回true。
     */
    public boolean checkAndTransferFromHut(@Nullable final ItemStack is)
    {
        for (final BlockPos pos : building.getContainers())
        {
            final BlockEntity entity = world.getBlockEntity(pos);
            if (entity instanceof TileEntityRack && ((TileEntityRack) entity).hasItemStack(is, 1, false))
            {
                entity.getCapability(ITEM_HANDLER_CAPABILITY, null)
                      .ifPresent((handler) ->  InventoryUtils.transferItemStackIntoNextBestSlotInItemHandler(handler, (stack) -> ItemStackUtils.compareItemStacksIgnoreStackSize(is, stack), getInventory()));
                return true;
            }
        }

        return false;
    }

    /**
     * 设置AI当前正在行走的方块。
     *
     * @param stand 要行走的位置
     * @return 在行走到方块时返回true
     */
    protected final boolean walkToBlock(@NotNull final BlockPos stand)
    {
        return walkToBlock(stand, DEFAULT_RANGE_FOR_DELAY);
    }

    /**
     * 设置AI当前正在行走的方块。
     *
     * @param stand 要行走的位置
     * @param range 我们需要多接近
     * @return 在行走到方块时返回true
     */
    protected final boolean walkToBlock(@NotNull final BlockPos stand, final int range)
    {
        if (proxy == null)
        {
            proxy = new EntityCitizenWalkToProxy(worker);
        }
        if (!proxy.walkToBlock(stand, range))
        {
            workOnBlock(null, DELAY_RECHECK);
            return true;
        }
        return false;
    }

    /**
     * 设置AI当前正在工作的方块。此方块将在延迟时接收动画击中。
     *
     * @param target  将被击中的方块
     * @param timeout 击打方块的时间（以滴答为单位）
     */
    private void workOnBlock(@Nullable final BlockPos target, final int timeout)
    {
        this.currentWorkingLocation = target;
    }

    /**
     * 查找@see ItemStack的工具栏架子。
     * 如果找到，将从中获取工具。请确保工人站在箱子旁边，以不破坏沉浸感。此外，确保为堆栈有库存空间。
     *
     * @param entity    箱子或建筑物的tileEntity。
     * @param toolType  工具的类型。
     * @param minLevel  最低工具等级。
     * @param maxLevel  最高工具等级。
     * @return 如果找到工具，则返回true。
     */
    public boolean retrieveToolInTileEntity(final BlockEntity entity, final IToolType toolType, final int minLevel, final int maxLevel)
    {
        if (ToolType.NONE.equals(toolType))
        {
            return false;
        }
        return InventoryFunctions.matchFirstInProviderWithAction(
          entity,
          stack -> ItemStackUtils.hasToolLevel(stack, toolType, minLevel, maxLevel),
          this::takeItemStackFromProvider
        );
    }

    /**
     * 从工人箱子的指定槽中取出物品并放入其库存中。如果库存已满，只会移动适合的部分。请注意，这个方法不应该是私有的，因为通常情况下，泛型访问在lambda中无法使用。
     *
     * @param provider  要取出物品的提供者。
     * @param slotIndex 要取出的槽位。
     */
    public void takeItemStackFromProvider(@NotNull final ICapabilityProvider provider, final int slotIndex)
    {
        InventoryUtils.transferItemStackIntoNextBestSlotFromProvider(provider, slotIndex, worker.getInventoryCitizen());
    }

    /**
     * 确保我们有一个合适的工具可用。将根据需要设置{@code needsTool}。
     *
     * @param toolType 要检查的工具类型。
     * @return 如果有工具则返回false
     */
    public boolean checkForToolOrWeapon(@NotNull final IToolType toolType)
    {
        final boolean needTool = checkForToolOrWeapon(toolType, TOOL_LEVEL_WOOD_OR_GOLD);
        worker.getCitizenData().setIdleAtJob(needTool);
        return needTool;
    }

    protected boolean checkForToolOrWeapon(@NotNull final IToolType toolType, final int minimalLevel)
    {
        final ImmutableList<IRequest<? extends Tool>> openToolRequests =
          building.getOpenRequestsOfTypeFiltered(
            worker.getCitizenData(),
            TypeToken.of(Tool.class),
            r -> r.getRequest().getToolClass().equals(toolType) && r.getRequest().getMinLevel() >= minimalLevel);
        final ImmutableList<IRequest<? extends Tool>> completedToolRequests =
          building.getCompletedRequestsOfTypeFiltered(
            worker.getCitizenData(),
            TypeToken.of(Tool.class),
            r -> r.getRequest().getToolClass().equals(toolType) && r.getRequest().getMinLevel() >= minimalLevel);

        if (checkForNeededTool(toolType, minimalLevel))
        {
            if (openToolRequests.isEmpty() && completedToolRequests.isEmpty())
            {
                final Tool request = new Tool(toolType, minimalLevel, building.getMaxToolLevel() < minimalLevel ? minimalLevel : building.getMaxToolLevel());
                worker.getCitizenData().createRequest(request);
            }
            delay = 0;
            return true;
        }

        return false;
    }

    /**
     * 确保我们有一个合适的工具可用。对工具进行ASync调用。
     *
     * @param toolType     要请求的工具类型
     * @param minimalLevel 工具的最低等级
     * @param maximalLevel 工具的最高等级
     */
    protected void checkForToolorWeaponASync(@NotNull final IToolType toolType, final int minimalLevel, final int maximalLevel)
    {
        final ImmutableList<IRequest<? extends Tool>> openToolRequests =
          building.getOpenRequestsOfTypeFiltered(
            worker.getCitizenData(),
            TypeToken.of(Tool.class),
            r -> r.getRequest().getToolClass().equals(toolType) && r.getRequest().getMinLevel() >= minimalLevel);
        final ImmutableList<IRequest<? extends Tool>> completedToolRequests =
          building.getCompletedRequestsOfTypeFiltered(
            worker.getCitizenData(),
            TypeToken.of(Tool.class),
            r -> r.getRequest().getToolClass().equals(toolType) && r.getRequest().getMinLevel() >= minimalLevel);

        if (openToolRequests.isEmpty() && completedToolRequests.isEmpty() && !hasOpenToolRequest(toolType))
        {
            final Tool request = new Tool(toolType, minimalLevel, maximalLevel);
            worker.getCitizenData().createRequestAsync(request);
        }
    }

    /**
     * 取消特定市民某种装甲类型的所有请求。
     *
     * @param armorType 装甲类型。
     */
    protected void cancelAsynchRequestForArmor(final IToolType armorType)
    {
        final List<IRequest<? extends Tool>> openRequests =
          building.getOpenRequestsOfTypeFiltered(worker.getCitizenData(), TypeConstants.TOOL, iRequest -> iRequest.getRequest().getToolClass() == armorType);
        for (final IRequest<?> token : openRequests)
        {
            worker.getCitizenColonyHandler().getColony().getRequestManager().updateRequestState(token.getId(), RequestState.CANCELLED);
        }
    }

    /**
     * 检查是否存在特定工具类型的未处理请求。
     *
     * @param key 工具类型。
     * @return 如果存在，则返回 true。
     */
    private boolean hasOpenToolRequest(final IToolType key)
    {
        return building.hasWorkerOpenRequestsFiltered(worker.getCitizenData().getId(),
          iRequest -> iRequest.getRequest() instanceof Tool && ((Tool) iRequest.getRequest()).getToolClass() == key);
    }

    /**
     * 检查是否需要一种工具。
     * <p>
     * 不要用于查找镐，因为它需要达到最低级别。
     *
     * @param toolType     方块所需的工具。
     * @param minimalLevel 最低级别要求。
     * @return 如果需要工具，则返回 true。
     */
    private boolean checkForNeededTool(@NotNull final IToolType toolType, final int minimalLevel)
    {
        final int maxToolLevel = worker.getCitizenColonyHandler().getWorkBuilding().getMaxToolLevel();
        final InventoryCitizen inventory = worker.getInventoryCitizen();
        if (InventoryUtils.isToolInItemHandler(inventory, toolType, minimalLevel, maxToolLevel))
        {
            return false;
        }

        delay += DELAY_RECHECK;
        return walkToBuilding() || !retrieveToolInHut(toolType, minimalLevel);
    }

    /**
     * 检查工人小屋中的所有储物箱，寻找所需的工具。
     *
     * @param toolType     请求的工具类型（数量不受关注）
     * @param minimalLevel 工具应具备的最低级别。
     * @return 如果找到该类型的一叠工具，则返回 true。
     */
    public boolean retrieveToolInHut(final IToolType toolType, final int minimalLevel)
    {
        if (building != null)
        {
            final Predicate<ItemStack> toolPredicate = stack -> ItemStackUtils.hasToolLevel(stack, toolType, minimalLevel, building.getMaxToolLevel());
            for (final BlockPos pos : building.getContainers())
            {
                final BlockEntity entity = world.getBlockEntity(pos);
                if (entity instanceof TileEntityRack)
                {
                    if (ToolType.NONE.equals(toolType))
                    {
                        return false;
                    }

                    if (((TileEntityRack) entity).hasItemStack(toolPredicate))
                    {
                        if (InventoryUtils.transferItemStackIntoNextBestSlotInItemHandler(entity.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null).orElseGet(null), toolPredicate, worker.getInventoryCitizen()))
                        {
                            return true;
                        }
                    }
                }
                else if (entity instanceof ChestBlockEntity)
                {
                    if (retrieveToolInTileEntity(building.getTileEntity(), toolType, minimalLevel, building.getMaxToolLevel()))
                    {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    /**
     * 获取用于存放库存的建筑物。
     * @return 默认情况下返回自身的建筑物。
     */
    protected IBuilding getBuildingToDump()
    {
        return building;
    }

    /**
     * 走向建筑物并卸载库存。如果库存已卸载，则继续执行以便可以解决状态。
     *
     * @return INVENTORY_FULL（库存已满） | IDLE（空闲）
     */
    @NotNull
    private IAIState dumpInventory()
    {
        final IBuilding building = getBuildingToDump();
        if (building == null)
        {
            // 哎呀，这不应该发生。重新启动 AI。
            return afterDump();
        }
        List<ICitizenData> deliveryman = building.getColony()
                .getCitizenManager()
                .getCitizens()
                .stream()
                .filter(citizen -> citizen.getJob() instanceof JobDeliveryman && citizen.isIdleAtJob())
                .collect(Collectors.toList());

        if (!worker.isWorkerAtSiteWithMove(building.getPosition(), DEFAULT_RANGE_FOR_DELAY))
        {
            setDelay(WALK_DELAY);
            return INVENTORY_FULL;
        }

        if (InventoryUtils.isProviderFull(building))
        {
            final ICitizenData citizenData = worker.getCitizenData();
            if (citizenData != null)
            {
                citizenData
                  .triggerInteraction(new StandardInteraction(Component.translatable(COM_MINECOLONIES_COREMOD_ENTITY_WORKER_INVENTORYFULLCHEST),
                    ChatPriority.IMPORTANT));
            }
            if(deliveryman.isEmpty())return ORGANIZE_INVENTORY;
            // 在这种情况下，在制作过程中捡起物品是可以的，因为清理满的库存非常重要。
            // 请注意，当另一个请求已经在进行中时，这不会创建一个捡起请求。
            if (building.getPickUpPriority() > 0)
            {
                building.createPickupRequest(getMaxBuildingPriority(true));
                hasDumpedItems = false;
            }
            alreadyKept.clear();
            slotAt = 0;
            this.clearActionsDone();
            return afterDump();
        }
        else if (dumpOneMoreSlot())
        {
            return INVENTORY_FULL;
        }

        alreadyKept.clear();
        slotAt = 0;
        this.clearActionsDone();

        if (isAfterDumpPickupAllowed() && building.getPickUpPriority() > 0 && hasDumpedItems)
        {
            // 工人目前未在制作物品，可以拾取。
            // 请注意，当另一个请求已经在进行中时，这不会创建拾取请求。
            building.createPickupRequest(scaledPriority(building.getPickUpPriority()));
            hasDumpedItems = false;
        }
        return afterDump();
    }

    /**
     * 倒掉后要进入的状态。
     *
     * @return 下一个状态。
     */
    public IAIState afterDump()
    {
        if (isPaused())
        {
            // 在进入暂停状态之前执行清理操作
            this.building.onCleanUp(worker.getCitizenData());
            return PAUSED;
        }
        return IDLE;
    }

    /**
     * 倾倒一个库存槽的物品到建筑箱子中。
     *
     * @return 如果需要继续倾倒更多则返回true。
     */
    @SuppressWarnings("PMD.PrematureDeclaration")
    private boolean dumpOneMoreSlot()
    {
        if (walkToBlock(getBuildingToDump().getPosition()))
        {
            return true;
        }

        @Nullable final IBuilding buildingWorker = building;

        ItemStack stackToDump = worker.getInventoryCitizen().getStackInSlot(slotAt);
        final int totalSize = worker.getInventoryCitizen().getSlots();

        while (stackToDump.isEmpty())
        {
            if (slotAt >= totalSize)
            {
                return false;
            }
            slotAt++;
            stackToDump = worker.getInventoryCitizen().getStackInSlot(slotAt);
        }

        boolean dumpAnyway = false;
        if (slotAt + MIN_OPEN_SLOTS * 2 >= totalSize)
        {
            final long openSlots = InventoryUtils.openSlotCount(worker.getInventoryCitizen());
            if (openSlots < MIN_OPEN_SLOTS * 2)
            {
                if (stackToDump.getCount() < CHANCE_TO_DUMP_50)
                {
                    dumpAnyway = worker.getRandom().nextBoolean();
                }
                else
                {
                    dumpAnyway = worker.getRandom().nextInt(stackToDump.getCount()) < CHANCE_TO_DUMP;
                }
            }
        }

        if (buildingWorker != null && !ItemStackUtils.isEmpty(stackToDump))
        {
            final int amount = dumpAnyway ? stackToDump.getCount() : buildingWorker.buildingRequiresCertainAmountOfItem(stackToDump, alreadyKept, true, job.getJobRegistryEntry());
            if (amount > 0)
            {
                final ItemStack activeStack = getInventory().extractItem(slotAt, amount, false);
                InventoryUtils.transferItemStackIntoNextBestSlotInItemHandler(activeStack, getBuildingToDump().getCapability(ITEM_HANDLER_CAPABILITY, null).orElseGet(null));
                hasDumpedItems = true;
            }
        }
        slotAt++;

        return slotAt < totalSize;
    }

    /**
     * 可以被实现重写，用于指定对工人有用的物品。当工人的库存已满时，他将尝试保留这些物品。 ItemStack 数量将被忽略，将取第一个找到的堆叠。
     *
     * @return 一个包含对工人有用的物品的列表
     */
    @NotNull
    protected List<ItemStack> itemsNiceToHave()
    {
        return new ArrayList<>();
    }

    /**
     * 清除已完成动作计数器。将其用于倾倒物品到箱子时调用。
     */
    private void clearActionsDone()
    {
        job.clearActionsDone();
    }

    /**
     * 计算市民的库存。
     *
     * @return 与此AI的市民匹配的InventoryCitizen。
     */
    @NotNull
    protected InventoryCitizen getInventory()
    {
        return worker.getInventoryCitizen();
    }

    /**
     * 检查并确保我们拥有最适合的工具来执行任务。
     * <p>
     * 如果我们没有适合的工具，将会发出请求并立即返回。
     *
     * @param target 要挖掘的 BlockState
     * @param pos    要挖掘的位置
     * @return 如果我们有适合的工具则返回 true
     */
    public final boolean holdEfficientTool(@NotNull final BlockState target, final BlockPos pos)
    {
        final int bestSlot = getMostEfficientTool(target, pos);
        if (bestSlot >= 0)
        {
            worker.getCitizenData().setIdleAtJob(false);
            worker.getCitizenItemHandler().setHeldItem(InteractionHand.MAIN_HAND, bestSlot);
            return true;
        }
        requestTool(target, pos);
        return false;
    }

    /**
     * 请求适用于此方块的合适工具。
     *
     * @param target 要采矿的方块状态
     * @param pos    要采矿的位置
     */
    private void requestTool(@NotNull final BlockState target, final BlockPos pos)
    {
        final IToolType toolType = WorkerUtil.getBestToolForBlock(target, target.getDestroySpeed(world, pos), building);
        final int required = WorkerUtil.getCorrectHarvestLevelForBlock(target);
        if (building.getMaxToolLevel() < required && worker.getCitizenData() != null)
        {
            worker.getCitizenData().triggerInteraction(new PosBasedInteraction(
              Component.translatable(RequestSystemTranslationConstants.REQUEST_SYSTEM_BUILDING_LEVEL_TOO_LOW, new ItemStack(target.getBlock()).getHoverName(), pos.getX(), pos.getY(), pos.getZ()),
              ChatPriority.IMPORTANT,
              Component.translatable(RequestSystemTranslationConstants.REQUEST_SYSTEM_BUILDING_LEVEL_TOO_LOW),
              pos));
        }
        updateToolFlag(toolType, required);
    }

    /**
     * 检查指定工具的指定级别是否可用。如果不可用，更新该工具的 needsTool 标志。
     *
     * @param toolType 所需工具
     * @param required 所需级别（仅适用于镐）
     */
    private void updateToolFlag(@NotNull final IToolType toolType, final int required)
    {
        if (ToolType.PICKAXE.equals(toolType))
        {
            checkForToolOrWeapon(toolType, required);
        }
        else
        {
            checkForToolOrWeapon(toolType);
        }
    }

    /**
     * 计算对该方块使用最有效的工具。
     *
     * @param target 要挖掘的方块状态
     * @param pos    方块所在的位置。
     * @return 拥有最佳工具的槽位
     */
    protected int getMostEfficientTool(@NotNull final BlockState target, final BlockPos pos)
    {
        final IToolType toolType = WorkerUtil.getBestToolForBlock(target, target.getDestroySpeed(world, pos), building);
        final int required = WorkerUtil.getCorrectHarvestLevelForBlock(target);

        if (toolType == ToolType.NONE)
        {
            final int heldSlot = worker.getInventoryCitizen().getHeldItemSlot(InteractionHand.MAIN_HAND);
            return heldSlot >= 0 ? heldSlot : 0;
        }

        int bestSlot = -1;
        int bestLevel = Integer.MAX_VALUE;
        @NotNull final InventoryCitizen inventory = worker.getInventoryCitizen();
        final int maxToolLevel = worker.getCitizenColonyHandler().getWorkBuilding().getMaxToolLevel();

        for (int i = 0; i < worker.getInventoryCitizen().getSlots(); i++)
        {
            final ItemStack item = inventory.getStackInSlot(i);
            final int level = ItemStackUtils.getMiningLevel(item, toolType);

            if (level > -1 && level >= required && level < bestLevel && ItemStackUtils.verifyToolLevel(item, level, required, maxToolLevel))
            {
                bestSlot = i;
                bestLevel = level;
            }
        }

        return bestSlot;
    }

    /**
     * 会延迟一次，然后第二次通过。用于方便而不是SetDelay。
     *
     * @param time 等待的时间
     * @return 如果需要等待则返回true
     */
    protected final boolean hasNotDelayed(final int time)
    {
        if (!hasDelayed)
        {
            setDelay(time);
            hasDelayed = true;
            return true;
        }
        hasDelayed = false;
        return false;
    }

    /**
     * 通知AI你执行了一项额外的动作。
     * <p>
     * 如果动作数量超过一定值，AI将卸下其物品。这也会触发AI感到饥饿。
     * <p>
     * 例如：
     * <p>
     * 在执行x个动作后，将所有物品带回。
     */
    public final void incrementActionsDoneAndDecSaturation()
    {
        worker.decreaseSaturationForAction();
        incrementActionsDone();
    }

    /**
     * 通知AI您已执行了一项额外操作。
     * <p>
     * 如果操作次数超过一定数量，AI将清空其物品库存。
     * <p>
     * 例如：
     * <p>
     * 在执行 x 次操作后，将所有物品带回来。
     *
     * @see #incrementActionsDone(int)
     */
    protected final void incrementActionsDone()
    {
        job.incrementActionsDone();
    }

    /**
     * 重置AI完成的动作。
     *
     * @see #incrementActionsDone(int)
     */
    protected final void resetActionsDone()
    {
        job.clearActionsDone();
    }

    /**
     * 通知AI你已经执行了numberOfActions更多的动作。
     * <p>
     * 如果动作超过一定数量，AI将会清空其库存。
     * <p>
     * 例如：
     * <p>
     * 在执行x个动作后，将所有物品带回来。
     *
     * @param numberOfActions 一次添加的动作数量。
     * @see #incrementActionsDone()
     */
    protected final void incrementActionsDone(final int numberOfActions)
    {
        job.incrementActionsDone(numberOfActions);
    }

    /**
     * 计算工作位置。
     * <p>
     * 获取工人想要工作的位置，并返回最适合的工作位置。
     * <p>
     *
     * @param targetPosition 想要工作的位置。
     * @return BlockPos 最适合工作的位置。
     */
    public BlockPos getWorkingPosition(final BlockPos targetPosition)
    {
        return targetPosition;
    }

    /**
     * 计算工作位置。
     * <p>
     * 从宽度和长度中取一个最小距离。
     * <p>
     * 然后在该距离处找到地板水平，然后检查是否包含两个空气层。
     *
     * @param distance  需要离建筑物远离的额外距离。
     * @param targetPos 需要进行工作的目标位置。
     * @param offset    附加偏移量
     * @return BlockPos 可以工作的位置。
     */
    public BlockPos getWorkingPosition(final int distance, final BlockPos targetPos, final int offset)
    {
        if (offset > MAX_ADDITIONAL_RANGE_TO_BUILD)
        {
            return targetPos;
        }

        @NotNull final Direction[] directions = {Direction.EAST, Direction.WEST, Direction.NORTH, Direction.SOUTH};

        //then get a solid place with two air spaces above it in any direction.
        for (final Direction direction : directions)
        {
            @NotNull final BlockPos positionInDirection = getPositionInDirection(direction, distance + offset, targetPos);
            if (EntityUtils.checkForFreeSpace(world, positionInDirection)
                  && world.getBlockState(positionInDirection.above()).is(BlockTags.SAPLINGS))
            {
                return positionInDirection;
            }
        }

        //if necessary we call it recursively and add some "offset" to the sides.
        return getWorkingPosition(distance, targetPos, offset + 1);
    }

    /**
     * 获取特定方向上的一个地板位置。
     *
     * @param facing    方向。
     * @param distance  距离。
     * @param targetPos 要操作的位置。
     * @return 一个 BlockPos 位置。
     */
    @NotNull
    private BlockPos getPositionInDirection(final Direction facing, final int distance, final BlockPos targetPos)
    {
        return BlockPosUtil.getFloor(targetPos.relative(facing, distance), world);
    }

    /**
     * 请求一个物品堆列表。
     *
     * @param stacks 物品堆。
     * @return 如果它们在库存中则返回true。
     */
    public boolean checkIfRequestForItemExistOrCreate(@NotNull final ItemStack... stacks)
    {
        return checkIfRequestForItemExistOrCreate(Lists.newArrayList(stacks));
    }

    /**
     * 检查库存中是否有任何一个物品堆叠。
     *
     * @param stacks 物品堆叠列表。
     * @return 如果有则返回 true。
     */
    public boolean checkIfRequestForItemExistOrCreate(@NotNull final Collection<ItemStack> stacks)
    {
        return stacks.stream().allMatch(this::checkIfRequestForItemExistOrCreate);
    }

    /**
     * 检查一个堆叠（stack）是否已经被请求或在库存中。如果不在库存中且尚未被请求，创建请求。
     *
     * @param stack 被请求的堆叠。
     * @return 如果在库存中，则返回 true，否则返回 false。
     */
    public boolean checkIfRequestForItemExistOrCreate(@NotNull final ItemStack stack)
    {
        return checkIfRequestForItemExistOrCreate(stack, stack.getCount(), stack.getCount());
    }

    /**
     * 检查是否已经请求了一个堆叠或者在库存中。如果不在库存中且尚未被请求，创建请求。
     *
     * @param stack 请求的堆叠。
     * @param count 请求的数量。
     * @param minCount 达到的最小数量。
     * @return 如果在库存中则返回true，否则返回false。
     */
    public boolean checkIfRequestForItemExistOrCreate(@NotNull final ItemStack stack, final int count, final int minCount)
    {
        if (InventoryUtils.hasItemInItemHandler(worker.getInventoryCitizen(),
          s -> ItemStackUtils.compareItemStacksIgnoreStackSize(s, stack)))
        {
            return true;
        }

        if (building.getOpenRequestsOfTypeFiltered(worker.getCitizenData(), TypeConstants.DELIVERABLE,
          (IRequest<? extends IDeliverable> r) -> r.getRequest().matches(stack)).isEmpty()
              && building.getCompletedRequestsOfTypeFiltered(worker.getCitizenData(), TypeConstants.DELIVERABLE,
          (IRequest<? extends IDeliverable> r) -> r.getRequest().matches(stack)).isEmpty())
        {
            final Stack stackRequest = new Stack(stack, count, minCount);
            worker.getCitizenData().createRequest(stackRequest);
        }

        return false;
    }

    /**
     * 请求一个物品堆列表。
     *
     * @param stacks 物品堆列表。
     * @return 如果它们在库存中，则返回true。
     */
    public boolean checkIfRequestForItemExistOrCreateAsync(@NotNull final ItemStack... stacks)
    {
        return checkIfRequestForItemExistOrCreateAsync(Lists.newArrayList(stacks));
    }

    /**
     * 检查库存中是否存在任何一个堆叠项。
     *
     * @param stacks 堆叠项列表。
     * @return 如果存在则返回 true。
     */
    public boolean checkIfRequestForItemExistOrCreateAsync(@NotNull final Collection<ItemStack> stacks)
    {
        return stacks.stream().allMatch(this::checkIfRequestForItemExistOrCreateAsync);
    }

    /**
     * 检查堆栈是否已经被请求或已在库存中。如果不在库存中且尚未请求，则创建请求。
     *
     * @param stack 请求的堆栈。
     * @return 如果在库存中则返回 true，否则返回 false。
     */
    public boolean checkIfRequestForItemExistOrCreateAsync(@NotNull final ItemStack stack)
    {
        return checkIfRequestForItemExistOrCreateAsync(stack, stack.getCount(), stack.getCount());
    }

    /**
     * 检查是否已经请求了一堆物品或者是否已经在库存中。如果不在库存中且未被请求过，创建请求。
     *
     * @param stack 请求的物品堆栈。
     * @param count 数量。
     * @param minCount 最小数量。
     * @return 如果在库存中则为true，否则为false。
     */
    public boolean checkIfRequestForItemExistOrCreateAsync(@NotNull final ItemStack stack, final int count, final int minCount)
    {
        return checkIfRequestForItemExistOrCreateAsync(stack, count, minCount, true);
    }

    /**
     * 检查堆叠是否已被请求或已存在于库存中。如果不在库存中且未被请求过，则创建请求。
     *
     * @param stack    请求的堆叠。
     * @param count    总计数。
     * @param minCount 最小计数。
     * @param matchNBT 是否需要匹配 NBT。
     * @return 如果在库存中则返回 true，否则返回 false。
     */
    public boolean checkIfRequestForItemExistOrCreateAsync(@NotNull final ItemStack stack, final int count, final int minCount, final boolean matchNBT)
    {
        if (stack.isEmpty())
        {
            return true;
        }

        final int invCount = InventoryUtils.getItemCountInItemHandler(worker.getInventoryCitizen(), s -> ItemStackUtils.compareItemStacksIgnoreStackSize(s, stack));
        if (invCount >= count)
        {
            return true;
        }
        final int updatedCount = count - invCount;
        final int updatedMinCount = Math.min(updatedCount, minCount);

        if (InventoryUtils.hasBuildingEnoughElseCount(building, new ItemStorage(stack, true, matchNBT), updatedMinCount) >= updatedMinCount &&
              InventoryUtils.transferXOfFirstSlotInProviderWithIntoNextFreeSlotInItemHandler(
                building, itemStack -> ItemStackUtils.compareItemStacksIgnoreStackSize(itemStack, stack, true, matchNBT),
                updatedCount,
                worker.getInventoryCitizen()))
        {
            return true;
        }

        if (building.getOpenRequestsOfTypeFiltered(worker.getCitizenData(), TypeConstants.DELIVERABLE,
          (IRequest<? extends IDeliverable> r) -> r.getRequest().matches(stack)).isEmpty()
            && building.getCompletedRequestsOfTypeFiltered(worker.getCitizenData(), TypeConstants.DELIVERABLE,
          (IRequest<? extends IDeliverable> r) -> r.getRequest().matches(stack)).isEmpty())
        {
            final Stack stackRequest = new Stack(stack, updatedCount, updatedMinCount, matchNBT);
            worker.getCitizenData().createRequestAsync(stackRequest);
        }

        return false;
    }

    /**
     * 检查是否存在某个可交付物的请求。
     *
     * @param deliverable 要检查请求的可交付物。
     * @return 如果可用或已转移，则为true。
     */
    public boolean checkIfRequestForItemExistOrCreate(@NotNull final IDeliverable deliverable)
    {
        final int invCount = InventoryUtils.getItemCountInItemHandler(worker.getInventoryCitizen(), deliverable::matches);
        if (invCount >= deliverable.getCount())
        {
            return true;
        }
        final int updatedCount = deliverable.getCount() - invCount;
        final int updatedMinCount = Math.min(updatedCount, deliverable.getMinimumCount());

        if (InventoryUtils.hasBuildingEnoughElseCount(building,
          deliverable::matches, updatedMinCount) >= updatedMinCount &&
              InventoryUtils.transferXOfFirstSlotInProviderWithIntoNextFreeSlotInItemHandler(
                building, deliverable::matches,
                updatedCount,
                worker.getInventoryCitizen()))
        {
            return true;
        }

        if (building.getOpenRequestsOfTypeFiltered(worker.getCitizenData(), TypeConstants.DELIVERABLE,
          (IRequest<? extends IDeliverable> r) -> r.getRequest().getClass().equals(deliverable.getClass())).isEmpty()
              && building.getCompletedRequestsOfTypeFiltered(worker.getCitizenData(), TypeConstants.DELIVERABLE,
          (IRequest<? extends IDeliverable> r) -> r.getRequest().getClass().equals(deliverable.getClass())).isEmpty())
        {
            worker.getCitizenData().createRequestAsync(deliverable);
        }

        return false;
    }

    /**
     * 检查标签是否已经被请求或者在库存中。如果不在库存中且未被请求，创建请求。
     *
     * @param tag 请求的标签。
     * @return 如果在库存中则返回 true，否则返回 false。
     */
    public boolean checkIfRequestForTagExistOrCreateAsync(@NotNull final TagKey<Item> tag, final int count)
    {
        if (InventoryUtils.hasItemInItemHandler(worker.getInventoryCitizen(), stack -> stack.is(tag) && stack.getCount() >= count))
        {
            return true;
        }

        if (InventoryUtils.hasBuildingEnoughElseCount(building,
          itemStack -> itemStack.is(tag), count) >= count &&
              InventoryUtils.transferXOfFirstSlotInProviderWithIntoNextFreeSlotInItemHandler(
                building, itemStack -> itemStack.is(tag),
                count,
                worker.getInventoryCitizen()))
        {
            return true;
        }

        if (building.getOpenRequestsOfTypeFiltered(worker.getCitizenData(), TypeConstants.TAG_REQUEST,
          (IRequest<? extends RequestTag> r) -> r.getRequest().getTag().equals(tag)).isEmpty()
            && building.getCompletedRequestsOfTypeFiltered(worker.getCitizenData(), TypeConstants.TAG_REQUEST,
          (IRequest<? extends RequestTag> r) -> r.getRequest().getTag().equals(tag)).isEmpty())
        {
            final IDeliverable tagRequest = new RequestTag(tag, count);
            worker.getCitizenData().createRequestAsync(tagRequest);
        }

        return false;
    }

    /**
     * 尝试从一个位置将符合条件的物品转移到厨师那里。
     *
     * @param pos       要转移的位置。
     * @param predicate 要评估的条件。
     * @return 如果取消状态为true。
     */

    private boolean tryTransferFromPosToWorkerIfNeeded(final BlockPos pos, @NotNull final Tuple<Predicate<ItemStack>, Integer> predicate)
    {
        final BlockEntity entity = world.getBlockEntity(pos);
        if (entity == null)
        {
            return true;
        }

        int existingAmount = InventoryUtils.getItemCountInItemHandler(worker.getInventoryCitizen(), predicate.getA());
        int amount;
        if (predicate.getB() > existingAmount)
        {
            amount = predicate.getB() - existingAmount;
        }
        else
        {
            return true; // has already needed transfers...
        }

        InventoryUtils.transferXOfFirstSlotInProviderWithIntoNextFreeSlotInItemHandlerWithResult(entity, predicate.getA(), amount, worker.getInventoryCitizen());
        existingAmount = InventoryUtils.getItemCountInItemHandler(worker.getInventoryCitizen(), predicate.getA());
        // has already needed transfers...
        return existingAmount >= predicate.getB();
    }

    /**
     * 工人是否暂停？
     *
     * @return 如果暂停则返回 true
     */
    private boolean isPaused()
    {
        return worker.getCitizenData().isPaused();
    }

    /**
     * 工作器是否在启动时处于暂停状态
     *
     * @return 如果在启动时处于暂停状态则返回true
     */
    private boolean isStartingPaused()
    {
        return isPaused() && getState() != PAUSED && getState() != INVENTORY_FULL;
    }

    /**
     * 暂停活动。
     *
     * @return 下一个状态
     */
    private IAIState bePaused()
    {
        if (!worker.getNavigation().isDone())
        {
            return null;
        }

        // Pick random activity.
        final int percent = worker.getRandom().nextInt(ONE_HUNDRED_PERCENT);
        if (percent < VISIT_BUILDING_CHANCE)
        {
            worker.getNavigation().tryMoveToBlockPos(building.getPosition(), worker.getRandom().nextBoolean() ? DEFAULT_SPEED * 1.5D : DEFAULT_SPEED * 2.2D);
        }
        else if (percent < WANDER_CHANCE)
        {
            worker.getNavigation().moveToRandomPos(10, DEFAULT_SPEED);
        }

        return null;
    }

    /**
     * 工人是否处于暂停状态但未行走。
     *
     * @return 如果已安排重新启动，则返回true。
     */
    private boolean shouldRestart()
    {
        return worker.getCitizenData().shouldRestart() && this.isPaused();
    }

    /**
     * 重新启动AI、建筑等。
     *
     * @return <code>State.INIT</code>
     */
    private IAIState restart()
    {
        this.building.onCleanUp(worker.getCitizenData());
        this.building.onRestart(worker.getCitizenData());
        setDelay(WALK_DELAY);
        worker.getCitizenData().restartDone();
        return INIT;
    }

    /**
     * 重新启动AI、建筑等。
     *
     * @return <code>State.INIT</code>
     */
    public int getExceptionTimer()
    {
        return exceptionTimer;
    }

    /**
     * 走向建筑物并整理库存，将所有多余的物品送至仓库
     *
     * @return TRANSFER_TO_WAREHOUSE(完成)|ORGANIZE_INVENTORY(未完成)
     */
    public IAIState clearInventory()
    {
        final IBuilding building = getBuildingToDump();
        if (building == null)
        {
            // 哎呀，这不应该发生。重新启动 AI。
            return afterDump();
        }
        if (!worker.isWorkerAtSiteWithMove(building.getPosition(), DEFAULT_RANGE_FOR_DELAY))    //去小屋整理
        {
            setDelay(WALK_DELAY);
            return ORGANIZE_INVENTORY;
        }
        if(!clearBackpack())return ORGANIZE_INVENTORY;
        if(pickupFromBuilding())return TRANSFER_TO_WAREHOUSE;
        else return ORGANIZE_INVENTORY;
    }


    int currentSlot = 0;
    /**
     * 从建筑物中收集不需要的物品。
     *
     * @return 当完成时返回 true。
     */
    private boolean pickupFromBuilding()
    {
        final IBuilding building = getBuildingToDump();
        if (InventoryUtils.openSlotCount(worker.getInventoryCitizen()) <= 0)
        {
            currentSlot = 0;
            return true;
        }   //背包已满,返回true

        final IItemHandler handler = building.getCapability(ITEM_HANDLER_CAPABILITY, null).orElse(null);
        if (handler == null)
        {
            return false;
        }   //容器不存在,返回false

        if (currentSlot >= handler.getSlots())
        {
            currentSlot = 0;
            return true;
        }   //指针超过最大容量,返回true

        final ItemStack stack = handler.getStackInSlot(currentSlot);

        if (stack.isEmpty())
        {
            return false;
        }   //指向物品格为空,返回false

        final int amount = building.buildingRequiresCertainAmountOfItem(stack, alreadyKept, false);
        if (amount <= 0)
        {
            return false;
        }   //指向物品需全部保留,返回false

        if (ItemStackUtils.isEmpty(handler.getStackInSlot(currentSlot)))
        {
            return false;
        }   //指向物品格为空,返回false 重复检查,意义不明
            //检查结束,取出物品放入背包,标记建筑需要更新,消耗饱食度
        final ItemStack activeStack = handler.extractItem(currentSlot, amount, false);
        InventoryUtils.transferItemStackIntoNextBestSlotInItemHandler(activeStack, worker.getInventoryCitizen());
        building.markDirty();
        worker.decreaseSaturationForContinuousAction();
        return false;
    }
    int currentSlot2 = 0;
    /**
     * 从身上卸下需要保管的物品。
     *
     * @return 当完成时返回 true。
     */
    private boolean clearBackpack() {
        final IBuilding building = getBuildingToDump();
        final IItemHandler handler = building.getCapability(ITEM_HANDLER_CAPABILITY, null).orElse(null);
        if (handler == null) {
            currentSlot2 = 0;
            return true;
        }   //容器不存在,返回true
        if (InventoryUtils.openSlotCount(handler) <= 0) {
            currentSlot2 = 0;
            return true;
        }   //建筑库存已满,返回true
        if (currentSlot2 >= worker.getInventoryCitizen().getSlots()) {
            currentSlot2 = 0;
            return true;
        }   //指针超过最大容量,返回true
        final ItemStack stack = worker.getInventoryCitizen().getStackInSlot(currentSlot2);
        if (stack.isEmpty()) {
            currentSlot2++;
            return false;
        }   //指向物品格为空,返回false
        List<ItemStack> reservedItems = itemsNiceToHave();
        if(reservedItems.isEmpty()){
            currentSlot2 = 0;
            return true;
        }   //需保管物品表为空，返回true
        ItemStack itemStack = worker.getInventoryCitizen().getStackInSlot(currentSlot2);
        for (ItemStack item : reservedItems) {
            if (item == itemStack) {//格子物品等于需保留物品则卸下并+1，否则直接+1
                //检查结束,取出物品放入仓库,标记建筑需要更新,消耗饱食度
                final ItemStack activeStack = worker.getInventoryCitizen().extractItem(currentSlot2, 64, false);
                InventoryUtils.transferItemStackIntoNextBestSlotInItemHandler(activeStack, handler);
                building.markDirty();
                worker.decreaseSaturationForContinuousAction();
                currentSlot2++;
                return false;
            }else{
                currentSlot2++;
                return false;
            }
        }
        return false;
    }

    /**
     * 前往仓库卸货
     *
     * @return IDLE(完成)|TRANSFER_TO_WAREHOUSE(未完成)
     */
    private IAIState unloadCargoAtWarehouse(){
        IBuilding building = getBuildingToDump();
        if (building == null)
        {
            // 哎呀，这不应该发生。重新启动 AI。
            return afterDump();
        }
        IWareHouse warehouse = building.getColony().getBuildingManager().getClosestWarehouseInColony(building.getPosition());
        if(warehouse == null){
            return IDLE;
        }
        worker.getCitizenStatusHandler().setLatestStatus(Component.translatable("com.minecolonies.coremod.status.dumping"));

        if (!worker.isWorkerAtSiteWithMove(warehouse.getPosition(), DEFAULT_RANGE_FOR_DELAY))
        {
            setDelay(WALK_DELAY);
            return TRANSFER_TO_WAREHOUSE;
        }

        warehouse.getTileEntity().dumpInventoryIntoWareHouse(worker.getInventoryCitizen());
        worker.getCitizenItemHandler().setHeldItem(InteractionHand.MAIN_HAND, 0);
        return IDLE;
    }
}
