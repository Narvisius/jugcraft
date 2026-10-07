package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.chemistry.FluidRecipe;
import io.github.jimbozoomer.jugcraft.chemistry.FluidRecipes;
import io.github.jimbozoomer.jugcraft.chemistry.PetroFluids;
import io.github.jimbozoomer.jugcraft.config.FeatureEnabledCondition;
import io.github.jimbozoomer.jugcraft.config.JugcraftConfig;
import io.github.jimbozoomer.jugcraft.energy.EnergyStorage;
import io.github.jimbozoomer.jugcraft.energy.SimpleEnergyStorage;
import io.github.jimbozoomer.jugcraft.machine.JugcraftMachines;
import io.github.jimbozoomer.jugcraft.machine.MachineBlockEntity;
import io.github.jimbozoomer.jugcraft.machine.MachineKind;
import io.github.jimbozoomer.jugcraft.machine.MachineRecipe;
import io.github.jimbozoomer.jugcraft.machine.MachineRecipes;
import io.github.jimbozoomer.jugcraft.materials.JugcraftMaterials;
import io.github.jimbozoomer.jugcraft.materials.LoreItem;
import io.github.jimbozoomer.jugcraft.prospecting.OreSurvey;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

/**
 * Thallite, slice 1 (docs/features/thallite.md): the ore and its material forms, its smelting and 9-to-1 recipes, its
 * processing through the shared machines (crusher and pulverizer 2 a block with a 10% iron dust byproduct, ore washer 3,
 * acid leaching 4, the Metal Press and the dearer hand-made plate), its worldgen (veins everywhere and rich pockets in
 * Lush Caves and the Glowcap Grotto) and its feature switch. {@code ThalliteClientGameTests} shows it in game.
 */
public class ThalliteGameTests {
	private static final List<String> BLOCKS = List.of("thallite_ore", "deepslate_thallite_ore", "raw_thallite_block", "thallite_block");
	private static final List<String> ITEMS = List.of("raw_thallite", "thallite_ingot", "thallite_nugget", "thallite_dust",
			"thallite_plate", "washed_thallite_ore");
	private static final ResourceKey<PlacedFeature> VEINS = ResourceKey.create(Registries.PLACED_FEATURE, Jugcraft.id("ore_thallite"));
	private static final ResourceKey<PlacedFeature> RICH = ResourceKey.create(Registries.PLACED_FEATURE, Jugcraft.id("ore_thallite_rich"));
	private static final TagKey<Biome> RICH_BIOMES = TagKey.create(Registries.BIOME, Jugcraft.id("has_ore/thallite_rich"));
	private static final ResourceKey<Biome> GLOWCAP_GROTTO = ResourceKey.create(Registries.BIOME, Jugcraft.id("glowcap_grotto"));

	private static Item item(String path) {
		return BuiltInRegistries.ITEM.getValue(Jugcraft.id(path));
	}

	private static Block block(String path) {
		return BuiltInRegistries.BLOCK.getValue(Jugcraft.id(path));
	}

