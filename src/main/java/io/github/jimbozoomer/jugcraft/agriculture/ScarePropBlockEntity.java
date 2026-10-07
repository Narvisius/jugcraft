package io.github.jimbozoomer.jugcraft.agriculture;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * Watches for passers-by for a {@link ScareProp}: every {@value #PERIOD} ticks it looks at the level's players (no
 * entity search) and at redstone, raises the prop when someone comes near or while it is powered, and lowers it when
 * its time is up. When it next may go off is saved, so a prop that just went off stays quiet through a reload.
 */
public class ScarePropBlockEntity extends BlockEntity {
	public static final int PERIOD = 10;

	private long downAt;
	private long readyAt;

	public ScarePropBlockEntity(BlockPos pos, BlockState state) {
		super(JugcraftAgriculture.SCARE_PROP_ENTITY, pos, state);
	}

	void serverTick(ServerLevel level) {
		if (Math.floorMod(level.getGameTime() + worldPosition.asLong(), PERIOD) == 0) {
			update(level);
		}
	}

	/** Raises or lowers the prop for who is near and for redstone; returns whether it is up. */
	public boolean update(ServerLevel level) {
		BlockState state = getBlockState();
		if (!(state.getBlock() instanceof ScareProp prop)) {
			return false;
		}
		long time = level.getGameTime();
		boolean raised = state.getValue(ScareProp.RAISED);
		boolean powered = level.hasNeighborSignal(worldPosition);
		boolean up = raised;
		if (powered) {
			downAt = time + PERIOD;
			up = true;
		} else if (!raised && time >= readyAt && someoneNear(level, worldPosition, prop.reach())) {
			downAt = time + prop.upTicks();
			readyAt = downAt + prop.cooldownTicks();
			up = true;
		} else if (raised && time >= downAt) {
			up = false;
		}
		if (up != raised) {
			level.setBlock(worldPosition, state.setValue(ScareProp.RAISED, up), Block.UPDATE_ALL);
			level.gameEvent(null, GameEvent.BLOCK_CHANGE, worldPosition);
			if (up) {
				prop.onRaise(level, worldPosition, state);
			}
		}
		setChanged();
		return up;
	}

	/** Whether a player who is neither sneaking nor a spectator is within {@code reach} blocks of the prop. */
	public static boolean someoneNear(ServerLevel level, BlockPos pos, double reach) {
		Vec3 centre = Vec3.atCenterOf(pos);
		for (ServerPlayer player : level.players()) {
			if (!player.isSpectator() && !player.isShiftKeyDown() && player.distanceToSqr(centre) <= reach * reach) {
				return true;
			}
		}
		return false;
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		downAt = input.getLongOr("down_at", 0L);
		readyAt = input.getLongOr("ready_at", 0L);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putLong("down_at", downAt);
		output.putLong("ready_at", readyAt);
	}
}
