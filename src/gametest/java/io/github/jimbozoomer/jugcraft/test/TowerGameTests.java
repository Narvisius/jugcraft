package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.drone.DroneTerminalBlockEntity;
import io.github.jimbozoomer.jugcraft.drone.DroneTier;
import io.github.jimbozoomer.jugcraft.drone.HoloTableBlock;
import io.github.jimbozoomer.jugcraft.drone.JugcraftDrones;
import io.github.jimbozoomer.jugcraft.tower.JugcraftTower;
import io.github.jimbozoomer.jugcraft.tower.TowerCoreBlockEntity;
import io.github.jimbozoomer.jugcraft.tower.TowerData;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * In-game tests for the Drone Tower (docs/features/drone-tower.md): its data, the core building the Command
 * Post (tier 1) itself, tower tiers gating drone tiers, the hangars holding a full 100-drone fleet, and
 * drones flying the next tier's tiles in from the tower's modules.
 */
public class TowerGameTests {
	private static final String ARENA = "jugcraft-test:drone_tower";
	private static final String FULL = "jugcraft-test:drone_tower_full";
	/** The core's position in the small arena (the tower's field is 39x39 round it). */
	private static final BlockPos CORE = new BlockPos(21, 1, 21);

	@GameTest
	public void towerDataIsConsistent(GameTestHelper helper) {
		TowerData data = TowerData.get();
		helper.assertTrue(data.tiers.size() == TowerData.TIERS, "9 tiers, got " + data.tiers.size());
		for (BlockState state : data.palette) {
			helper.assertTrue(!state.isAir(), "every tower block is a real block (an unknown ID reads as air)");
		}
		int last = 0;
		for (TowerData.Tier tier : data.tiers) {
			helper.assertTrue(tier.capacity > last, "tier " + tier.number + " holds more drones than the one before");
			int hangars = tier.docks.size();
			if (tier.number >= 2) {
				helper.assertTrue(last + hangars == tier.capacity, "tier " + tier.number + ": " + hangars + " new hangars for "
						+ (tier.capacity - last) + " more drones");
			}
			last = tier.capacity;
			helper.assertTrue(tier.modules.getOrDefault("structural_module", 0) > 0, "tier " + tier.number + " costs structural modules");
			Set<Integer> covered = new HashSet<>();
			for (TowerData.Tile tile : tier.tiles) {
				helper.assertTrue(tile.blocks().length <= TowerData.TILE * TowerData.TILE, "a tile holds at most one 4x4 layer");
				for (int b : tile.blocks()) {
					helper.assertTrue(covered.add(b), "every block is in exactly one tile");
				}
			}
			helper.assertTrue(covered.size() == tier.place.length, "tiles cover all " + tier.place.length + " blocks of tier " + tier.number);
		}
		helper.assertTrue(last == 133, "a finished tower holds 133 drones, got " + last);
		helper.assertTrue(data.pads.size() == 8, "8 ground pads");
		helper.succeed();
	}

	/** A creative player gets the Creative Quick Start once; a survival player does not; the Field Manual has its pages. */
	@GameTest
	public void guideBooks(GameTestHelper helper) {
		net.minecraft.server.level.ServerPlayer player = helper.makeMockServerPlayerInLevel();
		// Joining may already have handed the book over (the test world is creative): start from a clean slate.
		player.removeTag(io.github.jimbozoomer.jugcraft.drone.GuideBooks.GIVEN_TAG);
		player.getInventory().clearContent();
		helper.assertFalse(io.github.jimbozoomer.jugcraft.drone.GuideBooks.giveCreativeGuide(player, GameType.SURVIVAL), "no creative book in survival");
		helper.assertTrue(io.github.jimbozoomer.jugcraft.drone.GuideBooks.giveCreativeGuide(player, GameType.CREATIVE), "a creative player gets the book");
		helper.assertTrue(player.getInventory().contains(new ItemStack(io.github.jimbozoomer.jugcraft.drone.GuideBooks.CREATIVE_GUIDE)),
				"the book is in the inventory");
		helper.assertFalse(io.github.jimbozoomer.jugcraft.drone.GuideBooks.giveCreativeGuide(player, GameType.CREATIVE), "only once");
		helper.assertTrue(io.github.jimbozoomer.jugcraft.drone.GuideBooks.MANUAL instanceof io.github.jimbozoomer.jugcraft.drone.GuideBooks.Book book
				&& book.bookId().equals("drone_tower_manual"), "the Field Manual is a guide book that opens its own pages");
		helper.succeed();
	}

