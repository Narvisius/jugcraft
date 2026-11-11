package io.github.jimbozoomer.jugcraft.test;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.jimbozoomer.jugcraft.drone.CargoPackagerBlockEntity;
import io.github.jimbozoomer.jugcraft.drone.DroneTerminalBlockEntity;
import io.github.jimbozoomer.jugcraft.drone.DroneTier;
import io.github.jimbozoomer.jugcraft.drone.JugcraftDrones;
import io.github.jimbozoomer.jugcraft.tower.ExchangeStationBlockEntity;
import io.github.jimbozoomer.jugcraft.tower.JugcraftTower;
import io.github.jimbozoomer.jugcraft.tower.TowerCoreBlockEntity;
import io.github.jimbozoomer.jugcraft.tower.TowerData;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.TicketStorage;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

/**
 * In-game tests for a saved tower of the industrial design (layout 7) becoming the Tri-Pillar
 * (TowerCoreBlockEntity: startConversion, convertTick and endConversion). The core converts on its first server
 * tick. A core that never built anything just takes the new design. A built tower is cleared and rebuilt to the
 * same tier, and its depot carries over.
 */
public class TowerConversionGameTests {
	/** The design the saved towers in these tests were built in. */
	private static final int OLD = TowerData.INDUSTRIAL_LAYOUT;
	/** The old terminal's charge, and how much of it standby drones may use up before it is checked. */
	private static final long ENERGY = 150_000, DRAIN = 10_000;

	/** An industrial core that never built anything is the Tri-Pillar after one tick: nothing is cleared, built or started. */
	@GameTest(structure = TriPillarArena.ARENA, maxTicks = 100, skyAccess = true)
	public void unbuiltOldCoreTakesTheNewDesign(GameTestHelper helper) {
		if (TestFocus.skip("tower")) {
			helper.succeed();
			return;
		}
		ServerLevel level = helper.getLevel();
		TowerCoreBlockEntity core = TriPillarArena.placeCore(helper);
		BlockPos pos = core.getBlockPos();
		saveAs(level, core, OLD, 0, 0);
		TowerData old = TowerData.get(OLD);
		helper.assertTrue(core.layoutVersion() == OLD && core.layout() == old && !core.converting(), "the core was saved in the industrial design");
		// A block of the old design where that design puts it: a conversion that cleared would take it away.
		int[] mark = Arrays.stream(old.tier(1).place).filter(b -> b[1] >= 1 && !(old.state(b[3]).getBlock() instanceof EntityBlock))
				.findFirst().orElseThrow();
		BlockPos markPos = pos.offset(mark[0], mark[1], mark[2]);
		Block markBlock = old.state(mark[3]).getBlock();
		level.setBlock(markPos, old.state(mark[3]), 3);
		helper.runAfterDelay(1, () -> {
			helper.assertTrue(core.layoutVersion() == TowerData.CURRENT_LAYOUT && core.layout() == TowerData.get(),
					"after one tick the core is the Tri-Pillar, got layout " + core.layoutVersion());
			helper.assertTrue(!core.converting() && core.tier() == 0 && core.building() == 0 && core.foundationState() == 0,
					"there was nothing to convert: tier " + core.tier() + ", building " + core.building() + ", foundation " + core.foundationState());
			helper.assertTrue(core.needsFoundation() && core.nextStep() == core.layout().foundation, "its next step is the Tri-Pillar's foundation");
		});
		helper.runAfterDelay(40, () -> {
			helper.assertTrue(level.getBlockState(markPos).is(markBlock), "nothing was cleared: " + level.getBlockState(markPos));
			helper.assertTrue(!level.getBlockState(TriPillarArena.at(core, TriPillarArena.TERMINAL)).is(JugcraftDrones.TERMINAL) && core.terminal() == null,
					"nothing was built");
			helper.assertTrue(!core.converting() && core.foundationState() == 0 && core.buildProgress() == 0, "and nothing started");
			helper.succeed();
		});
	}

	/**
	 * An industrial tower at tier 1, building its tier 2, with 26 drones, power, items and a stocked core, is rebuilt as
	 * the Tri-Pillar at tier 1. The tier 2 it had paid for comes back as the Tri-Pillar tier 2's blocks in the core's store,
	 * its new terminal takes the depot over (24 drones on the roof, the
	 * other two handed back as items), its items reach the new packager, nothing of the old design is left outside the
	 * new one and the exchanges are bound.
	 */
	@GameTest(structure = TriPillarArena.ARENA, maxTicks = 1200, skyAccess = true)
	public void industrialTowerIsRebuiltAsTheTriPillar(GameTestHelper helper) {
		if (TestFocus.skip("tower")) {
			helper.succeed();
			return;
		}
		Player owner = helper.makeMockPlayer(GameType.CREATIVE);
		ServerLevel level = helper.getLevel();
		TowerCoreBlockEntity core = TriPillarArena.placeCore(helper);
		BlockPos pos = core.getBlockPos();
		core.setOwner(owner.getUUID());
		saveAs(level, core, OLD, 0, 0);
		core.buildInstantly(level, 1);
		TowerData old = TowerData.get(OLD);
		helper.assertTrue(core.layoutVersion() == OLD && core.layout() == old && core.tier() == 1, "an industrial tower at tier 1");
		DroneTerminalBlockEntity oldTerminal = core.terminal();
		helper.assertTrue(oldTerminal != null && oldTerminal.getBlockPos().equals(pos.offset(old.terminal[0], old.terminal[1], old.terminal[2])),
				"the industrial command post has its terminal");
		int capacity = TowerData.get().capacity(1);
		int drones = capacity + 2;
		helper.assertTrue(drones <= old.capacity(1), "the industrial tier 1 holds more drones than the Tri-Pillar's roof pads");
		for (int i = 0; i < drones; i++) {
			helper.assertTrue(oldTerminal.linkDrone(owner, DroneTier.COURIER_QUAD), "drone " + (i + 1) + " links to the industrial depot");
		}
		oldTerminal.energy().setAmount(ENERGY);
		CargoPackagerBlockEntity oldPackager = oldTerminal.packagerEntity();
		helper.assertTrue(oldPackager != null, "the industrial depot has its packager");
		oldPackager.setItem(0, new ItemStack(Items.STONE_BRICKS, 64));
		oldPackager.setItem(1, new ItemStack(Items.IRON_INGOT, 20));
		TriPillarArena.stock(core);
		Map<Item, Integer> stocked = new HashMap<>(core.store());
		core.tryUpgrade(owner);
		helper.assertTrue(core.building() == 2, "the industrial tower is building its tier 2");
		for (var c : old.cost(old.tier(2)).entrySet()) {
			helper.assertTrue(core.stored(c.getKey()) == stocked.get(c.getKey()) - c.getValue(), "tier 2's blocks were paid: " + c.getKey());
		}
		Map<Item, Integer> afterPaying = new HashMap<>(core.store());
		// Where the Tri-Pillar's foundation and roof pads put blocks (relative to the core).
		TowerData tri = TowerData.get();
		Map<BlockPos, Block> rebuilt = new HashMap<>();
		for (TowerData.Tier step : List.of(tri.foundation, tri.tier(1))) {
			for (int[] b : step.place) {
				rebuilt.put(new BlockPos(b[0], b[1], b[2]), tri.state(b[3]).getBlock());
			}
		}
		Item drone = JugcraftDrones.DRONES.get(DroneTier.COURIER_QUAD);
		helper.startSequence()
				.thenWaitUntil(() -> helper.assertTrue(!core.converting() && core.layoutVersion() == TowerData.CURRENT_LAYOUT,
						"the tower is rebuilt (converting " + core.converting() + ", progress " + core.buildProgress() + ")"))
				.thenExecute(() -> {
					helper.assertTrue(level.getBlockEntity(pos) == core, "the core stays where it was");
					helper.assertTrue(core.tier() == 1 && core.foundationState() == 2 && core.building() == 0,
							"rebuilt to the old tier: tier " + core.tier() + ", foundation " + core.foundationState() + ", building " + core.building());
					TowerData now = TowerData.get();
					for (var c : now.cost(now.tier(2)).entrySet()) {
						helper.assertTrue(core.stored(c.getKey()) == afterPaying.getOrDefault(c.getKey(), 0) + c.getValue(),
								"the tier 2 the old tower was building comes back as the Tri-Pillar tier 2's blocks: " + c.getKey()
										+ " " + core.stored(c.getKey()));
					}
					DroneTerminalBlockEntity terminal = core.terminal();
					helper.assertTrue(terminal != null && terminal != oldTerminal
							&& terminal.getBlockPos().equals(TriPillarArena.at(core, TriPillarArena.TERMINAL)), "dispatch has the new terminal");
					helper.assertTrue(owner.getUUID().equals(terminal.owner()) && terminal.tower() == core, "it is the owner's, and the tower's");
					helper.assertTrue(terminal.fleet().size() == capacity && terminal.maxDrones() == capacity
							&& terminal.fleet().drones().stream().allMatch(tier -> tier == DroneTier.COURIER_QUAD),
							"the new depot took " + capacity + " of the drones, got " + terminal.fleet().size());
					long energy = terminal.energy().getAmount();
					helper.assertTrue(energy <= ENERGY && energy > ENERGY - DRAIN, "the depot's charge carried over: " + energy);
					CargoPackagerBlockEntity packager = terminal.packagerEntity();
					helper.assertTrue(packager != null && packager.getBlockPos().equals(TriPillarArena.at(core, TriPillarArena.PACKAGER)),
							"the new depot has its packager");
					BlockPos at = terminal.getBlockPos();
					helper.assertTrue(countNear(level, at, packager, Items.STONE_BRICKS) == 64 && countNear(level, at, packager, Items.IRON_INGOT) == 20,
							"the packager's items carried over (in the new packager or left on the terminal)");
					helper.assertTrue(countNear(level, at, null, drone) == drones - capacity,
							"the drones with no dock were handed back on the terminal, got " + countNear(level, at, null, drone));
					helper.assertTrue(!(level.getBlockEntity(oldTerminal.getBlockPos()) instanceof DroneTerminalBlockEntity), "the old terminal is gone");
					int left = 0;
					BlockPos first = null;
					for (int[] b : old.tier(1).place) {
						BlockPos rel = new BlockPos(b[0], b[1], b[2]);
						if (rel.equals(BlockPos.ZERO) || rebuilt.containsKey(rel)) {
							continue;
						}
						if (level.getBlockState(pos.offset(rel)).is(old.state(b[3]).getBlock())) {
							left++;
							first = first == null ? rel : first;
						}
					}
					helper.assertTrue(left == 0, left + " blocks of the old design are left where the Tri-Pillar has none, first at " + first);
					for (int dx = -7; dx <= 7; dx++) {
						for (int dz = -7; dz <= 7; dz++) {
							BlockPos rel = new BlockPos(dx, 0, dz);
							if (rel.equals(BlockPos.ZERO)) {
								continue;
							}
							Block floor = rebuilt.get(rel);
							helper.assertTrue(floor != null && level.getBlockState(pos.offset(rel)).is(floor),
									"the core stands in the hall floor: " + rel + " is " + level.getBlockState(pos.offset(rel)));
						}
					}
					for (boolean power : new boolean[] {true, false}) {
						BlockPos master = TriPillarArena.master(core, power);
						helper.assertTrue(level.getBlockEntity(master) instanceof ExchangeStationBlockEntity station && station.terminal() == terminal,
								"the " + (power ? "Energy" : "Cargo") + " Exchange is bound to the new terminal");
					}
					// the control building: its blocks the roof pads (tier 1, rebuilt too) don't put something else in place of
					// (they lay their pads and walkways on its roof slab)
					TowerData.Tier foundation = tri.foundation;
					java.util.Set<BlockPos> roofed = new java.util.HashSet<>();
					for (int[] b : tri.tier(1).place) roofed.add(new BlockPos(b[0], b[1], b[2]));
					for (int[] b : tri.tier(1).clear) roofed.add(new BlockPos(b[0], b[1], b[2]));
					int standing = 0, building = 0;
					for (int[] b : foundation.place) {
						BlockPos rel = new BlockPos(b[0], b[1], b[2]);
						if (roofed.contains(rel)) continue;
						building++;
						if (level.getBlockState(pos.offset(rel)).is(tri.state(b[3]).getBlock())) standing++;
					}
					helper.assertTrue(standing * 100L >= building * 99L, "the control building stands: " + standing + " of " + building
							+ " blocks; missing " + TriPillarArena.missing(level, core, foundation));
					helper.assertTrue(TriPillarArena.countPlaced(level, core, tri.tier(1)) == tri.tier(1).place.length, "the roof pads are in");
				})
				.thenSucceed();
	}

