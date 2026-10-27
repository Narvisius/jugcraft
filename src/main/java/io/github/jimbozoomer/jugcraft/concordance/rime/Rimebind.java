package io.github.jimbozoomer.jugcraft.concordance.rime;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.ConcordanceEffects;
import io.github.jimbozoomer.jugcraft.concordance.effect.EffectKind;
import io.github.jimbozoomer.jugcraft.concordance.effect.EffectSpec;
import io.github.jimbozoomer.jugcraft.concordance.effect.Tolerance;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import org.jspecify.annotations.Nullable;

/**
 * Rimebind (tools/concordance_rime.py). Rimebound ({@code jugcraft:rimebound}) takes {@value #SLOW} of movement speed a
 * level (an attribute modifier on the effect, as agriculture/Hexes); every Rimebound that takes starts the hidden Thaw
 * ({@code jugcraft:rime_thaw}) for the binding's time plus {@value #GRACE_TICKS} ticks, and the boundary refuses Rimebound
 * while a creature thaws (a Guard): at most 3 s in every 5, never rooted. Both save with the creature; milk clears both.
 */
public final class Rimebind {
	public static final String STATUS = "jugcraft:rimebound";
	public static final double SLOW = 0.2;
	public static final int GRACE_TICKS = 40;
	public static Holder<MobEffect> RIMEBOUND;
	public static Holder<MobEffect> RIME_THAW;

	private Rimebind() {
	}

	/** A status with no more than its attribute changes (MobEffect's constructor is protected). */
	private static final class Status extends MobEffect {
		Status(MobEffectCategory category, int colour) {
			super(category, colour);
		}
	}

	static void register() {
		RIMEBOUND = Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT, Jugcraft.id("rimebound"),
				new Status(MobEffectCategory.HARMFUL, 0x8FD3F0).addAttributeModifier(Attributes.MOVEMENT_SPEED, Jugcraft.id("rimebound"),
						-SLOW, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
		RIME_THAW = Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT, Jugcraft.id("rime_thaw"),
				new Status(MobEffectCategory.NEUTRAL, 0xCFEFF7));
		// The grace: while a creature thaws, no one's Rimebound takes it (immune, and nothing is spent).
		ConcordanceEffects.guard((context, spec, target) -> binds(spec) && target.hasEffect(RIME_THAW));
	}

	public static boolean binds(EffectSpec spec) {
		return spec.kind() == EffectKind.STATUS && STATUS.equals(spec.status());
	}

	/** A hostile creature at normal tolerance: whose slowing is restraint, Rimekeeping's cold work (02:135; B-P34). */
	public static boolean restrains(LivingEntity target) {
		return target instanceof Enemy && ConcordanceEffects.tolerance(target) == Tolerance.NORMAL;
	}

	/**
	 * After a status took (ConcordanceEffects): a Rimebound starts its Thaw; on a hostile creature at normal tolerance it is
	 * restraint, Rimekeeping's cold work (02:135). Any other status is left alone.
	 */
	public static void bound(@Nullable ServerPlayer person, LivingEntity target, EffectSpec effect) {
		if (!binds(effect)) {
			return;
		}
		MobEffectInstance binding = target.getEffect(RIMEBOUND);
		int ticks = binding == null ? effect.duration() : binding.getDuration();
		target.addEffect(new MobEffectInstance(RIME_THAW, ticks + GRACE_TICKS, 0, false, false, false));
		if (restrains(target)) {
			Rime.practise(person, Rime.KEY_RESTRAINT);
		}
	}
}
