package com.minecolonies.coremod.entity.ai.citizen.deliveryman;

import com.minecolonies.api.colony.buildings.IBuilding;
import com.minecolonies.api.colony.buildings.workerbuildings.IWareHouse;
import com.minecolonies.api.colony.interactionhandling.ChatPriority;
import com.minecolonies.api.colony.requestsystem.location.ILocation;
import com.minecolonies.api.colony.requestsystem.request.IRequest;
import com.minecolonies.api.colony.requestsystem.requestable.IRequestable;
import com.minecolonies.api.colony.requestsystem.requestable.deliveryman.Delivery;
import com.minecolonies.api.colony.requestsystem.requestable.deliveryman.IDeliverymanRequestable;
import com.minecolonies.api.crafting.ItemStorage;
import com.minecolonies.api.entity.ai.statemachine.AITarget;
import com.minecolonies.api.entity.ai.statemachine.states.IAIState;
import com.minecolonies.api.entity.citizen.VisibleCitizenStatus;
import com.minecolonies.api.tileentities.AbstractTileEntityColonyBuilding;
import com.minecolonies.api.tileentities.TileEntityColonyBuilding;
import com.minecolonies.api.tileentities.TileEntityRack;
import com.minecolonies.api.util.InventoryUtils;
import com.minecolonies.api.util.ItemStackUtils;
import com.minecolonies.api.util.Log;
import com.minecolonies.api.util.constant.Constants;
import com.minecolonies.coremod.colony.buildings.AbstractBuilding;
import com.minecolonies.coremod.colony.buildings.modules.WorkerBuildingModule;
import com.minecolonies.coremod.colony.buildings.workerbuildings.BuildingDeliveryman;
import com.minecolonies.coremod.colony.interactionhandling.PosBasedInteraction;
import com.minecolonies.coremod.colony.interactionhandling.StandardInteraction;
import com.minecolonies.coremod.colony.jobs.JobDeliveryman;
import com.minecolonies.coremod.colony.requestsystem.requests.StandardRequests.DeliveryRequest;
import com.minecolonies.coremod.colony.requestsystem.requests.StandardRequests.PickupRequest;
import com.minecolonies.coremod.entity.ai.basic.AbstractEntityAIInteract;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.InteractionHand;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraftforge.items.IItemHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.minecolonies.api.entity.ai.statemachine.states.AIWorkerState.*;
import static com.minecolonies.api.util.constant.Constants.TICKS_SECOND;
import static com.minecolonies.api.util.constant.TranslationConstants.*;
import static net.minecraftforge.items.CapabilityItemHandler.ITEM_HANDLER_CAPABILITY;

/**
 * 满足需求并交付物品。
 */
public class EntityAIWorkDeliveryman extends AbstractEntityAIInteract<JobDeliveryman, BuildingDeliveryman>
{
    /**
     * 工人在做出任何决策之前应该与仓库保持的最小距离。
     */
    private static final int MIN_DISTANCE_TO_WAREHOUSE = 5;

    /**
     * 等待工作人员决定要做什么的时间为5秒。
     */
    private static final int DECISION_DELAY = TICKS_SECOND * 5;

    /**
     * 等待工作人员决定如何处理，等待5秒钟。
     */
    private static final int PICKUP_DELAY = 20 * 5;

    /**
     * 手持的物品槽位。
     */
    private static final int SLOT_HAND = 0;

    /**
     * 以至少PRIORITY_FORCING_DUMP优先级完成请求将强制进行转储。
     */
    private static final int PRIORITY_FORCING_DUMP = 10;

    /**
     * 交付图标
     */
    private final static VisibleCitizenStatus DELIVERING           =
      new VisibleCitizenStatus(new ResourceLocation(Constants.MOD_ID, "textures/icons/work/delivery.png"), "com.minecolonies.gui.visiblestatus.delivery");

    /**
     * 渲染元背包。
     */
    public static final String RENDER_META_BACKPACK = "backpack";

