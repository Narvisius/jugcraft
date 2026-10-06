package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.jimbozoomer.jugcraft.nuclear.MultiFurnitureBlock;
import io.github.jimbozoomer.jugcraft.nuclear.StatusScreenBlockEntity;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.phys.Vec3;

/**
 * The plant status screen's live page, drawn over the whole 3 x 2 screen from its bottom middle block: the reactor's
 * state in big letters, power with a bar, rods, core temperature, the generator's output, fuel, alarms, and the core
 * temperature trend as a sparkline.
 */
public class StatusScreenRenderer implements BlockEntityRenderer<StatusScreenBlockEntity, StatusScreenRenderer.State> {
	private static final String SPARKS = "▁▂▃▄▅▆▇█";
	private final Font font;

	record Line(float x, float y, float scale, FormattedCharSequence text, int colour) {
	}

	public static class State extends BlockEntityRenderState {
		Direction facing = Direction.NORTH;
		final List<Line> lines = new ArrayList<>();
	}

	public StatusScreenRenderer(BlockEntityRendererProvider.Context context) {
		this.font = context.font();
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public boolean shouldRenderOffScreen() {
		return true;
	}

	@Override
	public void extractRenderState(StatusScreenBlockEntity screen, State state, float partialTick, Vec3 cameraPos,
			ModelFeatureRenderer.CrumblingOverlay crumbling) {
		BlockEntityRenderState.extractBase(screen, state, crumbling);
		state.facing = screen.getBlockState().getBlock() instanceof MultiFurnitureBlock ? screen.getBlockState().getValue(MultiFurnitureBlock.FACING) : Direction.NORTH;
		state.lines.clear();
		// Page coordinates: 240 x 150 font pixels over the 3 x 2 block screen inside its frame.
		add(state, 10, 8, 1, "UNIT 1   PLANT STATUS", 0xFFE8EEF4);
		add(state, 178, 8, 1, "● LIVE", 0xFF60E070);
		String big;
		int colour;
		switch (screen.status) {
			case 1 -> {
				big = "REACTOR ONLINE";
				colour = 0xFF5CE07A;
			}
			case 2 -> {
				big = "REACTOR SHUT DOWN";
				colour = 0xFFF0C040;
			}
			case 3 -> {
				big = "REACTOR TRIPPED";
				colour = 0xFFFF5A4A;
			}
			default -> {
				big = "NO SIGNAL";
				colour = 0xFF8A949E;
			}
		}
		add(state, 10, 24, 2, big, colour);
		if (screen.status == 0) {
			add(state, 10, 52, 1, "No reactor within range of this screen.", 0xFFB0B8C0);
			return;
		}
		int power = (int) Math.round(screen.power * 100);
		add(state, 10, 52, 1, "Thermal power", 0xFFB0B8C0);
		add(state, 104, 52, 1, power + "%  " + bar(screen.power, 12), 0xFFE8EEF4);
		add(state, 10, 64, 1, "Control rods", 0xFFB0B8C0);
		add(state, 104, 64, 1, Math.round(screen.rods) + "% inserted", 0xFFE8EEF4);
		add(state, 10, 76, 1, "Core temperature", 0xFFB0B8C0);
		add(state, 104, 76, 1, Math.round(screen.temperature) + " °C", screen.temperature >= 350 ? 0xFFFF5A4A : 0xFFE8EEF4);
		add(state, 10, 88, 1, "Generator output", 0xFFB0B8C0);
		add(state, 104, 88, 1, String.format("%,d JE/t", screen.electric), 0xFFE8EEF4);
		add(state, 10, 100, 1, "Fuel assemblies", 0xFFB0B8C0);
		add(state, 104, 100, 1, screen.fuel + " / " + screen.fuelSize, 0xFFE8EEF4);
		add(state, 10, 112, 1, "Alarms", 0xFFB0B8C0);
		int alarms = Integer.bitCount(screen.alarms);
		add(state, 104, 112, 1, alarms == 0 ? "none" : alarms + " active", alarms == 0 ? 0xFF5CE07A : 0xFFFF5A4A);
		add(state, 10, 128, 0.75F, "Core temperature trend", 0xFF8A949E);
		// The trend line is drawn to fit the page whatever the font's block characters measure.
		String spark = spark(screen.trend);
		float fit = Math.min(1F, 220F / Math.max(1, font.width(spark)));
		add(state, 10, 138, fit, spark, 0xFF5AB0F0);
	}

	private void add(State state, float x, float y, float scale, String text, int colour) {
		state.lines.add(new Line(x, y, scale, Component.literal(text).getVisualOrderText(), colour));
	}

	private static String bar(double value, int cells) {
		int filled = (int) Math.round(Math.max(0, Math.min(1, value)) * cells);
		return "▮".repeat(filled) + "▯".repeat(cells - filled);
	}

	private static String spark(int[] trend) {
		int lo = Integer.MAX_VALUE, hi = Integer.MIN_VALUE;
		for (int t : trend) {
			lo = Math.min(lo, t);
			hi = Math.max(hi, t);
		}
		StringBuilder out = new StringBuilder();
		for (int c = 0; c < 36; c++) {
			int i = c * trend.length / 36;
			int k = hi == lo ? 3 : (int) Math.round((trend[i] - lo) * 7.0 / (hi - lo));
			out.append(SPARKS.charAt(Math.max(0, Math.min(7, k))));
		}
		return out.toString();
	}

	@Override
	public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
		pose.pushPose();
		// The screen block faces the way its viewer looks; the page faces back out of the wall.
		Direction out = state.facing.getOpposite();
		pose.translate(0.5, 0, 0.5);
		pose.mulPose(new org.joml.Matrix4f().rotationY((float) Math.toRadians(-out.toYRot())));
		// Local frame: x to the viewer's right, y up, z out of the wall (the wall at z = -0.5); the screen's glass is
		// 1.6 px out, the page just in front of it. The page spans x -1.4..1.4, y 1.9..0.1.
		float pageScale = 2.8F / 240F;
		pose.translate(-1.4F, 1.9F, -0.5F + 1.6F / 16 + 0.004F);
		pose.scale(pageScale, -pageScale, pageScale);
		for (Line line : state.lines) {
			pose.pushPose();
			pose.translate(line.x(), line.y(), 0);
			pose.scale(line.scale(), line.scale(), 1);
			collector.submitText(pose, 0, 0, line.text(), false, Font.DisplayMode.POLYGON_OFFSET, LightCoordsUtil.FULL_BRIGHT, line.colour(), 0, 0);
			pose.popPose();
		}
		pose.popPose();
	}
}
