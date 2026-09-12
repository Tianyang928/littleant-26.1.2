package net.tianyang928.littleant.entity;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.LinkedHashMap;
import java.util.Map;

public class AntEntityGlobalData extends SavedData {

    private final LinkedHashMap<String, Integer> characterNames = new LinkedHashMap<>() {{
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
        this.characterNames.replaceAll((k, v) -> characterNames.getOrDefault(k, 0));
    }

    public AntEntityGlobalData() {
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        CompoundTag compoundtag = new CompoundTag();
        this.characterNames.forEach(compoundtag::putInt);
        tag.put("character_names", compoundtag);
        return tag;
    }

    public static AntEntityGlobalData load(CompoundTag tag, HolderLookup.Provider registries) {
        AntEntityGlobalData data = new AntEntityGlobalData();
        CompoundTag compoundtag = tag.getCompound("character_names");
        for(String characterName : compoundtag.getAllKeys()) {
            int count = compoundtag.getInt(characterName);
            if (data.characterNames.containsKey(characterName) && count >= 0) {
                data.characterNames.put(characterName, count);
            }
        }
        return data;
    }

    public static final SavedData.Factory<AntEntityGlobalData> FACTORY =
            new SavedData.Factory<>(AntEntityGlobalData::new, AntEntityGlobalData::load, DataFixTypes.SAVED_DATA_MAP_INDEX);

    public static AntEntityGlobalData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(FACTORY, "ant_entity_data");
    }

    public LinkedHashMap<String, Integer> getCharacterNames() {
        return characterNames;
    }
    public String[] getSkinNames() {
        return SKIN_NAMES;
    }

    public void addNameCount(String characterName) {
        characterNames.put(characterName, characterNames.getOrDefault(characterName, 0) + 1);
        this.setDirty();
    }
}
