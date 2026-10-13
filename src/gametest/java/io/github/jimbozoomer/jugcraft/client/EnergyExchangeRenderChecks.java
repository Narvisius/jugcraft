package io.github.jimbozoomer.jugcraft.client;

import io.github.jimbozoomer.jugcraft.drone.DepotView;
import io.github.jimbozoomer.jugcraft.drone.FlightPath;
import io.github.jimbozoomer.jugcraft.tower.ExchangeStationBlockEntity;
import java.util.UUID;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;

/** Tests the actual renderer against loaded multiplayer snapshots, including identity loss and chunk unload. */
public final class EnergyExchangeRenderChecks {
	private EnergyExchangeRenderChecks() { }
	public static boolean ready(Minecraft client, BlockPos first, BlockPos second) {
		return client.level.getBlockEntity(first) instanceof ExchangeStationBlockEntity a && EnergyExchangeRenderer.linkedView(a) != null
				&& client.level.getBlockEntity(second) instanceof ExchangeStationBlockEntity b && EnergyExchangeRenderer.linkedView(b) != null;
	}
	public static double charge(Minecraft client, BlockPos at) {
		if (!(client.level.getBlockEntity(at) instanceof ExchangeStationBlockEntity station)) return -1;
		var view = EnergyExchangeRenderer.linkedView(station);
		return view == null || view.capacity <= 0 ? -1 : (double) view.energy * 100 / view.capacity;
	}
	public static void actualSnapshotsAndIdentity(ClientGameTestContext context, BlockPos first, BlockPos second) {
		context.runOnClient(client -> {
			var renderer = new EnergyExchangeRenderer(client.font);
			var a = (ExchangeStationBlockEntity) client.level.getBlockEntity(first);
			var b = (ExchangeStationBlockEntity) client.level.getBlockEntity(second);
			var av = EnergyExchangeRenderer.linkedView(a); var bv = EnergyExchangeRenderer.linkedView(b);
			if (av == null || bv == null || av == bv || av.depotId.equals(bv.depotId) || av.energy == bv.energy)
				throw new AssertionError("Two towers do not have independent exact power snapshots");
			for (var station : new ExchangeStationBlockEntity[] {a, b}) {
				var state = renderer.createRenderState(); renderer.extractRenderState(station, state, 0, client.player.position(), null);
				var view = EnergyExchangeRenderer.linkedView(station);
				if (!state.visible || !state.connected || state.energy != view.energy || state.capacity != view.capacity
						|| state.demand != view.draw() || state.load != view.loadPercent()
						|| !state.stored.equals(String.format(java.util.Locale.ROOT, "%,d JE", view.energy)))
					throw new AssertionError("Power instruments do not show their original terminal's actual stored energy and demand");
			}
			var reused = renderer.createRenderState();
			UUID id = av.depotId; BlockPos core = av.towerPos;
			try {
				av.depotId = bv.depotId;
				renderer.extractRenderState(a, reused, 0, client.player.position(), null);
				assertOffline(reused, "same-position replacement depot");
				av.depotId = id; av.towerPos = bv.towerPos;
				renderer.extractRenderState(a, reused, 0, client.player.position(), null);
				assertOffline(reused, "another tower core");
				av.towerPos = core; av.depotId = null;
				renderer.extractRenderState(a, reused, 0, client.player.position(), null);
				assertOffline(reused, "missing snapshot depot identity");
			} finally { av.depotId = id; av.towerPos = core; }
			renderer.extractRenderState(a, reused, 0, client.player.position(), null);
			if (!reused.connected) throw new AssertionError("Restoring the exact snapshot did not restore its display");
		});
	}
	public static void unloadedTerminalIsOffline(ClientGameTestContext context, BlockPos anchor) {
		context.runOnClient(client -> {
			var station = (ExchangeStationBlockEntity) client.level.getBlockEntity(anchor);
			BlockPos terminal = station.displayTerminalPos();
			if (ChunkPos.containing(terminal).equals(ChunkPos.containing(anchor))) throw new AssertionError("Unload regression must keep the station's own chunk loaded");
			var renderer = new EnergyExchangeRenderer(client.font); var reused = renderer.createRenderState();
			renderer.extractRenderState(station, reused, 0, client.player.position(), null);
			if (!reused.connected) throw new AssertionError("Unload regression begins without a live station link");
			client.level.getChunkSource().drop(ChunkPos.containing(terminal));
			if (client.level.isLoaded(terminal)) throw new AssertionError("The actual terminal chunk was not unloaded");
			renderer.extractRenderState(station, reused, 0, client.player.position(), null);
			assertOffline(reused, "unloaded terminal chunk");
		});
	}
	private static void assertOffline(EnergyExchangeRenderer.State state, String reason) {
		if (state.connected || state.charged || state.lowPower || state.energy != 0 || state.capacity != 0 || state.demand != 0
				|| !state.stored.equals("-- JE") || !state.draw.equals("-- JE/t")) throw new AssertionError("Stale power readings survived " + reason);
	}
	public static void needleAndSnapshotChannels(ClientGameTestContext context) {
		context.runOnClient(client -> {
			var state = new EnergyExchangeRenderer(client.font).createRenderState(); var view = new DepotView();
			view.capacity = 200000; view.energy = 100000; view.standbyDraw = 256; view.workingDraw = 6144;
			EnergyExchangeRenderer.populate(state, view);
			if (state.charge != 50 || state.demand != 256 || state.load != 6 || state.charged || state.lowPower)
				throw new AssertionError("Idle charge and demand are not independent truthful channels");
			var path = new FlightPath(new double[][] {{0, 1, 0}, {2, 1, 2}, {0, 1, 0}}, new double[] {3, 3}, 4);
			view.flights.add(new DepotView.FlightView(0, 1, path, 0, 100, 1000)); view.lowPower = true;
			EnergyExchangeRenderer.populate(state, view);
			if (state.demand != 6144 || state.load != 150 || !state.lowPower || !state.loading.equals("LOAD 150%"))
				throw new AssertionError("Active demand/overload must remain exact even when the analog meter reaches its stop");
			if (EnergyExchangeRenderer.needleAngle(150) != EnergyExchangeRenderer.needleAngle(100)
					|| Math.abs(EnergyExchangeRenderer.needleAngle(50) - Math.PI / 2) > .000001
					|| EnergyExchangeRenderer.meterAngle(0) != Math.PI / 2 || EnergyExchangeRenderer.meterAngle(100) != 0)
				throw new AssertionError("Dial/relay needle stops or half-scale orientation disagree with the authored front-view scale");
			view.energy = view.capacity; view.lowPower = false; EnergyExchangeRenderer.populate(state, view);
			if (!state.charged || state.charge != 100 || state.lowPower) throw new AssertionError("Charged lamp does not follow the real buffer");
			EnergyExchangeRenderer.populate(state, null); assertOffline(state, "link loss after a charged snapshot");
		});
	}
}
