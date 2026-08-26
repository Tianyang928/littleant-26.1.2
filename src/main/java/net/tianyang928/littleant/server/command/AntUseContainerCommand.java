package net.tianyang928.littleant.server.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.commands.arguments.item.ItemInput;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.tianyang928.littleant.entity.AntEntity;
import net.tianyang928.littleant.entity.ai.goal.UseContainerGoal;

import java.util.Objects;

public final class AntUseContainerCommand {
    private AntUseContainerCommand() {}

    public static void register(RegisterCommandsEvent event) {
        var buildContext = event.getBuildContext();
        event.getDispatcher().register(
                Commands.literal("antusecontainer")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.argument("name", StringArgumentType.string())
                                .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                        .then(Commands.literal("put")
                                                .then(Commands.argument("item", ItemArgument.item(buildContext))
                                                        .then(Commands.argument("slot", IntegerArgumentType.integer(0))
                                                                .executes(context -> execute(context, UseContainerGoal.Operation.PUT, 1))
                                                                .then(Commands.argument("count", IntegerArgumentType.integer(1, 64))
                                                                        .executes(context -> execute(context, UseContainerGoal.Operation.PUT, IntegerArgumentType.getInteger(context, "count")))))))
                                        .then(Commands.literal("take")
                                                .then(Commands.argument("item", ItemArgument.item(buildContext))
                                                        .then(Commands.argument("slot", IntegerArgumentType.integer(0))
                                                                .executes(context -> execute(context, UseContainerGoal.Operation.TAKE, 1))
                                                                .then(Commands.argument("count", IntegerArgumentType.integer(1, 64))
                                                                        .executes(context -> execute(context, UseContainerGoal.Operation.TAKE, IntegerArgumentType.getInteger(context, "count"))))))))));
    }

    private static int execute(CommandContext<CommandSourceStack> context, UseContainerGoal.Operation operation, int amount)
            throws CommandSyntaxException {
        String name = StringArgumentType.getString(context, "name");
        BlockPos pos = BlockPosArgument.getLoadedBlockPos(context, "pos");
        ItemInput item = ItemArgument.getItem(context, "item");
        int slot = IntegerArgumentType.getInteger(context, "slot");
        ServerLevel level = context.getSource().getLevel();
        int count = 0;
        for (var entity : level.getEntities().getAll()) {
            if (entity instanceof AntEntity ant && ant.hasCustomName()
                    && name.equals(Objects.requireNonNull(ant.getCustomName()).getString())) {
                ant.setContainerTarget(pos, operation, item.item().value(), slot, amount);
                count++;
            }
        }
        if (count == 0) {
            context.getSource().sendFailure(Component.literal("未找到名为 \"" + name + "\" 的 Ant"));
            return 0;
        }
        int matched = count;
        context.getSource().sendSuccess(() -> {
            try {
                return Component.literal(
                        "已让 " + matched + " 个 Ant " + (operation == UseContainerGoal.Operation.PUT ? "放入" : "取出")
                                + " " + amount + " 个 " + item.createItemStack(1).getDisplayName().getString() + "，容器槽位 " + slot);
            } catch (CommandSyntaxException e) {
                throw new RuntimeException(e);
            }
        }, true);
        return count;
    }
}
