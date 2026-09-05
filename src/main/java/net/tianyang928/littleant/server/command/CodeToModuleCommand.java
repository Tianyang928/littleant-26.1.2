package net.tianyang928.littleant.server.command;

import com.google.gson.JsonParser;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.tianyang928.littleant.entity.AntEntity;
import net.tianyang928.littleant.entity.ai.brain.JsonToModuleConverter;

import java.util.Objects;

public final class CodeToModuleCommand {
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("antcodetomodule")
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
                                                var blocks = new JsonToModuleConverter().convert(JsonParser.parseString(StringArgumentType.getString(context, "source")).getAsJsonObject());
                                                ant.replaceBrainBlocks(blocks);
                                                context.getSource().sendSuccess(() -> Component.literal("已导入 " + blocks.size() + " 个模块"), true);
                                            } catch (RuntimeException exception) {
                                                context.getSource().sendFailure(Component.literal("代码无效: " + exception.getMessage()));
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
                                            () -> Component.literal("已让 " + matched + " 个 Ant 从代码转换为模块"), true);
                                    return count;
                        }))));
    }
    private CodeToModuleCommand() {}
}
