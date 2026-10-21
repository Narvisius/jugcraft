package io.github.jimbozoomer.jugcraft.client;

import static io.github.jimbozoomer.jugcraft.client.ExchangeScreen.*;

import io.github.jimbozoomer.jugcraft.finance.CasinoBlock;
import io.github.jimbozoomer.jugcraft.finance.CasinoPayload;
import io.github.jimbozoomer.jugcraft.finance.FinanceActionPayload;
import io.github.jimbozoomer.jugcraft.finance.FinanceActionPayload.Action;
import io.github.jimbozoomer.jugcraft.finance.FinanceBook;
import io.github.jimbozoomer.jugcraft.finance.PokerHands;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * What every casino game's screen shares, in the Exchange's walnut and brass: the frame, a header with the game, its
 * owner (the house), its limits and your Jugs; a chip rack to pick a bet size; the owner's limits panel; and a bottom
 * bar with the last result and the game's log. Each game draws its own table in between.
 */
public abstract class CasinoScreen extends Screen {
	protected static final int FRAME = 4;
	protected static final int HEADER = 24;
	protected static final int BAR = 34;
	protected static final int FELT = 0xFF2F5B3A;
	protected static final int FELT_LIGHT = 0xFF3A6B47;
	protected static final int FELT_DARK = 0xFF244A2E;
	protected static final int RAIL = 0xFF5A1F17;
	protected static final int CARD_FACE = 0xFFF2EBDD;
	protected static final int CARD_RED = 0xFFC0362C;
	protected static final int CARD_BLACK = 0xFF1E1E22;
	protected static final int CARD_BACK = 0xFF8E2A2A;
	protected static final int RED = 0xFFB3261E;
	protected static final int BLACK = 0xFF1C1A1A;
	protected static final int ZERO = 0xFF2E8B46;
	protected static final int WHITE = 0xFFEFE8D6;
	private static final long[] CHIPS = {1, 5, 25, 100, 500, 1_000, 5_000, 25_000, 100_000};
	private static final int[] CHIP_COLOURS = {0xFFE8E2D0, 0xFFC0362C, 0xFF2E8B46, 0xFF1E1E22, 0xFF7A3FB0, 0xFFE0A030,
			0xFF3A6EA5, 0xFFC94A8A, 0xFF8A6A2C};

	protected CasinoPayload data;
	protected int left;
	protected int top;
	protected int panelWidth;
	protected int panelHeight;
	protected long chip = 1;
	protected int timer;
	protected int ticks;
	/** How far into the next tick this frame is drawn (for smooth turning). */
	protected float partial;
	private boolean closing;
	private boolean limitsOpen;
	private String minText = "";
	private String maxText = "";
	private boolean bankChoice;
	/**
	 * What the bottom bar and the Jugs counter show. While a result is still being revealed (reels turning, a ball
	 * falling) they keep the view from before it, so the outcome is never told before the screen shows it.
	 */
	private List<String> shownLog = List.of();
	private String shownNotice = "";
	private boolean shownNoticeOk;
	private long shownCash;
	private String clockText = "";
	private int riskChoice = 7;
	private String daysText = "";
	private String cutText = "";

	protected CasinoScreen(CasinoPayload data) {
		super(Component.literal(CasinoBlock.Game.byId(data.game()).title));
		this.data = data;
		this.timer = data.timer();
		this.chip = Math.max(1, data.minBet());
		this.shownLog = data.log();
		this.shownCash = data.cash();
	}

	/** True while the screen is still revealing a result the server has already settled (see {@link #shownLog}). */
	protected boolean revealing() {
		return false;
	}

	/** The player's Jugs as the screen should show them now (held back while a result is revealed). */
	protected long shownCash() {
		return shownCash;
	}

	/** Opens or updates the screen for a game's view. */
	static void receive(CasinoPayload payload) {
		Minecraft client = Minecraft.getInstance();
		if (client.gui.screen() instanceof CasinoScreen screen && screen.data.pos() == payload.pos()) {
			screen.update(payload);
		} else if (payload.open()) {
			CasinoBlock.Game game = CasinoBlock.Game.byId(payload.game());
			CasinoScreen screen = switch (game) {
				case BLACKJACK -> new BlackjackScreen(payload);
				case ROULETTE -> new RouletteScreen(payload);
				case CRAPS -> new CrapsScreen(payload);
				case ARCADE -> new ArcadeScreen(payload);
				case PLINKO -> new PlinkoScreen(payload);
				case PRIZE_WHEEL -> new WheelScreen(payload);
				case COIN_PUSHER -> new PusherScreen(payload);
				case BACCARAT -> new BaccaratScreen(payload);
				case LOTTERY -> new LotteryScreen(payload);
				case DERBY -> new DerbyScreen(payload);
				default -> new SlotScreen(payload);
			};
			client.gui.setScreen(screen);
		}
	}

