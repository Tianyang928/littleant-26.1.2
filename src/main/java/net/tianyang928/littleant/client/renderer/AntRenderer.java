package net.tianyang928.littleant.client.renderer;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.monster.zombie.ZombieModel;
import net.minecraft.client.renderer.entity.ArmorModelSet;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.resources.Identifier;
import net.tianyang928.littleant.LittleAnt;
import net.tianyang928.littleant.client.render_state.AntRenderState;
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
    public void extractRenderState(AntEntity entity, AntRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);

        // 从 entity 读取实体皮肤，写入 state
        state.skinName = entity.getSkinNameAccessor();
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