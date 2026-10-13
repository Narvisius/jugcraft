package io.github.jimbozoomer.jugcraft.concordance.spirits;

import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.Saved;
import io.github.jimbozoomer.jugcraft.concordance.resource.BoundWill;
import io.github.jimbozoomer.jugcraft.concordance.resource.BoundWillLedger;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * Every sealed agreement's Bound Will record (roadmap steps 7 and 17), saved with the world
 * (data/jugcraft_bound_wills.dat in the Overworld): an agreement with an identity, held by one player, never merged,
 * split or counted. Sealing a spirit's agreement adds one; releasing it (the anchor broken, the holder's release)
 * removes it.
 */
public final class BoundWills extends SavedData {
	private static final Codec<BoundWill> RECORD = RecordCodecBuilder.create(i -> i.group(
			UUIDUtil.CODEC.fieldOf("id").forGetter(BoundWill::id), Codec.STRING.fieldOf("agreement").forGetter(BoundWill::agreement),
			Codec.STRING.fieldOf("counterpart").forGetter(BoundWill::counterpart), UUIDUtil.CODEC.fieldOf("holder").forGetter(BoundWill::holder),
			Codec.LONG.fieldOf("sealed_at").forGetter(BoundWill::sealedAt), Codec.BOOL.fieldOf("transferable").forGetter(BoundWill::transferable))
			.apply(i, BoundWill::new));
	/** Versioned, and read will by will: one that cannot be read is kept as written (roadmap step 30). */
	public static final Codec<BoundWills> CODEC = Saved.versioned("bound_wills", Saved.keeping("bound_wills", UUIDUtil.STRING_CODEC, RECORD)
			.xmap(kept -> new BoundWills(new BoundWillLedger(kept.read()), kept.unread()), wills -> new Saved.Kept<>(wills.ledger.records(), wills.unread)));
	static final SavedDataType<BoundWills> TYPE = new SavedDataType<>(Jugcraft.id("bound_wills"), BoundWills::new, CODEC, null);

	private BoundWillLedger ledger;
	/** Bound Wills saved in a form this version cannot read, kept to be written back unchanged. */
	private final Map<String, Dynamic<?>> unread = new LinkedHashMap<>();

	BoundWills() {
		this(BoundWillLedger.EMPTY);
	}

	BoundWills(BoundWillLedger ledger) {
		this.ledger = ledger;
	}

	private BoundWills(BoundWillLedger ledger, Map<String, Dynamic<?>> unread) {
		this(ledger);
		this.unread.putAll(unread);
	}

	public static BoundWills of(MinecraftServer server) {
		return server.overworld().getDataStorage().computeIfAbsent(TYPE);
	}

	public BoundWillLedger ledger() {
		return ledger;
	}

	public BoundWillLedger.Change seal(BoundWill record) {
		BoundWillLedger.Change change = ledger.seal(record);
		if (change.outcome() == BoundWillLedger.Outcome.DONE) {
			ledger = change.ledger();
			setDirty();
		}
		return change;
	}

	public BoundWillLedger.Change release(UUID id, UUID holder) {
		BoundWillLedger.Change change = ledger.release(id, holder);
		if (change.outcome() == BoundWillLedger.Outcome.DONE) {
			ledger = change.ledger();
			setDirty();
		}
		return change;
	}
}