    /**
     * 在采集步骤中从库存中剩余的堆叠数量。
     */
    private int currentSlot = 0;

    /**
     * 工人在当前采集过程中已经获得的物品堆叠数量。
     */
    private List<ItemStorage> alreadyKept = new ArrayList<>();

    /**
     * 初始化快递员并添加他的所有任务。
     *
     * @param deliveryman 他的工作。
     */
    public EntityAIWorkDeliveryman(@NotNull final JobDeliveryman deliveryman)
    {
        super(deliveryman);
        super.registerTargets(
          /*
           * Check if tasks should be executed.
           */
          new AITarget(IDLE, () -> START_WORKING, 1),
          new AITarget(START_WORKING, this::checkIfExecute, this::decide, DECISION_DELAY),
          new AITarget(PREPARE_DELIVERY, this::prepareDelivery, STANDARD_DELAY),
          new AITarget(DELIVERY, this::deliver, STANDARD_DELAY),
          new AITarget(PICKUP, this::pickup, PICKUP_DELAY),
          new AITarget(DUMPING, this::dump, TICKS_SECOND)

        );
        worker.setCanPickUpLoot(true);
    }

    @Override
    protected void updateRenderMetaData()
    {
        worker.setRenderMetadata(worker.getInventoryCitizen().isEmpty() ? "" : RENDER_META_BACKPACK);
    }

    @Override
    public Class<BuildingDeliveryman> getExpectedBuildingClass()
    {
        return BuildingDeliveryman.class;
    }

    /**
     * 从请求了取件的小屋中取物品。
     *
     * @return 下一个要前往的状态。
     */
    private IAIState pickup()
    {
        setDelay(WALK_DELAY);
        final IRequest<? extends IDeliverymanRequestable> currentTask = job.getCurrentTask();

        if (!(currentTask instanceof PickupRequest))
        {
            // The current task has changed since the Decision-state. Restart.
            return START_WORKING;
        }

        if (cannotHoldMoreItems())
        {
            this.alreadyKept = new ArrayList<>();
            this.currentSlot = 0;
            return DUMPING;
        }

        worker.getCitizenData().setVisibleStatus(DELIVERING);
        worker.getCitizenStatusHandler().setLatestStatus(Component.translatable("com.minecolonies.coremod.status.gathering"));

        final BlockPos pickupTarget = currentTask.getRequester().getLocation().getInDimensionLocation();
        if (pickupTarget != BlockPos.ZERO && !worker.isWorkerAtSiteWithMove(pickupTarget, MIN_DISTANCE_TO_WAREHOUSE))
        {
            return PICKUP;
        }

        final IBuilding pickupBuilding = building.getColony().getBuildingManager().getBuilding(pickupTarget);
        if (pickupBuilding == null)
        {
            job.finishRequest(false);
            return START_WORKING;
        }

        if (pickupFromBuilding(pickupBuilding))
        {
            this.alreadyKept = new ArrayList<>();
            this.currentSlot = 0;
            job.finishRequest(true);

            if (currentTask.getRequest().getPriority() >= PRIORITY_FORCING_DUMP)
            {
                return DUMPING;
            }
            else
            {
                return START_WORKING;
            }
        }
        else if (InventoryUtils.openSlotCount(worker.getInventoryCitizen()) <= 0)
        {
            this.alreadyKept = new ArrayList<>();
            this.currentSlot = 0;
            return DUMPING;
        }

        setDelay(5);
        currentSlot++;
        return PICKUP;
    }

