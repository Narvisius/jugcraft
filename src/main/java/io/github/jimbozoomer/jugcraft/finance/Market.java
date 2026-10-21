package io.github.jimbozoomer.jugcraft.finance;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Prediction;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * The commodities market and the requests board, saved with the overworld (data/jugcraft_market.dat).
 *
 * <p>Listed items are held by the Exchange (taken from the seller's inventory when listed), so a buyer gets them at once
 * even when the seller is offline. Bids and paid requests hold the buyer's money in the ledger's escrow. Anything owed
 * to a player who is offline, or whose inventory is full, waits in their collection box. Money always moves through
 * {@link FinanceBook}, so every sale is in the chat ledger and no money is created.
 */
public final class Market extends SavedData {
	public static final int MAX_LISTINGS_PER_PLAYER = 20;
	public static final int MAX_REQUESTS_PER_PLAYER = 10;
	public static final int MAX_QUANTITY = 64 * 36;
	public static final int[] AUCTION_DAYS = {1, 3, 7};
	/** A new bid must beat the last by 5% and by at least one Jug. */
	public static final int MIN_RAISE_BP = 500;

	/** Items for sale. Fixed: {@code price} per item. Auction: {@code price} is the opening bid for the whole lot. */
	public static final class Listing {
		public final long id;
		public final UUID seller;
		public final String sellerName;
		public final ItemStack item;
		public int quantity;
		public final long price;
		public final boolean auction;
		public final long endDay;
		public long bid;
		public UUID bidder;
		public String bidderName;
		/** The company selling it (its treasury is {@link #seller}), or 0 for a player. */
		public final long corp;
		/** The Market Chest it came from ({@link #chestRef}), or empty. */
		public final String chest;

		public Listing(long id, UUID seller, String sellerName, ItemStack item, int quantity, long price, boolean auction,
				long endDay, long bid, Optional<UUID> bidder, String bidderName) {
			this(id, seller, sellerName, item, quantity, price, auction, endDay, bid, bidder, bidderName, 0L, "");
		}

		public Listing(long id, UUID seller, String sellerName, ItemStack item, int quantity, long price, boolean auction,
				long endDay, long bid, Optional<UUID> bidder, String bidderName, long corp, String chest) {
			this.id = id;
			this.seller = seller;
			this.sellerName = sellerName;
			this.item = item.copyWithCount(1);
			this.quantity = quantity;
			this.price = price;
			this.auction = auction;
			this.endDay = endDay;
			this.bid = bid;
			this.bidder = bidder.orElse(null);
			this.bidderName = bidderName;
			this.corp = corp;
			this.chest = chest == null ? "" : chest;
		}

		/** What the lot is worth at its listed price (fixed) or its standing bid (auction). */
		public long value() {
			return auction ? Math.max(bid, price) : price * quantity;
		}
	}

	/** A wanted item. {@code price} per item; 0 means an advertisement with no money held. */
	public static final class Request {
		public final long id;
		public final UUID owner;
		public final String ownerName;
		public final ItemStack item;
		public int quantity;
		public final long price;

		public Request(long id, UUID owner, String ownerName, ItemStack item, int quantity, long price) {
			this.id = id;
			this.owner = owner;
			this.ownerName = ownerName;
			this.item = item.copyWithCount(1);
			this.quantity = quantity;
			this.price = price;
		}
	}

	private static final Codec<UUID> UUID_STRING = Codec.STRING.xmap(UUID::fromString, UUID::toString);

	private static final Codec<Listing> LISTING = RecordCodecBuilder.create(i -> i.group(
			Codec.LONG.fieldOf("id").forGetter(l -> l.id),
			UUIDUtil.CODEC.fieldOf("seller").forGetter(l -> l.seller),
			Codec.STRING.fieldOf("seller_name").forGetter(l -> l.sellerName),
			ItemStack.CODEC.fieldOf("item").forGetter(l -> l.item),
			Codec.INT.fieldOf("quantity").forGetter(l -> l.quantity),
			Codec.LONG.fieldOf("price").forGetter(l -> l.price),
			Codec.BOOL.fieldOf("auction").forGetter(l -> l.auction),
			Codec.LONG.fieldOf("end_day").forGetter(l -> l.endDay),
			Codec.LONG.fieldOf("bid").forGetter(l -> l.bid),
			UUIDUtil.CODEC.optionalFieldOf("bidder").forGetter(l -> Optional.ofNullable(l.bidder)),
			Codec.STRING.optionalFieldOf("bidder_name", "").forGetter(l -> l.bidderName == null ? "" : l.bidderName),
			Codec.LONG.optionalFieldOf("corp", 0L).forGetter(l -> l.corp),
			Codec.STRING.optionalFieldOf("chest", "").forGetter(l -> l.chest)
	).apply(i, Listing::new));

	private static final Codec<Request> REQUEST = RecordCodecBuilder.create(i -> i.group(
			Codec.LONG.fieldOf("id").forGetter(r -> r.id),
			UUIDUtil.CODEC.fieldOf("owner").forGetter(r -> r.owner),
			Codec.STRING.fieldOf("owner_name").forGetter(r -> r.ownerName),
			ItemStack.CODEC.fieldOf("item").forGetter(r -> r.item),
			Codec.INT.fieldOf("quantity").forGetter(r -> r.quantity),
			Codec.LONG.fieldOf("price").forGetter(r -> r.price)
	).apply(i, Request::new));

	static final Codec<Market> CODEC = RecordCodecBuilder.create(i -> i.group(
			Codec.LONG.optionalFieldOf("next_id", 1L).forGetter(m -> m.nextId),
			LISTING.listOf().optionalFieldOf("listings", List.of()).forGetter(m -> m.listings),
			REQUEST.listOf().optionalFieldOf("requests", List.of()).forGetter(m -> m.requests),
			Codec.unboundedMap(UUID_STRING, ItemStack.CODEC.listOf()).optionalFieldOf("collection", Map.of()).forGetter(m -> m.collection),
			Codec.unboundedMap(Codec.STRING, Codec.LONG).optionalFieldOf("last_price", Map.of()).forGetter(m -> m.lastPrice),
			Codec.unboundedMap(Codec.STRING, Codec.LONG).optionalFieldOf("today_value", Map.of()).forGetter(m -> m.todayValue),
			Codec.unboundedMap(Codec.STRING, Codec.LONG).optionalFieldOf("today_quantity", Map.of()).forGetter(m -> m.todayQuantity),
			Codec.INT.optionalFieldOf("inflation_bp", 0).forGetter(m -> m.inflationBp),
			Codec.BOOL.optionalFieldOf("inflation_known", false).forGetter(m -> m.inflationKnown),
			Codec.unboundedMap(Codec.STRING, Codec.LONG).optionalFieldOf("chests", Map.of()).forGetter(m -> m.chests)
	).apply(i, Market::new));
	static final SavedDataType<Market> TYPE = new SavedDataType<>(Jugcraft.id("market"), Market::new, CODEC, null);

	private long nextId;
	private final List<Listing> listings;
	private final List<Request> requests;
	private final Map<UUID, List<ItemStack>> collection;
	/** Average unit price at the last day each item traded, in cents, by item id. */
	private final Map<String, Long> lastPrice;
	private final Map<String, Long> todayValue;
	private final Map<String, Long> todayQuantity;
	private int inflationBp;
	private boolean inflationKnown;
	/** Every Market Chest ({@link #chestRef}) and the company it sells for. */
	private final Map<String, Long> chests;

	Market() {
		this(1L, List.of(), List.of(), Map.<UUID, List<ItemStack>>of(), Map.<String, Long>of(), Map.<String, Long>of(),
				Map.<String, Long>of(), 0, false, Map.<String, Long>of());
	}

	Market(long nextId, List<Listing> listings, List<Request> requests, Map<UUID, List<ItemStack>> collection,
			Map<String, Long> lastPrice, Map<String, Long> todayValue, Map<String, Long> todayQuantity, int inflationBp,
			boolean inflationKnown, Map<String, Long> chests) {
		this.chests = new HashMap<>(chests);
		this.nextId = Math.max(1, nextId);
		this.listings = new ArrayList<>(listings);
		this.requests = new ArrayList<>(requests);
		this.collection = new HashMap<UUID, List<ItemStack>>();
		for (Map.Entry<UUID, List<ItemStack>> entry : collection.entrySet()) {
			this.collection.put(entry.getKey(), new ArrayList<ItemStack>(entry.getValue()));
		}
		this.lastPrice = new HashMap<>(lastPrice);
		this.todayValue = new HashMap<>(todayValue);
		this.todayQuantity = new HashMap<>(todayQuantity);
		this.inflationBp = inflationBp;
		this.inflationKnown = inflationKnown;
	}

	public static Market get(MinecraftServer server) {
		return server.overworld().getDataStorage().computeIfAbsent(TYPE);
	}

	public List<Listing> listings() {
		return listings;
	}

	public List<Request> requests() {
		return requests;
	}

	public int inflationBp() {
		return inflationBp;
	}

	public boolean inflationKnown() {
		return inflationKnown;
	}

	/** Everything listed, at its listed price or standing bid. */
	public long goodsValue() {
		long sum = 0;
		for (Listing listing : listings) {
			sum += listing.value();
		}
		return sum;
	}

	/** How many items wait in {@code id}'s collection box. */
	public int waiting(UUID id) {
		int count = 0;
		for (ItemStack stack : collection.getOrDefault(id, List.of())) {
			count += stack.getCount();
		}
		return count;
	}

	// ------------------------------------------------------------------ items in and out

	private static boolean same(ItemStack a, ItemStack b) {
		return ItemStack.isSameItemSameComponents(a, b);
	}

	/** How many items like {@code template} the player carries. */
	static int carried(ServerPlayer player, ItemStack template, boolean anyComponents) {
		Inventory inventory = player.getInventory();
		int count = 0;
		for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
			ItemStack stack = inventory.getItem(slot);
			if (!stack.isEmpty() && (anyComponents ? stack.is(template.getItem()) : same(stack, template))) {
				count += stack.getCount();
			}
		}
		return count;
	}

	/** Takes {@code count} items like {@code template} from the player; the caller has checked they carry enough. */
	private static void take(ServerPlayer player, ItemStack template, int count, boolean anyComponents) {
		Inventory inventory = player.getInventory();
		int left = count;
		for (int slot = 0; slot < inventory.getContainerSize() && left > 0; slot++) {
			ItemStack stack = inventory.getItem(slot);
			if (!stack.isEmpty() && (anyComponents ? stack.is(template.getItem()) : same(stack, template))) {
				int part = Math.min(left, stack.getCount());
				stack.shrink(part);
				left -= part;
			}
		}
		inventory.setChanged();
	}

	/** Gives {@code count} items like {@code template} to a player: into their inventory if online, else their box. */
	private void give(MinecraftServer server, UUID to, ItemStack template, int count) {
		ServerPlayer player = server.getPlayerList().getPlayer(to);
		int max = Math.max(1, template.getMaxStackSize());
		for (int left = count; left > 0; left -= max) {
			ItemStack stack = template.copyWithCount(Math.min(max, left));
			if (player != null) {
				player.getInventory().add(stack);
			}
			if (!stack.isEmpty()) {
				collection.computeIfAbsent(to, id -> new ArrayList<>()).add(stack);
			}
		}
		setDirty();
	}

	/** Transfers only what fits; uncollected purchases remain saved in the collection box. */
	public FinanceBook.Result collect(ServerPlayer player) {
		List<ItemStack> box = collection.remove(player.getUUID());
		if (box == null || box.isEmpty()) {
			return FinanceBook.Result.fail("Your collection box is empty.");
		}
		int count = 0;
		List<ItemStack> remaining = new ArrayList<>();
		for (ItemStack stack : box) {
			int before = stack.getCount();
			player.getInventory().add(stack);
			count += before - stack.getCount();
			if (!stack.isEmpty()) remaining.add(stack);
		}
		if (!remaining.isEmpty()) collection.put(player.getUUID(), remaining);
		setDirty();
		return new FinanceBook.Result(count > 0, "Collected " + count + " items."
				+ (remaining.isEmpty() ? "" : " Make inventory space to collect the rest."));
	}

	static String itemKey(ItemStack stack) {
		return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
	}

	static String describe(ItemStack stack, int count) {
		return count + " " + stack.getHoverName().getString();
	}

	/** A Market Chest's reference: its dimension and packed block position. */
	public static String chestRef(net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos) {
		return level.dimension().identifier() + "@" + pos.asLong();
	}

	/** True when {@code player} is the seller, or controls the company selling. */
	static boolean sellsIt(MinecraftServer server, UUID player, Listing listing) {
		return listing.seller.equals(player) || (listing.corp > 0 && Equities.get(server).controls(player, listing.seller));
	}

	/** Who gets a company's unsold goods back: the controlling shareholder, else the founder. */
	private static UUID returnTo(MinecraftServer server, Listing listing) {
		if (listing.corp <= 0) {
			return listing.seller;
		}
		Equities equities = Equities.get(server);
		Equities.Corporation corp = equities.corp(listing.corp);
		if (corp == null) {
			return listing.seller;
		}
		UUID controller = equities.controller(corp);
		return controller != null ? controller : corp.founder;
	}

	/** The least the next bid on {@code listing} may be, with the admins' bid step. */
	public static long minimumBid(Listing listing, FinanceBook book) {
		return minimumBid(listing.bid, listing.price, (int) ExchangeSettings.value(book, ExchangeSettings.BID_STEP));
	}

	/** The least the next bid may be: the opening price, or the standing bid raised by {@code stepBp} (at least 1). */
	public static long minimumBid(long bid, long opening, int stepBp) {
		if (bid <= 0) {
			return opening;
		}
		long raise = Math.max(1, bid / 10_000 * stepBp + (bid % 10_000 * stepBp + 9_999) / 10_000);
		return bid + raise;
	}

	/** Takes the admins' sales tax from a seller after a sale; a no-op at 0%. */
	private static void taxSale(FinanceBook book, UUID seller, String sellerName, long value, long day) {
		int bp = (int) ExchangeSettings.value(book, ExchangeSettings.SALES_TAX);
		long tax = FinanceBook.percentOf(value, bp);
		if (tax > 0) {
			book.tax(seller, tax, day, sellerName + " paid " + book.format(tax) + " sales tax.");
		}
	}

	private void recordTrade(ItemStack item, long value, int quantity) {
		if (quantity <= 0) {
			return;
		}
		String key = itemKey(item);
		todayValue.merge(key, value, Long::sum);
		todayQuantity.merge(key, (long) quantity, Long::sum);
	}

	private Listing listing(long id) {
		for (Listing listing : listings) {
			if (listing.id == id) {
				return listing;
			}
		}
		return null;
	}

	private Request request(long id) {
		for (Request request : requests) {
			if (request.id == id) {
				return request;
			}
		}
		return null;
	}

	// ------------------------------------------------------------------ commodities

	/**
	 * Lists the item in the player's main hand: {@code quantity} of it, from anywhere in their inventory. With a
	 * {@code corpId}, the player's company sells it: the company shows as the seller and the money goes to its treasury.
	 */
	public FinanceBook.Result list(ServerPlayer player, FinanceBook book, int quantity, long price, boolean auction,
			int days, long corpId, long day) {
		ItemStack hand = player.getMainHandItem();
		if (hand.isEmpty()) {
			return FinanceBook.Result.fail("Hold the item you want to sell in your main hand.");
		}
		UUID sellerId = player.getUUID();
		String seller = player.getName().getString();
		if (corpId > 0) {
			Equities equities = Equities.get(player.level().getServer());
			Equities.Corporation corp = equities.corp(corpId);
			if (corp == null || !player.getUUID().equals(equities.controller(corp))) {
				return FinanceBook.Result.fail("Only the holder of most of a company's shares sells for it.");
			}
			sellerId = corp.treasury;
			seller = corp.name + " (" + corp.ticker + ")";
		}
		UUID who = sellerId;
		long limit = ExchangeSettings.value(book, ExchangeSettings.LISTINGS);
		long mine = listings.stream().filter(l -> l.seller.equals(who)).count();
		if (mine >= limit) {
			return FinanceBook.Result.fail((corpId > 0 ? "The company has " : "You already have ") + limit + " listings.");
		}
		int have = carried(player, hand, false);
		if (quantity <= 0 || quantity > Math.min(have, MAX_QUANTITY)) {
			return FinanceBook.Result.fail("You carry " + have + " of that item.");
		}
		if (price <= 0 || price > FinanceBook.MAX_AMOUNT / (auction ? 1 : quantity)) {
			return FinanceBook.Result.fail("Choose a price.");
		}
		int length = AUCTION_DAYS[Math.max(0, Math.min(AUCTION_DAYS.length - 1, days))];
		ItemStack template = hand.copyWithCount(1);
		take(player, template, quantity, false);
		Listing listing = new Listing(nextId++, sellerId, seller, template, quantity, price, auction,
				auction ? day + length : 0, 0, Optional.empty(), "", Math.max(0, corpId), "");
		listings.add(listing);
		setDirty();
		String what = describe(template, quantity);
		book.note(day, player.getUUID(), corpId > 0 ? sellerId : null, auction
				? seller + " put " + what + " up for auction from " + book.format(price) + ", closing on day " + listing.endDay + "."
				: seller + " listed " + what + " at " + book.format(price) + " each.");
		return new FinanceBook.Result(true, "Listed " + what + ".");
	}

	public FinanceBook.Result buy(ServerPlayer player, FinanceBook book, long id, int quantity, long day) {
		Listing listing = listing(id);
		if (listing == null || listing.auction) {
			return FinanceBook.Result.fail("That listing is no longer for sale.");
		}
		if (sellsIt(player.level().getServer(), player.getUUID(), listing)) {
			return FinanceBook.Result.fail(listing.corp > 0 ? "That is your company's listing." : "That is your own listing.");
		}
		int count = Math.min(quantity, listing.quantity);
		if (count <= 0) {
			return FinanceBook.Result.fail("Choose how many to buy.");
		}
		long cost = listing.price * count;
		String what = describe(listing.item, count);
		FinanceBook.Result paid = book.trade(player.getUUID(), listing.seller, cost, day,
				player.getName().getString() + " bought " + what + " from " + listing.sellerName + " for " + book.format(cost) + ".");
		if (!paid.ok()) {
			return paid;
		}
		taxSale(book, listing.seller, listing.sellerName, cost, day);
		listing.quantity -= count;
		if (listing.quantity <= 0) {
			listings.remove(listing);
		}
		recordTrade(listing.item, cost, count);
		give(player.level().getServer(), player.getUUID(), listing.item, count);
		return new FinanceBook.Result(true, "Bought " + what + ".");
	}

	public FinanceBook.Result bid(ServerPlayer player, FinanceBook book, long id, long amount, long day) {
		Listing listing = listing(id);
		if (listing == null || !listing.auction || listing.endDay <= day) {
			return FinanceBook.Result.fail("That auction has closed.");
		}
		if (sellsIt(player.level().getServer(), player.getUUID(), listing)) {
			return FinanceBook.Result.fail("You cannot bid on your own auction.");
		}
		if (player.getUUID().equals(listing.bidder)) {
			return FinanceBook.Result.fail("You already hold the top bid.");
		}
		long least = minimumBid(listing, book);
		if (amount < least) {
			return FinanceBook.Result.fail("Bid at least " + book.format(least) + ".");
		}
		String name = player.getName().getString();
		String what = describe(listing.item, listing.quantity);
		FinanceBook.Result held = book.hold(player.getUUID(), amount, day,
				name + " bid " + book.format(amount) + " for " + what + " (auction #" + listing.id + ").");
		if (!held.ok()) {
			return held;
		}
		if (listing.bidder != null) {
			book.release(listing.bidder, listing.bid, day, listing.bidderName + " was outbid on auction #" + listing.id
					+ "; " + book.format(listing.bid) + " returned.");
		}
		listing.bid = amount;
		listing.bidder = player.getUUID();
		listing.bidderName = name;
		setDirty();
		return new FinanceBook.Result(true, "You hold the top bid.");
	}

	/** Takes a listing down and returns its items. Auctions with a bid stay up, except for an admin. */
	public FinanceBook.Result cancelListing(ServerPlayer player, FinanceBook book, long id, boolean admin, long day) {
		Listing listing = listing(id);
		MinecraftServer server = player.level().getServer();
		boolean own = listing != null && sellsIt(server, player.getUUID(), listing);
		if (listing == null || (!admin && !own)) {
			return FinanceBook.Result.fail("No such listing of yours.");
		}
		if (listing.auction && listing.bidder != null) {
			if (!admin) {
				return FinanceBook.Result.fail("An auction with a bid runs to its close.");
			}
			book.release(listing.bidder, listing.bid, day, "The bid of " + listing.bidderName + " on auction #" + listing.id
					+ " was returned.");
		}
		listings.remove(listing);
		// A company's goods go to whoever runs it (the player taking them down, if it is them).
		UUID back = own ? player.getUUID() : returnTo(server, listing);
		give(server, back, listing.item, listing.quantity);
		book.note(day, listing.seller, null, "Listing #" + listing.id + " (" + describe(listing.item, listing.quantity)
				+ ") was taken down" + (admin && !own ? " by an admin." : "."));
		return new FinanceBook.Result(true, "Listing taken down; the items went back to "
				+ (back.equals(player.getUUID()) ? "you" : listing.sellerName) + ".");
	}

	// ------------------------------------------------------------------ requests

	/** Posts a request for an item by id. A price holds price × quantity until the request is filled or cancelled. */
	public FinanceBook.Result postRequest(ServerPlayer player, FinanceBook book, String itemId, int quantity, long price, long day) {
		Identifier key = Identifier.tryParse(itemId == null ? "" : itemId.trim());
		Item item = key == null ? Items.AIR : BuiltInRegistries.ITEM.getValue(key);
		if (item == Items.AIR) {
			return FinanceBook.Result.fail("Pick the item you want.");
		}
		long limit = ExchangeSettings.value(book, ExchangeSettings.REQUESTS);
		long mine = requests.stream().filter(r -> r.owner.equals(player.getUUID())).count();
		if (mine >= limit) {
			return FinanceBook.Result.fail("You already have " + limit + " requests.");
		}
		if (quantity <= 0 || quantity > MAX_QUANTITY) {
			return FinanceBook.Result.fail("Ask for between 1 and " + MAX_QUANTITY + ".");
		}
		if (price < 0 || price > FinanceBook.MAX_AMOUNT / quantity) {
			return FinanceBook.Result.fail("That price is out of range.");
		}
		ItemStack template = new ItemStack(item);
		String name = player.getName().getString();
		String what = describe(template, quantity);
		if (price > 0) {
			FinanceBook.Result held = book.hold(player.getUUID(), price * quantity, day,
					name + " wants " + what + " at " + book.format(price) + " each (" + book.format(price * quantity) + " held).");
			if (!held.ok()) {
				return held;
			}
		} else {
			book.note(day, player.getUUID(), null, name + " is looking for " + what + ".");
		}
		requests.add(new Request(nextId++, player.getUUID(), name, template, quantity, price));
		setDirty();
		return new FinanceBook.Result(true, "Request posted.");
	}

	/** Hands over items to a paid request and takes the money held for them. */
	public FinanceBook.Result fill(ServerPlayer player, FinanceBook book, long id, int quantity, long day) {
		Request request = request(id);
		if (request == null) {
			return FinanceBook.Result.fail("That request is gone.");
		}
		if (request.owner.equals(player.getUUID())) {
			return FinanceBook.Result.fail("That is your own request.");
		}
		if (request.price <= 0) {
			return FinanceBook.Result.fail("That is an advertisement: talk to " + request.ownerName + " to trade.");
		}
		int have = carried(player, request.item, false);
		int count = Math.min(Math.min(quantity, request.quantity), have);
		if (count <= 0) {
			return FinanceBook.Result.fail("You carry none of that item.");
		}
		take(player, request.item, count, false);
		long value = request.price * count;
		String what = describe(request.item, count);
		book.settle(request.owner, player.getUUID(), value, day,
				player.getName().getString() + " filled " + request.ownerName + "'s request with " + what + " for " + book.format(value) + ".");
		taxSale(book, player.getUUID(), player.getName().getString(), value, day);
		request.quantity -= count;
		if (request.quantity <= 0) {
			requests.remove(request);
		}
		recordTrade(request.item, value, count);
		give(player.level().getServer(), request.owner, request.item, count);
		return new FinanceBook.Result(true, "Delivered " + what + ".");
	}

	public FinanceBook.Result cancelRequest(ServerPlayer player, FinanceBook book, long id, boolean admin, long day) {
		Request request = request(id);
		if (request == null || (!admin && !request.owner.equals(player.getUUID()))) {
			return FinanceBook.Result.fail("No such request of yours.");
		}
		requests.remove(request);
		if (request.price > 0) {
			book.release(request.owner, request.price * request.quantity, day, request.ownerName + "'s request for "
					+ describe(request.item, request.quantity) + " was withdrawn; " + book.format(request.price * request.quantity) + " returned.");
		} else {
			book.note(day, request.owner, null, request.ownerName + "'s advertisement was withdrawn.");
		}
		setDirty();
		return new FinanceBook.Result(true, "Request withdrawn.");
	}

	// ------------------------------------------------------------------ market chests

	/** Records which company a Market Chest sells for (0 forgets the chest). */
	public void setChest(String ref, long corp) {
		if (corp > 0) {
			chests.put(ref, corp);
		} else {
			chests.remove(ref);
		}
		setDirty();
	}

	/** How many Market Chests sell for {@code corp}. */
	public int chestsOf(long corp) {
		int count = 0;
		for (long c : chests.values()) {
			if (c == corp) {
				count++;
			}
		}
		return count;
	}

	/** The fixed-price listings that came from one chest. */
	public List<Listing> chestListings(String ref) {
		List<Listing> out = new ArrayList<>();
		for (Listing listing : listings) {
			if (listing.chest.equals(ref)) {
				out.add(listing);
			}
		}
		return out;
	}

	/**
	 * Lists items from a Market Chest for its company at {@code price} each. Joins the chest's listing of the same
	 * item at the same price when there is one, so a chest makes one listing per item. Returns how many were listed:
	 * fewer than offered (or none) when the company has as many listings as the admins allow.
	 */
	public int listFromChest(MinecraftServer server, FinanceBook book, Equities.Corporation corp, String ref, ItemStack stack,
			long price, long day) {
		int count = Math.min(stack.getCount(), MAX_QUANTITY);
		if (count <= 0 || price <= 0 || price > FinanceBook.MAX_AMOUNT / MAX_QUANTITY) {
			return 0;
		}
		for (Listing listing : listings) {
			if (listing.chest.equals(ref) && listing.price == price && same(listing.item, stack)) {
				int room = Math.max(0, MAX_QUANTITY - listing.quantity);
				int added = Math.min(room, count);
				listing.quantity += added;
				setDirty();
				return added;
			}
		}
		long limit = ExchangeSettings.value(book, ExchangeSettings.LISTINGS);
		long mine = listings.stream().filter(l -> l.seller.equals(corp.treasury)).count();
		if (mine >= limit) {
			return 0;
		}
		book.treasury(corp.treasury, corp.ticker);
		String seller = corp.name + " (" + corp.ticker + ")";
		Listing listing = new Listing(nextId++, corp.treasury, seller, stack.copyWithCount(1), count, price, false, 0, 0,
				Optional.empty(), "", corp.id, ref);
		listings.add(listing);
		setDirty();
		book.note(day, corp.treasury, null, seller + " listed " + describe(stack, count) + " at " + book.format(price) + " each.");
		return count;
	}

	/**
	 * Takes a chest's unsold goods off the market. {@code insert} puts as many as fit back into the chest and says how
	 * many it took; what does not fit stays listed. Returns how many items came back.
	 */
	public int takeBack(String ref, java.util.function.ToIntFunction<ItemStack> insert) {
		int total = 0;
		for (Listing listing : chestListings(ref)) {
			int put = Math.max(0, Math.min(listing.quantity, insert.applyAsInt(listing.item.copyWithCount(listing.quantity))));
			listing.quantity -= put;
			total += put;
			if (listing.quantity <= 0) {
				listings.remove(listing);
			}
		}
		if (total > 0) {
			setDirty();
		}
		return total;
	}

	// ------------------------------------------------------------------ days

	/** Closes finished auctions and measures the day's prices against the last ones. */
	public void newDay(MinecraftServer server, FinanceBook book, long day) {
		for (Listing listing : new ArrayList<>(listings)) {
			if (!listing.auction || listing.endDay > day) {
				continue;
			}
			listings.remove(listing);
			String what = describe(listing.item, listing.quantity);
			if (listing.bidder != null) {
				book.settle(listing.bidder, listing.seller, listing.bid, day, listing.bidderName + " won auction #"
						+ listing.id + " (" + what + ") from " + listing.sellerName + " for " + book.format(listing.bid) + ".");
				taxSale(book, listing.seller, listing.sellerName, listing.bid, day);
				recordTrade(listing.item, listing.bid, listing.quantity);
				give(server, listing.bidder, listing.item, listing.quantity);
			} else {
				book.note(day, listing.seller, null, "Auction #" + listing.id + " (" + what + ") closed with no bids; the items went back to "
						+ listing.sellerName + ".");
				give(server, returnTo(server, listing), listing.item, listing.quantity);
			}
		}
		// Inflation: the average change in unit price of everything that traded since the last measure.
		long changes = 0;
		int counted = 0;
		for (Map.Entry<String, Long> entry : todayQuantity.entrySet()) {
			long quantity = entry.getValue();
			if (quantity <= 0) {
				continue;
			}
			// In hundredths of a Jug, so goods worth less than a Jug each still have a price.
			long average = todayValue.getOrDefault(entry.getKey(), 0L) * 100 / quantity;
			Long before = lastPrice.get(entry.getKey());
			if (before != null && before > 0) {
				changes += Math.max(-10_000L, Math.min(100_000L, (average - before) * 10_000 / before));
				counted++;
			}
			lastPrice.put(entry.getKey(), Math.max(1, average));
		}
		if (counted > 0) {
			inflationBp = (int) (changes / counted);
			inflationKnown = true;
		}
		todayValue.clear();
		todayQuantity.clear();
		setDirty();
	}
}
