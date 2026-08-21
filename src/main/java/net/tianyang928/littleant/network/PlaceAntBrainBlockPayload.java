package net.tianyang928.littleant.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.tianyang928.littleant.LittleAnt;
import net.tianyang928.littleant.gui.AntBrainProgramMenu;

import java.util.UUID;

/** Client request to drop one palette block onto the program canvas. */
public record PlaceAntBrainBlockPayload(int containerId, String text, int x, int y, String id) implements CustomPacketPayload {
    public static final Type<PlaceAntBrainBlockPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(LittleAnt.MOD_ID, "place_ant_brain_block"));
    public static final StreamCodec<ByteBuf, PlaceAntBrainBlockPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, PlaceAntBrainBlockPayload::containerId,
            ByteBufCodecs.stringUtf8(64), PlaceAntBrainBlockPayload::text,
            ByteBufCodecs.VAR_INT, PlaceAntBrainBlockPayload::x,
            ByteBufCodecs.VAR_INT, PlaceAntBrainBlockPayload::y,
            ByteBufCodecs.stringUtf8(36), PlaceAntBrainBlockPayload::id,
            PlaceAntBrainBlockPayload::new);

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handlePacketFromClient(PlaceAntBrainBlockPayload payload, IPayloadContext context) {
        if (context.player().containerMenu.containerId != payload.containerId()
                || !(context.player().containerMenu instanceof AntBrainProgramMenu menu)
                || !menu.stillValid(context.player())
                || !AntBrainProgramMenu.KNOWN_BLOCKS.contains(payload.text())) {
            return;
        }
        // Canvas-relative coordinates and the count are bounded server-side, never trusted from the client.
        menu.ant.addBrainBlock(payload.text(), Mth.clamp(payload.x(), 0, 4096), Mth.clamp(payload.y(), 0, 4096), UUID.fromString(payload.id));
    }
}
