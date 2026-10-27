package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.spirits.PactStatePayload;
import io.github.jimbozoomer.jugcraft.concordance.spirits.RosterRequestPayload;
import io.github.jimbozoomer.jugcraft.concordance.spirits.RosterStatePayload;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import org.jspecify.annotations.Nullable;

/**
 * Concordance Chunk D, D-07 (D-S11): the roster, native and narrated. Every pact and agreement the player holds (one without
 * its place included), their courier requests and the rest of their familiars, spirits and constructs, as the server lists
 * them: a portrait (a render of the worker's own model; a request's item), its kind, service, place and whether it is
 * loaded, current task, next obligation and why it waits, each state and place in words beside a mark of its own (never
 * colour alone). Under each row its actions: suspend, resume, end (which asks first, the keyboard on Keep it), cancel and
 * recover; each sends one bounded request through {@link RosterClient}, and the server's answer, a refusal included, is
 * shown with its mark (PactScreen.Verdict) and read aloud. Eight rows a page (Page Up and Page Down turn it), the page
 * scrolling within. Every row is one focusable, narrated line and every action a button: Tab and Shift+Tab move, Enter or
 * Space presses, Esc closes. It fits the smallest GUI (ScreenKit.Fitted; ScreenFitClientGameTests) and, an in-game screen,
 * draws subtitles over itself. No text box, chat command, vanilla Dialog or GuiLib, and no second inventory: cargo is the
 * courier ledger's and only the server moves it.
 */
public class RosterScreen extends Screen implements ScreenKit.Fitted {
	/** Rows a page shows (the server sends at most RosterStatePayload.MAX_ROWS: at most eight pages). */
	public static final int ROWS_PER_PAGE = 8;
	private static final int GOLD = 0xFFE6C586;
	private static final int TEXT = 0xFFE2E7F1;
	private static final int DIM = 0xFF9CAAC3;
	private static final int GOOD = 0xFFA6D6C3;
	private static final int WARN = 0xFFE8B07A;
	private static final int MAX_WIDTH = 440;
	private static final int MAX_HEIGHT = 300;
	private static final int TOP = 26;
	private static final int FOOT = 20;
	private static final int PORTRAIT = 28;
	private static final int ACTION_HEIGHT = 18;
	private static final int MARK = 9;
	/** The place's marks, beside the states' (RosterStatePayload's numbers). */
	private static final int LOADED = 100;
	private static final int NOT_LOADED = 101;
	/** A row's actions, in the order their keys sit under it. */
	private static final int[] ACTIONS = {RosterRequestPayload.SUSPEND, RosterRequestPayload.RESUME, RosterRequestPayload.END,
			RosterRequestPayload.CANCEL, RosterRequestPayload.RECOVER};
	private final Runnable listener = () -> dirty = true;
	private final Map<String, AbstractWidget> controls = new LinkedHashMap<>();
	/** Each control's row (its place in the listing), so the keyboard stays with a row's place when the row goes. */
	private final Map<String, Integer> places = new HashMap<>();
	private final Set<String> footer = new LinkedHashSet<>();
	/** How each row's portrait was last drawn ("portrait" or "icon"), by row id, for tests. */
	private final Map<UUID, String> drawn = new HashMap<>();
	private @Nullable ComposerScroll body;
	private boolean dirty;
	private boolean fromTop;
	private int x;
	private int y;
	private int w;
	private int h;
	private int at;
	private int page;
	private String focus = "";
	private int focusRow = -1;

	public RosterScreen() {
		super(Component.translatable("screen.jugcraft.roster.title"));
	}

	static Component ui(String key, Object... args) {
		return Component.translatable("screen.jugcraft.roster." + key, args);
	}

	/** Forgets the portraits (RosterClient does, on leaving a world). */
	static void forget() {
		Portraits.clear();
	}

