package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.jimbozoomer.jugcraft.building.SearchlightBlock;
import io.github.jimbozoomer.jugcraft.building.Trenchworks;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Draws a searchlight's turning head from the quads tools/trenchworks.py exports (assets/jugcraft/trench_quads.json):
 * the yoke turned to the block's yaw, the drum also tilted up, its lens glowing and, while lit, its long beam.
 */
public class SearchlightRenderer implements BlockEntityRenderer<SearchlightBlock.Entity, SearchlightRenderer.State> {
	/** The head's pivot in the block, in pixels: keep in sync with PIVOT in tools/trenchworks.py. */
	private static final float[] PIVOT = {8, 11, 8};
	private static final int FULL_BRIGHT = 0xF000F0;

	public static final class State extends BlockEntityRenderState {
		float yaw;
		float tilt;
		boolean lit;
	}

	public SearchlightRenderer(BlockEntityRendererProvider.Context context) {
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(SearchlightBlock.Entity entity, State state, float partialTick, Vec3 camera,
			ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
		BlockEntityRenderState.extractBase(entity, state, crumbling);
		BlockState block = entity.getBlockState();
		if (block.getBlock() instanceof SearchlightBlock) {
			state.yaw = block.getValue(SearchlightBlock.YAW) * 360.0F / Trenchworks.SEARCHLIGHT_YAWS;
			state.tilt = block.getValue(SearchlightBlock.TILT) * 15.0F;
			state.lit = block.getValue(SearchlightBlock.LIT);
		}
	}

	@Override
	public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
		pose.pushPose();
		pose.translate(PIVOT[0] / 16.0F, PIVOT[1] / 16.0F, PIVOT[2] / 16.0F);
		pose.rotateDegrees(Axis.YP, -state.yaw);
		draw("searchlight_yoke", pose, collector, state.lightCoords);
		pose.rotateDegrees(Axis.XP, -state.tilt);
		draw("searchlight_drum", pose, collector, state.lightCoords);
		draw("searchlight_lens", pose, collector, state.lit ? FULL_BRIGHT : state.lightCoords);
		if (state.lit) {
			draw("searchlight_beam", pose, collector, FULL_BRIGHT);
		}
		pose.popPose();
	}

	private static void draw(String part, PoseStack pose, SubmitNodeCollector collector, int light) {
		QuadModel model = DecorQuads.get(part);
		if (model != null) {
			model.submit(pose, collector, light);
		}
	}

	/** The beam reaches far beyond the block. */
	@Override
	public boolean shouldRenderOffScreen() {
		return true;
	}

	@Override
	public int getViewDistance() {
		return 128;
	}
}
