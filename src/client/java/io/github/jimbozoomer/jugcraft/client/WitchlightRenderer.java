package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.jimbozoomer.jugcraft.agriculture.WitchlightBlock;
import io.github.jimbozoomer.jugcraft.agriculture.WitchlightBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.WitchlightLampPostBlock;
import io.github.jimbozoomer.jugcraft.agriculture.Witchlights;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Draws what burns in a witchlight's lantern (decor19_quads.json): a wisp ({@code witchlight_wisp}) turning and bobbing,
 * and a glow ({@code witchlight_glow}) in the glass's colour, small and dim asleep and flaring large when awake, easing
 * between them over {@value Witchlights#FADE_TICKS} ticks. The lantern's centre depends on how it is held.
 */
public class WitchlightRenderer implements BlockEntityRenderer<WitchlightBlockEntity, WitchlightRenderer.State> {
	/** The lantern's centre, pixels, on a stake, hanging, and on a lamp-post's upper half facing north. */
	private static final float[] STAKE = {8.0F, 10.0F, 8.0F};
	private static final float[] HANGING = {8.0F, 7.0F, 8.0F};
	private static final float[] POST = {8.0F, 6.5F, 3.5F};

	public static final class State extends BlockEntityRenderState {
		float[] centre = STAKE;
		Direction facing = Direction.NORTH;
		int colour = 0xFFB45CFF;
		float glow;
		float time;
	}

	public WitchlightRenderer(BlockEntityRendererProvider.Context context) {
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(WitchlightBlockEntity lamp, State state, float partialTick, Vec3 camera,
			ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
		BlockEntityRenderState.extractBase(lamp, state, crumbling);
		BlockState block = lamp.getBlockState();
		if (!block.hasProperty(Witchlights.LIT) || lamp.getLevel() == null) {
			return;
		}
		if (block.getBlock() instanceof WitchlightLampPostBlock) {
			state.centre = POST;
			state.facing = block.getValue(WitchlightLampPostBlock.FACING);
		} else {
			state.centre = block.getBlock() instanceof WitchlightBlock light && light.mount() == WitchlightBlock.Mount.HANGING ? HANGING : STAKE;
			state.facing = Direction.NORTH;
		}
		state.colour = 0xFF000000 | block.getValue(Witchlights.COLOUR).rgb();
		float target = block.getValue(Witchlights.LIT) ? 1.0F : 0.0F;
		float now = Math.floorMod(lamp.getLevel().getGameTime(), 1L << 20) + partialTick;
		if (lamp.glow < 0) {
			lamp.glow = target;
		} else {
			float step = Mth.clamp(now - lamp.glowTime, 0.0F, Witchlights.FADE_TICKS) / Witchlights.FADE_TICKS;
			lamp.glow = lamp.glow < target ? Math.min(target, lamp.glow + step * 2) : Math.max(target, lamp.glow - step);
		}
		lamp.glowTime = now;
		state.glow = lamp.glow;
		state.time = now + (lamp.getBlockPos().hashCode() & 255);
	}

	@Override
	public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
		QuadModel wisp = DecorQuads.get("witchlight_wisp");
		QuadModel glow = DecorQuads.get("witchlight_glow");
		pose.pushPose();
		pose.translate(0.5F, 0.0F, 0.5F);
		pose.rotateDegrees(Axis.YP, -RockingChairRenderer.yRotation(state.facing));
		pose.translate(state.centre[0] / 16 - 0.5F, state.centre[1] / 16, state.centre[2] / 16 - 0.5F);
		if (glow != null) {
			float size = 0.55F + 0.45F * state.glow + 0.04F * Mth.sin(state.time * 0.2F);
			pose.pushPose();
			pose.scale(size, size, size);
			int alpha = (int) (90 + 140 * state.glow);
			glow.submit(pose, collector, 0xF000F0, (alpha << 24) | (state.colour & 0xFFFFFF));
			pose.popPose();
		}
		if (wisp != null) {
			pose.pushPose();
			pose.translate(0.0F, 0.6F * Mth.sin(state.time * 0.09F) / 16, 0.0F);
			pose.rotateDegrees(Axis.YP, state.time * 4.0F);
			float size = 0.7F + 0.3F * state.glow;
			pose.scale(size, size, size);
			wisp.submit(pose, collector, 0xF000F0, state.colour);
			pose.popPose();
		}
		pose.popPose();
	}
}
