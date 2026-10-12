package io.github.jimbozoomer.jugcraft.raiders;

import io.github.jimbozoomer.jugcraft.chemistry.PetroItems;
import io.github.jimbozoomer.jugcraft.guns.MobGuns;
import io.github.jimbozoomer.jugcraft.town.Townsfolk;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * Raider infantry (batch 57): four kinds, by entity type ({@link Role}).
 * <ul>
 * <li>The <b>grunt</b> charges in with a cleaver (an iron axe).</li>
 * <li>The <b>grenadier</b> lobs small grenades ({@link RaiderBomb}) from up to {@value JugcraftRaiders#GRENADE_MAX_RANGE}
 * blocks, every {@value JugcraftRaiders#GRENADE_COOLDOWN} ticks; closer than {@value JugcraftRaiders#GRENADE_MIN_RANGE} it
 * clubs instead.</li>
 * <li>The <b>officer</b> rallies the raiders near them (Speed and Strength). When an officer falls, the raiders around
 * lose heart (Weakness and Slowness), and a player who struck them down takes their insignia.</li>
 * <li>The <b>gunner</b> (slice 10F of the guns) carries one of the owner's service arms and fires it
 * ({@link MobGuns}): it closes to the gun's reach, fires in bursts and reloads; its bullets pass through raiders.</li>
 * </ul>
 * All of them march on their raid's objective ({@link MarchGoal}) and fight players and the town's folk on the way
 * (and whatever hurts them); town guards and sentry guns fight back (they are hostile mobs). They never drop what they
 * carry, but for a gunner's gun, which a player's kill may win ({@link MobGuns#DROP}), empty.
 */
public class RaiderInfantry extends Monster implements RangedAttackMob, Raider, MobGuns.Gunner {
	/** Which kind of infantry, by entity type. */
	public enum Role {
		GRUNT, GRENADIER, OFFICER, GUNNER
	}

	private final RaidMember member = new RaidMember();
	private int rally;
	/** How long a grunt has gone without getting closer to where it is going (for its siege ladders). */
	private int stuck;
	/** The closest it has come to where it is going since it last got stuck. */
	private double closest = Double.MAX_VALUE;
	/** A gunner's gunfire (set as its goals are, by the constructor of Mob; so no initialiser here). */
	private MobGuns.@Nullable FireGoal gunfire;

	public RaiderInfantry(EntityType<? extends RaiderInfantry> type, Level level) {
		super(type, level);
		xpReward = role() == Role.OFFICER ? 10 : 5;
		ItemStack held = switch (role()) {
			case GRUNT -> new ItemStack(Items.IRON_AXE);
			case GRENADIER -> new ItemStack(PetroItems.GRENADE);
			case OFFICER -> new ItemStack(Items.IRON_SWORD);
			case GUNNER -> MobGuns.arm(level, random);
		};
		setItemSlot(EquipmentSlot.MAINHAND, held);
		// What they carry stays with them (their loot is the loot table's), but for a gunner's gun.
		setDropChance(EquipmentSlot.MAINHAND, role() == Role.GUNNER ? MobGuns.DROP : 0.0F);
	}

	public static AttributeSupplier.Builder attributes(double health, double damage, double armour, double speed) {
		return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, health).add(Attributes.ATTACK_DAMAGE, damage)
				.add(Attributes.ARMOR, armour).add(Attributes.MOVEMENT_SPEED, speed).add(Attributes.FOLLOW_RANGE, 40.0);
	}

	public Role role() {
		EntityType<?> type = getType();
		return type == JugcraftRaiders.GRENADIER ? Role.GRENADIER : type == JugcraftRaiders.OFFICER ? Role.OFFICER
				: type == JugcraftRaiders.GUNNER ? Role.GUNNER : Role.GRUNT;
	}

	/** A gunner's gunfire: its magazine and whether it is reloading (null for the other kinds). */
	public MobGuns.@Nullable FireGoal gunfire() {
		return gunfire;
	}

	/** Raiders' bullets pass through raiders, as their grenades do. */
	@Override
	public boolean spares(LivingEntity other) {
		return other instanceof Raider;
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(1, new FloatGoal(this));
		if (role() == Role.GRENADIER) {
			goalSelector.addGoal(2, new RangedAttackGoal(this, 1.0, JugcraftRaiders.GRENADE_COOLDOWN, JugcraftRaiders.GRENADE_MAX_RANGE));
		} else if (role() == Role.GUNNER) {
			gunfire = new MobGuns.FireGoal(this, 1.0);
			goalSelector.addGoal(2, gunfire);
		} else {
			goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.15, false));
		}
		goalSelector.addGoal(4, new MarchGoal(this, () -> member.objective, 1.0));
		goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.8));
		goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 12.0F));
		targetSelector.addGoal(1, new HurtByTargetGoal(this, RaiderInfantry.class, RaiderWalker.class, RaiderBlimp.class).setAlertOthers());
		targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
		targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Townsfolk.class, true));
	}

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		if (member.checkRaid(level, this)) {
			return;
		}
		if (role() == Role.GRUNT) {
			climbWalls(level);
		}
		if (role() == Role.OFFICER && ++rally >= JugcraftRaiders.RALLY_TICKS) {
			rally = 0;
			for (LivingEntity near : level.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(JugcraftRaiders.RALLY_RADIUS),
					e -> e instanceof Raider && e != this && e.isAlive())) {
				near.addEffect(new MobEffectInstance(MobEffects.SPEED, JugcraftRaiders.RALLY_EFFECT, 0, true, true), this);
				near.addEffect(new MobEffectInstance(MobEffects.STRENGTH, JugcraftRaiders.RALLY_EFFECT, 0, true, true), this);
			}
		}
	}

	/**
	 * Siege ladders: a grunt on its way somewhere (someone it hunts, or its raid's objective) that has got no closer to
	 * it for {@value JugcraftRaiders#LADDER_STUCK} ticks with a wall in front of it, on that side, props a ladder up the
	 * wall, up to {@value JugcraftRaiders#LADDER_MAX} blocks or the top of the wall, and climbs. A blocked path leaves a
	 * mob standing at the wall rather than pushing into it, so this goes by progress rather than by collisions. Only
	 * where mob griefing is on.
	 */
	private void climbWalls(ServerLevel level) {
		BlockPos goal = getTarget() != null ? getTarget().blockPosition() : member.objective;
		if (goal == null || onClimbable()) {
			stuck = 0;
			closest = Double.MAX_VALUE;
			return;
		}
		double dx = goal.getX() + 0.5 - getX();
		double dz = goal.getZ() + 0.5 - getZ();
		double distance = Math.sqrt(dx * dx + dz * dz);
		if (distance < closest - 0.5) {
			closest = distance;
			stuck = 0;
			return;
		}
		if (++stuck < JugcraftRaiders.LADDER_STUCK || !level.getGameRules().get(net.minecraft.world.level.gamerules.GameRules.MOB_GRIEFING)) {
			return;
		}
		stuck = 0;
		closest = Double.MAX_VALUE;
		net.minecraft.core.Direction facing = Math.abs(dx) > Math.abs(dz)
				? (dx > 0 ? net.minecraft.core.Direction.EAST : net.minecraft.core.Direction.WEST)
				: (dz > 0 ? net.minecraft.core.Direction.SOUTH : net.minecraft.core.Direction.NORTH);
		BlockPos at = blockPosition();
		int placed = 0;
		for (int dy = 0; dy < JugcraftRaiders.LADDER_MAX; dy++) {
			BlockPos spot = at.above(dy);
			BlockPos wall = spot.relative(facing);
			if (!level.getBlockState(wall).isFaceSturdy(level, wall, facing.getOpposite()) || !level.getBlockState(spot).isAir()
					&& !SiegeLadderBlock.is(level.getBlockState(spot))) {
				break;
			}
			if (!SiegeLadderBlock.is(level.getBlockState(spot))) {
				level.setBlock(spot, JugcraftRaiders.SIEGE_LADDER.defaultBlockState()
						.setValue(net.minecraft.world.level.block.LadderBlock.FACING, facing.getOpposite()), net.minecraft.world.level.block.Block.UPDATE_ALL);
				placed++;
			}
		}
		if (placed > 0) {
			level.playSound(null, getX(), getY(), getZ(), SoundEvents.LADDER_PLACE, SoundSource.HOSTILE, 1.0F, 0.8F);
		}
	}

	/** A grenadier throws a grenade, or clubs a target too close to throw at. */
	@Override
	public void performRangedAttack(LivingEntity target, float power) {
		if (!(level() instanceof ServerLevel level)) {
			return;
		}
		if (distanceTo(target) < JugcraftRaiders.GRENADE_MIN_RANGE) {
			if (distanceTo(target) < 2.5F) {
				doHurtTarget(level, target);
			}
			return;
		}
		throwAt(level, this, target, false);
		level.playSound(null, getX(), getY(), getZ(), SoundEvents.SNOWBALL_THROW, SoundSource.HOSTILE, 1.0F, 0.6F);
	}

	/** Lobs a grenade (or, {@code heavy}, a bomb) from {@code thrower} so that it lands about where {@code target} stands. */
	static void throwAt(ServerLevel level, LivingEntity thrower, LivingEntity target, boolean heavy) {
		throwAt(level, thrower, target, heavy, null);
	}

	/** As above, launched from {@code from} (a walker's hull gun) rather than the thrower's hand. */
	static void throwAt(ServerLevel level, LivingEntity thrower, LivingEntity target, boolean heavy, net.minecraft.world.phys.@Nullable Vec3 from) {
		RaiderBomb bomb = new RaiderBomb(level, thrower, heavy);
		if (from != null) {
			bomb.setPos(from);
		}
		double dx = target.getX() - thrower.getX();
		double dz = target.getZ() - thrower.getZ();
		double dy = target.getY(0.3) - bomb.getY();
		double flat = Math.sqrt(dx * dx + dz * dz);
		bomb.shoot(dx, dy + flat * 0.2, dz, (float) Math.min(1.4, 0.6 + flat * 0.035), 4.0F);
		level.addFreshEntity(bomb);
	}

	/** An officer's fall breaks the raiders' nerve around them. */
	@Override
	public void die(DamageSource source) {
		super.die(source);
		if (role() == Role.OFFICER && level() instanceof ServerLevel level) {
			double reach = JugcraftRaiders.RALLY_RADIUS * 4.0 / 3.0;
			for (LivingEntity near : level.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(reach),
					e -> e instanceof Raider && e != this && e.isAlive())) {
				near.removeEffect(MobEffects.STRENGTH);
				near.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, JugcraftRaiders.ROUT_TICKS, 1));
				near.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, JugcraftRaiders.ROUT_TICKS, 0));
			}
			level.playSound(null, getX(), getY(), getZ(), SoundEvents.PILLAGER_CELEBRATE, SoundSource.HOSTILE, 1.0F, 0.6F);
		}
		if (level() instanceof ServerLevel level) {
			RaiderRaids.fallen(level, this);
		}
	}

	@Override
	public @Nullable UUID raid() {
		return member.raid;
	}

	@Override
	public @Nullable BlockPos objective() {
		return member.objective;
	}

	@Override
	public void joinRaid(UUID raid, BlockPos objective) {
		member.join(raid, objective);
	}

	@Override
	public boolean removeWhenFarAway(double distance) {
		return !member.persistent() && super.removeWhenFarAway(distance);
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return SoundEvents.PILLAGER_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.PILLAGER_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.PILLAGER_DEATH;
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		member.save(output);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		member.load(input);
	}
}