	protected void update(CasinoPayload payload) {
		CasinoPayload before = data;
		data = payload;
		timer = payload.timer();
		if (chip < payload.minBet() || chip > payload.maxBet()) {
			chip = Math.max(1, payload.minBet());
		}
		// The settings panel does not change with the game; rebuilding it would drop the text box being typed in.
		// Closing or saving the panel rebuilds everything anyway.
		if (needsRebuild(before, payload) && !limitsOpen) {
			rebuildWidgets();
		}
	}

	/**
	 * True while the owner's settings panel is open over the table: the table's own click areas must then let clicks
	 * through to the panel's buttons and boxes.
	 */
	protected boolean settingsOpen() {
		return limitsOpen;
	}

	/** True when the buttons on show change with this view. */
	protected abstract boolean needsRebuild(CasinoPayload before, CasinoPayload after);

	protected void send(Action action, int period, String target, long amount) {
		if (action == Action.CASINO_BET) {
			CasinoSound.chip(data.pos());
		}
		ClientPlayNetworking.send(new FinanceActionPayload(action, target, amount, period, "", data.pos()));
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public void tick() {
		super.tick();
		ticks++;
		if (timer > 0) {
			timer--;
		}
		tickCelebration();
	}

	// ------------------------------------------------------------------ wins: banners, coins and fanfare

	private static final String[] TIER_TITLES = {"", "BIG WIN!", "MEGA WIN!", "EPIC WIN!", "JACKPOT!"};
	private static final int[] TIER_COLOURS = {0, 0xFFFFD84A, 0xFF5AE0FF, 0xFFFF6AE0, 0xFFFFF4A0};

	private record Coin(double x, double y, double vx, double vy, int born, int spin) {
	}

	private int celebrationTier;
	private int celebrationAge = 1000;
	private long celebrationWin;
	private final List<Coin> coins = new ArrayList<>();

	/**
	 * Celebrates a win the screen has just shown: a chime for an ordinary win; for a win of 10, 25 or 50 times the stake
	 * (or a jackpot) a BIG, MEGA or EPIC WIN banner with a shower of coins and a fanfare that grows with it.
	 */
	protected void celebrate(long win, long stake, boolean jackpot) {
		if (win <= 0) {
			return;
		}
		double ratio = stake > 0 ? (double) win / stake : 0;
		int tier = jackpot ? 4 : ratio >= 50 ? 3 : ratio >= 25 ? 2 : ratio >= 10 ? 1 : 0;
		long pos = data.pos();
		CasinoSound.play(pos, net.minecraft.sounds.SoundEvents.EXPERIENCE_ORB_PICKUP, 0.5F, 1.4F);
		if (tier == 0) {
			return;
		}
		celebrationTier = tier;
		celebrationAge = 0;
		celebrationWin = win;
		CasinoSound.play(pos, net.minecraft.sounds.SoundEvents.PLAYER_LEVELUP, 0.7F, 1.0F);
		if (tier >= 2) {
			CasinoSound.play(pos, net.minecraft.sounds.SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 0.6F, 1.0F);
		}
		if (tier >= 3) {
			CasinoSound.play(pos, net.minecraft.sounds.SoundEvents.FIREWORK_ROCKET_TWINKLE, 0.8F, 1.0F);
		}
		if (tier >= 4) {
			CasinoSound.play(pos, net.minecraft.sounds.SoundEvents.NOTE_BLOCK_BELL.value(), 1.0F, 1.0F);
			CasinoSound.play(pos, net.minecraft.sounds.SoundEvents.FIREWORK_ROCKET_BLAST, 0.8F, 1.0F);
		}
	}

	/** How long the banner stays: longer for a bigger win. */
	private int celebrationLength() {
		return 60 + celebrationTier * 25;
	}

	private void tickCelebration() {
		celebrationAge++;
		if (celebrationTier > 0 && celebrationAge < celebrationLength()) {
			// A shower of coins: more of them the bigger the win.
			for (int i = 0; i < celebrationTier; i++) {
				int seed = ticks * 31 + i * 17;
				double x = left + 10 + Math.floorMod(seed * 7919, Math.max(1, panelWidth - 20));
				coins.add(new Coin(x, top + 6, (Math.floorMod(seed, 7) - 3) * 0.3, 0.5 + Math.floorMod(seed, 5) * 0.2, ticks,
						Math.floorMod(seed, 4)));
			}
			if (celebrationAge % 6 == 0) {
				CasinoSound.coin(data.pos(), 1.0F + Math.floorMod(celebrationAge, 5) * 0.12F);
			}
		}
		coins.removeIf(c -> ticks - c.born() > 60);
		if (celebrationAge >= celebrationLength()) {
			celebrationTier = 0;
		}
	}

	private void drawCelebration(GuiGraphicsExtractor g) {
		for (Coin c : coins) {
			double t = ticks - c.born() + partial;
			int x = (int) (c.x() + c.vx() * t);
			int y = (int) (c.y() + c.vy() * t + 0.09 * t * t);
			if (y > top + panelHeight - 4) {
				continue;
			}
			// A coin turning as it falls: wide, then edge on.
			int phase = (int) ((t / 3 + c.spin()) % 4);
			int w = phase == 0 ? 5 : phase == 2 ? 1 : 3;
			g.fill(x - w / 2, y, x - w / 2 + w, y + 5, 0xFFB8913F);
			g.fill(x - w / 2, y, x - w / 2 + Math.max(1, w - 1), y + 4, GOLD);
		}
		if (celebrationTier <= 0) {
			return;
		}
		int length = celebrationLength();
		// The banner grows in, holds, then fades.
		float grow = Math.min(1F, (celebrationAge + partial) / 6F);
		int alpha = celebrationAge > length - 12 ? Math.max(0, (int) (255 * (length - celebrationAge - partial) / 12F)) : 255;
		if (alpha <= 8) {
			return;
		}
		int cx = left + panelWidth / 2;
		int cy = top + panelHeight / 2 - 10;
		int halfW = (int) (Math.min(150, panelWidth / 2 - 20) * grow);
		int halfH = (int) (24 * grow);
		int a = alpha << 24;
		g.fill(cx - halfW - 2, cy - halfH - 2, cx + halfW + 2, cy + halfH + 2, a | 0x1A110A);
		g.fill(cx - halfW - 1, cy - halfH - 1, cx + halfW + 1, cy + halfH + 1, a | (TIER_COLOURS[celebrationTier] & 0xFFFFFF));
		g.fill(cx - halfW, cy - halfH, cx + halfW, cy + halfH, a | 0x140C08);
		// Chasing lights along the banner's edges.
		for (int x = cx - halfW + 2; x < cx + halfW - 2; x += 6) {
			boolean on = ((x - cx + celebrationAge * 3) / 6) % 2 == 0;
			int bulb = a | ((on ? TIER_COLOURS[celebrationTier] : 0x5A4A2A) & 0xFFFFFF);
			g.fill(x, cy - halfH + 2, x + 2, cy - halfH + 4, bulb);
			g.fill(x, cy + halfH - 4, x + 2, cy + halfH - 2, bulb);
		}
		if (grow < 1F) {
			return;
		}
		String title = TIER_TITLES[celebrationTier];
		int colour = (celebrationAge / 3) % 2 == 0 ? TIER_COLOURS[celebrationTier] : 0xFFFFFFFF;
		// The title twice, a pixel apart, for a heavy look.
		g.text(font, title, cx - font.width(title) / 2, cy - 12, (a | (colour & 0xFFFFFF)), true);
		g.text(font, title, cx - font.width(title) / 2 + 1, cy - 12, (a | (colour & 0xFFFFFF)), false);
		String amount = "+" + FinanceBook.amount(celebrationWin) + (celebrationWin == 1 ? " Jug" : " Jugs");
		g.text(font, amount, cx - font.width(amount) / 2, cy + 4, a | 0xFFF4E0, true);
	}

	/** Games that call {@link #celebrate} themselves; the others get a chime when the log shows the player winning. */
	protected boolean ownCelebration() {
		return false;
	}

	@Override
	public void onClose() {
		closeOnce();
		super.onClose();
	}

	@Override
	public void removed() {
		closeOnce();
		super.removed();
	}

	private void closeOnce() {
		if (!closing) {
			closing = true;
			send(Action.CASINO_CLOSE, 0, "", 0);
		}
	}

	protected int preferredWidth() {
		return 440;
	}

	protected int preferredHeight() {
		return 270;
	}

	protected int tableTop() {
		return top + FRAME + HEADER + 4;
	}

	protected int tableBottom() {
		return top + panelHeight - FRAME - BAR - 4;
	}

	@Override
	protected void init() {
		panelWidth = Math.min(preferredWidth(), width - 16);
		panelHeight = Math.min(preferredHeight(), height - 16);
		left = (width - panelWidth) / 2;
		top = (height - panelHeight) / 2;
		if (data.isOwner()) {
			int sw = settingsWidth();
			addRenderableWidget(new Plate(left + panelWidth - FRAME - sw - 6, top + FRAME + 5, sw, 13, limitsOpen ? "Close" : "Settings",
					Plate.Kind.WOOD, b -> {
						limitsOpen = !limitsOpen;
						minText = String.valueOf(data.minBet());
						maxText = String.valueOf(data.maxBet());
						bankChoice = data.bankHouse();
						clockText = table(1) > 0 ? String.valueOf(table(1)) : "";
						riskChoice = (int) Math.max(1, table(7));
						daysText = table(9) > 0 ? String.valueOf(table(9)) : "";
						cutText = String.valueOf(table(10));
						rebuildWidgets();
					}));
			if (limitsOpen) {
				int x = settingsX();
				int y = settingsY();
				CasinoBlock.Game game = CasinoBlock.Game.byId(data.game());
				addRenderableWidget(box(x + 72, y + 14, 60, minText, 12, t -> minText = t));
				addRenderableWidget(box(x + 72, y + 32, 60, maxText, 12, t -> maxText = t));
				addRenderableWidget(new Plate(x + 72, y + 50, 70, 14, bankChoice ? "The server" : "Me", Plate.Kind.WOOD, b -> {
					bankChoice = !bankChoice;
					rebuildWidgets();
				}));
				if (table(2) == 1) {
					addRenderableWidget(box(x + 72, y + 68, 30, clockText, 3, t -> clockText = t));
				}
				if (game == CasinoBlock.Game.PLINKO) {
					String[] names = {"Low", "Med", "High"};
					for (int r = 0; r < 3; r++) {
						int bit = 1 << r;
						boolean on = (riskChoice & bit) != 0;
						addRenderableWidget(new Plate(x + 72 + r * 31, y + 68, 29, 14, names[r], on ? Plate.Kind.TAB_OPEN : Plate.Kind.TAB, b -> {
							// At least one risk stays open.
							if ((riskChoice ^ bit) != 0) {
								riskChoice ^= bit;
							}
							rebuildWidgets();
						}));
					}
				}
				if (game == CasinoBlock.Game.LOTTERY) {
					addRenderableWidget(box(x + 72, y + 68, 30, daysText, 2, t -> daysText = t));
					addRenderableWidget(box(x + 72, y + 84, 30, cutText, 2, t -> cutText = t));
				}
				if (game == CasinoBlock.Game.DERBY) {
					addRenderableWidget(box(x + 72, y + 84, 30, cutText, 2, t -> cutText = t));
				}
				addRenderableWidget(new Plate(x + 176, y + 80, 96, 14, "Reset takings", Plate.Kind.WOOD,
						b -> send(Action.CASINO_LIMITS, 2, "", 0)));
				addRenderableWidget(new Plate(x + 176, y + 104, 96, 16, "Save", Plate.Kind.BRASS, b -> {
					String extra = "clock=" + blankZero(clockText) + ";risks=" + riskChoice + ";days=" + blankZero(daysText) + ";cut="
							+ blankZero(cutText);
					ClientPlayNetworking.send(new FinanceActionPayload(Action.CASINO_LIMITS, String.valueOf(FinanceBook.parseAmount(maxText)),
							FinanceBook.parseAmount(minText), bankChoice ? 1 : 0, extra, data.pos()));
					limitsOpen = false;
					rebuildWidgets();
				}));
				return;
			}
		}
		initGame();
	}

	protected abstract void initGame();

	/** How this game's screen is dressed (each game picks its own; see {@link CasinoTheme}). */
	protected CasinoTheme theme() {
		return CasinoTheme.WALNUT;
	}

	/** The owner's table info: see {@code CasinoBlockEntity.tableInfo}. */
	protected long table(int i) {
		long[] t = data.table();
		return t != null && i < t.length ? t[i] : 0;
	}

	private EditBox box(int x, int y, int w, String value, int length, java.util.function.Consumer<String> responder) {
		EditBox box = new EditBox(font, x, y, w, 14, Component.literal(""));
		box.setValue(value);
		box.setMaxLength(length);
		box.setResponder(responder);
		return box;
	}

	private static String blankZero(String text) {
		return text == null || text.trim().isEmpty() ? "0" : text.trim();
	}

	private int settingsX() {
		return left + panelWidth / 2 - 140;
	}

	private int settingsY() {
		return Math.max(tableTop() + 6, top + panelHeight / 2 - 72);
	}

	/** The Settings button fits its word. */
	private int settingsWidth() {
		return font.width("Settings") + 12;
	}

	/** The chip rack: the denominations within the table's limits, as plates; the picked one is lit. */
	protected int addChipRack(int x, int y) {
		int cx = x;
		for (long value : chips()) {
			long v = value;
			Plate plate = addRenderableWidget(new Plate(cx, y, 28, 14, shortAmount(value), v == chip ? Plate.Kind.TAB_OPEN : Plate.Kind.WOOD,
					b -> {
						chip = v;
						rebuildWidgets();
					}));
			plate.active = v != chip;
			cx += 30;
		}
		return cx;
	}

	protected List<Long> chips() {
		List<Long> out = new ArrayList<>();
		for (long value : CHIPS) {
			if (value >= data.minBet() && value <= data.maxBet()) {
				out.add(value);
			}
		}
		if (out.isEmpty()) {
			out.add(data.minBet());
		}
		while (out.size() > 6) {
			out.remove(out.size() - 1);
		}
		return out;
	}

	protected static String shortAmount(long value) {
		if (value >= 1_000_000 && value % 1_000_000 == 0) {
			return value / 1_000_000 + "M";
		}
		if (value >= 1_000 && value % 1_000 == 0) {
			return value / 1_000 + "k";
		}
		return String.valueOf(value);
	}

	// ------------------------------------------------------------------ drawing

	@Override
	public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
		super.extractBackground(g, mouseX, mouseY, delta);
		int right = left + panelWidth;
		int bottom = top + panelHeight;
		CasinoTheme theme = theme();
		theme.drawFrame(g, left, top, right, bottom, ticks);
		sunken(g, left + FRAME, top + FRAME, right - FRAME, top + FRAME + HEADER, theme.plaque(), theme.plaqueDark(), theme.plaqueLight());
		rivet(g, left + FRAME + 2, top + FRAME + 2);
		rivet(g, right - FRAME - 4, top + FRAME + 2);
		sunken(g, left + FRAME, bottom - FRAME - BAR, right - FRAME, bottom - FRAME, theme.plaque(), theme.plaqueDark(),
				theme.plaqueLight());
		drawTableBack(g, mouseX, mouseY);
		if (limitsOpen) {
			// The settings panel's backing goes under the widgets, so its boxes and buttons show on top of it.
			int x = settingsX();
			int y = settingsY();
			g.fill(x - 9, y - 9, x + 289, y + 141, GOLD_DIM);
			g.fill(x - 8, y - 8, x + 288, y + 140, PLAQUE);
			g.fill(x + 164, y - 2, x + 165, y + 134, GOLD_DIM);
		}
	}

