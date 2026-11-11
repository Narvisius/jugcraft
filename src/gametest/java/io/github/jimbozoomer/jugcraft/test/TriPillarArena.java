package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.tower.ExchangeStationBlock;
import io.github.jimbozoomer.jugcraft.tower.ExchangeStations;
import io.github.jimbozoomer.jugcraft.tower.JugcraftTower;
import io.github.jimbozoomer.jugcraft.tower.TowerCoreBlockEntity;
import io.github.jimbozoomer.jugcraft.tower.TowerData;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

/**
 * Where the Tri-Pillar Drone Tower stands in its game tests, and the places in it the tests look at (all from
 * tools/tower_tripillar.py and tools/tripillar_hall.py, relative to the Tower Core; the hall floor is the core's level,
 * so what stands on it is at y 1).
 *
 * <p>The foundation (the control building with its exchanges, dispatch and the goods lift's house) reaches x -52..52, y -3..26,
 * z -53..45 from the core; the finished tower is 262 high. {@link #ARENA} holds the foundation and tiers 1 and 2
 * (128 x 48 x 112), {@link #FULL} the whole tower (128 x 272 x 112); both put the core at {@link #CORE}.
 */
public final class TriPillarArena {
	private TriPillarArena() { }

	/** The foundation and tiers 1-2. */
	public static final String ARENA = "jugcraft-test:drone_tower_tri";
	/** The whole tower, tiers 1-9. */
	public static final String FULL = "jugcraft-test:drone_tower_tri_full";
	/** The core's place in either arena: the foundation's x -52..62, z -53..45 and y -3 fit inside. */
	public static final BlockPos CORE = new BlockPos(56, 4, 56);

	// ------------------------------------------------------------------ places in the tower (relative to the core)

