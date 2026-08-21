package net.tianyang928.littleant.server.command;

import com.mojang.brigadier.arguments.StringArgumentType;
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

public class AntFindBlockCommand {
    public static void register(RegisterCommandsEvent event) {
        var buildContext = event.getBuildContext();
        // find block
        event.getDispatcher().register(
                Commands.literal("antfindblock")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.argument("name", StringArgumentType.string())
                                .then(Commands.argument("block", BlockStateArgument.block(buildContext))
                                        .executes(context -> {
                                            String name = StringArgumentType.getString(context, "name");
                                            BlockInput blockInput = BlockStateArgument.getBlock(context, "block");
                                            ServerLevel level = context.getSource().getLevel();

                                            Block block = blockInput.getState().getBlock();
                                            BlockPos resultPos = null;
                                            int count = 0;
                                            for (var entity : level.getEntities().getAll()) {
                                                if (entity instanceof AntEntity ant
                                                        && ant.hasCustomName()
                                                        && name.equals(Objects.requireNonNull(ant.getCustomName()).getString())) {
                                                    resultPos = ant.setFindBlockTarget(block);
                                                    if(resultPos!=null){
                                                        BlockPos finalResultPos = resultPos;
                                                        context.getSource().sendSuccess(() -> Component.literal(name + " 已找到 " + block.getName() + " 在 " + finalResultPos), true);
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
                                                    () -> Component.literal("已让 " + matched + " 个 Ant 查找 " + block.getName()), true);
                                            return count;
                                        }))));
    }
}
