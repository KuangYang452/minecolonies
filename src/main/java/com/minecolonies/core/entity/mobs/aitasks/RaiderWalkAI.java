package com.minecolonies.core.entity.mobs.aitasks;

import com.ldtteam.structurize.util.BlockUtils;
import com.minecolonies.api.IMinecoloniesAPI;
import com.minecolonies.api.colony.colonyEvents.EventStatus;
import com.minecolonies.api.colony.colonyEvents.IColonyEvent;
import com.minecolonies.api.colony.colonyEvents.IColonyRaidEvent;
import com.minecolonies.api.entity.ai.IStateAI;
import com.minecolonies.api.entity.ai.combat.CombatAIStates;
import com.minecolonies.api.entity.ai.statemachine.states.IState;
import com.minecolonies.api.entity.ai.statemachine.tickratestatemachine.ITickRateStateMachine;
import com.minecolonies.api.entity.ai.statemachine.tickratestatemachine.TickingTransition;
import com.minecolonies.api.entity.ai.workers.util.IBuilderUndestroyable;
import com.minecolonies.api.entity.mobs.AbstractEntityRaiderMob;
import com.minecolonies.api.entity.pathfinding.IPathJob;
import com.minecolonies.api.items.ModTags;
import com.minecolonies.api.util.BlockPosUtil;
import com.minecolonies.api.util.DamageSourceKeys;
import com.minecolonies.api.util.Log;
import com.minecolonies.api.util.MathUtils;
import com.minecolonies.core.colony.buildings.AbstractBuilding;
import com.minecolonies.core.colony.events.raid.HordeRaidEvent;
import com.minecolonies.core.colony.events.raid.pirateEvent.ShipBasedRaiderUtils;
import com.minecolonies.core.entity.pathfinding.pathresults.PathResult;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

import static com.minecolonies.api.util.BlockPosUtil.HORIZONTAL_DIRS;
import static com.minecolonies.api.util.constant.Constants.TICKS_SECOND;

/**
 * AI for handling the raiders walking directions
 */
public class RaiderWalkAI implements IStateAI
{
    /**
     * The entity using this AI
     */
    private final AbstractEntityRaiderMob raider;

    /**
     * Target block we're walking to
     */
    private BlockPos targetBlock = null;

    /**
     * Campfire walk timer
     */
    private long walkTimer = 0;

    /**
     * Random path result.
     */
    private PathResult<? extends IPathJob> randomPathResult;

    /**
     * Count of ladders this raider can place.
     */
    private int ladderCount = 3;

    /**
     * Count of leave bridge blocks this raider can place.
     */
    private int leaveBridgeCount = 3;

    /**
     * Count of leave bridge blocks this raider can place.
     */
    private int breakBlockCount = 3;

    /**
     * If we are currently trying to move to a random block.
     */
    private boolean walkInBuildingState = false;

    /**
     * Unstuck cooldown
     */
    private int unstuckCooldown;

    public RaiderWalkAI(final AbstractEntityRaiderMob raider, final ITickRateStateMachine<IState> stateMachine)
    {
        this.raider = raider;
        stateMachine.addTransition(new TickingTransition<>(CombatAIStates.NO_TARGET, this::walk, () -> null, 80));
        stateMachine.addTransition(new TickingTransition<>(CombatAIStates.NO_TARGET, this::unstuck, () -> null, 20));
    }

