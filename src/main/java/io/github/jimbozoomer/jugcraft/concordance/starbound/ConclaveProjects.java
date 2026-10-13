package io.github.jimbozoomer.jugcraft.concordance.starbound;

import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.Saved;
import io.github.jimbozoomer.jugcraft.concordance.conclave.ProjectState;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import org.jspecify.annotations.Nullable;

/**
 * The Conclave's world record (roadmap step 23): every owner's current project (personal or a party's), the projects
 * each owner has finished, and what is owed to players who were offline when they earned it (a teaching, a stage's or a
 * project's renown and reward), given when they next join. One place, saved with the world, so a project never depends
 * on a block standing or a player being online.
 */
public final class ConclaveProjects extends SavedData {
	/**
	 * Something owed to an offline player: a {@code teaching} ({@code subject} the research, {@code detail} the learner) or
	 * a {@code project} award ({@code subject} the project, {@code detail} the stage or "complete"), with its tradition,
	 * renown and reward.
	 */
	public record Owed(String kind, String subject, String detail, String tradition, int renown, String reward, int count) {
		static final Codec<Owed> CODEC = RecordCodecBuilder.create(i -> i.group(Codec.STRING.fieldOf("kind").forGetter(Owed::kind),
				Codec.STRING.fieldOf("subject").forGetter(Owed::subject), Codec.STRING.fieldOf("detail").forGetter(Owed::detail),
				Codec.STRING.fieldOf("tradition").forGetter(Owed::tradition), Codec.INT.fieldOf("renown").forGetter(Owed::renown),
				Codec.STRING.fieldOf("reward").forGetter(Owed::reward), Codec.INT.fieldOf("count").forGetter(Owed::count)).apply(i, Owed::new));
	}

	static final Codec<ProjectState> PROJECT_CODEC = RecordCodecBuilder.create(i -> i.group(
			Codec.STRING.fieldOf("project").forGetter(ProjectState::project), Codec.STRING.fieldOf("owner").forGetter(ProjectState::owner),
			UUIDUtil.CODEC.fieldOf("founder").forGetter(ProjectState::founder), Codec.LONG.fieldOf("started").forGetter(ProjectState::started),
			Codec.INT.fieldOf("stage").forGetter(ProjectState::stage),
			Codec.unboundedMap(Codec.STRING, Codec.INT).fieldOf("progress").forGetter(ProjectState::progress),
			UUIDUtil.CODEC.listOf().xmap(HashSet::new, List::copyOf).fieldOf("contributors").forGetter(state -> new HashSet<>(state.contributors())),
			Codec.LONG.listOf().xmap(HashSet::new, List::copyOf).fieldOf("days").forGetter(state -> new HashSet<>(state.days())),
			UUIDUtil.CODEC.listOf().xmap(HashSet::new, List::copyOf).fieldOf("everyone").forGetter(state -> new HashSet<>(state.everyone())),
			Codec.LONG.fieldOf("finished").forGetter(ProjectState::finished)).apply(i, ProjectState::new));

	private record Stored(Saved.Kept<String, ProjectState> projects, Map<String, List<String>> finished, Map<String, List<Owed>> owed,
			Map<String, List<ProjectState>> setAside) {
		static final Codec<Stored> CODEC = RecordCodecBuilder.create(i -> i.group(
				Saved.keeping("conclave", Codec.STRING, PROJECT_CODEC).fieldOf("projects").forGetter(Stored::projects),
				Codec.unboundedMap(Codec.STRING, Codec.STRING.listOf()).fieldOf("finished").forGetter(Stored::finished),
				Codec.unboundedMap(Codec.STRING, Owed.CODEC.listOf()).fieldOf("owed").forGetter(Stored::owed),
				Codec.unboundedMap(Codec.STRING, PROJECT_CODEC.listOf()).optionalFieldOf("set_aside", Map.of()).forGetter(Stored::setAside))
				.apply(i, Stored::new));
	}

