package net.tianyang928.littleant.datagen;

import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.tianyang928.littleant.LittleAnt;
import net.tianyang928.littleant.block.ModBlocks;

public class ModModelProvider extends BlockStateProvider {
    public ModModelProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, LittleAnt.MOD_ID, existingFileHelper);
    }

    @Override
    protected void registerStatesAndModels() {
        ModelFile pheromoneModel = models()
                .getBuilder("pheromone_block")
                .texture("particle", modLoc("item/pheromone_block"));
        simpleBlock(ModBlocks.PHEROMONE_BLOCK.get(), pheromoneModel);

        ModelFile craftingTableModel = models().cubeAll("ant_crafting_table", modLoc("block/ant_crafting_table"));
        simpleBlock(ModBlocks.ANT_CRAFTING_TABLE.get(), craftingTableModel);
    }
}
