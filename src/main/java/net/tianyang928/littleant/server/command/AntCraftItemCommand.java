package net.tianyang928.littleant.server.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.tianyang928.littleant.entity.AntEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class AntCraftItemCommand {
    public static void register(RegisterCommandsEvent event){
        var buildContext = event.getBuildContext();
        // craft item
        event.getDispatcher().register(
                Commands.literal("antcraft")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.argument("name", StringArgumentType.string())
                                .then(Commands.argument("amount", IntegerArgumentType.integer(1, 64))
                                        .then(Commands.literal("2x2")
                                                .then(Commands.argument("slot1", ItemArgument.item(buildContext))
                                                        .then(Commands.argument("slot2", ItemArgument.item(buildContext))
                                                                .then(Commands.argument("slot3", ItemArgument.item(buildContext))
                                                                        .then(Commands.argument("slot4", ItemArgument.item(buildContext))
                                                                                .executes(AntCraftItemCommand::executeInventoryCraft))))))
                                        .then(Commands.literal("3x3")
                                                .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                                        .then(Commands.argument("slot1", ItemArgument.item(buildContext))
                                                                .then(Commands.argument("slot2", ItemArgument.item(buildContext))
                                                                        .then(Commands.argument("slot3", ItemArgument.item(buildContext))
                                                                                .then(Commands.argument("slot4", ItemArgument.item(buildContext))
                                                                                        .then(Commands.argument("slot5", ItemArgument.item(buildContext))
                                                                                                .then(Commands.argument("slot6", ItemArgument.item(buildContext))
                                                                                                        .then(Commands.argument("slot7", ItemArgument.item(buildContext))
                                                                                                                .then(Commands.argument("slot8", ItemArgument.item(buildContext))
                                                                                                                        .then(Commands.argument("slot9", ItemArgument.item(buildContext))
                                                                                                                                .executes(AntCraftItemCommand::executeTableCraft))))))))))))
                                )));
    }


    private static int executeInventoryCraft(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        return queueCraft(context, CraftingInput.of(2, 2, readSlots(context, 4)), null);
    }

    private static int executeTableCraft(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        return queueCraft(context, CraftingInput.of(3, 3, readSlots(context, 9)),
                BlockPosArgument.getLoadedBlockPos(context, "pos"));
    }

    private static List<ItemStack> readSlots(CommandContext<CommandSourceStack> context, int count) throws CommandSyntaxException {
        List<ItemStack> items = new ArrayList<>(count);
        for (int i = 1; i <= count; i++) {
            items.add(ItemArgument.getItem(context, "slot" + i).createItemStack(1));
        }
        return items;
    }

    private static int queueCraft(CommandContext<CommandSourceStack> context, CraftingInput input, BlockPos tablePos) {
        String name = StringArgumentType.getString(context, "name");
        int amountCrafted = IntegerArgumentType.getInteger(context, "amount");
        int count = 0;
        for (var entity : context.getSource().getLevel().getEntities().getAll()) {
            if (entity instanceof AntEntity ant && ant.hasCustomName()
                    && name.equals(Objects.requireNonNull(ant.getCustomName()).getString())) {
                if (tablePos == null) {
                    ant.setInventoryCraftingInput(input, amountCrafted);
                }
                else {
                    ant.setCraftingTableInput(input, tablePos, amountCrafted);
                }
                count++;
            }
        }
        if (count == 0) {
            context.getSource().sendFailure(Component.literal("未找到名为 \"" + name + "\" 的 Ant"));
            return 0;
        }
        int matched = count;
        context.getSource().sendSuccess(() -> Component.literal("已让 " + matched + " 个 Ant 执行合成调试"), true);
        return count;
    }
}
