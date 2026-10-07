package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.jimbozoomer.jugcraft.agriculture.HauntedDiningChairBlock;
import io.github.jimbozoomer.jugcraft.agriculture.HauntedDiningChairBlockEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Draws the Haunted Dining Chair (decor19_quads.json: {@code haunted_dining_chair}, facing north) where it has slid to:
 * out from its table by up to {@value HauntedDiningChairBlock#SLIDE} pixels while {@link HauntedDiningChairBlock#OUT},
 * easing there and back over about half a second, with a little judder as it goes.
 */
public class DiningChairRenderer implements BlockEntityRenderer<HauntedDiningChairBlockEntity, DiningChairRenderer.State> {
	private static final float EASE_TICKS = 10.0F;

	public static final class State extends BlockEntityRenderState {
		Direction facing = Direction.NORTH;
		float out;
		float judder;
	}

	public DiningChairRenderer(BlockEntityRendererProvider.Context context) {
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(HauntedDiningChairBlockEntity chair, State state, float partialTick, Vec3 camera,
			ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
		BlockEntityRenderState.extractBase(chair, state, crumbling);
		BlockState block = chair.getBlockState();
		if (!(block.getBlock() instanceof HauntedDiningChairBlock) || chair.getLevel() == null) {
			return;
		}
		state.facing = block.getValue(HauntedDiningChairBlock.FACING);
		float target = block.getValue(HauntedDiningChairBlock.OUT) ? 1.0F : 0.0F;
		float now = Math.floorMod(chair.getLevel().getGameTime(), 1L << 20) + partialTick;
		if (chair.drawnTime < 0) {
			chair.drawnOut = target;
		} else {
			float step = Mth.clamp(now - chair.drawnTime, 0.0F, EASE_TICKS) / EASE_TICKS;
			chair.drawnOut = chair.drawnOut < target ? Math.min(target, chair.drawnOut + step) : Math.max(target, chair.drawnOut - step);
		}
		chair.drawnTime = now;
		state.out = chair.drawnOut * chair.drawnOut * (3 - 2 * chair.drawnOut);
		boolean moving = chair.drawnOut > 0.0F && chair.drawnOut < 1.0F;
		state.judder = moving ? 0.6F * Mth.sin(now * 3.1F) : 0.0F;
	}

	@Override
	public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
		QuadModel chair = DecorQuads.get("haunted_dining_chair");
		if (chair == null) {
			return;
		}
		pose.pushPose();
		pose.translate(0.5F, 0.0F, 0.5F);
		pose.rotateDegrees(Axis.YP, -RockingChairRenderer.yRotation(state.facing));
		// Away from the table, which it faces (north in its own frame), and a judder about its legs.
		pose.translate(0.0F, 0.0F, state.out * HauntedDiningChairBlock.SLIDE / 16);
		pose.rotateDegrees(Axis.YP, state.judder);
		pose.translate(-0.5F, 0.0F, -0.5F);
		chair.submit(pose, collector, state.lightCoords);
		pose.popPose();
	}
}
