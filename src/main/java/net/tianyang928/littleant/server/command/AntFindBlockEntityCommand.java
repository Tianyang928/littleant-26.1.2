package net.tianyang928.littleant.server.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.blocks.BlockInput;
import net.minecraft.commands.arguments.blocks.BlockStateArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.tianyang928.littleant.entity.AntEntity;

import java.util.Objects;

public class AntFindBlockEntityCommand {
    public static void register(RegisterCommandsEvent event) {
        var buildContext = event.getBuildContext();
        // find block entity
        event.getDispatcher().register(
                Commands.literal("antfindblockentity")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.argument("name", StringArgumentType.string())
                                .then(Commands.argument("block_entity", BlockStateArgument.block(buildContext))
                                        .executes(context -> {
                                            String name = StringArgumentType.getString(context, "name");
                                            BlockInput blockInput = BlockStateArgument.getBlock(context, "block_entity");
                                            ServerLevel level = context.getSource().getLevel();

                                            BlockState blockState = blockInput.getState();
                                            if(!blockState.hasBlockEntity()){
                                                context.getSource().sendFailure(Component.literal("该块没有实体"));
                                                return 0;
                                            }
                                            int count = 0;
                                            for (var entity : level.getEntities().getAll()) {
                                                if (entity instanceof AntEntity ant
                                                        && ant.hasCustomName()
                                                        && name.equals(Objects.requireNonNull(ant.getCustomName()).getString())) {
                                                    ant.setFindBlockEntityTarget(blockState.getBlock());
                                                    count++;
                                                }
                                            }
                                            if (count == 0) {
                                                context.getSource().sendFailure(Component.literal("未找到名为 \"" + name + "\" 的 Ant"));
                                                return 0;
                                            }
                                            int matched = count;
                                            context.getSource().sendSuccess(
                                                    () -> Component.literal("已让 " + matched + " 个 Ant 查找 " + blockState.getBlock().getName()), true);
                                            return count;
                                        }))));
    }
}
