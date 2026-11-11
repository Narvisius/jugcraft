package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.jimbozoomer.jugcraft.drone.DepotDisplayBlockEntity;
import io.github.jimbozoomer.jugcraft.drone.DepotView;
import io.github.jimbozoomer.jugcraft.drone.DroneTerminalBlockEntity;
import io.github.jimbozoomer.jugcraft.drone.FlightPath;
import io.github.jimbozoomer.jugcraft.tower.TowerCoreBlockEntity;
import io.github.jimbozoomer.jugcraft.tower.TowerData;
import java.util.Locale;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * The Holo Command Table (tower/HoloCommandTableBlock; work list item 5, approved 22 October 2026): its mesh, lit by
 * the world, with its LED lines, lenses and the projection field full bright and the toe kick's wash glowing; over the
 * field the live site plan at 1:12 (the control building's hexagon, the pillars once tier 2 stands, the roof pads,
 * every drone within the site, a radar sweep turning every six seconds); the five touch screens round the south half
 * (POWER, SUPPLY, FLEET, JOBS, TOWER) with their status lamps, all read from the depot the table is pinned to. Over the
 * Tower Core it also shows the tower hologram, the projector and the ticker as the central holo table did
 * ({@link HoloMapRenderer}); set anywhere else it shows its own field and screens only.
 */
public class HoloCommandTableRenderer extends HoloMapRenderer {
	private static final RenderType MESH_SOLID = RenderTypes.entityCutout(HoloCommandTableMesh.TEXTURE);
	private static final RenderType MESH_GLOW = GlowTypes.glow(HoloCommandTableMesh.TEXTURE);
	private static final RenderType OVERLAY = GlowTypes.glow(DroneDepotRenderer.TEXTURE);
	/** The screens' glass on the table's texture (tools/holo_command_table.py: the "screen" tile). */
	private static final float[] SCREEN_UV = {(640 + 2) / 1024f, (64 + 2) / 512f, (704 - 2) / 1024f, (128 - 2) / 512f};
	private static final String[] TITLES = {"POWER", "SUPPLY", "FLEET", "JOBS", "TOWER"};
	/** The screens' text: font pixels across the screen's height. */
	private static final float SCREEN_PX = 64;
	private static final int AMBER = 0xFFFFB04A, TEXT = 0xFFE2EAF0, VALUE = 0xFFFFE2B4, RED = 0xFFFF5A4A, GREEN = 0xFF5AE678;
	/** The pillar feet on the plan (tools/tripillar_geometry.py at tier 2: centres, half width, chamfer). */
	private static final int[][] PILLARS = {{0, 17}, {15, -8}, {-15, -8}};
	private static final int PILLAR_HW = 9, PILLAR_CH = 3, HEX_R = 52;
	private static final double PLAN_SCALE = 1 / 12.0;

	public static final class TableState extends State {
		/** Each screen's rows ({label, value} pairs) and bar (0 to 1, or -1 for none), and its lamp's colour. */
		final String[][] rows = new String[5][];
		final float[] bars = new float[5];
		final int[] lamps = new int[5];
		/** The field's live plan and the screens' bars and lamps (glowing quads in the table's frame). */
		final HoloGeometry.Quads plan = HoloGeometry.Quads.perFrame();
		boolean meshOk;
	}

	private final Font tableFont;

	public HoloCommandTableRenderer(BlockEntityRendererProvider.Context context) {
		super(context);
		this.tableFont = context.font();
	}

	@Override
	public State createRenderState() {
		return new TableState();
	}

	@Override
	public void extractRenderState(DepotDisplayBlockEntity table, State base, float partialTick, Vec3 cameraPos,
			ModelFeatureRenderer.CrumblingOverlay crumbling) {
		super.extractRenderState(table, base, partialTick, cameraPos, crumbling);
		TableState state = (TableState) base;
		state.plan.clear();
		for (int i = 0; i < 5; i++) {
			state.rows[i] = null;
			state.bars[i] = -1;
			state.lamps[i] = 0;
		}
		HoloCommandTableMesh mesh = HoloCommandTableMesh.get();
		state.meshOk = mesh != null;
		Level level = table.getLevel();
		if (level == null || !table.isAnchor() || mesh == null) {
			return;
		}
		if (state.mode != MODE_TOWER) {
			// the small depot map and the topographic map are the 3x3 tables' pictures: this table shows its own
			state.boxes.clear();
			state.glow.clear();
			state.solid.clear();
			state.parts.clear();
			state.labels.clear();
			state.model = null;
		}
		double now = level.getGameTime() + partialTick;
		BlockPos link = table.terminal();
		DepotView view = link != null && level.getBlockEntity(link) instanceof DroneTerminalBlockEntity terminal ? terminal.view() : null;
		TowerCoreBlockEntity core = view != null && view.towerPos != null && level.getBlockEntity(view.towerPos) instanceof TowerCoreBlockEntity c
				? c : null;
		plan(state, mesh, table.getBlockPos(), core, view, link, now);
		screens(state, mesh, core, view, now);
	}

