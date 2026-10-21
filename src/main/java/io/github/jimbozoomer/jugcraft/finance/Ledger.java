package io.github.jimbozoomer.jugcraft.finance;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * The server's {@link FinanceBook}, saved with the overworld (data/jugcraft_finance.dat). One per server, whatever
 * dimension a player is in. Call {@link #setDirty()} after changing the book.
 */
public final class Ledger extends SavedData {
	private static final Codec<FinanceBook.Account> ACCOUNT = RecordCodecBuilder.create(i -> i.group(
			UUIDUtil.CODEC.fieldOf("id").forGetter(a -> a.id),
			Codec.STRING.fieldOf("name").forGetter(a -> a.name),
			Codec.LONG.fieldOf("debt").forGetter(a -> a.debt),
			Codec.LONG.optionalFieldOf("principal", -1L).forGetter(a -> a.principal),
			Codec.LONG.optionalFieldOf("due_day", -1L).forGetter(a -> a.dueDay),
			Codec.INT.optionalFieldOf("credit_tier", 0).forGetter(a -> a.creditTier),
			Codec.BOOL.optionalFieldOf("starter_taken", false).forGetter(a -> a.starterTaken),
			Codec.BOOL.optionalFieldOf("late_charged", false).forGetter(a -> a.lateCharged),
			Codec.BOOL.optionalFieldOf("corporate", false).forGetter(a -> a.corporate)
	).apply(i, (id, name, debt, principal, dueDay, tier, starter, late, corporate) -> {
		FinanceBook.Account account = new FinanceBook.Account(id, name, debt, principal < 0 ? debt : principal, dueDay, tier,
				starter, late);
		account.corporate = corporate;
		return account;
	}));

	private static final Codec<FinanceBook.Contract> CONTRACT = RecordCodecBuilder.create(i -> i.group(
			Codec.LONG.fieldOf("id").forGetter(c -> c.id),
			UUIDUtil.CODEC.fieldOf("payer").forGetter(c -> c.payer),
			UUIDUtil.CODEC.fieldOf("payee").forGetter(c -> c.payee),
			Codec.LONG.fieldOf("amount").forGetter(c -> c.amount),
			Codec.STRING.xmap(FinanceBook.Period::parse, FinanceBook.Period::name).fieldOf("period").forGetter(c -> c.period),
			Codec.STRING.fieldOf("note").forGetter(c -> c.note),
			Codec.LONG.fieldOf("next_day").forGetter(c -> c.nextDay),
			Codec.BOOL.fieldOf("active").forGetter(c -> c.active)
	).apply(i, FinanceBook.Contract::new));

	private static final Codec<FinanceBook.Entry> ENTRY = RecordCodecBuilder.create(i -> i.group(
			Codec.LONG.fieldOf("day").forGetter(FinanceBook.Entry::day),
			UUIDUtil.CODEC.optionalFieldOf("first").forGetter(e -> Optional.ofNullable(e.first())),
			UUIDUtil.CODEC.optionalFieldOf("second").forGetter(e -> Optional.ofNullable(e.second())),
			Codec.STRING.fieldOf("text").forGetter(FinanceBook.Entry::text)
	).apply(i, (day, first, second, text) -> new FinanceBook.Entry(day, first.orElse(null), second.orElse(null), text)));

    private static final Codec<FinanceBook.PrizeDebt> PRIZE_DEBT = RecordCodecBuilder.create(i -> i.group(
            UUIDUtil.CODEC.optionalFieldOf("house").forGetter(d -> Optional.ofNullable(d.house())),
            UUIDUtil.CODEC.fieldOf("player").forGetter(FinanceBook.PrizeDebt::player),
            Codec.LONG.fieldOf("amount").forGetter(FinanceBook.PrizeDebt::amount)
    ).apply(i, (house, player, amount) -> new FinanceBook.PrizeDebt(house.orElse(null), player, amount)));

	private static final Codec<FinanceBook> BOOK = RecordCodecBuilder.create(i -> i.group(
			// "rate_bp" (the old daily interest) is no longer read: loans now carry a flat fee.
			Codec.INT.optionalFieldOf("fee_bp", FinanceBook.DEFAULT_FEE_BP).forGetter(FinanceBook::feeBp),
			Codec.LONG.optionalFieldOf("borrow_limit", FinanceBook.DEFAULT_LIMIT).forGetter(FinanceBook::borrowLimit),
			Codec.INT.optionalFieldOf("term_days", FinanceBook.DEFAULT_TERM_DAYS).forGetter(FinanceBook::termDays),
			Codec.INT.optionalFieldOf("share_bp", FinanceBook.DEFAULT_SHARE_BP).forGetter(FinanceBook::shareBp),
			Codec.LONG.optionalFieldOf("reserve", FinanceBook.DEFAULT_RESERVE).forGetter(FinanceBook::reserve),
			Codec.LONG.optionalFieldOf("last_day", -1L).forGetter(FinanceBook::lastDay),
			Codec.LONG.optionalFieldOf("next_contract", 1L).forGetter(FinanceBook::nextContractId),
			Codec.BOOL.optionalFieldOf("broadcast", true).forGetter(FinanceBook::broadcast),
			Codec.LONG.optionalFieldOf("day_offset", 0L).forGetter(FinanceBook::dayOffset),
			Codec.LONG.optionalFieldOf("escrow", 0L).forGetter(FinanceBook::escrow),
			ACCOUNT.listOf().optionalFieldOf("accounts", List.of()).forGetter(b -> new ArrayList<>(b.accounts())),
			CONTRACT.listOf().optionalFieldOf("contracts", List.of()).forGetter(FinanceBook::contracts),
			ENTRY.listOf().optionalFieldOf("log", List.of()).forGetter(b -> new ArrayList<>(b.log())),
			Codec.unboundedMap(Codec.STRING, Codec.LONG).optionalFieldOf("settings", Map.of()).forGetter(FinanceBook::settings),
            Codec.unboundedMap(UUIDUtil.STRING_CODEC, Codec.LONG).optionalFieldOf("pending_credits", Map.of()).forGetter(FinanceBook::pendingCredits),
            PRIZE_DEBT.listOf().optionalFieldOf("prize_debts",List.of()).forGetter(FinanceBook::prizeDebts)
	).apply(i, (fee, limit, term, share, reserve, lastDay, nextContract, broadcast, offset, escrow, accounts, contracts, log,
			settings, credits, prizeDebts) -> {
		FinanceBook book = new FinanceBook(fee, limit, term, share, reserve, lastDay, nextContract, broadcast, offset, escrow,
				accounts, contracts, log);
		settings.forEach(book::putSetting);
        book.restoreCredits(credits);
        book.restorePrizeDebts(prizeDebts);
		return book;
	}));

	public static final Codec<Ledger> CODEC = BOOK.xmap(Ledger::new, Ledger::book);
	static final SavedDataType<Ledger> TYPE = new SavedDataType<>(Jugcraft.id("finance"), Ledger::new, CODEC, null);

	private final FinanceBook book;

	Ledger() {
		this(new FinanceBook());
	}

	public Ledger(FinanceBook book) {
		this.book = book;
	}

	public FinanceBook book() {
		return book;
	}

	/** The server's ledger. Always the overworld's, so every dimension shares one economy. */
	public static Ledger get(MinecraftServer server) {
		Ledger ledger = server.overworld().getDataStorage().computeIfAbsent(TYPE);
		ledger.book.useWallet(new JugsWallet(server));
		return ledger;
	}
}
