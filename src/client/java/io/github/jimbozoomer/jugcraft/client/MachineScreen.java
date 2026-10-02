package io.github.jimbozoomer.jugcraft.client;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.chemistry.FluidMachineSpec;
import io.github.jimbozoomer.jugcraft.chemistry.PetroFluids;
import io.github.jimbozoomer.jugcraft.machine.MachineBlockEntity;
import io.github.jimbozoomer.jugcraft.machine.MachineKind;
import io.github.jimbozoomer.jugcraft.machine.MachineMenu;
import io.github.jimbozoomer.jugcraft.machine.SideConfig;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariantAttributes;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.util.FormattedCharSequence;
import org.jspecify.annotations.Nullable;

/**
 * Screen for every machine (redesigned in batch 22): the machine bay (energy bar on the left, slots, gauges and
 * progress in the middle) over the player inventory, and a control terminal on the right whose screen says what the
 * machine is for and how it is doing (status, progress, power and its rate), with the side and redstone controls
 * below it. The look follows the machine's model: dieselpunk amber, electric green or lab teal
 * ({@link MachineScreenThemes}).
 */
public class MachineScreen extends AbstractContainerScreen<MachineMenu> {
	/** The machine bay and inventory; the terminal is joined to their right. */
	private static final int BAY_WIDTH = 176;
	private static final int TERMINAL_WIDTH = 92;
	/** The terminal's screen: text starts here and wraps at this width. */
	private static final int SCREEN_X = 185;
	private static final int SCREEN_Y = 9;
	private static final int SCREEN_WIDTH = 74;
	private static final int SCREEN_BOTTOM = 94;
	/** Below the screen: the side controls (processors) or more readouts. */
	private static final int CONTROLS_Y = 106;
	private static final int CONTROLS_BOTTOM = 137;
	private static final int BAR_X = 10;
	private static final int BAR_Y = 17;
	private static final int BAR_WIDTH = 12;
	private static final int BAR_HEIGHT = 52;

	private static final int FLAME = 0xFFE06020;
	private static final int WATER = 0xFF3060D0;
	private static final int LAVA = 0xFFE87010;

	// Side configuration in the terminal: a cross of face buttons (front in the middle), redstone, and eject.
	private static final int[][] FACE_BUTTON_XY = {{228, 117}, {240, 128}, {216, 117}, {240, 117}, {228, 106}, {228, 128}};
	private static final String[] FACE_LETTERS = {"F", "B", "L", "R", "T", "D"};
	private static final int[] MODE_COLORS = {0x5AA0FF, 0xFFA040, 0x70E070, 0x9A9A9A};
	private static final int[] REDSTONE_COLORS = {0x9A9A9A, 0xFF4030, 0x802018};
	private final Button[] faceButtons = new Button[6];
	private Button ejectButton;
	private Button redstoneButton;
	private int shownSides = -1;
	private final MachineScreenThemes.Theme theme;
	/** Energy a second ago (20 ticks of samples), for the rate and the charging/discharging status. */
	private final long[] energyHistory = new long[20];
	private int samples;

