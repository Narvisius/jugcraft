package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.jimbozoomer.jugcraft.nuclear.MultiFurnitureBlock;
import io.github.jimbozoomer.jugcraft.nuclear.PerfScreenBlockEntity;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
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
 * The unit performance screen's page (the administration building part 2; nuclear/PerfScreenBlockEntity.java), drawn over
 * the whole 3 x 2 screen from its bottom middle block as the plant status screen's is: the unit's output now in big
 * figures (OFF LINE, NO SIGNAL), the reactor's power, today's and this month's generation, the capacity factor, days on
 * line and since the last trip, and each hour of today's generation as a bar.
 */
public class PerfScreenRenderer implements BlockEntityRenderer<PerfScreenBlockEntity, PerfScreenRenderer.State> {
	private static final String BARS = "▁▂▃▄▅▆▇█";
	private final Font font;

	record Line(float x, float y, float scale, FormattedCharSequence text, int colour) {
	}

	public static class State extends BlockEntityRenderState {
		Direction facing = Direction.NORTH;
		final List<Line> lines = new ArrayList<>();
	}

	public PerfScreenRenderer(BlockEntityRendererProvider.Context context) {
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
	public void extractRenderState(PerfScreenBlockEntity screen, State state, float partialTick, Vec3 cameraPos,
			ModelFeatureRenderer.CrumblingOverlay crumbling) {
		BlockEntityRenderState.extractBase(screen, state, crumbling);
		state.facing = screen.getBlockState().getBlock() instanceof MultiFurnitureBlock ? screen.getBlockState().getValue(MultiFurnitureBlock.FACING) : Direction.NORTH;
		state.lines.clear();
		long dayTime = screen.getLevel() == null ? screen.dayTime : screen.getLevel().getOverworldClockTime();
		add(state, 10, 8, 1, "UNIT 1   PERFORMANCE", 0xFFE8EEF4);
		right(state, 230, 8, 1, io.github.jimbozoomer.jugcraft.nuclear.TscRoom.clock(dayTime), 0xFF78C6EE);
		String big;
		int colour;
		if (screen.status == 0) {
			big = "NO SIGNAL";
			colour = 0xFF8A949E;
		} else if (screen.mwe > 0) {
			big = screen.mwe + " MWe";
			colour = 0xFF5CE07A;
		} else {
			big = screen.status == 3 ? "TRIPPED" : "OFF LINE";
			colour = screen.status == 3 ? 0xFFFF5A4A : 0xFFF0C040;
		}
		add(state, 10, 24, 2, big, colour);
		add(state, 140, 26, 1, "NET OUTPUT", 0xFFB0B8C0);
		add(state, 140, 37, 1, screen.status == 0 ? "NO REACTOR" : "REACTOR " + screen.power + "%", 0xFFE8EEF4);
		row(state, 0, "Generation today", String.format(Locale.ROOT, "%,d MWh", Math.round(screen.dayMwh)));
		row(state, 1, "This month", String.format(Locale.ROOT, "%,d MWh", Math.round(screen.monthMwh)));
		double cf = screen.capacityFactor();
		row(state, 2, "Capacity factor", cf < 0 ? "--" : String.format(Locale.ROOT, "%.1f %%", Math.min(100, cf)));
		double on = screen.daysOnLine();
		row(state, 3, "Days on line", on < 0 ? "OFF LINE" : String.valueOf((long) on));
		row(state, 4, "Days since last trip", String.valueOf((long) screen.daysSinceTrip()));
		add(state, 10, 118, 0.75F, "Generation, each hour today (06:00 on)", 0xFF8A949E);
		StringBuilder bars = new StringBuilder();
		double full = io.github.jimbozoomer.jugcraft.nuclear.DeskScreenBlockEntity.RATED_MWE;
		for (double h : screen.hours) {
			int k = (int) Math.round(Math.max(0, Math.min(1, h / full)) * 7);
			bars.append(h <= 0 ? ' ' : BARS.charAt(k));
		}
		float fit = Math.min(1.6F, 220F / Math.max(1, font.width(bars.toString())));
		add(state, 10, 130, fit, bars.toString(), 0xFF5AB0F0);
	}

	private void row(State state, int i, String label, String value) {
		float y = 54 + i * 12;
		add(state, 10, y, 1, label, 0xFFB0B8C0);
		right(state, 230, y, 1, value, 0xFFE8EEF4);
	}

	private void add(State state, float x, float y, float scale, String text, int colour) {
		state.lines.add(new Line(x, y, scale, Texts.of(text), colour));
	}

	private void right(State state, float x, float y, float scale, String text, int colour) {
		state.lines.add(new Line(x - font.width(text) * scale, y, scale, Texts.of(text), colour));
	}

	@Override
	public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
		pose.pushPose();
		// As the plant status screen's (client/StatusScreenRenderer.java): the page on the screen's glass, out of the wall.
		Direction out = state.facing.getOpposite();
		pose.translate(0.5, 0, 0.5);
		pose.mulPose(new org.joml.Matrix4f().rotationY((float) Math.toRadians(-out.toYRot())));
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
