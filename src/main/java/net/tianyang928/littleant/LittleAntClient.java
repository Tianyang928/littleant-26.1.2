package net.tianyang928.littleant;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.KeyMapping;
import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;
import net.tianyang928.littleant.client.overlay.debug.AntDebugClientState;
import net.tianyang928.littleant.client.overlay.debug.AntDebugOverlay;
import net.tianyang928.littleant.network.SyncAntTaskDebugPayload;
import net.tianyang928.littleant.entity.ModEntities;
import net.tianyang928.littleant.gui.ModMenus;
import net.tianyang928.littleant.gui.screen.PheromoneListScreen;
import net.tianyang928.littleant.client.renderer.AntRenderer;
import net.tianyang928.littleant.gui.screen.AntInventoryScreen;
import net.tianyang928.littleant.gui.screen.AntBrainProgramScreen;
import net.tianyang928.littleant.network.SyncPheromonePayload;

// This class will not load on dedicated servers. Accessing client side code from here is safe.
@Mod(value = LittleAnt.MOD_ID, dist = Dist.CLIENT)
// You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
@EventBusSubscriber(modid = LittleAnt.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public class LittleAntClient {
    public LittleAntClient(ModContainer container) {
        // Allows NeoForge to create a config screen for this mod's configs.
        // The config screen is accessed by going to the Mods screen > clicking on your mod > clicking on config.
        // Do not forget to add translations for your config options to the en_us.json file.
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        // Some client setup code
        LittleAnt.LOGGER.info("HELLO FROM CLIENT SETUP");
        LittleAnt.LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());
    }

    @SubscribeEvent
    static void registerScreens(
            RegisterMenuScreensEvent event
    ) {
        event.register(
                ModMenus.PHEROMONE_LIST_MENU.get(),
                PheromoneListScreen::new
        );
        event.register(
                ModMenus.ANT_INVENTORY_MENU.get(),
                AntInventoryScreen::new
        );
        event.register(
                ModMenus.ANT_BRAIN_PROGRAM_MENU.get(),
                AntBrainProgramScreen::new
        );
    }

    //public static final KeyMapping.Category KEY_CATEGORY = new KeyMapping.Category(ResourceLocation.fromNamespaceAndPath(LittleAnt.MOD_ID, "main"));
    public static final KeyMapping DEBUG_TOGGLE = new KeyMapping("key.littleant.toggle_debug", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_F8, ResourceLocation.fromNamespaceAndPath(LittleAnt.MOD_ID, "main").toString());
    @SubscribeEvent static void registerKeys(RegisterKeyMappingsEvent event) { event.register(DEBUG_TOGGLE); }
    @SubscribeEvent static void registerGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.CHAT, ResourceLocation.fromNamespaceAndPath(LittleAnt.MOD_ID, "ant_task_debug"), AntDebugOverlay::render);
    }



    @SubscribeEvent
    static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.ANT.get(), AntRenderer::new);
    }

    @EventBusSubscriber(modid = LittleAnt.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.GAME)
    private static final class GameEvents {
        @SubscribeEvent
        static void clientTick(ClientTickEvent.Post event) {
            while (DEBUG_TOGGLE.consumeClick()) AntDebugClientState.toggle();
            if (Minecraft.getInstance().level == null) AntDebugClientState.clear();
        }
    }
}
