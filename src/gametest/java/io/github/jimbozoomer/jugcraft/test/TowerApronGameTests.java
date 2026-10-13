package io.github.jimbozoomer.jugcraft.test;

import com.google.gson.JsonParser;
import io.github.jimbozoomer.jugcraft.drone.BuildJobs;
import io.github.jimbozoomer.jugcraft.drone.PlatformLayout;
import io.github.jimbozoomer.jugcraft.party.UseMode;
import io.github.jimbozoomer.jugcraft.tower.ExchangeStationBlockEntity;
import io.github.jimbozoomer.jugcraft.tower.ExchangeStations;
import io.github.jimbozoomer.jugcraft.tower.JugcraftTower;
import io.github.jimbozoomer.jugcraft.tower.TowerCoreBlockEntity;
import io.github.jimbozoomer.jugcraft.tower.TowerData;
import io.github.jimbozoomer.jugcraft.tower.TowerPanelBlock;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.zip.GZIPInputStream;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.AABB;

/** The outer paving is built by the tower, but stays ordinary removable world geometry. */
public class TowerApronGameTests {
	private static boolean skip(GameTestHelper h) {
		if (!TestFocus.skip("tower")) return false;
		h.succeed(); return true;
	}

	private static List<BlockPos> cosmeticApron() {
		try (var in = TowerData.class.getResourceAsStream("/data/jugcraft/drone_tower/tower_industrial.json.gz")) {
			if (in == null) throw new AssertionError("Missing current tower design");
			var data = JsonParser.parseReader(new InputStreamReader(new GZIPInputStream(in), StandardCharsets.UTF_8)).getAsJsonObject();
			List<BlockPos> positions = new ArrayList<>();
			for (var row : data.getAsJsonArray("cosmetic_apron")) {
				var xyz = row.getAsJsonArray();
				positions.add(new BlockPos(xyz.get(0).getAsInt(), xyz.get(1).getAsInt(), xyz.get(2).getAsInt()));
			}
			return List.copyOf(positions);
		} catch (java.io.IOException e) {
			throw new AssertionError("Could not read cosmetic apron test metadata", e);
		}
	}

