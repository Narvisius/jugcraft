package io.github.jimbozoomer.jugcraft.client;

import static io.github.jimbozoomer.jugcraft.client.ExchangeScreen.*;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.jimbozoomer.jugcraft.finance.FinanceActionPayload;
import io.github.jimbozoomer.jugcraft.finance.FinanceActionPayload.Action;
import io.github.jimbozoomer.jugcraft.finance.FinanceBook;
import io.github.jimbozoomer.jugcraft.finance.MarketChestMenu;
import io.github.jimbozoomer.jugcraft.finance.MarketChestPayload;
import java.util.List;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * The Market Chest's screen, in the Exchange's walnut and brass: the chest's 27 slots and the player's inventory on the
 * left, and on the right the counter's ledger: which company it sells for, every kind of item it deals in (price,
 * how many are listed and how many still wait in the chest), a price box, Pause and Take back. Everything shown comes
 * from the server ({@link MarketChestPayload}); every change is checked there.
 */
public class MarketChestScreen extends AbstractContainerScreen<MarketChestMenu> {
	private static final int CHEST_WIDTH = 176;
	private static final int PANEL_WIDTH = 156;
	private static final int ROW = 18;
	private static final int ROWS_TOP = 34;
	private static final int VISIBLE_ROWS = 4;
	private static final int REFRESH_TICKS = 60;

	private MarketChestPayload data;
	private String selected = "";
	private String priceText = "";
	private int scroll;
	private int ticks;
	private EditBox priceBox;

