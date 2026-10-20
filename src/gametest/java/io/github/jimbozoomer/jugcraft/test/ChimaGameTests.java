package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.creatures.chima.Chima;
import io.github.jimbozoomer.jugcraft.creatures.chima.ChimaMod;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;

/**
 * In-game tests for Chima the lion cub (docs/features/chima.md): a wild one hunts down a cow and then rests; beef tames
 * it like a cat, now and then, and its owner (only) has it sit and get up; a jack o'lantern, rotten flesh, a bone and a
 * fish each give it its look, and its owner sneaking with an empty hand gives it its own back; a tame one leaves cows be,
 * shrugs off its owner's blows and is healed by beef; and its tags and spawn egg load.
 */
public class ChimaGameTests {
	/** A grass pen eight blocks across, walled in glass two high so neither cub nor cow wanders out. */
	private static void pen(GameTestHelper helper) {
		for (int x = 0; x <= 7; x++) {
			for (int z = 0; z <= 7; z++) {
				helper.setBlock(new BlockPos(x, 1, z), Blocks.GRASS_BLOCK);
				if (x == 0 || z == 0 || x == 7 || z == 7) {
					helper.setBlock(new BlockPos(x, 2, z), Blocks.GLASS);
					helper.setBlock(new BlockPos(x, 3, z), Blocks.GLASS);
				}
			}
		}
	}

