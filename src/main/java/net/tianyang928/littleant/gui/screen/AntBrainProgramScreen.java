package net.tianyang928.littleant.gui.screen;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.tianyang928.littleant.LittleAnt;
import net.tianyang928.littleant.entity.ai.brain.*;
import net.tianyang928.littleant.gui.AntBrainProgramMenu;
import net.tianyang928.littleant.network.UpdateAntBrainProgramPayload;

import java.util.*;

/** Registry-driven visual editor with graph-aware dragging, snapping and editable literal inputs. */
public class AntBrainProgramScreen extends AbstractContainerScreen<AntBrainProgramMenu> {
    private static final float TEXT_SCALE = 0.75f;
    private static final int SIDEBAR_WIDTH=66, PALETTE_WIDTH=198, HEADER_HEIGHT=28, PALETTE_HEADER_HEIGHT=34, CANVAS_TOP=58, LIST_GAP=8;
    private static final int STACK_SNAP_DISTANCE=18, INPUT_SNAP_DISTANCE=18;
    private static final Map<String,List<BlockDefinition>> MODULES_BY_CATEGORY=ModuleRegistry.byCategory();
    private static final List<String> CATEGORIES=List.copyOf(MODULES_BY_CATEGORY.keySet());
    private int selectedCategory,paletteScroll,dragOffsetX,dragOffsetY,dragX,dragY;
    private int mouseX, mouseY;
    private String draggingOpcode; private UUID draggingId; private boolean draggingFromPalette;
    private final LinkedHashMap<UUID,BrainBlock> placedBlocks=new LinkedHashMap<>();
    private final List<PaletteEntry> paletteEntries=new ArrayList<>();
    private final LinkedHashMap<UUID,BlockRenderLayout> canvasLayouts=new LinkedHashMap<>();
    private final Map<InputKey,ScalableEditBox> inputBoxes=new LinkedHashMap<>(); private SnapTarget snapTarget;
    //private int scaledMouseX, scaledMouseY;

