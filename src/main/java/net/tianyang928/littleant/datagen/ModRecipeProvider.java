package net.tianyang928.littleant.datagen;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.tianyang928.littleant.block.ModBlocks;
import net.tianyang928.littleant.item.ModItems;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;


public class ModRecipeProvider extends RecipeProvider {
    public ModRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
    }

    @Override
    protected void buildRecipes(@NotNull RecipeOutput recipeOutput) {
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModItems.ANT_SPAWN_EGG.get())
                .requires(Blocks.CRAFTING_TABLE)
                .requires(Blocks.DIRT)
                .requires(Blocks.COBBLESTONE)
                .requires(Items.WATER_BUCKET)
                .requires(Items.IRON_INGOT)
                .requires(Items.BREAD)
                .requires(Items.BONE)
                .requires(Items.ROTTEN_FLESH)
                .requires(ItemTags.LOGS)
                .unlockedBy("has_crafting_table", has(Blocks.CRAFTING_TABLE))
                .unlockedBy("has_dirt", has(Blocks.DIRT))
                .unlockedBy("has_cobblestone", has(Blocks.COBBLESTONE))
                .unlockedBy("has_water_bucket", has(Items.WATER_BUCKET))
                .unlockedBy("has_iron_ingot", has(Items.IRON_INGOT))
                .unlockedBy("has_bread", has(Items.BREAD))
                .unlockedBy("has_bone", has(Items.BONE))
                .unlockedBy("has_rotten_flesh", has(Items.ROTTEN_FLESH))
                .unlockedBy("has_logs", has(ItemTags.LOGS))
                .save(recipeOutput);
    }
}
