package com.minecolonies.api.colony.managers.interfaces;

import com.minecolonies.api.colony.ICitizenData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 市民管理器的接口。
 */
public interface ICitizenManager extends IEntityManager
{

    /**
     * 生成一个全新的市民。
     */
    void spawnOrCreateCitizen();

    /**
     * 生成具有特定市民数据的市民。
     *
     * @param data  用于生成市民的数据。
     * @param world 要生成市民的世界。
     * @return 生成的市民的市民数据。
     */
    default ICitizenData spawnOrCreateCitizen(final ICitizenData data, @NotNull final Level world)
    {
        return this.spawnOrCreateCivilian(data, world, null, false);
    }

    /**
     * 生成具有特定市民数据的市民。
     *
     * @param data     用于生成市民的数据。
     * @param world    要生成市民的世界。
     * @param spawnPos 要生成的位置。
     * @return 新的市民。
     */
    default ICitizenData spawnOrCreateCitizen(final ICitizenData data, @NotNull final Level world, final BlockPos spawnPos)
    {
        return this.spawnOrCreateCivilian(data, world, spawnPos, false);
    }

    /**
     * 获取第一个失业的市民。
     *
     * @return 没有当前工作的市民。
     */
    @Nullable
    ICitizenData getJoblessCitizen();

    /**
     * 重新计算殖民地中可以容纳的市民数量。
     */
    void calculateMaxCitizens();

    @Override
    ICitizenData createAndRegisterCivilianData();

    /**
     * 从保存的NBT中复活市民。
     *
     * @param compoundNBT 保存的市民NBT
     * @param resetId 如果为true，将计算一个新的市民ID
     * @param world 世界
     * @param spawnPos 复活市民的位置
     * @return 复活的市民的市民数据
     */
    ICitizenData resurrectCivilianData(@NotNull final CompoundTag compoundNBT, final boolean resetId, @NotNull final Level world, final BlockPos spawnPos);

    /**
     * 获取所有市民。
     *
     * @return 市民列表的副本。
     */
    List<ICitizenData> getCitizens();

    /**
     * 获取殖民地的最大市民数量。
     *
     * @return 数量。
     */
    int getMaxCitizens();

    /**
     * 获取殖民地的潜在最大市民数量。潜在值考虑所有可用的床位，包括未分配的哨塔。
     *
     * @return 数量。
     */
    int getPotentialMaxCitizens();

    /**
     * 基于研究获取最大市民数量。
     *
     * @return 最大值。
     */
    double maxCitizensFromResearch();

    /**
     * 获取当前市民数量，可能大于{@link #getMaxCitizens()}。
     *
     * @return 殖民地中的当前市民数量。
     */
    int getCurrentCitizenCount();

    /**
     * 设置新的最大市民数量。
     *
     * @param newMaxCitizens 要设置的数量。
     */
    void setMaxCitizens(final int newMaxCitizens);

    /**
     * 设置新的潜在最大市民数量。潜在值考虑所有可用的床位，包括未分配的哨塔。
     *
     * @param newMaxCitizens 要设置的潜在数量。
     */
    void setPotentialMaxCitizens(final int newMaxCitizens);

    /**
     * 检查市民的幸福度并更新殖民地的幸福度。
     */
    void checkCitizensForHappiness();

    /**
     * 更新所有活跃市民的市民数据。
     */
    void tickCitizenData();

    /**
     * 调用此方法以设置殖民地中所有市民是否哀悼。
     *
     * @param mourn 表示市民是否应该哀悼的布尔值
     */
    void updateCitizenMourn(final ICitizenData data, final boolean mourn);

    /**
     * 调用此方法以设置所有市民是否入睡。
     *
     * @param sleep 表示所有市民是否入睡的布尔值
     */
    void updateCitizenSleep(final boolean sleep);

    /**
     * 获取一个随机的市民。
     *
     * @return 随机市民。
     */
    ICitizenData getRandomCitizen();

    /**
     * 更新所有市民的修饰符。
     *
     * @param id 名称。
     */
    void updateModifier(final String id);

    /**
     * 当市民入睡时调用此方法。
     */
    void onCitizenSleep();

    @Override
    ICitizenData getCivilian(final int citizenId);

    /**
     * 早上调用此方法。
     */
    void onWakeUp();
}
