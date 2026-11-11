package io.github.jimbozoomer.jugcraft.tower;

import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;

/**
 * A Neon Line (work list item 11, made real 3D neon after Justin's walk through: tools/neon_3d.py): the wall's own
 * block (its material at its {@link TowerPanelBlock#PHASE}, drawn as its neighbours are) with a lined groove round each
 * side open to the air and a round glass neon tube in it. Each side is {@link Side#OPEN} (the tube runs along it),
 * {@link Side#LINE} (another line beside it: the tube runs on into it) or {@link Side#WALL} (a plain opaque block: the
 * tube ends in a cap there); at a corner whose two sides are open the tube bends round, unless another line touches
 * this one only at that corner ({@link #NE} ...: a chamfer one block thick), where a 45 degree piece takes it on to that
 * line instead. It works these out from its neighbours (as tools/neon_lines.shape writes the tower's); it lets its
 * neighbours draw their faces against it (no occlusion), so a groove ending at a plain block is closed by its face.
 */
public final class NeonLineBlock extends Block {
	public enum Side implements StringRepresentable {
		OPEN("open"), LINE("line"), WALL("wall");

		private final String name;

		Side(String name) {
			this.name = name;
		}

		@Override
		public String getSerializedName() {
			return name;
		}
	}

	public static final EnumProperty<Side> NORTH = EnumProperty.create("north", Side.class);
	public static final EnumProperty<Side> EAST = EnumProperty.create("east", Side.class);
	public static final EnumProperty<Side> SOUTH = EnumProperty.create("south", Side.class);
	public static final EnumProperty<Side> WEST = EnumProperty.create("west", Side.class);
	public static final BooleanProperty NE = BooleanProperty.create("ne");
	public static final BooleanProperty SE = BooleanProperty.create("se");
	public static final BooleanProperty SW = BooleanProperty.create("sw");
	public static final BooleanProperty NW = BooleanProperty.create("nw");
	private static final Map<Direction, EnumProperty<Side>> SIDES = Map.of(Direction.NORTH, NORTH, Direction.EAST, EAST,
			Direction.SOUTH, SOUTH, Direction.WEST, WEST);

	public NeonLineBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(TowerPanelBlock.PHASE, 0).setValue(NORTH, Side.WALL).setValue(EAST, Side.WALL)
				.setValue(SOUTH, Side.WALL).setValue(WEST, Side.WALL).setValue(NE, false).setValue(SE, false).setValue(SW, false)
				.setValue(NW, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(TowerPanelBlock.PHASE, NORTH, EAST, SOUTH, WEST, NE, SE, SW, NW);
	}

	public static EnumProperty<Side> side(Direction direction) {
		return SIDES.get(direction);
	}

	/** What a side meets: another line (a faced wall's too: {@link FacedLineBlock}), a wall (an opaque full block; or
	 * framed glass standing against it: a line stops at a pane, it never turns onto the reveal beside one, where the
	 * room behind the glass would see it), or anything else (open). */
	public static Side sideOf(BlockState neighbour) {
		if (neighbour.getBlock() instanceof NeonLineBlock || neighbour.getBlock() instanceof FacedLineBlock) {
			return Side.LINE;
		}
		return neighbour.isSolidRender() || neighbour.getBlock() instanceof io.github.jimbozoomer.jugcraft.nuclear.FramedGlassBlock
				? Side.WALL : Side.OPEN;
	}

	/** The state's sides and joins from the blocks round {@code pos} (its phase kept). */
	public static BlockState shaped(BlockState state, BlockGetter level, BlockPos pos) {
		for (Direction d : Direction.Plane.HORIZONTAL) {
			state = state.setValue(SIDES.get(d), sideOf(level.getBlockState(pos.relative(d))));
		}
		state = state.setValue(NE, joined(state, level, pos, Direction.NORTH, Direction.EAST));
		state = state.setValue(SE, joined(state, level, pos, Direction.EAST, Direction.SOUTH));
		state = state.setValue(SW, joined(state, level, pos, Direction.SOUTH, Direction.WEST));
		return state.setValue(NW, joined(state, level, pos, Direction.WEST, Direction.NORTH));
	}

	private static boolean joined(BlockState state, BlockGetter level, BlockPos pos, Direction a, Direction b) {
		return state.getValue(SIDES.get(a)) == Side.OPEN && state.getValue(SIDES.get(b)) == Side.OPEN
				&& level.getBlockState(pos.relative(a).relative(b)).getBlock() instanceof NeonLineBlock;
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return shaped(TowerPanelBlock.at(defaultBlockState(), context.getClickedPos()), context.getLevel(), context.getClickedPos());
	}

	@Override
	protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
			BlockPos neighborPos, BlockState neighborState, RandomSource random) {
		return direction.getAxis().isHorizontal() ? shaped(state, level, pos) : state;
	}

	@Override
	protected BlockState rotate(BlockState state, Rotation rotation) {
		BlockState out = state;
		for (Direction d : Direction.Plane.HORIZONTAL) {
			out = out.setValue(SIDES.get(rotation.rotate(d)), state.getValue(SIDES.get(d)));
		}
		boolean[] corners = {state.getValue(NE), state.getValue(SE), state.getValue(SW), state.getValue(NW)};
		int k = switch (rotation) {
			case CLOCKWISE_90 -> 1;
			case CLOCKWISE_180 -> 2;
			case COUNTERCLOCKWISE_90 -> 3;
			default -> 0;
		};
		BooleanProperty[] order = {NE, SE, SW, NW};
		for (int i = 0; i < 4; i++) {
			out = out.setValue(order[(i + k) % 4], corners[i]);
		}
		return out;
	}

	@Override
	protected BlockState mirror(BlockState state, Mirror mirror) {
		if (mirror == Mirror.NONE) {
			return state;
		}
		BlockState out = state;
		for (Direction d : Direction.Plane.HORIZONTAL) {
			out = out.setValue(SIDES.get(mirror.mirror(d)), state.getValue(SIDES.get(d)));
		}
		if (mirror == Mirror.LEFT_RIGHT) {         // north and south swap
			return out.setValue(NE, state.getValue(SE)).setValue(SE, state.getValue(NE)).setValue(SW, state.getValue(NW))
					.setValue(NW, state.getValue(SW));
		}
		return out.setValue(NE, state.getValue(NW)).setValue(NW, state.getValue(NE)).setValue(SE, state.getValue(SW))
				.setValue(SW, state.getValue(SE));
	}
}
