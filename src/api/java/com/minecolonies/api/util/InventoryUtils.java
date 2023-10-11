package com.minecolonies.api.util;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.minecolonies.api.colony.buildings.IBuilding;
import com.minecolonies.api.crafting.ItemStorage;
import com.minecolonies.api.tileentities.TileEntityColonyBuilding;
import com.minecolonies.api.tileentities.TileEntityRack;
import com.minecolonies.api.util.constant.IToolType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.items.IItemHandler;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.IntStream;

import static com.minecolonies.api.util.constant.TranslationConstants.MESSAGE_INFO_PLAYER_INVENTORY_FULL_HOTBAR_INSERT;
import static net.minecraftforge.items.CapabilityItemHandler.ITEM_HANDLER_CAPABILITY;

/**
 * 用于库存的实用方法。
 */
public class InventoryUtils
{
    /**
     * 用于在世界中生成物品的若干数值。
     */
    private static final double SPAWN_MODIFIER    = 0.8D;
    private static final double SPAWN_ADDITION    = 0.1D;
    private static final int    MAX_RANDOM_SPAWN  = 21;
    private static final int    MIN_RANDOM_SPAWN  = 10;
    private static final double MOTION_MULTIPLIER = 0.05000000074505806D;
    private static final double MOTION_Y_MIN      = 0.20000000298023224D;

    /**
     * 私有构造函数以隐藏隐式构造函数。
     */
    private InventoryUtils()
    {
        /*
         * 故意留空。
         */
    }

    /**
     * 过滤物品处理器（IItemHandler）中的物品列表，使用{@link #compareItems(ItemStack, Item)}来匹配堆栈，使用MetaData和{@link #getItemFromBlock(Block)}作为谓词的参数。
     *
     * @param itemHandler 物品处理器
     * @param block       要过滤的方块
     * @return 物品堆栈列表
     */
    @NotNull
    public static List<ItemStack> filterItemHandler(@NotNull final IItemHandler itemHandler, @NotNull final Block block)
    {
        return filterItemHandler(itemHandler, (ItemStack stack) -> compareItems(stack, getItemFromBlock(block)));
    }

    /**
     * 在物品处理器（IItemHandler）中过滤匹配给定谓词的物品列表。
     *
     * @param itemHandler                 要获取物品的IItemHandler
     * @param itemStackSelectionPredicate 用于匹配堆栈的谓词
     * @return 匹配给定谓词的物品堆栈列表
     */
    @NotNull
    public static List<ItemStack> filterItemHandler(@NotNull final IItemHandler itemHandler, @NotNull final Predicate<ItemStack> itemStackSelectionPredicate)
    {
        @NotNull final ArrayList<ItemStack> filtered = new ArrayList<>();
        //检查每个itemHandler槽
        for (int slot = 0; slot < itemHandler.getSlots(); slot++)
        {
            final ItemStack stack = itemHandler.getStackInSlot(slot);
            if (!ItemStackUtils.isEmpty(stack) && itemStackSelectionPredicate.test(stack))
            {
                filtered.add(stack);
            }
        }
        return filtered;
    }

    /**
     * 比较物品堆栈中的物品是否与给定物品相等。
     *
     * @param itemStack  要检查的物品堆栈
     * @param targetItem 要检查的物品
     * @return 如果物品堆栈中的物品等于目标物品，则返回true
     */
    private static boolean compareItems(@Nullable final ItemStack itemStack, final Item targetItem)
    {
        return !ItemStackUtils.isEmpty(itemStack) && itemStack.getItem() == targetItem;
    }

    /**
     * 将方块转换为其物品形式，以便进行比较。
     *
     * @param block 要转换的方块
     * @return 注册表中的物品
     */
    public static Item getItemFromBlock(final Block block)
    {
        return Item.byBlock(block);
    }

    /**
     * 过滤物品处理器（IItemHandler）中的物品列表，使用{@link #compareItems(ItemStack, Item)}来匹配目标物品和itemDamage作为参数。
     *
     * @param itemHandler 要获取物品的库存
     * @param targetItem  要查找的物品
     * @return 包含给定物品的库存中的物品堆栈列表
     */
    @NotNull
    public static List<ItemStack> filterItemHandler(@NotNull final IItemHandler itemHandler, @NotNull final Item targetItem)
    {
        return filterItemHandler(itemHandler, (ItemStack stack) -> compareItems(stack, targetItem));
    }

    /**
     * 返回在IItemHandler中第一次出现的方块的索引。
     *
     * @param itemHandler 要检查的IItemHandler
     * @param block       要查找的方块
     * @return 第一次出现的方块的索引
     */
    public static int findFirstSlotInItemHandlerWith(@NotNull final IItemHandler itemHandler, @NotNull final Block block)
    {
        return findFirstSlotInItemHandlerWith(itemHandler, getItemFromBlock(block));
    }

    /**
     * 返回在IItemHandler中第一次出现具有给定ItemDamage的物品的索引。
     *
     * @param itemHandler 要检查的IItemHandler
     * @param targetItem  要查找的物品
     * @return 第一次出现的物品的索引
     */
    public static int findFirstSlotInItemHandlerWith(@NotNull final IItemHandler itemHandler, @NotNull final Item targetItem)
    {
        return findFirstSlotInItemHandlerWith(itemHandler, (ItemStack stack) -> compareItems(stack, targetItem));
    }

    /**
     * 返回在IItemHandler中第一次出现与给定谓词匹配的ItemStack的索引。
     *
     * @param itemHandler                 要检查的ItemHandler
     * @param itemStackSelectionPredicate 用于匹配的谓词
     * @return 第一次出现的索引
     */
    public static int findFirstSlotInItemHandlerWith(@NotNull final IItemHandler itemHandler, @NotNull final Predicate<ItemStack> itemStackSelectionPredicate)
    {
        for (int slot = 0; slot < itemHandler.getSlots(); slot++)
        {
            if (itemStackSelectionPredicate.test(itemHandler.getStackInSlot(slot)))
            {
                return slot;
            }
        }

        return -1;
        //TODO: 稍后增强合同以删除对slot := -1的比较
        //throw new IllegalStateException("Item "+targetItem.getTranslationKey() + " not found in ItemHandler!");
    }

    /**
     * 减少物品处理器中特定堆栈的数量。
     *
     * @param itemHandler                 处理器
     * @param itemStackSelectionPredicate 谓词
     * @return 如果成功则返回true
     */
    public static boolean shrinkItemCountInItemHandler(final IItemHandler itemHandler, @NotNull final Predicate<ItemStack> itemStackSelectionPredicate)
    {
        final Predicate<ItemStack> predicate = ItemStackUtils.NOT_EMPTY_PREDICATE.and(itemStackSelectionPredicate);
        for (int slot = 0; slot < itemHandler.getSlots(); slot++)
        {
            if (predicate.test(itemHandler.getStackInSlot(slot)))
            {
                itemHandler.getStackInSlot(slot).shrink(1);
                return true;
            }
        }
        return false;
    }

    /**
     * 返回在IItemHandler中匹配给定谓词的所有ItemStack的索引。
     *
     * @param itemHandler                 要检查的ItemHandler
     * @param itemStackSelectionPredicate 用于匹配的谓词
     * @return 匹配项的索引列表
     */
    public static List<Integer> findAllSlotsInItemHandlerWith(@NotNull final IItemHandler itemHandler, @NotNull final Predicate<ItemStack> itemStackSelectionPredicate)
    {
        final List<Integer> returnList = new ArrayList<>();
        for (int slot = 0; slot < itemHandler.getSlots(); slot++)
        {
            if (itemStackSelectionPredicate.test(itemHandler.getStackInSlot(slot)))
            {
                returnList.add(slot);
            }
        }

        return returnList;
    }

    /**
     * 返回IItemHandler中的物品数量。
     *
     * @param itemHandler 要扫描的IItemHandler
     * @param block       要计数的方块
     * @return 匹配给定方块和ItemDamage的堆栈的数量
     */
    public static int getItemCountInItemHandler(@Nullable final IItemHandler itemHandler, @NotNull final Block block)
    {
        if (itemHandler == null)
        {
            Log.getLogger().error("这不应该发生，请通知开发人员！", new Exception("getItemCountInItemHandler收到了一个空的itemHandler"));
        }
        return itemHandler == null ? 0 : getItemCountInItemHandler(itemHandler, getItemFromBlock(block));
    }

    /**
     * 返回IItemHandler中的物品数量。
     *
     * @param itemHandler 要扫描的IItemHandler
     * @param targetItem  要计数的物品
     * @return 匹配给定物品和ItemDamage的堆栈的数量
     */
    public static int getItemCountInItemHandler(@Nullable final IItemHandler itemHandler, @NotNull final Item targetItem)
    {
        if (itemHandler == null)
        {
            Log.getLogger().error("这不应该发生，请通知开发人员！", new Exception("getItemCountInItemHandler收到了一个空的itemHandler"));
        }
        return itemHandler == null ? 0 : getItemCountInItemHandler(itemHandler, (ItemStack stack) -> compareItems(stack, targetItem));
    }

    /**
     * 返回IItemHandler中的物品数量。
     *
     * @param itemHandler                 要扫描的IItemHandler
     * @param itemStackSelectionPredicate 用于选择要计数的堆栈的谓词
     * @return 匹配给定谓词的堆栈的数量
     */
    public static int getItemCountInItemHandler(@Nullable final IItemHandler itemHandler, @NotNull final Predicate<ItemStack> itemStackSelectionPredicate)
    {
        if (itemHandler == null)
        {
            Log.getLogger().error("这不应该发生，请通知开发人员！", new Exception("getItemCountInItemHandler收到了一个空的itemHandler"));
        }
        return itemHandler == null ? 0 : filterItemHandler(itemHandler, itemStackSelectionPredicate).stream().mapToInt(ItemStackUtils::getSize).sum();
    }

    public static int getItemCountInItemHandlers(@Nullable final Collection<IItemHandler> itemHandlers, @NotNull final Predicate<ItemStack> itemStackPredicate)
    {
        int count = 0;
        if (itemHandlers != null)
        {
            Set<ItemStack> itemSet = new HashSet<>();
            for (final IItemHandler handler : itemHandlers)
            {
                itemSet.addAll(filterItemHandler(handler, itemStackPredicate));
            }

            for (final ItemStack stack : itemSet)
            {
                count += stack.getCount();
            }
        }
        return count;
    }

    /**
     * 检查IItemHandler中是否存在方块。通过{@link #getItemCountInItemHandler(IItemHandler, Block)} > 0进行检查。
     *
     * @param itemHandler 要扫描的IItemHandler
     * @param block       要计数的方块
     * @return 如果在IItemHandler中存在则返回true，否则返回false
     */
    public static boolean hasItemInItemHandler(@Nullable final IItemHandler itemHandler, @NotNull final Block block)
    {
        if (itemHandler == null)
        {
            Log.getLogger().error("这不应该发生，请通知开发人员！", new Exception("hasItemInItemHandler收到了一个空的itemHandler"));
        }
        return itemHandler != null && hasItemInItemHandler(itemHandler, getItemFromBlock(block));
    }

    /**
     * 检查IItemHandler中是否存在物品。通过{@link #getItemCountInItemHandler(IItemHandler, Item)} > 0进行检查。
     *
     * @param itemHandler 要扫描的IItemHandler
     * @param item        要计数的物品
     * @return 如果在IItemHandler中存在则返回true，否则返回false
     */
    public static boolean hasItemInItemHandler(@Nullable final IItemHandler itemHandler, @NotNull final Item item)
    {
        if (itemHandler == null)
        {
            Log.getLogger().error("这不应该发生，请通知开发人员！", new Exception("hasItemInItemHandler收到了一个空的itemHandler"));
        }
        return itemHandler != null && hasItemInItemHandler(itemHandler, (ItemStack stack) -> compareItems(stack, item));
    }

    /**
     * 检查IItemHandler中是否存在物品。通过{@link InventoryUtils#getItemCountInItemHandler(IItemHandler, Predicate)} > 0进行检查。
     *
     * @param itemHandler                 要扫描的IItemHandler
     * @param itemStackSelectionPredicate 用于匹配ItemStack的谓词
     * @return 如果在IItemHandler中存在则返回true，否则返回false
     */
    public static boolean hasItemInItemHandler(@Nullable final IItemHandler itemHandler, @NotNull final Predicate<ItemStack> itemStackSelectionPredicate)
    {
        if (itemHandler == null)
        {
            Log.getLogger().error("这不应该发生，请通知开发人员！", new Exception("hasItemInItemHandler收到了一个空的itemHandler"));
        }
        return itemHandler != null && findFirstSlotInItemHandlerNotEmptyWith(itemHandler, itemStackSelectionPredicate) > -1;
    }

    /**
     * 返回是否IItemHandler已满。
     *
     * @param itemHandler 要检查的IItemHandler
     * @return 如果IItemHandler已满则返回true，否则返回false
     */
    public static boolean isItemHandlerFull(@Nullable final IItemHandler itemHandler)
    {
        if (itemHandler == null)
        {
            Log.getLogger().error("这不应该发生，请通知开发人员！", new Exception("hasItemInItemHandler收到了一个空的itemHandler"));
        }
        return itemHandler == null || getFirstOpenSlotFromItemHandler(itemHandler) == -1;
    }

    /**
     * 返回IItemHandler中的第一个开放槽。
     *
     * @param itemHandler 要检查的IItemHandler
     * @return 槽位索引，如果未找到则返回-1
     */
    public static int getFirstOpenSlotFromItemHandler(@Nullable final IItemHandler itemHandler)
    {
        if (itemHandler == null)
        {
            Log.getLogger().error("这不应该发生，请通知开发人员！", new Exception("hasItemInItemHandler收到了一个空的itemHandler"));
            return -1;
        }
        //测试使用两个不同的ItemStack在模拟模式下插入。
        return IntStream.range(0, itemHandler.getSlots())
                .filter(slot -> ItemStackUtils.isEmpty(itemHandler.getStackInSlot(slot)))
                .findFirst()
                .orElse(-1);
    }

    /**
     * 计算库存中的开放槽数量。
     *
     * @param itemHandler 库存
     * @return 开放槽数量
     */
    public static long openSlotCount(@Nullable final IItemHandler itemHandler)
    {
        if (itemHandler == null)
        {
            Log.getLogger().error("这不应该发生，请通知开发人员！", new Exception("hasItemInItemHandler收到了一个空的itemHandler"));
            return 0;
        }
        return IntStream.range(0, itemHandler.getSlots())
                .filter(slot -> ItemStackUtils.isEmpty(itemHandler.getStackInSlot(slot)))
                .count();
    }

