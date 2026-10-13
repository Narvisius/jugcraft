package io.github.jimbozoomer.jugcraft.machine.form;

import io.github.jimbozoomer.jugcraft.chemistry.FluidMachineSpec;
import io.github.jimbozoomer.jugcraft.chemistry.FluidRecipe;
import io.github.jimbozoomer.jugcraft.machine.Footprint;
import io.github.jimbozoomer.jugcraft.machine.MachineKind;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.material.Fluid;
import org.jspecify.annotations.Nullable;

/**
 * The physical form of an industrial machine (docs/features/industrial-machine-foundation.md): an immutable,
 * validated description of the blocks it fills inside its envelope, where pipes, cables and conveyors may join it,
 * the reusable tools it holds and the processes it can run. Its family, a {@link MachineKind}, owns the recipes and
 * the energy figures; the form owns the dimensions, ports, sockets, capabilities and operating profile, so a new
 * physical form can run its family's recipes without a new backend.
 *
 * <p>An envelope is 2 to 6 blocks on every axis, so at most {@link #MAX_POSITIONS} positions are examined when a form
 * is placed or checked. Layers are given bottom first; a layer is {@code depth} rows from the front to the back, and
 * a row is {@code width} characters from the viewer's left to right, standing in front of the machine: {@code C} the
 * controller (a structural block on the ground layer, where the machine's block entity lives), {@code #} another
 * structural block, {@code ~} clearance for a moving part and {@code .} open access space (see {@link FormCell}).
 *
 * <p>Item slots come in this order: inputs, outputs, tool sockets, then the two upgrade slots if it takes upgrades.
 * Tanks are inputs first, then outputs, and are saved by their role names, never by their positions, so adding a
 * tank to a later version of a form cannot move one tank's contents into another.
 *
 * <p>A form either processes or generates. A processing form declares capabilities its family's recipes ask for and
 * takes power at its {@link FormPort.Kind#ENERGY_IN} ports. A generator form (a generator family) declares the
 * {@link FormFuel fuels} it burns instead: one fuel tank, no products, slots or upgrades, and its power leaves at
 * {@link FormPort.Kind#ENERGY_OUT} ports; it never takes power in.
 */
public final class MachineForm {
	public static final int MIN_EXTENT = 2;
	public static final int MAX_EXTENT = 6;
	/** The most positions an envelope can have: a full 6x6x6. */
	public static final int MAX_POSITIONS = MAX_EXTENT * MAX_EXTENT * MAX_EXTENT;
	/** Upgrade slots on a form that takes speed and efficiency cards. */
	public static final int UPGRADE_SLOTS = 2;

	private final Identifier id;
	private final int version;
	private final MachineKind family;
	private final int width;
	private final int depth;
	private final int height;
	private final FormCell[] cells;
	private final int controller;
	private final Footprint footprint;
	private final int[] partCells;
	private final int[] cellParts;
	private final int[] clearance;
	private final List<FormPort> ports;
	private final @Nullable FormPort[] portsByFace;
	private final List<ToolSocket> sockets;
	private final Set<String> capabilities;
	private final OperatingProfile profile;
	private final FluidMachineSpec tanks;
	private final List<String> tankRoles;
	private final Map<String, String> roleAliases;
	private final int itemInputs;
	private final int itemOutputs;
	private final boolean upgrades;
	private final int warmupTicks;
	private final List<FormFuel> fuels;

