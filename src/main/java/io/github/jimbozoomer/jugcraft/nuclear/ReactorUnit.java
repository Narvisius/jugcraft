package io.github.jimbozoomer.jugcraft.nuclear;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

/**
 * Which equipment a reactor vessel is joined to, worked out from the world (docs/features/nuclear.md, "What makes a
 * working unit"): the Reactor Coolant Pipe from the vessel to steam generators, coolant pumps and a pressurizer (or
 * equipment touching the vessel directly); the Insulated Steam Line from the steam generators to a turbine-generator,
 * a feedwater pump and a condenser; and a condenser and a main transformer touching the turbine-generator (a
 * condenser can be on the steam line instead). Looked at again only when equipment or a pipe changes.
 */
public final class ReactorUnit {
	/** Pipe blocks one search will follow. */
	public static final int MAX_PIPES = 4_096;

	public final List<BlockPos> steamGenerators = new ArrayList<>();
	public final List<BlockPos> pumps = new ArrayList<>();
	public final List<BlockPos> pressurizers = new ArrayList<>();
	public final List<BlockPos> turbines = new ArrayList<>();
	public final List<BlockPos> feedwaterPumps = new ArrayList<>();
	public final List<BlockPos> condensers = new ArrayList<>();
	public final List<BlockPos> transformers = new ArrayList<>();
	/** A small modular reactor's unit: its small turbine-generators (each its own condenser and transformer). */
	public final List<BlockPos> smallTurbines = new ArrayList<>();
	public boolean module;

	/** Working loops: a steam generator with a coolant pump each, at most four. */
	public int loops() {
		if (module) {
			return smallTurbines.isEmpty() ? 0 : 1;
		}
		return Math.min(4, Math.min(steamGenerators.size(), pumps.size()));
	}

	/** Share of the rated heat the unit can carry away (before cooling water). */
	public double cooling() {
		if (module) {
			// Natural circulation inside the module: it only needs somewhere for its steam to go.
			return smallTurbines.isEmpty() ? 0 : 1;
		}
		if (pressurizers.isEmpty() || feedwaterPumps.isEmpty() || condensers.isEmpty()) {
			// No pressurizer: the coolant boils. No feedwater pump or condenser: the steam generators boil dry.
			return 0;
		}
		return loops() / 4.0;
	}

	public boolean steamToPower() {
		if (module) {
			return !smallTurbines.isEmpty();
		}
		return !turbines.isEmpty() && !transformers.isEmpty() && cooling() > 0;
	}

	public boolean formed() {
		return loops() > 0 && cooling() > 0 && steamToPower();
	}

	/** What is missing, as translation keys (the console lists them). */
	/** The most electric power the unit's generators can make (JE/t): each turbine-generator takes a standard unit's output. */
	public long generatorLimit() {
		if (module) {
			return smallTurbines.size() * ReactorCore.Design.SMALL_MODULAR.ratedJe();
		}
		return turbines.size() * ReactorCore.RATED_JE;
	}

	public List<String> missing() {
		List<String> out = new ArrayList<>();
		if (module) {
			if (smallTurbines.isEmpty()) out.add("small_turbine");
			return out;
		}
		if (steamGenerators.isEmpty()) out.add("steam_generator");
		if (pumps.isEmpty()) out.add("coolant_pump");
		if (pressurizers.isEmpty()) out.add("pressurizer");
		if (turbines.isEmpty()) out.add("turbine");
		if (feedwaterPumps.isEmpty()) out.add("feedwater_pump");
		if (condensers.isEmpty()) out.add("condenser");
		if (transformers.isEmpty()) out.add("transformer");
		return out;
	}

