package net.tianyang928.littleant.entity.ai.goal;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.tianyang928.littleant.LittleAnt;

import java.util.EnumSet;
import java.util.HashSet;

public class FindNearestBlockGoal extends Goal {
    private final int SEARCH_RADIUS = 64;
    private boolean hasTarget;
    private boolean isFinding;
    public BlockPos resultBlockPos;
    private final PathfinderMob mob;
    private Block blockToFind;
    private int hashrateOccupied = 0;

    private final char[][][] isBlockOpaque = new char[2*SEARCH_RADIUS+1][2*SEARCH_RADIUS+1][2*SEARCH_RADIUS+1];
    private static final HashSet<Vec3> sphericalShellVectorDict = new HashSet<>();

    public FindNearestBlockGoal(PathfinderMob mob, Block blockToFind) {
        this.mob = mob;
        this.blockToFind = blockToFind;
        this.hasTarget = true;
        this.resultBlockPos = null;
        this.isFinding = true;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));

        // collect all possible vectors on the sphere
        initSphericalShellVectorDict();
        resetIsBlockOpaque();
    }

    private void initSphericalShellVectorDict() {
        if(!sphericalShellVectorDict.isEmpty()) {
            return;
        }

        int hi = SEARCH_RADIUS*SEARCH_RADIUS;
        int lo = (SEARCH_RADIUS-1)*(SEARCH_RADIUS-1);
        for(int x = 0; x <= SEARCH_RADIUS; x++) {
            int x2 = x * x;
            for(int y = 0; y <= SEARCH_RADIUS; y++) {
                int s = x2 + y * y;
                int z2_max=hi-s;
                if(z2_max<=0) {
                    // 补上x^2+y^2=SEARCH_RADIUS^2的情况
                    if((SEARCH_RADIUS+1)*(SEARCH_RADIUS+1)-s > 0) {
                        int z = 0;
                        Vec3 norm = new Vec3(x, y, z).normalize();
                        double nx = norm.x();
                        double ny = norm.y();
                        double nz = norm.z();
                        sphericalShellVectorDict.add(new Vec3(nx, ny, nz));
                        sphericalShellVectorDict.add(new Vec3(nx*-1, ny, nz));
                        sphericalShellVectorDict.add(new Vec3(nx, ny*-1, nz));
                        sphericalShellVectorDict.add(new Vec3(nx*-1, ny*-1, nz));
                    }
                    continue;
                }
                int z2_min=lo-s;
                int z_low = Mth.floor(Math.sqrt(z2_min))+1;
                int z_high = Mth.floor(Math.sqrt(z2_max));
                for(int z = z_low; z <= z_high; z++) {
                    // add all 8 vectors on the sphere
                    // normal
                    Vec3 norm = new Vec3(x, y, z).normalize();
                    double nx = norm.x();
                    double ny = norm.y();
                    double nz = norm.z();
                    sphericalShellVectorDict.add(new Vec3(nx, ny, nz));
                    sphericalShellVectorDict.add(new Vec3(nx*-1, ny, nz));
                    sphericalShellVectorDict.add(new Vec3(nx, ny*-1, nz));
                    sphericalShellVectorDict.add(new Vec3(nx, ny, nz*-1));
                    sphericalShellVectorDict.add(new Vec3(nx*-1, ny*-1, nz));
                    sphericalShellVectorDict.add(new Vec3(nx*-1, ny, nz*-1));
                    sphericalShellVectorDict.add(new Vec3(nx, ny*-1, nz*-1));
                    sphericalShellVectorDict.add(new Vec3(nx*-1, ny*-1, nz*-1));
                }
            }
        }
    }

    private void resetIsBlockOpaque() {
        for(int x = 0; x <= isBlockOpaque.length-1; x++) {
            for(int y = 0; y <= isBlockOpaque[x].length-1; y++) {
                for(int z = 0; z <= isBlockOpaque[x][y].length-1; z++) {
                    isBlockOpaque[x][y][z] = '0';
                }
            }
        }
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
        resetIsBlockOpaque();
    }

    public void clearTarget() {
        this.hasTarget = false;
        this.isFinding = false;
        resetIsBlockOpaque();
    }

    private BlockPos findBlock() {
        BlockPos eyeBlockPos =
                new BlockPos(
                        Mth.floor(this.mob.getEyePosition().x),
                        Mth.floor(this.mob.getEyePosition().y),
                        Mth.floor(this.mob.getEyePosition().z)
                );
        BlockPos.MutableBlockPos result = new BlockPos.MutableBlockPos(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE);
        int maxSteps = SEARCH_RADIUS * 4;
        for (Vec3 v : sphericalShellVectorDict) {
            Vec3 tempFloatPos = this.mob.getEyePosition();
            BlockPos.MutableBlockPos tempBlockPos =
                    new BlockPos.MutableBlockPos(
                            eyeBlockPos.getX(),
                            eyeBlockPos.getY(),
                            eyeBlockPos.getZ()
                    );
            for (int i = 0; i < maxSteps; i++) {
                // 如果这条视线已经遇到过不透明的方块，就直接跳出
                if(isBlockOpaque
                        [tempBlockPos.getX()-eyeBlockPos.getX()+SEARCH_RADIUS]
                        [tempBlockPos.getY()-eyeBlockPos.getY()+SEARCH_RADIUS]
                        [tempBlockPos.getZ()-eyeBlockPos.getZ()+SEARCH_RADIUS] == 'Y') {
                    break;
                }
                // 如果这条视线已经遇到过透明的方块，就继续前进
                else if(isBlockOpaque
                        [tempBlockPos.getX()-eyeBlockPos.getX()+SEARCH_RADIUS]
                        [tempBlockPos.getY()-eyeBlockPos.getY()+SEARCH_RADIUS]
                        [tempBlockPos.getZ()-eyeBlockPos.getZ()+SEARCH_RADIUS] == '0') {
                    // 只有遇到为‘0’时，说明遇到没有遍历过的方块，才需要判断是否是目标方块
                    BlockState state = this.mob.level().getBlockState(tempBlockPos);

                    boolean canOcclude = state.canOcclude();
                    if (canOcclude) {
                        isBlockOpaque
                            [tempBlockPos.getX() - eyeBlockPos.getX() + SEARCH_RADIUS]
                            [tempBlockPos.getY() - eyeBlockPos.getY() + SEARCH_RADIUS]
                            [tempBlockPos.getZ() - eyeBlockPos.getZ() + SEARCH_RADIUS] = 'Y';
                    } else {
                        isBlockOpaque
                                [tempBlockPos.getX() - eyeBlockPos.getX() + SEARCH_RADIUS]
                                [tempBlockPos.getY() - eyeBlockPos.getY() + SEARCH_RADIUS]
                                [tempBlockPos.getZ() - eyeBlockPos.getZ() + SEARCH_RADIUS] = 'N';
                    }
                    boolean isTarget = state.is(blockToFind);
                    if (isTarget) {
                        double distanceToEyeSqr = tempBlockPos.distSqr(eyeBlockPos);
                        if (result.distSqr(eyeBlockPos) > distanceToEyeSqr) {
                            result.set(tempBlockPos);
                            if(distanceToEyeSqr <= 32.0D * 32.0D) {
                                return result.immutable();
                            }
                        }
                    }

                    if(canOcclude || isTarget) {
                        break;
                    }
                }

                // 沿射线前进一个很小的步长
                tempFloatPos = tempFloatPos.add(v.scale(0.25));

                tempBlockPos.set(
                        Mth.floor(tempFloatPos.x),
                        Mth.floor(tempFloatPos.y),
                        Mth.floor(tempFloatPos.z)
                );

            }
        }
        // 如果没有找到目标方块，就返回null
        if(result.getX() == Integer.MAX_VALUE) {
            return null;
        }
        return result.immutable();
    }

    @Override
    public void tick() {

        // 随意看向一个方向
        if(this.mob.level().getRandom().nextInt(20) == 0) {
            double rnd = (Math.PI * 2D) * this.mob.getRandom().nextDouble();
            double relX = Math.cos(rnd);
            double relZ = Math.sin(rnd);
            this.mob.getLookControl().setLookAt(this.mob.getX() + relX, this.mob.getEyeY(), this.mob.getZ() + relZ);
        }
        // 如果当前tickCount和上一次相同，说明已经有一个其他实体进行过搜索，那么直接返回
        if(Integer.hashCode(this.mob.tickCount) == this.hashrateOccupied) {
            return;
        }
        this.hashrateOccupied = Integer.hashCode(this.mob.tickCount);

        // 每tick最多一个实体搜索方块
        resultBlockPos = findBlock();
        if(resultBlockPos == null){
            LittleAnt.LOGGER.info("[FindNearestBlockGoal] 没有找到 {}", blockToFind.getName());
        }
        else{
            LittleAnt.LOGGER.info("[FindNearestBlockGoal] 最近的 {} 在 {}", blockToFind.getName(), resultBlockPos.toString());
        }
        clearTarget();
    }
}