	private MachineForm(Builder builder) {
		this.id = builder.id;
		this.version = builder.version;
		this.family = builder.family;
		this.profile = builder.profile;
		this.upgrades = builder.upgrades;
		this.warmupTicks = builder.warmup;
		this.fuels = List.copyOf(builder.fuels);
		if (builder.layers.isEmpty() || builder.layers.getFirst().length == 0) {
			throw invalid("has no layers");
		}
		this.height = builder.layers.size();
		this.depth = builder.layers.getFirst().length;
		this.width = builder.layers.getFirst()[0].length();
		for (int extent : new int[] {width, depth, height}) {
			if (extent < MIN_EXTENT || extent > MAX_EXTENT) {
				throw invalid("must be " + MIN_EXTENT + " to " + MAX_EXTENT + " blocks on every axis, not "
						+ width + "x" + depth + "x" + height);
			}
		}
		boolean generator = !fuels.isEmpty();
		if (generator ? !family.isGenerator() : !family.isFluidProcessor()) {
			throw invalid(generator ? "burns fuel, so needs a generator family, not " + family.id
					: "needs a fluid-processing family for its recipes, not " + family.id);
		}

		// The occupancy mask, and the one controller on the ground layer.
		this.cells = new FormCell[width * depth * height];
		int found = -1;
		for (int layer = 0; layer < height; layer++) {
			String[] rows = builder.layers.get(layer);
			if (rows.length != depth) {
				throw invalid("layer " + layer + " has " + rows.length + " rows, not " + depth);
			}
			for (int row = 0; row < depth; row++) {
				if (rows[row].length() != width) {
					throw invalid("layer " + layer + " row " + row + " is " + rows[row].length() + " wide, not " + width);
				}
				for (int column = 0; column < width; column++) {
					char symbol = rows[row].charAt(column);
					int index = cellIndex(column, row, layer);
					cells[index] = FormCell.of(symbol);
					if (symbol == 'C') {
						if (found >= 0) {
							throw invalid("has more than one controller (C)");
						}
						found = index;
					}
				}
			}
		}
		if (found < 0) {
			throw invalid("has no controller (C)");
		}
		if (layer(found) != 0) {
			throw invalid("must have its controller on the ground layer, where a player can reach it");
		}
		this.controller = found;

		// The structure and clearance must reach every face of the stated envelope, so its size is honest.
		int[] min = {width, depth, height};
		int[] max = {-1, -1, -1};
		int structure = 0;
		List<Integer> clear = new ArrayList<>();
		for (int index = 0; index < cells.length; index++) {
			if (cells[index] == FormCell.ACCESS) {
				continue;
			}
			int[] at = {column(index), row(index), layer(index)};
			for (int axis = 0; axis < 3; axis++) {
				min[axis] = Math.min(min[axis], at[axis]);
				max[axis] = Math.max(max[axis], at[axis]);
			}
			if (cells[index] == FormCell.STRUCTURE) {
				structure++;
			} else {
				clear.add(index);
			}
		}
		if (min[0] != 0 || min[1] != 0 || min[2] != 0 || max[0] != width - 1 || max[1] != depth - 1 || max[2] != height - 1) {
			throw invalid("does not fill its stated " + width + "x" + depth + "x" + height + " envelope");
		}
		if (structure < 2) {
			throw invalid("needs at least two structural blocks");
		}
		this.clearance = clear.stream().mapToInt(Integer::intValue).toArray();

		// Parts: the controller is part 0, then every other structural block in order. Every part must touch the
		// rest, so breaking or checking the machine never leaves a floating piece.
		this.partCells = new int[structure];
		this.cellParts = new int[cells.length];
		Arrays.fill(cellParts, -1);
		partCells[0] = controller;
		cellParts[controller] = 0;
		int next = 1;
		for (int index = 0; index < cells.length; index++) {
			if (cells[index] == FormCell.STRUCTURE && index != controller) {
				partCells[next] = index;
				cellParts[index] = next++;
			}
		}
		if (connected() != structure) {
			throw invalid("has structural blocks that do not touch the rest of the machine");
		}
		Vec3i[] offsets = new Vec3i[structure];
		for (int part = 0; part < structure; part++) {
			offsets[part] = offset(partCells[part]);
		}
		this.footprint = Footprint.of(offsets);

		// Tanks, by role, and the item slots.
		List<String> roles = new ArrayList<>(builder.inputTanks);
		roles.addAll(builder.outputTanks);
		if (new HashSet<>(roles).size() != roles.size()) {
			throw invalid("has two tanks with the same role");
		}
		this.tankRoles = List.copyOf(roles);
		this.tanks = new FluidMachineSpec(Collections.nCopies(builder.inputTanks.size(), profile.bufferMb()),
				Collections.nCopies(builder.outputTanks.size(), profile.bufferMb()), builder.itemInputs, builder.itemOutputs);
		for (Map.Entry<String, String> alias : builder.aliases.entrySet()) {
			if (roles.contains(alias.getKey()) || !roles.contains(alias.getValue())) {
				throw invalid("has an alias " + alias.getKey() + " -> " + alias.getValue() + " that is not an old name of a current tank");
			}
		}
		this.roleAliases = Collections.unmodifiableMap(new LinkedHashMap<>(builder.aliases));
		if (builder.itemInputs < 0 || builder.itemOutputs < 0) {
			throw invalid("cannot have a negative number of item slots");
		}
		this.itemInputs = builder.itemInputs;
		this.itemOutputs = builder.itemOutputs;
		Set<String> socketNames = new HashSet<>();
		for (ToolSocket socket : builder.sockets) {
			if (!socketNames.add(socket.name())) {
				throw invalid("has two tool sockets named " + socket.name());
			}
		}
		this.sockets = List.copyOf(builder.sockets);
		if (generator) {
			// A generator burns from one fuel tank and makes only power.
			if (builder.inputTanks.size() != 1 || !builder.outputTanks.isEmpty() || itemInputs > 0 || itemOutputs > 0
					|| !sockets.isEmpty() || upgrades || !builder.capabilities.isEmpty()) {
				throw invalid("is a generator: one fuel tank and no other tanks, slots, sockets, upgrades or capabilities");
			}
			Set<Identifier> burnt = new HashSet<>();
			for (FormFuel fuel : fuels) {
				if (!burnt.add(fuel.fluid())) {
					throw invalid("lists fuel " + fuel.fluid() + " twice");
				}
				if (fuel.jePerMb() <= 0 || fuel.jePerTick() <= 0 || fuel.jePerTick() > family.capacity) {
					throw invalid("burns " + fuel.fluid() + " for nothing, or makes more a tick than it can store");
				}
			}
		} else if (builder.capabilities.isEmpty()) {
			throw invalid("runs no processes: give it at least one capability");
		}
		this.capabilities = Collections.unmodifiableSet(new LinkedHashSet<>(builder.capabilities));
		if (warmupTicks < 0) {
			throw invalid("cannot warm up for a negative time");
		}

		// Ports: on a structural block, on a face that looks out of the machine, reaching something that exists.
		this.portsByFace = new FormPort[structure * FormSide.values().length];
		Set<String> portNames = new HashSet<>();
		for (FormPort port : builder.ports) {
			if (!portNames.add(port.name())) {
				throw invalid("has two ports named " + port.name());
			}
			if (!inside(port.column(), port.row(), port.layer())
					|| cells[cellIndex(port.column(), port.row(), port.layer())] != FormCell.STRUCTURE) {
				throw invalid("has port " + port.name() + " off its structure");
			}
			int column = port.column() + port.side().column;
			int row = port.row() + port.side().row;
			int layer = port.layer() + port.side().layer;
			if (inside(column, row, layer) && cells[cellIndex(column, row, layer)] != FormCell.ACCESS) {
				throw invalid("has port " + port.name() + " facing into its own " + cells[cellIndex(column, row, layer)].name().toLowerCase());
			}
			int target = port.target();
			boolean reaches = switch (port.kind()) {
				case FLUID_IN -> target >= 0 && target < builder.inputTanks.size();
				case FLUID_OUT -> target >= 0 && target < builder.outputTanks.size();
				case ITEM_IN -> itemInputs > 0 && (target == FormPort.ALL || target >= 0 && target < itemInputs);
				case ITEM_OUT -> itemOutputs > 0 && (target == FormPort.ALL || target >= 0 && target < itemOutputs);
				case ENERGY_IN -> !generator && family.usesPower();
				case ENERGY_OUT -> generator;
			};
			if (!reaches) {
				throw invalid("has port " + port.name() + " reaching nothing");
			}
			int face = cellParts[cellIndex(port.column(), port.row(), port.layer())] * FormSide.values().length + port.side().ordinal();
			if (portsByFace[face] != null) {
				throw invalid("has ports " + portsByFace[face].name() + " and " + port.name() + " on the same face");
			}
			portsByFace[face] = port;
		}
		this.ports = List.copyOf(builder.ports);
		if (generator && (ports.stream().noneMatch(port -> port.kind() == FormPort.Kind.FLUID_IN)
				|| ports.stream().noneMatch(port -> port.kind() == FormPort.Kind.ENERGY_OUT))) {
			throw invalid("is a generator with no way for its fuel to come in or its power to go out");
		}
	}

