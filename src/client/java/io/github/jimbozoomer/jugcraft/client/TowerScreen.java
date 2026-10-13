package io.github.jimbozoomer.jugcraft.client;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.tower.JugcraftTower;
import io.github.jimbozoomer.jugcraft.tower.TowerCoreBlockEntity;
import io.github.jimbozoomer.jugcraft.tower.TowerData;
import io.github.jimbozoomer.jugcraft.tower.TowerUpgradePayload;
import java.util.Arrays;
import java.util.Map;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/**
 * The Drone Tower status screen: a picture of the tower as built so far (the next tier as a dull red ghost),
 * its tier and what that unlocks, the modules the next tier costs (held / needed), the build progress, and
 * the UPGRADE DRONE TOWER button. Opened from the Tower Core, or from the depot terminal's TOWER button.
 * Everything shown comes from the core's synced block entity; the server checks the upgrade again.
 */
public class TowerScreen extends Screen {
	private static final int WIDTH = 316;
	private static final int HEIGHT = 234;
	private static final int FRAME = 0xFF3A2422;
	private static final int GLASS = 0xF0141012;
	private static final int PANEL = 0xFF1E1A1C;
	private static final int ACCENT = 0xFFE05040;
	private static final int DIM = 0xFFC28E84;
	private static final int WHITE = 0xFFECE6E6;
	private static final int AMBER = 0xFFFFC04A;
	/** Size of the tower pictures (tools/drone_tower.py screen_image), in pixels. */
	private static final int IMAGE_W = 118;
	private static final int IMAGE_H = 434;
	/** Blocks above y 0 the pictures cover (2 pixels per block). */
	private static final int IMAGE_BLOCKS = IMAGE_H / 2;

	private final BlockPos pos;
	private final BlockPos accessPos;
	private int left;
	private int top;
	private Button upgrade;
	private int costScroll;
	private static final int COST_ROWS = 4;

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

	@Override
	protected void init() {
		left = (width - WIDTH) / 2;
		top = (height - HEIGHT) / 2;
		upgrade = addRenderableWidget(Button.builder(Component.translatable("screen.jugcraft.tower.upgrade"),
				button -> ClientPlayNetworking.send(new TowerUpgradePayload(pos, accessPos))).bounds(left + 108, top + HEIGHT - 30, 196, 20).build());
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
		graphics.fill(left + 6, top + 22, left + 98, top + HEIGHT - 6, PANEL);
		graphics.fill(left + 102, top + 22, left + WIDTH - 6, top + HEIGHT - 6, PANEL);
		for (int[] c : new int[][] {{left, top}, {left + WIDTH - 10, top}, {left, top + HEIGHT - 2}, {left + WIDTH - 10, top + HEIGHT - 2}}) {
			graphics.fill(c[0], c[1], c[0] + 10, c[1] + 2, ACCENT);
		}
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		super.extractRenderState(graphics, mouseX, mouseY, delta);
		graphics.text(font, "▌" + Component.translatable("screen.jugcraft.tower.title").getString(), left + 8, top + 7, ACCENT, false);
		TowerCoreBlockEntity core = core();
		if (core == null) {
			upgrade.active = false;
			return;
		}
		int tier = core.tier();
		drawTower(graphics, core, tier);
		int x = left + 110;
		int y = top + 28;
		String name = Component.translatable("tower.jugcraft.tier." + tier).getString();
		say(graphics, Component.translatable("screen.jugcraft.tower.tier", tier, name.toUpperCase(java.util.Locale.ROOT)).getString(), x, y, WHITE);
		y += 12;
		say(graphics, tier >= 1 ? Component.translatable("screen.jugcraft.tower.unlocked", tier, core.layout().capacity(tier)).getString()
				: Component.translatable("screen.jugcraft.tower.none_unlocked").getString(), x, y, DIM);
		y += 16;
		if (core.building() > 0) {
			int percent = Math.round(core.buildProgress() * 100);
			say(graphics, Component.translatable("screen.jugcraft.tower.building", core.building(), percent).getString(), x, y, AMBER);
			y += 12;
			bar(graphics, x, y, percent, 100, ACCENT);
			y += 10;
			say(graphics, Component.translatable(core.building() == 1 ? "screen.jugcraft.tower.building_core"
					: "screen.jugcraft.tower.building_drones").getString(), x, y, DIM);
			upgrade.active = false;
			return;
		}
		if (tier >= TowerData.TIERS) {
			say(graphics, Component.translatable("screen.jugcraft.tower.complete").getString(), x, y, ACCENT);
			upgrade.active = false;
			return;
		}
		TowerData.Tier next = core.layout().tier(tier + 1);
		say(graphics, Component.translatable("screen.jugcraft.tower.next", tier + 1,
				Component.translatable("tower.jugcraft.tier." + (tier + 1)).getString().toUpperCase(java.util.Locale.ROOT)).getString(), x, y, AMBER);
		y += 14;
		boolean enough = true;
		var costs = new java.util.ArrayList<>(next.modules.entrySet());
		for (var cost : costs) {
			int index = Arrays.asList(JugcraftTower.MODULES).indexOf(cost.getKey());
			enough &= index >= 0 && core.modules(index) >= cost.getValue();
		}
		costScroll = Math.max(0, Math.min(costScroll, Math.max(0, costs.size() - COST_ROWS)));
		for (int row = 0; row < COST_ROWS && row + costScroll < costs.size(); row++) {
			var cost = costs.get(row + costScroll);
			int index = Arrays.asList(JugcraftTower.MODULES).indexOf(cost.getKey());
			int have = index < 0 ? 0 : core.modules(index);
			var item = JugcraftTower.MODULE_ITEMS.get(cost.getKey());
			var stack = item == null ? net.minecraft.world.item.ItemStack.EMPTY : new net.minecraft.world.item.ItemStack(item);
			graphics.item(stack, x, y);
			String label = stack.getHoverName().getString();
			String count = have + "/" + cost.getValue();
			int countX = left + WIDTH - 12 - font.width(count);
			graphics.text(font, fit(label, Math.max(0, countX - (x + 20) - 6)), x + 20, y + 4, WHITE, false);
			graphics.text(font, count, countX, y + 4, have >= cost.getValue() ? ACCENT : AMBER, false);
			if (mouseX >= x && mouseX < left + WIDTH - 10 && mouseY >= y && mouseY < y + 18) {
				graphics.setTooltipForNextFrame(font, Component.literal(label + " - missing " + Math.max(0, cost.getValue() - have)), mouseX, mouseY);
			}
			y += 18;
		}
		if (costs.size() > COST_ROWS) {
			graphics.text(font, "SCROLL " + (costScroll + 1) + "-" + Math.min(costs.size(), costScroll + COST_ROWS) + "/" + costs.size(), x, top + 181, DIM, false);
		}
		y = top + 177;
		if (tier == 0 && !core.plinthComplete()) {
			say(graphics, Component.translatable("screen.jugcraft.tower.plinth_missing").getString(), x, y, AMBER);
			enough = false;
		}
		y = top + 190;
		say(graphics, Component.translatable("screen.jugcraft.tower.hint").getString(), x, y, DIM);
		upgrade.active = enough;
	}

