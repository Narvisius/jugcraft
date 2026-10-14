package io.github.jimbozoomer.jugcraft.client;

import io.github.jimbozoomer.jugcraft.guide.encyclopedia.EncyclopediaPayload;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Util;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * The encyclopedia's drawn parts (Concordance Chunk A, A-04): a row's mark, a row of item icons and a recipe's crafting
 * grid. None takes the keyboard or speaks: the row's own text says what the mark means, and carries the items' names
 * for the narrator ({@link #names}). Hovering an item shows its tooltip.
 */
final class EncyclopediaWidgets {
	/** One item slot, with its frame. */
	static final int SLOT = 18;
	private static final int FRAME = 0xFF8B8B8B;
	private static final int WELL = 0xFF373737;
	private static final int DONE = 0xFF55C46A;
	private static final int NEXT = 0xFFFFC23D;
	private static final int OPEN = 0xFF6FC8F0;
	private static final int LATER = 0xFF9A96A8;
	private static final int BLOCKED = 0xFFF0605A;
	private static final int REQUIRED = 0xFFF2F0F8;
	private static final int RECOMMENDED = 0xFF6FC8F0;
	private static final int TRADE = 0xFFFFD98A;
	private static final int BOSS = 0xFFC79BF2;

	private EncyclopediaWidgets() {
	}

	/** The names of {@code items}, for a row's narration ("Amethyst Shard, Glowstone Dust"). */
	static Component names(List<Item> items) {
		MutableComponent out = Component.empty();
		for (int i = 0; i < items.size(); i++) {
			if (i > 0) {
				out.append(", ");
			}
			out.append(items.get(i).getDefaultInstance().getHoverName());
		}
		return out;
	}

	/** A grid in words, for its row's narration: what goes in, row by row, and what it makes. */
	static Component describe(EncyclopediaPayload.Grid grid) {
		MutableComponent slots = Component.empty();
		int shown = 0;
		for (List<Item> slot : grid.slots()) {
			if (slot.isEmpty()) {
				continue;
			}
			if (shown++ > 0) {
				slots.append(", ");
			}
			slots.append(slot.getFirst().getDefaultInstance().getHoverName());
		}
		return Component.translatable("encyclopedia.jugcraft.grid.narration", station(grid), slots, grid.count(), grid.result().getDefaultInstance().getHoverName());
	}

	static Component station(EncyclopediaPayload.Grid grid) {
		return Component.translatable(switch (grid.station()) {
			case EncyclopediaPayload.Grid.SHAPELESS -> "encyclopedia.jugcraft.grid.shapeless";
			case EncyclopediaPayload.Grid.STONECUTTER -> "encyclopedia.jugcraft.grid.stonecutter";
			default -> "encyclopedia.jugcraft.grid.shaped";
		});
	}

	/**
	 * A step's or a way's mark, 8 by 8, beside its words. A step: Done a filled square, Next an arrowhead, Can do now a
	 * hollow square, Later a dot, Not available a cross. A way: Required a filled diamond, Recommended a hollow one, Trade
	 * a ring, Optional (after a boss) a skull.
	 */
	static final class Mark extends AbstractWidget {
		static final int WIDTH = 10;
		private final EncyclopediaPayload.Row row;

		Mark(int x, EncyclopediaPayload.Row row) {
			super(x, 0, WIDTH, WIDTH, CommonComponents.EMPTY);
			this.row = row;
			active = false;
		}

		@Override
		protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
			int x = getX() + 1;
			int y = getY() + 1;
			if (row.kindOf() == EncyclopediaPayload.Kind.STEP) {
				switch (row.stateOf()) {
					case DONE -> graphics.fill(x, y, x + 8, y + 8, DONE);
					case NEXT -> {
						for (int r = 0; r < 8; r++) {
							int reach = Math.min(r, 7 - r);
							graphics.fill(x + 1, y + r, x + 2 + reach * 2, y + r + 1, NEXT);
						}
					}
					case OPEN -> graphics.outline(x, y, 8, 8, OPEN);
					case BLOCKED -> cross(graphics, x, y, BLOCKED);
					default -> graphics.fill(x + 2, y + 2, x + 6, y + 6, LATER);
				}
				return;
			}
			if (row.stateOf() == EncyclopediaPayload.StepState.BLOCKED) {
				cross(graphics, x, y, BLOCKED);
				return;
			}
			switch (row.edgeOf()) {
				case REQUIRED -> diamond(graphics, x, y, REQUIRED, true);
				case RECOMMENDED -> diamond(graphics, x, y, RECOMMENDED, false);
				case TRADE -> ring(graphics, x, y, TRADE);
				case OPTIONAL_BOSS -> skull(graphics, x, y, BOSS);
				default -> {
				}
			}
		}

		private static void cross(GuiGraphicsExtractor graphics, int x, int y, int colour) {
			for (int i = 0; i < 8; i++) {
				graphics.fill(x + i, y + i, x + i + 1, y + i + 1, colour);
				graphics.fill(x + 7 - i, y + i, x + 8 - i, y + i + 1, colour);
			}
		}

		private static void diamond(GuiGraphicsExtractor graphics, int x, int y, int colour, boolean filled) {
			for (int r = 0; r < 8; r++) {
				int half = r < 4 ? r : 7 - r;
				if (filled) {
					graphics.fill(x + 3 - half, y + r, x + 5 + half, y + r + 1, colour);
				} else {
					graphics.fill(x + 3 - half, y + r, x + 4 - half, y + r + 1, colour);
					graphics.fill(x + 4 + half, y + r, x + 5 + half, y + r + 1, colour);
				}
			}
		}

		private static void ring(GuiGraphicsExtractor graphics, int x, int y, int colour) {
			graphics.fill(x + 2, y, x + 6, y + 1, colour);
			graphics.fill(x + 2, y + 7, x + 6, y + 8, colour);
			graphics.fill(x, y + 2, x + 1, y + 6, colour);
			graphics.fill(x + 7, y + 2, x + 8, y + 6, colour);
			graphics.fill(x + 1, y + 1, x + 2, y + 2, colour);
			graphics.fill(x + 6, y + 1, x + 7, y + 2, colour);
			graphics.fill(x + 1, y + 6, x + 2, y + 7, colour);
			graphics.fill(x + 6, y + 6, x + 7, y + 7, colour);
		}

		private static void skull(GuiGraphicsExtractor graphics, int x, int y, int colour) {
			graphics.fill(x + 1, y, x + 7, y + 6, colour);
			graphics.fill(x + 2, y + 6, x + 6, y + 8, colour);
			graphics.fill(x + 2, y + 2, x + 3, y + 4, 0xFF141419);
			graphics.fill(x + 5, y + 2, x + 6, y + 4, 0xFF141419);
		}

		@Override
		protected void updateWidgetNarration(NarrationElementOutput output) {
		}
	}

	/** A row of items, each in a slot; hovering one shows its tooltip. */
	static final class Items extends AbstractWidget {
		private final Font font;
		private final List<ItemStack> stacks = new ArrayList<>();

		Items(int x, Font font, List<Item> items) {
			super(x, 0, Math.max(1, items.size()) * SLOT, SLOT, CommonComponents.EMPTY);
			this.font = font;
			items.forEach(item -> stacks.add(item.getDefaultInstance()));
			active = false;
		}

		@Override
		protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
			for (int i = 0; i < stacks.size(); i++) {
				slot(graphics, font, stacks.get(i), getX() + i * SLOT, getY(), mouseX, mouseY);
			}
		}

		@Override
		protected void updateWidgetNarration(NarrationElementOutput output) {
		}
	}

	/**
	 * A recipe's crafting grid as the server has it: its slots (a slot that takes any of several shows each in turn, one a
	 * second), an arrow, and what it makes with how many, and the station in words beside it.
	 */
	static final class Grid extends AbstractWidget {
		private final Font font;
		private final EncyclopediaPayload.Grid grid;
		private final int columns;
		private final int rows;

		Grid(int x, Font font, EncyclopediaPayload.Grid grid) {
			super(x, 0, 0, 0, CommonComponents.EMPTY);
			this.font = font;
			this.grid = grid;
			columns = grid.station() == EncyclopediaPayload.Grid.STONECUTTER ? 1 : Math.max(1, Math.min(3, grid.width()));
			rows = Math.max(1, (grid.slots().size() + columns - 1) / columns);
			setWidth(3 * SLOT + 14 + SLOT + 4 + font.width(station(grid)));
			setHeight(Math.max(rows, 1) * SLOT);
			active = false;
		}

		@Override
		protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
			long second = Util.getMillis() / 1000L;
			for (int i = 0; i < grid.slots().size(); i++) {
				int sx = getX() + (i % columns) * SLOT;
				int sy = getY() + (i / columns) * SLOT;
				List<Item> choices = grid.slots().get(i);
				ItemStack stack = choices.isEmpty() ? ItemStack.EMPTY : choices.get((int) (second % choices.size())).getDefaultInstance();
				slot(graphics, font, stack, sx, sy, mouseX, mouseY);
			}
			int arrowX = getX() + 3 * SLOT + 2;
			int middle = getY() + (rows * SLOT - font.lineHeight) / 2;
			graphics.text(font, "→", arrowX, middle, 0xFFC9C3D8, false);
			int resultX = arrowX + 12;
			int resultY = getY() + (rows * SLOT - SLOT) / 2;
			ItemStack result = new ItemStack(grid.result(), Math.max(1, grid.count()));
			slot(graphics, font, result, resultX, resultY, mouseX, mouseY);
			graphics.text(font, station(grid), resultX + SLOT + 4, middle, 0xFFC9C3D8, false);
		}

		@Override
		protected void updateWidgetNarration(NarrationElementOutput output) {
		}
	}

	/** One slot: its frame, its item and count, and the item's tooltip while the mouse is over it. */
	private static void slot(GuiGraphicsExtractor graphics, Font font, ItemStack stack, int x, int y, int mouseX, int mouseY) {
		graphics.fill(x, y, x + SLOT - 1, y + SLOT - 1, FRAME);
		graphics.fill(x + 1, y + 1, x + SLOT - 2, y + SLOT - 2, WELL);
		if (stack.isEmpty()) {
			return;
		}
		graphics.item(stack, x + 1, y + 1);
		graphics.itemDecorations(font, stack, x + 1, y + 1);
		if (mouseX >= x && mouseX < x + SLOT - 1 && mouseY >= y && mouseY < y + SLOT - 1) {
			graphics.setTooltipForNextFrame(font, stack, mouseX, mouseY);
		}
	}
}
