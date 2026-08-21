package net.tianyang928.littleant.entity.ai.brain;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.tianyang928.littleant.entity.AntEntity;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Converts nested JSON programs back into editor graph nodes. */
public final class CodeToModuleConverter {
    public Map<UUID, BrainBlock> convert(JsonObject root) {
        if (root == null || !root.has("blocks") || !root.get("blocks").isJsonArray()) throw new IllegalArgumentException("程序必须包含 blocks 数组");
        LinkedHashMap<UUID, BrainBlock> result = new LinkedHashMap<>();
        for (JsonElement element : root.getAsJsonArray("blocks")) {
            parseChain(element, null, result, 0);
        }
        if (result.size() > 256) {
            throw new IllegalArgumentException("模块数量不能超过 256");
        }
        return result;
    }

    private UUID parseChain(JsonElement element, UUID parent, Map<UUID, BrainBlock> out, int depth) {
        if (depth > 32) {
            throw new IllegalArgumentException("模块嵌套不能超过 32 层");
        }
        if (!element.isJsonObject()) {
            throw new IllegalArgumentException("模块必须是对象");
        }
        JsonObject json = element.getAsJsonObject();
        UUID id = uuid(json, "id");
        if (json.has("ref") && json.get("ref").getAsBoolean()) {
            return id;
        }
        if (out.containsKey(id)) {
            return id;
        }
        if (out.size() >= 256) {
            throw new IllegalArgumentException("模块数量不能超过 256");
        }
        String opcode = required(json, "opcode");
        if (!ModuleRegistry.contains(opcode)) {
            throw new IllegalArgumentException("未知模块: " + opcode);
        }
        List<InputSlot> inputs = new ArrayList<>();
        if (json.has("inputs")) {
            for (Map.Entry<String, JsonElement> entry : json.getAsJsonObject("inputs").entrySet()) {
                JsonObject value = entry.getValue().getAsJsonObject();
                ValueType type = value.has("type") ? ValueType.valueOf(value.get("type").getAsString()) : ValueType.ANY;
                UUID child = value.has("block") ? parseChain(value.get("block"), id, out, depth + 1) : null;
                String literal = value.has("value") ? value.get("value").getAsString() : null;
                inputs.add(new InputSlot(entry.getKey(), type, literal, child));
            }
        }
        BrainBlock block = new BrainBlock(opcode, json.has("x") ? json.get("x").getAsInt() : 0, json.has("y") ? json.get("y").getAsInt() : 0, id, inputs, null, parent);
        out.put(id, block);
        if (json.has("next")) {
            UUID next = parseChain(json.get("next"), parent, out, depth + 1);
            out.put(id, new BrainBlock(opcode, block.x(), block.y(), id, inputs, next, parent));
        }
        return id;
    }

    private static UUID uuid(JsonObject object, String key) { if (!object.has(key)) return UUID.randomUUID(); try { return UUID.fromString(object.get(key).getAsString()); } catch (IllegalArgumentException e) { throw new IllegalArgumentException("无效 UUID: " + object.get(key)); } }
    private static String required(JsonObject object, String key) { if (!object.has(key) || !object.get(key).isJsonPrimitive()) throw new IllegalArgumentException("缺少字段: " + key); return object.get(key).getAsString(); }
}
