package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.jimbozoomer.jugcraft.agriculture.OfrendaBlock;
import io.github.jimbozoomer.jugcraft.agriculture.OfrendaBlockEntity;
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
 * The offerings on an ofrenda, standing on its tiers facing whoever stands before it: two on the top tier, two on the
 * middle and two on the bottom, in the order they were set out. Drawn for an ofrenda facing north (its front low tier to
 * the north) and turned to its facing.
 */
public class OfrendaRenderer implements BlockEntityRenderer<OfrendaBlockEntity, OfrendaRenderer.State> {
	/** Where each offering stands, in pixels: {x, the tier's top, z}. */
	static final float[][] PLACES = {{4.5F, 15.0F, 13.0F}, {11.5F, 15.0F, 13.0F}, {4.5F, 10.0F, 7.5F}, {11.5F, 10.0F, 7.5F},
			{4.5F, 5.0F, 2.5F}, {11.5F, 5.0F, 2.5F}};
	private static final float SCALE = 0.36F;

	private final ItemModelResolver itemModels;

	public static final class State extends BlockEntityRenderState {
		final ItemStackRenderState[] offerings = new ItemStackRenderState[OfrendaBlockEntity.SLOTS];
		Direction facing = Direction.NORTH;

		State() {
			for (int i = 0; i < offerings.length; i++) {
				offerings[i] = new ItemStackRenderState();
			}
		}
	}

	public OfrendaRenderer(BlockEntityRendererProvider.Context context) {
		this.itemModels = context.itemModelResolver();
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(OfrendaBlockEntity ofrenda, State state, float partialTick, Vec3 camera,
			ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
		BlockEntityRenderState.extractBase(ofrenda, state, crumbling);
		state.facing = ofrenda.getBlockState().hasProperty(OfrendaBlock.FACING) ? ofrenda.getBlockState().getValue(OfrendaBlock.FACING)
				: Direction.NORTH;
		List<ItemStack> offerings = ofrenda.offerings();
		for (int i = 0; i < OfrendaBlockEntity.SLOTS; i++) {
			ItemStack offering = i < offerings.size() ? offerings.get(i) : ItemStack.EMPTY;
			itemModels.updateForTopItem(state.offerings[i], offering, ItemDisplayContext.FIXED, ofrenda.getLevel(), null,
					(int) ofrenda.getBlockPos().asLong() + i);
		}
	}

	@Override
	public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
		pose.pushPose();
		pose.translate(0.5F, 0.0F, 0.5F);
		pose.rotateDegrees(Axis.YP, -RockingChairRenderer.yRotation(state.facing));
		pose.translate(-0.5F, 0.0F, -0.5F);
		for (int i = 0; i < OfrendaBlockEntity.SLOTS; i++) {
			if (state.offerings[i].isEmpty()) {
				continue;
			}
			float[] place = PLACES[i];
			pose.pushPose();
			// Stand it on the tier, its face to the front (north).
			pose.translate(place[0] / 16.0F, place[1] / 16.0F + SCALE / 2.0F, place[2] / 16.0F);
			pose.rotateDegrees(Axis.YP, 180.0F);
			pose.scale(SCALE, SCALE, SCALE);
			state.offerings[i].submit(pose, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
			pose.popPose();
		}
		pose.popPose();
	}
}
