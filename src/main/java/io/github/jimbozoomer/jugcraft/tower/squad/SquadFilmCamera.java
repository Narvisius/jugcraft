package io.github.jimbozoomer.jugcraft.tower.squad;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * Where the squad film's camera is and looks each tick (tower/squad/SquadFilm; the client puts the player there:
 * client/FilmShoot; the game test flies it alongside a real drop and fight to check what it shows). The shots, in turn:
 * on the ground behind the squad's spot looking up at the flare and following the pods down ({@link Shot#SKY}); beside
 * the landing as the hatches blow and the soldiers walk out to their spot, the pods blowing apart behind them
 * ({@link Shot#LANDED}); on the squad formed up, the Warden's ground behind them, calling the Warden up
 * ({@link Shot#FORM}); low before the Warden as it comes up and roars ({@link Shot#RISE}); round behind it as it goes for
 * them, its back to the camera, so the holes the bolts burn show the light through ({@link Shot#FIGHT}); on it as it
 * falls ({@link Shot#FALL}); up over the squad ({@link Shot#AFTER}). The camera follows springs toward where each shot
 * wants it (critically damped: it eases off and settles, never jumps), and never goes into the ground.
 */
public final class SquadFilmCamera {
	public enum Shot { SKY, LANDED, FORM, RISE, FIGHT, FALL, AFTER, END }

	/** How far (blocks) the camera keeps from anyone's outline. */
	public static final double CLEAR = 0.8;

	private final SquadFilm s;
	public Shot shot = Shot.SKY;
	private long shotStart = -1;
	public Vec3 pos, look;
	private Vec3 vel = Vec3.ZERO, lookVel = Vec3.ZERO;
	/** The camera's turn (degrees, kept unwrapped so it never spins round) and tilt. */
	public float yaw = Float.NaN, pitch;
	/** Round the Warden: the camera's angle (degrees: 0 toward the squad, + to its left) and its spring's speed. */
	private double orbit = 25, orbitVel;
	private Vec3 wardenFwd, wardenSeen, afterDir;
	private long podsGoneAt = -1;
	/** Set once the camera's ready for the Warden (the squad formed up, a moment on them). */
	public boolean wantWarden;
	/** Each shot's start: the game tick and the shot. */
	public final List<String> cues = new ArrayList<>();

	public SquadFilmCamera(SquadFilm scene) {
		this.s = scene;
	}

	/** Where the flare hangs: over the drop, off toward the tower. */
	public Vec3 flare() {
		return s.mark.add(s.out.scale(SquadDrop.FLARE_OUT)).add(0, SquadDrop.FLARE_UP, 0);
	}

	private void cue(Shot next, long now) {
		shot = next;
		shotStart = now;
		cues.add(now + " " + next);
	}

	/** One tick: where each shot wants the camera now, and the springs a step toward it. {@code rolling}: the action's begun. */
	public void step(Level level, long now, boolean rolling) {
		if (shotStart < 0) {
			shotStart = now;
		}
		long held = now - shotStart;
		long age = s.launched < 0 ? -1 : now - s.launched;
		Vec3 wantPos, wantLook;
		double wp = 1.0, wl = 1.4;
		List<DropPod> pods = s.launched < 0 ? List.of()
				: level.getEntitiesOfClass(DropPod.class, new AABB(s.mark, s.mark).inflate(90, 320, 90), d -> d.start() == s.launched);
		if (s.launched >= 0 && s.podsLeft == 0 && podsGoneAt < 0) {
			podsGoneAt = now;
		}
		switch (shot) {
			case SKY -> {
				Vec3 toMark = s.mark.subtract(s.camSky).multiply(1, 0, 1).normalize();
				double push = smooth(Mth.clamp((age + 40) / 380.0, 0, 1)) * 4;
				wantPos = s.camSky.add(toMark.scale(push));
				wantLook = podsLook(pods, age);
				int lastLand = 0;
				for (DropPod d : pods) {
					lastLand = Math.max(lastLand, d.land());
				}
				if (rolling && (!pods.isEmpty() && age > lastLand + 30 || age > 1000)) {
					cue(Shot.LANDED, now);
				}
			}
			case LANDED -> {
				wantPos = s.camGround;
				wantLook = crowd(level, pods);
				wl = 1.2;
				if (podsGoneAt >= 0 && now - podsGoneAt > 40 || held > 340) {
					cue(Shot.FORM, now);
				}
			}
			case FORM -> {
				wantPos = s.camForm;
				wantLook = s.anchor.add(0, 1.2, 0).lerp(s.warden.add(0, 1.5, 0), 0.25);
				wl = 1.2;
				if (!wantWarden && (s.formedAt >= 0 && held > 50 || held > 220)) {
					wantWarden = true;
				}
				if (s.wardenUp >= 0 && now - s.wardenUp > 30) {
					cue(Shot.RISE, now);
				}
			}
			case RISE -> {
				Entity w = level.getEntity(s.wardenId);
				Vec3 c = w != null ? w.position() : s.warden;
				aimAtSquad(c);
				orbit = spring1(orbit, 25, 1.5);
				double rise = Mth.clamp((now - s.wardenUp) / 134.0, 0, 1);
				wantPos = round(c, orbit, 7.5, 1.1);
				wantLook = c.add(0, 0.7 + 1.5 * rise, 0);
				wp = 1.6;
				wl = 2.5;
				boolean moving = w != null && Math.hypot(w.getX() - s.warden.x, w.getZ() - s.warden.z) > 2.5;
				if (s.wardenEmerged >= 0 && (moving || now - s.wardenEmerged > 110) || s.wardenDied >= 0) {
					cue(Shot.FIGHT, now);
				}
			}
			case FIGHT -> {
				Entity w = level.getEntity(s.wardenId);
				if (w != null) {
					wardenSeen = w.position();
				}
				Vec3 c = wardenSeen != null ? wardenSeen : s.warden;
				aimAtSquad(c);
				orbit = spring1(orbit, 150, 1.0);
				wantPos = round(c, orbit, 6.5, 2.0);
				wantLook = c.add(0, 1.7, 0);
				wp = 3.0;
				wl = 4.0;
				boolean down = w == null || !w.isAlive() || w instanceof LivingEntity le && le.isDeadOrDying() || s.wardenDied >= 0;
				if (down) {
					cue(Shot.FALL, now);
				} else if (held > 900 || s.menAlive == 0 && held > 60) {
					cue(Shot.AFTER, now);
				}
			}
			case FALL -> {
				Entity w = level.getEntity(s.wardenId);
				if (w != null) {
					wardenSeen = w.position();
				}
				Vec3 c = wardenSeen != null ? wardenSeen : s.warden;
				orbit += 0.6;
				wantPos = round(c, orbit, 5.0, 1.4);
				wantLook = c.add(0, 0.6, 0);
				wp = 1.5;
				wl = 2.0;
				if (held > 50) {
					cue(Shot.AFTER, now);
				}
			}
			case AFTER -> {
				if (afterDir == null) {
					Vec3 d = pos.subtract(s.anchor).multiply(1, 0, 1);
					afterDir = d.lengthSqr() < 1e-4 ? s.out.scale(-1) : d.normalize();
				}
				wantPos = s.anchor.add(afterDir.scale(13)).add(0, 7.5, 0);
				wantLook = s.anchor.add(0, 0.8, 0);
				wp = 0.9;
				wl = 1.2;
				if (held > 100) {
					cue(Shot.END, now);
				}
			}
			default -> {
				wantPos = pos != null ? pos : s.camSky;
				wantLook = look != null ? look : s.anchor;
			}
		}
		if (pos == null) {
			pos = wantPos;
			look = wantLook;
		} else {
			double dt = 0.05;
			vel = vel.add(wantPos.subtract(pos).scale(wp * wp * dt)).subtract(vel.scale(2 * wp * dt));
			pos = pos.add(vel.scale(dt));
			lookVel = lookVel.add(wantLook.subtract(look).scale(wl * wl * dt)).subtract(lookVel.scale(2 * wl * dt));
			look = look.add(lookVel.scale(dt));
		}
		int ground = level.getHeight(Heightmap.Types.MOTION_BLOCKING, Mth.floor(pos.x), Mth.floor(pos.z));
		if (pos.y < ground + 0.7) {
			pos = new Vec3(pos.x, ground + 0.7, pos.z);
		}
		// never inside anyone: out of the way of a soldier walking past (side to side, to just clear of them)
		for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(pos, pos).inflate(3), e -> e.isAlive() && !(e instanceof Player))) {
			AABB box = e.getBoundingBox().inflate(CLEAR);
			if (box.contains(pos)) {
				Vec3 c = box.getCenter(), away = new Vec3(pos.x - c.x, 0, pos.z - c.z);
				away = away.lengthSqr() < 1e-6 ? new Vec3(1, 0, 0) : away.normalize();
				double r = Math.max(box.getXsize(), box.getZsize()) * 0.5 * Math.sqrt(2) + 0.02;
				pos = new Vec3(c.x + away.x * r, pos.y, c.z + away.z * r);
			}
		}
		Vec3 d = look.subtract(pos);
		float ty = (float) Math.toDegrees(Math.atan2(-d.x, d.z));
		if (!Float.isNaN(yaw)) {
			ty = yaw + Mth.wrapDegrees(ty - yaw);
		}
		yaw = ty;
		pitch = Mth.clamp((float) -Math.toDegrees(Math.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z))), -90, 90);
	}

	/** Where to look while the pods come down: their middle, the ones falling counting most, the ones down a little. */
	private Vec3 podsLook(List<DropPod> pods, long age) {
		if (age < 0 || pods.isEmpty()) {
			return flare();
		}
		double x = 0, y = 0, z = 0, total = 0;
		Vector3f at = new Vector3f();
		for (DropPod d : pods) {
			double w;
			Vec3 p;
			if (age < d.land()) {
				d.at(age, at);
				p = d.position().add(at.x, at.y, at.z);
				w = age < d.setsOff() ? 0.25 : 1 + 2.0 * (age - d.setsOff()) / JugcraftSquads.FALL_TICKS;
			} else {
				p = d.position().add(0, 1.2, 0);
				w = 0.5;
			}
			x += p.x * w;
			y += p.y * w;
			z += p.z * w;
			total += w;
		}
		return total <= 0 ? flare() : new Vec3(x / total, y / total, z / total);
	}

	/** The middle of what's going on on the ground: the pods still there and the soldiers. */
	private Vec3 crowd(Level level, List<DropPod> pods) {
		double x = 0, y = 0, z = 0;
		int n = 0;
		for (DropPod d : pods) {
			x += d.getX();
			y += d.getY() + 1.4;
			z += d.getZ();
			n++;
		}
		for (Legionary l : level.getEntitiesOfClass(Legionary.class, new AABB(s.mark, s.mark).inflate(140, 60, 140), LivingEntity::isAlive)) {
			x += l.getX();
			y += l.getY() + 1.2;
			z += l.getZ();
			n++;
		}
		return n == 0 ? s.mark.add(0, 1, 0) : new Vec3(x / n, y / n, z / n);
	}

	/** The Warden's way toward the squad's spot (kept once it's among them). */
	private void aimAtSquad(Vec3 c) {
		Vec3 d = s.anchor.subtract(c).multiply(1, 0, 1);
		if (wardenFwd == null || d.length() > 7) {
			wardenFwd = d.lengthSqr() < 1e-4 ? s.across.scale(-s.side) : d.normalize();
		}
	}

	/** A point round {@code c}: {@code angle} degrees from its way toward the squad (+ to its left), {@code r} out, {@code up} up. */
	private Vec3 round(Vec3 c, double angle, double r, double up) {
		Vec3 f = wardenFwd, left = new Vec3(f.z, 0, -f.x);
		double a = Math.toRadians(angle);
		return c.add(f.scale(Math.cos(a) * r)).add(left.scale(Math.sin(a) * r)).add(0, up, 0);
	}

	/** One tick of the orbit's spring (speed {@code w}) toward {@code target}. */
	private double spring1(double x, double target, double w) {
		double dt = 0.05;
		orbitVel += ((target - x) * w * w - 2 * w * orbitVel) * dt;
		return x + orbitVel * dt;
	}

	private static double smooth(double x) {
		return x * x * (3 - 2 * x);
	}

	/** The camera now, for the record. */
	public String tag(long now) {
		return String.format(Locale.ROOT, "%s %d %.2f %.2f %.2f", shot, now, pos.x, pos.y, pos.z);
	}
}
