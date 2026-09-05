package net.tianyang928.littleant.server.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Util;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.tianyang928.littleant.LittleAnt;
import net.tianyang928.littleant.entity.AntEntity;
import net.tianyang928.littleant.entity.ai.brain.ModuleToJsonConverter;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public final class ModuleToCodeCommand {
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("antmoduletocode")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.argument("name", StringArgumentType.string())
                        .executes(context -> {
                            String name = StringArgumentType.getString(context, "name");
                            ServerLevel level = context.getSource().getLevel();
                            int count = 0;
                            for (var entity : level.getEntities().getAll()) {
                                if (entity instanceof AntEntity ant
                                        && ant.hasCustomName()
                                        && name.equals(Objects.requireNonNull(ant.getCustomName()).getString())) {
                                    String json = new ModuleToJsonConverter().convert(ant.getBrainBlocks()).toString();
                                    context.getSource().sendSuccess(() -> Component.literal(json), false);
                                    count++;
                                }
                            }
                            if (count == 0) {
                                context.getSource().sendFailure(Component.literal("未找到名为 \"" + name + "\" 的 Ant"));
                                return 0;
                            }
                            int matched = count;
                            context.getSource().sendSuccess(
                                    () -> Component.literal("已让 " + matched + " 个 Ant 从module转换为代码"), true);
                            return count;
                })));
    }
    private ModuleToCodeCommand() {}
}
