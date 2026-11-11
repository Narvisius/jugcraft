package io.github.jimbozoomer.jugcraft.warehouse;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * One move of a drone, worked out whole when it starts (the server sends it to the players near once; they animate it
 * themselves on the game clock): keyframes of its pose (where its middle is, which way it faces, its moving parts'
 * values, what it carries) at ticks from the start, and the events the server carries out on the way (a case taken
 * off the conveyor, a pallet set in its place). Between keyframes everything moves evenly.
 *
 * <p>The parts' values (p): a pallet drone's lift (px its carriage is up), traverse (px its head is across) and head
 * (degrees its forks are turned: 0 ahead, -90 to its right); a stacker's arm (base turn, shoulder, elbow, wrist, in
 * degrees) and its film carriage (px up the mast). look: what it carries (a pallet drone: the pallet's look, or -1;
 * a stacker: the case's kind, or -1).
 */
public record Leg(long start, int y, List<Key> keys, List<Event> events) {
	public static final Codec<Leg> CODEC = RecordCodecBuilder.create(i -> i.group(
			Codec.LONG.fieldOf("start").forGetter(Leg::start),
			Codec.INT.fieldOf("y").forGetter(Leg::y),
			Key.CODEC.listOf().fieldOf("keys").forGetter(Leg::keys),
			Event.CODEC.listOf().fieldOf("events").forGetter(Leg::events)).apply(i, Leg::new));
	public static final StreamCodec<ByteBuf, Leg> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_LONG, Leg::start, ByteBufCodecs.VAR_INT, Leg::y,
			Key.STREAM_CODEC.apply(ByteBufCodecs.list(512)), Leg::keys,
			Event.STREAM_CODEC.apply(ByteBufCodecs.list(64)), Leg::events, Leg::new);

	public int duration() {
		return keys.isEmpty() ? 0 : keys.get(keys.size() - 1).t();
	}

	public long end() {
		return start + duration();
	}

	public Key last() {
		return keys.get(keys.size() - 1);
	}

	/** A leg of one keyframe: standing still in a pose from a tick on. */
	public static Leg still(long start, int y, Key pose) {
		return new Leg(start, y, List.of(new Key(0, pose.x(), pose.z(), pose.yaw(), pose.p(), pose.look())), List.of());
	}

	/** The pose at a moment (time: game ticks, with the part tick). */
	public Key at(double time) {
		double t = time - start;
		if (keys.size() == 1 || t <= 0) {
			return keys.get(0);
		}
		Key prev = keys.get(0);
		for (int k = 1; k < keys.size(); k++) {
			Key next = keys.get(k);
			if (t <= next.t()) {
				double f = next.t() == prev.t() ? 1 : (t - prev.t()) / (next.t() - prev.t());
				return Key.between(prev, next, f);
			}
			prev = next;
		}
		return prev;
	}

	/** A keyframe: at tick t from the leg's start, the drone's middle at (x, z) (blocks), facing yaw, its parts p, carrying look. */
	public record Key(int t, float x, float z, float yaw, List<Float> p, int look) {
		public static final Codec<Key> CODEC = RecordCodecBuilder.create(i -> i.group(
				Codec.INT.fieldOf("t").forGetter(Key::t), Codec.FLOAT.fieldOf("x").forGetter(Key::x), Codec.FLOAT.fieldOf("z").forGetter(Key::z),
				Codec.FLOAT.fieldOf("yaw").forGetter(Key::yaw), Codec.FLOAT.listOf().fieldOf("p").forGetter(Key::p),
				Codec.INT.fieldOf("look").forGetter(Key::look)).apply(i, Key::new));
		public static final StreamCodec<ByteBuf, Key> STREAM_CODEC = StreamCodec.composite(
				ByteBufCodecs.VAR_INT, Key::t, ByteBufCodecs.FLOAT, Key::x, ByteBufCodecs.FLOAT, Key::z, ByteBufCodecs.FLOAT, Key::yaw,
				ByteBufCodecs.FLOAT.apply(ByteBufCodecs.list(8)), Key::p, ByteBufCodecs.VAR_INT, Key::look, Key::new);

		public float part(int i) {
			return i < p.size() ? p.get(i) : 0;
		}

		public Key at(int t) {
			return new Key(t, x, z, yaw, p, look);
		}

		static Key between(Key a, Key b, double f) {
			List<Float> p = new ArrayList<>(Math.max(a.p.size(), b.p.size()));
			for (int i = 0; i < Math.max(a.p.size(), b.p.size()); i++) {
				p.add((float) (a.part(i) + (b.part(i) - a.part(i)) * f));
			}
			float dyaw = (float) (((b.yaw - a.yaw) % 360 + 540) % 360 - 180);
			// What it carries changes at the keyframe it changes at.
			return new Key((int) Math.round(a.t + (b.t - a.t) * f), (float) (a.x + (b.x - a.x) * f), (float) (a.z + (b.z - a.z) * f),
					(float) (a.yaw + dyaw * f), p, f >= 1 ? b.look : a.look);
		}
	}

	/**
	 * Something the server does at tick t of the leg (Planner: PICK_CASE, PLACE_CASE, WRAP, PICK_PALLET, PUT_PALLET,
	 * DOCK, UNDOCK) at a block (the intake's, station's or dock's origin, the rack place), with an argument.
	 */
	public record Event(int t, int type, BlockPos pos, int arg) {
		public static final Codec<Event> CODEC = RecordCodecBuilder.create(i -> i.group(
				Codec.INT.fieldOf("t").forGetter(Event::t), Codec.INT.fieldOf("type").forGetter(Event::type),
				BlockPos.CODEC.fieldOf("pos").forGetter(Event::pos), Codec.INT.fieldOf("arg").forGetter(Event::arg)).apply(i, Event::new));
		public static final StreamCodec<ByteBuf, Event> STREAM_CODEC = StreamCodec.composite(
				ByteBufCodecs.VAR_INT, Event::t, ByteBufCodecs.VAR_INT, Event::type, BlockPos.STREAM_CODEC, Event::pos,
				ByteBufCodecs.VAR_INT, Event::arg, Event::new);
	}
}
