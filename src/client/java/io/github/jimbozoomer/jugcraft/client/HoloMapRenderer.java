package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.jimbozoomer.jugcraft.drone.DepotDisplayBlockEntity;
import io.github.jimbozoomer.jugcraft.drone.DepotView;
import io.github.jimbozoomer.jugcraft.drone.DroneTerminalBlockEntity;
import io.github.jimbozoomer.jugcraft.drone.DroneTier;
import io.github.jimbozoomer.jugcraft.drone.FlightPath;
import io.github.jimbozoomer.jugcraft.drone.HoloTableBlock;
import io.github.jimbozoomer.jugcraft.drone.PlatformLayout;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * The hologram map over a formed hologram table: a small, see-through, glowing model of the linked
 * depot. It shows the platform, pads (with their charger ports), the supply pickup, the terminal, every
 * docked drone, and every flying drone at its real position and height (with a line down to the map
 * and a marker on each stop it is heading to), under a slowly sweeping scan bar. It is a few hundred
 * quads at most, built from the depot view the client already has.
 */
public class HoloMapRenderer implements BlockEntityRenderer<DepotDisplayBlockEntity, HoloMapRenderer.State> {
	private static final RenderType RENDER_TYPE = RenderTypes.entityTranslucentEmissive(DroneDepotRenderer.TEXTURE);
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

	public static class State extends BlockEntityRenderState {
		/** Boxes to draw: x0, y0, z0, x1, y1, z1 and colour (as float bits), relative to the anchor block. */
		final List<float[]> boxes = new ArrayList<>();
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
	public void extractRenderState(DepotDisplayBlockEntity table, State state, float partialTick, Vec3 cameraPos,
			ModelFeatureRenderer.CrumblingOverlay crumbling) {
		BlockEntityRenderState.extractBase(table, state, crumbling);
		state.boxes.clear();
		Level level = table.getLevel();
		if (level == null || !table.isAnchor()) {
			return;
		}
		double cx = 0.5, cz = 0.5, floor = HoloTableBlock.HEIGHT / 16.0 + FLOOR;
		double now = level.getGameTime() + partialTick;
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

		BlockPos link = table.terminal();
		DepotView view = link != null && level.getBlockEntity(link) instanceof DroneTerminalBlockEntity terminal ? terminal.view() : null;
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
		minX = Math.min(minX, link.getX());
		maxX = Math.max(maxX, link.getX() + 1);
		minZ = Math.min(minZ, link.getZ());
		maxZ = Math.max(maxZ, link.getZ() + 1);
		double span = Math.max(maxX - minX, maxZ - minZ) + 4;
		double scale = 2 * HALF / span;
		double midX = (minX + maxX) / 2, midZ = (minZ + maxZ) / 2;
		Mapper map = new Mapper(anchor, cx, floor, cz, midX, midZ, view.platformY + 1, scale);

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
	 * The Drone Tower as a hologram, as it really stands: the finished tiers solid, and the tier being built
	 * growing in (amber) tile by tile as the drones put them in. Nothing that isn't built yet is shown.
	 */
	private static void tower(State state, Level level, BlockPos core, Mapper map) {
		if (!(level.getBlockEntity(core) instanceof io.github.jimbozoomer.jugcraft.tower.TowerCoreBlockEntity entity)) {
			return;
		}
		int tier = entity.tier();
		int building = entity.building();
		java.util.BitSet filled = entity.filledTiles();
		int bucket = Math.round(entity.buildProgress() * 200);
		String key = core.asLong() + "/" + tier + "/" + building + "/" + bucket + "/" + filled.hashCode();
		if (!key.equals(towerKey)) {
			towerKey = key;
			towerCells = towerCells(tier, building, entity.buildProgress(), filled);
		}
		if (towerCells.isEmpty()) {
			return;
		}
		// The tower is far taller than wide: squash it so the finished tower (spire tip and all) would fit between
		// the table and the command room's ceiling. The scale stays the same as it grows.
		int top = io.github.jimbozoomer.jugcraft.tower.TowerData.get().tier(io.github.jimbozoomer.jugcraft.tower.TowerData.TIERS).top;
		double vertical = Math.min(map.scale(), TOWER_MAX_HEIGHT / top);
		for (int[] c : towerCells) {
			double wx0 = core.getX() + c[0] * TOWER_CELL, wz0 = core.getZ() + c[2] * TOWER_CELL;
			double y0 = map.floor() + 0.02 + c[1] * TOWER_CELL * vertical;
			add(state, map.x(wx0) + 0.002, y0, map.z(wz0) + 0.002, map.x(wx0 + TOWER_CELL) - 0.002,
					y0 + TOWER_CELL * vertical - 0.002, map.z(wz0 + TOWER_CELL) - 0.002, c[3] == 1 ? TOWER_BUILDING : TOWER);
		}
	}

	private static final int TOWER_CELL = 3;
	/** Table top to the command room's ceiling panels, less a hair: the spire's tip may just touch them. */
	private static final double TOWER_MAX_HEIGHT = 7.2;
	private static final int TOWER = 0x46E05A48;
	private static final int TOWER_BUILDING = 0x66FFC04A;
	private static String towerKey = "";
	private static List<int[]> towerCells = List.of();

	/**
	 * Occupied 3x3x3 cells {cx, cy, cz, building?} of the tower as built: every tier up to {@code tier}, plus what is
	 * in of {@code building} (tier 1 by the core's own progress, higher tiers by the tiles the drones have filled).
	 */
	static List<int[]> towerCells(int tier, int building, float progress, java.util.BitSet filled) {
		var data = io.github.jimbozoomer.jugcraft.tower.TowerData.get();
		java.util.Set<Long> solid = new java.util.HashSet<>();
		java.util.Set<Long> fresh = new java.util.HashSet<>();
		for (int t = 1; t <= io.github.jimbozoomer.jugcraft.tower.TowerData.TIERS; t++) {
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
				java.util.Arrays.fill(built, true);
			} else if (t == 1) {
				int n = Math.round(td.place.length * Math.max(0, Math.min(1, progress)));
				java.util.Arrays.fill(built, 0, Math.min(n, built.length), true);
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
		java.util.Map<Long, int[]> out = new java.util.HashMap<>();
		for (long key : solid) {
			BlockPos p = BlockPos.of(key);
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
	private record Mapper(BlockPos anchor, double cx, double floor, double cz, double midX, double midZ, double groundY, double scale) {
		double x(double wx) {
			return cx + (wx - midX) * scale;
		}

		double z(double wz) {
			return cz + (wz - midZ) * scale;
		}

		double y(double wy) {
			return floor + Math.max(0, Math.min(MAX_HEIGHT, (wy - groundY) * scale * 1.6));
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
		if (state.boxes.isEmpty()) {
			return;
		}
		List<float[]> boxes = List.copyOf(state.boxes);
		collector.submitCustomGeometry(poseStack, RENDER_TYPE, (pose, consumer) -> {
			DroneDepotRenderer.Geometry g = new DroneDepotRenderer.Geometry(pose, consumer);
			g.light = LightCoordsUtil.FULL_BRIGHT;
			for (float[] b : boxes) {
				g.color = Float.floatToRawIntBits(b[6]);
				g.box(b[0], b[1], b[2], b[3], b[4], b[5], DroneModel.WHITE);
			}
		});
	}
}