    /**
     * 强制将ItemStack添加到IItemHandler中。
     *
     * @param itemHandler              要添加ItemStack的IItemHandler
     * @param itemStack                要添加的ItemStack
     * @param itemStackToKeepPredicate 确定要保留在库存中的ItemStack的谓词。返回false以替换。
     * @return 已替换的ItemStack，如果没有替换则返回null
     */
    @Nullable
    public static ItemStack forceItemStackToItemHandler
(
      @NotNull final IItemHandler itemHandler,
      @NotNull final ItemStack itemStack,
      @NotNull final Predicate<ItemStack> itemStackToKeepPredicate)
    {
        final ItemStack standardInsertionResult = addItemStackToItemHandlerWithResult(itemHandler, itemStack);

        if (!ItemStackUtils.isEmpty(standardInsertionResult))
        {
            for (int i = 0; i < itemHandler.getSlots() && !ItemStackUtils.isEmpty(standardInsertionResult); i++)
            {
                final ItemStack localStack = itemHandler.getStackInSlot(i);
                if (ItemStackUtils.isEmpty(localStack) || !itemStackToKeepPredicate.test(localStack))
                {
                    final ItemStack removedStack = itemHandler.extractItem(i, Integer.MAX_VALUE, false);
                    final ItemStack localInsertionResult = itemHandler.insertItem(i, standardInsertionResult, false);

                    if (ItemStackUtils.isEmpty(localInsertionResult))
                    {
                        //Insertion successful. Returning the extracted stack.
                        return removedStack.copy();
                    }
                    else
                    {
                        //Insertion failed. The inserted stack was not accepted completely. Undo the extraction.
                        itemHandler.insertItem(i, removedStack, false);
                    }
                }
            }
        }
        return standardInsertionResult;
    }
    /**
     * 返回存储中物品栈的数量。这等于 {@link #getItemHandlerAsList(IItemHandler)}<code>.length();</code>。
     *
     * @param itemHandler 要计算物品栈数量的 {@link IItemHandler}。
     * @return {@link IItemHandler} 中物品栈的数量。
     */
    public static int getAmountOfStacksInItemHandler(@NotNull final IItemHandler itemHandler)
    {
        return getItemHandlerAsList(itemHandler).size();
    }

    /**
     * 返回一个 {@link IItemHandler} 作为物品栈列表。
     *
     * @param itemHandler 要转换的库存。
     * @return 物品栈列表。
     */
    @NotNull
    public static List<ItemStack> getItemHandlerAsList(@NotNull final IItemHandler itemHandler)
    {
        return filterItemHandler(itemHandler, (ItemStack stack) -> true);
    }

    /**
     * 过滤一个 {@link ICapabilityProvider} 中的物品列表，使用 {@link #compareItems(ItemStack, Item)} 进行匹配。
     * 使用 MetaData 和 {@link #getItemFromBlock(Block)} 作为 Predicate 的参数。
     *
     * @param provider 要过滤的提供者
     * @param block    要过滤的方块
     * @return 物品栈列表
     */
    @NotNull
    public static List<ItemStack> filterProvider(@NotNull final ICapabilityProvider provider, final Block block)
    {
        return filterProvider(provider, (ItemStack stack) -> compareItems(stack, getItemFromBlock(block)));
    }

    /**
     * 在 {@link ICapabilityProvider} 中过滤一个物品列表，匹配给定的谓词。
     *
     * @param provider                    用于获取物品的 {@link ICapabilityProvider}。
     * @param itemStackSelectionPredicate 用于匹配物品栈的谓词。
     * @return 匹配给定谓词的物品栈列表。
     */
    @NotNull
    public static List<ItemStack> filterProvider(@NotNull final ICapabilityProvider provider, @NotNull final Predicate<ItemStack> itemStackSelectionPredicate)
    {
        return getFromProviderForAllSides(provider, itemStackSelectionPredicate);
    }

    /**
     * 处理 {@link ICapabilityProvider} 的所有 {@link Direction} 的给定谓词，包括内部的一个（传递 null 作为参数）。
     *
     * @param provider  要处理的提供者
     * @param predicate 用于匹配每个方向的 {@link IItemHandler} 中的物品栈的谓词。
     * @return 组合的 {@link List}<{@link ItemStack}>，就像给定谓词在给定提供者的所有 {@link IItemHandler} 的所有物品栈上都被调用一样。
     */
    @NotNull
    private static List<ItemStack> getFromProviderForAllSides(@NotNull final ICapabilityProvider provider, @NotNull final Predicate<ItemStack> predicate)
    {
        final Set<ItemStack> combinedList = new HashSet<>();

        for (final IItemHandler handler : getItemHandlersFromProvider(provider))
        {
            if (handler != null)
            {
                combinedList.addAll(filterItemHandler(handler, predicate));
            }
        }
        return new ArrayList<>(combinedList);
    }

    /**
     * 从给定提供者获取所有的 IItemHandlers 的方法。
     *
     * @param provider 要获取 IItemHandlers 的提供者。
     * @return 一个包含所有独特的 IItemHandlers 的列表。
     */
    @NotNull
    public static Set<IItemHandler> getItemHandlersFromProvider(@NotNull final ICapabilityProvider provider)
    {
        final Set<IItemHandler> handlerList = new HashSet<>();
        for (final Direction side : Direction.values())
        {
            provider.getCapability(ITEM_HANDLER_CAPABILITY, side).ifPresent(handlerList::add);
        }
        provider.getCapability(ITEM_HANDLER_CAPABILITY, null).ifPresent(handlerList::add);
        return handlerList;
    }

    /**
     * 过滤一个物品列表，使用 {@link #compareItems(ItemStack, Item)} 进行匹配，使用 targetItem 和 itemDamage 作为参数，在 {@link ICapabilityProvider} 中。
     *
     * @param provider   要获取物品的提供者
     * @param targetItem 要查找的物品
     * @return 包含给定物品的库存中的物品栈列表
     */
    @NotNull
    public static List<ItemStack> filterProvider(@NotNull final ICapabilityProvider provider, @Nullable final Item targetItem)
    {
        return filterProvider(provider, (ItemStack stack) -> compareItems(stack, targetItem));
    }

    /**
     * 返回 {@link ICapabilityProvider} 中第一个出现的方块的索引。
     *
     * @param provider {@link ICapabilityProvider} 要检查。
     * @param block    要查找的方块。
     * @return 第一个出现的方块的索引。
     */
    public static int findFirstSlotInProviderWith(@NotNull final ICapabilityProvider provider, final Block block)
    {
        return findFirstSlotInProviderWith(provider, getItemFromBlock(block));
    }

    /**
     * 返回 {@link ICapabilityProvider} 中具有给定 ItemDamage 的 Item 的第一个出现的索引。
     *
     * @param provider   要检查的 {@link ICapabilityProvider}。
     * @param targetItem 要查找的物品。
     * @return 第一个出现的索引。
     */
    public static int findFirstSlotInProviderWith(@NotNull final ICapabilityProvider provider, final Item targetItem)
    {
        return findFirstSlotInProviderNotEmptyWith(provider, (ItemStack stack) -> compareItems(stack, targetItem));
    }

    /**
     * 返回一个包含在提供者中与提供的谓词匹配的所有物品处理程序和槽位的映射。在 {@link ICapabilityProvider} 中使用给定谓词。
     *
     * @param provider                    要检查的提供者
     * @param itemStackSelectionPredicate 用于匹配的谓词。
     * @return 第一个出现的索引。
     */
    public static Map<IItemHandler, List<Integer>> findAllSlotsInProviderWith(@NotNull final ICapabilityProvider provider, final Predicate<ItemStack> itemStackSelectionPredicate)
    {
        final Map<IItemHandler, List<Integer>> map = new HashMap<>();
        for (final IItemHandler handler : getItemHandlersFromProvider(provider))
        {
            final List<Integer> tempList = findAllSlotsInItemHandlerWith(handler, itemStackSelectionPredicate);
            if (!tempList.isEmpty())
            {
                map.put(handler, tempList);
            }
        }

        return map;
    }

    /**
     * 返回 {@link ICapabilityProvider} 中第一个不为空的物品栈匹配给定谓词的索引。
     *
     * @param provider                    要检查的提供者
     * @param itemStackSelectionPredicate 用于匹配的谓词。
     * @return 第一个出现的索引。
     */
    public static int findFirstSlotInProviderNotEmptyWith(@NotNull final ICapabilityProvider provider, final Predicate<ItemStack> itemStackSelectionPredicate)
    {
        for (final IItemHandler handler : getItemHandlersFromProvider(provider))
        {
            final int foundSlot = findFirstSlotInItemHandlerNotEmptyWith(handler, itemStackSelectionPredicate);
            if (foundSlot > -1)
            {
                return foundSlot;
            }
        }

        return -1;
    }

    /**
     * 返回 {@link ICapabilityProvider} 中第一个不为空的物品栈匹配给定谓词列表的索引。
     *
     * @param provider                    要检查的提供者
     * @param itemStackSelectionPredicate 要匹配的谓词列表。
     * @return 第一个出现的索引。
     */
    public static int findFirstSlotInProviderNotEmptyWith(@NotNull final ICapabilityProvider provider, final List<Predicate<ItemStack>> itemStackSelectionPredicate)
    {
        for (final IItemHandler handler : getItemHandlersFromProvider(provider))
        {
            final int foundSlot = findFirstSlotInItemHandlerNotEmptyWith(handler, itemStackSelectionPredicate);
            if (foundSlot > -1)
            {
                return foundSlot;
            }
        }

        return -1;
    }

    /**
     * 返回 {@link IItemHandler} 中第一个不为空的物品栈匹配给定谓词列表的索引。同时应用非空检查。
     *
     * @param itemHandler                 要检查的物品处理程序
     * @param itemStackSelectionPredicate 要匹配的谓词列表。
     * @return 第一个出现的索引。
     */
    private static int findFirstSlotInItemHandlerNotEmptyWith(final IItemHandler itemHandler, final List<Predicate<ItemStack>> itemStackSelectionPredicate)
    {
        for (final Predicate<ItemStack> predicate : itemStackSelectionPredicate)
        {
            for (int slot = 0; slot < itemHandler.getSlots(); slot++)
            {
                if (ItemStackUtils.NOT_EMPTY_PREDICATE.and(predicate).test(itemHandler.getStackInSlot(slot)))
                {
                    return slot;
                }
            }
        }

        return -1;
    }

    /**
     * 返回 {@link IItemHandler} 中第一个不为空的物品栈匹配给定谓词的索引。同时应用非空检查。
     *
     * @param itemHandler                 要检查的物品处理程序
     * @param itemStackSelectionPredicate 要匹配的谓词。
     * @return 第一个出现的索引。
     */
    public static int findFirstSlotInItemHandlerNotEmptyWith(@NotNull final IItemHandler itemHandler, @NotNull final Predicate<ItemStack> itemStackSelectionPredicate)
    {
        @NotNull final Predicate<ItemStack> firstWorthySlotPredicate = ItemStackUtils.NOT_EMPTY_PREDICATE.and(itemStackSelectionPredicate);

        for (int slot = 0; slot < itemHandler.getSlots(); slot++)
        {
            if (firstWorthySlotPredicate.test(itemHandler.getStackInSlot(slot)))
            {
                return slot;
            }
        }

        return -1;
        //TODO: Later harden contract to remove compare on slot := -1
        //throw new IllegalStateException("Item "+targetItem.getTranslationKey() + " not found in ItemHandler!");
    }

    /**
     * 返回 {@link ICapabilityProvider} 中的物品栈数量。
     *
     * @param provider {@link ICapabilityProvider} 要扫描的提供者。
     * @param block    要计数的方块
     * @return 匹配给定方块和 ItemDamage 的堆栈的数量。
     */
    public static int getItemCountInProvider(@NotNull final ICapabilityProvider provider, @NotNull final Block block)
    {
        return getItemCountInProvider(provider, getItemFromBlock(block));
    }

    /**
     * 返回 {@link ICapabilityProvider} 中的物品栈数量。
     *
     * @param provider   {@link ICapabilityProvider} 要扫描的提供者。
     * @param targetItem 要计数的物品。
     * @return 匹配给定物品和 ItemDamage 的堆栈的数量。
     */
    public static int getItemCountInProvider(@NotNull final ICapabilityProvider provider, @NotNull final Item targetItem)
    {
        return getItemCountInProvider(provider, (ItemStack stack) -> compareItems(stack, targetItem));
    }

    /**
     * 返回 {@link ICapabilityProvider} 中的物品栈数量。
     *
     * @param provider                    {@link ICapabilityProvider} 要扫描的提供者。
     * @param itemStackSelectionPredicate 用于选择要计数的物品栈的谓词。
     * @return 匹配给定谓词的堆栈的数量。
     */
    public static int getItemCountInProvider(@NotNull final ICapabilityProvider provider, @NotNull final Predicate<ItemStack> itemStackSelectionPredicate)
    {
        return getItemHandlersFromProvider(provider).stream().filter(Objects::nonNull)
                 .mapToInt(handler -> filterItemHandler(handler, itemStackSelectionPredicate).stream().mapToInt(ItemStackUtils::getSize).sum())
                 .sum();
    }

    /**
     * 返回 {@link ICapabilityProvider} 中耐久性的总和。
     *
     * @param provider                    {@link ICapabilityProvider} 要扫描的提供者。
     * @param itemStackSelectionPredicate 用于选择要计数的物品栈的谓词。
     * @return 匹配给定谓词的堆栈的耐久性总和。
     */
    public static int getDurabilityInProvider(@NotNull final ICapabilityProvider provider, @NotNull final Predicate<ItemStack> itemStackSelectionPredicate)
    {
        return getItemHandlersFromProvider(provider).stream().filter(Objects::nonNull)
                 .mapToInt(handler -> filterItemHandler(handler, itemStackSelectionPredicate).stream().mapToInt(ItemStackUtils::getDurability).sum())
                 .sum();
    }

