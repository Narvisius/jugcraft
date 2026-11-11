package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.nuclear.EquipmentBlockEntity;
import io.github.jimbozoomer.jugcraft.nuclear.EquipmentKind;
import io.github.jimbozoomer.jugcraft.nuclear.Switchgear;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Draws the reactor unit's equipment in 3D from the anchor (docs/features/nuclear.md): the pressure vessel on its
 * skirt with the control-rod drive forest on its head (the drive rods rise as the rods come out; the head comes off
 * while refuelling), the steam generators with their drum, cone and walkway, the coolant pumps and their big motors,
 * the pressurizer, the condenser with its water boxes, the feedwater pump skid, the main
 * transformer with radiators, conservator and bushings, and the control console (its lamps show a trip). (The
 * turbine-generator is drawn from its own parts since turbine hall part 1: client/TurbineParts.java.)
 *
 * <p>Geometry is written in the equipment's own frame: x to the placer's right, y up, z away from the placer, in
 * blocks from the anchor's corner. Every face is tiled about a block to a texture so nothing looks stretched.
 * Materials are 128-pixel regions of textures/entity/nuclear_equipment.png (tools/nuclear.py EQ_MATERIALS).
 */
public class NuclearRenderer implements BlockEntityRenderer<EquipmentBlockEntity, NuclearRenderer.State>, Sight.Always {   // its drawing also puffs smoke
	public static final Identifier TEXTURE = Jugcraft.id("textures/entity/nuclear_equipment.png");
	private static final RenderType RENDER_TYPE = RenderTypes.entityCutout(TEXTURE);
	/**
	 * The sheet's rows of 128-pixel cells (eight across): 1024 x 1408 since turbine hall part 1 (tools/nuclear.py EQ_ROWS, EQ_CELL;
	 * 8e: the lettered cells drawn at 128 px so their nameplates read, the others' 64 px texels doubled).
	 */
	static final int ROWS = 11;
	private static final int CELL = 128;
	private static final float PX = 1F / (8 * CELL), PX_V = 1F / (CELL * ROWS);

	static final float[] STAINLESS = region(0, 0);
	static final float[] STAINLESS_DARK = region(1, 0);
	static final float[] CARBON_STEEL = region(2, 0);
	static final float[] PAINT_GREY = region(3, 0);
	static final float[] PAINT_GREEN = region(4, 0);
	static final float[] PAINT_BLUE = region(5, 0);
	static final float[] PAINT_WHITE = region(6, 0);
	static final float[] CONCRETE = region(7, 0);
	static final float[] GRATING = region(0, 1);
	static final float[] HAZARD = region(1, 1);
	static final float[] INSULATION = region(2, 1);
	static final float[] WELD_BAND = region(3, 1);
	static final float[] FLANGE = region(4, 1);
	static final float[] CRDM = region(5, 1);
	static final float[] COPPER = region(6, 1);
	static final float[] TANK_GREY = region(7, 1);
	static final float[] RADIATOR = region(0, 2);
	static final float[] GLAZE_BROWN = region(1, 2);
	static final float[] GLASS = region(2, 2);
	static final float[] SCREEN = region(3, 2);
	static final float[] DESK = region(4, 2);
	static final float[] RUBBER = region(5, 2);
	static final float[] LAMP_RED = region(6, 2);
	static final float[] LAMP_GREEN = region(7, 2);
	static final float[] TREFOIL_SIGN = region(0, 3);
	static final float[] GENERATOR = region(1, 3);
	static final float[] FINS = region(2, 3);
	static final float[] HANDRAIL = region(3, 3);
	static final float[] NAMEPLATE = region(4, 3);
	static final float[] TURBINE_BLUE = region(5, 3);
	static final float[] PIPE_RED = region(6, 3);
	static final float[] WATER = region(7, 3);
	static final float[] PAINT_YELLOW = region(0, 4);
	static final float[] MIMIC = region(1, 4);
	static final float[] DISPLAY = region(2, 4);
	static final float[] CHERENKOV = region(3, 4);
	static final float[] FUEL_RACK = region(4, 4);
	static final float[] LOUVRE = region(5, 4);
	static final float[] SWITCHGEAR = region(6, 4);
	static final float[] FILTER_BANK = region(7, 4);
	static final float[] SIGN_PANEL = region(0, 5);
	static final float[] SIGN_EDGE = region(1, 5);
	static final float[] ENGINE_GREEN = region(2, 5);
	static final float[] ROCKER = region(3, 5);
	static final float[] GUNMETAL = region(4, 5);
	static final float[] RED_IRON = region(5, 5);
	static final float[] PIPE_GREEN = region(6, 5);
	static final float[] PIPE_BROWN = region(7, 5);
	static final float[] PIPE_YELLOW = region(0, 6);
	static final float[] PIPE_LBLUE = region(1, 6);
	static final float[] CRANKCASE_DOOR = region(2, 6);
	static final float[] GAUGE_BOARD = region(3, 6);
	static final float[] SIGN_AUTOSTART = region(4, 6);
	static final float[] STENCIL_EDG = region(5, 6);
	static final float[] TURBO = region(6, 6);
	static final float[] BLACK = region(7, 6);
	// The control room's boards (round 23, client/ControlBoards.java): lamps, annunciator windows, meters, charts.
	static final float[] LAMP_WHITE = region(0, 7);
	static final float[] LAMP_AMBER = region(1, 7);
	static final float[] ANNUNCIATOR = region(2, 7);
	static final float[] METER = region(3, 7);
	static final float[] DIAL = region(4, 7);
	static final float[] CHART = region(5, 7);
	static final float[] DRPI = region(6, 7);
	static final float[] SWATCHES = region(7, 7);
	static final float[] NAMEPLATES = region(0, 8);
	static final float[] NAMEPLATES2 = region(1, 8);
	static final float[] NAMEPLATES_INST = region(2, 8);
	static final float[] TITLES = region(3, 8);
	static final float[] DIGITAL = region(4, 8);
	static final float[] PLACARDS = region(5, 8);
	static final float[] BINDERS = region(6, 8);
	static final float[] NAMEPLATES3 = region(7, 8);
	static final float[] NAMEPLATES4 = region(0, 9);
	// The control room HVAC machines (round 26, #11; tools/hvac_boards.py): title plates by train, nameplates, placards.
	static final float[] HVAC_TITLES_EFU = region(1, 9);
	static final float[] HVAC_TITLES_AHU = region(2, 9);
	static final float[] HVAC_TITLES_CHILLER = region(3, 9);
	static final float[] HVAC_TITLES_PUMP = region(4, 9);
	static final float[] NAMEPLATES5 = region(5, 9);
	static final float[] NAMEPLATES6 = region(6, 9);
	static final float[] HVAC_PLACARDS = region(7, 9);
	// Turbine hall part 1: the turbine's lagging, its LP hoods' paint and the generator's frame (tools/turbine_set.py).
	static final float[] TURBINE_LAGGING = region(0, 10);
	static final float[] LP_HOOD = region(1, 10);
	static final float[] GEN_FRAME = region(2, 10);
	// Turbine hall part 2: the condenser shells' and the oil plant's paint (tools/feed_train.py).
	static final float[] COND_SHELL = region(3, 10);
	/** Every material on the sheet by name, in the sheet's order (tools/nuclear.py EQ_MATERIALS). */
	static final String[] SHEET = {
			"stainless", "stainless_dark", "carbon_steel", "paint_grey", "paint_green", "paint_blue", "paint_white",
			"concrete", "grating", "hazard", "insulation", "weld_band", "flange", "crdm", "copper", "tank_grey", "radiator",
			"glaze_brown", "glass", "screen", "desk", "rubber", "lamp_red", "lamp_green", "trefoil_sign", "generator", "fins",
			"handrail", "nameplate", "turbine_blue", "pipe_red", "water", "paint_yellow", "mimic", "display", "cherenkov",
			"fuel_rack", "louvre", "switchgear", "filter_bank", "sign_panel", "sign_edge", "engine_green", "rocker",
			"gunmetal", "red_iron", "pipe_green", "pipe_brown", "pipe_yellow", "pipe_lblue", "crankcase_door", "gauge_board",
			"sign_autostart", "stencil_edg", "turbo", "black", "lamp_white", "lamp_amber", "annunciator", "meter", "dial",
			"chart", "drpi", "swatches", "nameplates", "nameplates2", "nameplates_inst", "titles", "digital", "placards",
			"binders", "nameplates3", "nameplates4",
			// The control room HVAC machines' title plates by train, nameplates and placards (round 26, #11).
			"hvac_titles_efu", "hvac_titles_ahu", "hvac_titles_chiller", "hvac_titles_pump", "nameplates5", "nameplates6", "hvac_placards",
			// The main turbine-generator (turbine hall part 1, tools/turbine_set.py): lagging, the LP hoods' plate, the generator's paint.
			"turbine_lagging", "lp_hood", "gen_frame",
			// The condenser shells' and the oil plant's paint (turbine hall part 2, tools/feed_train.py).
			"cond_shell"};
	private static final java.util.Map<String, float[]> BY_NAME = new java.util.HashMap<>();

	static {
		for (int i = 0; i < SHEET.length; i++) {
			BY_NAME.put(SHEET[i], region(i % 8, i / 8));
		}
	}

	/** A sheet material by its name (tools/nuclear.py EQ_MATERIALS). */
	static float[] material(String name) {
		float[] uv = BY_NAME.get(name);
		return uv != null ? uv : PAINT_GREY;
	}

	private static float[] region(int col, int row) {
		return new float[] {(col * CELL + 0.5F) * PX, (row * CELL + 0.5F) * PX_V, (col * CELL + CELL - 0.5F) * PX, (row * CELL + CELL - 0.5F) * PX_V};
	}

	private final Font font;

	public NuclearRenderer(net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context context) {
		this.font = context.font();
	}

	public static class State extends BlockEntityRenderState {
		EquipmentKind kind = EquipmentKind.CONTROL_CONSOLE;
		Direction facing = Direction.NORTH;
		int lightLevel;
		/** Reactor vessel: rod insertion (0 out .. 100 in), head off for refuelling. */
		float rods = 100;
		boolean open;
		/** Console: the linked reactor is tripped. */
		boolean tripped;
		float time;
		/** Full detail, or the far-away mesh. */
		boolean detail = true;
		/** A diesel generator: running, its speed (0..1) and its shaft's turn (degrees). */
		boolean running;
		float speed, angle;
		/**
		 * The diesel generator set and its starting air (round 31, client/DieselParts.java): the pressure its gauge shows
		 * (psig), ready to start (the set), joined to its set (an air piece), the set's engine readings close up.
		 */
		double air;
		boolean ready, joined;
		/** A diesel room's monorail (round 31 part 3): its trolley's place along the beam and its hook's drop (blocks). */
		double hoistX = 2, hoistDrop;
		EdgTelemetry.@org.jspecify.annotations.Nullable Snapshot engine;
		/**
		 * A remote shutdown panel: it has found a reactor, the reactor is there (loaded on the server), control is at
		 * the panel; the reactor's power and output (0..1), core temperature, and its fuel damaged.
		 */
		boolean panelLinked, panelOnline, remote, fuelDamaged;
		float power, output, temperature;
		/** A load centre: its breakers (a bit each, closed when set), its bus's charge (0..1), each breaker's flow (JE/t). */
		int breakers = Switchgear.DEFAULT_BREAKERS;
		/** Which labelled version it is (its anchor's variant: MCC 1A or 1B; round 26, 8c). */
		int variant;
		/** Standing hidden under a building whose compiled site draws it (the medium plant's reactor equipment). */
		boolean hidden;
		/** A control room HVAC machine (round 26, #11): where it is (its gauges' wander), as {@link #running}, {@link #speed}. */
		int seed;
		float charge;
		final int[] flows = new int[Switchgear.BREAKERS];
		/** A load centre (older rooms round 1, client/LoadCentreParts.java): what it shows; null for anything else. */
		LoadCentreParts.@org.jspecify.annotations.Nullable View lc;
		/** A live control board (round 27, client/LiveBoards.java): its state as the server last sent it, and the game time. */
		io.github.jimbozoomer.jugcraft.nuclear.ControlBoard.@org.jspecify.annotations.Nullable State board;
		double clock;
		/** The main turbine-generator or a reheater (turbine hall part 1, client/TurbineParts.java): what its live parts show. */
		TurbineParts.Readings turbine = TurbineParts.Readings.STILL;
		/** The turbine hall's overhead crane (turbine hall part 3, client/CraneParts.java): where its motions are now. */
		CraneParts.@org.jspecify.annotations.Nullable View crane;
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public boolean shouldRenderOffScreen() {
		return true;
	}

	@Override
	public int getViewDistance() {
		return 192;
	}

