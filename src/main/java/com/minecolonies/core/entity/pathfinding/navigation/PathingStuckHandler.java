package com.minecolonies.core.entity.pathfinding.navigation;

import com.minecolonies.api.entity.pathfinding.IStuckHandler;
import com.minecolonies.api.entity.pathfinding.IStuckHandlerEntity;
import com.minecolonies.api.util.BlockPosUtil;
import com.minecolonies.api.util.constant.ColonyConstants;
import com.minecolonies.core.entity.pathfinding.SurfaceType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.phys.Vec3;

import java.util.Random;

/**
 * Stuck handler for pathing
 */
public class PathingStuckHandler implements IStuckHandler
{
    /**
     * The distance at which we consider a target to arrive
     */
    private static final double MIN_TARGET_DIST = 3;

    /**
     * Constants related to tp.
     */
    private static final int MIN_TP_DELAY    = 120 * 20;
    private static final int MIN_DIST_FOR_TP = 10;

    /**
     * Rough amount of ticks taken to travel one block
     */
    private static final int TICKS_PER_BLOCK = 7;

    /**
     * Amount of path steps allowed to teleport on stuck, 0 = disabled
     */
    private int teleportRange = 0;

    /**
     * Max timeout per block to go, default = 10sec per block
     */
    private int timePerBlockDistance = 200;

    /**
     * The current stucklevel, determines actions taken
     */
    private int stuckLevel = 0;

    /**
     * Global timeout counter, used to determine when we're completly stuck
     */
    private int globalTimeout = 0;

    /**
     * The previously desired go to position of the entity
     */
    private BlockPos prevDestination = BlockPos.ZERO;

    /**
     * Whether teleport to goal at full stuck is enabled
     */
    private boolean canTeleportGoal = false;

    /**
     * Chance to bypass moving away.
     */
    private double chanceToByPassMovingAway = 0;

    /**
     * Temporary comparison variables to compare with last update
     */
    private boolean hadPath         = false;
    private int     lastPathIndex   = -1;
    private int     progressedNodes = 0;

    /**
     * Delay before taking unstuck actions in ticks, default 60 seconds
     */
    private int delayBeforeActions       = 10 * 20;
    private int delayToNextUnstuckAction = delayBeforeActions;

    /**
     * The start position of moving away unstuck
     */
    private BlockPos  moveAwayStartPos = BlockPos.ZERO;
    private Direction movingAwayDir    = Direction.EAST;

    private Random rand = new Random();

    private PathingStuckHandler()
    {
    }

    /**
     * Creates a new stuck handler
     *
     * @return new stuck handler
     */
    public static PathingStuckHandler createStuckHandler()
    {
        return new PathingStuckHandler();
    }

    /**
     * Checks the entity for stuck
     *
     * @param navigator navigator to check
     */
    @Override
    public void checkStuck(final AbstractAdvancedPathNavigate navigator)
    {
        if (navigator.getDesiredPos() == null || navigator.getDesiredPos().equals(BlockPos.ZERO))
        {
            resetGlobalStuckTimers();
            return;
        }

        if (navigator.getOurEntity() instanceof IStuckHandlerEntity && !((IStuckHandlerEntity) navigator.getOurEntity()).canBeStuck())
        {
            resetGlobalStuckTimers();
            return;
        }

        final double distanceToGoal =
          navigator.getOurEntity().position().distanceTo(new Vec3(navigator.getDesiredPos().getX(), navigator.getDesiredPos().getY(), navigator.getDesiredPos().getZ()));

        // Close enough to be considered at the goal
        if (distanceToGoal < MIN_TARGET_DIST)
        {
            resetGlobalStuckTimers();
            return;
        }

        // Global timeout check
        if (prevDestination.equals(navigator.getDesiredPos()))
        {
            globalTimeout++;

            // Try path first, if path fits target pos
            if (globalTimeout > Math.max(MIN_TP_DELAY, timePerBlockDistance * Math.max(MIN_DIST_FOR_TP, distanceToGoal)))
            {
                completeStuckAction(navigator);
            }
        }
        else
        {
            resetGlobalStuckTimers();
        }

        delayToNextUnstuckAction--;
        prevDestination = navigator.getDesiredPos();

        if (navigator.getPath() == null || navigator.getPath().isDone())
        {
            // With no path reset the last path index point to -1
            lastPathIndex = -1;
            progressedNodes = 0;

            // Stuck when we have no path and had no path last update before
            if (!hadPath)
            {
                tryUnstuck(navigator);
            }
        }
        else
        {
            if (navigator.getPath().getNextNodeIndex() == lastPathIndex)
            {
                // Stuck when we have a path, but are not progressing on it
                tryUnstuck(navigator);
            }
            else
            {
                if (lastPathIndex != -1)
                {
                    if (lastPathIndex != navigator.getPath().getNextNodeIndex())
                    {
                        // Delay next action when the entity is moving
                        delayToNextUnstuckAction = Math.max(delayToNextUnstuckAction, 100);
                    }
                    else if (lastPathIndex < 2 && navigator.getPath().getNodeCount() > 2)
                    {
                        // Skip ahead on the node index, incase the starting position is bad
                        navigator.getPath().setNextNodeIndex(2);
                    }

                    if ((stuckLevel == 0 || navigator.getPath().getTarget().distSqr(prevDestination) < 25))
                    {
                        progressedNodes = navigator.getPath().getNextNodeIndex() > lastPathIndex ? progressedNodes + 1 : progressedNodes - 1;

                        if (progressedNodes > 5 && (navigator.getPath().getEndNode() == null || !moveAwayStartPos.equals(navigator.getPath().getEndNode().asBlockPos())))
                        {
                            // Not stuck when progressing
                            resetStuckTimers();
                        }
                    }
                }
            }
        }

        lastPathIndex = navigator.getPath() != null ? navigator.getPath().getNextNodeIndex() : -1;

        hadPath = navigator.getPath() != null && !navigator.getPath().isDone();
    }

