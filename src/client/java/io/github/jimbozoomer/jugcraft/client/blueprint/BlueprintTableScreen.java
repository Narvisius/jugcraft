package io.github.jimbozoomer.jugcraft.client.blueprint;

import io.github.jimbozoomer.jugcraft.blueprint.Blueprint;
import io.github.jimbozoomer.jugcraft.blueprint.BlueprintNetwork;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

/**
 * The Blueprint Table. LIBRARY: every blueprint the server knows (the mod's structures first, then imported
 * ones) with a front view, size and bill of materials; PRINT gives you the blueprint (free). IMPORT: paste the
 * text of a blueprint (.jugbp.json, for example one an AI designed) and IMPORT adds it to the library for
 * everyone on the server; problems are explained in plain words.
 */
public class BlueprintTableScreen extends Screen {
	private static final int W = 340;
	private static final int H = 220;
	private static final int ROW = 14;
	static BlueprintTableScreen open;

	private final BlockPos table;
	private int left;
	private int top;
	private boolean importing;
	private int selected;
	private int scroll;
	private String selectId = "";
	private int[][] preview;
	private String previewFor = "";
	private Button print;
	private MultiLineEditBox paste;
	private final List<Button> libraryButtons = new ArrayList<>();
	private final List<Button> importButtons = new ArrayList<>();
	private String result = "";
	private boolean resultOk;

	public BlueprintTableScreen(BlockPos table) {
		super(Component.translatable("block.jugcraft.blueprint_table"));
		this.table = table;
	}

	@Override
	protected void init() {
		open = this;
		left = (width - W) / 2;
		top = (height - H) / 2;
		libraryButtons.clear();
		importButtons.clear();
		addRenderableWidget(Button.builder(Component.literal("LIBRARY"), b -> setImporting(false)).bounds(left + W - 128, top + 4, 60, 14).build());
		addRenderableWidget(Button.builder(Component.literal("IMPORT"), b -> setImporting(true)).bounds(left + W - 64, top + 4, 58, 14).build());
		print = addRenderableWidget(Button.builder(Component.literal("PRINT"), b -> printSelected()).bounds(left + W - 76, top + H - 30, 68, 20).build());
		libraryButtons.add(print);
		paste = new MultiLineEditBox.Builder().setX(left + 8).setY(top + 34).setTextColor(0xFFD2C8C8).setCursorColor(SciFi.ACCENT)
				.setPlaceholder(Component.literal("{\"format\": 1, \"name\": \"...\", \"palette\": {...}, \"layers\": [...]}"))
				.build(font, W - 16, 112, Component.literal("Blueprint text"));
		paste.setCharacterLimit(Blueprint.MAX_CHARS);
		addRenderableWidget(paste);
		importButtons.add(addRenderableWidget(Button.builder(Component.literal("PASTE"), b -> {
			paste.setValue(Minecraft.getInstance().keyboardHandler.getClipboard());
			result = "";
		}).bounds(left + 8, top + H - 30, 80, 20).build()));
		importButtons.add(addRenderableWidget(Button.builder(Component.literal("CLEAR"), b -> {
			paste.setValue("");
			result = "";
		}).bounds(left + 94, top + H - 30, 80, 20).build()));
		importButtons.add(addRenderableWidget(Button.builder(Component.literal("IMPORT"), b -> sendImport()).bounds(left + W - 96, top + H - 30, 88, 20).build()));
		setImporting(importing);
	}

	private void setImporting(boolean value) {
		importing = value;
		libraryButtons.forEach(b -> b.visible = !importing);
		importButtons.forEach(b -> b.visible = importing);
		paste.visible = importing;
		if (importing) {
			setFocused(paste);
		}
	}

