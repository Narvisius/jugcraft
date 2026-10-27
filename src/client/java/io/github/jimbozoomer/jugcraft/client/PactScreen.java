package io.github.jimbozoomer.jugcraft.client;

import io.github.jimbozoomer.jugcraft.concordance.spirits.PactMenu;
import io.github.jimbozoomer.jugcraft.concordance.spirits.PactRequestPayload;
import io.github.jimbozoomer.jugcraft.concordance.spirits.PactStatePayload;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Util;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * Concordance Chunk D (D-04, D-05; D-S14, D-S15): the pact perch's screen, native and narrated.
 * <p>
 * While Stolas visits, it gives his three beats, a page each, then his terms: every term D-S15 lists, the Open Hand
 * Compact's materials drawn, and what stands in the way, as the server says it. Accept and Decline sit under them; Accept
 * is offered only when nothing stands in the way. Nothing is spent or agreed until Accept, and the keyboard lands on the
 * terms' first line, never on Accept.
 * <p>
 * For a kept pact it shows how the pact stands, then Star-reckoning (a pattern the holder observed, then Recall), the
 * offerings the holder carries (each paid into the pact's keeping), Suspend or Resume, and End, which asks first. A pact
 * without its perch can be seated here or ended.
 * <p>
 * The server's answer, a refusal with its reason included, is shown and read aloud. Each answer carries its mark (a tick
 * or a cross), and Stolas's subtitles draw over the screen (D-06); ending asks first, the keyboard on Keep it
 * (KeepFirstConfirmScreen). Every word is a focusable, narrated
 * paragraph and every control a button: Tab and Shift+Tab move, Enter or Space presses, Esc leaves. The screen fits any
 * window down to 320x180 scaled pixels and scrolls its body. Nothing in it decides: each button sends one bounded request
 * through {@link PactClient}. No text box, chat command, vanilla Dialog or GuiLib.
 */
public class PactScreen extends Screen implements MenuAccess<PactMenu> {
	/** The page his terms are on, after his three beats. */
	public static final int TERMS_PAGE = PactStatePayload.MAX_BEATS;
	private static final int GOLD = 0xFFE6C586;
	private static final int TEXT = 0xFFE2E7F1;
	private static final int DIM = 0xFF9CAAC3;
	private static final int GOOD = 0xFFA6D6C3;
	private static final int WARN = 0xFFE8B07A;
	private static final int MAX_WIDTH = 420;
	private static final int MAX_HEIGHT = 260;
	private static final int TOP = 26;
	private static final int FOOT = 20;
	/** The sign by his name, the Styx flower named for him (unit 3 gives him a portrait). */
	private static final String SIGN = "jugcraft:stolas_starflower";
	private final PactMenu menu;
	private final Runnable listener = () -> dirty = true;
	private final Map<String, AbstractWidget> controls = new LinkedHashMap<>();
	private final Set<String> footer = new LinkedHashSet<>();
	private @Nullable ComposerScroll body;
	private boolean dirty;
	private boolean termsTab;
	private boolean asked;
	private int x;
	private int y;
	private int w;
	private int h;
	private int at;
	private int page;
	private int mode = -1;
	private int pattern = -1;
	private String focus = "";

	public PactScreen(PactMenu menu, Inventory inventory, Component title) {
		super(title);
		this.menu = menu;
	}

	@Override
	public PactMenu getMenu() {
		return menu;
	}

	static Component ui(String key, Object... args) {
		return Component.translatable("screen.jugcraft.pact." + key, args);
	}

	@Override
	protected void init() {
		PactClient.unlisten(listener);
		PactClient.listen(listener);
		double scroll = body == null ? 0 : body.scrollAmount();
		controls.clear();
		footer.clear();
		at = 0;
		w = Math.min(MAX_WIDTH, width - 16);
		h = Math.min(MAX_HEIGHT, height - 12);
		x = (width - w) / 2;
		y = (height - h) / 2;
		PactStatePayload state = PactClient.state(menu);
		if (state != null && state.mode() != mode) {
			// Sealed, ended or seated: the new state of things starts at its first page, its service before its terms.
			mode = state.mode();
			page = 0;
			termsTab = false;
			asked = false;
			pattern = -1;
			scroll = 0;
			focus = mode == PactStatePayload.NEGOTIATE ? "beat" : state.result().isPresent() ? "result" : "note:0";
		}
		if (state != null && PactClient.takeChanged()) {
			// His terms changed under the screen: they are shown again, from their first line, before anything is accepted.
			page = state.beats().size();
			termsTab = true;
			scroll = 0;
			focus = "para:0";
		}
		body = addRenderableWidget(new ComposerScroll(minecraft, x + 10, y + TOP, w - 20, Math.max(24, h - TOP - FOOT - 10), this::focused));
		if (state == null) {
			say(ui("opening"), DIM, "");
			footer(button("close", ui("close"), Component.empty(), true, this::onClose, false));
		} else {
			switch (state.mode()) {
				case PactStatePayload.NEGOTIATE -> negotiate(state);
				case PactStatePayload.KEEP -> keep(state);
				case PactStatePayload.SEAT -> seat(state);
				default -> closed(state);
			}
		}
		body.arrange(scroll);
		restore();
	}

