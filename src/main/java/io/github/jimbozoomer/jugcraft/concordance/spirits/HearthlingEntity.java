package io.github.jimbozoomer.jugcraft.concordance.spirits;

import com.geckolib.animation.RawAnimation;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.jimbozoomer.jugcraft.concordance.ConcordanceEffects;
import io.github.jimbozoomer.jugcraft.concordance.ConcordanceProgress;
import io.github.jimbozoomer.jugcraft.concordance.effect.Cause;
import io.github.jimbozoomer.jugcraft.concordance.effect.EffectKind;
import io.github.jimbozoomer.jugcraft.concordance.effect.EffectSpec;
import io.github.jimbozoomer.jugcraft.concordance.effect.Intent;
import io.github.jimbozoomer.jugcraft.concordance.effect.Ledger;
import io.github.jimbozoomer.jugcraft.concordance.effect.Stacking;
import io.github.jimbozoomer.jugcraft.concordance.rules.Evidence;
import io.github.jimbozoomer.jugcraft.concordance.worker.Bond;
import io.github.jimbozoomer.jugcraft.concordance.worker.Status;
import io.github.jimbozoomer.jugcraft.concordance.worker.WorkerDefinition;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * The Hearthling (roadmap step 17), a familiar: bound to one person by a {@link Bond}, it keeps close, and when they are
 * badly hurt it mends them through the shared effect boundary, as often as its bond allows. It never crosses into
 * another dimension or teleports into an unloaded place: when its person is offline it waits where it is ("owner
 * offline"), and when they are in another dimension it waits ("other dimension") until they come back or recall it.
 */
public class HearthlingEntity extends WorkerEntity<HearthlingEntity> {
	public static final String DEFINITION = "jugcraft:hearthling";
	/** Its person must be at or under this share of their health to be mended. */
	public static final float HURT = 0.5F;
	/** How far it may lag before it hops to its person (in the same dimension, beside them). */
	public static final double LEASH = 24.0;
	/** Game ticks between two visits that grow the bond. */
	public static final long VISIT_TICKS = 200L;
	private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.hearthling.idle");
	private static final RawAnimation FOLLOWING = RawAnimation.begin().thenLoop("animation.hearthling.following");
	private static final RawAnimation SUPPORTING = RawAnimation.begin().thenLoop("animation.hearthling.supporting");
	private static final RawAnimation WAITING = RawAnimation.begin().thenLoop("animation.hearthling.waiting");
	public static final Codec<Bond> BOND_CODEC = RecordCodecBuilder.create(i -> i.group(
			Codec.INT.fieldOf("strength").forGetter(Bond::strength), Codec.LONG.fieldOf("window").forGetter(Bond::window),
			Codec.INT.fieldOf("gained_today").forGetter(Bond::gainedToday), Codec.LONG.fieldOf("last_support").forGetter(Bond::lastSupport),
			Codec.LONG.fieldOf("last_seen").forGetter(Bond::lastSeen)).apply(i, Bond::new));

	private Bond bond = Bond.NEW;

	public HearthlingEntity(EntityType<? extends PathfinderMob> type, Level level) {
		super(type, level);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 10.0).add(Attributes.MOVEMENT_SPEED, 0.3).add(Attributes.FOLLOW_RANGE, 32.0);
	}

	@Override
	public String kind() {
		return "familiar";
	}

	public Bond bond() {
		return bond;
	}

	public void setBond(Bond bond) {
		this.bond = bond;
	}

	private WorkerDefinition.@Nullable Familiar terms() {
		return Workers.catalog().get(DEFINITION, WorkerDefinition.Familiar.class);
	}

	@Override
	public Status think(ServerLevel level, long time, long gameTime) {
		Status next = decide(level, gameTime);
		setStatus(next);
		return next;
	}

	private Status decide(ServerLevel level, long gameTime) {
		WorkerDefinition.Familiar terms = terms();
		if (!Workers.enabled() || terms == null) {
			stop();
			return Status.DISABLED;
		}
		if (owner == null || !getUUID().equals(WorkerRoster.of(level.getServer()).familiar(owner))) {
			// Released (or replaced) while it was away: a familiar its person no longer keeps leaves.
			discard();
			return Status.FINISHED;
		}
		ServerPlayer person = level.getServer().getPlayerList().getPlayer(owner);
		if (person == null) {
			stop();
			return Status.OWNER_OFFLINE;
		}
		if (person.level() != level) {
			stop();
			return Status.OTHER_DIMENSION;
		}
		double distance = distanceTo(person);
		if (distance <= terms.follow() && gameTime - bond.lastSeen() >= VISIT_TICKS) {
			bond = bond.together(gameTime);
		} else if (distance > terms.follow()) {
			bond = bond.neglected(gameTime);
		}
		if (person.getHealth() <= person.getMaxHealth() * HURT && bond.mayMend(gameTime) && distance <= terms.follow()) {
			if (mend(level, person, terms)) {
				bond = bond.mended(gameTime);
				ConcordanceProgress.record(person, new Evidence.Practiced(Workers.ACTIVITY, "familiar"));
				return Status.SUPPORTING;
			}
		}
		if (distance > LEASH) {
			BlockPos beside = person.blockPosition().relative(person.getDirection().getOpposite());
			if (level.isLoaded(beside) && level.getBlockState(beside).isAir()) {
				teleportTo(beside.getX() + 0.5, beside.getY(), beside.getZ() + 0.5);
				arrived();
				return Status.FOLLOWING;
			}
		}
		if (distance > terms.follow()) {
			return walkTo(person.position(), 1.2F, 2, gameTime) ? Status.FOLLOWING : Status.CANNOT_NAVIGATE;
		}
		arrived();
		return Status.IDLE;
	}

	/** Mends its person through the shared effect boundary; returns whether the effect took. */
	boolean mend(ServerLevel level, ServerPlayer person, WorkerDefinition.Familiar terms) {
		Cause cause = Cause.of(owner, Cause.Origin.CREATURE, Workers.FAMILIAR_SOURCE, ConcordanceEffects.nextSerial());
		Ledger ledger = new Ledger(new Ledger.Limits(1, EffectKind.STATUS.work, 0));
		EffectSpec spec = new EffectSpec(EffectKind.STATUS, Intent.HELPFUL, terms.amplifier(), terms.duration(), terms.effect(), Stacking.STRONGEST, null);
		return ConcordanceEffects.apply(new ConcordanceEffects.Context(level, cause, this, ledger, "familiar", position()), spec, person).applied();
	}

	/** Killed: struck off its person's roster, so they may bond another. */
	@Override
	public void remove(RemovalReason reason) {
		if (reason.shouldDestroy() && level() instanceof ServerLevel server && owner != null
				&& getUUID().equals(WorkerRoster.of(server.getServer()).familiar(owner))) {
			WorkerRoster.of(server.getServer()).remove(owner, getUUID());
		}
		super.remove(reason);
	}

	@Override
	public Component describe() {
		return Component.translatable("message.jugcraft.concordance.workers.familiar", super.describe(), bond.strength(), Bond.MAX);
	}

	@Override
	protected RawAnimation animation(Status status) {
		return switch (status) {
			case FOLLOWING -> FOLLOWING;
			case SUPPORTING -> SUPPORTING;
			case OWNER_OFFLINE, OTHER_DIMENSION, DISABLED, CANNOT_NAVIGATE -> WAITING;
			default -> IDLE;
		};
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.store("bond", BOND_CODEC, bond);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		bond = input.read("bond", BOND_CODEC).orElse(Bond.NEW);
	}
}
