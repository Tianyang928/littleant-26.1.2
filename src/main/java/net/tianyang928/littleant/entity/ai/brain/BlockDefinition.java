package net.tianyang928.littleant.entity.ai.brain;

import java.util.List;

public record BlockDefinition(String opcode, String category, BlockShape shape,
                              List<InputDefinition> inputs, String translationKey, int color, List<String> displayFormat) {
    public BlockDefinition {
        inputs = List.copyOf(inputs);
    }
}
