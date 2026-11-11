package io.github.jimbozoomer.jugcraft.nuclear;

import io.github.jimbozoomer.jugcraft.blueprint.Blueprint;
import io.github.jimbozoomer.jugcraft.blueprint.SurveyStakeBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import java.util.List;

/**
 * A cooling tower's controller. Every few seconds it checks its tower against the tower's blueprint (a spread of the
 * blueprint's blocks must be in place around it); a formed tower lends its capacity to the reactors whose condensers
 * reach it by conduit, a share of a standard unit's heat each tick, and steams in proportion to what it took.
 *
 * <p>The medium plant's tower as built to the detail standard (Tower.MEDIUM; towers and pump houses build, 19 October
 * 2026) is a compiled site (ct_medium) this controller anchors: its live parts show the stair tower's lights by the
 * photocell, the shell door and the crossover door (each opened and shut with a click), MU-1 (its handwheel turns it
 * open or shut), the obstruction lights flashing white by day and red by night, the sprays and rain while water flows,
 * and the local panel LP-CT1A's lamps, meters and LED windows from the tower's own figures. Its basin holds water: the
 * tower evaporates it as it works and make-up from the lake intake (through MU-1) keeps it at 92 %; a basin run down
 * below 30 % gives no more cooling.
 */
public class CoolingTowerBlockEntity extends BlockEntity implements SiteAnchor {
	/** Blocks of the tower's blueprint checked each time (spread over the whole tower). */
	public static final int SAMPLES = 160;
	/** Ticks between checks of the tower. */
	public static final int CHECK_INTERVAL = 100;

	private boolean formed;
	/** Share of capacity used, averaged over the last second (synced: the plume follows it). */
	private float load;
	/** The medium tower's basin level (fraction; 0.92 is its normal), its doors, MU-1, the panel's lamp test (until). */
	private float basin = 0.92F;
	private boolean door, door2, mu1 = true;
	private long lampTest;
	/** Whether make-up reaches the basin now (an intake with a make-up pump running, or no intake at all: the lake). */
	private boolean makeup = true;
	/** The level below which the medium tower's circulating water pumps lose suction: no cooling. */
	public static final float LOW_LOW = 0.30F;
	/** Basin level lost per tick working at full capacity without make-up (92 % to low-low in about six minutes). */
	public static final float EVAPORATION = 0.62F / 7200;
	/** Make-up at full flow, per tick: a quarter more than full-load evaporation, so it holds the level. */
	public static final float MAKEUP_RATE = EVAPORATION * 1.25F;
	private long claimTick = Long.MIN_VALUE;
	private double used;
	private double usedSum;
	private int usedTicks;

	public CoolingTowerBlockEntity(BlockPos pos, BlockState state) {
		super(JugcraftNuclear.COOLING_TOWER_ENTITY, pos, state);
	}

	public CoolingTowerBlock.Tower tower() {
		return getBlockState().getValue(CoolingTowerBlock.TOWER);
	}

	public boolean formed() {
		return formed;
	}

	public float load() {
		return load;
	}

	/**
	 * Takes up to {@code want} of this tower's capacity for this tick (in standard units of reactor heat). Returns what
	 * it could give: nothing unless the tower is formed, and never more than its capacity among all its users.
	 */
	public double claim(long gameTime, double want) {
		if (!formed || want <= 0 || tower() == CoolingTowerBlock.Tower.MEDIUM && basin < LOW_LOW) {
			return 0;
		}
		if (gameTime != claimTick) {
			claimTick = gameTime;
			used = 0;
		}
		double give = Math.max(0, Math.min(want, tower().capacity - used));
		used += give;
		usedSum += give;
		return give;
	}

	/** Rotation of the tower from its blueprint as drawn (the controller faces south in the file). */
	public static Rotation rotationOf(Direction facing) {
		return switch (facing) {
			case WEST -> Rotation.CLOCKWISE_90;
			case NORTH -> Rotation.CLOCKWISE_180;
			case EAST -> Rotation.COUNTERCLOCKWISE_90;
			default -> Rotation.NONE;
		};
	}

