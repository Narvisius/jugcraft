package io.github.jimbozoomer.jugcraft.concordance.reliquary;

import io.github.jimbozoomer.jugcraft.concordance.relic.RelicState;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

/**
 * A relic (roadmap step 20). The item does nothing by itself: {@link Reliquary} works it where it is, in the contexts its
 * definition names, and {@link ReliquaryShrineBlockEntity} works it installed. Its charge, bond and last pulse are its
 * {@link Reliquary#RELIC} component.
 */
public class RelicItem extends Item {
	public RelicItem(Properties properties) {
		super(properties);
	}

	/** Pulses change only the relic's component: no re-equip bob in the hand. */
	public boolean allowComponentsUpdateAnimation(Player player, InteractionHand hand, ItemStack oldStack, ItemStack newStack) {
		return false;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip,
			TooltipFlag flag) {
		// Only what the relic itself carries: its definition is the server's and is not sent to clients, so where it works
		// is in its description, generated from the same data.
		Identifier id = BuiltInRegistries.ITEM.getKey(this);
		tooltip.accept(Component.translatable("tooltip." + id.getNamespace() + "." + id.getPath()).withStyle(ChatFormatting.GRAY));
		RelicState state = stack.getOrDefault(Reliquary.RELIC, RelicState.FRESH);
		tooltip.accept(Component.translatable("tooltip.jugcraft.concordance.relic.charge", state.charge()).withStyle(ChatFormatting.AQUA));
		if (state.owner() != null) {
			tooltip.accept(Component.translatable("tooltip.jugcraft.concordance.relic.bound").withStyle(ChatFormatting.GOLD));
		}
	}
}
