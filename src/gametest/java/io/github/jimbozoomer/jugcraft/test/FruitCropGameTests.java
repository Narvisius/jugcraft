package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.agriculture.CookingPotBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.CookingPotRecipe;
import io.github.jimbozoomer.jugcraft.agriculture.JugcraftAgriculture;
import io.github.jimbozoomer.jugcraft.agriculture.PreserveJarItem;
import io.github.jimbozoomer.jugcraft.agriculture.TallCrop;
import io.github.jimbozoomer.jugcraft.agriculture.TallCropBlock;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * In-game tests for the fruit crops (tools/fruit_crops.py): the strawberry plant, the blueberry bush and the coffee plant
 * are planted from their seeds on farmland and ripen a block tall; ripe, they are picked and fruit again; a fruit crafts
 * into its seeds; wild plants give seeds; coffee cherries roast into beans; the three jams cook in the Cooking Pot; and
 * the foods carry their values. The plum and banana trees are orchard trees, tested with the others in
 * {@link OrchardGameTests}.
 */
public class FruitCropGameTests {
	private static final TallCrop[] BUSHES = {TallCrop.STRAWBERRY, TallCrop.BLUEBERRY, TallCrop.COFFEE};
	private static final String[] WILD = {"wild_strawberries", "wild_blueberries", "wild_coffee"};

	private static Item item(String id) {
		return JugcraftAgriculture.item(id);
	}

	private static TallCropBlock bush(TallCrop crop) {
		return JugcraftAgriculture.TALL_CROPS.get(crop);
	}

	private static void farmland(GameTestHelper helper, BlockPos pos) {
		helper.setBlock(pos, Blocks.FARMLAND.defaultBlockState().setValue(BlockStateProperties.MOISTURE, 7));
	}

	private static int droppedNear(GameTestHelper helper, BlockPos pos, Item item) {
		AABB area = new AABB(helper.absolutePos(pos)).inflate(1.5);
		return helper.getLevel().getEntitiesOfClass(ItemEntity.class, area, entity -> entity.getItem().is(item)).stream()
				.mapToInt(entity -> entity.getItem().getCount()).sum();
	}

	// ---------------------------------------------------------------- the bushes

