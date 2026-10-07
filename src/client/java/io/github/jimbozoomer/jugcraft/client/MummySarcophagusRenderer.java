package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.jimbozoomer.jugcraft.agriculture.DecorationBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.MummySarcophagusBlock;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Draws the Mummy Sarcophagus's lid and mummy on its lower half. Opening, the lid swings out on its hinge, then the
 * mummy lurches out with its arms coming up; closing, the mummy goes back first and the lid follows. Coordinates are
 * in pixels of the lower half, for a sarcophagus facing north.
 */
public class MummySarcophagusRenderer implements BlockEntityRenderer<DecorationBlockEntity, MummySarcophagusRenderer.State> {
	/** The lid's hinge, on its west edge at the back of the lid. */
	private static final float HINGE_X = 2.25F;
	private static final float HINGE_Z = 4.5F;
	/** The mummy's shoulders, about which its arms come up. */
	private static final float SHOULDER_Y = 22.5F;
	private static final float SHOULDER_Z = 7.25F;
	/** Each sarcophagus as this client last left it: {lid angle, lurch, game time}. */
	private final Map<DecorationBlockEntity, double[]> cases = new WeakHashMap<>();

	public static final class State extends BlockEntityRenderState {
		Direction facing = Direction.NORTH;
		float lid;
		float lurch;
	}

	public MummySarcophagusRenderer(BlockEntityRendererProvider.Context context) {
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(DecorationBlockEntity sarcophagus, State state, float partialTick, Vec3 camera,
			ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
		BlockEntityRenderState.extractBase(sarcophagus, state, crumbling);
		BlockState block = sarcophagus.getBlockState();
		Level level = sarcophagus.getLevel();
		if (!(block.getBlock() instanceof MummySarcophagusBlock) || level == null) {
			return;
		}
		state.facing = block.getValue(MummySarcophagusBlock.FACING);
		boolean open = block.getValue(MummySarcophagusBlock.OPEN);
		double now = level.getGameTime() + (double) partialTick;
		double[] at = cases.computeIfAbsent(sarcophagus, s -> new double[] {0.0, 0.0, now});
		float elapsed = (float) Math.max(0.0, Math.min(20.0, now - at[2]));
		float lid = (float) at[0];
		float lurch = (float) at[1];
		if (open) {
			lid = MummySarcophagusBlock.approach(lid, MummySarcophagusBlock.LID_DEGREES, MummySarcophagusBlock.LID_SPEED * elapsed);
			if (lid > MummySarcophagusBlock.LID_DEGREES * 0.6F) {
				lurch = MummySarcophagusBlock.approach(lurch, MummySarcophagusBlock.LURCH, MummySarcophagusBlock.LURCH_SPEED * elapsed);
			}
		} else {
			lurch = MummySarcophagusBlock.approach(lurch, 0.0F, MummySarcophagusBlock.LURCH_SPEED * elapsed);
			if (lurch < 0.1F) {
				lid = MummySarcophagusBlock.approach(lid, 0.0F, MummySarcophagusBlock.LID_SPEED * elapsed);
			}
		}
		at[0] = lid;
		at[1] = lurch;
		at[2] = now;
		state.lid = lid;
		state.lurch = lurch;
	}

	@Override
	public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
		QuadModel lid = DecorQuads.get("mummy_sarcophagus_lid");
		QuadModel mummy = DecorQuads.get("mummy_sarcophagus_mummy");
		QuadModel arms = DecorQuads.get("mummy_sarcophagus_arms");
		pose.pushPose();
		pose.translate(0.5F, 0.0F, 0.5F);
		pose.rotateDegrees(Axis.YP, -RockingChairRenderer.yRotation(state.facing));
		pose.translate(-0.5F, 0.0F, -0.5F);
		if (lid != null) {
			pose.pushPose();
			pose.translate(HINGE_X / 16, 0.0F, HINGE_Z / 16);
			pose.rotateDegrees(Axis.YP, state.lid);
			pose.translate(-HINGE_X / 16, 0.0F, -HINGE_Z / 16);
			lid.submit(pose, collector, state.lightCoords);
			pose.popPose();
		}
		pose.translate(0.0F, 0.0F, -state.lurch / 16);
		if (mummy != null) {
			mummy.submit(pose, collector, state.lightCoords);
		}
		if (arms != null) {
			pose.translate(0.0F, SHOULDER_Y / 16, SHOULDER_Z / 16);
			pose.rotateDegrees(Axis.XP, 90.0F * state.lurch / MummySarcophagusBlock.LURCH);
			pose.translate(0.0F, -SHOULDER_Y / 16, -SHOULDER_Z / 16);
			arms.submit(pose, collector, state.lightCoords);
		}
		pose.popPose();
	}

	@Override
	public boolean shouldRenderOffScreen() {
		return true; // It stands two blocks tall.
	}
}