	private IllegalArgumentException invalid(String problem) {
		return new IllegalArgumentException("Machine form " + id + " " + problem);
	}

	/** How many structural blocks join the controller through shared faces. */
	private int connected() {
		boolean[] seen = new boolean[cells.length];
		ArrayDeque<Integer> open = new ArrayDeque<>();
		open.add(controller);
		seen[controller] = true;
		int count = 0;
		while (!open.isEmpty()) {
			int index = open.poll();
			count++;
			for (FormSide side : FormSide.values()) {
				int column = column(index) + side.column;
				int row = row(index) + side.row;
				int layer = layer(index) + side.layer;
				if (inside(column, row, layer)) {
					int neighbour = cellIndex(column, row, layer);
					if (!seen[neighbour] && cells[neighbour] == FormCell.STRUCTURE) {
						seen[neighbour] = true;
						open.add(neighbour);
					}
				}
			}
		}
		return count;
	}

	private boolean inside(int column, int row, int layer) {
		return column >= 0 && column < width && row >= 0 && row < depth && layer >= 0 && layer < height;
	}

	public static Builder builder(Identifier id, MachineKind family) {
		return new Builder(id, family);
	}

	public Identifier id() {
		return id;
	}

	/** The saved layout version; raise it when tanks are renamed (see {@link Builder#alias}). */
	public int version() {
		return version;
	}

