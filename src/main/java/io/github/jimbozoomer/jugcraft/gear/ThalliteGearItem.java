package io.github.jimbozoomer.jugcraft.gear;

import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

/**
 * A thallite tool or armor piece, plain or Earthbound (docs/features/thallite.md). It is a plain item in every number;
 * only its tooltip differs: its traits, Regrowth and, on Earthbound armor, Rooted, each named and described while
 * Shift is held (docs/features/trait-details.md). An Earthbound piece also has its grey line of lore first, as the
 * other styled armor does. What the traits do is {@link ThalliteGear}.
 */
public class ThalliteGearItem extends Item {
	private final boolean earthbound;

	public ThalliteGearItem(Properties properties, boolean earthbound) {
		super(properties);
		this.earthbound = earthbound;
	}

	/** Whether this is an Earthbound piece, with Rooted. */
	public boolean earthbound() {
		return earthbound;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip,
			TooltipFlag flag) {
		if (earthbound) {
			tooltip.accept(Component.translatable("tooltip.jugcraft." + BuiltInRegistries.ITEM.getKey(this).getPath())
					.withStyle(ChatFormatting.GRAY));
		}
		traits(TraitTooltips.of(tooltip));
	}

	/** The traits: Regrowth, then Rooted on Earthbound armor, each a name and, while Shift is held, what it does. */
	public void traits(TraitTooltips traits) {
		traits.trait("tooltip.jugcraft.thallite.regrowth.trait", ChatFormatting.GREEN,
				Component.translatable("tooltip.jugcraft.thallite.regrowth"));
		if (earthbound) {
			traits.trait("tooltip.jugcraft.thallite.rooted.trait", ChatFormatting.GOLD,
					Component.translatable("tooltip.jugcraft.thallite.rooted"));
		}
		traits.end();
	}
}
