package net.tianyang928.littleant.entity.ai.interaction;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.tianyang928.littleant.entity.AntEntity;

import java.util.UUID;

/** Player-compatible right-click pipelines for an {@link AntEntity}. */
public final class AntInteractionService {
    private static final GameProfile ANT_PROFILE = new GameProfile(
            UUID.fromString("4b5ab57f-ec22-44a4-ae24-e85c5cabe91d"), "[LittleAnt]");

    private AntInteractionService() {}

    /** Creates the server-side player context used by interactions that require a Player. */
    public static FakePlayer createFakePlayer(AntEntity ant) {
        if (!(ant.level() instanceof ServerLevel level)) {
            throw new IllegalStateException("Ant interactions require a server level");
        }
        FakePlayer fake = FakePlayerFactory.get(level, ANT_PROFILE);
        fake.stopUsingItem();
        fake.setPos(ant.getX(), ant.getY(), ant.getZ());
        fake.setYRot(ant.getYRot());
        fake.setXRot(ant.getXRot());
        return fake;
    }

    public static InteractionResult useItemAsMob(AntEntity ant, InteractionHand hand) {
        if (!(ant.level() instanceof ServerLevel level)) return InteractionResult.PASS;
        ItemStack stack = ant.getItemInHand(hand);
        if (stack.isEmpty()) return InteractionResult.PASS;

        // The task defines a bow use as one fully drawn shot. FakePlayer preserves
        // mod hooks, projectile creation, enchantments, ammo and durability behavior.
        if (stack.getItem() instanceof BowItem) {
            return withFakePlayer(ant, hand, stack, fake -> {
                InteractionResult start = fake.gameMode.useItem(fake, level, fake.getItemInHand(hand), hand);
                if (start.consumesAction()) {
                    fake.getUseItem().releaseUsing(level, fake, 71980);
                    fake.stopUsingItem();
                    return InteractionResult.SUCCESS;
                }
                return start;
            });
        }

        // Crossbow is intentionally two uses: charge on the first call, shoot on the second.
        if (stack.getItem() instanceof CrossbowItem) {
            return withFakePlayer(ant, hand, stack, fake -> {
                ItemStack weapon = fake.getItemInHand(hand);
                if (CrossbowItem.isCharged(weapon)) {
                    return fake.gameMode.useItem(fake, level, weapon, hand);
                }
                InteractionResult start = fake.gameMode.useItem(fake, level, weapon, hand);
                if (start.consumesAction()) {
                    int chargeTicks = CrossbowItem.getChargeDuration(weapon, fake);
                    for (int used = 0; used <= chargeTicks; used++) {
                        weapon.onUseTick(level, fake, weapon.getUseDuration(fake) - used);
                    }
                    fake.releaseUsingItem();
                }
                return start;
            });
        }

        // Shields, spyglasses and similar items only need LivingEntity's sustained
        // use state. A later use call (or another goal) may stop/release that state.
        // Instant-use and modded items need Player context; the full game-mode path
        // also fires NeoForge right-click events and handles transformed stacks.
        return withFakePlayer(ant, hand, stack,
                fake -> fake.gameMode.useItem(fake, level, fake.getItemInHand(hand), hand));
    }

    public static InteractionResult useBlockAsMob(AntEntity ant, BlockPos pos, Direction face,
                                                    InteractionHand hand, boolean secondaryUse,
                                                    boolean useHeldItem) {
        if (!(ant.level() instanceof ServerLevel level) || level.getBlockState(pos).isAir()) {
            return InteractionResult.PASS;
        }
        ItemStack supplied = useHeldItem ? ant.getItemInHand(hand) : ItemStack.EMPTY;
        return withFakePlayer(ant, hand, supplied, fake -> {
            fake.setShiftKeyDown(secondaryUse);
            BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos), face, pos, false);
            return fake.gameMode.useItemOn(fake, level, fake.getItemInHand(hand), hand, hit);
        }, useHeldItem);
    }

    public static InteractionResult interactEntityAsMob(AntEntity ant, Entity target,
                                                         InteractionHand hand, boolean secondaryUse,
                                                         boolean useHeldItem) {
        if (!(ant.level() instanceof ServerLevel) || target == null || !target.isAlive()
                || target.level() != ant.level()) return InteractionResult.PASS;
        ItemStack supplied = useHeldItem ? ant.getItemInHand(hand) : ItemStack.EMPTY;
        return withFakePlayer(ant, hand, supplied, fake -> {
            fake.setShiftKeyDown(secondaryUse);
            Vec3 localHit = new Vec3(0.0D, target.getBbHeight() * 0.5D, 0.0D);
            InteractionResult eventResult = CommonHooks.onInteractEntityAt(fake, target, localHit, hand);
            return eventResult != null ? eventResult : fake.interactOn(target, hand);
        }, useHeldItem);
    }


    private static InteractionResult withFakePlayer(AntEntity ant, InteractionHand hand, ItemStack supplied,
                                                     FakePlayerAction action) {
        return withFakePlayer(ant, hand, supplied, action, true);
    }

    private static InteractionResult withFakePlayer(AntEntity ant, InteractionHand hand, ItemStack supplied,
                                                     FakePlayerAction action, boolean syncBack) {
        ServerLevel level = (ServerLevel) ant.level();
        FakePlayer fake = FakePlayerFactory.get(level, ANT_PROFILE);
        prepare(fake, ant, hand, supplied);
        try {
            InteractionResult result = action.run(fake);
            if (syncBack) {
                ant.setItemInHand(hand, fake.getItemInHand(hand).copy());
                for (int slot = 0; slot < ant.getInventory().getContainerSize(); slot++) {
                    if (slot != ant.getSelectedSlot()) {
                        ant.getInventory().setItem(slot, fake.getInventory().getItem(fakeSlot(slot)).copy());
                    }
                }
            }
            return result;
        } finally {
            fake.stopUsingItem();
            fake.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            fake.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
            for (int slot = 0; slot < fake.getInventory().getContainerSize(); slot++) {
                fake.getInventory().setItem(slot, ItemStack.EMPTY);
            }
            // TODO: Check if this is correct. I'm not sure.
            fake.getCooldowns().removeCooldown(supplied.getItem());
            fake.setShiftKeyDown(false);
            ant.syncSelectedItemNow();
        }
    }

    private static void prepare(FakePlayer fake, AntEntity ant, InteractionHand hand, ItemStack supplied) {
        fake.stopUsingItem();
        fake.setPos(ant.getX(), ant.getY(), ant.getZ());
        fake.setYRot(ant.getYRot());
        fake.setXRot(ant.getXRot());
        fake.setShiftKeyDown(false);
        fake.getInventory().selected = 0;
        fake.setItemInHand(hand, supplied.copy());
        // ProjectileWeaponItem asks LivingEntity#getProjectile. Mirror the ant's
        // remaining inventory so arrows and fireworks are consumed by vanilla code.
        for (int slot = 0; slot < ant.getInventory().getContainerSize(); slot++) {
            if (slot != ant.getSelectedSlot()) {
                fake.getInventory().setItem(fakeSlot(slot), ant.getInventory().getItem(slot).copy());
            }
        }
    }

    /** Fake slot zero is reserved for its selected/main-hand stack. */
    private static int fakeSlot(int antSlot) { return antSlot == 0 ? 9 : antSlot; }

    @FunctionalInterface
    private interface FakePlayerAction { InteractionResult run(FakePlayer player); }
}
