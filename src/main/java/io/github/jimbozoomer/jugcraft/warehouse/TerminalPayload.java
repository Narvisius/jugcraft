package io.github.jimbozoomer.jugcraft.warehouse;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;

/**
 * The Warehouse Control Terminal's pages (server to a player with its screen open, every second): the stock (each
 * item: how many, cases, pallets, where), the fleet (each drone: what it is doing, where to, its battery), the last
 * pallets put away, the pallet places used, the cases waiting at the intakes, the items stored in all. Without the
 * stock (items empty) it also goes every two seconds to the players near the terminal, for its desk's monitors.
 */
public record TerminalPayload(BlockPos terminal, boolean primary, List<Item> items, List<DroneLine> fleet, List<Logged> log, int placesUsed,
		int placesTotal, int casesWaiting, long itemsIn, boolean truncated, long stored, int types, List<OrderLine> orders, Outbound out)
		implements CustomPacketPayload {
	public static final Type<TerminalPayload> TYPE = new Type<>(Jugcraft.id("warehouse_terminal"));
	private static final StreamCodec<RegistryFriendlyByteBuf, TerminalPayload> BASE = StreamCodec.composite(
			BlockPos.STREAM_CODEC, TerminalPayload::terminal, ByteBufCodecs.BOOL, TerminalPayload::primary,
			Item.STREAM_CODEC.apply(ByteBufCodecs.list(4096)), TerminalPayload::items,
			DroneLine.STREAM_CODEC.apply(ByteBufCodecs.list(256)), TerminalPayload::fleet,
			Logged.STREAM_CODEC.apply(ByteBufCodecs.list(16)), TerminalPayload::log,
			ByteBufCodecs.VAR_INT, TerminalPayload::placesUsed, ByteBufCodecs.VAR_INT, TerminalPayload::placesTotal,
			ByteBufCodecs.VAR_INT, TerminalPayload::casesWaiting, ByteBufCodecs.VAR_LONG, TerminalPayload::itemsIn,
			ByteBufCodecs.BOOL, TerminalPayload::truncated, ByteBufCodecs.VAR_LONG, TerminalPayload::stored,
			ByteBufCodecs.VAR_INT, TerminalPayload::types, OrderLine.STREAM_CODEC.apply(ByteBufCodecs.list(256)), TerminalPayload::orders,
			Outbound.STREAM_CODEC, TerminalPayload::out, TerminalPayload::new);
	public static final StreamCodec<RegistryFriendlyByteBuf, TerminalPayload> CODEC = BASE;

	public record Item(ItemStack item, long count, int cases, int pallets, List<String> places) {
		public static final StreamCodec<RegistryFriendlyByteBuf, Item> STREAM_CODEC = StreamCodec.composite(
				ItemStack.STREAM_CODEC, Item::item, ByteBufCodecs.VAR_LONG, Item::count, ByteBufCodecs.VAR_INT, Item::cases,
				ByteBufCodecs.VAR_INT, Item::pallets, ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list(64)), Item::places, Item::new);
	}

	/** A drone: its name, kind, what it is doing, where to, its battery, the floor it works (1 the lowest; part W3). */
	public record DroneLine(String name, int kind, String job, String where, float battery, int floor, int id, int flags) {
		public static final StreamCodec<RegistryFriendlyByteBuf, DroneLine> STREAM_CODEC = StreamCodec.composite(
				ByteBufCodecs.STRING_UTF8, DroneLine::name, ByteBufCodecs.VAR_INT, DroneLine::kind, ByteBufCodecs.STRING_UTF8, DroneLine::job,
				ByteBufCodecs.STRING_UTF8, DroneLine::where, ByteBufCodecs.FLOAT, DroneLine::battery, ByteBufCodecs.VAR_INT, DroneLine::floor,
				ByteBufCodecs.VAR_INT, DroneLine::id, ByteBufCodecs.VAR_INT, DroneLine::flags, DroneLine::new);

		/** Part W5: paused, out of service, being located (Drone.PAUSED, OUT_OF_SERVICE, LOCATING). */
		public boolean flag(int f) {
			return (flags & f) != 0;
		}
	}

	/**
	 * An order (part W2) as the screens show it: its number, who for, the item, how many and how many have come off
	 * pallets so far, to the WINDOW or PIPES, its state (Order.STATES), where it is now, how far along (0..1), the
	 * ticks it was placed, fetched, at the hatch, ready (0 not yet), the stream it refills (0 none), its owner's UUID.
	 */
	public record OrderLine(int id, String who, ItemStack item, long amount, long filled, int dest, int state, String now, float progress,
			List<Long> times, int stream, String owner, boolean rush) {
		public static final StreamCodec<RegistryFriendlyByteBuf, OrderLine> STREAM_CODEC = StreamCodec.composite(
				ByteBufCodecs.VAR_INT, OrderLine::id, ByteBufCodecs.STRING_UTF8, OrderLine::who, ItemStack.OPTIONAL_STREAM_CODEC, OrderLine::item,
				ByteBufCodecs.VAR_LONG, OrderLine::amount, ByteBufCodecs.VAR_LONG, OrderLine::filled, ByteBufCodecs.VAR_INT, OrderLine::dest,
				ByteBufCodecs.VAR_INT, OrderLine::state, ByteBufCodecs.STRING_UTF8, OrderLine::now, ByteBufCodecs.FLOAT, OrderLine::progress,
				ByteBufCodecs.VAR_LONG.apply(ByteBufCodecs.list(8)), OrderLine::times, ByteBufCodecs.VAR_INT, OrderLine::stream,
				ByteBufCodecs.STRING_UTF8, OrderLine::owner, ByteBufCodecs.BOOL, OrderLine::rush, OrderLine::new);

		public boolean open() {
			return state <= 2;
		}
	}

	/**
	 * Ready Storage and the Output Port: slots used and in all, how many Ready Storages, the hatches' busy share (%),
	 * what they are doing, the port's mode, its buffer's stacks, its streams. Part W4: the liquids stored (mB), how many
	 * Drum Emptying Stations and Drum Filling Stations, the Liquids Out flanges' mode, what the filling stations are
	 * filling drums with now (names, "" none).
	 */
	public record Outbound(int poolUsed, int poolSlots, int hatches, int busy, String hatch, int portMode, int buffer, int streams,
			long liquids, int emptiers, int fillers, int liquidMode, String liquidIn) {
		public static final StreamCodec<RegistryFriendlyByteBuf, Outbound> STREAM_CODEC = StreamCodec.composite(
				ByteBufCodecs.VAR_INT, Outbound::poolUsed, ByteBufCodecs.VAR_INT, Outbound::poolSlots, ByteBufCodecs.VAR_INT, Outbound::hatches,
				ByteBufCodecs.VAR_INT, Outbound::busy, ByteBufCodecs.STRING_UTF8, Outbound::hatch, ByteBufCodecs.VAR_INT, Outbound::portMode,
				ByteBufCodecs.VAR_INT, Outbound::buffer, ByteBufCodecs.VAR_INT, Outbound::streams, ByteBufCodecs.VAR_LONG, Outbound::liquids,
				ByteBufCodecs.VAR_INT, Outbound::emptiers, ByteBufCodecs.VAR_INT, Outbound::fillers, ByteBufCodecs.VAR_INT, Outbound::liquidMode,
				ByteBufCodecs.STRING_UTF8, Outbound::liquidIn, Outbound::new);
		public static final Outbound NONE = new Outbound(0, 0, 0, 0, "NONE", 0, 0, 0, 0, 0, 0, 0, "");
	}

	public record Logged(long time, ItemStack item, int count, String place) {
		public static final StreamCodec<RegistryFriendlyByteBuf, Logged> STREAM_CODEC = StreamCodec.composite(
				ByteBufCodecs.VAR_LONG, Logged::time, ItemStack.STREAM_CODEC, Logged::item, ByteBufCodecs.VAR_INT, Logged::count,
				ByteBufCodecs.STRING_UTF8, Logged::place, Logged::new);
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
