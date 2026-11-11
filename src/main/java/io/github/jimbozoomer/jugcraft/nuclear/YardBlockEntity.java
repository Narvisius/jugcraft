package io.github.jimbozoomer.jugcraft.nuclear;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.jimbozoomer.jugcraft.energy.EnergyNetworks;
import io.github.jimbozoomer.jugcraft.energy.SimpleEnergyStorage;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * A yard's controller (transformer yard and switchyard build, 20 October 2026; claude/jugcraft-transformer-yard-proposal.md):
 * the anchor of its compiled site (YardBlock), keeping what its machines do.
 *
 * <p>The transformer yard (transformer_yard): the reactor's generator power comes in on the isolated phase bus
 * (EquipmentBlockEntity hands it over: {@link #take}); the unit auxiliary transformers UAT-1A and 1B feed the plant's
 * own load centres (the buried feed under UAT-1A's bus duct), the main transformers MT-1A, 1B and 1C pass the rest to
 * the switchyard (while its breaker is closed; with no switchyard near, into a cable at the east gantry's feet). Each
 * transformer warms with its load and cools by its fan groups in stages (Yards.Unit): the needles, drag hands, lamps,
 * the fans turning and the pumps' flow gauges show it. A main transformer too hot trips (its lockout) and the breaker
 * opens until it has cooled. The deluge valve house: each valve set's manual pull release trips its zone (the priming
 * gauge drops, the TRIPPED lamp, the transformer's spray rings run, the water-motor gong rings outside) until RESET on
 * the releasing panel; SILENCE stops the gongs.
 *
 * <p>The switchyard (switchyard): bay 1's breaker BKR 1-1 follows the plant (it closes a couple of seconds after the
 * generator comes on, opens when it stops or trips: Justin, 18 October, all breakers cosmetic), its flag, lamps and
 * operations counter with it; line 1 takes the power from its line traps' feet. The disconnect 89-1 opens and closes
 * from its motor operator's buttons, its blades swinging up: opened while the breaker carries the generator it draws
 * an arc (a flash and a bang, burns anyone near, and the breaker trips) - open the breaker first (stop the
 * generator). Its earth switch can only be put on with 89-1 open, and 89-1 cannot close while it is on. SAT-1 stands
 * energized from the grid, lightly loaded, and carries the house load while the generator is off; its tap changer
 * steps to hold the 4160 V buses (the tap needle, its drag hands, its operations counter). In a thunderstorm the line's
 * surge arresters count the strikes.
 *
 * <p>Diesels make real power (Justin, 27 October 2026: "Storms and a test switch"): the switchyard is where the plant
 * meets the grid. In a thunderstorm each surge its arresters count is a strike you see (a bolt onto the yard: on the
 * line's gantry, the breaker, the disconnect or SAT-1; it only shows, it burns nothing); one in {@link #TRIP_ONE_IN} trips
 * the lines, and the grid is lost to the plant for {@link #LOST_MIN} to {@link #LOST_MAX} ticks (BKR 1-1 opens with them),
 * until they reclose by themselves. The plant's vessel reads it (PlantPower): no grid, no SAT-1, and the safety buses
 * go to their diesels. The transformer yard's unit auxiliary transformers feed the plant's load centres while the
 * generator runs; while it doesn't, SAT-1 does from the grid (and, with the grid lost, the diesels do once their load
 * sequencers reach the battery chargers).
 */
public class YardBlockEntity extends BlockEntity implements SiteAnchor {
	public static final String[] YARD_UNITS = {"MT-1A", "MT-1B", "MT-1C", "UAT-1A", "UAT-1B"};
	static final String[] YARD_PREFIX = {"mt1a_", "mt1b_", "mt1c_", "uat1a_", "uat1b_"};
	public static final String[] ZONES = {"DV-1", "DV-2", "DV-3", "DV-4", "DV-5", "DV-6", "DV-7"};
	public static final String[] PROTECTS = {"MT-1A", "MT-1B", "MT-1C", "MT-1S", "UAT-1A", "UAT-1B", "SAT-1"};
	/** Each unit auxiliary transformer's share of its rating carrying the plant's house load (30 of 48 MVA). */
	public static final float HOUSE_LOAD = 0.62F;
	/** SAT-1's: lightly loaded, or carrying the house load (60 of 100 MVA) while the generator is off. */
	public static final float SAT_LIGHT = 0.05F, SAT_HOUSE = 0.6F;
	/** Seconds the breaker takes to close after the generator comes on. */
	public static final int CLOSE_SECONDS = 2;
	/** Ticks: the disconnect's and the earth switch's travel. */
	public static final int DS_TRAVEL = 80, ES_TRAVEL = 40;
	/** Ticks into the disconnect's opening that its blades part (and arc, under load). */
	public static final int ARC_AFTER = 10;
	public static final long BUFFER = ReactorCore.RATED_JE * 2;
	/** A strike on the yard trips the lines one time in this many; the grid is then lost this long (ticks: 2 to 5 minutes). */
	public static final int TRIP_ONE_IN = 8, LOST_MIN = 2_400, LOST_MAX = 6_000;
	/** The house load the grid (through SAT-1) or the diesels feed the plant's load centres with, the generator off (JE a tick: a load centre's incoming rate). */
	public static final long HOUSE_SUPPLY = Switchgear.INCOMING_RATE;
	private static final EnumSet<Direction> ALL = EnumSet.allOf(Direction.class);

	final Yards.Unit[] units;
	public final SimpleEnergyStorage output;
	// The transformer yard.
	private long offered, exported;
	private long offeredSecond, exportedSecond;
	private int running;
	private int deluge;
	private boolean silenced;
	private @Nullable YardBlockEntity switchyard;
	// The switchyard.
	private boolean bkrClosed;
	private int wantFor;
	private int bkrOps = 1284, ltcOps = 21877, tap = 17, tapMin = 17, tapMax = 17;
	private final int[] arrOps = {3, 3, 3};
	private boolean dsOpen, esOn, arcLock;
	private double dsFrom, esFrom;
	private long dsAt = -100000, esAt = -100000, arcAt = -1, nextTap;
	// Both.
	private long lastDay = -1;
	private int shown;
	private List<BlockPos> terminals;
	private @Nullable BlockPos houseFeed;
	/** A switchyard's line 1 gantry anchor (power lines round 2): the plant's power goes out over a line strung from it. */
	private @Nullable BlockPos lineGantry;
	/** A switchyard: the grid lost until this game time (0: it's there); the strikes seen. */
	private long gridLostUntil;
	private int strikes;
	/** A transformer yard: the house load fed from SAT-1 or the diesels (refilled each tick), and until when the diesels feed it. */
	private final SimpleEnergyStorage houseSupply = new SimpleEnergyStorage(HOUSE_SUPPLY, 0, HOUSE_SUPPLY, () -> {
	});
	private long dieselHouseUntil;

	public YardBlockEntity(BlockPos pos, BlockState state) {
		super(JugcraftNuclear.YARD_ENTITY, pos, state);
		if (state.getValue(YardBlock.WORKS) == YardBlock.Works.YARD) {
			units = new Yards.Unit[] {new Yards.Unit(Yards.Kind.GSU), new Yards.Unit(Yards.Kind.GSU), new Yards.Unit(Yards.Kind.GSU),
					new Yards.Unit(Yards.Kind.UAT), new Yards.Unit(Yards.Kind.UAT)};
		} else {
			units = new Yards.Unit[] {new Yards.Unit(Yards.Kind.SAT)};
		}
		output = new SimpleEnergyStorage(BUFFER, 0, BUFFER, this::setChanged);
	}

	public YardBlock.Works works() {
		return getBlockState().getValue(YardBlock.WORKS);
	}

	public boolean yard() {
		return works() == YardBlock.Works.YARD;
	}

	// ------------------------------------------------------------------ what it does (the reactor and the other yard read these)

	/** The generator's power offered this tick (the transformer yard): what it takes in (its buffer's room). */
	public long take(long electric) {
		if (!yard() || electric <= 0) return 0;
		offered += electric;
		if (lockout()) return 0;
		long put = Math.min(output.getCapacity() - output.getAmount(), electric);
		output.setAmount(output.getAmount() + put);
		return put;
	}

	/** The generator running (power offered on the bus in the last second). */
	public boolean generatorRunning() {
		return running > 0;
	}

	/** A main transformer's lockout (too hot): the generator's breaker stays open. */
	public boolean lockout() {
		if (!yard()) return false;
		for (int i = 0; i < 3; i++) {
			if (units[i].tripped) return true;
		}
		return false;
	}

	public boolean uatTripped() {
		return yard() && (units[3].tripped || units[4].tripped);
	}

	public boolean breakerClosed() {
		return bkrClosed;
	}

	/** A switchyard: the grid there (its lines not tripped). */
	public boolean gridLive(long now) {
		return gridLostUntil <= now;
	}

	/** A switchyard: when its lines reclose (game time; 0 they're in). */
	public long gridBack() {
		return gridLostUntil;
	}

	/** A transformer yard: its switchyard (the nearest, as last found), or null. */
	public @Nullable YardBlockEntity switchyard() {
		YardBlockEntity sw = switchyard;
		return sw == null || sw.isRemoved() ? null : sw;
	}

	/** A transformer yard: the diesels carry the plant's load centres this tick and the next few (the vessel's PlantPower). */
	public void dieselsFeeding(long now) {
		dieselHouseUntil = now + 5;
	}

	/** A switchyard: a strike trips its lines now, for {@code ticks} (the game tests; the strikes themselves). */
	public void tripLines(ServerLevel level, long ticks) {
		long now = level.getGameTime();
		boolean was = gridLive(now);
		gridLostUntil = now + Math.max(1, ticks);
		if (was) {
			Vec3 at = point("breaker");
			if (at != null) {
				level.playSound(null, at.x, at.y, at.z, JugcraftNuclear.YARD_BREAKER, SoundSource.BLOCKS, 3.0F, 0.9F);
			}
		}
		if (bkrClosed) operate(level, false);
		wantFor = 0;
		sync();
	}

	/** m:ss. */
	static String mmss(long seconds) {
		long t = Math.max(0, seconds);
		return String.format(Locale.ROOT, "%d:%02d", t / 60, t % 60);
	}

	public int deluge() {
		return deluge;
	}

	public Yards.Unit unit(int i) {
		return units[i];
	}

	public int breakerOps() {
		return bkrOps;
	}

	public int tap() {
		return tap;
	}

	/** The disconnect's travel now (0 closed, 1 open). */
	public double dsPos() {
		return travel(dsOpen, dsFrom, dsAt, DS_TRAVEL);
	}

	/** The earth switch's travel now (0 off, 1 earthed). */
	public double esPos() {
		return travel(esOn, esFrom, esAt, ES_TRAVEL);
	}

	private double travel(boolean to, double from, long at, int ticks) {
		double t = level == null ? 1e9 : (level.getGameTime() - at) / (double) ticks;
		return Math.max(0, Math.min(1, from + (to ? t : -t)));
	}

	/** The share of the rating the main transformers carried out to the switchyard in the last second. */
	public float exportLoad() {
		return exportedSecond / 20F / ReactorCore.RATED_JE;
	}

	// ------------------------------------------------------------------ ticking

	public void serverTick(ServerLevel level) {
		serverTransfer(level);
		if (Math.floorMod(level.getGameTime() + getBlockPos().hashCode(), 20) == 0) {
			serverSecond(level);
		}
	}

	/** Each tick's work: the power on its way, the gongs, an arc falling due (the game tests call it straight). */
	public void serverTransfer(ServerLevel level) {
		long now = level.getGameTime();
		if (terminals == null || now % 200 == 0) {
			findPoints();
		}
		if (yard()) {
			// The unit auxiliary transformers: the plant's own load centres first. With the generator off, SAT-1 feeds
			// them from the grid (a plant with no switchyard has its grid at the line's far end), or the diesels do.
			if (running > 0 && !uatTripped() && houseFeed != null && output.getAmount() > 0) {
				EnergyNetworks.pushToNeighbors(level, houseFeed.above(), output, output.getAmount(), EnumSet.of(Direction.DOWN));
			} else if (houseFeed != null && running == 0) {
				YardBlockEntity sw = switchyard();
				if (sw == null || sw.gridLive(now) || now < dieselHouseUntil) {
					houseSupply.setAmount(HOUSE_SUPPLY);
					EnergyNetworks.pushToNeighbors(level, houseFeed.above(), houseSupply, HOUSE_SUPPLY, EnumSet.of(Direction.DOWN));
				}
			}
			// The main transformers: out to the switchyard while its breaker is closed (or, none near, into a cable at
			// the east gantry's feet).
			YardBlockEntity sw = switchyard;
			if (sw != null && (sw.isRemoved() || sw.getLevel() != level)) {
				sw = null;
			}
			if (sw != null) {
				if (sw.breakerClosed() && output.getAmount() > 0) {
					long move = Math.min(output.getAmount(), sw.output.getCapacity() - sw.output.getAmount());
					output.setAmount(output.getAmount() - move);
					sw.output.setAmount(sw.output.getAmount() + move);
					exported += move;
				}
			} else if (output.getAmount() > 0) {
				for (BlockPos t : terminals) {
					if (output.getAmount() <= 0) break;
					exported += EnergyNetworks.pushToNeighbors(level, t, output, output.getAmount(), ALL);
				}
			}
			if (deluge != 0 && !silenced && now % 20 == 0) {
				gongs(level);
			}
		} else {
			// Out over line 1 when a line is strung from its gantry, else into cables at the line entry.
			if (lineGantry != null && output.getAmount() > 0
					&& level.getBlockEntity(lineGantry) instanceof io.github.jimbozoomer.jugcraft.powerline.LineAnchorBlockEntity gantry
					&& !gantry.connections().isEmpty() && gantry.energy() != null) {
				try (net.fabricmc.fabric.api.transfer.v1.transaction.Transaction tx = net.fabricmc.fabric.api.transfer.v1.transaction.Transaction.openOuter()) {
					long moved = gantry.energy().insert(output.getAmount(), tx);
					tx.commit();
					output.setAmount(output.getAmount() - moved);
				}
			}
			for (BlockPos t : terminals) {
				if (output.getAmount() <= 0) break;
				EnergyNetworks.pushToNeighbors(level, t, output, output.getAmount(), ALL);
			}
			if (arcAt >= 0 && now >= arcAt) {
				arc(level);
			}
		}
	}

	/** Its second's work (each second, spread by position; the game tests call it straight). */
	public void serverSecond(ServerLevel level) {
		long now = level.getGameTime();
		long day = Math.floorDiv(level.getOverworldClockTime(), 24000L);
		if (lastDay >= 0 && day != lastDay) {
			// Dawn: the operator's round puts the drag hands back to the needles.
			for (Yards.Unit u : units) {
				u.resetDrag();
			}
			tapMin = tapMax = tap;
		}
		lastDay = day;
		float ambient = Yards.ambient(level);
		if (yard()) {
			offeredSecond = offered;
			exportedSecond = exported;
			offered = exported = 0;
			running = offeredSecond > 0 ? running + 1 : 0;
			boolean on = running > 0;
			float k = exportLoad();
			for (int i = 0; i < 3; i++) {
				units[i].second(ambient, k, on);
			}
			for (int i = 3; i < 5; i++) {
				units[i].second(ambient, HOUSE_LOAD, on);
			}
			switchyard = Yards.nearest(level, getBlockPos(), YardBlock.Works.SWITCHYARD);
		} else {
			YardBlockEntity yard = Yards.nearest(level, getBlockPos(), YardBlock.Works.YARD);
			// BKR 1-1 follows the plant: closed while the generator runs (with 89-1 closed and nothing locked out, and the
			// grid there to close onto).
			boolean want = yard != null && yard.generatorRunning() && !yard.lockout() && !dsOpen && dsPos() == 0 && !esOn && !arcLock
					&& gridLive(now);
			if (want) {
				wantFor++;
				if (!bkrClosed && wantFor >= CLOSE_SECONDS) operate(level, true);
			} else {
				wantFor = 0;
				if (bkrClosed) operate(level, false);
			}
			boolean house = yard == null || !yard.generatorRunning() || yard.uatTripped();
			units[0].second(ambient, house ? SAT_HOUSE : SAT_LIGHT, true);
			// The tap changer steps (now and then) to hold the 4160 V buses: higher under the house load.
			if (now >= nextTap) {
				int target = 16 + Math.round(units[0].load * 5) + level.getRandom().nextInt(3) - 1;
				if (target != tap) {
					tap = Math.max(12, Math.min(22, tap + Integer.signum(target - tap)));
					ltcOps++;
					tapMin = Math.min(tapMin, tap);
					tapMax = Math.max(tapMax, tap);
					Vec3 at = point("sat");
					if (at != null) level.playSound(null, at.x, at.y, at.z, SoundEvents.PISTON_CONTRACT, SoundSource.BLOCKS, 0.4F, 0.6F);
				}
				nextTap = now + 600 + level.getRandom().nextInt(1200);
			}
			// A thunderstorm: now and then a strike on the yard, counted by an arrester; now and then one trips the lines.
			if (level.isThundering() && level.getRandom().nextInt(90) == 0) {
				arrOps[level.getRandom().nextInt(3)]++;
				strike(level);
				if (level.getRandom().nextInt(TRIP_ONE_IN) == 0) {
					tripLines(level, LOST_MIN + level.getRandom().nextInt(LOST_MAX - LOST_MIN + 1));
				}
			}
			// The lines reclose by themselves once the fault has cleared.
			if (gridLostUntil > 0 && gridLive(now)) {
				gridLostUntil = 0;
				Vec3 at = point("breaker");
				if (at != null) {
					level.playSound(null, at.x, at.y, at.z, JugcraftNuclear.YARD_BREAKER, SoundSource.BLOCKS, 3.0F, 1.1F);
				}
			}
		}
		int s = signature();
		if (s != shown) {
			shown = s;
			sync();
		} else {
			setChanged();
		}
	}

	private int signature() {
		int h = java.util.Objects.hash(deluge, silenced, bkrClosed, bkrOps, ltcOps, tap, tapMin, tapMax, arrOps[0], arrOps[1], arrOps[2],
				dsOpen, esOn, arcLock, running > 0, gridLostUntil);
		for (Yards.Unit u : units) {
			h = 31 * h + java.util.Objects.hash(Math.round(u.oil), Math.round(u.wdg), Math.round(u.oilMax), Math.round(u.wdgMax), u.stage,
					u.tripped, u.energized);
		}
		return h;
	}

	/**
	 * A lightning bolt onto the yard (it only shows and sounds: it burns and hurts nothing): onto the top of the line's
	 * gantry, the breaker, the disconnect or SAT-1, whichever is picked.
	 */
	private void strike(ServerLevel level) {
		List<Vec3> spots = new ArrayList<>();
		for (String n : new String[] {"breaker", "disconnect", "sat"}) {
			Vec3 at = point(n);
			if (at != null) spots.add(at);
		}
		if (lineGantry != null) spots.add(Vec3.atCenterOf(lineGantry));
		if (spots.isEmpty()) spots.add(Vec3.atCenterOf(getBlockPos()));
		Vec3 at = spots.get(level.getRandom().nextInt(spots.size()));
		int top = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, (int) Math.floor(at.x), (int) Math.floor(at.z));
		net.minecraft.world.entity.LightningBolt bolt = net.minecraft.world.entity.EntityTypes.LIGHTNING_BOLT.create(level,
				net.minecraft.world.entity.EntitySpawnReason.TRIGGERED);
		if (bolt != null) {
			bolt.setVisualOnly(true);
			bolt.snapTo(new Vec3(at.x, Math.max(at.y, top), at.z));
			level.addFreshEntity(bolt);
		}
		strikes++;
	}

	/** The breaker opening or closing: its flag and lamps, its counter, its bang. */
	private void operate(ServerLevel level, boolean close) {
		bkrClosed = close;
		bkrOps++;
		Vec3 at = point("breaker");
		if (at != null) {
			level.playSound(null, at.x, at.y, at.z, JugcraftNuclear.YARD_BREAKER, SoundSource.BLOCKS, 3.0F, 1.0F);
		}
	}

	/** 89-1's blades parting under load: the arc. */
	private void arc(ServerLevel level) {
		arcAt = -1;
		Vec3 at = point("disconnect");
		if (at != null) {
			// (across the three poles: along the site's z, turned with it)
			Vec3 w = Yards.turn(siteFacing(), 0.6, 0.6, 4.5);
			double sx = Math.abs(w.x), sz = Math.abs(w.z);
			level.sendParticles(ParticleTypes.ELECTRIC_SPARK, at.x, at.y, at.z, 120, sx, 0.6, sz, 0.6);
			level.sendParticles(ParticleTypes.EXPLOSION, at.x, at.y, at.z, 3, sx * 0.6, 0.3, sz * 0.6, 0);
			level.sendParticles(ParticleTypes.LARGE_SMOKE, at.x, at.y, at.z, 30, sx * 0.9, 0.4, sz * 0.9, 0.02);
			level.playSound(null, at.x, at.y, at.z, JugcraftNuclear.YARD_ARC, SoundSource.BLOCKS, 6.0F, 1.0F);
			AABB near = new AABB(at, at).inflate(7.0);
			for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, near)) {
				e.hurtServer(level, level.damageSources().lightningBolt(), 10.0F);
				e.igniteForSeconds(3.0F);
			}
		}
		// Its protection sees the fault: the breaker trips and stays open until 89-1 is closed again.
		arcLock = true;
		if (bkrClosed) operate(level, false);
		sync();
	}

	/** The water-motor gongs of the zones flowing. */
	private void gongs(ServerLevel level) {
		JsonArray g = live("gongs") instanceof JsonArray a ? a : null;
		if (g == null) return;
		for (int k = 0; k < Math.min(6, g.size()); k++) {
			if ((deluge & 1 << k) == 0) continue;
			Vec3 at = sitePoint(g.get(k).getAsJsonArray());
			if (at != null) level.playSound(null, at.x, at.y, at.z, JugcraftNuclear.YARD_GONG, SoundSource.BLOCKS, 1.5F, 1.0F);
		}
		// DV-7's gong is on the house's wall too (its set is the seventh)
		if ((deluge & 1 << 6) != 0 && g.size() > 6) {
			Vec3 at = sitePoint(g.get(6).getAsJsonArray());
			if (at != null) level.playSound(null, at.x, at.y, at.z, JugcraftNuclear.YARD_GONG, SoundSource.BLOCKS, 1.5F, 1.0F);
		}
	}

	// ------------------------------------------------------------------ the client: the deluge's water, the hum

	public void clientTick(Level level) {
		SiteData d = data();
		if (d == null) return;
		long now = level.getGameTime();
		Player p = level.getNearestPlayer(getBlockPos().getX() + 0.5, getBlockPos().getY() + 0.5, getBlockPos().getZ() + 0.5, 96, false);
		if (p == null) return;
		// The deluge sprays: this site's zones (the switchyard's SAT-1 is DV-7, whose valve is in the transformer yard).
		int zones = deluge;
		if (!yard()) {
			YardBlockEntity y = Yards.nearest(level, getBlockPos(), YardBlock.Works.YARD);
			zones = y == null ? 0 : y.deluge();
		}
		JsonElement sprays = d.live.get("sprays");
		if (zones != 0 && sprays != null && sprays.isJsonObject()) {
			for (int k = 0; k < ZONES.length; k++) {
				if ((zones & 1 << k) == 0) continue;
				JsonElement list = sprays.getAsJsonObject().get(ZONES[k]);
				if (list == null) continue;
				JsonArray a = list.getAsJsonArray();
				for (int i = (int) (now % 2); i < a.size(); i += 2) {
					JsonArray n = a.get(i).getAsJsonArray();
					Vec3 at = Yards.world(getBlockPos(), siteFacing(), d, n.get(0).getAsDouble(), n.get(1).getAsDouble(), n.get(2).getAsDouble());
					Vec3 dir = Yards.turn(siteFacing(), n.get(3).getAsDouble(), n.get(4).getAsDouble(), n.get(5).getAsDouble());
					double t = level.getRandom().nextDouble() * 1.1;
					level.addParticle(ParticleTypes.SPLASH, at.x + dir.x * t, at.y + dir.y * t, at.z + dir.z * t, dir.x * 0.3, 0.05, dir.z * 0.3);
					if (level.getRandom().nextInt(4) == 0) {
						level.addParticle(ParticleTypes.FALLING_WATER, at.x + dir.x * 1.2, at.y + dir.y * 1.2, at.z + dir.z * 1.2, 0, 0, 0);
					}
				}
			}
		}
		// The transformers' hum, quiet and close: louder with their load.
		JsonElement hum = d.live.get("hum");
		if (hum != null && hum.isJsonArray() && Math.floorMod(now + getBlockPos().hashCode(), 40) == 0) {
			JsonArray a = hum.getAsJsonArray();
			for (int i = 0; i < a.size() && i < units.length; i++) {
				if (!units[i].energized) continue;
				JsonArray c = a.get(i).getAsJsonArray();
				Vec3 at = Yards.world(getBlockPos(), siteFacing(), d, c.get(0).getAsDouble(), c.get(1).getAsDouble(), c.get(2).getAsDouble());
				if (p.distanceToSqr(at) > 18 * 18) continue;
				level.playLocalSound(at.x, at.y, at.z, JugcraftNuclear.YARD_HUM, SoundSource.BLOCKS, 0.25F + 0.25F * Math.min(1, units[i].load),
						1.0F, false);
			}
		}
	}

	// ------------------------------------------------------------------ the controls

	@Override
	public void onZone(ServerLevel level, Player player, String zone) {
		long now = level.getGameTime();
		int colon = zone.indexOf(':');
		String head = colon < 0 ? zone : zone.substring(0, colon);
		String arg = colon < 0 ? "" : zone.substring(colon + 1);
		switch (head) {
			case "dv_pull" -> {
				int k = Integer.parseInt(arg);
				deluge |= 1 << k;
				silenced = false;
				player.sendOverlayMessage(YardBlock.line("yard_deluge", ZONES[k], PROTECTS[k]));
				level.playSound(null, player.blockPosition(), SoundEvents.IRON_TRAPDOOR_OPEN, SoundSource.BLOCKS, 1.0F, 0.6F);
			}
			case "dv_reset" -> {
				if (deluge == 0) {
					player.sendOverlayMessage(YardBlock.line("yard_deluge_armed"));
					return;
				}
				deluge = 0;
				silenced = false;
				player.sendOverlayMessage(YardBlock.line("yard_deluge_reset"));
				level.playSound(null, player.blockPosition(), SoundEvents.STONE_BUTTON_CLICK_ON, SoundSource.BLOCKS, 0.6F, 1.0F);
			}
			case "dv_silence" -> {
				silenced = true;
				player.sendOverlayMessage(YardBlock.line("yard_deluge_silence"));
				level.playSound(null, player.blockPosition(), SoundEvents.STONE_BUTTON_CLICK_ON, SoundSource.BLOCKS, 0.6F, 1.0F);
			}
			case "ds_open" -> {
				if (dsOpen) {
					player.sendOverlayMessage(YardBlock.line("yard_ds_already_open"));
					return;
				}
				dsFrom = dsPos();
				dsOpen = true;
				dsAt = now;
				YardBlockEntity yard = Yards.nearest(level, getBlockPos(), YardBlock.Works.YARD);
				if (bkrClosed && yard != null && yard.generatorRunning()) {
					arcAt = now + ARC_AFTER;
				}
				player.sendOverlayMessage(YardBlock.line("yard_ds_opening"));
				motor(level);
			}
			case "ds_close" -> {
				if (!dsOpen) {
					player.sendOverlayMessage(YardBlock.line("yard_ds_already_closed"));
					return;
				}
				if (esOn || esPos() > 0) {
					player.sendOverlayMessage(YardBlock.line("yard_ds_earthed"));
					return;
				}
				dsFrom = dsPos();
				dsOpen = false;
				dsAt = now;
				arcAt = -1;
				arcLock = false;
				player.sendOverlayMessage(YardBlock.line("yard_ds_closing"));
				motor(level);
			}
			case "es_toggle" -> {
				if (!esOn && (!dsOpen || dsPos() < 1)) {
					player.sendOverlayMessage(YardBlock.line("yard_es_interlock"));
					return;
				}
				esFrom = esPos();
				esOn = !esOn;
				esAt = now;
				player.sendOverlayMessage(YardBlock.line(esOn ? "yard_es_on" : "yard_es_off"));
				level.playSound(null, player.blockPosition(), SoundEvents.CHAIN_PLACE, SoundSource.BLOCKS, 1.0F, 0.7F);
			}
			case "bkr_info" -> player.sendOverlayMessage(YardBlock.line("yard_bkr_info", bkrClosed ? "closed" : "open"));
			default -> {
				return;
			}
		}
		sync();
	}

	private void motor(ServerLevel level) {
		Vec3 at = point("disconnect");
		if (at != null) level.playSound(null, at.x, at.y - 3, at.z, SoundEvents.PISTON_EXTEND, SoundSource.BLOCKS, 1.0F, 0.5F);
	}

	/** What its controller says when clicked. */
	public Component status() {
		if (yard()) {
			Object[] a = new Object[11];
			a[0] = String.format(Locale.ROOT, "%.0f", exportLoad() * 100);
			for (int i = 0; i < 3; i++) {
				a[1 + 2 * i] = Math.round(units[i].oil);
				a[2 + 2 * i] = Math.round(units[i].wdg);
			}
			a[7] = Math.round(units[3].oil);
			a[8] = Math.round(units[4].oil);
			a[9] = Integer.bitCount(deluge);
			a[10] = lockout() ? YardBlock.line("yard_lockout") : Component.empty();
			return YardBlock.line("yard_status", a);
		}
		long now = level == null ? 0 : level.getGameTime();
		Component grid = gridLive(now) ? YardBlock.line("switchyard_grid_live") : YardBlock.line("switchyard_grid_lost", mmss((gridLostUntil - now) / 20));
		return YardBlock.line("switchyard_status", bkrClosed ? "closed" : "open", dsPos() == 0 ? "closed" : dsPos() >= 1 ? "open" : "moving",
				esPos() >= 1 ? "on" : "off", String.format(Locale.ROOT, "%,d", output.getAmount()), tap, Math.round(units[0].oil)).copy().append(grid);
	}

	// ------------------------------------------------------------------ the site

	private @Nullable SiteData data() {
		return SiteData.get(works().site);
	}

	private @Nullable JsonElement live(String key) {
		SiteData d = data();
		return d == null ? null : d.live.get(key);
	}

	private @Nullable Vec3 sitePoint(JsonArray c) {
		SiteData d = data();
		if (d == null) return null;
		return Yards.world(getBlockPos(), siteFacing(), d, c.get(0).getAsDouble(), c.get(1).getAsDouble(), c.get(2).getAsDouble());
	}

	/** A named point of the site in the world (its live data's "points": the breaker, the disconnect's blades, SAT-1). */
	private @Nullable Vec3 point(String name) {
		JsonElement pts = live("points");
		if (pts == null || !pts.isJsonObject() || !pts.getAsJsonObject().has(name)) return null;
		return sitePoint(pts.getAsJsonObject().getAsJsonArray(name));
	}

	private void findPoints() {
		SiteData d = data();
		List<BlockPos> out = new ArrayList<>();
		houseFeed = null;
		lineGantry = null;
		if (d != null) {
			SiteRegistry.Placement p = new SiteRegistry.Placement(getBlockPos(), siteFacing(), d, new AABB(getBlockPos()));
			out = Yards.cells(p, d.live.get("terminals"));
			JsonElement h = d.live.get("house_feed");
			if (h != null && h.isJsonArray()) {
				JsonArray a = h.getAsJsonArray();
				houseFeed = p.world(a.get(0).getAsInt(), a.get(1).getAsInt(), a.get(2).getAsInt());
			}
			JsonElement g = d.live.get("line_gantry");
			if (g != null && g.isJsonArray()) {
				JsonArray a = g.getAsJsonArray();
				lineGantry = p.world(a.get(0).getAsInt(), a.get(1).getAsInt(), a.get(2).getAsInt());
			}
		}
		terminals = out;
	}

	@Override
	public @Nullable String siteKey() {
		return works().site;
	}

	@Override
	public Direction siteFacing() {
		return getBlockState().getValue(YardBlock.FACING);
	}

	private static final java.util.Map<String, SiteData.Group[][]> TABLES = new java.util.concurrent.ConcurrentHashMap<>();

	@Override
	public SiteData.Group[][] groups() {
		SiteData d = data();
		if (d == null) return new SiteData.Group[0][];
		return TABLES.computeIfAbsent(d.key, k -> d.groupTable());
	}

	/** Each variable of a site: what drives it (by its name, worked out once). */
	private record Slot(int kind, int unit, int arg, int yes, int no, boolean numeric) {
	}

	private static final int OIL = 0, OIL_MAX = 1, WDG = 2, WDG_MAX = 3, LVL = 4, PUMPS = 5, FANS = 6, F1 = 7, F2 = 8, HOT = 9, DV = 10, TAP = 11,
			TAP_MIN = 12, TAP_MAX = 13, BKR = 14, DS = 15, DS_FLAG = 16, ES = 17, ES_FLAG = 18, OTHER = 19;
	private static final String[] FIELDS = {"oil", "oilmax", "wdg", "wdgmax", "lvl", "pumps", "fans", "f1", "f2", "hot"};
	private static final java.util.Map<String, Slot[]> SLOTS = new java.util.concurrent.ConcurrentHashMap<>();

	private static Slot[] slots(SiteData d) {
		return SLOTS.computeIfAbsent(d.key, k -> {
			Slot[] out = new Slot[d.variables.size()];
			for (int i = 0; i < out.length; i++) {
				SiteData.Variable v = d.variables.get(i);
				String n = v.name();
				int kind = OTHER, unit = 0, arg = 0;
				String[] prefixes = d.key.equals(YardBlock.Works.YARD.site) ? YARD_PREFIX : new String[] {"sat_"};
				for (int u = 0; u < prefixes.length; u++) {
					if (n.startsWith(prefixes[u])) {
						String f = n.substring(prefixes[u].length());
						for (int j = 0; j < FIELDS.length; j++) {
							if (FIELDS[j].equals(f)) {
								kind = j;
								unit = u;
							}
						}
					}
				}
				if (kind == OTHER) {
					switch (n) {
						case "tap" -> kind = TAP;
						case "tapmin" -> kind = TAP_MIN;
						case "tapmax" -> kind = TAP_MAX;
						case "bkr" -> kind = BKR;
						case "ds" -> kind = DS;
						case "dsflag" -> kind = DS_FLAG;
						case "es" -> kind = ES;
						case "esflag" -> kind = ES_FLAG;
						default -> {
							if (n.startsWith("dv") && n.length() == 3 && Character.isDigit(n.charAt(2))) {
								kind = DV;
								arg = n.charAt(2) - '1';
							}
						}
					}
				}
				boolean numeric = v.values().length > 0 && !v.values()[0].equals("True") && !v.values()[0].equals("False");
				out[i] = new Slot(kind, unit, arg, v.indexOf("True"), v.indexOf("False"), numeric);
			}
			return out;
		});
	}

	@Override
	public int value(int v) {
		SiteData d = data();
		if (d == null || level == null || v >= d.variables.size()) return 0;
		Slot s = slots(d)[v];
		SiteData.Variable var = d.variables.get(v);
		Yards.Unit u = s.unit < units.length ? units[s.unit] : null;
		long now = level.getGameTime();
		return switch (s.kind) {
			case OIL -> u == null ? 0 : var.nearest(u.oil);
			case OIL_MAX -> u == null ? 0 : var.nearest(u.oilMax);
			case WDG -> u == null ? 0 : var.nearest(u.wdg);
			case WDG_MAX -> u == null ? 0 : var.nearest(u.wdgMax);
			case LVL -> u == null ? 0 : var.nearest(u.level());
			case PUMPS -> u != null && u.stage >= 1 ? s.yes : s.no;
			case FANS -> u == null ? 0 : s.numeric ? var.indexOf(String.valueOf(u.stage)) : u.stage >= 1 ? s.yes : s.no;
			// (the fans turn a step a tick, each transformer's a little out of step with the others)
			case F1 -> u != null && u.stage >= 1 ? (int) Math.floorMod(now + s.unit * 2L, var.values().length) : 0;
			case F2 -> u != null && u.stage >= 2 ? (int) Math.floorMod(now + s.unit * 2L + 3, var.values().length) : 0;
			case HOT -> u != null && u.hot() ? s.yes : s.no;
			case DV -> (deluge & 1 << s.arg) != 0 ? s.yes : s.no;
			case TAP -> var.nearest(tap);
			case TAP_MIN -> var.nearest(tapMin);
			case TAP_MAX -> var.nearest(tapMax);
			case BKR -> bkrClosed ? s.yes : s.no;
			case DS -> var.nearest(dsPos() * 90);
			case DS_FLAG -> dsPos() == 0 ? s.yes : s.no;
			case ES -> var.nearest(1.6 + esPos() * 88.4);
			case ES_FLAG -> esPos() >= 1 ? s.yes : s.no;
			default -> 0;
		};
	}

	/** The operations counters' figures (SiteLive's displays: the first four wheels, the last). */
	@Override
	public double display(String key) {
		int n;
		if (key.startsWith("bkr_ops")) n = bkrOps;
		else if (key.startsWith("ltc_ops")) n = ltcOps;
		else if (key.startsWith("arr_a")) n = arrOps[0];
		else if (key.startsWith("arr_b")) n = arrOps[1];
		else if (key.startsWith("arr_c")) n = arrOps[2];
		else return Double.NaN;
		return key.endsWith("_hi") ? Math.floorMod(n / 10, 10000) : Math.floorMod(n, 10);
	}

	// ------------------------------------------------------------------ saving and syncing

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
		for (int i = 0; i < units.length; i++) {
			units[i].load(in, "u" + i + "_");
		}
		output.setAmount(Math.min(output.getCapacity(), in.getLongOr("output", 0L)));
		running = in.getIntOr("running", 0);
		deluge = in.getIntOr("deluge", 0);
		silenced = in.getBooleanOr("silenced", false);
		bkrClosed = in.getBooleanOr("bkr_closed", false);
		bkrOps = in.getIntOr("bkr_ops", 1284);
		ltcOps = in.getIntOr("ltc_ops", 21877);
		tap = in.getIntOr("tap", 17);
		tapMin = in.getIntOr("tap_min", tap);
		tapMax = in.getIntOr("tap_max", tap);
		for (int k = 0; k < 3; k++) {
			arrOps[k] = in.getIntOr("arr_ops" + k, 3);
		}
		dsOpen = in.getBooleanOr("ds_open", false);
		esOn = in.getBooleanOr("es_on", false);
		arcLock = in.getBooleanOr("arc_lock", false);
		dsFrom = in.getDoubleOr("ds_from", dsOpen ? 1 : 0);
		esFrom = in.getDoubleOr("es_from", esOn ? 1 : 0);
		dsAt = in.getLongOr("ds_at", -100000L);
		esAt = in.getLongOr("es_at", -100000L);
		lastDay = in.getLongOr("last_day", -1L);
		gridLostUntil = in.getLongOr("grid_lost_until", 0L);
		strikes = in.getIntOr("strikes", 0);
	}

	@Override
	protected void saveAdditional(ValueOutput out) {
		super.saveAdditional(out);
		for (int i = 0; i < units.length; i++) {
			units[i].save(out, "u" + i + "_");
		}
		out.putLong("output", output.getAmount());
		out.putInt("running", running);
		out.putInt("deluge", deluge);
		out.putBoolean("silenced", silenced);
		out.putBoolean("bkr_closed", bkrClosed);
		out.putInt("bkr_ops", bkrOps);
		out.putInt("ltc_ops", ltcOps);
		out.putInt("tap", tap);
		out.putInt("tap_min", tapMin);
		out.putInt("tap_max", tapMax);
		for (int k = 0; k < 3; k++) {
			out.putInt("arr_ops" + k, arrOps[k]);
		}
		out.putBoolean("ds_open", dsOpen);
		out.putBoolean("es_on", esOn);
		out.putBoolean("arc_lock", arcLock);
		out.putDouble("ds_from", dsFrom);
		out.putDouble("es_from", esFrom);
		out.putLong("ds_at", dsAt);
		out.putLong("es_at", esAt);
		out.putLong("last_day", lastDay);
		out.putLong("grid_lost_until", gridLostUntil);
		out.putInt("strikes", strikes);
	}

	// ------------------------------------------------------------------ for the game tests

	/** Puts every transformer at a temperature (and its drag hands with it). */
	public void setTemperatures(float oil, float wdg) {
		for (Yards.Unit u : units) {
			u.oil = u.oilMax = oil;
			u.wdg = u.wdgMax = wdg;
		}
		sync();
	}

	/** Moves the disconnect's and the earth switch's travel to its end now (as if it had finished moving). */
	public void finishTravel() {
		dsAt = esAt = -100000L;
		dsFrom = dsOpen ? 1 : 0;
		esFrom = esOn ? 1 : 0;
		sync();
	}
}