	public void serverTick(ServerLevel level) {
		long now = level.getGameTime();
		if (Math.floorMod(now + getBlockPos().hashCode(), CHECK_INTERVAL) == 0) {
			boolean was = formed;
			formed = check(level);
			if (was != formed) {
				sync();
			}
			// A finished tower fills its basin (and tops it up now and then): drones don't carry water.
			if (formed && tower() != CoolingTowerBlock.Tower.MEDIUM && (!was || Math.floorMod(now / CHECK_INTERVAL, 12) == 0)) {
				fillBasin(level);
			}
		}
		if (tower() == CoolingTowerBlock.Tower.MEDIUM && formed && Math.floorMod(now, 20) == 0) {
			basinTick(level, now);
		}
		if (++usedTicks >= 20) {
			float next = (float) Math.min(1, usedSum / usedTicks / tower().capacity);
			usedSum = 0;
			usedTicks = 0;
			if (Math.abs(next - load) > 0.04F || (next == 0) != (load == 0)) {
				load = next;
				sync();
			}
		}
	}

	/** Checks the tower now (normally every few seconds); for the game tests. */
	public boolean checkNow(ServerLevel level) {
		formed = check(level);
		sync();
		return formed;
	}

	/** Is the tower around this controller built? Compares a spread of its blueprint's blocks with the world. */
	private boolean check(ServerLevel level) {
		Blueprint blueprint = Blueprint.get(tower().blueprint, false);
		if (blueprint == null) {
			return false;
		}
		Rotation rotation = rotationOf(getBlockState().getValue(CoolingTowerBlock.FACING));
		int self = controllerCell(blueprint);
		if (self < 0) {
			return false;
		}
		BlockPos selfOffset = blueprint.offset(self, rotation);
		int checked = 0, matched = 0;
		int step = Math.max(1, blueprint.size() / SAMPLES);
		for (int i = step / 2; i < blueprint.size(); i += step) {
			BlockState wanted = blueprint.state(i, rotation);
			if (!wanted.getFluidState().isEmpty()) {
				continue;
			}
			BlockPos at = getBlockPos().offset(blueprint.offset(i, rotation).subtract(selfOffset));
			if (!level.isLoaded(at)) {
				continue;
			}
			checked++;
			if (SurveyStakeBlockEntity.matches(level.getBlockState(at), wanted)) {
				matched++;
			}
		}
		return checked >= SAMPLES / 3 && matched >= checked * 0.9;
	}

	/** The tower's water cells (its basin), as offsets from the controller, by blueprint and rotation. */
	private static final java.util.Map<String, List<BlockPos>> BASINS = new java.util.HashMap<>();

	private static synchronized List<BlockPos> basin(Blueprint blueprint, Rotation rotation) {
		return BASINS.computeIfAbsent(blueprint.id + "/" + rotation.ordinal(), key -> {
			List<BlockPos> out = new java.util.ArrayList<>();
			int self = controllerCell(blueprint);
			if (self < 0) return out;
			BlockPos selfOffset = blueprint.offset(self, rotation);
			for (int i = 0; i < blueprint.size(); i++) {
				if (!blueprint.state(i, Rotation.NONE).getFluidState().isEmpty()) {
					out.add(blueprint.offset(i, rotation).subtract(selfOffset));
				}
			}
			return List.copyOf(out);
		});
	}

	/** Water where the basin should be and there is only air. */
	private void fillBasin(ServerLevel level) {
		Blueprint blueprint = Blueprint.get(tower().blueprint, false);
		if (blueprint == null) return;
		Rotation rotation = rotationOf(getBlockState().getValue(CoolingTowerBlock.FACING));
		net.minecraft.world.level.block.state.BlockState water = net.minecraft.world.level.block.Blocks.WATER.defaultBlockState();
		for (BlockPos offset : basin(blueprint, rotation)) {
			BlockPos at = getBlockPos().offset(offset);
			if (level.isLoaded(at) && level.getBlockState(at).isAir()) {
				level.setBlock(at, water, Block.UPDATE_CLIENTS);
			}
		}
	}

	private static int controllerCell(Blueprint blueprint) {
		for (int i = 0; i < blueprint.size(); i++) {
			if (blueprint.state(i, Rotation.NONE).getBlock() instanceof CoolingTowerBlock) {
				return i;
			}
		}
		return -1;
	}

