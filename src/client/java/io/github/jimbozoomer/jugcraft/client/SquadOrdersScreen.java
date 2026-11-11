package io.github.jimbozoomer.jugcraft.client;

import io.github.jimbozoomer.jugcraft.tower.squad.Legionary;
import io.github.jimbozoomer.jugcraft.tower.squad.SquadPayloads;
import java.util.List;
import java.util.Locale;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;

/**
 * A squad leader's orders panel (sheet 4's, approved 9 November 2026; tower/squad/SquadOrders): over the view, his rank
 * and name and the squad's strength, its order and whether it's in a fight, the roster with each one's health, and the
 * orders: FOLLOW ME, HOLD THIS POSITION, PATROL AREA (its radius set with - and +), SEND HOME (not in a fight). Laid out
 * in the mockup's 960 x 600 and scaled to the screen.
 */
public class SquadOrdersScreen extends Screen {
	private static final float W = 960, H = 600;
	private static final int AMBER = 0xFFFFBA74, GREEN = 0xFF5BE38A, TXT = 0xFFC3DBDC, DIM = 0xFF6F8487;
	private static final int BG = 0xE1090D0F, PANEL = 0xF0101B1B, GRID = 0xFF202C30, HEAD = 0xEB2B1C0C, ON = 0xF03C2812, GREY = 0xF0141A1C;
	private static final int X0 = 530, Y0 = 70, X1 = 930, Y1 = 540;

	private SquadPayloads.Orders page;
	private int radius;
	private float scale, ox, oy;

	public SquadOrdersScreen(SquadPayloads.Orders page) {
		super(Component.translatable("screen.jugcraft.orders"));
		this.page = page;
		radius = page.radius();
	}

	static void register() {
		ClientPlayNetworking.registerGlobalReceiver(SquadPayloads.Orders.TYPE, (page, context) -> {
			Minecraft client = Minecraft.getInstance();
			if (client.gui.screen() instanceof SquadOrdersScreen open && open.page.leader() == page.leader()) {
				open.page = page;
			} else if (page.open()) {
				client.gui.setScreen(new SquadOrdersScreen(page));
			}
		});
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	/** The view darkened under the panel (as the mockup), not blurred. */
	@Override
	public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
		g.fill(0, 0, width, height, 0x5A080A0E);
	}

	/** The rows' spacing for the roster's size (a squad of seven packed a little closer). */
	private int rowH() {
		return page.roster().size() > 5 ? 16 : 22;
	}

	private int buttonsTop() {
		return Y0 + 80 + page.roster().size() * rowH() + 10;
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
		super.extractRenderState(g, mouseX, mouseY, partialTick);
		scale = Math.min(width / W, height / H) * 0.98F;
		ox = (width - W * scale) / 2;
		oy = (height - H * scale) / 2;
		g.pose().pushMatrix();
		g.pose().translate(ox, oy);
		g.pose().scale(scale, scale);
		float mx = (mouseX - ox) / scale, my = (mouseY - oy) / scale;
		g.fill(X0, Y0, X1, Y1, BG);
		box(g, X0, Y0, X1, Y1, 2, GRID);
		corners(g, X0, Y0, X1, Y1);
		g.fill(X0 + 2, Y0 + 2, X1 - 2, Y0 + 26, HEAD);
		String[] title = page.title().split(";", 2);
		String rank = Component.translatable("squad.jugcraft.rank." + title[0]).getString();
		String name = title.length > 1 ? title[1] : "";
		text(g, Component.literal(Component.translatable("screen.jugcraft.orders.title", rank, name, page.roster().size(), page.size()).getString()
				.toUpperCase(Locale.ROOT)), X0 + 12, Y0 + 7, 15, AMBER);
		Component order = Component.translatable("screen.jugcraft.orders.order." + Mth.clamp(page.order(), 0, 3), page.radius());
		text(g, Component.translatable("screen.jugcraft.orders.current", order), X0 + 14, Y0 + 36, 14, GREEN);
		text(g, Component.translatable(page.inCombat() ? "screen.jugcraft.orders.fighting" : "screen.jugcraft.orders.quiet"), X0 + 14, Y0 + 56, 11,
				page.inCombat() ? AMBER : DIM);
		// their day in the field: the time left till the leader calls the ship home (counted down here)
		Minecraft mc = Minecraft.getInstance();
		if (page.recallAt() >= 0 && mc.level != null && page.order() != Legionary.HOME) {
			long left = page.recallAt() - mc.level.getGameTime();
			Component recall = left > 0 ? Component.translatable("screen.jugcraft.orders.recall", String.format(Locale.ROOT, "%d:%02d", left / 1200, left / 20 % 60))
					: Component.translatable("screen.jugcraft.orders.recall_due");
			float w = font.width(recall) * 1.1F;
			text(g, recall, X1 - 14 - w, Y0 + 56, 11, left <= Legionary.RECALL_WARNING ? AMBER : TXT);
		}
		List<String> roster = page.roster();
		for (int k = 0; k < roster.size(); k++) {
			String[] f = roster.get(k).split(";", 3);
			int y = Y0 + 80 + k * rowH();
			String rk = Component.translatable("squad.jugcraft.rank." + f[0]).getString().toUpperCase(Locale.ROOT);
			text(g, Component.literal(rk.substring(0, Math.min(4, rk.length())) + "  " + (f.length > 1 ? f[1] : "")), X0 + 14, y, 12, TXT);
			float hp = f.length > 2 ? Mth.clamp(parse(f[2]) / 100F, 0, 1) : 1;
			g.fill(X0 + 236, y + 3, X0 + 376, y + 13, 0xFF1E282A);
			g.fill(X0 + 236, y + 3, X0 + 236 + Math.round(140 * hp), y + 13, hp > 0.5F ? GREEN : AMBER);
		}
		int by = buttonsTop();
		boolean homeOk = !page.inCombat() && page.order() != Legionary.HOME;
		button(g, by, Legionary.FOLLOW, "follow", mx, my, true);
		button(g, by + 58, Legionary.HOLD, "hold", mx, my, true);
		button(g, by + 116, Legionary.PATROL, "patrol", mx, my, true);
		// the patrol's radius, - and + either side
		g.fill(X0 + 290, by + 122, X0 + 378, by + 146, BG);
		box(g, X0 + 290, by + 122, X0 + 378, by + 146, 2, GRID);
		text(g, Component.literal("-  " + radius + "  +"), X0 + 298, by + 127, 14, AMBER);
		button(g, by + 174, Legionary.HOME, "home", mx, my, homeOk);
		if (!page.note().isEmpty()) {
			text(g, Component.translatable("message.jugcraft.squad.order." + page.note()), X0 + 14, by + 236, 11, AMBER);
		} else {
			text(g, Component.translatable("screen.jugcraft.orders.foot1"), X0 + 14, by + 236, 11, DIM);
			text(g, Component.translatable("screen.jugcraft.orders.foot2"), X0 + 14, by + 250, 11, DIM);
		}
		g.pose().popMatrix();
	}

