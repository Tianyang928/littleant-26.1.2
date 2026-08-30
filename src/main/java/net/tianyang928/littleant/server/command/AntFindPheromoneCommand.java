package net.tianyang928.littleant.server.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.blocks.BlockInput;
import net.minecraft.commands.arguments.blocks.BlockStateArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.tianyang928.littleant.entity.AntEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class AntFindPheromoneCommand {
    public static void register(RegisterCommandsEvent event) {
        var buildContext = event.getBuildContext();
        // find block entity
        event.getDispatcher().register(
                Commands.literal("antfindpheromone")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.argument("name", StringArgumentType.string())
                                .then(Commands.argument("pheromone_name", StringArgumentType.string())
                                        .executes(context -> {
                                            String name = StringArgumentType.getString(context, "name");
                                            String pheromoneName = StringArgumentType.getString(context, "pheromone_name");
                                            ServerLevel level = context.getSource().getLevel();
                                            List<BlockPos> resultPos = new ArrayList<>();
                                            int count = 0;
                                            for (var entity : level.getEntities().getAll()) {
                                                if (entity instanceof AntEntity ant
                                                        && ant.hasCustomName()
                                                        && name.equals(Objects.requireNonNull(ant.getCustomName()).getString())) {
                                                    BlockPos result = ant.setFindPheromoneTarget(pheromoneName);
                                                    if(result != null) {
                                                        resultPos.add(result);
                                                    }
                                                    count++;
                                                }
                                            }
                                            if (count == 0) {
                                                context.getSource().sendFailure(Component.literal("未找到名为 \"" + name + "\" 的 Ant"));
                                                return 0;
                                            }
                                            if(resultPos.isEmpty()){
                                                context.getSource().sendFailure(Component.literal("未找到名为 \"" + pheromoneName + "\" 的 信息素方块"));
                                                return 0;
                                            }
                                            int matched = count;
                                            context.getSource().sendSuccess(
                                                    () -> Component.literal("已让 " + matched + " 个 Ant 查找 \"" + pheromoneName + "\"" + " 位置在 " + resultPos), true);
                                            return count;
                                        }))));
    }
}
