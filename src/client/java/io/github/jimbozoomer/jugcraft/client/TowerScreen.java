package io.github.jimbozoomer.jugcraft.client;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.tower.TowerCoreBlockEntity;
import io.github.jimbozoomer.jugcraft.tower.TowerCosts;
import io.github.jimbozoomer.jugcraft.tower.TowerData;
import io.github.jimbozoomer.jugcraft.tower.TowerUpgradePayload;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * The Drone Tower status screen: a picture of the tower as built so far (the next step as a dull red ghost), its tier
 * and what that unlocks, then the next step's materials: every block it places, how many of each the core holds and
 * how many it needs (scrolls), with how much of it is in. LOAD FROM INVENTORY puts in everything the tower still needs
 * from the player's inventory; UPGRADE DRONE TOWER pays the step and starts it. Opened from the Tower Core, or from
 * the depot terminal's TOWER button. Everything shown comes from the core's synced block entity; the server checks
 * both buttons again.
 */
public class TowerScreen extends Screen {
	private static final int WIDTH = 364;
	private static final int HEIGHT = 250;
	private static final int FRAME = 0xFF3A2422;
	private static final int GLASS = 0xF0141012;
	private static final int PANEL = 0xFF1E1A1C;
	private static final int ROW = 0xFF241F21;
	private static final int ACCENT = 0xFFE05040;
	private static final int DIM = 0xFFC28E84;
	private static final int WHITE = 0xFFECE6E6;
	private static final int AMBER = 0xFFFFC04A;
	private static final int READY = 0xFF7FD28A;
	/** Size of the tower pictures (tools/drone_tower.py screen_image), in pixels. */
	private static final int IMAGE_W = 118;
	private static final int IMAGE_H = 434;
	/** Blocks above y 0 the pictures cover (2 pixels per block). */
	private static final int IMAGE_BLOCKS = IMAGE_H / 2;
	/** The picture panel's width; the text panel starts after it. */
	private static final int PICTURE_W = 104;
	private static final int ROW_H = 18;

	private final BlockPos pos;
	private final BlockPos accessPos;
	private int left;
	private int top;
	private Button upgrade;
	private Button load;
	private int costScroll;
	/** Where the materials list is and how many rows fit (set while drawing, used by scrolling). */
	private int listTop, listRows;

	public TowerScreen(BlockPos pos) {
		this(pos, pos);
	}

	public TowerScreen(BlockPos pos, BlockPos accessPos) {
		super(Component.translatable("container.jugcraft.drone_tower"));
		this.pos = pos;
		this.accessPos = accessPos;
	}

	private TowerCoreBlockEntity core() {
		Minecraft minecraft = Minecraft.getInstance();
		return minecraft.level != null && minecraft.level.getBlockEntity(pos) instanceof TowerCoreBlockEntity core ? core : null;
	}

	private int textLeft() {
		return left + PICTURE_W + 14;
	}

	private int textRight() {
		return left + WIDTH - 12;
	}

