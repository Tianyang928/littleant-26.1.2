package net.tianyang928.littleant.entity.ai.goal;

import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.tianyang928.littleant.entity.AntEntity;

import java.util.List;
import java.util.Optional;

/** Shared server-side crafting logic for the ant's crafting goals. */
final class AntCrafting {
    private AntCrafting() {
    }

    static CraftingInput copyInput(CraftingInput input) {
        return CraftingInput.of(input.width(), input.height(), input.items().stream().map(ItemStack::copy).toList());
    }

    static Optional<RecipeHolder<CraftingRecipe>> findRecipe(ServerLevel level, CraftingInput input) {
        return level.recipeAccess().getRecipeFor(RecipeType.CRAFTING, input, level);
    }

    static boolean hasIngredients(SimpleContainer inventory, CraftingInput input) {
        // 复制一份物品栏，不会改变原始物品栏
        List<ItemStack> available = inventory.getItems().stream().map(ItemStack::copy).toList();
        for (ItemStack ingredient : input.items()) {
            if (ingredient.isEmpty()) continue;
            boolean found = false;
            for (ItemStack stack : available) {
                if (ItemStack.isSameItemSameComponents(stack, ingredient) && !stack.isEmpty()) {
                    stack.shrink(1);
                    found = true;
                    break;
                }
            }
            if (!found) return false;
        }
        return true;
    }

    static boolean craft(AntEntity ant, ServerLevel level, CraftingInput input, RecipeHolder<CraftingRecipe> recipeHolder) {
        SimpleContainer inventory = ant.getInventory();
        if (!hasIngredients(inventory, input)) {
            return false;
        }

        ItemStack result = recipeHolder.value().assemble(input);
        if (result.isEmpty() || !result.isItemEnabled(level.enabledFeatures())) {
            return false;
        }

        NonNullList<ItemStack> remainingItems = recipeHolder.value().getRemainingItems(input);
        consumeIngredients(inventory, input);
        addOrDrop(ant, level, result);
        for (ItemStack remainingItem : remainingItems) {
            addOrDrop(ant, level, remainingItem);
        }
        return true;
    }

    private static void consumeIngredients(SimpleContainer inventory, CraftingInput input) {
        for (ItemStack ingredient : input.items()) {
            if (ingredient.isEmpty()) {
                continue;
            }
            for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
                ItemStack stack = inventory.getItem(slot);
                if (ItemStack.isSameItemSameComponents(stack, ingredient) && !stack.isEmpty()) {
                    inventory.removeItem(slot, 1);
                    break;
                }
            }
        }
    }

    private static void addOrDrop(AntEntity ant, ServerLevel level, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        ItemStack overflow = ant.getInventory().addItem(stack.copy());
        if (!overflow.isEmpty()) {
            ant.spawnAtLocation(level, overflow);
        }
    }
}
