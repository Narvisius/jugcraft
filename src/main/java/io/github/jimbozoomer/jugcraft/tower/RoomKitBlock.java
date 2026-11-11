package io.github.jimbozoomer.jugcraft.tower;

import io.github.jimbozoomer.jugcraft.placement.StructureGhost;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * A room kit piece ({@link RoomKit}): its properties are the ones its blockstate uses (facing, half, part, variant, a
 * row's left and right neighbours, the test cage's sides...). A piece over several cells is placed from one item, whole
 * (facing the player), and broken as one: one piece, one item. A piece with a variant comes out as one of them at
 * random. A piece in a row (left/right) joins the same piece beside it facing the same way. Its hitbox follows its
 * model.
 */
public class RoomKitBlock extends Block implements StructureGhost.Source {
	public static final EnumProperty<Direction> FACING_PROP = BlockStateProperties.HORIZONTAL_FACING;
	private static final ThreadLocal<RoomKit.Spec> BUILDING = new ThreadLocal<>();
	private static final ThreadLocal<Boolean> CLEARING = ThreadLocal.withInitial(() -> false);
	private static final Map<String, List<Property<?>>> PROPERTIES = new HashMap<>();

	/** What using a piece with an empty hand does, by piece (its id): given by the features that put a piece to use (the
	 * manager's desk's requisitions: tower/squad/Requisitions). */
	public interface Use {
		net.minecraft.world.InteractionResult use(BlockState state, Level level, BlockPos pos, Player player);
	}

	public static final Map<String, Use> USES = new HashMap<>();

	public final RoomKit.Spec spec;
	private final List<Property<?>> props;
	private final Map<String, Property<?>> byName = new HashMap<>();
	private final @Nullable EnumProperty<Direction> facing;
	private final @Nullable BooleanProperty left, right;
	private final Map<BlockState, VoxelShape> shapes;
	private final Map<BlockState, List<RoomKit.Live>> lives = new IdentityHashMap<>();

	static RoomKitBlock create(RoomKit.Spec spec, Properties properties) {
		return create(spec, properties, RoomKitBlock::new);
	}

	/** A piece's block made with {@code make} (a subclass's constructor, crafting/AdvancedCraftingTableBlock's too) while its properties are known. */
	public static RoomKitBlock create(RoomKit.Spec spec, Properties properties, java.util.function.BiFunction<RoomKit.Spec, Properties, RoomKitBlock> make) {
		BUILDING.set(spec);
		try {
			return make.apply(spec, properties);
		} finally {
			BUILDING.remove();
		}
	}

	/** One of the piece's properties by name (null if it has none). */
	public @Nullable Property<?> property(String name) {
		return byName.get(name);
	}

	/** Where each of the piece's cells is, its anchor at {@code anchor}. */
	public List<BlockPos> cells(BlockPos anchor, BlockState state) {
		List<BlockPos> out = new ArrayList<>();
		Direction d = facing(state);
		for (RoomKit.Cell c : spec.cells()) {
			out.add(anchor.offset(offset(c, d)));
		}
		return out;
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	protected RoomKitBlock(RoomKit.Spec spec, Properties properties) {
		super(properties);
		this.spec = spec;
		this.props = properties(spec);
		for (Property<?> p : props) {
			byName.put(p.getName(), p);
		}
		facing = (EnumProperty<Direction>) byName.get("facing");
		left = byName.get("left") instanceof BooleanProperty b ? b : null;
		right = byName.get("right") instanceof BooleanProperty b ? b : null;
		BlockState state = stateDefinition.any();
		for (Property p : props) {
			state = state.setValue(p, (Comparable) first(p));
		}
		registerDefaultState(withCell(state, spec.cells().get(0)));
		Map<BlockState, VoxelShape> made = new IdentityHashMap<>();
		for (BlockState s : stateDefinition.getPossibleStates()) {
			StringBuilder key = new StringBuilder();
			props.stream().sorted(java.util.Comparator.comparing(Property::getName)).forEach(p -> {
				if (!key.isEmpty()) {
					key.append(',');
				}
				key.append(p.getName()).append('=').append(name(p, s));
			});
			Integer index = spec.states().get(key.toString());
			made.put(s, RoomKit.shape(index == null ? -1 : index));
			Integer live = spec.live().get(key.toString());
			if (live != null && live < RoomKit.LIVE.size()) {
				lives.put(s, RoomKit.LIVE.get(live));
			}
		}
		shapes = made;
	}

	@Override
	protected net.minecraft.world.InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
			net.minecraft.world.phys.BlockHitResult hit) {
		Use use = USES.get(spec.id());
		return use != null ? use.use(state, level, pos, player) : super.useWithoutItem(state, level, pos, player, hit);
	}

	/** The pictures this state draws live (none for most). */
	public List<RoomKit.Live> live(BlockState state) {
		return lives.getOrDefault(state, List.of());
	}

