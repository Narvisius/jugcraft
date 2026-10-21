package io.github.jimbozoomer.jugcraft.client;

import static io.github.jimbozoomer.jugcraft.client.ExchangeScreen.*;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;

/**
 * The Exchange's own guide book, in the Exchange's walnut-and-brass style: the page titles down the left, the open
 * page on the right (scroll with the wheel when it runs long). Pages come from assets/jugcraft/exchange_guide/en_us.json
 * (tools/exchange_guide.py). Opened by the Exchange Guide item and by the Guide button on the Exchange screen, which it
 * returns to when closed. Reading only.
 */
public class ExchangeGuideScreen extends Screen {
	private static final Identifier CONTENT = Jugcraft.id("exchange_guide/en_us.json");
	private static final int MAX_WIDTH = 384;
	private static final int MAX_HEIGHT = 236;
	private static final int FRAME = 4;
	private static final int HEADER = 22;
	private static final int LIST_WIDTH = 112;
	private static final int ROW = 11;
	private static final int LINE = 10;
	private static int lastPage;

	private final Screen parent;
	private final List<JsonObject> pages = new ArrayList<>();
	private String title = "The Exchange";
	private int page = lastPage;
	private int listScroll;
	private int textScroll;
	private int left;
	private int top;
	private int panelWidth;
	private int panelHeight;

	public ExchangeGuideScreen(Screen parent) {
		super(Component.literal("The Exchange: A Trader's Guide"));
		this.parent = parent;
		load();
	}

	private void load() {
		Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(CONTENT);
		if (resource.isEmpty()) {
			return;
		}
		try (Reader reader = resource.get().openAsReader()) {
			JsonObject book = JsonParser.parseReader(reader).getAsJsonObject();
			if (book.has("title")) {
				title = book.get("title").getAsString();
			}
			for (JsonElement element : book.getAsJsonArray("pages")) {
				pages.add(element.getAsJsonObject());
			}
		} catch (Exception e) {
			Jugcraft.LOGGER.warn("Could not read the Exchange guide", e);
		}
		page = Math.max(0, Math.min(page, pages.size() - 1));
	}

	@Override
	protected void init() {
		panelWidth = Math.min(MAX_WIDTH, width - 16);
		panelHeight = Math.min(MAX_HEIGHT, height - 16);
		left = (width - panelWidth) / 2;
		top = (height - panelHeight) / 2;
		int rows = visibleRows();
		listScroll = Math.max(0, Math.min(listScroll, pages.size() - rows));
		if (page < listScroll) {
			listScroll = page;
		} else if (page >= listScroll + rows) {
			listScroll = page - rows + 1;
		}
		for (int i = 0; i < rows && listScroll + i < pages.size(); i++) {
			int index = listScroll + i;
			Plate row = addRenderableWidget(new Plate(listX(), listTop() + i * ROW, LIST_WIDTH, ROW, "", Plate.Kind.ROW, b -> open(index)));
			row.active = index != page;
		}
		int by = top + panelHeight - FRAME - 16;
		Plate previous = addRenderableWidget(new Plate(pageX(), by, 46, 13, "< Back", Plate.Kind.WOOD, b -> open(page - 1)));
		previous.active = page > 0;
		Plate next = addRenderableWidget(new Plate(pageX() + pageWidth() - 46, by, 46, 13, "Next >", Plate.Kind.BRASS, b -> open(page + 1)));
		next.active = page < pages.size() - 1;
		addRenderableWidget(new Plate(listX(), by, LIST_WIDTH, 13, parent instanceof ExchangeScreen ? "Back to the Exchange" : "Close",
				Plate.Kind.WOOD, b -> onClose()));
	}

	private void open(int index) {
		if (index < 0 || index >= pages.size()) {
			return;
		}
		page = index;
		lastPage = index;
		textScroll = 0;
		rebuildWidgets();
	}

	private int listX() {
		return left + FRAME + 6;
	}

	private int listTop() {
		return top + FRAME + HEADER + 8;
	}

	private int visibleRows() {
		return Math.max(1, (top + panelHeight - FRAME - 22 - listTop()) / ROW);
	}

	private int pageX() {
		return listX() + LIST_WIDTH + 10;
	}

	private int pageWidth() {
		return left + panelWidth - FRAME - 6 - pageX();
	}

	private int textTop() {
		return listTop() + 22;
	}

	private int textBottom() {
		return top + panelHeight - FRAME - 20;
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		if (mouseX < pageX()) {
			int before = listScroll;
			listScroll = Math.max(0, Math.min(listScroll - (int) Math.signum(scrollY), pages.size() - visibleRows()));
			if (listScroll != before) {
				rebuildWidgets();
			}
		} else {
			textScroll = Math.max(0, Math.min(textScroll - (int) Math.signum(scrollY) * LINE * 2, maxTextScroll()));
		}
		return true;
	}

	private List<FormattedCharSequence> lines() {
		List<FormattedCharSequence> lines = new ArrayList<>();
		if (pages.isEmpty()) {
			return lines;
		}
		boolean first = true;
		for (JsonElement paragraph : pages.get(page).getAsJsonArray("text")) {
			if (!first) {
				lines.add(null); // a half-line gap between paragraphs
			}
			first = false;
			lines.addAll(font.split(Component.literal(paragraph.getAsString()), pageWidth() - 8));
		}
		return lines;
	}

	private int textHeight(List<FormattedCharSequence> lines) {
		int h = 0;
		for (FormattedCharSequence line : lines) {
			h += line == null ? LINE / 2 : LINE;
		}
		return h;
	}

	private int maxTextScroll() {
		return Math.max(0, textHeight(lines()) - (textBottom() - textTop() - 4));
	}

