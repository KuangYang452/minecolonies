package com.minecolonies.coremod.entity.ai.basic;

import com.minecolonies.coremod.colony.buildings.AbstractBuilding;
import com.minecolonies.coremod.colony.jobs.AbstractJob;
import org.jetbrains.annotations.NotNull;

/**
 * 技能的AI类。
 *
 * @param <J> 此AI需要履行的职责。
 */
public abstract class AbstractEntityAISkill<J extends AbstractJob<?, J>, B extends AbstractBuilding> extends AbstractEntityAIBasic<J, B>
{

    /**
     * 为每个AI设置一些重要的骨架内容。
     *
     * @param job 工作类。
     */
    protected AbstractEntityAISkill(@NotNull final J job)
    {
        super(job);
    }
}
