package com.minecolonies.api.colony.buildings;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.reflect.TypeToken;
import com.minecolonies.api.colony.ICitizenData;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.buildings.modules.IBuildingModule;
import com.minecolonies.api.colony.buildings.modules.settings.ISetting;
import com.minecolonies.api.colony.buildings.modules.settings.ISettingKey;
import com.minecolonies.api.colony.buildings.registry.BuildingEntry;
import com.minecolonies.api.colony.jobs.registry.JobEntry;
import com.minecolonies.api.colony.requestsystem.request.IRequest;
import com.minecolonies.api.colony.requestsystem.requestable.IRequestable;
import com.minecolonies.api.colony.requestsystem.requester.IRequester;
import com.minecolonies.api.colony.requestsystem.resolver.IRequestResolver;
import com.minecolonies.api.colony.requestsystem.resolver.IRequestResolverProvider;
import com.minecolonies.api.colony.requestsystem.token.IToken;
import com.minecolonies.api.crafting.ItemStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.Tuple;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.items.IItemHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Predicate;

import static com.minecolonies.api.util.constant.Suppression.GENERIC_WILDCARD;
import static com.minecolonies.api.util.constant.ToolLevelConstants.TOOL_LEVEL_MAXIMUM;
import static com.minecolonies.api.util.constant.ToolLevelConstants.TOOL_LEVEL_WOOD_OR_GOLD;

public interface IBuilding extends IBuildingContainer, IRequestResolverProvider, IRequester, ISchematicProvider
{
    /**
     * 询问木制工具的最低等级。 (WOOD_HUT_LEVEL + 1 == 石制工具)
     */
    int WOOD_HUT_LEVEL = 0;

    /**
     * 检查建筑是否具有特定模块。
     * @param clazz 要检查的模块的类或接口。
     * @return 如果是，则返回true。
     */
    boolean hasModule(final Class<? extends IBuildingModule> clazz);

    /**
     * 获取具有特定类或接口的第一个模块。
     * @param clazz 模块的类或接口。
     * @return 模块，如果不存在则为空。
     */
    @NotNull
    <T extends IBuildingModule> T getFirstModuleOccurance(Class<T> clazz);

    /**
     * 获取具有特定类或接口的第一个模块。
     * @param clazz 模块的类或接口。
     * @return 模块，如果不存在则为空。
     */
    @NotNull
    <T extends IBuildingModule> Optional<T> getFirstOptionalModuleOccurance(Class<T> clazz);

    /**
     * 获取具有特定类或接口的所有模块。
     * @param clazz 模块的接口（或类，但在这种情况下更喜欢使用getModule）
     * @return 模块列表，如果没有匹配项则为空。
     */
    @NotNull
    <T extends IBuildingModule> List<T> getModules(Class<T> clazz);

    /**
     * 获取与特定谓词匹配的模块。
     * @param clazz 模块的类。
     * @param modulePredicate 要匹配的谓词。
     * @param <T> 模块类型。
     * @return 第一个匹配的模块。
     * @throws IllegalArgumentException 如果您的条件不匹配任何模块
     */
    @NotNull
    <T extends IBuildingModule> T getModuleMatching(Class<T> clazz, Predicate<? super T> modulePredicate);

    /**
     * 注册建筑的特定模块。
     * @param module 要注册的模块。
     */
    void registerModule(@NotNull final IBuildingModule module);

    /**
     * 获取建筑的自定义名称。
     *
     * @return 自定义名称。
     */
    @NotNull
    String getCustomName();

    /**
     * 获取建筑的显示名称。
     * 返回自定义名称（如果有）或原理图名称。
     *
     * @return 显示名称。
     */
    @NotNull
    String getBuildingDisplayName();

    /**
     * 在新的一天开始时执行。
     */
    default void onWakeUp() { }

    /**
     * 在市民完成清理库存后调用，当市民暂停后调用。仅用于清理状态。
     *
     * @param citizen 清理的市民。
     */
    void onCleanUp(ICitizenData citizen);

    /**
     * 在调用RestartCitizenMessage时并且工人已暂停时执行。用于重置，onCleanUp在此之前调用
     *
     * @param citizen 分配给建筑的市民。
     */
    void onRestart(ICitizenData citizen);

