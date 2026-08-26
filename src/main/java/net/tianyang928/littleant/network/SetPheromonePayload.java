package net.tianyang928.littleant.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.PacketDistributor;
import net.tianyang928.littleant.LittleAnt;
import net.tianyang928.littleant.gui.PheromoneListMenu;

public record SetPheromonePayload(
        int containerId,
        String id,
        int amount
) implements CustomPacketPayload {

    public static final Type<SetPheromonePayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(
                    LittleAnt.MOD_ID,
                    "set_pheromone"
            ));

    // 编码成字节流 解码成SetPheromonePayload对象
    public static final StreamCodec<ByteBuf, SetPheromonePayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT,
                    SetPheromonePayload::containerId,
                    ByteBufCodecs.stringUtf8(64),
                    SetPheromonePayload::id,
                    ByteBufCodecs.VAR_INT,
                    SetPheromonePayload::amount,
                    SetPheromonePayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    // 从客户端处理pheromone更新, run in server
    public static void handlePacketFromClient(
            SetPheromonePayload payload,
            IPayloadContext context
    ) {
        Player player = context.player();

        if (player.containerMenu.containerId != payload.containerId()) {
            return;
        }

        if (!(player.containerMenu instanceof PheromoneListMenu menu)) {
            return;
        }

        if (!menu.stillValid(player)) {
            return;
        }

        String id = payload.id();
        int amount = payload.amount();

        if (!menu.setPheromone(id, amount)) {
            return;
        }
        ServerPlayer serverPlayer = (ServerPlayer) player;
        for (ServerPlayer viewer : serverPlayer.level().players()) {
            if (viewer.containerMenu instanceof PheromoneListMenu viewerMenu
                    && viewerMenu.isViewing(menu.getPheromoneBlockEntity())) {
                PacketDistributor.sendToPlayer(
                        viewer,
                        new SyncPheromonePayload(viewerMenu.containerId, id, amount)
                );
            }
        }
        LittleAnt.LOGGER.info("[SetPheromonePayload] handle: {} {}", id, amount);
    }
}
