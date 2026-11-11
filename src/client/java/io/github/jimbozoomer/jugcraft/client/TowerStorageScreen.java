package io.github.jimbozoomer.jugcraft.client;

import io.github.jimbozoomer.jugcraft.tower.TowerCoreBlockEntity;
import io.github.jimbozoomer.jugcraft.tower.TowerData;
import io.github.jimbozoomer.jugcraft.tower.TowerStorageMenu;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;

/**
 * The tower storage at the Cargo Exchange (tower/TowerStorageMenu), in the tower screens' colours: TOWER STORAGE and
 * the next tier over a chest's grid of what it holds (six rows of nine, scrolled with the wheel or the bar on the
 * right), how many of its 540 stacks are used, LOAD BLOCKS, and the player's inventory.
 */
public class TowerStorageScreen extends AbstractContainerScreen<TowerStorageMenu> {
	private static final int FRAME = 0xFF3A2422, GLASS = 0xF0141012;
	private static final int ACCENT = 0xFFE05040, DIM = 0xFFC28E84, WHITE = 0xFFECE6E6, AMBER = 0xFFFFC04A;
	private static final int SLOT_EDGE = 0xFF3A2422, SLOT_IN = 0xFF0C0A0B, BAR = 0xFF0C0A0B, THUMB = 0xFFC28E84, THUMB_ON = 0xFFE05040;
	/** The scroll bar: right of the grid, as tall as it. */
	private static final int BAR_X = TowerStorageMenu.GRID_X + TowerStorageMenu.COLS * 18 + 4, BAR_W = 12, THUMB_H = 15;
	private boolean dragging;
	/** The row last asked for (the server's answer comes back as the menu's row). */
	private int asked = -1;

