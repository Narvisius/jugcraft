package io.github.jimbozoomer.jugcraft.client;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.tower.squad.JugcraftSquads;
import io.github.jimbozoomer.jugcraft.tower.squad.SquadPayloads;
import java.util.List;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;

/**
 * The manager's desk's requisitions page (sheet 5's "shop"; tower/squad/Requisitions): a card for each squad (its pod,
 * rank, price, what's on the player's account for it, whether the tower can pay), the one picked laid out against what
 * the tower holds (its store and the logistics network its terminal reaches), and REQUISITION, which buys one onto the
 * player's account. Laid out in the mockup's 960 x 600 and scaled to the screen.
 */
public class RequisitionScreen extends Screen {
	private static final Identifier CARD = Jugcraft.id("textures/gui/drop_pod_card.png");
	private static final float W = 960, H = 600;
	private static final int AMBER = 0xFFFFBA74, GREEN = 0xFF5BE38A, TXT = 0xFFC3DBDC, DIM = 0xFF6F8487, WHITE = 0xFFECF0F0;
	private static final int BG = 0xE1090D0F, PANEL = 0xF0101B1B, GRID = 0xFF202C30, HEAD = 0xEB2B1C0C, PICKED = 0xF0281C0E;
	private static final String[] PAY = {"iron", "copper", "gold", "diamond"};

	private BlockPos desk;
	private int[] have = new int[4], charges = new int[3];
	/** The player's price for each size now (up 10% for each of that size they've bought). */
	private int[][] prices = {JugcraftSquads.COST[0], JugcraftSquads.COST[1], JugcraftSquads.COST[2]};
	private boolean allowed, inField;
	private int picked = 1;
	private float scale, ox, oy;

	public RequisitionScreen(SquadPayloads.Page page) {
		super(Component.translatable("screen.jugcraft.requisitions"));
		desk = page.desk();
		update(page);
	}

	/** The page as the server last sent it. */
	void update(SquadPayloads.Page page) {
		desk = page.desk();
		have = ints(page.have(), 4);
		charges = ints(page.charges(), 3);
		int[] flat = ints(page.prices(), 12);
		for (int k = 0; k < 3; k++) {
			prices[k] = java.util.Arrays.copyOfRange(flat, k * 4, k * 4 + 4);
		}
		allowed = page.allowed();
		inField = page.inField();
	}

	private static int[] ints(List<Integer> list, int n) {
		int[] out = new int[n];
		for (int i = 0; i < n && i < list.size(); i++) {
			out[i] = list.get(i);
		}
		return out;
	}

