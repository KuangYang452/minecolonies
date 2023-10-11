package com.minecolonies.coremod.colony.jobs;

import com.google.common.collect.ImmutableList;
import net.minecraft.resources.ResourceLocation;
import com.minecolonies.api.client.render.modeltype.ModModelTypes;
import com.minecolonies.api.colony.ICitizenData;
import com.minecolonies.api.colony.buildings.workerbuildings.IWareHouse;
import com.minecolonies.api.colony.requestsystem.StandardFactoryController;
import com.minecolonies.api.colony.requestsystem.data.IRequestSystemDeliveryManJobDataStore;
import com.minecolonies.api.colony.requestsystem.manager.IRequestManager;
import com.minecolonies.api.colony.requestsystem.request.IRequest;
import com.minecolonies.api.colony.requestsystem.request.RequestState;
import com.minecolonies.api.colony.requestsystem.requestable.deliveryman.AbstractDeliverymanRequestable;
import com.minecolonies.api.colony.requestsystem.requestable.deliveryman.Delivery;
import com.minecolonies.api.colony.requestsystem.requestable.deliveryman.IDeliverymanRequestable;
import com.minecolonies.api.colony.requestsystem.requestable.deliveryman.Pickup;
import com.minecolonies.api.colony.requestsystem.token.IToken;
import com.minecolonies.api.entity.citizen.AbstractEntityCitizen;
import com.minecolonies.api.util.BlockPosUtil;
import com.minecolonies.api.util.Log;
import com.minecolonies.api.util.Tuple;
import com.minecolonies.api.util.constant.NbtTagConstants;
import com.minecolonies.api.util.constant.TypeConstants;
import com.minecolonies.coremod.colony.buildings.modules.CourierAssignmentModule;
import com.minecolonies.coremod.colony.buildings.modules.WorkerBuildingModule;
import com.minecolonies.coremod.colony.requestsystem.requests.StandardRequests;
import com.minecolonies.coremod.entity.ai.citizen.deliveryman.EntityAIWorkDeliveryman;
import com.minecolonies.coremod.util.AttributeModifierUtils;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import static com.minecolonies.api.colony.requestsystem.requestable.deliveryman.AbstractDeliverymanRequestable.getPlayerActionPriority;
import static com.minecolonies.api.util.constant.BuildingConstants.TAG_ONGOING;
import static com.minecolonies.api.util.constant.CitizenConstants.SKILL_BONUS_ADD;
import static com.minecolonies.api.util.constant.Suppression.UNCHECKED;
import static com.minecolonies.api.util.constant.TranslationConstants.COM_MINECOLONIES_COREMOD_ENTITY_DELIVERYMAN_FORCEPICKUP;

/**
 * 送货员工作的类。
 */
public class JobDeliveryman extends AbstractJob<EntityAIWorkDeliveryman, JobDeliveryman>
{
    private IToken<?> rsDataStoreToken;

    /**
     * 每级的行走速度奖励
     */
    public static final double BONUS_SPEED_PER_LEVEL = 0.003;

    /**
     * 用于向后兼容的旧字段。
     */
    private int ongoingDeliveries;

    /**
     * 实例化送货员工作。
     *
     * @param entity 成为送货员的市民
     */
    public JobDeliveryman(final ICitizenData entity)
    {
        super(entity);
        if (entity != null)
        {
            setupRsDataStore();
        }
    }

    private void setupRsDataStore()
    {
        rsDataStoreToken = this.getCitizen()
                             .getColony()
                             .getRequestManager()
                             .getDataStoreManager()
                             .get(
                               StandardFactoryController.getInstance().getNewInstance(TypeConstants.ITOKEN),
                               TypeConstants.REQUEST_SYSTEM_DELIVERY_MAN_JOB_DATA_STORE
                             )
                             .getId();
    }

    @Override
    public void onLevelUp()
    {
        if (getCitizen().getEntity().isPresent())
        {
            final AbstractEntityCitizen worker = getCitizen().getEntity().get();
            final AttributeModifier speedModifier = new AttributeModifier(SKILL_BONUS_ADD, getCitizen().getCitizenSkillHandler().getLevel(getCitizen().getWorkBuilding().getModuleMatching(
              WorkerBuildingModule.class, m -> m.getJobEntry() == getJobRegistryEntry()).getPrimarySkill()) * BONUS_SPEED_PER_LEVEL, AttributeModifier.Operation.ADDITION);
            AttributeModifierUtils.addModifier(worker, speedModifier, Attributes.MOVEMENT_SPEED);
        }
    }

