package io.github.jimbozoomer.jugcraft.town;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Operator-made NPCs and guards, anywhere in the world (docs/features/custom-npcs.md).
 *
 * <p>Each is an ordinary {@link Townsfolk} (the town's own entity, look and behaviour) outside the town's list: its
 * spot is -1, so the town's builder, upgrades, roster and raids never touch it, and it is never tied to the generated
 * city. What an operator set on it lives in its {@link Data}, saved with the entity. A small index ({@link Registry})
 * numbers them for commands and remembers removals, so one removed while unloaded leaves when it next loads.
 *
 * <ul>
 * <li><b>NPCs</b> stand at their post, wander a few blocks, and say the operator's line (or the town's seasonal
 * lines).</li>
 * <li><b>Guards</b> carry the town guard's sword, behaviour and skin. They fight monsters only (never players or
 * pets: the town guard's targeting), hold their stand post, and walk an operator's patrol waypoints in order.</li>
 * </ul>
 *
 * <p>Every edit is an operator command ({@code /npc}, {@code /guard}, {@code /jugcraft npc}: {@link CustomNpcCommand}),
 * checked on the server. A player's skin comes from
 * their name, resolved off the server thread with a bound on outstanding lookups ({@link NpcSkins}), and the resolved
 * profile (textures included) is saved with the NPC. Placing or editing one never changes a block; nothing here loads
 * a chunk.
 */
public final class CustomNpcs {
	public static final String NPC = "npc", GUARD = "guard";
	public static final String SAVE_KEY = "jugcraft_custom_npc";
	public static final int MAX_NAME = 32, MAX_LINE = 256, MAX_LINES = 8, MAX_WAYPOINTS = 16, WAYPOINT_REACH = 64, MAX_WANDER = 16;
	public static final String DEFAULT_NPC_SKIN = "townsfolk_1", DEFAULT_GUARD_SKIN = "guard_blue";
	/** The synced skin value meaning "draw the synced player profile" (see {@link Townsfolk#profile()}). */
	public static final String PLAYER_SKIN = "player";
	/** Ticks a guard waits at each patrol waypoint, and how long it may make no progress before skipping one. */
	public static final int DWELL_TICKS = 60, STUCK_TICKS = 200;
	/** A guard holds within this many blocks of its stand post when it has no patrol. */
	public static final int GUARD_POST_RANGE = 0;
	private static final int MAX_TOMBSTONES = 256;

	private CustomNpcs() {
	}

	/** Registers the commands and the index's clean-up. Call once from {@link JugcraftTown#register()}. */
	public static void register() {
		CustomNpcCommand.register();
		ServerEntityEvents.ENTITY_UNLOAD.register((entity, level) -> {
			if (entity instanceof Townsfolk npc && npc.custom() != null && entity.getRemovalReason() != null
					&& entity.getRemovalReason().shouldDestroy()) {
				// Killed (only /kill can) or removed: it is gone for good.
				registry(level.getServer()).forget(npc.custom().id, npc.getUUID());
			}
		});
	}

	public static boolean isOperator(CommandSourceStack source) {
		return Commands.LEVEL_GAMEMASTERS.check(source.permissions());
	}

	// ---------------------------------------------------------------- what an operator set

	/** One NPC's settings, saved with the entity under {@link #SAVE_KEY}. */
	public static final class Data {
		public static final Codec<Data> CODEC = RecordCodecBuilder.create(i -> i.group(
				Codec.INT.fieldOf("id").forGetter(d -> d.id),
				Codec.STRING.fieldOf("kind").forGetter(d -> d.kind),
				Codec.STRING.optionalFieldOf("town_skin", DEFAULT_NPC_SKIN).forGetter(d -> d.townSkin),
				Codec.STRING.optionalFieldOf("skin_name", "").forGetter(d -> d.skinName),
				ResolvableProfile.CODEC.optionalFieldOf("profile").forGetter(d -> Optional.ofNullable(d.profile)),
				BlockPos.CODEC.listOf().optionalFieldOf("patrol", List.of()).forGetter(d -> d.patrol),
				Codec.INT.optionalFieldOf("patrol_index", 0).forGetter(d -> d.patrolIndex),
				Codec.STRING.listOf().optionalFieldOf("lines", List.of()).forGetter(d -> List.copyOf(d.lines)),
				Codec.INT.optionalFieldOf("wander", 2).forGetter(d -> d.wander),
				UUIDUtil.CODEC.optionalFieldOf("creator").forGetter(d -> Optional.ofNullable(d.creator))
		).apply(i, Data::new));