	@Override
	protected void init() {
		left = (width - WIDTH) / 2;
		top = (height - HEIGHT) / 2;
		int x = textLeft() - 4;
		int w = (left + WIDTH - 8 - x - 4) / 2;
		load = addRenderableWidget(Button.builder(Component.translatable("screen.jugcraft.tower.load"),
				button -> ClientPlayNetworking.send(new TowerUpgradePayload(pos, accessPos, true))).bounds(x, top + HEIGHT - 28, w, 20)
				.tooltip(net.minecraft.client.gui.components.Tooltip.create(Component.translatable("screen.jugcraft.tower.load_hint"))).build());
		upgrade = addRenderableWidget(Button.builder(Component.translatable("screen.jugcraft.tower.upgrade"),
				button -> ClientPlayNetworking.send(new TowerUpgradePayload(pos, accessPos, false))).bounds(x + w + 4, top + HEIGHT - 28, w, 20).build());
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		super.extractBackground(graphics, mouseX, mouseY, delta);
		graphics.fill(left - 2, top - 2, left + WIDTH + 2, top + HEIGHT + 2, FRAME);
		graphics.fill(left, top, left + WIDTH, top + HEIGHT, GLASS);
		graphics.fill(left + 6, top + 22, left + 6 + PICTURE_W, top + HEIGHT - 6, PANEL);
		graphics.fill(left + PICTURE_W + 10, top + 22, left + WIDTH - 6, top + HEIGHT - 6, PANEL);
		for (int[] c : new int[][] {{left, top}, {left + WIDTH - 10, top}, {left, top + HEIGHT - 2}, {left + WIDTH - 10, top + HEIGHT - 2}}) {
			graphics.fill(c[0], c[1], c[0] + 10, c[1] + 2, ACCENT);
		}
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		super.extractRenderState(graphics, mouseX, mouseY, delta);
		graphics.text(font, "▌" + Component.translatable("screen.jugcraft.tower.title").getString(), left + 8, top + 7, ACCENT, false);
		TowerCoreBlockEntity core = core();
		listRows = 0;
		if (core == null) {
			upgrade.active = false;
			load.active = false;
			return;
		}
		int tier = core.tier();
		drawTower(graphics, core, tier);
		int x = textLeft();
		int y = top + 28;
		String name = Component.translatable("tower.jugcraft.tier." + tier).getString();
		y = say(graphics, Component.translatable("screen.jugcraft.tower.tier", tier, name.toUpperCase(Locale.ROOT)).getString(), x, y, WHITE);
		y = say(graphics, tier >= 1 ? Component.translatable("screen.jugcraft.tower.unlocked", tier, core.layout().capacity(tier)).getString()
				: Component.translatable("screen.jugcraft.tower.none_unlocked").getString(), x, y, DIM) + 5;
		if (core.converting() || core.buildingFoundation() || core.building() > 0) {
			int percent = Math.round(core.buildProgress() * 100);
			String line = core.converting() ? Component.translatable("screen.jugcraft.tower.converting", percent).getString()
					: core.buildingFoundation() ? Component.translatable("screen.jugcraft.tower.building_foundation", percent).getString()
					: Component.translatable("screen.jugcraft.tower.building", core.building(), percent).getString();
			y = say(graphics, line, x, y, AMBER) + 2;
			bar(graphics, x, y, percent, 100, ACCENT);
			y += 10;
			say(graphics, Component.translatable(core.converting() ? "screen.jugcraft.tower.converting_note"
					: core.buildingFoundation() || core.building() == 1 ? "screen.jugcraft.tower.building_core"
					: "screen.jugcraft.tower.building_drones").getString(), x, y, DIM);
			upgrade.active = false;
			load.active = true;
			return;
		}
		TowerData.Tier next = core.nextStep();
		if (next == null) {
			say(graphics, Component.translatable("screen.jugcraft.tower.complete").getString(), x, y, ACCENT);
			upgrade.active = false;
			load.active = false;
			return;
		}
		String step = core.needsFoundation()
				? Component.translatable("screen.jugcraft.tower.next_foundation",
						Component.translatable("tower.jugcraft.foundation").getString().toUpperCase(Locale.ROOT)).getString()
				: Component.translatable("screen.jugcraft.tower.next", tier + 1,
						Component.translatable("tower.jugcraft.tier." + (tier + 1)).getString().toUpperCase(Locale.ROOT)).getString();
		y = say(graphics, step, x, y, AMBER) + 2;

		// The materials: how much of the step is in, then one row per block.
		Map<Item, Integer> cost = core.layout().cost(next);
		List<Map.Entry<Item, Integer>> rows = new ArrayList<>(cost.entrySet());
		int kinds = 0, have = 0, need = TowerCosts.total(cost);
		for (var c : rows) {
			int held = Math.min(c.getValue(), core.stored(c.getKey()));
			have += held;
			if (held >= c.getValue()) {
				kinds++;
			}
		}
		boolean enough = kinds == rows.size();
		graphics.text(font, Component.translatable("screen.jugcraft.tower.materials", kinds, rows.size(), number(have), number(need)).getString(),
				x, y, enough ? READY : WHITE, false);
		y += 10;
		bar(graphics, x, y, have, need, enough ? READY : AMBER);
		y += 9;
		listTop = y;
		int listBottom = top + HEIGHT - 34;
		if (!core.firstLayer()) {
			// past the first layer the blocks wait in the tower storage, filled at the Cargo Exchange
			listBottom -= 10;
			int used = core.storageStacksUsed();
			String storage = Component.translatable("screen.jugcraft.tower.storage", used, TowerCoreBlockEntity.STORAGE_STACKS).getString();
			graphics.text(font, storage, x, top + HEIGHT - 41, used >= TowerCoreBlockEntity.STORAGE_STACKS ? AMBER : DIM, false);
		}
		listRows = Math.max(1, (listBottom - listTop) / ROW_H);
		costScroll = Math.max(0, Math.min(costScroll, Math.max(0, rows.size() - listRows)));
		for (int row = 0; row < listRows && row + costScroll < rows.size(); row++) {
			var c = rows.get(row + costScroll);
			int ry = listTop + row * ROW_H;
			if (row % 2 == 0) {
				graphics.fill(x - 2, ry, textRight() + 2, ry + ROW_H, ROW);
			}
			ItemStack stack = new ItemStack(c.getKey());
			graphics.item(stack, x, ry + 1);
			int held = core.stored(c.getKey());
			String count = number(Math.min(held, c.getValue())) + " / " + number(c.getValue());
			int countX = textRight() - font.width(count);
			String label = stack.getHoverName().getString();
			graphics.text(font, fit(label, Math.max(0, countX - (x + 20) - 6)), x + 20, ry + 5, WHITE, false);
			graphics.text(font, count, countX, ry + 5, held >= c.getValue() ? READY : AMBER, false);
			if (mouseX >= x - 2 && mouseX < textRight() + 2 && mouseY >= ry && mouseY < ry + ROW_H) {
				graphics.setTooltipForNextFrame(font, held >= c.getValue()
						? Component.translatable("screen.jugcraft.tower.row_ready", label)
						: Component.translatable("screen.jugcraft.tower.row_missing", label, number(c.getValue() - held)), mouseX, mouseY);
			}
		}
		if (rows.size() > listRows) {
			String more = Component.translatable("screen.jugcraft.tower.scroll", costScroll + 1, Math.min(rows.size(), costScroll + listRows),
					rows.size()).getString();
			graphics.text(font, more, textRight() - font.width(more), top + 23 - 10, DIM, false);
		}
		upgrade.active = enough;
		// after the first layer only a creative player loads here: everyone else uses the Cargo Exchange
		load.active = core.firstLayer() || minecraft != null && minecraft.player != null && minecraft.player.hasInfiniteMaterials();
	}

