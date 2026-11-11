package io.github.jimbozoomer.jugcraft.warehouse;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;

/**
 * The GOODS IN screen's status (server to the player using it, twice a second): what the case packer is packing and
 * how far, the cases on its conveyor, the pallets being built, the pallet places used, each pipe inlet's flow, and the
 * last pallets put away.
 */
public record GoodsInPayload(ItemStack packing, float progress, int conveyor, int building, int placesUsed, int placesTotal, List<Inlet> inlets,
		List<TerminalPayload.Logged> log, boolean networked, long time) implements CustomPacketPayload {
	public static final Type<GoodsInPayload> TYPE = new Type<>(Jugcraft.id("warehouse_goods_in"));
	public static final StreamCodec<RegistryFriendlyByteBuf, GoodsInPayload> CODEC = StreamCodec.composite(
			ItemStack.OPTIONAL_STREAM_CODEC, GoodsInPayload::packing, ByteBufCodecs.FLOAT, GoodsInPayload::progress,
			ByteBufCodecs.VAR_INT, GoodsInPayload::conveyor, ByteBufCodecs.VAR_INT, GoodsInPayload::building,
			ByteBufCodecs.VAR_INT, GoodsInPayload::placesUsed, ByteBufCodecs.VAR_INT, GoodsInPayload::placesTotal,
			Inlet.STREAM_CODEC.apply(ByteBufCodecs.list(4)), GoodsInPayload::inlets,
			TerminalPayload.Logged.STREAM_CODEC.apply(ByteBufCodecs.list(16)), GoodsInPayload::log,
			ByteBufCodecs.BOOL, GoodsInPayload::networked, ByteBufCodecs.VAR_LONG, GoodsInPayload::time, GoodsInPayload::new);

	/** An inlet: whether it has a pipe on it, what came last, items a second, backed up. */
	public record Inlet(boolean piped, ItemStack last, int rate, boolean backed) {
		public static final StreamCodec<RegistryFriendlyByteBuf, Inlet> STREAM_CODEC = StreamCodec.composite(
				ByteBufCodecs.BOOL, Inlet::piped, ItemStack.OPTIONAL_STREAM_CODEC, Inlet::last, ByteBufCodecs.VAR_INT, Inlet::rate,
				ByteBufCodecs.BOOL, Inlet::backed, Inlet::new);
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
