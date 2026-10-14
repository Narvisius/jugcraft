package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.jimbozoomer.jugcraft.guide.encyclopedia.EncyclopediaPayload;
import io.github.jimbozoomer.jugcraft.guide.encyclopedia.Navigator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.FocusableTextWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.jspecify.annotations.Nullable;

/**
 * The Jugcraft Encyclopedia's screen (Concordance Chunk A, A-04): vanilla widgets only, every control on the keyboard
 * and read out by the narrator. A tab for each page the server sent (only pages with something to show) and the Keys
 * page; the page's rows in a scrolling body: headings, the route's steps (each says in words where it stands and how
 * much it matters, beside a mark of its own shape and colour), the ways to a step, facts, crafting grids and item icons
 * with their tooltips, and links (the codex, the Spell Composer, the journal, another page). Exact figures show with X,
 * as in the journal.
 * <p>
 * Keys: Left and Right change page; Tab and Up and Down move between controls; Page Up and Page Down scroll; Backspace
 * goes back to the page a link left; R asks the server again; X shows exact figures; Esc or the encyclopedia's own key
 * closes it, back to the game or, when it was opened from the inventory, to a fresh inventory. It only shows what the
 * server (and the client's own bindings) say.
 */
public class EncyclopediaScreen extends Screen {
	private static final int MAX_WIDTH = 440;
	private static final int TOP = 22;
	private static final int ROW = 20;
	private static final int GAP = 4;
	private static final int INDENT = 14;
	private static final int SCROLLBAR = 10;
	private static final int PANEL = 0xF0141419;
	private static final int EDGE = 0xFF6F5BA8;
	private static final int TITLE = 0xFFFFD98A;

	/** Where Back and Esc lead: the game, or a fresh inventory (the encyclopedia was opened from the inventory's button). */
	public record Back(boolean inventory) {
		public static final Back GAME = new Back(false);
		public static final Back INVENTORY = new Back(true);
	}

	private final Back back;
	private final Runnable listener = () -> dirty = true;
	/** A stable name for each control, so focus comes back to the same one after the screen is laid out again. */
	private final Map<GuiEventListener, String> keys = new IdentityHashMap<>();
	private @Nullable ComposerScroll body;
	private @Nullable Button exact;
	private int left;
	private int contentWidth;
	private String laidOut = "";
	private double scroll;
	private String focus = "";
	private boolean dirty;

	public EncyclopediaScreen(Back back) {
		super(Component.translatable("screen.jugcraft.encyclopedia.title"));
		this.back = back;
	}

	/** The page being read (tests read it). */
	public String page() {
		EncyclopediaPayload.Page page = EncyclopediaClient.navigator().current();
		return page == null ? "" : page.id();
	}

	public Back back() {
		return back;
	}

