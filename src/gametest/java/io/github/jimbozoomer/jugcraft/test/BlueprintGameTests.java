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
			helper.assertTrue(targets.stream().allMatch(t -> t.pos().getY() == stakePos.getY()), "only the floor layer is offered first");
			List<BuildJobs.Target> stranger = BuildJobs.sources().stream()
					.flatMap(s -> s.openTargets(helper.getLevel(), stakePos, 64, UUID.randomUUID(), UseMode.PERSONAL, 64).stream())
					.filter(t -> t.owner().equals(owner)).toList();
			helper.assertTrue(stranger.isEmpty(), "a stranger's depot gets no jobs from a Personal blueprint");
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
