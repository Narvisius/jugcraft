package io.github.jimbozoomer.jugcraft.lair;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.agriculture.HalloweenSeason;
import io.github.jimbozoomer.jugcraft.config.JugcraftConfig;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.jspecify.annotations.Nullable;

/**
 * The Last Rites, which open the Hollow Acre (docs/features/hollow-acre.md): a grave (any headstone,
 * {@code #jugcraft:headstones}) in the Overworld, at night, with at least {@value #CANDLES} lit candles
 * ({@code #jugcraft:last_rites_candles}) within {@value #CANDLE_RANGE} blocks of it and a Mourning Wreath laid on it or
 * beside it; then the Death Knell rung within {@value #GRAVE_RANGE} blocks. The candles flare blue, the wreath is taken,
 * a gate of grey mist opens over the grave and the ringer goes through. When a step is missing the knell's chime says
 * which, and nothing is used up; when every instance of the lair is taken, the same.
 */
public final class LastRites {
	public static final TagKey<Block> GRAVES = TagKey.create(Registries.BLOCK, Jugcraft.id("headstones"));
	public static final TagKey<Block> CANDLES_TAG = TagKey.create(Registries.BLOCK, Jugcraft.id("last_rites_candles"));
	public static final int GRAVE_RANGE = 4;
	public static final int CANDLE_RANGE = 4;
	public static final int CANDLES = 4;
	public static final int WREATH_RANGE = 2;
	public static final int KNELL_COOLDOWN = 40;

	private LastRites() {
	}

	/** What ringing the knell here found missing, in the order it is checked, or null when the rites are complete. */
	public enum Missing {
		DISABLED, NO_GRAVE, NOT_OVERWORLD, NOT_NIGHT, OFF_SEASON, CANDLES, NO_WREATH, FULL
	}

	/** The result of ringing: what was missing (or null), the grave, the candles counted and the instance opened. */
	public record Ring(@Nullable Missing missing, @Nullable BlockPos grave, int candles, @Nullable LairInstance instance) {
	}

	/** Rings the Death Knell where {@code player} stands. */
	public static Ring ring(ServerLevel level, ServerPlayer player) {
		return ring(level, player, !level.isBrightOutside());
	}

