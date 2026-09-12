package net.tianyang928.littleant.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.tianyang928.littleant.LittleAnt;
import net.neoforged.neoforge.registries.DeferredHolder;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(Registries.ENTITY_TYPE, LittleAnt.MOD_ID);

    public static final DeferredHolder<EntityType<?>, EntityType<AntEntity>> ANT = ENTITIES.register(
            "ant",
            () -> EntityType.Builder.of(AntEntity::new, MobCategory.MISC)
                    .sized(0.6F, 1.8F).eyeHeight(1.53F).clientTrackingRange(8).build("ant")
    );

    public static void register(IEventBus eventBus) {
        ENTITIES.register(eventBus);
    }
}
