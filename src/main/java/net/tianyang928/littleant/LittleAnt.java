package net.tianyang928.littleant;

import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.tianyang928.littleant.block.ModBlocks;
import net.tianyang928.littleant.blockentity.ModBlockEntities;
import net.tianyang928.littleant.creativemodetab.ModCreativeModeTabs;
import net.tianyang928.littleant.entity.AntEntity;
import net.tianyang928.littleant.entity.ModEntities;
import net.tianyang928.littleant.gui.ModMenus;
import net.tianyang928.littleant.item.ModItems;
import net.tianyang928.littleant.network.*;
import net.tianyang928.littleant.server.command.*;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;

@Mod(LittleAnt.MOD_ID)
public class LittleAnt {
    public static final String MOD_ID = "littleant";
    // Directly reference a slf4j logger
    public static final Logger LOGGER = LogUtils.getLogger();

    public LittleAnt(IEventBus modEventBus, ModContainer modContainer) {
        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::registerPayloads);

        ModCreativeModeTabs.register(modEventBus);
        ModItems.register(modEventBus);
        ModBlocks.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        ModMenus.register(modEventBus);
        ModEntities.register(modEventBus);

        // Register ourselves for server and other game events we are interested in.
        // Note that this is necessary if and only if we want *this* class (LittleAnt) to respond directly to events.
        // Do not add this line if there are no @SubscribeEvent-annotated functions in this class, like onServerStarting() below.
        NeoForge.EVENT_BUS.register(this);

        // Register the item to a creative tab
        modEventBus.addListener(this::addCreative);
        modEventBus.addListener(this::registerAttributes);

        // Register our mod's ModConfigSpec so that FML can create and load the config file for us
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(FMLCommonSetupEvent event) {

    }

    /** Server-bound editing payloads must be registered on both integrated and dedicated servers. */
    private void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(SetPheromonePayload.TYPE, SetPheromonePayload.STREAM_CODEC, SetPheromonePayload::handlePacketFromClient);
        registrar.playToClient(SyncPheromonePayload.TYPE, SyncPheromonePayload.STREAM_CODEC);
        registrar.playToClient(SyncAntTaskDebugPayload.TYPE, SyncAntTaskDebugPayload.STREAM_CODEC);
        registrar.playToServer(UpdateAntBrainProgramPayload.TYPE, UpdateAntBrainProgramPayload.STREAM_CODEC,
                UpdateAntBrainProgramPayload::handlePacketFromClient);
        registrar.playToServer(RunAntScriptPayload.TYPE, RunAntScriptPayload.STREAM_CODEC,
                RunAntScriptPayload::handlePacketFromClient);
        registrar.playToServer(SetDebugOverlayVisiblePayload.TYPE, SetDebugOverlayVisiblePayload.STREAM_CODEC, SetDebugOverlayVisiblePayload::handlePacketFromClient);
    }

    // Add the example block item to the building blocks tab
    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.SPAWN_EGGS) {
            event.accept(ModItems.ANT_SPAWN_EGG);
        }

        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(ModBlocks.ANT_CRAFTING_TABLE);
        }
    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        // Do something when the server starts
        LOGGER.info("HELLO from server starting");
    }

    private void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(ModEntities.ANT.get(), AntEntity.createAttributes().build());
    }

    @SubscribeEvent
    public void registerCommands(RegisterCommandsEvent event) {
        AntBreakBlockCommand.register(event);
        AntSetBlockCommand.register(event);
        AntGiveCommand.register(event);
        AntUseContainerCommand.register(event);
        AntFindBlockCommand.register(event);
        AntFindBlockEntityCommand.register(event);
        AntCraftItemCommand.register(event);
        AntFindEntityCommand.register(event);
        ModuleToCodeCommand.register(event);
        CodeToModuleCommand.register(event);
        AntRunJsonCommand.register(event);
        AntFindPheromoneCommand.register(event);
        AntAttackCommand.register(event);
    }
}
