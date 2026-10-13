package io.github.jimbozoomer.jugcraft.concordance.sky;

import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;

/** The observatory's item, with a line saying what it is for ({@code tooltip.jugcraft.<id>}). */
public class SkyItem extends BlockItem {
	public SkyItem(Block block, Properties properties) {
		super(block, properties);
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip,
			TooltipFlag flag) {
		Identifier id = BuiltInRegistries.ITEM.getKey(this);
		tooltip.accept(Component.translatable("tooltip." + id.getNamespace() + "." + id.getPath()).withStyle(ChatFormatting.GRAY));
	}
}
