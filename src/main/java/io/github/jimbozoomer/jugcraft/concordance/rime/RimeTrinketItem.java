package io.github.jimbozoomer.jugcraft.concordance.rime;

import eu.pb4.trinkets.api.callback.TrinketCallback;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

/**
 * A worn piece of the travel kit ({@link RimeGear}): a focus of water in the Spell Focus slot, the Water Bangle in a
 * Bracelet slot (Ember's Trinkets slots, given by data). It gives no attribute and holds no Focus: what it does is read
 * where it works (RimeGear.holdTicks, RimeGear.bangle), only from a slot that applies effects, never from the hand.
 */
public class RimeTrinketItem extends Item implements TrinketCallback {
	public RimeTrinketItem(Properties properties) {
		super(properties);
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip,
			TooltipFlag flag) {
		Identifier id = BuiltInRegistries.ITEM.getKey(this);
		tooltip.accept(Component.translatable("tooltip." + id.getNamespace() + "." + id.getPath()).withStyle(ChatFormatting.GRAY));
	}
}
