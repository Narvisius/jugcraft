package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.jimbozoomer.jugcraft.logistics.ConveyorBlock;
import io.github.jimbozoomer.jugcraft.logistics.ConveyorBlockEntity;
import java.util.ArrayList;
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
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/** Draws the stacks riding a conveyor, upright like dropped items, gliding between ticks (and climbing on slopes). */
public class ConveyorRenderer implements BlockEntityRenderer<ConveyorBlockEntity, ConveyorRenderer.State> {
	/** Height of the belt surface (blocks). */
	private static final float BELT_TOP = 5 / 16.0F;

	private final ItemModelResolver itemModels;

	public static final class State extends BlockEntityRenderState {
		final List<ItemStackRenderState> items = new ArrayList<>();
		final List<Float> progress = new ArrayList<>();
		final List<Float> rise = new ArrayList<>();
		Direction facing = Direction.NORTH;
	}

	public ConveyorRenderer(BlockEntityRendererProvider.Context context) {
		this.itemModels = context.itemModelResolver();
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(ConveyorBlockEntity conveyor, State state, float partialTick, Vec3 camera,
			ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
		BlockEntityRenderState.extractBase(conveyor, state, crumbling);
		state.facing = conveyor.getBlockState().getValue(ConveyorBlock.FACING);
		state.items.clear();
		state.progress.clear();
		state.rise.clear();
		int seed = (int) conveyor.getBlockPos().asLong();
		for (ConveyorBlockEntity.Entry entry : conveyor.items()) {
			ItemStackRenderState item = new ItemStackRenderState();
			itemModels.updateForTopItem(item, entry.stack, ItemDisplayContext.GROUND, conveyor.getLevel(), null, seed++);
			state.items.add(item);
			float progress = Mth.lerp(partialTick, entry.previous, entry.progress);
			state.progress.add(progress);
			state.rise.add(conveyor.riseAt(progress));
		}
	}

	@Override
	public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
		for (int i = 0; i < state.items.size(); i++) {
			ItemStackRenderState item = state.items.get(i);
			if (item.isEmpty()) {
				continue;
			}
			float along = state.progress.get(i) - 0.5F;
			pose.pushPose();
			pose.translate(0.5F + along * state.facing.getStepX(), BELT_TOP + state.rise.get(i), 0.5F + along * state.facing.getStepZ());
			pose.rotateDegrees(Axis.YP, -state.facing.toYRot());
			item.submit(pose, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
			pose.popPose();
		}
	}
}
