package net.tianyang928.littleant.server.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.commands.arguments.item.ItemInput;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.tianyang928.littleant.entity.AntEntity;

import java.util.Objects;

public class AntGiveCommand {
    public static void register(RegisterCommandsEvent event) {
        var buildContext = event.getBuildContext();
        // give ant item
        event.getDispatcher().register(
                Commands.literal("antgive")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
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
