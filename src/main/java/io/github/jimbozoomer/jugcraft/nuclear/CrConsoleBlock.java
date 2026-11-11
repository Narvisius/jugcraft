package io.github.jimbozoomer.jugcraft.nuclear;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A control room operator's console, the shift manager's desk or the remote shutdown station (older rooms rounds 5 and 7:
 * ControlRoomConsoles; three blocks
 * wide, two high: its desk in the lower row, the screens' tops and the position's plate in the upper). Its hitbox is
 * the desk and the screens on their bridge. A click on a screen steps it to its next page; anywhere else says what the
 * console reads and what its screens show.
 */
public class CrConsoleBlock extends LivePanelBlock {
	public CrConsoleBlock(Properties properties, int variants) {
		super(properties, 3, 2, variants);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		Direction facing = state.getValue(FACING);
		if (state.getValue(partProperty()) / width == 0) {
			// The desk to its rubber edge; the screens' bridge and their lower half on it.
			return Shapes.or(turned(new double[] {0, 0, 0.4, 16, 12.6, 16}, facing), turned(new double[] {0, 12.6, 0.4, 16, 16, 4.6}, facing));
		}
		// The screens' upper half (and the plate over the middle ones).
		return turned(new double[] {0, 0, 1.0, 16, 8.4, 3.8}, facing);
	}

	@Override
	protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return state.getValue(partProperty()) / width == 0 ? getShape(state, level, pos, context) : Shapes.empty();
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}
		Direction facing = state.getValue(FACING);
		BlockPos origin = pos.subtract(offset(facing, state.getValue(partProperty())));
		if (!(level.getBlockEntity(origin) instanceof LivePanelBlockEntity panel)) {
			return InteractionResult.PASS;
		}
		double[] at = local(facing, origin, hit.getLocation());
		// The layout is across the whole piece; the placing block is its middle column.
		double x = at[0] + 16 * (origin() % width);
		int m = at[2] < 6.0 ? ControlRoomConsoles.screenAt(ControlRoomConsoles.screens(state) == 2, x, at[1]) : -1;
		if (m >= 0) {
			String[] pages = ControlRoomConsoles.pages(state);
			ControlRoomConsoles.step(panel, m, pages.length);
			panel.changedAndSync((ServerLevel) level);
			level.playSound(null, pos, SoundEvents.STONE_BUTTON_CLICK_ON, SoundSource.BLOCKS, 0.4F, 1.6F);
			player.sendOverlayMessage(Component.translatable("message.jugcraft.nuclear.crc_page", pages[ControlRoomConsoles.page(panel, m, pages.length)]));
			return InteractionResult.SUCCESS_SERVER;
		}
		player.sendOverlayMessage(Component.translatable("message.jugcraft.nuclear.crc_status",
				ControlRoomConsoles.describe(panel, panel.panel, (ServerLevel) level)));
		return InteractionResult.SUCCESS_SERVER;
	}
}
