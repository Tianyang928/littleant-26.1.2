package net.tianyang928.littleant.entity.ai.sense;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;

import java.util.*;

public class FindDropList {
    private final PathfinderMob mob;

    public FindDropList(PathfinderMob mob) {
        this.mob = mob;
    }

    public List<BlockPos> setTarget(Item item, int limit) {
        if (item == null || limit <= 0) return List.of();
        List<ItemEntity> es = mob.level().getEntities(EntityType.ITEM, mob.getBoundingBox().inflate(64), e -> e.isAlive() && e.getItem().is(item) && mob.distanceToSqr(e) <= 4096);
        es.sort(Comparator.comparingDouble(mob::distanceToSqr));
        return es.stream().limit(Math.min(limit, 256)).map(Entity::blockPosition).toList();
    }
}
