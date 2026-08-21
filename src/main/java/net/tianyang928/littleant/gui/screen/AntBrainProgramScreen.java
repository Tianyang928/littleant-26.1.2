package net.tianyang928.littleant.gui.screen;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.tianyang928.littleant.entity.ai.brain.BlockDefinition;
import net.tianyang928.littleant.entity.ai.brain.BlockRenderLayout;
import net.tianyang928.littleant.entity.ai.brain.BrainBlock;
import net.tianyang928.littleant.entity.ai.brain.InputSlot;
import net.tianyang928.littleant.entity.ai.brain.ModuleRegistry;
import net.tianyang928.littleant.entity.ai.brain.ValueType;
import net.tianyang928.littleant.gui.AntBrainProgramMenu;
import net.tianyang928.littleant.network.PlaceAntBrainBlockPayload;
import net.tianyang928.littleant.network.RemoveAntBrainBlockPayload;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Registry-driven visual editor. Shapes are rectangular until the sprite renderer is introduced. */
public class AntBrainProgramScreen extends AbstractContainerScreen<AntBrainProgramMenu> {
    private static final int SIDEBAR_WIDTH = 66;
    private static final int PALETTE_WIDTH = 330;
    private static final int HEADER_HEIGHT = 28;
    private static final int PALETTE_HEADER_HEIGHT = 34;
    private static final int CANVAS_TOP = 58;
    private static final int LIST_GAP = 8;
    private static final Map<String, List<BlockDefinition>> MODULES_BY_CATEGORY = ModuleRegistry.byCategory();
    private static final List<String> CATEGORIES = List.copyOf(MODULES_BY_CATEGORY.keySet());

    private int selectedCategory;
    private String draggingOpcode;
    private UUID draggingId;
    private int draggingStartX;
    private int draggingStartY;
    private int paletteScroll;
    private final LinkedHashMap<UUID, BrainBlock> placedBlocks = new LinkedHashMap<>();
    private final List<PaletteEntry> paletteEntries = new ArrayList<>();
    private final LinkedHashMap<UUID, BlockRenderLayout> canvasLayouts = new LinkedHashMap<>();
    private int mouseX;
    private int mouseY;
    private int lastTimeMouseClickX;
    private int lastTimeMouseClickY;