	@Override
	public void removed() {
		open = null;
		super.removed();
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	private List<Blueprint> library() {
		return Blueprint.all(true);
	}

	private void printSelected() {
		List<Blueprint> all = library();
		if (selected >= 0 && selected < all.size()) {
			ClientPlayNetworking.send(new BlueprintNetwork.PrintPayload(table, all.get(selected).id));
		}
	}

	private void sendImport() {
		String text = paste.getValue().strip();
		if (text.isEmpty()) {
			result = "Paste a blueprint first (PASTE takes what you copied).";
			resultOk = false;
			return;
		}
		int parts = (text.length() + BlueprintNetwork.CHUNK - 1) / BlueprintNetwork.CHUNK;
		for (int i = 0; i < parts; i++) {
			ClientPlayNetworking.send(new BlueprintNetwork.ImportPayload(i, parts,
					text.substring(i * BlueprintNetwork.CHUNK, Math.min(text.length(), (i + 1) * BlueprintNetwork.CHUNK))));
		}
		result = "Checking...";
		resultOk = true;
	}

	/** For the client game test: open IMPORT, paste what is on the clipboard and press IMPORT. */
	public void testImport() {
		setImporting(true);
		paste.setValue(Minecraft.getInstance().keyboardHandler.getClipboard());
		sendImport();
	}

	/** The server's answer to an import. */
	void importResult(BlueprintNetwork.ImportResultPayload payload) {
		result = payload.message();
		resultOk = payload.ok();
		if (payload.ok()) {
			selectId = payload.id();
		}
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (!importing) {
			double x = event.x(), y = event.y();
			if (x >= left + 8 && x < left + 140 && y >= top + 36 && y < top + 36 + rows() * ROW) {
				int index = scroll + (int) ((y - top - 36) / ROW);
				if (index < library().size()) {
					selected = index;
					return true;
				}
			}
		}
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean mouseScrolled(double x, double y, double dx, double dy) {
		if (!importing) {
			scroll = Math.max(0, Math.min(Math.max(0, library().size() - rows()), scroll - (int) Math.signum(dy)));
			return true;
		}
		return super.mouseScrolled(x, y, dx, dy);
	}

	private int rows() {
		return (H - 80) / ROW;
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
		super.extractBackground(g, mouseX, mouseY, delta);
		SciFi.frame(g, left, top, left + W, top + H);
		if (!importing) {
			g.fill(left + 6, top + 22, left + 142, top + H - 36, SciFi.PANEL);
			g.fill(left + 146, top + 22, left + W - 6, top + H - 36, SciFi.PANEL);
		} else {
			g.fill(left + 7, top + 33, left + W - 7, top + 147, 0xFF3C2826);
			g.fill(left + 8, top + 34, left + W - 8, top + 146, 0xFF080708);
		}
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
		super.extractRenderState(g, mouseX, mouseY, delta);
		g.text(font, "| BLUEPRINT TABLE", left + 8, top + 7, SciFi.ACCENT, false);
		if (importing) {
			g.text(font, "Paste a blueprint (.jugbp.json text) below, then press IMPORT.", left + 8, top + 22, SciFi.DIM, false);
			if (!result.isEmpty()) {
				List<net.minecraft.util.FormattedCharSequence> lines = font.split(Component.literal(result), W - 20);
				for (int i = 0; i < Math.min(3, lines.size()); i++) {
					g.text(font, lines.get(i), left + 10, top + 152 + i * 10, resultOk ? SciFi.GOOD : SciFi.AMBER, false);
				}
			}
			return;
		}
		List<Blueprint> all = library();
		if (!selectId.isEmpty()) {
			for (int i = 0; i < all.size(); i++) {
				if (all.get(i).id.equals(selectId)) {
					selected = i;
					scroll = Math.max(0, Math.min(Math.max(0, all.size() - rows()), i - rows() / 2));
				}
			}
			selectId = "";
		}
		selected = Math.max(0, Math.min(all.size() - 1, selected));
		g.text(font, "LIBRARY", left + 10, top + 25, SciFi.DIM, false);
		for (int i = 0; i < rows() && scroll + i < all.size(); i++) {
			Blueprint b = all.get(scroll + i);
			int y = top + 36 + i * ROW;
			if (scroll + i == selected) {
				g.fill(left + 8, y - 2, left + 140, y + ROW - 3, SciFi.SELECTED);
			}
			g.text(font, SciFi.fit(font, b.name, 126), left + 11, y, b.source.equals("imported") ? SciFi.AMBER : SciFi.WHITE, false);
		}
		g.text(font, all.size() + " blueprints", left + 10, top + H - 48, SciFi.DIM, false);
		print.active = !all.isEmpty();
		if (all.isEmpty()) {
			g.text(font, "No blueprints yet", left + 152, top + 28, SciFi.DIM, false);
			return;
		}
		Blueprint b = all.get(selected);
		drawPreview(g, b, left + 150, top + 26, 100, H - 66);
		int x = left + 256;
		int y = top + 26;
		g.text(font, SciFi.fit(font, b.name.toUpperCase(java.util.Locale.ROOT), W - 262), x, y, SciFi.WHITE, false);
		g.text(font, b.sizeX + " x " + b.sizeY + " x " + b.sizeZ, x, y + 12, SciFi.DIM, false);
		g.text(font, b.size() + " blocks", x, y + 22, SciFi.DIM, false);
		g.text(font, b.source, x, y + 32, SciFi.DIM, false);
		int line = 0;
		for (Map.Entry<Block, Integer> entry : b.materials().entrySet()) {
			if (line >= 6) {
				break;
			}
			ItemStack stack = new ItemStack(entry.getKey().asItem());
			int ly = y + 46 + line * 17;
			g.item(stack, x, ly);
			g.text(font, SciFi.fit(font, entry.getValue() + " x " + stack.getHoverName().getString(), W - 284), x + 18, ly + 4, SciFi.AMBER, false);
			if (mouseX >= x && mouseX < x + 16 && mouseY >= ly && mouseY < ly + 16) {
				g.setTooltipForNextFrame(font, stack, mouseX, mouseY);
			}
			line++;
		}
		g.text(font, "Free to print", left + 150, top + H - 26, SciFi.DIM, false);
	}

	/** The front of the blueprint (as seen from the stake), each block in its map colour, scaled to fit. */
	private void drawPreview(GuiGraphicsExtractor g, Blueprint b, int x0, int y0, int w, int h) {
		if (!b.id.equals(previewFor)) {
			previewFor = b.id;
			int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE;
			for (Blueprint.Cell c : b.rawCells()) {
				minX = Math.min(minX, c.offset().getX());
				maxX = Math.max(maxX, c.offset().getX());
				minY = Math.min(minY, c.offset().getY());
				maxY = Math.max(maxY, c.offset().getY());
			}
			int pw = maxX - minX + 1, ph = maxY - minY + 1;
			int[][] colour = new int[pw][ph];
			int[][] depth = new int[pw][ph];
			for (int[] row : depth) {
				java.util.Arrays.fill(row, Integer.MIN_VALUE);
			}
			var level = Minecraft.getInstance().level;
			for (Blueprint.Cell c : b.rawCells()) {
				int px = c.offset().getX() - minX, py = c.offset().getY() - minY, z = c.offset().getZ();
				if (z > depth[px][py]) {
					depth[px][py] = z;
					int rgb = level == null ? 0x808080 : c.state().getMapColor(level, BlockPos.ZERO).col;
					colour[px][py] = 0xFF000000 | rgb;
				}
			}
			preview = colour;
		}
		int pw = preview.length, ph = preview[0].length;
		int scale = Math.max(1, Math.min(w / pw, h / ph));
		int ox = x0 + (w - pw * scale) / 2, oy = y0 + h - ph * scale;
		for (int px = 0; px < pw; px++) {
			for (int py = 0; py < ph; py++) {
				if (preview[px][py] != 0) {
					int sx = ox + px * scale, sy = oy + (ph - 1 - py) * scale;
					g.fill(sx, sy, sx + scale, sy + scale, preview[px][py]);
				}
			}
		}
	}
}