	private static TagKey<Block> blockTag(String namespace, String path) {
		return TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(namespace, path));
	}

	private static TagKey<Item> itemTag(String path) {
		return TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("c", path));
	}

	/**
	 * Every block and item is registered, the ingot carries its lore line, the ores are in their c: tags (so the Ore Drill
	 * takes them and the Prospector hears them) and need a stone pickaxe, like tin, zinc and lead.
	 */
	@GameTest
	public void thalliteIsRegistered(GameTestHelper helper) {
		for (String path : BLOCKS) {
			helper.assertTrue(BuiltInRegistries.BLOCK.containsKey(Jugcraft.id(path)), "No block " + path);
			helper.assertTrue(BuiltInRegistries.ITEM.containsKey(Jugcraft.id(path)), "No block item " + path);
		}
		for (String path : ITEMS) {
			helper.assertTrue(BuiltInRegistries.ITEM.containsKey(Jugcraft.id(path)), "No item " + path);
		}
		helper.assertTrue(JugcraftMaterials.THALLITE.ingot == item("thallite_ingot") && JugcraftMaterials.THALLITE.ore == block("thallite_ore"),
				"JugcraftMaterials.THALLITE is not the registered thallite");
		helper.assertTrue(item("thallite_ingot") instanceof LoreItem, "The thallite ingot has no lore line");
		helper.assertTrue(!(item("thallite_nugget") instanceof LoreItem), "Only the ingot carries the lore line");
		for (String ore : List.of("thallite_ore", "deepslate_thallite_ore")) {
			BlockState state = block(ore).defaultBlockState();
			helper.assertTrue(state.is(blockTag("c", "ores/thallite")) && state.is(blockTag("c", "ores")), ore + " is not in #c:ores/thallite");
			helper.assertTrue(new ItemStack(Items.STONE_PICKAXE).isCorrectToolForDrops(state), "A stone pickaxe cannot mine " + ore);
			helper.assertTrue(!new ItemStack(Items.WOODEN_PICKAXE).isCorrectToolForDrops(state), "A wooden pickaxe mines " + ore);
		}
		helper.assertTrue(block("deepslate_thallite_ore").defaultBlockState().is(blockTag("c", "ores_in_ground/deepslate")),
				"Deepslate thallite ore is not in #c:ores_in_ground/deepslate");
		helper.assertTrue(new ItemStack(item("thallite_ingot")).is(itemTag("ingots/thallite")), "No #c:ingots/thallite");
		helper.assertTrue(new ItemStack(item("raw_thallite")).is(itemTag("raw_materials/thallite")), "No #c:raw_materials/thallite");
		helper.assertTrue(new ItemStack(item("thallite_plate")).is(itemTag("plates/thallite")), "No #c:plates/thallite");
		helper.assertTrue(new ItemStack(item("thallite_dust")).is(itemTag("dusts/thallite")), "No #c:dusts/thallite");
		helper.assertTrue(OreSurvey.FAMILIES.stream().anyMatch(family -> family.name().equals("thallite")
				&& family.icon().equals(Jugcraft.id("thallite_ore"))), "The Prospector does not listen for thallite");
		helper.succeed();
	}

	/** Each ore drops one raw thallite to a stone pickaxe, itself to Silk Touch, and up to four to Fortune III. */
	@GameTest
	public void thalliteOreDropsRawThallite(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
		ItemStack silk = new ItemStack(Items.IRON_PICKAXE);
		silk.enchant(level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SILK_TOUCH), 1);
		ItemStack fortune = new ItemStack(Items.IRON_PICKAXE);
		fortune.enchant(level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.FORTUNE), 3);
		for (String ore : List.of("thallite_ore", "deepslate_thallite_ore")) {
			BlockState state = block(ore).defaultBlockState();
			List<ItemStack> plain = Block.getDrops(state, level, pos, null, null, new ItemStack(Items.STONE_PICKAXE));
			helper.assertTrue(plain.size() == 1 && plain.get(0).is(item("raw_thallite")) && plain.get(0).getCount() == 1,
					ore + " gave a stone pickaxe " + plain);
			List<ItemStack> silkDrops = Block.getDrops(state, level, pos, null, null, silk);
			helper.assertTrue(silkDrops.size() == 1 && silkDrops.get(0).is(item(ore)), ore + " gave Silk Touch " + silkDrops);
			// Vanilla's ore formula: Fortune III gives 1 to 4; 64 breaks all giving one has odds of 0.4^64.
			int most = 0;
			for (int i = 0; i < 64; i++) {
				List<ItemStack> drops = Block.getDrops(state, level, pos, null, null, fortune);
				helper.assertTrue(drops.size() == 1 && drops.get(0).is(item("raw_thallite")) && drops.get(0).getCount() <= 4,
						ore + " gave Fortune III " + drops);
				most = Math.max(most, drops.get(0).getCount());
			}
			helper.assertTrue(most > 1, ore + ": Fortune III never gave more than one raw thallite");
		}
		helper.succeed();
	}

	/**
	 * Raw thallite and its ores smelt in a furnace and a blast furnace into an ingot; nuggets, ingots and blocks, and raw
	 * thallite and raw blocks, convert 9 to 1 both ways; dust smelts back into an ingot.
	 */
	@GameTest
	public void thalliteSmeltsAndCompacts(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		var recipes = level.getServer().getRecipeManager();
		for (String input : List.of("raw_thallite", "thallite_ore", "deepslate_thallite_ore", "thallite_dust")) {
			SingleRecipeInput single = new SingleRecipeInput(new ItemStack(item(input)));
			ItemStack smelted = recipes.getRecipeFor(RecipeType.SMELTING, single, level)
					.orElseThrow(() -> helper.assertionException("A furnace does not smelt " + input)).value().assemble(single);
			helper.assertTrue(smelted.is(item("thallite_ingot")) && smelted.getCount() == 1, "A furnace made " + smelted + " from " + input);
			ItemStack blasted = recipes.getRecipeFor(RecipeType.BLASTING, single, level)
					.orElseThrow(() -> helper.assertionException("A blast furnace does not smelt " + input)).value().assemble(single);
			helper.assertTrue(blasted.is(item("thallite_ingot")) && blasted.getCount() == 1, "A blast furnace made " + blasted + " from " + input);
		}
		// Nine small make one big, and one big makes nine small.
		String[][] pairs = {{"thallite_nugget", "thallite_ingot"}, {"thallite_ingot", "thallite_block"}, {"raw_thallite", "raw_thallite_block"}};
		for (String[] pair : pairs) {
			List<ItemStack> nine = new ArrayList<>();
			for (int i = 0; i < 9; i++) {
				nine.add(new ItemStack(item(pair[0])));
			}
			CraftingInput full = CraftingInput.of(3, 3, nine);
			ItemStack big = recipes.getRecipeFor(RecipeType.CRAFTING, full, level)
					.orElseThrow(() -> helper.assertionException("Nine " + pair[0] + " craft nothing")).value().assemble(full);
			helper.assertTrue(big.is(item(pair[1])) && big.getCount() == 1, "Nine " + pair[0] + " made " + big);
			CraftingInput one = CraftingInput.of(1, 1, List.of(new ItemStack(item(pair[1]))));
			ItemStack small = recipes.getRecipeFor(RecipeType.CRAFTING, one, level)
					.orElseThrow(() -> helper.assertionException("One " + pair[1] + " crafts nothing")).value().assemble(one);
			helper.assertTrue(small.is(item(pair[0])) && small.getCount() == 9, "One " + pair[1] + " made " + small);
		}
		helper.succeed();
	}

	/**
	 * The shared ore processing (tools/machines.py): crusher 2 raw a block; pulverizer 2 dusts a block with a 10% iron dust
	 * byproduct, and 1 dust from washed ore, raw thallite or an ingot; ore washer 3 washed ores; acid leaching 4; the
	 * Metal Press 1 plate an ingot; by hand, 2 ingots a plate.
	 */
	@GameTest
	public void thalliteProcessingYields(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		for (String ore : List.of("thallite_ore", "deepslate_thallite_ore")) {
			ItemStack crushed = recipe(helper, MachineKind.CRUSHER, ore).output().create();
			helper.assertTrue(crushed.is(item("raw_thallite")) && crushed.getCount() == 2, "The crusher makes " + crushed + " from " + ore);
			MachineRecipe pulverizing = recipe(helper, MachineKind.PULVERIZER, ore);
			ItemStack dust = pulverizing.output().create();
			helper.assertTrue(dust.is(item("thallite_dust")) && dust.getCount() == 2, "The pulverizer makes " + dust + " from " + ore);
			helper.assertTrue(pulverizing.byproducts().size() == 1, "Expected one byproduct, got " + pulverizing.byproducts());
			MachineRecipe.Byproduct iron = pulverizing.byproducts().getFirst();
			helper.assertTrue(iron.result().create().is(item("iron_dust")) && Math.abs(iron.chance() - 0.1F) < 1.0E-6F,
					"The pulverizer's byproduct from " + ore + " is " + iron);
			ItemStack washed = recipe(helper, MachineKind.ORE_WASHER, ore).output().create();
			helper.assertTrue(washed.is(item("washed_thallite_ore")) && washed.getCount() == 3, "The ore washer makes " + washed + " from " + ore);
			// Acid leaching in the chemical reactor: 250 mB of sulfuric acid and the ore give four washed ores.
			List<FluidRecipe> leaching = FluidRecipes.recipes(level.getServer(), MachineKind.CHEMICAL_REACTOR).stream()
					.filter(r -> r.itemsMatch(List.of(new ItemStack(item(ore))))).toList();
			helper.assertTrue(leaching.size() == 1, "Expected one leaching recipe for " + ore + ", got " + leaching.size());
			FluidRecipe leach = leaching.getFirst();
			ItemStack leached = leach.results().getFirst().create();
			helper.assertTrue(leached.is(item("washed_thallite_ore")) && leached.getCount() == 4, "Leaching makes " + leached + " from " + ore);
			helper.assertTrue(leach.fluids().size() == 1 && leach.fluids().getFirst().fluid() == PetroFluids.SULFURIC_ACID.source()
					&& leach.fluids().getFirst().amount() == 250, "Leaching " + ore + " takes " + leach.fluids());
		}
		for (String input : List.of("washed_thallite_ore", "raw_thallite", "thallite_ingot")) {
			ItemStack dust = recipe(helper, MachineKind.PULVERIZER, input).output().create();
			helper.assertTrue(dust.is(item("thallite_dust")) && dust.getCount() == 1, "The pulverizer makes " + dust + " from " + input);
		}
		ItemStack pressed = recipe(helper, MachineKind.METAL_PRESS, "thallite_ingot").output().create();
		helper.assertTrue(pressed.is(item("thallite_plate")) && pressed.getCount() == 1, "The Metal Press makes " + pressed);
		ItemStack ingot = new ItemStack(item("thallite_ingot"));
		CraftingInput column = CraftingInput.of(1, 2, List.of(ingot.copy(), ingot.copy()));
		ItemStack plate = level.getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, column, level)
				.orElseThrow(() -> helper.assertionException("Two thallite ingots in a column craft nothing")).value().assemble(column);
		helper.assertTrue(plate.is(item("thallite_plate")) && plate.getCount() == 1, "Two ingots by hand made " + plate);
		helper.assertTrue(level.recipeAccess().byKey(ResourceKey.create(Registries.RECIPE, Jugcraft.id("thallite_plate_by_hand"))).isPresent(),
				"The hand-made plate recipe does not load");
		helper.succeed();
	}

	private static MachineRecipe recipe(GameTestHelper helper, MachineKind kind, String input) {
		return MachineRecipes.find(helper.getLevel(), kind, new ItemStack(item(input)))
				.orElseThrow(() -> helper.assertionException("No " + kind + " recipe for " + input));
	}

	/** A powered pulverizer grinds one thallite ore into two thallite dusts. */
	@GameTest(maxTicks = 400)
	public void pulverizerGrindsThalliteOre(GameTestHelper helper) {
		BlockPos pos = new BlockPos(2, 1, 2);
		helper.setBlock(pos, JugcraftMachines.MACHINES.get(MachineKind.PULVERIZER).defaultBlockState());
		EnergyStorage storage = EnergyStorage.SIDED.find(helper.getLevel(), helper.absolutePos(pos), Direction.UP);
		helper.assertTrue(storage instanceof SimpleEnergyStorage, "The pulverizer has no energy storage");
		((SimpleEnergyStorage) storage).setAmount(storage.getCapacity());
		MachineBlockEntity pulverizer = helper.getBlockEntity(pos, MachineBlockEntity.class);
		pulverizer.setItem(0, new ItemStack(item("thallite_ore")));
		helper.succeedWhen(() -> {
			ItemStack output = pulverizer.getItem(MachineKind.PULVERIZER.outputSlot());
			helper.assertTrue(output.is(item("thallite_dust")) && output.getCount() == 2, "Pulverizer output is " + output);
		});
	}

	/**
	 * The worldgen: both placed features load; the rich pockets' biome tag holds Lush Caves and the Glowcap Grotto and
	 * nothing else, so Lush Caves keep them with the biomes switch off; Lush Caves carry the rich veins among their ores
	 * and a plains biome does not.
	 */
	@GameTest
	public void thalliteWorldgenFeatures(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		Registry<PlacedFeature> placed = level.registryAccess().lookupOrThrow(Registries.PLACED_FEATURE);
		helper.assertTrue(placed.get(VEINS).isPresent(), "No placed feature ore_thallite");
		helper.assertTrue(placed.get(RICH).isPresent(), "No placed feature ore_thallite_rich");
		Registry<Biome> biomes = level.registryAccess().lookupOrThrow(Registries.BIOME);
		Holder<Biome> lush = biomes.getOrThrow(Biomes.LUSH_CAVES);
		Holder<Biome> grotto = biomes.getOrThrow(GLOWCAP_GROTTO);
		Holder<Biome> plains = biomes.getOrThrow(Biomes.PLAINS);
		Holder<Biome> dripstone = biomes.getOrThrow(Biomes.DRIPSTONE_CAVES);
		helper.assertTrue(lush.is(RICH_BIOMES) && grotto.is(RICH_BIOMES), "Lush Caves and the Glowcap Grotto are not both in #jugcraft:has_ore/thallite_rich");
		helper.assertTrue(!plains.is(RICH_BIOMES) && !dripstone.is(RICH_BIOMES), "Plains or dripstone caves are in #jugcraft:has_ore/thallite_rich");
		helper.assertTrue(hasOre(lush, RICH), "Lush Caves do not carry thallite's rich veins");
		helper.assertTrue(!hasOre(plains, RICH), "Plains carry thallite's rich veins");
		helper.succeed();
	}

	/** Whether {@code biome} lists {@code feature} in its underground ores step (after Fabric's biome modifications). */
	private static boolean hasOre(Holder<Biome> biome, ResourceKey<PlacedFeature> feature) {
		List<HolderSet<PlacedFeature>> steps = biome.value().getGenerationSettings().features();
		int step = GenerationStep.Decoration.UNDERGROUND_ORES.ordinal();
		if (step >= steps.size()) {
			return false;
		}
		for (Holder<PlacedFeature> holder : steps.get(step)) {
			if (holder.is(feature)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Both veins, placed straight into solid rock (without their placement rules), make thallite ore in stone and deepslate
	 * thallite ore in deepslate.
	 */
	@GameTest
	public void thalliteVeinsPlaceOre(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		Registry<PlacedFeature> placed = level.registryAccess().lookupOrThrow(Registries.PLACED_FEATURE);
		for (ResourceKey<PlacedFeature> key : List.of(VEINS, RICH)) {
			PlacedFeature vein = placed.getOrThrow(key).value();
			for (Block rock : List.of(Blocks.STONE, Blocks.DEEPSLATE)) {
				Block ore = rock == Blocks.STONE ? block("thallite_ore") : block("deepslate_thallite_ore");
				int found = 0;
				for (int seed = 1; seed <= 4 && found == 0; seed++) {
					fill(helper, rock);
					new PlacedFeature(vein.feature(), List.of()).place(level, level.getChunkSource().getGenerator(),
							RandomSource.create(seed), helper.absolutePos(new BlockPos(4, 4, 4)));
					found = count(helper, ore);
				}
				helper.assertTrue(found > 0, key.identifier() + " placed no " + BuiltInRegistries.BLOCK.getKey(ore) + " in " + rock);
			}
		}
		helper.succeed();
	}

	private static void fill(GameTestHelper helper, Block rock) {
		for (int x = 0; x < 8; x++) {
			for (int y = 1; y < 8; y++) {
				for (int z = 0; z < 8; z++) {
					helper.setBlock(new BlockPos(x, y, z), rock);
				}
			}
		}
	}

	private static int count(GameTestHelper helper, Block block) {
		int found = 0;
		for (int x = 0; x < 8; x++) {
			for (int y = 1; y < 8; y++) {
				for (int z = 0; z < 8; z++) {
					found += helper.getBlockState(new BlockPos(x, y, z)).is(block) ? 1 : 0;
				}
			}
		}
		return found;
	}

	/**
	 * The feature switch: "thallite" is a switch, on by default, and its load condition follows it, so with it on every
	 * thallite recipe loads. (Turning it off needs a restart with config/jugcraft.properties changed, which a game test
	 * cannot do; registrations never depend on it, as every block and item above is registered whatever the switch.)
	 */
	@GameTest
	public void thalliteHasItsOwnSwitch(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		helper.assertTrue(JugcraftConfig.FEATURES.contains("thallite"), "No thallite feature switch");
		helper.assertTrue(JugcraftConfig.isFeatureEnabled("thallite"), "The thallite switch is off by default");
		helper.assertTrue(new FeatureEnabledCondition("thallite").test(null) == JugcraftConfig.isFeatureEnabled("thallite"),
				"jugcraft:feature_enabled does not follow the thallite switch");
		List<String> recipes = List.of("thallite_block", "thallite_ingot_from_thallite_block", "thallite_ingot_from_nuggets",
				"thallite_nugget", "raw_thallite_block", "raw_thallite_from_raw_thallite_block",
				"thallite_ingot_from_smelting_raw_thallite", "thallite_ingot_from_blasting_raw_thallite",
				"thallite_ingot_from_smelting_thallite_ore", "thallite_ingot_from_blasting_thallite_ore",
				"thallite_ingot_from_smelting_thallite_dust", "thallite_ingot_from_blasting_thallite_dust", "thallite_plate_by_hand",
				"crushing/thallite_ore", "pulverizing/thallite_ore", "ore_washing/thallite_ore", "chemical_reaction/thallite_ore",
				"pressing/thallite_ingot");
		for (String id : recipes) {
			helper.assertTrue(level.recipeAccess().byKey(ResourceKey.create(Registries.RECIPE, Jugcraft.id(id))).isPresent(), "Recipe " + id + " does not load");
		}
		helper.succeed();
	}
}