	private static TowerCoreBlockEntity core(GameTestHelper h, int site) {
		ServerLevel level = h.getLevel();
		BlockPos near = h.absolutePos(new BlockPos(0, 2, 0)).offset(2_000_000 + site * 256, 0, 2_000_000);
		// Put the core next to a chunk edge, so both +/-40 corners cross the correct chunk boundaries.
		BlockPos at = new BlockPos(Math.floorDiv(near.getX(), 16) * 16 + 15, near.getY(), Math.floorDiv(near.getZ(), 16) * 16 + 15);
		for (int cx = (at.getX() - 40) >> 4; cx <= (at.getX() + 40) >> 4; cx++)
			for (int cz = (at.getZ() - 40) >> 4; cz <= (at.getZ() + 40) >> 4; cz++) level.getChunk(cx, cz);
		for (BlockPos p : cosmeticApron()) level.setBlock(at.offset(p), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
		for (int x = -7; x <= 7; x++) for (int z = -7; z <= 7; z++)
			level.setBlock(at.offset(x, 0, z), Blocks.CHISELED_STONE_BRICKS.defaultBlockState(), Block.UPDATE_ALL);
		level.setBlock(at, JugcraftTower.CORE.defaultBlockState(), Block.UPDATE_ALL);
		// This isolated site lies outside the game-test arena's ticking chunks. Bootstrap its normal tickets.
		TowerCoreBlockEntity.keepLoaded(level, at, true);
		return (TowerCoreBlockEntity) level.getBlockEntity(at);
	}

	private static void modules(TowerCoreBlockEntity core) {
		for (String module : JugcraftTower.MODULES)
			core.deposit(new ItemStack(JugcraftTower.MODULE_ITEMS.get(module), TowerCoreBlockEntity.MODULE_CAPACITY));
	}

	private static void saveAndReload(ServerLevel level, TowerCoreBlockEntity core) {
		var saved = core.saveWithoutMetadata(level.registryAccess());
		core.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), saved));
	}

	private static BlockState expected(TowerCoreBlockEntity core, BlockPos offset) {
		for (int[] b : core.layout().tier(1).place)
			if (offset.equals(new BlockPos(b[0], b[1], b[2]))) return core.layout().state(b[3]);
		throw new AssertionError("Apron position absent from actual tier-one placements: " + offset);
	}

	private static void workingDepot(GameTestHelper h, TowerCoreBlockEntity core) {
		var terminal = core.terminal();
		h.assertTrue(terminal != null && terminal.tower() == core, "the tower retains its own terminal");
		terminal.rescanNow();
		h.assertTrue(terminal.layout().isValid() && terminal.layout().cellKeys().size() < PlatformLayout.MAX_CELLS,
				"ordinary paving does not expand or truncate the functional landing-platform scan");
		h.assertTrue(terminal.layout().pads().size() == 8 && terminal.layout().pickups().size() == 1,
				"all eight ground pads and the pickup remain connected");
		h.assertTrue(terminal.packagerEntity() == h.getLevel().getBlockEntity(core.getBlockPos().offset(13, 1, 11)),
				"the original cargo packager remains reachable");
		for (var block : List.of(ExchangeStations.ENERGY, ExchangeStations.CARGO)) {
			BlockPos at = core.getBlockPos().offset(ExchangeStations.masterOffset(block, true));
			var station = (ExchangeStationBlockEntity) h.getLevel().getBlockEntity(at);
			h.assertTrue(station != null && station.formed() && station.terminal() == terminal,
					"removable paving is separate from the complete station and its exact terminal link");
		}
	}

	@GameTest(maxTicks = 1800, skyAccess = true)
	public void tierOneConstructsItsOwnPavingAndBacking(GameTestHelper h) {
		if (skip(h)) return;
		var core = core(h, 0); var level = h.getLevel();
		var owner = h.makeMockPlayer(GameType.CREATIVE);
		Set<BlockPos> optional = Set.copyOf(cosmeticApron());
		h.assertTrue(optional.size() > 5000, "both removable outer paving and backing are advertised");
		for (int x : new int[] {-40, 40}) for (int z : new int[] {-40, 40}) {
			h.assertTrue(optional.contains(new BlockPos(x, 0, z)) && optional.contains(new BlockPos(x, -1, z)),
					"outer corners belong to the ordinary removable apron");
			level.setBlock(core.getBlockPos().offset(x, 0, z), Blocks.DIRT.defaultBlockState(), Block.UPDATE_ALL);
		}
		modules(core); core.tryUpgrade(owner);
		h.assertTrue(core.building() == 1, "normal paid tier-one construction starts");
		int cells = core.layout().tier(1).place.length + core.layout().tier(1).air.length;
		h.assertTrue((cells + TowerCoreBlockEntity.CORE_BLOCKS_PER_TICK - 1) / TowerCoreBlockEntity.CORE_BLOCKS_PER_TICK < 1700,
				"the asynchronous construction timeout follows the actual bounded placement budget");
		h.succeedWhen(() -> {
			h.assertTrue(core.tier() == 1 && core.building() == 0, "normal construction completes");
			for (int x : new int[] {-40, 40}) for (int z : new int[] {-40, 40}) for (int y : new int[] {-1, 0}) {
				BlockPos offset = new BlockPos(x, y, z);
				h.assertTrue(level.getBlockState(core.getBlockPos().offset(offset)).is(expected(core, offset).getBlock()),
						"the tower itself constructs paving/backing across chunk edges " + offset);
			}
			workingDepot(h, core);
		});
	}

	@GameTest(maxTicks = 1800, skyAccess = true)
	public void processedApronCellsStayCustomizedAfterTierOneResume(GameTestHelper h) {
		if (skip(h)) return;
		var core = core(h, 2); var level = h.getLevel();
		var owner = h.makeMockPlayer(GameType.CREATIVE); modules(core); core.tryUpgrade(owner);
		var first = core.layout().tier(1); Set<BlockPos> optional = Set.copyOf(cosmeticApron());
		List<Integer> top = new ArrayList<>();
		for (int i = 0; i < first.place.length && top.size() < 3; i++) {
			int[] b = first.place[i];
			if (b[1] == 0 && optional.contains(new BlockPos(b[0], b[1], b[2]))) top.add(i);
		}
		h.assertTrue(top.size() == 3, "three ordered optional paving cells are available");
		int lastProcessed = top.get(1);
		for (int i = 0; i <= lastProcessed; i++) {
			int[] b = first.place[i]; BlockPos at = core.getBlockPos().offset(b[0], b[1], b[2]);
			level.setBlock(at, TowerPanelBlock.at(core.layout().state(b[3]), at), Block.UPDATE_ALL);
		}
		BlockPos gone = new BlockPos(first.place[top.get(0)][0], 0, first.place[top.get(0)][2]);
		BlockPos custom = new BlockPos(first.place[top.get(1)][0], 0, first.place[top.get(1)][2]);
		BlockPos pending = new BlockPos(first.place[top.get(2)][0], 0, first.place[top.get(2)][2]);
		level.setBlock(core.getBlockPos().offset(gone), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
		level.setBlock(core.getBlockPos().offset(gone.below()), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
		level.setBlock(core.getBlockPos().offset(custom), Blocks.POLISHED_DEEPSLATE.defaultBlockState(), Block.UPDATE_ALL);
		var saved = core.saveWithoutMetadata(level.registryAccess());
		saved.putInt("progress", first.air.length + lastProcessed + 1);
		core.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), saved));
		saveAndReload(level, core);
		h.succeedWhen(() -> {
			h.assertTrue(core.tier() == 1 && core.building() == 0, "unfinished tier one resumes to completion");
			h.assertTrue(level.getBlockState(core.getBlockPos().offset(gone)).isAir()
					&& level.getBlockState(core.getBlockPos().offset(gone.below())).isAir(), "processed paving and backing are never reconstructed");
			h.assertTrue(level.getBlockState(core.getBlockPos().offset(custom)).is(Blocks.POLISHED_DEEPSLATE), "the player's replacement survives resume");
			h.assertTrue(level.getBlockState(core.getBlockPos().offset(pending)).is(expected(core, pending).getBlock()),
					"unprocessed paving is still laid during its original construction");
			workingDepot(h, core);
		});
	}

	@GameTest(maxTicks = 100, skyAccess = true)
	public void removedApronSurvivesReloadAndUpgradeWithAThreeWideEntrance(GameTestHelper h) {
		if (skip(h)) return;
		var core = core(h, 3); var level = h.getLevel();
		var owner = h.makeMockPlayer(GameType.CREATIVE); core.setOwner(owner.getUUID()); core.buildInstantly(level, 1);
		assertEntrance(h, core);
		List<BlockPos> optional = cosmeticApron();
		for (BlockPos p : optional) level.setBlock(core.getBlockPos().offset(p), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
		BlockPos custom = optional.stream().filter(p -> p.getY() == 0).findFirst().orElseThrow();
		level.setBlock(core.getBlockPos().offset(custom), Blocks.POLISHED_DEEPSLATE.defaultBlockState(), Block.UPDATE_ALL);
		saveAndReload(level, core); modules(core); core.tryUpgrade(owner);
		h.assertTrue(core.building() == 2, "normal paid upgrade starts after paving removal and reload");
		for (int pass = 0; pass < 256 && core.building() == 2; pass++) {
			List<BuildJobs.Target> targets = BuildJobs.sources().stream()
					.flatMap(s -> s.openTargets(level, core.getBlockPos(), 250, owner.getUUID(), UseMode.PERSONAL, 512).stream())
					.filter(t -> t.owner().equals(owner.getUUID())).toList();
			for (var target : targets) h.assertTrue(target.source().fill(level, target, target.state()), "normal tower delivery completes its paid tile");
			core.serverTick(level);
		}
		h.assertTrue(core.tier() == 2 && core.building() == 0, "ordinary build jobs finish tier two without an apron requirement");
		assertEntrance(h, core);
		core.buildInstantly(level, 9); saveAndReload(level, core);
		for (BlockPos p : optional) h.assertTrue(p.equals(custom)
				? level.getBlockState(core.getBlockPos().offset(p)).is(Blocks.POLISHED_DEEPSLATE)
				: level.getBlockState(core.getBlockPos().offset(p)).isAir(), "later tiers never recreate or overwrite customized outer paving " + p);
		h.assertTrue(core.tier() == 9 && core.layout().tier(9).capacity == 133, "the complete fleet capacity survives paving removal");
		workingDepot(h, core); assertEntrance(h, core);
		var current = core.saveWithoutMetadata(level.registryAccess());
		for (int version : new int[] {TowerData.LEGACY_LAYOUT, TowerData.SERVICE_LAYOUT, TowerData.LARGE_INPUT_LAYOUT, TowerData.APRON_LAYOUT, TowerData.INDUSTRIAL_LAYOUT}) {
			var tagged = current.copy(); tagged.putInt("layout_version", version);
			core.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), tagged));
			h.assertTrue(TowerCoreBlockEntity.footprintRadius(level, core.getBlockPos()) == (version >= 6 ? 40 : version == 5 ? 32 : 24),
					"saved versions keep their exact forced-chunk footprint " + version);
		}
		core.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), current));
		h.succeed();
	}

	private static void assertEntrance(GameTestHelper h, TowerCoreBlockEntity core) {
		var level = h.getLevel();
		for (int x = -1; x <= 1; x++) for (int z = 8; z <= 20; z++) {
			BlockPos floor = core.getBlockPos().offset(x, 0, z);
			var support = level.getBlockState(floor).getCollisionShape(level, floor);
			h.assertTrue(!support.isEmpty() && support.bounds().maxY >= 1 - 1e-6, "the three-wide entrance has a real continuous floor " + floor);
			for (int y = 1; y <= 3; y++) {
				BlockPos at = floor.above(y);
				h.assertTrue(level.getBlockState(at).getCollisionShape(level, at).isEmpty(), "no pillar blocks the actual three-wide entrance " + at);
			}
		}
	}

	@GameTest(maxTicks = 100, skyAccess = true)
	public void savedApronLayoutKeepsItsOriginalTierOneLightCursor(GameTestHelper h) {
		if (skip(h)) return;
		var core = core(h, 4); var level = h.getLevel();
		var frozen = TowerData.get(TowerData.APRON_LAYOUT); var first = frozen.tier(1);
		int lamp = -1;
		for (int i = first.place.length / 2; i < first.place.length; i++) {
			int[] cell = first.place[i];
			if (cell[1] == 0 && frozen.state(cell[3]).is(JugcraftTower.BLOCKS.get("red_light_strip"))) { lamp = i; break; }
		}
		h.assertTrue(lamp > 0 && lamp + TowerCoreBlockEntity.CORE_BLOCKS_PER_TICK < first.place.length,
				"the original apron provides an unfinished ground-light cursor");
		int[] before = first.place[lamp - 1];
		BlockPos customized = core.getBlockPos().offset(before[0], before[1], before[2]);
		level.setBlock(customized, Blocks.DIAMOND_BLOCK.defaultBlockState(), Block.UPDATE_ALL);
		var saved = core.saveWithoutMetadata(level.registryAccess());
		saved.putInt("layout_version", TowerData.APRON_LAYOUT); saved.putInt("tier", 0); saved.putInt("building", 1);
		saved.putInt("progress", first.air.length + lamp);
		core.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), saved));
		saveAndReload(level, core);
		h.assertTrue(core.layout() == frozen && core.layoutVersion() == TowerData.APRON_LAYOUT,
				"saved layout six resumes its immutable design rather than the reduced-light design");
		core.serverTick(level);
		for (int i = lamp; i < lamp + TowerCoreBlockEntity.CORE_BLOCKS_PER_TICK; i++) {
			int[] cell = first.place[i]; BlockPos at = core.getBlockPos().offset(cell[0], cell[1], cell[2]);
			h.assertTrue(level.getBlockState(at).equals(TowerPanelBlock.at(frozen.state(cell[3]), at)),
					"the resumed tick uses the original ordered cell and light state " + i);
		}
		h.assertTrue(level.getBlockState(customized).is(Blocks.DIAMOND_BLOCK), "already processed paving is not revisited");
		h.assertTrue(core.saveWithoutMetadata(level.registryAccess()).getIntOr("progress", -1)
				== first.air.length + lamp + TowerCoreBlockEntity.CORE_BLOCKS_PER_TICK,
				"save/reload preserves and advances the old placement cursor exactly");
		h.succeed();
	}
}
