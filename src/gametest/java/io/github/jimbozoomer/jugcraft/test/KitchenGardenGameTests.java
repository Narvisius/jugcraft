package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.agriculture.CookingPotBlock;
import io.github.jimbozoomer.jugcraft.agriculture.CookingPotBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.CookingPotRecipe;
import io.github.jimbozoomer.jugcraft.agriculture.JugcraftAgriculture;
import io.github.jimbozoomer.jugcraft.agriculture.TallCrop;
import io.github.jimbozoomer.jugcraft.agriculture.TallCropBlock;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** In-game tests for the Agriculture branch's Kitchen Garden: trellises, tomatoes, peppers, new crops and the Cooking Pot. */
public class KitchenGardenGameTests {
	private static final BlockPos SOIL = new BlockPos(2, 1, 2);
	private static final BlockPos CROP = SOIL.above();

	private static Item item(String id) {
		return JugcraftAgriculture.item(id);
	}

	private static Block block(String id) {
		return JugcraftAgriculture.block(id);
	}

	private static TallCropBlock tomato() {
		return JugcraftAgriculture.TALL_CROPS.get(TallCrop.TOMATO);
	}

	private static void farmland(GameTestHelper helper, BlockPos pos) {
		helper.setBlock(pos, Blocks.FARMLAND.defaultBlockState().setValue(BlockStateProperties.MOISTURE, 7));
	}

	private static BlockHitResult hitTop(GameTestHelper helper, BlockPos pos) {
		BlockPos absolute = helper.absolutePos(pos);
		return new BlockHitResult(Vec3.atCenterOf(absolute).add(0.0, 0.5, 0.0), Direction.UP, absolute, false);
	}

	/** Farmland at {@link #SOIL}, a trellis tower {@code height} tall on it, and a tomato planted at the bottom. */
	private static void plantTomato(GameTestHelper helper, int height) {
		farmland(helper, SOIL);
		for (int i = 0; i < height; i++) {
			helper.setBlock(CROP.above(i), block("trellis"));
		}
		helper.setBlock(CROP, tomato().defaultBlockState());
	}

