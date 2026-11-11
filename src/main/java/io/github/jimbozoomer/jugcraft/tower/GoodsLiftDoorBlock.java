package io.github.jimbozoomer.jugcraft.tower;

import java.util.EnumMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The goods lift's landing door (work list item 20; tools/goods_lift.py): a room kit piece 3 wide and 4 high whose
 * frame is its model; client/GoodsLiftRenderer draws its leaves, its floor indicator and (at a shaft's lowest door) the
 * car live, from the door's own part models past its twelve cells. The lift is look-only: nothing ticks, and nobody
 * rides it ("NO PASSENGERS"), so its doorway is always closed to walking through, whether the leaves are drawn open
 * or shut (a shaft behind a top landing is a long way down).
 */
public class GoodsLiftDoorBlock extends RoomKitDisplayBlock {
	/** The cells the door has (3 across, 4 up); its part values past these are the renderer's models. */
	public static final int CELLS = 12;
	/** The leaves' plane (px from the front, the door facing north), closed across the opening's three rows. */
	private static final float LEAF_Z0 = 6.5F, LEAF_Z1 = 8.5F;
	private static final Map<Direction, VoxelShape> LEAVES = new EnumMap<>(Direction.class);

	static {
		LEAVES.put(Direction.NORTH, Block.box(0, 0, LEAF_Z0, 16, 16, LEAF_Z1));
		LEAVES.put(Direction.SOUTH, Block.box(0, 0, 16 - LEAF_Z1, 16, 16, 16 - LEAF_Z0));
		LEAVES.put(Direction.EAST, Block.box(16 - LEAF_Z1, 0, 0, 16 - LEAF_Z0, 16, 16));
		LEAVES.put(Direction.WEST, Block.box(LEAF_Z0, 0, 0, LEAF_Z1, 16, 16));
	}

	protected GoodsLiftDoorBlock(RoomKit.Spec spec, Properties properties) {
		super(spec, properties);
	}

	/** The door's part value of a state (0..11 its cells). */
	public int part(BlockState state) {
		Property<?> p = property("part");
		return p instanceof IntegerProperty i ? state.getValue(i) : 0;
	}

	public Direction facingOf(BlockState state) {
		return state.hasProperty(FACING_PROP) ? state.getValue(FACING_PROP) : Direction.NORTH;
	}

	@Override
	protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		VoxelShape shape = super.getCollisionShape(state, level, pos, context);
		int part = part(state);
		return part < 9 ? Shapes.or(shape, LEAVES.get(facingOf(state))) : shape;
	}
}