	/** Versioned, and read project by project: one that cannot be read is kept as written (roadmap step 30). */
	public static final Codec<ConclaveProjects> CODEC = Saved.versioned("conclave", Stored.CODEC.xmap(ConclaveProjects::new, ConclaveProjects::saved));
	static final SavedDataType<ConclaveProjects> TYPE = new SavedDataType<>(Jugcraft.id("conclave"), ConclaveProjects::new, CODEC, null);

	private final Map<String, ProjectState> projects = new LinkedHashMap<>();
	private final Map<String, List<String>> finished = new LinkedHashMap<>();
	private final Map<UUID, List<Owed>> owed = new LinkedHashMap<>();
	/** Projects saved in a form this version cannot read, kept to be written back unchanged. */
	private final Map<String, Dynamic<?>> unreadProjects = new LinkedHashMap<>();
	/** Unfinished projects whose definitions are gone, set aside by owner as they were (roadmap step 30). */
	private final Map<String, List<ProjectState>> setAside = new LinkedHashMap<>();

	ConclaveProjects() {
	}

	private ConclaveProjects(Stored saved) {
		projects.putAll(saved.projects().read());
		unreadProjects.putAll(saved.projects().unread());
		saved.setAside().forEach((owner, list) -> setAside.put(owner, new ArrayList<>(list)));
		saved.finished().forEach((owner, ids) -> finished.put(owner, new ArrayList<>(ids)));
		saved.owed().forEach((player, list) -> {
			try {
				owed.put(UUID.fromString(player), new ArrayList<>(list));
			} catch (IllegalArgumentException ignored) {
				// Not a player id: nothing can be owed to it.
			}
		});
	}

	private Stored saved() {
		Map<String, List<Owed>> byPlayer = new LinkedHashMap<>();
		owed.forEach((player, list) -> byPlayer.put(player.toString(), List.copyOf(list)));
		Map<String, List<String>> done = new LinkedHashMap<>();
		finished.forEach((owner, ids) -> done.put(owner, List.copyOf(ids)));
		Map<String, List<ProjectState>> aside = new LinkedHashMap<>();
		setAside.forEach((owner, list) -> aside.put(owner, List.copyOf(list)));
		return new Stored(new Saved.Kept<>(projects, unreadProjects), done, byPlayer, aside);
	}

	public static ConclaveProjects of(MinecraftServer server) {
		return server.overworld().getDataStorage().computeIfAbsent(TYPE);
	}

	public @Nullable ProjectState project(String owner) {
		return projects.get(owner);
	}

	public void put(ProjectState state) {
		projects.put(state.owner(), state);
		setDirty();
	}

	/**
	 * Sets aside {@code owner}'s current project, whose definition is gone (a data pack removed or renamed it): kept
	 * exactly as it was, with everyone's contributions, so an operator can restore it, but no longer in the way of a new
	 * project (roadmap step 30). Returns it, or null if there was none.
	 */
	public @Nullable ProjectState setAside(String owner) {
		ProjectState state = projects.remove(owner);
		if (state != null) {
			setAside.computeIfAbsent(owner, unused -> new ArrayList<>()).add(state);
			setDirty();
		}
		return state;
	}

	/** What {@code owner} has had set aside. */
	public List<ProjectState> setAsideFor(String owner) {
		return List.copyOf(setAside.getOrDefault(owner, List.of()));
	}

	/** Notes that {@code owner} finished {@code project}. */
	public void finish(String owner, String project) {
		finished.computeIfAbsent(owner, unused -> new ArrayList<>()).add(project);
		setDirty();
	}

	public List<String> finished(String owner) {
		return List.copyOf(finished.getOrDefault(owner, List.of()));
	}

	public void owe(UUID player, Owed debt) {
		owed.computeIfAbsent(player, unused -> new ArrayList<>()).add(debt);
		setDirty();
	}

	/** Everything owed to {@code player}, removed from the record (the caller gives it). */
	public List<Owed> settle(UUID player) {
		List<Owed> due = owed.remove(player);
		if (due == null) {
			return List.of();
		}
		setDirty();
		return due;
	}
}
