package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.agriculture.DecorationBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.WitchFireBrazierBlock;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Draws the Witch Fire Brazier's flames: two crossed sheets of flame over the coals, a smaller, paler pair inside them,
 * all tinted the flame's colour and flickering, at full brightness; nothing while it is out.
 */
public class WitchFireBrazierRenderer implements BlockEntityRenderer<DecorationBlockEntity, WitchFireBrazierRenderer.State> {
	private static final RenderType FLAME = RenderTypes.entityTranslucent(Jugcraft.id("textures/entity/witch_fire_flame.png"));
	private static final int FULL_BRIGHT = 0xF000F0;
	/** Where the flames stand on the coals, how wide and how tall they are, in pixels. */
	private static final float BASE_Y = 9.5F;
	private static final float WIDTH = 11.0F;
	private static final float HEIGHT = 14.0F;

	public static final class State extends BlockEntityRenderState {
		boolean lit;
		int color = 0xFFFFFFFF;
		float flicker;
		float sway;
	}

	public WitchFireBrazierRenderer(BlockEntityRendererProvider.Context context) {
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(DecorationBlockEntity brazier, State state, float partialTick, Vec3 camera,
			ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
		BlockEntityRenderState.extractBase(brazier, state, crumbling);
		BlockState block = brazier.getBlockState();
		Level level = brazier.getLevel();
		state.lit = false;
		if (!(block.getBlock() instanceof WitchFireBrazierBlock) || level == null) {
			return;
		}
		state.lit = block.getValue(WitchFireBrazierBlock.LIT);
		state.color = block.getValue(WitchFireBrazierBlock.FLAME).color;
		float time = (float) ((level.getGameTime() + (double) partialTick) % 24000.0) + (brazier.getBlockPos().hashCode() & 0xFF);
		state.flicker = 1.0F + 0.12F * Mth.sin(time * 0.9F) + 0.07F * Mth.sin(time * 2.3F + 1.0F);
		state.sway = 0.6F * Mth.sin(time * 0.31F);
	}

	@Override
	public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
		if (!state.lit) {
			return;
		}
		int color = state.color;
		int pale = 0xE0000000 | (lighter(color >> 16) << 16) | (lighter(color >> 8) << 8) | lighter(color);
		collector.submitCustomGeometry(pose, FLAME, (matrix, buffer) -> {
			sheets(buffer, matrix, WIDTH, HEIGHT * state.flicker, state.sway, color, DecorDraw.TWO_SIDED_LIFT);
			// The pale core shares the big sheets' planes, so it stands further out on each side to show in front of them.
			sheets(buffer, matrix, WIDTH * 0.55F, HEIGHT * 0.65F * (2.0F - state.flicker), -state.sway, pale, 3 * DecorDraw.TWO_SIDED_LIFT);
		});
	}

	private static int lighter(int channel) {
		return Math.min(255, (channel & 0xFF) + 80);
	}

	/**
	 * Two crossed sheets of flame, each drawn from both sides (each side {@code lift} pixels off the middle, never one plane
	 * twice), their tips leaning by {@code sway} pixels.
	 */
	private static void sheets(VertexConsumer buffer, PoseStack.Pose matrix, float width, float height, float sway, int color, float lift) {
		float y0 = BASE_Y / 16;
		float y1 = (BASE_Y + height) / 16;
		float h = width / 2 / 16 * 0.7071F;
		float s = sway / 16;
		for (int diagonal = 0; diagonal < 2; diagonal++) {
			float dx = h;
			float dz = diagonal == 0 ? h : -h;
			float[][] front = {{0.5F - dx, y0, 0.5F - dz, 0, 1}, {0.5F - dx + s, y1, 0.5F - dz, 0, 0}, {0.5F + dx + s, y1, 0.5F + dz, 1, 0},
					{0.5F + dx, y0, 0.5F + dz, 1, 1}};
			float nx = diagonal == 0 ? -0.7071F : 0.7071F;
			DecorDraw.twoSided(buffer, matrix, front, nx, 0, 0.7071F, color, FULL_BRIGHT, lift);
		}
	}
}
