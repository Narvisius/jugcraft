package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.blueprint.Blueprint;
import io.github.jimbozoomer.jugcraft.blueprint.BlueprintItem;
import io.github.jimbozoomer.jugcraft.blueprint.BlueprintTableBlock;
import io.github.jimbozoomer.jugcraft.blueprint.JugcraftBlueprints;
import io.github.jimbozoomer.jugcraft.blueprint.SurveyStakeBlockEntity;
import io.github.jimbozoomer.jugcraft.drone.BuildJobs;
import io.github.jimbozoomer.jugcraft.party.UseMode;
import io.github.jimbozoomer.jugcraft.tower.JugcraftTower;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ComparatorBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.entity.ComparatorBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.phys.AABB;

/** The Blueprint System's first slice: built-in blueprints load, stakes offer bottom-up jobs, and finished builds pop off. */
public class BlueprintGameTests {
	@GameTest
	public void uploadIsBoundedAndOrdered(GameTestHelper helper) {
		if (TestFocus.skip("blueprints")) {
			helper.succeed();
			return;
		}
		var upload = new io.github.jimbozoomer.jugcraft.blueprint.BlueprintUpload(2);
		helper.assertTrue(upload.append(0, 2, "first"), "First part accepted");
		helper.assertFalse(upload.append(0, 2, "duplicate"), "Repeated part refused");
		helper.assertFalse(upload.append(1, 3, "changed"), "Changed total refused");
		helper.assertTrue(upload.append(1, 2, "second"), "Next part accepted");
		helper.assertTrue(upload.complete() && upload.text().equals("firstsecond"), "Only valid parts assembled");
		helper.assertFalse(upload.append(2, 2, "extra"), "Completed upload refuses more data");
		var large = new io.github.jimbozoomer.jugcraft.blueprint.BlueprintUpload(33);
		for (int i = 0; i < 32; i++) {
			helper.assertTrue(large.append(i, 33, "x".repeat(8000)), "Bounded part accepted");
		}
		helper.assertFalse(large.append(32, 33, "x".repeat(8000)), "Total character limit enforced");
		helper.assertFalse(large.complete(), "Oversized upload cannot complete");
		helper.succeed();
	}
	private static final String ARENA = "jugcraft-test:drone_tower";

	@GameTest
	public void builtInBlueprintsLoad(GameTestHelper helper) {
		if (TestFocus.skip("blueprints")) {
			helper.succeed();
			return;
		}
		Blueprint tower = Blueprint.get("drone_tower_foundation", false);
		Blueprint church = Blueprint.get("small_church", false);
		helper.assertTrue(tower != null && tower.size() == 225, "tower foundation: 15x15 plinth with the core (" + (tower == null ? -1 : tower.size()) + ")");
		helper.assertTrue(church != null && church.size() > 500, "small church loads (" + (church == null ? -1 : church.size()) + " blocks)");
		Blueprint arc = Blueprint.get("arc_furnace", false);
		helper.assertTrue(arc != null && arc.size() == 27, "arc furnace: 26 casings and the controller");
		long cores = tower.cells(Rotation.NONE).stream().filter(c -> c.state().is(JugcraftTower.CORE)).count();
		helper.assertTrue(cores == 1, "one Tower Core in the foundation");
		// Turning keeps every block and turns stairs with it.
		helper.assertTrue(church.cells(Rotation.CLOCKWISE_90).size() == church.size(), "rotation keeps every block");
		helper.assertTrue(BlueprintItem.idOf(BlueprintItem.stack("small_church")).equals("small_church"), "the item carries its blueprint id");
		helper.succeed();
	}

