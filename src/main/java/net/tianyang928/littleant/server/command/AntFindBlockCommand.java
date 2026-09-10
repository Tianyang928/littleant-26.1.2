package net.tianyang928.littleant.server.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.blocks.BlockInput;
import net.minecraft.commands.arguments.blocks.BlockStateArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.tianyang928.littleant.entity.AntEntity;

import java.util.Objects;
import java.util.List;

public class AntFindBlockCommand {
    public static void register(RegisterCommandsEvent event) {
        var buildContext = event.getBuildContext();
        // find block
        event.getDispatcher().register(
                Commands.literal("antfindblock")
                        .requires(player -> player.hasPermission(2))
                        .then(Commands.argument("name", StringArgumentType.string())
                                .then(Commands.argument("block", BlockStateArgument.block(buildContext))
                                        .executes(context -> execute(context, 1))
                                        .then(Commands.argument("count", IntegerArgumentType.integer(1, 256))
                                        .executes(context -> execute(context, IntegerArgumentType.getInteger(context, "count")))))));
    }

    private static int execute(com.mojang.brigadier.context.CommandContext<net.minecraft.commands.CommandSourceStack> context, int requestedCount) {
                                            String name = StringArgumentType.getString(context, "name");
                                            BlockInput blockInput = BlockStateArgument.getBlock(context, "block");
                                            ServerLevel level = context.getSource().getLevel();

                                            Block block = blockInput.getState().getBlock();
                                            int count = 0;
                                            for (var entity : level.getEntities().getAll()) {
                                                if (entity instanceof AntEntity ant
                                                        && ant.hasCustomName()
                                                        && name.equals(Objects.requireNonNull(ant.getCustomName()).getString())) {
                                                    List<BlockPos> resultPos = ant.setFindBlockListTarget(List.of(block), requestedCount);
                                                    if (!resultPos.isEmpty()) context.getSource().sendSuccess(() -> Component.translatable("command.littleant.find.result", name, block.getName(), resultPos.toString()), true);
                                                    count++;
                                                }
                                            }
                                            if (count == 0) {
                                                context.getSource().sendFailure(Component.translatable("command.littleant.ant_not_found", name));
                                                return 0;
                                            }
                                            int matched = count;
                                            context.getSource().sendSuccess(
                                                    () -> Component.translatable("command.littleant.find.assigned", matched, block.getName()), true);
                                            return count;
    }
}