	private void negotiate(PactStatePayload state) {
		int beats = state.beats().size();
		page = Math.min(page, beats);
		if (page < beats) {
			say(state.beats().get(page), TEXT, "beat");
			footer(button("back", ui("back"), Component.empty(), page > 0, () -> turn(page - 1), false),
					button("next", page + 1 < beats ? ui("next") : ui("hear_terms"), Component.empty(), true, () -> turn(page + 1), false),
					button("leave", ui("leave"), ui("leave.hint"), true, this::onClose, false));
			return;
		}
		terms(state);
		notes(state, ui("in_the_way"), WARN);
		result(state);
		boolean free = !PactClient.waiting();
		footer(button("back", ui("back"), Component.empty(), beats > 0, () -> turn(beats - 1), false),
				button("accept", ui("accept"), ui("accept.hint"), free && state.offers(PactRequestPayload.ACCEPT),
						() -> PactClient.request(menu, PactRequestPayload.ACCEPT, 0), false),
				button("decline", ui("decline"), ui("decline.hint"), free && state.offers(PactRequestPayload.DECLINE),
						() -> PactClient.request(menu, PactRequestPayload.DECLINE, 0), false));
	}

	private void keep(PactStatePayload state) {
		notes(state, ui("standing"), TEXT);
		result(state);
		if (termsTab) {
			terms(state);
		} else {
			service(state);
			offerings(state);
			boolean free = !PactClient.waiting();
			if (state.offers(PactRequestPayload.RESUME)) {
				inline("resume", ui("resume"), ui("resume.hint"), free, () -> PactClient.request(menu, PactRequestPayload.RESUME, 0), false);
			} else if (state.offers(PactRequestPayload.SUSPEND)) {
				inline("suspend", ui("suspend"), ui("suspend.hint"), free, () -> PactClient.request(menu, PactRequestPayload.SUSPEND, 0), false);
			}
			inline("end", ui("end"), ui("end.hint"), free && state.offers(PactRequestPayload.END), this::confirmEnd, false);
		}
		footer(button("tab:service", ui("tab.service"), Component.empty(), true, () -> showTerms(false), !termsTab),
				button("tab:terms", ui("tab.terms"), Component.empty(), true, () -> showTerms(true), termsTab),
				button("close", ui("close"), Component.empty(), true, this::onClose, false));
	}

	private void seat(PactStatePayload state) {
		notes(state, ui("without_perch"), WARN);
		result(state);
		terms(state);
		boolean free = !PactClient.waiting();
		footer(button("seat", ui("seat"), ui("seat.hint"), free && state.offers(PactRequestPayload.SEAT),
						() -> PactClient.request(menu, PactRequestPayload.SEAT, 0), false),
				button("end", ui("end"), ui("end.hint"), free && state.offers(PactRequestPayload.END), this::confirmEnd, false),
				button("close", ui("close"), Component.empty(), true, this::onClose, false));
	}

	private void closed(PactStatePayload state) {
		notes(state, ui("closed"), WARN);
		result(state);
		footer(button("close", ui("close"), Component.empty(), true, this::onClose, false));
	}

	/** Star-reckoning (D-S1): a pattern the holder observed, chosen here, then Recall with its cost named. */
	private void service(PactStatePayload state) {
		say(ui("recall_heading"), GOLD, "");
		List<PactStatePayload.Choice> patterns = state.patterns();
		if (patterns.isEmpty()) {
			say(ui("no_patterns"), DIM, "");
			return;
		}
		pattern = Math.min(pattern, patterns.size() - 1);
		for (int i = 0; i < patterns.size(); i++) {
			PactStatePayload.Choice choice = patterns.get(i);
			int index = i;
			Component hint = !choice.ready() ? ui("pattern.claimed") : i == pattern ? ui("pattern.chosen", choice.amount()) : ui("pattern.cost", choice.amount());
			inline("pattern:" + i, choice.name(), hint, true, () -> {
				pattern = index;
				dirty = true;
			}, i == pattern);
		}
		PactStatePayload.Choice chosen = pattern >= 0 ? patterns.get(pattern) : null;
		inline("recall", chosen == null ? ui("recall.none") : ui("recall", chosen.name()),
				chosen == null ? Component.empty() : ui("pattern.cost", chosen.amount()),
				chosen != null && chosen.ready() && !PactClient.waiting() && state.offers(PactRequestPayload.RECALL),
				() -> PactClient.request(menu, PactRequestPayload.RECALL, pattern), false);
	}

