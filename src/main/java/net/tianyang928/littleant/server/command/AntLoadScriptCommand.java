package net.tianyang928.littleant.server.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Util;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.tianyang928.littleant.LittleAnt;
import net.tianyang928.littleant.entity.AntEntity;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public final class AntLoadScriptCommand {
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("antloadscript")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.argument("name", StringArgumentType.string())
                        .then(Commands.argument("source", StringArgumentType.greedyString())
                                .executes(context -> {
                                    String name = StringArgumentType.getString(context, "name");
                                    ServerLevel level = context.getSource().getLevel();
                                    int count = 0;
                                    for (var entity : level.getEntities().getAll()) {
                                        if (entity instanceof AntEntity ant
                                                && ant.hasCustomName()
                                                && name.equals(Objects.requireNonNull(ant.getCustomName()).getString())) {
                                            try {
                                                // 文件读取完成后在主线程执行
                                                readModuleFile(Path.of(StringArgumentType.getString(context, "source"))).thenAccept(ant::runScript).exceptionally(error -> {
                                                    LittleAnt.LOGGER.error("Error processing module", error);
                                                    context.getSource().sendFailure(Component.translatable("command.littleant.script.invalid", error.getMessage()));
                                                    return null;
                                                });

                                                context.getSource().sendSuccess(() -> Component.translatable("command.littleant.script.started"), true);
                                            } catch (RuntimeException exception) {
                                                context.getSource().sendFailure(Component.translatable("command.littleant.script.invalid", exception.getMessage()));
                                            }
                                            count++;
                                        }
                                    }
                                    if (count == 0) {
                                        context.getSource().sendFailure(Component.translatable("command.littleant.ant_not_found", name));
                                        return 0;
                                    }
                                    int matched = count;
                                    context.getSource().sendSuccess(
                                            () -> Component.translatable("command.littleant.script.assigned", matched), true);
                                    return count;
                                }))));
    }


    public static CompletableFuture<String> readModuleFile(Path filePath) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return Files.readString(filePath);
            } catch (Exception e) {
                LittleAnt.LOGGER.error("Failed to read module file: {}", filePath, e);
                throw new RuntimeException(e);
            }
        }, Util.ioPool());
    }

    private AntLoadScriptCommand() {}
}
