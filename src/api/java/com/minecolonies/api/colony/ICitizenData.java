package com.minecolonies.api.colony;

import com.minecolonies.api.colony.buildings.IBuilding;
import com.minecolonies.api.colony.buildings.IBuilding;
import com.minecolonies.api.colony.jobs.IJob;
import com.minecolonies.api.entity.citizen.AbstractEntityCitizen;
import com.minecolonies.api.entity.citizen.VisibleCitizenStatus;
import com.minecolonies.api.entity.citizen.citizenhandlers.ICitizenHappinessHandler;
import com.minecolonies.api.entity.citizen.citizenhandlers.ICitizenMournHandler;
import com.minecolonies.api.entity.citizen.citizenhandlers.ICitizenSkillHandler;
import com.minecolonies.api.util.Tuple;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.Random;

public interface ICitizenData extends ICivilianData
{
    /**
     * 市民的最大饱和度。
     */
    int MAX_SATURATION = 20;

    /**
     * 当建筑物被销毁时，通知市民以便进行与建筑物关联的任何清理，建筑物自己的 IBuilding.onDestroyed 未执行的清理工作。
     *
     * @param building 被销毁的建筑物。
     */
    void onRemoveBuilding(IBuilding building);

    /**
     * 返回市民的家庭建筑物。
     *
     * @return 家庭建筑物。
     */
    @Nullable
    IBuilding getHomeBuilding();

    /**
     * 设置市民的家庭建筑物。
     *
     * @param building 家庭建筑物。
     */
    void setHomeBuilding(@Nullable IBuilding building);

    /**
     * 返回市民的工作建筑物。
     *
     * @return 市民的工作建筑物。
     */
    @Nullable
    IBuilding getWorkBuilding();

    /**
     * 设置市民的工作建筑物。
     *
     * @param building 工作建筑物。
     */
    void setWorkBuilding(@Nullable IBuilding building);

    /**
     * 返回市民的工作。
     *
     * @return 市民的工作。
     */
    IJob<?> getJob();

    /**
     * 设置市民的工作。
     *
     * @param job 市民的工作。
     */
    void setJob(IJob<?> job);

    /**
     * 返回市民所需的工作子类。类型不匹配时返回 null。
     *
     * @param type 所需的工作类型。
     * @param <J>  返回的工作类型。
     * @return 此市民拥有的工作。
     */
    @Nullable
    <J extends IJob<?>> J getJob(@NotNull Class<J> type);

    /**
     * 设置市民的上一个位置。
     *
     * @param lastPosition 上一个位置。
     */
    void setLastPosition(BlockPos lastPosition);

    /**
     * 获取市民的上一个位置。
     *
     * @return 上一个位置。
     */
    BlockPos getLastPosition();

    /**
     * 设置市民当前的饱和度。
     *
     * @param saturation 要设置的饱和度。
     */
    void setSaturation(double saturation);

    /**
     * 检查市民是否正在睡觉。
     *
     * @return 如果是，则返回 true。
     */
    boolean isAsleep();

    /**
     * 获取床的位置。
     *
     * @return 床的位置。
     */
    BlockPos getBedPos();

    /**
     * 设置是否正在睡觉。
     *
     * @param asleep 如果正在睡觉，则为 true。
     */
    void setAsleep(boolean asleep);

    /**
     * 设置床的位置。
     *
     * @param bedPos 要设置的位置。
     */
    void setBedPos(BlockPos bedPos);

    /**
     * 市民幸福度的处理程序。
     *
     * @return 处理程序的实例。
     */
    ICitizenHappinessHandler getCitizenHappinessHandler();

    /**
     * 市民悼念的处理程序。
     *
     * @return 处理程序的实例。
     */
    ICitizenMournHandler getCitizenMournHandler();

    /**
     * 获取市民技能处理程序。
     *
     * @return 处理程序。
     */
    ICitizenSkillHandler getCitizenSkillHandler();

    /**
     * 计划重启和清理。
     *
     * @param player 调度它的玩家。
     */
    void scheduleRestart(ServerPlayer player);

    /**
     * 是否应重启 AI，还要重新启动建筑等。
     *
     * @return 如果是，则返回 true。
     */
    boolean shouldRestart();

    /**
     * 重启完成。
     */
    void restartDone();

