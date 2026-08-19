package net.tianyang928.littleant.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.tianyang928.littleant.gui.PheromoneListMenu;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Optional;

public class PheromoneBlockEntity extends BlockEntity implements MenuProvider, ContainerData {
    private final LinkedHashMap<Integer, Integer> pheromoneMap = new LinkedHashMap<>();
    private int tickCount = 1;

    // shared data with client
    private int tmpId = -1;
    private int tmpAmount = -1;

    public PheromoneBlockEntity(BlockPos worldPosition, BlockState blockState) {
        super(ModBlockEntities.PHEROMONE_BLOCK_ENTITY.get(), worldPosition, blockState);
    }

    // Read values from the passed ValueInput here.
    @Override
    public void loadAdditional(@NotNull ValueInput input) {
        super.loadAdditional(input);
        // Will default to 0 if absent. See the ValueIO article for more information.
        Optional<int[]> pheromoneId;
        Optional<int[]> pheromoneAmount;
        pheromoneId = input.getIntArray("pheromone_id");
        pheromoneAmount = input.getIntArray("pheromone_amount");

        // Map pheromone id to amount
        if(pheromoneId.isPresent() && pheromoneAmount.isPresent()) {
            for (int i = 0; i < pheromoneId.get().length; i++) {
                this.pheromoneMap.put(pheromoneId.get()[i], pheromoneAmount.get()[i]);
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
                this.pheromoneMap.entrySet().removeIf(entry -> entry.getKey() == key);
            }
        }
        // return Map to id and amount
        Optional<int[]> pheromoneId;
        Optional<int[]> pheromoneAmount;
        pheromoneId = Optional.of(new int[this.pheromoneMap.size()]);
        pheromoneAmount = Optional.of(new int[this.pheromoneMap.size()]);

        int index = 0;
        for (int key : this.pheromoneMap.keySet()) {
            pheromoneId.get()[index] = key;
            pheromoneAmount.get()[index] = this.pheromoneMap.get(key);
            index++;
        }

        output.putIntArray("pheromone_id", pheromoneId.get());
        output.putIntArray("pheromone_amount", pheromoneAmount.get());
    }

    public static void tick(Level level, BlockPos blockPos, BlockState blockState, PheromoneBlockEntity blockEntity) {
        // decrease the amount of every pheromone every 20 minutes
        if(blockEntity.tickCount % (20*1200) != 0) {
            blockEntity.tickCount++;
            return;
        }
        blockEntity.tickCount = 1;
        for(int key : blockEntity.pheromoneMap.keySet()) {
            if(blockEntity.pheromoneMap.get(key) <= 0) {
                continue;
            }
            blockEntity.pheromoneMap.put(key, blockEntity.pheromoneMap.get(key) - 1);
        }
    }

    public LinkedHashMap<Integer, Integer> getPheromoneList() {
        return this.pheromoneMap;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable(
                "block.littleant.pheromone_block"
        );
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int i, Inventory inventory, Player player) {
        tmpId = -1;
        tmpAmount = -1;
        return new PheromoneListMenu(i, inventory, this);
    }

    @Override
    public void writeClientSideData(AbstractContainerMenu menu, RegistryFriendlyByteBuf buf) {
        // write pheromone map to buffer
        for(int key : this.pheromoneMap.keySet()) {
            buf.writeInt(key);
            buf.writeInt(this.pheromoneMap.get(key));
        }
    }

    @Override
    public int get(int index) {
        return switch (index) {
            case 0 -> tmpId;
            case 1 -> tmpAmount;
            default -> 0;
        };
    }

    @Override
    public void set(int index, int value) {
        switch (index) {
            case 0 -> tmpId = value;
            case 1 -> tmpAmount = value;
        }
        setChanged();
    }

    @Override
    public int getCount() {
        return 2;
    }
}
