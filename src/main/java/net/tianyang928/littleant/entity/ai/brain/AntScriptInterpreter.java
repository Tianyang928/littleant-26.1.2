package net.tianyang928.littleant.entity.ai.brain;

import net.minecraft.world.entity.ai.goal.Goal;
import net.tianyang928.littleant.LittleAnt;
import net.tianyang928.littleant.entity.AntEntity;
import net.tianyang928.littleant.entity.ai.sense.FindBlock;

import java.util.*;

/**
 * Small, deterministic interpreter for Python-like command scripts; no Python runtime is required.
 */
public final class AntScriptInterpreter {

    private Map<UUID, BrainBlock> blocks;
    private final LinkedHashSet<UUID> aiStarts = new LinkedHashSet<>();
    private final LinkedHashSet<UUID> tickStarts = new LinkedHashSet<>();
    private final Map<String, List<UUID>> receiveGoalRoots = new LinkedHashMap<>();
    private final AntEntity ant;
    private boolean isRunning = false;
    private int loadedSignature;
    private boolean executingCustomGoal;
    private boolean customGoalFinished;
    private final AntGoalScheduler goalScheduler;
    private final AntBlackboard blackboard;

    public AntScriptInterpreter(AntEntity ant) {
        this.ant = ant;
        this.goalScheduler = new AntGoalScheduler(ant);
        this.blackboard = new AntBlackboard(ant);
    }

    public void loadProgram(Map<UUID, BrainBlock> program) {
        int signature = program.hashCode();
        if (loadedSignature == signature) {
            return;
        }
        loadedSignature = signature;
        blocks = Map.copyOf(program);
        aiStarts.clear();
        tickStarts.clear();
        receiveGoalRoots.clear();
        goalScheduler.clear();
        Set<UUID> incoming = new HashSet<>();
        blocks.values().forEach(b -> {
            if (b.next() != null) incoming.add(b.next());
        });
        blocks.values().stream().filter(b -> b.parent() == null && !incoming.contains(b.id()) && (b.opcode().equals("ai_start"))).map(BrainBlock::id).forEach(aiStarts::add);
        blocks.values().stream().filter(b -> b.parent() == null && !incoming.contains(b.id()) && (b.opcode().equals("tick_start"))).map(BrainBlock::id).forEach(tickStarts::add);
        blocks.values().stream()
                .filter(b -> b.parent() == null && !incoming.contains(b.id()) && b.opcode().equals("receive_goal"))
                .forEach(b -> receiveGoalRoots.computeIfAbsent(inputNumber(b, "goal", "", blocks), ignored -> new ArrayList<>()).add(b.id()));
        isRunning = !aiStarts.isEmpty() || !tickStarts.isEmpty() || !receiveGoalRoots.isEmpty();


        ant.needAiRestart = true;
    }

    public void start() {
        if (!isRunning || blocks.isEmpty()) {
            return;
        }
        for (UUID id : aiStarts) {
            BrainBlock block = blocks.get(id);
            if (block == null) {
                continue;
            }
            executeBlock(block, new HashSet<>());
        }
    }

    public void tick() {
        if (!isRunning || blocks.isEmpty()) {
            return;
        }
        for (UUID id : tickStarts) {
            BrainBlock block = blocks.get(id);
            if (block == null) {
                continue;
            }
            executeBlock(block, new HashSet<>());
        }
        goalScheduler.tick(this::tickCustomGoal);
    }

    public AntBlackboard blackboard() {
        return blackboard;
    }

    private boolean tickCustomGoal(List<UUID> roots) {
        executingCustomGoal = true;
        customGoalFinished = false;
        for (UUID root : roots) {
            BrainBlock hat = blocks.get(root);
            if (hat != null && hat.next() != null && blocks.containsKey(hat.next())) {
                executeBlock(blocks.get(hat.next()), new HashSet<>());
            }
            if (customGoalFinished) break;
        }
        executingCustomGoal = false;
        return customGoalFinished;
    }

