package io.github.jimbozoomer.jugcraft.finance;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * The land office: who owns which Overworld chunk, which are for sale, and whom each owner trusts. Saved with the
 * overworld (data/jugcraft_land.dat).
 *
 * <p>Unclaimed chunks are bought from the central bank: the base price, 10% dearer for every chunk the buyer already
 * owns, paid into the reserve (a money sink). Owned chunks can be put up for sale at the owner's price; a buyer pays
 * the owner. Only the owner, players they trust and operators may break, place or use blocks, or touch entities, in a
 * claimed chunk. Every check is one hash lookup, and nothing here loads chunks.
 */
public final class LandRegistry extends SavedData {
	public static final long DEFAULT_BASE_PRICE = 25L; // Jugs
	public static final int GROWTH_BP = 1_000; // each chunk owned makes the next 10% dearer
	/** How far, in chunks, the land map reaches from the player; claims beyond it are refused. */
	public static final int MAP_RADIUS = 7;
	public static final int MAX_TRUSTED = 32;

	/**
	 * One claimed chunk. {@code price} above zero means it is for sale at that price. An auction runs while
	 * {@code auctionEnd} is not -1: {@code opening} is the least first bid, and the top bid's money is held in escrow.
	 */
	public static final class Claim {
		public final long key;
		public UUID owner;
		public String ownerName;
		public long price;
		public long auctionEnd;
		public long opening;
		public long bid;
		public UUID bidder;
		public String bidderName;

		public Claim(long key, UUID owner, String ownerName, long price) {
			this(key, owner, ownerName, price, -1, 0, 0, java.util.Optional.empty(), "");
		}

		public Claim(long key, UUID owner, String ownerName, long price, long auctionEnd, long opening, long bid,
				java.util.Optional<UUID> bidder, String bidderName) {
			this.key = key;
			this.owner = owner;
			this.ownerName = ownerName;
			this.price = price;
			this.auctionEnd = auctionEnd;
			this.opening = opening;
			this.bid = bid;
			this.bidder = bidder.orElse(null);
			this.bidderName = bidderName;
		}

		public boolean onAuction() {
			return auctionEnd >= 0;
		}

		void endAuction() {
			auctionEnd = -1;
			opening = 0;
			bid = 0;
			bidder = null;
			bidderName = "";
		}
	}

	private static final Codec<UUID> UUID_STRING = Codec.STRING.xmap(UUID::fromString, UUID::toString);
	private static final Codec<Claim> CLAIM = RecordCodecBuilder.create(i -> i.group(
			Codec.LONG.fieldOf("chunk").forGetter(c -> c.key),
			UUIDUtil.CODEC.fieldOf("owner").forGetter(c -> c.owner),
			Codec.STRING.fieldOf("owner_name").forGetter(c -> c.ownerName),
			Codec.LONG.optionalFieldOf("price", 0L).forGetter(c -> c.price),
			Codec.LONG.optionalFieldOf("auction_end", -1L).forGetter(c -> c.auctionEnd),
			Codec.LONG.optionalFieldOf("opening", 0L).forGetter(c -> c.opening),
			Codec.LONG.optionalFieldOf("bid", 0L).forGetter(c -> c.bid),
			UUIDUtil.CODEC.optionalFieldOf("bidder").forGetter(c -> java.util.Optional.ofNullable(c.bidder)),
			Codec.STRING.optionalFieldOf("bidder_name", "").forGetter(c -> c.bidderName == null ? "" : c.bidderName)
	).apply(i, Claim::new));
	static final Codec<LandRegistry> CODEC = RecordCodecBuilder.create(i -> i.group(
			Codec.LONG.optionalFieldOf("base_price", DEFAULT_BASE_PRICE).forGetter(l -> l.basePrice),
			CLAIM.listOf().optionalFieldOf("claims", List.of()).forGetter(l -> new ArrayList<Claim>(l.claims.values())),
			Codec.unboundedMap(UUID_STRING, UUIDUtil.CODEC.listOf()).optionalFieldOf("trusted", Map.<UUID, List<UUID>>of())
					.forGetter(l -> {
						Map<UUID, List<UUID>> out = new HashMap<UUID, List<UUID>>();
						for (Map.Entry<UUID, Set<UUID>> e : l.trusted.entrySet()) {
							out.put(e.getKey(), new ArrayList<UUID>(e.getValue()));
						}
						return out;
					})
	).apply(i, LandRegistry::new));
	static final SavedDataType<LandRegistry> TYPE = new SavedDataType<>(Jugcraft.id("land"), LandRegistry::new, CODEC, null);

