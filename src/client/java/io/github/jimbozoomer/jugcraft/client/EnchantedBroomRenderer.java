package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.jimbozoomer.jugcraft.agriculture.EnchantedBroomBlock;
import io.github.jimbozoomer.jugcraft.agriculture.EnchantedBroomBlockEntity;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Draws the Enchanted Broom standing on its bristles. Asleep it stands still; charged, it sways on its own and its
 * bristle-tips glow; sweeping, it leans toward the nearest dropped item and swishes back and forth. Model from
 * decor17_quads.json (tools/decor17_data.py), standing about (8, 0, 8).
 */
public class EnchantedBroomRenderer implements BlockEntityRenderer<EnchantedBroomBlockEntity, EnchantedBroomRenderer.State> {
	private static final int FULL_BRIGHT = 0xF000F0;
	private static final float LEAN = 18.0F;
	private static final float SWISH = 14.0F;
	/** Where each broom last saw the nearest item from: {yaw, game time}. */
	private final Map<EnchantedBroomBlockEntity, double[]> aims = new WeakHashMap<>();

	public static final class State extends BlockEntityRenderState {
		boolean charged;
		boolean sweeping;
		float time;
		float yaw;
		int seed;
	}

	public EnchantedBroomRenderer(BlockEntityRendererProvider.Context context) {
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(EnchantedBroomBlockEntity broom, State state, float partialTick, Vec3 camera,
			ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
		BlockEntityRenderState.extractBase(broom, state, crumbling);
		BlockState block = broom.getBlockState();
		Level level = broom.getLevel();
		state.charged = block.hasProperty(EnchantedBroomBlock.CHARGED) && block.getValue(EnchantedBroomBlock.CHARGED);
		state.sweeping = state.charged && block.getValue(EnchantedBroomBlock.SWEEPING);
		state.time = level == null ? 0 : (level.getGameTime() % 24000) + partialTick;
		state.seed = broom.getBlockPos().hashCode();
		if (state.sweeping && level != null) {
			double[] aim = aims.computeIfAbsent(broom, b -> new double[] {0.0, -100.0});
			if (level.getGameTime() - aim[1] >= 10) {
				aim[0] = nearestYaw(level, broom.getBlockPos(), aim[0]);
				aim[1] = level.getGameTime();
			}
			state.yaw = (float) aim[0];
		}
	}

	/** The way to the nearest dropped item within its reach, as a turn about y (degrees), or {@code fallback}. */
	private static double nearestYaw(Level level, BlockPos pos, double fallback) {
		Vec3 centre = Vec3.atBottomCenterOf(pos);
		List<ItemEntity> items = level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(EnchantedBroomBlock.RANGE, 1, EnchantedBroomBlock.RANGE));
		ItemEntity nearest = null;
		double best = Double.MAX_VALUE;
		for (ItemEntity item : items) {
			double d = item.position().distanceToSqr(centre);
			if (d < best) {
				best = d;
				nearest = item;
			}
		}
		if (nearest == null) {
			return fallback;
		}
		Vec3 to = nearest.position().subtract(centre);
		return Math.toDegrees(Math.atan2(-to.x, -to.z));
	}

	@Override
	public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
		QuadModel broom = DecorQuads.get("enchanted_broom");
		QuadModel glow = DecorQuads.get("enchanted_broom_glow");
		pose.pushPose();
		pose.translate(0.5F, 0.0F, 0.5F);
		if (state.sweeping) {
			// Lean toward the item (the model's front is north), swishing the bristles side to side.
			pose.rotateDegrees(Axis.YP, state.yaw);
			pose.rotateDegrees(Axis.XP, -LEAN);
			pose.rotateDegrees(Axis.ZP, SWISH * Mth.sin(state.time * 0.55F));
		} else if (state.charged) {
			float phase = (state.seed & 0xFF) / 40.0F;
			pose.rotateDegrees(Axis.XP, 3.0F * Mth.sin(state.time * 0.05F + phase));
			pose.rotateDegrees(Axis.ZP, 3.0F * Mth.cos(state.time * 0.04F + phase));
		}
		pose.translate(-0.5F, 0.0F, -0.5F);
		if (broom != null) {
			broom.submit(pose, collector, state.lightCoords);
		}
		if (state.charged && glow != null) {
			glow.submit(pose, collector, FULL_BRIGHT);
		}
		pose.popPose();
	}
}