    private void executeBlock(BrainBlock block, Set<UUID> active) {
        if (executingCustomGoal && customGoalFinished) {
            return;
        }
//        if (!active.add(block.id())) {
//            return;
//        }
        switch (block.opcode()) {
            case "ai_start", "tick_start", "receive_goal" -> {}
            case "start_goal" -> {
                List<String> all_param;

                double priority;
                try {
                    all_param = Arrays.stream(inputNumber(block, "goal", "", blocks).split(",", -1)).map(String::trim).toList();
                    if (all_param.isEmpty() || all_param.getFirst().isBlank()) {
                        break;
                    }
                    priority = Double.parseDouble(inputNumber(block, "priority", "", blocks));
                } catch (RuntimeException e) {
                    break;
                }
                EnumSet<Goal.Flag> flags = EnumSet.noneOf(Goal.Flag.class);
                if (inputBoolean(block, "move_flag", false, blocks)) {
                    flags.add(Goal.Flag.MOVE);
                }
                if (inputBoolean(block, "look_flag", false, blocks)) {
                    flags.add(Goal.Flag.LOOK);
                }
                if (inputBoolean(block, "jump_flag", false, blocks)) {
                    flags.add(Goal.Flag.JUMP);
                }
                LittleAnt.LOGGER.info("[AntScriptInterpreter] start_goal flags {}", flags);
                goalScheduler.submit(block.id(), all_param.getFirst(), priority, flags, all_param.subList(1, all_param.size()), blocks,
                        //允许多个同名goal模组（gui内的）同时被激活
                        receiveGoalRoots.getOrDefault(all_param.getFirst(), List.of()));
                LittleAnt.LOGGER.info("[AntScriptInterpreter] start_goal name {}, receiveGoalRootsList {}",all_param.getFirst(),receiveGoalRoots.getOrDefault(all_param.getFirst(), List.of()));
            }
            case "clear_goal" -> goalScheduler.clear();
            case "finish_goal" -> { if (executingCustomGoal) customGoalFinished = true; }
            case "move_to_xyz" -> {
                try {
                    blackboard.scriptMoveTo(Double.parseDouble(inputNumber(block, "x", "0", blocks)),
                                    Double.parseDouble(inputNumber(block, "y", "0", blocks)),
                                    Double.parseDouble(inputNumber(block, "z", "0", blocks)));
                } catch (RuntimeException e) {
                    break;
                }
            }
            case "step_forward" -> {
                try {
                    blackboard.scriptStepForward(Double.parseDouble(inputNumber(block, "distance", "0", blocks)));
                } catch (RuntimeException e) {
                    break;
                }
            }
            case "look_at_xyz" -> {
                try {
                    blackboard.scriptLookAt(Double.parseDouble(inputNumber(block, "x", "0", blocks)),
                                    Double.parseDouble(inputNumber(block, "y", "0", blocks)),
                                    Double.parseDouble(inputNumber(block, "z", "0", blocks)));
                } catch (RuntimeException e) {
                    break;
                }
            }
            case "rotate" -> {
                try {
                    blackboard.scriptRotate(Double.parseDouble(inputNumber(block, "angle", "0", blocks)));
                } catch (RuntimeException e) {
                    break;
                }
            }
            case "say" -> {
                blackboard.scriptSay(inputNumber(block, "message", "0", blocks));
            }
            case "set_speed" -> {
                try {
                    blackboard.scriptSetSpeed(Double.parseDouble(inputNumber(block, "speed", "0", blocks)));
                } catch (RuntimeException e) {
                    break;
                }
            }
            case "repeat" -> {
                try {
                    int count = Math.max(0, (int) Double.parseDouble(inputNumber(block, "count", "0", blocks)));
                    InputSlot body = input(block, "body");
                    if (body != null && body.blockId() != null && blocks.containsKey(body.blockId())) {
                        for (int i = 0; i < count; i++) {
                            executeBlock(blocks.get(body.blockId()), active);
                        }
                    }
                } catch (RuntimeException e) {
                    break;
                }

            }
            case "if" -> {
                InputSlot body = input(block, "body");
                boolean condition = inputBoolean(block, "condition", false, blocks);
                if (condition && body != null && blocks.containsKey(body.blockId())) {
                    executeBlock(blocks.get(body.blockId()), active);
                }
            }
            case "if_else" -> {
                InputSlot body_if = input(block, "body_if");
                InputSlot body_else = input(block, "body_else");
                boolean condition = inputBoolean(block, "condition", false, blocks);
                if (condition && body_if != null && blocks.containsKey(body_if.blockId())) {
                    executeBlock(blocks.get(body_if.blockId()), active);
                } else if (!condition && body_else != null && blocks.containsKey(body_else.blockId())) {
                    executeBlock(blocks.get(body_else.blockId()), active);
                }
            }
            case "while" -> {
                InputSlot body = input(block, "body");
                int repeatTimes = 0;
                while (inputBoolean(block, "condition", false, blocks)) {
                    if (body != null && body.blockId() != null && blocks.containsKey(body.blockId())) {
                        executeBlock(blocks.get(body.blockId()), active);
                    }
                    repeatTimes++;
                    if(repeatTimes > 1000){
                        break;
                    }
                }
            }
            case "set_variable" -> {
                String name = inputNumber(block,"name","0",blocks);
                if(name.isEmpty()){
                    break;
                }
                blackboard.setVariable(name, inputNumber(block,"value","0",blocks));
            }
            default -> {}
        }
        if ((!executingCustomGoal || !customGoalFinished) && block.next() != null && blocks.containsKey(block.next())) {
            executeBlock(blocks.get(block.next()), active);
        }
    }

