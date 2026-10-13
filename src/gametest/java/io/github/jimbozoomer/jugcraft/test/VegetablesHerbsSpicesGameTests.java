package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.agriculture.CinnamonLogBlock;
import io.github.jimbozoomer.jugcraft.agriculture.CookingPotBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.CookingPotRecipe;
import io.github.jimbozoomer.jugcraft.agriculture.CropGrowth;
import io.github.jimbozoomer.jugcraft.agriculture.HerbBundleBlock;
import io.github.jimbozoomer.jugcraft.agriculture.JugcraftAgriculture;
import io.github.jimbozoomer.jugcraft.agriculture.SpiceGrinding;
import io.github.jimbozoomer.jugcraft.agriculture.SpiceRackBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.TallCrop;
import io.github.jimbozoomer.jugcraft.agriculture.TallCropBlock;
import io.github.jimbozoomer.jugcraft.concordance.JugcraftConcordance;
import io.github.jimbozoomer.jugcraft.concordance.RateGate;
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
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * In-game tests for garden crops, herbs and spices, part b (tools/vegetables.py, herbs.py, spices.py): the low crops planted
 * from their seeds and giving their produce; the bushes, herbs and vines picked and growing back; peas feeding their
 * neighbours; the Planter Box growing crops as watered farmland; herb bundles hanging, drying and giving Dried Herbs; sprigs
 * potting herbs; an axe peeling cinnamon from its log; the cinnamon sapling growing its tree; the Spice Rack holding
 * spices; the Mortar and Pestle grinding paprika; the dishes cooking; and the wild plants giving their seeds.
 */
public class VegetablesHerbsSpicesGameTests {
	/** The low crops, their seed and what they give ripe. */
	private static final String[][] LOW = {{"lettuce_crop", "lettuce_seeds", "lettuce"}, {"spinach_crop", "spinach_seeds", "spinach"},
			{"radish_crop", "radish", "radish"}, {"pea_crop", "peas", "peas"}, {"ginger_crop", "ginger", "ginger"},
			{"mustard_crop", "mustard_seeds", "mustard_seeds"}, {"saffron_crop", "crocus_bulb", "saffron"}};
	/** What is picked a block tall and grows back. */
	private static final TallCrop[] PICKED = {TallCrop.EGGPLANT, TallCrop.ZUCCHINI, TallCrop.BASIL, TallCrop.MINT, TallCrop.ROSEMARY,
			TallCrop.THYME, TallCrop.PARSLEY, TallCrop.SAGE, TallCrop.DILL, TallCrop.CHIVES};
	/** What climbs a trellis. */
	private static final TallCrop[] VINES = {TallCrop.CUCUMBER, TallCrop.PEPPERCORN, TallCrop.VANILLA};
	/** Each wild plant (tools/agriculture.py WILD_CROPS order) and the seed it gives. */
	private static final String[][] WILD = {{"wild_lettuce", "lettuce_seeds"}, {"wild_spinach", "spinach_seeds"}, {"wild_radishes", "radish"},
			{"wild_peas", "peas"}, {"wild_cucumbers", "cucumber_seeds"}, {"wild_eggplant", "eggplant_seeds"}, {"wild_zucchini", "zucchini_seeds"},
			{"wild_basil", "basil"}, {"wild_mint", "mint"}, {"wild_rosemary", "rosemary"}, {"wild_thyme", "thyme"}, {"wild_parsley", "parsley"},
			{"wild_sage", "sage"}, {"wild_dill", "dill"}, {"wild_chives", "chives"}, {"wild_peppercorns", "peppercorns"},
			{"wild_vanilla", "vanilla_pods"}, {"wild_ginger", "ginger"}, {"wild_mustard", "mustard_seeds"}, {"wild_saffron", "crocus_bulb"}};

	private static Item item(String id) {
		return JugcraftAgriculture.item(id);
	}

	private static Block block(String id) {
		return JugcraftAgriculture.block(id);
	}