    /**
     * 当建筑放置在世界中时调用。
     */
    void onPlacement();

    /**
     * 当玩家靠近建筑时调用。
     *
     * @param player 进入的玩家
     */
    default void onPlayerEnterNearby(final Player player) {}

    /**
     * 当玩家进入建筑区域时调用。
     *
     * @param player 进入的玩家
     */
    default void onPlayerEnterBuilding(final Player player) {}

    /**
     * 检查块是否与当前对象匹配。
     *
     * @param block 您要知道它是否与此类匹配的块。
     * @return 如果块与此类匹配，则为true，否则为false。
     */
    boolean isMatchingBlock(@NotNull Block block);

    /**
     * 摧毁块。调用onDestroyed（）。
     */
    void destroy();

    void onDestroyed();

    /**
     * 从建筑中获取殖民地。
     * @return 它所属的殖民地。
     */
    IColony getColony();

    /**
     * 用于定义建筑是否可以由等级不到1的建筑师建造。
     *
     * @param newLevel 建筑的新等级。
     * @return 如果可以，则返回true。
     */
    boolean canBeBuiltByBuilder(int newLevel);

    @Override
    void markDirty();

    /**
     * 检查此建筑是否有工作订单。
     *
     * @return 如果建筑正在建设、升级或维修，则返回true。
     */
    boolean hasWorkOrder();

    /**
     * 删除建筑的工作订单。
     * <p>
     * 删除升级或维修工作订单
     */
    void removeWorkOrder();

    /**
     * 计算根据等级计算的建筑的索赔半径。
     *
     * @param buildingLevel 建筑等级。
     * @return 半径。
     */
    int getClaimRadius(int buildingLevel);

    /**
     * 序列化为视图。发送3个整数。 1）类名称的哈希码。 2）建筑等级。 3）最大建筑等级。
     *
     * @param buf 用于写入的FriendlyByteBuf。
     */
    void serializeToView(@NotNull FriendlyByteBuf buf);

    /**
     * 设置建筑的自定义名称。
     *
     * @param name 要设置的名称。
     */
    void setCustomBuildingName(String name);

    /**
     * 检查建筑是否应由dman收集。
     *
     * @return 如果是，则返回true。
     */
    boolean canBeGathered();

    /**
     * 为当前建筑请求升级。
     *
     * @param player  发出请求的玩家。
     * @param builder 分配的建筑师。
     */
    void requestUpgrade(Player player, BlockPos builder);

    /**
     * 为当前建筑请求移除。
     *
     * @param player  发出请求的玩家。
     * @param builder 分配的建筑师。
     */
    void requestRemoval(Player player, BlockPos builder);

    /**
     * 为当前建筑请求维修。
     *
     * @param builder 分配的建筑师。
     */
    void requestRepair(BlockPos builder);

    /**
     * 检查建筑是否已经建造。
     *
     * @return 如果是，则返回true。
     */
    boolean isBuilt();

    /**
     * 拆解建筑。
     */
    void deconstruct();

    /**
     * 在升级过程完成时调用。我们抑制了此警告，因为此参数将在覆盖此方法的子类中使用。
     *
     * @param newLevel 新等级。
     */
    void onUpgradeComplete(int newLevel);

    /**
     * 此建筑附近是否有警卫建筑
     *
     * @return true/false
     */
    boolean isGuardBuildingNear();

    /**
     * 请求重新计算此建筑是否附近有警卫建筑
     */
    void resetGuardBuildingNear();

    /**
     * 检查工人是否需要某种物品的一定数量，已经保留了该物品。
     * 如果工人需要某种物品的一定数量，则始终保留一个堆栈。 以确保安全。
     *
     * @param stack            要检查的堆栈。
     * @param localAlreadyKept 已经保留的物品。
     * @param inventory        它应该在库存中还是在建筑中。
     * @param jobEntry 尝试倾倒的工作条目。
     * @return 可以倾倒的数量，或者如果不能则为0。
     */
    int buildingRequiresCertainAmountOfItem(ItemStack stack, List<ItemStorage> localAlreadyKept, boolean inventory, @Nullable final JobEntry jobEntry);

