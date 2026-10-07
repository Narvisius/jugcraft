package io.github.jimbozoomer.jugcraft.agriculture;

import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

/** The Stonemason's Chisel: used on a headstone it opens the epitaph screen ({@link Epitaphs}); each epitaph cut wears it by one. */
public class StonemasonsChiselItem extends Item {
	public StonemasonsChiselItem(Properties properties) {
		super(properties);
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
		super.appendHoverText(stack, context, display, tooltip, flag);
		tooltip.accept(Component.translatable("tooltip.jugcraft.chisel").withStyle(ChatFormatting.GRAY));
	}
}
