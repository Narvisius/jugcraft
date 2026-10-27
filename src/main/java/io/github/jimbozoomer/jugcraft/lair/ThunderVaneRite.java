package io.github.jimbozoomer.jugcraft.lair;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.config.JugcraftConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The Thunder Vane's raising, which opens the Crown of Thunder (docs/features/crown-of-thunder.md): the vane raised in the
 * Overworld while it rains or thunders, in the mountains ({@code #jugcraft:thunder_vane_lands}: the high country and every
 * mountain and hill biome), standing at a height of {@value #MIN_Y} or more, under the open sky. Lightning strikes the
 * vane (only light and thunder: it hurts nobody and lights nothing), the vane spins wild and is used up, and Grey Mist (a
 * {@link MistGateEntity}) tears open where it was raised; the raiser comes down on the Crown's terrace. For
 * {@code lairs.gate_seconds} the mist is the gate: anyone who uses it follows, while there is room. Nobody is taken in by
 * standing near it.
 *
 * <p>When a step is missing (outside the Overworld, fair weather, out of the mountains, too low, under a roof, a mist
 * already open within {@value #MIST_RANGE} blocks) the vane only says which and nothing is used up; when every instance of
 * the Crown is taken, the same. The Storm Roc is no Witching Season boss: the vane works all year, whatever
 * {@code lairs.off_season} says.
 */
public final class ThunderVaneRite {
	public static final TagKey<Biome> LANDS = TagKey.create(Registries.BIOME, Jugcraft.id("thunder_vane_lands"));
	public static final int COOLDOWN = 40;
	/** How near another open mist into the Crown must not be (blocks). */
	public static final int MIST_RANGE = 3;
	/** The least height to raise the vane at. */
	public static final int MIN_Y = 120;

	private ThunderVaneRite() {
	}

	/** What the raising found missing here, in the order it is checked, or null when it is answered. */
	public enum Missing {
		DISABLED, NOT_OVERWORLD, FAIR, NOT_MOUNTAINS, LOW, ROOFED, MISTING, FULL
	}

	/** The result of a raising: what was missing (or null), and the instance opened. */
	public record Raising(@Nullable Missing missing, @Nullable LairInstance instance) {
	}

	/** Raises {@code vane} where {@code player} stands. */
	public static Raising raise(ServerLevel level, ServerPlayer player, ItemStack vane) {
		BlockPos feet = HowlingTotemRite.feet(player);
		return raise(level, player, vane, level.isRaining() || level.isThundering(), level.getBiome(feet).is(LANDS));
	}

	/**
	 * Raises {@code vane} where {@code player} stands, whether it is {@code stormy} and whether the land is the mountains
	 * ({@code mountains}) as given (game tests set them). Opens an instance of the Crown and takes the player in, or says
	 * what is missing and uses nothing.
	 */
	public static Raising raise(ServerLevel level, ServerPlayer player, ItemStack vane, boolean stormy, boolean mountains) {
		BlockPos feet = HowlingTotemRite.feet(player);
		// The wind catches the vane.
		level.playSound(null, feet, SoundEvents.BREEZE_IDLE_AIR, SoundSource.PLAYERS, 1.0F, 0.7F);
		Missing missing = check(level, feet, stormy, mountains);
		if (missing != null) {
			player.sendOverlayMessage(message(missing));
			return new Raising(missing, null);
		}
		LairInstance instance = Lairs.open(level.getServer(), Lair.CROWN_OF_THUNDER);
		if (instance == null) {
			player.sendOverlayMessage(message(Missing.FULL));
			return new Raising(Missing.FULL, null);
		}
		vane.consume(1, player);
		strike(level, Vec3.atBottomCenterOf(feet));
		level.sendParticles(ParticleTypes.CLOUD, player.getX(), feet.getY() + 0.4, player.getZ(), 30, 1.0, 0.4, 1.0, 0.05);
		level.sendParticles(ParticleTypes.ELECTRIC_SPARK, player.getX(), feet.getY() + 1.4, player.getZ(), 24, 0.6, 0.8, 0.6, 0.2);
		MistGateEntity.open(level, feet.below(), instance, Lairs.gateTicks());
		player.sendSystemMessage(Component.translatable("message.jugcraft.lair.vane.opened"));
		follow(player, instance);
		return new Raising(null, instance);
	}

	/** Checks a raising where someone stands with their feet in {@code feet}, without changing anything. */
	public static @Nullable Missing check(ServerLevel level, BlockPos feet, boolean stormy, boolean mountains) {
		if (!JugcraftConfig.isFeatureEnabled(JugcraftLairs.FEATURE)) {
			return Missing.DISABLED;
		}
		if (level.dimension() != Level.OVERWORLD) {
			return Missing.NOT_OVERWORLD;
		}
		if (!stormy) {
			return Missing.FAIR;
		}
		if (!mountains) {
			return Missing.NOT_MOUNTAINS;
		}
		if (feet.getY() < MIN_Y) {
			return Missing.LOW;
		}
		if (HowlingTotemRite.roofed(level, feet)) {
			return Missing.ROOFED;
		}
		if (mist(level, feet) != null) {
			return Missing.MISTING;
		}
		return null;
	}

	public static Component message(Missing missing) {
		return switch (missing) {
			case DISABLED -> Component.translatable("message.jugcraft.lair.rite.disabled");
			case NOT_OVERWORLD -> Component.translatable("message.jugcraft.lair.vane.not_overworld");
			case FAIR -> Component.translatable("message.jugcraft.lair.vane.fair");
			case NOT_MOUNTAINS -> Component.translatable("message.jugcraft.lair.vane.not_mountains");
			case LOW -> Component.translatable("message.jugcraft.lair.vane.low", MIN_Y);
			case ROOFED -> Component.translatable("message.jugcraft.lair.vane.roofed");
			case MISTING -> Component.translatable("message.jugcraft.lair.vane.misting");
			case FULL -> Component.translatable("message.jugcraft.lair.full", Component.translatable("lair.jugcraft.crown_of_thunder"));
		};
	}

	/** Lightning that only shows: a flash and a thunderclap at {@code at}, hurting nobody, lighting nothing. */
	static void strike(ServerLevel level, Vec3 at) {
		LightningBolt bolt = EntityTypes.LIGHTNING_BOLT.create(level, EntitySpawnReason.TRIGGERED);
		if (bolt != null) {
			bolt.setVisualOnly(true);
			bolt.snapTo(at);
			level.addFreshEntity(bolt);
		}
	}

	/** An open mist into the Crown of Thunder within {@value #MIST_RANGE} blocks of {@code at}, or null. */
	public static @Nullable MistGateEntity mist(ServerLevel level, BlockPos at) {
		AABB near = new AABB(at.getX() - MIST_RANGE, at.getY() - MIST_RANGE, at.getZ() - MIST_RANGE, at.getX() + MIST_RANGE + 1,
				at.getY() + MIST_RANGE + 1, at.getZ() + MIST_RANGE + 1);
		for (MistGateEntity gate : level.getEntitiesOfClass(MistGateEntity.class, near, gate -> gate.lair() == Lair.CROWN_OF_THUNDER)) {
			if (gate.instance() != null) {
				return gate;
			}
		}
		return null;
	}

	/** Takes {@code player} into the Crown through the mist: the wind lifts them, and they come down on the terrace. */
	static boolean follow(ServerPlayer player, LairInstance instance) {
		ServerLevel from = (ServerLevel) player.level();
		BlockPos at = player.blockPosition();
		if (!Lairs.enter(player, instance)) {
			return false;
		}
		from.playSound(null, at, SoundEvents.BREEZE_IDLE_AIR, SoundSource.PLAYERS, 1.0F, 0.5F);
		from.sendParticles(ParticleTypes.CLOUD, at.getX() + 0.5, at.getY() + 0.8, at.getZ() + 0.5, 16, 0.3, 0.8, 0.3, 0.08);
		return true;
	}
}
