package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.agriculture.HearthOvenBlock;
import io.github.jimbozoomer.jugcraft.agriculture.HearthOvenBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.PieFilling;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The pie in a Hearth Oven, drawn for an oven facing north and turned to its facing: a round pie in its tin on the hearth
 * inside the mouth, its crust pale dough when it goes in, golden as it bakes and black once it burns, its filling showing
 * through the vent in the middle.
 */
public class HearthOvenRenderer implements BlockEntityRenderer<HearthOvenBlockEntity, HearthOvenRenderer.State> {
	private static final RenderType CRUST = RenderTypes.entityCutout(Jugcraft.id("textures/block/pie_crust.png"));
	private static final int DOUGH = 0xF0DCB4;
	private static final int GOLDEN = 0xD08A3A;
	private static final int BURNT = 0x2A1A10;

	public static final class State extends BlockEntityRenderState {
		Direction facing = Direction.NORTH;
		@Nullable PieFilling pie;
		int baked;
	}

	public HearthOvenRenderer(BlockEntityRendererProvider.Context context) {
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(HearthOvenBlockEntity oven, State state, float partialTick, Vec3 camera,
			ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
		BlockEntityRenderState.extractBase(oven, state, crumbling);
		state.facing = oven.getBlockState().hasProperty(HearthOvenBlock.FACING) ? oven.getBlockState().getValue(HearthOvenBlock.FACING)
				: Direction.NORTH;
		state.pie = oven.pie();
		state.baked = oven.baked();
	}

	/** The crust's colour after {@code baked} points: dough to golden by the time it is baked, golden to black as it burns. */
	static int crust(int baked) {
		if (baked <= HearthOvenBlockEntity.BAKED) {
			return mix(DOUGH, GOLDEN, (float) baked / HearthOvenBlockEntity.BAKED);
		}
		return mix(GOLDEN, BURNT, (float) (baked - HearthOvenBlockEntity.BAKED) / (HearthOvenBlockEntity.BURNT - HearthOvenBlockEntity.BAKED));
	}

	private static int mix(int from, int to, float t) {
		float k = Mth.clamp(t, 0.0F, 1.0F);
		int r = (int) Mth.lerp(k, (from >> 16) & 0xFF, (to >> 16) & 0xFF);
		int g = (int) Mth.lerp(k, (from >> 8) & 0xFF, (to >> 8) & 0xFF);
		int b = (int) Mth.lerp(k, from & 0xFF, to & 0xFF);
		return 0xFF000000 | r << 16 | g << 8 | b;
	}

	@Override
	public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
		PieFilling pie = state.pie;
		if (pie == null) {
			return;
		}
		int light = state.lightCoords;
		int crust = crust(state.baked);
		boolean burnt = state.baked >= HearthOvenBlockEntity.BURNT;
		int filling = burnt ? 0xFF1A100A : 0xFF000000 | pie.color;
		pose.pushPose();
		pose.translate(0.5F, 0.0F, 0.5F);
		pose.rotateDegrees(Axis.YP, -RockingChairRenderer.yRotation(state.facing));
		pose.translate(-0.5F, 0.0F, -0.5F);
		collector.submitCustomGeometry(pose, CRUST, (matrix, buffer) -> {
			// The pie, a rounded square of crust in two steps, and its filling in the vent.
			TintedBoxes.box(buffer, matrix, 5.5F, 2.0F, 6.5F, 10.5F, 3.25F, 11.5F, crust, light);
			TintedBoxes.box(buffer, matrix, 6.0F, 3.25F, 7.0F, 10.0F, 3.75F, 11.0F, crust, light);
			TintedBoxes.box(buffer, matrix, 7.25F, 3.75F, 8.25F, 8.75F, 3.85F, 9.75F, filling, light);
		});
		pose.popPose();
	}
}