	/** The machine kind whose recipes and energy figures this form uses. */
	public MachineKind family() {
		return family;
	}

	public int width() {
		return width;
	}

	public int depth() {
		return depth;
	}

	public int height() {
		return height;
	}

	/** Positions in the envelope: width x depth x height. */
	public int positions() {
		return cells.length;
	}

	public int cellIndex(int column, int row, int layer) {
		return (layer * depth + row) * width + column;
	}

	public int column(int cell) {
		return cell % width;
	}

	public int row(int cell) {
		return cell / width % depth;
	}

	public int layer(int cell) {
		return cell / (width * depth);
	}

	public FormCell cell(int index) {
		return cells[index];
	}

	public int controllerCell() {
		return controller;
	}

	/**
	 * Where cell {@code cell} sits relative to the controller in a north-facing machine (x east, y up, z south), as
	 * {@link Footprint} offsets are written: columns run to the viewer's right, which is west.
	 */
	public Vec3i offset(int cell) {
		return new Vec3i(column(controller) - column(cell), layer(cell) - layer(controller), row(cell) - row(controller));
	}

	/** The world position of cell {@code cell} for a controller at {@code controllerPos} facing {@code facing}. */
	public BlockPos cellPos(BlockPos controllerPos, Direction facing, int cell) {
		return controllerPos.offset(Footprint.rotate(offset(cell), facing));
	}

	/** The structural blocks; part 0 is the controller. */
	public Footprint footprint() {
		return footprint;
	}

	public int partCell(int part) {
		return partCells[part];
	}

	/** The part a cell holds, or -1 for clearance and access cells. */
	public int cellPart(int cell) {
		return cellParts[cell];
	}

	/** The clearance cells, which must stay free. */
	public int[] clearanceCells() {
		return clearance.clone();
	}

	public List<FormPort> ports() {
		return ports;
	}

	/** The port on face {@code side} of part {@code part}, or null when that face is closed. */
	public @Nullable FormPort port(int part, FormSide side) {
		if (part < 0 || part >= partCells.length) {
			return null;
		}
		return portsByFace[part * FormSide.values().length + side.ordinal()];
	}

	public List<ToolSocket> sockets() {
		return sockets;
	}

	public Set<String> capabilities() {
		return capabilities;
	}

	/** Whether this form may run {@code recipe}: it must declare the recipe's capability. */
	public boolean runs(FluidRecipe recipe) {
		return recipe.machine() == family && capabilities.contains(recipe.capability());
	}

	/** Whether this is a generator form: it burns {@link #fuels()} and gives power out. */
	public boolean generator() {
		return !fuels.isEmpty();
	}

	/** What a generator form burns, in its declared order; empty for a processing form. */
	public List<FormFuel> fuels() {
		return fuels;
	}

	/** The fuel entry for {@code fluid}, or null when this form does not burn it. */
	public @Nullable FormFuel fuel(Fluid fluid) {
		for (FormFuel fuel : fuels) {
			if (fuel.is(fluid)) {
				return fuel;
			}
		}
		return null;
	}

