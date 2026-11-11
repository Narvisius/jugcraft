package io.github.jimbozoomer.jugcraft.nuclear;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

/**
 * The working equipment of a reactor unit (docs/features/nuclear.md), each one placed whole from its item and drawn by
 * the client. Sizes are blocks across (to the placer's right), deep (away from the placer) and tall, from the
 * approved equipment sheet. Same ids and order as EQUIPMENT in tools/nuclear.py (checked by tools/check_mod_data.py).
 */
public enum EquipmentKind {
	//                         id                        wide deep tall
	REACTOR_VESSEL("reactor_pressure_vessel", 7, 7, 20),
	STEAM_GENERATOR("reactor_steam_generator", 7, 7, 21),
	COOLANT_PUMP("reactor_coolant_pump", 5, 3, 10),
	PRESSURIZER("pressurizer", 5, 3, 18),
	TURBINE_GENERATOR("turbine_generator", 11, 50, 10),
	CONDENSER("condenser", 12, 6, 6),
	FEEDWATER_PUMP("feedwater_pump", 3, 2, 3),
	MAIN_TRANSFORMER("main_transformer", 5, 5, 8),
	CONTROL_CONSOLE("reactor_control_console", 3, 2, 3),
	// The pre-made plants' own reactors (docs/features/nuclear.md, "Pre-made plants").
	EPR_VESSEL("epr_reactor_vessel", 8, 8, 22),
	SMALL_MODULAR_REACTOR("small_modular_reactor", 5, 5, 22),
	SMALL_TURBINE("small_turbine_generator", 3, 5, 4),
	// The pump house's circulating water pumps: a cooling tower only works with them on its conduit.
	CIRCULATING_WATER_PUMP("circulating_water_pump", 3, 3, 6),
	// Plant equipment that only has to look right (the plants' buildings): drawn by the client, placed whole.
	POLAR_CRANE("polar_crane", 40, 5, 5),
	REFUELING_MACHINE("refueling_machine", 15, 4, 8),
	// The turbine hall's bridge crane (turbine hall part 3, tools/hall_crane.py; HallCrane): its bridge, trolley and hooks
	// run from the crane pendant stations; only its anchor is a frame block (it moves).
	OVERHEAD_CRANE("overhead_crane", 42, 6, 5),
	MOISTURE_SEPARATOR_REHEATER("moisture_separator_reheater", 5, 20, 5),
	FEEDWATER_HEATER("feedwater_heater", 3, 12, 3),
	// The V20 set (round 31, tools/diesel_set.py): drawn from its own parts, its coupling, fan, gauges and lamps live.
	EMERGENCY_DIESEL_GENERATOR("emergency_diesel_generator", 10, 21, 7),
	STORAGE_TANK("storage_tank", 5, 5, 9),
	HEAT_EXCHANGER("heat_exchanger", 3, 9, 3),
	PUMP_SKID("pump_skid", 3, 5, 3),
	FUEL_HANDLING_MACHINE("fuel_handling_machine", 14, 5, 7),
	SPENT_FUEL_RACK("spent_fuel_rack", 4, 4, 4),
	DRY_STORAGE_CASK("dry_storage_cask", 3, 3, 6),
	TRAVELLING_WATER_SCREEN("travelling_water_screen", 6, 2, 9),
	PLANT_DISPLAY_WALL("plant_display_wall", 9, 1, 4),
	MAIN_CONTROL_BOARD("main_control_board", 6, 2, 3),
	SWITCHGEAR_LINEUP("switchgear_lineup", 8, 2, 3),
	AIR_HANDLING_UNIT("air_handling_unit", 4, 8, 3),
	// The control room's system boards (round 23): each its own layout (client/ControlBoards.java).
	ESF_BOARD("esf_control_board", 6, 2, 3),
	RCS_BOARD("rcs_control_board", 6, 2, 3),
	SECONDARY_BOARD("secondary_control_board", 6, 2, 3),
	TURBINE_BOARD("turbine_control_board", 6, 2, 3),
	ELECTRICAL_BOARD("electrical_control_board", 6, 2, 3),
	RADIATION_BOARD("radiation_control_board", 6, 2, 3),
	// The control building's remote shutdown panel (round 24): it works. It finds the nearest reactor itself, shows its
	// state, trips it, and takes control from the control room's console with its transfer switches.
	REMOTE_SHUTDOWN_PANEL("remote_shutdown_panel", 6, 1, 3),
	// The DC equipment rooms' 480 V motor control centres (round 26, 8a): they work (nuclear/MotorControlCenter.java).
	MCC_LINEUP("mcc_lineup", 4, 1, 3),
	// The control room's ventilation plant (round 26, #11; tools/hvac_boards.py): they run as the room's HVAC control panel
	// has them (HvacRoom), normal or emergency, labelled by train (their anchor's variant: 0 train A, 1 train B).
	CR_FILTRATION_UNIT("cr_filtration_unit", 3, 7, 3),
	CR_AIR_HANDLER("cr_air_handling_unit", 3, 7, 3),
	CR_CHILLER("cr_chiller", 3, 5, 3),
	CHW_PUMP("chw_pump", 1, 3, 2),
	// The diesel generator's starting air system (round 31, tools/diesel_set.py; StartingAir): each joins the nearest
	// diesel generator, labelled by its anchor's variant (the receivers 1A-1, 1A-2, 1B-1, 1B-2; the compressors 1A-A to
	// 1B-B; the dryer 1A or 1B).
	EDG_AIR_RECEIVER("edg_air_receiver", 2, 2, 5),
	EDG_AIR_COMPRESSOR("edg_air_compressor", 3, 2, 2),
	EDG_AIR_DRYER("edg_air_dryer", 2, 1, 2),
	// The diesel rooms' own plant (round 31 part 3, tools/diesel_room.py; DieselRoom): each joins the nearest diesel
	// generator as the starting air does, labelled by its anchor's variant (0 1A, 1 1B); its gauges, lamps and sight glass
	// read its generator; the CO2 bank discharges into its room (Co2System), the monorail's hoist travels and lifts from
	// its pendant (Monorail).
	EDG_JW_KEEPWARM("edg_jw_keepwarm", 3, 2, 2),
	EDG_LO_KEEPWARM("edg_lo_keepwarm", 3, 2, 2),
	EDG_EXPANSION_TANK("edg_expansion_tank", 2, 2, 5),
	EDG_JW_COOLER("edg_jw_cooler", 7, 2, 3),
	EDG_NGR("edg_ngr", 2, 2, 2),
	EDG_GEN_CUBICLE("edg_gen_cubicle", 3, 2, 3),
	EDG_CO2_BANK("edg_co2_bank", 3, 1, 2),
	EDG_MONORAIL("edg_monorail", 20, 2, 1),
	// The turbine hall's feed train and the turbine's oil plant (turbine hall part 2, tools/feed_train.py; TurbineHall): each
	// joins the nearest turbine-generator, labelled by its anchor's variant (the condenser shells and hotwells 1A to 1C,
	// the condensate pumps 1A to 1C, the main feed pumps 1A and 1B); their gauges, lamps, levels and couplings live; the
	// condensate pumps start and stop at their stations, the main feed pumps trip and reset at theirs, and the plant
	// answers (ReactorCore's feed train).
	CONDENSER_SHELL("condenser_shell", 12, 6, 6),
	CONDENSER_HOTWELL("condenser_hotwell", 12, 6, 6),
	CONDENSATE_PUMP("condensate_pump", 3, 2, 5),
	MAIN_FEED_PUMP("main_feed_pump", 4, 11, 5),
	LUBE_OIL_RESERVOIR("lube_oil_reservoir", 4, 8, 4),
	EHC_UNIT("ehc_unit", 3, 5, 3),
	SEAL_OIL_UNIT("seal_oil_unit", 3, 5, 3),
	H2_GAS_PANEL("h2_gas_panel", 4, 2, 3),
	// Turbine hall part 3 (tools/oil_room.py): the lube oil room's conditioner, storage tanks (clean and dirty: their
	// anchor's variant) and transfer pumps, joining the nearest turbine as the oil plant does.
	LO_CONDITIONER("lo_conditioner", 3, 2, 3),
	LO_STORAGE_TANK("lo_storage_tank", 3, 3, 8),
	LO_TRANSFER_PUMPS("lo_transfer_pumps", 2, 2, 2);

