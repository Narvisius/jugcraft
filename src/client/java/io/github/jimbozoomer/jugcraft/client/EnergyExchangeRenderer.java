package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.jimbozoomer.jugcraft.client.DroneDepotRenderer.Geometry;
import io.github.jimbozoomer.jugcraft.drone.DepotView;
import io.github.jimbozoomer.jugcraft.drone.DroneTerminalBlockEntity;
import io.github.jimbozoomer.jugcraft.tower.ExchangePortBlock;
import io.github.jimbozoomer.jugcraft.tower.ExchangeStationBlock;
import io.github.jimbozoomer.jugcraft.tower.ExchangeStationBlockEntity;
import io.github.jimbozoomer.jugcraft.tower.ExchangeStations;
import io.github.jimbozoomer.jugcraft.tower.JugcraftTower;
import java.util.Locale;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/** The station's instruments read its exact terminal snapshot. No ticker, buffer or world search. */
public final class EnergyExchangeRenderer implements BlockEntityRenderer<ExchangeStationBlockEntity, EnergyExchangeRenderer.State> {
	private static final RenderType INSTRUMENTS = RenderTypes.entityCutoutCull(LivePanelRenderer.TEXTURE);
	private static final double FRONT = .05;
	private final Font font;
	public static final class State extends BlockEntityRenderState {
		Direction facing = Direction.NORTH;
		boolean visible, large, connected, charged, lowPower;
		long energy, capacity, demand, load;
		double charge;
		String stored = "-- JE", total = "NO TOWER LINK", draw = "-- JE/t", loading = "LINK OFFLINE";
	}
	public EnergyExchangeRenderer(BlockEntityRendererProvider.Context context) { this(context.font()); }
	EnergyExchangeRenderer(Font font) { this.font = font; }
	@Override public State createRenderState() { return new State(); }
	@Override public int getViewDistance() { return 48; }
	// The instruments are up to four metres from the front-corner controller block.
	@Override public boolean shouldRenderOffScreen() { return true; }

