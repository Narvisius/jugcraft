package io.github.jimbozoomer.jugcraft.nuclear;

import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

/**
 * A piece of the hazmat suit: hood with respirator, coverall with air tank and gloves, trousers, or boots. Each worn
 * piece stops a quarter of what the full suit stops (90% of nuclear radiation). Drawn in 3D on the wearer by the
 * client's hazmat layer.
 */
public class HazmatItem extends Item {
	public HazmatItem(Properties properties) {
		super(properties);
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
		super.appendHoverText(stack, context, display, tooltip, flag);
		tooltip.accept(Component.translatable("tooltip.jugcraft.hazmat").withStyle(ChatFormatting.GRAY));
	}
}
