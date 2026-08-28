package net.tianyang928.littleant.entity.ai.debug;

public record TaskDebugEntry(boolean foreground, TaskDebugState state, int depth, String name, double priority) {}
