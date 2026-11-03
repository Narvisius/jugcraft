package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.jimbozoomer.jugcraft.pets.Pets;
import java.util.List;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import org.jspecify.annotations.Nullable;

/**
 * The companion stable (docs/features/pet-roster.md): every companion the server lists for this player, in a framed
 * wood-and-leather stable with paw marks. A scrolling roster of cards (a small portrait, the name, the level and a
 * coloured status: deployed, in the stable, knocked out with the time it still needs) beside the chosen companion's page
 * (a large portrait that turns to the mouse, its kind, level and bond, health, the four behaviours and one big Deploy or
 * Recall key). Everything is chosen by clicking or the keys; nothing needs typing.
 *
 * <p>The server owns the roster: this screen only shows the rows it sent ({@link Pets.Snapshot}) and asks it to act
 * ({@link Pets.Action}); ownership, cooldowns, knockout recovery and the exact stored entity are the server's checks,
 * so a refused action comes back as its message. Rows arrive 200 at a time; the ends of the list fetch more.
 *
 * <p>Keys: Up and Down (Page Up/Down, Home/End) pick a companion, Enter deploys or recalls it, 1 to 4 set its behaviour,
 * R refreshes, Tab and Space move to and press the other keys, Esc closes. Tooltips explain every status and key. It
 * fits the smallest GUI (320 x 240).
 */
public final class PetInventoryScreen extends Screen implements ScreenKit.Fitted {
	// ------------------------------------------------------------------ the look: warm wood, leather, parchment
	private static final int WOOD_DARK = 0xFF3A2414, WOOD = 0xFF6E4527, WOOD_LIGHT = 0xFFA4744A, WOOD_GRAIN = 0xFF5E3A20;
	private static final int LEATHER = 0xFF4E3220, LEATHER_DARK = 0xFF3A2416, STITCH = 0xFFC9A574;
	private static final int PARCHMENT = 0xFFEAD7AE, PARCHMENT_EDGE = 0xFFB8986A, INK = 0xFF3A2816, INK_SOFT = 0xFF6B5134;
	private static final int CREAM = 0xFFF3E4C4, CREAM_SOFT = 0xFFD6BF97, BRASS = 0xFFD9B060;
	private static final int DEPLOYED = 0xFF4C8A3C, STABLED = 0xFFB27A2C, KNOCKED_OUT = 0xFFA8382C;
	private static final int HEART = 0xFFD8473C, BOND = 0xFF5FA8C8, TRACK = 0xFF2A1A0E;
	/** Card height in the roster, and the smallest panel (the smallest GUI less a margin). */
	private static final int ROW = 26, MIN_W = 312, MIN_H = 228;
	private static final String[] MODE_LABELS = {"Follow", "Stay", "Defend", "Passive"};
	private static final String[] MODE_HELP = {
			"Follow: keeps close, catches up when left behind (never into unloaded land or another's claim).",
			"Stay: sits or holds its place until you choose another behaviour.",
			"Defend: turns on monsters threatening you or it; never on players or anyone's pets.",
			"Passive: never starts or answers a fight; it still flees from danger."};

	private Pets.Snapshot data;
	/** When this snapshot arrived, to count a knocked-out companion's recovery down between updates. */
	private long receivedAt = System.currentTimeMillis();
	private int selected;
	private @Nullable String selectedId;
	private float scroll, shownScroll;
	private int left, top, panelW, panelH, listX, listY, listW, listH, detailX, detailW;
	private @Nullable StableButton action;
	/** The four behaviour keys (none while the stable is empty). */
	private final @Nullable StableButton[] modeKeys = new StableButton[4];
	/** The big key's label as last set, so it is only updated when it changes. */
	private @Nullable String actionShown;

	public PetInventoryScreen(Pets.Snapshot data) {
		super(Component.literal("Companion Stable"));
		this.data = data;
	}

