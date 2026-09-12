package net.tianyang928.littleant.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.SpawnEggItem;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.tianyang928.littleant.LittleAnt;
import net.tianyang928.littleant.entity.ModEntities;
import net.tianyang928.littleant.block.ModBlocks;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(LittleAnt.MOD_ID);

    public static final DeferredItem<SpawnEggItem> ANT_SPAWN_EGG =
            ITEMS.registerItem(
                    "ant_spawn_egg",
                    properties -> new SpawnEggItem(
                            ModEntities.ANT.get(),
                            0xA52A2A,
                            0xB5E61D,
                            properties
                    )
            );
    public static final DeferredItem<Item> PHEROMONE_BLOCK = ITEMS.registerItem(
            "pheromone_block", properties -> new BlockItem(ModBlocks.PHEROMONE_BLOCK.get(), properties));
    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
