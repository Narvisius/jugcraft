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
	/** Energy readout: amber like the energy bar, a shade darker so it reads on the gray panel; no shadow. */
	private static final int READOUT = 0xFFB8740A;

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
		} else if (kind.isBoiler()) {
			slotFrame(graphics, x + MachineMenu.INPUT_X, y + 17);
			slotFrame(graphics, x + MachineMenu.INPUT_X, y + 53);
			slotFrame(graphics, x + MachineMenu.OUTPUT_X, y + MachineMenu.SLOT_Y);
			int maxBurn = Math.max(1, menu.data(MachineBlockEntity.DATA_MAX_BURN));
			int flame = menu.data(MachineBlockEntity.DATA_BURN) * 14 / maxBurn;
			graphics.fill(x + 57, y + 37 + 14 - flame, x + 71, y + 51, FLAME);
			// Water tank gauge on the right.
			graphics.fill(x + 149, y + BAR_Y - 1, x + 163, y + BAR_Y + BAR_HEIGHT + 1, DARK);
			int water = menu.data(MachineBlockEntity.DATA_TANK) * BAR_HEIGHT / kind.tankCapacity();
			graphics.fill(x + 150, y + BAR_Y + BAR_HEIGHT - water, x + 162, y + BAR_Y + BAR_HEIGHT, WATER);
		} else if (kind == MachineKind.GEOTHERMAL_GENERATOR) {
			// Lava tank gauge on the right.
			graphics.fill(x + 149, y + BAR_Y - 1, x + 163, y + BAR_Y + BAR_HEIGHT + 1, DARK);
			int lava = menu.data(MachineBlockEntity.DATA_TANK) * BAR_HEIGHT / MachineKind.GEOTHERMAL_TANK;
			graphics.fill(x + 150, y + BAR_Y + BAR_HEIGHT - lava, x + 162, y + BAR_Y + BAR_HEIGHT, LAVA);
		} else if (kind.isFluidProcessor()) {
			FluidMachineSpec spec = kind.fluidSpec();
			for (int tank = 0; tank < spec.tanks(); tank++) {
				int tankX = x + tankX(spec, tank);
				graphics.fill(tankX - 1, y + MachineMenu.TANK_Y - 1, tankX + MachineMenu.TANK_WIDTH + 1,
						y + MachineMenu.TANK_Y + MachineMenu.TANK_HEIGHT + 1, DARK);
				int level = menu.tankAmount(tank) * MachineMenu.TANK_HEIGHT / Math.max(1, spec.capacity(tank));
				if (menu.tankFluid(tank) != 0 && level > 0) {
					graphics.fill(tankX, y + MachineMenu.TANK_Y + MachineMenu.TANK_HEIGHT - level, tankX + MachineMenu.TANK_WIDTH,
							y + MachineMenu.TANK_Y + MachineMenu.TANK_HEIGHT, fluidColor(fluid(tank)));
				}
			}
			for (int slot = 0; slot < spec.itemInputs(); slot++) {
				slotFrame(graphics, x + MachineMenu.fluidItemInputX(spec, slot), y + MachineMenu.SLOT_Y);
			}
			for (int slot = 0; slot < spec.itemOutputs(); slot++) {
				slotFrame(graphics, x + MachineMenu.fluidItemOutputX(spec, slot), y + MachineMenu.SLOT_Y);
			}
			int maxProgress = Math.max(1, menu.data(MachineBlockEntity.DATA_MAX_PROGRESS));
			int arrow = menu.data(MachineBlockEntity.DATA_PROGRESS) * 24 / maxProgress;
			int arrowX = x + MachineMenu.FLUID_ARROW_X;
			graphics.fill(arrowX, y + 41, arrowX + 24, y + 45, DARK);
			graphics.fill(arrowX, y + 41, arrowX + arrow, y + 45, PROGRESS);
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
				graphics.fill(x + 29, y + BAR_Y - 1, x + 37, y + 53, DARK);
				int water = menu.data(MachineBlockEntity.DATA_TANK) * (52 - BAR_Y) / kind.tankCapacity();
				graphics.fill(x + 30, y + 52 - water, x + 36, y + 52, WATER);
			}
			int maxProgress = Math.max(1, menu.data(MachineBlockEntity.DATA_MAX_PROGRESS));
			int arrow = menu.data(MachineBlockEntity.DATA_PROGRESS) * 24 / maxProgress;
			int arrowX = x + MachineMenu.arrowX(kind);
			graphics.fill(arrowX, y + 41, arrowX + 24, y + 45, DARK);
			graphics.fill(arrowX, y + 41, arrowX + arrow, y + 45, PROGRESS);
		}
	}

	@Override
	protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		super.extractLabels(graphics, mouseX, mouseY);
		// The auto-crafter's grid covers the energy readout's place; its bar still shows the charge.
		if (menu.kind().usesPower() && menu.kind() != MachineKind.AUTO_CRAFTER) {
			String energy = compact(menu.energy()) + " / " + compact(menu.capacity()) + " JE";
			if (menu.kind().isFluidProcessor()) {
				// The tank gauges fill the machine area; the readout sits at the right of the inventory label row.
				graphics.text(font, energy, imageWidth - 8 - font.width(energy), inventoryLabelY, READOUT, false);
			} else {
				graphics.text(font, energy, 28, 60, READOUT, false);
			}
		}
		if (menu.kind() == MachineKind.ARC_FURNACE) {
			String key = menu.data(MachineBlockEntity.DATA_FORMED) == 1
					? "container.jugcraft.arc_furnace.formed" : "container.jugcraft.arc_furnace.incomplete";
			graphics.text(font, Component.translatable(key).getString(), 28, 18, TEXT, false);
		} else if (menu.kind() == MachineKind.WIND_TURBINE) {
			String key = menu.data(MachineBlockEntity.DATA_FORMED) == 1
					? "container.jugcraft.wind_turbine.clear" : "container.jugcraft.wind_turbine.blocked";
			graphics.text(font, Component.translatable(key).getString(), 28, 18, TEXT, false);
		} else if (menu.kind() == MachineKind.FRACKING_RIG) {
			String key = menu.data(MachineBlockEntity.DATA_FORMED) == 1
					? "container.jugcraft.fracking_rig.shale" : "container.jugcraft.fracking_rig.none";
			graphics.text(font, Component.translatable(key).getString(), 28, 18, TEXT, false);
		} else if (menu.kind() == MachineKind.PUMPJACK) {
			String key = menu.data(MachineBlockEntity.DATA_FORMED) == 1
					? "container.jugcraft.pumpjack.oil" : "container.jugcraft.pumpjack.dry";
			graphics.text(font, Component.translatable(key).getString(), 28, 18, TEXT, false);
		} else if (menu.kind() == MachineKind.WATER_WHEEL) {
			String key = menu.data(MachineBlockEntity.DATA_FORMED) == 1
					? "container.jugcraft.water_wheel.turning" : "container.jugcraft.water_wheel.still";
			graphics.text(font, Component.translatable(key).getString(), 28, 18, TEXT, false);
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

	private static void slotFrame(GuiGraphicsExtractor graphics, int slotX, int slotY) {
		graphics.fill(slotX - 1, slotY - 1, slotX + 17, slotY + 17, DARK);
		graphics.fill(slotX, slotY, slotX + 17, slotY + 17, LIGHT);
		graphics.fill(slotX, slotY, slotX + 16, slotY + 16, SLOT);
	}
}
