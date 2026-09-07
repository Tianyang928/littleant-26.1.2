package net.tianyang928.littleant.entity.ai.sense;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class FindBlockList {
    private static final int SEARCH_RADIUS = 64;
    private static final int MAX_RESULTS = 256;
    private final PathfinderMob mob;

    public FindBlockList(PathfinderMob mob) { this.mob = mob; }

    public List<BlockPos> setTarget(List<Block> blocks, int requestedCount) {
        if (blocks == null || blocks.isEmpty() || requestedCount <= 0) return List.of();
        int limit = Math.min(requestedCount, MAX_RESULTS);
        BlockPos center = mob.blockPosition();
        List<BlockPos> results = new ArrayList<>();

        // Scan complete shells, so all directions advance outward together.
        for (int radius = 0; radius <= SEARCH_RADIUS; radius++) {
            for (int x = -radius; x <= radius; x++) {
                for (int y = -radius; y <= radius; y++) {
                    for (int z = -radius; z <= radius; z++) {
                        if (Math.max(Math.max(Math.abs(x), Math.abs(y)), Math.abs(z)) != radius) continue;
                        if (x * x + y * y + z * z > SEARCH_RADIUS * SEARCH_RADIUS) continue;
                        BlockPos candidate = center.offset(x, y, z);
                        if (blocks.contains(mob.level().getBlockState(candidate).getBlock()) && isVisible(candidate)) {
                            results.add(candidate.immutable());
                            if(results.size() >= limit) {
                                results.sort(Comparator.comparingDouble(p -> p.distSqr(center)));
                                return List.copyOf(results.subList(0, Math.min(limit, results.size())));
                            }
                        }
                    }
                }
            }
        }
        results.sort(Comparator.comparingDouble(p -> p.distSqr(center)));
        return List.copyOf(results.subList(0, Math.min(limit, results.size())));
    }

    private boolean isVisible(BlockPos pos) {
        var hit = mob.level().clip(new ClipContext(mob.getEyePosition(), Vec3.atCenterOf(pos),
                ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, mob));
        return hit.getType() != HitResult.Type.MISS && hit.getBlockPos().equals(pos);
    }
}