	public static ReactorUnit find(Level level, BlockPos vessel) {
		EquipmentRegistry registry = EquipmentRegistry.of(level);
		ReactorUnit unit = new ReactorUnit();
		EquipmentRegistry.Entry self = registry.at(vessel);
		if (self != null && self.kind() == EquipmentKind.SMALL_MODULAR_REACTOR) {
			unit.module = true;
			for (BlockPos anchor : joined(level, registry, List.of(vessel), PipeKind.STEAM)) {
				EquipmentRegistry.Entry e = registry.at(anchor);
				if (e != null && e.kind() == EquipmentKind.SMALL_TURBINE) {
					unit.smallTurbines.add(anchor);
				}
			}
			// The small turbine-generator is its own condenser and transformer.
			unit.condensers.addAll(unit.smallTurbines);
			unit.transformers.addAll(unit.smallTurbines);
			return unit;
		}
		Set<BlockPos> primary = joined(level, registry, List.of(vessel), PipeKind.COOLANT);
		for (BlockPos anchor : primary) {
			EquipmentRegistry.Entry e = registry.at(anchor);
			if (e == null) continue;
			switch (e.kind()) {
				case STEAM_GENERATOR -> unit.steamGenerators.add(anchor);
				case COOLANT_PUMP -> unit.pumps.add(anchor);
				case PRESSURIZER -> unit.pressurizers.add(anchor);
				default -> {
				}
			}
		}
		if (!unit.steamGenerators.isEmpty()) {
			for (BlockPos anchor : joined(level, registry, unit.steamGenerators, PipeKind.STEAM)) {
				EquipmentRegistry.Entry e = registry.at(anchor);
				if (e == null) continue;
				switch (e.kind()) {
					case TURBINE_GENERATOR -> unit.turbines.add(anchor);
					case FEEDWATER_PUMP -> unit.feedwaterPumps.add(anchor);
					case CONDENSER -> unit.condensers.add(anchor);
					default -> {
					}
				}
			}
		}
		for (BlockPos turbine : unit.turbines) {
			for (BlockPos anchor : touching(level, registry, turbine)) {
				EquipmentRegistry.Entry e = registry.at(anchor);
				if (e == null) continue;
				if (e.kind() == EquipmentKind.CONDENSER && !unit.condensers.contains(anchor)) unit.condensers.add(anchor);
				if (e.kind() == EquipmentKind.MAIN_TRANSFORMER && !unit.transformers.contains(anchor)) unit.transformers.add(anchor);
			}
		}
		return unit;
	}

	/**
	 * Every piece of equipment joined to {@code from} by pipes of one kind (following the pipes, and equipment that
	 * takes that pipe touching any of it), excluding the starting equipment itself.
	 */
	static Set<BlockPos> joined(Level level, EquipmentRegistry registry, List<BlockPos> from, PipeKind kind) {
		Set<BlockPos> found = new LinkedHashSet<>();
		Set<BlockPos> seenPipes = new HashSet<>();
		ArrayDeque<BlockPos> pipes = new ArrayDeque<>();
		for (BlockPos anchor : from) {
			for (BlockPos cell : outside(registry, anchor)) {
				if (isPipe(level, cell, kind) && seenPipes.add(cell)) {
					pipes.add(cell);
				}
			}
			for (BlockPos other : touching(level, registry, anchor)) {
				EquipmentRegistry.Entry e = registry.at(other);
				if (e != null && e.kind().takes(kind)) {
					found.add(other);
				}
			}
		}
		while (!pipes.isEmpty() && seenPipes.size() <= MAX_PIPES) {
			BlockPos pipe = pipes.poll();
			for (Direction d : Direction.values()) {
				BlockPos next = pipe.relative(d);
				if (isPipe(level, next, kind)) {
					if (seenPipes.add(next)) {
						pipes.add(next);
					}
					continue;
				}
				BlockPos anchor = registry.anchorOf(next);
				if (anchor != null) {
					EquipmentRegistry.Entry e = registry.at(anchor);
					if (e != null && e.kind().takes(kind)) {
						found.add(anchor);
					}
				}
			}
		}
		found.removeAll(from);
		return found;
	}

	static boolean isPipe(Level level, BlockPos pos, PipeKind kind) {
		return level.getBlockState(pos).getBlock() instanceof NuclearPipeBlock pipe && pipe.kind == kind;
	}

	/** Every block next to the equipment's body (outside it). */
	static Set<BlockPos> outside(EquipmentRegistry registry, BlockPos anchor) {
		EquipmentRegistry.Entry e = registry.at(anchor);
		Set<BlockPos> out = new HashSet<>();
		if (e == null) {
			return out;
		}
		Set<BlockPos> body = new HashSet<>();
		for (BlockPos local : e.kind().solidCells()) {
			body.add(anchor.offset(EquipmentKind.toWorld(local, e.facing())));
		}
		body.add(anchor);
		for (BlockPos cell : body) {
			for (Direction d : Direction.values()) {
				BlockPos next = cell.relative(d);
				if (!body.contains(next)) {
					out.add(next);
				}
			}
		}
		return out;
	}

	/** Other equipment touching this one. */
	static Set<BlockPos> touching(Level level, EquipmentRegistry registry, BlockPos anchor) {
		Set<BlockPos> out = new LinkedHashSet<>();
		for (BlockPos cell : outside(registry, anchor)) {
			BlockPos other = registry.anchorOf(cell);
			if (other != null && !other.equals(anchor)) {
				out.add(other);
			}
		}
		return out;
	}
}
