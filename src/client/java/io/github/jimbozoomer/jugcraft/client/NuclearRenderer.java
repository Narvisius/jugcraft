package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.nuclear.EquipmentBlockEntity;
import io.github.jimbozoomer.jugcraft.nuclear.EquipmentKind;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Draws the reactor unit's equipment in 3D from the anchor (docs/features/nuclear.md): the pressure vessel on its
 * skirt with the control-rod drive forest on its head (the drive rods rise as the rods come out; the head comes off
 * while refuelling), the steam generators with their drum, cone and walkway, the coolant pumps and their big motors,
 * the pressurizer, the turbine-generator on its deck (HP turbine, moisture separator reheaters, three LP turbines,
 * crossover pipes, generator and exciter), the condenser with its water boxes, the feedwater pump skid, the main
 * transformer with radiators, conservator and bushings, and the control console (its lamps show a trip).
 *
 * <p>Geometry is written in the equipment's own frame: x to the placer's right, y up, z away from the placer, in
 * blocks from the anchor's corner. Every face is tiled about a block to a texture so nothing looks stretched.
 * Materials are 64-pixel regions of textures/entity/nuclear_equipment.png (tools/nuclear.py EQ_MATERIALS).
 */
public class NuclearRenderer implements BlockEntityRenderer<EquipmentBlockEntity, NuclearRenderer.State> {
	public static final Identifier TEXTURE = Jugcraft.id("textures/entity/nuclear_equipment.png");
	private static final RenderType RENDER_TYPE = RenderTypes.entityCutout(TEXTURE);
	private static final float PX = 1 / 512F;

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

	private static float[] region(int col, int row) {
		return new float[] {(col * 64 + 0.5F) * PX, (row * 64 + 0.5F) * PX, (col * 64 + 63.5F) * PX, (row * 64 + 63.5F) * PX};
	}

