package net.tianyang928.littleant.compatibility.jade;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

import java.util.Optional;

public class AntFoodLevelJadeComponentProvider implements IEntityComponentProvider {
    public static final AntFoodLevelJadeComponentProvider INSTANCE = new AntFoodLevelJadeComponentProvider();

    @Override
    public void appendTooltip(ITooltip iTooltip, EntityAccessor entityAccessor, IPluginConfig iPluginConfig) {
        Optional<Integer> foodLevel = AntFoodLevelJadeDataProvider.INSTANCE.decodeFromData(entityAccessor);
        if(foodLevel.isPresent()){
            iTooltip.add(Component.translatable("antentity.littleant.food_level", foodLevel.get()));
        }
    }

    @Override
    public Identifier getUid() {
        return AntFoodLevelJadePlugin.ANT_FOOD_LEVEL;
    }
}
