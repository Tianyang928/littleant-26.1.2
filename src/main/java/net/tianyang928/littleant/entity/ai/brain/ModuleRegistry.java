package net.tianyang928.littleant.entity.ai.brain;

import java.util.*;

/** Single source of truth for toolbox, validation and runtime module metadata. */
public final class ModuleRegistry {
    private static final Map<String, Integer> CATEGORY_COLORS = new LinkedHashMap<>();
    static {
        CATEGORY_COLORS.put("event", 0xFFBF00FF );
        CATEGORY_COLORS.put("behavior", 0xFF4C97FF );
        CATEGORY_COLORS.put("control", 0xFFFFAB19 );
        CATEGORY_COLORS.put("operators", 0xFF59C059 );
        CATEGORY_COLORS.put("goal", 0xFF9966FF );
        CATEGORY_COLORS.put("sense", 0xFF5CB1D6 );
        CATEGORY_COLORS.put("variables", 0xFFFF8C1A );
    }
    private static final Map<String, BlockDefinition> MODULES = new LinkedHashMap<>();
    static {
        // event
        add("tick_start", "event", BlockShape.HAT, List.of());
        add("ai_start", "event", BlockShape.HAT, List.of());
        add("start_goal", "event", BlockShape.COMMAND, List.of(new InputDefinition("goal", ValueType.TEXT, ""),new InputDefinition("priority", ValueType.NUMBER, "1"),new InputDefinition("move_flag", ValueType.BOOLEAN, "false"),new InputDefinition("look_flag", ValueType.BOOLEAN, "false"),new InputDefinition("jump_flag", ValueType.BOOLEAN, "false")));
        add("receive_goal", "event", BlockShape.HAT, List.of(new InputDefinition("goal", ValueType.TEXT, "custom_goal")));

        // behavior
        add("move_to_xyz", "behavior", BlockShape.COMMAND, blockPos("x", "y", "z"));
        add("move_to_blockpos", "behavior", BlockShape.COMMAND, List.of(new InputDefinition("blockpos", ValueType.LIST, "")));
        add("step_forward", "behavior", BlockShape.COMMAND, List.of(new InputDefinition("distance", ValueType.NUMBER, "1")));
        add("look_at_xyz", "behavior", BlockShape.COMMAND, blockPos("x", "y", "z"));
        add("look_at_blockpos", "behavior", BlockShape.COMMAND, List.of(new InputDefinition("blockpos", ValueType.LIST, "")));
        add("rotate", "behavior", BlockShape.COMMAND, List.of(new InputDefinition("angle", ValueType.NUMBER, "1")));
        add("say", "behavior", BlockShape.COMMAND, List.of(new InputDefinition("message", ValueType.TEXT, "")));
        add("switch_inventory_slot","behavior",BlockShape.COMMAND, List.of(new InputDefinition("slot", ValueType.NUMBER, "0")));
        add("jump", "behavior", BlockShape.COMMAND, List.of());
        add("set_speed", "behavior", BlockShape.COMMAND, List.of(new InputDefinition("speed", ValueType.NUMBER, "0.3")));

        // control
        add("repeat", "control", BlockShape.C_SHAPE, List.of(new InputDefinition("count", ValueType.NUMBER, "10", true), new InputDefinition("body", ValueType.BLOCK, "")));
        add("if", "control", BlockShape.C_SHAPE, List.of(new InputDefinition("condition", ValueType.BOOLEAN, "false", true), new InputDefinition("body", ValueType.BLOCK, "")));
        add("if_else", "control", BlockShape.E_SHAPE, List.of(new InputDefinition("condition", ValueType.BOOLEAN, "false", true), new InputDefinition("body_if", ValueType.BLOCK, ""),new InputDefinition("body_else", ValueType.BLOCK, "")));
        add("while", "control", BlockShape.C_SHAPE, List.of(new InputDefinition("condition", ValueType.BOOLEAN, "false", true), new InputDefinition("body", ValueType.BLOCK, "")));

        // operators
        add("add", "operators", BlockShape.REPORTER, numbers("a", "b"));
        add("subtract", "operators", BlockShape.REPORTER, numbers("a", "b"));
        add("multiply", "operators", BlockShape.REPORTER, numbers("a", "b"));
        add("divide", "operators", BlockShape.REPORTER, numbers("a", "b"));
        add("mod", "operators", BlockShape.REPORTER, numbers("a", "b"));
        add("absolute", "operators", BlockShape.REPORTER, List.of(new InputDefinition("number", ValueType.NUMBER, "0", true)));
        add("random", "operators", BlockShape.REPORTER, numbers("min", "max"));
        add("greater_than", "operators", BlockShape.BOOLEAN, numbers("a", "b"));
        add("less_than", "operators", BlockShape.BOOLEAN, numbers("a", "b"));
        add("equal", "operators", BlockShape.BOOLEAN, numbers("a", "b"));
        add("not", "operators", BlockShape.BOOLEAN, List.of(new InputDefinition("condition", ValueType.BOOLEAN, "true", true)));
        add("and", "operators", BlockShape.BOOLEAN, List.of(new InputDefinition("condition_a", ValueType.BOOLEAN, "false", true), new InputDefinition("condition_b", ValueType.BOOLEAN, "false", true)));
        add("or", "operators", BlockShape.BOOLEAN, List.of(new InputDefinition("condition_a", ValueType.BOOLEAN, "false", true), new InputDefinition("condition_b", ValueType.BOOLEAN, "false", true)));
        add("true", "operators", BlockShape.BOOLEAN, List.of());
        add("false", "operators", BlockShape.BOOLEAN, List.of());

        // goal
        add("break_block_xyz", "goal", BlockShape.REPORTER, blockPos("x", "y", "z"));
        add("break_block_blockpos", "goal", BlockShape.REPORTER, List.of(new InputDefinition("blockpos", ValueType.LIST, "")));
        add("set_block_xyz", "goal", BlockShape.REPORTER, blockPos("x", "y", "z"));
        add("set_block_blockpos", "goal", BlockShape.REPORTER, List.of(new InputDefinition("blockpos", ValueType.LIST, "")));
        add("use_crafting_table", "goal", BlockShape.REPORTER, craftItem(9));
        add("use_inventory_crafting", "goal", BlockShape.REPORTER, craftItem(4));
        add("use_barrel", "goal", BlockShape.REPORTER, List.of());
        add("use_furnace", "goal", BlockShape.REPORTER, List.of());
        add("clear_goal", "goal", BlockShape.COMMAND, List.of());
        add("finish_goal", "goal", BlockShape.COMMAND, List.of());
        add("already_has_goal", "goal", BlockShape.BOOLEAN, List.of(new InputDefinition("goal", ValueType.TEXT, "")));
        add("already_has_goal_at_priority", "goal", BlockShape.BOOLEAN, List.of(new InputDefinition("goal", ValueType.TEXT, ""), new InputDefinition("priority", ValueType.NUMBER, "1", true)));

        // sense
        add("health", "sense", BlockShape.REPORTER, List.of());
        add("food_level", "sense", BlockShape.REPORTER, List.of());
        add("x","sense", BlockShape.REPORTER, List.of());
        add("y","sense", BlockShape.REPORTER, List.of());
        add("z","sense", BlockShape.REPORTER, List.of());
        add("distance_to_xyz", "sense", BlockShape.REPORTER, blockPos("x","y","z"));
        add("distance_to_blockpos", "sense", BlockShape.REPORTER, List.of(new InputDefinition("blockpos", ValueType.LIST, "")));
        add("get_block_xyz","sense", BlockShape.REPORTER, blockPos("x","y","z"));
        add("get_block_blockpos", "sense", BlockShape.REPORTER, List.of(new InputDefinition("blockpos", ValueType.LIST, "")));
        add("has_item_in_inventory","sense", BlockShape.BOOLEAN, List.of(new InputDefinition("item", ValueType.TEXT, "minecraft:stone"), new InputDefinition("slot", ValueType.NUMBER, "0", true)));
        add("get_item_in_inventory","sense", BlockShape.REPORTER, List.of(new InputDefinition("slot", ValueType.NUMBER, "0", true)));
        add("time","sense", BlockShape.REPORTER, List.of());
        add("is_hurt","sense", BlockShape.BOOLEAN, List.of());
        add("is_on_fire","sense", BlockShape.BOOLEAN, List.of());
        add("is_in_water","sense", BlockShape.BOOLEAN, List.of());
        add("is_under_water","sense", BlockShape.BOOLEAN, List.of());
        add("last_hurt_by_entity","sense", BlockShape.REPORTER, List.of());
        add("find_block", "sense", BlockShape.REPORTER, List.of(new InputDefinition("block", ValueType.TEXT, "minecraft:stone")));
        add("find_entity", "sense", BlockShape.REPORTER, List.of(new InputDefinition("entity", ValueType.TEXT, "minecraft:pig")));
        add("find_block_entity", "sense", BlockShape.REPORTER, List.of(new InputDefinition("block_entity", ValueType.TEXT, "minecraft:chest")));
        add("find_pheromone", "sense", BlockShape.REPORTER, List.of(new InputDefinition("pheromone", ValueType.TEXT, "home")));
        add("get_surrounding_pheromone", "sense", BlockShape.REPORTER, List.of());
        add("find_nearest_block", "sense", BlockShape.REPORTER, List.of());
        add("find_nearest_entity", "sense", BlockShape.REPORTER, List.of());
        add("has_item_in_container", "sense", BlockShape.BOOLEAN, List.of(new InputDefinition("item", ValueType.TEXT, "minecraft:stone"), new InputDefinition("slot", ValueType.NUMBER, "0", true)));
        add("get_item_in_container", "sense", BlockShape.REPORTER, List.of(new InputDefinition("slot", ValueType.NUMBER, "0", true)));

        // variables
        add("set_variable", "variables", BlockShape.COMMAND, List.of(new InputDefinition("name", ValueType.TEXT, "value"), new InputDefinition("value", ValueType.ANY, "0")));
    }

