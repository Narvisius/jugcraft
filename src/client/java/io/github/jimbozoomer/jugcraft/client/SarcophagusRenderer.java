package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.jimbozoomer.jugcraft.agriculture.SarcophagusBlock;
import io.github.jimbozoomer.jugcraft.agriculture.SarcophagusBlockEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Draws a Stone Sarcophagus's lid (decor18_quads.json, one for each stone and carving) from its head half. Opening, the
 * heavy lid slides aside over {@value #OPEN_TICKS} ticks and tips down on that side; when the sarcophagus knocks it
 * jolts up and settles at each knock.
 */
public class SarcophagusRenderer implements BlockEntityRenderer<SarcophagusBlockEntity, SarcophagusRenderer.State> {
	public static final int OPEN_TICKS = 12;
	/** How far the lid slides (pixels) and tips (degrees) when fully open. */
	private static final float SLIDE = 9.0F;
	private static final float TIP = 10.0F;
	private static final int SHUDDER_TICKS = 8;

	public static final class State extends BlockEntityRenderState {
		String lid = "";
		Direction facing = Direction.NORTH;
		float open;
		float shudder;
	}

	public SarcophagusRenderer(BlockEntityRendererProvider.Context context) {
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(SarcophagusBlockEntity tomb, State state, float partialTick, Vec3 camera,
			ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
		BlockEntityRenderState.extractBase(tomb, state, crumbling);
		BlockState block = tomb.getBlockState();
		if (!(block.getBlock() instanceof SarcophagusBlock) || tomb.getLevel() == null) {
			return;
		}
		state.lid = BuiltInRegistries.BLOCK.getKey(block.getBlock()).getPath() + "_lid_" + block.getValue(SarcophagusBlock.LID).getSerializedName();
		state.facing = block.getValue(SarcophagusBlock.FACING);
		float now = tomb.getLevel().getGameTime() + partialTick;
		float target = block.getValue(SarcophagusBlock.OPEN) ? 1.0F : 0.0F;
		if (tomb.lidTime < 0) {
			tomb.lid = target;
		} else {
			float step = Mth.clamp(now - tomb.lidTime, 0.0F, OPEN_TICKS) / OPEN_TICKS;
			tomb.lid = tomb.lid < target ? Math.min(target, tomb.lid + step) : Math.max(target, tomb.lid - step);
		}
		tomb.lidTime = now;
		state.open = tomb.lid * tomb.lid * (3 - 2 * tomb.lid);
		float since = now - tomb.marked();
		state.shudder = since >= 0 && since < SHUDDER_TICKS ? Mth.sin(since / SHUDDER_TICKS * Mth.PI) : 0.0F;
	}

	@Override
	public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
		QuadModel lid = DecorQuads.get(state.lid);
		if (lid == null) {
			return;
		}
		pose.pushPose();
		pose.translate(0.5F, 0.0F, 0.5F);
		pose.rotateDegrees(Axis.YP, -RockingChairRenderer.yRotation(state.facing));
		pose.translate(-0.5F, 0.0F, -0.5F);
		float slide = SLIDE * state.open / 16;
		pose.translate(slide, (0.4F * state.open + 0.6F * state.shudder) / 16, 0.0F);
		// Tip it down on the side it slid to, about its edge there, and jolt it about its length when it knocks.
		float pivotX = 15.2F / 16, pivotY = 11.0F / 16;
		pose.translate(pivotX, pivotY, 1.0F);
		pose.rotateDegrees(Axis.ZP, -TIP * state.open);
		pose.rotateDegrees(Axis.XP, 1.5F * state.shudder);
		pose.translate(-pivotX, -pivotY, -1.0F);
		lid.submit(pose, collector, state.lightCoords);
		pose.popPose();
	}
}