	private long basePrice;
	private final Map<Long, Claim> claims = new HashMap<Long, Claim>();
	private final Map<UUID, Set<UUID>> trusted = new HashMap<UUID, Set<UUID>>();

	LandRegistry() {
		this(DEFAULT_BASE_PRICE, List.<Claim>of(), Map.<UUID, List<UUID>>of());
	}

	LandRegistry(long basePrice, List<Claim> claims, Map<UUID, List<UUID>> trusted) {
		this.basePrice = Math.max(0, basePrice);
		for (Claim claim : claims) {
			this.claims.put(claim.key, claim);
		}
		for (Map.Entry<UUID, List<UUID>> e : trusted.entrySet()) {
			this.trusted.put(e.getKey(), new HashSet<UUID>(e.getValue()));
		}
	}

	public static LandRegistry get(MinecraftServer server) {
		return server.overworld().getDataStorage().computeIfAbsent(TYPE);
	}

	// ------------------------------------------------------------------ chunk keys

	/** Packs chunk coordinates into one key; the client uses the same packing. */
	public static long key(int chunkX, int chunkZ) {
		return (chunkX & 0xFFFFFFFFL) | ((long) chunkZ << 32);
	}

	public static int keyX(long key) {
		return (int) key;
	}

	public static int keyZ(long key) {
		return (int) (key >> 32);
	}

	public static long keyOf(BlockPos pos) {
		return key(pos.getX() >> 4, pos.getZ() >> 4);
	}

	// ------------------------------------------------------------------ reading

	public long basePrice() {
		return basePrice;
	}

	public Claim claim(long key) {
		return claims.get(key);
	}

	public int owned(UUID owner) {
		int count = 0;
		for (Claim claim : claims.values()) {
			if (claim.owner.equals(owner)) {
				count++;
			}
		}
		return count;
	}

	/** What the next unclaimed chunk costs {@code buyer}: the base price, the admins' step dearer for each chunk they own. */
	public long nextPrice(UUID buyer, FinanceBook book) {
		long price = basePrice;
		int growth = (int) ExchangeSettings.value(book, ExchangeSettings.LAND_GROWTH);
		int owned = owned(buyer);
		for (int i = 0; i < owned && price < FinanceBook.MAX_AMOUNT && growth > 0; i++) {
			price = Math.min(FinanceBook.MAX_AMOUNT, price + Math.max(1, FinanceBook.percentOf(price, growth)));
		}
		return price;
	}

	/** Null when {@code buyer} may own one more chunk, else why not (the admins' limit). */
	private String overLimit(UUID buyer, FinanceBook book) {
		long max = ExchangeSettings.value(book, ExchangeSettings.LAND_MAX);
		return max > 0 && owned(buyer) >= max ? "Nobody may own more than " + max + " chunks." : null;
	}

	public Set<UUID> trustedBy(UUID owner) {
		return trusted.getOrDefault(owner, Set.of());
	}

	/** Claims within {@code radius} chunks of a centre, for the map. */
	public List<Claim> around(int centreX, int centreZ, int radius) {
		List<Claim> out = new ArrayList<Claim>();
		for (int dz = -radius; dz <= radius; dz++) {
			for (int dx = -radius; dx <= radius; dx++) {
				Claim claim = claims.get(key(centreX + dx, centreZ + dz));
				if (claim != null) {
					out.add(claim);
				}
			}
		}
		return out;
	}

