package net.tianyang928.littleant.entity.ai.goal;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.pathfinder.Path;
import net.tianyang928.littleant.entity.AntEntity;
import net.tianyang928.littleant.entity.ai.interaction.AntInteractionService;

import java.util.EnumSet;

/** Moves into reach, looks at a block/entity target and performs exactly one right click. */
public final class UseInteractionGoal extends Goal {
    public enum Kind { BLOCK, ENTITY }

    private static final double REACH_SQR = 4.5D * 4.5D;
    private final AntEntity ant;
    private final Kind kind;
    private final BlockPos blockPos;
    private final int entityId;
    private final Direction face;
    private final boolean secondaryUse;
    private final boolean useHeldItem;
    private Path path;
    private boolean finished;

    private UseInteractionGoal(AntEntity ant, Kind kind, BlockPos blockPos, int entityId,
                               Direction face, boolean useHeldItem) {
        this.ant = ant;
        this.kind = kind;
        this.blockPos = blockPos;
        this.entityId = entityId;
        this.face = face;
        this.secondaryUse = ant.isCrouching;
        this.useHeldItem = useHeldItem;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    public static UseInteractionGoal block(AntEntity ant, BlockPos pos, Direction face,
                                           boolean useHeldItem) {
        return new UseInteractionGoal(ant, Kind.BLOCK, pos.immutable(), -1, face, useHeldItem);
    }

    public static UseInteractionGoal entity(AntEntity ant, int entityId,
                                            boolean useHeldItem) {
        return new UseInteractionGoal(ant, Kind.ENTITY, null, entityId, Direction.UP, useHeldItem);
    }

    @Override
    public boolean canUse() {
        if (this.finished) return false;
        if (!targetValid()) return false;
        if (inReach()) return true;
        this.path = ant.getNavigation().createPath(targetPos(), 2, 64);
        return this.path != null;
    }

    @Override public void start() { if (!inReach() && path != null) ant.getNavigation().moveTo(path, ant.speedModifier); }
    @Override public boolean canContinueToUse() { return !finished && targetValid() && (inReach() || !ant.getNavigation().isDone()); }
    //@Override public boolean requiresUpdateEveryTick() { return true; }
    @Override public void stop() { ant.getNavigation().stop(); }

    @Override
    public void tick() {
        Entity target = targetEntity();
        if (kind == Kind.BLOCK) ant.getLookControl().setLookAt(blockPos.getX() + .5, blockPos.getY() + .5, blockPos.getZ() + .5);
        else if (target != null) ant.getLookControl().setLookAt(target);
        if (!inReach()) return;
        if (kind == Kind.BLOCK) AntInteractionService.useBlockAsMob(ant, blockPos, face, InteractionHand.MAIN_HAND, secondaryUse, useHeldItem);
        else AntInteractionService.interactEntityAsMob(ant, target, InteractionHand.MAIN_HAND, secondaryUse, useHeldItem);
        ant.swing(InteractionHand.MAIN_HAND, true);
        finished = true;
    }

    private boolean targetValid() {
        return kind == Kind.BLOCK ? !ant.level().getBlockState(blockPos).isAir()
                : kind == Kind.ENTITY ? targetEntity() != null && targetEntity().isAlive() : true;
    }

    private boolean inReach() {
        if (kind == Kind.BLOCK) return ant.distanceToSqr(blockPos.getCenter()) <= REACH_SQR;
        Entity target = targetEntity();
        return target != null && ant.distanceToSqr(target) <= REACH_SQR;
    }

    private BlockPos targetPos() { return kind == Kind.BLOCK ? blockPos : targetEntity().blockPosition(); }
    private Entity targetEntity() { return ant.level().getEntity(entityId); }
}
