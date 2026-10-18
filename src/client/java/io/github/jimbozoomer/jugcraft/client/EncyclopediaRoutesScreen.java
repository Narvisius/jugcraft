package io.github.jimbozoomer.jugcraft.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.klikli_dev.modonomicon.client.gui.BookGuiManager;
import com.klikli_dev.modonomicon.client.gui.book.BookAddress;
import com.klikli_dev.modonomicon.data.BookDataManager;
import com.mojang.blaze3d.platform.InputConstants;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import org.jspecify.annotations.Nullable;

/** Illustrated chapter directory over the existing generated handbook/codex routes; no duplicate progression data. */
public class EncyclopediaRoutesScreen extends Screen {
    private static final Identifier CONTENT = Jugcraft.id("encyclopedia/en_us.json");
    private static final Identifier CODEX = Jugcraft.id("arcane_concordance");
    private record Link(String type, int chapter, int page, @Nullable Identifier category) {
        Component describe() { return Component.translatable("screen.jugcraft.routes.opens." + type); }
    }
    private record Entry(String title, String text, String icon, Link link) { }
    private record Route(String title, String icon, int accent, List<String> summary, List<Entry> milestones, List<Entry> chapters) { }
    private final @Nullable Screen parent;
    private final List<String> intro = new ArrayList<>();
    private final List<Route> routes = new ArrayList<>();
    private int route = -1, cursor = -1, left, top, panelWidth, panelHeight, pageWidth;
    private boolean codexLoaded, milestones;
    private @Nullable ComposerScroll directory;
    private @Nullable ComposerScroll details;
    private List<Entry> entries = List.of();
    private List<Button> entryButtons = new ArrayList<>();

    public EncyclopediaRoutesScreen(@Nullable Screen parent) {
        super(Component.translatable("screen.jugcraft.routes.title")); this.parent = parent; load();
    }
	private void load() {
		Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(CONTENT);
		if (resource.isEmpty()) {
			return;
		}
		try (Reader reader = resource.get().openAsReader()) {
			JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
			for (JsonElement paragraph : root.getAsJsonArray("intro")) {
				intro.add(paragraph.getAsString());
			}
			for (JsonElement element : root.getAsJsonArray("routes")) {
				routes.add(parseRoute(element.getAsJsonObject()));
			}
		} catch (Exception e) {
			Jugcraft.LOGGER.warn("Could not read the Jugcraft Encyclopedia", e);
			intro.clear();
			routes.clear();
		}
	}

	private static Route parseRoute(JsonObject data) {
		List<String> summary = new ArrayList<>();
		for (JsonElement paragraph : data.getAsJsonArray("summary")) {
			summary.add(paragraph.getAsString());
		}
		int accent = 0xFF000000 | Integer.parseInt(data.get("accent").getAsString(), 16);
		return new Route(data.get("title").getAsString(), data.get("icon").getAsString(), accent, summary,
				parseEntries(data.getAsJsonArray("milestones")), parseEntries(data.getAsJsonArray("chapters")));
	}

	private static List<Entry> parseEntries(JsonArray array) {
		List<Entry> entries = new ArrayList<>();
		for (JsonElement element : array) {
			JsonObject data = element.getAsJsonObject();
			JsonObject link = data.getAsJsonObject("link");
			String type = link.get("type").getAsString();
			Link target = new Link(type, link.has("chapter") ? link.get("chapter").getAsInt() : 0,
					link.has("page") ? link.get("page").getAsInt() : 0,
					link.has("category") ? Identifier.parse(link.get("category").getAsString()) : null);
			entries.add(new Entry(data.get("title").getAsString(), data.has("text") ? data.get("text").getAsString() : "",
					data.get("icon").getAsString(), target));
		}
		return entries;
	}

