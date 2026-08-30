package net.tianyang928.littleant.server.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.tianyang928.littleant.entity.AntEntity;

import java.util.Objects;

public class AntAttackCommand {
    public static void register(RegisterCommandsEvent event){
        // attack entity
        event.getDispatcher().register(
                Commands.literal("antattack")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.argument("name", StringArgumentType.string())
                                .then(Commands.argument("entity_id", IntegerArgumentType.integer())
                                        .executes(context -> {
                                            String name = StringArgumentType.getString(context, "name");
                                            int entityId = IntegerArgumentType.getInteger(context, "entity_id");
                                            ServerLevel level = context.getSource().getLevel();
                                            LivingEntity target = level.getEntity(entityId) instanceof LivingEntity? (LivingEntity) level.getEntity(entityId) : null;
                                            int count = 0;
                                            for (var entity : level.getEntities().getAll()) {
                                                if (entity instanceof AntEntity ant
                                                        && ant.hasCustomName()
                                                        && name.equals(Objects.requireNonNull(ant.getCustomName()).getString())) {
                                                    if(target != null){
                                                        ant.setMeleeAttackTarget(target);
                                                    }
                                                    else{
                                                        context.getSource().sendFailure(Component.literal("未找到 ID 为 " + entityId + " 的实体"));
                                                        return 0;
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
                                                    () -> Component.literal("已让 " + matched + " 个 Ant 攻击 " + target.getName()), true);
                                            return count;
                                        }))));
    }
}
