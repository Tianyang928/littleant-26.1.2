package net.tianyang928.littleant.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.tianyang928.littleant.LittleAnt;
import net.tianyang928.littleant.client.debug.AntDebugClientState;
import net.tianyang928.littleant.gui.AntBrainProgramMenu;

public record SetDebugOverlayVisiblePayload (int containerId, int visible) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<SetDebugOverlayVisiblePayload> TYPE = new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(LittleAnt.MOD_ID, "set_debug_overlay_visible"));
    public static final StreamCodec<ByteBuf, SetDebugOverlayVisiblePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, SetDebugOverlayVisiblePayload::containerId,
            ByteBufCodecs.VAR_INT, SetDebugOverlayVisiblePayload::visible,
            SetDebugOverlayVisiblePayload::new);
    @Override public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }
    public static void handlePacketFromClient(SetDebugOverlayVisiblePayload payload, IPayloadContext context) {
        if (context.player().containerMenu.containerId != payload.containerId()
                || !(context.player().containerMenu instanceof AntBrainProgramMenu menu)
                || !menu.stillValid(context.player()) || menu.ant == null) return;
        LittleAnt.LOGGER.info("SetDebugOverlayVisiblePayload: {}", payload);
               AntDebugClientState.setEnabled(payload.visible == 1);
    }
}
