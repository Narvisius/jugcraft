package io.github.jimbozoomer.jugcraft.tower.crew;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * Who stands for each crew post of each tower in a dimension (by the Tower Core's position): the crew member's UUID,
 * or since when the post has stood empty (a death, or someone gone missing), how many the post has hired and the skin
 * its hand wears (so two in one tower never share a face). A post with no entry has never been staffed.
 */
public final class TowerCrewState extends SavedData {
	/** One post's record. {@code vacant}: the game time it fell empty, or -1 while someone stands for it. */
	public record Slot(long core, String post, Optional<UUID> crew, long vacant, int hires, String skin) {
		static final Codec<Slot> CODEC = RecordCodecBuilder.create(i -> i.group(
				Codec.LONG.fieldOf("core").forGetter(Slot::core),
				Codec.STRING.fieldOf("post").forGetter(Slot::post),
				UUIDUtil.CODEC.optionalFieldOf("crew").forGetter(Slot::crew),
				Codec.LONG.optionalFieldOf("vacant", -1L).forGetter(Slot::vacant),
				Codec.INT.optionalFieldOf("hires", 0).forGetter(Slot::hires),
				Codec.STRING.optionalFieldOf("skin", "").forGetter(Slot::skin)).apply(i, Slot::new));
	}

	static final Codec<TowerCrewState> CODEC = Slot.CODEC.listOf().xmap(TowerCrewState::new, TowerCrewState::slots);
	static final SavedDataType<TowerCrewState> TYPE = new SavedDataType<>(Jugcraft.id("tower_crew"), TowerCrewState::new, CODEC, null);

	private final Map<Long, Map<String, Slot>> towers = new HashMap<>();

	public TowerCrewState() {
	}

	private TowerCrewState(List<Slot> slots) {
		for (Slot slot : slots) {
			towers.computeIfAbsent(slot.core(), k -> new LinkedHashMap<>()).put(slot.post(), slot);
		}
	}

	public static TowerCrewState get(ServerLevel level) {
		return level.getDataStorage().computeIfAbsent(TYPE);
	}

	private List<Slot> slots() {
		List<Slot> out = new ArrayList<>();
		towers.values().forEach(m -> out.addAll(m.values()));
		return out;
	}

	public @Nullable Slot slot(BlockPos core, String post) {
		Map<String, Slot> posts = towers.get(core.asLong());
		return posts == null ? null : posts.get(post);
	}

	/** The skins the tower's crew wear now. */
	public List<String> worn(BlockPos core) {
		List<String> out = new ArrayList<>();
		Map<String, Slot> posts = towers.get(core.asLong());
		if (posts != null) {
			for (Slot s : posts.values()) {
				if (s.crew().isPresent() && !s.skin().isEmpty()) {
					out.add(s.skin());
				}
			}
		}
		return out;
	}

	/** A new hand stands for the post. */
	public void hired(BlockPos core, String post, UUID crew, String skin) {
		Slot old = slot(core, post);
		put(new Slot(core.asLong(), post, Optional.of(crew), -1L, old == null ? 1 : old.hires() + 1, skin));
	}

	/** The post's hand died (or went missing) at {@code time}; only the hand on record can leave it. */
	public void vacate(BlockPos core, String post, @Nullable UUID crew, long time) {
		Slot old = slot(core, post);
		if (old == null || old.vacant() >= 0 || crew != null && !old.crew().map(crew::equals).orElse(false)) {
			return;
		}
		put(new Slot(core.asLong(), post, Optional.empty(), time, old.hires(), old.skin()));
	}

	/** The hand on record turned up after all (its chunk was slow to load): the post is staffed again. */
	public void found(BlockPos core, String post, UUID crew) {
		Slot old = slot(core, post);
		if (old != null && old.vacant() >= 0) {
			put(new Slot(core.asLong(), post, Optional.of(crew), -1L, old.hires(), old.skin()));
		}
	}

	private void put(Slot slot) {
		towers.computeIfAbsent(slot.core(), k -> new LinkedHashMap<>()).put(slot.post(), slot);
		setDirty();
	}

	/** Forgets a tower (its core is gone). */
	public void forget(BlockPos core) {
		if (towers.remove(core.asLong()) != null) {
			setDirty();
		}
	}
}