    private boolean unstuck()
    {
        final int stuckLevel = raider.getNavigation().getStuckLevel();
        if (stuckLevel >= 3)
        {
            if (unstuckCooldown <= 0)
            {
                // Stuck levels vary. Usually between 3 and 8 and rarely it drops over. So we want to do sth like:
                // Does it seem we're stuck on a solid block?
                // -- Yes, check if we can place ladder,
                // -- --> Yes, place ladder + cooldown
                // -- --> No, break block + longer cooldown
                // -- No, check if there is a gap in front of us (must be more than 1 deep)
                // -- --> Yes, gap, place leaf + cooldown
                // -- --> No, are we significantly above the y level of the colony center?
                // -- -- --> Yes, break block below
                // -- -- --> No, do nothin.

                final Direction facing = Direction.Plane.HORIZONTAL.getRandomDirection(raider.getRandom());
                BlockPos unstuckPos = raider.blockPosition().above().relative(facing);
                final BlockState worldState = raider.level().getBlockState(unstuckPos);
                if (BlockUtils.isAnySolid(worldState))
                {
                    // Check if there is a block in there somewhere. If so, go up.
                    BlockState currentStuckState = raider.level().getBlockState(unstuckPos.relative(facing.getOpposite()));
                    while (currentStuckState.getBlock() == Blocks.LADDER)
                    {
                        unstuckPos = unstuckPos.above();
                        currentStuckState = raider.level().getBlockState(unstuckPos.relative(facing.getOpposite()));
                    }
                    final BlockPos relative = unstuckPos.relative(facing.getOpposite());

                    if (!raider.level().getBlockState(relative).isSolid())
                    {
                        for (final Direction dir : HORIZONTAL_DIRS)
                        {
                            final BlockState toPlace = Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, dir.getOpposite());
                            if (Blocks.LADDER.canSurvive(toPlace, raider.level, relative))
                            {
                                raider.level().setBlockAndUpdate(relative, toPlace);
                                unstuckCooldown += 200;
                                return true;
                            }
                        }
                    }

                    if (IMinecoloniesAPI.getInstance().getConfig().getServer().raidersbreakblocks.get()
                          && !currentStuckState.isAir()
                          && (currentStuckState.canBeReplaced()
                                || (!(currentStuckState.getBlock() instanceof IBuilderUndestroyable && !currentStuckState.is(ModTags.indestructible)
                                        && currentStuckState.getBlock() != Blocks.LADDER))))
                    {
                        if (breakBlocksAhead(raider.level(), unstuckPos, facing))
                        {
                            unstuckCooldown += 400;
                            return true;
                        }
                    }

                    //todo bridge attempt
                }

                Log.getLogger().warn("Stuck stuck stuck!!!!" + stuckLevel);
                if (stuckLevel > 6)
                {
                    if (IMinecoloniesAPI.getInstance().getConfig().getServer().raidersbreakblocks.get())
                    {
                        for (int i = 1; i <= 6; i++)
                        {
                            if (!raider.level().isEmptyBlock(BlockPos.containing(raider.position()).relative(facing, i)) || !raider.level()
                                                                                                                               .isEmptyBlock(BlockPos.containing(raider.position())
                                                                                                                                               .relative(facing, i)
                                                                                                                                               .above()))
                            {
                                breakBlocksAhead(raider.level(), BlockPos.containing(raider.position()).relative(facing, i - 1), facing);
                                break;
                            }
                        }
                    }
                }
            }
            else
            {
                unstuckCooldown-=100;
            }
        }
        // are we in front of a hole? build a bridge. Each entity can bridge at most 5 blocks by themselves. (make it depend on difficulty)

        // are we in front of a wall? place a ladder, each entity can ladder up to 5 blocks by themselves. (make it depend on difficulty)

        // are we at a ladder? and can't place any more? go up and try to break the highest block above the ladder.

        // are we stuck at a wall with over 10 ladders up already? then break one block if we can (each entity can break 1 block!)

