package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.agriculture.Candelabra;
import io.github.jimbozoomer.jugcraft.agriculture.CandelabrumBlock;
import io.github.jimbozoomer.jugcraft.agriculture.DecorationBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.FloorCandelabrumBlock;
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
 * Draws the candles of a wrought-iron candelabrum (the floor candelabrum's lower half, the table candelabrum, the wall
 * girandole and the branching chandelier) where {@link Candelabra#candles} puts them: each a taper of its wax's colour
 * with a wick, wax running down it and pooling in its pan as the drips grow, and, while lit, a flame of its flame's colour
 * at full brightness, each flickering on its own.
 */
public class CandelabrumRenderer implements BlockEntityRenderer<DecorationBlockEntity, CandelabrumRenderer.State> {
	private static final RenderType WAX = RenderTypes.entityCutout(Jugcraft.id("textures/entity/witchs_workshop_wax.png"));
	private static final RenderType FLAME = RenderTypes.entityCutout(Jugcraft.id("textures/entity/witchs_workshop_flame.png"));
	private static final int FULL_BRIGHT = 0xF000F0;
	private static final int WICK = 0xFF1C1612;
	private static final float FLAME_WIDTH = 1.5F;
	private static final float FLAME_HEIGHT = 2.8F;

	public static final class State extends BlockEntityRenderState {
		String kind = "";
		Direction facing = Direction.NORTH;
		int wax;
		int flame;
		boolean lit;
		int drips;
		float time;
		int seed;
	}

	public CandelabrumRenderer(BlockEntityRendererProvider.Context context) {
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(DecorationBlockEntity fitting, State state, float partialTick, Vec3 camera,
			ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
		BlockEntityRenderState.extractBase(fitting, state, crumbling);
		BlockState block = fitting.getBlockState();
		state.kind = "";
		if (!block.hasProperty(Candelabra.WAX)) {
			return;
		}
		if (block.getBlock() instanceof FloorCandelabrumBlock) {
			state.kind = FloorCandelabrumBlock.KIND;
			state.facing = block.getValue(FloorCandelabrumBlock.FACING);
		} else if (block.getBlock() instanceof CandelabrumBlock candelabrum) {
			state.kind = candelabrum.kind();
			state.facing = CandelabrumBlock.facing(block);
		}
		state.wax = block.getValue(Candelabra.WAX).rgb;
		state.flame = block.getValue(Candelabra.FLAME).rgb;
		state.lit = block.getValue(Candelabra.LIT);
		state.drips = block.getValue(Candelabra.DRIPS);
		state.time = fitting.getLevel() == null ? 0 : (fitting.getLevel().getGameTime() % 24000) + partialTick;
		state.seed = fitting.getBlockPos().hashCode();
	}

	@Override
	public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
		float[][] candles = Candelabra.candles(state.kind);
		if (candles.length == 0) {
			return;
		}
		pose.pushPose();
		pose.translate(0.5F, 0.0F, 0.5F);
		pose.rotateDegrees(Axis.YP, -RockingChairRenderer.yRotation(state.facing));
		pose.translate(-0.5F, 0.0F, -0.5F);
		int wax = 0xFF000000 | state.wax;
		int drip = 0xFF000000 | lighter(state.wax);
		int light = state.lightCoords;
		int drips = state.drips;
		float w = Candelabra.CANDLE_WIDTH / 2;
		collector.submitCustomGeometry(pose, WAX, (matrix, buffer) -> {
			for (int i = 0; i < candles.length; i++) {
				float[] c = candles[i];
				float x = c[0] / 16;
				float y = c[1] / 16;
				float z = c[2] / 16;
				float h = c[3] / 16;
				float r = w / 16;
				DecorDraw.box(buffer, matrix, x - r, y, z - r, x + r, y + h, z + r, 0.0F, 0.0F, 0.7F, 1.0F, wax, light,
						DecorDraw.NORTH | DecorDraw.SOUTH | DecorDraw.EAST | DecorDraw.WEST);
				DecorDraw.box(buffer, matrix, x - r, y, z - r, x + r, y + h, z + r, 0.75F, 0.0F, 1.0F, 0.25F, wax, light, DecorDraw.UP);
				float wick = 0.25F / 16;
				DecorDraw.box(buffer, matrix, x - wick, y + h, z - wick, x + wick, y + h + 0.8F / 16, z + wick, 0.75F, 0.0F, 0.8F, 0.05F, WICK, light,
						DecorDraw.ALL);
				// Runs of wax down the sides, one more side each stage, and from the second stage a puddle round its foot.
				for (int d = 0; d < drips; d++) {
					float length = h * (0.35F + 0.15F * ((i + d) % 3));
					float t = 0.25F / 16;
					switch (d) {
						case 0 -> DecorDraw.box(buffer, matrix, x - 0.2F / 16, y + h - length, z - r - t, x + 0.3F / 16, y + h, z - r, 0.75F, 0.0F, 1.0F,
								0.25F, drip, light, DecorDraw.ALL);
						case 1 -> DecorDraw.box(buffer, matrix, x + r, y + h - length, z - 0.3F / 16, x + r + t, y + h, z + 0.2F / 16, 0.75F, 0.0F, 1.0F,
								0.25F, drip, light, DecorDraw.ALL);
						default -> DecorDraw.box(buffer, matrix, x - r - t, y + h - length, z - 0.1F / 16, x - r, y + h, z + 0.4F / 16, 0.75F, 0.0F, 1.0F,
								0.25F, drip, light, DecorDraw.ALL);
					}
				}
				if (drips >= 2) {
					float pool = r + 0.5F / 16;
					DecorDraw.box(buffer, matrix, x - pool, y, z - pool, x + pool, y + 0.3F / 16, z + pool, 0.75F, 0.0F, 1.0F, 0.25F, drip, light,
							DecorDraw.ALL);
				}
			}
		});
		if (state.lit) {
			int flame = 0xFF000000 | state.flame;
			float time = state.time;
			int seed = state.seed;
			collector.submitCustomGeometry(pose, FLAME, (matrix, buffer) -> {
				for (int i = 0; i < candles.length; i++) {
					float[] c = candles[i];
					float x = c[0] / 16;
					float z = c[2] / 16;
					float y = (c[1] + c[3] + 0.3F) / 16;
					float flicker = 0.85F + 0.15F * Mth.sin(time * 0.9F + i * 2.3F + seed) + 0.05F * Mth.sin(time * 2.7F + i);
					float h = FLAME_HEIGHT * flicker / 16;
					float hw = FLAME_WIDTH / 32;
					float lean = 0.15F / 16 * Mth.sin(time * 0.3F + i);
					DecorDraw.quad(buffer, matrix, new float[][] {{x - hw, y, z, 1, 1}, {x - hw + lean, y + h, z, 1, 0}, {x + hw + lean, y + h, z, 0, 0},
							{x + hw, y, z, 0, 1}}, 0, 0, -1, flame, FULL_BRIGHT);
					DecorDraw.quad(buffer, matrix, new float[][] {{x, y, z + hw, 1, 1}, {x, y + h, z + hw + lean, 1, 0}, {x, y + h, z - hw + lean, 0, 0},
							{x, y, z - hw, 0, 1}}, -1, 0, 0, flame, FULL_BRIGHT);
				}
			});
		}
		pose.popPose();
	}

	/** A wax colour a little paler, for its fresh drips. */
	private static int lighter(int rgb) {
		int r = Math.min(255, ((rgb >> 16) & 0xFF) + 24);
		int g = Math.min(255, ((rgb >> 8) & 0xFF) + 24);
		int b = Math.min(255, (rgb & 0xFF) + 24);
		return (r << 16) | (g << 8) | b;
	}

	/** The chandelier reaches a block out on every side; draw it when it is only just in view. */
	@Override
	public boolean shouldRenderOffScreen() {
		return true;
	}
}