	/** An order's button: lit for the squad's order now, greyed when it can't be given. */
	private void button(GuiGraphicsExtractor g, int y, int order, String key, float mx, float my, boolean usable) {
		boolean on = page.order() == order;
		boolean over = usable && in(mx, my, X0 + 14, y, X1 - 14, y + 48) && !(order == Legionary.PATROL && overRadius(mx, my, y - 116));
		g.fill(X0 + 14, y, X1 - 14, y + 48, on ? ON : usable ? over ? 0xF0182628 : PANEL : GREY);
		box(g, X0 + 14, y, X1 - 14, y + 48, 2, on ? AMBER : over ? 0xFF3A4A50 : GRID);
		text(g, Component.translatable("screen.jugcraft.orders.button." + key), X0 + 28, y + 8, 16, on ? AMBER : usable ? TXT : DIM);
		text(g, Component.translatable("screen.jugcraft.orders.button." + key + ".sub"), X0 + 28, y + 30, 11, DIM);
	}

	private static boolean overRadius(float mx, float my, int by) {
		return in(mx, my, X0 + 290, by + 122, X0 + 378, by + 146);
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		float mx = (float) ((event.x() - ox) / scale), my = (float) ((event.y() - oy) / scale);
		int by = buttonsTop();
		if (overRadius(mx, my, by)) {
			int step = mx < X0 + 334 ? -Legionary.PATROL_STEP : Legionary.PATROL_STEP;
			radius = Mth.clamp(radius + step, Legionary.PATROL_MIN, Legionary.PATROL_MAX);
			click();
			if (page.order() == Legionary.PATROL) {
				send(Legionary.PATROL);                     // patrolling now: the new radius at once
			}
			return true;
		}
		int[] orders = {Legionary.FOLLOW, Legionary.HOLD, Legionary.PATROL, Legionary.HOME};
		for (int k = 0; k < orders.length; k++) {
			int y = by + k * 58;
			if (in(mx, my, X0 + 14, y, X1 - 14, y + 48)) {
				if (orders[k] == Legionary.HOME && (page.inCombat() || page.order() == Legionary.HOME)) {
					return true;
				}
				send(orders[k]);
				click();
				return true;
			}
		}
		return super.mouseClicked(event, doubleClick);
	}

	private void send(int order) {
		ClientPlayNetworking.send(new SquadPayloads.Order(page.leader(), order, radius));
	}

	private static int parse(String s) {
		try {
			return Integer.parseInt(s);
		} catch (NumberFormatException e) {
			return 100;
		}
	}

	private static void click() {
		Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
	}

	private static boolean in(float mx, float my, int x0, int y0, int x1, int y1) {
		return mx >= x0 && mx < x1 && my >= y0 && my < y1;
	}

	private static void box(GuiGraphicsExtractor g, int x0, int y0, int x1, int y1, int t, int colour) {
		g.fill(x0, y0, x1, y0 + t, colour);
		g.fill(x0, y1 - t, x1, y1, colour);
		g.fill(x0, y0, x0 + t, y1, colour);
		g.fill(x1 - t, y0, x1, y1, colour);
	}

	private static void corners(GuiGraphicsExtractor g, int x0, int y0, int x1, int y1) {
		int a = 14, t = 3;
		g.fill(x0, y0, x0 + a, y0 + t, AMBER);
		g.fill(x0, y0, x0 + t, y0 + a, AMBER);
		g.fill(x1 - a, y0, x1, y0 + t, AMBER);
		g.fill(x1 - t, y0, x1, y0 + a, AMBER);
		g.fill(x0, y1 - t, x0 + a, y1, AMBER);
		g.fill(x0, y1 - a, x0 + t, y1, AMBER);
		g.fill(x1 - a, y1 - t, x1, y1, AMBER);
		g.fill(x1 - t, y1 - a, x1, y1, AMBER);
	}

	/** Text at the mockup's point size. */
	private void text(GuiGraphicsExtractor g, Component text, float x, float y, float size, int colour) {
		float s = size / 10F;
		g.pose().pushMatrix();
		g.pose().translate(x, y);
		g.pose().scale(s, s);
		g.text(font, text, 0, 0, colour, false);
		g.pose().popMatrix();
	}
}