	private static TallCropBlock crop(TallCrop crop) {
		return JugcraftAgriculture.TALL_CROPS.get(crop);
	}

	private static void farmland(GameTestHelper helper, BlockPos pos) {
		helper.setBlock(pos, Blocks.FARMLAND.defaultBlockState().setValue(BlockStateProperties.MOISTURE, 7));
	}

	private static BlockHitResult top(GameTestHelper helper, BlockPos pos) {
		BlockPos absolute = helper.absolutePos(pos);
		return new BlockHitResult(Vec3.atCenterOf(absolute).add(0.0, 0.5, 0.0), Direction.UP, absolute, false);
	}

	private static Player holding(GameTestHelper helper, ItemStack held) {
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		player.setItemInHand(InteractionHand.MAIN_HAND, held);
		return player;
	}

	/** How many of {@code item} lie dropped in the test area (each test drops each item from one place only). */
	private static int dropped(GameTestHelper helper, Item item) {
		AABB area = new AABB(helper.absolutePos(BlockPos.ZERO)).expandTowards(8, 8, 8).inflate(2.0);
		return helper.getLevel().getEntitiesOfClass(ItemEntity.class, area, entity -> entity.getItem().is(item)).stream()
				.mapToInt(entity -> entity.getItem().getCount()).sum();
	}