	/** Client: the plume, from the top of the tower (or each fan), as strong as the tower is busy. */
	public void clientTick(Level level) {
		if (load <= 0.01F || !formed) {
			return;
		}
		RandomSource random = level.getRandom();
		CoolingTowerBlock.Tower tower = tower();
		Direction facing = getBlockState().getValue(CoolingTowerBlock.FACING);
		// The fans run along the bank's length: across the controller's facing.
		Direction along = facing.getClockWise();
		double perTick = tower == CoolingTowerBlock.Tower.MECHANICAL ? 0.35 : 2.5;
		net.minecraft.world.phys.Vec3 axis = axis();
		for (int fan : tower.fans) {
			double count = perTick * load;
			while (count > 0 && (count >= 1 || random.nextDouble() < count)) {
				count -= 1;
				double angle = random.nextDouble() * Math.PI * 2, r = Math.sqrt(random.nextDouble()) * tower.plumeRadius;
				double x = axis.x + along.getStepX() * fan + Math.cos(angle) * r;
				double z = axis.z + along.getStepZ() * fan + Math.sin(angle) * r;
				double y = getBlockPos().getY() + tower.plumeHeight + 1;
				level.addAlwaysVisibleParticle(JugcraftNuclear.COOLING_PLUME, true, x, y, z, 0.03, 0.25 + random.nextDouble() * 0.1, 0.01);
			}
			// Under the fill the cooled water rains into the basin (only drawn near the player, like rain).
			int drops = tower == CoolingTowerBlock.Tower.MECHANICAL ? 2 : 14;
			for (int k = 0; k < drops * load + random.nextFloat(); k++) {
				double x, z;
				if (tower == CoolingTowerBlock.Tower.MECHANICAL) {
					double a = (random.nextDouble() * 2 - 1) * tower.fillRadius, b = (random.nextDouble() * 2 - 1) * 5;
					x = getBlockPos().getX() + 0.5 + along.getStepX() * (fan + a) + facing.getStepX() * b;
					z = getBlockPos().getZ() + 0.5 + along.getStepZ() * (fan + a) + facing.getStepZ() * b;
				} else {
					double angle = random.nextDouble() * Math.PI * 2, r = 4 + Math.sqrt(random.nextDouble()) * (tower.fillRadius - 4);
					x = axis.x + Math.cos(angle) * r;
					z = axis.z + Math.sin(angle) * r;
				}
				level.addParticle(net.minecraft.core.particles.ParticleTypes.FALLING_WATER, x, getBlockPos().getY() + tower.fillHeight - 0.05, z, 0, 0, 0);
			}
		}
		// The sound of the rain inside, now and then.
		if (level.getGameTime() % 30 == 0) {
			level.playLocalSound(axis.x, getBlockPos().getY() + tower.fillHeight * 0.5, axis.z,
					net.minecraft.sounds.SoundEvents.WEATHER_RAIN, net.minecraft.sounds.SoundSource.BLOCKS,
					tower == CoolingTowerBlock.Tower.MECHANICAL ? 0.6F * load : 1.4F * load, 0.75F, false);
		}
	}

	/** Client: how far the fans have turned (degrees), for the renderer. */
	public float fanAngle(float partialTick) {
		if (level == null || !formed) return 0;
		return ((level.getGameTime() % 3600) + partialTick) * 9F * Math.max(0.15F, load);
	}

	private void sync() {
		setChanged();
		if (level != null && !level.isClientSide()) {
			level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
		}
	}

