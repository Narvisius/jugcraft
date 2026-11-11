package io.github.jimbozoomer.jugcraft.nuclear;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The medium plant's main transformer yard and switchyard working (transformer yard and switchyard build, 20 October
 * 2026; claude/jugcraft-transformer-yard-proposal.md): the transformers' heating and cooling, finding a reactor's yard
 * on its generator's bus, the yards finding each other, the sites' points in the world.
 *
 * <p>Each transformer's top oil and winding hot spot follow its load as IEEE C57.91's exponential model does (top-oil
 * rise over ambient = rated rise x ((K^2 R + 1) / (R + 1))^n, the winding's gradient over the oil = rated gradient x
 * K^1.6), with the rated rises of the cooling it has running: its fan groups start in stages as it warms (group 1 at
 * 65 C top oil or 75 C winding, group 2 at 75 / 85 C, each stopping 5 C lower). HI TEMP alarms at 95 / 110 C; it trips
 * (its 86 lockout) at 105 / 130 C and resets once it has cooled to 80 / 95 C. The game's time runs fast: the oil
 * settles in minutes, not hours.
 */
public final class Yards {
	/** How far a reactor's generator or a yard looks for the other yard (blocks). */
	public static final int REACH = 160;
	/** The oil's and the winding's time constants (seconds). */
	public static final float TAU_OIL = 180F, TAU_WDG = 20F;
	public static final float HI_OIL = 95F, HI_WDG = 110F, TRIP_OIL = 105F, TRIP_WDG = 130F, RESET_OIL = 80F, RESET_WDG = 95F;

	private Yards() {
	}

	/** A transformer's kind: its rated top-oil rises and winding gradients by the cooling running, losses ratio R. */
	public enum Kind {
		// MT-1A/B/C: OFAF 400 MVA, two groups (pumps with group 1); full load at full cooling: top oil 71 C, winding 84 C.
		GSU(new float[] {101, 64, 46}, new float[] {18, 15, 13}, 6),
		// UAT-1A/B: ONAN/ONAF 36/48 MVA, one fan stage; the house load (30 MVA each) with fans: 58 C, winding 77 C.
		UAT(new float[] {100, 63}, new float[] {48, 41}, 5),
		// SAT-1: ONAN/ONAF/ONAF 60/80/100 MVA; lightly loaded about 46 C, the house load (60 MVA) with group 1 about 63 C.
		SAT(new float[] {105, 75, 55}, new float[] {30, 24, 20}, 5);

		final float[] rise, gradient;
		final float losses;

		Kind(float[] rise, float[] gradient, float losses) {
			this.rise = rise;
			this.gradient = gradient;
			this.losses = losses;
		}

		public int stages() {
			return rise.length - 1;
		}
	}

	/** One transformer's temperatures, its cooling stage and its lockout. */
	public static final class Unit {
		public final Kind kind;
		public float oil = 25, wdg = 25, oilMax = 25, wdgMax = 25;
		public int stage;
		public boolean tripped, energized;
		public float load;

		public Unit(Kind kind) {
			this.kind = kind;
		}

		/** A second: energized carrying k of its rating, in the ambient's air. */
		public void second(float ambient, float k, boolean energized) {
			this.energized = energized;
			this.load = energized ? k : 0;
			float rise = 0, grad = 0;
			if (energized) {
				double r = kind.losses;
				rise = (float) (kind.rise[stage] * Math.pow((k * k * r + 1) / (r + 1), 0.9));
				grad = (float) (kind.gradient[stage] * Math.pow(Math.max(0, k), 1.6));
			}
			oil += (ambient + rise - oil) * (float) (1 - Math.exp(-1 / TAU_OIL));
			wdg += (oil + grad - wdg) * (float) (1 - Math.exp(-1 / TAU_WDG));
			oilMax = Math.max(oilMax, oil);
			wdgMax = Math.max(wdgMax, wdg);
			// The cooling groups, in stages (AUTO).
			if (stage < kind.stages() && (oil >= 65 + 10 * stage || wdg >= 75 + 10 * stage)) {
				stage++;
			} else if (stage > 0 && oil < 50 + 10 * stage && wdg < 60 + 10 * stage) {
				stage--;
			}
			if (oil >= TRIP_OIL || wdg >= TRIP_WDG) {
				tripped = true;
			} else if (tripped && oil < RESET_OIL && wdg < RESET_WDG) {
				tripped = false;
			}
		}

		public boolean hot() {
			return oil >= HI_OIL || wdg >= HI_WDG;
		}

		/** The magnetic oil level gauge: the oil swells as it warms. */
		public float level() {
			return Math.max(45, Math.min(75, 50 + (oil - 20) * 0.2F));
		}

		/** The drag hands put back to the needles (at dawn, the shift's round). */
		public void resetDrag() {
			oilMax = oil;
			wdgMax = wdg;
		}

		void save(net.minecraft.world.level.storage.ValueOutput out, String p) {
			out.putFloat(p + "oil", oil);
			out.putFloat(p + "wdg", wdg);
			out.putFloat(p + "oil_max", oilMax);
			out.putFloat(p + "wdg_max", wdgMax);
			out.putInt(p + "stage", stage);
			out.putBoolean(p + "tripped", tripped);
			out.putBoolean(p + "energized", energized);
			out.putFloat(p + "load", load);
		}

