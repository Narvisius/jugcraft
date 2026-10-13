package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.agriculture.FeastBlock;
import io.github.jimbozoomer.jugcraft.agriculture.FeastDish;
import io.github.jimbozoomer.jugcraft.agriculture.FoodDisplay;
import io.github.jimbozoomer.jugcraft.agriculture.HearthOvenBlock;
import io.github.jimbozoomer.jugcraft.agriculture.HearthOvenBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.JugcraftAgriculture;
import io.github.jimbozoomer.jugcraft.agriculture.PieBlock;
import io.github.jimbozoomer.jugcraft.agriculture.PieFilling;
import io.github.jimbozoomer.jugcraft.agriculture.ShowcaseBlockEntity;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * In-game tests for feasts and food displays (the kitchen and cooking expansion's slice 2, tools/feasts.py): the Hearth
 * Oven's pies in the owner's art (chocolate and the sweet berry cheesecake bake and slice like the others); vanilla's
 * pumpkin pie set down by a sneaking player, eaten and cut a slice at a time and picked up whole as the vanilla pie;
 * the feasts served into bowls, eaten in place, down to leftovers that clear; and the plate, platter and serving tray
 * holding a thing to each place, chosen by where they are used.
 */
public class FeastsGameTests {
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

	/** A survival player standing at {@code standAt}, holding {@code held}. */
	private static ServerPlayer player(GameTestHelper helper, BlockPos standAt, ItemStack held) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setGameMode(GameType.SURVIVAL);
		BlockPos absolute = helper.absolutePos(standAt);
		player.setPos(absolute.getX() + 0.5, absolute.getY(), absolute.getZ() + 0.5);
		player.setItemInHand(InteractionHand.MAIN_HAND, held);
		return player;
	}

	/** Uses what the player holds on {@code pos}, at {@code x}, {@code y}, {@code z} within the block, on face {@code side}. */
	private static InteractionResult useAt(GameTestHelper helper, ServerPlayer player, BlockPos pos, double x, double y, double z, Direction side) {
		BlockPos absolute = helper.absolutePos(pos);
		BlockHitResult hit = new BlockHitResult(new Vec3(absolute.getX() + x, absolute.getY() + y, absolute.getZ() + z), side, absolute, false);
		return player.gameMode.useItemOn(player, helper.getLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND, hit);
	}

	private static InteractionResult use(GameTestHelper helper, ServerPlayer player, BlockPos pos) {
		return useAt(helper, player, pos, 0.5, 0.5, 0.5, Direction.UP);
	}

	private static int dropped(GameTestHelper helper, Item item) {
		AABB area = new AABB(helper.absolutePos(BlockPos.ZERO)).expandTowards(8, 8, 8).inflate(2.0);
		return helper.getLevel().getEntitiesOfClass(ItemEntity.class, area, entity -> entity.getItem().is(item)).stream()
				.mapToInt(entity -> entity.getItem().getCount()).sum();
	}

	private static int servings(GameTestHelper helper, BlockPos pos) {
		return helper.getBlockState(pos).getValue(FeastBlock.SERVINGS_LEFT);
	}

	private static int signal(GameTestHelper helper, BlockPos pos) {
		ServerLevel level = helper.getLevel();
		BlockPos absolute = helper.absolutePos(pos);
		return level.getBlockState(absolute).getAnalogOutputSignal(level, absolute, Direction.NORTH);
	}

	// ---------------------------------------------------------------- pies

	/**
	 * The two new Hearth Oven pies in the owner's art bake like the others: a sweet berry cheesecake baked on time comes
	 * out whole; set down, a hungry player eats a slice of chocolate pie (four food) and a knife cuts a slice of
	 * cheesecake; their raw pies exist.
	 */
	@GameTest(maxTicks = 40)
	public void ownerPiesBakeAndSlice(GameTestHelper helper) {
		floor(helper);
		BlockPos ovenPos = new BlockPos(1, 2, 1);
		helper.setBlock(ovenPos, block("hearth_oven").defaultBlockState().setValue(HearthOvenBlock.FACING, Direction.SOUTH));
		HearthOvenBlockEntity oven = helper.getBlockEntity(ovenPos, HearthOvenBlockEntity.class);
		oven.set(1000, HearthOvenBlockEntity.MAX_HEAT, PieFilling.SWEET_BERRY, HearthOvenBlockEntity.BAKED - 6);
		ServerPlayer baker = player(helper, new BlockPos(1, 2, 4), ItemStack.EMPTY);
		helper.assertTrue(PieFilling.SWEET_BERRY.pie().equals("sweet_berry_cheesecake") && item("raw_sweet_berry_cheesecake") != null
				&& item("raw_chocolate_pie") != null, "The cheesecake and the chocolate pie have raw pies");
		helper.runAfterDelay(5, () -> {
			use(helper, baker, ovenPos);
			helper.assertTrue(baker.getInventory().countItem(item("sweet_berry_cheesecake")) == 1, "Out comes a sweet berry cheesecake");

			BlockPos chocolate = new BlockPos(4, 2, 2);
			helper.setBlock(chocolate, block("chocolate_pie"));
			baker.getFoodData().setFoodLevel(4);
			use(helper, baker, chocolate);
			helper.assertTrue(baker.getFoodData().getFoodLevel() == 8 && helper.getBlockState(chocolate).getValue(PieBlock.BITES) == 1,
					"A hungry player eats a slice of chocolate pie: four food");
			BlockPos cheesecake = new BlockPos(6, 2, 2);
			helper.setBlock(cheesecake, block("sweet_berry_cheesecake"));
			baker.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item("iron_knife")));
			use(helper, baker, cheesecake);
			helper.assertTrue(baker.getInventory().countItem(item("sweet_berry_cheesecake_slice")) == 1
					&& helper.getBlockState(cheesecake).getValue(PieBlock.BITES) == 1, "A knife cuts a slice of cheesecake");
			helper.succeed();
		});
	}

	/**
	 * Sneaking, a player sets a pumpkin pie down on a block, and it is used up; not sneaking, nothing is set down. Set
	 * down, a hungry player eats a slice (two food) and a knife cuts a Slice of Pumpkin Pie; four slices add up to the
	 * vanilla pie. Whole, it breaks into the vanilla pie; cut, into nothing.
	 */
	@GameTest(maxTicks = 20)
	public void pumpkinPieSetsDownAndSlices(GameTestHelper helper) {
		floor(helper);
		BlockPos ground = new BlockPos(3, 1, 3);
		BlockPos pie = ground.above();
		ServerPlayer player = player(helper, new BlockPos(3, 2, 6), new ItemStack(Items.PUMPKIN_PIE, 2));
		use(helper, player, ground);
		helper.assertTrue(helper.getBlockState(pie).isAir() && player.getMainHandItem().getCount() == 2, "Not sneaking, no pie is set down");
		player.setShiftKeyDown(true);
		useAt(helper, player, ground, 0.5, 1.0, 0.5, Direction.UP);
		helper.assertTrue(helper.getBlockState(pie).is(block("pumpkin_pie")) && player.getMainHandItem().getCount() == 1,
				"Sneaking, the pumpkin pie is set down and used up");
		player.setShiftKeyDown(false);

		FoodProperties slice = new ItemStack(item("pumpkin_pie_slice")).get(DataComponents.FOOD);
		FoodProperties whole = new ItemStack(Items.PUMPKIN_PIE).get(DataComponents.FOOD);
		helper.assertTrue(slice != null && whole != null && PieBlock.SLICES * slice.nutrition() == whole.nutrition()
				&& slice.saturation() * PieBlock.SLICES <= whole.saturation() + 1e-4, "Four slices add up to the vanilla pie");
		player.getFoodData().setFoodLevel(4);
		player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
		use(helper, player, pie);
		helper.assertTrue(player.getFoodData().getFoodLevel() == 6 && helper.getBlockState(pie).getValue(PieBlock.BITES) == 1,
				"A hungry player eats a slice: two food");
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item("flint_knife")));
		use(helper, player, pie);
		helper.assertTrue(player.getInventory().countItem(item("pumpkin_pie_slice")) == 1 && helper.getBlockState(pie).getValue(PieBlock.BITES) == 2,
				"A knife cuts a Slice of Pumpkin Pie");
		helper.getLevel().destroyBlock(helper.absolutePos(pie), true);
		BlockPos second = new BlockPos(5, 2, 3);
		helper.setBlock(second, block("pumpkin_pie"));
		helper.getLevel().destroyBlock(helper.absolutePos(second), true);
		helper.runAfterDelay(2, () -> {
			helper.assertTrue(dropped(helper, Items.PUMPKIN_PIE) == 1, "Only the whole pie breaks into a pumpkin pie: " + dropped(helper, Items.PUMPKIN_PIE));
			helper.succeed();
		});
	}

	// ---------------------------------------------------------------- feasts

	/**
	 * A roast chicken: whole, a comparator reads 15. A bowl takes a serving away (the bowl is used, a Bowl of Roast Chicken
	 * given: five food, back to a bowl when eaten); a hungry player eats one in place (five food); a full one doesn't. The
	 * last serving leaves the leftovers, which a use clears for a bone. A whole feast breaks into itself; one served from,
	 * into nothing. The stuffed pumpkin's leftovers give two pumpkin seeds; the gleaming salad glows while any is left.
	 */
	@GameTest(maxTicks = 20)
	public void feastsServeEatAndClear(GameTestHelper helper) {
		floor(helper);
		BlockPos pos = new BlockPos(3, 2, 3);
		helper.setBlock(pos, block("roast_chicken"));
		helper.assertTrue(servings(helper, pos) == FeastBlock.SERVINGS && signal(helper, pos) == 15, "A whole feast: four servings, 15");
		ServerPlayer player = player(helper, new BlockPos(3, 2, 6), new ItemStack(Items.BOWL, 3));
		player.getFoodData().setFoodLevel(20);
		use(helper, player, pos);
		ItemStack serving = new ItemStack(item("bowl_of_roast_chicken"));
		FoodProperties food = serving.get(DataComponents.FOOD);
		helper.assertTrue(servings(helper, pos) == 3 && player.getMainHandItem().getCount() == 2
				&& player.getInventory().countItem(item("bowl_of_roast_chicken")) == 1 && food != null
				&& food.nutrition() == FeastDish.ROAST_CHICKEN.nutrition && serving.getMaxStackSize() == 1,
				"A bowl takes a serving away: five food, one to a stack");
		player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
		use(helper, player, pos);
		helper.assertTrue(servings(helper, pos) == 3, "A full player eats nothing");
		player.getFoodData().setFoodLevel(4);
		use(helper, player, pos);
		helper.assertTrue(servings(helper, pos) == 2 && player.getFoodData().getFoodLevel() == 9, "A hungry player eats a serving: five food");
		player.getFoodData().setFoodLevel(4);
		use(helper, player, pos);
		player.getFoodData().setFoodLevel(4);
		use(helper, player, pos);
		helper.assertTrue(servings(helper, pos) == 0 && signal(helper, pos) == 0, "The last serving leaves the leftovers");
		use(helper, player, pos);
		helper.assertTrue(helper.getBlockState(pos).isAir(), "A use clears the leftovers");

		BlockPos pumpkin = new BlockPos(1, 2, 1);
		helper.setBlock(pumpkin, block("stuffed_pumpkin").defaultBlockState().setValue(FeastBlock.SERVINGS_LEFT, 0));
		use(helper, player, pumpkin);
		BlockPos whole = new BlockPos(5, 2, 1);
		helper.setBlock(whole, block("honey_glazed_ham"));
		helper.getLevel().destroyBlock(helper.absolutePos(whole), true);
		BlockPos served = new BlockPos(6, 2, 1);
		helper.setBlock(served, block("shepherds_pie").defaultBlockState().setValue(FeastBlock.SERVINGS_LEFT, 2));
		helper.getLevel().destroyBlock(helper.absolutePos(served), true);

		BlockPos salad = new BlockPos(1, 2, 5);
		helper.setBlock(salad, block("gleaming_salad"));
		BlockState full = helper.getBlockState(salad);
		BlockState empty = full.setValue(FeastBlock.SERVINGS_LEFT, 0);
		helper.assertTrue(full.getLightEmission() == FeastDish.GLEAMING_SALAD.light && empty.getLightEmission() == 0,
				"The gleaming salad glows while any is left");
		helper.runAfterDelay(2, () -> {
			helper.assertTrue(dropped(helper, Items.BONE) == 1, "The chicken's leftovers clear for a bone");
			helper.assertTrue(dropped(helper, Items.PUMPKIN_SEEDS) == 2, "The stuffed pumpkin's leftovers clear for two seeds");
			helper.assertTrue(dropped(helper, item("honey_glazed_ham")) == 1 && dropped(helper, item("shepherds_pie")) == 0,
					"Only a whole feast breaks into itself");
			helper.succeed();
		});
	}

	// ---------------------------------------------------------------- displays

	/**
	 * A platter facing north holds one thing to each quarter: an apple set at its north-west, bread at its south-east;
	 * an empty hand takes the apple back from where it lies; a comparator reads how full it is. A plate holds one thing.
	 * Broken, a display spills what is on it.
	 */
	@GameTest(maxTicks = 20)
	public void displaysHoldAThingToAPlace(GameTestHelper helper) {
		floor(helper);
		BlockPos pos = new BlockPos(3, 2, 3);
		helper.setBlock(pos, block("platter"));
		ShowcaseBlockEntity platter = helper.getBlockEntity(pos, ShowcaseBlockEntity.class);
		helper.assertTrue(platter.places() == FoodDisplay.PLATTER.places(), "A platter has four places");
		ServerPlayer player = player(helper, new BlockPos(3, 2, 6), new ItemStack(Items.APPLE, 3));
		useAt(helper, player, pos, 0.3, 1.0 / 16, 0.3, Direction.UP);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BREAD));
		useAt(helper, player, pos, 0.7, 1.0 / 16, 0.7, Direction.UP);
		helper.assertTrue(platter.get(0).is(Items.APPLE) && platter.get(3).is(Items.BREAD) && platter.count() == 2 && signal(helper, pos) == 7,
				"An apple lies at the north-west, bread at the south-east; a comparator reads 7");
		player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
		useAt(helper, player, pos, 0.3, 1.0 / 16, 0.3, Direction.UP);
		helper.assertTrue(platter.get(0).isEmpty() && player.getMainHandItem().is(Items.APPLE), "An empty hand takes the apple back");

		BlockPos plate = new BlockPos(5, 2, 3);
		helper.setBlock(plate, block("plate"));
		ShowcaseBlockEntity onPlate = helper.getBlockEntity(plate, ShowcaseBlockEntity.class);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item("cake_slice"), 2));
		use(helper, player, plate);
		helper.assertTrue(onPlate.places() == 1 && onPlate.get(0).is(item("cake_slice")) && player.getMainHandItem().getCount() == 1,
				"A plate holds one thing");
		use(helper, player, plate);
		helper.assertTrue(onPlate.count() == 0 && player.getInventory().countItem(item("cake_slice")) == 2, "Using it again takes it back");
		use(helper, player, plate);
		helper.getLevel().destroyBlock(helper.absolutePos(plate), true);
		helper.runAfterDelay(2, () -> {
			helper.assertTrue(dropped(helper, item("cake_slice")) == 1 && dropped(helper, item("plate")) == 1,
					"Broken, the plate spills its slice and drops itself");
			helper.succeed();
		});
	}
}