	public MarketChestScreen(MarketChestMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title, CHEST_WIDTH + PANEL_WIDTH, 166);
	}

	/** New panel numbers from the server. */
	public void update(MarketChestPayload payload) {
		if (payload.pos() != menu.pos().asLong()) {
			return;
		}
		boolean first = data == null;
		String before = layout(data);
		data = payload;
		if (first && !payload.rows().isEmpty() && selected.isEmpty()) {
			select(payload.rows().get(0));
		}
		// Rebuild only when the buttons would change, so a refresh never steals the price box's focus.
		if (first || !layout(payload).equals(before)) {
			boolean typing = priceBox != null && priceBox.isFocused();
			rebuildWidgets();
			if (typing && priceBox != null) {
				setFocused(priceBox);
			}
		}
	}

	/** What decides the panel's buttons: the company, the choices, pause, and each row's item and price. */
	private static String layout(MarketChestPayload payload) {
		if (payload == null) {
			return "";
		}
		StringBuilder out = new StringBuilder().append(payload.corpId()).append(payload.paused()).append(payload.choices().size());
		for (MarketChestPayload.Row row : payload.rows()) {
			out.append('|').append(row.key()).append('=').append(row.price());
		}
		return out.toString();
	}

	private void send(Action action, String target, long amount, int period) {
		ClientPlayNetworking.send(new FinanceActionPayload(action, target, amount, period, "", menu.pos().asLong()));
	}

	@Override
	protected void containerTick() {
		super.containerTick();
		if (ticks++ % REFRESH_TICKS == 0) {
			send(Action.CHEST_INFO, "", 0, 0);
		}
	}

	private int panelX() {
		return leftPos + CHEST_WIDTH + 4;
	}

	private int panelWidth() {
		return PANEL_WIDTH - 12;
	}

	private void select(MarketChestPayload.Row row) {
		selected = row.key();
		priceText = row.price() > 0 ? String.valueOf(row.price()) : "";
	}

	private MarketChestPayload.Row selectedRow() {
		if (data == null) {
			return null;
		}
		for (MarketChestPayload.Row row : data.rows()) {
			if (row.key().equals(selected)) {
				return row;
			}
		}
		return null;
	}

	@Override
	protected void init() {
		super.init();
		titleLabelX = 8;
		titleLabelY = 6;
		inventoryLabelY = MarketChestMenu.INVENTORY_Y - 11;
		if (data == null) {
			return;
		}
		int x = panelX();
		int w = panelWidth();
		// Which company it sells for: a button that steps through the ones the player controls.
		List<MarketChestPayload.Choice> choices = data.choices();
		if (choices.size() > 1 || (data.corpId() <= 0 && !choices.isEmpty())) {
			addRenderableWidget(new Plate(x + w - 44, topPos + 17, 44, 12, "Switch", Plate.Kind.WOOD, b -> {
				int at = 0;
				for (int i = 0; i < choices.size(); i++) {
					if (choices.get(i).id() == data.corpId()) {
						at = i + 1;
					}
				}
				send(Action.CHEST_COMPANY, "", choices.get(at % choices.size()).id(), 0);
			}));
		}
		List<MarketChestPayload.Row> rows = data.rows();
		scroll = Math.max(0, Math.min(scroll, rows.size() - VISIBLE_ROWS));
		for (int i = 0; i < VISIBLE_ROWS && scroll + i < rows.size(); i++) {
			MarketChestPayload.Row row = rows.get(scroll + i);
			Plate plate = addRenderableWidget(new Plate(x, topPos + ROWS_TOP + i * ROW, w, ROW, "", Plate.Kind.ROW, b -> {
				select(row);
				rebuildWidgets();
			}));
			plate.active = !row.key().equals(selected);
		}
		int py = topPos + ROWS_TOP + VISIBLE_ROWS * ROW + 6;
		priceBox = new EditBox(font, x, py, 58, 14, Component.literal("Price"));
		priceBox.setMaxLength(9);
		priceBox.setValue(priceText);
		priceBox.setHint(Component.literal("Price").withStyle(ChatFormatting.DARK_GRAY));
		priceBox.setResponder(text -> priceText = text);
		priceBox.active = selectedRow() != null;
		addRenderableWidget(priceBox);
		Plate set = addRenderableWidget(new Plate(x + 62, py, 40, 14, "Set", Plate.Kind.BRASS, b -> {
			long price = FinanceBook.parseAmount(priceText);
			if (price > 0) {
				send(Action.CHEST_PRICE, selected, price, 0);
			}
		}));
		set.active = selectedRow() != null;
		Plate stop = addRenderableWidget(new Plate(x + 104, py, w - 104, 14, "Stop", Plate.Kind.WOOD,
				b -> send(Action.CHEST_PRICE, selected, 0, 0)));
		stop.active = selectedRow() != null && selectedRow().price() > 0;
		int by = py + 18;
		int half = (w - 4) / 2;
		addRenderableWidget(new Plate(x, by, half, 14, data.paused() ? "Resume" : "Pause", Plate.Kind.WOOD,
				b -> send(Action.CHEST_PAUSE, "", 0, data.paused() ? 0 : 1)));
		addRenderableWidget(new Plate(x + half + 4, by, half, 14, "Take back", Plate.Kind.WOOD,
				b -> send(Action.CHEST_TAKE_BACK, "", 0, 0)));
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		if (data != null && mouseX >= panelX()) {
			int before = scroll;
			scroll = Math.max(0, Math.min(scroll - (int) Math.signum(scrollY), data.rows().size() - VISIBLE_ROWS));
			if (scroll != before) {
				rebuildWidgets();
			}
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
	}

	/** While the price box has focus, keys type into it (so E does not close the screen). */
	@Override
	public boolean keyPressed(KeyEvent event) {
		if (priceBox != null && priceBox.isFocused() && event.input() != InputConstants.KEY_ESCAPE) {
			priceBox.keyPressed(event);
			return true;
		}
		return super.keyPressed(event);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
		extractBackground(g, mouseX, mouseY, delta);
		super.extractRenderState(g, mouseX, mouseY, delta);
		extractPanelText(g);
		extractTooltip(g, mouseX, mouseY);
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
		int x0 = leftPos;
		int y0 = topPos;
		int x1 = leftPos + imageWidth;
		int y1 = topPos + imageHeight;
		g.fill(x0 - 3, y0 - 3, x1 + 3, y1 + 3, WALNUT_DARK);
		g.fill(x0 - 2, y0 - 2, x1 + 2, y1 + 2, BRASS);
		g.fill(x0 - 1, y0 - 1, x1 + 1, y1 + 1, WALNUT_DARK);
		raised(g, x0, y0, x1, y1, WALNUT, WALNUT_LIGHT, WALNUT_DARK);
		// The chest's bay and the inventory: dark slots in a slate tray.
		sunken(g, x0 + 5, y0 + 15, x0 + CHEST_WIDTH - 5, y0 + 15 + 3 * 18 + 6, SLATE, SLATE_DARK, SLATE_LIGHT);
		sunken(g, x0 + 5, y0 + MarketChestMenu.INVENTORY_Y - 3, x0 + CHEST_WIDTH - 5, y0 + imageHeight - 5, SLATE, SLATE_DARK,
				SLATE_LIGHT);
		for (int row = 0; row < 3; row++) {
			for (int col = 0; col < 9; col++) {
				slot(g, x0 + MarketChestMenu.SLOTS_X + col * 18, y0 + MarketChestMenu.SLOTS_Y + row * 18);
			}
		}
		for (int row = 0; row < 3; row++) {
			for (int col = 0; col < 9; col++) {
				slot(g, x0 + 8 + col * 18, y0 + MarketChestMenu.INVENTORY_Y + row * 18);
			}
		}
		for (int col = 0; col < 9; col++) {
			slot(g, x0 + 8 + col * 18, y0 + MarketChestMenu.INVENTORY_Y + 58);
		}
		// The counter's ledger on the right: a brass rule down the join, then a slate page.
		g.fill(x0 + CHEST_WIDTH - 1, y0 + 4, x0 + CHEST_WIDTH, y1 - 4, BRASS_DARK);
		int px = panelX();
		int w = panelWidth();
		sunken(g, px - 2, y0 + ROWS_TOP - 2, px + w + 2, y0 + ROWS_TOP + VISIBLE_ROWS * ROW + 2, SLATE, SLATE_DARK, SLATE_LIGHT);
		if (data != null) {
			for (int i = 0; i < VISIBLE_ROWS && scroll + i < data.rows().size(); i++) {
				int ry = y0 + ROWS_TOP + i * ROW;
				if (data.rows().get(scroll + i).key().equals(selected)) {
					g.fill(px, ry, px + w, ry + ROW, 0x55B8913F);
				} else if (i % 2 == 1) {
					g.fill(px, ry, px + w, ry + ROW, SLATE_STRIPE);
				}
			}
		}
		filigree(g, x0 - 2, y0 - 2, 1, 1);
		filigree(g, x1 + 1, y0 - 2, -1, 1);
		filigree(g, x0 - 2, y1 + 1, 1, -1);
		filigree(g, x1 + 1, y1 + 1, -1, -1);
	}

	private static void slot(GuiGraphicsExtractor g, int x, int y) {
		g.fill(x - 1, y - 1, x + 17, y + 17, SLATE_DARK);
		g.fill(x, y, x + 17, y + 17, SLATE_LIGHT);
		g.fill(x, y, x + 16, y + 16, 0xFF24211C);
	}

	@Override
	protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
		g.text(font, title, titleLabelX, titleLabelY, GOLD, false);
		g.text(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, TAN, false);
	}

	/** The ledger's words, drawn over its widgets. */
	private void extractPanelText(GuiGraphicsExtractor g) {
		int x = panelX();
		int w = panelWidth();
		int y0 = topPos;
		if (data == null) {
			g.text(font, "Reading the ledger...", x, y0 + 6, DIM, false);
			return;
		}
		String company = data.company().isEmpty() ? "No company yet" : data.company();
		g.text(font, font.plainSubstrByWidth(company, w), x, y0 + 6, GOLD, true);
		String status = data.corpId() <= 0 ? "Choose who it sells for" : (data.paused() ? "Paused" : "Selling")
				+ " · treasury " + FinanceBook.amount(data.treasury());
		g.text(font, font.plainSubstrByWidth(status, w - 48), x, y0 + 19, data.paused() ? LOSS : DIM, false);
		List<MarketChestPayload.Row> rows = data.rows();
		if (rows.isEmpty()) {
			g.text(font, "Put goods in the chest,", x + 4, y0 + ROWS_TOP + 4, DIM, false);
			g.text(font, "then set a price for each.", x + 4, y0 + ROWS_TOP + 14, DIM, false);
		}
		for (int i = 0; i < VISIBLE_ROWS && scroll + i < rows.size(); i++) {
			MarketChestPayload.Row row = rows.get(scroll + i);
			int ry = y0 + ROWS_TOP + i * ROW;
			if (!row.item().isEmpty()) {
				g.item(row.item(), x + 1, ry + 1);
			}
			String name = row.item().isEmpty() ? row.key() : row.item().getHoverName().getString();
			g.text(font, font.plainSubstrByWidth(name, w - 22), x + 20, ry + 1, CREAM, false);
			String line = (row.price() > 0 ? FinanceBook.amount(row.price()) + " each" : "no price") + " · " + row.listed()
					+ " listed · " + row.inChest() + " here";
			g.text(font, font.plainSubstrByWidth(line, w - 22), x + 20, ry + 10, row.price() > 0 ? GAIN : DIM, false);
		}
		if (scroll > 0) {
			g.text(font, "^", x + w - 6, y0 + ROWS_TOP + 1, DIM, false);
		}
		if (scroll + VISIBLE_ROWS < rows.size()) {
			g.text(font, "v", x + w - 6, y0 + ROWS_TOP + VISIBLE_ROWS * ROW - 9, DIM, false);
		}
		if (!data.notice().isEmpty()) {
			int ny = y0 + ROWS_TOP + VISIBLE_ROWS * ROW + 42;
			int ly = ny;
			for (var line : font.split(Component.literal(data.notice()), w)) {
				if (ly > y0 + imageHeight - 12) {
					break;
				}
				g.text(font, line, x, ly, data.noticeOk() ? GAIN : LOSS, false);
				ly += 9;
			}
		}
	}
}
