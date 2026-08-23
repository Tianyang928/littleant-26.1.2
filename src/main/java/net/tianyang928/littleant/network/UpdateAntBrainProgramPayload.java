package net.tianyang928.littleant.network;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.tianyang928.littleant.LittleAnt;
import net.tianyang928.littleant.entity.ai.brain.BlockDefinition;
import net.tianyang928.littleant.entity.ai.brain.BrainBlock;
import net.tianyang928.littleant.entity.ai.brain.InputDefinition;
import net.tianyang928.littleant.entity.ai.brain.InputSlot;
import net.tianyang928.littleant.entity.ai.brain.ModuleRegistry;
import net.tianyang928.littleant.entity.ai.brain.ValueType;
import net.tianyang928.littleant.gui.AntBrainProgramMenu;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Atomic client-to-server update for the visual program graph. */
public record UpdateAntBrainProgramPayload(int containerId, String program) implements CustomPacketPayload {
    public static final Type<UpdateAntBrainProgramPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(LittleAnt.MOD_ID, "update_ant_brain_program"));
    public static final StreamCodec<ByteBuf, UpdateAntBrainProgramPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, UpdateAntBrainProgramPayload::containerId,
            ByteBufCodecs.stringUtf8(1048576), UpdateAntBrainProgramPayload::program,
            UpdateAntBrainProgramPayload::new);

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static String encode(Map<UUID, BrainBlock> blocks) {
        JsonArray result = new JsonArray();
        for (BrainBlock block : blocks.values()) {
            JsonObject json = new JsonObject();
            json.addProperty("opcode", block.opcode());
            json.addProperty("x", block.x());
            json.addProperty("y", block.y());
            json.addProperty("id", block.id().toString());
            if (block.next() != null) json.addProperty("next", block.next().toString());
            if (block.parent() != null) json.addProperty("parent", block.parent().toString());
            JsonArray inputs = new JsonArray();
            for (InputSlot input : block.inputs()) {
                JsonObject slot = new JsonObject();
                slot.addProperty("name", input.name());
                slot.addProperty("type", input.type().name());
                if (input.value() != null) slot.addProperty("value", input.value());
                if (input.blockId() != null) slot.addProperty("block", input.blockId().toString());
                inputs.add(slot);
            }
            json.add("inputs", inputs);
            result.add(json);
        }
        return result.toString();
    }

    public static void handlePacketFromClient(UpdateAntBrainProgramPayload payload, IPayloadContext context) {
        if (context.player().containerMenu.containerId != payload.containerId()
                || !(context.player().containerMenu instanceof AntBrainProgramMenu menu)
                || !menu.stillValid(context.player()) || menu.ant == null) return;
        try {
            LinkedHashMap<UUID, BrainBlock> blocks = decodeAndValidate(payload.program());
            menu.ant.replaceBrainBlocks(blocks);
        } catch (RuntimeException exception) {
            LittleAnt.LOGGER.warn("Rejected invalid brain program update from {}: {}",
                    context.player().getScoreboardName(), exception.getMessage());
        }
    }

    private static LinkedHashMap<UUID, BrainBlock> decodeAndValidate(String source) {
        JsonArray array = JsonParser.parseString(source).getAsJsonArray();
        if (array.size() > 256) throw new IllegalArgumentException("too many blocks");
        LinkedHashMap<UUID, BrainBlock> result = new LinkedHashMap<>();
        for (JsonElement element : array) {
            JsonObject json = element.getAsJsonObject();
            String opcode = json.get("opcode").getAsString();
            BlockDefinition definition = ModuleRegistry.get(opcode);
            if (definition == null) throw new IllegalArgumentException("unknown opcode");
            UUID id = UUID.fromString(json.get("id").getAsString());
            if (result.containsKey(id)) throw new IllegalArgumentException("duplicate id");
            Map<String, InputDefinition> definitions = new HashMap<>();
            for (InputDefinition input : definition.inputs()) definitions.put(input.name(), input);
            Map<String, InputSlot> supplied = new HashMap<>();
            for (JsonElement inputElement : json.getAsJsonArray("inputs")) {
                JsonObject slotJson = inputElement.getAsJsonObject();
                String name = slotJson.get("name").getAsString();
                InputDefinition inputDefinition = definitions.get(name);
                if (inputDefinition == null || supplied.containsKey(name)) throw new IllegalArgumentException("invalid input");
                String value = slotJson.has("value") ? slotJson.get("value").getAsString() : null;
                if (value != null && value.length() > 256) throw new IllegalArgumentException("input too long");
                UUID block = slotJson.has("block") ? UUID.fromString(slotJson.get("block").getAsString()) : null;
                supplied.put(name, new InputSlot(name, inputDefinition.type(), value, block));
            }
            var inputs = definition.inputs().stream().map(d -> supplied.getOrDefault(d.name(),
                    InputSlot.literal(d.name(), d.type(), d.defaultValue()))).toList();
            UUID next = json.has("next") ? UUID.fromString(json.get("next").getAsString()) : null;
            UUID parent = json.has("parent") ? UUID.fromString(json.get("parent").getAsString()) : null;
            result.put(id, new BrainBlock(opcode, Mth.clamp(json.get("x").getAsInt(), 0, 4096),
                    Mth.clamp(json.get("y").getAsInt(), 0, 4096), id, inputs, next, parent));
        }
        validateReferences(result);
        return result;
    }

    private static void validateReferences(Map<UUID, BrainBlock> blocks) {
        Set<UUID> incoming = new HashSet<>();
        for (BrainBlock block : blocks.values()) {
            if (block.next() != null && (!blocks.containsKey(block.next()) || !incoming.add(block.next())))
                throw new IllegalArgumentException("invalid next link");
            if (block.parent() != null && !blocks.containsKey(block.parent())) throw new IllegalArgumentException("invalid parent");
            for (InputSlot input : block.inputs()) {
                if (input.blockId() != null && (!blocks.containsKey(input.blockId()) || !incoming.add(input.blockId())))
                    throw new IllegalArgumentException("invalid input link");
            }
        }
        Set<UUID> visiting = new HashSet<>(), visited = new HashSet<>();
        for (UUID start : blocks.keySet()) validateAcyclic(start, blocks, visiting, visited);
    }

    private static void validateAcyclic(UUID id, Map<UUID, BrainBlock> blocks,
                                        Set<UUID> visiting, Set<UUID> visited) {
        if (id == null || visited.contains(id)) return;
        if (!visiting.add(id)) throw new IllegalArgumentException("cycle");
        BrainBlock block = blocks.get(id);
        validateAcyclic(block.next(), blocks, visiting, visited);
        for (InputSlot input : block.inputs()) validateAcyclic(input.blockId(), blocks, visiting, visited);
        visiting.remove(id);
        visited.add(id);
    }
}
