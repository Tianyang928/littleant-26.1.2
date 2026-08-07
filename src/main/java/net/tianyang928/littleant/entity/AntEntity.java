package net.tianyang928.littleant.entity;

import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.tianyang928.littleant.LittleAnt;

public class AntEntity extends PathfinderMob {
    private static final String[] CHARACTER_NAMES = {
            "Ante",
            "Anthem",
            "Antler",
            "Antacid",
            "Antibiotic",
            "Antibody",
            "Antigen",
            "Antifreeze",
            "Antihistamine",
            "Antimalarial",
            "Antioxidant",
            "Antiperspirant",
            "Antipyretic",
            "Antirust",
            "Antiseptic",
            "Antisocial",
            "Antitank",
            "Antitoxin",
            "Antivirus",
            "Antiwar",
            "Antagonism",
            "Antagonist",
            "Antagonize",
            "Anticipation",
            "Anticipate",
            "Antipathy",
            "Antipodal",
            "Antiquate",
            "Antique",
            "Antiquity",
            "Antithesis",
            "Antonym",
            "Antarctic",
            "Anteater",
            "Antelope",
            "Antenna",
            "Anthrax",
            "Anthracite",
            "Anthology",
            "Anthropic",
            "Anthropoid",
            "Anthropology",
            "Anteroom",
            "Antirrhinum",
            "Antlered"
    };

    public Component characterName;

    public AntEntity(EntityType<? extends AntEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.30D)
                .add(Attributes.FOLLOW_RANGE, 24.0D)
                .add(Attributes.ATTACK_DAMAGE, 2.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.10D);
    }

    @Override
    protected void registerGoals() {
        // AI 阶段再加入目标；暂时保持为空。
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return super.hurtServer(level, source, amount);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
    }

    private Component getRandomCharacterName() {
        return Component.literal(CHARACTER_NAMES[(int) (Math.random() * CHARACTER_NAMES.length)]);
    }

    public Component updateCharacterName() {
        if(this.getCustomName() == null){
            this.characterName = getRandomCharacterName();
            this.setCustomName(this.characterName);
        }
        else {
            this.characterName = this.getCustomName();
        }
        return this.characterName;
    }
}