	private static List<Property<?>> properties(RoomKit.Spec spec) {
		return PROPERTIES.computeIfAbsent(spec.id(), id -> {
			List<Property<?>> out = new ArrayList<>();
			for (RoomKit.Prop p : spec.props()) {
				out.add(switch (p.type()) {
					case "facing" -> BlockStateProperties.HORIZONTAL_FACING;
					case "half" -> BlockStateProperties.DOUBLE_BLOCK_HALF;
					case "bool" -> BooleanProperty.create(p.name());
					case "int" -> IntegerProperty.create(p.name(), 0, p.max());
					default -> EnumProperty.create(p.name(), RoomKit.Value.class, p.values().stream().map(RoomKit.Value::of).toList());
				});
			}
			return List.copyOf(out);
		});
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	private static Comparable<?> first(Property p) {
		if (p == BlockStateProperties.HORIZONTAL_FACING) {
			return Direction.NORTH;
		}
		if (p == BlockStateProperties.DOUBLE_BLOCK_HALF) {
			return net.minecraft.world.level.block.state.properties.DoubleBlockHalf.LOWER;
		}
		if (p instanceof BooleanProperty) {
			return false;
		}
		return (Comparable) p.getPossibleValues().get(0);
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		RoomKit.Spec spec = BUILDING.get();
		if (spec != null) {
			properties(spec).forEach(builder::add);
		}
	}

	// ------------------------------------------------------------------ cells

	/** A state given a cell's part and half. */
	@SuppressWarnings({"unchecked", "rawtypes"})
	private BlockState withCell(BlockState state, RoomKit.Cell cell) {
		for (Map.Entry<String, String> e : cell.props().entrySet()) {
			Property p = byName.get(e.getKey());
			if (p != null) {
				state = state.setValue(p, (Comparable) p.getValue(e.getValue()).orElseThrow());
			}
		}
		return state;
	}

	/** The state of {@code cell} of the piece {@code state} belongs to. */
	public BlockState cellState(BlockState state, RoomKit.Cell cell) {
		return withCell(state, cell);
	}

	/** Which of the piece's cells a state is (its part and half), or -1. */
	private int cellOf(BlockState state) {
		List<RoomKit.Cell> cells = spec.cells();
		for (int i = 0; i < cells.size(); i++) {
			boolean match = true;
			for (Map.Entry<String, String> e : cells.get(i).props().entrySet()) {
				Property<?> p = byName.get(e.getKey());
				if (p != null && !name(p, state).equals(e.getValue())) {
					match = false;
					break;
				}
			}
			if (match) {
				return i;
			}
		}
		return -1;
	}

	private static <T extends Comparable<T>> String name(Property<T> p, BlockState state) {
		return p.getName(state.getValue(p));
	}

	/** The way the piece faces (north for a piece that doesn't turn). */
	public Direction facingOf(BlockState state) {
		return facing(state);
	}

	private Direction facing(BlockState state) {
		return facing == null ? Direction.NORTH : state.getValue(facing);
	}

	/** A cell's offset from the anchor for a piece facing {@code d} (the blockstate turns models clockwise). */
	static BlockPos offset(RoomKit.Cell c, Direction d) {
		return switch (d) {
			case EAST -> new BlockPos(-c.dz(), c.dy(), c.dx());
			case SOUTH -> new BlockPos(-c.dx(), c.dy(), -c.dz());
			case WEST -> new BlockPos(c.dz(), c.dy(), -c.dx());
			default -> new BlockPos(c.dx(), c.dy(), c.dz());
		};
	}

	/** Whether this state is the piece's anchor (the one cell that counts as the item). */
	public boolean isAnchor(BlockState state) {
		return cellOf(state) == 0;
	}

	/** The anchor's position for the block at {@code pos}. */
	public BlockPos anchor(BlockPos pos, BlockState state) {
		int i = cellOf(state);
		return i <= 0 ? pos : pos.subtract(offset(spec.cells().get(i), facing(state)));
	}

	// ------------------------------------------------------------------ placing and breaking

	@SuppressWarnings({"unchecked", "rawtypes"})
	@Override
	public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
		BlockState state = defaultBlockState();
		if (facing != null) {
			state = state.setValue(facing, context.getHorizontalDirection().getOpposite());
		}
		RandomSource random = RandomSource.create(context.getClickedPos().asLong());
		for (Property p : props) {
			String n = p.getName();
			if (n.equals("variant") || n.equals("colour")) {
				List<?> values = p.getPossibleValues();
				state = state.setValue(p, (Comparable) values.get(random.nextInt(values.size())));
			}
		}
		state = withCell(state, spec.cells().get(0));
		if (!blocked(context, state).isEmpty()) {
			return null;
		}
		return connect(state, context.getLevel(), context.getClickedPos());
	}

	private List<BlockPos> blocked(BlockPlaceContext context, BlockState state) {
		List<BlockPos> out = new ArrayList<>();
		Level level = context.getLevel();
		Direction d = facing(state);
		for (int i = 1; i < spec.cells().size(); i++) {
			BlockPos at = context.getClickedPos().offset(offset(spec.cells().get(i), d));
			if (level.isOutsideBuildHeight(at) || !level.getBlockState(at).canBeReplaced(context)) {
				out.add(at);
			}
		}
		return out;
	}

