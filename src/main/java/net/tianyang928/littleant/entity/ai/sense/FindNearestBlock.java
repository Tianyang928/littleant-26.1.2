package net.tianyang928.littleant.entity.ai.sense;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.block.Block;

public class FindNearestBlock {
    private final PathfinderMob mob;
    private Block block;
    public BlockPos resultBlockPos;

    public FindNearestBlock(PathfinderMob mob, Block block) {
        this.mob = mob;
        this.block = block;
    }

    public boolean canUse() {
        return block != null;
    }

    public BlockPos setTarget(Block block) {
        this.block = block;
        if (block == null) return resultBlockPos = null;
        var r = new FindBlockList(mob).setTarget(block, 1);
        return resultBlockPos = r.isEmpty() ? null : r.getFirst();
    }

    public void clearTarget() {
        resultBlockPos = null;
    }

    public void start() {
        setTarget(block);
    }
}
