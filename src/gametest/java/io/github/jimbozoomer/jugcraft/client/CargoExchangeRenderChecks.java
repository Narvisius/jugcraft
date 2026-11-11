package io.github.jimbozoomer.jugcraft.client;

import io.github.jimbozoomer.jugcraft.drone.DepotView;
import io.github.jimbozoomer.jugcraft.drone.CargoPackagerBlockEntity;
import io.github.jimbozoomer.jugcraft.tower.ExchangeStationBlockEntity;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;

/** Checks the operator pod and dispatcher against real server stock, without reading client inventory. */
public final class CargoExchangeRenderChecks {
	private CargoExchangeRenderChecks() { }
	public static int items(Minecraft client, BlockPos anchor) {
		if (!(client.level.getBlockEntity(anchor) instanceof ExchangeStationBlockEntity station)) return -1;
		var view = EnergyExchangeRenderer.linkedView(station);
		return view == null || view.cargo == null ? -1 : view.cargo.items();
	}
	public static boolean ready(Minecraft client, BlockPos first, BlockPos second) {
		return items(client, first) >= 0 && items(client, second) >= 0;
	}
	public static void independentSnapshotsAndDispatcher(ClientGameTestContext context, BlockPos first, BlockPos second, BlockPos energy) {
		context.runOnClient(client -> {
			var renderer = new CargoExchangeRenderer(client.font); var dispatcher = new ExchangeStationRenderer(client.font);
			var a = (ExchangeStationBlockEntity) client.level.getBlockEntity(first);
			var b = (ExchangeStationBlockEntity) client.level.getBlockEntity(second);
			var av = EnergyExchangeRenderer.linkedView(a); var bv = EnergyExchangeRenderer.linkedView(b);
			if (av == null || bv == null || av == bv || av.depotId.equals(bv.depotId) || av.cargo.items() == bv.cargo.items())
				throw new AssertionError("Cargo displays are not independent exact-tower stock snapshots");
			for (var station : new ExchangeStationBlockEntity[] {a, b}) {
				var state = renderer.createRenderState(); renderer.extractRenderState(station, state, 0, client.player.position(), null);
				var view = EnergyExchangeRenderer.linkedView(station);
				if (!state.visible || !state.connected || !state.available || state.items != view.cargo.items()
						|| state.occupied != view.cargo.occupiedSlots() || state.slots != 27 || !state.status.equals("IDLE"))
					throw new AssertionError("Cargo pod does not show the linked server inventory's real aggregate counts");
				if (state.stock.length() > 18 || state.occupancy.length() > 18 || state.status.length() > 18)
					throw new AssertionError("Cargo text exceeds the authored 18-character display width");
			}
			var state = dispatcher.createRenderState();
			var power = (ExchangeStationBlockEntity) client.level.getBlockEntity(energy);
			dispatcher.extractRenderState(power, state, 0, client.player.position(), null);
			if (state.kind != 1 || !state.energy.visible || !state.energy.connected)
				throw new AssertionError("Shared station registration lost the existing energy instruments");
			dispatcher.extractRenderState(a, state, 0, client.player.position(), null);
			if (state.kind != 2 || !state.cargo.visible || state.cargo.items != av.cargo.items())
				throw new AssertionError("Shared station registration dispatches cargo as a power station");
			var reused = renderer.createRenderState(); var id = av.depotId;
			try {
				av.depotId = bv.depotId; renderer.extractRenderState(a, reused, 0, client.player.position(), null);
				assertOffline(reused, "replacement/foreign depot identity");
			} finally { av.depotId = id; }
		});
	}
	public static void stateSemantics(ClientGameTestContext context) {
		context.runOnClient(client -> {
			var state = new CargoExchangeRenderer(client.font).createRenderState(); var view = new DepotView();
			CargoExchangeRenderer.populate(state, view, true);
			if (state.items != -1 || state.available || !state.status.equals("AWAIT SNAPSHOT"))
				throw new AssertionError("Older snapshots are being shown as an empty cargo inventory");
			view.cargo = new DepotView.CargoView(BlockPos.ZERO, 0, 0, 27, false);
			CargoExchangeRenderer.populate(state, view, true);
			if (!state.available || state.items != 0 || !state.status.equals("IDLE")) throw new AssertionError("Real empty idle inventory is not distinguished from unknown data");
			view.openTargets = 2; CargoExchangeRenderer.populate(state, view, true);
			if (!state.requested || !state.status.equals("SUPPLY READY")) throw new AssertionError("Known request state is not shown");
			view.missing.put("minecraft:stone_bricks", 3); CargoExchangeRenderer.populate(state, view, true);
			if (!state.shortage || !state.status.equals("SHORT 1 TYPES")) throw new AssertionError("Missing types are not the terminal's genuine shortage count");
			view.cargo = new DepotView.CargoView(BlockPos.ZERO, 0, 0, 27, true); CargoExchangeRenderer.populate(state, view, true);
			if (!state.unlimited || state.shortage || !state.status.equals("TEST SUPPLY")) throw new AssertionError("Unlimited test supply is misrepresented as physical stock or shortage");
			CargoExchangeRenderer.populate(state, view, false);
			if (state.available || state.items != -1 || !state.status.equals("STORE UNAVAILABLE")) throw new AssertionError("Unloaded store retains stale inventory totals");
			CargoExchangeRenderer.populate(state, null, false); assertOffline(state, "lost terminal");
		});
	}
	public static void unloadStoreThenTerminal(ClientGameTestContext context, BlockPos anchor) {
		context.runOnClient(client -> {
			var station = (ExchangeStationBlockEntity) client.level.getBlockEntity(anchor);
			var view = EnergyExchangeRenderer.linkedView(station); BlockPos store = view.cargo.packagerPos();
			BlockPos terminal = station.displayTerminalPos();
			ChunkPos stationChunk = ChunkPos.containing(anchor), storeChunk = ChunkPos.containing(store), terminalChunk = ChunkPos.containing(terminal);
			if (storeChunk.equals(stationChunk) || storeChunk.equals(terminalChunk) || terminalChunk.equals(stationChunk))
				throw new AssertionError("Actual cargo unload regression needs distinct station/store/terminal chunks");
			if (!(client.level.getBlockEntity(store) instanceof CargoPackagerBlockEntity)) throw new AssertionError("Exact store is not loaded before the unload regression");
			var renderer = new CargoExchangeRenderer(client.font); var state = renderer.createRenderState();
			renderer.extractRenderState(station, state, 0, client.player.position(), null);
			if (!state.available) throw new AssertionError("Unload regression begins without a live inventory snapshot");
			client.level.getChunkSource().drop(storeChunk);
			renderer.extractRenderState(station, state, 0, client.player.position(), null);
			if (!state.connected || state.available || state.items != -1 || !state.status.equals("STORE UNAVAILABLE"))
				throw new AssertionError("Store-only unload is not distinguished from a disconnected tower, or retains stale counts");
			client.level.getChunkSource().drop(terminalChunk);
			renderer.extractRenderState(station, state, 0, client.player.position(), null); assertOffline(state, "actual terminal chunk unload");
		});
	}
	private static void assertOffline(CargoExchangeRenderer.State state, String reason) {
		if (state.connected || state.available || state.requested || state.items != -1 || state.occupied != -1 || state.slots != -1
				|| !state.stock.equals("STOCK --") || !state.status.equals("NO TOWER LINK")) throw new AssertionError("Stale cargo state survived " + reason);
	}
}
