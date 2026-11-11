package io.github.jimbozoomer.jugcraft.client.warehouse;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.jimbozoomer.jugcraft.warehouse.DockBlock;
import io.github.jimbozoomer.jugcraft.warehouse.DockBlockEntity;
import io.github.jimbozoomer.jugcraft.warehouse.DroneKind;
import java.util.Locale;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * A charging dock's charger, live: its display (the drone's name; CHARGING and its battery, READY, OUT, or NO DRONE) and
 * its LED bar (the battery, ten segments; running along while it charges).
 */
public class DockRenderer implements BlockEntityRenderer<DockBlockEntity, DockRenderer.State> {
	private final Font font;

	public static class State extends BlockEntityRenderState {
		Direction facing = Direction.NORTH;
		@Nullable Page display, bar;
	}

	public DockRenderer(BlockEntityRendererProvider.Context context) {
		this.font = context.font();
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(DockBlockEntity dock, State state, float partialTick, Vec3 camera,
			ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
		BlockEntityRenderState.extractBase(dock, state, crumbling);
		state.facing = dock.getBlockState().getBlock() instanceof DockBlock ? dock.getBlockState().getValue(DockBlock.FACING) : Direction.NORTH;
		long time = dock.getLevel() == null ? 0 : dock.getLevel().getGameTime();
		Page display = new Page(font), bar = new Page(font);
		display.add(0, 0, 1, "DRONE CHARGER", Page.CYAN);
		if (dock.kind() == null) {
			display.add(0, 13, 1, "NO DRONE", Page.GREY);
		} else {
			String name = String.format(Locale.ROOT, "%s-%02d", dock.kind() == DroneKind.STACKER ? "SD" : "PD", dock.droneId());
			int pct = Math.round(dock.battery() * 100);
			display.add(0, 13, 1, name, Page.WHITE);
			if (!dock.home()) {
				display.right(130, 13, 1, "OUT", Page.CYAN);
			} else if (pct < 100) {
				display.right(130, 13, 1, "CHARGING " + pct + "%", Page.AMBER);
			} else {
				display.right(130, 13, 1, "READY", Page.GREEN);
			}
			// The bar: the battery; while charging, the next segment blinks.
			int n = Math.round(dock.battery() * 10);
			boolean charging = dock.home() && pct < 100;
			StringBuilder on = new StringBuilder(), off = new StringBuilder();
			for (int i = 0; i < 10; i++) {
				boolean lit = i < n || charging && i == n && (time / 10) % 2 == 0;
				(lit ? on : off).append(' ').append('■');
			}
			bar.add(0, 0, 1, on.toString(), dock.battery() < 0.2F ? Page.RED : Page.GREEN);
			bar.add(font.width(on.toString()), 0, 1, off.toString(), 0xFF233229);
		}
		state.display = display;
		state.bar = bar;
	}

	@Override
	public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
		pose.pushPose();
		Page.blockFrame(pose, state.facing);
		// The charger's front (x 9.2..15.4, y 7.2..15.6, z 2.8): its display window, its LED row.
		if (state.display != null) {
			state.display.draw(pose, collector, 9.95F, 14.55F, 2.8F, 4.7F / 130F);
		}
		if (state.bar != null) {
			state.bar.draw(pose, collector, 10.0F, 12.05F, 2.8F, 4.4F / 110F);
		}
		pose.popPose();
	}
}
