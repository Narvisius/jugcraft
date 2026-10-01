package io.github.jimbozoomer.jugcraft.party;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * All party rules, with no Minecraft types, so the logic can be tested on its own.
 *
 * <p>Players are identified by UUID. A party has a leader and members in join order (the leader is
 * always a member). Every lookup is a map access; nothing scans. Callers pass the current time in
 * milliseconds so invites and rate limits are deterministic in tests.
 *
 * <p>Not thread-safe: use it from the server thread only.
 */
public final class PartyManager {
	public static final int DEFAULT_MAX_SIZE = 8;
	public static final long DEFAULT_INVITE_TTL_MILLIS = 5 * 60 * 1000L;
	public static final int DEFAULT_INVITES_PER_MINUTE = 10;
	private static final long MINUTE_MILLIS = 60 * 1000L;

	/** Outcome of a party action; every refusal has its own value so commands can explain it. */
	public enum Result {
		OK,
		DISABLED,
		SELF,
		ALREADY_IN_PARTY,
		NOT_IN_PARTY,
		NOT_LEADER,
		NOT_MEMBER,
		TARGET_IN_PARTY,
		PARTY_FULL,
		ALREADY_INVITED,
		NO_INVITE,
		RATE_LIMITED
	}

	/** Called once after every change, with every player whose party membership or role changed. */
	@FunctionalInterface
	public interface Listener {
		void onPartyChange(Set<UUID> affectedPlayers);
	}

	/** A party. Members are kept in join order; the leader is always one of them. */
	public static final class Party {
		private final UUID id;
		private UUID leader;
		private final LinkedHashSet<UUID> members = new LinkedHashSet<>();

		Party(UUID id, UUID leader) {
			this.id = id;
			this.leader = leader;
			members.add(leader);
		}

		public UUID id() {
			return id;
		}

		public UUID leader() {
			return leader;
		}

		/** Members in join order (a copy). */
		public List<UUID> members() {
			return List.copyOf(members);
		}

		public int size() {
			return members.size();
		}
	}

	private final int maxSize;
	private final long inviteTtlMillis;
	private final int invitesPerMinute;
	private boolean enabled = true;
	private boolean dirty;

	private final Map<UUID, Party> parties = new LinkedHashMap<>();
	private final Map<UUID, Party> partyByMember = new HashMap<>();
	/** Invited player -> (party id -> expiry time), most recent invite last. */
	private final Map<UUID, LinkedHashMap<UUID, Long>> invites = new HashMap<>();
	/** Inviter -> times of their recent invites, for the per-minute limit. */
	private final Map<UUID, Deque<Long>> inviteTimes = new HashMap<>();
	/** Last known player names, so offline members can be listed and kicked by name. */
	private final Map<UUID, String> names = new HashMap<>();
	private final List<Listener> listeners = new CopyOnWriteArrayList<>();

	public PartyManager() {
		this(DEFAULT_MAX_SIZE, DEFAULT_INVITE_TTL_MILLIS, DEFAULT_INVITES_PER_MINUTE);
	}

	public PartyManager(int maxSize, long inviteTtlMillis, int invitesPerMinute) {
		if (maxSize < 2) {
			throw new IllegalArgumentException("A party needs room for at least 2 players");
		}
		this.maxSize = maxSize;
		this.inviteTtlMillis = inviteTtlMillis;
		this.invitesPerMinute = invitesPerMinute;
	}

	// ---------------------------------------------------------------- settings

	public int maxSize() {
		return maxSize;
	}

	/** When disabled, every player counts as a party of one and actions return DISABLED. Data is kept. */
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void addListener(Listener listener) {
		listeners.add(listener);
	}

	public void removeListener(Listener listener) {
		listeners.remove(listener);
	}

	/** True when something changed since the last {@link #clearDirty()}; used to decide when to save. */
	public boolean isDirty() {
		return dirty;
	}

	public void clearDirty() {
		dirty = false;
	}

	// ---------------------------------------------------------------- names

