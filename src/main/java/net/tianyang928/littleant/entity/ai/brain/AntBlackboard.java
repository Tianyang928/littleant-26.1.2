package net.tianyang928.littleant.entity.ai.brain;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.WorldlyContainerHolder;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.tianyang928.littleant.block.ModBlocks;
import net.tianyang928.littleant.block.PheromoneBlock;
import net.tianyang928.littleant.blockentity.PheromoneBlockEntity;
import net.tianyang928.littleant.entity.AntEntity;

import java.util.*;

/** Shared, tick-local and persistent-by-runtime facts used by sense, control and goal modules. */
public final class AntBlackboard {
    private final AntEntity ant;

    private final LinkedHashMap<String, String> variables = new LinkedHashMap<>();
    private final LinkedHashMap<String, List<String>> lists = new LinkedHashMap<>();
    private final LinkedHashMap<String, String> permanentVariables = new LinkedHashMap<>();
    private final LinkedHashMap<String, List<String>> permanentLists = new LinkedHashMap<>();

    public AntBlackboard(AntEntity antEntity) {
        this.ant = antEntity;
    }

    public void scriptSwitchInventorySlot(int slot) {
        ant.setSelectedSlot(slot);
    }

    public void scriptJump() {
        if(ant.onGround()) {
            ant.jumpFromGround();
        }
        else if(ant.isInLiquid())
        {
            if (ant.isInLava()) {
                ant.jumpInFluid(Blocks.LAVA.defaultBlockState().getFluidState().getFluidType());
            }
            else {
                ant.jumpInFluid(Blocks.WATER.defaultBlockState().getFluidState().getFluidType());
            }
        }
    }

    public void scriptLookAt(double x, double y, double z) {
        ant.getLookControl().setLookAt(x, y, z);
    }

    public void scriptRotate(double angle) {
        ant.setYRot(ant.getYRot()+(float)angle);
    }

    public void scriptSay(String message) {
        if (message != null && !message.isBlank()) {
            //LittleAnt.LOGGER.info("[Ant {}] {}", ant.getUUID(), message.substring(0, Math.min(256, message.length())));
            Component combinedMessage = Component.literal("[Ant "+ Objects.requireNonNull(ant.getCustomName()).getString()+"] "+message);
            if (ant.level() instanceof ServerLevel serverLevel) {
                // 广播给服务器所有玩家
                serverLevel.getServer().getPlayerList().broadcastSystemMessage(combinedMessage, false);
            }
        }
    }

    public void scriptSetPheromone(String pheromone) {
        BlockPos result = this.ant.setFindBlockEntityTarget(ModBlocks.PHEROMONE_BLOCK.get());
        if(result == null || result.distSqr(this.ant.blockPosition()) > 6*6) {
            for(int offsetX = -1; offsetX <= 1; offsetX++) {
                for(int offsetY = -1; offsetY <= 1; offsetY++) {
                    for(int offsetZ = -1; offsetZ <= 1; offsetZ++) {
                        BlockPos pos = new BlockPos(this.ant.getBlockX() + offsetX, this.ant.getBlockY() + offsetY, this.ant.getBlockZ() + offsetZ);
                        if(this.ant.level().getBlockState(pos).isAir()) {
                            BlockState blockState = ModBlocks.PHEROMONE_BLOCK.get().defaultBlockState();
                            this.ant.level().setBlock(pos, blockState, 3);
                            PheromoneBlockEntity blockEntity = (PheromoneBlockEntity) this.ant.level().getBlockEntity(pos);
                            if(blockEntity != null) {
                                blockEntity.getPheromoneList().put(pheromone, 1);
                            }
                        }
                    }
                }
            }
        }
        else {
            PheromoneBlockEntity blockEntity = (PheromoneBlockEntity) this.ant.level().getBlockEntity(result);
            if(blockEntity != null) {
                int currentAmount = blockEntity.getPheromoneList().getOrDefault(pheromone, 0);
                blockEntity.getPheromoneList().put(pheromone, currentAmount + 1);
            }
        }
    }

    public void scriptSetRun(Boolean run) {
        if(run) {
            ant.speedModifier = 1.25F;
        } else {
            ant.speedModifier = 0.0F;
        }
    }

    public void scriptSetCrouching(Boolean crouching) {
        ant.isCrouching = crouching;
        if(crouching) {
            ant.speedModifier = 0.5F;
        } else {
            ant.speedModifier = 1.0F;
        }
    }


    // senses

    public String getLastHurtByEntity() {
        return String.valueOf(this.ant.lastHurtBy.getId());
    }

    public String getHealth() {
        return String.valueOf(this.ant.getHealth());
    }

    public String getFoodLevel() {
        return String.valueOf(this.ant.getFoodData().getFoodLevel());
    }

