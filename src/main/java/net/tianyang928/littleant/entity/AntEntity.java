package net.tianyang928.littleant.entity;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.item.ItemStack;
import net.tianyang928.littleant.LittleAnt;
import net.tianyang928.littleant.entity.ai.goal.BreakBlockGoal;
import org.w3c.dom.Attr;

import javax.annotation.Nullable;
import java.util.LinkedHashMap;
import java.util.Objects;

public class AntEntity extends PathfinderMob {

    AntEntityGlobalData antEntityGlobalData = new AntEntityGlobalData();
    @Nullable
    private BreakBlockGoal breakBlockGoal;

    private static final EntityDataAccessor<String> skinNameAccessor =
            SynchedEntityData.defineId(
                    // The class of the entity.
                    AntEntity.class,
                    // The entity data accessor type.
                    EntityDataSerializers.STRING
            );

    public AntEntity(EntityType<? extends AntEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.30D)
                .add(Attributes.FOLLOW_RANGE, 24.0D)
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
    ){
        SpawnGroupData data = super.finalizeSpawn(level, difficulty, spawnReason, groupData);
        this.setCustomName(getRandomCharacterName());
        this.getEntityData().set(skinNameAccessor, getRandomSkinName());
        this.setCustomNameVisible(true);
        this.setPersistenceRequired();
        LittleAnt.LOGGER.info("[AntEntity] finalizeSpawn, full custom name: {}", Objects.requireNonNull(getCustomName()).getString());
        return data;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));

        this.breakBlockGoal = new BreakBlockGoal(this, BlockPos.ZERO);
        this.breakBlockGoal.clearTarget();
        this.goalSelector.addGoal(1, this.breakBlockGoal);
    }

    // Assigns the next block this ant should path to and mine.
    public void setBreakTarget(BlockPos target) {
        if (this.breakBlockGoal != null) {
            this.breakBlockGoal.setTarget(target);
        }
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(skinNameAccessor, "");
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return super.hurtServer(level, source, amount);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.getEntityData().set(skinNameAccessor, input.getStringOr("skin_name", ""));
        LittleAnt.LOGGER.info("[AntEntity] read skin name from save data: {}", getSkinNameAccessor());

        // 如果没有皮肤，随机选择一个皮肤
        ensureSkinName();
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putString("skin_name", getSkinNameAccessor());
        LittleAnt.LOGGER.info("[AntEntity] write skin name to save data: {}", getSkinNameAccessor());
    }

    private Component getRandomCharacterName() {
        // 从 CHARACTER_NAMES 中随机选择一个名字
        LinkedHashMap<String, Integer> CHARACTER_NAMES = antEntityGlobalData.getCharacterNames();
        int index = (int) (Math.random() * CHARACTER_NAMES.size());
        String selectedName = CHARACTER_NAMES.keySet().toArray(new String[0])[index];
        this.antEntityGlobalData.addNameCount(selectedName);
        if(CHARACTER_NAMES.get(selectedName) >= 2){
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
        if(getSkinNameAccessor().isEmpty()){
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
}
