package io.github.jimbozoomer.jugcraft.finance;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * The equities market: corporations, their shares and a bid/ask order book. Saved with the overworld
 * (data/jugcraft_equities.dat).
 *
 * <p>Chartering costs a fee paid to the reserve. The founder chooses how many shares the company has and how much of
 * it to offer to the public at a price: the offered shares go on the order book as the company's own sell order, and
 * what they sell for goes to its treasury, a {@link FinanceBook} account. Players trade shares with limit orders: a
 * buy order holds its money in escrow, a sell order holds its shares; orders that cross trade at the resting order's
 * price. The holder of more than half the shares controls the company: they pay dividends from the treasury (split by
 * shares held), change the logo and manage its land. Logos are 16x16 pixels in a 64-colour palette; an uploaded logo
 * is hidden until an operator approves it, a drawn one shows at once.
 */
public final class Equities extends SavedData {
	public static final long DEFAULT_CHARTER_FEE = 250L; // Jugs
	public static final int MAX_CORPS_PER_PLAYER = 3;
	public static final int MAX_CORPS = 32;
	/** The most companies the Exchange can list (the screen shows this many). */
	public static final int HARD_MAX_CORPS = 32;
	public static final long MAX_SHARES = 1_000_000L;
	public static final int MAX_ORDERS_PER_PLAYER = 20;
	public static final int LOGO_PIXELS = 256;
	/** Logo palette index meaning a transparent pixel; 0..63 are {@link #paletteColor(int)}. */
	public static final byte CLEAR = 64;

	/** A corporation. {@code treasury} is its account in the ledger. */
	public static final class Corporation {
		public final long id;
		public final String name;
		public final String ticker;
		public final UUID founder;
		public final UUID treasury;
		public long totalShares;
		public final Map<UUID, Long> holdings;
		public byte[] logo;
		public boolean logoUploaded;
		public boolean logoApproved;
		public long lastPrice;
		public long lastDividend;
		public long lastDividendDay;

		Corporation(long id, String name, String ticker, UUID founder, UUID treasury, long totalShares, Map<UUID, Long> holdings,
				byte[] logo, boolean logoUploaded, boolean logoApproved, long lastPrice, long lastDividend, long lastDividendDay) {
			this.id = id;
			this.name = name;
			this.ticker = ticker;
			this.founder = founder;
			this.treasury = treasury;
			this.totalShares = totalShares;
			this.holdings = new HashMap<UUID, Long>(holdings);
			this.logo = cleanLogo(logo);
			this.logoUploaded = logoUploaded;
			this.logoApproved = logoApproved;
			this.lastPrice = lastPrice;
			this.lastDividend = lastDividend;
			this.lastDividendDay = lastDividendDay;
		}

		/** The logo others may see: a drawn logo, or an approved upload; otherwise blank. */
		public byte[] visibleLogo() {
			return visibleLogo(true);
		}

		/** As {@link #visibleLogo()}; with {@code approvalNeeded} false an upload shows at once. */
		public byte[] visibleLogo(boolean approvalNeeded) {
			if (!logoUploaded || logoApproved || !approvalNeeded) {
				return logo;
			}
			byte[] blank = new byte[LOGO_PIXELS];
			java.util.Arrays.fill(blank, CLEAR);
			return blank;
		}
	}

	/** A resting limit order. Buy orders hold {@code held} money in escrow; sell orders hold their shares. */
	public static final class Order {
		public final long id;
		public final long corp;
		public final UUID owner;
		public final String ownerName;
		public final boolean buy;
		public final long price;
		public long quantity;
		public long held;

		Order(long id, long corp, UUID owner, String ownerName, boolean buy, long price, long quantity, long held) {
			this.id = id;
			this.corp = corp;
			this.owner = owner;
			this.ownerName = ownerName;
			this.buy = buy;
			this.price = price;
			this.quantity = quantity;
			this.held = held;
		}
	}

