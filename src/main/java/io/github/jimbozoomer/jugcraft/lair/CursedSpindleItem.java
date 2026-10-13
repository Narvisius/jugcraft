package io.github.jimbozoomer.jugcraft.lair;

import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

/** The Cursed Spindle: used on a Spinning Wheel at night it opens the Spindle Loft ({@link SpindleRite}), and is used up. */
public class CursedSpindleItem extends Item {
	public CursedSpindleItem(Properties properties) {
		super(properties);
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip,
			TooltipFlag flag) {
		tooltip.accept(Component.translatable("tooltip.jugcraft.cursed_spindle").withStyle(ChatFormatting.GRAY));
	}
}
