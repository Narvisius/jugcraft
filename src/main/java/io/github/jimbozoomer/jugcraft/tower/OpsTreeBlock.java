package io.github.jimbozoomer.jugcraft.tower;

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
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * The flight-ops trees (Justin, 5 November 2026: exotic trees in place of the hall's two clusters of flight-ops desks;
 * from the options sheet he chose option A for one floor and option D for the other): the {@link Kind#EMBER_OAK Ember
 * Oak} (the west floor) and the {@link Kind#DRIFT_ISLE Drift Isle} (the east), each about 9 blocks across, our own
 * designs. tools/ops_trees.py bakes them (three variants of each) and client/OpsTreeRenderer draws them live: the Ember
 * Oak's cracks breathing light, its hot leaves and coals flickering, sparks rising out of its crown; the Drift Isle's
 * island bobbing and slowly turning over its basin, the lift beam's light rising, the root tips pulsing, stones circling.
 * Which variant, and how it is turned, follows from {@link #seed} (the master's position), so trees placed side by side
 * differ.
 *
 * <p>This is a tree's master cell (the middle one, on the floor); its other cells are {@link OpsTreePartBlock}s: the
 * bed's cells round it, colliding on an eighth-of-a-block grid (the Ember Oak's with each basalt column as it stands, the
 * gravel and the trunk; the Drift Isle's with the basin, its floor and the seat), and over it the Ember Oak's trunk or
 * the Drift Isle's island (to stand on, and its rock hanging under it) and trunk. The crowns do not collide.
 */
public final class OpsTreeBlock extends Block implements EntityBlock, StructureGhost.Source {
	/** Cells either side of the master; the highest part over it. */
	public static final int REACH = 4, TOP = 4;
	/** The Ember Oak: how far out its columns reach, its gravel's top and edge, its trunk's radius at the floor and over it. */
	public static final double EMBER_R = 4.3, GRAVEL = 0.5, GRAVEL_R = 3.0, TRUNK_FOOT = 0.6, TRUNK_R = 0.45;
	/** The Ember Oak's soil (r185): how high its mound rises over the gravel's level at the trunk's foot (as
	 * tools/ops_trees.py's SOIL_MOUND; its ridges along the roots, a few hundredths, are left to the eye). */
	public static final double MOUND = 0.17;
	/** The Drift Isle: the seat's front edge and top, the basin wall's inner and outer faces and top, the basin's floor. */
	public static final double SEAT_R = 4.12, SEAT_TOP = 0.46, BASIN_IN = 2.9, BASIN_OUT = 3.2, WALL_TOP = 0.6, BASIN_FLOOR = 0.2;
	/** The Drift Isle's island (where it collides: its thick middle, a thinner rim; tops and bottoms in its cell, 2 up). */
	public static final double ISLE_R = 1.85, ISLE_MIDDLE = 1.3, ISLE_TOP = 0.875, ISLE_BOTTOM = 0.0, RIM_BOTTOM = 0.375, ISLE_TRUNK_R = 0.4;
	public static final int ISLE_DY = 2;

	public enum Kind {
		EMBER_OAK("ember_oak"), DRIFT_ISLE("drift_isle");

		public final String id;
		private List<Vec3i> footprint;
		private final VoxelShape[] shapes = new VoxelShape[(2 * REACH + 1) * (2 * REACH + 1) * (TOP + 1)];

		Kind(String id) {
			this.id = id;
		}

		/** How far from the middle its bed reaches (the cells it touches are its parts). */
		public double radius() {
			return this == EMBER_OAK ? EMBER_R : SEAT_R;
		}

		/** Its cells as offsets from the master: the master first, the bed's cells, then those over it. */
		public synchronized List<Vec3i> footprint() {
			if (footprint == null) {
				List<Vec3i> out = new ArrayList<>();
				out.add(Vec3i.ZERO);
				for (int dz = -REACH; dz <= REACH; dz++) {
					for (int dx = -REACH; dx <= REACH; dx++) {
						if ((dx != 0 || dz != 0) && near(dx, dz) < radius()) {
							out.add(new Vec3i(dx, 0, dz));
						}
					}
				}
				if (this == EMBER_OAK) {
					out.add(new Vec3i(0, 1, 0));
					out.add(new Vec3i(0, 2, 0));
				} else {
					for (int dz = -REACH; dz <= REACH; dz++) {
						for (int dx = -REACH; dx <= REACH; dx++) {
							if (near(dx, dz) < ISLE_R) {
								out.add(new Vec3i(dx, ISLE_DY, dz));
							}
						}
					}
					out.add(new Vec3i(0, 3, 0));
					out.add(new Vec3i(0, 4, 0));
				}
				footprint = List.copyOf(out);
			}
			return footprint;
		}
	}

	public final Kind kind;

	public OpsTreeBlock(Kind kind, Properties properties) {
		super(properties);
		this.kind = kind;
	}

	/** The seed a placed tree's variant and turn follow from: its master's position. */
	public static long seed(BlockPos master) {
		return CrystalTreeBlock.seed(master);
	}

	/** How near the master's cell (dx, dz) comes to the tree's middle (over the master cell's middle). */
	static double near(int dx, int dz) {
		double nx = Math.min(Math.max(0.5, dx), dx + 1), nz = Math.min(Math.max(0.5, dz), dz + 1);
		return Math.hypot(nx - 0.5, nz - 0.5);
	}

	// ------------------------------------------------------------------ the Ember Oak's columns

	/** Per column: x, z (from the tree's middle), its top; and a column's corners' distance from its middle. */
	private static double[] columns;
	private static double columnR;

	/** The Ember Oak's basalt columns, from data/jugcraft/ops_trees/ember_oak_bed.json (tools/ops_trees.py). */
	static synchronized double[] columns() {
		if (columns == null) {
			String path = "/data/jugcraft/ops_trees/ember_oak_bed.json";
			try (InputStream raw = OpsTreeBlock.class.getResourceAsStream(path)) {
				if (raw == null) {
					throw new IllegalStateException("missing " + path);
				}
				JsonObject root = JsonParser.parseReader(new InputStreamReader(raw, StandardCharsets.UTF_8)).getAsJsonObject();
				columnR = root.get("column_r").getAsDouble();
				var list = root.getAsJsonArray("columns");
				double[] out = new double[list.size() * 3];
				for (int i = 0; i < list.size(); i++) {
					var c = list.get(i).getAsJsonArray();
					out[i * 3] = c.get(0).getAsDouble();
					out[i * 3 + 1] = c.get(1).getAsDouble();
					out[i * 3 + 2] = c.get(2).getAsDouble();
				}
				columns = out;
			} catch (Exception e) {
				Jugcraft.LOGGER.error("Could not read the Ember Oak's bed", e);
				columns = new double[0];
			}
		}
		return columns;
	}

	/** How high the Ember Oak's bed stands at (x, z) from its middle: the column there (hexagons, a corner to the north
	 * and south), else the gravel, else nothing. */
	static double bedHeight(double x, double z) {
		double[] c = columns();
		double apothem = columnR * Math.sqrt(3) / 2, top = Math.hypot(x, z) <= GRAVEL_R ? GRAVEL + mound(Math.hypot(x, z)) : 0;
		for (int i = 0; i + 2 < c.length; i += 3) {
			double dx = x - c[i], dz = z - c[i + 1];
			if (Math.abs(dx) <= apothem && Math.abs(0.5 * dx + 0.8660254 * dz) <= apothem && Math.abs(-0.5 * dx + 0.8660254 * dz) <= apothem) {
				top = Math.max(top, c[i + 2]);
			}
		}
		if (c.length == 0) {
			double r = Math.hypot(x, z);
			top = r <= GRAVEL_R ? GRAVEL : r <= 3.55 ? 0.8 : r <= EMBER_R ? SEAT_TOP : 0;
		}
		return top;
	}

	/** How far the Ember Oak's soil rises over the gravel's level at r from its middle: its mound, level at the columns. */
	static double mound(double r) {
		return MOUND * Math.exp(-(r / 1.25) * (r / 1.25)) * Math.max(0, Math.min(1, (2.95 - r) / 0.35));
	}

	// ------------------------------------------------------------------ what collides

	/** What of the tree is in the master's cell (dx, dy, dz), on an eighth-of-a-block grid. */
	public static VoxelShape shape(Kind kind, int dx, int dy, int dz) {
		if (Math.abs(dx) > REACH || Math.abs(dz) > REACH || dy < 0 || dy > TOP) {
			return Shapes.empty();
		}
		int k = (dy * (2 * REACH + 1) + dz + REACH) * (2 * REACH + 1) + dx + REACH;
		VoxelShape cached = kind.shapes[k];
		if (cached != null) {
			return cached;
		}
		VoxelShape shape = Shapes.empty();
		for (int j = 0; j < 8; j++) {
			for (int i = 0; i < 8; i++) {
				double x = dx + (i + 0.5) / 8 - 0.5, z = dz + (j + 0.5) / 8 - 0.5, r = Math.hypot(x, z);
				double bottom = 0, top = 0;
				if (kind == Kind.EMBER_OAK) {
					if (dy == 0) {
						top = r <= TRUNK_FOOT ? 1.0 : bedHeight(x, z);
					} else if (dy <= 2) {
						top = r <= TRUNK_R ? 1.0 : 0;
					}
				} else if (dy == 0) {
					top = r <= BASIN_IN ? BASIN_FLOOR : r <= BASIN_OUT ? WALL_TOP : r <= SEAT_R ? SEAT_TOP : 0;
				} else if (dy == ISLE_DY) {
					if (r <= ISLE_R) {
						top = ISLE_TOP;
						bottom = r <= ISLE_MIDDLE ? underside(r) : RIM_BOTTOM;
					}
				} else if (dy > ISLE_DY) {
					top = r <= ISLE_TRUNK_R ? 1.0 : 0;
				}
				if (top > bottom) {
					shape = Shapes.or(shape, Shapes.box(i / 8.0, bottom, j / 8.0, (i + 1) / 8.0, top, (j + 1) / 8.0));
				}
			}
		}
		VoxelShape made = shape.optimize();
		kind.shapes[k] = made;
		return made;
	}

	/**
	 * The island's rock underneath at {@code r} from its middle, in its cell (2 up): the mockup's profile (tools/ops_trees.py
	 * drift_isle's under()), round and a little inside the rock's bumps, hanging below the cell down to its lowest point
	 * three quarters of a block under it (r181; Justin, 8 November 2026: "you can walk into the mountain thing that's down
	 * there"). A shape reaching below its cell still stops whoever walks into it: the part's shape is dynamic, which the
	 * game takes to mean it may reach past its own cell.
	 */
	static double underside(double r) {
		double f = r / 2.1;
		double y = ISLE_Y - 0.05 - 1.55 * (1 - Math.pow(f, 1.5)) - ISLE_DY;
		return Math.max(-0.75, Math.min(ISLE_BOTTOM, y + 0.03 + 0.1 * f));
	}

	/** The island's top over the master cell's floor (tools/ops_trees.py DRIFT_ISLE_Y). */
	static final double ISLE_Y = 2.85;

	public BlockState partState(Vec3i offset) {
		return partState(kind, offset);
	}

	public static BlockState partState(Kind kind, Vec3i offset) {
		return JugcraftTower.BLOCKS.get(kind.id + "_part").defaultBlockState()
				.setValue(OpsTreePartBlock.OX, offset.getX() + REACH)
				.setValue(OpsTreePartBlock.OY, offset.getY())
				.setValue(OpsTreePartBlock.OZ, offset.getZ() + REACH);
	}

	@Override
	public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
		if (!blocked(context).isEmpty()) {
			if (context.getPlayer() != null && !context.getLevel().isClientSide()) {
				context.getPlayer().sendOverlayMessage(Component.translatable("message.jugcraft." + kind.id + ".blocked"));
			}
			return null;
		}
		return defaultBlockState();
	}

	private List<BlockPos> blocked(BlockPlaceContext context) {
		Level level = context.getLevel();
		List<BlockPos> out = new ArrayList<>();
		for (Vec3i o : kind.footprint()) {
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
		for (Vec3i o : kind.footprint()) {
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
		for (Vec3i o : kind.footprint()) {
			if (!o.equals(Vec3i.ZERO)) {
				level.setBlock(pos.offset(o), partState(o), Block.UPDATE_ALL);
			}
		}
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new DepotDisplayBlockEntity(JugcraftTower.OPS_TREE_ENTITY, pos, state);
	}

	/** Gone, it takes its parts with it. */
	@Override
	protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
		super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
		if (level.getBlockState(pos).is(this)) {
			return;
		}
		for (Vec3i o : kind.footprint()) {
			BlockPos at = pos.offset(o);
			if (o.equals(Vec3i.ZERO) || !level.isLoaded(at)) {
				continue;
			}
			BlockState part = level.getBlockState(at);
			if (part.getBlock() instanceof OpsTreePartBlock p && p.kind == kind && OpsTreePartBlock.masterPos(at, part).equals(pos)) {
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
		return shape(kind, 0, 0, 0);
	}
}
