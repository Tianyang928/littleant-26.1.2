package net.tianyang928.littleant.inventory.screen;

import com.mojang.blaze3d.platform.cursor.CursorTypes;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.tianyang928.littleant.LittleAnt;
import net.tianyang928.littleant.inventory.PheromoneListMenu;

import java.util.HashMap;

public class PheromoneListScreen extends AbstractContainerScreen<PheromoneListMenu> {

    private static final Identifier BACKGROUND_LOCATION = Identifier.fromNamespaceAndPath(LittleAnt.MOD_ID,"textures/gui/container/pheromone_list_background.png");
    private static final Identifier SCROLLER_SPRITE = Identifier.fromNamespaceAndPath(LittleAnt.MOD_ID,"textures/gui/container/scroller.png");
    private static final Identifier SCROLLER_DISABLED_SPRITE = Identifier.fromNamespaceAndPath(LittleAnt.MOD_ID,"textures/gui/container/scroller_disabled.png");

    private int scrollOff;
    private int selectedItem;
    private boolean isDragging;
    // buttons rendered on screen
    private final PheromoneListScreen.PheromoneButton[] pheromoneButtons = new PheromoneListScreen.PheromoneButton[7];

    public PheromoneListScreen(PheromoneListMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 276, 166);
        this.inventoryLabelX = 107;
    }

    @Override
    protected void init() {
        super.init();
        int xo = (this.width - this.imageWidth) / 2;
        int yo = (this.height - this.imageHeight) / 2;
        int buttonY = yo + 16 + 2;

        for (int i = 0; i < 7; i++) {
            this.pheromoneButtons[i] = this.addRenderableWidget(new PheromoneButton(xo + 5, buttonY, i, button -> {
                if (button instanceof PheromoneButton) {
                    this.selectedItem = ((PheromoneButton)button).getIndex() + this.scrollOff;
                }
            }));
            buttonY += 20;
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractBackground(graphics, mouseX, mouseY, a);
        int xo = (this.width - this.imageWidth) / 2;
        int yo = (this.height - this.imageHeight) / 2;
        graphics.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND_LOCATION, xo, yo, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 512, 256);
    }

    private void extractScroller(GuiGraphicsExtractor graphics, int xo, int yo, int mouseX, int mouseY, HashMap<Integer, Integer> pheromones) {
        int steps = pheromones.size() + 1 - 7;
        if (steps > 1) {
            int leftOver = 139 - (27 + (steps - 1) * 139 / steps);
            int stepHeight = 1 + leftOver / steps + 139 / steps;
            int maxScrollerOff = 113;
            int scrollerYOff = Math.min(113, this.scrollOff * stepHeight);
            if (this.scrollOff == steps - 1) {
                scrollerYOff = 113;
            }

            int scrollerX = xo + 94;
            int scrollerY = yo + 18 + scrollerYOff;
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SCROLLER_SPRITE, scrollerX, scrollerY, 6, 27);
            if (mouseX >= scrollerX && mouseX < xo + 94 + 6 && mouseY >= scrollerY && mouseY <= scrollerY + 27) {
                graphics.requestCursor(this.isDragging ? CursorTypes.RESIZE_NS : CursorTypes.POINTING_HAND);
            }
        } else {
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SCROLLER_DISABLED_SPRITE, xo + 94, yo + 18, 6, 27);
        }
    }

    @Override
    public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractContents(graphics, mouseX, mouseY, a);
        HashMap<Integer, Integer> pheromones = this.menu.getPheromoneList();
        if (!pheromones.isEmpty()) {
            int xo = (this.width - this.imageWidth) / 2;
            int yo = (this.height - this.imageHeight) / 2;
            int pheromoneY = yo + 16 + 1;
            this.extractScroller(graphics, xo, yo, mouseX, mouseY, pheromones);
            int currentPheromoneIndex = 0;

            for (Integer pheromone : pheromones.keySet()) {
                if (!this.canScroll(pheromones.size()) || currentPheromoneIndex >= this.scrollOff && currentPheromoneIndex < 7 + this.scrollOff) {
                    int decorHeight = pheromoneY + 2;

                    String text = "Name: " + pheromone.toString() + " Amount: " + pheromones.get(pheromone).toString();
                    this.extractText(graphics, text, xo+10, decorHeight);
                    pheromoneY += 20;
                    currentPheromoneIndex++;
                } else {
                    currentPheromoneIndex++;
                }
            }

            for (PheromoneButton button : this.pheromoneButtons) {
                button.visible = button.index < pheromones.size();
            }
        }
    }

    private void extractText(GuiGraphicsExtractor graphics, String text, int xo, int yo){
        graphics.centeredText(
                this.font,
                text,
                xo,
                yo,
                0xFFFFFF
        );
    }

    private boolean canScroll(int numberOfOffers) {
        return numberOfOffers > 7;
    }

    @Override
    public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
        if (super.mouseScrolled(x, y, scrollX, scrollY)) {
            return true;
        } else {
            int numberOfOffers = this.menu.getPheromoneList().size();
            if (this.canScroll(numberOfOffers)) {
                int maxScrollOff = numberOfOffers - 7;
                this.scrollOff = Mth.clamp((int)(this.scrollOff - scrollY), 0, maxScrollOff);
            }

            return true;
        }
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        int numberOfOffers = this.menu.getPheromoneList().size();
        if (this.isDragging) {
            int fullScrollTopPos = this.topPos + 18;
            int fullScrollBottomPos = fullScrollTopPos + 139;
            int maxScrollOff = numberOfOffers - 7;
            float scrolling = ((float)event.y() - fullScrollTopPos - 13.5F) / (fullScrollBottomPos - fullScrollTopPos - 27.0F);
            scrolling = scrolling * maxScrollOff + 0.5F;
            this.scrollOff = Mth.clamp((int)scrolling, 0, maxScrollOff);
            return true;
        } else {
            return super.mouseDragged(event, dx, dy);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        int xo = (this.width - this.imageWidth) / 2;
        int yo = (this.height - this.imageHeight) / 2;
        if (this.canScroll(this.menu.getPheromoneList().size())
                && event.x() > xo + 94
                && event.x() < xo + 94 + 6
                && event.y() > yo + 18
                && event.y() <= yo + 18 + 139 + 1) {
            this.isDragging = true;
        }

        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        this.isDragging = false;
        return super.mouseReleased(event);
    }

    private class PheromoneButton extends Button.Plain {
        final int index;

        public PheromoneButton(int x, int y, int index, Button.OnPress onPress) {
            super(x, y, 88, 20, CommonComponents.EMPTY, onPress, DEFAULT_NARRATION);
            this.index = index;
            this.visible = false;
        }

        public int getIndex() {
            return this.index;
        }
    }
}