    public String distanceToTarget(double x, double y, double z) {
        return String.valueOf(Mth.sqrt((float) (this.ant.distanceToSqr(x, y, z))));
    }

    public String getBlock(double x, double y, double z) {
        return BuiltInRegistries.BLOCK.getKey(this.ant.level().getBlockState(new BlockPos((int) x, (int) y, (int) z)).getBlock()).toString();
    }

    public Boolean hasItemInInventory(String item) {
        for(int i = 0; i < this.ant.getInventory().getContainerSize(); i++) {
            if(BuiltInRegistries.ITEM.getKey(this.ant.getInventory().getSlot(i).get().getItem()).toString().equals(item)) {
                return true;
            }
        }
        return false;
    }

    public String getItemInInventory(double slot) {
        return BuiltInRegistries.ITEM.getKey(this.ant.getInventory().getSlot((int) slot).get().getItem()).toString();
    }

    public String getTime(){
        return String.valueOf(this.ant.level().getGameTime());
    }

    public Boolean isHurt() {
        return this.ant.level().getGameTime() - this.ant.lastHurtTime < 20;
    }

    public Boolean isOnFire() {
        return this.ant.isOnFire();
    }

    public Boolean isInWater() {
        return this.ant.isInWater();
    }

    public Boolean isUnderWater() {
        return this.ant.isUnderWater();
    }

    public String getX() {
        return String.valueOf(this.ant.getBlockX());
    }

    public String getY() {
        return String.valueOf(Math.ceil(this.ant.getY()));
    }

    public String getZ() {
        return String.valueOf(this.ant.getBlockZ());
    }

    public String getPos() {
        return this.ant.position().x + "," + this.ant.position().y + "," + this.ant.position().z;
    }

    public String findBlock(String block) {
        BlockPos result = this.ant.setFindBlockTarget(BuiltInRegistries.BLOCK.getValue(Identifier.tryParse(block)));
        if(result == null){
            return "";
        }
        return result.getX() + "," + result.getY() + "," + result.getZ();
    }

