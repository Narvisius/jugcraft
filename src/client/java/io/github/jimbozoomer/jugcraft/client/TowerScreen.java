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
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/**
 * The Drone Tower status screen, in the drone look (docs/features/machine-screens.md): a picture of the tower as built
 * so far (the next tier as a dull red ghost) in its own pane; beside it its tier and what that unlocks, the modules the
 * next tier costs (held / needed, short ones in amber), the build progress, and the UPGRADE DRONE TOWER key, lit only
 * when it can be pressed. The status lamp is amber while building and green when the next tier is ready to build.
 * Opened from the Tower Core, or from the depot terminal's TOWER key. Everything shown comes from the core's synced
 * block entity; the server checks the upgrade again.
 */
public class TowerScreen extends Screen {
	private static final int WIDTH = 300;
	private static final int HEIGHT = 190;
	private static final int AMBER = 0xFFFFC04A;
	/** Size of the tower pictures (tools/drone_tower.py screen_image), in pixels. */
	private static final int IMAGE_W = 118;
	private static final int IMAGE_H = 434;
	/** Blocks above y 0 the pictures cover (2 pixels per block). */
	private static final int IMAGE_BLOCKS = IMAGE_H / 2;

	private final BlockPos pos;
	private final MachineScreenThemes.Theme theme = MachineScreenThemes.screen("drone_tower");
	private int left;
	private int top;
	private ScreenKit.TextButton upgrade;

	public TowerScreen(BlockPos pos) {
		super(Component.translatable("container.jugcraft.drone_tower"));
		this.pos = pos;
	}

	private TowerCoreBlockEntity core() {
		Minecraft minecraft = Minecraft.getInstance();
		return minecraft.level != null && minecraft.level.getBlockEntity(pos) instanceof TowerCoreBlockEntity core ? core : null;
	}

	@Override
	protected void init() {
		left = (width - WIDTH) / 2;
		top = (height - HEIGHT) / 2;
		upgrade = addRenderableWidget(new ScreenKit.TextButton(left + 108, top + HEIGHT - 30, 180, 20, theme,
				Component.translatable("screen.jugcraft.tower.upgrade"), button -> ClientPlayNetworking.send(new TowerUpgradePayload(pos))));
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		super.extractBackground(graphics, mouseX, mouseY, delta);
		ScreenKit.body(graphics, theme, left, top, WIDTH, HEIGHT);
		ScreenKit.pane(graphics, theme, left + 6, top + 22, 92, HEIGHT - 28);
		ScreenKit.pane(graphics, theme, left + 102, top + 22, WIDTH - 108, HEIGHT - 28);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		TowerCoreBlockEntity core = core();
		// The key is only lit when it can be pressed; the lamp says why not.
		int lamp = theme.dim();
		if (core == null) {
			upgrade.active = false;
		} else if (core.building() > 0) {
			upgrade.active = false;
			lamp = AMBER;
		} else if (core.tier() >= TowerData.TIERS) {
			upgrade.active = false;
			lamp = theme.ok();
		} else {
			upgrade.active = canUpgrade(core);
			lamp = upgrade.active ? theme.ok() : theme.dim();
		}
		super.extractRenderState(graphics, mouseX, mouseY, delta);
		ScreenKit.title(graphics, font, theme, left + 4, top + 5, Component.translatable("screen.jugcraft.tower.title"), lamp, WIDTH - 8);
		if (core == null) {
			return;
		}
		int tier = core.tier();
		drawTower(graphics, tier);
		int x = left + 110;
		int y = top + 28;
		String name = Component.translatable("tower.jugcraft.tier." + tier).getString();
		say(graphics, Component.translatable("screen.jugcraft.tower.tier", tier, name.toUpperCase(java.util.Locale.ROOT)).getString(), x, y, theme.text());
		y += 12;
		say(graphics, tier >= 1 ? Component.translatable("screen.jugcraft.tower.unlocked", tier, TowerData.get().capacity(tier)).getString()
				: Component.translatable("screen.jugcraft.tower.none_unlocked").getString(), x, y, theme.dim());
		y += 16;
		if (core.building() > 0) {
			int percent = Math.round(core.buildProgress() * 100);
			say(graphics, Component.translatable("screen.jugcraft.tower.building", core.building(), percent).getString(), x, y, AMBER);
			y += 12;
			ScreenKit.bar(graphics, theme, x, y, left + WIDTH - 12 - x, 6, percent / 100.0, theme.energy());
			y += 10;
			say(graphics, Component.translatable(core.building() == 1 ? "screen.jugcraft.tower.building_core"
					: "screen.jugcraft.tower.building_drones").getString(), x, y, theme.dim());
			return;
		}
		if (tier >= TowerData.TIERS) {
			say(graphics, Component.translatable("screen.jugcraft.tower.complete").getString(), x, y, theme.label());
			return;
		}
		TowerData.Tier next = TowerData.get().tier(tier + 1);
		say(graphics, Component.translatable("screen.jugcraft.tower.next", tier + 1,
				Component.translatable("tower.jugcraft.tier." + (tier + 1)).getString().toUpperCase(java.util.Locale.ROOT)).getString(), x, y, AMBER);
		y += 14;
		for (Map.Entry<String, Integer> cost : next.modules.entrySet()) {
			int have = have(core, cost.getKey());
			boolean ok = have >= cost.getValue();
			String label = Component.translatable("item.jugcraft." + cost.getKey()).getString();
			graphics.text(font, ScreenKit.fit(font, label, 110), x, y, theme.text(), false);
			String count = have + "/" + cost.getValue();
			graphics.text(font, count, left + WIDTH - 12 - font.width(count), y, ok ? theme.ok() : AMBER, false);
			y += 11;
		}
		y += 4;
		if (tier == 0 && !core.plinthComplete()) {
			say(graphics, Component.translatable("screen.jugcraft.tower.plinth_missing").getString(), x, y, AMBER);
			y += 11;
		}
		MachineSkin.wrap(graphics, font, Component.translatable("screen.jugcraft.tower.hint"), x, y, left + WIDTH - 12 - x, upgrade.getY() - 2,
				theme.dim());
	}

