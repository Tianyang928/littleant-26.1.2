package net.tianyang928.littleant.entity.ai.brain;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;
import net.tianyang928.littleant.LittleAnt;
import net.tianyang928.littleant.entity.AntEntity;
import net.tianyang928.littleant.entity.ai.debug.TaskDebugState;
import net.tianyang928.littleant.entity.ai.sense.FindBlock;

import java.util.*;
import net.tianyang928.littleant.entity.ai.debug.TaskDebugEntry;

/**
 * Small, deterministic interpreter for Python-like command scripts; no Python runtime is required.
 */
public final class AntScriptInterpreter {

    private Map<UUID, BrainBlock> blocks;
    private final LinkedHashSet<UUID> aiStarts = new LinkedHashSet<>();
    private final LinkedHashSet<UUID> tickStarts = new LinkedHashSet<>();
    private final Map<String, List<UUID>> receiveGoalRoots = new LinkedHashMap<>();
    private final Map<String, List<UUID>> goalTickRoots = new LinkedHashMap<>();
    private final AntEntity ant;
    private boolean isRunning = false;
    private int loadedSignature;
    private boolean executingCustomGoal;
    private boolean customGoalFinished;
    private AntGoalScheduler.Task currentTask;
    private final AntGoalScheduler goalScheduler;
    private final AntBlackboard blackboard;

    public AntScriptInterpreter(AntEntity ant) {
        this.ant = ant;
        this.goalScheduler = new AntGoalScheduler(ant);
        this.blackboard = new AntBlackboard(ant);
    }

