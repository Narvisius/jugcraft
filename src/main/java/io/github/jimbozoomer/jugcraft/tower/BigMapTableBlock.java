package io.github.jimbozoomer.jugcraft.tower;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.drone.DepotDisplayBlockEntity;
import io.github.jimbozoomer.jugcraft.drone.DroneTerminalBlockEntity;
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
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * The Big Map Table (work list item 16, approved by Justin on 23 October 2026 after three takes): the map room's 13 by 13
 * octagonal table, the world round the tower over its glass well in miniature (tower/MapScan; client/BigMapTableRenderer
 * draws the table from tools/big_map_table.py's mesh and the map over it), a touch panel in each chamfered corner.
 *
 * <p>This is its master, the middle cell; the other cells its body covers are {@link BigMapTablePartBlock}s. The cells and
 * each one's collision come from the generator ({@code assets/jugcraft/tower_mesh/big_map_table.json}). It shows the drone
 * terminal it is pinned to (a tower's own, when the tower places it) or the nearest within {@link #linkRange()}; using any
 * part of it opens that terminal's screen, as the holo tables do.
 */
public final class BigMapTableBlock extends Block implements EntityBlock, DepotDisplayBlockEntity.Display, StructureGhost.Source {
	private static final String DATA = "/assets/jugcraft/tower_mesh/big_map_table.json";

	/** The cells (offsets from the master, the master's first) and each one's collision boxes (pixels, its own cell). */
	public static final List<Vec3i> CELLS;
	private static final List<double[][]> BOXES;
	private static final VoxelShape[] SHAPES;

	static {
		List<Vec3i> cells = new ArrayList<>();
		List<double[][]> boxes = new ArrayList<>();
		try (InputStream raw = BigMapTableBlock.class.getResourceAsStream(DATA)) {
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
			Jugcraft.LOGGER.error("Could not read the Big Map Table's cells {}", DATA, e);
			cells.clear();
			boxes.clear();
			cells.add(Vec3i.ZERO);
			boxes.add(new double[][] {{0, 0, 0, 16, 16, 16}});
		}
		if (cells.size() > BigMapTablePartBlock.PARTS) {
			throw new IllegalStateException(cells.size() + " Big Map Table cells: more than BigMapTablePartBlock.INDEX holds");
		}
		CELLS = List.copyOf(cells);
		BOXES = List.copyOf(boxes);
		SHAPES = new VoxelShape[CELLS.size()];
	}

	public BigMapTableBlock(Properties properties) {
		super(properties);
	}

	/** Cell {@code index}'s offset from the master. */
	public static Vec3i offset(int index) {
		return CELLS.get(Math.max(0, Math.min(index, CELLS.size() - 1)));
	}

	/** What of the table is in cell {@code index}. */
	public static VoxelShape shape(int index) {
		if (index < 0 || index >= CELLS.size()) {
			return Shapes.empty();
		}
		VoxelShape cached = SHAPES[index];
		if (cached != null) {
			return cached;
		}
		VoxelShape shape = Shapes.empty();
		for (double[] b : BOXES.get(index)) {
			shape = Shapes.or(shape, Block.box(b[0], b[1], b[2], b[3], b[4], b[5]));
		}
		VoxelShape made = shape.optimize();
		SHAPES[index] = made;
		return made;
	}

	public static BlockState partState(int index) {
		return JugcraftTower.BLOCKS.get("big_map_table_part").defaultBlockState().setValue(BigMapTablePartBlock.INDEX, index);
	}

	@Override
	public boolean isAnchor(BlockState state) {
		return true;
	}

	/** The map room is upstairs from the control hall's terminal. */
	@Override
	public int linkRange() {
		return 96;
	}

	@Override
	public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
		if (!blocked(context).isEmpty()) {
			if (context.getPlayer() != null && !context.getLevel().isClientSide()) {
				context.getPlayer().sendOverlayMessage(Component.translatable("message.jugcraft.big_map_table.blocked"));
			}
			return null;
		}
		return defaultBlockState();
	}

	private static List<BlockPos> blocked(BlockPlaceContext context) {
		Level level = context.getLevel();
		List<BlockPos> out = new ArrayList<>();
		for (Vec3i cell : CELLS) {
			BlockPos at = context.getClickedPos().offset(cell);
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
		for (int i = 0; i < CELLS.size(); i++) {
			cells.add(new StructureGhost.Cell(context.getClickedPos().offset(CELLS.get(i)), i == 0 ? defaultBlockState() : partState(i)));
		}
		return new StructureGhost(List.copyOf(cells), blocked(context));
	}

	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
		super.setPlacedBy(level, pos, state, placer, stack);
		if (level.isClientSide()) {
			return;
		}
		for (int i = 1; i < CELLS.size(); i++) {
			level.setBlock(pos.offset(CELLS.get(i)), partState(i), Block.UPDATE_ALL);
		}
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new BigMapTableBlockEntity(pos, state);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return level.isClientSide() || type != JugcraftTower.BIG_MAP_TABLE_ENTITY ? null
				: (BlockEntityTicker<T>) (BlockEntityTicker<BigMapTableBlockEntity>) BigMapTableBlockEntity::serverTick;
	}

	/** Gone, it takes its parts with it. */
	@Override
	protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
		super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
		if (level.getBlockState(pos).is(this)) {
			return;
		}
		for (int i = 1; i < CELLS.size(); i++) {
			BlockPos at = pos.offset(CELLS.get(i));
			if (!level.isLoaded(at)) {
				continue;
			}
			BlockState part = level.getBlockState(at);
			if (part.getBlock() instanceof BigMapTablePartBlock && BigMapTablePartBlock.masterPos(at, part).equals(pos)) {
				level.removeBlock(at, false);
			}
		}
	}

	/** Using the table opens its terminal's screen (claiming an unowned terminal), as the holo tables do. */
	static InteractionResult use(Level level, BlockPos master, Player player) {
		if (level.getBlockEntity(master) instanceof DepotDisplayBlockEntity display && display.terminal() != null
				&& level.isLoaded(display.terminal()) && level.getBlockEntity(display.terminal()) instanceof DroneTerminalBlockEntity terminal) {
			if (level.isClientSide()) {
				MasterConsoleBlock.openScreen.accept(display.terminal(), master);
			} else {
				if (terminal.owner() == null) {
					terminal.setOwner(player.getUUID());
					player.sendOverlayMessage(Component.translatable("message.jugcraft.drone.claimed"));
				}
				terminal.rescanNow();
				terminal.refreshClients();
			}
		} else if (!level.isClientSide()) {
			player.sendOverlayMessage(Component.translatable("message.jugcraft.master_console.unlinked"));
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		return use(level, pos, player);
	}

	@Override
	protected RenderShape getRenderShape(BlockState state) {
		// a model: the client bakes the fixture's parts that never move into the chunk's mesh (client/BakedFixtures); its
		// renderer draws the rest
		return RenderShape.MODEL;
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return shape(0);
	}
}
