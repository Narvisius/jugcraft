package io.github.jimbozoomer.jugcraft.lair;

import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;

/**
 * The Kiln Seal: pressed into a magma block in the Nether or a volcanic land, at any hour, it opens the Cinder Kiln
 * ({@link KilnSealRite}) and is used up. Every press, answered or not, rests the seal for {@value KilnSealRite#COOLDOWN}
 * ticks.
 */
public class KilnSealItem extends Item {
	public KilnSealItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		if (context.getLevel() instanceof ServerLevel level && context.getPlayer() instanceof ServerPlayer player) {
			ItemStack seal = context.getItemInHand();
			player.getCooldowns().addCooldown(seal, KilnSealRite.COOLDOWN);
			KilnSealRite.press(level, player, context.getClickedPos(), seal);
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip,
			TooltipFlag flag) {
		tooltip.accept(Component.translatable("tooltip.jugcraft.kiln_seal").withStyle(ChatFormatting.GRAY));
	}
}
