package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.jimbozoomer.jugcraft.concordance.compose.Composition;
import io.github.jimbozoomer.jugcraft.concordance.compose.Draft;
import io.github.jimbozoomer.jugcraft.concordance.compose.Grammar;
import io.github.jimbozoomer.jugcraft.concordance.compose.Slot;
import io.github.jimbozoomer.jugcraft.concordance.composer.ComposerResultPayload;
import io.github.jimbozoomer.jugcraft.concordance.composer.ComposerStatePayload;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BooleanSupplier;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.FocusableTextWidget;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

/**
 * The plain Spell Composer (Chunk A, A-03): vanilla widgets only, so it works without GuiLib, with every control on the
 * keyboard and read out by the narrator. From the top: the instrument in hand and its limits; the spell as text (type
 * it, or let the buttons write it); the spell's words in order (a branch on its own row), any one of which can be
 * chosen to move, remove or join a modifier to; the edits; every word this player has learned, by slot (modifiers
 * enabled only for a chosen word they can change); and what the server's check says. Inscribe (only once the spell as
 * it stands was checked and works) writes it on the instrument; Wipe clears the instrument. Keyboard: Tab and the arrow
 * keys move between controls, a word reached with them is chosen, Alt+Left and Alt+Right move it, Backspace removes it,
 * Ctrl+Z undoes. Nothing here relies on colour: a word a problem names is marked "!", the chosen word is in brackets,
 * and every line says what it means. It only edits a draft and shows what the server sent.
 */
public class ComposerScreen extends Screen {
	private static final int MAX_WIDTH = 400;
	private static final int TOP = 22;
	private static final int ROW = 20;
	private static final int GAP = 4;
	private static final int SCROLLBAR = 10;
	private static final int PANEL = 0xF0141419;
	private static final int EDGE = 0xFF6F5BA8;
	private static final int TITLE = 0xFFFFD98A;
	private static final Slot[] PALETTE = {Slot.DELIVERY, Slot.SELECTION, Slot.OPERATION, Slot.MODIFIER, Slot.TERMINATION};

	private final @Nullable Screen parent;
	private final Runnable listener = () -> dirty = true;
	/** A stable name for each control, so focus comes back to the same one after the screen is laid out again. */
	private final Map<GuiEventListener, String> keys = new IdentityHashMap<>();
	private @Nullable EditBox text;
	private @Nullable ComposerScroll body;
	private @Nullable Button inscribe;
	private @Nullable Button wipe;
	private int left;
	private int contentWidth;
	private double scroll;
	/** The control to give the keyboard after laying out: the spell's text box when the screen opens. */
	private String focus = "text";
	private boolean dirty;
	private boolean syncing;
	/** Set while the keyboard is given back after laying out, which must not choose a word. */
	private boolean restoring;

