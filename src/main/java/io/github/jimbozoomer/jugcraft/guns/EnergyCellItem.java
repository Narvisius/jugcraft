package io.github.jimbozoomer.jugcraft.guns;

import io.github.jimbozoomer.jugcraft.tools.Chargeable;
import io.github.jimbozoomer.jugcraft.tools.PoweredToolItem;
import io.github.jimbozoomer.jugcraft.tools.ToolUpgrades;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

/**
 * The Energy Cell (slice 8D): the energy weapons' ammunition. It holds charge like the powered tools
 * ({@link Chargeable}, the jugcraft:energy component, in JE) and fills at the Charging Station; capacity modules fit it
 * too. A reload draws each round's charge ({@link JugcraftGuns#CHARGE}) from the cells in the inventory and leaves the
 * cells, to be charged again ({@link GunShots}). Only the guns draw on it.
 */
public class EnergyCellItem extends Item implements Chargeable {
	/** JE a cell holds without capacity modules (tools/guns.py CELL_CAPACITY). */
	public static final long CAPACITY = 10_000;

	public EnergyCellItem(Properties properties) {
		super(properties);
	}

	@Override
	public long baseCapacity() {
		return CAPACITY;
	}

	@Override
	public boolean isBarVisible(ItemStack stack) {
		return true;
	}

	@Override
	public int getBarWidth(ItemStack stack) {
		return PoweredToolItem.barWidth(stack);
	}

	@Override
	public int getBarColor(ItemStack stack) {
		return PoweredToolItem.BAR_COLOR;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip,
			TooltipFlag flag) {
		tooltip.accept(PoweredToolItem.energyLine(stack));
		ToolUpgrades.appendTooltip(stack, tooltip);
		tooltip.accept(Component.translatable("tooltip.jugcraft.guns.energy_cell").withStyle(ChatFormatting.GRAY));
	}
}
