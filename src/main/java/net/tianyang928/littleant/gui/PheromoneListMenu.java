package net.tianyang928.littleant.gui;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.tianyang928.littleant.LittleAnt;
import net.tianyang928.littleant.block.ModBlocks;
import net.tianyang928.littleant.block_entity.PheromoneBlockEntity;

import java.util.LinkedHashMap;

public class PheromoneListMenu extends AbstractContainerMenu  {
    private final LinkedHashMap<Integer, Integer> pheromoneMap;
    ContainerLevelAccess access;
    PheromoneBlockEntity pheromoneBlockEntity;
    ContainerData containerData;

    // 客户端构造器
    public PheromoneListMenu(
            int containerId,
            Inventory inventory,
            RegistryFriendlyByteBuf data
    ) {
        super(ModMenus.PHEROMONE_LIST_MENU.get(), containerId);
        // deserialize hash
        LinkedHashMap<Integer, Integer> pheromoneMap_tmp = new LinkedHashMap<>();
        if(data != null){
            int index = 0;
            while(index < data.capacity()){
                int id = data.readInt();
                int amount = data.readInt();
                pheromoneMap_tmp.put(id, amount);
                index += 8;
                LittleAnt.LOGGER.info("[PheromoneListMenu] client:read: {} {}", id, amount);
            }
            LittleAnt.LOGGER.info("[PheromoneListMenu] client:read map: {}", pheromoneMap_tmp);
        }

        this.access = ContainerLevelAccess.NULL;
        this.pheromoneMap = pheromoneMap_tmp;
        this.containerData = new SimpleContainerData(2);
        this.addDataSlots(containerData);
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

        this.containerData = blockEntity;
        checkContainerDataCount(containerData, 2);
        this.addDataSlots(containerData);
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

    public LinkedHashMap<Integer, Integer> getPheromoneList() {
        return this.pheromoneMap;
    }

    // called in server side
    public void addNewPheromone(int id, int amount){
        if(amount < 0){
            return;
        }
        this.pheromoneMap.put(id, amount);
        this.pheromoneBlockEntity.set(0, id);
        this.pheromoneBlockEntity.set(1, amount);
        broadcastChanges();
        LittleAnt.LOGGER.info("[PheromoneListMenu] server:addNewPheromone: {} {}", id, amount);
        LittleAnt.LOGGER.info("[PheromoneListMenu] server:pheromoneMap: {}", this.pheromoneMap);
    }

    // called in server side
    public void updatePheromoneList(){
        int id = this.containerData.get(0);
        int amount = this.containerData.get(1);
        if(id == -1 || amount == -1){
            return;
        }
        this.pheromoneMap.put(id, amount);
//        LittleAnt.LOGGER.info("[PheromoneListMenu] client:updatePheromoneList: {} {}", id, amount);
//        LittleAnt.LOGGER.info("[PheromoneListMenu] client:pheromoneMap: {}", this.pheromoneMap);
    }
}
