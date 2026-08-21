package net.tianyang928.littleant.entity.ai.brain;

import java.util.List;
import java.util.UUID;

public record BrainBlock(String opcode, int x, int y, UUID id, List<InputSlot> inputs, UUID next, UUID parent) {
        public BrainBlock(String opcode, int x, int y, UUID id) { this(opcode, x, y, id, List.of(), null, null); }
    }