	static void register() {
		ClientPlayNetworking.registerGlobalReceiver(SquadPayloads.Page.TYPE, (page, context) -> {
			Minecraft client = Minecraft.getInstance();
			if (client.gui.screen() instanceof RequisitionScreen open) {
				open.update(page);
			} else if (page.open()) {
				client.gui.setScreen(new RequisitionScreen(page));
			}
		});
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	private boolean affords(int index) {
		for (int i = 0; i < 4; i++) {
			if (have[i] < prices[index][i]) {
				return false;
			}
		}
		return true;
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
		int x0 = 40, y0 = 28, x1 = 920, y1 = 572;
		g.fill(x0, y0, x1, y1, BG);
		box(g, x0, y0, x1, y1, 2, GRID);
		corners(g, x0, y0, x1, y1);
		g.fill(x0 + 2, y0 + 2, x1 - 2, y0 + 26, HEAD);
		text(g, Component.translatable("screen.jugcraft.requisitions.title"), x0 + 12, y0 + 7, 15, AMBER);
		for (int k = 0; k < 3; k++) {
			card(g, k, x0 + 12, y0 + 40 + k * 140, k == picked || in(mx, my, x0 + 12, y0 + 40 + k * 140, x0 + 262, y0 + 168 + k * 140));
		}
		detail(g, x0 + 280, y0, mx, my);
		g.pose().popMatrix();
	}

	/** A squad's card: its pod, rank, price, account and whether the tower can pay. */
	private void card(GuiGraphicsExtractor g, int k, int x, int y, boolean lit) {
		int n = JugcraftSquads.SIZES[k];
		boolean sel = k == picked;
		g.fill(x, y, x + 250, y + 128, sel ? PICKED : PANEL);
		box(g, x, y, x + 250, y + 128, 2, sel ? AMBER : lit ? 0xFF3A4A50 : GRID);
		g.fill(x + 8, y + 22, x + 76, y + 106, 0xFF090D0F);
		g.blit(RenderPipelines.GUI_TEXTURED, CARD, x + 10, y + 24, 0, 0, 64, 80, 64, 80, 64, 80);
		text(g, Component.translatable("screen.jugcraft.requisitions.squad", n), x + 86, y + 12, 13, sel ? AMBER : TXT);
		text(g, Component.translatable("screen.jugcraft.requisitions.lead", Component.translatable("squad.jugcraft.rank." + JugcraftSquads.RANKS[k]), n - 1),
				x + 86, y + 32, 12, TXT);
		int[] c = prices[k];
		text(g, Component.translatable("screen.jugcraft.requisitions.cost1", c[0], c[1]), x + 86, y + 52, 11, DIM);
		text(g, Component.translatable("screen.jugcraft.requisitions.cost2", c[2], c[3]), x + 86, y + 66, 11, DIM);
		text(g, Component.translatable("screen.jugcraft.requisitions.account", charges[k]), x + 86, y + 90, 13, charges[k] > 0 ? GREEN : DIM);
		boolean ok = allowed && affords(k);
		text(g, Component.translatable(ok ? "screen.jugcraft.requisitions.can_pay" : "screen.jugcraft.requisitions.short"), x + 86, y + 108, 11,
				ok ? GREEN : AMBER);
	}

	/** The picked squad against what the tower holds, and the requisition button. */
	private void detail(GuiGraphicsExtractor g, int rx, int y0, float mx, float my) {
		int n = JugcraftSquads.SIZES[picked];
		text(g, Component.translatable("screen.jugcraft.requisitions.heading", n, Component.translatable("squad.jugcraft.rank." + JugcraftSquads.RANKS[picked])),
				rx + 4, y0 + 40, 17, AMBER);
		text(g, Component.translatable("screen.jugcraft.requisitions.paid"), rx + 4, y0 + 64, 12, TXT);
		text(g, Component.translatable("screen.jugcraft.requisitions.used"), rx + 4, y0 + 82, 12, DIM);
		int[] need = prices[picked];
		String shortBy = null;
		for (int i = 0; i < 4; i++) {
			int y = y0 + 116 + i * 62;
			g.fill(rx, y, rx + 590, y + 52, PANEL);
			box(g, rx, y, rx + 590, y + 52, 2, GRID);
			g.pose().pushMatrix();
			g.pose().translate(rx + 12, y + 10);
			g.pose().scale(2, 2);
			g.item(new ItemStack(JugcraftSquads.PAY[i]), 0, 0);
			g.pose().popMatrix();
			text(g, Component.translatable("screen.jugcraft.requisitions.pay." + PAY[i]), rx + 54, y + 6, 14, TXT);
			text(g, Component.translatable("screen.jugcraft.requisitions.takes", need[i]), rx + 54, y + 28, 14, WHITE);
			boolean ok = have[i] >= need[i];
			text(g, Component.translatable("screen.jugcraft.requisitions.holds", allowed ? String.format("%,d", have[i]).replace(',', ' ') : "-"),
					rx + 250, y + 28, 14, ok ? GREEN : AMBER);
			text(g, ok ? Component.translatable("screen.jugcraft.requisitions.ok") : Component.translatable("screen.jugcraft.requisitions.short_by", need[i] - have[i]),
					rx + 490, y + 18, 14, ok ? GREEN : AMBER);
			if (!ok && shortBy == null) {
				shortBy = PAY[i];
			}
		}
		int by = y0 + 370;
		boolean can = allowed && affords(picked);
		boolean over = can && in(mx, my, rx, by, rx + 590, by + 44);
		g.fill(rx, by, rx + 590, by + 44, can ? over ? 0xF0503216 : 0xF03C2812 : 0xF0141A1C);
		box(g, rx, by, rx + 590, by + 44, 2, can ? AMBER : GRID);
		text(g, Component.translatable("screen.jugcraft.requisitions.buy", n), rx + 14, by + 13, 16, can ? AMBER : DIM);
		int line = by + 58;
		if (!allowed) {
			text(g, Component.translatable("screen.jugcraft.requisitions.not_yours"), rx + 4, line, 12, AMBER);
		} else if (shortBy != null) {
			int i = java.util.Arrays.asList(PAY).indexOf(shortBy);
			text(g, Component.translatable("screen.jugcraft.requisitions.bring", need[i] - have[i], Component.translatable("screen.jugcraft.requisitions.pay." + shortBy)),
					rx + 4, line, 12, AMBER);
		} else if (inField) {
			text(g, Component.translatable("screen.jugcraft.requisitions.in_field"), rx + 4, line, 12, AMBER);
		}
		text(g, Component.translatable("screen.jugcraft.requisitions.rule1"), rx + 4, line + 18, 12, TXT);
		text(g, Component.translatable("screen.jugcraft.requisitions.rule2"), rx + 4, line + 36, 12, DIM);
		text(g, Component.translatable("screen.jugcraft.requisitions.ledger", charges[0], charges[1], charges[2]), rx + 4, line + 54, 12, GREEN);
		text(g, Component.translatable("screen.jugcraft.requisitions.rule3"), rx + 4, line + 72, 12, DIM);
		text(g, Component.translatable("screen.jugcraft.requisitions.rule4"), rx + 4, line + 90, 12, DIM);
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		float mx = (float) ((event.x() - ox) / scale), my = (float) ((event.y() - oy) / scale);
		int x0 = 40, y0 = 28;
		for (int k = 0; k < 3; k++) {
			if (in(mx, my, x0 + 12, y0 + 40 + k * 140, x0 + 262, y0 + 168 + k * 140)) {
				picked = k;
				click();
				return true;
			}
		}
		int rx = x0 + 280, by = y0 + 370;
		if (in(mx, my, rx, by, rx + 590, by + 44) && allowed && affords(picked)) {
			ClientPlayNetworking.send(new SquadPayloads.Buy(desk, picked));
			click();
			return true;
		}
		return super.mouseClicked(event, doubleClick);
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

	/** The panel's amber corner brackets. */
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

	/** Text at the mockup's point size (its letters' height to the font's 9). */
	private void text(GuiGraphicsExtractor g, Component text, float x, float y, float size, int colour) {
		float s = size / 10F;
		g.pose().pushMatrix();
		g.pose().translate(x, y);
		g.pose().scale(s, s);
		g.text(font, text, 0, 0, colour, false);
		g.pose().popMatrix();
	}
}