    /**
     * 检查建筑物中是否有超过一定数量的堆栈。如果堆栈较少，则返回它的数量。
     *
     * @param provider 要检查的建筑物
     * @param stack    要检查的堆栈
     * @param count    要检查的数量
     * @return 匹配给定谓词的堆栈的数量。
     */
    public static int hasBuildingEnoughElseCount(@NotNull final IBuilding provider, @NotNull final ItemStorage stack, final int count)
    {
        int totalCount = 0;
        final Level world = provider.getColony().getWorld();

        for (final BlockPos pos : provider.getContainers())
        {
            if (WorldUtil.isBlockLoaded(world, pos))
            {
                final BlockEntity entity = world.getBlockEntity(pos);
                if (entity instanceof TileEntityRack)
                {
                    totalCount += ((TileEntityRack) entity).getCount(stack);
                }
                else if (entity instanceof ChestBlockEntity)
                {
                    totalCount += getItemCountInProvider(entity, itemStack -> ItemStackUtils.compareItemStacksIgnoreStackSize(itemStack, stack.getItemStack(), !stack.ignoreDamageValue(), !stack.ignoreNBT() ));
                }

                if (totalCount > count)
                {
                    return Integer.MAX_VALUE;
                }
            }
        }

        return totalCount;
    }

    /**
     * 检查建筑物中是否有超过一定数量的堆栈。如果堆栈较少，则返回它的数量。
     *
     * @param provider 要检查的建筑物
     * @param stack    要检查的堆栈
     * @param count    要检查的数量
     * @return 匹配给定谓词的堆栈的数量。
     */
    public static int hasBuildingEnoughElseCount(@NotNull final IBuilding provider, @NotNull final Predicate<ItemStack> stack, final int count)
    {
        int totalCount = 0;
        final Level world = provider.getColony().getWorld();

        for (final BlockPos pos : provider.getContainers())
        {
            if (WorldUtil.isBlockLoaded(world, pos))
            {
                final BlockEntity entity = world.getBlockEntity(pos);
                if (entity instanceof TileEntityRack)
                {
                    totalCount += ((TileEntityRack) entity).getItemCount(stack);
                }

                if (totalCount > count)
                {
                    return Integer.MAX_VALUE;
                }
            }
        }

        return totalCount;
    }

    /**
     * 计算建筑物中不同类型物品的数量。
     *
     * @param provider 要检查的建筑物。
     * @param stacks   要检查的堆栈。
     * @return 匹配给定堆栈的数量。
     */
    public static int getCountFromBuilding(@NotNull final IBuilding provider, @NotNull final List<ItemStorage> stacks)
    {
        int totalCount = 0;

        for (ItemStorage stack : stacks)
        {
            totalCount += getCountFromBuilding(provider, stack);
        }

        return totalCount;
    }

    /**
     * 计算建筑物中的物品数量。
     *
     * @param provider 要检查的建筑物
     * @param stack    要检查的堆栈
     * @return 匹配给定堆栈的数量。
     */
    public static int getCountFromBuilding(@NotNull final IBuilding provider, @NotNull final ItemStorage stack)
    {
        int totalCount = 0;
        final Level world = provider.getColony().getWorld();

        for (final BlockPos pos : provider.getContainers())
        {
            if (WorldUtil.isBlockLoaded(world, pos))
            {
                final BlockEntity entity = world.getBlockEntity(pos);
                if (entity instanceof TileEntityRack)
                {
                    totalCount += ((TileEntityRack) entity).getCount(stack);
                }
                else if (entity instanceof ChestBlockEntity)
                {
                    totalCount += getItemCountInProvider(entity, itemStack -> ItemStackUtils.compareItemStacksIgnoreStackSize(itemStack, stack.getItemStack()));
                }
            }
        }

        return totalCount;
    }
    /**
     * 计算给定建筑物中的空槽位数量。
     * @param ownBuilding 要检查的建筑物。
     * @return 空槽位的数量。
     */
    public static int countEmptySlotsInBuilding(final IBuilding ownBuilding)
    {
        int totalCount = 0;
        final Level world = ownBuilding.getColony().getWorld();

        for (final BlockPos pos : ownBuilding.getContainers())
        {
            if (WorldUtil.isBlockLoaded(world, pos))
            {
                final BlockEntity entity = world.getBlockEntity(pos);
                if (entity instanceof TileEntityRack)
                {
                    totalCount += ((TileEntityRack) entity).getFreeSlots();
                }
            }
        }

        return totalCount;
    }

    /**
     * 计算建筑物中的物品数量。
     *
     * @param provider  要检查的建筑物。
     * @param predicate 要匹配的谓词。
     * @return 匹配给定堆栈的堆栈的数量。
     */
    public static int getCountFromBuilding(@NotNull final IBuilding provider, @NotNull final Predicate<ItemStack> predicate)
    {
        int totalCount = 0;
        final Level world = provider.getColony().getWorld();

        for (final BlockPos pos : provider.getContainers())
        {
            if (WorldUtil.isBlockLoaded(world, pos))
            {
                final BlockEntity entity = world.getBlockEntity(pos);
                if (entity instanceof TileEntityRack)
                {
                    totalCount += ((TileEntityRack) entity).getItemCount(predicate);
                }
            }
        }

        return totalCount;
    }

    /**
     * 计算建筑物中的物品数量。
     * 仅计算特定物品的“限制”。
     *
     * @param provider  要检查的建筑物。
     * @param predicate 要匹配的谓词。
     * @return 匹配给定堆栈的堆栈的数量。
     */
    public static int getCountFromBuildingWithLimit(@NotNull final IBuilding provider, @NotNull final Predicate<ItemStack> predicate, final Function<ItemStack, Integer> limit)
    {
        final Level world = provider.getColony().getWorld();

        final Map<ItemStorage, Integer> allMatching = new HashMap<>();

        for (final BlockPos pos : provider.getContainers())
        {
            if (WorldUtil.isBlockLoaded(world, pos))
            {
                final BlockEntity entity = world.getBlockEntity(pos);
                if (entity instanceof TileEntityRack)
                {
                    for (final Map.Entry<ItemStorage, Integer> entry : ((TileEntityRack) entity).getAllContent().entrySet())
                    {
                        if (predicate.test(entry.getKey().getItemStack()))
                        {
                            allMatching.put(entry.getKey(), allMatching.getOrDefault(entry.getKey(), 0) + entry.getValue());
                        }
                    }
                }
            }
        }

        int totalCount = 0;
        for (final Map.Entry<ItemStorage, Integer> entry : allMatching.entrySet())
        {
            totalCount += Math.min(limit.apply(entry.getKey().getItemStack()), entry.getValue());
        }

        return totalCount;
    }

    /**
     * 检查玩家是否在{@link ICapabilityProvider}中拥有一个块。
     * 通过{@link #getItemCountInProvider(ICapabilityProvider, Block)} > 0;来检查。
     *
     * @param Provider {@link ICapabilityProvider} 要扫描的
     * @param block    要计数的块
     * @return 如果在{@link ICapabilityProvider}中，则为true，否则为false
     */
    public static boolean hasItemInProvider(@NotNull final ICapabilityProvider Provider, @NotNull final Block block)
    {
        return hasItemInProvider(Provider, getItemFromBlock(block));
    }

    /**
     * 检查玩家是否在{@link ICapabilityProvider}中拥有一个物品。
     * 通过{@link #getItemCountInProvider(ICapabilityProvider, Item)} > 0;来检查。
     *
     * @param Provider {@link ICapabilityProvider} 要扫描的
     * @param item     要计数的物品
     * @return 如果在{@link ICapabilityProvider}中，则为true，否则为false
     */
    public static boolean hasItemInProvider(@NotNull final ICapabilityProvider Provider, @NotNull final Item item)
    {
        return hasItemInProvider(Provider, (ItemStack stack) -> compareItems(stack, item));
    }

    /**
     * 检查玩家是否在{@link ICapabilityProvider}中拥有一个物品。
     * 通过{@link InventoryUtils#getItemCountInProvider(ICapabilityProvider, Predicate)} > 0;来检查。
     *
     * @param Provider                    {@link ICapabilityProvider} 要扫描的
     * @param itemStackSelectionPredicate 要匹配ItemStack的谓词。
     * @return 如果在{@link ICapabilityProvider}中，则为true，否则为false
     */
    public static boolean hasItemInProvider(@NotNull final ICapabilityProvider Provider, @NotNull final Predicate<ItemStack> itemStackSelectionPredicate)
    {
        for (IItemHandler handler : getItemHandlersFromProvider(Provider))
        {
            if (findFirstSlotInItemHandlerWith(handler, itemStackSelectionPredicate) != -1)
            {
                return true;
            }
        }
        return false;
    }

    /**
     * 返回{@link ICapabilityProvider}是否已满。
     *
     * @param provider {@link ICapabilityProvider}。
     * @return 如果{@link ICapabilityProvider}已满则返回true，否则返回false。
     */
    public static boolean isProviderFull(@NotNull final ICapabilityProvider provider)
    {
        return getFirstOpenSlotFromProvider(provider) == -1;
    }

    /**
     * 返回{@link ICapabilityProvider}中的第一个空槽位。
     *
     * @param provider 要检查的{@link ICapabilityProvider}。
     * @return 槽位索引，如果没有找到则返回-1。
     */
    public static int getFirstOpenSlotFromProvider(@NotNull final ICapabilityProvider provider)
    {
        return getItemHandlersFromProvider(provider).stream()
                .mapToInt(InventoryUtils::getFirstOpenSlotFromItemHandler)
                .filter(slotIndex -> slotIndex > -1)
                .findFirst()
                .orElse(-1);
    }

    /**
     * 检查{@link ICapabilityProvider}是否包含具有给定最小级别的以下工具名称。
     *
     * @param provider     要扫描的{@link ICapabilityProvider}。
     * @param toolType     要查找的工具的工具类型名称。
     * @param minimalLevel 要查找的最小级别。
     * @param maximumLevel 要查找的最大级别。
     * @return 如果在给定的{@link ICapabilityProvider}中找到具有给定工具类型名称的工具，则为true，否则为false。
     */
    public static boolean isToolInProvider(@NotNull final ICapabilityProvider provider, @NotNull final IToolType toolType, final int minimalLevel, final int maximumLevel)
    {
        return hasItemInProvider(provider, (ItemStack stack) -> ItemStackUtils.hasToolLevel(stack, toolType, minimalLevel, maximumLevel));
    }

    /**
     * 将堆栈添加到提供者。
     *
     * @param provider  要添加物品堆栈的{@link ICapabilityProvider}。
     * @param itemStack 要添加的物品堆栈。
     * @return 如果成功，则为true，否则为false。
     */
    public static boolean addItemStackToProvider(@NotNull final ICapabilityProvider provider, @Nullable final ItemStack itemStack)
    {
        return getItemHandlersFromProvider(provider).stream().anyMatch(handler -> addItemStackToItemHandler(handler, itemStack));
    }

    /**
     * 将堆栈添加到物品处理程序。
     *
     * @param itemHandler {@link IItemHandler} 要添加物品堆栈的。
     * @param itemStack   要添加的物品堆栈。
     * @return 如果成功，则为true，否则为false。
     */
    public static boolean addItemStackToItemHandler(@NotNull final IItemHandler itemHandler, @Nullable final ItemStack itemStack)
    {
        if (itemHandler.getSlots() == 0)
        {
            return false;
        }

        if (!ItemStackUtils.isEmpty(itemStack))
        {
            if (itemStack.isDamaged())
            {
                int slot = getFirstOpenSlotFromItemHandler(itemHandler);

                if (slot >= 0)
                {
                    itemHandler.insertItem(slot, itemStack, false);
                    return true;
                }
                else
                {
                    return false;
                }
            }
            else
            {
                ItemStack resultStack = itemStack.copy();
                int slot = 0;
                boolean placed = false;

                while (!ItemStackUtils.isEmpty(resultStack) && slot < itemHandler.getSlots())
                {
                    resultStack = itemHandler.insertItem(slot, resultStack, true);
                    if (ItemStackUtils.isEmpty(resultStack))
                    {
                        placed = true;
                        break;
                    }
                    slot++;
                }

                if (!placed)
                {
                    return false;
                }

                slot = 0;
                resultStack = itemStack;
                while (!ItemStackUtils.isEmpty(resultStack) && slot < itemHandler.getSlots())
                {
                    resultStack = itemHandler.insertItem(slot, resultStack, false);
                    if (ItemStackUtils.isEmpty(resultStack))
                    {
                        return true;
                    }
                    slot++;
                }

                // 这是永远不可能发生的！我们检查了它是否可能。这是不可能的。
                return false;
            }
        }
        else
        {
            return false;
        }
    }

    /**
     * 将堆栈添加到带有结果的提供者。
     *
     * @param provider  要添加物品堆栈的{@link ICapabilityProvider}。
     * @param itemStack 要添加的物品堆栈。
     * @return 当完全传输而不交换时为空，否则返回物品堆栈的部分传输剩余。
     */
    public static ItemStack addItemStackToProviderWithResult(@NotNull final ICapabilityProvider provider, @Nullable final ItemStack itemStack)
    {
        ItemStack activeStack = itemStack;

        if (ItemStackUtils.isEmpty(activeStack))
        {
            return ItemStackUtils.EMPTY;
        }

        for (final IItemHandler handler : getItemHandlersFromProvider(provider))
        {
            activeStack = addItemStackToItemHandlerWithResult(handler, activeStack);
        }

        return activeStack;
    }

    /**
     * 将堆栈添加到带有结果的处理程序。
     *
     * @param itemHandler {@link IItemHandler} 要添加物品堆栈的。
     * @param itemStack   要添加的物品堆栈。
     * @return 当完全传输时为空，否则返回物品堆栈的部分传输剩余。
     */
    public static ItemStack addItemStackToItemHandlerWithResult(@NotNull final IItemHandler itemHandler, @Nullable final ItemStack itemStack)
    {
        if (!ItemStackUtils.isEmpty(itemStack))
        {
            int slot;

            if (itemStack.isDamaged())
            {
                slot = getFirstOpenSlotFromItemHandler(itemHandler);

                if (slot >= 0)
                {
                    itemHandler.insertItem(slot, itemStack.copy(), false);
                    return ItemStackUtils.EMPTY;
                }
                else
                {
                    return itemStack;
                }
            }
            else
            {
                ItemStack leftOver = itemStack;
                slot = itemHandler.getSlots() == 0 ? -1 : 0;
                while (!ItemStackUtils.isEmpty(leftOver) && slot != -1 && slot != itemHandler.getSlots())
                {
                    leftOver = itemHandler.insertItem(slot, leftOver.copy(), false);
                    if (!ItemStackUtils.isEmpty(leftOver))
                    {
                        slot++;
                    }
                }

                return leftOver;
            }
        }
        else
        {
            return itemStack;
        }
    }