    public String findEntity(String entity) {
        int result = this.ant.setFindEntityTarget(BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.tryParse(entity)));
        if(result == -1){
            return "";
        }
        return String.valueOf(result);
    }

    public String findBlockEntity(String blockEntity) {
        BlockPos result = this.ant.setFindBlockEntityTarget(BuiltInRegistries.BLOCK.getValue(Identifier.tryParse(blockEntity)));
        if(result == null){
            return "";
        }
        return result.getX() + "," + result.getY() + "," + result.getZ();
    }

    public String findDrop(String item) {
        BlockPos result = this.ant.setFindDropTarget(BuiltInRegistries.ITEM.getValue(Identifier.tryParse(item)));
        if(result == null){
            return "";
        }
        return result.getX() + "," + result.getY() + "," + result.getZ();
    }

    public String findPheromone(String pheromone) {
        BlockPos result = this.ant.setFindPheromoneTarget(pheromone);
        if(result == null){
            return "";
        }
        return result.getX() + "," + result.getY() + "," + result.getZ();
    }

    public String getSurroundingPheromoneTypes() {
        return String.join(",", this.ant.getSurroundingPheromoneTypes());
    }

    public Boolean hasItemInContainer(String item, double x, double y, double z) {
        BlockPos containerPos = new BlockPos((int) x, (int) y, (int) z);
        Container container = getContainer(containerPos);
        if(container == null) {
            return false;
        }
        Item selectedItem = BuiltInRegistries.ITEM.getValue(Identifier.tryParse(item));
        for(int i = 0; i < container.getContainerSize(); i++) {
            if(container.getItem(i).getItem().equals(selectedItem)) {
                return true;
            }
        }
        return false;
    }
    public String getItemInContainer(int slot, double x, double y, double z) {
        BlockPos containerPos = new BlockPos((int) x, (int) y, (int) z);
        Container container = getContainer(containerPos);
        if(container == null) {
            return "";
        }
        if(slot < 0 || slot >= container.getContainerSize()) {
            return "";
        }
        return BuiltInRegistries.ITEM.getKey(container.getItem(slot).getItem()).toString();
    }



    public String getSpeed(){
        return String.valueOf(this.ant.speedModifier);
    }

    // variables

    public void setVariable(String name, String value){
        variables.put(name, value);
    }

    public String getVariable(String name){
        return variables.getOrDefault(name, "");
    }

    /** Saves the variable's current value as the value restored with this entity. */
    public void setVariablePermanent(String name) {
        if (!name.isEmpty() && variables.containsKey(name)) {
            permanentVariables.put(name, variables.get(name));
        }
    }

    public void newList(String name) {
        if (!name.isEmpty()) {
            lists.put(name, new ArrayList<>());
        }
    }

    /** Returns the list in the comma-separated representation used by LIST inputs. */
    public String getList(String name) {
        List<String> list = lists.get(name);
        return list == null ? "" : String.join(",", list);
    }

    public void setListValue(String name, int key, String value) {
        if (name.isEmpty() || key < 0) {
            return;
        }
        List<String> list = lists.computeIfAbsent(name, ignored -> new ArrayList<>());
        while (list.size() <= key) {
            list.add("");
        }
        list.set(key, value);
    }

    public void setWholeList(String name, String listStr) {
        if (name.isEmpty()) {
            return;
        }
        List<String> list = Arrays.stream(listStr.split(",")).toList();
        lists.put(name,list);
    }

    public void addList(String name, String listStr) {
        if (name.isEmpty()) {
            return;
        }
        List<String> listToPut = Arrays.stream(listStr.split(",")).toList();
        List<String> list = lists.computeIfAbsent(name, ignored -> new ArrayList<>());
        list.addAll(listToPut);
    }

    public void addValue(String name, String value) {
        if (name.isEmpty()) {
            return;
        }
        List<String> list = lists.computeIfAbsent(name, ignored -> new ArrayList<>());
        list.add(value);
    }

    public String getListValue(String name, int key) {
        List<String> list = lists.get(name);
        return list == null || key < 0 || key >= list.size() ? "" : list.get(key);
    }

    /** Saves a defensive snapshot, so later set_list calls do not silently alter persistence. */
    public void setListPermanent(String name) {
        List<String> list = lists.get(name);
        if (!name.isEmpty() && list != null) {
            permanentLists.put(name, new ArrayList<>(list));
        }
    }

    public void clearList(String name) {
        if (!name.isEmpty()) {
            lists.remove(name);
        }
    }

    public void readPermanentData(ValueInput input) {
        variables.clear();
        lists.clear();
        permanentVariables.clear();
        permanentLists.clear();

        for (ValueInput savedVariable : input.childrenListOrEmpty("PermanentVariables")) {
            String name = savedVariable.getStringOr("name", "");
            if (!name.isEmpty()) {
                String value = savedVariable.getStringOr("value", "");
                permanentVariables.put(name, value);
                variables.put(name, value);
            }
        }
        for (ValueInput savedList : input.childrenListOrEmpty("PermanentLists")) {
            String name = savedList.getStringOr("name", "");
            if (name.isEmpty()) {
                continue;
            }
            List<String> values = new ArrayList<>();
            for (ValueInput savedValue : savedList.childrenListOrEmpty("Values")) {
                values.add(savedValue.getStringOr("value", ""));
            }
            permanentLists.put(name, new ArrayList<>(values));
            lists.put(name, values);
        }
    }

    public void writePermanentData(ValueOutput output) {
        ValueOutput.ValueOutputList savedVariables = output.childrenList("PermanentVariables");
        permanentVariables.forEach((name, v) -> {
            ValueOutput child = savedVariables.addChild();
            child.putString("name", name);
            child.putString("value", v);
        });

        ValueOutput.ValueOutputList savedLists = output.childrenList("PermanentLists");
        permanentLists.forEach((name, values) -> {
            ValueOutput child = savedLists.addChild();
            child.putString("name", name);
            ValueOutput.ValueOutputList savedValues = child.childrenList("Values");
            for (String v : values) {
                savedValues.addChild().putString("value", v);
            }
        });
    }

    private Container getContainer(BlockPos containerPos) {
        if (containerPos == null || !this.ant.level().hasChunkAt(containerPos)) return null;
        BlockState state = this.ant.level().getBlockState(containerPos);
        if (state.getBlock() instanceof ChestBlock chestBlock) {
            return ChestBlock.getContainer(chestBlock, state, this.ant.level(), containerPos, false);
        }
        if (state.getBlock() instanceof WorldlyContainerHolder holder) {
            return holder.getContainer(state, this.ant.level(), containerPos);
        }
        BlockEntity blockEntity = this.ant.level().getBlockEntity(containerPos);
        return blockEntity instanceof Container container ? container : null;
    }

    public String getItemCountInInventory(double slot) {
        return String.valueOf(Objects.requireNonNull(this.ant.getInventory().getSlot((int) slot)).get().getCount());
    }

    public String getItemCountInContainer(double x, double y, double z, double slot) {
        BlockPos containerPos = new BlockPos((int) x, (int) y, (int) z);
        Container container = getContainer(containerPos);
        if(container == null) {
            return "";
        }
        if(slot < 0 || slot >= container.getContainerSize()) {
            return "";
        }
        return String.valueOf(container.getItem((int) slot).getCount());
    }
}
