package net.tianyang928.littleant.entity.ai.brain;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** Mutable graph container used by the editor and runtime. */
public final class BrainProgram {
    private final LinkedHashMap<UUID, ProgramBlock> blocks = new LinkedHashMap<>();
    public Collection<ProgramBlock> blocks() { return blocks.values(); }
    public ProgramBlock get(UUID id) { return blocks.get(id); }
    public void put(ProgramBlock block) { if (blocks.size() < 256) blocks.put(block.id(), block); }
    public void remove(UUID id) { blocks.remove(id); blocks.values().forEach(b -> b.disconnect(id)); }
    public Map<UUID, ProgramBlock> snapshot() { return Map.copyOf(blocks); }
    public UUID first() { return blocks.values().stream().filter(b -> b.parent() == null).findFirst().map(ProgramBlock::id).orElse(null); }

    public static final class ProgramBlock {
        private final UUID id;
        private final String opcode;
        private int x, y;
        private final ArrayList<InputSlot> inputs;
        private UUID next, parent;
        public ProgramBlock(UUID id, String opcode, int x, int y, Collection<InputSlot> inputs, UUID next, UUID parent) {
            this.id = id; this.opcode = opcode; this.x = x; this.y = y; this.inputs = new ArrayList<>(inputs); this.next = next; this.parent = parent;
        }
        public UUID id() { return id; } public String opcode() { return opcode; } public int x() { return x; } public int y() { return y; }
        public java.util.List<InputSlot> inputs() { return java.util.List.copyOf(inputs); } public UUID next() { return next; } public UUID parent() { return parent; }
        public void setNext(UUID next) { this.next = next; } public void setParent(UUID parent) { this.parent = parent; }
        public void disconnect(UUID removed) { if (removed.equals(next)) next = null; if (removed.equals(parent)) parent = null; inputs.removeIf(i -> removed.equals(i.blockId())); }
    }
}
