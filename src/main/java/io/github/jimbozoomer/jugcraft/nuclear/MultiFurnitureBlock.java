package io.github.jimbozoomer.jugcraft.nuclear;

import java.util.EnumMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Furniture bigger than a block, placed and broken as one: a wall screen several blocks across and high (vertical),
 * or a long oval table (flat). PART is the block's place: column c (to the user's right) and row r (up the wall, or
 * away from the user across the table), part = r * width + c. The block you place it on is the bottom middle (wall
 * screens) or the middle (tables); it needs all its cells free.
 */
public class MultiFurnitureBlock extends HorizontalDirectionalBlock {
	private static final Map<Integer, IntegerProperty> PROPERTIES = new java.util.HashMap<>();
	private static int building, buildingVariants;
	protected final int width, rows, variants;
	private final boolean boxy, low;
	private final double topHeight;
	private final IntegerProperty variant;
	protected final boolean vertical;
	private final IntegerProperty part;
	private final Map<Direction, VoxelShape[]> shapes = new EnumMap<>(Direction.class);

	static IntegerProperty property(int count) {
		return PROPERTIES.computeIfAbsent(count, n -> IntegerProperty.create("part", 0, n - 1));
	}

	protected static Properties prepare(Properties properties, int width, int rows, int variants) {
		building = width * rows;
		buildingVariants = variants;
		return properties;
	}

	public MultiFurnitureBlock(Properties properties, int width, int rows, boolean vertical) {
		this(properties, width, rows, vertical, 1);
	}

	/** variants: more than one picks one at random when placed (big framed pictures), kept by every part. */
	public MultiFurnitureBlock(Properties properties, int width, int rows, boolean vertical, int variants) {
		this(properties, width, rows, vertical, variants, false);
	}

	/** boxy: a solid machine standing on the floor (the copier) rather than a screen on a wall. */
	public MultiFurnitureBlock(Properties properties, int width, int rows, boolean vertical, int variants, boolean boxy) {
		this(properties, width, rows, vertical, variants, boxy, false);
	}

	/** low: a coffee table (its oval top at knee height) rather than a meeting table. */
	public MultiFurnitureBlock(Properties properties, int width, int rows, boolean vertical, int variants, boolean boxy, boolean low) {
		this(properties, width, rows, vertical, variants, boxy, low, 7);
	}

