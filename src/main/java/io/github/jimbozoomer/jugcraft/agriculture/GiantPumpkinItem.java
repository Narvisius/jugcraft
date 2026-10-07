package io.github.jimbozoomer.jugcraft.agriculture;

import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;

/**
 * A whole giant pumpkin, picked up by breaking it ({@link GiantPumpkinBlock}): placing it puts the same pumpkin back,
 * its cube reaching away from the player. Its tooltip gives its size and, full grown, its weight and whether it is
 * carved or lit.
 */
public class GiantPumpkinItem extends BlockItem {
	public GiantPumpkinItem(Block block, Properties properties) {
		super(block, properties);
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
		GiantPumpkinData data = stack.getOrDefault(JugcraftAgriculture.GIANT_PUMPKIN, GiantPumpkinData.FULL_GROWN);
		int size = data.size();
		tooltip.accept(Component.translatable("item.jugcraft.giant_pumpkin.size", size, size, size).withStyle(ChatFormatting.GRAY));
		if (size == GiantPumpkinBlock.MAX_SIZE) {
			tooltip.accept(Component.translatable("item.jugcraft.giant_pumpkin.weight", data.weight()).withStyle(ChatFormatting.GOLD));
		}
		if (data.carved()) {
			tooltip.accept(Component.translatable(data.lit() ? "item.jugcraft.giant_pumpkin.carved_lit" : "item.jugcraft.giant_pumpkin.carved")
					.withStyle(ChatFormatting.GRAY));
		}
	}
}
