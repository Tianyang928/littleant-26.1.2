package net.tianyang928.littleant.entity.ai.goal;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.tianyang928.littleant.LittleAnt;
import net.tianyang928.littleant.block.ModBlocks;

import java.util.EnumSet;
import java.util.List;

public class FindNearestEntityGoal extends Goal {
    // 寻找方块实体和寻找方块一样不能透视

    private final int SEARCH_RADIUS = 64;
    private boolean hasTarget;
    public Entity resultEntity;
    private final PathfinderMob mob;
    private EntityType<?> entityToFind;

    public FindNearestEntityGoal(PathfinderMob mob, EntityType<?> entityToFind){
        this.mob = mob;
        this.entityToFind = entityToFind;
        this.hasTarget = true;
        this.resultEntity = null;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        return this.hasTarget && this.entityToFind != null;
    }

    public void setTarget(EntityType<?> entityToFind) {
        this.entityToFind = entityToFind;
        this.resultEntity = null;
        this.hasTarget = true;
    }

    public void clearTarget() {
        this.hasTarget = false;
    }


    @Override
    public void start() {
        Entity result = null;
        List<Entity> targetedEntities = this.mob.level().getEntities(
                (Entity)null,
                new AABB(mob.getX()-64, mob.getY()-64, mob.getZ()-64, mob.getX()+64, mob.getY()+64, mob.getZ()+64),  // 范围边界框
                EntitySelector.NO_SPECTATORS  // 过滤条件
        );
        for (Entity potentialTarget : targetedEntities) {
            if(potentialTarget.getType() != this.entityToFind){
                continue;
            }
            if(potentialTarget == this.mob){
                continue;
            }
            if (this.mob.hasLineOfSight(potentialTarget)) {
                if(result == null || this.mob.distanceToSqr(result) > this.mob.distanceToSqr(potentialTarget.position())) {
                    result = potentialTarget;
                }
            }
        }
        this.hasTarget = false;
        if(result != null){
            this.resultEntity = result;
            LittleAnt.LOGGER.info("[FindNearestEntityGoal] 找到目标: {}", this.resultEntity.position());
            return;
        }
        this.resultEntity = null;
        LittleAnt.LOGGER.info("[FindNearestEntityGoal] 未找到目标");
    }
}