	/** {@code /dronetest build 1} refuses a core that is being rebuilt as the Tri-Pillar, and leaves it as it was. */
	@GameTest(structure = TriPillarArena.ARENA, maxTicks = 600, skyAccess = true)
	public void buildCommandRefusesAConvertingCore(GameTestHelper helper) {
		if (TestFocus.skip("tower")) {
			helper.succeed();
			return;
		}
		ServerLevel level = helper.getLevel();
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		TowerCoreBlockEntity core = TriPillarArena.placeCore(helper);
		BlockPos pos = core.getBlockPos();
		core.setOwner(player.getUUID());
		player.setPos(pos.getX() + .5, pos.getY() + 1, pos.getZ() + .5);
		// Saved at tier 1 of the industrial design: it converts. None of its old blocks are here, so the clearing is quick.
		saveAs(level, core, OLD, 1, 0);
		helper.startSequence()
				.thenWaitUntil(() -> helper.assertTrue(core.converting() && core.layoutVersion() == TowerData.CURRENT_LAYOUT,
						"the core is building the Tri-Pillar (converting " + core.converting() + ", layout " + core.layoutVersion() + ")"))
				.thenExecute(() -> {
					int tier = core.tier();
					int foundation = core.foundationState();
					int building = core.building();
					TowerData.Tier roof = core.layout().tier(1);
					int roofBefore = TriPillarArena.countPlaced(level, core, roof);
					int result = run(level, player, "dronetest build 1");
					helper.assertTrue(result == 0, "/dronetest build 1 refuses a converting core, returned " + result);
					helper.assertTrue(level.getBlockEntity(pos) == core && core.converting() && core.layoutVersion() == TowerData.CURRENT_LAYOUT
							&& core.tier() == tier && core.foundationState() == foundation && core.building() == building,
							"the refused command leaves the core as it was");
					helper.assertTrue(TriPillarArena.countPlaced(level, core, roof) == roofBefore, "no roof pad was put in");
					// The test is done with it: stop the rebuild.
					level.removeBlock(pos, false);
				})
				.thenSucceed();
	}

	/**
	 * While a core rebuilds an industrial tower its site is the Tri-Pillar's: it reports converting, its footprint
	 * radius is 64 (the industrial one was 40) and its chunks out to there are kept loaded.
	 */
	@GameTest(structure = TriPillarArena.ARENA, maxTicks = 100, skyAccess = true)
	public void convertingCoreKeepsTheTriPillarFootprint(GameTestHelper helper) {
		if (TestFocus.skip("tower")) {
			helper.succeed();
			return;
		}
		Player owner = helper.makeMockPlayer(GameType.CREATIVE);
		ServerLevel level = helper.getLevel();
		TowerCoreBlockEntity core = TriPillarArena.placeCore(helper);
		BlockPos pos = core.getBlockPos();
		core.setOwner(owner.getUUID());
		saveAs(level, core, OLD, 0, 0);
		core.buildInstantly(level, 1);
		helper.assertTrue(!core.converting() && core.tier() == 1 && TowerCoreBlockEntity.footprintRadius(level, pos) == TowerCoreBlockEntity.APRON_FOOTPRINT,
				"an industrial tier 1 tower keeps " + TowerCoreBlockEntity.APRON_FOOTPRINT + " blocks round it, got "
						+ TowerCoreBlockEntity.footprintRadius(level, pos));
		helper.runAfterDelay(1, () -> {
			helper.assertTrue(core.converting(), "on its first tick the core starts rebuilding");
			helper.assertTrue(core.layoutVersion() == OLD && core.buildProgress() < 0.3f,
					"it is still clearing the old blocks (layout " + core.layoutVersion() + ", progress " + core.buildProgress() + ")");
			helper.assertTrue(TowerCoreBlockEntity.footprintRadius(level, pos) == TowerCoreBlockEntity.TRIPILLAR_FOOTPRINT,
					"while converting its footprint is the Tri-Pillar's, got " + TowerCoreBlockEntity.footprintRadius(level, pos));
			long far = ChunkPos.containing(pos.offset(TowerCoreBlockEntity.TRIPILLAR_FOOTPRINT, 0, 0)).pack();
			var tickets = level.getDataStorage().computeIfAbsent(TicketStorage.TYPE);
			helper.assertTrue(tickets.getTickets(far).stream().anyMatch(ticket -> ticket.getType() == TowerCoreBlockEntity.CHUNK_TICKET),
					"its chunks out to " + TowerCoreBlockEntity.TRIPILLAR_FOOTPRINT + " blocks are kept loaded");
			// The test is done with it: stop the rebuild.
			level.removeBlock(pos, false);
			helper.succeed();
		});
	}

	// ------------------------------------------------------------------ helpers

	/** Reloads {@code core} as if it had been saved in {@code layout} at {@code tier}, building {@code building}. */
	/**
	 * A Tri-Pillar of the first revision (layout 8: the 3x3 holo table, five touch consoles, the master console at z -6) at
	 * tier 1 is refitted as the current one (the Holo Command Table and the Tower Control Console since layout 10): the old
	 * table, touch consoles and master console are gone, the new table and console stand whole and pinned, a storage rack
	 * that is in both keeps what it held, and the depot keeps its drones.
	 */
	@GameTest(structure = TriPillarArena.ARENA, maxTicks = 1200, skyAccess = true)
	public void firstTriPillarIsRefittedWithTheHoloCommandTable(GameTestHelper helper) {
		if (TestFocus.skip("tower")) {
			helper.succeed();
			return;
		}
		Player owner = helper.makeMockPlayer(GameType.CREATIVE);
		ServerLevel level = helper.getLevel();
		TowerCoreBlockEntity core = TriPillarArena.placeCore(helper);
		BlockPos pos = core.getBlockPos();
		core.setOwner(owner.getUUID());
		saveAs(level, core, TowerData.TRIPILLAR_LAYOUT, 0, 0);
		core.buildInstantly(level, 1);
		TowerData old = TowerData.get(TowerData.TRIPILLAR_LAYOUT);
		helper.assertTrue(core.layoutVersion() == TowerData.TRIPILLAR_LAYOUT && core.layout() == old && core.tier() == 1,
				"a first-revision Tri-Pillar at tier 1");
		Block touch = io.github.jimbozoomer.jugcraft.test.TowerGameTests.modBlock("touch_console");
		List<BlockPos> consoles = new java.util.ArrayList<>();
		BlockPos rack = null;
		for (int[] b : old.foundation.place) {
			Block block = old.state(b[3]).getBlock();
			if (block == touch) {
				consoles.add(pos.offset(b[0], b[1], b[2]));
			}
			BlockPos at = pos.offset(b[0], b[1], b[2]);
			if (rack == null && block == io.github.jimbozoomer.jugcraft.test.TowerGameTests.modBlock("storage_rack") && level.getBlockEntity(at) instanceof net.minecraft.world.Container) {
				rack = at;
			}
		}
		helper.assertTrue(consoles.size() == 5 && level.getBlockState(consoles.get(0)).is(touch), "the old hall has its five touch consoles");
		helper.assertTrue(level.getBlockState(pos.offset(0, 1, 0)).is(JugcraftDrones.HOLO_TABLE)
				&& level.getBlockState(pos.offset(0, 1, -6)).is(JugcraftTower.MASTER_CONSOLE), "and its 3x3 table and master console at z -6");
		BlockPos rackPos = rack;
		helper.assertTrue(rackPos != null && level.getBlockEntity(rackPos) instanceof net.minecraft.world.Container, "a storage rack in the service bay");
		((net.minecraft.world.Container) level.getBlockEntity(rackPos)).setItem(0, new ItemStack(Items.IRON_INGOT, 7));
		DroneTerminalBlockEntity terminal = core.terminal();
		for (int i = 0; i < 3; i++) {
			helper.assertTrue(terminal != null && terminal.linkDrone(owner, DroneTier.COURIER_QUAD), "drone " + (i + 1) + " links");
		}
		BlockPos table = pos.offset(TriPillarArena.CENTRAL_TABLE);
		helper.startSequence()
				.thenWaitUntil(() -> helper.assertTrue(!core.converting() && core.layoutVersion() == TowerData.CURRENT_LAYOUT,
						"the tower is refitted (converting " + core.converting() + ", progress " + core.buildProgress() + ")"))
				.thenExecute(() -> {
					helper.assertTrue(core.tier() == 1 && core.foundationState() == 2, "still at tier 1 with its foundation");
					var block = (io.github.jimbozoomer.jugcraft.tower.HoloCommandTableBlock) JugcraftTower.HOLO_COMMAND_TABLE;
					helper.assertTrue(level.getBlockState(table).is(block) && block.complete(level, table),
							"the Holo Command Table stands whole over the core: " + level.getBlockState(table));
					helper.assertTrue(level.getBlockEntity(table) instanceof io.github.jimbozoomer.jugcraft.drone.DepotDisplayBlockEntity display
							&& core.terminalPos().equals(display.terminal()), "and is pinned to the tower's terminal");
					for (BlockPos c : consoles) {
						BlockState here = level.getBlockState(c);
						helper.assertTrue(!here.is(touch), "the touch console at " + c + " is gone: " + here);
					}
					var console = (io.github.jimbozoomer.jugcraft.tower.TowerControlConsoleBlock) JugcraftTower.TOWER_CONTROL_CONSOLE;
					BlockPos consolePos = pos.offset(TriPillarArena.MASTER_CONSOLE);
					helper.assertTrue(level.getBlockState(pos.offset(0, 1, -6)).isAir() && level.getBlockState(consolePos).is(console)
							&& console.complete(level, consolePos, net.minecraft.core.Direction.NORTH),
							"the master console at z -6 is gone, the Tower Control Console stands whole north of the table");
					// (the storage racks went from the design with the parts shelving: what the rack held comes back on the terminal)
					DroneTerminalBlockEntity now = core.terminal();
					boolean kept = level.getBlockEntity(rackPos) instanceof net.minecraft.world.Container store && store.getItem(0).is(Items.IRON_INGOT)
							&& store.getItem(0).getCount() == 7;
					helper.assertTrue(kept || now != null && countNear(level, now.getBlockPos(), null, Items.IRON_INGOT) == 7,
							"the storage rack's iron is kept: in the rack if it's in both designs, else handed back on the terminal");
					helper.assertTrue(now != null && now.fleet().size() == 3, "the depot kept its three drones");
					helper.succeed();
				});
	}

