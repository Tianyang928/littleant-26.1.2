package net.tianyang928.littleant;

import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.tianyang928.littleant.datagen.ModBlockLootTableProvider;
import net.tianyang928.littleant.datagen.ModBlockTagsProvider;
import net.tianyang928.littleant.datagen.ModModelProvider;
import net.tianyang928.littleant.datagen.ModItemModelProvider;
import net.tianyang928.littleant.datagen.ModRecipeProvider;

import java.util.Collections;
import java.util.List;

@EventBusSubscriber(modid = LittleAnt.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public class LittleAntDataGen {
    @SubscribeEvent
    public static void gatherData(GatherDataEvent event) {
        DataGenerator generator = event.getGenerator();
        PackOutput packOutput = generator.getPackOutput();
        var lookupProvider = event.getLookupProvider();

        generator.addProvider(event.includeClient(), new ModModelProvider(packOutput, event.getExistingFileHelper()));
        generator.addProvider(event.includeClient(), new ModItemModelProvider(packOutput, event.getExistingFileHelper()));
        generator.addProvider(event.includeServer(), new ModBlockTagsProvider(packOutput, lookupProvider, null));
        generator.addProvider(event.includeServer(), new LootTableProvider(packOutput,
                                                            Collections.emptySet(),
                                                            List.of(new LootTableProvider.SubProviderEntry(ModBlockLootTableProvider::new,
                                                                                                            LootContextParamSets.BLOCK)),
                                                            lookupProvider));
        generator.addProvider(event.includeServer(), new ModRecipeProvider(packOutput, lookupProvider));

    }
}
