package io.github.jimbozoomer.jugcraft.nuclear;

import io.github.jimbozoomer.jugcraft.tower.FurnitureBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * The hand geometry reader at the entry lane (older rooms round 6; tools/entrance_hall.py hand_reader): put a hand on it
 * (a click) and it reads it, beeps and verifies whoever it is.
 */
public class HandReaderBlock extends FurnitureBlock {
	public HandReaderBlock(Properties properties, double[] shape) {
		super(properties, shape);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}
		level.playSound(null, pos, SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.BLOCKS, 0.5F, 1.8F);
		player.sendOverlayMessage(Component.translatable("message.jugcraft.nuclear.ac_hand", player.getName()));
		return InteractionResult.SUCCESS_SERVER;
	}
}
