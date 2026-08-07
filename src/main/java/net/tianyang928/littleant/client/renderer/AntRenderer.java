package net.tianyang928.littleant.client.renderer;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.Identifier;
import net.tianyang928.littleant.LittleAnt;
import net.tianyang928.littleant.client.render_state.AntRenderState;
import net.tianyang928.littleant.entity.AntEntity;
import java.util.Random;

public class AntRenderer extends HumanoidMobRenderer<AntEntity, AntRenderState, HumanoidModel<AntRenderState>> {
    private static final String[] characterNames = {
            "alex",
            "ari",
            "efe",
            "kai",
            "makena",
            "noor",
            "steve",
            "sunny",
            "zuri"
    };
    private static final Identifier[] TEXTURES = new Identifier[characterNames.length];
    static {
        for(int i = 0; i < characterNames.length; i++) {
            TEXTURES[i] = Identifier.fromNamespaceAndPath(
                    LittleAnt.MOD_ID,
                    "textures/entity/ant/" + characterNames[i] + ".png"
            );
        }
    }

    public AntRenderer(EntityRendererProvider.Context context) {
        super(
                context,
                new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER)),
                0.5F
        );
    }

    @Override
    public AntRenderState createRenderState() {
        return new AntRenderState();
    }

    @Override
    public void extractRenderState(AntEntity entity, AntRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);

        // 从 entity 读取实体名字，写入 state
        state.characterName = entity.updateCharacterName();
    }

    @Override
    public Identifier getTextureLocation(AntRenderState antRenderState) {
        int hash = antRenderState.characterName.getString().hashCode();
        hash = Math.abs(hash);
        hash %= TEXTURES.length;
        return TEXTURES[hash];
    }
}