package io.github.jimbozoomer.jugcraft.nuclear;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * How much gamma radiation gets through blocks (docs/features/nuclear.md, round 14: "realistic-ish"). Each block has a
 * factor: the share let through by a whole block's thickness (a metre). Radiation crossing part of a block, or a
 * block only partly filled (a slab, a door), is cut by that factor to the power of the distance it travels through
 * the solid, as real shielding falls off exponentially with thickness.
 * <ul>
 * <li>lead and shielding concrete, uranium metal and depleted uranium: 0.1% (lead glass 1%);</li>
 * <li>metal blocks: 1%; stone, concrete, brick, ore and other hard blocks: 10%;</li>
 * <li>dirt, sand, gravel, clay: 20%; glass and water: 30%; wood: 50%; wool, leaves, moss and the like: 85%.</li>
 * </ul>
 * A real metre of stone stops nearly all of it; these keep ore findable through a block or two of rock.
 */
public final class Shielding {
	public static final double LEAD = 0.001, LEAD_GLASS = 0.01, METAL = 0.01, STONE = 0.1, SOIL = 0.2, GLASS = 0.3,
			WATER = 0.3, LAVA = 0.1, WOOD = 0.5, LIGHT = 0.85, EQUIPMENT = 0.5, OTHER = 0.3;
	/** Below this nothing measurable is left. */
	static final double GONE = 1e-9;
	private static final Map<BlockState, Double> CACHE = new ConcurrentHashMap<>();
	private static volatile Map<Block, Double> special;

	private Shielding() {
	}

	/** Blocks whose factor is set by name (they are not in a tag): dense radioactive metals, lead glass, the door. */
	private static Map<Block, Double> special() {
		Map<Block, Double> s = special;
		if (s == null) {
			Map<Block, Double> m = new java.util.HashMap<>();
			for (var e : Map.of("uranium_block", LEAD, "raw_uranium_block", METAL, "depleted_uranium_armor", LEAD,
					"depleted_uranium_armor_stairs", LEAD, "depleted_uranium_armor_slab", LEAD, "lead_glass", LEAD_GLASS,
					"lead_door", LEAD, "lead_locker", METAL, "waste_drum", STONE, "lead_pig", METAL).entrySet()) {
				BuiltInRegistries.BLOCK.getOptional(Jugcraft.id(e.getKey())).ifPresent(b -> m.put(b, e.getValue()));
			}
			special = s = m;
		}
		return s;
	}

	/** The share let through by a block's whole cell (a metre straight through it). */
	public static double factor(BlockState state) {
		if (state.isAir()) {
			return 1;
		}
		Double cached = CACHE.get(state);
		if (cached != null) {
			return cached;
		}
		double f = compute(state);
		CACHE.put(state, f);
		return f;
	}

	private static double compute(BlockState state) {
		FluidState fluid = state.getFluidState();
		double liquid = fluid.is(FluidTags.WATER) ? WATER : fluid.is(FluidTags.LAVA) ? LAVA : 1;
		Block block = state.getBlock();
		if (block instanceof EquipmentFrameBlock) {
			// The equipment's own cells: big machines, mostly hollow shells and water.
			return EQUIPMENT * liquid;
		}
		Double named = special().get(block);
		if (named != null && block instanceof DoorBlock && state.getValue(DoorBlock.OPEN)) {
			return liquid;  // an open door lets everything by
		}
		VoxelShape shape;
		try {
			shape = state.getCollisionShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
		} catch (RuntimeException e) {
			// A block whose shape needs the world round it: take it as a full block of what it is made of.
			shape = net.minecraft.world.phys.shapes.Shapes.block();
		}
		if (named != null) {
			// Shaped like a door or a slab, it still is the full thickness of its material where it stands.
			return liquid * (block instanceof DoorBlock ? named : Math.pow(named, fill(shape)));
		}
		if (shape.isEmpty()) {
			return liquid;
		}
		double base;
		if (state.is(JugcraftNuclear.SHIELDING)) {
			base = LEAD;
		} else {
			base = material(state);
		}
		return liquid * Math.pow(base, fill(shape));
	}

	/** A full block's factor from what it is made of (tags first, then the sound it makes, then how hard it is). */
	private static double material(BlockState state) {
		SoundType sound = state.getSoundType();
		if (sound == SoundType.METAL || sound == SoundType.IRON || sound == SoundType.NETHERITE_BLOCK || sound == SoundType.COPPER
				|| sound == SoundType.COPPER_BULB || sound == SoundType.ANVIL || sound == SoundType.HEAVY_CORE || sound == SoundType.LODESTONE
				|| sound == SoundType.ANCIENT_DEBRIS) {
			return METAL;
		}
		if (sound == SoundType.GLASS) {
			return GLASS;
		}
		if (state.is(BlockTags.WOOL) || state.is(BlockTags.LEAVES) || state.is(BlockTags.WOOL_CARPETS) || sound == SoundType.WOOL
				|| sound == SoundType.MOSS || sound == SoundType.SNOW || sound == SoundType.POWDER_SNOW || sound == SoundType.SPONGE
				|| sound == SoundType.WET_SPONGE || sound == SoundType.SCAFFOLDING || sound == SoundType.COBWEB) {
			return LIGHT;
		}
		if (state.is(BlockTags.LOGS) || state.is(BlockTags.PLANKS) || state.is(BlockTags.MINEABLE_WITH_AXE)) {
			return WOOD;
		}
		if (state.is(BlockTags.DIRT) || state.is(BlockTags.SAND) || state.is(BlockTags.MINEABLE_WITH_SHOVEL)) {
			return SOIL;
		}
		if (state.is(BlockTags.BASE_STONE_OVERWORLD) || state.is(BlockTags.BASE_STONE_NETHER) || state.is(BlockTags.MINEABLE_WITH_PICKAXE)) {
			return STONE;
		}
		if (state.is(BlockTags.MINEABLE_WITH_HOE)) {
			return 0.7;
		}
		return OTHER;
	}

