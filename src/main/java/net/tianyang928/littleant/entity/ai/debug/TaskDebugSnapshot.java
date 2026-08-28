package net.tianyang928.littleant.entity.ai.debug;

import java.util.List;

public record TaskDebugSnapshot(int entityId, String antName, List<TaskDebugEntry> foreground, List<TaskDebugEntry> background) {
    public TaskDebugSnapshot {
        antName = antName == null ? "" : antName;
        foreground = List.copyOf(foreground);
        background = List.copyOf(background);
    }
    public boolean hasRunningForeground() { return foreground.stream().anyMatch(e -> e.state() == TaskDebugState.RUNNING); }
    public boolean hasRunningBackground() { return background.stream().anyMatch(e -> e.state() == TaskDebugState.RUNNING); }
}
