package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.agriculture.BubblingCauldronBlock;
import io.github.jimbozoomer.jugcraft.agriculture.BubblingCauldronBlock.Brew;
import io.github.jimbozoomer.jugcraft.agriculture.Hexes;
import io.github.jimbozoomer.jugcraft.agriculture.JugcraftAgriculture;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * In-game tests for the hex brews (fall addition 21): a brew bubbling over a heat source turns into a hex brew with its
 * hex ingredient, a cold one does not; a hex brew fills three bottles and is then empty, and only for a player with
 * build rights; the Shrinking Draught halves you, the Giant's Draught grows you only where there is room, each cancels
 * the other, Flying Ointment gives slow falling, and a shrunk player with no room to grow stays small; and the data
 * loads.
 */
public class HexBrewGameTests {
	private static final double EPSILON = 1.0E-6;

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

	private static InteractionResult use(GameTestHelper helper, ServerPlayer player, BlockPos pos) {
		BlockPos absolute = helper.absolutePos(pos);
		BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(absolute).relative(Direction.UP, 0.5), Direction.UP, absolute, false);
		return player.gameMode.useItemOn(player, helper.getLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND, hit);
	}

	private static Brew contents(GameTestHelper helper, BlockPos pos) {
		return helper.getBlockState(pos).getValue(BubblingCauldronBlock.CONTENTS);
	}

	private static int doses(GameTestHelper helper, BlockPos pos) {
		return helper.getBlockState(pos).getValue(BubblingCauldronBlock.DOSES_LEFT);
	}

	private static int carried(ServerPlayer player, Item item) {
		int count = 0;
		for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
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

	private static double scale(ServerPlayer player) {
		return player.getAttributeValue(Attributes.SCALE);
	}

	/** Drinks the draught {@code id} as if its use had run its full time, returning what is left in hand. */
	private static ItemStack drink(ServerPlayer player, String id) {
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item(id)));
		return player.getMainHandItem().finishUsingItem(player.level(), player);
	}

	/**
	 * Over a magma block, water, a spider eye (green) and a brown mushroom make the Shrinking Draught's brew with three
	 * doses; a spider eye then does not spoil it; a player without build rights draws nothing; three glass bottles draw
	 * three draughts and the pot is empty. The same brew in a cold pot stays green. Beans make the orange brew a
	 * giant's, a phantom membrane the purple brew flying ointment, and neither works on the wrong brew.
	 */
	@GameTest
	public void aHeatedBrewTurnsToAHex(GameTestHelper helper) {
		floor(helper);
		BlockPos pot = new BlockPos(2, 2, 2);
		helper.setBlock(pot.below(), Blocks.MAGMA_BLOCK);
		helper.setBlock(pot, block("bubbling_cauldron"));
		ServerPlayer witch = player(helper, new BlockPos(2, 2, 4), new ItemStack(Items.WATER_BUCKET));
		use(helper, witch, pot);
		witch.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.SPIDER_EYE));
		use(helper, witch, pot);
		helper.assertTrue(contents(helper, pot) == Brew.GREEN, "Water and a spider eye make the green brew");
		witch.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BROWN_MUSHROOM, 2));
		use(helper, witch, pot);
		helper.assertTrue(contents(helper, pot) == Brew.SHRINKING && doses(helper, pot) == BubblingCauldronBlock.DOSES
				&& witch.getMainHandItem().getCount() == 1, "A brown mushroom over the fire makes the shrinking brew, three doses");
		helper.assertTrue(helper.getBlockState(pot).getLightEmission() == BubblingCauldronBlock.BREW_LIGHT, "The hex brew glows");
		witch.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.SPIDER_EYE));
		use(helper, witch, pot);
		helper.assertTrue(contents(helper, pot) == Brew.SHRINKING && witch.getMainHandItem().getCount() == 1, "A brew ingredient does not spoil a hex");

		ServerPlayer visitor = player(helper, new BlockPos(3, 2, 4), new ItemStack(Items.GLASS_BOTTLE));
		visitor.setGameMode(GameType.ADVENTURE);
		use(helper, visitor, pot);
		helper.assertTrue(doses(helper, pot) == BubblingCauldronBlock.DOSES && visitor.getMainHandItem().is(Items.GLASS_BOTTLE),
				"Without build rights nothing is drawn");

		witch.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.GLASS_BOTTLE, 4));
		for (int drawn = 1; drawn <= BubblingCauldronBlock.DOSES; drawn++) {
			use(helper, witch, pot);
			helper.assertTrue(carried(witch, item("shrinking_draught")) == drawn, "Each bottle draws one draught");
			if (drawn < BubblingCauldronBlock.DOSES) {
				helper.assertTrue(contents(helper, pot) == Brew.SHRINKING && doses(helper, pot) == BubblingCauldronBlock.DOSES - drawn,
						"A dose fewer is left");
			}
		}
		helper.assertTrue(contents(helper, pot) == Brew.EMPTY && helper.getBlockState(pot).getLightEmission() == 0, "The last dose empties the pot");
		helper.assertTrue(carried(witch, Items.GLASS_BOTTLE) == 1, "Three bottles went in");
		use(helper, witch, pot);
		helper.assertTrue(carried(witch, item("shrinking_draught")) == BubblingCauldronBlock.DOSES, "An empty pot fills no more");

		BlockPos cold = new BlockPos(5, 2, 5);
		helper.setBlock(cold, block("bubbling_cauldron"));
		witch.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WATER_BUCKET));
		use(helper, witch, cold);
		witch.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.SPIDER_EYE));
		use(helper, witch, cold);
		witch.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BROWN_MUSHROOM));
		use(helper, witch, cold);
		helper.assertTrue(contents(helper, cold) == Brew.GREEN, "A cold brew does not take a hex");

		helper.assertTrue(BubblingCauldronBlock.hexFor(Brew.ORANGE, new ItemStack(item("beans"))) == Brew.GIANT, "Beans make the orange brew a giant's");
		helper.assertTrue(BubblingCauldronBlock.hexFor(Brew.PURPLE, new ItemStack(Items.PHANTOM_MEMBRANE)) == Brew.FLYING,
				"A phantom membrane makes the purple brew flying ointment");
		helper.assertTrue(BubblingCauldronBlock.hexFor(Brew.GREEN, new ItemStack(item("beans"))) == null
				&& BubblingCauldronBlock.hexFor(Brew.WATER, new ItemStack(Items.BROWN_MUSHROOM)) == null, "Hex ingredients need their own brew");
		helper.succeed();
	}

	/**
	 * The Shrinking Draught makes you half size and earns Drink Me, leaving a glass bottle; under a low ceiling the
	 * Giant's Draught is refused and not used up; a shrunk player about to grow back with no room stays small; in the
	 * open the Giant's Draught cancels the shrinking, makes you 1.6 times your size, reaching a block further, and earns
	 * Fee-Fi-Fo-Fum; the Shrinking Draught cancels it again; Flying Ointment gives slow falling.
	 */
	@GameTest
	public void draughtsChangeYourSize(GameTestHelper helper) {
		floor(helper);
		ServerLevel level = helper.getLevel();
		BlockPos stand = new BlockPos(2, 2, 2);
		ServerPlayer drinker = player(helper, stand, ItemStack.EMPTY);
		ItemStack left = drink(drinker, "shrinking_draught");
		helper.assertTrue(drinker.hasEffect(Hexes.SHRUNK) && Math.abs(scale(drinker) - (1.0 - Hexes.SHRUNK_SCALE)) < EPSILON,
				"The Shrinking Draught halves you");
		helper.assertTrue(left.is(Items.GLASS_BOTTLE), "It leaves a glass bottle");
		helper.assertTrue(earned(drinker, "drink_me"), "Drink Me is earned");

		// Room at normal height, not at a giant's.
		helper.setBlock(stand.above(2), Blocks.STONE);
		helper.assertTrue(Hexes.roomFor(drinker, 1.0) && !Hexes.roomToGrow(drinker), "Room to stand, not to grow");
		drinker.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item("giants_draught")));
		helper.assertTrue(drinker.getMainHandItem().use(level, drinker, InteractionHand.MAIN_HAND) == InteractionResult.FAIL,
				"The Giant's Draught is refused under a low ceiling");
		ItemStack kept = drinker.getMainHandItem().finishUsingItem(level, drinker);
		helper.assertTrue(kept.is(item("giants_draught")) && kept.getCount() == 1 && !drinker.hasEffect(Hexes.GIANT) && drinker.hasEffect(Hexes.SHRUNK),
				"and not used up even if drunk to the end");

		// No room at all to grow back: about to end, the shrinking lasts a little longer.
		helper.setBlock(stand.above(), Blocks.STONE);
		drinker.removeEffect(Hexes.SHRUNK);
		drinker.addEffect(new MobEffectInstance(Hexes.SHRUNK, 2));
		Hexes.keepSmallWithoutRoom(drinker);
		helper.assertTrue(drinker.getEffect(Hexes.SHRUNK) != null && drinker.getEffect(Hexes.SHRUNK).getDuration() > 2,
				"A shrunk player with no room to grow stays small");
		helper.setBlock(stand.above(), Blocks.AIR);
		helper.setBlock(stand.above(2), Blocks.AIR);

		drink(drinker, "giants_draught");
		helper.assertTrue(drinker.hasEffect(Hexes.GIANT) && !drinker.hasEffect(Hexes.SHRUNK), "The Giant's Draught cancels the shrinking");
		helper.assertTrue(Math.abs(scale(drinker) - (1.0 + Hexes.GIANT_SCALE)) < EPSILON, "and makes you 1.6 times your size");
		helper.assertTrue(Math.abs(drinker.getAttributeValue(Attributes.BLOCK_INTERACTION_RANGE)
				- drinker.getAttributeBaseValue(Attributes.BLOCK_INTERACTION_RANGE) - Hexes.GIANT_REACH) < EPSILON, "reaching a block further");
		helper.assertTrue(earned(drinker, "fee_fi_fo_fum"), "Fee-Fi-Fo-Fum is earned");

		drink(drinker, "shrinking_draught");
		helper.assertTrue(drinker.hasEffect(Hexes.SHRUNK) && !drinker.hasEffect(Hexes.GIANT)
				&& Math.abs(scale(drinker) - (1.0 - Hexes.SHRUNK_SCALE)) < EPSILON, "The Shrinking Draught cancels the giant's");

		drink(drinker, "flying_ointment");
		helper.assertTrue(drinker.hasEffect(MobEffects.SLOW_FALLING), "Flying Ointment gives slow falling");
		helper.succeed();
	}

	/** The hex tags, effects, draughts and advancements load. */
	@GameTest
	public void hexDataLoads(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		helper.assertTrue(new ItemStack(Items.BROWN_MUSHROOM).is(Brew.SHRINKING.hexIngredients()), "Brown mushrooms are a shrinking ingredient");
		helper.assertTrue(new ItemStack(item("beans")).is(Brew.GIANT.hexIngredients()), "Beans are a giant's ingredient");
		helper.assertTrue(new ItemStack(Items.PHANTOM_MEMBRANE).is(Brew.FLYING.hexIngredients()), "Phantom membranes are a flying ingredient");
		helper.assertTrue(BuiltInRegistries.MOB_EFFECT.containsKey(Jugcraft.id("shrunk")) && BuiltInRegistries.MOB_EFFECT.containsKey(Jugcraft.id("giant")),
				"The Shrunk and Giant effects are registered");
		for (Brew hex : new Brew[] {Brew.SHRINKING, Brew.GIANT, Brew.FLYING}) {
			helper.assertTrue(Hexes.draught(hex) != Items.AIR, Hexes.draughtId(hex) + " is registered");
		}
		for (String id : new String[] {"drink_me", "fee_fi_fo_fum"}) {
			helper.assertTrue(level.getServer().getAdvancements().get(Jugcraft.id(id)) != null, id + " loads");
		}
		helper.succeed();
	}
}
