package io.github.jimbozoomer.jugcraft.lair;

import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;

/**
 * Sealed Orders: used on a Field Telephone that stands in a trench, they open the Foundry Trench ({@link OrdersRite},
 * which hears the call before the telephone answers its own use) and are used up. Used on anything else, they say where
 * to use them.
 */
public class SealedOrdersItem extends Item {
	public SealedOrdersItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		Player player = context.getPlayer();
		if (player != null && !context.getLevel().isClientSide()) {
			player.sendOverlayMessage(Component.translatable("message.jugcraft.lair.orders.not_phone"));
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip,
			TooltipFlag flag) {
		tooltip.accept(Component.translatable("tooltip.jugcraft.sealed_orders").withStyle(ChatFormatting.GRAY));
	}
}
