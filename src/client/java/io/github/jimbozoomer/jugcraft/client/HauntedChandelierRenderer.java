package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.agriculture.DecorationBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.HauntedChandelierBlock;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Draws the Haunted Chandelier swinging slowly from its chain ({@link HauntedChandelierBlock#sway}), with a flickering
 * flame, at full brightness, on each burning candle.
 */
public class HauntedChandelierRenderer implements BlockEntityRenderer<DecorationBlockEntity, HauntedChandelierRenderer.State> {
	private static final RenderType FLAME = RenderTypes.entityCutout(Jugcraft.id("textures/entity/haunted_chandelier_flame.png"));
	private static final int FULL_BRIGHT = 0xF000F0;
	private static final float FLAME_WIDTH = 1.6F / 16;
	private static final float FLAME_HEIGHT = 2.6F / 16;

	public static final class State extends BlockEntityRenderState {
		int burning;
		float swayX;
		float swayZ;
		float time;
	}

	public HauntedChandelierRenderer(BlockEntityRendererProvider.Context context) {
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(DecorationBlockEntity chandelier, State state, float partialTick, Vec3 camera,
			ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
		BlockEntityRenderState.extractBase(chandelier, state, crumbling);
		BlockState block = chandelier.getBlockState();
		state.burning = 0;
		if (!(block.getBlock() instanceof HauntedChandelierBlock) || chandelier.getLevel() == null) {
			return;
		}
		state.burning = block.getValue(HauntedChandelierBlock.BURNING);
		state.time = chandelier.getLevel().getGameTime() % 24000 + partialTick;
		float[] sway = HauntedChandelierBlock.sway(chandelier.getBlockPos(), state.time);
		state.swayX = sway[0];
		state.swayZ = sway[1];
	}

	@Override
	public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
		QuadModel chandelier = DecorQuads.get("haunted_chandelier");
		pose.pushPose();
		pose.translate(0.5F, 1.0F, 0.5F);
		pose.rotateDegrees(Axis.XP, state.swayX);
		pose.rotateDegrees(Axis.ZP, state.swayZ);
		pose.translate(-0.5F, -1.0F, -0.5F);
		if (chandelier != null) {
			chandelier.submit(pose, collector, state.lightCoords);
		}
		if (state.burning > 0) {
			int burning = state.burning;
			float time = state.time;
			collector.submitCustomGeometry(pose, FLAME, (matrix, buffer) -> {
				for (int i = 0; i < burning; i++) {
					float[] wick = HauntedChandelierBlock.wick(i);
					float x = wick[0] / 16;
					float y = wick[1] / 16;
					float z = wick[2] / 16;
					float h = FLAME_HEIGHT * (0.85F + 0.15F * Mth.sin(time * 0.9F + i * 2.1F));
					float w = FLAME_WIDTH / 2;
					// Two crossed cards, each seen from both sides.
					DecorDraw.quad(buffer, matrix, new float[][] {{x - w, y, z, 1, 1}, {x - w, y + h, z, 1, 0}, {x + w, y + h, z, 0, 0}, {x + w, y, z, 0, 1}},
							0, 0, -1, 0xFFFFFFFF, FULL_BRIGHT);
					DecorDraw.quad(buffer, matrix, new float[][] {{x, y, z + w, 1, 1}, {x, y + h, z + w, 1, 0}, {x, y + h, z - w, 0, 0}, {x, y, z - w, 0, 1}},
							-1, 0, 0, 0xFFFFFFFF, FULL_BRIGHT);
				}
			});
		}
		pose.popPose();
	}
}
