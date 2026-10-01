package io.github.jimbozoomer.jugcraft.client;

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
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.util.FormattedCharSequence;

/**
 * The Drone Tower guide books (Field Manual, Creative Quick Start): one page at a time, a heading, a screenshot
 * from the game and a short paragraph, in dark ink on light paper so it reads easily. Pages come from
 * assets/jugcraft/guide/&lt;book&gt;.json and the lang file (tools/guide_books.py). Reading only.
 */
public class GuideBookScreen extends Screen {
	private static final int WIDTH = 260;
	private static final int HEIGHT = 262;
	private static final int MARGIN = 14;
	private static final int TEXT_W = WIDTH - 2 * MARGIN;
	/** Room the heading, the text (five lines) and the page buttons need besides the picture. */
	private static final int CHROME_H = 122;
	/** Screenshots are stored at 16:9, this size. */
	private static final int SHOT_W = 512;
	private static final int SHOT_H = 288;

	private static final int COVER = 0xFF2B2E36;
	private static final int COVER_EDGE = 0xFF8A2A22;
	private static final int PAPER = 0xFFF4EEDF;
	private static final int PAPER_SHADE = 0xFFE4DAC2;
	private static final int INK = 0xFF1E1E22;
	private static final int INK_HEADING = 0xFF7A1F18;
	private static final int INK_LIGHT = 0xFF6A6458;

	private final String book;
	private final List<JsonObject> pages = new ArrayList<>();
	private int page;
	private int left;
	private int top;
	/** The book's height and its picture's size, smaller on short windows so the whole page always fits. */
	private int bookHeight;
	private int imageW;
	private int imageH;
	private Button previous;
	private Button next;

	public GuideBookScreen(String book) {
		super(Component.translatable("item.jugcraft." + book));
		this.book = book;
		load();
	}

	public int pageCount() {
		return pages.size();
	}

	public void showPage(int index) {
		page = Math.max(0, Math.min(index, pages.size() - 1));
	}

	private void load() {
		Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(Jugcraft.id("guide/" + book + ".json"));
		if (resource.isEmpty()) {
			return;
		}
		try (Reader reader = resource.get().openAsReader()) {
			for (JsonElement element : JsonParser.parseReader(reader).getAsJsonObject().getAsJsonArray("pages")) {
				pages.add(element.getAsJsonObject());
			}
		} catch (Exception e) {
			Jugcraft.LOGGER.warn("Could not read the {} guide book", book, e);
		}
	}

	@Override
	protected void init() {
		left = (width - WIDTH) / 2;
		bookHeight = Math.min(HEIGHT, height - 12);
		imageW = Math.min(TEXT_W, (bookHeight - CHROME_H) * 16 / 9);
		imageH = imageW * 9 / 16;
		top = (height - bookHeight) / 2;
		previous = addRenderableWidget(Button.builder(Component.literal("<"), button -> turn(-1))
				.bounds(left + MARGIN, top + bookHeight - 22, 20, 16).build());
		next = addRenderableWidget(Button.builder(Component.literal(">"), button -> turn(1))
				.bounds(left + WIDTH - MARGIN - 20, top + bookHeight - 22, 20, 16).build());
	}

	private void turn(int delta) {
		if (!pages.isEmpty()) {
			page = Math.max(0, Math.min(pages.size() - 1, page + delta));
		}
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	/** The blurred world, then the book: a dark cover with a red edge, and the paper. */
	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		super.extractBackground(graphics, mouseX, mouseY, delta);
		graphics.fill(left - 5, top - 5, left + WIDTH + 5, top + bookHeight + 5, COVER_EDGE);
		graphics.fill(left - 4, top - 4, left + WIDTH + 4, top + bookHeight + 4, COVER);
		graphics.fill(left, top, left + WIDTH, top + bookHeight, PAPER);
		graphics.fill(left, top, left + 3, top + bookHeight, PAPER_SHADE);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		previous.visible = page > 0;
		next.visible = page < pages.size() - 1;
		super.extractRenderState(graphics, mouseX, mouseY, delta);
		if (pages.isEmpty()) {
			graphics.text(font, "Guide content missing", left + MARGIN, top + MARGIN, INK, false);
			return;
		}
		JsonObject data = pages.get(page);
		int x = left + MARGIN;
		int y = top + MARGIN - 2;
		Component heading = Component.translatable(data.get("heading").getAsString());
		graphics.text(font, heading, x, y, INK_HEADING, false);
		y += font.lineHeight + 2;
		graphics.fill(x, y, x + TEXT_W, y + 1, INK_HEADING);
		y += 5;
		if (data.has("image")) {
			Identifier image = Identifier.parse(data.get("image").getAsString());
			int ix = x + (TEXT_W - imageW) / 2;
			graphics.fill(ix - 1, y - 1, ix + imageW + 1, y + imageH + 1, INK_LIGHT);
			graphics.blit(RenderPipelines.GUI_TEXTURED, image, ix, y, 0, 0, imageW, imageH, SHOT_W, SHOT_H, SHOT_W, SHOT_H);
			y += imageH + 7;
		}
		for (FormattedCharSequence line : font.split(Component.translatable(data.get("text").getAsString()), TEXT_W)) {
			graphics.text(font, line, x, y, INK, false);
			y += font.lineHeight + 1;
		}
		String counter = (page + 1) + " / " + pages.size();
		graphics.text(font, counter, left + (WIDTH - font.width(counter)) / 2, top + bookHeight - 18, INK_LIGHT, false);
	}
}
