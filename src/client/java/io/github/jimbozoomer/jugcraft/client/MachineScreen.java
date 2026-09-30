package io.github.jimbozoomer.jugcraft.client;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.machine.MachineBlockEntity;
import io.github.jimbozoomer.jugcraft.machine.MachineKind;
import io.github.jimbozoomer.jugcraft.machine.MachineMenu;
import io.github.jimbozoomer.jugcraft.machine.SideConfig;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/** Screen for every machine: energy bar on the left, slots and progress in the middle. */
public class MachineScreen extends AbstractContainerScreen<MachineMenu> {
	private static final Identifier TEXTURE = Jugcraft.id("textures/gui/machine.png");
	private static final int BAR_X = 10;
	private static final int BAR_Y = 17;
	private static final int BAR_WIDTH = 12;
	private static final int BAR_HEIGHT = 52;

	private static final int DARK = 0xFF373737;
	private static final int SLOT = 0xFF8B8B8B;
	private static final int LIGHT = 0xFFFFFFFF;
	private static final int ENERGY = 0xFFE0B020;
	private static final int PROGRESS = 0xFF60A0E0;
	private static final int FLAME = 0xFFE06020;
	private static final int WATER = 0xFF3060D0;
	private static final int LAVA = 0xFFE87010;
	private static final int TEXT = 0xFF404040;

	// Side configuration: a cross of face buttons (front in the middle) and an eject toggle, on the right.
	private static final int[][] FACE_BUTTON_XY = {{150, 28}, {162, 40}, {138, 28}, {162, 28}, {150, 16}, {150, 40}};
	private static final String[] FACE_LETTERS = {"F", "B", "L", "R", "T", "D"};
	private static final int[] MODE_COLORS = {0x5AA0FF, 0xFFA040, 0x70E070, 0x9A9A9A};
	private static final int[] REDSTONE_COLORS = {0x9A9A9A, 0xFF4030, 0x802018};
	private final Button[] faceButtons = new Button[6];
	private Button ejectButton;
	private Button redstoneButton;
	private int shownSides = -1;

	public MachineScreen(MachineMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
	}

	@Override
	protected void init() {
		super.init();
		titleLabelX = (imageWidth - font.width(title)) / 2;
		shownSides = -1;
		if (!menu.kind().isProcessor()) {
			return;
		}
		for (int face = 0; face < faceButtons.length; face++) {
			int id = face;
			faceButtons[face] = addRenderableWidget(Button.builder(Component.literal(FACE_LETTERS[face]), button -> click(id))
					.bounds(leftPos + FACE_BUTTON_XY[face][0], topPos + FACE_BUTTON_XY[face][1], 11, 11).build());
		}
		ejectButton = addRenderableWidget(Button.builder(Component.empty(), button -> click(SideConfig.EJECT_BUTTON))
				.bounds(leftPos + 138, topPos + 54, 35, 12).build());
		redstoneButton = addRenderableWidget(Button.builder(Component.literal("R"), button -> click(SideConfig.REDSTONE_BUTTON))
				.bounds(leftPos + 162, topPos + 16, 11, 11).build());
	}

	private void click(int id) {
		if (minecraft != null && minecraft.gameMode != null) {
			minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
		}
	}

