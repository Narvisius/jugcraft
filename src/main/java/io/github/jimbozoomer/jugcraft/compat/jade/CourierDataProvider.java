package io.github.jimbozoomer.jugcraft.compat.jade;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.courier.CourierLedger;
import io.github.jimbozoomer.jugcraft.concordance.courier.CourierPostBlockEntity;
import io.github.jimbozoomer.jugcraft.concordance.courier.Couriers;
import io.github.jimbozoomer.jugcraft.concordance.logistics.Event;
import io.github.jimbozoomer.jugcraft.concordance.logistics.Place;
import io.github.jimbozoomer.jugcraft.concordance.logistics.Request;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IServerDataProvider;

/**
 * A Courier Post for Jade (roadmap step 18): how many of its requests are open and in transit, and its latest history
 * line (kind, request, amount and note). Names no player.
 */
public enum CourierDataProvider implements IServerDataProvider<BlockAccessor> {
	INSTANCE;

	public static final Identifier ID = Jugcraft.id("courier_post");
	public static final String KEY = Jugcraft.MOD_ID + ":courier_post";

	@Override
	public Identifier getUid() {
		return ID;
	}

	@Override
	public void appendServerData(CompoundTag data, BlockAccessor accessor) {
		if (accessor.getBlockEntity() instanceof CourierPostBlockEntity post && accessor.getLevel() instanceof ServerLevel level) {
			CourierLedger ledger = CourierLedger.of(level.getServer());
			Place at = Couriers.place(level, post.getBlockPos());
			int open = 0;
			int moving = 0;
			for (Request request : ledger.ledger().requests()) {
				if (request.ticket().post().equals(at)) {
					open++;
					if (request.progress().carried() > 0) {
						moving++;
					}
				}
			}
			CompoundTag snapshot = new CompoundTag();
			snapshot.putInt("open", open);
			snapshot.putInt("moving", moving);
			List<Event> history = ledger.ledger().history(at);
			if (!history.isEmpty()) {
				Event last = history.get(history.size() - 1);
				snapshot.putString("kind", last.kind());
				snapshot.putLong("request", last.request());
				snapshot.putInt("amount", last.amount());
				snapshot.putString("note", last.note());
			}
			data.put(KEY, snapshot);
		}
	}
}
