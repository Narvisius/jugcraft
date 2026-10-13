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
 * The Frost Horn: blown at night standing on snow or ice, it opens the Glacier Hall ({@link FrostHornRite}) and is used
 * up. Every blow, answered or not, rests the horn for {@value FrostHornRite#COOLDOWN} ticks.
 */
public class FrostHornItem extends Item {
	public FrostHornItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (level instanceof ServerLevel server && player instanceof ServerPlayer serverPlayer) {
			ItemStack horn = player.getItemInHand(hand);
			player.getCooldowns().addCooldown(horn, FrostHornRite.COOLDOWN);
			FrostHornRite.blow(server, serverPlayer, horn);
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip,
			TooltipFlag flag) {
		tooltip.accept(Component.translatable("tooltip.jugcraft.frost_horn").withStyle(ChatFormatting.GRAY));
	}
}