    /**
     * 检查建筑是否需要某种物品的一定数量，已经保留了该物品。
     * 如果工人需要某种物品的一定数量，则始终保留一个堆栈。 以确保安全。
     *
     * @param stack            要检查的堆栈。
     * @param localAlreadyKept 已经保留的物品。
     * @param inventory        它应该在库存中还是在建筑中。
     * @return 可以倾倒的数量，或者如果不能则为0。
     */
    default int buildingRequiresCertainAmountOfItem(ItemStack stack, List<ItemStorage> localAlreadyKept, boolean inventory)
    {
        return buildingRequiresCertainAmountOfItem(stack, localAlreadyKept, inventory, null);
    }
    /**
     * 如果要在库存中保留一定数量的物品，请覆盖此方法。当库存已满时，所有物品都会被倒入建筑物的储物箱。但您可以使用此方法保留一些堆栈。
     *
     * @return 应该保留的对象列表。
     */
    Map<Predicate<ItemStack>, Tuple<Integer, Boolean>> getRequiredItemsAndAmount();

    /**
     * 尝试将一个堆栈传输到建筑物的一个库存中并强制传输。
     *
     * @param stack 堆栈要传输。
     * @param world 要执行的世界。
     * @return 已替换的物品堆栈或无法传输的物品堆栈
     */
    @Nullable
    ItemStack forceTransferStack(ItemStack stack, Level world);

    /**
     * 为市民创建一个请求。
     *
     * @param citizenData 市民的数据。
     * @param requested   要创建的请求。
     * @param async       是否异步。
     * @param <R>         请求的类型。
     * @return 请求的令牌。
     */
    <R extends IRequestable> IToken<?> createRequest(@NotNull ICitizenData citizenData, @NotNull R requested, boolean async);

    /**
     * 为建筑物创建一个请求。
     *
     * @param requested 要创建的请求。
     * @param async     是否异步。
     * @param <R>       请求的类型。
     * @return 请求的令牌。
     */
    <R extends IRequestable> IToken<?> createRequest(@NotNull R requested, boolean async);

    boolean hasWorkerOpenRequests(final int citizenid);

    Collection<IRequest<?>> getOpenRequests(final int citizenid);

    boolean hasWorkerOpenRequestsFiltered(final int citizenid, @NotNull Predicate<IRequest<?>> selectionPredicate);

    /**
     * 检查市民是否有未完成的同步请求，阻止其工作。
     *
     * @param citizen 要检查的市民数据。
     * @return 如果存在未完成的非异步请求，则返回true。
     */
    boolean hasOpenSyncRequest(@NotNull ICitizenData citizen);

    <R> boolean hasWorkerOpenRequestsOfType(final int citizenid, TypeToken<R> requestType);

    @SuppressWarnings(GENERIC_WILDCARD)
    <R> ImmutableList<IRequest<? extends R>> getOpenRequestsOfType(
            final int citizenid,
            TypeToken<R> requestType);

    boolean hasCitizenCompletedRequests(@NotNull ICitizenData data);

    boolean hasCitizenCompletedRequestsToPickup(@NotNull ICitizenData data);

    Collection<IRequest<?>> getCompletedRequests(@NotNull ICitizenData data);

    @SuppressWarnings(GENERIC_WILDCARD)
    <R> ImmutableList<IRequest<? extends R>> getCompletedRequestsOfType(@NotNull ICitizenData citizenData, TypeToken<R> requestType);

    @SuppressWarnings(GENERIC_WILDCARD)
    <R> ImmutableList<IRequest<? extends R>> getCompletedRequestsOfTypeFiltered(
            @NotNull ICitizenData citizenData,
            TypeToken<R> requestType,
            Predicate<IRequest<? extends R>> filter);

    void markRequestAsAccepted(@NotNull ICitizenData data, @NotNull IToken<?> token);

    void cancelAllRequestsOfCitizen(@NotNull ICitizenData data);

    /**
     * 用给定的堆栈覆盖下一个打开的请求。
     * <p>
     * 我们使用squid:s135来处理循环中的不要有太多的continue语句，因为出于性能原因，这里是有意义的。
     *
     * @param stack 堆栈。
     */
    @SuppressWarnings("squid:S135")
    void overruleNextOpenRequestWithStack(@NotNull ItemStack stack);

    @SuppressWarnings(GENERIC_WILDCARD)
    <R> ImmutableList<IRequest<? extends R>> getOpenRequestsOfTypeFiltered(
            @NotNull ICitizenData citizenData,
            TypeToken<R> requestType,
            Predicate<IRequest<? extends R>> filter);

