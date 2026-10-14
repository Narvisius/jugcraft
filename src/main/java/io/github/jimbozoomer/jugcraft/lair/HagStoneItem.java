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
 * The Hag Stone: looked through at night, standing in shallow water or on mud in a wetland, it opens the Lantern Fen
 * ({@link HagStoneRite}) and is used up. Every look, answered or not, rests the stone for {@value HagStoneRite#COOLDOWN}
 * ticks.
 */
public class HagStoneItem extends Item {
	public HagStoneItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (level instanceof ServerLevel server && player instanceof ServerPlayer serverPlayer) {
			ItemStack stone = player.getItemInHand(hand);
			player.getCooldowns().addCooldown(stone, HagStoneRite.COOLDOWN);
			HagStoneRite.look(server, serverPlayer, stone);
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip,
			TooltipFlag flag) {
		tooltip.accept(Component.translatable("tooltip.jugcraft.hag_stone").withStyle(ChatFormatting.GRAY));
	}
}
