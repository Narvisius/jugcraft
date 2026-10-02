package io.github.jimbozoomer.jugcraft.client.blueprint;

import io.github.jimbozoomer.jugcraft.blueprint.Blueprint;
import io.github.jimbozoomer.jugcraft.blueprint.BlueprintNetwork;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

/**
 * The Blueprint Table, drawn as a drafting sheet clipped to a steel board, with four folder tabs:
 * <ul>
 * <li>STRUCTURE SET: the mod's blueprints of several parts making a whole (a complete plant).</li>
 * <li>INDIVIDUAL STRUCTURES: one structure, not a part and not divided (a church).</li>
 * <li>PARTIAL STRUCTURES: one part of a set (a single cooling tower).</li>
 * <li>IMPORT: every blueprint this player has imported (kept on their computer, {@link ImportHistory}), and a
 * new import on tracing paper: paste the text of a blueprint (.jugbp.json, for example one an AI designed) and
 * IMPORT adds it to the server's library; problems are explained in plain words.</li>
 * </ul>
 * The left of the sheet is the drawing register; the selected blueprint turns slowly in the middle
 * ({@link BlueprintTurntable}), with its bill of materials and a title block on the right. PRINT gives you the
 * blueprint (free).
 */
public class BlueprintTableScreen extends Screen {
	private static final int W = 410;
	private static final int H = 250;
	private static final int ROW = 13;
	private static final int SETS = 0, INDIVIDUAL = 1, PARTIAL = 2, IMPORT = 3;
	private static final Blueprint.Category[] CATEGORY = {Blueprint.Category.SET, Blueprint.Category.INDIVIDUAL, Blueprint.Category.PARTIAL};
	private static final String[] TAB_NAMES = {"STRUCTURE SET", "INDIVIDUAL STRUCTURES", "PARTIAL STRUCTURES", "IMPORT"};
	/** The colour chip on each tab: the colour of that kind of blueprint item. */
	private static final int[] TAB_CHIP = {0xFF408CF0, 0xFF408CF0, 0xFF46BE64, 0xFFDC4638};
	private static final int STEEL = 0xFF363A42, STEEL_LIGHT = 0xFF525862, STEEL_DARK = 0xFF1E2126, SCREW = 0xFF7C8086;
	private static final int PAPER = 0xFF1A3E80, GRID = 0xFF2C549C, GRID_MAJOR = 0xFF406AB4, INK = 0xFFE2ECFA, PALE = 0xFF96B2DC;
	private static final int STAMP_RED = 0xFFB02A22, STAMP_GREY = 0xFF464E5C, AMBER = 0xFFFFC86E, GOOD = 0xFF9CE6A4;
	/** The turning preview: its size in GUI pixels. */
	private static final int PREVIEW_W = 132, PREVIEW_H = 160;
	static BlueprintTableScreen open;

	private final BlockPos table;
	private int left;
	private int top;
	private int tab = INDIVIDUAL;
	private int selected;
	private int scroll;
	private String selectId = "";
	private MultiLineEditBox paste;
	private BlueprintTurntable turntable;
	private final List<Stamp> stamps = new ArrayList<>();
	private final int[][] tabBounds = new int[4][];
	private List<ImportHistory.Entry> history = List.of();
	/** The text sent with the last IMPORT, saved to the history once the server accepts it. */
	private String sent = "";
	private String result = "";
	private boolean resultOk;

	/** A rubber-stamp button drawn on the sheet: it only works while {@code active}. */
	private record Stamp(String label, int x, int y, int w, int h, int colour, boolean active, Runnable action) {
		boolean over(double mx, double my) {
			return mx >= x && mx < x + w && my >= y && my < y + h;
		}
	}

	public BlueprintTableScreen(BlockPos table) {
		super(Component.translatable("block.jugcraft.blueprint_table"));
		this.table = table;
	}

	// The sheet, inside the steel board, and its parts.
	private int px0() {
		return left + 6;
	}

	private int py0() {
		return top + 30;
	}

	private int px1() {
		return left + W - 6;
	}

	private int py1() {
		return top + H - 6;
	}

	/** The right edge of the drawing register. */
	private int listRight() {
		return px0() + 112;
	}

