package io.github.jimbozoomer.jugcraft.agriculture;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * The memorial bench (graveyard pack 4): a headstone in every other way (it weathers, waxes, takes its inscription on
 * the plaque of its top rail and breaks as one), and a seat for two: either half, used with an empty hand, sits the
 * player down on it ({@link Seat}).
 */
public class MemorialBenchBlock extends HeadstoneBlock implements Seat.Sittable {
	/** Where the sitter sits, in blocks above the bench's base. */
	public static final double SEAT = 0.475;

	public MemorialBenchBlock(Properties properties, Layout layout) {
		super(properties, layout);
	}

	@Override
	public double seatHeight(BlockState state) {
		return SEAT;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (player.isSecondaryUseActive()) {
			return InteractionResult.PASS;
		}
		if (level instanceof ServerLevel server && !Seat.sit(server, pos, state, player)) {
			return InteractionResult.PASS;
		}
		return InteractionResult.SUCCESS;
	}
}
