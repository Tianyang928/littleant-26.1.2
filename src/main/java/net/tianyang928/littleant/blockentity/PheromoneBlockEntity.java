package net.tianyang928.littleant.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
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

public class PheromoneBlockEntity extends BlockEntity implements MenuProvider {
    private final LinkedHashMap<String, Integer> pheromoneMap = new LinkedHashMap<>();
    private int tickCount = 1;

    public PheromoneBlockEntity(BlockPos worldPosition, BlockState blockState) {
        super(ModBlockEntities.PHEROMONE_BLOCK_ENTITY.get(), worldPosition, blockState);
    }

    // Read values from the passed ValueInput here.
    @Override
    public void loadAdditional(@NotNull ValueInput input) {
        super.loadAdditional(input);
        // Will default to 0 if absent. See the ValueIO article for more information.
        String pheromoneId;
        Optional<int[]> pheromoneAmount;
        pheromoneId = input.getStringOr("pheromone_id", "");
        pheromoneAmount = input.getIntArray("pheromone_amount");

        // Map pheromone id to amount
        if(!pheromoneId.isEmpty() && pheromoneAmount.isPresent()) {
            String[] pheromoneIdArray = pheromoneId.split(",");
            for (int i = 0; i < pheromoneIdArray.length && i < pheromoneAmount.get().length; i++) {
                this.pheromoneMap.put(pheromoneIdArray[i], pheromoneAmount.get()[i]);
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
        this.pheromoneMap.entrySet().removeIf(entry -> entry.getValue() <= 0);
        // return Map to id and amount
        StringBuilder pheromoneId = new StringBuilder();
        Optional<int[]> pheromoneAmount;
        for(String key : this.pheromoneMap.keySet()) {
            pheromoneId.append(key).append(",");
        }
        pheromoneAmount = Optional.of(new int[this.pheromoneMap.size()]);

        int index = 0;
        for (String key : this.pheromoneMap.keySet()) {
            pheromoneAmount.get()[index] = this.pheromoneMap.get(key);
            index++;
        }

        output.putString("pheromone_id", pheromoneId.toString());
        pheromoneId.delete(0, pheromoneId.length());
        output.putIntArray("pheromone_amount", pheromoneAmount.get());
    }

    public static void tick(Level level, BlockPos blockPos, BlockState blockState, PheromoneBlockEntity blockEntity) {
        // decrease the amount of every pheromone every 20 minutes
        if(blockEntity.tickCount % (20*1200) != 0) {
            blockEntity.tickCount++;
            return;
        }
        blockEntity.tickCount = 1;
        for(String key : blockEntity.pheromoneMap.keySet()) {
            if(blockEntity.pheromoneMap.get(key) <= 0) {
                continue;
            }
            blockEntity.pheromoneMap.put(key, blockEntity.pheromoneMap.get(key) - 1);
        }
    }

    public LinkedHashMap<String, Integer> getPheromoneList() {
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
        return new PheromoneListMenu(i, inventory, this);
    }

    @Override
    public void writeClientSideData(AbstractContainerMenu menu, RegistryFriendlyByteBuf buf) {
        buf.writeVarInt(this.pheromoneMap.size());
        for (var entry : this.pheromoneMap.entrySet()) {
            buf.writeUtf(entry.getKey(), 64);
            buf.writeVarInt(entry.getValue());
        }
    }
}
