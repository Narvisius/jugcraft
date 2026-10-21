package io.github.jimbozoomer.jugcraft.finance;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Server to client: one player's view of a casino game. The parts every game shares are fields; the game's own state
 * is a list of numbers ({@code data}) and of texts ({@code text}) laid out by {@link FinanceService#sendCasino} and read
 * back in the same order by the game's screen. Display only: every bet is checked again by the server.
 */
public record CasinoPayload(long pos, int game, boolean open, String owner, boolean isOwner, boolean bankHouse, long minBet, long maxBet,
		long limitMin, long limitMax, long cash, long cover, int phase, int timer, long[] data, List<String> text,
		List<String> log, String notice, boolean noticeOk, long[] table) implements CustomPacketPayload {
	/** {@code table}: see {@link CasinoBlockEntity#tableInfo}. */
	public static final int TABLE_FIELDS = 11;
	public static final int MAX_DATA = 4096;
	public static final int MAX_TEXT = 64;
	public static final int MAX_LINE = 160;
	public static final Type<CasinoPayload> TYPE = new Type<>(Jugcraft.id("casino"));
	public static final StreamCodec<RegistryFriendlyByteBuf, CasinoPayload> CODEC = StreamCodec.ofMember(
			CasinoPayload::write, CasinoPayload::read);

	private void write(RegistryFriendlyByteBuf buf) {
		buf.writeVarLong(pos);
		buf.writeVarInt(game);
		buf.writeBoolean(open);
		buf.writeUtf(owner, 40);
		buf.writeBoolean(isOwner);
		buf.writeBoolean(bankHouse);
		buf.writeVarLong(minBet);
		buf.writeVarLong(maxBet);
		buf.writeVarLong(limitMin);
		buf.writeVarLong(limitMax);
		buf.writeVarLong(cash);
		buf.writeVarLong(cover);
		buf.writeVarInt(phase);
		buf.writeVarInt(timer);
		int n = Math.min(MAX_DATA, data.length);
		buf.writeVarInt(n);
		for (int i = 0; i < n; i++) {
			buf.writeLong(data[i]);
		}
		int t = Math.min(MAX_TEXT, text.size());
		buf.writeVarInt(t);
		for (int i = 0; i < t; i++) {
			buf.writeUtf(text.get(i), 40);
		}
		int l = Math.min(8, log.size());
		buf.writeVarInt(l);
		for (int i = log.size() - l; i < log.size(); i++) {
			buf.writeUtf(log.get(i), MAX_LINE);
		}
		buf.writeUtf(notice, MAX_LINE);
		buf.writeBoolean(noticeOk);
		for (int i = 0; i < TABLE_FIELDS; i++) {
			buf.writeLong(i < table.length ? table[i] : 0L);
		}
	}

	private static CasinoPayload read(RegistryFriendlyByteBuf buf) {
		long pos = buf.readVarLong();
		int game = buf.readVarInt();
		boolean open = buf.readBoolean();
		String owner = buf.readUtf(40);
		boolean isOwner = buf.readBoolean();
		boolean bankHouse = buf.readBoolean();
		long minBet = buf.readVarLong();
		long maxBet = buf.readVarLong();
		long limitMin = buf.readVarLong();
		long limitMax = buf.readVarLong();
		long cash = buf.readVarLong();
		long cover = buf.readVarLong();
		int phase = buf.readVarInt();
		int timer = buf.readVarInt();
		int n = buf.readVarInt();
		if (n < 0 || n > MAX_DATA) {
			throw new IllegalArgumentException("Casino view too long: " + n);
		}
		long[] data = new long[n];
		for (int i = 0; i < n; i++) {
			data[i] = buf.readLong();
		}
		int t = Math.min(MAX_TEXT, buf.readVarInt());
		List<String> text = new ArrayList<>(t);
		for (int i = 0; i < t; i++) {
			text.add(buf.readUtf(40));
		}
		int l = Math.min(8, buf.readVarInt());
		List<String> log = new ArrayList<>(l);
		for (int i = 0; i < l; i++) {
			log.add(buf.readUtf(MAX_LINE));
		}
		String notice = buf.readUtf(MAX_LINE);
		boolean ok = buf.readBoolean();
		long[] table = new long[TABLE_FIELDS];
		for (int i = 0; i < TABLE_FIELDS; i++) {
			table[i] = buf.readLong();
		}
		return new CasinoPayload(pos, game, open, owner, isOwner, bankHouse, minBet, maxBet, limitMin, limitMax, cash, cover, phase,
				timer, data, text, log, notice, ok, table);
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	/** Reads {@link #data} in order. */
	public static final class Reader {
		private final long[] data;
		private int at;

		public Reader(long[] data) {
			this.data = data;
		}

		public long next() {
			return at < data.length ? data[at++] : 0;
		}

		public int nextInt() {
			return (int) next();
		}

		public boolean nextBool() {
			return next() != 0;
		}
	}
}
