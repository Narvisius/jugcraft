package io.github.jimbozoomer.jugcraft.finance;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.server.MinecraftServer;

/**
 * Every number an admin can change on the Exchange, in one list: the Economy tab's admin Settings page and
 * {@code /jugcraft exchange admin set <setting> <value>} both read it. Older settings keep their own homes (the loan
 * fee in the ledger, the base land price in the land office, the charter fee in the equities book); newer ones are
 * kept by key in the ledger ({@link FinanceBook#setting}).
 */
public final class ExchangeSettings {
	// Keys kept in the ledger.
	public static final String LATE_FEE = "late_fee";
	public static final String CREDIT_GROWTH = "credit_growth";
	public static final String CREDIT_STEPS = "credit_steps";
	public static final String SALES_TAX = "sales_tax";
	public static final String BID_STEP = "bid_step";
	public static final String LISTINGS = "listings_per_player";
	public static final String REQUESTS = "requests_per_player";
	public static final String LAND_GROWTH = "land_growth";
	public static final String LAND_MAX = "land_max_chunks";
	public static final String ISSUE_FEE = "share_issue_fee";
	public static final String COMPANIES_PER_PLAYER = "companies_per_player";
	public static final String MAX_COMPANIES = "max_companies";
	public static final String ORDERS = "orders_per_player";
	public static final String LOGO_APPROVAL = "logo_approval";
	public static final String CHESTS = "market_chests_per_company";
	public static final String CHEST_PRICE = "market_chest_price";
	public static final String POKER_SMALL_BLIND = "poker_small_blind";
	public static final String POKER_BIG_BLIND = "poker_big_blind";
	public static final String POKER_MIN_BUYIN = "poker_min_buyin";
	public static final String POKER_MAX_BUYIN = "poker_max_buyin";
	public static final String POKER_TURN_SECONDS = "poker_turn_seconds";
	public static final String CASINO_MIN_BET = "casino_min_bet";
	public static final String CASINO_MAX_BET = "casino_max_bet";
	public static final String CASINO_BET_SECONDS = "casino_bet_seconds";
	public static final String CASINO_TURN_SECONDS = "casino_turn_seconds";
	public static final String JACKPOT_SEED = "jackpot_seed";
	public static final String LOTTERY_DAYS = "lottery_days";

	/** How a value is shown and typed. PERCENT values are stored in basis points (1.25% = 125). */
	public enum Unit {
		JUGS, PERCENT, DAYS, COUNT, TIMES, TOGGLE
	}

	interface Getter {
		long get(MinecraftServer server, FinanceBook book);
	}

	interface Setter {
		FinanceBook.Result set(MinecraftServer server, FinanceBook book, long value, long day);
	}

	/** One setting: its key (for the command), section, label, unit, default and allowed range. */
	public record Setting(String key, String group, String label, Unit unit, long def, long min, long max, Getter getter,
			Setter setter) {
		public long get(MinecraftServer server) {
			return getter.get(server, Ledger.get(server).book());
		}

		public String show(long value) {
			return ExchangeSettings.show(unit, value);
		}
	}

	private static final List<Setting> ALL = new ArrayList<>();

