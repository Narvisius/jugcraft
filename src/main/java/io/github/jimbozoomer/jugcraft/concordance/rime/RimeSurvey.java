package io.github.jimbozoomer.jugcraft.concordance.rime;

import io.github.jimbozoomer.jugcraft.concordance.ConcordanceProgress;
import io.github.jimbozoomer.jugcraft.concordance.JugcraftConcordance;
import io.github.jimbozoomer.jugcraft.concordance.alchemy.Axis;
import io.github.jimbozoomer.jugcraft.concordance.alchemy.Vector;
import io.github.jimbozoomer.jugcraft.concordance.rules.Evidence;
import io.github.jimbozoomer.jugcraft.concordance.rules.ResearchState;
import io.github.jimbozoomer.jugcraft.config.JugcraftConfig;
import io.github.jimbozoomer.jugcraft.lair.Lair;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityLevelChangeEvents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

/**
 * Rimekeeping's winter survey (tools/concordance_rime.py SURVEY): arriving in the Glacier Hall by any route, or a brew that
 * carries Rime bottled by hand at an Alembic Crucible. Gated by First Light, not Rimekeeping, so either route can bring
 * understanding (02:136). The hall is caught by Fabric's level change (Lairs move players with teleportTo, Lairs.java:346):
 * no lair code changes.
 */
public final class RimeSurvey {
	public static final String ACTIVITY = "jugcraft:winter_survey";
	public static final String RESEARCH = "jugcraft:first_light";
	public static final String KEY_GLACIER_HALL = "glacier_hall";
	public static final String KEY_CRUCIBLE = "crucible";

	private RimeSurvey() {
	}

	static void register() {
		ServerEntityLevelChangeEvents.AFTER_PLAYER_CHANGE_LEVEL.register((player, origin, destination) -> arrived(player, destination.dimension()));
	}

	/** A player arrived in {@code level}: in the Glacier Hall's, the field study. */
	public static boolean arrived(ServerPlayer player, ResourceKey<Level> level) {
		return Lair.GLACIER_HALL.dimension.equals(level) && survey(player, KEY_GLACIER_HALL);
	}

	/** A dose bottled by hand (CrucibleBlockEntity.bottle): one that carries Rime is the workshop study. */
	public static boolean bottled(ServerPlayer player, Vector dose) {
		return dose.get(Axis.RIME) > 0 && survey(player, KEY_CRUCIBLE);
	}

	/** The survey, for someone who understands First Light; nothing while the Concordance is switched off (lairs are not). */
	private static boolean survey(ServerPlayer player, String key) {
		return JugcraftConfig.isFeatureEnabled(JugcraftConcordance.FEATURE)
				&& ConcordanceProgress.knowledge(player).state(RESEARCH).atLeast(ResearchState.UNDERSTOOD)
				&& ConcordanceProgress.record(player, new Evidence.Practiced(ACTIVITY, key)).recorded();
	}
}
