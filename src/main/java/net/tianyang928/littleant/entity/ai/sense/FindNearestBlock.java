package net.tianyang928.littleant.entity.ai.sense;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.block.Block;
import java.util.List;

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

    public BlockPos setTarget(List<Block> blocks) {
        this.block = blocks == null || blocks.isEmpty() ? null : blocks.getFirst();
        if (blocks == null || blocks.isEmpty()) return resultBlockPos = null;
        var r = new FindBlockList(mob).setTarget(blocks, 1);
        return resultBlockPos = r.isEmpty() ? null : r.getFirst();
    }

    public void clearTarget() {
        resultBlockPos = null;
    }

    public void start() {
        setTarget(block == null ? List.of() : List.of(block));
    }
}
