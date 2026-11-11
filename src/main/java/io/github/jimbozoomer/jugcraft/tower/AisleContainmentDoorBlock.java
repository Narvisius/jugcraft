package io.github.jimbozoomer.jugcraft.tower;

import io.github.jimbozoomer.jugcraft.placement.StructureGhost;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.StringRepresentable;
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
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * The Aisle Containment Door (work list item 13, approved by Justin on 22 October 2026 from the data hall mockup):
 * bi-parting automatic sliding doors at an end of a contained cold aisle, two blocks wide (the aisle) and two high, set
 * against the end racks. Its header and track run over both end racks, so each leaf slides a block open in front of its
 * rack. tools/aisle_containment.py makes its models; client/AisleContainmentDoorRenderer draws them (the frame, the
 * leaves slid as far as they are open, the sensors' lights, the label's text) and {@link AisleContainmentDoorBlockEntity}
 * is its sensors.
 *
 * <p>This is its master, the bottom cell on the left as you face it from outside; {@link #FACING} is its outside (where
 * whoever walks up to it comes from; the aisle is behind it). The other three cells are
 * {@link AisleContainmentDoorPartBlock}s. Shut, every cell collides with the door's depth against its back edge (the
 * end racks' plane); open, only the header (the top cells' top) does. {@link #PART} is only for the renderer to find
 * its models: the placed door is always {@link Part#NONE}.
 */
public final class AisleContainmentDoorBlock extends Block implements EntityBlock, StructureGhost.Source {
	public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
	public static final BooleanProperty OPEN = BlockStateProperties.OPEN;
	public static final EnumProperty<Part> PART = EnumProperty.create("part", Part.class);
	/** How far each leaf slides open (px; tools/aisle_containment.py OPEN_U). */
	public static final float SLIDE = 0.985F * 16;
	/** The frame's depth out from the racks' plane (px), the header's bottom in the top cells (px). */
	public static final double DEPTH = 2.24, HEADER = 13.76;
	private static final Map<Direction, VoxelShape[]> SHAPES = new EnumMap<>(Direction.class);
	private static final List<Vec3i> MODEL_CELLS = List.of(Vec3i.ZERO, new Vec3i(1, 0, 0), new Vec3i(0, 1, 0), new Vec3i(1, 1, 0));

	/** The models the renderer draws (the placed block draws none). */
	public enum Part implements StringRepresentable {
		NONE, FRAME_LEFT, FRAME_RIGHT, LEAF_LEFT, LEAF_RIGHT, LED_READY, LED_OPEN;

		@Override
		public String getSerializedName() {
			return name().toLowerCase(Locale.ROOT);
		}
	}

	static {
		for (Direction d : Direction.Plane.HORIZONTAL) {
			VoxelShape[] s = new VoxelShape[8];
			for (int k = 0; k < 8; k++) {
				int dy = (k >> 1) & 1;
				boolean open = (k & 4) != 0;
				double[] box = open ? (dy == 1 ? new double[] {0, HEADER, 16 - DEPTH, 16, 16, 16} : null)
						: new double[] {0, 0, 16 - DEPTH, 16, 16, 16};
				s[k] = box == null ? Shapes.empty() : FurnitureBlock.turned(box, d);
			}
			SHAPES.put(d, s);
		}
	}

	public AisleContainmentDoorBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(OPEN, false).setValue(PART, Part.NONE));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, OPEN, PART);
	}

	/** A cell offset of the door drawn facing north (its second cell +x) turned to face {@code facing} (clockwise, as the
	 * renderer turns its models). */
	public static int[] turn(int dx, int dz, Direction facing) {
		return switch (facing) {
			case EAST -> new int[] {-dz, dx};
			case SOUTH -> new int[] {-dx, -dz};
			case WEST -> new int[] {dz, -dx};
			default -> new int[] {dx, dz};
		};
	}

	/** The master and its three other cells, facing {@code facing}. */
	public static List<Vec3i> footprint(Direction facing) {
		List<Vec3i> out = new ArrayList<>();
		for (Vec3i m : MODEL_CELLS) {
			int[] t = turn(m.getX(), m.getZ(), facing);
			out.add(new Vec3i(t[0], m.getY(), t[1]));
		}
		return out;
	}

	/** What of the door is in a cell (dy its row): shut, its depth against the back; open, the header only. */
	public static VoxelShape shape(int dy, Direction facing, boolean open) {
		return SHAPES.get(facing)[(open ? 4 : 0) | (dy == 1 ? 2 : 0)];
	}

	/** A part's state for the cell at {@code offset} from its master (the offset as it stands, turned). */
	public static BlockState partState(Vec3i offset) {
		return JugcraftTower.BLOCKS.get("aisle_containment_door_part").defaultBlockState()
				.setValue(AisleContainmentDoorPartBlock.OX, offset.getX() + 1)
				.setValue(AisleContainmentDoorPartBlock.OY, offset.getY())
				.setValue(AisleContainmentDoorPartBlock.OZ, offset.getZ() + 1);
	}

	@Override
	public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
		BlockState state = defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
		if (!blocked(context, state).isEmpty()) {
			if (context.getPlayer() != null && !context.getLevel().isClientSide()) {
				context.getPlayer().sendOverlayMessage(Component.translatable("message.jugcraft.aisle_containment_door.blocked"));
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
		Direction facing = state.getValue(FACING);
		List<StructureGhost.Cell> cells = new ArrayList<>();
		for (Vec3i o : footprint(facing)) {
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
		Direction facing = state.getValue(FACING);
		for (Vec3i o : footprint(facing)) {
			if (!o.equals(Vec3i.ZERO)) {
				level.setBlock(pos.offset(o), partState(o), Block.UPDATE_ALL);
			}
		}
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new AisleContainmentDoorBlockEntity(pos, state);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		if (!(level instanceof ServerLevel) || type != JugcraftTower.AISLE_CONTAINMENT_DOOR_ENTITY) {
			return null;
		}
		return (BlockEntityTicker<T>) (BlockEntityTicker<AisleContainmentDoorBlockEntity>) (l, p, s, door) -> door.serverTick((ServerLevel) l, s);
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
			if (part.getBlock() instanceof AisleContainmentDoorPartBlock && AisleContainmentDoorPartBlock.masterPos(at, part).equals(pos)) {
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
		return shape(0, state.getValue(FACING), false);
	}

	/** Walkers plan their way through the doorway (it opens for them as they come, as the sliding doors do): shut, it was
	 * a wall to the path finder, and the technicians' way to the cold aisle went nowhere (10 November 2026). */
	@Override
	protected boolean isPathfindable(BlockState state, net.minecraft.world.level.pathfinder.PathComputationType type) {
		return type == net.minecraft.world.level.pathfinder.PathComputationType.LAND;
	}

	@Override
	protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return shape(0, state.getValue(FACING), state.getValue(OPEN));
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
