package io.github.jimbozoomer.jugcraft.client.warehouse;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.vertex.PoseStack;
import io.github.jimbozoomer.jugcraft.warehouse.GoodsOutBlockEntity;
import io.github.jimbozoomer.jugcraft.warehouse.Order;
import io.github.jimbozoomer.jugcraft.warehouse.PieceBlock;
import io.github.jimbozoomer.jugcraft.warehouse.WarehouseLayout;
import io.github.jimbozoomer.jugcraft.warehouse.WarehousePiece;
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
 * The Goods Out panel's live parts (part W2): the pickup window's tray sliding out to the window while someone has its
 * screen open (and back in), the green beacon over it (lit while an order waits to be collected), the OUT flange's flow
 * lamp (lit while the port sends), and the kiosk's screen (PICKUP: the orders in the line, ready ones first).
 */
public class GoodsOutRenderer implements BlockEntityRenderer<GoodsOutBlockEntity, GoodsOutRenderer.State> {
	private static final float WIDTH = 110;
	private static final int SLIDE = 20;
	private final Font font;

	public static class State extends BlockEntityRenderState {
		Direction facing = Direction.NORTH;
		float tray;
		boolean beacon, flowing, linked;
		@Nullable Page page;
	}

	public GoodsOutRenderer(BlockEntityRendererProvider.Context context) {
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
	public void extractRenderState(GoodsOutBlockEntity panel, State state, float partialTick, Vec3 camera,
			ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
		BlockEntityRenderState.extractBase(panel, state, crumbling);
		state.facing = panel.getBlockState().getBlock() instanceof PieceBlock ? panel.getBlockState().getValue(PieceBlock.FACING) : Direction.NORTH;
		double time = panel.getLevel() == null ? 0 : panel.getLevel().getGameTime() + partialTick;
		float f = (float) Math.max(0, Math.min(1, (time - panel.traySince()) / SLIDE));
		state.tray = panel.trayOut() ? f : 1 - f;
		state.beacon = panel.ready() && ((long) time / 10) % 3 != 0;
		state.flowing = panel.flowing();
		state.linked = panel.linked();
		Page page = new Page(font);
		page.title(WIDTH, "PICKUP", "WINDOW");
		if (!panel.linked()) {
			page.add(0, 18, 1, "NO READY STORAGE", Page.AMBER);
		} else if (panel.kiosk().isEmpty()) {
			page.add(0, 18, 1, "No orders waiting.", Page.GREY);
			page.add(0, 32, 1, "Order here:", Page.GREY);
			page.add(0, 44, 1, "use the kiosk", Page.CYAN);
		} else {
			int y = 16;
			for (GoodsOutBlockEntity.KioskLine line : panel.kiosk()) {
				int colour = line.state() == Order.READY ? Page.GREEN : line.state() == Order.QUEUED ? Page.GREY : Page.AMBER;
				String name = line.item().getHoverName().getString();
				if (name.length() > 12) {
					name = name.substring(0, 11) + ".";
				}
				page.add(0, y, 0.8F, "#" + line.id(), Page.GREY).right(WIDTH, y, 0.8F, Order.STATES[line.state()], colour);
				page.add(0, y + 9, 1, String.format(Locale.ROOT, "%s x%,d", name, line.amount()), Page.WHITE);
				y += 24;
			}
		}
		state.page = page;
	}

	@Override
	public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
		JsonObject g = WarehouseLayout.get().section("goods_out");
		int light = state.lightCoords;
		pose.pushPose();
		Page.pieceFrame(pose, state.facing, WarehousePiece.GOODS_OUT);
		// The tray: in by tray_in when away, out at the window while the screen is open.
		float in = g.get("tray_in").getAsFloat();
		pose.pushPose();
		pose.translate(0, 0, in * (1 - state.tray));
		WhQuads.submitPart(collector, pose, "out_tray", light, WhQuads.Tint.NONE);
		if (state.tray > 0.05F) {
			WhQuads.submitPart(collector, pose, "out_tray_load", light, WhQuads.Tint.NONE);
		}
		pose.popPose();
		if (state.beacon) {
			JsonArray b = g.getAsJsonArray("beacon");
			pose.pushPose();
			pose.translate(b.get(0).getAsFloat() - 8, b.get(1).getAsFloat() - 0.03F, b.get(2).getAsFloat() - 8);
			pose.scale(1.0F, 1.15F, 1.0F);
			WhQuads.submitPart(collector, pose, "lamp_green", light, WhQuads.Tint.NONE);
			pose.popPose();
		}
		if (state.flowing) {
			JsonArray o = g.getAsJsonArray("out_lamp");
			pose.pushPose();
			pose.translate(o.get(0).getAsFloat() - 8, o.get(1).getAsFloat() - 8, o.get(2).getAsFloat());
			WhQuads.submitPart(collector, pose, "flow_lamp", light, WhQuads.Tint.NONE);
			pose.popPose();
		}
		if (state.page != null) {
			JsonObject s = g.getAsJsonObject("screen");
			float x0 = s.get("x0").getAsFloat(), x1 = s.get("x1").getAsFloat(), y1 = s.get("y1").getAsFloat();
			state.page.draw(pose, collector, x0 + 0.3F, y1 - 0.3F, s.get("z").getAsFloat(), (x1 - x0 - 0.6F) / WIDTH);
		}
		pose.popPose();
	}
}
