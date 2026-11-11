package io.github.jimbozoomer.jugcraft.nuclear;

import java.util.Arrays;
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
 * A desk that joins up (RowFurnitureBlock) whose computer's screen is live (the administration building part 1): the
 * variants with a computer or laptop on them carry a DeskScreenBlockEntity; client/DeskScreenRenderer.java writes the
 * plant intranet on the screen.
 */
public class DeskComputerRowBlock extends RowFurnitureBlock implements EntityBlock {
	private final int[] screens;

	public static DeskComputerRowBlock create(Properties properties, String group, double seat, int variants, int[] screens, double[]... boxes) {
		building = variants;
		return new DeskComputerRowBlock(properties, group, seat, variants, screens, boxes);
	}

	private DeskComputerRowBlock(Properties properties, String group, double seat, int variants, int[] screens, double[]... boxes) {
		super(properties, group, seat, variants, boxes);
		this.screens = screens;
	}

	/** This state has a computer on it (its variant is one of the screen variants). */
	public boolean hasScreen(BlockState state) {
		int v = variantOf(state);
		return Arrays.stream(screens).anyMatch(s -> s == v);
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return hasScreen(state) ? new DeskScreenBlockEntity(pos, state) : null;
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