	private static final Codec<UUID> UUID_STRING = Codec.STRING.xmap(UUID::fromString, UUID::toString);
	private static final Codec<byte[]> LOGO = Codec.INT.listOf().xmap(list -> {
		byte[] out = new byte[list.size()];
		for (int i = 0; i < out.length; i++) {
			out[i] = (byte) (int) list.get(i);
		}
		return out;
	}, bytes -> {
		List<Integer> out = new ArrayList<Integer>(bytes.length);
		for (byte b : bytes) {
			out.add((int) b);
		}
		return out;
	});
	private static final Codec<Corporation> CORPORATION = RecordCodecBuilder.create(i -> i.group(
			Codec.LONG.fieldOf("id").forGetter(c -> c.id),
			Codec.STRING.fieldOf("name").forGetter(c -> c.name),
			Codec.STRING.fieldOf("ticker").forGetter(c -> c.ticker),
			UUIDUtil.CODEC.fieldOf("founder").forGetter(c -> c.founder),
			UUIDUtil.CODEC.fieldOf("treasury").forGetter(c -> c.treasury),
			Codec.LONG.fieldOf("shares").forGetter(c -> c.totalShares),
			Codec.unboundedMap(UUID_STRING, Codec.LONG).fieldOf("holdings").forGetter(c -> c.holdings),
			LOGO.fieldOf("logo").forGetter(c -> c.logo),
			Codec.BOOL.optionalFieldOf("logo_uploaded", false).forGetter(c -> c.logoUploaded),
			Codec.BOOL.optionalFieldOf("logo_approved", false).forGetter(c -> c.logoApproved),
			Codec.LONG.optionalFieldOf("last_price", 0L).forGetter(c -> c.lastPrice),
			Codec.LONG.optionalFieldOf("last_dividend", 0L).forGetter(c -> c.lastDividend),
			Codec.LONG.optionalFieldOf("last_dividend_day", -1L).forGetter(c -> c.lastDividendDay)
	).apply(i, Corporation::new));
	private static final Codec<Order> ORDER = RecordCodecBuilder.create(i -> i.group(
			Codec.LONG.fieldOf("id").forGetter(o -> o.id),
			Codec.LONG.fieldOf("corp").forGetter(o -> o.corp),
			UUIDUtil.CODEC.fieldOf("owner").forGetter(o -> o.owner),
			Codec.STRING.fieldOf("owner_name").forGetter(o -> o.ownerName),
			Codec.BOOL.fieldOf("buy").forGetter(o -> o.buy),
			Codec.LONG.fieldOf("price").forGetter(o -> o.price),
			Codec.LONG.fieldOf("quantity").forGetter(o -> o.quantity),
			Codec.LONG.optionalFieldOf("held", 0L).forGetter(o -> o.held)
	).apply(i, Order::new));
	static final Codec<Equities> CODEC = RecordCodecBuilder.create(i -> i.group(
			Codec.LONG.optionalFieldOf("next_id", 1L).forGetter(e -> e.nextId),
			Codec.LONG.optionalFieldOf("charter_fee", DEFAULT_CHARTER_FEE).forGetter(e -> e.charterFee),
			CORPORATION.listOf().optionalFieldOf("corporations", List.of()).forGetter(e -> new ArrayList<Corporation>(e.corps.values())),
			ORDER.listOf().optionalFieldOf("orders", List.of()).forGetter(e -> e.orders)
	).apply(i, Equities::new));
	static final SavedDataType<Equities> TYPE = new SavedDataType<>(Jugcraft.id("equities"), Equities::new, CODEC, null);

	private long nextId;
	private long charterFee;
	private final Map<Long, Corporation> corps = new LinkedHashMap<Long, Corporation>();
	private final List<Order> orders = new ArrayList<Order>();

	Equities() {
		this(1L, DEFAULT_CHARTER_FEE, List.<Corporation>of(), List.<Order>of());
	}