	@Override
	public void extractRenderState(EquipmentBlockEntity equipment, State state, float partialTick, Vec3 cameraPos,
			ModelFeatureRenderer.CrumblingOverlay crumbling) {
		BlockEntityRenderState.extractBase(equipment, state, crumbling);
		state.kind = equipment.kind();
		state.facing = equipment.facing();
		state.hidden = io.github.jimbozoomer.jugcraft.nuclear.EquipmentAnchorBlock.hidden(equipment.getBlockState());
		Level level = equipment.getLevel();
		if (level == null) {
			return;
		}
		BlockPos pos = equipment.getBlockPos();
		BlockPos lightAt = pos.offset(EquipmentKind.toWorld(new BlockPos(state.kind.wide / 2, state.kind.tall, state.kind.deep / 2), state.facing));
		state.lightLevel = LightCoordsUtil.getLightCoords(level, lightAt);
		state.rods = (float) equipment.core.rods;
		state.open = equipment.core.refuelOpen;
		state.tripped = false;
		if (state.kind == EquipmentKind.REMOTE_SHUTDOWN_PANEL) {
			// Its own copy of its reactor's state (synced with the panel).
			state.panelLinked = equipment.linked != null;
			state.panelOnline = state.panelLinked && equipment.panelOnline;
			state.tripped = state.panelOnline && equipment.core.scrammed;
			state.remote = state.panelOnline && equipment.core.remote;
			state.fuelDamaged = state.panelOnline && equipment.core.fuelDamaged;
			state.power = (float) equipment.core.fission;
			state.output = equipment.core.electric / (float) Math.max(1, equipment.core.design.ratedJe());
			state.temperature = (float) equipment.core.temperature;
		}
		state.variant = equipment.getBlockState().hasProperty(io.github.jimbozoomer.jugcraft.nuclear.EquipmentAnchorBlock.VARIANT)
				? equipment.getBlockState().getValue(io.github.jimbozoomer.jugcraft.nuclear.EquipmentAnchorBlock.VARIANT) : 0;

		if (state.kind.board() && equipment.bus != null) {
			state.breakers = equipment.breakers;
			state.charge = equipment.bus.getAmount() / (float) equipment.busCapacity();
			System.arraycopy(equipment.flows, 0, state.flows, 0, state.flows.length);
		}
		state.lc = null;
		if (state.kind == EquipmentKind.SWITCHGEAR_LINEUP && equipment.lc != null) {
			double t = level.getGameTime() + partialTick;
			state.lc = new LoadCentreParts.View(equipment.breakers, equipment.lc, state.charge, t, equipment.lc.fans ? (t * 28) % 360 : 15);
		}
		state.time = (level.getGameTime() % 24_000) + partialTick;
		if (state.kind.hvac()) {
			// Its fan or pump comes up to speed (and its readings with it) over three seconds, and runs down as long.
			float since = level.getGameTime() - equipment.runningSince + partialTick;
			state.running = equipment.running;
			state.speed = equipment.running ? Math.min(1F, since / HVAC_RAMP) : Math.max(0F, 1F - since / HVAC_RAMP);
			state.seed = pos.hashCode();
		}
		if (state.kind == EquipmentKind.EMERGENCY_DIESEL_GENERATOR) {
			diesel(equipment, state, level, partialTick);
		}
		Vec3 middle = Vec3.atCenterOf(pos.offset(EquipmentKind.toWorld(new BlockPos(state.kind.wide / 2, state.kind.tall / 2, state.kind.deep / 2), state.facing)));
		double reach = Math.max(state.kind.wide, Math.max(state.kind.deep, state.kind.tall)) / 2.0;
		state.detail = cameraPos.distanceTo(middle) - reach < DETAIL_DISTANCE;
		state.engine = null;
		if (state.kind.diesel()) {
			// The set's gauges read what its panels do (EdgTelemetry); a compressor's pulley comes up to speed in a second
			// and a half and runs down in two.
			state.air = equipment.air;
			state.joined = equipment.linked != null;
			if (state.kind == EquipmentKind.EMERGENCY_DIESEL_GENERATOR) {
				state.engine = state.detail ? EdgTelemetry.of(equipment, level.getGameTime() + partialTick) : null;
				state.ready = state.engine != null && state.engine.ready;
			} else if (state.kind == EquipmentKind.EDG_AIR_COMPRESSOR) {
				DieselParts.spin(equipment, level, partialTick, 30, 40, 4);
				state.running = equipment.running;
				state.angle = equipment.animAngle;
			} else if (state.kind.dieselRoom()) {
				// The room's own plant (round 31 part 3): its gauges and lamps read the generator it has joined; the
				// monorail's hoist where its pendant has driven it, moving there.
				state.running = false;
				if (state.detail && equipment.linked != null && level.getBlockEntity(equipment.linked) instanceof EquipmentBlockEntity edg
						&& edg.kind() == EquipmentKind.EMERGENCY_DIESEL_GENERATOR) {
					state.engine = EdgTelemetry.of(edg, level.getGameTime() + partialTick);
					state.running = edg.running;
				}
				if (state.kind == EquipmentKind.EDG_MONORAIL) {
					double t = level.getGameTime() + partialTick;
					state.hoistX = io.github.jimbozoomer.jugcraft.nuclear.Monorail.trolley(equipment, t);
					state.hoistDrop = io.github.jimbozoomer.jugcraft.nuclear.Monorail.hook(equipment, t);
				}
			}
		}
		if (state.kind.turbine()) {
			// Turbine hall part 1: the turbine's own synced state; a reheater reads the turbine it has joined. Part 2: a piece
			// of the feed train or the oil plant its own (its unit's plant as its turbine has it); a pump's coupling turns
			// while it runs.
			EquipmentBlockEntity t = state.kind == EquipmentKind.TURBINE_GENERATOR || state.kind.feedTrain() ? equipment
					: equipment.linked != null && level.getBlockEntity(equipment.linked) instanceof EquipmentBlockEntity tt
							&& tt.kind() == EquipmentKind.TURBINE_GENERATOR ? tt : null;
			if (t == null) {
				state.turbine = TurbineParts.Readings.STILL;
			} else {
				if (state.kind == EquipmentKind.TURBINE_GENERATOR) {
					TurbineParts.spin(equipment, t.turbineRpm);
				} else if (state.kind == EquipmentKind.CONDENSATE_PUMP || state.kind == EquipmentKind.MAIN_FEED_PUMP) {
					TurbineParts.spin(equipment, (t.turbineFlags & io.github.jimbozoomer.jugcraft.nuclear.TurbineHall.RUN) != 0 ? 1185 : 0);
				}
				double guard = level.getGameTime() < t.tripGuardUntil ? 1 : 0;
				state.turbine = new TurbineParts.Readings(t.turbineRpm, t.turbineLoad, t.turbineFlags, guard, equipment.animAngle);
			}
		}
		state.crane = state.kind == EquipmentKind.OVERHEAD_CRANE ? CraneParts.View.of(equipment, level.getGameTime() + partialTick) : null;
		if (state.crane != null) {
			// Its detail by where its bridge is now (it runs the length of the hall from its anchor).
			Vec3 at = middle.add(state.facing.getStepX() * state.crane.bridge(), 0, state.facing.getStepZ() * state.crane.bridge());
			state.detail = cameraPos.distanceTo(at) - reach < DETAIL_DISTANCE;
		}
		state.board = null;
		if (state.kind.liveBoard()) {
			// Its pointers swing toward their readings as it is drawn.
			state.board = equipment.board();
			state.clock = level.getGameTime() + partialTick;
			if (state.detail) {
				LiveBoards.readings(state.board, state.kind, state.clock);
			}
		}
	}

	/**
	 * A diesel generator's run: started, the air start turns it over slowly for a second and a half, it fires (a puff
	 * of black smoke at the stack) and runs up to speed in two and a half seconds; stopped, it coasts down over four.
	 * Running, exhaust smoke rises from the top of its stack (found above the exhaust's roof thimble).
	 */
	private static void diesel(EquipmentBlockEntity e, State state, Level level, float partialTick) {
		long nanos = System.nanoTime();
		float dt = e.animNanos == 0 ? 0 : Math.min(0.1F, (nanos - e.animNanos) / 1.0E9F);
		e.animNanos = nanos;
		long since = level.getGameTime() - e.runningSince;
		if (e.running) {
			float t = since + partialTick;
			e.animSpeed = t < 30 ? 0.12F : Math.min(1F, 0.12F + (t - 30) / 50F * 0.88F);
		} else {
			e.animSpeed = Math.max(0, e.animSpeed - dt * 0.25F);
		}
		e.animAngle = (e.animAngle + e.animSpeed * 540F * dt) % 360F;
		state.running = e.running;
		state.speed = e.animSpeed;
		state.angle = e.animAngle;
		if (!e.running || since < 28 || level.getGameTime() == e.smokeTick) {
			return;
		}
		e.smokeTick = level.getGameTime();
		// Where the exhaust comes out: straight up from its roof thimble (tools/diesel_set.py: live "stack", its x, the
		// roof's height and its z) to the first open air (the stack's top).
		com.google.gson.JsonObject live = ControlBoards.live(state.kind);
		com.google.gson.JsonArray stack = live != null && live.has("stack") ? live.getAsJsonArray("stack") : null;
		Direction right = state.facing.getClockWise();
		double lx = (stack == null ? 5.0 : stack.get(0).getAsDouble()) - 0.5, lz = (stack == null ? 1.45 : stack.get(2).getAsDouble()) - 0.5;
		double roof = stack == null ? 13 : stack.get(1).getAsDouble();
		BlockPos pos = e.getBlockPos();
		double x = pos.getX() + 0.5 + right.getStepX() * lx + state.facing.getStepX() * lz;
		double z = pos.getZ() + 0.5 + right.getStepZ() * lx + state.facing.getStepZ() * lz;
		if (e.smokeY == Integer.MIN_VALUE || level.getGameTime() % 200 == 0) {
			BlockPos p = BlockPos.containing(x, pos.getY() + roof + 0.05, z);
			for (int k = 0; k < 16 && !level.getBlockState(p).isAir(); k++) {
				p = p.above();
			}
			e.smokeY = p.getY();
		}
		net.minecraft.util.RandomSource random = level.getRandom();
		double y = e.smokeY + 0.05;
		if (since < 70) {
			// It has just fired: thick black smoke.
			for (int k = 0; k < 3; k++) {
				level.addParticle(net.minecraft.core.particles.ParticleTypes.LARGE_SMOKE, x + random.nextGaussian() * 0.25, y,
						z + random.nextGaussian() * 0.25, random.nextGaussian() * 0.02, 0.18 + random.nextDouble() * 0.08, random.nextGaussian() * 0.02);
			}
		} else if (level.getGameTime() % 3 == 0) {
			level.addParticle(net.minecraft.core.particles.ParticleTypes.LARGE_SMOKE, x + random.nextGaussian() * 0.2, y,
					z + random.nextGaussian() * 0.2, 0, 0.12 + random.nextDouble() * 0.05, 0);
			if (random.nextInt(4) == 0) {
				level.addParticle(net.minecraft.core.particles.ParticleTypes.CAMPFIRE_COSY_SMOKE, x, y + 0.3, z, 0, 0.05, 0);
			}
		}
	}

	/** Beyond this many blocks the low-detail mesh is drawn. */
	static final double DETAIL_DISTANCE = 40;
	/** Static meshes by kind and detail (index kind.ordinal() * 2 + (detail ? 1 : 0)). */
	private static final Mesh[] STATIC = new Mesh[EquipmentKind.values().length * 2];
	/** The vessel's closed head with its drive housings, by vessel (standard, EPR) and detail; off while refuelling. */
	private static final Mesh[][] HEAD = new Mesh[2][2];
	/** The vessel's drive rods at each 5% of travel, by vessel and detail. */
	private static final Mesh[][][] RODS = new Mesh[2][2][21];
	/** The EPR's vessel is the standard one drawn larger: 8 x 8 x 22 instead of 7 x 7 x 20. */
	static final double[] EPR_SCALE = {8 / 7.0, 22 / 20.0, 8 / 7.0};

	static Mesh staticMesh(EquipmentKind kind, boolean detail) {
		int i = kind.ordinal() * 2 + (detail ? 1 : 0);
		if (STATIC[i] == null) {
			Mesh mesh = new Mesh();
			Frame f = new Frame(mesh, detail);
			switch (kind) {
				case REACTOR_VESSEL -> vessel(f);
				case STEAM_GENERATOR -> steamGenerator(f);
				case COOLANT_PUMP -> coolantPump(f);
				case PRESSURIZER -> pressurizer(f);
				case FEEDWATER_PUMP -> feedwaterPump(f);
				case MAIN_TRANSFORMER -> transformer(f);
				case CONTROL_CONSOLE -> console(f);
				case EPR_VESSEL -> {
					f.scale(EPR_SCALE);
					vessel(f);
				}
				case SMALL_MODULAR_REACTOR -> smallModule(f);
				case SMALL_TURBINE -> smallTurbine(f);
				case CIRCULATING_WATER_PUMP -> circulatingWaterPump(f);
				default -> PlantDecor.draw(kind, f);
			}
			STATIC[i] = mesh;
		}
		return STATIC[i];
	}

	private static final Mesh[] GLOW = new Mesh[EquipmentKind.values().length * 2];

	static Mesh glowMesh(EquipmentKind kind, boolean detail) {
		int i = kind.ordinal() * 2 + (detail ? 1 : 0);
		if (GLOW[i] == null) {
			Mesh mesh = new Mesh();
			PlantDecor.glow(kind, new Frame(mesh, detail));
			GLOW[i] = mesh;
		}
		return GLOW[i];
	}

	static Mesh headMesh(boolean epr, boolean detail) {
		int i = detail ? 1 : 0, k = epr ? 1 : 0;
		if (HEAD[k][i] == null) {
			Mesh mesh = new Mesh();
			Frame f = new Frame(mesh, detail);
			if (epr) f.scale(EPR_SCALE);
			vesselHead(f);
			HEAD[k][i] = mesh;
		}
		return HEAD[k][i];
	}

	static Mesh rodsMesh(boolean epr, float rods, boolean detail) {
		int step = Math.max(0, Math.min(20, Math.round(rods / 5)));
		int i = detail ? 1 : 0, k = epr ? 1 : 0;
		if (RODS[k][i][step] == null) {
			Mesh mesh = new Mesh();
			Frame f = new Frame(mesh, detail);
			if (epr) f.scale(EPR_SCALE);
			vesselRods(f, step * 5);
			RODS[k][i][step] = mesh;
		}
		return RODS[k][i][step];
	}

