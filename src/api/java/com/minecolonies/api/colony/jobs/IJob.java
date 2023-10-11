package com.minecolonies.api.colony.jobs;

import com.minecolonies.api.colony.ICitizenData;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.jobs.registry.JobEntry;
import com.minecolonies.api.colony.requestsystem.token.IToken;
import com.minecolonies.api.entity.citizen.AbstractEntityCitizen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.util.INBTSerializable;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

import static com.minecolonies.api.util.constant.HappinessConstants.IDLE_AT_JOB_COMPLAINS_DAYS;
import static com.minecolonies.api.util.constant.HappinessConstants.IDLE_AT_JOB_DEMANDS_DAYS;

public interface IJob<AI extends Goal> extends INBTSerializable<CompoundTag>
{
    /**
     * 获取此工作的{@link JobEntry}。
     *
     * @return {@link JobEntry}。
     */
    JobEntry getJobRegistryEntry();

    /**
     * 获取市民执行此工作角色时要使用的RenderBipedCitizen.Model。
     *
     * @return 市民的模型。
     */
    ResourceLocation getModel();

    /**
     * 获取与此工作关联的殖民地（快捷方式为getAssignedCitizen().getColonyByPosFromWorld()）。
     *
     * @return 市民的{@link com.minecolonies.api.colony.IColony}。
     */
    IColony getColony();

    /**
     * 获取与此工作相关联的一组异步请求。
     *
     * @return 一组ITokens。
     */
    Set<IToken<?>> getAsyncRequests();

    /**
     * 重写以向给定的EntityAITask列表添加特定于工作的AI任务。
     *
     * @param tasks 要添加任务的EntityAITasks列表。
     */
    void addWorkerAIToTaskList(@NotNull GoalSelector tasks);

    /**
     * 生成要注册的AI类。
     * <p>
     * 抑制Sonar规则squid:S1452，此规则执行“不应在返回参数中使用通用通配符类型”，但在这种情况下不适用，因为我们对所有AbstractJob实现都满意，并且仅需要泛型用于Java。
     *
     * @return 您的个人AI实例。
     */
    @SuppressWarnings("squid:S1452")
    AI generateAI();

    /**
     * 检查市民今天是否已经检查了他的储物柜中的食物。
     *
     * @return 如果是，则为true。
     */
    boolean hasCheckedForFoodToday();

    /**
     * 设置市民今天已经在他的储物柜中搜寻食物。
     */
    void setCheckedForFood();

    /**
     * 此方法可用于显示当前状态。市民拥有。
     *
     * @return 在名牌中显示信息的小字符串
     */
    String getNameTagDescription();

    /**
     * 由AI骨架使用的方法来更改市民的名称。主要用于更新调试信息。
     *
     * @param nameTag 要显示的名称标签。
     */
    void setNameTag(String nameTag);

    /**
     * 重写此方法以实现特定于工作的死亡成就。
     *
     * @param source  死亡的来源
     * @param citizen 刚刚死亡的市民
     */
    void triggerDeathAchievement(DamageSource source, AbstractEntityCitizen citizen);

    /**
     * 当实体拾取堆栈时调用的方法。
     *
     * @param pickedUpStack 正在拾取的堆栈。
     * @return 当堆栈已用于解决请求时为true，否则为false。
     */
    boolean onStackPickUp(@NotNull ItemStack pickedUpStack);

    /**
     * 市民升级时的级别上升操作，允许基于工作的自定义操作。
     */
    default void onLevelUp()
    {}

    /**
     * 当实体生成/分配给工作时，初始化实体的值。
     *
     * @param citizen
     */
    default void initEntityValues(AbstractEntityCitizen citizen)
    {}

    /**
     * 获取此工作所属的CitizenData。
     *
     * @return 拥有此工作的CitizenData。
     */
    ICitizenData getCitizen();

    /**
     * 在每次殖民地醒来时执行。
     */
    void onWakeUp();

    /**
     * 检查是否可以进食。
     *
     * @return 如果可以，则为true。
     */
    boolean canAIBeInterrupted();

    /**
     * 获取已执行的操作数量的getter。
     *
     * @return 数量。
     */
    int getActionsDone();

    /**
     * 将自上次重置以来已完成的操作数增加1。例如，用于检测何时必须倾倒库存。
     */
    void incrementActionsDone();

    /**
     * 将自上次重置以来已完成的操作数增加numberOfActions。例如，用于检测何时必须倾倒库存。
     */
    void incrementActionsDone(int numberOfActions);

    /**
     * 清除已完成的操作计数器。在倾倒进储物柜时调用此方法。
     */
    void clearActionsDone();

    /**
     * 获取与此工作关联的工人AI。
     *
     * @return 工人AI。
     */
    AI getWorkerAI();

    /**
     * 检查市民是否处于空闲状态。
     *
     * @return 如果是，则为true。
     */
    boolean isIdling();

    /**
     * 重置AI。
     */
    void resetAI();

    /**
     * 检查工作是否允许回避。
     *
     * @return 如果是，则为true。
     */
    boolean allowsAvoidance();

    /**
     * 工作的疾病修饰符。
     *
     * @return 工作的修饰符。
     */
    int getDiseaseModifier();

    /**
     * 当工作被移除（市民死亡或工作变更）时。
     */
    void onRemoval();

    /**
     * 检查特定工作是否忽略特定伤害类型的方法。
     *
     * @param damageSource 要检查的伤害来源。
     * @return 如果是，则为true。
     */
    boolean ignoresDamage(@NotNull final DamageSource damageSource);

    /**
     * 将请求标记为同步请求（阻塞请求）。
     *
     * @param id id。
     */
    void markRequestSync(IToken<?> id);

    /**
     * 如果工人可以拾取堆栈。
     * @param pickedUpStack 要检查的堆栈。
     * @return 如果是，则为true。
     */
    boolean pickupSuccess(@NotNull ItemStack pickedUpStack);

    /**
     * 处理殖民地离线时间。
     * @param time 时间（秒）。
     */
    void processOfflineTime(long time);

    /**
     * 将工作序列化到缓冲区。
     * @param buffer 要序列化到的缓冲区。
     */
    void serializeToView(final FriendlyByteBuf buffer);

    /**
     * 获取工作在多少秒后认为自己处于不活跃状态的时间限制。
     * @return 限制，如果不适用则为-1。
     */
    default int getInactivityLimit()
    {
        return -1;
    }

    /**
     * 获取投诉或要求解决闲置状态的天数
     * @param isDemand 如果要查找要求时间则为true
     * @return 天数
     */
    default int getIdleSeverity(boolean isDemand)
    {
        if(isDemand)
        {
            return IDLE_AT_JOB_DEMANDS_DAYS;
        }
        else
        {
            return IDLE_AT_JOB_COMPLAINS_DAYS;
        }
    }

    /**
     * 在活动状态更改时触发基于工作的操作（从活动到非活动，或从非活动到活动）。
     * @param newState 新状态（true表示活动，false表示非活动）。
     */
    default void triggerActivityChangeAction(boolean newState)
    {
        //noop.
    }

    /**
     * 设置工作的注册条目。
     *
     * @param jobEntry 属于它的工作条目。
     */
    void setRegistryEntry(JobEntry jobEntry);

    /**
     * 工作是否为守卫。
     *
     * @return
     */
    default boolean isGuard()
    {
        return false;
    }
}