	// ------------------------------------------------------------------ protection

	/** True when {@code player} may change things at {@code pos} in {@code level}. */
	public static boolean mayBuild(Player player, Level level, BlockPos pos) {
		if (level.isClientSide() || level.dimension() != Level.OVERWORLD || !(player instanceof ServerPlayer server)) {
			return true;
		}
		MinecraftServer mc = level.getServer();
		if (mc == null) {
			return true;
		}
		LandRegistry land = get(mc);
		Claim claim = land.claims.get(keyOf(pos));
		if (claim == null || actsFor(mc, player.getUUID(), claim.owner)) {
			return true;
		}
		// Corporation land: the controlling shareholder's trusted players may build too.
		Equities.Corporation corp = Equities.get(mc).byTreasury(claim.owner);
		UUID boss = corp == null ? claim.owner : Equities.get(mc).controller(corp);
		if (boss != null && land.trustedBy(boss).contains(player.getUUID())) {
			return true;
		}
		return FinanceService.isAdmin(server);
	}

	/** True when {@code player} owns the land of {@code owner}: it is them, or a corporation they control. */
	public static boolean actsFor(MinecraftServer server, UUID player, UUID owner) {
		return owner.equals(player) || Equities.get(server).controls(player, owner);
	}

	/** Who owns the chunk at {@code pos}, for the refusal message. */
	public static String ownerAt(Level level, BlockPos pos) {
		MinecraftServer mc = level.getServer();
		Claim claim = mc == null ? null : get(mc).claims.get(keyOf(pos));
		return claim == null ? "someone" : claim.ownerName;
	}

	// ------------------------------------------------------------------ actions

	private static boolean inReach(ServerPlayer player, long key) {
		if (player.level().dimension() != Level.OVERWORLD) {
			return false;
		}
		BlockPos at = player.blockPosition();
		return Math.abs((long) keyX(key) - (at.getX() >> 4)) <= MAP_RADIUS && Math.abs((long) keyZ(key) - (at.getZ() >> 4)) <= MAP_RADIUS;
	}

	/**
	 * Buys a chunk: an unclaimed one from the bank at the buyer's next price, or one for sale from its owner. The
	 * client sends the price it showed; the purchase fails if the price has gone up since.
	 */
	public FinanceBook.Result buy(ServerPlayer player, FinanceBook book, long key, long shownPrice, long day) {
		if (!inReach(player, key)) {
			return FinanceBook.Result.fail("Stand in the Overworld within " + MAP_RADIUS + " chunks of the land you want.");
		}
		String name = player.getName().getString();
		String where = "chunk " + keyX(key) + ", " + keyZ(key);
		Claim claim = claims.get(key);
		if (claim == null) {
			if (touchesTown(player.level(), key)) {
				return FinanceBook.Result.fail("This chunk belongs to the town and cannot be bought.");
			}
			String limit = overLimit(player.getUUID(), book);
			if (limit != null) {
				return FinanceBook.Result.fail(limit);
			}
			long price = nextPrice(player.getUUID(), book);
			if (price > shownPrice) {
				return FinanceBook.Result.fail("The price is now " + book.format(price) + ": try again.");
			}
			FinanceBook.Result paid = book.payBank(player.getUUID(), price, day, name + " bought " + where + " from the land office for "
					+ book.format(price) + ".");
			if (!paid.ok()) {
				return paid;
			}
			claims.put(key, new Claim(key, player.getUUID(), name, 0));
			setDirty();
			return new FinanceBook.Result(true, "You own " + where + ".");
		}
		if (actsFor(player.level().getServer(), player.getUUID(), claim.owner)) {
			return FinanceBook.Result.fail("You already own this land.");
		}
		if (claim.onAuction()) {
			return FinanceBook.Result.fail("This land is up for auction: bid on it instead.");
		}
		if (claim.price <= 0) {
			return FinanceBook.Result.fail(claim.ownerName + " is not selling this land.");
		}
		String limit = overLimit(player.getUUID(), book);
		if (limit != null) {
			return FinanceBook.Result.fail(limit);
		}
		if (claim.price > shownPrice) {
			return FinanceBook.Result.fail("The price is now " + book.format(claim.price) + ": try again.");
		}
		FinanceBook.Result paid = book.trade(player.getUUID(), claim.owner, claim.price, day,
				name + " bought " + where + " from " + claim.ownerName + " for " + book.format(claim.price) + ".");
		if (!paid.ok()) {
			return paid;
		}
		claim.owner = player.getUUID();
		claim.ownerName = name;
		claim.price = 0;
		setDirty();
		return new FinanceBook.Result(true, "You own " + where + ".");
	}

