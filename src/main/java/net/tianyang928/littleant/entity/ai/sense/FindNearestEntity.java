package net.tianyang928.littleant.entity.ai.sense;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.phys.AABB;
import net.tianyang928.littleant.LittleAnt;

import java.util.Comparator;
import java.util.List;

public class FindNearestEntity {
    private final PathfinderMob mob;

    public FindNearestEntity(PathfinderMob mob) {
        this.mob = mob;
    }

    public int setTarget(EntityType<?> type) {
        if (type == null) return -1;
        List<Entity> es = mob.level().getEntities((Entity)null, mob.getBoundingBox().inflate(64), e -> e.isAlive() && e != mob && e.getType() == type && mob.distanceToSqr(e) <= 4096 && mob.hasLineOfSight(e));
        es.sort(Comparator.comparingDouble(mob::distanceToSqr));
        if(es.isEmpty()) return -1;
        return es.stream().limit(1).map(Entity::getId).toList().getFirst();
    }
}