	public void rememberName(UUID player, String name) {
		if (name == null || name.isBlank()) {
			return;
		}
		String clean = PartyStore.cleanName(name);
		if (!clean.equals(names.get(player))) {
			names.put(player, clean);
			if (partyByMember.containsKey(player)) {
				dirty = true;
			}
		}
	}

	public Optional<String> nameOf(UUID player) {
		return Optional.ofNullable(names.get(player));
	}

	/** Finds a member of {@code player}'s party by last known name (case-insensitive). */
	public Optional<UUID> findMemberByName(UUID player, String name) {
		Party party = partyByMember.get(player);
		if (party == null) {
			return Optional.empty();
		}
		for (UUID member : party.members) {
			String known = names.get(member);
			if (known != null && known.equalsIgnoreCase(name)) {
				return Optional.of(member);
			}
		}
		return Optional.empty();
	}

	// ---------------------------------------------------------------- queries

	public Optional<Party> partyOf(UUID player) {
		if (!enabled) {
			return Optional.empty();
		}
		return Optional.ofNullable(partyByMember.get(player));
	}

	/** A player is always "in the same party" as themselves; otherwise both must share a party. */
	public boolean sameParty(UUID a, UUID b) {
		if (a.equals(b)) {
			return true;
		}
		if (!enabled) {
			return false;
		}
		Party party = partyByMember.get(a);
		return party != null && party == partyByMember.get(b);
	}

	public boolean isLeader(UUID player) {
		if (!enabled) {
			return false;
		}
		Party party = partyByMember.get(player);
		return party != null && party.leader.equals(player);
	}

	/**
	 * The single shared rule for automation. A system always serves its own owner's jobs. It serves
	 * another player's job only when the system and the job are both in PARTY mode and the two owners
	 * are in the same party.
	 */
	public boolean mayServe(UUID systemOwner, UseMode systemMode, UUID jobOwner, UseMode jobMode) {
		if (systemOwner.equals(jobOwner)) {
			return true;
		}
		return systemMode == UseMode.PARTY && jobMode == UseMode.PARTY && sameParty(systemOwner, jobOwner);
	}

	public Collection<Party> parties() {
		return Collections.unmodifiableCollection(parties.values());
	}

	/** Party ids with an unexpired invite for {@code player}, oldest first. */
	public List<UUID> pendingInvites(UUID player, long now) {
		expireInvites(player, now);
		LinkedHashMap<UUID, Long> pending = invites.get(player);
		return pending == null ? List.of() : List.copyOf(pending.keySet());
	}

	// ---------------------------------------------------------------- actions

	public Result create(UUID player) {
		if (!enabled) {
			return Result.DISABLED;
		}
		if (partyByMember.containsKey(player)) {
			return Result.ALREADY_IN_PARTY;
		}
		Party party = new Party(UUID.randomUUID(), player);
		parties.put(party.id, party);
		partyByMember.put(player, party);
		changed(Set.of(player));
		return Result.OK;
	}

	/**
	 * The leader invites a player. A player without a party gets one created automatically, so
	 * "/party invite Alex" just works. Invites expire and are limited per minute.
	 */
	public Result invite(UUID inviter, UUID target, long now) {
		if (!enabled) {
			return Result.DISABLED;
		}
		if (inviter.equals(target)) {
			return Result.SELF;
		}
		if (partyByMember.containsKey(target)) {
			return Result.TARGET_IN_PARTY;
		}
		Party party = partyByMember.get(inviter);
		if (party != null && !party.leader.equals(inviter)) {
			return Result.NOT_LEADER;
		}
		if (party != null && party.size() >= maxSize) {
			return Result.PARTY_FULL;
		}
		Deque<Long> recent = inviteTimes.computeIfAbsent(inviter, key -> new ArrayDeque<>());
		while (!recent.isEmpty() && now - recent.peekFirst() >= MINUTE_MILLIS) {
			recent.pollFirst();
		}
		if (recent.size() >= invitesPerMinute) {
			return Result.RATE_LIMITED;
		}
		if (party != null) {
			expireInvites(target, now);
			LinkedHashMap<UUID, Long> pending = invites.get(target);
			if (pending != null && pending.containsKey(party.id)) {
				return Result.ALREADY_INVITED;
			}
		}
		if (party == null) {
			create(inviter);
			party = partyByMember.get(inviter);
		}
		recent.addLast(now);
		invites.computeIfAbsent(target, key -> new LinkedHashMap<>()).put(party.id, now + inviteTtlMillis);
		return Result.OK;
	}

