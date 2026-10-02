package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.blueprint.Blueprint;
import io.github.jimbozoomer.jugcraft.blueprint.BlueprintItem;
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
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/** The Blueprint System's first slice: built-in blueprints load, stakes offer bottom-up jobs, and finished builds pop off. */
public class BlueprintGameTests {
	@GameTest
	public void uploadIsBoundedAndOrdered(GameTestHelper helper) {
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

	/** Blueprint colours: built-in whole builds blue, parts of a larger build green, imports red (item and hologram). */
	@GameTest
	public void blueprintKinds(GameTestHelper helper) {
		String hut = "{\"format\": 1, \"name\": \"Kind Test\", \"palette\": {\"S\": \"minecraft:stone_bricks\"},"
				+ " \"layers\": [[\"SSS\"]], \"anchor\": [1, 0, 2]}";
		try {
			helper.assertTrue(Blueprint.get("small_church", false).kind == Blueprint.Kind.COMPLETE, "a built-in build is complete (blue)");
			helper.assertTrue(Blueprint.parse("t/part", hut.replace("\"format\": 1", "\"format\": 1, \"kind\": \"part\""), "built in").kind
					== Blueprint.Kind.PART, "a built-in part is green");
			helper.assertTrue(Blueprint.parse("t/imp", hut.replace("\"format\": 1", "\"format\": 1, \"kind\": \"complete\""), "imported").kind
					== Blueprint.Kind.IMPORTED, "an import is red whatever its file says");
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
		expectInvalid(helper, good.replace("\"format\": 1", "\"format\": 2"), "format");
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

	@GameTest(structure = ARENA, maxTicks = 100, skyAccess = true)
	public void stakeOffersBottomLayerFirst(GameTestHelper helper) {
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

	@GameTest(structure = ARENA, maxTicks = 200, skyAccess = true)
	public void finishedFoundationPopsOff(GameTestHelper helper) {
		BlockPos stakePos = helper.absolutePos(new BlockPos(22, 1, 40));
		helper.getLevel().setBlockAndUpdate(stakePos, JugcraftBlueprints.STAKE.defaultBlockState());
		SurveyStakeBlockEntity stake = (SurveyStakeBlockEntity) helper.getLevel().getBlockEntity(stakePos);
		stake.setup("drone_tower_foundation", Rotation.NONE, UUID.randomUUID());
		for (Map.Entry<BlockPos, BlockState> entry : stake.wanted().entrySet()) {
			helper.getLevel().setBlockAndUpdate(entry.getKey(), entry.getValue());
		}
		helper.succeedWhen(() -> {
			helper.assertTrue(helper.getLevel().getBlockState(stakePos).is(Blocks.AIR), "the stake popped off");
			helper.assertTrue(!helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(stakePos).inflate(2),
					e -> e.getItem().is(JugcraftBlueprints.BLUEPRINT)).isEmpty(), "the blueprint came back");
		});
	}
}
