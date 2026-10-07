package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.agriculture.FeastTableBlock;
import io.github.jimbozoomer.jugcraft.agriculture.FeastTableBlockEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The dishes on a Harvest Feast Table: each on a cream plate on the runner, the food lying on it, heaped higher the more
 * servings are left (one, two or three pieces for a few, some or many).
 */
public class FeastTableRenderer implements BlockEntityRenderer<FeastTableBlockEntity, FeastTableRenderer.State> {
	private static final RenderType PLATE = RenderTypes.entityCutout(Jugcraft.id("textures/entity/feast_plate.png"));
	private static final float TOP = 13.25F;
	private static final float SCALE = 0.42F;
	private static final float[][] HEAP = {{0.0F, 0.0F, 0.0F}, {-0.06F, 0.03F, 35.0F}, {0.05F, 0.06F, -50.0F}};

	private final ItemModelResolver itemModels;

	public static final class State extends BlockEntityRenderState {
		final ItemStackRenderState[] dishes = {new ItemStackRenderState(), new ItemStackRenderState()};
		final int[] pieces = new int[FeastTableBlockEntity.DISHES];
		Direction.Axis axis = Direction.Axis.X;
	}

	public FeastTableRenderer(BlockEntityRendererProvider.Context context) {
		this.itemModels = context.itemModelResolver();
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(FeastTableBlockEntity table, State state, float partialTick, Vec3 camera,
			ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
		BlockEntityRenderState.extractBase(table, state, crumbling);
		state.axis = table.getBlockState().hasProperty(FeastTableBlock.AXIS) ? table.getBlockState().getValue(FeastTableBlock.AXIS) : Direction.Axis.X;
		for (int i = 0; i < FeastTableBlockEntity.DISHES; i++) {
			ItemStack dish = table.dish(i);
			state.pieces[i] = dish.isEmpty() ? 0 : dish.getCount() >= 6 ? 3 : dish.getCount() >= 3 ? 2 : 1;
			itemModels.updateForTopItem(state.dishes[i], dish, ItemDisplayContext.FIXED, table.getLevel(), null, (int) table.getBlockPos().asLong() + i);
		}
	}

	@Override
	public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
		int light = state.lightCoords;
		for (int i = 0; i < FeastTableBlockEntity.DISHES; i++) {
			// Dish 0 is at the length's negative end along the table, dish 1 at its positive end.
			float along = i == 0 ? 4.0F : 12.0F;
			float x = state.axis == Direction.Axis.X ? along : 8.0F;
			float z = state.axis == Direction.Axis.X ? 8.0F : along;
			collector.submitCustomGeometry(pose, PLATE, (matrix, buffer) -> TintedBoxes.box(buffer, matrix, x - 3.0F, TOP, z - 3.0F, x + 3.0F, TOP + 0.5F,
					z + 3.0F, 0xFFFFFFFF, light));
			if (state.pieces[i] == 0 || state.dishes[i].isEmpty()) {
				continue;
			}
			for (int piece = 0; piece < state.pieces[i]; piece++) {
				pose.pushPose();
				pose.translate(x / 16.0F + HEAP[piece][0], (TOP + 0.5F) / 16.0F + 0.02F + HEAP[piece][1], z / 16.0F - HEAP[piece][0]);
				pose.rotateDegrees(Axis.YP, HEAP[piece][2] + (state.axis == Direction.Axis.Z ? 90.0F : 0.0F));
				pose.rotateDegrees(Axis.XP, 90.0F);
				pose.scale(SCALE, SCALE, SCALE);
				state.dishes[i].submit(pose, collector, light, OverlayTexture.NO_OVERLAY, 0);
				pose.popPose();
			}
		}
	}
}
