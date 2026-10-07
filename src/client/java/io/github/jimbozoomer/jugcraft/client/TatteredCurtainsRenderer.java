package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.agriculture.DecorationBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.MourningAngelBlock;
import io.github.jimbozoomer.jugcraft.agriculture.TatteredCurtainsBlock;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Draws Tattered Curtains: closed, a sheet of ragged cheesecloth swaying in a draft ({@link TatteredCurtainsBlock#sway},
 * more the further below the rod); open, drawn back in bunches at both sides; the iron rod across the top block of a
 * drape, and the ragged hem on its bottom block. Coordinates are in pixels, for curtains facing north (hung against
 * the south side of their block).
 */
public class TatteredCurtainsRenderer implements BlockEntityRenderer<DecorationBlockEntity, TatteredCurtainsRenderer.State> {
	private static final RenderType CLOTH = RenderTypes.entityCutout(Jugcraft.id("textures/entity/tattered_curtains.png"));
	private static final RenderType HEM = RenderTypes.entityCutout(Jugcraft.id("textures/entity/tattered_curtains_hem.png"));
	private static final RenderType ROD = RenderTypes.entitySolid(Jugcraft.id("textures/block/tattered_curtains_rod.png"));
	private static final int COLUMNS = 8;
	private static final int ROWS = 4;
	/** Where the cloth hangs (z) and the rod runs, for curtains facing north. */
	private static final float CLOTH_Z = 13.5F;

	public static final class State extends BlockEntityRenderState {
		Direction facing = Direction.NORTH;
		boolean open;
		boolean rod;
		boolean hem;
		/** For each vertex of the cloth (row by row from the top), how far it stands toward the room, in pixels. */
		final float[] sway = new float[(COLUMNS + 1) * (ROWS + 1)];
	}

	public TatteredCurtainsRenderer(BlockEntityRendererProvider.Context context) {
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(DecorationBlockEntity curtains, State state, float partialTick, Vec3 camera,
			ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
		BlockEntityRenderState.extractBase(curtains, state, crumbling);
		BlockState block = curtains.getBlockState();
		Level level = curtains.getLevel();
		if (!(block.getBlock() instanceof TatteredCurtainsBlock) || level == null) {
			return;
		}
		state.facing = block.getValue(TatteredCurtainsBlock.FACING);
		state.open = block.getValue(TatteredCurtainsBlock.OPEN);
		TatteredCurtainsBlock.Part part = block.getValue(TatteredCurtainsBlock.PART);
		state.rod = part.top();
		state.hem = part.bottom();
		// How many blocks of the same drape hang above this one.
		BlockPos pos = curtains.getBlockPos();
		int above = 0;
		for (BlockPos at = pos.above(); above < TatteredCurtainsBlock.MAX_DROP; at = at.above()) {
			BlockState over = level.getBlockState(at);
			if (!over.is(block.getBlock()) || over.getValue(TatteredCurtainsBlock.FACING) != state.facing) {
				break;
			}
			above++;
		}
		boolean night = MourningAngelBlock.night(level);
		float time = level.getGameTime() % 24000 + partialTick;
		for (int row = 0; row <= ROWS; row++) {
			float down = above + (float) row / ROWS;
			for (int col = 0; col <= COLUMNS; col++) {
				state.sway[row * (COLUMNS + 1) + col] = state.open ? 0.0F
						: TatteredCurtainsBlock.sway(pos, night, time, down, (float) col / COLUMNS);
			}
		}
	}

	@Override
	public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
		int light = state.lightCoords;
		pose.pushPose();
		pose.translate(0.5F, 0.0F, 0.5F);
		pose.rotateDegrees(Axis.YP, -RockingChairRenderer.yRotation(state.facing));
		pose.translate(-0.5F, 0.0F, -0.5F);
		if (state.open) {
			// Drawn back: a bunch of cloth at each side, a little proud of the wall.
			collector.submitCustomGeometry(pose, state.hem ? HEM : CLOTH, (matrix, buffer) -> {
				DecorDraw.box(buffer, matrix, 0.0F, 0.0F, 12.5F / 16, 3.0F / 16, 1.0F, 14.5F / 16, 0, 0, 0.25F, 1, 0xFFFFFFFF, light, DecorDraw.ALL);
				DecorDraw.box(buffer, matrix, 13.0F / 16, 0.0F, 12.5F / 16, 1.0F, 1.0F, 14.5F / 16, 0.75F, 0, 1, 1, 0xFFFFFFFF, light, DecorDraw.ALL);
			});
		} else {
			float[] sway = state.sway.clone();
			collector.submitCustomGeometry(pose, state.hem ? HEM : CLOTH, (matrix, buffer) -> {
				for (int row = 0; row < ROWS; row++) {
					for (int col = 0; col < COLUMNS; col++) {
						float x0 = (float) col / COLUMNS;
						float x1 = (float) (col + 1) / COLUMNS;
						float yTop = 1.0F - (float) row / ROWS;
						float yBottom = 1.0F - (float) (row + 1) / ROWS;
						float zTL = (CLOTH_Z - sway[row * (COLUMNS + 1) + col]) / 16;
						float zTR = (CLOTH_Z - sway[row * (COLUMNS + 1) + col + 1]) / 16;
						float zBL = (CLOTH_Z - sway[(row + 1) * (COLUMNS + 1) + col]) / 16;
						float zBR = (CLOTH_Z - sway[(row + 1) * (COLUMNS + 1) + col + 1]) / 16;
						// Seen from the room (the north), x runs right to left; the texture's u runs left to right.
						DecorDraw.quad(buffer, matrix, new float[][] {
								{x0, yBottom, zBL, 1 - x0, 1 - yBottom}, {x0, yTop, zTL, 1 - x0, 1 - yTop},
								{x1, yTop, zTR, 1 - x1, 1 - yTop}, {x1, yBottom, zBR, 1 - x1, 1 - yBottom}}, 0, 0, -1, 0xFFFFFFFF, light);
					}
				}
			});
		}
		if (state.rod) {
			collector.submitCustomGeometry(pose, ROD, (matrix, buffer) -> {
				DecorDraw.box(buffer, matrix, 0.0F, 14.75F / 16, (CLOTH_Z - 0.75F) / 16, 1.0F, 15.75F / 16, (CLOTH_Z + 0.25F) / 16, 0, 0, 1, 0.0625F,
						0xFFFFFFFF, light, DecorDraw.ALL);
			});
		}
		pose.popPose();
	}
}
