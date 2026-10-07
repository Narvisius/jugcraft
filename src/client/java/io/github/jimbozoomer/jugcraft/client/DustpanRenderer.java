package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.jimbozoomer.jugcraft.agriculture.DustpanBlock;
import io.github.jimbozoomer.jugcraft.agriculture.DustpanBlockEntity;
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
 * Draws the heap of sweepings in a Dustpan: up to three of what it holds, lying in the pan at odd angles (tools/decor17_data.py
 * PAN_HEAP), for a pan facing north turned to its facing.
 */
public class DustpanRenderer implements BlockEntityRenderer<DustpanBlockEntity, DustpanRenderer.State> {
	private static final float[][] HEAP = {{6.0F, 7.0F}, {10.0F, 8.0F}, {8.0F, 10.5F}};
	private static final float[] TURNS = {20.0F, -35.0F, 70.0F};
	private static final float SCALE = 0.36F;
	private final ItemModelResolver itemModels;

	public static final class State extends BlockEntityRenderState {
		final ItemStackRenderState[] heap = {new ItemStackRenderState(), new ItemStackRenderState(), new ItemStackRenderState()};
		int count;
		Direction facing = Direction.NORTH;
	}

	public DustpanRenderer(BlockEntityRendererProvider.Context context) {
		this.itemModels = context.itemModelResolver();
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(DustpanBlockEntity pan, State state, float partialTick, Vec3 camera,
			ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
		BlockEntityRenderState.extractBase(pan, state, crumbling);
		state.facing = pan.getBlockState().hasProperty(DustpanBlock.FACING) ? pan.getBlockState().getValue(DustpanBlock.FACING) : Direction.NORTH;
		state.count = 0;
		for (int slot = 0; slot < pan.getContainerSize() && state.count < state.heap.length; slot++) {
			ItemStack stack = pan.getItem(slot);
			if (!stack.isEmpty()) {
				itemModels.updateForTopItem(state.heap[state.count], stack, ItemDisplayContext.FIXED, pan.getLevel(), null,
						(int) pan.getBlockPos().asLong() + slot);
				state.count++;
			}
		}
	}

	@Override
	public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
		for (int i = 0; i < state.count; i++) {
			if (state.heap[i].isEmpty()) {
				continue;
			}
			pose.pushPose();
			pose.translate(0.5F, 0.0F, 0.5F);
			pose.rotateDegrees(Axis.YP, -RockingChairRenderer.yRotation(state.facing));
			pose.translate(HEAP[i][0] / 16 - 0.5F, (0.9F + i * 0.4F) / 16, HEAP[i][1] / 16 - 0.5F);
			pose.rotateDegrees(Axis.YP, TURNS[i]);
			pose.rotateDegrees(Axis.XP, 90.0F);
			pose.scale(SCALE, SCALE, SCALE);
			state.heap[i].submit(pose, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
			pose.popPose();
		}
	}
}
