package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.jimbozoomer.jugcraft.agriculture.DecorationBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.InflatableBlock;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Draws a Yard Inflatable's figure on its lower half. Switched on, it fills over {@link InflatableBlock#INFLATE_TICKS}
 * ticks, flopping forward as it rises and straightening as it fills; full, it wobbles in the breeze (more in the rain)
 * and glows from inside. Switched off, it sags and folds flat over {@link InflatableBlock#DEFLATE_TICKS}. A figure in
 * a chunk that has just come into view starts as it should be, full or flat.
 */
public class InflatableRenderer implements BlockEntityRenderer<DecorationBlockEntity, InflatableRenderer.State> {
	private static final int FULL_BRIGHT = 0xF000F0;
	/** How far it flops forward half way up or down. */
	private static final float FLOP_DEGREES = 25.0F;
	/** Each figure as this client last left it: {fill, game time}. */
	private final Map<DecorationBlockEntity, double[]> figures = new WeakHashMap<>();

	public static final class State extends BlockEntityRenderState {
		Direction facing = Direction.NORTH;
		String design = "ghost";
		float fill;
		float wobbleX;
		float wobbleZ;
	}

	public InflatableRenderer(BlockEntityRendererProvider.Context context) {
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(DecorationBlockEntity figure, State state, float partialTick, Vec3 camera,
			ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
		BlockEntityRenderState.extractBase(figure, state, crumbling);
		BlockState block = figure.getBlockState();
		Level level = figure.getLevel();
		if (!(block.getBlock() instanceof InflatableBlock inflatable) || level == null) {
			return;
		}
		state.facing = block.getValue(InflatableBlock.FACING);
		state.design = inflatable.design();
		boolean inflated = InflatableBlock.inflated(block);
		double now = level.getGameTime() + (double) partialTick;
		double[] at = figures.computeIfAbsent(figure, f -> new double[] {inflated ? 1.0 : 0.0, now});
		double elapsed = Math.max(0.0, Math.min(40.0, now - at[1]));
		double step = elapsed / (inflated ? InflatableBlock.INFLATE_TICKS : -InflatableBlock.DEFLATE_TICKS);
		at[0] = Mth.clamp(at[0] + step, 0.0, 1.0);
		at[1] = now;
		state.fill = (float) at[0];
		float[] wobble = InflatableBlock.wobble(figure.getBlockPos(), (float) (now % 24000.0), level.getRainLevel(partialTick));
		state.wobbleX = wobble[0] * state.fill;
		state.wobbleZ = wobble[1] * state.fill;
	}

	@Override
	public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
		QuadModel model = DecorQuads.get("inflatable_" + state.design);
		if (model == null) {
			return;
		}
		float fill = state.fill;
		float eased = fill * fill * (3.0F - 2.0F * fill);
		float flop = FLOP_DEGREES * 4.0F * fill * (1.0F - fill);
		pose.pushPose();
		pose.translate(0.5F, 0.0F, 0.5F);
		pose.rotateDegrees(Axis.YP, -RockingChairRenderer.yRotation(state.facing));
		pose.rotateDegrees(Axis.XP, state.wobbleX - flop);
		pose.rotateDegrees(Axis.ZP, state.wobbleZ);
		float wide = 1.0F + 0.2F * (1.0F - eased);
		pose.scale(wide, 0.08F + 0.92F * eased, wide);
		pose.translate(-0.5F, 0.0F, -0.5F);
		model.submit(pose, collector, fill > 0.5F ? FULL_BRIGHT : state.lightCoords);
		pose.popPose();
	}

	@Override
	public boolean shouldRenderOffScreen() {
		return true; // It stands two blocks tall.
	}
}
