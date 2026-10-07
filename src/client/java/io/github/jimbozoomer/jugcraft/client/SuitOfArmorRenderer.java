package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.agriculture.DecorationBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.MourningAngelBlock;
import io.github.jimbozoomer.jugcraft.agriculture.SuitOfArmorBlock;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Draws the Suit of Armor's helmet on its upper half, turned toward the nearest player within
 * {@link SuitOfArmorBlock#WATCH_RANGE} blocks ({@link SuitOfArmorBlock#watchYaw}), slowly
 * ({@link SuitOfArmorBlock#TURN_SPEED} degrees a tick); at night a red glow in the visor. Every client works out the
 * same nearest player, so everyone sees the suit watch the same one.
 */
public class SuitOfArmorRenderer implements BlockEntityRenderer<DecorationBlockEntity, SuitOfArmorRenderer.State> {
	private static final RenderType GLOW = RenderTypes.entityCutout(Jugcraft.id("textures/entity/suit_of_armor_glow.png"));
	private static final int FULL_BRIGHT = 0xF000F0;
	/** The pivot of the head (the top of the gorget), in pixels of the upper block. */
	private static final float PIVOT_Y = 9.0F;
	/** Each suit's helmet: {yaw, the game time it was last drawn}. */
	private final Map<DecorationBlockEntity, double[]> heads = new WeakHashMap<>();

	public static final class State extends BlockEntityRenderState {
		Direction facing = Direction.NORTH;
		float yaw;
		boolean night;
	}

	public SuitOfArmorRenderer(BlockEntityRendererProvider.Context context) {
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(DecorationBlockEntity suit, State state, float partialTick, Vec3 camera,
			ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
		BlockEntityRenderState.extractBase(suit, state, crumbling);
		BlockState block = suit.getBlockState();
		Level level = suit.getLevel();
		if (!(block.getBlock() instanceof SuitOfArmorBlock) || level == null) {
			return;
		}
		state.facing = block.getValue(SuitOfArmorBlock.FACING);
		state.night = MourningAngelBlock.night(level);
		Vec3 head = Vec3.atBottomCenterOf(suit.getBlockPos()).add(0.0, PIVOT_Y / 16, 0.0);
		Vec3 nearest = null;
		double best = SuitOfArmorBlock.WATCH_RANGE * SuitOfArmorBlock.WATCH_RANGE;
		for (Player player : level.players()) {
			Vec3 eyes = player.getEyePosition(partialTick);
			double distance = eyes.distanceToSqr(head);
			if (!player.isSpectator() && distance <= best) {
				best = distance;
				nearest = eyes;
			}
		}
		float target = SuitOfArmorBlock.watchYaw(head, state.facing, nearest);
		double now = level.getGameTime() + (double) partialTick;
		double[] look = heads.computeIfAbsent(suit, s -> new double[] {target, now});
		float elapsed = (float) Math.max(0.0, Math.min(20.0, now - look[1]));
		look[0] = SuitOfArmorBlock.turnToward((float) look[0], target, SuitOfArmorBlock.TURN_SPEED * elapsed);
		look[1] = now;
		state.yaw = (float) look[0];
	}

	@Override
	public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
		QuadModel helmet = DecorQuads.get("suit_of_armor_helmet");
		pose.pushPose();
		pose.translate(0.5F, PIVOT_Y / 16, 0.5F);
		pose.rotateDegrees(Axis.YP, -RockingChairRenderer.yRotation(state.facing) - state.yaw);
		pose.translate(-0.5F, -PIVOT_Y / 16, -0.5F);
		if (helmet != null) {
			helmet.submit(pose, collector, state.lightCoords);
		}
		if (state.night) {
			collector.submitCustomGeometry(pose, GLOW, (matrix, buffer) -> {
				float z = 4.45F / 16;
				float x0 = 5.5F / 16;
				float x1 = 10.5F / 16;
				float y0 = 11.6F / 16;
				float y1 = 12.4F / 16;
				DecorDraw.quad(buffer, matrix, new float[][] {{x0, y0, z, 1, 1}, {x0, y1, z, 1, 0}, {x1, y1, z, 0, 0}, {x1, y0, z, 0, 1}},
						0, 0, -1, 0xFFFFFFFF, FULL_BRIGHT);
			});
		}
		pose.popPose();
	}
}
