package io.github.jimbozoomer.jugcraft.client.warehouse;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.jimbozoomer.jugcraft.warehouse.RackBlock;
import io.github.jimbozoomer.jugcraft.warehouse.RackBlockEntity;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * A pallet place's location label, written on its beam's label plate (the first level's beam carries the ground place's
 * label too), when you are near enough to read it.
 */
public class RackRenderer implements BlockEntityRenderer<RackBlockEntity, RackRenderer.State> {
	private final Font font;

	public static class State extends BlockEntityRenderState {
		Direction facing = Direction.NORTH;
		@Nullable Page up, down;
		float upX;
	}

	public RackRenderer(BlockEntityRendererProvider.Context context) {
		this.font = context.font();
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public int getViewDistance() {
		return 12;
	}

	@Override
	public void extractRenderState(RackBlockEntity rack, State state, float partialTick, Vec3 camera,
			ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
		BlockEntityRenderState.extractBase(rack, state, crumbling);
		state.up = state.down = null;
		if (!(rack.getBlockState().getBlock() instanceof RackBlock) || rack.label().isEmpty()) {
			return;
		}
		state.facing = rack.getBlockState().getValue(RackBlock.FACING);
		RackBlock.RackLevel level = rack.getBlockState().getValue(RackBlock.LEVEL);
		if (level == RackBlock.RackLevel.GROUND) {
			return;
		}
		state.up = new Page(font).add(0, 0, 1, rack.label(), 0xFF141416);
		state.upX = level == RackBlock.RackLevel.FIRST ? 8.4F : 5.9F;
		if (level == RackBlock.RackLevel.FIRST && !rack.groundLabel().isEmpty()) {
			state.down = new Page(font).add(0, 0, 1, rack.groundLabel(), 0xFF141416);
		}
	}

	@Override
	public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
		if (state.up == null) {
			return;
		}
		pose.pushPose();
		Page.blockFrame(pose, state.facing);
		// The plates: 4.2 px wide, y 0.25..1.35, faces at z 15.7; the writing right of the arrow, above the barcode.
		state.up.draw(pose, collector, state.upX + 0.75F, 1.3F, 15.7F, 0.07F);
		if (state.down != null) {
			state.down.draw(pose, collector, 3.4F + 0.75F, 1.3F, 15.7F, 0.07F);
		}
		pose.popPose();
	}
}