	/** Upkeep (D-S3): each offering the holder carries, paid into the pact's keeping (a server action, never a slot: D-S6). */
	private void offerings(PactStatePayload state) {
		say(ui("offerings_heading"), GOLD, "");
		if (state.offerings().isEmpty()) {
			say(ui("no_offerings"), DIM, "");
			return;
		}
		for (int i = 0; i < state.offerings().size(); i++) {
			PactStatePayload.Choice choice = state.offerings().get(i);
			int index = i;
			inline("offer:" + i, ui("offer", choice.name(), choice.amount()), ui("offer.hint"),
					choice.ready() && !PactClient.waiting() && state.offers(PactRequestPayload.DEPOSIT),
					() -> PactClient.request(menu, PactRequestPayload.DEPOSIT, index), false);
		}
	}

	private void terms(PactStatePayload state) {
		say(ui("terms"), GOLD, "para:0");
		for (PactStatePayload.Term term : state.terms()) {
			Component label = Component.translatableWithFallback("screen.jugcraft.pact.term." + term.key(), term.key());
			say(ui("term_row", label, term.value()), TEXT, "term:" + term.key());
			if ("compact".equals(term.key()) && !state.compact().isEmpty()) {
				Stacks stacks = new Stacks(body.getX() + 3, font, state.compact(), body.getWidth() - 8);
				body.add(stacks, at);
				at += stacks.getHeight() + 3;
			}
		}
		if (state.mode() == PactStatePayload.NEGOTIATE) {
			say(ui("terms_note"), DIM, "");
		}
	}

	private void notes(PactStatePayload state, Component heading, int colour) {
		if (state.notes().isEmpty()) {
			return;
		}
		say(heading, colour, "");
		for (int i = 0; i < state.notes().size(); i++) {
			say(state.notes().get(i), colour, "note:" + i);
		}
	}

	private void result(PactStatePayload state) {
		if (PactClient.waiting()) {
			say(ui("waiting"), DIM, "result");
		} else {
			state.result().ifPresent(result -> verdict(result.message(), result.done()));
		}
	}

	/** D-06: the answer, marked done or refused (Verdict), in its colour, under the key "result" (as say would put it). */
	private void verdict(Component text, boolean done) {
		int colour = done ? GOOD : WARN;
		Verdict line = new Verdict(text.copy().withColor(colour & 0xFFFFFF), done, font, body.getWidth() - 8, colour);
		line.setX(body.getX() + 3);
		body.add(line, at);
		at += line.getHeight() + 3;
		controls.put("result", line);
	}

	private void turn(int to) {
		PactStatePayload state = PactClient.state(menu);
		if (state == null) {
			return;
		}
		page = Math.max(0, Math.min(to, state.beats().size()));
		if (page == state.beats().size()) {
			if (!asked) {
				// The holder asks to hear his terms, once (D-S9): nothing is stored or spent.
				asked = true;
				PactClient.request(menu, PactRequestPayload.CHOOSE, 0);
			}
			focus = "para:0";
		} else {
			minecraft.getNarrator().saySystemNow(state.beats().get(page));
		}
		dirty = true;
	}

	private void showTerms(boolean terms) {
		termsTab = terms;
		dirty = true;
	}

	/**
	 * Ending asks first, through vanilla's confirmation with the keyboard on Keep it (KeepFirstConfirmScreen, D-06); only its
	 * yes sends END, and Esc keeps the pact.
	 */
	private void confirmEnd() {
		PactStatePayload state = PactClient.state(menu);
		Component speaker = state == null ? title : state.speaker();
		minecraft.gui.setScreen(new KeepFirstConfirmScreen(yes -> {
			minecraft.gui.setScreen(this);
			if (yes) {
				PactClient.request(menu, PactRequestPayload.END, 0);
			}
		}, ui("end.title", speaker), ui("end.confirm", speaker), ui("end.yes"), ui("end.no")));
	}