    private static Component ui(String key, Object... args) { return ComposerScreen.ui(key, args); }
    public int route() { return route; }
    public void showRoute(int index) { route = index; cursor = -1; milestones = false; directory = null; rebuildWidgets(); }
    private void showHome() { route = -1; directory = null; rebuildWidgets(); }
    @Override public void onClose() { minecraft.gui.setScreen(parent); }
    @Override public boolean isPauseScreen() { return false; }
    @Override protected void init() {
        panelWidth = Math.min(700, width - 20); panelHeight = Math.min(410, height - 18);
        left = (width - panelWidth) / 2; top = (height - panelHeight) / 2; pageWidth = panelWidth / 2 - 34;
        codexLoaded = BookDataManager.get().getBook(CODEX) != null;
        tab(left + 18, top + panelHeight - 29, Math.min(100, pageWidth), 18, ui("contents"), "", false, this::onClose);
        if (route < 0 || route >= routes.size()) {
            route = -1;
            for (int i = 0; i < routes.size(); i++) {
                int at = i;
                Route r = routes.get(i);
                tab(left + (i % 2) * panelWidth / 2 + 20, top + 58, pageWidth, 56,
                    Component.literal(r.title()), r.icon(), false, () -> showRoute(at));
            }
            return;
        }
        Route shown = routes.get(route);
        tab(left + 18, top + 42, pageWidth / 2 - 2, 20, ui("book.chapters"), "", !milestones,
            () -> { milestones = false; cursor = -1; directory = null; rebuildWidgets(); });
        tab(left + 20 + pageWidth / 2, top + 42, pageWidth / 2 - 2, 20, ui("book.progression"), "", milestones,
            () -> { milestones = true; cursor = -1; directory = null; rebuildWidgets(); });
        double scroll = directory == null ? 0 : directory.scrollAmount();
        directory = addRenderableWidget(new ComposerScroll(minecraft, left + 18, top + 68, pageWidth + 4, panelHeight - 109, unused -> {}));
        entries = milestones ? shown.milestones() : shown.chapters();
        entryButtons.clear();
        int cy = 0;
        for (int i = 0; i < entries.size(); i++) {
            int at = i; Entry entry = entries.get(i);
            BookArt.Tab button = new BookArt.Tab(left + 18, 0, pageWidth - 6, 27, Component.literal(entry.title()), Component.literal(entry.text()),
                entry.icon(), cursor == i, false, unused -> { cursor = at; rebuildWidgets(); });
            directory.add(button, cy); entryButtons.add(button); cy += 29;
        }
        directory.arrange(scroll);
        int rx = left + panelWidth / 2 + 15;
        details = addRenderableWidget(new ComposerScroll(minecraft, rx, top + 82, pageWidth + 3, panelHeight - 140, unused -> {}));
        int dy = 0;
        if (cursor < 0 || cursor >= entries.size()) {
            dy = paragraph(dy, ui("book.choose_topic"));
            for (String text : shown.summary()) dy = paragraph(dy + 5, Component.literal(text));
        } else {
            Entry entry = entries.get(cursor);
            dy = paragraph(dy, Component.literal(entry.title()).withColor(BookArt.INK & 0xFFFFFF));
            if (!entry.text().isBlank()) dy = paragraph(dy + 7, Component.literal(entry.text()));
            dy = paragraph(dy + 10, entry.link().describe());
            Button read = tab(rx, top + panelHeight - 51, pageWidth, 20, ui("book.read"), "", true, () -> follow(entry.link()));
            read.active = available(entry.link());
            if (!read.active) paragraph(dy + 6, Component.translatable("screen.jugcraft.routes.unavailable"));
        }
        details.arrange(0);
    }
    private int paragraph(int y, Component text) {
        var widget = new BookArt.Paragraph(text.copy().withColor(BookArt.INK & 0xFFFFFF), font, pageWidth - 9, BookArt.INK);
        widget.setX(left + panelWidth / 2 + 17); details.add(widget, y);
        return y + widget.getHeight() + 3;
    }
    private Button tab(int x, int y, int w, int h, Component label, String icon, boolean selected, Runnable action) {
        return addRenderableWidget(new BookArt.Tab(x, y, w, h, label, Component.empty(), icon, selected, false, unused -> action.run()));
    }
	private boolean available(Link link) {
		return !link.type().equals("codex") || codexLoaded;
	}

	/** Opens where a link leads. The handbook closes back here; the codex and the journal close to the game. */
	private void follow(Link link) {
		if (!available(link)) {
			return;
		}
		switch (link.type()) {
			case "handbook" -> minecraft.gui.setScreen(new HandbookScreen(link.chapter(), link.page(), this));
			case "codex" -> BookGuiManager.get().openBook(BookAddress.of(CODEX, link.category(), null, -1));
			case "journal" -> JournalClient.open(minecraft);
			default -> Jugcraft.LOGGER.warn("Encyclopedia link of unknown type {}", link.type());
		}
	}

    @Override public boolean keyPressed(KeyEvent event) {
        if (route < 0) return super.keyPressed(event);
        if (event.input() == InputConstants.KEY_BACKSPACE) { showHome(); return true; }
        if (event.input() == InputConstants.KEY_DOWN || event.input() == InputConstants.KEY_UP) {
            if (!entries.isEmpty()) {
                int step = event.input() == InputConstants.KEY_DOWN ? 1 : -1;
                cursor = cursor < 0 ? (step > 0 ? 0 : entries.size() - 1) : Math.floorMod(cursor + step, entries.size());
                rebuildWidgets(); directory.setFocused(entryButtons.get(cursor)); setFocused(directory);
            }
            return true;
        }
        if (event.input() == InputConstants.KEY_RETURN && getFocused() == directory && cursor >= 0 && cursor < entries.size()) { follow(entries.get(cursor).link()); return true; }
        return super.keyPressed(event);
    }
    @Override public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        g.fill(0, 0, width, height, 0xB20C1118); BookArt.book(g, left, top, panelWidth, panelHeight);
        String title = route < 0 ? ui("book.guides").getString() : routes.get(route).title();
        g.text(font, BookArt.fit(font, title, pageWidth), left + 20, top + 21, BookArt.INK, false);
        BookArt.rule(g, left + 20, top + 36, pageWidth - 4);
        int rx = left + panelWidth / 2 + 15;
        g.text(font, ui("book.field_notes"), rx, top + 21, BookArt.INK, false);
        BookArt.rule(g, rx, top + 36, pageWidth - 4);
        if (route >= 0) {
            String icon = cursor >= 0 && cursor < entries.size() ? entries.get(cursor).icon() : routes.get(route).icon();
            int cx = rx + pageWidth / 2;
            BookArt.circle(g, cx, top + 59, 17, 0x80876D43);
            BookArt.icon(g, icon, cx - 8, top + 51);
        }
    }
}