	@Override
	public StructureGhost ghost(BlockPlaceContext context) {
		BlockState state = getStateForPlacement(context);
		boolean fits = state != null;
		if (state == null) {
			state = withCell(facing == null ? defaultBlockState() : defaultBlockState().setValue(facing, context.getHorizontalDirection().getOpposite()),
					spec.cells().get(0));
		}
		List<StructureGhost.Cell> cells = new ArrayList<>();
		Direction d = facing(state);
		for (RoomKit.Cell c : spec.cells()) {
			cells.add(new StructureGhost.Cell(context.getClickedPos().offset(offset(c, d)), withCell(state, c)));
		}
		List<BlockPos> blocked = fits ? List.of() : blocked(context, state);
		if (!fits && blocked.isEmpty()) {
			blocked = List.of(context.getClickedPos());
		}
		return new StructureGhost(List.copyOf(cells), blocked);
	}

	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
		super.setPlacedBy(level, pos, state, placer, stack);
		if (!level.isClientSide()) {
			placeRest(level, pos, state);
		}
	}

	/** The anchor put in on its own (the tower's build lists every cell; a command, a blueprint): the rest comes too. */
	@Override
	protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
		super.onPlace(state, level, pos, oldState, movedByPiston);
		if (!level.isClientSide() && !CLEARING.get() && !oldState.is(this) && spec.multi() && isAnchor(state)) {
			placeRest(level, pos, state);
		}
		if (!level.isClientSide() && RetiredBlocks.retired(this)) {
			RetiredBlocks.queue(level, pos);                   // (a retired piece: what replaces it, at the end of the tick)
		}
	}

	private void placeRest(Level level, BlockPos anchor, BlockState state) {
		Direction d = facing(state);
		for (int i = 1; i < spec.cells().size(); i++) {
			RoomKit.Cell c = spec.cells().get(i);
			BlockPos at = anchor.offset(offset(c, d));
			BlockState want = withCell(state, c);
			BlockState there = level.getBlockState(at);
			if (!level.isOutsideBuildHeight(at) && there != want && there.canBeReplaced()) {
				level.setBlock(at, want, Block.UPDATE_ALL);
			}
		}
	}

	@Override
	public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
		if (!level.isClientSide() && spec.multi()) {
			clear(level, pos, state);
		}
		return super.playerWillDestroy(level, pos, state, player);
	}

	/** However a cell goes (an explosion, a command), the rest of the piece goes with it. */
	@Override
	protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
		super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
		if (spec.multi() && !CLEARING.get() && !level.getBlockState(pos).is(this)) {
			clear(level, pos, state);
		}
	}

	private void clear(Level level, BlockPos pos, BlockState state) {
		Direction d = facing(state);
		BlockPos anchor = anchor(pos, state);
		CLEARING.set(true);
		try {
			for (RoomKit.Cell c : spec.cells()) {
				BlockPos at = anchor.offset(offset(c, d));
				BlockState there = level.getBlockState(at);
				if (!at.equals(pos) && there.is(this) && facing(there) == d && anchor(at, there).equals(anchor)) {
					level.setBlock(at, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
				}
			}
		} finally {
			CLEARING.set(false);
		}
	}

	// ------------------------------------------------------------------ rows

	/**
	 * A piece in a row joins the same piece beside it facing the same way (its left and right); a tier riser shows its
	 * nosing and step light on each side it steps down (nothing beside it there: edge_north...).
	 */
	private BlockState connect(BlockState state, LevelReader level, BlockPos pos) {
		for (Direction d : Direction.Plane.HORIZONTAL) {
			if (byName.get("edge_" + d.getSerializedName()) instanceof BooleanProperty edge) {
				state = state.setValue(edge, level.getBlockState(pos.relative(d)).isAir());
			}
		}
		if (left == null || right == null || facing == null) {
			return state;
		}
		Direction d = state.getValue(facing);
		return state.setValue(left, joins(level.getBlockState(pos.relative(d.getCounterClockWise())), d))
				.setValue(right, joins(level.getBlockState(pos.relative(d.getClockWise())), d));
	}

	private boolean joins(BlockState other, Direction d) {
		return other.is(this) && other.getValue(facing) == d;
	}

	@Override
	protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
			BlockPos neighborPos, BlockState neighborState, RandomSource random) {
		if ((left != null || byName.containsKey("edge_north")) && direction.getAxis().isHorizontal()) {
			return connect(state, level, pos);
		}
		return state;
	}

	// ------------------------------------------------------------------ shape, turning

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		VoxelShape shape = shapes.get(state);
		return shape == null ? RoomKit.shape(-1) : shape;
	}

	@Override
	protected BlockState rotate(BlockState state, Rotation rotation) {
		return facing == null ? state : state.setValue(facing, rotation.rotate(state.getValue(facing)));
	}

	@Override
	protected BlockState mirror(BlockState state, Mirror mirror) {
		return facing == null ? state : state.rotate(mirror.getRotation(state.getValue(facing)));
	}
}