	// ------------------------------------------------------------------ the field

	private static int alpha(int rgb, int a) {
		return Math.max(0, Math.min(255, a)) << 24 | (rgb & 0xFFFFFF);
	}

	/** A point of the tower (blocks from the core's corner, continuous) on the field. */
	private static double px(double dx) {
		return 0.5 + (dx - 0.5) * PLAN_SCALE;
	}

	private static void plan(TableState state, HoloCommandTableMesh mesh, BlockPos anchor, TowerCoreBlockEntity core, DepotView view, BlockPos link,
			double now) {
		HoloGeometry.Quads q = state.plan;
		float y = mesh.fieldY + 0.004f;
		double r = mesh.fieldR - 0.02;
		double pulse = 0.8 + 0.2 * Math.sin(now * 0.08);
		// the rim of the field, softly pulsing
		q.ring(0.5, y, 0.5, r - 0.01, 0.025, 96, alpha(0xFFB050, (int) (0x90 * pulse)));
		// the radar sweep: a wedge fading behind its leading edge, one turn every six seconds
		double lead = (now / 120.0) % 1.0 * 2 * Math.PI;
		int slices = 24;
		double span = Math.toRadians(50);
		for (int k = 0; k < slices; k++) {
			double a0 = lead - span * (k + 1) / slices, a1 = lead - span * k / slices, am = (a0 + a1) / 2;
			int a = (int) (0x58 * Math.pow(1 - k / (double) slices, 1.6));
			int c = alpha(0xFFB860, a), rim = alpha(0xFFB860, a / 2);
			q.facing(0.5f, y + 0.001f, 0.5f, (float) (0.5 + Math.sin(a0) * r), y + 0.001f, (float) (0.5 - Math.cos(a0) * r),
					(float) (0.5 + Math.sin(am) * r), y + 0.001f, (float) (0.5 - Math.cos(am) * r), (float) (0.5 + Math.sin(a1) * r), y + 0.001f,
					(float) (0.5 - Math.cos(a1) * r), 0, 1, 0, c, rim, rim, rim);
		}
		q.ribbon(0.5, y + 0.002, 0.5, 0.5 + Math.sin(lead) * r, y + 0.002, 0.5 - Math.cos(lead) * r, 0.018, 0xC0FFD090);
		if (core == null || core.layout().foundation == null) {
			return;
		}
		// the control building's hexagon (vertices east, west and at 60 degrees), round the core's middle
		double hr = HEX_R * PLAN_SCALE;
		for (int k = 0; k < 6; k++) {
			double a0 = Math.PI / 6 + k * Math.PI / 3, a1 = a0 + Math.PI / 3;
			q.ribbon(0.5 + Math.sin(a0) * hr, y + 0.003, 0.5 - Math.cos(a0) * hr, 0.5 + Math.sin(a1) * hr, y + 0.003, 0.5 - Math.cos(a1) * hr,
					0.03, alpha(0xFFBA60, (int) (0xE0 * pulse)));
		}
		// the pillars: bright once tier 2 stands, faint before
		int tier = core.tier();
		int pillar = tier >= 2 ? 0xD0FFB45A : 0x50FFB45A;
		for (int[] c : PILLARS) {
			double x0 = c[0] - PILLAR_HW, x1 = c[0] + PILLAR_HW + 1, z0 = c[1] - PILLAR_HW, z1 = c[1] + PILLAR_HW + 1, ch = PILLAR_CH;
			double[][] pts = {{x0 + ch, z0}, {x1 - ch, z0}, {x1, z0 + ch}, {x1, z1 - ch}, {x1 - ch, z1}, {x0 + ch, z1}, {x0, z1 - ch}, {x0, z0 + ch}};
			for (int k = 0; k < pts.length; k++) {
				double[] a = pts[k], b = pts[(k + 1) % pts.length];
				q.ribbon(px(a[0]), y + 0.003, px(a[1]), px(b[0]), y + 0.003, px(b[1]), 0.022, pillar);
			}
			if (tier >= 2) {
				double cx = px(c[0] + 0.5), cz = px(c[1] + 0.5), h = PILLAR_HW * PLAN_SCALE * 0.9;
				q.facing((float) (cx - h), y + 0.002f, (float) (cz - h), (float) (cx + h), y + 0.002f, (float) (cz - h), (float) (cx + h), y + 0.002f,
						(float) (cz + h), (float) (cx - h), y + 0.002f, (float) (cz + h), 0, 1, 0, 0x30FFA040, 0x30FFA040, 0x30FFA040, 0x30FFA040);
			}
		}
		// the roof pads' docks once tier 1 is up
		TowerData data = core.layout();
		BlockPos at = core.getBlockPos();
		if (tier >= 1) {
			for (TowerData.Dock d : data.tier(1).docks) {
				double cx = px(d.x()), cz = px(d.z()), h = 0.035;
				q.facing((float) (cx - h), y + 0.003f, (float) (cz - h), (float) (cx + h), y + 0.003f, (float) (cz - h), (float) (cx + h), y + 0.003f,
						(float) (cz + h), (float) (cx - h), y + 0.003f, (float) (cz + h), 0, 1, 0, 0x90FFD49A, 0x90FFD49A, 0x90FFD49A, 0x90FFD49A);
			}
		}
		if (view == null || link == null) {
			return;
		}
		// the drones within the site: docked white, flying blue, carrying amber
		double reach = mesh.fieldR / PLAN_SCALE - 2;
		for (DepotView.DockView dock : view.docks) {
			double dx = link.getX() + dock.x() - at.getX(), dz = link.getZ() + dock.z() - at.getZ();
			if (Math.hypot(dx - 0.5, dz - 0.5) < reach) {
				q.disc(px(dx), y + 0.005, px(dz), 0.045, 8, 0xE0F4F6F8);
			}
		}
		for (DepotView.FlightView flight : view.flights) {
			FlightPath.Pose pose = flight.path().poseAt(flight.ticksAt(now, view.syncTime));
			double dx = link.getX() + pose.x() - at.getX(), dz = link.getZ() + pose.z() - at.getZ();
			if (Math.hypot(dx - 0.5, dz - 0.5) < reach) {
				q.disc(px(dx), y + 0.006, px(dz), 0.06, 10, pose.cable() > 0 ? 0xF0FFC04A : 0xF088DCFF);
			}
		}
	}

