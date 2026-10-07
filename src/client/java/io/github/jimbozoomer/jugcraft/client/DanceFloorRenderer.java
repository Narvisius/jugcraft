package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.agriculture.DanceFloorBlock;
import io.github.jimbozoomer.jugcraft.agriculture.DecorationBlockEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Lights a Monster Mash Dance Floor tile that the music reaches: its lamps glow at full brightness in four quarters of
 * Halloween colours that step along every {@value #BEAT} ticks in diagonal waves across the floor, flaring on the beat
 * and fading till the next; tiles further from the music glow a little less.
 */
public class DanceFloorRenderer implements BlockEntityRenderer<DecorationBlockEntity, DanceFloorRenderer.State> {
	private static final RenderType GLOW = RenderTypes.entityTranslucent(Jugcraft.id("textures/entity/dance_floor_glow.png"));
	private static final int FULL_BRIGHT = 0xF000F0;
	/** Orange, purple, slime green and magenta. */
	private static final int[] COLOURS = {0xFF8C1A, 0x9B30FF, 0x7CFF3A, 0xFF3AA8};
	private static final int BEAT = 10;
	private static final float ABOVE = 0.003F;

	public static final class State extends BlockEntityRenderState {
		boolean lit;
		int step;
		float flare;
		float dim;
		int wave;
	}

	public DanceFloorRenderer(BlockEntityRendererProvider.Context context) {
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(DecorationBlockEntity tile, State state, float partialTick, Vec3 camera,
			ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
		BlockEntityRenderState.extractBase(tile, state, crumbling);
		BlockState block = tile.getBlockState();
		Level level = tile.getLevel();
		state.lit = false;
		if (!(block.getBlock() instanceof DanceFloorBlock) || level == null || !DanceFloorBlock.lit(block)) {
			return;
		}
		state.lit = true;
		double time = level.getGameTime() + (double) partialTick;
		state.step = (int) Math.floor(time / BEAT);
		state.flare = 1.0F - (float) ((time % BEAT) / BEAT) * 0.55F;
		state.dim = 1.0F - block.getValue(DanceFloorBlock.DISTANCE) * 0.07F;
		BlockPos pos = tile.getBlockPos();
		state.wave = pos.getX() + pos.getZ();
	}

	@Override
	public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
		if (!state.lit) {
			return;
		}
		int alpha = Math.round(255 * state.flare * state.dim);
		float y = 1.0F + ABOVE;
		collector.submitCustomGeometry(pose, GLOW, (matrix, buffer) -> {
			for (int i = 0; i < 2; i++) {
				for (int j = 0; j < 2; j++) {
					int colour = COLOURS[Math.floorMod(state.step - state.wave * 2 - i - j, COLOURS.length)];
					int argb = (alpha << 24) | colour;
					float x0 = i * 0.5F;
					float z0 = j * 0.5F;
					float u0 = i * 0.5F;
					float v0 = j * 0.5F;
					float[][] corners = {{x0, y, z0, u0, v0}, {x0, y, z0 + 0.5F, u0, v0 + 0.5F}, {x0 + 0.5F, y, z0 + 0.5F, u0 + 0.5F, v0 + 0.5F},
							{x0 + 0.5F, y, z0, u0 + 0.5F, v0}};
					DecorDraw.quad(buffer, matrix, corners, 0, 1, 0, argb, FULL_BRIGHT);
				}
			}
		});
	}
}