	@Override
	public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
		if (state.hidden) {
			return;
		}
		boolean detail = state.detail;
		// A load centre's live parts (the sheet's: still and lit; its own texture's: the legend pass), made once a frame.
		Mesh[] lcLive = {new Mesh(), new Mesh(), new Mesh()};
		if (detail && state.kind == EquipmentKind.SWITCHGEAR_LINEUP && state.lc != null) {
			Frame leg = new Frame(lcLive[2], true);
			leg.legends = true;
			LoadCentreParts.live(new Frame(lcLive[0], true), new Frame(lcLive[1], true), leg, state.lc);
		}
		collector.submitCustomGeometry(pose, RENDER_TYPE, (p, consumer) -> {
			if (state.crane != null) {
				// The turbine hall's crane (turbine hall part 3): its parts where its motions have them.
				CraneParts.draw(p, consumer, state.facing, state.lightLevel, state.crane, detail, state.time);
				return;
			}
			staticMesh(state.kind, detail).replay(p, consumer, state.facing, state.lightLevel);
			if (state.kind == EquipmentKind.REACTOR_VESSEL || state.kind == EquipmentKind.EPR_VESSEL) {
				boolean epr = state.kind == EquipmentKind.EPR_VESSEL;
				if (state.open) {
					// Head off: the refuelling cavity's water over the fuel, glowing blue.
					Mesh water = new Mesh();
					Frame f = new Frame(water, detail);
					if (epr) f.scale(EPR_SCALE);
					f.disc(v(3.5, 12.46, 3.5), v(0, 1, 0), 2.55, f.sides(24), WATER);
					water.replay(p, consumer, state.facing, LightCoordsUtil.FULL_BRIGHT);
				} else {
					headMesh(epr, detail).replay(p, consumer, state.facing, state.lightLevel);
					rodsMesh(epr, state.rods, detail).replay(p, consumer, state.facing, state.lightLevel);
				}
			} else if (state.kind == EquipmentKind.SWITCHGEAR_LINEUP && state.lc != null) {
				// The rebuilt load centre (older rooms round 1): its lit parts, its names by its label, and close up its
				// breakers' fronts, lamps, flags and handles, its pointers and fans (its plates in their own pass below).
				glowMesh(state.kind, detail).replay(p, consumer, state.facing, LightCoordsUtil.FULL_BRIGHT);
				if (detail) {
					Mesh[] named = LoadCentreParts.namedMesh(state.variant);
					if (named[0].vertices() > 0) named[0].replay(p, consumer, state.facing, state.lightLevel);
					if (named[1].vertices() > 0) named[1].replay(p, consumer, state.facing, LightCoordsUtil.FULL_BRIGHT);
					if (lcLive[0].vertices() > 0) lcLive[0].replay(p, consumer, state.facing, state.lightLevel);
					if (lcLive[1].vertices() > 0) lcLive[1].replay(p, consumer, state.facing, LightCoordsUtil.FULL_BRIGHT);
				}
			} else if (state.kind.board()) {
				// A load centre's or motor control centre's lit parts, and its breakers' lamps, flags and handles as they
				// stand.
				glowMesh(state.kind, detail).replay(p, consumer, state.facing, LightCoordsUtil.FULL_BRIGHT);
				if (detail) {
					Mesh[] live = loadCentreMesh(state.kind, state.breakers, state.variant);
					live[0].replay(p, consumer, state.facing, state.lightLevel);
					live[1].replay(p, consumer, state.facing, LightCoordsUtil.FULL_BRIGHT);
				}
			} else if (state.kind.hvac()) {
				// A control room HVAC machine (round 26, #11): its lit parts, its name by its train, and close up its gauges'
				// needles, its station's lamps and the chiller's display glass as it runs.
				glowMesh(state.kind, detail).replay(p, consumer, state.facing, LightCoordsUtil.FULL_BRIGHT);
				if (detail) {
					Mesh[] named = hvacStateMesh(state.kind, state.variant);
					named[0].replay(p, consumer, state.facing, state.lightLevel);
					named[1].replay(p, consumer, state.facing, LightCoordsUtil.FULL_BRIGHT);
					Mesh still = new Mesh(), lit = new Mesh();
					hvacLive(new Frame(still, true), new Frame(lit, true), state.kind, state.running, state.speed, state.time, state.seed);
					if (still.vertices() > 0) still.replay(p, consumer, state.facing, state.lightLevel);
					if (lit.vertices() > 0) lit.replay(p, consumer, state.facing, LightCoordsUtil.FULL_BRIGHT);
				}
			} else if (state.kind.liveBoard()) {
				// A live control board (round 27): its lit parts, and close up its lamps, pointers, bar graphs, figures,
				// traces and handles as the plant has them (its legends and windows are drawn in their own pass below).
				glowMesh(state.kind, detail).replay(p, consumer, state.facing, LightCoordsUtil.FULL_BRIGHT);
				// (The display wall's live parts are all on its own texture: drawn in its legend pass below.)
				if (detail && state.board != null && state.kind != EquipmentKind.PLANT_DISPLAY_WALL) {
					Mesh still = new Mesh(), lit = new Mesh();
					LiveBoards.draw(new Frame(still, true), new Frame(lit, true), state.board, state.kind, state.clock);
					if (still.vertices() > 0) still.replay(p, consumer, state.facing, state.lightLevel);
					if (lit.vertices() > 0) lit.replay(p, consumer, state.facing, LightCoordsUtil.FULL_BRIGHT);
				}
			} else if (state.kind.turbine()) {
				// The main turbine-generator and its reheaters (turbine hall part 1): a reheater's label by its anchor's variant,
				// and close up the turbine's figures, needles, lamps, valve stems, trip handle and guard, turning gear lever and
				// what turns (their plates and gauge faces are drawn in their own pass below).
				if (detail) {
					Mesh[] named = TurbineParts.namedMesh(state.kind, state.variant);
					if (named[0].vertices() > 0) named[0].replay(p, consumer, state.facing, state.lightLevel);
					if (named[1].vertices() > 0) named[1].replay(p, consumer, state.facing, LightCoordsUtil.FULL_BRIGHT);
					Mesh still = new Mesh(), lit = new Mesh();
					TurbineParts.live(new Frame(still, true), new Frame(lit, true), state.kind, state.turbine);
					if (still.vertices() > 0) still.replay(p, consumer, state.facing, state.lightLevel);
					if (lit.vertices() > 0) lit.replay(p, consumer, state.facing, LightCoordsUtil.FULL_BRIGHT);
				}
			} else if (state.kind.diesel()) {
				// The diesel generator set and its starting air (round 31): its label by its anchor's variant, and close up
				// what turns, its needles and its lamps (its plates and signs are drawn in their own pass below).
				if (detail) {
					Mesh[] named = DieselParts.namedMesh(state.kind, state.variant);
					if (named[0].vertices() > 0) named[0].replay(p, consumer, state.facing, state.lightLevel);
					if (named[1].vertices() > 0) named[1].replay(p, consumer, state.facing, LightCoordsUtil.FULL_BRIGHT);
					Mesh still = new Mesh(), lit = new Mesh();
					DieselParts.live(new Frame(still, true), new Frame(lit, true), state.kind,
							new DieselParts.Readings(state.running, state.angle, state.air, state.ready, state.joined, state.engine, state.hoistX,
									state.hoistDrop));
					if (still.vertices() > 0) still.replay(p, consumer, state.facing, state.lightLevel);
					if (lit.vertices() > 0) lit.replay(p, consumer, state.facing, LightCoordsUtil.FULL_BRIGHT);
				}
			} else if (state.kind.decor()) {
				// The lit parts (screens, lamps, the glow over spent fuel), and whatever blinks.
				glowMesh(state.kind, detail).replay(p, consumer, state.facing, LightCoordsUtil.FULL_BRIGHT);
				if (detail) {
					Mesh blink = new Mesh();
					PlantDecor.blink(state.kind, new Frame(blink, true), state.time);
					if (blink.vertices() > 0) blink.replay(p, consumer, state.facing, LightCoordsUtil.FULL_BRIGHT);
				}
			}
		});
		if (state.kind == EquipmentKind.SWITCHGEAR_LINEUP && detail && state.lc != null) {
			collector.submitCustomGeometry(pose, LoadCentreParts.LEGEND_TYPE, (p, consumer) -> {
				LoadCentreParts.legendMesh(state.variant).replay(p, consumer, state.facing, state.lightLevel);
				if (lcLive[2].vertices() > 0) lcLive[2].replay(p, consumer, state.facing, state.lightLevel);
			});
			loadCentreTexts(state, pose, collector);
		} else if (state.kind.board() && detail) {
			loadCentreMeters(state, pose, collector);
		}
		if (state.kind == EquipmentKind.CR_CHILLER && detail) {
			chillerScreen(state, pose, collector);
		}
		if (state.kind.diesel() && detail) {
			collector.submitCustomGeometry(pose, DieselParts.LEGEND_TYPE, (p, consumer) ->
					DieselParts.legendMesh(state.kind, state.variant).replay(p, consumer, state.facing, state.lightLevel));
		}
		if (state.kind.turbine() && detail) {
			collector.submitCustomGeometry(pose, TurbineParts.LEGEND_TYPE, (p, consumer) ->
					TurbineParts.legendMesh(state.kind, state.variant).replay(p, consumer, state.facing, state.lightLevel));
		}
		if (state.crane != null && detail) {
			collector.submitCustomGeometry(pose, TurbineParts.LEGEND_TYPE, (p, consumer) ->
					CraneParts.legends(p, consumer, state.facing, state.lightLevel, state.crane));
		}
		if (state.kind == EquipmentKind.PLANT_DISPLAY_WALL && detail) {
			// Round 32: the plant display wall, on its own texture, and the words it writes.
			collector.submitCustomGeometry(pose, LiveBoards.WALL_TYPE, (p, consumer) ->
					LiveBoards.wall(p, consumer, state.facing, state.lightLevel, state.kind, state.board, state.clock));
			if (state.board != null) {
				wallTexts(state, pose, collector);
			}
		} else if (state.kind.liveBoard() && detail) {
			collector.submitCustomGeometry(pose, LiveBoards.LEGEND_TYPE, (p, consumer) ->
					LiveBoards.legends(p, consumer, state.facing, state.lightLevel, state.kind, state.board, state.clock));
			if (state.board != null) {
				boardScreens(state, pose, collector);
			}
		}
	}

	/** A live board's plant computer screens (client/LiveBoards.java screen): their lines over the dark glass. */
	private void boardScreens(State s, PoseStack pose, SubmitNodeCollector collector) {
		com.google.gson.JsonObject live = ControlBoards.live(s.kind);
		if (live == null || !live.has("screens")) {
			return;
		}
		pose.pushPose();
		pose.translate(0.5F, 0.0F, 0.5F);
		pose.rotateDegrees(Axis.YP, -yRotation(s.facing));
		pose.translate(-0.5F, 0.0F, -0.5F);
		for (com.google.gson.JsonElement e : live.getAsJsonArray("screens")) {
			com.google.gson.JsonObject sc = e.getAsJsonObject();
			double x0 = sc.get("x0").getAsDouble(), y0 = sc.get("y0").getAsDouble(), x1 = sc.get("x1").getAsDouble(), y1 = sc.get("y1").getAsDouble();
			double z = sc.get("z").getAsDouble();
			java.util.List<LiveBoards.Line> lines = LiveBoards.screen(sc.get("id").getAsString(), s.board.plant, s.clock);
			double lineHeight = (y1 - y0 - 0.03) / 5.4, k = lineHeight / 10.0, left = x0 + 0.03, right = x1 - 0.03;
			for (int i = 0; i < lines.size(); i++) {
				LiveBoards.Line line = lines.get(i);
				double y = y1 - 0.025 - 7 * k - i * lineHeight;
				double width = (font.width(line.text()) + (line.value().isEmpty() ? 0 : font.width(line.value()) + 8)) * k;
				double kk = width > right - left ? k * (right - left) / width : k;
				text(pose, collector, line.text(), left, y, z, kk, line.colour());
				if (!line.value().isEmpty()) {
					text(pose, collector, line.value(), right - font.width(line.value()) * kk, y, z, kk, line.colour());
				}
			}
		}
		pose.popPose();
	}

	/**
	 * Round 32: the words the plant display wall writes (nuclear/DisplayWall.java text): each in the game's font, its
	 * capitals h high with their middle where tools/display_wall.py put it, shrunk to its widest; the banner's alarms
	 * (their board's name over their legend: light on red, dark on lit amber).
	 */
	private void wallTexts(State s, PoseStack pose, SubmitNodeCollector collector) {
		com.google.gson.JsonObject live = ControlBoards.live(s.kind);
		if (live == null || !live.has("texts")) {
			return;
		}
		net.minecraft.client.multiplayer.ClientLevel world = net.minecraft.client.Minecraft.getInstance().level;
		long day = world == null ? 6000 : world.getOverworldClockTime();
		pose.pushPose();
		pose.translate(0.5F, 0.0F, 0.5F);
		pose.rotateDegrees(Axis.YP, -yRotation(s.facing));
		pose.translate(-0.5F, 0.0F, -0.5F);
		for (com.google.gson.JsonElement e : live.getAsJsonArray("texts")) {
			com.google.gson.JsonObject t = e.getAsJsonObject();
			io.github.jimbozoomer.jugcraft.nuclear.DisplayWall.Text text = io.github.jimbozoomer.jugcraft.nuclear.DisplayWall.text(t.get("id").getAsString(),
					s.board.plant, s.board, s.clock, day);
			if (text.text().isEmpty()) {
				continue;
			}
			com.google.gson.JsonArray c = t.getAsJsonArray("c");
			int colour = text.colour() != 0 ? text.colour() : 0xFF000000 | c.get(0).getAsInt() << 16 | c.get(1).getAsInt() << 8 | c.get(2).getAsInt();
			wallText(pose, collector, text.text(), t.get("x").getAsDouble(), t.get("y").getAsDouble(), t.get("z").getAsDouble(), t.get("h").getAsDouble(),
					t.get("a").getAsString(), t.has("w") ? t.get("w").getAsDouble() : 0, colour);
		}
		boolean fast = ((long) (s.clock / 4)) % 2 == 0;
		for (com.google.gson.JsonElement e : live.getAsJsonArray("alarms")) {
			com.google.gson.JsonObject a = e.getAsJsonObject();
			com.google.gson.JsonArray slots = a.getAsJsonArray("slots");
			double z = a.get("tz").getAsDouble();
			for (int i = 0; i < Math.min(slots.size(), s.board.wallAlarms.length); i++) {
				com.google.gson.JsonArray q = slots.get(i).getAsJsonArray();
				int packed = s.board.wallAlarms[i];
				byte st = io.github.jimbozoomer.jugcraft.nuclear.DisplayWall.state(packed);
				boolean lit = st == io.github.jimbozoomer.jugcraft.nuclear.ControlBoard.ACKED || st == io.github.jimbozoomer.jugcraft.nuclear.ControlBoard.ALERT && fast;
				boolean amber = LiveBoards.wallColour(packed).equals("amber");
				int ink = lit && amber ? io.github.jimbozoomer.jugcraft.nuclear.DisplayWall.INK : 0xFFF4F4F2;
				double x0 = q.get(0).getAsDouble(), x1 = q.get(2).getAsDouble(), y1 = q.get(3).getAsDouble(), w = x1 - x0 - 0.08;
				wallText(pose, collector, io.github.jimbozoomer.jugcraft.nuclear.DisplayWall.boardName(io.github.jimbozoomer.jugcraft.nuclear.DisplayWall.kind(packed)),
						x0 + 0.04, y1 - 0.05, z, 0.036, "l", w, ink);
				java.util.List<String> lines = LiveBoards.wallLegend(packed);
				for (int k = 0; k < lines.size(); k++) {
					wallText(pose, collector, lines.get(k), x0 + 0.04, y1 - 0.13 - k * 0.08, z, 0.052, "l", w, ink);
				}
			}
		}
		pose.popPose();
	}

