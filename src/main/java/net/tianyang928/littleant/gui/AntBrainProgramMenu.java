package net.tianyang928.littleant.gui;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.tianyang928.littleant.client.debug.AntDebugClientState;
import net.tianyang928.littleant.entity.AntEntity;
import net.tianyang928.littleant.entity.ai.brain.BrainBlock;
import net.tianyang928.littleant.entity.ai.brain.InputSlot;
import net.tianyang928.littleant.entity.ai.brain.ValueType;
import net.tianyang928.littleant.entity.ai.brain.ModuleRegistry;

import javax.annotation.Nullable;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.ArrayList;
import java.util.UUID;

/** Menu backing the visual, Scratch-like editor for one ant's brain. */
public class AntBrainProgramMenu extends AbstractContainerMenu {
    public static final Set<String> KNOWN_BLOCKS = ModuleRegistry.all().stream().map(d -> d.opcode()).collect(java.util.stream.Collectors.toUnmodifiableSet());
    @Nullable
    public final AntEntity ant;
    private final LinkedHashMap<UUID, BrainBlock> placedBlocks;
    public boolean debugOverlayEnabled;

    /** Client factory. The complete immutable editing snapshot follows the entity id. */
    public AntBrainProgramMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf data) {
        super(ModMenus.ANT_BRAIN_PROGRAM_MENU.get(), containerId);
        this.debugOverlayEnabled = data.readBoolean();
        int entityId = data.readVarInt();
        this.ant = inventory.player.level().getEntity(entityId) instanceof AntEntity found ? found : null;
        int count = data.readVarInt();
        LinkedHashMap<UUID, BrainBlock> blocks = new java.util.LinkedHashMap<>();
        for (int i = 0; i < count; i++) {
            String opcode = data.readUtf(64);
            int x = data.readVarInt();
            int y = data.readVarInt();
            UUID id = data.readUUID();
            int inputCount = data.readVarInt();
            ArrayList<InputSlot> inputs = new ArrayList<>();
            for (int j = 0; j < inputCount; j++) {
                String name = data.readUtf(32);
                ValueType type;
                try { type = ValueType.valueOf(data.readUtf(16)); } catch (IllegalArgumentException ex) { type = ValueType.ANY; }
                int valueLength = data.readVarInt();
                String value = data.readUtf(valueLength);
                UUID ref = data.readBoolean() ? data.readUUID() : null;
                inputs.add(new InputSlot(name, type, value.isEmpty() ? null : value, ref));
            }
            UUID next = data.readBoolean() ? data.readUUID() : null;
            UUID parent = data.readBoolean() ? data.readUUID() : null;
            blocks.put(id, new BrainBlock(opcode, x, y, id, inputs, next, parent));
        }
        this.placedBlocks = (LinkedHashMap<UUID, BrainBlock>)blocks.clone();
    }

    public AntBrainProgramMenu(int containerId, Inventory inventory, AntEntity ant) {
        super(ModMenus.ANT_BRAIN_PROGRAM_MENU.get(), containerId);
        this.ant = ant;
        this.placedBlocks = ant.getBrainBlocks();
        this.debugOverlayEnabled = AntDebugClientState.enabled();

        ant.setIsProgrammingBrain(inventory.player, true);
    }

    public LinkedHashMap<UUID, BrainBlock> getPlacedBlocks() {
        return this.placedBlocks;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int i) {
        return null;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.ant != null && this.ant.isAlive() && player.isWithinEntityInteractionRange(this.ant, 4.0);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        ant.setIsProgrammingBrain(null, false);
    }
}