	/** Accepts the most recent unexpired invite. */
	public Result accept(UUID player, long now) {
		List<UUID> pending = pendingInvites(player, now);
		if (pending.isEmpty()) {
			return enabled ? Result.NO_INVITE : Result.DISABLED;
		}
		return accept(player, pending.get(pending.size() - 1), now);
	}

	public Result accept(UUID player, UUID partyId, long now) {
		if (!enabled) {
			return Result.DISABLED;
		}
		if (partyByMember.containsKey(player)) {
			return Result.ALREADY_IN_PARTY;
		}
		expireInvites(player, now);
		LinkedHashMap<UUID, Long> pending = invites.get(player);
		Party party = parties.get(partyId);
		if (pending == null || !pending.containsKey(partyId) || party == null) {
			if (pending != null) {
				pending.remove(partyId);
			}
			return Result.NO_INVITE;
		}
		if (party.size() >= maxSize) {
			return Result.PARTY_FULL;
		}
		party.members.add(player);
		partyByMember.put(player, party);
		invites.remove(player);
		changed(new HashSet<>(party.members));
		return Result.OK;
	}

	/** Declines the most recent unexpired invite. */
	public Result decline(UUID player, long now) {
		if (!enabled) {
			return Result.DISABLED;
		}
		List<UUID> pending = pendingInvites(player, now);
		if (pending.isEmpty()) {
			return Result.NO_INVITE;
		}
		invites.get(player).remove(pending.get(pending.size() - 1));
		return Result.OK;
	}

	/**
	 * Leaves the party. A leaving leader hands over to the longest-standing member; the last member
	 * leaving disbands the party.
	 */
	public Result leave(UUID player) {
		if (!enabled) {
			return Result.DISABLED;
		}
		Party party = partyByMember.get(player);
		if (party == null) {
			return Result.NOT_IN_PARTY;
		}
		Set<UUID> affected = new HashSet<>(party.members);
		removeMember(party, player);
		changed(affected);
		return Result.OK;
	}

	public Result kick(UUID leader, UUID target) {
		if (!enabled) {
			return Result.DISABLED;
		}
		Party party = partyByMember.get(leader);
		if (party == null) {
			return Result.NOT_IN_PARTY;
		}
		if (!party.leader.equals(leader)) {
			return Result.NOT_LEADER;
		}
		if (leader.equals(target)) {
			return Result.SELF;
		}
		if (!party.members.contains(target)) {
			return Result.NOT_MEMBER;
		}
		Set<UUID> affected = new HashSet<>(party.members);
		removeMember(party, target);
		changed(affected);
		return Result.OK;
	}

	public Result transferLeader(UUID leader, UUID target) {
		if (!enabled) {
			return Result.DISABLED;
		}
		Party party = partyByMember.get(leader);
		if (party == null) {
			return Result.NOT_IN_PARTY;
		}
		if (!party.leader.equals(leader)) {
			return Result.NOT_LEADER;
		}
		if (leader.equals(target)) {
			return Result.SELF;
		}
		if (!party.members.contains(target)) {
			return Result.NOT_MEMBER;
		}
		party.leader = target;
		changed(new HashSet<>(party.members));
		return Result.OK;
	}

