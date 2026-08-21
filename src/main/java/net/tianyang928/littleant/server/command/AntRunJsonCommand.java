package net.tianyang928.littleant.server.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.tianyang928.littleant.entity.AntEntity;
import net.tianyang928.littleant.entity.ai.brain.AntScriptInterpreter;

import java.util.Objects;

public final class AntRunJsonCommand {
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("antrunjson")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.argument("name", StringArgumentType.string())
                        .then(Commands.argument("json", StringArgumentType.greedyString())
                                .executes(context -> {
                                    String name = StringArgumentType.getString(context, "name");
                                    ServerLevel level = context.getSource().getLevel();
                                    int count = 0;
                                    for (var entity : level.getEntities().getAll()) {
                                        if (entity instanceof AntEntity ant
                                                && ant.hasCustomName()
                                                && name.equals(Objects.requireNonNull(ant.getCustomName()).getString())) {
                                            try {
                                                ant.runScript(StringArgumentType.getString(context, "json"));
                                                context.getSource().sendSuccess(() -> Component.literal("JSON 脚本已执行"), true);
                                            } catch (RuntimeException exception) {
                                                context.getSource().sendFailure(Component.literal("JSON 脚本无效: " + exception.getMessage()));
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
                                            () -> Component.literal("已让 " + matched + " 个 Ant 执行 JSON 脚本"), true);
                                    return count;
                                }))));
    }
    private AntRunJsonCommand() {}
}
