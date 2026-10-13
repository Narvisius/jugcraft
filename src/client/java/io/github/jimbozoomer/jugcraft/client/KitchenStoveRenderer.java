package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.jimbozoomer.jugcraft.agriculture.KitchenStoveBlock;
import io.github.jimbozoomer.jugcraft.agriculture.KitchenStoveBlockEntity;
import java.util.List;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The food on a Kitchen Stove's hob: up to six pieces lying on the grill in two rows of three, turned with the stove.
 */
public class KitchenStoveRenderer implements BlockEntityRenderer<KitchenStoveBlockEntity, KitchenStoveRenderer.State> {
	/** Where each piece lies on the grill (pixels, for a stove facing north), and how far round it is turned. */
	private static final float[][] PLACES = {{4.0F, 5.0F, 10.0F}, {8.0F, 5.0F, -20.0F}, {12.0F, 5.0F, 35.0F},
			{4.0F, 11.0F, -40.0F}, {8.0F, 11.0F, 15.0F}, {12.0F, 11.0F, -5.0F}};
	private static final float TOP = 16.05F;
	private static final float SCALE = 0.32F;

	private final ItemModelResolver itemModels;

	public static final class State extends BlockEntityRenderState {
		final ItemStackRenderState[] food = new ItemStackRenderState[KitchenStoveBlockEntity.SLOTS];
		Direction facing = Direction.NORTH;

		State() {
			for (int i = 0; i < food.length; i++) {
				food[i] = new ItemStackRenderState();
			}
		}
	}

	public KitchenStoveRenderer(BlockEntityRendererProvider.Context context) {
		this.itemModels = context.itemModelResolver();
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(KitchenStoveBlockEntity stove, State state, float partialTick, Vec3 camera,
			ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
		BlockEntityRenderState.extractBase(stove, state, crumbling);
		BlockState block = stove.getBlockState();
		state.facing = block.hasProperty(KitchenStoveBlock.FACING) ? block.getValue(KitchenStoveBlock.FACING) : Direction.NORTH;
		List<ItemStack> items = stove.items();
		for (int i = 0; i < state.food.length; i++) {
			ItemStack stack = i < items.size() ? items.get(i) : ItemStack.EMPTY;
			itemModels.updateForTopItem(state.food[i], stack, ItemDisplayContext.FIXED, stove.getLevel(), null, (int) stove.getBlockPos().asLong() + i);
		}
	}

	@Override
	public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
		for (int i = 0; i < state.food.length; i++) {
			if (!state.food[i].isEmpty()) {
				KitchenDraw.flat(pose, collector, state.food[i], state.facing, PLACES[i][0], TOP, PLACES[i][1], SCALE, PLACES[i][2],
						state.lightCoords);
			}
		}
	}
}
