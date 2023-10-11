package com.minecolonies.api.util;

import com.google.common.collect.Lists;
import com.minecolonies.api.MinecoloniesAPIProxy;
import com.minecolonies.api.colony.IColonyManager;
import com.minecolonies.api.compatibility.Compatibility;
import com.minecolonies.api.crafting.ItemStorage;
import com.minecolonies.api.entity.citizen.AbstractEntityCitizen;
import com.minecolonies.api.items.ModItems;
import com.minecolonies.api.util.constant.IToolType;
import com.minecolonies.api.util.constant.ToolType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import net.minecraft.world.level.block.entity.FurnaceBlockEntity;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraftforge.common.ToolAction;
import net.minecraftforge.common.ToolActions;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.minecolonies.api.items.ModTags.fungi;
import static com.minecolonies.api.util.constant.Constants.*;

/**
 * 用于库存的实用方法。
 */
public final class ItemStackUtils
{
    /**
     * 表示1.10中的空物品栈的变量，用于便于更新到1.11。
     */
    public static final ItemStack EMPTY = ItemStack.EMPTY;

    /**
     * 用于检查物品栈是否为空的谓词。
     */
    @NotNull
    public static final Predicate<ItemStack> EMPTY_PREDICATE = ItemStackUtils::isEmpty;

    /**
     * 物品栈非空的谓词（不为空）的否定。
     */
    @NotNull
    public static final Predicate<ItemStack> NOT_EMPTY_PREDICATE = EMPTY_PREDICATE.negate();

    /**
     * 用于幸运附魔ID的复合标签。
     */
    private static final String NBT_TAG_ENCHANT_ID = "id";

    /**
     * 用于幸运附魔等级的复合标签。
     */
    private static final String NBT_TAG_ENCHANT_LEVEL = "lvl";

    /**
     * 幸运附魔的复合ID。
     */
    private static final int FORTUNE_ENCHANT_ID = 35;

    /**
     * 用于丝滑触控附魔的复合ID。
     */
    private static final int SILK_TOUCH_ENCHANT_ID = 33;

    /**
     * 如果此堆叠是标准食物物品（至少具有一些治疗和一些饱和度，不纯粹用于效果），则为true。
     */
    public static final Predicate<ItemStack> ISFOOD =
      stack -> ItemStackUtils.isNotEmpty(stack) && stack.isEdible() && stack.getItem().getFoodProperties() != null && stack.getItem().getFoodProperties().getNutrition() > 0
                 && stack.getItem().getFoodProperties().getSaturationModifier() > 0;

    /**
     * 描述在熔炉中有效的物品的谓词。
     */
    public static Predicate<ItemStack> IS_SMELTABLE;

    /**
     * 描述可以食用的食物的谓词（不是生的）。
     */
    public static Predicate<ItemStack> CAN_EAT;

    /**
     * 描述可烹饪物品的谓词。
     */
    public static Predicate<ItemStack> ISCOOKABLE;

    /**
     * 用于检查堆肥物品的谓词。
     */
    public static final Predicate<ItemStack> IS_COMPOST = stack -> !stack.isEmpty() && stack.getItem() == ModItems.compost;

    /**
     * 私有构造函数，隐藏了隐式构造函数。
     */
    private ItemStackUtils()
    {
        /*
         * 故意保留为空。
         */
    }

    /**
     * 从entityInfo对象获取实体。
     *
     * @param entityData 输入。
     * @param world      世界。
     * @return 输出对象或null。
     */
    @Nullable
    public static Entity getEntityFromEntityInfoOrNull(final CompoundTag entityData, final Level world)
    {
        try
        {
            final Optional<EntityType<?>> type = EntityType.by(entityData);
            if (type.isPresent())
            {
                final Entity entity = type.get().create(world);
                if (entity != null)
                {
                    entity.load(entityData);
                    return entity;
                }
            }
        }
        catch (final RuntimeException e)
        {
            Log.getLogger().info("无法恢复实体", e);
            return null;
        }
        return null;
    }

