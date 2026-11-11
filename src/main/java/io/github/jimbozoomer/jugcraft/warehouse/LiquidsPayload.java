package io.github.jimbozoomer.jugcraft.warehouse;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.util.List;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;

/**
 * Part W4: the LIQUIDS IN and LIQUIDS OUT screens' status (server to the player using one, twice a second). In: each
 * inlet (piped, its liquid, its flow, waiting its turn), the filling station (what it is doing, its buffer tank, its
 * drums and pallets). Out: the player's liquid orders, the holding tank and what of it is the player's, the flange's
 * mode and flow, the liquid streams, and the liquids stored (to start a stream with).
 */
public record LiquidsPayload(boolean in, boolean linked, List<Inlet> inlets, Station station, List<TerminalPayload.OrderLine> orders,
		FluidVariant tankLiquid, long tank, long tankCap, long mine, int mode, long flow, List<LiquidStream> streams, List<Stocked> stocked)
		implements CustomPacketPayload {
	public static final Type<LiquidsPayload> TYPE = new Type<>(Jugcraft.id("warehouse_liquids"));
	public static final StreamCodec<RegistryFriendlyByteBuf, LiquidsPayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.BOOL, LiquidsPayload::in, ByteBufCodecs.BOOL, LiquidsPayload::linked,
			Inlet.STREAM_CODEC.apply(ByteBufCodecs.list(8)), LiquidsPayload::inlets, Station.STREAM_CODEC, LiquidsPayload::station,
			TerminalPayload.OrderLine.STREAM_CODEC.apply(ByteBufCodecs.list(32)), LiquidsPayload::orders,
			FluidVariant.PACKET_CODEC, LiquidsPayload::tankLiquid, ByteBufCodecs.VAR_LONG, LiquidsPayload::tank,
			ByteBufCodecs.VAR_LONG, LiquidsPayload::tankCap, ByteBufCodecs.VAR_LONG, LiquidsPayload::mine, ByteBufCodecs.VAR_INT, LiquidsPayload::mode,
			ByteBufCodecs.VAR_LONG, LiquidsPayload::flow, LiquidStream.STREAM_CODEC.apply(ByteBufCodecs.list(8)), LiquidsPayload::streams,
			Stocked.STREAM_CODEC.apply(ByteBufCodecs.list(64)), LiquidsPayload::stocked, LiquidsPayload::new);

	/** An inlet: a pipe on it, the liquid that came last, mB a second, waiting its turn (the tank holds another). */
	public record Inlet(boolean piped, FluidVariant liquid, long rate, boolean waits) {
		public static final StreamCodec<RegistryFriendlyByteBuf, Inlet> STREAM_CODEC = StreamCodec.composite(
				ByteBufCodecs.BOOL, Inlet::piped, FluidVariant.PACKET_CODEC, Inlet::liquid, ByteBufCodecs.VAR_LONG, Inlet::rate,
				ByteBufCodecs.BOOL, Inlet::waits, Inlet::new);
	}

	/**
	 * The Drum Filling Station: its state (FillerBlockEntity), the buffer tank's liquid and mB, the drum it fills now
	 * (1..4, 0 none) and how far (mB), the empty drums by kind, the pallets, the free magazine places, a line saying what
	 * holds it up ("" none).
	 */
	public record Station(int state, FluidVariant liquid, long tank, int drum, long filled, List<Integer> drums, int pallets, int free, String note) {
		public static final StreamCodec<RegistryFriendlyByteBuf, Station> STREAM_CODEC = StreamCodec.composite(
				ByteBufCodecs.VAR_INT, Station::state, FluidVariant.PACKET_CODEC, Station::liquid, ByteBufCodecs.VAR_LONG, Station::tank,
				ByteBufCodecs.VAR_INT, Station::drum, ByteBufCodecs.VAR_LONG, Station::filled, ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list(4)),
				Station::drums, ByteBufCodecs.VAR_INT, Station::pallets, ByteBufCodecs.VAR_INT, Station::free, ByteBufCodecs.STRING_UTF8, Station::note,
				Station::new);
		public static final Station NONE = new Station(0, FluidVariant.blank(), 0, 0, 0, List.of(0, 0, 0, 0), 0, 0, "");
	}

	/** A liquid stream: its place in the terminal's streams, the liquid (a book drum), mB a second, the reserve (mB), held. */
	public record LiquidStream(int index, ItemStack liquid, int rate, long reserve, boolean held) {
		public static final StreamCodec<RegistryFriendlyByteBuf, LiquidStream> STREAM_CODEC = StreamCodec.composite(
				ByteBufCodecs.VAR_INT, LiquidStream::index, ItemStack.STREAM_CODEC, LiquidStream::liquid, ByteBufCodecs.VAR_INT, LiquidStream::rate,
				ByteBufCodecs.VAR_LONG, LiquidStream::reserve, ByteBufCodecs.BOOL, LiquidStream::held, LiquidStream::new);
	}

	/** A liquid the warehouse stores (a book drum) and how much (mB). */
	public record Stocked(ItemStack liquid, long amount) {
		public static final StreamCodec<RegistryFriendlyByteBuf, Stocked> STREAM_CODEC = StreamCodec.composite(
				ItemStack.STREAM_CODEC, Stocked::liquid, ByteBufCodecs.VAR_LONG, Stocked::amount, Stocked::new);
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
