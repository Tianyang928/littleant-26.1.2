package net.tianyang928.littleant.datagen;

import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.tianyang928.littleant.LittleAnt;
import net.tianyang928.littleant.block.ModBlocks;
import net.tianyang928.littleant.item.ModItems;

public class ModItemModelProvider extends ItemModelProvider {
    public ModItemModelProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, LittleAnt.MOD_ID, existingFileHelper);
    }

    @Override
    protected void registerModels() {
        basicItem(ModItems.PHEROMONE_BLOCK.get());
        simpleBlockItem(ModBlocks.ANT_CRAFTING_TABLE.get());
        spawnEggItem(ModItems.ANT_SPAWN_EGG.get());
    }
}
