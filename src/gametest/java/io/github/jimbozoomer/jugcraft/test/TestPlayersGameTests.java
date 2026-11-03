package io.github.jimbozoomer.jugcraft.test;

import com.mojang.authlib.GameProfile;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestInfo;
import net.minecraft.gametest.framework.RetryOptions;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Rotation;

/** Real mock-player joins with isolated completion listeners; no dependence on test order or a later batch. */
@SuppressWarnings("removal")
public class TestPlayersGameTests {
	@GameTest(maxTicks = 30)
	public void rawHelperPlayersStayUntilTheirTestCompletes(GameTestHelper helper) {
		Scope scope = scope(helper);
		ServerPlayer player = scope.helper().makeMockServerPlayerInLevel();
		ServerPlayer other = helper.makeMockServerPlayerInLevel();
		helper.assertTrue(online(player) && online(other), "Both test scopes have online players");
		helper.runAfterDelay(3, () -> {
			try {
				helper.assertTrue(online(player), "The raw helper's player survives while its test is active");
				scope.complete(true);
				helper.assertTrue(!online(player), "A passed test logs out its raw-helper player");
				helper.assertTrue(helper.getLevel().getPlayerByUUID(player.getUUID()) == null,
						"Completed fixture no longer remains in the world player list");
				helper.assertTrue(online(other), "Completing one test does not log out another test's player");
				helper.succeed();
			} finally {
				scope.complete(false);
			}
		});
	}

	@GameTest(maxTicks = 20)
	public void wrapperRegistrationAndFailureCleanupAreIdempotent(GameTestHelper helper) {
		Scope scope = scope(helper);
		try {
			ServerPlayer player = TestPlayers.mock(scope.helper());
			TestPlayers.track(scope.helper(), player);
			TestPlayers.track(scope.helper(), player);
			helper.assertTrue(tracked(scope.helper()) == 1, "Wrapper plus raw-helper hook retains one reference per player");
			helper.assertTrue(scope.info().getListeners().count() == 1, "One test has one cleanup listener");
			scope.complete(false);
			helper.assertTrue(!online(player), "A failed test also logs out its player");
			helper.assertTrue(tracked(scope.helper()) == 0, "Completion releases tracked player references");
			scope.complete(false);
			scope.complete(true);
			helper.assertTrue(!online(player), "Repeated completion cannot restore or remove a player twice");
			helper.succeed();
		} finally {
			scope.complete(false);
		}
	}

	@GameTest(maxTicks = 20)
	public void oldFixtureCleanupDoesNotDisconnectRejoinedIdentity(GameTestHelper helper) {
		Scope oldScope = scope(helper);
		Scope newScope = scope(helper);
		try {
			ServerPlayer old = TestPlayers.survivor(oldScope.helper());
			TestPlayers.play(old, 123);
			TestPlayers.leave(old);
			ServerPlayer rejoined = TestPlayers.again(newScope.helper(), old.getUUID());
			helper.assertTrue(old != rejoined && online(rejoined), "The same UUID rejoins as a new live player");
			helper.assertTrue(TestPlayers.played(rejoined) == 123, "Leaving/rejoining preserves the fixture's saved statistics");
			oldScope.complete(true);
			helper.assertTrue(online(rejoined), "Old fixture cleanup checks object identity before removing the UUID");
			newScope.complete(false);
			helper.assertTrue(!online(rejoined), "The rejoined survivor still belongs to its new test's cleanup");
			helper.succeed();
		} finally {
			oldScope.complete(false);
			newScope.complete(false);
		}
	}

	@GameTest(maxTicks = 20)
	public void immediateCompletionClosesAndReleasesMockTransport(GameTestHelper helper) {
		Scope scope = scope(helper);
		ServerPlayer player = scope.helper().makeMockServerPlayerInLevel();
		try {
			Field channelField = MockPlayerChannels.class.getDeclaredField("CHANNEL");
			Field connectionField = MockPlayerChannels.class.getDeclaredField("CONNECTION");
			channelField.setAccessible(true);
			connectionField.setAccessible(true);
			Object connection = ((Field) connectionField.get(null)).get(player.connection);
			io.netty.channel.embedded.EmbeddedChannel channel =
					(io.netty.channel.embedded.EmbeddedChannel) ((Field) channelField.get(null)).get(connection);
			io.netty.buffer.ByteBuf queued = io.netty.buffer.Unpooled.buffer().writeByte(1);
			channel.outboundMessages().add(queued);
			helper.assertTrue(channel.isOpen() && !channel.outboundMessages().isEmpty(), "Mock has queued login data before any tick");
			scope.complete(true);
			helper.assertTrue(!channel.isOpen() && channel.outboundMessages().isEmpty() && channel.inboundMessages().isEmpty(),
					"Immediate completion closes and empties the fake transport");
			helper.assertTrue(queued.refCnt() == 0, "Reference-counted queued data is released");
			scope.complete(false);
			helper.succeed();
		} catch (ReflectiveOperationException e) {
			throw new AssertionError("Cannot inspect mock transport", e);
		} finally {
			scope.complete(false);
		}
	}

	@GameTest(maxTicks = 20)
	public void nonMockPlayersAreNotRegistered(GameTestHelper helper) {
		Scope scope = scope(helper);
		ServerPlayer ordinary = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(),
				new GameProfile(UUID.randomUUID(), "not-a-mock"), ClientInformation.createDefault());
		helper.assertTrue(TestPlayers.track(scope.helper(), ordinary) == ordinary, "Tracking preserves the returned player");
		helper.assertTrue(scope.info().getListeners().count() == 0 && tracked(scope.helper()) == 0,
				"No embedded mock connection means no fixture cleanup ownership");
		helper.succeed();
	}

	private static boolean online(ServerPlayer player) {
		return player.level().getServer().getPlayerList().getPlayer(player.getUUID()) == player;
	}

	private static Scope scope(GameTestHelper parent) {
		try {
			Field field = GameTestHelper.class.getDeclaredField("testInfo");
			field.setAccessible(true);
			GameTestInfo source = (GameTestInfo) field.get(parent);
			GameTestInfo info = new GameTestInfo(source.getTestHolder(), Rotation.NONE, parent.getLevel(), RetryOptions.noRetries()) {
				@Override
				public BlockPos getTestOrigin() {
					return parent.absolutePos(BlockPos.ZERO);
				}
			};
			return new Scope(info, new GameTestHelper(info));
		} catch (ReflectiveOperationException e) {
			throw new AssertionError("Cannot create isolated fixture lifetime", e);
		}
	}

	private static int tracked(GameTestHelper helper) {
		try {
			Field field = TestPlayers.class.getDeclaredField("MADE");
			field.setAccessible(true);
			Map<?, ?> made = (Map<?, ?>) field.get(null);
			Object players = made.get(helper);
			return players instanceof List<?> list ? list.size() : 0;
		} catch (ReflectiveOperationException e) {
			throw new AssertionError("Cannot inspect retained fixture references", e);
		}
	}

	private record Scope(GameTestInfo info, GameTestHelper helper) {
		/** Dispatch the framework's completion notification without deliberately failing the outer regression test. */
		void complete(boolean passed) {
			info.getListeners().forEach(listener -> {
				if (passed) {
					listener.testPassed(info, null);
				} else {
					listener.testFailed(info, null);
				}
			});
		}
	}
}
