package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.jimbozoomer.jugcraft.agriculture.DecorationBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.MourningAngelBlock;
import io.github.jimbozoomer.jugcraft.agriculture.RockingChairBlock;
import io.github.jimbozoomer.jugcraft.agriculture.Seat;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Draws the Rocking Chair, turned to its facing and tipped about the middle of its runners by
 * {@link RockingChairBlock#rock}: gently under a sitter, on its own at night, still by day.
 */
public class RockingChairRenderer implements BlockEntityRenderer<DecorationBlockEntity, RockingChairRenderer.State> {
	/** Where the runners touch the floor, in blocks. */
	private static final float PIVOT_Y = 1.0F / 16;

	public static final class State extends BlockEntityRenderState {
		Direction facing = Direction.NORTH;
		float rock;
	}

	public RockingChairRenderer(BlockEntityRendererProvider.Context context) {
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(DecorationBlockEntity chair, State state, float partialTick, Vec3 camera,
			ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
		BlockEntityRenderState.extractBase(chair, state, crumbling);
		BlockState block = chair.getBlockState();
		state.rock = 0.0F;
		if (!(block.getBlock() instanceof RockingChairBlock) || chair.getLevel() == null) {
			return;
		}
		state.facing = block.getValue(RockingChairBlock.FACING);
		boolean occupied = !Seat.at(chair.getLevel(), chair.getBlockPos()).isEmpty();
		state.rock = RockingChairBlock.rock(chair.getBlockPos(), occupied, MourningAngelBlock.night(chair.getLevel()),
				chair.getLevel().getGameTime() + partialTick);
	}

	@Override
	public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
		QuadModel chair = DecorQuads.get("rocking_chair");
		if (chair == null) {
			return;
		}
		pose.pushPose();
		pose.translate(0.5F, 0.0F, 0.5F);
		pose.rotateDegrees(Axis.YP, -yRotation(state.facing));
		pose.translate(0.0F, PIVOT_Y, 0.0F);
		pose.rotateDegrees(Axis.XP, state.rock);
		pose.translate(-0.5F, -PIVOT_Y, -0.5F);
		chair.submit(pose, collector, state.lightCoords);
		pose.popPose();
	}

	static float yRotation(Direction facing) {
		return switch (facing) {
			case EAST -> 90;
			case SOUTH -> 180;
			case WEST -> 270;
			default -> 0;
		};
	}

	@Override
	public boolean shouldRenderOffScreen() {
		return true; // Its back rises above its block.
	}
}
