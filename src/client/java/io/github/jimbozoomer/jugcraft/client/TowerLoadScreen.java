package io.github.jimbozoomer.jugcraft.client;

import io.github.jimbozoomer.jugcraft.tower.TowerCoreBlockEntity;
import io.github.jimbozoomer.jugcraft.tower.TowerData;
import io.github.jimbozoomer.jugcraft.tower.TowerLoadMenu;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * The tower's loading screen (tower/TowerLoadMenu; the approved mockup tower_core_loading_mockup), in the tower screen's
 * colours: the blocks needed as rows (icon, name, a bar, in / needed; a finished row goes green), the PUT BLOCKS IN slot
 * with the total, LOAD ALL, and the player's inventory. At the core, the first layer (the foundation and tier 1) with
 * BUILD FIRST LAYER, live once everything is in; at the Cargo Exchange, the tower storage: the next tier's blocks, how
 * many of its 540 stacks the storage fills, and where to UPGRADE. The counts are the core's own (synced) data.
 */
public class TowerLoadScreen extends AbstractContainerScreen<TowerLoadMenu> {
	private static final int FRAME = 0xFF3A2422, GLASS = 0xF0141012, PANEL = 0xFF1E1A1C;
	private static final int ACCENT = 0xFFE05040, DIM = 0xFFC28E84, WHITE = 0xFFECE6E6, READY = 0xFF7FD28A, AMBER = 0xFFFFC04A;
	private static final int SLOT_EDGE = 0xFF3A2422, SLOT_IN = 0xFF0C0A0B;
	private static final int ROWS = 6, COL_W = 158, COL_STEP = 166, ROW_H = 20;
	private Button loadAll;
	private Button build;

