package net.tianyang928.littleant.compatibility.jade;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.tianyang928.littleant.entity.AntEntity;
import org.jspecify.annotations.Nullable;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.StreamServerDataProvider;

public class AntFoodLevelJadeDataProvider   implements StreamServerDataProvider<EntityAccessor, Integer> {
    public static final AntFoodLevelJadeDataProvider INSTANCE = new AntFoodLevelJadeDataProvider();

    @Override
    public @Nullable Integer streamData(EntityAccessor entityAccessor) {
        AntEntity antEntity;
        try {
            antEntity = (AntEntity)entityAccessor.getEntity();
        } catch (Exception e) {
            return null;
        }
        return antEntity.getFoodData().getFoodLevel();
    }

    @Override
    public StreamCodec<RegistryFriendlyByteBuf, Integer> streamCodec() {
        return ByteBufCodecs.VAR_INT.cast();
    }

    @Override
    public Identifier getUid() {
        return AntFoodLevelJadePlugin.ANT_FOOD_LEVEL;
    }
}
