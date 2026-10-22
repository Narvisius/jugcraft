package io.github.jimbozoomer.jugcraft.client;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.finance.FinanceActionPayload;
import io.github.jimbozoomer.jugcraft.finance.FinanceActionPayload.Action;
import io.github.jimbozoomer.jugcraft.finance.FinanceBook;
import io.github.jimbozoomer.jugcraft.finance.FinanceSnapshotPayload;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import io.github.jimbozoomer.jugcraft.finance.Equities;

/**
 * The Jugcraft Exchange, "Gilded Counting House" style: a walnut frame with gold filigree corners, a riveted header
 * plaque carrying the emblem, brass tabs and buttons, and dark slate ledger tables. Seven tabs; Banking, Economy and
 * Contracts work, the market and land tabs describe what is coming.
 *
 * <p>Display and form-filling only. Every button sends a {@link FinanceActionPayload}; the server decides and answers
 * with a fresh {@link FinanceSnapshotPayload}. The open screen asks for fresh numbers every five seconds.
 *
 * <p>Clicks are handled by real widgets ({@link Plate} buttons and a slider), never by reading raw mouse buttons.
 * The emblem is textures/gui/exchange_emblem.png (24x24, drawn by tools/exchange_textures.py).
 */
public class ExchangeScreen extends Screen {
	private static final Identifier EMBLEM = Jugcraft.id("textures/gui/exchange_emblem.png");
	private static final int EMBLEM_SIZE = 24;
	private static final int MAX_WIDTH = 480;
	private static final int MAX_HEIGHT = 300;
	private static final int FRAME = 4;
	private static final int HEADER = 30;
	private static final int TAB_HEIGHT = 14;
	private static final int REFRESH_TICKS = 100;
	private static final int LINE = 10;
	private static final int CONTRACT_ROW = 21;

	// Palette: walnut, brass, slate, cream.
	static final int WALNUT = 0xFF2A1A0E;
	static final int WALNUT_LIGHT = 0xFF5A3B1F;
	static final int WALNUT_DARK = 0xFF120A04;
	static final int WOOD = 0xFF3A2615;
	static final int WOOD_LIGHT = 0xFF5B3E22;
	static final int WOOD_DARK = 0xFF1F1309;
	static final int PLAQUE = 0xFF1A110A;
	static final int PLAQUE_DARK = 0xFF0C0704;
	static final int PLAQUE_LIGHT = 0xFF3D2814;
	static final int BRASS = 0xFFB8913F;
	static final int BRASS_LIGHT = 0xFFF0D07A;
	static final int BRASS_DARK = 0xFF6B4C1B;
	static final int GOLD = 0xFFE8C463;
	static final int GOLD_DIM = 0xFF8A6A2C;
	static final int SLATE = 0xFF1D1B18;
	static final int SLATE_STRIPE = 0xFF24211C;
	static final int SLATE_DARK = 0xFF0E0D0B;
	static final int SLATE_LIGHT = 0xFF35302A;
	static final int CREAM = 0xFFEFE2C2;
	static final int TAN = 0xFFCDB98F;
	static final int DIM = 0xFFA8977A;
	static final int GAIN = 0xFF7FC96A;
	static final int LOSS = 0xFFE0624F;
	private static final int SILVER = 0xFFC9C9C9;
	private static final int BRONZE = 0xFFC98A4A;

	private static final String[] TABS = {"Banking", "Economy", "Equities", "Commodities", "Requests", "Contracts", "Land"};
	private static final int BANKING = 0;
	private static final int ECONOMY = 1;
	private static final int CONTRACTS = 5;
	private static final int COMMODITIES = 3;
	private static final int REQUESTS = 4;
	private static final int LAND = 6;
	private static final int EQUITIES = 2;
	private static final int CORP_ROW = 18;
	private static final int LOGO_CELL = 7;
	private static final int[] PIE_COLORS = {0xFFE8C463, 0xFF5F93C6, 0xFF7FC96A, 0xFFE0624F, 0xFFC98A4A, 0xFF9C7AE0, 0xFFC9C9C9, 0xFF6B6B6B};
	private static final int MAP_CELLS = 15;
	private static final int ROW = 18;
	private static final int PICK_ROWS = 2;
	private static final String[] AUCTION_LENGTHS = {"1 day", "3 days", "7 days"};
	private static final String[] PERIODS = {"Once", "Daily", "Weekly"};
	private static final String[][] COMING = {
			{},
			{},
			{"The Equities Market", "Charter a corporation with its own name, crest and land. Issue shares with a slider "
					+ "for how much of it you sell, and trade them on a bid and ask book. Whoever holds the majority runs "
					+ "the company's land and stock. Dividends show as yield beside each listing, and every company opens "
					+ "an ownership chart."},
			{"The Commodities Market", "List goods at a fixed price or put them to auction on your own terms. Goods sold "
					+ "by a corporation are listed under its name, and a corporation's market chest lists what you put in "
					+ "it for you. Prices here will measure the day's inflation."},
			{"Requests", "Post what you are buying, how many and at what price, or simply advertise that you want it."},
			{},
			{"The Land Office", "A map of the land around you: buy chunks, with prices that rise the more you hold, and "
					+ "sell them on at your own price or by auction. Corporations zone their land from the Equities tab."}};


	private FinanceSnapshotPayload data;
	private int tab = BANKING;
	private int left;
	private int top;
	private int panelWidth;
	private int panelHeight;
	private final int[] tabX = new int[TABS.length + 1];
	private int ticks;
	private boolean adminView;
	private double borrowFraction = 0.25;
	private int contractScroll;

	// Form values survive widget rebuilds (tab switches, new data).
	private String payTo = "";
	private String payAmount = "";
	private String payNote = "";
	private int payPeriod;
	private String repayAmount = "";
	private String adminRate = "";
	private String adminLimit = "";
	private String adminTerm = "";
	private Plate starterButton;

	private Plate borrowButton;
	private BorrowSlider slider;

	// Commodities and requests.
	private int listScroll;
	private long selectedListing = -1;
	private long selectedRequest = -1;
	private boolean sellAuction;
	private int auctionLength;
	private String sellQuantity = "1";
	private String sellPrice = "";
	private String dealAmount = "";
	private String search = "";
	private List<Item> picks = List.of();
	private Item pickedItem;
	private String wantQuantity = "1";
	private String wantPrice = "";
	private final List<Plate> rowPlates = new ArrayList<>();
	private final List<Plate> pickPlates = new ArrayList<>();
	private Plate dealButton;
	private Plate takeDownButton;
	private Plate collectButton;
	private Plate listButton;
	private Plate postButton;

	// Land.
	private int selectedChunkX = Integer.MIN_VALUE;
	private int selectedChunkZ;
	private String landPrice = "";
	private String trustName = "";
	private Plate landBuy;
	private Plate landSell;
	private Plate landAbandon;
	private Plate landAdmin;
	private Plate landZone;
	private Plate landBid;
	private Plate landCallOff;
	private Plate landAuction;
	private Plate landAuctionLength;
	private int landAuctionIndex = 1;
	/** The company the Commodities form sells for (0: yourself). */
	private long sellAsCorp;
	private String issueQuantity = "";
	private String issuePrice = "";
	/** Admin settings page: first row shown, and what was typed per setting. */
	private int settingsScroll;
	private final java.util.Map<String, String> settingInputs = new java.util.HashMap<>();

	// Equities. Mode 0: the market; 1: chartering; 2: editing a company's logo.
	private int equityMode;
	private int corpView;
	private int corpScroll;
	private long selectedCorp = -1;
	private String charterName = "";
	private String charterTicker = "";
	private String charterShares = "1000";
	private double charterOffer = 0.25;
	private OfferSlider offerSlider;
	private EditBox offerBox;
	private boolean syncingOffer;
	private String charterPrice = "";
	private String orderQuantity = "";
	private String orderPrice = "";
	private String dividendAmount = "";
	private String logoPath = "";
	private byte[] logo = blankLogo();
	private boolean logoUploaded;
	private int brush = 0x30;
	private String clientNotice = "";
	/** True while extractBackground draws the page under the widgets. */
	private boolean backPass;

	public ExchangeScreen(FinanceSnapshotPayload data) {
		super(Component.literal("The Jugcraft Exchange"));
		this.data = data;
	}

	/** New numbers from the server. Rebuilds the widgets only when what they show has changed. */
	public void update(FinanceSnapshotPayload fresh) {
		if (!fresh.notice().isEmpty()) {
			clientNotice = "";
		}
		boolean rebuild = fresh.admin() != data.admin() || fresh.contracts().size() != data.contracts().size()
				|| activeCount(fresh) != activeCount(data);
		data = fresh;
		if (rebuild) {
			rebuildWidgets();
		} else {
			refreshButtons();
		}
	}

	private static long activeCount(FinanceSnapshotPayload snapshot) {
		return snapshot.contracts().stream().filter(FinanceSnapshotPayload.ContractView::active).count();
	}

	/** Like the inventory, the Exchange does not pause a singleplayer world, so its server keeps answering. */
	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public void tick() {
		super.tick();
		if (++ticks % REFRESH_TICKS == 0) {
			ClientPlayNetworking.send(FinanceActionPayload.simple(Action.REFRESH));
		}
	}

	private static void send(Action action, String target, long amount, int period, String note) {
		send(action, target, amount, period, note, 0);
	}

	private static void send(Action action, String target, long amount, int period, String note, long extra) {
		ClientPlayNetworking.send(new FinanceActionPayload(action, target, amount, period, note, extra));
	}

	// ------------------------------------------------------------------ layout

	private int contentTop() {
		return top + FRAME + HEADER + TAB_HEIGHT + 10;
	}

	private int contentBottom() {
		return top + panelHeight - FRAME - 18;
	}

	private int leftColumn() {
		return left + 10;
	}

	/** Width of the left column: half the page, but a narrow form beside a wide list on the market tabs, and just the map on Land. */
	private int columnWidth() {
		int inner = panelWidth - 30;
		return switch (tab) {
			case COMMODITIES, REQUESTS -> Math.min(inner - 120, Math.max(140, inner / 3));
			case LAND -> MAP_CELLS * cell() + 14;
			default -> inner / 2;
		};
	}

	/** Width of the right column: whatever the left column leaves. */
	private int rightWidth() {
		return panelWidth - 30 - columnWidth();
	}

	private int rightColumn() {
		return left + 20 + columnWidth();
	}

	/** Size of one chunk on the land map: as big as the page's height allows, leaving the Land Office 200 wide. */
	private int cell() {
		int byHeight = (contentBottom() - contentTop() - 8) / MAP_CELLS;
		int byWidth = (panelWidth - 30 - 200 - 14) / MAP_CELLS;
		return Math.max(9, Math.min(16, Math.min(byHeight, byWidth)));
	}

	/** Item picker columns that fit the request form. */
	private int pickColumns() {
		return Math.max(4, Math.min(9, columnWidth() / 18));
	}

	private int sliderY() {
		return contentTop() + 74;
	}

	// ------------------------------------------------------------------ widgets

	@Override
	protected void init() {
		panelWidth = Math.min(MAX_WIDTH, width - 12);
		panelHeight = Math.min(MAX_HEIGHT, height - 12);
		left = (width - panelWidth) / 2;
		top = (height - panelHeight) / 2;
		layoutTabs();
		for (int i = 0; i < TABS.length; i++) {
			int index = i;
			Plate plate = addRenderableWidget(new Plate(tabX[i], top + FRAME + HEADER + 2, tabX[i + 1] - tabX[i] - 2,
					TAB_HEIGHT, TABS[i], i == tab ? Plate.Kind.TAB_OPEN : Plate.Kind.TAB, b -> openTab(index)));
			plate.active = i != tab;
		}
		plate(left + panelWidth - FRAME - 46, top + panelHeight - FRAME - 13, 42, 12, "Guide", Plate.Kind.WOOD,
				b -> Minecraft.getInstance().gui.setScreen(new ExchangeGuideScreen(this)));
		borrowButton = null;
		starterButton = null;
		slider = null;
		dealButton = null;
		takeDownButton = null;
		collectButton = null;
		listButton = null;
		postButton = null;
		landBuy = null;
		landSell = null;
		landAbandon = null;
		landAdmin = null;
		landZone = null;
		rowPlates.clear();
		pickPlates.clear();
		switch (tab) {
			case BANKING -> {
				if (data.starterAvailable()) {
					starterButton = plate(leftColumn(), contentTop() + 102, columnWidth(), 15,
							"Take starter loan: " + FinanceBook.amount(data.borrowLimit()), Plate.Kind.BRASS,
							b -> send(Action.STARTER_LOAN, "", 0, 0, ""));
				}
			}
			case ECONOMY -> initEconomy();
			case CONTRACTS -> initContracts();
			case COMMODITIES -> initCommodities();
			case REQUESTS -> initRequests();
			case LAND -> initLand();
			case EQUITIES -> initEquities();
			default -> {
			}
		}
		refreshButtons();
	}

	private void openTab(int index) {
		tab = index;
		equityMode = 0;
		clientNotice = "";
		adminView = false;
		contractScroll = 0;
		listScroll = 0;
		rebuildWidgets();
	}

	private void layoutTabs() {
		int[] widths = new int[TABS.length];
		int total = 0;
		for (int i = 0; i < TABS.length; i++) {
			widths[i] = font.width(TABS[i]);
			total += widths[i];
		}
		int padding = Math.max(6, Math.min(16, (panelWidth - 2 * FRAME - 12 - total) / TABS.length));
		int x = left + FRAME + 6;
		for (int i = 0; i < TABS.length; i++) {
			tabX[i] = x;
			x += widths[i] + padding;
		}
		tabX[TABS.length] = x;
	}

	private EditBox box(int x, int y, int w, String value, int max, String hint, Consumer<String> onChange) {
		EditBox box = new EditBox(font, x, y, w, 14, Component.literal(hint));
		box.setMaxLength(max);
		box.setValue(value);
		box.setHint(Component.literal(hint).withStyle(ChatFormatting.DARK_GRAY));
		box.setResponder(text -> {
			onChange.accept(text);
			refreshButtons();
		});
		return addRenderableWidget(box);
	}

