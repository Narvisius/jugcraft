package io.github.jimbozoomer.jugcraft.agriculture;

import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;

/**
 * An Aura Candle as an item: made by dipping (never crafted), it carries its {@link CandleMix}, is coloured by its
 * outermost layer (the dyed colour, which its model tints by) and named for its wax and scents. Its tooltip tells its
 * scents, radius, burn time and layers.
 */
public class AuraCandleItem extends BlockItem {
	private static final String TOOLTIP = "tooltip.jugcraft.aura_candle.";

	public AuraCandleItem(Block block, Properties properties) {
		super(block, properties);
	}

	/** A candle item of {@code mix}. */
	public static ItemStack make(CandleMix mix) {
		ItemStack stack = new ItemStack(JugcraftAgriculture.item("aura_candle"));
		stack.set(JugcraftAgriculture.CANDLE_MIX, mix);
		stack.set(DataComponents.DYED_COLOR, new DyedItemColor(mix.color()));
		stack.set(DataComponents.ITEM_NAME, mix.name());
		return stack;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
		super.appendHoverText(stack, context, display, tooltip, flag);
		CandleMix mix = stack.get(JugcraftAgriculture.CANDLE_MIX);
		if (mix == null) {
			return;
		}
		if (mix.muddled()) {
			tooltip.accept(Component.translatable(TOOLTIP + "muddled").withStyle(ChatFormatting.RED));
		} else if (mix.scents().isEmpty()) {
			tooltip.accept(Component.translatable(TOOLTIP + "unscented").withStyle(ChatFormatting.GRAY));
		} else {
			MutableComponent scents = Component.empty();
			for (int i = 0; i < mix.scents().size(); i++) {
				scents.append(i == 0 ? Component.empty() : Component.literal(", ")).append(mix.scents().get(i).displayName());
			}
			tooltip.accept(scents);
			tooltip.accept(Component.translatable(TOOLTIP + "radius", mix.radius()).withStyle(ChatFormatting.GRAY));
		}
		int seconds = mix.remaining() / 20;
		tooltip.accept(Component.translatable(TOOLTIP + "burns", seconds / 60, String.format("%02d", seconds % 60)).withStyle(ChatFormatting.GRAY));
		tooltip.accept(Component.translatable(TOOLTIP + "layers", mix.dips(), AuraCandleBlock.MAX_DIPS).withStyle(ChatFormatting.GRAY));
		if (mix.bright()) {
			tooltip.accept(Component.translatable(TOOLTIP + "bright").withStyle(ChatFormatting.YELLOW));
		}
		if (mix.lasting()) {
			tooltip.accept(Component.translatable(TOOLTIP + "lasting").withStyle(ChatFormatting.RED));
		}
	}
}
