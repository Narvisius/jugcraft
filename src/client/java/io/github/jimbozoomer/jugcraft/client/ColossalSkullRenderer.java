package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.agriculture.ColossalSkullBlock;
import io.github.jimbozoomer.jugcraft.agriculture.ColossalSkullBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.MourningAngelBlock;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Draws the Colossal Skull's lower jaw (decor18_quads.json) from its first block, turned down about its hinge as far as
 * {@link ColossalSkullBlock#JAW_DEGREES} when it is powered, and snapping when used; and at night a faint blue glow deep
 * in each eye socket (tools/decor18.py SKULL sockets), breathing slowly.
 */
public class ColossalSkullRenderer implements BlockEntityRenderer<ColossalSkullBlockEntity, ColossalSkullRenderer.State> {
	private static final RenderType GLOW = RenderTypes.entityTranslucent(Jugcraft.id("textures/entity/crypt_socket_glow.png"));
	private static final int FULL_BRIGHT = 0xF000F0;
	/**
	 * Each socket: {x0, y0, x1, y1} in pixels, on a sheet at z {@value #SOCKET_Z} facing north: 0.1 pixel in front of the
	 * dark hollow that closes the back of the sockets (its face at z 8), so the hollow never hides it. tools/decor18.py
	 * SKULL holds the same numbers (sockets, glow_z, hollow_z), which the audit compares.
	 */
	private static final float[][] SOCKETS = {{-10.0F, 11.6F, -2.6F, 19.4F}, {2.6F, 11.6F, 10.0F, 19.4F}};
	private static final float SOCKET_Z = 7.9F;
	private static final float HINGE_Y = 8.0F;
	private static final float HINGE_Z = 15.0F;

	public static final class State extends BlockEntityRenderState {
		Direction facing = Direction.NORTH;
		float jaw;
		boolean night;
		float time;
	}

	public ColossalSkullRenderer(BlockEntityRendererProvider.Context context) {
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(ColossalSkullBlockEntity skull, State state, float partialTick, Vec3 camera,
			ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
		BlockEntityRenderState.extractBase(skull, state, crumbling);
		BlockState block = skull.getBlockState();
		if (!(block.getBlock() instanceof ColossalSkullBlock) || skull.getLevel() == null) {
			return;
		}
		state.facing = block.getValue(ColossalSkullBlock.FACING);
		float now = skull.getLevel().getGameTime() + partialTick;
		state.jaw = skull.openness(now, block.getValue(ColossalSkullBlock.POWERED));
		state.night = MourningAngelBlock.night(skull.getLevel());
		state.time = now;
	}

	@Override
	public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
		pose.pushPose();
		pose.translate(0.5F, 0.0F, 0.5F);
		pose.rotateDegrees(Axis.YP, -RockingChairRenderer.yRotation(state.facing));
		pose.translate(-0.5F, 0.0F, -0.5F);
		QuadModel jaw = DecorQuads.get("colossal_skull_jaw");
		if (jaw != null) {
			pose.pushPose();
			pose.translate(0.0F, HINGE_Y / 16, HINGE_Z / 16);
			pose.rotateDegrees(Axis.XP, -ColossalSkullBlock.JAW_DEGREES * state.jaw);
			pose.translate(0.0F, -HINGE_Y / 16, -HINGE_Z / 16);
			jaw.submit(pose, collector, state.lightCoords);
			pose.popPose();
		}
		if (state.night) {
			int alpha = (int) (110 + 60 * Mth.sin(state.time * 0.05F));
			int color = (alpha << 24) | 0x9CD8FF;
			collector.submitCustomGeometry(pose, GLOW, (matrix, buffer) -> {
				for (float[] s : SOCKETS) {
					DecorDraw.quad(buffer, matrix, new float[][] {{s[0] / 16, s[1] / 16, SOCKET_Z / 16, 0, 1}, {s[0] / 16, s[3] / 16, SOCKET_Z / 16, 0, 0},
							{s[2] / 16, s[3] / 16, SOCKET_Z / 16, 1, 0}, {s[2] / 16, s[1] / 16, SOCKET_Z / 16, 1, 1}}, 0, 0, -1, color, FULL_BRIGHT);
				}
			});
		}
		pose.popPose();
	}

	/** The jaw reaches out of the first block's own box, so it is drawn while the block is just off screen. */
	@Override
	public boolean shouldRenderOffScreen() {
		return true;
	}
}
