package net.tianyang928.littleant.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.tianyang928.littleant.LittleAnt;
import net.tianyang928.littleant.entity.AntEntity;

public class AntRenderer extends HumanoidMobRenderer<AntEntity, HumanoidModel<AntEntity>> {

    public AntRenderer(EntityRendererProvider.Context context) {
        super(
                context,
                new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER)),
                0.5F
        );
        this.addLayer(new HumanoidArmorLayer(
                this,
                new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)),
                new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)),
                context.getModelManager()));
    }

    @Override
    public ResourceLocation getTextureLocation(AntEntity antEntity) {
        String skinName = antEntity.getSkinNameAccessor();
        if(skinName.isEmpty()){
            skinName = "null";
        }
        return ResourceLocation.fromNamespaceAndPath(
                LittleAnt.MOD_ID,
                "textures/entity/ant/" + skinName + ".png"
        );
    }

    @Override
    public void render(AntEntity ant, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        this.model.rightArmPose = getArmPose(ant, HumanoidArm.RIGHT);
        this.model.leftArmPose = getArmPose(ant, HumanoidArm.LEFT);
        super.render(ant, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    private HumanoidModel.ArmPose getArmPose(AntEntity ant, HumanoidArm arm) {
        InteractionHand hand = arm == ant.getMainArm() ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
        ItemStack stack = ant.getItemInHand(hand);
        if (stack.isEmpty()) return HumanoidModel.ArmPose.EMPTY;
        if (!ant.swinging && stack.getItem() instanceof CrossbowItem && CrossbowItem.isCharged(stack)) {
            return HumanoidModel.ArmPose.CROSSBOW_HOLD;
        }

        if (!ant.isUsingItem() || ant.getUsedItemHand() != hand) return HumanoidModel.ArmPose.EMPTY;
        return switch (stack.getUseAnimation()) {
            case BLOCK -> HumanoidModel.ArmPose.BLOCK;
            case BOW -> HumanoidModel.ArmPose.BOW_AND_ARROW;
            case SPEAR -> HumanoidModel.ArmPose.THROW_SPEAR;
            case CROSSBOW -> HumanoidModel.ArmPose.CROSSBOW_CHARGE;
            case SPYGLASS -> HumanoidModel.ArmPose.SPYGLASS;
            case TOOT_HORN -> HumanoidModel.ArmPose.TOOT_HORN;
            case BRUSH -> HumanoidModel.ArmPose.BRUSH;
            default -> HumanoidModel.ArmPose.ITEM;
        };
    }

    @Override
    protected void setupRotations(AntEntity entity, PoseStack poseStack, float bob, float yBodyRot, float partialTick, float scale) {
        super.setupRotations(entity, poseStack, bob, yBodyRot, partialTick, scale);
        if (entity.getSwimAmount(partialTick) > 0.0F) {
            float targetXRot = entity.isInWater() ? -90.0F - bob : -90.0F;
            poseStack.mulPose(Axis.XP.rotationDegrees(Mth.lerp(entity.getSwimAmount(partialTick), 0.0F, targetXRot)));
            if (entity.isVisuallySwimming()) {
                poseStack.translate(0.0F, -1.0F, 0.3F);
            }
        }
    }
}