    /**
     * 从建筑物中收集不需要的物品。
     *
     * @param building 要收集物品的建筑物。
     * @return 当完成时返回 true。
     */
    private boolean pickupFromBuilding(@NotNull final IBuilding building)
    {
        if (cannotHoldMoreItems() || InventoryUtils.openSlotCount(worker.getInventoryCitizen()) <= 0)
        {
            return false;
        }

        final IItemHandler handler = building.getCapability(ITEM_HANDLER_CAPABILITY, null).orElse(null);
        if (handler == null)
        {
            return false;
        }
        
        if (currentSlot >= handler.getSlots())
        {
            return true;
        }

        final ItemStack stack = handler.getStackInSlot(currentSlot);

        if (stack.isEmpty())
        {
            return false;
        }

        final int amount = workerRequiresItem(building, stack, alreadyKept);
        if (amount <= 0)
        {
            return false;
        }

        if (ItemStackUtils.isEmpty(handler.getStackInSlot(currentSlot)))
        {
            return false;
        }

        final ItemStack activeStack = handler.extractItem(currentSlot, amount, false);
        InventoryUtils.transferItemStackIntoNextBestSlotInItemHandler(activeStack, worker.getInventoryCitizen());
        building.markDirty();
        worker.decreaseSaturationForContinuousAction();

        // The worker gets a little bit of exp for every itemstack he grabs.
        worker.getCitizenExperienceHandler().addExperience(0.01D);
        worker.getCitizenItemHandler().setHeldItem(InteractionHand.MAIN_HAND, SLOT_HAND);
        return false;
    }

    /**
     * 检查工人是否能够容纳这么多物品。这取决于他的建筑级别。级别1：1堆叠 级别2：2堆叠、4堆叠、8堆叠、无限堆叠。这是2^建筑级别-1。
     *
     * @return 此送货员是否能够容纳更多物品
     */
    private boolean cannotHoldMoreItems()
    {
        if (building.getBuildingLevel() >= building.getMaxBuildingLevel())
        {
            return false;
        }
        return InventoryUtils.getAmountOfStacksInItemHandler(worker.getInventoryCitizen()) >= Math.pow(2, building.getBuildingLevel() - 1.0D) + 1;
    }

    /**
     * 检查特定建筑的工作者是否现在需要该物品。或当前任务的建造者是否需要该物品。
     *
     * @param building         要检查的建筑。
     * @param stack            用于堆叠的物品堆。
     * @param localAlreadyKept 已经保留的资源。
     * @return 可以被丢弃的数量。
     */
    public static int workerRequiresItem(final IBuilding building, final ItemStack stack, final List<ItemStorage> localAlreadyKept)
    {
        return building.buildingRequiresCertainAmountOfItem(stack, localAlreadyKept, false);
    }

    /**
     * 倾倒库存到仓库。
     *
     * @return 下一个要前往的状态。
     */
    private IAIState dump()
    {
        worker.getCitizenStatusHandler().setLatestStatus(Component.translatable("com.minecolonies.coremod.status.dumping"));

        if (!worker.isWorkerAtSiteWithMove(getAndCheckWareHouse().getPosition(), MIN_DISTANCE_TO_WAREHOUSE))
        {
            setDelay(WALK_DELAY);
            return DUMPING;
        }

        getAndCheckWareHouse().getTileEntity().dumpInventoryIntoWareHouse(worker.getInventoryCitizen());
        worker.getCitizenItemHandler().setHeldItem(InteractionHand.MAIN_HAND, SLOT_HAND);

        return START_WORKING;
    }

    /**
     * 获取送货员的殖民地仓库。
     *
     * @return 仓库。如果没有注册仓库，则返回null。
     */
    @Nullable
    private IWareHouse getAndCheckWareHouse()
    {
        return job.findWareHouse();
    }

