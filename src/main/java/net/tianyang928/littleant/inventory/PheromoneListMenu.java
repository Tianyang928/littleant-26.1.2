package net.tianyang928.littleant.inventory;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.tianyang928.littleant.block.ModBlocks;
import net.tianyang928.littleant.block_entity.PheromoneBlockEntity;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;

public class PheromoneListMenu extends AbstractContainerMenu {
    private final HashMap<Integer, Integer> pheromoneMap = new HashMap<>();
    ContainerLevelAccess access;

    // 客户端构造器
    public PheromoneListMenu(
            int containerId,
            Inventory inventory,
            RegistryFriendlyByteBuf data
    ) {
        super(ModMenus.PHEROMONE_LIST_MENU.get(), containerId);
        // deserialize hash
        HashMap<Integer, Integer> pheromoneMap_tmp = new HashMap<>();
        if(data != null){
            int index = 0;
            while(index < data.capacity()){
                pheromoneMap_tmp.put(data.readInt(), data.readInt());
                index += 8;
            }
        }

        this.access = ContainerLevelAccess.NULL;
        this.pheromoneMap.putAll(pheromoneMap_tmp);
        this.addStandardInventorySlots(inventory, 108, 84);
    }

    // 服务端构造器
    public PheromoneListMenu(
            int containerId,
            Inventory inventory,
            PheromoneBlockEntity blockEntity
    ) {
        super(ModMenus.PHEROMONE_LIST_MENU.get(), containerId);
        this.pheromoneMap.putAll(blockEntity.getPheromoneList());
        this.access = ContainerLevelAccess.create(
                                blockEntity.getLevel(),
                                blockEntity.getBlockPos()
                        );
        this.addStandardInventorySlots(inventory, 108, 84);
    }

    private PheromoneListMenu(
            int containerId,
            Inventory inventory,
            ContainerLevelAccess access
    ) {
        super(ModMenus.PHEROMONE_LIST_MENU.get(), containerId);
        this.access = access;
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

    public HashMap<Integer, Integer> getPheromoneList() {
        return this.pheromoneMap;
    }
}