    @NotNull
    @Override
    public ResourceLocation getModel()
    {
        return ModModelTypes.COURIER_ID;
    }

    @Override
    public CompoundTag serializeNBT()
    {
        final CompoundTag compound = super.serializeNBT();
        compound.put(NbtTagConstants.TAG_RS_DMANJOB_DATASTORE, StandardFactoryController.getInstance().serialize(rsDataStoreToken));
        return compound;
    }

    @Override
    public void deserializeNBT(final CompoundTag compound)
    {
        super.deserializeNBT(compound);

        if (compound.getAllKeys().contains(NbtTagConstants.TAG_RS_DMANJOB_DATASTORE))
        {
            rsDataStoreToken = StandardFactoryController.getInstance().deserialize(compound.getCompound(NbtTagConstants.TAG_RS_DMANJOB_DATASTORE));
        }
        else
        {
            setupRsDataStore();
        }
        this.ongoingDeliveries = compound.getInt(TAG_ONGOING);
    }

    /**
     * 生成要注册的AI类。
     *
     * @return 您的个人AI实例。
     */
    @NotNull
    @Override
    public EntityAIWorkDeliveryman generateAI()
    {
        return new EntityAIWorkDeliveryman(this);
    }

    private IRequestSystemDeliveryManJobDataStore getDataStore()
    {
        return getCitizen().getColony().getRequestManager().getDataStoreManager().get(rsDataStoreToken, TypeConstants.REQUEST_SYSTEM_DELIVERY_MAN_JOB_DATA_STORE);
    }

    @Override
    public void serializeToView(final FriendlyByteBuf buffer)
    {
        super.serializeToView(buffer);
        StandardFactoryController.getInstance().serialize(buffer, rsDataStoreToken);
    }

    private LinkedList<IToken<?>> getTaskQueueFromDataStore()
    {
        return getDataStore().getQueue();
    }

    @Override
    public int getInactivityLimit()
    {
        return 60 * 10;
    }

    @Override
    public void triggerActivityChangeAction(final boolean newState)
    {
        try
        {
            if (newState)
            {
                getColony().getRequestManager().onColonyUpdate(request -> request.getRequest() instanceof Delivery || request.getRequest() instanceof Pickup);
            }
            else
            {
                cancelAssignedRequests();
            }
        }
        catch (final Exception ex)
        {
            Log.getLogger().warn("主动触发导致异常", ex);
        }
    }

    /**
     * 返回当前任务的{@link IRequest}。
     *
     * @return 当前任务的{@link IRequest}。
     */
    @SuppressWarnings(UNCHECKED)
    public IRequest<IDeliverymanRequestable> getCurrentTask()
    {
        final IToken<?> request = getTaskQueueFromDataStore().peekFirst();
        if (request == null)
        {
            return null;
        }

        return (IRequest<IDeliverymanRequestable>) getColony().getRequestManager().getRequestForToken(request);
    }

    /**
     * 用于向队列中添加请求的方法。
     *
     * @param token         要添加的请求的标记。
     * @param insertionIndex 插入的索引。
     */
    public void addRequest(@NotNull final IToken<?> token, final int insertionIndex)
    {
        final IRequestManager requestManager = getColony().getRequestManager();
        IRequest<? extends IDeliverymanRequestable> newRequest = (IRequest<? extends IDeliverymanRequestable>) (requestManager.getRequestForToken(token));

        LinkedList<IToken<?>> taskQueue = getTaskQueueFromDataStore();

        int offset = 0;
        for (int i = insertionIndex; i < taskQueue.size(); i++)
        {
            final IToken theToken = taskQueue.get(i);
            final IRequest<? extends IDeliverymanRequestable> request = (IRequest<? extends IDeliverymanRequestable>) (requestManager.getRequestForToken(theToken));
            if (request == null || request.getState() == RequestState.COMPLETED)
            {
                taskQueue.remove(theToken);
                i--;
                offset--;
            }
            else
            {
                request.getRequest().incrementPriorityDueToAging();
            }
        }

        getTaskQueueFromDataStore().add(Math.max(0, insertionIndex + offset), token);

        if (newRequest instanceof StandardRequests.PickupRequest && newRequest.getRequest().getPriority() == getPlayerActionPriority(true))
        {
            getCitizen().getEntity().ifPresent(e -> e.getCitizenChatHandler().sendLocalizedChat(COM_MINECOLONIES_COREMOD_ENTITY_DELIVERYMAN_FORCEPICKUP));
        }
    }

