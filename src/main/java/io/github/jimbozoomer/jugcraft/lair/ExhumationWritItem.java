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
 * The Writ of Exhumation: served on a stone sarcophagus in a crypt, it opens the Archive of Last Names
 * ({@link ExhumationRite}, which hears the serving before the sarcophagus opens its own lid) and is used up. Used on
 * anything else, it says where to serve it.
 */
public class ExhumationWritItem extends Item {
	public ExhumationWritItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		Player player = context.getPlayer();
		if (player != null && !context.getLevel().isClientSide()) {
			player.sendOverlayMessage(Component.translatable("message.jugcraft.lair.writ.not_tomb"));
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip,
			TooltipFlag flag) {
		tooltip.accept(Component.translatable("tooltip.jugcraft.exhumation_writ").withStyle(ChatFormatting.GRAY));
	}
}
