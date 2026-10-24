package io.github.jimbozoomer.jugcraft.client;

import io.github.jimbozoomer.jugcraft.concordance.BenchStatus;
import io.github.jimbozoomer.jugcraft.concordance.KindledLanternItem;
import io.github.jimbozoomer.jugcraft.concordance.LampwrightBenchBlockEntity;
import io.github.jimbozoomer.jugcraft.concordance.LampwrightBenchMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * The Lampwright's Bench, in the workshop look (docs/features/bench-screens.md): the specimen dish and the work slot at
 * the left, with the study's progress and the lantern's Radiance beside them, and the four actions as keys at the right.
 * A dimmed key says on hover what it needs; the status lamp is lit while a study runs. The server decides
 * ({@link LampwrightBenchBlockEntity#press}), the screen only shows what it was told.
 */
public class LampwrightBenchScreen extends AbstractContainerScreen<LampwrightBenchMenu> implements ScreenKit.Fitted {
	private static final int STUDY = 0xFFB89AE6;
	private static final int RADIANCE = 0xFFF4D27A;
	private static final int BAR_X = 48;
	private static final int BAR_WIDTH = 42;
	private static final int BUTTON_X = 96;
	private static final int BUTTON_WIDTH = 72;
	/** The casing and its glass end here; the inventory tray starts under them. */
	private static final int BODY_HEIGHT = 78;
	private final MachineScreenThemes.Theme theme = MachineScreenThemes.screen("lampwright_bench");
	/** The study (or cancel), kindle, infuse and channel keys, top to bottom. */
	private final ScreenKit.TextButton[] buttons = new ScreenKit.TextButton[4];

	public LampwrightBenchScreen(LampwrightBenchMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
	}

	@Override
	protected void init() {
		super.init();
		titleLabelX = 12;
		titleLabelY = 2;
		inventoryLabelY = 10_000;
		String[] labels = {"study", "kindle", "infuse", "channel"};
		for (int row = 0; row < buttons.length; row++) {
			int index = row;
			buttons[row] = addRenderableWidget(new ScreenKit.TextButton(leftPos + BUTTON_X, topPos + 16 + row * 15, BUTTON_WIDTH, 13, theme,
					Component.translatable("screen.jugcraft.concordance.bench." + labels[row]), button -> press(index)));
		}
		refresh();
	}

	/** The menu button a row stands for now: the first row cancels the viewer's own study while it runs. */
	private int buttonFor(int row) {
		return switch (row) {
			case 0 -> menu.studying() == 2 ? LampwrightBenchBlockEntity.BUTTON_CANCEL : LampwrightBenchBlockEntity.BUTTON_STUDY;
			case 1 -> LampwrightBenchBlockEntity.BUTTON_KINDLE;
			case 2 -> LampwrightBenchBlockEntity.BUTTON_INFUSE;
			default -> LampwrightBenchBlockEntity.BUTTON_CHANNEL;
		};
	}

	private void press(int row) {
		if (minecraft != null && minecraft.gameMode != null) {
			minecraft.gameMode.handleInventoryButtonClick(menu.containerId, buttonFor(row));
		}
	}

	private void refresh() {
		for (int row = 0; row < buttons.length; row++) {
			int id = buttonFor(row);
			BenchStatus status = menu.status(id);
			ScreenKit.TextButton button = buttons[row];
			button.active = status.ready();
			button.setTooltip(status.ready() ? null : Tooltip.create(status.message(menu.channelFocus())));
			if (row == 0) {
				button.setMessage(Component.translatable(id == LampwrightBenchBlockEntity.BUTTON_CANCEL
						? "screen.jugcraft.concordance.bench.cancel" : "screen.jugcraft.concordance.bench.study"));
				// Lit while this viewer's own study runs (the key then cancels it).
				button.setSelected(id == LampwrightBenchBlockEntity.BUTTON_CANCEL);
			}
		}
	}

	@Override
	protected void containerTick() {
		super.containerTick();
		refresh();
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		extractBackground(graphics, mouseX, mouseY, delta);
		super.extractRenderState(graphics, mouseX, mouseY, delta);
		extractTooltip(graphics, mouseX, mouseY);
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		int x = leftPos;
		int y = topPos;
		ScreenKit.body(graphics, theme, x, y, imageWidth, BODY_HEIGHT);
		ScreenKit.pane(graphics, theme, x + 3, y + 13, imageWidth - 6, BODY_HEIGHT - 15);
		ScreenKit.tray(graphics, theme, x, y + BODY_HEIGHT + 1, imageWidth, imageHeight - BODY_HEIGHT - 1);
		ScreenKit.inventory(graphics, theme, x + 8, y + 84);
		MachineSkin.lamp(graphics, x + 4, y + 3, menu.studying() != 0 ? theme.progress() : theme.dim());
		MachineSkin.slot(graphics, theme, x + LampwrightBenchMenu.SPECIMEN_X, y + LampwrightBenchMenu.SPECIMEN_Y);
		MachineSkin.slot(graphics, theme, x + LampwrightBenchMenu.WORK_X, y + LampwrightBenchMenu.WORK_Y);
		ScreenKit.bar(graphics, theme, x + BAR_X, y + LampwrightBenchMenu.SPECIMEN_Y + 12, BAR_WIDTH, 5,
				menu.studying() == 0 ? 0 : menu.progress(BAR_WIDTH) / (double) BAR_WIDTH, STUDY);
		int charge = menu.charge();
		ScreenKit.bar(graphics, theme, x + BAR_X, y + LampwrightBenchMenu.WORK_Y + 12, BAR_WIDTH, 5,
				charge <= 0 ? 0 : charge / (double) KindledLanternItem.CAPACITY, RADIANCE);
	}

	@Override
	protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		graphics.text(font, title, titleLabelX, titleLabelY, theme.label(), false);
		graphics.text(font, ScreenKit.fit(font, Component.translatable("screen.jugcraft.concordance.bench.specimen").getString(), BUTTON_X - BAR_X - 4),
				BAR_X, LampwrightBenchMenu.SPECIMEN_Y + 2, theme.dim(), false);
		int charge = menu.charge();
		String work = charge < 0 ? Component.translatable("screen.jugcraft.concordance.bench.work").getString()
				: charge + " / " + KindledLanternItem.CAPACITY;
		graphics.text(font, ScreenKit.fit(font, work, BUTTON_X - BAR_X - 4), BAR_X, LampwrightBenchMenu.WORK_Y + 2,
				charge < 0 ? theme.dim() : theme.text(), false);
	}

	@Override
	public ScreenRectangle panelArea() {
		return new ScreenRectangle(leftPos, topPos, imageWidth, imageHeight);
	}
}
