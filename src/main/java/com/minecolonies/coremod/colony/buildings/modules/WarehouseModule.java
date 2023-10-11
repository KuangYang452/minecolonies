package com.minecolonies.coremod.colony.buildings.modules;

import com.minecolonies.api.colony.buildings.modules.AbstractBuildingModule;
import com.minecolonies.api.colony.buildings.modules.IPersistentModule;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import org.jetbrains.annotations.NotNull;

/**
 * 仓库模块的处理器。
 */
public class WarehouseModule extends AbstractBuildingModule implements IPersistentModule
{
    /**
     * 用于存储容量的标签。
     */
    private static final String TAG_STORAGE = "tagStorage";

    /**
     * 存储升级级别。
     */
    private int storageUpgrade = 0;

    /**
     * 构造一个新的分组物品列表模块，使用唯一的列表标识符。
     */
    public WarehouseModule()
    {
        super();
    }

    @Override
    public void deserializeNBT(final CompoundTag compound)
    {
        storageUpgrade = compound.getInt(TAG_STORAGE);
    }

    @Override
    public void serializeNBT(final CompoundTag compound)
    {
        compound.putInt(TAG_STORAGE, storageUpgrade);
    }

    @Override
    public void serializeToView(@NotNull final FriendlyByteBuf buf)
    {
        buf.writeInt(storageUpgrade);
    }

    /**
     * 获取升级级别。
     * @return 级别。
     */
    public int getStorageUpgrade()
    {
        return storageUpgrade;
    }

    /**
     * 增加存储升级级别。
     */
    public void incrementStorageUpgrade()
    {
        this.storageUpgrade++;
    }
}