	/**
	 * A Tri-Pillar saved in layout 9 (the Holo Command Table, the old master console at z -10) at tier 1 is refitted as
	 * layout 10: the old master console is gone, the Tower Control Console stands whole and pinned to the tower's terminal
	 * on its floor band, the raised floor and the three console chairs (on it) stand behind it, the Holo Command Table (in
	 * both designs) is left standing, and the depot keeps its drones.
	 */
	@GameTest(structure = TriPillarArena.ARENA, maxTicks = 1200, skyAccess = true)
	public void holoTableTriPillarIsRefittedWithTheControlConsole(GameTestHelper helper) {
		if (TestFocus.skip("tower")) {
			helper.succeed();
			return;
		}
		Player owner = helper.makeMockPlayer(GameType.CREATIVE);
		ServerLevel level = helper.getLevel();
		TowerCoreBlockEntity core = TriPillarArena.placeCore(helper);
		BlockPos pos = core.getBlockPos();
		core.setOwner(owner.getUUID());
		saveAs(level, core, TowerData.TRIPILLAR_HOLO_TABLE_LAYOUT, 0, 0);
		core.buildInstantly(level, 1);
		helper.assertTrue(core.layoutVersion() == TowerData.TRIPILLAR_HOLO_TABLE_LAYOUT && core.tier() == 1, "a layout-9 Tri-Pillar at tier 1");
		helper.assertTrue(level.getBlockState(pos.offset(TriPillarArena.OLD_MASTER_CONSOLE)).is(JugcraftTower.MASTER_CONSOLE),
				"with its old master console at z -10");
		DroneTerminalBlockEntity terminal = core.terminal();
		for (int i = 0; i < 2; i++) {
			helper.assertTrue(terminal != null && terminal.linkDrone(owner, DroneTier.COURIER_QUAD), "drone " + (i + 1) + " links");
		}
		BlockPos table = pos.offset(TriPillarArena.CENTRAL_TABLE);
		helper.startSequence()
				.thenWaitUntil(() -> helper.assertTrue(!core.converting() && core.layoutVersion() == TowerData.CURRENT_LAYOUT,
						"the tower is refitted (converting " + core.converting() + ", progress " + core.buildProgress() + ")"))
				.thenExecute(() -> {
					helper.assertTrue(core.tier() == 1 && core.foundationState() == 2, "still at tier 1 with its foundation");
					var console = (io.github.jimbozoomer.jugcraft.tower.TowerControlConsoleBlock) JugcraftTower.TOWER_CONTROL_CONSOLE;
					BlockPos consolePos = pos.offset(TriPillarArena.MASTER_CONSOLE);
					helper.assertTrue(level.getBlockState(consolePos).is(console) && console.complete(level, consolePos, net.minecraft.core.Direction.NORTH),
							"the Tower Control Console stands whole: " + level.getBlockState(consolePos));
					helper.assertTrue(level.getBlockEntity(consolePos) instanceof io.github.jimbozoomer.jugcraft.drone.DepotDisplayBlockEntity display
							&& core.terminalPos().equals(display.terminal()), "and is pinned to the tower's terminal");
					helper.assertTrue(!level.getBlockState(pos.offset(TriPillarArena.OLD_MASTER_CONSOLE)).is(JugcraftTower.MASTER_CONSOLE),
							"the old master console is gone");
					for (BlockPos chair : TriPillarArena.CONSOLE_CHAIRS) {
						BlockState c = level.getBlockState(pos.offset(chair));
						helper.assertTrue(c.getBlock() instanceof io.github.jimbozoomer.jugcraft.tower.ConsoleChairBlock
								&& c.getValue(io.github.jimbozoomer.jugcraft.tower.ConsoleChairBlock.PLATFORM), "a console chair on the raised floor at " + chair);
					}
					helper.assertTrue(level.getBlockState(pos.offset(TriPillarArena.FLOOR_BACK_STEP)).getBlock()
							instanceof io.github.jimbozoomer.jugcraft.tower.RaisedFloorBlock, "the raised floor's back step");
					var holo = (io.github.jimbozoomer.jugcraft.tower.HoloCommandTableBlock) JugcraftTower.HOLO_COMMAND_TABLE;
					helper.assertTrue(level.getBlockState(table).is(holo) && holo.complete(level, table), "the Holo Command Table still stands whole");
					DroneTerminalBlockEntity now = core.terminal();
					helper.assertTrue(now != null && now.fleet().size() == 2, "the depot kept its two drones");
					helper.succeed();
				});
	}

	/**
	 * A Tri-Pillar saved in layout 10 (the pillar feet whole, 3x2 control screens on their faces, the riser ladders in the
	 * feet's inner corners) at tier 1 is refitted as layout 11: the feet are cut back round the rotunda, the screens and the
	 * ladders' feet in the cut are gone, a Curved Video Wall stands whole on each cut face, aimed at the core and pinned to
	 * the tower's terminal, the riser ladder goes up in each foot's outer corner, the console (in both designs) still
	 * stands, and the depot keeps its drones.
	 */
	@GameTest(structure = TriPillarArena.ARENA, maxTicks = 1200, skyAccess = true)
	public void consoleTriPillarIsRefittedWithTheVideoWalls(GameTestHelper helper) {
		if (TestFocus.skip("tower")) {
			helper.succeed();
			return;
		}
		Player owner = helper.makeMockPlayer(GameType.CREATIVE);
		ServerLevel level = helper.getLevel();
		TowerCoreBlockEntity core = TriPillarArena.placeCore(helper);
		BlockPos pos = core.getBlockPos();
		core.setOwner(owner.getUUID());
		saveAs(level, core, TowerData.TRIPILLAR_CONSOLE_LAYOUT, 0, 0);
		core.buildInstantly(level, 1);
		helper.assertTrue(core.layoutVersion() == TowerData.TRIPILLAR_CONSOLE_LAYOUT && core.tier() == 1, "a layout-10 Tri-Pillar at tier 1");
		BlockPos screen = pos.offset(-5, 4, -9), oldLadder = pos.offset(TriPillarArena.OLD_RISER_LADDER);
		helper.assertTrue(level.getBlockState(screen).getBlock() instanceof io.github.jimbozoomer.jugcraft.drone.ControlScreenBlock,
				"with a control screen on pillar C's face: " + level.getBlockState(screen));
		helper.assertTrue(level.getBlockState(oldLadder.above(3)).is(io.github.jimbozoomer.jugcraft.test.TowerGameTests.modBlock("tower_ladder")), "and the riser ladder in its inner corner");
		DroneTerminalBlockEntity terminal = core.terminal();
		for (int i = 0; i < 2; i++) {
			helper.assertTrue(terminal != null && terminal.linkDrone(owner, DroneTier.COURIER_QUAD), "drone " + (i + 1) + " links");
		}
		helper.startSequence()
				.thenWaitUntil(() -> helper.assertTrue(!core.converting() && core.layoutVersion() == TowerData.CURRENT_LAYOUT,
						"the tower is refitted (converting " + core.converting() + ", progress " + core.buildProgress() + ")"))
				.thenExecute(() -> {
					helper.assertTrue(core.tier() == 1 && core.foundationState() == 2, "still at tier 1 with its foundation");
					helper.assertTrue(level.getBlockState(screen).isAir(), "the control screen went with the cut: " + level.getBlockState(screen));
					helper.assertTrue(level.getBlockState(oldLadder.above(3)).isAir(), "the ladder's foot in the cut is gone");
					helper.assertTrue(level.getBlockState(pos.offset(TriPillarArena.RISER_LADDER).above(3)).is(io.github.jimbozoomer.jugcraft.nuclear.StorageTanks.LADDER),
							"the riser ladder goes up in the foot's outer corner");
					for (BlockPos wallPos : TriPillarArena.VIDEO_WALLS) {
						BlockPos master = pos.offset(wallPos);
						BlockState wall = level.getBlockState(master);
						helper.assertTrue(wall.getBlock() instanceof io.github.jimbozoomer.jugcraft.tower.CurvedVideoWallBlock, "a video wall at " + wallPos + ": " + wall);
						int aim = wall.getValue(io.github.jimbozoomer.jugcraft.tower.CurvedVideoWallBlock.AIM);
						int[] v = io.github.jimbozoomer.jugcraft.tower.CurvedVideoWallBlock.AIMS[aim];
						helper.assertTrue(master.getX() + v[0] == pos.getX() && master.getZ() + v[1] == pos.getZ(), "aimed at the core");
						for (net.minecraft.core.Vec3i o : io.github.jimbozoomer.jugcraft.tower.CurvedVideoWallBlock.footprint(aim)) {
							if (!o.equals(net.minecraft.core.Vec3i.ZERO)) {
								helper.assertTrue(level.getBlockState(master.offset(o)).getBlock() instanceof io.github.jimbozoomer.jugcraft.tower.CurvedVideoWallPartBlock,
										"the wall at " + wallPos + " is whole (part " + o + ")");
							}
						}
						helper.assertTrue(level.getBlockEntity(master) instanceof io.github.jimbozoomer.jugcraft.drone.DepotDisplayBlockEntity display
								&& core.terminalPos().equals(display.terminal()), "the wall at " + wallPos + " is pinned to the tower's terminal");
					}
					var console = (io.github.jimbozoomer.jugcraft.tower.TowerControlConsoleBlock) JugcraftTower.TOWER_CONTROL_CONSOLE;
					BlockPos consolePos = pos.offset(TriPillarArena.MASTER_CONSOLE);
					helper.assertTrue(level.getBlockState(consolePos).is(console) && console.complete(level, consolePos, net.minecraft.core.Direction.NORTH),
							"the Tower Control Console still stands whole");
					DroneTerminalBlockEntity now = core.terminal();
					helper.assertTrue(now != null && now.fleet().size() == 2, "the depot kept its two drones");
					helper.succeed();
				});
	}

