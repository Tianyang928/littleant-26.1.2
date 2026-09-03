package net.tianyang928.littleant.entity.ai.brain;

import java.util.UUID;

/** A literal value or a reference to another block supplied to an input. */
public record InputSlot(String name, ValueType type, String value, UUID blockId) {
    public static InputSlot literal(String name, ValueType type, String value) {
        return new InputSlot(name, type, value, null);
    }
    public static InputSlot block(String name, ValueType type, UUID id) {
        return new InputSlot(name, type, "", id);
    }
}
