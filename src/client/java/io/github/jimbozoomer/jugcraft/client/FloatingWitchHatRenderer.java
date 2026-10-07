package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.jimbozoomer.jugcraft.agriculture.DecorationBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.FloatingWitchHatBlock;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Draws a Floating Witch Hat bobbing ({@link FloatingWitchHatBlock#bob}) and turning slowly
 * ({@link FloatingWitchHatBlock#turn}), and, while it is lit, the candle flame under its brim at full brightness.
 */
public class FloatingWitchHatRenderer implements BlockEntityRenderer<DecorationBlockEntity, FloatingWitchHatRenderer.State> {
	private static final int FULL_BRIGHT = 0xF000F0;

	public static final class State extends BlockEntityRenderState {
		boolean lit;
		float bob;
		float turn;
	}

	public FloatingWitchHatRenderer(BlockEntityRendererProvider.Context context) {
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(DecorationBlockEntity hat, State state, float partialTick, Vec3 camera,
			ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
		BlockEntityRenderState.extractBase(hat, state, crumbling);
		BlockState block = hat.getBlockState();
		Level level = hat.getLevel();
		if (!(block.getBlock() instanceof FloatingWitchHatBlock) || level == null) {
			return;
		}
		double now = level.getGameTime() + (double) partialTick;
		state.lit = block.getValue(FloatingWitchHatBlock.LIT);
		state.bob = FloatingWitchHatBlock.bob(hat.getBlockPos(), now);
		state.turn = FloatingWitchHatBlock.turn(hat.getBlockPos(), now);
	}

	@Override
	public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
		QuadModel hat = DecorQuads.get("floating_witch_hat");
		QuadModel flame = DecorQuads.get("floating_witch_hat_flame");
		pose.pushPose();
		pose.translate(0.5F, state.bob / 16, 0.5F);
		pose.rotateDegrees(Axis.YP, state.turn);
		pose.translate(-0.5F, 0.0F, -0.5F);
		if (hat != null) {
			hat.submit(pose, collector, state.lit ? Math.max(state.lightCoords, 0xA000A0) : state.lightCoords);
		}
		if (state.lit && flame != null) {
			flame.submit(pose, collector, FULL_BRIGHT);
		}
		pose.popPose();
	}
}
