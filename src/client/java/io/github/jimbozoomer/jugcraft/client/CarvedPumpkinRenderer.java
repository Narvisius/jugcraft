package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.jimbozoomer.jugcraft.agriculture.CarvedPumpkinBlock;
import io.github.jimbozoomer.jugcraft.agriculture.CarvedPumpkinBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.PumpkinCarving;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Draws a hand-carved pumpkin's design over the vanilla pumpkin it is modelled as: one quad per carved
 * side, a hair outside the face, textured by {@link CarvingTextures}. Skin pixels are transparent. Each
 * side takes the light of the block in front of it; with a torch inside, the carving glows at full brightness
 * (candle-yellow, or blue from a soul torch).
 */
public class CarvedPumpkinRenderer implements BlockEntityRenderer<CarvedPumpkinBlockEntity, CarvedPumpkinRenderer.State> {
	/** How far outside the block the carving sits, to stay in front of the pumpkin's own face. */
	private static final float OUT = 0.002F;

	public static final class State extends BlockEntityRenderState {
		PumpkinCarving carving = PumpkinCarving.BLANK;
		Direction facing = Direction.NORTH;
		final int[] light = new int[PumpkinCarving.FACES];
		@Nullable RenderType type;
	}

	public CarvedPumpkinRenderer(BlockEntityRendererProvider.Context context) {
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(CarvedPumpkinBlockEntity pumpkin, State state, float partialTick, Vec3 camera,
			ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
		BlockEntityRenderState.extractBase(pumpkin, state, crumbling);
		state.carving = pumpkin.carving();
		state.type = null;
		BlockState block = pumpkin.getBlockState();
		if (state.carving.isBlank() || !(block.getBlock() instanceof CarvedPumpkinBlock)) {
			return;
		}
		boolean lit = block.getValue(CarvedPumpkinBlock.LIT);
		state.facing = block.getValue(CarvedPumpkinBlock.FACING);
		state.type = CarvingTextures.get(state.carving, lit, block.getValue(CarvedPumpkinBlock.SOUL), CarvingTextures.Glow.of(block.getBlock()));
		Level level = pumpkin.getLevel();
		for (int face = 0; face < PumpkinCarving.FACES; face++) {
			Direction side = PumpkinCarving.side(state.facing, face);
			state.light[face] = lit ? LightCoordsUtil.FULL_BRIGHT
					: level != null ? LightCoordsUtil.getLightCoords(level, pumpkin.getBlockPos().relative(side)) : state.lightCoords;
		}
	}

	@Override
	public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
		RenderType type = state.type;
		if (type == null) {
			return;
		}
		// The geometry is built later in the frame; take the values now.
		PumpkinCarving carving = state.carving;
		Direction facing = state.facing;
		int[] light = state.light.clone();
		collector.submitCustomGeometry(pose, type, (matrix, buffer) -> {
			for (int face = 0; face < PumpkinCarving.FACES; face++) {
				if (!carving.isBlank(face)) {
					side(buffer, matrix, PumpkinCarving.side(facing, face), face, light[face]);
				}
			}
		});
	}

	/**
	 * One face of the texture on one side, seen the right way round from outside: corners top-left,
	 * bottom-left, bottom-right, top-right (counter-clockwise seen from outside).
	 */
	static void side(VertexConsumer buffer, PoseStack.Pose matrix, Direction side, int face, int light) {
		float u0 = face / (float) PumpkinCarving.FACES;
		float u1 = (face + 1) / (float) PumpkinCarving.FACES;
		float[][] corners = switch (side) {
			case NORTH -> new float[][] {{1, 1, -OUT}, {1, 0, -OUT}, {0, 0, -OUT}, {0, 1, -OUT}};
			case SOUTH -> new float[][] {{0, 1, 1 + OUT}, {0, 0, 1 + OUT}, {1, 0, 1 + OUT}, {1, 1, 1 + OUT}};
			case WEST -> new float[][] {{-OUT, 1, 0}, {-OUT, 0, 0}, {-OUT, 0, 1}, {-OUT, 1, 1}};
			default -> new float[][] {{1 + OUT, 1, 1}, {1 + OUT, 0, 1}, {1 + OUT, 0, 0}, {1 + OUT, 1, 0}};
		};
		float[][] uv = {{u0, 0}, {u0, 1}, {u1, 1}, {u1, 0}};
		for (int i = 0; i < 4; i++) {
			buffer.addVertex(matrix, corners[i][0], corners[i][1], corners[i][2]).setColor(0xFFFFFFFF).setUv(uv[i][0], uv[i][1])
					.setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(matrix, side.getStepX(), side.getStepY(), side.getStepZ());
		}
	}
}