	/** Recolours the side buttons when the synced configuration changes. */
	private void refreshSideButtons() {
		int packed = menu.data(MachineBlockEntity.DATA_SIDES);
		if (ejectButton == null || packed == shownSides) {
			return;
		}
		shownSides = packed;
		SideConfig config = SideConfig.of(packed);
		for (SideConfig.Face face : SideConfig.Face.values()) {
			SideConfig.Mode mode = config.mode(face);
			Button button = faceButtons[face.ordinal()];
			button.setMessage(Component.literal(FACE_LETTERS[face.ordinal()]).withColor(MODE_COLORS[mode.ordinal()]));
			button.setTooltip(Tooltip.create(Component.translatable("container.jugcraft.side",
					Component.translatable("container.jugcraft.side." + face.name().toLowerCase()),
					Component.translatable("container.jugcraft.mode." + mode.name().toLowerCase()))));
		}
		ejectButton.setMessage(Component.translatable(config.eject() ? "container.jugcraft.eject.on" : "container.jugcraft.eject.off"));
		SideConfig.Redstone redstone = config.redstone();
		redstoneButton.setMessage(Component.literal("R").withColor(REDSTONE_COLORS[redstone.ordinal()]));
		redstoneButton.setTooltip(Tooltip.create(Component.translatable("container.jugcraft.redstone",
				Component.translatable("container.jugcraft.redstone." + redstone.name().toLowerCase()))));
		ejectButton.setTooltip(Tooltip.create(Component.translatable("container.jugcraft.eject.tooltip")));
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		extractBackground(graphics, mouseX, mouseY, delta);
		super.extractRenderState(graphics, mouseX, mouseY, delta);
		extractTooltip(graphics, mouseX, mouseY);
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		refreshSideButtons();
		int x = leftPos;
		int y = topPos;
		graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, 0.0F, 0.0F, imageWidth, imageHeight, 256, 256);

