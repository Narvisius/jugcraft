package io.github.jimbozoomer.jugcraft.client;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.util.Map;
import java.util.WeakHashMap;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.SpriteIconButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

/**
 * The encyclopedia's way in from the inventory (Concordance Chunk A, A-04 and A-D8): a small button with the owner's
 * scroll sprite, right of the recipe-book button in the survival inventory, moving with it when the recipe book opens;
 * in the creative inventory, on the Survival Inventory tab only. Pressing it closes the inventory as Esc does (the
 * carried stack and the crafting grid go back) and opens the encyclopedia, whose Back opens a fresh inventory.
 * <p>
 * Added through Fabric's screen events, with no mixin: the recipe-book button (vanilla's only image button there) is
 * the anchor, read each frame, since the screen's own position is not public. Without it the button sits where the
 * anchor would. A screen's entry is dropped when the screen is removed: the anchor's action holds its screen, which
 * holds the player and the level, so a kept entry would keep them all.
 */
public final class EncyclopediaInventoryButton {
	public static final Identifier ICON = Jugcraft.id("encyclopedia/inventory_button");
	public static final int WIDTH = 20;
	public static final int HEIGHT = 18;
	public static final int GAP = 2;
	/** The survival inventory's panel, and where the recipe-book button stands in it (vanilla 26.3's InventoryScreen). */
	static final int SURVIVAL_WIDTH = 176;
	static final int SURVIVAL_HEIGHT = 166;
	static final int SURVIVAL_X = 104 + 20 + GAP;
	static final int SURVIVAL_Y = 61;
	/**
	 * The creative inventory's panel, and a free place on its Survival Inventory tab: right of the inventory grid, below
	 * the slots Trinkets puts at (146, 20) and the frame it draws round them when they open (to y 42), and above the
	 * destroy slot (173, 112).
	 */
	static final int CREATIVE_WIDTH = 195;
	static final int CREATIVE_HEIGHT = 136;
	static final int CREATIVE_X = 171;
	static final int CREATIVE_Y = 44;
	private static final Map<Screen, Placed> PLACED = new WeakHashMap<>();

	/** The button on one screen, and the recipe-book button it stands beside (null in the creative inventory). */
	private record Placed(AbstractWidget button, @Nullable AbstractWidget anchor) {
	}

	private EncyclopediaInventoryButton() {
	}

	static void register() {
		ScreenEvents.AFTER_INIT.register(EncyclopediaInventoryButton::afterInit);
	}

	private static void afterInit(Minecraft client, Screen screen, int width, int height) {
		// InventoryScreen hands a creative player straight on to the creative inventory: only the screen that stays counts.
		if (client.gui.screen() != screen || !(screen instanceof InventoryScreen || screen instanceof CreativeModeInventoryScreen)) {
			return;
		}
		AbstractWidget anchor = null;
		if (screen instanceof InventoryScreen) {
			for (AbstractWidget widget : Screens.getWidgets(screen)) {
				if (widget instanceof ImageButton) {
					anchor = widget;
					break;
				}
			}
		}
		Component tooltip = EncyclopediaClient.key() == null || EncyclopediaClient.key().isUnbound()
				? Component.translatable("screen.jugcraft.encyclopedia.open.tooltip_unbound")
				: Component.translatable("screen.jugcraft.encyclopedia.open.tooltip", EncyclopediaClient.key().getTranslatedKeyMessage());
		SpriteIconButton button = SpriteIconButton.builder(Component.translatable("screen.jugcraft.encyclopedia.open"),
				pressed -> EncyclopediaClient.openFromInventory(client), true).size(WIDTH, HEIGHT).sprite(ICON, 16, 16).tooltip(tooltip).build();
		Placed placed = new Placed(button, anchor);
		place(screen, placed);
		Screens.getWidgets(screen).add(button);
		PLACED.put(screen, placed);
		// The anchor's action holds the screen, so the entry would keep the screen, its player and their level alive.
		ScreenEvents.remove(screen).register(PLACED::remove);
		// The recipe book moves its button (and the panel) when it opens or closes: follow it every frame.
		ScreenEvents.beforeExtract(screen).register((shown, graphics, mouseX, mouseY, delta) -> place(shown, placed));
	}

	private static void place(Screen screen, Placed placed) {
		AbstractWidget button = placed.button();
		if (screen instanceof CreativeModeInventoryScreen creative) {
			button.setPosition((screen.width - CREATIVE_WIDTH) / 2 + CREATIVE_X, (screen.height - CREATIVE_HEIGHT) / 2 + CREATIVE_Y);
			button.visible = creative.isInventoryOpen();
			button.active = button.visible;
			return;
		}
		AbstractWidget anchor = placed.anchor();
		if (anchor != null) {
			button.setPosition(anchor.getX() + anchor.getWidth() + GAP, anchor.getY());
			button.visible = anchor.visible;
			button.active = anchor.visible;
			return;
		}
		button.setPosition((screen.width - SURVIVAL_WIDTH) / 2 + SURVIVAL_X, (screen.height - SURVIVAL_HEIGHT) / 2 + SURVIVAL_Y);
	}

	/** The encyclopedia button on {@code screen}, if it has one (tests press it). */
	public static @Nullable AbstractWidget button(Screen screen) {
		Placed placed = PLACED.get(screen);
		return placed == null ? null : placed.button();
	}

	/** The recipe-book button the encyclopedia button stands beside on {@code screen}, if it found one (tests check it). */
	public static @Nullable AbstractWidget anchor(Screen screen) {
		Placed placed = PLACED.get(screen);
		return placed == null ? null : placed.anchor();
	}
}