	/**
	 * A Tri-Pillar saved in layout 11 (the lobby's first lit plinths: a steel dais, framed glass, a drone stand under a
	 * light panel) at tier 1 is refitted as layout 12: a Drone Display Case stands whole in each plinth's place, showing
	 * the same drone, the stands are gone, the video walls still stand, and the depot keeps its drones.
	 */
	@GameTest(structure = TriPillarArena.ARENA, maxTicks = 1200, skyAccess = true)
	public void videoWallTriPillarIsRefittedWithTheDisplayCases(GameTestHelper helper) {
		if (TestFocus.skip("tower")) {
			helper.succeed();
			return;
		}
		Player owner = helper.makeMockPlayer(GameType.CREATIVE);
		ServerLevel level = helper.getLevel();
		TowerCoreBlockEntity core = TriPillarArena.placeCore(helper);
		BlockPos pos = core.getBlockPos();
		core.setOwner(owner.getUUID());
		saveAs(level, core, TowerData.TRIPILLAR_VIDEO_WALL_LAYOUT, 0, 0);
		core.buildInstantly(level, 1);
		helper.assertTrue(core.layoutVersion() == TowerData.TRIPILLAR_VIDEO_WALL_LAYOUT && core.tier() == 1, "a layout-11 Tri-Pillar at tier 1");
		for (BlockPos casePos : TriPillarArena.DISPLAY_CASES) {
			BlockPos stand = pos.offset(casePos).above();
			helper.assertTrue(level.getBlockState(stand).is(io.github.jimbozoomer.jugcraft.test.TowerGameTests.modBlock("drone_display_stand")),
					"with a drone stand on the plinth at " + casePos + ": " + level.getBlockState(stand));
		}
		DroneTerminalBlockEntity terminal = core.terminal();
		for (int i = 0; i < 2; i++) {
			helper.assertTrue(terminal != null && terminal.linkDrone(owner, DroneTier.COURIER_QUAD), "drone " + (i + 1) + " links");
		}
		helper.startSequence()
				.thenWaitUntil(() -> helper.assertTrue(!core.converting() && core.layoutVersion() == TowerData.CURRENT_LAYOUT,
						"the tower is refitted (converting " + core.converting() + ", progress " + core.buildProgress() + ")"))
				.thenExecute(() -> {
					helper.assertTrue(core.tier() == 1 && core.foundationState() == 2, "still at tier 1 with its foundation");
					TowerGameTests.assertDisplayCases(helper, level, core);
					for (BlockPos wallPos : TriPillarArena.VIDEO_WALLS) {
						helper.assertTrue(level.getBlockState(pos.offset(wallPos)).getBlock() instanceof io.github.jimbozoomer.jugcraft.tower.CurvedVideoWallBlock,
								"the video wall at " + wallPos + " still stands");
					}
					DroneTerminalBlockEntity now = core.terminal();
					helper.assertTrue(now != null && now.fleet().size() == 2, "the depot kept its two drones");
					helper.succeed();
				});
	}

	/**
	 * A Tri-Pillar saved in layout 12 (four small planters with birch trees in the lobby) at tier 1 is refitted as layout
	 * 13: the planters are gone, the Crystal Tree stands whole in its planter bench, the display cases still stand, and the
	 * depot keeps its drones.
	 */
	@GameTest(structure = TriPillarArena.ARENA, maxTicks = 1200, skyAccess = true)
	public void displayCaseTriPillarIsRefittedWithTheCrystalTree(GameTestHelper helper) {
		if (TestFocus.skip("tower")) {
			helper.succeed();
			return;
		}
		Player owner = helper.makeMockPlayer(GameType.CREATIVE);
		ServerLevel level = helper.getLevel();
		TowerCoreBlockEntity core = TriPillarArena.placeCore(helper);
		BlockPos pos = core.getBlockPos();
		core.setOwner(owner.getUUID());
		saveAs(level, core, TowerData.TRIPILLAR_DISPLAY_CASE_LAYOUT, 0, 0);
		core.buildInstantly(level, 1);
		helper.assertTrue(core.layoutVersion() == TowerData.TRIPILLAR_DISPLAY_CASE_LAYOUT && core.tier() == 1, "a layout-12 Tri-Pillar at tier 1");
		BlockPos oldTrunk = pos.offset(TriPillarArena.OLD_PLANTER_TRUNK);
		helper.assertTrue(level.getBlockState(oldTrunk).is(net.minecraft.world.level.block.Blocks.STRIPPED_BIRCH_LOG),
				"with a small planter's birch trunk in the lobby: " + level.getBlockState(oldTrunk));
		DroneTerminalBlockEntity terminal = core.terminal();
		for (int i = 0; i < 2; i++) {
			helper.assertTrue(terminal != null && terminal.linkDrone(owner, DroneTier.COURIER_QUAD), "drone " + (i + 1) + " links");
		}
		helper.startSequence()
				.thenWaitUntil(() -> helper.assertTrue(!core.converting() && core.layoutVersion() == TowerData.CURRENT_LAYOUT,
						"the tower is refitted (converting " + core.converting() + ", progress " + core.buildProgress() + ")"))
				.thenExecute(() -> {
					helper.assertTrue(core.tier() == 1 && core.foundationState() == 2, "still at tier 1 with its foundation");
					TowerGameTests.assertCrystalTree(helper, level, core);
					TowerGameTests.assertDisplayCases(helper, level, core);
					DroneTerminalBlockEntity now = core.terminal();
					helper.assertTrue(now != null && now.fleet().size() == 2, "the depot kept its two drones");
					helper.succeed();
				});
	}

	/**
	 * A Tri-Pillar saved in layout 13 (two lounges by the entrance, three sofas a side over three small glass tables) at
	 * tier 1 is refitted as layout 14: the old lounges are gone, the lounge west of the lobby stands round the Long Coffee
	 * Table, the Crystal Tree still stands, and the depot keeps its drones.
	 */
	@GameTest(structure = TriPillarArena.ARENA, maxTicks = 1200, skyAccess = true)
	public void crystalTreeTriPillarIsRefittedWithTheLongCoffeeTable(GameTestHelper helper) {
		if (TestFocus.skip("tower")) {
			helper.succeed();
			return;
		}
		Player owner = helper.makeMockPlayer(GameType.CREATIVE);
		ServerLevel level = helper.getLevel();
		TowerCoreBlockEntity core = TriPillarArena.placeCore(helper);
		core.setOwner(owner.getUUID());
		saveAs(level, core, TowerData.TRIPILLAR_CRYSTAL_TREE_LAYOUT, 0, 0);
		core.buildInstantly(level, 1);
		helper.assertTrue(core.layoutVersion() == TowerData.TRIPILLAR_CRYSTAL_TREE_LAYOUT && core.tier() == 1, "a layout-13 Tri-Pillar at tier 1");
		BlockPos oldTable = TriPillarArena.at(core, TriPillarArena.OLD_LOUNGE_TABLES[0]);
		helper.assertTrue(level.getBlockState(oldTable).is(io.github.jimbozoomer.jugcraft.test.TowerGameTests.modBlock("glass_coffee_table")),
				"with an old lounge's small glass table by the entrance: " + level.getBlockState(oldTable));
		DroneTerminalBlockEntity terminal = core.terminal();
		for (int i = 0; i < 2; i++) {
			helper.assertTrue(terminal != null && terminal.linkDrone(owner, DroneTier.COURIER_QUAD), "drone " + (i + 1) + " links");
		}
		helper.startSequence()
				.thenWaitUntil(() -> helper.assertTrue(!core.converting() && core.layoutVersion() == TowerData.CURRENT_LAYOUT,
						"the tower is refitted (converting " + core.converting() + ", progress " + core.buildProgress() + ")"))
				.thenExecute(() -> {
					helper.assertTrue(core.tier() == 1 && core.foundationState() == 2, "still at tier 1 with its foundation");
					TowerGameTests.assertLounge(helper, level, core);
					TowerGameTests.assertCrystalTree(helper, level, core);
					DroneTerminalBlockEntity now = core.terminal();
					helper.assertTrue(now != null && now.fleet().size() == 2, "the depot kept its two drones");
					helper.succeed();
				});
	}

	/**
	 * A Tri-Pillar saved in layout 14 (the service bay's T4 tail-on to the arm, facing east) at tier 1 is refitted as
	 * layout 15: the T4 lies side-on to the arm (facing north), the arm faces it with its new collision, the lounge still
	 * stands round the Long Coffee Table, and the depot keeps its drones.
	 */
	@GameTest(structure = TriPillarArena.ARENA, maxTicks = 1200, skyAccess = true)
	public void longCoffeeTableTriPillarIsRefittedWithTheServiceBayTurned(GameTestHelper helper) {
		if (TestFocus.skip("tower")) {
			helper.succeed();
			return;
		}
		Player owner = helper.makeMockPlayer(GameType.CREATIVE);
		ServerLevel level = helper.getLevel();
		TowerCoreBlockEntity core = TriPillarArena.placeCore(helper);
		core.setOwner(owner.getUUID());
		saveAs(level, core, TowerData.TRIPILLAR_LONG_COFFEE_TABLE_LAYOUT, 0, 0);
		core.buildInstantly(level, 1);
		helper.assertTrue(core.layoutVersion() == TowerData.TRIPILLAR_LONG_COFFEE_TABLE_LAYOUT && core.tier() == 1, "a layout-14 Tri-Pillar at tier 1");
		var stand = level.getBlockState(TriPillarArena.at(core, TriPillarArena.SERVICE_STAND));
		helper.assertTrue(stand.is(io.github.jimbozoomer.jugcraft.test.TowerGameTests.modBlock("drone_display_stand"))
				&& stand.getValue(io.github.jimbozoomer.jugcraft.tower.HallFixtureBlock.FACING) == net.minecraft.core.Direction.EAST,
				"with the service bay's T4 tail-on to the arm: " + stand);
		DroneTerminalBlockEntity terminal = core.terminal();
		for (int i = 0; i < 2; i++) {
			helper.assertTrue(terminal != null && terminal.linkDrone(owner, DroneTier.COURIER_QUAD), "drone " + (i + 1) + " links");
		}
		helper.startSequence()
				.thenWaitUntil(() -> helper.assertTrue(!core.converting() && core.layoutVersion() == TowerData.CURRENT_LAYOUT,
						"the tower is refitted (converting " + core.converting() + ", progress " + core.buildProgress() + ")"))
				.thenExecute(() -> {
					helper.assertTrue(core.tier() == 1 && core.foundationState() == 2, "still at tier 1 with its foundation");
					TowerGameTests.assertServiceBay(helper, level, core);
					TowerGameTests.assertLounge(helper, level, core);
					DroneTerminalBlockEntity now = core.terminal();
					helper.assertTrue(now != null && now.fleet().size() == 2, "the depot kept its two drones");
					helper.succeed();
				});
	}