	/** Blueprint item colours (complete blue, part green, import red) and table categories (set, individual, partial). */
	@GameTest
	public void blueprintKinds(GameTestHelper helper) {
		if (TestFocus.skip("blueprints")) {
			helper.succeed();
			return;
		}
		String hut = "{\"format\": 1, \"name\": \"Kind Test\", \"palette\": {\"S\": \"minecraft:stone_bricks\"},"
				+ " \"layers\": [[\"SSS\"]], \"anchor\": [1, 0, 2]}";
		try {
			Blueprint church = Blueprint.get("small_church", false);
			helper.assertTrue(church.kind == Blueprint.Kind.COMPLETE && church.category == Blueprint.Category.INDIVIDUAL, "a standalone building is blue, an individual structure");
			Blueprint plant = Blueprint.parse("t/col", hut.replace("\"format\": 1", "\"format\": 1, \"kind\": \"set\""), "built in");
			helper.assertTrue(plant.kind == Blueprint.Kind.COMPLETE && plant.category == Blueprint.Category.SET, "a set is blue, under STRUCTURE SET");
			Blueprint tower = Blueprint.parse("t/part", hut.replace("\"format\": 1", "\"format\": 1, \"kind\": \"part\""), "built in");
			helper.assertTrue(tower.kind == Blueprint.Kind.PART && tower.category == Blueprint.Category.PARTIAL, "a part of a set is green, a partial structure");
			Blueprint imported = Blueprint.parse("t/imp", hut.replace("\"format\": 1", "\"format\": 1, \"kind\": \"set\""), "imported");
			helper.assertTrue(imported.kind == Blueprint.Kind.IMPORTED, "an import is red whatever its file says");
		} catch (Blueprint.Invalid e) {
			throw new AssertionError("a valid blueprint was refused: " + e.getMessage());
		}
		try {
			Blueprint.parse("t/bad", hut.replace("\"format\": 1", "\"format\": 1, \"kind\": \"giant\""), "built in");
			throw new AssertionError("a built-in blueprint with an unknown kind was accepted");
		} catch (Blueprint.Invalid e) {
			helper.assertTrue(e.getMessage().contains("kind"), "the error names the kind");
		}
		net.minecraft.world.item.ItemStack stack = io.github.jimbozoomer.jugcraft.blueprint.BlueprintItem.stack("small_church");
		net.minecraft.world.item.component.CustomModelData data = stack.get(net.minecraft.core.component.DataComponents.CUSTOM_MODEL_DATA);
		helper.assertTrue(data != null && "complete".equals(data.getString(0)), "the blueprint item carries its colour");
		helper.succeed();
	}

	@GameTest
	public void importChecksAndSaves(GameTestHelper helper) {
		if (TestFocus.skip("blueprints")) {
			helper.succeed();
			return;
		}
		String good = "{\"format\": 1, \"name\": \"Game Test Hut\", \"palette\": {\"S\": \"minecraft:stone_bricks\"},"
				+ " \"layers\": [[\"SSS\", \"SSS\"]], \"anchor\": [1, 0, 3]}";
		try {
			Blueprint imported = io.github.jimbozoomer.jugcraft.blueprint.BlueprintLibrary.importText(helper.getLevel().getServer(), good);
			helper.assertTrue(imported.size() == 6 && imported.id.equals("import/game_test_hut"), "import adds a 6-block blueprint");
			helper.assertTrue(Blueprint.get("import/game_test_hut", false) != null, "it is in the server library");
			helper.assertTrue(java.nio.file.Files.exists(io.github.jimbozoomer.jugcraft.blueprint.BlueprintLibrary.folder(helper.getLevel().getServer())
					.resolve("game_test_hut.jugbp.json")), "it is saved with the world");
		} catch (Blueprint.Invalid e) {
			throw new AssertionError("a valid blueprint was refused: " + e.getMessage());
		}
		expectInvalid(helper, good.replace("minecraft:stone_bricks", "minecraft:no_such_block"), "Unknown block");
		expectInvalid(helper, good.replace("minecraft:stone_bricks", "minecraft:command_block"), "not allowed");
		expectInvalid(helper, good.replace("\"SSS\", \"SSS\"", "\"SXS\""), "not in the palette");
		expectInvalid(helper, "{not json", "Not valid JSON");
		expectInvalid(helper, good.replace("minecraft:stone_bricks", "#minecraft:no_such_tag"), "Unknown block tag");
		expectInvalid(helper, good.replace("minecraft:stone_bricks", "#minecraft:portals"), "not allowed");
		expectInvalid(helper, good.replace("\"format\": 1", "\"format\": 3"), "format");
		helper.succeed();
	}

	private static void expectInvalid(GameTestHelper helper, String json, String reason) {
		try {
			Blueprint.parse("import/x", json, "imported");
			throw new AssertionError("accepted a bad blueprint (expected: " + reason + ")");
		} catch (Blueprint.Invalid e) {
			helper.assertTrue(e.getMessage().contains(reason), "message \"" + e.getMessage() + "\" mentions " + reason);
		}
	}

