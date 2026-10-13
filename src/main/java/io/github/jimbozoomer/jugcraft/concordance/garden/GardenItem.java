package io.github.jimbozoomer.jugcraft.concordance.garden;

import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;

/**
 * The garden's items (roadmap step 14): the crops, which plant themselves in a Verdant Bed, the bed and the living
 * devices, each with a line saying what it is for ({@code tooltip.jugcraft.<id>}).
 */
public class GardenItem extends BlockItem {
	public GardenItem(Block block, Properties properties) {
		super(block, properties);
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip,
			TooltipFlag flag) {
		line(this, tooltip);
	}

	static void line(Item item, Consumer<Component> tooltip) {
		Identifier id = BuiltInRegistries.ITEM.getKey(item);
		tooltip.accept(Component.translatable("tooltip." + id.getNamespace() + "." + id.getPath()).withStyle(ChatFormatting.GRAY));
	}

	/** Verdant Chaff: an item with its line, and nothing to place. */
	public static class Plain extends Item {
		public Plain(Properties properties) {
			super(properties);
		}

		@Override
		public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip,
				TooltipFlag flag) {
			line(this, tooltip);
		}
	}
}
