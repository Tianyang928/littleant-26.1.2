package net.tianyang928.littleant.entity.ai.goal;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.crafting.CraftingInput;
import net.tianyang928.littleant.LittleAnt;
import net.tianyang928.littleant.entity.AntEntity;

import javax.annotation.Nullable;
import java.util.EnumSet;

// Performs one player-inventory-sized (at most 2x2) crafting operation.
public class UseInventoryCraftingGoal extends Goal {
    private final AntEntity ant;
    @Nullable private CraftingInput input;
    private int amountCrafted = 0;

    public UseInventoryCraftingGoal(AntEntity ant) {
        this.ant = ant;
    }

    public void setInput(CraftingInput input, int amountCrafted) {
        this.input = AntCrafting.copyInput(input);
        this.amountCrafted = amountCrafted;
    }

    public void clearInput() {
        this.input = null;
    }

    @Override
    public boolean canUse() {
        if (!(this.ant.level() instanceof ServerLevel level) || this.input == null || this.input.isEmpty()
            || this.input.width() > 2 || this.input.height() > 2
            || !AntCrafting.hasIngredients(this.ant.getInventory(), this.input)
            || AntCrafting.findRecipe(level, this.input).isEmpty()
            || this.amountCrafted <= 0) {
            clearInput();
            return false;
        }
        return true;
    }

    @Override
    public void tick() {
        if (this.ant.level() instanceof ServerLevel level && this.input != null) {
            AntCrafting.findRecipe(level, this.input).ifPresent(recipe -> {
                for(int amount = 0; amount < this.amountCrafted; amount++){
                    if (AntCrafting.craft(this.ant, level, this.input, recipe)) {
                        this.ant.swing(InteractionHand.MAIN_HAND, true);
                        LittleAnt.LOGGER.info("[UseInventoryCraftingGoal] tick: craft one item successfully");
                    }
                    else{
                        LittleAnt.LOGGER.info("[UseInventoryCraftingGoal] tick: craft failed");
                    }
                }
            });
        }
        clearInput();
    }
}