    /**
     * 如果需要，将实体添加到构建器中。
     *
     * @param entityData 实体信息对象。
     * @param world      世界。
     * @param placer     实体放置者。
     * @return 物品栈的列表。
     */
    public static List<ItemStorage> getListOfStackForEntityInfo(final CompoundTag entityData, final Level world, final Entity placer)
    {
        if (entityData != null)
        {
            final Entity entity = getEntityFromEntityInfoOrNull(entityData, world);
            if (entity != null)
            {
                if (EntityUtils.isEntityAtPosition(entity, world, placer))
                {
                    return Collections.emptyList();
                }
                return getListOfStackForEntity(entity, placer);
            }
        }
        return Collections.emptyList();
    }

    /**
     * 如果需要，将实体添加到构建器中。
     *
     * @param entityData 实体信息对象。
     * @param world      世界。
     * @param placer     抽象实体市民。
     * @return 物品栈的列表。
     */
    public static List<ItemStorage> getListOfStackForEntityInfo(final CompoundTag entityData, final Level world, final AbstractEntityCitizen placer)
    {
        if (placer != null)
        {
            return getListOfStackForEntityInfo(entityData, world, (Entity) placer);
        }

        return Lists.newArrayList();
    }

    /**
     * 如果需要，将实体添加到构建器中。
     *
     * @param entity 实体对象。
     * @param placer 实体放置者。
     * @return 物品栈的列表。
     */
    public static List<ItemStorage> getListOfStackForEntity(final Entity entity, final Entity placer)
    {
        if (entity != null)
        {
            final List<ItemStorage> request = new ArrayList<>();
            if (entity instanceof ItemFrame)
            {
                final ItemStack stack = ((ItemFrame) entity).getItem();
                if (!ItemStackUtils.isEmpty(stack))
                {
                    ItemStackUtils.setSize(stack, 1);
                    request.add(new ItemStorage(stack));
                }
                request.add(new ItemStorage(new ItemStack(Items.ITEM_FRAME, 1)));
            }
            else if (entity instanceof ArmorStand)
            {
                request.add(new ItemStorage(entity.getPickedResult(new EntityHitResult(placer))));
                entity.getArmorSlots().forEach(item -> request.add(new ItemStorage(item)));
                entity.getHandSlots().forEach(item -> request.add(new ItemStorage(item)));
            }

            /*
            todo: 直到Forge修复此问题之前不激活。
            else if (!(entity instanceof MobEntity))
            {
                request.add(new ItemStorage(entity.getPickedResult(new EntityRayTraceResult(placer))));
            }*/

            return request.stream().filter(stack -> !stack.getItemStack().isEmpty()).collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    /**
     * 验证工作人员库存中是否有具有可接受级别的工具。
     *
     * @param stack        要测试的物品栈。
     * @param toolType     需要的工具类型
     * @param minimalLevel 工具要查找的最小级别。
     * @param maximumLevel 工具要查找的最大级别。
     * @return 如果工具可接受则为true
     */
    public static boolean hasToolLevel(@Nullable final ItemStack stack, final IToolType toolType, final int minimalLevel, final int maximumLevel)
    {
        if (isEmpty(stack))
        {
            return false;
        }

        final int level = Compatibility.isTinkersWeapon(stack) ? Compatibility.getToolLevel(stack) : getMiningLevel(stack, toolType);
        return isTool(stack, toolType) && verifyToolLevel(stack, level, minimalLevel, maximumLevel);
    }
    /**
     * 包装方法，用于检查堆栈是否为空。用于轻松升级到1.11版本。
     *
     * @param stack 要检查的堆栈。
     * @return 当堆栈为空时为true，否则为false。
     */
    @NotNull
    public static Boolean isEmpty(@Nullable final ItemStack stack)
    {
        return stack == null || stack.isEmpty();
    }

    public static Boolean isNotEmpty(@Nullable final ItemStack stack)
    {
        return !isEmpty(stack);
    }

    /**
     * 计算物品作为特定类型工具的采矿等级。
     *
     * @param stack    要测试的堆栈。
     * @param toolType 工具类别。
     * @return 采矿等级的整数值，>= 0 表示可接受。
     */
    public static int getMiningLevel(@Nullable final ItemStack stack, @Nullable final IToolType toolType)
    {
        if (toolType == ToolType.NONE)
        {
            // 对于不关心的方块，空手最佳（0比1更好）
            return stack == null ? 0 : 1;
        }
        if (!Compatibility.getMiningLevelCompatibility(stack, toolType.toString()))
        {
            return -1;
        }
        if (!isTool(stack, toolType))
        {
            return -1;
        }

        if (toolType == ToolType.SWORD && Compatibility.isTinkersWeapon(stack))
        {
            return Compatibility.getToolLevel(stack);
        }
        else if (Compatibility.isTinkersTool(stack, toolType))
        {
            return Compatibility.getToolLevel(stack);
        }

        if (ToolType.HOE.equals(toolType))
        {
            if (stack.getItem() instanceof HoeItem)
            {
                final HoeItem hoeItem = (HoeItem) stack.getItem();
                return hoeItem.getTier().getLevel();
            }
        }
        else if (ToolType.SWORD.equals(toolType))
        {
            if (stack.getItem() instanceof SwordItem)
            {
                final SwordItem SwordItem = (SwordItem) stack.getItem();
                return SwordItem.getTier().getLevel();
            }
        }
        else if (ToolType.HELMET.equals(toolType)
                   || ToolType.BOOTS.equals(toolType)
                   || ToolType.CHESTPLATE.equals(toolType)
                   || ToolType.LEGGINGS.equals(toolType))
        {
            if (stack.getItem() instanceof ArmorItem)
            {
                final ArmorItem ArmorItem = (ArmorItem) stack.getItem();
                return getArmorLevel(ArmorItem.getMaterial());
            }
        }
        else if (stack.getItem() instanceof FishingRodItem)
        {
            return getFishingRodLevel(stack);
        }
        else if (toolType.equals(ToolType.SHEARS))
        {
            return stack.getItem() instanceof ShearsItem ? 0 : -1;
        }
        else if (!toolType.hasVariableMaterials())
        {
            // 我们需要至少1级的工具
            return 1;
        }
        else if (stack.getItem() instanceof TieredItem)
        {
            return ((TieredItem) stack.getItem()).getTier().getLevel();
        }
        return -1;
    }

    /**
     * 检查第一个堆栈是否比第二个堆栈更好的工具。
     *
     * @param stack1 要检查的第一个堆栈。
     * @param stack2 要比较的第二个堆栈。
     * @return 如果更好则为true，如果更差或其中一个不是工具则为false。
     */
    public static boolean isBetterTool(final ItemStack stack1, final ItemStack stack2)
    {
        for (final ToolType toolType : ToolType.values())
        {
            if (isTool(stack1, toolType) && isTool(stack2, toolType) && getMiningLevel(stack1, toolType) > getMiningLevel(stack2, toolType))
            {
                return true;
            }
        }
        return false;
    }

    /**
     * 检查此ItemStack是否可以用作指定类型的工具。
     *
     * @param itemStack 要检查的物品。
     * @param toolType  工具类型。
     * @return 如果物品可以使用，则为true，否则为false。
     */
    public static boolean isTool(@Nullable final ItemStack itemStack, final IToolType toolType)
    {
        if (isEmpty(itemStack))
        {
            return false;
        }

        if (ToolType.AXE.equals(toolType) && itemStack.canPerformAction(ToolActions.AXE_DIG))
        {
            return true;
        }

        if (ToolType.SHOVEL.equals(toolType) && itemStack.canPerformAction(ToolActions.SHOVEL_DIG))
        {
            return true;
        }

        if (ToolType.PICKAXE.equals(toolType) && itemStack.canPerformAction(ToolActions.PICKAXE_DIG))
        {
            return true;
        }

        if (ToolType.HOE.equals(toolType))
        {
            for (final ToolAction action : ToolActions.DEFAULT_HOE_ACTIONS)
            {
                if (!itemStack.canPerformAction(action))
                {
                    break;
                }
            }
            return true;
        }
        if (ToolType.BOW.equals(toolType))
        {
            return itemStack.getItem() instanceof BowItem;
        }
        if (ToolType.SWORD.equals(toolType))
        {
            return itemStack.canPerformAction(ToolActions.SWORD_SWEEP) || Compatibility.isTinkersWeapon(itemStack);
        }
        if (ToolType.FISHINGROD.equals(toolType))
        {
            return itemStack.getItem() instanceof FishingRodItem;
        }
        if (ToolType.SHEARS.equals(toolType))
        {
            return itemStack.getItem() instanceof ShearsItem;
        }
        if (ToolType.HELMET.equals(toolType))
        {
            return itemStack.getItem() instanceof ArmorItem;
        }
        if (ToolType.LEGGINGS.equals(toolType))
        {
            return itemStack.getItem() instanceof ArmorItem;
        }
        if (ToolType.CHESTPLATE.equals(toolType))
        {
            return itemStack.getItem() instanceof ArmorItem;
        }
        if (ToolType.BOOTS.equals(toolType))
        {
            return itemStack.getItem() instanceof ArmorItem;
        }
        if (ToolType.SHIELD.equals(toolType))
        {
            return itemStack.getItem() instanceof ShieldItem;
        }
        if (ToolType.FLINT_N_STEEL.equals(toolType))
        {
            return itemStack.getItem() instanceof FlintAndSteelItem;
        }

        return false;
    }

    /**
     * 验证物品是否具有适当的等级。
     *
     * @param itemStack    需要的工具类型。
     * @param toolLevel    工具等级。
     * @param minimalLevel 最低需要的等级。
     * @param maximumLevel 最大需要的等级（通常是工作小屋的等级）。
     * @return 如果工具可接受则为true。
     */
    public static boolean verifyToolLevel(@NotNull final ItemStack itemStack, final int toolLevel, final int minimalLevel, final int maximumLevel)
    {
        if (toolLevel < minimalLevel)
        {
            return false;
        }
        return (toolLevel + getMaxEnchantmentLevel(itemStack) <= maximumLevel);
    }

    /**
     * 检查一个ItemStack是否是用于结构放置的装饰性物品。
     *
     * @param stack 要测试的ItemStack。
     * @return 如果是则为true。
     */
    public static boolean isDecoration(final ItemStack stack)
    {
        final Item item = stack.getItem();
        return item == Items.ITEM_FRAME
                 || item == Items.ARMOR_STAND
                 || !Block.byItem(item).defaultBlockState().getMaterial().isSolid();
    }

    /*
    private static int getToolLevel(final String material)
    {
        if ("WOOD".equals(material)
              || "GOLD".equals(material))
        {
            return 0;
        }
        else if ("STONE".equals(material))
        {
            return 1;
        }
        else if ("IRON".equals(material))
        {
            return 2;
        }
        else if ("DIAMOND".equals(material))
        {
            return 3;
        }
        return -1;
    }
    */

    /**
     * 此例程将护甲的材料类型转换为请求系统的数字值。
     *
     * @param material 护甲的材料类型。
     * @return 护甲等级。
     */
    private static int getArmorLevel(final ArmorMaterial material)
    {
        final int damageReductionAmount = material.getDefenseForSlot(EquipmentSlot.CHEST);
        if (damageReductionAmount <= ArmorMaterials.LEATHER.getDefenseForSlot(EquipmentSlot.CHEST))
        {
            return 0;
        }
        else if (damageReductionAmount <= ArmorMaterials.GOLD.getDefenseForSlot(EquipmentSlot.CHEST) && material != ArmorMaterials.CHAIN)
        {
            return 1;
        }
        else if (damageReductionAmount <= ArmorMaterials.CHAIN.getDefenseForSlot(EquipmentSlot.CHEST))
        {
            return 2;
        }
        else if (damageReductionAmount <= ArmorMaterials.IRON.getDefenseForSlot(EquipmentSlot.CHEST))
        {
            return 3;
        }
        else if (damageReductionAmount <= ArmorMaterials.DIAMOND.getDefenseForSlot(EquipmentSlot.CHEST))
        {
            return 4;
        }

        return 5;
    }

    /**
     * 从可用的耐久性和附魔状态估算钓鱼竿等级。
     *
     * @param itemStack 要检查的工具。
     * @return 相等的工具等级。
     */
    private static int getFishingRodLevel(final ItemStack itemStack)
    {
        if (itemStack.getItem() == Items.FISHING_ROD)
        {
            return 1;
        }
        if (!itemStack.isDamageableItem())
        {
            return 5;
        }
        final int rodDurability = itemStack.getMaxDamage();
        if (rodDurability <= (Tiers.WOOD.getUses() + MinecoloniesAPIProxy.getInstance().getConfig().getServer().fishingRodDurabilityAdjustT1.get()))
        {
            return 1;
        }
        else if (rodDurability <= (Tiers.IRON.getUses() + MinecoloniesAPIProxy.getInstance().getConfig().getServer().fishingRodDurabilityAdjustT2.get()))
        {
            return 2;
        }
        return 3;
    }

    /**
     * 计算此工具具有的最大等级附魔。
     *
     * @param itemStack 要检查的工具。
     * @return 最大附魔等级。
     */
    public static int getMaxEnchantmentLevel(final ItemStack itemStack)
    {
        if (itemStack == null)
        {
            return 0;
        }
        int maxLevel = 0;
        if (itemStack != null)
        {
            final ListTag ListNBT = itemStack.getEnchantmentTags();

            if (ListNBT != null)
            {
                for (int j = 0; j < ListNBT.size(); ++j)
                {
                    final int level = ListNBT.getCompound(j).getShort("lvl");
                    maxLevel = level > maxLevel ? level : maxLevel;
                }
            }
        }
        return Math.max(maxLevel - 1, 0);
    }
    /**
     * 计算此工具的幸运等级。
     *
     * @param tool 要检查的工具。
     * @return 幸运等级。
     */
    public static int getFortuneOf(@Nullable final ItemStack tool)
    {
        if (tool == null)
        {
            return 0;
        }
        // 计算幸运附魔
        int fortune = 0;
        if (tool.isEnchanted())
        {
            final ListTag t = tool.getEnchantmentTags();

            for (int i = 0; i < t.size(); i++)
            {
                final int id = t.getCompound(i).getShort(NBT_TAG_ENCHANT_ID);
                if (id == FORTUNE_ENCHANT_ID)
                {
                    fortune = t.getCompound(i).getShort(NBT_TAG_ENCHANT_LEVEL);
                }
            }
        }
        return fortune;
    }

    /**
     * 检查物品是否用作武器。
     *
     * @param stack 要分析的物品栈。
     * @return 如果是工具或剑则返回true。
     */
    public static boolean doesItemServeAsWeapon(@NotNull final ItemStack stack)
    {
        return stack.getItem() instanceof SwordItem || stack.getItem() instanceof DiggerItem || Compatibility.isTinkersWeapon(stack);
    }

    /**
     * 分配一个包含工具等级的字符串。
     *
     * @param toolGrade 工具等级的数字
     * @return 与工具对应的字符串
     */
    public static String swapArmorGrade(final int toolGrade)
    {
        switch (toolGrade)
        {
            case 0:
                return "Leather";
            case 1:
                return "Gold";
            case 2:
                return "Chain";
            case 3:
                return "Iron";
            case 4:
                return "Diamond";
            default:
                return "Better than Diamond";
        }
    }

    /**
     * 分配一个包含护甲等级的字符串。
     *
     * @param toolGrade 护甲等级的数字
     * @return 与护甲对应的字符串
     */
    public static String swapToolGrade(final int toolGrade)
    {
        switch (toolGrade)
        {
            case 0:
                return "Wood or Gold";
            case 1:
                return "Stone";
            case 2:
                return "Iron";
            case 3:
                return "Diamond";
            default:
                return "Better than Diamond";
        }
    }

    /**
     * 检查两个ItemStack是否可以合并。
     *
     * @param existingStack 现有的物品栈。
     * @param mergingStack  要合并的物品栈
     * @return 当它们可以合并时返回true，否则返回false。
     */
    @NotNull
    public static Boolean areItemStacksMergable(final ItemStack existingStack, final ItemStack mergingStack)
    {
        if (!compareItemStacksIgnoreStackSize(existingStack, mergingStack))
        {
            return false;
        }

        return existingStack.getMaxStackSize() >= (getSize(existingStack) + getSize(mergingStack));
    }

    /**
     * 比较两个栈，忽略它们的堆叠大小。
     *
     * @param itemStack1 要比较的左栈。
     * @param itemStack2 要比较的右栈。
     * @return 当它们除了堆叠大小以外相等时返回true，否则返回false。
     */
    @NotNull
    public static Boolean compareItemStacksIgnoreStackSize(final ItemStack itemStack1, final ItemStack itemStack2)
    {
        return compareItemStacksIgnoreStackSize(itemStack1, itemStack2, true, true);
    }

    /**
     * 获取栈的大小。这是为了在1.10和1.11之间的兼容性而设计的
     *
     * @param stack 要获取大小的栈
     * @return 栈的大小
     */
    public static int getSize(@NotNull final ItemStack stack)
    {
        if (ItemStackUtils.isEmpty(stack))
        {
            return 0;
        }

        return stack.getCount();
    }

    /**
     * 获取栈的耐久性。
     *
     * @param stack 要获取大小的栈
     * @return 栈的耐久性
     */
    public static int getDurability(@NotNull final ItemStack stack)
    {
        if (ItemStackUtils.isEmpty(stack))
        {
            return 0;
        }

        return stack.getMaxDamage() - stack.getDamageValue();
    }

    /**
     * 比较两个栈，忽略它们的堆叠大小。
     *
     * @param itemStack1  要比较的左栈。
     * @param itemStack2  要比较的右栈。
     * @param matchDamage 设置为true以匹配损坏数据。
     * @param matchNBT    设置为true以匹配NBT
     * @return 当它们除了堆叠大小以外相等时返回true，否则返回false。
     */
    public static boolean compareItemStacksIgnoreStackSize(final ItemStack itemStack1, final ItemStack itemStack2, final boolean matchDamage, final boolean matchNBT)
    {
        return compareItemStacksIgnoreStackSize(itemStack1, itemStack2, matchDamage, matchNBT, false);
    }
    /**
     * 用于比较两个堆栈，忽略它们的堆叠大小的方法。
     *
     * @param itemStack1  要比较的左侧堆栈。
     * @param itemStack2  要比较的右侧堆栈。
     * @param matchDamage 设置为true以匹配损坏数据。
     * @param matchNBT    设置为true以匹配NBT
     * @param min         如果堆栈2的数量必须至少与堆栈1相同。
     * @return 当它们相等（除了堆叠大小）时为true，否则为false。
     */
    public static boolean compareItemStacksIgnoreStackSize(
            final ItemStack itemStack1,
            final ItemStack itemStack2,
            final boolean matchDamage,
            final boolean matchNBT,
            final boolean min)
    {
        if (isEmpty(itemStack1) && isEmpty(itemStack2))
        {
            return true;
        }

        if (isEmpty(itemStack1) != isEmpty(itemStack2))
        {
            return false;
        }

        if (itemStack1.getItem() == itemStack2.getItem() && (!matchDamage || itemStack1.getDamageValue() == itemStack2.getDamageValue()))
        {
            if (!matchNBT)
            {
                // 不比较NBT
                return true;
            }

            if (min && itemStack1.getCount() > itemStack2.getCount())
            {
                return false;
            }

            // 然后按照NBT进行排序
            if (itemStack1.hasTag() && itemStack2.hasTag())
            {
                CompoundTag nbt1 = itemStack1.getTag();
                CompoundTag nbt2 = itemStack2.getTag();

                for (String key : nbt1.getAllKeys())
                {
                    if (!matchDamage && key.equals("Damage"))
                    {
                        continue;
                    }
                    if (!nbt2.contains(key) || !nbt1.get(key).equals(nbt2.get(key)))
                    {
                        return false;
                    }
                }

                return nbt1.getAllKeys().size() == nbt2.getAllKeys().size();
            }
            else
            {
                return (!itemStack1.hasTag() || itemStack1.getTag().isEmpty())
                         && (!itemStack2.hasTag() || itemStack2.getTag().isEmpty());
            }
        }
        return false;
    }

    /**
     * 用于检查堆栈是否在堆栈列表中的方法。
     *
     * @param stacks 堆栈列表。
     * @param stack  堆栈。
     * @return 如果是，则为true。
     */
    public static boolean compareItemStackListIgnoreStackSize(final List<ItemStack> stacks, final ItemStack stack)
    {
        return compareItemStackListIgnoreStackSize(stacks, stack, true, true);
    }

    /**
     * 用于检查堆栈是否在堆栈列表中的方法。
     *
     * @param stacks      堆栈列表。
     * @param stack       堆栈。
     * @param matchDamage 如果损坏需要匹配。
     * @param matchNBT    如果NBT需要匹配。
     * @return 如果是，则为true。
     */
    public static boolean compareItemStackListIgnoreStackSize(final List<ItemStack> stacks, final ItemStack stack, final boolean matchDamage, final boolean matchNBT)
    {
        for (final ItemStack tempStack : stacks)
        {
            if (compareItemStacksIgnoreStackSize(tempStack, stack, matchDamage, matchNBT))
            {
                return true;
            }
        }
        return false;
    }

    /**
     * 设置堆栈的大小的方法。这是为了1.10和1.11之间的兼容性。
     *
     * @param stack 要设置大小的堆栈
     * @param size  堆栈的大小
     */
    public static void setSize(@NotNull final ItemStack stack, final int size)
    {
        stack.setCount(size);
    }

    /**
     * 增加或减少堆栈大小的方法。
     *
     * @param stack  要设置大小的堆栈
     * @param amount 要增加堆栈大小的量（负数值以减少）
     */
    public static void changeSize(@NotNull final ItemStack stack, final int amount)
    {
        stack.setCount(stack.getCount() + amount);
    }

    /**
     * 允许从NBT中读取ItemStack数据的更新方法。
     *
     * @param compound 要从中读取的复合物。
     * @return 存储在NBT数据中的ItemStack。
     */
    @NotNull
    public static ItemStack deserializeFromNBT(@NotNull final CompoundTag compound)
    {
        return ItemStack.of(compound);
    }

    /**
     * 使用Oredict检查堆栈是否为树苗类型的方法。
     *
     * @param stack 要检查的堆栈。
     * @return 如果是树苗，则为true。
     */
    public static boolean isStackSapling(@Nullable final ItemStack stack)
    {
        if (ItemStackUtils.isEmpty(stack))
        {
            return false;
        }

        return stack.is(ItemTags.SAPLINGS) || stack.is(fungi) || Compatibility.isDynamicTreeSapling(stack);
    }

    /**
     * 检查熔炉中是否有可熔炼物品而燃料为空的方法。
     *
     * @param entity 熔炉。
     * @return 如果是，则为true。
     */
    public static boolean hasSmeltableInFurnaceAndNoFuel(final FurnaceBlockEntity entity)
    {
        return !ItemStackUtils.isEmpty(entity.getItem(SMELTABLE_SLOT))
                 && ItemStackUtils.isEmpty(entity.getItem(FUEL_SLOT));
    }

    /**
     * 检查熔炉中是否既没有可熔炼物品也没有燃料的方法。
     *
     * @param entity 熔炉。
     * @return 如果是，则为true。
     */
    public static boolean hasNeitherFuelNorSmeltAble(final FurnaceBlockEntity entity)
    {
        return ItemStackUtils.isEmpty(entity.getItem(SMELTABLE_SLOT))
                 && ItemStackUtils.isEmpty(entity.getItem(FUEL_SLOT));
    }

    /**
     * 检查熔炉中是否有燃料而可熔炼物品为空的方法。
     *
     * @param entity 熔炉。
     * @return 如果是，则为true。
     */
    public static boolean hasFuelInFurnaceAndNoSmeltable(final FurnaceBlockEntity entity)
    {
        return ItemStackUtils.isEmpty(entity.getItem(SMELTABLE_SLOT))
                 && !ItemStackUtils.isEmpty(entity.getItem(FUEL_SLOT));
    }

    /**
     * 检查酿造台中是否有可酿造物品而燃料为空的方法。
     *
     * @param entity 酿造台。
     * @return 如果是，则为true。
     */
    public static boolean hasBrewableAndNoFuel(final BrewingStandBlockEntity entity)
    {
        return !ItemStackUtils.isEmpty(entity.getItem(INGREDIENT_SLOT))
                 && ItemStackUtils.isEmpty(entity.getItem(BREWING_FUEL_SLOT));
    }

    /**
     * 检查酿造台中既没有可酿造物品也没有燃料的方法。
     *
     * @param entity 酿造台。
     * @return 如果是，则为true。
     */
    public static boolean hasNeitherFuelNorBrewable(final BrewingStandBlockEntity entity)
    {
        return ItemStackUtils.isEmpty(entity.getItem(INGREDIENT_SLOT))
                 && ItemStackUtils.isEmpty(entity.getItem(BREWING_FUEL_SLOT));
    }

    /**
     * 检查酿造台中是否有燃料而可酿造物品为空的方法。
     *
     * @param entity 酿造台。
     * @return 如果是，则为true。
     */
    public static boolean hasFuelAndNoBrewable(final BrewingStandBlockEntity entity)
    {
        return ItemStackUtils.isEmpty(entity.getItem(INGREDIENT_SLOT))
                 && !ItemStackUtils.isEmpty(entity.getItem(BREWING_FUEL_SLOT));
    }

    /**
     * 将带有NBT的项目字符串转换为ItemStack的方法。
     *
     * @param itemData 例如：minecraft:potion{Potion=minecraft:water}
     * @return 具有任何定义的NBT的堆栈
     */
    public static ItemStack idToItemStack(final String itemData)
    {
        String itemId = itemData;
        final int tagIndex = itemId.indexOf("{");
        final String tag = tagIndex > 0 ? itemId.substring(tagIndex) : null;
        itemId = tagIndex > 0 ? itemId.substring(0, tagIndex) : itemId;
        String[] split = itemId.split(":");
        if (split.length != 2)
        {
            if (split.length == 1)
            {
                final String[] tempArray = {"minecraft", split[0]};
                split = tempArray;
            }
            else
            {
                Log.getLogger().error("无法解析项目定义：" + itemData);
            }
        }
        final Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(split[0], split[1]));
        final ItemStack stack = new ItemStack(item);
        if (tag != null)
        {
            try
            {
                stack.setTag(TagParser.parseTag(tag));
            }
            catch (CommandSyntaxException e1)
            {
                //无法解析标签，丢弃它们。
                Log.getLogger().error("无法解析项目定义：" + itemData);
            }
        }
        if (stack.isEmpty())
        {
            Log.getLogger().warn("解析的项目定义返回为空：" + itemData);
        }
        return stack;
    }

    /**
     * 获取游戏中所有基本项目的列表，以及玩家背包中的任何额外项目（允许具有自定义NBT的项目，例如DO块或染色护甲）的方法。
     *
     * @param player 要检查背包的玩家。
     * @return 项目集。
     */
    public static Set<ItemStack> allItemsPlusInventory(@NotNull final Player player)
    {
        //首先获取所有已知项目
        final Set<ItemStorage> allItems = new HashSet<>(IColonyManager.getInstance().getCompatibilityManager().getSetOfAllItems());

        //加上玩家背包中尚未列出的所有项目（添加具有额外NBT的项目）
        for (final ItemStack stack : player.getInventory().items)
        {
            if (stack.isEmpty())
            {
                continue;
            }

            final ItemStack pristine = stack.copy();
            pristine.setCount(1);
            if (stack.isDamageableItem() && stack.isDamaged())
            {
                pristine.setDamageValue(0);
                //如果项目尚未在集合中，我们只想存储一个原始的！
            }
            allItems.add(new ItemStorage(pristine, true));
        }

        return allItems.stream().map(ItemStorage::getItemStack).collect(Collectors.toSet());
    }
}

