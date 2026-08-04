package net.tianyang928.littleant.datagen;

import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.ModelProvider;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.data.PackOutput;
import net.tianyang928.littleant.LittleAnt;
import net.tianyang928.littleant.block.ModBlocks;
import net.tianyang928.littleant.item.ModItems;

public class ModModelProvider extends ModelProvider {
    public ModModelProvider(PackOutput output) {
        super(output, LittleAnt.MOD_ID);
    }

    @Override
    protected void registerModels(BlockModelGenerators blockModels, ItemModelGenerators itemModels) {
        itemModels.generateFlatItem(ModItems.ANT_SPAWN_EGG.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(ModItems.PHEROMONE_BLOCK.get(), ModelTemplates.FLAT_ITEM);

        blockModels.createTrivialCube(ModBlocks.ANT_CRAFTING_TABLE.get());
        blockModels.createAirLikeBlock(ModBlocks.PHEROMONE_BLOCK.get(), ModItems.PHEROMONE_BLOCK.get());
    }
}
