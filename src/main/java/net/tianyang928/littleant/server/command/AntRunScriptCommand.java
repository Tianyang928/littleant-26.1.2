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

public final class AntRunScriptCommand {
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("antrunscript")
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
                                                    context.getSource().sendFailure(Component.literal("脚本无效: " + error.getMessage()));
                                                    return null;
                                                });

                                                context.getSource().sendSuccess(() -> Component.literal("脚本已执行"), true);
                                            } catch (RuntimeException exception) {
                                                context.getSource().sendFailure(Component.literal("脚本无效: " + exception.getMessage()));
                                            }
                                            count++;
                                        }
                                    }
                                    if (count == 0) {
                                        context.getSource().sendFailure(Component.literal("未找到名为 \"" + name + "\" 的 Ant"));
                                        return 0;
                                    }
                                    int matched = count;
                                    context.getSource().sendSuccess(
                                            () -> Component.literal("已让 " + matched + " 个 Ant 执行脚本"), true);
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

    private AntRunScriptCommand() {}
}