	public TowerLoadScreen(TowerLoadMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title, TowerLoadMenu.WIDTH, TowerLoadMenu.HEIGHT);
	}

	@Override
	protected void init() {
		super.init();
		int y = topPos + TowerLoadMenu.INPUT_Y - 2;
		loadAll = addRenderableWidget(Button.builder(Component.translatable("screen.jugcraft.tower_load.load_all"), b -> press(TowerLoadMenu.LOAD_ALL))
				.bounds(leftPos + 128, y, 80, 20).build());
		if (!menu.storage) {
			build = addRenderableWidget(Button.builder(Component.translatable("screen.jugcraft.tower_load.build"), b -> press(TowerLoadMenu.BUILD))
					.bounds(leftPos + 212, y, 120, 20).build());
			build.active = false;
		}
	}

	private void press(int id) {
		if (minecraft != null && minecraft.gameMode != null) {
			minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
		}
	}

	/** The rows: each block needed and how many (the first layer's, or the next tier's at the Cargo Exchange). */
	private Map<Item, Integer> needed(TowerCoreBlockEntity core) {
		if (!menu.storage) {
			return core.firstLayerCost();
		}
		TowerData.Tier next = core.nextStep();
		if (next == null) {
			return Map.of();
		}
		Map<Item, Integer> sorted = new java.util.LinkedHashMap<>();
		core.layout().cost(next).entrySet().stream().sorted(Map.Entry.<Item, Integer>comparingByValue().reversed())
				.forEach(e -> sorted.put(e.getKey(), e.getValue()));
		return sorted;
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
		super.extractBackground(g, mouseX, mouseY, delta);
		int left = leftPos, top = topPos, w = imageWidth, h = imageHeight;
		g.fill(left - 2, top - 2, left + w + 2, top + h + 2, FRAME);
		g.fill(left, top, left + w, top + h, GLASS);
		for (int[] c : new int[][] {{left, top}, {left + w - 10, top}, {left, top + h - 2}, {left + w - 10, top + h - 2}}) {
			g.fill(c[0], c[1], c[0] + 10, c[1] + 2, ACCENT);
		}
		TowerCoreBlockEntity core = menu.tower();
		// the title, and what it is for on the right
		g.fill(left + 8, top + 6, left + 11, top + 15, ACCENT);
		g.text(font, Component.translatable(menu.storage ? "screen.jugcraft.tower_load.storage_heading" : "screen.jugcraft.tower_load.heading").getString(),
				left + 14, top + 7, ACCENT, false);
		String right = core == null ? "" : rightTitle(core);
		g.text(font, right, left + w - 8 - font.width(right), top + 7, DIM, false);
		// the rows
		long in = 0, total = 0;
		List<Map.Entry<Item, Integer>> rows = core == null ? List.of() : new ArrayList<>(needed(core).entrySet());
		for (int i = 0; i < rows.size() && i < ROWS * 2; i++) {
			Item item = rows.get(i).getKey();
			int need = rows.get(i).getValue();
			int got = core.stored(item);
			in += Math.min(need, got);
			total += need;
			boolean done = got >= need;
			int cx = left + 8 + (i / ROWS) * COL_STEP, y = top + 20 + (i % ROWS) * ROW_H;
			g.fill(cx, y, cx + COL_W, y + ROW_H - 1, PANEL);
			g.item(new ItemStack(item), cx + 2, y + 2);
			String name = fit(new ItemStack(item).getHoverName().getString(), COL_W - 23);
			g.text(font, name, cx + 21, y + 2, done ? READY : WHITE, false);
			int bx0 = cx + 21, bx1 = cx + 71;
			g.fill(bx0, y + 13, bx1, y + 16, SLOT_IN);
			g.fill(bx0, y + 13, bx0 + (int) Math.round((bx1 - bx0) * Math.min(1.0, got / (double) Math.max(1, need))), y + 16, done ? READY : ACCENT);
			String count = number(got) + " / " + number(need);
			g.text(font, count, cx + COL_W - 2 - font.width(count), y + 11, done ? READY : DIM, false);
		}
		if (core != null && rows.isEmpty()) {
			g.text(font, Component.translatable("screen.jugcraft.tower.complete").getString(), left + 10, top + 24, READY, false);
		}
		// the PUT BLOCKS IN slot and its total
		slot(g, left + TowerLoadMenu.INPUT_X - 1, top + TowerLoadMenu.INPUT_Y - 1);
		int ly = top + TowerLoadMenu.INPUT_Y - 1;
		g.text(font, Component.translatable("screen.jugcraft.tower_load.put_in").getString(), left + 30, ly + 1, DIM, false);
		if (menu.storage && core != null) {
			int used = core.storageStacksUsed();
			g.text(font, Component.translatable("screen.jugcraft.tower_load.stacks", used, TowerCoreBlockEntity.STORAGE_STACKS).getString(), left + 30, ly + 10,
					used >= TowerCoreBlockEntity.STORAGE_STACKS ? AMBER : WHITE, false);
			int hx = left + 214;
			for (String line : List.of(Component.translatable("screen.jugcraft.tower_load.upgrade_at").getString(),
					Component.translatable("screen.jugcraft.tower_load.upgrade_at2").getString())) {
				g.text(font, line, hx, ly, DIM, false);
				ly += 10;
			}
		} else {
			g.text(font, number(in) + " / " + number(total), left + 30, ly + 10, total > 0 && in >= total ? READY : WHITE, false);
		}
		if (build != null) {
			build.active = core != null && total > 0 && in >= total && !core.firstLayerQueued();
		}
		// the inventory
		g.text(font, Component.translatable("screen.jugcraft.tower_load.inventory").getString(), left + TowerLoadMenu.INV_X, top + TowerLoadMenu.INV_Y - 11,
				DIM, false);
		for (int r = 0; r < 3; r++) {
			for (int c = 0; c < 9; c++) {
				slot(g, left + TowerLoadMenu.INV_X - 1 + c * 18, top + TowerLoadMenu.INV_Y - 1 + r * 18);
			}
		}
		for (int c = 0; c < 9; c++) {
			slot(g, left + TowerLoadMenu.INV_X - 1 + c * 18, top + TowerLoadMenu.HOTBAR_Y - 1);
		}
	}

	private String rightTitle(TowerCoreBlockEntity core) {
		if (menu.storage) {
			TowerData.Tier next = core.nextStep();
			return next == null ? "" : Component.translatable("screen.jugcraft.tower_load.next_tier", next.number,
					Component.translatable("tower.jugcraft.tier." + next.number).getString().toUpperCase(Locale.ROOT)).getString();
		}
		boolean foundation = core.firstLayerSteps().size() > 1;
		return Component.translatable(foundation ? "screen.jugcraft.tower_load.foundation_and_tier1" : "screen.jugcraft.tower_load.tier1").getString();
	}

	private void slot(GuiGraphicsExtractor g, int x, int y) {
		g.fill(x, y, x + 18, y + 18, SLOT_EDGE);
		g.fill(x + 1, y + 1, x + 17, y + 17, SLOT_IN);
	}

	private String fit(String s, int width) {
		if (font.width(s) <= width) {
			return s;
		}
		while (!s.isEmpty() && font.width(s + "...") > width) {
			s = s.substring(0, s.length() - 1);
		}
		return s + "...";
	}

	private static String number(long n) {
		return String.format(Locale.ROOT, "%,d", n);
	}

	@Override
	protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
		// everything is drawn with the panel (extractBackground)
	}
}
