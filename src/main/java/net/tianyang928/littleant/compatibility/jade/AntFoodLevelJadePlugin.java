package net.tianyang928.littleant.compatibility.jade;

import net.minecraft.resources.Identifier;
import net.tianyang928.littleant.entity.AntEntity;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;
import net.tianyang928.littleant.LittleAnt;


@WailaPlugin
public class AntFoodLevelJadePlugin implements IWailaPlugin{
    static Identifier ANT_FOOD_LEVEL = Identifier.fromNamespaceAndPath(LittleAnt.MOD_ID, "textures/item/ant_spawn_egg.png");

    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerEntityDataProvider(AntFoodLevelJadeDataProvider.INSTANCE, AntEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerEntityComponent(AntFoodLevelJadeComponentProvider.INSTANCE, AntEntity.class);
    }
}
