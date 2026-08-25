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

import java.util.List;

public class FindBlockEntity {
    // 寻找方块实体和寻找方块一样不能透视

    private final int SEARCH_RADIUS = 64;
    private boolean hasTarget;
    public BlockPos resultBlockPos;
    private final PathfinderMob mob;
    private Block blockEntityToFind;

    public FindBlockEntity(PathfinderMob mob, Block blockEntityToFind) {
        this.mob = mob;
        this.blockEntityToFind = blockEntityToFind;
        this.hasTarget = true;
        this.resultBlockPos = null;
    }

    public boolean canUse() {
        return this.hasTarget && this.blockEntityToFind != null;
    }

    public BlockPos setTarget(Block blockEntityToFind) {
        this.blockEntityToFind = blockEntityToFind;
        this.resultBlockPos = null;
        this.hasTarget = true;

        if(canUse()){
            start();
        }
        return resultBlockPos;
    }

    public void clearTarget() {
        this.hasTarget = false;
    }

    public void start() {
        BlockPos.MutableBlockPos result = new BlockPos.MutableBlockPos(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE);
        List<ChunkPos> list = ChunkPos.rangeClosed(
                ChunkPos.containing(mob.blockPosition()),
                Math.floorDiv(SEARCH_RADIUS, 16) + 1
        ).toList();
        // 遍历区块
        for (ChunkPos chunkPos : list) {
            LevelChunk levelChunk = mob.level().getChunkSource().getChunkNow(chunkPos.x(), chunkPos.z());
            if (levelChunk != null) {
                // 直接遍历区块中的 BlockEntity，而不是遍历所有方块
                for (BlockEntity potentialTarget : levelChunk.getBlockEntities().values()) {
                    BlockPos potentialTargetBlockPos = potentialTarget.getBlockPos();
                    // 获取目标方块的碰撞箱
                    if (potentialTarget.getBlockState().is(this.blockEntityToFind)) {
                        Vec3 from = new Vec3(mob.getX(), mob.getEyeY(), mob.getZ());
                        Vec3 to = Vec3.atCenterOf(potentialTargetBlockPos);
                        ClipContext context = new ClipContext(
                                from,
                                to,
                                ClipContext.Block.VISUAL,
                                ClipContext.Fluid.NONE,
                                mob
                        );
                        BlockHitResult blockHitResult = mob.level().clip(context);
                        if ((blockHitResult.getType() != HitResult.Type.MISS
                                && blockHitResult.getBlockPos().equals(potentialTargetBlockPos))
                                || blockEntityToFind.defaultBlockState().is(ModBlocks.PHEROMONE_BLOCK.get())) {
                            //能看到方块
                            if (mob.distanceToSqr(
                                    result.getX(),
                                    result.getY(),
                                    result.getZ())
                                    > mob.distanceToSqr(
                                    potentialTargetBlockPos.getX(),
                                    potentialTargetBlockPos.getY(),
                                    potentialTargetBlockPos.getZ())) {
                                result.set(potentialTargetBlockPos);
                            }
                        }
                    }
                }
            }
        }
        this.hasTarget = false;
        if (result.getX() != Integer.MAX_VALUE) {
            this.resultBlockPos = result.immutable();
            LittleAnt.LOGGER.info("[FindBlockEntity] 找到目标: {}", this.resultBlockPos);
            return;
        }
        this.resultBlockPos = null;
        LittleAnt.LOGGER.info("[FindBlockEntity] 未找到目标");
        clearTarget();
    }
}
