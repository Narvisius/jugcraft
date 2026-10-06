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
 * The plant status screen: a wall screen three blocks across and two high that shows the nearest reactor live (running
 * or shut down, power, rods, core temperature, the generator's output, fuel, alarms, the temperature trend). The
 * bottom middle block holds the readings; client/StatusScreenRenderer.java draws them across the whole screen.
 */
public class StatusScreenBlock extends MultiFurnitureBlock implements EntityBlock {
	public StatusScreenBlock(Properties properties) {
		super(properties, 3, 2, true);
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return state.getValue(partProperty()) == origin() ? new StatusScreenBlockEntity(pos, state) : null;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		if (!(level instanceof ServerLevel) || type != PlantInterior.STATUS_ENTITY) {
			return null;
		}
		return (BlockEntityTicker<T>) (BlockEntityTicker<StatusScreenBlockEntity>) (l, pos, s, screen) -> screen.serverTick((ServerLevel) l);
	}
}
