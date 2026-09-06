package net.tianyang928.littleant.entity.ai.sense;

import net.minecraft.world.entity.*;

import java.util.*;

public class FindEntityList {
    private final PathfinderMob mob;

    public FindEntityList(PathfinderMob mob) {
        this.mob = mob;
    }

    public List<Integer> setTarget(EntityType<?> type, int count) {
        if (type == null || count <= 0) return List.of();
        List<Entity> es = mob.level().getEntities((Entity)null, mob.getBoundingBox().inflate(64), e -> e.isAlive() && e != mob && e.getType() == type && mob.distanceToSqr(e) <= 4096 && mob.hasLineOfSight(e));
        es.sort(Comparator.comparingDouble(mob::distanceToSqr));
        return es.stream().limit(Math.min(count, 256)).map(Entity::getId).toList();
    }
}
