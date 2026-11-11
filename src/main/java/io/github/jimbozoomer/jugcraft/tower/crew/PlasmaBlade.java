package io.github.jimbozoomer.jugcraft.tower.crew;

import java.util.function.IntConsumer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

/**
 * The plasma blade in the Narv-Tech power armour's right arm (Justin, 9 November 2026, of the power armour sheets: "Build
 * it with the armour"; "Have it do the rifles burn through thing for a thrust, but the batons burn through thing for a
 * slash depending the move they perform"): the security detail and the drop squads' legionaries light it when what
 * they're fighting gets within {@value #REACH} blocks, too close to shoot. It comes out of its arm mount in
 * {@value #DEPLOY_TICKS} ticks (the shutters swing out, the spine drives out, the plasma runs up it: client/PowerArmor
 * draws that), the rifle dropped to the left hand; then a move about once a second: a thrust at arm's length
 * ({@value #SLASH_WITHIN} blocks or more: a lunge, {@value #THRUST_DAMAGE} damage, the rifle's wound, a tunnel right
 * through), a slash closer in ({@value #SLASH_DAMAGE} damage; since r196 its wound is the blade's own sweep, on
 * everything in it: PlasmaWounds.Swing). With nothing in reach for
 * {@value #STOW_AFTER} ticks it goes out and they go back to the rifle. Its sounds are the plasma baton's, deeper. The
 * idea of a blade in the suit's arm is from the Undying Mercenaries notes; nothing else of the series is used.
 */
public final class PlasmaBlade {
	/**
	 * How near (blocks, to the target's side) lights the blade; nearer than {@link #SLASH_WITHIN} a move is a slash; a
	 * thrust is thrown from within {@link #THRUST_WITHIN} (the lunge takes the point in); they close to {@link #CLOSE_TO};
	 * and a blow lands on what's within {@link #LANDS_WITHIN} when it falls (the blade's own reach and a little).
	 */
	public static final double REACH = 3.0, SLASH_WITHIN = 1.2, THRUST_WITHIN = 2.2, CLOSE_TO = 0.9, LANDS_WITHIN = 1.6;
	/**
	 * The blade's sweep, as the cut follows it (PlasmaWounds.Swing; r196): the plasma burns from {@value #INNER} to
	 * {@value #OUTER} blocks out from the shoulder (the mount's mouth to the point, at the suit's size), a slash comes down
	 * across {@value #SLASH_HALF} degrees either side of its aim, tilted {@value #SLASH_TILT} degrees to the wearer's left
	 * (from over the right shoulder down across to the left hip: client/TowerCrewModel.bladePose), its edge
	 * {@value #EDGE} px thick each side.
	 */
	public static final float INNER = 0.60F, OUTER = 1.29F, SLASH_HALF = 55F, SLASH_TILT = 35F, EDGE = 1.0F;
	public static final float THRUST_DAMAGE = PlasmaRifle.DAMAGE, SLASH_DAMAGE = PlasmaBaton.DAMAGE;
	/** Ticks: out of its mount; a move, and when its blow lands; between moves; nothing in reach before it's stowed. */
	public static final int DEPLOY_TICKS = 12, MOVE_TICKS = 10, MOVE_HIT = 5, COOLDOWN = 10, STOW_AFTER = 20;
	/** The baton's sounds played this much lower. */
	public static final float PITCH = 0.75F;
	private static final int THRUST = 1, SLASH = 2;

	private PlasmaBlade() {
	}

	/** Who wears the blade: what they're doing ({@link TowerCrew#ACT_BLADE} and the rest), for the client and for telling
	 * a blade's blow from a fist's ({@link PlasmaWounds#plasma}). */
	public interface Wearer {
		int act();
	}

	/** Whether {@code act} is one with the blade out: on guard, a thrust, a slash. */
	public static boolean out(int act) {
		return act == TowerCrew.ACT_BLADE || act == TowerCrew.ACT_THRUST || act == TowerCrew.ACT_SLASH;
	}

	/** How far {@code self} is from {@code target}'s side (blocks). */
	public static double gap(LivingEntity self, LivingEntity target) {
		return Math.sqrt(self.distanceToSqr(target)) - target.getBbWidth() / 2;
	}

	static void play(ServerLevel level, LivingEntity at, SoundEvent sound, float volume) {
		level.playSound(null, at.getX(), at.getY() + 1.0, at.getZ(), sound, SoundSource.HOSTILE, volume, PITCH * (0.97F + level.getRandom().nextFloat() * 0.06F));
	}

	/** A wearer's blade through a fight (their rifle goal keeps one). */
	public static final class Fight {
		private boolean out;
		private int since, move, moveT, cooldown, away;

