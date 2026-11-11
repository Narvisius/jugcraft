package io.github.jimbozoomer.jugcraft.tower.crew;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.phys.Vec3;

/**
 * The plasma baton (Justin approved the baton and take 12 of its sounds on 6 November 2026): what the tower's gate guards
 * carry, holstered down the back of the right thigh ({@link TowerCrew#GEAR_BATON}), its rod telescoped into the handle.
 * At a hostile mob, or at a player who hurts any of the crew near them, they draw it (the rod thrown out as the arm comes
 * up, then the plasma wrapping it as it lights), go in and strike, {@value #DAMAGE} damage a blow, and put it away when
 * it's over ({@link TowerCrew}'s baton goal). Rebuilt as real 3D gear on 10 November 2026 (he picked A, "Patrol":
 * tools/plasma_baton.py makes the item, which is what the guards carry). Players can have one from the creative
 * menu ({@link PlasmaBatonItem}); it isn't craftable. The sounds are made from two sounds out of a library Justin
 * subscribes to (tools/plasma_baton.py).
 */
public final class PlasmaBaton {
	/** Damage a blow does: three quarters of the rifle's bolt (Justin, 8 November 2026: "about 3/4 of the rifle"; was 7). */
	public static final float DAMAGE = PlasmaRifle.DAMAGE * 0.75F;
	/** The draw: the hand to the holster (the catch at 4 ticks), up to the guard, the rod thrown out on the way (6); the
	 * plasma lights over ticks 8-13. */
	public static final int DRAW_TICKS = 13, DRAW_CATCH = 4, DRAW_OUT = 6, DRAW_IGNITE = 7;
	/** A strike: up over the shoulder and down; the blow lands at {@value #STRIKE_HIT} ticks. Then a breath before the next. */
	public static final int STRIKE_TICKS = 8, STRIKE_HIT = 4, COOLDOWN = 12;
	/** Putting it away: the plasma out over 4 ticks, the hand down to the holster (the rod back in the handle at 6), in
	 * at 8, the arm back by 12. */
	public static final int HOLSTER_TICKS = 12, HOLSTER_IN = 6;
	/** Ticks with the target gone from sight before they give up and put it away. */
	public static final int UNSEEN_TICKS = 100;
	/**
	 * A swing of it, as the cut follows it (PlasmaWounds.Swing): the plasma burns from {@value #SWING_INNER} to
	 * {@value #SWING_OUTER} blocks out from the shoulder (the hand 0.59 out, the plasma from 0.285 to 0.727 along the rod
	 * across it: tools/plasma_baton.py's ROD_FROM and TIP at the crew's scale), over {@value #SWING_HALF} degrees either
	 * side of where it's aimed; its half-thickness (px).
	 */
	public static final float SWING_INNER = 0.66F, SWING_OUTER = 0.94F, SWING_HALF = 50F, EDGE = 1.25F;

	public static SoundEvent IGNITE, HUM, SWING, HIT, OFF, CATCH;
	public static Item BATON;

	private static final int CORE = 0xE6D2FF, GLOW = 0xA060FF;

	private PlasmaBaton() {
	}

	public static void register() {
		IGNITE = sound("plasma_baton.ignite");
		HUM = sound("plasma_baton.hum");
		SWING = sound("plasma_baton.swing");
		HIT = sound("plasma_baton.hit");
		OFF = sound("plasma_baton.off");
		CATCH = sound("plasma_baton.catch");
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Jugcraft.id("plasma_baton"));
		// a sword's speed, its blow the guards' (DAMAGE: the hand's 1 and diamond's 3 under it)
		BATON = Registry.register(BuiltInRegistries.ITEM, key, new PlasmaBatonItem(new Item.Properties().setId(key).stacksTo(1).rarity(Rarity.EPIC)
				.sword(ToolMaterial.DIAMOND, DAMAGE - 1.0F - ToolMaterial.DIAMOND.attackDamageBonus(), -2.4F)));
		// the creative menu only: no recipe
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.COMBAT).register(output -> output.accept(BATON));
	}

	private static SoundEvent sound(String path) {
		Identifier id = Jugcraft.id(path);
		return Registry.register(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id));
	}

	/** A sound from {@code at}, for everyone near. */
	public static void play(ServerLevel level, Entity at, SoundEvent sound, float volume) {
		level.playSound(null, at.getX(), at.getY() + 1.0, at.getZ(), sound, SoundSource.HOSTILE, volume, 0.95F + level.getRandom().nextFloat() * 0.1F);
	}

	/** A blow landing on {@code target}: the sear's sound, purple sparks and a wisp of smoke where it went in. */
	public static void hitEffects(ServerLevel level, Entity target, SoundSource source) {
		Vec3 at = new Vec3(target.getX(), target.getY(0.6), target.getZ());
		level.sendParticles(new DustParticleOptions(GLOW, 1.0F), at.x, at.y, at.z, 10, 0.18, 0.2, 0.18, 0.02);
		level.sendParticles(new DustParticleOptions(CORE, 0.7F), at.x, at.y, at.z, 6, 0.12, 0.15, 0.12, 0.02);
		level.sendParticles(ParticleTypes.ELECTRIC_SPARK, at.x, at.y, at.z, 6, 0.15, 0.15, 0.15, 0.3);
		level.sendParticles(ParticleTypes.SMOKE, at.x, at.y, at.z, 4, 0.1, 0.1, 0.1, 0.01);
		level.playSound(null, at.x, at.y, at.z, HIT, source, 1.0F, 0.95F + level.getRandom().nextFloat() * 0.1F);
	}
}
