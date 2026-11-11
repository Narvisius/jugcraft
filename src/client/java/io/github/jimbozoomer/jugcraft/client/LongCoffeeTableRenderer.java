package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.drone.DepotDisplayBlockEntity;
import io.github.jimbozoomer.jugcraft.tower.LongCoffeeTableBlock;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * The Long Coffee Table (tower/LongCoffeeTableBlock; work list item 3, approved 22 October 2026): its mesh from
 * tools/long_coffee_table.py turned to its facing (the iroko top, the runner's stainless lining, LED board and smoked glass,
 * the sled legs, the amber lines and their wash on the floor, the plant's mugs, magazines, a tablet, the model stand), a
 * 1:6 model of the T1 drone on the stand, and in the runner's two lines of amber LED dots the words Justin gave it,
 * "DRONE TOWER BROUGHT TO YOU BY NARV-TECH.", running on and on: each line reads to the sofa on its side.
 *
 * <p>Each line is a {@link TickerRail} (DOTS, one straight piece) laid flat: the rail is built standing up in its own
 * frame and the sink lays its dots onto the line's live face from the mesh (corners tl tr br bl as that side's reader
 * sees them, the band's top along tl-tr).
 */
public final class LongCoffeeTableRenderer implements BlockEntityRenderer<DepotDisplayBlockEntity, LongCoffeeTableRenderer.State> {
	static final String MESH_NAME = "long_coffee_table";
	static final Identifier TEXTURE = Jugcraft.id("textures/entity/long_coffee_table.png");
	private static final RenderType MESH = RenderTypes.entityCutout(TEXTURE);
	private static final RenderType GLASS = RenderTypes.entityTranslucentCull(TEXTURE);
	private static final RenderType GLOW = GlowTypes.glow(TEXTURE);
	private static final RenderType DOTS = RenderTypes.entityCutout(DroneDepotRenderer.TEXTURE);
	/** The lines' live kind in the mesh; their rows, LED pitch and colour; how fast they run (dot columns a tick). */
	static final int TICKER = 7, ROWS = 9, AMBER = 0xFFFFB048;
	static final double PITCH = 0.015, SPEED = 0.5;
	/** What runs along the table (Justin, 22 October 2026), over and over. */
	static final String TEXT = "DRONE TOWER BROUGHT TO YOU BY NARV-TECH.   " + TickerRail.SEPARATOR + "   ";
	/** The T1 model on its stand (tools/long_coffee_table.py MODEL_*): where it stands, its heading, its scale. */
	static final float MODEL_X = 0.10f, MODEL_Y = 0.475f, MODEL_Z = 0.33f, MODEL_SCALE = 1 / 6f;
	static final float MODEL_YAW = (float) Math.toRadians(35);

	public LongCoffeeTableRenderer(BlockEntityRendererProvider.Context context) {
	}

	public static final class State extends BlockEntityRenderState {
		boolean present;
		float facing;
		int modelLight;
		final HoloGeometry.Quads dots = HoloGeometry.Quads.perFrame();
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public int getViewDistance() {
		return 64;
	}

	/** The table reaches two blocks from its master cell. */
	@Override
	public boolean shouldRenderOffScreen() {
		return true;
	}

	/** A line of the runner: its rail (standing in its own frame) and where it lies on the table. */
	record Line(TickerRail rail, double half, double[] tl, double[] along, double[] down) {
		/** Lays the rail's point (x along it, y up it about its middle) onto the table. */
		void at(float x, float y, float[] out, int o) {
			double d = half - y;
			for (int i = 0; i < 3; i++) {
				out[o + i] = (float) (tl[i] + along[i] * x + down[i] * d);
			}
		}
	}

	private static Line[] lines;
	private static boolean linesTried;

	/** The runner's two lines from the mesh's live faces (ids 0 north, 1 south), or null. */
	static Line[] lines(ConsoleMesh mesh) {
		if (!linesTried) {
			linesTried = true;
			Line[] out = new Line[2];
			for (int i = 0; i < mesh.liveKind.length; i++) {
				int id = mesh.liveId[i];
				if (mesh.liveKind[i] != TICKER || id < 0 || id > 1) {
					continue;
				}
				float[][] f = mesh.live[i];
				double[] tl = {f[0][0], f[0][1], f[0][2]};
				double[] along = {f[1][0] - f[0][0], f[1][1] - f[0][1], f[1][2] - f[0][2]};
				double[] down = {f[3][0] - f[0][0], f[3][1] - f[0][1], f[3][2] - f[0][2]};
				double length = Math.sqrt(along[0] * along[0] + along[1] * along[1] + along[2] * along[2]);
				double height = Math.sqrt(down[0] * down[0] + down[1] * down[1] + down[2] * down[2]);
				if (length < 1e-6 || height < 1e-6) {
					continue;
				}
				for (int k = 0; k < 3; k++) {
					along[k] /= length;
					down[k] /= height;
				}
				TickerRail rail = TickerRail.at(0).line(0, 0, length, 0).dots(PITCH, ROWS).band(height / 2, 0, 0).colours(AMBER, AMBER, 0)
						.speed(SPEED).copies(0).build();
				out[id] = new Line(rail, height / 2, tl, along, down);
			}
			lines = out[0] != null && out[1] != null ? out : null;
		}
		return lines;
	}

	@Override
	public void extractRenderState(DepotDisplayBlockEntity be, State state, float partialTick, Vec3 cameraPos,
			ModelFeatureRenderer.CrumblingOverlay crumbling) {
		BlockEntityRenderState.extractBase(be, state, crumbling);
		state.dots.clear();
		BlockState bs = be.getBlockState();
		Level level = be.getLevel();
		state.present = level != null && bs.getBlock() instanceof LongCoffeeTableBlock;
		if (!state.present) {
			return;
		}
		state.facing = DroneDisplayCaseRenderer.facingYaw(bs.getValue(LongCoffeeTableBlock.FACING));
		state.modelLight = state.lightCoords;
		ConsoleMesh mesh = ConsoleMesh.get(MESH_NAME);
		Line[] ls = mesh == null ? null : lines(mesh);
		if (ls == null) {
			return;
		}
		double now = level.getGameTime() + partialTick;
		float[] p = new float[12];
		for (Line line : ls) {
			line.rail().draw(TEXT, now, null, (kind, ax, ay, az, bx, by, bz, cx, cy, cz, dx, dy, dz, nx, nz, argb) -> {
				line.at(ax, ay, p, 0);
				line.at(bx, by, p, 3);
				line.at(cx, cy, p, 6);
				line.at(dx, dy, p, 9);
				state.dots.facing(p[0], p[1], p[2], p[3], p[4], p[5], p[6], p[7], p[8], p[9], p[10], p[11], 0, 1, 0, argb, argb, argb, argb);
			});
		}
	}

	@Override
	public void submit(State state, PoseStack poses, SubmitNodeCollector collector, CameraRenderState camera) {
		ConsoleMesh mesh = ConsoleMesh.get(MESH_NAME);
		if (mesh == null || !state.present) {
			return;
		}
		poses.pushPose();
		poses.translate(0.5, 0, 0.5);
		poses.mulPose(new Matrix4f().rotationY(state.facing));
		poses.translate(-0.5, 0, -0.5);
		int light = state.lightCoords;
		collector.submitCustomGeometry(poses, MESH, (pose, consumer) -> {
			DroneDepotRenderer.Geometry g = new DroneDepotRenderer.Geometry(pose, consumer);
			ConsoleMesh.emit(g, mesh.quads[ConsoleMesh.SOLID], light, false);
			ConsoleMesh.emit(g, mesh.quads[ConsoleMesh.LIT], LightCoordsUtil.FULL_BRIGHT, true);
		});
		if (state.dots.size() > 0) {
			float[] dots = state.dots.toArray();
			collector.submitCustomGeometry(poses, DOTS, (pose, consumer) -> HoloMapRenderer.emitLit(new DroneDepotRenderer.Geometry(pose, consumer),
					dots, LightCoordsUtil.FULL_BRIGHT, 255));
		}
		int modelLight = state.modelLight;
		Retained.mark(collector);
		collector.submitCustomGeometry(poses, DroneMesh.RENDER_TYPE, (pose, consumer) -> DroneModel.draw(new DroneDepotRenderer.Geometry(pose, consumer, DroneMesh.RENDER_TYPE),
				MODEL_X, MODEL_Y, MODEL_Z, MODEL_YAW, 1, -1, 0, modelLight, 0, MODEL_SCALE));
		collector.submitCustomGeometry(poses, GLOW, (pose, consumer) -> ConsoleMesh.emit(new DroneDepotRenderer.Geometry(pose, consumer),
				mesh.quads[ConsoleMesh.GLOW], LightCoordsUtil.FULL_BRIGHT, true));
		collector.submitCustomGeometry(poses, GLASS, (pose, consumer) -> ConsoleMesh.emit(new DroneDepotRenderer.Geometry(pose, consumer),
				mesh.quads[ConsoleMesh.GLASS], light, false));
		poses.popPose();
	}
}
