package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.jimbozoomer.jugcraft.agriculture.SkilletBlock;
import io.github.jimbozoomer.jugcraft.agriculture.SkilletBlockEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * What is in a Skillet: the food frying in the middle of the pan (two pieces when there is more than one), and what has
 * been fried, heaped at the pan's far side.
 */
public class SkilletRenderer implements BlockEntityRenderer<SkilletBlockEntity, SkilletRenderer.State> {
	private static final float FLOOR = 1.05F;
	private static final float SCALE = 0.34F;

	private final ItemModelResolver itemModels;

	public static final class State extends BlockEntityRenderState {
		final ItemStackRenderState raw = new ItemStackRenderState();
		final ItemStackRenderState fried = new ItemStackRenderState();
		int rawCount;
		int friedCount;
		Direction facing = Direction.NORTH;
	}

	public SkilletRenderer(BlockEntityRendererProvider.Context context) {
		this.itemModels = context.itemModelResolver();
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(SkilletBlockEntity skillet, State state, float partialTick, Vec3 camera,
			ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
		BlockEntityRenderState.extractBase(skillet, state, crumbling);
		state.facing = skillet.getBlockState().hasProperty(SkilletBlock.FACING) ? skillet.getBlockState().getValue(SkilletBlock.FACING)
				: Direction.NORTH;
		state.rawCount = skillet.raw().getCount();
		state.friedCount = skillet.fried().getCount();
		int seed = (int) skillet.getBlockPos().asLong();
		itemModels.updateForTopItem(state.raw, skillet.raw(), ItemDisplayContext.FIXED, skillet.getLevel(), null, seed);
		itemModels.updateForTopItem(state.fried, skillet.fried(), ItemDisplayContext.FIXED, skillet.getLevel(), null, seed + 1);
	}

	@Override
	public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
		if (!state.raw.isEmpty()) {
			KitchenDraw.flat(pose, collector, state.raw, state.facing, 7.5F, FLOOR, 9.5F, SCALE, 20.0F, state.lightCoords);
			if (state.rawCount > 1) {
				KitchenDraw.flat(pose, collector, state.raw, state.facing, 9.0F, FLOOR + 0.3F, 8.0F, SCALE, -35.0F, state.lightCoords);
			}
		}
		if (!state.fried.isEmpty()) {
			float lift = state.raw.isEmpty() ? 0.0F : 0.6F;
			KitchenDraw.flat(pose, collector, state.fried, state.facing, 8.0F, FLOOR + lift, 11.5F, SCALE, -10.0F, state.lightCoords);
			if (state.friedCount > 1) {
				KitchenDraw.flat(pose, collector, state.fried, state.facing, 6.5F, FLOOR + lift + 0.3F, 12.0F, SCALE, 40.0F,
						state.lightCoords);
			}
		}
	}
}
