package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.jimbozoomer.jugcraft.drone.ControlScreenBlock;
import io.github.jimbozoomer.jugcraft.drone.DepotDisplayBlockEntity;
import io.github.jimbozoomer.jugcraft.drone.DepotView;
import io.github.jimbozoomer.jugcraft.drone.DroneTerminalBlockEntity;
import io.github.jimbozoomer.jugcraft.drone.PlatformLayout;
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
import net.minecraft.world.phys.Vec3;

/**
 * The live readout on a formed control room screen (six panels, 3x2), drawn by its top-left panel over
 * the whole screen: depot name and mode, a fleet bar, drones airborne, power bar, pads and supply
 * status. It reads the linked terminal's {@link DepotView}, so it costs nothing on the server.
 */
public class ControlScreenRenderer implements BlockEntityRenderer<DepotDisplayBlockEntity, ControlScreenRenderer.State> {
	/** Text pixels per block. */
	private static final float PIXELS = 64;
	private static final int ACCENT = 0xFFE05040;
	private static final int WHITE = 0xFFECE6E6;
	private static final int AMBER = 0xFFFFC04A;
	private static final int RED = 0xFFFF8A6A;
	private static final int DIM = 0xFF96463C;

	private final Font font;

	public static class State extends BlockEntityRenderState {
		Direction facing = Direction.NORTH;
		final List<Line> lines = new ArrayList<>();
	}

	record Line(FormattedCharSequence text, int color, float x, float y) {
	}

	public ControlScreenRenderer(BlockEntityRendererProvider.Context context) {
		this.font = context.font();
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(DepotDisplayBlockEntity screen, State state, float partialTick, Vec3 cameraPos,
			ModelFeatureRenderer.CrumblingOverlay crumbling) {
		BlockEntityRenderState.extractBase(screen, state, crumbling);
		state.lines.clear();
		state.facing = screen.getBlockState().getValue(ControlScreenBlock.FACING);
		if (!screen.isAnchor() || screen.getLevel() == null) {
			return;
		}
		BlockPos link = screen.terminal();
		DepotView view = link != null && screen.getLevel().getBlockEntity(link) instanceof DroneTerminalBlockEntity terminal ? terminal.view() : null;
		float y = 6;
		if (view == null) {
			add(state, "DRONE CONTROL", ACCENT, 8, y);
			add(state, "NO DEPOT LINKED", AMBER, 8, y + 14);
			add(state, "Place a Drone Depot Terminal", DIM, 8, y + 26);
			add(state, "within " + ControlScreenBlock.LINK_RANGE + " blocks.", DIM, 8, y + 36);
			return;
		}
		add(state, "DRONE DEPOT  ▸ " + view.mode.toUpperCase(java.util.Locale.ROOT), ACCENT, 8, y);
		y += 13;
		int fleet = view.docks.size() + view.flights.size();
		add(state, "FLEET " + fleet + "/" + view.maxDrones + "   AIRBORNE " + view.flying(), WHITE, 8, y);
		y += 10;
		add(state, bar(fleet, view.maxDrones, 24), ACCENT, 8, y);
		y += 12;
		long percent = view.capacity <= 0 ? 0 : view.energy * 100 / view.capacity;
		add(state, "POWER " + percent + "%   DRAW " + (view.flying() > 0 ? view.workingDraw : view.standbyDraw) + " JE/t",
				view.lowPower ? RED : WHITE, 8, y);
		y += 10;
		add(state, bar(view.energy, view.capacity, 24), view.lowPower ? RED : ACCENT, 8, y);
		y += 12;
		add(state, view.layoutStatus() == PlatformLayout.Status.OK
				? "PADS " + view.pads.size() + "   SLOTS " + view.slotsUsed + "/" + view.slots
				: "PLATFORM: " + view.layoutStatus().name().replace('_', ' '), view.layoutStatus() == PlatformLayout.Status.OK ? WHITE : AMBER, 8, y);
		y += 10;
		if (view.pickup == null) {
			add(state, "⚠ NO SUPPLY PICKUP", AMBER, 8, y);
		} else if (!view.packager) {
			add(state, "⚠ NO CARGO PACKAGER", AMBER, 8, y);
		} else if (!view.missing.isEmpty()) {
			var first = view.missing.entrySet().iterator().next();
			add(state, "⚠ NEED " + first.getValue() + "× " + first.getKey().replaceFirst("^[a-z0-9_]+:", "").replace('_', ' '), AMBER, 8, y);
		} else {
			add(state, "SUPPLY OK   JOBS " + view.openTargets, WHITE, 8, y);
		}
	}

	/** Width of the whole 3x2 screen in text pixels, less the margins. */
	private static final int TEXT_WIDTH = (int) (ControlScreenBlock.WIDTH * PIXELS) - 16;

	/** Adds a line, cut to fit the screen (ending in an ellipsis if it was cut). */
	private void add(State state, String text, int color, float x, float y) {
		String fitted = text;
		if (font.width(text) > TEXT_WIDTH - x + 8) {
			fitted = font.plainSubstrByWidth(text, (int) (TEXT_WIDTH - x + 8) - font.width("…")) + "…";
		}
		state.lines.add(new Line(Component.literal(fitted).getVisualOrderText(), color, x, y));
	}

	private static String bar(long value, long max, int cells) {
		int filled = max <= 0 ? 0 : (int) Math.min(cells, value * cells / max);
		return "▮".repeat(filled) + "▯".repeat(cells - filled);
	}

	@Override
	public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
		if (state.lines.isEmpty()) {
			return;
		}
		poseStack.pushPose();
		// Local frame: +z points out of the screen, +x to the viewer's right, +y up; origin at block centre.
		poseStack.translate(0.5, 0.5, 0.5);
		poseStack.mulPose(new org.joml.Matrix4f().rotationY((float) Math.toRadians(-state.facing.toYRot())));
		// Top-left corner of the whole 3x2 screen, just in front of the panel face.
		poseStack.translate(-0.5, 0.5, -0.5 + ControlScreenBlock.THICKNESS / 16.0 + 0.005);
		poseStack.scale(1 / PIXELS, -1 / PIXELS, 1 / PIXELS);
		for (Line line : state.lines) {
			collector.submitText(poseStack, line.x(), line.y(), line.text(), false, Font.DisplayMode.POLYGON_OFFSET,
					LightCoordsUtil.FULL_BRIGHT, line.color(), 0, 0);
		}
		poseStack.popPose();
	}
}