	// ------------------------------------------------------------------ the five screens

	private static String n(long v) {
		return String.format(Locale.ROOT, "%,d", v).replace(',', ' ');
	}

	private static void screens(TableState state, HoloCommandTableMesh mesh, TowerCoreBlockEntity core, DepotView view, double now) {
		if (view == null) {
			for (int i = 0; i < 5; i++) {
				state.rows[i] = new String[] {"NO DEPOT LINK", ""};
				state.lamps[i] = RED;
			}
		} else {
			long pct = view.capacity <= 0 ? 0 : view.energy * 100 / view.capacity;
			state.rows[0] = new String[] {"Battery", pct + " %", "Draw", n(view.draw()) + " JE/t", "Input", view.loadPercent() + " %"};
			state.bars[0] = view.capacity <= 0 ? 0 : (float) view.energy / view.capacity;
			state.lamps[0] = view.lowPower ? RED : GREEN;
			DepotView.CargoView cargo = view.cargo;
			int items = cargo == null ? 0 : cargo.items(), occupied = cargo == null ? 0 : cargo.occupiedSlots(), total = cargo == null ? 0 : cargo.totalSlots();
			state.rows[1] = new String[] {"In store", cargo != null && cargo.unlimited() ? "TEST" : n(items), "Slots", occupied + " / " + total, "Missing",
					view.missing.size() + " kinds"};
			state.bars[1] = total <= 0 ? 0 : (float) occupied / total;
			state.lamps[1] = !view.packager ? RED : view.missing.isEmpty() ? GREEN : AMBER;
			int out = view.flights.size(), docked = view.docks.size(), fleet = out + docked;
			state.rows[2] = new String[] {"Drones", fleet + " / " + view.maxDrones, "Out", String.valueOf(out), "Docked", String.valueOf(docked)};
			state.bars[2] = view.maxDrones <= 0 ? 0 : (float) fleet / view.maxDrones;
			state.lamps[2] = fleet == 0 ? AMBER : GREEN;
			int jobs = view.openTargets + view.covered;
			state.rows[3] = new String[] {"Open", n(view.openTargets), "Covered", n(view.covered), "Flying", String.valueOf(out)};
			state.bars[3] = jobs <= 0 ? -1 : (float) view.covered / jobs;
			state.lamps[3] = view.openTargets > 0 ? AMBER : GREEN;
			if (core != null) {
				int building = core.building();
				state.rows[4] = new String[] {"Tier", core.tier() + " / " + TowerData.TIERS, "Building",
						building > 0 ? "tier " + building + "  " + Math.round(core.buildProgress() * 100) + " %" : "-", "Bays",
						String.valueOf(core.layout().capacity(core.tier()))};
				state.bars[4] = building > 0 ? core.buildProgress() : (float) core.tier() / TowerData.TIERS;
				state.lamps[4] = building > 0 ? AMBER : GREEN;
			} else {
				state.rows[4] = new String[] {"Tower", "none", "", "", "", ""};
				state.lamps[4] = AMBER;
			}
		}
		// the bars, the title rules and the lamps: glowing quads on each screen's plane
		for (int i = 0; i < Math.min(5, mesh.screens.length); i++) {
			float[][] s = mesh.screens[i];
			Frame f = new Frame(s);
			f.rect(state.plan, 3, 12.5f, f.width - 3, 13.3f, AMBER, 0.0015f);
			if (state.bars[i] >= 0) {
				f.rect(state.plan, 4, 56, f.width - 4, 59, 0xFF2A323A, 0.0012f);
				f.rect(state.plan, 4, 56, 4 + (f.width - 8) * Math.max(0, Math.min(1, state.bars[i])), 59, AMBER, 0.0018f);
			}
			int lamp = state.lamps[i];
			if (lamp != 0) {
				double blink = lamp == AMBER ? 0.6 + 0.4 * Math.sin(now * 0.3) : 1;
				float[] p = s[4];
				float h = 0.016f;
				Vector3f r = f.r, d = f.d, nn = f.n;
				int c = alpha(lamp, (int) (0xFF * blink));
				state.plan.facing(p[0] - r.x * h - d.x * h + nn.x * 0.002f, p[1] - r.y * h - d.y * h + nn.y * 0.002f, p[2] - r.z * h - d.z * h + nn.z * 0.002f,
						p[0] + r.x * h - d.x * h + nn.x * 0.002f, p[1] + r.y * h - d.y * h + nn.y * 0.002f, p[2] + r.z * h - d.z * h + nn.z * 0.002f,
						p[0] + r.x * h + d.x * h + nn.x * 0.002f, p[1] + r.y * h + d.y * h + nn.y * 0.002f, p[2] + r.z * h + d.z * h + nn.z * 0.002f,
						p[0] - r.x * h + d.x * h + nn.x * 0.002f, p[1] - r.y * h + d.y * h + nn.y * 0.002f, p[2] - r.z * h + d.z * h + nn.z * 0.002f,
						nn.x, nn.y, nn.z, c, c, c, c);
			}
		}
	}

