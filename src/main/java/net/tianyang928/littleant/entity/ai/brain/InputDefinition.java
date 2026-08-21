package net.tianyang928.littleant.entity.ai.brain;

public record InputDefinition(String name, ValueType type, String defaultValue, boolean required) {
    public InputDefinition(String name, ValueType type, String defaultValue) {
        this(name, type, defaultValue, false);
    }
}
