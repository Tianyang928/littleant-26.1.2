package net.tianyang928.littleant;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.commands.arguments.item.ItemInput;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.tianyang928.littleant.block.ModBlocks;
import net.tianyang928.littleant.block_entity.ModBlockEntities;
import net.tianyang928.littleant.creativemodetab.ModCreativeModeTabs;
import net.tianyang928.littleant.entity.AntEntity;
import net.tianyang928.littleant.entity.ModEntities;
import net.tianyang928.littleant.inventory.ModMenus;
import net.tianyang928.littleant.item.ModItems;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;

import java.util.Objects;

@Mod(LittleAnt.MOD_ID)
public class LittleAnt {
    public static final String MOD_ID = "littleant";
    // Directly reference a slf4j logger
    public static final Logger LOGGER = LogUtils.getLogger();

    public LittleAnt(IEventBus modEventBus, ModContainer modContainer) {
        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);

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
        var buildContext = event.getBuildContext();

        // break block
        event.getDispatcher().register(
                Commands.literal("antbreakblock")
                        .requires(Commands.hasPermission(Commands.LEVEL_ALL))
                        .then(Commands.argument("name", StringArgumentType.string())
                                .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                        .executes(context -> {
                                            String name = StringArgumentType.getString(context, "name");
                                            BlockPos pos = BlockPosArgument.getLoadedBlockPos(context, "pos");
                                            ServerLevel level = context.getSource().getLevel();
                                            int count = 0;
                                            for (var entity : level.getEntities().getAll()) {
                                                if (entity instanceof AntEntity ant
                                                        && ant.hasCustomName()
                                                        && name.equals(Objects.requireNonNull(ant.getCustomName()).getString())) {
                                                    ant.setBreakTarget(pos);
                                                    count++;
                                                }
                                            }
                                            if (count == 0) {
                                                context.getSource().sendFailure(Component.literal("未找到名为 \"" + name + "\" 的 Ant"));
                                                return 0;
                                            }
                                            int matched = count;
                                            context.getSource().sendSuccess(
                                                    () -> Component.literal("已让 " + matched + " 个 Ant 挖掘 " + pos), true);
                                            return count;
                                        }))));


        // set block
        event.getDispatcher().register(
                Commands.literal("antsetblock")
                        .requires(Commands.hasPermission(Commands.LEVEL_ALL))
                        .then(Commands.argument("name", StringArgumentType.string())
                                .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                        .executes(context -> {
                                            String name = StringArgumentType.getString(context, "name");
                                            BlockPos pos = BlockPosArgument.getLoadedBlockPos(context, "pos");
                                            ServerLevel level = context.getSource().getLevel();
                                            int count = 0;
                                            for (var entity : level.getEntities().getAll()) {
                                                if (entity instanceof AntEntity ant
                                                        && ant.hasCustomName()
                                                        && name.equals(Objects.requireNonNull(ant.getCustomName()).getString())) {
                                                    ant.setSetTarget(pos);
                                                    count++;
                                                }
                                            }
                                            if (count == 0) {
                                                context.getSource().sendFailure(Component.literal("未找到名为 \"" + name + "\" 的 Ant"));
                                                return 0;
                                            }
                                            int matched = count;
                                            context.getSource().sendSuccess(
                                                    () -> Component.literal("已让 " + matched + " 个 Ant 放置 " + pos), true);
                                            return count;
                                        }))));

        // give ant item
        event.getDispatcher().register(
                Commands.literal("antgive")
                        .requires(Commands.hasPermission(Commands.LEVEL_ALL))
                        .then(Commands.argument("ant_name", StringArgumentType.string())
                                .then(Commands.argument("item", ItemArgument.item(buildContext))
                                        .then(Commands.argument("item_count", IntegerArgumentType.integer(1, 64))
                                                .executes(context -> {
                                                    String name = StringArgumentType.getString(context, "ant_name");
                                                    ItemInput itemInput = ItemArgument.getItem(context, "item");
                                                    int itemCount = IntegerArgumentType.getInteger(context, "item_count");
                                                    ServerLevel level = context.getSource().getLevel();

                                                    ItemStack itemStack = itemInput.createItemStack(itemCount);
                                                    int antCount = 0;
                                                    for (var entity : level.getEntities().getAll()) {
                                                        if (entity instanceof AntEntity ant
                                                                && ant.hasCustomName()
                                                                && name.equals(Objects.requireNonNull(ant.getCustomName()).getString())) {
                                                                ant.giveItem(itemStack);
                                                                antCount++;
                                                        }
                                                    }
                                                    if (antCount == 0) {
                                                        context.getSource().sendFailure(Component.literal("未找到名为 \"" + name + "\" 的 Ant"));
                                                        return 0;
                                                    }
                                                    int matched = antCount;
                                                    context.getSource().sendSuccess(
                                                            () -> Component.literal("已给 " + matched + " 个 Ant " + itemStack.getDisplayName() + " " + itemCount + " 个"), true);
                                                    return antCount;
                                                })))));
    }
}