    public AntBrainProgramScreen(AntBrainProgramMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 1, 1);
        placedBlocks.putAll(menu.getPlacedBlocks());
        inventoryLabelY = -1000;
    }

    @Override
    protected void init() {
        super.init();
        leftPos = topPos = 0;
        titleLabelY = -1000;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int x, int y, float p) {
        extractBlurredBackground(g);
        extractTransparentBackground(g);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float p) {
        mouseX = mx;
        mouseY = my;
        if (draggingOpcode != null) {
            dragX = mx - dragOffsetX;
            dragY = my - dragOffsetY;
        }
        rebuildLayouts();
        drawCategories(g);
        g.fill(0, 0, width, HEADER_HEIGHT, 0x661E2430);
        drawScaledText(g,Component.translatable("menu.littleant.ant_brain_program"), 12, 10, 1.0f,0xFFFFFFFF, true);
        drawPalette(g);
        drawCanvas(g);
        BlockRenderLayout preview = draggingLayout();
        snapTarget = preview == null ? null : findSnapTarget(preview, dragX, dragY);
        if (snapTarget != null) drawOutline(g, snapTarget.x(), snapTarget.y(), snapTarget.width(), snapTarget.height());
        if (preview != null)
            drawDraggingChain(g, snapTarget == null ? dragX : snapTarget.x(), snapTarget == null ? dragY : snapTarget.y());

        //drawInputBoxBackgrounds(g);
        super.extractRenderState(g, mx, my, p);
        syncInputBoxes();
    }

    private void rebuildLayouts() {
        paletteEntries.clear();
        if (!CATEGORIES.isEmpty()) {
            int yOffset = 8;
            int y = HEADER_HEIGHT + PALETTE_HEADER_HEIGHT - paletteScroll;
            for (BlockDefinition d : MODULES_BY_CATEGORY.getOrDefault(currentCategory(), List.of())) {
                BlockRenderLayout l = BlockRenderLayout.palette(d, this::textWidth);
                paletteEntries.add(new PaletteEntry(d, SIDEBAR_WIDTH + 8, y + yOffset, l));
                y += l.height() + LIST_GAP;
            }
        }
        canvasLayouts.clear();
        Set<UUID> nested = nestedBlockIds(), incomingNext = new HashSet<>();
        for (BrainBlock b : placedBlocks.values()) if (b.next() != null) incomingNext.add(b.next());
        if (draggingId != null) nested.addAll(ownedIds(draggingId));
        Set<UUID> laidOut = new HashSet<>();
        for (BrainBlock root : placedBlocks.values())
            if (!nested.contains(root.id()) && !incomingNext.contains(root.id())) {
                int x = canvasLeft() + root.x(), y = HEADER_HEIGHT + root.y();
                UUID current = root.id();
                while (current != null && laidOut.add(current) && !nested.contains(current)) {
                    BrainBlock b = placedBlocks.get(current);
                    if (b == null) break;
                    BlockRenderLayout l = BlockRenderLayout.block(b, placedBlocks, x, y, this::textWidth);
                    if (l == null) break;
                    canvasLayouts.put(b.id(), l);
                    y += l.height();
                    current = b.next();
                }
            }
    }

    private Set<UUID> nestedBlockIds() {
        Set<UUID> r = new HashSet<>();
        for (BrainBlock b : placedBlocks.values()) for (InputSlot i : b.inputs()) collectOwned(i.blockId(), r);
        return r;
    }

    private Set<UUID> ownedIds(UUID root) {
        Set<UUID> r = new HashSet<>();
        collectOwned(root, r);
        return r;
    }

    private Set<UUID> nestedOwnedIds(UUID root) {
        Set<UUID> r = new HashSet<>();
        if (root == null || !r.add(root)) return r;
        BrainBlock b = placedBlocks.get(root);
        if (b != null) for (InputSlot input : b.inputs()) collectOwned(input.blockId(), r);
        return r;
    }

    private void collectOwned(UUID id, Set<UUID> r) {
        if (id == null || !r.add(id)) return;
        BrainBlock b = placedBlocks.get(id);
        if (b == null) return;
        collectOwned(b.next(), r);
        for (InputSlot i : b.inputs()) collectOwned(i.blockId(), r);
    }

    private void drawCategories(GuiGraphicsExtractor g) {
        g.fill(0, HEADER_HEIGHT, SIDEBAR_WIDTH, height, 0xD9181D27);
        for (int i = 0; i < CATEGORIES.size(); i++) {
            int y = 42 + i * 32;
            g.fill(6, y, SIDEBAR_WIDTH - 6, y + 26, i == selectedCategory ? ModuleRegistry.categoryColor(CATEGORIES.get(i)) : 0xFF303947);
            drawScaledText(g, Component.translatable(ModuleRegistry.categoryTranslationKey(CATEGORIES.get(i))), 10, y + 9, 1.0f,0xFFFFFFFF, false);
        }
    }

    private void drawPalette(GuiGraphicsExtractor g) {
        int right = canvasLeft();
        g.fill(SIDEBAR_WIDTH, HEADER_HEIGHT, right, height, 0xE52B3340);
        g.enableScissor(SIDEBAR_WIDTH, HEADER_HEIGHT + PALETTE_HEADER_HEIGHT, right, height);
        for (PaletteEntry e : paletteEntries)
            if (e.y() + e.layout().height() >= HEADER_HEIGHT + PALETTE_HEADER_HEIGHT && e.y() < height)
                drawBlock(g, e.layout(), e.x(), e.y(), false);
        g.disableScissor();
        g.fill(SIDEBAR_WIDTH, HEADER_HEIGHT, right, PALETTE_HEADER_HEIGHT, 0xE52B3340);
        drawScaledText(g, Component.translatable(ModuleRegistry.categoryTranslationKey(currentCategory())), SIDEBAR_WIDTH + 8, 42, 1.0f,0xFFFFFFFF, false);
    }

    private void drawCanvas(GuiGraphicsExtractor g) {
        int left = canvasLeft();
        g.fill(left, HEADER_HEIGHT, width, height, 0x94131A25);
        drawScaledText(g, Component.literal("Canvas"), left + 12, 42, 1.0f, 0xFFDAE2F2, false);
        g.enableScissor(left, HEADER_HEIGHT, width, height);
        //boolean floating = draggingOpcode != null;
        for (BlockRenderLayout l : canvasLayouts.values()) drawBlock(g, l, l.x(), l.y(), false);
        g.disableScissor();
    }

    private void drawBlock(GuiGraphicsExtractor g, BlockRenderLayout l, int x, int y, boolean floating) {
        int xd2 = x + l.width() - l.height() / 2;
        int dx = x - l.x(), dy = y - l.y(), color = floating ? fade(l.definition().color()) : l.definition().color();

        if(l.definition().shape() == BlockShape.BOOLEAN){
            g.fill(x + l.height()/2, y, xd2, y + l.height(), color);
            drawTriangle(g, x, y, l.height(), -1, color, 0x00,lighten(color));
            drawTriangle(g, xd2, y, l.height(), 1, color, 0x00,lighten(color));
            g.fill(x + l.height()/2,y+l.height()-1,xd2,y+l.height(),lighten(color));
            g.fill(x + l.height()/2, y, xd2, y + 1, lighten(color));
        }
        else if(l.definition().shape() == BlockShape.REPORTER){
            int xd4 = x + l.width() - l.height() / 4;
            g.fill(x + l.height()/4, y, xd4, y + l.height(), color);
            drawSemicircle(g, x, y, l.height(), -1, color, 0x00,lighten(color));
            drawSemicircle(g, xd4, y, l.height(), 1, color, 0x00,lighten(color));
            g.fill(x + l.height()/4,y+l.height()-1,xd4,y+l.height(),lighten(color));
            g.fill(x + l.height()/4, y, xd4, y + 1, lighten(color));
        }
        else{
            g.fill(x , y, x + l.width(), y + l.height(), color);
            g.fill(x, y, x + l.width(), y + 1, lighten(color));
            g.fill(x,y,x+1,y+l.height(),lighten(color));
            g.fill(x,y+l.height()-1,x + l.width(),y+l.height(),lighten(color));
            g.fill(x + l.width(),y,x+l.width()+1,y+l.height(),lighten(color));
            if(l.definition().shape() == BlockShape.HAT){
                drawSemicircle(g, x, y-l.height()/4, l.height(), 0, color, 0x00,lighten(color));
            }
        }
        int inputIndex = 0;
        for (BlockRenderLayout.Element e : l.elements()) {
            int ex = x + e.x(), ey = y + e.y();
            if (e.kind() == BlockRenderLayout.ElementKind.INPUT) {
                if (e.nested() != null) {
                    // Nested blocks occupy the input slot from its left edge and
                    // are vertically centered when the slot is taller than the
                    // nested layout.
                    int nestedY = ey + Math.max(0, (e.height() - e.nested().height()) / 2);
                    drawBlock(g, e.nested(), ex, nestedY, floating);
                }
                else {
                    ValueType inputValueType = l.definition().inputs().get(inputIndex).type();
                    int exd2 = ex + e.width() - e.height() / 2;
                    int exd4 = ex + e.width() - e.height() / 4;
                    if(inputValueType == ValueType.BOOLEAN){
                        g.fill(ex + e.height()/2, ey, exd2, ey + e.height(), 0xCC202631);
                        drawTriangle(g, ex, ey, e.height(), -1, 0xCC202631, 0x00, 0x00);
                        drawTriangle(g, exd2, ey, e.height(), 1, 0xCC202631, 0x00, 0x00);
                    }
                    else{
                        g.fill(ex + e.height()/4, ey, exd4, ey + e.height(), 0xCC202631);
                        drawSemicircle(g, ex, ey, e.height(), -1, 0xCC202631, 0x00, 0x00);
                        drawSemicircle(g, exd4, ey, e.height(), 1, 0xCC202631, 0x00, 0x00);
                    }
                    drawScaledText(g, e.text(), ex + 3, ey + 5, TEXT_SCALE,0xFFFFFFFF, false);
                }
                inputIndex++;
            } else {
                drawScaledText(g, e.text(), ex, ey, TEXT_SCALE, 0xFFFFFFFF, false);
            }
        }
        for (BlockRenderLayout.Body b : l.bodies()) {
            int bx = x + b.x(), by = y + b.y();
            g.fill(bx, by, bx + b.width(), by + b.height(), 0x66202731);
            for (BlockRenderLayout c : b.children()) drawBlock(g, c, c.x() + dx, c.y() + dy, floating);
        }
    }

    private void drawOutline(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        g.fill(x - 2, y - 2, x + w + 2, y, 0xFFFFFFFF);
        g.fill(x - 2, y + h, x + w + 2, y + h + 2, 0xFFFFFFFF);
        g.fill(x - 2, y, x, y + h, 0xFFFFFFFF);
        g.fill(x + w, y, x + w + 2, y + h, 0xFFFFFFFF);
    }
    // 画上下对称的等腰45度三角形
    private void drawTriangle(GuiGraphicsExtractor g, int x, int y, int h, int direction, int triangleColor, int backgroundColor, int borderColor) {
        // direction 为1时，画右半三角形
        g.fill(x, y, x + h/2, y + h, backgroundColor);
        if(direction == -1) {
            for(int i = 0; i < h/2; i++) {
                g.fill(x+i,y+h/2-i,x+i+1,y+h/2+i,triangleColor);
                g.fill(x+i,y+h/2-i,x+i+1,y+h/2-i+1, borderColor);
                g.fill(x+i,y+h/2+i,x+i+1,y+h/2+i+1, borderColor);
            }
        }
        else if(direction == 1) {
            for(int i = 0; i < h/2; i++) {
                g.fill(x+i,y+i,x+i+1,y+h-i,triangleColor);
                g.fill(x+i,y+i,x+i+1,y+i+1, borderColor);
                g.fill(x+i,y+h-i-1,x+i+1,y+h-i, borderColor);
            }
        }
    }

    private void drawSemicircle(GuiGraphicsExtractor g, int x, int y, int h, int direction, int circleColor, int backgroundColor, int borderColor) {
        // direction 为1时，画右半圆
        g.fill(x, y, x + h/4, y + h, backgroundColor);
        if(direction == -1) {
            for(int i = h/4; i < h/2; i++) {
                g.fill(x+i-h/4,y+h/2-i,x+i+1-h/4,y+h/2+i,circleColor);
                g.fill(x+i-h/4,y+h/2-i-1,x+i+1-h/4,y+h/2-i, borderColor);
                g.fill(x+i-h/4,y+h/2+i,x+i+1-h/4,y+h/2+i+1, borderColor);
            }
        }
        else if(direction == 1) {
            for(int i = 0; i <= h/4; i++) {
                g.fill(x+i,y+i,x+i+1,y+h-i,circleColor);
                g.fill(x+i,y+i,x+i+1,y+i+1, borderColor);
                g.fill(x+i,y+h-i-1,x+i+1,y+h-i, borderColor);
            }
        }
        else if(direction == 0){
            for(int i = h/4; i <= h/2; i++) {
                g.fill(x+h/2-i,y+i-h/4,x+h/2+i,y+i-h/4+1,circleColor);
                g.fill(x+h/2-i,y+i-h/4-1,x+h/2-i,y+i-h/4, borderColor);
                g.fill(x+h/2+i,y+i-h/4,x+h/2+i,y+i-h/4+1, borderColor);
            }
        }
    }

    private void drawDraggingChain(GuiGraphicsExtractor g, int x, int y) {
        if (draggingId == null) {
            BlockRenderLayout l = draggingLayout();
            if (l != null) drawBlock(g, l, x, y, true);
            return;
        }
        Set<UUID> seen = new HashSet<>();
        UUID current = draggingId;
        int cy = y;
        while (current != null && seen.add(current)) {
            BrainBlock b = placedBlocks.get(current);
            if (b == null) break;
            BlockRenderLayout l = BlockRenderLayout.block(b, placedBlocks, x, cy, this::textWidth);
            if (l == null) break;
            drawBlock(g, l, x, cy, true);
            cy += l.height();
            current = b.next();
        }
    }

    private void drawScaledText(
            GuiGraphicsExtractor graphics,
            Component text,
            int x,
            int y,
            float scale,
            int color,
            boolean shadow
    ) {
        graphics.pose().pushMatrix();
        graphics.pose().translate(x, y);
        graphics.pose().scale(scale, scale);
        graphics.text(this.font, text, 0, 0, color, shadow);
        graphics.pose().popMatrix();
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent e, boolean d) {
        if (e.button() != 0) return super.mouseClicked(e, d);
        // AbstractContainerScreen consumes every left click, even when no child widget was hit.
        // Only delegate when the pointer is actually over a visible literal input.
        int x = (int) e.x(), y = (int) e.y();
        if (inputBoxAt(x, y) != null) return super.mouseClicked(e, d);
        clearFocus();
        for (int i = 0; i < CATEGORIES.size(); i++) {
            if (inside(x, y, 3, 42 + i * 32, SIDEBAR_WIDTH - 12, 26)) {
                selectedCategory = i;
                paletteScroll = 0;
                return true;
            }
        }
        PaletteEntry pe = paletteEntryAt(x, y);
        if (pe != null) {
            draggingOpcode = pe.definition().opcode();
            draggingId = null;
            draggingFromPalette = true;
            dragX = x - pe.layout().width() / 2;
            dragY = y - pe.layout().headerHeight() / 2;
            dragOffsetX = x - dragX;
            dragOffsetY = y - dragY;
            hideInputBoxes();
            return true;
        }
        LayoutHit hit = canvasBlockAt(x, y);
        if (hit != null) {
            draggingId = hit.id();
            draggingOpcode = placedBlocks.get(hit.id()).opcode();
            draggingFromPalette = false;
            dragX = hit.x();
            dragY = hit.y();
            dragOffsetX = x - dragX;
            dragOffsetY = y - dragY;
            detachIncoming(draggingId);
            hideInputBoxes();
            return true;
        }
        return true;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent e, double dx, double dy) {
        return draggingOpcode != null || super.mouseDragged(e, dx, dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent e) {
        if (e.button() != 0 || draggingOpcode == null) return super.mouseReleased(e);
        int x = (int) e.x(), y = (int) e.y();
        if (inside(x, y, canvasLeft(), HEADER_HEIGHT, width - canvasLeft(), height - HEADER_HEIGHT)) {
            if (draggingId == null) {
                draggingId = UUID.randomUUID();
                placedBlocks.put(draggingId, new BrainBlock(draggingOpcode, 0, 0, draggingId, ModuleRegistry.createDefaultInputs(draggingOpcode), null, null));
            }
            if (snapTarget != null) {
                applySnap(snapTarget);
            }
            else {
                setBlockPosition(draggingId, Math.max(0, dragX - canvasLeft()), Math.max(0, dragY - HEADER_HEIGHT), null);
            }
            sendProgram();
        } else if (!draggingFromPalette && draggingId != null) {
            for (UUID id : ownedIds(draggingId)) placedBlocks.remove(id);
            sendProgram();
        }
        draggingOpcode = null;
        draggingId = null;
        snapTarget = null;
        draggingFromPalette = false;
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent e) {
        if (e.key() == 261 && draggingId != null) {
            for (UUID id : ownedIds(draggingId)) placedBlocks.remove(id);
            sendProgram();
            draggingOpcode = null;
            draggingId = null;
            snapTarget = null;
            return true;
        }
        if (e.key() == 261 && !(getFocused() instanceof EditBox)) {
            LayoutHit hit = canvasBlockAt(mouseX, mouseY);
            if (hit != null) {
                deleteBlockAndNested(hit.id());
                sendProgram();
                return true;
            }
        }
        // EditBox handles printable characters in charTyped(), but returns false
        // here. Consume the inventory key first so AbstractContainerScreen does
        // not close this menu before the character event arrives.
        if (getFocused() instanceof EditBox editBox
                && editBox.isFocused()
                && minecraft.options.keyInventory.isActiveAndMatches(InputConstants.getKey(e))) {
            return true;
        }
        return super.keyPressed(e);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double sx, double sy) {
        if (x >= SIDEBAR_WIDTH && x < canvasLeft()) {
            int ch = paletteEntries.isEmpty() ? 0 : paletteEntries.getLast().y() + paletteEntries.getLast().layout().height() + paletteScroll, vh = height - HEADER_HEIGHT - PALETTE_HEADER_HEIGHT;
            paletteScroll = Math.max(0, Math.min(Math.max(0, ch - (HEADER_HEIGHT + PALETTE_HEADER_HEIGHT) - vh), paletteScroll - (int) (sy * 16)));
            return true;
        }
        return super.mouseScrolled(x, y, sx, sy);
    }

    @Override
    public void mouseMoved(double x, double y) {
        super.mouseMoved(x, y);
    }

    private BlockRenderLayout draggingLayout() {
        BlockDefinition d = ModuleRegistry.get(draggingOpcode);
        if (d == null) return null;
        if (draggingId == null) return BlockRenderLayout.palette(d, this::textWidth);
        BrainBlock b = placedBlocks.get(draggingId);
        return b == null ? null : BlockRenderLayout.block(b, placedBlocks, dragX, dragY, this::textWidth);
    }

    private SnapTarget findSnapTarget(BlockRenderLayout p, int x, int y) {
        boolean value = p.definition().shape() == BlockShape.BOOLEAN || p.definition().shape() == BlockShape.REPORTER;
        SnapTarget best = null;
        for (BlockRenderLayout r : canvasLayouts.values())
            best = better(best, findSnapRecursive(r, p, x, y, value, 0, 0));
        return best;
    }

    private SnapTarget findSnapRecursive(BlockRenderLayout l, BlockRenderLayout p, int px, int py, boolean value, int dx, int dy) {
        int lx = l.x() + dx, ly = l.y() + dy, dragHeight = draggingChainHeight();
        SnapTarget best = null;
        for (BlockRenderLayout.Element e : l.elements()) {
            int ex = lx + e.x(), ey = ly + e.y();
            if (e.nested() == null && value && compatible(p.definition().shape(), e.type())) {
                int dist = distance(px + p.width() / 2, py + p.headerHeight() / 2, ex + e.width() / 2, ey + e.height() / 2);
                if (dist <= INPUT_SNAP_DISTANCE)
                    best = new SnapTarget(SnapKind.INPUT, l.blockId(), e.inputName(), ex, ey, e.width(), e.height(), dist);
            } else if (e.nested() != null) {
                int nestedY = ey + Math.max(0, (e.height() - e.nested().height()) / 2);
                best = better(best, findSnapRecursive(e.nested(), p, px, py, value,
                        ex - e.nested().x(), nestedY - e.nested().y()));
            }
        }
        for (BlockRenderLayout.Body b : l.bodies()) {
            int bx = lx + b.x(), by = ly + b.y();
            if (!value && stackable(p.definition().shape()) && b.children().isEmpty()) {
                int dist = distance(px, py, bx, by);
                if (dist <= STACK_SNAP_DISTANCE)
                    best = better(best, new SnapTarget(SnapKind.BODY, l.blockId(), b.inputName(), bx, by, Math.max(p.width(), 24), dragHeight, dist));
            }
            for (BlockRenderLayout c : b.children())
                best = better(best, findSnapRecursive(c, p, px, py, value, dx, dy));
        }
        if (!value && stackable(p.definition().shape()) && stackable(l.definition().shape())) {
            int below = distance(px, py, lx, ly + l.height());
            if (below <= STACK_SNAP_DISTANCE)
                best = better(best, new SnapTarget(SnapKind.AFTER, l.blockId(), null, lx, ly + l.height(), p.width(), dragHeight, below));
            int above = distance(px, py + dragHeight, lx, ly);
            if (above <= STACK_SNAP_DISTANCE)
                best = better(best, new SnapTarget(SnapKind.BEFORE, l.blockId(), null, lx, ly - dragHeight, p.width(), dragHeight, above));
        }
        else if(!value && stackable(p.definition().shape())&& l.definition().shape() == BlockShape.HAT){
            int below = distance(px, py, lx, ly + l.height());
            if(below <= STACK_SNAP_DISTANCE)
                best = better(best, new SnapTarget(SnapKind.AFTER, l.blockId(), null, lx, ly + l.height(), p.width(), dragHeight, below));
        }
        return best;
    }

    private int draggingChainHeight() {
        BlockRenderLayout first = draggingLayout();
        if (first == null || draggingId == null) return first == null ? 0 : first.height();
        int h = 0;
        Set<UUID> seen = new HashSet<>();
        UUID current = draggingId;
        while (current != null && seen.add(current)) {
            BrainBlock b = placedBlocks.get(current);
            if (b == null) break;
            BlockRenderLayout l = BlockRenderLayout.block(b, placedBlocks, 0, 0, this::textWidth);
            if (l == null) break;
            h += l.height();
            current = b.next();
        }
        return h;
    }

    private void applySnap(SnapTarget t) {
        if (t.kind() == SnapKind.INPUT || t.kind() == SnapKind.BODY) {
            BrainBlock o = placedBlocks.get(t.owner());
            ValueType type = t.kind() == SnapKind.BODY ? ValueType.BLOCK : inputType(o, t.input());
            LittleAnt.LOGGER.info("[AntBrainProgramScreen] applySnap: t.input() {}, draggingId {}",t.input, draggingId);
            placedBlocks.put(o.id(), copy(o, o.x(), o.y(), replaceInput(o.inputs(), t.input(), InputSlot.block(t.input(), type, draggingId)), o.next(), o.parent()));
            setBlockPosition(draggingId, 0, 0, o.id());
            return;
        }
        BrainBlock a = placedBlocks.get(t.owner());
        UUID tail = chainTail(draggingId);
        if (t.kind() == SnapKind.AFTER) {
            UUID old = a.next();
            placedBlocks.put(a.id(), copy(a, a.x(), a.y(), a.inputs(), draggingId, a.parent()));
            BrainBlock tb = placedBlocks.get(tail);
            placedBlocks.put(tail, copy(tb, tb.x(), tb.y(), tb.inputs(), old, a.parent()));
            setBlockPosition(draggingId, 0, 0, a.parent());
        } else {
            replaceIncoming(a.id(), draggingId);
            BrainBlock tb = placedBlocks.get(tail);
            placedBlocks.put(tail, copy(tb, tb.x(), tb.y(), tb.inputs(), a.id(), a.parent()));
            setBlockPosition(draggingId, a.x(), a.y(), a.parent());
        }
    }

    private void detachIncoming(UUID id) {
        replaceIncoming(id, null);
        BrainBlock b = placedBlocks.get(id);
        if (b != null) placedBlocks.put(id, copy(b, b.x(), b.y(), b.inputs(), b.next(), null));
    }

    private void replaceIncoming(UUID oldId, UUID newId) {
        for (BrainBlock b : List.copyOf(placedBlocks.values())) {
            boolean changed = false;
            UUID next = b.next();
            if (oldId.equals(next)) {
                next = newId;
                changed = true;
            }
            List<InputSlot> inputs = new ArrayList<>();
            for (InputSlot s : b.inputs()) {
                if (oldId.equals(s.blockId())) {
                    inputs.add(newId == null ? InputSlot.literal(s.name(), s.type(), "") : InputSlot.block(s.name(), s.type(), newId));
                    changed = true;
                } else inputs.add(s);
            }
            if (changed) placedBlocks.put(b.id(), copy(b, b.x(), b.y(), inputs, next, b.parent()));
        }
    }

    private void deleteBlockAndNested(UUID id) {
        BrainBlock removed = placedBlocks.get(id);
        if (removed == null) return;

        Set<UUID> removedIds = nestedOwnedIds(id);
        UUID successor = removed.next();
        for (BrainBlock block : List.copyOf(placedBlocks.values())) {
            if (removedIds.contains(block.id())) continue;
            UUID next = block.next();
            UUID parent = block.parent();
            boolean changed = false;
            if (id.equals(next)) {
                next = successor;
                changed = true;
            } else if (removedIds.contains(next)) {
                next = null;
                changed = true;
            }
            if (removedIds.contains(parent)) {
                parent = null;
                changed = true;
            }
            List<InputSlot> inputs = new ArrayList<>();
            for (InputSlot input : block.inputs()) {
                if (removedIds.contains(input.blockId())) {
                    inputs.add(InputSlot.literal(input.name(), input.type(), ""));
                    changed = true;
                } else inputs.add(input);
            }
            if (changed) placedBlocks.put(block.id(), copy(block, block.x(), block.y(), inputs, next, parent));
        }
        for (UUID removedId : removedIds) placedBlocks.remove(removedId);
    }

    private void setBlockPosition(UUID id, int x, int y, UUID parent) {
        BrainBlock b = placedBlocks.get(id);
        placedBlocks.put(id, copy(b, x, y, b.inputs(), b.next(), parent));
        setChainParent(b.next(), parent, new HashSet<>());
    }

    private void setChainParent(UUID id, UUID parent, Set<UUID> seen) {
        if (id == null || !seen.add(id)) return;
        BrainBlock b = placedBlocks.get(id);
        if (b == null) return;
        placedBlocks.put(id, copy(b, b.x(), b.y(), b.inputs(), b.next(), parent));
        setChainParent(b.next(), parent, seen);
    }

    private UUID chainTail(UUID root) {
        Set<UUID> s = new HashSet<>();
        UUID c = root;
        while (placedBlocks.get(c).next() != null && s.add(c)) c = placedBlocks.get(c).next();
        return c;
    }

    private void syncInputBoxes() {
        Set<InputKey> visible = new HashSet<>();
        for (BlockRenderLayout l : canvasLayouts.values()) collectInputBoxes(l, 0, 0, visible);
        for (var e : inputBoxes.entrySet()) {
            e.getValue().visible = visible.contains(e.getKey()) && draggingOpcode == null;
        }
    }

    private ScalableEditBox inputBoxAt(double x, double y) {
        for (ScalableEditBox box : inputBoxes.values()) {
            if (box.visible && box.isMouseOver(x, y)) return box;
        }
        return null;
    }

    private void collectInputBoxes(BlockRenderLayout l, int dx, int dy, Set<InputKey> visible) {
        int lx = l.x() + dx, ly = l.y() + dy;
        int inputIndex = 0;
        for (BlockRenderLayout.Element e : l.elements()) {
            if(e.kind() == BlockRenderLayout.ElementKind.LABEL){
                continue;
            }
            ValueType type = l.definition().inputs().get(inputIndex).type();
            inputIndex++;

            int ex = lx + e.x(), ey = ly + e.y();
            if (e.nested() != null) {
                int nestedY = ey + Math.max(0, (e.height() - e.nested().height()) / 2);
                collectInputBoxes(e.nested(), ex - e.nested().x(), nestedY - e.nested().y(), visible);
            }
            else if (l.blockId() != null) {
                if(type == ValueType.BOOLEAN){
                    continue;
                }
                InputKey key = new InputKey(l.blockId(), e.inputName());
                visible.add(key);
                ScalableEditBox box = inputBoxes.get(key);
                if (box == null) {
                    // EditBox keeps its text metrics in unscaled (logical) pixels.
                    box = new ScalableEditBox(font, ex + 3, ey + 5, e.width(), e.height(), Component.literal(e.inputName()));
                    box.setMaxLength(256);
                    box.setBordered(false);
                    box.setTextColor(0xFFFFFFFF);
                    String initial = inputValue(l.blockId(), e.inputName());
                    box.setValue(initial == null ? "" : initial);
                    InputKey captured = key;
                    box.setResponder(v -> updateLiteral(captured, v));
                    inputBoxes.put(key, addRenderableWidget(box));
                }
                box.setX(ex + 3);
                box.setY(ey + 5);
                box.setWidth((int) Math.ceil(e.width() / TEXT_SCALE));
                box.setHeight((int) Math.ceil(e.height() / TEXT_SCALE));
                box.visible = draggingOpcode == null;
            }
        }
        for (BlockRenderLayout.Body b : l.bodies())
            for (BlockRenderLayout c : b.children()) collectInputBoxes(c, dx, dy, visible);
    }

    private void updateLiteral(InputKey key, String value) {
        BrainBlock b = placedBlocks.get(key.block());
        if (b == null) return;
        for (InputSlot i : b.inputs())
            if (i.name().equals(key.input()) && i.blockId() == null) {
                placedBlocks.put(b.id(), copy(b, b.x(), b.y(), replaceInput(b.inputs(), key.input(), InputSlot.literal(i.name(), i.type(), value)), b.next(), b.parent()));
                sendProgram();
                return;
            }
    }

    private void hideInputBoxes() {
        for (ScalableEditBox b : inputBoxes.values()) b.visible = false;
    }

    private void sendProgram() {
        ClientPacketDistributor.sendToServer(new UpdateAntBrainProgramPayload(menu.containerId, UpdateAntBrainProgramPayload.encode(placedBlocks)));
    }

    private PaletteEntry paletteEntryAt(int x, int y) {
        if (x < SIDEBAR_WIDTH || x >= canvasLeft() || y < HEADER_HEIGHT + PALETTE_HEADER_HEIGHT) return null;
        for (PaletteEntry e : paletteEntries)
            if (inside(x, y, e.x(), e.y(), e.layout().width(), e.layout().height())) return e;
        return null;
    }

    private LayoutHit canvasBlockAt(int x, int y) {
        List<BlockRenderLayout> roots = new ArrayList<>(canvasLayouts.values());
        for (int i = roots.size() - 1; i >= 0; i--) {
            LayoutHit h = blockAtRecursive(roots.get(i), x, y, 0, 0);
            if (h != null) return h;
        }
        return null;
    }

    private LayoutHit blockAtRecursive(BlockRenderLayout l, int x, int y, int dx, int dy) {
        int lx = l.x() + dx, ly = l.y() + dy;
        for (BlockRenderLayout.Element e : l.elements())
            if (e.nested() != null) {
                int nx = lx + e.x(), ny = ly + e.y();
                int nestedY = ny + Math.max(0, (e.height() - e.nested().height()) / 2);
                LayoutHit h = blockAtRecursive(e.nested(), x, y, nx - e.nested().x(), nestedY - e.nested().y());
                if (h != null) return h;
            }
        for (BlockRenderLayout.Body b : l.bodies())
            for (BlockRenderLayout c : b.children()) {
                LayoutHit h = blockAtRecursive(c, x, y, dx, dy);
                if (h != null) return h;
            }
        return inside(x, y, lx, ly, l.width(), l.height()) ? new LayoutHit(l.blockId(), lx, ly) : null;
    }

    private String inputValue(UUID id, String name) {
        BrainBlock b = placedBlocks.get(id);
        if (b != null) for (InputSlot s : b.inputs()) if (s.name().equals(name)) return s.value();
        return "";
    }

    private ValueType inputType(BrainBlock b, String name) {
        for (InputSlot s : b.inputs()) if (s.name().equals(name)) return s.type();
        return ValueType.ANY;
    }

    private static List<InputSlot> replaceInput(List<InputSlot> in, String name, InputSlot r) {
        return in.stream().map(i -> i.name().equals(name) ? r : i).toList();
    }

    private static BrainBlock copy(BrainBlock b, int x, int y, List<InputSlot> in, UUID next, UUID parent) {
        return new BrainBlock(b.opcode(), x, y, b.id(), in, next, parent);
    }

    private static boolean compatible(BlockShape d, ValueType t) {
        if(t == null){
            return false;
        }
        switch(t){
            case NUMBER, TEXT, LIST, BLOCK->{
                return d == BlockShape.REPORTER;
            }
            case BOOLEAN->{
                return d == BlockShape.BOOLEAN;
            }
            case ANY->{
                return true;
            }
        }
        return false;
    }

    private static boolean stackable(BlockShape s) {
        return s == BlockShape.COMMAND || s == BlockShape.C_SHAPE || s == BlockShape.E_SHAPE;
    }

    private static SnapTarget better(SnapTarget a, SnapTarget b) {
        return b == null || a != null && a.distance() <= b.distance() ? a : b;
    }

    private static int distance(int ax, int ay, int bx, int by) {
        int dx = ax - bx, dy = ay - by;
        return (int) Math.sqrt(dx * dx + dy * dy);
    }

    private int textWidth(Component t) {
        return (int) (font.width(t) * TEXT_SCALE);
    }

    private int canvasLeft() {
        return Math.min(width - 120, SIDEBAR_WIDTH + PALETTE_WIDTH);
    }

    private String currentCategory() {
        if (CATEGORIES.isEmpty()) return "";
        selectedCategory = Math.max(0, Math.min(selectedCategory, CATEGORIES.size() - 1));
        return CATEGORIES.get(selectedCategory);
    }

    private static boolean inside(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private static int fade(int c) {
        return c & 0x00FFFFFF | 0xDD000000;
    }

    private static int lighten(int c) {
        int r = Math.min(255, (c >> 16 & 255) + 30), g = Math.min(255, (c >> 8 & 255) + 30), b = Math.min(255, (c & 255) + 30);
        return c & 0xFF000000 | r << 16 | g << 8 | b;
    }

    private enum SnapKind {AFTER, BEFORE, INPUT, BODY}

    private record SnapTarget(SnapKind kind, UUID owner, String input, int x, int y, int width, int height,
                              int distance) {
    }

    private record PaletteEntry(BlockDefinition definition, int x, int y, BlockRenderLayout layout) {
    }

    private record LayoutHit(UUID id, int x, int y) {
    }

    private record InputKey(UUID block, String input) {
    }

    private class ScalableEditBox extends EditBox {

        public ScalableEditBox(Font font, int x, int y, int width, int height, Component narration) {
            super(font, x, y, (int) Math.ceil(width / TEXT_SCALE), (int) Math.ceil(height / TEXT_SCALE), narration);
            this.visible = true;
        }

        /**
         * The superclass stores logical (unscaled) dimensions, while the screen
         * receives mouse coordinates in rendered pixels.  Expose the rendered
         * dimensions to AbstractWidget's hit testing so hovering/clicking stops
         * at the visible edge of the field.
         */
        @Override
        public int getWidth() {
            return Math.round(super.getWidth() * TEXT_SCALE);
        }

        @Override
        public int getHeight() {
            return Math.round(super.getHeight() * TEXT_SCALE);
        }

        @Override
        public void extractWidgetRenderState(
                GuiGraphicsExtractor graphics,
                int mouseX,
                int mouseY,
                float partialTick
        ) {
            float x = this.getX();
            float y = this.getY();

            graphics.pose().pushMatrix();

            // 将缩放中心移动到输入框左上角
            graphics.pose().translate(x, y);
            graphics.pose().scale(TEXT_SCALE, TEXT_SCALE);
            graphics.pose().translate(-x, -y);

            int logicalMouseX = (int) (x + (mouseX - x) / TEXT_SCALE);
            int logicalMouseY = (int) (y + (mouseY - y) / TEXT_SCALE);
            super.extractWidgetRenderState(graphics, logicalMouseX, logicalMouseY, partialTick);

            graphics.pose().popMatrix();
        }

        @Override
        public boolean mouseClicked(MouseButtonEvent e, boolean d)  {
            // Hit testing must use rendered coordinates. Only EditBox's cursor
            // calculation needs the logical, inverse-scaled event.
            if (!this.isActive() || !this.isValidClickButton(e.buttonInfo()) || !this.isMouseOver(e.x(), e.y())) {
                return false;
            }
            this.playDownSound(Minecraft.getInstance().getSoundManager());
            this.onClick(toLogical(e), d);
            return true;
        }

        @Override
        public boolean mouseDragged(MouseButtonEvent e, double dx, double dy) {
            return super.mouseDragged(toLogical(e), dx / TEXT_SCALE, dy / TEXT_SCALE);
        }

        private MouseButtonEvent toLogical(MouseButtonEvent e) {
            int logicalX = (int) (this.getX() + (e.x() - this.getX()) / TEXT_SCALE);
            int logicalY = (int) (this.getY() + (e.y() - this.getY()) / TEXT_SCALE);
            return new MouseButtonEvent(logicalX, logicalY, e.buttonInfo());
        }
    }
}