	/** Every hangar floor of the finished tower is a whole joined-up pad, with nothing standing on or over it. */
	@GameTest
	public void hangarPadsAreWholeAndClear(GameTestHelper helper) {
		TowerData data = TowerData.get();
		Map<BlockPos, BlockState> world = new java.util.HashMap<>();
		for (TowerData.Tier tier : data.tiers) {
			for (int[] c : tier.clear) {
				world.remove(new BlockPos(c[0], c[1], c[2]));
			}
			for (int[] b : tier.place) {
				world.put(new BlockPos(b[0], b[1], b[2]), data.palette.get(b[3]));
			}
		}
		int pads = 0;
		for (Map.Entry<BlockPos, BlockState> e : world.entrySet()) {
			BlockState state = e.getValue();
			if (!state.hasProperty(io.github.jimbozoomer.jugcraft.tower.HangarPadBlock.PART)
					|| state.getValue(io.github.jimbozoomer.jugcraft.tower.HangarPadBlock.PART) == 0) {
				continue;
			}
			pads++;
			for (int dy = 1; dy <= 3; dy++) {
				BlockState above = world.get(e.getKey().above(dy));
				helper.assertTrue(above == null, "nothing over the hangar pad at " + e.getKey() + " (found " + above + " " + dy + " up)");
			}
		}
		helper.assertTrue(pads > 2000, "the finished tower lays its hangar pads as joined parts, got " + pads);
		helper.succeed();
	}

	@GameTest(structure = ARENA, maxTicks = 600, skyAccess = true)
	public void towerCoreBuildsTheCommandPost(GameTestHelper helper) {
		Player owner = helper.makeMockPlayer(GameType.CREATIVE);
		TowerCoreBlockEntity core = placeCore(helper);
		load(core, 1);
		core.tryUpgrade(owner);
		helper.assertTrue(core.building() == 1, "the upgrade started");
		helper.succeedWhen(() -> {
			helper.assertTrue(core.tier() == 1, "tier 1 built (progress " + core.buildProgress() + ")");
			DroneTerminalBlockEntity terminal = core.terminal();
			helper.assertTrue(terminal != null, "the command room has its terminal");
			helper.assertTrue(terminal.tower() == core, "the terminal belongs to the tower");
			helper.assertTrue(owner.getUUID().equals(terminal.owner()), "the tower's owner owns the terminal");
			terminal.rescanNow();
			helper.assertTrue(terminal.layout().pads().size() == 8, "8 ground pads formed, got " + terminal.layout().pads().size());
			helper.assertTrue(terminal.activePickups().size() == 1, "the ground supply pickup");
			BlockState table = helper.getLevel().getBlockState(core.getBlockPos().above());
			helper.assertTrue(table.is(JugcraftDrones.HOLO_TABLE) && table.getValue(HoloTableBlock.PART) > 0,
					"the plotting table over the core formed: " + table);
			helper.assertTrue(helper.getLevel().getBlockState(core.getBlockPos().offset(-4, 1, 1)).is(JugcraftTower.BLOCKS.get("operator_chair")),
					"real operator chairs in the command room");
		});
	}