    boolean overruleNextOpenRequestOfCitizenWithStack(@NotNull ICitizenData citizenData, @NotNull ItemStack stack);

    /**
     * 为建筑物创建一个取货请求。它将确保每个建筑物只存在一个取货请求，因此可以多次调用它是安全的。如果已经存在取货请求，或者优先级不在适当范围内，或者取货优先级设置为NEVER（0），则调用将返回false。
     *
     * @param scaledPriority 取货请求的优先级。此值已被考虑为已经缩放！
     * @return 如果可以创建取货请求，则返回true，否则返回false。
     */
    boolean createPickupRequest(final int scaledPriority);

    @Override
    ImmutableCollection<IRequestResolver<?>> getResolvers();

    ImmutableCollection<IRequestResolver<?>> createResolvers();

    IRequester getRequester();

    Optional<ICitizenData> getCitizenForRequest(@NotNull IToken<?> token);

    /**
     * 计算解决程序无法触及的保留堆栈数量。
     * @return 物品存储的映射。
     */
    Map<ItemStorage, Integer> reservedStacks();

    /**
     * 处理离线时间。
     * @param time 时间（以秒为单位）。
     */
    void processOfflineTime(long time);

    /**
     * 从原理图数据计算所有建筑物角落。
     */
    void calculateCorners();

    /**
     * 检查某个位置是否位于建筑物内。
     * @param pos 要检查的位置。
     * @return 如果是，则返回true。
     */
    boolean isInBuilding(@NotNull final BlockPos pos);

    /**
     * 将建筑物的级别升级到适合其原理图数据的级别
     */
    void upgradeBuildingLevelToSchematicData();

    /**
     * 获取按类型分类的所有打开请求的映射。
     * @return 映射。
     */
    Map<TypeToken<?>, Collection<IToken<?>>> getOpenRequestsByRequestableType();

    /**
     * 拾取建筑，包括级别，并将其放入玩家的库存中。
     * @param player 进行拾取的玩家。
     */
    void pickUp(final Player player);

    /**
     * 获取建筑类型
     *
     * @return 建筑类型
     */
    BuildingEntry getBuildingType();

    /**
     * 设置建筑类型
     *
     * @param buildingType 建筑类型
     */
    void setBuildingType(BuildingEntry buildingType);

    /**
     * 殖民地滴答声。
     */
    void onColonyTick(IColony colony);

    /**
     * 检查市民是否在工作请求中包含特定的堆栈。
     *
     * @param stack 要检查的堆栈。
     * @return 如果是，则返回true。
     */
    boolean isItemStackInRequest(@Nullable ItemStack stack);

    /**
     * 获取工人可用的最大工具等级。
     *
     * @return 整数。
     */
    default int getMaxToolLevel()
    {
        if (getBuildingLevel() >= getMaxBuildingLevel())
        {
            return TOOL_LEVEL_MAXIMUM;
        }
        else if (getBuildingLevel() <= WOOD_HUT_LEVEL)
        {
            return TOOL_LEVEL_WOOD_OR_GOLD;
        }
        return getBuildingLevel() - WOOD_HUT_LEVEL;
    }

    /**
     * 获取殖民地中的所有分配市民的集合。
     * @return 列表
     */
    Set<ICitizenData> getAllAssignedCitizen();

    /**
     * 获取与此建筑相关的所有处理程序。
     *
     * @return 建筑+市民的处理程序。
     */
    List<IItemHandler> getHandlers();

    /**
     * 获取键的设置。实用函数。
     * @param key 键。
     * @param <T> 键类型。
     * @return 设置。
     */
    <T extends ISetting> T getSetting(@NotNull final ISettingKey<T> key);

    /**
     * 获取键的设置。实用函数。
     * @param key 键。
     * @param <T> 键类型。
     * @return 包装值的可选项。
     */
    @NotNull
    <T extends ISetting> Optional<T> getOptionalSetting(@NotNull final ISettingKey<T> key);

    /**
     * 检查分配的市民是否允许食用以下堆栈。
     *
     * @param stack 要测试的堆栈。
     * @return 如果允许，则返回true。
     */
    boolean canEat(final ItemStack stack);
}