        return false;
    }

    /**
     * Walk raider towards the colony or campfires
     *
     * @return
     */
    private boolean walk()
    {
        if (raider.getColony() != null)
        {
            final IColonyEvent event = raider.getColony().getEventManager().getEventByID(raider.getEventID());
            if (event == null)
            {
                return false;
            }

            if (event.getStatus() == EventStatus.PREPARING && event instanceof HordeRaidEvent)
            {
                walkToCampFire();
                return false;
            }
            raider.setTempEnvDamageImmunity(false);

            if (targetBlock == null || raider.level.getGameTime() > walkTimer)
            {
                targetBlock = raider.getColony().getRaiderManager().getRandomBuilding();
                walkTimer = raider.level.getGameTime() + TICKS_SECOND * 240;

                final List<BlockPos> wayPoints = ((IColonyRaidEvent) event).getWayPoints();
                final BlockPos moveToPos = ShipBasedRaiderUtils.chooseWaypointFor(wayPoints, raider.blockPosition(), targetBlock);
                raider.getNavigation().moveToXYZ(moveToPos.getX(), moveToPos.getY(), moveToPos.getZ(), !moveToPos.equals(targetBlock) && moveToPos.distManhattan(wayPoints.get(0)) > 50 ? 1.8 : 1.1);
                walkInBuildingState = false;
                randomPathResult = null;
            }
            else if (walkInBuildingState)
            {
                final BlockPos moveToPos = findRandomPositionToWalkTo();
                if (moveToPos != null)
                {
                    if (moveToPos == BlockPos.ZERO)
                    {
                        walkInBuildingState = false;
                        targetBlock = null;
                        return false;
                    }
                    raider.getNavigation().moveToXYZ(moveToPos.getX(), moveToPos.getY(), moveToPos.getZ(), 0.9);
                    if (raider.blockPosition().distSqr(moveToPos) < 4)
                    {
                        if (raider.getRandom().nextDouble() < 0.25)
                        {
                            walkInBuildingState = false;
                            targetBlock = null;
                        }
                        else
                        {
                            randomPathResult = null;
                            walkTimer = raider.level.getGameTime() + TICKS_SECOND * 60;
                            findRandomPositionToWalkTo();
                        }
                    }
                }
            }
            else if (raider.blockPosition().distSqr(targetBlock) < 25)
            {
                findRandomPositionToWalkTo();
                walkTimer = raider.level.getGameTime() + TICKS_SECOND * 30;
                walkInBuildingState = true;
            }
            else if (raider.getNavigation().isDone() || raider.getNavigation().getDesiredPos() == null)
            {
                final List<BlockPos> wayPoints = ((IColonyRaidEvent) event).getWayPoints();
                final BlockPos moveToPos = ShipBasedRaiderUtils.chooseWaypointFor(wayPoints, raider.blockPosition(), targetBlock);

                if (moveToPos.equals(BlockPos.ZERO))
                {
                    Log.getLogger().warn("Raider trying to path to zero position, target pos:" + targetBlock + " Waypoints:");
                    for (final BlockPos pos : wayPoints)
                    {
                        Log.getLogger().warn(pos.toShortString());
                    }
                }
                else if (raider.getNavigation().isStuck())
                {
                    return false;
                }

                raider.getNavigation()
                  .moveToXYZ(moveToPos.getX(), moveToPos.getY(), moveToPos.getZ(), !moveToPos.equals(targetBlock) && moveToPos.distManhattan(wayPoints.get(0)) > 50 ? 1.8 : 1.1);
            }
        }

        return false;
    }

    protected BlockPos findRandomPositionToWalkTo()
    {
        if (randomPathResult == null || randomPathResult.failedToReachDestination())
        {
            if (raider.getColony().getBuildingManager().getBuilding(targetBlock) instanceof AbstractBuilding building
                  && building.getBuildingLevel() > 0
                  && !building.getCorners().getA().equals(building.getCorners().getB()))
            {
                randomPathResult = raider.getNavigation().moveToRandomPos(10, 0.9, building.getCorners());
                if (randomPathResult != null)
                {
                    randomPathResult.getJob().getPathingOptions().withCanEnterDoors(true).withToggleCost(0).withNonLadderClimbableCost(0);
                }
            }
            else
            {
                return BlockPos.ZERO;
            }
        }

        if (randomPathResult.isPathReachingDestination())
        {
            return randomPathResult.getPath().getEndNode().asBlockPos();
        }

        if (randomPathResult.isCancelled())
        {
            randomPathResult = null;
            return null;
        }

        return null;
    }

    /**
     * Chooses and walks to a random campfire
     */
    private void walkToCampFire()
    {
        if (raider.level.getGameTime() - walkTimer < 0)
        {
            return;
        }

        final BlockPos campFire = ((HordeRaidEvent) raider.getColony().getEventManager().getEventByID(raider.getEventID())).getRandomCampfire();

        if (campFire == null)
        {
            return;
        }

        walkTimer = raider.level.getGameTime() + raider.level.random.nextInt(1000);
        final BlockPos posAroundCampfire = BlockPosUtil.getRandomPosition(raider.level,
          campFire,
          BlockPos.ZERO,
          3,
          6);
        if (posAroundCampfire != null && posAroundCampfire != BlockPos.ZERO)
        {
            raider.getNavigation().moveToXYZ(posAroundCampfire.getX(), posAroundCampfire.getY(), posAroundCampfire.getZ(), 1.0);
        }
    }

    /**
     * Places ladders
     */
    private boolean placeLadders(final BlockPos inputPos)
    {
        BlockPos ladderPos = inputPos;
        while (raider.level().getBlockState(ladderPos).getBlock() == Blocks.LADDER && ladderPos.getY() <= raider.blockPosition().getY() + 10)
        {
            ladderPos = ladderPos.above();
        }

        return tryPlaceLadderAt(raider.level(), ladderPos);
    }

    /**
     * Tries to place leaves
     */
    private void placeLeaves()
    {
        final Level world = raider.level();

        final Direction badFacing = BlockPosUtil.getFacing(BlockPos.containing(raider.position()), raider.getNavigation().getDesiredPos()).getOpposite();

        for (final Direction dir : HORIZONTAL_DIRS)
        {
            if (dir == badFacing)
            {
                continue;
            }

            for (int i = 1; i <= (dir == badFacing.getOpposite() ? 3 : 1); i++)
            {
                final BlockPos pos = BlockPos.containing(raider.position()).below().relative(dir, i);
                if (world.isEmptyBlock(pos))
                {
                    leaveBridgeCount--;
                    world.setBlockAndUpdate(pos, Blocks.ACACIA_LEAVES.defaultBlockState());
                    break;
                }
            }
        }
    }

    /**
     * Tries to randomly break blocks
     */
    private void breakBlocks()
    {
        final Level world = raider.level();
        final Direction facing = BlockPosUtil.getFacing(BlockPos.containing(raider.position()), raider.getNavigation().getDesiredPos());

        if (breakBlocksAhead(world, raider.blockPosition(), facing) && raider.getHealth() >= raider.getMaxHealth() / 2)
        {
            breakBlockCount--;
            raider.hurt(world.damageSources().source(DamageSourceKeys.STUCK_DAMAGE), (float) Math.max(0.5, raider.getHealth() / 20.0));
        }
    }

    /**
     * Tries to place a ladder at the given position
     *
     * @param world world to use
     * @param pos   position to set
     */
    private boolean tryPlaceLadderAt(final Level world, final BlockPos pos)
    {
        final BlockState state = world.getBlockState(pos);
        if ((state.canBeReplaced() || state.isAir())
              || (state.getBlock() != Blocks.LADDER
              && !(state.getBlock() instanceof IBuilderUndestroyable)
              && !state.is(ModTags.indestructible)))
        {
            for (final Direction dir : HORIZONTAL_DIRS)
            {
                final BlockState toPlace = Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, dir.getOpposite());
                if (BlockUtils.isAnySolid(world.getBlockState(pos.relative(dir))) && Blocks.LADDER.canSurvive(toPlace, world, pos))
                {
                    world.setBlockAndUpdate(pos, toPlace);
                    ladderCount--;
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Attempt to break blocks that are blocking the entity to reach its destination.
     *
     * @param world  the world it is in.
     * @param start  the position the entity is at.
     * @param facing the direction the goal is in.
     */
    private boolean breakBlocksAhead(final Level world, final BlockPos start, final Direction facing)
    {
        // In entity
        if (!world.isEmptyBlock(start))
        {
            return setAirIfPossible(world, start);
        }

        // Above entity
        if (!world.isEmptyBlock(start.above(3)))
        {
            return setAirIfPossible(world, start.above(3));
        }

        // Goal direction up
        if (!world.isEmptyBlock(start.above().relative(facing)))
        {
            return setAirIfPossible(world, start.above().relative(facing));
        }

        // In goal direction
        if (!world.isEmptyBlock(start.relative(facing)))
        {
            return setAirIfPossible(world, start.relative(facing));
        }
        return false;
    }

    /**
     * Check if the block at the position is indestructible, if not, attempt to break it.
     *
     * @param world the world the block is in.
     * @param pos   the pos the block is at.
     */
    private boolean setAirIfPossible(final Level world, final BlockPos pos)
    {
        final BlockState state = world.getBlockState(pos);
        final Block blockAtPos = state.getBlock();
        if (blockAtPos instanceof IBuilderUndestroyable || state.is(ModTags.indestructible))
        {
            return false;
        }
        return world.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
    }
}

