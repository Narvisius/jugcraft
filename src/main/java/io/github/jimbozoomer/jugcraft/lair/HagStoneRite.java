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
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

/**
 * Looking through the Hag Stone, which opens the Lantern Fen (docs/features/lantern-fen.md): the stone looked through in
 * the Overworld at night, standing in shallow water or on mud ({@code #jugcraft:hag_stone_ground}) in a wetland
 * ({@code #jugcraft:hag_stone_lands}: every swamp, the Floodplain and the Muskeg). Through its hole a lantern burns far off
 * in mist; the water stills and mist rises from it with a marsh-light in it (a {@link MistGateEntity}); the stone is used
 * up and the fen takes the one who looked, who wakes on its jetty. For {@code lairs.gate_seconds} the mist is the gate:
 * anyone who uses it follows, while there is room. Nobody is taken in by standing near it.
 *
 * <p>When a step is missing (outside the Overworld, by day, out of the wetlands, out of the water and off the mud, a mist
 * already risen within {@value #MIST_RANGE} blocks) the stone only says which and nothing is used up; when every instance of
 * the fen is taken, the same. The Mire Hag is no Witching Season boss: the stone works all year, at night, whatever
 * {@code lairs.off_season} says.
 */
public final class HagStoneRite {
	public static final TagKey<Block> GROUND = TagKey.create(Registries.BLOCK, Jugcraft.id("hag_stone_ground"));
	public static final TagKey<Biome> LANDS = TagKey.create(Registries.BIOME, Jugcraft.id("hag_stone_lands"));
	public static final int COOLDOWN = 40;
	/** How near another open mist into the fen must not be (blocks). */
	public static final int MIST_RANGE = 3;

	private HagStoneRite() {
	}

	/** What looking through the stone here found missing, in the order it is checked, or null when the fen opens. */
	public enum Missing {
		DISABLED, NOT_OVERWORLD, NOT_NIGHT, NOT_WETLAND, NOT_WET, MISTING, FULL
	}

	/** The result of a look: what was missing (or null), and the instance opened. */
	public record Look(@Nullable Missing missing, @Nullable LairInstance instance) {
	}

	/** Looks through {@code stone} where {@code player} stands. */
	public static Look look(ServerLevel level, ServerPlayer player, ItemStack stone) {
		BlockPos feet = feet(player);
		return look(level, player, stone, !level.isBrightOutside(), level.getBiome(feet).is(LANDS));
	}

	/**
	 * Looks through {@code stone} where {@code player} stands, {@code night} and whether the land is a {@code wetland} as
	 * given (game tests set them). Opens an instance of the fen and takes the player in, or says what is missing and uses
	 * nothing.
	 */
	public static Look look(ServerLevel level, ServerPlayer player, ItemStack stone, boolean night, boolean wetland) {
		BlockPos feet = feet(player);
		level.playSound(null, feet, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0F, 0.6F);
		Missing missing = check(level, feet, night, wetland);
		if (missing != null) {
			player.sendOverlayMessage(message(missing));
			return new Look(missing, null);
		}
		LairInstance instance = Lairs.open(level.getServer(), Lair.LANTERN_FEN);
		if (instance == null) {
			player.sendOverlayMessage(message(Missing.FULL));
			return new Look(Missing.FULL, null);
		}
		stone.consume(1, player);
		level.playSound(null, feet, SoundEvents.WITCH_CELEBRATE, SoundSource.HOSTILE, 0.8F, 0.7F);
		level.playSound(null, feet, SoundEvents.BUBBLE_COLUMN_BUBBLE_POP, SoundSource.BLOCKS, 1.0F, 0.6F);
		level.sendParticles(ParticleTypes.CLOUD, player.getX(), feet.getY() + 0.2, player.getZ(), 30, 0.9, 0.2, 0.9, 0.01);
		level.sendParticles(ParticleTypes.GLOW, player.getX(), feet.getY() + 1.4, player.getZ(), 6, 0.3, 0.3, 0.3, 0.02);
		MistGateEntity.open(level, feet.below(), instance, Lairs.gateTicks());
		player.sendSystemMessage(Component.translatable("message.jugcraft.lair.stone.opened"));
		follow(player, instance);
		return new Look(null, instance);
	}

