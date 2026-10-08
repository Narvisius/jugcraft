package io.github.jimbozoomer.jugcraft.placement;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * What an MBS item would place where the player aims (Justin, October 8 2026: "I want the MBS to also show what it
 * would look like when you put it down"): every block it would set, as it would be set, and the cells where something
 * is in the way. The client draws it as a see-through copy of the real thing (client/MbsPreview.java), red when it
 * won't fit.
 *
 * @param cells   the blocks it would place, in the world
 * @param blocked the cells where something stands in the way (empty when it fits)
 */
public record StructureGhost(List<Cell> cells, List<BlockPos> blocked) {
	/** One block the structure would place. */
	public record Cell(BlockPos pos, BlockState state) {
	}

	public boolean fits() {
		return blocked.isEmpty();
	}

	/** A block whose item places a structure of several blocks (multi-block furniture, tables, machines). */
	public interface Source {
		/**
		 * What placing the block's item with {@code context} would set, worked out the way the block itself places
		 * (its getStateForPlacement and setPlacedBy), whether it fits or not; null when it would place nothing there.
		 */
		@Nullable StructureGhost ghost(BlockPlaceContext context);
	}
}