	/** Constant loaded endpoint lookups only; saved owner/depot authority remains entirely server-side. */
	static @Nullable DepotView linkedView(ExchangeStationBlockEntity station) {
		var level = station.getLevel();
		var terminalPos = station.displayTerminalPos(); var corePos = station.displayCorePos();
		if (level == null || station.isRemoved() || terminalPos == null || corePos == null || station.displayDepotId() == null
				|| !level.isLoaded(terminalPos) || !level.isLoaded(corePos)
				|| station.getBlockPos().distSqr(terminalPos) > ExchangePortBlock.RANGE * ExchangePortBlock.RANGE
				|| !level.getBlockState(corePos).is(JugcraftTower.CORE)
				|| !(level.getBlockEntity(terminalPos) instanceof DroneTerminalBlockEntity terminal) || terminal.isRemoved()) return null;
		DepotView view = terminal.view();
		return view != null && view.towerTier >= 1 && corePos.equals(view.towerPos)
				&& station.displayDepotId().equals(view.depotId) ? view : null;
	}
	@Override public void extractRenderState(ExchangeStationBlockEntity station, State state, float partialTick,
			Vec3 cameraPos, ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
		BlockEntityRenderState.extractBase(station, state, crumbling);
		state.visible = station.getBlockState().is(ExchangeStations.ENERGY)
				&& station.getBlockState().getValue(ExchangeStationBlock.PART) == 0 && !station.isRemoved();
		if (state.visible) state.facing = station.getBlockState().getValue(ExchangeStationBlock.FACING);
		state.large = state.visible && station.getBlockState().getValue(ExchangeStationBlock.LARGE);
		populate(state, state.visible ? linkedView(station) : null);
	}
	static void populate(State state, @Nullable DepotView view) {
		state.connected = view != null; state.charged = false; state.lowPower = false;
		state.energy = state.capacity = state.demand = state.load = 0; state.charge = 0;
		state.stored = "-- JE"; state.total = "NO TOWER LINK";
		state.draw = "-- JE/t"; state.loading = "LINK OFFLINE";
		if (view == null) return;
		state.energy = view.energy; state.capacity = view.capacity; state.demand = view.draw(); state.load = view.loadPercent();
		state.charge = view.capacity > 0 ? Math.max(0, Math.min(100, (double) view.energy * 100 / view.capacity)) : 0;
		state.charged = view.capacity > 0 && view.energy >= view.capacity; state.lowPower = view.lowPower;
		state.stored = String.format(Locale.ROOT, "%,d JE", view.energy);
		state.total = String.format(Locale.ROOT, "OF %,d JE", view.capacity);
		state.draw = String.format(Locale.ROOT, "%,d JE/t", state.demand);
		state.loading = "LOAD " + state.load + "%";
	}
	/** North-facing scales: zero lower-left, half scale up, full scale lower-right. */
	static double needleAngle(double percent) { return Math.toRadians(210 - 240 * Math.max(0, Math.min(100, percent)) / 100); }
	@Override public void submit(State state, PoseStack poses, SubmitNodeCollector collector, CameraRenderState camera) {
		if (!state.visible) return;
		poses.pushPose(); poses.translate(.5, 0, .5);
		float rotation = switch (state.facing) { case EAST -> -(float) Math.PI / 2; case SOUTH -> (float) Math.PI;
			case WEST -> (float) Math.PI / 2; default -> 0; };
		poses.mulPose(new org.joml.Matrix4f().rotationY(rotation));
		poses.translate(-.5, 0, -.5); poses.scale(1 / 16F, 1 / 16F, 1 / 16F);
		if (state.large) poses.translate(32, 0, 0);
		// Matches tools/tower_power_exchange.py and tower_power_exchange_panels.json, in model pixels.
		collector.submitCustomGeometry(poses, INSTRUMENTS, (pose, consumer) -> {
			Geometry geometry = new Geometry(pose, consumer); geometry.light = state.lightCoords;
			needle(geometry, 21, 35, needleAngle(state.charge), 2.35);
			needle(geometry, 27, 35, needleAngle(state.load), 2.35);
			needle(geometry, 54.35, 26.6, meterAngle(state.charge), 2.75);
			needle(geometry, 61.35, 26.6, meterAngle(state.load), 2.75);
			lamp(geometry, 35.6, 40, state.connected, 0xFF72D5A4);
			lamp(geometry, 35.6, 36.8, state.charged, 0xFFFFC45B);
			lamp(geometry, 35.6, 33.6, state.lowPower, 0xFFFF5C47);
		});
		text(poses, collector, state.stored, 24, 28.0);
		text(poses, collector, state.total, 24, 26.1);
		text(poses, collector, state.draw, 56, 39.0);
		text(poses, collector, state.loading, 56, 37.1);
		poses.popPose();
	}
	private void text(PoseStack poses, SubmitNodeCollector collector, String text, double x, double y) {
		poses.pushPose(); poses.translate(x, y, FRONT - .01);
		float scale = .62F / 7; poses.scale(-scale, -scale, -scale);
		collector.submitText(poses, -font.width(text) / 2F, -4.5F, Component.literal(text).getVisualOrderText(), false,
				Font.DisplayMode.POLYGON_OFFSET, LightCoordsUtil.FULL_BRIGHT, 0xFFD2E8CF, 0, 0);
		poses.popPose();
	}
	static double meterAngle(double percent) { return Math.toRadians(90 - 90 * Math.max(0, Math.min(100, percent)) / 100); }
	private static void needle(Geometry g, double x, double y, double angle, double radius) {
		double dx = -Math.cos(angle), dy = Math.sin(angle), length = radius * .82, tail = .29, width = .15;
		double nx = -dy * width / 2, ny = dx * width / 2;
		double ax = x - dx * tail, ay = y - dy * tail, bx = x + dx * length, by = y + dy * length;
		g.color = 0xFF272626;
		g.quad((float) (ax - nx), (float) (ay - ny), (float) FRONT, (float) (ax + nx), (float) (ay + ny), (float) FRONT,
				(float) (bx + nx), (float) (by + ny), (float) FRONT, (float) (bx - nx), (float) (by - ny), (float) FRONT,
				0, 0, -1, LivePanelRenderer.WHITE);
		frontSquare(g, x, y, FRONT - .01, .23, LivePanelRenderer.HUB);
	}
	private static void lamp(Geometry g, double x, double y, boolean lit, int color) {
		if (!lit) return;
		g.color = color; g.light = LightCoordsUtil.FULL_BRIGHT;
		frontSquare(g, x, y, FRONT, .48, LivePanelRenderer.LENS);
	}
	private static void frontSquare(Geometry g, double x, double y, double z, double radius, float[] uv) {
		g.quad((float) (x - radius), (float) (y - radius), (float) z, (float) (x - radius), (float) (y + radius), (float) z,
				(float) (x + radius), (float) (y + radius), (float) z, (float) (x + radius), (float) (y - radius), (float) z,
				0, 0, -1, uv);
	}
}
