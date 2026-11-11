package io.github.jimbozoomer.jugcraft.tower.crew;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.util.List;
import org.jspecify.annotations.Nullable;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * The plasma rifle (Justin approved the plasma rifle sheet, with the deeper sound, on 6 November 2026): what the tower's
 * security carry, in their power armour ({@link TowerCrew#GEAR_EXO}). They hold it at the low ready on duty, and stand,
 * raise it and fire at hostile mobs they can see within {@value #RANGE} blocks and at a player who hurts any of the crew
 * ({@link TowerCrew}'s rifle goal). A shot is a {@link PlasmaBolt}: fast, {@value #DAMAGE} damage, stopped by the first
 * thing it hits. Players can have one from the creative menu ({@link PlasmaRifleItem}: the NT-7, which works like the
 * guns); it isn't craftable. The shot and
 * the hit are Justin's picks from a sound library he subscribes to (tools/plasma_rifle.py cuts and writes them).
 */
public final class PlasmaRifle {
	/** Damage a bolt does (Justin, 7 November 2026: "I want it to like one shot a zombie", for everyone who carries it; it
	 * was 10, "dropping in like 2 hits"): 24, a zombie's 20 even through a little armour. */
	public static final float DAMAGE = 24.0F;
	/** A bolt's speed (blocks a tick) and how long it lives (ticks: 60 blocks at most). */
	public static final float SPEED = 3.0F;
	public static final int LIFE = 20;
	/** How far security see and fire, in blocks. */
	public static final double RANGE = 24.0;
	/** Ticks between shots (security's and a player's). */
	public static final int COOLDOWN = 20;
	/** Ticks security keep after a player who hurt one of the crew (unless the player gets out of range first). */
	public static final int ANGER_TICKS = 600;

	public static SoundEvent FIRE, HIT;
	public static EntityType<PlasmaBolt> BOLT;
	public static Item RIFLE;

	private PlasmaRifle() {
	}

	public static void register() {
		FIRE = sound("plasma_rifle.fire");
		HIT = sound("plasma_bolt.hit");
		PlasmaWounds.register();                    // the holes plasma burns (the rifle's bolts and the baton)
		ResourceKey<EntityType<?>> boltKey = ResourceKey.create(Registries.ENTITY_TYPE, Jugcraft.id("plasma_bolt"));
		BOLT = Registry.register(BuiltInRegistries.ENTITY_TYPE, boltKey, EntityType.Builder.<PlasmaBolt>of(PlasmaBolt::new, MobCategory.MISC)
				.sized(0.25F, 0.25F).clientTrackingRange(8).updateInterval(1).noSummon().build(boltKey));
		ResourceKey<Item> rifleKey = ResourceKey.create(Registries.ITEM, Jugcraft.id("plasma_rifle"));
		RIFLE = Registry.register(BuiltInRegistries.ITEM, rifleKey,
				new PlasmaRifleItem(new Item.Properties().setId(rifleKey).stacksTo(1).rarity(Rarity.EPIC)
						// aiming down its sight walks as aiming a gun does (P6, Justin's pick: it works like the guns)
						.component(net.minecraft.core.component.DataComponents.USE_EFFECTS,
								new net.minecraft.world.item.component.UseEffects(false, true, PlasmaRifleItem.AIM_SPEED))));
		PlasmaRifleItem.register();
		// the creative menu only: no recipe
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.COMBAT).register(output -> output.accept(RIFLE));
	}

	private static SoundEvent sound(String path) {
		Identifier id = Jugcraft.id(path);
		return Registry.register(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id));
	}

	/**
	 * Fires a bolt from {@code from} at {@code at} for {@code shooter} (its target, if a crew member's), and the shot's
	 * sound for everyone near; returns the bolt. The crew's have a little spread.
	 */
	public static PlasmaBolt fire(ServerLevel level, LivingEntity shooter, Vec3 from, Vec3 at, @Nullable Entity target) {
		return fire(level, shooter, from, at, target, 0.6F);
	}

	/** As {@link #fire(ServerLevel, LivingEntity, Vec3, Vec3, Entity)}, with this much spread (a player's: none). */
	public static PlasmaBolt fire(ServerLevel level, LivingEntity shooter, Vec3 from, Vec3 at, @Nullable Entity target, float spread) {
		PlasmaBolt bolt = new PlasmaBolt(level, shooter, from, target);
		Vec3 d = at.subtract(from);
		bolt.shoot(d.x, d.y, d.z, SPEED, spread);
		level.addFreshEntity(bolt);
		level.playSound(null, from.x, from.y, from.z, FIRE, shooter instanceof Player ? SoundSource.PLAYERS : SoundSource.HOSTILE, 2.0F,
				0.94F + level.getRandom().nextFloat() * 0.12F);
		return bolt;
	}

	/** Ticks a shooter waits with someone in the way (a soldier of the same squad, one of the crew, a player: often the
	 * very player the mob is on) before moving to where the line is clear. */
	public static final int BLOCKED_TICKS = 30;
	/** Where they look for a clear line: so far to either side of the line to the target (blocks), and so far toward it. */
	private static final double[] ACROSS = {1.5, 2.5, 3.5, 5.0, 6.5};
	private static final double[] TOWARD = {0.0, 1.5, -1.5};

	/**
	 * Where {@code shooter} goes with someone in the line of fire at {@code target} (Justin, 11 November 2026: mobs on him,
	 * the guards beside him doing nothing): the nearest place to either side of the line (a step toward it or back as
	 * well) with room to stand, ground under it and a clear shot from there; or, none found, a plain step to {@code side}.
	 */
	public static Vec3 aside(ServerLevel level, net.minecraft.world.entity.Mob shooter, LivingEntity target, int side) {
		Vec3 at = shooter.position();
		Vec3 d = target.position().subtract(at).multiply(1, 0, 1);
		d = d.lengthSqr() < 1.0E-4 ? new Vec3(0, 0, 1) : d.normalize();
		Vec3 across = new Vec3(-d.z, 0, d.x);
		Vec3 aim = new Vec3(target.getX(), target.getY(0.6), target.getZ());
		double eye = shooter.getEyeY() - shooter.getY();
		for (double a : ACROSS) {
			for (double t : TOWARD) {
				for (int s : new int[] {side, -side}) {
					Vec3 p = at.add(across.scale(a * s)).add(d.scale(t));
					net.minecraft.core.BlockPos cell = net.minecraft.core.BlockPos.containing(p);
					if (!level.getBlockState(cell.below()).isFaceSturdy(level, cell.below(), net.minecraft.core.Direction.UP)
							|| !level.noCollision(shooter, shooter.getBoundingBox().move(p.subtract(at)))) {
						continue;
					}
					Vec3 from = p.add(0, eye, 0);
					Vec3 muzzle = from.add(aim.subtract(from).normalize()).add(0, -0.12, 0);
					if (clearShot(level, shooter, muzzle, aim, target) && level.clip(new net.minecraft.world.level.ClipContext(from, aim,
							net.minecraft.world.level.ClipContext.Block.COLLIDER, net.minecraft.world.level.ClipContext.Fluid.NONE, shooter)).getType()
							== net.minecraft.world.phys.HitResult.Type.MISS) {
						return p;
					}
				}
			}
		}
		return at.add(across.scale(2.5 * side)).add(d);
	}

	/** Whether the line from {@code from} to {@code to} is clear of the crew and of players other than {@code target}. */
	public static boolean clearShot(ServerLevel level, Entity shooter, Vec3 from, Vec3 to, Entity target) {
		AABB box = new AABB(from, to).inflate(0.6);
		List<Entity> near = level.getEntities(shooter, box, e -> e != target && (e instanceof TowerCrew || e instanceof io.github.jimbozoomer.jugcraft.tower.squad.Legionary
				|| e instanceof Player p && !p.isSpectator()));
		for (Entity e : near) {
			if (e.getBoundingBox().inflate(0.3).clip(from, to).isPresent()) {
				return false;
			}
		}
		return true;
	}
}