	/** Tier 1 builds both exchanges; their ports feed the depot (power into the terminal, items into the packager). */
	@GameTest(structure = ARENA, maxTicks = 100, skyAccess = true)
	public void exchangePortsFeedTheDepot(GameTestHelper helper) {
		Player owner = helper.makeMockPlayer(GameType.CREATIVE);
		TowerCoreBlockEntity core = placeCore(helper);
		core.setOwner(owner.getUUID());
		core.buildInstantly(helper.getLevel(), 1);
		BlockPos power = core.getBlockPos().offset(-3 - 19, 2, 0);
		BlockPos cargo = core.getBlockPos().offset(39 - 19, 2, 0);
		helper.assertTrue(helper.getLevel().getBlockState(power).is(io.github.jimbozoomer.jugcraft.tower.ExchangePortBlock.ENERGY),
				"the Energy Exchange Port is on the west edge: " + helper.getLevel().getBlockState(power));
		helper.assertTrue(helper.getLevel().getBlockState(cargo).is(io.github.jimbozoomer.jugcraft.tower.ExchangePortBlock.CARGO),
				"the Cargo Exchange Port is on the east edge: " + helper.getLevel().getBlockState(cargo));
		DroneTerminalBlockEntity terminal = core.terminal();
		terminal.rescanNow();
		helper.assertTrue(terminal.layout().pads().size() == 8, "the exchanges leave the 8 pads intact");
		var energy = io.github.jimbozoomer.jugcraft.energy.EnergyStorage.SIDED.find(helper.getLevel(), power, net.minecraft.core.Direction.WEST);
		helper.assertTrue(energy != null, "the Energy Exchange Port exposes the depot's energy");
		long before = terminal.energy().getAmount();
		try (var tx = net.fabricmc.fabric.api.transfer.v1.transaction.Transaction.openOuter()) {
			energy.insert(1000, tx);
			tx.commit();
		}
		helper.assertTrue(terminal.energy().getAmount() > before, "power put into the port reached the terminal");
		var items = net.fabricmc.fabric.api.transfer.v1.item.ItemStorage.SIDED.find(helper.getLevel(), cargo, net.minecraft.core.Direction.EAST);
		helper.assertTrue(items != null, "the Cargo Exchange Port exposes the depot's cargo store");
		try (var tx = net.fabricmc.fabric.api.transfer.v1.transaction.Transaction.openOuter()) {
			long moved = items.insert(net.fabricmc.fabric.api.transfer.v1.item.ItemVariant.of(net.minecraft.world.item.Items.STONE_BRICKS), 16, tx);
			tx.commit();
			helper.assertTrue(moved == 16, "16 stone bricks went in, moved " + moved);
		}
		helper.assertTrue(terminal.packagerEntity().count(stack -> stack.is(net.minecraft.world.item.Items.STONE_BRICKS)) >= 16,
				"the items reached the packager");
		helper.succeed();
	}

	@GameTest(structure = ARENA, maxTicks = 100, skyAccess = true)
	public void upgradeNeedsPlinthAndModules(GameTestHelper helper) {
		Player owner = helper.makeMockPlayer(GameType.CREATIVE);
		BlockPos at = helper.absolutePos(CORE);
		helper.getLevel().setBlock(at, JugcraftTower.CORE.defaultBlockState(), 3);
		TowerCoreBlockEntity core = (TowerCoreBlockEntity) helper.getLevel().getBlockEntity(at);
		load(core, 1);
		core.tryUpgrade(owner);
		helper.assertTrue(core.building() == 0, "no plinth: nothing starts");
		placePlinth(helper);
		core.tryUpgrade(owner);
		helper.assertTrue(core.building() == 1, "plinth and modules: tier 1 starts");
		TowerCoreBlockEntity bare = core;
		helper.assertTrue(bare.modules(0) == TowerCoreBlockEntity.MODULE_CAPACITY - TowerData.get().tier(1).modules.get("structural_module"),
				"the modules were paid");
		helper.succeed();
	}