	static {
		// Bank and loans.
		ALL.add(new Setting("loan_fee", "Bank", "Loan fee", Unit.PERCENT, FinanceBook.DEFAULT_FEE_BP, 0, 10_000,
				(s, b) -> b.feeBp(), (s, b, v, d) -> b.setFee((int) v, d)));
		ALL.add(new Setting("starter_loan", "Bank", "Starter loan and first credit limit", Unit.JUGS, FinanceBook.DEFAULT_LIMIT,
				0, FinanceBook.MAX_AMOUNT, (s, b) -> b.borrowLimit(), (s, b, v, d) -> b.setLimit(v, d)));
		ALL.add(new Setting("loan_term", "Bank", "Days to repay a loan", Unit.DAYS, FinanceBook.DEFAULT_TERM_DAYS, 1,
				FinanceBook.MAX_TERM_DAYS, (s, b) -> b.termDays(), (s, b, v, d) -> b.setTerm((int) v, d)));
		ALL.add(new Setting("sales_share", "Bank", "Share of sales that repays a loan", Unit.PERCENT, FinanceBook.DEFAULT_SHARE_BP,
				0, 10_000, (s, b) -> b.shareBp(), (s, b, v, d) -> b.setShare((int) v, d)));
		ALL.add(stored(LATE_FEE, "Bank", "Late fee on an overdue loan", Unit.PERCENT, FinanceBook.LATE_FEE_BP, 0, 10_000));
		ALL.add(stored(CREDIT_GROWTH, "Bank", "Credit limit grows by (times) per on-time payoff", Unit.TIMES,
				FinanceBook.CREDIT_GROWTH, 1, 10));
		ALL.add(stored(CREDIT_STEPS, "Bank", "Times the credit limit can grow", Unit.COUNT, FinanceBook.MAX_CREDIT_TIER, 0,
				FinanceBook.CREDIT_TIER_CAP));
		ALL.add(new Setting("chat_everyone", "Bank", "Deals shown in everyone's chat", Unit.TOGGLE, 1, 0, 1,
				(s, b) -> b.broadcast() ? 1 : 0, (s, b, v, d) -> {
					b.setBroadcast(v == 1);
					b.note(d, null, null, v == 1 ? "Every Exchange deal now shows in everyone's chat."
							: "Exchange deals now show only to the players in them.");
					return new FinanceBook.Result(true, v == 1 ? "Every deal now shows in everyone's chat."
							: "Deals now show only to the players in them.");
				}));
		// Market.
		ALL.add(stored(SALES_TAX, "Market", "Sales tax on market sales (to the reserve)", Unit.PERCENT, 0, 0, 5_000));
		ALL.add(stored(BID_STEP, "Market", "Each bid must beat the last by", Unit.PERCENT, Market.MIN_RAISE_BP, 0, 10_000));
		ALL.add(stored(LISTINGS, "Market", "Listings per player or company", Unit.COUNT, Market.MAX_LISTINGS_PER_PLAYER, 1, 500));
		ALL.add(stored(REQUESTS, "Market", "Requests per player", Unit.COUNT, Market.MAX_REQUESTS_PER_PLAYER, 1, 500));
		// Land.
		ALL.add(new Setting("land_base_price", "Land", "Base price of a chunk", Unit.JUGS, LandRegistry.DEFAULT_BASE_PRICE, 0,
				FinanceBook.MAX_AMOUNT, (s, b) -> LandRegistry.get(s).basePrice(),
				(s, b, v, d) -> LandRegistry.get(s).setBasePrice(b, v, d)));
		ALL.add(stored(LAND_GROWTH, "Land", "Each chunk owned makes the next dearer by", Unit.PERCENT, LandRegistry.GROWTH_BP,
				0, 100_000));
		ALL.add(stored(LAND_MAX, "Land", "Most chunks one owner may hold (0: no limit)", Unit.COUNT, 0, 0, 100_000));
		// Companies.
		ALL.add(new Setting("charter_fee", "Companies", "Charter fee", Unit.JUGS, Equities.DEFAULT_CHARTER_FEE, 0,
				FinanceBook.MAX_AMOUNT, (s, b) -> Equities.get(s).charterFee(),
				(s, b, v, d) -> Equities.get(s).setCharterFee(b, v, d)));
		ALL.add(stored(ISSUE_FEE, "Companies", "Fee to issue new shares", Unit.JUGS, 0, 0, FinanceBook.MAX_AMOUNT));
		ALL.add(stored(COMPANIES_PER_PLAYER, "Companies", "Companies one player may found", Unit.COUNT,
				Equities.MAX_CORPS_PER_PLAYER, 0, 100));
		ALL.add(stored(MAX_COMPANIES, "Companies", "Companies listed on the Exchange", Unit.COUNT, Equities.MAX_CORPS, 1,
				Equities.HARD_MAX_CORPS));
		ALL.add(stored(ORDERS, "Companies", "Open share orders per player", Unit.COUNT, Equities.MAX_ORDERS_PER_PLAYER, 1, 500));
		ALL.add(stored(LOGO_APPROVAL, "Companies", "Uploaded logos need an admin's approval", Unit.TOGGLE, 1, 0, 1));
		ALL.add(stored(CHESTS, "Companies", "Market Chests per company", Unit.COUNT, 4, 0, 100));
		ALL.add(stored(CHEST_PRICE, "Companies", "Price of a Market Chest", Unit.JUGS, 200, 0, FinanceBook.MAX_AMOUNT));
		// Games.
		ALL.add(stored(POKER_SMALL_BLIND, "Games", "Poker small blind", Unit.JUGS, 1, 1, 100_000));
		ALL.add(stored(POKER_BIG_BLIND, "Games", "Poker big blind", Unit.JUGS, 2, 1, 100_000));
		ALL.add(stored(POKER_MIN_BUYIN, "Games", "Least a poker buy-in may be", Unit.JUGS, 40, 1, FinanceBook.MAX_AMOUNT));
		ALL.add(stored(POKER_MAX_BUYIN, "Games", "Most a poker buy-in may be", Unit.JUGS, 400, 1, FinanceBook.MAX_AMOUNT));
		ALL.add(stored(POKER_TURN_SECONDS, "Games", "Seconds each poker player has to act", Unit.COUNT, 30, 10, 300));
		ALL.add(stored(CASINO_MIN_BET, "Games", "Least bet any casino game may take", Unit.JUGS, 1, 1, FinanceBook.MAX_AMOUNT));
		ALL.add(stored(CASINO_MAX_BET, "Games", "Most bet any casino game may take", Unit.JUGS, 1_000, 1, FinanceBook.MAX_AMOUNT));
		ALL.add(stored(CASINO_BET_SECONDS, "Games", "Seconds to bet at blackjack, roulette and craps", Unit.COUNT, 20, 5, 120));
		ALL.add(stored(CASINO_TURN_SECONDS, "Games", "Seconds each blackjack player has to act", Unit.COUNT, 20, 5, 120));
		ALL.add(stored(JACKPOT_SEED, "Games", "Progressive jackpot's starting meter", Unit.JUGS, 500, 0, FinanceBook.MAX_AMOUNT));
		ALL.add(stored(LOTTERY_DAYS, "Games", "Days between lottery draws", Unit.DAYS, 7, 1, 365));
	}

