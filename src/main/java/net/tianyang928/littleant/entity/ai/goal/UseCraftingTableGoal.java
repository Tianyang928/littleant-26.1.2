package net.tianyang928.littleant.entity.ai.goal;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.pathfinder.Path;
import net.tianyang928.littleant.LittleAnt;
import net.tianyang928.littleant.entity.AntEntity;

import javax.annotation.Nullable;
import java.util.EnumSet;
import java.util.Optional;

public class UseCraftingTableGoal extends Goal {

    private final AntEntity ant;
    @Nullable private CraftingInput input;
    @Nullable private BlockPos craftingTablePos;
    @Nullable private Path path;
    private int amountCrafted = 0;

    public UseCraftingTableGoal(AntEntity ant) {
        this.ant = ant;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    // Queues one crafting operation. The input is copied to prevent later mutation by its caller.
    public void setInput(CraftingInput input, BlockPos craftingTablePos, int amountCrafted) {
        this.input = AntCrafting.copyInput(input);
        this.craftingTablePos = craftingTablePos;
        this.path = null;
        this.amountCrafted = amountCrafted;
    }

    public void clearInput() {
        this.input = null;
        this.craftingTablePos = null;
        this.path = null;
        //this.ant.getNavigation().stop();
        this.amountCrafted = 0;
    }

    @Override
    public boolean canUse() {
        ServerLevel level = serverLevel();
        if (level == null || this.input == null || this.input.width() > 3 || this.input.height() > 3 || this.input.isEmpty()) {
            clearInput();
            return false;
        }
        if (!AntCrafting.hasIngredients(this.ant.getInventory(), this.input) || findRecipe(level).isEmpty() || this.amountCrafted <= 0) {
            clearInput();
            return false;
        }
        if (!isValidCraftingTable()) {
            clearInput();
            return false;
        }
        if (isCraftingTableReachable()) {
            return true;
        }
        this.path = this.ant.getNavigation().createPath(this.craftingTablePos, 2,64);
        return this.path != null && !this.path.isDone();
    }

    @Override
    public void start() {
        if (!isCraftingTableReachable() && this.path != null) {
            this.ant.getNavigation().moveTo(this.path, 1.0D);
        }
    }

    @Override
    public boolean canContinueToUse() {
        return this.input != null && this.craftingTablePos != null && isValidCraftingTable()
            && (isCraftingTableReachable() || !this.ant.getNavigation().isDone()) && this.amountCrafted > 0;
    }

    @Override
    public void stop() {
        this.ant.getNavigation().stop();
    }

    @Override
    public void tick() {
        this.ant.getLookControl().setLookAt(this.craftingTablePos.getX() + 0.5D, this.craftingTablePos.getY() + 0.5D, this.craftingTablePos.getZ() + 0.5D);
        if (!isCraftingTableReachable()) {
            if (this.ant.getNavigation().isDone()) {
                this.path = this.ant.getNavigation().createPath(this.craftingTablePos, 2,64);
                if (this.path != null) this.ant.getNavigation().moveTo(this.path, 1.0D);
            }
            return;
        }
        ServerLevel level = serverLevel();
        if (level != null && this.input != null) {
                findRecipe(level).ifPresent(recipe -> {
                    for(int amount = 0; amount < this.amountCrafted; amount++){
                        if (AntCrafting.craft(this.ant, level, this.input, recipe)) {
                            this.ant.swing(InteractionHand.MAIN_HAND, true);
                            LittleAnt.LOGGER.info("[UseCraftingTableGoal] tick: craft one item successfully");
                        }
                        else{
                            LittleAnt.LOGGER.info("[UseCraftingTableGoal] tick: craft failed");
                        }
                    }
                });
        }
        clearInput();
    }

    @Nullable
    private ServerLevel serverLevel() {
        return this.ant.level() instanceof ServerLevel serverLevel ? serverLevel : null;
    }

    private Optional<RecipeHolder<CraftingRecipe>> findRecipe(ServerLevel level) {
        return AntCrafting.findRecipe(level, this.input);
    }

    private boolean isValidCraftingTable() {
        return this.craftingTablePos != null && this.ant.level().hasChunkAt(this.craftingTablePos)
            && this.ant.level().getBlockState(this.craftingTablePos).is(Blocks.CRAFTING_TABLE);
    }

    private boolean isCraftingTableReachable() {
        return this.craftingTablePos != null && this.ant.distanceToSqr(
            this.craftingTablePos.getX() + 0.5D, this.craftingTablePos.getY() + 0.5D - 1.0D, this.craftingTablePos.getZ() + 0.5D) <= 4.0D * 4.0D;
    }
}
