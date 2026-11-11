package io.github.jimbozoomer.jugcraft.client.warehouse;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.vertex.PoseStack;
import io.github.jimbozoomer.jugcraft.warehouse.PieceBlock;
import io.github.jimbozoomer.jugcraft.warehouse.SignalBoxBlockEntity;
import io.github.jimbozoomer.jugcraft.warehouse.WarehouseLayout;
import io.github.jimbozoomer.jugcraft.warehouse.WarehousePiece;
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
 * Part W5: the Warehouse Signal Box's LEDs (PWR and LINK green while it has a warehouse, IN amber while it gets
 * redstone, OUT red while it gives it) and its little screen (its rule, what it counts, the signal).
 */
public class SignalBoxRenderer implements BlockEntityRenderer<SignalBoxBlockEntity, SignalBoxRenderer.State> {
	private static final float WIDTH = 60;
	private final Font font;

	public static class State extends BlockEntityRenderState {
		Direction facing = Direction.NORTH;
		boolean linked, in, out, blink;
		@Nullable Page page;
	}

	public SignalBoxRenderer(BlockEntityRendererProvider.Context context) {
		this.font = context.font();
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(SignalBoxBlockEntity box, State state, float partialTick, Vec3 camera, ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
		BlockEntityRenderState.extractBase(box, state, crumbling);
		state.facing = box.getBlockState().getBlock() instanceof PieceBlock ? box.getBlockState().getValue(PieceBlock.FACING) : Direction.NORTH;
		state.linked = box.linked();
		state.in = box.input();
		state.out = box.output() > 0;
		state.blink = box.getLevel() != null && box.getLevel().getGameTime() / 10 % 2 == 0;
		Page page = new Page(font);
		boolean output = box.mode() == SignalBoxBlockEntity.OUTPUT;
		page.add(0, 0, 0.8F, output ? "OUT" : "IN", Page.CYAN);
		page.right(WIDTH, 0, 0.8F, box.linked() ? "WH" : "NO WH", box.linked() ? Page.GREEN : Page.AMBER);
		String rule = box.describe();
		String shown = rule.startsWith("IN  ") ? rule.substring(4) : rule;
		float s = Math.min(0.9F, WIDTH / Math.max(1, font.width(shown)));
		page.add(0, 10, s, shown, Page.WHITE);
		String sig = output ? (box.output() > 0 ? "ON  " + box.output() : "OFF") : (box.input() ? "ACTIVE" : "WAITING");
		page.add(0, 20, 0.85F, sig, output ? (box.output() > 0 ? Page.GREEN : Page.DIM) : (box.input() ? Page.AMBER : Page.DIM));
		state.page = page;
	}

	@Override
	public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
		JsonObject g = WarehouseLayout.get().section("signal_box");
		pose.pushPose();
		Page.pieceFrame(pose, state.facing, WarehousePiece.SIGNAL_BOX);
		JsonArray leds = g.getAsJsonArray("leds");
		boolean[] lit = {state.linked, state.linked && state.blink, state.in, state.out};
		int[] colour = {SwitchboardRenderer.GREEN, SwitchboardRenderer.GREEN, SwitchboardRenderer.AMBER, SwitchboardRenderer.RED};
		for (int k = 0; k < leds.size() && k < 4; k++) {
			if (lit[k]) {
				JsonArray l = leds.get(k).getAsJsonArray();
				WhQuads.lamp(collector, pose, l.get(0).getAsFloat(), l.get(1).getAsFloat(), l.get(2).getAsFloat(), l.get(3).getAsFloat(), colour[k]);
			}
		}
		if (state.page != null) {
			JsonObject s = g.getAsJsonObject("screen");
			float x0 = s.get("x0").getAsFloat(), x1 = s.get("x1").getAsFloat(), y1 = s.get("y1").getAsFloat();
			state.page.draw(pose, collector, x0 + 0.3F, y1 - 0.3F, s.get("z").getAsFloat(), (x1 - x0 - 0.6F) / WIDTH);
		}
		pose.popPose();
	}

	@Override
	public int getViewDistance() {
		return 32;
	}
}
