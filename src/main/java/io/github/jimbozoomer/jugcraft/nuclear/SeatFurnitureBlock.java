package io.github.jimbozoomer.jugcraft.nuclear;

import io.github.jimbozoomer.jugcraft.tower.FurnitureBlock;
import io.github.jimbozoomer.jugcraft.tower.SeatEntity;
import io.github.jimbozoomer.jugcraft.tower.Sittable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** A single seat (a cafe chair, a visitor's chair): use it to sit down, facing the way it faces. */
public class SeatFurnitureBlock extends FurnitureBlock implements Sittable {
	private final double seat;

	public SeatFurnitureBlock(Properties properties, double seat, double[]... boxes) {
		super(properties, boxes);
		this.seat = seat;
	}

	@Override
	public double seatHeight() {
		return seat;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!level.isClientSide()) {
			SeatEntity.sit(level, pos, player, state.getValue(FACING), seat);
		}
		return InteractionResult.SUCCESS;
	}
}