	/** A line of the wall's words: capitals h high, their middle at y, anchored l, c or r at x, no wider than widest. */
	private void wallText(PoseStack pose, SubmitNodeCollector collector, String s, double x, double y, double z, double h, String anchor, double widest, int colour) {
		double k = h / 7.0;
		double width = font.width(s) * k;
		if (widest > 0 && width > widest) {
			k *= widest / width;
			width = widest;
		}
		double left = anchor.equals("l") ? x : anchor.equals("c") ? x - width / 2 : x - width;
		text(pose, collector, s, left, y - 3.5 * k, z, k, colour);
	}

	// ------------------------------------------------------------------ the control room HVAC machines (nuclear/HvacRoom.java)

	/** How long a machine takes to come up to speed or run down (game ticks). */
	static final float HVAC_RAMP = 60;
	/** Each machine's name by its train (still, lit), by kind and variant. */
	private static final java.util.Map<Integer, Mesh[]> HVAC_NAMED = new java.util.HashMap<>();

	static Mesh[] hvacStateMesh(EquipmentKind kind, int variant) {
		return HVAC_NAMED.computeIfAbsent(kind.ordinal() << 4 | variant, k -> {
			Mesh still = new Mesh(), lit = new Mesh();
			ControlBoards.state(kind, "v" + Math.min(1, variant), new Frame(still, true), false);
			ControlBoards.state(kind, "v" + Math.min(1, variant), new Frame(lit, true), true);
			return new Mesh[] {still, lit};
		});
	}

	/**
	 * A gauge's reading (a fraction of its dial) stopped and running (tools/hvac_boards.py: the filters' differential
	 * pressures on 0 to 4 inches of water, nothing across them without flow; chilled water pressures on 0 to 100 psig,
	 * the loop's static 30 psig from its expansion tank when its pump is stopped).
	 */
	static double[] hvacGauge(String name) {
		return switch (name) {
			case "PREFLTR DP" -> new double[] {0, 0.55 / 4};
			case "HEPA DP" -> new double[] {0, 1.4 / 4};
			case "CHAR DP" -> new double[] {0, 1.1 / 4};
			case "POST DP" -> new double[] {0, 0.9 / 4};
			case "FILTER DP" -> new double[] {0, 0.7 / 4};
			case "CHWS P" -> new double[] {0.30, 0.62};
			case "CHWR P" -> new double[] {0.30, 0.48};
			case "EVAP IN" -> new double[] {0.30, 0.45};
			case "EVAP OUT" -> new double[] {0.30, 0.40};
			case "SUCTION" -> new double[] {0.30, 0.22};
			case "DISCH" -> new double[] {0.30, 0.68};
			default -> new double[] {0, 0.5};
		};
	}

	/**
	 * A machine's live parts (where they are: tools/hvac_boards.py "live"): each gauge's needle at its reading (swept
	 * from 225 degrees round to -45 across its dial, wandering a little while it runs), its local station's RUNNING
	 * (red) and STOPPED (green) lamps, the chiller's display glass (its readings drawn over it).
	 */
	static void hvacLive(Frame still, Frame lit, EquipmentKind kind, boolean running, float ramp, float time, int seed) {
		com.google.gson.JsonObject live = ControlBoards.live(kind);
		if (live == null || !live.has("mats")) {
			return;
		}
		com.google.gson.JsonObject mats = live.getAsJsonObject("mats");
		com.google.gson.JsonElement needle = mats.get("needle");
		if (live.has("gauges")) {
			int i = 0;
			for (com.google.gson.JsonElement e : live.getAsJsonArray("gauges")) {
				com.google.gson.JsonObject g = e.getAsJsonObject();
				double[] read = hvacGauge(g.get("name").getAsString());
				double f = read[0] + (read[1] - read[0]) * ramp;
				if (ramp > 0) {
					f += ramp * (0.006 * Math.sin(time * 0.05 + i * 1.7 + seed % 97) + 0.003 * Math.sin(time * 0.23 + i * 2.3));
				}
				f = Math.max(-0.02, Math.min(1.02, f));
				double a = Math.toRadians(225 - 270 * f);
				com.google.gson.JsonArray c = g.getAsJsonArray("c");
				double side = g.get("side").getAsDouble(), len = g.get("len").getAsDouble();
				Vec3 centre = v(c.get(0).getAsDouble(), c.get(1).getAsDouble(), c.get(2).getAsDouble());
				Vec3 d = v(0, Math.sin(a), side * Math.cos(a));
				Vec3 across = v(side, 0, 0);
				ControlBoards.oriented(still, centre.add(d.scale(len * 0.4)).add(side * 0.006, 0, 0), d, across, len * 0.6, 0.004, 0.006, needle);
				ControlBoards.oriented(still, centre.add(side * 0.008, 0, 0), v(0, 1, 0), across, 0.01, 0.004, 0.01, needle);
				i++;
			}
		}
		if (live.has("stations")) {
			for (com.google.gson.JsonElement e : live.getAsJsonArray("stations")) {
				com.google.gson.JsonObject st = e.getAsJsonObject();
				double side = st.get("side").getAsDouble(), r = st.get("r").getAsDouble();
				com.google.gson.JsonArray lamps = st.getAsJsonArray("lamps");
				for (int k = 0; k < 2 && k < lamps.size(); k++) {
					com.google.gson.JsonArray at = lamps.get(k).getAsJsonArray();
					boolean on = k == 0 ? running : !running;
					String mat = k == 0 ? (on ? "red" : "off_red") : (on ? "green" : "off_green");
					double x = at.get(0).getAsDouble(), y = at.get(1).getAsDouble(), z = at.get(2).getAsDouble();
					ControlBoards.box(on ? lit : still, Math.min(x, x + side * 0.006), y - r, z - r, Math.max(x, x + side * 0.006), y + r, z + r, mats.get(mat));
				}
			}
		}
		if (live.has("display")) {
			com.google.gson.JsonObject d = live.getAsJsonObject("display");
			double x = d.get("x").getAsDouble(), z0 = d.get("z0").getAsDouble(), z1 = d.get("z1").getAsDouble();
			double y0 = d.get("y0").getAsDouble(), y1 = d.get("y1").getAsDouble();
			ControlBoards.face(lit, v(x, y1, z1), v(x, y0, z1), v(x, y0, z0), v(x, y1, z0), mats.get("glass"));
		}
	}

	/**
	 * The chiller's display (on its control panel, on its left side), in green, as the approved mockup has it: its tag,
	 * state and load; leaving and entering chilled water (deg F); evaporator and condenser pressures (psig); motor amps
	 * and oil pressure (standing by: ready to start). Each comes up or down with the machine and wanders a little.
	 */
	private void chillerScreen(State s, PoseStack pose, SubmitNodeCollector collector) {
		com.google.gson.JsonObject live = ControlBoards.live(s.kind);
		if (live == null || !live.has("display")) {
			return;
		}
		com.google.gson.JsonObject d = live.getAsJsonObject("display");
		double x = d.get("x").getAsDouble() - 0.004, z0 = d.get("z0").getAsDouble(), z1 = d.get("z1").getAsDouble();
		double y0 = d.get("y0").getAsDouble(), y1 = d.get("y1").getAsDouble();
		float f = s.speed;
		double w = Math.sin(s.time * 0.031 + s.seed % 89) * 0.5 + Math.sin(s.time * 0.113) * 0.3;
		String tag = "CH-1" + (s.variant == 1 ? "B" : "A");
		String state = s.running ? (f >= 1 ? "RUNNING" : "STARTING") : f > 0 ? "STOPPING" : "STANDBY";
		java.util.Locale r = java.util.Locale.ROOT;
		String[] lines = {
				f > 0 ? String.format(r, "%s  %s  %.0f%%", tag, state, 62 * f + w * f) : tag + "  STANDBY  AUTO",
				String.format(r, "LCHWT %.1fF  ECHWT %.1fF", 55.2 - 11.1 * f + 0.15 * w * f, 55.4 - 1.4 * f + 0.1 * w * f),
				String.format(r, "EVAP %.0f PSIG  COND %.0f", 62 - 24 * f + w * f, 70 + 58 * f + 1.5 * w * f),
				f > 0 ? String.format(r, "AMPS %.0f  OIL %.0f PSID", 71 * f + 1.5 * w * f, 52 * f + 0.5 * w * f) : "READY TO START"};
		int green = 0xFF78FF96, amber = 0xFFF0B030;
		double margin = 0.03, lineHeight = (y1 - y0 - 2 * margin) / lines.length;
		int widest = 0;
		for (String line : lines) {
			widest = Math.max(widest, font.width(line));
		}
		// As large as the lines fit: across the glass, and a line's 7-pixel letters with a gap.
		double k = Math.min(lineHeight / 10.0, (z1 - z0 - 2 * margin) / Math.max(1, widest));
		pose.pushPose();
		pose.translate(0.5F, 0.0F, 0.5F);
		pose.rotateDegrees(Axis.YP, -yRotation(s.facing));
		pose.translate(-0.5F, 0.0F, -0.5F);
		for (int i = 0; i < lines.length; i++) {
			double y = y1 - margin - (i + 1) * lineHeight + (lineHeight - 7 * k) / 2;
			int colour = i == 0 && (state.equals("STARTING") || state.equals("STOPPING")) ? amber : green;
			sideText(pose, collector, lines[i], x, y, z1 - margin, k, colour);
		}
		pose.popPose();
	}

	/**
	 * Text on a face on the equipment's left side (facing out to its left, at x in its frame), its baseline at y, starting
	 * at z and running toward the front (smaller z: to the right of whoever stands there).
	 */
	private void sideText(PoseStack pose, SubmitNodeCollector collector, String s, double x, double y, double z, double k, int colour) {
		pose.pushPose();
		pose.translate((float) x, (float) y, (float) (1 - z));
		pose.rotateDegrees(Axis.YP, -90);
		pose.scale((float) k, (float) -k, (float) k);
		collector.submitText(pose, 0, -7, Texts.of(s), false, Font.DisplayMode.POLYGON_OFFSET,
				LightCoordsUtil.FULL_BRIGHT, colour, 0, 0);
		pose.popPose();
	}

	// ------------------------------------------------------------------ the 480 V load centre (nuclear/Switchgear.java)

	/** The load centre's breaker lamps and flags by its breakers (still, lit), and its meters' dark glass. */
	private static final java.util.Map<Integer, Mesh[]> LOAD_CENTRE = new java.util.HashMap<>();

	static Mesh[] loadCentreMesh(EquipmentKind kind, int breakers, int variant) {
		return LOAD_CENTRE.computeIfAbsent(kind.ordinal() << 18 | variant << 14 | breakers, b -> {
			Mesh still = new Mesh(), lit = new Mesh();
			loadCentreLive(new Frame(still, true), new Frame(lit, true), kind, breakers);
			// Its name by its variant (a motor control centre's MCC 1A or 1B), where the board draws one.
			ControlBoards.state(kind, "v" + variant, new Frame(still, true), false);
			ControlBoards.state(kind, "v" + variant, new Frame(lit, true), true);
			return new Mesh[] {still, lit};
		});
	}

	/**
	 * A load centre's or motor control centre's live parts (where they are: tools/control_boards.py load_center's and
	 * motor_control_center's "live"): over each breaker its
	 * red lamp lit while it is closed, its green lamp while it is open (the other lens unlit); in its window the
	 * breaker's flag, red for closed, green for open; the meters' dark glass, their readings drawn over it.
	 */
	static void loadCentreLive(Frame still, Frame lit, EquipmentKind kind, int breakers) {
		com.google.gson.JsonObject live = ControlBoards.live(kind);
		if (live == null) {
			return;
		}
		Vec3 n = v(0, 0, -1);
		com.google.gson.JsonArray list = live.getAsJsonArray("breakers");
		for (int i = 0; i < list.size() && i < 31; i++) {
			com.google.gson.JsonObject b = list.get(i).getAsJsonObject();
			boolean closed = (breakers & 1 << i) != 0;
			double h = b.get("lamp").getAsDouble() / 2 + 0.005, z = b.get("lamp_z").getAsDouble();
			com.google.gson.JsonArray lamps = b.getAsJsonArray("lamps");
			for (int k = 0; k < 2; k++) {
				// The left lamp green (open), the right red (closed).
				boolean on = k == 0 ? !closed : closed;
				float[] uv = on ? (k == 0 ? LAMP_GREEN : LAMP_RED) : (k == 0 ? swatch(3, 0) : swatch(0, 0));
				double x = lamps.get(k).getAsJsonArray().get(0).getAsDouble(), y = lamps.get(k).getAsJsonArray().get(1).getAsDouble();
				(on ? lit : still).quad(v(x - h, y + h, z), v(x - h, y - h, z), v(x + h, y - h, z), v(x + h, y + h, z), n, uv[0], uv[1], uv[2], uv[3]);
			}
			if (b.has("flag")) {
				com.google.gson.JsonArray fl = b.getAsJsonArray("flag");
				double x0 = fl.get(0).getAsDouble(), y0 = fl.get(1).getAsDouble(), x1 = fl.get(2).getAsDouble(), y1 = fl.get(3).getAsDouble();
				double fz = b.get("flag_z").getAsDouble();
				float[] uv = closed ? swatch(0, 0) : swatch(3, 0);
				still.quad(v(x0, y1, fz), v(x0, y0, fz), v(x1, y0, fz), v(x1, y1, fz), n, uv[0], uv[1], uv[2], uv[3]);
			}
			// A motor control centre's operating handle: up while its breaker is on, down while off.
			if (b.has("on")) {
				ControlBoards.ops(still, b.getAsJsonArray(closed ? "on" : "off"));
			}
		}
		float[] glass = swatch(0, 2);
		for (com.google.gson.JsonElement e : live.getAsJsonArray("meters")) {
			com.google.gson.JsonObject m = e.getAsJsonObject();
			com.google.gson.JsonArray box = m.getAsJsonArray("box");
			double x0 = box.get(0).getAsDouble(), y0 = box.get(1).getAsDouble(), x1 = box.get(2).getAsDouble(), y1 = box.get(3).getAsDouble();
			double z = m.get("z").getAsDouble();
			lit.quad(v(x0, y1, z), v(x0, y0, z), v(x1, y0, z), v(x1, y1, z), n, glass[0], glass[1], glass[2], glass[3]);
		}
	}

