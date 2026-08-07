package net.tianyang928.littleant.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.tianyang928.littleant.LittleAnt;

import java.util.LinkedHashMap;
import java.util.Map;

public class AntEntityGlobalData extends SavedData {

    private static final LinkedHashMap<String, Integer> CHARACTER_NAMES = new LinkedHashMap<>() {{
        put("Ante", 0);
        put("Anthem", 0);
        put("Antler", 0);
        put("Antacid", 0);
        put("Antibiotic", 0);
        put("Antibody", 0);
        put("Antigen", 0);
        put("Antifreeze", 0);
        put("Antihistamine", 0);
        put("Antimalarial", 0);
        put("Antioxidant", 0);
        put("Antiperspirant", 0);
        put("Antipyretic", 0);
        put("Antirust", 0);
        put("Antiseptic", 0);
        put("Antisocial", 0);
        put("Antitank", 0);
        put("Antitoxin", 0);
        put("Antivirus", 0);
        put("Antiwar", 0);
        put("Antagonism", 0);
        put("Antagonist", 0);
        put("Antagonize", 0);
        put("Anticipation", 0);
        put("Anticipate", 0);
        put("Antipathy", 0);
        put("Antipodal", 0);
        put("Antiquate", 0);
        put("Antique", 0);
        put("Antiquity", 0);
        put("Antithesis", 0);
        put("Antonym", 0);
        put("Antarctic", 0);
        put("Anteater", 0);
        put("Antelope", 0);
        put("Antenna", 0);
        put("Anthrax", 0);
        put("Anthracite", 0);
        put("Anthology", 0);
        put("Anthropic", 0);
        put("Anthropoid", 0);
        put("Anthropology", 0);
        put("Anteroom", 0);
        put("Antirrhinum", 0);
        put("Antlered", 0);
    }};

    private static final String[] SKIN_NAMES = {
            "alex",
            "ari",
            "efe",
            "kai",
            "makena",
            "noor",
            "steve",
            "sunny",
            "zuri",
            "dream",
            "herobrine",
            "zombie",
            "little-chicken",
            "ugly-steve",
            "white-guy",
            "villager"
    };

    public AntEntityGlobalData(Map<String, Integer> characterNames) {
        CHARACTER_NAMES.replaceAll((k, v) -> characterNames.getOrDefault(k, 0));
    }

    public AntEntityGlobalData() {
    }

    // 定义序列化用的 Codec
    public static final Codec<AntEntityGlobalData> CODEC = RecordCodecBuilder.create(
            instance -> instance.group(Codec.unboundedMap(Codec.STRING, Codec.INT)
                    .optionalFieldOf("character_names", Map.of())
                    .forGetter(AntEntityGlobalData::getCharacterNames)
            ).apply(instance, AntEntityGlobalData::new)
    );

    public static final SavedDataType<AntEntityGlobalData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(LittleAnt.MOD_ID, "ant_entity_data"),
            AntEntityGlobalData::new,
            CODEC,
            DataFixTypes.SAVED_DATA_WANDERING_TRADER
    );

    public LinkedHashMap<String, Integer> getCharacterNames() {
        return CHARACTER_NAMES;
    }
    public String[] getSkinNames() {
        return SKIN_NAMES;
    }

    public void addNameCount(String characterName) {
        CHARACTER_NAMES.put(characterName, CHARACTER_NAMES.getOrDefault(characterName, 0) + 1);
        this.setDirty();
    }
}
