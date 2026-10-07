package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.agriculture.BatJarBlock;
import io.github.jimbozoomer.jugcraft.agriculture.BeatingHeartJarBlock;
import io.github.jimbozoomer.jugcraft.agriculture.BroomRackBlock;
import io.github.jimbozoomer.jugcraft.agriculture.Candelabra;
import io.github.jimbozoomer.jugcraft.agriculture.CuriosityCabinetBlock;
import io.github.jimbozoomer.jugcraft.agriculture.DecorationBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.DustpanBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.EnchantedBroomBlock;
import io.github.jimbozoomer.jugcraft.agriculture.EnchantedBroomBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.GiantBeatingHeartBlock;
import io.github.jimbozoomer.jugcraft.agriculture.HandJarBlock;
import io.github.jimbozoomer.jugcraft.agriculture.HornedSkullCauldronBlock;
import io.github.jimbozoomer.jugcraft.agriculture.HornedSkullCauldronBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.JugcraftAgriculture;
import io.github.jimbozoomer.jugcraft.agriculture.MothCaseBlock;
import io.github.jimbozoomer.jugcraft.agriculture.ShowcaseBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.SpecimenJarBlock;
import io.github.jimbozoomer.jugcraft.agriculture.SpecimenTankBlock;
import io.github.jimbozoomer.jugcraft.agriculture.SpecimenVesselBlock;
import io.github.jimbozoomer.jugcraft.agriculture.TallSpecimenJarBlock;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * In-game tests for Halloween decorations batch 17, the Witch's Workshop: the Horned Skull Cauldron holds potions without
 * gain or loss, heats over an Ember Bed, wafts a lasting potion onto those near and floats ingredients; the candelabra
 * take waxes and flames, light by flint and steel and by redstone, drip and are scraped, and hang or stand as they
 * should; the Enchanted Broom sweeps dropped items into a Dustpan only while anointed; the Curiosity Cabinet, Bell Jar and
 * Broom Rack show what they are given; the Moth Display Case changes its moths; the Oddity Jars beat, wake and point; the
 * Giant's Beating Heart stands whole and beats; the Tall Specimen Jar and Specimen Tank stand whole and keep their
 * specimens; and the data loads.
 */
public class WitchsWorkshopGameTests {
	private static Block block(String id) {
		return JugcraftAgriculture.block(id);
	}

	private static Item item(String id) {
		return JugcraftAgriculture.item(id);
	}

