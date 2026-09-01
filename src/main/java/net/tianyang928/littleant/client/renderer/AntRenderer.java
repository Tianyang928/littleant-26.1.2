package net.tianyang928.littleant.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.ArmorModelSet;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.tianyang928.littleant.LittleAnt;
import net.tianyang928.littleant.client.renderstate.AntRenderState;
import net.tianyang928.littleant.entity.AntEntity;

public class AntRenderer extends HumanoidMobRenderer<AntEntity, AntRenderState, HumanoidModel<AntRenderState>> {

    public AntRenderer(EntityRendererProvider.Context context) {
        super(
                context,
                new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER)),
                0.5F
        );
        this.addLayer(new HumanoidArmorLayer(
                this,
                        ArmorModelSet.bake(ModelLayers.PLAYER_ARMOR, context.getModelSet(), HumanoidModel::new),
                        context.getEquipmentRenderer()));
    }

    @Override
    public AntRenderState createRenderState() {
        return new AntRenderState();
    }

    @Override
    protected HumanoidModel.ArmPose getArmPose(AntEntity ant, HumanoidArm arm) {
        ItemStack stack = ant.getItemHeldByArm(arm);
        if (stack.isEmpty()) return HumanoidModel.ArmPose.EMPTY;
        if (!ant.swinging && stack.getItem() instanceof CrossbowItem && CrossbowItem.isCharged(stack)) {
            return HumanoidModel.ArmPose.CROSSBOW_HOLD;
        }

        InteractionHand hand = arm == ant.getMainArm() ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
        if (!ant.isUsingItem() || ant.getUsedItemHand() != hand) return super.getArmPose(ant, arm);
        return switch (stack.getUseAnimation()) {
            case BLOCK -> HumanoidModel.ArmPose.BLOCK;
            case BOW -> HumanoidModel.ArmPose.BOW_AND_ARROW;
            case TRIDENT -> HumanoidModel.ArmPose.THROW_TRIDENT;
            case CROSSBOW -> HumanoidModel.ArmPose.CROSSBOW_CHARGE;
            case SPYGLASS -> HumanoidModel.ArmPose.SPYGLASS;
            case TOOT_HORN -> HumanoidModel.ArmPose.TOOT_HORN;
            case BRUSH -> HumanoidModel.ArmPose.BRUSH;
            case SPEAR -> HumanoidModel.ArmPose.SPEAR;
            default -> HumanoidModel.ArmPose.ITEM;
        };
    }

    @Override
    public void extractRenderState(AntEntity entity, AntRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);

        // 从 entity 读取实体皮肤，写入 state
        state.skinName = entity.getSkinNameAccessor();
    }

    @Override
    protected void setupRotations(AntRenderState state, PoseStack poseStack, float bodyRot, float entityScale) {
        super.setupRotations(state, poseStack, bodyRot, entityScale);
        if (state.swimAmount > 0.0F) {
            float targetXRot = state.isInWater ? -90.0F - state.xRot : -90.0F;
            poseStack.mulPose(Axis.XP.rotationDegrees(Mth.lerp(state.swimAmount, 0.0F, targetXRot)));
            if (state.isVisuallySwimming) {
                poseStack.translate(0.0F, -1.0F, 0.3F);
            }
        }
    }

    @Override
    public Identifier getTextureLocation(AntRenderState antRenderState) {
        String skinName = antRenderState.skinName;
        if(skinName.isEmpty()){
            skinName = "null";
        }
        return Identifier.fromNamespaceAndPath(
                LittleAnt.MOD_ID,
                "textures/entity/ant/" + skinName + ".png"
        );
    }
}