	public final String id;
	public final int wide, deep, tall;
	private List<BlockPos> solid;

	EquipmentKind(String id, int wide, int deep, int tall) {
		this.id = id;
		this.wide = wide;
		this.deep = deep;
		this.tall = tall;
	}

	/** The pipe kinds that join to this equipment. */
	public boolean takes(PipeKind pipe) {
		return switch (pipe) {
			case COOLANT -> this == REACTOR_VESSEL || this == EPR_VESSEL || this == STEAM_GENERATOR || this == COOLANT_PUMP || this == PRESSURIZER;
			case STEAM -> this == STEAM_GENERATOR || this == TURBINE_GENERATOR || this == FEEDWATER_PUMP || this == CONDENSER
					|| this == SMALL_MODULAR_REACTOR || this == SMALL_TURBINE;
			case WATER -> this == CONDENSER || this == SMALL_TURBINE || this == CIRCULATING_WATER_PUMP;
			case BUS -> this == TURBINE_GENERATOR || this == MAIN_TRANSFORMER;
		};
	}

	/** Plant equipment that only has to look right: it takes no pipes and does no work. */
	public boolean decor() {
		return ordinal() >= POLAR_CRANE.ordinal() && this != REMOTE_SHUTDOWN_PANEL && this != SWITCHGEAR_LINEUP && this != MCC_LINEUP && !hvac()
				&& !liveBoard() && !diesel();
	}

