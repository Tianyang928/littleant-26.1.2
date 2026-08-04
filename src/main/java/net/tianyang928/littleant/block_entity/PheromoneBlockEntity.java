package net.tianyang928.littleant.block_entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.tianyang928.littleant.block.PheromoneBlock;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Optional;

public class PheromoneBlockEntity extends BlockEntity{
    private final HashMap<Integer, Integer> pheromoneMap = new HashMap<>();
    private int tickCount = 0;

    public PheromoneBlockEntity(BlockPos worldPosition, BlockState blockState) {
        super(ModBlockEntities.PHEROMONE_BLOCK_ENTITY.get(), worldPosition, blockState);
    }

    // Read values from the passed ValueInput here.
    @Override
    public void loadAdditional(@NotNull ValueInput input) {
        super.loadAdditional(input);
        // Will default to 0 if absent. See the ValueIO article for more information.
        Optional<int[]> pheromoneType;
        Optional<int[]> pheromoneAmount;
        pheromoneType = input.getIntArray("pheromone_type");
        pheromoneAmount = input.getIntArray("pheromone_amount");

        // Map pheromone type to amount
        if(pheromoneType.isPresent() && pheromoneAmount.isPresent()) {
            for (int i = 0; i < pheromoneType.get().length; i++) {
                this.pheromoneMap.put(pheromoneType.get()[i], pheromoneAmount.get()[i]);
            }
        }
    }

    // Save values into the passed ValueOutput here.
    @Override
    public void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        // clear pheromone map keys that have amount <= 0
        if(this.pheromoneMap.isEmpty()) {
            return;
        }
        for(int key : this.pheromoneMap.keySet()) {
            if(this.pheromoneMap.get(key) <= 0) {
                this.pheromoneMap.remove(key);
            }
        }
        // return Map to type and amount
        Optional<int[]> pheromoneType;
        Optional<int[]> pheromoneAmount;
        pheromoneType = Optional.of(new int[this.pheromoneMap.size()]);
        pheromoneAmount = Optional.of(new int[this.pheromoneMap.size()]);

        int index = 0;
        for (int key : this.pheromoneMap.keySet()) {
            pheromoneType.get()[index] = key;
            pheromoneAmount.get()[index] = this.pheromoneMap.get(key);
            index++;
        }

        output.putIntArray("pheromone_type", pheromoneType.get());
        output.putIntArray("pheromone_amount", pheromoneAmount.get());
    }

    public static void tick(Level level, BlockPos blockPos, BlockState blockState, PheromoneBlockEntity blockEntity) {
        // decrease the amount of every pheromone every 20 minutes
        if(blockEntity.tickCount % 20*1200 != 0) {
            blockEntity.tickCount++;
            return;
        }
        blockEntity.tickCount = 0;
        for(int key : blockEntity.pheromoneMap.keySet()) {
            if(blockEntity.pheromoneMap.get(key) <= 0) {
                continue;
            }
            blockEntity.pheromoneMap.put(key, blockEntity.pheromoneMap.get(key) - 1);
        }
    }
}
