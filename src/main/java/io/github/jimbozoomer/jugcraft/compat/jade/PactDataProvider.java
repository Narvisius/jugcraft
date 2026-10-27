package io.github.jimbozoomer.jugcraft.compat.jade;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.spirits.AgreementHolder;
import io.github.jimbozoomer.jugcraft.concordance.spirits.PactPerchBlockEntity;
import io.github.jimbozoomer.jugcraft.concordance.spirits.PactReading;
import io.github.jimbozoomer.jugcraft.concordance.spirits.SpiritAnchorBlockEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IServerDataProvider;

/**
 * Concordance Chunk D, D-06 (G11): a pact perch and a Spirit Anchor for Jade: whether it keeps an agreement, with whom, its
 * service and state, the next offering or the grace, and why its presence waits (PactReading: read only). Names no player
 * and shows nothing of the escrow. Loaded only through Jade's entrypoint.
 */
public enum PactDataProvider implements IServerDataProvider<BlockAccessor> {
	PERCH("pact_perch"),
	ANCHOR("spirit_anchor");

	public final Identifier id;
	public final String dataKey;

	PactDataProvider(String path) {
		this.id = Jugcraft.id(path);
		this.dataKey = Jugcraft.MOD_ID + ":" + path;
	}

	@Override
	public Identifier getUid() {
		return id;
	}

	@Override
	public void appendServerData(CompoundTag data, BlockAccessor accessor) {
		boolean mine = this == PERCH ? accessor.getBlockEntity() instanceof PactPerchBlockEntity
				: accessor.getBlockEntity() instanceof SpiritAnchorBlockEntity;
		if (mine && accessor.getBlockEntity() instanceof AgreementHolder.Holds holds && accessor.getLevel() instanceof ServerLevel level) {
			data.put(dataKey, PactReading.holder(level, holds, level.getGameTime()));
		}
	}
}
