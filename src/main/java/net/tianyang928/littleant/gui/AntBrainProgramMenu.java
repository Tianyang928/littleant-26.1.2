package net.tianyang928.littleant.gui;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.tianyang928.littleant.entity.AntEntity;

import javax.annotation.Nullable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

/** Menu backing the visual, Scratch-like editor for one ant's brain. */
public class AntBrainProgramMenu extends AbstractContainerMenu {
    public static final Set<String> KNOWN_BLOCKS = Set.of(
            "move_to", "break_block", "place_block", "craft_item", "wait", "repeat", "if", "find_nearest_block", "find_nearest_entity", "sense_pheromone", "set_variable");
    @Nullable
    public final AntEntity ant;
    private final LinkedHashMap<Integer, AntEntity.BrainBlock> placedBlocks;

    /** Client factory. The complete immutable editing snapshot follows the entity id. */
    public AntBrainProgramMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf data) {
        super(ModMenus.ANT_BRAIN_PROGRAM_MENU.get(), containerId);
        int entityId = data.readVarInt();
        this.ant = inventory.player.level().getEntity(entityId) instanceof AntEntity found ? found : null;
        int count = data.readVarInt();
        LinkedHashMap<Integer, AntEntity.BrainBlock> blocks = new java.util.LinkedHashMap<>();
        for (int i = 0; i < count; i++) {
            String text = data.readUtf(64);
            int x = data.readVarInt();
            int y = data.readVarInt();
            int id = data.readVarInt();
            blocks.put(id, new AntEntity.BrainBlock(text, x, y, id));
        }
        this.placedBlocks = (LinkedHashMap<Integer, AntEntity.BrainBlock>)blocks.clone();
    }

    public AntBrainProgramMenu(int containerId, Inventory inventory, AntEntity ant) {
        super(ModMenus.ANT_BRAIN_PROGRAM_MENU.get(), containerId);
        this.ant = ant;
        this.placedBlocks = ant.getBrainBlocks();
    }

    public LinkedHashMap<Integer, AntEntity.BrainBlock> getPlacedBlocks() {
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
}
