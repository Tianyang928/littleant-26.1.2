package net.tianyang928.littleant.entity.ai.brain;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.tianyang928.littleant.LittleAnt;
import net.tianyang928.littleant.entity.AntEntity;
import net.tianyang928.littleant.entity.ai.goal.*;
import net.tianyang928.littleant.entity.ai.debug.TaskDebugEntry;
import net.tianyang928.littleant.entity.ai.debug.TaskDebugState;

import java.util.*;

/** Owns long-running script actions. Foreground tasks are FIFO; background tasks use priority arbitration. */
public final class AntGoalScheduler {
    private final AntEntity ant;
    private final Deque<Task> backgroundQueue = new ArrayDeque<>();
    private final List<Task> backgroundActive = new ArrayList<>();
    private final Deque<Task> foregroundQueue = new ArrayDeque<>();
    private Task foregroundActive;
    private long sequence;
    private final Deque<TaskDebugEntry> finishedHistory = new ArrayDeque<>();

    public AntGoalScheduler(AntEntity ant) { this.ant = ant; }

    public void submitBackground(UUID startBlock, String name, double priority, EnumSet<Goal.Flag> flags, List<String> args,
                                 List<UUID> startRoots, List<UUID> tickRoots, Task parent) {
        Task task = createTask(startBlock, name, priority, flags, args, startRoots, tickRoots, parent, false);
        if (task == null || containsTask(task)) return;
        attach(task, parent);
        // A spawned background child participates in the global scheduler, but must
        // not preempt its own ancestor while that ancestor is still submitting it.
        List<Task> conflicts = backgroundActive.stream()
                .filter(running -> !isAncestor(running, task))
                .filter(task::conflictsWith)
                .toList();
        if (conflicts.isEmpty()) backgroundActive.add(task);
        else if (conflicts.stream().allMatch(running -> task.priority() < running.priority())) {
            for (Task conflict : conflicts) suspendTree(conflict, true);
            backgroundActive.add(task);
        } else backgroundQueue.add(task);
    }

    /** Adds a FIFO foreground task. Its resource flags come from the goal implementation. */
    public void submitForeground(UUID startBlock, String name, List<String> args, List<UUID> startRoots, List<UUID> tickRoots, Task parent) {
        Task task = createTask(startBlock, name, 0, null, args, startRoots, tickRoots, parent, true);
        if (task == null || containsTask(task)) return;
        attach(task, parent);
        enqueueForeground(task, false);
    }

    public void submitMoveTo(UUID sourceBlock, BlockPos target, Task parent) {
        Task task = new MoveToTask(sourceBlock, "vanilla:move_to",sequence++, target, parent);
        if (containsTask(task)) return;
        attach(task, parent);
        enqueueForeground(task, false);
    }

    public void requestFinishCurrentGoal(Task target) {
        if (target != null) target.finishNow = true;
    }

    public void requestFinishCurrentGoalDelay(Task target) {
        if (target != null) target.finishAfterForeground = true;
    }

    public void tick(NeoGoalRunner runner) {
        tickForeground(runner);
        tickBackground(runner);
    }

    public void clear() {
        if (foregroundActive != null) suspendTree(foregroundActive, false);
        foregroundActive = null;
        List.copyOf(foregroundQueue).forEach(task -> suspendTree(task, false));
        List.copyOf(backgroundActive).forEach(task -> suspendTree(task, false));
        List.copyOf(backgroundQueue).forEach(task -> suspendTree(task, false));
        foregroundQueue.clear(); backgroundActive.clear(); backgroundQueue.clear();
        finishedHistory.clear();
    }

    /** Returns a bounded, render/network-safe view of scheduler state. */
    public List<TaskDebugEntry> debugForeground() {
        List<TaskDebugEntry> result = new ArrayList<>();
        finishedHistory.stream().filter(TaskDebugEntry::foreground).forEach(result::add);
        appendDebug(result, foregroundActive, true, TaskDebugState.RUNNING, 0);
        appendDebug(result, foregroundQueue, true, TaskDebugState.QUEUED, 0);
        return List.copyOf(result);
    }

