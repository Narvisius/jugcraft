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
 * The Blueprint Table, in four tabs:
 * <ul>
 * <li>STRUCTURE SET: the mod's blueprints of several parts making a whole (a complete plant).</li>
 * <li>INDIVIDUAL STRUCTURES: one structure, not a part and not divided (a church).</li>
 * <li>PARTIAL STRUCTURES: one part of a set (a single cooling tower).</li>
 * <li>IMPORT: every blueprint this player has imported (kept on their computer, {@link ImportHistory}), and a
 * new import: paste the text of a blueprint (.jugbp.json, for example one an AI designed) and IMPORT adds it to
 * the server's library; problems are explained in plain words.</li>
 * </ul>
 * Each list shows a front view, size and bill of materials; PRINT gives you the blueprint (free).
 */
public class BlueprintTableScreen extends Screen {
	private static final int W = 410;
	private static final int H = 234;
	/** Everything below the tab row starts this much lower. */
	private static final int DY = 14;
	private static final int ROW = 14;
	private static final int SETS = 0, INDIVIDUAL = 1, PARTIAL = 2, IMPORT = 3;
	private static final Blueprint.Category[] CATEGORY = {Blueprint.Category.SET, Blueprint.Category.INDIVIDUAL, Blueprint.Category.PARTIAL};
	static BlueprintTableScreen open;

