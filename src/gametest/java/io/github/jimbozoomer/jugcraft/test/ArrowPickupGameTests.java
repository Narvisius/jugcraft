package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.nouveau.NouveauGear;
import io.github.jimbozoomer.jugcraft.weapons.ArmBowItem;
import io.github.jimbozoomer.jugcraft.weapons.JugcraftArms;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

/** Fires real bows and returns their arrows through vanilla's survival inventory pickup path. */
public class ArrowPickupGameTests {
    @GameTest
    public void fiveRecoveredArrowsStackWithUnfiredAmmo(GameTestHelper helper) {
        for (var item : List.of(JugcraftArms.KIT.get("bronze_longbow"), JugcraftArms.KIT.get("steel_longbow"), Items.BOW)) {
            var player = archer(helper, new ItemStack(item), new ItemStack(Items.ARROW, 16));
            for (int shot = 0; shot < 5; shot++) {
                var arrow = loose(helper, player);
                arrow.playerTouch(player);
                helper.assertTrue(arrow.isRemoved(), "Survival player picks up the arrow");
            }
            assertOneStack(helper, player, new ItemStack(Items.ARROW), 16);
        }
        helper.succeed();
    }

    @GameTest
    public void pickupPreservesPotionNameAndSpectralArrowIdentity(GameTestHelper helper) {
        var tipped = new ItemStack(Items.TIPPED_ARROW, 16);
        tipped.set(DataComponents.POTION_CONTENTS, new PotionContents(Potions.POISON));
        tipped.set(DataComponents.CUSTOM_NAME, Component.literal("Keep this arrow's name"));
        for (var ammo : List.of(tipped, new ItemStack(Items.SPECTRAL_ARROW, 16))) {
            var expected = ammo.copyWithCount(1);
            var player = archer(helper, new ItemStack(JugcraftArms.KIT.get("bronze_longbow")), ammo);
            var arrow = loose(helper, player);
            arrow.playerTouch(player);
            helper.assertTrue(arrow.isRemoved(), "Special arrow can be recovered");
            assertOneStack(helper, player, expected, 16);
        }
        helper.succeed();
    }

    @GameTest
    public void previouslyMarkedArrowCanBeRecoveredClean(GameTestHelper helper) {
        var oldAmmo = new ItemStack(Items.ARROW);
        oldAmmo.set(NouveauGear.ARROW_SHOT, java.util.UUID.randomUUID());
        var player = archer(helper, new ItemStack(JugcraftArms.KIT.get("bronze_longbow")), oldAmmo);
        var arrow = loose(helper, player);
        player.getInventory().setItem(1, new ItemStack(Items.ARROW, 15));
        arrow.playerTouch(player);
        helper.assertTrue(arrow.isRemoved(), "An arrow marked by an older build can be recovered");
        assertOneStack(helper, player, new ItemStack(Items.ARROW), 16);
        helper.succeed();
    }

    @GameTest
    public void failedPickupKeepsShotIdentityAndPickupRestrictions(GameTestHelper helper) {
        var player = archer(helper, new ItemStack(JugcraftArms.KIT.get("bronze_longbow")), new ItemStack(Items.ARROW, 16));
        var arrow = loose(helper, player);
        var shot = arrow.getPickupItemStackOrigin().get(NouveauGear.ARROW_SHOT);
        for (int slot = 0; slot < 36; slot++) player.getInventory().setItem(slot, new ItemStack(Items.STONE, 64));
        arrow.playerTouch(player);
        helper.assertTrue(!arrow.isRemoved() && shot.equals(arrow.getPickupItemStackOrigin().get(NouveauGear.ARROW_SHOT)),
                "Full inventory must not erase the live projectile's shared spell-shot identity");
        player.getInventory().setItem(1, ItemStack.EMPTY);
        for (var restriction : List.of(AbstractArrow.Pickup.DISALLOWED, AbstractArrow.Pickup.CREATIVE_ONLY)) {
            arrow.pickup = restriction;
            arrow.playerTouch(player);
            helper.assertTrue(!arrow.isRemoved() && player.getInventory().getItem(1).isEmpty(), "Pickup restriction remains enforced");
        }
        arrow.pickup = AbstractArrow.Pickup.ALLOWED;
        arrow.playerTouch(player);
        helper.assertTrue(arrow.isRemoved(), "Pickup succeeds once there is space");
        assertOneStack(helper, player, new ItemStack(Items.ARROW), 1);
        helper.succeed();
    }

    private static ServerPlayer archer(GameTestHelper helper, ItemStack bow, ItemStack ammo) {
        var player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        var at = helper.absolutePos(new net.minecraft.core.BlockPos(1, 2, 1));
        player.setPos(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
        player.getInventory().clearContent();
        player.setItemInHand(InteractionHand.MAIN_HAND, bow);
        player.getInventory().setItem(1, ammo);
        return player;
    }

    private static AbstractArrow loose(GameTestHelper helper, ServerPlayer player) {
        var bow = player.getMainHandItem();
        int draw = bow.getItem() instanceof ArmBowItem longbow ? longbow.ranged().draw() : BowItem.MAX_DRAW_DURATION;
        helper.assertTrue(bow.getItem().releaseUsing(bow, helper.getLevel(), player, bow.getUseDuration(player) - draw), "Bow fires");
        var arrows = helper.getLevel().getEntitiesOfClass(AbstractArrow.class, player.getBoundingBox().inflate(4), a -> a.getOwner() == player && !a.isRemoved());
        helper.assertTrue(arrows.size() == 1, "Exactly one arrow was fired, found " + arrows.size());
        var arrow = arrows.getFirst();
        // Advance the real arrow tick hook deterministically, without relying on flight or collision timing.
        arrow.setNoPhysics(true);
        arrow.setDeltaMovement(Vec3.ZERO);
        arrow.tickCount = 1;
        arrow.tick();
        helper.assertTrue(arrow.getPickupItemStackOrigin().has(NouveauGear.ARROW_SHOT), "Live arrow has its internal shot ID");
        return arrow;
    }

    private static void assertOneStack(GameTestHelper helper, ServerPlayer player, ItemStack expected, int count) {
        int stacks = 0;
        int total = 0;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            var stack = player.getInventory().getItem(slot);
            if (!stack.is(expected.getItem())) continue;
            stacks++;
            total += stack.getCount();
            helper.assertTrue(ItemStack.isSameItemSameComponents(expected, stack), "Recovered ammo keeps only its original item components");
        }
        helper.assertTrue(stacks == 1 && total == count, "Expected one stack of " + count + ", found " + stacks + " stacks holding " + total);
    }
}
