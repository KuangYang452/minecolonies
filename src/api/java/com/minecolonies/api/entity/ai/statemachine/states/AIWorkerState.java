package com.minecolonies.api.entity.ai.statemachine.states;

/**
 * 基本状态，包含所有 AI 使用的状态。请通过扩展此类创建您的 AI 需要的状态，并请对每个状态进行文档记录其功能。
 */
public enum AIWorkerState implements IAIState
{

    /*
###GENERAL###
     */
    /**
     * 这是ai的空闲状态。从这里开始，它将开始工作。在你的ai中使用这个状态来启动你的代码。
     */
    IDLE(true),
    /**
     * 此状态仅用于ai初始化。它检查是否有任何重要的东西为空。
     */
    INIT(true),
    /**
     * 库存必须转储。
     */
    INVENTORY_FULL(false),
    /**
     * 检查所有必需的项目。
     */
    PREPARING(true),
    /**
     * 步行到大楼开始工作。
     */
    START_WORKING(true),
    /**
     * 人工智能需要一些它正在等待的项目。
     */
    NEEDS_ITEM(true),
    /**
     * 开始构建结构迭代器。
     */
    START_BUILDING(false),
    /**
     * 挖掘一个方块。
     */
    MINE_BLOCK(false),
    /**
     * 加载结构和要求。
     */
    LOAD_STRUCTURE(false),
    /**
     * 创建实体结构。
     */
    BUILDING_STEP(false),
    /**
     * 完成建筑。
     */
    COMPLETE_BUILD(false),
    /**
     * 建筑后捡拾剩余的物品。
     */
    PICK_UP_RESIDUALS(true),
    /**
     * 决定下一个AI状态。
     */
    DECIDE(true),
    /**
     * 不工作，可以用于业余活动。
     */
    PAUSED(true),
    /**
     * 步行到调试目标。
     */
    WALK_TO(true),
    /**
     * 整理库存。
     */
    ORGANIZE_INVENTORY(false),
    /**
     * 向仓库移交多余物品。
     */
    TRANSFER_TO_WAREHOUSE(true),
    /**
     * 从仓库提货。
     */
    PICK_UP_FROM_WAREHOUSE(true),
    /*
###FISHERMAN###
     */
    /**
     * 渔夫正在找水。
     */
    FISHERMAN_SEARCHING_WATER(true),
    /**
     * 渔夫找到了水，可以开始捕鱼了。
     */
    FISHERMAN_WALKING_TO_WATER(true),
    FISHERMAN_CHECK_WATER(true),
    FISHERMAN_START_FISHING(false),

    /*
###Lumberjack###
     */
    /**
     * 伐木工人正在开始他/她的日常工作。
     */
    LUMBERJACK_START_WORKING(true),
    /**
     * 伐木工人正在找树。
     */
    LUMBERJACK_SEARCHING_TREE(true),
    /**
     * 伐木工人找到了树，可以开始砍伐了。
     */
    LUMBERJACK_CHOP_TREE(false),
    /**
     * 伐木工人正在采集树苗。
     */
    LUMBERJACK_GATHERING(true),
    /**
     * There are no trees in his search range.
     */
    LUMBERJACK_NO_TREES_FOUND(true),

    /*
###Miner###
     */
    /**
     * 检查是否有矿井。
     */
    MINER_CHECK_MINESHAFT(true),
    /**
     * 矿工走向梯子。
     */
    MINER_WALKING_TO_LADDER(true),
    /**
     * 矿工修理梯子。
     */
    MINER_REPAIRING_LADDER(true),
    /**
     * 矿工挖掘他的竖井。
     */
    MINER_MINING_SHAFT(true),
    /**
     * 矿工建造了他的竖井。
     */
    MINER_BUILDING_SHAFT(true),
    /**
     * 矿工挖掘一个节点。
     */
    MINER_MINING_NODE(true),

    /*
###Builder###
     */

    /**
     * 捡起他可能需要的所有材料。
     */
    PICK_UP(false),

    /*
###FARMER###
    */

    /**
     * 锄地。
     */
    FARMER_HOE(false),

    /**
     * 播种。
     */
    FARMER_PLANT(false),

    /**
     * 收割庄稼。
     */
    FARMER_HARVEST(false),

      /*
###Undertaker###
    */