	/**
	 * The diesel generator set, its starting air system (round 31) and its room's own plant (round 31 part 3): drawn from
	 * their own parts (tools/diesel_set.py, tools/diesel_room.py) with their legends on their own texture, their gauges,
	 * lamps and turning parts live.
	 */
	public boolean diesel() {
		return this == EMERGENCY_DIESEL_GENERATOR || startingAir() || dieselRoom();
	}

	/** A piece of a diesel room's own plant (round 31 part 3, DieselRoom): it joins the nearest generator. */
	public boolean dieselRoom() {
		return ordinal() >= EDG_JW_KEEPWARM.ordinal() && ordinal() <= EDG_MONORAIL.ordinal();
	}

	/** A piece of a diesel generator's starting air system (StartingAir). */
	public boolean startingAir() {
		return this == EDG_AIR_RECEIVER || this == EDG_AIR_COMPRESSOR || this == EDG_AIR_DRYER;
	}

	/**
	 * A control room board that reads the plant (round 27, nuclear/ControlBoard.java): its annunciator, meters, lamps
	 * and working controls. Round 28 brought the safety systems, steam and feedwater, and radiation boards live; round 29
	 * the remote shutdown panel (its switches on its face); round 30 the reactor control console; round 32 the plant
	 * display wall (DisplayWall: its mimic, safety functions and trends, and the boards' alarms on its banner).
	 */
	public boolean liveBoard() {
		return this == RCS_BOARD || this == TURBINE_BOARD || this == ELECTRICAL_BOARD || this == ESF_BOARD || this == SECONDARY_BOARD
				|| this == RADIATION_BOARD || this == REMOTE_SHUTDOWN_PANEL || this == CONTROL_CONSOLE || this == PLANT_DISPLAY_WALL;
	}

	/** The control room's HVAC machines (round 26, #11): run and stopped by the room's HVAC control panel (HvacRoom). */
	public boolean hvac() {
		return this == CR_FILTRATION_UNIT || this == CR_AIR_HANDLER || this == CR_CHILLER || this == CHW_PUMP;
	}

	/** A working power board: a load centre or a motor control centre (a bus, breakers, feeders). */
	public boolean board() {
		return this == SWITCHGEAR_LINEUP || this == MCC_LINEUP;
	}

	/** Equipment that holds a reactor core (and is run from a console). */
	public boolean reactor() {
		return this == REACTOR_VESSEL || this == EPR_VESSEL || this == SMALL_MODULAR_REACTOR;
	}

	/**
	 * Whether this kind can stand hidden (EquipmentAnchorBlock.HIDDEN, 22 October 2026): the medium plant's reactor
	 * equipment, under its reactor building's floor, whose look is the building's compiled site (tools/reactor_site.py):
	 * only its anchor, no frame, nothing drawn, joined by buried pipe at the anchor itself.
	 */
	public boolean hideable() {
		return this == REACTOR_VESSEL || this == STEAM_GENERATOR || this == COOLANT_PUMP || this == PRESSURIZER;
	}

	/** The core design of a reactor. */
	public ReactorCore.Design design() {
		return switch (this) {
			case EPR_VESSEL -> ReactorCore.Design.EPR;
			case SMALL_MODULAR_REACTOR -> ReactorCore.Design.SMALL_MODULAR;
			default -> ReactorCore.Design.STANDARD;
		};
	}

	/** Equipment that takes cooling water (and, with a cooling tower on its conduit, only make-up water). */
	public boolean cooled() {
		return this == CONDENSER || this == SMALL_TURBINE;
	}

