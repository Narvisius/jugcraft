package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.jimbozoomer.jugcraft.agriculture.GhostBellBlock;
import io.github.jimbozoomer.jugcraft.agriculture.GhostBellBlockEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Draws the Ghost Bell and its little ghost clapper hanging from the post's arm: swinging side to side the whole time a
 * round of Ghost Tag is on, the ghost swinging after the bell; just stirring otherwise.
 */
public class GhostBellRenderer implements BlockEntityRenderer<GhostBellBlockEntity, GhostBellRenderer.State> {
	/** Where the bell hangs, and the clapper inside it (pixels). */
	private static final float[] BELL = {8.0F, 15.0F, 7.25F};
	private static final float[] CLAPPER = {8.0F, 13.0F, 7.25F};
	private static final float SWING = 28.0F;
	private static final float CLAPPER_SWING = 42.0F;
	/** Ticks a swing there and back takes. */
	private static final float PERIOD = 24.0F;

	public static final class State extends BlockEntityRenderState {
		Direction facing = Direction.NORTH;
		float bell;
		float clapper;
	}

	public GhostBellRenderer(BlockEntityRendererProvider.Context context) {
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(GhostBellBlockEntity entity, State state, float partialTick, Vec3 camera,
			ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
		BlockEntityRenderState.extractBase(entity, state, crumbling);
		BlockState block = entity.getBlockState();
		Level level = entity.getLevel();
		state.bell = 0.0F;
		state.clapper = 0.0F;
		if (!(block.getBlock() instanceof GhostBellBlock) || level == null) {
			return;
		}
		state.facing = block.getValue(GhostBellBlock.FACING);
		float time = (float) ((level.getGameTime() + (double) partialTick) % 24000.0) + (entity.getBlockPos().hashCode() & 0xFF);
		float phase = time * Mth.TWO_PI / PERIOD;
		if (block.getValue(GhostBellBlock.RINGING)) {
			state.bell = SWING * Mth.sin(phase);
			state.clapper = CLAPPER_SWING * Mth.sin(phase - 0.9F);
		} else {
			state.bell = 1.5F * Mth.sin(time * 0.05F);
			state.clapper = 4.0F * Mth.sin(time * 0.07F);
		}
	}

	@Override
	public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
		QuadModel bell = DecorQuads.get("ghost_bell");
		QuadModel clapper = DecorQuads.get("ghost_bell_clapper");
		pose.pushPose();
		pose.translate(0.5F, 0.0F, 0.5F);
		pose.rotateDegrees(Axis.YP, -RockingChairRenderer.yRotation(state.facing));
		pose.translate(-0.5F, 0.0F, -0.5F);
		turn(pose, BELL, state.bell);
		if (bell != null) {
			bell.submit(pose, collector, state.lightCoords);
		}
		turn(pose, CLAPPER, state.clapper - state.bell);
		if (clapper != null) {
			clapper.submit(pose, collector, state.lightCoords);
		}
		pose.popPose();
	}

	/** Turns the pose by {@code degrees} about the z line through {@code pivot} (pixels). */
	private static void turn(PoseStack pose, float[] pivot, float degrees) {
		pose.translate(pivot[0] / 16, pivot[1] / 16, pivot[2] / 16);
		pose.rotateDegrees(Axis.ZP, degrees);
		pose.translate(-pivot[0] / 16, -pivot[1] / 16, -pivot[2] / 16);
	}
}
