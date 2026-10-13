package io.github.jimbozoomer.jugcraft.client;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.BenchStatus;
import io.github.jimbozoomer.jugcraft.concordance.KindledLanternItem;
import io.github.jimbozoomer.jugcraft.concordance.LampwrightBenchBlockEntity;
import io.github.jimbozoomer.jugcraft.concordance.LampwrightBenchMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/**
 * The Lampwright's Bench: the specimen dish and the work slot at the left, with the study's progress and the lantern's
 * Radiance beside them, and the four actions at the right. A greyed button says on hover what it needs; the server
 * decides ({@link LampwrightBenchBlockEntity#press}), the screen only shows what it was told.
 */
public class LampwrightBenchScreen extends AbstractContainerScreen<LampwrightBenchMenu> {
	private static final Identifier TEXTURE = Jugcraft.id("textures/gui/machine.png");
	private static final int DARK = 0xFF373737;
	private static final int SLOT = 0xFF8B8B8B;
	private static final int LIGHT = 0xFFFFFFFF;
	private static final int STUDY = 0xFFB89AE6;
	private static final int RADIANCE = 0xFFF4D27A;
	private static final int TEXT = 0xFF404040;
	private static final int BAR_X = 48;
	private static final int BAR_WIDTH = 42;
	private static final int BUTTON_X = 96;
	private static final int BUTTON_WIDTH = 72;
	/** The study (or cancel), kindle, infuse and channel buttons, top to bottom. */
	private final Button[] buttons = new Button[4];

	public LampwrightBenchScreen(LampwrightBenchMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
	}

	@Override
	protected void init() {
		super.init();
		String[] labels = {"study", "kindle", "infuse", "channel"};
		for (int row = 0; row < buttons.length; row++) {
			int index = row;
			buttons[row] = addRenderableWidget(Button.builder(Component.translatable("screen.jugcraft.concordance.bench." + labels[row]),
					button -> press(index)).bounds(leftPos + BUTTON_X, topPos + 16 + row * 16, BUTTON_WIDTH, 14).build());
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
			Button button = buttons[row];
			button.active = status.ready();
			button.setTooltip(status.ready() ? null : Tooltip.create(status.message(menu.channelFocus())));
			if (row == 0) {
				button.setMessage(Component.translatable(id == LampwrightBenchBlockEntity.BUTTON_CANCEL
						? "screen.jugcraft.concordance.bench.cancel" : "screen.jugcraft.concordance.bench.study"));
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
		graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, 0.0F, 0.0F, imageWidth, imageHeight, 256, 256);
		slotFrame(graphics, x + LampwrightBenchMenu.SPECIMEN_X, y + LampwrightBenchMenu.SPECIMEN_Y);
		slotFrame(graphics, x + LampwrightBenchMenu.WORK_X, y + LampwrightBenchMenu.WORK_Y);
		bar(graphics, x + BAR_X, y + LampwrightBenchMenu.SPECIMEN_Y + 12, menu.studying() == 0 ? 0 : menu.progress(BAR_WIDTH), STUDY);
		int charge = menu.charge();
		bar(graphics, x + BAR_X, y + LampwrightBenchMenu.WORK_Y + 12,
				charge <= 0 ? 0 : charge * BAR_WIDTH / KindledLanternItem.CAPACITY, RADIANCE);
	}

	private static void bar(GuiGraphicsExtractor graphics, int x, int y, int filled, int color) {
		graphics.fill(x, y, x + BAR_WIDTH, y + 4, DARK);
		if (filled > 0) {
			graphics.fill(x, y, x + filled, y + 4, color);
		}
	}

	@Override
	protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		super.extractLabels(graphics, mouseX, mouseY);
		graphics.text(font, Component.translatable("screen.jugcraft.concordance.bench.specimen").getString(), BAR_X,
				LampwrightBenchMenu.SPECIMEN_Y + 2, TEXT, false);
		int charge = menu.charge();
		String work = charge < 0 ? Component.translatable("screen.jugcraft.concordance.bench.work").getString()
				: charge + " / " + KindledLanternItem.CAPACITY;
		graphics.text(font, work, BAR_X, LampwrightBenchMenu.WORK_Y + 2, TEXT, false);
	}

	private static void slotFrame(GuiGraphicsExtractor graphics, int slotX, int slotY) {
		graphics.fill(slotX - 1, slotY - 1, slotX + 17, slotY + 17, DARK);
		graphics.fill(slotX, slotY, slotX + 17, slotY + 17, LIGHT);
		graphics.fill(slotX, slotY, slotX + 16, slotY + 16, SLOT);
	}
}