	/**
	 * A Tri-Pillar saved in layout 15 (two storage racks on the workshop's parts wall) at tier 1 is refitted as layout 16:
	 * the Drone Parts Shelving stands there whole, both units, the service bay keeps its turned T4 and its arm, and the
	 * depot keeps its drones.
	 */
	@GameTest(structure = TriPillarArena.ARENA, maxTicks = 1200, skyAccess = true)
	public void serviceArmTriPillarIsRefittedWithThePartsShelving(GameTestHelper helper) {
		if (TestFocus.skip("tower")) {
			helper.succeed();
			return;
		}
		Player owner = helper.makeMockPlayer(GameType.CREATIVE);
		ServerLevel level = helper.getLevel();
		TowerCoreBlockEntity core = TriPillarArena.placeCore(helper);
		core.setOwner(owner.getUUID());
		saveAs(level, core, TowerData.TRIPILLAR_SERVICE_ARM_LAYOUT, 0, 0);
		core.buildInstantly(level, 1);
		helper.assertTrue(core.layoutVersion() == TowerData.TRIPILLAR_SERVICE_ARM_LAYOUT && core.tier() == 1, "a layout-15 Tri-Pillar at tier 1");
		var rack = level.getBlockState(TriPillarArena.at(core, TriPillarArena.PARTS_SHELVING[0]));
		helper.assertTrue(rack.is(io.github.jimbozoomer.jugcraft.test.TowerGameTests.modBlock("storage_rack")),
				"with a storage rack on the parts wall: " + rack);
		DroneTerminalBlockEntity terminal = core.terminal();
		for (int i = 0; i < 2; i++) {
			helper.assertTrue(terminal != null && terminal.linkDrone(owner, DroneTier.COURIER_QUAD), "drone " + (i + 1) + " links");
		}
		helper.startSequence()
				.thenWaitUntil(() -> helper.assertTrue(!core.converting() && core.layoutVersion() == TowerData.CURRENT_LAYOUT,
						"the tower is refitted (converting " + core.converting() + ", progress " + core.buildProgress() + ")"))
				.thenExecute(() -> {
					helper.assertTrue(core.tier() == 1 && core.foundationState() == 2, "still at tier 1 with its foundation");
					TowerGameTests.assertPartsShelving(helper, level, core);
					TowerGameTests.assertServiceBay(helper, level, core);
					DroneTerminalBlockEntity now = core.terminal();
					helper.assertTrue(now != null && now.fleet().size() == 2, "the depot kept its two drones");
					helper.succeed();
				});
	}

	/**
	 * A Tri-Pillar saved in layout 44 (the twelve operator rows in the hall, the security office's single desks, the wall
	 * stub upstairs) at tier 1 is refitted as the current layout (45 on, Justin, 5 November 2026): the Ember Oak and the
	 * Drift Isle stand where the rows were (centred in their floors since layout 46), the desk rows left are whole, the security office has no single desks, the stub is gone, a
	 * stray zombie inside is cleared and the depot keeps its drones; the tower then hires its crew, one for each post.
	 */
	@GameTest(structure = TriPillarArena.ARENA, maxTicks = 1200, skyAccess = true)
	public void dioramaCaseTriPillarIsRefittedWithTheOpsTreesAndHiresItsCrew(GameTestHelper helper) {
		if (TestFocus.skip("tower")) {
			helper.succeed();
			return;
		}
		Player owner = helper.makeMockPlayer(GameType.CREATIVE);
		ServerLevel level = helper.getLevel();
		TowerCoreBlockEntity core = TriPillarArena.placeCore(helper);
		core.setOwner(owner.getUUID());
		saveAs(level, core, TowerData.TRIPILLAR_DIORAMA_CASE_LAYOUT, 0, 0);
		core.buildInstantly(level, 1);
		helper.assertTrue(core.layoutVersion() == TowerData.TRIPILLAR_DIORAMA_CASE_LAYOUT && core.tier() == 1, "a layout-44 Tri-Pillar at tier 1");
		BlockPos oldDesk = TriPillarArena.at(core, new BlockPos(-20, 1, 7));
		helper.assertTrue(level.getBlockState(oldDesk).getBlock() instanceof io.github.jimbozoomer.jugcraft.tower.ConsoleDeskBlock,
				"with an operator row in the hall: " + level.getBlockState(oldDesk));
		DroneTerminalBlockEntity terminal = core.terminal();
		for (int i = 0; i < 2; i++) {
			helper.assertTrue(terminal != null && terminal.linkDrone(owner, DroneTier.COURIER_QUAD), "drone " + (i + 1) + " links");
		}
		var registry = io.github.jimbozoomer.jugcraft.tower.TowerRegistry.get(level);
		registry.claim(owner.getUUID(), core.getBlockPos());
		BlockPos inside = TriPillarArena.at(core, new BlockPos(-12, 1, -22));
		helper.assertTrue(registry.inTower(level, inside) && !registry.inTower(level, TriPillarArena.at(core, new BlockPos(0, 1, -70))),
				"the hall is in the tower, the ground well out in front of it is not");
		// a zombie as the world would spawn one (the test helper's own would be kept: it makes its mobs persistent)
		var zombie = net.minecraft.world.entity.EntityTypes.ZOMBIE.create(level, net.minecraft.world.entity.EntitySpawnReason.NATURAL);
		helper.assertTrue(zombie != null, "a zombie");
		zombie.snapTo(inside.getX() + 0.5, inside.getY(), inside.getZ() + 0.5, 0, 0);
		zombie.setNoAi(true);
		level.addFreshEntity(zombie);
		helper.startSequence()
				.thenWaitUntil(() -> helper.assertTrue(!core.converting() && core.layoutVersion() == TowerData.CURRENT_LAYOUT,
						"the tower is refitted (converting " + core.converting() + ", progress " + core.buildProgress() + ")"))
				.thenExecute(() -> {
					helper.assertTrue(core.tier() == 1 && core.foundationState() == 2, "still at tier 1 with its foundation");
					helper.assertTrue(zombie.isRemoved(), "the stray zombie in the hall is cleared");
					TowerGameTests.assertOpsTrees(helper, level, core);
					TowerGameTests.assertDeskRows(helper, level, core);
					for (BlockPos p : BlockPos.betweenClosed(TriPillarArena.at(core, new BlockPos(31, 1, 11)), TriPillarArena.at(core, new BlockPos(38, 1, 17)))) {
						BlockState s = level.getBlockState(p);
						helper.assertTrue(!s.is(io.github.jimbozoomer.jugcraft.test.TowerGameTests.modBlock("glass_desk")) && !s.is(io.github.jimbozoomer.jugcraft.test.TowerGameTests.modBlock("office_desk")),
								"no single desk left in the security office: " + s + " at " + p);
					}
					helper.assertTrue(level.getBlockState(TriPillarArena.at(core, new BlockPos(-15, 14, 22))).isAir(), "the wall stub upstairs is gone");
					DroneTerminalBlockEntity now = core.terminal();
					helper.assertTrue(now != null && now.fleet().size() == 2, "the depot kept its two drones");
					// the posts' chunks kept ticking (a post in a chunk that isn't is not due yet)
					forceChunks(level, core, true);
				})
				.thenIdle(3)
				.thenExecute(() -> {
					// the crew: one hired for each post, each where their post is (hired at once, as with nobody near: other
					// tests' players stand near and would have a drop ship bring them)
					io.github.jimbozoomer.jugcraft.tower.crew.TowerCrewCare.hireAtOnce(level, core);
					var crew = level.getEntitiesOfClass(io.github.jimbozoomer.jugcraft.tower.crew.TowerCrew.class,
							new AABB(core.getBlockPos()).inflate(80), c -> c.core().equals(core.getBlockPos()));
					java.util.Set<String> manned = new java.util.HashSet<>();
					crew.forEach(c -> { if (c.post() != null) manned.add(c.post().id()); });
					helper.assertTrue(crew.size() == core.layout().crew().size() && crew.size() == 47, "a crew member for each of the " + core.layout().crew().size()
							+ " posts: " + crew.size() + "; none at " + core.layout().crew().stream().map(io.github.jimbozoomer.jugcraft.tower.crew.CrewPost::id)
									.filter(id -> !manned.contains(id)).toList());
					helper.assertTrue(crew.stream().map(io.github.jimbozoomer.jugcraft.tower.crew.TowerCrew::skin).distinct().count() == crew.size(),
							"no two wear the same face");
					crew.forEach(c -> c.discard());
					io.github.jimbozoomer.jugcraft.tower.crew.TowerCrewState.get(level).forget(core.getBlockPos());
					registry.release(core.getBlockPos());
					forceChunks(level, core, false);
					helper.succeed();
				});
	}

