package net.tianyang928.littleant.entity;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.tianyang928.littleant.LittleAnt;
import net.tianyang928.littleant.entity.AntEntity;

import java.util.function.Supplier;

public class ModEntities {
    public static final DeferredRegister.Entities ENTITIES = DeferredRegister.createEntities(LittleAnt.MOD_ID);

    public static final Supplier<EntityType<AntEntity>> ANT_ENTITY = ENTITIES.register(
            "ant_entity",
            () -> EntityType.Builder.of(
                    AntEntity::new,
                    MobCategory.CREATURE
            )
                    .sized(0.6f, 1.8f)
                    .eyeHeight(1.5f)
                    .build(ResourceKey.create(
                            Registries.ENTITY_TYPE,
                            Identifier.fromNamespaceAndPath(LittleAnt.MOD_ID, "ant_entity")
                    ))
    );

}
