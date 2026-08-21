package net.tianyang928.littleant.entity.ai.brain;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.tianyang928.littleant.entity.AntEntity;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Converts the editor graph into a nested JSON program representation. */
public final class ModuleToCodeConverter {
    public JsonObject convert(Map<UUID, BrainBlock> blocks) {
        JsonObject root = new JsonObject();
        root.addProperty("version", 1);
        JsonArray stacks = new JsonArray();
        Set<UUID> visited = new HashSet<>();
        Set<UUID> incoming = new HashSet<>();
        blocks.values().forEach(b -> { if (b.next() != null) incoming.add(b.next()); });
        for (BrainBlock block : blocks.values()) {
            if (block.parent() == null && !incoming.contains(block.id())) {
                stacks.add(encode(block, blocks, visited));
            }
        }
        root.add("blocks", stacks);
        return root;
    }

    private JsonObject encode(BrainBlock block, Map<UUID, BrainBlock> blocks, Set<UUID> visited) {
        JsonObject out = new JsonObject();
        out.addProperty("id", block.id().toString());
        out.addProperty("opcode", block.opcode());
        out.addProperty("x", block.x());
        out.addProperty("y", block.y());
        if (!visited.add(block.id())) {
            out.addProperty("ref", true);
            return out;
        }
        JsonObject inputs = new JsonObject();
        for (InputSlot slot : block.inputs()) {
            JsonObject value = new JsonObject();
            value.addProperty("type", slot.type().name());
            if (slot.blockId() != null && blocks.containsKey(slot.blockId())) {
                value.add("block", encode(blocks.get(slot.blockId()), blocks, visited));
            }
            else if (slot.value() != null) {
                value.addProperty("value", slot.value());
            }
            inputs.add(slot.name(), value);
        }
        out.add("inputs", inputs);
        if (block.next() != null && blocks.containsKey(block.next())) {
            out.add("next", encode(blocks.get(block.next()), blocks, visited));
        }
        return out;
    }
}
