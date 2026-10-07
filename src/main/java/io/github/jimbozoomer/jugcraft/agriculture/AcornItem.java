package io.github.jimbozoomer.jugcraft.agriculture;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.gameevent.GameEvent;

/**
 * An acorn (fall addition 24): squirrels gather and bury them, it roasts into Roasted Acorns, and used on the top of
 * grass or other earth an oak sapling grows on, with open air above it, is planted, as an oak sapling. Planting needs
 * build rights there.
 */
public class AcornItem extends Item {
	public AcornItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		Level level = context.getLevel();
		BlockPos ground = context.getClickedPos();
		BlockPos spot = ground.above();
		Player player = context.getPlayer();
		if (context.getClickedFace() != Direction.UP || !level.getBlockState(spot).isAir() || !Werewolves.woodlandFloor(level, spot)
				|| (player != null && !player.mayUseItemAt(spot, Direction.UP, context.getItemInHand()))) {
			return InteractionResult.PASS;
		}
		if (!level.isClientSide()) {
			level.setBlock(spot, Blocks.OAK_SAPLING.defaultBlockState(), Block.UPDATE_ALL);
			level.playSound(null, spot, SoundEvents.CROP_PLANTED, SoundSource.BLOCKS, 1.0F, 1.0F);
			level.gameEvent(player, GameEvent.BLOCK_PLACE, spot);
			context.getItemInHand().consume(1, player);
		}
		return InteractionResult.SUCCESS;
	}
}
