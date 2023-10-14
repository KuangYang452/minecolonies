package com.minecolonies.api.util;

import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.items.IItemHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.*;

/**
 * 用于库存的Java 8函数接口。大多数方法将重新映射参数以减少重复。由于类型擦除冲突，不支持所有组合。
 */
public final class InventoryFunctions
{
    /**
     * 隐藏默认构造函数的私有构造函数。
     */
    private InventoryFunctions()
    {
        /*
         * 故意留空。
         */
    }

    /**
     * 在库存中查找与给定断言匹配的物品堆。
     *
     * @param provider 要搜索的提供者
     * @param tester   用于测试槽位的函数
     * @return 如果找到了物品堆，则为true
     */
    public static boolean matchFirstInProvider(final ICapabilityProvider provider, @NotNull final Predicate<ItemStack> tester)
    {
        return matchInProvider(provider, inv -> slot -> tester, true);
    }

    /**
     * 实际循环遍历提供者的顶层函数。如果找到一个匹配的物品堆，将返回。
     *
     * @param provider       要遍历的提供者
     * @param tester         用于测试槽位的函数
     * @param stopAfterFirst 是否在找到一个匹配的物品堆后停止执行
     * @return 如果找到了物品堆，则为true
     */
    private static boolean matchInProvider(
      @Nullable final ICapabilityProvider provider,
      @NotNull final Function<ICapabilityProvider, Function<Integer, Predicate<ItemStack>>> tester,
      final boolean stopAfterFirst)
    {
        if (provider == null)
        {
            return false;
        }

        boolean foundOne = false;
        for (final IItemHandler handler : InventoryUtils.getItemHandlersFromProvider(provider))
        {
            final int size = handler.getSlots();
            for (int slot = 0; slot < size; slot++)
            {
                final ItemStack stack = handler.getStackInSlot(slot);
                // 解链函数并应用它
                if (tester.apply(provider).apply(slot).test(stack))
                {
                    foundOne = true;
                    if (stopAfterFirst)
                    {
                        return true;
                    }
                }
            }
        }

        return foundOne;
    }

    /**
     * 顶层的matchFirst函数，将在找到第一个物品堆后停止。
     *
     * @param provider 要搜索的提供者
     * @param tester   用于测试槽位的函数
     * @return 如果找到了物品堆，则为true
     */
    /*
    private static boolean matchFirstInProvider(
                                                 final ICapabilityProvider provider,
                                                 @NotNull final Function<ICapabilityProvider, Function<Integer, Predicate<ItemStack>>> tester)
    {
        return matchInProvider(provider, tester, true);
    }
    */

    /**
     * 在库存中查找与给定断言匹配的物品堆。
     *
     * @param provider 要搜索的提供者
     * @param tester   用于测试槽位的函数
     * @param action   如果槽位匹配，要使用的函数
     * @return 如果找到了物品堆，则为true
     */
    public static boolean matchFirstInProviderWithAction(
      final ICapabilityProvider provider,
      @NotNull final Predicate<ItemStack> tester,
      @NotNull final IMatchActionResult action)
    {
        return matchInProvider(
          provider,
          inv -> slot -> stack ->
          {
              if (tester.test(stack))
              {
                  action.accept(provider, slot);
                  return true;
              }
              return false;
          },
          true);
    }

    /**
     * 在处理程序中查找与给定断言匹配的物品堆。
     *
     * @param itemHandler 处理程序要搜索的
     * @param tester      用于测试槽位的函数
     * @param action      如果槽位匹配，要使用的函数
     * @return 如果找到了物品堆，则为true
     */
    public static boolean matchFirstInHandlerWithAction(
      @NotNull final IItemHandler itemHandler,
      @NotNull final Predicate<ItemStack> tester,
      @NotNull final IMatchActionResultHandler action)
    {
        return matchInHandler(
          itemHandler,
          inv -> slot -> stack ->
          {
              if (tester.test(stack))
              {
                  action.accept(itemHandler, slot);
                  return true;
              }
              return false;
          });
    }

    /**
     * 如果在处理程序中找到了某物品堆，将返回。
     *
     * @param handler 要检查的处理程序
     * @param tester  用于测试槽位的函数
     * @return 如果找到了物品堆，则为true
     */
    private static boolean matchInHandler(
      @Nullable final IItemHandler handler,
      @NotNull final Function<IItemHandler, Function<Integer, Predicate<ItemStack>>> tester)
    {
        if (handler == null)
        {
            return false;
        }

        final int size = handler.getSlots();
        for (int slot = 0; slot < size; slot++)
        {
            final ItemStack stack = handler.getStackInSlot(slot);
            // 解链函数并应用它
            if (tester.apply(handler).apply(slot).test(stack))
            {
                return true;
            }
        }

        return false;
    }

    /**
     * 在库存中查找与给定断言匹配的物品堆。
     *
     * @param provider 要搜索的提供者
     * @param tester   用于测试槽位的函数
     * @param action   如果槽位匹配，要使用的函数
     * @return 如果找到了物品堆，则为true
     */
    public static boolean matchFirstInProviderWithSimpleAction(
      final ICapabilityProvider provider,
      @NotNull final Predicate<ItemStack> tester,
      @NotNull final Consumer<Integer> action)
    {
        return matchInProvider(
          provider,
          inv -> slot -> stack ->
          {
              if (tester.test(stack))
              {
                  action.accept(slot);
                  return true;
              }
              return false;
          },
          true);
    }

    /**
     * 在库存中查找与给定断言匹配的物品堆。 (IInventory, Integer) -&gt; Boolean
     *
     * @param inventory 要搜索的库存
     * @param tester    用于测试槽位的函数
     * @return 如果找到了物品堆，则为true
     */
    public static boolean matchFirstInProvider(final ICapabilityProvider inventory, @NotNull final BiPredicate<Integer, ItemStack> tester)
    {
        return matchInProvider(inventory, inv -> slot -> stack -> tester.test(slot, stack), true);
    }

    /**
     * 描述在找到匹配的物品堆后执行的操作的功能接口。
     */
    @FunctionalInterface
    public interface IMatchActionResult extends ObjIntConsumer<ICapabilityProvider>
    {
        /**
         * 当找到与搜索断言匹配的物品堆时执行的方法。
         *
         * @param provider 与搜索断言匹配的物品堆。
         * @param slotIndex 找到此物品堆的槽位索引。
         */
        @Override
        void accept(ICapabilityProvider provider, int slotIndex);
    }

    /**
     * 描述在找到匹配的物品堆后执行的操作的功能接口。
     */
    @FunctionalInterface
    public interface IMatchActionResultHandler extends ObjIntConsumer<IItemHandler>
    {
        /**
         * 当找到与搜索断言匹配的物品堆时执行的方法。
         *
         * @param handler 与搜索断言匹配的物品堆。
         * @param slotIndex 找到此物品堆的槽位索引。
         */
        @Override
        void accept(IItemHandler handler, int slotIndex);
    }
}
