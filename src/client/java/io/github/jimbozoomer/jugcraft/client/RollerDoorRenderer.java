package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.client.DroneDepotRenderer.Geometry;
import io.github.jimbozoomer.jugcraft.nuclear.RollerDoorBlock;
import io.github.jimbozoomer.jugcraft.nuclear.RollerDoorBlockEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The roller door in motion. While a door block's curtain moves, its block model is empty and this draws the part of
 * the curtain in its block: the curtain's bottom edge rises (or falls) through the opening at a steady speed and its
 * slats slide with it. The top block of every column also draws the coil on its barrel over the opening, on the
 * inside, fat when the door is rolled up and thin when it is down, with its end brackets.
 */
public class RollerDoorRenderer implements BlockEntityRenderer<RollerDoorBlockEntity, RollerDoorRenderer.State> {
	private static final Identifier SLATS = Jugcraft.id("textures/block/pi_roller_slats.png");
	private static final RenderType RENDER_TYPE = RenderTypes.entityCutout(SLATS);
	private static final float[] ALL = {0, 0, 1, 1};
	/** A strip of the slats picture: the dark gaps between slats read as the coil's wound turns. */
	private static final float[] BRACKET = {0.0F, 0.0F, 0.06F, 0.06F};

	public static final class State extends BlockEntityRenderState {
		Direction facing = Direction.NORTH;
		boolean moving, top;
		int row, rows;
		float openness;
	}

	public RollerDoorRenderer(BlockEntityRendererProvider.Context context) {
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(RollerDoorBlockEntity door, State state, float partialTick, Vec3 camera,
			ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
		BlockEntityRenderState.extractBase(door, state, crumbling);
		BlockState block = door.getBlockState();
		state.facing = block.getValue(RollerDoorBlock.FACING);
		state.moving = block.getValue(RollerDoorBlock.MOVING);
		int[] column = RollerDoorBlock.column(door.getLevel(), door.getBlockPos(), block);
		state.row = door.getBlockPos().getY() - column[0];
		state.rows = column[1] - column[0] + 1;
		state.top = door.getBlockPos().getY() == column[1];
		long time = door.getLevel() == null ? 0 : door.getLevel().getGameTime();
		state.openness = door.openness(time, partialTick);
	}

	@Override
	public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
		if (!state.moving && !state.top) {
			return;
		}
		pose.pushPose();
		// Model space: the door faces north (the way in), its curtain in the plane z 6.5 .. 9.5, the coil on the north side.
		pose.translate(0.5F, 0.0F, 0.5F);
		pose.rotateDegrees(Axis.YP, -yRotation(state.facing));
		pose.translate(-0.5F, 0.0F, -0.5F);
		pose.scale(1 / 16F, 1 / 16F, 1 / 16F);
		int light = state.lightCoords;
		collector.submitCustomGeometry(pose, RENDER_TYPE, (p, consumer) -> {
			Geometry g = new Geometry(p, consumer);
			g.light = light;
			if (state.moving) {
				curtain(g, state);
			}
			if (state.top) {
				coil(g, state.openness);
			}
		});
		pose.popPose();
	}

	/** The curtain in this block: from its bottom edge (if that is in this block) to the top, the slats slid up with it. */
	private static void curtain(Geometry g, State state) {
		// The curtain's bottom edge, in blocks above the bottom of the opening.
		double edge = state.openness * state.rows;
		double from = Math.max(0, edge - state.row);
		if (from >= 1) {
			return;
		}
		// The picture repeats every block; it is shifted with the curtain, so cut the span where the picture wraps.
		double shift = edge - Math.floor(edge);
		double y = from;
		while (y < 1 - 1e-6) {
			double phase = (y - shift) - Math.floor(y - shift);           // where in the picture this height is (0 bottom)
			double next = Math.min(1, y + (1 - phase));
			float v0 = (float) (1 - (phase + (next - y))), v1 = (float) (1 - phase);
			slab(g, y * 16, next * 16, v0, v1);
			y = next;
		}
	}

	/** A strip of curtain between heights y0 and y1 (pixels), its picture from v0 (top) to v1 (bottom). */
	private static void slab(Geometry g, double y0, double y1, float v0, float v1) {
		float a = (float) y0, b = (float) y1;
		float[] uv = {0, v0, 1, v1};
		// Front (south) and back (north) faces, and the edge at the bottom.
		g.quad(0, b, 9.5F, 0, a, 9.5F, 16, a, 9.5F, 16, b, 9.5F, 0, 0, 1, uv);
		g.quad(16, b, 6.5F, 16, a, 6.5F, 0, a, 6.5F, 0, b, 6.5F, 0, 0, -1, uv);
		g.quad(0, a, 6.5F, 0, a, 9.5F, 16, a, 9.5F, 16, a, 6.5F, 0, -1, 0, new float[] {0, v1 - 0.02F, 1, v1});
	}

	/** The coil on its barrel at the top of the opening, inside (north), its size by how much curtain is wound on. */
	private static void coil(Geometry g, float openness) {
		double r = 1.4 + 2.2 * Math.sqrt(openness);
		double cy = 16 - 3.8, cz = 6.4 - 3.6;
		LineRenderer.lathe(g, new Vec3(0.2, cy, cz), new Vec3(15.8, cy, cz), new double[][] {{0, r}, {15.6, r}}, 16, ALL);
		// The barrel's ends in their brackets on the wall over the opening.
		for (double x : new double[] {0, 15.4}) {
			LineRenderer.box(g, x, cy - 0.6, cz - 0.6, x + 0.6, 16, cz + 0.6, BRACKET);
		}
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
		return false;
	}
}