	/** Checks a look where someone stands with their feet in {@code feet}, without changing anything. */
	public static @Nullable Missing check(ServerLevel level, BlockPos feet, boolean night, boolean wetland) {
		if (!JugcraftConfig.isFeatureEnabled(JugcraftLairs.FEATURE)) {
			return Missing.DISABLED;
		}
		if (level.dimension() != Level.OVERWORLD) {
			return Missing.NOT_OVERWORLD;
		}
		if (!night) {
			return Missing.NOT_NIGHT;
		}
		if (!wetland) {
			return Missing.NOT_WETLAND;
		}
		if (!wet(level, feet)) {
			return Missing.NOT_WET;
		}
		if (mist(level, feet) != null) {
			return Missing.MISTING;
		}
		return null;
	}

	public static Component message(Missing missing) {
		return switch (missing) {
			case DISABLED -> Component.translatable("message.jugcraft.lair.rite.disabled");
			case NOT_OVERWORLD -> Component.translatable("message.jugcraft.lair.stone.not_overworld");
			case NOT_NIGHT -> Component.translatable("message.jugcraft.lair.stone.not_night");
			case NOT_WETLAND -> Component.translatable("message.jugcraft.lair.stone.not_wetland");
			case NOT_WET -> Component.translatable("message.jugcraft.lair.stone.not_wet");
			case MISTING -> Component.translatable("message.jugcraft.lair.stone.misting");
			case FULL -> Component.translatable("message.jugcraft.lair.full", Component.translatable("lair.jugcraft.lantern_fen"));
		};
	}

	/**
	 * The block a player's feet are in. Mud is lower than a whole block, so someone standing on it stands inside its cell:
	 * their feet are taken a little above where they stand, in the cell over the mud.
	 */
	public static BlockPos feet(Player player) {
		return BlockPos.containing(player.getX(), player.getY() + 0.2, player.getZ());
	}

	/**
	 * Whether someone with their feet in {@code feet} stands on mud, or in shallow water: water at their feet, none over it
	 * (so they are not under it) and none under it (so they are not swimming over deep water).
	 */
	public static boolean wet(Level level, BlockPos feet) {
		if (level.getBlockState(feet.below()).is(GROUND) || level.getBlockState(feet).is(GROUND)) {
			return true;
		}
		return level.getFluidState(feet).is(FluidTags.WATER) && !level.getFluidState(feet.above()).is(FluidTags.WATER)
				&& !level.getFluidState(feet.below()).is(FluidTags.WATER);
	}

	/** An open mist into the Lantern Fen within {@value #MIST_RANGE} blocks of {@code at}, or null. */
	public static @Nullable MistGateEntity mist(ServerLevel level, BlockPos at) {
		AABB near = new AABB(at.getX() - MIST_RANGE, at.getY() - MIST_RANGE, at.getZ() - MIST_RANGE, at.getX() + MIST_RANGE + 1,
				at.getY() + MIST_RANGE + 1, at.getZ() + MIST_RANGE + 1);
		for (MistGateEntity gate : level.getEntitiesOfClass(MistGateEntity.class, near, gate -> gate.lair() == Lair.LANTERN_FEN)) {
			if (gate.instance() != null) {
				return gate;
			}
		}
		return null;
	}

	/** Takes {@code player} into the fen through the mist: it closes round them, and they wake on the jetty. */
	static boolean follow(ServerPlayer player, LairInstance instance) {
		ServerLevel from = (ServerLevel) player.level();
		BlockPos at = player.blockPosition();
		if (!Lairs.enter(player, instance)) {
			return false;
		}
		from.playSound(null, at, SoundEvents.GENERIC_SPLASH, SoundSource.PLAYERS, 0.6F, 0.6F);
		from.sendParticles(ParticleTypes.CLOUD, at.getX() + 0.5, at.getY() + 0.8, at.getZ() + 0.5, 12, 0.3, 0.5, 0.3, 0.02);
		return true;
	}
}