	/** The table's felt and anything drawn under the buttons. */
	protected abstract void drawTableBack(GuiGraphicsExtractor g, int mouseX, int mouseY);

	/** Everything over the buttons. */
	protected abstract void drawTable(GuiGraphicsExtractor g, int mouseX, int mouseY);

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
		partial = delta;
		super.extractRenderState(g, mouseX, mouseY, delta);
		CasinoBlock.Game game = CasinoBlock.Game.byId(data.game());
		g.text(font, game.title.toUpperCase(java.util.Locale.ROOT), left + FRAME + 10, top + FRAME + 4, theme().title(), true);
		String who;
		if (data.isOwner()) {
			// The owner's own bets always play the bank: nobody wins Jugs off themselves.
			who = data.bankHouse() ? "the bank" : "you";
		} else {
			who = data.bankHouse() ? "the bank (" + data.owner() + "'s game)" : data.owner().isEmpty() ? "nobody" : data.owner();
		}
		String house = "House: " + who + " · Bets " + FinanceBook.amount(data.minBet()) + " to " + FinanceBook.amount(data.maxBet());
		g.text(font, font.plainSubstrByWidth(house, panelWidth - 140), left + FRAME + 10, top + FRAME + 14, DIM, false);
		if (!revealing()) {
			if (!ownCelebration() && shownLog != data.log()) {
				chimeForMyWins(shownLog, data.log());
			}
			shownLog = data.log();
			shownNotice = data.notice();
			shownNoticeOk = data.noticeOk();
			shownCash = data.cash();
		}
		String cash = "Your Jugs: " + FinanceBook.amount(shownCash);
		int cx = left + panelWidth - FRAME - (data.isOwner() ? settingsWidth() + 14 : 10) - font.width(cash);
		g.text(font, cash, cx, top + FRAME + 9, CREAM, false);
		if (limitsOpen) {
			drawLimits(g);
			return;
		}
		drawTable(g, mouseX, mouseY);
		// Bottom bar: the notice, else the last line of the log.
		int bx = left + FRAME + 8;
		int by = top + panelHeight - FRAME - BAR + 5;
		if (!shownNotice.isEmpty()) {
			g.text(font, font.plainSubstrByWidth(shownNotice, panelWidth - 20), bx, by, shownNoticeOk ? GAIN : LOSS, false);
		}
		List<String> log = shownLog;
		int lines = shownNotice.isEmpty() ? 3 : 2;
		int ly = by + (shownNotice.isEmpty() ? 0 : 10);
		for (int i = Math.max(0, log.size() - lines); i < log.size(); i++) {
			g.text(font, font.plainSubstrByWidth(log.get(i), panelWidth - 20), bx, ly, i == log.size() - 1 ? TAN : 0xFF8E8370, false);
			ly += 9;
		}
		drawCelebration(g);
	}

	/** Table games: a chime (and a fanfare for a big one) when a new log line says this player won. */
	private void chimeForMyWins(List<String> before, List<String> after) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || after.isEmpty()) {
			return;
		}
		String me = mc.player.getName().getString();
		int start = 0;
		if (!before.isEmpty()) {
			int at = after.lastIndexOf(before.get(before.size() - 1));
			start = at < 0 ? 0 : at + 1;
		}
		long best = 0;
		for (int i = start; i < after.size(); i++) {
			String line = after.get(i);
			if (line.startsWith(me + " ") && line.contains(" win")) {
				best = Math.max(best, firstNumber(line.substring(me.length())));
			}
		}
		if (best > 0) {
			// The log does not say what was staked: a banner for a win of 500 or more, a chime for the rest.
			celebrate(best, best >= 500 ? best / 10 : best, false);
		}
	}

	private static long firstNumber(String text) {
		StringBuilder digits = new StringBuilder();
		for (char c : text.toCharArray()) {
			if (Character.isDigit(c)) {
				digits.append(c);
			} else if (c == ',' && digits.length() > 0) {
				continue;
			} else if (digits.length() > 0) {
				break;
			}
		}
		try {
			return digits.length() == 0 ? 1 : Long.parseLong(digits.toString());
		} catch (NumberFormatException e) {
			return 1;
		}
	}

	private void drawLimits(GuiGraphicsExtractor g) {
		int x = settingsX();
		int y = settingsY();
		CasinoBlock.Game game = CasinoBlock.Game.byId(data.game());
		// The settings, on the left.
		g.text(font, "Table settings", x, y - 2, GOLD, false);
		g.text(font, game == CasinoBlock.Game.LOTTERY ? "Ticket price" : "Least bet", x, y + 17, DIM, false);
		g.text(font, "Most bet", x, y + 35, DIM, false);
		g.text(font, "House", x, y + 53, DIM, false);
		if (table(2) == 1) {
			g.text(font, "Betting clock", x, y + 71, DIM, false);
			g.text(font, clockText.trim().isEmpty() ? "(" + table(0) + "s)" : "s", x + 106, y + 71, 0xFF8E8370, false);
		}
		if (game == CasinoBlock.Game.PLINKO) {
			g.text(font, "Risks allowed", x, y + 71, DIM, false);
		}
		if (game == CasinoBlock.Game.DERBY) {
			g.text(font, "House cut", x, y + 87, DIM, false);
			g.text(font, "% of each pool", x + 106, y + 87, 0xFF8E8370, false);
		}
		if (game == CasinoBlock.Game.LOTTERY) {
			g.text(font, "Draw every", x, y + 71, DIM, false);
			g.text(font, daysText.trim().isEmpty() ? "(" + table(8) + " days)" : "days", x + 106, y + 71, 0xFF8E8370, false);
			g.text(font, "House cut", x, y + 87, DIM, false);
			g.text(font, "%", x + 106, y + 87, 0xFF8E8370, false);
		}
		g.text(font, bankChoice ? "The bank pays and keeps." : "You pay others' wins.", x, y + 101, TAN, false);
		g.text(font, "Your own bets play the bank.", x, y + 113, TAN, false);
		g.text(font, "Server allows " + FinanceBook.amount(data.limitMin()) + " to " + FinanceBook.amount(data.limitMax()), x, y + 125,
				DIM, false);
		// The takings, on the right.
		int tx = x + 176;
		int tr = x + 272;
		g.text(font, "Takings", tx, y - 2, GOLD, false);
		long in = table(3);
		long out = table(4);
		row(g, "Taken in", FinanceBook.amount(in), tx, tr, y + 14, CREAM);
		row(g, "Paid out", FinanceBook.amount(out), tx, tr, y + 26, CREAM);
		long kept = in - out;
		row(g, "Kept", (kept < 0 ? "-" : "") + FinanceBook.amount(Math.abs(kept)), tx, tr, y + 38, kept >= 0 ? GAIN : LOSS);
		row(g, "Biggest win", FinanceBook.amount(Math.max(0, table(5))), tx, tr, y + 50, CREAM);
		row(g, "Bets", FinanceBook.amount(table(6)), tx, tr, y + 62, CREAM);
	}

	private void row(GuiGraphicsExtractor g, String label, String value, int x, int right, int y, int colour) {
		g.text(font, label, x, y, DIM, false);
		g.text(font, value, right - font.width(value), y, colour, false);
	}

	// ------------------------------------------------------------------ shared pieces

	protected static void oval(GuiGraphicsExtractor g, int cx, int cy, int rx, int ry, int color) {
		if (rx <= 0 || ry <= 0) {
			return;
		}
		for (int dy = -ry; dy < ry; dy++) {
			double t = (dy + 0.5) / ry;
			int half = (int) Math.round(rx * Math.sqrt(Math.max(0, 1 - t * t)));
			g.fill(cx - half, cy + dy, cx + half, cy + dy + 1, color);
		}
	}

	/** A chip of a value's colour, 9 pixels across, with its value under or on it. */
	protected void chip(GuiGraphicsExtractor g, int x, int y, long value, boolean label) {
		int colour = CHIP_COLOURS[0];
		for (int i = CHIPS.length - 1; i >= 0; i--) {
			if (value >= CHIPS[i]) {
				colour = CHIP_COLOURS[i];
				break;
			}
		}
		g.fill(x + 1, y, x + 8, y + 9, 0xFF1A110A);
		g.fill(x, y + 1, x + 9, y + 8, 0xFF1A110A);
		g.fill(x + 1, y + 1, x + 8, y + 8, colour);
		g.fill(x + 3, y + 3, x + 6, y + 6, 0xFFEFE2C2);
		g.fill(x + 4, y + 1, x + 5, y + 2, 0xFFEFE2C2);
		g.fill(x + 4, y + 7, x + 5, y + 8, 0xFFEFE2C2);
		if (label) {
			String text = shortAmount(value);
			g.text(font, text, x + 10, y + 1, CREAM, true);
		}
	}

	/** One card: face up, face down (-2) or nothing (-1). Cards as in {@link PokerHands}. */
	protected void card(GuiGraphicsExtractor g, int x, int y, int w, int h, int card) {
		CardArt.draw(g::fill, x, y, w, h, card);
	}

	protected int seconds() {
		return Math.max(0, (timer + 19) / 20);
	}
}
