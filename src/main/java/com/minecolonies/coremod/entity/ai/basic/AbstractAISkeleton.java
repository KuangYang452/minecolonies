package com.minecolonies.coremod.entity.ai.basic;

import com.minecolonies.api.colony.jobs.IJob;
import com.minecolonies.api.entity.ai.DesiredActivity;
import com.minecolonies.api.entity.ai.Status;
import com.minecolonies.api.entity.ai.statemachine.states.AIWorkerState;
import com.minecolonies.api.entity.ai.statemachine.states.IAIState;
import com.minecolonies.api.entity.ai.statemachine.tickratestatemachine.ITickRateStateMachine;
import com.minecolonies.api.entity.ai.statemachine.tickratestatemachine.TickRateStateMachine;
import com.minecolonies.api.entity.ai.statemachine.tickratestatemachine.TickingTransition;
import com.minecolonies.api.entity.citizen.AbstractEntityCitizen;
import com.minecolonies.api.entity.citizen.VisibleCitizenStatus;
import com.minecolonies.api.util.CompatibilityUtils;
import com.minecolonies.api.util.ItemStackUtils;
import com.minecolonies.coremod.MineColonies;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.EnumSet;

import net.minecraft.world.entity.ai.goal.Goal.Flag;

/**
 * 工人AI的骨架类。这里将处理通用的目标执行。在此级别上没有实用性！
 *
 * @param <J> 此AI将拥有的工作。
 */
public abstract class AbstractAISkeleton<J extends IJob<?>> extends Goal
{
    /**
     * 市民的工作
     */
    @NotNull
    protected final J                     job;
    /**
     * 抽象的市民实体
     */
    @NotNull
    protected final AbstractEntityCitizen worker;
    protected final Level                 world;

    /**
     * 此AI使用的状态机
     */
    @NotNull
    private final ITickRateStateMachine<IAIState> stateMachine;

    /**
     * 为每个AI设置一些重要的骨架内容。
     *
     * @param job 工作类。
     */
    protected AbstractAISkeleton(@NotNull final J job)
    {
        super();

        if (!job.getCitizen().getEntity().isPresent())
        {
            throw new IllegalArgumentException("不能从附加到没有实体的市民的工作中实例化AI。");
        }

        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        this.job = job;
        this.worker = this.job.getCitizen().getEntity().get();
        this.world = CompatibilityUtils.getWorldFromCitizen(this.worker);
        stateMachine = new TickRateStateMachine<>(AIWorkerState.INIT, this::onException);
        stateMachine.setTickRate(MineColonies.getConfig().getServer().updateRate.get());
    }

    /**
     * 注册一个目标。
     *
     * @param target 要注册的目标。
     */
    public void registerTarget(final TickingTransition<IAIState> target)
    {
        stateMachine.addTransition(target);
    }

    /**
     * 注册您的AI需要的所有目标。它们将按注册顺序检查，因此请相应地排序它们。
     *
     * @param targets 需要注册的一些目标
     */
    protected final void registerTargets(final TickingTransition<IAIState>... targets)
    {
        Arrays.asList(targets).forEach(this::registerTarget);
    }

    /**
     * 返回是否应该开始执行目标。
     *
     * @return 如果需要执行，则为true。
     */
    @Override
    public final boolean canUse()
    {
        return worker.getDesiredActivity() == DesiredActivity.WORK;
    }

    /**
     * 返回是否应继续执行正在进行中的目标。
     */
    @Override
    public final boolean canContinueToUse()
    {
        return super.canContinueToUse();
    }

    /**
     * 执行一个一次性任务或开始执行连续任务。
     */
    @Override
    public final void start()
    {
        worker.getCitizenStatusHandler().setStatus(Status.WORKING);
        worker.getCitizenData().setVisibleStatus(VisibleCitizenStatus.WORKING);
    }

    /**
     * 重置任务。
     */
    @Override
    public final void stop()
    {
        resetAI();
        worker.getCitizenData().setVisibleStatus(null);
    }

    /**
     * 更新任务。
     */
    @Override
    public final void tick()
    {
        stateMachine.tick();
    }

    protected void onException(final RuntimeException e)
    {
    }

    /**
     * 设置标志位以告知其他任务是否可以并发运行，设置为final以保留行为：
     * 测试是一个简单的按位与 - 如果结果为零，则两个任务可以并发运行，如果不是，则它们必须互斥运行。
     *
     * @param mutexBits 要标记此标志位的位。
     */
    @Override
    public final void setFlags(final EnumSet<Flag> mutexBits)
    {
        super.setFlags(mutexBits);
    }

    /**
     * 获取AI当前所处的状态。
     *
     * @return 当前的IAIState。
     */
    public final IAIState getState()
    {
        return stateMachine.getState();
    }

    /**
     * 获取工人状态机的更新速率
     *
     * @return 更新速率
     */
    public int getTickRate()
    {
        return stateMachine.getTickRate();
    }

    /**
     * AI是否允许被中断
     *
     * @return 如果可以被中断，则为true
     */
    public boolean canBeInterrupted()
    {
        return getState().isOkayToEat();
    }

    /**
     * 重置工人AI为空闲状态，谨慎使用，中断所有当前的动作
     */
    public void resetAI()
    {
        stateMachine.reset();
        worker.setRenderMetadata("");
    }

    /**
     * 获取AI的状态机
     *
     * @return 状态机
     */
    public ITickRateStateMachine<IAIState> getStateAI()
    {
        return stateMachine;
    }

    /**
     * 在移除AI时。
     * 清理装备。
     */
    public void onRemoval()
    {
        worker.setItemSlot(EquipmentSlot.CHEST, ItemStackUtils.EMPTY);
        worker.setItemSlot(EquipmentSlot.FEET, ItemStackUtils.EMPTY);
        worker.setItemSlot(EquipmentSlot.HEAD, ItemStackUtils.EMPTY);
        worker.setItemSlot(EquipmentSlot.LEGS, ItemStackUtils.EMPTY);
        worker.setItemSlot(EquipmentSlot.OFFHAND, ItemStackUtils.EMPTY);
        worker.setItemSlot(EquipmentSlot.MAINHAND, ItemStackUtils.EMPTY);
    }
}