    /**
     * 强制将堆栈添加到提供者。
     *
     * @param provider                   要添加物品堆栈的{@link ICapabilityProvider}。
     * @param itemStack                  要添加的物品堆栈。
     * @param itemStackToKeepPredicate    确定要在库存中保留哪些ItemStack的{@link Predicate}。返回false以替换。
     * @return 已替换的itemStack。
     */
    @Nullable
    public static ItemStack forceItemStackToProvider(
            @NotNull final ICapabilityProvider provider,
            @NotNull final ItemStack itemStack,
            @NotNull final Predicate<ItemStack> itemStackToKeepPredicate)
    {
        final ItemStack standardInsertionResult = addItemStackToProviderWithResult(provider, itemStack);

        if (!ItemStackUtils.isEmpty(standardInsertionResult))
        {
            ItemStack resultStack = standardInsertionResult.copy();
            final Iterator<IItemHandler> iterator = getItemHandlersFromProvider(provider).iterator();
            while (iterator.hasNext() && !ItemStackUtils.isEmpty(resultStack))
            {
                resultStack = forceItemStackToItemHandler(iterator.next(), resultStack, itemStackToKeepPredicate);
            }

            return resultStack;
        }

        return ItemStackUtils.EMPTY;
    }

    /**
     * 返回库存中的物品堆栈数量。这等于{@link #getProviderAsList(ICapabilityProvider)}<code>.length();</code>。
     *
     * @param provider 要计算物品堆栈数量的{@link ICapabilityProvider}。
     * @return {@link ICapabilityProvider}中的物品堆栈数量。
     */
    public static int getAmountOfStacksInProvider(@NotNull final ICapabilityProvider provider)
    {
        return getProviderAsList(provider).size();
    }

    /**
     * 将{@link ICapabilityProvider}返回为物品堆栈列表。
     *
     * @param provider 要转换的提供者。
     * @return 物品堆栈的列表。
     */
    @NotNull
    public static List<ItemStack> getProviderAsList(@NotNull final ICapabilityProvider provider)
    {
        return filterProvider(provider, (ItemStack stack) -> true);
    }
    /**
     * 用于检查 {@link ICapabilityProvider} 是否具有任何 {@link IItemHandler} 的方法
     *
     * @param provider 要检查的提供者。
     * @return 如果提供者具有任何 {@link IItemHandler}，则返回 true，否则返回 false。
     */
    @NotNull
    public static boolean hasProviderIItemHandler(@NotNull final ICapabilityProvider provider)
    {
        return !getItemHandlersFromProvider(provider).isEmpty();
    }

    /**
     * 用于检查此提供者是否是有多个不同方向的 IItemHandler 的方法。
     *
     * @param provider 要检查的提供者。
     * @return 如果提供者具有多个不同方向的 IItemHandler，则返回 true，否则返回 false。
     */
    @NotNull
    public static boolean isProviderSided(@NotNull final ICapabilityProvider provider)
    {
        return getItemHandlersFromProvider(provider).size() > 1;
    }

    /**
     * 返回作为项目堆栈列表的 {@link IItemHandler}。
     *
     * @param provider 包含给定 {@link Direction} 的 {@link IItemHandler} 的 {@link ICapabilityProvider}
     * @param facing   要获取 {@link IItemHandler} 的方向。内部的可以为 null。
     * @return 项目堆栈的列表。
     */
    @NotNull
    public static List<ItemStack> getInventoryAsListFromProviderForSide(@NotNull final ICapabilityProvider provider, @Nullable final Direction facing)
    {
        return filterItemHandler(provider.getCapability(ITEM_HANDLER_CAPABILITY, facing).orElse(null), (ItemStack stack) -> true);
    }

    /**
     * 在 {@link IItemHandler} 中过滤项目列表，使用 {@link #compareItems(ItemStack, Item)} 进行匹配，使用 MetaData 和 {@link #getItemFromBlock(Block)} 作为 Predicate 的参数。
     *
     * @param provider 包含给定 {@link Direction} 的 {@link IItemHandler} 的 {@link ICapabilityProvider}
     * @param facing   要获取 {@link IItemHandler} 的方向。内部的可以为 null。
     * @param block    要过滤的方块
     * @return 项目堆栈的列表
     */
    @NotNull
    public static List<ItemStack> filterItemHandlerFromProviderForSide(
            @NotNull final ICapabilityProvider provider,
            @Nullable final Direction facing,
            @NotNull final Block block)
    {
        return filterItemHandler(provider.getCapability(ITEM_HANDLER_CAPABILITY, facing).orElse(null), (ItemStack stack) -> compareItems(stack, getItemFromBlock(block)));
    }

    /**
     * 在 {@link IItemHandler} 中过滤项目列表，使用 {@link #compareItems(ItemStack, Item)} 进行匹配，使用 targetItem 和 itemDamage 作为参数。
     *
     * @param provider     包含给定 {@link Direction} 的 {@link IItemHandler} 的 {@link ICapabilityProvider}
     * @param facing       要获取 {@link IItemHandler} 的方向。内部的可以为 null。
     * @param targetItem   要查找的项目
     * @param itemDamage   损坏值。
     * @return 包含给定项目的库存中的项目堆栈列表
     */
    @NotNull
    public static List<ItemStack> filterItemHandlerFromProviderForSide(
            @NotNull final ICapabilityProvider provider,
            @Nullable final Direction facing,
            @NotNull final Item targetItem,
            final int itemDamage)
    {
        return filterItemHandler(provider.getCapability(ITEM_HANDLER_CAPABILITY, facing).orElse(null), (ItemStack stack) -> compareItems(stack, targetItem));
    }

    /**
     * 在 {@link IItemHandler} 中过滤项目列表，匹配给定的断言，使用 {@link ICapabilityProvider}。
     *
     * @param provider                    包含给定 {@link Direction} 的 {@link IItemHandler} 的 {@link ICapabilityProvider}
     * @param facing                      要获取 {@link IItemHandler} 的方向。内部的可以为 null。
     * @param itemStackSelectionPredicate 用于匹配堆栈的断言。
     * @return 匹配给定断言的项目堆栈列表。
     */
    @NotNull
    public static List<ItemStack> filterItemHandlerFromProviderForSide(
            @NotNull final ICapabilityProvider provider,
            @Nullable final Direction facing,
            @NotNull final Predicate<ItemStack> itemStackSelectionPredicate)
    {
        if (!provider.getCapability(ITEM_HANDLER_CAPABILITY, facing).isPresent())
        {
            return Collections.emptyList();
        }

        return filterItemHandler(provider.getCapability(ITEM_HANDLER_CAPABILITY, facing).orElse(null), itemStackSelectionPredicate);
    }

    /**
     * 返回给定 {@link Direction} 的 {@link ICapabilityProvider} 中给定块的第一个出现的索引。
     *
     * @param provider   要检查的 {@link ICapabilityProvider}。
     * @param facing     要检查的方向。
     * @param block      要查找的块。
     * @param itemDamage 项目损坏值。
     * @return 第一个出现的索引。
     */
    public static int findFirstSlotInProviderForSideWith(
            @NotNull final ICapabilityProvider provider,
            @Nullable final Direction facing,
            @NotNull final Block block,
            final int itemDamage)
    {
        return findFirstSlotInProviderForSideWith(provider, facing, getItemFromBlock(block));
    }

    /**
     * 返回给定 {@link Direction} 的 {@link ICapabilityProvider} 中具有给定 ItemDamage 的项目的第一个出现的索引。
     *
     * @param provider   要检查的 {@link ICapabilityProvider}。
     * @param facing     要检查的方向。
     * @param targetItem 要查找的项目。
     * @return 第一个出现的索引。
     */
    public static int findFirstSlotInProviderForSideWith(
            @NotNull final ICapabilityProvider provider,
            @Nullable final Direction facing,
            @NotNull final Item targetItem)
    {
        return findFirstSlotInProviderForSideWith(provider, facing, (ItemStack stack) -> compareItems(stack, targetItem));
    }

    /**
     * 返回给定 {@link Direction} 的 {@link ICapabilityProvider} 中与给定断言匹配的 ItemStack 的第一个出现的索引。
     *
     * @param provider                    要检查的提供者
     * @param facing                      要检查的方向。
     * @param itemStackSelectionPredicate 用于匹配的断言。
     * @return 第一个出现的索引。
     */
    public static int findFirstSlotInProviderForSideWith(
      @NotNull final ICapabilityProvider provider,
      @Nullable final Direction facing,
      @NotNull final Predicate<ItemStack> itemStackSelectionPredicate)
    {
        if (!provider.getCapability(ITEM_HANDLER_CAPABILITY, facing).isPresent())
        {
            return -1;
            //TODO: 后续加强合同以删除对 slot 的比较 := -1
            //throw new IllegalStateException("Item "+targetItem.getTranslationKey() + " not found in ItemHandler!");
        }

        return findFirstSlotInItemHandlerWith(provider.getCapability(ITEM_HANDLER_CAPABILITY, facing).orElse(null), itemStackSelectionPredicate);
    }

    /**
     * 返回给定 {@link Direction} 的 {@link ICapabilityProvider} 中的项目数量。
     *
     * @param provider 要扫描的 {@link ICapabilityProvider}。
     * @param facing   要计数的方向。
     * @param block    要计数的块
     * @return 匹配给定块和 ItemDamage 的堆栈的数量
     */
    public static int getItemCountInProviderForSide(
      @NotNull final ICapabilityProvider provider,
      @Nullable final Direction facing,
      @NotNull final Block block)
    {
        return getItemCountInProviderForSide(provider, facing, getItemFromBlock(block));
    }

    /**
     * 返回给定 {@link Direction} 的 {@link ICapabilityProvider} 中的项目数量。
     *
     * @param provider   要扫描的 {@link ICapabilityProvider}。
     * @param facing     要计数的方向。
     * @param targetItem 要计数的项目
     * @return 匹配给定项目和 ItemDamage 的堆栈的数量
     */
    public static int getItemCountInProviderForSide(
      @NotNull final ICapabilityProvider provider,
      @Nullable final Direction facing,
      @NotNull final Item targetItem)
    {
        return getItemCountInProviderForSide(provider, facing, (ItemStack stack) -> compareItems(stack, targetItem));
    }

    /**
     * 返回给定 {@link Direction} 的 {@link ICapabilityProvider} 中的项目数量。
     *
     * @param provider                    要扫描的 {@link ICapabilityProvider}。
     * @param facing                      要计数的方向。
     * @param itemStackSelectionPredicate 用于选择要计数的堆栈的断言。
     * @return 匹配给定断言的堆栈的数量。
     */
    public static int getItemCountInProviderForSide(
      @NotNull final ICapabilityProvider provider,
      @Nullable final Direction facing,
      @NotNull final Predicate<ItemStack> itemStackSelectionPredicate)
    {
        if (!provider.getCapability(ITEM_HANDLER_CAPABILITY, facing).isPresent())
        {
            return 0;
        }

        return filterItemHandler(provider.getCapability(ITEM_HANDLER_CAPABILITY, facing).orElse(null), itemStackSelectionPredicate).stream()
                 .mapToInt(ItemStackUtils::getSize)
                 .sum();
    }

    /**
     * 检查玩家是否在 {@link ICapabilityProvider} 中有一个块，对于给定的 {@link Direction}。通过 {@link #getItemCountInProvider(ICapabilityProvider, Block)} > 0; 进行检查。
     *
     * @param provider 要扫描的 {@link ICapabilityProvider}
     * @param facing   要检查的侧面。
     * @param block    要计数的块
     * @return 如果在 {@link ICapabilityProvider} 中，则返回 true，否则返回 false
     */
    public static boolean hasItemInProviderForSide(@NotNull final ICapabilityProvider provider, @Nullable final Direction facing, @NotNull final Block block)
    {
        return hasItemInProviderForSide(provider, facing, getItemFromBlock(block));
    }

    /**
     * 检查玩家是否在 {@link ICapabilityProvider} 中有一个项目，对于给定的 {@link Direction}。通过 {@link #getItemCountInProvider(ICapabilityProvider, Item)} > 0; 进行检查。
     *
     * @param provider 要扫描的 {@link ICapabilityProvider}
     * @param facing   要检查的侧面。
     * @param item     要计数的项目
     * @return 如果在 {@link ICapabilityProvider} 中，则返回 true，否则返回 false
     */
    public static boolean hasItemInProviderForSide(@NotNull final ICapabilityProvider provider, @Nullable final Direction facing, @NotNull final Item item)
    {
        return hasItemInProviderForSide(provider, facing, (ItemStack stack) -> compareItems(stack, item));
    }

    /**
     * 检查玩家是否在 {@link ICapabilityProvider} 中有一个项目，对于给定的 {@link Direction}。通过 {@link InventoryUtils#getItemCountInProvider(ICapabilityProvider, Predicate)} > 0; 进行检查。
     *
     * @param provider                    要扫描的 {@link ICapabilityProvider}
     * @param facing                      要检查的侧面。
     * @param itemStackSelectionPredicate 用于匹配 ItemStack 的断言。
     * @return 如果在 {@link ICapabilityProvider} 中，则返回 true，否则返回 false
     */
    public static boolean hasItemInProviderForSide(
      @NotNull final ICapabilityProvider provider,
      @Nullable final Direction facing,
      @NotNull final Predicate<ItemStack> itemStackSelectionPredicate)
    {
        if (!provider.getCapability(ITEM_HANDLER_CAPABILITY, facing).isPresent())
        {
            return false;
        }

        return findFirstSlotInItemHandlerNotEmptyWith(provider.getCapability(ITEM_HANDLER_CAPABILITY, facing).orElse(null), itemStackSelectionPredicate) > -1;
    }

    /**
     * 返回 {@link ICapabilityProvider} 是否已满，对于给定的 {@link Direction}。
     *
     * @param provider 要检查的 {@link ICapabilityProvider}。
     * @param facing   要检查的侧面。
     * @return 如果 {@link ICapabilityProvider} 已满，则返回 true，否则返回 false。
     */
    public static boolean isProviderFull(@NotNull final ICapabilityProvider provider, @Nullable final Direction facing)
    {
        return getFirstOpenSlotFromProviderForSide(provider, facing) == -1;
    }

