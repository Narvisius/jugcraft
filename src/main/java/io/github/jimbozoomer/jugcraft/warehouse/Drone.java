package io.github.jimbozoomer.jugcraft.warehouse;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import org.jspecify.annotations.Nullable;

/**
 * One drone of a warehouse's fleet, as the terminal keeps it (no entity: the clients draw it from its legs). It lives
 * at its dock; its current move is its leg (its last keyframe is where it is once the leg is over).
 */
public final class Drone {
	/** What it is doing. */
	public static final int IDLE = 0, STACK = 1, WRAP = 2, PUTAWAY = 3, HOME = 4, FETCH = 5, RETURN = 6, TO_LIFT = 7, FROM_LIFT = 8;
	/** Part W5's: housekeeping (a cycle count, a consolidation). */
	public static final int COUNT = 9, CONSOLIDATE = 10;
	public static final String[] JOBS = {"IDLE", "STACKING", "WRAPPING", "PUTAWAY", "TO DOCK", "FETCHING", "RETURNING", "TO LIFT", "FROM LIFT",
			"COUNTING", "CONSOLIDATING"};
	/** Part W5: what the screens and renderers get (DroneLine, View): paused, out of service, being located (its beacon blue). */
	public static final int PAUSED = 1, OUT_OF_SERVICE = 2, LOCATING = 4;

	public final int id;
	public final DroneKind kind;
	public final BlockPos dock;
	public Leg leg;
	/** The leg's events carried out so far. */
	public int done;
	/** Where it is (its middle's cell) and the way it faces (Router heading) once its leg is over. */
	public long cell;
	public int heading;
	public @Nullable CaseLoad held;
	public @Nullable PalletLoad carried;
	public int job = IDLE;
	/** What its job is with (the intake, the station, the rack place). */
	public @Nullable BlockPos source, target;
	public float battery = 1;
	public boolean docked = true;
	/** Not before this tick: the next time it tries to go home after failing to find a way. */
	public long retry;
	/** Part W5: held (finishes its move and stops), out of service (stays on its dock), sent home, located till a tick. */
	public boolean paused, outOfService, sendHome;
	public long locateUntil;

	public Drone(int id, DroneKind kind, BlockPos dock, Leg leg, long cell, int heading) {
		this.id = id;
		this.kind = kind;
		this.dock = dock.immutable();
		this.leg = leg;
		this.cell = cell;
		this.heading = heading;
	}

	public String name() {
		return String.format(java.util.Locale.ROOT, "%s-%02d", kind.prefix, id);
	}

	public boolean busy(long now) {
		return job != IDLE && (now < leg.end() || done < leg.events().size());
	}

	public int flags(long now) {
		return (paused ? PAUSED : 0) | (outOfService ? OUT_OF_SERVICE : 0) | (now < locateUntil ? LOCATING : 0);
	}

	/** The saved drone. */
	public static final Codec<Drone> CODEC = RecordCodecBuilder.create(i -> i.group(
			Codec.INT.fieldOf("id").forGetter(d -> d.id),
			Codec.STRING.fieldOf("kind").forGetter(d -> d.kind.name()),
			BlockPos.CODEC.fieldOf("dock").forGetter(d -> d.dock),
			Leg.CODEC.fieldOf("leg").forGetter(d -> d.leg),
			Codec.INT.fieldOf("done").forGetter(d -> d.done),
			Codec.LONG.fieldOf("cell").forGetter(d -> d.cell),
			Codec.INT.fieldOf("heading").forGetter(d -> d.heading),
			CaseLoad.CODEC.optionalFieldOf("held").forGetter(d -> Optional.ofNullable(d.held)),
			PalletLoad.CODEC.optionalFieldOf("carried").forGetter(d -> Optional.ofNullable(d.carried)),
			Codec.INT.fieldOf("job").forGetter(d -> d.job),
			BlockPos.CODEC.optionalFieldOf("source").forGetter(d -> Optional.ofNullable(d.source)),
			BlockPos.CODEC.optionalFieldOf("target").forGetter(d -> Optional.ofNullable(d.target)),
			Codec.FLOAT.fieldOf("battery").forGetter(d -> d.battery),
			Codec.BOOL.fieldOf("docked").forGetter(d -> d.docked),
			Codec.BOOL.optionalFieldOf("paused", false).forGetter(d -> d.paused),
			Codec.BOOL.optionalFieldOf("out_of_service", false).forGetter(d -> d.outOfService)).apply(i, (id, kind, dock, leg, done, cell, heading, held, carried,
					job, source, target, battery, docked, paused, outOfService) -> {
				Drone d = new Drone(id, DroneKind.valueOf(kind), dock, leg, cell, heading);
				d.done = done;
				d.held = held.orElse(null);
				d.carried = carried.orElse(null);
				d.job = job;
				d.source = source.orElse(null);
				d.target = target.orElse(null);
				d.battery = battery;
				d.docked = docked;
				d.paused = paused;
				d.outOfService = outOfService;
				return d;
			}));

	/** What the clients get of a drone: its number, kind and leg (they draw it from that); part W5: its flags, and till when it is located. */
	public record View(int id, int kind, Leg leg, int flags, long locateUntil) {
		public static final Codec<View> CODEC = RecordCodecBuilder.create(i -> i.group(
				Codec.INT.fieldOf("id").forGetter(View::id), Codec.INT.fieldOf("kind").forGetter(View::kind),
				Leg.CODEC.fieldOf("leg").forGetter(View::leg), Codec.INT.optionalFieldOf("flags", 0).forGetter(View::flags),
				Codec.LONG.optionalFieldOf("locate_until", 0L).forGetter(View::locateUntil)).apply(i, View::new));
		public static final StreamCodec<ByteBuf, View> STREAM_CODEC = StreamCodec.composite(
				ByteBufCodecs.VAR_INT, View::id, ByteBufCodecs.VAR_INT, View::kind, Leg.STREAM_CODEC, View::leg, ByteBufCodecs.VAR_INT, View::flags,
				ByteBufCodecs.VAR_LONG, View::locateUntil, View::new);

		public DroneKind droneKind() {
			return DroneKind.values()[Math.max(0, Math.min(DroneKind.values().length - 1, kind))];
		}
	}

	public View view() {
		return new View(id, kind.ordinal(), leg, (paused ? PAUSED : 0) | (outOfService ? OUT_OF_SERVICE : 0), locateUntil);
	}

	public String prefix() {
		return kind.prefix;
	}
}
