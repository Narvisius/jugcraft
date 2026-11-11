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
 * The plant computer room's clean agent stations by its door (older rooms round 2: ServerRoom): the abort station (each
 * click holds the pre-discharge countdown a few seconds: keep clicking to keep holding it) and the manual release
 * station (pulled, the countdown starts at once; no abort holds it).
 */
public class AgentStationBlock extends FurnitureBlock {
	private final boolean abort;

	public AgentStationBlock(Properties properties, boolean abort, double[]... boxes) {
		super(properties, boxes);
		this.abort = abort;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!level.isClientSide()) {
			if (abort) {
				level.playSound(null, pos, SoundEvents.STONE_BUTTON_CLICK_ON, SoundSource.BLOCKS, 0.8F, 1.0F);
				ServerRoom.abort((ServerLevel) level, pos, player);
			} else {
				level.playSound(null, pos, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 1.0F, 0.8F);
				ServerRoom.pull((ServerLevel) level, pos, player);
			}
		}
		return InteractionResult.SUCCESS;
	}
}