	private static ServerPlayer player(GameTestHelper helper, BlockPos standAt, ItemStack held) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setGameMode(GameType.SURVIVAL);
		BlockPos absolute = helper.absolutePos(standAt);
		player.setPos(absolute.getX() + 0.5, absolute.getY(), absolute.getZ() + 0.5);
		player.setItemInHand(InteractionHand.MAIN_HAND, held);
		return player;
	}

	private static InteractionResult use(GameTestHelper helper, ServerPlayer player, BlockPos on, Direction face) {
		BlockPos absolute = helper.absolutePos(on);
		BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(absolute).relative(face, 0.5), face, absolute, false);
		return player.gameMode.useItemOn(player, helper.getLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND, hit);
	}

	private static InteractionResult useAt(GameTestHelper helper, ServerPlayer player, BlockPos on, Vec3 at, Direction face) {
		BlockHitResult hit = new BlockHitResult(at, face, helper.absolutePos(on), false);
		return player.gameMode.useItemOn(player, helper.getLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND, hit);
	}

	/** Places an item's block from below {@code at} with the player turned to {@code yaw} (0 faces south, so the prop faces north). */
	private static void place(GameTestHelper helper, ServerPlayer player, String id, BlockPos at, float yaw) {
		player.setYRot(yaw);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item(id)));
		BlockPos below = helper.absolutePos(at.below());
		BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(below).relative(Direction.UP, 0.5), Direction.UP, below, false);
		player.getMainHandItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit));
	}

	private static List<ItemEntity> drops(GameTestHelper helper, Item item) {
		AABB area = new AABB(helper.absolutePos(BlockPos.ZERO)).expandTowards(8, 8, 8).inflate(2.0);
		return helper.getLevel().getEntitiesOfClass(ItemEntity.class, area, entity -> entity.getItem().is(item));
	}

	private static void floor(GameTestHelper helper) {
		for (int x = 0; x < 9; x++) {
			for (int z = 0; z < 9; z++) {
				helper.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
			}
		}
	}

	private static ItemStack potion(net.minecraft.core.Holder<net.minecraft.world.item.alchemy.Potion> potion) {
		return PotionContents.createItemStack(Items.POTION, potion);
	}

	/** It holds water or a potion, three deep, and gives back what went in; a different potion is refused. */
	@GameTest
	public void cauldronHoldsPotionsWithoutGain(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		floor(helper);
		BlockPos at = new BlockPos(4, 2, 4);
		helper.setBlock(at, block("horned_skull_cauldron"));
		HornedSkullCauldronBlockEntity pot = (HornedSkullCauldronBlockEntity) level.getBlockEntity(helper.absolutePos(at));
		ServerPlayer witch = player(helper, new BlockPos(4, 2, 1), new ItemStack(Items.WATER_BUCKET));
		use(helper, witch, at, Direction.UP);
		helper.assertTrue(helper.getBlockState(at).getValue(HornedSkullCauldronBlock.LEVEL) == 3 && pot.isWater()
				&& witch.getMainHandItem().is(Items.BUCKET), "A water bucket fills it three deep and comes back empty");
		helper.assertTrue(helper.getBlockState(at).getLightEmission() == 0, "Water gives no light");
		use(helper, witch, at, Direction.UP);
		helper.assertTrue(helper.getBlockState(at).getValue(HornedSkullCauldronBlock.LEVEL) == 0 && witch.getMainHandItem().is(Items.WATER_BUCKET),
				"A bucket takes the full pot of water back");
		for (int i = 0; i < 3; i++) {
			witch.setItemInHand(InteractionHand.MAIN_HAND, potion(Potions.SWIFTNESS));
			use(helper, witch, at, Direction.UP);
			helper.assertTrue(witch.getMainHandItem().is(Items.GLASS_BOTTLE), "Each potion pours in, giving its bottle back");
		}
		BlockState full = helper.getBlockState(at);
		helper.assertTrue(full.getValue(HornedSkullCauldronBlock.LEVEL) == 3 && full.getValue(HornedSkullCauldronBlock.POTION)
				&& full.getLightEmission() == HornedSkullCauldronBlock.POTION_LIGHT + 3, "Three bottles fill it and the potion glows");
		witch.setItemInHand(InteractionHand.MAIN_HAND, potion(Potions.SWIFTNESS));
		use(helper, witch, at, Direction.UP);
		helper.assertTrue(witch.getMainHandItem().is(Items.POTION), "A fourth doesn't go in");
		helper.setBlock(at, Blocks.AIR);
		helper.setBlock(at, block("horned_skull_cauldron"));
		pot = (HornedSkullCauldronBlockEntity) level.getBlockEntity(helper.absolutePos(at));
		witch.setItemInHand(InteractionHand.MAIN_HAND, potion(Potions.SWIFTNESS));
		use(helper, witch, at, Direction.UP);
		witch.setItemInHand(InteractionHand.MAIN_HAND, potion(Potions.HEALING));
		use(helper, witch, at, Direction.UP);
		helper.assertTrue(witch.getMainHandItem().is(Items.POTION) && helper.getBlockState(at).getValue(HornedSkullCauldronBlock.LEVEL) == 1,
				"Another potion is refused");
		witch.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.GLASS_BOTTLE));
		use(helper, witch, at, Direction.UP);
		PotionContents drawn = witch.getMainHandItem().get(DataComponents.POTION_CONTENTS);
		helper.assertTrue(witch.getMainHandItem().is(Items.POTION) && drawn != null && drawn.is(Potions.SWIFTNESS),
				"A glass bottle draws the same potion back out");
		helper.assertTrue(helper.getBlockState(at).getValue(HornedSkullCauldronBlock.LEVEL) == 0 && pot.contents().equals(PotionContents.EMPTY),
				"and the pot is empty again");
		helper.succeed();
	}

	/** Over an Ember Bed it heats; stirred with the ladle, a lasting potion wafts onto at most four near, at a quarter. */
	@GameTest
	public void cauldronWaftsOverHeat(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		floor(helper);
		BlockPos bed = new BlockPos(4, 2, 4);
		BlockPos at = bed.above();
		helper.setBlock(bed, block("ember_bed"));
		helper.assertTrue(helper.getBlockState(bed).getLightEmission() == 9, "The ember bed glows");
		helper.setBlock(at, block("horned_skull_cauldron").defaultBlockState().setValue(HornedSkullCauldronBlock.HEATED,
				HornedSkullCauldronBlock.heats(helper.getBlockState(bed))));
		helper.assertTrue(helper.getBlockState(at).getValue(HornedSkullCauldronBlock.HEATED), "The ember bed heats the pot over it");
		ServerPlayer witch = player(helper, new BlockPos(4, 3, 2), potion(Potions.LONG_SWIFTNESS));
		use(helper, witch, at, Direction.UP);
		witch.setItemInHand(InteractionHand.MAIN_HAND, potion(Potions.LONG_SWIFTNESS));
		use(helper, witch, at, Direction.UP);
		List<ServerPlayer> guests = List.of(player(helper, new BlockPos(5, 3, 3), ItemStack.EMPTY), player(helper, new BlockPos(3, 3, 3), ItemStack.EMPTY),
				player(helper, new BlockPos(5, 3, 5), ItemStack.EMPTY), player(helper, new BlockPos(3, 3, 5), ItemStack.EMPTY));
		witch.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item("brew_ladle")));
		use(helper, witch, at, Direction.UP);
		helper.assertTrue(helper.getBlockState(at).getValue(HornedSkullCauldronBlock.LEVEL) == 1, "Wafting uses one level");
		int dosed = 0;
		for (ServerPlayer player : List.of(witch, guests.get(0), guests.get(1), guests.get(2), guests.get(3))) {
			MobEffectInstance effect = player.getEffect(MobEffects.SPEED);
			if (effect != null) {
				dosed++;
				helper.assertTrue(effect.getDuration() <= 9600 / 4 && effect.getDuration() > 9600 / 4 - 40,
						"Each gets a quarter of the potion's eight minutes: " + effect.getDuration());
			}
		}
		helper.assertTrue(dosed == HornedSkullCauldronBlock.WAFT_PLAYERS, "At most four players are wafted on: " + dosed);
		helper.assertTrue(HornedSkullCauldronBlock.wafted(new PotionContents(Potions.HEALING)).isEmpty(), "An instant potion doesn't waft");
		helper.succeed();
	}

	/** Things that float go in (three at most), tools don't; an empty hand fishes the last out; breaking gives them back. */
	@GameTest
	public void cauldronFloatsIngredients(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		floor(helper);
		BlockPos at = new BlockPos(4, 2, 4);
		helper.setBlock(at, block("horned_skull_cauldron"));
		HornedSkullCauldronBlockEntity pot = (HornedSkullCauldronBlockEntity) level.getBlockEntity(helper.absolutePos(at));
		ServerPlayer witch = player(helper, new BlockPos(4, 2, 1), new ItemStack(Items.APPLE, 5));
		use(helper, witch, at, Direction.UP);
		helper.assertTrue(pot.floating().isEmpty(), "Nothing floats in an empty pot");
		witch.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WATER_BUCKET));
		use(helper, witch, at, Direction.UP);
		witch.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.APPLE, 5));
		for (int i = 0; i < 4; i++) {
			use(helper, witch, at, Direction.UP);
		}
		helper.assertTrue(pot.floating().size() == HornedSkullCauldronBlock.FLOATERS && witch.getMainHandItem().getCount() == 2,
				"Three apples float, and no more");
		witch.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SWORD));
		use(helper, witch, at, Direction.UP);
		helper.assertTrue(witch.getMainHandItem().is(Items.IRON_SWORD), "A sword doesn't go in");
		witch.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
		use(helper, witch, at, Direction.UP);
		helper.assertTrue(pot.floating().size() == 2, "An empty hand fishes one out");
		helper.destroyBlock(at);
		long apples = level.getEntitiesOfClass(ItemEntity.class, new net.minecraft.world.phys.AABB(helper.absolutePos(at)).inflate(2))
				.stream().filter(e -> e.getItem().is(Items.APPLE)).mapToInt(e -> e.getItem().getCount()).sum();
		helper.assertTrue(apples >= 2, "Breaking it gives back what floats in it: " + apples);
		helper.succeed();
	}

	/** Candelabra take a wax and a flame, light and snuff, follow redstone, drip and are scraped; tall and hanging ones hold. */
	@GameTest
	public void candelabraLightAndColour(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		floor(helper);
		BlockPos at = new BlockPos(2, 2, 4);
		ServerPlayer chandler = player(helper, new BlockPos(2, 2, 1), new ItemStack(item("table_candelabrum")));
		use(helper, chandler, new BlockPos(2, 1, 4), Direction.UP);
		helper.assertTrue(helper.getBlockState(at).is(block("table_candelabrum")), "A table candelabrum stands on a block");
		chandler.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.FLINT_AND_STEEL));
		use(helper, chandler, at, Direction.NORTH);
		helper.assertTrue(helper.getBlockState(at).getValue(Candelabra.LIT) && helper.getBlockState(at).getLightEmission() == 9,
				"Flint and steel lights it (light 9)");
		chandler.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath("minecraft", "purple_dye"))));
		use(helper, chandler, at, Direction.NORTH);
		chandler.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.AMETHYST_SHARD));
		use(helper, chandler, at, Direction.NORTH);
		BlockState lit = helper.getBlockState(at);
		helper.assertTrue(lit.getValue(Candelabra.WAX) == Candelabra.Wax.PURPLE && lit.getValue(Candelabra.FLAME) == Candelabra.Flame.WITCHFIRE
				&& chandler.getMainHandItem().isEmpty(), "A dye turns the wax purple, an amethyst shard the flames to witchfire");
		chandler.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
		use(helper, chandler, at, Direction.NORTH);
		helper.assertTrue(!helper.getBlockState(at).getValue(Candelabra.LIT), "An empty hand snuffs it");
		helper.setBlock(at.east(), Blocks.REDSTONE_BLOCK);
		helper.assertTrue(helper.getBlockState(at).getValue(Candelabra.LIT), "A redstone signal lights it");
		helper.setBlock(at.east(), Blocks.AIR);
		helper.assertTrue(!helper.getBlockState(at).getValue(Candelabra.LIT), "and its going snuffs it");
		BlockState dripping = Candelabra.drip(helper.getBlockState(at).setValue(Candelabra.LIT, true), RandomSource.create(1L));
		int tries = 0;
		BlockState state = helper.getBlockState(at).setValue(Candelabra.LIT, true);
		RandomSource random = RandomSource.create(7L);
		while (state.getValue(Candelabra.DRIPS) < Candelabra.DRIP_STAGES && tries < 2000) {
			BlockState next = Candelabra.drip(state, random);
			state = next == null ? state : next;
			tries++;
		}
		helper.assertTrue(state.getValue(Candelabra.DRIPS) == Candelabra.DRIP_STAGES && Candelabra.drip(state, random) == null,
				"Lit, its drips grow to the last stage and stop: " + tries + (dripping == null ? "" : ""));
		helper.assertTrue(Candelabra.drip(state.setValue(Candelabra.LIT, false).setValue(Candelabra.DRIPS, 0), RandomSource.create(2L)) == null
				&& !Candelabra.drips(state.setValue(Candelabra.LIT, false)), "Unlit, nothing drips");
		helper.setBlock(at, state);
		chandler.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.SHEARS));
		use(helper, chandler, at, Direction.NORTH);
		helper.assertTrue(helper.getBlockState(at).getValue(Candelabra.DRIPS) == 0, "Shears scrape the drips off");
		helper.assertTrue(level.getEntitiesOfClass(ItemEntity.class, new net.minecraft.world.phys.AABB(helper.absolutePos(at)).inflate(2)).isEmpty(),
				"and give nothing");

		// The floor candelabrum is two blocks tall, both halves lit as one, light 14.
		chandler.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item("floor_candelabrum")));
		use(helper, chandler, new BlockPos(5, 1, 4), Direction.UP);
		BlockPos foot = new BlockPos(5, 2, 4);
		helper.assertTrue(helper.getBlockState(foot).is(block("floor_candelabrum")) && helper.getBlockState(foot.above()).is(block("floor_candelabrum"))
				&& helper.getBlockState(foot.above()).getValue(CuriosityCabinetBlock.HALF) == DoubleBlockHalf.UPPER, "A floor candelabrum stands two tall");
		chandler.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.FLINT_AND_STEEL));
		use(helper, chandler, foot.above(), Direction.NORTH);
		helper.assertTrue(helper.getBlockState(foot).getValue(Candelabra.LIT) && helper.getBlockState(foot.above()).getValue(Candelabra.LIT)
				&& helper.getBlockState(foot).getLightEmission() == 14, "Lit from its upper half, both halves burn at light 14");

		// The chandelier hangs under a block and falls without it.
		helper.setBlock(new BlockPos(7, 5, 4), Blocks.STONE);
		helper.setBlock(new BlockPos(7, 4, 4), block("branching_chandelier"));
		helper.assertTrue(helper.getBlockState(new BlockPos(7, 4, 4)).is(block("branching_chandelier")), "A chandelier hangs under a block");
		helper.setBlock(new BlockPos(7, 5, 4), Blocks.AIR);
		helper.assertTrue(helper.getBlockState(new BlockPos(7, 4, 4)).isAir(), "and comes down when that block goes");

		helper.assertTrue(Candelabra.candles("floor_candelabrum").length == 7 && Candelabra.candles("table_candelabrum").length == 3
				&& Candelabra.candles("wall_girandole").length == 3 && Candelabra.candles("branching_chandelier").length == 16,
				"Each fitting has its candles: 7, 3, 3 and 16");
		helper.succeed();
	}

	/** Anointed, the broom sweeps dropped items near a Dustpan into it; asleep, it does nothing. */
	@GameTest
	public void broomSweepsIntoDustpan(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		floor(helper);
		BlockPos at = new BlockPos(4, 2, 4);
		BlockPos panPos = new BlockPos(6, 2, 4);
		helper.setBlock(at, block("enchanted_broom"));
		helper.setBlock(panPos, block("dustpan"));
		EnchantedBroomBlockEntity broom = (EnchantedBroomBlockEntity) level.getBlockEntity(helper.absolutePos(at));
		DustpanBlockEntity pan = (DustpanBlockEntity) level.getBlockEntity(helper.absolutePos(panPos));
		Vec3 near = Vec3.atBottomCenterOf(helper.absolutePos(panPos)).add(0.3, 0.1, 0.2);
		ItemEntity apples = new ItemEntity(level, near.x, near.y, near.z, new ItemStack(Items.APPLE, 5));
		apples.setDeltaMovement(Vec3.ZERO);
		level.addFreshEntity(apples);
		helper.assertTrue(!helper.getBlockState(at).getValue(EnchantedBroomBlock.CHARGED) && broom.charge() == 0, "It starts asleep");
		ServerPlayer witch = player(helper, new BlockPos(4, 2, 1), new ItemStack(item("flying_ointment")));
		use(helper, witch, at, Direction.NORTH);
		helper.assertTrue(helper.getBlockState(at).getValue(EnchantedBroomBlock.CHARGED) && broom.charge() == EnchantedBroomBlock.CHARGE_TICKS
				&& witch.getMainHandItem().is(Items.GLASS_BOTTLE), "Flying Ointment wakes it for three days, and the bottle comes back");
		witch.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item("flying_ointment")));
		use(helper, witch, at, Direction.NORTH);
		helper.assertTrue(witch.getMainHandItem().is(item("flying_ointment")), "It won't take more while still nearly full");
		helper.assertTrue(broom.sweep(level, helper.absolutePos(at)), "It sweeps");
		int caught = 0;
		for (int slot = 0; slot < pan.getContainerSize(); slot++) {
			caught += pan.getItem(slot).is(Items.APPLE) ? pan.getItem(slot).getCount() : 0;
		}
		helper.assertTrue(caught == 5 && !apples.isAlive(), "The apples by the pan go into it: " + caught);
		ItemEntity far = new ItemEntity(level, helper.absolutePos(at).getX() - 2.5, helper.absolutePos(at).getY() + 0.1,
				helper.absolutePos(at).getZ() + 0.5, new ItemStack(Items.BONE));
		far.setDeltaMovement(Vec3.ZERO);
		level.addFreshEntity(far);
		broom.sweep(level, helper.absolutePos(at));
		helper.assertTrue(far.getDeltaMovement().x > 0.1, "One further off is pushed toward the pan: " + far.getDeltaMovement());
		far.discard();
		helper.succeed();
	}

	/** The cabinet takes one thing to a place, chosen by where it is used; the bell jar one; the rack only brooms. */
	@GameTest
	public void displaysShowWhatTheyAreGiven(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		floor(helper);
		BlockPos at = new BlockPos(4, 2, 4);
		ServerPlayer collector = player(helper, new BlockPos(4, 2, 1), new ItemStack(item("curiosity_cabinet")));
		use(helper, collector, new BlockPos(4, 1, 4), Direction.UP);
		BlockState cabinet = helper.getBlockState(at);
		helper.assertTrue(cabinet.is(block("curiosity_cabinet")) && cabinet.getValue(CuriosityCabinetBlock.FACING) == Direction.NORTH,
				"The cabinet faces its placer: " + cabinet);
		BlockPos lower = helper.absolutePos(at);
		ShowcaseBlockEntity shelves = (ShowcaseBlockEntity) level.getBlockEntity(lower);
		// Each place's middle on the front (north) face: x across, y up the 32 pixels.
		// Doubles: the test's blocks are millions of blocks out, where a float can't hold a sixteenth.
		double[] xs = {4.5, 8.0, 11.5};
		double[] ys = {6.0, 16.0, 26.0};
		for (int row = 0; row < 3; row++) {
			for (int column = 0; column < 3; column++) {
				Vec3 hit = new Vec3(lower.getX() + xs[column] / 16, lower.getY() + ys[row] / 16, lower.getZ() + 2.0 / 16);
				BlockPos on = ys[row] < 16 ? at : at.above();
				helper.assertTrue(CuriosityCabinetBlock.place(helper.getBlockState(on), lower, new BlockHitResult(hit, Direction.NORTH,
						helper.absolutePos(on), false)) == row * 3 + column, "A hit on place " + (row * 3 + column) + " picks it");
			}
		}
		collector.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.SKELETON_SKULL, 2));
		useAt(helper, collector, at.above(), new Vec3(lower.getX() + 8.0 / 16, lower.getY() + 26.0 / 16, lower.getZ() + 2.0 / 16), Direction.NORTH);
		helper.assertTrue(shelves.get(7).is(Items.SKELETON_SKULL) && collector.getMainHandItem().getCount() == 1,
				"Used on the top middle place, one skull goes there");
		BlockPos absolute = helper.absolutePos(at);
		helper.assertTrue(level.getBlockState(absolute).getAnalogOutputSignal(level, absolute, Direction.NORTH) == 2, "A comparator reads it");
		collector.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
		useAt(helper, collector, at.above(), new Vec3(lower.getX() + 8.0 / 16, lower.getY() + 26.0 / 16, lower.getZ() + 2.0 / 16), Direction.NORTH);
		helper.assertTrue(shelves.get(7).isEmpty() && collector.getMainHandItem().is(Items.SKELETON_SKULL), "and an empty hand takes it back");

		helper.setBlock(new BlockPos(1, 2, 6), block("bell_jar"));
		collector.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BEETROOT));
		use(helper, collector, new BlockPos(1, 2, 6), Direction.UP);
		ShowcaseBlockEntity jar = (ShowcaseBlockEntity) level.getBlockEntity(helper.absolutePos(new BlockPos(1, 2, 6)));
		helper.assertTrue(jar.get(0).is(Items.BEETROOT), "The bell jar shows one thing");

		helper.setBlock(new BlockPos(7, 3, 7), Blocks.STONE);
		helper.setBlock(new BlockPos(7, 3, 6), block("broom_rack").defaultBlockState().setValue(BroomRackBlock.FACING, Direction.NORTH));
		ShowcaseBlockEntity rack = (ShowcaseBlockEntity) level.getBlockEntity(helper.absolutePos(new BlockPos(7, 3, 6)));
		BlockPos rackPos = helper.absolutePos(new BlockPos(7, 3, 6));
		collector.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.APPLE));
		useAt(helper, collector, new BlockPos(7, 3, 6), new Vec3(rackPos.getX() + 3.5 / 16, rackPos.getY() + 0.7, rackPos.getZ() + 0.6), Direction.NORTH);
		helper.assertTrue(rack.count() == 0, "The rack takes no apple");
		collector.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item("witchs_broom")));
		useAt(helper, collector, new BlockPos(7, 3, 6), new Vec3(rackPos.getX() + 12.5 / 16, rackPos.getY() + 0.7, rackPos.getZ() + 0.6),
				Direction.NORTH);
		helper.assertTrue(rack.count() == 1 && !rack.get(2).isEmpty() && BroomRackBlock.peg(helper.getBlockState(new BlockPos(7, 3, 6)), rackPos,
				new Vec3(rackPos.getX() + 3.5 / 16, rackPos.getY() + 0.7, rackPos.getZ() + 0.6)) == 0, "It hangs a broom on the peg it is used at");
		helper.setBlock(new BlockPos(7, 3, 7), Blocks.AIR);
		helper.assertTrue(helper.getBlockState(new BlockPos(7, 3, 6)).isAir(), "It needs its wall");
		helper.succeed();
	}

	/** The moth case changes its moths; the heart beats a pulse and stops while powered from below; the bat wakes; the hand points. */
	@GameTest
	public void odditiesStir(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		floor(helper);
		helper.setBlock(new BlockPos(1, 3, 8), Blocks.STONE);
		helper.setBlock(new BlockPos(1, 3, 7), block("moth_display_case").defaultBlockState().setValue(MothCaseBlock.FACING, Direction.NORTH));
		ServerPlayer keeper = player(helper, new BlockPos(1, 2, 5), ItemStack.EMPTY);
		use(helper, keeper, new BlockPos(1, 3, 7), Direction.NORTH);
		helper.assertTrue(helper.getBlockState(new BlockPos(1, 3, 7)).getValue(MothCaseBlock.MOTH) == MothCaseBlock.Moth.DEATHS_HEAD,
				"Using the case changes its moths");

		BlockPos heart = new BlockPos(4, 2, 4);
		helper.setBlock(heart, block("beating_heart_jar"));
		BlockPos absolute = helper.absolutePos(heart);
		BlockState resting = helper.getBlockState(heart);
		resting.tick(level, absolute, level.getRandom());
		BlockState beating = helper.getBlockState(heart);
		helper.assertTrue(beating.getValue(BeatingHeartJarBlock.BEAT) && beating.getSignal(level, absolute, Direction.NORTH) == 15
				&& beating.getSignal(level, absolute, Direction.UP) == 0, "A beat gives 15 to the sides but not below");
		beating.tick(level, absolute, level.getRandom());
		helper.assertTrue(!helper.getBlockState(heart).getValue(BeatingHeartJarBlock.BEAT), "and stops after its pulse");
		helper.assertTrue(BeatingHeartJarBlock.period(resting) == 20, "At 60 a minute it beats every 20 ticks");
		use(helper, keeper, heart, Direction.NORTH);
		helper.assertTrue(helper.getBlockState(heart).getValue(BeatingHeartJarBlock.TEMPO) == 1
				&& BeatingHeartJarBlock.period(helper.getBlockState(heart)) == 15, "Using it changes its tempo to 80");
		helper.setBlock(heart.below(), Blocks.REDSTONE_BLOCK);
		helper.getBlockState(heart).setValue(BeatingHeartJarBlock.BEAT, false).tick(level, absolute, level.getRandom());
		helper.assertTrue(!helper.getBlockState(heart).getValue(BeatingHeartJarBlock.BEAT), "Powered from below, it doesn't beat");

		BlockPos bat = new BlockPos(7, 2, 4);
		helper.setBlock(bat, block("bat_in_a_jar"));
		player(helper, new BlockPos(7, 2, 2), ItemStack.EMPTY);
		helper.getBlockState(bat).tick(level, helper.absolutePos(bat), level.getRandom());
		helper.assertTrue(helper.getBlockState(bat).getValue(BatJarBlock.AWAKE), "A player coming near wakes the bat");

		BlockPos hand = new BlockPos(2, 2, 2);
		helper.setBlock(hand, block("hand_in_a_jar"));
		helper.setBlock(hand.west(), Blocks.REDSTONE_BLOCK);
		helper.assertTrue(helper.getBlockState(hand).getValue(HandJarBlock.POWERED), "Powered, the hand points");
		helper.succeed();
	}

	/**
	 * The Giant's Beating Heart stands 3 x 3 x 3, every block part of it and its block entity on the first; with a block in
	 * the way it is not placed. Each block's shape is its piece of the Beating Heart Jar's box made three times bigger. Its
	 * first block beats (15 to the sides, none below, then off after its pulse), use changes its tempo, and a signal from
	 * below any of its bottom blocks stops it. Broken anywhere, it goes whole and drops once.
	 */
	@GameTest(maxTicks = 40)
	public void giantHeartStandsAndBeats(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		floor(helper);
		GiantBeatingHeartBlock heart = (GiantBeatingHeartBlock) block("giant_beating_heart");
		BlockPos master = new BlockPos(6, 2, 2);
		BlockPos absolute = helper.absolutePos(master);
		ServerPlayer builder = player(helper, new BlockPos(1, 2, 8), ItemStack.EMPTY);
		BlockPos blocker = heart.partPos(absolute, Direction.NORTH, heart.cells().length - 1);
		level.setBlock(blocker, Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
		place(helper, builder, "giant_beating_heart", master, 0.0F);
		helper.assertBlockNotPresent(heart, master);
		level.setBlock(blocker, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
		place(helper, builder, "giant_beating_heart", master, 0.0F);
		helper.assertTrue(heart.cells().length == 27, "27 blocks");
		for (int part = 0; part < heart.cells().length; part++) {
			BlockState state = level.getBlockState(heart.partPos(absolute, Direction.NORTH, part));
			helper.assertTrue(state.is(heart) && heart.part(state) == part && state.getValue(GiantBeatingHeartBlock.FACING) == Direction.NORTH,
					"Part " + part + " of the jar is in place");
		}
		helper.assertTrue(level.getBlockEntity(absolute) instanceof DecorationBlockEntity
				&& level.getBlockEntity(heart.partPos(absolute, Direction.NORTH, GiantBeatingHeartBlock.MIDDLE)) == null,
				"Its block entity is on its first block only");
		// The jar's box, three times the Beating Heart Jar's (3.9 to 12.1 across and deep, 13.6 tall): 11.7 to 36.3 and
		// 40.8 pixels. Its first block, the front corner on the placer's left, holds 4.3 of it across and deep; the
		// bottom middle block is full; the top middle block 8.8 pixels of it.
		AABB first = level.getBlockState(absolute).getShape(level, absolute).bounds();
		// Parts count right, then up, then away: part 10 is one right and one away, part 16 the same two blocks up.
		BlockPos bottomMiddle = heart.partPos(absolute, Direction.NORTH, 10);
		BlockPos topMiddle = heart.partPos(absolute, Direction.NORTH, 16);
		AABB top = level.getBlockState(topMiddle).getShape(level, topMiddle).bounds();
		helper.assertTrue(Math.abs(first.maxX - 4.3 / 16) < 1e-6 && Math.abs(first.minZ - 11.7 / 16) < 1e-6 && first.minX == 0.0 && first.maxZ == 1.0
				&& level.getBlockState(bottomMiddle).getShape(level, bottomMiddle).bounds().equals(new AABB(0, 0, 0, 1, 1, 1))
				&& Math.abs(top.maxY - 8.8 / 16) < 1e-6 && top.minX == 0.0 && top.maxX == 1.0,
				"Its shape is the Beating Heart Jar's made three times bigger: first block " + first + ", top middle " + top);

		BlockState resting = level.getBlockState(absolute);
		helper.assertTrue(GiantBeatingHeartBlock.period(resting) == 30, "At 40 a minute it beats every 30 ticks");
		resting.tick(level, absolute, level.getRandom());
		BlockState beating = level.getBlockState(absolute);
		helper.assertTrue(beating.getValue(GiantBeatingHeartBlock.BEAT) && beating.getSignal(level, absolute, Direction.NORTH) == 15
				&& beating.getSignal(level, absolute, Direction.UP) == 0, "A beat gives 15 to the sides but not below");
		BlockPos middle = heart.partPos(absolute, Direction.NORTH, GiantBeatingHeartBlock.MIDDLE);
		helper.assertTrue(level.getBlockState(middle).getSignal(level, middle, Direction.NORTH) == 0, "Only its first block gives the signal");
		beating.tick(level, absolute, level.getRandom());
		helper.assertTrue(!level.getBlockState(absolute).getValue(GiantBeatingHeartBlock.BEAT), "and it stops after its pulse");
		builder.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
		use(helper, builder, new BlockPos(5, 3, 3), Direction.NORTH);
		helper.assertTrue(level.getBlockState(absolute).getValue(GiantBeatingHeartBlock.TEMPO) == 1
				&& GiantBeatingHeartBlock.period(level.getBlockState(absolute)) == 24, "Using any block of it changes its tempo to 50");
		BlockPos farCorner = heart.partPos(absolute, Direction.NORTH, 2);
		level.setBlock(farCorner.below(), Blocks.REDSTONE_BLOCK.defaultBlockState(), Block.UPDATE_ALL);
		level.getBlockState(absolute).setValue(GiantBeatingHeartBlock.BEAT, false).tick(level, absolute, level.getRandom());
		helper.assertTrue(!level.getBlockState(absolute).getValue(GiantBeatingHeartBlock.BEAT), "Powered from below its far corner, it doesn't beat");
		level.setBlock(farCorner.below(), Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);

		level.destroyBlock(heart.partPos(absolute, Direction.NORTH, 22), true);
		for (int part = 0; part < heart.cells().length; part++) {
			helper.assertFalse(level.getBlockState(heart.partPos(absolute, Direction.NORTH, part)).is(heart), "Broken anywhere, part " + part + " goes too");
		}
		helper.succeedWhen(() -> helper.assertTrue(drops(helper, item("giant_beating_heart")).stream().mapToInt(e -> e.getItem().getCount()).sum() == 1,
				"It drops once"));
	}

	/**
	 * The Giant's Beating Heart on the real tick scheduler, left alone after it is placed: it beats once a period (30 ticks
	 * at 40 a minute), and each beat's signal lasts {@value GiantBeatingHeartBlock#PULSE_TICKS} ticks. (Its own beat
	 * updates its parts, which must not push its next tick back to a whole period.)
	 */
	@GameTest(maxTicks = 160)
	public void giantHeartPulsesOnTheScheduler(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		floor(helper);
		GiantBeatingHeartBlock heart = (GiantBeatingHeartBlock) block("giant_beating_heart");
		BlockPos master = new BlockPos(6, 2, 2);
		BlockPos absolute = helper.absolutePos(master);
		ServerPlayer builder = player(helper, new BlockPos(1, 2, 8), ItemStack.EMPTY);
		place(helper, builder, "giant_beating_heart", master, 0.0F);
		helper.assertTrue(level.getBlockState(absolute).is(heart), "The heart stands");
		int period = GiantBeatingHeartBlock.period(level.getBlockState(absolute));
		// Each beat's first tick (game time) and how many ticks its signal stayed on, sampled once a tick.
		List<Long> onsets = new ArrayList<>();
		List<Integer> lengths = new ArrayList<>();
		helper.onEachTick(() -> {
			BlockState state = level.getBlockState(absolute);
			boolean on = state.is(heart) && state.getValue(GiantBeatingHeartBlock.BEAT)
					&& state.getSignal(level, absolute, Direction.NORTH) == 15;
			if (on && (lengths.isEmpty() || lengths.get(lengths.size() - 1) < 0)) {
				onsets.add(level.getGameTime());
				lengths.add(1);
			} else if (on) {
				lengths.set(lengths.size() - 1, lengths.get(lengths.size() - 1) + 1);
			} else if (!lengths.isEmpty() && lengths.get(lengths.size() - 1) > 0) {
				// A negative length marks the last pulse as over.
				lengths.set(lengths.size() - 1, -lengths.get(lengths.size() - 1));
			}
		});
		helper.runAfterDelay(period * 4 + GiantBeatingHeartBlock.PULSE_TICKS + 3, () -> {
			helper.assertTrue(onsets.size() >= 4, "It beats by itself once a period: beats at " + onsets);
			for (int i = 1; i < onsets.size(); i++) {
				helper.assertTrue(onsets.get(i) - onsets.get(i - 1) == period, "A beat every " + period + " ticks: beats at " + onsets);
			}
			for (int length : lengths) {
				helper.assertTrue(Math.abs(length) == GiantBeatingHeartBlock.PULSE_TICKS,
						"Each beat's signal lasts " + GiantBeatingHeartBlock.PULSE_TICKS + " ticks: " + lengths);
			}
			helper.succeed();
		});
	}

	/**
	 * The Tall Specimen Jar stands two blocks tall and the Specimen Tank 3 x 3 x 3, glowing, each shaped as the Specimen
	 * Jar made two or three times bigger; sneak-use on any block of one puts in the next specimen on all its blocks; broken
	 * anywhere, each goes whole and drops once, keeping its specimen.
	 */
	@GameTest(maxTicks = 40)
	public void biggerSpecimenJarsKeepTheirSpecimens(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		floor(helper);
		ServerPlayer keeper = player(helper, new BlockPos(1, 2, 8), ItemStack.EMPTY);
		SpecimenVesselBlock tall = (SpecimenVesselBlock) block("tall_specimen_jar");
		BlockPos jar = new BlockPos(1, 2, 2);
		place(helper, keeper, "tall_specimen_jar", jar, 0.0F);
		helper.assertTrue(helper.getBlockState(jar).is(tall) && helper.getBlockState(jar.above()).is(tall) && tall.part(helper.getBlockState(jar.above())) == 1
				&& helper.getBlockState(jar).getLightEmission() == TallSpecimenJarBlock.LIGHT
				&& helper.getBlockState(jar).getValue(SpecimenVesselBlock.SPECIMEN) == SpecimenJarBlock.Specimen.EYE, "The tall jar stands two tall, glowing, with an eye");
		// The Specimen Jar's box (4 to 12 across and deep, 13 tall) twice over: a full block under one 10 pixels tall.
		BlockPos lowerJar = helper.absolutePos(jar);
		AABB upperJar = level.getBlockState(lowerJar.above()).getShape(level, lowerJar.above()).bounds();
		helper.assertTrue(level.getBlockState(lowerJar).getShape(level, lowerJar).bounds().equals(new AABB(0, 0, 0, 1, 1, 1))
				&& Math.abs(upperJar.maxY - 10.0 / 16) < 1e-6 && upperJar.minX == 0.0 && upperJar.maxX == 1.0,
				"The tall jar's shape is the Specimen Jar's twice over: " + upperJar);

		SpecimenVesselBlock tank = (SpecimenVesselBlock) block("specimen_tank");
		BlockPos master = new BlockPos(6, 2, 4);
		BlockPos absolute = helper.absolutePos(master);
		place(helper, keeper, "specimen_tank", master, 0.0F);
		helper.assertTrue(tank.cells().length == 27, "The tank is 27 blocks");
		// Three times the Specimen Jar's box: 12 to 36 across and deep, 39 tall; its first block holds 4 of it each way.
		AABB tankFirst = level.getBlockState(absolute).getShape(level, absolute).bounds();
		helper.assertTrue(Math.abs(tankFirst.maxX - 4.0 / 16) < 1e-6 && Math.abs(tankFirst.minZ - 12.0 / 16) < 1e-6,
				"The tank's shape is the Specimen Jar's three times over: " + tankFirst);
		for (int part = 0; part < tank.cells().length; part++) {
			BlockState state = level.getBlockState(tank.partPos(absolute, Direction.NORTH, part));
			helper.assertTrue(state.is(tank) && tank.part(state) == part && state.getLightEmission() == SpecimenTankBlock.LIGHT,
					"Part " + part + " of the tank is in place and glows");
		}
		keeper.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
		keeper.setShiftKeyDown(true);
		use(helper, keeper, new BlockPos(5, 3, 5), Direction.NORTH);
		use(helper, keeper, jar.above(), Direction.NORTH);
		keeper.setShiftKeyDown(false);
		for (int part = 0; part < tank.cells().length; part++) {
			helper.assertTrue(level.getBlockState(tank.partPos(absolute, Direction.NORTH, part)).getValue(SpecimenVesselBlock.SPECIMEN)
					== SpecimenJarBlock.Specimen.TENTACLE, "Sneak-use puts a tentacle in every block of the tank: part " + part);
		}
		helper.assertTrue(helper.getBlockState(jar).getValue(SpecimenVesselBlock.SPECIMEN) == SpecimenJarBlock.Specimen.TENTACLE,
				"and in the tall jar, used at its top");
		use(helper, keeper, jar, Direction.NORTH);
		helper.assertTrue(helper.getBlockState(jar).getValue(SpecimenVesselBlock.SPECIMEN) == SpecimenJarBlock.Specimen.TENTACLE,
				"Used without sneaking, it keeps its specimen");

		level.destroyBlock(tank.partPos(absolute, Direction.NORTH, 7), true);
		level.destroyBlock(helper.absolutePos(jar.above()), true);
		for (int part = 0; part < tank.cells().length; part++) {
			helper.assertFalse(level.getBlockState(tank.partPos(absolute, Direction.NORTH, part)).is(tank), "Broken anywhere, part " + part + " goes too");
		}
		helper.assertFalse(helper.getBlockState(jar).is(tall), "and the tall jar goes whole");
		helper.succeedWhen(() -> {
			for (String id : List.of("tall_specimen_jar", "specimen_tank")) {
				List<ItemEntity> dropped = drops(helper, item(id));
				helper.assertTrue(dropped.size() == 1 && dropped.get(0).getItem().getCount() == 1, id + " drops once");
				BlockItemStateProperties state = dropped.get(0).getItem().get(DataComponents.BLOCK_STATE);
				helper.assertTrue(state != null && state.apply(block(id).defaultBlockState()).getValue(SpecimenVesselBlock.SPECIMEN)
						== SpecimenJarBlock.Specimen.TENTACLE, id + " keeps its tentacle: " + state);
			}
		});
	}

	/** Recipes and loot tables load. */
	@GameTest
	public void workshopDataLoads(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		for (String id : List.of("horned_skull_cauldron", "horned_skull_cauldron_from_bones", "ember_bed", "brew_ladle", "floor_candelabrum",
				"table_candelabrum", "wall_girandole", "branching_chandelier", "enchanted_broom", "dustpan", "broom_rack", "curiosity_cabinet", "bell_jar",
				"moth_display_case", "jar_of_eyeballs", "beating_heart_jar", "bat_in_a_jar", "two_headed_snake_jar", "hand_in_a_jar",
				"giant_beating_heart", "tall_specimen_jar", "specimen_tank")) {
			helper.assertTrue(level.recipeAccess().byKey(ResourceKey.create(Registries.RECIPE, Jugcraft.id(id))).isPresent(), id + " has a recipe");
		}
		for (String id : List.of("horned_skull_cauldron", "ember_bed", "floor_candelabrum", "table_candelabrum", "wall_girandole", "branching_chandelier",
				"enchanted_broom", "dustpan", "broom_rack", "curiosity_cabinet", "bell_jar", "moth_display_case", "jar_of_eyeballs", "beating_heart_jar",
				"bat_in_a_jar", "two_headed_snake_jar", "hand_in_a_jar", "giant_beating_heart", "tall_specimen_jar", "specimen_tank")) {
			helper.assertTrue(level.getServer().reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE,
					Jugcraft.id("blocks/" + id))) != LootTable.EMPTY, id + "'s loot loads");
		}
		helper.succeed();
	}
}