	/** topHeight: how high a boxy piece stands into its upper row, in pixels (a copier's top, a panel's full height). */
	public MultiFurnitureBlock(Properties properties, int width, int rows, boolean vertical, int variants, boolean boxy, boolean low,
			double topHeight) {
		super(prepare(properties, width, rows, variants));
		this.topHeight = topHeight;
		this.boxy = boxy;
		this.low = low;
		this.width = width;
		this.rows = rows;
		this.vertical = vertical;
		this.variants = variants;
		this.part = property(width * rows);
		this.variant = variants > 1 ? VariantFurnitureBlock.property(variants) : null;
		BlockState state = stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(part, origin());
		registerDefaultState(variant == null ? state : state.setValue(variant, 0));
		for (Direction facing : Direction.Plane.HORIZONTAL) {
			VoxelShape[] byPart = new VoxelShape[width * rows];
			for (int p = 0; p < byPart.length; p++) {
				byPart[p] = localShape(p % width, p / width, facing);
			}
			shapes.put(facing, byPart);
		}
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, property(building));
		if (buildingVariants > 1) {
			builder.add(VariantFurnitureBlock.property(buildingVariants));
		}
	}

	public IntegerProperty partProperty() {
		return part;
	}

	/** The part placed where you click. */
	public int origin() {
		return vertical ? width / 2 : (rows / 2) * width + width / 2;
	}

	/** Where part p stands relative to the origin, for a piece facing {@code facing}. */
	public BlockPos offset(Direction facing, int p) {
		int dc = p % width - origin() % width, dr = p / width - origin() / width;
		Direction right = facing.getClockWise();
		BlockPos at = BlockPos.ZERO.relative(right, dc);
		return vertical ? at.above(dr) : at.relative(facing.getOpposite(), dr);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		Direction facing = context.getHorizontalDirection();
		BlockPos at = context.getClickedPos();
		for (int p = 0; p < width * rows; p++) {
			if (p != origin() && !context.getLevel().getBlockState(at.offset(offset(facing, p))).canBeReplaced(context)) {
				return null;
			}
		}
		BlockState state = defaultBlockState().setValue(FACING, facing);
		return variant == null ? state : state.setValue(variant, context.getLevel().getRandom().nextInt(variants));
	}

	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
		if (!level.isClientSide()) {
			Direction facing = state.getValue(FACING);
			for (int p = 0; p < width * rows; p++) {
				if (p != origin()) {
					level.setBlock(pos.offset(offset(facing, p)), state.setValue(part, p), Block.UPDATE_ALL);
				}
			}
		}
	}

	@Override
	public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
		if (!level.isClientSide()) {
			Direction facing = state.getValue(FACING);
			BlockPos origin = pos.subtract(offset(facing, state.getValue(part)));
			for (int p = 0; p < width * rows; p++) {
				BlockPos at = origin.offset(offset(facing, p));
				if (!at.equals(pos) && level.getBlockState(at).is(this)) {
					level.setBlock(at, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
				}
			}
		}
		return super.playerWillDestroy(level, pos, state, player);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return shapes.get(state.getValue(FACING))[state.getValue(part)];
	}

	/**
	 * The part's hitbox. A wall screen is a thin panel on the wall side of its block; a table is its slice of an
	 * oval top over the whole piece (radius just inside its edges) and the pedestals under the middle row.
	 */
	private VoxelShape localShape(int c, int r, Direction facing) {
		if (boxy) {
			return turned(new double[] {0, 0, 2, 16, r == 0 ? 16 : topHeight, 15}, facing);
		}
		if (vertical) {
			return turned(new double[] {0, 0, 0, 16, 16, 1.6}, facing);
		}
		double a = width * 8 - 1, b = rows * 8 - 1, cx = width * 8, cz = rows * 8;
		VoxelShape shape = Shapes.empty();
		for (int z = 0; z < 16; z += 2) {
			double gz = r * 16 + z + 1 - cz;
			double half = gz * gz >= b * b ? 0 : a * Math.sqrt(1 - gz * gz / (b * b));
			double x0 = Math.max(0, cx - half - c * 16), x1 = Math.min(16, cx + half - c * 16);
			if (x1 - x0 > 0.5) {
				shape = Shapes.or(shape, turned(low ? new double[] {x0, 0, z, x1, 7.5, z + 2} : new double[] {x0, 12, z, x1, 13, z + 2}, facing));
			}
		}
		if (!low && r == rows / 2 && c > 0 && c < width - 1 && c % 2 == 1) {
			shape = Shapes.or(shape, turned(new double[] {5, 0, 5, 11, 12, 11}, facing));
		}
		return shape.isEmpty() ? Shapes.empty() : shape.optimize();
	}

	/** A box given for a piece facing north, turned to {@code facing} (clockwise, as the blockstate turns the model). */
	static VoxelShape turned(double[] b, Direction facing) {
		double x0 = b[0], z0 = b[2], x1 = b[3], z1 = b[5];
		int turns = switch (facing) {
			case EAST -> 1;
			case SOUTH -> 2;
			case WEST -> 3;
			default -> 0;
		};
		for (int i = 0; i < turns; i++) {
			double nx0 = 16 - z1, nx1 = 16 - z0, nz0 = x0, nz1 = x1;
			x0 = nx0;
			x1 = nx1;
			z0 = nz0;
			z1 = nz1;
		}
		return Block.box(x0, b[1], z0, x1, b[4], z1);
	}
}
