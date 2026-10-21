package io.github.jimbozoomer.jugcraft.finance;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Client to server: something the player asked the Exchange to do. The server checks everything again (amounts,
 * funds, permissions, rate) before acting; the client only fills in the form.
 *
 * @param action  what to do ({@link Action})
 * @param target  a player name (pay) or text (currency name), may be empty
 * @param amount  cents, a rate in basis points, or a contract id, depending on the action
 * @param period  {@link FinanceBook.Period} ordinal for payments
 * @param note    what the payer gets in return (payments)
 * @param extra   a second number: a quantity (buy, list, request, fill) or auction length index
 * @param data    a corporation logo (256 palette indices), else empty
 */
public record FinanceActionPayload(Action action, String target, long amount, int period, String note, long extra,
		byte[] data) implements CustomPacketPayload {
	public static final int MAX_DATA = 512;

	public FinanceActionPayload(Action action, String target, long amount, int period, String note, long extra) {
		this(action, target, amount, period, note, extra, new byte[0]);
	}

	public static final int MAX_TEXT = 128;
	public static final Type<FinanceActionPayload> TYPE = new Type<>(Jugcraft.id("finance_action"));
	public static final StreamCodec<RegistryFriendlyByteBuf, FinanceActionPayload> CODEC = StreamCodec.ofMember(
			FinanceActionPayload::write, FinanceActionPayload::read);

	/** Everything a player can ask. The ADMIN_ ones need operator permission on the server. */
	public enum Action {
		/** Send me the Exchange (and open it). */
		OPEN,
		/** Send me fresh numbers for the open screen. */
		REFRESH,
		BORROW,
		REPAY,
		PAY,
		CANCEL_CONTRACT,
		/** amount = the loan fee in basis points. */
		ADMIN_FEE,
		ADMIN_LIMIT,
		/** List the main-hand item: amount = price (per item, or opening bid), extra = quantity, period = 0 fixed / 1 auction, target = auction length index, note = company id to sell for (empty: yourself). */
		LIST_ITEM,
		/** amount = listing id, extra = quantity. */
		BUY,
		/** amount = listing id, extra = bid in cents. */
		BID,
		/** amount = listing id. */
		CANCEL_LISTING,
		COLLECT,
		/** target = item id, extra = quantity, amount = price per item (0 = advertisement). */
		POST_REQUEST,
		/** amount = request id, extra = quantity. */
		FILL_REQUEST,
		/** amount = request id. */
		CANCEL_REQUEST,
		/** Take the one-time starter loan. */
		STARTER_LOAN,
		/** amount = loan term in days. */
		ADMIN_TERM,
		/** amount = chunk key ({@link LandRegistry#key}), extra = the price the screen showed. */
		LAND_BUY,
		/** amount = chunk key, extra = asking price (0 = stop selling). */
		LAND_SELL,
		/** amount = chunk key. */
		LAND_ABANDON,
		/** target = player name, period = 1 to trust, 0 to stop trusting. */
		LAND_TRUST,
		/** amount = chunk key, extra = corporation id (0 = back to yourself). */
		LAND_ZONE,
		/** target = name, note = ticker, amount = shares, period = offered share in basis points, extra = price per share, data = logo (+1 byte: 1 uploaded). */
		CORP_CHARTER,
		/** amount = corporation id, period = 1 buy / 0 sell, extra = price per share, note = quantity. */
		CORP_ORDER,
		/** amount = order id. */
		CORP_CANCEL,
		/** amount = corporation id, extra = total dividend. */
		CORP_DIVIDEND,
		/** amount = corporation id, data = logo (+1 byte: 1 uploaded). */
		CORP_LOGO,
		/** amount = corporation id, period = 1 approve / 0 remove. */
		ADMIN_LOGO,
		/** target = setting key ({@link ExchangeSettings}), note = the value as typed. */
		ADMIN_SET,
		/** amount = corporation id, extra = price per new share, note = how many. */
		CORP_ISSUE,
		/** amount = chunk key, extra = opening bid, period = auction length index. */
		LAND_AUCTION,
		/** amount = chunk key, extra = bid. */
		LAND_BID,
		/** extra = Market Chest position: send me its panel. */
		CHEST_INFO,
		/** extra = chest position, target = item id, amount = price per item (0 = stop selling it). */
		CHEST_PRICE,
		/** extra = chest position, period = 1 pause / 0 resume. */
		CHEST_PAUSE,
		/** extra = chest position: bring unsold goods back into it. */
		CHEST_TAKE_BACK,
		/** extra = chest position, amount = corporation id. */
		CHEST_COMPANY,
		/** amount = corporation id: buy a Market Chest for it from the Exchange. */
		CHEST_BUY,
		/** extra = poker table position: keep sending me its view. */
		POKER_VIEW,
		/** extra = table position: I closed its overlay. */
		POKER_CLOSE,
		/** extra = table position, period = seat, amount = buy-in. */
		POKER_SIT,
		/** extra = table position: leave the table with my chips. */
		POKER_STAND,
		/** extra = table position, period = {@link PokerGame.Move} ordinal, amount = raise to. */
		POKER_ACT,
		/** extra = casino game position: keep sending me its view. */
		CASINO_VIEW,
		/** extra = game position: I closed its screen. */
		CASINO_CLOSE,
		/**
		 * extra = game position, amount = the bet. Blackjack: period = seat. Roulette: target = the numbers (bits).
		 * Craps: period = {@link Craps.Kind} ordinal, target = the number.
		 */
		CASINO_BET,
		/**
		 * extra = game position, period = the move. Blackjack: {@link Blackjack.Move} ordinal. Roulette: 0 take my
		 * chips back, 1 spin now. Craps: 0 roll, 1 odds (target = bet index, amount), 2 take down (target = bet
		 * index). Slots: 0 spin (amount = bet).
		 */
		CASINO_ACT,
		/**
		 * extra = game position, amount = least bet, target = most bet, period = 1 for the central bank as the house, note =
		 * further settings ("clock=20;risks=7": betting seconds, 0 for the server's; Plinko risks allowed, bits Low, Medium,
		 * High). Period 2 resets the takings instead. The owner only.
		 */
		CASINO_LIMITS,
		/**
		 * extra = poker table position, amount = small blind, target = "big blind,least buy-in,most buy-in,turn seconds" (each
		 * 0 for the server's setting). The table's owner only.
		 */
		POKER_SETTINGS;

		static Action byId(int id) {
			return id >= 0 && id < values().length ? values()[id] : REFRESH;
		}
	}

	public static FinanceActionPayload simple(Action action) {
		return new FinanceActionPayload(action, "", 0, 0, "", 0);
	}

	private void write(RegistryFriendlyByteBuf buf) {
		buf.writeVarInt(action.ordinal());
		buf.writeUtf(target, MAX_TEXT);
		buf.writeVarLong(amount);
		buf.writeVarInt(period);
		buf.writeUtf(note, MAX_TEXT);
		buf.writeVarLong(extra);
		int length = Math.min(MAX_DATA, data.length);
		buf.writeVarInt(length);
		for (int i = 0; i < length; i++) {
			buf.writeByte(data[i]);
		}
	}

	private static FinanceActionPayload read(RegistryFriendlyByteBuf buf) {
		Action action = Action.byId(buf.readVarInt());
		String target = buf.readUtf(MAX_TEXT);
		long amount = buf.readVarLong();
		int period = buf.readVarInt();
		String note = buf.readUtf(MAX_TEXT);
		long extra = buf.readVarLong();
		int length = buf.readVarInt();
		if (length < 0 || length > MAX_DATA) {
			throw new IllegalArgumentException("Logo data too long: " + length);
		}
		byte[] data = new byte[length];
		for (int i = 0; i < length; i++) {
			data[i] = buf.readByte();
		}
		return new FinanceActionPayload(action, target, amount, period, note, extra, data);
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
