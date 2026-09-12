package net.tianyang928.littleant.entity.ai.sense;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.tianyang928.littleant.LittleAnt;
import net.tianyang928.littleant.block.ModBlocks;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class FindNearestBlockEntity {
    private final PathfinderMob mob;

    public FindNearestBlockEntity(PathfinderMob mob) {
        this.mob = mob;
    }

    public BlockPos setTarget(List<Block> blocks) {
        if (blocks == null || blocks.isEmpty()) return null;
        List<BlockPos> r = new ArrayList<>();
        ChunkPos c = new ChunkPos(mob.blockPosition());
        for (ChunkPos cp : ChunkPos.rangeClosed(c, 5).toList()) {
            LevelChunk ch = mob.level().getChunkSource().getChunkNow(cp.x, cp.z);
            if (ch != null) for (BlockEntity e : ch.getBlockEntities().values())
                if (blocks.contains(e.getBlockState().getBlock()) && e.getBlockPos().distSqr(mob.blockPosition()) <= 4096 && visible(e.getBlockPos()))
                    r.add(e.getBlockPos().immutable());
        }
        r.sort(Comparator.comparingDouble(p -> mob.distanceToSqr(p.getX(), p.getY(), p.getZ())));
        if(r.isEmpty()) return null;
        return r.getFirst();
    }

    private boolean visible(BlockPos p) {
        var hit = mob.level().clip(new ClipContext(mob.getEyePosition(), Vec3.atCenterOf(p), ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, mob));
        return hit.getType() != HitResult.Type.MISS && hit.getBlockPos().equals(p);
    }
}
