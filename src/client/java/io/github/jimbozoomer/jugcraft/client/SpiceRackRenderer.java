package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.jimbozoomer.jugcraft.agriculture.SpiceRackBlock;
import io.github.jimbozoomer.jugcraft.agriculture.SpiceRackBlockEntity;
import java.util.List;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The spices on a Spice Rack, four on the upper shelf and four below, each standing up facing out, drawn for a rack facing
 * north (its shelves in the block's south half, tools/spice_data.py) and turned to its facing.
 */
public class SpiceRackRenderer implements BlockEntityRenderer<SpiceRackBlockEntity, SpiceRackRenderer.State> {
	/** Where each spice stands, in pixels: across, its foot above the floor, and from the north face. */
	private static final float[][] SPOTS = {{3.0F, 9.0F, 12.0F}, {6.33F, 9.0F, 12.0F}, {9.67F, 9.0F, 12.0F}, {13.0F, 9.0F, 12.0F},
			{3.0F, 1.0F, 12.0F}, {6.33F, 1.0F, 12.0F}, {9.67F, 1.0F, 12.0F}, {13.0F, 1.0F, 12.0F}};
	/** An item drawn FIXED is half a block; this makes each spice about four pixels across, as small jars and bundles. */
	private static final float SCALE = 0.5F;

	private final ItemModelResolver itemModels;

	public static final class State extends BlockEntityRenderState {
		final ItemStackRenderState[] spices = new ItemStackRenderState[SpiceRackBlockEntity.SLOTS];
		int count;
		Direction facing = Direction.NORTH;

		State() {
			for (int i = 0; i < spices.length; i++) {
				spices[i] = new ItemStackRenderState();
			}
		}
	}

	public SpiceRackRenderer(BlockEntityRendererProvider.Context context) {
		this.itemModels = context.itemModelResolver();
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(SpiceRackBlockEntity rack, State state, float partialTick, Vec3 camera,
			ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
		BlockEntityRenderState.extractBase(rack, state, crumbling);
		state.facing = rack.getBlockState().hasProperty(SpiceRackBlock.FACING) ? rack.getBlockState().getValue(SpiceRackBlock.FACING)
				: Direction.NORTH;
		List<ItemStack> spices = rack.spices();
		state.count = Math.min(spices.size(), state.spices.length);
		for (int i = 0; i < state.count; i++) {
			itemModels.updateForTopItem(state.spices[i], spices.get(i), ItemDisplayContext.FIXED, rack.getLevel(), null,
					(int) rack.getBlockPos().asLong() + i);
		}
	}

	@Override
	public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
		for (int i = 0; i < state.count; i++) {
			if (state.spices[i].isEmpty()) {
				continue;
			}
			pose.pushPose();
			pose.translate(0.5F, 0.0F, 0.5F);
			pose.rotateDegrees(Axis.YP, -RockingChairRenderer.yRotation(state.facing));
			pose.translate(SPOTS[i][0] / 16.0F - 0.5F, SPOTS[i][1] / 16.0F + SCALE * 0.25F, SPOTS[i][2] / 16.0F - 0.5F);
			// FIXED faces an item south; the rack's front is north.
			pose.rotateDegrees(Axis.YP, 180.0F);
			pose.scale(SCALE, SCALE, SCALE);
			state.spices[i].submit(pose, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
			pose.popPose();
		}
	}
}