	public MachineScreen(MachineMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title, BAY_WIDTH + TERMINAL_WIDTH, 166);
		this.theme = MachineScreenThemes.of(menu.kind());
	}

	@Override
	protected void init() {
		super.init();
		titleLabelX = (BAY_WIDTH - font.width(title)) / 2;
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
				.bounds(leftPos + 182, topPos + 125, 32, 12).build());
		redstoneButton = addRenderableWidget(Button.builder(Component.literal("R"), button -> click(SideConfig.REDSTONE_BUTTON))
				.bounds(leftPos + 250, topPos + 106, 11, 11).build());
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
		// "Eject" fits the button; green when on, gray when off, and the tooltip says which.
		ejectButton.setMessage(Component.translatable("container.jugcraft.eject").withColor(config.eject() ? 0x70E070 : 0x9A9A9A));
		SideConfig.Redstone redstone = config.redstone();
		redstoneButton.setMessage(Component.literal("R").withColor(REDSTONE_COLORS[redstone.ordinal()]));
		redstoneButton.setTooltip(Tooltip.create(Component.translatable("container.jugcraft.redstone",
				Component.translatable("container.jugcraft.redstone." + redstone.name().toLowerCase()))));
		ejectButton.setTooltip(Tooltip.create(Component.translatable(config.eject() ? "container.jugcraft.eject.on" : "container.jugcraft.eject.off")
				.append(". ").append(Component.translatable("container.jugcraft.eject.tooltip"))));
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		extractBackground(graphics, mouseX, mouseY, delta);
		super.extractRenderState(graphics, mouseX, mouseY, delta);
		extractTooltip(graphics, mouseX, mouseY);
		extractGaugeTooltip(graphics, mouseX, mouseY);
	}

	/** Exact numbers when hovering the energy bar or a tank gauge. */
	private void extractGaugeTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		int mx = mouseX - leftPos;
		int my = mouseY - topPos;
		MachineKind kind = menu.kind();
		String line = null;
		if (kind.usesPower() && mx >= BAR_X - 1 && mx <= BAR_X + BAR_WIDTH && my >= BAR_Y - 1 && my <= BAR_Y + BAR_HEIGHT) {
			line = String.format("%,d / %,d JE", menu.energy(), menu.capacity());
		} else if (kind.tankCapacity() > 0 && my >= BAR_Y - 1 && my <= BAR_Y + BAR_HEIGHT
				&& (kind.isProcessor() ? mx >= 29 && mx < 37 : mx >= 149 && mx < 163)) {
			line = String.format("%,d / %,d mB", menu.data(MachineBlockEntity.DATA_TANK), kind.tankCapacity());
		}
		FluidMachineSpec spec = kind.fluidSpec();
		if (line == null && spec != null && my >= MachineMenu.TANK_Y - 1 && my <= MachineMenu.TANK_Y + MachineMenu.TANK_HEIGHT) {
			for (int tank = 0; tank < spec.tanks(); tank++) {
				int tankX = tankX(spec, tank);
				if (mx >= tankX - 1 && mx <= tankX + MachineMenu.TANK_WIDTH) {
					Component name = menu.tankFluid(tank) == 0 ? Component.translatable("container.jugcraft.tank.empty")
							: FluidVariantAttributes.getName(FluidVariant.of(fluid(tank)));
					graphics.setTooltipForNextFrame(font, name.copy().append(String.format(": %,d / %,d mB",
							menu.tankAmount(tank), spec.capacity(tank))), mouseX, mouseY);
					return;
				}
			}
		}
		if (line == null) {
			return;
		}
		graphics.setTooltipForNextFrame(font, Component.literal(line), mouseX, mouseY);
	}

	/** Samples the stored energy once a tick, for the terminal's rate. */
	@Override
	protected void containerTick() {
		super.containerTick();
		energyHistory[samples % energyHistory.length] = menu.energy();
		samples++;
	}

	/** JE per tick over the last second (positive while charging), or 0 until a second has been sampled. */
	private long energyRate() {
		if (samples <= energyHistory.length) {
			return 0;
		}
		long then = energyHistory[samples % energyHistory.length];
		return (menu.energy() - then) / energyHistory.length;
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		refreshSideButtons();
		int x = leftPos;
		int y = topPos;
		graphics.blit(RenderPipelines.GUI_TEXTURED, theme.texture(), x, y, 0.0F, 0.0F, imageWidth, imageHeight, 512, 256);

		MachineKind kind = menu.kind();
		if (kind.usesPower()) {
			energyBar(graphics, x + BAR_X, y + BAR_Y);
		}
		if (kind == MachineKind.COAL_GENERATOR) {
			slotFrame(graphics, x + MachineMenu.INPUT_X, y + MachineMenu.SLOT_Y);
			flame(graphics, x + 57, y + 20);
		} else if (kind.isBoiler()) {
			slotFrame(graphics, x + MachineMenu.INPUT_X, y + 17);
			slotFrame(graphics, x + MachineMenu.INPUT_X, y + 53);
			slotFrame(graphics, x + MachineMenu.OUTPUT_X, y + MachineMenu.SLOT_Y);
			flame(graphics, x + 57, y + 37);
			tube(graphics, x + 150, y + BAR_Y, 12, BAR_HEIGHT, menu.data(MachineBlockEntity.DATA_TANK), kind.tankCapacity(), WATER);
		} else if (kind == MachineKind.GEOTHERMAL_GENERATOR) {
			tube(graphics, x + 150, y + BAR_Y, 12, BAR_HEIGHT, menu.data(MachineBlockEntity.DATA_TANK),
					MachineKind.GEOTHERMAL_TANK, LAVA);
		} else if (kind.isFluidProcessor()) {
			FluidMachineSpec spec = kind.fluidSpec();
			for (int tank = 0; tank < spec.tanks(); tank++) {
				int color = menu.tankFluid(tank) != 0 ? fluidColor(fluid(tank)) : 0;
				tube(graphics, x + tankX(spec, tank), y + MachineMenu.TANK_Y, MachineMenu.TANK_WIDTH, MachineMenu.TANK_HEIGHT,
						menu.tankAmount(tank), spec.capacity(tank), color);
			}
			for (int slot = 0; slot < spec.itemInputs(); slot++) {
				slotFrame(graphics, x + MachineMenu.fluidItemInputX(spec, slot), y + MachineMenu.SLOT_Y);
			}
			for (int slot = 0; slot < spec.itemOutputs(); slot++) {
				slotFrame(graphics, x + MachineMenu.fluidItemOutputX(spec, slot), y + MachineMenu.SLOT_Y);
			}
			arrow(graphics, x + MachineMenu.FLUID_ARROW_X, y + 43);
		} else if (kind.isProcessor()) {
			int inputs = kind.outputSlot();
			for (int slot = 0; slot < inputs; slot++) {
				slotFrame(graphics, x + MachineMenu.inputX(kind, slot), y + MachineMenu.inputY(kind, slot));
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
				tube(graphics, x + 30, y + BAR_Y, 6, 52 - BAR_Y, menu.data(MachineBlockEntity.DATA_TANK), kind.tankCapacity(), WATER);
			}
			arrow(graphics, x + MachineMenu.arrowX(kind), y + 43);
		}
	}

	/** The energy bar: a column of lit segments, filled from the bottom. */
	private void energyBar(GuiGraphicsExtractor graphics, int left, int top) {
		graphics.fill(left - 1, top - 1, left + BAR_WIDTH + 1, top + BAR_HEIGHT + 1, theme.slotDark());
		graphics.fill(left, top, left + BAR_WIDTH, top + BAR_HEIGHT, theme.slotFace());
		long capacity = Math.max(1, menu.capacity());
		int filled = (int) (menu.energy() * BAR_HEIGHT / capacity);
		int unlit = (theme.energy() & 0x00FFFFFF) | 0x30000000;
		for (int segment = 0; segment * 4 < BAR_HEIGHT; segment++) {
			int bottom = top + BAR_HEIGHT - segment * 4;
			boolean lit = segment * 4 < filled;
			graphics.fill(left + 1, bottom - 3, left + BAR_WIDTH - 1, bottom, lit ? theme.energy() : unlit);
		}
	}

	/** A glass gauge tube: a dark frame, the fluid filled from the bottom, a highlight down the glass and tick marks. */
	private void tube(GuiGraphicsExtractor graphics, int left, int top, int width, int height, int amount, int capacity, int color) {
		graphics.fill(left - 1, top - 1, left + width + 1, top + height + 1, theme.slotDark());
		graphics.fill(left, top, left + width, top + height, theme.slotFace());
		int level = capacity <= 0 ? 0 : (int) ((long) amount * height / capacity);
		if (color != 0 && level > 0) {
			graphics.fill(left, top + height - level, left + width, top + height, color);
			graphics.fill(left, top + height - level, left + width, top + height - level + 1, 0x60FFFFFF);
		}
		graphics.fill(left + 1, top + 1, left + 2, top + height - 1, 0x30FFFFFF);
		for (int tick = 1; tick < 4; tick++) {
			int tickY = top + height - tick * height / 4;
			graphics.fill(left + width - 3, tickY, left + width, tickY + 1, theme.slotLight());
		}
	}

	/** The fuel flame: a column that burns down as the fuel does. */
	private void flame(GuiGraphicsExtractor graphics, int left, int top) {
		int maxBurn = Math.max(1, menu.data(MachineBlockEntity.DATA_MAX_BURN));
		int flame = menu.data(MachineBlockEntity.DATA_BURN) * 14 / maxBurn;
		graphics.fill(left - 1, top - 1, left + 15, top + 15, theme.slotDark());
		graphics.fill(left, top, left + 14, top + 14, theme.slotFace());
		graphics.fill(left, top + 14 - flame, left + 14, top + 14, FLAME);
		if (flame > 2) {
			graphics.fill(left + 4, top + 14 - flame + 2, left + 10, top + 14, 0xFFFFC040);
		}
	}

	/** The progress arrow: a shaft and a head pointing right, filled from the left as the work goes on. */
	private void arrow(GuiGraphicsExtractor graphics, int left, int middle) {
		int maxProgress = Math.max(1, menu.data(MachineBlockEntity.DATA_MAX_PROGRESS));
		int done = menu.data(MachineBlockEntity.DATA_PROGRESS) * 24 / maxProgress;
		for (int column = 0; column < 24; column++) {
			// A shaft 4 pixels high, then a head narrowing from 11 pixels to 1.
			int half = column < 18 ? 2 : 6 - (column - 18);
			int color = column < done ? theme.progress() : theme.slotDark();
			graphics.fill(left + column, middle - half, left + column + 1, middle + half, color);
		}
	}

	@Override
	protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		graphics.text(font, title, titleLabelX, titleLabelY, theme.label(), false);
		graphics.text(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, theme.label(), false);
		terminal(graphics);
	}

	/** The terminal's screen: what the machine is for, then its status and readouts. */
	private void terminal(GuiGraphicsExtractor graphics) {
		MachineKind kind = menu.kind();
		int y = SCREEN_Y;
		for (FormattedCharSequence line : font.split(Component.translatable("container.jugcraft.tagline." + kind.id), SCREEN_WIDTH)) {
			graphics.text(font, line, SCREEN_X, y, theme.dim(), false);
			y += 9;
		}
		y += 2;
		graphics.fill(SCREEN_X, y, SCREEN_X + SCREEN_WIDTH, y + 1, (theme.dim() & 0x00FFFFFF) | 0x80000000);
		y += 4;
		Status status = status();
		graphics.text(font, "> " + Component.translatable("container.jugcraft.terminal." + status.key).getString(),
				SCREEN_X, y, status.warning ? theme.warn() : theme.text(), false);
		y += 11;
		boolean controls = kind.isProcessor();
		int bottom = controls ? SCREEN_BOTTOM : CONTROLS_BOTTOM;
		if (hasProgress(kind) && y + 9 <= bottom) {
			int maxProgress = Math.max(1, menu.data(MachineBlockEntity.DATA_MAX_PROGRESS));
			int percent = Math.min(100, menu.data(MachineBlockEntity.DATA_PROGRESS) * 100 / maxProgress);
			meter(graphics, y, percent, theme.progress(), percent + "%");
			y += 10;
		}
		if (kind.usesPower() && kind != MachineKind.AUTO_CRAFTER && y + 9 <= bottom) {
			String energy = compact(menu.energy()) + "/" + compact(menu.capacity());
			graphics.text(font, Component.translatable("container.jugcraft.terminal.power").getString(), SCREEN_X, y, theme.dim(), false);
			graphics.text(font, energy, SCREEN_X + SCREEN_WIDTH - font.width(energy), y, theme.text(), false);
			y += 10;
		}
		long rate = energyRate();
		if (kind.usesPower() && rate != 0 && y + 9 <= bottom) {
			String text = (rate > 0 ? "+" : "") + compact(Math.abs(rate)) + " JE/t";
			if (rate < 0) {
				text = "-" + text;
			}
			graphics.text(font, Component.translatable("container.jugcraft.terminal.rate").getString(), SCREEN_X, y, theme.dim(), false);
			graphics.text(font, text, SCREEN_X + SCREEN_WIDTH - font.width(text), y, theme.text(), false);
			y += 10;
		}
		String condition = conditionKey(kind);
		if (condition != null) {
			for (FormattedCharSequence line : font.split(Component.translatable(condition), SCREEN_WIDTH)) {
				if (y + 9 > bottom) {
					break;
				}
				graphics.text(font, line, SCREEN_X, y, menu.data(MachineBlockEntity.DATA_FORMED) == 1 ? theme.text() : theme.warn(), false);
				y += 9;
			}
		}
		if (controls) {
			graphics.text(font, Component.translatable("container.jugcraft.terminal.sides").getString(), 183, CONTROLS_Y + 2,
					theme.dim(), false);
		}
	}

	/** A one-line meter: a bar of ten cells and a label on the right. */
	private void meter(GuiGraphicsExtractor graphics, int y, int percent, int color, String label) {
		int cells = 10;
		int width = SCREEN_WIDTH - font.width(label) - 4;
		int cell = Math.max(2, width / cells);
		int lit = percent * cells / 100;
		for (int i = 0; i < cells; i++) {
			int left = SCREEN_X + i * cell;
			graphics.fill(left, y + 1, left + cell - 1, y + 7, i < lit ? color : (color & 0x00FFFFFF) | 0x30000000);
		}
		graphics.text(font, label, SCREEN_X + SCREEN_WIDTH - font.width(label), y, theme.text(), false);
	}

	private record Status(String key, boolean warning) {
	}

	/** What the terminal says the machine is doing, from what the menu syncs. */
	private Status status() {
		MachineKind kind = menu.kind();
		long rate = energyRate();
		boolean burning = menu.data(MachineBlockEntity.DATA_BURN) > 0;
		boolean working = menu.data(MachineBlockEntity.DATA_PROGRESS) > 0;
		if (kind.isBattery()) {
			return new Status(rate > 0 ? "charging" : rate < 0 ? "discharging" : "storing", false);
		}
		if (kind.isGenerator()) {
			return new Status(burning || working || rate > 0 ? "generating" : "idle", false);
		}
		if (working) {
			return new Status("running", false);
		}
		if (kind.usesPower() && kind.usePerTick > 0 && menu.energy() < kind.usePerTick) {
			return new Status("no_power", true);
		}
		return new Status("idle", false);
	}

	/** Machines whose progress means a batch of work (not engines, which use it for their fuel). */
	private static boolean hasProgress(MachineKind kind) {
		return (kind.isProcessor() || kind.isFluidProcessor()) && !kind.isGenerator() && kind != MachineKind.DIESEL_ENGINE
				&& kind != MachineKind.ADVANCED_ENGINE;
	}

	/** The structure or site condition some machines report (formed or not, oil or dry...), or null. */
	private @Nullable String conditionKey(MachineKind kind) {
		boolean ok = menu.data(MachineBlockEntity.DATA_FORMED) == 1;
		return switch (kind) {
			case ARC_FURNACE -> ok ? "container.jugcraft.arc_furnace.formed" : "container.jugcraft.arc_furnace.incomplete";
			case WIND_TURBINE -> ok ? "container.jugcraft.wind_turbine.clear" : "container.jugcraft.wind_turbine.blocked";
			case FRACKING_RIG -> ok ? "container.jugcraft.fracking_rig.shale" : "container.jugcraft.fracking_rig.none";
			case PUMPJACK -> ok ? "container.jugcraft.pumpjack.oil" : "container.jugcraft.pumpjack.dry";
			case WATER_WHEEL -> ok ? "container.jugcraft.water_wheel.turning" : "container.jugcraft.water_wheel.still";
			default -> null;
		};
	}

	/** 950, 12.5k, 400k, 64M: short enough for the terminal. */
	private static String compact(long value) {
		if (value < 1_000) {
			return Long.toString(value);
		}
		if (value < 100_000) {
			long tenths = value / 100;
			return tenths % 10 == 0 ? tenths / 10 + "k" : tenths / 10 + "." + tenths % 10 + "k";
		}
		if (value < 10_000_000) {
			return value / 1_000 + "k";
		}
		return value / 1_000_000 + "M";
	}

	/** Left edge of a fluid processor's tank gauge (input tanks first). */
	private static int tankX(FluidMachineSpec spec, int tank) {
		return spec.isInput(tank) ? MachineMenu.inputTankX(tank) : MachineMenu.outputTankX(spec, tank - spec.inputTanks().size());
	}

	private Fluid fluid(int tank) {
		return BuiltInRegistries.FLUID.byId(menu.tankFluid(tank));
	}

	/** Gauge colour: water and lava as on the boilers, petroleum fluids their own, anything else a neutral blue-grey. */
	private static int fluidColor(Fluid fluid) {
		if (fluid == Fluids.WATER || fluid == Fluids.FLOWING_WATER) {
			return WATER;
		}
		if (fluid == Fluids.LAVA || fluid == Fluids.FLOWING_LAVA) {
			return LAVA;
		}
		int color = PetroFluids.gaugeColor(fluid);
		return color != 0 ? color : 0xFF8090A8;
	}

	/** A sunken slot in the theme's colours. */
	private void slotFrame(GuiGraphicsExtractor graphics, int slotX, int slotY) {
		graphics.fill(slotX - 1, slotY - 1, slotX + 17, slotY + 17, theme.slotDark());
		graphics.fill(slotX, slotY, slotX + 17, slotY + 17, theme.slotLight());
		graphics.fill(slotX, slotY, slotX + 16, slotY + 16, theme.slotFace());
	}
}
