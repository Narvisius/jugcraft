package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.agriculture.CookingPotBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.CookingPotRecipe;
import io.github.jimbozoomer.jugcraft.agriculture.CropGrowth;
import io.github.jimbozoomer.jugcraft.agriculture.CuttingBoardBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.FullTatamiMatBlock;
import io.github.jimbozoomer.jugcraft.agriculture.JugcraftAgriculture;
import io.github.jimbozoomer.jugcraft.agriculture.MenuDishes;
import io.github.jimbozoomer.jugcraft.agriculture.PaddyCropBlock;
import io.github.jimbozoomer.jugcraft.agriculture.PlacedDishBlock;
import io.github.jimbozoomer.jugcraft.agriculture.RollMedleyBlock;
import io.github.jimbozoomer.jugcraft.agriculture.TallCrop;
import io.github.jimbozoomer.jugcraft.agriculture.TallCropBlock;
import io.github.jimbozoomer.jugcraft.agriculture.TatamiBlock;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.HayBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * In-game tests for rice and wet farming (the kitchen and cooking expansion's slice 4, tools/rice.py): rice planted only
 * into shallow water over bog soil, a paddy growing faster the more of it is flooded, ripening two blocks tall, picked
 * for its panicles and left standing, and leaving its water when it goes; wild rice planted and broken from either
 * half, shears taking the plant; the Cutting Board cutting panicles and kelp rolls; the storage blocks and tatami
 * recipes; tatami pairing and the full mat laid as a bed is, dropping once; the roll medley serving its rolls and giving
 * the platter back; the Cooking Pot's rice dishes; and the rice dishes setting down as the menu's do.
 */
public class RiceGameTests {
	private static Item item(String id) {
		return JugcraftAgriculture.item(id);
	}

	private static Block block(String id) {
		return JugcraftAgriculture.block(id);
	}

	private static TallCropBlock rice() {
		return JugcraftAgriculture.TALL_CROPS.get(TallCrop.RICE);
	}

	private static void floor(GameTestHelper helper) {
		for (int x = 0; x <= 7; x++) {
			for (int z = 0; z <= 7; z++) {
				helper.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
			}
		}
	}

	/** Water `depth` blocks deep at `water` over mud, with stone around it so nothing flows. */
	private static void bogCell(GameTestHelper helper, BlockPos water, int depth) {
		helper.setBlock(water.below(), Blocks.MUD);
		for (int i = 0; i < depth; i++) {
			BlockPos pos = water.above(i);
			helper.setBlock(pos, Blocks.WATER);
			for (Direction side : Direction.Plane.HORIZONTAL) {
				helper.setBlock(pos.relative(side), Blocks.STONE);
			}
		}
	}

	/** A survival player standing at {@code standAt}, looking north, holding {@code held}. */
	private static ServerPlayer player(GameTestHelper helper, BlockPos standAt, ItemStack held) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setGameMode(GameType.SURVIVAL);
		BlockPos absolute = helper.absolutePos(standAt);
		player.setPos(absolute.getX() + 0.5, absolute.getY(), absolute.getZ() + 0.5);
		player.setYRot(180.0F);
		player.setItemInHand(InteractionHand.MAIN_HAND, held);
		return player;
	}

	/** Uses what the player holds on the {@code side} face of {@code pos}. */
	private static InteractionResult useFace(GameTestHelper helper, ServerPlayer player, BlockPos pos, Direction side) {
		BlockPos absolute = helper.absolutePos(pos);
		BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(absolute).relative(side, 0.5), side, absolute, false);
		return player.gameMode.useItemOn(player, helper.getLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND, hit);
	}

	/** Uses what the player holds on {@code pos} itself (a medley, a cutting board). */
	private static InteractionResult use(GameTestHelper helper, ServerPlayer player, BlockPos pos) {
		BlockPos absolute = helper.absolutePos(pos);
		BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(absolute), Direction.UP, absolute, false);
		return player.gameMode.useItemOn(player, helper.getLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND, hit);
	}

	/** How many of {@code item} lie on the ground within a block and a half of {@code pos}. */
	private static int droppedNear(GameTestHelper helper, BlockPos pos, Item item) {
		AABB area = new AABB(helper.absolutePos(pos)).inflate(1.5);
		return helper.getLevel().getEntitiesOfClass(ItemEntity.class, area, entity -> entity.getItem().is(item)).stream()
				.mapToInt(entity -> entity.getItem().getCount()).sum();
	}

	private static int dropped(GameTestHelper helper, Item item) {
		AABB area = new AABB(helper.absolutePos(BlockPos.ZERO)).expandTowards(8, 8, 8).inflate(2.0);
		return helper.getLevel().getEntitiesOfClass(ItemEntity.class, area, entity -> entity.getItem().is(item)).stream()
				.mapToInt(entity -> entity.getItem().getCount()).sum();
	}

	private static int found(GameTestHelper helper, ServerPlayer player, Item item) {
		return dropped(helper, item) + player.getInventory().countItem(item);
	}

	private static int count(List<ItemStack> stacks, Item item) {
		return stacks.stream().filter(stack -> stack.is(item)).mapToInt(ItemStack::getCount).sum();
	}

	// ---------------------------------------------------------------- the paddy

	/**
	 * Rice plants only into shallow water over bog soil, not deep water nor dry mud; the plant is the paddy crop, holding
	 * its water, and broken it leaves the paddy flooded.
	 */
	@GameTest(maxTicks = 20)
	public void riceIsPlantedInShallowWater(GameTestHelper helper) {
		BlockPos shallow = new BlockPos(2, 2, 2);
		BlockPos deep = new BlockPos(5, 2, 2);
		BlockPos dry = new BlockPos(2, 2, 5);
		bogCell(helper, shallow, 1);
		bogCell(helper, deep, 2);
		helper.setBlock(dry.below(), Blocks.MUD);
		ServerPlayer player = player(helper, new BlockPos(5, 2, 6), new ItemStack(item("rice"), 3));
		for (BlockPos pos : List.of(shallow, deep, dry)) {
			useFace(helper, player, pos.below(), Direction.UP);
		}
		BlockState planted = helper.getBlockState(shallow);
		helper.assertTrue(planted.is(rice()) && rice() instanceof PaddyCropBlock && planted.getValue(TallCropBlock.SECTION) == 0
				&& planted.getValue(TallCropBlock.AGE) == 0, "Rice plants in shallow water: " + planted);
		helper.assertTrue(planted.getFluidState().is(FluidTags.WATER), "The rice plant holds its water");
		helper.assertBlockNotPresent(rice(), deep);
		helper.assertBlockNotPresent(rice(), dry);
		helper.assertTrue(player.getMainHandItem().getCount() == 2, "Only the shallow planting uses a grain");
		helper.getLevel().destroyBlock(helper.absolutePos(shallow), true);
		helper.assertBlockPresent(Blocks.WATER, shallow);
		helper.succeed();
	}

	/**
	 * A paddy grows by its flooded soil as a field by its farmland: a plant flooded all round grows two and a half times as
	 * fast as one in a single flooded cell. Random ticks ripen a plant to two blocks tall, its panicles in the air.
	 */
	@GameTest(maxTicks = 60)
	public void paddyGrowsByItsWater(GameTestHelper helper) {
		BlockPos single = new BlockPos(1, 2, 1);
		bogCell(helper, single, 1);
		helper.setBlock(single, rice());
		helper.setBlock(single.above().north(), Blocks.GLOWSTONE);
		BlockPos flooded = new BlockPos(5, 2, 5);
		for (int dx = -1; dx <= 1; dx++) {
			for (int dz = -1; dz <= 1; dz++) {
				bogCell(helper, flooded.offset(dx, 0, dz), 1);
			}
		}
		for (int dx = -1; dx <= 1; dx++) {
			for (int dz = -1; dz <= 1; dz++) {
				helper.setBlock(flooded.offset(dx, 0, dz), Blocks.WATER);
			}
		}
		helper.setBlock(flooded, rice());
		ServerLevel level = helper.getLevel();
		float alone = CropGrowth.paddySpeed(level, helper.absolutePos(single));
		float all = CropGrowth.paddySpeed(level, helper.absolutePos(flooded));
		helper.assertTrue(Math.abs(all / alone - 2.5F) < 0.01F, "A paddy flooded all round grows 2.5 times as fast: " + all + " / " + alone);
		// Give the light engine time to light the new blocks, then tick.
		helper.runAtTickTime(40, () -> {
			for (int i = 0; i < 400; i++) {
				helper.getBlockState(single).randomTick(level, helper.absolutePos(single), level.getRandom());
			}
			BlockState bottom = helper.getBlockState(single);
			BlockState top = helper.getBlockState(single.above());
			helper.assertTrue(TallCropBlock.isRipe(bottom) && top.is(rice()) && top.getValue(TallCropBlock.SECTION) == 1,
					"The rice ripens two blocks tall, found " + bottom + " under " + top);
			helper.assertTrue(bottom.getFluidState().is(FluidTags.WATER) && top.getFluidState().isEmpty(), "Its bottom wet, its top dry");
			helper.succeed();
		});
	}

	/**
	 * Picking ripe rice drops its panicles and leaves the stalks standing two blocks tall; breaking the top drops the
	 * plant's loot once and leaves the paddy's water.
	 */
	@GameTest(maxTicks = 20)
	public void ricePickedAndBroken(GameTestHelper helper) {
		BlockPos pos = new BlockPos(2, 2, 2);
		bogCell(helper, pos, 1);
		helper.setBlock(pos, rice());
		helper.assertTrue(rice().growTo(helper.getLevel(), helper.absolutePos(pos), TallCropBlock.MAX_AGE), "Rice grows to ripe");
		ServerPlayer player = player(helper, new BlockPos(2, 2, 5), ItemStack.EMPTY);
		use(helper, player, pos.above());
		BlockState bottom = helper.getBlockState(pos);
		helper.assertTrue(bottom.is(rice()) && bottom.getValue(TallCropBlock.AGE) == TallCrop.RICE.pickReset
				&& helper.getBlockState(pos.above()).is(rice()), "Picked, the stalks stand two blocks tall: " + bottom);
		int panicles = droppedNear(helper, pos.above(), item("rice_panicle"));
		helper.assertTrue(panicles >= TallCrop.RICE.pickMin && panicles <= TallCrop.RICE.pickMax, "Picking drops 2-3 panicles, not " + panicles);
		player.gameMode.destroyBlock(helper.absolutePos(pos.above()));
		helper.assertBlockPresent(Blocks.WATER, pos);
		helper.assertBlockNotPresent(rice(), pos.above());
		helper.assertTrue(droppedNear(helper, pos, item("rice")) >= 1, "The broken plant gives its rice back");
		helper.succeed();
	}

	// ---------------------------------------------------------------- wild rice

	/**
	 * Wild rice is planted in shallow water, two blocks tall. Broken at its top with shears it gives the plant itself; by
	 * hand, 1-2 rice. Either way it goes whole and leaves its water.
	 */
	@GameTest(maxTicks = 20)
	public void wildRiceFromEitherHalf(GameTestHelper helper) {
		BlockPos sheared = new BlockPos(1, 2, 1);
		BlockPos picked = new BlockPos(5, 2, 5);
		bogCell(helper, sheared, 1);
		bogCell(helper, picked, 1);
		ServerPlayer player = player(helper, new BlockPos(3, 2, 7), new ItemStack(item("wild_rice"), 2));
		useFace(helper, player, sheared.below(), Direction.UP);
		useFace(helper, player, picked.below(), Direction.UP);
		for (BlockPos pos : List.of(sheared, picked)) {
			BlockState lower = helper.getBlockState(pos);
			BlockState upper = helper.getBlockState(pos.above());
			helper.assertTrue(lower.is(block("wild_rice")) && lower.getValue(DoublePlantBlock.HALF) == DoubleBlockHalf.LOWER
					&& upper.is(block("wild_rice")) && upper.getValue(DoublePlantBlock.HALF) == DoubleBlockHalf.UPPER
					&& lower.getFluidState().is(FluidTags.WATER), "Wild rice stands two blocks tall in its water");
		}
		List<ItemStack> drops = Block.getDrops(helper.getBlockState(picked), helper.getLevel(), helper.absolutePos(picked), null);
		helper.assertTrue(count(drops, item("rice")) >= 1 && count(drops, item("rice")) <= 2, "Its foot's loot is 1-2 rice: " + drops);
		helper.assertTrue(Block.getDrops(helper.getBlockState(picked.above()), helper.getLevel(), helper.absolutePos(picked.above()), null)
				.isEmpty(), "Its top has no loot of its own");

		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.SHEARS));
		player.gameMode.destroyBlock(helper.absolutePos(sheared.above()));
		helper.assertTrue(droppedNear(helper, sheared, item("wild_rice")) == 1 && droppedNear(helper, sheared, item("rice")) == 0,
				"Shears on the top take the plant");
		player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
		player.gameMode.destroyBlock(helper.absolutePos(picked.above()));
		int rice = droppedNear(helper, picked, item("rice"));
		helper.assertTrue(rice >= 1 && rice <= 2 && droppedNear(helper, picked, item("wild_rice")) == 0, "By hand it gives 1-2 rice, not " + rice);
		for (BlockPos pos : List.of(sheared, picked)) {
			helper.assertBlockPresent(Blocks.WATER, pos);
			helper.assertBlockNotPresent(block("wild_rice"), pos.above());
		}
		helper.succeed();
	}

	// ---------------------------------------------------------------- the board, the pot and the recipes

	/** On the Cutting Board a knife cuts a panicle into two rice and a straw, and a kelp roll into four slices. */
	@GameTest(maxTicks = 20)
	public void boardCutsPaniclesAndRolls(GameTestHelper helper) {
		floor(helper);
		BlockPos board = new BlockPos(3, 2, 3);
		helper.setBlock(board, block("cutting_board"));
		CuttingBoardBlockEntity entity = helper.getBlockEntity(board, CuttingBoardBlockEntity.class);
		ServerPlayer player = player(helper, new BlockPos(3, 2, 6), new ItemStack(item("rice_panicle")));
		use(helper, player, board);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item("iron_knife")));
		use(helper, player, board);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item("kelp_roll")));
		use(helper, player, board);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item("iron_knife")));
		use(helper, player, board);
		helper.runAfterDelay(2, () -> {
			helper.assertTrue(entity.item().isEmpty() && found(helper, player, item("rice")) == 2 && found(helper, player, item("straw")) == 1,
					"A panicle cuts into two rice and a straw");
			helper.assertTrue(found(helper, player, item("kelp_roll_slice")) == 4, "A kelp roll cuts into four slices");
			helper.succeed();
		});
	}

	/** The Cooking Pot cooks rice, fried rice and mushroom rice from their ingredients. */
	@GameTest(maxTicks = 20)
	public void potCooksRice(GameTestHelper helper) {
		Object[][] recipes = {
				{"cooked_rice", new Item[] {Items.BOWL, item("rice"), item("rice")}},
				{"fried_rice", new Item[] {Items.BOWL, item("rice"), item("rice"), Items.EGG, Items.CARROT, item("onion")}},
				{"mushroom_rice", new Item[] {Items.BOWL, item("rice"), item("rice"), Items.BROWN_MUSHROOM, Items.RED_MUSHROOM, Items.CARROT}}};
		for (Object[] recipe : recipes) {
			List<ItemStack> slots = new ArrayList<>();
			for (Item ingredient : (Item[]) recipe[1]) {
				slots.add(new ItemStack(ingredient));
			}
			while (slots.size() < CookingPotBlockEntity.INPUTS) {
				slots.add(ItemStack.EMPTY);
			}
			Optional<CookingPotRecipe.Match> match = CookingPotRecipe.find(helper.getLevel().getServer(), slots);
			helper.assertTrue(match.isPresent() && match.get().recipe().output().create().is(item((String) recipe[0])),
					"Ingredients for " + recipe[0] + " found " + match.map(m -> m.recipe().output().create()));
		}
		helper.succeed();
	}

	/** Every crafting recipe of the slice loads, the storage blocks' packing and unpacking among them, and rice grows hydroponically. */
	@GameTest(maxTicks = 20)
	public void riceRecipesExist(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		for (String id : List.of("rice_from_panicle", "salmon_roll", "cod_roll", "kelp_roll", "rice_roll_medley", "tatami", "full_tatami_mat",
				"half_tatami_mat", "rice_bag", "rice_bale", "straw_bale", "rice_from_bag", "rice_panicle_from_bale", "straw_from_bale",
				"cutting/rice", "cutting/kelp_roll_slice", "pot_cooking/cooked_rice", "hydroponics/rice")) {
			helper.assertTrue(level.recipeAccess().byKey(ResourceKey.create(Registries.RECIPE, Jugcraft.id(id))).isPresent(), id + " has a recipe");
		}
		helper.assertTrue(block("rice_bale") instanceof HayBlock && block("straw_bale") instanceof HayBlock,
				"The bales soften a fall as a hay bale does");
		helper.succeed();
	}

	// ---------------------------------------------------------------- tatami

	/**
	 * A tatami set against the side of a lone tatami pairs with it, each facing the other; sneaking sets one down alone;
	 * a tatami whose partner goes stands alone again.
	 */
	@GameTest(maxTicks = 20)
	public void tatamiPairs(GameTestHelper helper) {
		floor(helper);
		BlockPos first = new BlockPos(2, 2, 2);
		helper.setBlock(first, block("tatami"));
		ServerPlayer player = player(helper, new BlockPos(3, 2, 6), new ItemStack(item("tatami"), 2));
		useFace(helper, player, first, Direction.EAST);
		BlockState left = helper.getBlockState(first);
		BlockState right = helper.getBlockState(first.east());
		helper.assertTrue(right.is(block("tatami")) && right.getValue(TatamiBlock.PAIRED) && right.getValue(TatamiBlock.FACING) == Direction.WEST
				&& left.getValue(TatamiBlock.PAIRED) && left.getValue(TatamiBlock.FACING) == Direction.EAST,
				"Set against a lone tatami, the two pair: " + left + ", " + right);
		player.setShiftKeyDown(true);
		useFace(helper, player, first.east(), Direction.EAST);
		player.setShiftKeyDown(false);
		helper.assertTrue(!helper.getBlockState(first.east(2)).getValue(TatamiBlock.PAIRED), "Sneaking, a tatami is set down alone");
		helper.getLevel().destroyBlock(helper.absolutePos(first.east()), true);
		helper.assertTrue(!helper.getBlockState(first).getValue(TatamiBlock.PAIRED), "Its partner gone, the tatami stands alone");
		helper.succeed();
	}

	/**
	 * A Full Tatami Mat is laid as a bed is: its foot where it is used, its head ahead of the player. Breaking either half
	 * takes both and drops one mat.
	 */
	@GameTest(maxTicks = 20)
	public void fullMatLaysTwoHalvesAndDropsOnce(GameTestHelper helper) {
		floor(helper);
		BlockPos[] feet = {new BlockPos(2, 2, 5), new BlockPos(5, 2, 5)};
		ServerPlayer player = player(helper, new BlockPos(3, 2, 7), new ItemStack(item("full_tatami_mat"), 2));
		for (BlockPos foot : feet) {
			useFace(helper, player, foot.below(), Direction.UP);
			BlockState footState = helper.getBlockState(foot);
			BlockState headState = helper.getBlockState(foot.north());
			helper.assertTrue(footState.is(block("full_tatami_mat")) && footState.getValue(FullTatamiMatBlock.PART) == BedPart.FOOT
					&& footState.getValue(FullTatamiMatBlock.FACING) == Direction.NORTH && headState.is(block("full_tatami_mat"))
					&& headState.getValue(FullTatamiMatBlock.PART) == BedPart.HEAD, "The mat lies two blocks long, ahead of the player");
		}
		helper.getLevel().destroyBlock(helper.absolutePos(feet[0]), true);
		helper.getLevel().destroyBlock(helper.absolutePos(feet[1].north()), true);
		helper.runAfterDelay(2, () -> {
			for (BlockPos foot : feet) {
				helper.assertBlockNotPresent(block("full_tatami_mat"), foot);
				helper.assertBlockNotPresent(block("full_tatami_mat"), foot.north());
			}
			helper.assertTrue(dropped(helper, item("full_tatami_mat")) == 2, "Each mat drops once, from foot or head: "
					+ dropped(helper, item("full_tatami_mat")));
			helper.succeed();
		});
	}

	// ---------------------------------------------------------------- the medley and the dishes

	/**
	 * The Rice Roll Medley serves its eight rolls a use at a time (four kelp roll slices, two cod and two salmon rolls, as
	 * it is made of), a comparator reading the rolls left; the bare platter is cleared and given back. Whole, it drops
	 * itself.
	 */
	@GameTest(maxTicks = 20)
	public void medleyServesItsRolls(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		floor(helper);
		BlockPos pos = new BlockPos(3, 2, 3);
		helper.setBlock(pos, block("rice_roll_medley"));
		BlockPos absolute = helper.absolutePos(pos);
		helper.assertTrue(level.getBlockState(absolute).getAnalogOutputSignal(level, absolute, Direction.NORTH) == 15, "A whole medley reads 15");
		helper.assertTrue(count(Block.getDrops(level.getBlockState(absolute), level, absolute, null), item("rice_roll_medley")) == 1,
				"Whole, it drops itself");
		ServerPlayer player = player(helper, new BlockPos(3, 2, 6), ItemStack.EMPTY);
		for (int i = 0; i < RollMedleyBlock.MAX; i++) {
			use(helper, player, pos);
		}
		helper.assertTrue(helper.getBlockState(pos).getValue(RollMedleyBlock.ROLLS) == 0
				&& level.getBlockState(absolute).getAnalogOutputSignal(level, absolute, Direction.NORTH) == 0, "Eight uses take every roll");
		helper.assertTrue(player.getInventory().countItem(item("kelp_roll_slice")) == 4 && player.getInventory().countItem(item("cod_roll")) == 2
				&& player.getInventory().countItem(item("salmon_roll")) == 2, "It serves the rolls it is made of");
		use(helper, player, pos);
		helper.assertTrue(helper.getBlockState(pos).isAir(), "A use clears the bare platter");
		helper.runAfterDelay(2, () -> {
			helper.assertTrue(dropped(helper, item("platter")) == 1 && dropped(helper, item("rice_roll_medley")) == 0, "The platter comes back");
			helper.succeed();
		});
	}

	/** The rice dishes set down as the menu's do: a sneaking player sets a kelp roll slice down facing them. */
	@GameTest(maxTicks = 20)
	public void riceDishesSetDown(GameTestHelper helper) {
		floor(helper);
		for (String dish : List.of("cooked_rice", "fried_rice", "mushroom_rice", "salmon_roll", "cod_roll", "kelp_roll", "kelp_roll_slice")) {
			helper.assertTrue(MenuDishes.PLACED.stream().anyMatch(entry -> entry.id().equals(dish))
					&& block(dish) instanceof PlacedDishBlock placed && placed.dish() == item(dish), dish + " sets down as itself");
		}
		BlockPos ground = new BlockPos(3, 1, 3);
		ServerPlayer player = player(helper, new BlockPos(3, 2, 6), new ItemStack(item("kelp_roll_slice"), 2));
		player.setShiftKeyDown(true);
		useFace(helper, player, ground, Direction.UP);
		player.setShiftKeyDown(false);
		BlockState set = helper.getBlockState(ground.above());
		helper.assertTrue(set.is(block("kelp_roll_slice")) && set.getValue(PlacedDishBlock.FACING) == Direction.SOUTH
				&& player.getMainHandItem().getCount() == 1, "Sneaking, a slice is set down facing the player: " + set);
		helper.succeed();
	}
}
