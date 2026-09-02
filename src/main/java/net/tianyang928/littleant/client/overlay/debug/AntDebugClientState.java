package net.tianyang928.littleant.client.overlay.debug;

import net.tianyang928.littleant.entity.ai.debug.TaskDebugCodec;
import net.tianyang928.littleant.entity.ai.debug.TaskDebugSnapshot;
import net.tianyang928.littleant.network.SyncAntTaskDebugPayload;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class AntDebugClientState {
    private static final Map<Integer, TaskDebugSnapshot> SNAPSHOTS = new ConcurrentHashMap<>();
    private static volatile boolean enabled = false;
    private AntDebugClientState() {}
    public static void put(SyncAntTaskDebugPayload payload) {
        SNAPSHOTS.put(payload.entityId(), new TaskDebugSnapshot(payload.entityId(), payload.antName(),
                TaskDebugCodec.decode(payload.foreground(), true), TaskDebugCodec.decode(payload.background(), false)));
    }
    public static Map<Integer, TaskDebugSnapshot> snapshots() { return Map.copyOf(SNAPSHOTS); }
    public static void clear() { SNAPSHOTS.clear(); }
    public static boolean enabled() { return enabled; }
    public static void toggle() { enabled = !enabled; }
    public static void setEnabled(boolean enabled) { AntDebugClientState.enabled = enabled; }
}
