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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

/**
 * The Frost Horn's call, which opens the Glacier Hall (docs/features/glacier-hall.md): the horn blown in the Overworld at
 * night, standing on snow or ice ({@code #jugcraft:frost_horn_ground}). Its call rolls over the snow and a roar answers
 * it; the snow at the blower's feet splits and a whirl of white mist (a {@link MistGateEntity}) rises there; the horn is
 * used up and the blower falls through, landing on the hall's ledge with frost on their skin ({@value #FROST_TICKS} of the
 * ticks that would freeze them through, so it does them no harm). For {@code lairs.gate_seconds} the whirl is the gate:
 * anyone who uses it follows, while there is room. Nobody is taken in by standing near it.
 *
 * <p>When a step is missing (by day, off the snow, outside the Overworld, a whirl already open within
 * {@value #WHIRL_RANGE} blocks) the horn only says which and nothing is used up; when every instance of the hall is
 * taken, the same. The Yeti King is no Witching Season boss: the horn works all year, whatever {@code lairs.off_season}
 * says.
 */
public final class FrostHornRite {
	public static final TagKey<Block> GROUND = TagKey.create(Registries.BLOCK, Jugcraft.id("frost_horn_ground"));
	public static final int COOLDOWN = 40;
	public static final int FROST_TICKS = 120;
	/** How near another open whirl into the hall must not be (blocks). */
	public static final int WHIRL_RANGE = 3;

	private FrostHornRite() {
	}

	/** What blowing the horn here found missing, in the order it is checked, or null when the call is answered. */
	public enum Missing {
		DISABLED, NOT_OVERWORLD, NOT_NIGHT, NO_SNOW, WHIRLING, FULL
	}

	/** The result of a blow: what was missing (or null), and the instance opened. */
	public record Call(@Nullable Missing missing, @Nullable LairInstance instance) {
	}

	/** Blows the Frost Horn where {@code player} stands. */
	public static Call blow(ServerLevel level, ServerPlayer player, ItemStack horn) {
		return blow(level, player, horn, !level.isBrightOutside());
	}

	/**
	 * Blows {@code horn} where {@code player} stands, {@code night} as given (game tests set it). Opens an instance of the
	 * hall and takes the player in, or says what is missing and uses nothing.
	 */
	public static Call blow(ServerLevel level, ServerPlayer player, ItemStack horn, boolean night) {
		BlockPos at = player.blockPosition();
		level.playSound(null, at, SoundEvents.RAID_HORN.value(), SoundSource.PLAYERS, 1.5F, 1.3F);
		Missing missing = check(level, at, night);
		if (missing != null) {
			player.sendOverlayMessage(message(missing));
			return new Call(missing, null);
		}
		LairInstance instance = Lairs.open(level.getServer(), Lair.GLACIER_HALL);
		if (instance == null) {
			player.sendOverlayMessage(message(Missing.FULL));
			return new Call(Missing.FULL, null);
		}
		horn.consume(1, player);
		level.playSound(null, at, SoundEvents.WARDEN_ROAR, SoundSource.HOSTILE, 1.0F, 0.8F);
		level.sendParticles(ParticleTypes.SNOWFLAKE, player.getX(), player.getY() + 0.3, player.getZ(), 40, 0.8, 0.2, 0.8, 0.08);
		MistGateEntity.open(level, at.below(), instance, Lairs.gateTicks());
		player.sendSystemMessage(Component.translatable("message.jugcraft.lair.horn.called"));
		follow(player, instance);
		return new Call(null, instance);
	}

	/** Checks the call at {@code at} (where the blower stands) without changing anything, {@code night} as given. */
	public static @Nullable Missing check(ServerLevel level, BlockPos at, boolean night) {
		if (!JugcraftConfig.isFeatureEnabled(JugcraftLairs.FEATURE)) {
			return Missing.DISABLED;
		}
		if (level.dimension() != Level.OVERWORLD) {
			return Missing.NOT_OVERWORLD;
		}
		if (!night) {
			return Missing.NOT_NIGHT;
		}
		if (!onSnow(level, at)) {
			return Missing.NO_SNOW;
		}
		if (whirl(level, at) != null) {
			return Missing.WHIRLING;
		}
		return null;
	}

	public static Component message(Missing missing) {
		return switch (missing) {
			case DISABLED -> Component.translatable("message.jugcraft.lair.rite.disabled");
			case NOT_OVERWORLD -> Component.translatable("message.jugcraft.lair.horn.not_overworld");
			case NOT_NIGHT -> Component.translatable("message.jugcraft.lair.horn.not_night");
			case NO_SNOW -> Component.translatable("message.jugcraft.lair.horn.no_snow");
			case WHIRLING -> Component.translatable("message.jugcraft.lair.horn.whirling");
			case FULL -> Component.translatable("message.jugcraft.lair.full", Component.translatable("lair.jugcraft.glacier_hall"));
		};
	}

	/** Whether someone at {@code at} stands on snow or ice: in it (a snow layer, powder snow) or on it. */
	public static boolean onSnow(Level level, BlockPos at) {
		return level.getBlockState(at).is(GROUND) || level.getBlockState(at.below()).is(GROUND);
	}

	/** An open whirl into the Glacier Hall within {@value #WHIRL_RANGE} blocks of {@code at}, or null. */
	public static @Nullable MistGateEntity whirl(ServerLevel level, BlockPos at) {
		AABB near = new AABB(at.getX() - WHIRL_RANGE, at.getY() - WHIRL_RANGE, at.getZ() - WHIRL_RANGE, at.getX() + WHIRL_RANGE + 1,
				at.getY() + WHIRL_RANGE + 1, at.getZ() + WHIRL_RANGE + 1);
		for (MistGateEntity gate : level.getEntitiesOfClass(MistGateEntity.class, near, gate -> gate.lair() == Lair.GLACIER_HALL)) {
			if (gate.instance() != null) {
				return gate;
			}
		}
		return null;
	}

	/** Takes {@code player} into the hall through the snow: they fall through, and land with frost on their skin. */
	static boolean follow(ServerPlayer player, LairInstance instance) {
		ServerLevel from = (ServerLevel) player.level();
		BlockPos at = player.blockPosition();
		if (!Lairs.enter(player, instance)) {
			return false;
		}
		from.playSound(null, at, SoundType.SNOW.getBreakSound(), SoundSource.PLAYERS, 1.0F, 0.7F);
		player.setTicksFrozen(Math.max(player.getTicksFrozen(), FROST_TICKS));
		return true;
	}
}