	private static int itemsAround(GameTestHelper helper, BlockPos pos, Item item) {
		int count = 0;
		for (ItemEntity entity : helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(pos)).inflate(3.0))) {
			if (entity.getItem().is(item)) {
				count += entity.getItem().getCount();
			}
		}
		return count;
	}

	// ---------------------------------------------------------------- trellis and tomatoes

	/** Tomato seeds do nothing on bare farmland; used on a trellis standing on farmland, they plant a tomato in it. */
	@GameTest
	public void tomatoesArePlantedOnATrellis(GameTestHelper helper) {
		farmland(helper, SOIL);
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item("tomato_seeds"), 2));
		helper.useBlock(SOIL, player, hitTop(helper, SOIL));
		helper.assertBlockNotPresent(tomato(), CROP);
		helper.setBlock(CROP, block("trellis"));
		helper.useBlock(CROP, player, hitTop(helper, CROP));
		helper.assertBlockPresent(tomato(), CROP);
		helper.assertTrue(player.getMainHandItem().getCount() == 1, "Planting should use one seed");
		helper.succeed();
	}

	/** A tomato only grows into trellis: without a second trellis it stops at one block; with one it climbs to two. */
	@GameTest
	public void tomatoesClimbTheirTrellis(GameTestHelper helper) {
		plantTomato(helper, 1);
		BlockPos bottom = helper.absolutePos(CROP);
		helper.assertTrue(tomato().growTo(helper.getLevel(), bottom, 2), "A tomato should grow while it fits one block");
		helper.assertTrue(!tomato().growTo(helper.getLevel(), bottom, 3), "A tomato grew into air above its trellis");
		helper.setBlock(CROP.above(), block("trellis"));
		helper.assertTrue(tomato().growTo(helper.getLevel(), bottom, 7), "A tomato should climb a second trellis");
		BlockState top = helper.getBlockState(CROP.above());
		helper.assertTrue(top.is(tomato()) && top.getValue(TallCropBlock.SECTION) == 1, "Expected the tomato's top in the trellis, found " + top);
		helper.assertTrue(!helper.getBlockState(CROP).getCollisionShape(helper.getLevel(), bottom).isEmpty(),
				"A tomato on its trellis should block movement like the trellis");
		helper.succeed();
	}

	/** Ripe tomatoes are picked and the plant keeps standing; breaking it gives back seeds, fruit and both trellises. */
	@GameTest(maxTicks = 40)
	public void brokenTomatoesLeaveTheirTrellises(GameTestHelper helper) {
		plantTomato(helper, 2);
		tomato().growTo(helper.getLevel(), helper.absolutePos(CROP), 7);
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		helper.useBlock(CROP.above(), player);
		helper.assertTrue(helper.getBlockState(CROP).getValue(TallCropBlock.AGE) == TallCrop.TOMATO.pickReset,
				"Picking should set the tomato back to its regrowth age");
		helper.assertItemEntityPresent(item("tomato"), CROP.above(), 3.0);
		tomato().growTo(helper.getLevel(), helper.absolutePos(CROP), 7);
		helper.getLevel().destroyBlock(helper.absolutePos(CROP.above()), true);
		helper.succeedWhen(() -> {
			helper.assertBlockNotPresent(tomato(), CROP);
			helper.assertTrue(itemsAround(helper, CROP, item("trellis")) == 2, "Expected both trellises back, found "
					+ itemsAround(helper, CROP, item("trellis")));
			helper.assertItemEntityPresent(item("tomato_seeds"), CROP, 3.0);
		});
	}

	/** Trellises and climbing tomatoes keep the farmland under them. */
	@GameTest(maxTicks = 60)
	public void trellisKeepsItsFarmland(GameTestHelper helper) {
		plantTomato(helper, 2);
		tomato().growTo(helper.getLevel(), helper.absolutePos(CROP), 7);
		farmland(helper, new BlockPos(4, 1, 2));
		helper.setBlock(new BlockPos(4, 2, 2), block("trellis"));
		helper.runAtTickTime(40, () -> {
			helper.assertBlockPresent(Blocks.FARMLAND, SOIL);
			helper.assertBlockPresent(Blocks.FARMLAND, new BlockPos(4, 1, 2));
			helper.assertBlockPresent(tomato(), CROP.above());
			helper.succeed();
		});
	}

	// ---------------------------------------------------------------- peppers and one-block crops

	/** Ripe peppers are picked like berries: peppers drop and the bush goes back to flowering. */
	@GameTest
	public void peppersArePickedAndRegrow(GameTestHelper helper) {
		farmland(helper, SOIL);
		helper.setBlock(CROP, JugcraftAgriculture.TALL_CROPS.get(TallCrop.PEPPER).defaultBlockState().setValue(TallCropBlock.AGE, 7));
		helper.useBlock(CROP, helper.makeMockPlayer(GameType.SURVIVAL));
		helper.assertTrue(helper.getBlockState(CROP).getValue(TallCropBlock.AGE) == TallCrop.PEPPER.pickReset,
				"Picking should set the pepper back to its regrowth age");
		helper.assertItemEntityPresent(item("pepper"), CROP, 3.0);
		helper.succeed();
	}

	/** Ripe onions, garlic, cabbage, oats and barley drop their harvest (26.x loot tables load). */
	@GameTest
	public void kitchenCropsDropTheirHarvest(GameTestHelper helper) {
		String[][] crops = {{"onion_crop", "onion"}, {"garlic_crop", "garlic"}, {"cabbage_crop", "cabbage"}, {"oat_crop", "oats"},
				{"barley_crop", "barley"}, {"cabbage_crop", "cabbage_seeds"}, {"oat_crop", "oat_seeds"}, {"barley_crop", "barley_seeds"}};
		for (String[] crop : crops) {
			BlockPos pos = new BlockPos(2, 2, 2);
			farmland(helper, pos.below());
			helper.setBlock(pos, ((CropBlock) block(crop[0])).getStateForAge(7));
			boolean found = false;
			for (ItemStack drop : Block.getDrops(helper.getBlockState(pos), helper.getLevel(), helper.absolutePos(pos), null)) {
				found |= drop.is(item(crop[1]));
			}
			helper.assertTrue(found, "A ripe " + crop[0] + " should drop " + crop[1]);
		}
		helper.succeed();
	}

	/** One swing of a sickle replants ripe onions and picks ripe peppers. */
	@GameTest
	public void sickleHarvestsTheKitchenGarden(GameTestHelper helper) {
		BlockPos onion = new BlockPos(1, 2, 2);
		BlockPos pepper = new BlockPos(2, 2, 2);
		farmland(helper, onion.below());
		farmland(helper, pepper.below());
		helper.setBlock(onion, ((CropBlock) block("onion_crop")).getStateForAge(7));
		helper.setBlock(pepper, JugcraftAgriculture.TALL_CROPS.get(TallCrop.PEPPER).defaultBlockState().setValue(TallCropBlock.AGE, 7));
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item("flint_sickle")));
		helper.useBlock(onion, player);
		helper.assertTrue(helper.getBlockState(onion).getValue(CropBlock.AGE) == 0, "The onion should be replanted");
		helper.assertTrue(helper.getBlockState(pepper).getValue(TallCropBlock.AGE) == TallCrop.PEPPER.pickReset, "The pepper should be picked");
		helper.succeed();
	}

	/** Kitchen foods carry the values in docs/branches/AGRICULTURE.md; soups return their bowl and do not stack. */
	@GameTest
	public void kitchenFoodsHaveTheirValues(GameTestHelper helper) {
		String[] foods = {"tomato", "pepper", "cabbage", "barley_bread", "sauerkraut", "garden_salad", "tomato_soup", "onion_soup",
				"vegetable_soup", "mushroom_barley_soup", "oat_porridge", "chili", "cabbage_rolls"};
		int[] nutrition = {3, 2, 3, 5, 4, 7, 8, 8, 10, 8, 6, 10, 6};
		for (int i = 0; i < foods.length; i++) {
			FoodProperties food = new ItemStack(item(foods[i])).get(DataComponents.FOOD);
			helper.assertTrue(food != null && food.nutrition() == nutrition[i], foods[i] + " should restore " + nutrition[i] + ", has " + food);
		}
		for (String raw : new String[] {"onion", "garlic", "oats", "barley"}) {
			helper.assertTrue(new ItemStack(item(raw)).get(DataComponents.FOOD) == null, raw + " should not be edible raw");
		}
		helper.assertTrue(new ItemStack(item("chili")).getMaxStackSize() == 1, "Soups should not stack");
		helper.succeed();
	}

	// ---------------------------------------------------------------- cooking pot

	/** Every Cooking Pot recipe loads and is found from its ingredients in any order. */
	@GameTest
	public void potRecipesLoad(GameTestHelper helper) {
		Object[][] recipes = {
				{"tomato_soup", new Item[] {item("onion"), item("tomato"), Items.BOWL, item("tomato")}},
				{"onion_soup", new Item[] {Items.BREAD, item("onion"), item("garlic"), item("onion"), Items.BOWL}},
				{"vegetable_soup", new Item[] {Items.POTATO, Items.CARROT, item("cabbage"), item("onion"), Items.BOWL}},
				{"mushroom_barley_soup", new Item[] {Items.BOWL, Items.BROWN_MUSHROOM, item("barley"), item("onion")}},
				{"oat_porridge", new Item[] {item("oats"), Items.SUGAR, item("oats"), Items.BOWL}},
				{"chili", new Item[] {Items.BEEF, item("onion"), item("pepper"), item("tomato"), item("beans"), Items.BOWL}},
				{"cabbage_rolls", new Item[] {item("garlic"), item("onion"), Items.BEEF, item("cabbage")}}};
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

	/**
	 * On a lit campfire the pot cooks batch after batch from stacked ingredients spread over any slots.
	 * Soups do not stack, so each bowl takes its own result slot.
	 */
	@GameTest(maxTicks = 600)
	public void potCooksOnACampfire(GameTestHelper helper) {
		BlockPos pos = new BlockPos(2, 2, 2);
		helper.setBlock(pos.below(), Blocks.CAMPFIRE);
		helper.setBlock(pos, block("cooking_pot"));
		CookingPotBlockEntity pot = helper.getBlockEntity(pos, CookingPotBlockEntity.class);
		pot.setItem(0, new ItemStack(Items.BOWL, 2));
		pot.setItem(1, new ItemStack(item("tomato"), 2));
		pot.setItem(3, new ItemStack(item("tomato"), 2));
		pot.setItem(5, new ItemStack(item("onion"), 2));
		helper.succeedWhen(() -> {
			int soups = 0;
			for (int slot = CookingPotBlockEntity.RESULT; slot < CookingPotBlockEntity.SLOTS; slot++) {
				soups += pot.getItem(slot).is(item("tomato_soup")) ? pot.getItem(slot).getCount() : 0;
			}
			helper.assertTrue(soups == 2, "Expected 2 tomato soup, found " + soups);
			for (int slot = 0; slot < CookingPotBlockEntity.INPUTS; slot++) {
				helper.assertTrue(pot.getItem(slot).isEmpty(), "Slot " + slot + " should be used up, holds " + pot.getItem(slot));
			}
		});
	}

	/** Breaking a Cooking Pot drops the pot and everything in it. */
	@GameTest(maxTicks = 40)
	public void brokenPotDropsItsContents(GameTestHelper helper) {
		BlockPos pos = new BlockPos(2, 2, 2);
		helper.setBlock(pos, block("cooking_pot"));
		CookingPotBlockEntity pot = helper.getBlockEntity(pos, CookingPotBlockEntity.class);
		pot.setItem(0, new ItemStack(Items.BOWL, 3));
		pot.setItem(CookingPotBlockEntity.RESULT + 1, new ItemStack(item("chili")));
		helper.getLevel().destroyBlock(helper.absolutePos(pos), true);
		helper.succeedWhen(() -> {
			helper.assertItemEntityPresent(Items.BOWL, pos, 2.0);
			helper.assertItemEntityPresent(item("chili"), pos, 2.0);
			helper.assertItemEntityPresent(item("cooking_pot"), pos, 2.0);
		});
	}

	/** Without heat below, or with a stray item among the ingredients, the pot does not cook. */
	@GameTest(maxTicks = 300)
	public void potNeedsHeatAndTheRightIngredients(GameTestHelper helper) {
		BlockPos cold = new BlockPos(1, 2, 2);
		BlockPos stray = new BlockPos(3, 2, 2);
		helper.setBlock(cold.below(), Blocks.STONE);
		helper.setBlock(stray.below(), Blocks.CAMPFIRE);
		for (BlockPos pos : new BlockPos[] {cold, stray}) {
			helper.setBlock(pos, block("cooking_pot"));
			CookingPotBlockEntity pot = helper.getBlockEntity(pos, CookingPotBlockEntity.class);
			pot.setItem(0, new ItemStack(Items.BOWL));
			pot.setItem(1, new ItemStack(item("tomato"), 2));
			pot.setItem(2, new ItemStack(item("onion")));
		}
		helper.getBlockEntity(stray, CookingPotBlockEntity.class).setItem(3, new ItemStack(Items.DIRT));
		helper.runAtTickTime(250, () -> {
			for (BlockPos pos : new BlockPos[] {cold, stray}) {
				helper.assertTrue(helper.getBlockEntity(pos, CookingPotBlockEntity.class).getItem(CookingPotBlockEntity.RESULT).isEmpty(),
						"The pot at " + pos + " should not have cooked");
				helper.assertTrue(!helper.getBlockState(pos).getValue(CookingPotBlock.COOKING), "The pot at " + pos + " should not show cooking");
			}
			helper.succeed();
		});
	}
}
