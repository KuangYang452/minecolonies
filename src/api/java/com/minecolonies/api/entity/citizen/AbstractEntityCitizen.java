package com.minecolonies.api.entity.citizen;

import com.google.common.collect.Lists;
import com.minecolonies.api.client.render.modeltype.ModModelTypes;
import com.minecolonies.api.client.render.modeltype.registry.IModelTypeRegistry;
import com.minecolonies.api.colony.ICitizenData;
import com.minecolonies.api.colony.ICitizenDataView;
import com.minecolonies.api.colony.jobs.IJob;
import com.minecolonies.api.colony.requestsystem.location.ILocation;
import com.minecolonies.api.entity.MinecoloniesMinecart;
import com.minecolonies.api.entity.ai.DesiredActivity;
import com.minecolonies.api.entity.ai.pathfinding.IWalkToProxy;
import com.minecolonies.api.entity.citizen.citizenhandlers.*;
import com.minecolonies.api.entity.pathfinding.AbstractAdvancedPathNavigate;
import com.minecolonies.api.entity.pathfinding.PathingStuckHandler;
import com.minecolonies.api.entity.pathfinding.registry.IPathNavigateRegistry;
import com.minecolonies.api.inventory.InventoryCitizen;
import com.minecolonies.api.sounds.EventType;
import com.minecolonies.api.util.CompatibilityUtils;
import com.minecolonies.api.util.ItemStackUtils;
import com.minecolonies.api.util.SoundUtils;
import com.mojang.datafixers.util.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.items.IItemHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Random;

import static com.minecolonies.api.util.constant.CitizenConstants.*;

/**
 * 抽象的市民实体。
 */
@SuppressWarnings({"PMD.ExcessiveImports", "PMD.CouplingBetweenObjects"})
public abstract class AbstractEntityCitizen extends AbstractCivilianEntity implements MenuProvider
{
    /**
     * 市民游泳速度因子
     */
    private static final double CITIZEN_SWIM_BONUS = 2.0;

