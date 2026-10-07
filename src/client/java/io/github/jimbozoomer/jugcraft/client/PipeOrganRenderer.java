package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.agriculture.PipeOrganBlock;
import io.github.jimbozoomer.jugcraft.agriculture.PipeOrganBlockEntity;
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
 * Draws the Phantom Pipe Organ's keyboard on its master block: {@value #KEYS} ivory keys with their ebony sharps, the
 * keys of the notes it is playing pressed down (from {@link PipeOrganBlockEntity#TUNE} and the time it started). The
 * case and pipes are block models. Coordinates are in pixels of the master block, for an organ facing north (the
 * keyboard reaches a block to either side).
 */
public class PipeOrganRenderer implements BlockEntityRenderer<PipeOrganBlockEntity, PipeOrganRenderer.State> {
	private static final RenderType IVORY = RenderTypes.entitySolid(Jugcraft.id("textures/block/phantom_pipe_organ_ivory.png"));
	private static final RenderType EBONY = RenderTypes.entitySolid(Jugcraft.id("textures/block/phantom_pipe_organ_ebony.png"));
	static final int KEYS = 28;
	/** The keyboard runs from x = LEFT (the player's left, the lowest key) to RIGHT, at the front of the key bed. */
	private static final float LEFT = 26.0F;
	private static final float RIGHT = -10.0F;
	private static final float PITCH = (LEFT - RIGHT) / KEYS;
	private static final float BED = 12.0F;
	private static final float FRONT = 2.5F;
	private static final float BACK = 7.0F;
	/** How long a key stays down after its note sounds, in ticks. */
	private static final int HOLD = 4;
	/** The sharps between white keys, by place in each octave of seven (after C, D, F, G and A). */
	private static final boolean[] SHARP_AFTER = {true, true, false, true, true, true, false};

	public static final class State extends BlockEntityRenderState {
		Direction facing = Direction.NORTH;
		int pressed;
	}

	public PipeOrganRenderer(BlockEntityRendererProvider.Context context) {
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(PipeOrganBlockEntity organ, State state, float partialTick, Vec3 camera,
			ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
		BlockEntityRenderState.extractBase(organ, state, crumbling);
		BlockState block = organ.getBlockState();
		state.pressed = 0;
		if (!(block.getBlock() instanceof PipeOrganBlock) || organ.getLevel() == null) {
			return;
		}
		state.facing = block.getValue(PipeOrganBlock.FACING);
		if (!block.getValue(PipeOrganBlock.PLAYING)) {
			return;
		}
		long elapsed = organ.getLevel().getGameTime() - organ.startTime();
		for (int[] note : PipeOrganBlockEntity.TUNE) {
			if (note[0] <= elapsed && elapsed < note[0] + HOLD) {
				state.pressed |= 1 << PipeOrganBlockEntity.key(note[1], note[2]);
			}
		}
	}

	@Override
	public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
		int light = state.lightCoords;
		int pressed = state.pressed;
		pose.pushPose();
		pose.translate(0.5F, 0.0F, 0.5F);
		pose.rotateDegrees(Axis.YP, -RockingChairRenderer.yRotation(state.facing));
		pose.translate(-0.5F, 0.0F, -0.5F);
		collector.submitCustomGeometry(pose, IVORY, (matrix, buffer) -> {
			for (int k = 0; k < KEYS; k++) {
				float x1 = LEFT - k * PITCH - 0.06F;
				float x0 = x1 - PITCH + 0.12F;
				float top = (pressed & (1 << k)) != 0 ? BED + 0.3F : BED + 0.8F;
				DecorDraw.box(buffer, matrix, x0 / 16, BED / 16, FRONT / 16, x1 / 16, top / 16, BACK / 16, 0, 0, 1, 1, 0xFFFFFFFF, light,
						DecorDraw.ALL & ~DecorDraw.DOWN);
			}
		});
		collector.submitCustomGeometry(pose, EBONY, (matrix, buffer) -> {
			for (int k = 0; k < KEYS - 1; k++) {
				if (!SHARP_AFTER[k % 7]) {
					continue;
				}
				float edge = LEFT - (k + 1) * PITCH;
				DecorDraw.box(buffer, matrix, (edge - 0.4F) / 16, (BED + 0.8F) / 16, (FRONT + 2.0F) / 16, (edge + 0.4F) / 16, (BED + 1.4F) / 16,
						BACK / 16, 0, 0, 1, 1, 0xFFFFFFFF, light, DecorDraw.ALL & ~DecorDraw.DOWN);
			}
		});
		pose.popPose();
	}

	@Override
	public boolean shouldRenderOffScreen() {
		return true; // The keyboard reaches into the blocks either side.
	}
}
