package io.github.jimbozoomer.jugcraft.lair;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.config.JugcraftConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

/**
 * The Kiln Seal's pressing, which opens the Cinder Kiln (docs/features/cinder-kiln.md): the seal pressed into a magma block
 * ({@code #jugcraft:kiln_seal_ground}) in the Nether, or in a volcanic land of the Overworld
 * ({@code #jugcraft:kiln_seal_lands}: the Volcano and the Cinder Barrens), at any hour. The magma cracks open and a vent of
 * sparks and smoke (a {@link MistGateEntity}) rises from it; the seal is used up and the presser sinks through the stone,
 * landing on the kiln's ledge. For {@code lairs.gate_seconds} the vent is the gate: anyone who uses it follows, while there
 * is room. Nobody is taken in by standing near it.
 *
 * <p>When something is missing (not a magma block, not the Nether or a volcanic land, a vent already open within
 * {@value #VENT_RANGE} blocks) the seal only says which and nothing is used up; when every instance of the kiln is taken,
 * the same. The Cinder Tyrant is no Witching Season boss: the seal works all year, whatever {@code lairs.off_season}
 * says.
 */
public final class KilnSealRite {
	public static final TagKey<Block> GROUND = TagKey.create(Registries.BLOCK, Jugcraft.id("kiln_seal_ground"));
	public static final TagKey<Biome> LANDS = TagKey.create(Registries.BIOME, Jugcraft.id("kiln_seal_lands"));
	public static final int COOLDOWN = 40;
	/** How near another open vent into the kiln must not be (blocks). */
	public static final int VENT_RANGE = 3;

	private KilnSealRite() {
	}

	/** What pressing the seal here found missing, in the order it is checked, or null when the kiln opens. */
	public enum Missing {
		DISABLED, NOT_MAGMA, NOT_VOLCANIC, VENTING, FULL
	}

	/** The result of a press: what was missing (or null), and the instance opened. */
	public record Press(@Nullable Missing missing, @Nullable LairInstance instance) {
	}

	/** Presses {@code seal} into the block at {@code magma} for {@code player}. */
	public static Press press(ServerLevel level, ServerPlayer player, BlockPos magma, ItemStack seal) {
		return press(level, player, magma, seal, volcanic(level.dimension(), level.getBiome(magma)));
	}

	/**
	 * Presses {@code seal} into the block at {@code magma} for {@code player}, whether the land is {@code volcanic} as
	 * given (game tests set it). Opens an instance of the kiln and takes the player in, or says what is missing and uses
	 * nothing.
	 */
	public static Press press(ServerLevel level, ServerPlayer player, BlockPos magma, ItemStack seal, boolean volcanic) {
		level.playSound(null, magma, SoundEvents.STONE_PLACE, SoundSource.PLAYERS, 1.0F, 0.6F);
		Missing missing = check(level, magma, volcanic);
		if (missing != null) {
			player.sendOverlayMessage(message(missing));
			return new Press(missing, null);
		}
		LairInstance instance = Lairs.open(level.getServer(), Lair.CINDER_KILN);
		if (instance == null) {
			player.sendOverlayMessage(message(Missing.FULL));
			return new Press(Missing.FULL, null);
		}
		seal.consume(1, player);
		level.playSound(null, magma, SoundEvents.BLAZE_SHOOT, SoundSource.BLOCKS, 1.0F, 0.5F);
		level.playSound(null, magma, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 1.0F, 0.6F);
		level.sendParticles(ParticleTypes.LAVA, magma.getX() + 0.5, magma.getY() + 1.0, magma.getZ() + 0.5, 16, 0.4, 0.1, 0.4, 0.0);
		level.sendParticles(ParticleTypes.LARGE_SMOKE, magma.getX() + 0.5, magma.getY() + 1.2, magma.getZ() + 0.5, 20, 0.4, 0.4, 0.4, 0.04);
		MistGateEntity.open(level, magma, instance, Lairs.gateTicks());
		player.sendSystemMessage(Component.translatable("message.jugcraft.lair.seal.opened"));
		follow(player, instance);
		return new Press(null, instance);
	}

	/** Checks a press into the block at {@code magma} without changing anything. */
	public static @Nullable Missing check(ServerLevel level, BlockPos magma) {
		return check(level, magma, volcanic(level.dimension(), level.getBiome(magma)));
	}

	/** Checks a press into the block at {@code magma}, whether the land is {@code volcanic} as given, changing nothing. */
	public static @Nullable Missing check(ServerLevel level, BlockPos magma, boolean volcanic) {
		if (!JugcraftConfig.isFeatureEnabled(JugcraftLairs.FEATURE)) {
			return Missing.DISABLED;
		}
		if (!level.getBlockState(magma).is(GROUND)) {
			return Missing.NOT_MAGMA;
		}
		if (!volcanic) {
			return Missing.NOT_VOLCANIC;
		}
		if (vent(level, magma) != null) {
			return Missing.VENTING;
		}
		return null;
	}

	/** Whether the seal takes in this dimension and biome: anywhere in the Nether, or in a volcanic land of the Overworld. */
	public static boolean volcanic(ResourceKey<Level> dimension, Holder<Biome> biome) {
		return dimension == Level.NETHER || dimension == Level.OVERWORLD && biome.is(LANDS);
	}

	public static Component message(Missing missing) {
		return switch (missing) {
			case DISABLED -> Component.translatable("message.jugcraft.lair.rite.disabled");
			case NOT_MAGMA -> Component.translatable("message.jugcraft.lair.seal.not_magma");
			case NOT_VOLCANIC -> Component.translatable("message.jugcraft.lair.seal.not_volcanic");
			case VENTING -> Component.translatable("message.jugcraft.lair.seal.venting");
			case FULL -> Component.translatable("message.jugcraft.lair.full", Component.translatable("lair.jugcraft.cinder_kiln"));
		};
	}

	/** An open vent into the Cinder Kiln within {@value #VENT_RANGE} blocks of {@code at}, or null. */
	public static @Nullable MistGateEntity vent(ServerLevel level, BlockPos at) {
		AABB near = new AABB(at.getX() - VENT_RANGE, at.getY() - VENT_RANGE, at.getZ() - VENT_RANGE, at.getX() + VENT_RANGE + 1,
				at.getY() + VENT_RANGE + 1, at.getZ() + VENT_RANGE + 1);
		for (MistGateEntity gate : level.getEntitiesOfClass(MistGateEntity.class, near, gate -> gate.lair() == Lair.CINDER_KILN)) {
			if (gate.instance() != null) {
				return gate;
			}
		}
		return null;
	}

	/** Takes {@code player} into the kiln through the stone: they sink through, and land on the ledge. */
	static boolean follow(ServerPlayer player, LairInstance instance) {
		ServerLevel from = (ServerLevel) player.level();
		BlockPos at = player.blockPosition();
		if (!Lairs.enter(player, instance)) {
			return false;
		}
		from.playSound(null, at, SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 0.8F, 0.5F);
		from.sendParticles(ParticleTypes.LARGE_SMOKE, at.getX() + 0.5, at.getY() + 0.5, at.getZ() + 0.5, 12, 0.3, 0.5, 0.3, 0.02);
		return true;
	}
}
