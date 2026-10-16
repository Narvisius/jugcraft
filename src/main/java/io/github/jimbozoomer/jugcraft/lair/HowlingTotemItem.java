package io.github.jimbozoomer.jugcraft.lair;

import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

/**
 * The Howling Totem: used at night in werewolf country, under the open sky and on woodland floor, it opens the Moonlit
 * Ring ({@link HowlingTotemRite}) and is used up. Every use, answered or not, rests the totem for
 * {@value HowlingTotemRite#COOLDOWN} ticks.
 */
public class HowlingTotemItem extends Item {
	public HowlingTotemItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (level instanceof ServerLevel server && player instanceof ServerPlayer serverPlayer) {
			ItemStack totem = player.getItemInHand(hand);
			player.getCooldowns().addCooldown(totem, HowlingTotemRite.COOLDOWN);
			HowlingTotemRite.howl(server, serverPlayer, totem);
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip,
			TooltipFlag flag) {
		tooltip.accept(Component.translatable("tooltip.jugcraft.howling_totem").withStyle(ChatFormatting.GRAY));
	}
}
