package io.github.jimbozoomer.jugcraft.warehouse;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;

/**
 * Builds a drone's leg keyframe by keyframe: where it is now (x, z, yaw, its parts, what it carries) and the tick from
 * the leg's start; each move adds a keyframe so long after the last.
 */
final class LegBuilder {
	final long start;
	final int y;
	private final List<Leg.Key> keys = new ArrayList<>();
	private final List<Leg.Event> events = new ArrayList<>();
	int t;
	float x, z, yaw;
	List<Float> p;
	int look;

	LegBuilder(long start, int y, Leg.Key from) {
		this.start = start;
		this.y = y;
		this.x = from.x();
		this.z = from.z();
		this.yaw = from.yaw();
		this.p = List.copyOf(from.p());
		this.look = from.look();
		keys.add(new Leg.Key(0, x, z, yaw, p, look));
	}

	long now() {
		return start + t;
	}

	/** Moves over dt ticks to a new pose (null: unchanged). */
	LegBuilder to(int dt, Float nx, Float nz, Float nyaw, List<Float> np) {
		t += Math.max(1, dt);
		if (nx != null) {
			x = nx;
		}
		if (nz != null) {
			z = nz;
		}
		if (nyaw != null) {
			yaw = nyaw;
		}
		if (np != null) {
			p = List.copyOf(np);
		}
		keys.add(new Leg.Key(t, x, z, yaw, p, look));
		return this;
	}

	/** Changes one part's value over dt ticks. */
	LegBuilder part(int dt, int index, float value) {
		List<Float> np = new ArrayList<>(p);
		while (np.size() <= index) {
			np.add(0F);
		}
		np.set(index, value);
		return to(dt, null, null, null, np);
	}

	/** Changes one part's value at a speed (units a tick). */
	LegBuilder partAt(float speed, int index, float value) {
		float from = index < p.size() ? p.get(index) : 0;
		return part((int) Math.ceil(Math.abs(value - from) / speed), index, value);
	}

	LegBuilder hold(int dt) {
		return to(dt, null, null, null, null);
	}

	/** From now on it carries look (a keyframe now, so the change is at this tick). */
	LegBuilder look(int value) {
		look = value;
		keys.add(new Leg.Key(t, x, z, yaw, p, look));
		return this;
	}

	LegBuilder event(int type, BlockPos pos, int arg) {
		events.add(new Leg.Event(t, type, pos.immutable(), arg));
		return this;
	}

	/** Drives a path (Router steps: the cells' middles, facing their headings, at their ticks). */
	LegBuilder path(List<Router.Step> steps) {
		for (Router.Step s : steps) {
			int at = (int) (s.time() - start);
			BlockPos c = BlockPos.of(s.cell());
			float nx = c.getX() + 0.5F, nz = c.getZ() + 0.5F, nyaw = Router.yaw(s.heading());
			if (at <= t) {
				if (Math.abs(nx - x) > 1e-4 || Math.abs(nz - z) > 1e-4 || Math.abs(nyaw - yaw) > 1e-3) {
					to(1, nx, nz, nyaw, null);
				}
				continue;
			}
			to(at - t, nx, nz, nyaw, null);
		}
		return this;
	}

	Leg build() {
		return new Leg(start, y, List.copyOf(keys), List.copyOf(events));
	}
}
