package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.jimbozoomer.jugcraft.agriculture.CrawlingHandBlock;
import io.github.jimbozoomer.jugcraft.agriculture.DecorationBlockEntity;
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
 * Draws the Crawling Hand (decor19_quads.json: {@code crawling_hand} and its four fingers, for a hand lying in the middle
 * of its block pointing north). At rest it drums its fingers one after another; powered, it scuttles clockwise round a
 * rounded square on top of its block, a lap every {@value CrawlingHandBlock#LAP_TICKS} ticks, turned the way it runs,
 * its fingers walking.
 */
public class CrawlingHandRenderer implements BlockEntityRenderer<DecorationBlockEntity, CrawlingHandRenderer.State> {
	/** Each finger's knuckle, pixels, about which it lifts (tools/decor19_data.py KNUCKLES). */
	private static final float[][] KNUCKLES = {{4.8F, 1.4F, 5.0F}, {6.4F, 1.4F, 4.4F}, {8.0F, 1.4F, 4.6F}, {9.6F, 1.4F, 5.2F}};
	/** Half the side of the square the hand runs round, pixels. */
	private static final float RUN = 3.0F;

	public static final class State extends BlockEntityRenderState {
		Direction facing = Direction.NORTH;
		boolean powered;
		float time;
	}

	public CrawlingHandRenderer(BlockEntityRendererProvider.Context context) {
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(DecorationBlockEntity hand, State state, float partialTick, Vec3 camera,
			ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
		BlockEntityRenderState.extractBase(hand, state, crumbling);
		BlockState block = hand.getBlockState();
		if (!(block.getBlock() instanceof CrawlingHandBlock) || hand.getLevel() == null) {
			return;
		}
		state.facing = block.getValue(CrawlingHandBlock.FACING);
		state.powered = block.getValue(CrawlingHandBlock.POWERED);
		state.time = Math.floorMod(hand.getLevel().getGameTime(), 24000L) + partialTick + (hand.getBlockPos().hashCode() & 63);
	}

	@Override
	public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
		QuadModel palm = DecorQuads.get("crawling_hand");
		if (palm == null) {
			return;
		}
		pose.pushPose();
		pose.translate(0.5F, 0.0F, 0.5F);
		pose.rotateDegrees(Axis.YP, -RockingChairRenderer.yRotation(state.facing));
		float speed;
		if (state.powered) {
			// Round the square: along each side for a quarter lap, turning a right angle at each corner.
			float lap = (state.time % CrawlingHandBlock.LAP_TICKS) / CrawlingHandBlock.LAP_TICKS * 4.0F;
			int side = (int) lap;
			float along = lap - side;
			float x = -RUN + 2 * RUN * along;
			pose.rotateDegrees(Axis.YP, -90.0F * side);
			pose.translate(x / 16, 0.0F, -RUN / 16);
			pose.rotateDegrees(Axis.YP, -90.0F);
			speed = 0.9F;
		} else {
			speed = 0.25F;
		}
		pose.translate(-0.5F, 0.0F, -0.5F);
		palm.submit(pose, collector, state.lightCoords);
		for (int i = 0; i < KNUCKLES.length; i++) {
			QuadModel finger = DecorQuads.get("crawling_hand_finger_" + i);
			if (finger == null) {
				continue;
			}
			float lift = state.powered ? 25.0F * Math.max(0.0F, Mth.sin(state.time * speed + i * 1.6F))
					: 30.0F * Math.max(0.0F, Mth.sin(state.time * speed - i * 0.9F));
			float[] k = KNUCKLES[i];
			pose.pushPose();
			pose.translate(k[0] / 16, k[1] / 16, k[2] / 16);
			pose.rotateDegrees(Axis.XP, -lift);
			pose.translate(-k[0] / 16, -k[1] / 16, -k[2] / 16);
			finger.submit(pose, collector, state.lightCoords);
			pose.popPose();
		}
		pose.popPose();
	}
}
