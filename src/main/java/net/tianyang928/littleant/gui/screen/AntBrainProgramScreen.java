package net.tianyang928.littleant.gui.screen;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.tianyang928.littleant.LittleAnt;
import net.tianyang928.littleant.entity.AntEntity;
import net.tianyang928.littleant.gui.AntBrainProgramMenu;
import net.tianyang928.littleant.network.PlaceAntBrainBlockPayload;
import net.tianyang928.littleant.network.RemoveAntBrainBlockPayload;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Random;

/** Full-screen, translucent Scratch-inspired palette and canvas for an ant brain. */
public class AntBrainProgramScreen extends AbstractContainerScreen<AntBrainProgramMenu> {
    /** Placeholder identifiers reserved for the eventual hand-drawn block sprites. */
    @SuppressWarnings("unused")
    private static final Identifier BLOCK_SPRITE_ATLAS = Identifier.fromNamespaceAndPath(
            LittleAnt.MOD_ID, "textures/gui/ant_brain/block_identifiers.png");
    private static final int SIDEBAR_WIDTH = 50;
    private static final int PALETTE_WIDTH = 148;
    private static final int CANVAS_PADDING = 18;
    private static final int BLOCK_WIDTH = 132;
    private static final int BLOCK_HEIGHT = 24;
    private static final List<Category> CATEGORIES = List.of(
            new Category("behavior", "运动", 0xFF4C97FF, List.of("move_to", "break_block", "place_block")),
            new Category("control", "控制", 0xFFFFAB19, List.of("wait", "repeat", "if")),
            new Category("goal", "目标", 0xFF9966FF, List.of("craft_item", "find_nearest_block", "find_nearest_entity")),
            new Category("sense", "侦测", 0xFF5CB1D6, List.of("sense_pheromone")),
            new Category("variables", "变量", 0xFFFF8C1A, List.of("set_variable")));

    private int selectedCategory;
    private String draggingBlock;
    private int paletteScroll;
    private final LinkedHashMap<Integer, AntEntity.BrainBlock> placedBlocks = new LinkedHashMap<>();

    private int mouseX, mouseY;

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
        // Vanilla's in-world background supplies both translucency and the configured menu blur.
        this.extractBlurredBackground(graphics);
        this.extractTransparentBackground(graphics);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {

        drawCategories(graphics, mouseX, mouseY);
        drawPalette(graphics, mouseX, mouseY);
        drawCanvas(graphics, mouseX, mouseY);

        graphics.fill(0, 0, this.width, 28, 0xD91E2430);
        graphics.text(this.font, Component.translatable("menu.littleant.ant_brain_program"), 12, 10, 0xFFFFFFFF, true);
        //graphics.text(this.font, "拖动模块到右侧编程区域（暂不执行或组装）", 210, 10, 0xFFB8C3D9, false);

        if (this.draggingBlock != null) {
            String blockText = draggingBlock;
            if(this.draggingBlock.contains("-")) {
                blockText = this.draggingBlock.split("-")[0];
            }
            drawBlock(graphics, blockText, mouseX - BLOCK_WIDTH / 2, mouseY - BLOCK_HEIGHT / 2, colorFor(blockText), true);
        }
        //graphics.pose().popMatrix();
    }

    private void drawCategories(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.fill(0, 28, SIDEBAR_WIDTH, this.height, 0xD9181D27);
        for (int i = 0; i < CATEGORIES.size(); i++) {
            int y = 44 + i * 32;
            int color = i == this.selectedCategory ? CATEGORIES.get(i).color : 0xFF303947;
            graphics.fill(8, y, SIDEBAR_WIDTH - 8, y + 26, color);
            graphics.text(this.font, CATEGORIES.get(i).label, 16, y + 9, 0xFFFFFFFF, false);
        }
    }

    private void drawPalette(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.fill(SIDEBAR_WIDTH, 28, SIDEBAR_WIDTH + PALETTE_WIDTH, this.height, 0xE52B3340);
        Category category = CATEGORIES.get(this.selectedCategory);

        int y = 62 - this.paletteScroll;
        for (String id : category.blocks) {
            if(y<28 || y>this.height) {
                y += BLOCK_HEIGHT + 8;
                continue;
            }
            drawBlock(graphics, id, SIDEBAR_WIDTH + 8, y, category.color, false);
            y += BLOCK_HEIGHT + 8;
        }
        graphics.fill(SIDEBAR_WIDTH, 28, SIDEBAR_WIDTH + PALETTE_WIDTH, 62, 0xE52B3340);
        graphics.text(this.font, category.label + " 模块", SIDEBAR_WIDTH + 8, 42, 0xFFFFFFFF, false);
    }

    private void drawCanvas(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int x = SIDEBAR_WIDTH + PALETTE_WIDTH;
        graphics.fill(x, 28, this.width, this.height, 0x94131A25);
        graphics.text(this.font, "编程区域", x + CANVAS_PADDING, 42, 0xFFDAE2F2, false);
        for (AntEntity.BrainBlock block : this.placedBlocks.values()) {
            drawBlock(graphics, block.text(), x + block.x(), 58 + block.y(), colorFor(block.text()), false);
        }
    }