    /**
     * 将物品交付给小屋。待办事项：当前的前提条件是：dman（交付人员）的库存可能仅包含所请求的物品堆叠。
     *
     * @return 下一个状态。
     */
    private IAIState deliver()
    {
        final IRequest<? extends IDeliverymanRequestable> currentTask = job.getCurrentTask();

        if (!(currentTask instanceof DeliveryRequest))
        {
            // The current task has changed since the Decision-state.
            // Since prepareDelivery() was called earlier, go dumping first and then restart.
            return DUMPING;
        }

        worker.getCitizenData().setVisibleStatus(DELIVERING);
        worker.getCitizenStatusHandler().setLatestStatus(Component.translatable("com.minecolonies.coremod.status.delivering"));

        final ILocation targetBuildingLocation = ((Delivery) currentTask.getRequest()).getTarget();
        if (!targetBuildingLocation.isReachableFromLocation(worker.getLocation()))
        {
            Log.getLogger().info(worker.getCitizenColonyHandler().getColony().getName() + ": " + worker.getName() + ": Can't inter dimension yet: ");
            return START_WORKING;
        }

        if (!worker.isWorkerAtSiteWithMove(targetBuildingLocation.getInDimensionLocation(), MIN_DISTANCE_TO_WAREHOUSE))
        {
            setDelay(WALK_DELAY);
            return DELIVERY;
        }

        final BlockEntity tileEntity = world.getBlockEntity(targetBuildingLocation.getInDimensionLocation());

        if (!(tileEntity instanceof TileEntityColonyBuilding))
        {
            // TODO: Non-Colony deliveries are unsupported yet. Fix that at some point in time.
            job.finishRequest(true);
            return START_WORKING;
        }

        final IBuilding targetBuilding = ((AbstractTileEntityColonyBuilding) tileEntity).getBuilding();

        boolean success = true;
        boolean extracted = false;
        final IItemHandler workerInventory = worker.getInventoryCitizen();
        final List<ItemStorage> itemsToDeliver = job.getTaskListWithSameDestination((IRequest<? extends Delivery>) currentTask).stream().map(r -> new ItemStorage(r.getRequest().getStack())).collect(Collectors.toList());

        for (int i = 0; i < workerInventory.getSlots(); i++)
        {
            if (workerInventory.getStackInSlot(i).isEmpty())
            {
                continue;
            }

            if (!itemsToDeliver.contains(new ItemStorage(workerInventory.getStackInSlot(i))))
            {
                continue;
            }

            final ItemStack stack = workerInventory.extractItem(i, Integer.MAX_VALUE, false);

            if (ItemStackUtils.isEmpty(stack))
            {
                continue;
            }

            extracted = true;
            final ItemStack insertionResultStack;

            // TODO: Please only push items into the target that were actually requested.
            if (targetBuilding instanceof AbstractBuilding)
            {
                insertionResultStack = InventoryUtils.forceItemStackToItemHandler(
                  targetBuilding.getCapability(ITEM_HANDLER_CAPABILITY, null).orElseGet(null), stack, ((IBuilding) targetBuilding)::isItemStackInRequest);
            }
            else
            {
                // Buildings that are not inherently part of the request system, but just receive a delivery, cannot have their items replaced.
                // Therefore, the keep-predicate always returns true.
                insertionResultStack =
                  InventoryUtils.forceItemStackToItemHandler(targetBuilding.getCapability(ITEM_HANDLER_CAPABILITY, null).orElseGet(null),
                    stack,
                    itemStack -> true);
            }

            if (!ItemStackUtils.isEmpty(insertionResultStack))
            {
                // A stack was replaced (meaning the inventory didn't have enough space).

                if (ItemStack.matches(insertionResultStack, stack) && worker.getCitizenData() != null)
                {
                    // The replaced stack is the same as the one we tried to put into the inventory.
                    // Meaning, replacing failed.
                    success = false;

                    if (targetBuilding.hasModule(WorkerBuildingModule.class))
                    {
                        worker.getCitizenData()
                          .triggerInteraction(new PosBasedInteraction(Component.translatable(COM_MINECOLONIES_COREMOD_JOB_DELIVERYMAN_NAMEDCHESTFULL,
                            targetBuilding.getFirstModuleOccurance(WorkerBuildingModule.class).getFirstCitizen().getName()),
                            ChatPriority.IMPORTANT,
                            Component.translatable(COM_MINECOLONIES_COREMOD_JOB_DELIVERYMAN_CHESTFULL),
                            targetBuilding.getID()));
                    }
                    else
                    {
                        worker.getCitizenData()
                          .triggerInteraction(new PosBasedInteraction(Component.translatable(COM_MINECOLONIES_COREMOD_JOB_DELIVERYMAN_CHESTFULL,
                            Component.literal(" :" + targetBuilding.getSchematicName())),
                            ChatPriority.IMPORTANT,
                            Component.translatable(COM_MINECOLONIES_COREMOD_JOB_DELIVERYMAN_CHESTFULL),
                            targetBuildingLocation.getInDimensionLocation()));
                    }
                }

                //Insert the result back into the inventory so we do not lose it.
                workerInventory.insertItem(i, insertionResultStack, false);
            }
        }

        if (!extracted)
        {
            // This can only happen if the dman's inventory was completely empty.
            // Let the retry-system handle this case.
            worker.decreaseSaturationForContinuousAction();
            worker.getCitizenItemHandler().setHeldItem(InteractionHand.MAIN_HAND, SLOT_HAND);
            job.finishRequest(false);

            // No need to go dumping in this case.
            return START_WORKING;
        }

        worker.getCitizenExperienceHandler().addExperience(1.5D);
        worker.decreaseSaturationForContinuousAction();
        worker.getCitizenItemHandler().setHeldItem(InteractionHand.MAIN_HAND, SLOT_HAND);
        job.finishRequest(true);

        return success ? START_WORKING : DUMPING;
    }