    public void loadProgram(Map<UUID, BrainBlock> program) {
        int signature = program.hashCode();
        if (loadedSignature == signature && !ant.needAiRestart) {
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
        blocks.values().stream()
                .filter(b -> b.parent() == null && !incoming.contains(b.id()) && b.opcode().equals("goal_tick_start"))
                .forEach(b -> goalTickRoots.computeIfAbsent(inputNumber(b, "goal", "", blocks), ignored -> new ArrayList<>()).add(b.id()));
        isRunning = !aiStarts.isEmpty() || !tickStarts.isEmpty() || !receiveGoalRoots.isEmpty() || !goalTickRoots.isEmpty();


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

    public List<TaskDebugEntry> debugForeground() { return goalScheduler.debugForeground(); }
    public List<TaskDebugEntry> debugBackground() { return goalScheduler.debugBackground(); }

    private boolean tickCustomGoal(AntGoalScheduler.Task task, List<UUID> startRoots, List<UUID> tickRoots) {
        if(task instanceof AntGoalScheduler.CustomTask customTask) {
            executingCustomGoal = true;
            customGoalFinished = false;
            currentTask = customTask;
            if (!customTask.started) {
                customTask.started = true;
                for (UUID root : startRoots) {
                    BrainBlock hat = blocks.get(root);
                    if (hat != null && hat.next() != null && blocks.containsKey(hat.next())) {
                        executeBlock(blocks.get(hat.next()), new HashSet<>());
                    }
                    if (customGoalFinished) break;
                }
            }
            customGoalFinished = (customTask.started && tickRoots.isEmpty()) || customGoalFinished;
            for (UUID root : tickRoots) {
                BrainBlock hat = blocks.get(root);
                if (hat != null && hat.next() != null && blocks.containsKey(hat.next())) {
                    executeBlock(blocks.get(hat.next()), new HashSet<>());
                }
                if (customGoalFinished) break;
            }

            currentTask = null;
            executingCustomGoal = false;
            return customGoalFinished;
        }
        return true;
    }

    private void executeBlock(BrainBlock block, Set<UUID> active) {
        if (executingCustomGoal && customGoalFinished) {
            return;
        }
//        if (!active.add(block.id())) {
//            return;
//        }
        switch (block.opcode()) {
            case "ai_start", "tick_start", "receive_goal", "goal_tick_start" -> {}
            case "submit_foreground_goal", "submit_background_goal" -> {
                List<String> all_param;
                try {
                    all_param = Arrays.stream(inputNumber(block, "goal", "", blocks).split(",", -1)).map(String::trim).toList();
                    if (all_param.isEmpty() || all_param.getFirst().isBlank()) {
                        break;
                    }
                } catch (RuntimeException e) {
                    break;
                }
                if (block.opcode().equals("submit_foreground_goal")) {
                    goalScheduler.submitForeground(block.id(), all_param.getFirst(), all_param.subList(1, all_param.size()),
                            receiveGoalRoots.getOrDefault(all_param.getFirst(), List.of()), goalTickRoots.getOrDefault(all_param.getFirst(), List.of()), currentTask);
                    break;
                }

                double priority;
                try {
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
                goalScheduler.submitBackground(block.id(), all_param.getFirst(), priority, flags, all_param.subList(1, all_param.size()),
                        receiveGoalRoots.getOrDefault(all_param.getFirst(), List.of()), goalTickRoots.getOrDefault(all_param.getFirst(), List.of()), currentTask);
            }
            case "clear_goal" -> goalScheduler.clear();
            case "finish_current_goal" -> {
                if (executingCustomGoal) customGoalFinished = true;
                if (currentTask == null) break;
                LittleAnt.LOGGER.info("[AntGoalScheduler] Request immediate finish: {}", currentTask.name);
                goalScheduler.requestFinishCurrentGoal(currentTask);
            }
            case "finish_current_goal_delay" -> {
                if (executingCustomGoal) customGoalFinished = true;
                if (currentTask == null) break;
                LittleAnt.LOGGER.info("[AntGoalScheduler] Request finish after foreground: {}", currentTask.name);
                goalScheduler.requestFinishCurrentGoalDelay(currentTask);
            }
            case "move_to_xyz" -> {
                try {
                    goalScheduler.submitMoveTo(block.id(), new BlockPos(
                            (int) Double.parseDouble(inputNumber(block, "x", "0", blocks)),
                            (int) Double.parseDouble(inputNumber(block, "y", "0", blocks)),
                            (int) Double.parseDouble(inputNumber(block, "z", "0", blocks))), currentTask);
                } catch (RuntimeException e) {
                    break;
                }
            }
            case "move_to_blockpos" -> {
                try {
                    String[] coordinates = inputNumber(block, "blockpos", "", blocks).split(",", -1);
                    if (coordinates.length != 3) break;
                    goalScheduler.submitMoveTo(block.id(), new BlockPos(
                            (int) Double.parseDouble(coordinates[0].trim()),
                            (int) Double.parseDouble(coordinates[1].trim()),
                            (int) Double.parseDouble(coordinates[2].trim())), currentTask);
                } catch (RuntimeException e) {
                    break;
                }
            }
            case "step_forward" -> {
                try {
                    double distance = Double.parseDouble(inputNumber(block, "distance", "0", blocks));
                    goalScheduler.submitStepForward(block.id(), distance, currentTask);
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
            case "switch_inventory_slot" -> {
                try {
                    blackboard.scriptSwitchInventorySlot((int)Double.parseDouble(inputNumber(block, "slot", "0", blocks)));
                } catch (RuntimeException e) {
                    break;
                }
            }
            case "jump" -> {
                try {
                    blackboard.scriptJump();
                } catch (RuntimeException e) {
                    break;
                }
            }
            case "set_run" -> {
                try {
                    blackboard.scriptSetRun(inputBoolean(block, "run", false, blocks));
                } catch (RuntimeException e) {
                    break;
                }
            }
            case "set_crouching" -> {
                try {
                    blackboard.scriptSetCrouching(inputBoolean(block, "crouching", false, blocks));
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
                    LittleAnt.LOGGER.info("[AntScriptInterpreter]: module [repeat] exception: {}", e.toString());
                }

            }
            case "if" -> {
                try {
                    InputSlot body = input(block, "body");
                    boolean condition = inputBoolean(block, "condition", false, blocks);
                    if (condition && body != null && blocks.containsKey(body.blockId())) {
                        executeBlock(blocks.get(body.blockId()), active);
                    }
                } catch (RuntimeException e) {
                    e.printStackTrace();
                    LittleAnt.LOGGER.info("[AntScriptInterpreter]: module [if] exception: {}", e.toString());
                }
            }
            case "if_else" -> {
                try {
                    InputSlot body_if = input(block, "body_if");
                    InputSlot body_else = input(block, "body_else");
                    boolean condition = inputBoolean(block, "condition", false, blocks);
                    if (condition && body_if != null && blocks.containsKey(body_if.blockId())) {
                        executeBlock(blocks.get(body_if.blockId()), active);
                    } else if (!condition && body_else != null && blocks.containsKey(body_else.blockId())) {
                        executeBlock(blocks.get(body_else.blockId()), active);
                    }
                } catch (RuntimeException e) {
                    e.printStackTrace();
                    LittleAnt.LOGGER.info("[AntScriptInterpreter]: module [if-else] exception: {}", e.toString());
                }
            }
            case "while" -> {
                try {
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
                } catch (RuntimeException e) {
                    e.printStackTrace();
                    LittleAnt.LOGGER.info("[AntScriptInterpreter]: module [while] exception: {}", e.toString());
                }
            }
            case "set_variable" -> {
                String name = inputNumber(block,"name","0",blocks);
                if(name.isEmpty()){
                    break;
                }
                blackboard.setVariable(name, inputNumber(block,"value","0",blocks));
            }
            case "set_variable_permanent" -> {
                String name = inputNumber(block, "name", "", blocks);
                if (!name.isEmpty()) {
                    blackboard.setVariablePermanent(name);
                }
            }
            case "new_list" -> {
                String name = inputNumber(block, "name", "", blocks);
                if (!name.isEmpty()) {
                    blackboard.newList(name);
                }
            }
            case "add_list" -> {
                String name = inputNumber(block, "name", "", blocks);
                String list = inputNumber(block, "list", "", blocks);
                blackboard.addList(name,list);
            }
            case "add_value" -> {
                String name = inputNumber(block, "name", "", blocks);
                String value = inputNumber(block, "value", "", blocks);
                blackboard.addValue(name,value);
            }
            case "set_list_kv" -> {
                String name = inputNumber(block, "name", "", blocks);
                try {
                    int key = (int) Double.parseDouble(inputNumber(block, "key", "-1", blocks));
                    blackboard.setListValue(name, key, inputNumber(block, "value", "", blocks));
                } catch (RuntimeException ignored) {
                }
            }
            case "set_list_list" -> {
                String name = inputNumber(block, "name", "", blocks);
                String list = inputNumber(block, "list", "", blocks);
                blackboard.setWholeList(name,list);
            }
            case "set_list_permanent" -> {
                String name = inputNumber(block, "name", "", blocks);
                if (!name.isEmpty()) {
                    blackboard.setListPermanent(name);
                }
            }
            case "clear_list" -> {
                String name = inputNumber(block, "name", "", blocks);
                if (!name.isEmpty()) {
                    blackboard.clearList(name);
                }
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
            case "join_string_list" -> {
                String[] strings = inputNumber(block, "strings", "", blocks, active).split(",", -1);
                return String.join("", strings);
            }
            case "join_string_str" -> {
                return inputNumber(block, "string1", "", blocks, active)
                        + inputNumber(block, "string2", "", blocks, active);
            }
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
                return "vanilla:break_block" + "," + x + "," + y + "," + z;
            }
            case "set_block_xyz" -> {
                String x = inputNumber(block, "x", "0", blocks, active);
                String y = inputNumber(block, "y", "0", blocks, active);
                String z = inputNumber(block, "z", "0", blocks, active);
                return "vanilla:set_block" + "," + x + "," + y + "," + z;
            }
            case "break_block_blockpos" -> {
                String blockpos = inputNumber(block, "blockpos", "0", blocks, active);
                if(blockpos.split(",").length != 3) {
                    return "";
                }
                return "vanilla:break_block" + "," + blockpos;
            }
            case "set_block_blockpos" -> {
                String blockpos = inputNumber(block, "blockpos", "0", blocks, active);
                if(blockpos.split(",").length != 3) {
                    return "";
                }
                return "vanilla:set_block" + "," + blockpos;
            }
            case "better_float" -> { return "vanilla:better_float"; }
            case "use_container_xyz" -> {
                return String.join(",", "vanilla:use_container",
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
                return String.join(",", "vanilla:use_container",
                        blockpos,
                        String.valueOf(inputBoolean(block, "put_in", true, blocks, active)),
                        inputNumber(block, "item", "minecraft:stone", blocks, active),
                        inputNumber(block, "slot", "0", blocks, active),
                        inputNumber(block, "amount", "1", blocks, active));
            }
            case "melee_attack" -> {
                return String.join(",", "vanilla:melee_attack", inputNumber(block, "target", "-1", blocks, active));
            }
            case "use_item" -> { return "vanilla:use_item"; }
            case "use_block_xyz" -> {
                return String.join(",", "vanilla:use_block",
                        inputNumber(block, "x", "0", blocks, active), inputNumber(block, "y", "0", blocks, active),
                        inputNumber(block, "z", "0", blocks, active), inputNumber(block, "face", "up", blocks, active),
                        String.valueOf(inputBoolean(block, "held_item", false, blocks, active)));
            }
            case "use_block_blockpos" -> {
                String blockpos = inputNumber(block, "blockpos", "0", blocks, active);
                if (blockpos.split(",").length != 3) return "";
                return String.join(",", "vanilla:use_block", blockpos,
                        inputNumber(block, "face", "up", blocks, active),
                        String.valueOf(inputBoolean(block, "held_item", false, blocks, active)));
            }
            case "interact_entity" -> {
                return String.join(",", "vanilla:interact_entity",
                        inputNumber(block, "target", "-1", blocks, active),
                        String.valueOf(inputBoolean(block, "held_item", true, blocks, active)));
            }
            case "use_crafting_table_xyz", "use_inventory_crafting", "use_crafting_table_blockpos" -> {
                StringBuilder result = new StringBuilder(block.opcode().equals("use_inventory_crafting")?"vanilla:use_inventory_crafting":"vanilla:use_crafting_table").append(',').append(inputNumber(block, "amount", "1", blocks, active));
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
            case "find_drop" -> {
                String selectedDrop = inputNumber(block,"drop","0",blocks,active);
                return blackboard.findDrop(selectedDrop);
            }
            case "find_pheromone" -> {
                String selectedPheromone = inputNumber(block,"pheromone","0",blocks,active);
                return blackboard.findPheromone(selectedPheromone);
            }
            case "get_surrounding_pheromone_types" -> {
                return blackboard.getSurroundingPheromoneTypes();
            }
            case "get_item_in_container_xyz" -> {
                try{
                    int slot = Integer.parseInt(inputNumber(block, "slot", "0", blocks, active));
                    double x = Double.parseDouble(inputNumber(block, "x", "0", blocks, active));
                    double y = Double.parseDouble(inputNumber(block, "y", "0", blocks, active));
                    double z = Double.parseDouble(inputNumber(block, "z", "0", blocks, active));
                    return blackboard.getItemInContainer(slot, x, y, z);
                } catch (RuntimeException e) {
                    return "";
                }
            }
            case "get_item_in_container_blockpos" -> {
                try{
                    int slot = Integer.parseInt(inputNumber(block, "slot", "0", blocks, active));
                    String[] blockposStr = inputNumber(block, "blockpos", "", blocks, active).split(",");
                    double x = Double.parseDouble(blockposStr[0]);
                    double y = Double.parseDouble(blockposStr[1]);
                    double z = Double.parseDouble(blockposStr[2]);
                    return blackboard.getItemInContainer(slot, x, y, z);
                } catch (RuntimeException e) {
                    return "";
                }
            }
            case "get_speed" -> {
                return blackboard.getSpeed();
            }

            // variables
            case "get_variable" -> {
                return blackboard.getVariable(inputNumber(block,"name","0",blocks,active));
            }
            case "get_list" -> {
                return blackboard.getList(inputNumber(block, "name", "", blocks, active));
            }
            case "get_list_value" -> {
                try {
                    String name = inputNumber(block, "name", "", blocks, active);
                    int key = (int) Double.parseDouble(inputNumber(block, "key", "-1", blocks, active));
                    return blackboard.getListValue(name, key);
                } catch (RuntimeException ignored) {
                    return "";
                }
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
                    return a.equals(b);
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
            case "contain_str" -> {
                String source = inputNumber(block, "source", "0", blocks, active);
                String target = inputNumber(block, "target", "0", blocks, active);
                return source.contains(target);
            }

            // goal
            case "already_has_goal" -> {
                String name = inputNumber(block, "goal", "", blocks);
                List<TaskDebugEntry> foregroundGoal = debugForeground();
                List<TaskDebugEntry> backgroundGoal = debugBackground();
                return foregroundGoal.stream().anyMatch(entry -> entry.name().equals(name) && (entry.state().equals(TaskDebugState.QUEUED)||entry.state().equals(TaskDebugState.RUNNING))) ||
                        backgroundGoal.stream().anyMatch(entry -> entry.name().equals(name) && (entry.state().equals(TaskDebugState.QUEUED)||entry.state().equals(TaskDebugState.RUNNING)));
            }

            case "already_has_goal_at_priority" -> {
                String name = inputNumber(block, "goal", "", blocks);
                double priority = Double.parseDouble(inputNumber(block, "priority", "0", blocks, active));
                List<TaskDebugEntry> backgroundGoal = debugBackground();
                return backgroundGoal.stream().anyMatch(entry -> entry.name().equals(name) && (entry.state().equals(TaskDebugState.QUEUED)||entry.state().equals(TaskDebugState.RUNNING)) && entry.priority() == priority);
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
            case "has_item_in_container_xyz" -> {
                try{
                    double x = Double.parseDouble(inputNumber(block, "x", "0", blocks, active));
                    double y = Double.parseDouble(inputNumber(block, "y", "0", blocks, active));
                    double z = Double.parseDouble(inputNumber(block, "z", "0", blocks, active));
                    String item = inputNumber(block, "item", "0", blocks, active);
                    return blackboard.hasItemInContainer(item, x, y, z);
                } catch (RuntimeException e) {
                    return false;
                }
            }
            case "has_item_in_container_blockpos" -> {
                String item = inputNumber(block, "item", "0", blocks, active);
                String[] blockposStr = inputNumber(block, "blockpos", "", blocks, active).split(",");
                try{
                    double x = Double.parseDouble(blockposStr[0]);
                    double y = Double.parseDouble(blockposStr[1]);
                    double z = Double.parseDouble(blockposStr[2]);
                    return blackboard.hasItemInContainer(item, x, y, z);
                } catch (RuntimeException e) {
                    return false;
                }
            }


            default -> {
                return false;
            }
        }
    }
}