    private void drawBlock(GuiGraphicsExtractor graphics, String text, int x, int y, int color, boolean floating) {
        // Until art exists, this identifier atlas is the stable resource hook for every future sprite.
        graphics.fill(x, y, x + BLOCK_WIDTH, y + BLOCK_HEIGHT, floating ? 0xEEFFFFFF & color : color);
        graphics.fill(x + 6, y + BLOCK_HEIGHT - 3, x + 18, y + BLOCK_HEIGHT, 0xFF18202D);
        graphics.fill(x + BLOCK_WIDTH - 18, y + BLOCK_HEIGHT - 3, x + BLOCK_WIDTH - 6, y + BLOCK_HEIGHT, 0xFF18202D);
        graphics.text(this.font, Component.translatable("block.littleant.ant_brain." + text), x + 10, y + 8, 0xFFFFFFFF, false);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() != 0) {
            return true;
        }
        int x = (int) event.x();
        int y = (int) event.y();
        for (int i = 0; i < CATEGORIES.size(); i++) {
            if (inside(x, y, 8, 44 + i * 32, SIDEBAR_WIDTH - 16, 26)) {
                this.selectedCategory = i;
                this.paletteScroll = 0;
                return true;
            }
        }

        // draggingblock 可用于存储两种数据：模块id或模块文本
        String blockText = paletteBlockAt(x, y);
        if (blockText != null) {
            this.draggingBlock = blockText;
        }
        else {
            int blockId = canvasBlockAt(x, y);
            if(blockId != -1) {
                blockText = this.placedBlocks.get(blockId).text();
                this.draggingBlock = blockText + "-" + blockId;
                this.placedBlocks.remove(blockId);
                ClientPacketDistributor.sendToServer(new RemoveAntBrainBlockPayload(this.menu.containerId, blockId));
            }

        }
        return true;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        return this.draggingBlock != null || super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (event.button() == 0 && this.draggingBlock != null) {
            int canvasX = SIDEBAR_WIDTH + PALETTE_WIDTH;
            int x = (int) event.x();
            int y = (int) event.y();
            if (x >= canvasX && y >= 58) {
                int relativeX = Math.max(0, x - canvasX - BLOCK_WIDTH / 2);
                int relativeY = Math.max(0, y - 58 - BLOCK_HEIGHT / 2);
                int blockId = -1;
                if(this.draggingBlock.contains("-")) {
                    String [] result = this.draggingBlock.split("-");
                    this.draggingBlock = result[0];
                    blockId = Integer.parseInt(result[1]);
                    LittleAnt.LOGGER.info("[AntBrainProgramScreen] blockId {} ", blockId);
                }
                else {
                    Random random = new Random();
                    do {
                        blockId = random.nextInt(Integer.MAX_VALUE);
                    } while (this.placedBlocks.containsKey(blockId));

                }
                this.placedBlocks.put(blockId, new AntEntity.BrainBlock(this.draggingBlock, relativeX, relativeY, blockId));
                ClientPacketDistributor.sendToServer(new PlaceAntBrainBlockPayload(this.menu.containerId, this.draggingBlock, relativeX, relativeY, blockId));
            }
            this.draggingBlock = null;
            return true;
        }
        return true;
    }

    @Override
    public void mouseMoved(double x, double y) {
        this.mouseX = (int) x;
        this.mouseY = (int) y;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if(event.key() == 261) {
            int blockId = canvasBlockAt(this.mouseX, this.mouseY);
            if(blockId != -1) {
                this.placedBlocks.remove(blockId);
                ClientPacketDistributor.sendToServer(new RemoveAntBrainBlockPayload(this.menu.containerId, blockId));
            }
            return true;
        }
        super.keyPressed(event);
        return true;
    }

    @Override
    public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
        if (x >= SIDEBAR_WIDTH && x < SIDEBAR_WIDTH + PALETTE_WIDTH) {
            this.paletteScroll = Math.max(0, this.paletteScroll - (int) (scrollY * 12));
            return true;
        }
        return true;
    }

    private String paletteBlockAt(int x, int y) {
        if (x < SIDEBAR_WIDTH + 8 || x >= SIDEBAR_WIDTH + 8 + BLOCK_WIDTH || y < 62) {
            return null;
        }
        int index = (y - 62 + this.paletteScroll) / (BLOCK_HEIGHT + 8);
        List<String> blocks = CATEGORIES.get(this.selectedCategory).blocks;
        return index >= 0 && index < blocks.size() && (y - 62 + this.paletteScroll) % (BLOCK_HEIGHT + 8) < BLOCK_HEIGHT ? blocks.get(index) : null;
    }

    private int canvasBlockAt(int x, int y) {
        if (x < SIDEBAR_WIDTH + PALETTE_WIDTH || y < 58) {
            return -1;
        }
        for (AntEntity.BrainBlock block : this.placedBlocks.values()) {
            if (inside(x, y, block.x() + SIDEBAR_WIDTH + PALETTE_WIDTH, block.y() + 58, BLOCK_WIDTH, BLOCK_HEIGHT)) {
                return block.id();
            }
        }
        return -1;
    }

    private static boolean inside(int mouseX, int mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    private static int colorFor(String id) {
        return CATEGORIES.stream().filter(category -> category.blocks.contains(id)).findFirst().map(category -> category.color).orElse(0xFF6B778D);
    }

    private record Category(String id, String label, int color, List<String> blocks) {}
}