	/** Equipment that sends a unit's power out into cables. */
	public boolean powerOut() {
		return this == MAIN_TRANSFORMER || this == SMALL_TURBINE;
	}

	/**
	 * Whether a block cell (local coordinates: x to the right, y up, z away from the placer) is part of the
	 * equipment's body: the cells that get a frame block. Round vessels leave their corners free.
	 */
	public boolean solid(int x, int y, int z) {
		double cx = x + 0.5 - wide / 2.0, cz = z + 0.5 - deep / 2.0;
		double r2 = cx * cx + cz * cz;
		return switch (this) {
			// The vessel on its support skirt; the drive housings on top are narrower.
			case REACTOR_VESSEL -> y < 14 ? r2 <= 3.3 * 3.3 : r2 <= 2.4 * 2.4;
			// The narrow tube bundle below, the wide steam drum above (a cone between).
			case STEAM_GENERATOR -> y < 10 ? r2 <= 2.5 * 2.5 : r2 <= 3.3 * 3.3;
			// The pump casing at the front, the tall motor stand behind it.
			case COOLANT_PUMP -> y < 4 || (y < 9 && z >= 1);
			case PRESSURIZER -> (cx * cx) <= 1.6 * 1.6;
			// Turbine hall part 1 (tools/turbine_set.py): the machine standing on the operating floor.
			case TURBINE_GENERATOR -> turbineSolid(x, y, z);
			case MAIN_TRANSFORMER -> y < 5 && x < 5 && z < 5;
			// The EPR's larger vessel: wider shell, a taller drive forest on top.
			case EPR_VESSEL -> y < 15 ? r2 <= 3.8 * 3.8 : r2 <= 2.8 * 2.8;
			// A slim integral module (reactor, steam generator and pressurizer in one steel containment).
			case SMALL_MODULAR_REACTOR -> r2 <= 2.4 * 2.4;
			case EMERGENCY_DIESEL_GENERATOR -> dieselSolid(x, y, z);
			// Turbine hall part 2: a column left open inside the hotwell for the circulating water conduit up to its condenser.
			case CONDENSER_HOTWELL -> !(x == 5 && z == 1);
			// Turbine hall part 3: the crane moves along its runway; only its anchor stands.
			case OVERHEAD_CRANE -> x == 0 && y == 0 && z == 0;
			default -> true;
		};
	}

	/**
	 * The turbine-generator's body (turbine hall part 1, tools/turbine_set.py; tools/nuclear_plants.py turbine_solid): the
	 * front standard, the HP and its steam chests either side, the bearing pedestals, the LPs' hoods all across with their
	 * covers and intercept valves above, the turning gear and its station, the generator and its coolers, the exciter.
	 */
	static boolean turbineSolid(int x, int y, int z) {
		if (z <= 2) {
			return x >= 4 && x <= 6 && y <= 2;
		}
		if (z <= 8) {
			return x >= 3 && x <= 7 && y <= 2 || (x <= 2 || x >= 8) && y <= 3;
		}
		if (z == 9 || z == 18 || z == 27) {
			return x >= 4 && x <= 6 && y <= 2;
		}
		if (z <= 35) {
			return y <= 2 || x >= 1 && x <= 9 && y <= 5;
		}
		if (z <= 37) {
			return x >= 3 && x <= 6 && y <= 2 || x == 2 && y <= 1 && z == 37;
		}
		if (z <= 46) {
			return x >= 3 && x <= 7 && y <= 3 || x >= 4 && x <= 6 && y == 4 && (z >= 39 && z <= 41 || z >= 43 && z <= 45);
		}
		return x >= 4 && x <= 6 && y <= 2;
	}

	/**
	 * The main turbine-generator and its moisture separator reheaters (turbine hall part 1, tools/turbine_set.py): drawn
	 * from their own parts with their legends on their own texture; the turbine's valve stems, gauges, speed, lamps, trip
	 * handle and turning gear lever, the reheaters' gauges live (TurbineHall).
	 */
	public boolean turbine() {
		return this == TURBINE_GENERATOR || this == MOISTURE_SEPARATOR_REHEATER || feedTrain();
	}

	/**
	 * The turbine hall's condenser (since part 2 drawn from its own parts, its hotwell under it) and the hall's feed train
	 * and oil plant (turbine hall part 2): each joins the nearest turbine-generator and shows its unit's plant.
	 */
	public boolean feedTrain() {
		return this == CONDENSER || ordinal() >= CONDENSER_SHELL.ordinal() && ordinal() <= LO_TRANSFER_PUMPS.ordinal();
	}

