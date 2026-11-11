package io.github.jimbozoomer.jugcraft.tower;

import io.github.jimbozoomer.jugcraft.drone.DepotDisplayBlockEntity;
import io.github.jimbozoomer.jugcraft.drone.DroneDepots;
import io.github.jimbozoomer.jugcraft.placement.StructureGhost;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * The Long Coffee Table (work list item 3, approved by Justin on 22 October 2026 from the mockup with the ticker runner, the
 * nuclear plant's mugs on it and "Drone Tower brought to you by Narv-Tech." running along it): the Tri-Pillar lobby
 * lounge's long coffee table, 4.0 x 1.1, its top 0.40 high. tools/long_coffee_table.py makes its mesh (a solid iroko top with a
 * smoked-glass runner down the middle over an LED dot board, graphite sled legs, an amber line under each long edge, two
 * mugs, magazines, a tablet and a model stand); client/LongCoffeeTableRenderer draws it and runs the words along the runner's
 * two lines, each reading to the sofa on its side, and puts a 1:6 model of the T1 drone on the stand.
 *
 * <p>This is its master cell; the table's middle is the corner of it that {@link #FACING} turns (the north-west corner
 * facing north), so the table covers 4 x 2 cells: the other 7 are {@link LongCoffeeTablePartBlock}s, each colliding with the
 * part of the table in its cell. Facing north (or south) the table's length runs east-west.
 */
public final class LongCoffeeTableBlock extends Block implements EntityBlock, DepotDisplayBlockEntity.Display, StructureGhost.Source {
	public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
	/** The table's half length and half depth, and its top's height. */
	public static final double HALF_LENGTH = 2.0, HALF_DEPTH = 0.55, TOP = 0.40;
	/** How far a part may be from its master (cells). */
	public static final int REACH = 2;
	private static final Map<Direction, List<Vec3i>> FOOTPRINTS = new EnumMap<>(Direction.class);
	private static final Map<Direction, VoxelShape[]> SHAPES = new EnumMap<>(Direction.class);

	static {
		for (Direction d : Direction.Plane.HORIZONTAL) {
			List<Vec3i> cells = new ArrayList<>();
			cells.add(Vec3i.ZERO);
			for (int dx = -2; dx <= 1; dx++) {
				for (int dz = -1; dz <= 0; dz++) {
					int[] t = turn(dx, dz, d);
					if (t[0] != 0 || t[1] != 0) {
						cells.add(new Vec3i(t[0], 0, t[1]));
					}
				}
			}
			FOOTPRINTS.put(d, List.copyOf(cells));
			SHAPES.put(d, new VoxelShape[(2 * REACH + 1) * (2 * REACH + 1)]);
		}
	}