	public Result disband(UUID leader) {
		if (!enabled) {
			return Result.DISABLED;
		}
		Party party = partyByMember.get(leader);
		if (party == null) {
			return Result.NOT_IN_PARTY;
		}
		if (!party.leader.equals(leader)) {
			return Result.NOT_LEADER;
		}
		Set<UUID> affected = new HashSet<>(party.members);
		for (UUID member : affected) {
			partyByMember.remove(member);
		}
		parties.remove(party.id);
		dropInvitesFor(party.id);
		changed(affected);
		return Result.OK;
	}

	/** Removes every expired invite. Call occasionally (for example once a minute). */
	public void expireInvites(long now) {
		for (Iterator<Map.Entry<UUID, LinkedHashMap<UUID, Long>>> it = invites.entrySet().iterator(); it.hasNext(); ) {
			LinkedHashMap<UUID, Long> pending = it.next().getValue();
			pending.values().removeIf(expiry -> expiry <= now);
			if (pending.isEmpty()) {
				it.remove();
			}
		}
		inviteTimes.values().removeIf(times -> times.isEmpty() || now - times.peekLast() >= MINUTE_MILLIS);
	}

	// ---------------------------------------------------------------- loading (used by PartyStore)

	/** Replaces all parties with loaded data. Invites are not saved: they expire within minutes anyway. */
	void load(List<Party> loaded, Map<UUID, String> loadedNames) {
		parties.clear();
		partyByMember.clear();
		invites.clear();
		inviteTimes.clear();
		names.clear();
		names.putAll(loadedNames);
		for (Party party : loaded) {
			for (UUID member : List.copyOf(party.members)) {
				if (partyByMember.containsKey(member)) {
					// A player can only be in one party; keep the first and drop duplicates.
					party.members.remove(member);
				}
			}
			if (party.members.isEmpty()) {
				continue;
			}
			if (!party.members.contains(party.leader)) {
				party.leader = party.members.iterator().next();
			}
			parties.put(party.id, party);
			for (UUID member : party.members) {
				partyByMember.put(member, party);
			}
		}
		dirty = false;
	}

	static Party newParty(UUID id, UUID leader, List<UUID> members) {
		Party party = new Party(id, leader);
		party.members.clear();
		party.members.addAll(members);
		return party;
	}

	Map<UUID, String> namesSnapshot() {
		Map<UUID, String> snapshot = new HashMap<>();
		for (Party party : parties.values()) {
			for (UUID member : party.members) {
				String name = names.get(member);
				if (name != null) {
					snapshot.put(member, name);
				}
			}
		}
		return snapshot;
	}

	// ---------------------------------------------------------------- internals

	private void removeMember(Party party, UUID player) {
		party.members.remove(player);
		partyByMember.remove(player);
		if (party.members.isEmpty()) {
			parties.remove(party.id);
			dropInvitesFor(party.id);
		} else if (party.leader.equals(player)) {
			party.leader = party.members.iterator().next();
		}
	}

	private void dropInvitesFor(UUID partyId) {
		invites.values().forEach(pending -> pending.remove(partyId));
		invites.values().removeIf(Map::isEmpty);
	}

	private void expireInvites(UUID player, long now) {
		LinkedHashMap<UUID, Long> pending = invites.get(player);
		if (pending == null) {
			return;
		}
		pending.entrySet().removeIf(entry -> entry.getValue() <= now || !parties.containsKey(entry.getKey()));
		if (pending.isEmpty()) {
			invites.remove(player);
		}
	}

	private void changed(Set<UUID> affected) {
		dirty = true;
		Set<UUID> view = Collections.unmodifiableSet(new HashSet<>(affected));
		for (Listener listener : listeners) {
			listener.onPartyChange(view);
		}
	}

	/** For tests and debugging: the number of stored invites across all players. */
	int inviteCount() {
		int count = 0;
		for (LinkedHashMap<UUID, Long> pending : invites.values()) {
			count += pending.size();
		}
		return count;
	}

	/** Players currently in any party (a copy). */
	public List<UUID> allMembers() {
		return new ArrayList<>(partyByMember.keySet());
	}
}
