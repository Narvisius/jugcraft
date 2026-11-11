package io.github.jimbozoomer.jugcraft.nuclear;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The switchgear room's log desk at its laptop end (older rooms round 1; SwitchgearRoom): one block, its hitbox the
 * desk's; the laptop's screen live. A click says what it shows.
 */
public class SgrLaptopBlock extends LivePanelBlock {
	public SgrLaptopBlock(Properties properties) {
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
		player.sendOverlayMessage(Component.translatable("message.jugcraft.nuclear.sgr_laptop", SwitchgearRoom.describe(SwitchgearRoom.nearest(level, pos))));
		return InteractionResult.SUCCESS_SERVER;
	}
}
