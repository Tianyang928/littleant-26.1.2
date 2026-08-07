package net.tianyang928.littleant.entity;

import java.util.function.Supplier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.tianyang928.littleant.LittleAnt;

public final class ModEntities {
    public static final DeferredRegister.Entities ENTITIES =
            DeferredRegister.createEntities(LittleAnt.MOD_ID);

    public static final Supplier<EntityType<AntEntity>> ANT = ENTITIES.registerEntityType(
            "ant",
            AntEntity::new,
            MobCategory.CREATURE,
            builder -> builder
                    .sized(0.6F, 1.8F)
                    .eyeHeight(1.53F)
                    .clientTrackingRange(8)
    );

    public static void register(IEventBus eventBus) {
        ENTITIES.register(eventBus);
    }
}