package net.tianyang928.littleant.entity.ai.goal;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.tianyang928.littleant.entity.AntEntity;

/** MeleeAttackGoal variant whose target is supplied directly by an entity instance. */
public final class EntityMeleeAttackGoal extends MeleeAttackGoal {
    private final LivingEntity target;
    public EntityMeleeAttackGoal(AntEntity ant, LivingEntity target, boolean follow) {
        super(ant, ant.speedModifier,follow);
        this.target = target;
    }
    @Override public boolean canUse() {
        if (!target.isAlive()) return false;
        mob.setTarget(target);
        return super.canUse();
    }
}