	private ExchangeSettings() {
	}

	private static Setting stored(String key, String group, String label, Unit unit, long def, long min, long max) {
		return new Setting(key, group, label, unit, def, min, max, (s, b) -> b.setting(key, def), (s, b, v, d) -> {
			b.putSetting(key, v);
			b.note(d, null, null, "Admin setting changed: " + label + " is now " + show(unit, v) + ".");
			return new FinanceBook.Result(true, label + ": " + show(unit, v) + ".");
		});
	}

	public static List<Setting> all() {
		return ALL;
	}

	public static Setting find(String key) {
		for (Setting setting : ALL) {
			if (setting.key().equalsIgnoreCase(key == null ? "" : key.trim())) {
				return setting;
			}
		}
		return null;
	}

	/** A setting kept in the ledger, read from a book (for code that has the book but not the server). */
	public static long value(FinanceBook book, String key) {
		Setting setting = find(key);
		return book.setting(key, setting == null ? 0 : setting.def());
	}

	public static String show(Unit unit, long value) {
		return switch (unit) {
			case JUGS -> FinanceBook.format(value, FinanceBook.CURRENCY);
			case PERCENT -> FinanceBook.rateText((int) value);
			case DAYS -> value + (value == 1 ? " day" : " days");
			case COUNT -> FinanceBook.amount(value);
			case TIMES -> "x" + value;
			case TOGGLE -> value == 1 ? "On" : "Off";
		};
	}

	/** What an admin typed, as the stored value; -1 when it cannot be read. */
	public static long parse(Unit unit, String text) {
		String clean = text == null ? "" : text.trim().toLowerCase(Locale.ROOT);
		switch (unit) {
			case PERCENT:
				return FinanceBook.parseRate(clean);
			case TOGGLE:
				return switch (clean) {
					case "on", "yes", "true", "1" -> 1;
					case "off", "no", "false", "0" -> 0;
					default -> -1;
				};
			case JUGS:
				if (clean.equals("0")) {
					return 0;
				}
				return FinanceBook.parseAmount(clean);
			default:
				clean = clean.replace(",", "").replace("x", "");
				if (!clean.matches("\\d{1,9}")) {
					return -1;
				}
				return Long.parseLong(clean);
		}
	}

	/** Changes a setting by key from typed text, checking its range; writes a ledger line. */
	public static FinanceBook.Result set(MinecraftServer server, String key, String text, long day) {
		Setting setting = find(key);
		if (setting == null) {
			return FinanceBook.Result.fail("No setting called \"" + FinanceBook.clean(key, 40) + "\".");
		}
		long value = parse(setting.unit(), text);
		if (value < setting.min() || value > setting.max()) {
			return FinanceBook.Result.fail(setting.label() + " must be between " + show(setting.unit(), setting.min()) + " and "
					+ show(setting.unit(), setting.max()) + ".");
		}
		FinanceBook book = Ledger.get(server).book();
		return setting.setter().set(server, book, value, day);
	}
}
