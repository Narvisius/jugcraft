package io.github.jimbozoomer.jugcraft.concordance;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.effect.Cause;
import io.github.jimbozoomer.jugcraft.concordance.effect.EffectKind;
import io.github.jimbozoomer.jugcraft.concordance.effect.EffectSpec;
import io.github.jimbozoomer.jugcraft.concordance.effect.Intent;
import io.github.jimbozoomer.jugcraft.concordance.effect.Ledger;
import io.github.jimbozoomer.jugcraft.concordance.effect.Stacking;
import io.github.jimbozoomer.jugcraft.concordance.effect.Tolerance;
import io.github.jimbozoomer.jugcraft.concordance.ember.Ember;
import io.github.jimbozoomer.jugcraft.concordance.sign.Sign;
import io.github.jimbozoomer.jugcraft.concordance.sign.Signs;
import io.github.jimbozoomer.jugcraft.party.JugcraftParties;
import io.github.jimbozoomer.jugcraft.town.TownProtection;
import io.github.jimbozoomer.jugcraft.weapons.ArmItem;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.spell_power.api.SpellDamageSource;
import net.spell_power.api.SpellSchool;
import net.spell_power.api.SpellSchools;
import org.jspecify.annotations.Nullable;

/**
 * The shared effect boundary: the one place a Concordance effect changes the world, whatever delivered it (a composed
 * spell, an invocation, an item, and later rituals, alchemy, shrines and creatures). Every application here, in order:
 * <ol>
 * <li>checks the target suits the operation (a creature or a block);</li>
 * <li>for a harmful effect, applies the friendly-fire rules ({@link #mayHarm}): never the effect's own actor; another
 * player only where the actor may hurt them (the server's PvP setting) and they share no party; players in creative or
 * spectator mode are immune;</li>
 * <li>applies the creature's tolerance ({@link Tolerance}: {@code #jugcraft:concordance/immune} and
 * {@code /resistant}) or, for a block, the place's protection (spawn protection, protected towns, build
 * permission);</li>
 * <li>admits the application to the event's {@link Ledger} (one-time accounting, target and work limits);</li>
 * <li>applies it, attributing it to its {@link Cause}: damage is dealt as the actor (kill credit), statuses name the
 * actor as their source, drops fall as the actor would make them;</li>
 * <li>shows it: particles and sound sent to clients. Presentation never applies anything, so a client can never repeat
 * an effect.</li>
 * </ol>
 * Lasting effects (status, protection, detection) stack by the effect's {@link Stacking} rule and expire by their own
 * time, as vanilla status effects. Damage goes through the damage type of the effect's Spell Power school (generic
 * magic if it names none), so Spell Power's rules apply as for any spell: it bypasses armour and shields, and Spell
 * Power's magic resistance reduces it. This boundary does not scale or reduce damage itself: the magnitudes it receives
 * are final (Spell Power scaling, where an operation declares it, is applied to the plan before it runs:
 * {@code Plan.scaled}).
 */
public final class ConcordanceEffects {
	public static final TagKey<EntityType<?>> IMMUNE = TagKey.create(Registries.ENTITY_TYPE, Jugcraft.id("concordance/immune"));
	public static final TagKey<EntityType<?>> RESISTANT = TagKey.create(Registries.ENTITY_TYPE, Jugcraft.id("concordance/resistant"));
	public static final TagKey<Block> INTERACTABLE = TagKey.create(Registries.BLOCK, Jugcraft.id("concordance/interactable"));
	public static final TagKey<Block> HARVESTABLE = TagKey.create(Registries.BLOCK, Jugcraft.id("concordance/harvestable"));
	private static final AtomicLong SERIALS = new AtomicLong();
	private static final List<Guard> GUARDS = new CopyOnWriteArrayList<>();

	/**
	 * A protection a feature adds to the boundary (roadmap step 22's moving ward): asked of every creature operation after
	 * the multiplayer rules, it may refuse it, and the target is then immune to it.
	 */
	@FunctionalInterface
	public interface Guard {
		boolean refuses(Context context, EffectSpec spec, LivingEntity target);
	}

	/** Adds a guard (at registration). */
	public static void guard(Guard guard) {
		GUARDS.add(guard);
	}

	private ConcordanceEffects() {
	}