		int id;
		final String kind;
		String townSkin;
		/** The player whose skin it wears ("" for a town skin), and that player's resolved profile (textures included). */
		String skinName;
		@Nullable ResolvableProfile profile;
		final List<BlockPos> patrol;
		int patrolIndex;
		/** What it says, in turn, to whoever talks to it (none: the town's lines for the season). */
		final List<String> lines = new ArrayList<>();
		int wander;
		@Nullable UUID creator;
		/** The latest skin lookup asked for; a reply for an older one is stale and ignored. */
		int skinRequest;
		int lastTalk = -100;
		int nextLine;

		Data(int id, String kind, String townSkin, String skinName, Optional<ResolvableProfile> profile, List<BlockPos> patrol,
				int patrolIndex, List<String> lines, int wander, Optional<UUID> creator) {
			this.id = id;
			this.kind = GUARD.equals(kind) ? GUARD : NPC;
			this.townSkin = TOWN_SKIN.matcher(townSkin).matches() ? townSkin : this.kind.equals(GUARD) ? DEFAULT_GUARD_SKIN : DEFAULT_NPC_SKIN;
			this.skinName = NpcSkins.validName(skinName) ? skinName : "";
			this.profile = this.skinName.isEmpty() ? null : profile.orElse(null);
			this.patrol = new ArrayList<>(patrol.subList(0, Math.min(patrol.size(), MAX_WAYPOINTS)));
			this.patrolIndex = this.patrol.isEmpty() ? 0 : Math.floorMod(patrolIndex, this.patrol.size());
			for (String line : lines) {
				String said = clean(line, MAX_LINE);
				if (!said.isEmpty() && this.lines.size() < MAX_LINES) {
					this.lines.add(said);
				}
			}
			this.wander = Math.clamp(wander, 0, MAX_WANDER);
			this.creator = creator.orElse(null);
		}

		public int id() {
			return id;
		}

		public String kind() {
			return kind;
		}

		public boolean guard() {
			return kind.equals(GUARD);
		}

		public String skinName() {
			return skinName;
		}

		public @Nullable ResolvableProfile profile() {
			return profile;
		}

		public List<BlockPos> patrol() {
			return List.copyOf(patrol);
		}

		public int patrolIndex() {
			return patrolIndex;
		}

		public List<String> lines() {
			return List.copyOf(lines);
		}

		public int wander() {
			return wander;
		}

		public String townSkin() {
			return townSkin;
		}
	}

	private static final java.util.regex.Pattern TOWN_SKIN = java.util.regex.Pattern.compile("[a-z0-9_]{1,40}");

	/** Text an operator typed, without formatting codes or control characters, trimmed and bounded. */
	public static String clean(@Nullable String text, int max) {
		if (text == null) {
			return "";
		}
		StringBuilder out = new StringBuilder();
		for (int i = 0; i < text.length() && out.length() < max; i++) {
			char c = text.charAt(i);
			if (c == '§') {
				i++;   // a formatting code and the character after it
			} else if (!Character.isISOControl(c)) {
				out.append(c);
			}
		}
		return out.toString().trim();
	}

	/** Town skins an NPC may wear: those the town's own residents wear (every one has a texture). */
	public static Set<String> townSkins() {
		Set<String> skins = new HashSet<>(Set.of(DEFAULT_NPC_SKIN, DEFAULT_GUARD_SKIN));
		for (TownData.Spot spot : TownData.get().spots) {
			skins.add(spot.skin());
		}
		return skins;
	}

	// ---------------------------------------------------------------- making and finding them

