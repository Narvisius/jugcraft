package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.jimbozoomer.jugcraft.drone.DepotDisplayBlockEntity;
import io.github.jimbozoomer.jugcraft.drone.DepotView;
import io.github.jimbozoomer.jugcraft.drone.DroneTerminalBlockEntity;
import io.github.jimbozoomer.jugcraft.tower.MasterConsoleBlock;
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
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/** Three live instruments on one console; consumes the terminal's existing snapshot without another ticker. */
public final class MasterConsoleRenderer implements BlockEntityRenderer<DepotDisplayBlockEntity, MasterConsoleRenderer.State> {
	private static final float PIXELS = 128;
	private final Font font;
	public static final class State extends BlockEntityRenderState {
		Direction facing = Direction.NORTH;
		final List<String> main = new ArrayList<>(), power = new ArrayList<>(), supply = new ArrayList<>();
		boolean warning;
	}
	public MasterConsoleRenderer(BlockEntityRendererProvider.Context context) { this(context.font()); }
	MasterConsoleRenderer(Font font) { this.font = font; }
	@Override public State createRenderState() { return new State(); }
	@Override public int getViewDistance() { return 48; }
	// The three-metre monitor bank extends beyond the anchor block's frustum box.
	@Override public boolean shouldRenderOffScreen() { return true; }
	@Override public void extractRenderState(DepotDisplayBlockEntity console, State state, float partialTick, Vec3 cameraPos,
			ModelFeatureRenderer.CrumblingOverlay crumbling) {
		BlockEntityRenderState.extractBase(console, state, crumbling);
		state.facing = console.getBlockState().getValue(MasterConsoleBlock.FACING);
		DepotView view = null;
		double now = 0;
		if (console.isAnchor() && console.getLevel() != null) {
			now = console.getLevel().getGameTime() + partialTick;
			if (console.terminal() != null && console.getLevel().getBlockEntity(console.terminal()) instanceof DroneTerminalBlockEntity terminal)
				view = terminal.view();
		}
		populate(state, view, now);
	}
	static void populate(State state, @Nullable DepotView view, double now) {
		state.main.clear(); state.power.clear(); state.supply.clear(); state.warning = false;
		state.main.add("TOWER CONTROL"); state.power.add("POWER BUS"); state.supply.add("LOGISTICS");
		if (view == null) {
			state.main.add("AWAITING TERMINAL"); state.main.add("NO LIVE LINK");
			state.power.add("--"); state.supply.add("--"); return;
		}
		int fleet = view.docks.size() + view.flights.size();
		state.main.add("TIER " + view.towerTier + "  " + view.mode.toUpperCase(Locale.ROOT));
		state.main.add("FLEET " + fleet + "/" + view.maxDrones);
		state.main.add("ACTIVE " + view.active() + "  AIR " + view.airborne(now));
		state.main.add("PADS " + view.padCount() + "  FREE " + Math.max(0, view.maxDrones - fleet));
		state.main.add("RIGHT CLICK: CONTROLS");
		state.power.add("BATTERY " + (view.capacity <= 0 ? 0 : view.energy * 100 / view.capacity) + "%");
		state.power.add("DRAW " + view.draw()); state.power.add("JE/t  " + view.loadPercent() + "%");
		state.power.add(view.lowPower ? "LOW POWER" : "BUS ONLINE");
		state.supply.add("JOBS " + view.openTargets); state.supply.add("PICKUPS " + view.pickups.size());
		state.supply.add(view.packager ? "CARGO ONLINE" : "NO PACKAGER");
		state.warning = view.pickups.isEmpty() || !view.packager || !view.missing.isEmpty();
		state.supply.add(!view.missing.isEmpty() ? "SHORT " + view.missing.size() : state.warning ? "CHECK SUPPLY" : "SUPPLY READY");
	}
	@Override public void submit(State state, PoseStack poses, SubmitNodeCollector collector, CameraRenderState camera) {
		poses.pushPose(); poses.translate(.5, 0, .5);
		float rotation = switch (state.facing) { case EAST -> -(float) Math.PI / 2; case SOUTH -> (float) Math.PI;
			case WEST -> (float) Math.PI / 2; default -> 0; };
		poses.mulPose(new org.joml.Matrix4f().rotationY(rotation));
		// These match tools/tower_master_console.py SCREEN_PLANES. North-facing instruments face -z.
		panel(state.main, 17.3, 24.1, 23.39, 144, false, poses, collector);
		panel(state.power, 30.2, 23.7, 23.39, 76, false, poses, collector);
		panel(state.supply, -4.4, 23.7, 23.39, 76, state.warning, poses, collector);
		poses.popPose();
	}
	private void panel(List<String> lines, double right, double top, double z, int width, boolean warning,
			PoseStack poses, SubmitNodeCollector collector) {
		poses.pushPose(); poses.translate(right / 16 - .5, top / 16, z / 16 - .5);
		poses.scale(-1 / PIXELS, -1 / PIXELS, -1 / PIXELS);
		for (int i = 0; i < lines.size(); i++) {
			String text = font.plainSubstrByWidth(lines.get(i), width);
			int color = i == 0 ? 0xFFFFC586 : warning && i == lines.size() - 1 ? 0xFFFF9568 : 0xFFC4E1DF;
			collector.submitText(poses, 0, i * 10, Component.literal(text).getVisualOrderText(), false,
					Font.DisplayMode.POLYGON_OFFSET, LightCoordsUtil.FULL_BRIGHT, color, 0, 0);
		}
		poses.popPose();
	}
}
