package io.github.jimbozoomer.jugcraft.weapons;

import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlocksAttacks;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * A thrown arm in flight (Arms VIII, batch 59, docs/features/arms-viii.md): it carries the arm itself
 * ({@link ThrownArmItem}), flies as its {@link JugcraftArms.Thrown} says, and strikes the first foe in its way for its
 * damage, more for the arm's damage enchantments (as Impaling adds to a trident's throw), through the trident's damage
 * (the death message says the victim was impaled). Then it comes down where it struck, as the arm again, to be picked up,
 * and never despawns while it waits; one thrown from creative only vanishes. By kind:
 *
 * <ul>
 * <li>javelin: flies far and straight;</li>
 * <li>francisca: a foe blocking it with a shield or a parrying arm stops blocking for
 * {@link JugcraftArms#FRANCISCA_DISABLE} seconds, as an axe's blow does;</li>
 * <li>chakram: flies flat, through every foe in its way (up to {@link JugcraftArms#CHAKRAM_TARGETS}); at
 * {@link JugcraftArms#CHAKRAM_RANGE} blocks, or where it meets a block, it turns back to its thrower, through anything,
 * striking each foe again on the way, and is caught (into the inventory, or at the thrower's feet if it is full);</li>
 * <li>harpoon: keeps its speed underwater; the foe it strikes is hauled towards its thrower and dragged from the
 * saddle.</li>
 * </ul>
 *
 * <p>A player's throw strikes only what that player may strike (allies, mounts, pets and protected foes are spared, as
 * by the arms' two-handed blows). Everything is worked on the server; the client draws it ({@code ThrownArmRenderer}).
 */
public class ThrownArm extends ThrowableItemProjectile {
	/** The chakram is flying back (synced, so the client moves it as the server does). */
	private static final EntityDataAccessor<Boolean> RETURNING = SynchedEntityData.defineId(ThrownArm.class, EntityDataSerializers.BOOLEAN);
	/** A thrown thing keeps this share of its speed a tick in water (ThrowableProjectile's), which the harpoon undoes. */
	private static final double WATER_INERTIA = 0.8;

	/** Ticks in flight. */
	private int flight;
	/** Thrown from creative: it vanishes where it lands instead of leaving the arm. */
	private boolean creative;
	/** Where it was thrown from: the chakram turns back CHAKRAM_RANGE blocks off. */
	private Vec3 from = Vec3.ZERO;
	/** Its speed when thrown: the harpoon keeps it underwater. */
	private double launch;
	/** The foes it has struck on the way out, and on the way back (each is struck once each way). */
	private final Set<Integer> struckOut = new HashSet<>();
	private final Set<Integer> struckBack = new HashSet<>();

	public ThrownArm(EntityType<? extends ThrownArm> type, Level level) {
		super(type, level);
	}

	public ThrownArm(Level level, LivingEntity owner, ItemStack stack) {
		super(JugcraftArms.THROWN_ARM, owner, level, stack);
		from = owner.getEyePosition();
	}

	/** Thrown from creative: it leaves nothing behind. */
	void fromCreative() {
		creative = true;
	}

	@Override
	protected Item getDefaultItem() {
		return JugcraftArms.ITEMS.get("bronze_javelin");
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(RETURNING, false);
	}

	/** The arm's kind (javelin, francisca, chakram or harpoon). */
	public String kind() {
		return getItem().getItem() instanceof ArmItem arm ? arm.kind() : "javelin";
	}

	/** How it flies: its arm's thrown numbers (a javelin's if it carries none). */
	public JugcraftArms.Thrown thrown() {
		if (getItem().getItem() instanceof ThrownArmItem arm) {
			return arm.thrown();
		}
		return JugcraftArms.THROWN.getFirst();
	}

	/** Whether it is a chakram flying back to its thrower. */
	public boolean isReturning() {
		return entityData.get(RETURNING);
	}

	@Override
	protected double getDefaultGravity() {
		return thrown().gravity();
	}

	@Override
	public void tick() {
		if (isReturning()) {
			returnTick();
			return;
		}
		if (flight == 0) {
			launch = getDeltaMovement().length();
		}
		super.tick();
		if (isRemoved()) {
			return;
		}
		flight++;
		if (kind().equals("harpoon") && isInWater()) {
			// Undo the water's drag: as in air, so it flies true underwater (never faster than it was thrown).
			Vec3 motion = getDeltaMovement();
			double speed = motion.length();
			if (speed > 1.0E-4 && speed < launch) {
				setDeltaMovement(motion.scale(Math.min(launch / speed, JugcraftArms.HARPOON_WATER / WATER_INERTIA)));
			}
		}
		if (level() instanceof ServerLevel level && kind().equals("chakram")
				&& (position().distanceTo(from) >= JugcraftArms.CHAKRAM_RANGE || flight > JugcraftArms.CHAKRAM_MAX_TICKS)) {
			turnBack(level);
		}
	}

	/** The chakram turns back to its thrower (or falls where it is, if its thrower is gone). */
	private void turnBack(ServerLevel level) {
		if (!(getOwner() instanceof LivingEntity owner) || !owner.isAlive() || owner.level() != level) {
			land(level, position());
			return;
		}
		entityData.set(RETURNING, true);
		level.playSound(null, getX(), getY(), getZ(), SoundEvents.TRIDENT_RETURN, SoundSource.PLAYERS, 0.8F, 1.4F);
	}

	/** The chakram's way back: straight to its thrower's chest, through anything, cutting the foes it passes. */
	private void returnTick() {
		baseTick();
		flight++;
		Entity owner = getOwner();
		if (owner == null || !owner.isAlive() || owner.level() != level()) {
			if (level() instanceof ServerLevel level) {
				land(level, position());
			}
			return;
		}
		Vec3 target = owner.position().add(0.0, owner.getBbHeight() * 0.6, 0.0);
		Vec3 toward = target.subtract(position());
		double distance = toward.length();
		Vec3 step = distance > 1.0E-4 ? toward.scale(Math.min(distance, JugcraftArms.CHAKRAM_RETURN) / distance) : Vec3.ZERO;
		if (level() instanceof ServerLevel level) {
			AABB swept = getBoundingBox().expandTowards(step).inflate(0.3);
			for (Entity foe : level.getEntities(this, swept, this::canHitEntity)) {
				if (struckBack.size() < JugcraftArms.CHAKRAM_TARGETS && struckBack.add(foe.getId())) {
					// Its cut on the way out may be moments ago: let that go, as the arts' repeated hits do.
					if (foe instanceof LivingEntity living) {
						living.damageCooldownTime = 0;
					}
					strike(level, foe);
				}
			}
			if (distance <= JugcraftArms.CHAKRAM_CATCH || flight > JugcraftArms.CHAKRAM_MAX_TICKS * 2) {
				caught(level, owner);
				return;
			}
		}
		setDeltaMovement(step);
		setPos(position().add(step));
		updateRotation();
	}

	/** The chakram is back: into its thrower's inventory, or at their feet if it is full. */
	private void caught(ServerLevel level, Entity owner) {
		if (!creative) {
			ItemStack arm = getItem().copy();
			if (!(owner instanceof Player player) || !player.getInventory().add(arm)) {
				land(level, owner.position());
				return;
			}
		}
		level.playSound(null, owner.getX(), owner.getY(), owner.getZ(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.4F, 1.6F);
		discard();
	}

	@Override
	protected boolean canHitEntity(Entity entity) {
		if (!super.canHitEntity(entity) || entity == getOwner() || struckOut.contains(entity.getId()) && !isReturning()
				|| struckBack.contains(entity.getId()) && isReturning()) {
			return false;
		}
		if (getOwner() instanceof ServerPlayer player && level() instanceof ServerLevel level) {
			return entity instanceof LivingEntity foe ? TwoHanded.target(player, foe) && TwoHanded.allowed(player, level, foe)
					: entity.isAttackable();
		}
		return true;
	}

	@Override
	protected void onHitEntity(EntityHitResult hit) {
		super.onHitEntity(hit);
		if (!(level() instanceof ServerLevel level)) {
			return;
		}
		Entity target = hit.getEntity();
		if (kind().equals("chakram")) {
			// Through every foe in its way, each once, up to CHAKRAM_TARGETS.
			if (struckOut.size() < JugcraftArms.CHAKRAM_TARGETS && struckOut.add(target.getId())) {
				strike(level, target);
			}
			if (struckOut.size() >= JugcraftArms.CHAKRAM_TARGETS) {
				turnBack(level);
			}
			return;
		}
		strike(level, target);
		land(level, position());
	}

	@Override
	protected void onHitBlock(BlockHitResult hit) {
		super.onHitBlock(hit);
		if (!(level() instanceof ServerLevel level) || isRemoved()) {
			return;
		}
		level.playSound(null, getX(), getY(), getZ(), SoundEvents.TRIDENT_HIT_GROUND, SoundSource.PLAYERS, 0.8F, 1.0F);
		if (kind().equals("chakram")) {
			turnBack(level);
			return;
		}
		// Just off the face it struck, so it does not land inside the block.
		Direction face = hit.getDirection();
		land(level, hit.getLocation().add(face.getStepX() * 0.25, face.getStepY() * 0.25, face.getStepZ() * 0.25));
	}

	/** Strikes a foe for its thrown damage (and its enchantments), then its kind's own effect. */
	private void strike(ServerLevel level, Entity target) {
		Entity owner = getOwner();
		ItemStack arm = getItem();
		DamageSource source = damageSources().trident(this, owner == null ? this : owner);
		float damage = EnchantmentHelper.modifyDamage(level, arm, target, source, thrown().damage());
		LivingEntity blocker = target instanceof LivingEntity living && living.isBlocking() ? living : null;
		boolean hurt = target.hurtServer(level, source, damage);
		if (hurt) {
			EnchantmentHelper.doPostAttackEffectsWithItemSource(level, target, source, arm);
		}
		level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.TRIDENT_HIT, SoundSource.PLAYERS, 1.0F,
				kind().equals("chakram") ? 1.5F : 1.0F);
		if (!(target instanceof LivingEntity foe)) {
			return;
		}
		switch (kind()) {
			case "francisca" -> {
				if (blocker != null) {
					disableBlocking(level, blocker);
				}
			}
			case "harpoon" -> {
				if (hurt && owner != null) {
					haul(foe, owner);
				}
			}
			default -> {
			}
		}
	}

	/** The francisca knocks a raised shield (or parrying arm) down for FRANCISCA_DISABLE seconds, as an axe does. */
	public static void disableBlocking(ServerLevel level, LivingEntity blocker) {
		ItemStack using = blocker.getUseItem();
		BlocksAttacks blocks = using.get(DataComponents.BLOCKS_ATTACKS);
		if (blocks != null) {
			blocks.disable(level, blocker, JugcraftArms.FRANCISCA_DISABLE, using);
		}
	}

	/** The harpoon hauls its foe towards the thrower, harder the further off it is, and drags it from the saddle. */
	private static void haul(LivingEntity foe, Entity owner) {
		Vec3 back = new Vec3(owner.getX() - foe.getX(), 0.0, owner.getZ() - foe.getZ());
		double distance = back.length();
		double resist = Math.max(0.0, 1.0 - foe.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE));
		double speed = Math.min(JugcraftArms.HARPOON_PULL_MAX, JugcraftArms.HARPOON_PULL * Math.max(0.0, distance - 1.5)) * resist;
		Vec3 pull = distance > 1.0E-4 ? back.scale(speed / distance) : Vec3.ZERO;
		foe.setDeltaMovement(pull.x, 0.3 * resist, pull.z);
		if (foe instanceof ServerPlayer player) {
			player.connection.send(new ClientboundSetEntityMotionPacket(player));
		}
		if (foe.isPassenger() && !foe.is(EntityTypeTags.CANNOT_BE_DISMOUNTED_BY_ITEM_USAGE)) {
			foe.stopRiding();
		}
	}

	/** Comes down at `at` as the arm again (nothing, if thrown from creative), to be picked up; it never despawns. */
	private void land(ServerLevel level, Vec3 at) {
		if (!creative) {
			ItemEntity item = new ItemEntity(level, at.x, at.y, at.z, getItem().copy());
			item.setDeltaMovement(0.0, 0.1, 0.0);
			item.setUnlimitedLifetime();
			item.setPickUpDelay(10);
			level.addFreshEntity(item);
		}
		level.sendParticles(ParticleTypes.CRIT, at.x, at.y, at.z, 4, 0.1, 0.1, 0.1, 0.05);
		discard();
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putBoolean("creative", creative);
		output.putBoolean("returning", isReturning());
		output.putInt("flight", flight);
		output.putDouble("from_x", from.x);
		output.putDouble("from_y", from.y);
		output.putDouble("from_z", from.z);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		creative = input.getBooleanOr("creative", false);
		entityData.set(RETURNING, input.getBooleanOr("returning", false));
		flight = input.getIntOr("flight", 0);
		from = new Vec3(input.getDoubleOr("from_x", getX()), input.getDoubleOr("from_y", getY()), input.getDoubleOr("from_z", getZ()));
	}
}