	Equities(long nextId, long charterFee, List<Corporation> corps, List<Order> orders) {
		this.nextId = Math.max(1, nextId);
		this.charterFee = Math.max(0, charterFee);
		for (Corporation corp : corps) {
			this.corps.put(corp.id, corp);
		}
		this.orders.addAll(orders);
	}

	public static Equities get(MinecraftServer server) {
		return server.overworld().getDataStorage().computeIfAbsent(TYPE);
	}

	// ------------------------------------------------------------------ logos

	/** The 64-colour logo palette: four levels of red, green and blue. */
	public static int paletteColor(int index) {
		int r = (index >> 4) & 3;
		int g = (index >> 2) & 3;
		int b = index & 3;
		return 0xFF000000 | (r * 85 << 16) | (g * 85 << 8) | b * 85;
	}

	/** The palette index nearest an ARGB colour (CLEAR when mostly transparent). */
	public static byte nearest(int argb) {
		if ((argb >>> 24) < 128) {
			return CLEAR;
		}
		int r = Math.round(((argb >> 16) & 0xFF) / 85.0F);
		int g = Math.round(((argb >> 8) & 0xFF) / 85.0F);
		int b = Math.round((argb & 0xFF) / 85.0F);
		return (byte) (r << 4 | g << 2 | b);
	}

	static byte[] cleanLogo(byte[] logo) {
		byte[] out = new byte[LOGO_PIXELS];
		for (int i = 0; i < LOGO_PIXELS; i++) {
			byte v = logo != null && i < logo.length ? logo[i] : CLEAR;
			out[i] = v >= 0 && v <= CLEAR ? v : CLEAR;
		}
		return out;
	}

	// ------------------------------------------------------------------ reading

	public long charterFee() {
		return charterFee;
	}

	public java.util.Collection<Corporation> corporations() {
		return corps.values();
	}

	public Corporation corp(long id) {
		return corps.get(id);
	}

	public Corporation byTicker(String ticker) {
		for (Corporation corp : corps.values()) {
			if (corp.ticker.equalsIgnoreCase(ticker == null ? "" : ticker.trim())) {
				return corp;
			}
		}
		return null;
	}

	public Corporation byTreasury(UUID treasury) {
		for (Corporation corp : corps.values()) {
			if (corp.treasury.equals(treasury)) {
				return corp;
			}
		}
		return null;
	}

	public List<Order> orders(long corp) {
		List<Order> out = new ArrayList<Order>();
		for (Order order : orders) {
			if (order.corp == corp) {
				out.add(order);
			}
		}
		return out;
	}

	/** Shares {@code owner} has, counting those in their open sell orders. */
	public long sharesOf(Corporation corp, UUID owner) {
		long shares = corp.holdings.getOrDefault(owner, 0L);
		for (Order order : orders) {
			if (order.corp == corp.id && !order.buy && order.owner.equals(owner)) {
				shares += order.quantity;
			}
		}
		return shares;
	}

	/** Everyone holding shares (including in sell orders), most first. */
	public List<Map.Entry<UUID, Long>> holders(Corporation corp) {
		Map<UUID, Long> all = new HashMap<UUID, Long>();
		corp.holdings.forEach((id, n) -> {
			if (n > 0) {
				all.merge(id, n, Long::sum);
			}
		});
		for (Order order : orders) {
			if (order.corp == corp.id && !order.buy) {
				all.merge(order.owner, order.quantity, Long::sum);
			}
		}
		List<Map.Entry<UUID, Long>> list = new ArrayList<Map.Entry<UUID, Long>>(all.entrySet());
		list.sort(Map.Entry.<UUID, Long>comparingByValue().reversed());
		return list;
	}

	/** The player holding more than half the shares, or null. The company's own unsold shares control nothing. */
	public UUID controller(Corporation corp) {
		for (Map.Entry<UUID, Long> holder : holders(corp)) {
			if (!holder.getKey().equals(corp.treasury) && holder.getValue() * 2 > corp.totalShares) {
				return holder.getKey();
			}
		}
		return null;
	}

