package net.tianyang928.littleant.gui;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.tianyang928.littleant.block.ModBlocks;
import net.tianyang928.littleant.blockentity.PheromoneBlockEntity;

import java.util.LinkedHashMap;

public class PheromoneListMenu extends AbstractContainerMenu  {
    private final LinkedHashMap<String, Integer> pheromoneMap;
    private final ContainerLevelAccess access;
    private final PheromoneBlockEntity pheromoneBlockEntity;

    // 客户端构造器
    public PheromoneListMenu(
            int containerId,
            Inventory inventory,
            RegistryFriendlyByteBuf data
    ) {
        super(ModMenus.PHEROMONE_LIST_MENU.get(), containerId);
        LinkedHashMap<String, Integer> pheromoneMap_tmp = new LinkedHashMap<>();
        if (data != null) {
            int count = data.readVarInt();
            if (count < 0 || count > 256) {
                throw new IllegalArgumentException("Invalid pheromone entry count: " + count);
            }
            for (int i = 0; i < count; i++) {
                String id = data.readUtf(64);
                int amount = data.readVarInt();
                pheromoneMap_tmp.put(id, amount);
            }
        }

        this.access = ContainerLevelAccess.NULL;
        this.pheromoneMap = pheromoneMap_tmp;
        this.pheromoneBlockEntity = null;
    }

    // 服务端构造器
    public PheromoneListMenu(
            int containerId,
            Inventory inventory,
            PheromoneBlockEntity blockEntity
    ) {
        super(ModMenus.PHEROMONE_LIST_MENU.get(), containerId);
        this.pheromoneMap = blockEntity.getPheromoneList();
        this.access = ContainerLevelAccess.create(
                                blockEntity.getLevel(),
                                blockEntity.getBlockPos()
                        );

        this.pheromoneBlockEntity = blockEntity;
    }

    @Override
    public boolean stillValid(Player player) {
        return AbstractContainerMenu.stillValid(
                access,
                player,
                ModBlocks.PHEROMONE_BLOCK.get()
        );
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
    }

    public LinkedHashMap<String, Integer> getPheromoneList() {
        return this.pheromoneMap;
    }

    /** Returns whether this server menu displays the supplied block entity. */
    public boolean isViewing(PheromoneBlockEntity blockEntity) {
        return this.pheromoneBlockEntity == blockEntity;
    }

    public PheromoneBlockEntity getPheromoneBlockEntity() {
        return this.pheromoneBlockEntity;
    }

    /** Applies a server-authoritative edit. */
    public boolean setPheromone(String id, int amount) {
        if (id.isBlank() || id.length() > 64 || amount < 0 || this.pheromoneBlockEntity == null) {
            return false;
        }
        if (this.pheromoneMap.size() >= 256 && !this.pheromoneMap.containsKey(id)) {
            return false;
        }
        this.pheromoneMap.put(id, amount);
        this.pheromoneBlockEntity.setChanged();
        return true;
    }

    /** Applies an update received from the server on the client menu. */
    public void updatePheromone(String id, int amount) {
        if (id.isBlank() || amount < 0) {
            return;
        }
        this.pheromoneMap.put(id, amount);
    }
}
