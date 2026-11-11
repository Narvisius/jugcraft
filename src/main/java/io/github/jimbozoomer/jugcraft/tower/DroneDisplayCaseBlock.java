package io.github.jimbozoomer.jugcraft.tower;

import io.github.jimbozoomer.jugcraft.drone.DepotDisplayBlockEntity;
import io.github.jimbozoomer.jugcraft.placement.StructureGhost;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
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
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * The Drone Display Case (work list item 1, approved by Justin on 22 October 2026, without the placard): a museum
 * vitrine for a drone, 5 x 5 cells and 3.37 high, the way gallery showcases are built: a graphite plinth on a lit
 * shadow-gap kick, a frameless low-iron glass hood, an LED light hood on top with the ticker rail round it (the drone's
 * name, in four spots, running round all four sides), the drone turning on a turntable inside. tools/drone_display_case.py
 * describes it; client/DroneDisplayCaseRenderer draws it. The Tri-Pillar has two in its lobby, past the security gates.
 *
 * <p>This is its master cell (the middle one at the bottom); its other cells are {@link DroneDisplayCasePartBlock}s.
 * {@link #TIER} is the drone shown (using the case with an empty hand shows the next); {@link #FACING} is its front (a
 * player places it facing them; the back has the panels' lock). Each cell collides with what of the case is in it: the
 * plinth, the glass panes, the light hood.
 */
public final class DroneDisplayCaseBlock extends Block implements EntityBlock, StructureGhost.Source {
	/** The plinth's half width, its top; the glass's outer face (half width), thickness and height; the light hood's height. */
	public static final double HALF = 2.45, PLINTH = 0.62, GLASS = 2.40, GLASS_T = 0.025, GLASS_H = 2.45, HOOD = 0.30;
	/** The turntable's top, where the drone stands. */
	public static final double DECK = PLINTH + 0.09;
	/** Cells either side of the master, and high. */
	public static final int REACH = 2, HEIGHT = 4;
	public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
	public static final IntegerProperty TIER = IntegerProperty.create("tier", 1, 9);

	/** The case's cells from the master: the master first, then layer by layer, row by row. */
	public static final List<Vec3i> FOOTPRINT = footprint();
	private static final VoxelShape[] SHAPES = new VoxelShape[(2 * REACH + 1) * (2 * REACH + 1) * HEIGHT];

	public DroneDisplayCaseBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(TIER, 1));
	}

	private static List<Vec3i> footprint() {
		List<Vec3i> out = new ArrayList<>();
		out.add(Vec3i.ZERO);
		for (int dy = 0; dy < HEIGHT; dy++) {
			for (int dz = -REACH; dz <= REACH; dz++) {
				for (int dx = -REACH; dx <= REACH; dx++) {
					if (dx != 0 || dy != 0 || dz != 0) {
						out.add(new Vec3i(dx, dy, dz));
					}
				}
			}
		}
		return List.copyOf(out);
	}

	/** The case's solid pieces, in the master cell's frame (its middle over x, z 0.5): x0 y0 z0 x1 y1 z1. */
	private static double[][] pieces() {
		double c = 0.5, top = PLINTH + GLASS_H, in = GLASS - GLASS_T;
		return new double[][] {
				{c - HALF, 0, c - HALF, c + HALF, PLINTH, c + HALF},
				{c - HALF, top, c - HALF, c + HALF, top + HOOD, c + HALF},
				{c - GLASS, PLINTH, c - GLASS, c + GLASS, top, c - in},
				{c - GLASS, PLINTH, c + in, c + GLASS, top, c + GLASS},
				{c - GLASS, PLINTH, c - in, c - in, top, c + in},
				{c + in, PLINTH, c - in, c + GLASS, top, c + in},
		};
	}

	/** What of the case is in the master's cell (dx, dy, dz): the same whichever way it faces (it is square). */
	public static VoxelShape shape(int dx, int dy, int dz) {
		if (Math.abs(dx) > REACH || Math.abs(dz) > REACH || dy < 0 || dy >= HEIGHT) {
			return Shapes.empty();
		}
		int k = ((dy * (2 * REACH + 1)) + dz + REACH) * (2 * REACH + 1) + dx + REACH;
		VoxelShape cached = SHAPES[k];
		if (cached != null) {
			return cached;
		}
		VoxelShape shape = Shapes.empty();
		for (double[] p : pieces()) {
			double x0 = Math.max(p[0] - dx, 0), y0 = Math.max(p[1] - dy, 0), z0 = Math.max(p[2] - dz, 0);
			double x1 = Math.min(p[3] - dx, 1), y1 = Math.min(p[4] - dy, 1), z1 = Math.min(p[5] - dz, 1);
			if (x1 - x0 > 1e-6 && y1 - y0 > 1e-6 && z1 - z0 > 1e-6) {
				shape = Shapes.or(shape, Shapes.box(x0, y0, z0, x1, y1, z1));
			}
		}
		VoxelShape made = shape.optimize();
		SHAPES[k] = made;
		return made;
	}

	public static BlockState partState(Vec3i offset) {
		return JugcraftTower.BLOCKS.get("drone_display_case_part").defaultBlockState()
				.setValue(DroneDisplayCasePartBlock.OX, offset.getX() + REACH)
				.setValue(DroneDisplayCasePartBlock.OY, offset.getY())
				.setValue(DroneDisplayCasePartBlock.OZ, offset.getZ() + REACH);
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, TIER);
	}

	@Override
	public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
		BlockState state = defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
		if (!blocked(context).isEmpty()) {
			if (context.getPlayer() != null && !context.getLevel().isClientSide()) {
				context.getPlayer().sendOverlayMessage(Component.translatable("message.jugcraft.drone_display_case.blocked"));
			}
			return null;
		}
		return state;
	}

	private static List<BlockPos> blocked(BlockPlaceContext context) {
		Level level = context.getLevel();
		List<BlockPos> out = new ArrayList<>();
		for (Vec3i o : FOOTPRINT) {
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
		for (Vec3i o : FOOTPRINT) {
			cells.add(new StructureGhost.Cell(context.getClickedPos().offset(o), o.equals(Vec3i.ZERO) ? state : partState(o)));
		}
		return new StructureGhost(List.copyOf(cells), blocked(context));
	}

	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
		super.setPlacedBy(level, pos, state, placer, stack);
		if (level.isClientSide()) {
			return;
		}
		for (Vec3i o : FOOTPRINT) {
			if (!o.equals(Vec3i.ZERO)) {
				level.setBlock(pos.offset(o), partState(o), Block.UPDATE_ALL);
			}
		}
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new DepotDisplayBlockEntity(JugcraftTower.DRONE_DISPLAY_CASE_ENTITY, pos, state);
	}

	/** Gone, it takes its parts with it. */
	@Override
	protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
		super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
		if (level.getBlockState(pos).is(this)) {
			return;
		}
		for (Vec3i o : FOOTPRINT) {
			BlockPos at = pos.offset(o);
			if (o.equals(Vec3i.ZERO) || !level.isLoaded(at)) {
				continue;
			}
			BlockState part = level.getBlockState(at);
			if (part.getBlock() instanceof DroneDisplayCasePartBlock && DroneDisplayCasePartBlock.masterPos(at, part).equals(pos)) {
				level.removeBlock(at, false);
			}
		}
	}

	/** The next drone in the case, said over the hotbar. */
	static InteractionResult nextTier(Level level, BlockPos master, Player player) {
		BlockState state = level.getBlockState(master);
		if (!(state.getBlock() instanceof DroneDisplayCaseBlock)) {
			return InteractionResult.PASS;
		}
		if (!level.isClientSide()) {
			int next = state.getValue(TIER) % 9 + 1;
			level.setBlock(master, state.setValue(TIER, next), Block.UPDATE_ALL);
			var item = io.github.jimbozoomer.jugcraft.drone.DroneTier.byNumber(next)
					.map(t -> io.github.jimbozoomer.jugcraft.drone.JugcraftDrones.DRONES.get(t)).orElse(null);
			Component name = item == null ? Component.literal("T" + next) : new ItemStack(item).getHoverName();
			player.sendOverlayMessage(Component.translatable("message.jugcraft.drone_display_case.tier", name));
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		return nextTier(level, pos, player);
	}

	@Override
	protected RenderShape getRenderShape(BlockState state) {
		return RenderShape.INVISIBLE;
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return shape(0, 0, 0);
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
