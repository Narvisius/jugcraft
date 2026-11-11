package io.github.jimbozoomer.jugcraft.nuclear;

import io.github.jimbozoomer.jugcraft.tower.FurnitureBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * A fire alarm pull station (punch list #15: the cable spreading room's, by its door). Pull it and the nearest
 * pre-action releasing panel goes into alarm and releases at once (SpreadingRoomFire.pull): the water goes into the
 * pipes, and the heads open where there is a fire.
 */
public class FirePullStationBlock extends FurnitureBlock {
	public FirePullStationBlock(Properties properties, double[]... boxes) {
		super(properties, boxes);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!level.isClientSide()) {
			level.playSound(null, pos, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 1.0F, 0.8F);
			SpreadingRoomFire.pull((ServerLevel) level, pos, player);
		}
		return InteractionResult.SUCCESS;
	}
}
