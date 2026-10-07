package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.agriculture.BlackLightBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.DecorationBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.GlowPaintBlock;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Draws Glow Paint blazing out under a black light: its design again, green-white and at full brightness, just in front
 * of the faint paint, as bright as the nearest shining {@code BlackLightBlock} makes it
 * ({@link BlackLightBlockEntity#glowAt}).
 */
public class GlowPaintRenderer implements BlockEntityRenderer<DecorationBlockEntity, GlowPaintRenderer.State> {
	private static final int FULL_BRIGHT = 0xF000F0;
	private static final float OUT = 0.15F / 16;
	private static final Map<GlowPaintBlock.Design, RenderType> GLOWS = new EnumMap<>(GlowPaintBlock.Design.class);

	static {
		for (GlowPaintBlock.Design design : GlowPaintBlock.Design.values()) {
			GLOWS.put(design, RenderTypes.entityTranslucent(Jugcraft.id("textures/entity/glow_paint_" + design.getSerializedName() + "_glow.png")));
		}
	}

	public static final class State extends BlockEntityRenderState {
		Direction facing = Direction.NORTH;
		GlowPaintBlock.Design design = GlowPaintBlock.Design.SKULL;
		float glow;
	}

	public GlowPaintRenderer(BlockEntityRendererProvider.Context context) {
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(DecorationBlockEntity paint, State state, float partialTick, Vec3 camera,
			ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
		BlockEntityRenderState.extractBase(paint, state, crumbling);
		BlockState block = paint.getBlockState();
		Level level = paint.getLevel();
		state.glow = 0.0F;
		if (!(block.getBlock() instanceof GlowPaintBlock) || level == null) {
			return;
		}
		state.facing = block.getValue(GlowPaintBlock.FACING);
		state.design = block.getValue(GlowPaintBlock.DESIGN);
		state.glow = BlackLightBlockEntity.glowAt(level, paint.getBlockPos());
	}

	@Override
	public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
		if (state.glow <= 0.0F) {
			return;
		}
		int color = (Math.round(state.glow * 255) << 24) | 0xFFFFFF;
		pose.pushPose();
		Direction facing = state.facing;
		if (facing.getAxis().isHorizontal()) {
			pose.translate(0.5F, 0.0F, 0.5F);
			pose.rotateDegrees(Axis.YP, -RockingChairRenderer.yRotation(facing));
			pose.translate(-0.5F, 0.0F, -0.5F);
		}
		collector.submitCustomGeometry(pose, GLOWS.get(state.design), (matrix, buffer) -> {
			if (facing == Direction.UP) {
				float y = OUT;
				DecorDraw.quad(buffer, matrix, new float[][] {{0, y, 0, 0, 0}, {0, y, 1, 0, 1}, {1, y, 1, 1, 1}, {1, y, 0, 1, 0}}, 0, 1, 0, color, FULL_BRIGHT);
			} else if (facing == Direction.DOWN) {
				float y = 1 - OUT;
				DecorDraw.quad(buffer, matrix, new float[][] {{0, y, 1, 0, 0}, {0, y, 0, 0, 1}, {1, y, 0, 1, 1}, {1, y, 1, 1, 0}}, 0, -1, 0, color, FULL_BRIGHT);
			} else {
				float z = 1 - OUT;
				DecorDraw.quad(buffer, matrix, new float[][] {{0, 0, z, 1, 1}, {0, 1, z, 1, 0}, {1, 1, z, 0, 0}, {1, 0, z, 0, 1}}, 0, 0, -1, color, FULL_BRIGHT);
			}
		});
		pose.popPose();
	}
}