	@Override
	protected void init() {
		EncyclopediaClient.unlisten(listener);
		EncyclopediaClient.listen(listener);
		keys.clear();
		contentWidth = Math.max(80, Math.min(MAX_WIDTH, width - 24 - SCROLLBAR));
		left = (width - contentWidth - SCROLLBAR) / 2;
		int footerY = height - 26;
		int third = (contentWidth + SCROLLBAR - 8) / 3;
		// The footer comes first, so its buttons take a click before anything scrolled beneath them.
		addRenderableWidget(keyed("refresh", Button.builder(Component.translatable("screen.jugcraft.encyclopedia.refresh"),
				button -> EncyclopediaClient.request()).bounds(left, footerY, third, ROW).build()));
		exact = addRenderableWidget(keyed("exact", Button.builder(exactLabel(), button -> toggleExact())
				.bounds(left + third + 4, footerY, third, ROW).build()));
		addRenderableWidget(keyed("done", Button.builder(Component.translatable(back.inventory() ? "gui.back" : "gui.done"), button -> onClose())
				.bounds(left + 2 * (third + 4), footerY, third, ROW).build()));
		Navigator navigator = EncyclopediaClient.navigator();
		boolean waiting = EncyclopediaClient.available() && !navigator.received();
		int bodyTop = waiting ? TOP : tabs(navigator);
		if (body != null) {
			// Laid out again (an answer, a resize, back from the Spell Composer): keep the place the player scrolled to.
			scroll = body.scrollAmount();
		}
		body = addRenderableWidget(new ComposerScroll(minecraft, left, bodyTop, contentWidth + SCROLLBAR, Math.max(ROW, footerY - 4 - bodyTop), this::focused));
		int y = 0;
		Button guides = Button.builder(Component.translatable("screen.jugcraft.routes.open"),
				button -> minecraft.gui.setScreen(new EncyclopediaRoutesScreen(this)))
				.bounds(left, 0, contentWidth, ROW).build();
		body.add(keyed("guides", guides), y);
		y += ROW + GAP;
		if (!EncyclopediaClient.available()) {
			y = line(y, Component.translatable("screen.jugcraft.encyclopedia.unavailable").withStyle(ChatFormatting.YELLOW), 0, "unavailable");
		}
		for (int i = 0; i < EncyclopediaClient.notices().size(); i++) {
			y = line(y, EncyclopediaClient.notices().get(i).copy().withStyle(ChatFormatting.YELLOW), 0, "notice:" + i);
		}
		EncyclopediaPayload.Page page = waiting ? null : navigator.current();
		if (waiting) {
			y = line(y, Component.translatable("screen.jugcraft.encyclopedia.loading"), 0, "loading");
		} else if (page == null) {
			y = line(y, Component.translatable("screen.jugcraft.encyclopedia.empty"), 0, "empty");
		} else {
			y = rows(y, page);
		}
		line(y + GAP, Component.translatable("screen.jugcraft.encyclopedia.help", EncyclopediaClient.key().getTranslatedKeyMessage())
				.withStyle(ChatFormatting.GRAY), 0, "help");
		String shown = page == null ? "" : page.id();
		body.arrange(shown.equals(laidOut) ? scroll : 0);
		laidOut = shown;
		for (Map.Entry<GuiEventListener, String> control : keys.entrySet()) {
			if (control.getValue().equals(focus)) {
				refocus(control.getKey());
				break;
			}
		}
	}

	/**
	 * Opening and every new layout call this after {@link #init()}, which has already given the keyboard back to the
	 * control that had it; vanilla's default would move it on one control each time.
	 */
	@Override
	protected void setInitialFocus() {
		if (getFocused() == null) {
			super.setInitialFocus();
		}
	}

	/** A tab for each page (the one being read is greyed), or, when they do not fit, arrows and the page's name. */
	private int tabs(Navigator navigator) {
		List<EncyclopediaPayload.Page> pages = navigator.pages();
		EncyclopediaPayload.Page current = navigator.current();
		int total = 0;
		for (EncyclopediaPayload.Page page : pages) {
			total += font.width(page.title()) + 12 + GAP;
		}
		if (total - GAP <= contentWidth + SCROLLBAR) {
			int x = left;
			for (EncyclopediaPayload.Page page : pages) {
				int tab = font.width(page.title()) + 12;
				Button button = addRenderableWidget(keyed("tab:" + page.id(), Button.builder(page.title(), pressed -> turnTo(page.id()))
						.bounds(x, TOP, tab, ROW).build()));
				button.active = current == null || !current.id().equals(page.id());
				x += tab + GAP;
			}
		} else if (!pages.isEmpty()) {
			addRenderableWidget(keyed("previous", Button.builder(Component.literal("<"), pressed -> turn(-1)).bounds(left, TOP, ROW, ROW).build()));
			addRenderableWidget(keyed("next", Button.builder(Component.literal(">"), pressed -> turn(1))
					.bounds(left + contentWidth + SCROLLBAR - ROW, TOP, ROW, ROW).build()));
			int at = current == null ? 0 : Math.max(0, pages.indexOf(current));
			Component label = Component.translatable("screen.jugcraft.encyclopedia.page", current == null ? Component.empty() : current.title(), at + 1, pages.size());
			FocusableTextWidget name = FocusableTextWidget.builder(label, font, 2).maxWidth(contentWidth + SCROLLBAR - 2 * ROW - 2 * GAP)
					.alwaysShowBorder(false).backgroundFill(FocusableTextWidget.BackgroundFill.NEVER).build();
			name.setPosition(left + (contentWidth + SCROLLBAR - name.getWidth()) / 2, TOP + (ROW - name.getHeight()) / 2);
			addRenderableWidget(keyed("page", name));
		}
		return TOP + ROW + GAP;
	}

