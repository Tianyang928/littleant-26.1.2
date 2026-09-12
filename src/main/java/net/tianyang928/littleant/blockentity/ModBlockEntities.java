package net.tianyang928.littleant.blockentity;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.tianyang928.littleant.LittleAnt;
import net.tianyang928.littleant.block.ModBlocks;

import java.util.function.Supplier;

public final class ModBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, LittleAnt.MOD_ID);

    public static final Supplier<BlockEntityType<PheromoneBlockEntity>> PHEROMONE_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register(
            "pheromone_block_entity",
            () -> BlockEntityType.Builder.of(PheromoneBlockEntity::new, ModBlocks.PHEROMONE_BLOCK.get()).build(null)
    );

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITY_TYPES.register(eventBus);
    }
}
