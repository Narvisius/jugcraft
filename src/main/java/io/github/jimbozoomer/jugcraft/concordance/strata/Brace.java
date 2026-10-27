package io.github.jimbozoomer.jugcraft.concordance.strata;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.effect.EffectKind;
import io.github.jimbozoomer.jugcraft.concordance.effect.EffectSpec;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.jspecify.annotations.Nullable;

/**
 * Brace's Braced status (tools/concordance_strata.py): +{@value #RESISTANCE} knockback resistance a level, exclusive, for
 * 5 s. Not the war fork's BRACE trait nor a tower shield's brace (weapons/JugcraftArms). It is spent by the first push it
 * meets: a blow vanilla knocks back (full damage, not #minecraft:no_knockback, not wholly stopped by a shield), a harmful
 * Concordance push, or a scripted push that asks ({@link #spend}; the Yeti King's, B-09, and the Leaf Blower's stream).
 * Spent, it still braces for the rest of that server tick and goes when the tick ends, so a whole blow is braced whatever
 * order its parts come in; spent between ticks (a push from a cast's packet), it goes when the next tick starts. Helpful
 * pushes (one's own Flashstep) neither feel nor spend it ({@link #resistanceWithout}). Teleports, portals and a lair's
 * mist ignore it. Who put it on is kept beside it, not saved ({@link #BRACER}).
 */
public final class Brace {
	public static final String STATUS = "jugcraft:braced";
	public static final double RESISTANCE = 0.5;
	public static Holder<MobEffect> BRACED;
	/** Who put the Brace on: not saved, so a Brace carried through a relog credits nobody. */
	public static AttachmentType<UUID> BRACER;
	/** Braces spent this tick (server thread only). */
	private static final Set<LivingEntity> SPENT = Collections.newSetFromMap(new IdentityHashMap<>());

	private Brace() {
	}

	private static final class Status extends MobEffect {
		Status(MobEffectCategory category, int colour) {
			super(category, colour);
		}
	}

	static void register() {
		BRACED = Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT, Jugcraft.id("braced"),
				new Status(MobEffectCategory.BENEFICIAL, 0x8A7F72).addAttributeModifier(Attributes.KNOCKBACK_RESISTANCE, Jugcraft.id("braced"),
						RESISTANCE, AttributeModifier.Operation.ADD_VALUE));
		BRACER = AttachmentRegistry.<UUID>builder().buildAndRegister(Jugcraft.id("brace_caster"));
		// Vanilla knocks back only a blow that took full damage this tick (hurtTime set to hurtDuration), may knock back,
		// and was not wholly stopped by a shield (mc263src LivingEntity.java:1179-1205).
		ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, baseDamage, damage, blocked) -> {
			if (!source.is(DamageTypeTags.NO_KNOCKBACK) && !(blocked && damage <= 0.0F) && entity.hurtTime > 0
					&& entity.hurtTime == entity.hurtDuration) {
				spend(entity);
			}
		});
		// Packets run between ticks, after END_SERVER_TICK (mc263src MinecraftServer.java:836-837): a Brace a cast's push
		// spent there goes before the next tick's blows, not at that tick's end.
		ServerTickEvents.START_SERVER_TICK.register(server -> settle());
		ServerTickEvents.END_SERVER_TICK.register(server -> settle());
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> SPENT.clear());
	}

	/** After a status took (ConcordanceEffects): a Brace remembers who put it on, and that is Stratawrighting practice. */
	public static void braced(@Nullable ServerPlayer person, LivingEntity target, EffectSpec effect) {
		if (effect.kind() != EffectKind.STATUS || !STATUS.equals(effect.status())) {
			return;
		}
		if (person != null) {
			target.setAttached(BRACER, person.getUUID());
		} else {
			target.removeAttached(BRACER);
		}
		Strata.braced(person, target);
	}

	/**
	 * A push about to land on {@code target}, met by its Brace if it has one: spends it (it braces the rest of this tick)
	 * and returns the share to let through, 1 - RESISTANCE a level, or 1 unbraced. Safe more than once a blow.
	 */
	public static double spend(LivingEntity target) {
		MobEffectInstance brace = BRACED == null ? null : target.getEffect(BRACED);
		if (brace == null) {
			return 1.0;
		}
		if (SPENT.add(target)) {
			held(target);
		}
		return Math.max(0.0, 1.0 - RESISTANCE * (brace.getAmplifier() + 1));
	}

	/** Who put {@code target}'s Brace on, if known (B-09's deed). */
	public static @Nullable UUID caster(LivingEntity target) {
		return target.getAttached(BRACER);
	}

	/** Knockback resistance without the Brace: its own modifiers off for the reading and back at once (as vanilla does). */
	public static double resistanceWithout(LivingEntity entity) {
		MobEffectInstance brace = BRACED == null ? null : entity.getEffect(BRACED);
		if (brace == null) {
			return entity.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE);
		}
		MobEffect effect = BRACED.value();
		effect.removeAttributeModifiers(entity.getAttributes());
		double without = entity.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE);
		effect.addAttributeModifiers(entity.getAttributes(), brace.getAmplifier());
		return without;
	}

	private static void held(LivingEntity target) {
		UUID caster = target.getAttached(BRACER);
		if (caster != null && target.level() instanceof ServerLevel level) {
			Strata.held(level.getServer().getPlayerList().getPlayer(caster));
		}
	}

	static void settle() {
		if (SPENT.isEmpty()) {
			return;
		}
		for (LivingEntity entity : List.copyOf(SPENT)) {
			entity.removeEffect(BRACED);
			entity.removeAttached(BRACER);
		}
		SPENT.clear();
	}
}
