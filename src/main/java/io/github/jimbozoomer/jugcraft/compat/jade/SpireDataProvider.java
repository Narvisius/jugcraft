package io.github.jimbozoomer.jugcraft.compat.jade;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.spire.ConcordSpire;
import io.github.jimbozoomer.jugcraft.concordance.spire.SpireHeartBlockEntity;
import io.github.jimbozoomer.jugcraft.concordance.spire.SpireRecord;
import io.github.jimbozoomer.jugcraft.concordance.wonder.SpireConfiguration;
import io.github.jimbozoomer.jugcraft.concordance.wonder.SpireDefinition;
import io.github.jimbozoomer.jugcraft.concordance.wonder.SpireState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IServerDataProvider;

/**
 * A Spire Heart for Jade (roadmap step 25): its configuration, its phase (or raised), what it shows (working, or why its
 * field rests), the days of upkeep held and its store against a day's upkeep. The server's own record, never a client's
 * guess; names no player.
 */
public enum SpireDataProvider implements IServerDataProvider<BlockAccessor> {
	INSTANCE;

	public static final Identifier ID = Jugcraft.id("spire_heart");
	public static final String KEY = Jugcraft.MOD_ID + ":spire_heart";

	@Override
	public Identifier getUid() {
		return ID;
	}

	@Override
	public void appendServerData(CompoundTag data, BlockAccessor accessor) {
		if (accessor.getBlockEntity() instanceof SpireHeartBlockEntity heart && accessor.getLevel() instanceof ServerLevel level) {
			CompoundTag snapshot = new CompoundTag();
			snapshot.putString("status", heart.status());
			SpireDefinition definition = ConcordSpire.definition();
			SpireState state = SpireRecord.of(level.getServer()).spire(ConcordSpire.id(level, heart.getBlockPos()));
			if (definition != null && state != null) {
				snapshot.putString("configuration", state.configuration());
				snapshot.putInt("phase", state.phase());
				snapshot.putInt("phases", definition.phases().size());
				snapshot.putString("phase_id", state.raised(definition) ? "" : definition.phases().get(state.phase()).id());
				snapshot.putInt("sustained", state.sustained());
				SpireConfiguration configuration = ConcordSpire.catalog().configuration(state.configuration());
				if (configuration != null) {
					snapshot.putString("item", configuration.upkeepItem());
					snapshot.putInt("stock", heart.count(configuration.upkeepItem()));
					snapshot.putInt("daily", configuration.upkeepCount());
				}
			}
			data.put(KEY, snapshot);
		}
	}
}
