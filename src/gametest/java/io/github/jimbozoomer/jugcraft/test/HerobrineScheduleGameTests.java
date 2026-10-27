package io.github.jimbozoomer.jugcraft.test;

import com.mojang.serialization.JsonOps;
import io.github.jimbozoomer.jugcraft.herobrine.HerobrineProgress;
import io.github.jimbozoomer.jugcraft.herobrine.HerobrineSchedule;
import io.github.jimbozoomer.jugcraft.herobrine.HerobrineScheduleDirector;
import io.github.jimbozoomer.jugcraft.herobrine.HerobrineSeason;
import java.time.Instant;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/** Real Minecraft codec/SavedData checks; not a claim of rendered two-client visibility testing. */
public final class HerobrineScheduleGameTests {
	private static final Instant NOW = Instant.parse("2026-11-03T23:00:00-05:00");
	@GameTest(maxTicks=20)
	public void worldLedgerPersistsSeenAndUnseenPlayers(GameTestHelper h) {
		var data = HerobrineProgress.get(h.getLevel().getServer());
		var schedule = data.schedule(); UUID first = UUID.randomUUID(), second = UUID.randomUUID();
		var players = List.of(new HerobrineSchedule.Candidate(first, true, true), new HerobrineSchedule.Candidate(second, true, true));
		for (int i = 0; i < 600; i++) {
			for (var request : schedule.step(NOW, players)) {
				UUID entity = UUID.randomUUID(); schedule.placed(request, entity);
				if (request.player().equals(first)) schedule.seen(request, entity, first, NOW);
				schedule.ended(request, entity);
			}
		}
		data.flushChanges();
		h.assertTrue(data.isDirty(), "Progress changes mark native SavedData dirty");
		var json = HerobrineProgress.CODEC.encodeStart(JsonOps.INSTANCE, data).getOrThrow();
		var restored = HerobrineProgress.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow();
		h.assertTrue(restored.schedule().snapshot().equals(schedule.snapshot()), "Native codec preserves all progress and cooldowns");
		h.assertTrue(restored.schedule().progress(first).sightings() == 1, "Only visible first player is seen");
		h.assertTrue(restored.schedule().progress(second).sightings() == 0, "Unseen second player remains prioritized");
		h.assertTrue(restored.schedule().liveCount() == 0, "No dead reservations after restart");
		var director = new HerobrineScheduleDirector(h.getLevel().getServer(), java.time.Clock.fixed(NOW, java.time.ZoneOffset.UTC), HerobrineSeason.DEFAULT);
		h.assertTrue(director.hasSeen(first) && !director.hasSeen(second) && !director.hasSeen(UUID.randomUUID()),
				"Narrative eligibility reads saved sightings, not introductions this session");
		h.succeed();
	}
	@GameTest(maxTicks=20)
	public void invalidProgressIsRejectedByCodec(GameTestHelper h) {
		var entry = new HerobrineSchedule.Progress(UUID.randomUUID(), 100, 1000, 1000, true, 0, 0, 2000, 0);
		var json = HerobrineProgress.ENTRY.encodeStart(JsonOps.INSTANCE, entry).getOrThrow().getAsJsonObject();
		json.addProperty("active_ticks", -1);
		h.assertTrue(HerobrineProgress.ENTRY.parse(JsonOps.INSTANCE, json).error().isPresent(), "Reject corrupt negative playtime");
		h.succeed();
	}
	@GameTest(maxTicks=20)
	public void novemberThirdStaysActiveAfterUtcMidnight(GameTestHelper h) {
		h.assertTrue(HerobrineSeason.DEFAULT.eventActive(NOW), "Nov 3 23:00 local is still high activity");
		h.assertTrue(!HerobrineSeason.DEFAULT.eventActive(Instant.parse("2026-11-04T05:00:00Z")), "Taper at New York midnight Nov 4");
		h.succeed();
	}
	@GameTest(maxTicks=20)
	public void sharedWitnessDoesNotConsumeUnseenTargetsTurn(GameTestHelper h) {
		UUID target = UUID.randomUUID(), witness = UUID.randomUUID();
		var schedule = new HerobrineSchedule(HerobrineSeason.DEFAULT, new Random(13), List.of());
		HerobrineSchedule.Request request = null;
		for (int i = 0; i < 600 && request == null; i++) {
			var requests = schedule.step(NOW, List.of(new HerobrineSchedule.Candidate(target, true, true)));
			if (!requests.isEmpty()) request = requests.getFirst();
		}
		h.assertTrue(request != null, "First target has an encounter by ten active minutes");
		UUID entity = UUID.randomUUID(); schedule.placed(request, entity);
		schedule.step(NOW, List.of(new HerobrineSchedule.Candidate(target, true, true), new HerobrineSchedule.Candidate(witness, true, true)));
		schedule.seen(request, entity, witness, NOW); schedule.ended(request, entity);
		h.assertTrue(schedule.progress(target).sightings() == 0 && schedule.progress(witness).sightings() == 1,
				"Shared sighting only credits the actual witness");
		h.assertTrue(schedule.progress(target).retryAt() - schedule.progress(target).activeTicks() == 600,
				"Unseen target receives a thirty-active-second retry");
		h.succeed();
	}
}