	/** What happened to one application. Only {@link #APPLIED} changed anything. */
	public enum Result {
		APPLIED,
		/** A lasting effect met a stronger or longer one ({@link Stacking}) and changed nothing. */
		KEPT,
		/** Nothing there for it to act on (no open air, nothing ripe, already at full health). */
		NOTHING,
		/** The target is immune to it ({@link Tolerance}, creative or spectator mode, vanilla effect immunity). */
		IMMUNE,
		/** Friendly fire: the actor may not harm this target. */
		FRIENDLY,
		/** The block is protected from the actor (spawn protection, a town, build permission). */
		NOT_ALLOWED,
		/** The event's ledger refused it: a repeat, or over its target or work limit. */
		LIMIT,
		/** The operation does not act on this kind of target. */
		WRONG_TARGET;

		public boolean applied() {
			return this == APPLIED;
		}
	}

	/**
	 * Who an application belongs to: the level, its cause and the actor entity if it is present, the event's ledger,
	 * the application's step key in that ledger (unique within the event) and the point the effect came from (pushes
	 * go away from it).
	 */
	public record Context(ServerLevel level, Cause cause, @Nullable Entity actor, Ledger ledger, String step, Vec3 origin) {
	}

	/** A new event serial: one per cast, use or run, unique while the server runs. */
	public static long nextSerial() {
		return SERIALS.incrementAndGet();
	}

	/** Applies a creature operation to {@code target}. */
	public static Result apply(Context context, EffectSpec spec, LivingEntity target) {
		if (spec.kind().on != EffectKind.On.CREATURE) {
			return Result.WRONG_TARGET;
		}
		if (!target.isAlive() || target.isRemoved()) {
			return Result.NOTHING;
		}
		EffectSpec effect = spec;
		// The multiplayer rules judge the person behind it (roadmap step 28): its actor, or whoever its cause names.
		ServerPlayer person = person(context);
		Entity harmer = context.actor() instanceof Player || person == null ? context.actor() : person;
		boolean unanswered = target instanceof Player && absent(context);
		// A push moves another player whatever it is meant for, so it faces the rule a harmful effect would (step 28).
		if (spec.intent() != Intent.HARMFUL && spec.kind() == EffectKind.MOVEMENT && target instanceof Player && harmer != target
				&& (unanswered || !mayHarm(harmer, target))) {
			return Result.FRIENDLY;
		}
		if (spec.intent() == Intent.HARMFUL) {
			// Creative mode's own protection (its invulnerable ability, which game mode changes keep), or a spectator.
			if (target instanceof Player player && (player.getAbilities().invulnerable || player.isSpectator())) {
				return Result.IMMUNE;
			}
			// A player is harmed only by someone here to answer to the PvP rules and parties.
			if (unanswered || !mayHarm(harmer, target)) {
				return Result.FRIENDLY;
			}
			// Roadmap step 28: only what that person could strike by hand (claims and protected creatures refuse it).
			if (!Authority.mayStrike(context.level(), context.actor(), behind(context), target)) {
				return Result.NOT_ALLOWED;
			}
			EffectSpec.Adjusted adjusted = tolerance(target).adjust(spec);
			if (adjusted == null) {
				return Result.IMMUNE;
			}
			effect = adjusted.spec();
		}
		for (Guard guard : GUARDS) {
			if (guard.refuses(context, effect, target)) {
				return Result.IMMUNE;
			}
		}
		if (!context.ledger().admit(context.step(), target.getStringUUID(), spec.kind().work).admitted()) {
			return Result.LIMIT;
		}
		ServerLevel level = context.level();
		Result result = switch (effect.kind()) {
			case DAMAGE -> target.hurtServer(level, damageSource(level, effect, context.actor()), effect.magnitude()) ? Result.APPLIED
					: Result.NOTHING;
			case RESTORATION -> {
				float before = target.getHealth();
				target.heal(effect.magnitude());
				yield target.getHealth() > before ? Result.APPLIED : Result.NOTHING;
			}
			case MOVEMENT -> push(target, context.origin(), effect);
			case STATUS -> {
				Holder<MobEffect> status = status(effect.status());
				yield status == null ? Result.NOTHING : lasting(target, status, effect.magnitude(), effect, context.actor());
			}
			case PROTECTION -> lasting(target, MobEffects.ABSORPTION, effect.protectionAmplifier(), effect, context.actor());
			case DETECTION -> lasting(target, MobEffects.GLOWING, 0, effect, context.actor());
			default -> Result.WRONG_TARGET;
		};
		if (result.applied()) {
			show(level, effect.kind(), target.getX(), target.getY() + target.getBbHeight() * 0.6, target.getZ());
		}
		return result;
	}