	/** A screen's frame: font pixels (x right, y down from its top-left corner) to the table's frame. */
	private static final class Frame {
		final Vector3f tl, r, d, n;
		final float k, width;

		Frame(float[][] s) {
			tl = new Vector3f(s[0][0], s[0][1], s[0][2]);
			Vector3f tr = new Vector3f(s[1][0], s[1][1], s[1][2]), bl = new Vector3f(s[3][0], s[3][1], s[3][2]);
			r = tr.sub(tl, new Vector3f());
			float w = r.length();
			r.normalize();
			d = bl.sub(tl, new Vector3f());
			float h = d.length();
			d.normalize();
			n = new Vector3f(d).cross(r).normalize();
			k = h / SCREEN_PX;
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

		Matrix4f matrix(float lift) {
			return new Matrix4f(r.x, r.y, r.z, 0, -d.x, -d.y, -d.z, 0, n.x, n.y, n.z, 0, tl.x + n.x * lift, tl.y + n.y * lift, tl.z + n.z * lift, 1);
		}
	}

	// ------------------------------------------------------------------ drawing

	@Override
	public void submit(State base, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
		super.submit(base, poseStack, collector, camera);
		TableState state = (TableState) base;
		HoloCommandTableMesh mesh = HoloCommandTableMesh.get();
		if (mesh == null) {
			return;
		}
		int light = state.lightCoords;
		collector.submitCustomGeometry(poseStack, MESH_SOLID, (pose, consumer) -> {
			DroneDepotRenderer.Geometry g = new DroneDepotRenderer.Geometry(pose, consumer);
			emitMesh(g, mesh.quads[HoloCommandTableMesh.SOLID], light, false);
			emitMesh(g, mesh.quads[HoloCommandTableMesh.LIT], LightCoordsUtil.FULL_BRIGHT, true);
			for (float[][] s : mesh.screens) {
				// the screens' glass, full bright (counter-clockwise as its user sees it: top-left, bottom-left, bottom-right, top-right)
				int[] order = {0, 3, 2, 1};
				float[][] uv = {{SCREEN_UV[0], SCREEN_UV[1]}, {SCREEN_UV[2], SCREEN_UV[1]}, {SCREEN_UV[2], SCREEN_UV[3]}, {SCREEN_UV[0], SCREEN_UV[3]}};
				for (int k : order) {
					g.meshVertex(s[k][0], s[k][1], s[k][2], uv[k][0], uv[k][1], 0, 1, 0, 0xFFFFFFFF, LightCoordsUtil.FULL_BRIGHT);
				}
			}
		});
		if (mesh.quads[HoloCommandTableMesh.GLOW].length > 0) {
			collector.submitCustomGeometry(poseStack, MESH_GLOW, (pose, consumer) -> emitMesh(new DroneDepotRenderer.Geometry(pose, consumer),
					mesh.quads[HoloCommandTableMesh.GLOW], LightCoordsUtil.FULL_BRIGHT, true));
		}
		if (state.plan.size() > 0) {
			float[] plan = state.plan.toArray();
			collector.submitCustomGeometry(poseStack, OVERLAY, (pose, consumer) -> emit(new DroneDepotRenderer.Geometry(pose, consumer), plan,
					LightCoordsUtil.FULL_BRIGHT));
		}
		for (int i = 0; i < Math.min(5, mesh.screens.length); i++) {
			String[] rows = state.rows[i];
			if (rows == null) {
				continue;
			}
			Frame f = new Frame(mesh.screens[i]);
			poseStack.pushPose();
			poseStack.mulPose(f.matrix(0.0025f));
			poseStack.scale(f.k, -f.k, f.k);
			text(poseStack, collector, TITLES[i], 4, 3, AMBER, false, f.width);
			for (int row = 0; row + 1 < rows.length; row += 2) {
				float y = 18 + row / 2 * 12;
				text(poseStack, collector, rows[row], 4, y, TEXT, false, f.width);
				text(poseStack, collector, rows[row + 1], f.width - 4, y, VALUE, true, f.width);
			}
			poseStack.popPose();
		}
	}