		void load(net.minecraft.world.level.storage.ValueInput in, String p) {
			oil = in.getFloatOr(p + "oil", 25);
			wdg = in.getFloatOr(p + "wdg", 25);
			oilMax = in.getFloatOr(p + "oil_max", oil);
			wdgMax = in.getFloatOr(p + "wdg_max", wdg);
			stage = in.getIntOr(p + "stage", 0);
			tripped = in.getBooleanOr(p + "tripped", false);
			energized = in.getBooleanOr(p + "energized", false);
			load = in.getFloatOr(p + "load", 0);
		}
	}

	/** The air's temperature in the yard (deg C): 25, 4 warmer mid-afternoon and cooler before dawn. */
	public static float ambient(Level level) {
		long t = Math.floorMod(level.getOverworldClockTime(), 24000L);
		return 25F + 4F * (float) Math.cos(2 * Math.PI * (t - 8000) / 24000.0);
	}

	// ------------------------------------------------------------------ finding yards

	/** The nearest other yard of a kind (its controller loaded) within {@link #REACH}, or null. */
	public static @Nullable YardBlockEntity nearest(Level level, BlockPos from, YardBlock.Works works) {
		YardBlockEntity best = null;
		double bestD = (double) REACH * REACH;
		for (SiteRegistry.Placement p : SiteRegistry.placements(level, works.site)) {
			double d = p.anchor().distSqr(from);
			if (d <= bestD && level.isLoaded(p.anchor()) && SiteRegistry.anchorOf(level, p) instanceof YardBlockEntity yard
					&& yard.works() == works) {
				best = yard;
				bestD = d;
			}
		}
		return best;
	}

	/**
	 * The transformer yards on a reactor's generators' isolated phase bus: following the bus pipes from the generators,
	 * a yard whose own bus (one of its site's blocks) touches one (its anchors).
	 */
	public static List<BlockPos> onBus(Level level, EquipmentRegistry registry, List<BlockPos> turbines) {
		List<BlockPos> start = new ArrayList<>();
		for (BlockPos t : turbines) {
			for (BlockPos cell : ReactorUnit.outside(registry, t)) {
				if (ReactorUnit.isPipe(level, cell, PipeKind.BUS)) start.add(cell);
			}
		}
		return yardsFrom(level, start);
	}

	/** The transformer yards a run of bus pipes reaches (from the pipes given, following the pipes). */
	public static List<BlockPos> yardsFrom(Level level, List<BlockPos> pipes) {
		List<BlockPos> out = new ArrayList<>();
		Set<BlockPos> seen = new HashSet<>();
		ArrayDeque<BlockPos> queue = new ArrayDeque<>();
		for (BlockPos cell : pipes) {
			if (seen.add(cell)) queue.add(cell);
		}
		while (!queue.isEmpty() && seen.size() <= ReactorUnit.MAX_PIPES) {
			BlockPos pipe = queue.poll();
			for (Direction d : Direction.values()) {
				BlockPos next = pipe.relative(d);
				if (ReactorUnit.isPipe(level, next, PipeKind.BUS)) {
					if (seen.add(next)) queue.add(next.immutable());
					continue;
				}
				SiteRegistry.Hit hit = SiteRegistry.find(level, next);
				if (hit != null && hit.placement().data().key.equals(YardBlock.Works.YARD.site) && !out.contains(hit.placement().anchor())) {
					out.add(hit.placement().anchor().immutable());
				}
			}
		}
		return out;
	}

	// ------------------------------------------------------------------ the sites' points in the world

	/** A point of a site (its frame, as the site's live data gives them) in the world. */
	public static Vec3 world(BlockPos anchor, Direction facing, SiteData d, double x, double y, double z) {
		double lx = x - d.ax - 0.5, lz = z - d.az - 0.5;
		double wx, wz;
		switch (facing) {
			case WEST -> {
				wx = -lz;
				wz = lx;
			}
			case NORTH -> {
				wx = -lx;
				wz = -lz;
			}
			case EAST -> {
				wx = lz;
				wz = -lx;
			}
			default -> {
				wx = lx;
				wz = lz;
			}
		}
		return new Vec3(anchor.getX() + 0.5 + wx, anchor.getY() + (y - d.ay), anchor.getZ() + 0.5 + wz);
	}

	/** A direction of a site in the world (turned with it). */
	public static Vec3 turn(Direction facing, double x, double y, double z) {
		return switch (facing) {
			case WEST -> new Vec3(-z, y, x);
			case NORTH -> new Vec3(-x, y, -z);
			case EAST -> new Vec3(z, y, -x);
			default -> new Vec3(x, y, z);
		};
	}

	/** A list of site cells in the live data ([[x, y, z], ...]) as world blocks. */
	public static List<BlockPos> cells(SiteRegistry.Placement p, @Nullable JsonElement list) {
		List<BlockPos> out = new ArrayList<>();
		if (list == null || !list.isJsonArray()) return out;
		for (JsonElement e : list.getAsJsonArray()) {
			JsonArray a = e.getAsJsonArray();
			out.add(p.world(a.get(0).getAsInt(), a.get(1).getAsInt(), a.get(2).getAsInt()));
		}
		return out;
	}
}