	public OperatingProfile profile() {
		return profile;
	}

	/** The process tanks; each holds the profile's buffer. */
	public FluidMachineSpec tanks() {
		return tanks;
	}

	/** The role name of every tank, inputs first. */
	public List<String> tankRoles() {
		return tankRoles;
	}

	/** Old tank role names saved by earlier versions, and the role each now loads into. */
	public Map<String, String> roleAliases() {
		return roleAliases;
	}

	public int itemInputs() {
		return itemInputs;
	}

	public int itemOutputs() {
		return itemOutputs;
	}

	public int firstOutputSlot() {
		return itemInputs;
	}

	public int firstSocketSlot() {
		return itemInputs + itemOutputs;
	}

	public int firstUpgradeSlot() {
		return firstSocketSlot() + sockets.size();
	}

	public int upgradeSlots() {
		return upgrades ? UPGRADE_SLOTS : 0;
	}

	public int containerSize() {
		return firstUpgradeSlot() + upgradeSlots();
	}

	/**
	 * Paid ticks at the start of a batch begun from cold that the machine reports as warming. The owner chose heating
	 * included in the processing power: warming is part of a batch's paid work, never an extra charge.
	 */
	public int warmupTicks() {
		return warmupTicks;
	}

	@Override
	public String toString() {
		return "MachineForm[" + id + " " + width + "x" + depth + "x" + height + "]";
	}

	public static final class Builder {
		private final Identifier id;
		private final MachineKind family;
		private int version = 1;
		private final List<String[]> layers = new ArrayList<>();
		private OperatingProfile profile = OperatingProfile.ENTRY;
		private final List<String> inputTanks = new ArrayList<>();
		private final List<String> outputTanks = new ArrayList<>();
		private int itemInputs;
		private int itemOutputs;
		private final List<ToolSocket> sockets = new ArrayList<>();
		private final List<FormPort> ports = new ArrayList<>();
		private final Set<String> capabilities = new LinkedHashSet<>();
		private final Map<String, String> aliases = new LinkedHashMap<>();
		private boolean upgrades;
		private int warmup;
		private final List<FormFuel> fuels = new ArrayList<>();

		private Builder(Identifier id, MachineKind family) {
			this.id = id;
			this.family = family;
		}

		public Builder version(int version) {
			this.version = version;
			return this;
		}

		/** The next layer up: its rows from the front to the back (see {@link MachineForm}). */
		public Builder layer(String... rows) {
			layers.add(rows.clone());
			return this;
		}

		public Builder profile(OperatingProfile profile) {
			this.profile = profile;
			return this;
		}

		public Builder inputTank(String role) {
			inputTanks.add(role);
			return this;
		}

		public Builder outputTank(String role) {
			outputTanks.add(role);
			return this;
		}

		public Builder items(int inputs, int outputs) {
			this.itemInputs = inputs;
			this.itemOutputs = outputs;
			return this;
		}

		public Builder socket(String name, TagKey<Item> accepts) {
			sockets.add(new ToolSocket(name, accepts));
			return this;
		}

		public Builder port(String name, FormPort.Kind kind, int target, int column, int row, int layer, FormSide side) {
			ports.add(new FormPort(name, kind, target, column, row, layer, side));
			return this;
		}

		/** A process capability its family's recipes can ask for (see {@link FluidRecipe#capability()}). */
		public Builder capability(String capability) {
			capabilities.add(capability);
			return this;
		}

		public Builder upgrades() {
			this.upgrades = true;
			return this;
		}

		public Builder warmup(int ticks) {
			this.warmup = ticks;
			return this;
		}

		/**
		 * Makes this a generator form burning {@code fluid}: {@code jePerMb} JE from each millibucket, made into
		 * {@code jePerTick} JE a tick while it burns.
		 */
		public Builder fuel(Identifier fluid, int jePerMb, int jePerTick) {
			fuels.add(new FormFuel(fluid, jePerMb, jePerTick));
			return this;
		}

		/** Contents an earlier version saved under {@code oldRole} load into tank {@code newRole}. */
		public Builder alias(String oldRole, String newRole) {
			aliases.put(oldRole, newRole);
			return this;
		}

		/** Checks the whole description and builds it, or throws an exception naming the first problem. */
		public MachineForm build() {
			return new MachineForm(this);
		}
	}
}
