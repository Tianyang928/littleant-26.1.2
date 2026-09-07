package net.tianyang928.littleant.entity.ai.brain;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.WorldlyContainerHolder;
import net.minecraft.world.entity.Entity;
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
import net.minecraft.world.entity.EntityType;

import java.util.*;
import java.util.stream.Collectors;

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
        BlockPos result = this.ant.setFindBlockEntityTarget(List.of(ModBlocks.PHEROMONE_BLOCK.get()));
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

    public String getEntityPos(int entityId) {
        Entity entity = this.ant.level().getEntity(entityId);
        if(entity == null) {
            return "[]";
        }
        return "[" + entity.position().x + "," + entity.position().y + "," + entity.position().z + "]";
    }

    public Boolean hasItemInInventory(String item) {
        List<Item> items = TagSupport.items(item);
        for(int i = 0; i < this.ant.getInventory().getContainerSize(); i++) {
            if(items.contains(this.ant.getInventory().getSlot(i).get().getItem())) {
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
        return "[" + this.ant.position().x + "," + this.ant.position().y + "," + this.ant.position().z + "]";
    }

    public String findNearestBlock(String block) {
        BlockPos result = this.ant.setFindBlockTarget(TagSupport.blocks(block));
        if(result == null){
            return "[]";
        }
        return "[" + result.getX() + "," + result.getY() + "," + result.getZ() + "]";
    }

    public String findNearestEntity(String entity) {
        int result = this.ant.setFindEntityTarget(TagSupport.entities(entity));
        if(result == -1){
            return "";
        }
        return String.valueOf(result);
    }

    public String findNearestBlockEntity(String blockEntity) {
        BlockPos result = this.ant.setFindBlockEntityTarget(TagSupport.blocks(blockEntity));
        if(result == null){
            return "[]";
        }
        return "[" + result.getX() + "," + result.getY() + "," + result.getZ() + "]";
    }

    public String findNearestDrop(String item) {
        BlockPos result = this.ant.setFindDropTarget(TagSupport.items(item));
        if(result == null){
            return "[]";
        }
        return "[" + result.getX() + "," + result.getY() + "," + result.getZ() + "]";
    }

    public String findNearestPheromone(String pheromone) {
        BlockPos result = this.ant.setFindPheromoneTarget(pheromone);
        if(result == null){
            return "[]";
        }
        return "[" + result.getX() + "," + result.getY() + "," + result.getZ() + "]";
    }

    private static String positions(List<BlockPos> values) {
        return values.stream().map(p -> "[" + p.getX() + "," + p.getY() + "," + p.getZ() + "]").collect(Collectors.joining(",", "[", "]"));
    }

    public String findBlockList(String block, int count) {
        return positions(ant.setFindBlockListTarget(TagSupport.blocks(block), count));
    }

    public String findBlockEntityList(String block, int count) {
        return positions(ant.setFindBlockEntityListTarget(TagSupport.blocks(block), count));
    }

    public String findPheromoneList(String type, int count) {
        return positions(ant.setFindPheromoneListTarget(type, count));
    }

    public String findEntityList(String entity, int count) {
        return ant.setFindEntityListTarget(TagSupport.entities(entity), count).stream().map(String::valueOf).collect(Collectors.joining(",", "[", "]"));
    }

    public String findDropList(String item, int count) {
        return positions(ant.setFindDropListTarget(TagSupport.items(item), count));
    }

    public String getSurroundingPheromoneTypes() {
        return this.ant.getSurroundingPheromoneTypes().stream().collect(Collectors.joining(",", "[", "]"));
    }

    public Boolean hasItemInContainer(String item, double x, double y, double z) {
        BlockPos containerPos = new BlockPos((int) x, (int) y, (int) z);
        Container container = getContainer(containerPos);
        if(container == null) {
            return false;
        }
        List<Item> selectedItems = TagSupport.items(item);
        for(int i = 0; i < container.getContainerSize(); i++) {
            if(selectedItems.contains(container.getItem(i).getItem())) {
                return true;
            }
        }
        return false;
    }

    public boolean isInTag(String target, String tag) {
        if (target == null || tag == null) return false;
        if (TagSupport.blockInTag(BuiltInRegistries.BLOCK.getValue(Identifier.tryParse(target)), tag)) return true;
        if (TagSupport.itemInTag(BuiltInRegistries.ITEM.getValue(Identifier.tryParse(target)), tag)) return true;
        return TagSupport.entityInTag(BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.tryParse(target)), tag);
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

    public boolean isRunning(){
        return this.ant.speedModifier == 1.25;
    }

    public boolean isCrouching(){
        return this.ant.isCrouching();
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

    /** Returns a bracketed list representation, preserving nested list strings. */
    public String getList(String name) {
        List<String> list = lists.get(name);
        if (list == null) return "[]";
        return list.stream().map(v -> v == null ? "" : v).collect(Collectors.joining(",", "[", "]"));
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
        lists.put(name, parseList(listStr));
    }

    public void addList(String name, String listStr) {
        if (name.isEmpty()) {
            return;
        }
        List<String> listToPut = parseList(listStr);
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

    /** Splits a list literal on top-level commas, honoring nested brackets and quotes. */
    static List<String> parseList(String text) {
        if (text == null) return new ArrayList<>();
        String s = text.trim();
        if (s.startsWith("[") && s.endsWith("]")) s = s.substring(1, s.length() - 1).trim();
        if (s.isEmpty()) return new ArrayList<>();
        List<String> out = new ArrayList<>();
        StringBuilder item = new StringBuilder(); int depth = 0; boolean quoted = false; char quote = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if ((c == '\"' || c == '\'') && (i == 0 || s.charAt(i - 1) != '\\')) {
                if (!quoted) { quoted = true; quote = c; }
                else if (quote == c) quoted = false;
                item.append(c);
                continue;
            }
            if (!quoted && c == '[') depth++; else if (!quoted && c == ']') depth--;
            if (!quoted && depth == 0 && c == ',') { out.add(unquoteListValue(item.toString().trim())); item.setLength(0); } else item.append(c);
        }
        out.add(unquoteListValue(item.toString().trim()));
        return out;
    }

    private static String unquoteListValue(String value) {
        if (value.length() >= 2 && ((value.startsWith("\"") && value.endsWith("\"")) || (value.startsWith("'") && value.endsWith("'")))) {
            return value.substring(1, value.length() - 1).replace("\\\"", "\"").replace("\\\\", "\\");
        }
        return value;
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