	public ComposerScreen(@Nullable Screen parent) {
		super(Component.translatable("screen.jugcraft.composer.title"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		ComposerClient.unlisten(listener);
		ComposerClient.listen(listener);
		keys.clear();
		contentWidth = Math.max(80, Math.min(MAX_WIDTH, width - 24 - SCROLLBAR));
		left = (width - contentWidth - SCROLLBAR) / 2;
		int footerY = height - 26;
		int third = (contentWidth + SCROLLBAR - 8) / 3;
		// The footer comes first, so its buttons take a click before anything scrolled beneath them.
		inscribe = addRenderableWidget(keyed("inscribe", Button.builder(Component.translatable("screen.jugcraft.composer.inscribe"),
				button -> ComposerClient.inscribe()).bounds(left, footerY, third, ROW).build()));
		wipe = addRenderableWidget(keyed("wipe", Button.builder(Component.translatable("screen.jugcraft.composer.wipe"),
				button -> ComposerClient.wipe()).bounds(left + third + 4, footerY, third, ROW).build()));
		addRenderableWidget(keyed("done", Button.builder(Component.translatable(parent == null ? "gui.done" : "gui.back"),
				button -> onClose()).bounds(left + 2 * (third + 4), footerY, third, ROW).build()));
		inscribe.active = ComposerClient.inscribable();
		wipe.active = ComposerClient.wipeable();
		body = addRenderableWidget(new ComposerScroll(minecraft, left, TOP, contentWidth + SCROLLBAR, Math.max(ROW, footerY - 4 - TOP),
				this::focused));
		int y = status(0);
		y = textBox(y + GAP);
		y = strip(y + GAP);
		y = edits(y + GAP);
		y = palette(y + GAP);
		y = report(y + GAP);
		line(y + GAP, Component.translatable("screen.jugcraft.composer.help"), "help");
		body.arrange(scroll);
		Draft draft = ComposerClient.draft();
		if (focus.startsWith("part:") && draft.chosen() != null) {
			// On the spell's words, the keyboard follows the chosen word wherever an edit (move, remove, undo) put it.
			focus = "part:" + draft.level() + ":" + draft.index();
		}
		restoring = true;
		try {
			for (Map.Entry<GuiEventListener, String> control : keys.entrySet()) {
				if (control.getValue().equals(focus)) {
					refocus(control.getKey());
					break;
				}
			}
		} finally {
			restoring = false;
		}
	}

	/**
	 * Opening and every new layout call this after {@link #init()}, which has already given the keyboard back to the
	 * control that had it (the text box when the screen opens). Vanilla's default for keyboard players moves on one
	 * control from there, which after each layout would walk the keyboard away (and across the spell's words).
	 */
	@Override
	protected void setInitialFocus() {
		if (getFocused() == null) {
			super.setInitialFocus();
		}
	}

	private <T extends GuiEventListener> T keyed(String key, T control) {
		keys.put(control, key);
		return control;
	}

	/** The instrument and its limits, or why there is nothing to compose for. */
	private int status(int y) {
		ComposerStatePayload state = ComposerClient.state();
		if (!ComposerClient.available()) {
			return line(y, Component.translatable("screen.jugcraft.composer.unavailable"), "status");
		}
		if (state == null) {
			return line(y, Component.translatable("screen.jugcraft.composer.opening"), "status");
		}
		if (state.status() == ComposerStatePayload.DISABLED) {
			return line(y, Component.translatable("message.jugcraft.concordance.disabled"), "status");
		}
		if (state.status() != ComposerStatePayload.READY || state.instrument().isEmpty()) {
			return line(y, Component.translatable("compose.jugcraft.problem.no_instrument"), "status");
		}
		ComposerStatePayload.InstrumentView instrument = state.instrument().get();
		Component held = minecraft.player == null ? Component.empty() : minecraft.player.getMainHandItem().getHoverName();
		y = line(y, Component.translatable("screen.jugcraft.composer.instrument", held, ComposerClient.capacity(), instrument.capacity(),
				instrument.targets(), instrument.work(), instrument.branches()), "status");
		return line(y, state.inscribed().isEmpty() ? Component.translatable("compose.jugcraft.nothing_inscribed")
				: Component.translatable("screen.jugcraft.composer.inscribed", state.inscribed()), "inscribed");
	}

	/** The spell as text: the box shows the draft, and typing in it rewrites the draft. */
	private int textBox(int y) {
		if (text == null) {
			text = new EditBox(font, left, 0, contentWidth, ROW, Component.translatable("screen.jugcraft.composer.text"));
			text.setMaxLength(Grammar.MAX_TEXT);
			text.setHint(Component.translatable("screen.jugcraft.composer.hint"));
			text.setResponder(this::typed);
		}
		text.setX(left);
		text.setWidth(contentWidth);
		// Show the draft, unless the box already reads as the same spell (so typing keeps its place).
		Draft shown = new Draft();
		shown.read(text.getValue(), false);
		if (!shown.text().equals(ComposerClient.draft().text())) {
			syncing = true;
			text.setValue(ComposerClient.draft().text());
			syncing = false;
		}
		body.add(keyed("text", text), y);
		return y + ROW;
	}

	private void typed(String value) {
		if (!syncing) {
			ComposerClient.draft().read(value, true);
			ComposerClient.edited();
		}
	}

	/** The spell's words in order, one row per level; any can be chosen. */
	private int strip(int y) {
		Draft draft = ComposerClient.draft();
		if (draft.typed()) {
			return line(y, Component.translatable("screen.jugcraft.composer.typed"), "strip");
		}
		List<List<Draft.Part>> levels = draft.levels();
		if (levels.size() == 1 && levels.getFirst().isEmpty()) {
			return line(y, Component.translatable("screen.jugcraft.composer.empty"), "strip");
		}
		Set<String> blamed = blamed();
		for (int i = 0; i < levels.size(); i++) {
			List<AbstractWidget> chips = new ArrayList<>();
			for (int j = 0; j < levels.get(i).size(); j++) {
				Draft.Part part = levels.get(i).get(j);
				int level = i;
				int index = j;
				boolean chosen = draft.index() == j && draft.level() == i;
				boolean wrong = blamed.contains(Composition.shortName(part.id()))
						|| part.modifiers().stream().anyMatch(modifier -> blamed.contains(Composition.shortName(modifier)));
				Component label = Component.literal((wrong ? "! " : "") + (chosen ? "[" + part.written() + "]" : part.written()));
				Button chip = Button.builder(label, button -> choose(level, index)).width(fit(label)).build();
				chip.setTooltip(Tooltip.create(describe(part)));
				chips.add(keyed("part:" + i + ":" + j, chip));
			}
			y = flow(y, Component.translatable(i == 0 ? "screen.jugcraft.composer.spell" : "screen.jugcraft.composer.then"), chips) + 2;
		}
		return y;
	}

	/** The written names the last check's problems blame. */
	private static Set<String> blamed() {
		ComposerResultPayload result = ComposerClient.result();
		return result == null ? Set.of() : new HashSet<>(result.report().blamed());
	}

	private void choose(int level, int index) {
		Draft draft = ComposerClient.draft();
		if (draft.level() == level && draft.index() == index) {
			draft.choose(-1, -1);
		} else {
			draft.choose(level, index);
		}
		ComposerClient.changed();
	}

	/** The edits, each enabled only when it can do something. */
	private int edits(int y) {
		Draft draft = ComposerClient.draft();
		Draft.Part chosen = draft.chosen();
		ComposerStatePayload state = ComposerClient.state();
		int branches = state == null || state.instrument().isEmpty() ? 0 : state.instrument().get().branches();
		int size = draft.levels().get(draft.level()).size();
		List<AbstractWidget> buttons = new ArrayList<>();
		buttons.add(edit("screen.jugcraft.composer.earlier", chosen != null && draft.index() > 0, () -> draft.move(-1)));
		buttons.add(edit("screen.jugcraft.composer.later", chosen != null && draft.index() < size - 1, () -> draft.move(1)));
		buttons.add(edit("screen.jugcraft.composer.remove", chosen != null, draft::remove));
		buttons.add(edit("screen.jugcraft.composer.unjoin", chosen != null && !chosen.modifiers().isEmpty(), draft::unjoin));
		buttons.add(edit("screen.jugcraft.composer.branch", draft.canBranch() && draft.levels().size() - 1 < branches, draft::branch));
		buttons.add(edit("screen.jugcraft.composer.unbranch", draft.levels().size() > 1, draft::unbranch));
		buttons.add(edit("screen.jugcraft.composer.undo", draft.canUndo(), draft::undo));
		buttons.add(edit("screen.jugcraft.composer.restart", !draft.isEmpty() || draft.levels().size() > 1, draft::clear));
		buttons.add(edit("screen.jugcraft.composer.load", state != null && !state.inscribed().isEmpty(), () -> {
			ComposerClient.load();
			return false;
		}));
		return flow(y, null, buttons);
	}

	private Button edit(String key, boolean active, BooleanSupplier change) {
		Component label = Component.translatable(key);
		Button button = Button.builder(label, unused -> {
			if (change.getAsBoolean()) {
				ComposerClient.edited();
			}
		}).width(fit(label)).build();
		button.active = active;
		return keyed(key, button);
	}

	/** Every word the player has learned, by slot; a modifier only for a chosen word it can change. */
	private int palette(int y) {
		ComposerStatePayload state = ComposerClient.state();
		if (state == null || state.status() != ComposerStatePayload.READY) {
			return y;
		}
		Draft.Part chosen = ComposerClient.draft().chosen();
		ComposerStatePayload.Word chosenWord = chosen == null ? null : ComposerClient.word(chosen.id());
		int changes = chosenWord == null ? 0 : chosenWord.aspects();
		for (Slot slot : PALETTE) {
			List<AbstractWidget> words = new ArrayList<>();
			for (ComposerStatePayload.Word word : state.words()) {
				if (ComposerClient.slot(word) != slot) {
					continue;
				}
				Component label = Component.literal(Composition.shortName(word.id()));
				Button button = Button.builder(label, unused -> add(word)).width(fit(label)).build();
				button.setTooltip(Tooltip.create(describe(word.id())));
				if (slot == Slot.MODIFIER) {
					button.active = chosen != null && (changes & word.aspects()) != 0 && !chosen.modifiers().contains(word.id())
							&& chosen.modifiers().size() < Grammar.MAX_MODIFIERS;
				}
				words.add(keyed("word:" + word.id(), button));
			}
			if (!words.isEmpty()) {
				y = flow(y, Component.translatable("screen.jugcraft.composer.slot." + slot.id()), words) + 2;
			}
		}
		if (state.locked() > 0) {
			y = line(y, Component.translatable("screen.jugcraft.composer.locked", state.locked()), "locked");
		}
		return y;
	}

	private void add(ComposerStatePayload.Word word) {
		Slot slot = ComposerClient.slot(word);
		if (slot != null && ComposerClient.draft().add(word.id(), slot, ComposerClient::slotOf)) {
			ComposerClient.edited();
		}
	}

	/** What the server's last check said (and whether a newer one is on its way). */
	private int report(int y) {
		if (ComposerClient.draft().isEmpty()) {
			return y;
		}
		if (ComposerClient.writing()) {
			y = line(y, Component.translatable("screen.jugcraft.composer.writing"), "busy");
		} else if (ComposerClient.checking()) {
			y = line(y, Component.translatable("screen.jugcraft.composer.checking"), "busy");
		}
		ComposerResultPayload result = ComposerClient.result();
		if (result == null) {
			return y;
		}
		y = line(y, result.heading(), "heading");
		List<Component> lines = result.report().lines();
		for (int i = 0; i < lines.size(); i++) {
			y = line(y, Component.literal("- ").append(lines.get(i)), "line:" + i);
		}
		return y;
	}

	/** A line of text the keyboard can reach and the narrator reads. */
	private int line(int y, Component line, String key) {
		FocusableTextWidget widget = FocusableTextWidget.builder(line, font, 2).maxWidth(contentWidth).alwaysShowBorder(false)
				.backgroundFill(FocusableTextWidget.BackgroundFill.NEVER).build();
		widget.setX(left);
		body.add(keyed(key, widget), y);
		return y + widget.getHeight();
	}

	/** Lays {@code widgets} out in rows after {@code label}, wrapping at the content width; returns the y below them. */
	private int flow(int y, @Nullable Component label, List<AbstractWidget> widgets) {
		int start = left;
		if (label != null) {
			StringWidget title = new StringWidget(label, font);
			title.setX(left);
			body.add(title, y + (ROW - font.lineHeight) / 2);
			start = Math.min(left + font.width(label) + GAP, left + contentWidth / 3);
		}
		int x = start;
		int row = y;
		for (AbstractWidget widget : widgets) {
			if (x > start && x + widget.getWidth() > left + contentWidth) {
				x = start;
				row += ROW + 2;
			}
			widget.setX(x);
			body.add(widget, row);
			x += widget.getWidth() + 2;
		}
		return row + ROW;
	}

	private int fit(Component label) {
		return Math.min(contentWidth, Math.max(24, font.width(label) + 10));
	}

	/** A part's words: each one's name, what it does and what it costs. */
	private static Component describe(Draft.Part part) {
		MutableComponent out = describe(part.id()).copy();
		for (String modifier : part.modifiers()) {
			out.append("\n").append(describe(modifier));
		}
		return out;
	}

	private static MutableComponent describe(String id) {
		Identifier parsed = Identifier.tryParse(id);
		String key = parsed == null ? "component.jugcraft." + id : "component." + parsed.getNamespace() + "." + parsed.getPath().replace('/', '.');
		MutableComponent out = Component.translatableWithFallback(key, Composition.shortName(id));
		out.append(": ").append(Component.translatableWithFallback(key + ".description", ""));
		ComposerStatePayload.Word word = ComposerClient.word(id);
		if (word != null) {
			out.append(" ").append(Component.translatable("screen.jugcraft.composer.word_cost", word.capacity(), word.focus()));
		}
		return out;
	}

	/** Keeps the composer's place: the control the keyboard reached, and choosing a word reached with it. */
	private void focused(@Nullable GuiEventListener control) {
		String key = control == null ? null : keys.get(control);
		if (key == null) {
			return;
		}
		focus = key;
		if (!restoring && key.startsWith("part:") && minecraft.getLastInputType().isKeyboard()) {
			String[] at = key.split(":");
			int level = Integer.parseInt(at[1]);
			int index = Integer.parseInt(at[2]);
			Draft draft = ComposerClient.draft();
			if (draft.level() != level || draft.index() != index) {
				draft.choose(level, index);
				ComposerClient.changed();
			}
		}
	}

	private void refocus(GuiEventListener control) {
		if (body != null && body.children().contains(control)) {
			body.setFocused(control);
			setFocused(body);
		} else {
			setFocused(control);
		}
	}

	@Override
	public void setFocused(@Nullable GuiEventListener focused) {
		super.setFocused(focused);
		String key = focused == null ? null : keys.get(focused);
		if (key != null) {
			focus = key;
		}
	}

	@Override
	public void tick() {
		super.tick();
		if (dirty) {
			// Laid out again once a tick at most, after whatever changed (an edit, the server's state or answer).
			dirty = false;
			scroll = body == null ? 0 : body.scrollAmount();
			rebuildWidgets();
		} else if (inscribe != null && wipe != null) {
			inscribe.active = ComposerClient.inscribable();
			wipe.active = ComposerClient.wipeable();
		}
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		Draft draft = ComposerClient.draft();
		boolean inText = text != null && text.isFocused();
		// Ctrl+Z (Cmd+Z on macOS), matched like vanilla's edit shortcuts: by the letter on the keyboard layout.
		if (event.shortcutKey() == InputConstants.KEYCODE_Z && event.hasControlDownWithQuirk() && !event.hasShiftDown() && !event.hasAltDown()) {
			if (draft.undo()) {
				ComposerClient.edited();
			}
			return true;
		}
		if (!inText && event.hasAltDown() && (event.input() == InputConstants.KEY_LEFT || event.input() == InputConstants.KEY_RIGHT)) {
			if (draft.move(event.input() == InputConstants.KEY_LEFT ? -1 : 1)) {
				ComposerClient.edited();
			}
			return true;
		}
		if (!inText && event.input() == InputConstants.KEY_BACKSPACE && draft.chosen() != null) {
			if (draft.remove()) {
				ComposerClient.edited();
			}
			return true;
		}
		return super.keyPressed(event);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public void onClose() {
		minecraft.gui.setScreen(parent);
	}

	@Override
	public void removed() {
		ComposerClient.unlisten(listener);
		super.removed();
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		super.extractBackground(graphics, mouseX, mouseY, delta);
		int right = left + contentWidth + SCROLLBAR;
		graphics.fill(left - 5, 3, right + 5, height - 3, EDGE);
		graphics.fill(left - 4, 4, right + 4, height - 4, PANEL);
		graphics.text(font, title, left + (contentWidth + SCROLLBAR - font.width(title)) / 2, 8, TITLE, false);
	}
}