	/** The depot terminal: in dispatch east of pillar B (layout 37, tools/tripillar_stations.py), out of sight under the
	 * cargo hall's floor since layout 40 (the hall's consoles work it). */
	public static final BlockPos TERMINAL = new BlockPos(31, -1, -8);
	/** The Energy Exchange's master cell and the Cargo Exchange's, both facing east out of the intake face. */
	public static final BlockPos ENERGY_MASTER = new BlockPos(45, 1, -9), CARGO_MASTER = new BlockPos(45, 1, 1);
	/** The cargo packager (the depot's store) under the cargo hall's floor, and the supply pickup's middle: on the goods lift
	 * house's top since layout 40 (a 3x3 pickup on its 3x3 landing platform, over the car). */
	public static final BlockPos PACKAGER = new BlockPos(31, -1, -2), ROOF_PICKUP = new BlockPos(38, 26, -13);
	/** The goods lift's landing door's anchor: the ground floor's (facing west, into the cargo hall); its roof door went with
	 * layout 40 (the car runs up into the house under the pickup). */
	public static final BlockPos LIFT_GROUND = new BlockPos(36, 1, -12);
	/** The cargo hall (layout 40): the Case Packer's anchor (facing west, its back to the intake room's wall) and the pack spot. */
	public static final BlockPos CASE_PACKER = new BlockPos(36, 1, 2), PACK_SPOT = new BlockPos(34, 0, 3);
	/** The packer's infeed conveyor's anchor (layout 41: its spout over the hopper; facing west, its belt running east through
	 * the intake room's wall to the cargo exchange's back). */
	public static final BlockPos PACKER_INFEED = new BlockPos(36, 3, 3);
	/** The Holo Command Table's middle part (over the core: the tower hologram) and the map table's anchor (the topographic map). */
	public static final BlockPos CENTRAL_TABLE = new BlockPos(0, 1, 0);
	/** The hall's small 3x3 hologram table (its middle cell, the anchor) and its desk row's first desk: the map table until
	 * layout 25 took them out, back with layout 29 bearing the tower diorama (redesign 3), its desks on the tower's build;
	 * the operator's chair behind the middle desk. */
	public static final BlockPos MODEL_TABLE = new BlockPos(-24, 1, -25), MODEL_DESK = new BlockPos(-25, 1, -21),
			MODEL_CHAIR = new BlockPos(-24, 1, -20);
	/** The Tower Control Console's controller cell (part 0, facing north) north of the Holo Command Table (layout 10). */
	public static final BlockPos MASTER_CONSOLE = new BlockPos(0, 1, -9);
	/** The old master console's controller cell (layouts 8 and 9). */
	public static final BlockPos OLD_MASTER_CONSOLE = new BlockPos(0, 1, -10);
	/** The three Console Chairs on the raised floor behind the console, and a cell of the floor's back step. */
	public static final BlockPos[] CONSOLE_CHAIRS = {new BlockPos(0, 1, -11), new BlockPos(-4, 1, -10), new BlockPos(4, 1, -10)};
	public static final BlockPos FLOOR_BACK_STEP = new BlockPos(0, 1, -12);
	/** The Curved Video Walls' master cells (layout 11): A south of the table, B north-east, C north-west; each aimed at the core cell. */
	public static final BlockPos[] VIDEO_WALLS = {new BlockPos(0, 1, 15), new BlockPos(13, 1, -8), new BlockPos(-13, 1, -8)};
	/** Pillar C's riser ladder: its foot in the rotunda cut (layout 11: gone below the soffit), its new foot in the outer corner. */
	public static final BlockPos OLD_RISER_LADDER = new BlockPos(-9, 1, -2), RISER_LADDER = new BlockPos(-21, 1, -14);
	/** A cell of pillar C's face over the cut at the slab edge (amber, the ticker runs on round over the wall). */
	public static final BlockPos CUT_EDGE = new BlockPos(-14, 10, -8);
	/**
	 * The lobby's Drone Display Cases' master cells (layout 12; the middle cell on the hall floor, facing north), west then
	 * east, and the drone tier each shows; the first lit plinths' drone stands stood a block over them (layouts 8-11).
	 */
	public static final BlockPos[] DISPLAY_CASES = {new BlockPos(-14, 1, -38), new BlockPos(13, 1, -38)};
	public static final int[] DISPLAY_CASE_TIERS = {4, 1};
	/** The Crystal Tree's master cell (layout 13: the planter's middle, on the hall floor) and a cell of one of the four
	 * small planters it replaced (a birch trunk, layouts 8-12). */
	public static final BlockPos CRYSTAL_TREE = new BlockPos(14, 1, -27), OLD_PLANTER_TRUNK = new BlockPos(9, 2, -31);
	/** The Long Coffee Table's master cell (layout 14: the table's middle is its north-west corner), the first cells of the
	 * lounge's two rows of six sofas (north row facing south, laid west; south row facing north, laid east), and a small glass
	 * table of each old lounge it replaced (layouts 8-13). */
	public static final BlockPos LONG_COFFEE_TABLE = new BlockPos(-14, 1, -27);
	/** A column of each kind of wall carried up to the hall's ceiling (layout 27): the security office's glass front, the
	 * crew lounge's (upstairs) and the data hall's east wall: the band's y and the hall ceiling's y over it. */
	public static final int[][] WALLS_TO_CEILING = {{30, 17, 5, 10}, {0, 30, 16, 19}, {-37, -5, 5, 10}};
	public static final BlockPos[] SOFA_ROWS = {new BlockPos(-12, 1, -30), new BlockPos(-17, 1, -25)};   // (layout 26: a walk from the table)
	public static final BlockPos[] OLD_LOUNGE_TABLES = {new BlockPos(-25, 1, -36), new BlockPos(24, 1, -36)};
	/** The service bay's drone stand under the T4 on its cradle (layout 15: facing north, the drone side-on to the arm;
	 * layouts 8-14 faced east, the tail to it) and its robot arm, facing west at it (work list item 8). */
	public static final BlockPos SERVICE_STAND = new BlockPos(25, 1, -30), SERVICE_ARM = new BlockPos(27, 1, -30);
	/** The Drone Parts Shelving's masters on the workshop's parts wall (layout 16: stock a west, b east, facing south to the
	 * service bay), where the two storage racks stood (layouts 8-15). */
	public static final BlockPos[] PARTS_SHELVING = {new BlockPos(23, 1, -35), new BlockPos(27, 1, -35)};
	/** The data hall (layout 17, work list item 13) in the hall's west corner: its racks' rows (row A at z -2 facing north,
	 * fronts south; row B at z 1 facing south), the containment doors' masters (east end facing east, west end facing
	 * west), the UPS module (the room's hub), CRAC-1's left block, the reader doors and the light box's placing block; the
	 * glass-door racks behind the operator areas (layouts 8-16) stood at x -35 and 35, z 8..13. */
	public static final int[] DATA_HALL_RACK_X = {-48, -47, -46, -45, -44, -43, -42, -41};
	public static final int DATA_HALL_ROW_A = -2, DATA_HALL_ROW_B = 1;
	public static final BlockPos DATA_HALL_DOOR_EAST = new BlockPos(-40, 1, -1), DATA_HALL_DOOR_WEST = new BlockPos(-49, 1, 0);
	public static final BlockPos DATA_HALL_UPS = new BlockPos(-42, 1, 7), DATA_HALL_CRAC = new BlockPos(-46, 1, -7);
	public static final BlockPos DATA_HALL_ENTRY = new BlockPos(-37, 1, -1), DATA_HALL_SIGN = new BlockPos(-36, 5, -1);
	public static final int OLD_RACK_X = 35;
	/**
	 * The desk rows (work list item 14): {x, z} of the operator's left-hand desk, {dx, dz} to the next, and the row's role
	 * (tower/ConsoleDeskBlock ROLES): reception's two, the build desk's, the cargo hall's DISPATCH and SUPPLY (layout 40).
	 * The twelve operator rows went with layout 45 for the flight-ops trees ({@link #OLD_OPERATOR_ROWS}).
	 */
	public static final int[][] DESK_ROWS = {
			{-6, -36, 1, 0, 12}, {4, -36, 1, 0, 13}, {-25, -21, 1, 0, 5}, {31, -9, 0, 1, 3}, {31, -3, 0, 1, 7}};
	/** The operator rows' first desks to layout 44 ({x, z}, each row three along z): the trees stand there since 45. */
	public static final int[][] OLD_OPERATOR_ROWS = {{-20, 7}, {-20, 12}, {-25, 7}, {-25, 12}, {-30, 7}, {-30, 12},
			{20, 7}, {20, 12}, {25, 7}, {25, 12}, {20, 17}, {25, 17}};
	/** The flight-ops trees' master cells (layout 45): the Ember Oak west, the Drift Isle east. */
	public static final BlockPos EMBER_OAK = new BlockPos(-23, 1, 12), DRIFT_ISLE = new BlockPos(19, 1, 12);
	/** The Holo Projector's master cell (layout 19, work list item 15: in the lobby past the security gates, facing north to
	 * the entrance), and the six holo emitters it replaced over the operator areas (layouts 8-18: x -16 and 16, z 6, 10, 14). */
	public static final BlockPos HOLO_PROJECTOR = new BlockPos(0, 1, -33);
	public static final BlockPos[] OLD_EMITTERS = {new BlockPos(-16, 1, 6), new BlockPos(-16, 1, 10), new BlockPos(-16, 1, 14),
			new BlockPos(16, 1, 6), new BlockPos(16, 1, 10), new BlockPos(16, 1, 14)};
	/**
	 * The Maintenance Doors (layout 20, work list item 10), a pair in every pillar doorway: {tier (0: the foundation's riser
	 * rooms off the hall), x and z of its two cells, the way out of the pillar (x, z), the floor's y}. Tier 2's are off the
	 * roof, tiers 3-9's off the tie decks, facing the lift core.
	 */
	public static final int[][] MAINTENANCE_DOORS = {
			{0, 0, 26, 1, 26, 0, 1, 0}, {0, 24, -8, 24, -7, 1, 0, 0}, {0, -24, -8, -24, -7, -1, 0, 0},
			{2, 9, 18, 9, 19, 1, 0, 20}, {2, 15, 1, 16, 1, 0, 1, 20}, {2, -16, 1, -15, 1, 0, 1, 20},
			{3, 0, 8, 1, 8, 0, -1, 36}, {3, 6, -3, 6, -2, -1, 0, 36}, {3, -6, -3, -6, -2, 1, 0, 36},
			{4, 0, 8, 1, 8, 0, -1, 55}, {4, 6, -3, 6, -2, -1, 0, 55}, {4, -6, -3, -6, -2, 1, 0, 55},
			{5, 0, 7, 1, 7, 0, -1, 76}, {5, 5, -3, 5, -2, -1, 0, 76}, {5, -5, -3, -5, -2, 1, 0, 76},
			{6, 0, 8, 1, 8, 0, -1, 100}, {6, 6, -4, 6, -3, -1, 0, 100}, {6, -6, -4, -6, -3, 1, 0, 100},
			{7, 0, 8, 1, 8, 0, -1, 130}, {7, 6, -4, 6, -3, -1, 0, 130}, {7, -6, -4, -6, -3, 1, 0, 130},
			{8, 0, 8, 1, 8, 0, -1, 162}, {8, 6, -4, 6, -3, -1, 0, 162}, {8, -6, -4, -6, -3, 1, 0, 162},
			{9, 0, 7, 1, 7, 0, -1, 198}, {9, 5, -4, 5, -3, -1, 0, 198}, {9, -5, -4, -5, -3, 1, 0, 198}};
	/** The neon light lines (layout 21, work list item 11): a cell of the lobby's floor line to the entrance (along z, in the
	 * epoxy), and the neon tubes' feet up pillar A's foot's outer corners (east of it in the left corner, west in the right). */
	public static final BlockPos NEON_FLOOR_LINE = new BlockPos(0, 0, -37);
	public static final BlockPos[] NEON_TUBES = {new BlockPos(6, 1, 27), new BlockPos(-6, 1, 27)};
	/** The joined girders (layout 22, work list item 12): the feet of the entrance canopy's two posts (columns 9 high), and
	 * the master of the Cradle Struts under the tier-2 supply cradle (pillar A's, its diagonal -x +z: facing south). */
	public static final BlockPos[] CANOPY_POSTS = {new BlockPos(-8, 1, -53), new BlockPos(7, 1, -53)};
	public static final int CANOPY_POST_HEIGHT = 9;
	public static final BlockPos CRADLE_STRUTS = new BlockPos(-9, 29, 24);
	/** The map room (layout 23, work list item 16): the Big Map Table's master (its middle cell, on the upper floor), the
	 * sign's first part (over the doors, on the band's gallery side), the doors' west leaf, the ceiling panels taken out
	 * over the map, and a cell of each of its walls and of its glass front. */
	public static final BlockPos BIG_MAP_TABLE = new BlockPos(1, 12, -34);
	public static final BlockPos MAP_ROOM_SIGN = new BlockPos(-1, 17, -21);
	public static final BlockPos MAP_ROOM_DOOR = new BlockPos(0, 12, -22);
	public static final BlockPos[] MAP_LAMPS_OUT = {new BlockPos(0, 18, -35), new BlockPos(5, 18, -35), new BlockPos(0, 18, -30),
			new BlockPos(5, 18, -30)};
	public static final BlockPos[] MAP_ROOM_WALLS = {new BlockPos(-11, 14, -30), new BlockPos(13, 14, -30), new BlockPos(5, 17, -22)};
	public static final BlockPos MAP_ROOM_GLASS = new BlockPos(5, 14, -22);
	/** The ground floor's rooms (layout 24, work list item 22 part A): the locker room's three shower stalls (facing east,
	 * north to south), the restroom's toilets (accessible and standard) and urinal, the washroom's toilet, the security
	 * office's alarm console (its left part) and the live screen's top-left part over it, the cafe's live screen (its
	 * bottom-left part), a station battery (string 1's first rack's last part) and the UPS module; the door pairs' first
	 * leaves (security and the cafe's west pair glass, the locker and battery rooms' steel) and the plaques by them. */
	public static final BlockPos[] SHOWER_STALLS = {new BlockPos(-25, 1, 34), new BlockPos(-25, 1, 35), new BlockPos(-25, 1, 36)};
	public static final BlockPos[] RESTROOM_TOILETS = {new BlockPos(-28, 1, -32), new BlockPos(-26, 1, -32), new BlockPos(-23, 1, -43)};
	public static final BlockPos RESTROOM_URINAL = new BlockPos(-23, 1, -32);
	public static final BlockPos SECURITY_CONSOLE = new BlockPos(33, 1, 11);      // (east of pillar B since layout 38, by the curtain since 40)
	public static final BlockPos SECURITY_SCREEN = new BlockPos(33, 3, 11);
	/** The ground floor break area's Flat Screen TV over its media console (layout 51; the Lounge TV before), its
	 * controller: the bottom-left block as its viewer sees it; the break area's long glass coffee table's middle. */
	public static final BlockPos CAFE_TV = new BlockPos(23, 2, 36);
	/** The upper lounge's Flat Screen TV (layout 39), its controller: the bottom-left block as its viewer sees it. */
	public static final BlockPos LOUNGE_TV = new BlockPos(18, 13, 36);
	public static final BlockPos CAFE_TABLE = new BlockPos(21, 1, 37);
	/** The crew cafe's kitchen (layout 32): the Kitchen Run's anchor (part 0, the lower half) and the Fridge Freezer's. */
	public static final BlockPos CAFE_KITCHEN_RUN = new BlockPos(-23, 1, 43);
	public static final BlockPos CAFE_FRIDGE = new BlockPos(-23, 1, 44);
	public static final BlockPos STATION_BATTERY = new BlockPos(25, 1, 32);
	public static final BlockPos UPS_MODULE = new BlockPos(28, 1, 22);
	public static final BlockPos SECURITY_DOOR = new BlockPos(30, 1, 15);
	public static final BlockPos CAFE_DOOR = new BlockPos(-13, 1, 30);
	public static final BlockPos LOCKER_DOOR = new BlockPos(-31, 1, 21);
	/** (In its west wall, on the hall, since layout 49: till then in its north wall at 36, 1, 21, down a slot one wide.) */
	public static final BlockPos BATTERY_DOOR = new BlockPos(24, 1, 24);
	public static final BlockPos SECURITY_PLAQUE = new BlockPos(29, 5, 15);
	public static final BlockPos CAFE_PLAQUE = new BlockPos(-13, 5, 29);
	public static final BlockPos LOCKER_PLAQUE = new BlockPos(-31, 5, 20);
	public static final BlockPos BATTERY_PLAQUE = new BlockPos(23, 5, 24);
	/** The upper floor's rooms (layout 25, work list item 22 part B; tools/tripillar_rooms_upper.py): the six Pilot
	 * Simulators' anchors (scenarios 0 to 5: the far row from the west, then the near one), the meeting room's boardroom
	 * table, the bottle filler on the gallery (against the custodian's closet since the restrooms went, layout 38), the
	 * wall screens' anchors (the training room's lesson screen first; the map room's two since layout 38), the plaques by
	 * the rooms' doors (on the band over the door since the sliding doors, layout 47) with their labels. */
	public static final BlockPos[] PILOT_SIMULATORS = {new BlockPos(-32, 12, 9), new BlockPos(-27, 12, 9), new BlockPos(-22, 12, 9),
			new BlockPos(-32, 12, 14), new BlockPos(-27, 12, 14), new BlockPos(-22, 12, 14)};
	public static final BlockPos BOARDROOM_TABLE = new BlockPos(-34, 12, -14);
	public static final BlockPos BOTTLE_FILLER = new BlockPos(-22, 12, -39);
	public static final BlockPos LESSON_SCREEN = new BlockPos(-39, 15, -2);
	public static final BlockPos[] WALL_SCREENS = {LESSON_SCREEN, new BlockPos(14, 16, -31),
			new BlockPos(27, 14, -29), new BlockPos(-19, 14, 21), new BlockPos(-34, 14, -16), new BlockPos(-10, 15, -32),
			new BlockPos(12, 15, -36)};
	public static final BlockPos[] UPPER_PLAQUES = {new BlockPos(-26, 16, -39),
			new BlockPos(31, 16, -21), new BlockPos(27, 16, -7), new BlockPos(29, 16, 10), new BlockPos(29, 16, 19), new BlockPos(-24, 16, 4),
			new BlockPos(-11, 16, 29), new BlockPos(-22, 16, 31), new BlockPos(-37, 16, 10), new BlockPos(-27, 16, -10)};
	public static final String[] UPPER_PLAQUE_LABELS = {"custodian", "manager", "engineering_lab", "first_aid", "crew_rest",
			"simulators", "crew_lounge", "offices", "training_room", "meeting_room"};
	/** The amber slab edge round the atrium (the status ticker runs along it), and one cell of it. */
	public static final int TICKER_Y = 10;
	public static final BlockPos TICKER_EDGE = new BlockPos(0, TICKER_Y, -15);
	/** The roof the tier-1 pads stand on. */
	public static final int ROOF_Y = 20;

