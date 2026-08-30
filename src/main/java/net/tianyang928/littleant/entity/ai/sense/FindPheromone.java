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
import net.tianyang928.littleant.blockentity.PheromoneBlockEntity;

import java.util.List;

public class FindPheromone {
    private final int SEARCH_RADIUS = 64;
    private boolean hasTarget;
    public BlockPos resultBlockPos;
    private final PathfinderMob mob;
    private final Block blockEntityToFind = ModBlocks.PHEROMONE_BLOCK.get();
    private String pheromoneTypeToFind;

    public FindPheromone(PathfinderMob mob, String pheromone) {
        this.mob = mob;
        this.pheromoneTypeToFind = pheromone;
        this.hasTarget = true;
        this.resultBlockPos = null;
    }

    public boolean canUse() {
        return this.hasTarget && this.pheromoneTypeToFind != null && !this.pheromoneTypeToFind.isEmpty();
    }

    public BlockPos setTarget(String pheromone) {
        this.pheromoneTypeToFind = pheromone;
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
                        if(mob.level().getBlockEntity(potentialTargetBlockPos) instanceof PheromoneBlockEntity pheromoneBlockEntity){
                            if(!pheromoneBlockEntity.getPheromoneList().containsKey(this.pheromoneTypeToFind)){
                                continue;
                            }
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
