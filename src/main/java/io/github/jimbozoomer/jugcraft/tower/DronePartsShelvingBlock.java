package io.github.jimbozoomer.jugcraft.tower;

import io.github.jimbozoomer.jugcraft.drone.DepotDisplayBlockEntity;
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
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
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
 * The Drone Parts Shelving (work list item 9, approved by Justin on 22 October 2026 from the take-2 mockup): the Tri-Pillar
 * workshop's parts wall, industrial longspan shelving 2.94 wide, 0.60 deep and 2.30 high with five levels of the fleet's
 * spares on it. tools/drone_parts_shelving.py makes its meshes (one per {@link #STOCK}: a with the frames, the batteries
 * charging, motors, hardware, props, the T4's rotors, cartons and cases and the extinguisher on its end; b with booms, fan
 * ducts, gimbal cases, big motors, long props and cartons); client/DronePartsShelvingRenderer draws them and keeps the
 * battery gauges, the station's slot lights and the two screens live.
 *
 * <p>This is its master, the middle cell at the bottom; {@link #FACING} is its front (where its user stands), so it covers
 * 3 cells across its front and 3 high: the other 8 are {@link DronePartsShelvingPartBlock}s, each colliding with the
 * shelving's depth in its cell (its back against the cell's back edge).
 */
public final class DronePartsShelvingBlock extends Block implements EntityBlock, DepotDisplayBlockEntity.Display, StructureGhost.Source {
	public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
	public static final EnumProperty<Stock> STOCK = EnumProperty.create("stock", Stock.class);
	/** How far a part may be from its master across, and how many cells high it stands. */
	public static final int REACH = 1, HEIGHT = 3;
	/** The shelving in its frame (facing south): half its width, its back and front, its height. */
	public static final double HALF_WIDTH = 1.47, BACK = -0.472, FRONT = 0.145, TOP = 2.30;
	private static final Map<Direction, List<Vec3i>> FOOTPRINTS = new EnumMap<>(Direction.class);
	private static final Map<Direction, VoxelShape[]> SHAPES = new EnumMap<>(Direction.class);

	/** What is on it. */
	public enum Stock implements StringRepresentable {
		A, B;

		@Override
		public String getSerializedName() {
			return name().toLowerCase(Locale.ROOT);
		}
	}

	static {
		for (Direction d : Direction.Plane.HORIZONTAL) {
			List<Vec3i> cells = new ArrayList<>();
			cells.add(Vec3i.ZERO);
			for (int dy = 0; dy < HEIGHT; dy++) {
				for (int dx = -REACH; dx <= REACH; dx++) {
					int[] t = turn(dx, 0, d);
					if (t[0] != 0 || t[1] != 0 || dy != 0) {
						cells.add(new Vec3i(t[0], dy, t[1]));
					}
				}
			}
			FOOTPRINTS.put(d, List.copyOf(cells));
			SHAPES.put(d, new VoxelShape[(2 * REACH + 1) * (2 * REACH + 1) * HEIGHT]);
		}
	}

	public DronePartsShelvingBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.SOUTH).setValue(STOCK, Stock.A));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, STOCK);
	}

	/** A cell offset of the shelving facing south (its front to +z) turned to face {@code facing}. */
	static int[] turn(int dx, int dz, Direction facing) {
		return switch (facing) {
			case NORTH -> new int[] {-dx, -dz};
			case EAST -> new int[] {dz, -dx};
			case WEST -> new int[] {-dz, dx};
			default -> new int[] {dx, dz};
		};
	}

	/** The master and the 8 cells it covers, facing {@code facing}. */
	public static List<Vec3i> footprint(Direction facing) {
		return FOOTPRINTS.get(facing);
	}

	/** What of the shelving is in the master's cell (dx, dy, dz), facing {@code facing}: its depth across the cell, as high as it reaches. */
	public static VoxelShape shape(int dx, int dy, int dz, Direction facing) {
		if (Math.abs(dx) > REACH || Math.abs(dz) > REACH || dy < 0 || dy >= HEIGHT) {
			return Shapes.empty();
		}
		VoxelShape[] cache = SHAPES.get(facing);
		int k = (dy * (2 * REACH + 1) + dz + REACH) * (2 * REACH + 1) + dx + REACH;
		if (cache[k] != null) {
			return cache[k];
		}
		// back to the model's offset (facing south) and its slice of the shelving, in the cell's own 0..1
		int[] back = switch (facing) {
			case NORTH -> new int[] {-dx, -dz};
			case EAST -> new int[] {-dz, dx};
			case WEST -> new int[] {dz, -dx};
			default -> new int[] {dx, dz};
		};
		VoxelShape made = Shapes.empty();
		if (back[1] == 0) {
			double u0 = Math.max(-HALF_WIDTH, back[0] - 0.5) - (back[0] - 0.5), u1 = Math.min(HALF_WIDTH, back[0] + 0.5) - (back[0] - 0.5);
			double w0 = BACK + 0.5, w1 = FRONT + 0.5;
			double y1 = Math.min(1.0, TOP - dy);
			if (u1 > u0 && y1 > 0) {
				double[] b = switch (facing) {
					case NORTH -> new double[] {1 - u1, 1 - w1, 1 - u0, 1 - w0};
					case EAST -> new double[] {w0, 1 - u1, w1, 1 - u0};
					case WEST -> new double[] {1 - w1, u0, 1 - w0, u1};
					default -> new double[] {u0, w0, u1, w1};
				};
				made = Shapes.box(b[0], 0, b[1], b[2], y1, b[3]);
			}
		}
		cache[k] = made;
		return made;
	}

	public static BlockState partState(Vec3i offset) {
		return JugcraftTower.BLOCKS.get("drone_parts_shelving_part").defaultBlockState()
				.setValue(DronePartsShelvingPartBlock.OX, offset.getX() + REACH)
				.setValue(DronePartsShelvingPartBlock.OY, offset.getY())
				.setValue(DronePartsShelvingPartBlock.OZ, offset.getZ() + REACH);
	}

	@Override
	public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
		BlockState state = defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
		if (!blocked(context, state).isEmpty()) {
			if (context.getPlayer() != null && !context.getLevel().isClientSide()) {
				context.getPlayer().sendOverlayMessage(Component.translatable("message.jugcraft.drone_parts_shelving.blocked"));
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
		return new DepotDisplayBlockEntity(JugcraftTower.DRONE_PARTS_SHELVING_ENTITY, pos, state);
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
			if (part.getBlock() instanceof DronePartsShelvingPartBlock && DronePartsShelvingPartBlock.masterPos(at, part).equals(pos)) {
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
		return shape(0, 0, 0, state.getValue(FACING));
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