    /**
     * 为送货员准备交付。检查建筑物是否仍然需要物品，以及仓库中是否仍然有所需物品。
     *
     * @return 下一个要前往的状态。
     */
    private IAIState prepareDelivery()
    {
        final IRequest<? extends IRequestable> currentTask = job.getCurrentTask();
        if (!(currentTask instanceof DeliveryRequest))
        {
            // The current task has changed since the Decision-state.
            // Restart.
            return START_WORKING;
        }

        final List<IRequest<? extends Delivery>> taskList = job.getTaskListWithSameDestination((IRequest<? extends Delivery>) currentTask);
        final List<ItemStack> alreadyInInv = new ArrayList<>();
        IRequest<? extends Delivery> nextPickUp = null;

        int parallelDeliveryCount = 0;
        for (final IRequest<? extends Delivery> task : taskList)
        {
            parallelDeliveryCount++;
            int totalCount = InventoryUtils.getItemCountInItemHandler(worker.getInventoryCitizen(),
              itemStack -> ItemStackUtils.compareItemStacksIgnoreStackSize(task.getRequest().getStack(), itemStack));
            int hasCount = 0;
            for (final ItemStack stack : alreadyInInv)
            {
                if (ItemStackUtils.compareItemStacksIgnoreStackSize(stack, task.getRequest().getStack()))
                {
                    hasCount += stack.getCount();
                }
            }

            if (totalCount < hasCount + task.getRequest().getStack().getCount())
            {
                nextPickUp = task;
                break;
            }
            else
            {
                alreadyInInv.add(task.getRequest().getStack());
            }
        }

        if (nextPickUp == null || parallelDeliveryCount > 1 + (getSecondarySkillLevel() / 5))
        {
            return DELIVERY;
        }

        final ILocation location = nextPickUp.getRequest().getStart();

        if (!location.isReachableFromLocation(worker.getLocation()))
        {
            job.finishRequest(false);
            return START_WORKING;
        }

        if (walkToBlock(location.getInDimensionLocation()))
        {
            return PREPARE_DELIVERY;
        }

        if (getInventory().isFull())
        {
            return DUMPING;
        }

        final BlockEntity tileEntity = world.getBlockEntity(location.getInDimensionLocation());
        job.addConcurrentDelivery(nextPickUp.getId());
        if (gatherIfInTileEntity(tileEntity, nextPickUp.getRequest().getStack()))
        {
            return PREPARE_DELIVERY;
        }

        if (parallelDeliveryCount > 1)
        {
            job.removeConcurrentDelivery(nextPickUp.getId());
            return DELIVERY;
        }

        job.finishRequest(false);
        job.removeConcurrentDelivery(nextPickUp.getId());
        return START_WORKING;
    }