	private static int carried(Player player, Item item) {
		int count = 0;
		for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
			ItemStack stack = player.getInventory().getItem(slot);
			if (stack.is(item)) {
				count += stack.getCount();
			}
		}
		return count;
	}

	// ---------------------------------------------------------------- crops

	/** Each low crop's seed plants it on farmland; ripe and broken, it gives its produce. */
	@GameTest(maxTicks = 40)
	public void lowCropsArePlantedAndGiveTheirProduce(GameTestHelper helper) {
		for (int i = 0; i < LOW.length; i++) {
			BlockPos soil = new BlockPos(i, 1, 2);
			farmland(helper, soil);
			Player player = holding(helper, new ItemStack(item(LOW[i][1]), 2));
			helper.useBlock(soil, player, top(helper, soil));
			helper.assertBlockPresent(block(LOW[i][0]), soil.above());
			helper.assertTrue(player.getMainHandItem().getCount() == 1, "Planting " + LOW[i][0] + " uses one " + LOW[i][1]);
			helper.setBlock(soil.above(), helper.getBlockState(soil.above()).setValue(BlockStateProperties.AGE_7, 7));
			helper.getLevel().destroyBlock(helper.absolutePos(soil.above()), true);
		}
		helper.succeedWhen(() -> {
			for (int i = 0; i < LOW.length; i++) {
				helper.assertTrue(dropped(helper, item(LOW[i][2])) >= 1, "A ripe " + LOW[i][0] + " gives " + LOW[i][2]);
			}
		});
	}

	/** Ripe, the eggplant, zucchini and every herb are picked for their produce, go back to their regrowth age and ripen again. */
	@GameTest(maxTicks = 20)
	public void bushesAndHerbsArePickedAndGrowBack(GameTestHelper helper) {
		for (int i = 0; i < PICKED.length; i++) {
			TallCrop tall = PICKED[i];
			BlockPos pos = new BlockPos(i % 5, 2, 1 + 3 * (i / 5));
			farmland(helper, pos.below());
			helper.setBlock(pos, crop(tall).defaultBlockState().setValue(TallCropBlock.AGE, TallCropBlock.MAX_AGE));
			helper.useBlock(pos, helper.makeMockPlayer(GameType.SURVIVAL));
			BlockState picked = helper.getBlockState(pos);
			helper.assertTrue(picked.is(crop(tall)) && picked.getValue(TallCropBlock.AGE) == tall.pickReset,
					tall.blockId + " goes back to age " + tall.pickReset + " when picked, found " + picked);
			int produce = dropped(helper, item(tall.produceId));
			helper.assertTrue(produce >= tall.pickMin && produce <= tall.pickMax,
					"Picking " + tall.blockId + " gives " + tall.pickMin + "-" + tall.pickMax + " " + tall.produceId + ", not " + produce);
			helper.assertTrue(crop(tall).growTo(helper.getLevel(), helper.absolutePos(pos), TallCropBlock.MAX_AGE), tall.blockId + " ripens again");
		}
		helper.succeed();
	}

	/**
	 * Cucumber, peppercorn and vanilla seeds plant nothing on bare farmland; used on a trellis on farmland they plant their vine,
	 * which climbs into a second trellis, and ripe, is picked for its produce.
	 */
	@GameTest(maxTicks = 20)
	public void vinesClimbTheirTrellises(GameTestHelper helper) {
		for (int i = 0; i < VINES.length; i++) {
			TallCrop vine = VINES[i];
			BlockPos soil = new BlockPos(1 + 2 * i, 1, 2);
			farmland(helper, soil);
			Player player = holding(helper, new ItemStack(item(vine.seedId), 2));
			helper.useBlock(soil, player, top(helper, soil));
			helper.assertBlockNotPresent(crop(vine), soil.above());
			helper.setBlock(soil.above(), block("trellis"));
			helper.setBlock(soil.above(2), block("trellis"));
			helper.useBlock(soil.above(), player, top(helper, soil.above()));
			helper.assertBlockPresent(crop(vine), soil.above());
			helper.assertTrue(crop(vine).growTo(helper.getLevel(), helper.absolutePos(soil.above()), TallCropBlock.MAX_AGE),
					vine.blockId + " grows ripe up its trellis");
			helper.assertBlockPresent(crop(vine), soil.above(2));
			helper.useBlock(soil.above(2), helper.makeMockPlayer(GameType.SURVIVAL));
			int produce = dropped(helper, item(vine.produceId));
			helper.assertTrue(produce >= vine.pickMin && produce <= vine.pickMax,
					"Picking " + vine.blockId + " gives " + vine.pickMin + "-" + vine.pickMax + " " + vine.produceId + ", not " + produce);
		}
		helper.succeed();
	}

	/** Peas fix nitrogen: lettuce beside them grows faster than lettuce alone. */
	@GameTest
	public void peasFeedTheirNeighbours(GameTestHelper helper) {
		BlockPos alone = new BlockPos(1, 2, 1);
		BlockPos beside = new BlockPos(5, 2, 5);
		for (BlockPos pos : List.of(alone, beside, beside.east())) {
			farmland(helper, pos.below());
		}
		helper.setBlock(alone, block("lettuce_crop"));
		helper.setBlock(beside, block("lettuce_crop"));
		helper.setBlock(beside.east(), block("pea_crop"));
		helper.assertTrue(CropGrowth.nextToLegume(helper.getLevel(), helper.absolutePos(beside)), "Peas are a legume");
		float lone = CropGrowth.speed(helper.getLevel(), helper.absolutePos(alone), false);
		float fed = CropGrowth.speed(helper.getLevel(), helper.absolutePos(beside), false);
		helper.assertTrue(fed > lone, "Lettuce beside peas grows faster (" + fed + ") than alone (" + lone + ")");
		helper.succeed();
	}

	// ---------------------------------------------------------------- the planter box

	/** Seeds and sprigs plant in a Planter Box, and a crop in one grows as on watered farmland, faster than on dry. */
	@GameTest
	public void planterBoxesGrowCropsAsWateredFarmland(GameTestHelper helper) {
		BlockPos box = new BlockPos(1, 1, 1);
		BlockPos wet = new BlockPos(5, 1, 1);
		BlockPos dry = new BlockPos(1, 1, 5);
		BlockPos herbBox = new BlockPos(5, 1, 5);
		helper.setBlock(box, block("planter_box"));
		helper.setBlock(herbBox, block("planter_box"));
		farmland(helper, wet);
		helper.setBlock(dry, Blocks.FARMLAND.defaultBlockState().setValue(BlockStateProperties.MOISTURE, 0));
		helper.useBlock(box, holding(helper, new ItemStack(item("lettuce_seeds"))), top(helper, box));
		helper.useBlock(herbBox, holding(helper, new ItemStack(item("basil"))), top(helper, herbBox));
		helper.assertBlockPresent(block("lettuce_crop"), box.above());
		helper.assertBlockPresent(crop(TallCrop.BASIL), herbBox.above());
		helper.setBlock(wet.above(), block("lettuce_crop"));
		helper.setBlock(dry.above(), block("lettuce_crop"));
		ServerLevel level = helper.getLevel();
		float inBox = CropGrowth.speed(level, helper.absolutePos(box.above()), false);
		float watered = CropGrowth.speed(level, helper.absolutePos(wet.above()), false);
		float dried = CropGrowth.speed(level, helper.absolutePos(dry.above()), false);
		helper.assertTrue(inBox == watered && inBox > dried, "In a box " + inBox + ", on watered farmland " + watered + ", on dry " + dried);
		helper.succeed();
	}

	// ---------------------------------------------------------------- herbs: bundles and pots

	/**
	 * A herb bundle hangs only from the underside of a block. It dries in time; broken fresh it gives itself, broken dried four
	 * Dried Herbs; and it falls when its block is taken away.
	 */
	@GameTest(maxTicks = 40)
	public void herbBundlesHangAndDry(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos fresh = new BlockPos(1, 2, 1);
		BlockPos drying = new BlockPos(5, 2, 1);
		BlockPos falling = new BlockPos(1, 2, 5);
		BlockPos loose = new BlockPos(5, 2, 5);
		for (BlockPos pos : List.of(fresh, drying, falling)) {
			helper.setBlock(pos.above(), Blocks.OAK_PLANKS);
		}
		Block basil = block("basil_bundle");
		Block mint = block("mint_bundle");
		Block sage = block("sage_bundle");
		helper.assertTrue(!basil.defaultBlockState().canSurvive(level, helper.absolutePos(loose)), "A bundle cannot hang from nothing");
		helper.setBlock(fresh, basil);
		helper.setBlock(drying, mint);
		helper.setBlock(falling, sage);
		helper.assertTrue(basil.defaultBlockState().canSurvive(level, helper.absolutePos(fresh)), "A bundle hangs under a block");
		for (int i = 0; i < 200 && !helper.getBlockState(drying).getValue(HerbBundleBlock.DRIED); i++) {
			helper.getBlockState(drying).randomTick(level, helper.absolutePos(drying), level.getRandom());
		}
		helper.assertTrue(helper.getBlockState(drying).getValue(HerbBundleBlock.DRIED), "A hung bundle dries in time");
		helper.assertTrue(!helper.getBlockState(drying).isRandomlyTicking(), "A dried bundle rests");
		level.destroyBlock(helper.absolutePos(fresh), true);
		level.destroyBlock(helper.absolutePos(drying), true);
		helper.setBlock(falling.above(), Blocks.AIR);
		helper.succeedWhen(() -> {
			helper.assertTrue(dropped(helper, item("basil_bundle")) == 1, "Broken fresh, a bundle gives itself");
			helper.assertTrue(dropped(helper, item("dried_herbs")) == 4 && dropped(helper, item("mint_bundle")) == 0,
					"Broken dried, it gives its four sprigs as Dried Herbs");
			helper.assertBlockNotPresent(sage, falling);
		});
	}

	/** Four sprigs and a string tie a bundle; a sprig on a flower pot pots the herb, which gives back its pot and a sprig. */
	@GameTest(maxTicks = 40)
	public void sprigsAreTiedAndPotted(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		CraftingInput input = CraftingInput.of(3, 2, List.of(new ItemStack(item("mint")), new ItemStack(item("mint")), new ItemStack(item("mint")),
				new ItemStack(item("mint")), new ItemStack(Items.STRING), ItemStack.EMPTY));
		ItemStack tied = level.getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, level)
				.orElseThrow(() -> helper.assertionException("Four mint and a string tie nothing")).value().assemble(input);
		helper.assertTrue(tied.is(item("mint_bundle")) && tied.getCount() == 1, "Four mint and a string tie " + tied);
		BlockPos pot = new BlockPos(2, 2, 2);
		helper.setBlock(pot, Blocks.FLOWER_POT);
		Player player = holding(helper, new ItemStack(item("rosemary"), 2));
		helper.useBlock(pot, player);
		helper.assertBlockPresent(block("potted_rosemary"), pot);
		helper.assertTrue(player.getMainHandItem().getCount() == 1, "Potting uses the sprig");
		level.destroyBlock(helper.absolutePos(pot), true);
		helper.succeedWhen(() -> {
			helper.assertTrue(dropped(helper, Items.FLOWER_POT) == 1 && dropped(helper, item("rosemary")) == 1,
					"A potted herb gives back its pot and its sprig");
		});
	}

	// ---------------------------------------------------------------- spices

	/** An axe strips a cinnamon log, peeling 1-2 Cinnamon off it and wearing the axe; a stripped log gives no more. */
	@GameTest(maxTicks = 40)
	public void axesPeelCinnamonFromItsLogs(GameTestHelper helper) {
		BlockPos log = new BlockPos(2, 2, 2);
		helper.setBlock(log, block("cinnamon_log"));
		Player player = holding(helper, new ItemStack(Items.IRON_AXE));
		helper.useBlock(log, player);
		helper.assertBlockPresent(block("stripped_cinnamon_log"), log);
		helper.assertTrue(player.getMainHandItem().getDamageValue() == 1, "Stripping wears the axe");
		helper.useBlock(log, player);
		helper.succeedWhen(() -> {
			int bark = dropped(helper, item(CinnamonLogBlock.BARK));
			helper.assertTrue(bark >= CinnamonLogBlock.BARK_MIN && bark <= CinnamonLogBlock.BARK_MAX,
					"Stripping peels " + CinnamonLogBlock.BARK_MIN + "-" + CinnamonLogBlock.BARK_MAX + " cinnamon, not " + bark);
		});
	}

	/**
	 * A cinnamon sapling grows into its tree: cinnamon logs under a crown of its own leaves, grown (not persistent). A tree
	 * needs more space above it than the test area has, so the sapling goes on top of whatever closes the area above.
	 */
	@GameTest
	public void cinnamonSaplingsGrowTheirTree(GameTestHelper helper) {
		BlockPos sapling = new BlockPos(3, 2, 3);
		for (int dy = 0; dy <= 24; dy++) {
			if (!helper.getBlockState(sapling.above(dy)).isAir()) {
				sapling = sapling.above(dy + 1);
				break;
			}
		}
		helper.setBlock(sapling.below(), Blocks.DIRT);
		helper.setBlock(sapling, block("cinnamon_sapling"));
		ServerLevel level = helper.getLevel();
		BlockPos absolute = helper.absolutePos(sapling);
		helper.assertTrue(JugcraftAgriculture.CINNAMON_GROWER.growTree(level, level.getChunkSource().getGenerator(), absolute,
				level.getBlockState(absolute), level.getRandom()), "The cinnamon sapling at " + sapling + " grows into a tree");
		helper.assertBlockPresent(block("cinnamon_log"), sapling);
		int leaves = 0;
		for (BlockPos pos : BlockPos.betweenClosed(absolute.offset(-3, 0, -3), absolute.offset(3, 10, 3))) {
			BlockState state = level.getBlockState(pos);
			if (state.is(block("cinnamon_leaves"))) {
				leaves++;
				helper.assertTrue(!state.getValue(LeavesBlock.PERSISTENT), "A grown tree's leaves are not persistent");
			}
		}
		helper.assertTrue(leaves >= 12, "Expected a crown of cinnamon leaves, found " + leaves);
		ResourceKey<PlacedFeature> patch = ResourceKey.create(Registries.PLACED_FEATURE, Jugcraft.id("patch_cinnamon_tree"));
		helper.assertTrue(level.registryAccess().lookupOrThrow(Registries.PLACED_FEATURE).get(patch).isPresent(), "The cinnamon tree grows wild");
		helper.succeed();
	}

	/**
	 * A Spice Rack takes spices, one at a time, up to its slots, and nothing else; a comparator reads it full; an empty hand
	 * takes the last back; broken, it spills the rest.
	 */
	@GameTest(maxTicks = 40)
	public void spiceRacksHoldSpices(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos rack = new BlockPos(2, 2, 2);
		helper.setBlock(rack, block("spice_rack"));
		Player player = holding(helper, new ItemStack(Items.BREAD));
		helper.useBlock(rack, player);
		helper.assertTrue(player.getMainHandItem().getCount() == 1, "Bread is no spice");
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item("paprika"), 16));
		for (int i = 0; i < SpiceRackBlockEntity.SLOTS + 1; i++) {
			helper.useBlock(rack, player);
		}
		SpiceRackBlockEntity entity = (SpiceRackBlockEntity) level.getBlockEntity(helper.absolutePos(rack));
		helper.assertTrue(entity != null && entity.count() == SpiceRackBlockEntity.SLOTS, "The rack holds " + SpiceRackBlockEntity.SLOTS);
		helper.assertTrue(player.getMainHandItem().getCount() == 16 - SpiceRackBlockEntity.SLOTS, "A full rack takes no more");
		BlockPos absolute = helper.absolutePos(rack);
		helper.assertTrue(level.getBlockState(absolute).getAnalogOutputSignal(level, absolute, Direction.NORTH) == 15, "A full rack reads 15");
		Player empty = helper.makeMockPlayer(GameType.SURVIVAL);
		helper.useBlock(rack, empty);
		helper.assertTrue(entity.count() == SpiceRackBlockEntity.SLOTS - 1 && carried(empty, item("paprika")) == 1,
				"An empty hand takes the last spice back");
		level.destroyBlock(absolute, true);
		helper.succeedWhen(() -> helper.assertTrue(dropped(helper, item("paprika")) == SpiceRackBlockEntity.SLOTS - 1,
				"A broken rack spills its spices"));
	}

	/** The Mortar and Pestle grinds a Dried Chili in the other hand into Paprika; a chili dries from a pepper. */
	@GameTest
	public void mortarsGrindChiliIntoPaprika(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		SingleRecipeInput pepper = new SingleRecipeInput(new ItemStack(item("pepper")));
		ItemStack dried = level.getServer().getRecipeManager().getRecipeFor(RecipeType.SMOKING, pepper, level)
				.orElseThrow(() -> helper.assertionException("A smoker dries no pepper")).value().assemble(pepper);
		helper.assertTrue(dried.is(item(SpiceGrinding.INPUT)), "A pepper dries into " + dried);
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setGameMode(GameType.SURVIVAL);
		RateGate.forget(player.getUUID());
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(JugcraftConcordance.MORTAR));
		player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(item(SpiceGrinding.INPUT), 2));
		JugcraftConcordance.MORTAR.use(level, player, InteractionHand.MAIN_HAND);
		helper.assertTrue(player.getOffhandItem().getCount() == 1, "Grinding uses one chili");
		helper.assertTrue(carried(player, item(SpiceGrinding.RESULT)) == SpiceGrinding.COUNT, "A chili grinds into " + SpiceGrinding.COUNT + " paprika");
		helper.assertTrue(player.getMainHandItem().is(JugcraftConcordance.MORTAR), "The mortar is kept");
		helper.succeed();
	}

	// ---------------------------------------------------------------- dishes and the wild

	/** The new dishes cook in the Cooking Pot (and the menu's ratatouille from eggplant and zucchini); eggplant roasts. */
	@GameTest
	public void dishesCook(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		Map<String, List<ItemStack>> dishes = Map.of(
				"pea_soup", List.of(new ItemStack(Items.BOWL), new ItemStack(item("peas"), 2), new ItemStack(item("onion")), new ItemStack(Items.CARROT)),
				"ratatouille", List.of(new ItemStack(Items.BOWL), new ItemStack(item("tomato")), new ItemStack(item("pepper")),
						new ItemStack(item("onion")), new ItemStack(item("eggplant")), new ItemStack(item("zucchini"))),
				"pesto_pasta", List.of(new ItemStack(Items.BOWL), new ItemStack(item("raw_pasta")), new ItemStack(item("basil"), 2),
						new ItemStack(item("garlic")), new ItemStack(item("tomato"))),
				"mint_tea", List.of(new ItemStack(Items.GLASS_BOTTLE), new ItemStack(item("mint"), 2), new ItemStack(Items.SUGAR)),
				"chicken_curry", List.of(new ItemStack(Items.BOWL), new ItemStack(item("rice")), new ItemStack(Items.CHICKEN),
						new ItemStack(item("ginger")), new ItemStack(item("paprika")), new ItemStack(item("onion"))),
				"saffron_rice", List.of(new ItemStack(Items.BOWL), new ItemStack(item("rice"), 2), new ItemStack(item("saffron")), new ItemStack(item("onion"))),
				"vanilla_custard", List.of(new ItemStack(Items.BOWL), new ItemStack(Items.EGG), new ItemStack(Items.SUGAR),
						new ItemStack(item("vanilla_pods")), new ItemStack(item("milk_bottle"))),
				"dill_pickles", List.of(new ItemStack(item("mason_jar")), new ItemStack(item("cucumber"), 4), new ItemStack(item("dill")),
						new ItemStack(item("mustard_seeds")), new ItemStack(item("cider_vinegar"))));
		for (Map.Entry<String, List<ItemStack>> dish : dishes.entrySet()) {
			List<ItemStack> slots = new ArrayList<>(dish.getValue());
			while (slots.size() < CookingPotBlockEntity.INPUTS) {
				slots.add(ItemStack.EMPTY);
			}
			Optional<CookingPotRecipe.Match> match = CookingPotRecipe.find(level.getServer(), slots);
			helper.assertTrue(match.isPresent() && match.get().recipe().output().create().is(item(dish.getKey())),
					"The Cooking Pot makes " + dish.getKey() + " from " + dish.getValue());
		}
		SingleRecipeInput eggplant = new SingleRecipeInput(new ItemStack(item("eggplant")));
		ItemStack roasted = level.getServer().getRecipeManager().getRecipeFor(RecipeType.SMELTING, eggplant, level)
				.orElseThrow(() -> helper.assertionException("An eggplant roasts into nothing")).value().assemble(eggplant);
		helper.assertTrue(roasted.is(item("roasted_eggplant")) && roasted.get(DataComponents.FOOD) != null, "An eggplant roasts into " + roasted);
		helper.succeed();
	}

	/** Every new wild plant, broken by hand, gives 1-2 of its crop's seed, and grows wild (its patch loads). */
	@GameTest(maxTicks = 40)
	public void wildPlantsGiveSeeds(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		var placed = level.registryAccess().lookupOrThrow(Registries.PLACED_FEATURE);
		for (int i = 0; i < WILD.length; i++) {
			BlockPos pos = new BlockPos(i % 4 * 2, 2, i / 4);
			helper.setBlock(pos.below(), Blocks.GRASS_BLOCK);
			helper.setBlock(pos, block(WILD[i][0]));
			level.destroyBlock(helper.absolutePos(pos), true);
			ResourceKey<PlacedFeature> patch = ResourceKey.create(Registries.PLACED_FEATURE, Jugcraft.id("patch_" + WILD[i][0]));
			helper.assertTrue(placed.get(patch).isPresent(), WILD[i][0] + " grows wild (patch_" + WILD[i][0] + ")");
		}
		helper.succeedWhen(() -> {
			for (String[] wild : WILD) {
				int seeds = dropped(helper, item(wild[1]));
				helper.assertTrue(seeds >= 1 && seeds <= 2, wild[0] + " gives 1-2 " + wild[1] + ", not " + seeds);
			}
		});
	}
}
