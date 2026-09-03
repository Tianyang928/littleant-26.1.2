package net.tianyang928.littleant.server.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.commands.arguments.item.ItemInput;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.tianyang928.littleant.entity.AntEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class AntFindDropCommand {
    public static void register(RegisterCommandsEvent event) {
        var buildContext = event.getBuildContext();
        // find drop
        event.getDispatcher().register(
                Commands.literal("antfinddrop")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.argument("name", StringArgumentType.string())
                                .then(Commands.argument("item", ItemArgument.item(buildContext))
                                        .executes(context -> {
                                            String name = StringArgumentType.getString(context, "name");
                                            ItemInput itemInput = ItemArgument.getItem(context, "item");
                                            Item item = itemInput.item().value();
                                            ServerLevel level = context.getSource().getLevel();
                                            List<net.minecraft.core.BlockPos> positions = new ArrayList<>();
                                            int count = 0;
                                            for (var entity : level.getEntities().getAll()) {
                                                if (entity instanceof AntEntity ant
                                                        && ant.hasCustomName()
                                                        && name.equals(Objects.requireNonNull(ant.getCustomName()).getString())) {
                                                    net.minecraft.core.BlockPos result = ant.setFindDropTarget(item);
                                                    if(result != null) positions.add(result);
                                                    count++;
                                                }
                                            }
                                            if (count == 0) {
                                                context.getSource().sendFailure(Component.literal("未找到名为 \"" + name + "\" 的 Ant"));
                                                return 0;
                                            }
                                            if (positions.isEmpty()) {
                                                context.getSource().sendFailure(Component.literal("附近未找到掉落物 " + itemInput.createItemStack(1).getDisplayName().getString()));
                                                return 0;
                                            }
                                            int matched = count;
                                            context.getSource().sendSuccess(
                                                    () -> {
                                                        try {
                                                            return Component.literal("已让 " + matched + " 个 Ant 查找掉落物 " + itemInput.createItemStack(1).getDisplayName().getString() + "，位置 " + positions);
                                                        } catch (CommandSyntaxException e) {
                                                            throw new RuntimeException(e);
                                                        }
                                                    }, true);
                                            return count;
                                        }))));
    }
}