	/** True when {@code player} controls the corporation whose treasury is {@code owner}. */
	public boolean controls(UUID player, UUID owner) {
		Corporation corp = byTreasury(owner);
		return corp != null && player.equals(controller(corp));
	}

	/** Corporations {@code player} controls. */
	public List<Corporation> controlledBy(UUID player) {
		List<Corporation> out = new ArrayList<Corporation>();
		for (Corporation corp : corps.values()) {
			if (player.equals(controller(corp))) {
				out.add(corp);
			}
		}
		return out;
	}

	// ------------------------------------------------------------------ chartering

	/**
	 * Charters a corporation: pays the fee, gives the founder every share not offered, and puts the offered shares
	 * ({@code offerBp} of them) on the book at {@code price} each, for the company's treasury.
	 */
	public FinanceBook.Result charter(ServerPlayer player, FinanceBook book, String rawName, String rawTicker, long shares,
			int offerBp, long price, byte[] logo, boolean uploaded, long day) {
		String name = FinanceBook.clean(rawName, 24);
		String ticker = FinanceBook.clean(rawTicker, 5).toUpperCase(Locale.ROOT);
		if (name.length() < 3) {
			return FinanceBook.Result.fail("Give the corporation a name of at least 3 letters.");
		}
		if (!ticker.matches("[A-Z]{2,5}")) {
			return FinanceBook.Result.fail("The ticker must be 2 to 5 letters.");
		}
		if (byTicker(ticker) != null) {
			return FinanceBook.Result.fail("The ticker " + ticker + " is taken.");
		}
		for (Corporation corp : corps.values()) {
			if (corp.name.equalsIgnoreCase(name)) {
				return FinanceBook.Result.fail("A corporation is already called " + name + ".");
			}
		}
		long maxCorps = Math.min(HARD_MAX_CORPS, ExchangeSettings.value(book, ExchangeSettings.MAX_COMPANIES));
		if (corps.size() >= maxCorps) {
			return FinanceBook.Result.fail("The exchange lists " + maxCorps + " corporations already.");
		}
		long perPlayer = ExchangeSettings.value(book, ExchangeSettings.COMPANIES_PER_PLAYER);
		long mine = corps.values().stream().filter(c -> c.founder.equals(player.getUUID())).count();
		if (mine >= perPlayer) {
			return FinanceBook.Result.fail(perPlayer == 0 ? "The admins have closed chartering."
					: "You have founded " + perPlayer + " corporations already.");
		}
		if (shares < 1 || shares > MAX_SHARES) {
			return FinanceBook.Result.fail("Issue between 1 and " + MAX_SHARES + " shares.");
		}
		offerBp = Math.max(0, Math.min(10_000, offerBp));
		long offered = shares * offerBp / 10_000;
		if (offered > 0 && (price <= 0 || price > FinanceBook.MAX_AMOUNT / offered)) {
			return FinanceBook.Result.fail("Set a price per share for the shares you offer.");
		}
		FinanceBook.Result paid = book.payBank(player.getUUID(), charterFee, day, player.getName().getString() + " chartered "
				+ name + " (" + ticker + ") for " + book.format(charterFee) + ".");
		if (!paid.ok()) {
			return paid;
		}
		long id = nextId++;
		UUID treasury = UUID.nameUUIDFromBytes(("jugcraft:corporation:" + id).getBytes(StandardCharsets.UTF_8));
		book.treasury(treasury, ticker);
		Map<UUID, Long> holdings = new HashMap<UUID, Long>();
		if (shares - offered > 0) {
			holdings.put(player.getUUID(), shares - offered);
		}
		Corporation corp = new Corporation(id, name, ticker, player.getUUID(), treasury, shares, holdings, logo, uploaded,
				false, offered > 0 ? price : 0, 0, -1);
		corps.put(id, corp);
		if (offered > 0) {
			orders.add(new Order(nextId++, id, treasury, ticker, false, price, offered, 0));
			book.note(day, player.getUUID(), null, ticker + " offers " + offered + " of its " + shares + " shares at "
					+ book.format(price) + " each.");
		}
		setDirty();
		return new FinanceBook.Result(true, name + " is chartered" + (uploaded ? "; its logo waits for an admin's approval." : "."));
	}

