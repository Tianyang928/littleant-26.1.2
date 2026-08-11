package net.tianyang928.littleant.entity.ai.goal;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Path;
import net.tianyang928.littleant.LittleAnt;
import net.tianyang928.littleant.entity.AntEntity;

import java.util.EnumSet;

public class SetBlockGoal extends Goal {
    public BlockPos blockPos;
    public BlockState holdingBlockState;
    protected final PathfinderMob mob;
    private boolean hasTarget;
    private long lastCanUseCheck;
    private Path path;


    public SetBlockGoal(PathfinderMob mob, BlockPos blockPos) {
        this.mob = mob;
        this.blockPos = blockPos;
        this.hasTarget = true;
        this.updateHoldingBlockState();

        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    public void setTarget(BlockPos blockPos) {
        this.blockPos = blockPos.immutable();
        this.hasTarget = true;
        this.lastCanUseCheck = 0L;
        this.updateHoldingBlockState();
    }

    public void clearTarget() {
        this.hasTarget = false;
        this.mob.getNavigation().stop();
        if(this.mob instanceof AntEntity antEntity) {
            antEntity.tryGettingDownWater = false;
        }
    }

    @Override
    public boolean canUse() {
        if (!this.hasTarget
                || this.holdingBlockState == null
                || (!this.mob.level().getBlockState(this.blockPos).isAir()
                && !this.mob.level().getBlockState(this.blockPos).canBeReplaced())) {
            this.clearTarget();
            return false;
        }
        long time = this.mob.level().getGameTime();
        if (time - this.lastCanUseCheck < 20L) {
            return false;
        }
        else {
            this.lastCanUseCheck = time;

            // 直接能放置就不寻路了
            if (isBlockReachable()) {
                return true;
            }
            this.path = this.mob.getNavigation().createPath(this.blockPos, 2);
            return this.path != null && !this.path.isDone();
        }
    }

    @Override
    public void start() {
        super.start();
        if (!isBlockReachable()) {
            this.mob.getNavigation().moveTo(this.path, 1.0D);
        }
    }

    @Override
    public boolean canContinueToUse() {
        this.updateHoldingBlockState();
        return (this.mob.level().getBlockState(this.blockPos).isAir()
                || this.mob.level().getBlockState(this.blockPos).canBeReplaced())
                && (isBlockReachable() || !this.mob.getNavigation().isDone())
                && this.hasTarget;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        // breakTime uses Minecraft game ticks, not the goal selector's reduced tick rate.
        return true;
    }

    @Override
    public void stop() {
        super.stop();
        this.mob.getNavigation().stop();
        this.mob.level().destroyBlockProgress(this.mob.getId(), this.blockPos, -1);
    }

    @Override
    public void tick() {
        super.tick();
        // 看向方块
        this.mob.getLookControl().setLookAt(this.blockPos.getX() + 0.5D, this.blockPos.getY() + 0.5D, this.blockPos.getZ() + 0.5D);

        // 在水下溺水时，优先上浮
        if (this.mob.getAirSupply() < 100) {
            if(this.mob instanceof AntEntity antEntity) {
                antEntity.tryGettingDownWater = false;
            }
        }

        double horizontalDistance = this.mob.distanceToSqr(this.blockPos.getX() + 0.5D, this.mob.getY(), this.blockPos.getZ() + 0.5D);
        if(this.mob.level().getRandom().nextInt(60) == 0){
            LittleAnt.LOGGER.info("[BreakBlockGoal] tick, horizontal distance: {}, air supply: {}", horizontalDistance, this.mob.getAirSupply());
        }
        if (this.mob.isInWater() && horizontalDistance <= 2.0D * 2.0D && !isBlockReachable()) {
            if(this.mob.getAirSupply() == 300){
                if(this.mob instanceof AntEntity antEntity) {
                    antEntity.tryGettingDownWater = true;
                }
            }
            if(this.mob.getAirSupply() > 100){
                if(this.mob instanceof AntEntity antEntity) {
                    if(antEntity.tryGettingDownWater){
                        antEntity.getDownInWater();
                    }
                }
            }
        }

        if (!isBlockReachable()) {
            if (this.mob.getNavigation().isDone()) {
                this.path = this.mob.getNavigation().createPath(this.blockPos, 2);
                if (this.path != null) this.mob.getNavigation().moveTo(this.path, 1.0D);
            }
            return;
        }

        this.mob.level().setBlock(this.blockPos, holdingBlockState, 3);
        SoundEvent placeSound = this.mob.level().getBlockState(blockPos).getSoundType().getPlaceSound();
        this.mob.level().playSound(null, blockPos, placeSound, SoundSource.BLOCKS, 1.0F, 1.0F);
        this.mob.swing(InteractionHand.MAIN_HAND, true);
        this.mob.getMainHandItem().shrink(1);
        clearTarget();
        //LittleAnt.LOGGER.info("[SetBlockGoal] tick, swing hand once");
    }

    private BlockPos getNearestStandableBlockPos() {
        return this.blockPos;
    }

    // 够不着方块
    private boolean isBlockReachable() {
        return this.mob.distanceToSqr(this.blockPos.getX() + 0.5D, this.blockPos.getY() + 0.5D - 1.0D, this.blockPos.getZ() + 0.5D) <= 4.0D * 4.0D;
    }

    private void updateHoldingBlockState() {
        if(this.mob.getMainHandItem().getItem() instanceof BlockItem blockItem) {
            this.holdingBlockState = blockItem.getBlock().defaultBlockState();
        }
        else {
            this.holdingBlockState = null;
        }
    }
}
