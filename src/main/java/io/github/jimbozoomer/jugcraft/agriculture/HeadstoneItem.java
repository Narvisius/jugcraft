package io.github.jimbozoomer.jugcraft.agriculture;

import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;

/** A headstone as an item; one that carries an epitaph (broken from where it stood) shows it. */
public class HeadstoneItem extends BlockItem {
	public HeadstoneItem(Block block, Properties properties) {
		super(block, properties);
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
		super.appendHoverText(stack, context, display, tooltip, flag);
		Epitaph epitaph = stack.get(JugcraftAgriculture.EPITAPH);
		if (epitaph == null || epitaph.isBlank()) {
			return;
		}
		tooltip.accept(Component.translatable("tooltip.jugcraft.headstone.epitaph").withStyle(ChatFormatting.GRAY));
		for (String line : epitaph.lines()) {
			tooltip.accept(Component.literal("  " + line).withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
		}
	}
}