	public NuclearRenderer(net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context context) {
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
		if (state.kind == EquipmentKind.CONTROL_CONSOLE && equipment.linked != null
				&& level.getBlockEntity(equipment.linked) instanceof EquipmentBlockEntity vessel) {
			state.tripped = vessel.core.scrammed;
		}
		state.time = (level.getGameTime() % 24_000) + partialTick;
		if (state.kind == EquipmentKind.EMERGENCY_DIESEL_GENERATOR) {
			diesel(equipment, state, level, partialTick);
		}
		Vec3 middle = Vec3.atCenterOf(pos.offset(EquipmentKind.toWorld(new BlockPos(state.kind.wide / 2, state.kind.tall / 2, state.kind.deep / 2), state.facing)));
		double reach = Math.max(state.kind.wide, Math.max(state.kind.deep, state.kind.tall)) / 2.0;
		state.detail = cameraPos.distanceTo(middle) - reach < DETAIL_DISTANCE;
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
		// Where the exhaust comes out: straight up from its roof thimble to the first open air (the stack's top).
		Direction right = state.facing.getClockWise();
		double lx = 3.5 - 0.5, lz = 0.95 - 0.5;
		BlockPos pos = e.getBlockPos();
		double x = pos.getX() + 0.5 + right.getStepX() * lx + state.facing.getStepX() * lz;
		double z = pos.getZ() + 0.5 + right.getStepZ() * lx + state.facing.getStepZ() * lz;
		if (e.smokeY == Integer.MIN_VALUE || level.getGameTime() % 200 == 0) {
			BlockPos p = BlockPos.containing(x, pos.getY() + 11.05, z);
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
				case TURBINE_GENERATOR -> turbine(f);
				case CONDENSER -> condenser(f);
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
		boolean detail = state.detail;
		collector.submitCustomGeometry(pose, RENDER_TYPE, (p, consumer) -> {
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
			} else if (state.kind == EquipmentKind.CONTROL_CONSOLE) {
				Mesh lamps = new Mesh();
				consoleLamps(new Frame(lamps, detail), state);
				lamps.replay(p, consumer, state.facing, LightCoordsUtil.FULL_BRIGHT);
			} else if (state.kind.decor()) {
				// The lit parts (screens, lamps, the glow over spent fuel), and whatever blinks.
				glowMesh(state.kind, detail).replay(p, consumer, state.facing, LightCoordsUtil.FULL_BRIGHT);
				if (detail) {
					Mesh blink = new Mesh();
					PlantDecor.blink(state.kind, new Frame(blink, true), state.time);
					PlantDecor.liveLamps(state.kind, new Frame(blink, true), state.time, state.speed, state.running);
					if (blink.vertices() > 0) blink.replay(p, consumer, state.facing, LightCoordsUtil.FULL_BRIGHT);
					// What moves while it runs, lit like the rest.
					Mesh live = new Mesh();
					PlantDecor.live(state.kind, new Frame(live, true), state.time, state.speed, state.angle, state.running);
					if (live.vertices() > 0) live.replay(p, consumer, state.facing, state.lightLevel);
				}
			}
		});
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
			Direction right = facing.getClockWise();
			float rx = right.getStepX(), rz = right.getStepZ(), fx = facing.getStepX(), fz = facing.getStepZ();
			for (int i = 0; i < size; i += 8) {
				float lx = data[i] - 0.5F, lz = data[i + 2] - 0.5F;
				float x = 0.5F + rx * lx + fx * lz, z = 0.5F + rz * lx + fz * lz;
				float nx = rx * data[i + 5] + fx * data[i + 7], nz = rz * data[i + 5] + fz * data[i + 7];
				consumer.addVertex(pose, x, data[i + 1], z).setColor(0xFFFFFFFF).setUv(data[i + 3], data[i + 4])
						.setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, nx, data[i + 6], nz);
			}
		}
	}

	/** Builds a piece's geometry into a {@link Mesh}. {@code detail} is false for the far-away version. */
	static final class Frame {
		final Mesh mesh;
		final boolean detail;
		/** Stretch applied to everything recorded (a larger version of the same piece). */
		double sx = 1, sy = 1, sz = 1;

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
			mesh.vertex((float) tl.x * ax, (float) tl.y * ay, (float) tl.z * az, u0, v0, nx, ny, nz);
			mesh.vertex((float) bl.x * ax, (float) bl.y * ay, (float) bl.z * az, u0, v1, nx, ny, nz);
			mesh.vertex((float) br.x * ax, (float) br.y * ay, (float) br.z * az, u1, v1, nx, ny, nz);
			mesh.vertex((float) tr.x * ax, (float) tr.y * ay, (float) tr.z * az, u1, v0, nx, ny, nz);
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

	static void turbine(Frame f) {
		double c = 5.5, axisY = 4.6;
		// The concrete deck and its grating walkway along the sides.
		f.box(0, 0, 0, 11, 3, 50, CONCRETE);
		f.box(0, 3, 0, 11, 3.05, 50, GRATING, GRATING, CONCRETE);
		for (int side : new int[] {0, 1}) {
			double x = side == 0 ? 0.05 : 10.95;
			f.box(x - 0.03, 3.05, 0.2, x + 0.03, 4.1, 49.8, HANDRAIL);
		}
		// Shaft line, bearing pedestals.
		f.cylinder(v(c, axisY, 1.0), v(c, axisY, 49.0), 0.25, f.sides(12), STAINLESS, true);
		for (double z : new double[] {1.5, 9.5, 18.5, 27.5, 36.5, 46.5}) {
			f.box(c - 0.9, 3.05, z - 0.4, c + 0.9, axisY - 0.2, z + 0.4, CARBON_STEEL);
		}
		// High-pressure turbine, with the main steam inlet (stop and control valve chest) on top of it.
		halfCasing(f, c, axisY, 2.5, 9.0, 1.9, TURBINE_BLUE);
		f.box(c - 0.9, axisY + 1.7, 4.6, c + 0.9, axisY + 2.6, 6.4, CARBON_STEEL);
		f.pipe(0.55, INSULATION, v(c, axisY + 2.6, 5.5), v(c, 10.0, 5.5));
		// Moisture separator reheaters either side, on saddles.
		for (double x : new double[] {1.45, 9.55}) {
			f.revolve(v(x, axisY, 3.0), v(x, axisY, 4.0), new double[][] {{0, 0.3}, {0.5, 0.95}, {26.5, 0.95}, {27, 0.3}}, f.sides(16), PAINT_GREY, true, true);
			for (double z = 5; z < 30; z += 6) {
				f.box(x - 0.7, 3.05, z - 0.2, x + 0.7, axisY - 0.6, z + 0.2, CARBON_STEEL);
			}
		}
		// Three low-pressure turbines.
		for (double z0 : new double[] {10.0, 19.0, 28.0}) {
			f.box(c - 3.3, 3.05, z0, c + 3.3, 5.6, z0 + 8.0, TURBINE_BLUE, TURBINE_BLUE, TURBINE_BLUE);
			halfCasing(f, c, 5.6, z0 + 0.4, z0 + 7.6, 2.9, TURBINE_BLUE);
			f.box(c - 3.35, 5.5, z0 - 0.05, c + 3.35, 5.7, z0 + 8.05, FLANGE);
		}
		// Crossover pipes from the HP turbine over the LP turbines.
		for (double x : new double[] {c - 1.4, c + 1.4}) {
			f.pipe(0.55, PAINT_GREY, v(x, axisY + 1.4, 7.5), v(x, 9.1, 7.5), v(x, 9.1, 32.0));
			for (double z : new double[] {14.0, 23.0, 32.0}) {
				f.cylinder(v(x, 9.1, z), v(x, 8.1, z), 0.55, f.sides(12), PAINT_GREY, false);
			}
		}
		// Generator, with its end shields and the exciter.
		f.box(c - 2.4, 3.05, 37.2, c + 2.4, axisY - 0.8, 45.8, CARBON_STEEL);
		f.revolve(v(c, axisY + 0.4, 37.0), v(c, axisY + 0.4, 38.0), new double[][] {{0, 1.6}, {0.5, 2.15}, {8.5, 2.15}, {9.0, 1.6}}, 32,
				GENERATOR, true, true);
		for (double z : new double[] {39.0, 41.5, 44.0}) {
			f.revolve(v(c, axisY + 0.4, z), v(c, axisY + 0.4, z + 1), new double[][] {{0, 2.2}, {0.25, 2.2}}, f.sides(32), PAINT_GREEN, true, true);
		}
		f.cylinder(v(c, axisY, 46.4), v(c, axisY, 48.8), 1.0, f.sides(16), PAINT_GREY, true);
		f.plate(c - 0.6, 5.0, 46.9, c + 0.6, 5.6, 46.95, 0, NAMEPLATE, PAINT_GREY);
		// Bus duct from the generator to the side, toward the main transformer.
		f.box(c + 2.0, axisY + 0.6, 40.5, 11.0, axisY + 1.4, 41.5, PAINT_GREY);
		f.box(10.2, 3.05, 40.6, 10.8, axisY + 0.6, 41.4, PAINT_GREY);
	}

	/** A turbine casing: a horizontal half-cylinder (its top) with a split flange, over a box base. */
	static void halfCasing(Frame f, double c, double y, double z0, double z1, double r, float[] uv) {
		int sides = f.sides(32);
		int repeats = Frame.repeatsFor(Math.PI * r * 2, sides);
		for (int k = 0; k < sides / 2; k++) {
			double a0 = Math.PI * k / (sides / 2.0), a1 = Math.PI * (k + 1) / (sides / 2.0);
			Vec3 d0 = v(Math.cos(a0), Math.sin(a0), 0), d1 = v(Math.cos(a1), Math.sin(a1), 0);
			double slice = repeats / (double) sides, u0 = (k * slice) % 1.0;
			float fu0 = (float) (uv[0] + (uv[2] - uv[0]) * u0), fu1 = (float) (uv[0] + (uv[2] - uv[0]) * Math.min(1, u0 + slice));
			int rows = (int) Math.ceil(z1 - z0);
			for (int row = 0; row < rows; row++) {
				double za = z0 + (z1 - z0) * row / rows, zb = z0 + (z1 - z0) * (row + 1) / rows;
				Vec3 p00 = v(c + d0.x * r, y + d0.y * r, za), p01 = v(c + d0.x * r, y + d0.y * r, zb);
				Vec3 p10 = v(c + d1.x * r, y + d1.y * r, za), p11 = v(c + d1.x * r, y + d1.y * r, zb);
				f.quad(p11, p10, p00, p01, d0.add(d1).normalize(), fu0, uv[1], fu1, uv[3]);
			}
		}
		// The two ends.
		for (int end = 0; end < 2; end++) {
			double z = end == 0 ? z0 : z1;
			Vec3 n = v(0, 0, end == 0 ? -1 : 1);
			for (int k = 0; k < sides / 2; k += 2) {
				double a0 = Math.PI * k / (sides / 2.0), a1 = Math.PI * (k + 1) / (sides / 2.0), a2 = Math.PI * (k + 2) / (sides / 2.0);
				Vec3 centre = v(c, y, z);
				Vec3 p0 = v(c + Math.cos(a0) * r, y + Math.sin(a0) * r, z), p1 = v(c + Math.cos(a1) * r, y + Math.sin(a1) * r, z);
				Vec3 p2 = v(c + Math.cos(a2) * r, y + Math.sin(a2) * r, z);
				if (end == 0) {
					f.quad(centre, p0, p1, p2, n, uv[0], uv[1], uv[2], uv[3]);
				} else {
					f.quad(centre, p2, p1, p0, n, uv[0], uv[1], uv[2], uv[3]);
				}
			}
		}
		f.box(c - r - 0.15, y - 0.1, z0 - 0.1, c + r + 0.15, y + 0.12, z1 + 0.1, FLANGE);
		f.box(c - r, 3.05, z0, c + r, y - 0.1, z1, uv);
	}

	static void condenser(Frame f) {
		// The shell with stiffening ribs, the exhaust neck on top, water boxes on both ends with the circulating water
		// nozzles, the hotwell along the bottom, and a nameplate.
		f.box(0.6, 0.4, 0.3, 11.4, 5.0, 5.7, TANK_GREY);
		for (double x = 1.6; x < 11; x += 1.5) {
			f.box(x - 0.08, 0.4, 0.22, x + 0.08, 5.0, 5.78, CARBON_STEEL);
		}
		f.box(1.5, 5.0, 1.0, 10.5, 5.95, 5.0, STAINLESS_DARK, FLANGE, STAINLESS_DARK);
		f.box(0.6, 0, 1.2, 11.4, 0.45, 4.8, CARBON_STEEL);
		for (int end = 0; end < 2; end++) {
			double x0 = end == 0 ? 0.05 : 11.4, x1 = end == 0 ? 0.6 : 11.95;
			f.box(x0, 0.6, 0.6, x1, 4.6, 5.4, PAINT_BLUE);
			double nx = end == 0 ? 0.05 : 11.95, out = end == 0 ? -0.05 : 0.05;
			for (double y : new double[] {1.5, 3.6}) {
				f.cylinder(v(nx, y, 3.0), v(nx + out * 1, y, 3.0), 0.75, f.sides(16), PAINT_BLUE, true);
			}
		}
		f.plate(5.4, 2.5, 0.25, 6.6, 3.1, 0.3, 0, NAMEPLATE, TANK_GREY);
		f.plate(2.6, 2.4, 0.25, 3.4, 3.2, 0.3, 0, HAZARD, TANK_GREY);
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

	static void console(Frame f) {
		// The desk, its sloped instrument top, the vertical board with three screens, and the SCRAM button.
		f.box(0.05, 0, 0.15, 2.95, 0.72, 1.35, PAINT_GREY, DESK, PAINT_GREY);
		Vec3 tl = v(0.05, 0.98, 1.3), bl = v(0.05, 0.72, 0.15), br = v(2.95, 0.72, 0.15), tr = v(2.95, 0.98, 1.3);
		f.tiled(tl, bl, br, tr, v(0, 0.98, -0.2).normalize(), DESK, 1.5);
		// Close the sides under the sloped top.
		f.tri(v(0.05, 0.98, 1.3), v(0.05, 0.72, 0.15), v(0.05, 0.72, 1.3), v(-1, 0, 0), PAINT_GREY);
		f.tri(v(2.95, 0.98, 1.3), v(2.95, 0.72, 1.3), v(2.95, 0.72, 0.15), v(1, 0, 0), PAINT_GREY);
		f.box(0.05, 0, 1.3, 2.95, 1.95, 1.95, PAINT_GREY);
		f.cylinder(v(2.55, 0.86, 0.6), v(2.55, 0.98, 0.6), 0.14, f.sides(12), LAMP_RED, true);
		f.box(2.35, 0.8, 0.4, 2.75, 0.84, 0.8, HAZARD);
	}

	/** The console's lit parts: three screens and the annunciator lamps (red and blinking on a trip). */
	static void consoleLamps(Frame f, State s) {
		for (int i = 0; i < 3; i++) {
			double x0 = 0.15 + i * 0.95;
			f.quad(v(x0, 1.8, 1.34), v(x0, 1.05, 1.34), v(x0 + 0.85, 1.05, 1.34), v(x0 + 0.85, 1.8, 1.34), v(0, 0, -1),
					SCREEN[0], SCREEN[1], SCREEN[2], SCREEN[3]);
		}
		boolean blink = s.tripped && ((int) (s.time / 10)) % 2 == 0;
		for (int i = 0; i < 8; i++) {
			double x0 = 0.2 + i * 0.33;
			float[] lamp = s.tripped ? (blink || i % 2 == 0 ? LAMP_RED : LAMP_GREEN) : LAMP_GREEN;
			f.quad(v(x0, 1.92, 1.34), v(x0, 1.84, 1.34), v(x0 + 0.25, 1.84, 1.34), v(x0 + 0.25, 1.92, 1.34), v(0, 0, -1),
					lamp[0], lamp[1], lamp[2], lamp[3]);
		}
	}
}
