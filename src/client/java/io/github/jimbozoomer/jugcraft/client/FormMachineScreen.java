package io.github.jimbozoomer.jugcraft.client;

import static io.github.jimbozoomer.jugcraft.client.MachineSkin.BUTTON_STEP;
import static io.github.jimbozoomer.jugcraft.client.MachineSkin.BUTTON_X;
import static io.github.jimbozoomer.jugcraft.client.MachineSkin.BUTTON_Y;
import static io.github.jimbozoomer.jugcraft.client.MachineSkin.HELP_BOTTOM;
import static io.github.jimbozoomer.jugcraft.client.MachineSkin.HELP_WIDTH;
import static io.github.jimbozoomer.jugcraft.client.MachineSkin.HELP_X;
import static io.github.jimbozoomer.jugcraft.client.MachineSkin.HELP_Y;
import static io.github.jimbozoomer.jugcraft.client.MachineSkin.STATUS_BOTTOM;
import static io.github.jimbozoomer.jugcraft.client.MachineSkin.STATUS_WIDTH;
import static io.github.jimbozoomer.jugcraft.client.MachineSkin.STATUS_X;
import static io.github.jimbozoomer.jugcraft.client.MachineSkin.STATUS_Y;

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
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import org.jspecify.annotations.Nullable;

/**
 * The screen of a formed industrial machine (docs/features/machine-screens.md), in the forms' look
 * ({@link MachineScreenThemes#forms()}): the machine pane holds the energy bar, tanks, slots and progress (one thin bar
 * per lane under the arrow when a profile runs several batches), and the status pane says the machine's state and the
 * one thing to fix, in the order the plan gives (process, inputs, tool, energy, reserved outputs and completion), then
 * progress with the time left, power and batches. Reserved room in an output tank shows as a pale band above its fluid.
 * Icon buttons pause or resume it, cancel its batches, open its recipe page ({@link MachineRecipeScreen}) and choose
 * whether excess byproduct is vented, or excess water drained. The help box holds the tool sockets and upgrade slots, or
 * says what the machine is for when it has none.
 *
 * <p>A generator form shows its fuel tank, an arrow lit while it makes power and its store on the right, where a
 * processor's outputs would be; the status pane gives the power it made last tick. It has no batches, so no Cancel.
 */
public class FormMachineScreen extends AbstractContainerScreen<FormMachineMenu> {
	private static final int BAY_WIDTH = 176;
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
	private MachineSkin.IconButton pauseButton;
	private MachineSkin.@Nullable IconButton ventButton;
	/** Drain excess water (or vent excess gas) on a form with a drain output. */
	private MachineSkin.@Nullable IconButton drainButton;
	/** The recipe page, on processors; lit while a recipe is locked. */
	private MachineSkin.@Nullable IconButton recipeButton;
	private boolean recipePageOpen;
	/** The first lane's progress a second ago (20 ticks of samples), for the time left. */
	private final int[] progressHistory = new int[20];
	private int samples;

