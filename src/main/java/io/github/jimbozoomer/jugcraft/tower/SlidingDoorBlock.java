package io.github.jimbozoomer.jugcraft.tower;

import io.github.jimbozoomer.jugcraft.placement.StructureGhost;
import java.util.ArrayList;
import java.util.HashMap;
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
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * An automatic sliding door (Justin, 8 November 2026; {@link SlidingDoorStyle}: the entrance's six-wide one, a room's
 * double or single in clear glass or steel). One class for every kind: the style says its size and its solid parts.
 * Anyone (or any mob that isn't hostile: the tower's crew go through) walking up to it from either side opens it; it
 * slides shut a moment after the doorway is clear ({@link SlidingDoorBlockEntity}). client/SlidingDoorRenderer draws it.
 *
 * <p>This is its master, the bottom cell at one end ({@code x 0} of its frame); {@link #FACING} is its outside (where
 * the leaves hang for a room door). The other cells are {@link SlidingDoorPartBlock}s. Each cell collides with what of
 * the door is in it: the lining or the jambs, sidelights and header always, the leaves while shut.
 */
public final class SlidingDoorBlock extends Block implements EntityBlock, StructureGhost.Source {
	public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
	public static final BooleanProperty OPEN = BlockStateProperties.OPEN;
	/** A single door's leaf slides to -x of its frame instead of +x. */
	public static final BooleanProperty MIRRORED = BooleanProperty.create("mirrored");
	private static final Map<String, VoxelShape> SHAPES = new HashMap<>();
	public final SlidingDoorStyle style;

	public SlidingDoorBlock(SlidingDoorStyle style, Properties properties) {
		super(properties);
		this.style = style;
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(OPEN, false).setValue(MIRRORED, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, OPEN, MIRRORED);
	}

	/** The cells of a door of {@code style} facing {@code facing}, as offsets from the master (its frame's x 0..w, y 0..h). */
	public static List<Vec3i> footprint(SlidingDoorStyle style, Direction facing) {
		List<Vec3i> out = new ArrayList<>();
		for (int dy = 0; dy < style.height; dy++) {
			for (int dx = 0; dx < style.width; dx++) {
				int[] t = AisleContainmentDoorBlock.turn(dx, 0, facing);
				out.add(new Vec3i(t[0], dy, t[1]));
			}
		}
		return out;
	}

	/** The frame's cell (x, y) of the offset {@code o} from the master, turned back from {@code facing}. */
	static int[] frameCell(Vec3i o, Direction facing) {
		Direction back = switch (facing) {
			case EAST -> Direction.WEST;
			case WEST -> Direction.EAST;
			default -> facing;
		};
		int[] t = AisleContainmentDoorBlock.turn(o.getX(), o.getZ(), back);
		return new int[] {t[0], o.getY()};
	}

	/** What of the door is in its frame's cell (dx, dy), turned to face {@code facing}: open or shut. */
	public static VoxelShape shape(SlidingDoorStyle style, boolean mirrored, Direction facing, int dx, int dy, boolean open) {
		String key = style + ":" + mirrored + ":" + facing + ":" + dx + ":" + dy + ":" + open;
		VoxelShape cached = SHAPES.get(key);
		if (cached != null) {
			return cached;
		}
		List<double[]> boxes = new ArrayList<>(style.fixedBoxes(mirrored));
		if (!open) {
			boxes.addAll(style.leafBoxes());
		}
		VoxelShape out = Shapes.empty();
		for (double[] b : boxes) {
			double x0 = Math.max(b[0], dx), x1 = Math.min(b[3], dx + 1), y0 = Math.max(b[1], dy), y1 = Math.min(b[4], dy + 1);
			double z0 = Math.max(b[2], 0), z1 = Math.min(b[5], 1);
			if (x1 - x0 < 1e-4 || y1 - y0 < 1e-4 || z1 - z0 < 1e-4) {
				continue;
			}
			out = Shapes.or(out, turned(new double[] {(x0 - dx) * 16, (y0 - dy) * 16, z0 * 16, (x1 - dx) * 16, (y1 - dy) * 16, z1 * 16}, facing));
		}
		out = out.optimize();
		synchronized (SHAPES) {
			SHAPES.put(key, out);
		}
		return out;
	}

