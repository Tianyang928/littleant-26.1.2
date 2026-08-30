package net.tianyang928.littleant.entity.ai.sense;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.tianyang928.littleant.block.ModBlocks;
import net.tianyang928.littleant.blockentity.PheromoneBlockEntity;

import java.util.List;
import java.util.Set;

public class GetSurroundingPheromoneType {
    private final int SEARCH_RADIUS = 64;
    private boolean hasTarget;
    public Set<String> resultPheromoneTypes = Set.of();
    private final PathfinderMob mob;
    private final Block blockEntityToFind = ModBlocks.PHEROMONE_BLOCK.get();

    public GetSurroundingPheromoneType(PathfinderMob mob) {
        this.mob = mob;
        this.hasTarget = true;
    }

    public boolean canUse() {
        return this.hasTarget;
    }

    public Set<String> setTarget() {
        this.resultPheromoneTypes.clear();
        this.hasTarget = true;

        if(canUse()){
            start();
        }
        return resultPheromoneTypes;
    }

    public void clearTarget() {
        this.hasTarget = false;
    }

    public void start() {
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
                            resultPheromoneTypes.addAll(pheromoneBlockEntity.getPheromoneList().keySet());
                        }
                    }
                }
            }
        }
        this.hasTarget = false;
        clearTarget();
    }
}
