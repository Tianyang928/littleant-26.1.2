package net.tianyang928.littleant.block_entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.tianyang928.littleant.LittleAnt;
import net.tianyang928.littleant.block.ModBlocks;

import java.util.function.Supplier;

public class ModBlockEntities extends BlockEntity {
    public ModBlockEntities(BlockEntityType<?> type, BlockPos worldPosition, BlockState blockState) {
        super(type, worldPosition, blockState);
    }

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, LittleAnt.MOD_ID);

    public static final Supplier<BlockEntityType<PheromoneBlockEntity>> PHEROMONE_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register(
            "pheromone_block_entity",
            () -> new BlockEntityType<>(
                    PheromoneBlockEntity::new,
                    false,
                    ModBlocks.PHEROMONE_BLOCK.get()
            )
    );

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITY_TYPES.register(eventBus);
    }
}