	@Override
	protected void init() {
		RosterClient.unlisten(listener);
		RosterClient.listen(listener);
		double scroll = body == null || fromTop ? 0 : body.scrollAmount();
		fromTop = false;
		controls.clear();
		places.clear();
		footer.clear();
		at = 0;
		w = Math.min(MAX_WIDTH, width - 16);
		h = Math.min(MAX_HEIGHT, height - 12);
		x = (width - w) / 2;
		y = (height - h) / 2;
		RosterStatePayload state = RosterClient.state();
		List<RosterStatePayload.Row> rows = state == null ? List.of() : state.rows();
		int pages = pages();
		page = Math.max(0, Math.min(page, pages - 1));
		body = addRenderableWidget(new ComposerScroll(minecraft, x + 10, y + TOP, w - 20, Math.max(24, h - TOP - FOOT - 10), this::focused));
		answer();
		if (state == null) {
			say(ui(RosterClient.available() ? "opening" : "unavailable"), DIM, "opening");
		} else {
			if (!state.enabled()) {
				say(ui("disabled"), WARN, "note");
			}
			if (rows.isEmpty()) {
				say(ui("empty"), TEXT, "empty");
				say(ui("empty.hint"), DIM, "");
			}
			for (int i = page * ROWS_PER_PAGE; i < Math.min(rows.size(), (page + 1) * ROWS_PER_PAGE); i++) {
				row(rows.get(i), i);
			}
			if (state.more() > 0 && page == pages - 1) {
				say(ui("more", state.more(), RosterStatePayload.MAX_ROWS), DIM, "more");
			}
		}
		boolean free = !RosterClient.waiting() && RosterClient.available();
		footer(button("previous", ui("previous"), ui("previous.hint"), page > 0, () -> turn(page - 1)),
				button("next", ui("next"), ui("next.hint"), page + 1 < pages, () -> turn(page + 1)),
				button("refresh", ui("refresh"), ui("refresh.hint"), free, RosterClient::refresh),
				button("close", ui("close"), Component.empty(), true, this::onClose));
		body.arrange(scroll);
		restore(rows);
	}

	/** The server's answer to the last action, with its mark (D-06's Verdict), kept until the next is sent; while one waits, that it waits. */
	private void answer() {
		if (RosterClient.waiting()) {
			say(ui("waiting"), DIM, "result");
			return;
		}
		PactStatePayload.Result answer = RosterClient.result();
		if (answer != null) {
			int colour = answer.done() ? GOOD : WARN;
			PactScreen.Verdict line = new PactScreen.Verdict(answer.message().copy().withColor(colour & 0xFFFFFF), answer.done(), font,
					body.getWidth() - 10, colour);
			line.setX(body.getX() + 2);
			body.add(line, at);
			at += line.getHeight() + 3;
			controls.put("result", line);
		}
	}

	private void row(RosterStatePayload.Row row, int index) {
		Card card = new Card(body.getX() + 2, row, body.getWidth() - 10);
		body.add(card, at);
		at += card.getHeight() + 2;
		keep("row:" + row.id(), card, index);
		List<Integer> offered = new ArrayList<>();
		for (int action : ACTIONS) {
			if (row.offers(action)) {
				offered.add(action);
			}
		}
		if (!offered.isEmpty()) {
			int left = card.getX() + PORTRAIT + 6;
			int room = card.getX() + card.getWidth() - left;
			int each = Math.max(24, Math.min(96, (room - 4 * (offered.size() - 1)) / offered.size()));
			boolean free = !RosterClient.waiting() && RosterClient.available();
			for (int i = 0; i < offered.size(); i++) {
				int action = offered.get(i);
				String name = RosterRequestPayload.ACTIONS.get(action);
				// Cancel does not ask (nothing is lost); its hint says, in the server's words, what it does.
				Component hint = action == RosterRequestPayload.CANCEL && !row.confirm().getString().isEmpty() ? row.confirm() : ui(name + ".hint", row.name());
				BookArt.Tab tab = new BookArt.Tab(left + i * (each + 4), 0, each, ACTION_HEIGHT, ui(name), hint, "", false, true, unused -> act(row, action));
				tab.active = free;
				body.add(tab, at);
				keep(name + ":" + row.id(), tab, index);
			}
			at += ACTION_HEIGHT;
		}
		at += 8;
	}

	private void act(RosterStatePayload.Row row, int action) {
		if (action == RosterRequestPayload.END) {
			confirmEnd(row);
		} else {
			RosterClient.request(action, row.id(), row.revision());
		}
	}

	/**
	 * Ending asks first (D-07): only its yes sends END, for the row and revision it was asked about, with the server's words
	 * (what comes back, once); Esc and Keep it keep it, and the keyboard starts on Keep it (KeepFirstConfirmScreen).
	 */
	private void confirmEnd(RosterStatePayload.Row row) {
		minecraft.gui.setScreen(new KeepFirstConfirmScreen(yes -> {
			minecraft.gui.setScreen(this);
			if (yes) {
				RosterClient.request(RosterRequestPayload.END, row.id(), row.revision());
			}
		}, ui("end.title", row.name()), row.confirm().getString().isEmpty() ? ui("end.confirm", row.name(), row.kind()) : row.confirm(),
				ui("end.yes"), ui("end.no")));
	}

