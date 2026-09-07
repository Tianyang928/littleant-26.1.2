package net.tianyang928.littleant.entity.ai.brain;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.List;

/** Resolves either a registry id or a tag id (with or without a leading '#'). */
public final class TagSupport {
    private TagSupport() {}
    private static Identifier id(String value) {
        if (value == null) return null;
        String v = value.trim();
        if (v.startsWith("#")) v = v.substring(1);
        try { return Identifier.tryParse(v); } catch (RuntimeException e) { return null; }
    }
    public static <T> List<T> values(Registry<T> registry, net.minecraft.resources.ResourceKey<Registry<T>> key, String value) {
        Identifier id = id(value);
        if (id == null) return List.of();
        List<T> result = new ArrayList<>();
        if (value != null && value.trim().startsWith("#") || (registry.getValue(id) == null)) {
            TagKey<T> tag = TagKey.create(key, id);
            for (Holder<T> holder : registry.getTagOrEmpty(tag)) result.add(holder.value());
        }
        if (result.isEmpty()) {
            T direct = registry.getValue(id);
            if (direct != null) result.add(direct);
        }
        return List.copyOf(result);
    }
    /** Strict tag lookup: never falls back to interpreting the tag name as a registry id. */
    public static <T> boolean contains(Registry<T> registry,
                                       net.minecraft.resources.ResourceKey<Registry<T>> key,
                                       T value, String tagName) {
        if (value == null) return false;
        Identifier tagId = id(tagName);
        if (tagId == null) return false;
        TagKey<T> tag = TagKey.create(key, tagId);
        for (Holder<T> holder : registry.getTagOrEmpty(tag)) {
            if (holder.value() == value) return true;
        }
        return false;
    }
    public static List<Block> blocks(String value) { return values(BuiltInRegistries.BLOCK, Registries.BLOCK, value); }
    public static List<Item> items(String value) { return values(BuiltInRegistries.ITEM, Registries.ITEM, value); }
    public static List<EntityType<?>> entities(String value) { return values(BuiltInRegistries.ENTITY_TYPE, Registries.ENTITY_TYPE, value); }
    public static boolean blockInTag(Block value, String tag) { return contains(BuiltInRegistries.BLOCK, Registries.BLOCK, value, tag); }
    public static boolean itemInTag(Item value, String tag) { return contains(BuiltInRegistries.ITEM, Registries.ITEM, value, tag); }
    public static boolean entityInTag(EntityType<?> value, String tag) { return contains(BuiltInRegistries.ENTITY_TYPE, Registries.ENTITY_TYPE, value, tag); }
}
