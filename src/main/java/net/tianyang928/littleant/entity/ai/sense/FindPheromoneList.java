package net.tianyang928.littleant.entity.ai.sense;

import net.minecraft.core.*;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.tianyang928.littleant.block.ModBlocks;
import net.tianyang928.littleant.blockentity.PheromoneBlockEntity;

import java.util.*;

public class FindPheromoneList {
    private final PathfinderMob mob;

    public FindPheromoneList(PathfinderMob mob) {
        this.mob = mob;
    }

    public List<BlockPos> setTarget(String type, int limit) {
        if (type == null || type.isEmpty() || limit <= 0) return List.of();
        List<BlockPos> r = new ArrayList<>();
        ChunkPos c = new ChunkPos(mob.blockPosition());
        for (ChunkPos cp : ChunkPos.rangeClosed(c, 5).toList()) {
            LevelChunk ch = mob.level().getChunkSource().getChunkNow(cp.x, cp.z);
            if (ch != null) for (BlockEntity e : ch.getBlockEntities().values())
                if (e.getBlockPos().distSqr(mob.blockPosition()) <= 4096 && e.getBlockState().is(ModBlocks.PHEROMONE_BLOCK.get()) && e instanceof PheromoneBlockEntity p && p.getPheromoneList().containsKey(type))
                    r.add(e.getBlockPos().immutable());
        }
        r.sort(Comparator.comparingDouble(p -> mob.distanceToSqr(p.getX(), p.getY(), p.getZ())));
        return List.copyOf(r.subList(0, Math.min(Math.min(limit, 256), r.size())));
    }
}
