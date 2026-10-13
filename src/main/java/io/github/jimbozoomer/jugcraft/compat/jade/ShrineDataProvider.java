package io.github.jimbozoomer.jugcraft.compat.jade;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.relic.Context;
import io.github.jimbozoomer.jugcraft.concordance.relic.RelicDefinition;
import io.github.jimbozoomer.jugcraft.concordance.reliquary.Reliquary;
import io.github.jimbozoomer.jugcraft.concordance.reliquary.ReliquaryShrineBlockEntity;
import java.util.stream.Collectors;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IServerDataProvider;

/**
 * A Reliquary Shrine for Jade (roadmap step 20): the relic it holds, its charge, and whether it is working or exactly
 * why not, with the contexts the relic works in (for the reasons that name them). Names no player.
 */
public enum ShrineDataProvider implements IServerDataProvider<BlockAccessor> {
	INSTANCE;

	public static final Identifier ID = Jugcraft.id("reliquary_shrine");
	public static final String KEY = Jugcraft.MOD_ID + ":reliquary_shrine";

	@Override
	public Identifier getUid() {
		return ID;
	}

	@Override
	public void appendServerData(CompoundTag data, BlockAccessor accessor) {
		if (accessor.getBlockEntity() instanceof ReliquaryShrineBlockEntity shrine) {
			CompoundTag snapshot = new CompoundTag();
			ItemStack relic = shrine.relic();
			snapshot.putString("status", shrine.status());
			if (!relic.isEmpty()) {
				RelicDefinition definition = Reliquary.definition(relic);
				snapshot.putString("relic", relic.getItem().getDescriptionId());
				snapshot.putInt("charge", Reliquary.state(relic).charge());
				snapshot.putInt("capacity", definition == null ? 0 : definition.capacity());
				snapshot.putString("contexts", definition == null ? ""
						: definition.contexts().stream().map(context -> context.id).collect(Collectors.joining(",")));
				snapshot.putString("mode", definition == null || definition.mode(Context.INSTALLED) == null ? ""
						: definition.mode(Context.INSTALLED).id());
			}
			data.put(KEY, snapshot);
		}
	}
}
