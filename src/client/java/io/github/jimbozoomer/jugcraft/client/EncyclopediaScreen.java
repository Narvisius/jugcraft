package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.jimbozoomer.jugcraft.guide.encyclopedia.EncyclopediaPayload;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.jspecify.annotations.Nullable;

/** The illustrated field guide: a small contents page, topic index, and a server-authored detail page.
 * Recipes, discovery states and links remain authoritative snapshots. Keyboard, narration, inspection and inventory
 * return use the existing entry points; this screen never grants progress or changes a player's inventory. */
public class EncyclopediaScreen extends Screen {
	private static final int MAX_WIDTH = 700;
	private static final int ROW = 20;
	private static final int GAP = 4;
	private static final int INDENT = 14;
	private static final int SCROLLBAR = 10;

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
	private int left;
	private int contentWidth;
	private String laidOut = "";
	private double scroll;
	private String focus = "";
	private boolean dirty;
    private boolean home = true;
    private String emptyPage = "";

    private int bookLeft, bookTop, bookWidth, bookHeight, pageWidth, section;
    private @Nullable ComposerScroll contents;
    private final java.util.ArrayList<Section> sections = new java.util.ArrayList<>();
    private long turnedAt = net.minecraft.util.Util.getMillis();
    private record Section(Component title, List<EncyclopediaPayload.Row> rows) { }

