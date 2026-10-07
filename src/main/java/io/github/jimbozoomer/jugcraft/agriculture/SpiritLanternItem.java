package io.github.jimbozoomer.jugcraft.agriculture;

import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

/**
 * The Spirit Lantern: a brass lantern with a pale green flame behind amethyst glass. Held in either hand, it reveals
 * restless spirits within {@value Spirits#REVEAL_RADIUS} blocks to everyone near ({@link RestlessSpirit#look}); it does
 * nothing else, and needs no fuel.
 */
public class SpiritLanternItem extends Item {
	public SpiritLanternItem(Properties properties) {
		super(properties);
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
		tooltip.accept(Component.translatable("item.jugcraft.spirit_lantern.tooltip", Spirits.REVEAL_RADIUS).withStyle(ChatFormatting.DARK_AQUA));
	}
}