	/** Text in font pixels from the screen's top-left corner (x right, y down); right-aligned text ends at x. */
	private void text(PoseStack poseStack, SubmitNodeCollector collector, String s, float x, float y, int color, boolean right, float width) {
		if (s == null || s.isEmpty()) {
			return;
		}
		float w = tableFont.width(s);
		float at = right ? x - w : x;
		collector.submitText(poseStack, at, y, Texts.of(s), false, Font.DisplayMode.POLYGON_OFFSET,
				LightCoordsUtil.FULL_BRIGHT, color, 0, 0);
	}

	/** The mesh's corners {x, y, z, u, v, nx, ny, nz}; lit faces take an up normal so the world's shading leaves them bright. */
	private static void emitMesh(DroneDepotRenderer.Geometry g, float[] corners, int light, boolean lit) {
		for (int o = 0; o + HoloCommandTableMesh.CORNER <= corners.length; o += HoloCommandTableMesh.CORNER) {
			if (lit) {
				g.meshVertex(corners[o], corners[o + 1], corners[o + 2], corners[o + 3], corners[o + 4], 0, 1, 0, 0xFFFFFFFF, light);
			} else {
				g.meshVertex(corners[o], corners[o + 1], corners[o + 2], corners[o + 3], corners[o + 4], corners[o + 5], corners[o + 6], corners[o + 7],
						0xFFFFFFFF, light);
			}
		}
	}
}