	/** The drafting station is two blocks; breaking either half takes both and drops one table. */
	@GameTest(structure = ARENA, maxTicks = 40, skyAccess = true)
	public void blueprintTableIsTwoBlocks(GameTestHelper helper) {
		if (TestFocus.skip("blueprints")) {
			helper.succeed();
			return;
		}
		BlockPos main = helper.absolutePos(new BlockPos(10, 1, 10));
		BlockState state = JugcraftBlueprints.TABLE.defaultBlockState().setValue(BlueprintTableBlock.FACING, net.minecraft.core.Direction.NORTH);
		BlockPos side = BlueprintTableBlock.partner(state, main);
		helper.assertTrue(side.equals(main.west()), "facing north, the side half is to the west (the player's right)");
		helper.getLevel().setBlockAndUpdate(main, state);
		helper.getLevel().setBlockAndUpdate(side, state.setValue(BlueprintTableBlock.PART, BlueprintTableBlock.Part.SIDE));
		helper.getLevel().destroyBlock(side, true);
		helper.succeedWhen(() -> {
			helper.assertTrue(helper.getLevel().getBlockState(main).isAir(), "the main half went with the side half");
			java.util.List<ItemEntity> drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(main).inflate(3),
					e -> e.getItem().is(JugcraftBlueprints.TABLE.asItem()));
			helper.assertTrue(drops.stream().mapToInt(e -> e.getItem().getCount()).sum() == 1, "one table dropped");
		});
	}

	@GameTest(structure = ARENA, maxTicks = 100, skyAccess = true)
	public void stakeOffersBottomLayerFirst(GameTestHelper helper) {
		if (TestFocus.skip("blueprints")) {
			helper.succeed();
			return;
		}
		UUID owner = UUID.randomUUID();
		BlockPos stakePos = helper.absolutePos(new BlockPos(22, 1, 40));
		helper.getLevel().setBlockAndUpdate(stakePos, JugcraftBlueprints.STAKE.defaultBlockState());
		SurveyStakeBlockEntity stake = (SurveyStakeBlockEntity) helper.getLevel().getBlockEntity(stakePos);
		stake.setup("small_church", Rotation.NONE, owner);
		helper.runAfterDelay(2, () -> {
			List<BuildJobs.Target> targets = BuildJobs.sources().stream()
					.flatMap(s -> s.openTargets(helper.getLevel(), stakePos, 64, owner, UseMode.PERSONAL, 64).stream())
					.filter(t -> t.owner().equals(owner)).toList();
			helper.assertTrue(!targets.isEmpty(), "the stake offers jobs to its owner's depot");
			int window = SurveyStakeBlockEntity.LAYER_WINDOW;
			helper.assertTrue(targets.stream().allMatch(t -> t.pos().getY() - stakePos.getY() <= window),
					"only the bottom few layers are offered first");
			helper.assertTrue(targets.stream().anyMatch(t -> t.pos().getY() == stakePos.getY()), "the floor layer is among them");
			helper.assertTrue(targets.stream().allMatch(t -> stake.wanted().keySet().stream().noneMatch(p -> p.getX() == t.pos().getX()
					&& p.getZ() == t.pos().getZ() && p.getY() < t.pos().getY())), "never a block over an empty cell of its column");
			List<BuildJobs.Target> stranger = BuildJobs.sources().stream()
					.flatMap(s -> s.openTargets(helper.getLevel(), stakePos, 64, UUID.randomUUID(), UseMode.PERSONAL, 64).stream())
					.filter(t -> t.owner().equals(owner)).toList();
			helper.assertTrue(stranger.isEmpty(), "a stranger's depot gets no jobs from a Personal blueprint");
			helper.succeed();
		});
	}

	/**
	 * Wrong blocks in the footprint (drawn red) are offered first, from the top down, and replaced: terrain without
	 * drops, a chest broken with its contents dropped.
	 */
	@GameTest(structure = ARENA, maxTicks = 100, skyAccess = true)
	public void stakeReplacesBlocksInTheWay(GameTestHelper helper) {
		if (TestFocus.skip("blueprints")) {
			helper.succeed();
			return;
		}
		UUID owner = UUID.randomUUID();
		BlockPos stakePos = helper.absolutePos(new BlockPos(22, 1, 40));
		helper.getLevel().setBlockAndUpdate(stakePos, JugcraftBlueprints.STAKE.defaultBlockState());
		SurveyStakeBlockEntity stake = (SurveyStakeBlockEntity) helper.getLevel().getBlockEntity(stakePos);
		stake.setup("small_church", Rotation.NONE, owner);
		// A column of the church: a chest in its lowest cell, dirt in the next one up.
		BlockPos low = stake.wanted().keySet().stream().filter(p -> stake.wanted().containsKey(p.above()))
				.min(java.util.Comparator.comparingInt(BlockPos::getY)).orElseThrow();
		helper.getLevel().setBlockAndUpdate(low, Blocks.CHEST.defaultBlockState());
		((net.minecraft.world.level.block.entity.ChestBlockEntity) helper.getLevel().getBlockEntity(low)).setItem(0, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND));
		helper.getLevel().setBlockAndUpdate(low.above(), Blocks.DIRT.defaultBlockState());
		helper.runAfterDelay(2, () -> {
			List<BuildJobs.Target> targets = BuildJobs.sources().stream()
					.flatMap(s -> s.openTargets(helper.getLevel(), stakePos, 64, owner, UseMode.PERSONAL, 64).stream())
					.filter(t -> t.owner().equals(owner)).toList();
			helper.assertTrue(targets.size() >= 2 && targets.get(0).pos().equals(low.above()) && targets.get(1).pos().equals(low),
					"the blocks in the way come first, top down, got " + targets.stream().limit(3).map(BuildJobs.Target::pos).toList());
			for (BuildJobs.Target target : targets.subList(0, 2)) {
				helper.assertTrue(target.source().fill(helper.getLevel(), target, target.state()), "replaced " + target.pos());
				helper.assertTrue(SurveyStakeBlockEntity.matches(helper.getLevel().getBlockState(target.pos()), target.state()), "the right block is in at " + target.pos());
			}
			helper.assertTrue(!helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(low).inflate(2),
					e -> e.getItem().is(net.minecraft.world.item.Items.DIAMOND)).isEmpty(), "the chest's contents were dropped, not lost");
			helper.succeed();
		});
	}

	/**
	 * A comparator beside a stake reads how much of the build is in place, 1 + 14 x placed / total like a container's
	 * fill (0 with nothing placed): first what the ground already holds, then with about half the rest set.
	 */
	@GameTest(structure = ARENA, maxTicks = 200, skyAccess = true)
	public void aComparatorReadsTheBuildsProgress(GameTestHelper helper) {
		if (TestFocus.skip("blueprints")) {
			helper.succeed();
			return;
		}
		BlockPos stakePos = helper.absolutePos(new BlockPos(22, 1, 40));
		helper.getLevel().setBlockAndUpdate(stakePos, JugcraftBlueprints.STAKE.defaultBlockState());
		SurveyStakeBlockEntity stake = (SurveyStakeBlockEntity) helper.getLevel().getBlockEntity(stakePos);
		stake.setup("drone_tower_foundation", Rotation.NONE, UUID.randomUUID());
		Map<BlockPos, BlockState> wanted = stake.wanted();
		Direction side = Direction.Plane.HORIZONTAL.stream().filter(d -> !wanted.containsKey(stakePos.relative(d))).findFirst().orElseThrow();
		BlockPos comparator = stakePos.relative(side);
		// A comparator needs solid ground under it.
		helper.getLevel().setBlockAndUpdate(comparator.below(), Blocks.STONE.defaultBlockState());
		helper.getLevel().setBlockAndUpdate(comparator, Blocks.COMPARATOR.defaultBlockState().setValue(ComparatorBlock.FACING, side.getOpposite()));
		helper.assertTrue(helper.getLevel().getBlockState(comparator).is(Blocks.COMPARATOR), "The comparator did not stay beside the stake");
		int total = wanted.size();
		int[] expected = {-1};
		helper.runAfterDelay(45, () -> {
			// Whatever of the foundation the arena already holds counts too.
			List<Map.Entry<BlockPos, BlockState>> open = wanted.entrySet().stream()
					.filter(entry -> !SurveyStakeBlockEntity.matches(helper.getLevel().getBlockState(entry.getKey()), entry.getValue())).toList();
			int already = total - open.size();
			int before = already == 0 ? 0 : 1 + 14 * already / total;
			helper.assertTrue(stake.signal() == before && output(helper, comparator) == before,
					"With " + already + " of " + total + " in place the stake gives " + stake.signal() + " and the comparator reads "
							+ output(helper, comparator) + ", not " + before);
			int half = open.size() / 2;
			open.stream().limit(half).forEach(entry -> helper.getLevel().setBlockAndUpdate(entry.getKey(), entry.getValue()));
			expected[0] = 1 + 14 * (already + half) / total;
		});
		helper.succeedWhen(() -> {
			helper.assertTrue(expected[0] >= 0, "Not built yet");
			helper.assertTrue(stake.signal() == expected[0], "The stake gives " + stake.signal() + ", not " + expected[0]);
			helper.assertTrue(output(helper, comparator) == expected[0], "The comparator reads " + output(helper, comparator) + ", not " + expected[0]);
		});
	}

	private static int output(GameTestHelper helper, BlockPos comparator) {
		return helper.getLevel().getBlockEntity(comparator) instanceof ComparatorBlockEntity entity ? entity.getOutputSignal() : -1;
	}

	/**
	 * "Any material" slots: a palette entry naming a block tag ({@code "#minecraft:planks"}) shows the tag's first block,
	 * and any block of the tag counts there. Birch and spruce planks set by hand count; a drone's job for the last cell
	 * names the tag, and dark oak planks it brings complete the build. A block of another tag does not count. Stairs of
	 * another wood keep the drawn stairs' facing.
	 */
	@GameTest(structure = ARENA, maxTicks = 200, skyAccess = true)
	public void anyMaterialSlotsTakeAnyBlockOfTheirTag(GameTestHelper helper) {
		if (TestFocus.skip("blueprints")) {
			helper.succeed();
			return;
		}
		String json = "{\"format\": 1, \"name\": \"Any Plank Bench\", \"palette\": {\"P\": \"#minecraft:planks\"},"
				+ " \"layers\": [[\"PPP\"]], \"anchor\": [1, 0, 2]}";
		Blueprint bench;
		try {
			bench = io.github.jimbozoomer.jugcraft.blueprint.BlueprintLibrary.importText(helper.getLevel().getServer(), json);
		} catch (Blueprint.Invalid e) {
			throw new AssertionError("a blueprint with a tag slot was refused: " + e.getMessage());
		}
		helper.assertTrue(bench.anyOf(0) != null && bench.anyOf(0).equals(BlockTags.PLANKS) && bench.state(0, Rotation.NONE).is(BlockTags.PLANKS),
				"the slot names the planks tag and shows a plank, got " + bench.anyOf(0) + " " + bench.state(0, Rotation.NONE));
		UUID owner = UUID.randomUUID();
		BlockPos stakePos = helper.absolutePos(new BlockPos(22, 1, 40));
		helper.getLevel().setBlockAndUpdate(stakePos, JugcraftBlueprints.STAKE.defaultBlockState());
		SurveyStakeBlockEntity stake = (SurveyStakeBlockEntity) helper.getLevel().getBlockEntity(stakePos);
		stake.setup(bench.id, Rotation.NONE, owner);
		List<BlockPos> cells = List.copyOf(stake.wanted().keySet());
		helper.assertTrue(cells.size() == 3, "the bench has three cells, got " + cells.size());
		helper.getLevel().setBlockAndUpdate(cells.get(0), Blocks.BIRCH_PLANKS.defaultBlockState());
		helper.getLevel().setBlockAndUpdate(cells.get(1), Blocks.SPRUCE_PLANKS.defaultBlockState());
		helper.getLevel().setBlockAndUpdate(cells.get(2), Blocks.STONE.defaultBlockState());
		helper.runAfterDelay(2, () -> {
			List<BuildJobs.Target> targets = BuildJobs.sources().stream()
					.flatMap(s -> s.openTargets(helper.getLevel(), stakePos, 64, owner, UseMode.PERSONAL, 64).stream())
					.filter(t -> t.owner().equals(owner)).toList();
			helper.assertTrue(targets.size() == 1 && targets.get(0).pos().equals(cells.get(2)) && BlockTags.PLANKS.equals(targets.get(0).anyOf()),
					"only the stone cell is still to do, as any planks: " + targets.stream().map(t -> t.pos() + " " + t.anyOf()).toList());
			BuildJobs.Target target = targets.get(0);
			helper.assertTrue(target.source().fill(helper.getLevel(), target, BuildJobs.like(target.state(), Blocks.DARK_OAK_PLANKS)),
					"the drone's dark oak planks went in");
		});
		helper.runAfterDelay(4, () -> {
			BlockState stairs = Blocks.OAK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.EAST).setValue(StairBlock.HALF, Half.TOP);
			BlockState spruce = BuildJobs.like(stairs, Blocks.SPRUCE_STAIRS);
			helper.assertTrue(spruce.is(Blocks.SPRUCE_STAIRS) && spruce.getValue(StairBlock.FACING) == Direction.EAST
					&& spruce.getValue(StairBlock.HALF) == Half.TOP, "the spruce stairs kept the drawn facing and half: " + spruce);
		});
		helper.succeedWhen(() -> helper.assertTrue(helper.getLevel().getBlockState(stakePos).is(Blocks.AIR),
				"the bench of three woods is complete and the stake popped off"));
	}

	/**
	 * The design kit: the AI guide in the jar shows the example and the tag slots; the example imports as a 5 x 4 x 4
	 * shed with a planks slot; the block list holds stairs with their properties but no command block; the tag list
	 * holds planks but not the portals tag, which holds forbidden blocks.
	 */
	@GameTest
	public void theDesignKitDescribesWhatTheGameAllows(GameTestHelper helper) {
		if (TestFocus.skip("blueprints")) {
			helper.succeed();
			return;
		}
		String guide = io.github.jimbozoomer.jugcraft.blueprint.BlueprintKit.resource(io.github.jimbozoomer.jugcraft.blueprint.BlueprintKit.GUIDE);
		helper.assertTrue(guide.contains("Garden Shed") && guide.contains("#minecraft:planks") && guide.contains("8,192"),
				"the guide shows the example, tag slots and limits");
		try {
			Blueprint shed = Blueprint.parse("import/kit_example",
					io.github.jimbozoomer.jugcraft.blueprint.BlueprintKit.resource(io.github.jimbozoomer.jugcraft.blueprint.BlueprintKit.EXAMPLE), "imported");
			helper.assertTrue(shed.sizeX == 5 && shed.sizeY == 4 && shed.sizeZ == 4 && BlockTags.PLANKS.equals(shed.anyOf(shed.size() - 1)),
					"the example is a 5 x 4 x 4 shed roofed with any planks, got " + shed.sizeX + "x" + shed.sizeY + "x" + shed.sizeZ);
		} catch (Blueprint.Invalid e) {
			throw new AssertionError("the kit's example does not import: " + e.getMessage());
		}
		String blocks = io.github.jimbozoomer.jugcraft.blueprint.BlueprintKit.blocks();
		helper.assertTrue(blocks.contains("\nminecraft:stone_bricks\n") && blocks.contains("\nminecraft:oak_stairs [") && blocks.contains("facing=")
				&& !blocks.contains("minecraft:command_block") && !blocks.contains("\nminecraft:air"), "the block list holds what is allowed only");
		String tags = io.github.jimbozoomer.jugcraft.blueprint.BlueprintKit.tags();
		helper.assertTrue(tags.contains("\n#minecraft:planks (") && !tags.contains("#minecraft:portals"), "the tag list holds usable tags only");
		helper.succeed();
	}

	@GameTest(structure = ARENA, maxTicks = 200, skyAccess = true)
	public void finishedFoundationPopsOff(GameTestHelper helper) {
		if (TestFocus.skip("blueprints")) {
			helper.succeed();
			return;
		}
		BlockPos stakePos = helper.absolutePos(new BlockPos(22, 1, 40));
		helper.getLevel().setBlockAndUpdate(stakePos, JugcraftBlueprints.STAKE.defaultBlockState());
		SurveyStakeBlockEntity stake = (SurveyStakeBlockEntity) helper.getLevel().getBlockEntity(stakePos);
		stake.setup("drone_tower_foundation", Rotation.NONE, UUID.randomUUID());
		for (Map.Entry<BlockPos, BlockState> entry : stake.wanted().entrySet()) {
			helper.getLevel().setBlockAndUpdate(entry.getKey(), entry.getValue());
		}
		helper.succeedWhen(() -> {
			helper.assertTrue(helper.getLevel().getBlockState(stakePos).is(Blocks.AIR), "the stake popped off");
			helper.assertTrue(helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(stakePos).inflate(2),
					e -> e.getItem().is(JugcraftBlueprints.BLUEPRINT)).isEmpty(), "the blueprint was used up: nothing drops");
		});
	}
}