    public List<TaskDebugEntry> debugBackground() {
        List<TaskDebugEntry> result = new ArrayList<>();
        finishedHistory.stream().filter(entry -> !entry.foreground()).forEach(result::add);
        appendDebug(result, backgroundActive, false, TaskDebugState.RUNNING, 0);
        appendDebug(result, backgroundQueue, false, TaskDebugState.QUEUED, 0);
        return List.copyOf(result);
    }

    private void appendDebug(List<TaskDebugEntry> result, Task task, boolean foreground, TaskDebugState state, int depth) {
        if (task == null || task.foreground != foreground || result.size() >= 64) return;
        result.add(new TaskDebugEntry(foreground, task.debugState(state), depth, task.name, task.priority()));
        for (Task child : task.children) appendDebug(result, child, foreground, stateOf(child), depth + 1);
    }

    private void appendDebug(List<TaskDebugEntry> result, Collection<Task> tasks, boolean foreground, TaskDebugState state, int depth) {
        for (Task task : tasks) {
            if (task.foreground != foreground || task.parent != null && task.parent.foreground == foreground) continue;
            int actualDepth = depth;
            for (Task parent = task.parent; parent != null; parent = parent.parent) actualDepth++;
            appendDebug(result, task, foreground, state, actualDepth);
        }
    }

    private TaskDebugState stateOf(Task task) {
        if (task.suspended) return TaskDebugState.SUSPENDED;
        if (task == foregroundActive || backgroundActive.contains(task)
                || task.parent != null && task.parent.foregroundActive == task) return TaskDebugState.RUNNING;
        return TaskDebugState.QUEUED;
    }

    private void tickForeground(NeoGoalRunner runner) {
        if (foregroundActive == null && !foregroundQueue.isEmpty() && isParentActive(foregroundQueue.peekFirst())) {
            foregroundActive = foregroundQueue.removeFirst();
            foregroundActive.suspended = false;
            for (Task task : List.copyOf(backgroundActive)) {
                if (foregroundActive.conflictsWith(task)) {
                    suspendTree(task, true);
                    LittleAnt.LOGGER.info("[AntGoalScheduler] tickForeground: suspend background task {}, because of conflict with foreground task {}", task.name, foregroundActive.name);
                }
            }
        }
        if (foregroundActive != null && foregroundActive.tick(runner)) {
            finishTree(foregroundActive);
            foregroundActive = null;
        }
    }

    private void tickBackground(NeoGoalRunner runner) {
        promoteBackground();
        for (Task task : List.copyOf(backgroundActive)) {
            if (!isBlockedByForeground(task) && task.tick(runner)) finishTree(task);
        }
        promoteBackground();
    }

    private boolean isBlockedByForeground(Task task) { return foregroundActive != null && foregroundActive.conflictsWith(task); }

    private boolean isParentActive(Task task) {
        return task.parent == null || isTaskActive(task.parent) && isParentActive(task.parent);
    }

    private boolean isTaskActive(Task task) {
        if (backgroundActive.contains(task) || foregroundActive == task) return true;
        return task.parent != null && task.parent.foregroundActive == task;
    }

    private void promoteBackground() {
        List<Task> ordered = backgroundQueue.stream().sorted(Comparator.comparingDouble(Task::priority).thenComparingLong(Task::sequence)).toList();
        for (Task candidate : ordered) {
            if (isParentActive(candidate) && !isBlockedByForeground(candidate)
                    && backgroundActive.stream().filter(running -> !isAncestor(running, candidate)).noneMatch(candidate::conflictsWith)
                    && noEarlierQueuedConflict(candidate, ordered)) {
                backgroundQueue.remove(candidate); candidate.suspended = false; backgroundActive.add(candidate);
            }
        }
    }

    private boolean noEarlierQueuedConflict(Task candidate, List<Task> ordered) {
        for (Task other : ordered) {
            if (other == candidate) return true;
            if (backgroundQueue.contains(other) && candidate.conflictsWith(other)) return false;
        }
        return true;
    }

    private void attach(Task task, Task parent) {
        if (parent != null) {
            parent.children.add(task);
            LittleAnt.LOGGER.info("[AntGoalScheduler] attach task {} to parent {}", task.name, parent.name);
        }
    }

