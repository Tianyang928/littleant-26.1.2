package net.tianyang928.littleant.server.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
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
        // find pheromone
        event.getDispatcher().register(
                Commands.literal("antfindpheromone")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.argument("name", StringArgumentType.string())
                                .then(Commands.argument("pheromone_name", StringArgumentType.string())
                                        .executes(context -> execute(context, 1))
                                        .then(Commands.argument("count", IntegerArgumentType.integer(1, 256))
                                        .executes(context -> execute(context, IntegerArgumentType.getInteger(context, "count")))))));
    }
    private static int execute(com.mojang.brigadier.context.CommandContext<net.minecraft.commands.CommandSourceStack> context, int requestedCount) {
                                            String name = StringArgumentType.getString(context, "name");
                                            String pheromoneName = StringArgumentType.getString(context, "pheromone_name");
                                            ServerLevel level = context.getSource().getLevel();
                                            List<BlockPos> resultPos = new ArrayList<>();
                                            int count = 0;
                                            for (var entity : level.getEntities().getAll()) {
                                                if (entity instanceof AntEntity ant
                                                        && ant.hasCustomName()
                                                        && name.equals(Objects.requireNonNull(ant.getCustomName()).getString())) {
                                                    resultPos.addAll(ant.setFindPheromoneListTarget(pheromoneName, requestedCount));
                                                    count++;
                                                }
                                            }
                                            if (count == 0) {
                                                context.getSource().sendFailure(Component.translatable("command.littleant.ant_not_found", name));
                                                return 0;
                                            }
                                            if(resultPos.isEmpty()){
                                                context.getSource().sendFailure(Component.translatable("command.littleant.find.none", pheromoneName));
                                                return 0;
                                            }
                                            int matched = count;
                                            context.getSource().sendSuccess(
                                                    () -> Component.translatable("command.littleant.find.result", name, pheromoneName, resultPos.toString()), true);
                                            return count;
    }
}