	/** The tower picture: the part from the ground to a little above the next tier's top, scaled into the left panel. */
	private void drawTower(GuiGraphicsExtractor graphics, TowerCoreBlockEntity core, int tier) {
		int showTop = tier < TowerData.TIERS ? core.layout().tier(tier + 1).top + 6 : IMAGE_BLOCKS;
		showTop = Math.max(24, Math.min(IMAGE_BLOCKS, showTop));
		float v0 = 1f - (float) showTop / IMAGE_BLOCKS;
		int panelW = 88;
		int panelH = HEIGHT - 48;
		int imageWidth = core.layoutVersion() >= TowerData.FIRST_INDUSTRIAL_LAYOUT ? 142 : IMAGE_W;
		float aspect = (float) imageWidth / (showTop * 2f);
		int w = Math.min(panelW, Math.round(panelH * aspect));
		int h = Math.min(panelH, Math.round(w / aspect));
		int px = left + 8 + (panelW - w) / 2;
		int py = top + 24 + panelH - h;
		String folder = TowerData.previewFolder(core.layoutVersion());
		Identifier image = Jugcraft.id("textures/gui/" + folder + "/tier_" + Math.max(0, Math.min(9, tier)) + ".png");
		graphics.blit(image, px, py, px + w, py + h, 0f, 1f, v0, 1f);
		graphics.text(font, fit(Component.translatable("screen.jugcraft.tower.legend").getString(), 86), left + 9, top + HEIGHT - 16, DIM, false);
	}

	@Override
	public boolean mouseScrolled(double x, double y, double dx, double dy) {
		TowerCoreBlockEntity core = core();
		if (core != null && core.tier() < TowerData.TIERS && x >= left + 102 && x < left + WIDTH && y >= top + 76 && y < top + 191) {
			int max = Math.max(0, core.layout().tier(core.tier() + 1).modules.size() - COST_ROWS);
			costScroll = Math.max(0, Math.min(max, costScroll - (int) Math.signum(dy)));
			return true;
		}
		return super.mouseScrolled(x, y, dx, dy);
	}

	private void say(GuiGraphicsExtractor g, String text, int x, int y, int color) {
		g.text(font, fit(text, left + WIDTH - 12 - x), x, y, color, false);
	}

	private String fit(String text, int width) {
		if (font.width(text) <= width) {
			return text;
		}
		return font.plainSubstrByWidth(text, Math.max(0, width - font.width("…"))) + "…";
	}

	private void bar(GuiGraphicsExtractor g, int x, int y, long value, long max, int color) {
		int w = left + WIDTH - 12 - x;
		g.fill(x, y, x + w, y + 6, 0xFF0C0A0B);
		int filled = max <= 0 ? 0 : (int) Math.min(w, value * w / max);
		g.fill(x, y, x + filled, y + 6, color);
	}
}
