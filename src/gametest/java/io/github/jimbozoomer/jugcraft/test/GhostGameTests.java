package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.agriculture.AuraCandleBlock;
import io.github.jimbozoomer.jugcraft.agriculture.AuraCandleBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.CandleMix;
import io.github.jimbozoomer.jugcraft.agriculture.CandleScent;
import io.github.jimbozoomer.jugcraft.agriculture.CandleWax;
import io.github.jimbozoomer.jugcraft.agriculture.JugcraftAgriculture;
import io.github.jimbozoomer.jugcraft.agriculture.RestlessSpirit;
import io.github.jimbozoomer.jugcraft.agriculture.Spirits;
import io.github.jimbozoomer.jugcraft.agriculture.WaxPotBlockEntity;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * In-game tests for ghost hunting: graves (gravestones and grave mounds) take random ticks and raise spirits only at
 * night, up to a few near; a spirit hides until a Spirit Lantern (or a Revealing candle) reveals it, and only then can
 * be aimed at and caught in a glass bottle as Ectoplasm, earning Ghost Hunter; revealed, it shies from a player but never
 * leaves its haunt, so it can be cornered; blows pass through it and it fades at dawn, leaving nothing; Ectoplasm scents
 * wax (giving its bottle back), and a Ghostly candle turns players near invisible.
 *
 * <p>Spirits are stepped by hand with the night passed in, as the shared world clock may read day, or, where a test needs
 * them to move, given an AI that always thinks it is night. Only one test holds a Spirit Lantern; the others reveal their
 * spirits directly. Every spirit a test makes is gone before it ends.
 */
public class GhostGameTests {
	/** A spirit that always thinks it is night and is always revealed: its own AI, but not the shared world clock. */
	private static final class NightSpirit extends RestlessSpirit {
		NightSpirit(ServerLevel level) {
			super(JugcraftAgriculture.RESTLESS_SPIRIT, level);
		}

		@Override
		protected void customServerAiStep(ServerLevel level) {
			reveal(level.getGameTime() + REVEAL_TICKS);
			step(level, true);
		}
	}

	private static Item item(String id) {
		return JugcraftAgriculture.item(id);
	}

	private static Block block(String id) {
		return JugcraftAgriculture.block(id);
	}

	private static void floor(GameTestHelper helper) {
		for (int x = 0; x <= 7; x++) {
			for (int z = 0; z <= 7; z++) {
				helper.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
			}
		}
	}