	@Override
	public void onClose() {
		Minecraft.getInstance().gui.setScreen(parent);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
		super.extractBackground(g, mouseX, mouseY, delta);
		int right = left + panelWidth;
		int bottom = top + panelHeight;
		g.fill(left - 3, top - 3, right + 3, bottom + 3, WALNUT_DARK);
		g.fill(left - 2, top - 2, right + 2, bottom + 2, BRASS);
		g.fill(left - 1, top - 1, right + 1, bottom + 1, WALNUT_DARK);
		raised(g, left, top, right, bottom, WALNUT, WALNUT_LIGHT, WALNUT_DARK);
		int hx0 = left + FRAME;
		int hx1 = right - FRAME;
		int hy0 = top + FRAME;
		int hy1 = hy0 + HEADER;
		sunken(g, hx0, hy0, hx1, hy1, PLAQUE, PLAQUE_DARK, PLAQUE_LIGHT);
		rivet(g, hx0 + 2, hy0 + 2);
		rivet(g, hx1 - 4, hy0 + 2);
		rivet(g, hx0 + 2, hy1 - 4);
		rivet(g, hx1 - 4, hy1 - 4);
		// The contents list and the page sit in slate boxes, like the Exchange's tables.
		sunken(g, listX() - 2, listTop() - 2, listX() + LIST_WIDTH + 2, listTop() + visibleRows() * ROW + 2, SLATE, SLATE_DARK, SLATE_LIGHT);
		sunken(g, pageX() - 2, listTop() - 2, pageX() + pageWidth() + 2, textBottom() + 1, SLATE, SLATE_DARK, SLATE_LIGHT);
		int rows = visibleRows();
		for (int i = 0; i < rows && listScroll + i < pages.size(); i++) {
			int y = listTop() + i * ROW;
			if (listScroll + i == page) {
				g.fill(listX(), y, listX() + LIST_WIDTH, y + ROW, 0x55B8913F);
			} else if (i % 2 == 1) {
				g.fill(listX(), y, listX() + LIST_WIDTH, y + ROW, SLATE_STRIPE);
			}
		}
		filigree(g, left - 2, top - 2, 1, 1);
		filigree(g, right + 1, top - 2, -1, 1);
		filigree(g, left - 2, bottom + 1, 1, -1);
		filigree(g, right + 1, bottom + 1, -1, -1);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
		super.extractRenderState(g, mouseX, mouseY, delta);
		int cx = left + panelWidth / 2;
		String heading = font.plainSubstrByWidth(title.toUpperCase(java.util.Locale.ROOT), panelWidth - 40);
		g.text(font, heading, cx - font.width(heading) / 2, top + FRAME + 7, GOLD, true);
		diamond(g, cx - font.width(heading) / 2 - 8, top + FRAME + 11, 2, BRASS);
		diamond(g, cx + font.width(heading) / 2 + 7, top + FRAME + 11, 2, BRASS);

		int rows = visibleRows();
		for (int i = 0; i < rows && listScroll + i < pages.size(); i++) {
			int index = listScroll + i;
			String name = (index + 1) + ". " + pages.get(index).get("title").getAsString();
			g.text(font, font.plainSubstrByWidth(name, LIST_WIDTH - 6), listX() + 3, listTop() + i * ROW + 2,
					index == page ? GOLD : CREAM, false);
		}
		if (listScroll > 0) {
			g.text(font, "^", listX() + LIST_WIDTH - 7, listTop() + 1, DIM, false);
		}
		if (listScroll + rows < pages.size()) {
			g.text(font, "v", listX() + LIST_WIDTH - 7, listTop() + rows * ROW - 9, DIM, false);
		}
		if (pages.isEmpty()) {
			g.text(font, "The guide's pages are missing.", pageX() + 4, listTop() + 4, LOSS, false);
			return;
		}

		JsonObject open = pages.get(page);
		int px = pageX();
		int py = listTop();
		if (open.has("icon")) {
			ItemStack icon = new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse(open.get("icon").getAsString())));
			g.item(icon, px + 2, py + 1);
		}
		g.text(font, font.plainSubstrByWidth(open.get("title").getAsString(), pageWidth() - 26), px + 22, py + 5, GOLD, true);
		g.fill(px + 2, py + 19, px + pageWidth() - 2, py + 20, GOLD_DIM);
		String count = (page + 1) + " / " + pages.size();
		g.text(font, count, px + (pageWidth() - font.width(count)) / 2, top + panelHeight - FRAME - 13, DIM, false);

		List<FormattedCharSequence> lines = lines();
		textScroll = Math.max(0, Math.min(textScroll, maxTextScroll()));
		g.enableScissor(px, textTop(), px + pageWidth(), textBottom() - 2);
		int y = textTop() + 2 - textScroll;
		for (FormattedCharSequence line : lines) {
			if (line == null) {
				y += LINE / 2;
				continue;
			}
			g.text(font, line, px + 4, y, CREAM, false);
			y += LINE;
		}
		g.disableScissor();
		if (maxTextScroll() > 0) {
			// A thin brass scroll bar on the page's right edge.
			int trackTop = textTop();
			int trackBottom = textBottom() - 2;
			int track = trackBottom - trackTop;
			int total = textHeight(lines);
			int thumb = Math.max(8, track * track / Math.max(1, total));
			int thumbTop = trackTop + (track - thumb) * textScroll / Math.max(1, maxTextScroll());
			g.fill(px + pageWidth() - 2, trackTop, px + pageWidth(), trackBottom, SLATE_DARK);
			g.fill(px + pageWidth() - 2, thumbTop, px + pageWidth(), thumbTop + thumb, BRASS);
		}
	}
}
