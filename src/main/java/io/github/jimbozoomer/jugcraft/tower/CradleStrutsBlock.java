package io.github.jimbozoomer.jugcraft.tower;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.drone.DepotDisplayBlockEntity;
import io.github.jimbozoomer.jugcraft.placement.StructureGhost;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
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
 * The Cradle Struts (work list item 12, approved by Justin on 23 October 2026 after three takes): the pair of raked
 * steel struts under one of the Tri-Pillar's supply cradles, in place of the staircases of girder blocks it stood on.
 * Each strut is one wide-flange member from the pillar's chamfer up under the cradle, its ends cut flat onto their
 * plates (the foot on an end plate bolted to a bracket on the chamfer, a seat angle under it; the head on an end plate
 * and a bearing plate bolted up into the deck), flat-bar bracing on gusset plates between the two.
 * tools/cradle_struts.py makes the mesh; client/CradleStrutsRenderer draws it.
 *
 * <p>This is its master cell, the foot of the strut on the cradle's left seen from the pillar; the other cells the struts
 * pass through are {@link CradleStrutsPartBlock}s. The cells and each one's collision come from the generator
 * ({@code assets/jugcraft/tower_mesh/cradle_struts.json}), for the canonical pair rising toward -x +z ({@link #FACING}
 * south); west, north and east turn it a quarter clockwise (seen from above) each (-x -z, +x -z, +x +z).
 */
public final class CradleStrutsBlock extends Block implements EntityBlock, StructureGhost.Source {
	public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
	private static final String DATA = "/assets/jugcraft/tower_mesh/cradle_struts.json";

	/** The cells from the master facing south, the master's first; each one's collision boxes (pixels, its own cell). */
	public static final List<Vec3i> CELLS;
	private static final List<double[][]> BOXES;
	private static final VoxelShape[][] SHAPES;

	static {
		List<Vec3i> cells = new ArrayList<>();
		List<double[][]> boxes = new ArrayList<>();
		try (InputStream raw = CradleStrutsBlock.class.getResourceAsStream(DATA)) {
			if (raw == null) {
				throw new IllegalStateException("missing " + DATA);
			}
			JsonObject root = JsonParser.parseReader(new InputStreamReader(raw, StandardCharsets.UTF_8)).getAsJsonObject();
			JsonArray cs = root.getAsJsonArray("cells"), bs = root.getAsJsonArray("boxes");
			for (int i = 0; i < cs.size(); i++) {
				JsonArray c = cs.get(i).getAsJsonArray();
				cells.add(new Vec3i(c.get(0).getAsInt(), c.get(1).getAsInt(), c.get(2).getAsInt()));
				JsonArray list = bs.get(i).getAsJsonArray();
				double[][] out = new double[list.size()][6];
				for (int k = 0; k < list.size(); k++) {
					JsonArray b = list.get(k).getAsJsonArray();
					for (int j = 0; j < 6; j++) {
						out[k][j] = b.get(j).getAsDouble();
					}
				}
				boxes.add(out);
			}
		} catch (Exception e) {
			Jugcraft.LOGGER.error("Could not read the cradle struts' cells {}", DATA, e);
			cells.clear();
			boxes.clear();
			cells.add(Vec3i.ZERO);
			boxes.add(new double[][] {{0, 0, 0, 16, 16, 16}});
		}
		if (cells.size() > CradleStrutsPartBlock.PARTS) {
			throw new IllegalStateException(cells.size() + " cradle strut cells: more than CradleStrutsPartBlock.INDEX holds");
		}
		CELLS = List.copyOf(cells);
		BOXES = List.copyOf(boxes);
		SHAPES = new VoxelShape[4][CELLS.size()];
	}

	public CradleStrutsBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.SOUTH));
	}

	/** Quarter turns clockwise (seen from above) from the canonical pair (facing south). */
	public static int turns(Direction facing) {
		return switch (facing) {
			case WEST -> 1;
			case NORTH -> 2;
			case EAST -> 3;
			default -> 0;
		};
	}

	/** A canonical cell offset turned to the pair's facing. */
	public static Vec3i turn(Vec3i o, Direction facing) {
		int x = o.getX(), z = o.getZ();
		for (int i = 0; i < turns(facing); i++) {
			int t = x;
			x = -z;
			z = t;
		}
		return new Vec3i(x, o.getY(), z);
	}

	/** Cell {@code index}'s offset from the master, the pair facing {@code facing}. */
	public static Vec3i offset(int index, Direction facing) {
		return turn(CELLS.get(Math.max(0, Math.min(index, CELLS.size() - 1))), facing);
	}

	/** What of the struts is in cell {@code index} of a pair facing {@code facing}. */
	public static VoxelShape shape(int index, Direction facing) {
		if (index < 0 || index >= CELLS.size()) {
			return Shapes.empty();
		}
		int t = turns(facing);
		VoxelShape cached = SHAPES[t][index];
		if (cached != null) {
			return cached;
		}
		VoxelShape shape = Shapes.empty();
		for (double[] b : BOXES.get(index)) {
			double x0 = b[0], z0 = b[2], x1 = b[3], z1 = b[5];
			for (int i = 0; i < t; i++) {
				// (x, z) -> (16 - z, x) about the cell's middle
				double nx0 = 16 - z1, nx1 = 16 - z0, nz0 = x0, nz1 = x1;
				x0 = nx0;
				x1 = nx1;
				z0 = nz0;
				z1 = nz1;
			}
			shape = Shapes.or(shape, Block.box(x0, b[1], z0, x1, b[4], z1));
		}
		VoxelShape made = shape.optimize();
		SHAPES[t][index] = made;
		return made;
	}

	public static BlockState partState(int index, Direction facing) {
		return JugcraftTower.BLOCKS.get("cradle_struts_part").defaultBlockState().setValue(CradleStrutsPartBlock.INDEX, index)
				.setValue(CradleStrutsPartBlock.FACING, facing);
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING);
	}

	@Override
	public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
		BlockState state = defaultBlockState().setValue(FACING, context.getHorizontalDirection());
		if (!blocked(context, state.getValue(FACING)).isEmpty()) {
			if (context.getPlayer() != null && !context.getLevel().isClientSide()) {
				context.getPlayer().sendOverlayMessage(Component.translatable("message.jugcraft.cradle_struts.blocked"));
			}
			return null;
		}
		return state;
	}

	private static List<BlockPos> blocked(BlockPlaceContext context, Direction facing) {
		Level level = context.getLevel();
		List<BlockPos> out = new ArrayList<>();
		for (int i = 0; i < CELLS.size(); i++) {
			BlockPos at = context.getClickedPos().offset(offset(i, facing));
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
		Direction facing = context.getHorizontalDirection();
		BlockState state = defaultBlockState().setValue(FACING, facing);
		List<StructureGhost.Cell> cells = new ArrayList<>();
		for (int i = 0; i < CELLS.size(); i++) {
			cells.add(new StructureGhost.Cell(context.getClickedPos().offset(offset(i, facing)), i == 0 ? state : partState(i, facing)));
		}
		return new StructureGhost(List.copyOf(cells), blocked(context, facing));
	}

	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
		super.setPlacedBy(level, pos, state, placer, stack);
		if (level.isClientSide()) {
			return;
		}
		Direction facing = state.getValue(FACING);
		for (int i = 1; i < CELLS.size(); i++) {
			level.setBlock(pos.offset(offset(i, facing)), partState(i, facing), Block.UPDATE_ALL);
		}
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new DepotDisplayBlockEntity(JugcraftTower.CRADLE_STRUTS_ENTITY, pos, state);
	}

	/** Gone, it takes its parts with it. */
	@Override
	protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
		super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
		if (level.getBlockState(pos).is(this)) {
			return;
		}
		Direction facing = state.getValue(FACING);
		for (int i = 1; i < CELLS.size(); i++) {
			BlockPos at = pos.offset(offset(i, facing));
			if (!level.isLoaded(at)) {
				continue;
			}
			BlockState part = level.getBlockState(at);
			if (part.getBlock() instanceof CradleStrutsPartBlock && CradleStrutsPartBlock.masterPos(at, part).equals(pos)) {
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
		return shape(0, state.getValue(FACING));
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