	@GameTest(structure = ARENA, maxTicks = 200, skyAccess = true)
	public void towerTierGatesDroneTiers(GameTestHelper helper) {
		Player owner = helper.makeMockPlayer(GameType.CREATIVE);
		TowerCoreBlockEntity core = placeCore(helper);
		core.setOwner(owner.getUUID());
		core.buildInstantly(helper.getLevel(), 1);
		DroneTerminalBlockEntity terminal = core.terminal();
		helper.assertTrue(terminal != null, "terminal built");
		helper.assertTrue(terminal.maxTier() == 1 && terminal.maxDrones() == 32, "tier 1: T1 drones, 32 of them (4 per pad)");
		helper.assertTrue(!terminal.linkDrone(owner, DroneTier.SURVEY_HEXACOPTER), "a T2 drone needs a tier 2 tower");
		helper.assertTrue(terminal.linkDrone(owner, DroneTier.COURIER_QUAD), "a T1 drone links");
		core.buildInstantly(helper.getLevel(), 2);
		helper.assertTrue(terminal.maxTier() == 2 && terminal.maxDrones() == 56, "tier 2: T2 drones, 56 of them");
		helper.assertTrue(terminal.linkDrone(owner, DroneTier.SURVEY_HEXACOPTER), "now a T2 drone links");
		helper.assertTrue(!terminal.linkDrone(owner, DroneTier.LIFTER_OCTOCOPTER), "but not T3");
		Vec3 hangar = terminal.dockOf(1);
		helper.assertTrue(hangar.y > core.getBlockPos().getY() + 10, "the T2 drone docks in a deck hangar, at " + hangar);
		helper.succeed();
	}

	@GameTest(structure = FULL, maxTicks = 400, skyAccess = true)
	public void finishedTowerHoldsAHundredDrones(GameTestHelper helper) {
		Player owner = helper.makeMockPlayer(GameType.CREATIVE);
		BlockPos center = new BlockPos(36, 1, 36);
		placePlinth(helper, center);
		BlockPos at = helper.absolutePos(center);
		helper.getLevel().setBlock(at, JugcraftTower.CORE.defaultBlockState(), 3);
		TowerCoreBlockEntity core = (TowerCoreBlockEntity) helper.getLevel().getBlockEntity(at);
		core.setOwner(owner.getUUID());
		core.buildInstantly(helper.getLevel(), 9);
		DroneTerminalBlockEntity terminal = core.terminal();
		helper.assertTrue(terminal != null && terminal.maxTier() == 9 && terminal.maxDrones() == 133, "tier 9: every drone tier, 133 drones");
		helper.assertTrue(terminal.activePickups().size() == 7 - 1, "six tower pickups (the ground one is under the deck)");
		int linked = 0;
		for (Map.Entry<DroneTier, Integer> batch : Map.of(DroneTier.SUPERCONDUCTING_LIFTER, 33, DroneTier.TANDEM_FREIGHTER, 32,
				DroneTier.LIFTER_OCTOCOPTER, 68).entrySet()) {
			for (int i = 0; i < batch.getValue(); i++) {
				helper.assertTrue(terminal.linkDrone(owner, batch.getKey()), "drone " + (linked + 1) + " (" + batch.getKey() + ") finds a dock");
				linked++;
			}
		}
		helper.assertTrue(!terminal.linkDrone(owner, DroneTier.COURIER_QUAD), "the 134th drone is refused");
		Set<String> spots = new HashSet<>();
		for (int i = 0; i < linked; i++) {
			Vec3 dock = terminal.dockOf(i);
			helper.assertTrue(spots.add(Math.round(dock.x * 2) + "," + Math.round(dock.y * 2) + "," + Math.round(dock.z * 2)),
					"every drone has its own dock (drone " + i + " at " + dock + ")");
		}
		helper.succeed();
	}