    private static List<InputDefinition> numbers(String a, String b) {
        return List.of(new InputDefinition(a, ValueType.NUMBER, "0", true), new InputDefinition(b, ValueType.NUMBER, "0", true));
    }

    private static List<InputDefinition> blockPos(String x, String y, String z) {
        return List.of(new InputDefinition(x, ValueType.NUMBER, "0", true), new InputDefinition(y, ValueType.NUMBER, "0", true), new InputDefinition(z, ValueType.NUMBER, "0", true));
    }

    private static List<InputDefinition> craftItem(int slot) {
        List<InputDefinition> result = new ArrayList<>(List.of(new InputDefinition("amount", ValueType.NUMBER, "1", true)));
        result.addAll(blockPos("x", "y", "z"));
        for(int i = 0; i < slot; i++) {
            result.add(new InputDefinition("slot" + i, ValueType.TEXT, "minecraft:air", true));
        }
        return result;
    }

    private static void add(String op, String cat, BlockShape shape, List<InputDefinition> in) {
        MODULES.put(op, new BlockDefinition(op, cat, shape, in, "opcode.littleant.ant_brain." + op, CATEGORY_COLORS.get(cat)));
    }

    public static List<String> getDisplayFormat(String opcode){
        BlockDefinition def = get(opcode);
        if(def == null) {
            return List.of();
        }
        switch (opcode) {
            case "use_crafting_table" -> {
                List<String> format = new ArrayList<>();
                format.add(def.translationKey());
                format.add("amount");
                format.add("()");
                format.add("3x3");
                return format;
            }
            case "use_inventory_crafting" -> {
                List<String> format = new ArrayList<>();
                format.add(def.translationKey());
                format.add("amount");
                format.add("()");
                format.add("2x2");
                return format;
            }
            default -> {
                List<String> format = new ArrayList<>();
                format.add(def.translationKey());
                for(InputDefinition input : def.inputs()) {

                    switch(input.type()){
                        case TEXT,NUMBER,LIST,ANY:
                            format.add(input.name());
                            format.add("()");
                            break;
                        case BOOLEAN:
                            format.add(input.name());
                            format.add("<>");
                            break;
                        case BLOCK:
                            format.add(input.name());
                            format.add("{}");
                            break;
                        default:
                            format.add("unknown");
                            break;
                    }
                }
                return format;
            }
        }
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

    public static Map<String, Integer> categoryColors() {
        return Map.copyOf(CATEGORY_COLORS);
    }

    public static int categoryColor(String category) {
        return CATEGORY_COLORS.getOrDefault(category, 0xFF6B778D);
    }

    public static String categoryTranslationKey(String category) {
        return "category.littleant.ant_brain." + category;
    }

    public static List<InputSlot> createDefaultInputs(String opcode) {
        BlockDefinition definition = get(opcode);
        if (definition == null) return List.of();
        return definition.inputs().stream()
                .map(input -> InputSlot.literal(input.name(), input.type(), input.defaultValue()))
                .toList();
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