    /**
     * Empty The grave
     */
    EMPTY_GRAVE(false),

    /**
     * Dig The grave
     */
    DIG_GRAVE(false),

    /**
     * Bury the citizen
     */
    BURY_CITIZEN(false),

    /**
     * Attempt Resurrect
     */
    TRY_RESURRECT(false),

      /*
###Guard###
    */

    /**
     * Decision state for guards.
     */
    GUARD_DECIDE(false),

    /**
     * Physically attack the target.
     */
    GUARD_ATTACK_PHYSICAL(false),

    /**
     * Use a ranged attack against the target.
     */
    GUARD_ATTACK_RANGED(false),

    /**
     * Allow the guard to protect himself.
     */
    GUARD_ATTACK_PROTECT(false),

    /**
     * Patrol through the village.
     */
    GUARD_PATROL(true),

    /**
     * Sleeping on duty
     */
    GUARD_SLEEP(false),

    /**
     * Wake up another guard
     */
    GUARD_WAKE(false),

    /**
     * Follow a player.
     */
    GUARD_FOLLOW(true),

    /**
     * Guard a position.
     */
    GUARD_GUARD(true),

    /**
     * Regen at the building.
     */
    GUARD_REGEN(true),

    /**
     * Helping out a citizen in danger
     */
    HELP_CITIZEN(false),

    /*
###Deliveryman###
    */

    /**
     * 从仓库拿东西。
     */
    PREPARE_DELIVERY(true),

    /**
     * 交付所需的物品或工具。
     */
    DELIVERY(true),

    /**
     * 从建筑物中拾取不需要的物品。
     */
    PICKUP(true),

    /**
     * 将存货倾倒在仓库的箱子上。
     */
    DUMPING(false),

     /*
###Baker###
    */

    /**
     * Knead the dough.
     */
    BAKER_KNEADING(false),

    /**
     * Bake the dough.
     */
    BAKER_BAKING(false),

    /**
     * Finish up the product.
     */
    BAKER_FINISHING(false),

    /**
     * Take the product out of the oven.
     */
    BAKER_TAKE_OUT_OF_OVEN(false),

    /*
###Furnace users###
     */

    /**
     * smelter smelts ore until its a bar.
     */
    START_USING_FURNACE(true),

    /**
     * Gathering ore from his building.
     */
    GATHERING_REQUIRED_MATERIALS(true),

    /**
     * Retrieve the ore from the furnace.
     */
    RETRIEVING_END_PRODUCT_FROM_FURNACE(true),

    /**
     * Retrieve used fuel from the furnace.
     */
    RETRIEVING_USED_FUEL_FROM_FURNACE(true),

    /**
     * Fuel the furnace
     */
    ADD_FUEL_TO_FURNACE(true),

    /*
###Cook###
     */

    /**
     * Serve food to the citizen inside the building.
     */
    COOK_SERVE_FOOD_TO_CITIZEN(true),

    /*
### Herders ###
     */

    /**
     * Breed two animals together.
     */
    HERDER_BREED(false),

    /**
     * Butcher an animal.
     */
    HERDER_BUTCHER(false),

    /**
     * Pickup items within area.
     */
    HERDER_PICKUP(true),

    /**
     * Pickup items within area.
     */
    HERDER_FEED(false),

    /*
### Cowboy ###
     */

    /**
     * Milk cows!
     */
    COWBOY_MILK(false),

    /*
### Shepherd ###
     */

    /**
     * Shear a sheep!
     */
    SHEPHERD_SHEAR(false),

    /*
### Composter ###
     */

    /**
     * Fill up the barrels
     */
    COMPOSTER_FILL(true),

    /**
     * Take the compost from the barrels
     */
    COMPOSTER_HARVEST(true),

    /**
     * Gather materials from the building
     */
    GET_MATERIALS(true),

    /*
### Student ###
     */

    STUDY(true),
    /*
### General Training AI ###

    /**
     * Wander around the building
     */
    TRAINING_WANDER(true),

    /**
     * Go to the shooting position.
     */
    GO_TO_TARGET(true),

    /**
     * Find the position to train from.
     */
    COMBAT_TRAINING(true),

    /*
### Archers in Training ###
     */

    /**
     * Find a good position to shoot from.
     */
    ARCHER_FIND_SHOOTING_STAND_POSITION(false),

