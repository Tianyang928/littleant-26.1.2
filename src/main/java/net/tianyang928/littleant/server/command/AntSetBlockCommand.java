package net.tianyang928.littleant.server.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.tianyang928.littleant.entity.AntEntity;

import java.util.Objects;

public class AntSetBlockCommand {
    public static void register(RegisterCommandsEvent event) {
        // set block
        event.getDispatcher().register(
                Commands.literal("antsetblock")
                        .requires(player -> player.hasPermission(2))
                        .then(Commands.argument("name", StringArgumentType.string())
                                .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                        .executes(context -> {
                                            String name = StringArgumentType.getString(context, "name");
                                            BlockPos pos = BlockPosArgument.getLoadedBlockPos(context, "pos");
                                            ServerLevel level = context.getSource().getLevel();
                                            int count = 0;
                                            for (var entity : level.getEntities().getAll()) {
                                                if (entity instanceof AntEntity ant
                                                        && ant.hasCustomName()
                                                        && name.equals(Objects.requireNonNull(ant.getCustomName()).getString())) {
                                                    ant.setSetTarget(pos);
                                                    count++;
                                                }
                                            }
                                            if (count == 0) {
                                                context.getSource().sendFailure(Component.translatable("command.littleant.ant_not_found", name));
                                                return 0;
                                            }
                                            int matched = count;
                                            context.getSource().sendSuccess(
                                                    () -> Component.translatable("command.littleant.set.success", matched, pos.toString()), true);
                                            return count;
                                        }))));
    }
}
