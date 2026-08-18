package net.tianyang928.littleant.gui.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Inventory;
import net.tianyang928.littleant.LittleAnt;
import net.tianyang928.littleant.gui.AntInventoryMenu;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** A compact chest-style screen for editing an ant's carried items and armor. */
public class AntInventoryScreen extends AbstractContainerScreen<AntInventoryMenu> {
    private static final Identifier BACKGROUND = Identifier.fromNamespaceAndPath("littleant", "textures/gui/container/ant_inventory_background.png");

    public AntInventoryScreen(AntInventoryMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 174);
        this.inventoryLabelY = 79;
        this.inventoryLabelX = 82;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        graphics.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, x, y, 0, 0, this.imageWidth, this.imageHeight, 256, 256);

        if(this.menu.ant == null) {
            LittleAnt.LOGGER.info("[AntInventoryScreen] client ant is null");
            return;
        }
        extractEntityInInventoryFollowsMouse(graphics, x+26, y+18, x+77, y+86, 25, 0.1F, mouseX, mouseY, this.menu.ant);
    }

    public static void extractEntityInInventoryFollowsMouse(
            GuiGraphicsExtractor graphics, int x0, int y0, int x1, int y1, int size, float offsetY, float mouseX, float mouseY, LivingEntity entity
    ) {
        float centerX = (x0 + x1) / 2.0F;
        float centerY = (y0 + y1) / 2.0F;
        float xAngle = (float)Math.atan((centerX - mouseX) / 40.0F);
        float yAngle = (float)Math.atan((centerY - mouseY) / 40.0F);
        // Forge: Allow passing in direct angle components instead of mouse position
        renderEntityInInventoryFollowsAngle(graphics, x0, y0, x1, y1, size, offsetY, xAngle, yAngle, entity);
    }

    public static void renderEntityInInventoryFollowsAngle(
            GuiGraphicsExtractor graphics, int x0, int y0, int x1, int y1, int size, float offsetY, float xAngle, float yAngle, LivingEntity entity
    ) {
        Quaternionf rotation = new Quaternionf().rotateZ((float) Math.PI);
        Quaternionf xRotation = new Quaternionf().rotateX(yAngle * 20.0F * (float) (Math.PI / 180.0));
        rotation.mul(xRotation);
        EntityRenderState renderState = extractRenderState(entity);
        if (renderState instanceof LivingEntityRenderState livingRenderState) {
            livingRenderState.bodyRot = 180.0F + xAngle * 20.0F;
            livingRenderState.yRot = xAngle * 20.0F;
            if (livingRenderState.pose != Pose.FALL_FLYING) {
                livingRenderState.xRot = -yAngle * 20.0F;
            } else {
                livingRenderState.xRot = 0.0F;
            }

            livingRenderState.boundingBoxWidth = livingRenderState.boundingBoxWidth / livingRenderState.scale;
            livingRenderState.boundingBoxHeight = livingRenderState.boundingBoxHeight / livingRenderState.scale;
            livingRenderState.scale = 1.0F;
        }

        Vector3f translation = new Vector3f(0.0F, renderState.boundingBoxHeight / 2.0F + offsetY, 0.0F);
        graphics.entity(renderState, size, translation, rotation, xRotation, x0, y0, x1, y1);
    }

    private static EntityRenderState extractRenderState(LivingEntity entity) {
        EntityRenderDispatcher entityRenderDispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        EntityRenderer<? super LivingEntity, ?> renderer = entityRenderDispatcher.getRenderer(entity);
        EntityRenderState renderState = renderer.createRenderState(entity, 1.0F);
        renderState.shadowPieces.clear();
        renderState.outlineColor = 0;
        return renderState;
    }
}
