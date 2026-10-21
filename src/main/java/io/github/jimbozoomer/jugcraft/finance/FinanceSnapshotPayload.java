package io.github.jimbozoomer.jugcraft.finance;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;

/**
 * Server to client: everything one player's Exchange screen shows. Sent when they open it, after each of their actions
 * and on their screen's slow refresh, never on a timer to players who are not looking. It is display-only: the client
 * never decides anything from it.
 */
public record FinanceSnapshotPayload(boolean open, String notice, boolean noticeOk, String currency, long day,
		int feeBp, long borrowLimit, long reserve, long circulation, long balance, long debt, long borrowable,
		boolean admin, List<Rank> richList, List<ContractView> contracts, List<String> history,
		long escrow, long goodsValue, int inflationBp, boolean inflationKnown, int waiting,
		List<ListingView> listings, List<RequestView> requests, long dueDay, boolean overdue, long creditLimit,
		boolean starterAvailable, int termDays, int shareBp, LandView land, EquityView equities,
		List<SettingView> settings) implements CustomPacketPayload {
	public static final int MAX_LIST = 32;
	public static final int MAX_MARKET = 64;
	public static final int MAX_TEXT = 256;
	public static final Type<FinanceSnapshotPayload> TYPE = new Type<>(Jugcraft.id("finance_snapshot"));
	public static final StreamCodec<RegistryFriendlyByteBuf, FinanceSnapshotPayload> CODEC = StreamCodec.ofMember(
			FinanceSnapshotPayload::write, FinanceSnapshotPayload::read);

	/** A rich-list row. */
	public record Rank(String name, long balance) {
	}

	/** A contract as one of its parties sees it. {@code outgoing} means this player pays. */
	public record ContractView(long id, boolean outgoing, String other, long amount, int period, String note, long nextDay,
			boolean active) {
	}

	/** A market listing. {@code mine}: this player is selling it; {@code leading}: this player holds the top bid. */
	public record ListingView(long id, ItemStack item, int quantity, long price, boolean auction, long endDay, long bid,
			long minBid, boolean mine, boolean leading, String seller, boolean company) {
	}

	/** An admin setting ({@link ExchangeSettings}); sent to operators only. {@code unit} is the Unit's ordinal. */
	public record SettingView(String key, String group, String label, int unit, long value) {
	}

	/** A request. Price 0 is an advertisement. */
	public record RequestView(long id, ItemStack item, int quantity, long price, boolean mine, String owner) {
	}

	/** A claimed chunk on the land map, relative to the map's centre. */
	public record Plot(int dx, int dz, String owner, boolean mine, boolean trusted, long price, long auctionEnd, long bid,
			long minBid, boolean leading) {
		public boolean onAuction() {
			return auctionEnd >= 0;
		}
	}

	/**
	 * The land map around the player. {@code active} is false outside the Overworld. {@code nextPrice} is what an
	 * unclaimed chunk costs this player.
	 */
	public record LandView(boolean active, int centreX, int centreZ, long nextPrice, long basePrice, int owned,
			List<Plot> plots, List<String> trusted) {
		public static final LandView NONE = new LandView(false, 0, 0, 0, 0, 0, List.of(), List.of());
	}

	/** One price level of the order book: shares wanted or offered at a price. */
	public record Level(long price, long quantity) {
	}

	/** A shareholder on the ownership chart. */
	public record Holding(String name, long shares, boolean me) {
	}

	/** One of this player's open orders ({@code company}: the company's own order, which its controller manages). */
	public record MyOrder(long id, boolean buy, long price, long quantity, boolean company) {
	}

	/** A listed corporation as this player sees it. */
	public record CorpView(long id, String name, String ticker, byte[] logo, boolean logoPending, long price, long totalShares,
			long lastDividend, long treasury, String controller, boolean controlledByMe, long myShares, List<Holding> holders,
			List<Level> asks, List<Level> bids, List<MyOrder> myOrders) {
	}

	public record EquityView(long charterFee, long issueFee, long chestPrice, List<CorpView> corps) {
		public static final EquityView NONE = new EquityView(0, 0, 0, List.of());
	}

	private static void writeEquities(RegistryFriendlyByteBuf buf, EquityView view) {
		buf.writeVarLong(view.charterFee());
		buf.writeVarLong(view.issueFee());
		buf.writeVarLong(view.chestPrice());
		List<CorpView> corps = head(view.corps(), Equities.HARD_MAX_CORPS);
		buf.writeVarInt(corps.size());
		for (CorpView c : corps) {
			buf.writeVarLong(c.id());
			buf.writeUtf(c.name(), MAX_TEXT);
			buf.writeUtf(c.ticker(), MAX_TEXT);
			for (int i = 0; i < Equities.LOGO_PIXELS; i++) {
				buf.writeByte(i < c.logo().length ? c.logo()[i] : Equities.CLEAR);
			}
			buf.writeBoolean(c.logoPending());
			buf.writeVarLong(c.price());
			buf.writeVarLong(c.totalShares());
			buf.writeVarLong(c.lastDividend());
			buf.writeVarLong(c.treasury());
			buf.writeUtf(c.controller(), MAX_TEXT);
			buf.writeBoolean(c.controlledByMe());
			buf.writeVarLong(c.myShares());
			List<Holding> holders = head(c.holders(), 8);
			buf.writeVarInt(holders.size());
			for (Holding h : holders) {
				buf.writeUtf(h.name(), MAX_TEXT);
				buf.writeVarLong(h.shares());
				buf.writeBoolean(h.me());
			}
			for (List<Level> side : List.of(c.asks(), c.bids())) {
				List<Level> levels = head(side, 5);
				buf.writeVarInt(levels.size());
				for (Level l : levels) {
					buf.writeVarLong(l.price());
					buf.writeVarLong(l.quantity());
				}
			}
			List<MyOrder> mine = head(c.myOrders(), 20);
			buf.writeVarInt(mine.size());
			for (MyOrder o : mine) {
				buf.writeVarLong(o.id());
				buf.writeBoolean(o.buy());
				buf.writeVarLong(o.price());
				buf.writeVarLong(o.quantity());
				buf.writeBoolean(o.company());
			}
		}
	}

	private static EquityView readEquities(RegistryFriendlyByteBuf buf) {
		long fee = buf.readVarLong();
		long issueFee = buf.readVarLong();
		long chestPrice = buf.readVarLong();
		int count = Math.min(Equities.HARD_MAX_CORPS, buf.readVarInt());
		List<CorpView> corps = new ArrayList<>(count);
		for (int n = 0; n < count; n++) {
			long id = buf.readVarLong();
			String name = buf.readUtf(MAX_TEXT);
			String ticker = buf.readUtf(MAX_TEXT);
			byte[] logo = new byte[Equities.LOGO_PIXELS];
			for (int i = 0; i < logo.length; i++) {
				logo[i] = buf.readByte();
			}
			boolean pending = buf.readBoolean();
			long price = buf.readVarLong();
			long total = buf.readVarLong();
			long dividend = buf.readVarLong();
			long treasury = buf.readVarLong();
			String controller = buf.readUtf(MAX_TEXT);
			boolean mineControl = buf.readBoolean();
			long myShares = buf.readVarLong();
			int hc = Math.min(8, buf.readVarInt());
			List<Holding> holders = new ArrayList<>(hc);
			for (int i = 0; i < hc; i++) {
				holders.add(new Holding(buf.readUtf(MAX_TEXT), buf.readVarLong(), buf.readBoolean()));
			}
			List<List<Level>> sides = new ArrayList<>(2);
			for (int side = 0; side < 2; side++) {
				int lc = Math.min(5, buf.readVarInt());
				List<Level> levels = new ArrayList<>(lc);
				for (int i = 0; i < lc; i++) {
					levels.add(new Level(buf.readVarLong(), buf.readVarLong()));
				}
				sides.add(levels);
			}
			int oc = Math.min(20, buf.readVarInt());
			List<MyOrder> mine = new ArrayList<>(oc);
			for (int i = 0; i < oc; i++) {
				mine.add(new MyOrder(buf.readVarLong(), buf.readBoolean(), buf.readVarLong(), buf.readVarLong(), buf.readBoolean()));
			}
			corps.add(new CorpView(id, name, ticker, logo, pending, price, total, dividend, treasury, controller, mineControl,
					myShares, holders, sides.get(0), sides.get(1), mine));
		}
		return new EquityView(fee, issueFee, chestPrice, corps);
	}

	/** Total money as players would count it: cash, held money, the reserve and goods at their listed prices. */
	public long totalMoney() {
		return circulation + escrow + reserve + goodsValue;
	}

	private static <T> List<T> head(List<T> list, int max) {
		return list.subList(0, Math.min(max, list.size()));
	}

	private void write(RegistryFriendlyByteBuf buf) {
		buf.writeBoolean(open);
		buf.writeUtf(notice, MAX_TEXT);
		buf.writeBoolean(noticeOk);
		buf.writeUtf(currency, MAX_TEXT);
		buf.writeVarLong(day);
		buf.writeVarInt(feeBp);
		buf.writeVarLong(borrowLimit);
		buf.writeVarLong(reserve);
		buf.writeVarLong(circulation);
		buf.writeVarLong(balance);
		buf.writeVarLong(debt);
		buf.writeVarLong(borrowable);
		buf.writeBoolean(admin);
		List<Rank> ranks = head(richList, MAX_LIST);
		buf.writeVarInt(ranks.size());
		for (Rank rank : ranks) {
			buf.writeUtf(rank.name(), MAX_TEXT);
			buf.writeVarLong(rank.balance());
		}
		List<ContractView> deals = head(contracts, MAX_LIST);
		buf.writeVarInt(deals.size());
		for (ContractView c : deals) {
			buf.writeVarLong(c.id());
			buf.writeBoolean(c.outgoing());
			buf.writeUtf(c.other(), MAX_TEXT);
			buf.writeVarLong(c.amount());
			buf.writeVarInt(c.period());
			buf.writeUtf(c.note(), MAX_TEXT);
			buf.writeVarLong(c.nextDay());
			buf.writeBoolean(c.active());
		}
		List<String> lines = head(history, MAX_LIST);
		buf.writeVarInt(lines.size());
		for (String line : lines) {
			buf.writeUtf(line, MAX_TEXT);
		}
		buf.writeVarLong(escrow);
		buf.writeVarLong(goodsValue);
		buf.writeVarInt(inflationBp);
		buf.writeBoolean(inflationKnown);
		buf.writeVarInt(waiting);
		List<ListingView> lots = head(listings, MAX_MARKET);
		buf.writeVarInt(lots.size());
		for (ListingView l : lots) {
			buf.writeVarLong(l.id());
			ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, l.item());
			buf.writeVarInt(l.quantity());
			buf.writeVarLong(l.price());
			buf.writeBoolean(l.auction());
			buf.writeVarLong(l.endDay());
			buf.writeVarLong(l.bid());
			buf.writeVarLong(l.minBid());
			buf.writeBoolean(l.mine());
			buf.writeBoolean(l.leading());
			buf.writeUtf(l.seller(), MAX_TEXT);
			buf.writeBoolean(l.company());
		}
		List<RequestView> wants = head(requests, MAX_MARKET);
		buf.writeVarInt(wants.size());
		for (RequestView r : wants) {
			buf.writeVarLong(r.id());
			ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, r.item());
			buf.writeVarInt(r.quantity());
			buf.writeVarLong(r.price());
			buf.writeBoolean(r.mine());
			buf.writeUtf(r.owner(), MAX_TEXT);
		}
		buf.writeVarLong(dueDay + 1);
		buf.writeBoolean(overdue);
		buf.writeVarLong(creditLimit);
		buf.writeBoolean(starterAvailable);
		buf.writeVarInt(termDays);
		buf.writeVarInt(shareBp);
		buf.writeBoolean(land.active());
		buf.writeVarInt(land.centreX());
		buf.writeVarInt(land.centreZ());
		buf.writeVarLong(land.nextPrice());
		buf.writeVarLong(land.basePrice());
		buf.writeVarInt(land.owned());
		List<Plot> plots = head(land.plots(), 256);
		buf.writeVarInt(plots.size());
		for (Plot p : plots) {
			buf.writeVarInt(p.dx());
			buf.writeVarInt(p.dz());
			buf.writeUtf(p.owner(), MAX_TEXT);
			buf.writeBoolean(p.mine());
			buf.writeBoolean(p.trusted());
			buf.writeVarLong(p.price());
			buf.writeVarLong(p.auctionEnd() + 1);
			buf.writeVarLong(p.bid());
			buf.writeVarLong(p.minBid());
			buf.writeBoolean(p.leading());
		}
		List<String> names = head(land.trusted(), MAX_LIST);
		buf.writeVarInt(names.size());
		for (String name : names) {
			buf.writeUtf(name, MAX_TEXT);
		}
		writeEquities(buf, equities);
		List<SettingView> rows = head(settings, 64);
		buf.writeVarInt(rows.size());
		for (SettingView row : rows) {
			buf.writeUtf(row.key(), MAX_TEXT);
			buf.writeUtf(row.group(), MAX_TEXT);
			buf.writeUtf(row.label(), MAX_TEXT);
			buf.writeVarInt(row.unit());
			buf.writeVarLong(row.value());
		}
	}

	private static FinanceSnapshotPayload read(RegistryFriendlyByteBuf buf) {
		boolean open = buf.readBoolean();
		String notice = buf.readUtf(MAX_TEXT);
		boolean noticeOk = buf.readBoolean();
		String currency = buf.readUtf(MAX_TEXT);
		long day = buf.readVarLong();
		int feeBp = buf.readVarInt();
		long limit = buf.readVarLong();
		long reserve = buf.readVarLong();
		long circulation = buf.readVarLong();
		long balance = buf.readVarLong();
		long debt = buf.readVarLong();
		long borrowable = buf.readVarLong();
		boolean admin = buf.readBoolean();
		int ranks = Math.min(MAX_LIST, buf.readVarInt());
		List<Rank> richList = new ArrayList<>(ranks);
		for (int i = 0; i < ranks; i++) {
			richList.add(new Rank(buf.readUtf(MAX_TEXT), buf.readVarLong()));
		}
		int count = Math.min(MAX_LIST, buf.readVarInt());
		List<ContractView> contracts = new ArrayList<>(count);
		for (int i = 0; i < count; i++) {
			contracts.add(new ContractView(buf.readVarLong(), buf.readBoolean(), buf.readUtf(MAX_TEXT), buf.readVarLong(),
					buf.readVarInt(), buf.readUtf(MAX_TEXT), buf.readVarLong(), buf.readBoolean()));
		}
		int lines = Math.min(MAX_LIST, buf.readVarInt());
		List<String> history = new ArrayList<>(lines);
		for (int i = 0; i < lines; i++) {
			history.add(buf.readUtf(MAX_TEXT));
		}
		long escrow = buf.readVarLong();
		long goodsValue = buf.readVarLong();
		int inflationBp = buf.readVarInt();
		boolean inflationKnown = buf.readBoolean();
		int waiting = buf.readVarInt();
		int lots = Math.min(MAX_MARKET, buf.readVarInt());
		List<ListingView> listings = new ArrayList<>(lots);
		for (int i = 0; i < lots; i++) {
			long id = buf.readVarLong();
			ItemStack item = ItemStack.OPTIONAL_STREAM_CODEC.decode(buf);
			listings.add(new ListingView(id, item, buf.readVarInt(), buf.readVarLong(), buf.readBoolean(), buf.readVarLong(),
					buf.readVarLong(), buf.readVarLong(), buf.readBoolean(), buf.readBoolean(), buf.readUtf(MAX_TEXT),
					buf.readBoolean()));
		}
		int wants = Math.min(MAX_MARKET, buf.readVarInt());
		List<RequestView> requests = new ArrayList<>(wants);
		for (int i = 0; i < wants; i++) {
			long id = buf.readVarLong();
			ItemStack item = ItemStack.OPTIONAL_STREAM_CODEC.decode(buf);
			requests.add(new RequestView(id, item, buf.readVarInt(), buf.readVarLong(), buf.readBoolean(), buf.readUtf(MAX_TEXT)));
		}
		long dueDay = buf.readVarLong() - 1;
		boolean overdue = buf.readBoolean();
		long creditLimit = buf.readVarLong();
		boolean starterAvailable = buf.readBoolean();
		int termDays = buf.readVarInt();
		int shareBp = buf.readVarInt();
		boolean landActive = buf.readBoolean();
		int centreX = buf.readVarInt();
		int centreZ = buf.readVarInt();
		long nextPrice = buf.readVarLong();
		long basePrice = buf.readVarLong();
		int owned = buf.readVarInt();
		int plotCount = Math.min(256, buf.readVarInt());
		List<Plot> plots = new ArrayList<>(plotCount);
		for (int i = 0; i < plotCount; i++) {
			plots.add(new Plot(buf.readVarInt(), buf.readVarInt(), buf.readUtf(MAX_TEXT), buf.readBoolean(), buf.readBoolean(),
					buf.readVarLong(), buf.readVarLong() - 1, buf.readVarLong(), buf.readVarLong(), buf.readBoolean()));
		}
		int nameCount = Math.min(MAX_LIST, buf.readVarInt());
		List<String> trusted = new ArrayList<>(nameCount);
		for (int i = 0; i < nameCount; i++) {
			trusted.add(buf.readUtf(MAX_TEXT));
		}
		LandView land = new LandView(landActive, centreX, centreZ, nextPrice, basePrice, owned, plots, trusted);
		EquityView equities = readEquities(buf);
		int settingCount = Math.min(64, buf.readVarInt());
		List<SettingView> settings = new ArrayList<>(settingCount);
		for (int i = 0; i < settingCount; i++) {
			settings.add(new SettingView(buf.readUtf(MAX_TEXT), buf.readUtf(MAX_TEXT), buf.readUtf(MAX_TEXT), buf.readVarInt(),
					buf.readVarLong()));
		}
		return new FinanceSnapshotPayload(open, notice, noticeOk, currency, day, feeBp, limit, reserve, circulation,
				balance, debt, borrowable, admin, richList, contracts, history, escrow, goodsValue, inflationBp,
				inflationKnown, waiting, listings, requests, dueDay, overdue, creditLimit, starterAvailable, termDays, shareBp, land,
				equities, settings);
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
