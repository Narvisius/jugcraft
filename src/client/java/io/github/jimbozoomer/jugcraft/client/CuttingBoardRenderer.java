package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.jimbozoomer.jugcraft.agriculture.CuttingBoardBlock;
import io.github.jimbozoomer.jugcraft.agriculture.CuttingBoardBlockEntity;
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

/** What lies on a Cutting Board, flat in the middle of the board. */
public class CuttingBoardRenderer implements BlockEntityRenderer<CuttingBoardBlockEntity, CuttingBoardRenderer.State> {
	private static final float TOP = 1.05F;
	private static final float SCALE = 0.55F;

	private final ItemModelResolver itemModels;

	public static final class State extends BlockEntityRenderState {
		final ItemStackRenderState item = new ItemStackRenderState();
		Direction facing = Direction.NORTH;
	}

	public CuttingBoardRenderer(BlockEntityRendererProvider.Context context) {
		this.itemModels = context.itemModelResolver();
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(CuttingBoardBlockEntity board, State state, float partialTick, Vec3 camera,
			ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
		BlockEntityRenderState.extractBase(board, state, crumbling);
		state.facing = board.getBlockState().hasProperty(CuttingBoardBlock.FACING) ? board.getBlockState().getValue(CuttingBoardBlock.FACING)
				: Direction.NORTH;
		itemModels.updateForTopItem(state.item, board.item(), ItemDisplayContext.FIXED, board.getLevel(), null, (int) board.getBlockPos().asLong());
	}

	@Override
	public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
		if (!state.item.isEmpty()) {
			KitchenDraw.flat(pose, collector, state.item, state.facing, 7.0F, TOP, 8.0F, SCALE, 0.0F, state.lightCoords);
		}
	}
}