	// ------------------------------------------------------------------ setting up

	/** A Tower Core in the arena's ground (a new core: the Tri-Pillar, nothing built). */
	public static TowerCoreBlockEntity placeCore(GameTestHelper helper) {
		BlockPos at = helper.absolutePos(CORE);
		helper.getLevel().setBlock(at, JugcraftTower.CORE.defaultBlockState(), 3);
		return (TowerCoreBlockEntity) helper.getLevel().getBlockEntity(at);
	}

	/** A core owned by {@code owner}, built at once to {@code tier} (0: the foundation alone). */
	public static TowerCoreBlockEntity build(GameTestHelper helper, Player owner, int tier) {
		TowerCoreBlockEntity core = placeCore(helper);
		core.setOwner(owner.getUUID());
		core.buildInstantly(helper.getLevel(), tier);
		return core;
	}

	/** Fills the core with every block the tower still needs (as players, hoppers or pipes would). */
	public static void stock(TowerCoreBlockEntity core) {
		core.stockAll();
	}

	/** {@code relative} (to the core) in the world. */
	public static BlockPos at(TowerCoreBlockEntity core, BlockPos relative) {
		return core.getBlockPos().offset(relative);
	}

	/** How many of a build step's blocks stand in the world. */
	public static int countPlaced(ServerLevel level, TowerCoreBlockEntity core, TowerData.Tier step) {
		int found = 0;
		TowerData data = core.layout();
		for (int[] block : step.place) {
			if (level.getBlockState(core.getBlockPos().offset(block[0], block[1], block[2])).is(data.state(block[3]).getBlock())) {
				found++;
			}
		}
		return found;
	}

