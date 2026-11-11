package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.jimbozoomer.jugcraft.drone.DepotDisplayBlockEntity;
import io.github.jimbozoomer.jugcraft.drone.DepotView;
import io.github.jimbozoomer.jugcraft.drone.DroneTerminalBlockEntity;
import io.github.jimbozoomer.jugcraft.drone.FlightPath;
import io.github.jimbozoomer.jugcraft.tower.RaisedFloorBlock;
import io.github.jimbozoomer.jugcraft.tower.TowerControlConsoleBlock;
import io.github.jimbozoomer.jugcraft.tower.TowerCoreBlockEntity;
import io.github.jimbozoomer.jugcraft.tower.TowerData;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * The Tower Control Console (tower/TowerControlConsoleBlock; work list item 6, approved 22 October 2026): its mesh from
 * tools/tower_control_console.py (shaded per corner, the screens' glass, LED lines, task lights and signs full bright)
 * and its live parts, all read from the depot it is pinned to and the tower it serves:
 * <ul>
 * <li>nine monitor pages: FLEET, SITE (a radar of the drones round the terminal), JOBS; TOWER, ALERTS, BAYS (every
 * hangar bay, home, out or empty); POWER (with the battery's last few minutes), SUPPLY, LINK;</li>
 * <li>the three riser clocks (the world's time of day and day number), the three annunciator modules and the TOWER
 * CONTROL switch box's eight lamps (blinking amber while a tier builds), the two desk phones' displays, the loop
 * panels' TALK key pulsing;</li>
 * <li>the floor band's step (nosing, step light, fascia) wherever its floor reaches a cell edge with nothing beyond it
 * to carry the floor on.</li>
 * </ul>
 */
public final class TowerControlConsoleRenderer implements BlockEntityRenderer<DepotDisplayBlockEntity, TowerControlConsoleRenderer.State> {
	static final String MESH_NAME = "tower_control_console";
	private static final RenderType MESH = RenderTypes.entityCutout(ConsoleMesh.TEXTURE);
	private static final RenderType OVERLAY = GlowTypes.glow(DroneDepotRenderer.TEXTURE);
	static final String[] PAGES = {"FLEET", "SITE", "JOBS", "TOWER", "ALERTS", "BAYS", "POWER", "SUPPLY", "LINK"};
	private static final String[] SWITCH_LAMPS = {"LOW PWR", "PICKUP", "PACKAGER", "FLEET FULL", "BUILDING", "LINK", "EXCH E", "EXCH C"};
	private static final String[] MODULE_LAMPS = {"PICKUP", "PACKAGER", "LINK", "LOW PWR", "FLEET FULL", "BUILDING"};
	private static final int AMBER = 0xFFFFB04A, TEXT = 0xFFE2EAF0, VALUE = 0xFFFFE2B4, RED = 0xFFFF5A4A, GREEN = 0xFF5AE678,
			DIM = 0xFF7C8A96, BAR = 0xFF141A20, TRACK = 0xFF2A323A, BLUE = 0xFF88DCFF;
	/** Font pixels across each kind of live face's height. */
	private static final float[] FACE_PX = {80, 180, 30, 80, 20, 26};
	private static final Map<BlockPos, float[]> HISTORY = new HashMap<>();

	private final Font font;

	public TowerControlConsoleRenderer(BlockEntityRendererProvider.Context context) {
		this(context.font());
	}

	TowerControlConsoleRenderer(Font font) {
		this.font = font;
	}

	record Text(int face, String s, float x, float y, int color, int align, float scale) {
	}

	public static final class State extends BlockEntityRenderState {
		Direction facing = Direction.NORTH;
		boolean anchor;
		final HoloGeometry.Quads overlay = HoloGeometry.Quads.perFrame();
		final List<Text> texts = new ArrayList<>();
		final boolean[] exposed = new boolean[64];
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public int getViewDistance() {
		return 64;
	}

	/** The console reaches eleven blocks from its master cell. */
	@Override
	public boolean shouldRenderOffScreen() {
		return true;
	}

	// ------------------------------------------------------------------ the live parts

	@Override
	public void extractRenderState(DepotDisplayBlockEntity console, State state, float partialTick, Vec3 cameraPos,
			ModelFeatureRenderer.CrumblingOverlay crumbling) {
		BlockEntityRenderState.extractBase(console, state, crumbling);
		state.facing = console.getBlockState().getValue(TowerControlConsoleBlock.FACING);
		state.anchor = console.isAnchor();
		state.overlay.clear();
		state.texts.clear();
		java.util.Arrays.fill(state.exposed, false);
		ConsoleMesh mesh = ConsoleMesh.get(MESH_NAME);
		Level level = console.getLevel();
		if (mesh == null || level == null || !state.anchor) {
			return;
		}
		double now = level.getGameTime() + partialTick;
		BlockPos link = console.terminal();
		DepotView view = link != null && level.getBlockEntity(link) instanceof DroneTerminalBlockEntity terminal ? terminal.view() : null;
		TowerCoreBlockEntity core = view != null && view.towerPos != null && level.getBlockEntity(view.towerPos) instanceof TowerCoreBlockEntity c
				? c : null;
		long day = level.getOverworldClockTime();
		Live live = new Live(state, mesh, view, core, now, day, history(console.getBlockPos(), view, level.getGameTime()), link);
		for (int i = 0; i < mesh.live.length; i++) {
			live.face(i);
		}
		BlockPos master = console.getBlockPos();
		for (int e = 0; e < Math.min(mesh.edgeCells.length, state.exposed.length); e++) {
			int[] c = mesh.edgeCells[e];
			int turns = switch (state.facing) {
				case EAST -> 1;
				case SOUTH -> 2;
				case WEST -> 3;
				default -> 0;
			};
			Direction side = new Direction[] {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST}[(c[2] + turns) % 4];
			int dx = c[0], dz = c[1];
			for (int t = 0; t < turns; t++) {
				int x = dx;
				dx = -dz;
				dz = x;
			}
			BlockPos cell = master.offset(dx, 0, dz);
			state.exposed[e] = level.isLoaded(cell.relative(side)) && RaisedFloorBlock.stepsDown(level, cell, side);
		}
	}

	/** The battery's charge every second for the last 48 seconds, per console (the POWER page's trace). */
	private static float[] history(BlockPos pos, DepotView view, long gameTime) {
		float[] h = HISTORY.computeIfAbsent(pos.immutable(), p -> new float[50]);
		long slot = gameTime / 20;
		if (view != null && view.capacity > 0 && (long) h[49] != slot) {
			System.arraycopy(h, 1, h, 0, 47);
			h[47] = (float) view.energy / view.capacity;
			h[48] = Math.min(48, h[48] + 1);
			h[49] = slot;
		}
		return h;
	}

	private static String n(long v) {
		return String.format(Locale.ROOT, "%,d", v).replace(',', ' ');
	}

	private static int alpha(int argb, double a) {
		return (int) (Math.max(0, Math.min(1, a)) * 255) << 24 | (argb & 0xFFFFFF);
	}

	private static int dim(int argb) {
		int r = (argb >> 16 & 0xFF) / 4, g = (argb >> 8 & 0xFF) / 4, b = (argb & 0xFF) / 4;
		return 0xFF000000 | r << 16 | g << 8 | b;
	}

	/** Fills one console's live parts for a frame. */
	private static final class Live {
		final State state;
		final ConsoleMesh mesh;
		final DepotView view;
		final TowerCoreBlockEntity core;
		final double now;
		final float[] history;
		final BlockPos link;
		final String clock, hhmm;
		final long dayNumber;
		final int fleet;
		final boolean building;

		Live(State state, ConsoleMesh mesh, DepotView view, TowerCoreBlockEntity core, double now, long day, float[] history, BlockPos link) {
			this.state = state;
			this.mesh = mesh;
			this.view = view;
			this.core = core;
			this.now = now;
			this.history = history;
			this.link = link;
			long t = Math.floorMod(day + 6000, 24000);
			long h = t / 1000, m = t % 1000 * 60 / 1000, s = t % 1000 * 3600 / 1000 % 60;
			this.clock = String.format(Locale.ROOT, "%02d:%02d:%02d", h, m, s);
			this.hhmm = String.format(Locale.ROOT, "%02d:%02d", h, m);
			this.dayNumber = Math.floorDiv(day, 24000) + 1;
			this.fleet = view == null ? 0 : view.docks.size() + view.flights.size();
			this.building = core != null && core.building() > 0;
		}

		void face(int i) {
			int kind = mesh.liveKind[i], id = mesh.liveId[i];
			Frame f = Frame.of(mesh, i);
			switch (kind) {
				case ConsoleMesh.MONITOR -> monitor(i, f, id);
				case ConsoleMesh.CLOCK -> {
					text(i, clock, f.width / 2, 3.5f, 0xFFFF7A28, 2, 1.7f);
					text(i, "DAY " + dayNumber + "   LOCAL", f.width / 2, 21, 0xFFC88C50, 2, 0.75f);
				}
				case ConsoleMesh.ANNUNCIATOR -> {
					for (int k = 0; k < 6; k++) {
						int c = lamp(MODULE_LAMPS[k]);
						float x0 = 6 + (k % 3) * 71, y0 = 6 + (k / 3) * 36;
						if (c != 0) {
							f.rect(state.overlay, x0 + 1, y0 + 1, x0 + 65, y0 + 30, c, 0.0015f);
						}
						text(i, MODULE_LAMPS[k], x0 + 33, y0 + 11.5f, c != 0 ? 0xFF14100A : 0xFF6A6A6A, 2, 1.0f);
					}
				}
				case ConsoleMesh.LAMP -> {
					String name = SWITCH_LAMPS[Math.max(0, Math.min(SWITCH_LAMPS.length - 1, id))];
					int c = lamp(name);
					int base = switch (name) {
						case "LOW PWR" -> RED;
						case "FLEET FULL", "BUILDING" -> AMBER;
						default -> GREEN;
					};
					f.rect(state.overlay, 0, 0, f.width, 20, c != 0 ? c : dim(base), 0.0005f);
					text(i, name, f.width / 2, 7.2f, c != 0 ? 0xFF14100A : 0xFF8A8A8A, 2, 0.6f);
				}
				case ConsoleMesh.LCD -> {
					int ink = 0xFF14202A;
					text(i, "TWR OPS  " + (4020 + id), 3, 2, ink, 0, 0.55f);
					text(i, hhmm + "   DAY " + dayNumber, 3, 9, ink, 0, 0.55f);
					text(i, "Call    Hold    Conf", 3, 19.5f, ink, 0, 0.5f);
				}
				case ConsoleMesh.KEYSET -> {
					double pulse = 0.35 + 0.35 * Math.sin(now * 0.25);
					f.rect(state.overlay, 8, 28, 66, 70, alpha(0xFFFFFF, pulse * 0.5), 0.0015f);
					if (view != null && !view.flights.isEmpty()) {
						f.rect(state.overlay, 138, 28, 196, 70, alpha(0xFFC060, 0.25 + 0.2 * Math.sin(now * 0.4)), 0.0015f);
					}
				}
				default -> {
				}
			}
		}

		/** A lamp's live colour, or 0 when it is out. */
		int lamp(String name) {
			if (view == null) {
				return name.equals("LINK") || name.equals("LOW PWR") ? 0 : 0;
			}
			double blink = 0.55 + 0.45 * Math.sin(now * 0.3);
			return switch (name) {
				case "LOW PWR" -> view.lowPower ? RED : 0;
				case "PICKUP" -> view.pickups.isEmpty() ? 0 : GREEN;
				case "PACKAGER" -> view.packager ? GREEN : 0;
				case "LINK" -> GREEN;
				case "FLEET FULL" -> fleet >= view.maxDrones && view.maxDrones > 0 ? AMBER : 0;
				case "BUILDING" -> building ? alpha(AMBER, blink) : 0;
				case "EXCH E" -> view.energy > 0 ? GREEN : 0;
				case "EXCH C" -> view.cargo != null && view.cargo.items() > 0 ? GREEN : 0;
				default -> 0;
			};
		}

		void text(int face, String s, float x, float y, int color, int align, float scale) {
			state.texts.add(new Text(face, s, x, y, color, align, scale));
		}

		void rows(int face, Frame f, String... kv) {
			for (int r = 0; r + 1 < kv.length; r += 2) {
				float y = 17 + r / 2 * 11;
				text(face, kv[r], 5, y, TEXT, 0, 1);
				text(face, kv[r + 1], f.width - 5, y, VALUE, 1, 1);
			}
		}

		void bar(Frame f, float fraction) {
			f.rect(state.overlay, 5, 72, f.width - 5, 75, TRACK, 0.0012f);
			f.rect(state.overlay, 5, 72, 5 + (f.width - 10) * Math.max(0, Math.min(1, fraction)), 75, AMBER, 0.0018f);
		}

		void monitor(int i, Frame f, int page) {
			String title = PAGES[Math.max(0, Math.min(PAGES.length - 1, page))];
			f.rect(state.overlay, 0, 0, f.width, 11.5f, BAR, 0.0010f);
			f.rect(state.overlay, 0, 11.5f, f.width, 12.3f, AMBER, 0.0014f);
			text(i, title, 5, 2.5f, 0xFFFFF0DC, 0, 1);
			text(i, clock, f.width - 5, 2.5f, DIM, 1, 0.85f);
			if (view == null) {
				text(i, "NO DEPOT LINK", f.width / 2, 36, RED, 2, 1.2f);
				text(i, "Pin it to a terminal", f.width / 2, 50, DIM, 2, 0.85f);
				return;
			}
			switch (title) {
				case "FLEET" -> {
					rows(i, f, "Drones", fleet + " / " + view.maxDrones, "Airborne", String.valueOf(view.airborne(now)), "Docked",
							String.valueOf(view.docks.size()), "Highest tier", "T" + view.maxTier);
					bar(f, view.maxDrones <= 0 ? 0 : (float) fleet / view.maxDrones);
				}
				case "JOBS" -> {
					int jobs = view.openTargets + view.covered;
					rows(i, f, "Open", n(view.openTargets), "Covered", n(view.covered), "Flying", String.valueOf(view.flights.size()),
							"Pickups", String.valueOf(view.pickups.size()));
					bar(f, jobs <= 0 ? 0 : (float) view.covered / jobs);
				}
				case "TOWER" -> {
					if (core == null) {
						rows(i, f, "Tower", "none in reach");
						return;
					}
					int b = core.building();
					rows(i, f, "Tier", core.tier() + " / " + TowerData.TIERS, "Building", b > 0 ? "tier " + b + "  " + Math.round(core.buildProgress() * 100)
							+ " %" : "-", "Bays", String.valueOf(core.layout().capacity(core.tier())), "Pads", String.valueOf(view.padCount()));
					bar(f, b > 0 ? core.buildProgress() : (float) core.tier() / TowerData.TIERS);
				}
				case "POWER" -> {
					long pct = view.capacity <= 0 ? 0 : view.energy * 100 / view.capacity;
					rows(i, f, "Battery", pct + " %", "Draw", n(view.draw()) + " JE/t", "Load", view.loadPercent() + " %");
					trace(f, 47, 69);
					bar(f, view.capacity <= 0 ? 0 : (float) view.energy / view.capacity);
				}
				case "SUPPLY" -> {
					DepotView.CargoView cargo = view.cargo;
					int items = cargo == null ? 0 : cargo.items(), occupied = cargo == null ? 0 : cargo.occupiedSlots(), total = cargo == null ? 0
							: cargo.totalSlots();
					rows(i, f, "In store", cargo != null && cargo.unlimited() ? "TEST" : n(items), "Slots", occupied + " / " + total, "Missing",
							view.missing.size() + " kinds");
					int k = 0;
					for (Map.Entry<String, Integer> e : view.missing.entrySet()) {
						if (k >= 2) {
							break;
						}
						String name = e.getKey().substring(e.getKey().indexOf(':') + 1).replace('_', ' ');
						text(i, name, 5, 50 + k * 10, AMBER, 0, 0.85f);
						text(i, n(e.getValue()), f.width - 5, 50 + k * 10, AMBER, 1, 0.85f);
						k++;
					}
					bar(f, total <= 0 ? 0 : (float) occupied / total);
				}
				case "LINK" -> rows(i, f, "Depot", view.mode.toUpperCase(Locale.ROOT), "Platform",
						view.layoutStatus().name().toLowerCase(Locale.ROOT).replace('_', ' '), "Pads", String.valueOf(view.padCount()), "Pickups",
						String.valueOf(view.pickups.size()), "Packager", view.packager ? "ONLINE" : "OFFLINE");
				case "ALERTS" -> alerts(i, f);
				case "BAYS" -> bays(i, f);
				case "SITE" -> site(i, f);
				default -> {
				}
			}
		}

		void alerts(int i, Frame f) {
			List<String> lines = new ArrayList<>();
			List<Integer> colors = new ArrayList<>();
			if (view.lowPower) {
				lines.add("LOW POWER  " + (view.capacity <= 0 ? 0 : view.energy * 100 / view.capacity) + " %");
				colors.add(RED);
			}
			if (!view.packager) {
				lines.add("NO PACKAGER");
				colors.add(RED);
			}
			if (!view.missing.isEmpty()) {
				lines.add("SHORT " + view.missing.size() + " KINDS");
				colors.add(AMBER);
			}
			if (view.pickups.isEmpty()) {
				lines.add("NO SUPPLY PICKUP");
				colors.add(AMBER);
			}
			if (building) {
				lines.add("TIER " + core.building() + " BUILDING " + Math.round(core.buildProgress() * 100) + " %");
				colors.add(AMBER);
			}
			if (fleet >= view.maxDrones && view.maxDrones > 0) {
				lines.add("FLEET FULL");
				colors.add(AMBER);
			}
			if (!view.flights.isEmpty()) {
				lines.add(view.flights.size() + " DRONES OUT");
				colors.add(GREEN);
			}
			if (lines.isEmpty()) {
				lines.add("ALL SYSTEMS NOMINAL");
				colors.add(GREEN);
			}
			for (int k = 0; k < Math.min(6, lines.size()); k++) {
				text(i, hhmm, 5, 17 + k * 10, DIM, 0, 0.85f);
				text(i, lines.get(k), 33, 17 + k * 10, colors.get(k), 0, 0.85f);
			}
		}

		void bays(int i, Frame f) {
			int home = 0, out = 0, empty = 0;
			for (int k = 0; k < view.doors.size(); k++) {
				int[] d = view.doors.get(k);
				int status = d.length > 12 ? d[12] : 0;
				if (status == 1) home++;
				else if (status == 2) out++;
				else empty++;
				if (k < 64) {
					float x0 = 6 + (k % 16) * 8.2f, y0 = 16 + (k / 16) * 8;
					int c = status == 1 ? GREEN : status == 2 ? AMBER : 0xFF3A444E;
					f.rect(state.overlay, x0, y0, x0 + 6.4f, y0 + 6.2f, c, 0.0015f);
				}
			}
			if (view.doors.isEmpty()) {
				text(i, "No hangar bays", f.width / 2, 30, DIM, 2, 1);
			}
			text(i, "Home " + home + "   Out " + out, 5, 52, TEXT, 0, 1);
			text(i, "Empty " + empty + "   Pads " + view.padCount(), 5, 63, TEXT, 0, 1);
		}

		/** The battery's charge over the last minute or so, as a line. */
		void trace(Frame f, float top, float bottom) {
			int count = (int) history[48];
			f.rect(state.overlay, 5, bottom, f.width - 5, bottom + 0.5f, TRACK, 0.0012f);
			if (count < 2) {
				return;
			}
			float dx = (f.width - 10) / 47f;
			for (int k = 48 - count; k < 47; k++) {
				float x0 = 5 + k * dx, x1 = x0 + dx;
				float y0 = bottom - (bottom - top) * history[k], y1 = bottom - (bottom - top) * history[k + 1];
				f.line(state.overlay, x0, y0, x1, y1, 1.0f, AMBER, 0.0016f);
			}
		}

		/** A radar of the site: rings every 16 blocks round the terminal, a turning sweep, docked and flying drones. */
		void site(int i, Frame f) {
			float cx = f.width / 2, cy = 45, r = 29;
			double range = 64;
			for (int ring = 1; ring <= 4; ring++) {
				float rr = r * ring / 4;
				for (int k = 0; k < 40; k++) {
					double a0 = 2 * Math.PI * k / 40, a1 = 2 * Math.PI * (k + 1) / 40;
					f.line(state.overlay, cx + (float) Math.cos(a0) * rr, cy + (float) Math.sin(a0) * rr, cx + (float) Math.cos(a1) * rr,
							cy + (float) Math.sin(a1) * rr, 0.5f, ring == 4 ? 0xC0FFB050 : 0x60FFB050, 0.0012f);
				}
			}
			double lead = now / 120.0 % 1.0 * 2 * Math.PI;
			for (int k = 0; k < 10; k++) {
				double a = lead - k * 0.06;
				f.line(state.overlay, cx, cy, cx + (float) Math.cos(a) * r, cy + (float) Math.sin(a) * r, 0.8f, alpha(0xFFC060, 0.7 * (1 - k / 10.0)),
						0.0013f);
			}
			float k = (float) (r / range);
			for (DepotView.DockView dock : view.docks) {
				float x = cx + (float) dock.x() * k, y = cy + (float) dock.z() * k;
				if (Math.hypot(x - cx, y - cy) < r) {
					f.rect(state.overlay, x - 0.8f, y - 0.8f, x + 0.8f, y + 0.8f, 0xE0F4F6F8, 0.0016f);
				}
			}
			for (DepotView.FlightView flight : view.flights) {
				FlightPath.Pose pose = flight.path().poseAt(flight.ticksAt(now, view.syncTime));
				float x = cx + (float) pose.x() * k, y = cy + (float) pose.z() * k;
				if (Math.hypot(x - cx, y - cy) < r) {
					f.rect(state.overlay, x - 1.2f, y - 1.2f, x + 1.2f, y + 1.2f, pose.cable() > 0 ? 0xF0FFC04A : BLUE, 0.0017f);
				}
			}
			text(i, "R " + (int) range, 5, 69, DIM, 0, 0.8f);
			text(i, view.flights.size() + " out", f.width - 5, 69, DIM, 1, 0.8f);
		}
	}

	/** A live face's frame: font pixels (x right, y down from its top-left corner) to the console's frame. */
	static final class Frame {
		final Vector3f tl, r, d, n;
		final float k, width;
		private static final Map<Integer, Frame> CACHE = new HashMap<>();

		static Frame of(ConsoleMesh mesh, int face) {
			return CACHE.computeIfAbsent(face, i -> new Frame(mesh.live[i], FACE_PX[Math.max(0, Math.min(FACE_PX.length - 1, mesh.liveKind[i]))]));
		}

		Frame(float[][] s, float px) {
			tl = new Vector3f(s[0][0], s[0][1], s[0][2]);
			Vector3f tr = new Vector3f(s[1][0], s[1][1], s[1][2]), bl = new Vector3f(s[3][0], s[3][1], s[3][2]);
			r = tr.sub(tl, new Vector3f());
			float w = r.length();
			r.normalize();
			d = bl.sub(tl, new Vector3f());
			float h = d.length();
			d.normalize();
			n = new Vector3f(d).cross(r).normalize();
			k = h / px;
			width = w / k;
		}

		float[] at(float x, float y, float lift) {
			return new float[] {tl.x + r.x * x * k + d.x * y * k + n.x * lift, tl.y + r.y * x * k + d.y * y * k + n.y * lift,
					tl.z + r.z * x * k + d.z * y * k + n.z * lift};
		}

		void rect(HoloGeometry.Quads q, float x0, float y0, float x1, float y1, int color, float lift) {
			float[] a = at(x0, y0, lift), b = at(x1, y0, lift), c = at(x1, y1, lift), e = at(x0, y1, lift);
			q.facing(a[0], a[1], a[2], b[0], b[1], b[2], c[0], c[1], c[2], e[0], e[1], e[2], n.x, n.y, n.z, color, color, color, color);
		}

		/** A straight stroke {@code w} font pixels wide. */
		void line(HoloGeometry.Quads q, float x0, float y0, float x1, float y1, float w, int color, float lift) {
			float dx = x1 - x0, dy = y1 - y0, l = (float) Math.hypot(dx, dy);
			if (l < 1e-4f) {
				return;
			}
			float px = -dy / l * w / 2, py = dx / l * w / 2;
			float[] a = at(x0 + px, y0 + py, lift), b = at(x1 + px, y1 + py, lift), c = at(x1 - px, y1 - py, lift), e = at(x0 - px, y0 - py, lift);
			q.facing(a[0], a[1], a[2], b[0], b[1], b[2], c[0], c[1], c[2], e[0], e[1], e[2], n.x, n.y, n.z, color, color, color, color);
		}

		Matrix4f matrix(float lift) {
			return new Matrix4f(r.x, r.y, r.z, 0, -d.x, -d.y, -d.z, 0, n.x, n.y, n.z, 0, tl.x + n.x * lift, tl.y + n.y * lift, tl.z + n.z * lift, 1);
		}
	}

	// ------------------------------------------------------------------ drawing

	@Override
	public void submit(State state, PoseStack poses, SubmitNodeCollector collector, CameraRenderState camera) {
		ConsoleMesh mesh = ConsoleMesh.get(MESH_NAME);
		if (mesh == null || !state.anchor) {
			return;
		}
		poses.pushPose();
		float rotation = switch (state.facing) {
			case EAST -> -(float) Math.PI / 2;
			case SOUTH -> (float) Math.PI;
			case WEST -> (float) Math.PI / 2;
			default -> 0;
		};
		poses.translate(0.5, 0, 0.5);
		poses.mulPose(new Matrix4f().rotationY(rotation));
		poses.translate(-0.5, 0, -0.5);
		int light = state.lightCoords;
		boolean[] exposed = state.exposed.clone();
		collector.submitCustomGeometry(poses, MESH, (pose, consumer) -> {
			DroneDepotRenderer.Geometry g = new DroneDepotRenderer.Geometry(pose, consumer);
			if (!BakedFixtures.baked(io.github.jimbozoomer.jugcraft.tower.JugcraftTower.TOWER_CONTROL_CONSOLE)) {          // else in the chunk's mesh
				ConsoleMesh.emit(g, mesh.quads[ConsoleMesh.SOLID], light, false);
				ConsoleMesh.emit(g, mesh.quads[ConsoleMesh.LIT], LightCoordsUtil.FULL_BRIGHT, true);
			}
			for (int e = 0; e < Math.min(exposed.length, mesh.edgeCells.length); e++) {
				if (exposed[e]) {
					edge(g, mesh, e, light);
				}
			}
		});
		if (state.overlay.size() > 0) {
			float[] quads = state.overlay.toArray();
			collector.submitCustomGeometry(poses, OVERLAY, (pose, consumer) -> HoloMapRenderer.emit(new DroneDepotRenderer.Geometry(pose, consumer),
					quads, LightCoordsUtil.FULL_BRIGHT));
		}
		for (Text t : state.texts) {
			Frame f = Frame.of(mesh, t.face());
			poses.pushPose();
			poses.mulPose(f.matrix(0.0028f));
			poses.scale(f.k * t.scale(), -f.k * t.scale(), f.k * t.scale());
			float w = font.width(t.s());
			float x = t.x() / t.scale() - (t.align() == 1 ? w : t.align() == 2 ? w / 2 : 0);
			collector.submitText(poses, x, t.y() / t.scale(), Texts.of(t.s()), false, Font.DisplayMode.POLYGON_OFFSET,
					LightCoordsUtil.FULL_BRIGHT, t.color(), 0, 0);
			poses.popPose();
		}
		poses.popPose();
	}

	/**
	 * The floor band's step along one exposed cell edge, as the Raised Floor draws its own: a dark fascia, the amber step
	 * light set back under the aluminium nosing, the nosing's top a little proud of the carpet.
	 */
	private static void edge(DroneDepotRenderer.Geometry g, ConsoleMesh mesh, int e, int light) {
		int[] c = mesh.edgeCells[e];
		float t0 = mesh.edgeSpans[e][0], t1 = mesh.edgeSpans[e][1];
		// the edge's ends, and the way out of the cell (all in the console's frame, facing north)
		float ax, az, bx, bz, ox, oz;
		switch (c[2]) {
			case 0 -> { ax = c[0] + t0; az = c[1]; bx = c[0] + t1; bz = c[1]; ox = 0; oz = -1; }
			case 1 -> { ax = c[0] + 1; az = c[1] + t0; bx = c[0] + 1; bz = c[1] + t1; ox = 1; oz = 0; }
			case 2 -> { ax = c[0] + t0; az = c[1] + 1; bx = c[0] + t1; bz = c[1] + 1; ox = 0; oz = 1; }
			default -> { ax = c[0]; az = c[1] + t0; bx = c[0]; bz = c[1] + t1; ox = -1; oz = 0; }
		}
		float px = 1 / 16f;
		float nose = 0.035f, back = 0.2f * px;
		// face: x0 z0 x1 z1 y0 y1 inset tile light
		face(g, mesh.edgeTiles[2], ax, az, bx, bz, ox, oz, 0, 1.6f * px, 0, light);
		face(g, mesh.edgeTiles[1], ax, az, bx, bz, ox, oz, 1.6f * px, 2.45f * px, back, LightCoordsUtil.FULL_BRIGHT);
		face(g, mesh.edgeTiles[0], ax, az, bx, bz, ox, oz, 2.45f * px, 3.1f * px, 0, light);
		// the nosing's top and underside, and the ledge under the step light
		flat(g, mesh.edgeTiles[0], ax, az, bx, bz, ox, oz, 3.1f * px, nose, true, light);
		flat(g, mesh.edgeTiles[0], ax, az, bx, bz, ox, oz, 2.45f * px, nose, false, light);
		flat(g, mesh.edgeTiles[2], ax, az, bx, bz, ox, oz, 1.6f * px, back, true, light);
	}

	/** An upright face along the edge, facing out (inset: how far back into the cell), counter-clockwise from outside. */
	private static void face(DroneDepotRenderer.Geometry g, float[] uv, float ax, float az, float bx, float bz, float ox, float oz, float y0,
			float y1, float inset, int light) {
		float sx = -ox * inset, sz = -oz * inset;
		// seen from outside (looking in, the viewer's right is (oz, -ox)): top-left, bottom-left, bottom-right, top-right
		float ux = bx - ax, uz = bz - az;
		boolean bRight = ux * oz - uz * ox > 0;
		float[][] p = bRight ? new float[][] {{ax, y1, az}, {ax, y0, az}, {bx, y0, bz}, {bx, y1, bz}}
				: new float[][] {{bx, y1, bz}, {bx, y0, bz}, {ax, y0, az}, {ax, y1, az}};
		float[][] t = {{uv[0], uv[1]}, {uv[0], uv[3]}, {uv[2], uv[3]}, {uv[2], uv[1]}};
		for (int k = 0; k < 4; k++) {
			g.meshVertex(p[k][0] + sx, p[k][1], p[k][2] + sz, t[k][0], t[k][1], ox, 0, oz, 0xFFFFFFFF, light);
		}
	}

	/** A flat strip along the edge, {@code depth} into the cell, at height y, facing up or down. */
	private static void flat(DroneDepotRenderer.Geometry g, float[] uv, float ax, float az, float bx, float bz, float ox, float oz, float y,
			float depth, boolean up, int light) {
		float ix = -ox * depth, iz = -oz * depth;
		float[][] p = {{ax, y, az}, {bx, y, bz}, {bx + ix, y, bz + iz}, {ax + ix, y, az + iz}};
		// a, b, b + in, a + in go round anticlockwise seen from above when (b - a) x (in) points up
		float ux = bx - ax, uz = bz - az;
		boolean ccwUp = uz * ix - ux * iz > 0;
		int[] order = ccwUp == up ? new int[] {0, 1, 2, 3} : new int[] {0, 3, 2, 1};
		float[][] t = {{uv[0], uv[1]}, {uv[2], uv[1]}, {uv[2], uv[3]}, {uv[0], uv[3]}};
		for (int k : order) {
			g.meshVertex(p[k][0], p[k][1], p[k][2], t[k][0], t[k][1], 0, up ? 1 : -1, 0, 0xFFFFFFFF, light);
		}
	}
}
