package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.jimbozoomer.jugcraft.agriculture.GargoyleSentinelBlock;
import io.github.jimbozoomer.jugcraft.agriculture.GargoyleSentinelBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.SuitOfArmorBlock;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Draws the Gargoyle Sentinel's head (decor18_quads.json), turning about its neck to follow the hostile mob the server
 * named, at most {@link GargoyleSentinelBlock#TURN_SPEED} degrees a tick and {@link GargoyleSentinelBlock#MAX_TURN} either
 * way, back to straight ahead with none; and its eyes, which glow brighter the nearer the mob (its signal).
 */
public class GargoyleSentinelRenderer implements BlockEntityRenderer<GargoyleSentinelBlockEntity, GargoyleSentinelRenderer.State> {
	private static final float NECK_X = 8.0F;
	private static final float NECK_Z = 7.0F;
	private static final int FULL_BRIGHT = 0xF000F0;

	public static final class State extends BlockEntityRenderState {
		Direction facing = Direction.NORTH;
		float yaw;
		int power;
	}

	public GargoyleSentinelRenderer(BlockEntityRendererProvider.Context context) {
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(GargoyleSentinelBlockEntity sentinel, State state, float partialTick, Vec3 camera,
			ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
		BlockEntityRenderState.extractBase(sentinel, state, crumbling);
		BlockState block = sentinel.getBlockState();
		if (!(block.getBlock() instanceof GargoyleSentinelBlock) || sentinel.getLevel() == null) {
			return;
		}
		state.facing = block.getValue(GargoyleSentinelBlock.FACING);
		state.power = block.getValue(GargoyleSentinelBlock.POWER);
		Entity target = sentinel.target() < 0 ? null : sentinel.getLevel().getEntity(sentinel.target());
		Vec3 head = Vec3.atCenterOf(sentinel.getBlockPos()).add(0.0, 0.5, 0.0);
		float wanted = SuitOfArmorBlock.watchYaw(head, state.facing, target == null ? null : target.getEyePosition(partialTick));
		wanted = Mth.clamp(wanted, -GargoyleSentinelBlock.MAX_TURN, GargoyleSentinelBlock.MAX_TURN);
		sentinel.headYaw = SuitOfArmorBlock.turnToward(sentinel.headYaw, wanted, GargoyleSentinelBlock.TURN_SPEED * 0.25F);
		state.yaw = sentinel.headYaw;
	}

	@Override
	public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
		pose.pushPose();
		pose.translate(0.5F, 0.0F, 0.5F);
		pose.rotateDegrees(Axis.YP, -RockingChairRenderer.yRotation(state.facing));
		pose.translate(-0.5F, 0.0F, -0.5F);
		pose.translate(NECK_X / 16, 0.0F, NECK_Z / 16);
		pose.rotateDegrees(Axis.YP, -state.yaw);
		pose.translate(-NECK_X / 16, 0.0F, -NECK_Z / 16);
		QuadModel head = DecorQuads.get("gargoyle_sentinel_head");
		if (head != null) {
			head.submit(pose, collector, state.lightCoords);
		}
		QuadModel eyes = DecorQuads.get("gargoyle_sentinel_eyes");
		if (eyes != null && state.power > 0) {
			int v = 80 + state.power * 11;
			eyes.submit(pose, collector, FULL_BRIGHT, 0xFF000000 | v << 16 | v << 8 | v);
		}
		pose.popPose();
	}
}
