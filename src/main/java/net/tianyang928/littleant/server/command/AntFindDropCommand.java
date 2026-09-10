package net.tianyang928.littleant.server.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
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
                        .requires(player -> player.hasPermission(2))
                        .then(Commands.argument("name", StringArgumentType.string())
                                .then(Commands.argument("item", ItemArgument.item(buildContext))
                                        .executes(context -> execute(context, 1))
                                        .then(Commands.argument("count", IntegerArgumentType.integer(1, 256))
                                        .executes(context -> execute(context, IntegerArgumentType.getInteger(context, "count")))))));
    }
    private static int execute(com.mojang.brigadier.context.CommandContext<net.minecraft.commands.CommandSourceStack> context, int requestedCount) throws CommandSyntaxException {
                                            String name = StringArgumentType.getString(context, "name");
                                            ItemInput itemInput = ItemArgument.getItem(context, "item");
                                            Item item = itemInput.getItem();
                                            ServerLevel level = context.getSource().getLevel();
                                            List<net.minecraft.core.BlockPos> positions = new ArrayList<>();
                                            int count = 0;
                                            for (var entity : level.getEntities().getAll()) {
                                                if (entity instanceof AntEntity ant
                                                        && ant.hasCustomName()
                                                        && name.equals(Objects.requireNonNull(ant.getCustomName()).getString())) {
                                                    positions.addAll(ant.setFindDropListTarget(List.of(item), requestedCount));
                                                    count++;
                                                }
                                            }
                                            if (count == 0) {
                                                context.getSource().sendFailure(Component.translatable("command.littleant.ant_not_found", name));
                                                return 0;
                                            }
                                            if (positions.isEmpty()) {
                                                context.getSource().sendFailure(Component.translatable("command.littleant.find.none", itemInput.createItemStack(1,false).getDisplayName()));
                                                return 0;
                                            }
                                            context.getSource().sendSuccess(
                                                    () -> {
                                                        try {
                                                            return Component.translatable("command.littleant.find.result", name, itemInput.createItemStack(1,false).getDisplayName(), positions.toString());
                                                        } catch (CommandSyntaxException e) {
                                                            throw new RuntimeException(e);
                                                        }
                                                    }, true);
                                            return count;
    }
}