	private void turn(int to) {
		int next = Math.max(0, Math.min(to, pages() - 1));
		if (next == page) {
			return;
		}
		page = next;
		fromTop = true;
		dirty = true;
		minecraft.getNarrator().saySystemNow(heading());
	}

	private void keep(String key, AbstractWidget widget, int index) {
		controls.put(key, widget);
		places.put(key, index);
	}

	private void say(Component text, int colour, String key) {
		BookArt.Paragraph line = new BookArt.Paragraph(text.copy().withColor(colour & 0xFFFFFF), font, body.getWidth() - 10, colour);
		line.setX(body.getX() + 2);
		body.add(line, at);
		at += line.getHeight() + 3;
		if (!key.isEmpty()) {
			controls.put(key, line);
		}
	}

	private Button button(String key, Component label, Component hint, boolean active, Runnable action) {
		BookArt.Tab tab = new BookArt.Tab(0, 0, 40, FOOT, label, hint, "", false, true, unused -> action.run());
		tab.active = active;
		controls.put(key, tab);
		footer.add(key);
		return addRenderableWidget(tab);
	}

	private void footer(Button... buttons) {
		int gap = 4;
		int each = Math.max(24, (w - 20 - gap * (buttons.length - 1)) / buttons.length);
		for (int i = 0; i < buttons.length; i++) {
			buttons[i].setX(x + 10 + i * (each + gap));
			buttons[i].setY(y + h - FOOT - 6);
			buttons[i].setWidth(each);
		}
	}

	/**
	 * The keyboard stays where it was: on the same control after a refresh or an answer; when that control is gone (its row
	 * ended) or cannot be pressed (an answer is awaited), on the row now in its place on this page, else the page's first
	 * line; before anything was focused, setInitialFocus decides.
	 */
	private void restore(List<RosterStatePayload.Row> rows) {
		if (focus.isEmpty() || body == null) {
			return;
		}
		AbstractWidget widget = controls.get(focus);
		if (widget == null || !widget.active) {
			widget = null;
			int first = page * ROWS_PER_PAGE;
			int last = Math.min(rows.size(), first + ROWS_PER_PAGE) - 1;
			if (last >= first) {
				int index = focusRow < 0 ? first : Math.max(first, Math.min(focusRow, last));
				widget = controls.get("row:" + rows.get(index).id());
			}
			if (widget == null) {
				widget = controls.containsKey("empty") ? controls.get("empty") : controls.get("close");
			}
		}
		if (widget == null || !widget.active) {
			return;
		}
		if (body.children().contains(widget)) {
			setFocused(body);
			body.setFocused(widget);
		} else {
			setFocused(widget);
		}
	}

	private void focused(@Nullable GuiEventListener target) {
		controls.forEach((key, widget) -> {
			if (widget == target) {
				focus = key;
				focusRow = places.getOrDefault(key, -1);
			}
		});
	}

	@Override
	public void setFocused(@Nullable GuiEventListener target) {
		super.setFocused(target);
		focused(target);
	}

	@Override
	protected void setInitialFocus() {
		// After a rebuild the keyboard is where restore put it; vanilla's would move it on by one.
		if (getFocused() == null) {
			super.setInitialFocus();
		}
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (event.input() == InputConstants.KEY_PAGEDOWN || event.input() == InputConstants.KEY_PAGEUP) {
			turn(page + (event.input() == InputConstants.KEY_PAGEDOWN ? 1 : -1));
			return true;
		}
		return super.keyPressed(event);
	}

	@Override
	public void tick() {
		super.tick();
		if (dirty) {
			dirty = false;
			rebuildWidgets();
		}
	}

