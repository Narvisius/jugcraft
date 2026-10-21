package io.github.jimbozoomer.jugcraft.finance;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * The Exchange's bank and market rules, in Jugcraft's one currency: Jugs (the town's credit, {@code town/Jugs}).
 * Plain Java on purpose: no Minecraft types, so the rules can be tested without a game. Players' Jugs live in a
 * {@link Wallet} (the real Jugs balances in game, a map in tests); this book keeps what the Exchange adds around them:
 * the central bank's reserve, loans, money held for bids and requests, contracts and the chat ledger. {@link Ledger}
 * saves it with the overworld and {@link FinanceService} drives it.
 *
 * <p>Amounts are whole Jugs. Jugs never exist as an item. The Exchange puts Jugs into play only from the central bank's
 * reserve (loans and admin grants), which the admin can enlarge by minting; the town's shops make and take Jugs on
 * their own. Every movement the Exchange makes is written to the chat ledger ({@link #drainNotices()}).
 *
 * <p>Loans exist to get new players trading: each player may take one starter loan, every loan carries a flat fee
 * and is due a set number of days later, and while a player owes the bank a share of everything they earn on the
 * market repays it. An overdue borrower pays a late fee once, gives a larger share of sales, and may sell but not buy,
 * bid, borrow or pay other players until the debt is cleared. Paying a loan off on time raises the player's credit
 * limit. Fees are owed, not created: they become money only when repaid, and then they return to the reserve.
 */
public final class FinanceBook {
	/** Largest single amount anyone may enter: the most Jugs a player may hold. Keeps every sum far from overflow. */
	public static final long MAX_AMOUNT = 1_000_000_000L;
	/** Debt stops compounding here so an abandoned loan cannot overflow. */
	public static final long MAX_DEBT = 1_000_000_000_000_000L;
	/** The owner's currency (docs/features/walled-town.md); the Exchange does not add another. */
	public static final String CURRENCY = "Jugs";
	public static final long DEFAULT_RESERVE = 1_000_000L;
	/** Flat fee on each loan: 10%. */
	public static final int DEFAULT_FEE_BP = 1_000;
	/** Starting credit limit, and the starter loan, in Jugs (the town pays about a Jug for an iron ingot). */
	public static final long DEFAULT_LIMIT = 100L;
	/** Days to repay a loan. */
	public static final int DEFAULT_TERM_DAYS = 10;
	/** Share of market earnings that repays a loan: 25%, doubled while overdue. */
	public static final int DEFAULT_SHARE_BP = 2_500;
	/** Charged once when a loan goes overdue: 10% of what is owed. */
	public static final int LATE_FEE_BP = 1_000;
	/** Each on-time payoff multiplies the credit limit by this, up to {@link #MAX_CREDIT_TIER} times. */
	public static final int CREDIT_GROWTH = 3;
	public static final int MAX_CREDIT_TIER = 3;
	/** The most credit steps an admin may allow (the tier saved on an account is clamped to this). */
	public static final int CREDIT_TIER_CAP = 10;
	public static final int MAX_TERM_DAYS = 365;
	public static final int MAX_NOTE = 100;
	public static final int MAX_CURRENCY_NAME = 24;
	/** Days caught up at most when the server was off or the world stood still; later days are skipped, not charged. */
	public static final int MAX_CATCH_UP_DAYS = 7;
	public static final int LOG_SIZE = 200;
	public static final int RICH_LIST = 10;

	/** How often a contract pays. */
	public enum Period {
		ONCE(0), DAILY(1), WEEKLY(7);

		public final int days;

		Period(int days) {
			this.days = days;
		}

		public static Period byId(int id) {
			return id >= 0 && id < values().length ? values()[id] : ONCE;
		}

		public static Period parse(String name) {
			for (Period period : values()) {
				if (period.name().equalsIgnoreCase(name)) {
					return period;
				}
			}
			return ONCE;
		}
	}

	/**
	 * One player's account. The name is the last one they played under, for display and for paying them offline.
	 * {@code debt} is everything owed (borrowed money plus fees); {@code principal} is the borrowed part, which the
	 * credit limit caps. {@code dueDay} is -1 with no loan.
	 */
	public static final class Account {
		public final UUID id;
		public String name;
		public long debt;
		public long principal;
		public long dueDay;
		public int creditTier;
		public boolean starterTaken;
		public boolean lateCharged;
		/** A corporation's treasury rather than a player (see {@link Equities}). */
		public boolean corporate;

		public Account(UUID id, String name, long debt) {
			this(id, name, debt, debt, -1, 0, false, false);
		}

		public Account(UUID id, String name, long debt, long principal, long dueDay, int creditTier, boolean starterTaken,
				boolean lateCharged) {
			this.id = id;
			this.name = name;
			this.debt = debt;
			this.principal = Math.max(0, Math.min(principal, debt));
			this.dueDay = debt > 0 ? dueDay : -1;
			this.creditTier = Math.max(0, Math.min(CREDIT_TIER_CAP, creditTier));
			this.starterTaken = starterTaken;
			this.lateCharged = lateCharged;
		}
	}

	/** A payment agreement: the payer pays the payee {@code amount} every period until either cancels or a payment bounces. */
	public static final class Contract {
		public final long id;
		public final UUID payer;
		public final UUID payee;
		public final long amount;
		public final Period period;
		public final String note;
		public long nextDay;
		public boolean active;

		public Contract(long id, UUID payer, UUID payee, long amount, Period period, String note, long nextDay, boolean active) {
			this.id = id;
			this.payer = payer;
			this.payee = payee;
			this.amount = amount;
			this.period = period;
			this.note = note;
			this.nextDay = nextDay;
			this.active = active;
		}
	}

	/** A line of the chat ledger. {@code first}/{@code second} are the parties (null for the central bank). */
	public record Entry(long day, UUID first, UUID second, String text) {
	}

	/** What an action did: {@code ok} and a message for the player who asked. */
	public record Result(boolean ok, String message) {
		static Result fail(String message) {
			return new Result(false, message);
		}
	}

	/** Where players' (and corporations') Jugs are kept. In game: the town's {@code Jugs}; in tests: {@link MapWallet}. */
	public interface Wallet {
		long balance(UUID id);

		/** Adds Jugs; false, and nothing added, when the holder would pass the most they may hold. */
		boolean add(UUID id, long amount);

		/** Takes Jugs if the holder has that many; false, and nothing taken, otherwise. */
		boolean take(UUID id, long amount);

		/** Every holder and their Jugs, for the money supply and the rich list. */
		Map<UUID, Long> all();
	}

	/** A wallet kept in a map: for tests, and before the game hands the book the real one. */
	public static final class MapWallet implements Wallet {
		private final Map<UUID, Long> balances = new java.util.HashMap<>();

		@Override
		public long balance(UUID id) {
			return balances.getOrDefault(id, 0L);
		}

		@Override
		public boolean add(UUID id, long amount) {
			if (amount < 0 || amount > MAX_AMOUNT - balance(id)) {
				return false;
			}
			balances.merge(id, amount, Long::sum);
			return true;
		}

		@Override
		public boolean take(UUID id, long amount) {
			if (amount < 0 || balance(id) < amount) {
				return false;
			}
			balances.put(id, balance(id) - amount);
			return true;
		}

		@Override
		public Map<UUID, Long> all() {
			return java.util.Collections.unmodifiableMap(balances);
		}
	}

	/** A rich-list row. */
	public record Rich(UUID id, String name, long cash) {
	}

	private Wallet wallet = new MapWallet();
	private final Map<UUID, Long> pendingCredits = new LinkedHashMap<>();
	public Map<UUID, Long> pendingCredits() { return java.util.Collections.unmodifiableMap(pendingCredits); }
	void restoreCredits(Map<UUID, Long> saved) { saved.forEach((id, value) -> { if (value > 0) pendingCredits.put(id, value); }); }
	private int feeBp;
	private long borrowLimit;
	private int termDays;
	private int shareBp;
	private long reserve;
	private long lastDay;
	private long nextContractId;
	private boolean broadcast;
	private long dayOffset;
	/** Money held for open bids and buy requests: still money, owned by its bidder until the deal settles. */
	private long escrow;
	private final Map<UUID, Account> accounts = new LinkedHashMap<>();
	private final List<Contract> contracts = new ArrayList<>();
	private final Deque<Entry> log = new ArrayDeque<>();
	private final List<Entry> pending = new ArrayList<>();
	/** Admin settings the Exchange keeps here by key ({@link ExchangeSettings}); missing keys use their defaults. */
	private final Map<String, Long> settings = new LinkedHashMap<>();

	public FinanceBook() {
		this(DEFAULT_FEE_BP, DEFAULT_LIMIT, DEFAULT_TERM_DAYS, DEFAULT_SHARE_BP, DEFAULT_RESERVE, -1, 1,
				true, 0, 0, List.of(), List.of(), List.of());
	}

	/** Restores a saved book (see {@link Ledger}). */
	public FinanceBook(int feeBp, long borrowLimit, int termDays, int shareBp, long reserve, long lastDay,
			long nextContractId, boolean broadcast, long dayOffset, long escrow, Collection<Account> accounts,
			Collection<Contract> contracts, Collection<Entry> log) {
		this.feeBp = Math.max(0, Math.min(10_000, feeBp));
		this.borrowLimit = Math.max(0, borrowLimit);
		this.termDays = Math.max(1, Math.min(MAX_TERM_DAYS, termDays));
		this.shareBp = Math.max(0, Math.min(10_000, shareBp));
		this.reserve = Math.max(0, reserve);
		this.lastDay = lastDay;
		this.nextContractId = Math.max(1, nextContractId);
		this.broadcast = broadcast;
		this.dayOffset = Math.max(0, dayOffset);
		this.escrow = Math.max(0, escrow);
		for (Account account : accounts) {
			this.accounts.put(account.id, account);
		}
		this.contracts.addAll(contracts);
		this.log.addAll(log);
		while (this.log.size() > LOG_SIZE) {
			this.log.removeFirst();
		}
	}

	// ------------------------------------------------------------------ reading

	public String currency() {
		return CURRENCY;
	}

	/** An admin setting kept in the book, or {@code fallback} when it was never set. */
	public long setting(String key, long fallback) {
		Long value = settings.get(key);
		return value == null ? fallback : value;
	}

	public void putSetting(String key, long value) {
		settings.put(key, value);
	}

	/** Every setting an admin changed, for saving. */
	public Map<String, Long> settings() {
		return settings;
	}

	/** Late fee, in basis points of what is owed. */
	public int lateFeeBp() {
		return (int) setting(ExchangeSettings.LATE_FEE, LATE_FEE_BP);
	}

	/** Each on-time payoff multiplies the credit limit by this. */
	public int creditGrowth() {
		return (int) setting(ExchangeSettings.CREDIT_GROWTH, CREDIT_GROWTH);
	}

	/** How many times the credit limit can grow. */
	public int creditSteps() {
		return (int) Math.min(CREDIT_TIER_CAP, setting(ExchangeSettings.CREDIT_STEPS, MAX_CREDIT_TIER));
	}

	/** Hands the book the real Jugs (the game does this each time it loads the book). */
	public void useWallet(Wallet wallet) {
		this.wallet = wallet;
	}

	/** {@code id}'s Jugs. */
	public long cash(UUID id) {
		return wallet.balance(id) + pendingCredits.getOrDefault(id, 0L);
	}

	/** Overflow remains the recipient's saved Exchange credit, never a donation to the bank. */
	private void give(UUID id, long amount) {
		if (amount > 0 && !wallet.add(id, amount)) {
			pendingCredits.merge(id, amount, Math::addExact);
		}
	}

	private boolean take(UUID id, long amount) {
		if (amount < 0 || cash(id) < amount) return false;
        long fromCredit = Math.min(amount, pendingCredits.getOrDefault(id, 0L));
        if (!wallet.take(id, amount - fromCredit)) return false;
        long remaining = pendingCredits.getOrDefault(id, 0L) - fromCredit;
        if (remaining == 0) pendingCredits.remove(id); else pendingCredits.put(id, remaining);
        return true;
	}

	/** The flat fee on each loan, in basis points. */
	public int feeBp() {
		return feeBp;
	}

	/** The starting credit limit, which is also the starter loan. */
	public long borrowLimit() {
		return borrowLimit;
	}

	public int termDays() {
		return termDays;
	}

	/** Share of market earnings that repays a loan, in basis points (doubled while overdue). */
	public int shareBp() {
		return shareBp;
	}

	/** {@code id}'s credit limit: the base limit, tripled for each loan paid off on time. */
	public long creditLimit(UUID id) {
		Account account = accounts.get(id);
		long limit = borrowLimit;
		int steps = account == null ? 0 : Math.min(account.creditTier, creditSteps());
		for (int i = 0; i < steps; i++) {
			limit = Math.min(MAX_AMOUNT, limit * Math.max(1, creditGrowth()));
		}
		return limit;
	}

	/** True when {@code id} owes money past its due day. */
	public boolean overdue(UUID id, long day) {
		Account account = accounts.get(id);
		return account != null && account.debt > 0 && account.dueDay >= 0 && day > account.dueDay;
	}

	/** True when {@code id} may still take the one-time starter loan. */
	public boolean starterAvailable(UUID id) {
		Account account = accounts.get(id);
		return account != null && !account.starterTaken && account.debt == 0 && borrowLimit > 0;
	}

	/** The share of market earnings {@code id} gives to their loan today, in basis points. */
	public int shareFor(UUID id, long day) {
		return overdue(id, day) ? Math.min(10_000, shareBp * 2) : shareBp;
	}

	public long reserve() {
		return reserve;
	}

	public long lastDay() {
		return lastDay;
	}

	public long nextContractId() {
		return nextContractId;
	}

	public boolean broadcast() {
		return broadcast;
	}

	/** Days the operators have closed early ({@code /jugcraft exchange admin nextday}); added to the world's day count. */
	public long dayOffset() {
		return dayOffset;
	}

	/** Moves the books one day ahead of the world (an early close, for events or testing). */
	public void closeDayEarly() {
		dayOffset++;
	}

	public Collection<Account> accounts() {
		return accounts.values();
	}

	public List<Contract> contracts() {
		return contracts;
	}

	public Collection<Entry> log() {
		return log;
	}

	public Account find(UUID id) {
		return accounts.get(id);
	}

	/** The account last seen under {@code name} (ignoring case), or null. */
	public Account findByName(String name) {
		for (Account account : accounts.values()) {
			if (account.name.equalsIgnoreCase(name.trim())) {
				return account;
			}
		}
		return null;
	}

	/** Gets or opens an account, remembering the player's current name. */
	public Account account(UUID id, String name) {
		Account account = accounts.computeIfAbsent(id, key -> new Account(key, name, 0));
		account.name = name;
		return account;
	}

    private Map<UUID, Long> balancesWithCredits() {
        Map<UUID, Long> all = new java.util.HashMap<>(wallet.all());
        pendingCredits.forEach((id, value) -> all.merge(id, value, Math::addExact));
        return all;
    }

	/** Jugs players hold (corporate treasuries excluded). */
	public long circulation() {
		long sum = 0;
		for (Map.Entry<UUID, Long> held : balancesWithCredits().entrySet()) {
			Account account = accounts.get(held.getKey());
			if (account == null || !account.corporate) {
				sum += held.getValue();
			}
		}
		return sum;
	}

	/** Jugs in corporate treasuries. */
	public long treasuries() {
		long sum = 0;
		for (Account account : accounts.values()) {
			if (account.corporate) {
				sum += cash(account.id);
			}
		}
		return sum;
	}

	/** Jugs that exist: what players and companies hold, what is held for bids and requests, and the reserve. */
	public long totalMoney() {
		return circulation() + treasuries() + escrow + reserve;
	}

	public long escrow() {
		return escrow;
	}

	/** The players holding the most Jugs, most first. */
	public List<Rich> richList() {
		List<Rich> out = new ArrayList<>();
		for (Map.Entry<UUID, Long> held : balancesWithCredits().entrySet()) {
			Account account = accounts.get(held.getKey());
			if (held.getValue() > 0 && (account == null || !account.corporate)) {
				out.add(new Rich(held.getKey(), account == null ? "?" : account.name, held.getValue()));
			}
		}
		out.sort(Comparator.comparingLong(Rich::cash).reversed());
		return out.size() > RICH_LIST ? new ArrayList<>(out.subList(0, RICH_LIST)) : out;
	}

	/** How much more {@code id} may borrow right now: their credit limit less what they borrowed, within the reserve. */
	public long borrowable(UUID id) {
		Account account = accounts.get(id);
		long borrowed = account == null ? 0 : account.principal;
		return Math.max(0, Math.min(creditLimit(id) - borrowed, reserve));
	}

	/** The ledger lines that mention {@code id}, newest first. */
	public List<Entry> historyOf(UUID id, int max) {
		List<Entry> out = new ArrayList<>();
		var it = log.descendingIterator();
		while (it.hasNext() && out.size() < max) {
			Entry entry = it.next();
			if (id.equals(entry.first()) || id.equals(entry.second())) {
				out.add(entry);
			}
		}
		return out;
	}

	/** New ledger lines since the last call, oldest first, for the chat. */
	public List<Entry> drainNotices() {
		List<Entry> out = List.copyOf(pending);
		pending.clear();
		loud.retainAll(java.util.Set.copyOf(out));
		return out;
	}

	// ------------------------------------------------------------------ players

	/** The flat fee on a loan of {@code amount}, rounded up to the cent. */
	public long fee(long amount) {
		return percent(amount, feeBp);
	}

	/**
	 * Lends {@code amount} from the reserve. The player owes it plus the fee, due {@link #termDays()} after their first
	 * open loan: borrowing more does not move an existing due day.
	 */
	public Result borrow(UUID id, long amount, long day) {
		Account account = accounts.get(id);
		if (account == null) {
			return Result.fail("You have no account yet.");
		}
		if (amount <= 0 || amount > MAX_AMOUNT) {
			return Result.fail("Choose an amount to borrow.");
		}
		if (overdue(id, day)) {
			return Result.fail("Your loan is overdue: repay it before you borrow again.");
		}
		long limit = creditLimit(id);
		if (account.principal + amount > limit) {
			return Result.fail("That passes your credit limit of " + format(limit) + ".");
		}
		if (amount > reserve) {
			return Result.fail("The central bank holds only " + format(reserve) + ".");
		}
		long fee = fee(amount);
		reserve -= amount;
		give(id, amount);
		account.debt += amount + fee;
		account.principal += amount;
		if (account.dueDay < 0) {
			account.dueDay = day + termDays;
			account.lateCharged = false;
		}
		record(day, id, null, account.name + " borrowed " + format(amount) + " from the central bank (fee " + format(fee)
				+ "); " + format(account.debt) + " due by day " + account.dueDay + ".");
		return new Result(true, "Borrowed " + format(amount) + ". Repay " + format(account.debt) + " by day " + account.dueDay + ".");
	}

	/** The one-time starter loan: the base credit limit, on the usual terms. */
	public Result starterLoan(UUID id, long day) {
		Account account = accounts.get(id);
		if (account == null) {
			return Result.fail("You have no account yet.");
		}
		if (account.starterTaken) {
			return Result.fail("You have already had your starter loan.");
		}
		if (account.debt > 0) {
			return Result.fail("Repay your current loan first.");
		}
		Result result = borrow(id, borrowLimit, day);
		if (result.ok()) {
			account.starterTaken = true;
		}
		return result;
	}

	public Result repay(UUID id, long amount, long day) {
		Account account = accounts.get(id);
		if (account == null || account.debt <= 0) {
			return Result.fail("You owe the central bank nothing.");
		}
		long paid = Math.min(Math.min(amount, account.debt), cash(id));
		if (amount <= 0 || paid <= 0) {
			return Result.fail(cash(id) <= 0 ? "You have no Jugs to repay with." : "Choose an amount to repay.");
		}
		take(id, paid);
		applyRepayment(account, paid, day, "repaid");
		return new Result(true, "Repaid " + format(paid) + ".");
	}

	/** Takes {@code paid} (already removed from the player) off their debt into the reserve, and settles a payoff. */
	private void applyRepayment(Account account, long paid, long day, String verb) {
		long before = account.debt;
		account.debt -= paid;
		reserve += paid;
		// The borrowed part shrinks in step with the whole, so fees and principal are repaid together.
		account.principal = before <= 0 ? 0 : Math.max(0, account.principal - (paid * account.principal + before - 1) / before);
		if (account.debt > 0) {
			record(day, account.id, null, account.name + " " + verb + " " + format(paid) + " to the central bank; "
					+ format(account.debt) + " still owed.");
			return;
		}
		account.principal = 0;
		boolean onTime = !account.lateCharged && account.dueDay >= 0 && day <= account.dueDay;
		account.dueDay = -1;
		account.lateCharged = false;
		if (onTime && account.creditTier < creditSteps()) {
			account.creditTier++;
			record(day, account.id, null, account.name + " " + verb + " " + format(paid) + " and paid off their loan on time: "
					+ "credit limit raised to " + format(creditLimit(account.id)) + ".");
		} else {
			record(day, account.id, null, account.name + " " + verb + " " + format(paid) + " and is debt-free.");
		}
	}

	/**
	 * A player earned {@code amount} on the market (already in their balance): while they owe the bank, their share
	 * goes straight to the loan.
	 */
	private void repayFromEarnings(Account account, long amount, long day) {
		if (account == null || account.debt <= 0 || amount <= 0) {
			return;
		}
		long part = Math.min(Math.min(account.debt, cash(account.id)), amount * shareFor(account.id, day) / 10_000);
		if (part > 0 && take(account.id, part)) {
			applyRepayment(account, part, day, "put " + rateText(shareFor(account.id, day)) + " of a sale,");
		}
	}

	private Result overdueBlock(UUID id, long day) {
		return overdue(id, day) ? Result.fail("Your loan is overdue: you can sell, but not buy, bid or pay, until it is repaid.") : null;
	}

	/**
	 * Signs a contract and makes its first payment now. {@link Period#ONCE} is a plain transfer; the others repeat
	 * every period from today. The note says what the payer gets in return; it is a promise, not enforced.
	 */
	public Result pay(UUID payerId, String payeeName, long amount, Period period, String note, long day) {
		Account payer = accounts.get(payerId);
		Account payee = payeeName == null ? null : findByName(payeeName);
		if (payer == null) {
			return Result.fail("You have no account yet.");
		}
		if (payee == null) {
			return Result.fail("No account for \"" + clean(payeeName == null ? "" : payeeName, 16) + "\". They must have joined this server.");
		}
		if (payee == payer) {
			return Result.fail("You cannot pay yourself.");
		}
		if (amount <= 0 || amount > MAX_AMOUNT) {
			return Result.fail("Choose an amount to pay.");
		}
		Result blocked = overdueBlock(payerId, day);
		if (blocked != null) {
			return blocked;
		}
		if (cash(payerId) < amount) {
			return Result.fail("You hold only " + format(cash(payerId)) + ".");
		}
		String terms = clean(note, MAX_NOTE);
		take(payerId, amount);
		give(payee.id, amount);
		String in = terms.isEmpty() ? "" : " for \"" + terms + "\"";
		if (period == Period.ONCE) {
			record(day, payer.id, payee.id, payer.name + " paid " + payee.name + " " + format(amount) + in + ".");
			return new Result(true, "Paid " + payee.name + " " + format(amount) + ".");
		}
		Contract contract = new Contract(nextContractId++, payer.id, payee.id, amount, period, terms, day + period.days, true);
		contracts.add(contract);
		record(day, payer.id, payee.id, "Contract #" + contract.id + ": " + payer.name + " pays " + payee.name + " "
				+ format(amount) + " " + period.name().toLowerCase(Locale.ROOT) + in + ". First payment made.");
		return new Result(true, "Contract #" + contract.id + " signed.");
	}

	/** Either party may end a recurring contract. */
	public Result cancel(UUID actor, long contractId, long day) {
		for (Contract contract : contracts) {
			if (contract.id == contractId && contract.active && (actor.equals(contract.payer) || actor.equals(contract.payee))) {
				contract.active = false;
				record(day, contract.payer, contract.payee, "Contract #" + contract.id + " was cancelled by " + nameOf(actor) + ".");
				prune();
				return new Result(true, "Contract #" + contract.id + " cancelled.");
			}
		}
		return Result.fail("No such active contract of yours.");
	}

	// ------------------------------------------------------------------ markets

	/** Moves {@code amount} from the buyer to the seller (a market sale). Fails without changing anything when short. */
	public Result trade(UUID buyerId, UUID sellerId, long amount, long day, String text) {
		Account buyer = accounts.get(buyerId);
		Account seller = accounts.get(sellerId);
		if (buyer == null || seller == null) {
			return Result.fail("That account no longer exists.");
		}
		if (amount <= 0 || amount > MAX_AMOUNT) {
			return Result.fail("That price is out of range.");
		}
		Result blocked = overdueBlock(buyerId, day);
		if (blocked != null) {
			return blocked;
		}
		if (!take(buyerId, amount)) {
			return Result.fail("You hold only " + format(cash(buyerId)) + ".");
		}
		give(sellerId, amount);
		record(day, buyerId, sellerId, text);
		repayFromEarnings(seller, amount, day);
		return new Result(true, text);
	}

	/** Sets aside {@code amount} of the player's money (a bid or a buy request). */
	public Result hold(UUID id, long amount, long day, String text) {
		Account account = accounts.get(id);
		if (account == null) {
			return Result.fail("You have no account yet.");
		}
		if (amount <= 0 || amount > MAX_AMOUNT) {
			return Result.fail("That amount is out of range.");
		}
		Result blocked = overdueBlock(id, day);
		if (blocked != null) {
			return blocked;
		}
		if (!take(id, amount)) {
			return Result.fail("You hold only " + format(cash(id)) + ".");
		}
		escrow += amount;
		record(day, id, null, text);
		return new Result(true, text);
	}

	/** Gives held money back to its owner (outbid, request cancelled). {@code text} may be null for no ledger line. */
	public void release(UUID id, long amount, long day, String text) {
		long back = Math.max(0, Math.min(amount, escrow));
		escrow -= back;
		give(id, back);
		if (text != null) {
			record(day, id, null, text);
		}
	}

	/** Pays held money to someone (an auction won, a request filled). */
	public void settle(UUID from, UUID to, long amount, long day, String text) {
		long paid = Math.max(0, Math.min(amount, escrow));
		escrow -= paid;
		give(to, paid);
		record(day, from, to, text);
		repayFromEarnings(accounts.get(to), paid, day);
	}

	/** Pays the central bank (land from the land office, a corporation charter): the money goes to the reserve. */
	public Result payBank(UUID id, long amount, long day, String text) {
		Account account = accounts.get(id);
		if (account == null) {
			return Result.fail("You have no account yet.");
		}
		if (amount < 0 || amount > MAX_AMOUNT) {
			return Result.fail("That price is out of range.");
		}
		Result blocked = overdueBlock(id, day);
		if (blocked != null) {
			return blocked;
		}
		if (!take(id, amount)) {
			return Result.fail("That costs " + format(amount) + "; you hold " + format(cash(id)) + ".");
		}
		reserve += amount;
		record(day, id, null, text);
		return new Result(true, text);
	}

	/**
	 * Takes a tax of up to {@code amount} from {@code id} into the reserve (a market sales tax): never more than they
	 * hold, and with no overdue check, since it follows a sale they already made. Returns what was taken.
	 */
	public long tax(UUID id, long amount, long day, String text) {
		long due = Math.min(Math.max(0, amount), cash(id));
		if (due <= 0 || !take(id, due)) {
			return 0;
		}
		reserve += due;
		if (text != null) {
			record(day, id, null, text);
		}
		return due;
	}


    /** Unpaid winnings are liabilities, not spendable currency. Never discard a winner's entitlement. */
    public record PrizeDebt(UUID house, UUID player, long amount) { }
    private final List<PrizeDebt> prizeDebts = new ArrayList<>();
    public List<PrizeDebt> prizeDebts() { return List.copyOf(prizeDebts); }
    void restorePrizeDebts(List<PrizeDebt> debts) {
        for (var debt : debts) if (debt.amount() > 0) prizeDebts.add(debt);
    }
    private boolean owesPrizes(UUID house) {
        return prizeDebts.stream().anyMatch(d -> java.util.Objects.equals(d.house(), house));
    }
    public long bankGameCover() { return owesPrizes(null) ? 0 : reserve; }
    private void owePrize(UUID house, UUID player, long amount, long day) {
        if (amount <= 0) return;
        for (int i=0;i<prizeDebts.size();i++) {
            var d=prizeDebts.get(i);
            if (java.util.Objects.equals(house,d.house()) && player.equals(d.player())) {
                prizeDebts.set(i,new PrizeDebt(house,player,Math.addExact(d.amount(),amount)));
                loud.add(record(day,player,house,format(amount)+" winnings are awaiting house funding; your claim is saved and will be paid automatically."));
                return;
            }
        }
        prizeDebts.add(new PrizeDebt(house,player,amount));
        loud.add(record(day,player,house,format(amount)+" winnings are awaiting house funding; your claim is saved and will be paid automatically."));
    }
    /** Bounded retry by the server, including after loading a save. Only moves existing money. */
    public boolean payPrizeDebts(long day) {
        boolean changed=false;
        int budget=Math.min(64,prizeDebts.size());
        for (int checked=0;checked<budget;checked++) {
            var d=prizeDebts.removeFirst();long paid=0;
            if (d.house()!=null) {
                long cashPayment=Math.min(d.amount(),Math.max(0,cash(d.house())));
                if(cashPayment>0 && take(d.house(),cashPayment)) paid=cashPayment;
                paid+=houseLoan(d.house(),d.amount()-paid,day);
            } else {
                paid=Math.min(d.amount(),reserve);reserve-=paid;
            }
            if(paid>0) {
                give(d.player(),paid);changed=true;
                loud.add(record(day,d.player(),d.house(),format(paid)+" of saved winnings has now been paid."));
            }
            if(paid<d.amount()) prizeDebts.add(new PrizeDebt(d.house(),d.player(),d.amount()-paid));
        }
        return changed;
    }

	// ------------------------------------------------------------------ casino games

	/**
	 * What the house (a casino game's owner) can pay out now: their Jugs plus what the central bank would still lend
	 * them. A game takes no bet that could win more than this.
	 */
	public long houseCover(UUID owner, long day) {
		if (owner == null || accounts.get(owner) == null || owesPrizes(owner)) {
			return 0;
		}
		return cash(owner) + (overdue(owner, day) ? 0 : borrowable(owner));
	}

	/** Sets a player's stake aside for a game, with no chat line (games bet often). */
	public Result stake(UUID id, long amount, long day) {
		Account account = accounts.get(id);
		if (account == null) {
			return Result.fail("You have no account yet.");
		}
		if (amount <= 0 || amount > MAX_AMOUNT) {
			return Result.fail("That bet is out of range.");
		}
		Result blocked = overdueBlock(id, day);
		if (blocked != null) {
			return blocked;
		}
		if (!take(id, amount)) {
			return Result.fail("You hold only " + format(cash(id)) + ".");
		}
		escrow += amount;
		return new Result(true, "");
	}

	/**
	 * Settles a stake. The player gets {@code payout} (0 when they lose, the stake back on a push, the stake and their
	 * winnings on a win); what is left of a lost stake goes to the house. A win beyond the stake is paid by the house,
	 * and what the house lacks the central bank pays, as a loan to the house on the usual terms. {@code stake} may be
	 * 0 (a bet that stays up and only collects its winnings). {@code text}, when not null, goes in the ledger (big
	 * wins). Returns what the bank lent.
	 */
	public long settleStake(UUID player, UUID house, long stake, long payout, long day, String text) {
		long held = Math.max(0, Math.min(stake, escrow));
		escrow -= held;
		long lent = 0;
		if (house == null) {
			// The central bank is the house: it keeps what is lost and pays wins from the reserve.
			long out = Math.max(0, payout);
			if (out <= held) {
				reserve += held - out;
				give(player, out);
			} else {
				long win = Math.min(out - held, reserve);
				reserve -= win;
				give(player, held + win);
                owePrize(null,player,out-held-win,day);
			}
			if (text != null) {
				loud.add(record(day, player, null, text));
			}
			return 0;
		}
		if (payout <= held) {
			give(player, Math.max(0, payout));
			give(house, held - Math.max(0, payout));
		} else {
			give(player, held);
			long win = payout - held;
			long fromHouse = Math.min(win, Math.max(0, cash(house)));
			if (fromHouse > 0 && take(house, fromHouse)) {
				give(player, fromHouse);
			} else {
				fromHouse = 0;
			}
			long shortfall = win - fromHouse;
			if (shortfall > 0) {
				lent = houseLoan(house, shortfall, day);
				give(player, lent);
                owePrize(house,player,shortfall-lent,day);
			}
		}
		if (text != null) {
			loud.add(record(day, player, house, text));
		}
		return lent;
	}

	/** Pays held money (a coin pusher's shelf, a lottery pot) to someone, quietly; null pays the reserve. */
	public void escrowTo(UUID to, long amount) {
		long paid = Math.max(0, Math.min(amount, escrow));
		escrow -= paid;
		if (to == null) {
			reserve += paid;
		} else {
			give(to, paid);
		}
	}

	/**
	 * Sells something priced by odds (a scratch card): the buyer pays {@code price}; {@code bankShare} goes to the
	 * reserve, which pays the prizes, and the rest to the house (null: the reserve too).
	 */
	public Result sellChance(UUID buyer, UUID house, long price, long bankShare, long day) {
		Account account = accounts.get(buyer);
		if (account == null) {
			return Result.fail("You have no account yet.");
		}
		Result blocked = overdueBlock(buyer, day);
		if (blocked != null) {
			return blocked;
		}
		if (price <= 0 || !take(buyer, price)) {
			return Result.fail("That costs " + format(price) + "; you hold " + format(cash(buyer)) + ".");
		}
		long bank = Math.max(0, Math.min(price, bankShare));
		reserve += bank;
		if (house == null) {
			reserve += price - bank;
		} else {
			give(house, price - bank);
		}
		return new Result(true, "");
	}

	/** The central bank pays a prize from the reserve (as much as it holds). */
	public long bankPays(UUID to, long amount, long day, String text) {
		long paid = Math.max(0, Math.min(amount, reserve));
		reserve -= paid;
		give(to, paid);
        owePrize(null,to,Math.max(0,amount)-paid,day);
		if (text != null) {
			loud.add(record(day, to, null, text));
		}
		return paid;
	}

	/** Writes a line to the ledger (and the chat), with no money moving. */
	public void announceLine(long day, UUID who, String text) {
		record(day, who, null, text);
	}

	/** Hands a stake back unplayed (a bet withdrawn, a game called off). */
	public void refundStake(UUID id, long amount, long day) {
		release(id, amount, day, null);
	}

	/** The central bank lends a casino's owner what their game must pay out (no credit limit: the bet was checked). */
	private long houseLoan(UUID owner, long amount, long day) {
		Account account = accounts.get(owner);
		long lent = Math.min(amount, reserve);
		if (account == null || lent <= 0) {
			return 0;
		}
		long fee = fee(lent);
		reserve -= lent;
		account.debt += lent + fee;
		account.principal += lent;
		if (account.dueDay < 0) {
			account.dueDay = day + termDays;
			account.lateCharged = false;
		}
		record(day, owner, null, account.name + "'s casino game was short: the central bank lent " + format(lent)
				+ " to pay a winner (fee " + format(fee) + "); " + format(account.debt) + " due by day " + account.dueDay + ".");
		return lent;
	}

	/** Opens (or finds) a corporation's treasury account. */
	public Account treasury(UUID id, String name) {
		Account account = account(id, name);
		account.corporate = true;
		account.starterTaken = true;
		return account;
	}

	/** Moves money between two accounts with no market rules (dividends, treasury payments). */
	public Result transfer(UUID fromId, UUID toId, long amount, long day, String text) {
		Account from = accounts.get(fromId);
		Account to = accounts.get(toId);
		if (from == null || to == null) {
			return Result.fail("That account no longer exists.");
		}
		if (amount <= 0 || !take(fromId, amount)) {
			return Result.fail("Only " + format(cash(fromId)) + " is available.");
		}
		give(toId, amount);
		if (text != null) {
			record(day, fromId, toId, text);
		}
		return new Result(true, "Paid " + format(amount) + ".");
	}

	/** A ledger line with no money moving (a listing posted, an item collected). */
	public void note(long day, UUID first, UUID second, String text) {
		record(day, first, second, text);
	}

	// ------------------------------------------------------------------ time

	/**
	 * Runs every day from the last one seen up to {@code day}: interest on loans, then due contract payments.
	 * Returns how many days were run. The first call only notes the day.
	 */
	public int advanceTo(long day) {
		if (lastDay < 0) {
			lastDay = day;
			return 0;
		}
		if (day <= lastDay) {
			return 0;
		}
		if (day - lastDay > MAX_CATCH_UP_DAYS) {
			lastDay = day - MAX_CATCH_UP_DAYS;
		}
		int run = 0;
		while (lastDay < day) {
			lastDay++;
			runDay(lastDay);
			run++;
		}
		return run;
	}

	private void runDay(long day) {
		for (Account account : accounts.values()) {
			if (account.debt <= 0) {
				continue;
			}
			if (account.dueDay < 0) {
				account.dueDay = day + termDays; // A debt from before due days existed.
			} else if (day > account.dueDay && !account.lateCharged) {
				long fee = percent(account.debt, lateFeeBp());
				account.debt = Math.min(MAX_DEBT, account.debt + fee);
				account.lateCharged = true;
				record(day, account.id, null, account.name + "'s loan is overdue: a late fee of " + format(fee) + " was added ("
						+ format(account.debt) + " owed). Until it is repaid they can sell but not buy, and "
						+ rateText(shareFor(account.id, day)) + " of their sales go to the bank.");
			}
		}
		for (Contract contract : contracts) {
			if (!contract.active || contract.nextDay > day) {
				continue;
			}
			Account payer = accounts.get(contract.payer);
			Account payee = accounts.get(contract.payee);
			if (payer != null && overdue(payer.id, day)) {
				contract.active = false;
				record(day, contract.payer, contract.payee, "Contract #" + contract.id + " lapsed: " + payer.name
						+ "'s loan is overdue.");
				continue;
			}
			if (payer == null || payee == null || !take(payer.id, contract.amount)) {
				contract.active = false;
				record(day, contract.payer, contract.payee, "Contract #" + contract.id + " lapsed: " + nameOf(contract.payer)
						+ " could not pay " + format(contract.amount) + ".");
				continue;
			}
			give(payee.id, contract.amount);
			contract.nextDay = day + contract.period.days;
			record(day, payer.id, payee.id, "Contract #" + contract.id + ": " + payer.name + " paid " + payee.name + " "
					+ format(contract.amount) + ".");
		}
		prune();
	}

	/** {@code basisPoints} of {@code amount}, rounded up to the cent (for screens showing what a loan will cost). */
	public static long percentOf(long amount, int basisPoints) {
		return percent(amount, basisPoints);
	}

	/** {@code basisPoints} of {@code amount}, rounded up to the cent. */
	static long percent(long amount, int basisPoints) {
		long whole = amount / 10_000 * basisPoints;
		long part = (amount % 10_000 * basisPoints + 9_999) / 10_000;
		return whole + part;
	}

	/** Forgets finished contracts beyond the newest 64 so the save stays small. */
	private void prune() {
		long inactive = contracts.stream().filter(contract -> !contract.active).count();
		var it = contracts.iterator();
		while (inactive > 64 && it.hasNext()) {
			if (!it.next().active) {
				it.remove();
				inactive--;
			}
		}
	}

	// ------------------------------------------------------------------ central bank (admin)

	/** Sets the flat loan fee for new loans. */
	public Result setFee(int basisPoints, long day) {
		if (basisPoints < 0 || basisPoints > 10_000) {
			return Result.fail("The fee must be between 0% and 100%.");
		}
		feeBp = basisPoints;
		record(day, null, null, "The central bank set its loan fee to " + rateText(feeBp) + ".");
		return new Result(true, "Loan fee set to " + rateText(feeBp) + ".");
	}

	/** Sets the starting credit limit, which is also the starter loan. */
	public Result setLimit(long amount, long day) {
		if (amount < 0 || amount > MAX_AMOUNT) {
			return Result.fail("Choose a limit between 0 and " + format(MAX_AMOUNT) + ".");
		}
		borrowLimit = amount;
		record(day, null, null, "The central bank set the starting credit limit and starter loan to " + format(amount) + ".");
		return new Result(true, "Starting limit set to " + format(amount) + ".");
	}

	/** Sets how many days new loans run. */
	public Result setTerm(int days, long day) {
		if (days < 1 || days > MAX_TERM_DAYS) {
			return Result.fail("The term must be between 1 and " + MAX_TERM_DAYS + " days.");
		}
		termDays = days;
		record(day, null, null, "The central bank now lends for " + days + " days.");
		return new Result(true, "Loan term set to " + days + " days.");
	}

	/** Sets the share of market earnings that repays a loan. */
	public Result setShare(int basisPoints, long day) {
		if (basisPoints < 0 || basisPoints > 10_000) {
			return Result.fail("The share must be between 0% and 100%.");
		}
		shareBp = basisPoints;
		record(day, null, null, "Borrowers now repay " + rateText(shareBp) + " of their market earnings ("
				+ rateText(Math.min(10_000, shareBp * 2)) + " when overdue).");
		return new Result(true, "Repayment share set to " + rateText(shareBp) + ".");
	}

	/** Cancels some or all of a player's debt (no money moves; the reserve simply never gets it back). */
	public Result forgive(UUID id, long amount, long day) {
		Account account = accounts.get(id);
		if (account == null || account.debt <= 0) {
			return Result.fail("That player owes nothing.");
		}
		long cut = amount <= 0 ? account.debt : Math.min(amount, account.debt);
		long before = account.debt;
		account.debt -= cut;
		account.principal = account.debt == 0 ? 0 : account.principal * account.debt / before;
		if (account.debt == 0) {
			account.dueDay = -1;
			account.lateCharged = false;
		}
		record(day, id, null, "The central bank forgave " + account.name + " " + format(cut) + " of debt"
				+ (account.debt == 0 ? "." : "; " + format(account.debt) + " still owed."));
		return new Result(true, "Forgave " + format(cut) + ".");
	}

	/** Lets a player take the starter loan again (for example after a reset). */
	public Result resetStarter(UUID id, long day) {
		Account account = accounts.get(id);
		if (account == null) {
			return Result.fail("That player has no account yet.");
		}
		account.starterTaken = false;
		record(day, id, null, account.name + " may take a starter loan again.");
		return new Result(true, account.name + " may take a starter loan again.");
	}

	public Result mint(long amount, long day) {
		if (amount <= 0 || amount > MAX_AMOUNT || reserve > MAX_DEBT) {
			return Result.fail("Choose an amount to print.");
		}
		reserve += amount;
		record(day, null, null, "The central bank printed " + format(amount) + " into its reserve.");
		return new Result(true, "Reserve is now " + format(reserve) + ".");
	}

	public Result burn(long amount, long day) {
		if (amount <= 0 || amount > reserve) {
			return Result.fail("The reserve holds " + format(reserve) + ".");
		}
		reserve -= amount;
		record(day, null, null, "The central bank retired " + format(amount) + " from its reserve.");
		return new Result(true, "Reserve is now " + format(reserve) + ".");
	}

	/** Gives money from the reserve to a player, with no debt (a stipend, prize or starting grant). */
	public Result grant(UUID id, long amount, long day) {
		Account account = accounts.get(id);
		if (account == null) {
			return Result.fail("That player has no account yet.");
		}
		if (amount <= 0 || amount > reserve) {
			return Result.fail("The reserve holds " + format(reserve) + ".");
		}
		reserve -= amount;
		give(id, amount);
		record(day, id, null, "The central bank granted " + account.name + " " + format(amount) + ".");
		return new Result(true, "Granted " + format(amount) + " to " + account.name + ".");
	}

	public void setBroadcast(boolean broadcast) {
		this.broadcast = broadcast;
	}

	// ------------------------------------------------------------------ text

	private Entry record(long day, UUID first, UUID second, String text) {
		Entry entry = new Entry(day, first, second, text);
		log.addLast(entry);
		while (log.size() > LOG_SIZE) {
			log.removeFirst();
		}
		pending.add(entry);
		return entry;
	}

	/**
	 * The lines worth telling the server about in chat: big casino wins and lottery jackpots (contributor, 17 October
	 * 2026: "I dont want everything appearing in chat anymore, maybe big wins at the casino thats it"). Every other line
	 * goes only to the history and the server log.
	 */
	private final java.util.Set<Entry> loud = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());

	/** True (once) when {@code entry} is a big win to announce in chat. */
	public boolean takeLoud(Entry entry) {
		return loud.remove(entry);
	}

	/** Writes a big win to the history and marks it for chat. */
	public void shout(long day, UUID who, String text) {
		loud.add(record(day, who, null, text));
	}

	private String nameOf(UUID id) {
		Account account = accounts.get(id);
		return account == null ? "someone" : account.name;
	}

	public String format(long jugs) {
		return format(jugs, CURRENCY);
	}

	/** {@code 1234} → {@code "1,234 Jugs"} ({@code "1 Jug"} for one). */
	public static String format(long jugs, String currency) {
		return amount(jugs) + " " + (Math.abs(jugs) == 1 && currency.endsWith("s") ? currency.substring(0, currency.length() - 1) : currency);
	}

	/** {@code 1234} → {@code "1,234"}. */
	public static String amount(long jugs) {
		return String.format(Locale.ROOT, "%,d", jugs);
	}

	/** {@code 125} → {@code "1.25%"}. */
	public static String rateText(int basisPoints) {
		return String.format(Locale.ROOT, "%d.%02d%%", basisPoints / 100, basisPoints % 100);
	}

	/** Parses what a player typed ({@code "1,250"}) into whole Jugs; -1 when it is not a positive whole amount. */
	public static long parseAmount(String text) {
		if (text == null) {
			return -1;
		}
		String clean = text.trim().replace(",", "").replace("_", "");
		if (clean.endsWith(".0") || clean.endsWith(".00")) {
			clean = clean.substring(0, clean.indexOf('.'));
		}
		if (clean.isEmpty() || !clean.matches("\\d{1,10}")) {
			return -1;
		}
		long jugs = Long.parseLong(clean);
		return jugs > 0 && jugs <= MAX_AMOUNT ? jugs : -1;
	}

	/** Parses a percentage ({@code "1.25"}) into basis points; -1 when invalid. */
	public static int parseRate(String text) {
		if (text == null) {
			return -1;
		}
		String clean = text.trim().replace("%", "");
		if (!clean.matches("\\d{1,3}(\\.\\d{0,2})?")) {
			return -1;
		}
		int dot = clean.indexOf('.');
		int whole = Integer.parseInt(dot < 0 ? clean : clean.substring(0, dot));
		String frac = dot < 0 ? "" : clean.substring(dot + 1);
		int bp = whole * 100 + (frac.isEmpty() ? 0 : Integer.parseInt((frac + "0").substring(0, 2)));
		return bp <= 10_000 ? bp : -1;
	}

	/** Strips control characters and section signs (formatting codes) and trims to {@code max}. */
	public static String clean(String text, int max) {
		if (text == null) {
			return "";
		}
		StringBuilder out = new StringBuilder();
		for (int i = 0; i < text.length() && out.length() < max; i++) {
			char c = text.charAt(i);
			if (c >= ' ' && c != '§' && c != 127) {
				out.append(c);
			}
		}
		return out.toString().trim();
	}

}
