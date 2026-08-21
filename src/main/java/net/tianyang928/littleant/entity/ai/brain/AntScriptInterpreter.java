package net.tianyang928.littleant.entity.ai.brain;

import net.tianyang928.littleant.entity.AntEntity;

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
        if (!active.add(block.id())) {
            return;
        }
        switch (block.opcode()) {
            case "ai_start", "tick_start", "receive_goal" -> {}
            case "start_goal" -> {
                List<String> all_param = Arrays.stream(inputNumber(block, "goal", "", blocks).split(",", -1)).map(String::trim).toList();
                if (all_param.isEmpty() || all_param.getFirst().isBlank()) break;
                double priority;
                try {
                    priority = Double.parseDouble(inputNumber(block, "priority", "", blocks));
                } catch (NumberFormatException e) {
                    break;
                }
                EnumSet<AntGoalScheduler.Flag> flags = EnumSet.noneOf(AntGoalScheduler.Flag.class);
                if (inputBoolean(block, "move_flag", false, blocks)) flags.add(AntGoalScheduler.Flag.MOVE);
                if (inputBoolean(block, "look_flag", false, blocks)) flags.add(AntGoalScheduler.Flag.LOOK);
                if (inputBoolean(block, "jump_flag", false, blocks)) flags.add(AntGoalScheduler.Flag.JUMP);
                goalScheduler.submit(block.id(), all_param.getFirst(), priority, flags, all_param.subList(1, all_param.size()), blocks,
                        //允许多个同名goal模组（gui内的）同时被激活
                        receiveGoalRoots.getOrDefault(all_param.getFirst(), List.of()));
            }
            case "clear_goal" -> goalScheduler.clear();
            case "finish_goal" -> { if (executingCustomGoal) customGoalFinished = true; }
            case "move_to_xyz" -> {
                try {
                    ant.scriptMoveTo(Double.parseDouble(inputNumber(block, "x", "0", blocks)),
                                    Double.parseDouble(inputNumber(block, "y", "0", blocks)),
                                    Double.parseDouble(inputNumber(block, "z", "0", blocks)));
                } catch (NumberFormatException e) {
                    break;
                }
            }
            case "step_forward" -> {
                try {
                    ant.scriptStepForward(Double.parseDouble(inputNumber(block, "distance", "0", blocks)));
                } catch (NumberFormatException e) {
                    break;
                }
            }
            case "look_at_xyz" -> {
                try {
                    ant.scriptLookAt(Double.parseDouble(inputNumber(block, "x", "0", blocks)),
                                    Double.parseDouble(inputNumber(block, "y", "0", blocks)),
                                    Double.parseDouble(inputNumber(block, "z", "0", blocks)));
                } catch (NumberFormatException e) {
                    break;
                }
            }
            case "rotate" -> {
                try {
                    ant.scriptRotate(Double.parseDouble(inputNumber(block, "angle", "0", blocks)));
                } catch (NumberFormatException e) {
                    break;
                }
            }
            case "repeat" -> {
                int count;
                try {
                    count = Math.min(1000, Math.max(0, (int) Double.parseDouble(inputNumber(block, "count", "0", blocks))));
                } catch (NumberFormatException e) {
                    break;
                }
                InputSlot body = input(block, "body");
                if (body != null && blocks.containsKey(body.blockId())) {
                    for (int i = 0; i < count; i++) {
                        executeBlock(blocks.get(body.blockId()), active);
                    }
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
        if (!active.add(block.id())) {
            return "0";
        }
        switch (block.opcode()) {
            case "add" -> {
                try {
                    double a = Double.parseDouble(inputNumber(block, "a", "0", blocks, active));
                    double b = Double.parseDouble(inputNumber(block, "b", "0", blocks, active));
                    return String.valueOf(a + b);
                } catch (NumberFormatException e) {
                    return "0";
                }
            }
            case "subtract" -> {
                try {
                    double a = Double.parseDouble(inputNumber(block, "a", "0", blocks, active));
                    double b = Double.parseDouble(inputNumber(block, "b", "0", blocks, active));
                    return String.valueOf(a - b);
                } catch (NumberFormatException e) {
                    return "0";
                }
            }
            case "multiply" -> {
                try {
                    double a = Double.parseDouble(inputNumber(block, "a", "0", blocks, active));
                    double b = Double.parseDouble(inputNumber(block, "b", "0", blocks, active));
                    return String.valueOf(a * b);
                } catch (NumberFormatException e) {
                    return "0";
                }
            }
            case "divide" -> {
                try {
                    double a = Double.parseDouble(inputNumber(block, "a", "0", blocks, active));
                    double b = Double.parseDouble(inputNumber(block, "b", "0", blocks, active));
                    return String.valueOf(b == 0 ? 0 : a / b);
                } catch (NumberFormatException e) {
                    return "0";
                }
            }
            case "mod" -> {
                try {
                    double a = Double.parseDouble(inputNumber(block, "a", "0", blocks, active));
                    double b = Double.parseDouble(inputNumber(block, "b", "0", blocks, active));
                    return String.valueOf(b == 0 ? 0 : a % b);
                } catch (NumberFormatException e) {
                    return "0";
                }
            }
            case "absolute" -> {
                try {
                    double a = Double.parseDouble(inputNumber(block, "number", "0", blocks, active));
                    return String.valueOf(Math.abs(a));
                } catch (NumberFormatException e) {
                    return "0";
                }
            }
            case "random" -> {
                try {
                    double a = Double.parseDouble(inputNumber(block, "min", "0", blocks, active));
                    double b = Double.parseDouble(inputNumber(block, "max", "0", blocks, active));
                    return String.valueOf(Math.random() * (b - a) + a);
                } catch (NumberFormatException e) {
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
                return "break_block" + "," + blockpos;
            }
            case "set_block_blockpos" -> {
                String blockpos = inputNumber(block, "blockpos", "0", blocks, active);
                return "set_block" + "," + blockpos;
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
                } catch (NumberFormatException e) {
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
                } catch (NumberFormatException e) {
                    return "-1";
                }
            }
            case "get_block_xyz" -> {
                try{
                    String x = inputNumber(block, "x", "0", blocks, active);
                    String y = inputNumber(block, "y", "0", blocks, active);
                    String z = inputNumber(block, "z", "0", blocks, active);
                    return blackboard.getBlock(Double.parseDouble(x), Double.parseDouble(y), Double.parseDouble(z));
                } catch (NumberFormatException e) {
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
                } catch (NumberFormatException e) {
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
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private boolean booleanModule(BrainBlock block, Map<UUID, BrainBlock> blocks, Set<UUID> active) {
        if (!active.add(block.id())) {
            return false;
        }
        switch (block.opcode()) {
            case "greater_than" -> {
                String a = inputNumber(block, "a", "0", blocks, active);
                String b = inputNumber(block, "b", "0", blocks, active);

                try {
                    double aNum = Double.parseDouble(a);
                    double bNum = Double.parseDouble(b);
                    return aNum > bNum;
                } catch (NumberFormatException e) {
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
                } catch (NumberFormatException e) {
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
                } catch (NumberFormatException e) {
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

            // sense
            case "has_item_in_inventory" -> {
                String item = inputNumber(block, "item", "0", blocks, active);
                return blackboard.hasItemInInventory(item);
            }
            default -> {
                return false;
            }
        }
    }
}
