package io.github.jimbozoomer.jugcraft.client.warehouse;

import com.google.gson.JsonObject;
import com.mojang.blaze3d.vertex.PoseStack;
import io.github.jimbozoomer.jugcraft.warehouse.Drone;
import io.github.jimbozoomer.jugcraft.warehouse.DroneKind;
import io.github.jimbozoomer.jugcraft.warehouse.Leg;
import io.github.jimbozoomer.jugcraft.warehouse.MapPayload;
import io.github.jimbozoomer.jugcraft.warehouse.PieceBlock;
import io.github.jimbozoomer.jugcraft.warehouse.StatusBoardBlockEntity;
import io.github.jimbozoomer.jugcraft.warehouse.TerminalBlockEntity;
import io.github.jimbozoomer.jugcraft.warehouse.WarehouseLayout;
import io.github.jimbozoomer.jugcraft.warehouse.WarehousePiece;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Part W5: the Warehouse Status Board's screen (the mockup w5_office): WAREHOUSE STATUS, the floor shown and the time;
 * the live floor map (the lanes, walkways and crossings, the racks coloured by how full their places are, the pieces,
 * the drones where they are now) and the day's numbers (places, orders, drones, power, counts), what housekeeping is
 * doing. The map comes from its terminal (MapPayload), the drones from the terminal's fleet, the numbers from the board.
 */
public class StatusBoardRenderer implements BlockEntityRenderer<StatusBoardBlockEntity, StatusBoardRenderer.State> {
	/** The page's size in font pixels (the screen is 45.6 x 27.6 px). */
	private static final float PW = 456, PH = 276, MAP_W = 320, MAP_TOP = 22, MAP_H = 246;
	private static final int C_LANE = 0xFF1C2632, C_WALK = 0xFF1E4232, C_CROSS = 0xFF4A3C1C, C_DOCK = 0xFF263848, C_PIECE = 0xFF24425E,
			C_FULL = 0xFF2878C8, C_PART = 0xFFC89632, C_EMPTY = 0xFF26323E, C_SOME = 0xFF1E5A8C;
	private final Font font;

	public static class State extends BlockEntityRenderState {
		Direction facing = Direction.NORTH;
		@Nullable Page page;
		float[] rects = new float[0];
		int[] colours = new int[0];
		int n;
	}

