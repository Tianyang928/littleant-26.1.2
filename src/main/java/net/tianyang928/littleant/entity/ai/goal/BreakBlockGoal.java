package net.tianyang928.littleant.entity.ai.goal;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Path;
import net.tianyang928.littleant.LittleAnt;
import net.tianyang928.littleant.entity.AntEntity;

import java.util.EnumSet;

public class BreakBlockGoal extends Goal {
    public BlockPos blockPos;
    protected final PathfinderMob mob;
    private boolean hasTarget;
    protected int breakTime;
    private long lastCanUseCheck;
    private Path path;
    protected int lastBreakProgress = -1;
    private long lastSwingHandCheck;


    public BreakBlockGoal(PathfinderMob mob, BlockPos blockPos) {
        this.mob = mob;
        this.blockPos = blockPos;
        this.hasTarget = true;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    public void setTarget(BlockPos blockPos) {
        this.blockPos = blockPos.immutable();
        this.hasTarget = true;
        this.lastCanUseCheck = 0L;
        this.lastSwingHandCheck = 0L;
        this.breakTime = 0;
        if(this.mob instanceof AntEntity antEntity) {
            antEntity.tryGettingDownWater = false;
        }
    }

    public void clearTarget() {
        this.hasTarget = false;
        this.mob.getNavigation().stop();
        if(this.mob instanceof AntEntity antEntity) {
            antEntity.tryGettingDownWater = false;
        }
    }

    private long getBlockBreakTime() {
        BlockState blockState = mob.level().getBlockState(blockPos);
        if (blockState.isAir() || blockState.getDestroySpeed(mob.level(), blockPos) < 0.0F) {
            return -1;
        }
        float toolSpeed = 1;
        ItemStack item = this.mob.getMainHandItem();
        if (!item.isEmpty()) {
            // Minecraft 的破坏公式：破坏时间 = 方块硬度 / (工具速度 * 30) * 20 ticks
            toolSpeed = item.getDestroySpeed(blockState);
            if (toolSpeed > 1.0F) {
                var miningEfficiency = this.mob.getAttribute(Attributes.MINING_EFFICIENCY);
                if (miningEfficiency != null) {
                    toolSpeed += (float) miningEfficiency.getValue();
                }
            }
        }
        float blockHardness = blockState.getDestroySpeed(mob.level(), blockPos);
        //LittleAnt.LOGGER.info("[BreakBlockGoal] getBlockBreakTime, block hardness: {}, tool speed: {}, break time: {}", blockHardness, toolSpeed, Math.max(1, (int)Math.ceil(blockHardness / (toolSpeed * 30.0F) * 20.0F)));
        return Math.max(1, (int)Math.ceil(blockHardness / (toolSpeed) * 20.0F));
    }

    @Override
    public boolean canUse() {
        if (!this.hasTarget || this.getBlockBreakTime() < 0L) {
            this.clearTarget();
            return false;
        }
        long time = this.mob.level().getGameTime();
        if (time - this.lastCanUseCheck < 20L) {
            return false;
        } else {
            this.lastCanUseCheck = time;

            // 直接能挖就不寻路了
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
        this.breakTime = 0;
        this.lastBreakProgress = -1;
        if (!isBlockReachable()) {
            this.mob.getNavigation().moveTo(this.path, 1.0D);
        }
    }

    @Override
    public boolean canContinueToUse() {
        return !this.mob.level().getBlockState(this.blockPos).isAir()
                && (isBlockReachable() || !this.mob.getNavigation().isDone())
                && this.breakTime <= getBlockBreakTime()
                && this.hasTarget;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        // breakTime uses Minecraft game ticks, not the goal selector's reduced tick rate.
        return false;
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

        long time = this.mob.level().getGameTime();
        if (time - this.lastSwingHandCheck >= 6L) {
            this.lastSwingHandCheck = time;
            SoundEvent breakSound = this.mob.level().getBlockState(blockPos).getSoundType().getHitSound();
            this.mob.level().playSound(null, blockPos, breakSound, SoundSource.BLOCKS, 1.0F, 1.0F);
            this.mob.swing(InteractionHand.MAIN_HAND, true);
            //LittleAnt.LOGGER.info("[BreakBlockGoal] tick, swing hand once");
        }

        this.breakTime++;
        int progress = (int) ((float) this.breakTime / this.getBlockBreakTime() * 10.0F);
        if (progress != this.lastBreakProgress) {
            this.mob.level().destroyBlockProgress(this.mob.getId(), this.blockPos, progress);
            this.lastBreakProgress = progress;
        }

        if (this.breakTime >= this.getBlockBreakTime()) {
            boolean canDestroy = hasCorrectToolForDrops();
            this.mob.level().destroyBlock(this.blockPos, canDestroy, this.mob, 512);
            clearTarget();
        }
    }

    // 够不着方块
    private boolean isBlockReachable() {
        return this.mob.distanceToSqr(this.blockPos.getX() + 0.5D, this.blockPos.getY() + 0.5D - 1.0D, this.blockPos.getZ() + 0.5D) <= 4.0D * 4.0D;
    }

    private boolean hasCorrectToolForDrops() {
        BlockState state = mob.level().getBlockState(this.blockPos);
        return !state.requiresCorrectToolForDrops() || this.mob.getMainHandItem().isCorrectToolForDrops(state);
    }
}