	/** A box in a cell (px, the door facing north) turned about the cell's middle to face {@code facing}, as the renderer turns. */
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

	/** A part's state for the cell at {@code offset} from its master (the offset as it stands, turned). */
	public static BlockState partState(SlidingDoorStyle style, Direction facing, Vec3i offset) {
		int[] cell = frameCell(offset, facing);
		return JugcraftTower.BLOCKS.get("sliding_door_part").defaultBlockState()
				.setValue(SlidingDoorPartBlock.OX, offset.getX() + SlidingDoorPartBlock.SPAN)
				.setValue(SlidingDoorPartBlock.OY, offset.getY())
				.setValue(SlidingDoorPartBlock.OZ, offset.getZ() + SlidingDoorPartBlock.SPAN)
				.setValue(SlidingDoorPartBlock.WALK, style.walkable(cell[0], cell[1]));
	}

	@Override
	public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
		BlockState state = defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
		if (!blocked(context, state).isEmpty()) {
			if (context.getPlayer() != null && !context.getLevel().isClientSide()) {
				context.getPlayer().sendOverlayMessage(Component.translatable("message.jugcraft.sliding_door.blocked", style.width, style.height));
			}
			return null;
		}
		return state;
	}

	private List<BlockPos> blocked(BlockPlaceContext context, BlockState state) {
		Level level = context.getLevel();
		List<BlockPos> out = new ArrayList<>();
		for (Vec3i o : footprint(style, state.getValue(FACING))) {
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
		for (Vec3i o : footprint(style, state.getValue(FACING))) {
			cells.add(new StructureGhost.Cell(context.getClickedPos().offset(o), o.equals(Vec3i.ZERO) ? state : partState(style, state.getValue(FACING), o)));
		}
		return new StructureGhost(List.copyOf(cells), blocked(context, state));
	}

	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
		super.setPlacedBy(level, pos, state, placer, stack);
		if (level.isClientSide()) {
			return;
		}
		for (Vec3i o : footprint(style, state.getValue(FACING))) {
			if (!o.equals(Vec3i.ZERO)) {
				level.setBlock(pos.offset(o), partState(style, state.getValue(FACING), o), Block.UPDATE_ALL);
			}
		}
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new SlidingDoorBlockEntity(pos, state);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		if (!(level instanceof ServerLevel) || type != JugcraftTower.SLIDING_DOOR_ENTITY) {
			return null;
		}
		return (BlockEntityTicker<T>) (BlockEntityTicker<SlidingDoorBlockEntity>) (l, p, s, door) -> door.serverTick((ServerLevel) l, s);
	}

	/** Gone, it takes its parts with it. */
	@Override
	protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
		super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
		if (level.getBlockState(pos).is(this)) {
			return;
		}
		for (Vec3i o : footprint(style, state.getValue(FACING))) {
			BlockPos at = pos.offset(o);
			if (o.equals(Vec3i.ZERO) || !level.isLoaded(at)) {
				continue;
			}
			BlockState part = level.getBlockState(at);
			if (part.getBlock() instanceof SlidingDoorPartBlock && SlidingDoorPartBlock.masterPos(at, part).equals(pos)) {
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
		VoxelShape s = shape(style, state.getValue(MIRRORED), state.getValue(FACING), 0, 0, false);
		return s.isEmpty() ? Block.box(0, 0, 0, 16, 2, 16) : s;
	}

	@Override
	protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return shape(style, state.getValue(MIRRORED), state.getValue(FACING), 0, 0, state.getValue(OPEN));
	}

	/** Mobs plan their way through the doorway (it opens for them as they come). */
	@Override
	protected boolean isPathfindable(BlockState state, PathComputationType type) {
		return type == PathComputationType.LAND && style.walkable(0, 0);
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