	private Component heading() {
		RosterStatePayload state = RosterClient.state();
		return state == null ? Component.empty() : ui("page", page + 1, pages(), state.rows().size());
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
		// The Concordance's dark panel, without the animated blur; nothing here moves (reduced motion asks nothing more).
		g.fill(0, 0, width, height, 0xCF050913);
		g.fill(x, y, x + w, y + h, 0xFF0C1424);
		g.outline(x, y, w, h, 0xFF74613F);
		g.outline(x + 3, y + 3, w - 6, h - 6, 0xFF243148);
		BookArt.icon(g, "jugcraft:spirit_anchor", x + 8, y + 5);
		Component heading = heading();
		int right = font.width(heading);
		g.text(font, BookArt.fit(font, title.getString(), w - 44 - right), x + 28, y + 10, GOLD, false);
		g.text(font, heading, x + w - 10 - right, y + 10, DIM, false);
		g.fill(x + 8, y + TOP - 3, x + w - 8, y + TOP - 2, 0xFF344158);
		// Subtitles over the roster (the HUD defers them to here for an in-game screen, as vanilla's extractBackground draws them).
		minecraft.gui.hud.extractDeferredSubtitles();
	}

	@Override
	public boolean isInGameUi() {
		// An in-game screen, as a chest's: the HUD defers its subtitles to extractBackground (Hud.extractSubtitleOverlay).
		return true;
	}

	@Override
	public Component getNarrationMessage() {
		return CommonComponents.joinForNarration(title, heading());
	}

