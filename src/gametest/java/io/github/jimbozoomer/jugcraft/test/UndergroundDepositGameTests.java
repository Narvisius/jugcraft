package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.deposit.*;
import io.github.jimbozoomer.jugcraft.energy.SimpleEnergyStorage;
import io.github.jimbozoomer.jugcraft.machine.*;
import io.github.jimbozoomer.jugcraft.prospecting.OreSurvey;
import java.util.*;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.storage.TagValueInput;

public class UndergroundDepositGameTests {
	private static final String ARENA = "jugcraft-test:arms_arena";

	@GameTest(structure = ARENA) public void undergroundDepositTallAssemblyPlacesAndBreaksAsOneUpgrade(GameTestHelper h) {
		var drill = rig(h, false); var level = h.getLevel(); var pos = drill.getBlockPos();
		var mast = DeepDrillingAssemblyBlock.mount(pos, drill.getBlockState());
		var block = JugcraftDeposits.DEEP_DRILLING_ASSEMBLY;
		var player = h.makeMockServerPlayerInLevel();
		player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
		player.snapTo(mast.getX() + 2, mast.getY(), mast.getZ(), 0, 0);
		ItemStack stack = new ItemStack(block.asItem(), 3);
		level.setBlock(mast.above(), Blocks.STONE.defaultBlockState(), 3);
		DeepDrillingAssemblyBlock.install(stack, level, pos, drill.getBlockState(), player);
		h.assertTrue(stack.getCount() == 3 && level.isEmptyBlock(mast) && level.getBlockState(mast.above()).is(Blocks.STONE),
				"Upper obstruction must reject the entire upgrade without consuming it");
		level.removeBlock(mast.above(), false);
		for (boolean breakUpper : new boolean[]{true, false}) {
			DeepDrillingAssemblyBlock.install(stack, level, pos, drill.getBlockState(), player);
			h.assertTrue(DeepDrillingAssemblyBlock.installed(level, pos, drill.getBlockState()), "Two-part mast not installed");
			h.assertTrue(level.getBlockState(mast.above()).getValue(DeepDrillingAssemblyBlock.HALF)
					== net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER, "Missing physical upper mast");
			var broken = breakUpper ? mast.above() : mast;
			level.destroyBlock(broken, true);
			h.assertTrue(level.isEmptyBlock(mast) && level.isEmptyBlock(mast.above()), "Breaking either half must remove both");
			var drops = level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
					new net.minecraft.world.phys.AABB(mast).inflate(3), e -> e.getItem().is(block.asItem()));
			h.assertTrue(drops.stream().mapToInt(e -> e.getItem().getCount()).sum() == 1, "Must drop one assembly, not two");
			drops.forEach(net.minecraft.world.entity.Entity::discard);
		}
		h.assertTrue(stack.getCount() == 1, "Each placement must consume one assembly");
		player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, stack);
		stack.useOn(new net.minecraft.world.item.context.UseOnContext(player, net.minecraft.world.InteractionHand.MAIN_HAND,
				new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(mast.below()),
						Direction.UP, mast.below(), false)));
		h.assertTrue(stack.isEmpty() && DeepDrillingAssemblyBlock.installed(level, pos, drill.getBlockState()),
				"Ordinary block-item placement must create both halves and consume one assembly");
		player.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
		player.gameMode.destroyBlock(mast.above());
		h.assertTrue(level.isEmptyBlock(mast) && level.isEmptyBlock(mast.above()), "Creative break must remove both halves");
		h.assertTrue(level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
				new net.minecraft.world.phys.AABB(mast).inflate(3), e -> e.getItem().is(block.asItem())).isEmpty(),
				"Creative removal must not drop an assembly");
		h.succeed();
	}

	@GameTest public void undergroundDepositShapesAreBoundedConnectedVariedAndRepeatable(GameTestHelper h) {
		for (var family : UndergroundDepositFeature.Family.values()) {
			Set<List<BlockPos>> variants = new HashSet<>();
			long heights = 0;
			for (int seed = 0; seed < 160; seed++) {
				List<BlockPos> plan = UndergroundDepositFeature.plan(family, RandomSource.create(seed), BlockPos.ZERO);
				h.assertTrue(plan.equals(UndergroundDepositFeature.plan(family, RandomSource.create(seed), BlockPos.ZERO)), "Seed changed " + family);
				h.assertTrue(plan.size() >= family.minBlocks && plan.size() <= family.maxBlocks, "Reserve budget " + family);
				Set<BlockPos> reached = new HashSet<>(); ArrayDeque<BlockPos> queue = new ArrayDeque<>(); queue.add(plan.getFirst());
				Set<BlockPos> all = new HashSet<>(plan);
				while (!queue.isEmpty()) {
					BlockPos p = queue.remove();
					if (!reached.add(p)) continue;
					for (Direction d : Direction.values()) if (all.contains(p.relative(d))) queue.add(p.relative(d));
				}
				h.assertTrue(reached.size() == plan.size(), "Disconnected core " + family);
				int dx = span(plan, 0), dy = span(plan, 1), dz = span(plan, 2);
				h.assertTrue(Math.max(dx, dz) <= family.maxLength && Math.min(dx, dz) <= family.maxWidth && dy <= family.maxHeight, "Envelope " + family);
				variants.add(plan);
				int y = family.centerY(RandomSource.create(seed * 991L));
				h.assertTrue(y >= family.minY && y <= family.maxY, "Depth range " + family);
				heights += y;
			}
			h.assertTrue(variants.size() > 140, "Insufficient shape variety " + family);
			double expected = (family.minY + family.peakY + family.maxY) / 3.0;
			h.assertTrue(Math.abs(heights / 160.0 - expected) < 6, "Depth distribution " + family);
		}
		h.succeed();
	}

	private static int span(List<BlockPos> positions, int axis) {
		return positions.stream().mapToInt(p -> axis == 0 ? p.getX() : axis == 1 ? p.getY() : p.getZ()).max().orElseThrow()
				- positions.stream().mapToInt(p -> axis == 0 ? p.getX() : axis == 1 ? p.getY() : p.getZ()).min().orElseThrow() + 1;
	}

	@GameTest(structure = ARENA) public void undergroundDepositBasicReachAndFamilyGrouping(GameTestHelper h) {
		var drill = rig(h, false);
		var level = h.getLevel(); var center = DeepDrillingAssemblyBlock.center(drill.getBlockPos(), drill.getBlockState());
		put(level, center.below(), "coal"); put(level, center.offset(2, -3, 2), "deepslate_coal");
		put(level, center.offset(-2, -3, -2), "iron"); put(level, center.offset(3, -1, 0), "copper");
		put(level, center.below(4), "tin");
		var found = drill.findDeposits(level, drill.getBlockPos(), drill.getBlockState());
		h.assertTrue(found.size() == 2 && found.contains(center.below()) && found.contains(center.offset(-2, -3, -2)), "Basic 5x5x3 reach / family grouping");
		for (var block : JugcraftDeposits.BLOCKS.values()) {
			h.assertTrue(block.defaultBlockState().getPistonPushReaction() == PushReaction.IMMOVEABLE, "Piston reserve reset");
			h.assertTrue(!OreDrilling.isOre(block.defaultBlockState()), "Bulk block tagged as ordinary ore");
			h.assertTrue(Block.getDrops(block.defaultBlockState(), level, center, null).isEmpty(), "Mineable reserve block");
		}
		var mastDrops = Block.getDrops(JugcraftDeposits.DEEP_DRILLING_ASSEMBLY.defaultBlockState(), level, center, null);
		h.assertTrue(mastDrops.size() == 1 && mastDrops.getFirst().is(JugcraftDeposits.DEEP_DRILLING_ASSEMBLY.asItem())
				&& mastDrops.getFirst().getCount() == 1, "Mast must drop exactly one reusable assembly");
		h.succeed();
	}

	@GameTest(structure = ARENA) public void undergroundDepositBoringAndExtractionSurviveReloadAndPowerLoss(GameTestHelper h) {
		var drill = rig(h, true); var level = h.getLevel();
		var source = DeepDrillingAssemblyBlock.center(drill.getBlockPos(), drill.getBlockState()).below(2);
		put(level, source, "deepslate_iron"); put(level, source.east(), "iron");
		tick(level, drill, 7, true);
		h.assertTrue(drill.depositDepth() == 0 && drill.depositBoreProgress() == 7, "One-second boring step");
		reload(level, drill); tick(level, drill, 10, false);
		h.assertTrue(drill.depositBoreProgress() == 7 && drill.depositDepth() == 0, "Unpowered boring advanced");
		tick(level, drill, 34, true); // finish depth 1, scan empty, then bore depth 2
		h.assertTrue(drill.depositDepth() == 2, "Failed to reach depth two");
		tick(level, drill, 123, true);
		int progress = drill.processingProgress();
		h.assertTrue(progress == 123, "Extraction progress " + progress);
		reload(level, drill); tick(level, drill, 20, false);
		h.assertTrue(drill.processingProgress() == progress, "Partial extraction lost on load/power loss");
		tick(level, drill, 300 - progress, true);
		h.assertTrue(count(drill, Items.RAW_IRON) == 1, "Stone/deepslate doubled extraction");
		h.assertTrue(Deposits.remaining(level, source) + Deposits.remaining(level, source.east()) == 1999, "Shared reserve not debited once");
		h.succeed();
	}

	@GameTest(structure = ARENA) public void undergroundDepositFullOutputsPauseAndOtherFamiliesContinue(GameTestHelper h) {
		var drill = rig(h, true); var level = h.getLevel();
		BlockPos source = DeepDrillingAssemblyBlock.center(drill.getBlockPos(), drill.getBlockState()).below();
		put(level, source, "coal"); put(level, source.east(), "iron");
		for (int slot = 0; slot < 3; slot++) drill.setItem(slot, new ItemStack(Items.COBBLESTONE, 64));
		tick(level, drill, 60, true);
		h.assertTrue(drill.depositDepth() == 0 && drill.depositBoreProgress() == 0, "Full outputs bored onward");
		drill.setItem(1, ItemStack.EMPTY); tick(level, drill, 20 + 100, true);
		int progress = drill.processingProgress();
		drill.setItem(1, new ItemStack(Items.COBBLESTONE, 64)); tick(level, drill, 100, true);
		h.assertTrue(drill.processingProgress() == progress && Deposits.remaining(level, source) == 1000, "Full output consumed progress or reserve");
		drill.setItem(1, new ItemStack(Items.RAW_IRON, 1)); tick(level, drill, 300 - progress, true);
		h.assertTrue(count(drill, Items.RAW_IRON) == 2 && Deposits.remaining(level, source) == 1000, "Blocked first family prevented second output");
		h.succeed();
	}

	@GameTest(structure = ARENA) public void undergroundDepositDepthBoundaryDepletionAndBedrock(GameTestHelper h) {
		var drill = rig(h, true); var level = h.getLevel();
		BlockPos center = DeepDrillingAssemblyBlock.center(drill.getBlockPos(), drill.getBlockState());
		BlockPos edge = center.offset(4, -128, 4), outside = center.offset(5, -128, 0), below = center.below(129);
		put(level, edge, "deepslate_tin"); put(level, outside, "coal"); put(level, below, "iron");
		Deposits.extract(level, edge, 999);
		tick(level, drill, 128 * 21 + 301, true);
		h.assertTrue(level.getBlockState(edge).is(Blocks.DEEPSLATE) && count(drill, JugcraftDeposits.BLOCKS.get("tin_deposit").yield()) == 1, "Depth-128 extraction / deepslate filler");
		h.assertTrue(Deposits.remaining(level, outside) == 1000 && Deposits.remaining(level, below) == 1000, "Exceeded 9x9x128 boundary");
		int depth = drill.depositDepth(); tick(level, drill, 50, true);
		h.assertTrue(depth == 128 && drill.depositDepth() == 128, "Maximum depth exceeded");
		// A rebuilt rig must bore again, but the world's reserves remain exhausted.
		var saved = drill.saveWithoutMetadata(level.registryAccess());
		saved.putInt("depositDepth", 0); saved.putBoolean("depositLayerReady", false);
		drill.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), saved));
		level.setBlock(center.below(), Blocks.BEDROCK.defaultBlockState(), 3);
		tick(level, drill, 40, true);
		h.assertTrue(drill.depositDepth() == 0, "Bored through bedrock");
		h.succeed();
	}

	@GameTest(structure = ARENA) public void undergroundDepositProtectionAndSharedReserve(GameTestHelper h) {
		var drill = rig(h, true); var level = h.getLevel();
		BlockPos source = DeepDrillingAssemblyBlock.center(drill.getBlockPos(), drill.getBlockState()).below();
		put(level, source, "iron"); Deposits.extract(level, source, 999);
		var actor = net.fabricmc.fabric.api.entity.FakePlayer.get(level, new com.mojang.authlib.GameProfile(UUID.randomUUID(), "DepositTest"));
		drill.claimDepositDrill(actor);
		var denied = new java.util.concurrent.atomic.AtomicReference<BlockPos>(source);
		net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents.BEFORE.register((l, p, at, state, be) -> !at.equals(denied.get()));
		try {
			tick(level, drill, 340, true);
			h.assertTrue(Deposits.remaining(level, source) == 1 && count(drill, Items.RAW_IRON) == 0, "Ignored protection callback");
		} finally { denied.set(null); }
		h.assertTrue(Deposits.extract(level, source, 1) == 1 && Deposits.extract(level, source, 1) == 0, "Two workers duplicated last unit");
		h.assertTrue(level.getBlockState(source).is(Blocks.STONE), "Stone filler missing");
		h.succeed();
	}

	@GameTest(structure = ARENA) public void undergroundDepositOverlappingRigsAndUpgradeRate(GameTestHelper h) {
		var level = h.getLevel(); var first = rig(h, true);
		var second = placeRig(level, first.getBlockPos().east(4), true);
		BlockPos shared = DeepDrillingAssemblyBlock.center(first.getBlockPos(), first.getBlockState()).offset(2, -1, 0);
		put(level, shared, "deepslate_copper"); Deposits.extract(level, shared, 999);
		first.setItem(3, new ItemStack(MachineUpgrades.SPEED, 2));
		first.setItem(4, new ItemStack(MachineUpgrades.EFFICIENCY, 2));
		tick(level, first, 20, true);
		h.assertTrue(((SimpleEnergyStorage) first.energyFor(null)).getAmount() == 19968 && first.depositDepth() == 1, "Boring cost/rate must ignore extraction cards");
		tick(level, first, 149, true);
		h.assertTrue(count(first, Items.RAW_COPPER) == 0 && first.processingDuration() == 150, "Speed cards produced early");
		tick(level, first, 1, true);
		h.assertTrue(((SimpleEnergyStorage) first.energyFor(null)).getAmount() == 20000 - first.upgrades().use(32), "Efficiency did not apply to deep extraction");
		tick(level, second, 340, true);
		h.assertTrue(count(first, Items.RAW_COPPER) + count(second, Items.RAW_COPPER) == 1 && Deposits.remaining(level, shared) == 0,
				"Overlapping rigs duplicated the last shared unit");
		h.succeed();
	}

	@GameTest public void undergroundDepositConfigurationAndCaveBiomePolicy(GameTestHelper h) {
		var level = h.getLevel();
		for (var family : UndergroundDepositFeature.Family.values()) {
			var feature = (UndergroundDepositFeature) level.registryAccess().lookupOrThrow(Registries.FEATURE)
					.getValue(Jugcraft.id("underground_" + family.id() + "_deposit"));
			h.assertTrue(feature.family() == family, "Configured family mismatch");
			var cave = level.registryAccess().lookupOrThrow(Registries.BIOME).getOrThrow(net.minecraft.world.level.biome.Biomes.LUSH_CAVES);
			h.assertTrue(!cave.is(feature.surfaceBiomes()), "Cave biome must not replace surface eligibility");
			var key = switch(family) {
				case COAL -> net.minecraft.world.level.biome.Biomes.FOREST;
				case IRON, TIN -> net.minecraft.world.level.biome.Biomes.STONY_PEAKS;
				case COPPER -> net.minecraft.world.level.biome.Biomes.BADLANDS;
			};
			h.assertTrue(level.registryAccess().lookupOrThrow(Registries.BIOME).getOrThrow(key).is(feature.surfaceBiomes()), "Missing native biome route");
		}
		h.succeed();
	}

	@GameTest(structure = ARENA) public void undergroundDepositPlacementPreservesObstructionsAndCover(GameTestHelper h) {
		var level = h.getLevel(); BlockPos center = h.absolutePos(new BlockPos(16, 1, 16)).atY(160);
		for (var family : UndergroundDepositFeature.Family.values()) {
			var feature = new UndergroundDepositFeature(family, TagKey.create(Registries.BIOME, Jugcraft.id("has_bulk_deposit/" + family.id())));
			fill(level, center.offset(-12, -4, -12), center.offset(12, 18, 12), Blocks.DEEPSLATE);
			List<BlockPos> expected = UndergroundDepositFeature.plan(family, RandomSource.create(17), center);
			BlockPos obstruction = expected.getLast();
			level.setBlock(obstruction, Blocks.DIAMOND_ORE.defaultBlockState(), 2);
			h.assertTrue(feature.placeBody(level, RandomSource.create(17), center), "Valid body rejected " + family);
			h.assertTrue(level.getBlockState(obstruction).is(Blocks.DIAMOND_ORE), "Replaced existing ore");
			long count = expected.stream().filter(p -> level.getBlockState(p).getBlock() instanceof DepositBlock).count();
			h.assertTrue(count >= family.minBlocks && count <= family.maxBlocks, "Placed reserve range " + family);
			fill(level, center.offset(-12, -4, -12), center.offset(12, 18, 12), Blocks.AIR);
			fill(level, center.offset(-12, -4, -12), center.offset(12, 3, 12), Blocks.STONE);
			h.assertTrue(!feature.placeBody(level, RandomSource.create(17), center), "Shallow body generated " + family);
			fill(level, center.offset(-12, -4, -12), center.offset(12, 18, 12), Blocks.WATER);
			h.assertTrue(!feature.placeBody(level, RandomSource.create(17), center), "Replaced fluid " + family);
		}
		h.succeed();
	}

	@GameTest(structure = ARENA) public void undergroundDepositProspectorAndAssemblyRecipe(GameTestHelper h) {
		var level = h.getLevel(); BlockPos source = h.absolutePos(new BlockPos(8, 1, 8)).atY(180);
		put(level, source, "copper");
		var readings = OreSurvey.survey(level, source, RandomSource.create(42), 0, 1);
		h.assertTrue(readings.stream().anyMatch(r -> r.label().equals("prospector.jugcraft.bulk_copper")), "No distinct bulk reading");
		Deposits.extract(level, source, 1000);
		h.assertTrue(OreSurvey.survey(level, source, RandomSource.create(42), 0, 1).stream().noneMatch(r -> r.label().equals("prospector.jugcraft.bulk_copper")), "Empty reserve still surveyed");
		List<ItemStack> grid = new ArrayList<>();
		for (String id : List.of("steel_plate", "steel_gear", "steel_plate", "steel_gear", "basic_circuit", "steel_gear", "steel_plate", "steel_pickaxe", "steel_plate"))
			grid.add(new ItemStack(BuiltInRegistries.ITEM.getValue(Jugcraft.id(id))));
		var input = CraftingInput.of(3, 3, grid);
		var recipe = level.getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, level).orElseThrow();
		h.assertTrue(recipe.value().assemble(input).is(JugcraftDeposits.DEEP_DRILLING_ASSEMBLY.asItem()), "Assembly not reachable from steel-tier ingredients");
		h.succeed();
	}

	public static MachineBlockEntity rig(GameTestHelper h, boolean deep) {
		var level = h.getLevel(); var pos = h.absolutePos(new BlockPos(12, 1, 12)).atY(220);
		for (int dx = -6; dx <= 6; dx += 6) for (int dz = -6; dz <= 6; dz += 6) level.getChunkAt(pos.offset(dx, 0, dz));
		return placeRig(level, pos, deep);
	}

	public static MachineBlockEntity placeRig(ServerLevel level, BlockPos pos, boolean deep) {
		var block = JugcraftMachines.MACHINES.get(MachineKind.DEPOSIT_DRILL);
		var state = block.defaultBlockState().setValue(MachineBlock.FACING, Direction.NORTH);
		level.setBlock(pos, state, 3); block.setPlacedBy(level, pos, state, null, ItemStack.EMPTY);
		if (deep) net.minecraft.world.level.block.DoublePlantBlock.placeAt(level,
				JugcraftDeposits.DEEP_DRILLING_ASSEMBLY.defaultBlockState(), DeepDrillingAssemblyBlock.mount(pos, state), 3);
		return (MachineBlockEntity) level.getBlockEntity(pos);
	}
	private static void put(ServerLevel level, BlockPos pos, String family) { level.setBlock(pos, JugcraftDeposits.BLOCKS.get(family + "_deposit").defaultBlockState(), 3); }
	public static void fill(ServerLevel level, BlockPos a, BlockPos b, Block block) {
		for (var p : BlockPos.betweenClosed(a, b)) level.setBlock(p, block.defaultBlockState(), 2);
	}
	public static void tick(ServerLevel level, MachineBlockEntity drill, int ticks, boolean powered) {
		for (int n = 0; n < ticks; n++) {
			((SimpleEnergyStorage) drill.energyFor(null)).setAmount(powered ? 20000 : 0);
			drill.serverTick(level, drill.getBlockPos(), drill.getBlockState());
		}
	}
	private static void reload(ServerLevel level, MachineBlockEntity drill) {
		var saved = drill.saveWithoutMetadata(level.registryAccess());
		drill.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), saved));
	}
	private static int count(MachineBlockEntity drill, net.minecraft.world.item.Item item) {
		int count = 0; for (int i = 0; i < 3; i++) if (drill.getItem(i).is(item)) count += drill.getItem(i).getCount(); return count;
	}
}