    /**
     * 返回 {@link ICapabilityProvider} 中的第一个空槽位，对于给定的 {@link Direction}。
     *
     * @param provider 要检查的 {@link ICapabilityProvider}。
     * @param facing   要检查的侧面。
     * @return 插槽索引或如果没有找到则为 -1。
     */
    public static int getFirstOpenSlotFromProviderForSide(@NotNull final ICapabilityProvider provider, @Nullable final Direction facing)
    {
        if (!provider.getCapability(ITEM_HANDLER_CAPABILITY, facing).isPresent())
        {
            return -1;
        }

        return getFirstOpenSlotFromItemHandler(provider.getCapability(ITEM_HANDLER_CAPABILITY, facing).orElse(null));
    }
    /**
     * 检查给定的 {@link ICapabilityProvider} 是否包含具有给定最小级别的指定工具名称，针对给定的 {@link Direction}。
     *
     * @param provider     要扫描的 {@link ICapabilityProvider}。
     * @param facing       要检查的方向。
     * @param toolType     要查找的工具类型。
     * @param minimalLevel 要查找的最小级别。
     * @param maximumLevel 要查找的最大级别。
     * @return 如果在给定的 {@link ICapabilityProvider} 中找到具有给定工具类型名称的工具，则返回 true，否则返回 false。
     */
    public static boolean isToolInProviderForSide(
            @NotNull final ICapabilityProvider provider, @Nullable final Direction facing, @NotNull final IToolType toolType,
            final int minimalLevel, final int maximumLevel)
    {
        if (!provider.getCapability(ITEM_HANDLER_CAPABILITY, facing).isPresent())
        {
            return false;
        }

        return isToolInItemHandler(provider.getCapability(ITEM_HANDLER_CAPABILITY, facing).orElse(null), toolType, minimalLevel, maximumLevel);
    }

    /**
     * 检查给定的 {@link IItemHandler} 是否包含具有给定最小级别的指定工具名称。
     *
     * @param itemHandler  要扫描的 {@link IItemHandler}。
     * @param toolType     要查找的工具的工具类型。
     * @param minimalLevel 要查找的最小级别。
     * @param maximumLevel 要查找的最大级别。
     * @return 如果在给定的 {@link IItemHandler} 中找到具有给定工具类型名称的工具，则返回 true，否则返回 false。
     */
    public static boolean isToolInItemHandler(@NotNull final IItemHandler itemHandler, @NotNull final IToolType toolType, final int minimalLevel, final int maximumLevel)
    {
        return hasItemInItemHandler(itemHandler, (ItemStack stack) ->
                ItemStackUtils.hasToolLevel(stack, toolType, minimalLevel, maximumLevel));
    }

    /**
     * 清空整个 {@link IItemHandler}。
     *
     * @param itemHandler 要清空的 {@link IItemHandler}。
     */
    public static void clearItemHandler(@NotNull final IItemHandler itemHandler)
    {
        for (int slotIndex = 0; slotIndex < itemHandler.getSlots(); slotIndex++)
        {
            itemHandler.extractItem(slotIndex, Integer.MAX_VALUE, false);
        }
    }

    /**
     * 如果 {@link IItemHandler} 包含给定的工具类型，则返回槽位编号。
     *
     * @param itemHandler  要从中获取槽位的 {@link IItemHandler}。
     * @param toolType     要查找的工具类型。
     * @param minimalLevel 要查找的最小级别。
     * @param maximumLevel 要查找的最大级别。
     * @return 如果找到，则返回槽位编号；如果未找到，则返回 -1。
     */
    public static int getFirstSlotOfItemHandlerContainingTool(
            @NotNull final IItemHandler itemHandler, @NotNull final IToolType toolType, final int minimalLevel,
            final int maximumLevel)
    {
        return findFirstSlotInItemHandlerWith(itemHandler,
                (ItemStack stack) -> ItemStackUtils.hasToolLevel(stack, toolType, minimalLevel, maximumLevel));
    }

    /**
     * 验证工人的库存中是否有一个具有可接受级别的工具。
     *
     * @param itemHandler   工人的库存。
     * @param toolType      需要的工具类型。
     * @param requiredLevel 最低工具级别。
     * @param maximumLevel  工人的小屋级别。
     * @return 如果工具可接受，则返回 true。
     */
    public static boolean hasItemHandlerToolWithLevel(@NotNull final IItemHandler itemHandler, final IToolType toolType, final int requiredLevel, final int maximumLevel)
    {
        return findFirstSlotInItemHandlerWith(itemHandler,
                (ItemStack stack) -> (!ItemStackUtils.isEmpty(stack) && (ItemStackUtils.isTool(stack, toolType) && ItemStackUtils.verifyToolLevel(stack,
                        ItemStackUtils.getMiningLevel(stack, toolType),
                        requiredLevel, maximumLevel)))) > -1;
    }

    /**
     * 将给定源 {@link IItemHandler} 中的 ItemStack 与给定目标 {@link ICapabilityProvider} 中的 ItemStack 进行交换的方法。
     *
     * @param sourceHandler 作为源的 {@link IItemHandler}。
     * @param sourceIndex   要提取的槽位的索引。
     * @param targetProvider 作为目标的 {@link ICapabilityProvider}。
     * @return 如果交换成功，则返回 true；否则返回 false。
     */
    public static boolean transferItemStackIntoNextFreeSlotInProvider(
            @NotNull final IItemHandler sourceHandler,
            @NotNull final int sourceIndex,
            @NotNull final ICapabilityProvider targetProvider)
    {
        for (final IItemHandler handler : getItemHandlersFromProvider(targetProvider))
        {
            if (transferItemStackIntoNextFreeSlotInItemHandler(sourceHandler, sourceIndex, handler))
            {
                return true;
            }
        }

        return false;
    }

    /**
     * 将给定源 {@link IItemHandler} 中的 ItemStack 与给定目标 {@link IItemHandler} 中的 ItemStack 进行交换的方法。
     *
     * @param sourceHandler 作为源的 {@link IItemHandler}。
     * @param sourceIndex   要提取的槽位的索引。
     * @param targetHandler 作为目标的 {@link IItemHandler}。
     * @return 如果交换成功，则返回 true；否则返回 false。
     */
    public static boolean transferItemStackIntoNextFreeSlotInItemHandler(
            @NotNull final IItemHandler sourceHandler,
            final int sourceIndex,
            @NotNull final IItemHandler targetHandler)
    {
        ItemStack sourceStack = sourceHandler.extractItem(sourceIndex, Integer.MAX_VALUE, true);

        if (ItemStackUtils.isEmpty(sourceStack))
        {
            return true;
        }

        boolean success = false;
        for (int i = 0; i < targetHandler.getSlots(); i++)
        {
            sourceStack = targetHandler.insertItem(i, sourceStack, true);
            if (ItemStackUtils.isEmpty(sourceStack))
            {
                success = true;
                break;
            }
        }

        if (!success)
        {
            return false;
        }

        sourceStack = sourceHandler.extractItem(sourceIndex, Integer.MAX_VALUE, false);

        for (int i = 0; i < targetHandler.getSlots(); i++)
        {
            sourceStack = targetHandler.insertItem(i, sourceStack, false);
            if (ItemStackUtils.isEmpty(sourceStack))
            {
                return true;
            }
        }

        sourceHandler.insertItem(sourceIndex, sourceStack, false);
        return false;
    }

    /**
     * 将给定源 {@link IItemHandler} 中的 ItemStack 的一部分与给定目标 {@link IItemHandler} 中的 ItemStack 进行交换的方法。
     *
     * @param sourceHandler 作为源的 {@link IItemHandler}。
     * @param sourceIndex   要提取的槽位的索引。
     * @param count         数量。
     * @param targetHandler 作为目标的 {@link IItemHandler}。
     * @return 如果交换成功，则返回 true；否则返回 false。
     */
    public static boolean transferXOfItemStackIntoNextFreeSlotInItemHandler(
            @NotNull final IItemHandler sourceHandler,
            final int sourceIndex,
            final int count,
            @NotNull final IItemHandler targetHandler)
    {
        ItemStack sourceStack = sourceHandler.extractItem(sourceIndex, count, true);

        if (ItemStackUtils.isEmpty(sourceStack))
        {
            return true;
        }

        boolean success = false;
        for (int i = 0; i < targetHandler.getSlots(); i++)
        {
            sourceStack = targetHandler.insertItem(i, sourceStack, true);
            if (ItemStackUtils.isEmpty(sourceStack))
            {
                success = true;
                break;
            }
        }

        if (!success)
        {
            return false;
        }

        sourceStack = sourceHandler.extractItem(sourceIndex, count, false);

        for (int i = 0; i < targetHandler.getSlots(); i++)
        {
            sourceStack = targetHandler.insertItem(i, sourceStack, false);
            if (ItemStackUtils.isEmpty(sourceStack))
            {
                return true;
            }
        }

        sourceHandler.insertItem(sourceIndex, sourceStack, false);
        return false;
    }

    /**
     * 将给定源 {@link IItemHandler} 中的 ItemStack 与给定目标 {@link IItemHandler} 中的 ItemStack 进行交换的方法。尝试合并现有的 ItemStack。
     *
     * @param sourceHandler 作为源的 {@link IItemHandler}。
     * @param sourceIndex   要提取的槽位的索引。
     * @param targetHandler 作为目标的 {@link IItemHandler}。
     * @return 如果交换成功，则返回 true；否则返回 false。
     */
    public static boolean transferItemStackIntoNextBestSlotInItemHandler(
            @NotNull final IItemHandler sourceHandler,
            final int sourceIndex,
            @NotNull final IItemHandler targetHandler)
    {
        ItemStack sourceStack = sourceHandler.extractItem(sourceIndex, Integer.MAX_VALUE, true);

        if (ItemStackUtils.isEmpty(sourceStack))
        {
            return true;
        }

        if (addItemStackToItemHandler(targetHandler, sourceStack))
        {
            sourceHandler.extractItem(sourceIndex, Integer.MAX_VALUE, false);
            return true;
        }
        return false;
    }

