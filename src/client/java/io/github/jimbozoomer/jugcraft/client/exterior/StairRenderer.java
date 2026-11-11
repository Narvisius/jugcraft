package io.github.jimbozoomer.jugcraft.client.exterior;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.jimbozoomer.jugcraft.client.warehouse.WhQuads;
import io.github.jimbozoomer.jugcraft.exterior.JugcraftExterior;
import io.github.jimbozoomer.jugcraft.exterior.StairBlockEntity;
import io.github.jimbozoomer.jugcraft.exterior.SteelStairBlock;
import io.github.jimbozoomer.jugcraft.warehouse.HiddenBlock;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;

/**
 * A steel stair section's sloped parts, one side of it (drawn from its part 0 cell; block models only turn by 22.5
 * or 45 degrees and these stand at 33.7): the channel stringer under the tread ends and the handrail 0.9 m over the
 * nosings, on brackets off a wall where the side is against one, otherwise on guard posts with a knee rail. Where the
 * flight starts and ends the handrail runs on level for 0.3 m, as a real one does. The parts are the hidden
 * exterior_part block's models (tools/exterior_data.py PARTS), each running along -z from 0 to -16 and sheared up the
 * slope here to its length.
 */
public class StairRenderer implements BlockEntityRenderer<StairBlockEntity, StairRenderer.State> {
	static final float RISE = (float) SteelStairBlock.RISE, RUN = (float) SteelStairBlock.RUN, SLOPE = RISE / RUN;
	/** The handrail and the knee rail over the nosings (px). */
	static final float RAIL_UP = 14.4F, KNEE_UP = 7.2F, EXT = 4.8F;

	public static class State extends BlockEntityRenderState {
		Direction facing = Direction.NORTH;
		boolean right, wall, first, last;
	}

	public StairRenderer(BlockEntityRendererProvider.Context context) {
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(StairBlockEntity stair, State state, float partialTick, Vec3 camera,
			ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
		BlockEntityRenderState.extractBase(stair, state, crumbling);
		BlockState here = stair.getBlockState();
		Level level = stair.getLevel();
		if (!(here.getBlock() instanceof SteelStairBlock) || level == null) {
			return;
		}
		Direction f = here.getValue(SteelStairBlock.FACING);
		SteelStairBlock.Side side = here.getValue(SteelStairBlock.SIDE);
		BlockPos pos = stair.getBlockPos();
		state.facing = f;
		state.right = side == SteelStairBlock.Side.RIGHT;
		state.wall = SteelStairBlock.wallBeside(level, pos, f, side) || SteelStairBlock.wallBeside(level, pos.above(), f, side);
		state.first = !same(level.getBlockState(pos.relative(f.getOpposite()).below()), f, side, 3);
		state.last = !same(level.getBlockState(pos.relative(f, 3).above(2)), f, side, 0);
	}

	private static boolean same(BlockState s, Direction f, SteelStairBlock.Side side, int part) {
		return s.getBlock() instanceof SteelStairBlock && s.getValue(SteelStairBlock.FACING) == f && s.getValue(SteelStairBlock.SIDE) == side
				&& s.getValue(SteelStairBlock.PART) == part;
	}

	@Override
	public boolean shouldRenderOffScreen() {
		return true;
	}

	/** The pitch line through the nosings over the section's foot (px over the cell's floor; z from 16 at the foot). */
	static float line(float z) {
		return RISE + (16F - z) * SLOPE;
	}

	@Override
	public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
		pose.pushPose();
		float y = switch (state.facing) {
			case EAST -> 90;
			case SOUTH -> 180;
			case WEST -> 270;
			default -> 0;
		};
		pose.translate(0.5F, 0, 0.5F);
		pose.rotateDegrees(Axis.YP, -y);
		pose.translate(-0.5F, 0, -0.5F);
		pose.scale(1 / 16F, 1 / 16F, 1 / 16F);
		String s = state.right ? "_r" : "_l";
		int light = state.lightCoords;
		// The stringer: from under the floor at the flight's foot to the landing it carries to at its top.
		float zEnd = state.last ? 16F - (RISE * 11 - 1.2F) / SLOPE : 16F - 48F;
		float zStart = state.first ? 17.2F : 16F;
		sloped(pose, collector, "stringer" + s, zStart, zEnd, line(zStart), light);
		// The handrail (and a guard's knee rail and posts, or a wall rail's brackets).
		float zTop = state.last ? 16F - 44F : 16F - 48F;
		String rail = (state.wall ? "rail_wall" : "rail_guard") + s;
		sloped(pose, collector, rail, 16F, zTop, line(16F) + RAIL_UP, light);
		if (state.first) {
			level(pose, collector, rail, 16F + EXT, 16F, line(16F) + RAIL_UP, light);
		}
		if (state.last) {
			level(pose, collector, rail, zTop, zTop - EXT, line(zTop) + RAIL_UP, light);
		}
		if (state.wall) {
			for (int j : new int[] {2, 8}) {
				float z = 16F - RUN * j - 2F;
				upright(pose, collector, "bracket" + s, z, line(z) + RAIL_UP, 1F, light);
			}
		} else {
			sloped(pose, collector, "knee" + s, 16F, zTop, line(16F) + KNEE_UP, light);
			int[] posts = state.last ? new int[] {0, 6, 11} : new int[] {0, 6};
			for (int j : posts) {
				float z = 16F - RUN * j - 2F;
				upright(pose, collector, "post" + s, z, line(z) + 1.6F, (RAIL_UP - 1.6F) / 16F, light);
			}
		}
		pose.popPose();
	}

	/** A part sheared up the slope from za to zb (zb < za), its line at height ya over za. */
	private static void sloped(PoseStack pose, SubmitNodeCollector collector, String part, float za, float zb, float ya, int light) {
		shear(pose, collector, part, za, zb, ya, SLOPE, light);
	}

	/** A part run on level from za to zb at height ya. */
	private static void level(PoseStack pose, SubmitNodeCollector collector, String part, float za, float zb, float ya, int light) {
		shear(pose, collector, part, za, zb, ya, 0F, light);
	}

	private static void shear(PoseStack pose, SubmitNodeCollector collector, String part, float za, float zb, float ya, float slope, int light) {
		float s = (za - zb) / 16F;
		Matrix4f m = new Matrix4f();
		m.m21(-slope * s);
		m.m22(s);
		m.m31(ya);
		m.m32(za);
		pose.pushPose();
		pose.mulPose(m);
		WhQuads.submit(collector, pose, quads(part), light, WhQuads.Tint.NONE);
		pose.popPose();
	}

	/** A part standing straight at z, its foot at y, stretched up by k. */
	private static void upright(PoseStack pose, SubmitNodeCollector collector, String part, float z, float y, float k, int light) {
		pose.pushPose();
		pose.translate(0F, y, z);
		pose.scale(1F, k, 1F);
		WhQuads.submit(collector, pose, quads(part), light, WhQuads.Tint.NONE);
		pose.popPose();
	}

	private static java.util.List<net.minecraft.client.resources.model.geometry.BakedQuad> quads(String part) {
		return WhQuads.quads(((HiddenBlock) JugcraftExterior.PART).with(JugcraftExterior.part(part)));
	}
}