	@Override
	public Packet<ClientGamePacketListener> getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		TagValueOutput out = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, registries);
		saveAdditional(out);
		return out.buildResult();
	}

	@Override
	protected void loadAdditional(ValueInput in) {
		super.loadAdditional(in);
		formed = in.getBooleanOr("formed", false);
		load = in.getFloatOr("load", 0);
		basin = in.getFloatOr("basin", 0.92F);
		door = in.getBooleanOr("door", false);
		door2 = in.getBooleanOr("door2", false);
		mu1 = in.getBooleanOr("mu1", true);
		makeup = in.getBooleanOr("makeup", true);
		lampTest = in.getLongOr("lamp_test", 0L);
	}

	@Override
	protected void saveAdditional(ValueOutput out) {
		super.saveAdditional(out);
		out.putBoolean("formed", formed);
		out.putFloat("load", load);
		if (tower() == CoolingTowerBlock.Tower.MEDIUM) {
			out.putFloat("basin", basin);
			out.putBoolean("door", door);
			out.putBoolean("door2", door2);
			out.putBoolean("mu1", mu1);
			out.putBoolean("makeup", makeup);
			out.putLong("lamp_test", lampTest);
		}
	}

	// ------------------------------------------------------------------ the medium tower's site (ct_medium)

	/** The tower's basin level (fraction). */
	public float basin() {
		return basin;
	}

	public boolean mu1Open() {
		return mu1;
	}

	/** For the game tests and the intake: whether make-up reaches the basin now. */
	public boolean makeup() {
		return makeup;
	}

	/** Sets the basin level (game tests). */
	public void setBasin(float level) {
		basin = Math.max(0, Math.min(1, level));
		sync();
	}

	/**
	 * Each second: the basin loses what the tower evaporated (its load) and gains make-up (through MU-1, from the lake
	 * intake's make-up pumps, held at 92 % by the make-up's level control); the level is synced as it moves.
	 */
	private void basinTick(ServerLevel level, long now) {
		makeup = WaterWorks.makeupAvailable(level, getBlockPos());
		float before = basin;
		basin -= EVAPORATION * 20 * load;
		if (mu1 && makeup && basin < 0.92F) {
			basin = Math.min(0.92F, basin + MAKEUP_RATE * 20);
		}
		basin = Math.max(0, basin);
		basinWater(level);
		if (Math.abs(basin - before) > 1e-6 && (Math.round(basin * 200) != Math.round(before * 200) || basin < LOW_LOW)) {
			sync();
		} else if (basin != before) {
			setChanged();
		}
	}

	/**
	 * The basin's water blocks follow its level: the top layer (level with the controller) stands while the level is
	 * over half, the layer under it over a tenth, the outlet sump's below. Changed a few hundred blocks a second.
	 */
	private void basinWater(ServerLevel level) {
		Blueprint blueprint = Blueprint.get(tower().blueprint, false);
		if (blueprint == null) return;
		Rotation rotation = rotationOf(getBlockState().getValue(CoolingTowerBlock.FACING));
		int changed = 0;
		BlockPos self = getBlockPos();
		for (BlockPos offset : wetCells(blueprint, rotation)) {
			int dy = offset.getY();
			boolean want = dy >= 0 ? basin >= 0.5F : dy == -1 ? basin >= 0.1F : basin >= 0.02F;
			BlockPos at = self.offset(offset);
			if (!level.isLoaded(at)) continue;
			BlockState st = level.getBlockState(at);
			if (st.getBlock() instanceof SiteBlock) {
				if (st.getValue(SiteBlock.WATERLOGGED) != want) {
					level.setBlock(at, st.setValue(SiteBlock.WATERLOGGED, want), Block.UPDATE_CLIENTS);
					changed++;
				}
			} else if (want && st.isAir()) {
				level.setBlock(at, net.minecraft.world.level.block.Blocks.WATER.defaultBlockState(), Block.UPDATE_CLIENTS);
				changed++;
			} else if (!want && st.is(net.minecraft.world.level.block.Blocks.WATER)) {
				level.setBlock(at, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
				changed++;
			}
			if (changed >= 400) break;
		}
	}

	/** The blueprint's water: its water blocks and its waterlogged structure, as offsets from the controller. */
	private static final java.util.Map<String, List<BlockPos>> WET = new java.util.HashMap<>();

	private static synchronized List<BlockPos> wetCells(Blueprint blueprint, Rotation rotation) {
		return WET.computeIfAbsent(blueprint.id + "/" + rotation.ordinal(), key -> {
			List<BlockPos> out = new java.util.ArrayList<>();
			int self = controllerCell(blueprint);
			if (self < 0) return out;
			BlockPos selfOffset = blueprint.offset(self, rotation);
			for (int i = 0; i < blueprint.size(); i++) {
				BlockState st = blueprint.state(i, Rotation.NONE);
				if (!st.getFluidState().isEmpty()) {
					out.add(blueprint.offset(i, rotation).subtract(selfOffset));
				}
			}
			// The top layer first: it goes first as the level falls.
			out.sort(java.util.Comparator.comparingInt(p -> -p.getY()));
			return List.copyOf(out);
		});
	}

	/** The tower's axis on the grass (the plume's and the rain's middle): the controller itself for the older kinds. */
	public net.minecraft.world.phys.Vec3 axis() {
		BlockPos at = getBlockPos();
		if (tower() != CoolingTowerBlock.Tower.MEDIUM) {
			return new net.minecraft.world.phys.Vec3(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
		}
		// The controller's cell is (0, -1, 62) of the site; the axis is the site's origin.
		double dx = -0.5, dz = -62.5;
		double[] t = switch (getBlockState().getValue(CoolingTowerBlock.FACING)) {
			case WEST -> new double[] {-dz, dx};
			case NORTH -> new double[] {-dx, -dz};
			case EAST -> new double[] {dz, -dx};
			default -> new double[] {dx, dz};
		};
		return new net.minecraft.world.phys.Vec3(at.getX() + 0.5 + t[0], at.getY() + 1, at.getZ() + 0.5 + t[1]);
	}

	@Override
	public @org.jspecify.annotations.Nullable String siteKey() {
		return tower() == CoolingTowerBlock.Tower.MEDIUM ? "ct_medium" : null;
	}

	@Override
	public Direction siteFacing() {
		return getBlockState().getValue(CoolingTowerBlock.FACING);
	}

	private static SiteData.Group[][] table;

	@Override
	public SiteData.Group[][] groups() {
		if (table == null) {
			SiteData d = SiteData.get("ct_medium");
			table = d == null ? new SiteData.Group[0][] : d.groupTable();
		}
		return siteKey() == null ? new SiteData.Group[0][] : table;
	}

	/** Whether it is dark enough for the photocell (dusk to dawn). */
	private boolean night() {
		if (level == null) return false;
		long t = Math.floorMod(level.getOverworldClockTime(), 24000L);
		return t > 12600 && t < 23400;
	}

	private boolean running() {
		return formed && load > 0.01F;
	}

	private double hot() {
		return running() ? 70 + 35 * load : 72;
	}

	private double cold() {
		return running() ? 70 + 15 * load : 72;
	}

	@Override
	public int value(int v) {
		SiteData d = SiteData.get("ct_medium");
		if (d == null || v >= d.variables.size() || level == null) return 0;
		SiteData.Variable var = d.variables.get(v);
		boolean test = level.getGameTime() < lampTest;
		return switch (var.name()) {
			case "lights" -> night() ? 1 : 0;
			case "door" -> door ? 1 : 0;
			case "door2" -> door2 ? 1 : 0;
			case "mu1" -> mu1 ? 0 : 1;
			case "beacon" -> Math.floorMod(level.getGameTime(), 30) < 6 ? (night() ? 2 : 1) : 0;
			case "running" -> running() ? 0 : 1;
			case "hi" -> test || basin > 0.98F ? 1 : 0;
			case "lo" -> test || basin < 0.6F ? 1 : 0;
			case "m_level" -> var.nearest(basin * 100);
			case "m_hot" -> var.nearest(hot());
			case "m_cold" -> var.nearest(cold());
			case "m_flow" -> var.nearest(running() ? 480 : 0);
			default -> 0;
		};
	}

	@Override
	public double display(String key) {
		return switch (key) {
			case "range" -> hot() - cold();
			case "approach" -> running() ? cold() - 72 : Double.NaN;
			case "wet_bulb" -> 72;
			default -> Double.NaN;
		};
	}

	@Override
	public void onZone(ServerLevel level, net.minecraft.world.entity.player.Player player, String zone) {
		switch (zone) {
			case "door", "door2" -> {
				boolean open = zone.equals("door") ? (door = !door) : (door2 = !door2);
				level.playSound(null, player.blockPosition(), open ? net.minecraft.sounds.SoundEvents.IRON_DOOR_OPEN
						: net.minecraft.sounds.SoundEvents.IRON_DOOR_CLOSE, net.minecraft.sounds.SoundSource.BLOCKS, 1, 1);
				sync();
			}
			case "mu1" -> {
				mu1 = !mu1;
				player.sendOverlayMessage(net.minecraft.network.chat.Component.translatable(mu1 ? "message.jugcraft.nuclear.mu1_open"
						: "message.jugcraft.nuclear.mu1_shut"));
				sync();
			}
			case "lamp_test" -> {
				lampTest = level.getGameTime() + 60;
				sync();
			}
			case "alarm_ack" -> player.sendOverlayMessage(net.minecraft.network.chat.Component.translatable(
					"message.jugcraft.nuclear.lp_ct1a", Math.round(basin * 100), mu1 ? "open" : "shut"));
			default -> {
			}
		}
	}

}