	/** What of {@code step} is missing, for a failure's message: the five blocks most missed, how many and one place each. */
	public static String missing(ServerLevel level, TowerCoreBlockEntity core, TowerData.Tier step) {
		TowerData data = core.layout();
		java.util.Map<String, int[]> by = new java.util.HashMap<>();
		java.util.Map<String, BlockPos> where = new java.util.HashMap<>();
		for (int[] block : step.place) {
			BlockPos at = core.getBlockPos().offset(block[0], block[1], block[2]);
			var want = data.state(block[3]).getBlock();
			if (!level.getBlockState(at).is(want)) {
				String key = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(want).getPath() + " (is "
						+ net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(level.getBlockState(at).getBlock()).getPath() + ")";
				by.computeIfAbsent(key, k -> new int[1])[0]++;
				where.putIfAbsent(key, new BlockPos(block[0], block[1], block[2]));
			}
		}
		StringBuilder out = new StringBuilder();
		by.entrySet().stream().sorted((a, b) -> b.getValue()[0] - a.getValue()[0]).limit(5)
				.forEach(e -> out.append(e.getKey()).append(" x").append(e.getValue()[0]).append(" at ").append(where.get(e.getKey()).toShortString()).append("; "));
		return out.toString();
	}

	/** The world position of an exchange's marked port (power in, items in), from its master cell. */
	public static BlockPos port(ServerLevel level, BlockPos master) {
		var state = level.getBlockState(master);
		if (!(state.getBlock() instanceof ExchangeStationBlock block)) {
			throw new IllegalStateException("no exchange at " + master + ": " + state);
		}
		var footprint = block.footprint(state);
		return footprint.partPos(master, state.getValue(ExchangeStationBlock.FACING), block.portPart(state));
	}

	/** The master cell of a Tri-Pillar's exchange (the energy one or the cargo one). */
	public static BlockPos master(TowerCoreBlockEntity core, boolean energy) {
		return core.getBlockPos().offset(ExchangeStations.masterOffset(energy ? ExchangeStations.ENERGY : ExchangeStations.CARGO, core.layoutVersion()));
	}
}
