package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.jimbozoomer.jugcraft.agriculture.DecorationBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.FloatingTableSettingBlock;
import io.github.jimbozoomer.jugcraft.agriculture.MourningAngelBlock;
import java.util.List;
import java.util.Map;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Draws a Floating Table Setting (decor19_quads.json: {@code setting_*}, laid for a place facing north): each piece hovers
 * and bobs out of step with the others. Laid for dinner, the goblet now and then tips over at night and rights itself;
 * laid for tea, the pot now and then tips and pours a cup; the candle's flame flickers while lit.
 */
public class TableSettingRenderer implements BlockEntityRenderer<DecorationBlockEntity, TableSettingRenderer.State> {
	private static final Map<FloatingTableSettingBlock.Setting, List<String>> PIECES = Map.of(
			FloatingTableSettingBlock.Setting.DINNER, List.of("setting_plate", "setting_cutlery", "setting_goblet", "setting_candlestick"),
			FloatingTableSettingBlock.Setting.TEA, List.of("setting_saucer", "setting_cup", "setting_teapot", "setting_candlestick"),
			FloatingTableSettingBlock.Setting.FEAST, List.of("setting_platter", "setting_goblet", "setting_candlestick"));
	/** Where the goblet and the teapot stand, and the candle's wick, in pixels (tools/decor19_data.py draws them there). */
	private static final float[] GOBLET = {11.5F, 1.5F, 5.0F};
	private static final float[] TEAPOT = {5.0F, 1.5F, 6.0F};
	private static final float[] WICK = {12.5F, 9.6F, 11.5F};
	private static final int TIP_PERIOD = 300;

	public static final class State extends BlockEntityRenderState {
		Direction facing = Direction.NORTH;
		FloatingTableSettingBlock.Setting setting = FloatingTableSettingBlock.Setting.DINNER;
		boolean lit;
		boolean night;
		float time;
	}

	public TableSettingRenderer(BlockEntityRendererProvider.Context context) {
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(DecorationBlockEntity place, State state, float partialTick, Vec3 camera,
			ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
		BlockEntityRenderState.extractBase(place, state, crumbling);
		BlockState block = place.getBlockState();
		if (!(block.getBlock() instanceof FloatingTableSettingBlock) || place.getLevel() == null) {
			return;
		}
		state.facing = block.getValue(FloatingTableSettingBlock.FACING);
		state.setting = block.getValue(FloatingTableSettingBlock.SETTING);
		state.lit = block.getValue(FloatingTableSettingBlock.LIT);
		state.night = MourningAngelBlock.night(place.getLevel());
		state.time = Math.floorMod(place.getLevel().getGameTime() + (place.getBlockPos().hashCode() & 511), 24000L) + partialTick;
	}

	/** How far over a piece is tipped (degrees) at {@code time}: for 40 ticks in each period, over, held, and back. */
	static float tip(float time, int period, float most) {
		float into = time % period;
		if (into >= 40) {
			return 0.0F;
		}
		float t = into < 10 ? into / 10 : into < 30 ? 1.0F : (40 - into) / 10;
		return most * t * t * (3 - 2 * t);
	}

	@Override
	public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
		pose.pushPose();
		pose.translate(0.5F, 0.0F, 0.5F);
		pose.rotateDegrees(Axis.YP, -RockingChairRenderer.yRotation(state.facing));
		pose.translate(-0.5F, 0.0F, -0.5F);
		List<String> pieces = PIECES.get(state.setting);
		for (int i = 0; i < pieces.size(); i++) {
			QuadModel piece = DecorQuads.get(pieces.get(i));
			if (piece == null) {
				continue;
			}
			pose.pushPose();
			pose.translate(0.0F, (0.6F + 0.6F * Mth.sin(state.time * 0.08F + i * 1.9F)) / 16, 0.0F);
			float[] pivot = null;
			float angle = 0.0F;
			if (pieces.get(i).equals("setting_goblet") && state.night) {
				pivot = GOBLET;
				angle = tip(state.time, TIP_PERIOD, 85.0F);
			} else if (pieces.get(i).equals("setting_teapot")) {
				pivot = TEAPOT;
				angle = -tip(state.time + 150, TIP_PERIOD / 2, 40.0F);
			}
			if (pivot != null && angle != 0.0F) {
				pose.translate(pivot[0] / 16, pivot[1] / 16, pivot[2] / 16);
				pose.rotateDegrees(Axis.ZP, angle);
				pose.translate(-pivot[0] / 16, -pivot[1] / 16, -pivot[2] / 16);
			}
			piece.submit(pose, collector, state.lightCoords);
			if (pieces.get(i).equals("setting_candlestick") && state.lit) {
				QuadModel flame = DecorQuads.get("setting_flame");
				if (flame != null) {
					float flicker = 1.0F + 0.15F * Mth.sin(state.time * 1.3F) + 0.08F * Mth.sin(state.time * 3.7F);
					pose.translate(WICK[0] / 16, WICK[1] / 16, WICK[2] / 16);
					pose.scale(1.0F, flicker, 1.0F);
					pose.translate(-WICK[0] / 16, -WICK[1] / 16, -WICK[2] / 16);
					flame.submit(pose, collector, 0xF000F0);
				}
			}
			pose.popPose();
		}
		pose.popPose();
	}
}
