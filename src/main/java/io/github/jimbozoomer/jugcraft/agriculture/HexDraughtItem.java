package io.github.jimbozoomer.jugcraft.agriculture;

import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

/**
 * A draught drawn from a hex brew (fall addition 21): drunk like a potion, it does what {@link Hexes#drink} says. The
 * Giant's Draught is refused, and not used up, where there is no room to grow.
 */
public class HexDraughtItem extends Item {
	private final BubblingCauldronBlock.Brew hex;

	public HexDraughtItem(Properties properties, BubblingCauldronBlock.Brew hex) {
		super(properties);
		this.hex = hex;
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (hex == BubblingCauldronBlock.Brew.GIANT && !Hexes.roomToGrow(player)) {
			if (player instanceof ServerPlayer drinker) {
				drinker.sendOverlayMessage(Component.translatable("message.jugcraft.hex.no_room"));
			}
			return InteractionResult.FAIL;
		}
		return super.use(level, player, hand);
	}

	@Override
	public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
		if (entity instanceof ServerPlayer drinker) {
			if (hex == BubblingCauldronBlock.Brew.GIANT && !Hexes.roomToGrow(drinker)) {
				drinker.sendOverlayMessage(Component.translatable("message.jugcraft.hex.no_room"));
				return stack;
			}
			Hexes.drink(drinker, hex);
		}
		return super.finishUsingItem(stack, level, entity);
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
		super.appendHoverText(stack, context, display, tooltip, flag);
		tooltip.accept(Component.translatable("tooltip.jugcraft.hex." + switch (hex) {
			case SHRINKING -> "shrinking";
			case GIANT -> "giant";
			default -> "flying";
		}).withStyle(ChatFormatting.DARK_PURPLE));
	}
}
