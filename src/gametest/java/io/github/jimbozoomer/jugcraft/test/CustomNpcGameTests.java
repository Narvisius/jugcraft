package io.github.jimbozoomer.jugcraft.test;

import com.mojang.authlib.GameProfile;
import io.github.jimbozoomer.jugcraft.town.CustomNpcs;
import io.github.jimbozoomer.jugcraft.town.NpcSkins;
import io.github.jimbozoomer.jugcraft.town.Townsfolk;
import io.github.jimbozoomer.jugcraft.town.JugcraftTown;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

/**
 * Operator-made NPCs and guards ({@link CustomNpcs}): the commands and their permission, what is saved, guards fighting
 * monsters but never players, patrols in order, removal while unloaded, and the bounded, stale-proof skin lookup.
 */
public class CustomNpcGameTests {
	@GameTest(maxTicks = 20)
	public void operatorsMakeAndEditNpcsByCommandAndOthersCannot(GameTestHelper h) {
		floor(h, 8, 8);
		MinecraftServer server = h.getLevel().getServer();
		ServerPlayer visitor = h.makeMockServerPlayerInLevel();
		visitor.setGameMode(GameType.SURVIVAL);
		CommandSourceStack stranger = visitor.createCommandSourceStack().withSuppressedOutput().withPosition(centre(h, 1, 1));
		h.assertFalse(CustomNpcs.isOperator(stranger), "The mock player is not an operator (precondition)");
		server.getCommands().performPrefixedCommand(stranger, "npc create Mallory");
		server.getCommands().performPrefixedCommand(stranger, "guard create Mallory");
		server.getCommands().performPrefixedCommand(stranger, "jugcraft npc create Mallory");
		h.assertTrue(custom(h).isEmpty(), "A player who is not an operator makes nothing");

		CommandSourceStack operator = at(h, server, 2, 2);
		server.getCommands().performPrefixedCommand(operator, "npc create Old Tom");
		List<Townsfolk> made = custom(h);
		h.assertTrue(made.size() == 1, "The operator's /npc create makes one NPC: " + made.size());
		Townsfolk tom = made.getFirst();
		CustomNpcs.Data data = tom.custom();
		h.assertTrue(!data.guard() && tom.getName().getString().equals("Old Tom") && tom.spot() == -1 && tom.role().equals("townsfolk")
				&& tom.getItemBySlot(EquipmentSlot.MAINHAND).isEmpty() && tom.home().equals(h.absolutePos(new BlockPos(2, 2, 2))),
				"An NPC, named, outside the town's list, posted where the operator stood");
		h.assertTrue(CustomNpcs.registry(server).entries().get(data.id()) != null, "and numbered in the index");

		// Edits act on the selection (making one selects it): no number needed.
		server.getCommands().performPrefixedCommand(operator, "npc say Mind the step");
		server.getCommands().performPrefixedCommand(operator, "npc say add §cRed§r text, no codes");
		server.getCommands().performPrefixedCommand(operator, "npc name Thomas");
		server.getCommands().performPrefixedCommand(operator, "npc skin town baker");
		server.getCommands().performPrefixedCommand(operator, "npc wander 5");
		h.assertTrue(data.lines().equals(List.of("Mind the step", "Red text, no codes")), "Dialogue lines, formatting codes dropped: " + data.lines());
		h.assertTrue(tom.getName().getString().equals("Thomas") && tom.skin().equals("baker") && data.wander() == 5, "Renamed, reskinned, wanders 5");
		server.getCommands().performPrefixedCommand(operator, "npc skin town no_such_skin");
		h.assertTrue(tom.skin().equals("baker"), "A skin the town does not have is refused");
		server.getCommands().performPrefixedCommand(at(h, server, 5, 2), "npc post");
		h.assertTrue(tom.home().equals(h.absolutePos(new BlockPos(5, 2, 2))), "The post moves to where the operator stands");

		// A guard, by the /guard alias; it becomes the selection.
		server.getCommands().performPrefixedCommand(at(h, server, 2, 6), "guard create Watchman");
		Townsfolk guard = custom(h).stream().filter(npc -> npc.custom().guard()).findFirst().orElse(null);
		h.assertTrue(guard != null && guard.isGuard() && guard.getItemBySlot(EquipmentSlot.MAINHAND).is(Items.IRON_SWORD)
				&& guard.skin().equals(CustomNpcs.DEFAULT_GUARD_SKIN), "/guard create makes a guard with the town guard's sword and skin");
		server.getCommands().performPrefixedCommand(at(h, server, 6, 6), "npc patrol add");
		server.getCommands().performPrefixedCommand(at(h, server, 6, 2), "npc patrol add");
		server.getCommands().performPrefixedCommand(at(h, server, 2, 2), "guard patrol add");
		h.assertTrue(guard.custom().patrol().equals(List.of(h.absolutePos(new BlockPos(6, 2, 6)), h.absolutePos(new BlockPos(6, 2, 2)),
				h.absolutePos(new BlockPos(2, 2, 2)))), "Waypoints are added, in order, where the operator stands");
		server.getCommands().performPrefixedCommand(operator, "npc patrol remove");
		h.assertTrue(guard.custom().patrol().size() == 2, "patrol remove with no number drops the last waypoint");
		server.getCommands().performPrefixedCommand(operator, "npc patrol add ~100 ~ ~");
		h.assertTrue(guard.custom().patrol().size() == 2, "A waypoint beyond reach of the post is refused");
		h.assertTrue(data.patrol().isEmpty(), "Editing the guard left Thomas alone");

		// Select by number, then remove.
		server.getCommands().performPrefixedCommand(operator, "npc select " + data.id());
		server.getCommands().performPrefixedCommand(operator, "npc remove");
		server.getCommands().performPrefixedCommand(operator, "jugcraft npc remove " + guard.custom().id());
		h.assertTrue(tom.isRemoved() && guard.isRemoved() && CustomNpcs.registry(server).entries().get(data.id()) == null,
				"Both are removed, and the index forgets them");
		h.succeed();
	}