		MachineKind kind = menu.kind();
		if (kind.usesPower()) {
			// Energy bar, filled from the bottom.
			graphics.fill(x + BAR_X - 1, y + BAR_Y - 1, x + BAR_X + BAR_WIDTH + 1, y + BAR_Y + BAR_HEIGHT + 1, DARK);
			long capacity = Math.max(1, menu.capacity());
			int filled = (int) (menu.energy() * BAR_HEIGHT / capacity);
			graphics.fill(x + BAR_X, y + BAR_Y + BAR_HEIGHT - filled, x + BAR_X + BAR_WIDTH, y + BAR_Y + BAR_HEIGHT, ENERGY);
		}
		if (kind == MachineKind.COAL_GENERATOR) {
			slotFrame(graphics, x + MachineMenu.INPUT_X, y + MachineMenu.SLOT_Y);
			int maxBurn = Math.max(1, menu.data(MachineBlockEntity.DATA_MAX_BURN));
			int flame = menu.data(MachineBlockEntity.DATA_BURN) * 14 / maxBurn;
			graphics.fill(x + 57, y + 20 + 14 - flame, x + 71, y + 34, FLAME);
		} else if (kind == MachineKind.STEAM_GENERATOR) {
			slotFrame(graphics, x + MachineMenu.INPUT_X, y + 17);
			slotFrame(graphics, x + MachineMenu.INPUT_X, y + 53);
			slotFrame(graphics, x + MachineMenu.OUTPUT_X, y + MachineMenu.SLOT_Y);
			int maxBurn = Math.max(1, menu.data(MachineBlockEntity.DATA_MAX_BURN));
			int flame = menu.data(MachineBlockEntity.DATA_BURN) * 14 / maxBurn;
			graphics.fill(x + 57, y + 37 + 14 - flame, x + 71, y + 51, FLAME);
			// Water tank gauge on the right.
			graphics.fill(x + 149, y + BAR_Y - 1, x + 163, y + BAR_Y + BAR_HEIGHT + 1, DARK);
			int water = menu.data(MachineBlockEntity.DATA_TANK) * BAR_HEIGHT / MachineKind.STEAM_TANK;
			graphics.fill(x + 150, y + BAR_Y + BAR_HEIGHT - water, x + 162, y + BAR_Y + BAR_HEIGHT, WATER);
		} else if (kind == MachineKind.GEOTHERMAL_GENERATOR) {
			// Lava tank gauge on the right.
			graphics.fill(x + 149, y + BAR_Y - 1, x + 163, y + BAR_Y + BAR_HEIGHT + 1, DARK);
			int lava = menu.data(MachineBlockEntity.DATA_TANK) * BAR_HEIGHT / MachineKind.GEOTHERMAL_TANK;
			graphics.fill(x + 150, y + BAR_Y + BAR_HEIGHT - lava, x + 162, y + BAR_Y + BAR_HEIGHT, LAVA);
		} else if (kind.isProcessor()) {
			int inputs = kind.outputSlot();
			for (int slot = 0; slot < inputs; slot++) {
				slotFrame(graphics, x + MachineMenu.inputX(inputs, slot), y + MachineMenu.SLOT_Y);
			}
			slotFrame(graphics, x + MachineMenu.OUTPUT_X, y + MachineMenu.SLOT_Y);
			for (int index = 0; index < kind.byproductSlots(); index++) {
				slotFrame(graphics, x + MachineMenu.byproductX(index), y + MachineMenu.BYPRODUCT_Y);
			}
			for (int index = 0; index < kind.upgradeSlots(); index++) {
				slotFrame(graphics, x + MachineMenu.upgradeX(index), y + MachineMenu.UPGRADE_Y);
			}
			if (kind.tankCapacity() > 0) {
				// Water gauge between the energy bar and the input slot.
				graphics.fill(x + 29, y + BAR_Y - 1, x + 37, y + 53, DARK);
				int water = menu.data(MachineBlockEntity.DATA_TANK) * (52 - BAR_Y) / kind.tankCapacity();
				graphics.fill(x + 30, y + 52 - water, x + 36, y + 52, WATER);
			}
			int maxProgress = Math.max(1, menu.data(MachineBlockEntity.DATA_MAX_PROGRESS));
			int arrow = menu.data(MachineBlockEntity.DATA_PROGRESS) * 24 / maxProgress;
			graphics.fill(x + 80, y + 41, x + 104, y + 45, DARK);
			graphics.fill(x + 80, y + 41, x + 80 + arrow, y + 45, PROGRESS);
		}
	}

	@Override
	protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		super.extractLabels(graphics, mouseX, mouseY);
		if (menu.kind().usesPower()) {
			String energy = compact(menu.energy()) + " / " + compact(menu.capacity()) + " JE";
			graphics.text(font, energy, 28, 60, TEXT);
		}
		if (menu.kind() == MachineKind.ARC_FURNACE) {
			String key = menu.data(MachineBlockEntity.DATA_FORMED) == 1
					? "container.jugcraft.arc_furnace.formed" : "container.jugcraft.arc_furnace.incomplete";
			graphics.text(font, Component.translatable(key).getString(), 28, 18, TEXT);
		} else if (menu.kind() == MachineKind.WIND_TURBINE) {
			String key = menu.data(MachineBlockEntity.DATA_FORMED) == 1
					? "container.jugcraft.wind_turbine.clear" : "container.jugcraft.wind_turbine.blocked";
			graphics.text(font, Component.translatable(key).getString(), 28, 18, TEXT);
} else if (menu.kind() == MachineKind.WATER_WHEEL) {
			String key = menu.data(MachineBlockEntity.DATA_FORMED) == 1
					? "container.jugcraft.water_wheel.turning" : "container.jugcraft.water_wheel.still";
			graphics.text(font, Component.translatable(key).getString(), 28, 18, TEXT);
		}
	}

	/** 950, 12.5k, 400k: short enough to fit beside the upgrade slots. */
	private static String compact(long value) {
		if (value < 1_000) {
			return Long.toString(value);
		}
		if (value < 100_000) {
			long tenths = value / 100;
			return tenths % 10 == 0 ? tenths / 10 + "k" : tenths / 10 + "." + tenths % 10 + "k";
		}
		return value / 1_000 + "k";
	}

	private static void slotFrame(GuiGraphicsExtractor graphics, int slotX, int slotY) {
		graphics.fill(slotX - 1, slotY - 1, slotX + 17, slotY + 17, DARK);
		graphics.fill(slotX, slotY, slotX + 17, slotY + 17, LIGHT);
		graphics.fill(slotX, slotY, slotX + 16, slotY + 16, SLOT);
	}
}