	/**
	 * The crew find their way in and up (Justin, 10 November 2026: "a bunch of NPCs that should have been inside walking
	 * outside ... I only saw like 2 NPCs upstairs"; tower/crew/CrewRoutes). A Tri-Pillar on open ground, its floor a step
	 * up from the ground as on a flat world, everyone with a chair or a station to get to put down outside the gates at
	 * midday (as off a drop ship): the tower's walk map is made, with a way on through every speed gate's lane, shut as
	 * they are; everyone walks in, everyone whose post is upstairs gets upstairs (before the walk map they stopped on the
	 * floor under their desks), and nobody had to be put back to get there. Measured before the walk map: 28 of 47 at their posts after four minutes,
	 * seven still outside against the walls, nobody upstairs who had to climb.
	 */
	@GameTest(structure = "jugcraft-test:drone_tower_grounds", maxTicks = 4400, skyAccess = true)
	public void crewWalkInFromTheGatesAndUpstairs(GameTestHelper helper) {
		if (TestFocus.skip("tower")) {
			helper.succeed();
			return;
		}
		Player owner = helper.makeMockPlayer(GameType.CREATIVE);
		ServerLevel level = helper.getLevel();
		BlockPos c = helper.absolutePos(new BlockPos(100, 5, 110));
		for (int x = -98; x <= 98; x++) {
			for (int z = -108; z <= 92; z++) {
				level.setBlock(c.offset(x, -1, z), Blocks.STONE.defaultBlockState(), net.minecraft.world.level.block.Block.UPDATE_CLIENTS);
			}
		}
		level.setBlock(c, JugcraftTower.CORE.defaultBlockState(), 3);
		TowerCoreBlockEntity core = (TowerCoreBlockEntity) level.getBlockEntity(c);
		core.setOwner(owner.getUUID());
		core.buildInstantly(level, 1);
		var registry = io.github.jimbozoomer.jugcraft.tower.TowerRegistry.get(level);
		registry.claim(owner.getUUID(), c);
		for (int cx = (c.getX() - 100) >> 4; cx <= (c.getX() + 100) >> 4; cx++) {
			for (int cz = (c.getZ() - 110) >> 4; cz <= (c.getZ() + 94) >> 4; cz++) {
				level.setChunkForced(cx, cz, true);
			}
		}
		java.util.List<io.github.jimbozoomer.jugcraft.tower.crew.TowerCrew> crew = new java.util.ArrayList<>();
		helper.startSequence().thenIdle(5).thenExecute(() -> {
			io.github.jimbozoomer.jugcraft.tower.crew.TowerCrewCare.hireAtOnce(level, core);
			crew.addAll(level.getEntitiesOfClass(io.github.jimbozoomer.jugcraft.tower.crew.TowerCrew.class, new AABB(c).inflate(120), m -> m.core().equals(c)));
			helper.assertTrue(crew.size() == core.layout().crew().size(), "a crew member for each post: " + crew.size());
			// those with a chair or a station to get to, unarmed: the guards would shoot at the mobs of the tests round
			// this one (the drop ship's gun test lost its zombie to them), and the patrols never stand still to be counted
			crew.removeIf(m -> {
				var p = m.post();
				if (p == null || p.patrol() || m.armed() || m.batoned()) {
					m.discard();
					return true;
				}
				return false;
			});
			helper.assertTrue(crew.size() >= 20 && crew.stream().filter(m -> m.post().home().getY() >= 10).count() >= 8,
					"the seated and stationed crew, eight or more of them from upstairs: " + crew.size());
			int i = 0;
			for (var m : crew) {
				m.setTestClock(12500);                      // midday: everyone at work, nobody on a break
				if (m.post() != null && !m.post().outside()) {
					m.stopRiding();
					m.snapTo(c.getX() - 23 + i + 0.5, c.getY(), c.getZ() - 62 - (i % 2) * 2 + 0.5, 0, 0);
					i++;
				}
			}
		}).thenWaitUntil(() -> {
			helper.assertTrue(io.github.jimbozoomer.jugcraft.tower.crew.CrewRoutes.ready(level, c), "the tower's walk map is made");
			java.util.List<String> outside = new java.util.ArrayList<>(), below = new java.util.ArrayList<>();
			for (var m : crew) {
				var p = m.post();
				if (p == null || p.outside() || !m.isAlive()) {
					continue;
				}
				double y = m.getY() - c.getY();
				String where = p.id() + " (" + Math.round(m.getX() - c.getX()) + ", " + Math.round(y) + ", " + Math.round(m.getZ() - c.getZ()) + ")";
				if (y < 0.6) {
					outside.add(where);
				}
				int floor = p.home().getY();
				if (floor >= 20 ? y < 6 : floor >= 10 && !m.isPassenger() && Math.abs(y - floor) > 1.5) {
					below.add(where);
				}
			}
			helper.assertTrue(outside.isEmpty(), "still outside: " + outside);
			helper.assertTrue(below.isEmpty(), "not upstairs yet: " + below);
		}).thenExecute(() -> {
			var putBack = crew.stream().filter(m -> m.putBacks() > 0).map(m -> m.post().id()).toList();
			helper.assertTrue(putBack.isEmpty(), "nobody was put back, they walked: " + putBack);
			helper.assertTrue(io.github.jimbozoomer.jugcraft.tower.crew.CrewRoutes.cells(level, c) > 20000,
					"the walk map covers the tower and the ground round it: " + io.github.jimbozoomer.jugcraft.tower.crew.CrewRoutes.cells(level, c) + " cells");
			int lanes = 0;
			for (BlockPos p : BlockPos.betweenClosed(c.offset(-60, 1, -60), c.offset(60, 1, 55))) {
				BlockState s = level.getBlockState(p);
				if (s.getBlock() instanceof io.github.jimbozoomer.jugcraft.nuclear.SpeedGateBlock
						&& s.getValue(io.github.jimbozoomer.jugcraft.nuclear.SpeedGateBlock.PART) == io.github.jimbozoomer.jugcraft.nuclear.SpeedGateBlock.Part.LANE) {
					lanes++;
					var on = io.github.jimbozoomer.jugcraft.tower.crew.CrewRoutes.steps(level, c, p);
					helper.assertTrue(on != null && on.size() >= 2, "a way on through the speed gate's lane at " + p.subtract(c) + ": " + on);
				}
			}
			helper.assertTrue(lanes >= 2, "the entrance's speed gates are there: " + lanes + " lanes");
			crew.forEach(m -> m.discard());
			io.github.jimbozoomer.jugcraft.tower.crew.TowerCrewState.get(level).forget(c);
			io.github.jimbozoomer.jugcraft.tower.crew.CrewRoutes.forget(level, c);
			registry.release(c);
			for (int cx = (c.getX() - 100) >> 4; cx <= (c.getX() + 100) >> 4; cx++) {
				for (int cz = (c.getZ() - 110) >> 4; cz <= (c.getZ() + 94) >> 4; cz++) {
					level.setChunkForced(cx, cz, false);
				}
			}
			helper.succeed();
		});
	}

	/** Keeps the chunks of the tower's foundation (to 60 blocks round its core) loaded and ticking, or lets them go. */
	private static void forceChunks(ServerLevel level, TowerCoreBlockEntity core, boolean on) {
		BlockPos c = core.getBlockPos();
		for (int cx = (c.getX() - 60) >> 4; cx <= (c.getX() + 60) >> 4; cx++) {
			for (int cz = (c.getZ() - 60) >> 4; cz <= (c.getZ() + 60) >> 4; cz++) {
				level.setChunkForced(cx, cz, on);
			}
		}
	}