	/**
	 * A new NPC or guard standing at {@code at}, its stand post there. Null (nothing made) when its body would not fit
	 * there: placing one never moves or replaces a block.
	 */
	public static @Nullable Townsfolk create(ServerLevel level, Vec3 at, float yaw, String name, String kind, @Nullable UUID creator) {
		String shown = clean(name, MAX_NAME);
		if (shown.isEmpty()) {
			return null;
		}
		Townsfolk npc = JugcraftTown.TOWNSFOLK.create(level, EntitySpawnReason.COMMAND);
		if (npc == null) {
			return null;
		}
		boolean guard = GUARD.equals(kind);
		npc.snapTo(at.x, at.y, at.z, yaw, 0);
		if (!level.noCollision(npc) || !level.isLoaded(BlockPos.containing(at))) {
			return null;
		}
		BlockPos post = BlockPos.containing(at);
		String skin = guard ? DEFAULT_GUARD_SKIN : DEFAULT_NPC_SKIN;
		npc.setUp(new TownData.Spot(-1, guard ? "guard" : "townsfolk", null, shown, skin, BlockPos.ZERO), post);
		Data data = new Data(0, guard ? GUARD : NPC, skin, "", Optional.empty(), List.of(), 0, List.of(), guard ? GUARD_POST_RANGE : 2,
				Optional.ofNullable(creator));
		npc.custom(data);
		if (!level.addFreshEntity(npc)) {
			return null;
		}
		data.id = registry(level.getServer()).enroll(npc);
		return npc;
	}

	/** A loaded custom NPC by its number, or null (not loaded, removed or never made). Never loads a chunk. */
	public static @Nullable Townsfolk find(MinecraftServer server, int id) {
		Registry.Entry entry = registry(server).entries.get(id);
		if (entry == null) {
			return null;
		}
		ServerLevel level = server.getLevel(ResourceKey.create(Registries.DIMENSION, Identifier.parse(entry.dimension())));
		return level != null && level.getEntity(entry.uuid()) instanceof Townsfolk npc && npc.custom() != null ? npc : null;
	}

	/** Removes an NPC: at once if loaded, otherwise when it next loads. False when there is no such NPC. */
	public static boolean remove(MinecraftServer server, int id) {
		Registry registry = registry(server);
		Registry.Entry entry = registry.entries.get(id);
		if (entry == null) {
			return false;
		}
		Townsfolk npc = find(server, id);
		registry.bury(id, entry.uuid());
		if (npc != null) {
			npc.discard();
		}
		return true;
	}

	public static void rename(Townsfolk npc, String name) {
		String shown = clean(name, MAX_NAME);
		if (!shown.isEmpty()) {
			npc.setCustomName(Component.literal(shown));
			index(npc);
		}
	}

	/** Replaces everything it says with this one line (empty: back to the town's lines). */
	public static void setLine(Townsfolk npc, String line) {
		Data data = npc.custom();
		if (data != null) {
			data.lines.clear();
			data.nextLine = 0;
			String said = clean(line, MAX_LINE);
			if (!said.isEmpty()) {
				data.lines.add(said);
			}
		}
	}

	/** Adds a line it says after the others. Why it was refused, or null when added. */
	public static @Nullable String addLine(Townsfolk npc, String line) {
		Data data = npc.custom();
		String said = clean(line, MAX_LINE);
		if (data == null || said.isEmpty()) {
			return "That line is empty.";
		}
		if (data.lines.size() >= MAX_LINES) {
			return "It already has " + MAX_LINES + " lines.";
		}
		data.lines.add(said);
		return null;
	}

	public static boolean removeLine(Townsfolk npc, int index) {
		Data data = npc.custom();
		if (data == null || index < 0 || index >= data.lines.size()) {
			return false;
		}
		data.lines.remove(index);
		data.nextLine = 0;
		return true;
	}

	public static void setWander(Townsfolk npc, int blocks) {
		Data data = npc.custom();
		if (data != null) {
			data.wander = Math.clamp(blocks, 0, MAX_WANDER);
		}
	}

	/** Wear one of the town's skins (and drop any player skin). False for a skin the town does not have. */
	public static boolean setTownSkin(Townsfolk npc, String skin) {
		Data data = npc.custom();
		if (data == null || !townSkins().contains(skin)) {
			return false;
		}
		data.townSkin = skin;
		data.skinName = "";
		data.profile = null;
		data.skinRequest++;   // a lookup still on its way no longer applies
		showSkin(npc);
		return true;
	}