    private void enqueueForeground(Task task, boolean first) {
        Deque<Task> queue = task.parent == null ? foregroundQueue : task.parent.foregroundQueue;
        if (first) queue.addFirst(task); else queue.addLast(task);
    }

    private boolean containsTask(Task candidate) {
        if (candidate.parent != null) return candidate.parent.children.stream().anyMatch(task -> task.sameInvocation(candidate));
        return allTasks().stream().anyMatch(task -> task.sameInvocation(candidate));
    }

    private List<Task> allTasks() {
        List<Task> tasks = new ArrayList<>(foregroundQueue);
        if (foregroundActive != null) tasks.add(foregroundActive);
        tasks.addAll(backgroundActive); tasks.addAll(backgroundQueue);
        return tasks;
    }

    /** Suspending a parent recursively stops every submitted descendant and preserves the subtree for resumption. */
    private void suspendTree(Task task, boolean requeueRoot) {
        for (Task child : List.copyOf(task.children)) suspendTree(child, true);
        task.suspend();
        task.suspended = requeueRoot;
        backgroundActive.remove(task); foregroundQueue.remove(task); backgroundQueue.remove(task);
        if (task.parent != null) {
            task.parent.foregroundQueue.remove(task);
            if (task.parent.foregroundActive == task) task.parent.foregroundActive = null;
        }
        if (foregroundActive == task) foregroundActive = null;
        if (requeueRoot) {
            if (task.foreground) enqueueForeground(task, true);
            else backgroundQueue.add(task);
        }
    }

    public void finishTree(Task task) {
        if (task == null) return;
        task.stop();
        task.suspended = false;
        int finishedDepth = 0;
        for (Task parent = task.parent; parent != null; parent = parent.parent) finishedDepth++;
        backgroundActive.remove(task); backgroundQueue.remove(task); foregroundQueue.remove(task);
        if (foregroundActive == task) {

            foregroundActive = null;
        }
        // Background children were spawned, not awaited. Once their owner finishes
        // they will also finish / re-parent to null parent.
        LittleAnt.LOGGER.info("[AntGoalScheduler] finishTree: stop task {}", task.name);
        for (Task child : List.copyOf(task.children)) {
            if (!child.foreground) {
                child.parent = null;
                //backgroundActive.remove(child); backgroundQueue.remove(child);
                task.children.remove(child);
            }
            else {
                // Foreground descendants belong to the parent's sequential work.
                // Cancelling the parent must remove them, not merely stop them,
                // or tickNestedForeground can start them again later this tick.
                finishTree(child);
            }
        }
        if (task.parent != null) {
            task.parent.foregroundQueue.remove(task);
            if (task.parent.foregroundActive == task) task.parent.foregroundActive = null;
            task.parent.children.remove(task);
        }
        finishedHistory.addFirst(new TaskDebugEntry(task.foreground, TaskDebugState.FINISHED, finishedDepth, task.name, task.priority()));
        while (finishedHistory.size() > 10) finishedHistory.removeLast();
    }

    /** Advances a custom task's foreground child inside its parent's own tick. */
    private void tickNestedForeground(Task parent, NeoGoalRunner runner) {
        if (parent.finishNow) return;
        if (parent.foregroundActive == null && !parent.foregroundQueue.isEmpty()) {
            parent.foregroundActive = parent.foregroundQueue.removeFirst();
            parent.foregroundActive.suspended = false;
            Task child = parent.foregroundActive;
            for (Task background : List.copyOf(backgroundActive)) {
                if (!isAncestor(background, child) && child.conflictsWith(background)) suspendTree(background, true);
            }
        }
        Task child = parent.foregroundActive;
        if (child != null && child.tick(runner)) finishTree(child);
    }

    private boolean isAncestor(Task possibleAncestor, Task task) {
        for (Task parent = task.parent; parent != null; parent = parent.parent) {
            if (parent == possibleAncestor) return true;
        }
        return false;
    }

