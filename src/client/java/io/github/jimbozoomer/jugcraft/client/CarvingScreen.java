package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.jimbozoomer.jugcraft.agriculture.CarvePayload;
import io.github.jimbozoomer.jugcraft.agriculture.CarvingFace;
import io.github.jimbozoomer.jugcraft.agriculture.CarvingTemplates;
import io.github.jimbozoomer.jugcraft.agriculture.OpenCarvingPayload;
import io.github.jimbozoomer.jugcraft.agriculture.PumpkinCarving;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;

/**
 * The Carving Knife's screen: one side of a pumpkin as a grid, a pixel per block texel: 16x16 on a pumpkin,
 * 48x48 on a full-grown giant pumpkin. Left-drag carves with the chosen tool (Cut through, Shave the skin, or
 * Erase this session's strokes), right-drag erases. A carving only goes deeper: what was carved before this
 * screen opened is fixed. Mirror copies every stroke to the other half; starter faces (blown up on a giant)
 * and a Pumpkin Stencil held in the other hand can be pressed in; the candle shows it lit. Done sends the face
 * to the server, which checks it and carves ({@link io.github.jimbozoomer.jugcraft.agriculture.PumpkinCarvings}).
 */
public class CarvingScreen extends Screen {
	/** The grid is about this many GUI pixels a side, whatever the face size. */
	private static final int GRID_SPAN = 160;
	private static final int WIDTH = 304;
	private static final int HEIGHT = 222;
	/** The preview shows a face this many GUI pixels a side. */
	private static final int PREVIEW = 48;
	private static final int UNDO_LIMIT = 64;

	private static final int RIND = 0xFF6B3A10;
	private static final int PANEL = 0xFF2B1A0E;
	private static final int PANEL_LIGHT = 0xFF4A2E16;
	private static final int TEXT = 0xFFF5D9A8;
	private static final int TEXT_DIM = 0xFFB89468;
	private static final int SELECTED = 0xFFFFC040;
	private static final int SKIN = 0xFFE08A2A;
	private static final int SKIN_RIB = 0xFFC0661A;

	private enum Tool {
		CUT, SHAVE, ERASE
	}

	/** A face to press in: a starter face (named by its translation key) or the held stencil. */
	private record Pattern(String name, int[] face16) {
	}

	private final BlockPos pos;
	private final Direction side;
	private final int size;
	private final int cell;
	private final int grid;
	private final int[] brushes;
	private final int[] original;
	private final int[] face;
	private final boolean freeDraw;
	private final List<Pattern> patterns = new ArrayList<>();
	private final Deque<int[]> undo = new ArrayDeque<>();
	private Tool tool = Tool.CUT;
	private int brushIndex;
	private boolean mirror;
	private boolean litPreview;
	private int template;
	private boolean painting;
	private boolean erasing;
	private Component notice = Component.empty();
	private int left;
	private int top;
	private int gridX;
	private int gridY;
	private final Button[] toolButtons = new Button[Tool.values().length];

	public CarvingScreen(OpenCarvingPayload payload) {
		super(Component.translatable("screen.jugcraft.carving"));
		this.pos = payload.pos();
		this.side = payload.side();
		this.size = payload.face().size();
		this.cell = Math.max(1, GRID_SPAN / size);
		this.grid = size * cell;
		// A giant face is three times as wide, so its brushes are bigger.
		this.brushes = size == PumpkinCarving.SIZE ? new int[] {1, 2, 3} : new int[] {1, 3, 5, 7};
		this.original = payload.face().face().clone();
		this.face = payload.face().face().clone();
		this.freeDraw = payload.freeDraw();
		for (CarvingTemplates.Template each : CarvingTemplates.ALL) {
			patterns.add(new Pattern("carving.jugcraft.template." + each.id(), each.face()));
		}
		payload.stencil().ifPresent(stencil -> {
			patterns.add(new Pattern("screen.jugcraft.carving.stencil", stencil));
			template = patterns.size() - 1;
		});
		if (!freeDraw) {
			notice = Component.translatable("screen.jugcraft.carving.templates_only");
		}
	}

