package io.github.jimbozoomer.jugcraft.weapons;

import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

/** An item with a grey line saying what it does, from {@code tooltip.jugcraft.<id>} (batch 31: grenades, mask, medicines). */
public class DescribedItem extends Item {
	public DescribedItem(Properties properties) {
		super(properties);
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip,
			TooltipFlag flag) {
		describe(stack, tooltip);
	}

	static void describe(ItemStack stack, Consumer<Component> tooltip) {
		tooltip.accept(Component.translatable("tooltip.jugcraft." + BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath())
				.withStyle(ChatFormatting.GRAY));
	}
}
