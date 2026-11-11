package io.github.jimbozoomer.jugcraft.nuclear;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A DC equipment room's log desk at its laptop end (older rooms round 3; Justin: "Log desk laptop"): one block, its
 * hitbox the desk's; the laptop's screen live, its switchboard's room (DcRoom.screen). A click steps its page (the room,
 * the feeders 01 to 09, 10 to 12) and says how the bus stands.
 */
public class DcLaptopBlock extends LivePanelBlock {
	public DcLaptopBlock(Properties properties) {
		super(properties, 1, 1, 1);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return turned(new double[] {0, 0, 1, 16, 16, 14.6}, state.getValue(FACING));
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}
		if (!(level.getBlockEntity(pos) instanceof LivePanelBlockEntity laptop)) {
			return InteractionResult.PASS;
		}
		laptop.page = (laptop.page + 1) % DcRoom.PAGES;
		laptop.changedMode((ServerLevel) level);
		level.playSound(null, pos, SoundEvents.STONE_BUTTON_CLICK_ON, SoundSource.BLOCKS, 0.3F, 1.8F);
		player.sendOverlayMessage(Component.translatable("message.jugcraft.nuclear.dc_laptop", laptop.page + 1, DcRoom.PAGES,
				DcRoom.describe(level, DcRoom.board(level, laptop))));
		return InteractionResult.SUCCESS_SERVER;
	}
}
