package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.agriculture.DecorationBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.MourningAngelBlock;
import io.github.jimbozoomer.jugcraft.agriculture.SpiritMirrorBlock;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Draws the pale face in a Spirit Mirror at night ({@link SpiritMirrorBlock#face}), faintly glowing and see-through,
 * only for a viewer in front of the mirror within {@link SpiritMirrorBlock#RANGE} blocks.
 */
public class SpiritMirrorRenderer implements BlockEntityRenderer<DecorationBlockEntity, SpiritMirrorRenderer.State> {
	private static final RenderType FACE = RenderTypes.entityTranslucent(Jugcraft.id("textures/entity/spirit_mirror_face.png"));
	private static final int GLOW = 0xF000A0;

	public static final class State extends BlockEntityRenderState {
		Direction facing = Direction.NORTH;
		float alpha;
	}

	public SpiritMirrorRenderer(BlockEntityRendererProvider.Context context) {
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(DecorationBlockEntity mirror, State state, float partialTick, Vec3 camera,
			ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
		BlockEntityRenderState.extractBase(mirror, state, crumbling);
		BlockState block = mirror.getBlockState();
		state.alpha = 0.0F;
		if (!(block.getBlock() instanceof SpiritMirrorBlock) || mirror.getLevel() == null) {
			return;
		}
		state.facing = block.getValue(SpiritMirrorBlock.FACING);
		Vec3 glass = Vec3.atCenterOf(mirror.getBlockPos());
		Vec3 toViewer = camera.subtract(glass);
		boolean inFront = toViewer.x * state.facing.getStepX() + toViewer.z * state.facing.getStepZ() > 0.2;
		if (!inFront || toViewer.length() > SpiritMirrorBlock.RANGE) {
			return;
		}
		float time = mirror.getLevel().getGameTime() % 24000 + partialTick;
		state.alpha = SpiritMirrorBlock.face(mirror.getBlockPos(), MourningAngelBlock.night(mirror.getLevel()), time);
	}

	@Override
	public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
		if (state.alpha <= 0.01F) {
			return;
		}
		int color = ((int) (state.alpha * 200) << 24) | 0xFFFFFF;
		pose.pushPose();
		// Drawn for a mirror facing north (on the wall to the south), then turned to the real facing.
		pose.translate(0.5F, 0.0F, 0.5F);
		pose.rotateDegrees(Axis.YP, -RockingChairRenderer.yRotation(state.facing));
		pose.translate(-0.5F, 0.0F, -0.5F);
		collector.submitCustomGeometry(pose, FACE, (matrix, buffer) -> {
			float z = 14.96F / 16;
			float x0 = 4.0F / 16;
			float x1 = 12.0F / 16;
			float y0 = 3.0F / 16;
			float y1 = 12.0F / 16;
			DecorDraw.quad(buffer, matrix, new float[][] {{x0, y0, z, 1, 1}, {x0, y1, z, 1, 0}, {x1, y1, z, 0, 0}, {x1, y0, z, 0, 1}},
					0, 0, -1, color, GLOW);
		});
		pose.popPose();
	}
}