	/** The page's rows, from {@code y} in the body; returns the y below them. */
	private int rows(int y, EncyclopediaPayload.Page page) {
		boolean showExact = ConcordanceClientOptions.exactValues();
		for (int i = 0; i < page.rows().size(); i++) {
			EncyclopediaPayload.Row row = page.rows().get(i);
			String key = page.id() + ":" + i;
			EncyclopediaPayload.Kind kind = row.kindOf();
			if (kind == EncyclopediaPayload.Kind.HEADING) {
				y = line(y + (i > 0 ? GAP : 0), row.text().copy().withStyle(ChatFormatting.GOLD), 0, key);
				continue;
			}
			if (kind == EncyclopediaPayload.Kind.LINK && row.link().isPresent()) {
				EncyclopediaPayload.Link link = row.link().get();
				int width = Math.min(contentWidth, font.width(row.text()) + 16);
				Button button = Button.builder(row.text(), pressed -> EncyclopediaClient.follow(minecraft, link, this)).bounds(left, 0, width, ROW).build();
				body.add(keyed(key, button), y);
				y += ROW + 2;
				continue;
			}
			int indent = kind == EncyclopediaPayload.Kind.EDGE ? INDENT : 0;
			boolean marked = kind == EncyclopediaPayload.Kind.STEP || kind == EncyclopediaPayload.Kind.EDGE;
			MutableComponent text = words(row);
			if (showExact && row.exact().isPresent()) {
				text.append(" ").append(row.exact().get().copy().withStyle(ChatFormatting.GRAY));
			}
			int textX = indent + (marked ? EncyclopediaWidgets.Mark.WIDTH + 2 : 0);
			FocusableTextWidget words = text(text, textX);
			if (row.grid().isPresent()) {
				words.setUsageNarration(EncyclopediaWidgets.describe(row.grid().get()), null);
			} else if (!row.icons().isEmpty()) {
				words.setUsageNarration(EncyclopediaWidgets.names(row.icons()), null);
			}
			if (marked) {
				body.add(new EncyclopediaWidgets.Mark(left + indent, row), y + 2);
			}
			body.add(keyed(key, words), y);
			y += words.getHeight();
			if (row.grid().isPresent()) {
				EncyclopediaWidgets.Grid grid = new EncyclopediaWidgets.Grid(left + textX + 2, font, row.grid().get());
				body.add(grid, y + 1);
				y += grid.getHeight() + 3;
			} else if (!row.icons().isEmpty()) {
				EncyclopediaWidgets.Items icons = new EncyclopediaWidgets.Items(left + textX + 2, font, row.icons());
				body.add(icons, y + 1);
				y += icons.getHeight() + 3;
			}
		}
		return y;
	}

	/** A row's words: a step's state and weight, or a way's weight, before its text. Nothing relies on the colour. */
	private static MutableComponent words(EncyclopediaPayload.Row row) {
		return switch (row.kindOf()) {
			case STEP -> Component.translatable("encyclopedia.jugcraft.row.step", state(row.stateOf()),
					row.edgeOf() == EncyclopediaPayload.EdgeKind.OPTIONAL_BOSS
							? Component.translatable("encyclopedia.jugcraft.edge.optional_boss.step").withStyle(ChatFormatting.LIGHT_PURPLE)
							: edge(row.edgeOf()), row.text());
			case EDGE -> Component.translatable("encyclopedia.jugcraft.row.edge", edge(row.edgeOf()), row.text());
			default -> row.text().copy();
		};
	}

	private static Component state(EncyclopediaPayload.StepState state) {
		ChatFormatting colour = switch (state) {
			case DONE -> ChatFormatting.GREEN;
			case NEXT -> ChatFormatting.GOLD;
			case OPEN -> ChatFormatting.AQUA;
			case BLOCKED -> ChatFormatting.RED;
			default -> ChatFormatting.GRAY;
		};
		return Component.translatable("encyclopedia.jugcraft.state." + state.id()).withStyle(colour);
	}

