package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.finance.CasinoBlock;
import io.github.jimbozoomer.jugcraft.finance.CasinoBlockEntity;
import io.github.jimbozoomer.jugcraft.finance.Derby;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The Derby Table's six miniature horses, each with a jockey in its lane's silks, running the race round the track in
 * the world for everyone nearby to watch. The race comes from the table's data (seed, finishing order and start time),
 * so every player sees the same race and the horses cross the line in the order the server drew. Between races they
 * stand behind the start, at the finish line.
 */
public class DerbyRenderer implements BlockEntityRenderer<CasinoBlockEntity, DerbyRenderer.State> {
	/** A plain pale texture, tinted per part (coat, silks, legs). */
	private static final RenderType SOLID = RenderTypes.entitySolid(Jugcraft.id("textures/block/casino_card.png"));
	private static final int[] COATS = {0xFF7A421E, 0xFF2A1A10, 0xFFB0A8A0, 0xFF8A5A2A, 0xFF4A2A14, 0xFFC8A070};
	private static final int LEGS = 0xFF1A100A;

	public static final class State extends BlockEntityRenderState {
		boolean derby;
		Direction facing = Direction.NORTH;
		Derby.Phase phase = Derby.Phase.IDLE;
		long seed;
		int[] order = new int[Derby.HORSES];
		double raceTime;
	}

	public DerbyRenderer(BlockEntityRendererProvider.Context context) {
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(CasinoBlockEntity table, State state, float partialTick, Vec3 camera,
			ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
		BlockEntityRenderState.extractBase(table, state, crumbling);
		state.derby = table.game() == CasinoBlock.Game.DERBY;
		if (!state.derby) {
			return;
		}
		state.facing = table.getBlockState().getValue(CasinoBlock.FACING);
		Derby derby = table.derby();
		state.phase = derby.phase;
		state.seed = derby.seed;
		System.arraycopy(derby.order, 0, state.order, 0, Derby.HORSES);
		long now = table.getLevel() == null ? 0 : table.getLevel().getGameTime();
		state.raceTime = (now - derby.startTime + partialTick) / (double) Derby.RACE_TICKS;
	}

	@Override
	public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
		if (!state.derby) {
			return;
		}
		pose.pushPose();
		// Turn the north-facing layout to the table's facing, as its block models are turned.
		pose.translate(0.5F, 0.0F, 0.5F);
		pose.rotateDegrees(Axis.YP, -yRotation(state.facing));
		pose.translate(-0.5F, 0.0F, -0.5F);
		int light = state.lightCoords;
		boolean running = state.phase == Derby.Phase.RACING || state.phase == Derby.Phase.RESULT;
		double t = state.phase == Derby.Phase.RESULT ? 1.2 : Math.max(0, Math.min(1.2, state.raceTime));
		int[] place = Derby.places(state.order);
		collector.submitCustomGeometry(pose, SOLID, (matrix, buffer) -> {
			for (int h = 0; h < Derby.HORSES; h++) {
				double lap;
				double gallop;
				if (running) {
					lap = Derby.progress(state.seed, h, place[h], t);
					gallop = t * Derby.RACE_TICKS * 0.55 + h;
				} else {
					// At the start, just behind the line, standing.
					lap = -0.012;
					gallop = 0;
				}
				horse(buffer, matrix, light, Math.min(lap, 1.06), h, gallop, running && lap < 1.06);
			}
		});
		pose.popPose();
	}

	/** One horse and jockey, in lane {@code lane}, {@code lap} round the track, legs at stride {@code gallop}. */
	private static void horse(VertexConsumer buffer, PoseStack.Pose matrix, int light, double lap, int lane, double gallop,
			boolean moving) {
		double[] at = Derby.lanePoint(lap, lane);
		double[] ahead = Derby.lanePoint(lap + 0.003, lane);
		double fx = ahead[0] - at[0];
		double fz = ahead[1] - at[1];
		double len = Math.hypot(fx, fz);
		if (len < 1e-6) {
			fx = 1;
			fz = 0;
		} else {
			fx /= len;
			fz /= len;
		}
		double bob = moving ? Math.abs(Math.sin(gallop * 0.9)) * 0.35 : 0;
		Frame f = new Frame(at[0], 12 + bob, at[1], fx, fz);
		int coat = COATS[lane];
		int silk = Derby.SILKS[lane];
		// Body, neck and head, tail.
		cuboid(buffer, matrix, light, f, -1.8, 1.0, -0.6, 1.8, 2.6, 0.6, coat);
		cuboid(buffer, matrix, light, f, 1.3, 2.0, -0.45, 2.3, 3.4, 0.45, coat);
		cuboid(buffer, matrix, light, f, 2.1, 2.9, -0.4, 3.3, 3.6, 0.4, coat);
		cuboid(buffer, matrix, light, f, -2.4, 1.6, -0.2, -1.8, 2.5, 0.2, LEGS);
		// The jockey: silks, and a cap.
		cuboid(buffer, matrix, light, f, -0.6, 2.6, -0.5, 0.6, 3.9, 0.5, silk);
		cuboid(buffer, matrix, light, f, -0.3, 3.9, -0.35, 0.5, 4.5, 0.35, silk);
		// Four legs, front and back pairs swinging opposite ways at a gallop.
		double swing = moving ? Math.sin(gallop) * 0.45 : 0;
		for (int side = -1; side <= 1; side += 2) {
			double z = side * 0.35;
			cuboid(buffer, matrix, light, f, 1.1 + swing, -bob, z - 0.2, 1.5 + swing, 1.0, z + 0.2, LEGS);
			cuboid(buffer, matrix, light, f, -1.5 - swing, -bob, z - 0.2, -1.1 - swing, 1.0, z + 0.2, LEGS);
		}
	}

