package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.agriculture.SpiritBoard;
import io.github.jimbozoomer.jugcraft.agriculture.SpiritBoardBlock;
import io.github.jimbozoomer.jugcraft.agriculture.SpiritBoardBlockEntity;
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
 * The Spirit Board's lettered face and its planchette, drawn for a board read from the north and turned to its facing. The
 * face is a 64 by 48 picture laid over the board's top (more letters than a block texture holds). The planchette, a walnut
 * heart with a glass lens, slides from stop to stop as the server tells it, easing in and out, and swivels as it goes
 * towards the board's ends; resting on a letter mid-séance it circles slowly, as a planchette under fingers does.
 */
public class SpiritBoardRenderer implements BlockEntityRenderer<SpiritBoardBlockEntity, SpiritBoardRenderer.State> {
	private static final RenderType FACE = RenderTypes.entityCutout(Jugcraft.id("textures/entity/spirit_board.png"));
	private static final RenderType PLANCHETTE = RenderTypes.entityCutout(Jugcraft.id("textures/entity/planchette.png"));
	private static final RenderType WOOD = RenderTypes.entityCutout(Jugcraft.id("textures/entity/planchette_wood.png"));
	/** The board's top, in pixels of the block, for a board facing north (read from the north side). */
	static final float X0 = 0.5F;
	static final float X1 = 15.5F;
	static final float Z0 = 2.5F;
	static final float Z1 = 13.5F;
	static final float TOP = 1.0F;
	/** Half the planchette's width and length, and how high it stands on its felt feet. */
	static final float HALF = 2.4F;
	static final float FEET = 0.35F;
	static final float THICK = 0.45F;

	public static final class State extends BlockEntityRenderState {
		Direction facing = Direction.NORTH;
		char from = SpiritBoard.REST;
		char to = SpiritBoard.REST;
		float moved;
		boolean active;
		float time;
	}

	public SpiritBoardRenderer(BlockEntityRendererProvider.Context context) {
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(SpiritBoardBlockEntity board, State state, float partialTick, Vec3 camera,
			ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
		BlockEntityRenderState.extractBase(board, state, crumbling);
		state.facing = board.getBlockState().hasProperty(SpiritBoardBlock.FACING) ? board.getBlockState().getValue(SpiritBoardBlock.FACING)
				: Direction.NORTH;
		long time = board.getLevel() == null ? 0L : board.getLevel().getGameTime();
		state.from = board.from();
		state.to = board.to();
		state.time = (time % 24000L) + partialTick;
		state.moved = Mth.clamp((time - board.moveStart() + partialTick) / board.moveTicks(), 0.0F, 1.0F);
		state.active = board.active();
	}

	/** Where {@code stop} is on the face, in its pixels (the middle of the board if it isn't one). */
	private static float[] place(char stop) {
		float[] place = SpiritBoard.place(stop);
		return place == null ? SpiritBoard.place(SpiritBoard.REST) : place;
	}

	@Override
	public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
		int light = state.lightCoords;
		pose.pushPose();
		pose.translate(0.5F, 0.0F, 0.5F);
		pose.rotateDegrees(Axis.YP, -RockingChairRenderer.yRotation(state.facing));
		pose.translate(-0.5F, 0.0F, -0.5F);
		// The face, read from the north: the picture's left is the board's east end, its top the far (south) side.
		float y = TOP + 0.05F;
		collector.submitCustomGeometry(pose, FACE, (matrix, buffer) -> TintedBoxes.flat(buffer, matrix,
				new float[][] {{X1, y, Z1}, {X0, y, Z1}, {X0, y, Z0}, {X1, y, Z0}}, new float[][] {{0, 0}, {1, 0}, {1, 1}, {0, 1}}, -1, light));
		// The planchette, eased from one stop to the next; circling gently on a letter while fingers are on it.
		float[] from = place(state.from);
		float[] to = place(state.to);
		float t = state.moved * state.moved * (3.0F - 2.0F * state.moved);
		float u = Mth.lerp(t, from[0], to[0]);
		float v = Mth.lerp(t, from[1], to[1]);
		if (state.active && state.moved >= 1.0F) {
			u += 0.8F * Mth.cos(state.time * 0.15F);
			v += 0.6F * Mth.sin(state.time * 0.15F);
		}
		float px = X1 - u * (X1 - X0) / SpiritBoard.FACE_WIDTH;
		float pz = Z1 - v * (Z1 - Z0) / SpiritBoard.FACE_HEIGHT;
		float swivel = (u - SpiritBoard.FACE_WIDTH / 2.0F) / (SpiritBoard.FACE_WIDTH / 2.0F) * 20.0F;
		pose.pushPose();
		pose.translate(px / 16.0F, y / 16.0F, pz / 16.0F);
		pose.rotateDegrees(Axis.YP, swivel);
		collector.submitCustomGeometry(pose, WOOD, (matrix, buffer) -> {
			// Three felt feet and the body's thickness, a little inside the heart's outline.
			TintedBoxes.box(buffer, matrix, -1.5F, 0.0F, -1.2F, -0.9F, FEET, -0.6F, 0xFF2A2A2A, light);
			TintedBoxes.box(buffer, matrix, 0.9F, 0.0F, -1.2F, 1.5F, FEET, -0.6F, 0xFF2A2A2A, light);
			TintedBoxes.box(buffer, matrix, -0.3F, 0.0F, 1.4F, 0.3F, FEET, 2.0F, 0xFF2A2A2A, light);
			TintedBoxes.box(buffer, matrix, -1.7F, FEET, -1.7F, 1.7F, FEET + THICK - 0.02F, 1.2F, -1, light);
			TintedBoxes.box(buffer, matrix, -0.8F, FEET, 1.2F, 0.8F, FEET + THICK - 0.02F, 2.0F, -1, light);
		});
		float top = FEET + THICK;
		collector.submitCustomGeometry(pose, PLANCHETTE, (matrix, buffer) -> TintedBoxes.flat(buffer, matrix,
				new float[][] {{HALF, top, HALF}, {-HALF, top, HALF}, {-HALF, top, -HALF}, {HALF, top, -HALF}},
				new float[][] {{0, 0}, {1, 0}, {1, 1}, {0, 1}}, -1, light));
		pose.popPose();
		pose.popPose();
	}
}
