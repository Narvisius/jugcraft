package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.agriculture.DecorationBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.SingingPumpkinBlock;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Brings a Singing Pumpkin's carved face to life over its block model (whose face has glowing eye holes and a closed
 * smile): dark pupils that roll idly round its eyes, and when it sings (the block event marks the time) the mouth opens
 * into a round glowing O for {@value SingingPumpkinBlock#OPEN_TICKS} ticks, the eyes widen and the pupils roll up as if
 * it were giving its all. Each voice's pumpkin is its own shape, so its face sits in its own place ({@link #FACES}, from
 * tools/decor20.py VOICES). Drawn at full brightness, as the carving glows.
 */
public class SingingPumpkinRenderer implements BlockEntityRenderer<DecorationBlockEntity, SingingPumpkinRenderer.State> {
	private static final RenderType FACE = RenderTypes.entityTranslucent(Jugcraft.id("textures/entity/singing_pumpkin_face.png"));
	/**
	 * Each voice's face, in pixels for a pumpkin facing north: its front's distance from the north side, the eyes' height
	 * and distance either side of the middle, an eye's size, the mouth's height and width, and its height wide open.
	 */
	static final float[][] FACES = {
			{0.5F, 7.5F, 3.5F, 3.5F, 3.5F, 7.0F, 4.5F},
			{1.5F, 8.5F, 3.0F, 3.0F, 4.5F, 5.5F, 4.0F},
			{2.0F, 9.5F, 2.8F, 2.8F, 5.0F, 5.0F, 4.0F},
			{3.0F, 7.0F, 2.3F, 2.4F, 3.6F, 4.0F, 3.4F}};
	/** Sitting proud of the front by this much (pixels), the pupils a little more. */
	private static final float OUT = 0.06F;
	/** The ticks the mouth takes to open, and to close at the end. */
	private static final float OPENING = 2.0F;
	private static final float CLOSING = 6.0F;

	public static final class State extends BlockEntityRenderState {
		Direction facing = Direction.NORTH;
		int voice;
		float open;
		float time;
	}

	public SingingPumpkinRenderer(BlockEntityRendererProvider.Context context) {
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	/** How wide the mouth is open, 0 to 1, {@code since} ticks after it began to sing. */
	static float open(double since) {
		if (since < 0 || since >= SingingPumpkinBlock.OPEN_TICKS) {
			return 0.0F;
		}
		float opening = Math.min(1.0F, (float) since / OPENING);
		float closing = Math.min(1.0F, (float) (SingingPumpkinBlock.OPEN_TICKS - since) / CLOSING);
		return Math.min(opening, closing);
	}

	@Override
	public void extractRenderState(DecorationBlockEntity pumpkin, State state, float partialTick, Vec3 camera,
			ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
		BlockEntityRenderState.extractBase(pumpkin, state, crumbling);
		BlockState block = pumpkin.getBlockState();
		Level level = pumpkin.getLevel();
		if (!(block.getBlock() instanceof SingingPumpkinBlock singer) || level == null) {
			return;
		}
		state.facing = block.getValue(SingingPumpkinBlock.FACING);
		state.voice = singer.voice().ordinal();
		double now = level.getGameTime() + (double) partialTick;
		state.open = pumpkin.marked() == Long.MIN_VALUE ? 0.0F : open(now - pumpkin.marked());
		state.time = (float) (now % 24000.0) + (pumpkin.getBlockPos().hashCode() & 0xFF);
	}

	@Override
	public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
		float[] face = FACES[state.voice];
		float open = state.open;
		float time = state.time;
		pose.pushPose();
		pose.translate(0.5F, 0.0F, 0.5F);
		pose.rotateDegrees(Axis.YP, -RockingChairRenderer.yRotation(state.facing));
		pose.translate(-0.5F, 0.0F, -0.5F);
		collector.submitCustomGeometry(pose, FACE, (matrix, buffer) -> {
			float z = (face[0] - OUT) / 16.0F;
			float eye = face[3] * (1.0F + 0.3F * open);
			// Idle, the pupils wander; singing, they roll up.
			float lookX = (1.0F - open) * 0.3F * Mth.sin(time * 0.045F);
			float lookY = (1.0F - open) * 0.2F * Mth.cos(time * 0.031F) + open * 0.3F;
			for (int side = -1; side <= 1; side += 2) {
				float x = 8.0F + side * face[2];
				if (open > 0.0F) {
					rect(buffer, matrix, x, face[1], z, eye, eye, 0.0F, 0.0F, 0.5F, 0.5F);
				}
				float pupil = face[3] * 0.42F;
				rect(buffer, matrix, x + lookX * face[3], face[1] + lookY * face[3], z - OUT / 16.0F, pupil, pupil, 0.0F, 0.5F, 0.25F, 0.75F);
			}
			if (open > 0.0F) {
				float height = Mth.lerp(open, 1.0F, face[6]);
				rect(buffer, matrix, 8.0F, face[4], z, face[5] * (1.0F - 0.25F * open), height, 0.5F, 0.0F, 1.0F, 0.5F);
			}
		});
		pose.popPose();
	}

	/**
	 * A rectangle facing north (out of the pumpkin's face) centred at ({@code x}, {@code y}) pixels and {@code z} blocks,
	 * {@code width} by {@code height} pixels, showing the texture's (u0, v0)-(u1, v1).
	 */
	private static void rect(VertexConsumer buffer, PoseStack.Pose matrix, float x, float y, float z, float width, float height, float u0, float v0,
			float u1, float v1) {
		float x0 = (x - width / 2) / 16.0F;
		float x1 = (x + width / 2) / 16.0F;
		float y0 = (y - height / 2) / 16.0F;
		float y1 = (y + height / 2) / 16.0F;
		// Wound as DecorDraw's north faces: seen from the north +x is to the viewer's left, so the texture's left edge is at the high x.
		float[][] corners = {{x0, y0, z, u1, v1}, {x0, y1, z, u1, v0}, {x1, y1, z, u0, v0}, {x1, y0, z, u0, v1}};
		DecorDraw.quad(buffer, matrix, corners, 0, 0, -1, 0xFFFFFFFF, LightCoordsUtil.FULL_BRIGHT);
	}
}
