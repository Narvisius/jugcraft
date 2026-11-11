package io.github.jimbozoomer.jugcraft.nuclear;

import io.github.jimbozoomer.jugcraft.tower.FurnitureBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/** The one-block office desk with its computer, the screen live as on the desks that join up (DeskComputerRowBlock). */
public class DeskComputerBlock extends FurnitureBlock implements EntityBlock {
	public DeskComputerBlock(Properties properties, double[]... boxes) {
		super(properties, boxes);
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new DeskScreenBlockEntity(pos, state);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		if (!(level instanceof ServerLevel) || type != PlantInterior.DESK_ENTITY) {
			return null;
		}
		return (BlockEntityTicker<T>) (BlockEntityTicker<DeskScreenBlockEntity>) (l, pos, s, screen) -> screen.serverTick((ServerLevel) l);
	}
}
