package com.minecolonies.api.colony.requestsystem.requestable.deliveryman;

import com.google.common.reflect.TypeToken;
import com.minecolonies.api.colony.requestsystem.factory.IFactoryController;
import com.minecolonies.api.util.ReflectionUtils;
import com.minecolonies.api.util.constant.TypeConstants;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Class used to represent pickups inside the request system. This class can be used to request a pickup of
 */
public class Pickup extends AbstractDeliverymanRequestable
{
    ////// --------------------------- NBTConstants --------------------------- \\\\\\
    protected static final String NBT_PICKUP_FILTER = "Filter";
    ////// --------------------------- NBTConstants --------------------------- \\\\\\

    /**
     * Set of type tokens belonging to this class.
     */
    private final static Set<TypeToken<?>>
      TYPE_TOKENS = ReflectionUtils.getSuperClasses(TypeToken.of(Pickup.class)).stream().filter(type -> !type.equals(TypeConstants.OBJECT)).collect(Collectors.toSet());

    /**
     * The itemstack to filter for this specific pickup.
     */
    @Nullable
    private List<ItemStack> pickupFilter;

    /**
     * Constructor for Delivery requests
     *
     * @param priority the priority of the request.
     * @param filter   a filter to apply to the pickup request.
     */
    public Pickup(final int priority, final @Nullable List<ItemStack> filter)
    {
        super(priority);
        if (filter != null)
        {
            this.pickupFilter = filter.stream().map(ItemStack::copy).collect(Collectors.toList());
        }
    }

    /**
     * Get the itemstack to filter for this specific pickup.
     *
     * @return the itemstack or null.
     */
    @Nullable
    public List<ItemStack> getPickupFilter()
    {
        return pickupFilter;
    }

    /**
     * Add or update a pickup filter.
     *
     * @param stacks the input stacks.
     */
    public void addToPickupFilter(final List<ItemStack> stacks)
    {
        if (pickupFilter == null)
        {
            pickupFilter = stacks;
        }
        else
        {
            pickupFilter.addAll(stacks);
        }
    }

    @NotNull
    public static CompoundTag serialize(@NotNull final IFactoryController controller, final Pickup pickup)
    {
        final CompoundTag compound = new CompoundTag();
        compound.put(NBT_PRIORITY, controller.serialize(pickup.getPriority()));
        if (pickup.getPickupFilter() != null)
        {
            final ListTag listTag = new ListTag();
            for (final ItemStack itemStack : pickup.getPickupFilter())
            {
                listTag.add(itemStack.save(new CompoundTag()));
            }
            compound.put(NBT_PICKUP_FILTER, listTag);
        }
        return compound;
    }

    @NotNull
    public static Pickup deserialize(@NotNull final IFactoryController controller, @NotNull final CompoundTag compound)
    {
        final int priority = controller.deserialize(compound.getCompound(NBT_PRIORITY));
        final List<ItemStack> pickupFilter;
        if (compound.contains(NBT_PICKUP_FILTER))
        {
            pickupFilter = new ArrayList<>();
            final ListTag list = compound.getList(NBT_PICKUP_FILTER, Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++)
            {
                pickupFilter.add(ItemStack.of(list.getCompound(i)));
            }
        }
        else
        {
            pickupFilter = null;
        }
        return new Pickup(priority, pickupFilter);
    }

    /**
     * Serialize the deliverable.
     *
     * @param controller the controller.
     * @param buffer     the buffer to write to.
     * @param input      the input to serialize.
     */
    public static void serialize(final IFactoryController controller, final FriendlyByteBuf buffer, final Pickup input)
    {
        buffer.writeInt(input.getPriority());
        buffer.writeBoolean(input.getPickupFilter() != null);
        if (input.getPickupFilter() != null)
        {
            buffer.writeInt(input.getPickupFilter().size());
            for (final ItemStack itemStack : input.getPickupFilter())
            {
                buffer.writeItem(itemStack);
            }
        }
    }

    /**
     * Deserialize the deliverable.
     *
     * @param controller the controller.
     * @param buffer     the buffer to read.
     * @return the deliverable.
     */
    public static Pickup deserialize(final IFactoryController controller, final FriendlyByteBuf buffer)
    {
        final int priority = buffer.readInt();
        final List<ItemStack> pickupFilter;
        if (buffer.readBoolean())
        {
            pickupFilter = new ArrayList<>();
            final int itemCount = buffer.readInt();
            for (int i = 0; i < itemCount; i++)
            {
                pickupFilter.add(buffer.readItem());
            }
        }
        else
        {
            pickupFilter = null;
        }
        return new Pickup(priority, pickupFilter);
    }

    @Override
    public boolean equals(final Object o)
    {
        // Note that the super class will compare the priority.
        if (!super.equals(o))
        {
            return false;
        }
        if (this == o)
        {
            return true;
        }
        return o instanceof Pickup;
    }

    @Override
    public String toString()
    {
        return "Pickup{" +
                 "priority=" + priority +
                 '}';
    }

    @Override
    public Set<TypeToken<?>> getSuperClasses()
    {
        return TYPE_TOKENS;
    }
}
