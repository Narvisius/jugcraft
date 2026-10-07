package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.jimbozoomer.jugcraft.agriculture.PumpkinCrateBlock;
import io.github.jimbozoomer.jugcraft.agriculture.PumpkinCrateBlockEntity;
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
 * Draws the produce in a Pumpkin Crate: up to four pieces, two by two, sitting in the crate with their tops above its
 * slats, each turned a little differently so a full crate doesn't look stamped out.
 */
public class PumpkinCrateRenderer implements BlockEntityRenderer<PumpkinCrateBlockEntity, PumpkinCrateRenderer.State> {
	/** Where each piece sits, in pixels from the crate's north-west corner (for a north-facing crate). */
	private static final float[][] SPOTS = {{4.25F, 4.25F}, {11.75F, 4.25F}, {4.25F, 11.75F}, {11.75F, 11.75F}};
	private static final float[] TURNS = {8.0F, -12.0F, 20.0F, -4.0F};
	/** A block item drawn FIXED is half a block; this makes each piece seven pixels across. */
	private static final float SCALE = 0.875F;
	private static final float FLOOR = 1.5F / 16.0F;

	private final ItemModelResolver itemModels;

	public static final class State extends BlockEntityRenderState {
		final ItemStackRenderState[] produce = {new ItemStackRenderState(), new ItemStackRenderState(), new ItemStackRenderState(),
				new ItemStackRenderState()};
		int count;
		Direction facing = Direction.NORTH;
	}

	public PumpkinCrateRenderer(BlockEntityRendererProvider.Context context) {
		this.itemModels = context.itemModelResolver();
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(PumpkinCrateBlockEntity crate, State state, float partialTick, Vec3 camera,
			ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
		BlockEntityRenderState.extractBase(crate, state, crumbling);
		state.facing = crate.getBlockState().getValue(PumpkinCrateBlock.FACING);
		List<ItemStack> produce = crate.produce();
		state.count = Math.min(produce.size(), state.produce.length);
		for (int i = 0; i < state.count; i++) {
			itemModels.updateForTopItem(state.produce[i], produce.get(i), ItemDisplayContext.FIXED, crate.getLevel(), null,
					(int) crate.getBlockPos().asLong() + i);
		}
	}

	@Override
	public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
		for (int i = 0; i < state.count; i++) {
			if (state.produce[i].isEmpty()) {
				continue;
			}
			pose.pushPose();
			pose.translate(0.5F, 0.0F, 0.5F);
			pose.rotateDegrees(Axis.YP, -yRotation(state.facing));
			pose.translate(SPOTS[i][0] / 16.0F - 0.5F, FLOOR + SCALE * 0.25F, SPOTS[i][1] / 16.0F - 0.5F);
			pose.rotateDegrees(Axis.YP, TURNS[i]);
			pose.scale(SCALE, SCALE, SCALE);
			state.produce[i].submit(pose, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
			pose.popPose();
		}
	}

	private static float yRotation(Direction facing) {
		return switch (facing) {
			case EAST -> 90;
			case SOUTH -> 180;
			case WEST -> 270;
			default -> 0;
		};
	}
}