	private static Component edge(EncyclopediaPayload.EdgeKind edge) {
		ChatFormatting colour = switch (edge) {
			case RECOMMENDED -> ChatFormatting.AQUA;
			case TRADE -> ChatFormatting.YELLOW;
			case OPTIONAL_BOSS -> ChatFormatting.LIGHT_PURPLE;
			default -> ChatFormatting.WHITE;
		};
		return Component.translatable("encyclopedia.jugcraft.edge." + edge.id()).withStyle(colour);
	}

	private int line(int y, Component text, int indent, String key) {
		FocusableTextWidget widget = text(text, indent);
		body.add(keyed(key, widget), y);
		return y + widget.getHeight();
	}

	private FocusableTextWidget text(Component text, int indent) {
		FocusableTextWidget widget = FocusableTextWidget.builder(text, font, 2).maxWidth(contentWidth - indent).alwaysShowBorder(false)
				.backgroundFill(FocusableTextWidget.BackgroundFill.NEVER).build();
		widget.setCentered(false);
		widget.setX(left + indent);
		return widget;
	}

	private <T extends GuiEventListener> T keyed(String key, T control) {
		keys.put(control, key);
		return control;
	}

	private void focused(@Nullable GuiEventListener control) {
		String key = control == null ? null : keys.get(control);
		if (key != null) {
			focus = key;
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

	private static Component exactLabel() {
		return Component.translatable(ConcordanceClientOptions.exactValues() ? "screen.jugcraft.journal.exact_on" : "screen.jugcraft.journal.exact_off");
	}

	private void toggleExact() {
		ConcordanceClientOptions.setExactValues(!ConcordanceClientOptions.exactValues());
		dirty = true;
	}

	private void turnTo(String page) {
		if (EncyclopediaClient.navigator().go(page)) {
			focus = "tab:" + page;
			dirty = true;
		}
	}

	private void turn(int step) {
		if (EncyclopediaClient.navigator().turn(step)) {
			dirty = true;
		}
	}

	@Override
	public void tick() {
		super.tick();
		if (dirty) {
			// Laid out again once a tick at most, after whatever changed (an answer, a page, the exact figures).
			dirty = false;
			rebuildWidgets();
		}
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (EncyclopediaClient.key().matches(event)) {
			onClose();
			return true;
		}
		int key = event.input();
		if (key == InputConstants.KEY_LEFT) {
			turn(-1);
		} else if (key == InputConstants.KEY_RIGHT) {
			turn(1);
		} else if (key == InputConstants.KEY_BACKSPACE) {
			if (EncyclopediaClient.navigator().back()) {
				dirty = true;
			}
		} else if (key == InputConstants.KEY_PAGEUP && body != null) {
			body.setScrollAmount(body.scrollAmount() - body.getHeight() + ROW);
		} else if (key == InputConstants.KEY_PAGEDOWN && body != null) {
			body.setScrollAmount(body.scrollAmount() + body.getHeight() - ROW);
		} else if (event.shortcutKey() == InputConstants.KEYCODE_R) {
			// The letters as the keyboard's layout names them, as vanilla's own shortcuts are.
			EncyclopediaClient.request();
		} else if (event.shortcutKey() == InputConstants.KEYCODE_X) {
			toggleExact();
		} else {
			return super.keyPressed(event);
		}
		return true;
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	/** Back to the game, or to a fresh inventory (as the inventory key would open it). */
	@Override
	public void onClose() {
		if (back.inventory() && minecraft.player != null && minecraft.gameMode != null && !minecraft.player.isSpectator()) {
			if (minecraft.gameMode.isServerControlledInventory()) {
				minecraft.gui.setScreen(null);
				minecraft.player.sendOpenInventory();
			} else {
				minecraft.gui.setScreen(new InventoryScreen(minecraft.player));
			}
			return;
		}
		minecraft.gui.setScreen(null);
	}

	@Override
	public void removed() {
		EncyclopediaClient.unlisten(listener);
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

	/** Every control in the body (tests find the rows and links through it). */
	public List<? extends GuiEventListener> rows() {
		return body == null ? List.of() : body.children();
	}

	/** The body's widget showing {@code text} exactly, if any (tests use it). */
	public @Nullable AbstractWidget find(Component text) {
		if (body == null) {
			return null;
		}
		for (GuiEventListener child : body.children()) {
			if (child instanceof AbstractWidget widget && widget.getMessage().getString().equals(text.getString())) {
				return widget;
			}
		}
		return null;
	}
}
