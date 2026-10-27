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
 * The Thunder Vane: raised high in the mountains under the open sky while it rains, it opens the Crown of Thunder
 * ({@link ThunderVaneRite}) and is used up. Every use, answered or not, rests the vane for {@value ThunderVaneRite#COOLDOWN}
 * ticks.
 */
public class ThunderVaneItem extends Item {
	public ThunderVaneItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (level instanceof ServerLevel server && player instanceof ServerPlayer serverPlayer) {
			ItemStack vane = player.getItemInHand(hand);
			player.getCooldowns().addCooldown(vane, ThunderVaneRite.COOLDOWN);
			ThunderVaneRite.raise(server, serverPlayer, vane);
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip,
			TooltipFlag flag) {
		tooltip.accept(Component.translatable("tooltip.jugcraft.thunder_vane").withStyle(ChatFormatting.GRAY));
	}
}
