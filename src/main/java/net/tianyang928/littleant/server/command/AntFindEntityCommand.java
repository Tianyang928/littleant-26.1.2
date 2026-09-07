package net.tianyang928.littleant.server.command;

import com.mojang.brigadier.arguments.StringArgumentType;
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
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.argument("name", StringArgumentType.string())
                                .then(Commands.argument("entity", ResourceArgument.resource(buildContext, Registries.ENTITY_TYPE))
                                        .executes(context -> {
                                            String name = StringArgumentType.getString(context, "name");
                                            Holder.Reference<EntityType<?>> entityType = ResourceArgument.getSummonableEntityType(context, "entity");
                                            ServerLevel level = context.getSource().getLevel();
                                            List<Integer> entityIds = new ArrayList<>();
                                            int count = 0;
                                            for (var entity : level.getEntities().getAll()) {
                                                if (entity instanceof AntEntity ant
                                                        && ant.hasCustomName()
                                                        && name.equals(Objects.requireNonNull(ant.getCustomName()).getString())) {
                                                    int result = ant.setFindEntityTarget(List.of(entityType.value()));
                                                    if(result != -1) {
                                                        entityIds.add(result);
                                                    }
                                                    count++;
                                                }
                                            }
                                            if (count == 0) {
                                                context.getSource().sendFailure(Component.literal("未找到名为 \"" + name + "\" 的 Ant"));
                                                return 0;
                                            }
                                            if (entityIds.isEmpty()) {
                                                context.getSource().sendFailure(Component.literal("未找到目标实体" + entityType.value()));
                                                return 0;
                                            }
                                            int matched = count;
                                            context.getSource().sendSuccess(
                                                    () -> Component.literal("已让 " + matched + " 个 Ant 查找 " + entityType.value() + " id分别为" + entityIds), true);
                                            return count;
                                        }))));
    }
}
