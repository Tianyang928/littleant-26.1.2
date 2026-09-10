package net.tianyang928.littleant.server.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceArgument;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.tianyang928.littleant.entity.AntEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class AntFindEntityCommand {
    public static void register(RegisterCommandsEvent event) {
        var buildContext = event.getBuildContext();
        // find entity
        event.getDispatcher().register(
                Commands.literal("antfindentity")
                        .requires(player -> player.hasPermission(2))
                        .then(Commands.argument("name", StringArgumentType.string())
                                .then(Commands.argument("entity", ResourceArgument.resource(buildContext, Registries.ENTITY_TYPE))
                                        .executes(context -> execute(context, 1))
                                        .then(Commands.argument("count", IntegerArgumentType.integer(1, 256))
                                        .executes(context -> execute(context, IntegerArgumentType.getInteger(context, "count")))))));
    }
    private static int execute(com.mojang.brigadier.context.CommandContext<net.minecraft.commands.CommandSourceStack> context, int requestedCount) throws CommandSyntaxException {
                                            String name = StringArgumentType.getString(context, "name");
                                            Holder.Reference<EntityType<?>> entityType = ResourceArgument.getSummonableEntityType(context, "entity");
                                            ServerLevel level = context.getSource().getLevel();
                                            List<Integer> entityIds = new ArrayList<>();
                                            int count = 0;
                                            for (var entity : level.getEntities().getAll()) {
                                                if (entity instanceof AntEntity ant
                                                        && ant.hasCustomName()
                                                        && name.equals(Objects.requireNonNull(ant.getCustomName()).getString())) {
                                                    entityIds.addAll(ant.setFindEntityListTarget(List.of(entityType.value()), requestedCount));
                                                    count++;
                                                }
                                            }
                                            if (count == 0) {
                                                context.getSource().sendFailure(Component.translatable("command.littleant.ant_not_found", name));
                                                return 0;
                                            }
                                            if (entityIds.isEmpty()) {
                                                context.getSource().sendFailure(Component.translatable("command.littleant.find.none", entityType.value()));
                                                return 0;
                                            }
                                            int matched = count;
                                            context.getSource().sendSuccess(
                                                    () -> Component.translatable("command.littleant.find.result", name, entityType.value().toString(), entityIds.toString()), true);
                                            return count;
    }
}
