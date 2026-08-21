package net.tianyang928.littleant.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.tianyang928.littleant.LittleAnt;
import net.tianyang928.littleant.gui.AntBrainProgramMenu;

import java.util.UUID;

public record RemoveAntBrainBlockPayload(int containerId, String id) implements CustomPacketPayload {
    public static final Type<RemoveAntBrainBlockPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(LittleAnt.MOD_ID, "remove_ant_brain_block"));
    public static final StreamCodec<ByteBuf, RemoveAntBrainBlockPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, RemoveAntBrainBlockPayload::containerId,
            ByteBufCodecs.stringUtf8(32), RemoveAntBrainBlockPayload::id,
            RemoveAntBrainBlockPayload::new);

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handlePacketFromClient(RemoveAntBrainBlockPayload payload, IPayloadContext context) {
        if (context.player().containerMenu.containerId != payload.containerId()
                || !(context.player().containerMenu instanceof AntBrainProgramMenu menu)
                || !menu.stillValid(context.player())) {
            return;
        }
        // Canvas-relative coordinates and the count are bounded server-side, never trusted from the client.
        if (menu.ant != null) {
            menu.ant.removeBrainBlock(UUID.fromString(payload.id));
        }
    }
}