	static boolean touchesTown(Level level, long key) {
		return TownLand.touchesTown(level, key);
	}

	/** Puts the owner's chunk up for sale at {@code price}, or takes it off the market with 0. */
	public FinanceBook.Result sell(ServerPlayer player, FinanceBook book, long key, long price, long day) {
		Claim claim = claims.get(key);
		if (claim == null || !actsFor(player.level().getServer(), player.getUUID(), claim.owner)) {
			return FinanceBook.Result.fail("You do not own that land.");
		}
		if (price < 0 || price > FinanceBook.MAX_AMOUNT) {
			return FinanceBook.Result.fail("Choose a price.");
		}
		String where = "chunk " + keyX(key) + ", " + keyZ(key);
		if (claim.onAuction()) {
			if (price > 0) {
				return FinanceBook.Result.fail("Stop the auction first.");
			}
			if (claim.bidder != null) {
				return FinanceBook.Result.fail("An auction with a bid runs to its close.");
			}
			claim.endAuction();
			setDirty();
			book.note(day, player.getUUID(), null, "The auction of " + where + " was called off.");
			return new FinanceBook.Result(true, "Auction called off.");
		}
		claim.price = price;
		setDirty();
		if (price == 0) {
			return new FinanceBook.Result(true, where + " is no longer for sale.");
		}
		book.note(day, player.getUUID(), null, claim.ownerName + " put " + where + " up for sale at " + book.format(price) + ".");
		return new FinanceBook.Result(true, where + " is for sale at " + book.format(price) + ".");
	}

	/** Gives up a chunk (no refund). Operators may remove anyone's claim. */
	public FinanceBook.Result abandon(ServerPlayer player, FinanceBook book, long key, boolean admin, long day) {
		Claim claim = claims.get(key);
		if (claim == null || (!admin && !actsFor(player.level().getServer(), player.getUUID(), claim.owner))) {
			return FinanceBook.Result.fail("You do not own that land.");
		}
		if (claim.onAuction() && claim.bidder != null) {
			if (!admin || actsFor(player.level().getServer(), player.getUUID(), claim.owner)) {
				return FinanceBook.Result.fail("An auction with a bid runs to its close.");
			}
			book.release(claim.bidder, claim.bid, day, claim.bidderName + "'s bid on chunk " + keyX(key) + ", " + keyZ(key)
					+ " was returned.");
		}
		claims.remove(key);
		setDirty();
		String where = "chunk " + keyX(key) + ", " + keyZ(key);
		boolean byAdmin = !actsFor(player.level().getServer(), player.getUUID(), claim.owner);
		book.note(day, claim.owner, null, (byAdmin ? "An admin removed " + claim.ownerName + "'s claim on " : claim.ownerName + " gave up ")
				+ where + ".");
		return new FinanceBook.Result(true, where + " is unclaimed now.");
	}