    // 用于获取特定名称的block成员
    private InputSlot input(BrainBlock block, String name) {
        return block.inputs().stream().filter(i -> i.name().equals(name)).findFirst().orElse(null);
    }

    // inputNumber 用于层级结构的判断，  reporterModule 用于计算结果
    private String inputNumber(BrainBlock block, String name, String fallback, Map<UUID, BrainBlock> blocks) {
        return inputNumber(block, name, fallback, blocks, new HashSet<>());
    }

    private String inputNumber(BrainBlock block, String name, String fallback, Map<UUID, BrainBlock> blocks, Set<UUID> active) {
        InputSlot slot = input(block, name);
        if (slot == null) {
            return fallback;
        }
        if (slot.blockId() != null && blocks.containsKey(slot.blockId())) {
            return reporterModule(blocks.get(slot.blockId()), blocks, active);
        }
        return slot.value();
    }

    private String reporterModule(BrainBlock block, Map<UUID, BrainBlock> blocks, Set<UUID> active) {
        // 防止循环引用，如果当前block已经被计算，直接返回0，避免无限递归
//        if (!active.add(block.id())) {
//            return "0";
//        }
        switch (block.opcode()) {
            case "add" -> {
                try {
                    double a = Double.parseDouble(inputNumber(block, "a", "0", blocks, active));
                    double b = Double.parseDouble(inputNumber(block, "b", "0", blocks, active));
                    return String.valueOf(a + b);
                } catch (RuntimeException e) {
                    return "0";
                }
            }
            case "subtract" -> {
                try {
                    double a = Double.parseDouble(inputNumber(block, "a", "0", blocks, active));
                    double b = Double.parseDouble(inputNumber(block, "b", "0", blocks, active));
                    return String.valueOf(a - b);
                } catch (RuntimeException e) {
                    return "0";
                }
            }
            case "multiply" -> {
                try {
                    double a = Double.parseDouble(inputNumber(block, "a", "0", blocks, active));
                    double b = Double.parseDouble(inputNumber(block, "b", "0", blocks, active));
                    return String.valueOf(a * b);
                } catch (RuntimeException e) {
                    return "0";
                }
            }
            case "divide" -> {
                try {
                    double a = Double.parseDouble(inputNumber(block, "a", "0", blocks, active));
                    double b = Double.parseDouble(inputNumber(block, "b", "0", blocks, active));
                    return String.valueOf(b == 0 ? 0 : a / b);
                } catch (RuntimeException e) {
                    return "0";
                }
            }
            case "mod" -> {
                try {
                    double a = Double.parseDouble(inputNumber(block, "a", "0", blocks, active));
                    double b = Double.parseDouble(inputNumber(block, "b", "0", blocks, active));
                    return String.valueOf(b == 0 ? 0 : a % b);
                } catch (RuntimeException e) {
                    return "0";
                }
            }
            case "absolute" -> {
                try {
                    double a = Double.parseDouble(inputNumber(block, "number", "0", blocks, active));
                    return String.valueOf(Math.abs(a));
                } catch (RuntimeException e) {
                    return "0";
                }
            }
            case "random" -> {
                try {
                    double a = Double.parseDouble(inputNumber(block, "min", "0", blocks, active));
                    double b = Double.parseDouble(inputNumber(block, "max", "0", blocks, active));
                    return String.valueOf(Math.random() * (b - a) + a);
                } catch (RuntimeException e) {
                    return "0";
                }
            }

            // goals
            case "break_block_xyz" -> {
                String x = inputNumber(block, "x", "0", blocks, active);
                String y = inputNumber(block, "y", "0", blocks, active);
                String z = inputNumber(block, "z", "0", blocks, active);
                return "break_block" + "," + x + "," + y + "," + z;
            }
            case "set_block_xyz" -> {
                String x = inputNumber(block, "x", "0", blocks, active);
                String y = inputNumber(block, "y", "0", blocks, active);
                String z = inputNumber(block, "z", "0", blocks, active);
                return "set_block" + "," + x + "," + y + "," + z;
            }
            case "break_block_blockpos" -> {
                String blockpos = inputNumber(block, "blockpos", "0", blocks, active);
                if(blockpos.split(",").length != 3) {
                    return "";
                }
                return "break_block" + "," + blockpos;
            }
            case "set_block_blockpos" -> {
                String blockpos = inputNumber(block, "blockpos", "0", blocks, active);
                if(blockpos.split(",").length != 3) {
                    return "";
                }
                return "set_block" + "," + blockpos;
            }
            case "better_float" -> { return "better_float"; }
            case "use_container_xyz" -> {
                return String.join(",", "use_container",
                                    inputNumber(block, "x", "0", blocks, active),
                                    inputNumber(block, "y", "0", blocks, active),
                                    inputNumber(block, "z", "0", blocks, active),
                                    String.valueOf(inputBoolean(block, "put_in", true, blocks, active)),
                                    inputNumber(block, "item", "minecraft:stone", blocks, active),
                                    inputNumber(block, "slot", "0", blocks, active),
                                    inputNumber(block, "amount", "1", blocks, active));
            }
            case "use_container_blockpos" -> {
                String blockpos = inputNumber(block, "blockpos", "0", blocks, active);
                if(blockpos.split(",").length != 3) {
                    return "";
                }
                return String.join(",", "use_container",
                        blockpos,
                        String.valueOf(inputBoolean(block, "put_in", true, blocks, active)),
                        inputNumber(block, "item", "minecraft:stone", blocks, active),
                        inputNumber(block, "slot", "0", blocks, active),
                        inputNumber(block, "amount", "1", blocks, active));
            }
            case "melee_attack" -> {
                return String.join(",", "melee_attack", inputNumber(block, "target", "-1", blocks, active));
            }
            case "use_crafting_table_xyz", "use_inventory_crafting", "use_crafting_table_blockpos" -> {
                StringBuilder result = new StringBuilder(block.opcode()).append(',').append(inputNumber(block, "amount", "1", blocks, active));
                if (block.opcode().equals("use_crafting_table_xyz")) {
                    result.append(',').append(inputNumber(block, "x", "0", blocks, active)).append(',').append(inputNumber(block, "y", "0", blocks, active)).append(',').append(inputNumber(block, "z", "0", blocks, active));
                }
                else if(block.opcode().equals("use_crafting_table_blockpos")) {
                    String blockpos = inputNumber(block, "blockpos", "0", blocks, active);
                    if(blockpos.split(",").length != 3) {
                        return "";
                    }
                    result.append(',').append(blockpos);
                }
                int slots = block.opcode().equals("use_crafting_table_xyz") ? 9 : 4;
                for (int i = 0; i < slots; i++) result.append(',').append(inputNumber(block, "slot" + i, "minecraft:air", blocks, active));
                return result.toString();
            }
            default -> {
                return "";
            }
            // sense
            case "health" -> {
                return blackboard.getHealth();
            }
            case "food_level" -> {
                return blackboard.getFoodLevel();
            }
            case "distance_to_xyz" -> {
                try{
                    String x = inputNumber(block, "x", "0", blocks, active);
                    String y = inputNumber(block, "y", "0", blocks, active);
                    String z = inputNumber(block, "z", "0", blocks, active);
                    return blackboard.distanceToTarget(Double.parseDouble(x), Double.parseDouble(y), Double.parseDouble(z));
                } catch (RuntimeException e) {
                    return "-1";
                }
            }
            case "distance_to_blockpos" -> {
                String blockpos = inputNumber(block, "blockpos", "0", blocks, active);
                String[] posList = blockpos.split(",");
                if(posList.length != 3){
                    return "-1";
                }
                try{
                    return blackboard.distanceToTarget(Double.parseDouble(posList[0]), Double.parseDouble(posList[1]), Double.parseDouble(posList[2]));
                } catch (RuntimeException e) {
                    return "-1";
                }
            }
            case "get_block_xyz" -> {
                try{
                    String x = inputNumber(block, "x", "0", blocks, active);
                    String y = inputNumber(block, "y", "0", blocks, active);
                    String z = inputNumber(block, "z", "0", blocks, active);
                    return blackboard.getBlock(Double.parseDouble(x), Double.parseDouble(y), Double.parseDouble(z));
                } catch (RuntimeException e) {
                    return "";
                }
            }
            case "get_block_blockpos" -> {
                String blockpos = inputNumber(block, "blockpos", "0", blocks, active);
                String[] posList = blockpos.split(",");
                if(posList.length != 3){
                    return "";
                }
                try{
                    return blackboard.getBlock(Double.parseDouble(posList[0]), Double.parseDouble(posList[1]), Double.parseDouble(posList[2]));
                } catch (RuntimeException e) {
                    return "";
                }
            }
            case "x" -> {
                return blackboard.getX();
            }
            case "y" -> {
                return blackboard.getY();
            }
            case "z" -> {
                return blackboard.getZ();
            }
            case "pos" -> {
                return blackboard.getPos();
            }
            case "get_item_in_inventory" -> {
                try {
                    double slot = Double.parseDouble(inputNumber(block, "slot", "0", blocks, active));
                    return blackboard.getItemInInventory(slot);
                }
                catch (RuntimeException e){
                    return "";
                }
            }
            case "time" -> {
                return blackboard.getTime();
            }
            case "last_hurt_by_entity" -> {
                return blackboard.getLastHurtByEntity();
            }
            case "find_block" -> {
                String selectedBlock = inputNumber(block,"block","0",blocks,active);
                return blackboard.findBlock(selectedBlock);
            }
            case "find_entity" -> {
                String selectedEntity = inputNumber(block,"entity","0",blocks,active);
                return blackboard.findEntity(selectedEntity);
            }
            case "find_block_entity" -> {
                String selectedBlockEntity = inputNumber(block,"block_entity","0",blocks,active);
                return blackboard.findBlockEntity(selectedBlockEntity);
            }
            case "get_speed" -> {
                return blackboard.getSpeed();
            }

            // variables
            case "get_variable" -> {
                return blackboard.getVariable(inputNumber(block,"name","0",blocks,active));
            }
        }
    }

