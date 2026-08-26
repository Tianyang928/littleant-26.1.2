package net.tianyang928.littleant.entity.ai.brain;

import net.minecraft.core.BlockPos;
import net.tianyang928.littleant.LittleAnt;
import net.tianyang928.littleant.entity.AntEntity;
import net.tianyang928.littleant.entity.ai.goal.BreakBlockGoal;
import net.tianyang928.littleant.entity.ai.goal.SetBlockGoal;
import net.tianyang928.littleant.entity.ai.goal.BetterFloatGoal;
import net.tianyang928.littleant.entity.ai.goal.UseContainerGoal;
import net.tianyang928.littleant.entity.ai.goal.UseCraftingTableGoal;
import net.tianyang928.littleant.entity.ai.goal.UseInventoryCraftingGoal;
import net.tianyang928.littleant.entity.ai.goal.EntityMeleeAttackGoal;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** Runs non-conflicting script goals in parallel and retains preempted goals for resumption. */
public final class AntGoalScheduler {
    public final AntEntity ant;
    private final Deque<Task> queue = new ArrayDeque<>();
    private final List<Task> active = new ArrayList<>();
    //private final Set<UUID> registeredStarts = new HashSet<>();
    private long sequence;

    public AntGoalScheduler(AntEntity ant) { this.ant = ant; }

    public void submit(UUID startBlock, String name, double priority, EnumSet<Goal.Flag> flags, List<String> args,
                       Map<UUID, BrainBlock> blocks, List<UUID> receiveRoots) {
        //if (!registeredStarts.add(startBlock)) return;
        Task task = createTask(startBlock, name, priority, flags, args, blocks, receiveRoots, sequence++);
        //if (task == null) registeredStarts.remove(startBlock);
        if (task == null) return;

        List<Task> conflicts = active.stream().filter(task::conflictsWith).toList();
        LittleAnt.LOGGER.info("[AntGoalScheduler] submit: submitting {}, now active is {}", name, active);
        if (conflicts.isEmpty()) {
            active.add(task);
        } else if (conflicts.stream().allMatch(running -> priority < running.priority())) {
            for (Task conflict : conflicts) {
                conflict.suspend();
                active.remove(conflict);
                queue.add(conflict);
            }
            active.add(task);
        } else {
            queue.add(task);
        }
    }

    public void tick(NeoGoalRunner neoGoalRunner) {
        promoteAvailable();
        for (Task task : List.copyOf(active)) {
            if (task.tick(neoGoalRunner)) {
                active.remove(task);
                queue.remove(task);
                LittleAnt.LOGGER.info("[AntGoalScheduler] tick: Task {} completed and removed from active list.", task);
                //registeredStarts.remove(task.startBlock());
            }
        }
        promoteAvailable();
    }

    public void clear() {
        active.forEach(Task::suspend);
        active.clear();
        queue.clear();
        //registeredStarts.clear();
    }

    private void promoteAvailable() {
        List<Task> ordered = queue.stream()
                .sorted(Comparator.comparingDouble(Task::priority).thenComparingLong(Task::sequence))
                .toList();
        for (Task candidate : ordered) {
            if (active.stream().noneMatch(candidate::conflictsWith)
                    && noEarlierQueuedConflict(candidate, ordered)) {
                queue.remove(candidate);
                active.add(candidate);
                LittleAnt.LOGGER.info("[AntGoalScheduler] promoteAvailable Task {} add to active.", candidate.priority());
            }
        }
    }

    private boolean noEarlierQueuedConflict(Task candidate, List<Task> ordered) {
        for (Task other : ordered) {
            if (other == candidate) return true;
            if (queue.contains(other) && candidate.conflictsWith(other)) return false;
        }
        return true;
    }