	@GameTest(maxTicks = 20)
	public void everythingAnOperatorSetIsSavedWithTheNpc(GameTestHelper h) {
		floor(h, 8, 8);
		ServerLevel level = h.getLevel();
		Townsfolk npc = CustomNpcs.create(level, centre(h, 2, 2), 90, "Saved Sal", CustomNpcs.NPC, null);
		h.assertTrue(npc != null, "Made");
		CustomNpcs.setLine(npc, "One");
		CustomNpcs.addLine(npc, "Two");
		CustomNpcs.setWander(npc, 7);
		h.assertTrue(CustomNpcs.addWaypoint(npc, level, h.absolutePos(new BlockPos(5, 2, 5))) == null, "Waypoint added");
		GameProfile profile = new GameProfile(UUID.randomUUID(), "Sal");
		CustomNpcs.setPlayerSkin(npc, "Sal", ResolvableProfile.createResolved(profile));
		h.assertTrue(npc.skin().equals(CustomNpcs.PLAYER_SKIN) && npc.profile().equals(npc.custom().profile()), "The player skin is synced");

		TagValueOutput out = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
		npc.saveWithoutId(out);
		Townsfolk copy = JugcraftTown.TOWNSFOLK.create(level, EntitySpawnReason.COMMAND);
		copy.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), out.buildResult()));
		CustomNpcs.Data loaded = copy.custom();
		h.assertTrue(loaded != null && loaded.id() == npc.custom().id() && !loaded.guard() && loaded.lines().equals(List.of("One", "Two"))
				&& loaded.wander() == 7 && loaded.patrol().equals(List.of(h.absolutePos(new BlockPos(5, 2, 5))))
				&& loaded.skinName().equals("Sal") && loaded.profile() != null, "The settings come back after a save and load");
		h.assertTrue(copy.skin().equals(CustomNpcs.PLAYER_SKIN) && copy.profile().equals(loaded.profile()) && copy.home().equals(npc.home())
				&& copy.spot() == -1, "with the player skin re-synced, the post kept and still outside the town's list");
		CustomNpcs.remove(level.getServer(), npc.custom().id());
		h.succeed();
	}

	@GameTest(maxTicks = 200)
	public void guardsFightMonstersButNeverPlayers(GameTestHelper h) {
		floor(h, 8, 8);
		ServerLevel level = h.getLevel();
		Townsfolk guard = CustomNpcs.create(level, centre(h, 2, 2), 0, "Sentry", CustomNpcs.GUARD, null);
		ServerPlayer player = h.makeMockServerPlayerInLevel();
		player.setGameMode(GameType.SURVIVAL);
		player.snapTo(centre(h, 3, 3).x, centre(h, 3, 3).y, centre(h, 3, 3).z, 0, 0);
		h.assertFalse(guard.hurtServer(level, level.damageSources().playerAttack(player), 4.0F), "Players cannot hurt a guard");
		h.assertTrue(guard.getHealth() == guard.getMaxHealth(), "and it takes no damage");
		Zombie zombie = EntityTypes.ZOMBIE.create(level, EntitySpawnReason.COMMAND);
		zombie.snapTo(centre(h, 6, 6).x, centre(h, 6, 6).y, centre(h, 6, 6).z, 0, 0);
		zombie.setPersistenceRequired();
		zombie.setNoAi(true);
		level.addFreshEntity(zombie);
		boolean[] wrong = {false};
		h.succeedWhen(() -> {
			// Checked every tick until it passes.
			if (guard.getTarget() instanceof Player) {
				wrong[0] = true;
			}
			h.assertFalse(wrong[0], "A guard never targets a player");
			h.assertTrue(guard.getTarget() == zombie, "The guard turns on the monster");
			zombie.discard();
			CustomNpcs.remove(level.getServer(), guard.custom().id());
		});
	}

	@GameTest(maxTicks = 600)
	public void patrolsAreWalkedInOrder(GameTestHelper h) {
		floor(h, 8, 8);
		ServerLevel level = h.getLevel();
		// An NPC rather than a guard, so a monster from a neighbouring test cannot pull it away; both use the same goal.
		Townsfolk guard = CustomNpcs.create(level, centre(h, 1, 1), 0, "Patroller", CustomNpcs.NPC, null);
		BlockPos first = h.absolutePos(new BlockPos(6, 2, 1)), second = h.absolutePos(new BlockPos(6, 2, 6));
		h.assertTrue(CustomNpcs.addWaypoint(guard, level, first) == null && CustomNpcs.addWaypoint(guard, level, second) == null, "Two waypoints");
		List<Integer> order = new ArrayList<>();
		h.succeedWhen(() -> {
			int next = guard.custom().patrolIndex();
			if (order.isEmpty() || order.getLast() != next) {
				if (!order.isEmpty()) { var reached = order.getLast() == 0 ? first : second; h.assertTrue(guard.distanceToSqr(reached.getX()+.5,reached.getY(),reached.getZ()+.5)<2.25, "Patrol physically reaches each waypoint, not just advances after a stall"); }
                order.add(next);
			}
			h.assertTrue(order.size() >= 3, "It has gone round: " + order);
			h.assertTrue(order.subList(0, 3).equals(List.of(0, 1, 0)), "Waypoints are walked in order: " + order);
			CustomNpcs.remove(level.getServer(), guard.custom().id());
		});
	}

	@GameTest(maxTicks = 60)
	public void aRemovedNpcNeverComesBackFromDisk(GameTestHelper h) {
		floor(h, 8, 8);
		ServerLevel level = h.getLevel();
		Townsfolk npc = CustomNpcs.create(level, centre(h, 2, 2), 0, "Ghost", CustomNpcs.NPC, null);
		int id = npc.custom().id();
		// A copy as a chunk on disk would hold it (removed while unloaded, or an old copy of the chunk).
		TagValueOutput out = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
		npc.saveWithoutId(out);
		h.assertTrue(CustomNpcs.find(level.getServer(), id) == npc, "Found by number while loaded");
		h.assertTrue(CustomNpcs.remove(level.getServer(), id) && npc.isRemoved(), "Removed by number");
		h.assertFalse(CustomNpcs.remove(level.getServer(), id), "A number already removed is not found again");
		Townsfolk returned = JugcraftTown.TOWNSFOLK.create(level, EntitySpawnReason.LOAD);
		returned.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), out.buildResult()));
		level.addFreshEntity(returned);
		h.succeedWhen(() -> h.assertTrue(returned.isRemoved(), "The removed NPC leaves when it loads"));
	}

	@GameTest(maxTicks = 20)
	public void skinLookupsAreBoundedAndStaleRepliesChangeNothing(GameTestHelper h) {
		floor(h, 8, 8);
		ServerLevel level = h.getLevel();
		MinecraftServer server = level.getServer();
		List<CompletableFuture<GameProfile>> asked = new ArrayList<>();
		NpcSkins.useResolver((s, name) -> {
			CompletableFuture<GameProfile> reply = new CompletableFuture<>();
			asked.add(reply);
			return reply;
		}, profile -> true);
		List<Townsfolk> npcs = new ArrayList<>();
		try {
			for (int i = 0; i < 6; i++) {
				npcs.add(CustomNpcs.create(level, centre(h, 1 + i, 3), 0, "Model " + i, CustomNpcs.NPC, null));
			}
			h.assertTrue(NpcSkins.request(server, npcs.get(0), "not a name!", null) == NpcSkins.Result.INVALID, "Not a player name");
			String[] names = {"Alpha", "Bravo", "Charlie", "Delta"};
			for (int i = 0; i < 4; i++) {
				h.assertTrue(NpcSkins.request(server, npcs.get(i), names[i], null) == NpcSkins.Result.STARTED, "Lookup " + i + " starts");
			}
			h.assertTrue(NpcSkins.outstanding() == NpcSkins.MAX_OUTSTANDING && asked.size() == 4, "Four in flight");
			h.assertTrue(NpcSkins.request(server, npcs.get(4), "Echo", null) == NpcSkins.Result.BUSY, "A fifth name is refused");
			h.assertTrue(NpcSkins.request(server, npcs.get(5), "alpha", null) == NpcSkins.Result.JOINED && asked.size() == 4,
					"The same name (any case) joins the lookup on its way");

			// Charlie's NPC is given a town skin before the reply: the reply is stale.
			CustomNpcs.setTownSkin(npcs.get(2), "baker");
		} catch (RuntimeException | AssertionError failed) {
			cleanUp(server, npcs);
			throw failed;
		}
		// Replies arrive; they are applied on the server thread.
		asked.get(0).complete(new GameProfile(UUID.randomUUID(), "Alpha"));
		asked.get(1).completeExceptionally(new RuntimeException("lookup failed"));
		asked.get(2).complete(new GameProfile(UUID.randomUUID(), "Charlie"));
		h.succeedWhen(() -> {
			h.assertTrue(npcs.get(0).skin().equals(CustomNpcs.PLAYER_SKIN) && npcs.get(0).custom().skinName().equals("Alpha")
					&& npcs.get(5).custom().skinName().equals("Alpha"), "Both NPCs waiting on Alpha wear Alpha's skin");
			h.assertTrue(npcs.get(1).skin().equals(CustomNpcs.DEFAULT_NPC_SKIN) && npcs.get(1).custom().profile() == null,
					"A failed lookup changes nothing");
			h.assertTrue(npcs.get(2).skin().equals("baker") && npcs.get(2).custom().profile() == null, "A stale reply changes nothing");
			h.assertTrue(NpcSkins.outstanding() == 1, "Only Delta is still in flight");
			h.assertTrue(NpcSkins.request(server, npcs.get(4), "ALPHA", null) == NpcSkins.Result.APPLIED
					&& npcs.get(4).custom().skinName().equals("Alpha"), "A found name is cached and applied at once");
			h.assertTrue(NpcSkins.request(server, npcs.get(4), "Bravo", null) == NpcSkins.Result.INVALID, "A name just not found is not retried at once");
			cleanUp(server, npcs);
		});
	}

	private static void cleanUp(MinecraftServer server, List<Townsfolk> npcs) {
		NpcSkins.useResolver(NpcSkins.SERVER, null);
		for (Townsfolk npc : npcs) {
			if (npc != null && npc.custom() != null) {
				CustomNpcs.remove(server, npc.custom().id());
			}
		}
	}

	// ---------------------------------------------------------------- helpers

	private static Vec3 centre(GameTestHelper h, int x, int z) {
		BlockPos at = h.absolutePos(new BlockPos(x, 2, z));
		return new Vec3(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
	}

	/** The console, as an operator, standing at (x, 2, z) of the test. */
	private static CommandSourceStack at(GameTestHelper h, MinecraftServer server, int x, int z) {
		return server.createCommandSourceStack().withSuppressedOutput().withLevel(h.getLevel()).withPosition(centre(h, x, z))
				.withRotation(new Vec2(0, 0));
	}

	private static List<Townsfolk> custom(GameTestHelper h) {
		AABB area = new AABB(Vec3.atCenterOf(h.absolutePos(BlockPos.ZERO)), Vec3.atCenterOf(h.absolutePos(new BlockPos(8, 4, 8)))).inflate(1);
		return h.getLevel().getEntitiesOfClass(Townsfolk.class, area, npc -> npc.custom() != null && !npc.isRemoved());
	}

	private static void floor(GameTestHelper h, int width, int depth) {
		for (int x = 0; x < width; x++) {
			for (int z = 0; z < depth; z++) {
				h.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
			}
		}
	}
}