	// ------------------------------------------------------------------ trading

	/** Places a limit order and matches it against the book. */
	public FinanceBook.Result order(ServerPlayer player, FinanceBook book, long corpId, boolean buy, long quantity, long price,
			long day) {
		Corporation corp = corps.get(corpId);
		if (corp == null) {
			return FinanceBook.Result.fail("That corporation is gone.");
		}
		if (quantity <= 0 || quantity > corp.totalShares) {
			return FinanceBook.Result.fail("Choose how many shares.");
		}
		if (price <= 0 || price > FinanceBook.MAX_AMOUNT / quantity) {
			return FinanceBook.Result.fail("Choose a price per share.");
		}
		UUID id = player.getUUID();
		long maxOrders = ExchangeSettings.value(book, ExchangeSettings.ORDERS);
		long open = orders.stream().filter(o -> o.owner.equals(id)).count();
		if (open >= maxOrders) {
			return FinanceBook.Result.fail("You have " + maxOrders + " open orders already.");
		}
		String name = player.getName().getString();
		Order order;
		if (buy) {
			FinanceBook.Result held = book.hold(id, quantity * price, day, name + " bids for " + quantity + " " + corp.ticker
					+ " at " + book.format(price) + " each.");
			if (!held.ok()) {
				return held;
			}
			order = new Order(nextId++, corpId, id, name, true, price, quantity, quantity * price);
		} else {
			long have = corp.holdings.getOrDefault(id, 0L);
			if (have < quantity) {
				return FinanceBook.Result.fail("You hold " + have + " " + corp.ticker + " shares outside your orders.");
			}
			corp.holdings.put(id, have - quantity);
			order = new Order(nextId++, corpId, id, name, false, price, quantity, 0);
			book.note(day, id, null, name + " offers " + quantity + " " + corp.ticker + " at " + book.format(price) + " each.");
		}
		long filled = match(corp, order, book, day);
		if (order.quantity > 0) {
			orders.add(order);
		}
		cleanHoldings(corp);
		setDirty();
		String verb = buy ? "Bought " : "Sold ";
		if (filled == quantity) {
			return new FinanceBook.Result(true, verb + filled + " " + corp.ticker + ".");
		}
		return new FinanceBook.Result(true, (filled > 0 ? verb + filled + "; " : "") + order.quantity + " wait on the book.");
	}

	/** Trades {@code incoming} against resting orders at their prices; returns the shares traded. */
	private long match(Corporation corp, Order incoming, FinanceBook book, long day) {
		List<Order> resting = new ArrayList<Order>();
		for (Order order : orders) {
			if (order.corp == corp.id && order.buy != incoming.buy && !order.owner.equals(incoming.owner)) {
				resting.add(order);
			}
		}
		resting.sort(incoming.buy
				? Comparator.comparingLong((Order o) -> o.price).thenComparingLong(o -> o.id)
				: Comparator.comparingLong((Order o) -> -o.price).thenComparingLong(o -> o.id));
		long traded = 0;
		for (Order rest : resting) {
			if (incoming.quantity <= 0) {
				break;
			}
			boolean crosses = incoming.buy ? rest.price <= incoming.price : rest.price >= incoming.price;
			if (!crosses) {
				break;
			}
			long fill = Math.min(incoming.quantity, rest.quantity);
			long cost = fill * rest.price;
			Order buyer = incoming.buy ? incoming : rest;
			Order seller = incoming.buy ? rest : incoming;
			book.settle(buyer.owner, seller.owner, cost, day, buyer.ownerName + " bought " + fill + " " + corp.ticker + " from "
					+ seller.ownerName + " at " + book.format(rest.price) + " each.");
			buyer.held -= cost;
			corp.holdings.merge(buyer.owner, fill, Long::sum);
			incoming.quantity -= fill;
			rest.quantity -= fill;
			corp.lastPrice = rest.price;
			traded += fill;
			if (rest.quantity <= 0) {
				orders.remove(rest);
				if (rest.buy && rest.held > 0) {
					book.release(rest.owner, rest.held, day, null);
					rest.held = 0;
				}
			}
		}
		if (incoming.buy) {
			// Bought cheaper than the limit: return the difference now; a finished order returns the rest.
			long keep = incoming.quantity * incoming.price;
			if (incoming.held > keep) {
				book.release(incoming.owner, incoming.held - keep, day, null);
				incoming.held = keep;
			}
		}
		return traded;
	}

