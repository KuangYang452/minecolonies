package com.minecolonies.core.colony.expeditions;

import com.minecolonies.api.colony.expeditions.ExpeditionStatus;
import com.minecolonies.api.colony.expeditions.IExpedition;
import com.minecolonies.api.colony.expeditions.IExpeditionMember;
import com.minecolonies.api.colony.expeditions.IExpeditionStage;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

import static com.minecolonies.api.util.constant.ExpeditionConstants.EXPEDITION_STAGE_WILDERNESS;

/**
 * Class for an expedition instance.
 */
public class Expedition implements IExpedition
{
    /**
     * Nbt tag constants.
     */
    private static final String TAG_EQUIPMENT   = "equipment";
    private static final String TAG_MEMBERS     = "members";
    private static final String TAG_MEMBER_TYPE = "memberType";
    private static final String TAG_STATUS      = "status";

    /**
     * The equipment given to the expedition prior to starting.
     */
    @NotNull
    protected final List<ItemStack> equipment;

    /**
     * The members of the expedition.
     */
    @NotNull
    protected final Map<Integer, IExpeditionMember<?>> members;

    /**
     * The results of this expedition.
     */
    protected final Deque<IExpeditionStage> results = new ArrayDeque<>(List.of(new ExpeditionStage(Component.translatable(EXPEDITION_STAGE_WILDERNESS))));

    /**
     * The stage of the expedition.
     */
    protected ExpeditionStatus status;

    /**
     * Deserialization constructor.
     *
     * @param equipment the list of equipment for this expedition.
     * @param members   the members for this expedition.
     * @param status    the status of the expedition.
     */
    public Expedition(final @NotNull List<ItemStack> equipment, final @NotNull List<IExpeditionMember<?>> members, final ExpeditionStatus status)
    {
        this.equipment = equipment;
        this.members = members.stream().collect(Collectors.toMap(IExpeditionMember::getId, v -> v));
        this.status = status;
    }

    /**
     * Create an expedition instance from compound data.
     *
     * @param compound the compound data.
     * @return the expedition instance.
     */
    @NotNull
    public static Expedition loadFromNBT(final CompoundTag compound)
    {
        final List<IExpeditionMember<?>> members = new ArrayList<>();
        final ListTag membersList = compound.getList(TAG_MEMBERS, Tag.TAG_COMPOUND);
        for (int i = 0; i < membersList.size(); ++i)
        {
            final CompoundTag memberCompound = membersList.getCompound(i);
            final String memberType = memberCompound.getString(TAG_MEMBER_TYPE);
            if (Objects.equals(memberType, "citizen"))
            {
                members.add(new ExpeditionCitizenMember(memberCompound));
            }
            else if (Objects.equals(memberType, "visitor"))
            {
                members.add(new ExpeditionVisitorMember(memberCompound));
            }
        }

        final List<ItemStack> equipment = new ArrayList<>();
        final ListTag equipmentList = compound.getList(TAG_EQUIPMENT, Tag.TAG_COMPOUND);
        for (int i = 0; i < equipmentList.size(); ++i)
        {
            equipment.add(ItemStack.of(equipmentList.getCompound(i)));
        }
        final ExpeditionStatus status = compound.contains(TAG_STATUS) ? ExpeditionStatus.valueOf(compound.getString(TAG_STATUS)) : ExpeditionStatus.CREATED;

        return new Expedition(equipment, members, status);
    }

    @Override
    public ExpeditionStatus getStatus()
    {
        return this.status;
    }

    @Override
    public void setStatus(final ExpeditionStatus stage)
    {
        this.status = stage;
    }

    @Override
    @NotNull
    public List<IExpeditionMember<?>> getMembers()
    {
        return this.members.values().stream().toList();
    }

    @Override
    @NotNull
    public List<ItemStack> getEquipment()
    {
        return equipment;
    }

    @Override
    @NotNull
    public List<IExpeditionMember<?>> getActiveMembers()
    {
        return this.members.values().stream().filter(f -> !f.isDead()).toList();
    }

    @Override
    @NotNull
    public List<IExpeditionStage> getResults()
    {
        return this.results.stream().toList();
    }

    @Override
    public void advanceStage(final Component header)
    {
        this.results.add(new ExpeditionStage(header));
    }

    @Override
    public void rewardFound(final ItemStack itemStack)
    {
        this.results.getLast().addReward(itemStack);
    }

    @Override
    public void mobKilled(final EntityType<?> type)
    {
        this.results.getLast().addKill(type);
    }

    @Override
    public void memberLost(final IExpeditionMember<?> member)
    {
        this.results.getLast().memberLost(member);
        member.died();
    }

    /**
     * Write this expedition builder to compound data.
     *
     * @param compound the compound tag.
     */
    @Override
    public void write(final CompoundTag compound)
    {
        final ListTag memberTag = new ListTag();
        for (final IExpeditionMember<?> member : members.values())
        {
            final CompoundTag memberCompound = new CompoundTag();
            if (member instanceof ExpeditionCitizenMember)
            {
                memberCompound.putString(TAG_MEMBER_TYPE, "citizen");
            }
            else if (member instanceof ExpeditionVisitorMember)
            {
                memberCompound.putString(TAG_MEMBER_TYPE, "visitor");
            }
            member.write(memberCompound);
            memberTag.add(memberCompound);
        }
        compound.put(TAG_MEMBERS, memberTag);

        final ListTag equipmentTag = new ListTag();
        for (final ItemStack itemStack : equipment)
        {
            equipmentTag.add(itemStack.serializeNBT());
        }
        compound.put(TAG_EQUIPMENT, equipmentTag);
        compound.putString(TAG_STATUS, status.name());
    }
}