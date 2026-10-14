package io.github.jimbozoomer.jugcraft.nuclear;

import java.util.EnumMap;
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
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Framed Glass: an aluminium storefront partition (the plants' lobbies, the shift supervisor's office) that meets the
 * glass doors flush. The panel is 3 px deep like a door's leaf and sits at the edge of its block nearest whoever placed
 * it, as a door does; placed beside a door (or over or under other framed glass) it takes that plane, so glass and
 * doors make one face.
 *
 * <p>The glass runs on unbroken (Justin: "as continuous as possible, just glass"): framed glass facing the same way
 * beside, above or below carries the glass straight on with no frame between. The frame shows only where the glass
 * needs support: a jamb at an end against anything else (a wall, a door, nothing), a corner post where the run turns
 * inside this block (the block in front is framed glass at right angles; a stub of glass runs along the side to it),
 * a sill along the bottom and a head along the top, and round a door set in it: the jambs either side up to the door's
 * head, the head over it ({@link Bottom#DOOR}) and its corners in the blocks beside ({@link End#HEAD}).
 * Models: tools/framed_glass.py, which works out the same states for the pre-made plants.
 */
public class FramedGlassBlock extends Block {
	public enum End implements StringRepresentable {
		/** The glass carries on into the next block. */
		JOIN("join"),
		/** A 2 px jamb: the glass ends here. */
		JAMB("jamb"),
		/** A 3 px corner post and a stub of glass back along the side to the next run. */
		CORNER("corner"),
		/** The glass carries on, but the next block is over a door: the corner of the door's head frame is here. */
		HEAD("head");

		private final String name;

		End(String name) {
			this.name = name;
		}

		@Override
		public String getSerializedName() {
			return name;
		}
	}

	public enum Bottom implements StringRepresentable {
		/** A sill under the glass. */
		SILL("sill"),
		/** The glass carries on into framed glass below. */
		GLASS("glass"),
		/** Over a door: the door's head (drawn as a sill; the blocks beside finish its corners). */
		DOOR("door");

		private final String name;

		Bottom(String name) {
			this.name = name;
		}

		@Override
		public String getSerializedName() {
			return name;
		}
	}

	public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
	public static final EnumProperty<End> LEFT = EnumProperty.create("left", End.class);
	public static final EnumProperty<End> RIGHT = EnumProperty.create("right", End.class);
	public static final BooleanProperty UP = BooleanProperty.create("up");
	public static final EnumProperty<Bottom> BOTTOM = EnumProperty.create("bottom", Bottom.class);

	/** Facing north: the panel along the south edge (z 13..16), as a north-facing door's leaf; the corner stubs. */
	private static final Map<Direction, VoxelShape> PANEL = Shapes.rotateHorizontal(Block.box(0, 0, 13, 16, 16, 16));
	private static final Map<Direction, VoxelShape> LEFT_STUB = Shapes.rotateHorizontal(Block.box(0, 0, 0, 3, 16, 13));
	private static final Map<Direction, VoxelShape> RIGHT_STUB = Shapes.rotateHorizontal(Block.box(13, 0, 0, 16, 16, 13));
	private static final Map<Direction, VoxelShape[]> SHAPES = new EnumMap<>(Direction.class);

	static {
		for (Direction facing : Direction.Plane.HORIZONTAL) {
			VoxelShape[] shapes = new VoxelShape[4];
			for (int i = 0; i < 4; i++) {
				VoxelShape shape = PANEL.get(facing);
				if ((i & 1) != 0) {
					shape = Shapes.or(shape, LEFT_STUB.get(facing));
				}
				if ((i & 2) != 0) {
					shape = Shapes.or(shape, RIGHT_STUB.get(facing));
				}
				shapes[i] = shape;
			}
			SHAPES.put(facing, shapes);
		}
	}

	public FramedGlassBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(LEFT, End.JAMB)
				.setValue(RIGHT, End.JAMB).setValue(UP, false).setValue(BOTTOM, Bottom.SILL));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, LEFT, RIGHT, UP, BOTTOM);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		LevelReader level = context.getLevel();
		BlockPos pos = context.getClickedPos();
		Direction facing = context.getHorizontalDirection();
		BlockState below = level.getBlockState(pos.below());
		BlockState above = level.getBlockState(pos.above());
		if (below.is(this)) {
			facing = below.getValue(FACING);
		} else if (above.is(this)) {
			facing = above.getValue(FACING);
		} else {
			// Beside a door or framed glass in the same line: take its plane, so the two meet flush.
			for (Direction side : new Direction[] {facing.getCounterClockWise(), facing.getClockWise()}) {
				BlockState next = level.getBlockState(pos.relative(side));
				if (next.getBlock() instanceof DoorBlock && next.getValue(DoorBlock.FACING).getAxis() == facing.getAxis()) {
					facing = next.getValue(DoorBlock.FACING);
					break;
				}
				if (next.is(this) && next.getValue(FACING).getAxis() == facing.getAxis()) {
					facing = next.getValue(FACING);
					break;
				}
			}
		}
		return connected(defaultBlockState().setValue(FACING, facing), level, pos);
	}

	@Override
	protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
			BlockPos neighbourPos, BlockState neighbour, RandomSource random) {
		return connected(state, level, pos);
	}

	/** The state's ends, top and bottom from its neighbours (tools/framed_glass.py connections). */
	public BlockState connected(BlockState state, BlockGetter level, BlockPos pos) {
		Direction facing = state.getValue(FACING);
		BlockState below = level.getBlockState(pos.below());
		BlockState above = level.getBlockState(pos.above());
		Bottom bottom = below.is(this) && below.getValue(FACING) == facing ? Bottom.GLASS
				: below.getBlock() instanceof DoorBlock ? Bottom.DOOR : Bottom.SILL;
		BlockState front = level.getBlockState(pos.relative(facing));
		return state.setValue(BOTTOM, bottom)
				.setValue(UP, above.is(this) && above.getValue(FACING) == facing)
				.setValue(LEFT, end(level, pos, facing, front, facing.getCounterClockWise(), facing.getClockWise(), bottom))
				.setValue(RIGHT, end(level, pos, facing, front, facing.getClockWise(), facing.getCounterClockWise(), bottom));
	}

	private End end(BlockGetter level, BlockPos pos, Direction facing, BlockState front, Direction side, Direction cornerWhen, Bottom bottom) {
		if (front.is(this) && front.getValue(FACING) == cornerWhen) {
			return End.CORNER;
		}
		BlockState next = level.getBlockState(pos.relative(side));
		if (!next.is(this)) {
			return End.JAMB;
		}
		Direction nextFacing = next.getValue(FACING);
		if (nextFacing == facing) {
			return next.getValue(BOTTOM) == Bottom.DOOR && bottom == Bottom.GLASS ? End.HEAD : End.JOIN;
		}
		// The next block turns a corner towards this one: its stub carries the glass on into this block.
		if (nextFacing == side.getOpposite() && nextFacing.getAxis() != facing.getAxis()) {
			return End.JOIN;
		}
		return End.JAMB;
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		int stubs = (state.getValue(LEFT) == End.CORNER ? 1 : 0) | (state.getValue(RIGHT) == End.CORNER ? 2 : 0);
		return SHAPES.get(state.getValue(FACING))[stubs];
	}

	@Override
	protected float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
		return 1.0F;
	}

	@Override
	protected boolean propagatesSkylightDown(BlockState state) {
		return true;
	}

	@Override
	protected BlockState rotate(BlockState state, Rotation rotation) {
		return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
	}

	@Override
	protected BlockState mirror(BlockState state, Mirror mirror) {
		Direction facing = state.getValue(FACING);
		Direction mirrored = mirror.mirror(facing);
		if (mirrored == facing && mirror == Mirror.NONE) {
			return state;
		}
		// A mirror turns the panel round or reflects it along its run: either way its left and right swap.
		return state.setValue(FACING, mirrored).setValue(LEFT, state.getValue(RIGHT)).setValue(RIGHT, state.getValue(LEFT));
	}
}
