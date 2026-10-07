package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.agriculture.HalloweenBonfireBlock;
import io.github.jimbozoomer.jugcraft.agriculture.HalloweenBonfireBlockEntity;
import java.util.List;
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
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Draws the Halloween Bonfire's flames, three pairs of crossed sheets of fire round the log cone, each flickering and
 * swaying on its own, at full brightness, a block and a half tall; and the food on its four skewers, at the skewers' tips over
 * the fire. Nothing but the food while it is out.
 */
public class HalloweenBonfireRenderer implements BlockEntityRenderer<HalloweenBonfireBlockEntity, HalloweenBonfireRenderer.State> {
	private static final RenderType FLAME = RenderTypes.entityTranslucent(Jugcraft.id("textures/entity/bonfire_flame.png"));
	private static final int FULL_BRIGHT = 0xF000F0;
	/** The flames' sheets: width and height in pixels, and how far round each pair is turned. */
	private static final float[][] SHEETS = {{26.0F, 27.0F, 0.0F}, {22.0F, 23.0F, 30.0F}, {16.0F, 18.0F, 60.0F}};
	private static final float BASE_Y = 3.0F;
	/** Where the skewers' tips hold the food over the fire (pixels), one per skewer, round the cone. */
	private static final float[][] TIPS = {{8.0F, 10.0F, 4.2F}, {11.8F, 10.0F, 8.0F}, {8.0F, 10.0F, 11.8F}, {4.2F, 10.0F, 8.0F}};
	private static final float FOOD_SCALE = 0.35F;

	private final ItemModelResolver itemModels;

	public static final class State extends BlockEntityRenderState {
		boolean lit;
		float time;
		final ItemStackRenderState[] food = {new ItemStackRenderState(), new ItemStackRenderState(), new ItemStackRenderState(),
				new ItemStackRenderState()};
	}

	public HalloweenBonfireRenderer(BlockEntityRendererProvider.Context context) {
		this.itemModels = context.itemModelResolver();
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(HalloweenBonfireBlockEntity bonfire, State state, float partialTick, Vec3 camera,
			ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
		BlockEntityRenderState.extractBase(bonfire, state, crumbling);
		BlockState block = bonfire.getBlockState();
		Level level = bonfire.getLevel();
		state.lit = block.getBlock() instanceof HalloweenBonfireBlock && block.getValue(HalloweenBonfireBlock.LIT);
		state.time = level == null ? 0.0F : (float) ((level.getGameTime() + (double) partialTick) % 24000.0) + (bonfire.getBlockPos().hashCode() & 0xFF);
		List<ItemStack> items = bonfire.items();
		for (int i = 0; i < state.food.length; i++) {
			ItemStack stack = i < items.size() ? items.get(i) : ItemStack.EMPTY;
			itemModels.updateForTopItem(state.food[i], stack, ItemDisplayContext.FIXED, level, null, (int) bonfire.getBlockPos().asLong() + i);
		}
	}

	@Override
	public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
		for (int i = 0; i < state.food.length; i++) {
			if (state.food[i].isEmpty()) {
				continue;
			}
			pose.pushPose();
			pose.translate(TIPS[i][0] / 16, TIPS[i][1] / 16, TIPS[i][2] / 16);
			pose.rotateDegrees(Axis.YP, i * 90.0F);
			pose.scale(FOOD_SCALE, FOOD_SCALE, FOOD_SCALE);
			state.food[i].submit(pose, collector, state.lit ? FULL_BRIGHT : state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
			pose.popPose();
		}
		if (!state.lit) {
			return;
		}
		float time = state.time;
		collector.submitCustomGeometry(pose, FLAME, (matrix, buffer) -> {
			for (int i = 0; i < SHEETS.length; i++) {
				float flicker = 1.0F + 0.12F * Mth.sin(time * (0.8F + i * 0.3F) + i) + 0.06F * Mth.sin(time * 2.1F + i * 2.0F);
				float sway = 0.8F * Mth.sin(time * 0.27F + i * 1.7F);
				sheets(buffer, matrix, SHEETS[i][0], SHEETS[i][1] * flicker, sway, SHEETS[i][2]);
			}
		});
	}

	/**
	 * Two crossed sheets of flame turned {@code turn} degrees, each drawn from both sides (each side lifted off the middle,
	 * never one plane twice), tips leaning by {@code sway} pixels.
	 */
	private static void sheets(VertexConsumer buffer, PoseStack.Pose matrix, float width, float height, float sway, float turn) {
		float y0 = BASE_Y / 16;
		float y1 = (BASE_Y + height) / 16;
		float s = sway / 16;
		for (int pair = 0; pair < 2; pair++) {
			double angle = Math.toRadians(turn + pair * 90.0 + 45.0);
			float dx = (float) (Math.cos(angle) * width / 2 / 16);
			float dz = (float) (Math.sin(angle) * width / 2 / 16);
			float[][] front = {{0.5F - dx, y0, 0.5F - dz, 0, 1}, {0.5F - dx + s, y1, 0.5F - dz, 0, 0}, {0.5F + dx + s, y1, 0.5F + dz, 1, 0},
					{0.5F + dx, y0, 0.5F + dz, 1, 1}};
			float nx = (float) -Math.sin(angle);
			float nz = (float) Math.cos(angle);
			DecorDraw.twoSided(buffer, matrix, front, nx, 0, nz, 0xFFFFFFFF, FULL_BRIGHT, DecorDraw.TWO_SIDED_LIFT);
		}
	}
}