	private Plate plate(int x, int y, int w, int h, String label, Plate.Kind kind, Button.OnPress press) {
		return addRenderableWidget(new Plate(x, y, w, h, label, kind, press));
	}

	private void initEconomy() {
		int x = leftColumn();
		int y = contentTop();
		int w = columnWidth();
		if (data.admin()) {
			int tx = adminView ? x + settingsWidth() - 44 : x + w - 44;
			plate(tx, y - 2, 44, 12, adminView ? "< Back" : "Admin", Plate.Kind.WOOD, b -> {
				adminView = !adminView;
				rebuildWidgets();
			});
		}
		if (adminView && data.admin()) {
			initSettings();
			return;
		}
		slider = addRenderableWidget(new BorrowSlider(x + 4, sliderY(), w - 8, 12));
		borrowButton = plate(x + 4, sliderY() + 16, w - 8, 14, "Borrow", Plate.Kind.BRASS, b -> {
			long amount = borrowAmount();
			if (amount > 0) {
				send(Action.BORROW, "", amount, 0, "");
			}
		});
		box(x + 34, y + 118, w - 34 - 46, repayAmount, 16, "Amount", v -> repayAmount = v);
		plate(x + w - 42, y + 118, 42, 14, "Repay", Plate.Kind.WOOD, b -> {
			long amount = FinanceBook.parseAmount(repayAmount);
			if (amount > 0) {
				send(Action.REPAY, "", amount, 0, "");
			}
		});
	}

	private void initContracts() {
		int x = leftColumn();
		int y = contentTop();
		int w = columnWidth();
		box(x, y + 22, w, payTo, 16, "Player name", v -> payTo = v);
		box(x, y + 50, w - 52, payAmount, 16, "Amount", v -> payAmount = v);
		plate(x + w - 48, y + 50, 48, 14, PERIODS[payPeriod], Plate.Kind.WOOD, b -> {
			payPeriod = (payPeriod + 1) % PERIODS.length;
			b.setMessage(Component.literal(PERIODS[payPeriod]));
		});
		box(x, y + 78, w, payNote, FinanceBook.MAX_NOTE, "e.g. an electricity connection", v -> payNote = v);
		plate(x, y + 98, w, 15, "Sign & Pay", Plate.Kind.BRASS, b -> {
			long amount = FinanceBook.parseAmount(payAmount);
			if (amount > 0 && !payTo.isBlank()) {
				send(Action.PAY, payTo.trim(), amount, payPeriod, payNote);
			}
		});

		List<FinanceSnapshotPayload.ContractView> rows = data.contracts();
		int rx = rightColumn();
		int visible = visibleContractRows();
		contractScroll = Math.max(0, Math.min(contractScroll, Math.max(0, rows.size() - visible)));
		for (int i = 0; i < visible && contractScroll + i < rows.size(); i++) {
			FinanceSnapshotPayload.ContractView row = rows.get(contractScroll + i);
			if (row.active() && row.period() != FinanceBook.Period.ONCE.ordinal()) {
				long id = row.id();
				plate(rx + columnWidth() - 28, contractRowY(i) + 2, 24, 11, "End", Plate.Kind.WOOD,
						b -> send(Action.CANCEL_CONTRACT, "", id, 0, ""));
			}
		}
	}

	private int visibleContractRows() {
		return Math.max(1, (contentBottom() - contentTop() - 16) / CONTRACT_ROW);
	}

	private int contractRowY(int index) {
		return contentTop() + 14 + index * CONTRACT_ROW;
	}

	private long borrowAmount() {
		long most = data.borrowable();
		if (most <= 0) {
			return 0;
		}
		long amount = Math.round(borrowFraction * most);
		if (most >= 50) {
			amount = amount / 5 * 5; // Steps of 5 Jugs on the slider; the far end reaches the full amount.
			if (borrowFraction >= 0.999) {
				amount = most;
			}
		}
		return Math.max(0, Math.min(most, amount));
	}

	private void refreshButtons() {
		refreshLandButtons();
		if (borrowButton != null) {
			long amount = borrowAmount();
			borrowButton.active = amount > 0 && !data.overdue();
			long owe = amount + FinanceBook.percentOf(amount, data.feeBp());
			borrowButton.setMessage(Component.literal(amount > 0 ? "Borrow (repay " + FinanceBook.amount(owe) + ")" : "Borrow"));
		}
		refreshMarketButtons();
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		if (tab == ECONOMY && adminView && data.admin()) {
			int before = settingsScroll;
			settingsScroll = Math.max(0, Math.min(settingsScroll - (int) Math.signum(scrollY),
					Math.max(0, settingRows().size() - visibleSettingRows())));
			if (settingsScroll != before) {
				rebuildWidgets();
			}
			return true;
		}
		if (tab == EQUITIES && equityMode == 0 && mouseX < rightColumn()) {
			int before = corpScroll;
			corpScroll = Math.max(0, Math.min(corpScroll - (int) Math.signum(scrollY),
					Math.max(0, data.equities().corps().size() - corpRows())));
			if (corpScroll != before) {
				rebuildWidgets();
			}
			return true;
		}
		if ((tab == COMMODITIES || tab == REQUESTS) && mouseX >= rightColumn()) {
			int size = tab == COMMODITIES ? data.listings().size() : data.requests().size();
			listScroll = Math.max(0, Math.min(listScroll - (int) Math.signum(scrollY), Math.max(0, size - visibleRows())));
			refreshButtons();
			return true;
		}
		if (tab == CONTRACTS && mouseX >= rightColumn()) {
			int before = contractScroll;
			contractScroll = Math.max(0, Math.min(contractScroll - (int) Math.signum(scrollY),
					Math.max(0, data.contracts().size() - visibleContractRows())));
			if (contractScroll != before) {
				rebuildWidgets();
			}
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
	}

	// ------------------------------------------------------------------ commodities and requests

	private int listTop() {
		return contentTop() + 14;
	}

	private int listBottom() {
		return contentBottom() - 34;
	}

	private int visibleRows() {
		return Math.max(1, (listBottom() - listTop()) / ROW);
	}

	private int rowY(int index) {
		return listTop() + index * ROW;
	}

	private int detailY() {
		return listBottom() + 4;
	}

	private FinanceSnapshotPayload.ListingView selectedListingView() {
		for (FinanceSnapshotPayload.ListingView l : data.listings()) {
			if (l.id() == selectedListing) {
				return l;
			}
		}
		return null;
	}

	private FinanceSnapshotPayload.RequestView selectedRequestView() {
		for (FinanceSnapshotPayload.RequestView r : data.requests()) {
			if (r.id() == selectedRequest) {
				return r;
			}
		}
		return null;
	}

	private static ItemStack hand() {
		LocalPlayer player = Minecraft.getInstance().player;
		return player == null ? ItemStack.EMPTY : player.getMainHandItem();
	}

	/** How many items like the one in hand the player carries (display only; the server counts again). */
	private static int carried(ItemStack template) {
		LocalPlayer player = Minecraft.getInstance().player;
		if (player == null || template.isEmpty()) {
			return 0;
		}
		int count = 0;
		for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
			ItemStack stack = player.getInventory().getItem(slot);
			if (ItemStack.isSameItemSameComponents(stack, template)) {
				count += stack.getCount();
			}
		}
		return count;
	}

	private static int parseCount(String text, int fallback) {
		try {
			return Integer.parseInt(text.trim());
		} catch (NumberFormatException e) {
			return fallback;
		}
	}

	/** One clickable slot per visible list row; scrolling changes which entry each slot shows. */
	private void addRowPlates(boolean listings) {
		int rx = rightColumn();
		for (int i = 0; i < visibleRows(); i++) {
			int slot = i;
			rowPlates.add(plate(rx + 1, rowY(i), rightWidth() - 2, ROW, "", Plate.Kind.ROW, b -> {
				int index = listScroll + slot;
				if (listings && index < data.listings().size()) {
					selectedListing = data.listings().get(index).id();
				} else if (!listings && index < data.requests().size()) {
					selectedRequest = data.requests().get(index).id();
				}
				dealAmount = "";
				rebuildWidgets();
			}));
		}
	}

	// ------------------------------------------------------------------ admin settings

	/** The settings page: group headings and settings, one per row, in a list that scrolls. */
	private List<Object> settingRows() {
		List<Object> rows = new ArrayList<>();
		String group = "";
		for (FinanceSnapshotPayload.SettingView setting : data.settings()) {
			if (!setting.group().equals(group)) {
				group = setting.group();
				rows.add(group);
			}
			rows.add(setting);
		}
		return rows;
	}

	private static final int SETTING_ROW = 15;

	private int settingsTop() {
		return contentTop() + 14;
	}

	private int visibleSettingRows() {
		return Math.max(1, (contentBottom() - settingsTop()) / SETTING_ROW);
	}

	private int settingsWidth() {
		return panelWidth - 20;
	}

	private void initSettings() {
		List<Object> rows = settingRows();
		settingsScroll = Math.max(0, Math.min(settingsScroll, rows.size() - visibleSettingRows()));
		int x = leftColumn();
		int w = settingsWidth();
		for (int i = 0; i < visibleSettingRows() && settingsScroll + i < rows.size(); i++) {
			if (!(rows.get(settingsScroll + i) instanceof FinanceSnapshotPayload.SettingView setting)) {
				continue;
			}
			int y = settingsTop() + i * SETTING_ROW;
			box(x + w - 112, y, 72, settingInputs.getOrDefault(setting.key(), ""), 12, unitHint(setting.unit()),
					v -> settingInputs.put(setting.key(), v));
			plate(x + w - 36, y, 36, 14, "Set", Plate.Kind.BRASS, b -> {
				String typed = settingInputs.getOrDefault(setting.key(), "").trim();
				if (!typed.isEmpty()) {
					send(Action.ADMIN_SET, setting.key(), 0, 0, typed);
					settingInputs.remove(setting.key());
					rebuildWidgets();
				}
			});
		}
	}

	private static String unitHint(int unit) {
		return switch (io.github.jimbozoomer.jugcraft.finance.ExchangeSettings.Unit.values()[Math.max(0, Math.min(5, unit))]) {
			case JUGS -> "Jugs";
			case PERCENT -> "%";
			case DAYS -> "days";
			case COUNT -> "number";
			case TIMES -> "times";
			case TOGGLE -> "on / off";
		};
	}

	private void drawSettings(GuiGraphicsExtractor g) {
		int x = leftColumn();
		int y = contentTop();
		int w = settingsWidth();
		heading(g, "Central Bank: Server Settings", x, y, w - 48);
		slate(g, x, settingsTop() - 2, x + w, contentBottom());
		List<Object> rows = settingRows();
		for (int i = 0; i < visibleSettingRows() && settingsScroll + i < rows.size(); i++) {
			Object row = rows.get(settingsScroll + i);
			int ry = settingsTop() + i * SETTING_ROW;
			if (row instanceof String group) {
				text(g, group.toUpperCase(java.util.Locale.ROOT), x + 4, ry + 4, GOLD, false);
				paint(g, x + 8 + font.width(group.toUpperCase(java.util.Locale.ROOT)), ry + 8, x + w - 4, ry + 9, GOLD_DIM);
				continue;
			}
			FinanceSnapshotPayload.SettingView setting = (FinanceSnapshotPayload.SettingView) row;
			if (i % 2 == 1) {
				paint(g, x + 1, ry - 1, x + w - 1, ry + SETTING_ROW - 1, SLATE_STRIPE);
			}
			String value = io.github.jimbozoomer.jugcraft.finance.ExchangeSettings.show(
					io.github.jimbozoomer.jugcraft.finance.ExchangeSettings.Unit.values()[Math.max(0, Math.min(5, setting.unit()))],
					setting.value());
			int valueX = x + w - 118 - font.width(value);
			text(g, font.plainSubstrByWidth(setting.label(), valueX - x - 10), x + 4, ry + 3, CREAM, false);
			text(g, value, valueX, ry + 3, GAIN, false);
		}
		if (settingsScroll > 0) {
			text(g, "^ scroll", x + w / 2 - 16, settingsTop() - 11, DIM, false);
		}
		if (settingsScroll + visibleSettingRows() < rows.size()) {
			text(g, "more below (scroll)", x + w / 2 - 40, contentBottom() + 2, DIM, false);
		}
	}