	/** How much of its cell a shape fills (0..1). */
	static double fill(VoxelShape shape) {
		if (shape.isEmpty()) {
			return 0;
		}
		double v = 0;
		for (AABB box : shape.toAabbs()) {
			v += Math.max(0, Math.min(1, box.maxX) - Math.max(0, box.minX)) * Math.max(0, Math.min(1, box.maxY) - Math.max(0, box.minY))
					* Math.max(0, Math.min(1, box.maxZ) - Math.max(0, box.minZ));
		}
		return Math.min(1, v);
	}

	/** Block states by position (RadiationField.states reads them without loading chunks). */
	public interface States {
		BlockState at(int x, int y, int z);
	}

	/** Which cells a ray should pass through without being cut (the source's own block, and what else asks). */
	public interface Skip {
		boolean skip(BlockPos cell, BlockState state);
	}

	/**
	 * The share of radiation from {@code from} that reaches {@code to} through the blocks between (1 = nothing in the
	 * way). Walks the cells the straight line crosses (a voxel walk) and cuts it by each cell's factor to the power of
	 * the length of line inside it. The cell the line ends in (the eye) is not counted, nor cells {@code skip} names.
	 */
	public static double through(States level, Vec3 from, Vec3 to, Skip skip) {
		double dx = to.x - from.x, dy = to.y - from.y, dz = to.z - from.z;
		double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
		if (length < 1e-6) {
			return 1;
		}
		int x = floor(from.x), y = floor(from.y), z = floor(from.z);
		int ex = floor(to.x), ey = floor(to.y), ez = floor(to.z);
		int sx = dx > 0 ? 1 : -1, sy = dy > 0 ? 1 : -1, sz = dz > 0 ? 1 : -1;
		// Distance along the line (0..1 of it) to the next boundary on each axis, and between boundaries.
		double tdx = dx == 0 ? Double.MAX_VALUE : Math.abs(1 / dx), tdy = dy == 0 ? Double.MAX_VALUE : Math.abs(1 / dy), tdz = dz == 0 ? Double.MAX_VALUE : Math.abs(1 / dz);
		double tx = dx == 0 ? Double.MAX_VALUE : ((dx > 0 ? x + 1 - from.x : from.x - x) * tdx);
		double ty = dy == 0 ? Double.MAX_VALUE : ((dy > 0 ? y + 1 - from.y : from.y - y) * tdy);
		double tz = dz == 0 ? Double.MAX_VALUE : ((dz > 0 ? z + 1 - from.z : from.z - z) * tdz);
		double t = 0, factor = 1;
		BlockPos.MutableBlockPos cell = new BlockPos.MutableBlockPos();
		for (int guard = 0; guard < 4096; guard++) {
			double next = Math.min(1, Math.min(tx, Math.min(ty, tz)));
			boolean last = x == ex && y == ey && z == ez;
			if (last) {
				break;
			}
			double inside = (next - t) * length;
			if (inside > 1e-9) {
				cell.set(x, y, z);
				BlockState state = level.at(x, y, z);
				if (!state.isAir() && (skip == null || !skip.skip(cell, state))) {
					double f = factor(state);
					if (f < 1) {
						factor *= Math.pow(f, inside);
						if (factor < GONE) {
							return 0;
						}
					}
				}
			}
			if (next >= 1) {
				break;
			}
			t = next;
			if (tx <= ty && tx <= tz) {
				x += sx;
				tx += tdx;
			} else if (ty <= tz) {
				y += sy;
				ty += tdy;
			} else {
				z += sz;
				tz += tdz;
			}
		}
		return factor;
	}

	/**
	 * What a ray from {@code from} to {@code to} passes through (tests, to say where radiation gets out): each cell that
	 * cuts it, as "x,y,z block factor^length", at most {@code max} of them.
	 */
	public static java.util.List<String> trace(States level, Vec3 from, Vec3 to, Skip skip, int max) {
		java.util.List<String> out = new java.util.ArrayList<>();
		Vec3 d = to.subtract(from);
		double length = d.length();
		int steps = (int) Math.ceil(length * 8);
		BlockPos last = null;
		for (int i = 0; i <= steps && out.size() < max; i++) {
			BlockPos cell = BlockPos.containing(from.add(d.scale(i / (double) steps)));
			if (cell.equals(last)) {
				continue;
			}
			last = cell;
			BlockState state = level.at(cell.getX(), cell.getY(), cell.getZ());
			if (state.isAir() || skip != null && skip.skip(cell, state)) {
				continue;
			}
			double f = factor(state);
			out.add(cell.toShortString() + " " + BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath() + " " + String.format(java.util.Locale.ROOT, "%.3g", f));
		}
		return out;
	}

	private static int floor(double v) {
		return (int) Math.floor(v);
	}

	/** Forgets cached factors (after data packs reload tags). */
	public static void clear() {
		CACHE.clear();
		special = null;
	}
}