    /**
     * 用于标记当前请求已完成的方法。
     *
     * @param successful 处理是否成功。
     */
    public void finishRequest(final boolean successful)
    {
        if (getTaskQueueFromDataStore().isEmpty())
        {
            return;
        }

        final IToken<?> current = getTaskQueueFromDataStore().getFirst();

        final IRequest<?> request = getColony().getRequestManager().getRequestForToken(current);

        if (request == null)
        {
            if (!getTaskQueueFromDataStore().isEmpty() && current == getTaskQueueFromDataStore().getFirst())
            {
                getTaskQueueFromDataStore().removeFirst();
            }
            return;
        }
        else if (request.getRequest() instanceof Delivery)
        {
            final List<IRequest<? extends Delivery>> taskList = getTaskListWithSameDestination((IRequest<? extends Delivery>) request);
            if (ongoingDeliveries != 0)
            {
                for (int i = 0; i < Math.max(1, Math.min(ongoingDeliveries, taskList.size())); i++)
                {
                    final IRequest<? extends Delivery> req = taskList.get(i);
                    if (req.getState() == RequestState.IN_PROGRESS)
                    {
                        getColony().getRequestManager().updateRequestState(req.getId(), successful ? RequestState.RESOLVED : RequestState.FAILED);
                    }
                    getTaskQueueFromDataStore().remove(req.getId());
                }
            }
            else
            {
                for (final IToken<?> token : new ArrayList<>(getDataStore().getOngoingDeliveries()))
                {
                    final IRequest<?> req = getColony().getRequestManager().getRequestForToken(token);
                    if (req != null && req.getState() == RequestState.IN_PROGRESS)
                    {
                        getColony().getRequestManager().updateRequestState(req.getId(), successful ? RequestState.RESOLVED : RequestState.FAILED);
                    }
                    getTaskQueueFromDataStore().remove(token);
                    getDataStore().getOngoingDeliveries().remove(token);
                }
            }
        }
        else if (request.getRequest() instanceof Pickup)
        {
            getTaskQueueFromDataStore().remove(request.getId());
            getColony().getRequestManager().updateRequestState(current, successful ? RequestState.RESOLVED : RequestState.FAILED);
        }
        else
        {
            getColony().getRequestManager().updateRequestState(current, successful ? RequestState.RESOLVED : RequestState.FAILED);

            //Just to be sure lets delete them!
            if (!getTaskQueueFromDataStore().isEmpty() && current == getTaskQueueFromDataStore().getFirst())
            {
                getTaskQueueFromDataStore().removeFirst();
            }
        }

        getCitizen().getWorkBuilding().markDirty();
    }

    /**
     * 当要删除的任务被取消时调用的方法。
     *
     * @param token 要删除的任务的标记。
     */
    public void onTaskDeletion(@NotNull final IToken<?> token)
    {
        if (getTaskQueueFromDataStore().contains(token))
        {
            getTaskQueueFromDataStore().remove(token);
        }

        if (getCitizen().getWorkBuilding() != null)
        {
            getCitizen().getWorkBuilding().markDirty();
        }
    }

    /**
     * 获取此工作的任务队列的方法。
     *
     * @return 任务队列。
     */
    public List<IToken<?>> getTaskQueue()
    {
        return ImmutableList.copyOf(getTaskQueueFromDataStore());
    }

    private void cancelAssignedRequests()
    {
        for (final IToken<?> t : getTaskQueue())
        {
            final IRequest<?> r = getColony().getRequestManager().getRequestForToken(t);
            if (r != null)
            {
                getColony().getRequestManager().updateRequestState(t, RequestState.FAILED);
            }
            else
            {
                Log.getLogger().warn("糟糕，送货员无法取消ID为 " + t.toString() + " 的请求，因为它不存在");
            }
            getTaskQueueFromDataStore().remove(t);
        }
    }

    @Override
    public void onRemoval()
    {
        getCitizen().setWorking(false);
        try
        {
            cancelAssignedRequests();
        }
        catch (final Exception ex)
        {
            Log.getLogger().warn("主动触发导致异常", ex);
        }

        getColony().getRequestManager().getDataStoreManager().remove(this.rsDataStoreToken);
    }