	/** One tower per player per dimension; the tower's own plotting table is tied to its own terminal. */
	@GameTest(structure = ARENA, maxTicks = 100, skyAccess = true)
	public void oneTowerPerPlayerAndPinnedDisplays(GameTestHelper helper) {
		Player owner = helper.makeMockPlayer(GameType.CREATIVE);
		TowerCoreBlockEntity core = placeCore(helper);
		core.setOwner(owner.getUUID());
		io.github.jimbozoomer.jugcraft.tower.TowerRegistry.get(helper.getLevel()).claim(owner.getUUID(), core.getBlockPos());
		helper.assertTrue(!io.github.jimbozoomer.jugcraft.tower.TowerCoreBlock.mayPlace(helper.getLevel(), owner), "a second core in the same dimension is refused");
		Player other = helper.makeMockPlayer(GameType.CREATIVE);
		helper.assertTrue(io.github.jimbozoomer.jugcraft.tower.TowerCoreBlock.mayPlace(helper.getLevel(), other), "another player may still place one");
		core.buildInstantly(helper.getLevel(), 1);
		TowerData data = TowerData.get();
		BlockPos terminal = core.getBlockPos().offset(data.terminal[0], data.terminal[1], data.terminal[2]);
		boolean pinned = false;
		for (int[] b : data.tier(1).place) {
			if (helper.getLevel().getBlockEntity(core.getBlockPos().offset(b[0], b[1], b[2])) instanceof io.github.jimbozoomer.jugcraft.drone.DepotDisplayBlockEntity display
					&& display.getBlockState().is(JugcraftDrones.HOLO_TABLE)) {
				pinned |= terminal.equals(display.terminal());
			}
		}
		helper.assertTrue(pinned, "the command post's plotting table is linked to the tower's own terminal");
		helper.succeed();
	}

	/** Creative Drones fly from a tier 1 tower, draw no power at all and still build the next tier. */
	@GameTest(structure = ARENA, maxTicks = 2400, skyAccess = true)
	public void creativeDronesNeedNoPower(GameTestHelper helper) {
		Player owner = helper.makeMockPlayer(GameType.CREATIVE);
		TowerCoreBlockEntity core = placeCore(helper);
		core.setOwner(owner.getUUID());
		core.buildInstantly(helper.getLevel(), 1);
		DroneTerminalBlockEntity terminal = core.terminal();
		for (int i = 0; i < 4; i++) {
			helper.assertTrue(terminal.linkDrone(owner, DroneTier.CREATIVE_DRONE), "creative drone " + i + " links to a tier 1 tower");
		}
		helper.assertTrue(terminal.fleet().standbyDraw() == 0 && terminal.fleet().workingDraw() == 0, "creative drones draw no power");
		terminal.energy().setAmount(0);
		load(core, 2);
		core.tryUpgrade(owner);
		int before = countDeck(helper.getLevel(), core.getBlockPos());
		helper.succeedWhen(() -> helper.assertTrue(countDeck(helper.getLevel(), core.getBlockPos()) > before,
				"creative drones fly tier 2 in with an empty battery (status: " + terminal.status().stream().map(c -> c.getString()).toList() + ")"));
	}

	/** Stray blocks inside the tower's rooms and under the deck are cleared; a chest there is left alone. */
	@GameTest(structure = ARENA, maxTicks = 100, skyAccess = true)
	public void strayBlocksInsideAreCleared(GameTestHelper helper) {
		TowerCoreBlockEntity core = placeCore(helper);
		TowerData data = TowerData.get();
		helper.assertTrue(data.tier(1).air.length > 0 && data.tier(2).air.length > 0, "tiers 1 and 2 list cells to clear");
		ServerLevel level = helper.getLevel();
		int[] room = data.tier(1).air[data.tier(1).air.length / 2];
		int[] chest = data.tier(1).air[data.tier(1).air.length / 2 + 1];
		int[] deck = data.tier(2).air[data.tier(2).air.length / 2];
		BlockPos roomPos = core.getBlockPos().offset(room[0], room[1], room[2]);
		BlockPos chestPos = core.getBlockPos().offset(chest[0], chest[1], chest[2]);
		BlockPos deckPos = core.getBlockPos().offset(deck[0], deck[1], deck[2]);
		level.setBlockAndUpdate(roomPos, Blocks.DIRT.defaultBlockState());
		level.setBlockAndUpdate(chestPos, Blocks.CHEST.defaultBlockState());
		level.setBlockAndUpdate(deckPos, Blocks.OAK_LOG.defaultBlockState());
		core.buildInstantly(level, 2);
		helper.assertTrue(level.getBlockState(roomPos).isAir(), "the dirt in the command room is cleared");
		helper.assertTrue(level.getBlockState(deckPos).isAir(), "the log under the hangar deck is cleared");
		helper.assertTrue(level.getBlockState(chestPos).is(Blocks.CHEST), "a chest is never cleared");
		helper.succeed();
	}

