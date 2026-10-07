package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.agriculture.CandyBase;
import io.github.jimbozoomer.jugcraft.agriculture.CandyKettleBlock;
import io.github.jimbozoomer.jugcraft.agriculture.CandyKettleBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.CandyStage;
import io.github.jimbozoomer.jugcraft.agriculture.Candies;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * A Candy Kettle's batch and its thermometer: the syrup (or cream) at the height of its sugar, in its colour, browning
 * past hard crack and black once burnt; and the needle on the dial clipped to the kettle's front, swinging from
 * {@value CandyKettleBlockEntity#ROOM} degrees at bottom left round to {@value CandyKettleBlockEntity#MAX_TEMP} at bottom
 * right, over the stages painted on the dial.
 */
public class CandyKettleRenderer implements BlockEntityRenderer<CandyKettleBlockEntity, CandyKettleRenderer.State> {
	private static final RenderType SYRUP = RenderTypes.entityCutout(Jugcraft.id("textures/entity/candy_syrup.png"));
	private static final RenderType NEEDLE = RenderTypes.entityTranslucent(Jugcraft.id("textures/entity/candy_needle.png"));
	/** The dial's centre, in the kettle's pixels as it faces north, and how far round the needle swings (degrees). */
	static final float DIAL_X = 8.0F;
	static final float DIAL_Y = 6.5F;
	static final float DIAL_Z = 1.0F;
	static final float SWING = 270.0F;
	private static final int SYRUP_COLOR = 0xF2E6BE;
	private static final int CREAM_COLOR = 0xF4EEDC;
	private static final int CARAMEL_COLOR = 0xB86A1E;
	private static final int BURNT_COLOR = 0x2A1A10;

	public static final class State extends BlockEntityRenderState {
		Direction facing = Direction.NORTH;
		boolean filled;
		int sugar;
		boolean cream;
		int color;
		int cooked;
		int temperature;
	}

	public CandyKettleRenderer(BlockEntityRendererProvider.Context context) {
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(CandyKettleBlockEntity kettle, State state, float partialTick, Vec3 camera,
			ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
		BlockEntityRenderState.extractBase(kettle, state, crumbling);
		state.facing = kettle.getBlockState().hasProperty(CandyKettleBlock.FACING) ? kettle.getBlockState().getValue(CandyKettleBlock.FACING)
				: Direction.NORTH;
		state.filled = kettle.base() != null || kettle.sugar() > 0;
		state.sugar = kettle.sugar();
		state.cream = kettle.base() == CandyBase.CREAM;
		state.color = kettle.dyed() || !kettle.flavours().isEmpty() ? kettle.color() : Candies.NATURAL;
		state.cooked = kettle.cooked();
		state.temperature = kettle.temperature();
	}

	/** The needle's angle, clockwise from straight up, at {@code temperature}. */
	static float angle(int temperature) {
		float range = CandyKettleBlockEntity.MAX_TEMP - CandyKettleBlockEntity.ROOM;
		return -SWING / 2 + SWING * (temperature - CandyKettleBlockEntity.ROOM) / range;
	}

	/** The batch's colour: its own (or plain syrup's or cream's), browning towards caramel past hard crack, black once burnt. */
	static int syrupColor(State state) {
		int own = state.color != Candies.NATURAL ? state.color : state.cream ? CREAM_COLOR : SYRUP_COLOR;
		if (state.cooked >= CandyStage.BURNT.from) {
			return BURNT_COLOR;
		}
		if (state.cooked >= CandyStage.HARD_CRACK.from) {
			float t = (state.cooked - CandyStage.HARD_CRACK.from) / (float) (CandyStage.BURNT.from - CandyStage.HARD_CRACK.from);
			return mix(own, CARAMEL_COLOR, t);
		}
		return own;
	}

	private static int mix(int a, int b, float t) {
		int r = Math.round(((a >> 16) & 0xFF) * (1 - t) + ((b >> 16) & 0xFF) * t);
		int g = Math.round(((a >> 8) & 0xFF) * (1 - t) + ((b >> 8) & 0xFF) * t);
		int bl = Math.round((a & 0xFF) * (1 - t) + (b & 0xFF) * t);
		return (r << 16) | (g << 8) | bl;
	}

	@Override
	public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
		int light = state.lightCoords;
		pose.pushPose();
		pose.translate(0.5F, 0.0F, 0.5F);
		pose.rotateDegrees(Axis.YP, -RockingChairRenderer.yRotation(state.facing));
		pose.translate(-0.5F, 0.0F, -0.5F);
		if (state.filled) {
			float y = 3.0F + 1.25F * Math.max(1, state.sugar);
			int color = 0xFF000000 | syrupColor(state);
			collector.submitCustomGeometry(pose, SYRUP, (matrix, buffer) -> TintedBoxes.top(buffer, matrix, 3.0F, 3.0F, 13.0F, 13.0F, y, color, light));
		}
		// The needle in a pose of its own, so the syrup's (already submitted) is left as it was.
		pose.pushPose();
		pose.translate(DIAL_X / 16, DIAL_Y / 16, (DIAL_Z - 0.06F) / 16);
		pose.rotateDegrees(Axis.ZP, angle(state.temperature));
		float w = 0.3F / 16;
		float tail = -0.6F / 16;
		float tip = 2.1F / 16;
		collector.submitCustomGeometry(pose, NEEDLE, (matrix, buffer) -> DecorDraw.quad(buffer, matrix,
				new float[][] {{w, tail, 0, 0, 1}, {-w, tail, 0, 1, 1}, {-w, tip, 0, 1, 0}, {w, tip, 0, 0, 0}}, 0, 0, -1, 0xFFFFFFFF, light));
		pose.popPose();
		pose.popPose();
	}
}
