package net.tianyang928.littleant.entity.ai.goal;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.tianyang928.littleant.entity.AntEntity;

public class BetterFloatGoal extends FloatGoal {

    private final Mob mob;

    public BetterFloatGoal(Mob mob) {
        super(mob);
        this.mob = mob;
    }

    @Override
    public boolean canUse() {
        if(this.mob instanceof AntEntity antEntity) {
            if(antEntity.tryGettingDownWater){
                this.mob.getNavigation().setCanFloat(false);
                return false;
            }
        }

        this.mob.getNavigation().setCanFloat(true);
        return super.canUse();
    }
}