    /**
     * 查找第一个与{@code is}类型相同的@see ItemStack。它将从箱子中取出并放入工作人员的库存中。
     * 确保工作人员站在箱子旁边以避免破坏沉浸感。
     * 也确保有足够的库存空间来存放这个堆叠物品。
     *
     * @param entity 箱子、建筑物或架子的TileEntity。
     * @param is     物品栈。
     * @return 如果找到了该堆叠物品，则返回true。
     */
    public boolean gatherIfInTileEntity(final BlockEntity entity, final ItemStack is)
    {
        if (ItemStackUtils.isEmpty(is))
        {
            return false;
        }

        if ((entity instanceof TileEntityColonyBuilding && InventoryUtils.hasBuildingEnoughElseCount(((TileEntityColonyBuilding) entity).getBuilding(), new ItemStorage(is), is.getCount()) >= is.getCount()) ||
              (entity instanceof TileEntityRack && ((TileEntityRack) entity).getCount(new ItemStorage(is)) >= is.getCount()))
        {
            final IItemHandler handler = entity.getCapability(ITEM_HANDLER_CAPABILITY, null).resolve().orElse(null);
            if (handler != null)
            {
                return InventoryUtils.transferItemStackIntoNextFreeSlotFromItemHandler(handler,
                  stack -> !ItemStackUtils.isEmpty(stack) && ItemStackUtils.compareItemStacksIgnoreStackSize(is, stack, true, true),
                  is.getCount(),
                  worker.getInventoryCitizen());
            }
        }

        return false;
    }

    /**
     * 检查仓库以获取下一个任务。
     *
     * @return 下一个要前往的 AiState。
     */
    private IAIState decide() {
        // 设置市民数据可见状态为“工作中”。
        worker.getCitizenData().setVisibleStatus(VisibleCitizenStatus.WORKING);
        final IRequest<? extends IDeliverymanRequestable> currentTask = job.getCurrentTask();
        if (currentTask == null) {
            // 如果没有待处理的交付/取货请求，就在仓库附近闲逛。
            if (!worker.isWorkerAtSiteWithMove(getAndCheckWareHouse().getPosition(), MIN_DISTANCE_TO_WAREHOUSE)) {
                setDelay(WALK_DELAY);
                return START_WORKING;
            } else {
                if (!worker.getInventoryCitizen().isEmpty()) {
                    return DUMPING;
                } else {
                    return START_WORKING;
                }
            }
        }
        if (currentTask instanceof DeliveryRequest) {
            // 在进行交付之前，首先需要卸货物。
            if (!worker.getInventoryCitizen().isEmpty()) {
                return DUMPING;
            } else {
                return PREPARE_DELIVERY;
            }
        } else {
            return PICKUP;
        }
    }

    /**
     * 检查送货员代码是否应该执行。更具体地说，检查他是否有一个工作的仓库。
     *
     * @return 如果应继续按计划执行，则返回false。
     */
    private boolean checkIfExecute()
    {
        final IWareHouse wareHouse = getAndCheckWareHouse();
        if (wareHouse != null)
        {
            worker.getCitizenData().setWorking(true);
            if (wareHouse.getTileEntity() == null)
            {
                return false;
            }
            {
                return true;
            }
        }

        worker.getCitizenData().setWorking(false);
        if (worker.getCitizenData() != null)
        {
            worker.getCitizenData()
              .triggerInteraction(new StandardInteraction(Component.translatable(COM_MINECOLONIES_COREMOD_JOB_DELIVERYMAN_NOWAREHOUSE),
                ChatPriority.BLOCKING));
        }
        return false;
    }
}
