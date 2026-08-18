package net.tianyang928.littleant.gui;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.tianyang928.littleant.LittleAnt;

public class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(
                    Registries.MENU,
                    LittleAnt.MOD_ID
            );

    public static final DeferredHolder<MenuType<?>, MenuType<PheromoneListMenu>> PHEROMONE_LIST_MENU =
            MENUS.register(
                    "pheromone_list",
                    () -> IMenuTypeExtension.create(
                            PheromoneListMenu::new
                    )
            );

    public static final DeferredHolder<MenuType<?>, MenuType<AntInventoryMenu>> ANT_INVENTORY_MENU =
            MENUS.register(
                    "ant_inventory",
                    () -> IMenuTypeExtension.create(
                            AntInventoryMenu::new
                    )
            );

    public static void register(IEventBus eventBus) {
        MENUS.register(eventBus);
    }
}
