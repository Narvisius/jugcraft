package io.github.jimbozoomer.jugcraft.tower;

import io.github.jimbozoomer.jugcraft.drone.DepotDisplayBlockEntity;
import io.github.jimbozoomer.jugcraft.drone.DroneDepots;
import io.github.jimbozoomer.jugcraft.placement.StructureGhost;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
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
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * The Curved Video Wall (work list item 7, approved by Justin on 22 October 2026): a fine-pitch LED wall built concave
 * round an axis 14.62 blocks in front of it, 30 cabinet columns by 22 rows (17.9 blocks along the curve, 7.4 high) in a
 * black surround with a vented plinth, amber-lined trims and end posts, 9 blocks high in all; tools/curved_video_wall.py
 * describes it. The Tri-Pillar has one on each of the rotunda's cut pillar faces, round the hall's axis.
 *
 * <p>This is its master cell (the bottom of the middle column, holding the pinned depot display); its other cells are
 * {@link CurvedVideoWallPartBlock}s. It is aimed at its axis: {@link #AIM} picks one of {@link #AIMS}, the whole-block
 * vectors from the master cell to the axis cell between 14.5 and 15.4 long, so the walls of a ring of them can share one
 * axis exactly; placed by a player, the axis is the one nearest the way back to the player. {@link #BOARD} is what it
 * shows (using it with an empty hand turns to the next): OPERATIONS, FLEET BOARD or LOGISTICS, drawn with the mesh by
 * client/CurvedVideoWallRenderer from the depot it is pinned to. Each cell collides with the wall's body in it on an
 * eighth-of-a-block grid.
 */
public final class CurvedVideoWallBlock extends Block implements EntityBlock, DepotDisplayBlockEntity.Display, StructureGhost.Source {
	/** The LED surface, the surround's front and back (from the axis), half the span, the end posts' width (blocks). */
	public static final double R_S = 14.62, R_F = 14.56, R_B = 14.95, HALF = Math.toRadians(35), POST_W = 0.12;
	/** Cells high; how far (in cells, east-west and north-south) its parts can lie from the master. */
	public static final int HEIGHT = 9, SPAN = 9;
	/** Every aim, as tools/curved_video_wall.aims() lists them: {vx, vz} from the master cell to the axis cell. */
	public static final int[][] AIMS = aims();
	public static final IntegerProperty AIM = IntegerProperty.create("aim", 0, AIMS.length - 1);
	public static final EnumProperty<Board> BOARD = EnumProperty.create("board", Board.class);

	private static final List<?>[] FOOTPRINTS = new List<?>[AIMS.length];
	private static final VoxelShape[][] SHAPES = new VoxelShape[AIMS.length][(2 * SPAN + 1) * (2 * SPAN + 1)];

	/** What the wall shows. */
	public enum Board implements StringRepresentable {
		OPERATIONS, FLEET, LOGISTICS;

		@Override
		public String getSerializedName() {
			return name().toLowerCase(Locale.ROOT);
		}
	}

	public CurvedVideoWallBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(AIM, aimOf(0, -15)).setValue(BOARD, Board.OPERATIONS));
	}

	private static int[][] aims() {
		List<int[]> out = new ArrayList<>();
		for (int x = -16; x <= 16; x++) {
			for (int z = -16; z <= 16; z++) {
				int d = x * x + z * z;
				if (d >= 211 && d <= 237) {
					out.add(new int[] {x, z});
				}
			}
		}
		out.sort(Comparator.comparingDouble(v -> angle(v[0], v[1])));
		return out.toArray(int[][]::new);
	}

	private static double angle(double x, double z) {
		double a = Math.atan2(z, x);
		return a < 0 ? a + 2 * Math.PI : a;
	}

	/** The aim whose vector is (vx, vz), or -1. */
	public static int aimOf(int vx, int vz) {
		for (int i = 0; i < AIMS.length; i++) {
			if (AIMS[i][0] == vx && AIMS[i][1] == vz) {
				return i;
			}
		}
		return -1;
	}

	/** The wall's middle, seen from its axis (radians, from east towards south). */
	public static double centre(int aim) {
		return Math.atan2(-AIMS[aim][1], -AIMS[aim][0]);
	}

	/** Whether (x, z), from the axis, is in the body of a wall centred on {@code psi}: front to back, across span and posts. */
	static boolean inBody(double x, double z, double psi) {
		double r = Math.hypot(x, z);
		if (r < R_F - 0.04 || r > R_B) {
			return false;
		}
		double d = Math.atan2(z, x) - psi + Math.PI;
		d = d - 2 * Math.PI * Math.floor(d / (2 * Math.PI)) - Math.PI;
		return Math.abs(d) <= HALF + POST_W / r;
	}

	/** The body in the master's cell (dx, dz), as 64 bits: bit i + 8 j for the eighth (i east, j south). */
	public static long mask(int aim, int dx, int dz) {
		int vx = AIMS[aim][0], vz = AIMS[aim][1];
		double psi = centre(aim);
		double cx = dx - vx, cz = dz - vz;
		long m = 0;
		for (int j = 0; j < 8; j++) {
			for (int i = 0; i < 8; i++) {
				if (inBody(cx + (i + 0.5) / 8 - 0.5, cz + (j + 0.5) / 8 - 0.5, psi)) {
					m |= 1L << (i + 8 * j);
				}
			}
		}
		return m;
	}

	/** The wall's cells for {@code aim} from the master: the master first, then layer by layer, row by row. */
	@SuppressWarnings("unchecked")
	public static List<Vec3i> footprint(int aim) {
		List<Vec3i> cached = (List<Vec3i>) FOOTPRINTS[aim];
		if (cached != null) {
			return cached;
		}
		List<int[]> cells = new ArrayList<>();
		for (int dz = -SPAN; dz <= SPAN; dz++) {
			for (int dx = -SPAN; dx <= SPAN; dx++) {
				if (mask(aim, dx, dz) != 0) {
					cells.add(new int[] {dx, dz});
				}
			}
		}
		List<Vec3i> out = new ArrayList<>();
		out.add(Vec3i.ZERO);
		for (int dy = 0; dy < HEIGHT; dy++) {
			for (int[] c : cells) {
				if (dy != 0 || c[0] != 0 || c[1] != 0) {
					out.add(new Vec3i(c[0], dy, c[1]));
				}
			}
		}
		List<Vec3i> made = List.copyOf(out);
		FOOTPRINTS[aim] = made;
		return made;
	}

	/** What the body takes of the master's cell (dx, dz) at any height (empty when it misses the cell). */
	public static VoxelShape shape(int aim, int dx, int dz) {
		if (Math.abs(dx) > SPAN || Math.abs(dz) > SPAN) {
			return Shapes.empty();
		}
		int k = (dx + SPAN) * (2 * SPAN + 1) + dz + SPAN;
		VoxelShape cached = SHAPES[aim][k];
		if (cached != null) {
			return cached;
		}
		long m = mask(aim, dx, dz);
		VoxelShape shape = Shapes.empty();
		for (int j = 0; j < 8; j++) {
			for (int i = 0; i < 8; i++) {
				if ((m >>> (i + 8 * j) & 1L) != 0) {
					shape = Shapes.or(shape, Block.box(i * 2, 0, j * 2, i * 2 + 2, 16, j * 2 + 2));
				}
			}
		}
		VoxelShape made = shape.optimize();
		SHAPES[aim][k] = made;
		return made;
	}

	/** The aim for a player placing it facing {@code yaw} (degrees): the axis as nearly as can be the way back to them. */
	public static int aimFacing(float yaw) {
		double rad = Math.toRadians(yaw);
		double bx = Math.sin(rad), bz = -Math.cos(rad);
		int best = 0;
		double bestDot = -2;
		for (int i = 0; i < AIMS.length; i++) {
			double l = Math.hypot(AIMS[i][0], AIMS[i][1]);
			double dot = (AIMS[i][0] * bx + AIMS[i][1] * bz) / l;
			if (dot > bestDot) {
				bestDot = dot;
				best = i;
			}
		}
		return best;
	}

	public static BlockState partState(Vec3i offset) {
		return JugcraftTower.BLOCKS.get("curved_video_wall_part").defaultBlockState()
				.setValue(CurvedVideoWallPartBlock.OX, offset.getX() + SPAN)
				.setValue(CurvedVideoWallPartBlock.OY, offset.getY())
				.setValue(CurvedVideoWallPartBlock.OZ, offset.getZ() + SPAN);
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(AIM, BOARD);
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
	public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
		BlockState state = defaultBlockState().setValue(AIM, aimFacing(context.getRotation()));
		if (!blocked(context, state).isEmpty()) {
			if (context.getPlayer() != null && !context.getLevel().isClientSide()) {
				context.getPlayer().sendOverlayMessage(Component.translatable("message.jugcraft.curved_video_wall.blocked"));
			}
			return null;
		}
		return state;
	}

	private static List<BlockPos> blocked(BlockPlaceContext context, BlockState state) {
		Level level = context.getLevel();
		List<BlockPos> out = new ArrayList<>();
		for (Vec3i o : footprint(state.getValue(AIM))) {
			BlockPos at = context.getClickedPos().offset(o);
			if (!level.isLoaded(at) || level.isOutsideBuildHeight(at) || !level.getWorldBorder().isWithinBounds(at)
					|| context.getPlayer() != null && !level.mayInteract(context.getPlayer(), at)
					|| !level.getBlockState(at).canBeReplaced(BlockPlaceContext.at(context, at, net.minecraft.core.Direction.UP))) {
				out.add(at);
			}
		}
		return List.copyOf(out);
	}

	@Override
	public StructureGhost ghost(BlockPlaceContext context) {
		BlockState state = defaultBlockState().setValue(AIM, aimFacing(context.getRotation()));
		List<StructureGhost.Cell> cells = new ArrayList<>();
		for (Vec3i o : footprint(state.getValue(AIM))) {
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
		for (Vec3i o : footprint(state.getValue(AIM))) {
			if (!o.equals(Vec3i.ZERO)) {
				level.setBlock(pos.offset(o), partState(o), Block.UPDATE_ALL);
			}
		}
		if (level.getBlockEntity(pos) instanceof DepotDisplayBlockEntity display) {
			var terminal = DroneDepots.nearest(level, pos, linkRange());
			if (terminal != null) {
				display.pin(terminal.getBlockPos());
			}
		}
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new DepotDisplayBlockEntity(JugcraftTower.CURVED_VIDEO_WALL_ENTITY, pos, state);
	}

	/** Gone, it takes its parts with it. */
	@Override
	protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
		super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
		if (level.getBlockState(pos).is(this)) {
			return;
		}
		for (Vec3i o : footprint(state.getValue(AIM))) {
			BlockPos at = pos.offset(o);
			if (o.equals(Vec3i.ZERO) || !level.isLoaded(at)) {
				continue;
			}
			BlockState part = level.getBlockState(at);
			if (part.getBlock() instanceof CurvedVideoWallPartBlock && CurvedVideoWallPartBlock.masterPos(at, part).equals(pos)) {
				level.removeBlock(at, false);
			}
		}
	}

	/** The next board, said over the hotbar. */
	static InteractionResult nextBoard(Level level, BlockPos master, Player player) {
		BlockState state = level.getBlockState(master);
		if (!(state.getBlock() instanceof CurvedVideoWallBlock)) {
			return InteractionResult.PASS;
		}
		if (!level.isClientSide()) {
			Board next = Board.values()[(state.getValue(BOARD).ordinal() + 1) % Board.values().length];
			level.setBlock(master, state.setValue(BOARD, next), Block.UPDATE_ALL);
			player.sendOverlayMessage(Component.translatable("message.jugcraft.curved_video_wall.board",
					Component.translatable("message.jugcraft.curved_video_wall.board." + next.getSerializedName())));
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		return nextBoard(level, pos, player);
	}

	@Override
	protected RenderShape getRenderShape(BlockState state) {
		return RenderShape.INVISIBLE;
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return shape(state.getValue(AIM), 0, 0);
	}

	@Override
	protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return shape(state.getValue(AIM), 0, 0);
	}

	/** The aim turned with a structure (the aims are whole-block vectors, so every quarter turn of one is another). */
	@Override
	protected BlockState rotate(BlockState state, Rotation rotation) {
		int[] v = AIMS[state.getValue(AIM)];
		int[] t = CurvedVideoWallPartBlock.turn(v[0], v[1], rotation);
		int aim = aimOf(t[0], t[1]);
		return aim < 0 ? state : state.setValue(AIM, aim);
	}

	@Override
	protected BlockState mirror(BlockState state, Mirror mirror) {
		int[] v = AIMS[state.getValue(AIM)];
		int[] t = CurvedVideoWallPartBlock.flip(v[0], v[1], mirror);
		int aim = aimOf(t[0], t[1]);
		return aim < 0 ? state : state.setValue(AIM, aim);
	}
}
