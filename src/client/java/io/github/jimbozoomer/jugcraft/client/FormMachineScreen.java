package io.github.jimbozoomer.jugcraft.client;

import io.github.jimbozoomer.jugcraft.chemistry.FluidMachineSpec;
import io.github.jimbozoomer.jugcraft.chemistry.PetroFluids;
import io.github.jimbozoomer.jugcraft.machine.MachineMenu;
import io.github.jimbozoomer.jugcraft.machine.form.FormMachineBlockEntity;
import io.github.jimbozoomer.jugcraft.machine.form.FormMachineMenu;
import io.github.jimbozoomer.jugcraft.machine.form.MachineForm;
import io.github.jimbozoomer.jugcraft.machine.form.MachineStatus;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariantAttributes;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

/**
 * The screen of a formed industrial machine, in its family's {@link MachineScreenThemes theme}: the bay holds the
 * energy bar, tanks, slots and progress (one thin bar per lane under the arrow when a profile runs several batches),
 * and the terminal says the machine's state and the one thing to fix, in the order the plan gives: process, inputs,
 * tool, energy, reserved outputs and completion. Reserved room in an output tank shows as a pale band above its fluid.
 * The tool sockets and upgrade slots sit in the terminal's lower panel; Pause and Cancel are the local controls.
 *
 * <p>A generator form shows its fuel tank, an arrow lit while it makes power and its store on the right, where a
 * processor's outputs would be; the terminal gives the power it made last tick. It has no batches, so no Cancel.
 */
public class FormMachineScreen extends AbstractContainerScreen<FormMachineMenu> {
	private static final int BAY_WIDTH = 176;
	private static final int TERMINAL_WIDTH = 92;
	private static final int SCREEN_X = 185;
	private static final int SCREEN_Y = 9;
	private static final int SCREEN_WIDTH = 74;
	private static final int TEXT_BOTTOM = 80;
	private static final int BUTTON_Y = 82;
	private static final int BAR_X = 10;
	private static final int BAR_Y = 17;
	private static final int BAR_WIDTH = 12;
	private static final int BAR_HEIGHT = 52;
	/** A generator's store, at the right of the bay where its power goes out. */
	private static final int OUT_BAR_X = BAY_WIDTH - BAR_X - BAR_WIDTH;
	private static final int ARROW_Y = 43;
	private static final int WATER = 0xFF3060D0;
	private static final int LAVA = 0xFFE87010;

	private final MachineScreenThemes.Theme theme;
	private Button pauseButton;

