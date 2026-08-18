package net.tianyang928.littleant.entity.ai.goal;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.behavior.TransportItemsBetweenContainers;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;
import net.tianyang928.littleant.LittleAnt;
import net.tianyang928.littleant.block.ModBlocks;

import java.util.EnumSet;
import java.util.List;

public class FindNearestBlockEntityGoal extends Goal {
    // 寻找方块实体和寻找方块一样不能透视

    private final int SEARCH_RADIUS = 64;
    private boolean hasTarget;
    public BlockPos resultBlockPos;
    private final PathfinderMob mob;
    private Block blockEntityToFind;

    public FindNearestBlockEntityGoal(PathfinderMob mob, Block blockEntityToFind){
        this.mob = mob;
        this.blockEntityToFind = blockEntityToFind;
        this.hasTarget = true;
        this.resultBlockPos = null;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        return this.hasTarget && this.blockEntityToFind != null;
    }

    public void setTarget(Block blockEntityToFind) {
        this.blockEntityToFind = blockEntityToFind;
        this.resultBlockPos = null;
        this.hasTarget = true;
    }

    public void clearTarget() {
        this.hasTarget = false;
    }

    @Override
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
                    if (potentialTarget.getBlockState().is(this.blockEntityToFind)
                            && (isTargetBlockEntityVisible(potentialTarget.getBlockPos())
                            // 仅当目标是pheromone_block时，才可透视
                                 || blockEntityToFind.defaultBlockState().is(ModBlocks.PHEROMONE_BLOCK.get()))) {
                        BlockPos potentialTargetBlockPos = potentialTarget.getBlockPos();
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
        this.hasTarget = false;
        if(result.getX() != Integer.MAX_VALUE){
            this.resultBlockPos = result.immutable();
            LittleAnt.LOGGER.info("[FindNearestBlockEntityGoal] 找到目标: {}", this.resultBlockPos);
            return;
        }
        this.resultBlockPos = null;
        LittleAnt.LOGGER.info("[FindNearestBlockEntityGoal] 未找到目标");
    }

    private boolean isTargetBlockEntityVisible(BlockPos blockPos) {
        int maxSteps = SEARCH_RADIUS * 4;
        BlockPos eyeBlockPos =
                new BlockPos(
                        Mth.floor(this.mob.getEyePosition().x),
                        Mth.floor(this.mob.getEyePosition().y),
                        Mth.floor(this.mob.getEyePosition().z)
                );
        Vec3 norm = new Vec3(blockPos).subtract(this.mob.getEyePosition()).normalize();
        Vec3 tempFloatPos = this.mob.getEyePosition();
        BlockPos.MutableBlockPos tempBlockPos =
                new BlockPos.MutableBlockPos(
                        eyeBlockPos.getX(),
                        eyeBlockPos.getY(),
                        eyeBlockPos.getZ()
                );
        for(int i = 0; i < maxSteps; i++){
            if(tempBlockPos.distSqr(eyeBlockPos) >= blockPos.distSqr(eyeBlockPos)){
                return true;
            }
            BlockState state = this.mob.level().getBlockState(tempBlockPos);

            boolean canOcclude = state.canOcclude();
            if (canOcclude) {
                return false;
            }
            // 沿射线前进一个很小的步长
            tempFloatPos = tempFloatPos.add(norm.scale(0.25));

            tempBlockPos.set(
                    Mth.floor(tempFloatPos.x),
                    Mth.floor(tempFloatPos.y),
                    Mth.floor(tempFloatPos.z)
            );
        }
        return false;
    }
}
