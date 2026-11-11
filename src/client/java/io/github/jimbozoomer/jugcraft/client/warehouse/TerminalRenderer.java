package io.github.jimbozoomer.jugcraft.client.warehouse;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.jimbozoomer.jugcraft.warehouse.PieceBlock;
import io.github.jimbozoomer.jugcraft.warehouse.TerminalBlockEntity;
import io.github.jimbozoomer.jugcraft.warehouse.TerminalPayload;
import io.github.jimbozoomer.jugcraft.warehouse.WarehouseLayout;
import io.github.jimbozoomer.jugcraft.warehouse.WarehousePiece;
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
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The Control Terminal's three desk monitors, live (the terminal's pages, every two seconds: WarehouseClient.snapshot):
 * FLEET (each drone and what it is doing), STOCK (places used, items, kinds, cases waiting), PUT AWAY (the last pallets
 * stored, with their places).
 */
public class TerminalRenderer implements BlockEntityRenderer<TerminalBlockEntity, TerminalRenderer.State> {
	private static final float WIDTH = 200;
	private final Font font;

	public static class State extends BlockEntityRenderState {
		Direction facing = Direction.NORTH;
		final Page[] pages = new Page[3];
	}

	public TerminalRenderer(BlockEntityRendererProvider.Context context) {
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
	public int getViewDistance() {
		return 32;
	}

	@Override
	public void extractRenderState(TerminalBlockEntity terminal, State state, float partialTick, Vec3 camera,
			ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
		BlockEntityRenderState.extractBase(terminal, state, crumbling);
		state.facing = terminal.getBlockState().getBlock() instanceof PieceBlock ? terminal.getBlockState().getValue(PieceBlock.FACING) : Direction.NORTH;
		TerminalPayload info = WarehouseClient.snapshot(terminal.getBlockPos());
		Page fleet = new Page(font), stock = new Page(font), log = new Page(font);
		fleet.title(WIDTH, "FLEET", info == null ? "" : info.fleet().size() + " DRONES");
		stock.title(WIDTH, "WMS  STOCK", "WH-1");
		log.title(WIDTH, "PUT AWAY", "LAST " + (info == null ? 0 : info.log().size()));
		if (info == null) {
			for (Page p : new Page[] {fleet, stock, log}) {
				p.add(0, 30, 1, "CONNECTING...", Page.GREY);
			}
		} else if (!info.primary()) {
			for (Page p : new Page[] {fleet, stock, log}) {
				p.add(0, 30, 1, "ANOTHER TERMINAL RUNS", Page.AMBER).add(0, 44, 1, "THIS WAREHOUSE", Page.AMBER);
			}
		} else {
			int y = 18;
			for (TerminalPayload.DroneLine d : info.fleet()) {
				if (y > 110) {
					break;
				}
				int colour = d.job().equals("IDLE") || d.job().equals("READY") ? Page.CYAN : d.job().equals("CHARGING") ? Page.AMBER : Page.GREEN;
				fleet.add(0, y, 1, d.name(), Page.WHITE).right(WIDTH, y, 1, d.job() + (d.where().isEmpty() ? "" : " " + d.where()), colour);
				y += 13;
			}
			if (info.fleet().isEmpty()) {
				fleet.add(0, 18, 1, "NO DRONES DOCKED", Page.GREY);
			}
			double used = info.placesTotal() == 0 ? 0 : info.placesUsed() / (double) info.placesTotal();
			stock.add(0, 18, 1, "PALLET PLACES", Page.GREY).right(WIDTH, 18, 1, info.placesUsed() + " / " + info.placesTotal(), Page.WHITE);
			stock.bar(0, 31, 1, 24, used, Page.CYAN);
			stock.add(0, 46, 1, "ITEMS STORED", Page.GREY).right(WIDTH, 46, 1, String.format(Locale.ROOT, "%,d", info.stored()), Page.WHITE);
			stock.add(0, 59, 1, info.out().liquids() > 0 ? "LIQUIDS" : "ITEM KINDS", Page.GREY).right(WIDTH, 59, 1, info.out().liquids() > 0
					? io.github.jimbozoomer.jugcraft.warehouse.Drums.buckets(info.out().liquids()) : String.valueOf(info.types()), Page.WHITE);
			stock.add(0, 72, 1, "INTAKE WAITING", Page.GREY).right(WIDTH, 72, 1, info.casesWaiting() + " CASES", info.casesWaiting() > 0 ? Page.AMBER : Page.WHITE);
			stock.add(0, 85, 1, "STORED IN ALL", Page.GREY).right(WIDTH, 85, 1, String.format(Locale.ROOT, "%,d", info.itemsIn()), Page.WHITE);
			y = 18;
			// Part W2: with orders about, the third monitor shows the order line; else the last pallets put away.
			List<TerminalPayload.OrderLine> live = info.orders().stream().filter(o -> o.state() < 5).toList();
			if (!live.isEmpty()) {
				log = new Page(font);
				log.title(WIDTH, "ORDERS", live.size() + " OPEN");
				for (TerminalPayload.OrderLine o : live) {
					if (y > 110) {
						break;
					}
					String name = WhGui.name(o.item());
					if (name.length() > 13) {
						name = name.substring(0, 12) + ".";
					}
					int colour = o.state() == 3 ? Page.GREEN : o.state() == 0 ? Page.GREY : o.state() == 4 ? Page.CYAN : Page.AMBER;
					boolean liquid = io.github.jimbozoomer.jugcraft.warehouse.Drums.isLiquid(o.item());
					log.add(0, y, 1, String.format(Locale.ROOT, "#%d %s %s", o.id(), name, (liquid ? "" : "x") + WhGui.amount(o.item(), o.amount())), Page.WHITE)
							.right(WIDTH, y, 1, io.github.jimbozoomer.jugcraft.warehouse.Order.STATES[o.state()], colour);
					y += 13;
				}
				y = 999;
			}
			for (TerminalPayload.Logged l : y > 900 ? List.<TerminalPayload.Logged>of() : info.log()) {
				if (y > 110) {
					break;
				}
				String name = WhGui.name(l.item());
				if (name.length() > 16) {
					name = name.substring(0, 15) + ".";
				}
				boolean liquid = io.github.jimbozoomer.jugcraft.warehouse.Drums.isLiquid(l.item());
				log.add(0, y, 1, name + " " + (liquid ? "" : "x") + WhGui.amount(l.item(), l.count()), Page.WHITE).right(WIDTH, y, 1, l.place(), Page.CYAN);
				y += 13;
			}
			if (info.log().isEmpty() && y < 900) {
				log.add(0, 18, 1, "NOTHING STORED YET", Page.GREY);
			}
		}
		state.pages[0] = fleet;
		state.pages[1] = stock;
		state.pages[2] = log;
	}

	@Override
	public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
		pose.pushPose();
		Page.pieceFrame(pose, state.facing, WarehousePiece.TERMINAL);
		int i = 0;
		for (JsonElement e : WarehouseLayout.get().array("terminal")) {
			JsonObject m = e.getAsJsonObject();
			float x0 = m.get("x0").getAsFloat(), x1 = m.get("x1").getAsFloat(), y1 = m.get("y1").getAsFloat(), z = m.get("z").getAsFloat();
			float turn = m.get("turn").getAsFloat();
			var pivot = m.getAsJsonArray("pivot");
			pose.pushPose();
			if (turn != 0) {
				pose.translate(pivot.get(0).getAsFloat(), pivot.get(1).getAsFloat(), pivot.get(2).getAsFloat());
				pose.rotateDegrees(Axis.YP, turn);
				pose.translate(-pivot.get(0).getAsFloat(), -pivot.get(1).getAsFloat(), -pivot.get(2).getAsFloat());
			}
			int page = switch (m.get("page").getAsString()) {
				case "fleet" -> 0;
				case "stock" -> 1;
				default -> 2;
			};
			if (state.pages[page] != null) {
				state.pages[page].draw(pose, collector, x0 + 0.5F, y1 - 0.5F, z, (x1 - x0 - 1.0F) / WIDTH);
			}
			pose.popPose();
			i++;
		}
		pose.popPose();
	}
}