	/**
	 * Moves one of your chunks into a corporation you control (zoning), or with {@code corpId} 0 takes a chunk of a
	 * corporation you control back into your own name.
	 */
	public FinanceBook.Result zone(ServerPlayer player, FinanceBook book, long key, long corpId, long day) {
		MinecraftServer server = player.level().getServer();
		Claim claim = claims.get(key);
		if (claim == null || !actsFor(server, player.getUUID(), claim.owner)) {
			return FinanceBook.Result.fail("You do not own that land.");
		}
		if (claim.onAuction()) {
			return FinanceBook.Result.fail("That land is up for auction.");
		}
		Equities equities = Equities.get(server);
		String where = "chunk " + keyX(key) + ", " + keyZ(key);
		if (corpId == 0) {
			claim.owner = player.getUUID();
			claim.ownerName = player.getName().getString();
		} else {
			Equities.Corporation corp = equities.corp(corpId);
			if (corp == null || !player.getUUID().equals(equities.controller(corp))) {
				return FinanceBook.Result.fail("You do not control that corporation.");
			}
			claim.owner = corp.treasury;
			claim.ownerName = corp.ticker;
		}
		claim.price = 0;
		setDirty();
		book.note(day, player.getUUID(), null, player.getName().getString() + " zoned " + where + " to " + claim.ownerName + ".");
		return new FinanceBook.Result(true, where + " now belongs to " + claim.ownerName + ".");
	}

	// ------------------------------------------------------------------ auctions

	/** The least the next bid on a chunk may be. */
	public static long minimumBid(Claim claim, FinanceBook book) {
		return Market.minimumBid(claim.bid, claim.opening, (int) ExchangeSettings.value(book, ExchangeSettings.BID_STEP));
	}

	/** The owner puts a chunk up for auction for {@code days}: the top bidder when it closes buys it. */
	public FinanceBook.Result auction(ServerPlayer player, FinanceBook book, long key, long opening, int lengthIndex, long day) {
		Claim claim = claims.get(key);
		if (claim == null || !actsFor(player.level().getServer(), player.getUUID(), claim.owner)) {
			return FinanceBook.Result.fail("You do not own that land.");
		}
		if (claim.onAuction()) {
			return FinanceBook.Result.fail("That land is already up for auction.");
		}
		if (opening <= 0 || opening > FinanceBook.MAX_AMOUNT) {
			return FinanceBook.Result.fail("Set an opening bid.");
		}
		int days = Market.AUCTION_DAYS[Math.max(0, Math.min(Market.AUCTION_DAYS.length - 1, lengthIndex))];
		claim.price = 0;
		claim.auctionEnd = day + days;
		claim.opening = opening;
		setDirty();
		String where = "chunk " + keyX(key) + ", " + keyZ(key);
		book.note(day, player.getUUID(), null, claim.ownerName + " put " + where + " up for auction from " + book.format(opening)
				+ ", closing on day " + claim.auctionEnd + ".");
		return new FinanceBook.Result(true, where + " is up for auction until day " + claim.auctionEnd + ".");
	}

	/** Bids on a chunk's auction: the money is held until the bidder is outbid or the auction closes. */
	public FinanceBook.Result bid(ServerPlayer player, FinanceBook book, long key, long amount, long day) {
		Claim claim = claims.get(key);
		if (claim == null || !claim.onAuction() || claim.auctionEnd <= day) {
			return FinanceBook.Result.fail("That auction has closed.");
		}
		if (actsFor(player.level().getServer(), player.getUUID(), claim.owner)) {
			return FinanceBook.Result.fail("You cannot bid on your own land.");
		}
		if (player.getUUID().equals(claim.bidder)) {
			return FinanceBook.Result.fail("You already hold the top bid.");
		}
		String limit = overLimit(player.getUUID(), book);
		if (limit != null) {
			return FinanceBook.Result.fail(limit);
		}
		long least = minimumBid(claim, book);
		if (amount < least) {
			return FinanceBook.Result.fail("Bid at least " + book.format(least) + ".");
		}
		String name = player.getName().getString();
		String where = "chunk " + keyX(key) + ", " + keyZ(key);
		FinanceBook.Result held = book.hold(player.getUUID(), amount, day, name + " bid " + book.format(amount) + " for " + where + ".");
		if (!held.ok()) {
			return held;
		}
		if (claim.bidder != null) {
			book.release(claim.bidder, claim.bid, day, claim.bidderName + " was outbid on " + where + "; "
					+ book.format(claim.bid) + " returned.");
		}
		claim.bid = amount;
		claim.bidder = player.getUUID();
		claim.bidderName = name;
		setDirty();
		return new FinanceBook.Result(true, "You hold the top bid for " + where + ".");
	}