	/**
	 * The rebuilt load centre's words (tools/load_centre.py live "texts"): each trip unit's amps and operations count (on
	 * its breaker's front, out with it), the power monitor's lines, the transformer's temperatures; in the game's font,
	 * capitals h high with their middle at y, shrunk to fit their width.
	 */
	private void loadCentreTexts(State s, PoseStack pose, SubmitNodeCollector collector) {
		com.google.gson.JsonObject live = ControlBoards.live(s.kind);
		if (live == null || s.lc == null || !live.has("texts")) {
			return;
		}
		pose.pushPose();
		pose.translate(0.5F, 0.0F, 0.5F);
		pose.rotateDegrees(Axis.YP, -yRotation(s.facing));
		pose.translate(-0.5F, 0.0F, -0.5F);
		for (com.google.gson.JsonElement e : live.getAsJsonArray("texts")) {
			com.google.gson.JsonObject t = e.getAsJsonObject();
			String text = s.lc.text(t.get("id").getAsString());
			if (text.isEmpty()) {
				continue;
			}
			double h = t.get("h").getAsDouble(), k = h / 7.0;
			double width = font.width(text) * k;
			if (t.has("w") && width > t.get("w").getAsDouble()) {
				k *= t.get("w").getAsDouble() / width;
				width = t.get("w").getAsDouble();
			}
			double x = t.get("x").getAsDouble();
			String a = t.get("a").getAsString();
			double x0 = a.equals("r") ? x - width : a.equals("c") ? x - width / 2 : x;
			com.google.gson.JsonArray c = t.getAsJsonArray("c");
			int colour = 0xFF000000 | c.get(0).getAsInt() << 16 | c.get(1).getAsInt() << 8 | c.get(2).getAsInt();
			text(pose, collector, text, x0, t.get("y").getAsDouble() - 3.5 * k, t.get("z").getAsDouble() + s.lc.dz(t), k, colour);
		}
		pose.popPose();
	}

	/**
	 * The load centre's meters' readings in red LED figures: the bus's charge (%), its volts (480 while it holds any
	 * charge), each feeder's amps (its flow in JE/t over four).
	 */
	private void loadCentreMeters(State s, PoseStack pose, SubmitNodeCollector collector) {
		com.google.gson.JsonObject live = ControlBoards.live(s.kind);
		if (live == null) {
			return;
		}
		int red = 0xFFFF3A26;
		pose.pushPose();
		pose.translate(0.5F, 0.0F, 0.5F);
		pose.rotateDegrees(Axis.YP, -yRotation(s.facing));
		pose.translate(-0.5F, 0.0F, -0.5F);
		int amps = 0;
		for (com.google.gson.JsonElement e : live.getAsJsonArray("meters")) {
			com.google.gson.JsonObject m = e.getAsJsonObject();
			com.google.gson.JsonArray box = m.getAsJsonArray("box");
			double x0 = box.get(0).getAsDouble(), y0 = box.get(1).getAsDouble(), x1 = box.get(2).getAsDouble(), y1 = box.get(3).getAsDouble();
			String value = switch (m.get("kind").getAsString()) {
				case "charge" -> Math.round(s.charge * 100) + "%";
				case "volts" -> s.charge > 0.0005F ? "480" : "0";
				default -> {
					amps++;
					yield String.valueOf(amps < Switchgear.BREAKERS ? s.flows[amps] / 4 : 0);
				}
			};
			double k = (y1 - y0) * 0.72 / 7;
			double y = (y0 + y1) / 2 - 3.5 * k;
			text(pose, collector, value, x1 - 0.008 - font.width(value) * k, y, m.get("z").getAsDouble(), k, red);
		}
		pose.popPose();
	}

	// ------------------------------------------------------------------ swatches

	/** A patch of the swatch cell (tools/control_boards.py SWATCH: column, row). */
	static float[] swatch(int col, int row) {
		float[] c = SWATCHES;
		float du = (c[2] - c[0]) / 4, dv = (c[3] - c[1]) / 4;
		return new float[] {c[0] + du * (col + 0.1F), c[1] + dv * (row + 0.1F), c[0] + du * (col + 0.9F), c[1] + dv * (row + 0.9F)};
	}

	/** Text on a face at depth z of the equipment's frame, its baseline at y (in the frame turned as the equipment is). */
	private void text(PoseStack pose, SubmitNodeCollector collector, String s, double x, double y, double z, double k, int colour) {
		pose.pushPose();
		// The face is at z in the equipment's frame (z away from whoever stands at it): 1 - z here.
		pose.translate((float) x, (float) y, (float) (1 - z + 0.003));
		pose.scale((float) k, (float) -k, (float) k);
		collector.submitText(pose, 0, -7, Texts.of(s), false, Font.DisplayMode.POLYGON_OFFSET,
				LightCoordsUtil.FULL_BRIGHT, colour, 0, 0);
		pose.popPose();
	}

	private static float yRotation(Direction facing) {
		return switch (facing) {
			case EAST -> 90;
			case SOUTH -> 180;
			case WEST -> 270;
			default -> 0;
		};
	}

	// ------------------------------------------------------------------ the item

	/**
	 * The equipment's item texture: the material sheet with every region one flat colour (its average), drawn by
	 * tools/structure_items.py. Shrunk to a slot, a block-sized texture tile is smaller than a pixel and would sparkle.
	 */
	public static final Identifier ITEM_TEXTURE = Jugcraft.id("textures/entity/nuclear_equipment_item.png");
	/** The item's moving and lit parts as they stand still (vessel closed, rods in, lamps steady), by kind. */
	private static final Mesh[] ITEM_STILL = new Mesh[EquipmentKind.values().length];
	private static final Mesh[] ITEM_LIT = new Mesh[EquipmentKind.values().length];

	/**
	 * The equipment as its item shows it (client/StructureItemRenderers.java), in full detail: the vessel closed with
	 * its rods in, the console's screens and lamps, a piece of plant decor with its lit parts and what moves on it
	 * standing still. Turned so its front, the side that faces whoever placed it, faces north: a block item's right side
	 * in the slot (minecraft:block/block's gui rotation, 30 and 225 degrees).
	 */
	static void drawItem(EquipmentKind kind, PoseStack.Pose pose, com.mojang.blaze3d.vertex.VertexConsumer consumer, int light) {
		drawStill(kind, Direction.SOUTH, pose, consumer, light);
	}

	/**
	 * The equipment where its item would place it (client/MbsPreview.java, the placement preview): as its item shows
	 * it, facing {@code facing} from its anchor block as placed equipment does, lit evenly.
	 */
	static void drawGhost(EquipmentKind kind, Direction facing, PoseStack.Pose pose, com.mojang.blaze3d.vertex.VertexConsumer consumer) {
		drawStill(kind, facing, pose, consumer, LightCoordsUtil.FULL_BRIGHT);
	}

	/** The equipment standing still (vessel closed, rods in, lamps steady), facing {@code turned}. */
	private static void drawStill(EquipmentKind kind, Direction turned, PoseStack.Pose pose, com.mojang.blaze3d.vertex.VertexConsumer consumer, int light) {
		staticMesh(kind, true).replay(pose, consumer, turned, light);
		int i = kind.ordinal();
		if (ITEM_STILL[i] == null) {
			Mesh still = new Mesh(), lit = new Mesh();
			if (kind == EquipmentKind.REACTOR_VESSEL || kind == EquipmentKind.EPR_VESSEL) {
				boolean epr = kind == EquipmentKind.EPR_VESSEL;
				Frame head = new Frame(still, true), rods = new Frame(still, true);
				if (epr) {
					head.scale(EPR_SCALE);
					rods.scale(EPR_SCALE);
				}
				vesselHead(head);
				vesselRods(rods, 100);
			} else if (kind == EquipmentKind.SWITCHGEAR_LINEUP) {
				// (Its plates are on its own texture: the item shows its body, breakers and lamps.)
				ControlBoards.lit(kind, new Frame(lit, true));
				Frame leg = new Frame(new Mesh(), true);
				leg.legends = true;
				LoadCentreParts.live(new Frame(still, true), new Frame(lit, true), leg,
						new LoadCentreParts.View(Switchgear.DEFAULT_BREAKERS, new io.github.jimbozoomer.jugcraft.nuclear.LoadCentre.State(), 1F, 0, 15));
				ControlBoards.state(kind, "v0", new Frame(still, true), false);
			} else if (kind.board()) {
				ControlBoards.lit(kind, new Frame(lit, true));
				loadCentreLive(new Frame(still, true), new Frame(lit, true), kind,
						kind == EquipmentKind.MCC_LINEUP ? io.github.jimbozoomer.jugcraft.nuclear.MotorControlCenter.DEFAULT_BREAKERS : Switchgear.DEFAULT_BREAKERS);
				ControlBoards.state(kind, "v0", new Frame(still, true), false);
			} else if (kind.hvac()) {
				ControlBoards.lit(kind, new Frame(lit, true));
				ControlBoards.state(kind, "v0", new Frame(still, true), false);
				ControlBoards.state(kind, "v0", new Frame(lit, true), true);
				hvacLive(new Frame(still, true), new Frame(lit, true), kind, false, 0, 0, 0);
			} else if (kind.liveBoard()) {
				ControlBoards.lit(kind, new Frame(lit, true));
			} else if (kind.turbine()) {
				ControlBoards.state(kind, "v0", new Frame(still, true), false);
				TurbineParts.live(new Frame(still, true), new Frame(lit, true), kind, TurbineParts.Readings.STILL);
			} else if (kind.diesel()) {
				ControlBoards.state(kind, "v0", new Frame(still, true), false);
				DieselParts.live(new Frame(still, true), new Frame(lit, true), kind, DieselParts.Readings.STILL);
			} else if (kind.decor()) {
				PlantDecor.glow(kind, new Frame(lit, true));
				PlantDecor.blink(kind, new Frame(lit, true), 0);
			}
			ITEM_LIT[i] = lit;
			ITEM_STILL[i] = still;
		}
		if (ITEM_STILL[i].vertices() > 0) ITEM_STILL[i].replay(pose, consumer, turned, light);
		if (ITEM_LIT[i].vertices() > 0) ITEM_LIT[i].replay(pose, consumer, turned, LightCoordsUtil.FULL_BRIGHT);
	}

	// ------------------------------------------------------------------ the equipment's frame and tiled primitives

	/**
	 * Recorded geometry of one piece in its own frame (x right, y up, z away, in blocks from the anchor's corner):
	 * eight floats a vertex. It is built once per kind and detail level and replayed every frame, turned to the
	 * equipment's facing, so drawing a plant costs no trigonometry.
	 */
	static final class Mesh {
		private float[] data = new float[8 * 1024];
		private int size;

		void vertex(float x, float y, float z, float u, float v, float nx, float ny, float nz) {
			if (size + 8 > data.length) {
				data = java.util.Arrays.copyOf(data, data.length * 2);
			}
			data[size++] = x;
			data[size++] = y;
			data[size++] = z;
			data[size++] = u;
			data[size++] = v;
			data[size++] = nx;
			data[size++] = ny;
			data[size++] = nz;
		}

		int vertices() {
			return size / 8;
		}

		/** Emits the mesh for a piece facing {@code facing}, lit with {@code light}. */
		void replay(PoseStack.Pose pose, com.mojang.blaze3d.vertex.VertexConsumer consumer, Direction facing, int light) {
			replay(pose, consumer, facing, light, 0xFFFFFFFF);
		}

		/**
		 * The same, shifted (dx, dy, dz blocks in the piece's own frame) before it is turned: a part that moves on the piece
		 * (turbine hall part 3: the crane's bridge, trolley and hooks, client/CraneParts.java).
		 */
		void replay(PoseStack.Pose pose, com.mojang.blaze3d.vertex.VertexConsumer consumer, Direction facing, int light, float dx, float dy, float dz) {
			Direction right = facing.getClockWise();
			float rx = right.getStepX(), rz = right.getStepZ(), fx = facing.getStepX(), fz = facing.getStepZ();
			for (int i = 0; i < size; i += 8) {
				float lx = data[i] + dx - 0.5F, lz = data[i + 2] + dz - 0.5F;
				float x = 0.5F + rx * lx + fx * lz, z = 0.5F + rz * lx + fz * lz;
				float nx = rx * data[i + 5] + fx * data[i + 7], nz = rz * data[i + 5] + fz * data[i + 7];
				consumer.addVertex(pose, x, data[i + 1] + dy, z).setColor(0xFFFFFFFF).setUv(data[i + 3], data[i + 4])
						.setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, nx, data[i + 6], nz);
			}
		}

