package net.tianyang928.littleant.client.debug;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.DeltaTracker;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.EntityHitResult;
import net.tianyang928.littleant.entity.ai.debug.TaskDebugEntry;
import net.tianyang928.littleant.entity.ai.debug.TaskDebugSnapshot;
import net.tianyang928.littleant.entity.ai.debug.TaskDebugState;
import net.tianyang928.littleant.entity.AntEntity;
import org.lwjgl.glfw.GLFW;

import java.util.Comparator;
import java.util.List;

public final class AntDebugOverlay {
    private static final int WIDTH = 190;
    private static final int SECTION_HEIGHT = 92;
    private static final int BORDER = 0xFF858585;
    private static final int BACKGROUND = 0xB8181818;
    private static final int ACTIVE_HEADER = 0xFFFFE79A;
    private AntDebugOverlay() {}

    public static void render(GuiGraphicsExtractor graphics, DeltaTracker ignored) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!AntDebugClientState.enabled() || minecraft.player == null || minecraft.level == null || minecraft.options.hideGui) return;
        TaskDebugSnapshot snapshot = selectSnapshot(minecraft);
        if (snapshot == null) return;

        boolean enlarged = GLFW.glfwGetKey(minecraft.getWindow().handle(), GLFW.GLFW_KEY_TAB) == GLFW.GLFW_PRESS;
        int width = enlarged ? WIDTH * 2 : WIDTH;
        int sectionHeight = enlarged ? SECTION_HEIGHT * 2 : SECTION_HEIGHT;
        drawSection(graphics, 5, 5, width, sectionHeight, "Foreground goal", snapshot.foreground(), snapshot.hasRunningForeground(), false);
        drawSection(graphics, 5, 5 + sectionHeight, width, sectionHeight, "Background goal", snapshot.background(), snapshot.hasRunningBackground(), true);
        graphics.text(Minecraft.getInstance().font, Component.translatable("tip.littleant.debug.f8_to_close"), 5, 5 + sectionHeight*2 + 4, 0xFFFFFFFF, false);
    }

    private static TaskDebugSnapshot selectSnapshot(Minecraft minecraft) {
        if (minecraft.hitResult instanceof EntityHitResult hit && hit.getEntity() instanceof AntEntity) {
            TaskDebugSnapshot aimed = AntDebugClientState.snapshots().get(hit.getEntity().getId());
            if (aimed != null) return aimed;
        }
        return AntDebugClientState.snapshots().values().stream()
                .filter(snapshot -> minecraft.level.getEntity(snapshot.entityId()) != null)
                .filter(snapshot -> minecraft.player.distanceToSqr(minecraft.level.getEntity(snapshot.entityId())) <= 32 * 32)
                .min(Comparator.comparingDouble(snapshot -> minecraft.player.distanceToSqr(minecraft.level.getEntity(snapshot.entityId()))))
                .orElse(null);
    }

    private static void drawSection(GuiGraphicsExtractor graphics, int x, int y, int width, int sectionHeight, String title,
                                    List<TaskDebugEntry> entries, boolean running, boolean showPriority) {
        graphics.fill(x, y, x + width, y + sectionHeight, BACKGROUND);
        graphics.fill(x, y, x + width, y + 1, BORDER);
        graphics.fill(x, y + sectionHeight - 1, x + width, y + sectionHeight, BORDER);
        graphics.fill(x, y, x + 1, y + sectionHeight, BORDER);
        graphics.fill(x + width - 1, y, x + width, y + sectionHeight, BORDER);
        graphics.text(Minecraft.getInstance().font, title, x + 5, y + 4, running ? ACTIVE_HEADER : 0xFFFFFFFF, false);
        graphics.enableScissor(x + 2, y + 15, x + width - 2, y + sectionHeight - 2);
        // 计算运行中的任务, 绿色运行中任务只能在section的上半部分
        int lineY = y + 17;
        int runningIndex = 0;
        boolean hasRunning = false;
        for (TaskDebugEntry entry : entries) {
            if (entry.state() == TaskDebugState.RUNNING) {
                hasRunning = true;
                break;
            }
            runningIndex++;
        }
        if(hasRunning) {
            lineY = (Math.min(runningIndex, sectionHeight/20-1)-runningIndex)*10+y+17;
        }
        for (TaskDebugEntry entry : entries) {
            String priority = showPriority ? "[" + formatPriority(entry.priority()) + "]" : "";
            int priorityX = x + width - 5 - Minecraft.getInstance().font.width(priority);
            int available = (showPriority ? priorityX - 4 : x + width - 5) - (x + 5);
            String name = "  ".repeat(entry.depth()) + entry.name();
            name = Minecraft.getInstance().font.plainSubstrByWidth(name, Math.max(0, available));
            graphics.text(Minecraft.getInstance().font, name, x + 5, lineY, color(entry.state()), false);
            if (showPriority) graphics.text(Minecraft.getInstance().font, priority, priorityX, lineY, color(entry.state()), false);
            lineY += 10;
        }
        graphics.disableScissor();
    }

    private static String formatPriority(double priority) {
        return priority == Math.rint(priority) ? Long.toString((long) priority) : String.format(java.util.Locale.ROOT, "%.2f", priority);
    }

    private static int color(TaskDebugState state) {
        return switch (state) {
            case RUNNING -> 0xFF73D97C;
            case FINISHED -> 0xFF909090;
            case QUEUED -> 0xFFFFFFFF;
            case SUSPENDED -> 0xFFB87979;
        };
    }
}