    public AntBrainProgramScreen(AntBrainProgramMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 1, 1);
        this.placedBlocks.putAll(menu.getPlacedBlocks());
        this.inventoryLabelY = -1000;
    }

    @Override
    protected void init() {
        super.init();
        this.leftPos = 0;
        this.topPos = 0;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        this.extractBlurredBackground(graphics);
        this.extractTransparentBackground(graphics);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        rebuildLayouts();
        drawCategories(graphics);
        drawPalette(graphics);
        drawCanvas(graphics);
        graphics.fill(0, 0, this.width, HEADER_HEIGHT, 0xD91E2430);
        graphics.text(this.font, Component.translatable("menu.littleant.ant_brain_program"), 12, 10, 0xFFFFFFFF, true);

        if (this.draggingOpcode != null) {
            BlockDefinition definition = ModuleRegistry.get(this.draggingOpcode);
            if (definition != null) {
                BlockRenderLayout preview = BlockRenderLayout.palette(definition, this::textWidth);
                drawBlock(graphics, preview, mouseX - lastTimeMouseClickX + draggingStartX + canvasLeft(), mouseY - lastTimeMouseClickY + draggingStartY + CANVAS_TOP, true);
            }
        }
    }

    private void rebuildLayouts() {
        paletteEntries.clear();
        if (!CATEGORIES.isEmpty()) {
            int y = HEADER_HEIGHT + PALETTE_HEADER_HEIGHT - paletteScroll;
            for (BlockDefinition definition : MODULES_BY_CATEGORY.getOrDefault(currentCategory(), List.of())) {
                BlockRenderLayout layout = BlockRenderLayout.palette(definition, this::textWidth);
                paletteEntries.add(new PaletteEntry(definition, SIDEBAR_WIDTH + 8, y, layout));
                y += layout.height() + LIST_GAP;
            }
        }

        canvasLayouts.clear();
        Set<UUID> nested = nestedBlockIds();
        int canvasLeft = canvasLeft();
        for (BrainBlock block : placedBlocks.values()) {
            if (nested.contains(block.id())) continue;
            BlockRenderLayout layout = BlockRenderLayout.block(block, placedBlocks,
                    canvasLeft + block.x(), CANVAS_TOP + block.y(), this::textWidth);
            if (layout != null) canvasLayouts.put(block.id(), layout);
        }
    }

    private Set<UUID> nestedBlockIds() {
        Set<UUID> result = new HashSet<>();
        for (BrainBlock block : placedBlocks.values()) {
            for (InputSlot input : block.inputs()) {
                collectNested(input.blockId(), input.type() == ValueType.BLOCK, result);
            }
        }
        return result;
    }

    private void collectNested(UUID id, boolean includeNextChain, Set<UUID> result) {
        UUID current = id;
        while (current != null && result.add(current)) {
            BrainBlock child = placedBlocks.get(current);
            if (child == null) return;
            for (InputSlot input : child.inputs()) {
                collectNested(input.blockId(), input.type() == ValueType.BLOCK, result);
            }
            current = includeNextChain ? child.next() : null;
        }
    }

    private void drawCategories(GuiGraphicsExtractor graphics) {
        graphics.fill(0, HEADER_HEIGHT, SIDEBAR_WIDTH, this.height, 0xD9181D27);
        for (int i = 0; i < CATEGORIES.size(); i++) {
            int y = 42 + i * 32;
            String category = CATEGORIES.get(i);
            int color = i == selectedCategory ? ModuleRegistry.categoryColor(category) : 0xFF303947;
            graphics.fill(6, y, SIDEBAR_WIDTH - 6, y + 26, color);
            graphics.text(this.font, Component.translatable(ModuleRegistry.categoryTranslationKey(category)),
                    10, y + 9, 0xFFFFFFFF, false);
        }
    }

    private void drawPalette(GuiGraphicsExtractor graphics) {
        int right = canvasLeft();
        graphics.fill(SIDEBAR_WIDTH, HEADER_HEIGHT, right, this.height, 0xE52B3340);
        graphics.enableScissor(SIDEBAR_WIDTH, HEADER_HEIGHT + PALETTE_HEADER_HEIGHT, right, this.height);
        for (PaletteEntry entry : paletteEntries) {
            if (entry.y() + entry.layout().height() >= HEADER_HEIGHT + PALETTE_HEADER_HEIGHT && entry.y() < this.height) {
                drawBlock(graphics, entry.layout(), entry.x(), entry.y(), false);
            }
        }
        graphics.disableScissor();
        graphics.fill(SIDEBAR_WIDTH, HEADER_HEIGHT, right, HEADER_HEIGHT + PALETTE_HEADER_HEIGHT, 0xE52B3340);
        graphics.text(this.font, Component.translatable(ModuleRegistry.categoryTranslationKey(currentCategory())),
                SIDEBAR_WIDTH + 8, 42, 0xFFFFFFFF, false);
    }

    private void drawCanvas(GuiGraphicsExtractor graphics) {
        int left = canvasLeft();
        graphics.fill(left, HEADER_HEIGHT, this.width, this.height, 0x94131A25);
        graphics.text(this.font, Component.literal("Canvas"), left + 12, 42, 0xFFDAE2F2, false);
        graphics.enableScissor(left, CANVAS_TOP, this.width, this.height);
        for (BlockRenderLayout layout : canvasLayouts.values()) drawBlock(graphics, layout, layout.x(), layout.y(), false);
        graphics.disableScissor();
    }

    private void drawBlock(GuiGraphicsExtractor graphics, BlockRenderLayout layout, int drawX, int drawY, boolean floating) {
        int dx = drawX - layout.x();
        int dy = drawY - layout.y();
        int color = floating ? fade(layout.definition().color()) : layout.definition().color();
        graphics.fill(drawX, drawY, drawX + layout.width(), drawY + layout.height(), color);
        graphics.fill(drawX, drawY, drawX + layout.width(), drawY + 1, lighten(color));

        for (BlockRenderLayout.Element element : layout.elements()) {
            int ex = drawX + element.x();
            int ey = drawY + element.y();
            if (element.kind() == BlockRenderLayout.ElementKind.INPUT) {
                if (element.nested() != null) {
                    drawBlock(graphics, element.nested(), ex, ey, floating);
                } else {
                    graphics.fill(ex, ey, ex + element.width(), ey + element.height(), 0xCC202631);
                    graphics.text(this.font, element.text(), ex + 6, ey + 5, 0xFFFFFFFF, false);
                }
            } else {
                graphics.text(this.font, element.text(), ex, ey, 0xFFFFFFFF, false);
            }
        }

        for (BlockRenderLayout.Body body : layout.bodies()) {
            int bx = drawX + body.x();
            int by = drawY + body.y();
            graphics.fill(bx, by, bx + body.width(), by + body.height(), 0x66202731);
            for (BlockRenderLayout child : body.children()) {
                drawBlock(graphics, child, child.x() + dx, child.y() + dy, floating);
            }
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        lastTimeMouseClickX = (int) event.x();
        lastTimeMouseClickY = (int) event.y();
        if (event.button() != 0) {
            return super.mouseClicked(event, doubleClick);
        }
        int x = (int) event.x();
        int y = (int) event.y();
        for (int i = 0; i < CATEGORIES.size(); i++) {
            if (inside(x, y, 6, 42 + i * 32, SIDEBAR_WIDTH - 12, 26)) {
                selectedCategory = i;
                paletteScroll = 0;
                return true;
            }
        }

        PaletteEntry paletteEntry = paletteEntryAt(x, y);
        if (paletteEntry != null) {
            draggingOpcode = paletteEntry.definition().opcode();
            draggingId = null;
            return true;
        }

        UUID blockId = canvasBlockAt(x, y);
        if (blockId != null) {
            BrainBlock block = placedBlocks.remove(blockId);
            draggingOpcode = block.opcode();
            draggingId = blockId;
            draggingStartX = block.x();
            draggingStartY = block.y();
            ClientPacketDistributor.sendToServer(new RemoveAntBrainBlockPayload(this.menu.containerId, blockId.toString()));
            return true;
        }
        return true;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        return draggingOpcode != null || super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (event.button() != 0 || draggingOpcode == null) return super.mouseReleased(event);
        BlockDefinition definition = ModuleRegistry.get(draggingOpcode);
        BlockRenderLayout preview = definition == null ? null : BlockRenderLayout.palette(definition, this::textWidth);
        int x = (int) event.x();
        int y = (int) event.y();
        if (preview != null && x >= canvasLeft() && y >= CANVAS_TOP) {
            int worldX = Math.max(0, x - canvasLeft() - preview.width() / 2);
            int worldY = Math.max(0, y - CANVAS_TOP - preview.headerHeight() / 2);
            UUID id = draggingId == null ? UUID.randomUUID() : draggingId;
            placedBlocks.put(id, new BrainBlock(draggingOpcode, worldX, worldY, id,
                    ModuleRegistry.createDefaultInputs(draggingOpcode), null, null));
            ClientPacketDistributor.sendToServer(new PlaceAntBrainBlockPayload(
                    this.menu.containerId, draggingOpcode, worldX, worldY, id.toString()));
        }
        draggingOpcode = null;
        draggingId = null;
        return true;
    }

    @Override
    public void mouseMoved(double x, double y) {
        mouseX = (int) x;
        mouseY = (int) y;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == 261) {
            UUID blockId = canvasBlockAt(mouseX, mouseY);
            if (blockId != null) {
                placedBlocks.remove(blockId);
                ClientPacketDistributor.sendToServer(new RemoveAntBrainBlockPayload(this.menu.containerId, blockId.toString()));
            }
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
        if (x >= SIDEBAR_WIDTH && x < canvasLeft()) {
            int contentHeight = paletteEntries.isEmpty() ? 0
                    : paletteEntries.getLast().y() + paletteEntries.getLast().layout().height() + paletteScroll;
            int visibleHeight = this.height - HEADER_HEIGHT - PALETTE_HEADER_HEIGHT;
            int maxScroll = Math.max(0, contentHeight - (HEADER_HEIGHT + PALETTE_HEADER_HEIGHT) - visibleHeight);
            paletteScroll = Math.max(0, Math.min(maxScroll, paletteScroll - (int) (scrollY * 16)));
            return true;
        }
        return super.mouseScrolled(x, y, scrollX, scrollY);
    }

    private PaletteEntry paletteEntryAt(int x, int y) {
        if (x < SIDEBAR_WIDTH || x >= canvasLeft() || y < HEADER_HEIGHT + PALETTE_HEADER_HEIGHT) return null;
        for (PaletteEntry entry : paletteEntries) {
            if (inside(x, y, entry.x(), entry.y(), entry.layout().width(), entry.layout().height())) return entry;
        }
        return null;
    }

    private UUID canvasBlockAt(int x, int y) {
        if (x < canvasLeft() || y < CANVAS_TOP) return null;
        List<Map.Entry<UUID, BlockRenderLayout>> entries = new ArrayList<>(canvasLayouts.entrySet());
        for (int i = entries.size() - 1; i >= 0; i--) {
            UUID hit = blockAtRecursive(entries.get(i).getValue(), x, y, 0, 0);
            if (hit != null) return hit;
        }
        return null;
    }

    private UUID blockAtRecursive(BlockRenderLayout layout, int x, int y, int dx, int dy) {
        for (BlockRenderLayout.Element element : layout.elements()) {
            if (element.nested() == null) continue;
            int nestedX = layout.x() + dx + element.x();
            int nestedY = layout.y() + dy + element.y();
            UUID hit = blockAtRecursive(element.nested(), x, y,
                    nestedX - element.nested().x(), nestedY - element.nested().y());
            if (hit != null) return hit;
        }
        for (BlockRenderLayout.Body body : layout.bodies()) {
            for (BlockRenderLayout child : body.children()) {
                UUID hit = blockAtRecursive(child, x, y, dx, dy);
                if (hit != null) return hit;
            }
        }
        return x >= layout.x() + dx && x < layout.x() + dx + layout.width()
                && y >= layout.y() + dy && y < layout.y() + dy + layout.height()
                ? layout.blockId() : null;
    }

    private int textWidth(Component text) {
        return this.font.width(text);
    }

    private int canvasLeft() {
        return Math.min(this.width - 120, SIDEBAR_WIDTH + PALETTE_WIDTH);
    }

    private String currentCategory() {
        if (CATEGORIES.isEmpty()) return "";
        selectedCategory = Math.max(0, Math.min(selectedCategory, CATEGORIES.size() - 1));
        return CATEGORIES.get(selectedCategory);
    }

    private static boolean inside(int mouseX, int mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    private static int fade(int color) {
        return (color & 0x00FFFFFF) | 0xDD000000;
    }

    private static int lighten(int color) {
        int r = Math.min(255, ((color >> 16) & 255) + 30);
        int g = Math.min(255, ((color >> 8) & 255) + 30);
        int b = Math.min(255, (color & 255) + 30);
        return (color & 0xFF000000) | r << 16 | g << 8 | b;
    }

    private record PaletteEntry(BlockDefinition definition, int x, int y, BlockRenderLayout layout) {}
}
