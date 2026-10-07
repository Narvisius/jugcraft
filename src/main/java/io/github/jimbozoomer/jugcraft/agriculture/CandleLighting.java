package io.github.jimbozoomer.jugcraft.agriculture;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.gameevent.GameEvent;

/**
 * Lighting and snuffing a candle-lit decoration, as the Haunted Chandelier and Floating Candles are: flint and steel
 * (worn by one use) or a fire charge (used up) lights it; an empty hand snuffs it. The Shadow Puppet Lamp, the Mini
 * Pumpkin Stack and the Floating Witch Hat share it.
 */
public final class CandleLighting {
	private CandleLighting() {
	}

	/** Flint and steel or a fire charge lights {@code lit}; anything else does what it would on any block. */
	public static InteractionResult light(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
			BooleanProperty lit) {
		boolean flint = stack.is(Items.FLINT_AND_STEEL);
		if (!flint && !stack.is(Items.FIRE_CHARGE) || state.getValue(lit)) {
			return InteractionResult.TRY_WITH_EMPTY_HAND;
		}
		if (!level.isClientSide()) {
			level.setBlock(pos, state.setValue(lit, true), Block.UPDATE_ALL);
			level.playSound(null, pos, flint ? SoundEvents.FLINTANDSTEEL_USE : SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 1.0F,
					level.getRandom().nextFloat() * 0.4F + 0.8F);
			level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
			if (flint) {
				stack.hurtAndBreak(1, player, hand);
			} else {
				stack.consume(1, player);
			}
		}
		return InteractionResult.SUCCESS;
	}

	/** An empty hand snuffs {@code lit}. */
	public static InteractionResult snuff(BlockState state, Level level, BlockPos pos, Player player, BooleanProperty lit) {
		if (!state.getValue(lit)) {
			return InteractionResult.PASS;
		}
		if (!level.isClientSide()) {
			level.setBlock(pos, state.setValue(lit, false), Block.UPDATE_ALL);
			level.playSound(null, pos, SoundEvents.CANDLE_EXTINGUISH, SoundSource.BLOCKS, 1.0F, 1.0F);
			level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
		}
		return InteractionResult.SUCCESS;
	}
}
