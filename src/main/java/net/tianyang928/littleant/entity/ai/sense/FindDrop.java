package net.tianyang928.littleant.entity.ai.sense;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.phys.AABB;
import net.tianyang928.littleant.LittleAnt;

import java.util.List;

public class FindDrop {
    // 可以透视

    private final int SEARCH_RADIUS = 64;
    private boolean hasTarget;
    public Entity resultEntity;
    private final PathfinderMob mob;
    private Item itemToFind;

    public FindDrop(PathfinderMob mob, Item itemToFind){
        this.mob = mob;
        this.itemToFind = itemToFind;
        this.hasTarget = true;
        this.resultEntity = null;
    }

    public boolean canUse() {
        return this.hasTarget && this.itemToFind != null;
    }

    public BlockPos setTarget(Item itemToFind) {
        this.itemToFind = itemToFind;
        this.resultEntity = null;
        this.hasTarget = true;

        if(canUse()){
            start();
        }
        return resultEntity == null? null : resultEntity.blockPosition();
    }

    public void clearTarget() {
        this.hasTarget = false;
    }


    public void start() {
        Entity result = null;
        List<ItemEntity> targetedDrops = this.mob.level().getEntities(
                EntityType.ITEM,
                new AABB(mob.getX()-32, mob.getY()-32, mob.getZ()-32, mob.getX()+32, mob.getY()+32, mob.getZ()+32),  // 范围边界框
                Entity::isAlive
        );
        for (ItemEntity potentialTarget : targetedDrops) {
            if(!potentialTarget.getItem().is(this.itemToFind)){
                continue;
            }
            if(result == null || this.mob.distanceToSqr(result) > this.mob.distanceToSqr(potentialTarget.position())) {
                result = potentialTarget;
            }
        }
        this.hasTarget = false;
        if(result != null){
            this.resultEntity = result;
            LittleAnt.LOGGER.info("[FindDrop] 找到目标: {}", this.resultEntity.position());
            return;
        }
        this.resultEntity = null;
        LittleAnt.LOGGER.info("[FindDrop] 未找到目标");
        clearTarget();
    }
}
