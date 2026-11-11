package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.jimbozoomer.jugcraft.drone.DepotDisplayBlockEntity;
import io.github.jimbozoomer.jugcraft.drone.DepotView;
import io.github.jimbozoomer.jugcraft.drone.DroneTerminalBlockEntity;
import io.github.jimbozoomer.jugcraft.drone.DroneTier;
import io.github.jimbozoomer.jugcraft.drone.FlightPath;
import io.github.jimbozoomer.jugcraft.drone.HoloTableBlock;
import io.github.jimbozoomer.jugcraft.drone.PlatformLayout;
import io.github.jimbozoomer.jugcraft.tower.JugcraftTower;
import io.github.jimbozoomer.jugcraft.tower.TowerCoreBlockEntity;
import io.github.jimbozoomer.jugcraft.tower.TowerData;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * What a formed hologram table shows. Three kinds:
 *
 * <ul>
 * <li><b>The Tri-Pillar's central table</b> (the one over the Tower Core, in the atrium): the tower as built, as a solid
 * amber model at 1:12, lit from within (3-block cells, dark bands at the tier lines, the tier being built brighter as it
 * goes in, a faint glow round it), turning slowly in place over the table under the projector that hangs on three trusses from the atrium's ceiling (the lantern's top
 * once tier 2 is up, the roof's skylight before), with the drones round it, and the status ticker running round the
 * opening's amber slab edge (approved R7, R9 and R11).</li>
 * <li><b>Any other table linked to a Tri-Pillar</b> (the hall's small table): the {@link TowerDiorama}, a still scale
 * model of the finished tower in 20 chunks of plain Minecraft land, cut square so its soil shows, lit by the room
 * (Justin's redesign 3, approved 25 October 2026; it was the topographic map of R10).</li>
 * <li><b>Any other table</b>: the small see-through map of its depot (platform, pads, pickup, terminal, drones and their
 * stops, an earlier design's tower), under a sweeping scan bar.</li>
 * </ul>
 */
public class HoloMapRenderer implements BlockEntityRenderer<DepotDisplayBlockEntity, HoloMapRenderer.State> {
	private static final RenderType RENDER_TYPE = GlowTypes.glow(DroneDepotRenderer.TEXTURE);
	private static final RenderType SOLID = RenderTypes.entitySolid(DroneDepotRenderer.TEXTURE);
	/** Half the width of the projected map, in blocks (the table is 3 blocks across). */
	private static final double HALF = 1.2;
	/** Height of the map floor above the table top. */
	private static final double FLOOR = 0.35;
	private static final double MAX_HEIGHT = 1.3;

	private static final int GRID = 0x30C84838;
	private static final int PLATFORM = 0x3AD04A3A;
	private static final int EDGE = 0x90E05A48;
	private static final int PAD = 0x80D85040;
	private static final int CHARGER = 0xE0FFA090;
	private static final int PICKUP = 0xA0FFB040;
	private static final int TERMINAL = 0xE0FFFFFF;
	private static final int DOCKED = 0xD0F4E4E0;
	private static final int FLYING = 0xF0FF7A60;
	private static final int CARRYING = 0xF0FFC04A;
	private static final int LINE = 0x50E06A58;
	private static final int STOP = 0xC0FFC04A;
	private static final int SCAN = 0x28F08A70;

	public static final int MODE_DEPOT = 0, MODE_TOWER = 1, MODE_DIORAMA = 2;

	/** A text label in table space: at (x, y, z), turned {@code yaw} (0 reads from +z), {@code scale} blocks a font pixel. */
	record Label(float x, float y, float z, float yaw, float scale, String text, int color, boolean centred) { }

	/** One part drawn solid: a box in a frame at (x, y, z) turned yaw then pitched. */
	record Part(float x, float y, float z, float yaw, float pitch, float x0, float y0, float z0, float x1, float y1, float z1, int color) { }

	public static class State extends BlockEntityRenderState {
		int mode;
		/** The camera in the anchor's space (the diorama draws only the faces turned to it); the anchor's position. */
		double camX, camY, camZ;
		BlockPos at = BlockPos.ZERO;
		/** Boxes to draw: x0, y0, z0, x1, y1, z1 and colour (as float bits), relative to the anchor block. */
		final List<float[]> boxes = new ArrayList<>();
		/** Glowing (translucent) quads. */
		final HoloGeometry.Quads glow = HoloGeometry.Quads.perFrame();
		/** Solid quads: the map table's relief. */
		final HoloGeometry.Quads solid = HoloGeometry.Quads.perFrame();
		/** Solid parts: the projector and its trusses. */
		final List<Part> parts = new ArrayList<>();
		final List<Label> labels = new ArrayList<>();
		/** The tower model (cell units, as {@link HoloGeometry#mesh} makes it), drawn solid at {@link #modelBase}. */
		float[] model;
		float modelBase;
		/** How far the tower model has turned about the table's middle (radians; it turns slowly in place). */
		float spin;
	}

	private final Font font;

	public HoloMapRenderer() {
		this.font = Minecraft.getInstance().font;
	}

	public HoloMapRenderer(BlockEntityRendererProvider.Context context) {
		this.font = context.font();
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
		return 128;
	}

	@Override
	public void extractRenderState(DepotDisplayBlockEntity table, State state, float partialTick, Vec3 cameraPos,
			ModelFeatureRenderer.CrumblingOverlay crumbling) {
		BlockEntityRenderState.extractBase(table, state, crumbling);
		state.boxes.clear();
		state.glow.clear();
		state.solid.clear();
		state.parts.clear();
		state.labels.clear();
		state.model = null;
		state.mode = MODE_DEPOT;
		Level level = table.getLevel();
		if (level == null || !table.isAnchor()) {
			return;
		}
		double now = level.getGameTime() + partialTick;
		BlockPos link = table.terminal();
		DepotView view = link != null && level.getBlockEntity(link) instanceof DroneTerminalBlockEntity terminal ? terminal.view() : null;
		TowerCoreBlockEntity core = view != null && view.towerPos != null && level.getBlockEntity(view.towerPos) instanceof TowerCoreBlockEntity c ? c : null;
		if (core != null && core.layout().foundation != null) {
			BlockPos anchor = table.getBlockPos(), at = core.getBlockPos();
			Vec3 camera = cameraPos == null ? Vec3.ZERO : cameraPos.subtract(anchor.getX(), anchor.getY(), anchor.getZ());
			if (anchor.getX() == at.getX() && anchor.getZ() == at.getZ() && anchor.getY() == at.getY() + 1) {
				state.mode = MODE_TOWER;
				towerHologram(state, level, anchor, core, view, link, now);
			} else {
				state.mode = MODE_DIORAMA;
				state.camX = camera.x;
				state.camY = camera.y;
				state.camZ = camera.z;
				state.at = anchor.immutable();
			}
			return;
		}
		depotMap(state, level, table, view, link, now);
	}

	// ================================================================== the depot map (any other table)

	private static void depotMap(State state, Level level, DepotDisplayBlockEntity table, DepotView view, BlockPos link, double now) {
		double cx = 0.5, cz = 0.5, floor = HoloTableBlock.HEIGHT / 16.0 + FLOOR;
		// Map frame and grid.
		frame(state, cx, floor, cz, HALF, GRID, 0.008);
		for (int i = 1; i < 8; i++) {
			double d = -HALF + i * HALF / 4;
			add(state, cx + d - 0.002, floor, cz - HALF, cx + d + 0.002, floor + 0.002, cz + HALF, GRID);
			add(state, cx - HALF, floor, cz + d - 0.002, cx + HALF, floor + 0.002, cz + d + 0.002, GRID);
		}
		// Scan bar sweeping across the map.
		double sweep = -HALF + ((now % 80) / 80.0) * 2 * HALF;
		add(state, cx - HALF, floor, cz + sweep - 0.01, cx + HALF, floor + 0.35, cz + sweep + 0.01, SCAN);
		// Projector beams from the table top to the map corners.
		for (int sx = -1; sx <= 1; sx += 2) {
			for (int sz = -1; sz <= 1; sz += 2) {
				beam(state, cx, HoloTableBlock.HEIGHT / 16.0, cz, cx + sx * HALF, floor, cz + sz * HALF, 0x18E06A58);
			}
		}

		if (view == null || view.layoutStatus() == PlatformLayout.Status.NO_PLATFORM) {
			return;
		}
		BlockPos anchor = table.getBlockPos();
		// Area to show: the platform plus every flight's points, with a margin.
		double minX = view.minX, maxX = view.minX + view.width, minZ = view.minZ, maxZ = view.minZ + view.depth;
		for (DepotView.FlightView flight : view.flights) {
			FlightPath path = flight.path();
			for (int i = 0; i < path.points(); i++) {
				minX = Math.min(minX, link.getX() + path.x(i));
				maxX = Math.max(maxX, link.getX() + path.x(i));
				minZ = Math.min(minZ, link.getZ() + path.z(i));
				maxZ = Math.max(maxZ, link.getZ() + path.z(i));
			}
		}
		double verticalScale = -1, heightLimit = MAX_HEIGHT;
		if (view.towerPos != null && level.getBlockEntity(view.towerPos) instanceof TowerCoreBlockEntity core) {
			double[] bounds = TOWER_BOUNDS.computeIfAbsent(core.layoutVersion(), version -> towerBounds(core.layout()));
			minX = Math.min(minX, view.towerPos.getX() + bounds[0]);
			maxX = Math.max(maxX, view.towerPos.getX() + bounds[1]);
			minZ = Math.min(minZ, view.towerPos.getZ() + bounds[2]);
			maxZ = Math.max(maxZ, view.towerPos.getZ() + bounds[3]);
			verticalScale = TOWER_MAX_HEIGHT / core.layout().tier(TowerData.TIERS).top;
			heightLimit = TOWER_MAX_HEIGHT;
		}
		minX = Math.min(minX, link.getX());
		maxX = Math.max(maxX, link.getX() + 1);
		minZ = Math.min(minZ, link.getZ());
		maxZ = Math.max(maxZ, link.getZ() + 1);
		double span = Math.max(maxX - minX, maxZ - minZ) + 4;
		double scale = 2 * HALF / span;
		double midX = (minX + maxX) / 2, midZ = (minZ + maxZ) / 2;
		Mapper map = new Mapper(anchor, cx, floor, cz, midX, midZ, view.platformY + 1, scale,
				verticalScale < 0 ? scale * 1.6 : Math.min(scale, verticalScale), heightLimit);

		// Platform, pads, pickup, terminal.
		map.rect(state, view.minX, view.minZ, view.minX + view.width, view.minZ + view.depth, 0.004, PLATFORM);
		map.outline(state, view.minX, view.minZ, view.minX + view.width, view.minZ + view.depth, EDGE);
		for (int[] pad : view.pads) {
			map.rect(state, pad[0], pad[1], pad[0] + PlatformLayout.PAD_SIZE, pad[1] + PlatformLayout.PAD_SIZE, 0.012, PAD);
			double px = pad[0] + PlatformLayout.PAD_SIZE / 2.0, pz = pad[1] + PlatformLayout.PAD_SIZE / 2.0;
			map.rect(state, px - 0.6, pz - 0.6, px + 0.6, pz + 0.6, 0.02, CHARGER);
		}
		if (view.pickup != null) {
			map.rect(state, view.pickup[0], view.pickup[1], view.pickup[0] + PlatformLayout.PICKUP_SIZE, view.pickup[1] + PlatformLayout.PICKUP_SIZE,
					0.016, PICKUP);
		}
		map.marker(state, link.getX() + 0.5, view.platformY + 1.5, link.getZ() + 0.5, 0.05, TERMINAL);
		if (view.towerPos != null) {
			tower(state, level, view.towerPos, map);
		}

		// Drones.
		for (DepotView.DockView dock : view.docks) {
			map.marker(state, link.getX() + dock.x(), link.getY() + dock.y() + 0.2, link.getZ() + dock.z(), droneSize(dock.tier()), DOCKED);
		}
		for (DepotView.FlightView flight : view.flights) {
			FlightPath path = flight.path();
			FlightPath.Pose pose = path.poseAt(flight.ticksAt(now, view.syncTime));
			double wx = link.getX() + pose.x(), wy = link.getY() + pose.y(), wz = link.getZ() + pose.z();
			map.marker(state, wx, wy + 0.2, wz, droneSize(flight.tier()), pose.cable() > 0 ? CARRYING : FLYING);
			map.drop(state, wx, wy, wz, LINE);
			for (int i = 2; i < path.points() - 1; i++) {
				map.marker(state, link.getX() + path.x(i), view.platformY + 1, link.getZ() + path.z(i), 0.02, STOP);
			}
		}
	}

	/**
	 * An earlier design's Drone Tower as a hologram, as it really stands: the finished tiers solid, and the tier being
	 * built growing in (amber) tile by tile as the drones put them in. Nothing that isn't built yet is shown.
	 */
	private static void tower(State state, Level level, BlockPos core, Mapper map) {
		if (!(level.getBlockEntity(core) instanceof TowerCoreBlockEntity entity)) {
			return;
		}
		List<int[]> towerCells = cachedTowerCells(entity);
		if (towerCells.isEmpty()) {
			return;
		}
		// Keep the full tower readable above an operations table without obscuring the room's consoles.
		// The scale stays the same as it grows.
		double vertical = map.verticalScale();
		for (int[] c : towerCells) {
			double wx0 = core.getX() + c[0] * TOWER_CELL, wz0 = core.getZ() + c[2] * TOWER_CELL;
			double y0 = map.floor() + 0.02 + c[1] * TOWER_CELL * vertical;
			add(state, map.x(wx0) + 0.002, y0, map.z(wz0) + 0.002, map.x(wx0 + TOWER_CELL) - 0.002,
					y0 + TOWER_CELL * vertical - 0.002, map.z(wz0 + TOWER_CELL) - 0.002, c[3] == 1 ? TOWER_BUILDING : TOWER);
		}
	}

	private static final int TOWER_CELL = 3;
	/** Full-height overview above the operations table, leaving the command room and displays visible. */
	private static final double TOWER_MAX_HEIGHT = 2.8;
	private static final int TOWER = 0x46E05A48;
	private static final int TOWER_BUILDING = 0x66FFC04A;
	/** The foundation's shown progress moves in steps of this many blocks (each step builds the model again). */
	private static final int FOUNDATION_STEP = 1024;
	// Entity identity keeps identical coordinates in different worlds independent. Weak keys release
	// unloaded towers; each table reuses only its own tower's last synchronized construction state.
	private static final Map<TowerCoreBlockEntity, Projection> PROJECTIONS = new WeakHashMap<>();

	private static final class Projection {
		final int layout, foundation, foundationBlocks, tier, building, firstTierBlocks;
		final BitSet filled;
		final List<int[]> cells;
		float[] model;
		/** The model's highest point, in cells. */
		float modelTop;

		Projection(int layout, int foundation, int foundationBlocks, int tier, int building, int firstTierBlocks, BitSet filled, List<int[]> cells) {
			this.layout = layout;
			this.foundation = foundation;
			this.foundationBlocks = foundationBlocks;
			this.tier = tier;
			this.building = building;
			this.firstTierBlocks = firstTierBlocks;
			this.filled = filled;
			this.cells = cells;
		}

		boolean matches(TowerCoreBlockEntity core, int foundationBlocks, int firstTierBlocks, BitSet filled) {
			return layout == core.layoutVersion() && foundation == core.foundationState() && this.foundationBlocks == foundationBlocks
					&& tier == core.tier() && building == core.building() && this.firstTierBlocks == firstTierBlocks && this.filled.equals(filled);
		}
	}

	private static final Map<Integer, double[]> TOWER_BOUNDS = new HashMap<>();

	private static Projection projection(TowerCoreBlockEntity core) {
		BitSet filled = core.filledTiles();
		TowerData data = core.layout();
		int firstTierBlocks = core.building() == 1 ? firstTierPlaced(data, core.buildProgress()) : 0;
		int foundationBlocks = core.foundationState() == 1 ? foundationPlaced(data, core.buildProgress()) : 0;
		Projection cached = PROJECTIONS.get(core);
		if (cached == null || !cached.matches(core, foundationBlocks, firstTierBlocks, filled)) {
			cached = compute(core, data, foundationBlocks, firstTierBlocks, filled, core.buildProgress());
			PROJECTIONS.put(core, cached);
		}
		return cached;
	}

	private static Projection compute(TowerCoreBlockEntity core, TowerData data, int foundationBlocks, int firstTierBlocks, BitSet filled, float progress) {
		return new Projection(core.layoutVersion(), core.foundationState(), foundationBlocks, core.tier(), core.building(), firstTierBlocks,
				(BitSet) filled.clone(), towerCells(data, core.foundationState(), foundationBlocks, core.tier(), core.building(), progress, filled));
	}

	/** The model being worked out off the render thread, per tower (the last finished one is shown meanwhile). */
	private static final Map<TowerCoreBlockEntity, java.util.concurrent.CompletableFuture<Projection>> PENDING = new WeakHashMap<>();
	private static final Map<TowerCoreBlockEntity, Projection> SHOWN = new WeakHashMap<>();

	/**
	 * The Tri-Pillar's model for the central table: the whole tower's cells and their mesh take tens of milliseconds, so
	 * a change (a tile the drones put in, a step of the foundation) is worked out in the background and shown when it is
	 * ready; null until the first one is.
	 */
	private static Projection modelProjection(TowerCoreBlockEntity core) {
		BitSet filled = core.filledTiles();
		TowerData data = core.layout();
		float progress = core.buildProgress();
		int firstTierBlocks = core.building() == 1 ? firstTierPlaced(data, progress) : 0;
		int foundationBlocks = core.foundationState() == 1 ? foundationPlaced(data, progress) : 0;
		Projection shown = SHOWN.get(core);
		if (shown != null && shown.matches(core, foundationBlocks, firstTierBlocks, filled)) {
			return shown;
		}
		var pending = PENDING.get(core);
		if (pending != null && pending.isDone()) {
			Projection done = pending.getNow(null);
			PENDING.remove(core);
			if (done != null) {
				SHOWN.put(core, done);
				shown = done;
				if (done.matches(core, foundationBlocks, firstTierBlocks, filled)) return shown;
			}
			pending = null;
		}
		if (pending == null) {
			// the snapshot is taken here, on the render thread; the work reads only it and the (fixed) tower data
			Projection start = new Projection(core.layoutVersion(), core.foundationState(), foundationBlocks, core.tier(), core.building(),
					firstTierBlocks, (BitSet) filled.clone(), null);
			int foundation = core.foundationState(), tier = core.tier(), building = core.building();
			PENDING.put(core, java.util.concurrent.CompletableFuture.supplyAsync(() -> {
				Projection p = new Projection(start.layout, start.foundation, start.foundationBlocks, start.tier, start.building, start.firstTierBlocks,
						start.filled, towerCells(data, foundation, foundationBlocks, tier, building, progress, start.filled));
				model(p);
				return p;
			}, net.minecraft.util.Util.backgroundExecutor()));
		}
		return shown;
	}

	static List<int[]> cachedTowerCells(TowerCoreBlockEntity core) {
		return projection(core).cells;
	}

	static int firstTierPlaced(TowerData data, float progress) {
		return placed(data.tier(1), progress);
	}

	/** How many of a core-built step's blocks are in at {@code progress} (the air cells are cleared first). */
	private static int placed(TowerData.Tier step, float progress) {
		return Math.max(0, Math.min(step.place.length,
				Math.round(Math.clamp(progress, 0, 1) * (step.place.length + step.air.length)) - step.air.length));
	}

	/** The foundation's blocks shown at {@code progress}, in steps of {@link #FOUNDATION_STEP}. */
	static int foundationPlaced(TowerData data, float progress) {
		if (data.foundation == null) return 0;
		int n = placed(data.foundation, progress);
		return n >= data.foundation.place.length ? n : n / FOUNDATION_STEP * FOUNDATION_STEP;
	}

	/** Includes the complete coarse hologram cells, so a larger apron cannot project outside the table. */
	private static double[] towerBounds(TowerData data) {
		double[] bounds = {Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY};
		List<TowerData.Tier> steps = new ArrayList<>(data.tiers);
		if (data.foundation != null) steps.add(data.foundation);
		for (var tier : steps) {
			for (int[] b : tier.place) {
				int x = Math.floorDiv(b[0], TOWER_CELL) * TOWER_CELL, z = Math.floorDiv(b[2], TOWER_CELL) * TOWER_CELL;
				bounds[0] = Math.min(bounds[0], x); bounds[1] = Math.max(bounds[1], x + TOWER_CELL);
				bounds[2] = Math.min(bounds[2], z); bounds[3] = Math.max(bounds[3], z + TOWER_CELL);
			}
		}
		return bounds;
	}

	/**
	 * Occupied 3x3x3 cells {cx, cy, cz, building?} of the tower as built: every tier up to {@code tier}, plus what is
	 * in of {@code building} (tier 1 by the core's own progress, higher tiers by the tiles the drones have filled).
	 * A design with a foundation counts it as built once a tier is.
	 */
	static List<int[]> towerCells(int tier, int building, float progress, BitSet filled) {
		return towerCells(TowerData.get(), tier, building, progress, filled);
	}

	static List<int[]> towerCells(TowerData data, int tier, int building, float progress, BitSet filled) {
		int foundation = data.foundation == null ? 0 : tier >= 1 || building >= 1 ? 2 : 0;
		return towerCells(data, foundation, 0, tier, building, progress, filled);
	}

	/**
	 * As above, with the foundation: {@code foundation} 0 not started, 1 being built ({@code foundationBlocks} of it in),
	 * 2 built. Cells the foundation's build is putting in count as new (amber) too.
	 */
	static List<int[]> towerCells(TowerData data, int foundation, int foundationBlocks, int tier, int building, float progress, BitSet filled) {
		Set<Long> solid = new HashSet<>();
		Set<Long> fresh = new HashSet<>();
		if (data.foundation != null && foundation >= 1) {
			int[][] place = data.foundation.place;
			int n = foundation == 2 ? place.length : Math.min(foundationBlocks, place.length);
			for (int i = 0; i < n; i++) {
				long key = BlockPos.asLong(place[i][0], place[i][1], place[i][2]);
				solid.add(key);
				if (foundation == 1) fresh.add(key);
			}
		}
		for (int t = 1; t <= TowerData.TIERS; t++) {
			if (t > tier && t != building) {
				continue;
			}
			var td = data.tier(t);
			for (int[] c : td.clear) {
				long key = BlockPos.asLong(c[0], c[1], c[2]);
				solid.remove(key);
				fresh.remove(key);
			}
			boolean[] built = new boolean[td.place.length];
			if (t <= tier) {
				Arrays.fill(built, true);
			} else if (t == 1) {
				int n = firstTierPlaced(data, progress);
				Arrays.fill(built, 0, Math.min(n, built.length), true);
			} else {
				for (int i = filled.nextSetBit(0); i >= 0 && i < td.tiles.size(); i = filled.nextSetBit(i + 1)) {
					for (int index : td.tiles.get(i).blocks()) {
						built[index] = true;
					}
				}
			}
			for (int i = 0; i < td.place.length; i++) {
				if (!built[i]) {
					continue;
				}
				int[] b = td.place[i];
				long key = BlockPos.asLong(b[0], b[1], b[2]);
				solid.add(key);
				if (t == building && t > tier) {
					fresh.add(key);
				} else {
					fresh.remove(key);
				}
			}
		}
		Map<Long, int[]> out = new HashMap<>();
		boolean model = data.foundation != null; // the Tri-Pillar's model: the building and what stands, not the yard's ground
		for (long key : solid) {
			BlockPos p = BlockPos.of(key);
			if (model && !HoloGeometry.modelled(p.getX(), p.getY(), p.getZ())) {
				continue;
			}
			int cx = Math.floorDiv(p.getX(), TOWER_CELL), cy = Math.floorDiv(p.getY(), TOWER_CELL), cz = Math.floorDiv(p.getZ(), TOWER_CELL);
			int[] cell = out.computeIfAbsent(BlockPos.asLong(cx, cy, cz), k -> new int[] {cx, cy, cz, 0});
			if (fresh.contains(key)) {
				cell[3] = 1; // a cell shows amber if any of it went in this build
			}
		}
		return new ArrayList<>(out.values());
	}

	private static double droneSize(int tier) {
		return DroneTier.byNumber(tier).map(t -> switch (t.size()) {
			case SMALL -> 0.03;
			case MEDIUM -> 0.045;
			case LARGE -> 0.07;
		}).orElse(0.03);
	}

	/** Converts world positions to map positions (relative to the table anchor). */
	private record Mapper(BlockPos anchor, double cx, double floor, double cz, double midX, double midZ, double groundY, double scale,
			double verticalScale, double heightLimit) {
		double x(double wx) {
			return cx + (wx - midX) * scale;
		}

		double z(double wz) {
			return cz + (wz - midZ) * scale;
		}

		double y(double wy) {
			return floor + Math.max(0, Math.min(heightLimit, (wy - groundY) * verticalScale));
		}

		void rect(State s, double x0, double z0, double x1, double z1, double lift, int color) {
			add(s, x(x0), floor + lift, z(z0), x(x1), floor + lift + 0.004, z(z1), color);
		}

		void outline(State s, double x0, double z0, double x1, double z1, int color) {
			double w = 0.006;
			add(s, x(x0), floor, z(z0) - w, x(x1), floor + 0.02, z(z0) + w, color);
			add(s, x(x0), floor, z(z1) - w, x(x1), floor + 0.02, z(z1) + w, color);
			add(s, x(x0) - w, floor, z(z0), x(x0) + w, floor + 0.02, z(z1), color);
			add(s, x(x1) - w, floor, z(z0), x(x1) + w, floor + 0.02, z(z1), color);
		}

		void marker(State s, double wx, double wy, double wz, double size, int color) {
			double mx = x(wx), my = y(wy), mz = z(wz);
			add(s, mx - size, my - size, mz - size, mx + size, my + size, mz + size, color);
		}

		void drop(State s, double wx, double wy, double wz, int color) {
			double mx = x(wx), mz = z(wz);
			add(s, mx - 0.003, floor, mz - 0.003, mx + 0.003, y(wy), mz + 0.003, color);
		}
	}

	// ================================================================== the Tri-Pillar's tower hologram

	/**
	 * The tower model's scale: one model block is this many tower blocks. 1:12 (it was 1:9) keeps the control building
	 * (8.7 across) clear of the hall's view and inside the floor ring, the finished tower 22 tall.
	 */
	static final double MODEL_SCALE = 12;
	/** The model's ground, in table space: core level + 3, over the head of anyone walking under its edge. */
	static final float MODEL_BASE = 2.0f;
	/** The model turns slowly in place: one turn every 90 seconds (radians a tick). */
	static final double SPIN = 2 * Math.PI / 1800;
	private static final int GRAPHITE = 0xFF30353B, STEEL = 0xFF5B626A, AMBER_SOLID = 0xFFE39A44;


	private static float[] model(Projection p) {
		if (p.model == null) {
			p.model = HoloGeometry.mesh(p.cells);
			for (int i = 1; i < p.model.length; i += 4) {
				p.modelTop = Math.max(p.modelTop, p.model[i]); // every corner's y
			}
		}
		return p.model;
	}


	/** Truss lengths per anchor (they run to the first block in their way), looked up again now and then. */
	private static final Map<BlockPos, float[]> TRUSSES = new HashMap<>();
	/** Directions of the trusses, clockwise from north: towards the three pillar feet (A south, B north-east, C north-west). */
	private static final double[] TRUSS_ANGLES = {Math.toRadians(180), Math.atan2(15, 8), 2 * Math.PI - Math.atan2(15, 8)};

	private void towerHologram(State state, Level level, BlockPos anchor, TowerCoreBlockEntity core, DepotView view, BlockPos link, double now) {
		Projection p = modelProjection(core);
		float[] model = p == null ? new float[0] : model(p);
		state.model = model;
		state.modelBase = MODEL_BASE;
		state.spin = (float) ((now * SPIN) % (2 * Math.PI));
		float top = MODEL_BASE + (p == null ? 0 : p.modelTop) * (float) (TOWER_CELL / MODEL_SCALE);
		float table = HoloTableBlock.HEIGHT / 16f;
		double pulse = 0.85 + 0.15 * Math.sin(now * 0.11);
		// the table's lens and the light column up to the model's ground
		if (!(level.getBlockState(anchor).getBlock() instanceof io.github.jimbozoomer.jugcraft.tower.HoloCommandTableBlock)) {
			// (the Holo Command Table's whole glass field projects the model instead: HoloCommandTableRenderer)
			int lens = alpha(0xFFE8A0, (int) (0xC0 * pulse));
			state.glow.disc(0.5, table + 0.004, 0.5, 0.42, 16, lens);
			state.glow.column(0.5, table, 0.5, 0.55, MODEL_BASE, 12, alpha(0xFFB060, (int) (0x2A * pulse)));
		}
		// a scan ring sweeping up the model
		double t = (now % 240) / 240.0, scanY = MODEL_BASE + (top - MODEL_BASE) * t;
		state.glow.ring(0.5, scanY, 0.5, 5.0, 0.03, 72, alpha(0xFFC070, (int) (0x70 * Math.sin(Math.PI * t))));

		// the projector on its three trusses under the ceiling
		BlockPos c = core.getBlockPos();
		// table space: under the lantern's lamps once tier 2 has taken the roof's skylight out of the middle, else under the skylight
		float ceiling = (level.getBlockState(c.above(20)).isAir() ? 34 : 20) - 1;
		float[] lengths = trussLengths(level, anchor, c, ceiling, now);
		float chordTop = ceiling - 0.02f;
		for (int k = 0; k < 3; k++) {
			truss(state, (float) (Math.PI - TRUSS_ANGLES[k]), chordTop, lengths[k]);
		}
		state.parts.add(new Part(0.5f, chordTop, 0.5f, 0, 0, -0.8f, -0.14f, -0.8f, 0.8f, 0, 0.8f, STEEL));
		state.parts.add(new Part(0.5f, chordTop, 0.5f, 0, 0, -0.55f, -1.0f, -0.55f, 0.55f, -0.14f, 0.55f, GRAPHITE));
		state.parts.add(new Part(0.5f, chordTop, 0.5f, 0, 0, -0.58f, -0.62f, -0.58f, 0.58f, -0.52f, 0.58f, AMBER_SOLID));
		state.parts.add(new Part(0.5f, chordTop, 0.5f, 0, 0, -0.34f, -1.22f, -0.34f, 0.34f, -1.0f, 0.34f, STEEL));
		float lensY = chordTop - 1.225f;
		state.glow.disc(0.5, lensY, 0.5, 0.26, 16, alpha(0xFFF0C8, (int) (0xE0 * pulse)), true);
		// the light cone down onto the model
		int ray = alpha(0xFFB060, (int) (0x1C * pulse));
		for (int k = 0; k < 8; k++) {
			double a = k * Math.PI / 4 + now * 0.004;
			state.glow.ray(0.5, lensY, 0.5, 0.5 + Math.sin(a) * 2.2, top + 0.2, 0.5 - Math.cos(a) * 2.2, 0.03, ray);
		}

		// drones round the tower (the far ones are on the map table)
		for (DepotView.DockView dock : view.docks) {
			modelMarker(state, c, link.getX() + dock.x(), link.getY() + dock.y() + 0.3, link.getZ() + dock.z(), droneSize(dock.tier()) * 0.6, DOCKED,
					state.spin);
		}
		for (DepotView.FlightView flight : view.flights) {
			FlightPath.Pose pose = flight.path().poseAt(flight.ticksAt(now, view.syncTime));
			double wx = link.getX() + pose.x(), wy = link.getY() + pose.y(), wz = link.getZ() + pose.z();
			double dx = wx - c.getX(), dz = wz - c.getZ();
			if (dx * dx + dz * dz < 72 * 72) {
				modelMarker(state, c, wx, wy + 0.3, wz, droneSize(flight.tier()) * 0.6, pose.cable() > 0 ? CARRYING : FLYING, state.spin);
			}
		}
		ticker(state, level, c, core, view, now);
	}

	/** A drone by the tower model, where it is round the real tower, turned with the model ({@code spin}). */
	private static void modelMarker(State state, BlockPos core, double wx, double wy, double wz, double size, int color, double spin) {
		double ox = (wx - core.getX() - 0.5) / MODEL_SCALE, oz = (wz - core.getZ() - 0.5) / MODEL_SCALE;
		// as Geometry.yaw turns the model: towards +x from +z
		double x = 0.5 + ox * Math.cos(spin) + oz * Math.sin(spin), z = 0.5 - ox * Math.sin(spin) + oz * Math.cos(spin);
		double y = MODEL_BASE + Math.max(0, wy - core.getY()) / MODEL_SCALE;
		add(state, x - size, y - size, z - size, x + size, y + size, z + size, color);
	}

	private static float[] trussLengths(Level level, BlockPos anchor, BlockPos core, float ceiling, double now) {
		float[] cached = TRUSSES.get(anchor);
		if (cached != null && cached[3] == ceiling && now - cached[4] < 200 && now >= cached[4]) {
			return cached;
		}
		float[] out = {7, 7, 7, ceiling, (float) now};
		int y = core.getY() + 1 + (int) Math.floor(ceiling - 0.3);
		for (int k = 0; k < 3; k++) {
			double sx = Math.sin(TRUSS_ANGLES[k]), sz = -Math.cos(TRUSS_ANGLES[k]);
			float end = 15;
			for (double r = 1.0; r <= 16; r += 0.25) {
				BlockPos at = new BlockPos(core.getX() + (int) Math.floor(0.5 + sx * r), y, core.getZ() + (int) Math.floor(0.5 + sz * r));
				if (!level.getBlockState(at).isAir()) {
					end = (float) (r - 0.3);
					break;
				}
			}
			out[k] = Math.max(1.5f, end);
		}
		TRUSSES.put(anchor.immutable(), out);
		return out;
	}

	/**
	 * One box-section truss from the projector out to {@code length} blocks along yaw {@code yaw} (+z of its frame points
	 * out): four chords, posts and alternating diagonals on both sides, a plate where it meets the wall.
	 */
	private static void truss(State state, float yaw, float top, float length) {
		float w = 0.22f, depth = 0.46f, s = 0.035f, start = 0.8f;
		List<Part> p = state.parts;
		for (float x : new float[] {-w, w}) {
			p.add(new Part(0.5f, top, 0.5f, yaw, 0, x - s, -s * 2, start, x + s, 0, length, STEEL));
			p.add(new Part(0.5f, top, 0.5f, yaw, 0, x - s, -depth, start, x + s, -depth + s * 2, length, STEEL));
		}
		int bays = Math.max(1, Math.round((length - start) / 0.9f));
		float pitch = (length - start) / bays;
		for (int i = 0; i <= bays; i++) {
			float z = start + i * pitch;
			for (float x : new float[] {-w, w}) {
				p.add(new Part(0.5f, top, 0.5f, yaw, 0, x - s * 0.8f, -depth, z - s * 0.8f, x + s * 0.8f, 0, z + s * 0.8f, STEEL));
			}
			p.add(new Part(0.5f, top, 0.5f, yaw, 0, -w, -s * 1.6f, z - s * 0.7f, w, -s * 0.2f, z + s * 0.7f, STEEL));
			if (i < bays) {
				// the diagonal across this bay, on both sides, going down then up
				float drop = depth - s * 2, len = (float) Math.hypot(pitch, drop), angle = (float) Math.atan2(drop, pitch);
				float mid = z + pitch / 2;
				for (float x : new float[] {-w, w}) {
					p.add(diagonal(yaw, top, x, mid, depth, len, i % 2 == 0 ? angle : -angle, s));
				}
			}
		}
		p.add(new Part(0.5f, top, 0.5f, yaw, 0, -0.36f, -0.62f, length - 0.02f, 0.36f, 0.08f, length + 0.06f, GRAPHITE));
	}

	/** A diagonal brace: centred at {@code mid} along the truss and half way down it, tipped by {@code angle}. */
	private static Part diagonal(float yaw, float top, float x, float mid, float depth, float len, float angle, float s) {
		// the frame's origin sits at the brace's middle; the box runs along its own z, tipped by the pitch
		double sy = Math.sin(yaw), cy = Math.cos(yaw);
		float ox = (float) (0.5 + x * cy + mid * sy), oz = (float) (0.5 - x * sy + mid * cy);
		return new Part(ox, top - depth / 2, oz, yaw, angle, -s * 0.7f, -s * 0.7f, -len / 2, s * 0.7f, s * 0.7f, len / 2, STEEL);
	}

	/** The ticker's line round the opening: radius from the axis, table-space height (the amber slab edge at y 10). */
	private static final double TICKER_R = 14.45, TICKER_Y = 9.5, TICKER_PX = 0.07, TICKER_ADVANCE = 6 * TICKER_PX, TICKER_SPEED = 0.06;
	private static final int TICKER_TEXT = 0xFFFFE6BC, TICKER_SEP = 0xFFFFA040, TICKER_BAND = 0xC0140B05;
	/**
	 * The ring round the opening on a {@link TickerRail}: one turn clockwise from the south (the middle of video wall A),
	 * read from inside, smooth lettering on a dark band, the status text over and over.
	 */
	private static final TickerRail TICKER = TickerRail.at(TICKER_Y).arc(0.5, 0.5, TICKER_R, Math.PI, 3 * Math.PI).closed()
			.font(TICKER_ADVANCE, TICKER_PX).band(0.31, 0.025, 0).colours(TICKER_TEXT, TICKER_SEP, TICKER_BAND).speed(TICKER_SPEED).build();

	private void ticker(State state, Level level, BlockPos core, TowerCoreBlockEntity tower, DepotView view, double now) {
		Block strip = JugcraftTower.BLOCKS.get("amber_light_strip");
		if (strip == null) return;
		TICKER.draw(tickerText(tower, view), now,
				(x, z) -> edgeBuilt(level, core, strip, Math.atan2(x - 0.5, -(z - 0.5))),
				new TickerRail.Sink() {
					@Override
					public void quad(int kind, float ax, float ay, float az, float bx, float by, float bz, float cx, float cy, float cz, float dx, float dy,
							float dz, float nx, float nz, int argb) {
						state.glow.facing(ax, ay, az, bx, by, bz, cx, cy, cz, dx, dy, dz, nx, 0, nz, argb, argb, argb, argb);
					}

					@Override
					public void glyph(float x, float y, float z, float yaw, float px, char ch, int argb) {
						state.labels.add(new Label(x, y, z, yaw, px, String.valueOf(ch), argb, true));
					}
				});
	}

	/** Per core: whether the slab edge's amber is built round each half degree of the opening, and when that was read. */
	private static final Map<BlockPos, boolean[]> TICKER_EDGE = new HashMap<>();
	private static final Map<BlockPos, Long> TICKER_EDGE_READ = new HashMap<>();
	private static final int EDGE_BUCKETS = 720;

	/**
	 * Whether the ticker has its slab edge at {@code theta}: walking out from the opening at slab height (y 10), the first
	 * block met is an amber strip or a neon line (layout 21), along the ray or one a little to either side (a ray can graze a corner between two
	 * strips). Since the rotunda cut (layout 11) the edge runs all round, over the video walls too; while a tower is
	 * built or rebuilt the ticker shows only where its edge stands. Read again every two seconds.
	 */
	private static boolean edgeBuilt(Level level, BlockPos core, Block strip, double theta) {
		long now = level.getGameTime();
		boolean[] known = TICKER_EDGE.computeIfAbsent(core.immutable(), c -> new boolean[EDGE_BUCKETS * 2]);
		Long read = TICKER_EDGE_READ.get(core);
		if (read == null || now - read > 40 || now < read) {
			java.util.Arrays.fill(known, false);
			TICKER_EDGE_READ.put(core.immutable(), now);
		}
		double turn = theta / (2 * Math.PI);
		int k = (int) Math.floorMod((long) Math.floor((turn - Math.floor(turn)) * EDGE_BUCKETS), (long) EDGE_BUCKETS);
		if (!known[EDGE_BUCKETS + k]) {
			double mid = 2 * Math.PI * (k + 0.5) / EDGE_BUCKETS;
			boolean built = false;
			for (double d : new double[] {0, -0.0026, 0.0026, -0.0052, 0.0052}) {
				if (firstIsStrip(level, core, strip, mid + d)) {
					built = true;
					break;
				}
			}
			known[k] = built;
			known[EDGE_BUCKETS + k] = true;
		}
		return known[k];
	}

	private static boolean firstIsStrip(Level level, BlockPos core, Block strip, double theta) {
		double sx = Math.sin(theta), sz = -Math.cos(theta);
		BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
		for (double r = 14.6; r < 17.5; r += 0.1) {
			at.set(core.getX() + (int) Math.floor(0.5 + sx * r), core.getY() + 10, core.getZ() + (int) Math.floor(0.5 + sz * r));
			if (!level.isLoaded(at)) {
				return false;
			}
			BlockState state = level.getBlockState(at);
			if (!state.isAir()) {
				return state.is(strip) || JugcraftTower.isNeonLine(state);
			}
		}
		return false;
	}

	private static String tierName(int tier) {
		return Component.translatable("tower.jugcraft.tier." + tier).getString().toUpperCase(Locale.ROOT);
	}

	static String tickerText(TowerCoreBlockEntity core, DepotView view) {
		String sep = "   ·   ";
		StringBuilder m = new StringBuilder("TRI-PILLAR DRONE TOWER");
		int pct = Math.round(core.buildProgress() * 100);
		if (core.converting()) {
			m.append(sep).append("REBUILDING ").append(pct).append('%');
		} else {
			m.append(sep).append("TIER ").append(core.tier());
			if (core.tier() >= 1) m.append(' ').append(tierName(core.tier()));
			if (core.buildingFoundation()) {
				m.append(sep).append("BUILDING THE FOUNDATION ").append(pct).append('%');
			} else if (core.building() > 0) {
				m.append(sep).append("BUILDING T").append(core.building()).append(' ').append(tierName(core.building())).append(' ').append(pct).append('%');
			}
		}
		m.append(sep).append("FLEET ").append(view.docks.size() + view.flights.size()).append('/').append(view.maxDrones);
		m.append(sep).append(view.flights.size()).append(" OUT");
		m.append(sep).append("BATTERY ").append(view.capacity <= 0 ? 0 : view.energy * 100 / view.capacity).append('%');
		m.append(sep).append(view.missing.isEmpty() ? "SUPPLY OK" : "SUPPLY SHORT " + view.missing.size());
		m.append(sep).append("JOBS ").append(view.openTargets);
		m.append(sep);
		return m.toString();
	}

	// ================================================================== drawing

	private static int alpha(int rgb, int a) {
		return Math.max(0, Math.min(255, a)) << 24 | (rgb & 0xFFFFFF);
	}

	private static void frame(State state, double cx, double y, double cz, double half, int color, double w) {
		add(state, cx - half, y, cz - half - w, cx + half, y + 0.01, cz - half + w, color);
		add(state, cx - half, y, cz + half - w, cx + half, y + 0.01, cz + half + w, color);
		add(state, cx - half - w, y, cz - half, cx - half + w, y + 0.01, cz + half, color);
		add(state, cx + half - w, y, cz - half, cx + half + w, y + 0.01, cz + half, color);
	}

	/** A thin beam from (x0, y0, z0) to (x1, y1, z1), approximated by a few short boxes. */
	private static void beam(State state, double x0, double y0, double z0, double x1, double y1, double z1, int color) {
		int steps = 6;
		for (int i = 0; i < steps; i++) {
			double t = (i + 0.5) / steps;
			double x = x0 + (x1 - x0) * t, y = y0 + (y1 - y0) * t, z = z0 + (z1 - z0) * t;
			double w = 0.004;
			add(state, x - w, y - (y1 - y0) / steps / 2, z - w, x + w, y + (y1 - y0) / steps / 2, z + w, color);
		}
	}

	private static void add(State state, double x0, double y0, double z0, double x1, double y1, double z1, int color) {
		state.boxes.add(new float[] {(float) x0, (float) y0, (float) z0, (float) x1, (float) y1, (float) z1, Float.intBitsToFloat(color)});
	}

	@Override
	public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
		if (state.mode == MODE_DIORAMA) {
			// nothing of it in view, nothing drawn (it stands over the table, past its anchor's cell)
			TowerDiorama diorama = TowerDiorama.get();
			if (diorama != null && (camera.cullFrustum == null || camera.cullFrustum.isVisible(new net.minecraft.world.phys.AABB(state.at.getX() - 1,
					state.at.getY(), state.at.getZ() - 1, state.at.getX() + 2, state.at.getY() + TowerDiorama.HEIGHT, state.at.getZ() + 2)))) {
				diorama.submit(poseStack, collector, state.lightCoords, state.camX, state.camY, state.camZ);
			}
			return;
		}
		if (state.model != null && state.model.length > 0) {
			float[] model = state.model;
			float base = state.modelBase;
			float spin = state.spin;
			// The model turns about the table's middle (the core's centre), lit from within.
			collector.submitCustomGeometry(poseStack, SOLID, (pose, consumer) -> {
				DroneDepotRenderer.Geometry g = new DroneDepotRenderer.Geometry(pose, consumer);
				g.translate(0.5, base, 0.5).yaw(spin).translate(-0.5 / MODEL_SCALE, 0, -0.5 / MODEL_SCALE).scale(TOWER_CELL / MODEL_SCALE);
				emitLit(g, model, LightCoordsUtil.FULL_BRIGHT, 0xFF);
			});
			// A faint glow round it: the same model a hair larger, see-through and emissive.
			collector.submitCustomGeometry(poseStack, RENDER_TYPE, (pose, consumer) -> {
				DroneDepotRenderer.Geometry g = new DroneDepotRenderer.Geometry(pose, consumer);
				g.translate(0.5, base - 0.03, 0.5).yaw(spin).scale(1.012).translate(-0.5 / MODEL_SCALE, 0, -0.5 / MODEL_SCALE)
						.scale(TOWER_CELL / MODEL_SCALE);
				emitLit(g, model, LightCoordsUtil.FULL_BRIGHT, 0x2E);
			});
		}
		if (state.solid.size() > 0) {
			float[] solid = state.solid.toArray();
			collector.submitCustomGeometry(poseStack, SOLID, (pose, consumer) -> {
				emit(new DroneDepotRenderer.Geometry(pose, consumer), solid, LightCoordsUtil.FULL_BRIGHT);
			});
		}
		if (!state.parts.isEmpty()) {
			List<Part> parts = List.copyOf(state.parts);
			collector.submitCustomGeometry(poseStack, SOLID, (pose, consumer) -> {
				DroneDepotRenderer.Geometry g = new DroneDepotRenderer.Geometry(pose, consumer);
				g.light = LightCoordsUtil.FULL_BRIGHT;
				for (Part p : parts) {
					g.push();
					g.translate(p.x(), p.y(), p.z()).yaw(p.yaw()).pitch(p.pitch());
					g.color = p.color();
					g.box(p.x0(), p.y0(), p.z0(), p.x1(), p.y1(), p.z1(), DroneModel.WHITE);
					g.pop();
				}
			});
		}
		if (!state.boxes.isEmpty() || state.glow.size() > 0) {
			List<float[]> boxes = List.copyOf(state.boxes);
			float[] glow = state.glow.toArray();
			collector.submitCustomGeometry(poseStack, RENDER_TYPE, (pose, consumer) -> {
				DroneDepotRenderer.Geometry g = new DroneDepotRenderer.Geometry(pose, consumer);
				g.light = LightCoordsUtil.FULL_BRIGHT;
				for (float[] b : boxes) {
					g.color = Float.floatToRawIntBits(b[6]);
					g.box(b[0], b[1], b[2], b[3], b[4], b[5], DroneModel.WHITE);
				}
				emit(g, glow, LightCoordsUtil.FULL_BRIGHT);
			});
		}
		for (Label label : state.labels) {
			poseStack.pushPose();
			poseStack.translate(label.x(), label.y(), label.z());
			poseStack.mulPose(new Matrix4f().rotationY(label.yaw()));
			poseStack.scale(label.scale(), -label.scale(), label.scale());
			float x = label.centred() ? -font.width(label.text()) / 2f : 0;
			collector.submitText(poseStack, x, -4, Texts.of(label.text()), false, Font.DisplayMode.POLYGON_OFFSET,
					LightCoordsUtil.FULL_BRIGHT, label.color(), 0, 0);
			poseStack.popPose();
		}
	}

	/**
	 * Draws the tower model's quads with every normal up, so the world's directional shading leaves the colours baked into
	 * the mesh as they are (the model is lit from within), with alpha {@code alpha} on each colour.
	 */
	static void emitLit(DroneDepotRenderer.Geometry g, float[] quads, int light, int alpha) {
		float[] uv = DroneModel.WHITE;
		float u = (uv[0] + uv[2]) / 2, v = (uv[1] + uv[3]) / 2;
		for (int q = 0; q + HoloGeometry.Quads.STRIDE <= quads.length; q += HoloGeometry.Quads.STRIDE) {
			for (int k = 0; k < 4; k++) {
				int o = q + k * 4;
				int color = (Float.floatToRawIntBits(quads[o + 3]) & 0xFFFFFF) | alpha << 24;
				g.meshVertex(quads[o], quads[o + 1], quads[o + 2], u, v, 0, 1, 0, color, light);
			}
		}
	}

	/** Draws {@link HoloGeometry.Quads} (positions in the Geometry's frame), normals from their corners. */
	static void emit(DroneDepotRenderer.Geometry g, float[] quads, int light) {
		float[] uv = DroneModel.WHITE;
		float u = (uv[0] + uv[2]) / 2, v = (uv[1] + uv[3]) / 2;
		for (int q = 0; q + HoloGeometry.Quads.STRIDE <= quads.length; q += HoloGeometry.Quads.STRIDE) {
			float ax = quads[q], ay = quads[q + 1], az = quads[q + 2];
			float bx = quads[q + 4], by = quads[q + 5], bz = quads[q + 6];
			float cx = quads[q + 8], cy = quads[q + 9], cz = quads[q + 10];
			float ux = bx - ax, uy = by - ay, uz = bz - az, wx = cx - bx, wy = cy - by, wz = cz - bz;
			float nx = uy * wz - uz * wy, ny = uz * wx - ux * wz, nz = ux * wy - uy * wx;
			if (nx * nx + ny * ny + nz * nz < 1e-12f) {
				// a corner pair met (a fan's middle): use the other three
				float dx = quads[q + 12], dy = quads[q + 13], dz = quads[q + 14];
				ux = cx - ax; uy = cy - ay; uz = cz - az; wx = dx - cx; wy = dy - cy; wz = dz - cz;
				nx = uy * wz - uz * wy; ny = uz * wx - ux * wz; nz = ux * wy - uy * wx;
			}
			for (int k = 0; k < 4; k++) {
				int o = q + k * 4;
				g.meshVertex(quads[o], quads[o + 1], quads[o + 2], u, v, nx, ny, nz, Float.floatToRawIntBits(quads[o + 3]), light);
			}
		}
	}
}