    private Task createTask(UUID startBlock, String name, double priority, EnumSet<Goal.Flag> requestedFlags,
                            List<String> args, List<UUID> startRoots, List<UUID> tickRoots, Task parent, boolean foreground) {
        Goal goal = createVanillaGoal(name, args);
        long order = sequence++;
        if (goal != null) {
            EnumSet<Goal.Flag> flags = requestedFlags == null ? EnumSet.copyOf(goal.getFlags()) : EnumSet.copyOf(requestedFlags);
            return new VanillaTask(startBlock, name, priority, order, flags, goal, parent, foreground);
        }
        if (startRoots.isEmpty() && tickRoots.isEmpty()) return null;
        EnumSet<Goal.Flag> flags = requestedFlags == null ? null : EnumSet.copyOf(requestedFlags);
        return new CustomTask(startBlock, name,priority, order, flags, startRoots, tickRoots, parent, foreground);
    }

    private Goal createVanillaGoal(String name, List<String> args) {
        try {
            return switch (name) {
                case "vanilla:break_block" -> args.size() == 3 ? breakBlock(args) : null;
                case "vanilla:set_block" -> args.size() == 3 ? new SetBlockGoal(ant, blockPos(args, 0)) : null;
                case "vanilla:better_float" -> new BetterFloatGoal(ant);
                case "vanilla:use_inventory_crafting", "vanilla:use_crafting_table" -> craftingGoal(name, args);
                case "vanilla:use_container" -> containerGoal(args);
                case "vanilla:melee_attack" -> meleeGoal(args);
                default -> null;
            };
        } catch (RuntimeException ignored) { return null; }
    }

    private BreakBlockGoal breakBlock(List<String> args) {
        BreakBlockGoal goal = new BreakBlockGoal(ant, BlockPos.ZERO); goal.setTarget(blockPos(args, 0)); return goal;
    }

    private BlockPos blockPos(List<String> args, int offset) {
        return new BlockPos((int) Double.parseDouble(args.get(offset)), (int) Double.parseDouble(args.get(offset + 1)), (int) Double.parseDouble(args.get(offset + 2)));
    }

    private Goal craftingGoal(String name, List<String> args) {
        int slots = name.equals("use_crafting_table") ? 9 : 4;
        int expected = switch (name) { case "use_crafting_table" -> 1 + slots + 3; case "use_inventory_crafting" -> 1 + slots; default -> 1 + slots + 1; };
        if (args.size() != expected) return null;
        int amount = Integer.parseInt(args.getFirst()); int offset = 1; BlockPos pos = BlockPos.ZERO;
        if (name.equals("use_crafting_table")) { pos = blockPos(args, 1); offset = 4; }
        List<ItemStack> items = new ArrayList<>(slots);
        for (int i = 0; i < slots; i++) {
            var item = BuiltInRegistries.ITEM.getOptional(Identifier.tryParse(args.get(offset + i))).orElse(null);
            if (item == null) return null;
            items.add(new ItemStack(item));
        }
        CraftingInput input = CraftingInput.of(name.equals("use_crafting_table") ? 3 : 2, name.equals("use_crafting_table") ? 3 : 2, items);
        if (name.equals("use_crafting_table")) {
            UseCraftingTableGoal goal = new UseCraftingTableGoal(ant); goal.setInput(input, pos, amount); return goal;
        }
        UseInventoryCraftingGoal goal = new UseInventoryCraftingGoal(ant); goal.setInput(input, amount); return goal;
    }

    private Goal containerGoal(List<String> args) {
        if (args.size() != 7) return null;
        var item = BuiltInRegistries.ITEM.getOptional(Identifier.tryParse(args.get(4))).orElse(null);
        if (item == null) return null;
        UseContainerGoal goal = new UseContainerGoal(ant);
        goal.setRequest(blockPos(args, 0), UseContainerGoal.Operation.valueOf(args.get(3).trim().toUpperCase()), item, Integer.parseInt(args.get(5)), Integer.parseInt(args.get(6)));
        return goal;
    }

    private Goal meleeGoal(List<String> args) {
        if (args.size() != 1) return null;
        LivingEntity target = ant.level().getEntity(Integer.parseInt(args.getFirst())) instanceof LivingEntity living ? living : null;
        return target == null ? null : new EntityMeleeAttackGoal(ant, target, true);
    }

    public interface NeoGoalRunner { boolean run(Task task, List<UUID> receiveRoots, List<UUID> tickReceiveRoots); }