	public StatusBoardRenderer(BlockEntityRendererProvider.Context context) {
		this.font = context.font();
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(StatusBoardBlockEntity board, State state, float partialTick, Vec3 camera,
			ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
		BlockEntityRenderState.extractBase(board, state, crumbling);
		state.facing = board.getBlockState().getBlock() instanceof PieceBlock ? board.getBlockState().getValue(PieceBlock.FACING) : Direction.NORTH;
		long now = board.getLevel() == null ? 0 : board.getLevel().getGameTime();
		BlockPos tp = board.terminalPos();
		MapPayload map = WarehouseClient.map(tp);
		Page page = new Page(font);
		state.page = page;
		state.n = 0;
		page.add(4, 4, 1.2F, "WAREHOUSE STATUS", Page.WHITE);
		long t = (now + 6000) % 24000;
		page.right(PW - 4, 5, 1, String.format(Locale.ROOT, "%02d:%02d", t / 1000, t % 1000 * 60 / 1000), Page.CYAN);
		if (tp == null) {
			page.add(PW / 2 - font.width("NO TERMINAL") * 1.5F / 2, PH / 2 - 6, 1.5F, "NO TERMINAL", Page.DIM);
			return;
		}
		int[] st = board.stats();
		int floors = Math.max(1, map == null ? st[8] : map.floors().size());
		int floor = board.floor() >= 0 ? Math.min(board.floor(), floors - 1) : (int) (now / 200 % floors);
		page.add(150, 5, 0.9F, "FLOOR " + (floor + 1) + " OF " + floors, Page.GREY);
		// The numbers, down the right.
		float rx = MAP_W + 14, rw = PW - 4;
		String[][] rows = {{"PLACES", st[0] + " / " + st[1]}, {"ORDERS", st[2] + " open"}, {"READY", String.valueOf(st[3])},
				{"DRONES", st[4] + " work"}, {"", st[5] + " charge"}, {"POWER", String.format(Locale.ROOT, "%,d JE/t", board.draw())},
				{"STORED", st[6] + " %"}, {"COUNTED", st[7] + " % today"}};
		int[] colours = {Page.CYAN, Page.WHITE, st[3] > 0 ? Page.GREEN : Page.WHITE, Page.GREEN, Page.AMBER, Page.WHITE, st[6] < 10 ? Page.AMBER : Page.GREEN,
				Page.CYAN};
		for (int k = 0; k < rows.length; k++) {
			float y = 26 + k * 25;
			if (!rows[k][0].isEmpty()) {
				page.add(rx, y, 0.7F, rows[k][0], Page.GREY);
			}
			page.right(rw, y + 9, 1, rows[k][1], colours[k]);
		}
		if (!board.house().isEmpty()) {
			String h = board.house();
			float scale = Math.min(0.75F, (rw - rx) / Math.max(1, font.width(h)));
			page.add(rx, PH - 14, scale, h, Page.GREEN);
		}
		if (map == null || map.floors().isEmpty()) {
			page.add(MAP_W / 2 - 40, PH / 2, 1, "CONNECTING...", Page.DIM);
			return;
		}
		MapPayload.Floor f = map.floors().get(Math.min(floor, map.floors().size() - 1));
		// The map: cells in font pixels, the y down (the page's), as squares fitted to the map's area.
		float cell = Math.min(MAP_W / Math.max(1, f.w()), MAP_H / Math.max(1, f.d()));
		float ox = 4 + (MAP_W - f.w() * cell) / 2, oy = MAP_TOP + (MAP_H - f.d() * cell) / 2;
		Map<Long, MapPayload.Column> cols = new HashMap<>();
		for (MapPayload.Column c : f.racks()) {
			cols.put(BlockPos.asLong(c.x(), 0, c.z()), c);
		}
		int max = f.w() * f.d() + 64;
		float[] r = new float[max * 4];
		int[] c = new int[max];
		int n = 0;
		float gap = cell > 3 ? cell * 0.08F : 0;
		for (int z = 0; z < f.d(); z++) {
			for (int x = 0; x < f.w(); x++) {
				byte code = f.cells()[z * f.w() + x];
				if (code == MapPayload.NONE) {
					continue;
				}
				int colour = switch (code) {
					case MapPayload.WALKWAY -> C_WALK;
					case MapPayload.CROSSING -> C_CROSS;
					case MapPayload.DOCK -> C_DOCK;
					case MapPayload.PIECE -> C_PIECE;
					case MapPayload.RACK -> rack(cols.get(BlockPos.asLong(f.x0() + x, 0, f.z0() + z)));
					default -> C_LANE;
				};
				r[n * 4] = ox + x * cell;
				r[n * 4 + 1] = oy + z * cell;
				r[n * 4 + 2] = ox + (x + 1) * cell - gap;
				r[n * 4 + 3] = oy + (z + 1) * cell - gap;
				c[n++] = colour;
			}
		}
		for (MapPayload.Piece p : f.pieces()) {
			float px = ox + (p.x0() - f.x0() + p.x1() - f.x0() + 1) * cell / 2, py = oy + (p.z0() - f.z0() + p.z1() - f.z0() + 1) * cell / 2;
			float s = Math.min(0.6F, Math.max(0.3F, cell / 14));
			page.add(px - font.width(p.name()) * s / 2, py - 4 * s, s, p.name(), Page.WHITE);
		}
		// The drones where they are now (the terminal's fleet as the client has it).
		if (board.getLevel() != null && board.getLevel().getBlockEntity(tp) instanceof TerminalBlockEntity te) {
			for (Drone.View v : te.clientFleet()) {
				Leg leg = v.leg();
				if (leg.y() != f.y() || n >= max) {
					continue;
				}
				Leg.Key k = leg.at(now + partialTick);
				float s = Math.max(3, cell * 0.7F), dx = ox + (k.x() - f.x0()) * cell, dy = oy + (k.z() - f.z0()) * cell;
				boolean pallet = v.droneKind() == DroneKind.PALLET;
				int colour = (v.flags() & Drone.OUT_OF_SERVICE) != 0 ? Page.RED : (v.flags() & Drone.PAUSED) != 0 ? Page.GREY : pallet ? Page.AMBER : Page.GREEN;
				if (now < v.locateUntil() && now / 5 % 2 == 0) {
					colour = 0xFF3C8CFF;
				}
				r[n * 4] = dx - s / 2;
				r[n * 4 + 1] = dy - s / 2;
				r[n * 4 + 2] = dx + s / 2;
				r[n * 4 + 3] = dy + s / 2;
				c[n++] = colour;
				String name = String.format(Locale.ROOT, "%s-%02d", v.droneKind().prefix, v.id());
				page.add(dx + s / 2 + 1, dy - s / 2 - 1, 0.45F, name, Page.WHITE);
			}
		}
		state.rects = r;
		state.colours = c;
		state.n = n;
	}

	private static int rack(MapPayload.@Nullable Column c) {
		if (c == null || c.used() == 0) {
			return C_EMPTY;
		}
		return c.parts() > 0 ? C_PART : c.used() >= c.levels() ? C_FULL : C_SOME;
	}

	@Override
	public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
		JsonObject s = WarehouseLayout.get().section("status_board").getAsJsonObject("screen");
		float x0 = s.get("x0").getAsFloat(), x1 = s.get("x1").getAsFloat(), y1 = s.get("y1").getAsFloat(), z = s.get("z").getAsFloat();
		float scale = (x1 - x0) / PW;
		pose.pushPose();
		Page.pieceFrame(pose, state.facing, WarehousePiece.STATUS_BOARD);
		if (state.n > 0) {
			// The map's cells, from the page's font pixels (y down from the screen's top) to the piece's.
			float[] px = new float[state.n * 4];
			for (int k = 0; k < state.n; k++) {
				px[k * 4] = x0 + state.rects[k * 4] * scale;
				px[k * 4 + 1] = y1 - state.rects[k * 4 + 3] * scale;
				px[k * 4 + 2] = x0 + state.rects[k * 4 + 2] * scale;
				px[k * 4 + 3] = y1 - state.rects[k * 4 + 1] * scale;
			}
			WhQuads.rects(collector, pose, px, state.colours, state.n, z + 0.006F);
		}
		if (state.page != null) {
			state.page.draw(pose, collector, x0, y1, z, scale);
		}
		pose.popPose();
	}

	@Override
	public int getViewDistance() {
		return 48;
	}

	@Override
	public boolean shouldRenderOffScreen() {
		return true;
	}
}
