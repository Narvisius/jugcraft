package io.github.jimbozoomer.jugcraft.chemistry;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Soap (batch 15): lye saponifies fat into soap. Use a bar to wash every status effect off, good or bad, as a bucket of
 * milk does, without the bucket. One bar a wash.
 */
public class SoapItem extends Item {
	public SoapItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (player.getActiveEffects().isEmpty()) {
			return InteractionResult.PASS;
		}
		if (!level.isClientSide()) {
			player.removeAllEffects();
			ItemStack stack = player.getItemInHand(hand);
			stack.consume(1, player);
		}
		return InteractionResult.SUCCESS;
	}
}
