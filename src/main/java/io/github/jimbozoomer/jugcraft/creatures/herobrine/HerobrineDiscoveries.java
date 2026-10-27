package io.github.jimbozoomer.jugcraft.creatures.herobrine;

import io.github.jimbozoomer.jugcraft.herobrine.HerobrineSeason;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.levelgen.Heightmap;

/** Additional exploration marks independent of seeing the apparition; no player progress/rewards are granted. */
public final class HerobrineDiscoveries {
    private final MinecraftServer server;
    private final HerobrineSeason season;
    private final Map<UUID, Integer> activeSeconds = new HashMap<>();
    public HerobrineDiscoveries(MinecraftServer server, HerobrineSeason season) { this.server = server; this.season = season; }

    /** Once per second; one bounded placement search globally per ten seconds, never per-player per-tick scans. */
    public void tick(List<HerobrineDirector.Candidate> candidates, Instant now) {
        if (!HerobrineMarks.enabled()) return;
        var online = new HashSet<UUID>();
        HerobrineDirector.Candidate chosen = null;
        int best = 0;
        for (var candidate : candidates) {
            UUID id = candidate.player().getUUID();
            online.add(id);
            if (!candidate.active() || !candidate.calm()) continue;
            int credit = activeSeconds.merge(id, 1, (a, b) -> Math.min(7200, a + b));
            var level = candidate.player().level();
            BlockPos pos = candidate.player().blockPosition();
            boolean underground = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, pos.getX(), pos.getZ()) >= pos.getY() + 8;
            int interval = (underground ? 180 : 900) * (season.eventActive(now) ? 1 : 6);
            int overdue = credit - interval;
            if (overdue >= best) { best = overdue; chosen = candidate; }
        }
        activeSeconds.keySet().retainAll(online);
        if (chosen == null || server.getTickCount() % 200 != 0 || HerobrineMarks.building() >= HerobrineMarks.MAX_JOBS) return;
        var player = chosen.player();
        var level = server.overworld();
        BlockPos at = player.blockPosition();
        boolean underground = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, at.getX(), at.getZ()) >= at.getY() + 8;
        HerobrineMarkPlans.Plan plan = underground ? HerobrineDepths.find(level, at, false) : null;
        if (plan == null) plan = HerobrineDepths.find(level, at, true);
        if (plan == null && !underground) plan = level.getRandom().nextInt(4) == 0
                ? HerobrineSkyIslands.find(level, at) : HerobrineSurfacePlans.find(level, at);
        // Failed searches wait a minute before retrying. Restart/reconnect resets conservatively; saved 3D spacing persists.
        int interval = (underground ? 180 : 900) * (season.eventActive(now) ? 1 : 6);
        activeSeconds.put(player.getUUID(), plan == null ? Math.max(0, interval - 60) : 0);
        if (plan != null) HerobrineMarks.start(level, plan);
    }
}