	@GameTest(structure = ARENA, maxTicks = 2400, skyAccess = true)
	public void dronesFlyTheNextTierIn(GameTestHelper helper) {
		Player owner = helper.makeMockPlayer(GameType.CREATIVE);
		TowerCoreBlockEntity core = placeCore(helper);
		core.setOwner(owner.getUUID());
		core.buildInstantly(helper.getLevel(), 1);
		DroneTerminalBlockEntity terminal = core.terminal();
		terminal.energy().setAmount(DroneTerminalBlockEntity.ENERGY_CAPACITY);
		for (int i = 0; i < 4; i++) {
			helper.assertTrue(terminal.linkDrone(owner, DroneTier.COURIER_QUAD), "drone " + i + " links");
		}
		load(core, 2);
		core.tryUpgrade(owner);
		helper.assertTrue(core.building() == 2, "tier 2 started");
		int before = countDeck(helper.getLevel(), core.getBlockPos());
		helper.onEachTick(() -> terminal.energy().setAmount(DroneTerminalBlockEntity.ENERGY_CAPACITY));
		helper.succeedWhen(() -> {
			helper.assertTrue(core.buildProgress() > 0, "a drone flew a tile of the hangar deck in (status: "
					+ terminal.status().stream().map(c -> c.getString()).toList() + ")");
			helper.assertTrue(countDeck(helper.getLevel(), core.getBlockPos()) > before, "tier 2 blocks appeared");
		});
	}

	// ------------------------------------------------------------------ helpers

	/** How many of tier 2's blocks are in place (the tier is built from the ground up, legs first). */
	private static int countDeck(ServerLevel level, BlockPos core) {
		int found = 0;
		TowerData data = TowerData.get();
		for (int[] block : data.tier(2).place) {
			if (level.getBlockState(core.offset(block[0], block[1], block[2])).is(data.state(block[3]).getBlock())) {
				found++;
			}
		}
		return found;
	}

	private static TowerCoreBlockEntity placeCore(GameTestHelper helper) {
		placePlinth(helper);
		BlockPos at = helper.absolutePos(CORE);
		helper.getLevel().setBlock(at, JugcraftTower.CORE.defaultBlockState(), 3);
		return (TowerCoreBlockEntity) helper.getLevel().getBlockEntity(at);
	}

	private static void placePlinth(GameTestHelper helper) {
		placePlinth(helper, CORE);
	}

	private static void placePlinth(GameTestHelper helper, BlockPos center) {
		for (int dx = -7; dx <= 7; dx++) {
			for (int dz = -7; dz <= 7; dz++) {
				helper.getLevel().setBlock(helper.absolutePos(center.offset(dx, 0, dz)), Blocks.CHISELED_STONE_BRICKS.defaultBlockState(), 3);
			}
		}
	}

	/** Fills the core with every kind of module (as a player or a pipe would). */
	private static void load(TowerCoreBlockEntity core, int tier) {
		for (String module : JugcraftTower.MODULES) {
			core.deposit(new ItemStack(JugcraftTower.MODULE_ITEMS.get(module), TowerCoreBlockEntity.MODULE_CAPACITY));
		}
	}
}
