package net.tianyang928.littleant.attribute;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.tianyang928.littleant.LittleAnt;

public class ModAttributes {
    // 自定义属性：蚂蚁力量
    public static final Holder<Attribute> ANT_STRENGTH = register(
            "ant_strength",
            new RangedAttribute("attribute.littleant.ant_strength", 1.0, 0.0, 100.0)
                    .setSyncable(true)
    );

    // 自定义属性：蚂蚁速度加成
    public static final Holder<Attribute> ANT_SPEED_BONUS = register(
            "ant_speed_bonus",
            new RangedAttribute("attribute.littleant.ant_speed_bonus", 0.0, 0.0, 10.0)
                    .setSyncable(true)
    );

    private static Holder<Attribute> register(String name, Attribute attribute) {
        // 使用你的 mod ID 作为命名空间
        return Registry.registerForHolder(
                BuiltInRegistries.ATTRIBUTE,
                Identifier.fromNamespaceAndPath(LittleAnt.MOD_ID, name),
                attribute
        );
    }

    // 初始化方法（需要在主类中调用）
    public static void register() {
        // 这个方法会在类加载时触发静态字段初始化
    }
}