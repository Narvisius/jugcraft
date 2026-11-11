package io.github.jimbozoomer.jugcraft.tower;

import io.github.jimbozoomer.jugcraft.drone.DepotDisplayBlockEntity;
import io.github.jimbozoomer.jugcraft.drone.DroneDepots;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * A room kit piece's live pictures (tower/RoomKitDisplayBlock; client/RoomKitDisplayRenderer draws them): the depot it
 * shows (pinned to the tower's terminal by its build, or to the nearest one when it has none), and what it counts (the
 * bottle filler's bottles).
 */
public class RoomKitBlockEntity extends DepotDisplayBlockEntity {
	/** How far a piece with no terminal looks for one (blocks), as the wall screens. */
	public static final int REACH = 64;
	private int count;

	public RoomKitBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	/** What it has counted (the bottles a bottle filler has filled). */
	public int count() {
		return count;
	}

	public void addCount(int n) {
		count += n;
		setChanged();
		if (level != null) {
			level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
		}
	}

	/** A piece with no depot takes the nearest one in reach (every two seconds, staggered). */
	public static void serverTick(Level level, BlockPos pos, BlockState state, RoomKitBlockEntity entity) {
		if (entity.terminal() == null && (level.getGameTime() + pos.hashCode()) % RELINK_INTERVAL == 0) {
			var nearest = DroneDepots.nearest(level, pos, REACH);
			if (nearest != null) {
				entity.pin(nearest.getBlockPos());
			}
		}
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		count = input.getIntOr("count", 0);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		if (count != 0) {
			output.putInt("count", count);
		}
	}
}
