package io.github.jimbozoomer.jugcraft.agriculture;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * A piñata (fall addition 28): a papier-mâché pumpkin, star or bat ({@link Kind}) hung on a rope from the underside of a
 * block, {@value #DROP} blocks above its foot. The client draws it (client/PinataRenderer.java) from
 * tools/pinata_data.py's quads, swinging when it is hit and torn once it has taken half its hits.
 *
 * <p>Anyone can fill it ({@link #fill}): a whole stack at a time, up to {@value #SLOTS} stacks. Whoever hung it can take
 * it down again ({@link #takeDown}), its contents and all. A charged swing by a player (at least {@value #CHARGED} of
 * their attack damage) is a hit, two with the Piñata Stick; weak swings only rock it, and nothing else touches it. On
 * its kind's last hit it bursts ({@link #burst}): confetti, its contents sprayed out, Piñata Party for whoever burst it
 * and Blind Luck if they wore a Blindfold. If what it hangs from goes, it falls: it drops itself and its contents.
 * Its numbers are tools/pinata.py's.
 */
public class Pinata extends Entity {
	public static final int SLOTS = 9;
	public static final double DROP = 1.5;
	public static final float CHARGED = 0.75F;
	public static final int STICK_HITS = 2;
	/** How often (ticks) it checks what it hangs from. */
	private static final int SUPPORT_EVERY = 20;

	/** The three kinds: their item, the hits each takes and their confetti's colours. */
	public enum Kind {
		PUMPKIN("pumpkin", "pumpkin_pinata", 8, new int[] {0xF08A1C, 0xFFB347, 0x3C8C2C, 0x1C1C1C}),
		STAR("star", "star_pinata", 10, new int[] {0xF04C8C, 0xFFD23C, 0x2CC4D8, 0xF0782C}),
		BAT("bat", "bat_pinata", 6, new int[] {0x2A2430, 0x7A3CA8, 0xC8C0D0, 0xE8442C});

		public final String id;
		public final String item;
		public final int hits;
		final int[] confetti;

		Kind(String id, String item, int hits, int[] confetti) {
			this.id = id;
			this.item = item;
			this.hits = hits;
			this.confetti = confetti;
		}

		public static Kind byId(String id) {
			for (Kind kind : values()) {
				if (kind.id.equals(id)) {
					return kind;
				}
			}
			return PUMPKIN;
		}
	}

	private static final EntityDataAccessor<Integer> KIND = SynchedEntityData.defineId(Pinata.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> HITS = SynchedEntityData.defineId(Pinata.class, EntityDataSerializers.INT);
	/** Counts every swing at it, hit or not, for the client to set it swinging; and whether the last was a hit. */
	private static final EntityDataAccessor<Integer> SWINGS = SynchedEntityData.defineId(Pinata.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Boolean> STRONG = SynchedEntityData.defineId(Pinata.class, EntityDataSerializers.BOOLEAN);

	private final List<ItemStack> contents = new ArrayList<>();
	private @Nullable UUID owner;

	public Pinata(EntityType<? extends Pinata> type, Level level) {
		super(type, level);
		noPhysics = true;
		setNoGravity(true);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		builder.define(KIND, Kind.PUMPKIN.ordinal());
		builder.define(HITS, 0);
		builder.define(SWINGS, 0);
		builder.define(STRONG, false);
	}

	/** Hangs a piñata of {@code kind} for {@code owner} under the block at {@code support}; returns it, or null. */
	public static @Nullable Pinata hang(ServerLevel level, BlockPos support, Kind kind, @Nullable Player owner) {
		Pinata pinata = JugcraftAgriculture.PINATA.create(level, EntitySpawnReason.TRIGGERED);
		if (pinata == null) {
			return null;
		}
		pinata.setPos(support.getX() + 0.5, support.getY() - DROP, support.getZ() + 0.5);
		pinata.setKind(kind);
		pinata.owner = owner == null ? null : owner.getUUID();
		return level.addFreshEntity(pinata) ? pinata : null;
	}

	/** Whether a block can hold a piñata hung from its underside: it has something solid to tie the rope to. */
	public static boolean canHangFrom(Level level, BlockPos support) {
		return !level.getBlockState(support).getCollisionShape(level, support).isEmpty();
	}

	public Kind kind() {
		int index = entityData.get(KIND);
		return index >= 0 && index < Kind.values().length ? Kind.values()[index] : Kind.PUMPKIN;
	}

	public void setKind(Kind kind) {
		entityData.set(KIND, kind.ordinal());
	}

	/** Hits it has taken (it bursts at its kind's {@link Kind#hits}). */
	public int hits() {
		return entityData.get(HITS);
	}

	/** Whether it has taken half its hits or more, and is drawn torn. */
	public boolean torn() {
		return hits() * 2 >= kind().hits;
	}

	public int swings() {
		return entityData.get(SWINGS);
	}

	public boolean strongSwing() {
		return entityData.get(STRONG);
	}

	public List<ItemStack> contents() {
		return List.copyOf(contents);
	}

	public boolean ownedBy(Player player) {
		return owner == null || owner.equals(player.getUUID());
	}

	/** Where its rope is tied: the block it hangs from. */
	public BlockPos support() {
		return BlockPos.containing(getX(), getY() + DROP + 0.01, getZ());
	}

	/** Puts a copy of {@code stack} in, whole, if a slot is free; returns whether it went in. */
	public boolean fill(ItemStack stack) {
		if (stack.isEmpty() || contents.size() >= SLOTS) {
			return false;
		}
		contents.add(stack.copy());
		return true;
	}

	/** Takes it down for {@code player}: its contents and the piñata itself go to them (or at their feet). */
	public void takeDown(ServerLevel level, ServerPlayer player) {
		for (ItemStack stack : contents) {
			give(level, player, stack);
		}
		contents.clear();
		give(level, player, new ItemStack(JugcraftAgriculture.item(kind().item)));
		level.playSound(null, getX(), getY() + 0.5, getZ(), SoundEvents.BUNDLE_INSERT, SoundSource.PLAYERS, 0.8F, 0.8F);
		discard();
	}

	private static void give(ServerLevel level, ServerPlayer player, ItemStack stack) {
		if (!player.getInventory().add(stack)) {
			player.spawnAtLocation(level, stack);
		}
	}

	/** A swing at it from {@code player} with {@code amount} damage: a hit if it was charged; returns whether it was. */
	public boolean swingAt(ServerLevel level, Player player, float amount) {
		double full = player.getAttributeValue(Attributes.ATTACK_DAMAGE);
		boolean hit = amount >= CHARGED * full;
		entityData.set(SWINGS, swings() + 1);
		entityData.set(STRONG, hit);
		if (!hit) {
			level.playSound(null, getX(), getY() + 0.5, getZ(), SoundEvents.WOOL_HIT, SoundSource.PLAYERS, 0.5F, 1.4F);
			return false;
		}
		int add = player.getMainHandItem().is(JugcraftAgriculture.item(Pinatas.STICK)) ? STICK_HITS : 1;
		int hits = Math.min(kind().hits, hits() + add);
		entityData.set(HITS, hits);
		level.playSound(null, getX(), getY() + 0.5, getZ(), SoundEvents.WOOL_HIT, SoundSource.PLAYERS, 1.0F, 0.9F + 0.1F * random.nextFloat());
		confetti(level, 4, 0.1);
		if (hits >= kind().hits) {
			burst(level, player);
		}
		return true;
	}

	/** It bursts: confetti, its contents sprayed out round it, and the advancements for whoever burst it. */
	public void burst(ServerLevel level, @Nullable Player burster) {
		confetti(level, 60, 0.35);
		level.playSound(null, getX(), getY() + 0.5, getZ(), SoundEvents.FIREWORK_ROCKET_BLAST, SoundSource.PLAYERS, 1.0F, 1.2F);
		level.playSound(null, getX(), getY() + 0.5, getZ(), SoundEvents.WOOL_BREAK, SoundSource.PLAYERS, 1.0F, 0.8F);
		spill(level, true);
		if (burster instanceof ServerPlayer player) {
			TrickOrTreat.award(player, "pinata_party");
			if (player.getItemBySlot(EquipmentSlot.HEAD).is(JugcraftAgriculture.item(Pinatas.BLINDFOLD))) {
				TrickOrTreat.award(player, "blind_luck");
			}
		}
		discard();
	}

	/** Drops its contents where it hangs, sprayed out round it if {@code spray}. */
	private void spill(ServerLevel level, boolean spray) {
		for (ItemStack stack : contents) {
			ItemEntity item = new ItemEntity(level, getX(), getY() + 0.4, getZ(), stack.copy());
			if (spray) {
				double angle = random.nextDouble() * Math.PI * 2;
				double speed = 0.15 + random.nextDouble() * 0.15;
				item.setDeltaMovement(new Vec3(Math.cos(angle) * speed, 0.25 + random.nextDouble() * 0.15, Math.sin(angle) * speed));
			}
			item.setDefaultPickUpDelay();
			level.addFreshEntity(item);
		}
		contents.clear();
	}

	private void confetti(ServerLevel level, int count, double speed) {
		int[] colours = kind().confetti;
		for (int i = 0; i < colours.length; i++) {
			level.sendParticles(ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, 0xFF000000 | colours[i]), getX(), getY() + 0.45, getZ(),
					count / colours.length, 0.25, 0.25, 0.25, speed);
		}
	}

	@Override
	public void tick() {
		super.tick();
		if (level() instanceof ServerLevel level && tickCount % SUPPORT_EVERY == 0 && !canHangFrom(level, support())) {
			// What it hung from is gone: it falls, and drops itself and its contents.
			spill(level, false);
			spawnAtLocation(level, new ItemStack(JugcraftAgriculture.item(kind().item)));
			discard();
		}
	}

	/** Only a player's own swing reaches it; it takes no other harm. */
	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		if (isRemoved() || !(source.getEntity() instanceof Player player) || source.getDirectEntity() != player) {
			return false;
		}
		swingAt(level, player, amount);
		return true;
	}

	@Override
	public boolean isPickable() {
		return !isRemoved();
	}

	@Override
	public boolean isPushable() {
		return false;
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		setKind(Kind.byId(input.getStringOr("kind", Kind.PUMPKIN.id)));
		entityData.set(HITS, Math.max(0, input.getIntOr("hits", 0)));
		owner = input.read("owner", UUIDUtil.CODEC).orElse(null);
		contents.clear();
		input.read("contents", ItemStack.CODEC.listOf()).ifPresent(list -> list.stream().filter(stack -> !stack.isEmpty()).limit(SLOTS)
				.forEach(contents::add));
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		output.putString("kind", kind().id);
		output.putInt("hits", hits());
		if (owner != null) {
			output.store("owner", UUIDUtil.CODEC, owner);
		}
		output.store("contents", ItemStack.CODEC.listOf(), List.copyOf(contents));
	}
}
