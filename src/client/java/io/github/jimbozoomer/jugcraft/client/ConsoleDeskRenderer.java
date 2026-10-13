package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.jimbozoomer.jugcraft.drone.DepotDisplayBlockEntity;
import io.github.jimbozoomer.jugcraft.drone.DroneTerminalBlockEntity;
import io.github.jimbozoomer.jugcraft.tower.FurnitureBlock;
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
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.phys.Vec3;

/** Client-only readout on the actual monitor face, reusing its tower terminal's existing snapshot. */
public final class ConsoleDeskRenderer implements BlockEntityRenderer<DepotDisplayBlockEntity, ConsoleDeskRenderer.State> {
	private static final float PIXELS = 144;
	private final Font font;
	public static final class State extends BlockEntityRenderState {
		Direction facing = Direction.NORTH;
		final List<String> lines = new ArrayList<>();
		boolean warning;
	}
	public ConsoleDeskRenderer(BlockEntityRendererProvider.Context context) { this(context.font()); }
	ConsoleDeskRenderer(Font font) { this.font = font; }
	@Override public State createRenderState() { return new State(); }
	@Override public int getViewDistance() { return 48; }
	@Override public void extractRenderState(DepotDisplayBlockEntity desk, State state, float partialTick, Vec3 cameraPos,
			ModelFeatureRenderer.CrumblingOverlay crumbling) {
		BlockEntityRenderState.extractBase(desk, state, crumbling);
		state.lines.clear(); state.warning = false;
		state.facing = desk.getBlockState().getValue(FurnitureBlock.FACING);
		if (desk.getLevel() == null) return;
		var link = desk.terminal();
		if (link == null || !(desk.getLevel().getBlockEntity(link) instanceof DroneTerminalBlockEntity terminal)) {
			state.lines.add("DRONE OPERATIONS"); state.lines.add("NO TERMINAL LINK"); return;
		}
		populate(state, terminal.view(), desk.getLevel().getGameTime() + partialTick);
	}

	static void populate(State state, io.github.jimbozoomer.jugcraft.drone.@org.jspecify.annotations.Nullable DepotView view, double now) {
		state.lines.clear(); state.warning = false;
		if (view == null) { state.lines.add("DRONE OPERATIONS"); state.lines.add("SYNCHRONIZING..."); return; }
		int total = view.docks.size() + view.flights.size();
		state.lines.add("T" + view.towerTier + "  FLIGHT OPERATIONS");
		state.lines.add("FLEET " + total + "/" + view.maxDrones);
		state.lines.add("ACTIVE " + view.active() + "  AIR " + view.airborne(now));
		state.lines.add("BATTERY " + (view.capacity <= 0 ? 0 : view.energy * 100 / view.capacity) + "%");
		state.lines.add("DRAW " + view.draw() + " JE/t " + view.loadPercent() + "%");
		state.lines.add("PADS " + view.padCount() + "  FREE " + Math.max(0, view.maxDrones - total));
		state.warning = view.pickups.isEmpty() || !view.packager || !view.missing.isEmpty();
		state.lines.add(view.pickups.isEmpty() ? "NO PICKUP" : !view.packager ? "NO PACKAGER"
				: !view.missing.isEmpty() ? "SUPPLY SHORT " + view.missing.size() : "SUPPLY OK  JOBS " + view.openTargets);
	}
	@Override public void submit(State state, PoseStack poses, SubmitNodeCollector collector, CameraRenderState camera) {
		if (state.lines.isEmpty()) return;
		poses.pushPose();
		poses.translate(.5, 0, .5);
		float rotation = switch (state.facing) { case EAST -> -(float) Math.PI / 2; case SOUTH -> (float) Math.PI;
			case WEST -> (float) Math.PI / 2; default -> 0; };
		poses.mulPose(new org.joml.Matrix4f().rotationY(rotation));
		poses.translate(-.5 + 1.4 / 16, 24.1 / 16, -.5 + 3.85 / 16);
		poses.scale(1 / PIXELS, -1 / PIXELS, 1 / PIXELS);
		for (int i = 0; i < state.lines.size(); i++) {
			String line = font.plainSubstrByWidth(state.lines.get(i), 119);
			int color = i == 0 ? 0xFFFFBA74 : state.warning && i == state.lines.size() - 1 ? 0xFFFF895B : 0xFFC3DBDC;
			collector.submitText(poses, 0, i * 10, Component.literal(line).getVisualOrderText(), false,
					Font.DisplayMode.POLYGON_OFFSET, LightCoordsUtil.FULL_BRIGHT, color, 0, 0);
		}
		poses.popPose();
	}
}