	private static void cleanHoldings(Corporation corp) {
		corp.holdings.values().removeIf(n -> n <= 0);
	}

	/** Cancels an order: money or shares go back. The controller may cancel the company's own orders; operators any. */
	public FinanceBook.Result cancel(ServerPlayer player, FinanceBook book, long orderId, boolean admin, long day) {
		Order order = null;
		for (Order o : orders) {
			if (o.id == orderId) {
				order = o;
			}
		}
		if (order == null) {
			return FinanceBook.Result.fail("That order is gone.");
		}
		Corporation corp = corps.get(order.corp);
		boolean companyOrder = corp != null && order.owner.equals(corp.treasury) && player.getUUID().equals(controller(corp));
		if (!admin && !order.owner.equals(player.getUUID()) && !companyOrder) {
			return FinanceBook.Result.fail("That is not your order.");
		}
		orders.remove(order);
		if (order.buy) {
			book.release(order.owner, order.held, day, order.ownerName + " withdrew a bid for " + order.quantity + " "
					+ (corp == null ? "shares" : corp.ticker) + ".");
		} else if (corp != null) {
			corp.holdings.merge(order.owner, order.quantity, Long::sum);
			book.note(day, order.owner, null, order.ownerName + " withdrew an offer of " + order.quantity + " " + corp.ticker + ".");
		}
		setDirty();
		return new FinanceBook.Result(true, "Order withdrawn.");
	}

	/**
	 * The controller issues {@code quantity} new shares at {@code price} each: they go on the book as the company's own
	 * sell order, so what they sell for goes to the treasury. Every holder's share of the company shrinks (dilution).
	 * The admins' issue fee, if any, is paid to the reserve by the controller.
	 */
	public FinanceBook.Result issue(ServerPlayer player, FinanceBook book, long corpId, long quantity, long price, long day) {
		Corporation corp = corps.get(corpId);
		if (corp == null || !player.getUUID().equals(controller(corp))) {
			return FinanceBook.Result.fail("Only the holder of most of the shares issues new ones.");
		}
		if (quantity <= 0 || corp.totalShares + quantity > MAX_SHARES) {
			return FinanceBook.Result.fail("A company may have at most " + FinanceBook.amount(MAX_SHARES) + " shares; it has "
					+ FinanceBook.amount(corp.totalShares) + ".");
		}
		if (price <= 0 || price > FinanceBook.MAX_AMOUNT / quantity) {
			return FinanceBook.Result.fail("Set a price per new share.");
		}
		long fee = ExchangeSettings.value(book, ExchangeSettings.ISSUE_FEE);
		String name = player.getName().getString();
		if (fee > 0) {
			FinanceBook.Result paid = book.payBank(player.getUUID(), fee, day, name + " paid " + book.format(fee)
					+ " to issue " + corp.ticker + " shares.");
			if (!paid.ok()) {
				return paid;
			}
		}
		corp.totalShares += quantity;
		orders.add(new Order(nextId++, corp.id, corp.treasury, corp.ticker, false, price, quantity, 0));
		setDirty();
		book.note(day, player.getUUID(), null, corp.ticker + " issued " + FinanceBook.amount(quantity) + " new shares at "
				+ book.format(price) + " each (" + FinanceBook.amount(corp.totalShares) + " in all).");
		return new FinanceBook.Result(true, "Issued " + FinanceBook.amount(quantity) + " shares; they are on the book for "
				+ corp.ticker + "'s treasury.");
	}