	public FormMachineScreen(FormMachineMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title, BAY_WIDTH + TERMINAL_WIDTH, 166);
		this.theme = MachineScreenThemes.of(menu.form().family());
	}

	@Override
	protected void init() {
		super.init();
		titleLabelX = (BAY_WIDTH - font.width(title)) / 2;
		pauseButton = addRenderableWidget(Button.builder(Component.translatable("container.jugcraft.form.pause"),
				button -> click(FormMachineBlockEntity.BUTTON_PAUSE)).bounds(leftPos + SCREEN_X, topPos + BUTTON_Y, 36, 12).build());
		if (!menu.form().generator()) {
			addRenderableWidget(Button.builder(Component.translatable("container.jugcraft.form.cancel"),
					button -> click(FormMachineBlockEntity.BUTTON_CANCEL)).bounds(leftPos + SCREEN_X + 38, topPos + BUTTON_Y, 36, 12).build());
		}
	}

	/** Where the energy bar stands: a processor's on the left, before its inputs; a generator's on the right. */
	private int barX() {
		return menu.form().generator() ? OUT_BAR_X : BAR_X;
	}

	private void click(int id) {
		if (minecraft != null && minecraft.gameMode != null) {
			minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
		}
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		pauseButton.setMessage(Component.translatable(menu.paused() ? "container.jugcraft.form.resume" : "container.jugcraft.form.pause"));
		extractBackground(graphics, mouseX, mouseY, delta);
		super.extractRenderState(graphics, mouseX, mouseY, delta);
		extractTooltip(graphics, mouseX, mouseY);
		extractGaugeTooltip(graphics, mouseX, mouseY);
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		int x = leftPos;
		int y = topPos;
		graphics.blit(RenderPipelines.GUI_TEXTURED, theme.texture(), x, y, 0.0F, 0.0F, imageWidth, imageHeight, 512, 256);
		MachineForm form = menu.form();
		FluidMachineSpec spec = form.tanks();
		energyBar(graphics, x + barX(), y + BAR_Y);
		for (int tank = 0; tank < spec.tanks(); tank++) {
			int color = menu.tankFluid(tank) != 0 ? fluidColor(fluid(tank)) : 0;
			tube(graphics, x + tankX(spec, tank), y + MachineMenu.TANK_Y, menu.tankAmount(tank), menu.tankReserved(tank),
					spec.capacity(tank), color);
		}
		for (int slot = 0; slot < form.itemInputs(); slot++) {
			slotFrame(graphics, x + MachineMenu.fluidItemInputX(spec, slot), y + MachineMenu.SLOT_Y);
		}
		for (int slot = 0; slot < form.itemOutputs(); slot++) {
			slotFrame(graphics, x + MachineMenu.fluidItemOutputX(spec, slot), y + MachineMenu.SLOT_Y);
		}
		int tools = form.sockets().size() + form.upgradeSlots();
		for (int index = 0; index < tools; index++) {
			slotFrame(graphics, x + FormMachineMenu.toolX(index), y + FormMachineMenu.toolY(index));
			if (index < form.sockets().size() && menu.locked(index)) {
				graphics.fill(x + FormMachineMenu.toolX(index), y + FormMachineMenu.toolY(index) + 15,
						x + FormMachineMenu.toolX(index) + 16, y + FormMachineMenu.toolY(index) + 16, theme.warn());
			}
		}
		int arrowX = x + MachineMenu.fluidArrowX(spec);
		if (form.generator()) {
			arrow(graphics, arrowX, y + ARROW_Y, menu.laneUse() > 0 ? 1 : 0);
			return;
		}
		arrow(graphics, arrowX, y + ARROW_Y, fraction(0));
		int lanes = form.profile().lanes();
		for (int lane = 1; lane < lanes; lane++) {
			int top = y + ARROW_Y + 8 + 3 * (lane - 1);
			int done = (int) (fraction(lane) * 24);
			graphics.fill(arrowX, top, arrowX + 24, top + 2, theme.slotDark());
			graphics.fill(arrowX, top, arrowX + done, top + 2, theme.progress());
		}
	}

	/** How far lane {@code lane}'s batch is, 0 to 1 (0 when the lane is free). */
	private float fraction(int lane) {
		int duration = menu.laneDuration(lane);
		return duration <= 0 ? 0 : Math.min(1, menu.laneProgress(lane) / (float) duration);
	}

	@Override
	protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		graphics.text(font, title, titleLabelX, titleLabelY, theme.label(), false);
		graphics.text(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, theme.label(), false);
		terminal(graphics);
	}

	/** The terminal: state, then the reason, progress, power and lanes. */
	private void terminal(GuiGraphicsExtractor graphics) {
		MachineStatus status = menu.status();
		int y = SCREEN_Y;
		// The state wraps like the reason: "Waiting for input" is wider than the terminal.
		for (FormattedCharSequence line : font.split(Component.literal("> ").append(status.title(menu.form())), SCREEN_WIDTH)) {
			graphics.text(font, line, SCREEN_X, y, status.state().warning() ? theme.warn() : theme.text(), false);
			y += 9;
		}
		y += 2;
		for (FormattedCharSequence line : font.split(status.detail(menu.form()), SCREEN_WIDTH)) {
			if (y + 9 > TEXT_BOTTOM) {
				return;
			}
			graphics.text(font, line, SCREEN_X, y, status.state().warning() ? theme.warn() : theme.dim(), false);
			y += 9;
		}
		y += 2;
		if (menu.form().generator()) {
			if (y + 9 <= TEXT_BOTTOM) {
				row(graphics, y, Component.translatable("container.jugcraft.terminal.rate").getString(), menu.laneUse() + " JE/t");
				y += 10;
			}
			if (y + 9 <= TEXT_BOTTOM) {
				powerRow(graphics, y);
			}
			return;
		}
		int running = 0;
		int best = 0;
		for (int lane = 0; lane < menu.form().profile().lanes(); lane++) {
			if (menu.laneDuration(lane) > 0) {
				running++;
				best = Math.max(best, (int) (fraction(lane) * 100));
			}
		}
		if (y + 9 <= TEXT_BOTTOM) {
			meter(graphics, y, best, theme.progress(), best + "%");
			y += 10;
		}
		if (y + 9 <= TEXT_BOTTOM) {
			powerRow(graphics, y);
			y += 10;
		}
		if (y + 9 <= TEXT_BOTTOM) {
			row(graphics, y, Component.translatable("container.jugcraft.form.lanes").getString(), running + "/" + menu.form().profile().lanes());
		}
	}

	/** The stored power against the store's size, in whole thousands when tenths would not fit beside the label. */
	private void powerRow(GuiGraphicsExtractor graphics, int y) {
		String label = Component.translatable("container.jugcraft.terminal.power").getString();
		String energy = compact(menu.energy()) + "/" + compact(menu.capacity());
		if (!fits(label, energy)) {
			energy = thousands(menu.energy()) + "/" + thousands(menu.capacity());
		}
		row(graphics, y, label, energy);
	}

	/** Whether a label and its value fit side by side on one terminal line, two pixels apart. */
	private boolean fits(String label, String value) {
		return font.width(label) + 2 + font.width(value) <= SCREEN_WIDTH;
	}

	/** A label on the left and its value on the right; a value too wide for both is shown alone. */
	private void row(GuiGraphicsExtractor graphics, int y, String label, String value) {
		if (fits(label, value)) {
			graphics.text(font, label, SCREEN_X, y, theme.dim(), false);
		}
		graphics.text(font, value, SCREEN_X + SCREEN_WIDTH - font.width(value), y, theme.text(), false);
	}

	/** Exact numbers over the energy bar and the tanks; socket names over the sockets. */
	private void extractGaugeTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		int mx = mouseX - leftPos;
		int my = mouseY - topPos;
		MachineForm form = menu.form();
		FluidMachineSpec spec = form.tanks();
		if (mx >= barX() - 1 && mx <= barX() + BAR_WIDTH && my >= BAR_Y - 1 && my <= BAR_Y + BAR_HEIGHT) {
			graphics.setTooltipForNextFrame(font, Component.literal(String.format("%,d / %,d JE", menu.energy(), menu.capacity())),
					mouseX, mouseY);
			return;
		}
		if (my >= MachineMenu.TANK_Y - 1 && my <= MachineMenu.TANK_Y + MachineMenu.TANK_HEIGHT) {
			for (int tank = 0; tank < spec.tanks(); tank++) {
				int tankX = tankX(spec, tank);
				if (mx >= tankX - 1 && mx <= tankX + MachineMenu.TANK_WIDTH) {
					Component name = menu.tankFluid(tank) == 0 ? Component.translatable("container.jugcraft.tank.empty")
							: FluidVariantAttributes.getName(FluidVariant.of(fluid(tank)));
					Component line = MachineStatus.tankLabel(form, tank).copy().append(": ").append(name)
							.append(String.format(" %,d / %,d mB", menu.tankAmount(tank), spec.capacity(tank)));
					if (menu.tankReserved(tank) > 0) {
						line = line.copy().append(" ").append(Component.translatable("container.jugcraft.form.reserved", menu.tankReserved(tank)));
					}
					graphics.setTooltipForNextFrame(font, line, mouseX, mouseY);
					return;
				}
			}
		}
		for (int socket = 0; socket < form.sockets().size(); socket++) {
			int sx = FormMachineMenu.toolX(socket);
			int sy = FormMachineMenu.toolY(socket);
			if (mx >= sx && mx < sx + 16 && my >= sy && my < sy + 16 && hoveredSlot != null && !hoveredSlot.hasItem()) {
				Component label = MachineStatus.socketLabel(form, socket);
				graphics.setTooltipForNextFrame(font, menu.locked(socket)
						? label.copy().append(" ").append(Component.translatable("container.jugcraft.form.locked")) : label, mouseX, mouseY);
				return;
			}
		}
	}

	private void energyBar(GuiGraphicsExtractor graphics, int left, int top) {
		graphics.fill(left - 1, top - 1, left + BAR_WIDTH + 1, top + BAR_HEIGHT + 1, theme.slotDark());
		graphics.fill(left, top, left + BAR_WIDTH, top + BAR_HEIGHT, theme.slotFace());
		long capacity = Math.max(1, menu.capacity());
		int filled = (int) (menu.energy() * BAR_HEIGHT / capacity);
		int unlit = (theme.energy() & 0x00FFFFFF) | 0x30000000;
		for (int segment = 0; segment * 4 < BAR_HEIGHT; segment++) {
			int bottom = top + BAR_HEIGHT - segment * 4;
			graphics.fill(left + 1, bottom - 3, left + BAR_WIDTH - 1, bottom, segment * 4 < filled ? theme.energy() : unlit);
		}
	}

	/** A gauge tube: the fluid filled from the bottom, reserved room as a pale band above it, and tick marks. */
	private void tube(GuiGraphicsExtractor graphics, int left, int top, int amount, int reserved, int capacity, int color) {
		int width = MachineMenu.TANK_WIDTH;
		int height = MachineMenu.TANK_HEIGHT;
		graphics.fill(left - 1, top - 1, left + width + 1, top + height + 1, theme.slotDark());
		graphics.fill(left, top, left + width, top + height, theme.slotFace());
		int level = capacity <= 0 ? 0 : (int) ((long) amount * height / capacity);
		int booked = capacity <= 0 ? 0 : (int) ((long) Math.min(capacity, amount + reserved) * height / capacity);
		if (booked > level) {
			graphics.fill(left, top + height - booked, left + width, top + height - level, 0x40FFFFFF);
		}
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

	private void arrow(GuiGraphicsExtractor graphics, int left, int middle, float fraction) {
		int done = (int) (fraction * 24);
		for (int column = 0; column < 24; column++) {
			int half = column < 18 ? 2 : 6 - (column - 18);
			graphics.fill(left + column, middle - half, left + column + 1, middle + half, column < done ? theme.progress() : theme.slotDark());
		}
	}

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

	private void slotFrame(GuiGraphicsExtractor graphics, int slotX, int slotY) {
		graphics.fill(slotX - 1, slotY - 1, slotX + 17, slotY + 17, theme.slotDark());
		graphics.fill(slotX, slotY, slotX + 17, slotY + 17, theme.slotLight());
		graphics.fill(slotX, slotY, slotX + 16, slotY + 16, theme.slotFace());
	}

	private static int tankX(FluidMachineSpec spec, int tank) {
		return spec.isInput(tank) ? MachineMenu.inputTankX(tank) : MachineMenu.outputTankX(spec, tank - spec.inputTanks().size());
	}

	private Fluid fluid(int tank) {
		return BuiltInRegistries.FLUID.byId(menu.tankFluid(tank));
	}

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

	/** Whole thousands, for a line too narrow for {@link #compact}'s tenths: 29k rather than 29.3k. */
	private static String thousands(long value) {
		if (value < 1_000) {
			return Long.toString(value);
		}
		return value < 10_000_000 ? value / 1_000 + "k" : value / 1_000_000 + "M";
	}

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
}