	public FormMachineScreen(FormMachineMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, menu.form().id().getPath().equals("gas_cleaning_rack")
				? Component.translatable("container.jugcraft.form.cleaning_title") : title, 268, 166);
		this.theme = MachineScreenThemes.forms();
	}

	/** Opens the recipe page; the menu stays open behind it until the page returns here. */
	public void openRecipes() {
		recipePageOpen = true;
		minecraft.gui.setScreen(new MachineRecipeScreen(this, menu));
	}

	void returnFromRecipes() {
		recipePageOpen = false;
	}

	@Override
	public void removed() {
		if (!recipePageOpen) {
			super.removed();
		}
	}

	private String overflowKey(String suffix) {
		return "container.jugcraft.form." + (menu.form().ventGas() == null ? "drain_" : "vent_") + suffix;
	}

	@Override
	protected void init() {
		super.init();
		titleLabelX = MachineSkin.TITLE_X;
		titleLabelY = MachineSkin.TITLE_Y;
		int x = leftPos + BUTTON_X;
		pauseButton = addRenderableWidget(new MachineSkin.IconButton(x, topPos + BUTTON_Y, theme, MachineSkin.Icon.PAUSE, 0xFF5A6068,
				Component.translatable("container.jugcraft.form.pause"), button -> click(FormMachineBlockEntity.BUTTON_PAUSE)));
		x += BUTTON_STEP;
		if (!menu.form().generator()) {
			MachineSkin.IconButton cancel = addRenderableWidget(new MachineSkin.IconButton(x, topPos + BUTTON_Y, theme, MachineSkin.Icon.CANCEL,
					0xFFC83228, Component.translatable("container.jugcraft.form.cancel"), button -> click(FormMachineBlockEntity.BUTTON_CANCEL)));
			cancel.setTooltip(Tooltip.create(Component.translatable("container.jugcraft.form.cancel")));
			x += BUTTON_STEP;
			recipeButton = addRenderableWidget(new MachineSkin.IconButton(x, topPos + BUTTON_Y, theme, MachineSkin.Icon.RECIPES,
					0xFF3C78C8, Component.translatable("container.jugcraft.form.recipes"), button -> openRecipes()));
			x += BUTTON_STEP;
		}
		// No form both vents its excess and drains it, so the two share the last place in the row.
		if (menu.form().ventsAny()) {
			ventButton = addRenderableWidget(new MachineSkin.IconButton(x, topPos + BUTTON_Y, theme, MachineSkin.Icon.VENT, 0xFF32AA46,
					Component.translatable("container.jugcraft.form.excess_vent"), button -> click(FormMachineBlockEntity.BUTTON_HOLD_VENTED)));
		} else if (menu.form().waterDrainOutput() >= 0) {
			drainButton = addRenderableWidget(new MachineSkin.IconButton(x, topPos + BUTTON_Y, theme,
					menu.form().ventGas() == null ? MachineSkin.Icon.DRAIN : MachineSkin.Icon.VENT, 0xFF3060D0,
					Component.translatable(overflowKey("on")), button -> click(FormMachineBlockEntity.BUTTON_DRAIN)));
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
		boolean paused = menu.paused();
		pauseButton.setIcon(paused ? MachineSkin.Icon.PLAY : MachineSkin.Icon.PAUSE);
		pauseButton.setLit(paused);
		pauseButton.setTooltip(Tooltip.create(Component.translatable(paused ? "container.jugcraft.form.resume" : "container.jugcraft.form.pause")));
		if (ventButton != null) {
			boolean holds = menu.holdsVented();
			ventButton.setLit(!holds);
			ventButton.setTooltip(Tooltip.create(Component.translatable(holds ? "container.jugcraft.form.excess_hold" : "container.jugcraft.form.excess_vent")
					.append(". ").append(Component.translatable("container.jugcraft.form.excess_tooltip"))));
		}
		if (drainButton != null) {
			Component drain = Component.translatable(overflowKey(menu.data(FormMachineBlockEntity.DATA_DRAIN) != 0 ? "on" : "off"));
			drainButton.setMessage(drain);
			drainButton.setLit(menu.data(FormMachineBlockEntity.DATA_DRAIN) != 0);
			drainButton.setTooltip(Tooltip.create(drain.copy().append(". ").append(Component.translatable(overflowKey("hint")))));
		}
		if (recipeButton != null) {
			boolean locked = menu.selectedRecipe() != 0;
			Component recipes = Component.translatable(locked ? "container.jugcraft.form.recipes_locked" : "container.jugcraft.form.recipes");
			recipeButton.setMessage(recipes);
			recipeButton.setLit(locked);
			recipeButton.setTooltip(Tooltip.create(recipes.copy().append(". ").append(Component.translatable("container.jugcraft.form.recipes_hint"))));
		}
		extractBackground(graphics, mouseX, mouseY, delta);
		super.extractRenderState(graphics, mouseX, mouseY, delta);
		extractTooltip(graphics, mouseX, mouseY);
		extractGaugeTooltip(graphics, mouseX, mouseY);
	}

	/** Samples the first lane's progress once a tick, for the time left. */
	@Override
	protected void containerTick() {
		super.containerTick();
		progressHistory[samples % progressHistory.length] = menu.laneProgress(0);
		samples++;
	}

	/** Seconds the first lane's batch has left at the pace of the last second, or -1 when that pace is not known. */
	private int secondsLeft() {
		int progress = menu.laneProgress(0);
		int duration = menu.laneDuration(0);
		if (samples <= progressHistory.length || duration <= 0 || progress <= 0) {
			return -1;
		}
		int advanced = progress - progressHistory[samples % progressHistory.length];
		if (advanced <= 0) {
			return -1;
		}
		return (int) Math.ceil((duration - progress) / (double) advanced);
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		int x = leftPos;
		int y = topPos;
		graphics.blit(RenderPipelines.GUI_TEXTURED, theme.texture(), x, y, 0.0F, 0.0F, imageWidth, imageHeight, 512, 256);
		MachineStatus status = menu.status();
		MachineSkin.lamp(graphics, x + MachineSkin.LAMP_X, y + MachineSkin.LAMP_Y,
				status.state().warning() ? theme.warn() : status.state().working() ? theme.ok() : theme.dim());
		MachineForm form = menu.form();
		FluidMachineSpec spec = form.tanks();
		MachineSkin.energyBar(graphics, theme, x + barX(), y + BAR_Y, BAR_WIDTH, BAR_HEIGHT, menu.energy(), menu.capacity());
		for (int tank = 0; tank < spec.tanks(); tank++) {
			int color = menu.tankFluid(tank) != 0 ? fluidColor(fluid(tank)) : 0;
			tube(graphics, x + tankX(spec, tank), y + MachineMenu.TANK_Y, menu.tankAmount(tank), menu.tankReserved(tank),
					spec.capacity(tank), color);
		}
		for (int slot = 0; slot < form.itemInputs(); slot++) {
			MachineSkin.slot(graphics, theme, x + MachineMenu.fluidItemInputX(spec, slot), y + MachineMenu.SLOT_Y);
		}
		for (int slot = 0; slot < form.itemOutputs(); slot++) {
			MachineSkin.slot(graphics, theme, x + MachineMenu.fluidItemOutputX(spec, slot), y + MachineMenu.SLOT_Y);
		}
		int tools = form.sockets().size() + form.upgradeSlots();
		for (int index = 0; index < tools; index++) {
			int sx = x + FormMachineMenu.toolX(index);
			int sy = y + FormMachineMenu.toolY(index);
			MachineSkin.slot(graphics, theme, sx, sy);
			if (index < form.sockets().size() && menu.locked(index)) {
				graphics.fill(sx, sy + 15, sx + 16, sy + 16, theme.warn());
			} else if (index >= form.sockets().size() && toolSlotEmpty(index)) {
				MachineSkin.hint(graphics, theme, sx, sy, MachineSkin.Icon.UPGRADE);
			}
		}
		int arrowX = x + MachineMenu.fluidArrowX(spec);
		if (form.generator()) {
			MachineSkin.arrow(graphics, theme, arrowX, y + ARROW_Y, menu.laneUse() > 0 ? 1 : 0);
			return;
		}
		MachineSkin.arrow(graphics, theme, arrowX, y + ARROW_Y, fraction(0));
		int lanes = form.profile().lanes();
		for (int lane = 1; lane < lanes; lane++) {
			int top = y + ARROW_Y + 8 + 3 * (lane - 1);
			int done = (int) (fraction(lane) * 24);
			graphics.fill(arrowX, top, arrowX + 24, top + 2, theme.slotDark());
			graphics.fill(arrowX, top, arrowX + done, top + 2, theme.progress());
		}
	}

	/** Whether the tool or upgrade slot at {@code index} (sockets first) is empty, so it shows its hint. */
	private boolean toolSlotEmpty(int index) {
		int sx = FormMachineMenu.toolX(index);
		int sy = FormMachineMenu.toolY(index);
		for (net.minecraft.world.inventory.Slot slot : menu.slots) {
			if (slot.x == sx && slot.y == sy) {
				return !slot.hasItem();
			}
		}
		return false;
	}

	/** How far lane {@code lane}'s batch is, 0 to 1 (0 when the lane is free). */
	private float fraction(int lane) {
		int duration = menu.laneDuration(lane);
		return duration <= 0 ? 0 : Math.min(1, menu.laneProgress(lane) / (float) duration);
	}

	@Override
	protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		graphics.text(font, title, titleLabelX, titleLabelY, theme.label(), false);
		statusPane(graphics);
		MachineForm form = menu.form();
		if (form.sockets().isEmpty() && form.upgradeSlots() == 0) {
			MachineSkin.wrap(graphics, font, Component.translatableWithFallback("container.jugcraft.tagline." + form.id().getPath(), ""),
					HELP_X, HELP_Y, HELP_WIDTH, HELP_BOTTOM, theme.help());
		}
	}

	/** The status pane: state, then the reason, progress with the time left, power and batches. */
	private void statusPane(GuiGraphicsExtractor graphics) {
		MachineStatus status = menu.status();
		boolean warning = status.state().warning();
		int y = MachineSkin.wrap(graphics, font, status.title(menu.form()), STATUS_X, STATUS_Y, STATUS_WIDTH, STATUS_BOTTOM,
				warning ? theme.warn() : theme.text());
		y += 2;
		y = MachineSkin.wrap(graphics, font, status.detail(menu.form()), STATUS_X, y, STATUS_WIDTH, STATUS_BOTTOM,
				warning ? theme.warn() : theme.dim());
		y += 2;
		if (menu.form().generator()) {
			if (y + 9 <= STATUS_BOTTOM) {
				row(graphics, y, Component.translatable("container.jugcraft.terminal.rate").getString(), menu.laneUse() + " JE/t");
				y += 10;
			}
			if (y + 9 <= STATUS_BOTTOM) {
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
		if (running > 0 && y + 9 <= STATUS_BOTTOM) {
			MachineSkin.meter(graphics, font, theme, y, best, best + "%");
			y += 10;
			int seconds = secondsLeft();
			if (seconds >= 0 && y + 9 <= STATUS_BOTTOM) {
				graphics.text(font, Component.translatable("container.jugcraft.terminal.time_left", MachineSkin.duration(seconds)),
						STATUS_X, y, theme.dim(), false);
				y += 10;
			}
		}
		if (y + 9 <= STATUS_BOTTOM) {
			powerRow(graphics, y);
			y += 10;
		}
		if (menu.form().profile().lanes() > 1 && y + 9 <= STATUS_BOTTOM) {
			row(graphics, y, Component.translatable("container.jugcraft.form.lanes").getString(), running + "/" + menu.form().profile().lanes());
			y += 10;
		}
		if (menu.form().id().getPath().equals("coal_gasifier") && y + 9 <= STATUS_BOTTOM) {
			// What the gasifier has let out so far, in mB.
			long emitted = ((long) menu.data(FormMachineBlockEntity.DATA_EMISSIONS_HIGH) << 16) | menu.data(FormMachineBlockEntity.DATA_EMISSIONS_LOW);
			row(graphics, y, Component.translatable("container.jugcraft.terminal.emitted").getString(), MachineSkin.compact(emitted));
		}
	}

	/**
	 * The stored power against the store's size, in whole thousands when tenths would not fit beside the label, and the
	 * stored power alone when even that would not (a store of 100,000 JE or more, holding 10,000 or more); the bar and
	 * its tooltip still give the size.
	 */
	private void powerRow(GuiGraphicsExtractor graphics, int y) {
		String label = Component.translatable("container.jugcraft.terminal.power").getString();
		String energy = MachineSkin.compact(menu.energy()) + "/" + MachineSkin.compact(menu.capacity());
		if (!fits(label, energy)) {
			energy = MachineSkin.thousands(menu.energy()) + "/" + MachineSkin.thousands(menu.capacity());
		}
		if (!fits(label, energy)) {
			energy = MachineSkin.thousands(menu.energy());
		}
		row(graphics, y, label, energy);
	}

	/** Whether a label and its value fit side by side on one status line, two pixels apart. */
	private boolean fits(String label, String value) {
		return font.width(label) + 2 + font.width(value) <= STATUS_WIDTH;
	}

	/** A label on the left and its value on the right; a value too wide for both is shown alone. */
	private void row(GuiGraphicsExtractor graphics, int y, String label, String value) {
		if (fits(label, value)) {
			graphics.text(font, label, STATUS_X, y, theme.dim(), false);
		}
		graphics.text(font, value, STATUS_X + STATUS_WIDTH - font.width(value), y, theme.text(), false);
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
		int tools = form.sockets().size() + form.upgradeSlots();
		for (int index = form.sockets().size(); index < tools; index++) {
			int sx = FormMachineMenu.toolX(index);
			int sy = FormMachineMenu.toolY(index);
			if (mx >= sx && mx < sx + 16 && my >= sy && my < sy + 16 && hoveredSlot != null && !hoveredSlot.hasItem()) {
				graphics.setTooltipForNextFrame(font, Component.translatable("container.jugcraft.slot.upgrade"), mouseX, mouseY);
				return;
			}
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
}
