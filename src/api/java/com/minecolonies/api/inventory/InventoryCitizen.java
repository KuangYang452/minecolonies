package com.minecolonies.api.inventory;

import com.minecolonies.api.colony.ICitizenData;
import com.minecolonies.api.util.ItemStackUtils;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.Nameable;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraftforge.items.IItemHandlerModifiable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.function.Consumer;

import static com.minecolonies.api.research.util.ResearchConstants.CITIZEN_INV_SLOTS;
import static com.minecolonies.api.util.constant.NbtTagConstants.TAG_SIZE;

/**
 * 公民的基本库存。
 */
public class InventoryCitizen implements IItemHandlerModifiable, Nameable
{
    /**
     * 如果未找到插槽，则返回的插槽。
     */
    private static final int NO_SLOT = -1;

    /**
     * 默认库存大小。
     */
    private static final int DEFAULT_INV_SIZE = 27;
    private static final int ROW_SIZE         = 9;

    /**
     * 空插槽的数量
     */
    private int freeSlots = DEFAULT_INV_SIZE;

    /**
     * 库存。（27个主库存，4个盔甲插槽，1个副手插槽）
     */
    private NonNullList<ItemStack> mainInventory = NonNullList.withSize(DEFAULT_INV_SIZE, ItemStackUtils.EMPTY);

    /**
     * 当前持有物品的索引（0-8）。
     */
    private int mainItem    = NO_SLOT;
    private int offhandItem = NO_SLOT;

    /**
     * 库存的自定义名称。在我们的情况下是公民的名称。
     */
    private String customName;

    /**
     * 拥有库存的公民。
     */
    private ICitizenData citizen;

    /**
     * 创建公民的库存。
     *
     * @param title         库存的标题。
     * @param localeEnabled 布尔值，库存是否具有自定义名称。
     * @param citizen       拥有库存的公民。
     */
    public InventoryCitizen(final String title, final boolean localeEnabled, final ICitizenData citizen)
    {
        this.citizen = citizen;
        if (localeEnabled)
        {
            customName = title;
        }
    }

    /**
     * 创建公民的库存。
     *
     * @param title         库存的标题。
     * @param localeEnabled 布尔值，库存是否具有自定义名称。
     */
    public InventoryCitizen(final String title, final boolean localeEnabled)
    {
        if (localeEnabled)
        {
            customName = title;
        }
    }

    /**
     * 设置库存的名称。
     *
     * @param customName 用于设置名称的字符串。
     */
    public void setCustomName(final String customName)
    {
        this.customName = customName;
    }

    /**
     * 返回当前由公民持有的物品。
     *
     * @param hand 手中持有的手。
     * @return 公民当前持有的{@link ItemStack}。
     */
    public ItemStack getHeldItem(final InteractionHand hand)
    {
        if (hand.equals(InteractionHand.MAIN_HAND))
        {
            return getStackInSlot(mainItem);
        }

        return getStackInSlot(offhandItem);
    }

    /**
     * 设置由公民持有的物品。
     *
     * @param hand 手中持有的手。
     * @param slot 要由公民持有的物品的插槽索引。
     */
    public void setHeldItem(final InteractionHand hand, final int slot)
    {
        if (hand.equals(InteractionHand.MAIN_HAND))
        {
            this.mainItem = slot;
        }

        this.offhandItem = slot;
    }

    /**
     * 获取由公民持有的物品的插槽。
     *
     * @param hand 手中持有的手。
     * @return 持有物品的插槽索引
     */
    public int getHeldItemSlot(final InteractionHand hand)
    {
        if (hand.equals(InteractionHand.MAIN_HAND))
        {
            return mainItem;
        }

        return offhandItem;
    }

    @Override
    public int getSlots()
    {
        return this.mainInventory.size();
    }

    /**
     * 检查库存是否有空间。
     *
     * @return 如果主库存（不包括盔甲插槽）有空插槽，则返回true。
     */
    public boolean hasSpace()
    {
        return freeSlots > 0;
    }

    /**
     * 检查库存是否完全为空。
     *
     * @return 如果主库存（不包括盔甲插槽）完全为空，则返回true。
     */
    public boolean isEmpty()
    {
        return freeSlots == mainInventory.size();
    }

    /**
     * 检查库存是否完全满。
     *
     * @return 如果主库存（不包括盔甲插槽）完全满，则返回true。
     */
    public boolean isFull()
    {
        return freeSlots == 0;
    }

