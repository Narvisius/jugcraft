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
 * The unit performance screen (the administration building part 2: the board room and the plant manager's office): a
 * wall screen three blocks across and two high that keeps the unit's generation (PerfScreenBlockEntity, in its bottom
 * middle block); client/PerfScreenRenderer.java draws it across the whole screen.
 */
public class PerfScreenBlock extends MultiFurnitureBlock implements EntityBlock {
	public PerfScreenBlock(Properties properties) {
		super(properties, 3, 2, true);
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return state.getValue(partProperty()) == origin() ? new PerfScreenBlockEntity(pos, state) : null;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		if (!(level instanceof ServerLevel) || type != PlantInterior.PERF_ENTITY) {
			return null;
		}
		return (BlockEntityTicker<T>) (BlockEntityTicker<PerfScreenBlockEntity>) (l, pos, s, screen) -> screen.serverTick((ServerLevel) l);
	}
}
