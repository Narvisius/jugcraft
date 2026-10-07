package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.agriculture.AuraCandleBlock;
import io.github.jimbozoomer.jugcraft.agriculture.AuraCandleBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.CandleMix;
import io.github.jimbozoomer.jugcraft.agriculture.CandleWax;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * An Aura Candle on its dish (the block model): the candle in its outermost layer's colour, as wide as its layers make
 * it and burning down to a quarter of its height, with a drip of wax down two sides, its wick, and while lit a
 * flickering flame in its first scent's colour (warm yellow when it has none) round a warm white heart, at full brightness.
 */
public class AuraCandleRenderer implements BlockEntityRenderer<AuraCandleBlockEntity, AuraCandleRenderer.State> {
	private static final RenderType WAX = RenderTypes.entityCutout(Jugcraft.id("textures/entity/candle_wax.png"));
	private static final RenderType FLAME = RenderTypes.entityCutout(Jugcraft.id("textures/entity/candle_flame.png"));
	private static final int FULL_BRIGHT = 0xF000F0;
	private static final int WICK = 0xFF2A2018;
	private static final int WARM_FLAME = 0xFFFFC870;
	/** The flame's untinted heart, drawn in front of its coloured edge so every flame reads as fire. */
	private static final int CORE = 0xFFFFF4C8;
	/** The flame is two crossed planes. */
	private static final double[] TURNS = {Math.PI / 4, -Math.PI / 4};
	/** Half the candle's width, in pixels, by layers. */
	private static final float[] HALF_WIDTH = {1.0F, 1.5F, 2.0F, 3.0F};

	public static final class State extends BlockEntityRenderState {
		CandleMix mix = CandleMix.plain(CandleWax.TALLOW);
		boolean lit;
		float time;
	}

	public AuraCandleRenderer(BlockEntityRendererProvider.Context context) {
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(AuraCandleBlockEntity candle, State state, float partialTick, Vec3 camera,
			ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
		BlockEntityRenderState.extractBase(candle, state, crumbling);
		state.mix = candle.mix();
		state.lit = candle.getBlockState().hasProperty(AuraCandleBlock.LIT) && candle.getBlockState().getValue(AuraCandleBlock.LIT);
		state.time = candle.getLevel() == null ? 0 : candle.getLevel().getGameTime() + partialTick;
	}

	@Override
	public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
		CandleMix mix = state.mix;
		float hw = HALF_WIDTH[mix.dips() - 1];
		float top = (float) AuraCandleBlockEntity.top(mix) * 16.0F;
		int color = 0xFF000000 | mix.color();
		int drip = 0xFF000000 | WaxPotRenderer.darker(mix.color());
		int light = state.lightCoords;
		collector.submitCustomGeometry(pose, WAX, (matrix, buffer) -> {
			TintedBoxes.box(buffer, matrix, 8 - hw, 1, 8 - hw, 8 + hw, top, 8 + hw, color, light);
			TintedBoxes.box(buffer, matrix, 8 + hw, Math.max(1, top - 3), 7.5F, 8 + hw + 0.4F, top - 0.2F, 8.5F, drip, light);
			TintedBoxes.box(buffer, matrix, 7.4F, Math.max(1, top - 2), 8 - hw - 0.4F, 8.2F, top - 0.2F, 8 - hw, drip, light);
			TintedBoxes.box(buffer, matrix, 7.75F, top, 7.75F, 8.25F, top + 1.2F, 8.25F, WICK, light);
		});
		if (!state.lit) {
			return;
		}
		int flame = mix.scents().isEmpty() || mix.muddled() ? WARM_FLAME : 0xFF000000 | mix.scents().get(0).color;
		float flicker = 1.0F + 0.12F * Mth.sin(state.time * 0.9F) + 0.06F * Mth.sin(state.time * 2.3F);
		float height = (4.5F + mix.dips() * 0.75F) * flicker;
		float width = 3.0F + mix.dips() * 0.5F;
		float base = top + 0.4F;
		collector.submitCustomGeometry(pose, FLAME, (matrix, buffer) -> {
			for (double turn : TURNS) {
				// Each side of a flame stands off its middle (never one plane drawn twice), the bright core further out so it
				// shows in front of the flame from either side.
				TintedBoxes.plane(buffer, matrix, turn, width, base, base + height, flame, FULL_BRIGHT, DecorDraw.TWO_SIDED_LIFT);
				TintedBoxes.plane(buffer, matrix, turn, width * 0.5F, base, base + height * 0.6F, CORE, FULL_BRIGHT, 3 * DecorDraw.TWO_SIDED_LIFT);
			}
		});
	}
}
