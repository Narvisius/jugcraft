package io.github.jimbozoomer.jugcraft.client;

import net.minecraft.client.gui.components.Button;

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
import io.github.jimbozoomer.jugcraft.machine.MachineBlockEntity;
import io.github.jimbozoomer.jugcraft.machine.MachineKind;
import io.github.jimbozoomer.jugcraft.machine.MachineMenu;
import io.github.jimbozoomer.jugcraft.machine.SideConfig;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariantAttributes;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import org.jspecify.annotations.Nullable;

/**
 * Screen for every machine (docs/features/machine-screens.md), in one of five looks by tech level
 * ({@link MachineScreenThemes}). The body holds the title with a status lamp, the machine pane (energy bar, slots,
 * gauges and progress, where the menu puts them) and the status pane: one word for what the machine is doing, what to
 * do about it when it is stuck, progress with the time left, and power. Under it sit the inventory tray, the icon
 * buttons (redstone, eject, sides) and the help box with what the machine is for. The Sides button turns the status
 * pane into the machine's six faces, each clicked to change what it does, with the key in the help box. Hovering an
 * empty slot says what goes in it.
 */
public class MachineScreen extends AbstractContainerScreen<MachineMenu> {
	private static final int BAR_X = 10;
	private static final int BAR_Y = 17;
	private static final int BAR_WIDTH = 12;
	private static final int BAR_HEIGHT = 52;
	/** The big charge gauge on a machine with nothing else in its pane, centred with its percentage under it. */
	private static final int CHARGE_X = 25;
	private static final int CHARGE_Y = 25;
	/** The player's inventory and hotbar: the menu's last slots. */
	private static final int PLAYER_SLOTS = 36;

	private static final int FLAME = 0xFFE06020;
	private static final int WATER = 0xFF3060D0;
	private static final int LAVA = 0xFFE87010;

	// The sides view: a cross of faces (front in the middle, the back apart on the right) in the status pane.
	private static final int[][] FACE_XY = {{203, 37}, {241, 37}, {191, 37}, {215, 37}, {203, 25}, {203, 49}};
	private static final String[] FACE_LETTERS = {"F", "B", "L", "R", "T", "D"};
	/** Mode colours, input, output, both and closed; the key in the help box uses the same. */
	private static final int[] MODE_COLORS = {0xFF5AA0FF, 0xFFFFA040, 0xFF70E070, 0xFF9A9A9A};
	private static final String[] MODE_KEYS = {"input", "output", "both", "none"};
	/** The redstone icon's colour by mode: the look's own while ignored, red with a signal, dark red without. */
	private static final int[] REDSTONE_COLORS = {0, 0xFFFF4030, 0xFF902018};
	/** The lab look's tab colours for the three controls. */
	private static final int TAB_REDSTONE = 0xFFC83228;
	private static final int TAB_EJECT = 0xFF32AA46;
	private static final int TAB_SIDES = 0xFF285AC8;

	private final MachineSkin.FaceButton[] faceButtons = new MachineSkin.FaceButton[6];
	private MachineSkin.@Nullable IconButton ejectButton;
	private MachineSkin.@Nullable IconButton redstoneButton;
	private MachineSkin.@Nullable IconButton sidesButton;
	private boolean sidesView;
	private final Button[] batteryButtons = new Button[4];
	private int shownSides = -1;
	private final MachineScreenThemes.Theme theme;
	/** Energy and progress a second ago (20 ticks of samples), for the rate, the time left and the status. */
	private final long[] energyHistory = new long[20];
	private final int[] progressHistory = new int[20];
	private int samples;

