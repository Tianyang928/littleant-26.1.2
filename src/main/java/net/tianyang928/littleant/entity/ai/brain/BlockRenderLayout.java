package net.tianyang928.littleant.entity.ai.brain;

import net.minecraft.network.chat.Component;
import net.tianyang928.littleant.LittleAnt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Immutable geometry shared by block rendering and hit testing. */
public record BlockRenderLayout(
        UUID blockId,
        BlockDefinition definition,
        int x,
        int y,
        int width,
        int height,
        int headerHeight,
        List<Element> elements,
        List<Body> bodies) {

    public static final int INDENT = 6;
    public static final int PADDING = 5;
    public static final int GAP = 2;
    public static final int MIN_COMMAND_HEIGHT = 16;
    public static final int MIN_REPORTER_HEIGHT = 14;
    public static final int EMPTY_BODY_HEIGHT = 28;
    public static final int MAX_HEADER_WIDTH = 188;

    public BlockRenderLayout {
        elements = List.copyOf(elements);
        bodies = List.copyOf(bodies);
    }

    @FunctionalInterface
    public interface TextMeasurer {
        int width(Component text);
    }

    public enum ElementKind { LABEL, INPUT }

    public record Element(ElementKind kind, String inputName, ValueType type, Component text,
                          int x, int y, int width, int height, BlockRenderLayout nested) {}

    public record Body(String inputName, int x, int y, int width, int height,
                       List<BlockRenderLayout> children) {
        public Body { children = List.copyOf(children); }
    }

    public boolean contains(int px, int py) {
        return px >= x && px < x + width && py >= y && py < y + height;
    }

    public static BlockRenderLayout palette(BlockDefinition definition, TextMeasurer measurer) {
        return build(null, definition, definition.inputs().stream()
                .map(input -> InputSlot.literal(input.name(), input.type(), input.defaultValue()))
                .toList(), Map.of(), 0, 0, measurer, new HashSet<>(),true);
    }

    public static BlockRenderLayout block(BrainBlock block, Map<UUID, BrainBlock> blocks,
                                          int x, int y, TextMeasurer measurer) {
        BlockDefinition definition = ModuleRegistry.get(block.opcode());
        if (definition == null) return null;
        return build(block.id(), definition, effectiveInputs(block, definition), blocks, x, y, measurer, new HashSet<>(),false);
    }

    private static BlockRenderLayout build(UUID blockId, BlockDefinition definition, List<InputSlot> inputs,
                                           Map<UUID, BrainBlock> blocks, int x, int y,
                                           TextMeasurer measurer, Set<UUID> active
                                            , boolean isPalette) {
        if (blockId != null && !active.add(blockId)) return cycleFallback(blockId, definition, x, y);
        Map<String, InputSlot> inputsByName = new HashMap<>();
        for (InputSlot input : inputs) inputsByName.put(input.name(), input);
        ArrayList<Element> elements = new ArrayList<>();
        ArrayList<String> bodyNames = new ArrayList<>();
        int cursorX = PADDING;
        int cursorY = PADDING;
        int lineHeight = 0;
        int inputIndex = 0;

        for (String token : ModuleRegistry.getDisplayFormat(definition.opcode())) {
            if ("{}".equals(token)) {
                try{
                    bodyNames.add(inputs.get(inputIndex).name());
                }
                catch (IndexOutOfBoundsException e){
                    LittleAnt.LOGGER.error("[BlockRenderLayout] IndexOutOfBoundsException: {}, opcode: {}", e.getMessage(), definition.opcode());
                }
                inputIndex++;
                continue;
            }
            if ("()".equals(token) || "<>".equals(token)) {
                InputSlot input = null;
                try {
                    input = inputs.get(inputIndex);
                }
                catch (IndexOutOfBoundsException e){
                    LittleAnt.LOGGER.error("[BlockRenderLayout] IndexOutOfBoundsException: {}, opcode: {}", e.getMessage(), definition.opcode());
                }
                if (input != null) {
                    BrainBlock nestedBlock = input.blockId() == null ? null : blocks.get(input.blockId());
                    BlockDefinition nestedDefinition = nestedBlock == null ? null : ModuleRegistry.get(nestedBlock.opcode());
                    BlockRenderLayout nested = nestedDefinition == null ? null : build(nestedBlock.id(), nestedDefinition,
                            effectiveInputs(nestedBlock, nestedDefinition), blocks, 0, 0, measurer, active, false);
                    Component value = Component.literal(input.value() == null ? "" : input.value());
                    int elementWidth = nested == null ? Math.max(20, measurer.width(value) + 6) : nested.width();
                    // Reserve the full line box for nested inputs. The nested
                    // block is drawn inside this slot and vertically centered
                    // by the screen renderer.
                    int elementHeight = nested == null ? 12 : Math.max(nested.height(), lineHeight);
                    int[] position = wrap(cursorX, cursorY, lineHeight, elementWidth, false);
                    cursorX = position[0]; cursorY = position[1]; lineHeight = position[2];
                    elements.add(new Element(ElementKind.INPUT, input.name(), input.type(), value,
                            cursorX, cursorY, elementWidth, elementHeight, nested));
                    cursorX += elementWidth + GAP;
                    lineHeight = Math.max(lineHeight, elementHeight);
                }
                inputIndex++;
                continue;
            }
            if ("2x2".equals(token) || "3x3".equals(token)) {
                int grid = token.charAt(0) - '0';
                for (int i = 0; i < grid * grid; i++) {
                    InputSlot input = inputsByName.get("slot" + i);
                    if (input == null) continue;
                    Component value = Component.literal(shortValue(input.value()));
                    int elementWidth = 36;
                    if (i % grid == 0) { cursorX = PADDING; cursorY += lineHeight + GAP; lineHeight = 0; }
                    elements.add(new Element(ElementKind.INPUT, input.name(), input.type(), value,
                            cursorX, cursorY, elementWidth, 12, null));
                    cursorX += elementWidth + GAP;
                    lineHeight = 12;
                }
                inputIndex++;
                continue;
            }

            Component text = Component.translatable(token);
            int elementWidth = measurer.width(text);
            int[] position = wrap(cursorX, cursorY, lineHeight, elementWidth, isPalette);
            cursorX = position[0]; cursorY = position[1]; lineHeight = position[2];
            elements.add(new Element(ElementKind.LABEL, null, null, text,
                    cursorX, cursorY + 5, elementWidth, 9, null));
            cursorX += elementWidth + GAP;
            lineHeight = Math.max(lineHeight, 12);
        }

        int headerWidth = PADDING * 2;
        for (Element element : elements) headerWidth = Math.max(headerWidth, element.x() + element.width() + PADDING);
        int minimumHeight = switch (definition.shape()) {
            case REPORTER, BOOLEAN -> MIN_REPORTER_HEIGHT;
            default -> MIN_COMMAND_HEIGHT;
        };
        int headerHeight = Math.max(minimumHeight, cursorY + lineHeight + PADDING);
        int totalWidth = headerWidth;
        int totalHeight = headerHeight;
        ArrayList<Body> bodies = new ArrayList<>();

        for (String bodyName : bodyNames) {
            InputSlot bodyInput = inputsByName.get(bodyName);
            Chain chain = measureChain(bodyInput == null ? null : bodyInput.blockId(), blocks,
                    x + INDENT, y + totalHeight, measurer, active);
            int bodyHeight = Math.max(EMPTY_BODY_HEIGHT, chain.height());
            int bodyWidth = Math.max(100, chain.width());
            bodies.add(new Body(bodyName, INDENT, totalHeight, bodyWidth, bodyHeight, chain.layouts()));
            totalWidth = Math.max(totalWidth, INDENT + bodyWidth + PADDING);
            totalHeight += bodyHeight + PADDING;
        }
        if (!bodyNames.isEmpty()) totalHeight += 10;

        if (blockId != null) active.remove(blockId);
        return new BlockRenderLayout(blockId, definition, x, y, totalWidth, totalHeight,
                headerHeight, elements, bodies);
    }

    private static int[] wrap(int cursorX, int cursorY, int lineHeight, int elementWidth, boolean isPalette) {
        if (cursorX > PADDING && cursorX + elementWidth + PADDING > MAX_HEADER_WIDTH && isPalette) {
            return new int[]{PADDING, cursorY + lineHeight + GAP, 0};
        }
        return new int[]{cursorX, cursorY, lineHeight};
    }

    private static Chain measureChain(UUID root, Map<UUID, BrainBlock> blocks, int x, int y,
                                      TextMeasurer measurer, Set<UUID> active) {
        if (root == null) return new Chain(List.of(), 0, 0);
        ArrayList<BlockRenderLayout> layouts = new ArrayList<>();
        Set<UUID> chainVisited = new HashSet<>();
        int currentY = y;
        int width = 0;
        UUID current = root;
        while (current != null && chainVisited.add(current)) {
            BrainBlock child = blocks.get(current);
            if (child == null) break;
            BlockDefinition childDefinition = ModuleRegistry.get(child.opcode());
            if (childDefinition == null) break;
            BlockRenderLayout layout = build(child.id(), childDefinition,
                    effectiveInputs(child, childDefinition), blocks, x, currentY, measurer, active, false);
            layouts.add(layout);
            width = Math.max(width, layout.width());
            currentY += layout.height();
            current = child.next();
        }
        return new Chain(layouts, width, currentY - y);
    }

    private static List<InputSlot> effectiveInputs(BrainBlock block, BlockDefinition definition) {
        Map<String, InputSlot> actual = new HashMap<>();
        for (InputSlot input : block.inputs()) actual.put(input.name(), input);
        return definition.inputs().stream().map(input -> actual.getOrDefault(input.name(),
                InputSlot.literal(input.name(), input.type(), input.defaultValue()))).toList();
    }

    private static BlockRenderLayout cycleFallback(UUID id, BlockDefinition definition, int x, int y) {
        return new BlockRenderLayout(id, definition, x, y, 80, MIN_COMMAND_HEIGHT,
                MIN_COMMAND_HEIGHT, List.of(), List.of());
    }

    private static String shortValue(String value) {
        if (value == null) return "";
        return value.length() <= 4 ? value : value.substring(0, 3) + "…";
    }

    private record Chain(List<BlockRenderLayout> layouts, int width, int height) {}
}
