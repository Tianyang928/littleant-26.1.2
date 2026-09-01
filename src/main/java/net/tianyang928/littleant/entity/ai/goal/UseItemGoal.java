package net.tianyang928.littleant.entity.ai.goal;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.tianyang928.littleant.entity.AntEntity;
import net.tianyang928.littleant.entity.ai.interaction.AntInteractionService;

import java.util.EnumSet;

/** Uses the selected item over time so LivingEntity's synchronized use animation is visible. */
public final class UseItemGoal extends Goal {
    private enum Mode { CONSUME, BOW, CROSSBOW_CHARGE, SUSTAINED, INSTANT }

    private static final int BOW_DRAW_TICKS = 20;
    private static final int SUSTAINED_USE_TICKS = 20;
    private final AntEntity ant;
    private final InteractionHand hand;
    private Mode mode;
    private ItemStack startedStack = ItemStack.EMPTY;
    private int useTicks;
    private int targetTicks;
    private boolean started;
    private boolean finished;

    public UseItemGoal(AntEntity ant, InteractionHand hand) {
        this.ant = ant;
        this.hand = hand;
        //this.setFlags(EnumSet.of(Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (finished) return false;
        ItemStack stack = ant.getItemInHand(hand);
        if (stack.isEmpty()) return false;

        if (stack.get(DataComponents.CONSUMABLE) != null) {
            mode = Mode.CONSUME;
        } else if (stack.getItem() instanceof BowItem) {
            mode = Mode.BOW;
            targetTicks = BOW_DRAW_TICKS;
        } else if (stack.getItem() instanceof CrossbowItem && !CrossbowItem.isCharged(stack)) {
            mode = Mode.CROSSBOW_CHARGE;
            targetTicks = CrossbowItem.getChargeDuration(stack, ant);
        } else if (stack.getUseDuration(ant) > 0) {
            mode = Mode.SUSTAINED;
            targetTicks = Math.min(stack.getUseDuration(ant), SUSTAINED_USE_TICKS);
        } else {
            mode = Mode.INSTANT;
        }
        return true;
    }

    @Override
    public void start() {
        started = true;
        useTicks = 0;
        startedStack = ant.getItemInHand(hand);

        switch (mode) {
            case CONSUME -> {
                InteractionResult result = AntInteractionService.useItemAsMob(ant, hand);
                if (!result.consumesAction() || !ant.isUsingItem()) finished = true;
            }
            case BOW, CROSSBOW_CHARGE -> {
                ant.startUsingItem(hand);
                if (!ant.isUsingItem()) finished = true;
            }
            case SUSTAINED -> {
                InteractionResult result = AntInteractionService.useItemAsMob(ant, hand);
                if (!result.consumesAction() || !ant.isUsingItem()) finished = true;
            }
            case INSTANT -> {
                AntInteractionService.useItemAsMob(ant, hand);
                ant.swing(hand, true);
                finished = true;
            }
        }
    }

    @Override
    public boolean canContinueToUse() {
        if (!started || finished) return false;
        ItemStack current = ant.getItemInHand(hand);
        return ItemStack.isSameItemSameComponents(startedStack, current)
                && (mode == Mode.INSTANT || ant.isUsingItem());
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        if (finished) return;
        if (mode == Mode.CONSUME) {
            if (!ant.isUsingItem()) finished = true;
            return;
        }

        if (mode == Mode.SUSTAINED && ++useTicks >= targetTicks) {
            ant.stopUsingItem();
            finished = true;
        } else if ((mode == Mode.BOW || mode == Mode.CROSSBOW_CHARGE) && ++useTicks >= targetTicks) {
            // Ant owns the visible charging state; FakePlayer is used only at the
            // release boundary because vanilla projectile weapons require Player.
            ant.stopUsingItem();
            if (mode == Mode.BOW || !CrossbowItem.isCharged(ant.getItemInHand(hand))) {
                AntInteractionService.useItemAsMob(ant, hand);
            }
            finished = true;
        }
    }

    @Override
    public void stop() {
        if (ant.isUsingItem() && ant.getUsedItemHand() == hand) {
            ant.stopUsingItem();
        }
    }
}