	private void say(Component text, int colour, String key) {
		BookArt.Paragraph line = new BookArt.Paragraph(text.copy().withColor(colour & 0xFFFFFF), font, body.getWidth() - 8, colour);
		line.setX(body.getX() + 3);
		body.add(line, at);
		at += line.getHeight() + 3;
		if (!key.isEmpty()) {
			controls.put(key, line);
		}
	}

	private void inline(String key, Component label, Component hint, boolean active, Runnable action, boolean selected) {
		BookArt.Tab tab = new BookArt.Tab(body.getX() + 3, 0, body.getWidth() - 8, 20, label, hint, "", selected, true, unused -> action.run());
		tab.active = active;
		body.add(tab, at);
		at += tab.getHeight() + 3;
		controls.put(key, tab);
	}

	private Button button(String key, Component label, Component hint, boolean active, Runnable action, boolean selected) {
		BookArt.Tab tab = new BookArt.Tab(0, 0, 40, FOOT, label, hint, "", selected, true, unused -> action.run());
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

	private void restore() {
		AbstractWidget widget = controls.get(focus);
		if (widget == null || !widget.active || body == null) {
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
		// While an answer is awaited the pressed button is disabled: the keyboard waits with it, not at the top.
		if (getFocused() == null && !PactClient.waiting()) {
			super.setInitialFocus();
		}
	}

	@Override
	public void tick() {
		super.tick();
		if (dirty) {
			dirty = false;
			rebuildWidgets();
		}
	}

	private Component heading(PactStatePayload state) {
		return switch (state.mode()) {
			case PactStatePayload.NEGOTIATE -> page < state.beats().size() ? ui("page", page + 1, state.beats().size()) : ui("terms");
			case PactStatePayload.KEEP -> termsTab ? ui("terms") : ui("kept");
			case PactStatePayload.SEAT -> ui("without_perch");
			default -> Component.empty();
		};
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
		// No animated blur; reduced motion holds the stars still (as the composer's, ComposerScreen.java:270-276).
		g.fill(0, 0, width, height, 0xCF050913);
		g.fill(x, y, x + w, y + h, 0xFF0C1424);
		g.outline(x, y, w, h, 0xFF74613F);
		g.outline(x + 3, y + 3, w - 6, h - 6, 0xFF243148);
		double time = ConcordanceClientOptions.reducedMotion() ? 0 : Util.getMillis() / 1600.0;
		for (int i = 0; i < 36; i++) {
			int sx = x + 6 + Math.floorMod(i * 97 + i * i * 13, Math.max(1, w - 12));
			int sy = y + 6 + Math.floorMod(i * 59 + 17, Math.max(1, h - 12));
			int alpha = (int) (35 + 25 * (1 + Math.sin(time + i * 2.3)));
			g.fill(sx, sy, sx + 1, sy + 1, (alpha << 24) | 0x9ABBD6);
		}
		PactStatePayload state = PactClient.state(menu);
		Component heading = state == null ? Component.empty() : heading(state);
		int right = font.width(heading);
		BookArt.icon(g, SIGN, x + 8, y + 5);
		g.text(font, BookArt.fit(font, (state == null ? title : state.speaker()).getString(), w - 44 - right), x + 28, y + 10, GOLD, false);
		g.text(font, heading, x + w - 10 - right, y + 10, DIM, false);
		g.fill(x + 8, y + TOP - 3, x + w - 8, y + TOP - 2, 0xFF344158);
		// D-06 (03:159): his sounds' subtitles over this screen (deferred to here, as vanilla's Screen.extractBackground does).
		minecraft.gui.hud.extractDeferredSubtitles();
	}

	@Override
	public Component getNarrationMessage() {
		PactStatePayload state = PactClient.state(menu);
		return state == null ? title : CommonComponents.joinForNarration(state.speaker(), heading(state));
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
	public boolean isInGameUi() {
		// D-06: an in-game screen, as a chest's: the HUD defers its subtitles to extractBackground (Hud.extractSubtitleOverlay).
		return true;
	}

	@Override
	public void onClose() {
		// Closing is leaving: a visit he holds no pact for ends there (D-S7); a kept pact is untouched.
		minecraft.player.closeContainer();
		super.onClose();
	}

	@Override
	public void removed() {
		PactClient.unlisten(listener);
		super.removed();
	}

	/** The page shown (0 to 2 his words, {@link #TERMS_PAGE} his terms), for tests. */
	public int page() {
		return page;
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

	/** What does not fit: the panel or body off screen, a footer button outside the panel or narrower than 24 (tests: none). */
	public List<String> outside() {
		List<String> out = new ArrayList<>();
		if (x < 0 || y < 0 || x + w > width || y + h > height) {
			out.add("panel");
		}
		if (body != null && (body.getY() + body.getHeight() > y + h - FOOT - 6 || body.getHeight() < 24)) {
			out.add("body");
		}
		for (String key : footer) {
			AbstractWidget widget = controls.get(key);
			if (widget.getX() < x || widget.getY() < y || widget.getX() + widget.getWidth() > x + w || widget.getY() + widget.getHeight() > y + h
					|| widget.getWidth() < 24) {
				out.add(key);
			}
		}
		return out;
	}

	/**
	 * Concordance Chunk D, D-06 (03:159): the answer to the last request with its mark, a tick when it was done, a cross when
	 * it was refused, drawn before its words, so the outcome never rests on colour alone nor on his gesture or its sound being
	 * seen or heard. Focusable and narrated ("Done: ..." or "Refused: ..."), as BookArt.Paragraph is. The roster uses it too.
	 */
	public static final class Verdict extends AbstractWidget {
		private static final int MARK = 9;
		private final Font font;
		private final List<FormattedCharSequence> lines;
		private final int colour;
		private final boolean refused;

		Verdict(Component text, boolean done, Font font, int width, int colour) {
			super(0, 0, Math.max(30, width), 1, text);
			this.font = font;
			this.colour = colour;
			this.refused = !done;
			lines = font.split(text, getWidth() - MARK - 6);
			setHeight(Math.max(MARK + 4, Math.max(1, lines.size()) * (font.lineHeight + 2) + 2));
		}

		/** Whether its mark is the refusal's cross (tests). */
		public boolean refused() {
			return refused;
		}

		@Override
		protected void extractWidgetRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
			int mx = getX() + 1;
			int my = getY() + 2;
			if (refused) {
				for (int i = 0; i < MARK; i++) {
					g.fill(mx + i, my + i, mx + i + 1, my + i + 1, colour);
					g.fill(mx + MARK - 1 - i, my + i, mx + MARK - i, my + i + 1, colour);
				}
			} else {
				for (int i = 0; i < 3; i++) {
					g.fill(mx + i, my + 4 + i, mx + i + 1, my + 6 + i, colour);
				}
				for (int i = 0; i < 6; i++) {
					g.fill(mx + 3 + i, my + 6 - i, mx + 4 + i, my + 8 - i, colour);
				}
			}
			int ty = getY() + 1;
			for (FormattedCharSequence line : lines) {
				g.text(font, line, getX() + MARK + 5, ty, colour, false);
				ty += font.lineHeight + 2;
			}
			if (isFocused()) {
				g.outline(getX(), getY(), width, height, colour);
			}
		}

		@Override
		protected void updateWidgetNarration(NarrationElementOutput output) {
			output.add(NarratedElementType.TITLE, ui(refused ? "result.refused" : "result.done", getMessage()));
		}
	}

	/** The compact's materials, as icons with their counts; its term's line says them in words, so this takes no focus. */
	private static final class Stacks extends AbstractWidget {
		private static final int SLOT = 18;
		private final Font font;
		private final List<ItemStack> stacks;

		Stacks(int x, Font font, List<ItemStack> stacks, int maxWidth) {
			super(x, 0, Math.min(stacks.size(), Math.max(1, maxWidth / SLOT)) * SLOT,
					((stacks.size() + Math.max(1, maxWidth / SLOT) - 1) / Math.max(1, maxWidth / SLOT)) * SLOT, CommonComponents.EMPTY);
			this.font = font;
			this.stacks = stacks;
			active = false;
		}

		@Override
		protected void extractWidgetRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
			int columns = Math.max(1, width / SLOT);
			for (int i = 0; i < stacks.size(); i++) {
				int sx = getX() + (i % columns) * SLOT;
				int sy = getY() + (i / columns) * SLOT;
				ItemStack stack = stacks.get(i);
				g.fill(sx, sy, sx + SLOT - 1, sy + SLOT - 1, 0xFF243148);
				g.item(stack, sx + 1, sy + 1);
				g.itemDecorations(font, stack, sx + 1, sy + 1);
				if (mouseX >= sx && mouseX < sx + SLOT - 1 && mouseY >= sy && mouseY < sy + SLOT - 1) {
					g.setTooltipForNextFrame(font, stack, mouseX, mouseY);
				}
			}
		}

		@Override
		protected void updateWidgetNarration(NarrationElementOutput output) {
		}
	}
}