	/** How many of a module the core holds. */
	private static int have(TowerCoreBlockEntity core, String module) {
		int index = Arrays.asList(JugcraftTower.MODULES).indexOf(module);
		return index < 0 ? 0 : core.modules(index);
	}

	/** Whether the core holds every module the next tier costs (and, for the first tier, its plinth is built). */
	private static boolean canUpgrade(TowerCoreBlockEntity core) {
		int tier = core.tier();
		if (tier == 0 && !core.plinthComplete()) {
			return false;
		}
		for (Map.Entry<String, Integer> cost : TowerData.get().tier(tier + 1).modules.entrySet()) {
			if (have(core, cost.getKey()) < cost.getValue()) {
				return false;
			}
		}
		return true;
	}

	/** The tower picture: the part from the ground to a little above the next tier's top, scaled into the left pane. */
	private void drawTower(GuiGraphicsExtractor graphics, int tier) {
		int showTop = tier < TowerData.TIERS ? TowerData.get().tier(tier + 1).top + 6 : IMAGE_BLOCKS;
		showTop = Math.max(24, Math.min(IMAGE_BLOCKS, showTop));
		float v0 = 1f - (float) showTop / IMAGE_BLOCKS;
		int panelW = 88;
		int panelH = HEIGHT - 56;
		float aspect = (float) IMAGE_W / (showTop * 2f);
		int w = Math.min(panelW, Math.round(panelH * aspect));
		int h = Math.min(panelH, Math.round(w / aspect));
		int px = left + 8 + (panelW - w) / 2;
		int py = top + 24 + panelH - h;
		Identifier image = Jugcraft.id("textures/gui/drone_tower/tier_" + Math.max(0, Math.min(9, tier)) + ".png");
		graphics.blit(image, px, py, px + w, py + h, 0f, 1f, v0, 1f);
		// The legend, on two lines under the picture.
		MachineSkin.wrap(graphics, font, Component.translatable("screen.jugcraft.tower.legend"), left + 9, top + HEIGHT - 26, 86, top + HEIGHT - 7,
				theme.dim());
	}

	private void say(GuiGraphicsExtractor g, String text, int x, int y, int color) {
		g.text(font, ScreenKit.fit(font, text, left + WIDTH - 12 - x), x, y, color, false);
	}
}