	/** Rings the Death Knell where {@code player} stands, {@code night} as given (game tests set it). */
	public static Ring ring(ServerLevel level, ServerPlayer player, boolean night) {
		BlockPos at = player.blockPosition();
		level.playSound(null, at, SoundEvents.BELL_BLOCK, SoundSource.PLAYERS, 1.0F, 0.6F);
		Ring ring = check(level, at, night);
		if (ring.missing() != null) {
			player.sendOverlayMessage(message(ring));
			return ring;
		}
		LairInstance instance = Lairs.open(level.getServer(), Lair.HOLLOW_ACRE);
		if (instance == null) {
			Ring full = new Ring(Missing.FULL, ring.grave(), ring.candles(), null);
			player.sendOverlayMessage(message(full));
			return full;
		}
		BlockPos grave = ring.grave();
		BlockPos wreath = wreath(level, grave);
		if (wreath != null) {
			level.removeBlock(wreath, false);
		}
		for (BlockPos candle : candles(level, grave)) {
			level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, candle.getX() + 0.5, candle.getY() + 0.7, candle.getZ() + 0.5, 8, 0.15, 0.2, 0.15, 0.02);
		}
		level.playSound(null, grave, SoundEvents.BELL_RESONATE, SoundSource.BLOCKS, 1.2F, 0.5F);
		MistGateEntity.open(level, grave, instance, Lairs.gateTicks());
		player.sendSystemMessage(Component.translatable("message.jugcraft.lair.rite.opened"));
		Lairs.enter(player, instance);
		return new Ring(null, grave, ring.candles(), instance);
	}

	/** Checks the rites at {@code at} without changing anything. */
	public static Ring check(Level level, BlockPos at) {
		return check(level, at, !level.isBrightOutside());
	}

	/** Checks the rites at {@code at} without changing anything, {@code night} as given. */
	public static Ring check(Level level, BlockPos at, boolean night) {
		if (!JugcraftConfig.isFeatureEnabled(JugcraftLairs.FEATURE)) {
			return new Ring(Missing.DISABLED, null, 0, null);
		}
		BlockPos grave = grave(level, at);
		if (grave == null) {
			return new Ring(Missing.NO_GRAVE, null, 0, null);
		}
		if (level.dimension() != Level.OVERWORLD) {
			return new Ring(Missing.NOT_OVERWORLD, grave, 0, null);
		}
		if (!night) {
			return new Ring(Missing.NOT_NIGHT, grave, 0, null);
		}
		if (!Lairs.offSeason() && !HalloweenSeason.active()) {
			return new Ring(Missing.OFF_SEASON, grave, 0, null);
		}
		int candles = countCandles(level, grave);
		if (candles < CANDLES) {
			return new Ring(Missing.CANDLES, grave, candles, null);
		}
		if (wreath(level, grave) == null) {
			return new Ring(Missing.NO_WREATH, grave, candles, null);
		}
		return new Ring(null, grave, candles, null);
	}

	public static Component message(Ring ring) {
		return switch (ring.missing()) {
			case DISABLED -> Component.translatable("message.jugcraft.lair.rite.disabled");
			case NO_GRAVE -> Component.translatable("message.jugcraft.lair.rite.no_grave");
			case NOT_OVERWORLD -> Component.translatable("message.jugcraft.lair.rite.not_overworld");
			case NOT_NIGHT -> Component.translatable("message.jugcraft.lair.rite.not_night");
			case OFF_SEASON -> Component.translatable("message.jugcraft.lair.rite.off_season");
			case CANDLES -> Component.translatable("message.jugcraft.lair.rite.candles", ring.candles(), CANDLES);
			case NO_WREATH -> Component.translatable("message.jugcraft.lair.rite.no_wreath");
			case FULL -> Component.translatable("message.jugcraft.lair.full", Component.translatable("lair.jugcraft.hollow_acre"));
			case null -> Component.translatable("message.jugcraft.lair.rite.opened");
		};
	}

	/** The nearest block of a headstone within {@value #GRAVE_RANGE} blocks of {@code at}. */
	public static @Nullable BlockPos grave(Level level, BlockPos at) {
		BlockPos best = null;
		double bestDistance = Double.MAX_VALUE;
		for (BlockPos pos : BlockPos.betweenClosed(at.offset(-GRAVE_RANGE, -GRAVE_RANGE, -GRAVE_RANGE),
				at.offset(GRAVE_RANGE, GRAVE_RANGE, GRAVE_RANGE))) {
			if (level.getBlockState(pos).is(GRAVES)) {
				double distance = pos.distSqr(at);
				if (distance < bestDistance) {
					bestDistance = distance;
					best = pos.immutable();
				}
			}
		}
		return best;
	}

	/** The lit candles round a grave, a block of vanilla candles counting each of its candles. */
	public static int countCandles(Level level, BlockPos grave) {
		int count = 0;
		for (BlockPos pos : candles(level, grave)) {
			BlockState state = level.getBlockState(pos);
			count += state.hasProperty(CandleBlock.CANDLES) ? state.getValue(CandleBlock.CANDLES) : 1;
		}
		return count;
	}

	private static List<BlockPos> candles(Level level, BlockPos grave) {
		List<BlockPos> out = new ArrayList<>();
		for (BlockPos pos : BlockPos.betweenClosed(grave.offset(-CANDLE_RANGE, -CANDLE_RANGE, -CANDLE_RANGE),
				grave.offset(CANDLE_RANGE, CANDLE_RANGE, CANDLE_RANGE))) {
			BlockState state = level.getBlockState(pos);
			if (state.is(CANDLES_TAG) && state.hasProperty(BlockStateProperties.LIT) && state.getValue(BlockStateProperties.LIT)) {
				out.add(pos.immutable());
			}
		}
		return out;
	}

	/** A Mourning Wreath laid on the grave or beside it (within {@value #WREATH_RANGE} blocks of the grave block). */
	public static @Nullable BlockPos wreath(Level level, BlockPos grave) {
		for (BlockPos pos : BlockPos.betweenClosed(grave.offset(-WREATH_RANGE, -1, -WREATH_RANGE),
				grave.offset(WREATH_RANGE, WREATH_RANGE, WREATH_RANGE))) {
			if (level.getBlockState(pos).is(JugcraftLairs.MOURNING_WREATH)) {
				return pos.immutable();
			}
		}
		return null;
	}
}
