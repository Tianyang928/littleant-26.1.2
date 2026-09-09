package net.tianyang928.littleant.entity.ai.goal;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.tianyang928.littleant.entity.AntEntity;

/** MeleeAttackGoal variant whose target is supplied directly by an entity instance. */
public final class EntityMeleeAttackGoal extends MeleeAttackGoal {
    private LivingEntity target;
    public EntityMeleeAttackGoal(AntEntity ant, LivingEntity target, boolean follow) {
        super(ant, ant.speedModifier,follow);
        this.target = null;
        this.setTarget(target);
    }
    @Override public boolean canUse() {
        if (target == null || !target.isAlive()) return false;
        mob.setTarget(target);
        return super.canUse();
    }

    @Override public boolean canContinueToUse() {
        if (target == null || !target.isAlive()) return false;
        mob.setTarget(target);
        return super.canContinueToUse();
    }

    @Override public void tick() {
        if (target != null && target.isAlive()) mob.setTarget(target);
        super.tick();
    }

    public void setTarget(LivingEntity target) {
        LivingEntity previous = this.target;
        this.target = target;
        // Keep MeleeAttackGoal's Mob target in sync when the goal is reused.
        // The scheduler normally does this in canUse() before registering a
        // newly-created goal, while command-driven attacks update an already
        // registered goal directly.
        if (target != null && target.isAlive()) {
            mob.setTarget(target);
        } else if (mob.getTarget() == previous) {
            mob.setTarget(null);
        }
    }
}