    private Task createTask(UUID startBlock, String name, double priority, EnumSet<Goal.Flag> flags, List<String> args,
                            Map<UUID, BrainBlock> blocks, List<UUID> roots, long order) {
        switch (name) {
            case "break_block" -> {
                if (args.size() != 3) {
                    return null;
                }
                try {
                    BreakBlockGoal goal = new BreakBlockGoal(ant, BlockPos.ZERO);
                    goal.setTarget(new BlockPos((int) Double.parseDouble(args.get(0)), (int) Double.parseDouble(args.get(1)), (int) Double.parseDouble(args.get(2))));
                    return new VanillaTask(startBlock, priority, order, flags, goal);
                } catch (RuntimeException ignored) {
                    return null;
                }
            }
            case "set_block" -> {
                if (args.size() != 3) {
                    return null;
                }
                try {
                    return new VanillaTask(startBlock, priority, order, flags, new SetBlockGoal(ant, new BlockPos((int) Double.parseDouble(args.get(0)), (int) Double.parseDouble(args.get(1)), (int) Double.parseDouble(args.get(2)))));
                } catch (RuntimeException ignored) {
                    return null;
                }
            }
            case "better_float" -> { return new VanillaTask(startBlock, priority, order, flags, new BetterFloatGoal(ant)); }
            case "use_inventory_crafting", "use_crafting_table", "use_crafting_table_blockpos" -> {
                int slots = name.equals("use_crafting_table") ? 9 : 4;
                int expected = 0;
                switch (name) {
                    case "use_crafting_table" -> expected = 1 + slots + 3;
                    case "use_inventory_crafting" -> expected = 1 + slots;
                    case "use_crafting_table_blockpos" -> expected = 1 + slots + 1;
                }
                if (args.size() != expected) return null;
                try {
                    int amount = Integer.parseInt(args.get(0));
                    int offset = 1;
                    BlockPos pos = BlockPos.ZERO;
                    if (name.equals("use_crafting_table")) { pos = new BlockPos((int)Double.parseDouble(args.get(1)), (int)Double.parseDouble(args.get(2)), (int)Double.parseDouble(args.get(3))); offset = 4; }
                    List<ItemStack> items = new ArrayList<>(slots);
                    for (int i = 0; i < slots; i++) { var item = BuiltInRegistries.ITEM.getOptional(Identifier.tryParse(args.get(offset + i))).orElse(null); if (item == null) return null; items.add(new ItemStack(item)); }
                    CraftingInput input = CraftingInput.of(name.equals("use_crafting_table") ? 3 : 2, name.equals("use_crafting_table") ? 3 : 2, items);
                    if (name.equals("use_crafting_table")) { UseCraftingTableGoal goal = new UseCraftingTableGoal(ant); goal.setInput(input, pos, amount); return new VanillaTask(startBlock, priority, order, flags, goal); }
                    UseInventoryCraftingGoal goal = new UseInventoryCraftingGoal(ant); goal.setInput(input, amount); return new VanillaTask(startBlock, priority, order, flags, goal);
                } catch (RuntimeException e) { return null; }
            }
            case "use_container" -> {
                if (args.size() != 7) return null;
                try {
                    BlockPos pos = new BlockPos((int)Double.parseDouble(args.get(0)), (int)Double.parseDouble(args.get(1)), (int)Double.parseDouble(args.get(2)));
                    UseContainerGoal.Operation operation = UseContainerGoal.Operation.valueOf(args.get(3).trim().toUpperCase());
                    var item = BuiltInRegistries.ITEM.getOptional(Identifier.tryParse(args.get(4))).orElse(null);
                    if (item == null) return null;
                    UseContainerGoal goal = new UseContainerGoal(ant); goal.setRequest(pos, operation, item, Integer.parseInt(args.get(5)), Integer.parseInt(args.get(6)));
                    return new VanillaTask(startBlock, priority, order, flags, goal);
                } catch (RuntimeException e) { return null; }
            }
            case "melee_attack" -> {
                if (args.size() != 1) return null;
                try {
                    LivingEntity target = ant.level().getEntity(Integer.parseInt(args.get(0))) instanceof LivingEntity living ? living : null;
                    if (target == null) return null;
                    return new VanillaTask(startBlock, priority, order, flags, new EntityMeleeAttackGoal(ant, target, true));
                } catch (RuntimeException e) { return null; }
            }
        }
        if (roots.isEmpty()) {
            return null;
        }
        return new CustomTask(startBlock, priority, order, flags, roots);
    }

    //public enum Flag { MOVE, LOOK, JUMP }

    public interface NeoGoalRunner { boolean run(List<UUID> receiveRoots); }

    private interface Task {
        UUID startBlock();
        double priority();
        long sequence();
        EnumSet<Goal.Flag> flags();
        boolean tick(NeoGoalRunner neoGoalRunner);
        default void suspend() {}
        default boolean conflictsWith(Task other) {
            return flags().stream().anyMatch(other.flags()::contains);
        }
    }

    private final class VanillaTask implements Task {
        private final UUID startBlock; private final double priority; private final long sequence; private final EnumSet<Goal.Flag> flags; private final Goal goal; private boolean started; private int attempts;
        VanillaTask(UUID startBlock, double priority, long sequence, EnumSet<Goal.Flag> flags, Goal goal) { this.startBlock = startBlock; this.priority = priority; this.sequence = sequence; this.flags = EnumSet.copyOf(flags); this.goal = goal; }
        public UUID startBlock() { return startBlock; }
        public double priority() { return priority; } public long sequence() { return sequence; }
        public EnumSet<Goal.Flag> flags() { return flags; }
        public boolean tick(NeoGoalRunner ignored) {
            if (!started) {
                if(goal.getFlags().contains(Goal.Flag.MOVE) && ant.isPathFinding()){
                    if(ant.level().getGameTime()%40 == 0){
                        LittleAnt.LOGGER.info("[AntGoalScheduler] goal {} can't execute, ant is path finding {}", goal.getClass(),ant.isPathFinding());
                    }
                    return false;
                }
                if (!goal.canUse()) {
                    //如果超过轮到他运行但是两秒内都不符合条件，那就删掉这个goal
                    return ++attempts > 40;
                }
                attempts = 0;
                //goal.start();
                ant.goalSelector.addGoal((int)priority, goal);
                started = true;
            }
            //goal.tick();
            if (!goal.canContinueToUse()) {
                //goal.stop();
                if(++attempts > 40) {
                    ant.goalSelector.removeGoal(goal);
                    return true;
                }
            }
            return false;
        }
        public void suspend() { if (started) goal.stop(); }
    }

    private final class CustomTask implements Task {
        private final UUID startBlock;
        private final double priority;
        private final long sequence;
        private final EnumSet<Goal.Flag> flags;
        private final List<UUID> roots;

        CustomTask(UUID startBlock, double priority, long sequence, EnumSet<Goal.Flag> flags, List<UUID> roots) {
            this.startBlock = startBlock;
            this.priority = priority;
            this.sequence = sequence;
            this.flags = EnumSet.copyOf(flags);
            this.roots = List.copyOf(roots);
            LittleAnt.LOGGER.info("[AntGoalScheduler] custom task: create new");
        }

        public UUID startBlock() {
            return startBlock;
        }

        public double priority() {
            return priority;
        }

        public long sequence() {
            return sequence;
        }

        public EnumSet<Goal.Flag> flags() {
            return flags;
        }

        public boolean tick(NeoGoalRunner neoGoalRunner) {
            return neoGoalRunner.run(roots);
        }
    }
}
