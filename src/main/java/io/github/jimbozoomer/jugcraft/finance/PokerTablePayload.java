package io.github.jimbozoomer.jugcraft.finance;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Server to client: one player's view of a poker table. Other players' hole cards are only included once they are
 * shown at a showdown, so a client never knows more than its player may. Display only; every move is checked again
 * by the server.
 */
public record PokerTablePayload(long pos, int design, boolean open, int phase, long hand, List<SeatView> seats, int[] board,
		long pot, long currentBet, long toCall, long minRaiseTo, long maxRaiseTo, int actor, int button, int mySeat,
		int timerTicks, long smallBlind, long bigBlind, long minBuyIn, long maxBuyIn, long cash, List<String> log,
		String notice, boolean noticeOk, String owner, boolean isOwner, int turnSeconds, long[] own) implements CustomPacketPayload {
	/** {@code own}: see {@link PokerTableBlockEntity#ownSettings}. */
	public static final int OWN_FIELDS = 7;
	public static final int MAX_TEXT = 160;
	public static final Type<PokerTablePayload> TYPE = new Type<>(Jugcraft.id("poker_table"));
	public static final StreamCodec<RegistryFriendlyByteBuf, PokerTablePayload> CODEC = StreamCodec.ofMember(
			PokerTablePayload::write, PokerTablePayload::read);

	/** A seat. Cards are -1 when hidden; an empty seat has an empty name. */
	public record SeatView(String name, long stack, long bet, boolean inHand, boolean folded, boolean allIn, int card1,
			int card2, boolean me) {
		public boolean occupied() {
			return !name.isEmpty();
		}
	}

	private void write(RegistryFriendlyByteBuf buf) {
		buf.writeVarLong(pos);
		buf.writeVarInt(design);
		buf.writeBoolean(open);
		buf.writeVarInt(phase);
		buf.writeVarLong(hand);
		buf.writeVarInt(seats.size());
		for (SeatView seat : seats) {
			buf.writeUtf(seat.name(), 40);
			buf.writeVarLong(seat.stack());
			buf.writeVarLong(seat.bet());
			buf.writeBoolean(seat.inHand());
			buf.writeBoolean(seat.folded());
			buf.writeBoolean(seat.allIn());
			buf.writeVarInt(seat.card1() + 1);
			buf.writeVarInt(seat.card2() + 1);
			buf.writeBoolean(seat.me());
		}
		for (int i = 0; i < 5; i++) {
			buf.writeVarInt(board[i] + 1);
		}
		buf.writeVarLong(pot);
		buf.writeVarLong(currentBet);
		buf.writeVarLong(toCall);
		buf.writeVarLong(minRaiseTo);
		buf.writeVarLong(maxRaiseTo);
		buf.writeVarInt(actor + 1);
		buf.writeVarInt(button + 1);
		buf.writeVarInt(mySeat + 1);
		buf.writeVarInt(timerTicks);
		buf.writeVarLong(smallBlind);
		buf.writeVarLong(bigBlind);
		buf.writeVarLong(minBuyIn);
		buf.writeVarLong(maxBuyIn);
		buf.writeVarLong(cash);
		buf.writeVarInt(Math.min(log.size(), 8));
		for (int i = Math.max(0, log.size() - 8); i < log.size(); i++) {
			buf.writeUtf(log.get(i), MAX_TEXT);
		}
		buf.writeUtf(notice, MAX_TEXT);
		buf.writeBoolean(noticeOk);
		buf.writeUtf(owner, 40);
		buf.writeBoolean(isOwner);
		buf.writeVarInt(turnSeconds);
		for (int i = 0; i < OWN_FIELDS; i++) {
			buf.writeVarLong(i < own.length ? Math.max(0, own[i]) : 0L);
		}
	}

	private static PokerTablePayload read(RegistryFriendlyByteBuf buf) {
		long pos = buf.readVarLong();
		int design = buf.readVarInt();
		boolean open = buf.readBoolean();
		int phase = buf.readVarInt();
		long hand = buf.readVarLong();
		int count = Math.min(PokerGame.SEATS, buf.readVarInt());
		List<SeatView> seats = new ArrayList<>(count);
		for (int i = 0; i < count; i++) {
			seats.add(new SeatView(buf.readUtf(40), buf.readVarLong(), buf.readVarLong(), buf.readBoolean(), buf.readBoolean(),
					buf.readBoolean(), buf.readVarInt() - 1, buf.readVarInt() - 1, buf.readBoolean()));
		}
		int[] board = new int[5];
		for (int i = 0; i < 5; i++) {
			board[i] = buf.readVarInt() - 1;
		}
		long pot = buf.readVarLong();
		long currentBet = buf.readVarLong();
		long toCall = buf.readVarLong();
		long minRaiseTo = buf.readVarLong();
		long maxRaiseTo = buf.readVarLong();
		int actor = buf.readVarInt() - 1;
		int button = buf.readVarInt() - 1;
		int mySeat = buf.readVarInt() - 1;
		int timer = buf.readVarInt();
		long sb = buf.readVarLong();
		long bb = buf.readVarLong();
		long minBuy = buf.readVarLong();
		long maxBuy = buf.readVarLong();
		long cash = buf.readVarLong();
		int lines = Math.min(8, buf.readVarInt());
		List<String> log = new ArrayList<>(lines);
		for (int i = 0; i < lines; i++) {
			log.add(buf.readUtf(MAX_TEXT));
		}
		String notice = buf.readUtf(MAX_TEXT);
		boolean ok = buf.readBoolean();
		String owner = buf.readUtf(40);
		boolean isOwner = buf.readBoolean();
		int turnSeconds = buf.readVarInt();
		long[] own = new long[OWN_FIELDS];
		for (int i = 0; i < OWN_FIELDS; i++) {
			own[i] = buf.readVarLong();
		}
		return new PokerTablePayload(pos, design, open, phase, hand, seats, board, pot, currentBet, toCall, minRaiseTo,
				maxRaiseTo, actor, button, mySeat, timer, sb, bb, minBuy, maxBuy, cash, log, notice, ok, owner, isOwner, turnSeconds, own);
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
