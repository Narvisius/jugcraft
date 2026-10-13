package io.github.jimbozoomer.jugcraft.compat.jade;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.spirits.ClockworkPorterEntity;
import io.github.jimbozoomer.jugcraft.concordance.spirits.GatheringShadeEntity;
import io.github.jimbozoomer.jugcraft.concordance.spirits.HearthlingEntity;
import io.github.jimbozoomer.jugcraft.concordance.spirits.WorkerEntity;
import io.github.jimbozoomer.jugcraft.concordance.worker.Bond;
import io.github.jimbozoomer.jugcraft.concordance.worker.WorkerDefinition;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IServerDataProvider;

/**
 * A familiar, spirit or construct for Jade (roadmap step 17): what it is doing or why not, and its own model's numbers
 * (a familiar's bond; what a spirit carries; a construct's integrity, energy and load). Names no player.
 */
public enum WorkerDataProvider implements IServerDataProvider<EntityAccessor> {
	INSTANCE;

	public static final Identifier ID = Jugcraft.id("worker");
	public static final String KEY = Jugcraft.MOD_ID + ":worker";

	@Override
	public Identifier getUid() {
		return ID;
	}

	@Override
	public void appendServerData(CompoundTag data, EntityAccessor accessor) {
		if (accessor.getEntity() instanceof WorkerEntity<?> worker) {
			data.put(KEY, snapshot(worker));
		}
	}

	public static CompoundTag snapshot(WorkerEntity<?> worker) {
		CompoundTag snapshot = new CompoundTag();
		snapshot.putString("status", worker.status().id);
		snapshot.putString("kind", worker.kind());
		if (worker instanceof HearthlingEntity hearthling) {
			snapshot.putInt("bond", hearthling.bond().strength());
			snapshot.putInt("bond_max", Bond.MAX);
		} else if (worker instanceof GatheringShadeEntity shade) {
			snapshot.putInt("carried", shade.carriedCount());
		} else if (worker instanceof ClockworkPorterEntity porter) {
			WorkerDefinition.Construct terms = ClockworkPorterEntity.terms();
			snapshot.putInt("integrity", porter.body().integrity());
			snapshot.putInt("integrity_max", terms == null ? 0 : terms.integrity());
			snapshot.putLong("energy", porter.body().energy());
			snapshot.putInt("energy_max", terms == null ? 0 : terms.energy());
			snapshot.putInt("carried", porter.carriedCount());
		}
		return snapshot;
	}
}