    /**
     * Select a random target.
     */
    ARCHER_SELECT_TARGET(true),

    /**
     * Check the shot result.
     */
    ARCHER_CHECK_SHOT(true),

    /**
     * Archer shoot target.
     */
    ARCHER_SHOOT(true),

            /*
### Knights in Training ###
     */

    /**
     * Guard attack a dummy.
     */
    KNIGHT_ATTACK_DUMMY(true),

    /**
     * Find dummy to attack
     */
    FIND_DUMMY_PARTNER(true),

    /**
     * Find a training partner
     */
    FIND_TRAINING_PARTNER(true),

    /**
     * Attack the training partner.
     */
    KNIGHT_TRAIN_WITH_PARTNER(true),

    /**
     * Attack protect in a certain direction.
     */
    KNIGHT_ATTACK_PROTECT(true),

        /*
### Crafter Workers ###
     */

    /**
     * Get the recipe.
     */
    GET_RECIPE(true),

    /**
     * Query the required items for a recipe.
     */
    QUERY_ITEMS(true),

    /**
     * Execute the crafting action.
     */
    CRAFT(true),

        /*
### Crusher ###
     */

    /**
     * Let the crusher crush blocks.
     */
    CRUSH(true),

            /*
### Sifter ###
     */

    /**
     * Let the sifter sieve blocks.
     */
    SIFT(true),

            /*
### Nether Worker ###
     */

    /**
     * Let the nether worker start out on the trip.
     */
    NETHER_LEAVE(true),

    /**
     * Let the nether worker return from the trip.
     */
    NETHER_AWAY(true),

    /**
     * Let the nether worker return from the trip.
     */
    NETHER_RETURN(true),

    /**
     * Let the nether worker open the portal to the nether
     */
    NETHER_OPENPORTAL(true),

    /**
     * Let the nether worker close the portal to the nether
     */
    NETHER_CLOSEPORTAL(true),


            /*
### Florist ###
     */

    /**
     * Let the florist harvest a flower.
     */
    FLORIST_HARVEST(true),

    /**
     * Let the florist compost the block.
     */
    FLORIST_COMPOST(true),

            /*
### Enchanter ###
     */

    /**
     * Let the enchanter gather experience.
     */
    ENCHANTER_DRAIN(true),

    /**
     * Enchant ancient tome.
     */
    ENCHANT(false),

    /*
### Avoid-AI ###
     */

    /**
     * Checking it the Avoid AI should start.
     */
    SAFE(true),

    NEED_RUN(true),

    RUNNING(true),

    /*
### Healer ###
   */
    REQUEST_CURE(true),

    CURE(true),

    WANDER(true),

    FREE_CURE(true),

    CURE_PLAYER(true),

    /*
### School related ###
     */
    TEACH(true),

    RECESS(true),

    /*
### Plantation related ###
     */
    PLANTATION_MOVE_TO_SOIL(true),

    PLANTATION_CHECK_SOIL(true),

    PLANTATION_CLEAR_OBSTACLE(true),

    PLANTATION_FARM(true),

    PLANTATION_PLANT(true),

    /*
### Beekeeper ###
     */
    BEEKEEPER_HARVEST(true),

        /*
###Alchemist users###
     */

    /**
     * brews potions until.
     */
    START_USING_BREWINGSTAND(true),

    /**
     * Retrieve the ore from the brewingStand.
     */
    RETRIEVING_END_PRODUCT_FROM_BREWINGSTAMD(true),

    /**
     * Retrieve used fuel from the brewingStand.
     */
    RETRIEVING_USED_FUEL_FROM_BREWINGSTAND(true),

    /**
     * Fuel the brewingStand.
     */
    ADD_FUEL_TO_BREWINGSTAND(true),

    /**
     * Harvest the mistletoes.
     */
    HARVEST_MISTLETOE(true),

    /**
     * Harvest the netherwart.
     */
    HARVEST_NETHERWART(true);

    /**
     * Is it okay to eat.
     */
    private boolean isOkayToEat;

    /**
     * Create a new one.
     *
     * @param okayToEat if okay.
     */
    AIWorkerState(final boolean okayToEat)
    {
        this.isOkayToEat = okayToEat;
    }

    /**
     * Method to check if it is okay.
     *
     * @return true if so.
     */
    public boolean isOkayToEat()
    {
        return isOkayToEat;
    }
}
