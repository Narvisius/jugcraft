package io.github.jimbozoomer.jugcraft.agriculture;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * Harvest Cheer: the warmth of a burning Harvest Effigy. A player near one gets Regeneration I for
 * {@value #REGENERATION_TICKS} ticks and Luck for {@value #LUCK_TICKS}, once a night however many effigies burn. The
 * server remembers the last night each player was cheered (data/jugcraft_harvest_cheer.dat in the Overworld), so leaving
 * and coming back, or a restart, gives no second helping.
 */
public final class HarvestCheer {
	public static final int REGENERATION_TICKS = 600;
	public static final int LUCK_TICKS = 6000;

	private HarvestCheer() {
	}

	/** Which night {@code dayTime} falls in: day 0's night is 0. */
	public static long night(long dayTime) {
		return Math.floorDiv(dayTime, 24000L);
	}

	/** Cheers {@code player} unless they were cheered tonight; returns whether they were. */
	public static boolean give(ServerLevel level, ServerPlayer player) {
		Data data = level.getServer().overworld().getDataStorage().computeIfAbsent(Data.TYPE);
		long tonight = night(level.getOverworldClockTime());
		Long last = data.nights.get(player.getUUID());
		if (last != null && last == tonight) {
			return false;
		}
		data.nights.put(player.getUUID(), tonight);
		data.setDirty();
		player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, REGENERATION_TICKS, 0));
		player.addEffect(new MobEffectInstance(MobEffects.LUCK, LUCK_TICKS, 0));
		player.sendOverlayMessage(Component.translatable("message.jugcraft.effigy.cheer"));
		return true;
	}

	/** Whether {@code player} has had tonight's cheer. */
	public static boolean cheered(ServerLevel level, UUID player) {
		Data data = level.getServer().overworld().getDataStorage().computeIfAbsent(Data.TYPE);
		Long last = data.nights.get(player);
		return last != null && last == night(level.getOverworldClockTime());
	}

	static final class Data extends SavedData {
		static final Codec<Data> CODEC = RecordCodecBuilder.create(i -> i.group(
				Codec.unboundedMap(UUIDUtil.STRING_CODEC, Codec.LONG).fieldOf("nights").forGetter(d -> d.nights))
				.apply(i, Data::new));
		static final SavedDataType<Data> TYPE = new SavedDataType<>(Jugcraft.id("harvest_cheer"), Data::new, CODEC, null);

		final Map<UUID, Long> nights = new HashMap<>();

		Data() {
		}

		Data(Map<UUID, Long> saved) {
			nights.putAll(saved);
		}
	}
}