	public MachineScreen(MachineMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title, 268, 166);
		this.theme = MachineScreenThemes.of(menu.kind());
	}

	@Override
	protected void init() {
		super.init();
		titleLabelX = MachineSkin.TITLE_X;
		titleLabelY = MachineSkin.TITLE_Y;
		shownSides = -1;
		if (menu.kind().isBattery()) {
			for (int i = 0; i < batteryButtons.length; i++) {
				int id = 20 + i;
				batteryButtons[i] = addRenderableWidget(Button.builder(Component.empty(), button -> click(id))
					.bounds(leftPos + 30, topPos + 19 + i * 13, 140, 12).build());
			}
			refreshBatteryButtons();
		}
		if (!menu.kind().isProcessor()) {
			return;
		}
		for (int face = 0; face < faceButtons.length; face++) {
			int id = face;
			faceButtons[face] = addRenderableWidget(new MachineSkin.FaceButton(leftPos + FACE_XY[face][0], topPos + FACE_XY[face][1],
					FACE_LETTERS[face], Component.translatable("container.jugcraft.side." + SideConfig.Face.values()[face].name().toLowerCase()),
					button -> click(id)));
		}
		redstoneButton = addRenderableWidget(new MachineSkin.IconButton(leftPos + BUTTON_X, topPos + BUTTON_Y, theme,
				MachineSkin.Icon.REDSTONE, TAB_REDSTONE, Component.translatable("container.jugcraft.redstone", ""),
				button -> click(SideConfig.REDSTONE_BUTTON)));
		ejectButton = addRenderableWidget(new MachineSkin.IconButton(leftPos + BUTTON_X + BUTTON_STEP, topPos + BUTTON_Y, theme,
				MachineSkin.Icon.EJECT, TAB_EJECT, Component.translatable("container.jugcraft.eject"),
				button -> click(SideConfig.EJECT_BUTTON)));
		sidesButton = addRenderableWidget(new MachineSkin.IconButton(leftPos + BUTTON_X + 2 * BUTTON_STEP, topPos + BUTTON_Y, theme,
				MachineSkin.Icon.SIDES, TAB_SIDES, Component.translatable("container.jugcraft.button.sides"), button -> showSides(!sidesView)));
		showSides(sidesView);
	}

	/** Shows the machine's faces in the status pane, or its status again. Public for the client game test. */
	public void showSides(boolean show) {
		sidesView = show;
		for (MachineSkin.FaceButton button : faceButtons) {
			if (button != null) {
				button.visible = show;
			}
		}
		if (sidesButton != null) {
			sidesButton.setIcon(show ? MachineSkin.Icon.BACK : MachineSkin.Icon.SIDES);
			sidesButton.setLit(show);
			sidesButton.setTooltip(Tooltip.create(Component.translatable(show ? "container.jugcraft.button.status" : "container.jugcraft.button.sides")));
		}
	}

	private void click(int id) {
		if (minecraft != null && minecraft.gameMode != null) {
			minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
		}
	}

	private void refreshBatteryButtons() {
		if (!menu.kind().isBattery() || batteryButtons[0] == null) return;
		int mode = Math.clamp(menu.data(MachineBlockEntity.DATA_BATTERY_MODE), 0, 3);
		int priority = Math.clamp(menu.data(MachineBlockEntity.DATA_BATTERY_PRIORITY), 0, 2);
		batteryButtons[0].setMessage(Component.translatable("container.jugcraft.battery.mode",
			Component.translatable("container.jugcraft.battery.mode." + mode)));
		batteryButtons[1].setMessage(Component.translatable("container.jugcraft.battery.reserve", menu.data(MachineBlockEntity.DATA_BATTERY_RESERVE)));
		batteryButtons[2].setMessage(Component.translatable("container.jugcraft.battery.priority",
			Component.translatable("container.jugcraft.battery.priority." + priority)));
		batteryButtons[3].setMessage(Component.translatable("container.jugcraft.battery.output", menu.data(MachineBlockEntity.DATA_BATTERY_OUTPUT)));
		batteryButtons[1].setTooltip(Tooltip.create(Component.translatable("container.jugcraft.battery.reserve.help")));
		batteryButtons[2].setTooltip(Tooltip.create(Component.translatable("container.jugcraft.battery.priority.help")));
		batteryButtons[3].setTooltip(Tooltip.create(Component.translatable("container.jugcraft.battery.output.help")));
		int modules = menu.data(MachineBlockEntity.DATA_BATTERY_MODULES);
		batteryButtons[0].setTooltip(Tooltip.create(menu.kind() == MachineKind.FLOW_BATTERY
			? Component.translatable("container.jugcraft.battery.modules", modules & 7,
				(modules & 32) != 0 ? 16384 : 8192,
				Component.translatable((modules & 16) != 0 ? "container.jugcraft.battery.detached" : "container.jugcraft.battery.connected"))
			: Component.translatable("container.jugcraft.battery.mode.help")));
	}

	/** Recolours the side buttons when the synced configuration changes. */
	private void refreshSideButtons() {
		int packed = menu.data(MachineBlockEntity.DATA_SIDES);
		if (ejectButton == null || redstoneButton == null || packed == shownSides) {
			return;
		}
		shownSides = packed;
		SideConfig config = SideConfig.of(packed);
		for (SideConfig.Face face : SideConfig.Face.values()) {
			SideConfig.Mode mode = config.mode(face);
			MachineSkin.FaceButton button = faceButtons[face.ordinal()];
			button.setColor(MODE_COLORS[mode.ordinal()]);
			button.setTooltip(Tooltip.create(Component.translatable("container.jugcraft.side",
					Component.translatable("container.jugcraft.side." + face.name().toLowerCase()),
					Component.translatable("container.jugcraft.mode." + mode.name().toLowerCase()))));
		}
		ejectButton.setLit(config.eject());
		ejectButton.setTooltip(Tooltip.create(Component.translatable(config.eject() ? "container.jugcraft.eject.on" : "container.jugcraft.eject.off")
				.append(". ").append(Component.translatable("container.jugcraft.eject.tooltip"))));
		SideConfig.Redstone redstone = config.redstone();
		redstoneButton.setIconColor(REDSTONE_COLORS[redstone.ordinal()]);
		redstoneButton.setLit(redstone != SideConfig.Redstone.IGNORED);
		redstoneButton.setTooltip(Tooltip.create(Component.translatable("container.jugcraft.redstone",
				Component.translatable("container.jugcraft.redstone." + redstone.name().toLowerCase()))));
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		refreshSideButtons();
		extractBackground(graphics, mouseX, mouseY, delta);
		super.extractRenderState(graphics, mouseX, mouseY, delta);
		extractTooltip(graphics, mouseX, mouseY);
		extractGaugeTooltip(graphics, mouseX, mouseY);
	}

	/** Exact numbers over the energy bar (or charge gauge) and the tanks, and what goes in an empty machine slot. */
	private void extractGaugeTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		int mx = mouseX - leftPos;
		int my = mouseY - topPos;
		MachineKind kind = menu.kind();
        if (kind.hasProcessTool() && mx >= MachineMenu.PATTERN_X - 1
                && mx <= MachineMenu.PATTERN_X + 17 && my >= MachineMenu.PATTERN_Y - 1
                && my <= MachineMenu.PATTERN_Y + 17 && menu.slots.get(kind.slots).getItem().isEmpty()) {
            graphics.setTooltipForNextFrame(font, Component.translatable(kind == MachineKind.CRYOGENIC_LIQUEFIER ? "container.jugcraft.cooling_module" : "container.jugcraft.pattern"), mouseX, mouseY);
            return;
        }
		String line = null;
		boolean overEnergy = bare(kind)
				? mx >= CHARGE_X - 1 && mx <= CHARGE_X + 127 && my >= CHARGE_Y - 1 && my <= CHARGE_Y + 26
				: mx >= BAR_X - 1 && mx <= BAR_X + BAR_WIDTH && my >= BAR_Y - 1 && my <= BAR_Y + BAR_HEIGHT;
		if (kind.usesPower() && overEnergy) {
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
					if (kind == MachineKind.CHEMICAL_REACTOR) name = Component.translatable("container.jugcraft.reactor.tank." + tank).append(": ").append(name);
					graphics.setTooltipForNextFrame(font, name.copy().append(String.format(": %,d / %,d mB",
							menu.tankAmount(tank), spec.capacity(tank))), mouseX, mouseY);
					return;
				}
			}
		}
		if (line != null) {
			graphics.setTooltipForNextFrame(font, Component.literal(line), mouseX, mouseY);
			return;
		}
		Slot slot = hoveredSlot;
		if (slot != null && slot.index < menu.slots.size() - PLAYER_SLOTS && !slot.hasItem()) {
			String role = slotRole(slot.getContainerSlot());
			if (role != null) {
				graphics.setTooltipForNextFrame(font, Component.translatable("container.jugcraft.slot." + role), mouseX, mouseY);
			}
		}
	}

	/** The menu's slot for a machine container slot, or null. */
	private @Nullable Slot machineSlot(int containerSlot) {
		for (int index = 0; index < menu.slots.size() - PLAYER_SLOTS; index++) {
			Slot slot = menu.getSlot(index);
			if (slot.getContainerSlot() == containerSlot) {
				return slot;
			}
		}
		return null;
	}

	private boolean empty(int containerSlot) {
		Slot slot = machineSlot(containerSlot);
		return slot == null || !slot.hasItem();
	}

	/** What a machine container slot is for (a lang key's last part), or null where the machine does not say. */
	private @Nullable String slotRole(int index) {
		MachineKind kind = menu.kind();
		if (kind == MachineKind.COAL_GENERATOR) {
			return index == 0 ? "fuel" : null;
		}
		if (kind.isBoiler()) {
			return index == MachineBlockEntity.SLOT_FUEL ? "fuel" : index == MachineBlockEntity.SLOT_WATER_IN ? "water"
					: index == MachineBlockEntity.SLOT_BUCKET_OUT ? "bucket" : null;
		}
		if (kind.isFluidProcessor()) {
			FluidMachineSpec spec = kind.fluidSpec();
			return index < spec.itemInputs() ? "in" : index < spec.itemInputs() + spec.itemOutputs() ? "out" : null;
		}
		if (!kind.isProcessor()) {
			return null;
		}
		int output = kind.outputSlot();
		if (index < output) {
			return "in";
		}
		if (index == output) {
			return "out";
		}
		if (index <= output + kind.byproductSlots()) {
			return "extra";
		}
		return index >= kind.slots && index < kind.slots + kind.upgradeSlots() ? "upgrade" : null;
	}

	/** Samples the stored energy and the progress once a tick, for the rate and the time left. */
	@Override
	protected void containerTick() {
		super.containerTick();
		energyHistory[samples % energyHistory.length] = menu.energy();
		progressHistory[samples % progressHistory.length] = menu.data(MachineBlockEntity.DATA_PROGRESS);
		samples++;
	}

	/** JE per tick over the last second (positive while charging), or 0 until a second has been sampled. */
	private long energyRate() {
		if (samples <= energyHistory.length) {
			return 0;
		}
		return (menu.energy() - energyHistory[samples % energyHistory.length]) / energyHistory.length;
	}

	/** Seconds the work in hand has left at the pace of the last second, or -1 when that pace is not known. */
	private int secondsLeft() {
		int progress = menu.data(MachineBlockEntity.DATA_PROGRESS);
		int max = menu.data(MachineBlockEntity.DATA_MAX_PROGRESS);
		if (samples <= progressHistory.length || max <= 0 || progress <= 0) {
			return -1;
		}
		int advanced = progress - progressHistory[samples % progressHistory.length];
		if (advanced <= 0) {
			return -1; // A batch finished within the second, or the work stalled.
		}
		return (int) Math.ceil((max - progress) / (double) advanced);
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		refreshSideButtons();
		refreshBatteryButtons();
		int x = leftPos;
		int y = topPos;
		graphics.blit(RenderPipelines.GUI_TEXTURED, theme.texture(), x, y, 0.0F, 0.0F, imageWidth, imageHeight, 512, 256);
		Status status = status();
		MachineSkin.lamp(graphics, x + MachineSkin.LAMP_X, y + MachineSkin.LAMP_Y,
				status.warning ? theme.warn() : status.active ? theme.ok() : theme.dim());

		MachineKind kind = menu.kind();
		if (kind.usesPower() && bare(kind) && !kind.isBattery()) {
			MachineSkin.charge(graphics, theme, x + CHARGE_X, y + CHARGE_Y, menu.energy(), menu.capacity());
		} else if (kind.usesPower()) {
			MachineSkin.energyBar(graphics, theme, x + BAR_X, y + BAR_Y, BAR_WIDTH, BAR_HEIGHT, menu.energy(), menu.capacity());
		}
		if (kind == MachineKind.COAL_GENERATOR) {
			slot(graphics, MachineMenu.INPUT_X, MachineMenu.SLOT_Y, 0, MachineSkin.Icon.FLAME);
			flame(graphics, x + 57, y + 20);
		} else if (kind.isBoiler()) {
			slot(graphics, MachineMenu.INPUT_X, 17, MachineBlockEntity.SLOT_WATER_IN, null);
			slot(graphics, MachineMenu.INPUT_X, 53, MachineBlockEntity.SLOT_FUEL, MachineSkin.Icon.FLAME);
			slot(graphics, MachineMenu.OUTPUT_X, MachineMenu.SLOT_Y, MachineBlockEntity.SLOT_BUCKET_OUT, null);
			flame(graphics, x + 57, y + 37);
			tube(graphics, x + 150, y + BAR_Y, 12, BAR_HEIGHT, menu.data(MachineBlockEntity.DATA_TANK), kind.tankCapacity(), WATER);
		} else if (kind == MachineKind.GEOTHERMAL_GENERATOR) {
			tube(graphics, x + 150, y + BAR_Y, 12, BAR_HEIGHT, menu.data(MachineBlockEntity.DATA_TANK), MachineKind.GEOTHERMAL_TANK, LAVA);
		} else if (kind.isFluidProcessor()) {
            if (kind.hasProcessTool()) {
                slot(graphics, MachineMenu.PATTERN_X, MachineMenu.PATTERN_Y, kind.slots, null);
            }
			FluidMachineSpec spec = kind.fluidSpec();
			for (int tank = 0; tank < spec.tanks(); tank++) {
				int color = menu.tankFluid(tank) != 0 ? fluidColor(fluid(tank)) : 0;
				tube(graphics, x + tankX(spec, tank), y + MachineMenu.TANK_Y, MachineMenu.TANK_WIDTH, MachineMenu.TANK_HEIGHT,
						menu.tankAmount(tank), spec.capacity(tank), color);
			}
			for (int slot = 0; slot < spec.itemInputs(); slot++) {
				slot(graphics, MachineMenu.fluidItemInputX(spec, slot), MachineMenu.SLOT_Y, slot, null);
			}
			for (int slot = 0; slot < spec.itemOutputs(); slot++) {
				slot(graphics, MachineMenu.fluidItemOutputX(spec, slot), MachineMenu.SLOT_Y, spec.itemInputs() + slot, null);
			}
			MachineSkin.arrow(graphics, theme, x + MachineMenu.fluidArrowX(spec), y + 43, progressFraction());
		} else if (kind.isProcessor()) {
			int inputs = kind.outputSlot();
			for (int slot = 0; slot < inputs; slot++) {
				slot(graphics, MachineMenu.inputX(kind, slot), MachineMenu.inputY(kind, slot), slot, null);
			}
			slot(graphics, MachineMenu.OUTPUT_X, MachineMenu.SLOT_Y, inputs, null);
			for (int index = 0; index < kind.byproductSlots(); index++) {
				slot(graphics, MachineMenu.byproductX(index), MachineMenu.BYPRODUCT_Y, inputs + 1 + index, null);
			}
			for (int index = 0; index < kind.upgradeSlots(); index++) {
				slot(graphics, MachineMenu.upgradeX(index), MachineMenu.UPGRADE_Y, kind.slots + index, MachineSkin.Icon.UPGRADE);
			}
			if (kind.tankCapacity() > 0) {
				// Water gauge between the energy bar and the input slot.
				tube(graphics, x + 30, y + BAR_Y, 6, 52 - BAR_Y, menu.data(MachineBlockEntity.DATA_TANK), kind.tankCapacity(), WATER);
			}
			MachineSkin.arrow(graphics, theme, x + MachineMenu.arrowX(kind), y + 43, progressFraction());
		}
	}

	/** Whether a machine has nothing in its pane but its energy (batteries, solar panels): no slots, tanks or fuel. */
	private static boolean bare(MachineKind kind) {
		return kind != MachineKind.COAL_GENERATOR && !kind.isBoiler() && kind != MachineKind.GEOTHERMAL_GENERATOR
				&& !kind.isFluidProcessor() && !kind.isProcessor();
	}

	/** A machine slot's frame at (slotX, slotY), with a faint hint of what goes there while it is empty. */
	private void slot(GuiGraphicsExtractor graphics, int slotX, int slotY, int containerSlot, MachineSkin.@Nullable Icon hint) {
		MachineSkin.slot(graphics, theme, leftPos + slotX, topPos + slotY);
		if (hint != null && empty(containerSlot)) {
			MachineSkin.hint(graphics, theme, leftPos + slotX, topPos + slotY, hint);
		}
	}

	private float progressFraction() {
		int max = menu.data(MachineBlockEntity.DATA_MAX_PROGRESS);
		return max <= 0 ? 0 : menu.data(MachineBlockEntity.DATA_PROGRESS) / (float) max;
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

	@Override
	protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		graphics.text(font, title, titleLabelX, titleLabelY, theme.label(), false);
		slotLabels(graphics);
		if (menu.kind().usesPower() && bare(menu.kind()) && !menu.kind().isBattery()) {
			String percent = menu.energy() * 100 / Math.max(1, menu.capacity()) + "%";
			graphics.text(font, percent, CHARGE_X + 61 - font.width(percent) / 2, CHARGE_Y + 31, theme.text(), false);
		}
		if (sidesView) {
			sides(graphics);
		} else {
			statusPane(graphics);
			MachineSkin.wrap(graphics, font, Component.translatable("container.jugcraft.tagline." + menu.kind().id), HELP_X,
					sidesButton != null ? HELP_Y : MachineSkin.HELP_TOP, HELP_WIDTH, HELP_BOTTOM, theme.help());
		}
	}

	/** IN and OUT over a processor's slots, where they fit without touching another slot. */
	private void slotLabels(GuiGraphicsExtractor graphics) {
		MachineKind kind = menu.kind();
		if (!kind.isProcessor() || kind == MachineKind.AUTO_CRAFTER || kind.isFluidProcessor()) {
			return;
		}
		int labelY = MachineMenu.SLOT_Y - 10;
		if (kind.outputSlot() > 0) {
			graphics.text(font, Component.translatable("container.jugcraft.label.in"), MachineMenu.inputX(kind, 0), labelY, theme.dim(), false);
		}
		if (kind.byproductSlots() <= 1) {
			graphics.text(font, Component.translatable("container.jugcraft.label.out"), MachineMenu.OUTPUT_X, labelY, theme.dim(), false);
		}
	}

	/** The status pane: what the machine is doing, what to do about it, progress, the time left and power. */
	private void statusPane(GuiGraphicsExtractor graphics) {
		MachineKind kind = menu.kind();
		Status status = status();
		int y = MachineSkin.wrap(graphics, font, Component.translatable("container.jugcraft.terminal." + status.key), STATUS_X, STATUS_Y,
				STATUS_WIDTH, STATUS_BOTTOM, status.warning ? theme.warn() : theme.text());
		y += 2;
		if (kind == MachineKind.DEPOSIT_DRILL && menu.data(MachineBlockEntity.DATA_DRILL_STAGE) > 0) {
			y = MachineSkin.wrap(graphics, font, Component.translatable(
					menu.data(MachineBlockEntity.DATA_DRILL_STAGE) == 1 ? "container.jugcraft.drill.boring" : "container.jugcraft.drill.extracting",
					menu.data(MachineBlockEntity.DATA_DRILL_DEPTH)), STATUS_X, y, STATUS_WIDTH, STATUS_BOTTOM, theme.text());
			y += 2;
		}
		if (status.hint != null) {
			y = MachineSkin.wrap(graphics, font, Component.translatable("container.jugcraft.hint." + status.hint), STATUS_X, y,
					STATUS_WIDTH, STATUS_BOTTOM, status.warning ? theme.warn() : theme.dim());
			y += 2;
		}
		if (hasProgress(kind) && (status.active || kind == MachineKind.DEPOSIT_DRILL
				&& menu.data(MachineBlockEntity.DATA_PROGRESS) > 0) && y + 9 <= STATUS_BOTTOM) {
			int percent = (int) Math.min(100, progressFraction() * 100);
			MachineSkin.meter(graphics, font, theme, y, percent, percent + "%");
			y += 10;
			int seconds = secondsLeft();
			if (seconds >= 0 && y + 9 <= STATUS_BOTTOM) {
				graphics.text(font, Component.translatable("container.jugcraft.terminal.time_left", MachineSkin.duration(seconds)),
						STATUS_X, y, theme.dim(), false);
				y += 10;
			}
		}
		if (kind.usesPower() && kind != MachineKind.AUTO_CRAFTER && y + 9 <= STATUS_BOTTOM) {
			row(graphics, y, Component.translatable("container.jugcraft.terminal.power").getString(),
					MachineSkin.compact(menu.energy()) + "/" + MachineSkin.compact(menu.capacity()));
			y += 10;
		}
		long rate = energyRate();
		if (kind.usesPower() && rate != 0 && y + 9 <= STATUS_BOTTOM) {
			String text = (rate > 0 ? "+" : "-") + MachineSkin.compact(Math.abs(rate)) + " JE/t";
			row(graphics, y, Component.translatable("container.jugcraft.terminal.rate").getString(), text);
			y += 10;
		}
		String condition = conditionKey(kind);
		if (condition != null) {
			MachineSkin.wrap(graphics, font, Component.translatable(condition), STATUS_X, y, STATUS_WIDTH, STATUS_BOTTOM,
					menu.data(MachineBlockEntity.DATA_FORMED) == 1 ? theme.text() : theme.warn());
		}
	}

	/** The sides view: its heading over the faces (which draw themselves), and the faces' key in the help box. */
	private void sides(GuiGraphicsExtractor graphics) {
		graphics.text(font, Component.translatable("container.jugcraft.terminal.sides"), STATUS_X, STATUS_Y - 1, theme.text(), false);
		int y = MachineSkin.wrap(graphics, font, Component.translatable("container.jugcraft.sides.help"), HELP_X, HELP_Y, HELP_WIDTH,
				HELP_BOTTOM, theme.help());
		y += 2;
		for (int mode = 0; mode < MODE_KEYS.length && y + 9 <= HELP_BOTTOM; mode++) {
			graphics.fill(HELP_X, y + 1, HELP_X + 6, y + 7, MODE_COLORS[mode]);
			graphics.text(font, Component.translatable("container.jugcraft.sides.key." + MODE_KEYS[mode]), HELP_X + 9, y, theme.help(), false);
			y += 9;
		}
	}

	/** A label on the left of the status pane and its value on the right; a value too wide for both is shown alone. */
	private void row(GuiGraphicsExtractor graphics, int y, String label, String value) {
		if (font.width(label) + 2 + font.width(value) <= STATUS_WIDTH) {
			graphics.text(font, label, STATUS_X, y, theme.dim(), false);
		}
		graphics.text(font, value, STATUS_X + STATUS_WIDTH - font.width(value), y, theme.text(), false);
	}

	/**
	 * What the machine is doing: a lang key under container.jugcraft.terminal, whether it is a problem, whether the
	 * machine is busy, and what to do about it (a lang key under container.jugcraft.hint), if anything.
	 */
	private record Status(String key, boolean warning, boolean active, @Nullable String hint) {
	}

	/** What the status pane says, from what the menu syncs: the machine's data and its slots. */
	private Status status() {
		MachineKind kind = menu.kind();
		long rate = energyRate();
		boolean burning = menu.data(MachineBlockEntity.DATA_BURN) > 0;
		boolean working = menu.data(MachineBlockEntity.DATA_PROGRESS) > 0;
		if (kind.isBattery()) {
			if ((menu.data(MachineBlockEntity.DATA_BATTERY_MODULES) & 16) != 0) return new Status("modules_missing", true, false, null);
			if (menu.data(MachineBlockEntity.DATA_BATTERY_MODE) == 3) return new Status("stopped", false, false, null);
			return new Status(rate > 0 ? "charging" : rate < 0 ? "discharging" : "storing", false, rate != 0, null);
		}
		if (kind.isGenerator()) {
			if (burning || working || rate > 0) {
				return new Status("generating", false, true, null);
			}
			boolean needsFuel = kind == MachineKind.COAL_GENERATOR ? empty(0) : kind.isBoiler() && empty(MachineBlockEntity.SLOT_FUEL);
			return new Status("idle", false, false, needsFuel ? "add_fuel" : null);
		}
		if (kind == MachineKind.DEPOSIT_DRILL) {
			if (menu.energy() < menu.data(MachineBlockEntity.DATA_DRILL_USE)) return new Status("no_power", true, false, "no_power");
			boolean full = true;
			for (int slot = 0; slot < kind.slots; slot++) {
				Slot output = machineSlot(slot);
				full &= output != null && !output.getItem().isEmpty() && output.getItem().getCount() >= output.getItem().getMaxStackSize();
			}
			if (full) return new Status("output_full", true, false, "output_full");
			return new Status(working ? "running" : "idle", false, working, null);
		}
		if (working) {
			return new Status("running", false, true, null);
		}
		if (kind.usesPower() && kind.usePerTick > 0 && menu.energy() < kind.usePerTick) {
			return new Status("no_power", true, false, "no_power");
		}
		if (kind.isProcessor() && !kind.isFluidProcessor()) {
			int output = kind.outputSlot();
			Slot result = machineSlot(output);
			ItemStack stack = result == null ? ItemStack.EMPTY : result.getItem();
			if (!stack.isEmpty() && stack.getCount() >= stack.getMaxStackSize()) {
				return new Status("output_full", true, false, "output_full");
			}
			boolean anyInput = false;
			for (int slot = 0; slot < output; slot++) {
				anyInput |= !empty(slot);
			}
			if (!anyInput) {
				return new Status("idle", false, false, "add_input");
			}
		}
		return new Status("idle", false, false, null);
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
}
