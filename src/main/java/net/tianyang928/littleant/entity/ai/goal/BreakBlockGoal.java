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
import net.tianyang928.littleant.entity.AntEntity;

import java.util.EnumSet;

public class BreakBlockGoal extends Goal {
    public BlockPos blockPos;
    protected final AntEntity ant;
    private boolean hasTarget;
    protected int breakTime;
    private Path path;
    protected int lastBreakProgress = -1;
    private long lastSwingHandCheck;


    public BreakBlockGoal(AntEntity ant, BlockPos blockPos) {
        this.ant = ant;
        this.blockPos = blockPos;
        this.hasTarget = true;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    public void setTarget(BlockPos blockPos) {
        this.blockPos = blockPos.immutable();
        this.hasTarget = true;
        this.lastSwingHandCheck = 0L;
        this.breakTime = 0;
        ant.tryGettingDownWater = false;
    }

    public void clearTarget() {
        this.hasTarget = false;
        //this.ant.getNavigation().stop();
        ant.tryGettingDownWater = false;
    }

    private long getBlockBreakTime() {
        BlockState blockState = ant.level().getBlockState(blockPos);
        if (blockState.isAir() || blockState.getDestroySpeed(ant.level(), blockPos) < 0.0F) {
            return -1;
        }
        float toolSpeed = 1;
        ItemStack item = this.ant.getMainHandItem();
        if (!item.isEmpty()) {
            // Minecraft 的破坏公式：破坏时间 = 方块硬度 / (工具速度 * 30) * 20 ticks
            toolSpeed = item.getDestroySpeed(blockState);
            if (toolSpeed > 1.0F) {
                var miningEfficiency = this.ant.getAttribute(Attributes.MINING_EFFICIENCY);
                if (miningEfficiency != null) {
                    toolSpeed += (float) miningEfficiency.getValue();
                }
            }
        }
        float blockHardness = blockState.getDestroySpeed(ant.level(), blockPos);
        //LittleAnt.LOGGER.info("[BreakBlockGoal] getBlockBreakTime, block hardness: {}, tool speed: {}, break time: {}", blockHardness, toolSpeed, Math.max(1, (int)Math.ceil(blockHardness / (toolSpeed * 30.0F) * 20.0F)));
        return Math.max(1, (int)Math.ceil(blockHardness / (toolSpeed) * 20.0F));
    }

    @Override
    public boolean canUse() {
        if (!this.hasTarget || this.getBlockBreakTime() < 0L || !isBlockBreakable()) {
            this.clearTarget();
            return false;
        }

        // 直接能挖就不寻路了
        if (isBlockReachable()) {
            return true;
        }
        this.path = this.ant.getNavigation().createPath(this.blockPos, 2,64);
        return this.path != null && !this.path.isDone();

    }

    @Override
    public void start() {
        super.start();
        this.breakTime = 0;
        this.lastBreakProgress = -1;
        if (!isBlockReachable()) {
            this.ant.getNavigation().moveTo(this.path, this.ant.speedModifier);
        }
    }

    @Override
    public boolean canContinueToUse() {
        return isBlockBreakable()
                && (isBlockReachable() || !this.ant.getNavigation().isDone())
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
        this.ant.getNavigation().stop();
        this.ant.level().destroyBlockProgress(this.ant.getId(), this.blockPos, -1);
    }

    @Override
    public void tick() {
        super.tick();
        // 看向方块
        this.ant.getLookControl().setLookAt(this.blockPos.getX() + 0.5D, this.blockPos.getY() + 0.5D, this.blockPos.getZ() + 0.5D);

        // 在水下溺水时，优先上浮
        if (this.ant.getAirSupply() < 100) {
            if(this.ant instanceof AntEntity antEntity) {
                antEntity.tryGettingDownWater = false;
            }
        }

        double horizontalDistance = this.ant.distanceToSqr(this.blockPos.getX() + 0.5D, this.ant.getY(), this.blockPos.getZ() + 0.5D);

        if (this.ant.isInWater() && horizontalDistance <= 2.0D * 2.0D && !isBlockReachable()) {
            if(this.ant.getAirSupply() == 300){
                if(this.ant instanceof AntEntity antEntity) {
                    antEntity.tryGettingDownWater = true;
                }
            }
            if(this.ant.getAirSupply() > 100){
                if(this.ant instanceof AntEntity antEntity) {
                    if(antEntity.tryGettingDownWater){
                        antEntity.getDownInWater();
                    }
                }
            }
        }

        if (!isBlockReachable()) {
            if (this.ant.getNavigation().isDone()) {
                this.path = this.ant.getNavigation().createPath(this.blockPos, 2,64);
                if (this.path != null) this.ant.getNavigation().moveTo(this.path, this.ant.speedModifier);
            }
            return;
        }

        long time = this.ant.level().getGameTime();
        if (time - this.lastSwingHandCheck >= 6L) {
            this.lastSwingHandCheck = time;
            SoundEvent breakSound = this.ant.level().getBlockState(blockPos).getSoundType().getHitSound();
            this.ant.level().playSound(null, blockPos, breakSound, SoundSource.BLOCKS, 1.0F, 1.0F);
            this.ant.swing(InteractionHand.MAIN_HAND, true);
            //LittleAnt.LOGGER.info("[BreakBlockGoal] tick, swing hand once");
        }

        this.breakTime++;
        int progress = (int) ((float) this.breakTime / this.getBlockBreakTime() * 10.0F);
        if (progress != this.lastBreakProgress) {
            this.ant.level().destroyBlockProgress(this.ant.getId(), this.blockPos, progress);
            this.lastBreakProgress = progress;
        }

        if (this.breakTime >= this.getBlockBreakTime()) {
            boolean canDestroy = hasCorrectToolForDrops();
            this.ant.level().destroyBlock(this.blockPos, canDestroy, this.ant, 512);
            clearTarget();
        }
    }

    // 够不着方块
    private boolean isBlockReachable() {
        return this.ant.distanceToSqr(this.blockPos.getX() + 0.5D, this.blockPos.getY() + 0.5D - 1.0D, this.blockPos.getZ() + 0.5D) <= 4.0D * 4.0D;
    }

    private boolean isBlockBreakable() {
        BlockState state = this.ant.level().getBlockState(this.blockPos);
        return !state.isAir()
                && !state.liquid();
    }

    private boolean hasCorrectToolForDrops() {
        BlockState state = ant.level().getBlockState(this.blockPos);
        return !state.requiresCorrectToolForDrops() || this.ant.getMainHandItem().isCorrectToolForDrops(state);
    }
}