	/** The tower picture: the part from the ground to a little above the next step's top, scaled into the left panel. */
	private void drawTower(GuiGraphicsExtractor graphics, TowerCoreBlockEntity core, int tier) {
		TowerData.Tier next = core.nextStep();
		int showTop = next != null ? next.top + 6 : IMAGE_BLOCKS;
		showTop = Math.max(24, Math.min(IMAGE_BLOCKS, showTop));
		float v0 = 1f - (float) showTop / IMAGE_BLOCKS;
		List<net.minecraft.util.FormattedCharSequence> legend = font.split(Component.translatable("screen.jugcraft.tower.legend"), PICTURE_W - 6);
		int panelW = PICTURE_W - 4;
		int panelH = HEIGHT - 36 - legend.size() * 10;
		int imageWidth = core.layoutVersion() >= TowerData.FIRST_INDUSTRIAL_LAYOUT ? 142 : IMAGE_W;
		float aspect = (float) imageWidth / (showTop * 2f);
		int w = Math.min(panelW, Math.round(panelH * aspect));
		int h = Math.min(panelH, Math.round(w / aspect));
		int px = left + 8 + (panelW - w) / 2;
		// A low, wide picture (the control building alone) sits in the middle of the panel, not at its foot.
		int py = top + 26 + (panelH - h) / 2;
		String folder = TowerData.previewFolder(core.layoutVersion());
		// With a foundation: tier_0 shows the bare site (the foundation outlined), foundation.png the control building.
		String picture = tier == 0 && core.layout().foundation != null && !core.needsFoundation() ? "foundation"
				: "tier_" + Math.max(0, Math.min(9, tier));
		Identifier image = Jugcraft.id("textures/gui/" + folder + "/" + picture + ".png");
		graphics.blit(image, px, py, px + w, py + h, 0f, 1f, v0, 1f);
		int ly = top + HEIGHT - 8 - legend.size() * 10;
		for (var line : legend) {
			graphics.text(font, line, left + 9, ly, DIM, false);
			ly += 10;
		}
	}

	@Override
	public boolean mouseScrolled(double x, double y, double dx, double dy) {
		TowerCoreBlockEntity core = core();
		if (core != null && core.nextStep() != null && listRows > 0 && x >= textLeft() - 4 && x < left + WIDTH && y >= listTop
				&& y < listTop + listRows * ROW_H) {
			int max = Math.max(0, core.layout().cost(core.nextStep()).size() - listRows);
			costScroll = Math.max(0, Math.min(max, costScroll - (int) Math.signum(dy) * 3));
			return true;
		}
		return super.mouseScrolled(x, y, dx, dy);
	}

	/** Writes {@code text} across the text panel, wrapping it onto as many lines as it needs. Returns the y below it. */
	private int say(GuiGraphicsExtractor g, String text, int x, int y, int color) {
		for (var line : font.split(Component.literal(text), textRight() - x)) {
			g.text(font, line, x, y, color, false);
			y += 10;
		}
		return y;
	}

	private String fit(String text, int width) {
		if (font.width(text) <= width) {
			return text;
		}
		return font.plainSubstrByWidth(text, Math.max(0, width - font.width("…"))) + "…";
	}

	private static String number(int n) {
		return String.format(Locale.ROOT, "%,d", n);
	}

	private void bar(GuiGraphicsExtractor g, int x, int y, long value, long max, int color) {
		int w = textRight() - x;
		g.fill(x, y, x + w, y + 5, 0xFF0C0A0B);
		int filled = max <= 0 ? 0 : (int) Math.min(w, value * w / max);
		g.fill(x, y, x + filled, y + 5, color);
	}
}