	/** Wear a player's resolved profile (from {@link NpcSkins}): kept with the NPC, drawn by every client. */
	public static void setPlayerSkin(Townsfolk npc, String name, ResolvableProfile profile) {
		Data data = npc.custom();
		if (data == null) {
			return;
		}
		data.skinName = name;
		data.profile = profile;
		showSkin(npc);
	}

	/** Pushes the NPC's skin to its synced data: a town skin, or a player's profile. */
	static void showSkin(Townsfolk npc) {
		Data data = npc.custom();
		if (data == null) {
			return;
		}
		if (data.profile != null) {
			npc.setProfile(data.profile);
			npc.setSkin(PLAYER_SKIN);
		} else {
			npc.setSkin(data.townSkin);
		}
	}

	/**
	 * Moves the stand post (the NPC walks there). Same dimension only; patrol waypoints out of reach of the new post
	 * are dropped. False when the NPC is elsewhere.
	 */
	public static boolean setPost(Townsfolk npc, ServerLevel level, BlockPos post) {
		Data data = npc.custom();
		if (data == null || npc.level() != level) {
			return false;
		}
		npc.post(post);
		data.patrol.removeIf(at -> !at.closerThan(post, WAYPOINT_REACH));
		data.patrolIndex = data.patrol.isEmpty() ? 0 : Math.floorMod(data.patrolIndex, data.patrol.size());
		return true;
	}

	/** Why a waypoint was refused, or null when it was added. */
	public static @Nullable String addWaypoint(Townsfolk npc, ServerLevel level, BlockPos at) {
		Data data = npc.custom();
		if (data == null || npc.level() != level) {
			return "It is in another dimension.";
		}
		if (data.patrol.size() >= MAX_WAYPOINTS) {
			return "It already has " + MAX_WAYPOINTS + " waypoints.";
		}
		if (!at.closerThan(npc.home(), WAYPOINT_REACH)) {
			return "Waypoints must be within " + WAYPOINT_REACH + " blocks of its post.";
		}
		data.patrol.add(at.immutable());
		return null;
	}

	public static boolean removeWaypoint(Townsfolk npc, int index) {
		Data data = npc.custom();
		if (data == null || index < 0 || index >= data.patrol.size()) {
			return false;
		}
		data.patrol.remove(index);
		data.patrolIndex = data.patrol.isEmpty() ? 0 : Math.floorMod(data.patrolIndex, data.patrol.size());
		return true;
	}

	public static void clearPatrol(Townsfolk npc) {
		Data data = npc.custom();
		if (data != null) {
			data.patrol.clear();
			data.patrolIndex = 0;
		}
	}

	// ---------------------------------------------------------------- hooks called by Townsfolk

	/** After loading: show its skin again (the synced data is not saved). */
	static void loaded(Townsfolk npc) {
		showSkin(npc);
	}

	/** Where it belongs now: the patrol waypoint it is walking to, or its stand post. */
	static BlockPos anchor(Townsfolk npc, Data data) {
		return data.patrol.isEmpty() ? npc.home() : data.patrol.get(Math.floorMod(data.patrolIndex, data.patrol.size()));
	}

	/** How far from {@link #anchor} it wanders: close on patrol, near its post as a guard, as set for an NPC. */
	static int range(Data data) {
		return !data.patrol.isEmpty() ? 2 : data.wander;
	}

	/**
	 * Once a second, on the server: leaves if it was removed while unloaded, and keeps the index's record of where it
	 * is up to date. True when it left.
	 */
	static boolean tick(ServerLevel level, Townsfolk npc, Data data) {
		Registry registry = registry(level.getServer());
		if (registry.buried(npc.getUUID())) {
			npc.discard();
			return true;
		}
		if (npc.tickCount % 200 == 0 || data.id == 0) {
			index(npc);
		}
		return false;
	}