	private void initCommodities() {
		int x = leftColumn();
		int y = contentTop();
		int w = columnWidth();
		int half = w / 2 - 2;
		plate(x, y + 44, half, 13, sellAuction ? "Auction" : "Fixed price", Plate.Kind.WOOD, b -> {
			sellAuction = !sellAuction;
			rebuildWidgets();
		});
		if (sellAuction) {
			plate(x + half + 4, y + 44, half, 13, AUCTION_LENGTHS[auctionLength], Plate.Kind.WOOD, b -> {
				auctionLength = (auctionLength + 1) % AUCTION_LENGTHS.length;
				b.setMessage(Component.literal(AUCTION_LENGTHS[auctionLength]));
			});
		}
		box(x, y + 69, half, sellQuantity, 6, "1", v -> sellQuantity = v);
		box(x + half + 4, y + 69, half, sellPrice, 16, "Jugs", v -> sellPrice = v);
		List<FinanceSnapshotPayload.CorpView> mine = myCompanies();
		if (mine.stream().noneMatch(c -> c.id() == sellAsCorp)) {
			sellAsCorp = 0;
		}
		int listW = mine.isEmpty() ? w : w - 60;
		listButton = plate(x, y + 87, listW, 14, "List for sale", Plate.Kind.BRASS, b -> {
			long price = FinanceBook.parseAmount(sellPrice);
			int quantity = parseCount(sellQuantity, 0);
			if (price > 0 && quantity > 0) {
				send(Action.LIST_ITEM, String.valueOf(auctionLength), price, sellAuction ? 1 : 0,
						sellAsCorp > 0 ? String.valueOf(sellAsCorp) : "", quantity);
			}
		});
		if (!mine.isEmpty()) {
			// Who sells: you, or one of the companies you control (its treasury gets the money).
			plate(x + w - 56, y + 87, 56, 14, "As: " + sellAsLabel(), Plate.Kind.WOOD, b -> {
				int at = 0;
				for (int i = 0; i < mine.size(); i++) {
					if (mine.get(i).id() == sellAsCorp) {
						at = i + 1;
					}
				}
				sellAsCorp = at >= mine.size() ? 0 : mine.get(at).id();
				rebuildWidgets();
			});
		}
		collectButton = plate(x + w - 48, y + 107, 44, 12, "Collect", Plate.Kind.BRASS, b -> send(Action.COLLECT, "", 0, 0, ""));

		addRowPlates(true);
		int rx = rightColumn();
		int rw = rightWidth();
		int dy = detailY() + 11;
		box(rx, dy, rw - 96, dealAmount, 16, "", v -> dealAmount = v);
		dealButton = plate(rx + rw - 92, dy, 44, 14, "Buy", Plate.Kind.BRASS, b -> {
			FinanceSnapshotPayload.ListingView sel = selectedListingView();
			if (sel == null) {
				return;
			}
			if (sel.auction()) {
				long bid = FinanceBook.parseAmount(dealAmount);
				send(Action.BID, "", sel.id(), 0, "", bid > 0 ? bid : sel.minBid());
			} else {
				send(Action.BUY, "", sel.id(), 0, "", Math.max(1, parseCount(dealAmount, 1)));
			}
		});
		takeDownButton = plate(rx + rw - 46, dy, 46, 14, "Remove", Plate.Kind.WOOD, b -> {
			if (selectedListingView() != null) {
				send(Action.CANCEL_LISTING, "", selectedListing, 0, "");
			}
		});
	}

	/** Companies this player controls. */
	private List<FinanceSnapshotPayload.CorpView> myCompanies() {
		List<FinanceSnapshotPayload.CorpView> out = new ArrayList<>();
		for (FinanceSnapshotPayload.CorpView c : data.equities().corps()) {
			if (c.controlledByMe()) {
				out.add(c);
			}
		}
		return out;
	}

	private String sellAsLabel() {
		for (FinanceSnapshotPayload.CorpView c : data.equities().corps()) {
			if (c.id() == sellAsCorp) {
				return c.ticker();
			}
		}
		return "me";
	}

	private void updatePicks() {
		String query = search.trim().toLowerCase(java.util.Locale.ROOT);
		if (query.length() < 2) {
			picks = List.of();
		} else {
			List<Item> found = new ArrayList<>();
			for (Item item : BuiltInRegistries.ITEM) {
				if (item == Items.AIR) {
					continue;
				}
				if (new ItemStack(item).getHoverName().getString().toLowerCase(java.util.Locale.ROOT).contains(query)) {
					found.add(item);
					if (found.size() >= pickColumns() * PICK_ROWS) {
						break;
					}
				}
			}
			picks = found;
		}
		refreshButtons();
	}

	private void initRequests() {
		int x = leftColumn();
		int y = contentTop();
		int w = columnWidth();
		int half = w / 2 - 2;
		EditBox searchBox = box(x, y + 13, w, search, 32, "Search items...", v -> {
			search = v;
			updatePicks();
		});
		searchBox.setFocused(false);
		for (int r = 0; r < PICK_ROWS; r++) {
			for (int c = 0; c < pickColumns(); c++) {
				int index = r * pickColumns() + c;
				pickPlates.add(plate(x + c * 18, y + 30 + r * 18, 18, 18, "", Plate.Kind.CELL, b -> {
					if (index < picks.size()) {
						pickedItem = picks.get(index);
						refreshButtons();
					}
				}));
			}
		}
		box(x, y + 90, half, wantQuantity, 6, "1", v -> wantQuantity = v);
		box(x + half + 4, y + 90, half, wantPrice, 16, "advert", v -> wantPrice = v);
		postButton = plate(x, y + 108, w, 14, "Post request", Plate.Kind.BRASS, b -> {
			int quantity = parseCount(wantQuantity, 0);
			long price = wantPrice.isBlank() ? 0 : FinanceBook.parseAmount(wantPrice);
			if (pickedItem != null && quantity > 0 && price >= 0) {
				send(Action.POST_REQUEST, BuiltInRegistries.ITEM.getKey(pickedItem).toString(), price, 0, "", quantity);
			}
		});

		addRowPlates(false);
		int rx = rightColumn();
		int rw = rightWidth();
		int dy = detailY() + 11;
		box(rx, dy, rw - 96, dealAmount, 6, "", v -> dealAmount = v);
		dealButton = plate(rx + rw - 92, dy, 44, 14, "Fill", Plate.Kind.BRASS, b -> {
			FinanceSnapshotPayload.RequestView sel = selectedRequestView();
			if (sel != null) {
				send(Action.FILL_REQUEST, "", sel.id(), 0, "", Math.max(1, parseCount(dealAmount, sel.quantity())));
			}
		});
		takeDownButton = plate(rx + rw - 46, dy, 46, 14, "Withdraw", Plate.Kind.WOOD, b -> {
			if (selectedRequestView() != null) {
				send(Action.CANCEL_REQUEST, "", selectedRequest, 0, "");
			}
		});
		updatePicks();
	}

	// ------------------------------------------------------------------ equities

	private static byte[] blankLogo() {
		byte[] blank = new byte[Equities.LOGO_PIXELS];
		java.util.Arrays.fill(blank, Equities.CLEAR);
		return blank;
	}

	private FinanceSnapshotPayload.CorpView selectedCorpView() {
		for (FinanceSnapshotPayload.CorpView corp : data.equities().corps()) {
			if (corp.id() == selectedCorp) {
				return corp;
			}
		}
		List<FinanceSnapshotPayload.CorpView> corps = data.equities().corps();
		return corps.isEmpty() ? null : corps.get(0);
	}

	private int corpRows() {
		return Math.max(1, (contentBottom() - contentTop() - 14 - 18) / CORP_ROW);
	}

	private byte[] logoData() {
		byte[] out = java.util.Arrays.copyOf(logo, Equities.LOGO_PIXELS + 1);
		out[Equities.LOGO_PIXELS] = (byte) (logoUploaded ? 1 : 0);
		return out;
	}

	private static void sendData(Action action, String target, long amount, int period, String note, long extra, byte[] data) {
		ClientPlayNetworking.send(new FinanceActionPayload(action, target, amount, period, note, extra, data));
	}

	private void initEquities() {
		if (equityMode == 1 || equityMode == 2) {
			initCharter();
			return;
		}
		int x = leftColumn();
		int w = columnWidth();
		List<FinanceSnapshotPayload.CorpView> corps = data.equities().corps();
		corpScroll = Math.max(0, Math.min(corpScroll, Math.max(0, corps.size() - corpRows())));
		for (int i = 0; i < corpRows() && corpScroll + i < corps.size(); i++) {
			long id = corps.get(corpScroll + i).id();
			plate(x + 1, contentTop() + 14 + i * CORP_ROW, w - 2, CORP_ROW, "", Plate.Kind.ROW, b -> {
				selectedCorp = id;
				rebuildWidgets();
			});
		}
		plate(x, contentBottom() - 15, w, 15, "Charter a company: " + FinanceBook.amount(data.equities().charterFee()),
				Plate.Kind.BRASS, b -> {
					equityMode = 1;
					logo = blankLogo();
					logoUploaded = false;
					rebuildWidgets();
				});
		FinanceSnapshotPayload.CorpView corp = selectedCorpView();
		if (corp == null) {
			return;
		}
		selectedCorp = corp.id();
		int rx = rightColumn();
		int y = contentTop();
		int third = (w - 4) / 3;
		String[] views = {"Overview", "Trade", "Manage"};
		for (int v = 0; v < 3; v++) {
			int view = v;
			if (v == 2 && !corp.controlledByMe() && !data.admin()) {
				continue;
			}
			Plate tabPlate = plate(rx + v * (third + 2), y + 12, third, 12, views[v], v == corpView ? Plate.Kind.TAB_OPEN : Plate.Kind.WOOD, b -> {
				corpView = view;
				rebuildWidgets();
			});
			tabPlate.active = v != corpView;
		}
		if (corpView == 2 && !corp.controlledByMe() && !data.admin()) {
			corpView = 0;
		}
		if (corpView == 1) {
			int q = (w - 4) * 3 / 10;
			box(rx, y + 93, q, orderQuantity, 9, "Shares", v -> orderQuantity = v);
			box(rx + q + 2, y + 93, q, orderPrice, 16, "Price", v -> orderPrice = v);
			int bw = (w - 2 * q - 8) / 2;
			plate(rx + 2 * q + 4, y + 93, bw, 14, "Buy", Plate.Kind.BRASS, b -> order(corp, true));
			plate(rx + 2 * q + 6 + bw, y + 93, bw, 14, "Sell", Plate.Kind.WOOD, b -> order(corp, false));
			for (int i = 0; i < 3 && i < corp.myOrders().size(); i++) {
				long orderId = corp.myOrders().get(i).id();
				plate(rx + w - 38, y + 120 + i * 11, 38, 10, "Cancel", Plate.Kind.WOOD,
						b -> send(Action.CORP_CANCEL, "", orderId, 0, ""));
			}
		} else if (corpView == 2) {
			if (corp.controlledByMe()) {
				box(rx, y + 40, w - 50, dividendAmount, 16, "Amount", v -> dividendAmount = v);
				plate(rx + w - 46, y + 40, 46, 14, "Pay", Plate.Kind.BRASS, b -> {
					long amount = FinanceBook.parseAmount(dividendAmount);
					if (amount > 0) {
						send(Action.CORP_DIVIDEND, "", corp.id(), 0, "", amount);
					}
				});
				int q = (w - 50) / 2;
				box(rx, y + 68, q - 2, issueQuantity, 7, "Shares", v -> issueQuantity = v);
				box(rx + q, y + 68, q - 2, issuePrice, 12, "Price", v -> issuePrice = v);
				plate(rx + w - 46, y + 68, 46, 14, "Issue", Plate.Kind.BRASS, b -> {
					long quantity = parseCount(issueQuantity, 0);
					long price = FinanceBook.parseAmount(issuePrice);
					if (quantity <= 0 || price <= 0) {
						clientNotice = "Type how many new shares and the price for each.";
						return;
					}
					send(Action.CORP_ISSUE, "", corp.id(), 0, String.valueOf(quantity), price);
					issueQuantity = "";
					issuePrice = "";
					rebuildWidgets();
				});
				plate(rx, y + 104, w, 12, "Buy a Market Chest: " + FinanceBook.amount(data.equities().chestPrice()) + " Jugs",
						Plate.Kind.WOOD, b -> send(Action.CHEST_BUY, "", corp.id(), 0, ""));
				plate(rx, y + 88, w, 13, "Edit the logo", Plate.Kind.WOOD, b -> {
					equityMode = 2;
					logo = corp.logo().clone();
					logoUploaded = false;
					rebuildWidgets();
				});
			}
			if (data.admin() && corp.logoPending()) {
				int half = (w - 26) / 2;
				plate(rx + 22, y + 132, half, 12, "Approve", Plate.Kind.BRASS, b -> send(Action.ADMIN_LOGO, "", corp.id(), 1, ""));
				plate(rx + 26 + half, y + 132, half, 12, "Remove", Plate.Kind.WOOD, b -> send(Action.ADMIN_LOGO, "", corp.id(), 0, ""));
			}
		}
	}

	private void order(FinanceSnapshotPayload.CorpView corp, boolean buy) {
		long quantity = parseCount(orderQuantity, 0);
		long price = FinanceBook.parseAmount(orderPrice);
		if (quantity <= 0 || price <= 0) {
			clientNotice = "Type how many shares and the price for each.";
			return;
		}
		send(Action.CORP_ORDER, "", corp.id(), buy ? 1 : 0, String.valueOf(quantity), price);
	}

	private int logoX() {
		return rightColumn();
	}

	private int logoY() {
		return contentTop() + 5;
	}