    /** Execution context used to bind custom-goal children to the currently running parent. */
    public abstract static class Task {
        private final UUID startBlock;
        private final double priority;
        private final long sequence;
        private final EnumSet<Goal.Flag> flags;
        protected Task parent;
        private final boolean foreground;
        protected boolean finishNow;
        protected boolean finishAfterForeground;
        private boolean suspended;
        protected final List<Task> children = new ArrayList<>();
        private final Deque<Task> foregroundQueue = new ArrayDeque<>();
        private Task foregroundActive;
        public String name;

        private Task(UUID startBlock, String name, double priority, long sequence, EnumSet<Goal.Flag> flags, Task parent, boolean foreground) {
            this.startBlock = startBlock; this.name = name; this.priority = priority; this.sequence = sequence; this.flags = flags; this.parent = parent; this.foreground = foreground;
        }
        public double priority() { return priority; }
        public long sequence() { return sequence; }
        private TaskDebugState debugState(TaskDebugState fallback) { return suspended ? TaskDebugState.SUSPENDED : fallback; }
        private boolean conflictsWith(Task other) {
            if (flags == null || other.flags == null) return false;
            return flags.stream().anyMatch(other.flags::contains);
        }
        private boolean sameInvocation(Task other) { return startBlock.equals(other.startBlock) && parent == other.parent && foreground == other.foreground; }
        protected abstract boolean tick(NeoGoalRunner runner);
        protected void suspend() {}
        protected void stop() { suspend(); }
        protected boolean childrenFinished() { return children.stream().noneMatch(child -> child.foreground); }
    }

    private final class VanillaTask extends Task {
        private final Goal goal;
        private boolean registered;
        private int attempts;
        private VanillaTask(UUID startBlock, String name, double priority, long sequence, EnumSet<Goal.Flag> flags, Goal goal, Task parent, boolean foreground) {
            super(startBlock, name, priority, sequence, flags, parent, foreground); this.goal = goal;
        }
        protected boolean tick(NeoGoalRunner ignored) {
            if (!registered) {
                if (!goal.canUse()) return ++attempts > 40;
                attempts = 0; ant.goalSelector.addGoal((int) priority(), goal); registered = true;
            }
            if(!goal.canContinueToUse()){
                attempts++;
            }
            else {
                attempts --;
                if(attempts <= 0){
                    attempts = 0;
                }
            }
            return attempts > 40;
        }
        protected void suspend() { if (registered) ant.goalSelector.removeGoal(goal); goal.stop(); registered = false; }
    }

    private final class MoveToTask extends Task {
        private final BlockPos target;
        private boolean started;
        private MoveToTask(UUID startBlock, String name, long sequence, BlockPos target, Task parent) {
            super(startBlock, name, 0, sequence, EnumSet.of(Goal.Flag.MOVE), parent, true); this.target = target.immutable();
        }
        protected boolean tick(NeoGoalRunner ignored) {
            if (!started) {
                var path = ant.getNavigation().createPath(target, 2, 64);
                if (path == null) return true;
                ant.getNavigation().moveTo(path, ant.speedModifier); started = true;
            }
            return ant.getNavigation().isDone() || ant.blockPosition().closerThan(target, 2.0D);
        }
        protected void suspend() { ant.getNavigation().stop(); started = false; }
    }

    public final class CustomTask extends Task {
        private final List<UUID> startRoots;
        private final List<UUID> tickRoots;
        private boolean bodyFinished;
        public boolean started = false;
        private CustomTask(UUID startBlock, String name, double priority, long sequence, EnumSet<Goal.Flag> flags, List<UUID> startRoots, List<UUID> tickRoots, Task parent, boolean foreground) {
            super(startBlock, name, priority, sequence, flags, parent, foreground);
            this.startRoots = List.copyOf(startRoots);
            this.tickRoots = List.copyOf(tickRoots);
        }
        protected boolean tick(NeoGoalRunner runner) {
            if (!bodyFinished) bodyFinished = runner.run(this, startRoots, tickRoots);
            if (finishNow) return true;
            tickNestedForeground(this, runner);
            return (bodyFinished || finishAfterForeground) && childrenFinished();
        }
    }
}
