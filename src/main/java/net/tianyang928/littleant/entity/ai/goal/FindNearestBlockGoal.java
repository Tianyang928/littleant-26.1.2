package net.tianyang928.littleant.entity.ai.goal;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.tianyang928.littleant.LittleAnt;
import net.tianyang928.littleant.block.ModBlocks;

import java.util.EnumSet;

public class FindNearestBlockGoal extends Goal {
    private int SEARCH_RADIUS = 64;
    private boolean hasTarget;
    private boolean isFinding;
    public BlockPos resultBlockPos;
    private final PathfinderMob mob;
    private Block blockToFind;
    private float lastStartDegree;

    public FindNearestBlockGoal(PathfinderMob mob, Block blockToFind) {
        this.mob = mob;
        this.blockToFind = blockToFind;
        this.hasTarget = true;
        this.resultBlockPos = null;
        this.isFinding = true;
        this.lastStartDegree = 0;
        this.setFlags(EnumSet.of(Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        return this.hasTarget && this.isFinding && this.blockToFind != null;
    }

    public void setTarget(Block blockToFind) {
        this.blockToFind = blockToFind;
        this.resultBlockPos = null;
        this.hasTarget = true;
        this.isFinding = true;
        this.lastStartDegree = 0;
    }

    public void clearTarget() {
        this.resultBlockPos = null;
        this.hasTarget = false;
        this.isFinding = false;
    }

    private BlockPos findBlock(
            double startAngle,
            double endAngle,
            int minY,
            int maxY
    ) {
        Vec3 origin = this.mob.getEyePosition();
        BlockPos.MutableBlockPos result = new BlockPos.MutableBlockPos();

        int centerX = Mth.floor(origin.x);
        int centerZ = Mth.floor(origin.z);

        for (int radius = 0; radius <= SEARCH_RADIUS; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) {
                        continue;
                    }

                    if (dx * dx + dz * dz > SEARCH_RADIUS * SEARCH_RADIUS) {
                        continue;
                    }

                    double angle = Math.toDegrees(Math.atan2(dz, dx));

                    if (!isAngleInSector(angle, startAngle, endAngle)) {
                        continue;
                    }

                    for (int y = minY; y <= maxY; y++) {
                        result.set(centerX + dx, y, centerZ + dz);

                        BlockState state = mob.level().getBlockState(result);

                        if (!state.is(blockToFind)) {
                            continue;
                        }

                        Vec3 targetCenter = Vec3.atCenterOf(result);
                        //LittleAnt.LOGGER.info("[FindNearestBlockGoal] 未进行射线检测，找到 {} 在 {}", blockToFind.getName(), targetCenter.toString());

                        if (hasLineOfSight(origin, targetCenter, result)) {
                            return result.immutable();
                        }
                    }
                }
            }
        }

        return null;
    }

    private boolean isAngleInSector(
            double angle,
            double start,
            double end
    ) {
        angle = Mth.wrapDegrees((float) angle);
        start = Mth.wrapDegrees((float) start);
        end = Mth.wrapDegrees((float) end);

        if (start <= end) {
            return angle >= start && angle <= end;
        }

        // 例如起点 170°，终点 -170°
        return angle >= start || angle <= end;
    }

    private boolean hasLineOfSight(
            Vec3 start,
            Vec3 end,
            BlockPos target
    ) {
        Vec3 direction = end.subtract(start);
        double length = direction.length();

        if (length <= 0.001) {
            return true;
        }

        Vec3 step = direction.normalize();

        BlockPos.MutableBlockPos pos =
                new BlockPos.MutableBlockPos(
                        Mth.floor(start.x),
                        Mth.floor(start.y),
                        Mth.floor(start.z)
                );

        int maxSteps = Mth.ceil(length * 4);

        for (int i = 0; i < maxSteps; i++) {
            if (pos.equals(target)) {
                return true;
            }

            BlockState state = this.mob.level().getBlockState(pos);

            if (isOpaqueObstacle(state, pos)) {
                return false;
            }

            // 沿射线前进一个很小的步长
            start = start.add(step.scale(0.25));

            pos.set(
                    Mth.floor(start.x),
                    Mth.floor(start.y),
                    Mth.floor(start.z)
            );
        }

        return false;
    }

    private boolean isOpaqueObstacle(
            BlockState state,
            BlockPos pos
    ) {
        return !state.isAir()
                && !state.is(ModBlocks.PHEROMONE_BLOCK)
                && !state.canBeReplaced();
    }

    @Override
    public void tick() {
        if(this.mob.level().getRandom().nextInt(20) == 0) {
            double rnd = (Math.PI * 2D) * this.mob.getRandom().nextDouble();
            double relX = Math.cos(rnd);
            double relZ = Math.sin(rnd);
            this.mob.getLookControl().setLookAt(this.mob.getX() + relX, this.mob.getEyeY(), this.mob.getZ() + relZ);
        }

        BlockPos tempBlockPos = findBlock(
                this.lastStartDegree,
                this.lastStartDegree + 15,
                this.mob.getBlockY() - 16,
                this.mob.getBlockY() + 32
        );
        this.lastStartDegree += 15;
        if(resultBlockPos == null) {
            this.resultBlockPos = tempBlockPos;
            LittleAnt.LOGGER.info("[FindNearestBlockGoal] resultBlockPos为空");
        }
        if(tempBlockPos != null && resultBlockPos != null) {
            if(this.mob.distanceToSqr(Vec3.atLowerCornerOf(tempBlockPos)) < this.mob.distanceToSqr(Vec3.atLowerCornerOf(resultBlockPos))) {
                this.resultBlockPos = tempBlockPos;
                LittleAnt.LOGGER.info("[FindNearestBlockGoal] 找到更近的方块: {}", tempBlockPos);
            }
        }
        LittleAnt.LOGGER.info("[FindNearestBlockGoal] lastStartDegree: {}", this.lastStartDegree);
        if(this.lastStartDegree >= 360 || (resultBlockPos != null && this.mob.distanceToSqr(Vec3.atLowerCornerOf(resultBlockPos)) < 1024.0D)) {

            if(resultBlockPos == null){
                LittleAnt.LOGGER.info("[FindNearestBlockGoal] 没有找到 {}", blockToFind.getName());
            }
            else{
                LittleAnt.LOGGER.info("[FindNearestBlockGoal] 最近的 {} 在 {}", blockToFind.getName(), resultBlockPos.toString());
            }
            clearTarget();
        }
    }
}