	/** Applies a block operation at {@code pos}. */
	public static Result apply(Context context, EffectSpec spec, BlockPos pos) {
		if (spec.kind().on != EffectKind.On.BLOCK) {
			return Result.WRONG_TARGET;
		}
		ServerLevel level = context.level();
		// A block changes only as the person behind the effect could change it by hand, asked of whoever answers for them
		// (Authority, roadmap step 28); a block is used only by a player's own effect.
		ServerPlayer person = person(context);
		ServerPlayer answering = person != null ? person : Authority.answering(level, behind(context));
		BlockState state = level.getBlockState(pos);
		boolean allowed = spec.kind() == EffectKind.INTERACTION ? context.actor() instanceof Player actor && mayUse(level, actor, pos, state)
				: Authority.mayChange(level, answering, pos);
		if (!accepts(level, spec, pos)) {
			return Result.NOTHING;
		}
		if (!allowed) {
			return Result.NOT_ALLOWED;
		}
		if (!context.ledger().admit(context.step(), "block:" + pos.asLong(), spec.kind().work).admitted()) {
			return Result.LIMIT;
		}
		Result result = switch (spec.kind()) {
			case ILLUMINATION -> {
				int steps = Math.clamp((spec.duration() + LumenMoteBlock.STEP_TICKS - 1) / LumenMoteBlock.STEP_TICKS, 1, LumenMoteBlock.MAX_STEPS);
				yield Illumination.kindle(level, answering, pos, steps).lit() ? Result.APPLIED : Result.NOTHING;
			}
			case INTERACTION -> state.useWithoutItem(level, (Player) context.actor(), new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false))
					.consumesAction() ? Result.APPLIED : Result.NOTHING;
			case HARVESTING -> {
				if (state.getBlock() instanceof CropBlock) {
					ArmItem.reap(level, pos, context.actor(), ItemStack.EMPTY);
				} else {
					level.destroyBlock(pos, true, context.actor());
				}
				yield Result.APPLIED;
			}
			case ALTERATION -> {
				if (Ember.kindles(spec)) {
					yield Ember.kindle(level, person, pos) ? Result.APPLIED : Result.NOTHING;
				}
				level.removeBlock(pos, false);
				level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.5F, 1.4F);
				yield Result.APPLIED;
			}
			default -> Result.WRONG_TARGET;
		};
		if (result.applied()) {
			show(level, spec.kind(), pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
		}
		return result;
	}

	/**
	 * Whether a block operation has something to act on at {@code pos}: open air for light, a listed block to use, a
	 * ripe crop or listed plant to gather, fire to put out (or, for Ember's alteration, an unlit hearth to kindle).
	 * Selections use this to choose blocks.
	 */
	public static boolean accepts(ServerLevel level, EffectSpec spec, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		return switch (spec.kind()) {
			case ILLUMINATION -> Illumination.open(state);
			case INTERACTION -> state.is(INTERACTABLE);
			case HARVESTING -> state.getBlock() instanceof CropBlock crop ? crop.isMaxAge(state) : state.is(HARVESTABLE);
			case ALTERATION -> Ember.kindles(spec) ? Ember.kindleable(state) : state.is(BlockTags.FIRE);
			default -> false;
		};
	}

	/**
	 * The player an effect acts for (roadmap step 28): its actor when that is a player, otherwise the player its cause
	 * names (a familiar's owner, a ritual's leader, a device's keeper) if they are here; null when nobody is behind it
	 * or they are away.
	 */
	public static @Nullable ServerPlayer person(Context context) {
		return context.actor() instanceof ServerPlayer player ? player : Authority.present(context.level(), behind(context));
	}

	/**
	 * Who the cause names as behind an effect, when that is someone other than the creature acting (a familiar's owner,
	 * a ritual's leader, a device's keeper): a creature acting for itself, or one still in the level, is nobody's.
	 */
	static @Nullable UUID behind(Context context) {
		UUID named = context.cause().actor();
		if (named == null || context.actor() != null && context.actor().getUUID().equals(named)) {
			return null;
		}
		Entity there = context.level().getEntity(named);
		return there == null || there instanceof Player ? named : null;
	}

	/** Whether the player behind an effect is away (offline or elsewhere): nobody is here to answer for it. */
	static boolean absent(Context context) {
		return !(context.actor() instanceof Player) && behind(context) != null && person(context) == null;
	}

	/**
	 * The friendly-fire rule for harmful effects: never the actor itself; another player only where the actor (a
	 * player) may hurt them and they are not in the actor's party. Creatures and sourceless effects may harm anyone
	 * else.
	 */
	public static boolean mayHarm(@Nullable Entity actor, LivingEntity target) {
		if (actor != null && actor == target) {
			return false;
		}
		if (target instanceof Player victim && actor instanceof Player attacker) {
			return attacker.canHarmPlayer(victim) && !JugcraftParties.sameParty(attacker.getUUID(), victim.getUUID());
		}
		return true;
	}

	public static Tolerance tolerance(LivingEntity target) {
		if (target.is(IMMUNE)) {
			return Tolerance.IMMUNE;
		}
		return target.is(RESISTANT) ? Tolerance.RESISTANT : Tolerance.NORMAL;
	}

	/** Whether the actor may use the block at {@code pos} as an interaction: towns allow their usable blocks. */
	private static boolean mayUse(ServerLevel level, Player player, BlockPos pos, BlockState state) {
		return level.isInWorldBounds(pos) && level.mayInteract(player, pos)
				&& (!TownProtection.denies(player, level, pos) || state.is(TownProtection.USABLE));
	}

	/**
	 * The damage source for a damage effect: the effect's Spell Power school (generic if it names none or an unknown
	 * one), dealt as the actor when there is a living one, so the actor gets the kill; plain magic otherwise.
	 */
	static DamageSource damageSource(ServerLevel level, EffectSpec effect, @Nullable Entity actor) {
		SpellSchool school = effect.school() == null ? null : SpellSchools.getSchool(effect.school());
		if (actor instanceof LivingEntity living) {
			return SpellDamageSource.create(school == null ? SpellSchools.GENERIC : school, living);
		}
		return level.damageSources().magic();
	}

	private static @Nullable Holder<MobEffect> status(@Nullable String id) {
		Identifier parsed = id == null ? null : Identifier.tryParse(id);
		if (parsed == null) {
			return null;
		}
		return BuiltInRegistries.MOB_EFFECT.get(ResourceKey.create(Registries.MOB_EFFECT, parsed)).<Holder<MobEffect>>map(holder -> holder)
				.orElse(null);
	}

	/** A lasting effect, combined with what the target already has by the effect's stacking rule. */
	private static Result lasting(LivingEntity target, Holder<MobEffect> effect, int amplifier, EffectSpec spec, @Nullable Entity actor) {
		MobEffectInstance existing = target.getEffect(effect);
		int hasTicks = existing == null ? 0 : existing.isInfiniteDuration() ? Integer.MAX_VALUE : existing.getDuration();
		Stacking.Result combined = spec.stacking().combine(existing == null ? 0 : existing.getAmplifier(), hasTicks, amplifier, spec.duration());
		if (!combined.changed()) {
			return Result.KEPT;
		}
		MobEffectInstance instance = new MobEffectInstance(effect, combined.ticks(), combined.amplifier());
		if (!target.canBeAffected(instance)) {
			return Result.IMMUNE;
		}
		return target.addEffect(instance, actor) ? Result.APPLIED : Result.KEPT;
	}

	/**
	 * A push away from {@code origin}, level, slowed by the target's knockback resistance. From where the target itself
	 * stands (a spell cast on oneself, such as Flashstep) there is no "away": it goes the way the target faces, level,
	 * or straight up if it looks straight up or down.
	 */
	private static Result push(LivingEntity target, Vec3 origin, EffectSpec effect) {
		Vec3 away = target.position().subtract(origin);
		Vec3 flat = new Vec3(away.x, 0.0, away.z);
		if (flat.lengthSqr() < 1.0E-4) {
			Vec3 look = target.getLookAngle();
			flat = new Vec3(look.x, 0.0, look.z);
		}
		Vec3 direction = flat.lengthSqr() < 1.0E-4 ? new Vec3(0.0, 1.0, 0.0) : flat.normalize();
		double speed = Math.min(effect.magnitude(), EffectSpec.MAX_PUSH) / 10.0
				* Math.max(0.0, 1.0 - target.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE));
		if (speed <= 0.0) {
			return Result.NOTHING;
		}
		target.push(direction.x * speed, 0.2 + direction.y * speed * 0.5, direction.z * speed);
		if (target instanceof ServerPlayer player) {
			player.connection.send(new ClientboundSetEntityMotionPacket(player));
		}
		return Result.APPLIED;
	}

	/**
	 * Presentation only: the effect's mark, a {@link Sign} each client nearby draws as its settings allow (roadmap step
	 * 27), shown only for an effect that was applied.
	 */
	private static void show(ServerLevel level, EffectKind kind, double x, double y, double z) {
		Signs.show(level, new Vec3(x, y, z), Sign.forEffect(kind), null);
	}

	/** The world's view of an actor: the entity behind a cause, if it is in this level. */
	public static @Nullable Entity actor(ServerLevel level, Cause cause) {
		return cause.actor() == null ? null : level.getEntity(cause.actor());
	}
}