	private void initCharter() {
		int x = leftColumn();
		int y = contentTop();
		int w = columnWidth();
		int half = (w - 4) / 2;
		if (equityMode == 1) {
			box(x, y + 22, w, charterName, 24, "Company name", v -> charterName = v);
			box(x, y + 46, half, charterTicker, 5, "TICKER", v -> charterTicker = v);
			box(x + half + 4, y + 46, half, charterShares, 7, "1000", v -> charterShares = v);
			offerSlider = addRenderableWidget(new OfferSlider(x, y + 74, w - 46, 12));
			offerBox = box(x + w - 42, y + 73, 42, percentText(charterOffer), 6, "%", v -> {
				if (syncingOffer) {
					return;
				}
				try {
					double percent = Double.parseDouble(v.trim().replace("%", ""));
					charterOffer = Math.max(0, Math.min(10_000, Math.round(percent * 100))) / 10_000.0;
					syncingOffer = true;
					offerSlider.set(charterOffer);
					syncingOffer = false;
				} catch (NumberFormatException ignored) {
					// keep the last good value while typing
				}
			});
			box(x, y + 98, w, charterPrice, 16, "Price per share", v -> charterPrice = v);
			plate(x, y + 118, w - 50, 15, "Charter", Plate.Kind.BRASS, b -> {
				long shares = parseCount(charterShares, 0);
				long price = charterPrice.isBlank() ? 0 : FinanceBook.parseAmount(charterPrice);
				int offerBp = (int) Math.round(charterOffer * 10_000);
				if (offerBp > 0 && price <= 0) {
					clientNotice = "Set a price per share for the shares you offer.";
					return;
				}
				sendData(Action.CORP_CHARTER, charterName.trim(), shares, offerBp, charterTicker.trim(), Math.max(0, price), logoData());
				equityMode = 0;
				rebuildWidgets();
			});
		} else {
			FinanceSnapshotPayload.CorpView corp = selectedCorpView();
			plate(x, y + 118, w - 50, 15, "Save logo", Plate.Kind.BRASS, b -> {
				if (corp != null) {
					sendData(Action.CORP_LOGO, "", corp.id(), 0, "", 0, logoData());
				}
				equityMode = 0;
				rebuildWidgets();
			});
		}
		plate(x + w - 46, y + 118, 46, 15, "Back", Plate.Kind.WOOD, b -> {
			equityMode = 0;
			rebuildWidgets();
		});
		// The logo editor: a 16x16 grid, the palette beside it, and loading a PNG.
		for (int i = 0; i < Equities.LOGO_PIXELS; i++) {
			int pixel = i;
			plate(logoX() + (i % 16) * LOGO_CELL, logoY() + (i / 16) * LOGO_CELL, LOGO_CELL, LOGO_CELL, "", Plate.Kind.MAP, b -> {
				logo[pixel] = (byte) brush;
			});
		}
		int px = logoX() + 16 * LOGO_CELL + 4;
		for (int c = 0; c <= 64; c++) {
			int colour = c;
			plate(px + (c % 8) * 7, logoY() + (c / 8) * 7, 7, 7, "", Plate.Kind.MAP, b -> brush = colour);
		}
		int rx = rightColumn();
		int rw = columnWidth();
		box(rx, logoY() + 116, rw - 50, logoPath, 256, "Path to a PNG file", v -> logoPath = v);
		plate(rx + rw - 46, logoY() + 116, 46, 14, "Load", Plate.Kind.WOOD, b -> loadLogo(java.nio.file.Path.of(logoPath.trim().replace("\"", ""))));
		plate(rx + rw - 46, logoY() + 132, 46, 12, "Clear", Plate.Kind.WOOD, b -> {
			logo = blankLogo();
			logoUploaded = false;
		});
	}

	/** Files dropped onto the window: the first PNG becomes the logo (only while chartering or editing a logo). */
	public void onFilesDrop(List<java.nio.file.Path> paths) {
		if (tab == EQUITIES && (equityMode == 1 || equityMode == 2) && !paths.isEmpty()) {
			loadLogo(paths.get(0));
		}
	}

	/** Reads an image, shrinks it to 16x16 and maps it onto the logo palette. Marks the logo as uploaded. */
	private void loadLogo(java.nio.file.Path path) {
		try {
			java.awt.image.BufferedImage image = javax.imageio.ImageIO.read(path.toFile());
			if (image == null) {
				clientNotice = "That file is not an image Minecraft can read (use a PNG).";
				return;
			}
			int w = image.getWidth();
			int h = image.getHeight();
			for (int y = 0; y < 16; y++) {
				for (int x = 0; x < 16; x++) {
					logo[y * 16 + x] = Equities.nearest(image.getRGB(Math.min(w - 1, x * w / 16 + w / 32), Math.min(h - 1, y * h / 16 + h / 32)));
				}
			}
			logoUploaded = true;
			clientNotice = "Logo loaded. Uploaded logos show once an admin approves them.";
		} catch (Exception e) {
			clientNotice = "Could not read that file: " + e.getMessage();
		}
	}

	/** Draws a logo with each pixel {@code scale} wide, joining runs of one colour. */
	private void drawLogo(GuiGraphicsExtractor g, byte[] pixels, int x, int y, int scale) {
		if (backPass) {
			return;
		}
		for (int row = 0; row < 16; row++) {
			int col = 0;
			while (col < 16) {
				byte c = pixels[row * 16 + col];
				int end = col + 1;
				while (end < 16 && pixels[row * 16 + end] == c) {
					end++;
				}
				if (c >= 0 && c < Equities.CLEAR) {
					g.fill(x + col * scale, y + row * scale, x + end * scale, y + (row + 1) * scale, Equities.paletteColor(c));
				}
				col = end;
			}
		}
	}

	/** A pie of shareholdings, drawn as horizontal spans. */
	private void drawPie(GuiGraphicsExtractor g, List<FinanceSnapshotPayload.Holding> holders, long total, int cx, int cy, int r) {
		if (!backPass || total <= 0) {
			return;
		}
		double[] ends = new double[holders.size()];
		double sum = 0;
		for (int i = 0; i < holders.size(); i++) {
			sum += (double) holders.get(i).shares() / total;
			ends[i] = sum;
		}
		g.fill(cx - r - 1, cy - r - 1, cx + r + 2, cy + r + 2, 0);
		for (int dy = -r; dy <= r; dy++) {
			int runStart = Integer.MIN_VALUE;
			int runColor = 0;
			for (int dx = -r; dx <= r + 1; dx++) {
				int color = 0;
				if (dx <= r && dx * dx + dy * dy <= r * r) {
					double angle = (Math.atan2(dx, -dy) / (2 * Math.PI) + 1) % 1;
					color = SLATE_LIGHT;
					for (int i = 0; i < ends.length; i++) {
						if (angle < ends[i]) {
							color = PIE_COLORS[i % PIE_COLORS.length];
							break;
						}
					}
				}
				if (color != runColor) {
					if (runColor != 0) {
						g.fill(cx + runStart, cy + dy, cx + dx, cy + dy + 1, runColor);
					}
					runStart = dx;
					runColor = color;
				}
			}
		}
	}

	private static String percentText(long part, long whole) {
		return whole <= 0 ? "0%" : String.format(Locale.ROOT, "%.1f%%", 100.0 * part / whole);
	}

	private void drawEquities(GuiGraphicsExtractor g) {
		if (equityMode == 1 || equityMode == 2) {
			drawCharter(g);
			return;
		}
		int x = leftColumn();
		int y = contentTop();
		int w = columnWidth();
		heading(g, "Listed Companies", x, y, w);
		List<FinanceSnapshotPayload.CorpView> corps = data.equities().corps();
		slate(g, x, y + 12, x + w, contentBottom() - 18);
		if (corps.isEmpty()) {
			paragraph(g, "No companies yet: charter the first.", x + 5, y + 18, w - 10, DIM, contentBottom() - 20);
		}
		FinanceSnapshotPayload.CorpView selected = selectedCorpView();
		for (int i = 0; i < corpRows() && corpScroll + i < corps.size(); i++) {
			FinanceSnapshotPayload.CorpView c = corps.get(corpScroll + i);
			int ry = y + 14 + i * CORP_ROW;
			if (selected != null && c.id() == selected.id()) {
				paint(g, x + 1, ry, x + w - 1, ry + CORP_ROW, BRASS_DARK);
			} else if (i % 2 == 1) {
				paint(g, x + 1, ry, x + w - 1, ry + CORP_ROW, SLATE_STRIPE);
			}
			drawLogo(g, c.logo(), x + 2, ry + 1, 1);
			String price = c.price() > 0 ? FinanceBook.amount(c.price()) : "-";
			text(g, price, x + w - 4 - font.width(price), ry + 1, CREAM, false);
			text(g, c.ticker(), x + 21, ry + 1, GOLD, false);
			text(g, font.plainSubstrByWidth(c.name(), w - 30 - font.width(c.ticker()) - font.width(price)), x + 25 + font.width(c.ticker()), ry + 1, CREAM, false);
			String sub = c.totalShares() + " shares" + (c.lastDividend() > 0 ? " · div " + FinanceBook.amount(c.lastDividend()) : "");
			text(g, font.plainSubstrByWidth(sub, w - 24), x + 21, ry + 10, DIM, false);
		}
		if (selected == null) {
			return;
		}
		int rx = rightColumn();
		heading(g, font.plainSubstrByWidth(selected.ticker() + " · " + selected.name(), w - 16), rx, y, w);
		int top = y + 28;
		switch (corpView) {
			case 1 -> drawTrade(g, selected, rx, top, w);
			case 2 -> drawManage(g, selected, rx, top, w);
			default -> drawOverview(g, selected, rx, top, w);
		}
	}

	private void drawOverview(GuiGraphicsExtractor g, FinanceSnapshotPayload.CorpView c, int rx, int y, int w) {
		slate(g, rx, y, rx + w, y + 38);
		paint(g, rx + 3, y + 3, rx + 35, y + 35, SLATE_DARK);
		drawLogo(g, c.logo(), rx + 3, y + 3, 2);
		text(g, "Price " + (c.price() > 0 ? FinanceBook.amount(c.price()) : "not traded"), rx + 40, y + 4, CREAM, false);
		String yield = c.price() > 0 && c.lastDividend() > 0
				? " (" + percentText(c.lastDividend(), c.price() * c.totalShares()) + ")" : "";
		text(g, font.plainSubstrByWidth("Dividend " + FinanceBook.amount(c.lastDividend()) + yield, w - 44), rx + 40, y + 14, DIM, false);
		text(g, font.plainSubstrByWidth("Treasury " + FinanceBook.amount(c.treasury()), w - 44), rx + 40, y + 24, DIM, false);
		if (c.logoPending()) {
			text(g, "logo awaits approval", rx + 40, y + 24, LOSS, false);
		}
		drawPie(g, c.holders(), c.totalShares(), rx + 18, y + 60, 16);
		int ly = y + 42;
		for (int i = 0; i < c.holders().size() && i < 7; i++) {
			FinanceSnapshotPayload.Holding h = c.holders().get(i);
			paint(g, rx + 40, ly + 1, rx + 46, ly + 7, PIE_COLORS[i % PIE_COLORS.length]);
			String share = percentText(h.shares(), c.totalShares());
			text(g, font.plainSubstrByWidth(h.name(), w - 56 - font.width(share)), rx + 49, ly, h.me() ? GOLD : CREAM, false);
			text(g, share, rx + w - font.width(share), ly, DIM, false);
			ly += 9;
		}
		int by = y + 106;
		text(g, font.plainSubstrByWidth("Controlled by " + c.controller(), w), rx, by, c.controlledByMe() ? GOLD : DIM, false);
		text(g, "You hold " + c.myShares() + " (" + percentText(c.myShares(), c.totalShares()) + ")", rx, by + 10, CREAM, false);
	}

	private void drawTrade(GuiGraphicsExtractor g, FinanceSnapshotPayload.CorpView c, int rx, int y, int w) {
		slate(g, rx, y, rx + w, y + 52);
		int half = w / 2;
		text(g, "Selling", rx + 4, y + 3, LOSS, false);
		text(g, "Buying", rx + half + 4, y + 3, GAIN, false);
		paint(g, rx + half, y + 2, rx + half + 1, y + 50, SLATE_LIGHT);
		for (int i = 0; i < 4; i++) {
			int ly = y + 13 + i * 9;
			if (i < c.asks().size()) {
				FinanceSnapshotPayload.Level l = c.asks().get(i);
				text(g, font.plainSubstrByWidth(l.quantity() + " @ " + FinanceBook.amount(l.price()), half - 6), rx + 4, ly, CREAM, false);
			}
			if (i < c.bids().size()) {
				FinanceSnapshotPayload.Level l = c.bids().get(i);
				text(g, font.plainSubstrByWidth(l.quantity() + " @ " + FinanceBook.amount(l.price()), half - 6), rx + half + 4, ly, CREAM, false);
			}
		}
		if (c.asks().isEmpty()) {
			text(g, "nothing", rx + 4, y + 13, DIM, false);
		}
		if (c.bids().isEmpty()) {
			text(g, "nothing", rx + half + 4, y + 13, DIM, false);
		}
		text(g, font.plainSubstrByWidth("You hold " + c.myShares() + " shares. Shares / price each:", w), rx, y + 55, DIM, false);
		text(g, "Your orders", rx, y + 83, DIM, false);
		if (c.myOrders().isEmpty()) {
			text(g, "none", rx + 60, y + 83, DIM, false);
		}
		for (int i = 0; i < 3 && i < c.myOrders().size(); i++) {
			FinanceSnapshotPayload.MyOrder o = c.myOrders().get(i);
			String line = (o.buy() ? "Buy " : "Sell ") + o.quantity() + " @ " + FinanceBook.amount(o.price()) + (o.company() ? " (co.)" : "");
			text(g, font.plainSubstrByWidth(line, w - 42), rx, y + 93 + i * 11, o.buy() ? GAIN : LOSS, false);
		}
	}

	private void drawManage(GuiGraphicsExtractor g, FinanceSnapshotPayload.CorpView c, int rx, int y, int w) {
		// y is contentTop + 28; the widgets above sit at contentTop + 40, + 68, + 88, + 104 and + 132.
		if (c.controlledByMe()) {
			text(g, font.plainSubstrByWidth("Dividend from treasury (" + FinanceBook.amount(c.treasury()) + ")", w), rx, y + 2, DIM, false);
			String fee = data.equities().issueFee() > 0 ? ", fee " + FinanceBook.amount(data.equities().issueFee()) : "";
			text(g, font.plainSubstrByWidth("Issue new shares" + fee + " (dilutes holders)", w), rx, y + 30, DIM, false);
		} else {
			text(g, "Admin view.", rx, y + 2, DIM, false);
		}
		if (data.admin() && c.logoPending()) {
			text(g, "Uploaded logo awaits approval:", rx, y + 92, LOSS, false);
			paint(g, rx, y + 102, rx + 18, y + 120, SLATE_DARK);
			drawLogo(g, c.logo(), rx + 1, y + 103, 1);
		}
	}