	/**
	 * Where a horse stands and which way it runs, in the table model's pixels: x across the table, y up from the
	 * floor, z back from the near edge. Turned into the block's own space (x east, z south, in blocks) for drawing.
	 */
	private record Frame(double x, double y, double z, double fx, double fz) {
		/** A point {@code a} ahead, {@code b} up and {@code c} to the side of the horse's centre, in block space. */
		float[] point(double a, double b, double c) {
			double mx = x + fx * a - fz * c;
			double mz = z + fz * a + fx * c;
			return new float[] {(float) (mx / 16.0), (float) ((y + b) / 16.0), (float) (1.0 - mz / 16.0)};
		}
	}

	/** A box in the horse's own axes (a forward, b up, c sideways), shaded a little darker on its sides and under. */
	private static void cuboid(VertexConsumer buffer, PoseStack.Pose matrix, int light, Frame f, double a0, double b0, double c0,
			double a1, double b1, double c1, int colour) {
		float[][] p = new float[8][];
		for (int i = 0; i < 8; i++) {
			p[i] = f.point((i & 1) == 0 ? a0 : a1, (i & 2) == 0 ? b0 : b1, (i & 4) == 0 ? c0 : c1);
		}
		int side = shade(colour, 0.8);
		int under = shade(colour, 0.55);
		// Each face's corners listed anticlockwise seen from outside (indices: bit 0 forward, bit 1 up, bit 2 side).
		face(buffer, matrix, light, colour, p[2], p[6], p[7], p[3]);   // top
		face(buffer, matrix, light, under, p[0], p[1], p[5], p[4]);    // bottom
		face(buffer, matrix, light, side, p[1], p[5], p[7], p[3]);     // front
		face(buffer, matrix, light, side, p[0], p[2], p[6], p[4]);     // back
		face(buffer, matrix, light, side, p[0], p[1], p[3], p[2]);     // one side
		face(buffer, matrix, light, side, p[4], p[6], p[7], p[5]);     // the other
	}

	private static void face(VertexConsumer buffer, PoseStack.Pose matrix, int light, int colour, float[] a, float[] b, float[] c,
			float[] d) {
		// The face's normal from its corners; the quad is drawn both ways round so it shows whichever way it winds.
		float ux = b[0] - a[0];
		float uy = b[1] - a[1];
		float uz = b[2] - a[2];
		float vx = d[0] - a[0];
		float vy = d[1] - a[1];
		float vz = d[2] - a[2];
		float nx = uy * vz - uz * vy;
		float ny = uz * vx - ux * vz;
		float nz = ux * vy - uy * vx;
		float n = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
		if (n < 1e-9F) {
			return;
		}
		nx /= n;
		ny /= n;
		nz /= n;
		vertex(buffer, matrix, light, colour, a, nx, ny, nz);
		vertex(buffer, matrix, light, colour, b, nx, ny, nz);
		vertex(buffer, matrix, light, colour, c, nx, ny, nz);
		vertex(buffer, matrix, light, colour, d, nx, ny, nz);
		vertex(buffer, matrix, light, colour, d, -nx, -ny, -nz);
		vertex(buffer, matrix, light, colour, c, -nx, -ny, -nz);
		vertex(buffer, matrix, light, colour, b, -nx, -ny, -nz);
		vertex(buffer, matrix, light, colour, a, -nx, -ny, -nz);
	}

	private static void vertex(VertexConsumer buffer, PoseStack.Pose matrix, int light, int colour, float[] at, float nx, float ny,
			float nz) {
		buffer.addVertex(matrix, at[0], at[1], at[2]).setColor(colour).setUv(0.4F, 0.4F).setOverlay(OverlayTexture.NO_OVERLAY)
				.setLight(light).setNormal(matrix, nx, ny, nz);
	}

	private static int shade(int argb, double k) {
		int r = (int) (((argb >> 16) & 0xFF) * k);
		int g = (int) (((argb >> 8) & 0xFF) * k);
		int b = (int) ((argb & 0xFF) * k);
		return (argb & 0xFF000000) | r << 16 | g << 8 | b;
	}

	private static float yRotation(Direction facing) {
		return switch (facing) {
			case EAST -> 90;
			case SOUTH -> 180;
			case WEST -> 270;
			default -> 0;
		};
	}

	@Override
	public boolean shouldRenderOffScreen() {
		return true; // The track spreads over twelve blocks beyond the table's own.
	}

	@Override
	public int getViewDistance() {
		return 64;
	}
}
