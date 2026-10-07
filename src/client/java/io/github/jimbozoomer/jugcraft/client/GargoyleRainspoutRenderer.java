package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.agriculture.GargoyleRainspoutBlock;
import io.github.jimbozoomer.jugcraft.agriculture.GargoyleRainspoutBlockEntity;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Draws the Gargoyle Rainspout's stream while it pours: water arcing out of the beast's mouth and falling to whatever is
 * under it (at most {@link GargoyleRainspoutBlock#STREAM} blocks), its texture running down.
 */
public class GargoyleRainspoutRenderer implements BlockEntityRenderer<GargoyleRainspoutBlockEntity, GargoyleRainspoutRenderer.State> {
	private static final RenderType STREAM = RenderTypes.entityTranslucent(Jugcraft.id("textures/entity/crypt_stream.png"));
	/** The mouth, in pixels for a spout facing north (tools/decor18_data.py SPOUT_MOUTH). */
	private static final float MOUTH_X = 8.0F;
	private static final float MOUTH_Y = 6.4F;
	private static final float MOUTH_Z = -1.0F;
	private static final float WIDTH = 1.4F;

	public static final class State extends BlockEntityRenderState {
		Direction facing = Direction.NORTH;
		boolean pouring;
		float fall;
		float time;
	}

	public GargoyleRainspoutRenderer(BlockEntityRendererProvider.Context context) {
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(GargoyleRainspoutBlockEntity spout, State state, float partialTick, Vec3 camera,
			ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
		BlockEntityRenderState.extractBase(spout, state, crumbling);
		BlockState block = spout.getBlockState();
		if (!(block.getBlock() instanceof GargoyleRainspoutBlock) || spout.getLevel() == null) {
			return;
		}
		state.facing = block.getValue(GargoyleRainspoutBlock.FACING);
		state.pouring = block.getValue(GargoyleRainspoutBlock.POURING);
		state.time = (spout.getLevel().getGameTime() % 1000) + partialTick;
		if (state.pouring) {
			BlockPos landing = GargoyleRainspoutBlock.landing(spout.getLevel(), spout.getBlockPos(), state.facing, GargoyleRainspoutBlock.STREAM);
			// From the mouth to the top of what it lands on (a full block's top; near enough for a cauldron's water).
			int below = landing == null ? GargoyleRainspoutBlock.STREAM : spout.getBlockPos().getY() - landing.getY() - 1;
			state.fall = MOUTH_Y / 16 + below;
		}
	}

	@Override
	public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
		if (!state.pouring) {
			return;
		}
		pose.pushPose();
		pose.translate(0.5F, 0.0F, 0.5F);
		pose.rotateDegrees(Axis.YP, -RockingChairRenderer.yRotation(state.facing));
		pose.translate(-0.5F, 0.0F, -0.5F);
		float x0 = (MOUTH_X - WIDTH / 2) / 16, x1 = (MOUTH_X + WIDTH / 2) / 16;
		float top = MOUTH_Y / 16, bottom = top - state.fall;
		float z = (MOUTH_Z - 2.5F) / 16;
		float scroll = state.time * 0.08F;
		float v0 = scroll, v1 = scroll + state.fall * 2;
		collector.submitCustomGeometry(pose, STREAM, (matrix, buffer) -> {
			int light = state.lightCoords;
			int color = 0xC8FFFFFF;
			// The arc out of the mouth, then the fall: two sheets crossing, so it reads from every side.
			DecorDraw.quad(buffer, matrix, new float[][] {{x0, top, MOUTH_Z / 16, 0, v0}, {x0, top - 0.12F, z, 0, v0 + 0.2F},
					{x1, top - 0.12F, z, 1, v0 + 0.2F}, {x1, top, MOUTH_Z / 16, 1, v0}}, 0, 1, 0, color, light);
			DecorDraw.quad(buffer, matrix, new float[][] {{x0, top - 0.12F, z, 0, v0}, {x0, bottom, z, 0, v1}, {x1, bottom, z, 1, v1},
					{x1, top - 0.12F, z, 1, v0}}, 0, 0, -1, color, light);
			DecorDraw.quad(buffer, matrix, new float[][] {{x1, top - 0.12F, z, 0, v0}, {x1, bottom, z, 0, v1}, {x0, bottom, z, 1, v1},
					{x0, top - 0.12F, z, 1, v0}}, 0, 0, 1, color, light);
			float zc0 = z - WIDTH / 32, zc1 = z + WIDTH / 32, xc = (x0 + x1) / 2;
			DecorDraw.quad(buffer, matrix, new float[][] {{xc, top - 0.12F, zc0, 0, v0}, {xc, bottom, zc0, 0, v1}, {xc, bottom, zc1, 1, v1},
					{xc, top - 0.12F, zc1, 1, v0}}, -1, 0, 0, color, light);
			DecorDraw.quad(buffer, matrix, new float[][] {{xc, top - 0.12F, zc1, 0, v0}, {xc, bottom, zc1, 0, v1}, {xc, bottom, zc0, 1, v1},
					{xc, top - 0.12F, zc0, 1, v0}}, 1, 0, 0, color, light);
		});
		pose.popPose();
	}

	@Override
	public boolean shouldRenderOffScreen() {
		return true;
	}
}
