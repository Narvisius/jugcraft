package io.github.jimbozoomer.jugcraft.lair.dreadnought;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.jspecify.annotations.Nullable;

/**
 * The Grounding Strap (docs/features/iron-dreadnought.md): a braided copper strap and clamp. Held in the offhand, harm of
 * the damage type tag {@code #jugcraft:electric} (lightning, a live power line's electrocution, a Tesla gun's zap, and the
 * Iron Dreadnought's arcs: his Coil Arc, his unrouted discharge and his Sparkers) does {@value #SHARE} of itself, after
 * armour (mixin/GroundingStrapMixin.java). It does nothing against any other harm. Decided on the server, as the damage
 * is; the work is bounded: a tag test and a look at the offhand, as each blow lands.
 */
public final class GroundingStrap {
	/** What share of electric harm gets through (tools/iron_dreadnought.py STRAP_SHARE). */
	public static final float SHARE = 0.5F;
	/** His arcs' damage type, and the electric harm the strap halves. */
	public static final ResourceKey<DamageType> ARC = ResourceKey.create(Registries.DAMAGE_TYPE, Jugcraft.id("arc"));
	public static final TagKey<DamageType> ELECTRIC = TagKey.create(Registries.DAMAGE_TYPE, Jugcraft.id("electric"));

	private GroundingStrap() {
	}

	/** Whether {@code living} holds a Grounding Strap in their offhand. */
	public static boolean holds(LivingEntity living) {
		return JugcraftDreadnought.GROUNDING_STRAP != null && living.getOffhandItem().is(JugcraftDreadnought.GROUNDING_STRAP);
	}

	/** The harm {@code amount} of {@code source} does to {@code living}: halved if it is electric and they hold the strap. */
	public static float scale(LivingEntity living, DamageSource source, float amount) {
		return source.is(ELECTRIC) && holds(living) ? amount * SHARE : amount;
	}

	/** An electric arc from {@code direct}, set off by {@code causing}. */
	public static DamageSource arc(ServerLevel level, Entity direct, @Nullable Entity causing) {
		return new DamageSource(level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(ARC), direct, causing);
	}
}
