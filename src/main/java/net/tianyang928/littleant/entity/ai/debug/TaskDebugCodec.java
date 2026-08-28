package net.tianyang928.littleant.entity.ai.debug;

import java.util.ArrayList;
import java.util.List;

/** Compact, bounded text representation used inside the debug payload. */
public final class TaskDebugCodec {
    private static final char FIELD = '\u001f';
    private static final char ROW = '\u001e';
    private TaskDebugCodec() {}

    public static String encode(List<TaskDebugEntry> entries) {
        StringBuilder result = new StringBuilder();
        for (TaskDebugEntry entry : entries) {
            if (!result.isEmpty()) result.append(ROW);
            result.append(entry.state().ordinal()).append(FIELD).append(entry.depth()).append(FIELD)
                    .append(entry.priority()).append(FIELD).append(sanitize(entry.name()));
        }
        return result.toString();
    }

    public static List<TaskDebugEntry> decode(String encoded, boolean foreground) {
        if (encoded.isEmpty()) return List.of();
        List<TaskDebugEntry> result = new ArrayList<>();
        for (String row : encoded.split(String.valueOf(ROW), -1)) {
            String[] fields = row.split(String.valueOf(FIELD), 4);
            if (fields.length != 4) continue;
            try {
                int state = Integer.parseInt(fields[0]);
                if (state < 0 || state >= TaskDebugState.values().length) continue;
                result.add(new TaskDebugEntry(foreground, TaskDebugState.values()[state], Math.max(0, Integer.parseInt(fields[1])), fields[3], Double.parseDouble(fields[2])));
            } catch (NumberFormatException ignored) {}
        }
        return List.copyOf(result);
    }

    private static String sanitize(String value) { return value == null ? "" : value.replace(FIELD, ' ').replace(ROW, ' '); }
}
