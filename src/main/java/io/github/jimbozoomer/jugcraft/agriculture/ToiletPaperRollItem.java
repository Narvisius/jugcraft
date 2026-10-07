package io.github.jimbozoomer.jugcraft.agriculture;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** A Toilet Paper Roll: throw it over trees and fences ({@link ToiletPaperRoll}). */
public class ToiletPaperRollItem extends Item {
	public ToiletPaperRollItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.SNOWBALL_THROW, SoundSource.PLAYERS, 0.5F, 0.6F);
		if (level instanceof ServerLevel server) {
			ToiletPaperRoll roll = new ToiletPaperRoll(server, player, stack);
			roll.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 1.1F, 1.0F);
			server.addFreshEntity(roll);
		}
		stack.consume(1, player);
		return InteractionResult.SUCCESS;
	}
}
