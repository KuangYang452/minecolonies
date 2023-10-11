package com.minecolonies.api.colony.requestsystem.requestable.deliveryman;

import com.google.common.reflect.TypeToken;
import com.minecolonies.api.colony.requestsystem.factory.IFactoryController;
import com.minecolonies.api.util.ReflectionUtils;
import com.minecolonies.api.util.constant.TypeConstants;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import org.jetbrains.annotations.NotNull;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * 用于表示请求系统内的提货的类。这个类可以用来请求提货。
 */
public class Pickup extends AbstractDeliverymanRequestable
{
    /**
     * 属于这个类的类型令牌集合。
     */
    private final static Set<TypeToken<?>>
      TYPE_TOKENS = ReflectionUtils.getSuperClasses(TypeToken.of(Pickup.class)).stream().filter(type -> !type.equals(TypeConstants.OBJECT)).collect(Collectors.toSet());

    /**
     * 交付请求的构造函数
     *
     * @param priority 请求的优先级。
     */
    public Pickup(final int priority)
    {
        super(priority);
    }

    @NotNull
    public static CompoundTag serialize(@NotNull final IFactoryController controller, final Pickup pickup)
    {
        final CompoundTag compound = new CompoundTag();
        compound.put(NBT_PRIORITY, controller.serialize(pickup.getPriority()));
        return compound;
    }

    @NotNull
    public static Pickup deserialize(@NotNull final IFactoryController controller, @NotNull final CompoundTag compound)
    {
        final int priority = controller.deserialize(compound.getCompound(NBT_PRIORITY));
        return new Pickup(priority);
    }

    /**
     * 序列化可交付物。
     *
     * @param controller 控制器。
     * @param buffer     要写入的缓冲区。
     * @param input      要序列化的输入。
     */
    public static void serialize(final IFactoryController controller, final FriendlyByteBuf buffer, final Pickup input)
    {
        buffer.writeInt(input.getPriority());
    }

    /**
     * 反序列化可交付物。
     *
     * @param controller 控制器。
     * @param buffer     要读取的缓冲区。
     * @return 可交付物。
     */
    public static Pickup deserialize(final IFactoryController controller, final FriendlyByteBuf buffer)
    {
        final int priority = buffer.readInt();

        return new Pickup(priority);
    }

    @Override
    public boolean equals(final Object o)
    {
        // 注意，超类将比较优先级。
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
