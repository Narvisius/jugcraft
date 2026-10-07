package io.github.jimbozoomer.jugcraft.agriculture;

import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;

/** A Cider Barrel as an item: one broken with cider in it keeps it ({@link BarrelCider}), and says how much. */
public class CiderBarrelItem extends BlockItem {
	public CiderBarrelItem(Block block, Properties properties) {
		super(block, properties);
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
		super.appendHoverText(stack, context, display, tooltip, flag);
		BarrelCider cider = stack.get(JugcraftAgriculture.BARREL_CIDER);
		if (cider != null) {
			tooltip.accept(Component.translatable("tooltip.jugcraft.cider_barrel.servings", cider.servings(), CiderBarrelBlockEntity.CAPACITY)
					.withStyle(ChatFormatting.GRAY));
		}
	}
}
