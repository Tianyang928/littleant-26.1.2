package net.tianyang928.littleant.entity.ai.sense;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.tianyang928.littleant.LittleAnt;
import net.tianyang928.littleant.block.ModBlocks;
import net.tianyang928.littleant.blockentity.PheromoneBlockEntity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class FindNearestPheromone {
    private final PathfinderMob mob;

    public FindNearestPheromone(PathfinderMob mob) {
        this.mob = mob;
    }

    public BlockPos setTarget(String type) {
        if (type == null || type.isEmpty()) return null;
        List<BlockPos> r = new ArrayList<>();
        ChunkPos c = new ChunkPos(mob.blockPosition());
        for (ChunkPos cp : ChunkPos.rangeClosed(c, 5).toList()) {
            LevelChunk ch = mob.level().getChunkSource().getChunkNow(cp.x, cp.z);
            if (ch != null) for (BlockEntity e : ch.getBlockEntities().values())
                if (e.getBlockPos().distSqr(mob.blockPosition()) <= 4096 && e.getBlockState().is(ModBlocks.PHEROMONE_BLOCK.get()) && e instanceof PheromoneBlockEntity p && p.getPheromoneList().containsKey(type))
                    r.add(e.getBlockPos().immutable());
        }
        r.sort(Comparator.comparingDouble(p -> mob.distanceToSqr(p.getX(), p.getY(), p.getZ())));
        if(r.isEmpty()) return null;
        return r.getFirst();
    }
}
