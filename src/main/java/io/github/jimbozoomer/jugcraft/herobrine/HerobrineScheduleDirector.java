package io.github.jimbozoomer.jugcraft.herobrine;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.config.JugcraftConfig;
import io.github.jimbozoomer.jugcraft.creatures.herobrine.Herobrine;
import io.github.jimbozoomer.jugcraft.creatures.herobrine.HerobrineDirector;
import io.github.jimbozoomer.jugcraft.creatures.herobrine.HerobrineEncounters;
import java.time.Clock;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;

/** Minecraft adapter for the shared Claude/Codex director seam. All callbacks stay on the server thread. */
public final class HerobrineScheduleDirector implements HerobrineDirector {
	private final MinecraftServer server;
	private final HerobrineProgress data;
	private final Clock clock;
	private final io.github.jimbozoomer.jugcraft.creatures.herobrine.HerobrineDiscoveries discoveries;
	public HerobrineScheduleDirector(MinecraftServer server, Clock clock, HerobrineSeason season) {
		this.server = server;
		this.clock = clock;
		this.discoveries = new io.github.jimbozoomer.jugcraft.creatures.herobrine.HerobrineDiscoveries(server, season);
		data = HerobrineProgress.get(server);
		data.schedule().season(season);
	}
	/** Call once during mod initialization, after HerobrineEncounters.register(). */
	public static void register() {
		ServerLifecycleEvents.SERVER_STARTED.register(server -> HerobrineEncounters.setDirector(
				new HerobrineScheduleDirector(server, Clock.systemUTC(), configuredSeason())));
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> HerobrineEncounters.setDirector(HerobrineDirector.NONE));
	}
	private static HerobrineSeason configuredSeason() {
		try {
			return new HerobrineSeason(LocalDate.parse(JugcraftConfig.textOption("herobrine.event_last_day")),
					ZoneId.of(JugcraftConfig.textOption("herobrine.timezone")));
		} catch (DateTimeException e) {
			Jugcraft.LOGGER.warn("Invalid Herobrine date/timezone; using November 3, 2026 inclusive in America/New_York", e);
			return HerobrineSeason.DEFAULT;
		}
	}
	@Override public List<Request> due(MinecraftServer server, List<Candidate> candidates) {
		if (server != this.server) return List.of();
		discoveries.tick(candidates, clock.instant());
		var requests = data.schedule().step(clock.instant(), candidates.stream().map(c ->
				new HerobrineSchedule.Candidate(c.player().getUUID(), c.active() && c.player().containerMenu == c.player().inventoryMenu,
						c.calm())).toList());
		data.flushChanges();
		return requests.stream().map(r -> new Request(r.player(), r.kind() == HerobrineSchedule.Kind.CLOSE
				? Herobrine.Kind.CLOSE : Herobrine.Kind.DISTANT, r.token())).toList();
	}
	private static HerobrineSchedule.Request core(Request request) {
		return new HerobrineSchedule.Request(request.player(), request.kind() == Herobrine.Kind.CLOSE
				? HerobrineSchedule.Kind.CLOSE : HerobrineSchedule.Kind.WATCHER, request.reservation());
	}
	/** Persistent narrative eligibility (HerobrineDirector.hasSeen): saved sightings, not this session's introductions. */
	@Override public boolean hasSeen(UUID player) {
		var progress = data.schedule().progress(player);
		return progress != null && progress.sightings() > 0;
	}
	@Override public void placed(Request request, UUID encounter) { data.schedule().placed(core(request), encounter); }
	@Override public void failed(Request request, Failure failure) {
		data.schedule().failed(core(request)); data.flushChanges();
	}
	@Override public void seen(Request request, UUID encounter, UUID player) {
		var witness = server.getPlayerList().getPlayer(player);
		// Recheck live eligibility: a menu, death or disconnect may occur between the once-a-second snapshots.
		if (witness == null || !witness.isAlive() || witness.isSpectator() || witness.level().dimension() != Level.OVERWORLD
				|| !HerobrineEncounters.attentive(witness, server.getTickCount())) return;
		data.schedule().seen(core(request), encounter, player, clock.instant()); data.flushChanges();
	}
	@Override public void ended(Request request, UUID encounter, Herobrine.Outcome outcome, Set<UUID> witnesses) {
		data.schedule().ended(core(request), encounter); data.flushChanges();
	}
}
