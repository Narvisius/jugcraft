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
 * The gatehouse's alarm station console (SecurityGatehouse; built as the control room's consoles: three blocks wide,
 * two high, three screens). A click on a screen steps it to its next page; anywhere else says what it watches.
 */
public class SecAlarmConsoleBlock extends LivePanelBlock {
	public SecAlarmConsoleBlock(Properties properties) {
		super(properties, 3, 2, 1);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		Direction facing = state.getValue(FACING);
		if (state.getValue(partProperty()) / width == 0) {
			return Shapes.or(turned(new double[] {0, 0, 0.4, 16, 12.6, 16}, facing), turned(new double[] {0, 12.6, 0.4, 16, 16, 4.6}, facing));
		}
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
		double x = at[0] + 16 * (origin() % width);
		int m = at[2] < 6.0 ? ControlRoomConsoles.screenAt(false, x, at[1]) : -1;
		if (m >= 0) {
			String[] pages = SecurityGatehouse.SAS_PAGES;
			ControlRoomConsoles.step(panel, m, pages.length);
			panel.changedAndSync((ServerLevel) level);
			level.playSound(null, pos, SoundEvents.STONE_BUTTON_CLICK_ON, SoundSource.BLOCKS, 0.4F, 1.6F);
			player.sendOverlayMessage(Component.translatable("message.jugcraft.nuclear.crc_page", pages[ControlRoomConsoles.page(panel, m, pages.length)]));
			return InteractionResult.SUCCESS_SERVER;
		}
		player.sendOverlayMessage(SecurityGatehouse.status(panel));
		return InteractionResult.SUCCESS_SERVER;
	}
}
