package net.tianyang928.littleant.entity.ai.sense;

import net.minecraft.core.*;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.*;

public class FindBlockEntityList {
    private final PathfinderMob mob;

    public FindBlockEntityList(PathfinderMob mob) {
        this.mob = mob;
    }

    public List<BlockPos> setTarget(List<Block> blocks, int limit) {
        if (blocks == null || blocks.isEmpty() || limit <= 0) return List.of();
        List<BlockPos> r = new ArrayList<>();
        ChunkPos c = new ChunkPos(mob.blockPosition());
        for (ChunkPos cp : ChunkPos.rangeClosed(c, 5).toList()) {
            LevelChunk ch = mob.level().getChunkSource().getChunkNow(cp.x, cp.z);
            if (ch != null) for (BlockEntity e : ch.getBlockEntities().values())
                if (blocks.contains(e.getBlockState().getBlock()) && e.getBlockPos().distSqr(mob.blockPosition()) <= 4096 && visible(e.getBlockPos()))
                    r.add(e.getBlockPos().immutable());
        }
        r.sort(Comparator.comparingDouble(p -> mob.distanceToSqr(p.getX(), p.getY(), p.getZ())));
        return List.copyOf(r.subList(0, Math.min(Math.min(limit, 256), r.size())));
    }

    private boolean visible(BlockPos p) {
        var hit = mob.level().clip(new ClipContext(mob.getEyePosition(), Vec3.atCenterOf(p), ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, mob));
        return hit.getType() != HitResult.Type.MISS && hit.getBlockPos().equals(p);
    }
}
