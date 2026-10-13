package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.jimbozoomer.jugcraft.client.DroneDepotRenderer.Geometry;
import io.github.jimbozoomer.jugcraft.drone.CargoPackagerBlockEntity;
import io.github.jimbozoomer.jugcraft.drone.DepotView;
import io.github.jimbozoomer.jugcraft.tower.ExchangeStationBlock;
import io.github.jimbozoomer.jugcraft.tower.ExchangeStationBlockEntity;
import io.github.jimbozoomer.jugcraft.tower.ExchangeStations;
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

/** Aggregate stock on the intake's operator pod. Inventory and transfers remain server-owned. */
public final class CargoExchangeRenderer implements BlockEntityRenderer<ExchangeStationBlockEntity, CargoExchangeRenderer.State> {
	private static final RenderType INSTRUMENTS = RenderTypes.entityCutoutCull(LivePanelRenderer.TEXTURE);
	private static final double FRONT = .3;
	private final Font font;
	public static final class State extends BlockEntityRenderState {
		Direction facing = Direction.NORTH;
		boolean visible, large, connected, available, requested, shortage, unlimited;
		int items = -1, occupied = -1, slots = -1;
		String stock = "STOCK --", occupancy = "SLOTS --/--", status = "NO TOWER LINK";
	}
	public CargoExchangeRenderer(BlockEntityRendererProvider.Context context) { this(context.font()); }
	CargoExchangeRenderer(Font font) { this.font = font; }
	@Override public State createRenderState() { return new State(); }
	@Override public int getViewDistance() { return 48; }
	@Override public boolean shouldRenderOffScreen() { return true; }
	@Override public void extractRenderState(ExchangeStationBlockEntity station, State state, float partialTick,
			Vec3 cameraPos, ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
		BlockEntityRenderState.extractBase(station, state, crumbling);
		state.visible = station.getBlockState().is(ExchangeStations.CARGO)
				&& station.getBlockState().getValue(ExchangeStationBlock.PART) == 0 && !station.isRemoved();
		if (state.visible) state.facing = station.getBlockState().getValue(ExchangeStationBlock.FACING);
		state.large = state.visible && station.getBlockState().getValue(ExchangeStationBlock.LARGE);
		DepotView view = state.visible ? EnergyExchangeRenderer.linkedView(station) : null;
		var store = view == null || view.cargo == null ? null : view.cargo.packagerPos();
		var level = station.getLevel();
		boolean loaded = store != null && level != null && level.isLoaded(store)
				&& level.getBlockEntity(store) instanceof CargoPackagerBlockEntity packager && !packager.isRemoved();
		populate(state, view, loaded);
	}
	static void populate(State state, @Nullable DepotView view, boolean storeLoaded) {
		state.connected = view != null; state.available = state.requested = state.shortage = state.unlimited = false;
		state.items = state.occupied = state.slots = -1;
		state.stock = "STOCK --"; state.occupancy = "SLOTS --/--"; state.status = "NO TOWER LINK";
		if (view == null) return;
		state.requested = view.openTargets > 0;
		if (view.cargo == null) { state.status = "AWAIT SNAPSHOT"; return; }
		if (!storeLoaded || view.cargo.packagerPos() == null) { state.status = "STORE UNAVAILABLE"; return; }
		DepotView.CargoView cargo = view.cargo;
		if (cargo.totalSlots() <= 0 || cargo.occupiedSlots() < 0 || cargo.occupiedSlots() > cargo.totalSlots() || cargo.items() < 0) {
			state.status = "STOCK UNKNOWN"; return;
		}
		state.available = true; state.items = cargo.items(); state.occupied = cargo.occupiedSlots(); state.slots = cargo.totalSlots();
		state.unlimited = cargo.unlimited(); state.shortage = view.openTargets > 0 && !view.missing.isEmpty() && !state.unlimited;
		state.stock = String.format(Locale.ROOT, "STOCK %,d ITEMS", state.items);
		state.occupancy = state.occupied + "/" + state.slots + " SLOTS";
		state.status = state.unlimited ? "TEST SUPPLY" : view.openTargets == 0 ? "IDLE"
				: state.shortage ? "SHORT " + view.missing.size() + " TYPES" : "SUPPLY READY";
	}
	@Override public void submit(State state, PoseStack poses, SubmitNodeCollector collector, CameraRenderState camera) {
		if (!state.visible) return;
		poses.pushPose(); poses.translate(.5, 0, .5);
		float rotation = switch (state.facing) { case EAST -> -(float) Math.PI / 2; case SOUTH -> (float) Math.PI;
			case WEST -> (float) Math.PI / 2; default -> 0; };
		poses.mulPose(new org.joml.Matrix4f().rotationY(rotation));
		poses.translate(-.5, 0, -.5); poses.scale(1 / 16F, 1 / 16F, 1 / 16F);
		if (state.large) poses.translate(32, 0, 0);
		// The native pod front is north/-z. These are anchor-relative model pixels.
		text(poses, collector, state.stock, 33.8, false);
		text(poses, collector, state.occupancy, 31.7, false);
		text(poses, collector, state.status, 29.6, state.shortage);
		collector.submitCustomGeometry(poses, INSTRUMENTS, (pose, consumer) -> {
			Geometry g = new Geometry(pose, consumer); g.light = LightCoordsUtil.FULL_BRIGHT;
			lamp(g, 6.2, state.connected, 0xFF72D5A4); lamp(g, 10.5, state.requested, 0xFFFFC45B);
		});
		poses.popPose();
	}
	private void text(PoseStack poses, SubmitNodeCollector collector, String text, double y, boolean warning) {
		// The 13.6-model-pixel display holds at most 18 normal glyphs at this authored size.
		String bounded = text.length() <= 18 ? text : text.substring(0, 17) + "+";
		poses.pushPose(); poses.translate(11, y, FRONT - .01); float scale = .65F / 7;
		poses.scale(-scale, -scale, -scale);
		collector.submitText(poses, -font.width(bounded) / 2F, -4.5F, Component.literal(bounded).getVisualOrderText(), false,
				Font.DisplayMode.POLYGON_OFFSET, LightCoordsUtil.FULL_BRIGHT, warning ? 0xFFFFC45B : 0xFFD2E8CF, 0, 0);
		poses.popPose();
	}
	private static void lamp(Geometry g, double x, boolean lit, int color) {
		if (!lit) return;
		g.color = color;
		g.quad((float) (x - .4), 25.1F, (float) FRONT, (float) (x - .4), 25.9F, (float) FRONT,
				(float) (x + .4), 25.9F, (float) FRONT, (float) (x + .4), 25.1F, (float) FRONT,
				0, 0, -1, LivePanelRenderer.LENS);
	}
}