	/** How many labels a piece has (its anchor's variant): the condenser shells', hotwells' and pumps' 1A, 1B... */
	public int labels() {
		return switch (this) {
			case CONDENSER, CONDENSER_SHELL, CONDENSER_HOTWELL, CONDENSATE_PUMP -> 3;
			case MAIN_FEED_PUMP, MOISTURE_SEPARATOR_REHEATER, LO_STORAGE_TANK -> 2;
			default -> 1;
		};
	}

	/**
	 * How a frame cell stands: a whole block, or half a block high where the piece's steps are drawn (so they can be
	 * walked up: the diesel generator's steps to its walkways).
	 */
	public EquipmentFrameBlock.Shape frameShape(int x, int y, int z) {
		if (this == EMERGENCY_DIESEL_GENERATOR && (x <= 2 || x >= 7) && (z == 12 && y == 1 || z == 14 && y == 0)) {
			return EquipmentFrameBlock.Shape.HALF;
		}
		return EquipmentFrameBlock.Shape.FULL;
	}

	/**
	 * The V20 diesel generator set's body (round 31, tools/diesel_set.py; tools/nuclear_plants.py diesel_solid): the
	 * engine, its free end and the generator along the middle (x 3 to 6), the exhaust manifold and the generator's air
	 * hood a block higher; the walkways either side (decks at 2), their steps down beside the generator (1.5, 1, 0.5:
	 * half-block frames where they end in a half); the gauge board at the front left, the fuel filter at the front right,
	 * the coolers and risers beside the free end, the terminal box on the generator's left.
	 */
	static boolean dieselSolid(int x, int y, int z) {
		if (x >= 3 && x <= 6) {
			if (z <= 1) {
				return y < 7;
			}
			if (z <= 12) {
				return y < 5 || x >= 4 && x <= 5 && y < 6;
			}
			return y < 5 || x >= 4 && x <= 5 && z >= 15 && z <= 17 && y < 6;
		}
		if (z <= 1) {
			if (x == 2 || x == 7) {
				return y < 6;
			}
			if (x <= 1) {
				return z == 0 && y < 3;
			}
			return x == 8 && z == 1 && y < 2;
		}
		if (z <= 12) {
			return y < 2;
		}
		if (z <= 14) {
			return y < 1;
		}
		return x == 2 && z >= 16 && z <= 17 && y < 3;
	}

	/** Every solid cell, local coordinates (the anchor's own cell included if solid). */
	public List<BlockPos> solidCells() {
		if (solid == null) {
			List<BlockPos> cells = new ArrayList<>();
			for (int y = 0; y < tall; y++) {
				for (int z = 0; z < deep; z++) {
					for (int x = 0; x < wide; x++) {
						if (solid(x, y, z)) {
							cells.add(new BlockPos(x, y, z));
						}
					}
				}
			}
			solid = List.copyOf(cells);
		}
		return solid;
	}

	/** The cells that get frame blocks: every solid cell but the anchor's own. */
	public List<BlockPos> frames(Direction facing) {
		List<BlockPos> out = new ArrayList<>();
		for (BlockPos local : solidCells()) {
			if (!local.equals(BlockPos.ZERO)) {
				out.add(toWorld(local, facing));
			}
		}
		return out;
	}

	/** A local cell as an offset from the anchor in the world, for equipment placed facing {@code facing}. */
	public static BlockPos toWorld(BlockPos local, Direction facing) {
		Direction right = facing.getClockWise();
		return new BlockPos(right.getStepX() * local.getX() + facing.getStepX() * local.getZ(), local.getY(),
				right.getStepZ() * local.getX() + facing.getStepZ() * local.getZ());
	}

	/** An offset from the anchor back to local coordinates (the inverse of {@link #toWorld}). */
	public static BlockPos toLocal(BlockPos offset, Direction facing) {
		Direction right = facing.getClockWise();
		int x = right.getStepX() * offset.getX() + right.getStepZ() * offset.getZ();
		int z = facing.getStepX() * offset.getX() + facing.getStepZ() * offset.getZ();
		return new BlockPos(x, offset.getY(), z);
	}

	public static EquipmentKind byId(String id) {
		for (EquipmentKind kind : values()) {
			if (kind.id.equals(id)) {
				return kind;
			}
		}
		return null;
	}
}
