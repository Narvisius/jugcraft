package io.github.jimbozoomer.jugcraft.raiders;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.config.JugcraftConfig;
import io.github.jimbozoomer.jugcraft.town.Town;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Raids (batch 57, docs/features/raiders.md), in the Overworld only. Every {@value #RAID_CHECK_TICKS} ticks, if raids are
 * on ({@link #enabled}), mobs spawn and it is not peaceful, and no raid is under way, a player who has played for the
 * grace days ({@code raiders.grace_days}) may be raided, at most once every {@code raiders.interval_days} in the world,
 * {@link #RAID_CHANCE} of the checks after that. The objective is the player's base (where they stand), or the town if
 * they are within {@value #TOWN_REACH} blocks of it.
 *
 * <p>A party ({@link #PARTY}, by the world's raid level, 1 to {@value #MAX_LEVEL}) gathers {@value #SPAWN_MIN} to
 * {@value #SPAWN_MAX} blocks away, on open ground outside the town, and marches on the objective; everyone near is told
 * which way it comes from. A bar shows how much of it is left to everyone within {@value #ABANDON_RANGE} blocks of the
 * objective. When every raider has fallen the raid is won, and the next is a level stronger. A raid that lasts
 * {@value #RAID_TIMEOUT} ticks, or has had nobody within {@value #ABANDON_RANGE} blocks of its objective for
 * {@value #ABANDON_TICKS}, withdraws: its raiders leave in a puff of smoke, then or as soon as they are next loaded.
 * Raids, the raid level and when the last began are saved with the world.
 */
public final class RaiderRaids {
	public static final int RAID_CHECK_TICKS = 1200;
	public static final float RAID_CHANCE = 0.2F;
	public static final int SPAWN_MIN = 48;
	public static final int SPAWN_MAX = 64;
	public static final int RAID_TIMEOUT = 12000;
	public static final int ABANDON_TICKS = 2400;
	public static final int ABANDON_RANGE = 160;
	public static final int MAX_LEVEL = 5;
	/** How near the town (blocks from its origin) a player must be for a raid to make for the town instead. */
	public static final int TOWN_REACH = 128;
	/** Ticks between a raid's own updates (its bar, whether it is won or abandoned). */
	private static final int UPDATE_TICKS = 10;
	/**
	 * Who comes at each raid level (1 to {@value #MAX_LEVEL}): grunts, grenadiers, officers, blimps, walkers and (slice 10F
	 * of the guns) gunners, who march in place of some of the grunts. Keep in sync with party() in tools/raiders.py;
	 * tools/check_mod_data.py checks it.
	 */
	public static final int[][] PARTY = {{2, 1, 1, 0, 0, 1}, {3, 1, 1, 1, 0, 1}, {3, 2, 1, 1, 1, 2}, {4, 2, 1, 1, 1, 2}, {4, 3, 1, 2, 1, 3}};

	private RaiderRaids() {
	}

	static void register() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			ServerLevel level = server.overworld();
			State state = State.get(level);
			if (server.getTickCount() % UPDATE_TICKS == 0 && !state.raids.isEmpty()) {
				state.update(level);
			}
			if (server.getTickCount() % RAID_CHECK_TICKS == 0) {
				maybeRaid(level, state);
			}
		});
	}

	/** Whether raids come at all: the feature is on and {@code raiders.raids} is not {@code off}. */
	public static boolean enabled() {
		return JugcraftConfig.isFeatureEnabled(JugcraftRaiders.FEATURE) && !"off".equalsIgnoreCase(JugcraftConfig.textOption("raiders.raids"));
	}

	private static long days(String option, int fallback) {
		try {
			return Math.max(0, Long.parseLong(JugcraftConfig.textOption(option).trim())) * 24000L;
		} catch (NumberFormatException e) {
			return fallback * 24000L;
		}
	}

	private static boolean on(String option) {
		return !"off".equalsIgnoreCase(JugcraftConfig.textOption(option).trim());
	}

	private static void maybeRaid(ServerLevel level, State state) {
		if (!enabled() || level.getDifficulty() == Difficulty.PEACEFUL || !level.getGameRules().get(GameRules.SPAWN_MOBS)
				|| !state.raids.isEmpty() || level.getGameTime() - state.lastRaid < days("raiders.interval_days", 3)) {
			return;
		}
		List<ServerPlayer> players = new ArrayList<>(level.players());
		Collections.shuffle(players);
		long grace = days("raiders.grace_days", 3);
		RandomSource random = level.getRandom();
		for (ServerPlayer player : players) {
			if (player.isSpectator() || player.isCreative() || player.getStats().getValue(Stats.CUSTOM.get(Stats.PLAY_TIME)) < grace) {
				continue;
			}
			if (random.nextFloat() < RAID_CHANCE) {
				start(level, objectiveFor(level, player), state.level, random);
			}
			return;
		}
	}

	/** Where a raid on {@code player} makes for: the town if they are near it, otherwise where they stand. */
	public static BlockPos objectiveFor(ServerLevel level, ServerPlayer player) {
		BlockPos town = Town.origin(level);
		if (town != null && player.blockPosition().distSqr(town) <= (double) TOWN_REACH * TOWN_REACH) {
			return town;
		}
		return player.blockPosition();
	}

	/**
	 * Starts a raid of {@code raidLevel} on {@code objective}: gathers its party on open ground nearby and sends it
	 * marching. Returns the raid, or null if there was nowhere for it to gather.
	 */
	public static @Nullable Raid start(ServerLevel level, BlockPos objective, int raidLevel, RandomSource random) {
		BlockPos gather = gatheringPoint(level, objective, random);
		return gather == null ? null : start(level, objective, gather, raidLevel, random);
	}

	/** Starts a raid of {@code raidLevel} on {@code objective}, its party gathering at {@code gather}. */
	public static Raid start(ServerLevel level, BlockPos objective, BlockPos gather, int raidLevel, RandomSource random) {
		raidLevel = Mth.clamp(raidLevel, 1, MAX_LEVEL);
		State state = State.get(level);
		Raid raid = new Raid(UUID.randomUUID(), objective.immutable(), raidLevel, level.getGameTime(), 0, 0, List.of());
		int[] party = PARTY[raidLevel - 1];
		List<EntityType<? extends Mob>> kinds = List.of(JugcraftRaiders.GRUNT, JugcraftRaiders.GRENADIER, JugcraftRaiders.OFFICER,
				JugcraftRaiders.BLIMP, JugcraftRaiders.WALKER, JugcraftRaiders.GUNNER);
		for (int kind = 0; kind < kinds.size(); kind++) {
			if (kind == 3 && !on("raiders.blimps") || kind == 4 && !on("raiders.walkers")) {
				continue;
			}
			for (int n = 0; n < party[kind]; n++) {
				Mob raider = JugcraftRaiders.armed(kinds.get(kind)).create(level, EntitySpawnReason.EVENT);
				if (raider == null) {
					continue;
				}
				double x = gather.getX() + 0.5 + (random.nextDouble() - 0.5) * 6;
				double z = gather.getZ() + 0.5 + (random.nextDouble() - 0.5) * 6;
				double y = kind == 3 ? gather.getY() + 20 : level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int) Math.floor(x), (int) Math.floor(z));
				raider.snapTo(x, y, z, random.nextFloat() * 360.0F, 0.0F);
				((Raider) raider).joinRaid(raid.id, raid.objective);
				if (level.addFreshEntity(raider)) {
					raid.members.add(raider.getUUID());
				}
			}
		}
		raid.total = raid.members.size();
		state.raids.add(raid);
		state.lastRaid = level.getGameTime();
		state.setDirty();
		Vec3 from = Vec3.atCenterOf(gather).subtract(Vec3.atCenterOf(objective));
		String direction = Math.abs(from.x) > Math.abs(from.z) ? from.x > 0 ? "east" : "west" : from.z > 0 ? "south" : "north";
		for (ServerPlayer player : level.getPlayers(p -> p.blockPosition().distSqr(objective) <= (double) ABANDON_RANGE * ABANDON_RANGE)) {
			player.sendSystemMessage(Component.translatable("message.jugcraft.raid.coming",
					Component.translatable("message.jugcraft.raid.dir." + direction)));
			level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.RAID_HORN.value(), SoundSource.HOSTILE, 64.0F, 0.8F);
		}
		return raid;
	}

	/** Open dry ground {@value #SPAWN_MIN} to {@value #SPAWN_MAX} blocks from {@code objective}, in a loaded chunk and outside the town. */
	private static @Nullable BlockPos gatheringPoint(ServerLevel level, BlockPos objective, RandomSource random) {
		for (int attempt = 0; attempt < 16; attempt++) {
			double angle = random.nextDouble() * Math.PI * 2.0;
			int distance = SPAWN_MIN + random.nextInt(SPAWN_MAX - SPAWN_MIN + 1);
			int x = objective.getX() + (int) Math.round(Math.cos(angle) * distance);
			int z = objective.getZ() + (int) Math.round(Math.sin(angle) * distance);
			if (!level.isLoaded(new BlockPos(x, level.getSeaLevel(), z))) {
				continue;
			}
			BlockPos spot = new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z);
			if (!level.getFluidState(spot.below()).isEmpty() || !level.getBlockState(spot.below()).isSolid() || Town.isProtected(level, spot)) {
				continue;
			}
			return spot;
		}
		return null;
	}

	/** Whether {@code raid} is still under way. */
	public static boolean active(ServerLevel level, UUID raid) {
		for (Raid r : State.get(level).raids) {
			if (r.id.equals(raid)) {
				return true;
			}
		}
		return false;
	}

	/** The raids under way. */
	public static List<Raid> raids(ServerLevel level) {
		return List.copyOf(State.get(level).raids);
	}

	/** Makes a raid withdraw now, as one left too long does: its loaded raiders leave, and the rest when next loaded. */
	public static void withdraw(ServerLevel level, Raid raid) {
		State state = State.get(level);
		if (state.raids.contains(raid)) {
			state.end(level, raid, false);
		}
	}

	/** The world's raid level: how strong the next raid will be. */
	public static int raidLevel(ServerLevel level) {
		return State.get(level).level;
	}

	/** A raider has fallen: it no longer counts toward its raid. */
	static void fallen(ServerLevel level, Raider raider) {
		if (raider.raid() == null || level.dimension() != net.minecraft.world.level.Level.OVERWORLD) {
			return;
		}
		State state = State.get(level);
		for (Raid raid : state.raids) {
			if (raid.id.equals(raider.raid()) && raid.members.remove(((Entity) raider).getUUID())) {
				state.setDirty();
			}
		}
	}

	/** A raid under way: its objective, level, start, how long it has been left alone, its size and who is left. */
	public static final class Raid {
		static final Codec<Raid> CODEC = RecordCodecBuilder.create(i -> i.group(
				UUIDUtil.STRING_CODEC.fieldOf("id").forGetter(r -> r.id),
				BlockPos.CODEC.fieldOf("objective").forGetter(r -> r.objective),
				Codec.INT.fieldOf("level").forGetter(r -> r.level),
				Codec.LONG.fieldOf("started").forGetter(r -> r.started),
				Codec.INT.optionalFieldOf("lonely", 0).forGetter(r -> r.lonely),
				Codec.INT.optionalFieldOf("total", 0).forGetter(r -> r.total),
				UUIDUtil.STRING_CODEC.listOf().optionalFieldOf("members", List.of()).forGetter(r -> List.copyOf(r.members)))
				.apply(i, Raid::new));

		final UUID id;
		final BlockPos objective;
		final int level;
		final long started;
		int lonely;
		int total;
		final Set<UUID> members = new HashSet<>();
		private @Nullable ServerBossEvent bar;

		Raid(UUID id, BlockPos objective, int level, long started, int lonely, int total, List<UUID> members) {
			this.id = id;
			this.objective = objective;
			this.level = level;
			this.started = started;
			this.lonely = lonely;
			this.total = total;
			this.members.addAll(members);
		}

		public UUID id() {
			return id;
		}

		public BlockPos objective() {
			return objective;
		}

		public int level() {
			return level;
		}

		/** The raiders still standing (some may be in unloaded chunks). */
		public Set<UUID> members() {
			return Set.copyOf(members);
		}

		/** How many raiders the raid began with. */
		public int total() {
			return total;
		}

		ServerBossEvent bar() {
			if (bar == null) {
				bar = new ServerBossEvent(id, Component.translatable("event.jugcraft.raid", level), BossEvent.BossBarColor.RED,
						BossEvent.BossBarOverlay.NOTCHED_10);
			}
			return bar;
		}
	}

	/** The world's raids, raid level and when the last raid began (in the Overworld's data). */
	public static final class State extends SavedData {
		static final Codec<State> CODEC = RecordCodecBuilder.create(i -> i.group(
				Codec.INT.optionalFieldOf("level", 1).forGetter(s -> s.level),
				Codec.LONG.optionalFieldOf("last_raid", 0L).forGetter(s -> s.lastRaid),
				Raid.CODEC.listOf().optionalFieldOf("raids", List.of()).forGetter(s -> List.copyOf(s.raids)))
				.apply(i, State::new));
		static final SavedDataType<State> TYPE = new SavedDataType<>(Jugcraft.id("raids"), State::new, CODEC, null);

		int level;
		long lastRaid;
		final List<Raid> raids = new ArrayList<>();

		State() {
			this(1, 0L, List.of());
		}

		State(int level, long lastRaid, List<Raid> raids) {
			this.level = Mth.clamp(level, 1, MAX_LEVEL);
			this.lastRaid = lastRaid;
			this.raids.addAll(raids);
		}

		static State get(ServerLevel level) {
			return level.getServer().overworld().getDataStorage().computeIfAbsent(TYPE);
		}

		/** Updates every raid: its bar and who sees it, and whether it is won or withdraws. */
		void update(ServerLevel world) {
			for (Raid raid : List.copyOf(raids)) {
				ServerBossEvent bar = raid.bar();
				double range = (double) ABANDON_RANGE * ABANDON_RANGE;
				boolean anyone = false;
				for (ServerPlayer player : world.players()) {
					boolean near = !player.isSpectator() && player.blockPosition().distSqr(raid.objective) <= range;
					anyone |= near;
					if (near) {
						bar.addPlayer(player);
					} else {
						bar.removePlayer(player);
					}
				}
				bar.setProgress(raid.total == 0 ? 0.0F : Mth.clamp(raid.members.size() / (float) raid.total, 0.0F, 1.0F));
				raid.lonely = anyone ? 0 : raid.lonely + UPDATE_TICKS;
				if (raid.members.isEmpty()) {
					end(world, raid, true);
				} else if (world.getGameTime() - raid.started >= RAID_TIMEOUT || raid.lonely >= ABANDON_TICKS) {
					end(world, raid, false);
				}
			}
		}

		/** Ends a raid: beaten (the next raid is stronger) or withdrawn (its raiders leave). */
		void end(ServerLevel world, Raid raid, boolean won) {
			raids.remove(raid);
			if (won) {
				level = Math.min(MAX_LEVEL, level + 1);
			} else {
				for (UUID id : raid.members) {
					if (world.getEntity(id) instanceof Mob mob) {
						RaidMember.withdraw(world, mob);
					}
				}
			}
			setDirty();
			ServerBossEvent bar = raid.bar();
			for (ServerPlayer player : List.copyOf(bar.getPlayers())) {
				player.sendSystemMessage(Component.translatable(won ? "message.jugcraft.raid.won" : "message.jugcraft.raid.withdrawn"));
				if (won) {
					// Everyone who saw it through earns Beat Them Back.
					var advancement = world.getServer().getAdvancements().get(Jugcraft.id("raid_beaten"));
					if (advancement != null) {
						player.getAdvancements().award(advancement, "done");
					}
					world.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE,
							SoundSource.PLAYERS, 1.0F, 1.0F);
				}
			}
			bar.removeAllPlayers();
		}
	}
}
