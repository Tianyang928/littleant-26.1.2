package net.tianyang928.littleant.item;

import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.tianyang928.littleant.LittleAnt;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(LittleAnt.MOD_ID);

    public static final DeferredItem<Item> ANT_SPAWN_EGG = ITEMS.registerSimpleItem("ant_spawn_egg");

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