	/** A player speaks to it: the operator's lines in turn, or the town's for the season. Operators sneaking see its number. */
	static InteractionResult talk(Townsfolk npc, ServerPlayer player, Data data) {
		if (npc.tickCount - data.lastTalk < 20) {
			return InteractionResult.SUCCESS;
		}
		data.lastTalk = npc.tickCount;
		Component said = !data.lines.isEmpty() ? Component.literal(data.lines.get(Math.floorMod(data.nextLine++, data.lines.size())))
				: Component.translatable("message.jugcraft.townsfolk." + (data.guard() ? "guard" : "townsfolk") + "." + TownDecor.theme()
						+ "." + Math.floorMod(npc.getUUID().hashCode() + npc.tickCount / 200, 3));
		player.sendSystemMessage(Component.translatable("message.jugcraft.townsfolk.says", npc.getDisplayName(), said));
		if (player.isShiftKeyDown() && isOperator(player.createCommandSourceStack())) {
			// An operator sneak-talking selects it for /npc.
			CustomNpcCommand.select(player.level().getServer(), player.getUUID(), data.id);
			player.sendSystemMessage(Component.literal((data.guard() ? "Guard #" : "NPC #") + data.id + " selected: /npc info, /npc skin, /npc say, /npc post, /npc patrol"));
		}
		npc.level().playSound(null, npc.blockPosition(), SoundEvents.VILLAGER_AMBIENT, SoundSource.NEUTRAL, 0.5F, 1.2F);
		return InteractionResult.SUCCESS;
	}

	private static void index(Townsfolk npc) {
		if (npc.level() instanceof ServerLevel level && npc.custom() != null) {
			Data data = npc.custom();
			data.id = registry(level.getServer()).record(data.id, npc);
		}
	}

	// ---------------------------------------------------------------- patrol

	/**
	 * Walks a custom NPC's patrol waypoints in order, waiting a moment at each. A fight (the guard's own melee goal,
	 * higher priority) interrupts it; it resumes after. A waypoint it cannot reach (blocked, or in a chunk not loaded:
	 * nothing is loaded for it) is skipped after {@link #STUCK_TICKS}.
	 */
	public static final class PatrolGoal extends Goal {
		private final Townsfolk npc;
		private int dwell;
		private int stalled;
		private double best = Double.MAX_VALUE;

		public PatrolGoal(Townsfolk npc) {
			this.npc = npc;
			setFlags(EnumSet.of(Goal.Flag.MOVE));
		}

		private @Nullable Data patrolling() {
			Data data = npc.custom();
			return data != null && (!data.patrol.isEmpty() || data.wander == 0) && !npc.isPassenger() && npc.getTarget() == null && !npc.isSleeping() ? data : null;
		}

		@Override
		public boolean canUse() {
			return patrolling() != null;
		}

		@Override
		public boolean canContinueToUse() {
			return patrolling() != null;
		}

		@Override
		public boolean requiresUpdateEveryTick() {
			return true;
		}

		@Override
		public void start() {
			dwell = 0;
			stalled = 0;
			best = Double.MAX_VALUE;
			walk();
		}

		@Override
		public void tick() {
			Data data = patrolling();
			if (data == null) {
				return;
			}
			BlockPos at = anchor(npc, data);
			double distance = npc.distanceToSqr(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
			if (distance < (data.patrol.isEmpty() ? .25 : 2.25)) {
				npc.getNavigation().stop();
				if (!data.patrol.isEmpty() && ++dwell >= DWELL_TICKS) {
					next(data);
				}
				return;
			}
			if (distance < best - 1) {
				best = distance;
				stalled = 0;
			} else if (++stalled >= STUCK_TICKS) {
				next(data);
				return;
			}
			if (npc.getNavigation().isDone() && npc.tickCount % 20 == 0) {
				walk();
			}
		}

		private void next(Data data) {
			data.patrolIndex = data.patrol.isEmpty() ? 0 : Math.floorMod(data.patrolIndex + 1, data.patrol.size());
			dwell = 0;
			stalled = 0;
			best = Double.MAX_VALUE;
			walk();
		}

		private void walk() {
			Data data = npc.custom();
			if (data != null && (!data.patrol.isEmpty() || data.wander == 0)) {
				BlockPos at = anchor(npc, data);
                if (!npc.level().hasChunkAt(at)) return;
				npc.getNavigation().moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0.7);
			}
		}

