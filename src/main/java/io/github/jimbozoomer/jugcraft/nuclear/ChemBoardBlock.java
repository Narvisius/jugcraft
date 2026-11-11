package io.github.jimbozoomer.jugcraft.nuclear;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * The chemistry lab's plant chemistry status board (ChemLab; tools/admin_rooms.py chem_status_board): a wall LCD three
 * blocks across and two high. A click says how many results are in specification and at their action level.
 */
public class ChemBoardBlock extends WallLivePanelBlock {
	public ChemBoardBlock(Properties properties) {
		super(properties, 3, 1, 2.4);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}
		BlockPos origin = pos.subtract(offset(state.getValue(FACING), state.getValue(partProperty())));
		if (level.getBlockEntity(origin) instanceof LivePanelBlockEntity b) {
			player.sendOverlayMessage(ChemLab.boardStatus(b));
		}
		return InteractionResult.SUCCESS_SERVER;
	}
}
