package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.jimbozoomer.jugcraft.agriculture.DecorationBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.EggSacClusterBlock;
import io.github.jimbozoomer.jugcraft.agriculture.MourningAngelBlock;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.MultifaceBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Draws the spiderlings of an Egg Sac Cluster: at night, on each face that has sacs, now and then (one hatching window in
 * three, a window every {@value #WINDOW} ticks, by where it is) three tiny spiders (decor19_quads.json:
 * {@code spiderling}) skitter out from the middle in different directions over {@value #RUN} ticks, shrinking away to
 * nothing. They are only drawn: nothing is spawned.
 */
public class EggSacRenderer implements BlockEntityRenderer<DecorationBlockEntity, EggSacRenderer.State> {
	static final int WINDOW = 160;
	static final int RUN = 30;
	private static final int SPIDERS = 3;

	public static final class State extends BlockEntityRenderState {
		final List<Direction> faces = new ArrayList<>();
		/** How far through the run, 0 to 1, or below 0 for none. */
		float run = -1.0F;
		float seed;
	}

	public EggSacRenderer(BlockEntityRendererProvider.Context context) {
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(DecorationBlockEntity sacs, State state, float partialTick, Vec3 camera,
			ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
		BlockEntityRenderState.extractBase(sacs, state, crumbling);
		state.faces.clear();
		state.run = -1.0F;
		BlockState block = sacs.getBlockState();
		if (!(block.getBlock() instanceof EggSacClusterBlock) || sacs.getLevel() == null || !MourningAngelBlock.night(sacs.getLevel())) {
			return;
		}
		long now = sacs.getLevel().getGameTime();
		int salt = sacs.getBlockPos().hashCode();
		long window = Math.floorDiv(now + (salt & 127), WINDOW);
		float into = Math.floorMod(now + (salt & 127), (long) WINDOW) + partialTick;
		if (Math.floorMod(window * 31 + salt, 3) != 0 || into >= RUN) {
			return;
		}
		for (Direction face : Direction.values()) {
			if (block.getValue(MultifaceBlock.getFaceProperty(face))) {
				state.faces.add(face);
			}
		}
		state.run = into / RUN;
		state.seed = Math.floorMod(window * 7 + salt, 360);
	}

	@Override
	public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
		QuadModel spider = DecorQuads.get("spiderling");
		if (spider == null || state.run < 0) {
			return;
		}
		float scale = 1.0F - state.run * state.run;
		for (Direction face : state.faces) {
			pose.pushPose();
			pose.translate(0.5F, 0.5F, 0.5F);
			pose.mulPose(new org.joml.Matrix4f().rotation(face.getOpposite().getRotation()));
			pose.translate(0.0F, -0.5F + 0.01F, 0.0F);
			for (int i = 0; i < SPIDERS; i++) {
				float heading = state.seed + i * 120.0F + face.ordinal() * 40.0F;
				float distance = state.run * 0.42F;
				pose.pushPose();
				pose.rotateDegrees(Axis.YP, heading);
				pose.translate(0.0F, 0.0F, distance);
				pose.rotateDegrees(Axis.YP, 10.0F * Mth.sin(state.run * 40.0F + i));
				pose.scale(scale, scale, scale);
				spider.submit(pose, collector, state.lightCoords);
				pose.popPose();
			}
			pose.popPose();
		}
	}
}
