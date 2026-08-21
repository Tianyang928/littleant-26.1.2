package net.tianyang928.littleant.entity.ai.brain;

import java.util.*;

/** Single source of truth for toolbox, validation and runtime module metadata. */
public final class ModuleRegistry {
    private static final Map<String, BlockDefinition> MODULES = new LinkedHashMap<>();
    static {
        // event
        add("tick_start", "event", BlockShape.HAT, List.of(), 0xFFBF00FF);
        add("ai_start", "event", BlockShape.HAT, List.of(), 0xFFBF00FF);
        add("start_goal", "event", BlockShape.COMMAND, List.of(new InputDefinition("goal", ValueType.TEXT, ""),new InputDefinition("priority", ValueType.NUMBER, "1"),new InputDefinition("move_flag", ValueType.BOOLEAN, "false"),new InputDefinition("look_flag", ValueType.BOOLEAN, "false"),new InputDefinition("jump_flag", ValueType.BOOLEAN, "false")), 0xFFBF00FF);
        add("receive_goal", "event", BlockShape.HAT, List.of(new InputDefinition("goal", ValueType.TEXT, "custom_goal")), 0xFF7F00FF);

        // behavior
        add("move_to_xyz", "behavior", BlockShape.COMMAND, blockPos("x", "y", "z"), 0xFF4C97FF);
        add("move_to_blockpos", "behavior", BlockShape.COMMAND, List.of(new InputDefinition("blockpos", ValueType.LIST, "")), 0xFF4C97FF);
        add("step_forward", "behavior", BlockShape.COMMAND, List.of(new InputDefinition("distance", ValueType.NUMBER, "1")), 0xFF4C97FF);
        add("look_at_xyz", "behavior", BlockShape.COMMAND, blockPos("x", "y", "z"), 0xFF4C97FF);
        add("look_at_blockpos", "behavior", BlockShape.COMMAND, List.of(new InputDefinition("blockpos", ValueType.LIST, "")), 0xFF4C97FF);
        add("rotate", "behavior", BlockShape.COMMAND, List.of(new InputDefinition("angle", ValueType.NUMBER, "1")), 0xFF4C97FF);
        add("say", "behavior", BlockShape.COMMAND, List.of(new InputDefinition("message", ValueType.TEXT, "")), 0xFF4C97FF);
        add("switch_inventory_slot","behavior",BlockShape.COMMAND, List.of(new InputDefinition("slot", ValueType.NUMBER, "0")), 0xFF4C97FF);
        add("jump", "behavior", BlockShape.COMMAND, List.of(), 0xFF4C97FF);
        add("set_speed", "behavior", BlockShape.COMMAND, List.of(new InputDefinition("speed", ValueType.NUMBER, "0.3")), 0xFF4C97FF);

        // control
        add("repeat", "control", BlockShape.C_SHAPE, List.of(new InputDefinition("count", ValueType.NUMBER, "10", true), new InputDefinition("body", ValueType.BLOCK, "")), 0xFFFFAB19);
        add("if", "control", BlockShape.C_SHAPE, List.of(new InputDefinition("condition", ValueType.BOOLEAN, "false", true), new InputDefinition("body", ValueType.BLOCK, "")), 0xFFFFAB19);
        add("if_else", "control", BlockShape.E_SHAPE, List.of(new InputDefinition("condition", ValueType.BOOLEAN, "false", true), new InputDefinition("body_if", ValueType.BLOCK, ""),new InputDefinition("body_else", ValueType.BLOCK, "")), 0xFFFFAB19);
        add("while", "control", BlockShape.C_SHAPE, List.of(new InputDefinition("condition", ValueType.BOOLEAN, "false", true), new InputDefinition("body", ValueType.BLOCK, "")), 0xFFFFAB19);

        // operators
        add("add", "operators", BlockShape.REPORTER, numbers("a", "b"), 0xFF59C059);
        add("subtract", "operators", BlockShape.REPORTER, numbers("a", "b"), 0xFF59C059);
        add("multiply", "operators", BlockShape.REPORTER, numbers("a", "b"), 0xFF59C059);
        add("divide", "operators", BlockShape.REPORTER, numbers("a", "b"), 0xFF59C059);
        add("mod", "operators", BlockShape.REPORTER, numbers("a", "b"), 0xFF59C059);
        add("absolute", "operators", BlockShape.REPORTER, List.of(new InputDefinition("number", ValueType.NUMBER, "0", true)), 0xFF59C059);
        add("random", "operators", BlockShape.REPORTER, numbers("min", "max"), 0xFF59C059);
        add("greater_than", "operators", BlockShape.BOOLEAN, numbers("a", "b"), 0xFF59C059);
        add("less_than", "operators", BlockShape.BOOLEAN, numbers("a", "b"), 0xFF59C059);
        add("equal", "operators", BlockShape.BOOLEAN, numbers("a", "b"), 0xFF59C059);
        add("not", "operators", BlockShape.BOOLEAN, List.of(new InputDefinition("condition", ValueType.BOOLEAN, "true", true)), 0xFF59C059);
        add("and", "operators", BlockShape.BOOLEAN, List.of(new InputDefinition("condition_a", ValueType.BOOLEAN, "false", true), new InputDefinition("condition_b", ValueType.BOOLEAN, "false", true)), 0xFF59C059);
        add("or", "operators", BlockShape.BOOLEAN, List.of(new InputDefinition("condition_a", ValueType.BOOLEAN, "false", true), new InputDefinition("condition_b", ValueType.BOOLEAN, "false", true)), 0xFF59C059);
        add("true", "operators", BlockShape.BOOLEAN, List.of(), 0xFF59C059);
        add("false", "operators", BlockShape.BOOLEAN, List.of(), 0xFF59C059);

        // goal
        add("break_block_xyz", "goal", BlockShape.REPORTER, blockPos("x", "y", "z"), 0xFF9966FF);
        add("break_block_blockpos", "goal", BlockShape.REPORTER, List.of(new InputDefinition("blockpos", ValueType.LIST, "")), 0xFF9966FF);
        add("set_block_xyz", "goal", BlockShape.REPORTER, blockPos("x", "y", "z"), 0xFF9966FF);
        add("set_block_blockpos", "goal", BlockShape.REPORTER, List.of(new InputDefinition("blockpos", ValueType.LIST, "")), 0xFF9966FF);
        add("use_crafting_table", "goal", BlockShape.REPORTER, craftItem(9), 0xFF9966FF);
        add("use_inventory_crafting", "goal", BlockShape.REPORTER, craftItem(4), 0xFF9966FF);
        add("use_barrel", "goal", BlockShape.REPORTER, List.of(), 0xFF9966FF);
        add("use_furnace", "goal", BlockShape.REPORTER, List.of(), 0xFF9966FF);
        add("clear_goal", "goal", BlockShape.COMMAND, List.of(), 0xFF9966FF);
        add("finish_goal", "goal", BlockShape.COMMAND, List.of(), 0xFF9966FF);
        add("already_has_goal", "goal", BlockShape.BOOLEAN, List.of(new InputDefinition("goal", ValueType.TEXT, "")), 0xFF9966FF);
        add("already_has_goal_at_priority", "goal", BlockShape.BOOLEAN, List.of(new InputDefinition("goal", ValueType.TEXT, ""), new InputDefinition("priority", ValueType.NUMBER, "1", true)), 0xFF9966FF);

        // sense
        add("health", "sense", BlockShape.REPORTER, List.of(), 0xFF5CB1D6);
        add("food_level", "sense", BlockShape.REPORTER, List.of(), 0xFF5CB1D6);
        add("x","sense", BlockShape.REPORTER, List.of(), 0xFF5CB1D6);
        add("y","sense", BlockShape.REPORTER, List.of(), 0xFF5CB1D6);
        add("z","sense", BlockShape.REPORTER, List.of(), 0xFF5CB1D6);
        add("distance_to_xyz", "sense", BlockShape.REPORTER, blockPos("x","y","z"), 0xFF5CB1D6);
        add("distance_to_blockpos", "sense", BlockShape.REPORTER, List.of(new InputDefinition("blockpos", ValueType.LIST, "")), 0xFF5CB1D6);
        add("get_block_xyz","sense", BlockShape.REPORTER, blockPos("x","y","z"), 0xFF5CB1D6);
        add("get_block_blockpos", "sense", BlockShape.REPORTER, List.of(new InputDefinition("blockpos", ValueType.LIST, "")), 0xFF5CB1D6);
        add("has_item_in_inventory","sense", BlockShape.BOOLEAN, List.of(new InputDefinition("item", ValueType.TEXT, "minecraft:stone"), new InputDefinition("slot", ValueType.NUMBER, "0", true)), 0xFF5CB1D6);
        add("get_item_in_inventory","sense", BlockShape.REPORTER, List.of(new InputDefinition("slot", ValueType.NUMBER, "0", true)), 0xFF5CB1D6);
        add("time","sense", BlockShape.REPORTER, List.of(), 0xFF5CB1D6);
        add("is_hurt","sense", BlockShape.BOOLEAN, List.of(), 0xFF5CB1D6);
        add("is_on_fire","sense", BlockShape.BOOLEAN, List.of(), 0xFF5CB1D6);
        add("is_in_water","sense", BlockShape.BOOLEAN, List.of(), 0xFF5CB1D6);
        add("is_under_water","sense", BlockShape.BOOLEAN, List.of(), 0xFF5CB1D6);
        add("last_hurt_by_entity","sense", BlockShape.REPORTER, List.of(), 0xFF5CB1D6);
        add("find_block", "sense", BlockShape.REPORTER, List.of(new InputDefinition("block", ValueType.TEXT, "minecraft:stone")), 0xFF9966FF);
        add("find_entity", "sense", BlockShape.REPORTER, List.of(new InputDefinition("entity", ValueType.TEXT, "minecraft:pig")), 0xFF9966FF);
        add("find_block_entity", "sense", BlockShape.REPORTER, List.of(new InputDefinition("block_entity", ValueType.TEXT, "minecraft:chest")), 0xFF9966FF);
        add("find_pheromone", "sense", BlockShape.REPORTER, List.of(new InputDefinition("pheromone", ValueType.TEXT, "home")), 0xFF9966FF);
        add("get_surrounding_pheromone", "sense", BlockShape.REPORTER, List.of(), 0xFF9966FF);
        add("find_nearest_block", "sense", BlockShape.REPORTER, List.of(), 0xFF9966FF);
        add("find_nearest_entity", "sense", BlockShape.REPORTER, List.of(), 0xFF9966FF);
        add("has_item_in_container", "sense", BlockShape.BOOLEAN, List.of(new InputDefinition("item", ValueType.TEXT, "minecraft:stone"), new InputDefinition("slot", ValueType.NUMBER, "0", true)), 0xFF5CB1D6);
        add("get_item_in_container", "sense", BlockShape.REPORTER, List.of(new InputDefinition("slot", ValueType.NUMBER, "0", true)), 0xFF5CB1D6);

        // variables
        add("set_variable", "variables", BlockShape.COMMAND, List.of(new InputDefinition("name", ValueType.TEXT, "value"), new InputDefinition("value", ValueType.ANY, "0")), 0xFFFF8C1A);
    }