    /**
     * 设置是否为儿童。
     *
     * @param isChild 布尔值。
     */
    void setIsChild(boolean isChild);

    /**
     * 检查市民是否刚刚吃过。
     *
     * @return 如果是，则返回 true。
     */
    boolean justAte();

    /**
     * 设置或重置市民是否刚刚吃过。
     *
     * @param justAte 如果刚刚吃过，则为 true；如果要重置，则为 false。
     */
    void setJustAte(boolean justAte);

    /**
     * 是否在工作中闲置。
     *
     * @return 如果是，则返回 true。
     */
    boolean isIdleAtJob();

    /**
     * 设置是否在工作中闲置。
     *
     * @param idle 如果是，则为 true。
     */
    void setIdleAtJob(final boolean idle);

    /**
     * 获取实体。
     * @return
     */
    @Override
    Optional<AbstractEntityCitizen> getEntity();

    /**
     * 获取市民的状态。
     *
     * @return 状态。
     */
    VisibleCitizenStatus getStatus();

    /**
     * 设置市民的状态。
     *
     * @param status 要设置的状态。
     */
    void setVisibleStatus(VisibleCitizenStatus status);

    /**
     * 获取市民的随机数。
     * @return 随机数。
     */
    Random getRandom();

    /**
     * 应用研究效果到数据的实体。
     */
    void applyResearchEffects();

    /**
     * 市民去睡觉时触发。
     */
    void onGoSleep();

    /**
     * 设置下一个复活位置。
     *
     * @param pos 要设置的位置。
     */
    void setNextRespawnPosition(final BlockPos pos);

    /**
     * 检查市民是否在背包中有不够好的食物（但没有好食物）。
     * @return 如果是，则返回 true。
     */
    boolean needsBetterFood();

    /**
     * 获取市民的伴侣。
     * @return 伴侣或 null（如果不存在）。
     */
    @Nullable
    ICitizenData getPartner();

    /**
     * 获取市民的孩子列表。
     * @return 市民的 ID 列表。
     */
    List<Integer> getChildren();

    /**
     * 获取市民的兄弟姐妹列表。
     * @return 市民的 ID 列表。
     */
    List<Integer> getSiblings();

    /**
     * 获取父母的姓名。
     * @return 姓名。
     */
    Tuple<String, String> getParents();

    /**
     * 向市民添加一个或多个兄弟姐妹。
     * @param siblings 兄弟姐妹的 ID。
     */
    void addSiblings(final Integer...siblings);

    /**
     * 向市民添加一个或多个孩子。
     * @param children 孩子的 ID。
     */
    void addChildren(final Integer...children);

    /**
     * 为市民设置新的伴侣。
     * @param id 伴侣的 ID。
     */
    void setPartner(final int id);

    /**
     * 当市民死亡时，将在相关的市民上调用此方法。
     * @param id 市民的 ID。
     */
    void onDeath(final Integer id);

    /**
     * 设置市民的父母。
     * @param firstParent 第一个父母的姓名。
     * @param secondParent 第二个父母的姓名。
     */
    void setParents(final String firstParent, final String secondParent);

    /**
     * 生成市民的姓名。
     * @param rand 使用的随机函数。
     * @param firstParentName 第一个父母的姓名。
     * @param secondParentName 第二个父母的姓名。
     */
    void generateName(@NotNull final Random rand, final String firstParentName, final String secondParentName);

    /**
     * 检查两个市民是否有亲缘关系。
     * @param data 市民的数据。
     * @return 如果是，则返回 true。
     */
    boolean isRelatedTo(ICitizenData data);

    /**
     * 检查两个市民是否住在一起。
     * @param data 另一个市民的数据。
     * @return 如果是，则返回 true。
     */
    boolean doesLiveWith(ICitizenData data);

    /**
     * 设置工作是否当前处于活动状态的工作者。
     *
     * @param b 如果是，则为 true。
     */
    default void setWorking(final boolean b)
    {
        // 空操作
    }

    /**
     * 检查工作者是否当前处于活动状态。
     * @return 如果是，则返回 true。
     */
    default boolean isWorking()
    {
        return true;
    }

    /**
     * 在市民复活时触发。
     */
    void onResurrect();

    /**
     * 检查市民是否具有某种自定义纹理。
     * @return 如果是，则返回 true。
     */
    default boolean hasCustomTexture()
    {
        return false;
    }
}
