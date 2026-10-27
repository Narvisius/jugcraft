package io.github.jimbozoomer.jugcraft.test;

import com.mojang.authlib.GameProfile;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.netty.channel.embedded.EmbeddedChannel;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.WeakHashMap;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestInfo;
import net.minecraft.gametest.framework.GameTestListener;
import net.minecraft.gametest.framework.GameTestRunner;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.players.PlayerList;
import net.minecraft.stats.Stats;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Mock players for the Concordance's server game tests, logged out once their test is over. The game's own
 * {@code makeMockServerPlayerInLevel} logs a player in for the rest of the run: each one keeps its chunks loaded and its
 * inventory synced, and by Concordance Chunk C the suite's 1,428 left the game test server out of heap (CI run
 * 40003960812). A player made here leaves the game ({@code PlayerList.remove}, as the town and alchemy tests do by hand)
 * when its test passes or fails: the test framework tells its listeners on the tick after the test is done, never during
 * it. A player its test has already logged out (to stand for one who is away) is left alone.
 * <p>
 * Concordance Chunk D adds the players its tests of absence need. Vanilla's mock is creative for good (its gameMode()
 * always answers creative, GameTestHelper.makeMockServerPlayerInLevel). {@link #survivor} joins an ordinary ServerPlayer
 * as the player list joins one, so survival is survival; {@link #leave} logs a player out as leaving does
 * ({@code PlayerList.remove} saves their data and statistics); {@link #again} brings the same identity back with its saved
 * data and statistics read as a join reads them (PrepareSpawnTask). A test player's connection is never ticked, so its
 * play time grows only by {@link #play}.
 */
public final class TestPlayers {
	private static final String SURVIVOR = "test-survivor";
	private static final @Nullable Field TEST_INFO = testInfoField();
	private static final Map<GameTestHelper, List<ServerPlayer>> MADE = Collections.synchronizedMap(new WeakHashMap<>());

	private TestPlayers() {
	}

	/** A mock player in the test's level, as {@code makeMockServerPlayerInLevel} makes it, that leaves when the test ends. */
	@SuppressWarnings("removal")
	public static ServerPlayer mock(GameTestHelper helper) {
		return track(helper, helper.makeMockServerPlayerInLevel());
	}

	/** A new ordinary survival player in the test's level, logged out when the test ends: neither creative nor with infinite materials. */
	public static ServerPlayer survivor(GameTestHelper helper) {
		ServerPlayer player = join(helper, new GameProfile(UUID.randomUUID(), SURVIVOR));
		player.setGameMode(GameType.SURVIVAL);
		helper.assertTrue(!player.isCreative() && !player.hasInfiniteMaterials(), "A survivor is an ordinary survival player");
		return player;
	}

	/** {@code player} logs out now: the player list saves their data and statistics, then forgets them. */
	public static void leave(ServerPlayer player) {
		PlayerList list = player.level().getServer().getPlayerList();
		if (list.getPlayer(player.getUUID()) != player) {
			throw new IllegalStateException(player.getUUID() + " is not in the game");
		}
		list.remove(player);
	}

	/** The person {@code id} joins again after {@link #leave}: the same identity, their saved data and statistics read back. */
	public static ServerPlayer again(GameTestHelper helper, UUID id) {
		if (helper.getLevel().getServer().getPlayerList().getPlayer(id) != null) {
			throw new IllegalStateException(id + " has not left");
		}
		return join(helper, new GameProfile(id, SURVIVOR));
	}

	/** {@code player}'s own play time in ticks (the game's statistic). */
	public static long played(ServerPlayer player) {
		return player.getStats().getValue(Stats.CUSTOM.get(Stats.PLAY_TIME));
	}

	/** {@code player} plays {@code ticks} more ticks, as the game counts each tick they are in it. */
	public static void play(ServerPlayer player, int ticks) {
		player.awardStat(Stats.PLAY_TIME, ticks);
	}

	/** Joins {@code profile} as a join does: constructed, its saved data read (or stood in the test), then placed in the game. */
	private static ServerPlayer join(GameTestHelper helper, GameProfile profile) {
		ServerLevel level = helper.getLevel();
		MinecraftServer server = level.getServer();
		CommonListenerCookie cookie = CommonListenerCookie.createInitial(profile, false);
		ServerPlayer player = new ServerPlayer(server, level, profile, cookie.clientInformation());
		Optional<CompoundTag> saved = server.getPlayerList().loadPlayerData(player.nameAndId());
		if (saved.isPresent()) {
			player.load(TagValueInput.create(ProblemReporter.DISCARDING, server.registryAccess(), saved.get()));
		} else {
			Vec3 at = helper.absoluteVec(new Vec3(0.5, 1.0, 0.5));
			player.snapTo(at.x, at.y, at.z, 0.0F, 0.0F);
		}
		Connection connection = new Connection(PacketFlow.SERVERBOUND);
		new EmbeddedChannel(connection);
		server.getPlayerList().placeNewPlayer(connection, player, cookie);
		return track(helper, player);
	}

	/** Logs {@code player} out when the test ends. */
	private static ServerPlayer track(GameTestHelper helper, ServerPlayer player) {
		List<ServerPlayer> made = MADE.get(helper);
		if (made == null) {
			made = new ArrayList<>();
			MADE.put(helper, made);
			GameTestInfo info = info(helper);
			if (info != null) {
				List<ServerPlayer> players = made;
				info.addListener(new GameTestListener() {
					@Override
					public void testStructureLoaded(GameTestInfo testInfo) {
					}

					@Override
					public void testPassed(GameTestInfo testInfo, GameTestRunner runner) {
						logOut(players);
					}

					@Override
					public void testFailed(GameTestInfo testInfo, GameTestRunner runner) {
						logOut(players);
					}

					@Override
					public void testAddedForRerun(GameTestInfo testInfo, GameTestInfo rerun, GameTestRunner runner) {
					}
				});
			}
		}
		made.add(player);
		return player;
	}

	private static void logOut(List<ServerPlayer> players) {
		for (ServerPlayer player : players) {
			PlayerList list = player.level().getServer().getPlayerList();
			if (list.getPlayer(player.getUUID()) == player) {
				list.remove(player);
			}
		}
		players.clear();
	}

	private static @Nullable GameTestInfo info(GameTestHelper helper) {
		if (TEST_INFO == null) {
			return null;
		}
		try {
			return (GameTestInfo) TEST_INFO.get(helper);
		} catch (IllegalAccessException e) {
			return null;
		}
	}

	/** GameTestHelper keeps its test private; without it a player made here simply stays, as before. */
	private static @Nullable Field testInfoField() {
		try {
			Field field = GameTestHelper.class.getDeclaredField("testInfo");
			field.setAccessible(true);
			return field;
		} catch (ReflectiveOperationException | RuntimeException e) {
			Jugcraft.LOGGER.warn("[test players] GameTestHelper.testInfo not found; mock players will stay logged in", e);
			return null;
		}
	}
}
