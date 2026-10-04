package io.github.jimbozoomer.jugcraft.nuclear;

import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

/** A nuclear item with a one-line description under its name (the Geiger counter, the check source). */
public class NuclearItem extends Item {
	private final String tooltip;

	public NuclearItem(Properties properties, String tooltip) {
		super(properties);
		this.tooltip = tooltip;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> lines, TooltipFlag flag) {
		super.appendHoverText(stack, context, display, lines, flag);
		lines.accept(Component.translatable(tooltip).withStyle(ChatFormatting.GRAY));
	}
}