	private void drawCharter(GuiGraphicsExtractor g) {
		int x = leftColumn();
		int y = contentTop();
		int w = columnWidth();
		int half = (w - 4) / 2;
		if (equityMode == 1) {
			heading(g, "Charter a Company", x, y, w);
			text(g, "Name", x, y + 13, DIM, false);
			text(g, "Ticker", x, y + 37, DIM, false);
			text(g, "Shares", x + half + 4, y + 37, DIM, false);
			long shares = parseCount(charterShares, 0);
			long offered = shares * Math.round(charterOffer * 10_000) / 10_000;
			text(g, font.plainSubstrByWidth("Offer " + percentText(charterOffer) + "% = " + offered + " shares (type any %)", w), x, y + 64, CREAM, false);
			text(g, "Price per offered share", x, y + 89, DIM, false);
			text(g, font.plainSubstrByWidth("Fee " + FinanceBook.amount(data.equities().charterFee()) + ". You keep the rest.", w),
					x, y + 137, DIM, false);
		} else {
			FinanceSnapshotPayload.CorpView corp = selectedCorpView();
			heading(g, "Logo for " + (corp == null ? "?" : corp.ticker()), x, y, w);
			paragraph(g, "Click the grid to paint with the colour picked on the right. Load or drop a PNG to upload one: "
					+ "uploaded logos show once an admin approves them.", x, y + 14, w, DIM, y + 110);
		}
		int lx = logoX();
		int ly = logoY();
		paint(g, lx - 1, ly - 1, lx + 16 * LOGO_CELL + 1, ly + 16 * LOGO_CELL + 1, GOLD_DIM);
		paint(g, lx, ly, lx + 16 * LOGO_CELL, ly + 16 * LOGO_CELL, SLATE_DARK);
		if (backPass) {
			for (int i = 0; i < Equities.LOGO_PIXELS; i++) {
				byte c = logo[i];
				int cx = lx + (i % 16) * LOGO_CELL;
				int cy = ly + (i / 16) * LOGO_CELL;
				g.fill(cx, cy, cx + LOGO_CELL, cy + LOGO_CELL, c >= 0 && c < Equities.CLEAR ? Equities.paletteColor(c)
						: ((i / 16 + i % 16) % 2 == 0 ? 0xFF2A2724 : 0xFF221F1C));
			}
			int px = lx + 16 * LOGO_CELL + 4;
			for (int c = 0; c <= 64; c++) {
				int cx = px + (c % 8) * 7;
				int cy = ly + (c / 8) * 7;
				g.fill(cx, cy, cx + 7, cy + 7, c < 64 ? Equities.paletteColor(c) : 0xFF2A2724);
				if (c == 64) {
					g.fill(cx + 1, cy + 3, cx + 6, cy + 4, LOSS);
				}
				if (c == brush) {
					g.fill(cx, cy, cx + 7, cy + 1, 0xFFFFFFFF);
					g.fill(cx, cy + 6, cx + 7, cy + 7, 0xFFFFFFFF);
				}
			}
		}
		text(g, "or drop a PNG on the window", lx, ly + 134, DIM, false);
		text(g, "Preview", lx + 16 * LOGO_CELL + 4, ly + 70, DIM, false);
		paint(g, lx + 16 * LOGO_CELL + 4, ly + 80, lx + 16 * LOGO_CELL + 40, ly + 116, SLATE_DARK);
		drawLogo(g, logo, lx + 16 * LOGO_CELL + 6, ly + 82, 2);
		drawLogo(g, logo, lx + 16 * LOGO_CELL + 42, ly + 82, 1);
	}

	/** The charter form's "offer to the public" slider. */
	/** A share of 1 as a percentage with up to two decimals: 0.125 -> "12.5". */
	private static String percentText(double share) {
		long hundredths = Math.round(share * 10_000);
		String text = (hundredths / 100) + (hundredths % 100 == 0 ? "" : String.format(Locale.ROOT, ".%02d", hundredths % 100));
		return text.endsWith("0") && text.contains(".") ? text.substring(0, text.length() - 1) : text;
	}

	private final class OfferSlider extends AbstractSliderButton {
		OfferSlider(int x, int y, int width, int height) {
			super(x, y, width, height, Component.empty(), charterOffer);
			updateMessage();
		}

		void set(double offer) {
			value = Math.max(0, Math.min(1, offer));
			updateMessage();
		}

		@Override
		protected void updateMessage() {
			setMessage(Component.literal("Offer " + percentText(charterOffer) + "%"));
		}

		@Override
		protected void applyValue() {
			charterOffer = Math.round(value * 100) / 100.0; // 1% steps by dragging; type in the box for finer
			if (offerBox != null && !syncingOffer) {
				syncingOffer = true;
				offerBox.setValue(percentText(charterOffer));
				syncingOffer = false;
			}
		}
	}

	// ------------------------------------------------------------------ land

	private int mapX() {
		return leftColumn() + (columnWidth() - MAP_CELLS * cell()) / 2;
	}

	private int mapY() {
		return contentTop() + 4;
	}

	private FinanceSnapshotPayload.Plot plotAt(int dx, int dz) {
		for (FinanceSnapshotPayload.Plot plot : data.land().plots()) {
			if (plot.dx() == dx && plot.dz() == dz) {
				return plot;
			}
		}
		return null;
	}

	private int selectedDx() {
		return selectedChunkX - data.land().centreX();
	}

	private int selectedDz() {
		return selectedChunkZ - data.land().centreZ();
	}

	private boolean selectionOnMap() {
		int half = MAP_CELLS / 2;
		return Math.abs(selectedDx()) <= half && Math.abs(selectedDz()) <= half;
	}

	private static long chunkKey(int x, int z) {
		return (x & 0xFFFFFFFFL) | ((long) z << 32);
	}

	private void initLand() {
		FinanceSnapshotPayload.LandView land = data.land();
		if (!land.active()) {
			return;
		}
		if (selectedChunkX == Integer.MIN_VALUE || !selectionOnMap()) {
			selectedChunkX = land.centreX();
			selectedChunkZ = land.centreZ();
		}
		int half = MAP_CELLS / 2;
		for (int row = 0; row < MAP_CELLS; row++) {
			for (int col = 0; col < MAP_CELLS; col++) {
				int dx = col - half;
				int dz = row - half;
				plate(mapX() + col * cell(), mapY() + row * cell(), cell(), cell(), "", Plate.Kind.MAP, b -> {
					selectedChunkX = data.land().centreX() + dx;
					selectedChunkZ = data.land().centreZ() + dz;
					landPrice = "";
					rebuildWidgets();
				});
			}
		}
		int rx = rightColumn();
		int w = rightWidth();
		int y = contentTop();
		landBuy = plate(rx, y + 52, w, 14, "Buy", Plate.Kind.BRASS, b -> {
			FinanceSnapshotPayload.Plot plot = plotAt(selectedDx(), selectedDz());
			long price = plot == null ? data.land().nextPrice() : plot.price();
			send(Action.LAND_BUY, "", chunkKey(selectedChunkX, selectedChunkZ), 0, "", price);
		});
		box(rx, y + 52, w - 98, landPrice, 16, "Price", v -> landPrice = v);
		landSell = plate(rx + w - 94, y + 52, 46, 14, "Sell", Plate.Kind.BRASS, b -> {
			FinanceSnapshotPayload.Plot plot = plotAt(selectedDx(), selectedDz());
			long price = plot != null && plot.price() > 0 && landPrice.isBlank() ? 0 : FinanceBook.parseAmount(landPrice);
			if (price >= 0) {
				send(Action.LAND_SELL, "", chunkKey(selectedChunkX, selectedChunkZ), 0, "", price);
			}
		});
		landAbandon = plate(rx + w - 46, y + 52, 46, 14, "Give up", Plate.Kind.WOOD,
				b -> send(Action.LAND_ABANDON, "", chunkKey(selectedChunkX, selectedChunkZ), 0, ""));
		landAdmin = plate(rx, y + 68, w, 12, "Admin: remove this claim", Plate.Kind.WOOD,
				b -> send(Action.LAND_ABANDON, "", chunkKey(selectedChunkX, selectedChunkZ), 0, ""));
		int halfW = (w - 4) / 2;
		landZone = plate(rx + halfW + 4, y + 68, halfW, 12, "Zone", Plate.Kind.WOOD, b -> {
			FinanceSnapshotPayload.CorpView target = zoneTarget();
			send(Action.LAND_ZONE, "", chunkKey(selectedChunkX, selectedChunkZ), 0, "", target == null ? 0 : target.id());
		});
		// Auctions: the owner auctions with the price box as the opening bid; others bid with it.
		landAuction = plate(rx, y + 68, halfW - 30, 12, "Auction", Plate.Kind.WOOD, b -> {
			long opening = FinanceBook.parseAmount(landPrice);
			if (opening <= 0) {
				clientNotice = "Type the opening bid in the price box, then press Auction.";
				return;
			}
			send(Action.LAND_AUCTION, "", chunkKey(selectedChunkX, selectedChunkZ), landAuctionIndex, "", opening);
		});
		landAuctionLength = plate(rx + halfW - 28, y + 68, 28, 12, AUCTION_LENGTHS[landAuctionIndex].replace(" days", "d")
				.replace(" day", "d"), Plate.Kind.WOOD, b -> {
			landAuctionIndex = (landAuctionIndex + 1) % AUCTION_LENGTHS.length;
			b.setMessage(Component.literal(AUCTION_LENGTHS[landAuctionIndex].replace(" days", "d").replace(" day", "d")));
		});
		landBid = plate(rx + w - 94, y + 52, 94, 14, "Bid", Plate.Kind.BRASS, b -> {
			FinanceSnapshotPayload.Plot plot = plotAt(selectedDx(), selectedDz());
			if (plot == null || !plot.onAuction()) {
				return;
			}
			long bid = FinanceBook.parseAmount(landPrice);
			send(Action.LAND_BID, "", chunkKey(selectedChunkX, selectedChunkZ), 0, "", bid > 0 ? bid : plot.minBid());
		});
		landCallOff = plate(rx + w - 94, y + 52, 94, 14, "Call off auction", Plate.Kind.WOOD,
				b -> send(Action.LAND_SELL, "", chunkKey(selectedChunkX, selectedChunkZ), 0, "", 0));
		box(rx, y + 98, w - 98, trustName, 16, "Player name", v -> trustName = v);
		plate(rx + w - 94, y + 98, 46, 14, "Trust", Plate.Kind.BRASS, b -> {
			if (!trustName.isBlank()) {
				send(Action.LAND_TRUST, trustName.trim(), 0, 1, "");
			}
		});
		plate(rx + w - 46, y + 98, 46, 14, "Remove", Plate.Kind.WOOD, b -> {
			if (!trustName.isBlank()) {
				send(Action.LAND_TRUST, trustName.trim(), 0, 0, "");
			}
		});
	}

	private void refreshLandButtons() {
		if (landBuy == null) {
			return;
		}
		FinanceSnapshotPayload.Plot plot = selectionOnMap() ? plotAt(selectedDx(), selectedDz()) : null;
		boolean mine = plot != null && plot.mine();
		boolean auction = plot != null && plot.onAuction();
		boolean buyable = selectionOnMap() && (plot == null || (!mine && !auction && plot.price() > 0));
		landBuy.visible = !mine && !auction;
		long price = plot == null ? data.land().nextPrice() : plot.price();
        landBuy.active = buyable && (!data.overdue() || (plot == null && price == 0));
        landBuy.setMessage(Component.literal(buyable ? (price == 0 ? "Claim free" : "Buy for " + FinanceBook.amount(price)) : "Not for sale"));
		landBid.visible = !mine && auction;
		landBid.active = auction && !plot.leading() && !data.overdue();
		landBid.setMessage(Component.literal(auction && plot.leading() ? "You lead" : "Bid"));
		landCallOff.visible = mine && auction;
		landCallOff.active = auction && plot.bid() <= 0;
		landSell.visible = mine && !auction;
		landSell.setMessage(Component.literal(mine && plot.price() > 0 && landPrice.isBlank() ? "Unlist" : "Sell"));
		landAbandon.visible = mine && !auction;
		landAdmin.visible = plot != null && !mine && data.admin();
		landAuction.visible = mine && !auction;
		landAuctionLength.visible = mine && !auction;
		boolean controlsAny = data.equities().corps().stream().anyMatch(FinanceSnapshotPayload.CorpView::controlledByMe);
		landZone.visible = mine && !auction && controlsAny;
		FinanceSnapshotPayload.CorpView target = zoneTarget();
		landZone.setMessage(Component.literal(target == null ? "To my name" : "Zone: " + target.ticker()));
		for (var child : children()) {
			if (child instanceof EditBox box && box.getY() == contentTop() + 52) {
				box.visible = (mine && !auction) || (!mine && auction);
				box.setHint(Component.literal(auction ? "Bid " + FinanceBook.amount(plot.minBid()) + "+" : "Price")
						.withStyle(ChatFormatting.DARK_GRAY));
			}
		}
	}

	private boolean sameOwner(int dx, int dz, String owner) {
		FinanceSnapshotPayload.Plot other = plotAt(dx, dz);
		return other != null && other.owner().equals(owner);
	}

	/** A white arrow where the player stands, pointing the way they face, like the marker on a vanilla map. */
	private void drawPlayerMarker(GuiGraphicsExtractor g, int mx, int my, int size) {
		var player = Minecraft.getInstance().player;
		if (player == null) {
			return;
		}
		int half = MAP_CELLS / 2;
		double originX = (data.land().centreX() - half) * 16.0;
		double originZ = (data.land().centreZ() - half) * 16.0;
		double scale = size / (MAP_CELLS * 16.0);
		int px = mx + (int) Math.round((player.getX() - originX) * scale);
		int py = my + (int) Math.round((player.getZ() - originZ) * scale);
		if (px < mx || py < my || px >= mx + size || py >= my + size) {
			return;
		}
		double yaw = Math.toRadians(player.getYRot());
		double fx = -Math.sin(yaw);
		double fz = Math.cos(yaw);
		// A small dotted arrow: outline, then body, then the tip.
		for (int i = -2; i <= 3; i++) {
			int ax = px + (int) Math.round(fx * i);
			int ay = py + (int) Math.round(fz * i);
			g.fill(ax - 2, ay - 2, ax + 2, ay + 2, 0xFF000000);
		}
		for (int i = -2; i <= 3; i++) {
			int ax = px + (int) Math.round(fx * i);
			int ay = py + (int) Math.round(fz * i);
			int r = i >= 2 ? 0 : 1;
			g.fill(ax - r, ay - r, ax + r + 1, ay + r + 1, 0xFFFFFFFF);
		}
	}