    /**
     * 调整此库存的大小。
     *
     * @param size       当前大小。
     * @param futureSize 未来大小。
     */
    private void resizeInventory(final int size, final int futureSize)
    {
        if (size < futureSize)
        {
            final NonNullList<ItemStack> inv = NonNullList.withSize(futureSize, ItemStackUtils.EMPTY);

            for (int i = 0; i < mainInventory.size(); i++)
            {
                inv.set(i, mainInventory.get(i));
            }

            mainInventory = inv;
            freeSlots += futureSize - size;
        }
    }

    /**
     * 获取此对象的名称。对于公民，这将返回其名称。
     *
     * @return 库存的名称。
     */
    @NotNull
    @Override
    public Component getName()
    {
        return Component.translatable(this.hasCustomName() ? this.customName : "citizen.inventory");
    }

    /**
     * 检查库存是否具有自定义名称。
     *
     * @return 如果库存具有自定义名称，则返回true。
     */
    @Override
    public boolean hasCustomName()
    {
        return this.customName != null;
    }

    /**
     * 返回给定插槽中的堆栈。
     *
     * @param index 索引。
     * @return 堆栈。
     */
    @NotNull
    @Override
    public ItemStack getStackInSlot(final int index)
    {
        if (index == NO_SLOT)
        {
            return ItemStack.EMPTY;
        }
        if (index >= mainInventory.size())
        {
            return ItemStack.EMPTY;
        }
        else
        {
            return mainInventory.get(index);
        }
    }

    /**
     * 在库存内损坏物品
     *
     * @param slot     要损坏的插槽
     * @param amount   损坏数量
     * @param entityIn 使用物品的实体
     * @param onBroken 物品损坏后的操作
     * @return 如果物品损坏，则返回true
     */
    public <T extends LivingEntity> boolean damageInventoryItem(final int slot, int amount, @Nullable T entityIn, @Nullable Consumer<T> onBroken)
    {
        final ItemStack stack = mainInventory.get(slot);
        if (!ItemStackUtils.isEmpty(stack))
        {
            // The 4 parameter inner call from forge is for adding a callback to alter the damage caused,
            // but unlike its description does not actually damage the item(despite the same function name). So used to just calculate the damage.
            stack.hurtAndBreak(stack.getItem().damageItem(stack, amount, entityIn, onBroken), entityIn, onBroken);

            if (ItemStackUtils.isEmpty(stack))
            {
                freeSlots++;
            }
        }

        return ItemStackUtils.isEmpty(stack);
    }

    /**
     * Shrinks an item in the given slot
     *
     * @param slot slot to shrink
     * @return true if item is empty afterwards
     */
    public boolean shrinkInventoryItem(final int slot)
    {
        final ItemStack stack = mainInventory.get(slot);
        if (!ItemStackUtils.isEmpty(stack))
        {
            stack.setCount(stack.getCount() - 1);

            if (ItemStackUtils.isEmpty(stack))
            {
                freeSlots++;
            }
        }

        return ItemStackUtils.isEmpty(stack);
    }

    @Nonnull
    @Override
    public ItemStack insertItem(final int slot, @Nonnull final ItemStack stack, final boolean simulate)
    {
        if (stack.isEmpty())
        {
            return stack;
        }

        final ItemStack copy = stack.copy();
        final ItemStack inSlot = mainInventory.get(slot);
        if (inSlot.getCount() >= inSlot.getMaxStackSize() || (!inSlot.isEmpty() && !ItemStackUtils.compareItemStacksIgnoreStackSize(inSlot, copy)))
        {
            return copy;
        }

        if (inSlot.isEmpty())
        {
            if (!simulate)
            {
                markDirty();
                freeSlots--;
                mainInventory.set(slot, copy);
                return ItemStack.EMPTY;
            }
            else
            {
                return ItemStack.EMPTY;
            }
        }

        final int avail = inSlot.getMaxStackSize() - inSlot.getCount();
        if (avail >= copy.getCount())
        {
            if (!simulate)
            {
                markDirty();
                inSlot.setCount(inSlot.getCount() + copy.getCount());
            }
            return ItemStack.EMPTY;
        }
        else
        {
            if (!simulate)
            {
                markDirty();
                inSlot.setCount(inSlot.getCount() + avail);
            }
            copy.setCount(copy.getCount() - avail);
            return copy;
        }
    }

