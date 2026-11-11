package io.github.jimbozoomer.jugcraft.tower;

import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;

/**
 * A Faced Wall (Justin's first-floor look-over, 10 November 2026; tools/faced_walls.py): a room's painted wall with the
 * hall's finish on the one face that looks onto the hall. {@link #FACING} is that face, in the block's material at its
 * {@link TowerPanelBlock#PHASE} (drawn as the pillars beside it are, the two-metre sheet running on from block to
 * block); the face opposite is the room's ({@link #INSIDE}: painted, or the plant restroom's wall tile or its cap
 * course, so a tiled room keeps its tiles) and the rest is painted. The rooms' walls are one block thick, so without it
 * a room standing in the tower's hall showed the hall its paint. An opaque full cube like the walls it stands in for.
 */
public final class FacedWallBlock extends Block {
	/** What the face opposite {@link #FACING} shows. */
	public enum Inside implements StringRepresentable {
		PAINTED("painted"), TILE("tile"), TILE_CAP("tile_cap");

		private final String name;

		Inside(String name) {
			this.name = name;
		}

		@Override
		public String getSerializedName() {
			return name;
		}
	}

	public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
	public static final EnumProperty<Inside> INSIDE = EnumProperty.create("inside", Inside.class);

	public FacedWallBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(INSIDE, Inside.PAINTED)
				.setValue(TowerPanelBlock.PHASE, 0));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, INSIDE, TowerPanelBlock.PHASE);
	}

	/** Its faced side toward whoever places it (they stand in the hall). */
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return TowerPanelBlock.at(defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite()),
				context.getClickedPos());
	}

	@Override
	protected BlockState rotate(BlockState state, Rotation rotation) {
		return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
	}

	@Override
	protected BlockState mirror(BlockState state, Mirror mirror) {
		return state.rotate(mirror.getRotation(state.getValue(FACING)));
	}
}
