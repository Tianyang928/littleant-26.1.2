package net.tianyang928.littleant.item;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.tianyang928.littleant.LittleAnt;
import net.tianyang928.littleant.block.ModBlocks;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(LittleAnt.MOD_ID);

    public static final DeferredItem<Item> ANT_SPAWN_EGG = ITEMS.registerSimpleItem("ant_spawn_egg");
    public static final DeferredItem<Item> PHEROMONE_BLOCK = ITEMS.registerItem("pheromone_block", properties -> new BlockItem(ModBlocks.PHEROMONE_BLOCK.get(), properties.useBlockDescriptionPrefix()));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