    private static List<InputDefinition> numbers(String a, String b) {
        return List.of(new InputDefinition(a, ValueType.NUMBER, "0", true), new InputDefinition(b, ValueType.NUMBER, "0", true));
    }

    private static List<InputDefinition> blockPos(String x, String y, String z) {
        return List.of(new InputDefinition(x, ValueType.NUMBER, "0", true), new InputDefinition(y, ValueType.NUMBER, "0", true), new InputDefinition(z, ValueType.NUMBER, "0", true));
    }

    private static List<InputDefinition> craftItem(int slot) {
        List<InputDefinition> result = new ArrayList<>(blockPos("x", "y", "z"));
        for(int i = 0; i < slot; i++) {
            result.add(new InputDefinition("slot" + i, ValueType.TEXT, "minecraft:air", true));
        }
        return result;
    }

    private static void add(String op, String cat, BlockShape shape, List<InputDefinition> in, int color) {
        MODULES.put(op, new BlockDefinition(op, cat, shape, in, "block.littleant.ant_brain." + op, color));
    }

    public static BlockDefinition get(String opcode) {
        return MODULES.get(opcode);
    }

    public static boolean contains(String opcode) {
        return MODULES.containsKey(opcode);
    }

    public static Collection<BlockDefinition> all() {
        return List.copyOf(MODULES.values());
    }

    public static Map<String, List<BlockDefinition>> byCategory() {
        Map<String, List<BlockDefinition>> out = new LinkedHashMap<>();
        for (BlockDefinition d : MODULES.values())
            out.computeIfAbsent(d.category(), k -> new java.util.ArrayList<>()).add(d);
        return out;
    }

    private ModuleRegistry() {
    }
}