		public boolean out() {
			return out;
		}

		/**
		 * A tick of the fight with {@code target} ({@code sees}: in sight; {@code mayClose}: free to step in at it):
		 * lights the blade when it's in reach, makes its moves, stows it when nothing's been in reach a while. Sets what
		 * they're doing through {@code act}. True while the blade has the fight (the rifle isn't fired then).
		 */
		public boolean tick(ServerLevel level, Mob self, LivingEntity target, boolean sees, boolean mayClose, IntConsumer act) {
			double gap = gap(self, target);
			// held to their post, only what comes within the blade's own reach
			boolean near = sees && gap <= (mayClose ? REACH : OUTER);
			if (!out) {
				if (!near) {
					return false;
				}
				out = true;
				since = move = moveT = away = 0;
				cooldown = 2;
				act.accept(TowerCrew.ACT_BLADE);
				play(level, self, PlasmaBaton.IGNITE, 1.0F);
			}
			since++;
			float yaw = (float) (Mth.atan2(target.getZ() - self.getZ(), target.getX() - self.getX()) * Mth.RAD_TO_DEG) - 90F;
			self.setYRot(yaw);
			self.setYBodyRot(yaw);
			if (move != 0) {
				if (++moveT == MOVE_HIT) {
					land(level, self, target, sees);
				}
				if (moveT >= MOVE_TICKS) {
					move = 0;
					cooldown = COOLDOWN;
					act.accept(TowerCrew.ACT_BLADE);
				}
				return true;
			}
			act.accept(TowerCrew.ACT_BLADE);
			if (near) {
				away = 0;
			} else if (++away > STOW_AFTER) {
				stow(level, self);
				return false;
			}
			boolean ready = since >= DEPLOY_TICKS;
			if (ready && near && gap <= (mayClose ? THRUST_WITHIN : OUTER) && --cooldown <= 0) {
				move = gap >= SLASH_WITHIN ? THRUST : SLASH;
				moveT = 0;
				act.accept(move == THRUST ? TowerCrew.ACT_THRUST : TowerCrew.ACT_SLASH);
				play(level, self, PlasmaBaton.SWING, 1.0F);
				self.getNavigation().stop();
				if (mayClose && gap > CLOSE_TO) {
					// the lunge (a thrust), a step in (a slash): in behind the blade, so that it goes where it's aimed (a push
					// that carries them to a slash's length by the time the blow lands: 2.1 times it, over five ticks)
					Vec3 to = new Vec3(target.getX() - self.getX(), 0, target.getZ() - self.getZ());
					if (to.lengthSqr() > 1e-4) {
						Vec3 push = to.normalize().scale(Math.min(0.6, (gap - CLOSE_TO) / 2.1));
						self.setDeltaMovement(self.getDeltaMovement().add(push.x, 0, push.z));
					}
				}
				return true;
			}
			// once it's out, in to a slash's length and no nearer (they stand while it deploys: the first move at arm's
			// length is the thrust)
			if (mayClose && ready && gap > CLOSE_TO) {
				if (self.getNavigation().isDone() || since % 10 == 1) {
					self.getNavigation().moveTo(target, 1.15);
				}
			} else {
				self.getNavigation().stop();
			}
			return true;
		}

		/** The blow, if it's still there to take it: its damage, the sear, and the wound its move leaves. */
		private void land(ServerLevel level, Mob self, LivingEntity target, boolean sees) {
			if (!target.isAlive() || !sees || gap(self, target) > LANDS_WITHIN) {
				return;
			}
			float damage = move == THRUST ? THRUST_DAMAGE : SLASH_DAMAGE;
			net.minecraft.world.damagesource.DamageSource source = self.damageSources().mobAttack(self);
			if (target.hurtServer(level, source, damage)) {
				self.setLastHurtMob(target);
			}
			PlasmaBaton.hitEffects(level, target, SoundSource.HOSTILE);
			// the cut is the blade's own path, through it and any foe in the way (PlasmaWounds.sweep, thrust)
			if (move == SLASH) {
				PlasmaWounds.sweep(level, self, target, PlasmaWounds.swing(self, target, null, SLASH_TILT, INNER, OUTER, SLASH_HALF, EDGE), source, damage);
			} else {
				PlasmaWounds.thrust(level, self, target, null, OUTER, PlasmaWounds.THRUST_RADIUS, source, damage);
			}
		}

		/** Puts it out (their goal's end, or nothing left in reach). */
		public void stow(ServerLevel level, Mob self) {
			if (out) {
				out = false;
				move = 0;
				play(level, self, PlasmaBaton.OFF, 0.9F);
			}
		}
	}
}