	public static void register() {
		ClientPlayNetworking.registerGlobalReceiver(Pets.View.TYPE, (packet, context) -> {
			var mc = Minecraft.getInstance();
			var data = Pets.JSON.fromJson(packet.json(), Pets.Snapshot.class);
			if (mc.gui.screen() instanceof PetInventoryScreen screen) {
				screen.update(data);
			} else {
				mc.gui.setScreen(new PetInventoryScreen(data));
			}
		});
		ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
			if (screen instanceof InventoryScreen || screen instanceof CreativeModeInventoryScreen) {
				Button open = Button.builder(Component.literal("Pets"), b -> ClientPlayNetworking.send(new Pets.Action("", "open")))
						.bounds(4, 4, 42, 18).build();
				open.setTooltip(Tooltip.create(Component.literal("Open your companion stable")));
				Screens.getWidgets(screen).add(open);
			}
		});
	}

	/**
	 * A new snapshot from the server: keeps the same companion chosen when it is still listed. The keys are updated in
	 * place (rebuilding them would move the keyboard focus); only an empty roster becoming filled, or the reverse,
	 * rebuilds them.
	 */
	private void update(Pets.Snapshot next) {
		String keep = selectedId;
		boolean wasEmpty = rows().isEmpty();
		data = next;
		receivedAt = System.currentTimeMillis();
		selected = 0;
		for (int i = 0; keep != null && i < rows().size(); i++) {
			if (rows().get(i).id().equals(keep)) {
				selected = i;
			}
		}
		if (wasEmpty != rows().isEmpty() || modeKeys[0] == null && !rows().isEmpty()) {
			rebuildWidgets();
		} else {
			chosenChanged();
		}
	}

	private List<Pets.Row> rows() {
		return data.pets();
	}

	private Pets.@Nullable Row chosen() {
		return rows().isEmpty() ? null : rows().get(Mth.clamp(selected, 0, rows().size() - 1));
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public ScreenRectangle panelArea() {
		return new ScreenRectangle(left, top, panelW, panelH);
	}

	// ------------------------------------------------------------------ layout and keys

	@Override
	protected void init() {
		panelW = Math.min(440, Math.max(Math.min(MIN_W, width - 8), width - 24));
		panelH = Math.min(282, Math.max(Math.min(MIN_H, height - 8), height - 24));
		left = (width - panelW) / 2;
		top = (height - panelH) / 2;
		listX = left + 9;
		listY = top + 30;
		listW = Mth.clamp(panelW * 2 / 5, 118, 172);
		listH = panelH - 30 - 46;
		detailX = listX + listW + 8;
		detailW = left + panelW - 9 - detailX;
		selected = Mth.clamp(selected, 0, Math.max(0, rows().size() - 1));
		Pets.Row pet = chosen();
		keepVisible();
		shownScroll = scroll;

		action = null;
		java.util.Arrays.fill(modeKeys, null);
		if (pet != null) {
			// The behaviour keys and the big action key along the bottom of the companion's page; added first, so Tab
			// reaches them before Refresh and Done.
			int modeY = top + panelH - 46 - 44;
			int keyW = (detailW - 3 * 3) / 4;
			for (int i = 0; i < 4; i++) {
				String mode = Pets.MODES.get(i);
				modeKeys[i] = addRenderableWidget(new StableButton(detailX + i * (keyW + 3), modeY, keyW, 18, Component.literal(MODE_LABELS[i]),
						b -> send(mode), false));
			}
			action = addRenderableWidget(new StableButton(detailX, modeY + 22, detailW, 20, actionLabel(pet), b -> primary(), true));
		}
		int footY = top + panelH - 26;
		StableButton refresh = addRenderableWidget(new StableButton(left + 9, footY, 60, 18, Component.literal("Refresh"), b -> send("open"), false));
		refresh.setTooltip(Tooltip.create(Component.literal("Ask the server for your companions again (R)")));
		StableButton done = addRenderableWidget(new StableButton(left + panelW - 9 - 56, footY, 56, 18, Component.literal("Done"), b -> onClose(), false));
		done.setTooltip(Tooltip.create(Component.literal("Close the stable (Esc)")));
		if (pet == null) {
			selectedId = null;
			return;
		}
		chosenChanged();
	}

	/** Sets the keys for the companion now chosen: its behaviour lit, the big key's label, the tooltips. */
	private void chosenChanged() {
		selected = Mth.clamp(selected, 0, Math.max(0, rows().size() - 1));
		Pets.Row pet = chosen();
		selectedId = pet == null ? null : pet.id();
		keepVisible();
		if (pet == null) {
			return;
		}
		for (int i = 0; i < 4; i++) {
			StableButton key = modeKeys[i];
			if (key == null) {
				continue;
			}
			key.selected = Pets.MODES.get(i).equals(pet.mode());
			key.active = !pet.ownOrders();
			key.setTooltip(Tooltip.create(Component.literal(pet.ownOrders()
					? "This companion keeps its own Orders menu; its level bonuses still apply here."
					: MODE_HELP[i] + "  (key " + (i + 1) + ")")));
		}
		actionShown = null;
		refreshAction();
	}

	/** What the big key does for this companion now. */
	private Component actionLabel(Pets.Row pet) {
		if (pet.state().equals("ACTIVE")) {
			return Component.literal("Recall to stable");
		}
		int wait = recoveryLeft(pet);
		return Component.literal(wait > 0 ? "Recovering  " + clock(wait) : "Deploy");
	}

	/** Keeps the big key's label and state current (a recovery counts down between server updates). */
	private void refreshAction() {
		Pets.Row pet = chosen();
		if (action == null || pet == null) {
			return;
		}
		Component label = actionLabel(pet);
		if (label.getString().equals(actionShown)) {
			return;   // unchanged since the last frame
		}
		actionShown = label.getString();
		action.setMessage(label);
		action.active = pet.state().equals("ACTIVE") || recoveryLeft(pet) == 0;
		action.setTooltip(Tooltip.create(Component.literal(pet.state().equals("ACTIVE")
				? "Send " + pet.name() + " back to the stable: the very same companion, safe until you deploy it again. (Enter)"
				: recoveryLeft(pet) > 0 ? pet.name() + " was knocked out and is recovering; it can be deployed when the minute is up."
				: "Bring " + pet.name() + " out beside you, on clear ground (or water, for swimmers). (Enter)")));
	}

	private void primary() {
		Pets.Row pet = chosen();
		if (pet != null && (pet.state().equals("ACTIVE") || recoveryLeft(pet) == 0)) {
			send(pet.state().equals("ACTIVE") ? "recall" : "summon");
		}
	}

	private void send(String operation) {
		Pets.Row pet = chosen();
		ClientPlayNetworking.send(new Pets.Action(pet == null ? "" : pet.id(), operation));
	}

	private int recoveryLeft(Pets.Row pet) {
		if (!pet.state().equals("KNOCKED_OUT") || pet.recovery() <= 0) {
			return 0;
		}
		return Math.max(0, pet.recovery() - (int) ((System.currentTimeMillis() - receivedAt) / 1000L));
	}

	private static String clock(int seconds) {
		return seconds / 60 + ":" + String.format("%02d", seconds % 60);
	}

	/** The roster's rows: the companions, with an extra row at either end when the server has more to send. */
	private boolean moreBefore() {
		return data.offset() > 0;
	}

	private boolean moreAfter() {
		return data.offset() + rows().size() < data.total();
	}

	private int rowCount() {
		return rows().size() + (moreBefore() ? 1 : 0) + (moreAfter() ? 1 : 0);
	}

	private int visibleRows() {
		return Math.max(1, listH / ROW);
	}

	private float maxScroll() {
		return Math.max(0, rowCount() * ROW - listH);
	}

	private void keepVisible() {
		int y = (selected + (moreBefore() ? 1 : 0)) * ROW;
		if (y < scroll) {
			scroll = y;
		} else if (y + ROW > scroll + listH) {
			scroll = y + ROW - listH;
		}
		scroll = Mth.clamp(scroll, 0, maxScroll());
	}

	private void select(int index) {
		if (rows().isEmpty()) {
			return;
		}
		int next = Mth.clamp(index, 0, rows().size() - 1);
		if (next != selected) {
			selected = next;
			chosenChanged();
		}
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		int key = event.input();
		if (key == InputConstants.KEY_UP) {
			select(selected - 1);
		} else if (key == InputConstants.KEY_DOWN) {
			select(selected + 1);
		} else if (key == InputConstants.KEY_PAGEUP) {
			select(selected - visibleRows());
		} else if (key == InputConstants.KEY_PAGEDOWN) {
			select(selected + visibleRows());
		} else if (key == InputConstants.KEY_HOME) {
			select(0);
		} else if (key == InputConstants.KEY_END) {
			select(rows().size() - 1);
		} else if (key == InputConstants.KEY_RETURN || key == InputConstants.KEY_NUMPADENTER) {
			primary();   // Enter always acts on the chosen companion; Space presses the highlighted key
		} else if (key >= InputConstants.KEY_1 && key <= InputConstants.KEY_4 && chosen() != null && !chosen().ownOrders()) {
			send(Pets.MODES.get(key - InputConstants.KEY_1));
		} else if (key == InputConstants.KEY_R) {
			send("open");
		} else {
			return super.keyPressed(event);
		}
		return true;
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		if (mouseX >= listX && mouseX < listX + listW && mouseY >= listY && mouseY < listY + listH) {
			scroll = Mth.clamp(scroll - (float) scrollY * ROW, 0, maxScroll());
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (super.mouseClicked(event, doubleClick)) {
			return true;
		}
		double mx = event.x(), my = event.y();
		if (event.button() != InputConstants.MOUSE_BUTTON_LEFT || mx < listX || mx >= listX + listW - 5 || my < listY || my >= listY + listH) {
			return false;
		}
		int row = (int) ((my - listY + shownScroll) / ROW);
		if (moreBefore() && row == 0) {
			ClientPlayNetworking.send(new Pets.Action(Integer.toString(Math.max(0, data.offset() - 200)), "page"));
			return true;
		}
		int index = row - (moreBefore() ? 1 : 0);
		if (index >= rows().size()) {
			if (moreAfter() && index == rows().size()) {
				ClientPlayNetworking.send(new Pets.Action(Integer.toString(data.offset() + 200), "page"));
			}
			return true;
		}
		if (index >= 0) {
			select(index);
			if (doubleClick) {
				primary();
			}
		}
		return true;
	}

	// ------------------------------------------------------------------ drawing

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
		shownScroll = Mth.lerp(Math.min(1.0F, delta * 0.6F), shownScroll, scroll);
		if (Math.abs(shownScroll - scroll) < 0.5F) {
			shownScroll = scroll;
		}
		refreshAction();
		frame(g);
		header(g);
		list(g, mx, my);
		Pets.Row pet = chosen();
		if (pet == null) {
			empty(g);
		} else {
			details(g, pet, mx, my);
		}
		message(g, mx, my);
		super.extractRenderState(g, mx, my, delta);
	}

	/** The stable's frame: a dark wood border with a lighter bevel and grain, round a stitched leather panel. */
	private void frame(GuiGraphicsExtractor g) {
		int x0 = left, y0 = top, x1 = left + panelW, y1 = top + panelH;
		g.fill(x0 - 3, y0 - 3, x1 + 3, y1 + 3, WOOD_DARK);
		g.fill(x0 - 2, y0 - 2, x1 + 2, y1 + 2, WOOD);
		for (int gy = y0 - 1; gy < y1 + 2; gy += 5) {   // grain along the boards
			g.fill(x0 - 2, gy, x0 + 4, gy + 1, WOOD_GRAIN);
			g.fill(x1 - 4, gy + 2, x1 + 2, gy + 3, WOOD_GRAIN);
		}
		ScreenKit.bevel(g, x0 - 2, y0 - 2, panelW + 4, panelH + 4, WOOD_LIGHT, WOOD_DARK);
		g.fill(x0 + 4, y0 + 4, x1 - 4, y1 - 4, LEATHER_DARK);
		g.fill(x0 + 5, y0 + 5, x1 - 5, y1 - 5, LEATHER);
		// Stitching just inside the leather's edge.
		for (int x = x0 + 8; x < x1 - 8; x += 6) {
			g.fill(x, y0 + 7, x + 3, y0 + 8, STITCH);
			g.fill(x, y1 - 8, x + 3, y1 - 7, STITCH);
		}
		for (int y = y0 + 10; y < y1 - 10; y += 6) {
			g.fill(x0 + 7, y, x0 + 8, y + 3, STITCH);
			g.fill(x1 - 8, y, x1 - 7, y + 3, STITCH);
		}
		// Brass corner studs.
		for (int[] c : new int[][] {{x0 - 1, y0 - 1}, {x1 - 3, y0 - 1}, {x0 - 1, y1 - 3}, {x1 - 3, y1 - 3}}) {
			g.fill(c[0], c[1], c[0] + 4, c[1] + 4, BRASS);
			g.fill(c[0] + 1, c[1] + 1, c[0] + 3, c[1] + 3, 0xFFF4D98C);
		}
	}

	/** The title on a wooden plaque between two paw marks, and how many companions there are. */
	private void header(GuiGraphicsExtractor g) {
		String title = "Companion Stable";
		int tw = font.width(title);
		int cx = left + panelW / 2;
		g.fill(cx - tw / 2 - 26, top + 10, cx + tw / 2 + 26, top + 24, WOOD_DARK);
		g.fill(cx - tw / 2 - 25, top + 11, cx + tw / 2 + 25, top + 23, WOOD);
		ScreenKit.bevel(g, cx - tw / 2 - 25, top + 11, tw + 50, 12, WOOD_LIGHT, WOOD_DARK);
		g.text(font, title, cx - tw / 2, top + 13, CREAM, true);
		paw(g, cx - tw / 2 - 19, top + 13, CREAM_SOFT);
		paw(g, cx + tw / 2 + 12, top + 13, CREAM_SOFT);
		String count = data.total() == 1 ? "1 companion" : data.total() + " companions";
		int countX = left + panelW - 12 - font.width(count);
		if (countX > cx + tw / 2 + 28) {
			g.text(font, count, countX, top + 14, CREAM_SOFT, false);
		}
	}

	/** The roster: a card for each companion, scrolled within its well, with a scroll bar when they do not all fit. */
	private void list(GuiGraphicsExtractor g, int mx, int my) {
		g.fill(listX - 1, listY - 1, listX + listW + 1, listY + listH + 1, LEATHER_DARK);
		g.fill(listX, listY, listX + listW, listY + listH, 0xFF2E1E12);
		g.enableScissor(listX, listY, listX + listW, listY + listH);
		int cardW = listW - (maxScroll() > 0 ? 6 : 0);
		int first = (int) (shownScroll / ROW);
		for (int row = first; row < rowCount() && row <= first + visibleRows() + 1; row++) {
			int y = listY + row * ROW - Math.round(shownScroll);
			boolean hover = mx >= listX && mx < listX + cardW && my >= Math.max(listY, y) && my < Math.min(listY + listH, y + ROW);
			if (moreBefore() && row == 0) {
				more(g, y, cardW, hover, "Earlier companions…");
				continue;
			}
			int index = row - (moreBefore() ? 1 : 0);
			if (index >= rows().size()) {
				more(g, y, cardW, hover, "More companions…");
				continue;
			}
			card(g, rows().get(index), index == selected, hover, y, cardW, mx, my);
		}
		g.disableScissor();
		if (maxScroll() > 0) {
			int trackX = listX + listW - 5;
			g.fill(trackX, listY + 1, trackX + 4, listY + listH - 1, TRACK);
			int knob = Math.max(12, (int) ((float) listH * listH / (rowCount() * ROW)));
			int knobY = listY + 1 + (int) ((listH - 2 - knob) * (shownScroll / maxScroll()));
			g.fill(trackX, knobY, trackX + 4, knobY + knob, WOOD_LIGHT);
			g.fill(trackX + 1, knobY + 1, trackX + 3, knobY + knob - 1, BRASS);
		}
	}

	private void more(GuiGraphicsExtractor g, int y, int w, boolean hover, String text) {
		g.fill(listX + 2, y + 2, listX + w - 2, y + ROW - 2, hover ? 0xFF5A3C26 : 0xFF3C281A);
		g.text(font, ScreenKit.fit(font, text, w - 12), listX + 8, y + 9, CREAM_SOFT, false);
	}

	/** One companion's card: portrait, name, level, and a status pill. */
	private void card(GuiGraphicsExtractor g, Pets.Row pet, boolean chosen, boolean hover, int y, int w, int mx, int my) {
		int x = listX + 2;
		int fill = chosen ? 0xFF6E4A2C : hover ? 0xFF5A3C26 : 0xFF3C281A;
		g.fill(x, y + 2, x + w - 4, y + ROW - 1, fill);
		if (chosen) {
			ScreenKit.bevel(g, x, y + 2, w - 4, ROW - 3, 0xFF8E6440, 0xFF2A1A0E);
			g.fill(x, y + 2, x + 2, y + ROW - 1, BRASS);   // the chosen card's brass edge
		}
		// The portrait.
		int px = x + 4, py = y + 4;
		g.fill(px, py, px + 18, py + 18, PARCHMENT_EDGE);
		g.fill(px + 1, py + 1, px + 17, py + 17, 0xFF2A3A30);
		Mob visual = visual(pet);
		if (visual != null && py + 18 > listY && py < listY + listH) {
			int size = (int) Math.max(4, Math.min(12, Math.min(12 / Math.max(0.1F, visual.getBbHeight()), 14 / Math.max(0.1F, visual.getBbWidth()))));
			InventoryScreen.extractEntityInInventoryFollowsMouse(g, px + 1, py + 1, px + 17, py + 17, size, 0.0625F, px + 9, py + 9, visual);
		} else {
			paw(g, px + 5, py + 5, 0xFF7C6A4C);
		}
		// The name and level, then the status pill.
		int textX = px + 22, textW = x + w - 6 - textX;
		String level = "Lv " + pet.level();
		g.text(font, ScreenKit.fit(font, pet.name(), textW - font.width(level) - 4), textX, y + 5, chosen ? 0xFFFFF4DC : CREAM, false);
		g.text(font, level, x + w - 6 - font.width(level), y + 5, BRASS, false);
		pill(g, textX, y + 15, pet, textW);
		if (hover && my >= listY && my < listY + listH) {
			tooltip(g, pet.name() + " — " + species(pet) + "\n" + statusText(pet), mx, my);
		}
	}

	/** A small coloured status pill: deployed (green), in the stable (amber), knocked out (red, gently pulsing). */
	private void pill(GuiGraphicsExtractor g, int x, int y, Pets.Row pet, int maxW) {
		// The full wording, or a shorter one when that does not fit (a narrow window): "KO 0:42" keeps the countdown.
		String text = shortStatus(pet);
		if (font.width(text) > maxW - 6) {
			text = switch (pet.state()) {
				case "ACTIVE" -> "Out";
				case "KNOCKED_OUT" -> recoveryLeft(pet) > 0 ? "KO " + clock(recoveryLeft(pet)) : "Ready";
				default -> "Stabled";
			};
		}
		int color = statusColor(pet);
		if (pet.state().equals("KNOCKED_OUT") && recoveryLeft(pet) > 0) {
			float pulse = 0.5F + 0.5F * Mth.sin((System.currentTimeMillis() % 2000L) / 2000.0F * Mth.TWO_PI);
			color = blend(color, 0xFFD45A48, pulse * 0.35F);
		}
		text = ScreenKit.fit(font, text, maxW - 6);
		int w = font.width(text) + 6;
		g.fill(x, y - 1, x + w, y + 8, color);
		g.fill(x, y - 1, x + w, y, blend(color, 0xFFFFFFFF, 0.25F));
		g.text(font, text, x + 3, y, 0xFFFFF6E6, false);
	}

	/** The chosen companion's page: a large portrait that follows the mouse, its kind, level, bond and health. */
	private void details(GuiGraphicsExtractor g, Pets.Row pet, int mx, int my) {
		int x = detailX, y = listY, w = detailW;
		int bottom = top + panelH - 46 - 48;
		g.fill(x - 1, y - 1, x + w + 1, bottom + 1, PARCHMENT_EDGE);
		g.fill(x, y, x + w, bottom, PARCHMENT);
		// Faint paw prints in the parchment's corner.
		paw(g, x + w - 14, bottom - 12, 0xFFDCC79C);
		paw(g, x + w - 24, bottom - 20, 0xFFDCC79C);
		// The portrait shrinks on short windows to leave room for the two bars (25 px) under it and the name, kind and
		// status beside it (34 px); neither ever overlaps the bars.
		int portrait = Mth.clamp(Math.min(bottom - y - 33, w / 3), 18, 64);
		int px = x + 4, py = y + 4;
		g.fill(px, py, px + portrait, py + portrait, WOOD);
		ScreenKit.bevel(g, px, py, portrait, portrait, WOOD_LIGHT, WOOD_DARK);
		g.fill(px + 2, py + 2, px + portrait - 2, py + portrait - 2, 0xFF2A3A30);
		Mob visual = visual(pet);
		if (visual != null) {
			int inner = portrait - 6;
			int size = (int) Math.max(6, Math.min(inner * 0.7F, Math.min(inner * 0.7F / Math.max(0.1F, visual.getBbHeight()),
					inner * 0.8F / Math.max(0.1F, visual.getBbWidth()))));
			InventoryScreen.extractEntityInInventoryFollowsMouse(g, px + 3, py + 3, px + portrait - 3, py + portrait - 3, size, 0.0625F, mx, my,
					visual);
		} else {
			paw(g, px + portrait / 2 - 3, py + portrait / 2 - 3, 0xFF7C6A4C);
		}
		// Name, kind and status beside the portrait.
		int tx = px + portrait + 6, tw = x + w - 4 - tx;
		g.text(font, ScreenKit.fit(font, pet.name(), tw), tx, py + 1, INK, false);
		g.text(font, ScreenKit.fit(font, species(pet), tw), tx, py + 12, INK_SOFT, false);
		pill(g, tx, py + 25, pet, tw);
		if (pet.state().equals("ACTIVE") && py + 44 <= py + portrait) {
			g.text(font, ScreenKit.fit(font, "Behaviour: " + MODE_LABELS[Math.max(0, Pets.MODES.indexOf(pet.mode()))], tw), tx, py + 36, INK_SOFT, false);
		}
		// Level and bond, then health, as labelled bars under the portrait.
		int barY = py + Math.max(portrait, 34) + 4;
		if (barY + 25 > bottom) {
			return;   // no room for the bars on so short a page: the roster card still shows the status
		}
		boolean max = pet.xp() >= Pets.MAX_XP || pet.level() >= 20;
		float bond = max ? 1.0F : (pet.xp() % Pets.XP_PER_LEVEL) / (float) Pets.XP_PER_LEVEL;
		String bondText = max ? "Level 20 · bond full" : "Level " + pet.level() + " · bond " + pet.xp() % Pets.XP_PER_LEVEL + "/" + Pets.XP_PER_LEVEL;
		bar(g, x + 4, barY, w - 8, bond, BOND, ScreenKit.fit(font, bondText, w - 14));
		if (within(mx, my, x + 4, barY, w - 8, 11)) {
			tooltip(g, "Bond grows as you travel together: one point for every six seconds on the move near you. "
					+ "Each level adds a little health, strength and pace (level 5 defenders mark threats).", mx, my);
		}
		boolean known = pet.health() >= 0;
		float health = known && pet.maxHealth() > 0 ? Mth.clamp(pet.health() / pet.maxHealth(), 0, 1) : known ? 1 : 0;
		String healthText = !known ? "Health unknown" : pet.maxHealth() > 0
				? "Health " + hp(pet.health()) + " / " + hp(pet.maxHealth()) : "Health " + hp(pet.health());
		heart(g, x + 4, barY + 15);
		bar(g, x + 13, barY + 13, w - 17, health, HEART, ScreenKit.fit(font, healthText, w - 23));
		if (within(mx, my, x + 4, barY + 13, w - 8, 11)) {
			tooltip(g, pet.state().equals("ACTIVE") ? "Its health now. Calm companions slowly recover."
					: "Its health when it entered the stable. A knock-out leaves it at half health.", mx, my);
		}
		// What its status means, in the room left on the page.
		int ty = barY + 28;
		for (var line : font.split(Component.literal(statusText(pet)), w - 10)) {
			if (ty + 9 > bottom - 2) {
				break;
			}
			g.text(font, line, x + 5, ty, INK_SOFT, false);
			ty += 10;
		}
	}

	/** The empty stable: a large paw and how to fill it. */
	private void empty(GuiGraphicsExtractor g) {
		int x = detailX, y = listY, w = detailW, bottom = top + panelH - 46;
		g.fill(x - 1, y - 1, x + w + 1, bottom + 1, PARCHMENT_EDGE);
		g.fill(x, y, x + w, bottom, PARCHMENT);
		bigPaw(g, x + w / 2 - 10, y + 10, 0xFFC9AE7E);
		String headline = ScreenKit.fit(font, "Your stable is empty", w - 8);
		g.text(font, headline, x + (w - font.width(headline)) / 2, y + 40, INK, false);
		int ty = y + 54;
		String help = "Tame a wolf, cat, parrot, horse or another companion and it joins this stable. "
				+ "Recall companions here to keep them safe; a knocked-out companion rests here for a minute before it can be deployed.";
		for (var line : font.split(Component.literal(help), w - 12)) {
			if (ty > bottom - 10) {
				break;
			}
			g.text(font, line, x + 6, ty, INK_SOFT, false);
			ty += 10;
		}
		String none = ScreenKit.fit(font, "No companions yet", listW - 8);
		g.text(font, none, listX + (listW - font.width(none)) / 2, listY + 10, CREAM_SOFT, false);
	}

	/** The server's last word (what an action did, or why not), on a strip above the bottom keys. */
	private void message(GuiGraphicsExtractor g, int mx, int my) {
		String text = data.message() == null ? "" : data.message();
		int y = top + panelH - 42;
		int x0 = left + 9, x1 = left + panelW - 9;
		g.fill(x0, y, x1, y + 12, LEATHER_DARK);
		if (text.isEmpty()) {
			g.text(font, ScreenKit.fit(font, "Pick a companion; Enter deploys or recalls it, 1–4 set its behaviour.", x1 - x0 - 8), x0 + 4, y + 2,
					0xFFA88E68, false);
			return;
		}
		g.text(font, ScreenKit.fit(font, text, x1 - x0 - 8), x0 + 4, y + 2, 0xFFF0CF84, false);
		if (font.width(text) > x1 - x0 - 8 && within(mx, my, x0, y, x1 - x0, 12)) {
			tooltip(g, text, mx, my);
		}
	}

	/**
	 * A tooltip wrapped to fit the window (at most 220 px wide; narrower windows narrower), at line breaks and between
	 * words: a long single line would otherwise run off a small window.
	 */
	private void tooltip(GuiGraphicsExtractor g, String text, int mx, int my) {
		int max = Math.max(80, Math.min(220, width - 24));
		List<Component> lines = new java.util.ArrayList<>();
		for (String paragraph : text.split("\n")) {
			StringBuilder line = new StringBuilder();
			for (String word : paragraph.split(" ")) {
				String next = line.isEmpty() ? word : line + " " + word;
				if (!line.isEmpty() && font.width(next) > max) {
					lines.add(Component.literal(line.toString()));
					line = new StringBuilder(word);
				} else {
					line = new StringBuilder(next);
				}
			}
			lines.add(Component.literal(line.toString()));
		}
		g.setTooltipForNextFrame(font, lines, java.util.Optional.empty(), mx, my);
	}

	// ------------------------------------------------------------------ small drawings

	/** A labelled bar: a dark track, the filled part in {@code color} with a lighter top line, the label over it. */
	private void bar(GuiGraphicsExtractor g, int x, int y, int w, float fraction, int color, String label) {
		g.fill(x, y, x + w, y + 11, TRACK);
		int filled = Math.round((w - 2) * Mth.clamp(fraction, 0, 1));
		if (filled > 0) {
			g.fill(x + 1, y + 1, x + 1 + filled, y + 10, color);
			g.fill(x + 1, y + 1, x + 1 + filled, y + 2, blend(color, 0xFFFFFFFF, 0.3F));
		}
		g.text(font, label, x + 3, y + 2, 0xFFFFF6E6, true);
	}

	/** A paw print, 7 x 7: a pad and four toes. */
	private static void paw(GuiGraphicsExtractor g, int x, int y, int color) {
		g.fill(x + 1, y + 4, x + 6, y + 7, color);
		g.fill(x + 2, y + 3, x + 5, y + 4, color);
		g.fill(x, y + 2, x + 1, y + 4, color);
		g.fill(x + 6, y + 2, x + 7, y + 4, color);
		g.fill(x + 2, y, x + 3, y + 2, color);
		g.fill(x + 4, y, x + 5, y + 2, color);
	}

	/** A paw print at three times the size, for the empty stable. */
	private static void bigPaw(GuiGraphicsExtractor g, int x, int y, int color) {
		g.fill(x + 4, y + 12, x + 17, y + 21, color);
		g.fill(x + 6, y + 9, x + 15, y + 12, color);
		g.fill(x, y + 5, x + 3, y + 11, color);
		g.fill(x + 18, y + 5, x + 21, y + 11, color);
		g.fill(x + 5, y, x + 9, y + 6, color);
		g.fill(x + 12, y, x + 16, y + 6, color);
	}

	/** A small heart, 7 x 6. */
	private static void heart(GuiGraphicsExtractor g, int x, int y) {
		g.fill(x, y + 1, x + 7, y + 3, HEART);
		g.fill(x + 1, y, x + 3, y + 1, HEART);
		g.fill(x + 4, y, x + 6, y + 1, HEART);
		g.fill(x + 1, y + 3, x + 6, y + 4, HEART);
		g.fill(x + 2, y + 4, x + 5, y + 5, HEART);
		g.fill(x + 3, y + 5, x + 4, y + 6, HEART);
		g.fill(x + 1, y + 1, x + 2, y + 2, 0xFFF4A39A);
	}

	// ------------------------------------------------------------------ words

	private String shortStatus(Pets.Row pet) {
		return switch (pet.state()) {
			case "ACTIVE" -> "Deployed";
			case "KNOCKED_OUT" -> recoveryLeft(pet) > 0 ? "Knocked out " + clock(recoveryLeft(pet)) : "Recovered";
			default -> "In the stable";
		};
	}

	private String statusText(Pets.Row pet) {
		return switch (pet.state()) {
			case "ACTIVE" -> "Deployed: out in the world with you (" + MODE_LABELS[Math.max(0, Pets.MODES.indexOf(pet.mode()))].toLowerCase() + ").";
			case "KNOCKED_OUT" -> recoveryLeft(pet) > 0 ? "Knocked out: recovering in the stable for " + clock(recoveryLeft(pet)) + " more."
					: "Recovered from a knock-out: ready to deploy.";
			default -> "In the stable: safe and stored, ready to deploy.";
		};
	}

	private int statusColor(Pets.Row pet) {
		return switch (pet.state()) {
			case "ACTIVE" -> DEPLOYED;
			case "KNOCKED_OUT" -> recoveryLeft(pet) > 0 ? KNOCKED_OUT : STABLED;
			default -> STABLED;
		};
	}

	/** The companion's kind, from its entity type (its own translated name). */
	private static String species(Pets.Row pet) {
		var id = net.minecraft.resources.Identifier.tryParse(pet.type());
		var registry = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE;
		return id != null && registry.containsKey(id) ? registry.getValue(id).getDescription().getString() : "Companion";
	}

	private static String hp(float value) {
		return value == Math.floor(value) ? Integer.toString((int) value) : String.format("%.1f", value);
	}

	/** The companion as a model to draw (the server's appearance-only preview, cached by the preview helper). */
	private static @Nullable Mob visual(Pets.Row pet) {
		var entry = new net.minecraft.nbt.CompoundTag();
		entry.putString("Claim", pet.id() + pet.preview());
		entry.putString("Type", pet.type());
		var preview = net.minecraft.nbt.CompoundTag.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE,
				com.google.gson.JsonParser.parseString(pet.preview())).result().orElse(new net.minecraft.nbt.CompoundTag());
		entry.put("Preview", preview);
		return local.peepo.client.TransportCratePreview.mob(entry);
	}

	private static boolean within(int mx, int my, int x, int y, int w, int h) {
		return mx >= x && mx < x + w && my >= y && my < y + h;
	}

	private static int blend(int a, int b, float t) {
		int ar = (a >> 16) & 0xFF, ag = (a >> 8) & 0xFF, ab = a & 0xFF;
		int br = (b >> 16) & 0xFF, bg = (b >> 8) & 0xFF, bb = b & 0xFF;
		return 0xFF000000 | Math.round(ar + (br - ar) * t) << 16 | Math.round(ag + (bg - ag) * t) << 8 | Math.round(ab + (bb - ab) * t);
	}

	/**
	 * A key in the stable's look: a wooden key with a lighter bevel (brass-edged for the big action); lit when selected
	 * (the companion's behaviour now), lighter when hovered or focused by the keyboard, dimmed when it cannot be pressed.
	 */
	static final class StableButton extends Button {
		boolean selected;
		private final boolean primary;

		StableButton(int x, int y, int w, int h, Component label, OnPress onPress, boolean primary) {
			super(x, y, w, h, label, onPress, Button.DEFAULT_NARRATION);
			this.primary = primary;
		}

		@Override
		protected void extractContents(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
			Font font = Minecraft.getInstance().font;
			int x = getX(), y = getY(), w = width, h = height;
			boolean hover = active && (isHovered() || isFocused());
			int face = primary ? 0xFF7A4A28 : WOOD;
			if (selected) {
				face = 0xFF8E6436;
			} else if (hover) {
				face = blend(face, 0xFFFFFFFF, 0.14F);
			} else if (!active) {
				face = blend(face, 0xFF000000, 0.35F);
			}
			g.fill(x, y, x + w, y + h, primary ? BRASS : WOOD_DARK);
			g.fill(x + 1, y + 1, x + w - 1, y + h - 1, face);
			g.fill(x + 1, y + 1, x + w - 1, y + 2, active ? blend(face, 0xFFFFFFFF, 0.3F) : face);
			g.fill(x + 1, y + h - 2, x + w - 1, y + h - 1, blend(face, 0xFF000000, 0.3F));
			if (selected) {
				g.fill(x + 3, y + h - 3, x + w - 3, y + h - 2, BRASS);
			}
			if (isFocused()) {
				g.outline(x - 1, y - 1, w + 2, h + 2, 0xFFFFE7A8);
			}
			String text = ScreenKit.fit(font, getMessage().getString(), w - 6);
			int color = active || selected ? 0xFFFFF4DC : 0xFFB59C7A;
			g.text(font, text, x + (w - font.width(text)) / 2, y + (h - 8) / 2, color, primary);
		}
	}
}
