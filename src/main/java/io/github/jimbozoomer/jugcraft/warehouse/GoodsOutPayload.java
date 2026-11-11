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
 * The Goods Out screens' status (part W2; server to the player using one, twice a second): the orders (theirs and
 * everyone's line), Ready Storage's slots used and in all and the player's own stacks, the tray, the next order coming
 * to it, the Output Port's mode, buffer and streams, how many items a second went out and whether a pipe is on the
 * flange, the time of day, the window's filter (mine) and its first row shown.
 */
public record GoodsOutPayload(BlockPos panel, List<TerminalPayload.OrderLine> orders, int poolUsed, int poolSlots, int mineStacks, boolean trayOut,
		String next, int portMode, List<ItemStack> buffer, List<StreamLine> streams, int flow, boolean piped, long time, boolean mine, int offset)
		implements CustomPacketPayload {
	public static final Type<GoodsOutPayload> TYPE = new Type<>(Jugcraft.id("warehouse_goods_out"));
	private record Head(BlockPos panel, List<TerminalPayload.OrderLine> orders, int poolUsed, int poolSlots, int mineStacks, boolean trayOut, String next) {
		static final StreamCodec<RegistryFriendlyByteBuf, Head> CODEC = StreamCodec.composite(
				BlockPos.STREAM_CODEC, Head::panel, TerminalPayload.OrderLine.STREAM_CODEC.apply(ByteBufCodecs.list(256)), Head::orders,
				ByteBufCodecs.VAR_INT, Head::poolUsed, ByteBufCodecs.VAR_INT, Head::poolSlots, ByteBufCodecs.VAR_INT, Head::mineStacks,
				ByteBufCodecs.BOOL, Head::trayOut, ByteBufCodecs.STRING_UTF8, Head::next, Head::new);
	}

	private record Tail(int portMode, List<ItemStack> buffer, List<StreamLine> streams, int flow, boolean piped, long time, boolean mine, int offset) {
		static final StreamCodec<RegistryFriendlyByteBuf, Tail> CODEC = StreamCodec.composite(
				ByteBufCodecs.VAR_INT, Tail::portMode, ItemStack.OPTIONAL_STREAM_CODEC.apply(ByteBufCodecs.list(PortBuffer.SLOTS)), Tail::buffer,
				StreamLine.STREAM_CODEC.apply(ByteBufCodecs.list(8)), Tail::streams, ByteBufCodecs.VAR_INT, Tail::flow, ByteBufCodecs.BOOL, Tail::piped,
				ByteBufCodecs.VAR_LONG, Tail::time, ByteBufCodecs.BOOL, Tail::mine, ByteBufCodecs.VAR_INT, Tail::offset, Tail::new);
	}

	public static final StreamCodec<RegistryFriendlyByteBuf, GoodsOutPayload> CODEC = StreamCodec.composite(Head.CODEC,
			p -> new Head(p.panel, p.orders, p.poolUsed, p.poolSlots, p.mineStacks, p.trayOut, p.next), Tail.CODEC,
			p -> new Tail(p.portMode, p.buffer, p.streams, p.flow, p.piped, p.time, p.mine, p.offset),
			(h, t) -> new GoodsOutPayload(h.panel, h.orders, h.poolUsed, h.poolSlots, h.mineStacks, h.trayOut, h.next, t.portMode, t.buffer,
					t.streams, t.flow, t.piped, t.time, t.mine, t.offset));

	/** A stream: its item, items a second, the reserve kept, held (at its reserve). */
	public record StreamLine(ItemStack item, int rate, long reserve, boolean held) {
		public static final StreamCodec<RegistryFriendlyByteBuf, StreamLine> STREAM_CODEC = StreamCodec.composite(
				ItemStack.OPTIONAL_STREAM_CODEC, StreamLine::item, ByteBufCodecs.VAR_INT, StreamLine::rate, ByteBufCodecs.VAR_LONG, StreamLine::reserve,
				ByteBufCodecs.BOOL, StreamLine::held, StreamLine::new);
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
