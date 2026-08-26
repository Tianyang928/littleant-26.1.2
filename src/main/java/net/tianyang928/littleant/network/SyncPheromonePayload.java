package net.tianyang928.littleant.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.tianyang928.littleant.LittleAnt;
import net.tianyang928.littleant.gui.PheromoneListMenu;

/** Server-to-client confirmation of one pheromone entry. */
public record SyncPheromonePayload(int containerId, String id, int amount) implements CustomPacketPayload {
    public static final Type<SyncPheromonePayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(LittleAnt.MOD_ID, "sync_pheromone"));

    public static final StreamCodec<ByteBuf, SyncPheromonePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, SyncPheromonePayload::containerId,
            ByteBufCodecs.stringUtf8(64), SyncPheromonePayload::id,
            ByteBufCodecs.VAR_INT, SyncPheromonePayload::amount,
            SyncPheromonePayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handlePheromoneSync(SyncPheromonePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (Minecraft.getInstance().player == null
                    || Minecraft.getInstance().player.containerMenu.containerId != payload.containerId()
                    || !(Minecraft.getInstance().player.containerMenu instanceof PheromoneListMenu menu)) {
                return;
            }
            menu.updatePheromone(payload.id(), payload.amount());
        });
    }
}
