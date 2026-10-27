package io.github.jimbozoomer.jugcraft.herobrine;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.Saved;
import java.util.List;
import java.util.Random;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/** One world-wide ledger, always stored in the Overworld, never a static cross-world player cache. */
public final class HerobrineProgress extends SavedData {
	private static final Codec<Long> NON_NEGATIVE = Codec.LONG.validate(value -> value >= 0
			? DataResult.success(value) : DataResult.error(() -> "Negative Herobrine progress"));
	public static final Codec<HerobrineSchedule.Progress> ENTRY = RecordCodecBuilder.create(i -> i.group(
			UUIDUtil.CODEC.fieldOf("player").forGetter(HerobrineSchedule.Progress::player),
			NON_NEGATIVE.fieldOf("active_ticks").forGetter(HerobrineSchedule.Progress::activeTicks),
			NON_NEGATIVE.fieldOf("due_at").forGetter(HerobrineSchedule.Progress::dueAt),
			Codec.intRange(1, Integer.MAX_VALUE).fieldOf("interval").forGetter(HerobrineSchedule.Progress::interval),
			Codec.BOOL.fieldOf("event_mode").forGetter(HerobrineSchedule.Progress::eventMode),
			NON_NEGATIVE.fieldOf("sightings").forGetter(HerobrineSchedule.Progress::sightings),
			Codec.LONG.fieldOf("last_seen").forGetter(HerobrineSchedule.Progress::lastSeenEpochSecond),
			NON_NEGATIVE.fieldOf("strong_due_at").forGetter(HerobrineSchedule.Progress::strongDueAt),
			NON_NEGATIVE.fieldOf("retry_at").forGetter(HerobrineSchedule.Progress::retryAt)
	).apply(i, HerobrineSchedule.Progress::new));
	public static final Codec<HerobrineProgress> CODEC = Saved.versioned("herobrine_progress",
			ENTRY.listOf().xmap(HerobrineProgress::new, p -> p.schedule.snapshot()));
	private static final SavedDataType<HerobrineProgress> TYPE = new SavedDataType<>(Jugcraft.id("herobrine_progress"),
			() -> new HerobrineProgress(List.of()), CODEC, null);
	private final HerobrineSchedule schedule;
	private long savedRevision;
	private HerobrineProgress(List<HerobrineSchedule.Progress> saved) {
		schedule = new HerobrineSchedule(HerobrineSeason.DEFAULT, new Random(), saved);
	}
	public static HerobrineProgress get(MinecraftServer server) {
		return server.overworld().getDataStorage().computeIfAbsent(TYPE);
	}
	public HerobrineSchedule schedule() { return schedule; }
	/** Call after every batch/callback. Mark dirty only on actual progress changes; vanilla owns disk scheduling. */
	public void flushChanges() {
		if (savedRevision != schedule.revision()) {
			savedRevision = schedule.revision();
			setDirty();
		}
	}
}
