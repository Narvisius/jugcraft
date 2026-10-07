package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.jimbozoomer.jugcraft.agriculture.ScarecrowBlock;
import io.github.jimbozoomer.jugcraft.agriculture.ScarecrowBlockEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Draws the head a Scarecrow wears on its shoulders, the way an armor stand wears a pumpkin: a pumpkin ten pixels a
 * side (bigger than a head) sitting on the post one pixel below the shirt's collar, its face turned the way the
 * scarecrow faces, drawn as {@link CarvedHead} draws a worn pumpkin.
 */
public class ScarecrowRenderer implements BlockEntityRenderer<ScarecrowBlockEntity, ScarecrowRenderer.State> {
	/** The head's size: an armor stand's pumpkin (a block drawn at 0.625 of a block). */
	private static final float HEAD = 10.0F / 16.0F;
	/** Where the bottom of the head sits in the upper half: a pixel below the top of the shirt (11 pixels up). */
	private static final float BOTTOM = 10.0F / 16.0F;

	private final ItemModelResolver itemModels;

	public static final class State extends BlockEntityRenderState {
		final CarvedHead head = new CarvedHead();
		Direction facing = Direction.NORTH;
	}

	public ScarecrowRenderer(BlockEntityRendererProvider.Context context) {
		this.itemModels = context.itemModelResolver();
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(ScarecrowBlockEntity scarecrow, State state, float partialTick, Vec3 camera,
			ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
		BlockEntityRenderState.extractBase(scarecrow, state, crumbling);
		state.facing = scarecrow.getBlockState().getValue(ScarecrowBlock.FACING);
		state.head.extract(itemModels, scarecrow.head(), scarecrow.getLevel(), (int) scarecrow.getBlockPos().asLong(), state.lightCoords);
	}

	@Override
	public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
		pose.pushPose();
		pose.translate(0.5F, BOTTOM + HEAD / 2.0F, 0.5F);
		state.head.submit(pose, collector, state.facing, HEAD, state.lightCoords);
		pose.popPose();
	}
}