	private void drawLand(GuiGraphicsExtractor g) {
		FinanceSnapshotPayload.LandView land = data.land();
		int x = leftColumn();
		int y = contentTop();
		int w = rightWidth();
		int cs = cell();
		if (!land.active()) {
			slate(g, x, y, left + panelWidth - 10, contentBottom());
			text(g, "Land can be claimed only in the Overworld.", x + 8, y + 10, DIM, false);
			return;
		}
		refreshLandButtons();
		int mx = mapX();
		int my = mapY();
		int size = MAP_CELLS * cs;
		int half = MAP_CELLS / 2;
		if (backPass) {
			// A paper map in a dark wooden frame, like a framed map on a wall.
			g.fill(mx - 6, my - 6, mx + size + 6, my + size + 6, WALNUT_DARK);
			g.fill(mx - 5, my - 5, mx + size + 5, my + size + 5, GOLD_DIM);
			g.fill(mx - 4, my - 4, mx + size + 4, my + size + 4, 0xFFB89B6A);
			g.fill(mx - 3, my - 3, mx + size + 3, my + size + 3, LandMapTexture.PAPER);
			g.fill(mx - 1, my - 1, mx + size + 1, my + size + 1, 0xFF8A7350);
			LandMapTexture.get(MAP_CELLS).draw(g, land.centreX(), land.centreZ(), mx, my, size);
			// Claims: a light wash and a coloured border so the ground still shows through.
			for (int row = 0; row < MAP_CELLS; row++) {
				for (int col = 0; col < MAP_CELLS; col++) {
					FinanceSnapshotPayload.Plot plot = plotAt(col - half, row - half);
					if (plot == null) {
						continue;
					}
					int edge = plot.mine() ? 0xFFE8C463 : plot.onAuction() || plot.price() > 0 ? 0xFF7FC96A
							: plot.trusted() ? 0xFF5F93C6 : 0xFFE0624F;
					int cx0 = mx + col * cs;
					int cy0 = my + row * cs;
					int cx1 = cx0 + cs;
					int cy1 = cy0 + cs;
					g.fill(cx0, cy0, cx1, cy1, (edge & 0x00FFFFFF) | 0x40000000);
					// Draw the border only where the neighbour is not the same owner, so estates read as one shape.
					String owner = plot.owner();
					if (!sameOwner(col - half, row - half - 1, owner)) g.fill(cx0, cy0, cx1, cy0 + 1, edge);
					if (!sameOwner(col - half, row - half + 1, owner)) g.fill(cx0, cy1 - 1, cx1, cy1, edge);
					if (!sameOwner(col - half - 1, row - half, owner)) g.fill(cx0, cy0, cx0 + 1, cy1, edge);
					if (!sameOwner(col - half + 1, row - half, owner)) g.fill(cx1 - 1, cy0, cx1, cy1, edge);
				}
			}
			// Faint chunk grid, the player's marker and the selection.
			for (int i = 1; i < MAP_CELLS; i++) {
				g.fill(mx + i * cs, my, mx + i * cs + 1, my + size, 0x1A000000);
				g.fill(mx, my + i * cs, mx + size, my + i * cs + 1, 0x1A000000);
			}
			drawPlayerMarker(g, mx, my, size);
			if (selectionOnMap()) {
				int sx = mx + (selectedDx() + half) * cs;
				int sy = my + (selectedDz() + half) * cs;
				g.fill(sx - 1, sy - 1, sx + cs + 1, sy, GOLD);
				g.fill(sx - 1, sy + cs, sx + cs + 1, sy + cs + 1, GOLD);
				g.fill(sx - 1, sy, sx, sy + cs, GOLD);
				g.fill(sx + cs, sy, sx + cs + 1, sy + cs, GOLD);
			}
		}

		int rx = rightColumn();
		heading(g, "The Land Office", rx, y, w);
		slate(g, rx, y + 13, rx + w, y + 48);
		FinanceSnapshotPayload.Plot plot = selectionOnMap() ? plotAt(selectedDx(), selectedDz()) : null;
		text(g, "Chunk " + selectedChunkX + ", " + selectedChunkZ, rx + 5, y + 17, GOLD, false);
		String status;
		int color = CREAM;
		if (plot == null) {
			status = land.nextPrice() == 0 ? "Unclaimed: free claim available" : "Unclaimed: " + FinanceBook.amount(land.nextPrice());
		} else if (plot.onAuction()) {
			String top = plot.bid() > 0 ? "top bid " + FinanceBook.amount(plot.bid()) : "no bids";
			status = (plot.mine() ? "Your auction" : plot.owner() + "'s auction") + ": " + top + ", to day " + plot.auctionEnd();
			color = plot.leading() ? GAIN : GOLD;
		} else if (plot.mine()) {
			status = plot.price() > 0 ? "Yours, for sale at " + FinanceBook.amount(plot.price()) : "Yours";
			color = GOLD;
		} else if (plot.price() > 0) {
			status = plot.owner() + " sells: " + FinanceBook.amount(plot.price());
			color = GAIN;
		} else {
			status = "Owned by " + plot.owner() + (plot.trusted() ? " (you may build)" : "");
			color = plot.trusted() ? 0xFF8DBDE6 : LOSS;
		}
		text(g, font.plainSubstrByWidth(status, w - 10), rx + 5, y + 27, color, false);
		text(g, font.plainSubstrByWidth("You own " + land.owned() + "; base price " + FinanceBook.amount(land.basePrice()), w - 10),
				rx + 5, y + 37, DIM, false);
		heading(g, "Trusted Builders", rx, y + 82, w);
		int ly = y + 118;
		if (land.trusted().isEmpty()) {
			text(g, "Only you build on your land.", rx, ly, DIM, false);
		} else {
			String names = String.join(", ", land.trusted());
			paragraph(g, names, rx, ly, w, CREAM, contentBottom() - 12);
		}
		// Legend at the foot of the Land Office.
		int lg = contentBottom() - 9;
		legend(g, rx, lg, 0xFFE8C463, "yours");
		legend(g, rx + 44, lg, 0xFF7FC96A, "for sale");
		legend(g, rx + 100, lg, 0xFFE0624F, "taken");
		legend(g, rx + 145, lg, 0xFF5F93C6, "trusted");
	}

	/** The corporation the selected chunk would be zoned into (null: back to the player's own name). */
	private FinanceSnapshotPayload.CorpView zoneTarget() {
		FinanceSnapshotPayload.Plot plot = selectionOnMap() ? plotAt(selectedDx(), selectedDz()) : null;
		for (FinanceSnapshotPayload.CorpView corp : data.equities().corps()) {
			if (corp.controlledByMe() && (plot == null || !corp.ticker().equals(plot.owner()))) {
				return corp;
			}
		}
		return null;
	}

	private void legend(GuiGraphicsExtractor g, int x, int y, int color, String label) {
		paint(g, x, y + 1, x + 6, y + 7, color);
		text(g, label, x + 9, y, DIM, false);
	}

	private void refreshMarketButtons() {
		int size = tab == COMMODITIES ? data.listings().size() : tab == REQUESTS ? data.requests().size() : 0;
		for (int i = 0; i < rowPlates.size(); i++) {
			rowPlates.get(i).active = listScroll + i < size;
		}
		for (int i = 0; i < pickPlates.size(); i++) {
			pickPlates.get(i).active = i < picks.size();
		}
		if (collectButton != null) {
			collectButton.active = data.waiting() > 0;
		}
		if (listButton != null) {
			listButton.active = !hand().isEmpty() && FinanceBook.parseAmount(sellPrice) > 0 && parseCount(sellQuantity, 0) > 0;
		}
		if (postButton != null) {
			long price = wantPrice.isBlank() ? 0 : FinanceBook.parseAmount(wantPrice);
			postButton.active = pickedItem != null && parseCount(wantQuantity, 0) > 0 && price >= 0;
		}
		if (dealButton != null && tab == COMMODITIES) {
			FinanceSnapshotPayload.ListingView sel = selectedListingView();
			dealButton.setMessage(Component.literal(sel != null && sel.auction() ? "Bid" : "Buy"));
			dealButton.active = sel != null && !sel.mine() && !(sel.auction() && sel.leading());
			takeDownButton.active = sel != null && (sel.mine() || data.admin());
		}
		if (dealButton != null && tab == REQUESTS) {
			FinanceSnapshotPayload.RequestView sel = selectedRequestView();
			dealButton.active = sel != null && !sel.mine() && sel.price() > 0;
			takeDownButton.active = sel != null && (sel.mine() || data.admin());
		}
	}

	/** The scrolling list box and its rows: item, a main line, a dim second line and a value on the right. */
	private void drawList(GuiGraphicsExtractor g, int count, long selected, java.util.function.IntFunction<Object[]> rowAt) {
		int rx = rightColumn();
		int w = rightWidth();
		slate(g, rx, listTop() - 2, rx + w, listBottom() + 1);
		if (count == 0) {
			text(g, "Nothing here yet.", rx + 6, listTop() + 4, DIM, false);
		}
		for (int i = 0; i < visibleRows() && listScroll + i < count; i++) {
			Object[] row = rowAt.apply(listScroll + i);
			long id = (Long) row[0];
			int ry = rowY(i);
			if (id == selected) {
				paint(g, rx + 1, ry, rx + w - 1, ry + ROW, BRASS_DARK);
			} else if (i % 2 == 1) {
				paint(g, rx + 1, ry, rx + w - 1, ry + ROW, SLATE_STRIPE);
			}
			if (!backPass) {
				g.item((ItemStack) row[1], rx + 2, ry + 1);
			}
			String value = (String) row[4];
			text(g, value, rx + w - 4 - font.width(value), ry + 1, (Integer) row[5], false);
			text(g, font.plainSubstrByWidth((String) row[2], w - 26 - font.width(value) - 4), rx + 21, ry + 1, CREAM, false);
			text(g, font.plainSubstrByWidth((String) row[3], w - 26), rx + 21, ry + 10, DIM, false);
		}
		if (count > visibleRows()) {
			String more = (listScroll + 1) + "-" + Math.min(count, listScroll + visibleRows()) + " of " + count;
			text(g, more, rx + w - font.width(more), contentTop(), DIM, false);
		}
	}

	private void drawCommodities(GuiGraphicsExtractor g) {
		int x = leftColumn();
		int y = contentTop();
		int w = columnWidth();
		int half = w / 2 - 2;
		heading(g, "Sell at the Exchange", x, y, w);
		slate(g, x, y + 13, x + w, y + 40);
		ItemStack hand = hand();
		if (hand.isEmpty()) {
			text(g, "Hold the item you want", x + 6, y + 17, DIM, false);
			text(g, "to sell in your main hand.", x + 6, y + 27, DIM, false);
		} else {
			if (!backPass) {
				g.item(hand, x + 5, y + 18);
			}
			text(g, font.plainSubstrByWidth(hand.getHoverName().getString(), w - 30), x + 26, y + 17, CREAM, false);
			text(g, "You carry " + carried(hand), x + 26, y + 27, DIM, false);
		}
		text(g, "Quantity", x, y + 60, DIM, false);
		text(g, sellAuction ? "Opening bid" : "Price each", x + half + 4, y + 60, DIM, false);
		slate(g, x, y + 105, x + w, y + 121);
		text(g, "Collection box: " + data.waiting(), x + 5, y + 109, data.waiting() > 0 ? GOLD : DIM, false);

		int rx = rightColumn();
		int rw = rightWidth();
		heading(g, "The Market", rx, y, rw - 50);
		List<FinanceSnapshotPayload.ListingView> lots = data.listings();
		drawList(g, lots.size(), selectedListing, index -> {
			FinanceSnapshotPayload.ListingView l = lots.get(index);
			String main = l.quantity() + " " + l.item().getHoverName().getString();
			String sub = l.mine() ? (l.company() ? l.seller() + " (yours)" : "yours") : l.seller();
			String value;
			int color;
			if (l.auction()) {
				value = l.bid() > 0 ? FinanceBook.amount(l.bid()) : "from " + FinanceBook.amount(l.price());
				color = l.leading() ? GAIN : GOLD;
				sub += " · auction to day " + l.endDay();
			} else {
				value = FinanceBook.amount(l.price()) + " ea";
				color = CREAM;
			}
			return new Object[] {l.id(), l.item(), main, sub, value, color};
		});
		FinanceSnapshotPayload.ListingView sel = selectedListingView();
		String detail;
		if (sel == null) {
			detail = "Pick a listing to buy or bid on.";
		} else if (sel.mine()) {
			detail = sel.auction() && sel.bid() > 0 ? "Your auction: top bid " + FinanceBook.amount(sel.bid()) : "Your listing.";
		} else if (sel.auction()) {
			detail = sel.leading() ? "You hold the top bid." : "Bid at least " + FinanceBook.amount(sel.minBid()) + ":";
		} else {
			detail = "How many? " + FinanceBook.amount(sel.price()) + " each:";
		}
		text(g, font.plainSubstrByWidth(detail, rw), rx, detailY(), DIM, false);
	}

