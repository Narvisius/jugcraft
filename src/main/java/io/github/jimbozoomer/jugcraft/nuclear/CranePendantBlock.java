package io.github.jimbozoomer.jugcraft.nuclear;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * The turbine hall's crane pendant station (turbine hall part 3, tools/hall_fittings.py crane_pendant): on a wall
 * column of the hall, its buttons run the nearest overhead crane in reach as a real pendant does (HallCrane): hold
 * NORTH, SOUTH, WEST or EAST to run the bridge or trolley that way, the main or auxiliary hook UP or DOWN, let go and it
 * stops; the red STOP takes the crane's power off, START puts it back (the POWER lamp lit); the horn sounds it.
 */
public class CranePendantBlock extends WallLivePanelBlock {
	/** Its buttons' middles on its front and their reach (model pixels, both blocks; HallCrane.BUTTONS' order). */
	static final double[][] BUTTONS = {{8.0, 22.7, 1.6}, {5.6, 19.6, 1.2}, {5.6, 15.5, 1.3}, {10.4, 15.5, 1.3}, {5.6, 12.2, 1.3},
			{10.4, 12.2, 1.3}, {5.6, 8.9, 1.3}, {10.4, 8.9, 1.3}, {5.6, 6.0, 1.3}, {10.4, 6.0, 1.3}, {8.0, 3.8, 1.1}};

	public CranePendantBlock(Properties properties) {
		super(properties, 1, 6.6);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}
		Direction facing = state.getValue(FACING);
		BlockPos origin = pos.subtract(offset(facing, state.getValue(partProperty())));
		double[] at = local(facing, origin, hit.getLocation());
		int button = -1;
		double best = Double.MAX_VALUE;
		for (int k = 0; k < BUTTONS.length; k++) {
			double d = Math.hypot(at[0] - BUTTONS[k][0], at[1] - BUTTONS[k][1]);
			if (d <= BUTTONS[k][2] && d < best) {
				button = k;
				best = d;
			}
		}
		if (button < 0) {
			return InteractionResult.PASS;
		}
		EquipmentBlockEntity crane = HallCrane.near((ServerLevel) level, origin);
		if (crane == null) {
			player.sendOverlayMessage(Component.translatable("message.jugcraft.nuclear.crane_none"));
			return InteractionResult.SUCCESS_SERVER;
		}
		if (button < 2 || button == BUTTONS.length - 1) {
			// STOP, START and the horn click (a held motion button sounds its motor starting instead: HallCrane.press).
			level.playSound(null, pos, SoundEvents.STONE_BUTTON_CLICK_ON, SoundSource.BLOCKS, 0.5F, 1.3F);
		}
		HallCrane.press((ServerLevel) level, crane, HallCrane.BUTTONS[button], player);
		return InteractionResult.SUCCESS_SERVER;
	}
}
