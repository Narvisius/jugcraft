package io.github.jimbozoomer.jugcraft.concordance.sky;

import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.Saved;
import io.github.jimbozoomer.jugcraft.concordance.resource.AstralLedger;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * Which celestial occurrences have paid whom (roadmap step 15), saved with the world (data/jugcraft_astral_claims.dat in
 * the Overworld) so that a claim survives restarts and the keeper being offline: for each player, an {@link AstralLedger}
 * (gathering, and recalls under {@code recall:<pattern>}), and the patterns they have observed with the game time they
 * last did (what a recall may call back). Only the server writes it.
 */
public final class AstralClaims extends SavedData {
	private static final Codec<AstralLedger.Claim> CLAIM = RecordCodecBuilder.create(i -> i.group(
			Codec.LONG.fieldOf("occurrence").forGetter(AstralLedger.Claim::occurrence),
			Codec.LONG.fieldOf("game_time").forGetter(AstralLedger.Claim::gameTime)).apply(i, AstralLedger.Claim::new));
	private static final Codec<AstralLedger> LEDGER = Codec.unboundedMap(Codec.STRING, CLAIM).xmap(AstralLedger::new, AstralLedger::claims);
	/** Versioned, and read player by player: a player's claims that cannot be read are kept as written (step 30). */
	public static final Codec<AstralClaims> CODEC = Saved.versioned("astral_claims", RecordCodecBuilder.create(i -> i.group(
			Saved.keeping("astral_claims", UUIDUtil.STRING_CODEC, LEDGER).fieldOf("ledgers")
					.forGetter(d -> new Saved.Kept<>(d.ledgers, d.unreadLedgers)),
			Saved.keeping("astral_claims", UUIDUtil.STRING_CODEC, Codec.unboundedMap(Codec.STRING, Codec.LONG)).fieldOf("observed")
					.forGetter(d -> new Saved.Kept<>(d.observed, d.unreadObserved)))
			.apply(i, AstralClaims::new)));
	static final SavedDataType<AstralClaims> TYPE = new SavedDataType<>(Jugcraft.id("astral_claims"), AstralClaims::new, CODEC, null);

	private final Map<UUID, AstralLedger> ledgers = new HashMap<>();
	private final Map<UUID, Map<String, Long>> observed = new HashMap<>();
	/** Players' claims and observations saved in a form this version cannot read, kept to be written back unchanged. */
	private final Map<String, Dynamic<?>> unreadLedgers = new LinkedHashMap<>();
	private final Map<String, Dynamic<?>> unreadObserved = new LinkedHashMap<>();

	AstralClaims() {
	}

	AstralClaims(Map<UUID, AstralLedger> ledgers, Map<UUID, Map<String, Long>> observed) {
		this.ledgers.putAll(ledgers);
		observed.forEach((player, patterns) -> this.observed.put(player, new TreeMap<>(patterns)));
	}

	private AstralClaims(Saved.Kept<UUID, AstralLedger> ledgers, Saved.Kept<UUID, Map<String, Long>> observed) {
		this(ledgers.read(), observed.read());
		unreadLedgers.putAll(ledgers.unread());
		unreadObserved.putAll(observed.unread());
	}

	public static AstralClaims of(MinecraftServer server) {
		return server.overworld().getDataStorage().computeIfAbsent(TYPE);
	}

	public AstralLedger ledger(UUID player) {
		return ledgers.getOrDefault(player, AstralLedger.EMPTY);
	}

	/** Claims an occurrence of {@code key} for {@code player}; records it only when granted. */
	public AstralLedger.Outcome claim(UUID player, String key, long occurrence, long gameTime, long minGap) {
		AstralLedger.Result result = ledger(player).claim(key, occurrence, gameTime, minGap);
		if (result.outcome() == AstralLedger.Outcome.GRANTED) {
			ledgers.put(player, result.ledger());
			setDirty();
		}
		return result.outcome();
	}

	public void observe(UUID player, String pattern, long gameTime) {
		observed.computeIfAbsent(player, unused -> new TreeMap<>()).put(pattern, gameTime);
		setDirty();
	}

	/** The patterns a player has observed and when they last did (game time). */
	public Map<String, Long> observed(UUID player) {
		return observed.getOrDefault(player, Map.of());
	}
}
