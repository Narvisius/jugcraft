package io.github.jimbozoomer.jugcraft.concordance.dreaming;

import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

/** Dreamglass (roadmap step 22): what a dream brings back, the stuff of ward sigils. */
public class DreamglassItem extends Item {
	public DreamglassItem(Properties properties) {
		super(properties);
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip,
			TooltipFlag flag) {
		tooltip.accept(Component.translatable("tooltip.jugcraft.dreamglass").withStyle(ChatFormatting.GRAY));
	}
}