	private void drawRequests(GuiGraphicsExtractor g) {
		int x = leftColumn();
		int y = contentTop();
		int w = columnWidth();
		int half = w / 2 - 2;
		heading(g, "Post a Request", x, y, w);
		for (int i = 0; i < pickPlates.size() && i < picks.size(); i++) {
			Plate cell = pickPlates.get(i);
			if (!backPass) {
				g.item(new ItemStack(picks.get(i)), cell.getX() + 1, cell.getY() + 1);
			}
			if (picks.get(i) == pickedItem) {
				paint(g, cell.getX(), cell.getY(), cell.getX() + 18, cell.getY() + 18, BRASS);
			}
		}
		if (picks.isEmpty()) {
			text(g, search.trim().length() < 2 ? "Type to find an item." : "No item matches.", x + 4, y + 40, DIM, false);
		}
		String wanted = pickedItem == null ? "No item picked" : new ItemStack(pickedItem).getHoverName().getString();
		text(g, font.plainSubstrByWidth("Wanted: " + wanted, w), x, y + 69, pickedItem == null ? DIM : GOLD, false);
		text(g, "Quantity", x, y + 80, DIM, false);
		text(g, "Price each", x + half + 4, y + 80, DIM, false);
		paragraph(g, "Leave the price blank to advertise; a price holds the money until filled.", x, y + 126, w, DIM,
				contentBottom());

		int rx = rightColumn();
		int rw = rightWidth();
		heading(g, "Wanted", rx, y, rw - 50);
		List<FinanceSnapshotPayload.RequestView> wants = data.requests();
		drawList(g, wants.size(), selectedRequest, index -> {
			FinanceSnapshotPayload.RequestView r = wants.get(index);
			String main = r.quantity() + " " + r.item().getHoverName().getString();
			String sub = r.mine() ? "yours" : r.owner();
			String value = r.price() > 0 ? FinanceBook.amount(r.price()) + " ea" : "advert";
			return new Object[] {r.id(), r.item(), main, sub, value, r.price() > 0 ? GOLD : DIM};
		});
		FinanceSnapshotPayload.RequestView sel = selectedRequestView();
		String detail;
		if (sel == null) {
			detail = "Pick a request to fill or withdraw.";
		} else if (sel.mine()) {
			detail = "Your request.";
		} else if (sel.price() <= 0) {
			detail = "An advert: talk to " + sel.owner() + " to trade.";
		} else {
			detail = "How many will you deliver?";
		}
		text(g, font.plainSubstrByWidth(detail, rw), rx, detailY(), DIM, false);
	}

	// ------------------------------------------------------------------ drawing helpers

	/** A raised block: face with a light top-left and a dark bottom-right edge. */
	static void raised(GuiGraphicsExtractor g, int x0, int y0, int x1, int y1, int face, int light, int dark) {
		g.fill(x0, y0, x1, y1, face);
		g.fill(x0, y0, x1, y0 + 1, light);
		g.fill(x0, y0, x0 + 1, y1, light);
		g.fill(x0, y1 - 1, x1, y1, dark);
		g.fill(x1 - 1, y0, x1, y1, dark);
	}

	/** A sunken block: dark top-left, light bottom-right. */
	static void sunken(GuiGraphicsExtractor g, int x0, int y0, int x1, int y1, int face, int dark, int light) {
		raised(g, x0, y0, x1, y1, face, dark, light);
	}

	/** A small diamond centred on (cx, cy) with radius r. */
	static void diamond(GuiGraphicsExtractor g, int cx, int cy, int r, int color) {
		for (int dy = -r; dy <= r; dy++) {
			int half = r - Math.abs(dy);
			g.fill(cx - half, cy + dy, cx + half + 1, cy + dy + 1, color);
		}
	}

	static void rivet(GuiGraphicsExtractor g, int x, int y) {
		g.fill(x, y, x + 2, y + 2, BRASS);
		g.fill(x, y, x + 1, y + 1, BRASS_LIGHT);
	}

	/** Gold filigree in one corner; dx/dy (+1 or -1) point into the panel. */
	static void filigree(GuiGraphicsExtractor g, int x, int y, int dx, int dy) {
		for (int i = 0; i < 10; i++) {
			px(g, x + i * dx, y, GOLD);
			px(g, x, y + i * dy, GOLD);
		}
		for (int i = 1; i < 9; i++) {
			px(g, x + i * dx, y + dy, GOLD_DIM);
			px(g, x + dx, y + i * dy, GOLD_DIM);
		}
		// The curl: a little square scroll set in from the corner.
		int[][] curl = {{3, 3}, {4, 3}, {5, 3}, {3, 4}, {3, 5}, {5, 4}, {5, 5}, {4, 5}, {6, 3}, {3, 6}};
		for (int[] p : curl) {
			px(g, x + p[0] * dx, y + p[1] * dy, GOLD);
		}
	}

	private static void px(GuiGraphicsExtractor g, int x, int y, int color) {
		g.fill(x, y, x + 1, y + 1, color);
	}

	/** Section heading: diamond, gold title, a rule to the column's end and a small closing diamond. */
	private void heading(GuiGraphicsExtractor g, String text, int x, int y, int w) {
		diamond(g, x + 2, y + 4, 2, GOLD);
		text = font.plainSubstrByWidth(text, Math.max(0, w - 10));
		text(g, text, x + 8, y, GOLD, true);
		int ruleStart = x + 12 + font.width(text);
		if (ruleStart < x + w - 6) {
			g.fill(ruleStart, y + 4, x + w - 5, y + 5, GOLD_DIM);
			g.fill(ruleStart, y + 5, x + w - 5, y + 6, WALNUT_DARK);
			diamond(g, x + w - 2, y + 4, 1, GOLD);
		}
	}

	/** The dark slate table box. */
	private void slate(GuiGraphicsExtractor g, int x0, int y0, int x1, int y1) {
		if (!backPass) {
			return;
		}
		sunken(g, x0, y0, x1, y1, SLATE, SLATE_DARK, SLATE_LIGHT);
	}

	/** One ledger row: dim label left, value right-aligned; a darker stripe when {@code stripe}. */
	private void row(GuiGraphicsExtractor g, String label, String value, int x, int y, int w, int valueColor, boolean stripe) {
		if (stripe) {
			paint(g, x + 1, y - 1, x + w - 1, y + LINE - 1, SLATE_STRIPE);
		}
		text(g, label, x + 4, y, DIM, false);
		String fitted = font.plainSubstrByWidth(value, Math.max(20, w - font.width(label) - 12));
		text(g, fitted, x + w - 4 - font.width(fitted), y, valueColor, false);
	}

	private String money(long cents) {
		return FinanceBook.format(cents, data.currency());
	}

	private int paragraph(GuiGraphicsExtractor g, String text, int x, int y, int w, int color, int maxY) {
		for (FormattedCharSequence line : font.split(Component.literal(text), w)) {
			if (y + 8 > maxY) {
				break;
			}
			text(g, line, x, y, color, false);
			y += LINE;
		}
		return y;
	}

	// ------------------------------------------------------------------ drawing

