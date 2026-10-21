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
 * The Altar Keystone: set into the Ancient Altar of Adam-ondi-Ahman at night, it opens the Cydonian Sanctum on Mars
 * ({@link AltarKeystoneRite}, which hears the setting before the altar's own use) and crumbles to dust. Used on anything
 * else, it says where to set it.
 */
public class AltarKeystoneItem extends Item {
	public AltarKeystoneItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		Player player = context.getPlayer();
		if (player != null && !context.getLevel().isClientSide()) {
			player.sendOverlayMessage(Component.translatable("message.jugcraft.lair.keystone.not_altar"));
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip,
			TooltipFlag flag) {
		tooltip.accept(Component.translatable("tooltip.jugcraft.altar_keystone").withStyle(ChatFormatting.GRAY));
	}
}