		@Override
		public void stop() {
			npc.getNavigation().stop();
		}
	}

	// ---------------------------------------------------------------- the index

	public static Registry registry(MinecraftServer server) {
		return server.overworld().getDataStorage().computeIfAbsent(Registry.TYPE);
	}

	/**
	 * Numbers custom NPCs for commands and remembers where each was last seen. The entity itself holds its settings;
	 * this only finds it. Removal tombstones persist so an NPC removed while unloaded cannot reappear on load.
	 */
	public static final class Registry extends SavedData {
		public record Entry(UUID uuid, String kind, String name, String dimension, BlockPos pos) {
			static final Codec<Entry> CODEC = RecordCodecBuilder.create(i -> i.group(
					UUIDUtil.CODEC.fieldOf("uuid").forGetter(Entry::uuid),
					Codec.STRING.fieldOf("kind").forGetter(Entry::kind),
					Codec.STRING.fieldOf("name").forGetter(Entry::name),
					Codec.STRING.fieldOf("dimension").forGetter(Entry::dimension),
					BlockPos.CODEC.fieldOf("pos").forGetter(Entry::pos)
			).apply(i, Entry::new));
		}

		public static final Codec<Registry> CODEC = RecordCodecBuilder.create(i -> i.group(
				Codec.INT.optionalFieldOf("next", 1).forGetter(r -> r.next),
				Codec.unboundedMap(Codec.STRING, Entry.CODEC).optionalFieldOf("npcs", Map.of()).forGetter(r -> {
					Map<String, Entry> out = new LinkedHashMap<>();
					r.entries.forEach((id, entry) -> out.put(Integer.toString(id), entry));
					return out;
				}),
				UUIDUtil.CODEC.listOf().optionalFieldOf("removed", List.of()).forGetter(r -> List.copyOf(r.removed))
		).apply(i, Registry::new));
		static final SavedDataType<Registry> TYPE = new SavedDataType<>(Jugcraft.id("custom_npcs"), Registry::new, CODEC, null);

		private int next;
		final Map<Integer, Entry> entries = new LinkedHashMap<>();
		private final List<UUID> removed = new ArrayList<>();

		public Registry() {
			this(1, Map.of(), List.of());
		}

		private Registry(int next, Map<String, Entry> entries, List<UUID> removed) {
			this.next = Math.max(1, next);
			entries.forEach((id, entry) -> {
				try {
					this.entries.put(Integer.parseInt(id), entry);
				} catch (NumberFormatException ignored) {
					// A damaged key: that NPC re-enrolls with a new number when it next loads.
				}
			});
			this.removed.addAll(removed);
			for (int id : this.entries.keySet()) {
				this.next = Math.max(this.next, id + 1);
			}
		}

		public Map<Integer, Entry> entries() {
			return Map.copyOf(entries);
		}

		boolean buried(UUID uuid) {
			return removed.contains(uuid);
		}

		int enroll(Townsfolk npc) {
			return record(0, npc);
		}

		/** Records where an NPC is; gives it a number if it has none (or another NPC holds its number). */
		int record(int id, Townsfolk npc) {
			Entry held = id > 0 ? entries.get(id) : null;
			if (id <= 0 || held != null && !held.uuid().equals(npc.getUUID())) {
				id = next++;
				setDirty();
			}
			next = Math.max(next, id + 1);   // a number kept from before is never handed out again
			Data data = npc.custom();
			Entry entry = new Entry(npc.getUUID(), data == null ? NPC : data.kind, npc.getName().getString(),
					npc.level().dimension().identifier().toString(), npc.blockPosition());
			if (!entry.equals(entries.get(id))) {
				entries.put(id, entry);
				setDirty();
			}
			return id;
		}

		void forget(int id, UUID uuid) {
			Entry entry = entries.get(id);
			if (entry != null && entry.uuid().equals(uuid)) {
				entries.remove(id);
				setDirty();
			}
		}

		void bury(int id, UUID uuid) {
			forget(id, uuid);
			if (!removed.contains(uuid)) {
				removed.add(uuid);
				// Keep deletion tombstones: evicting one would resurrect an unloaded NPC.
				setDirty();
			}
		}
	}
}