	@Override
	protected Component getUsageNarration() {
		return ui("help");
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public void removed() {
		RosterClient.unlisten(listener);
		super.removed();
	}

	@Override
	public ScreenRectangle panelArea() {
		return new ScreenRectangle(x, y, w, h);
	}

	/** The page shown, from 0 (tests). */
	public int page() {
		return page;
	}

	public int pages() {
		RosterStatePayload state = RosterClient.state();
		int rows = state == null ? 0 : state.rows().size();
		return Math.max(1, (rows + ROWS_PER_PAGE - 1) / ROWS_PER_PAGE);
	}

	/** The page row {@code id} is on, or -1 (tests). */
	public int pageOf(UUID id) {
		RosterStatePayload state = RosterClient.state();
		if (state != null) {
			for (int i = 0; i < state.rows().size(); i++) {
				if (state.rows().get(i).id().equals(id)) {
					return i / ROWS_PER_PAGE;
				}
			}
		}
		return -1;
	}

	/** How row {@code id}'s portrait was last drawn: "portrait", "icon", or "" if not yet (tests). */
	public String drawn(UUID id) {
		return drawn.getOrDefault(id, "");
	}

	/** The key of the control or line the keyboard is on ("" for none), for tests. */
	public String focusedKey() {
		GuiEventListener leaf = getFocused();
		while (leaf instanceof ContainerEventHandler container && container.getFocused() != null) {
			leaf = container.getFocused();
		}
		for (Map.Entry<String, AbstractWidget> entry : controls.entrySet()) {
			if (entry.getValue() == leaf) {
				return entry.getKey();
			}
		}
		return "";
	}

	/** The control or line under {@code key}, or null, for tests. */
	public @Nullable AbstractWidget control(String key) {
		return controls.get(key);
	}

	/** What does not fit: the panel off screen, the body too small, a control outside the panel or body or narrower than 20 (tests: none). */
	public List<String> outside() {
		List<String> out = new ArrayList<>();
		if (x < 0 || y < 0 || x + w > width || y + h > height) {
			out.add("panel");
		}
		if (body != null && (body.getY() + body.getHeight() > y + h - FOOT - 6 || body.getHeight() < 24)) {
			out.add("body");
		}
		for (Map.Entry<String, AbstractWidget> entry : controls.entrySet()) {
			AbstractWidget widget = entry.getValue();
			boolean inside = body != null && body.children().contains(widget);
			int left = inside ? body.getX() : x;
			int right = inside ? body.getX() + body.getWidth() : x + w;
			if (widget.getX() < left || widget.getX() + widget.getWidth() > right || widget.getWidth() < 20
					|| !inside && (widget.getY() < y || widget.getY() + widget.getHeight() > y + h)) {
				out.add(entry.getKey());
			}
		}
		return out;
	}

	private void portrait(GuiGraphicsExtractor g, RosterStatePayload.Row row, int x0, int y0, int x1, int y1) {
		LivingEntity creature = row.request() ? null : Portraits.of(row.portrait());
		if (creature != null) {
			int size = (int) Math.max(4.0F, Math.min((y1 - y0 - 2) / Math.max(0.1F, creature.getBbHeight()),
					(x1 - x0 - 2) / Math.max(0.1F, creature.getBbWidth())));
			// Facing the reader and still: it does not follow the mouse.
			InventoryScreen.extractEntityInInventoryFollowsMouse(g, x0, y0, x1, y1, size, 0.0625F, (x0 + x1) / 2.0F, (y0 + y1) / 2.0F, creature);
			drawn.put(row.id(), "portrait");
		} else {
			BookArt.icon(g, icon(row), x0 + (x1 - x0 - 16) / 2, y0 + (y1 - y0 - 16) / 2);
			drawn.put(row.id(), "icon");
		}
	}

	/** A request's item, or the item a worker's type names; the Spirit Anchor's when neither is an item. */
	private static String icon(RosterStatePayload.Row row) {
		Identifier id = row.portrait().isEmpty() ? null : Identifier.tryParse(row.portrait());
		return id != null && BuiltInRegistries.ITEM.containsKey(id) ? row.portrait() : "jugcraft:spirit_anchor";
	}

	private static Component stateWord(int icon) {
		return icon >= 0 && icon < RosterStatePayload.ICONS.size()
				? Component.translatable("screen.jugcraft.roster.state." + RosterStatePayload.ICONS.get(icon)) : Component.empty();
	}

	private static Component said(RosterStatePayload.Row row) {
		Component word = stateWord(row.icon());
		return row.waiting().getString().isEmpty() ? word : ui("row.state", word, row.waiting());
	}

	/** The row in words, as the narrator reads it and tests read it. */
	private static Component words(RosterStatePayload.Row row) {
		List<Component> parts = new ArrayList<>();
		parts.add(ui("row.title", row.name(), row.kind()));
		parts.add(said(row));
		if (!row.service().getString().isEmpty()) {
			parts.add(row.service());
		}
		if (!row.place().getString().isEmpty()) {
			parts.add(ui(row.loaded() ? "row.loaded" : "row.unloaded", row.place()));
		}
		if (!row.task().getString().isEmpty()) {
			parts.add(ui("row.task", row.task()));
		}
		if (!row.next().getString().isEmpty()) {
			parts.add(ui("row.obligation", row.next()));
		}
		return CommonComponents.joinForNarration(parts.toArray(Component[]::new));
	}

	/** A state's (or the place's) mark, 9 by 9 at (x, y): a shape of its own, always beside its words, so no meaning rests on colour. */
	static void mark(GuiGraphicsExtractor g, int mark, int x, int y) {
		switch (mark) {
			case RosterStatePayload.ACTIVE -> {
				// At work: a wedge pointing on.
				for (int r = 0; r < MARK; r++) {
					g.fill(x + 1, y + r, x + 2 + Math.min(r, MARK - 1 - r), y + r + 1, 0xFF8FD694);
				}
			}
			case RosterStatePayload.WAITING -> {
				// Waiting: an hourglass.
				for (int r = 0; r < MARK; r++) {
					int half = Math.abs(4 - r);
					g.fill(x + 4 - half, y + r, x + 5 + half, y + r + 1, 0xFFE8B07A);
				}
			}
			case RosterStatePayload.SUSPENDED -> {
				// Suspended: two bars.
				g.fill(x + 1, y, x + 4, y + MARK, 0xFFC9B6EA);
				g.fill(x + 5, y, x + 8, y + MARK, 0xFFC9B6EA);
			}
			case RosterStatePayload.UPKEEP -> {
				// An offering due, or held for one: a diamond with a stroke and a dot.
				for (int r = 0; r < MARK; r++) {
					int half = r < 5 ? r : 8 - r;
					g.fill(x + 4 - half, y + r, x + 5 + half, y + r + 1, 0xFFF0A35E);
				}
				g.fill(x + 4, y + 2, x + 5, y + 5, 0xFF1A1206);
				g.fill(x + 4, y + 6, x + 5, y + 7, 0xFF1A1206);
			}
			case RosterStatePayload.UNLOADED, NOT_LOADED -> {
				// Not loaded: a dotted square (as it last reported).
				for (int i = 0; i < MARK; i += 2) {
					g.fill(x + i, y, x + i + 1, y + 1, 0xFF9CAAC3);
					g.fill(x + i, y + 8, x + i + 1, y + 9, 0xFF9CAAC3);
					g.fill(x, y + i, x + 1, y + i + 1, 0xFF9CAAC3);
					g.fill(x + 8, y + i, x + 9, y + i + 1, 0xFF9CAAC3);
				}
			}
			case LOADED -> g.fill(x + 1, y + 1, x + 8, y + 8, 0xFF8FD694);
			case RosterStatePayload.LOST -> {
				// Its presence lost: an empty ring with a gap at its foot.
				ring(g, x, y, 0xFFB7C3D6);
				g.fill(x + 3, y + 8, x + 6, y + 9, 0xFF0C1424);
			}
			case RosterStatePayload.WITHOUT_PLACE -> {
				// Without its place: a cross.
				for (int i = 0; i < MARK; i++) {
					g.fill(x + i, y + i, x + i + 1, y + i + 1, 0xFFF0605A);
					g.fill(x + 8 - i, y + i, x + 9 - i, y + i + 1, 0xFFF0605A);
				}
			}
			case RosterStatePayload.UNCLAIMED -> {
				// Not yet claimed: a place's four corners, nothing inside.
				int c = 0xFFB7C3D6;
				g.fill(x, y, x + 3, y + 1, c);
				g.fill(x, y, x + 1, y + 3, c);
				g.fill(x + 6, y, x + 9, y + 1, c);
				g.fill(x + 8, y, x + 9, y + 3, c);
				g.fill(x, y + 8, x + 3, y + 9, c);
				g.fill(x, y + 6, x + 1, y + 9, c);
				g.fill(x + 6, y + 8, x + 9, y + 9, c);
				g.fill(x + 8, y + 6, x + 9, y + 9, c);
			}
			case RosterStatePayload.IN_TRANSIT -> {
				// In transit: an arrow going on.
				g.fill(x, y + 4, x + 6, y + 5, 0xFF8FD694);
				for (int i = 0; i < 4; i++) {
					g.fill(x + 5 + i, y + 1 + i, x + 6 + i, y + 8 - i, 0xFF8FD694);
				}
			}
			case RosterStatePayload.RETURNING -> {
				// Going back: an arrow turned home.
				g.fill(x + 3, y + 4, x + 9, y + 5, 0xFFE8B07A);
				for (int i = 0; i < 4; i++) {
					g.fill(x + 3 - i, y + 1 + i, x + 4 - i, y + 8 - i, 0xFFE8B07A);
				}
			}
			case RosterStatePayload.STRANDED -> {
				// Stranded cargo: a banded crate.
				g.fill(x, y + 1, x + 9, y + 9, 0xFFC99A5B);
				g.fill(x, y + 4, x + 9, y + 5, 0xFF4A3218);
				g.fill(x + 4, y + 1, x + 5, y + 9, 0xFF4A3218);
			}
			case RosterStatePayload.DISABLED -> {
				// Switched off: a ring struck through.
				ring(g, x, y, 0xFF8B91A1);
				for (int i = 1; i < 8; i++) {
					g.fill(x + 8 - i, y + i, x + 9 - i, y + i + 1, 0xFF8B91A1);
				}
			}
			default -> {
			}
		}
	}

	private static void ring(GuiGraphicsExtractor g, int x, int y, int colour) {
		g.fill(x + 2, y, x + 7, y + 1, colour);
		g.fill(x + 2, y + 8, x + 7, y + 9, colour);
		g.fill(x, y + 2, x + 1, y + 7, colour);
		g.fill(x + 8, y + 2, x + 9, y + 7, colour);
		g.fill(x + 1, y + 1, x + 2, y + 2, colour);
		g.fill(x + 7, y + 1, x + 8, y + 2, colour);
		g.fill(x + 1, y + 7, x + 2, y + 8, colour);
		g.fill(x + 7, y + 7, x + 8, y + 8, colour);
	}

	private record Line(List<FormattedCharSequence> parts, int colour, int mark) {
	}

	/**
	 * One row: its portrait and its lines, each state and place marked by a shape beside its words. It takes the keyboard so
	 * the narrator reads the row as one; its actions are the keys under it.
	 */
	private final class Card extends AbstractWidget {
		private final RosterStatePayload.Row row;
		private final List<Line> lines = new ArrayList<>();

		Card(int x, RosterStatePayload.Row row, int width) {
			super(x, 0, width, PORTRAIT + 4, words(row));
			this.row = row;
			int text = width - PORTRAIT - 10;
			line(ui("row.title", row.name(), row.kind()), GOLD, -1, text);
			line(said(row), TEXT, row.icon(), text);
			line(row.service(), DIM, -1, text);
			if (!row.place().getString().isEmpty()) {
				line(ui(row.loaded() ? "row.loaded" : "row.unloaded", row.place()), DIM, row.loaded() ? LOADED : NOT_LOADED, text);
			}
			if (!row.task().getString().isEmpty()) {
				line(ui("row.task", row.task()), TEXT, -1, text);
			}
			if (!row.next().getString().isEmpty()) {
				line(ui("row.obligation", row.next()), TEXT, -1, text);
			}
			int tall = 4;
			for (Line line : lines) {
				tall += line.parts().size() * (font.lineHeight + 1);
			}
			setHeight(Math.max(PORTRAIT + 4, tall));
		}

		private void line(Component text, int colour, int mark, int width) {
			if (!text.getString().isEmpty()) {
				lines.add(new Line(font.split(text, mark < 0 ? width : width - MARK - 2), colour, mark));
			}
		}

		@Override
		protected void extractWidgetRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
			int x0 = getX();
			int y0 = getY();
			g.fill(x0, y0, x0 + width, y0 + height, isFocused() || isHovered() ? 0x50243148 : 0x28243148);
			g.fill(x0 + 1, y0 + 1, x0 + PORTRAIT + 1, y0 + PORTRAIT + 1, 0xFF070C16);
			g.outline(x0 + 1, y0 + 1, PORTRAIT, PORTRAIT, 0xFF344158);
			portrait(g, row, x0 + 2, y0 + 2, x0 + PORTRAIT, y0 + PORTRAIT);
			int tx = x0 + PORTRAIT + 6;
			int ty = y0 + 2;
			for (Line line : lines) {
				if (line.mark() >= 0) {
					mark(g, line.mark(), tx, ty);
				}
				int lx = line.mark() >= 0 ? tx + MARK + 2 : tx;
				for (FormattedCharSequence part : line.parts()) {
					g.text(font, part, lx, ty, line.colour(), false);
					ty += font.lineHeight + 1;
				}
			}
			if (isFocused()) {
				g.outline(x0, y0, width, height, GOLD);
			}
		}

		@Override
		protected void updateWidgetNarration(NarrationElementOutput output) {
			output.add(NarratedElementType.TITLE, getMessage());
			output.add(NarratedElementType.USAGE, ui(row.actions() == 0 ? "row.usage.none" : "row.usage"));
		}
	}