    public static final EntityDataAccessor<Integer>  DATA_LEVEL           = SynchedEntityData.defineId(AbstractEntityCitizen.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<Integer>  DATA_TEXTURE         = SynchedEntityData.defineId(AbstractEntityCitizen.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<Integer>  DATA_IS_FEMALE       = SynchedEntityData.defineId(AbstractEntityCitizen.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<Integer>  DATA_COLONY_ID       = SynchedEntityData.defineId(AbstractEntityCitizen.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<Integer>  DATA_CITIZEN_ID      = SynchedEntityData.defineId(AbstractEntityCitizen.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<String>   DATA_MODEL           = SynchedEntityData.defineId(AbstractEntityCitizen.class, EntityDataSerializers.STRING);
    public static final EntityDataAccessor<String>   DATA_RENDER_METADATA = SynchedEntityData.defineId(AbstractEntityCitizen.class, EntityDataSerializers.STRING);
    public static final EntityDataAccessor<Boolean>  DATA_IS_ASLEEP       = SynchedEntityData.defineId(AbstractEntityCitizen.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Boolean>  DATA_IS_CHILD        = SynchedEntityData.defineId(AbstractEntityCitizen.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<BlockPos> DATA_BED_POS         = SynchedEntityData.defineId(AbstractEntityCitizen.class, EntityDataSerializers.BLOCK_POS);
    public static final EntityDataAccessor<String>   DATA_STYLE           = SynchedEntityData.defineId(AbstractEntityCitizen.class, EntityDataSerializers.STRING);
    public static final EntityDataAccessor<String>   DATA_TEXTURE_SUFFIX  = SynchedEntityData.defineId(AbstractEntityCitizen.class, EntityDataSerializers.STRING);
    public static final EntityDataAccessor<String>   DATA_JOB             = SynchedEntityData.defineId(AbstractEntityCitizen.class, EntityDataSerializers.STRING);

    /**
     * 默认模型。
     */
    private ResourceLocation modelId = ModModelTypes.SETTLER_ID;

    /**
     * 纹理ID。
     */
    private int textureId;

    /**
     * 额外的渲染数据。
     */
    private String renderMetadata = "";

    /**
     * 性别，如果为女性则为true。
     */
    private boolean female;

    /**
     * 纹理。
     */
    private ResourceLocation texture;

    /**
     * 纹理是否已经被市民视图初始化。
     */
    private boolean textureDirty = true;

    private AbstractAdvancedPathNavigate pathNavigate;

    /**
     * 计算实体碰撞的次数。
     */
    private int collisionCounter = 0;

    /**
     * 碰撞阈值
     */
    private final static int COLL_THRESHOLD = 50;

    /**
     * 检查装备是否已变脏的标志。
     */
    private boolean isEquipmentDirty = true;

    /**
     * 构造新市民类型实体的构造函数。
     *
     * @param type  实体类型。
     * @param world 世界。
     */
    public AbstractEntityCitizen(final EntityType<? extends AgeableMob> type, final Level world)
    {
        super(type, world);
    }

    /**
     * 获取带有其值的默认属性。
     *
     * @return 属性修饰符映射。
     */
    public static AttributeSupplier.Builder getDefaultAttributes()
    {
        return LivingEntity.createLivingAttributes()
                .add(Attributes.MAX_HEALTH, BASE_MAX_HEALTH)
                .add(Attributes.MOVEMENT_SPEED, BASE_MOVEMENT_SPEED)
                .add(Attributes.FOLLOW_RANGE, BASE_PATHFINDING_RANGE);
    }

    public GoalSelector getTasks()
    {
        return goalSelector;
    }

    public int getTicksExisted()
    {
        return tickCount;
    }

    @NotNull
    public BlockPos blockPosition()
    {
        return new BlockPos(getX(), getY(), getZ());
    }

    /**
     * 禁用物品拾取功能，因为我们自己处理它。
     */
    @Override
    public boolean canPickUpLoot()
    {
        return false;
    }

    /**
     * 禁用村民的基本转向逻辑。
     */
    @Override
    public boolean isControlledByLocalInstance()
    {
        return false;
    }

    /**
     * 计算调整后的伤害。
     * 对于非玩家实体，这实际上不会损坏装备。
     *
     * @param source 伤害来源
     * @param damage 伤害值
     * @return 装甲吸收后的伤害值。
     */
    public float calculateDamageAfterAbsorbs(DamageSource source, float damage)
    {
        float newDamage = this.getDamageAfterArmorAbsorb(source, damage);
        return this.getDamageAfterMagicAbsorb(source, newDamage);
    }

    @NotNull
    @Override
    public InteractionResult interactAt(final Player player, final Vec3 vec, final InteractionHand hand)
    {
        if (!player.level.isClientSide())
        {
            SoundUtils.playSoundAtCitizenWith(CompatibilityUtils.getWorldFromCitizen(this), this.blockPosition(), EventType.INTERACTION, this.getCitizenData());
        }

        return super.interactAt(player, vec, hand);
    }

    /**
     * 如果应运行较新的实体AI代码，则返回false。
     */
    @Override
    public boolean isNoAi()
    {
        return false;
    }

    /**
     * 设置所有市民的纹理并区分男性和女性。
     */
    public void setTexture()
    {
        if (!CompatibilityUtils.getWorldFromCitizen(this).isClientSide)
        {
            return;
        }

        texture = IModelTypeRegistry.getInstance().getModelType(getModelType()).getTexture(this);
        textureDirty = false;
    }

    /**
     * 获取市民数据视图。
     *
     * @return 视图。
     */
    public abstract ICitizenDataView getCitizenDataView();

    /**
     * 获取纹理的资源位置。
     *
     * @return 纹理的位置。
     */
    @NotNull
    public ResourceLocation getTexture()
    {
        if (texture == null
                || textureDirty
                || !texture.getPath().contains(getEntityData().get(DATA_STYLE))
                || !texture.getPath().contains(getEntityData().get(DATA_TEXTURE_SUFFIX)))
        {
            setTexture();
        }
        return texture;
    }

    /**
     * 设置纹理变脏。
     */
    public void setTextureDirty()
    {
        this.textureDirty = true;
    }

    /**
     * 获取分配给市民的模型。
     *
     * @return 模型。
     */
    public ResourceLocation getModelType()
    {
        return modelId;
    }

    /**
     * 目前我们不希望殖民地的居民有任何孩子。
     *
     * @return 孩子。
     */
    @Nullable
    @Override
    public AgeableMob getBreedOffspring(final ServerLevel world, final AgeableMob parent)
    {
        return null;
    }

    @Override
    protected void defineSynchedData()
    {
        super.defineSynchedData();
        entityData.define(DATA_TEXTURE_SUFFIX, "_b");
        entityData.define(DATA_TEXTURE, 0);
        entityData.define(DATA_LEVEL, 0);
        entityData.define(DATA_STYLE, "default");
        entityData.define(DATA_IS_FEMALE, 0);
        entityData.define(DATA_MODEL, ModModelTypes.SETTLER_ID.toString());
        entityData.define(DATA_RENDER_METADATA, "");
        entityData.define(DATA_IS_ASLEEP, false);
        entityData.define(DATA_IS_CHILD, false);
        entityData.define(DATA_BED_POS, new BlockPos(0, 0, 0));
        entityData.define(DATA_JOB, "");
    }

    /**
     * 获取市民是否为女性的getter方法。
     *
     * @return 如果为女性则为true。
     */
    public boolean isFemale()
    {
        return female;
    }

    /**
     * 设置性别。
     *
     * @param female 如果为女性则为true，否则为false。
     */
    public void setFemale(final boolean female)
    {
        this.female = female;
    }

    @NotNull
    @Override
    public AbstractAdvancedPathNavigate getNavigation()
    {
        if (this.pathNavigate == null)
        {
            this.pathNavigate = IPathNavigateRegistry.getInstance().getNavigateFor(this);
            this.navigation = pathNavigate;
            this.pathNavigate.setCanFloat(true);
            this.pathNavigate.setSwimSpeedFactor(CITIZEN_SWIM_BONUS);
            this.pathNavigate.getPathingOptions().setEnterDoors(true);
            this.pathNavigate.getPathingOptions().setCanOpenDoors(true);
            this.pathNavigate.setStuckHandler(PathingStuckHandler.createStuckHandler().withTeleportOnFullStuck().withTeleportSteps(5));
        }
        return pathNavigate;
    }

    /**
     * 为了解决许多尝试进入同一个门而被卡住的情况，允许暂时忽略实体碰撞。
     *
     * @param entityIn 要碰撞的实体
     */
    @Override
    public void push(@NotNull final Entity entityIn)
    {
        if ((collisionCounter += 2) > COLL_THRESHOLD)
        {
            if (collisionCounter > COLL_THRESHOLD * 2)
            {
                collisionCounter = 0;
            }

            return;
        }

        if (this.vehicle instanceof MinecoloniesMinecart)
        {
            return;
        }
        super.push(entityIn);
    }

    @Override
    public void onPlayerCollide(final Player player)
    {
        if (getCitizenData() == null)
        {
            super.onPlayerCollide(player);
            return;
        }

        final IJob<?> job = getCitizenData().getJob();
        if (job == null || !job.isGuard())
        {
            super.onPlayerCollide(player);
        }
        else
        {
            // 卫兵推开玩家
            push(player);
        }
    }

    @Override
    public boolean isPushable()
    {
        if (this.vehicle instanceof MinecoloniesMinecart)
        {
            return false;
        }
        return super.isPushable();
    }

    @Override
    public void aiStep()
    {
        super.aiStep();
        updateSwingTime();
        if (collisionCounter > 0)
        {
            collisionCounter--;
        }
    }

    @Override
    protected void tryAddSoulSpeed()
    {

    }

    @Override
    protected void removeSoulSpeed()
    {

    }

    @Override
    public boolean canSpawnSoulSpeedParticle()
    {
        return false;
    }

    /**
     * 设置市民的旋转。
     *
     * @param yaw   旋转的yaw。
     * @param pitch 旋转的pitch。
     */
    public void setOwnRotation(final float yaw, final float pitch)
    {
        this.setRot(yaw, pitch);
    }

    /**
     * 设置模型ID。
     *
     * @param model 要设置的模型。
     */
    public void setModelId(final ResourceLocation model)
    {
        this.modelId = model;
    }

    /**
     * 设置渲染元数据。
     *
     * @param renderMetadata 要设置的元数据。
     */
    public void setRenderMetadata(final String renderMetadata)
    {
        if (renderMetadata.equals(getRenderMetadata()))
        {
            return;
        }
        this.renderMetadata = renderMetadata;
        entityData.set(DATA_RENDER_METADATA, getRenderMetadata());
    }

    /**
     * 获取纹理ID。
     *
     * @return 纹理ID。
     */
    public int getTextureId()
    {
        return this.textureId;
    }

    /**
     * 设置纹理ID。
     *
     * @param textureId 纹理ID。
     */
    public void setTextureId(final int textureId)
    {
        this.textureId = textureId;
        entityData.set(DATA_TEXTURE, textureId);
    }

    /**
     * 获取渲染元数据。
     *
     * @return 元数据。
     */
    public String getRenderMetadata()
    {
        return renderMetadata;
    }

    /**
     * 获取市民的随机对象。
     *
     * @return 随机对象。
     */
    public RandomSource getRandom()
    {
        return random;
    }

    public int getOffsetTicks()
    {
        return this.tickCount + OFFSET_TICK_MULTIPLIER * this.getId();
    }

    @Override
    public boolean isBlocking()
    {
        return getUseItem().getItem() instanceof ShieldItem;
    }

    /**
     * 检查最近是否被打过。
     *
     * @return 打击次数。
     */
    public int getRecentlyHit()
    {
        return lastHurtByPlayerTime;
    }

    /**
     * 检查是否可以掉落战利品。
     *
     * @return 如果可以则为true。
     */
    public boolean checkCanDropLoot()
    {
        return shouldDropExperience();
    }

    /**
     * 获取市民的位置。
     *
     * @return 包含维度信息并且唯一的ILocation对象。
     */
    public abstract ILocation getLocation();

    /**
     * 检查工人是否在工作场地。如果不在，设置他的路径到该位置。
     *
     * @param site  应该走向的位置
     * @param range 检查范围
     * @return 如果工人在场地上则返回true，否则返回false。
     */
    public abstract boolean isWorkerAtSiteWithMove(@NotNull BlockPos site, int range);

    /**
     * 获取市民数据的getter方法。如果数据为空，则尝试从殖民地获取。
     *
     * @return 数据。
     */
    public abstract ICitizenData getCitizenData();

    /**
     * 返回该市民的库存。
     *
     * @return 该市民拥有的库存。
     */
    @NotNull
    public abstract InventoryCitizen getInventoryCitizen();

    @NotNull
    public abstract IItemHandler getItemHandlerCitizen();

    @NotNull
    public abstract DesiredActivity getDesiredActivity();

    /**
     * 设置市民实体的大小。
     *
     * @param width  宽度
     * @param height 高度
     */
    public abstract void setCitizensize(@NotNull float width, @NotNull float height);

    /**
     * 设置此实体是否为儿童。
     *
     * @param isChild 布尔值。
     */
    public abstract void setIsChild(boolean isChild);

    /**
     * 在逃离来自实体的攻击时播放移开声音。
     */
    public abstract void playMoveAwaySound();

    /**
     * 获取市民的路径代理。
     *
     * @return 代理。
     */
    public abstract IWalkToProxy getProxy();

    /**
     * 减少市民的饱和度以进行1个操作。
     */
    public abstract void decreaseSaturationForAction();

    /**
     * 减少市民的饱和度以进行1个连续的操作。
     */
    public abstract void decreaseSaturationForContinuousAction();

    /**
     * 与市民经验相关方法的处理程序。
     *
     * @return 处理程序的实例。
     */
    public abstract ICitizenExperienceHandler getCitizenExperienceHandler();

    /**
     * 与市民聊天相关方法的处理程序。
     *
     * @return 处理程序的实例。
     */
    public abstract ICitizenChatHandler getCitizenChatHandler();

    /**
     * 与市民状态相关方法的处理程序。
     *
     * @return 处理程序的实例。
     */
    public abstract ICitizenStatusHandler getCitizenStatusHandler();

    /**
     * 与市民物品相关方法的处理程序。
     *
     * @return 处理程序的实例。
     */
    public abstract ICitizenItemHandler getCitizenItemHandler();

    /**
     * 所有与库存相关方法的处理程序。
     *
     * @return 处理程序的实例。
     */
    public abstract ICitizenInventoryHandler getCitizenInventoryHandler();

    public abstract void setCitizenInventoryHandler(ICitizenInventoryHandler citizenInventoryHandler);

    /**
     * 所有与殖民地相关方法的处理程序。
     *
     * @return 处理程序的实例。
     */
    public abstract ICitizenColonyHandler getCitizenColonyHandler();

    public abstract void setCitizenColonyHandler(ICitizenColonyHandler citizenColonyHandler);

    /**
     * 所有与工作相关方法的处理程序。
     *
     * @return 处理程序的实例。
     */
    public abstract ICitizenJobHandler getCitizenJobHandler();

    /**
     * 处理所有与居民相关的方法的处理程序。
     *
     * @return 处理程序的实例。
     */
    public abstract ICitizenSleepHandler getCitizenSleepHandler();

    /**
     * 用于检查居民是否生病的处理程序。
     *
     * @return 处理程序的实例。
     */
    public abstract ICitizenDiseaseHandler getCitizenDiseaseHandler();

    public abstract void setCitizenDiseaseHandler(ICitizenDiseaseHandler citizenDiseaseHandler);

    /**
     * 通过考虑状态和工作任务来检查居民是否可以吃东西。
     *
     * @return 如果可以，则为true。
     */
    public abstract boolean isOkayToEat();

    /**
     * 检查居民是否可以被喂食。
     *
     * @return 如果可以，则为true。
     */
    public abstract boolean shouldBeFed();

    /**
     * 检查居民是否只是在工作中闲逛，现在可以吃东西。
     *
     * @return 如果可以，则为true。
     */
    public abstract boolean isIdlingAtJob();

    public abstract float getRotationYaw();

    public abstract float getRotationPitch();

    public abstract boolean isDead();

    public abstract void setCitizenSleepHandler(ICitizenSleepHandler citizenSleepHandler);

    public abstract void setCitizenJobHandler(ICitizenJobHandler citizenJobHandler);

    public abstract void setCitizenItemHandler(ICitizenItemHandler citizenItemHandler);

    public abstract void setCitizenChatHandler(ICitizenChatHandler citizenChatHandler);

    public abstract void setCitizenExperienceHandler(ICitizenExperienceHandler citizenExperienceHandler);

    /**
     * 获取居民是否正在逃离攻击者。
     *
     * @return 如果是，则为true。
     */
    public abstract boolean isCurrentlyFleeing();

    /**
     * 呼叫卫兵寻求对抗攻击者的帮助。
     *
     * @param attacker 攻击实体
     * @param guardHelpRange 我们搜索附近卫兵的平方距离
     */
    public abstract void callForHelp(final Entity attacker, final int guardHelpRange);

    /**
     * 设置逃离状态
     *
     * @param fleeing 如果逃离，则为true。
     */
    public abstract void setFleeingState(final boolean fleeing);

    /**
     * 公民姿势的设置器。
     *
     * @param pose 要设置的姿势。
     */
    public void updatePose(final Pose pose)
    {
        setPose(pose);
    }

    @Override
    public void detectEquipmentUpdates()
    {
        if (this.isEquipmentDirty)
        {
            this.isEquipmentDirty = false;
            List<Pair<EquipmentSlot, ItemStack>> list = Lists.newArrayListWithCapacity(6);

            list.add(new Pair<>(EquipmentSlot.CHEST, getItemBySlot(EquipmentSlot.CHEST)));
            list.add(new Pair<>(EquipmentSlot.FEET, getItemBySlot(EquipmentSlot.FEET)));
            list.add(new Pair<>(EquipmentSlot.HEAD, getItemBySlot(EquipmentSlot.HEAD)));
            list.add(new Pair<>(EquipmentSlot.LEGS, getItemBySlot(EquipmentSlot.LEGS)));
            list.add(new Pair<>(EquipmentSlot.OFFHAND, getItemBySlot(EquipmentSlot.OFFHAND)));
            list.add(new Pair<>(EquipmentSlot.MAINHAND, getItemBySlot(EquipmentSlot.MAINHAND)));
            ((ServerLevel) this.level).getChunkSource().broadcast(this, new ClientboundSetEquipmentPacket(this.getId(), list));
        }
    }

    @Override
    public void setItemSlot(final EquipmentSlot slot, @NotNull final ItemStack newItem)
    {
        if (!level.isClientSide)
        {
            final ItemStack previous = getItemBySlot(slot);
            if (!ItemStackUtils.compareItemStacksIgnoreStackSize(previous, newItem, false, true))
            {
                if (!previous.isEmpty())
                {
                    this.getAttributes().removeAttributeModifiers(previous.getAttributeModifiers(slot));
                }

                if (!newItem.isEmpty())
                {
                    this.getAttributes().addTransientAttributeModifiers(newItem.getAttributeModifiers(slot));
                }

                markEquipmentDirty();
            }
        }
        super.setItemSlot(slot, newItem);
    }

    /**
     * 标记装备状态为脏。
     */
    public void markEquipmentDirty()
    {
        this.isEquipmentDirty = true;
    }

    /**
     * 禁止受到液体的推动，以防止卡住
     *
     * @return
     */
    public boolean isPushedByFluid()
    {
        return false;
    }

    /**
     * 不允许气泡移动
     *
     * @param down
     */
    public void onInsideBubbleColumn(boolean down)
    {

    }

    /**
     * 在居民处排队一个声音。
     * @param soundEvent 要播放的声音事件。
     * @param pos 事件的位置。
     * @param length 事件的长度。
     * @param repetitions 播放的次数。
     */
    public abstract void queueSound(@NotNull final SoundEvent soundEvent, final BlockPos pos, final int length, final int repetitions);

    /**
     * 在居民处排队一个声音。
     * @param soundEvent 要播放的声音事件。
     * @param pos 事件的位置。
     * @param length 事件的长度。
     * @param repetitions 播放的次数。
     */
    public abstract void queueSound(@NotNull final SoundEvent soundEvent, final BlockPos pos, final int length, final int repetitions, final float volume, final float pitch);
}
