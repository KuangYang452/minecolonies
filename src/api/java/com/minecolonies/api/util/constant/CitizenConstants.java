package com.minecolonies.api.util.constant;

/**
 * 关于市民的常量。
 */
public final class CitizenConstants
{
    /**
     * 每个市民的基础移动速度。
     */
    public static final double BASE_MOVEMENT_SPEED = 0.3D;

    /**
     * 中等饱和度点。小于此值 = 差，大于此值 = 好。
     */
    public static final int AVERAGE_SATURATION = 10;

    /**
     * 低于此值为低饱和度。
     */
    public static final int LOW_SATURATION = 6;

    /**
     * 满饱和度量。
     */
    public static final double FULL_SATURATION = 20;

    /**
     * 治疗市民所需的刻数。
     */
    public static final int HEAL_CITIZENS_AFTER = 100;

    /**
     * 用于查看事物的偏航值差异。
     */
    public static final float  FACING_DELTA_YAW           = 10F;
    /**
     * 我们可以听到方块破碎声音的范围。
     */
    public static final double BLOCK_BREAK_SOUND_RANGE    = 16.0D;
    /**
     * 某人可以看到方块破碎粒子的范围。
     */
    public static final double BLOCK_BREAK_PARTICLE_RANGE = 16.0D;
    /**
     * 在不实际移动的情况下旋转所需的最小移动量。
     */
    public static final double MOVE_MINIMAL               = 0.001D;
    /**
     * 市民的基础最大生命值。
     */
    public static final double BASE_MAX_HEALTH            = 20D;
    /**
     * 市民的基础寻路范围。
     */
    public static final int    BASE_PATHFINDING_RANGE     = 100;
    /**
     * 市民的高度。
     */
    public static final double CITIZEN_HEIGHT             = 1.8D;
    /**
     * 市民的宽度。
     */
    public static final double CITIZEN_WIDTH              = 0.6D;
    /**
     * 市民旋转的速度。
     */
    public static final double ROTATION_MOVEMENT          = 30;
    /**
     * 20刻或也可表示：每秒一次。
     */
    public static final int    TICKS_20                   = 20;

    /**
     * 市民个人偏移的倍数。
     */
    public static final int    OFFSET_TICK_MULTIPLIER     = 7;
    /**
     * 市民要回家所需的范围。
     */
    public static final double RANGE_TO_BE_HOME           = 16;
    /**
     * 最新日志允许的最大行数。
     */
    public static final int    MAX_LINES_OF_LATEST_LOG    = 4;
    /**
     * 实体应该远离怪物的距离。
     */
    public static final double DISTANCE_OF_ENTITY_AVOID   = 5.0D;
    /**
     * 逃离实体时的初始速度。
     */
    public static final double INITIAL_RUN_SPEED_AVOID    = 1.1D;
    /**
     * 逃离实体时的后续速度。
     */
    public static final double LATER_RUN_SPEED_AVOID      = 0.8D;
    /**
     * 市民可以召唤卫兵帮助的最大平方距离，300个方块。
     */
    public static final int    MAX_GUARD_CALL_RANGE       = 90000;
    /**
     * 在极端饱和度情况下的大倍增因子。
     */
    public static final double BIG_SATURATION_FACTOR      = 0.05;
    /**
     * 每个新夜晚减少的饱和度因子 * 建筑等级。
     */
    public static final double SATURATION_DECREASE_FACTOR = 0.02;

    /**
     * 保持与当前建筑地点的最大范围。
     */
    public static final int EXCEPTION_TIMEOUT = 100;

    /**
     * 保持与当前建筑地点的最大附加范围。
     */
    public static final int MAX_ADDITIONAL_RANGE_TO_BUILD = 25;

    /**
     * 等待下一次检查物品的滴答数。
     */
    public static final int DELAY_RECHECK = 10;

    /**
     * 任何步行到方块的默认范围。
     */
    public static final int DEFAULT_RANGE_FOR_DELAY = 4;

    /**
     * 物品卸载前完成的动作数。
     */
    public static final int ACTIONS_UNTIL_DUMP = 32;

    /**
     * 挖掘时每x滴答数命中一个方块。
     */
    public static final int HIT_EVERY_X_TICKS = 5;

    /**
     * 建筑者永远不应填满的最小槽位数。
     */
    public static final long MIN_OPEN_SLOTS = 5;

    /**
     * 市民的最大等级。
     */
    public static final int MAX_CITIZEN_LEVEL = 99;

    /**
     * 卫兵建筑健康修饰符名称。
     */
    public static final String GUARD_HEALTH_MOD_BUILDING_NAME = "MinecoloniesGuardBuildingHP";

    /**
     * 研究健康修饰符名称。
     */
    public static final String RESEARCH_BONUS_MULTIPLIER = "ResearchSpeedBonus";

    /**
     * 附加技能奖励速度修饰符。
     */
    public static final String SKILL_BONUS_ADD = "SkillSpeedBonus";

    /**
     * 配置卫兵健康修饰符名称。
     */
    public static final String GUARD_HEALTH_MOD_CONFIG_NAME = "MinecoloniesGuardConfigHP";

    /**
     * 基于卫兵等级的健康奖励修饰符名称。
     */
    public static final String GUARD_HEALTH_MOD_LEVEL_NAME = "MinecoloniesGuardLevelHealth";

    /**
     * 当堆叠大小小于或等于此值时，卸载的机会为50%。
     */
    public static final int CHANCE_TO_DUMP_50 = 16;

    /**
     * 随机丢弃的机会，如果小于此值则丢弃，否则不丢弃。
     */
    public static final int CHANCE_TO_DUMP = 8;

    /**
     * 疾病标签。
     */
    public static final String TAG_DISEASE = "disease";

    /**
     * 免疫疾病标签。
     */
    public static final String TAG_IMMUNITY = "immunity";

    /**
     * 正午时间。
     */
    public static final int NOON = 6000;

    /**
     * 夜晚时间，可以睡觉的时间点。
     */
    public static final int NIGHT = 12600;

    /**
     * 与当前建筑地点保持的最小范围。
     */
    public static final int MIN_ADDITIONAL_RANGE_TO_BUILD = 3;

    /**
     * 表示某物是否是航路点的字符串。
     */
    public static final String WAYPOINT_STRING = "infrastructure";

    /**
     * 建筑师在每次建造时获得的经验值（还会额外根据属性修改器增加）。
     */
    public static final double XP_EACH_BUILDING = 8.0D;

    /**
     * 增加此值以减慢建筑速度。用于平衡工人级别速度的增加。
     */
    public static final int PROGRESS_MULTIPLIER = 10;

    /**
     * 建筑师在自己围住时逃跑的速度。
     */
    public static final double RUN_AWAY_SPEED = 4.1D;

    /**
     * 建筑师应该达到的标准工作范围。
     */
    public static final int STANDARD_WORKING_RANGE = 5;

    /**
     * 建筑师必须达到的最小工作范围，才能进行建造或清理。
     */
    public static final int MIN_WORKING_RANGE = 12;

    /**
     * 禁用的计时器。
     */
    public static final int DISABLED         = -1;
}
