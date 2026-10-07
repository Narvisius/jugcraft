package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.jimbozoomer.jugcraft.control.Channels;
import io.github.jimbozoomer.jugcraft.control.ControlMonitorBlock;
import io.github.jimbozoomer.jugcraft.control.ControlMonitorBlockEntity;
import io.github.jimbozoomer.jugcraft.control.LogicControllerBlockEntity;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.phys.Vec3;

/**
 * The control monitor's readout (batch 37), drawn by a formed screen's top-left panel over the whole 3x2 screen: a
 * row per channel that has a sensor or that the controller has on — its colour, name, reading, a bar, a two-minute
 * graph and ON/OFF. It reads the linked controller's synced data, so it costs the server nothing.
 */
public class ControlMonitorRenderer implements BlockEntityRenderer<ControlMonitorBlockEntity, ControlMonitorRenderer.State> {
	/** Text pixels per block. */
	private static final float PIXELS = 80;
	private static final int ROWS = 12;
	private static final int CYAN = 0xFF7FE6F0;
	private static final int DIM = 0xFF3C9AA8;
	private static final int WHITE = 0xFFE6F2F4;
	private static final int GREEN = 0xFF80FF90;
	private static final String SPARK = "▁▂▃▄▅▆▇█";

	private final Font font;

	public static class State extends BlockEntityRenderState {
		Direction facing = Direction.NORTH;
		final List<Line> lines = new ArrayList<>();
	}

	record Line(FormattedCharSequence text, int color, float x, float y) {
	}

	public ControlMonitorRenderer(BlockEntityRendererProvider.Context context) {
		this.font = context.font();
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(ControlMonitorBlockEntity monitor, State state, float partialTick, Vec3 cameraPos,
			ModelFeatureRenderer.CrumblingOverlay crumbling) {
		BlockEntityRenderState.extractBase(monitor, state, crumbling);
		state.lines.clear();
		state.facing = monitor.getBlockState().getValue(ControlMonitorBlock.FACING);
		if (!monitor.isAnchor() || monitor.getLevel() == null) {
			return;
		}
		BlockPos link = monitor.controller();
		add(state, "CONTROL MONITOR", CYAN, 8, 6);
		if (link == null || !(monitor.getLevel().getBlockEntity(link) instanceof LogicControllerBlockEntity controller)) {
			add(state, link == null ? "NO CONTROLLER" : "CONTROLLER OUT OF RANGE", WHITE, 8, 22);
			add(state, "Run a data cable from a panel", DIM, 8, 34);
			add(state, "to a logic controller.", DIM, 8, 44);
			return;
		}
		float y = 22;
		int shown = 0;
		for (DyeColor channel : DyeColor.values()) {
			int reading = controller.reading(channel);
			boolean on = controller.isOn(channel);
			if (reading < 0 && !on) {
				continue;
			}
			if (shown++ >= ROWS) {
				break;
			}
			int color = 0xFF000000 | LogicControllerScreen.COLORS[channel.ordinal()];
			add(state, "■", color, 8, y);
			add(state, Channels.name(channel).getString(), WHITE, 18, y);
			add(state, reading < 0 ? "--" : reading + "%", WHITE, 74, y);
			add(state, bar(reading, 8), color, 100, y);
			add(state, spark(controller.history(channel)), CYAN, 152, y);
			add(state, on ? "ON" : "OFF", on ? GREEN : DIM, 218, y);
			y += 11;
		}
		if (shown == 0) {
			add(state, "No sensors on this network yet.", DIM, 8, y);
		}
	}

	private void add(State state, String text, int color, float x, float y) {
		int room = (int) (ControlMonitorBlock.WIDTH * PIXELS - x - 4);
		String fitted = font.width(text) > room ? font.plainSubstrByWidth(text, room) : text;
		state.lines.add(new Line(Component.literal(fitted).getVisualOrderText(), color, x, y));
	}

	private static String bar(int percent, int cells) {
		int filled = percent <= 0 ? 0 : Math.min(cells, (percent * cells + 50) / 100);
		return "▮".repeat(filled) + "▯".repeat(cells - filled);
	}

	/** The last ten samples as a sparkline; a gap where there was no reading. */
	private static String spark(int[] history) {
		StringBuilder out = new StringBuilder();
		for (int i = Math.max(0, history.length - 10); i < history.length; i++) {
			int value = history[i];
			out.append(value < 0 ? ' ' : SPARK.charAt(Math.min(SPARK.length() - 1, value * SPARK.length() / 101)));
		}
		return out.toString();
	}

	@Override
	public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
		if (state.lines.isEmpty()) {
			return;
		}
		poseStack.pushPose();
		// Local frame: +z out of the screen, +x to the viewer's right, +y up; origin at the block centre.
		poseStack.translate(0.5, 0.5, 0.5);
		poseStack.mulPose(new org.joml.Matrix4f().rotationY((float) Math.toRadians(-state.facing.toYRot())));
		// The top-left corner of the whole screen, just in front of the panel face.
		poseStack.translate(-0.5, 0.5, -0.5 + ControlMonitorBlock.THICKNESS / 16.0 + 0.005);
		poseStack.scale(1 / PIXELS, -1 / PIXELS, 1 / PIXELS);
		for (Line line : state.lines) {
			collector.submitText(poseStack, line.x(), line.y(), line.text(), false, Font.DisplayMode.POLYGON_OFFSET,
					LightCoordsUtil.FULL_BRIGHT, line.color(), 0, 0);
		}
		poseStack.popPose();
	}
}