	@Override
	public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
		super.extractBackground(g, mouseX, mouseY, delta);
		int right = left + panelWidth;
		int bottom = top + panelHeight;
		// Dark outer edge, gold fillet, then the walnut frame.
		g.fill(left - 3, top - 3, right + 3, bottom + 3, WALNUT_DARK);
		g.fill(left - 2, top - 2, right + 2, bottom + 2, BRASS);
		g.fill(left - 1, top - 1, right + 1, bottom + 1, WALNUT_DARK);
		raised(g, left, top, right, bottom, WALNUT, WALNUT_LIGHT, WALNUT_DARK);
		// Header plaque with rivets.
		int hx0 = left + FRAME;
		int hy0 = top + FRAME;
		int hx1 = right - FRAME;
		int hy1 = top + FRAME + HEADER;
		sunken(g, hx0, hy0, hx1, hy1, PLAQUE, PLAQUE_DARK, PLAQUE_LIGHT);
		rivet(g, hx0 + 2, hy0 + 2);
		rivet(g, hx1 - 4, hy0 + 2);
		rivet(g, hx0 + 2, hy1 - 4);
		rivet(g, hx1 - 4, hy1 - 4);
		// Rule under the tabs, with a diamond set in its middle.
		int ry = top + FRAME + HEADER + TAB_HEIGHT + 3;
		int cx = left + panelWidth / 2;
		g.fill(hx0, ry, hx1, ry + 1, BRASS);
		g.fill(hx0, ry + 1, hx1, ry + 2, WALNUT_DARK);
		g.fill(cx - 5, ry - 3, cx + 6, ry + 5, WALNUT);
		diamond(g, cx, ry + 1, 3, GOLD);
		diamond(g, cx, ry + 1, 1, BRASS_DARK);
		// Footer plaque.
		sunken(g, hx0, bottom - FRAME - 14, hx1, bottom - FRAME, PLAQUE, PLAQUE_DARK, PLAQUE_LIGHT);
		// Filigree corners over the frame.
		filigree(g, left - 2, top - 2, 1, 1);
		filigree(g, right + 1, top - 2, -1, 1);
		filigree(g, left - 2, bottom + 1, 1, -1);
		filigree(g, right + 1, bottom + 1, -1, -1);
		// The page's boxes, stripes and rules go under the widgets; its text goes over them (extractRenderState).
		backPass = true;
		drawPage(g);
		backPass = false;
	}

	/** Draws the open tab: in the back pass only its boxes and rules, in the front pass only its text and overlays. */
	private void drawPage(GuiGraphicsExtractor g) {
		switch (tab) {
			case BANKING -> drawBanking(g);
			case ECONOMY -> drawEconomy(g);
			case CONTRACTS -> drawContracts(g);
			case COMMODITIES -> drawCommodities(g);
			case REQUESTS -> drawRequests(g);
			case LAND -> drawLand(g);
			case EQUITIES -> drawEquities(g);
			default -> drawComing(g, COMING[tab]);
		}
	}

	private void text(GuiGraphicsExtractor g, String s, int x, int y, int color, boolean shadow) {
		if (!backPass) {
			g.text(font, s, x, y, color, shadow);
		}
	}

	private void text(GuiGraphicsExtractor g, FormattedCharSequence s, int x, int y, int color, boolean shadow) {
		if (!backPass) {
			g.text(font, s, x, y, color, shadow);
		}
	}

	private void paint(GuiGraphicsExtractor g, int x0, int y0, int x1, int y1, int color) {
		if (backPass) {
			g.fill(x0, y0, x1, y1, color);
		}
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
		super.extractRenderState(g, mouseX, mouseY, delta);
		int hx0 = left + FRAME;
		int hy0 = top + FRAME;
		int hx1 = left + panelWidth - FRAME;
		// Emblem and title.
		g.blit(RenderPipelines.GUI_TEXTURED, EMBLEM, hx0 + 7, hy0 + 3, 0.0F, 0.0F, EMBLEM_SIZE, EMBLEM_SIZE, EMBLEM_SIZE, EMBLEM_SIZE);
		int tx = hx0 + 7 + EMBLEM_SIZE + 6;
		g.text(font, "THE JUGCRAFT EXCHANGE", tx, hy0 + 6, GOLD, true);
		String motto = "TRADE · LEND · PROSPER";
		diamond(g, tx + 1, hy0 + 20, 1, GOLD_DIM);
		g.text(font, motto, tx + 6, hy0 + 17, 0xFFA8977A, false);
		diamond(g, tx + 10 + font.width(motto), hy0 + 20, 1, GOLD_DIM);
		// Day and currency in an inset, brass-edged box.
		String day = "Your cash";
		String currency = font.plainSubstrByWidth(money(data.balance()), 110);
		int bw = Math.max(font.width(day), font.width(currency)) + 10;
		int bx = hx1 - 8 - bw;
		g.fill(bx - 1, hy0 + 3, bx + bw + 1, hy0 + HEADER - 3, GOLD_DIM);
		sunken(g, bx, hy0 + 4, bx + bw, hy0 + HEADER - 4, WALNUT, PLAQUE_DARK, WALNUT_LIGHT);
		g.text(font, day, bx + (bw - font.width(day)) / 2, hy0 + 6, TAN, false);
		g.text(font, currency, bx + (bw - font.width(currency)) / 2, hy0 + 15, GOLD, false);

		drawPage(g);
		// Footer: the last result, after a diamond.
		int fy = top + panelHeight - FRAME - 11;
		diamond(g, hx0 + 7, fy + 3, 1, GOLD);
		if (!clientNotice.isEmpty()) {
			g.text(font, font.plainSubstrByWidth(clientNotice, panelWidth - 2 * FRAME - 72), hx0 + 13, fy, LOSS, false);
		} else if (!data.notice().isEmpty()) {
			String notice = font.plainSubstrByWidth(data.notice(), panelWidth - 2 * FRAME - 72);
			g.text(font, notice, hx0 + 13, fy, data.noticeOk() ? GAIN : LOSS, false);
		} else {
			g.text(font, "Esc closes the ledger. The Guide explains everything.", hx0 + 13, fy, DIM, false);
		}
	}

	private void drawBanking(GuiGraphicsExtractor g) {
		int x = leftColumn();
		int y = contentTop();
		int w = columnWidth();
		heading(g, "Personal Banking", x, y, w);
		slate(g, x, y + 13, x + w, y + 99);
		text(g, "Cash in hand", x + 6, y + 18, DIM, false);
		FormattedCharSequence balance = Component.literal(money(data.balance())).withStyle(ChatFormatting.BOLD).getVisualOrderText();
		text(g, balance, x + 6, y + 30, data.balance() > 0 ? GAIN : CREAM, true);
		paint(g, x + 4, y + 43, x + w - 4, y + 44, GOLD_DIM);
		row(g, "Owed to the bank", money(data.debt()), x, y + 48, w, data.debt() > 0 ? LOSS : CREAM, true);
		row(g, "Loan due", dueText(), x, y + 58, w, data.overdue() ? LOSS : CREAM, false);
		row(g, "Net worth", money(data.balance() - data.debt()), x, y + 68, w,
				data.balance() >= data.debt() ? CREAM : LOSS, true);
		row(g, "Credit limit", money(data.creditLimit()), x, y + 78, w, CREAM, false);
		int py = data.starterAvailable() ? y + 121 : y + 104;
		if (data.overdue()) {
			paragraph(g, "Your loan is overdue. You can sell but not buy, bid or pay until it is repaid, and "
					+ shareText(true) + " of your sales go to the bank.", x, py, w, LOSS, contentBottom());
		} else if (data.debt() > 0) {
			paragraph(g, shareText(false) + " of what you earn on the market repays your loan. Pay it off on time "
					+ "to raise your credit limit.", x, py, w, DIM, contentBottom());
		} else if (data.starterAvailable()) {
			paragraph(g, "New here? The starter loan buys your first goods. Sell to pay it back within "
					+ data.termDays() + " days.", x, py, w, DIM, contentBottom());
		} else {
			paragraph(g, "Borrow from the central bank under Economy. Pay other players from Contracts.",
					x, py, w, DIM, contentBottom());
		}

		int rx = rightColumn();
		heading(g, "Recent Entries", rx, y, w);
		slate(g, rx, y + 13, rx + w, contentBottom());
		int ly = y + 18;
		if (data.history().isEmpty()) {
			text(g, "No entries yet.", rx + 6, ly, DIM, false);
		}
		boolean first = true;
		for (String entry : data.history()) {
			if (ly + 8 > contentBottom() - 3) {
				break;
			}
			if (!first) {
				paint(g, rx + 4, ly - 2, rx + w - 4, ly - 1, SLATE_LIGHT);
			}
			first = false;
			ly = paragraph(g, entry, rx + 6, ly, w - 12, CREAM, contentBottom() - 3) + 3;
		}
	}

	private void drawEconomy(GuiGraphicsExtractor g) {
		int x = leftColumn();
		int y = contentTop();
		int w = columnWidth();
		if (adminView && data.admin()) {
			drawSettings(g);
			return;
		} else {
			heading(g, "Central Bank Lending", x, y, data.admin() ? w - 48 : w);
			slate(g, x, y + 13, x + w, y + 57);
			row(g, "Fee, repaid in " + data.termDays() + " days", FinanceBook.rateText(data.feeBp()), x, y + 17, w, CREAM, false);
			row(g, "Your credit limit", money(data.creditLimit()), x, y + 27, w, CREAM, true);
			row(g, "You owe", data.debt() <= 0 ? money(0) : FinanceBook.amount(data.debt())
					+ (data.overdue() ? " OVERDUE" : " by day " + data.dueDay()), x, y + 37, w,
					data.overdue() ? LOSS : data.debt() > 0 ? GOLD : CREAM, false);
			row(g, "Available to you", money(data.borrowable()), x, y + 47, w, data.overdue() ? DIM : GAIN, true);
			// The borrowing card: amount, slider (drawn over the widget), button.
			slate(g, x, y + 60, x + w, y + 112);
			text(g, "Borrow", x + 6, y + 64, DIM, false);
			String amount = FinanceBook.amount(borrowAmount());
			text(g, amount, x + w - 6 - font.width(amount), y + 64, GOLD, false);
			if (slider != null && !backPass) {
				drawSlider(g, slider);
			}
			text(g, "Repay", x, y + 121, DIM, false);
		}

		int rx = rightColumn();
		heading(g, "The Economy", rx, y, w);
		int tw = (w - 4) / 2;
		tile(g, rx, y + 13, tw, "Held by players", FinanceBook.amount(data.circulation()), CREAM);
		tile(g, rx + tw + 4, y + 13, tw, "Central reserve", FinanceBook.amount(data.reserve()), CREAM);
		tile(g, rx, y + 37, tw, "Total money", FinanceBook.amount(data.totalMoney()), CREAM);
		if (data.inflationKnown()) {
			int bp = data.inflationBp();
			tile(g, rx + tw + 4, y + 37, tw, "Inflation (day)", (bp >= 0 ? "+" : "-") + FinanceBook.rateText(Math.abs(bp)),
					bp > 0 ? LOSS : GAIN);
		} else {
			tile(g, rx + tw + 4, y + 37, tw, "Inflation (day)", "no trades yet", DIM);
		}
		int ly0 = y + 63;
		slate(g, rx, ly0, rx + w, contentBottom());
		text(g, "Wealthiest holders", rx + 4, ly0 + 4, DIM, false);
		text(g, "Cash", rx + w - 4 - font.width("Cash"), ly0 + 4, DIM, false);
		paint(g, rx + 3, ly0 + 13, rx + w - 3, ly0 + 14, GOLD_DIM);
		int ly = ly0 + 17;
		if (data.richList().isEmpty()) {
			text(g, "Nobody holds money yet.", rx + 4, ly, DIM, false);
		}
		int place = 1;
		int rowH = 9;
		for (FinanceSnapshotPayload.Rank rank : data.richList()) {
			if (ly + 8 > contentBottom() - 2) {
				break;
			}
			if (place % 2 == 0) {
				paint(g, rx + 1, ly - 1, rx + w - 1, ly + rowH - 1, SLATE_STRIPE);
			}
			int medal = place == 1 ? GOLD : place == 2 ? SILVER : place == 3 ? BRONZE : GOLD_DIM;
			text(g, place + ".", rx + 4, ly, medal, false);
			String cash = FinanceBook.amount(rank.balance());
			String name = font.plainSubstrByWidth(rank.name(), w - font.width(cash) - 30);
			text(g, name, rx + 20, ly, CREAM, false);
			text(g, cash, rx + w - 4 - font.width(cash), ly, CREAM, false);
			ly += rowH;
			place++;
		}
	}

	private String dueText() {
		if (data.debt() <= 0 || data.dueDay() < 0) {
			return "no loan";
		}
		if (data.overdue()) {
			return "OVERDUE";
		}
		long left = data.dueDay() - data.day();
		return "day " + data.dueDay() + (left <= 0 ? " (today)" : " (" + left + (left == 1 ? " day)" : " days)"));
	}

	private String shareText(boolean overdue) {
		return FinanceBook.rateText(overdue ? Math.min(10_000, data.shareBp() * 2) : data.shareBp());
	}

	/** A small stat tile: dim label over a value. */
	private void tile(GuiGraphicsExtractor g, int x, int y, int w, String label, String value, int color) {
		slate(g, x, y, x + w, y + 22);
		text(g, font.plainSubstrByWidth(label, w - 8), x + 4, y + 3, DIM, false);
		text(g, font.plainSubstrByWidth(value, w - 8), x + 4, y + 12, color, false);
	}

	/** The brass rail and knob, drawn over Minecraft's slider (which keeps handling the mouse). */
	private void drawSlider(GuiGraphicsExtractor g, BorrowSlider s) {
		int x0 = s.getX();
		int y0 = s.getY();
		int x1 = x0 + s.getWidth();
		int y1 = y0 + s.getHeight();
		int mid = y0 + s.getHeight() / 2;
		g.fill(x0, y0, x1, y1, SLATE);
		g.fill(x0, mid - 1, x1, mid + 1, SLATE_DARK);
		int knob = x0 + (int) Math.round(borrowFraction * (s.getWidth() - 6));
		g.fill(x0, mid - 1, knob + 3, mid + 1, BRASS);
		raised(g, knob, y0, knob + 6, y1, s.isHovered() ? BRASS_LIGHT : BRASS, BRASS_LIGHT, BRASS_DARK);
	}

	private void drawContracts(GuiGraphicsExtractor g) {
		int x = leftColumn();
		int y = contentTop();
		int w = columnWidth();
		heading(g, "Draw Up a Contract", x, y, w);
		text(g, "Pay to", x, y + 13, DIM, false);
		text(g, "Amount, and how often", x, y + 41, DIM, false);
		text(g, "In return they give or promise", x, y + 69, DIM, false);
		paragraph(g, "The first payment is made on signing. Daily and weekly contracts pay each Minecraft "
				+ "day or week until either party ends them or the payer runs dry.", x, y + 119, w, DIM, contentBottom());

		int rx = rightColumn();
		heading(g, "Your Contracts", rx, y, w);
		slate(g, rx, y + 12, rx + w, contentBottom());
		List<FinanceSnapshotPayload.ContractView> rows = data.contracts();
		if (rows.isEmpty()) {
			text(g, "None yet.", rx + 4, y + 17, DIM, false);
		}
		int visible = visibleContractRows();
		for (int i = 0; i < visible && contractScroll + i < rows.size(); i++) {
			FinanceSnapshotPayload.ContractView row = rows.get(contractScroll + i);
			int ry = contractRowY(i);
			if (i % 2 == 1) {
				paint(g, rx + 1, ry, rx + w - 1, ry + CONTRACT_ROW, SLATE_STRIPE);
			}
			String period = PERIODS[Math.max(0, Math.min(PERIODS.length - 1, row.period()))].toLowerCase(Locale.ROOT);
			String line = "#" + row.id() + (row.outgoing() ? " to " : " from ") + row.other() + "  "
					+ FinanceBook.amount(row.amount()) + " " + period;
			int color = !row.active() ? DIM : row.outgoing() ? LOSS : GAIN;
			int textWidth = w - 8 - (row.active() && row.period() != 0 ? 30 : 0);
			text(g, font.plainSubstrByWidth(line, textWidth), rx + 4, ry + 2, color, false);
			String sub = row.note().isEmpty() ? "" : "“" + row.note() + "”";
			if (!row.active() && row.period() != 0) {
				sub = "ended" + (sub.isEmpty() ? "" : " · " + sub);
			} else if (row.active() && row.period() != 0) {
				sub = "next day " + row.nextDay() + (sub.isEmpty() ? "" : " · " + sub);
			}
			text(g, font.plainSubstrByWidth(sub, w - 8), rx + 4, ry + 11, DIM, false);
		}
		if (rows.size() > visible) {
			String more = (contractScroll + 1) + "-" + Math.min(rows.size(), contractScroll + visible) + " of " + rows.size();
			text(g, more, rx + w - 4 - font.width(more), contentBottom() - 10, DIM, false);
		}
	}

	private void drawComing(GuiGraphicsExtractor g, String[] page) {
		int w = Math.min(panelWidth - 48, 270);
		int x = left + (panelWidth - w) / 2;
		int y = contentTop() + 2;
		slate(g, x - 10, y, x + w + 10, contentBottom());
		if (!backPass) {
			g.blit(RenderPipelines.GUI_TEXTURED, EMBLEM, left + (panelWidth - EMBLEM_SIZE) / 2, y + 6, 0.0F, 0.0F,
					EMBLEM_SIZE, EMBLEM_SIZE, EMBLEM_SIZE, EMBLEM_SIZE);
		}
		String title = page[0];
		int ty = y + 36;
		text(g, title, x + (w - font.width(title)) / 2, ty, GOLD, true);
		diamond(g, x + (w - font.width(title)) / 2 - 7, ty + 4, 2, GOLD_DIM);
		diamond(g, x + (w + font.width(title)) / 2 + 6, ty + 4, 2, GOLD_DIM);
		int end = paragraph(g, page[1], x, ty + 16, w, CREAM, contentBottom() - 14);
		String soon = "This counter opens in a coming batch.";
		text(g, soon, x + (w - font.width(soon)) / 2, Math.min(end + 6, contentBottom() - 12), DIM, false);
	}

	// ------------------------------------------------------------------ widgets

	/** Minecraft's own slider for the mouse; {@link #drawSlider} paints the brass rail over it. */
	private final class BorrowSlider extends AbstractSliderButton {
		BorrowSlider(int x, int y, int width, int height) {
			super(x, y, width, height, Component.empty(), borrowFraction);
		}

		@Override
		protected void updateMessage() {
			setMessage(Component.empty());
		}

		@Override
		protected void applyValue() {
			borrowFraction = value;
			refreshButtons();
		}
	}

	/** A button drawn as a brass or walnut plate (or a tab). Minecraft's Button handles the mouse and keyboard. */
	static final class Plate extends Button {
		enum Kind { BRASS, WOOD, TAB, TAB_OPEN, ROW, CELL, MAP }

		private final Kind kind;

		Plate(int x, int y, int width, int height, String label, Kind kind, OnPress press) {
			super(x, y, width, height, Component.literal(label), press, Button.DEFAULT_NARRATION);
			this.kind = kind;
		}

		@Override
		protected void extractContents(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
			int x0 = getX();
			int y0 = getY();
			int x1 = x0 + getWidth();
			int y1 = y0 + getHeight();
			boolean hot = active && isHovered();
			int text;
			switch (kind) {
				case BRASS -> {
					if (active) {
						raised(g, x0, y0, x1, y1, hot ? BRASS_LIGHT : BRASS, BRASS_LIGHT, BRASS_DARK);
						text = PLAQUE;
					} else {
						raised(g, x0, y0, x1, y1, WOOD, WOOD_LIGHT, WOOD_DARK);
						text = DIM;
					}
				}
				case TAB_OPEN -> {
					raised(g, x0, y0, x1, y1 + 1, BRASS, BRASS_LIGHT, BRASS);
					text = PLAQUE;
				}
				case ROW -> {
					if (hot) {
						g.fill(x0, y0, x1, y1, 0x22F0D07A);
					}
					return; // The screen draws the row's item and text.
				}
				case MAP -> {
					if (hot) {
						g.fill(x0, y0, x1, y0 + 1, 0xFFFFFFFF);
						g.fill(x0, y1 - 1, x1, y1, 0xFFFFFFFF);
						g.fill(x0, y0, x0 + 1, y1, 0xFFFFFFFF);
						g.fill(x1 - 1, y0, x1, y1, 0xFFFFFFFF);
					}
					return; // The screen draws the map.
				}
				case CELL -> {
					if (active) {
						raised(g, x0, y0, x1, y1, hot ? SLATE_LIGHT : SLATE_STRIPE, SLATE_LIGHT, SLATE_DARK);
					}
					return; // The screen draws the item.
				}
				case TAB -> {
					raised(g, x0, y0 + 1, x1, y1, hot ? WOOD_LIGHT : WOOD, WOOD_LIGHT, WOOD_DARK);
					text = hot ? GOLD : TAN;
				}
				default -> {
					raised(g, x0, y0, x1, y1, hot ? WOOD_LIGHT : WOOD, WOOD_LIGHT, WOOD_DARK);
					text = hot ? GOLD : TAN;
				}
			}
			Font font = Minecraft.getInstance().font;
			String label = font.plainSubstrByWidth(getMessage().getString(), getWidth() - 4);
			int ty = y0 + (getHeight() - 8) / 2 + (kind == Kind.TAB ? 1 : 0);
			g.text(font, label, x0 + (getWidth() - font.width(label)) / 2, ty, text, false);
		}
	}
}
