package io.github.jimbozoomer.jugcraft.client;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

/** Presentation shared by the encyclopedia's contents and topic pages; uses the owner's unchanged book artwork. */
final class BookArt {
	static final int INK = 0xFF403527, MUTED = 0xFF776346, GOLD = 0xFF986B2E, GREEN = 0xFF426858;
	private static final Identifier BOOK = Jugcraft.id("textures/gui/encyclopedia/book.png");
	private BookArt() { }

	static void book(GuiGraphicsExtractor g, int x, int y, int w, int h) {
		g.fill(x + 5, y + 5, x + w + 5, y + h + 6, 0x8009070B);
		g.blit(RenderPipelines.GUI_TEXTURED, BOOK, x, y, 0, 0, w, h, 290, 188, 290, 188);
	}

	static void rule(GuiGraphicsExtractor g, int x, int y, int w) {
		g.fill(x, y, x + w, y + 1, 0x608D7046);
		star(g, x + w / 2, y, 3, GOLD);
	}

	static void star(GuiGraphicsExtractor g, int x, int y, int r, int color) {
		for (int row = -r; row <= r; row++) {
			int reach = Math.max(0, r - Math.abs(row));
			g.fill(x - reach, y + row, x + reach + 1, y + row + 1, color);
		}
	}

	static void line(GuiGraphicsExtractor g, int x, int y, int x2, int y2, int color) {
		int steps = Math.max(Math.abs(x2 - x), Math.abs(y2 - y));
		for (int i = 0; i <= steps; i++) {
			int px = x + (x2 - x) * i / Math.max(1, steps), py = y + (y2 - y) * i / Math.max(1, steps);
			g.fill(px, py, px + 1, py + 1, color);
		}
	}

	static void circle(GuiGraphicsExtractor g, int x, int y, int radius, int color) {
		int segments = 48;
		for (int i = 0; i < segments; i++) {
			double a = Math.PI * 2 * i / segments, b = Math.PI * 2 * (i + 1) / segments;
			line(g, x + (int) (Math.cos(a) * radius), y + (int) (Math.sin(a) * radius),
					x + (int) (Math.cos(b) * radius), y + (int) (Math.sin(b) * radius), color);
		}
	}

	static void icon(GuiGraphicsExtractor g, String id, int x, int y) {
		Identifier key = Identifier.tryParse(id);
		if (key != null) g.item(new ItemStack(BuiltInRegistries.ITEM.getValue(key)), x, y);
	}

	static String fit(Font font, String text, int width) {
		if (font.width(text) <= width) return text;
		return font.plainSubstrByWidth(text, Math.max(0, width - font.width("…"))) + "…";
	}

	/** Ink has no Minecraft text shadow. Keep paragraphs focusable and narrated without double-dark letter edges. */
	static final class Paragraph extends AbstractWidget {
		private final Font font;
		private final java.util.List<net.minecraft.util.FormattedCharSequence> lines;
		private final int color;
		private Component usage = Component.empty();
		Paragraph(Component text, Font font, int width, int color) {
			super(0, 0, Math.max(20, width), 1, text);
			this.font = font; this.color = color;
			lines = font.split(text, getWidth() - 2);
			setHeight(Math.max(1, lines.size()) * (font.lineHeight + 2) + 2);
		}
		void setUsageNarration(Component description, Object ignored) { usage = description; }
		@Override protected void extractWidgetRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
			int y = getY() + 1;
			for (var line : lines) { g.text(font, line, getX() + 1, y, color, false); y += font.lineHeight + 2; }
			if (isFocused()) g.outline(getX(), getY(), width, height, color);
		}
		@Override protected void updateWidgetNarration(NarrationElementOutput output) {
			output.add(NarratedElementType.TITLE, getMessage());
			if (!usage.getString().isEmpty()) output.add(NarratedElementType.USAGE, usage);
		}
	}

	/** Buttons keep vanilla's keyboard, activation and narration, with drawn paper / ribbon presentation. */
	static class Tab extends Button {
		private final String icon;
		private final Component subtitle;
		private final boolean selected, dark;
		Tab(int x, int y, int w, int h, Component title, Component subtitle, String icon, boolean selected, boolean dark, OnPress action) {
			super(x, y, w, h, title, action, DEFAULT_NARRATION);
			this.icon = icon; this.subtitle = subtitle; this.selected = selected; this.dark = dark;
			setTooltip(Tooltip.create(subtitle.getString().isEmpty() ? title : title.copy().append("\n").append(subtitle)));
		}
		@Override protected void extractContents(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
			Font font = Minecraft.getInstance().font;
			int x = getX(), y = getY();
			boolean lit = isHovered() || isFocused();
			int edge = dark ? 0xFF847254 : GOLD, ink = dark ? 0xFFEBDDBB : INK;
			g.fill(x, y, x + width, y + height, dark ? (selected || lit ? 0xFF343E52 : 0xFF161E30) : (selected || lit ? 0xFFE6D3AD : 0x40C7A76C));
			g.fill(x, y + height - 1, x + width, y + height, dark ? 0xFF435066 : 0x50986B2E);
			if (selected || lit) { g.fill(x, y, x + 2, y + height, edge); }
			if (isFocused()) g.outline(x, y, width, height, edge);
			int tx = x + 6;
			if (!icon.isEmpty()) { icon(g, icon, x + 5, y + (height - 16) / 2); tx += 22; }
			int ty = y + (height - 8) / 2;
			if (height >= 30 && !subtitle.getString().isEmpty()) ty = y + 6;
			String label = fit(font, getMessage().getString(), width <= 24 ? width - 4 : width - (tx - x) - 7);
            if (width <= 24) tx = x + (width - font.width(label)) / 2;
            g.text(font, label, tx, ty, active ? ink : (dark ? 0xFF8B91A1 : MUTED), false);
			if (height >= 30 && !subtitle.getString().isEmpty())
				g.text(font, fit(font, subtitle.getString(), width - (tx - x) - 7), tx, y + 18, dark ? 0xFFACAAB8 : MUTED, false);
		}
	}
}