	/** Closes land auctions that have run their time: the top bidder pays the owner and gets the chunk. */
	public void newDay(FinanceBook book, long day) {
		for (Claim claim : claims.values()) {
			if (!claim.onAuction() || claim.auctionEnd > day) {
				continue;
			}
			String where = "chunk " + keyX(claim.key) + ", " + keyZ(claim.key);
			if (claim.bidder != null) {
				book.settle(claim.bidder, claim.owner, claim.bid, day, claim.bidderName + " won the auction of " + where + " from "
						+ claim.ownerName + " for " + book.format(claim.bid) + ".");
				claim.owner = claim.bidder;
				claim.ownerName = claim.bidderName;
			} else {
				book.note(day, claim.owner, null, "The auction of " + where + " closed with no bids; " + claim.ownerName
						+ " keeps it.");
			}
			claim.endAuction();
			claim.price = 0;
			setDirty();
		}
	}

	/** Adds or removes a player the owner trusts on all their land. */
	public FinanceBook.Result trust(ServerPlayer player, FinanceBook book, String name, boolean add) {
		FinanceBook.Account other = book.findByName(name == null ? "" : name);
		if (other == null) {
			return FinanceBook.Result.fail("No player called \"" + FinanceBook.clean(name, 16) + "\" has joined this server.");
		}
		if (other.id.equals(player.getUUID())) {
			return FinanceBook.Result.fail("You always may build on your own land.");
		}
		Set<UUID> set = trusted.computeIfAbsent(player.getUUID(), id -> new HashSet<UUID>());
		if (add) {
			if (set.size() >= MAX_TRUSTED) {
				return FinanceBook.Result.fail("You trust " + MAX_TRUSTED + " players already.");
			}
			set.add(other.id);
		} else {
			set.remove(other.id);
		}
		setDirty();
		return new FinanceBook.Result(true, add ? other.name + " may now build on your land." : other.name + " may no longer build on your land.");
	}

	// ------------------------------------------------------------------ admin

	public FinanceBook.Result setBasePrice(FinanceBook book, long price, long day) {
		if (price < 0 || price > FinanceBook.MAX_AMOUNT) {
			return FinanceBook.Result.fail("Choose a price.");
		}
		basePrice = price;
		setDirty();
		book.note(day, null, null, "The land office set the base price of a chunk to " + book.format(price) + ".");
		return new FinanceBook.Result(true, "Base land price set to " + book.format(price) + ".");
	}

	/** Gives the chunk at a position to a player (or takes it from whoever has it). */
	public FinanceBook.Result assign(FinanceBook book, long key, FinanceBook.Account to, long day) {
		Claim claim = claims.get(key);
		if (claim != null && claim.onAuction()) {
			if (claim.bidder != null) {
				book.release(claim.bidder, claim.bid, day, claim.bidderName + "'s bid was returned.");
			}
			claim.endAuction();
		}
		if (claim == null) {
			claims.put(key, new Claim(key, to.id, to.name, 0));
		} else {
			claim.owner = to.id;
			claim.ownerName = to.name;
			claim.price = 0;
		}
		setDirty();
		book.note(day, to.id, null, "An admin gave chunk " + keyX(key) + ", " + keyZ(key) + " to " + to.name + ".");
		return new FinanceBook.Result(true, "Chunk given to " + to.name + ".");
	}
}
