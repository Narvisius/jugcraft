package io.github.jimbozoomer.jugcraft.solar;

import io.github.jimbozoomer.jugcraft.energy.EnergyConnectable;
import io.github.jimbozoomer.jugcraft.energy.EnergyNetworks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * The solar tracker: a solar panel on a motorised mount that follows the sun from east to west, catching full sun
 * all day, so it gives {@link SolarTrackerBlockEntity#OUTPUT} JE/t (two and a half solar panels) in daylight.
 * Cables take power from any side.
 */
public class SolarTrackerBlock extends SunTrackingBlock implements EnergyConnectable {
	public SolarTrackerBlock(Properties properties) {
		super(properties);
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new SolarTrackerBlockEntity(pos, state);
	}

	@Override
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		if (!(level instanceof ServerLevel)) {
			return null;
		}
		return createTickerHelper(type, JugcraftSolar.SOLAR_TRACKER_ENTITY,
				(tickLevel, pos, tickState, tracker) -> tracker.serverTick((ServerLevel) tickLevel, pos));
	}

	@Override
	protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
		EnergyNetworks.invalidate(level);
	}

	@Override
	protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
		EnergyNetworks.invalidate(level);
	}
}
