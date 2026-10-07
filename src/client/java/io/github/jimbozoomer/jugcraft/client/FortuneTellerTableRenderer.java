package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.agriculture.FortuneTellerTableBlock;
import io.github.jimbozoomer.jugcraft.agriculture.FortuneTellerTableBlockEntity;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Draws the Fortune Teller's Table's middle tarot card and its planchette. When a reading comes (the table's block
 * event), the card flips face up to show its picture and the planchette slides across the spirit board to YES, NO or
 * GOODBYE, circles there a while, and both go back; between readings the card lies face down and the planchette rests
 * in the middle of the board. Positions are in pixels on a table facing north; the visitor stands to the north, so the
 * board's left (its YES) is east.
 */
public class FortuneTellerTableRenderer implements BlockEntityRenderer<FortuneTellerTableBlockEntity, FortuneTellerTableRenderer.State> {
	private static final RenderType BACK = RenderTypes.entitySolid(Jugcraft.id("textures/block/fortune_card_back.png"));
	private static final RenderType[] FACES = new RenderType[FortuneTellerTableBlock.CARDS];
	/** The turning card (x0, z0, x1, z1), and the table top it lies on. */
	private static final float[] CARD = {6.75F, 1.75F, 9.25F, 5.25F};
	private static final float TOP = 12.0F;
	private static final float THICK = 0.25F;
	/** Where the planchette rests, and goes to for YES, NO and GOODBYE (x, z); its quads are drawn about (8, 8). */
	private static final float[] REST = {8.0F, 10.0F};
	private static final float[][] ANSWERS = {{11.2F, 12.0F}, {4.25F, 12.0F}, {8.0F, 8.0F}};
	private static final float FLIP_TICKS = 8.0F;

	static {
		for (int i = 0; i < FACES.length; i++) {
			FACES[i] = RenderTypes.entitySolid(Jugcraft.id("textures/block/fortune_card_" + i + ".png"));
		}
	}

	public static final class State extends BlockEntityRenderState {
		Direction facing = Direction.NORTH;
		int card;
		float flip;
		float x = REST[0];
		float z = REST[1];
	}

	public FortuneTellerTableRenderer(BlockEntityRendererProvider.Context context) {
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(FortuneTellerTableBlockEntity table, State state, float partialTick, Vec3 camera,
			ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
		BlockEntityRenderState.extractBase(table, state, crumbling);
		BlockState block = table.getBlockState();
		Level level = table.getLevel();
		state.flip = 0.0F;
		state.x = REST[0];
		state.z = REST[1];
		if (!(block.getBlock() instanceof FortuneTellerTableBlock) || level == null) {
			return;
		}
		state.facing = block.getValue(FortuneTellerTableBlock.FACING);
		float reading = FortuneTellerTableBlock.READING_TICKS;
		float t = (float) (level.getGameTime() - table.marked()) + partialTick;
		if (t < 0.0F || t > reading) {
			return;
		}
		state.card = Math.floorMod(table.card(), FACES.length);
		state.flip = 180.0F * ease(t / FLIP_TICKS) * ease((reading - t) / FLIP_TICKS);
		float[] target = ANSWERS[Math.floorMod(table.answer(), ANSWERS.length)];
		float there = ease((t - 10.0F) / 25.0F) * ease((reading - 5.0F - t) / 15.0F);
		state.x = REST[0] + (target[0] - REST[0]) * there + 0.4F * there * Mth.sin(t * 0.5F);
		state.z = REST[1] + (target[1] - REST[1]) * there + 0.4F * there * Mth.cos(t * 0.5F);
	}

	/** 0 to 1, easing in and out, for 0 to 1 (clamped). */
	private static float ease(float t) {
		float c = Mth.clamp(t, 0.0F, 1.0F);
		return c * c * (3.0F - 2.0F * c);
	}

	@Override
	public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
		QuadModel planchette = DecorQuads.get("fortune_planchette");
		pose.pushPose();
		pose.translate(0.5F, 0.0F, 0.5F);
		pose.rotateDegrees(Axis.YP, -RockingChairRenderer.yRotation(state.facing));
		pose.translate(-0.5F, 0.0F, -0.5F);
		if (planchette != null) {
			pose.pushPose();
			pose.translate((state.x - 8.0F) / 16, 0.0F, (state.z - 8.0F) / 16);
			planchette.submit(pose, collector, state.lightCoords);
			pose.popPose();
		}
		float x0 = CARD[0] / 16;
		float z0 = CARD[1] / 16;
		float x1 = CARD[2] / 16;
		float z1 = CARD[3] / 16;
		float y0 = TOP / 16;
		float y1 = (TOP + THICK) / 16;
		// The card flips sideways over its long middle, lifting as it turns.
		float middleX = (x0 + x1) / 2;
		float middleY = (y0 + y1) / 2;
		pose.translate(middleX, middleY + Mth.sin(state.flip * Mth.DEG_TO_RAD) * 1.5F / 16, 0.0F);
		pose.rotateDegrees(Axis.ZP, state.flip);
		pose.translate(-middleX, -middleY, 0.0F);
		int light = state.lightCoords;
		collector.submitCustomGeometry(pose, BACK, (matrix, buffer) -> DecorDraw.quad(buffer, matrix, new float[][] {
				{x0, y1, z0, 0, 0}, {x0, y1, z1, 0, 1}, {x1, y1, z1, 1, 1}, {x1, y1, z0, 1, 0}}, 0, 1, 0, 0xFFFFFFFF, light));
		// The picture is on the underside, the way up that reads from the north once the card has flipped over.
		collector.submitCustomGeometry(pose, FACES[state.card], (matrix, buffer) -> DecorDraw.quad(buffer, matrix, new float[][] {
				{x0, y0, z1, 0, 0}, {x0, y0, z0, 0, 1}, {x1, y0, z0, 1, 1}, {x1, y0, z1, 1, 0}}, 0, -1, 0, 0xFFFFFFFF, light));
		pose.popPose();
	}
}
