package net.tianyang928.littleant.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.tianyang928.littleant.LittleAnt;
import net.tianyang928.littleant.client.overlay.debug.AntDebugClientState;

public record SyncAntTaskDebugPayload(int entityId, String antName, String foreground, String background) implements CustomPacketPayload {
    public static final Type<SyncAntTaskDebugPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(LittleAnt.MOD_ID, "sync_ant_task_debug"));
    public static final StreamCodec<ByteBuf, SyncAntTaskDebugPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, SyncAntTaskDebugPayload::entityId,
            ByteBufCodecs.stringUtf8(64), SyncAntTaskDebugPayload::antName,
            ByteBufCodecs.stringUtf8(8192), SyncAntTaskDebugPayload::foreground,
            ByteBufCodecs.stringUtf8(8192), SyncAntTaskDebugPayload::background,
            SyncAntTaskDebugPayload::new);
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    public static void handle(SyncAntTaskDebugPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> AntDebugClientState.put(payload));
    }
}