    @Nonnull
    @Override
    public ItemStack extractItem(final int slot, final int amount, final boolean simulate)
    {
        final ItemStack inSlot = mainInventory.get(slot);
        if (inSlot.isEmpty())
        {
            return ItemStack.EMPTY;
        }
        if (amount >= inSlot.getCount())
        {
            if (!simulate)
            {
                markDirty();
                freeSlots++;
                mainInventory.set(slot, ItemStack.EMPTY);
            }
            return inSlot;
        }
        else
        {

            final ItemStack copy = inSlot.copy();
            copy.setCount(amount);
            if (!simulate)
            {
                markDirty();
                inSlot.setCount(inSlot.getCount() - amount);
                if (ItemStackUtils.isEmpty(inSlot))
                {
                    freeSlots++;
                }
            }
            return copy;
        }
    }

    @Override
    public int getSlotLimit(final int slot)
    {
        return 64;
    }

    @Override
    public boolean isItemValid(final int slot, @Nonnull final ItemStack stack)
    {
        return true;
    }

    /**
     * For tile entities, ensures the chunk containing the tile entity is saved to disk later - the game won't think it hasn't changed and skip it.
     */
    public void markDirty()
    {
        if (this.citizen != null)
        {
            this.citizen.markDirty();
        }
    }

    /**
     * Get the formatted TextComponent that will be used for the sender's username in chat.
     */
    @NotNull
    @Override
    public Component getDisplayName()
    {
        return this.hasCustomName() ? Component.literal(customName) : Component.literal(citizen.getName());
    }

    /**
     * Writes the inventory out as a list of compound tags. This is where the slot indices are used (+100 for armor, +80 for crafting).
     *
     * @param nbtTagList the taglist in.
     * @return the filled list.
     */
    public ListTag write(final ListTag nbtTagList)
    {
        if (citizen != null && citizen.getColony() != null)
        {
            final double researchEffect = citizen.getColony().getResearchManager().getResearchEffects().getEffectStrength(CITIZEN_INV_SLOTS);
            if (researchEffect > 0 && this.mainInventory.size() < DEFAULT_INV_SIZE + researchEffect)
            {
                resizeInventory(this.mainInventory.size(), (int) (DEFAULT_INV_SIZE + researchEffect));
            }
        }

        final CompoundTag sizeNbt = new CompoundTag();
        sizeNbt.putInt(TAG_SIZE, this.mainInventory.size());
        nbtTagList.add(sizeNbt);

        freeSlots = mainInventory.size();
        for (int i = 0; i < this.mainInventory.size(); ++i)
        {
            if (!(this.mainInventory.get(i)).isEmpty())
            {
                final CompoundTag compoundNBT = new CompoundTag();
                compoundNBT.putByte("Slot", (byte) i);
                (this.mainInventory.get(i)).save(compoundNBT);
                nbtTagList.add(compoundNBT);
                freeSlots--;
            }
        }

        return nbtTagList;
    }

    /**
     * Reads from the given tag list and fills the slots in the inventory with the correct items.
     *
     * @param nbtTagList the tag list.
     */
    public void read(final ListTag nbtTagList)
    {
        if (this.mainInventory.size() < nbtTagList.getCompound(0).getInt(TAG_SIZE))
        {
            int size = nbtTagList.getCompound(0).getInt(TAG_SIZE);
            size -= size % ROW_SIZE;
            this.mainInventory = NonNullList.withSize(size, ItemStackUtils.EMPTY);
        }

        freeSlots = mainInventory.size();

        for (int i = 1; i < nbtTagList.size(); ++i)
        {
            final CompoundTag compoundNBT = nbtTagList.getCompound(i);

            final int j = compoundNBT.getByte("Slot") & 255;
            final ItemStack itemstack = ItemStack.of(compoundNBT);

            if (!itemstack.isEmpty())
            {
                if (j < this.mainInventory.size())
                {
                    this.mainInventory.set(j, itemstack);
                    freeSlots--;
                }
            }
        }
    }

    @Override
    public void setStackInSlot(final int slot, @Nonnull final ItemStack stack)
    {
        if (!ItemStackUtils.isEmpty(stack))
        {
            if (ItemStackUtils.isEmpty(mainInventory.get(slot)))
            {
                freeSlots--;
            }
        }
        else if (!ItemStackUtils.isEmpty(mainInventory.get(slot)))
        {
            freeSlots++;
        }

        mainInventory.set(slot, stack);
    }
}