    /**
     * 检查送货员是否有相同的目标请求。
     *
     * @param request 要比较的请求。
     * @return 如果是，则返回0；如果不是，则返回1。
     */
    public int hasSameDestinationDelivery(@NotNull final IRequest<? extends Delivery> request)
    {
        for (final IToken<?> requestToken : getTaskQueue())
        {
            final IRequest<?> compareRequest = getColony().getRequestManager().getRequestForToken(requestToken);
            if (compareRequest != null && compareRequest.getRequest() instanceof Delivery)
            {
                final Delivery current = (Delivery) compareRequest.getRequest();
                final Delivery newDev = request.getRequest();
                if (haveTasksSameSourceAndDest(current, newDev))
                {
                    return 0;
                }
            }
        }

        return 1;
    }

    /**
     * 检查两个交付是否具有相同的源和目标。
     *
     * @param requestA 第一个请求。
     * @param requestB 第二个请求。
     * @return 如果是，则返回true。
     */
    private boolean haveTasksSameSourceAndDest(@NotNull final Delivery requestA, @NotNull final Delivery requestB)
    {
        if (requestA.getTarget().equals(requestB.getTarget()))
        {
            if (requestA.getStart().equals(requestB.getStart()))
            {
                return true;
            }
            for (final IWareHouse wareHouse : getColony().getBuildingManager().getWareHouses())
            {
                if (wareHouse.hasContainerPosition(requestA.getStart().getInDimensionLocation()) && wareHouse.hasContainerPosition(requestB.getStart().getInDimensionLocation()))
                {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 构建具有相同源/目标对的所有请求的列表。
     *
     * @param request 第一个请求。
     * @return 列表。
     */
    public List<IRequest<? extends Delivery>> getTaskListWithSameDestination(final IRequest<? extends Delivery> request)
    {
        final List<IRequest<? extends Delivery>> deliveryList = new ArrayList<>();
        deliveryList.add(request);
        for (final IToken<?> requestToken : getTaskQueue())
        {
            if (!requestToken.equals(request.getId()))
            {
                final IRequest<?> compareRequest = getColony().getRequestManager().getRequestForToken(requestToken);
                if (compareRequest != null && compareRequest.getRequest() instanceof Delivery)
                {
                    final Delivery current = (Delivery) compareRequest.getRequest();
                    final Delivery newDev = request.getRequest();
                    if (haveTasksSameSourceAndDest(current, newDev))
                    {
                        deliveryList.add((IRequest<? extends Delivery>) compareRequest);
                    }
                }
            }
        }
        return deliveryList;
    }

    /**
     * 为交付计算分数和位置的方法，分数越大，请求的适合度越差。
     *
     * @param newRequest 要检查的请求
     * @return 分数和放置位置的元组。
     */
    @NotNull
    public Tuple<Double, Integer> getScoreForDelivery(final IRequest<?> newRequest)
    {
        final List<IToken<?>> requestTokens = getTaskQueueFromDataStore();

        double totalScore = 10000;
        int bestRequestIndex = Math.max(0, requestTokens.size());

        if (requestTokens.isEmpty())
        {
            // 没有任务，与送货员位置比较
            totalScore = getClosenessFactorTo(getSource(newRequest),
              getTarget(newRequest),
              getCitizen().getLastPosition(),
              getTarget(newRequest));

            totalScore -= ((AbstractDeliverymanRequestable) newRequest.getRequest()).getPriority();
        }

        for (int i = 0; i < requestTokens.size(); i++)
        {
            final IRequest<?> compareRequest = getColony().getRequestManager().getRequestForToken(requestTokens.get(i));
            if (compareRequest == null)
            {
                continue;
            }

            if (compareRequest.getRequest() instanceof AbstractDeliverymanRequestable)
            {
                double score = getScoreOfRequestComparedTo(newRequest, compareRequest, i);

                if (score <= totalScore)
                {
                    bestRequestIndex = i + getPickupOrRequestOffset(newRequest, compareRequest);
                    totalScore = score;
                }
            }
        }

        totalScore += bestRequestIndex;

        return new Tuple<>(totalScore, bestRequestIndex);
    }

    /**
     * 计算两个请求之间的比较分数，使它们在许多方面可以比较。
     *
     * @param source         第一个请求的源
     * @param comparing      比较的请求
     * @param comparingIndex 比较请求在任务队列中的索引。将请求不在队列中时使用任务队列大小。
     * @return 两个请求的比较分数，越低越好。
     */
    public double getScoreOfRequestComparedTo(final IRequest<?> source, final IRequest<?> comparing, final int comparingIndex)
    {
        if (!(comparing != null && comparing.getRequest() instanceof AbstractDeliverymanRequestable && source != null
                && source.getRequest() instanceof AbstractDeliverymanRequestable))
        {
            return 100;
        }

        // 与现有请求的近度
        double score = getClosenessFactorTo(getSource(source), getTarget(source), getSource(comparing), getTarget(comparing));
        // 优先级的差异，与新进来的优先级相比较
        score += (((AbstractDeliverymanRequestable) comparing.getRequest()).getPriority() - ((AbstractDeliverymanRequestable) source.getRequest()).getPriority()) * 0.5;

        // 交替提取和交付的附加分数
        score += getPickUpRequestScore(source, comparing);

        // 越多的请求，分数越差
        score += getTaskQueue().size() - comparingIndex;

        return score;
    }

    /**
     * 获取拾取请求的正确任务插入顺序，如果添加新请求以匹配现有请求，并且现有请求是拾取请求，而新请求是交付请求，则它应该在前面。
     *
     * @param newRequest 新请求添加
     * @param existing   现有请求
     * @return 在现有请求之后插入的1，前面插入的0
     */
    private static int getPickupOrRequestOffset(final IRequest<?> newRequest, final IRequest<?> existing)
    {
        if (newRequest.getRequest() instanceof Delivery && existing.getRequest() instanceof Pickup)
        {
            return 0;
        }

        return 1;
    }

    /**
     * 为交付和拾取交替的漂亮分数
     *
     * @param newRequest 新请求
     * @param existing   现有请求
     * @return 交替交付和拾取更好的分数
     */
    private static int getPickUpRequestScore(final IRequest<?> newRequest, final IRequest<?> existing)
    {
        if (newRequest.getRequest() instanceof Pickup && existing.getRequest() instanceof Delivery
                || newRequest.getRequest() instanceof Delivery && existing.getRequest() instanceof Pickup)
        {
            return 0;
        }

        return 3;
    }

    /**
     * 计算两个不同交付向量的近度因子
     *
     * @param source1  第一个请求的源
     * @param target1  第一个请求的目标
     * @param source2  第二个请求的源
     * @param target2  第二个请求的目标
     * @return 两个向量之间的近度因子，1.0表示完全一致
     */
    public static double getClosenessFactorTo(final BlockPos source1, final BlockPos target1, final BlockPos source2, final BlockPos target2)
    {
        final double newLength = BlockPosUtil.getDistance(target1, source1);
        if (newLength <= 0)
        {
            // Return a relatively high value(bad) when the distance is bad.
            return 10;
        }

        final double targetCloseness = BlockPosUtil.getDistance(target1, target2) / newLength;
        final double sourceCloseness = BlockPosUtil.getDistance(source1, source2) / newLength;

        return (targetCloseness + sourceCloseness) * 5;
    }

    /**
     * 获取请求的源位置。
     *
     * @param request 请求
     * @return 源位置
     */
    private BlockPos getSource(final IRequest<?> request)
    {
        if (request.getRequest() instanceof Delivery)
        {
            return ((Delivery) request.getRequest()).getStart().getInDimensionLocation();
        }

        if (request.getRequest() instanceof Pickup)
        {
            final IWareHouse wareHouse = findWareHouse();
            if (wareHouse != null)
            {
                return wareHouse.getID();
            }
        }

        return null;
    }

    /**
     * 获取请求的目标位置。
     *
     * @param request 请求
     * @return 目标位置
     */
    private BlockPos getTarget(final IRequest<?> request)
    {
        if (request.getRequest() instanceof Delivery)
        {
            return ((Delivery) request.getRequest()).getTarget().getInDimensionLocation();
        }

        if (request.getRequest() instanceof Pickup)
        {
            return request.getRequester().getLocation().getInDimensionLocation();
        }

        return null;
    }
    /**
     * 查找我们的运送员被分配到的仓库
     *
     * @return 仓库建筑物或null
     */
    public IWareHouse findWareHouse()
    {
        for (final IWareHouse building : getColony().getBuildingManager().getWareHouses())
        {
            if (building.getFirstModuleOccurance(CourierAssignmentModule.class).hasAssignedCitizen(getCitizen()))
            {
                return building;
            }
        }

        return null;
    }

    /**
     * 添加正在进行的并发交付。
     * @param requestToken 请求的令牌。
     */
    public void addConcurrentDelivery(final IToken<?> requestToken)
    {
        getDataStore().getOngoingDeliveries().add(requestToken);
    }

    /**
     * 移除正在进行的并发交付。
     * @param requestToken 请求的令牌。
     */
    public void removeConcurrentDelivery(final IToken<?> requestToken)
    {
        getDataStore().getOngoingDeliveries().remove(requestToken);
    }
}
