package io.github.jimbozoomer.jugcraft.materials;

import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

/**
 * A material item with a lore line under its name, in grey italics, from {@code tooltip.jugcraft.<id>}: a metal's ingot
 * when its entry in tools/materials.py has "lore" ({@link MetalFamily.Builder#lore}). Thallite's reads "Green as a new
 * shoot." Only the tooltip differs from a plain item.
 */
public class LoreItem extends Item {
	public LoreItem(Properties properties) {
		super(properties);
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip,
			TooltipFlag flag) {
		tooltip.accept(Component.translatable("tooltip.jugcraft." + BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath())
				.withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
	}
}