	public LongCoffeeTableBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING);
	}

	/** A cell offset of the table facing north turned to face {@code facing} (about the master cell's middle). */
	static int[] turn(int dx, int dz, Direction facing) {
		return switch (facing) {
			case EAST -> new int[] {-dz, dx};
			case SOUTH -> new int[] {-dx, -dz};
			case WEST -> new int[] {dz, -dx};
			default -> new int[] {dx, dz};
		};
	}

	/** The table's middle in the master cell's frame (the north-west corner facing north, turned with it). */
	public static double[] middle(Direction facing) {
		return switch (facing) {
			case EAST -> new double[] {1, 0};
			case SOUTH -> new double[] {1, 1};
			case WEST -> new double[] {0, 1};
			default -> new double[] {0, 0};
		};
	}

	/** The master and the 7 cells round it the table covers, facing {@code facing}. */
	public static List<Vec3i> footprint(Direction facing) {
		return FOOTPRINTS.get(facing);
	}

	/** What of the table is in the master's cell (dx, dz), facing {@code facing}: its top's footprint, 0.40 high. */
	public static VoxelShape shape(int dx, int dz, Direction facing) {
		if (Math.abs(dx) > REACH || Math.abs(dz) > REACH) {
			return Shapes.empty();
		}
		VoxelShape[] cache = SHAPES.get(facing);
		int k = (dz + REACH) * (2 * REACH + 1) + dx + REACH;
		if (cache[k] != null) {
			return cache[k];
		}
		double[] c = middle(facing);
		boolean along = facing.getAxis() == Direction.Axis.Z;
		double hx = along ? HALF_LENGTH : HALF_DEPTH, hz = along ? HALF_DEPTH : HALF_LENGTH;
		double x0 = Math.max(c[0] - hx, dx) - dx, x1 = Math.min(c[0] + hx, dx + 1) - dx;
		double z0 = Math.max(c[1] - hz, dz) - dz, z1 = Math.min(c[1] + hz, dz + 1) - dz;
		VoxelShape made = x1 > x0 && z1 > z0 ? Shapes.box(x0, 0, z0, x1, TOP, z1) : Shapes.empty();
		cache[k] = made;
		return made;
	}

	public static BlockState partState(Vec3i offset) {
		return JugcraftTower.BLOCKS.get("long_coffee_table_part").defaultBlockState()
				.setValue(LongCoffeeTablePartBlock.OX, offset.getX() + REACH)
				.setValue(LongCoffeeTablePartBlock.OZ, offset.getZ() + REACH);
	}

	@Override
	public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
		BlockState state = defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
		if (!blocked(context, state).isEmpty()) {
			if (context.getPlayer() != null && !context.getLevel().isClientSide()) {
				context.getPlayer().sendOverlayMessage(Component.translatable("message.jugcraft.long_coffee_table.blocked"));
			}
			return null;
		}
		return state;
	}

	private static List<BlockPos> blocked(BlockPlaceContext context, BlockState state) {
		Level level = context.getLevel();
		List<BlockPos> out = new ArrayList<>();
		for (Vec3i o : footprint(state.getValue(FACING))) {
			BlockPos at = context.getClickedPos().offset(o);
			if (!level.isLoaded(at) || level.isOutsideBuildHeight(at) || !level.getWorldBorder().isWithinBounds(at)
					|| context.getPlayer() != null && !level.mayInteract(context.getPlayer(), at)
					|| !level.getBlockState(at).canBeReplaced(BlockPlaceContext.at(context, at, Direction.UP))) {
				out.add(at);
			}
		}
		return List.copyOf(out);
	}

	@Override
	public StructureGhost ghost(BlockPlaceContext context) {
		BlockState state = defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
		List<StructureGhost.Cell> cells = new ArrayList<>();
		for (Vec3i o : footprint(state.getValue(FACING))) {
			cells.add(new StructureGhost.Cell(context.getClickedPos().offset(o), o.equals(Vec3i.ZERO) ? state : partState(o)));
		}
		return new StructureGhost(List.copyOf(cells), blocked(context, state));
	}

	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
		super.setPlacedBy(level, pos, state, placer, stack);
		if (level.isClientSide()) {
			return;
		}
		for (Vec3i o : footprint(state.getValue(FACING))) {
			if (!o.equals(Vec3i.ZERO)) {
				level.setBlock(pos.offset(o), partState(o), Block.UPDATE_ALL);
			}
		}
		// a table by a depot runs that depot's words (a tower's own is pinned by its core)
		if (level.getBlockEntity(pos) instanceof DepotDisplayBlockEntity display) {
			var terminal = DroneDepots.nearest(level, pos, linkRange());
			if (terminal != null) {
				display.pin(terminal.getBlockPos());
			}
		}
	}

	@Override
	public boolean isAnchor(BlockState state) {
		return true;
	}

	@Override
	public int linkRange() {
		return 32;
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new DepotDisplayBlockEntity(JugcraftTower.LONG_COFFEE_TABLE_ENTITY, pos, state);
	}

	/** Gone, it takes its parts with it. */
	@Override
	protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
		super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
		if (level.getBlockState(pos).is(this)) {
			return;
		}
		for (Vec3i o : footprint(state.getValue(FACING))) {
			BlockPos at = pos.offset(o);
			if (o.equals(Vec3i.ZERO) || !level.isLoaded(at)) {
				continue;
			}
			BlockState part = level.getBlockState(at);
			if (part.getBlock() instanceof LongCoffeeTablePartBlock && LongCoffeeTablePartBlock.masterPos(at, part).equals(pos)) {
				level.removeBlock(at, false);
			}
		}
	}

	@Override
	protected RenderShape getRenderShape(BlockState state) {
		return RenderShape.INVISIBLE;
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return shape(0, 0, state.getValue(FACING));
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