	/**
	 * One visual-only creature per entity type a row names, never added to a level or ticked (the transport crate preview's
	 * pattern, reimplemented here): at most MAX, forgotten when the world changes or is left. A GeckoLib creature draws
	 * through its own renderer, holding its first frame.
	 */
	private static final class Portraits {
		private static final int MAX = 16;
		private static final Map<String, Optional<LivingEntity>> MADE = new LinkedHashMap<>(16, 0.75F, true);
		private static @Nullable ClientLevel level;
		private static int nextId = -3_000_000;

		private Portraits() {
		}

		static @Nullable LivingEntity of(String type) {
			ClientLevel world = Minecraft.getInstance().level;
			if (world == null || type.isEmpty()) {
				return null;
			}
			if (world != level) {
				MADE.clear();
				level = world;
			}
			Optional<LivingEntity> known = MADE.get(type);
			if (known != null) {
				return known.orElse(null);
			}
			LivingEntity made = null;
			Identifier id = Identifier.tryParse(type);
			EntityType<?> kind = id == null ? null : BuiltInRegistries.ENTITY_TYPE.getOptional(id).orElse(null);
			try {
				if (kind != null && kind.create(world, EntitySpawnReason.LOAD) instanceof LivingEntity creature) {
					creature.setId(nextId--);
					if (creature instanceof Mob mob) {
						mob.setNoAi(true);
					}
					creature.setYRot(0.0F);
					creature.yBodyRot = creature.yBodyRotO = creature.yHeadRot = creature.yHeadRotO = 0.0F;
					made = creature;
				}
			} catch (RuntimeException error) {
				Jugcraft.LOGGER.warn("Could not make a roster portrait of {}", type, error);
			}
			if (MADE.size() >= MAX) {
				MADE.remove(MADE.keySet().iterator().next());
			}
			MADE.put(type, Optional.ofNullable(made));
			return made;
		}

		static void clear() {
			MADE.clear();
			level = null;
		}
	}
}
