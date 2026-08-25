package net.tianyang928.littleant.entity;

import com.google.gson.JsonParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.InventoryCarrier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.tianyang928.littleant.LittleAnt;
import net.tianyang928.littleant.entity.ai.brain.*;
import net.tianyang928.littleant.entity.ai.goal.*;
import net.tianyang928.littleant.entity.ai.sense.FindBlock;
import net.tianyang928.littleant.entity.ai.sense.FindBlockEntity;
import net.tianyang928.littleant.entity.ai.sense.FindEntity;
import net.tianyang928.littleant.gui.AntInventoryMenu;
import net.tianyang928.littleant.gui.AntBrainProgramMenu;

import javax.annotation.Nullable;
import java.util.*;

public class AntEntity extends PathfinderMob implements InventoryCarrier {

    public static final int INVENTORY_SIZE = 9;
    private static final int INVENTORY_SLOT_OFFSET = 300;

    AntEntityGlobalData antEntityGlobalData = new AntEntityGlobalData();
    AntScriptInterpreter antScriptInterpreter = new AntScriptInterpreter(this);

    @Nullable
    private BreakBlockGoal breakBlockGoal;
    @Nullable
    private SetBlockGoal setBlockGoal;
    @Nullable
    private UseCraftingTableGoal useCraftingTableGoal;
    @Nullable
    private UseInventoryCraftingGoal useInventoryCraftingGoal;

    private final FindBlockEntity findBlockEntity = new FindBlockEntity(this, null);
    private final FindEntity findEntity = new FindEntity(this, null);
    private final FindBlock findBlock = new FindBlock(this, null);


    private final SimpleContainer inventory = new SimpleContainer(INVENTORY_SIZE);
    private final FoodData foodData = new FoodData();
    private int selectedSlot = 0;
    private BlockPos lastTimePos = null;
    private long lastUpdateTime = 0;
    private final LinkedHashMap<UUID, BrainBlock> brainBlocks = new LinkedHashMap<>();
    private boolean isProgrammingBrain = false;
    private Player programmingPlayer = null;
    public boolean needAiRestart = false;

    public boolean tryGettingDownWater = false;

    public LivingEntity lastHurtBy = null;
    public long lastHurtTime = -1;

    private static final EntityDataAccessor<String> skinNameAccessor =
            SynchedEntityData.defineId(
                    // The class of the entity.
                    AntEntity.class,
                    // The entity data accessor type.
                    EntityDataSerializers.STRING
            );
    private static final EntityDataAccessor<ItemStack> selectedItemAccessor =
            SynchedEntityData.defineId(AntEntity.class, EntityDataSerializers.ITEM_STACK);