	private final BlockPos table;
	private int left;
	private int top;
	private int tab = INDIVIDUAL;
	private int selected;
	private int scroll;
	private String selectId = "";
	private int[][] preview;
	private String previewFor = "";
	private Button print;
	private Button importButton;
	private Button remove;
	private MultiLineEditBox paste;
	private final List<Button> tabs = new ArrayList<>();
	private final List<Button> pasteButtons = new ArrayList<>();
	private List<ImportHistory.Entry> history = List.of();
	/** The text sent with the last IMPORT, saved to the history once the server accepts it. */
	private String sent = "";
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
		tabs.clear();
		pasteButtons.clear();
		history = ImportHistory.list();
		String[] names = {"STRUCTURE SET", "INDIVIDUAL STRUCTURES", "PARTIAL STRUCTURES", "IMPORT"};
		int tx = left + 6;
		for (int i = 0; i < names.length; i++) {
			int which = i;
			int w = font.width(names[i]) + 12;
			tabs.add(addRenderableWidget(Button.builder(Component.literal(names[i]), b -> setTab(which)).bounds(tx, top + 18, w, 14).build()));
			tx += w + 3;
		}
		print = addRenderableWidget(Button.builder(Component.literal("PRINT"), b -> printSelected()).bounds(left + W - 44, top + H - 30, 38, 20).build());
		importButton = addRenderableWidget(Button.builder(Component.literal("IMPORT"), b -> sendImport()).bounds(left + W - 94, top + H - 30, 48, 20).build());
		remove = addRenderableWidget(Button.builder(Component.literal("REMOVE"), b -> removeSelected()).bounds(left + 150, top + H - 30, 46, 20).build());
		paste = new MultiLineEditBox.Builder().setX(left + 148).setY(top + 38 + DY).setTextColor(0xFFD2C8C8).setCursorColor(SciFi.ACCENT)
				.setPlaceholder(Component.literal("{\"format\": 1, \"name\": \"...\", \"palette\": {...}, \"layers\": [...]}"))
				.build(font, W - 156, H - 112 - DY, Component.literal("Blueprint text"));
		paste.setCharacterLimit(Blueprint.MAX_CHARS);
		addRenderableWidget(paste);
		pasteButtons.add(addRenderableWidget(Button.builder(Component.literal("PASTE"), b -> {
			paste.setValue(Minecraft.getInstance().keyboardHandler.getClipboard());
			result = "";
		}).bounds(left + 150, top + H - 30, 46, 20).build()));
		pasteButtons.add(addRenderableWidget(Button.builder(Component.literal("CLEAR"), b -> {
			paste.setValue("");
			result = "";
		}).bounds(left + 199, top + H - 30, 44, 20).build()));
		setTab(tab);
	}

	private void setTab(int value) {
		if (value != tab) {
			selected = 0;
			scroll = 0;
			result = "";
		}
		tab = value;
		for (int i = 0; i < tabs.size(); i++) {
			tabs.get(i).active = i != tab;      // the open tab shows pressed
		}
		updateWidgets();
	}

	/** Which buttons show: the paste box and its buttons only on IMPORT's "new import" row. */
	private void updateWidgets() {
		boolean importing = tab == IMPORT;
		boolean newImport = importing && selected == 0;
		paste.visible = newImport;
		pasteButtons.forEach(b -> b.visible = newImport);
		importButton.visible = importing;
		remove.visible = importing && !newImport;
		print.visible = true;
		if (newImport) {
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

	/** The mod's own blueprints in the open tab: sets, individual structures or partial structures. */
	private List<Blueprint> library() {
		if (tab == IMPORT) {
			return List.of();
		}
		Blueprint.Category category = CATEGORY[tab];
		return Blueprint.all(true).stream().filter(b -> !b.source.equals("imported") && b.category == category).toList();
	}

	/** Rows in the open tab's list: on IMPORT, "+ NEW IMPORT" first, then the player's saved imports. */
	private int rowCount() {
		return tab == IMPORT ? history.size() + 1 : library().size();
	}

	/** The blueprint the right-hand side shows, or null (nothing, or a new import). */
	private Blueprint shown() {
		if (tab == IMPORT) {
			return selected > 0 && selected - 1 < history.size() ? history.get(selected - 1).blueprint() : null;
		}
		List<Blueprint> all = library();
		return all.isEmpty() ? null : all.get(Math.max(0, Math.min(all.size() - 1, selected)));
	}

	/** The server's copy of a saved import, if the server has it (so it can be printed). */
	private Blueprint onServer(ImportHistory.Entry entry) {
		return Blueprint.get("import/" + entry.slug(), true);
	}

	private void printSelected() {
		if (tab == IMPORT) {
			if (selected > 0 && selected - 1 < history.size()) {
				Blueprint server = onServer(history.get(selected - 1));
				if (server != null) {
					ClientPlayNetworking.send(new BlueprintNetwork.PrintPayload(table, server.id));
				}
			}
			return;
		}
		Blueprint b = shown();
		if (b != null) {
			ClientPlayNetworking.send(new BlueprintNetwork.PrintPayload(table, b.id));
		}
	}

	private void removeSelected() {
		if (tab == IMPORT && selected > 0 && selected - 1 < history.size()) {
			ImportHistory.remove(history.get(selected - 1).slug());
			history = ImportHistory.list();
			selected = Math.min(selected, history.size());
			updateWidgets();
		}
	}

	private void sendImport() {
		String text;
		if (selected == 0) {
			text = paste.getValue().strip();
		} else if (selected - 1 < history.size()) {
			text = history.get(selected - 1).text();
		} else {
			return;
		}
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
		sent = text;
		result = "Checking...";
		resultOk = true;
	}

	/** For the client game test: open IMPORT, paste what is on the clipboard and press IMPORT. */
	public void testImport() {
		setTab(IMPORT);
		selected = 0;
		updateWidgets();
		paste.setValue(Minecraft.getInstance().keyboardHandler.getClipboard());
		sendImport();
	}

	/** The server's answer to an import: on success the text is kept in the player's import history. */
	void importResult(BlueprintNetwork.ImportResultPayload payload) {
		result = payload.message();
		resultOk = payload.ok();
		if (payload.ok() && !sent.isEmpty()) {
			String slug = ImportHistory.save(sent);
			history = ImportHistory.list();
			for (int i = 0; i < history.size(); i++) {
				if (history.get(i).slug().equals(slug)) {
					selected = i + 1;
				}
			}
			paste.setValue("");
			updateWidgets();
		}
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		double x = event.x(), y = event.y();
		if (x >= left + 8 && x < left + 140 && y >= top + 36 + DY && y < top + 36 + DY + rows() * ROW) {
			int index = scroll + (int) ((y - top - 36 - DY) / ROW);
			if (index < rowCount()) {
				selected = index;
				if (tab == IMPORT) {
					result = "";
				}
				updateWidgets();
				return true;
			}
		}
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean mouseScrolled(double x, double y, double dx, double dy) {
		if (x < left + 142) {
			scroll = Math.max(0, Math.min(Math.max(0, rowCount() - rows()), scroll - (int) Math.signum(dy)));
			return true;
		}
		return super.mouseScrolled(x, y, dx, dy);
	}

	private int rows() {
		return (H - 80 - DY) / ROW;
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
		super.extractBackground(g, mouseX, mouseY, delta);
		SciFi.frame(g, left, top, left + W, top + H);
		g.fill(left + 6, top + 22 + DY, left + 142, top + H - 36, SciFi.PANEL);
		g.fill(left + 146, top + 22 + DY, left + W - 6, top + H - 36, SciFi.PANEL);
		if (paste.visible) {
			g.fill(left + 147, top + 37 + DY, left + W - 7, top + H - 73, 0xFF3C2826);
			g.fill(left + 148, top + 38 + DY, left + W - 8, top + H - 74, 0xFF080708);
		}
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
		super.extractRenderState(g, mouseX, mouseY, delta);
		g.text(font, "| BLUEPRINT TABLE", left + 8, top + 7, SciFi.ACCENT, false);
		if (tab != IMPORT && !selectId.isEmpty()) {
			List<Blueprint> all = library();
			for (int i = 0; i < all.size(); i++) {
				if (all.get(i).id.equals(selectId)) {
					selected = i;
					scroll = Math.max(0, Math.min(Math.max(0, all.size() - rows()), i - rows() / 2));
				}
			}
			selectId = "";
		}
		selected = Math.max(0, Math.min(rowCount() - 1, selected));
		g.text(font, new String[] {"SETS", "STRUCTURES", "PARTS", "YOUR IMPORTS"}[tab], left + 10, top + 25 + DY, SciFi.DIM, false);
		for (int i = 0; i < rows() && scroll + i < rowCount(); i++) {
			int index = scroll + i;
			int y = top + 36 + DY + i * ROW;
			if (index == selected) {
				g.fill(left + 8, y - 2, left + 140, y + ROW - 3, SciFi.SELECTED);
			}
			String name;
			int colour;
			if (tab == IMPORT) {
				name = index == 0 ? "+ NEW IMPORT" : history.get(index - 1).blueprint().name;
				colour = index == 0 ? SciFi.ACCENT : SciFi.AMBER;
			} else {
				name = library().get(index).name;
				colour = SciFi.WHITE;
			}
			g.text(font, SciFi.fit(font, name, 126), left + 11, y, colour, false);
		}
		int count = tab == IMPORT ? history.size() : rowCount();
		g.text(font, count + (tab == IMPORT ? " saved on this computer" : count == 1 ? " blueprint" : " blueprints"), left + 10, top + H - 48, SciFi.DIM, false);
		if (tab == IMPORT && !result.isEmpty()) {
			List<net.minecraft.util.FormattedCharSequence> lines = font.split(Component.literal(result), 128);
			int n = Math.min(4, lines.size());
			for (int i = 0; i < n; i++) {
				g.text(font, lines.get(i), left + 10, top + H - 60 - (n - i) * 10, resultOk ? SciFi.GOOD : SciFi.AMBER, false);
			}
		}
		if (tab == IMPORT && selected == 0) {
			g.text(font, "Paste a blueprint (.jugbp.json text), then IMPORT.", left + 150, top + 26 + DY, SciFi.DIM, false);
			importButton.active = true;
			print.active = false;
			return;
		}
		Blueprint b = shown();
		if (tab == IMPORT) {
			ImportHistory.Entry entry = selected - 1 < history.size() ? history.get(selected - 1) : null;
			print.active = entry != null && onServer(entry) != null;
			importButton.active = entry != null;
		} else {
			print.active = b != null;
		}
		if (b == null) {
			g.text(font, new String[] {"No structure sets yet", "No blueprints yet", "No partial structures yet", ""}[tab], left + 152, top + 28 + DY, SciFi.DIM, false);
			return;
		}
		drawPreview(g, b, left + 150, top + 26 + DY, 100, H - 66 - DY);
		int x = left + 256;
		int y = top + 26 + DY;
		g.text(font, SciFi.fit(font, b.name.toUpperCase(java.util.Locale.ROOT), W - 262), x, y, SciFi.WHITE, false);
		g.text(font, b.sizeX + " x " + b.sizeY + " x " + b.sizeZ, x, y + 12, SciFi.DIM, false);
		g.text(font, b.size() + " blocks", x, y + 22, SciFi.DIM, false);
		g.text(font, tab == IMPORT ? (print.active ? "on this server" : "not on this server yet") : b.source, x, y + 32, SciFi.DIM, false);
		int line = 0;
		for (Map.Entry<Block, Integer> entry : b.materials().entrySet()) {
			if (line >= (tab == IMPORT ? 4 : 6)) {
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
		if (tab != IMPORT) {
			g.text(font, "Free to print", left + 150, top + H - 26, SciFi.DIM, false);
		}
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
