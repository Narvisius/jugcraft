package io.github.jimbozoomer.jugcraft.agriculture;

import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

/**
 * A Pumpkin Stencil: a carved design traced from a pumpkin ({@link JugcraftAgriculture#STENCIL}, face 0).
 * Hold it in the other hand while using the Carving Knife and the carving screen offers it next to the
 * starter faces. A stencil without a design (from a command) offers nothing.
 */
public class PumpkinStencilItem extends Item {
	public PumpkinStencilItem(Properties properties) {
		super(properties);
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
		PumpkinCarving design = stack.get(JugcraftAgriculture.STENCIL);
		if (design == null || design.isBlank(0)) {
			tooltip.accept(Component.translatable("item.jugcraft.pumpkin_stencil.blank").withStyle(ChatFormatting.GRAY));
			return;
		}
		tooltip.accept(Component.translatable("item.jugcraft.pumpkin_stencil.holes", design.count(PumpkinCarving.CUT),
				design.count(PumpkinCarving.SHAVED)).withStyle(ChatFormatting.GRAY));
		tooltip.accept(Component.translatable("item.jugcraft.pumpkin_stencil.hint").withStyle(ChatFormatting.GRAY));
	}
}