		/** The same, coloured (a live board's annunciator windows: a lamp's colour behind the legend, or frosted unlit). */
		void replay(PoseStack.Pose pose, com.mojang.blaze3d.vertex.VertexConsumer consumer, Direction facing, int light, int colour) {
			Direction right = facing.getClockWise();
			float rx = right.getStepX(), rz = right.getStepZ(), fx = facing.getStepX(), fz = facing.getStepZ();
			for (int i = 0; i < size; i += 8) {
				float lx = data[i] - 0.5F, lz = data[i + 2] - 0.5F;
				float x = 0.5F + rx * lx + fx * lz, z = 0.5F + rz * lx + fz * lz;
				float nx = rx * data[i + 5] + fx * data[i + 7], nz = rz * data[i + 5] + fz * data[i + 7];
				consumer.addVertex(pose, x, data[i + 1], z).setColor(colour).setUv(data[i + 3], data[i + 4])
						.setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, nx, data[i + 6], nz);
			}
		}
	}

	/** Builds a piece's geometry into a {@link Mesh}. {@code detail} is false for the far-away version. */
	static final class Frame {
		final Mesh mesh;
		final boolean detail;
		/** It draws the live boards' legends (their own texture) rather than the sheet's materials (client/ControlBoards.java). */
		boolean legends;
		/** Stretch applied to everything recorded (a larger version of the same piece). */
		double sx = 1, sy = 1, sz = 1;
		/** A shift applied to everything recorded, before the stretch (a load centre's breaker racked out). */
		double ox, oy, oz;

		Frame(Mesh mesh, boolean detail) {
			this.mesh = mesh;
			this.detail = detail;
		}

		void scale(double[] s) {
			sx = s[0];
			sy = s[1];
			sz = s[2];
		}

		/** Sides for a round shape: fewer when far away. */
		int sides(int full) {
			return detail ? full : Math.max(6, full / 2);
		}

		/**
		 * A quad given top-left, bottom-left, bottom-right, top-right as seen from outside, in local coordinates. (The
		 * local frame, x right and z away, keeps what a viewer sees where it is: front faces stay front faces.)
		 */
		void quad(Vec3 tl, Vec3 bl, Vec3 br, Vec3 tr, Vec3 n, float u0, float v0, float u1, float v1) {
			float nx = (float) n.x, ny = (float) n.y, nz = (float) n.z;
			float ax = (float) sx, ay = (float) sy, az = (float) sz;
			float px = (float) ox, py = (float) oy, pz = (float) oz;
			mesh.vertex(((float) tl.x + px) * ax, ((float) tl.y + py) * ay, ((float) tl.z + pz) * az, u0, v0, nx, ny, nz);
			mesh.vertex(((float) bl.x + px) * ax, ((float) bl.y + py) * ay, ((float) bl.z + pz) * az, u0, v1, nx, ny, nz);
			mesh.vertex(((float) br.x + px) * ax, ((float) br.y + py) * ay, ((float) br.z + pz) * az, u1, v1, nx, ny, nz);
			mesh.vertex(((float) tr.x + px) * ax, ((float) tr.y + py) * ay, ((float) tr.z + pz) * az, u1, v0, nx, ny, nz);
		}

		/** A triangle (a quad with its last corner repeated), for the sides under sloped desk tops. */
		void tri(Vec3 a, Vec3 b, Vec3 c, Vec3 n, float[] uv) {
			quad(a, b, c, c, n, uv[0], uv[1], uv[2], uv[3]);
		}

		/** A quad tiled about every {@code tile} blocks across and down, so its texture keeps its size. */
		void tiled(Vec3 tl, Vec3 bl, Vec3 br, Vec3 tr, Vec3 n, float[] uv, double tile) {
			double width = tl.distanceTo(tr), height = tl.distanceTo(bl);
			int nu = Math.max(1, (int) Math.ceil(width / tile - 0.01)), nv = Math.max(1, (int) Math.ceil(height / tile - 0.01));
			double du = width / tile, dv = height / tile;
			for (int i = 0; i < nu; i++) {
				double a0 = i / (double) nu, a1 = (i + 1) / (double) nu;
				// Each tile shows as much of the texture as it is wide (a block per texture), so nothing stretches.
				float tu0 = uv[0], tu1 = (float) (uv[0] + (uv[2] - uv[0]) * Math.min(1, du / nu));
				for (int j = 0; j < nv; j++) {
					double b0 = j / (double) nv, b1 = (j + 1) / (double) nv;
					float tv0 = uv[1], tv1 = (float) (uv[1] + (uv[3] - uv[1]) * Math.min(1, dv / nv));
					Vec3 ptl = lerp(tl, tr, bl, br, a0, b0), pbl = lerp(tl, tr, bl, br, a0, b1);
					Vec3 pbr = lerp(tl, tr, bl, br, a1, b1), ptr = lerp(tl, tr, bl, br, a1, b0);
					quad(ptl, pbl, pbr, ptr, n, tu0, tv0, tu1, tv1);
				}
			}
		}

		private static Vec3 lerp(Vec3 tl, Vec3 tr, Vec3 bl, Vec3 br, double a, double b) {
			Vec3 top = tl.add(tr.subtract(tl).scale(a));
			Vec3 bottom = bl.add(br.subtract(bl).scale(a));
			return top.add(bottom.subtract(top).scale(b));
		}

		/** A box with its sides, top and bottom materials, tiled. */
		void box(double x0, double y0, double z0, double x1, double y1, double z1, float[] sides, float[] top, float[] bottom) {
			double t = 1;
			// Front (z0, seen from -z): left is x0.
			tiled(v(x0, y1, z0), v(x0, y0, z0), v(x1, y0, z0), v(x1, y1, z0), v(0, 0, -1), sides, t);
			tiled(v(x1, y1, z1), v(x1, y0, z1), v(x0, y0, z1), v(x0, y1, z1), v(0, 0, 1), sides, t);
			tiled(v(x0, y1, z1), v(x0, y0, z1), v(x0, y0, z0), v(x0, y1, z0), v(-1, 0, 0), sides, t);
			tiled(v(x1, y1, z0), v(x1, y0, z0), v(x1, y0, z1), v(x1, y1, z1), v(1, 0, 0), sides, t);
			tiled(v(x0, y1, z1), v(x0, y1, z0), v(x1, y1, z0), v(x1, y1, z1), v(0, 1, 0), top, t);
			tiled(v(x0, y0, z0), v(x0, y0, z1), v(x1, y0, z1), v(x1, y0, z0), v(0, -1, 0), bottom, t);
		}

		void box(double x0, double y0, double z0, double x1, double y1, double z1, float[] uv) {
			box(x0, y0, z0, x1, y1, z1, uv, uv, uv);
		}

		/** A flat plate facing -z (toward the placer) or another way, with its picture on that face only. */
		void plate(double x0, double y0, double z0, double x1, double y1, double z1, int facingSide, float[] picture, float[] edge) {
			if (!detail) {
				return;
			}
			box(x0, y0, z0, x1, y1, z1, edge);
			// The picture stands a little off the face (enough to never z-fight with it at any distance it is drawn).
			double e = 0.004;
			switch (facingSide) {
				case 0 -> quad(v(x0, y1, z0 - e), v(x0, y0, z0 - e), v(x1, y0, z0 - e), v(x1, y1, z0 - e), v(0, 0, -1),
						picture[0], picture[1], picture[2], picture[3]);
				case 1 -> quad(v(x1, y1, z1 + e), v(x1, y0, z1 + e), v(x0, y0, z1 + e), v(x0, y1, z1 + e), v(0, 0, 1),
						picture[0], picture[1], picture[2], picture[3]);
				case 2 -> quad(v(x0 - e, y1, z1), v(x0 - e, y0, z1), v(x0 - e, y0, z0), v(x0 - e, y1, z0), v(-1, 0, 0),
						picture[0], picture[1], picture[2], picture[3]);
				default -> quad(v(x1 + e, y1, z0), v(x1 + e, y0, z0), v(x1 + e, y0, z1), v(x1 + e, y1, z1), v(1, 0, 0),
						picture[0], picture[1], picture[2], picture[3]);
			}
		}

		/**
		 * A shape turned on a lathe along the axis from {@code a} toward {@code b}: profile = {distance along, radius}.
		 * Texture wraps round about a block per tile and runs a block per tile along the axis.
		 */
		void revolve(Vec3 a, Vec3 b, double[][] profile, int sides, float[] uv, boolean capStart, boolean capEnd) {
			Vec3 axis = b.subtract(a).normalize();
			Vec3 ref = Math.abs(axis.y) > 0.9 ? new Vec3(0, 0, 1) : new Vec3(0, 1, 0);
			Vec3 n1 = axis.cross(ref).normalize();
			Vec3 n2 = n1.cross(axis).normalize();
			double maxR = 0;
			for (double[] p : profile) maxR = Math.max(maxR, p[1]);
			int repeats = repeatsFor(2 * Math.PI * maxR, sides);
			for (int i = 0; i + 1 < profile.length; i++) {
				double d0 = profile[i][0], d1 = profile[i + 1][0], r0 = profile[i][1], r1 = profile[i + 1][1];
				double slant = Math.hypot(d1 - d0, r1 - r0);
				int rows = Math.max(1, (int) Math.ceil(slant - 0.05));
				for (int row = 0; row < rows; row++) {
					double t0 = row / (double) rows, t1 = (row + 1) / (double) rows;
					double da = d0 + (d1 - d0) * t0, db = d0 + (d1 - d0) * t1;
					double ra = r0 + (r1 - r0) * t0, rb = r0 + (r1 - r0) * t1;
					float vSpan = (float) Math.min(1, slant / rows);
					for (int k = 0; k < sides; k++) {
						double ang0 = 2 * Math.PI * k / sides, ang1 = 2 * Math.PI * (k + 1) / sides;
						Vec3 dir0 = n1.scale(Math.cos(ang0)).add(n2.scale(Math.sin(ang0)));
						Vec3 dir1 = n1.scale(Math.cos(ang1)).add(n2.scale(Math.sin(ang1)));
						Vec3 p00 = a.add(axis.scale(da)).add(dir0.scale(ra)), p01 = a.add(axis.scale(db)).add(dir0.scale(rb));
						Vec3 p10 = a.add(axis.scale(da)).add(dir1.scale(ra)), p11 = a.add(axis.scale(db)).add(dir1.scale(rb));
						Vec3 n = dir0.add(dir1).normalize();
						double slice = repeats / (double) sides;
						double u0 = (k * slice) % 1.0, u1 = u0 + slice;
						float fu0 = (float) (uv[0] + (uv[2] - uv[0]) * u0), fu1 = (float) (uv[0] + (uv[2] - uv[0]) * Math.min(1, u1));
						float fv0 = uv[1], fv1 = uv[1] + (uv[3] - uv[1]) * vSpan;
						// Seen from outside, with the far end (b) at the top of the texture.
						quad(p01, p00, p10, p11, n, fu0, fv0, fu1, fv1);
					}
				}
			}
			if (capStart && profile[0][1] > 1e-3) disc(a.add(axis.scale(profile[0][0])), axis.scale(-1), profile[0][1], sides, uv);
			int last = profile.length - 1;
			if (capEnd && profile[last][1] > 1e-3) disc(a.add(axis.scale(profile[last][0])), axis, profile[last][1], sides, uv);
		}

		/** How many times the texture goes round a circumference (about once a block, dividing the sides evenly). */
		static int repeatsFor(double circumference, int sides) {
			int want = Math.max(1, (int) Math.round(circumference));
			int best = 1;
			for (int r = 1; r <= sides; r++) {
				if (sides % r == 0 && Math.abs(r - want) < Math.abs(best - want)) best = r;
			}
			return best;
		}

		/** A flat round end facing {@code outward}. */
		void disc(Vec3 centre, Vec3 outward, double radius, int sides, float[] uv) {
			Vec3 ref = Math.abs(outward.y) > 0.9 ? new Vec3(0, 0, 1) : new Vec3(0, 1, 0);
			Vec3 n1 = outward.cross(ref).normalize();
			Vec3 n2 = n1.cross(outward).normalize();
			float cu = (uv[0] + uv[2]) / 2, cv = (uv[1] + uv[3]) / 2;
			float hu = (uv[2] - uv[0]) / 2 * (float) Math.min(1, radius), hv = (uv[3] - uv[1]) / 2 * (float) Math.min(1, radius);
			for (int k = 0; k < sides; k += 2) {
				double a0 = 2 * Math.PI * k / sides, a1 = 2 * Math.PI * (k + 1) / sides, a2 = 2 * Math.PI * (k + 2) / sides;
				Vec3 p0 = centre.add(n1.scale(Math.cos(a0) * radius)).add(n2.scale(Math.sin(a0) * radius));
				Vec3 p1 = centre.add(n1.scale(Math.cos(a1) * radius)).add(n2.scale(Math.sin(a1) * radius));
				Vec3 p2 = centre.add(n1.scale(Math.cos(a2) * radius)).add(n2.scale(Math.sin(a2) * radius));
				// A quad fan wound to face outward.
				quad(centre, p0, p1, p2, outward, cu - hu, cv - hv, cu + hu, cv + hv);
			}
		}

		/** A plain cylinder between two points. */
		void cylinder(Vec3 a, Vec3 b, double r, int sides, float[] uv, boolean caps) {
			revolve(a, b, new double[][] {{0, r}, {a.distanceTo(b), r}}, sides, uv, caps, caps);
		}

		/** A vertical cylinder standing at (cx, cz). */
		void column(double cx, double cz, double y0, double y1, double r, int sides, float[] uv) {
			cylinder(v(cx, y0, cz), v(cx, y1, cz), r, sides, uv, true);
		}

		/** A thin band round a vertical cylinder (a weld seam or stiffening ring). */
		void band(double cx, double cz, double y, double r, double h, float[] uv) {
			if (!detail) {
				return;
			}
			revolve(v(cx, y, cz), v(cx, y + h, cz), new double[][] {{0, r}, {h, r}}, 24, uv, true, true);
		}

		/** A dome (half an ellipsoid) on a vertical axis: from y0 up (or down when height is negative). */
		void dome(double cx, double cz, double y0, double r, double height, float[] uv) {
			double[][] profile = new double[7][];
			for (int i = 0; i < 7; i++) {
				double t = i / 6.0;
				profile[i] = new double[] {Math.abs(height) * t, r * Math.sqrt(Math.max(0, 1 - t * t))};
			}
			Vec3 a = v(cx, y0, cz), b = v(cx, y0 + Math.signum(height), cz);
			revolve(a, b, profile, sides(24), uv, false, false);
		}

		/** A straight run of pipe through points, with a flange at each end. */
		void pipe(double r, float[] uv, Vec3... points) {
			for (int i = 0; i + 1 < points.length; i++) {
				cylinder(points[i], points[i + 1], r, sides(12), uv, false);
				if (i > 0) {
					sphereJoint(points[i], r);
				}
			}
			if (!detail) {
				return;
			}
			cylinder(points[0], points[0].add(points[1].subtract(points[0]).normalize().scale(0.12)), r * 1.35, 12, FLANGE, true);
			Vec3 last = points[points.length - 1], prev = points[points.length - 2];
			cylinder(last.add(prev.subtract(last).normalize().scale(0.12)), last, r * 1.35, 12, FLANGE, true);
		}

		/** A rounded elbow joint. */
		void sphereJoint(Vec3 c, double r) {
			revolve(c.add(0, -r, 0), c.add(0, r, 0), new double[][] {{0, 0}, {r * 0.3, r * 0.8}, {r, r}, {r * 1.7, r * 0.8}, {2 * r, 0}},
					12, STAINLESS_DARK, false, false);
		}
	}

	static Vec3 v(double x, double y, double z) {
		return new Vec3(x, y, z);
	}

	// ------------------------------------------------------------------ the equipment

	static void vessel(Frame f) {
		double c = 3.5;
		// Concrete support skirt, then the vessel: bottom head, shell with weld seams, nozzle belt, closure flange.
		f.revolve(v(c, 0, c), v(c, 1, c), new double[][] {{0, 3.4}, {1.6, 3.4}, {1.6, 2.9}, {2.2, 2.9}}, f.sides(24), CONCRETE, false, true);
		f.dome(c, c, 3.6, 2.6, -1.5, STAINLESS);
		f.revolve(v(c, 3.6, c), v(c, 4.6, c), new double[][] {{0, 2.6}, {6.4, 2.6}}, f.sides(32), STAINLESS, false, false);
		if (f.detail) {
			for (double y : new double[] {5.4, 7.4}) {
				f.band(c, c, y, 2.63, 0.22, WELD_BAND);
			}
		}
		f.revolve(v(c, 10, c), v(c, 11, c), new double[][] {{0, 2.6}, {0.2, 2.95}, {1.5, 2.95}, {1.7, 2.6}}, f.sides(32), STAINLESS_DARK, false, false);
		// Hot- and cold-leg nozzles round the belt, flanged.
		for (int i = 0; i < 8; i++) {
			double a = Math.PI / 8 + i * Math.PI / 4;
			Vec3 dir = v(Math.cos(a), 0, Math.sin(a));
			Vec3 start = v(c, 10.85, c).add(dir.scale(2.9));
			Vec3 end = v(c, 10.85, c).add(dir.scale(3.45));
			f.cylinder(start, end, i % 2 == 0 ? 0.42 : 0.36, f.sides(12), STAINLESS, false);
			f.cylinder(end.subtract(dir.scale(0.1)), end, i % 2 == 0 ? 0.55 : 0.48, f.sides(12), FLANGE, true);
		}
		f.revolve(v(c, 11.7, c), v(c, 12.7, c), new double[][] {{0, 3.0}, {0.75, 3.0}}, f.sides(32), FLANGE, true, true);
		// Warning signs on the skirt.
		if (f.detail) {
			f.plate(c - 0.4, 0.5, c - 3.45, c + 0.4, 1.3, c - 3.4, 0, TREFOIL_SIGN, PAINT_GREY);
			f.plate(c - 0.4, 0.5, c + 3.4, c + 0.4, 1.3, c + 3.45, 1, TREFOIL_SIGN, PAINT_GREY);
		}
	}

	/**
	 * A small modular reactor: one slim steel containment holding the reactor, its steam generator and pressurizer,
	 * standing in the reactor pool on a support skirt. Steam and feedwater nozzles near the top, the drive housings on
	 * the top head, a bolted mid flange where it opens for refuelling.
	 */
	static void smallModule(Frame f) {
		double c = 2.5;
		for (int i = 0; i < 4; i++) {
			double a = Math.PI / 4 + i * Math.PI / 2;
			double x = c + Math.cos(a) * 1.6, z = c + Math.sin(a) * 1.6;
			f.box(x - 0.22, 0, z - 0.22, x + 0.22, 1.4, z + 0.22, CARBON_STEEL);
		}
		f.revolve(v(c, 0.9, c), v(c, 1.9, c), new double[][] {{0, 1.9}, {0.35, 1.9}}, f.sides(24), CARBON_STEEL, true, true);
		f.dome(c, c, 3.2, 2.2, -1.9, STAINLESS);
		f.revolve(v(c, 3.2, c), v(c, 4.2, c), new double[][] {{0, 2.2}, {10.0, 2.2}}, f.sides(32), STAINLESS, false, false);
		for (double y : new double[] {6.0, 9.0, 11.8}) {
			f.band(c, c, y, 2.23, 0.2, WELD_BAND);
		}
		f.revolve(v(c, 13.2, c), v(c, 14.2, c), new double[][] {{0, 2.2}, {0.1, 2.5}, {0.8, 2.5}, {0.9, 2.2}}, f.sides(32), FLANGE, false, false);
		f.revolve(v(c, 14.1, c), v(c, 15.1, c), new double[][] {{0, 2.2}, {5.3, 2.2}}, f.sides(32), STAINLESS, false, false);
		f.band(c, c, 17.0, 2.23, 0.2, WELD_BAND);
		f.dome(c, c, 19.4, 2.2, 1.7, STAINLESS);
		// Steam (upper) and feedwater (lower) nozzles, two of each, toward the front.
		for (int side : new int[] {-1, 1}) {
			double x = c + side * 0.9;
			f.cylinder(v(x, 18.4, c - 2.0), v(x, 18.4, c - 2.48), 0.32, f.sides(12), STAINLESS, false);
			f.cylinder(v(x, 18.4, c - 2.38), v(x, 18.4, c - 2.5), 0.44, f.sides(12), FLANGE, true);
			f.cylinder(v(x, 16.2, c - 2.0), v(x, 16.2, c - 2.48), 0.24, f.sides(12), STAINLESS, false);
			f.cylinder(v(x, 16.2, c - 2.38), v(x, 16.2, c - 2.5), 0.34, f.sides(12), FLANGE, true);
		}
		// The drive housings on the top head.
		double step = f.detail ? 0.42 : 0.84;
		for (double dx = -0.85; dx <= 0.86; dx += step) {
			for (double dz = -0.85; dz <= 0.86; dz += step) {
				if (dx * dx + dz * dz > 0.9 * 0.9) continue;
				f.cylinder(v(c + dx, 20.6, c + dz), v(c + dx, 21.85, c + dz), f.detail ? 0.1 : 0.18, f.detail ? 6 : 4, CRDM, true);
			}
		}
		// Lifting lugs.
		for (int i = 0; i < 4; i++) {
			double a = i * Math.PI / 2;
			double x = c + Math.cos(a) * 1.7, z = c + Math.sin(a) * 1.7;
			f.box(x - 0.12, 19.8, z - 0.12, x + 0.12, 20.9, z + 0.12, CARBON_STEEL);
		}
		if (f.detail) {
			f.plate(c - 0.35, 7.0, c - 2.27, c + 0.35, 7.7, c - 2.22, 0, TREFOIL_SIGN, PAINT_GREY);
			f.plate(c - 0.5, 10.4, c - 2.27, c + 0.5, 10.8, c - 2.22, 0, NAMEPLATE, PAINT_GREY);
		}
	}

	/**
	 * A small turbine-generator for one module, with its own condenser underneath: the turbine casing on a skid, the
	 * generator and exciter behind it, and the insulated steam inlet on top.
	 */
	static void smallTurbine(Frame f) {
		f.box(0, 0, 0, 3, 0.58, 5, CARBON_STEEL);
		f.box(0.02, 0.58, 0.02, 2.98, 0.6, 4.98, GRATING);
		// Condenser shell under the turbine.
		f.box(0.35, 0.6, 0.3, 2.65, 1.1, 2.7, PAINT_GREY);
		// Turbine casing with its split flange, along the shaft.
		f.cylinder(v(1.5, 1.95, 0.35), v(1.5, 1.95, 2.65), 0.9, f.sides(20), TURBINE_BLUE, true);
		f.cylinder(v(1.5, 1.95, 0.25), v(1.5, 1.95, 0.4), 1.0, f.sides(20), FLANGE, true);
		f.cylinder(v(1.5, 1.95, 2.6), v(1.5, 1.95, 2.75), 1.0, f.sides(20), FLANGE, true);
		f.box(0.45, 1.85, 0.4, 2.55, 2.05, 2.6, STAINLESS_DARK);
		// Generator and exciter.
		f.box(0.45, 0.6, 2.85, 2.55, 2.95, 4.55, GENERATOR, PAINT_GREEN, PAINT_GREEN);
		f.cylinder(v(1.5, 1.8, 2.65), v(1.5, 1.8, 2.9), 0.35, f.sides(12), STAINLESS, false);
		f.cylinder(v(1.5, 1.8, 4.55), v(1.5, 1.8, 4.95), 0.55, f.sides(16), PAINT_GREEN, true);
		// Steam inlet from above.
		f.cylinder(v(1.5, 2.8, 1.2), v(1.5, 4.0, 1.2), 0.32, f.sides(12), INSULATION, true);
		f.cylinder(v(1.5, 3.85, 1.2), v(1.5, 4.0, 1.2), 0.45, f.sides(12), FLANGE, true);
		if (f.detail) {
			f.plate(1.0, 1.4, 4.55, 2.0, 2.0, 4.6, 1, NAMEPLATE, PAINT_GREY);
			for (double x : new double[] {0.05, 2.95}) {
				f.box(x - 0.03, 0.6, 0.1, x + 0.03, 1.6, 0.16, HANDRAIL);
				f.box(x - 0.03, 0.6, 4.84, x + 0.03, 1.6, 4.9, HANDRAIL);
				f.box(x - 0.03, 1.55, 0.1, x + 0.03, 1.61, 4.9, HANDRAIL);
			}
		}
	}

	/**
	 * A vertical circulating water pump as in a pump house: the pump column going down into its bay, a cast discharge
	 * head with the outlet elbow, the motor stand and a big blue motor with its cooling fins and terminal box on top.
	 */
	static void circulatingWaterPump(Frame f) {
		double c = 1.5;
		f.box(0.05, 0, 0.05, 2.95, 0.28, 2.95, CARBON_STEEL);
		f.box(0.07, 0.28, 0.07, 2.93, 0.3, 2.93, GRATING);
		f.revolve(v(c, 0.3, c), v(c, 1.3, c), new double[][] {{0, 1.0}, {1.2, 1.0}, {1.5, 1.25}, {1.7, 1.25}}, f.sides(20), PAINT_BLUE, false, true);
		// The discharge elbow toward the front.
		f.cylinder(v(c, 1.0, c - 0.6), v(c, 1.0, c - 1.55), 0.55, f.sides(16), PAINT_BLUE, false);
		f.cylinder(v(c, 1.0, c - 1.4), v(c, 1.0, c - 1.55), 0.72, f.sides(16), FLANGE, true);
		// Motor stand: four legs and a ring.
		for (int i = 0; i < 4; i++) {
			double a = Math.PI / 4 + i * Math.PI / 2;
			double x = c + Math.cos(a) * 0.95, z = c + Math.sin(a) * 0.95;
			f.box(x - 0.1, 1.7, z - 0.1, x + 0.1, 2.6, z + 0.1, CARBON_STEEL);
		}
		f.cylinder(v(c, 1.9, c), v(c, 2.5, c), 0.35, f.sides(12), STAINLESS, false);
		f.revolve(v(c, 2.6, c), v(c, 3.6, c), new double[][] {{0, 1.25}, {0.25, 1.25}}, f.sides(24), CARBON_STEEL, true, true);
		// The motor.
		f.revolve(v(c, 2.85, c), v(c, 3.85, c), new double[][] {{0, 1.15}, {2.4, 1.15}, {2.6, 0.9}, {2.75, 0.9}}, f.sides(24), PAINT_BLUE, false, true);
		if (f.detail) {
			for (int k = 0; k < 16; k++) {
				double a = k * Math.PI / 8;
				double x = c + Math.cos(a) * 1.15, z = c + Math.sin(a) * 1.15;
				f.box(x - 0.04, 3.0, z - 0.04, x + 0.04, 5.1, z + 0.04, CARBON_STEEL);
			}
			f.box(c + 0.9, 3.6, c - 0.35, c + 1.45, 4.3, c + 0.35, PAINT_GREY);
			f.plate(c - 0.35, 4.3, c - 1.2, c + 0.35, 4.7, c - 1.16, 0, NAMEPLATE, PAINT_GREY);
		}
		f.box(c - 0.3, 5.6, c - 0.3, c + 0.3, 5.9, c + 0.3, HAZARD);
	}

	/** The closed head: dome, drive housings, shroud rings and the lifting rig (off while refuelling). */
	static void vesselHead(Frame f) {
		double c = 3.5;
		f.dome(c, c, 12.45, 2.6, 1.9, STAINLESS);
		// The control-rod drive housings in a circle on the head.
		double step = f.detail ? 0.45 : 0.9;
		for (double dx = -1.8; dx <= 1.81; dx += step) {
			for (double dz = -1.8; dz <= 1.81; dz += step) {
				if (dx * dx + dz * dz > 1.85 * 1.85) continue;
				double headTop = 12.45 + 1.9 * Math.sqrt(Math.max(0, 1 - (dx * dx + dz * dz) / (2.6 * 2.6)));
				f.cylinder(v(c + dx, headTop - 0.1, c + dz), v(c + dx, 17.6, c + dz), f.detail ? 0.11 : 0.2, f.detail ? 6 : 4, CRDM, true);
			}
		}
		// Cooling shroud ring and the lifting rig on top.
		f.revolve(v(c, 15.2, c), v(c, 16.2, c), new double[][] {{0, 2.25}, {0.25, 2.25}}, f.sides(24), CARBON_STEEL, true, true);
		f.revolve(v(c, 17.9, c), v(c, 18.9, c), new double[][] {{0, 2.25}, {0.3, 2.25}}, f.sides(24), CARBON_STEEL, true, true);
		for (int i = 0; i < 4; i++) {
			double a = Math.PI / 4 + i * Math.PI / 2;
			double x = c + Math.cos(a) * 2.1, z = c + Math.sin(a) * 2.1;
			f.box(x - 0.1, 13.4, z - 0.1, x + 0.1, 18.2, z + 0.1, CARBON_STEEL);
			f.cylinder(v(x, 18.2, z), v(c, 19.7, c), 0.08, 6, PAINT_GREY, false);
		}
		f.box(c - 0.25, 19.6, c - 0.25, c + 0.25, 19.95, c + 0.25, HAZARD);
	}

	/** The drive rods above the housings: they rise as the control rods come out ({@code rods} is % inserted). */
	static void vesselRods(Frame f, double rods) {
		if (!f.detail) {
			return;
		}
		double c = 3.5;
		double rise = 1.2 * (1 - rods / 100.0);
		for (double dx = -1.8; dx <= 1.81; dx += 0.45) {
			for (double dz = -1.8; dz <= 1.81; dz += 0.45) {
				if (dx * dx + dz * dz > 1.85 * 1.85) continue;
				f.cylinder(v(c + dx, 17.6, c + dz), v(c + dx, 17.9 + rise, c + dz), 0.05, 4, STAINLESS, true);
			}
		}
	}

	static void steamGenerator(Frame f) {
		double c = 3.5;
		// Four legs on a support ring, the channel head with its primary nozzles, the tube bundle shell, the cone and
		// the steam drum, a domed top with the steam outlet, and a walkway with handrails at the cone.
		for (int i = 0; i < 4; i++) {
			double a = Math.PI / 4 + i * Math.PI / 2;
			double x = c + Math.cos(a) * 2.0, z = c + Math.sin(a) * 2.0;
			f.box(x - 0.25, 0, z - 0.25, x + 0.25, 2.3, z + 0.25, CARBON_STEEL);
		}
		f.revolve(v(c, 2.2, c), v(c, 3.2, c), new double[][] {{0, 2.2}, {0.3, 2.2}}, f.sides(24), CARBON_STEEL, true, true);
		f.dome(c, c, 3.5, 2.05, -1.6, STAINLESS);
		for (int side : new int[] {-1, 1}) {
			Vec3 start = v(c + side * 0.9, 2.5, c - 1.4);
			f.cylinder(start, v(c + side * 0.9, 2.5, c - 2.45), 0.45, f.sides(12), STAINLESS, false);
			f.cylinder(v(c + side * 0.9, 2.5, c - 2.35), v(c + side * 0.9, 2.5, c - 2.5), 0.6, f.sides(12), FLANGE, true);
		}
		f.revolve(v(c, 3.5, c), v(c, 4.5, c), new double[][] {{0, 2.1}, {7.5, 2.1}, {9.5, 2.9}, {16.0, 2.9}}, f.sides(32), PAINT_WHITE, false, false);
		for (double y : new double[] {6, 8.5, 14.5, 17}) {
			double r = y < 11 ? 2.13 : 2.93;
			f.band(c, c, y, r, 0.2, WELD_BAND);
		}
		f.dome(c, c, 19.5, 2.9, 1.2, PAINT_WHITE);
		f.cylinder(v(c, 20.4, c), v(c, 21.0, c), 0.5, f.sides(12), STAINLESS, false);
		f.cylinder(v(c, 20.85, c), v(c, 21.0, c), 0.68, f.sides(12), FLANGE, true);
		// Feedwater nozzle on the drum.
		f.cylinder(v(c, 14.0, c - 2.85), v(c, 14.0, c - 3.45), 0.3, f.sides(12), STAINLESS, false);
		f.cylinder(v(c, 14.0, c - 3.35), v(c, 14.0, c - 3.5), 0.42, f.sides(12), FLANGE, true);
		// Upper supports and a walkway round the cone.
		f.revolve(v(c, 12.0, c), v(c, 13.0, c), new double[][] {{0, 3.35}, {0.12, 3.35}}, f.sides(24), GRATING, true, true);
		for (int i = 0; i < 8; i++) {
			double a = i * Math.PI / 4;
			double x = c + Math.cos(a) * 3.3, z = c + Math.sin(a) * 3.3;
			f.box(x - 0.04, 12.12, z - 0.04, x + 0.04, 13.1, z + 0.04, HANDRAIL);
		}
		f.revolve(v(c, 13.0, c), v(c, 14, c), new double[][] {{0, 3.33}, {0.06, 3.33}}, f.sides(24), HANDRAIL, false, false);
		f.revolve(v(c, 12.6, c), v(c, 13.6, c), new double[][] {{0, 3.33}, {0.05, 3.33}}, f.sides(24), HANDRAIL, false, false);
		f.plate(c - 0.5, 9.0, c - 2.2, c + 0.5, 9.5, c - 2.14, 0, NAMEPLATE, STAINLESS);
	}

	static void coolantPump(Frame f) {
		double cx = 2.5, cz = 1.5;
		// Feet, the pump bowl with its suction and discharge, the motor stand with its windows, the big motor with
		// cooling fins and an air cooler, and the flywheel cover.
		for (double[] foot : new double[][] {{0.6, 0.4}, {4.4, 0.4}, {2.5, 2.6}}) {
			f.box(foot[0] - 0.35, 0, foot[1] - 0.3, foot[0] + 0.35, 0.6, foot[1] + 0.3, CARBON_STEEL);
		}
		f.revolve(v(cx, 0.3, cz), v(cx, 1.3, cz), new double[][] {{0, 0.6}, {0.4, 1.1}, {1.1, 1.35}, {1.9, 1.2}, {2.3, 0.9}}, f.sides(24), STAINLESS_DARK,
				true, true);
		f.cylinder(v(cx, 1.2, cz - 0.9), v(cx, 1.2, cz - 1.5), 0.5, f.sides(12), STAINLESS_DARK, false);
		f.cylinder(v(cx, 1.2, cz - 1.38), v(cx, 1.2, cz - 1.5), 0.68, f.sides(12), FLANGE, true);
		f.cylinder(v(cx - 1.0, 1.4, cz), v(cx - 2.45, 1.4, cz), 0.45, f.sides(12), STAINLESS_DARK, false);
		f.cylinder(v(cx - 2.33, 1.4, cz), v(cx - 2.5, 1.4, cz), 0.62, f.sides(12), FLANGE, true);
		f.cylinder(v(cx, 2.55, cz), v(cx, 2.75, cz), 1.2, f.sides(24), FLANGE, true);
		f.cylinder(v(cx, 2.75, cz), v(cx, 4.5, cz), 0.85, f.sides(16), CARBON_STEEL, true);
		for (int i = 0; i < 4; i++) {
			double a = i * Math.PI / 2;
			f.box(cx + Math.cos(a) * 0.8 - 0.15, 3.0, cz + Math.sin(a) * 0.8 - 0.15, cx + Math.cos(a) * 0.8 + 0.15, 4.4,
					cz + Math.sin(a) * 0.8 + 0.15, STAINLESS);
		}
		f.cylinder(v(cx, 4.5, cz), v(cx, 8.4, cz), 1.3, f.sides(24), PAINT_BLUE, true);
		for (double y = 5.0; y < 8.2; y += 0.45) {
			f.band(cx, cz, y, 1.36, 0.12, FINS);
		}
		f.box(cx + 1.2, 4.9, cz - 0.8, cx + 2.2, 7.9, cz + 0.8, PAINT_BLUE, PAINT_GREY, PAINT_GREY);
		f.box(cx + 2.2, 5.2, cz - 0.7, cx + 2.35, 7.6, cz + 0.7, RADIATOR);
		f.revolve(v(cx, 8.4, cz), v(cx, 9.4, cz), new double[][] {{0, 1.15}, {0.6, 1.15}, {0.9, 0.7}}, f.sides(24), PAINT_BLUE, false, true);
		f.box(cx - 1.75, 5.5, cz - 0.45, cx - 1.25, 6.6, cz + 0.45, PAINT_GREY);
		f.plate(cx - 0.4, 6.0, cz - 1.33, cx + 0.4, 6.5, cz - 1.3, 0, NAMEPLATE, PAINT_BLUE);
	}

	static void pressurizer(Frame f) {
		double cx = 2.5, cz = 1.5;
		f.revolve(v(cx, 0, cz), v(cx, 1, cz), new double[][] {{0, 1.25}, {1.5, 1.25}}, f.sides(24), CARBON_STEEL, false, true);
		f.dome(cx, cz, 2.4, 1.3, -1.0, STAINLESS);
		f.cylinder(v(cx, 2.4, cz), v(cx, 16.0, cz), 1.3, f.sides(24), STAINLESS, false);
		for (double y : new double[] {4.5, 8.0, 11.5, 15.0}) {
			f.band(cx, cz, y, 1.33, 0.18, WELD_BAND);
		}
		f.dome(cx, cz, 16.0, 1.3, 1.0, STAINLESS);
		// Spray line in at the top, and three safety valves with their discharge pipes.
		f.pipe(0.12, STAINLESS, v(cx - 0.3, 16.9, cz), v(cx - 0.3, 17.6, cz), v(cx - 1.6, 17.6, cz), v(cx - 1.6, 12, cz - 0.9));
		for (int i = 0; i < 3; i++) {
			double x = cx - 0.5 + i * 0.5;
			f.cylinder(v(x, 16.85, cz + 0.4), v(x, 17.5, cz + 0.4), 0.13, f.sides(8), STAINLESS_DARK, true);
			f.box(x - 0.12, 17.5, cz + 0.28, x + 0.12, 17.8, cz + 0.52, PAINT_GREY);
		}
		f.box(cx - 1.75, 13.8, cz - 1.5, cx + 1.75, 13.95, cz - 1.2, GRATING);
		f.box(cx - 1.75, 13.95, cz - 1.52, cx + 1.75, 14.9, cz - 1.48, HANDRAIL);
		f.plate(cx - 0.4, 0.4, cz - 1.27, cx + 0.4, 1.2, cz - 1.22, 0, TREFOIL_SIGN, PAINT_GREY);
	}

	static void feedwaterPump(Frame f) {
		f.box(0.05, 0, 0.2, 2.95, 0.3, 1.8, CARBON_STEEL);
		// Motor along x with cooling fins, the coupling guard, and the pump with its nozzles up to the steam line.
		f.cylinder(v(0.15, 0.95, 1.0), v(1.35, 0.95, 1.0), 0.6, f.sides(16), PAINT_BLUE, true);
		for (double x = 0.3; x < 1.3; x += 0.2) {
			f.revolve(v(x, 0.95, 1.0), v(x + 1, 0.95, 1.0), new double[][] {{0, 0.66}, {0.06, 0.66}}, f.sides(16), FINS, true, true);
		}
		f.box(0.35, 0.3, 0.5, 1.15, 0.45, 1.5, CARBON_STEEL);
		f.box(1.35, 0.7, 0.75, 1.75, 1.2, 1.25, HAZARD);
		f.cylinder(v(1.75, 0.95, 1.0), v(2.8, 0.95, 1.0), 0.55, f.sides(16), STAINLESS_DARK, true);
		f.box(1.9, 0.3, 0.6, 2.7, 0.45, 1.4, CARBON_STEEL);
		// Discharge straight up out of the top of the casing's cell (to the feedwater line above), suction in from the end.
		f.pipe(0.24, STAINLESS, v(2.3, 1.45, 1.0), v(2.3, 2.2, 1.0), v(2.5, 2.4, 0.5), v(2.5, 3.0, 0.5));
		f.pipe(0.2, STAINLESS, v(2.8, 0.95, 1.5), v(3.0, 0.95, 1.5));
	}

	static void transformer(Frame f) {
		f.box(0, 0, 0, 5, 0.2, 5, CONCRETE);
		f.box(0.05, 0.2, 0.05, 4.95, 0.4, 0.15, CONCRETE);
		// Tank with stiffeners, radiator banks on both sides, the conservator, the bushings, the control cabinet.
		f.box(0.9, 0.2, 0.9, 4.1, 4.2, 4.1, TANK_GREY);
		for (double z = 1.4; z < 4.0; z += 0.8) {
			f.box(0.84, 0.2, z - 0.06, 0.9, 4.2, z + 0.06, CARBON_STEEL);
			f.box(4.1, 0.2, z - 0.06, 4.16, 4.2, z + 0.06, CARBON_STEEL);
		}
		f.box(0.85, 4.2, 0.85, 4.15, 4.35, 4.15, FLANGE);
		for (int side = 0; side < 2; side++) {
			double x0 = side == 0 ? 0.1 : 4.25, x1 = side == 0 ? 0.75 : 4.9;
			for (double z = 1.1; z < 3.9; z += 0.45) {
				f.box(x0, 0.7, z, x1, 3.8, z + 0.12, RADIATOR);
			}
			f.box(x0, 3.8, 1.0, x1, 4.0, 3.9, CARBON_STEEL);
			f.box(x0, 0.5, 1.0, x1, 0.7, 3.9, CARBON_STEEL);
		}
		f.cylinder(v(0.8, 5.1, 4.4), v(4.2, 5.1, 4.4), 0.45, f.sides(16), TANK_GREY, true);
		for (double x : new double[] {1.2, 3.8}) {
			f.box(x - 0.08, 4.35, 4.3, x + 0.08, 4.7, 4.5, CARBON_STEEL);
		}
		f.pipe(0.08, STAINLESS_DARK, v(2.5, 4.35, 3.8), v(2.5, 4.9, 4.35));
		for (double x : new double[] {1.4, 2.5, 3.6}) {
			bushing(f, x, 4.35, 2.2, 3.1);
		}
		for (double x : new double[] {1.6, 3.4}) {
			bushing(f, x, 4.35, 3.4, 1.2);
		}
		f.box(1.6, 0.6, 0.45, 3.4, 2.4, 0.9, PAINT_GREY);
		f.plate(1.9, 1.7, 0.43, 3.1, 2.2, 0.45, 0, NAMEPLATE, PAINT_GREY);
		f.plate(2.2, 0.9, 0.43, 2.8, 1.5, 0.45, 0, HAZARD, PAINT_GREY);
	}

	/** A porcelain bushing with sheds and a copper terminal on top. */
	static void bushing(Frame f, double x, double y0, double z, double height) {
		f.cylinder(v(x, y0, z), v(x, y0 + 0.3, z), 0.28, f.sides(12), CARBON_STEEL, true);
		java.util.List<double[]> profile = new java.util.ArrayList<>();
		profile.add(new double[] {0, 0.14});
		for (double d = 0.25; d + 0.2 < height; d += 0.28) {
			profile.add(new double[] {d - 0.05, 0.14});
			profile.add(new double[] {d, 0.26});
			profile.add(new double[] {d + 0.05, 0.26});
			profile.add(new double[] {d + 0.1, 0.14});
		}
		profile.add(new double[] {height, 0.14});
		f.revolve(v(x, y0 + 0.3, z), v(x, y0 + 1.3, z), profile.toArray(new double[0][]), f.sides(12), GLAZE_BROWN, false, true);
		f.cylinder(v(x, y0 + 0.3 + height, z), v(x, y0 + 0.55 + height, z), 0.1, f.sides(8), COPPER, true);
	}

	/**
	 * The reactor control console: a benchboard section three wide, in line with the control room's boards (round 30,
	 * tools/board_live.py console_board): a live board, its annunciator, nuclear instruments, rod position display,
	 * step counters, plant computer page and its working rod control and reactor trip switches drawn by {@link
	 * LiveBoards}.
	 */
	static void console(Frame f) {
		ControlBoards.still(EquipmentKind.CONTROL_CONSOLE, f);
	}
}
