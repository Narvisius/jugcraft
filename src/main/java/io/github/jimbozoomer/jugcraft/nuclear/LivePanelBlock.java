package io.github.jimbozoomer.jugcraft.nuclear;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * An emergency diesel generator room panel whose meters, lamps and displays follow its generator (round 14): the
 * engine control panel, the generator control panel, the voltage regulator, the protection relays, the load sequencer
 * and the battery charger. Placed like any panel two blocks high (MultiFurnitureBlock); its placing block holds the
 * link to its generator (LivePanelBlockEntity), and client/LivePanelRenderer.java draws what moves (laid out by
 * tools/plant_panels.py).
 */
public class LivePanelBlock extends MultiFurnitureBlock implements EntityBlock {
	public LivePanelBlock(Properties properties, int width, int rows) {
		super(properties, width, rows, true, 1, true, false, 15.6);
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return state.getValue(partProperty()) == origin() ? new LivePanelBlockEntity(pos, state) : null;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		if (!(level instanceof ServerLevel) || type != PlantInterior.LIVE_PANEL_ENTITY) {
			return null;
		}
		return (BlockEntityTicker<T>) (BlockEntityTicker<LivePanelBlockEntity>) (l, pos, s, panel) -> panel.serverTick((ServerLevel) l);
	}
}