	/** Each bush's seeds plant it on farmland, and it ripens through every age a block tall. */
	@GameTest
	public void bushesArePlantedAndRipenABlockTall(GameTestHelper helper) {
		for (int i = 0; i < BUSHES.length; i++) {
			TallCrop crop = BUSHES[i];
			BlockPos soil = new BlockPos(1 + 2 * i, 1, 2);
			farmland(helper, soil);
			Player player = helper.makeMockPlayer(GameType.SURVIVAL);
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item(crop.seedId)));
			BlockPos absolute = helper.absolutePos(soil);
			helper.useBlock(soil, player, new BlockHitResult(Vec3.atCenterOf(absolute).add(0.0, 0.5, 0.0), Direction.UP, absolute, false));
			helper.assertBlockPresent(bush(crop), soil.above());
			for (int age = 1; age <= TallCropBlock.MAX_AGE; age++) {
				helper.assertTrue(bush(crop).growTo(helper.getLevel(), helper.absolutePos(soil.above()), age),
						crop.blockId + " could not grow to age " + age);
				helper.assertBlockNotPresent(bush(crop), soil.above(2));
			}
			helper.assertTrue(TallCropBlock.isRipe(helper.getBlockState(soil.above())), crop.blockId + " should be ripe at age 7");
		}
		helper.succeed();
	}

	/** A ripe bush is picked for its fruit (as many as TallCrop gives), goes back to its regrowth age and ripens again. */
	@GameTest(maxTicks = 20)
	public void ripeBushesArePickedAndFruitAgain(GameTestHelper helper) {
		for (int i = 0; i < BUSHES.length; i++) {
			TallCrop crop = BUSHES[i];
			BlockPos pos = new BlockPos(1 + 3 * i, 2, 2);
			farmland(helper, pos.below());
			helper.setBlock(pos, bush(crop).defaultBlockState().setValue(TallCropBlock.AGE, TallCropBlock.MAX_AGE));
			helper.useBlock(pos, helper.makeMockPlayer(GameType.SURVIVAL));
			BlockState picked = helper.getBlockState(pos);
			helper.assertTrue(picked.is(bush(crop)) && picked.getValue(TallCropBlock.AGE) == crop.pickReset,
					"Picking should set the " + crop.blockId + " back to age " + crop.pickReset + ", found " + picked);
			int fruit = droppedNear(helper, pos, item(crop.produceId));
			helper.assertTrue(fruit >= crop.pickMin && fruit <= crop.pickMax,
					"Picking a " + crop.blockId + " drops " + crop.pickMin + "-" + crop.pickMax + " " + crop.produceId + ", not " + fruit);
			helper.assertTrue(bush(crop).growTo(helper.getLevel(), helper.absolutePos(pos), TallCropBlock.MAX_AGE),
					"A picked " + crop.blockId + " ripens again");
		}
		helper.succeed();
	}

	/** A wild plant broken by hand gives its crop's seeds (1-2); its patch and loot table load. */
	@GameTest(maxTicks = 40)
	public void wildBushesGiveSeeds(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		var placed = level.registryAccess().lookupOrThrow(Registries.PLACED_FEATURE);
		for (int i = 0; i < WILD.length; i++) {
			BlockPos pos = new BlockPos(1 + 3 * i, 2, 2);
			helper.setBlock(pos.below(), Blocks.GRASS_BLOCK);
			helper.setBlock(pos, JugcraftAgriculture.block(WILD[i]));
			level.destroyBlock(helper.absolutePos(pos), true);
			helper.assertTrue(level.getServer().reloadableRegistries().getLootTable(
					ResourceKey.create(Registries.LOOT_TABLE, Jugcraft.id("blocks/" + WILD[i]))) != LootTable.EMPTY, WILD[i] + "'s loot table loads");
			ResourceKey<PlacedFeature> patch = ResourceKey.create(Registries.PLACED_FEATURE, Jugcraft.id("patch_" + WILD[i]));
			helper.assertTrue(placed.get(patch).isPresent(), WILD[i] + " grows wild (patch_" + WILD[i] + ")");
		}
		helper.succeedWhen(() -> {
			for (int i = 0; i < WILD.length; i++) {
				int seeds = droppedNear(helper, new BlockPos(1 + 3 * i, 2, 2), item(BUSHES[i].seedId));
				helper.assertTrue(seeds >= 1 && seeds <= 2, WILD[i] + " gives 1-2 " + BUSHES[i].seedId + ", not " + seeds);
			}
		});
	}

	// ---------------------------------------------------------------- the fruit

	/** A strawberry, blueberries or coffee cherries craft into their seeds; coffee cherries roast into coffee beans. */
	@GameTest
	public void fruitGivesSeedsAndCherriesRoast(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		var recipes = level.getServer().getRecipeManager();
		for (TallCrop crop : BUSHES) {
			CraftingInput one = CraftingInput.of(1, 1, List.of(new ItemStack(item(crop.produceId))));
			ItemStack seeds = recipes.getRecipeFor(RecipeType.CRAFTING, one, level)
					.orElseThrow(() -> helper.assertionException(crop.produceId + " crafts nothing")).value().assemble(one);
			helper.assertTrue(seeds.is(item(crop.seedId)) && seeds.getCount() == 1, crop.produceId + " made " + seeds);
		}
		for (ItemStack roasted : List.of(roast(helper, RecipeType.SMELTING), roast(helper, RecipeType.SMOKING),
				roast(helper, RecipeType.CAMPFIRE_COOKING))) {
			helper.assertTrue(roasted.is(item("coffee_beans")) && roasted.getCount() == 1, "Coffee cherries roasted into " + roasted);
		}
		helper.succeed();
	}

	/** What a coffee cherry cooks into by {@code type} (a furnace, smoker or campfire). */
	private static <T extends Recipe<SingleRecipeInput>> ItemStack roast(GameTestHelper helper, RecipeType<T> type) {
		ServerLevel level = helper.getLevel();
		SingleRecipeInput cherries = new SingleRecipeInput(new ItemStack(item("coffee_cherries")));
		return level.getServer().getRecipeManager().getRecipeFor(type, cherries, level)
				.orElseThrow(() -> helper.assertionException(type + " does not roast coffee cherries")).value().assemble(cherries);
	}

	/** The jams cook in the Cooking Pot from a jar, their fruit and sugar. */
	@GameTest
	public void jamsCookInThePot(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		Map<String, List<ItemStack>> jams = Map.of(
				"strawberry_jam", List.of(new ItemStack(item("mason_jar")), new ItemStack(item("strawberry"), 6), new ItemStack(Items.SUGAR, 2)),
				"blueberry_jam", List.of(new ItemStack(item("mason_jar")), new ItemStack(item("blueberries"), 6), new ItemStack(Items.SUGAR, 2)),
				"plum_jam", List.of(new ItemStack(item("mason_jar")), new ItemStack(item("plum"), 3), new ItemStack(Items.SUGAR, 2)));
		for (Map.Entry<String, List<ItemStack>> jam : jams.entrySet()) {
			helper.assertTrue(level.recipeAccess().byKey(ResourceKey.create(Registries.RECIPE, Jugcraft.id("pot_cooking/" + jam.getKey()))).isPresent(),
					"Recipe pot_cooking/" + jam.getKey() + " loads");
			List<ItemStack> slots = new ArrayList<>(jam.getValue());
			while (slots.size() < CookingPotBlockEntity.INPUTS) {
				slots.add(ItemStack.EMPTY);
			}
			Optional<CookingPotRecipe.Match> match = CookingPotRecipe.find(level.getServer(), slots);
			helper.assertTrue(match.isPresent() && match.get().recipe().output().create().is(item(jam.getKey())),
					"The Cooking Pot makes " + jam.getKey() + " from " + jam.getValue());
		}
		helper.succeed();
	}

	/**
	 * The fruit and jams carry the values in tools/fruit_crops.py and tools/orchard.py (a jam's by the serving, as the
	 * pantry's preserves are eaten); coffee is not eaten as it is.
	 */
	@GameTest
	public void fruitHasItsValues(GameTestHelper helper) {
		String[] foods = {"strawberry", "blueberries", "plum", "banana"};
		int[] nutrition = {2, 2, 4, 4};
		for (int i = 0; i < foods.length; i++) {
			FoodProperties food = new ItemStack(item(foods[i])).get(DataComponents.FOOD);
			helper.assertTrue(food != null && food.nutrition() == nutrition[i], foods[i] + " should restore " + nutrition[i] + ", has " + food);
		}
		for (String jam : List.of("strawberry_jam", "blueberry_jam", "plum_jam")) {
			helper.assertTrue(item(jam) instanceof PreserveJarItem jar && jar.nutrition == 3, jam + " is a preserve of 3 a serving");
		}
		for (String raw : List.of("coffee_cherries", "coffee_beans")) {
			helper.assertTrue(new ItemStack(item(raw)).get(DataComponents.FOOD) == null, raw + " should not be eaten as it is");
		}
		helper.succeed();
	}
}
