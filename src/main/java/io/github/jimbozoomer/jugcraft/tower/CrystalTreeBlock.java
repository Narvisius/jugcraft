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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * The Crystal Tree (work list item 2, approved by Justin on 22 October 2026 from the third mockup, with a slight glow on
 * the crystals and "a different variant for each tree you put down"): an alien tree of our own design in a round planter
 * with a timber bench round it, about 9 blocks across. tools/crystal_tree.py makes the planter and the bench;
 * client/CrystalTreeGeometry grows the tree itself from {@link #seed} (the master's position), so every tree placed is
 * different; client/CrystalTreeRenderer draws it, sending pulses of light up the trunk and through the crystals.
 *
 * <p>This is its master cell (the middle one, on the floor); its other cells are {@link CrystalTreePartBlock}s: the
 * planter's and bench's cells round it (colliding with the gravel, the wall and the seat on an eighth-of-a-block grid)
 * and the trunk's cells over it. The crown does not collide.
 */
public final class CrystalTreeBlock extends Block implements EntityBlock, StructureGhost.Source {
	/** The planter's inner and outer wall, its top, the gravel's top; the seat's front edge and top (blocks). */
	public static final double R_IN = 3.33, R_OUT = 3.57, WALL_TOP = 0.88, GRAVEL = 0.70, SEAT_R = 4.12, SEAT_TOP = 0.46;
	/** The trunk's radius at the floor and above it. */
	public static final double TRUNK_FOOT = 0.55, TRUNK_R = 0.45;
	/** Cells either side of the master, and the trunk's cells over it. */
	public static final int REACH = 4, TRUNK_CELLS = 3;
	public static final List<Vec3i> FOOTPRINT = footprint();
	private static final VoxelShape[] SHAPES = new VoxelShape[(2 * REACH + 1) * (2 * REACH + 1) * (TRUNK_CELLS + 1)];

	public CrystalTreeBlock(Properties properties) {
		super(properties);
	}

	/** The seed the tree grows from: its master's position, so each tree placed is its own. */
	public static long seed(BlockPos master) {
		long h = master.asLong() * 0x9E3779B97F4A7C15L;
		return h ^ (h >>> 29) ^ 0x5DEECE66DL;
	}

	/** Whether any of the planter's round footprint (out to the seat's front) reaches into the master's cell (dx, dz). */
	static boolean inPlanter(int dx, int dz) {
		double nx = Math.min(Math.max(0.5, dx), dx + 1), nz = Math.min(Math.max(0.5, dz), dz + 1);
		return Math.hypot(nx - 0.5, nz - 0.5) < SEAT_R;
	}

	private static List<Vec3i> footprint() {
		List<Vec3i> out = new ArrayList<>();
		out.add(Vec3i.ZERO);
		for (int dz = -REACH; dz <= REACH; dz++) {
			for (int dx = -REACH; dx <= REACH; dx++) {
				if ((dx != 0 || dz != 0) && inPlanter(dx, dz)) {
					out.add(new Vec3i(dx, 0, dz));
				}
			}
		}
		for (int dy = 1; dy <= TRUNK_CELLS; dy++) {
			out.add(new Vec3i(0, dy, 0));
		}
		return List.copyOf(out);
	}

	/** How high the planter stands at r from its middle (0: nothing there), the trunk aside. */
	static double height(double r) {
		if (r <= R_IN) {
			return GRAVEL;
		}
		if (r <= R_OUT) {
			return WALL_TOP;
		}
		return r <= SEAT_R ? SEAT_TOP : 0;
	}

	/** What of the planter, the bench and the trunk is in the master's cell (dx, dy, dz), on an eighth-of-a-block grid. */
	public static VoxelShape shape(int dx, int dy, int dz) {
		if (Math.abs(dx) > REACH || Math.abs(dz) > REACH || dy < 0 || dy > TRUNK_CELLS) {
			return Shapes.empty();
		}
		int k = (dy * (2 * REACH + 1) + dz + REACH) * (2 * REACH + 1) + dx + REACH;
		VoxelShape cached = SHAPES[k];
		if (cached != null) {
			return cached;
		}
		VoxelShape shape = Shapes.empty();
		for (int j = 0; j < 8; j++) {
			for (int i = 0; i < 8; i++) {
				double x = dx + (i + 0.5) / 8 - 0.5, z = dz + (j + 0.5) / 8 - 0.5, r = Math.hypot(x, z);
				double top;
				if (dy == 0) {
					top = r <= TRUNK_FOOT ? 1.0 : height(r);
				} else {
					top = r <= TRUNK_R ? 1.0 : 0;
				}
				if (top > 0) {
					shape = Shapes.or(shape, Shapes.box(i / 8.0, 0, j / 8.0, (i + 1) / 8.0, top, (j + 1) / 8.0));
				}
			}
		}
		VoxelShape made = shape.optimize();
		SHAPES[k] = made;
		return made;
	}

	public static BlockState partState(Vec3i offset) {
		return JugcraftTower.BLOCKS.get("crystal_tree_part").defaultBlockState()
				.setValue(CrystalTreePartBlock.OX, offset.getX() + REACH)
				.setValue(CrystalTreePartBlock.OY, offset.getY())
				.setValue(CrystalTreePartBlock.OZ, offset.getZ() + REACH);
	}

	@Override
	public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
		if (!blocked(context).isEmpty()) {
			if (context.getPlayer() != null && !context.getLevel().isClientSide()) {
				context.getPlayer().sendOverlayMessage(Component.translatable("message.jugcraft.crystal_tree.blocked"));
			}
			return null;
		}
		return defaultBlockState();
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
		List<StructureGhost.Cell> cells = new ArrayList<>();
		for (Vec3i o : FOOTPRINT) {
			cells.add(new StructureGhost.Cell(context.getClickedPos().offset(o), o.equals(Vec3i.ZERO) ? defaultBlockState() : partState(o)));
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
		return new DepotDisplayBlockEntity(JugcraftTower.CRYSTAL_TREE_ENTITY, pos, state);
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
			if (part.getBlock() instanceof CrystalTreePartBlock && CrystalTreePartBlock.masterPos(at, part).equals(pos)) {
				level.removeBlock(at, false);
			}
		}
	}

	@Override
	protected RenderShape getRenderShape(BlockState state) {
		// a model: the client bakes the fixture's parts that never move into the chunk's mesh (client/BakedFixtures); its
		// renderer draws the rest
		return RenderShape.MODEL;
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return shape(0, 0, 0);
	}
}