	public TowerStorageScreen(TowerStorageMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title, TowerStorageMenu.WIDTH, TowerStorageMenu.HEIGHT);
	}

	@Override
	protected void init() {
		super.init();
		addRenderableWidget(Button.builder(Component.translatable("screen.jugcraft.tower_storage.load_blocks"), b -> press(TowerStorageMenu.LOAD_BLOCKS))
				.bounds(leftPos + imageWidth - 8 - 74, topPos + 5, 74, 18).build());
	}

	private void press(int id) {
		if (minecraft != null && minecraft.gameMode != null) {
			minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
		}
	}

	private int maxRow() {
		return TowerStorageMenu.maxRow(menu.stacks());
	}

	private int shownRow() {
		return asked >= 0 ? asked : menu.row();
	}

	/** Scrolls to {@code row} (asked of the server: the grid's slots are its). */
	private void scrollTo(int row) {
		row = Mth.clamp(row, 0, maxRow());
		if (row != shownRow()) {
			asked = row;
			press(TowerStorageMenu.SCROLL + row);
		}
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
		super.extractBackground(g, mouseX, mouseY, delta);
		if (asked == menu.row()) {
			asked = -1;                                          // (the server has it)
		}
		int left = leftPos, top = topPos, w = imageWidth, h = imageHeight;
		g.fill(left - 2, top - 2, left + w + 2, top + h + 2, FRAME);
		g.fill(left, top, left + w, top + h, GLASS);
		for (int[] c : new int[][] {{left, top}, {left + w - 10, top}, {left, top + h - 2}, {left + w - 10, top + h - 2}}) {
			g.fill(c[0], c[1], c[0] + 10, c[1] + 2, ACCENT);
		}
		TowerCoreBlockEntity core = menu.tower();
		// the title and the next tier; how full it is
		g.fill(left + 8, top + 7, left + 11, top + 16, ACCENT);
		g.text(font, Component.translatable("screen.jugcraft.tower_load.storage_heading").getString(), left + 14, top + 8, ACCENT, false);
		if (core != null) {
			int used = core.storageStacksUsed();
			g.text(font, Component.translatable("screen.jugcraft.tower_load.stacks", used, TowerCoreBlockEntity.STORAGE_STACKS).getString(),
					left + 8, top + 22, used >= TowerCoreBlockEntity.STORAGE_STACKS ? AMBER : WHITE, false);
			TowerData.Tier next = core.nextStep();
			if (next != null) {
				String tier = Component.translatable("screen.jugcraft.tower_load.next_tier", next.number,
						Component.translatable("tower.jugcraft.tier." + next.number).getString().toUpperCase(Locale.ROOT)).getString();
				g.text(font, tier, left + w - 8 - font.width(tier), top + 25, DIM, false);
			}
		}
		// the grid and its bar
		for (int r = 0; r < TowerStorageMenu.ROWS; r++) {
			for (int c = 0; c < TowerStorageMenu.COLS; c++) {
				slot(g, left + TowerStorageMenu.GRID_X - 1 + c * 18, top + TowerStorageMenu.GRID_Y - 1 + r * 18);
			}
		}
		int bx = left + BAR_X, by = top + TowerStorageMenu.GRID_Y - 1, bh = TowerStorageMenu.ROWS * 18;
		g.fill(bx, by, bx + BAR_W, by + bh, SLOT_EDGE);
		g.fill(bx + 1, by + 1, bx + BAR_W - 1, by + bh - 1, BAR);
		int max = maxRow();
		int ty = by + 1 + (max == 0 ? 0 : Math.round((bh - 2 - THUMB_H) * shownRow() / (float) max));
		g.fill(bx + 2, ty, bx + BAR_W - 2, ty + THUMB_H, max == 0 ? SLOT_EDGE : dragging ? THUMB_ON : THUMB);
		// the hint under it, and the inventory
		g.text(font, Component.translatable("screen.jugcraft.tower_storage.hint").getString(), left + 8,
				top + TowerStorageMenu.GRID_Y + TowerStorageMenu.ROWS * 18 + 3, DIM, false);
		for (int r = 0; r < 3; r++) {
			for (int c = 0; c < 9; c++) {
				slot(g, left + TowerStorageMenu.INV_X - 1 + c * 18, top + TowerStorageMenu.INV_Y - 1 + r * 18);
			}
		}
		for (int c = 0; c < 9; c++) {
			slot(g, left + TowerStorageMenu.INV_X - 1 + c * 18, top + TowerStorageMenu.HOTBAR_Y - 1);
		}
	}

	private void slot(GuiGraphicsExtractor g, int x, int y) {
		g.fill(x, y, x + 18, y + 18, SLOT_EDGE);
		g.fill(x + 1, y + 1, x + 17, y + 17, SLOT_IN);
	}

	private boolean onBar(double x, double y) {
		int bx = leftPos + BAR_X, by = topPos + TowerStorageMenu.GRID_Y - 1;
		return x >= bx && x < bx + BAR_W && y >= by && y < by + TowerStorageMenu.ROWS * 18;
	}

	private void dragTo(double y) {
		int by = topPos + TowerStorageMenu.GRID_Y, bh = TowerStorageMenu.ROWS * 18 - 2 - THUMB_H;
		float f = Mth.clamp((float) (y - by - THUMB_H / 2.0) / Math.max(1, bh), 0, 1);
		scrollTo(Math.round(f * maxRow()));
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubled) {
		if (event.button() == 0 && onBar(event.x(), event.y()) && maxRow() > 0) {
			dragging = true;
			dragTo(event.y());
			return true;
		}
		return super.mouseClicked(event, doubled);
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
		if (dragging) {
			dragTo(event.y());
			return true;
		}
		return super.mouseDragged(event, dx, dy);
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		if (dragging && event.button() == 0) {
			dragging = false;
			return true;
		}
		return super.mouseReleased(event);
	}

	@Override
	public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
		if (maxRow() > 0 && scrollY != 0) {
			scrollTo(shownRow() - (int) Math.signum(scrollY));
			return true;
		}
		return super.mouseScrolled(x, y, scrollX, scrollY);
	}

	@Override
	protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
		// everything is drawn with the panel (extractBackground)
	}
}
