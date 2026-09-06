package net.tianyang928.littleant.entity.ai.sense;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.phys.AABB;
import net.tianyang928.littleant.LittleAnt;

import java.util.Comparator;
import java.util.List;

public class FindNearestDrop {
    private final PathfinderMob mob;

    public FindNearestDrop(PathfinderMob mob) {
        this.mob = mob;
    }

    public BlockPos setTarget(Item item) {
        if (item == null) return null;
        List<ItemEntity> es = mob.level().getEntities(EntityType.ITEM, mob.getBoundingBox().inflate(64), e -> e.isAlive() && e.getItem().is(item) && mob.distanceToSqr(e) <= 4096);
        es.sort(Comparator.comparingDouble(mob::distanceToSqr));
        if(es.isEmpty()) return null;
        return es.stream().limit(1).toList().getFirst().blockPosition();
    }
}
