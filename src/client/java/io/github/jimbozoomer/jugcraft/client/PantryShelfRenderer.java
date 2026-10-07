package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.jimbozoomer.jugcraft.agriculture.PantryShelfBlock;
import io.github.jimbozoomer.jugcraft.agriculture.PantryShelfBlockEntity;
import java.util.List;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/** The jars on a Pantry Shelf, three on the upper shelf and three below, drawn for a shelf facing north and turned to its facing. */
public class PantryShelfRenderer implements BlockEntityRenderer<PantryShelfBlockEntity, PantryShelfRenderer.State> {
	private static final float[][] SPOTS = {{3.5F, 8.5F, 7.5F}, {8.0F, 8.5F, 7.5F}, {12.5F, 8.5F, 7.5F},
			{3.5F, 1.0F, 7.5F}, {8.0F, 1.0F, 7.5F}, {12.5F, 1.0F, 7.5F}};

	public static final class State extends BlockEntityRenderState {
		List<ItemStack> jars = List.of();
		Direction facing = Direction.NORTH;
	}

	public PantryShelfRenderer(BlockEntityRendererProvider.Context context) {
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(PantryShelfBlockEntity shelf, State state, float partialTick, Vec3 camera,
			ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
		BlockEntityRenderState.extractBase(shelf, state, crumbling);
		state.jars = shelf.jars();
		state.facing = shelf.getBlockState().hasProperty(PantryShelfBlock.FACING) ? shelf.getBlockState().getValue(PantryShelfBlock.FACING)
				: Direction.NORTH;
	}

	@Override
	public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
		pose.pushPose();
		pose.translate(0.5F, 0.0F, 0.5F);
		pose.rotateDegrees(Axis.YP, -RockingChairRenderer.yRotation(state.facing));
		pose.translate(-0.5F, 0.0F, -0.5F);
		PreserveJars.submit(collector, pose, state.jars, SPOTS, 3.5F, 5.0F, state.lightCoords);
		pose.popPose();
	}
}