    public boolean isHome() { return home; }
    public void showPage(String id) { emptyPage = ""; home = false; section = 0; scroll = 0; turnTo(id); dirty = true; }
    public void showContents() { emptyPage = ""; home = true; dirty = true; }
    private static Component ui(String key, Object... args) { return ComposerScreen.ui(key, args); }


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
        EncyclopediaClient.unlisten(listener); EncyclopediaClient.listen(listener); keys.clear();
        bookWidth = Math.min(MAX_WIDTH, width - 20); bookHeight = Math.min(410, height - 18);
        bookLeft = (width - bookWidth) / 2; bookTop = (height - bookHeight) / 2;
        pageWidth = bookWidth / 2 - 34;
        left = bookLeft + bookWidth / 2 + 15; contentWidth = pageWidth - 6;
        int footer = bookTop + bookHeight - 29;
        paperButton("home", bookLeft + 16, footer, Math.min(94, pageWidth), 18, ui("contents"), "", () -> showContents());
        paperButton("done", bookLeft + bookWidth - 37, bookTop + 14, 17, 17, Component.literal("×"), "", () -> onClose());
        if (home) { initHome(); body = null; return; }
        paperButton("refresh", left, footer, (pageWidth - 4) / 2, 18, ui("refresh"), "", EncyclopediaClient::request);
        paperButton("exact", left + (pageWidth - 4) / 2 + 3, footer, (pageWidth - 4) / 2, 18,
            ui(ConcordanceClientOptions.exactValues() ? "less" : "details"), "", this::toggleExact);
        int bodyTop = bookTop + 60;
        String current = page();
        if (body != null && current.equals(laidOut)) scroll = body.scrollAmount();
        else if (!current.equals(laidOut)) { scroll = 0; section = 0; }
        EncyclopediaPayload.Page page = emptyPage.isEmpty() ? EncyclopediaClient.navigator().current() : null;
        split(page);
        section = Math.max(0, Math.min(section, sections.size() - 1));
        contents = addRenderableWidget(new ComposerScroll(minecraft, bookLeft + 18, bookTop + 58, pageWidth + 4,
            Math.max(20, footer - bookTop - 66), this::focused));
        int cy = 0;
        for (int i = 0; i < sections.size(); i++) {
            int at = i;
            BookArt.Tab tab = new BookArt.Tab(bookLeft + 18, 0, pageWidth - 7, 27, sections.get(i).title(), Component.empty(),
                sectionIcon(sections.get(i)), section == i, false, unused -> { section = at; scroll = 0; body = null; turnedAt = net.minecraft.util.Util.getMillis(); dirty = true; });
            contents.add(keyed("section:" + i, tab), cy); cy += 29;
        }
        contents.arrange(Math.max(0, (section - 2) * 29));
        if (page != null && page.rows().stream().anyMatch(row -> row.link().isPresent() && row.link().get().kind() == EncyclopediaPayload.Link.COMPOSER)) {
            contents.setHeight(Math.max(20, contents.getHeight() - 24));
            paperButton("compose", bookLeft + 18, footer - 23, pageWidth - 7, 20,
                Component.translatable("encyclopedia.jugcraft.link.composer"), "", () -> ComposerClient.open(minecraft, this));
        }
        body = addRenderableWidget(new ComposerScroll(minecraft, left, bodyTop, contentWidth + SCROLLBAR,
            Math.max(ROW, footer - 7 - bodyTop), this::focused));
        int ly = 0;
        for (Component notice : EncyclopediaClient.notices()) ly = line(ly, notice, 0, "notice:" + ly);
        if (!EncyclopediaClient.available()) ly = line(ly, Component.translatable("screen.jugcraft.encyclopedia.unavailable"), 0, "unavailable");
        if (!sections.isEmpty()) {
            Section shown = sections.get(section);
            ly = rows(ly, new EncyclopediaPayload.Page(current, shown.title(), shown.rows()));
        } else line(ly, ui(EncyclopediaClient.navigator().received() ? "book.empty" : "book.loading"), 0, "empty");
        body.arrange(scroll); laidOut = current;
        for (var control : keys.entrySet()) if (control.getValue().equals(focus)) { refocus(control.getKey()); break; }
    }

    /** Opens the topic containing an existing row/link; also used by the keyboard integration test. */
    public void showSectionContaining(Component text) {
        home = false;
        split(EncyclopediaClient.navigator().current());
        for (int i = 0; i < sections.size(); i++) {
            if (sections.get(i).rows().stream().anyMatch(row -> row.text().getString().equals(text.getString()))) {
                section = i; scroll = 0; body = null; dirty = true; return;
            }
        }
    }

    private Button paperButton(String key, int x, int y, int w, int h, Component label, String icon, Runnable action) {
        return addRenderableWidget(keyed(key, new BookArt.Tab(x, y, w, h, label, Component.empty(), icon, false, false, unused -> action.run())));
    }

    private void initHome() {
        contents = null;
        String[] ids = {"technology", "magic", "discover", "spellcraft", "expedition", "keys"};
        String[] icons = {"jugcraft:steel_ingot", "jugcraft:initiate_wand", "minecraft:lantern", "minecraft:amethyst_shard", "minecraft:bone", "minecraft:compass"};
        int row = Math.min(40, (bookHeight - 80) / ids.length);
        for (int i = 0; i < ids.length; i++) {
            String id = ids[i];
            int route = i;
            Component subtitle = ui("book." + id + ".hint");
            BookArt.Tab tab = new BookArt.Tab(left, bookTop + 43 + i * row, pageWidth, row - 2,
                ui("book." + id), subtitle, icons[i], false, false, unused -> {
                    if (route < 2) { EncyclopediaRoutesScreen guide = new EncyclopediaRoutesScreen(this); minecraft.gui.setScreen(guide); guide.showRoute(route); }
                    else if (EncyclopediaClient.navigator().pages().stream().anyMatch(p -> p.id().equals(id))) showPage(id);
                    else { emptyPage = id; home = false; body = null; dirty = true; }
                });
            addRenderableWidget(keyed("chapter:" + id, tab));
        }

    }

    /** Headings and progression steps become topics; only the selected topic's detail is laid out. */
    private void split(EncyclopediaPayload.@Nullable Page page) {
        sections.clear(); if (page == null) return;
        Component title = page.title();
        java.util.ArrayList<EncyclopediaPayload.Row> rows = new java.util.ArrayList<>();
        for (EncyclopediaPayload.Row row : page.rows()) {
            if ((row.kindOf() == EncyclopediaPayload.Kind.HEADING || row.kindOf() == EncyclopediaPayload.Kind.STEP) && !rows.isEmpty()) {
                sections.add(new Section(title, List.copyOf(rows))); rows.clear();
            }
            if (rows.isEmpty() && (row.kindOf() == EncyclopediaPayload.Kind.HEADING || row.kindOf() == EncyclopediaPayload.Kind.STEP)) title = row.text();
            rows.add(row);
        }
        if (!rows.isEmpty()) sections.add(new Section(title, List.copyOf(rows)));
    }

    private String sectionIcon(Section section) {
        for (var row : section.rows()) if (!row.icons().isEmpty())
            return net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(row.icons().getFirst()).toString();
        return switch (page()) { case "spellcraft" -> "jugcraft:initiate_wand"; case "expedition" -> "minecraft:bone"; case "keys" -> "minecraft:compass"; case "household" -> "jugcraft:household_workbench"; case "habitats" -> "minecraft:oak_sapling"; case "beasts" -> "jugcraft:beast_animal_dictionary"; case "caverns" -> "jugcraft:cavern_cave_book"; default -> "minecraft:book"; };
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


	/** The page's rows, from {@code y} in the body; returns the y below them. */
	private int rows(int y, EncyclopediaPayload.Page page) {
		boolean showExact = ConcordanceClientOptions.exactValues();
		for (int i = 0; i < page.rows().size(); i++) {
			EncyclopediaPayload.Row row = page.rows().get(i);
			String key = page.id() + ":" + i;
			EncyclopediaPayload.Kind kind = row.kindOf();
			if (kind == EncyclopediaPayload.Kind.HEADING) {
				y = line(y + (i > 0 ? GAP : 0), row.text().copy().withColor(BookArt.INK & 0xFFFFFF), 0, key);
				continue;
			}
			if (kind == EncyclopediaPayload.Kind.LINK && row.link().isPresent()) {
				EncyclopediaPayload.Link link = row.link().get();
				Button button = new BookArt.Tab(left, 0, contentWidth, ROW, row.text(), Component.empty(), "", false, false, pressed -> EncyclopediaClient.follow(minecraft, link, this));
				body.add(keyed(key, button), y);
				y += ROW + 2;
				continue;
			}
			int indent = kind == EncyclopediaPayload.Kind.EDGE ? INDENT : 0;
			boolean marked = kind == EncyclopediaPayload.Kind.STEP || kind == EncyclopediaPayload.Kind.EDGE;
			MutableComponent text = words(row);
			if (showExact && row.exact().isPresent()) {
				text.append(" ").append(row.exact().get().copy().withStyle(ChatFormatting.DARK_GRAY));
			}
			int textX = indent + (marked ? EncyclopediaWidgets.Mark.WIDTH + 2 : 0);
			BookArt.Paragraph words = text(text, textX);
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
				EncyclopediaWidgets.Items icons = new EncyclopediaWidgets.Items(left + textX + 2, font, row.icons(), contentWidth - textX - 2);
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
							? Component.translatable("encyclopedia.jugcraft.edge.optional_boss.step").withStyle(ChatFormatting.DARK_PURPLE)
							: edge(row.edgeOf()), row.text());
			case EDGE -> Component.translatable("encyclopedia.jugcraft.row.edge", edge(row.edgeOf()), row.text());
			default -> row.text().copy();
		};
	}

	private static Component state(EncyclopediaPayload.StepState state) {
		ChatFormatting colour = switch (state) {
			case DONE -> ChatFormatting.DARK_GREEN;
			case NEXT -> ChatFormatting.DARK_PURPLE;
			case OPEN -> ChatFormatting.DARK_AQUA;
			case BLOCKED -> ChatFormatting.DARK_RED;
			default -> ChatFormatting.DARK_GRAY;
		};
		return Component.translatable("encyclopedia.jugcraft.state." + state.id()).withStyle(colour);
	}

	private static Component edge(EncyclopediaPayload.EdgeKind edge) {
		ChatFormatting colour = switch (edge) {
			case RECOMMENDED -> ChatFormatting.DARK_AQUA;
			case TRADE -> ChatFormatting.GOLD;
			case OPTIONAL_BOSS -> ChatFormatting.DARK_PURPLE;
			default -> ChatFormatting.DARK_GRAY;
		};
		return Component.translatable("encyclopedia.jugcraft.edge." + edge.id()).withStyle(colour);
	}

	private int line(int y, Component text, int indent, String key) {
		BookArt.Paragraph widget = text(text, indent);
		body.add(keyed(key, widget), y);
		return y + widget.getHeight();
	}

	private BookArt.Paragraph text(Component text, int indent) {
        BookArt.Paragraph widget = new BookArt.Paragraph(text.copy().withColor(BookArt.INK & 0xFFFFFF), font, contentWidth - indent, BookArt.INK);
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
		if (contents != null && contents.children().contains(control)) {
            contents.setFocused(control); setFocused(contents);
        } else if (body != null && body.children().contains(control)) {
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

	private void toggleExact() {
		ConcordanceClientOptions.setExactValues(!ConcordanceClientOptions.exactValues());
		dirty = true;
	}

	private void turnTo(String page) {
		home = false;
        turnedAt = net.minecraft.util.Util.getMillis();
		if (EncyclopediaClient.navigator().go(page)) {
			focus = "tab:" + page;
			dirty = true;
		}
	}

	private void turn(int step) {
        emptyPage = "";
		home = false;
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
			showContents();
		} else if (key == InputConstants.KEY_PAGEUP && body != null) {
			body.setScrollAmount(body.scrollAmount() - body.getHeight() + ROW);
		} else if (key == InputConstants.KEY_PAGEDOWN && body != null) {
			body.setScrollAmount(body.scrollAmount() + body.getHeight() - ROW);
		} else if (key == InputConstants.KEY_F6) {
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
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        g.fill(0, 0, width, height, 0xB20C1118);
        BookArt.book(g, bookLeft, bookTop, bookWidth, bookHeight);
        Component heading = home ? ui("book.volume") : !emptyPage.isEmpty() ? ui("book." + emptyPage) : (EncyclopediaClient.navigator().current() == null ? title : EncyclopediaClient.navigator().current().title());
        g.text(font, BookArt.fit(font, heading.getString(), pageWidth), bookLeft + 20, bookTop + 21, BookArt.INK, false);
        BookArt.rule(g, bookLeft + 20, bookTop + 36, pageWidth - 4);
        g.text(font, home ? ui("contents") : ui("book.topic", section + 1, Math.max(1, sections.size())), left, bookTop + 21, BookArt.INK, false);
        BookArt.rule(g, left, bookTop + 36, pageWidth - 2);
        if (home) {
            int cx = bookLeft + 18 + pageWidth / 2, cy = bookTop + (bookHeight - 50) / 2;
            int r = Math.min(45, Math.max(23, (bookHeight - 125) / 2));
            BookArt.circle(g, cx, cy, r, 0x80876D43);
            BookArt.circle(g, cx, cy, r - 4, 0x50876D43);
            BookArt.line(g, cx - r, cy, cx + r, cy, 0x60876D43);
            BookArt.line(g, cx, cy - r, cx, cy + r, 0x60876D43);
            BookArt.star(g, cx, cy, r / 2, BookArt.GOLD);
            BookArt.icon(g, "minecraft:book", cx - 8, cy - 8);
            BookArt.icon(g, "jugcraft:initiate_wand", cx - r - 7, cy - 8);
            BookArt.icon(g, "jugcraft:steel_ingot", cx + r - 8, cy - 8);
            BookArt.icon(g, "minecraft:oak_sapling", cx - 8, cy - r - 7);
            BookArt.icon(g, "minecraft:bone", cx - 8, cy + r - 8);
            int ty = bookTop + bookHeight - 87;
            for (var line : font.split(ui("book.intro"), pageWidth - 6)) {
                if (ty + font.lineHeight <= bookTop + bookHeight - 60) g.text(font, line, bookLeft + 20, ty, BookArt.MUTED, false);
                ty += font.lineHeight + 1;
            }
        } else {
            g.text(font, ui("book.topics"), bookLeft + 20, bookTop + 44, BookArt.MUTED, false);
            double age = net.minecraft.util.Util.getMillis() - turnedAt;
            if (!ConcordanceClientOptions.reducedMotion() && age < 240) {
                int edge = left + (int) (pageWidth * (1 - age / 240));
                g.fill(edge, bookTop + 43, edge + 2, bookTop + bookHeight - 37, 0x60896C3F);
            }
        }
    }

	/** Every control in the body (tests find the rows and links through it). */
	public List<? extends GuiEventListener> rows() {
		return body == null ? List.of() : body.children();
	}

	/** The body's widget showing {@code text} exactly, if any (tests use it). */
	public @Nullable AbstractWidget find(Component text) {
        for (GuiEventListener child : children()) {
            if (child instanceof AbstractWidget widget && widget.getMessage().getString().equals(text.getString())) return widget;
        }
        if (body == null) return null;
		for (GuiEventListener child : body.children()) {
			if (child instanceof AbstractWidget widget && widget.getMessage().getString().equals(text.getString())) {
				return widget;
			}
		}
		return null;
	}
}