    /**
     * 从给定源 {@link IItemHandler} 中的 ItemStack 向给定目标 {@link IItemHandler} 中的 ItemStack 进行交换的方法。
     *
     * @param sourceHandler 作为源的 {@link IItemHandler}。
     * @param predicate     ItemStack 的断言。
     * @param targetHandler 作为目标的 {@link IItemHandler}。
     * @return 如果交换成功，则返回 true；否则返回 false。
     */
    public static boolean transferItemStackIntoNextBestSlotInItemHandler(
            @NotNull final IItemHandler sourceHandler,
            final Predicate<ItemStack> predicate,
            @NotNull final IItemHandler targetHandler)
    {
        for (int i = 0; i < sourceHandler.getSlots(); i++)
        {
            if (predicate.test(sourceHandler.getStackInSlot(i)))
            {
                ItemStack sourceStack = sourceHandler.extractItem(i, Integer.MAX_VALUE, true);
                if (!sourceStack.isEmpty() && addItemStackToItemHandler(targetHandler, sourceStack))
                {
                    sourceHandler.extractItem(i, Integer.MAX_VALUE, false);
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * 将给定的 ItemStack 放入给定目标 {@link IItemHandler} 中的下一个最佳槽位的方法。尝试合并现有的 ItemStack。
     *
     * @param stack         要传输的 ItemStack。
     * @param targetHandler 作为目标的 {@link IItemHandler}。
     * @return 如果交换成功，则返回 true；否则返回 false。
     */
    public static boolean transferItemStackIntoNextBestSlotInItemHandler(final ItemStack stack, @NotNull final IItemHandler targetHandler)
    {
        return transferItemStackIntoNextBestSlotInItemHandlerWithResult(stack, targetHandler).isEmpty();
    }

    /**
     * 将给定的 ItemStack 放入给定目标 {@link IItemHandler} 中的下一个最佳槽位的方法。尝试合并现有的 ItemStack。
     *
     * @param stack         要传输的 ItemStack。
     * @param targetHandler 作为目标的 {@link IItemHandler}。
     * @return 剩余的 ItemStack。
     */
    public static ItemStack transferItemStackIntoNextBestSlotInItemHandlerWithResult(final ItemStack stack, @NotNull final IItemHandler targetHandler)
    {
        ItemStack sourceStack = stack.copy();

        if (ItemStackUtils.isEmpty(sourceStack))
        {
            return sourceStack;
        }

        sourceStack = mergeItemStackIntoNextBestSlotInItemHandlers(sourceStack, targetHandler);

        if (ItemStackUtils.isEmpty(sourceStack))
        {
            return sourceStack;
        }

        for (int i = 0; i < targetHandler.getSlots(); i++)
        {
            sourceStack = targetHandler.insertItem(i, sourceStack, false);
            if (ItemStackUtils.isEmpty(sourceStack))
            {
                return sourceStack;
            }
        }

        return sourceStack;
    }

    /**
     * 合并给定源 {@link IItemHandler} 中的 ItemStack 到给定目标 {@link IItemHandler} 中的方法。尝试合并 ItemStack，如果不可能则返回堆栈。
     *
     * @param sourceHandler 作为源的 {@link IItemHandler}。
     * @param sourceIndex   要提取的槽位的索引。
     * @param targetHandler 作为目标的 {@link IItemHandler}。
     */
    public static void mergeItemStackIntoNextBestSlotInItemHandlers(
            @NotNull final IItemHandler sourceHandler,
            final int sourceIndex,
            @NotNull final IItemHandler targetHandler)
    {
        ItemStack sourceStack = sourceHandler.extractItem(sourceIndex, Integer.MAX_VALUE, true);
        int amount = sourceStack.getCount();

        if (ItemStackUtils.isEmpty(sourceStack))
        {
            return;
        }

        for (int i = 0; i < targetHandler.getSlots(); i++)
        {
            if (!ItemStackUtils.isEmpty(targetHandler.getStackInSlot(i)) && ItemStackUtils.compareItemStacksIgnoreStackSize(targetHandler.getStackInSlot(i), sourceStack))
            {
                sourceStack = targetHandler.insertItem(i, sourceStack, false);
                if (ItemStackUtils.isEmpty(sourceStack))
                {
                    sourceHandler.extractItem(sourceIndex, Integer.MAX_VALUE, false);
                    return;
                }
            }
        }

        sourceHandler.extractItem(sourceIndex, amount - sourceStack.getCount(), false);
    }

    /**
     * 合并给定源 {@link IItemHandler} 中的 ItemStack 到给定目标 {@link IItemHandler} 中的方法。尝试合并 ItemStack，如果不可能则返回堆栈。
     *
     * @param stack         要添加的堆栈。
     * @param targetHandler 作为目标的 {@link IItemHandler}。
     * @return 如果交换成功，则返回 true；否则返回 false。
     */
    public static ItemStack mergeItemStackIntoNextBestSlotInItemHandlers(
      final ItemStack stack,
      @NotNull final IItemHandler targetHandler)
    {
        if (ItemStackUtils.isEmpty(stack))
        {
            return stack;
        }
        ItemStack sourceStack = stack.copy();

        for (int i = 0; i < targetHandler.getSlots(); i++)
        {
            if (!ItemStackUtils.isEmpty(targetHandler.getStackInSlot(i)) && ItemStackUtils.compareItemStacksIgnoreStackSize(targetHandler.getStackInSlot(i), sourceStack))
            {
                sourceStack = targetHandler.insertItem(i, sourceStack, false);
                if (ItemStackUtils.isEmpty(sourceStack))
                {
                    return sourceStack;
                }
            }
        }
        return sourceStack;
    }

    public static boolean transferXOfFirstSlotInProviderWithIntoNextFreeSlotInProvider(
      @NotNull final ICapabilityProvider sourceProvider,
      @NotNull final Predicate<ItemStack> itemStackSelectionPredicate,
      @NotNull final int amount, @NotNull final ICapabilityProvider targetProvider)
    {
        return transferXOfFirstSlotInProviderWithIntoNextFreeSlotInProviderWithResult(sourceProvider, itemStackSelectionPredicate, amount, targetProvider) == 0;
    }

    public static int transferXOfFirstSlotInProviderWithIntoNextFreeSlotInProviderWithResult(
      @NotNull final ICapabilityProvider sourceProvider,
      @NotNull final Predicate<ItemStack> itemStackSelectionPredicate,
      @NotNull final int amount, @NotNull final ICapabilityProvider targetProvider)
    {
        int currentAmount = amount;

        for (final IItemHandler handler : getItemHandlersFromProvider(targetProvider))
        {
            currentAmount = transferXOfFirstSlotInProviderWithIntoNextFreeSlotInItemHandlerWithResult(sourceProvider, itemStackSelectionPredicate, amount, handler);

            if (currentAmount <= 0)
            {
                return 0;
            }
        }

        return currentAmount;
    }

    public static boolean transferXOfFirstSlotInProviderWithIntoNextFreeSlotInItemHandler(
      @NotNull final ICapabilityProvider sourceProvider,
      @NotNull final Predicate<ItemStack> itemStackSelectionPredicate,
      final int amount, @NotNull final IItemHandler targetHandler)
    {
        return transferXOfFirstSlotInProviderWithIntoNextFreeSlotInItemHandlerWithResult(sourceProvider, itemStackSelectionPredicate, amount, targetHandler) == 0;
    }

    public static int transferXOfFirstSlotInProviderWithIntoNextFreeSlotInItemHandlerWithResult(
      @NotNull final ICapabilityProvider sourceProvider,
      @NotNull final Predicate<ItemStack> itemStackSelectionPredicate,
      final int amount, @NotNull final IItemHandler targetHandler)
    {
        int currentAmount = amount;
        for (final IItemHandler handler : getItemHandlersFromProvider(sourceProvider))
        {
            currentAmount = transferXOfFirstSlotInItemHandlerWithIntoNextFreeSlotInItemHandlerWithResult(handler, itemStackSelectionPredicate, currentAmount, targetHandler);

            if (currentAmount <= 0)
            {
                return 0;
            }
        }

        return currentAmount;
    }

    public static boolean transferXOfFirstSlotInItemHandlerWithIntoNextFreeSlotInItemHandler(
      @NotNull final IItemHandler sourceHandler,
      @NotNull final Predicate<ItemStack> itemStackSelectionPredicate,
      final int amount, @NotNull final IItemHandler targetHandler)
    {
        return transferXOfFirstSlotInItemHandlerWithIntoNextFreeSlotInItemHandlerWithResult(sourceHandler, itemStackSelectionPredicate, amount, targetHandler) == 0;
    }

    public static int transferXOfFirstSlotInItemHandlerWithIntoNextFreeSlotInItemHandlerWithResult(
      @NotNull final IItemHandler sourceHandler,
      @NotNull final Predicate<ItemStack> itemStackSelectionPredicate,
      final int amount, @NotNull final IItemHandler targetHandler)
    {
        int currentAmount = amount;
        int slot = 0;
        while (currentAmount > 0 && slot < sourceHandler.getSlots())
        {
            final int desiredItemSlot = InventoryUtils.findFirstSlotInItemHandlerNotEmptyWith(sourceHandler,
              itemStackSelectionPredicate::test);

            if (desiredItemSlot == -1)
            {
                return currentAmount;
            }

            final ItemStack returnStack = sourceHandler.extractItem(desiredItemSlot, currentAmount, false);

            if (!ItemStackUtils.isEmpty(returnStack))
            {
                if (!InventoryUtils.addItemStackToItemHandler(targetHandler, returnStack))
                {
                    sourceHandler.insertItem(desiredItemSlot, returnStack, false);
                    break;
                }
                // Only reduce if successfully inserted.
                currentAmount -= returnStack.getCount();
            }

            slot++;
        }

        return currentAmount;
    }
    /**
     * 接受与谓词匹配的项目并将其从一个处理器移动到另一个处理器的特定插槽。
     *
     * @param sourceHandler               源处理器。
     * @param itemStackSelectionPredicate 项目谓词。
     * @param amount                      最大提取数量
     * @param targetHandler               目标处理器。
     * @param slot                        要放置的插槽。
     * @return 实际转移的项目计数
     */
    public static int transferXInItemHandlerIntoSlotInItemHandler(
            final IItemHandler sourceHandler,
            final Predicate<ItemStack> itemStackSelectionPredicate,
            final int amount,
            final IItemHandler targetHandler, final int slot)
    {
        int actualTransferred = 0;
        while (actualTransferred < amount)
        {
            final int transferred = InventoryUtils.transferXOfFirstSlotInItemHandlerWithIntoInItemHandler(
                    sourceHandler,
                    itemStackSelectionPredicate,
                    amount - actualTransferred,
                    targetHandler,
                    slot);
            if (transferred <= 0)
            {
                break;
            }
            actualTransferred += transferred;
        }
        return actualTransferred;
    }

    /**
     * 接受与谓词匹配的项目并将其从一个处理器移动到另一个处理器的特定插槽。
     *
     * @param sourceHandler               源处理器。
     * @param itemStackSelectionPredicate 项目谓词。
     * @param amount                      最大提取数量
     * @param targetHandler               目标处理器。
     * @param slot                        要放置的插槽。
     * @return 实际转移的项目计数
     */
    public static int transferXOfFirstSlotInItemHandlerWithIntoInItemHandler(
            final IItemHandler sourceHandler,
            final Predicate<ItemStack> itemStackSelectionPredicate,
            final int amount,
            final IItemHandler targetHandler, final int slot)
    {
        final int desiredItemSlot = InventoryUtils.findFirstSlotInItemHandlerNotEmptyWith(sourceHandler,
                itemStackSelectionPredicate);

        if (desiredItemSlot == -1)
        {
            return 0;
        }
        final ItemStack returnStack = sourceHandler.extractItem(desiredItemSlot, amount, false);
        if (ItemStackUtils.isEmpty(returnStack))
        {
            return 0;
        }

        final ItemStack insertResult = targetHandler.insertItem(slot, returnStack, false);
        if (!ItemStackUtils.isEmpty(insertResult))
        {
            sourceHandler.insertItem(desiredItemSlot, insertResult, false);
            return returnStack.getCount() - insertResult.getCount();
        }
        return returnStack.getCount();
    }

    /**
     * 从给定的源{@link ICapabilityProvider}向给定的目标{@link IItemHandler}交换ItemStacks的方法。
     *
     * @param sourceProvider 源。
     * @param sourceIndex    正在提取的插槽的索引。
     * @param targetHandler  目标。
     * @return 成功时为true，否则为false。
     */
    public static boolean transferItemStackIntoNextFreeSlotFromProvider(
            @NotNull final ICapabilityProvider sourceProvider,
            @NotNull final int sourceIndex,
            @NotNull final IItemHandler targetHandler)
    {
        for (final IItemHandler handler : getItemHandlersFromProvider(sourceProvider))
        {
            if (transferItemStackIntoNextFreeSlotInItemHandler(handler, sourceIndex, targetHandler))
            {
                return true;
            }
        }

        return false;
    }

    /**
     * 从给定的源{@link IItemHandler}向给定的目标{@link IItemHandler}交换ItemStacks的方法。
     *
     * @param handler            源。
     * @param stackPredicate     要提取的堆栈类型。
     * @param count              要提取的数量。
     * @param targetHandler      目标。
     * @return 成功时为true，否则为false。
     */
    public static boolean transferItemStackIntoNextFreeSlotFromItemHandler(
            @NotNull final IItemHandler handler,
            @NotNull final Predicate<ItemStack> stackPredicate,
            final int count,
            @NotNull final IItemHandler targetHandler)
    {
        int totalCount = count;

        int index = findFirstSlotInItemHandlerWith(handler, stackPredicate);
        while (index != -1)
        {
            final int localCount = Math.min(totalCount, handler.getStackInSlot(index).getCount());
            if (transferXOfItemStackIntoNextFreeSlotInItemHandler(handler, index, localCount, targetHandler))
            {
                totalCount -= localCount;
            }

            if (totalCount <= 0)
            {
                return true;
            }
            index = findFirstSlotInItemHandlerWith(handler, stackPredicate);
        }

        return false;
    }

    /**
     * 从给定的源{@link ICapabilityProvider}向给定的目标{@link IItemHandler}交换指定数量的ItemStacks的方法。
     *
     * @param sourceProvider 源。
     * @param sourceIndex    正在提取的插槽的索引。
     * @param count          数量。
     * @param targetHandler  目标。
     * @return 成功时为true，否则为false。
     */
    public static boolean transferXOfItemStackIntoNextFreeSlotFromProvider(
            @NotNull final ICapabilityProvider sourceProvider,
            final int sourceIndex,
            final int count,
            @NotNull final IItemHandler targetHandler)
    {
        for (final IItemHandler handler : getItemHandlersFromProvider(sourceProvider))
        {
            if (transferXOfItemStackIntoNextFreeSlotInItemHandler(handler, sourceIndex, count, targetHandler))
            {
                return true;
            }
        }

        return false;
    }

    /**
     * 从给定的源{@link ICapabilityProvider}向给定的目标{@link IItemHandler}交换ItemStacks的方法。
     *
     * @param sourceProvider 源。
     * @param sourceIndex    正在提取的插槽的索引。
     * @param targetHandler  目标。
     * @return 成功时为true，否则为false。
     */
    public static boolean transferItemStackIntoNextBestSlotFromProvider(
            @NotNull final ICapabilityProvider sourceProvider,
            final int sourceIndex,
            @NotNull final IItemHandler targetHandler)
    {
        for (final IItemHandler handler : getItemHandlersFromProvider(sourceProvider))
        {
            if (transferItemStackIntoNextBestSlotInItemHandler(handler, sourceIndex, targetHandler))
            {
                return true;
            }
        }

        return false;
    }

    /**
     * 从给定的源{@link IItemHandler}向给定的目标{@link IItemHandler}交换ItemStacks的方法。
     *
     * @param sourceHandler  源。
     * @param sourceIndex    正在提取的插槽的索引。
     * @param targetHandler  目标。
     * @param targetIndex    正在插入的插槽的索引。
     * @return 成功时为true，否则为false。
     */
    public static boolean swapItemStacksInItemHandlers(
            @NotNull final IItemHandler sourceHandler,
            @NotNull final int sourceIndex,
            @NotNull final IItemHandler targetHandler,
            @NotNull final int targetIndex)
    {
        final ItemStack targetStack = targetHandler.extractItem(targetIndex, Integer.MAX_VALUE, false);
        final ItemStack sourceStack = sourceHandler.extractItem(sourceIndex, Integer.MAX_VALUE, true);

        final ItemStack resultSourceSimulationInsertion = targetHandler.insertItem(targetIndex, sourceStack, true);
        if (ItemStackUtils.isEmpty(resultSourceSimulationInsertion) || ItemStackUtils.isEmpty(targetStack))
        {
            targetHandler.insertItem(targetIndex, sourceStack, false);
            sourceHandler.extractItem(sourceIndex, Integer.MAX_VALUE, false);
            sourceHandler.insertItem(sourceIndex, targetStack, false);

            return true;
        }
        else
        {
            targetHandler.insertItem(targetIndex, targetStack, false);

            return false;
        }
    }

    /**
     * 从给定提供者中移除一组堆栈。
     *
     * @param provider 提供者。
     * @param input    堆栈列表。
     * @return 如果成功则为true。
     */
    public static boolean removeStacksFromProvider(final ICapabilityProvider provider, final List<ItemStack> input)
    {
        for (final IItemHandler handler : getItemHandlersFromProvider(provider))
        {
            if (!removeStacksFromItemHandler(handler, input))
            {
                return false;
            }
        }

        return true;
    }

    /**
     * 从给定的Itemhandler中移除一组堆栈。
     *
     * @param handler Itemhandler。
     * @param input   堆栈列表。
     * @return 如果成功则为true。
     */
    public static boolean removeStacksFromItemHandler(final IItemHandler handler, final List<ItemStack> input)
    {
        final List<ItemStack> list = new ArrayList<>();
        int maxTries = 0;
        for (final ItemStack stack : input)
        {
            maxTries += ItemStackUtils.getSize(stack);
            list.add(stack.copy());
        }

        boolean success = true;
        int i = 0;
        int tries = 0;
        while (i < list.size() && tries < maxTries)
        {
            final ItemStack stack = list.get(i);
            final int slot = findFirstSlotInItemHandlerNotEmptyWith(handler, lStack -> ItemStackUtils.compareItemStacksIgnoreStackSize(stack, lStack));

            if (slot == -1)
            {
                success = false;
                i++;
                continue;
            }

            final int removedSize = ItemStackUtils.getSize(handler.extractItem(slot, ItemStackUtils.getSize(stack), false));

            if (removedSize == ItemStackUtils.getSize(stack))
            {
                i++;
            }
            else
            {
                ItemStackUtils.changeSize(stack, -removedSize);
            }
            tries++;
        }

        return success && i >= list.size();
    }

    /**
     * 尝试从给定的Itemhandler中移除一个堆栈及其大小。仅在可以完全移除整个大小时才会移除。
     *
     * @param handler Itemhandler。
     * @param input   要移除的堆栈。
     * @return 如果已移除堆栈则为true。
     */
    public static boolean tryRemoveStackFromItemHandler(final IItemHandler handler, final ItemStack input)
    {
        int amount = input.getCount();

        for (int i = 0; i < handler.getSlots(); i++)
        {
            if (ItemStackUtils.compareItemStacksIgnoreStackSize(handler.getStackInSlot(i), input))
            {
                amount = amount - handler.extractItem(i, amount, false).getCount();

                if (amount == 0)
                {
                    return true;
                }
            }
        }

        final ItemStack revertStack = input.copy();
        revertStack.setCount(input.getCount() - amount);
        addItemStackToItemHandler(handler, revertStack);
        return false;
    }

    /**
     * 强制从给定的Itemhandler中移除一个堆栈的特定数量。
     *
     * @param handler Itemhandler。
     * @param input   要移除的堆栈。
     * @param count   要移除的数量。
     */
    public static void removeStackFromItemHandler(final IItemHandler handler, final ItemStack input, final int count)
    {
        final ItemStack workingStack = input.copy();
        int localCount = count;
        int tries = 0;
        while (tries < count)
        {
            final int slot = findFirstSlotInItemHandlerNotEmptyWith(handler, stack -> ItemStackUtils.compareItemStacksIgnoreStackSize(workingStack, stack));
            if (slot == -1)
            {
                return;
            }

            final int removedSize = ItemStackUtils.getSize(handler.extractItem(slot, localCount, false));

            if (removedSize == count)
            {
                return;
            }
            else
            {
                localCount -= removedSize;
            }
            tries++;
        }
    }

    /**
     * 检查提供者中是否存在某个项目，但提供者未满。
     *
     * @param provider 提供者。
     * @param item     项目。
     * @param amount   要考虑的堆叠大小。
     * @return 插槽或-1。
     */
    public static int findSlotInProviderNotFullWithItem(final ICapabilityProvider provider, final Item item, final int amount)
    {
        for (final IItemHandler handler : getItemHandlersFromProvider(provider))
        {
            final int foundSlot = findSlotInItemHandlerNotFullWithItem(handler, (ItemStack stack) -> compareItems(stack, item), amount);
            //TODO: 当合同稍后变得更加坚固时，将此-1检查替换为try-catch块。
            if (foundSlot > -1)
            {
                return foundSlot;
            }
        }

        return -1;
    }

    /**
     * 检查处理器中是否存在某个项目，但处理器未满。一旦找到空插槽和匹配的插槽，立即返回。返回最后找到的匹配插槽。
     *
     * @param handler                     处理器。
     * @param itemStackSelectionPredicate 选择谓词。
     * @param amount                      要考虑的堆叠大小。
     * @return 插槽或-1。
     */
    public static int findSlotInItemHandlerNotFullWithItem(
      final IItemHandler handler,
      @NotNull final Predicate<ItemStack> itemStackSelectionPredicate,
      final int amount)
    {
        boolean foundEmptySlot = false;
        boolean foundItem = false;
        int itemSlot = -1;
        for (int slot = 0; slot < handler.getSlots(); slot++)
        {
            final ItemStack stack = handler.getStackInSlot(slot);
            if (ItemStackUtils.isEmpty(stack))
            {
                foundEmptySlot = true;
            }
            else if (itemStackSelectionPredicate.test(stack))
            {
                if (ItemStackUtils.getSize(stack) + amount <= stack.getMaxStackSize())
                {
                    foundEmptySlot = true;
                }
                foundItem = true;
                itemSlot = slot;
            }

            if (foundItem && foundEmptySlot)
            {
                return itemSlot;
            }
        }

        return -1;
    }

    /**
     * Check if a similar item is in the handler but without the provider being full. Return as soon as an empty slot and a matching slot has been found. Returns the last matching
     * slot it found.
     *
     * @param handler the handler to check.
     * @param inStack the ItemStack
     * @return true if fitting.
     */
    public static boolean findSlotInItemHandlerNotFullWithItem(
      final IItemHandler handler,
      final ItemStack inStack)
    {
        if (handler == null)
        {
            return false;
        }

        boolean foundEmptySlot = false;
        boolean foundItem = false;
        for (int slot = 0; slot < handler.getSlots(); slot++)
        {
            final ItemStack stack = handler.getStackInSlot(slot);
            if (ItemStackUtils.isEmpty(stack))
            {
                foundEmptySlot = true;
            }
            else if (compareItems(stack, inStack.getItem()))
            {
                if (ItemStackUtils.getSize(stack) + ItemStackUtils.getSize(inStack) <= stack.getMaxStackSize())
                {
                    foundEmptySlot = true;
                }
                foundItem = true;
            }

            if (foundItem && foundEmptySlot)
            {
                return true;
            }
        }

        return false;
    }

    /**
     * Drop an actual itemHandler in the world.
     *
     * @param handler the handler.
     * @param world   the world.
     * @param x       the x pos.
     * @param y       the y pos.
     * @param z       the z pos.
     */
    public static void dropItemHandler(final IItemHandler handler, final Level world, final int x, final int y, final int z)
    {
        for (int i = 0; i < handler.getSlots(); ++i)
        {
            final ItemStack itemstack = handler.getStackInSlot(i);

            if (itemstack != null)
            {
                spawnItemStack(world, x, y, z, itemstack);
            }
        }
    }

    /**
     * Attempt to transfer as much item as possible from origin to target inventory
     *
     * @param origin the handler.
     * @param target   the world.
     * @return true if all item transfered, false if some item remain in origin
     */
    public static boolean transferAllItemHandler(final IItemHandler origin, final IItemHandler target)
    {
        for (int i = 0; i < origin.getSlots(); ++i)
        {
            final ItemStack itemStack = origin.getStackInSlot(i);
            if(!ItemStackUtils.isEmpty(itemStack))
            {
                if(addItemStackToItemHandler(target, itemStack))
                {
                    removeStackFromItemHandler(origin, itemStack, itemStack.getCount());
                }
                else return false;
            }
        }

        return true;
    }

    /**
     * Spawn an itemStack in the world.
     *
     * @param worldIn the world.
     * @param x       the x pos.
     * @param y       the y pos.
     * @param z       the z pos.
     * @param stack   the stack to drop.
     */
    public static void spawnItemStack(final Level worldIn, final double x, final double y, final double z, final ItemStack stack)
    {
        final Random random = new Random();
        final double spawnX = random.nextDouble() * SPAWN_MODIFIER + SPAWN_ADDITION;
        final double spawnY = random.nextDouble() * SPAWN_MODIFIER + SPAWN_ADDITION;
        final double spawnZ = random.nextDouble() * SPAWN_MODIFIER + SPAWN_ADDITION;

        while (stack.getCount() > 0)
        {
            final int randomSplitStackSize = random.nextInt(MAX_RANDOM_SPAWN) + MIN_RANDOM_SPAWN;
            final ItemEntity ItemEntity = new ItemEntity(worldIn, x + spawnX, y + spawnY, z + spawnZ, stack.split(randomSplitStackSize));

            ItemEntity.setDeltaMovement(random.nextGaussian() * MOTION_MULTIPLIER, random.nextGaussian() * MOTION_MULTIPLIER + MOTION_Y_MIN, random.nextGaussian() * MOTION_MULTIPLIER);
            worldIn.addFreshEntity(ItemEntity);
        }
    }

    /**
     * Calculates howmany items match the given predicate that are in the list.
     *
     * @param stacks         the stacks to count in.
     * @param stackPredicate the condition to count for.
     * @return The sum of the itemstack sizes that match the predicate
     */
    public static int getItemCountInStackLick(@NotNull final List<ItemStack> stacks, @NotNull final Predicate<ItemStack> stackPredicate)
    {
        return stacks.stream().filter(ItemStackUtils::isNotEmpty).filter(stackPredicate).mapToInt(ItemStackUtils::getSize).sum();
    }

    /**
     * Checks if all stacks given in the list are in the itemhandler given
     *
     * @param stacks  The stacks that should be in the itemhandler
     * @param handler The itemhandler to check in
     * @return True when all stacks are in the handler, false when not
     */
    public static boolean areAllItemsInItemHandler(@NotNull final List<ItemStack> stacks, @NotNull final IItemHandler handler)
    {
        return areAllItemsInItemHandlerList(stacks, ImmutableList.of(handler));
    }

    /**
     * Checks if all stacks given in the list are in the capability provider given
     *
     * @param stacks   The stacks that should be in the itemhandler
     * @param provider The provider to check in
     * @return True when all stacks are in the handler, false when not
     */
    public static boolean areAllItemsInProvider(@NotNull final List<ItemStack> stacks, @NotNull final ICapabilityProvider provider)
    {
        return areAllItemsInItemHandlerList(stacks, getItemHandlersFromProvider(provider));
    }

    /**
     * Checks if all stacks given in the list are in at least one of the given the itemhandlers
     *
     * @param stacks   The stacks that should be in the itemhandlers
     * @param handlers The itemhandlers to check in
     * @return True when all stacks are in at least one of the handlers, false when not
     */
    public static boolean areAllItemsInItemHandlerList(@NotNull final List<ItemStack> stacks, @NotNull final Collection<IItemHandler> handlers)
    {
        if (stacks.isEmpty())
        {
            return true;
        }

        if (handlers.isEmpty())
        {
            return false;
        }

        final Map<ItemStack, Integer> requiredCountForStacks = getMergedCountedStacksFromList(stacks);

        return requiredCountForStacks.keySet().stream().allMatch(itemStack -> {
            final int countInHandlerList =
              handlers.stream().mapToInt(handler -> getItemCountInItemHandler(handler, itemStack1 -> ItemStackUtils.compareItemStacksIgnoreStackSize(itemStack, itemStack1))).sum();
            return countInHandlerList >= requiredCountForStacks.get(itemStack);
        });
    }

    /**
     * This method calculates the amount of items in itemstacks are contained within a list.
     *
     * @param stacks The stacks to count.
     * @return A map with a entry for each unique unified itemstack and its count in the list.
     */
    public static Map<ItemStack, Integer> getMergedCountedStacksFromList(@NotNull final List<ItemStack> stacks)
    {
        final Map<ItemStack, Integer> requiredCountForStacks = Maps.newHashMap();
        stacks.forEach(targetStack -> {
            final Optional<ItemStack>
              alreadyContained = requiredCountForStacks.keySet().stream().filter(itemStack -> ItemStackUtils.compareItemStacksIgnoreStackSize(itemStack, targetStack)).findFirst();

            if (alreadyContained.isPresent())
            {
                requiredCountForStacks.put(alreadyContained.get(), requiredCountForStacks.get(alreadyContained.get()) + targetStack.getCount());
            }
            else
            {
                requiredCountForStacks.put(targetStack, targetStack.getCount());
            }
        });

        return requiredCountForStacks;
    }

    /**
     * This method splits a map with an entry for each unique unified itemstack and its count into a list of itemstacks that represent the maps, taken the max stack size into
     * account.
     *
     * @param mergedCountedStacks the map with the unique unified itemstacks and their counts.
     * @return The list of itemstacks that represent the map, taken the max stack size into account.
     */
    public static List<ItemStack> splitMergedCountedStacksIntoMaxContentStacks(@NotNull final Map<ItemStack, Integer> mergedCountedStacks)
    {
        final List<ItemStack> list = Lists.newArrayList();
        for (final Map.Entry<ItemStack, Integer> itemStackIntegerEntry : mergedCountedStacks.entrySet())
        {
            final int minimalFullStacks = itemStackIntegerEntry.getValue() / itemStackIntegerEntry.getKey().getMaxStackSize();
            final int residualStackSize = itemStackIntegerEntry.getValue() % itemStackIntegerEntry.getKey().getMaxStackSize();

            for (int i = 0; i < minimalFullStacks; i++)
            {
                final ItemStack tobeAdded = itemStackIntegerEntry.getKey().copy();
                tobeAdded.setCount(tobeAdded.getMaxStackSize());

                list.add(tobeAdded);
            }

            if (residualStackSize > 0)
            {
                final ItemStack tobeAdded = itemStackIntegerEntry.getKey().copy();
                tobeAdded.setCount(residualStackSize);

                list.add(tobeAdded);
            }
        }

        return list;
    }

    /**
     * Searches a given itemhandler for the stacks given and returns the list that is contained in the itemhandler
     *
     * @param stacks  The stacks to search for
     * @param handler The handler to search in
     * @return The sublist of the stacks list contained in the itemhandler.
     */
    public static List<ItemStack> getContainedFromItemHandler(@NotNull final List<ItemStack> stacks, @NotNull final IItemHandler handler)
    {
        final List<ItemStack> result = Lists.newArrayList();

        final Map<ItemStack, Integer> inputCounts = getMergedCountedStacksFromList(stacks);
        final Map<ItemStack, Integer> inventoryCounts = getMergedCountedStacksFromList(getItemHandlerAsList(handler));

        final Map<ItemStack, Integer> resultingContained = new HashMap<>();
        inputCounts
          .forEach((itemStack, count) -> {

              int remainingCount = count;
              for (Map.Entry<ItemStack, Integer> entry : inventoryCounts.entrySet())
              {
                  ItemStack containedStack = entry.getKey();
                  final Integer containedCount = entry.getValue();
                  if (ItemStackUtils.compareItemStacksIgnoreStackSize(itemStack, containedStack))
                  {
                      remainingCount -= containedCount;
                  }
              }

              if (remainingCount <= 0)
              {
                  resultingContained.put(itemStack, count);
              }
          });

        resultingContained
          .forEach((itemStack, count) -> {
              final int fullStackCount = count / itemStack.getMaxStackSize();
              final int missingPartialCount = count % itemStack.getMaxStackSize();

              for (int i = 0; i < fullStackCount; i++)
              {
                  final ItemStack targetStack = itemStack.copy();
                  targetStack.setCount(targetStack.getMaxStackSize());

                  result.add(targetStack);
              }

              if (missingPartialCount != 0)
              {
                  final ItemStack targetStack = itemStack.copy();
                  targetStack.setCount(missingPartialCount);

                  result.add(targetStack);
              }
          });

        return result;
    }

    /**
     * Unifies a list of stacks so that they are all packed to together to the max stack size.
     *
     * @param stacks The stacks to pack.
     * @return The packed stacks
     */
    public static List<ItemStack> processItemStackListAndMerge(@NotNull final List<ItemStack> stacks)
    {
        return splitMergedCountedStacksIntoMaxContentStacks(getMergedCountedStacksFromList(stacks));
    }

    /**
     * Attempts a swap with the given itemstacks, from the source to the target inventory. Itemstacks in the target that match the given toKeepInTarget predicate will not be
     * swapped out, if swapping is needed
     *
     * @param targetInventory   The target inventory.
     * @param sourceInventories The source inventory.
     * @param toSwap            The list of stacks to swap.
     * @param toKeepInTarget    The predicate that determines what not to swap in the target.
     * @return True when moving was successfull, false when not
     */
    public static boolean moveItemStacksWithPossibleSwap(
      @NotNull final IItemHandler targetInventory,
      @NotNull final Collection<IItemHandler> sourceInventories,
      @NotNull final List<ItemStack> toSwap,
      @NotNull final Predicate<ItemStack> toKeepInTarget)
    {
        if (targetInventory.getSlots() < toSwap.size())
        {
            return false;
        }

        final Predicate<ItemStack> wantToKeep = toKeepInTarget.or(stack -> ItemStackUtils.compareItemStackListIgnoreStackSize(toSwap, stack));

        for (final ItemStack itemStack : toSwap)
        {
            for (final IItemHandler sourceInventory : sourceInventories)
            {
                if (tryRemoveStackFromItemHandler(sourceInventory, itemStack))
                {
                    ItemStack forcingResult = forceItemStackToItemHandler(targetInventory, itemStack, wantToKeep);

                    if (forcingResult != null && !forcingResult.isEmpty())
                    {
                        addItemStackToItemHandler(sourceInventory, forcingResult);
                    }
                }
            }
            return false;
        }

        return true;
    }

    /**
     * Search for a certain itemStack in the inventory and decrease it by 1.
     *
     * @param invWrapper the inventory item handler.
     * @param itemStack  the itemStack to decrease.
     */
    public static void reduceStackInItemHandler(final IItemHandler invWrapper, final ItemStack itemStack)
    {
        reduceStackInItemHandler(invWrapper, itemStack, 1);
    }

    /**
     * Search for a certain itemStack in the inventory and decrease it by a certain quantity.
     *
     * @param invWrapper the inventory item handler.
     * @param itemStack  the itemStack to decrease.
     * @param quantity   the quantity.
     */
    public static void reduceStackInItemHandler(final IItemHandler invWrapper, final ItemStack itemStack, final int quantity)
    {
        for (int i = 0; i < invWrapper.getSlots(); i++)
        {
            if (ItemStackUtils.compareItemStacksIgnoreStackSize(invWrapper.getStackInSlot(i), itemStack))
            {
                invWrapper.getStackInSlot(i).shrink(quantity);
                return;
            }
        }
    }

    /**
     * Search for a certain itemStack in the inventory and decrease it by a certain quantity.
     *
     * @param invWrapper the inventory item handler.
     * @param itemStack  the itemStack to decrease.
     * @param quantity   the quantity.
     * @return true if successfully.
     */
    public static boolean attemptReduceStackInItemHandler(final IItemHandler invWrapper, final ItemStack itemStack, final int quantity)
    {
        return attemptReduceStackInItemHandler(invWrapper, itemStack, quantity, false, false);
    }

    /**
     * Search for a certain itemStack in the inventory and decrease it by a certain quantity.
     *
     * @param invWrapper the inventory item handler.
     * @param itemStack  the itemStack to decrease.
     * @param quantity   the quantity.
     * @param ignoreDamage ignore damage values.
     * @param ignoreNBT ignore NBT values.
     * @return true if successfully.
     */
    public static boolean attemptReduceStackInItemHandler(final IItemHandler invWrapper, final ItemStack itemStack, final int quantity, final boolean ignoreDamage, final boolean ignoreNBT)
    {
        if (getItemCountInItemHandler(invWrapper, stack -> !stack.isEmpty() && ItemStackUtils.compareItemStacksIgnoreStackSize(stack, itemStack, !ignoreDamage, !ignoreNBT)) < quantity)
        {
            return false;
        }

        int qty = quantity;
        for (int i = 0; i < invWrapper.getSlots(); i++)
        {
            final ItemStack stack = invWrapper.getStackInSlot(i);
            if (ItemStackUtils.compareItemStacksIgnoreStackSize(stack, itemStack, !ignoreDamage, !ignoreNBT))
            {
                if (stack.getCount() >= qty)
                {
                    invWrapper.extractItem(i, qty, false);
                    return true;
                }
                else
                {
                    qty -= stack.getCount();
                    invWrapper.extractItem(i, stack.getCount(), false);
                }
            }
        }
        return false;
    }

    /**
     * Sums up all items in the given provider/handlers
     *
     * @param provider inventory provider
     * @param handlers inventory handlers
     * @return Map of IdentityItemstorage
     */
    public static Map<ItemStorage, ItemStorage> getAllItemsForProviders(final ICapabilityProvider provider, final IItemHandler... handlers)
    {
        final Set<IItemHandler> providerHandlers = getItemHandlersFromProvider(provider);
        if (handlers != null)
        {
            providerHandlers.addAll(Arrays.asList(handlers));
        }

        return getAllItemsForProviders(providerHandlers);
    }

    /**
     * Sums up all items in the given handlers
     *
     * @param handlers inventory handlers
     * @return Map of IdentityItemstorage
     */
    public static Map<ItemStorage, ItemStorage> getAllItemsForProviders(final IItemHandler... handlers)
    {
        return getAllItemsForProviders(new HashSet<>(Arrays.asList(handlers)));
    }

    /**
     * Sums up all items in the given handlers
     *
     * @param handlerList inventory handlers
     * @return Map of IdentityItemstorage
     */
    public static Map<ItemStorage, ItemStorage> getAllItemsForProviders(Set<IItemHandler> handlerList)
    {
        final Map<ItemStorage, ItemStorage> storageMap = new HashMap<>();
        for (final IItemHandler handler : handlerList)
        {
            for (int i = 0; i < handler.getSlots(); i++)
            {
                final ItemStack containedStack = handler.getStackInSlot(i);
                if (!ItemStackUtils.isEmpty(containedStack))
                {
                    final ItemStorage storage = new ItemStorage(containedStack.copy(), false, false);

                    if (storageMap.containsKey(storage))
                    {
                        final ItemStorage existing = storageMap.get(storage);
                        existing.setAmount(existing.getAmount() + storage.getAmount());
                    }
                    else
                    {
                        storageMap.put(storage, storage);
                    }
                }
            }
        }

        return storageMap;
    }

    /**
     * Returns whether two sets of itemstorage has the same content
     *
     * @param first     First set of item storages
     * @param second    Second set of item storages
     * @param showTrace whther to print a stacktrace on false
     * @return true if matching
     */
    public static boolean doStorageSetsMatch(Map<ItemStorage, ItemStorage> first, Map<ItemStorage, ItemStorage> second, boolean showTrace)
    {
        for (final ItemStorage storage : first.keySet())
        {
            final ItemStorage compareStorage = second.get(storage);

            if (compareStorage == null || storage.getAmount() != compareStorage.getAmount())
            {
                if (showTrace)
                {
                    Log.getLogger().warn("Possible inventory issue, not matching:", new Exception());
                }
                return false;
            }
        }

        for (final ItemStorage storage : second.keySet())
        {
            final ItemStorage compareStorage = first.get(storage);

            if (compareStorage == null || storage.getAmount() != compareStorage.getAmount())
            {
                if (showTrace)
                {
                    Log.getLogger().warn("Possible inventory issue, not matching:", new Exception());
                }
                return false;
            }
        }

        return true;
    }

    /**
     * Transfers food items from the source with the required saturation value, or as much as possible.
     *
     * @param source             to extract items from
     * @param target             to insert intems into
     * @param requiredSaturation required saturation value
     * @param foodPredicate      food choosing predicate
     * @return true if any food was transferred
     */
    public static boolean transferFoodUpToSaturation(
      final ICapabilityProvider source,
      final IItemHandler target,
      final int requiredSaturation,
      final Predicate<ItemStack> foodPredicate)
    {
        Set<IItemHandler> handlers = getItemHandlersFromProvider(source);

        int foundSaturation = 0;

        for (final IItemHandler handler : handlers)
        {
            for (int i = 0; i < handler.getSlots(); i++)
            {
                final ItemStack stack = handler.getStackInSlot(i);

                if (!ItemStackUtils.isEmpty(stack) && foodPredicate.test(stack))
                {
                    // Found food
                    final FoodProperties itemFood = stack.getItem().getFoodProperties();
                    if (itemFood == null)
                    {
                        continue;
                    }

                    int amount = (int) Math.round(Math.ceil((requiredSaturation - foundSaturation) / (float) itemFood.getNutrition()));

                    final ItemStack extractedFood;
                    if (amount > stack.getCount())
                    {
                        // Not enough yet
                        foundSaturation += stack.getCount() * itemFood.getNutrition();
                        extractedFood = handler.extractItem(i, stack.getCount(), false);
                    }
                    else
                    {
                        // Stack is sufficient
                        extractedFood = handler.extractItem(i, amount, false);
                        foundSaturation = requiredSaturation;
                    }

                    if (!ItemStackUtils.isEmpty(extractedFood))
                    {
                        if (!addItemStackToItemHandler(target, extractedFood))
                        {
                            // Swap if need
                            final int slot = findFirstSlotInItemHandlerNotEmptyWith(target, s -> !foodPredicate.test(s));
                            if (slot != -1)
                            {
                                final ItemStack swappedItem = target.extractItem(slot, target.getStackInSlot(slot).getCount(), false);
                                addItemStackToProvider(source, swappedItem);
                                addItemStackToItemHandler(target, extractedFood);
                            }
                        }
                    }

                    if (foundSaturation >= requiredSaturation)
                    {
                        return true;
                    }
                }
            }
        }

        return foundSaturation > 0;
    }

    /**
     * Tries to put given itemstack in hotbar and select it, fails when player inventory is full, successes otherwise.
     *
     * @param itemStack   itemstack to put into player's inv
     * @param player player entity
     * @return true if item was put into player's inv, false if dropped
     */
    public static boolean putItemToHotbarAndSelectOrDrop(final ItemStack itemStack, final Player player)
    {
        final Inventory playerInv = player.getInventory();

        final int emptySlot = playerInv.getFreeSlot();
        if (emptySlot == -1) // try full inv first
        {
            player.drop(itemStack, false);
            return false;
        }
        else
        {
            final int hotbarSlot = playerInv.getSuitableHotbarSlot();
            final ItemStack curHotbarItem = playerInv.getItem(hotbarSlot);

            // check if we need to make space first
            if (!curHotbarItem.isEmpty())
            {
                playerInv.setItem(emptySlot, curHotbarItem);
            }

            playerInv.setItem(hotbarSlot, itemStack);
            playerInv.selected = hotbarSlot;
            playerInv.setChanged();
            updateHeldItemFromServer(player);
            return true;
        }
    }

    /**
     * Tries to put given itemstack in hotbar, fails when player inventory is full, successes otherwise.
     * If fails sends a message to player about dropped item.
     *
     * @param itemStack   itemstack to put into player's inv
     * @param player player entity
     * @return true if item was put into player's inv, false if dropped
     */
    public static boolean putItemToHotbarAndSelectOrDropMessage(final ItemStack itemStack, final Player player)
    {
        final boolean result = putItemToHotbarAndSelectOrDrop(itemStack, player);

        if (!result)
        {
            MessageUtils.format(itemStack.getDisplayName().copy())
              .append(MESSAGE_INFO_PLAYER_INVENTORY_FULL_HOTBAR_INSERT)
              .sendTo(player);
        }
        return result;
    }

    /**
     * If item is already in inventory then it's moved to hotbar and returned.
     * Else {@link #putItemToHotbarAndSelectOrDrop} is called with itemstack created from given factory.
     *
     * @param item             item to search for
     * @param player           player inventory to check and use
     * @param itemStackFactory factory for new item if not found
     * @param messageOnDrop    if true message player when new item was dropped
     * @return itemstack in hotbar or dropped in front of player
     */
    public static ItemStack getOrCreateItemAndPutToHotbarAndSelectOrDrop(final Item item,
        final Player player,
        final Supplier<ItemStack> itemStackFactory,
        final boolean messageOnDrop)
    {
        final Inventory playerInv = player.getInventory();

        for (int slot = 0; slot < playerInv.items.size(); slot++)
        {
            final ItemStack itemSlot = playerInv.getItem(slot);
            if (itemSlot.getItem() == item)
            {
                if (!Inventory.isHotbarSlot(slot))
                {
                    playerInv.pickSlot(slot);
                }
                else
                {
                    playerInv.selected = slot;
                }
                playerInv.setChanged();
                updateHeldItemFromServer(player);
                return itemSlot;
            }
        }

        final ItemStack newItem = itemStackFactory.get();
        if (messageOnDrop)
        {
            putItemToHotbarAndSelectOrDropMessage(newItem, player);
        }
        else
        {
            putItemToHotbarAndSelectOrDrop(newItem, player);
        }
        return newItem;
    }

    /**
     * Updates held item slot on client. Client autoupdates server once per tick.
     *
     * @param player player to sync
     */
    private static void updateHeldItemFromServer(final Player player)
    {
        if (player instanceof ServerPlayer)
        {
            ((ServerPlayer) player).server.getPlayerList().sendAllPlayerInfo((ServerPlayer) player);
        }
    }

    /**
     * Check if there is enough of a given stack in the provider.
     * @param entity the provider.
     * @param stack the stack to count.
     * @param count the count.
     * @return true if enough.
     */
    public static boolean hasEnoughInProvider(final BlockEntity entity, final ItemStack stack, final int count)
    {
        if (entity instanceof TileEntityColonyBuilding)
        {
            return InventoryUtils.hasBuildingEnoughElseCount( ((TileEntityColonyBuilding) entity).getBuilding(), new ItemStorage(stack), stack.getCount()) >= count;
        }
        else if (entity instanceof TileEntityRack)
        {
            return ((TileEntityRack) entity).getCount(stack, false, false) >= count;
        }

        return getItemCountInProvider(entity, itemStack -> !ItemStackUtils.isEmpty(itemStack) && ItemStackUtils.compareItemStacksIgnoreStackSize(itemStack, stack, true, true)) >= count;
    }
}