    private boolean inputBoolean(BrainBlock block, String name, boolean fallback, Map<UUID, BrainBlock> blocks) {
        return inputBoolean(block, name, fallback, blocks, new HashSet<>());
    }

    private boolean inputBoolean(BrainBlock block, String name, boolean fallback, Map<UUID, BrainBlock> blocks, Set<UUID> active) {
        InputSlot slot = input(block, name);
        if (slot == null) {
            return fallback;
        }
        if (slot.blockId() != null && blocks.containsKey(slot.blockId())) {
            return booleanModule(blocks.get(slot.blockId()), blocks, active);
        }
        try {
            return Boolean.parseBoolean(slot.value());
        } catch (RuntimeException e) {
            return fallback;
        }
    }

    private boolean booleanModule(BrainBlock block, Map<UUID, BrainBlock> blocks, Set<UUID> active) {
//        if (!active.add(block.id())) {
//            return false;
//        }
        switch (block.opcode()) {
            case "greater_than" -> {
                String a = inputNumber(block, "a", "0", blocks, active);
                String b = inputNumber(block, "b", "0", blocks, active);

                try {
                    double aNum = Double.parseDouble(a);
                    double bNum = Double.parseDouble(b);
                    return aNum > bNum;
                } catch (RuntimeException e) {
                    return a.compareTo(b) > 0;
                }
            }
            case "less_than" -> {
                String a = inputNumber(block, "a", "0", blocks, active);
                String b = inputNumber(block, "b", "0", blocks, active);

                try {
                    double aNum = Double.parseDouble(a);
                    double bNum = Double.parseDouble(b);
                    return aNum < bNum;
                } catch (RuntimeException e) {
                    return a.compareTo(b) < 0;
                }
            }
            case "equal" -> {
                String a = inputNumber(block, "a", "0", blocks, active);
                String b = inputNumber(block, "b", "0", blocks, active);

                try {
                    double aNum = Double.parseDouble(a);
                    double bNum = Double.parseDouble(b);
                    return aNum == bNum;
                } catch (RuntimeException e) {
                    return a.compareTo(b) == 0;
                }
            }
            case "not" -> {
                boolean a = inputBoolean(block, "condition", false, blocks, active);
                return !a;
            }
            case "and" -> {
                boolean a = inputBoolean(block, "condition_a", false, blocks, active);
                boolean b = inputBoolean(block, "condition_b", false, blocks, active);
                return a && b;
            }
            case "or" -> {
                boolean a = inputBoolean(block, "condition_a", false, blocks, active);
                boolean b = inputBoolean(block, "condition_b", false, blocks, active);
                return a || b;
            }
            case "true" -> {
                return true;
            }
            case "false" -> {
                return false;
            }

            // sense
            case "has_item_in_inventory" -> {
                String item = inputNumber(block, "item", "0", blocks, active);
                return blackboard.hasItemInInventory(item);
            }
            case "is_hurt" -> {
                return blackboard.isHurt();
            }
            case "is_on_fire" -> {
                return blackboard.isOnFire();
            }
            case "is_in_water" -> {
                return blackboard.isInWater();
            }
            case "is_under_water" -> {
                return blackboard.isUnderWater();
            }


            default -> {
                return false;
            }
        }
    }
}