    public AntEntity(EntityType<? extends AntEntity> type, Level level) {
        super(type, level);
        this.setCanPickUpLoot(true);
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand == InteractionHand.MAIN_HAND && !this.level().isClientSide() && player instanceof ServerPlayer serverPlayer) {
            // Shift deliberately selects the programming surface; normal use remains the inventory.
            serverPlayer.openMenu(player.isShiftKeyDown()
                    ? new MenuProvider() {
                        @Override
                        public Component getDisplayName() {
                            return Component.translatable("menu.littleant.ant_brain_program");
                        }

                        @Override
                        public AbstractContainerMenu createMenu(int id, Inventory inventory, Player ignored) {
                            return new AntBrainProgramMenu(id, inventory, AntEntity.this);
                        }

                        @Override
                        public void writeClientSideData(AbstractContainerMenu menu, RegistryFriendlyByteBuf buf) {
                            AntEntity.this.writeBrainProgramClientData(buf);
                        }
                    }
                    : new MenuProvider() {
                        @Override
                        public Component getDisplayName() {
                            // 如果有自定义名称，就显示自定义名称
                            return Component.translatable("container.littleant.ant_inventory");
                        }

                        @Override
                        public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
                            return new AntInventoryMenu(containerId, playerInventory, AntEntity.this);
                        }

                        @Override
                        public void writeClientSideData(AbstractContainerMenu menu, RegistryFriendlyByteBuf buf) {
                            AntEntity.this.writeInventoryClientData(buf);
                        }
                    }
            );
        }
        return InteractionResult.SUCCESS;
    }

    /** Sends the program snapshot when the dedicated brain menu is opened. */
    public void writeBrainProgramClientData(RegistryFriendlyByteBuf buf) {
        buf.writeVarInt(this.getId());
        buf.writeVarInt(this.brainBlocks.size());
        for (BrainBlock block : this.brainBlocks.values()) {
            buf.writeUtf(block.opcode(), 64);
            buf.writeVarInt(block.x());
            buf.writeVarInt(block.y());
            buf.writeUUID(block.id());
            buf.writeVarInt(block.inputs().size());
            for (InputSlot slot : block.inputs()) {
                buf.writeUtf(slot.name(), 32); buf.writeUtf(slot.type().name(), 16);
                String value = slot.value() == null ? "" : slot.value();
                buf.writeVarInt(value.length());
                buf.writeUtf(value, Math.max(1, value.length()));
                writeUuid(buf, slot.blockId());
            }
            writeUuid(buf, block.next());
            writeUuid(buf, block.parent());
        }
    }

    public void writeInventoryClientData(RegistryFriendlyByteBuf buf) {
        buf.writeInt(this.getId());
    }

    private static void writeUuid(RegistryFriendlyByteBuf buf, UUID id) {
        buf.writeBoolean(id != null);
        if (id != null) { buf.writeUUID(id); }
    }

    public LinkedHashMap<UUID, BrainBlock> getBrainBlocks() {
        return (LinkedHashMap<UUID, BrainBlock>) this.brainBlocks.clone();
    }

    public void addBrainBlock(String opcode, int x, int y, UUID id) {
        if (this.brainBlocks.size() < 256 && ModuleRegistry.contains(opcode)) {
            this.brainBlocks.put(id, new BrainBlock(opcode, x, y, id,
                    ModuleRegistry.createDefaultInputs(opcode), null, null));
        }
    }

    public void removeBrainBlock(UUID id) {
        this.brainBlocks.remove(id);
        LittleAnt.LOGGER.info("[AntEntity] removeBrainBlock, id: {}", id);
    }

    public void replaceBrainBlocks(Map<UUID, BrainBlock> blocks) {
        this.brainBlocks.clear();
        this.brainBlocks.putAll(blocks);
    }



    public void clearActiveGoals() {
        if (breakBlockGoal != null) breakBlockGoal.clearTarget();
        if (setBlockGoal != null) setBlockGoal.clearTarget();
    }

    public void runScript(String source) {
        this.brainBlocks.clear();
        this.brainBlocks.putAll( new CodeToModuleConverter().convert(JsonParser.parseString(source).getAsJsonObject()));
    }

    public void tickBrainProgram() {
        if (!level().isClientSide()) {
            antScriptInterpreter.loadProgram(this.brainBlocks);
            if (needAiRestart) {
                antScriptInterpreter.start();
                needAiRestart = false;
            }
            antScriptInterpreter.tick();
        }
    }

    public AntBlackboard getBrainBlackboard() {
        return antScriptInterpreter.blackboard();
    }

    public void setIsProgrammingBrain(Player player, boolean isProgramming) {
        this.isProgrammingBrain = isProgramming;
        this.programmingPlayer = player;
        this.needAiRestart = true;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.30D)
                .add(Attributes.FOLLOW_RANGE, 64.0D)
                .add(Attributes.ATTACK_DAMAGE, 2.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.10D)
                .add(Attributes.MINING_EFFICIENCY, 0.0D);
    }

    @Override
    public @Nullable SpawnGroupData finalizeSpawn(
            ServerLevelAccessor level,
            DifficultyInstance difficulty,
            EntitySpawnReason spawnReason,
            @Nullable SpawnGroupData groupData
    ) {
        SpawnGroupData data = super.finalizeSpawn(level, difficulty, spawnReason, groupData);
        this.setCustomName(getRandomCharacterName());
        this.getEntityData().set(skinNameAccessor, getRandomSkinName());
        this.setCustomNameVisible(true);
        this.setPersistenceRequired();
        this.setCanPickUpLoot(true);


        LittleAnt.LOGGER.info("[AntEntity] finalizeSpawn, full custom name: {}", Objects.requireNonNull(getCustomName()).getString());
        return data;
    }

    @Override
    protected void registerGoals() {
        //break block init
        this.breakBlockGoal = new BreakBlockGoal(this, BlockPos.ZERO);
        this.breakBlockGoal.clearTarget();
        this.goalSelector.addGoal(1, this.breakBlockGoal);
        //set block init
        this.setBlockGoal = new SetBlockGoal(this, BlockPos.ZERO);
        this.setBlockGoal.clearTarget();
        this.goalSelector.addGoal(1, this.setBlockGoal);
        // Crafting is explicitly requested by the ant's AI, never selected automatically.
        this.useCraftingTableGoal = new UseCraftingTableGoal(this);
        this.goalSelector.addGoal(1, this.useCraftingTableGoal);
        this.useInventoryCraftingGoal = new UseInventoryCraftingGoal(this);
        this.goalSelector.addGoal(1, this.useInventoryCraftingGoal);

        //this.goalSelector.addGoal(1, new PanicGoal(this, 1.25));
        //this.goalSelector.addGoal(2, new BetterFloatGoal(this));

        //this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 1.0));
        //this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
        //this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
    }

    // Assigns the next block this ant should path to and mine.
    public void setBreakTarget(BlockPos target) {
        if (this.breakBlockGoal != null) {
            this.breakBlockGoal.setTarget(target);
        }
    }
    public void setSetTarget(BlockPos target) {
        if (this.setBlockGoal != null) {
            this.setBlockGoal.setTarget(target);
        }
    }
    public BlockPos setFindBlockTarget(Block blockToFind) {
        if (this.findBlock != null) {
            return this.findBlock.setTarget(blockToFind);
        }
        return null;
    }

    public void setCraftingTableInput(CraftingInput input, BlockPos craftingTablePos, int amountCrafted) {
        if (this.useCraftingTableGoal != null) {
            this.useCraftingTableGoal.setInput(input, craftingTablePos, amountCrafted);
        }
    }

    public void setInventoryCraftingInput(CraftingInput input, int amountCrafted) {
        if (this.useInventoryCraftingGoal != null) {
            this.useInventoryCraftingGoal.setInput(input, amountCrafted);
        }
    }

    public BlockPos setFindBlockEntityTarget(Block blockEntity) {
        if(this.findBlockEntity != null && blockEntity.defaultBlockState().hasBlockEntity()){
            return this.findBlockEntity.setTarget(blockEntity);
        }
        return null;
    }
    public BlockPos setFindEntityTarget(EntityType<?> entityType) {
        if(this.findEntity != null){
            return this.findEntity.setTarget(entityType);
        }
        return null;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(skinNameAccessor, "");
        builder.define(selectedItemAccessor, ItemStack.EMPTY);
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        Entity attacker = source.getEntity();
        this.lastHurtTime = level.getGameTime();
        // 记录最后攻击者
        if (attacker instanceof LivingEntity) {
            this.setLastHurtBy((LivingEntity) attacker);
        }
        return super.hurtServer(level, source, amount);
    }

    private void setLastHurtBy(LivingEntity attacker) {
        this.lastHurtBy = attacker;
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.getEntityData().set(skinNameAccessor, input.getStringOr("skin_name", ""));
        this.readInventoryFromTag(input);
        this.selectedSlot = input.getIntOr("selected_slot", 0);
        this.selectedSlot = Mth.clamp(this.selectedSlot, 0, INVENTORY_SIZE - 1);
        this.syncSelectedItem();
        this.foodData.readAdditionalSaveData(input);
        this.brainBlocks.clear();
        for (ValueInput child : input.childrenListOrEmpty("BrainBlocks")) {
            String opcode = child.getStringOr("opcode", "");
            if (!opcode.isEmpty()) {
                UUID next = parseUuid(child.getStringOr("next", ""));
                UUID parent = parseUuid(child.getStringOr("parent", ""));
                UUID id = parseUuid(child.getStringOr("id", ""));
                List<InputSlot> inputs = new ArrayList<>();
                for (ValueInput savedInput : child.childrenListOrEmpty("Inputs")) {
                    String name = savedInput.getStringOr("name", "");
                    ValueType type;
                    try { type = ValueType.valueOf(savedInput.getStringOr("type", ValueType.ANY.name())); }
                    catch (IllegalArgumentException ignored) { type = ValueType.ANY; }
                    String value = savedInput.getStringOr("value", "");
                    inputs.add(new InputSlot(name, type, value, parseUuid(savedInput.getStringOr("block", ""))));
                }
                if (inputs.isEmpty()) inputs = ModuleRegistry.createDefaultInputs(opcode);
                if (id != null) this.brainBlocks.put(id, new BrainBlock(opcode, child.getIntOr("x", 0), child.getIntOr("y", 0), id, inputs, next, parent));
            }
        }
        LittleAnt.LOGGER.info("[AntEntity] read skin name from save data: {}", getSkinNameAccessor());
        // 读取库存, 打印列表
        for (int slot = 0; slot < INVENTORY_SIZE; slot++) {
            LittleAnt.LOGGER.info("[AntEntity] read inventory from save data, slot: {}, itemStack: {}", slot, this.inventory.getItem(slot).getDisplayName());
        }
        // 如果没有皮肤，随机选择一个皮肤
        ensureSkinName();
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putString("skin_name", getSkinNameAccessor());
        this.writeInventoryToTag(output);
        output.putInt("selected_slot", this.selectedSlot);
        this.foodData.addAdditionalSaveData(output);
        ValueOutput.ValueOutputList brainBlockList = output.childrenList("BrainBlocks");
        for (BrainBlock block : this.brainBlocks.values()) {
            ValueOutput child = brainBlockList.addChild();
            child.putString("opcode", block.opcode());
            child.putInt("x", block.x());
            child.putInt("y", block.y());
            child.putString("id", block.id().toString());
            if (block.next() != null) child.putString("next", block.next().toString());
            if (block.parent() != null) child.putString("parent", block.parent().toString());
            ValueOutput.ValueOutputList inputList = child.childrenList("Inputs");
            for (InputSlot input : block.inputs()) {
                ValueOutput savedInput = inputList.addChild();
                savedInput.putString("name", input.name());
                savedInput.putString("type", input.type().name());
                if (input.value() != null) savedInput.putString("value", input.value());
                if (input.blockId() != null) savedInput.putString("block", input.blockId().toString());
            }
        }
        LittleAnt.LOGGER.info("[AntEntity] write skin name to save data: {}", getSkinNameAccessor());
    }

    private static UUID parseUuid(String value) {
        if (value == null || value.isEmpty()) return null;
        try { return UUID.fromString(value); } catch (IllegalArgumentException ignored) { return null; }
    }

    @Override
    public SimpleContainer getInventory() {
        return this.inventory;
    }

    public FoodData getFoodData() {
        return this.foodData;
    }

    @Override
    public @Nullable SlotAccess getSlot(int slot) {
        int inventorySlot = slot - INVENTORY_SLOT_OFFSET;
        return inventorySlot >= 0 && inventorySlot < INVENTORY_SIZE
                ? this.inventory.getSlot(inventorySlot)
                : super.getSlot(slot);
    }

    @Override
    protected void pickUpItem(ServerLevel level, ItemEntity itemEntity) {
        InventoryCarrier.pickUpItem(level, this, this, itemEntity);
        this.syncSelectedItem();
    }

    @Override
    public boolean wantsToPickUp(ServerLevel level, ItemStack itemStack) {
        //LittleAnt.LOGGER.info("[AntEntity] wantsToPickUp, itemStack: {}", itemStack.getDisplayName());
        return this.inventory.canAddItem(itemStack);
    }

    /**
     * Consumes one food item from the ant's inventory when it is hungry.
     * AI goals can call this method when they decide that eating is appropriate.
     */
    public boolean eatFromInventory() {
        if (!this.foodData.needsFood() || this.inventory == null) {
            return false;
        }

        for (int slot = 0; slot < INVENTORY_SIZE; slot++) {
            ItemStack itemStack = this.inventory.getItem(slot);
            FoodProperties foodProperties = itemStack.get(DataComponents.FOOD);
            if (foodProperties != null) {
                this.foodData.eat(foodProperties);
                itemStack.shrink(1);
                this.selectedSlot = slot;
                this.syncSelectedItem();
                return true;
            }
        }

        return false;
    }

    private Component getRandomCharacterName() {
        // 从 CHARACTER_NAMES 中随机选择一个名字
        LinkedHashMap<String, Integer> CHARACTER_NAMES = antEntityGlobalData.getCharacterNames();
        int index = (int) (Math.random() * CHARACTER_NAMES.size());
        String selectedName = CHARACTER_NAMES.keySet().toArray(new String[0])[index];
        this.antEntityGlobalData.addNameCount(selectedName);
        if (CHARACTER_NAMES.get(selectedName) >= 2) {
            selectedName += CHARACTER_NAMES.get(selectedName);
        }

        LittleAnt.LOGGER.info("[AntEntity] random new character name: {}", selectedName);
        return Component.literal(selectedName);
    }

    private String getRandomSkinName() {
        // 从 SKIN_NAMES 中随机选择一个皮肤
        int skinIndex = (int) (Math.random() * antEntityGlobalData.getSkinNames().length);
        LittleAnt.LOGGER.info("[AntEntity] random new skin name: {}", getSkinNameAccessor());
        return antEntityGlobalData.getSkinNames()[skinIndex];
    }

    public void ensureSkinName() {
        if (getSkinNameAccessor().isEmpty()) {
            this.getEntityData().set(skinNameAccessor, getRandomSkinName());
        }
    }

    public String getSkinNameAccessor() {
        return this.getEntityData().get(skinNameAccessor);
    }

    @Override
    public void aiStep() {
        this.updateSwingTime();
        super.aiStep();
    }

    @Override
    public ItemStack getItemBySlot(EquipmentSlot slot) {
        if(slot == EquipmentSlot.MAINHAND){
            // The inventory itself is not network-synchronized.  Clients must use
            // this synced snapshot when rendering the selected inventory slot.
            if(this.inventory == null){
                return ItemStack.EMPTY;
            }

            return this.level().isClientSide()
                    ? this.getEntityData().get(selectedItemAccessor)
                    : this.inventory.getItem(this.selectedSlot);
        }
        else if(slot == EquipmentSlot.OFFHAND){
            return ItemStack.EMPTY;
        }
        return super.getItemBySlot(slot);
    }

    public void getDownInWater() {
        this.sinkInFluid(NeoForgeMod.WATER_TYPE.value());
    }

    public void giveItem(ItemStack item){
        this.inventory.setItem(this.selectedSlot, item.copy());
        this.syncSelectedItem();
    }

    public int getSelectedSlot() {
        return this.selectedSlot;
    }

    public void setSelectedSlot(int slot) {
        this.selectedSlot = Mth.clamp(slot, 0, INVENTORY_SIZE - 1);
        this.syncSelectedItem();
    }

    private void syncSelectedItem() {
        if (!this.level().isClientSide()) {
            ItemStack selectedItem = this.inventory.getItem(this.selectedSlot);
            if (!ItemStack.matches(this.getEntityData().get(selectedItemAccessor), selectedItem)) {
                this.getEntityData().set(selectedItemAccessor, selectedItem.copy());
            }
        }
    }

    @Override
    public void tick() {
        super.tick();

        this.syncSelectedItem();
        long currentTime = this.level().getGameTime();
        if(this.lastTimePos == null){
            this.lastTimePos = this.blockPosition();
        }
        // 每1200个tick更新一次食物等级
        if((currentTime - this.lastUpdateTime) >= 1200){
            this.lastUpdateTime = currentTime;
            int minusFoodLevel = (int) Mth.sqrt((float)this.distanceToSqr(this.lastTimePos.getX(), this.lastTimePos.getY(), this.lastTimePos.getZ()))%100;
            this.foodData.setFoodLevel(this.foodData.getFoodLevel() - minusFoodLevel);
            this.lastTimePos = this.blockPosition();
        }

        // 执行脚本
        if(!this.isProgrammingBrain){
            tickBrainProgram();
        }
    }
}
