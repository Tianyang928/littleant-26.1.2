package net.tianyang928.littleant.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.tianyang928.littleant.LittleAnt;
import net.tianyang928.littleant.gui.AntBrainProgramMenu;

/** Explicit server-side entry point for the small script runtime. */
public record RunAntScriptPayload(int containerId, String source) implements CustomPacketPayload {
    public static final Type<RunAntScriptPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(LittleAnt.MOD_ID, "run_ant_script"));
    public static final StreamCodec<ByteBuf, RunAntScriptPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, RunAntScriptPayload::containerId,
            ByteBufCodecs.stringUtf8(8192), RunAntScriptPayload::source,
            RunAntScriptPayload::new);
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    public static void handlePacketFromClient(RunAntScriptPayload payload, IPayloadContext context) {
        if (context.player().containerMenu.containerId != payload.containerId()
                || !(context.player().containerMenu instanceof AntBrainProgramMenu menu)
                || !menu.stillValid(context.player()) || menu.ant == null) return;
        menu.ant.runScript(payload.source());
    }
}