	private static ServerPlayer player(GameTestHelper helper, BlockPos standAt) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setGameMode(GameType.SURVIVAL);
		BlockPos absolute = helper.absolutePos(standAt);
		player.setPos(absolute.getX() + 0.5, absolute.getY(), absolute.getZ() + 0.5);
		// Test players start out holding the Creative Tower Guide: empty their hands.
		player.getInventory().clearContent();
		return player;
	}

	private static void interact(ServerPlayer player, Chima chima, boolean sneaking) {
		player.setShiftKeyDown(sneaking);
		player.interactOn(chima, InteractionHand.MAIN_HAND, chima.position());
		player.setShiftKeyDown(false);
	}

	/** Feeds it beef until it takes (at most 60 pieces, which fail to tame it about once in 37 billion). */
	private static int tame(ServerPlayer player, Chima chima) {
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BEEF, 64));
		int fed = 0;
		while (!chima.tame() && fed < 60) {
			interact(player, chima, false);
			fed++;
		}
		return fed;
	}

	/** A wild cub in a pen with a cow runs it down; afterwards it rests from hunting for a while. */
	@GameTest(maxTicks = 600)
	public void aWildOneHuntsCows(GameTestHelper helper) {
		pen(helper);
		Chima chima = helper.spawn(ChimaMod.CHIMA, new BlockPos(2, 2, 2));
		Cow cow = helper.spawn(EntityTypes.COW, new BlockPos(5, 2, 5));
		helper.succeedWhen(() -> {
			helper.assertTrue(!cow.isAlive(), "The cow is still up: " + cow.getHealth() + " health, the cub after " + chima.getTarget());
			helper.assertTrue(chima.rest() > 0, "Having made its kill, it rests");
		});
	}

	/**
	 * Beef tames it, though not every piece: each one is eaten. Tamed, it is the feeder's and sits. Its owner's empty hand
	 * has it get up and sit again; a stranger's does nothing.
	 */
	@GameTest
	public void beefTamesItLikeACat(GameTestHelper helper) {
		pen(helper);
		ServerPlayer owner = player(helper, new BlockPos(2, 2, 3));
		ServerPlayer stranger = player(helper, new BlockPos(5, 2, 3));
		Chima chima = helper.spawn(ChimaMod.CHIMA, new BlockPos(3, 2, 3));
		helper.assertTrue(!chima.tame() && !chima.sitting(), "It starts out wild");
		int fed = tame(owner, chima);
		helper.assertTrue(chima.tame() && chima.ownedBy(owner) && !chima.ownedBy(stranger), "Beef tamed it to its feeder after " + fed + " pieces");
		helper.assertTrue(owner.getMainHandItem().getCount() == 64 - fed, "Every piece was eaten: " + owner.getMainHandItem().getCount() + " left of 64 after " + fed);
		helper.assertTrue(chima.sitting(), "Newly tamed, it sits");
		owner.getInventory().clearContent();
		interact(stranger, chima, false);
		helper.assertTrue(chima.sitting(), "A stranger can't have it get up");
		interact(owner, chima, false);
		helper.assertTrue(!chima.sitting(), "Its owner has it get up");
		interact(owner, chima, false);
		helper.assertTrue(chima.sitting(), "and sit again");
		helper.succeed();
	}

	/**
	 * A jack o'lantern, rotten flesh, a bone and a fish each give a wild one its look, using the item up; a tame one's look
	 * is its owner's to change, not a stranger's, and its owner sneaking with an empty hand gives it its own back.
	 */
	@GameTest
	public void foodChangesItsLook(GameTestHelper helper) {
		pen(helper);
		ServerPlayer owner = player(helper, new BlockPos(2, 2, 3));
		ServerPlayer stranger = player(helper, new BlockPos(5, 2, 3));
		Chima chima = helper.spawn(ChimaMod.CHIMA, new BlockPos(3, 2, 3));
		Object[][] looks = {{Items.JACK_O_LANTERN, Chima.Skin.PUMPKIN}, {Items.ROTTEN_FLESH, Chima.Skin.ZOMBIE}, {Items.BONE, Chima.Skin.SKELETON},
				{Items.COD, Chima.Skin.SHARK}, {Items.SALMON, Chima.Skin.SHARK}};
		for (Object[] look : looks) {
			Item item = (Item) look[0];
			Chima.Skin skin = (Chima.Skin) look[1];
			Chima.Skin before = chima.skin();
			stranger.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item, 2));
			interact(stranger, chima, false);
			int left = stranger.getMainHandItem().getCount();
			helper.assertTrue(chima.skin() == skin, item + " gives it the " + skin.id() + " look, not " + chima.skin().id());
			helper.assertTrue(left == (before == skin ? 2 : 1), item + " is used up only when the look changes: " + left + " left");
		}
		stranger.getInventory().clearContent();

		tame(owner, chima);
		owner.getInventory().clearContent();
		stranger.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BONE));
		interact(stranger, chima, false);
		helper.assertTrue(chima.skin() == Chima.Skin.SHARK && stranger.getMainHandItem().getCount() == 1, "A stranger can't change a tame one's look");
		owner.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BONE));
		interact(owner, chima, false);
		helper.assertTrue(chima.skin() == Chima.Skin.SKELETON, "Its owner can");
		owner.getInventory().clearContent();
		boolean satBefore = chima.sitting();
		interact(owner, chima, true);
		helper.assertTrue(chima.skin() == Chima.Skin.NORMAL && chima.sitting() == satBefore, "Its owner sneaking with an empty hand gives it its own look back");
		helper.succeed();
	}

	/** A tame cub leaves a cow in its pen alone, doesn't take its owner's blows, and beef heals it. */
	@GameTest(maxTicks = 300)
	public void aTameOneLeavesCowsBe(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		pen(helper);
		ServerPlayer owner = player(helper, new BlockPos(1, 2, 6));
		Chima chima = helper.spawn(ChimaMod.CHIMA, new BlockPos(2, 2, 2));
		Cow cow = helper.spawn(EntityTypes.COW, new BlockPos(5, 2, 5));
		tame(owner, chima);
		chima.setSitting(false);
		chima.setHealth(8.0F);
		helper.assertTrue(!chima.hurtServer(level, level.damageSources().playerAttack(owner), 4.0F) && chima.getHealth() == 8.0F, "Its owner's blows don't hurt it");
		owner.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.COOKED_BEEF, 3));
		interact(owner, chima, false);
		helper.assertTrue(chima.getHealth() == 8.0F + Chima.BEEF_HEAL && owner.getMainHandItem().getCount() == 2, "Beef heals it: " + chima.getHealth());
		helper.runAfterDelay(240, () -> {
			helper.assertTrue(cow.isAlive() && cow.getHealth() == cow.getMaxHealth(), "The tame cub let the cow be: " + cow.getHealth() + " health");
			helper.succeed();
		});
	}

	/** Retaliation cannot bypass the shared creature claim boundary or hurt its owner. */
	@GameTest
	public void tameAttacksRespectClaimsAndOwner(GameTestHelper helper) {
		pen(helper);
		var owner = player(helper, new BlockPos(2, 2, 3));
		var stranger = player(helper, new BlockPos(5, 2, 3));
		var chima = helper.spawn(ChimaMod.CHIMA, new BlockPos(3, 2, 3));
		var cow = helper.spawn(EntityTypes.COW, new BlockPos(4, 2, 3));
		chima.tameTo(owner);
		helper.assertTrue(!chima.comeToOwner(), "A sitting companion must stay put");
		chima.setSitting(false);
		try (var claims = TestClaims.open().creature(cow, stranger.getUUID())) {
			chima.setTarget(cow);
			helper.assertTrue(!chima.doHurtTarget(helper.getLevel(), cow) && cow.getHealth() == cow.getMaxHealth(),
					"Tamed retaliation must respect another player's claimed animal");
			helper.assertTrue(chima.getTarget() == null, "Release a forbidden target");
		}
		float health = owner.getHealth();
		helper.assertTrue(!chima.doHurtTarget(helper.getLevel(), owner) && owner.getHealth() == health,
				"A pet must not hurt its own owner");
		helper.succeed();
	}

	/** Beef is its food; each look's items give that look and others none; its spawn egg makes one. */
	@GameTest
	public void chimaDataLoads(GameTestHelper helper) {
		helper.assertTrue(new ItemStack(Items.BEEF).is(Chima.FOOD) && new ItemStack(Items.COOKED_BEEF).is(Chima.FOOD), "Beef is its food");
		helper.assertTrue(Chima.Skin.of(new ItemStack(Items.JACK_O_LANTERN)) == Chima.Skin.PUMPKIN
				&& Chima.Skin.of(new ItemStack(Items.ROTTEN_FLESH)) == Chima.Skin.ZOMBIE && Chima.Skin.of(new ItemStack(Items.BONE)) == Chima.Skin.SKELETON
				&& Chima.Skin.of(new ItemStack(Items.TROPICAL_FISH)) == Chima.Skin.SHARK && Chima.Skin.of(new ItemStack(Items.PUFFERFISH)) == Chima.Skin.SHARK,
				"Each look's items give it");
		helper.assertTrue(Chima.Skin.of(new ItemStack(Items.BEEF)) == null && Chima.Skin.of(new ItemStack(Items.CARVED_PUMPKIN)) == null, "Other things give none");
		helper.assertTrue(new ItemStack(ChimaMod.SPAWN_EGG).getItem() == ChimaMod.SPAWN_EGG, "Its spawn egg is registered");
		helper.succeed();
	}
}
