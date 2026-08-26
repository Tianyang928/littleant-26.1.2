package net.tianyang928.littleant.entity.ai.sense;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.phys.AABB;
import net.tianyang928.littleant.LittleAnt;

import java.util.List;
import java.util.UUID;

public class FindEntity {
    // 寻找方块实体和寻找方块一样不能透视

    private final int SEARCH_RADIUS = 64;
    private boolean hasTarget;
    public Entity resultEntity;
    private final PathfinderMob mob;
    private EntityType<?> entityToFind;

    public FindEntity(PathfinderMob mob, EntityType<?> entityToFind){
        this.mob = mob;
        this.entityToFind = entityToFind;
        this.hasTarget = true;
        this.resultEntity = null;
    }

    public boolean canUse() {
        return this.hasTarget && this.entityToFind != null;
    }

    public int setTarget(EntityType<?> entityToFind) {
        this.entityToFind = entityToFind;
        this.resultEntity = null;
        this.hasTarget = true;

        if(canUse()){
            start();
        }
        return resultEntity.getId();
    }

    public void clearTarget() {
        this.hasTarget = false;
    }


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
            LittleAnt.LOGGER.info("[FindEntity] 找到目标: {}", this.resultEntity.position());
            return;
        }
        this.resultEntity = null;
        LittleAnt.LOGGER.info("[FindEntity] 未找到目标");
        clearTarget();
    }

}