	private static void saveAs(ServerLevel level, TowerCoreBlockEntity core, int layout, int tier, int building) {
		CompoundTag tag = core.saveWithoutMetadata(level.registryAccess());
		tag.putInt("layout_version", layout);
		tag.putInt("tier", tier);
		tag.putInt("building", building);
		core.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), tag));
	}

	/** How many of {@code item} are in {@code packager} (if any) and lying within four blocks of {@code terminal}. */
	private static int countNear(ServerLevel level, BlockPos terminal, @Nullable CargoPackagerBlockEntity packager, Item item) {
		int count = packager == null ? 0 : packager.count(stack -> stack.is(item));
		for (ItemEntity entity : level.getEntitiesOfClass(ItemEntity.class, new AABB(terminal).inflate(4), e -> e.getItem().is(item))) {
			count += entity.getItem().getCount();
		}
		return count;
	}

	/** Runs a command as {@code player} (with operator rights); -1 if it does not parse. */
	private static int run(ServerLevel level, ServerPlayer player, String command) {
		try {
			return level.getServer().getCommands().getDispatcher().execute(command, level.getServer().createCommandSourceStack().withEntity(player));
		} catch (CommandSyntaxException e) {
			return -1;
		}
	}

	/**
	 * A Tri-Pillar saved in layout 16 (glass-door racks behind the operator areas) at tier 1 is refitted as layout 17: the
	 * racks are gone and the data hall stands in the west corner (its racks, its contained aisle, its doors and plant),
	 * the parts shelving stays, and the depot keeps its drones.
	 */
	@GameTest(structure = TriPillarArena.ARENA, maxTicks = 1200, skyAccess = true)
	public void partsShelvingTriPillarIsRefittedWithTheDataHall(GameTestHelper helper) {
		if (TestFocus.skip("tower")) {
			helper.succeed();
			return;
		}
		Player owner = helper.makeMockPlayer(GameType.CREATIVE);
		ServerLevel level = helper.getLevel();
		TowerCoreBlockEntity core = TriPillarArena.placeCore(helper);
		core.setOwner(owner.getUUID());
		saveAs(level, core, TowerData.TRIPILLAR_PARTS_SHELVING_LAYOUT, 0, 0);
		core.buildInstantly(level, 1);
		helper.assertTrue(core.layoutVersion() == TowerData.TRIPILLAR_PARTS_SHELVING_LAYOUT && core.tier() == 1, "a layout-16 Tri-Pillar at tier 1");
		var rack = level.getBlockState(TriPillarArena.at(core, new net.minecraft.core.BlockPos(-TriPillarArena.OLD_RACK_X, 1, 8)));
		helper.assertTrue(rack.is(io.github.jimbozoomer.jugcraft.test.TowerGameTests.modBlock("equipment_rack")),
				"with glass-door racks behind the operator areas: " + rack);
		DroneTerminalBlockEntity terminal = core.terminal();
		for (int i = 0; i < 2; i++) {
			helper.assertTrue(terminal != null && terminal.linkDrone(owner, DroneTier.COURIER_QUAD), "drone " + (i + 1) + " links");
		}
		helper.startSequence()
				.thenWaitUntil(() -> helper.assertTrue(!core.converting() && core.layoutVersion() == TowerData.CURRENT_LAYOUT,
						"the tower is refitted (converting " + core.converting() + ", progress " + core.buildProgress() + ")"))
				.thenExecute(() -> {
					helper.assertTrue(core.tier() == 1 && core.foundationState() == 2, "still at tier 1 with its foundation");
					TowerGameTests.assertDataHall(helper, level, core);
					TowerGameTests.assertPartsShelving(helper, level, core);
					DroneTerminalBlockEntity now = core.terminal();
					helper.assertTrue(now != null && now.fleet().size() == 2, "the depot kept its two drones");
					helper.succeed();
				});
	}
	/**
	 * A Tri-Pillar saved with layout 17 (its desks plain, each on its own) is refitted with the remade desks (work list
	 * item 14): the same desks, now joined into their rows and working their roles, still pinned to the tower's terminal;
	 * the depot keeps its drones.
	 */
	@GameTest(structure = TriPillarArena.ARENA, maxTicks = 1200, skyAccess = true)
	public void dataHallTriPillarIsRefittedWithTheDeskRows(GameTestHelper helper) {
		if (TestFocus.skip("tower")) {
			helper.succeed();
			return;
		}
		Player owner = helper.makeMockPlayer(GameType.CREATIVE);
		ServerLevel level = helper.getLevel();
		TowerCoreBlockEntity core = TriPillarArena.placeCore(helper);
		core.setOwner(owner.getUUID());
		saveAs(level, core, TowerData.TRIPILLAR_DATA_HALL_LAYOUT, 0, 0);
		core.buildInstantly(level, 1);
		helper.assertTrue(core.layoutVersion() == TowerData.TRIPILLAR_DATA_HALL_LAYOUT && core.tier() == 1, "a layout-17 Tri-Pillar at tier 1");
		var airspace = level.getBlockState(TriPillarArena.at(core, new net.minecraft.core.BlockPos(-20, 1, 13)));
		helper.assertTrue(airspace.getBlock() instanceof io.github.jimbozoomer.jugcraft.tower.ConsoleDeskBlock
				&& airspace.getValue(io.github.jimbozoomer.jugcraft.tower.ConsoleDeskBlock.ROLE) == 0, "with its desks on no role yet: " + airspace);
		DroneTerminalBlockEntity terminal = core.terminal();
		for (int i = 0; i < 2; i++) {
			helper.assertTrue(terminal != null && terminal.linkDrone(owner, DroneTier.COURIER_QUAD), "drone " + (i + 1) + " links");
		}
		helper.startSequence()
				.thenWaitUntil(() -> helper.assertTrue(!core.converting() && core.layoutVersion() == TowerData.CURRENT_LAYOUT,
						"the tower is refitted (converting " + core.converting() + ", progress " + core.buildProgress() + ")"))
				.thenExecute(() -> {
					helper.assertTrue(core.tier() == 1 && core.foundationState() == 2, "still at tier 1 with its foundation");
					TowerGameTests.assertDeskRows(helper, level, core);
					TowerGameTests.assertDataHall(helper, level, core);
					DroneTerminalBlockEntity now = core.terminal();
					helper.assertTrue(now != null && now.fleet().size() == 2, "the depot kept its two drones");
					helper.succeed();
				});
	}

	/**
	 * A Tri-Pillar saved with layout 18 (the six floating holo panels over its operator areas) is refitted with the Holo
	 * Projector (work list item 15): the panels gone, the projector whole in the lobby past the gates; the desks stay as
	 * they were and the depot keeps its drones.
	 */
	@GameTest(structure = TriPillarArena.ARENA, maxTicks = 1200, skyAccess = true)
	public void deskScreensTriPillarIsRefittedWithTheHoloProjector(GameTestHelper helper) {
		if (TestFocus.skip("tower")) {
			helper.succeed();
			return;
		}
		Player owner = helper.makeMockPlayer(GameType.CREATIVE);
		ServerLevel level = helper.getLevel();
		TowerCoreBlockEntity core = TriPillarArena.placeCore(helper);
		core.setOwner(owner.getUUID());
		saveAs(level, core, TowerData.TRIPILLAR_DESK_SCREENS_LAYOUT, 0, 0);
		core.buildInstantly(level, 1);
		helper.assertTrue(core.layoutVersion() == TowerData.TRIPILLAR_DESK_SCREENS_LAYOUT && core.tier() == 1, "a layout-18 Tri-Pillar at tier 1");
		for (net.minecraft.core.BlockPos old : TriPillarArena.OLD_EMITTERS) {
			var emitter = level.getBlockState(TriPillarArena.at(core, old));
			helper.assertTrue(emitter.is(io.github.jimbozoomer.jugcraft.test.TowerGameTests.modBlock("holo_emitter")), "with its holo emitter at " + old + ": " + emitter);
		}
		helper.assertTrue(level.getBlockState(TriPillarArena.at(core, TriPillarArena.HOLO_PROJECTOR)).isAir(), "and no projector yet");
		DroneTerminalBlockEntity terminal = core.terminal();
		for (int i = 0; i < 2; i++) {
			helper.assertTrue(terminal != null && terminal.linkDrone(owner, DroneTier.COURIER_QUAD), "drone " + (i + 1) + " links");
		}
		helper.startSequence()
				.thenWaitUntil(() -> helper.assertTrue(!core.converting() && core.layoutVersion() == TowerData.CURRENT_LAYOUT,
						"the tower is refitted (converting " + core.converting() + ", progress " + core.buildProgress() + ")"))
				.thenExecute(() -> {
					helper.assertTrue(core.tier() == 1 && core.foundationState() == 2, "still at tier 1 with its foundation");
					TowerGameTests.assertHoloProjector(helper, level, core);
					TowerGameTests.assertDeskRows(helper, level, core);
					DroneTerminalBlockEntity now = core.terminal();
					helper.assertTrue(now != null && now.fleet().size() == 2, "the depot kept its two drones");
					helper.succeed();
				});
	}

	/**
	 * A Tri-Pillar saved with layout 19 (its pillar doorways open) is refitted with the Maintenance Doors (work list item
	 * 10): the riser rooms off the hall get their pairs, their badge readers the tower owner's; the projector stays.
	 */
	@GameTest(structure = TriPillarArena.ARENA, maxTicks = 1200, skyAccess = true)
	public void holoHeadTriPillarIsRefittedWithTheMaintenanceDoors(GameTestHelper helper) {
		if (TestFocus.skip("tower")) {
			helper.succeed();
			return;
		}
		Player owner = helper.makeMockPlayer(GameType.CREATIVE);
		ServerLevel level = helper.getLevel();
		TowerCoreBlockEntity core = TriPillarArena.placeCore(helper);
		core.setOwner(owner.getUUID());
		saveAs(level, core, TowerData.TRIPILLAR_HOLO_HEAD_LAYOUT, 0, 0);
		core.buildInstantly(level, 1);
		helper.assertTrue(core.layoutVersion() == TowerData.TRIPILLAR_HOLO_HEAD_LAYOUT && core.tier() == 1, "a layout-19 Tri-Pillar at tier 1");
		int[] a = TriPillarArena.MAINTENANCE_DOORS[0];
		helper.assertTrue(level.getBlockState(TriPillarArena.at(core, new net.minecraft.core.BlockPos(a[1], a[7] + 1, a[2]))).isAir(),
				"with pillar A's riser room doorway open");
		helper.startSequence()
				.thenWaitUntil(() -> helper.assertTrue(!core.converting() && core.layoutVersion() == TowerData.CURRENT_LAYOUT,
						"the tower is refitted (converting " + core.converting() + ", progress " + core.buildProgress() + ")"))
				.thenExecute(() -> {
					helper.assertTrue(core.tier() == 1 && core.foundationState() == 2, "still at tier 1 with its foundation");
					TowerGameTests.assertMaintenanceDoors(helper, level, core);
					TowerGameTests.assertHoloProjector(helper, level, core);
					helper.succeed();
				});
	}

	/**
	 * A Tri-Pillar saved with layout 20 (amber boxes and end rods) is refitted with the neon light lines (work list item 11):
	 * none of the old lights left, the lines, floor lines and tubes in their places; the doors stay.
	 */
	@GameTest(structure = TriPillarArena.ARENA, maxTicks = 1200, skyAccess = true)
	public void maintenanceDoorTriPillarIsRefittedWithTheNeonLines(GameTestHelper helper) {
		if (TestFocus.skip("tower")) {
			helper.succeed();
			return;
		}
		Player owner = helper.makeMockPlayer(GameType.CREATIVE);
		ServerLevel level = helper.getLevel();
		TowerCoreBlockEntity core = TriPillarArena.placeCore(helper);
		core.setOwner(owner.getUUID());
		saveAs(level, core, TowerData.TRIPILLAR_MAINTENANCE_DOOR_LAYOUT, 0, 0);
		core.buildInstantly(level, 1);
		helper.assertTrue(core.layoutVersion() == TowerData.TRIPILLAR_MAINTENANCE_DOOR_LAYOUT && core.tier() == 1, "a layout-20 Tri-Pillar at tier 1");
		helper.assertTrue(level.getBlockState(TriPillarArena.at(core, TriPillarArena.TICKER_EDGE)).is(io.github.jimbozoomer.jugcraft.test.TowerGameTests.modBlock("amber_light_strip")),
				"with its amber slab edge");
		helper.startSequence()
				.thenWaitUntil(() -> helper.assertTrue(!core.converting() && core.layoutVersion() == TowerData.CURRENT_LAYOUT,
						"the tower is refitted (converting " + core.converting() + ", progress " + core.buildProgress() + ")"))
				.thenExecute(() -> {
					helper.assertTrue(core.tier() == 1 && core.foundationState() == 2, "still at tier 1 with its foundation");
					TowerGameTests.assertNeonLines(helper, level, core);
					TowerGameTests.assertMaintenanceDoors(helper, level, core);
					helper.succeed();
				});
	}

	/**
	 * A Tri-Pillar saved with layout 21 (girder blocks stacked as they were placed) is refitted with the joined girders
	 * (work list item 12): the entrance canopy's posts are columns joined all the way up on their plates; the neon lines
	 * stay.
	 */
	@GameTest(structure = TriPillarArena.ARENA, maxTicks = 1200, skyAccess = true)
	public void neonTriPillarIsRefittedWithTheJoinedGirders(GameTestHelper helper) {
		if (TestFocus.skip("tower")) {
			helper.succeed();
			return;
		}
		Player owner = helper.makeMockPlayer(GameType.CREATIVE);
		ServerLevel level = helper.getLevel();
		TowerCoreBlockEntity core = TriPillarArena.placeCore(helper);
		core.setOwner(owner.getUUID());
		saveAs(level, core, TowerData.TRIPILLAR_NEON_LAYOUT, 0, 0);
		core.buildInstantly(level, 1);
		helper.assertTrue(core.layoutVersion() == TowerData.TRIPILLAR_NEON_LAYOUT && core.tier() == 1, "a layout-21 Tri-Pillar at tier 1");
		helper.startSequence()
				.thenWaitUntil(() -> helper.assertTrue(!core.converting() && core.layoutVersion() == TowerData.CURRENT_LAYOUT,
						"the tower is refitted (converting " + core.converting() + ", progress " + core.buildProgress() + ")"))
				.thenExecute(() -> {
					helper.assertTrue(core.tier() == 1 && core.foundationState() == 2, "still at tier 1 with its foundation");
					TowerGameTests.assertCanopyPosts(helper, level, core);
					TowerGameTests.assertNeonLines(helper, level, core);
					helper.succeed();
				});
	}

	/**
	 * A Tri-Pillar saved with layout 22 (no map room) is refitted with the map room and its Big Map Table (work list item
	 * 16): the room's walls, glass front, doors and sign, the table whole and pinned to the tower's terminal; the joined
	 * girders stay.
	 */
	@GameTest(structure = TriPillarArena.ARENA, maxTicks = 1200, skyAccess = true)
	public void strutsTriPillarIsRefittedWithTheMapRoom(GameTestHelper helper) {
		if (TestFocus.skip("tower")) {
			helper.succeed();
			return;
		}
		Player owner = helper.makeMockPlayer(GameType.CREATIVE);
		ServerLevel level = helper.getLevel();
		TowerCoreBlockEntity core = TriPillarArena.placeCore(helper);
		core.setOwner(owner.getUUID());
		saveAs(level, core, TowerData.TRIPILLAR_CRADLE_STRUTS_LAYOUT, 0, 0);
		core.buildInstantly(level, 1);
		helper.assertTrue(core.layoutVersion() == TowerData.TRIPILLAR_CRADLE_STRUTS_LAYOUT && core.tier() == 1, "a layout-22 Tri-Pillar at tier 1");
		helper.startSequence()
				.thenWaitUntil(() -> helper.assertTrue(!core.converting() && core.layoutVersion() == TowerData.CURRENT_LAYOUT,
						"the tower is refitted (converting " + core.converting() + ", progress " + core.buildProgress() + ")"))
				.thenExecute(() -> {
					helper.assertTrue(core.tier() == 1 && core.foundationState() == 2, "still at tier 1 with its foundation");
					TowerGameTests.assertCanopyPosts(helper, level, core);
					TowerGameTests.assertMapRoom(helper, level, core);
					helper.succeed();
				});
	}

	/**
	 * A Tri-Pillar saved with layout 23 (the ground floor's rooms empty) is refitted with them fitted out (work list item
	 * 22, part A): the shower stalls, the toilets, the alarm console, the screens, the batteries, the doors and plaques;
	 * the map room stays.
	 */
	@GameTest(structure = TriPillarArena.ARENA, maxTicks = 1200, skyAccess = true)
	public void mapRoomTriPillarIsRefittedWithTheGroundFloorRooms(GameTestHelper helper) {
		if (TestFocus.skip("tower")) {
			helper.succeed();
			return;
		}
		Player owner = helper.makeMockPlayer(GameType.CREATIVE);
		ServerLevel level = helper.getLevel();
		TowerCoreBlockEntity core = TriPillarArena.placeCore(helper);
		core.setOwner(owner.getUUID());
		saveAs(level, core, TowerData.TRIPILLAR_MAP_ROOM_LAYOUT, 0, 0);
		core.buildInstantly(level, 1);
		helper.assertTrue(core.layoutVersion() == TowerData.TRIPILLAR_MAP_ROOM_LAYOUT && core.tier() == 1, "a layout-23 Tri-Pillar at tier 1");
		helper.startSequence()
				.thenWaitUntil(() -> helper.assertTrue(!core.converting() && core.layoutVersion() == TowerData.CURRENT_LAYOUT,
						"the tower is refitted (converting " + core.converting() + ", progress " + core.buildProgress() + ")"))
				.thenExecute(() -> {
					helper.assertTrue(core.tier() == 1 && core.foundationState() == 2, "still at tier 1 with its foundation");
					TowerGameTests.assertMapRoom(helper, level, core);
					TowerGameTests.assertGroundFloorRooms(helper, level, core);
					helper.succeed();
				});
	}

	/**
	 * A Tri-Pillar saved with layout 24 (the upper floor's rooms empty, the hall's small map table) is refitted with the
	 * upper floor's rooms fitted out from the room kits (work list item 22, part B): the simulators, the boardroom table,
	 * the wall screens pinned to its terminal, the partitioned toilet, the plaques; the small table kept, pinned and bearing
	 * the tower diorama now, its desks on the build; the ground floor's rooms and the map room stay.
	 */
	@GameTest(structure = TriPillarArena.ARENA, maxTicks = 1200, skyAccess = true)
	public void groundRoomsTriPillarIsRefittedWithTheUpperFloorRooms(GameTestHelper helper) {
		if (TestFocus.skip("tower")) {
			helper.succeed();
			return;
		}
		Player owner = helper.makeMockPlayer(GameType.CREATIVE);
		ServerLevel level = helper.getLevel();
		TowerCoreBlockEntity core = TriPillarArena.placeCore(helper);
		core.setOwner(owner.getUUID());
		saveAs(level, core, TowerData.TRIPILLAR_GROUND_ROOMS_LAYOUT, 0, 0);
		core.buildInstantly(level, 1);
		helper.assertTrue(core.layoutVersion() == TowerData.TRIPILLAR_GROUND_ROOMS_LAYOUT && core.tier() == 1, "a layout-24 Tri-Pillar at tier 1");
		helper.assertTrue(level.getBlockState(TriPillarArena.at(core, TriPillarArena.MODEL_TABLE)).is(io.github.jimbozoomer.jugcraft.drone.JugcraftDrones.HOLO_TABLE),
				"layout 24 has the hall's small map table");
		helper.startSequence()
				.thenWaitUntil(() -> helper.assertTrue(!core.converting() && core.layoutVersion() == TowerData.CURRENT_LAYOUT,
						"the tower is refitted (converting " + core.converting() + ", progress " + core.buildProgress() + ")"))
				.thenExecute(() -> {
					helper.assertTrue(core.tier() == 1 && core.foundationState() == 2, "still at tier 1 with its foundation");
					TowerGameTests.assertMapRoom(helper, level, core);
					TowerGameTests.assertGroundFloorRooms(helper, level, core);
					TowerGameTests.assertUpperFloorRooms(helper, level, core);
					helper.succeed();
				});
	}

	/**
	 * A Tri-Pillar saved with layout 25 (before Justin's walk through) is refitted with the fixes: the riser ladder caged
	 * over its landing with a flared foot, the lounge's sofas a walk from the table, the rooms' walls up to the ceiling,
	 * the upper floor's rooms as before.
	 */
	@GameTest(structure = TriPillarArena.ARENA, maxTicks = 1200, skyAccess = true)
	public void upperRoomsTriPillarIsRefittedWithTheReviewFixes(GameTestHelper helper) {
		if (TestFocus.skip("tower")) {
			helper.succeed();
			return;
		}
		Player owner = helper.makeMockPlayer(GameType.CREATIVE);
		ServerLevel level = helper.getLevel();
		TowerCoreBlockEntity core = TriPillarArena.placeCore(helper);
		core.setOwner(owner.getUUID());
		saveAs(level, core, TowerData.TRIPILLAR_UPPER_ROOMS_LAYOUT, 0, 0);
		core.buildInstantly(level, 1);
		BlockPos ladder = TriPillarArena.at(core, TriPillarArena.RISER_LADDER);
		helper.assertTrue(core.layoutVersion() == TowerData.TRIPILLAR_UPPER_ROOMS_LAYOUT && level.getBlockState(ladder).is(io.github.jimbozoomer.jugcraft.test.TowerGameTests.modBlock("tower_ladder")),
				"a layout-25 Tri-Pillar with its tower ladders");
		helper.startSequence()
				.thenWaitUntil(() -> helper.assertTrue(!core.converting() && core.layoutVersion() == TowerData.CURRENT_LAYOUT,
						"the tower is refitted (converting " + core.converting() + ", progress " + core.buildProgress() + ")"))
				.thenExecute(() -> {
					for (int y = 0; y <= 10; y++) {
						BlockState s = level.getBlockState(ladder.above(y));
						helper.assertTrue(s.is(io.github.jimbozoomer.jugcraft.nuclear.StorageTanks.LADDER), "the caged ladder at height " + (y + 1) + ": " + s);
					}
					helper.assertTrue(!level.getBlockState(ladder.above(1)).getValue(io.github.jimbozoomer.jugcraft.nuclear.SteelLadderBlock.CAGE)
							&& level.getBlockState(ladder.above(2)).getValue(io.github.jimbozoomer.jugcraft.nuclear.SteelLadderBlock.BOTTOM),
							"no cage over the floor, the cage's flared foot at the third length");
					TowerGameTests.assertLounge(helper, level, core);
					TowerGameTests.assertGroundFloorRooms(helper, level, core);
					TowerGameTests.assertUpperFloorRooms(helper, level, core);
					TowerGameTests.assertWallsToCeiling(helper, level, core);
					helper.succeed();
				});
	}

	/**
	 * A Tri-Pillar saved with layout 41 is refitted (layouts 42 and 43) and its small hologram table stays formed, the
	 * diorama on it (a conversion re-placed it as loose sections before), its panel runs' end corners are mirrored, and the
	 * atrium dome is cut into its feet.
	 */
	@GameTest(structure = TriPillarArena.ARENA, maxTicks = 1200, skyAccess = true)
	public void cargo2TriPillarKeepsItsDioramaTableFormed(GameTestHelper helper) {
		if (TestFocus.skip("tower")) {
			helper.succeed();
			return;
		}
		Player owner = helper.makeMockPlayer(GameType.CREATIVE);
		ServerLevel level = helper.getLevel();
		TowerCoreBlockEntity core = TriPillarArena.placeCore(helper);
		core.setOwner(owner.getUUID());
		saveAs(level, core, TowerData.TRIPILLAR_CARGO2_LAYOUT, 0, 0);
		core.buildInstantly(level, 1);
		BlockPos table = TriPillarArena.at(core, TriPillarArena.MODEL_TABLE);
		helper.assertTrue(core.layoutVersion() == TowerData.TRIPILLAR_CARGO2_LAYOUT
				&& level.getBlockState(table).getValue(io.github.jimbozoomer.jugcraft.drone.HoloTableBlock.PART) == io.github.jimbozoomer.jugcraft.drone.HoloTableBlock.PROJECTOR_PART,
				"a layout-41 Tri-Pillar, its small table formed: " + level.getBlockState(table));
		helper.startSequence()
				.thenWaitUntil(() -> helper.assertTrue(!core.converting() && core.layoutVersion() == TowerData.CURRENT_LAYOUT,
						"the tower is refitted (converting " + core.converting() + ", progress " + core.buildProgress() + ")"))
				.thenExecute(() -> {
					TowerGameTests.assertModelTable(helper, level, core);
					TowerGameTests.assertPanelEnds(helper, level, core);
					TowerGameTests.assertAtriumDome(helper, level, core);
					helper.succeed();
				});
	}
}