	private int brush() {
		return brushes[brushIndex];
	}

	@Override
	protected void init() {
		left = (width - WIDTH) / 2;
		top = (height - HEIGHT) / 2;
		gridX = left + 12;
		gridY = top + 24;
		int x = gridX + grid + 12;
		int y = gridY;
		int column = WIDTH - (x - left) - 12;
		Tool[] tools = Tool.values();
		int toolWidth = (column - 4) / tools.length;
		for (int i = 0; i < tools.length; i++) {
			Tool each = tools[i];
			toolButtons[i] = addRenderableWidget(Button.builder(Component.translatable("screen.jugcraft.carving." + each.name().toLowerCase()),
					button -> tool = each).bounds(x + i * (toolWidth + 2), y, toolWidth, 16).build());
			toolButtons[i].active = freeDraw;
		}
		int half = (column - 2) / 2;
		Button brushButton = addRenderableWidget(Button.builder(brushLabel(), button -> {
			brushIndex = (brushIndex + 1) % brushes.length;
			button.setMessage(brushLabel());
		}).bounds(x, y + 20, half, 16).build());
		Button mirrorButton = addRenderableWidget(Button.builder(mirrorLabel(), button -> {
			mirror = !mirror;
			button.setMessage(mirrorLabel());
		}).bounds(x + half + 2, y + 20, half, 16).build());
		brushButton.active = freeDraw;
		mirrorButton.active = freeDraw;
		addRenderableWidget(Button.builder(templateLabel(), button -> {
			template = (template + 1) % patterns.size();
			button.setMessage(templateLabel());
		}).bounds(x, y + 40, column - 36, 16).build());
		addRenderableWidget(Button.builder(Component.translatable("screen.jugcraft.carving.apply"), button -> applyTemplate())
				.bounds(x + column - 34, y + 40, 34, 16).build());
		addRenderableWidget(Button.builder(candleLabel(), button -> {
			litPreview = !litPreview;
			button.setMessage(candleLabel());
		}).bounds(x, y + 60, column, 16).build());
		addRenderableWidget(Button.builder(Component.translatable("screen.jugcraft.carving.undo"), button -> undo())
				.bounds(x, y + 80, half, 16).build());
		addRenderableWidget(Button.builder(Component.translatable("screen.jugcraft.carving.reset"), button -> {
			pushUndo();
			System.arraycopy(original, 0, face, 0, face.length);
		}).bounds(x + half + 2, y + 80, half, 16).build());
		addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> finish())
				.bounds(x, top + HEIGHT - 22, half, 16).build());
		addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), button -> onClose())
				.bounds(x + half + 2, top + HEIGHT - 22, half, 16).build());
	}

	private Component brushLabel() {
		return Component.translatable("screen.jugcraft.carving.brush", brush());
	}

	private Component mirrorLabel() {
		return Component.translatable(mirror ? "screen.jugcraft.carving.mirror_on" : "screen.jugcraft.carving.mirror_off");
	}

	private Component templateLabel() {
		return Component.translatable(patterns.get(template).name());
	}

	private Component candleLabel() {
		return Component.translatable(litPreview ? "screen.jugcraft.carving.candle_on" : "screen.jugcraft.carving.candle_off");
	}

	/** Sends the face to the server (only if anything changed) and closes. */
	private void finish() {
		if (!Arrays.equals(face, original)) {
			ClientPlayNetworking.send(new CarvePayload(pos, side, new CarvingFace.Sized(size, face.clone())));
		}
		onClose();
	}

	/**
	 * Presses the chosen starter face or stencil in (blown up to fit a giant face), deepening what is there
	 * (with free drawing) or replacing it exactly.
	 */
	private void applyTemplate() {
		int[] shape = CarvingFace.scale(patterns.get(template).face16(), size);
		int[] result = new int[face.length];
		for (int y = 0; y < size; y++) {
			for (int x = 0; x < size; x++) {
				int depth = CarvingFace.pixel(shape, size, x, y);
				CarvingFace.setPixel(result, size, x, y, freeDraw ? Math.max(depth, pixel(x, y)) : depth);
			}
		}
		if (!CarvingFace.deepensOnly(original, result, size)) {
			notice = Component.translatable("screen.jugcraft.carving.does_not_fit");
			return;
		}
		pushUndo();
		System.arraycopy(result, 0, face, 0, face.length);
	}

	private int pixel(int x, int y) {
		return CarvingFace.pixel(face, size, x, y);
	}

	private void pushUndo() {
		undo.push(face.clone());
		while (undo.size() > UNDO_LIMIT) {
			undo.removeLast();
		}
	}

	private void undo() {
		if (!undo.isEmpty()) {
			System.arraycopy(undo.pop(), 0, face, 0, face.length);
		}
	}

	/** The middle of a grid cell in GUI coordinates (the client game test clicks the grid through it). */
	public double cellCentreX(int x) {
		return gridX + x * cell + cell / 2.0;
	}

	public double cellCentreY(int y) {
		return gridY + y * cell + cell / 2.0;
	}

	private boolean inGrid(double mouseX, double mouseY) {
		return mouseX >= gridX && mouseX < gridX + grid && mouseY >= gridY && mouseY < gridY + grid;
	}

	/** Applies the brush (and its mirror image) at the mouse. */
	private void paint(double mouseX, double mouseY) {
		if (!inGrid(mouseX, mouseY)) {
			return;
		}
		int cx = (int) Math.floor((mouseX - gridX) / cell);
		int cy = (int) Math.floor((mouseY - gridY) / cell);
		int brush = brush();
		int low = -(brush - 1) / 2;
		int high = brush / 2;
		for (int dy = low; dy <= high; dy++) {
			for (int dx = low; dx <= high; dx++) {
				set(cx + dx, cy + dy);
				if (mirror) {
					set(size - 1 - (cx + dx), cy + dy);
				}
			}
		}
	}

	private void set(int x, int y) {
		if (x < 0 || y < 0 || x >= size || y >= size) {
			return;
		}
		int fixed = CarvingFace.pixel(original, size, x, y);
		int now = pixel(x, y);
		int depth;
		if (erasing || tool == Tool.ERASE) {
			depth = fixed;
		} else if (tool == Tool.CUT) {
			depth = PumpkinCarving.CUT;
		} else {
			depth = Math.max(now, PumpkinCarving.SHAVED); // shaving never fills a hole back in
		}
		CarvingFace.setPixel(face, size, x, y, Math.max(depth, fixed));
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (super.mouseClicked(event, doubleClick)) {
			return true;
		}
		// Minecraft 26.3 numbers mouse buttons from 1 (left 1, middle 2, right 3); older versions counted from 0.
		if (freeDraw && inGrid(event.x(), event.y())
				&& (event.button() == InputConstants.MOUSE_BUTTON_LEFT || event.button() == InputConstants.MOUSE_BUTTON_RIGHT)) {
			pushUndo();
			painting = true;
			erasing = event.button() == InputConstants.MOUSE_BUTTON_RIGHT;
			paint(event.x(), event.y());
			return true;
		}
		return false;
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
		if (painting) {
			paint(event.x(), event.y());
			return true;
		}
		return super.mouseDragged(event, dragX, dragY);
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		if (painting) {
			painting = false;
			// A click that changed nothing leaves nothing to undo.
			if (!undo.isEmpty() && Arrays.equals(undo.peek(), face)) {
				undo.pop();
			}
			return true;
		}
		return super.mouseReleased(event);
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		// Ctrl+Z (Cmd+Z on macOS), matched like vanilla's edit shortcuts: by the letter on the keyboard layout.
		if (event.shortcutKey() == InputConstants.KEYCODE_Z && event.hasControlDownWithQuirk() && !event.hasShiftDown() && !event.hasAltDown()) {
			undo();
			return true;
		}
		return super.keyPressed(event);
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		super.extractBackground(graphics, mouseX, mouseY, delta);
		graphics.fill(left - 2, top - 2, left + WIDTH + 2, top + HEIGHT + 2, RIND);
		graphics.fill(left, top, left + WIDTH, top + HEIGHT, PANEL);
		graphics.fill(left, top, left + WIDTH, top + 1, PANEL_LIGHT);
		graphics.fill(gridX - 2, gridY - 2, gridX + grid + 2, gridY + grid + 2, RIND);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		super.extractRenderState(graphics, mouseX, mouseY, delta);
		graphics.text(font, title, left + 12, top + 9, TEXT, false);

		// The face, a cell per texel, with faint lines between cells big enough to have them.
		for (int y = 0; y < size; y++) {
			for (int x = 0; x < size; x++) {
				int cx = gridX + x * cell;
				int cy = gridY + y * cell;
				graphics.fill(cx, cy, cx + cell, cy + cell, color(x, y));
				if (cell >= 6) {
					graphics.fill(cx, cy, cx + cell, cy + 1, 0x22000000);
					graphics.fill(cx, cy, cx + 1, cy + cell, 0x22000000);
				}
			}
		}
		// The brush under the mouse, and its mirror image.
		if (freeDraw && inGrid(mouseX, mouseY)) {
			int cx = (mouseX - gridX) / cell;
			int cy = (mouseY - gridY) / cell;
			int brush = brush();
			int low = -(brush - 1) / 2;
			int span = brush * cell;
			graphics.outline(gridX + (cx + low) * cell, gridY + (cy + low) * cell, span, span, 0xCCFFFFFF);
			if (mirror) {
				int mx = size - 1 - (cx + low + brush - 1);
				graphics.outline(gridX + mx * cell, gridY + (cy + low) * cell, span, span, 0x88FFFFFF);
			}
		}
		// The selected tool.
		Button selected = toolButtons[tool.ordinal()];
		if (freeDraw) {
			graphics.outline(selected.getX() - 1, selected.getY() - 1, selected.getWidth() + 2, selected.getHeight() + 2, SELECTED);
		}

		// The whole face small, as it will look on the pumpkin.
		int px = gridX + grid + 12;
		int py = gridY + 102;
		graphics.text(font, Component.translatable("screen.jugcraft.carving.preview"), px, py, TEXT_DIM, false);
		int scale = Math.max(1, PREVIEW / size);
		int span = size * scale;
		graphics.fill(px - 1, py + 11, px + span + 1, py + 12 + span, RIND);
		for (int y = 0; y < size; y++) {
			for (int x = 0; x < size; x++) {
				graphics.fill(px + x * scale, py + 12 + y * scale, px + (x + 1) * scale, py + 12 + (y + 1) * scale, color(x, y));
			}
		}

		Component hint = notice.getString().isEmpty() ? Component.translatable("screen.jugcraft.carving.hint") : notice;
		graphics.text(font, hint, gridX, gridY + grid + 8, notice.getString().isEmpty() ? TEXT_DIM : SELECTED, false);
	}

	/** A cell's colour: the pumpkin's skin with darker ribs, shaved flesh, or a hole (lit or not). */
	private int color(int x, int y) {
		int depth = pixel(x, y);
		if (depth == PumpkinCarving.SKIN) {
			// A rib every five texels of a pumpkin's side, as wide on a giant.
			return x * PumpkinCarving.SIZE / size % 5 == 2 ? SKIN_RIB : SKIN;
		}
		if (depth == PumpkinCarving.SHAVED) {
			return litPreview ? CarvingTextures.SHAVED_LIT : CarvingTextures.SHAVED;
		}
		boolean wall = y == 0 || pixel(x, y - 1) != PumpkinCarving.CUT;
		return litPreview ? (wall ? CarvingTextures.HOLE_WALL_LIT : CarvingTextures.HOLE_LIT) : (wall ? CarvingTextures.HOLE_WALL : CarvingTextures.HOLE);
	}
}
