package net.tianyang928.littleant.entity.ai.goal;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.pathfinder.Path;
import net.tianyang928.littleant.LittleAnt;
import net.tianyang928.littleant.entity.AntEntity;

import java.util.EnumSet;

public class SetBlockGoal extends Goal {
    public BlockPos blockPos;
    public BlockState holdingBlockState;
    protected final AntEntity ant;
    private boolean hasTarget;
    private long lastCanUseCheck;
    private Path path;


    public SetBlockGoal(AntEntity ant, BlockPos blockPos) {
        this.ant = ant;
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
        //this.ant.getNavigation().stop();
        if(this.ant instanceof AntEntity antEntity) {
            antEntity.tryGettingDownWater = false;
        }
    }

    @Override
    public boolean canUse() {
        if (!this.hasTarget
                || this.holdingBlockState == null
                || !canBlockPlaced()) {
            this.clearTarget();
            return false;
        }
        long time = this.ant.level().getGameTime();
        if (time - this.lastCanUseCheck < 20L) {
            return false;
        }
        else {
            this.lastCanUseCheck = time;

            // 直接能放置就不寻路了
            if (isBlockReachable()) {
                return true;
            }
            this.path = this.ant.getNavigation().createPath(this.blockPos, 2,64);
            return this.path != null && !this.path.isDone();
        }
    }

    @Override
    public void start() {
        super.start();
        if (!isBlockReachable()) {
            this.ant.getNavigation().moveTo(this.path, this.ant.speedModifier);
        }
    }

    @Override
    public boolean canContinueToUse() {
        this.updateHoldingBlockState();
        return (canBlockPlaced())
                && (isBlockReachable() || !this.ant.getNavigation().isDone())
                && this.hasTarget;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        // breakTime uses Minecraft game ticks, not the goal selector's reduced tick rate.
        // should be false, or blocks will be set twice
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
        if(this.ant.level().getRandom().nextInt(60) == 0){
            LittleAnt.LOGGER.info("[BreakBlockGoal] tick, horizontal distance: {}, air supply: {}", horizontalDistance, this.ant.getAirSupply());
        }
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


        // 调用 setPlacedBy 处理双方块物品（如门、床）的另一半放置
        Block placedBlock = holdingBlockState.getBlock();
        ItemStack itemStack = this.ant.getMainHandItem();
        this.ant.level().setBlock(this.blockPos, holdingBlockState, 3);
        placedBlock.setPlacedBy(this.ant.level(), this.blockPos, holdingBlockState, this.ant, itemStack);

//        LittleAnt.LOGGER.info("[SetBlockGoal] tick, has propertyHorizontalDirectionalBlock: {}", holdingBlockState.hasProperty(HorizontalDirectionalBlock.FACING));
//        LittleAnt.LOGGER.info("[SetBlockGoal] tick, place block, direction: {}", holdingBlockState.getValue(HorizontalDirectionalBlock.FACING));
//        LittleAnt.LOGGER.info("[SetBlockGoal] tick, ant direction: {}", this.ant.getDirection());

        SoundEvent placeSound = this.ant.level().getBlockState(blockPos).getSoundType().getPlaceSound();
        this.ant.level().playSound(null, blockPos, placeSound, SoundSource.BLOCKS, 1.0F, 1.0F);
        this.ant.swing(InteractionHand.MAIN_HAND, true);
        LittleAnt.LOGGER.info("[SetBlockGoal] tick, shrink one item");
        itemStack.shrink(1);

        clearTarget();
        //LittleAnt.LOGGER.info("[SetBlockGoal] tick, swing hand once");
    }

    // 够不着方块
    private boolean isBlockReachable() {
        return this.ant.distanceToSqr(this.blockPos.getX() + 0.5D, this.blockPos.getY() + 0.5D - 1.0D, this.blockPos.getZ() + 0.5D) <= 4.0D * 4.0D;
    }

    private void updateHoldingBlockState() {
        if(this.ant.getMainHandItem().getItem() instanceof BlockItem blockItem) {
            this.holdingBlockState = blockItem.getBlock().defaultBlockState();
            if(holdingBlockState.hasProperty(BlockStateProperties.FACING)) {
                holdingBlockState = holdingBlockState.setValue(BlockStateProperties.FACING, this.ant.getDirection());
            }
            else if(holdingBlockState.hasProperty(HorizontalDirectionalBlock.FACING)) {
                holdingBlockState = holdingBlockState.setValue(HorizontalDirectionalBlock.FACING, this.ant.getDirection());
            }
        }
        else {
            this.holdingBlockState = null;
        }
    }

    private boolean canBlockPlaced() {
        if(this.holdingBlockState == null){
            return false;
        }
        if(!this.ant.level().getBlockState(this.blockPos).isAir()
                && !this.ant.level().getBlockState(this.blockPos).canBeReplaced()){
            return false;
        }
        Block placedBlock = holdingBlockState.getBlock();
        if(placedBlock instanceof BedBlock){
            BlockPos otherPos = blockPos.relative(holdingBlockState.getValue(BedBlock.FACING));
            if(this.ant.level().getBlockState(otherPos).isAir() || this.ant.level().getBlockState(otherPos).canBeReplaced()){
                return true;
            }
            return false;
        }
        if(placedBlock instanceof DoorBlock){
            BlockPos otherPos = blockPos.above();
            if(this.ant.level().getBlockState(otherPos).isAir() || this.ant.level().getBlockState(otherPos).canBeReplaced()){
                return true;
            }
            return false;
        }
        return true;
    }
}