	/** The controller pays {@code amount} from the treasury, split among shareholders by shares held. */
	public FinanceBook.Result dividend(ServerPlayer player, FinanceBook book, long corpId, long amount, long day) {
		Corporation corp = corps.get(corpId);
		if (corp == null || !player.getUUID().equals(controller(corp))) {
			return FinanceBook.Result.fail("Only the holder of most of the shares pays dividends.");
		}
		long treasury = book.cash(corp.treasury);
		if (amount <= 0 || amount > treasury) {
			return FinanceBook.Result.fail("The treasury holds " + book.format(treasury) + ".");
		}
		List<Map.Entry<UUID, Long>> holders = new ArrayList<Map.Entry<UUID, Long>>();
		long eligible = 0;
		for (Map.Entry<UUID, Long> holder : holders(corp)) {
			if (!holder.getKey().equals(corp.treasury)) {
				holders.add(holder);
				eligible += holder.getValue();
			}
		}
		if (eligible <= 0) {
			return FinanceBook.Result.fail("Nobody holds shares to pay.");
		}
		long paid = 0;
		for (Map.Entry<UUID, Long> holder : holders) {
			long part = (long) ((double) amount * holder.getValue() / eligible);
			if (part > 0 && book.transfer(corp.treasury, holder.getKey(), part, day, null).ok()) {
				paid += part;
			}
		}
		corp.lastDividend = paid;
		corp.lastDividendDay = day;
		setDirty();
		book.note(day, player.getUUID(), null, corp.ticker + " paid a dividend of " + book.format(paid) + " to its "
				+ holders.size() + " shareholders.");
		return new FinanceBook.Result(true, "Paid " + book.format(paid) + " to " + holders.size() + " shareholders.");
	}

	/** The controller changes the logo; an uploaded one waits for approval. */
	public FinanceBook.Result setLogo(ServerPlayer player, long corpId, byte[] logo, boolean uploaded) {
		Corporation corp = corps.get(corpId);
		if (corp == null || !player.getUUID().equals(controller(corp))) {
			return FinanceBook.Result.fail("Only the holder of most of the shares changes the logo.");
		}
		corp.logo = cleanLogo(logo);
		corp.logoUploaded = uploaded;
		corp.logoApproved = false;
		setDirty();
		return new FinanceBook.Result(true, uploaded ? "Logo uploaded; it shows once an admin approves it." : "Logo changed.");
	}

	public FinanceBook.Result approveLogo(long corpId, boolean approve) {
		Corporation corp = corps.get(corpId);
		if (corp == null) {
			return FinanceBook.Result.fail("No such corporation.");
		}
		if (approve) {
			corp.logoApproved = true;
		} else {
			corp.logo = cleanLogo(null);
			corp.logoUploaded = false;
		}
		setDirty();
		return new FinanceBook.Result(true, corp.ticker + "'s logo was " + (approve ? "approved." : "removed."));
	}

	public FinanceBook.Result setCharterFee(FinanceBook book, long fee, long day) {
		if (fee < 0 || fee > FinanceBook.MAX_AMOUNT) {
			return FinanceBook.Result.fail("Choose a fee.");
		}
		charterFee = fee;
		setDirty();
		book.note(day, null, null, "Chartering a corporation now costs " + book.format(fee) + ".");
		return new FinanceBook.Result(true, "Charter fee set to " + book.format(fee) + ".");
	}
}
