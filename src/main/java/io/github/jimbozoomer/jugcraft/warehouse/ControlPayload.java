package io.github.jimbozoomer.jugcraft.warehouse;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Part W5: the terminal's CONTROL page (server to a player with the terminal's or a switchboard's screen open, every
 * second; TerminalBlockEntity.controlView): access (owner, PERSONAL / PARTY, what this player may change), the job
 * priorities, housekeeping (consolidation, cycle counts, what it is doing, the last adjustments), power (what comes
 * in, what is drawn, what is stored, each circuit on / shed and what it draws), the fleet paused, the Signal Boxes'
 * inputs that are on.
 */
public record ControlPayload(BlockPos terminal, String owner, boolean party, boolean mayChange, boolean mayAccess, List<Integer> priorities,
		int consolidate, int count, int counted, int places, List<String> adjustments, String house, String next, boolean unlimited, int boards,
		long supply, long draw, long stored, long capacity, long peak, int on, int shed, List<Long> loads, boolean low, boolean mainOff,
		boolean paused, List<String> signals) implements CustomPacketPayload {
	public static final Type<ControlPayload> TYPE = new Type<>(Jugcraft.id("warehouse_control"));
	public static final StreamCodec<RegistryFriendlyByteBuf, ControlPayload> CODEC = StreamCodec.of(ControlPayload::write, ControlPayload::read);
	/** The job groups the priorities order. */
	public static final int ORDERS = 0, PUTAWAY = 1, LIQUIDS = 2, HOUSEKEEPING = 3;
	public static final String[] GROUPS = {"ORDERS OUT", "PUT AWAY", "LIQUIDS", "HOUSEKEEPING"};
	public static final String[] GROUP_WHAT = {"fetch pallets for orders (items)", "finished pallets from the stations to the racks",
			"drum pallets to and from the emptying and filling stations", "consolidation and cycle counts (only when idle)"};
	/** Housekeeping's modes. */
	public static final int OFF = 0, WHEN_IDLE = 1;
	public static final int COUNT_OFF = 0, COUNT_DAILY = 1, COUNT_NONSTOP = 2;

	private static void write(RegistryFriendlyByteBuf buf, ControlPayload p) {
		buf.writeBlockPos(p.terminal);
		buf.writeUtf(p.owner);
		buf.writeBoolean(p.party);
		buf.writeBoolean(p.mayChange);
		buf.writeBoolean(p.mayAccess);
		int np = Math.min(8, p.priorities.size());
		buf.writeVarInt(np);
		for (int i = 0; i < np; i++) {
			buf.writeVarInt(p.priorities.get(i));
		}
		buf.writeVarInt(p.consolidate);
		buf.writeVarInt(p.count);
		buf.writeVarInt(p.counted);
		buf.writeVarInt(p.places);
		writeStrings(buf, p.adjustments);
		buf.writeUtf(p.house);
		buf.writeUtf(p.next);
		buf.writeBoolean(p.unlimited);
		buf.writeVarInt(p.boards);
		buf.writeVarLong(p.supply);
		buf.writeVarLong(p.draw);
		buf.writeVarLong(p.stored);
		buf.writeVarLong(p.capacity);
		buf.writeVarLong(p.peak);
		buf.writeVarInt(p.on);
		buf.writeVarInt(p.shed);
		int nl = Math.min(16, p.loads.size());
		buf.writeVarInt(nl);
		for (int i = 0; i < nl; i++) {
			buf.writeVarLong(p.loads.get(i));
		}
		buf.writeBoolean(p.low);
		buf.writeBoolean(p.mainOff);
		buf.writeBoolean(p.paused);
		writeStrings(buf, p.signals);
	}

	private static ControlPayload read(RegistryFriendlyByteBuf buf) {
		BlockPos terminal = buf.readBlockPos();
		String owner = buf.readUtf();
		boolean party = buf.readBoolean(), mayChange = buf.readBoolean(), mayAccess = buf.readBoolean();
		int n = Math.min(8, buf.readVarInt());
		List<Integer> priorities = new ArrayList<>();
		for (int i = 0; i < n; i++) {
			priorities.add(buf.readVarInt());
		}
		int consolidate = buf.readVarInt(), count = buf.readVarInt(), counted = buf.readVarInt(), places = buf.readVarInt();
		List<String> adjustments = readStrings(buf);
		String house = buf.readUtf(), next = buf.readUtf();
		boolean unlimited = buf.readBoolean();
		int boards = buf.readVarInt();
		long supply = buf.readVarLong(), draw = buf.readVarLong(), stored = buf.readVarLong(), capacity = buf.readVarLong(), peak = buf.readVarLong();
		int on = buf.readVarInt(), shed = buf.readVarInt();
		int m = Math.min(16, buf.readVarInt());
		List<Long> loads = new ArrayList<>();
		for (int i = 0; i < m; i++) {
			loads.add(buf.readVarLong());
		}
		boolean low = buf.readBoolean(), mainOff = buf.readBoolean(), paused = buf.readBoolean();
		List<String> signals = readStrings(buf);
		return new ControlPayload(terminal, owner, party, mayChange, mayAccess, priorities, consolidate, count, counted, places, adjustments, house,
				next, unlimited, boards, supply, draw, stored, capacity, peak, on, shed, loads, low, mainOff, paused, signals);
	}

	private static void writeStrings(RegistryFriendlyByteBuf buf, List<String> list) {
		int n = Math.min(32, list.size());
		buf.writeVarInt(n);
		for (int i = 0; i < n; i++) {
			buf.writeUtf(list.get(i));
		}
	}

	private static List<String> readStrings(RegistryFriendlyByteBuf buf) {
		int n = Math.min(32, buf.readVarInt());
		List<String> out = new ArrayList<>();
		for (int i = 0; i < n; i++) {
			out.add(buf.readUtf());
		}
		return out;
	}

	public boolean circuitOn(int k) {
		return (on >> k & 1) != 0;
	}

	public boolean circuitShed(int k) {
		return (shed >> k & 1) != 0;
	}

	public long load(int k) {
		return k < loads.size() ? loads.get(k) : 0;
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
