package io.github.jimbozoomer.jugcraft.walker;

import io.github.jimbozoomer.jugcraft.artillery.ArtilleryShell;
import io.github.jimbozoomer.jugcraft.artillery.JugcraftArtillery;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * The Armoured Walker (batch 58, docs/features/armoured-walker.md), after the owner's own Blender model: a squat walker
 * in riveted blue-grey plate with a hull cannon and a piston ram. It walks, turns, climbs, jumps and burns fuel exactly
 * as the {@link DieselWalker} does. Holding use fires the hull cannon where the pilot looks, a Heavy Shell every
 * {@value #CANNON_COOLDOWN} ticks from the pilot's own (none in creative): a damage-only blast, as every gun's.
 * Attack rams the piston arm into whatever is in front ({@value #RAM_DAMAGE} damage, thrown hard). The pilot sits in a
 * roof hatch, head out. It is tougher than the Diesel Walker ({@value #HEALTH} to knock down) but has no drill.
 */
public class ArmouredWalker extends DieselWalker {
	public static final int CANNON_COOLDOWN = 40;
	public static final double CANNON_SPEED = 2.4;
	public static final int RAM_DAMAGE = 16;
	public static final double RAM_KNOCKBACK = 1.6;
	public static final double RAM_REACH = 2.6;
	public static final int RAM_COOLDOWN = 24;
	public static final int HEALTH = 90;
	public static final float WIDTH = 2.6F;
	public static final float HEIGHT = 4.6F;
	/** The muzzle, in blocks before turning with the walker (MUZZLE in tools/armoured_walker.py, in pixels, over 16). */
	public static final Vec3 MUZZLE = new Vec3(0, 3.5, 1.8125);
	/** The roof hatch the pilot sits in. */
	private static final Vec3 HATCH = new Vec3(0, 3.3, -0.2);
	private int lastShot = -CANNON_COOLDOWN;
	private boolean warned;

	public ArmouredWalker(EntityType<? extends ArmouredWalker> type, Level level) {
		super(type, level);
	}

	/** Holding use: fire the cannon, when it is loaded, toward where the pilot looks. Never counts as drilling. */
	@Override
	protected boolean useHeld(ServerLevel level, ServerPlayer pilot) {
		if (tickCount - lastShot < CANNON_COOLDOWN) {
			return false;
		}
		if (!pilot.getAbilities().instabuild) {
			int slot = pilot.getInventory().findSlotMatchingItem(new ItemStack(JugcraftArtillery.HEAVY_SHELL_ITEM));
			if (slot < 0) {
				if (!warned) {
					pilot.sendOverlayMessage(Component.translatable("message.jugcraft.artillery.no_shells",
							new ItemStack(JugcraftArtillery.HEAVY_SHELL_ITEM).getHoverName()));
					warned = true;
				}
				return false;
			}
			pilot.getInventory().removeItem(slot, 1);
		}
		warned = false;
		lastShot = tickCount;
		Vec3 muzzle = position().add(MUZZLE.yRot(-getYRot() * Mth.DEG_TO_RAD));
		Vec3 aim = pilot.getViewVector(1.0F);
		ArtilleryShell shell = new ArtilleryShell(JugcraftArtillery.HEAVY_SHELL, level, pilot);
		shell.setPos(muzzle);
		shell.shoot(aim.x, aim.y, aim.z, (float) CANNON_SPEED, 0.5F);
		level.addFreshEntity(shell);
		level.sendParticles(ParticleTypes.LARGE_SMOKE, muzzle.x, muzzle.y, muzzle.z, 10, 0.25, 0.25, 0.25, 0.03);
		level.sendParticles(ParticleTypes.FLAME, muzzle.x, muzzle.y, muzzle.z, 5, 0.1, 0.1, 0.1, 0.02);
		level.playSound(null, muzzle.x, muzzle.y, muzzle.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 2.0F, 0.8F);
		return false;
	}

	/** Attack: the piston ram, into everything in a box just in front. */
	@Override
	protected void attack(ServerLevel level, ServerPlayer pilot, Vec3 heading) {
		Vec3 centre = position().add(heading.scale(RAM_REACH)).add(0, 1.6, 0);
		AABB reach = new AABB(centre, centre).inflate(1.5);
		for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, reach, e -> e != pilot && e.isAlive())) {
			if (target.hurtServer(level, level.damageSources().playerAttack(pilot), RAM_DAMAGE)) {
				target.setDeltaMovement(target.getDeltaMovement().add(heading.x * RAM_KNOCKBACK, 0.5, heading.z * RAM_KNOCKBACK));
				if (target instanceof ServerPlayer hit) {
					hit.connection.send(new net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket(hit));
				}
			}
		}
		level.playSound(null, centre.x, centre.y, centre.z, SoundEvents.PISTON_EXTEND, SoundSource.PLAYERS, 1.2F, 0.6F);
		level.playSound(null, centre.x, centre.y, centre.z, SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.5F, 0.7F);
	}

	@Override
	protected int attackCooldown() {
		return RAM_COOLDOWN;
	}

	@Override
	protected int health() {
		return HEALTH;
	}

	@Override
	protected ItemStack dropStack() {
		return new ItemStack(JugcraftWalkers.ARMOURED_WALKER_ITEM);
	}

	@Override
	protected Vec3 seat() {
		return HATCH;
	}
}