	private static ServerPlayer player(GameTestHelper helper, BlockPos standAt, ItemStack held) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setGameMode(GameType.SURVIVAL);
		BlockPos absolute = helper.absolutePos(standAt);
		player.setPos(absolute.getX() + 0.5, absolute.getY(), absolute.getZ() + 0.5);
		player.setItemInHand(InteractionHand.MAIN_HAND, held);
		return player;
	}

	/** A spirit floating at {@code pos}, haunting the block below it, with no AI of its own: the test steps it by hand. */
	private static RestlessSpirit spirit(GameTestHelper helper, BlockPos pos) {
		RestlessSpirit spirit = helper.spawnWithNoFreeWill(JugcraftAgriculture.RESTLESS_SPIRIT, pos);
		spirit.setNoAi(true);
		spirit.setPersistenceRequired();
		spirit.setHome(helper.absolutePos(pos.below()));
		return spirit;
	}

	private static int count(ServerPlayer player, Item item) {
		int count = 0;
		for (int slot = 0; slot < 36; slot++) {
			ItemStack stack = player.getInventory().getItem(slot);
			if (stack.is(item)) {
				count += stack.getCount();
			}
		}
		return count;
	}

	private static boolean earned(ServerPlayer player, String id) {
		AdvancementHolder advancement = player.level().getServer().getAdvancements().get(Jugcraft.id(id));
		return advancement != null && player.getAdvancements().getOrStartProgress(advancement).isDone();
	}

	private static int near(ServerLevel level, BlockPos at) {
		return level.getEntitiesOfClass(RestlessSpirit.class, new AABB(at).inflate(Spirits.NEAR_RANGE)).size();
	}

	// ---------------------------------------------------------------- graves

	/**
	 * Every grave takes random ticks. By day none stirs; at night spirits rise over a gravestone, haunting it, unseen and
	 * out of any aim, until {@value Spirits#NEAR_CAP} are near; a grave mound stirs too.
	 */
	@GameTest(maxTicks = 20)
	public void gravesStirAtNight(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		floor(helper);
		BlockPos stone = new BlockPos(2, 2, 2);
		BlockPos mound = new BlockPos(5, 2, 5);
		helper.setBlock(stone, block("rounded_gravestone"));
		helper.setBlock(mound, block("grave_mound"));
		helper.assertTrue(helper.getBlockState(stone).isRandomlyTicking() && helper.getBlockState(mound).isRandomlyTicking()
				&& block("cross_gravestone").defaultBlockState().isRandomlyTicking() && block("obelisk_gravestone").defaultBlockState().isRandomlyTicking(),
				"Every grave takes random ticks");
		BlockPos grave = helper.absolutePos(stone);
		helper.assertTrue(Spirits.rise(level, grave, false) == null, "By day no spirit rises");
		List<RestlessSpirit> risen = new ArrayList<>();
		for (int i = 0; i < 10; i++) {
			RestlessSpirit spirit = Spirits.rise(level, grave, true);
			if (spirit == null) {
				break;
			}
			risen.add(spirit);
		}
		int near = near(level, grave);
		helper.assertTrue(!risen.isEmpty() && near == Spirits.NEAR_CAP, "At night spirits rise, until " + Spirits.NEAR_CAP + " are near: " + near);
		RestlessSpirit first = risen.get(0);
		helper.assertTrue(first.home().equals(grave) && first.getY() > grave.getY() + 0.9, "Each rises over its grave, which it haunts");
		helper.assertTrue(!first.revealed() && !first.isPickable(), "unseen, and out of any aim");
		for (RestlessSpirit spirit : risen) {
			spirit.discard();
		}
		RestlessSpirit fromMound = Spirits.rise(level, helper.absolutePos(mound), true);
		helper.assertTrue(fromMound != null && fromMound.home().equals(helper.absolutePos(mound)), "A grave mound stirs too");
		fromMound.discard();
		helper.succeed();
	}

	// ---------------------------------------------------------------- the lantern and the bottle

	/**
	 * Hidden, a spirit can't be caught. A Spirit Lantern held in the off hand fifteen blocks above doesn't reveal it;
	 * within {@value Spirits#REVEAL_RADIUS} blocks it does, so it fades in and can be aimed at; a glass bottle then catches
	 * it as Ectoplasm (one bottle of two), earning Ghost Hunter. (The lantern is out of reach of other tests' spirits, high
	 * above, and put away at once.)
	 */
	@GameTest(maxTicks = 20)
	public void aLanternRevealsAndABottleCatches(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		floor(helper);
		RestlessSpirit spirit = spirit(helper, new BlockPos(4, 3, 4));
		ServerPlayer catcher = player(helper, new BlockPos(4, 2, 2), new ItemStack(Items.GLASS_BOTTLE, 2));
		helper.assertFalse(spirit.look(level), "With no lantern near it stays hidden");
		catcher.interactOn(spirit, InteractionHand.MAIN_HAND, spirit.position());
		helper.assertTrue(!spirit.isRemoved() && catcher.getMainHandItem().getCount() == 2 && !spirit.isPickable(), "Hidden, a bottle can't catch it");

		ServerPlayer bearer = player(helper, new BlockPos(4, 3 + Spirits.REVEAL_RADIUS + 3, 4), ItemStack.EMPTY);
		bearer.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(item("spirit_lantern")));
		helper.assertFalse(spirit.look(level), "A lantern " + (Spirits.REVEAL_RADIUS + 3) + " blocks above doesn't reach it");
		BlockPos close = helper.absolutePos(new BlockPos(1, 2, 6));
		bearer.setPos(close.getX() + 0.5, close.getY(), close.getZ() + 0.5);
		long now = level.getGameTime();
		boolean revealed = spirit.look(level);
		bearer.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
		helper.assertTrue(revealed && spirit.revealed() && spirit.isPickable(), "A lantern held near, even in the off hand, reveals it");
		helper.assertTrue(spirit.visibility(now, 0.0F) == 0.0F && spirit.visibility(now + RestlessSpirit.FADE_TICKS, 0.0F) == 1.0F
				&& spirit.visibility(now + RestlessSpirit.REVEAL_TICKS, 0.0F) == 0.0F, "It fades in, and out when no longer revealed");

		InteractionResult caught = catcher.interactOn(spirit, InteractionHand.MAIN_HAND, spirit.position());
		helper.assertTrue(caught.consumesAction() && spirit.isRemoved(), "Revealed, a glass bottle catches it: " + caught);
		helper.assertTrue(catcher.getMainHandItem().is(Items.GLASS_BOTTLE) && catcher.getMainHandItem().getCount() == 1
				&& count(catcher, item("ectoplasm")) == 1, "One bottle filled with Ectoplasm");
		helper.assertTrue(earned(catcher, "ghost_hunter"), "Catching one earns Ghost Hunter");
		helper.succeed();
	}

	// ---------------------------------------------------------------- the chase

	/**
	 * Revealed, a spirit shies away from a player two blocks off (a sneaking one gets closer); hidden, it heeds no one. Its
	 * haunt keeps it within {@value RestlessSpirit#HAUNT_RADIUS} blocks of its grave and a little above it, so a player
	 * herding one across the ground drives it to the edge of its haunt and no farther.
	 */
	@GameTest(maxTicks = 160)
	public void aRevealedSpiritShiesButStaysInItsHaunt(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		floor(helper);
		RestlessSpirit shy = spirit(helper, new BlockPos(4, 3, 4));
		shy.reveal(level.getGameTime() + 200);
		ServerPlayer chaser = player(helper, new BlockPos(4, 2, 2), ItemStack.EMPTY);
		helper.assertTrue(shy.step(level, true), "A player two blocks off makes a revealed spirit shy away");
		helper.assertTrue(shy.getDeltaMovement().z > 0.0, "away from them: " + shy.getDeltaMovement());
		chaser.setShiftKeyDown(true);
		helper.assertFalse(shy.step(level, true), "A sneaking player two blocks off doesn't");
		chaser.setShiftKeyDown(false);
		RestlessSpirit hidden = spirit(helper, new BlockPos(1, 3, 6));
		chaser.setPos(hidden.getX(), hidden.getY() - 0.5, hidden.getZ() - 1.5);
		helper.assertFalse(hidden.step(level, true), "Hidden, it heeds no one");
		shy.discard();
		hidden.discard();

		Vec3 haunt = new Vec3(10.5, 64.0, 0.5);
		Vec3 kept = RestlessSpirit.within(haunt, new Vec3(30.5, 80.0, 0.5));
		helper.assertTrue(Math.abs(kept.x - (10.5 + RestlessSpirit.HAUNT_RADIUS)) < 1.0E-6 && Math.abs(kept.y - (64.0 + RestlessSpirit.HAUNT_HEIGHT)) < 1.0E-6,
				"Its haunt holds it within " + RestlessSpirit.HAUNT_RADIUS + " blocks across and " + RestlessSpirit.HAUNT_HEIGHT + " up: " + kept);
		helper.assertTrue(RestlessSpirit.within(haunt, new Vec3(11.5, 60.0, 0.5)).y == 64.5, "and never down into the ground by its grave");

		// Herded east across the ground with its own AI, it goes as far as its haunt allows.
		BlockPos home = helper.absolutePos(new BlockPos(1, 2, 4));
		NightSpirit herded = new NightSpirit(level);
		herded.setHome(home);
		herded.setPersistenceRequired();
		herded.snapTo(home.getX() + 0.5, home.getY() + 1.0, home.getZ() + 0.5, 0.0F, 0.0F);
		level.addFreshEntity(herded);
		double startX = herded.getX();
		helper.onEachTick(() -> chaser.setPos(herded.getX() - 1.5, herded.getY() - 0.5, herded.getZ()));
		helper.runAfterDelay(120, () -> {
			double edge = home.getX() + 0.5 + RestlessSpirit.HAUNT_RADIUS;
			helper.assertTrue(herded.getX() > startX + 3.0, "Herded, it shies a good way east: " + (herded.getX() - startX));
			helper.assertTrue(herded.getX() <= edge + 0.35, "but no farther than its haunt's edge: " + herded.getX() + " of " + edge);
			herded.discard();
			helper.succeed();
		});
	}

	// ---------------------------------------------------------------- blows, dawn and candles

	/**
	 * A blow passes through a spirit; at dawn it fades, leaving nothing. A Revealing candle's aura makes it glow, which
	 * reveals it to all.
	 */
	@GameTest(maxTicks = 20)
	public void blowsPassThroughDawnFadesCandlesReveal(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		floor(helper);
		RestlessSpirit struck = spirit(helper, new BlockPos(4, 3, 4));
		struck.reveal(level.getGameTime() + 200);
		ServerPlayer player = player(helper, new BlockPos(4, 2, 2), ItemStack.EMPTY);
		float health = struck.getHealth();
		helper.assertTrue(!struck.hurtServer(level, level.damageSources().playerAttack(player), 6.0F) && !struck.isRemoved()
				&& struck.getHealth() == health, "A blow passes through it");
		struck.step(level, false);
		helper.assertTrue(struck.isRemoved(), "At dawn it fades");
		helper.assertTrue(level.getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(new BlockPos(4, 3, 4))).inflate(3.0)).isEmpty(),
				"leaving nothing");

		RestlessSpirit glowing = spirit(helper, new BlockPos(2, 3, 6));
		helper.assertFalse(glowing.look(level), "Unlit, nothing reveals it");
		BlockPos candlePos = new BlockPos(5, 2, 6);
		CandleMix mix = CandleMix.first(CandleWax.BEESWAX, 0x9FFFE8, List.of(CandleScent.REVEALING), false, false);
		helper.setBlock(candlePos, block("aura_candle").defaultBlockState().setValue(AuraCandleBlock.DIPS, mix.dips()).setValue(AuraCandleBlock.LIT, true));
		helper.getBlockEntity(candlePos, AuraCandleBlockEntity.class).setMix(mix);
		AuraCandleBlockEntity.pulse(level, helper.absolutePos(candlePos), mix);
		helper.assertTrue(glowing.hasEffect(MobEffects.GLOWING) && glowing.look(level) && glowing.revealed(), "A Revealing candle's aura reveals it");
		glowing.discard();
		helper.succeed();
	}

	/**
	 * Ectoplasm is the Ghostly scent: stirred into molten wax it scents it and gives its bottle back, and a lit Ghostly
	 * candle turns the players near invisible. The Spirit Lantern's recipe loads.
	 */
	@GameTest(maxTicks = 20)
	public void ectoplasmMakesGhostlyCandles(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		floor(helper);
		helper.assertTrue(CandleScent.of(new ItemStack(item("ectoplasm"))) == CandleScent.GHOSTLY && CandleScent.GHOSTLY.effect == MobEffects.INVISIBILITY,
				"Ectoplasm is the Ghostly scent, of invisibility");
		BlockPos potPos = new BlockPos(3, 2, 3);
		helper.setBlock(potPos, block("wax_melting_pot"));
		WaxPotBlockEntity pot = helper.getBlockEntity(potPos, WaxPotBlockEntity.class);
		ServerPlayer player = player(helper, new BlockPos(3, 2, 1), new ItemStack(Items.HONEYCOMB, 2));
		BlockPos absolute = helper.absolutePos(potPos);
		BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(absolute).relative(Direction.UP, 0.5), Direction.UP, absolute, false);
		for (int i = 0; i < 2; i++) {
			player.gameMode.useItemOn(player, level, player.getMainHandItem(), InteractionHand.MAIN_HAND, hit);
		}
		pot.meltAll();
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item("ectoplasm"), 2));
		player.gameMode.useItemOn(player, level, player.getMainHandItem(), InteractionHand.MAIN_HAND, hit);
		helper.assertTrue(pot.scents().contains(CandleScent.GHOSTLY), "Stirred into molten wax, it scents it");
		helper.assertTrue(player.getMainHandItem().getCount() == 1 && count(player, Items.GLASS_BOTTLE) == 1, "and gives its bottle back");

		BlockPos candlePos = new BlockPos(5, 2, 5);
		CandleMix mix = CandleMix.first(CandleWax.BEESWAX, CandleScent.GHOSTLY.color, List.of(CandleScent.GHOSTLY), false, false);
		helper.setBlock(candlePos, block("aura_candle").defaultBlockState().setValue(AuraCandleBlock.DIPS, mix.dips()).setValue(AuraCandleBlock.LIT, true));
		helper.getBlockEntity(candlePos, AuraCandleBlockEntity.class).setMix(mix);
		AuraCandleBlockEntity.pulse(level, helper.absolutePos(candlePos), mix);
		helper.assertTrue(player.hasEffect(MobEffects.INVISIBILITY), "A lit Ghostly candle turns players near invisible");
		helper.assertTrue(level.recipeAccess().byKey(ResourceKey.create(Registries.RECIPE, Jugcraft.id("spirit_lantern"))).isPresent(),
				"The Spirit Lantern's recipe loads");
		helper.succeed();
	}
}