	@Override
	protected void init() {
		open = this;
		left = (width - W) / 2;
		top = (height - H) / 2;
		history = ImportHistory.list();
		if (turntable == null) {
			turntable = new BlueprintTurntable(PREVIEW_W, PREVIEW_H);
		}
		paste = new MultiLineEditBox.Builder().setX(listRight() + 22).setY(py0() + 14).setTextColor(0xFF28303C).setCursorColor(0xFF28303C)
				.setShowBackground(false)
				.setPlaceholder(Component.literal("{\"format\": 1, \"name\": \"...\", \"palette\": {...}, \"layers\": [...]}"))
				.build(font, px1() - listRight() - 34, py1() - py0() - 62, Component.literal("Blueprint text"));
		paste.setCharacterLimit(Blueprint.MAX_CHARS);
		addRenderableWidget(paste);
		setTab(tab);
	}

	private void setTab(int value) {
		if (value != tab) {
			selected = 0;
			scroll = 0;
			result = "";
		}
		tab = value;
		updateWidgets();
	}

	/** The paste box shows only on IMPORT's "new import" row. */
	private void updateWidgets() {
		boolean newImport = tab == IMPORT && selected == 0;
		paste.visible = newImport;
		if (newImport) {
			setFocused(paste);
		}
	}

	@Override
	public void removed() {
		open = null;
		if (turntable != null) {
			turntable.close();
			turntable = null;
		}
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

	/** The blueprint the sheet shows, or null (nothing, or a new import). */
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

	private Blueprint printable() {
		if (tab == IMPORT) {
			return selected > 0 && selected - 1 < history.size() ? onServer(history.get(selected - 1)) : null;
		}
		return shown();
	}

	private void printSelected() {
		Blueprint b = printable();
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

	/** The stamps for what is on the sheet now. */
	private void layoutStamps() {
		stamps.clear();
		int px1 = px1(), py0 = py0(), py1 = py1(), lr = listRight();
		boolean newImport = tab == IMPORT && selected == 0;
		if (newImport) {
			stamps.add(new Stamp("PASTE", lr + 16, py1 - 30, 44, 17, STAMP_GREY, true, () -> {
				paste.setValue(Minecraft.getInstance().keyboardHandler.getClipboard());
				result = "";
			}));
			stamps.add(new Stamp("CLEAR", lr + 64, py1 - 30, 44, 17, STAMP_GREY, true, () -> {
				paste.setValue("");
				result = "";
			}));
			stamps.add(new Stamp("IMPORT", px1 - 64, py1 - 30, 56, 17, STAMP_RED, true, this::sendImport));
			return;
		}
		stamps.add(new Stamp("PRINT", px1 - 60, py0 + 7, 52, 18, STAMP_RED, printable() != null, this::printSelected));
		if (tab == IMPORT && selected > 0) {
			stamps.add(new Stamp("IMPORT", px1 - 116, py0 + 7, 52, 18, STAMP_GREY, true, this::sendImport));
			stamps.add(new Stamp("REMOVE", px1 - 60, py0 + 28, 52, 18, STAMP_GREY, true, this::removeSelected));
		}
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		double x = event.x(), y = event.y();
		for (int i = 0; i < tabBounds.length; i++) {
			int[] b = tabBounds[i];
			if (b != null && x >= b[0] && x < b[2] && y >= b[1] && y < b[3]) {
				setTab(i);
				return true;
			}
		}
		for (Stamp stamp : List.copyOf(stamps)) {
			if (stamp.over(x, y)) {
				if (stamp.active()) {
					stamp.action().run();
					layoutStamps();
				}
				return true;
			}
		}
		int listTop = py0() + 22;
		if (x >= px0() + 4 && x < listRight() && y >= listTop && y < listTop + rows() * ROW) {
			int index = scroll + (int) ((y - listTop) / ROW);
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
		if (x < listRight()) {
			scroll = Math.max(0, Math.min(Math.max(0, rowCount() - rows()), scroll - (int) Math.signum(dy)));
			return true;
		}
		return super.mouseScrolled(x, y, dx, dy);
	}

	private int rows() {
		return (py1() - py0() - 50) / ROW;
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
		super.extractBackground(g, mouseX, mouseY, delta);
		drawBoard(g);
		drawTabs(g);
		drawSheet(g);
		if (paste.visible) {
			// tracing paper over the sheet, with a shadow
			int x0 = listRight() + 16, y0 = py0() + 8, x1 = px1() - 8, y1 = py1() - 36;
			g.fill(x0 + 3, y0 + 3, x1 + 3, y1 + 3, 0xFF12285A);
			g.fill(x0, y0, x1, y1, 0xFFD6DEE8);
			g.fill(x0, y0, x1, y0 + 1, 0xFFF0F4F8);
		}
	}

	/** The steel drawing board: a rim, four screws, the title and the drafting tools on the rail. */
	private void drawBoard(GuiGraphicsExtractor g) {
		g.fill(left - 1, top - 1, left + W + 1, top + H + 1, STEEL_DARK);
		g.fill(left, top, left + W, top + H, STEEL);
		g.outline(left + 2, top + 2, W - 4, H - 4, STEEL_LIGHT);
		for (int[] c : new int[][] {{left + 4, top + 4}, {left + W - 8, top + 4}, {left + 4, top + H - 8}, {left + W - 8, top + H - 8}}) {
			g.fill(c[0], c[1] + 1, c[0] + 4, c[1] + 3, SCREW);
			g.fill(c[0] + 1, c[1], c[0] + 3, c[1] + 4, SCREW);
			g.fill(c[0], c[1] + 2, c[0] + 4, c[1] + 3, STEEL_DARK);   // the slot
		}
		g.text(font, Component.literal("BLUEPRINT TABLE").withStyle(ChatFormatting.BOLD), left + 12, top + 6, 0xFFDEE2E8, false);
		int tx = left + W - 80, ty = top + 6, tool = 0xFFC8CCD2;
		g.outline(tx, ty + 1, 24, 5, tool);                              // a scale ruler
		for (int k = 2; k < 24; k += 3) {
			g.fill(tx + k, ty + 1, tx + k + 1, ty + 3, tool);
		}
		for (int k = 0; k < 9; k++) {                                     // a set square
			g.fill(tx + 32, ty + k, tx + 33, ty + k + 1, tool);
			g.fill(tx + 32 + k, ty + k, tx + 33 + k, ty + k + 1, tool);
		}
		g.fill(tx + 32, ty + 8, tx + 41, ty + 9, tool);
		for (int k = 0; k < 9; k++) {                                     // a compass
			g.fill(tx + 52 - k / 2, ty + k, tx + 53 - k / 2, ty + k + 1, tool);
			g.fill(tx + 52 + k / 2, ty + k, tx + 53 + k / 2, ty + k + 1, tool);
		}
		g.fill(tx + 51, ty - 2, tx + 54, ty + 1, tool);
	}

	/** Folder tabs along the top of the sheet, each with its colour chip; the open one joins the sheet. */
	private void drawTabs(GuiGraphicsExtractor g) {
		int x = px0();
		int bottom = py0();
		for (int i = 0; i < TAB_NAMES.length; i++) {
			int w = font.width(TAB_NAMES[i]) + 22;
			boolean active = i == tab;
			int fill = active ? PAPER : 0xFF3A424E;
			int edge = active ? INK : STEEL_LIGHT;
			int h = 13;
			for (int r = 0; r < h; r++) {                                 // a trapezoid, a row at a time
				int inset = (h - r) * 4 / h;
				int y = bottom - h + r;
				g.fill(x + inset, y, x + w - inset, y + 1, fill);
				g.fill(x + inset, y, x + inset + 1, y + 1, edge);
				g.fill(x + w - inset - 1, y, x + w - inset, y + 1, edge);
			}
			g.fill(x + 4, bottom - h, x + w - 4, bottom - h + 1, edge);
			g.fill(x + 8, bottom - 9, x + 13, bottom - 4, TAB_CHIP[i]);
			g.text(font, TAB_NAMES[i], x + 16, bottom - 10, active ? INK : PALE, false);
			tabBounds[i] = new int[] {x, bottom - h, x + w, bottom};
			x += w + 2;
		}
	}

	/** Blueprint paper with a fine and a coarse grid and an inked border. */
	private void drawSheet(GuiGraphicsExtractor g) {
		int x0 = px0(), y0 = py0(), x1 = px1(), y1 = py1();
		g.fill(x0, y0, x1, y1, PAPER);
		for (int x = x0 + 4; x < x1; x += 4) {
			g.fill(x, y0, x + 1, y1, (x - x0) % 20 == 0 ? GRID_MAJOR : GRID);
		}
		for (int y = y0 + 4; y < y1; y += 4) {
			g.fill(x0, y, x1, y + 1, (y - y0) % 20 == 0 ? GRID_MAJOR : GRID);
		}
		int[] tb = tabBounds[tab];
		g.outline(x0 + 2, y0 + 2, x1 - x0 - 4, y1 - y0 - 4, INK);
		if (tb != null) {
			g.fill(tb[0] + 1, y0, tb[2] - 1, y0 + 2, PAPER);         // the open tab runs into the sheet
		}
		g.fill(listRight() + 5, y0 + 6, listRight() + 6, y1 - 6, INK);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
		super.extractRenderState(g, mouseX, mouseY, delta);
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
		layoutStamps();
		drawRegister(g);
		Blueprint b = shown();
		if (tab == IMPORT && selected == 0) {
			g.text(font, "PASTE  .jugbp.json  TEXT HERE", listRight() + 22, py1() - 46, 0xFF78829A, false);
		} else if (b == null) {
			g.text(font, new String[] {"No structure sets yet", "No blueprints yet", "No partial structures yet", ""}[tab],
					listRight() + 16, py0() + 12, PALE, false);
		} else {
			drawDrawing(g, b, mouseX, mouseY);
		}
		for (Stamp stamp : stamps) {
			drawStamp(g, stamp, stamp.over(mouseX, mouseY));
		}
	}

	/** The drawing register: one numbered line per blueprint (or saved import), with its colour chip. */
	private void drawRegister(GuiGraphicsExtractor g) {
		int x0 = px0() + 8, x1 = listRight() - 2, y = py0() + 9;
		g.text(font, tab == IMPORT ? "YOUR IMPORTS" : "DRAWING REGISTER", x0, y, PALE, false);
		for (int i = 0; i < rows() && scroll + i < rowCount(); i++) {
			int index = scroll + i;
			int ry = py0() + 24 + i * ROW;
			if (index == selected) {
				g.outline(x0 - 4, ry - 3, x1 - x0 + 4, ROW, INK);
			}
			if (tab == IMPORT && index == 0) {
				g.text(font, Component.literal("+ NEW IMPORT").withStyle(ChatFormatting.BOLD), x0, ry, INK, false);
				continue;
			}
			String name = tab == IMPORT ? history.get(index - 1).blueprint().name : library().get(index).name;
			String number = String.format("%02d  ", tab == IMPORT ? index : index + 1);
			g.text(font, number + SciFi.fit(font, name, x1 - x0 - 12 - font.width(number)), x0, ry, tab == IMPORT ? AMBER : INK, false);
			g.fill(x1 - 8, ry + 1, x1 - 3, ry + 6, TAB_CHIP[tab]);
		}
		int count = tab == IMPORT ? history.size() : rowCount();
		g.text(font, count + (tab == IMPORT ? " saved locally" : count == 1 ? " drawing" : " drawings"), x0, py1() - 14, PALE, false);
		if (tab == IMPORT && !result.isEmpty()) {
			List<net.minecraft.util.FormattedCharSequence> lines = font.split(Component.literal(result), x1 - x0);
			int n = Math.min(5, lines.size());
			for (int i = 0; i < n; i++) {
				g.text(font, lines.get(i), x0, py1() - 26 - (n - i) * 10, resultOk ? GOOD : AMBER, false);
			}
		}
	}

	/** The selected blueprint: turning in the middle with a dimension line, materials and the title block. */
	private void drawDrawing(GuiGraphicsExtractor g, Blueprint b, int mouseX, int mouseY) {
		int vx = listRight() + 14, vy = py0() + 10;
		if (turntable != null) {
			turntable.draw(g, b, vx, vy);
		}
		int dy = vy + PREVIEW_H + 8;
		g.fill(vx + 4, dy, vx + PREVIEW_W - 4, dy + 1, INK);
		g.fill(vx + 4, dy - 3, vx + 5, dy + 4, INK);
		g.fill(vx + PREVIEW_W - 5, dy - 3, vx + PREVIEW_W - 4, dy + 4, INK);
		String size = b.sizeX + " x " + b.sizeY + " x " + b.sizeZ;
		g.text(font, size, vx + (PREVIEW_W - font.width(size)) / 2, dy + 4, INK, false);

		int rx = px1() - 136, rw = 128;
		if (tab != IMPORT) {
			g.text(font, "free", px1() - 60, py0() + 28, PALE, false);
		}
		int my = py0() + 50;
		g.text(font, "BILL OF MATERIALS", rx, my, PALE, false);
		int line = 0;
		for (Map.Entry<Block, Integer> entry : b.materials().entrySet()) {
			if (line >= 3) {
				break;
			}
			ItemStack stack = new ItemStack(entry.getKey().asItem());
			int ly = my + 11 + line * 17;
			g.item(stack, rx, ly);
			g.text(font, SciFi.fit(font, entry.getValue() + " x " + stack.getHoverName().getString(), rw - 20), rx + 18, ly + 4, INK, false);
			if (mouseX >= rx && mouseX < rx + 16 && mouseY >= ly && mouseY < ly + 16) {
				g.setTooltipForNextFrame(font, stack, mouseX, mouseY);
			}
			line++;
		}
		// the title block
		String type = switch (b.kind) {
			case PART -> "PARTIAL - GREEN";
			case IMPORTED -> "IMPORT - RED";
			default -> b.category == Blueprint.Category.SET ? "SET - BLUE" : "INDIVIDUAL - BLUE";
		};
		String[][] rows = {{"TITLE", b.name.toUpperCase(java.util.Locale.ROOT)}, {"TYPE", type},
				{"BLOCKS", String.format("%,d", b.size())},
				{"SOURCE", tab != IMPORT ? b.source : printable() != null ? "on this server" : "not on server yet"}};
		int ty0 = py1() - 8 - rows.length * 13;
		g.fill(rx - 2, ty0 - 2, px1() - 6, py1() - 6, PAPER);
		g.outline(rx - 2, ty0 - 2, px1() - 4 - rx, py1() - 4 - ty0, INK);
		for (int i = 0; i < rows.length; i++) {
			int y = ty0 + 2 + i * 13;
			if (i > 0) {
				g.fill(rx - 2, y - 3, px1() - 6, y - 2, INK);
			}
			g.text(font, rows[i][0], rx + 2, y, PALE, false);
			g.text(font, SciFi.fit(font, rows[i][1], rw - 44), rx + 42, y, i == 1 ? TAB_CHIP[tab] | 0xFF202020 : INK, false);
		}
	}

	/** A rubber stamp: a solid colour with a pale rim and bold white lettering; grey and dull while inactive. */
	private void drawStamp(GuiGraphicsExtractor g, Stamp s, boolean hover) {
		int colour = !s.active() ? 0xFF3C4250 : hover ? brighten(s.colour()) : s.colour();
		int rim = s.active() ? 0xFFF2CCC2 : 0xFF6C7482;
		if (s.colour() == STAMP_GREY && s.active()) {
			rim = 0xFFC8CED6;
		}
		g.fill(s.x() + 1, s.y(), s.x() + s.w() - 1, s.y() + s.h(), rim);
		g.fill(s.x(), s.y() + 1, s.x() + s.w(), s.y() + s.h() - 1, rim);
		g.fill(s.x() + 1, s.y() + 1, s.x() + s.w() - 1, s.y() + s.h() - 1, colour);
		Component text = Component.literal(s.label()).withStyle(ChatFormatting.BOLD);
		g.text(font, text, s.x() + (s.w() - font.width(text)) / 2, s.y() + (s.h() - 8) / 2 + 1, s.active() ? 0xFFFCF2EE : 0xFF8890A0, false);
	}

	private static int brighten(int argb) {
		int r = Math.min(255, ((argb >> 16) & 255) + 28), g = Math.min(255, ((argb >> 8) & 255) + 28), b = Math.min(255, (argb & 255) + 28);
		return 0xFF000000 | r << 16 | g << 8 | b;
	}
}