    /**
     * Resets global stuck timers
     */
    private void resetGlobalStuckTimers()
    {
        globalTimeout = 0;
        prevDestination = BlockPos.ZERO;
        resetStuckTimers();
    }

    /**
     * Final action when completly stuck before resetting stuck handler and path
     */
    private void completeStuckAction(final AbstractAdvancedPathNavigate navigator)
    {
        final BlockPos desired = navigator.getDesiredPos();
        final Level world = navigator.getOurEntity().level();
        final Mob entity = navigator.getOurEntity();

        if (canTeleportGoal)
        {
            final BlockPos tpPos = BlockPosUtil.findAround(world, desired, 10, 10,
              (posworld, pos) -> SurfaceType.getSurfaceType(posworld, posworld.getBlockState(pos.below()), pos.below()) == SurfaceType.WALKABLE
                                   && SurfaceType.getSurfaceType(posworld, posworld.getBlockState(pos), pos) == SurfaceType.DROPABLE
                                   && SurfaceType.getSurfaceType(posworld, posworld.getBlockState(pos.above()), pos.above()) == SurfaceType.DROPABLE);
            if (tpPos != null)
            {
                entity.teleportTo(tpPos.getX() + 0.5, tpPos.getY(), tpPos.getZ() + 0.5);
            }
        }

        navigator.stop();
        resetGlobalStuckTimers();
    }

    /**
     * Tries unstuck options depending on the level
     */
    private void tryUnstuck(final AbstractAdvancedPathNavigate navigator)
    {
        if (delayToNextUnstuckAction > 0)
        {
            return;
        }
        delayToNextUnstuckAction = 100;

        // Clear path
        if (stuckLevel == 0)
        {
            stuckLevel++;
            delayToNextUnstuckAction = 100;
            navigator.getOurEntity().stopRiding();
            BlockPos desired = navigator.getDesiredPos();
            navigator.stop();
            navigator.setDesiredPos(desired);
            return;
        }

        // Move away, with chance to skip this.
        if (stuckLevel == 1 && rand.nextDouble() > chanceToByPassMovingAway || ((stuckLevel >= 3 && stuckLevel <= 8) && rand.nextBoolean()))
        {
            delayToNextUnstuckAction = 600;

            if (navigator.getPath() != null)
            {
                moveAwayStartPos = navigator.getPath().getNodePos(navigator.getPath().getNextNodeIndex());
            }
            else
            {
                moveAwayStartPos = navigator.getOurEntity().blockPosition().above();
            }

            navigator.stop();
            final int range = ColonyConstants.rand.nextInt(20) + Math.min(100, BlockPosUtil.distManhattan(navigator.ourEntity.blockPosition(), prevDestination));
            navigator.moveTowards(navigator.getOurEntity().blockPosition().relative(movingAwayDir, 40), range, 1.0f);
            movingAwayDir = movingAwayDir.getClockWise();
            navigator.setPauseTicks(range * TICKS_PER_BLOCK);
            return;
        }

        // Skip ahead
        if (stuckLevel == 2 && teleportRange > 0 && hadPath)
        {
            int index = Math.min(navigator.getPath().getNextNodeIndex() + teleportRange, navigator.getPath().getNodeCount() - 1);
            final Node togo = navigator.getPath().getNode(index);
            navigator.getOurEntity().teleportTo(togo.x + 0.5d, togo.y, togo.z + 0.5d);
            delayToNextUnstuckAction = 300;
        }

        stuckLevel++;

        if (stuckLevel == 9)
        {
            completeStuckAction(navigator);
            resetStuckTimers();
        }
    }

    /**
     * Resets timers
     */
    private void resetStuckTimers()
    {
        delayToNextUnstuckAction = delayBeforeActions;
        lastPathIndex = -1;
        progressedNodes = 0;
        stuckLevel = 0;
        moveAwayStartPos = BlockPos.ZERO;
    }

    public PathingStuckHandler withChanceToByPassMovingAway(final double chance)
    {
        chanceToByPassMovingAway = chance;
        return this;
    }

    /**
     * Enables teleporting a certain amount of steps along a generated path
     *
     * @param steps steps to teleport
     * @return this
     */
    public PathingStuckHandler withTeleportSteps(int steps)
    {
        teleportRange = steps;
        return this;
    }

    public PathingStuckHandler withTeleportOnFullStuck()
    {
        canTeleportGoal = true;
        return this;
    }

    /**
     * Sets the time per block distance to travel, before timing out
     *
     * @param time in ticks to set
     * @return this
     */
    public PathingStuckHandler withTimePerBlockDistance(int time)
    {
        timePerBlockDistance = time;
        return this;
    }

    /**
     * Sets the delay before taking stuck actions
     *
     * @param delay to set
     * @return this
     */
    public PathingStuckHandler withDelayBeforeStuckActions(int delay)
    {
        delayBeforeActions = delay;
        return this;
    }

    @Override
    public int getStuckLevel()
    {
        return stuckLevel;
    }
}
