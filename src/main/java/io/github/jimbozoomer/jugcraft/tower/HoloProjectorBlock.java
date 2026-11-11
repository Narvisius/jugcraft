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
 * The Holo Projector (work list item 15, approved by Justin on 23 October 2026 after six takes): a 3 x 3 floor projector
 * throwing a big hologram of his character's face into the air, angry, its head and eyes on the nearest player within
 * 20 blocks, a ring of solid words ("You are being watched") turning round it. It replaced the six floating holo panels
 * over the Tri-Pillar's operator areas; the Tri-Pillar has one, in its lobby past the security gates.
 * tools/holo_projector.py makes its housing and the head (tools/holo_head.py); client/HoloProjectorRenderer draws them.
 *
 * <p>This is its master cell (the middle one); its other cells are {@link HoloProjectorPartBlock}s. {@link #FACING} is
 * where the head looks when nobody is near (a player places it facing them). Each cell collides with the slab and deck.
 */
public final class HoloProjectorBlock extends Block implements EntityBlock, StructureGhost.Source {
	/** The slab's half width and top; the dark deck's half width and top. */
	public static final double HALF = 1.5, SLAB = 0.20, DECK_HALF = 1.38, DECK = 0.26;
	/** Cells either side of the master. */
	public static final int REACH = 1;
	public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

	/** The projector's cells from the master: the master first, then row by row. */
	public static final List<Vec3i> FOOTPRINT = footprint();
	private static final VoxelShape[] SHAPES = new VoxelShape[(2 * REACH + 1) * (2 * REACH + 1)];

	public HoloProjectorBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
	}

	private static List<Vec3i> footprint() {
		List<Vec3i> out = new ArrayList<>();
		out.add(Vec3i.ZERO);
		for (int dz = -REACH; dz <= REACH; dz++) {
			for (int dx = -REACH; dx <= REACH; dx++) {
				if (dx != 0 || dz != 0) {
					out.add(new Vec3i(dx, 0, dz));
				}
			}
		}
		return List.copyOf(out);
	}

	/** What of the projector is in the master's cell (dx, dz): the slab, and the deck on it. */
	public static VoxelShape shape(int dx, int dz) {
		if (Math.abs(dx) > REACH || Math.abs(dz) > REACH) {
			return Shapes.empty();
		}
		int k = (dz + REACH) * (2 * REACH + 1) + dx + REACH;
		VoxelShape cached = SHAPES[k];
		if (cached != null) {
			return cached;
		}
		double c = 0.5;
		double[][] pieces = {{c - HALF, 0, c - HALF, c + HALF, SLAB, c + HALF}, {c - DECK_HALF, SLAB, c - DECK_HALF, c + DECK_HALF, DECK, c + DECK_HALF}};
		VoxelShape shape = Shapes.empty();
		for (double[] p : pieces) {
			double x0 = Math.max(p[0] - dx, 0), z0 = Math.max(p[2] - dz, 0);
			double x1 = Math.min(p[3] - dx, 1), z1 = Math.min(p[5] - dz, 1);
			if (x1 - x0 > 1e-6 && z1 - z0 > 1e-6) {
				shape = Shapes.or(shape, Shapes.box(x0, p[1], z0, x1, p[4], z1));
			}
		}
		VoxelShape made = shape.optimize();
		SHAPES[k] = made;
		return made;
	}

	public static BlockState partState(Vec3i offset) {
		return JugcraftTower.BLOCKS.get("holo_projector_part").defaultBlockState()
				.setValue(HoloProjectorPartBlock.OX, offset.getX() + REACH)
				.setValue(HoloProjectorPartBlock.OZ, offset.getZ() + REACH);
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING);
	}

	@Override
	public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
		BlockState state = defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
		if (!blocked(context).isEmpty()) {
			if (context.getPlayer() != null && !context.getLevel().isClientSide()) {
				context.getPlayer().sendOverlayMessage(Component.translatable("message.jugcraft.holo_projector.blocked"));
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
		return new DepotDisplayBlockEntity(JugcraftTower.HOLO_PROJECTOR_ENTITY, pos, state);
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
			if (part.getBlock() instanceof HoloProjectorPartBlock && HoloProjectorPartBlock.masterPos(at, part).equals(pos)) {
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
		return shape(0, 0);
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
