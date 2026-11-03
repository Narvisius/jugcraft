package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.raiders.JugcraftRaiders;
import io.github.jimbozoomer.jugcraft.raiders.RaiderBlimp;
import io.github.jimbozoomer.jugcraft.raiders.RaiderCrew;
import io.github.jimbozoomer.jugcraft.raiders.RaiderInfantry;
import io.github.jimbozoomer.jugcraft.raiders.RaiderRaids;
import io.github.jimbozoomer.jugcraft.raiders.RaiderWalker;
import io.github.jimbozoomer.jugcraft.season.JugcraftSeasons;
import io.github.jimbozoomer.jugcraft.town.JugcraftTown;
import io.github.jimbozoomer.jugcraft.town.TownData;
import io.github.jimbozoomer.jugcraft.town.Townsfolk;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * Raider crews (raiders/RaiderCrew, docs/features/raiders.md "Crews"): raid machines are crewed from the party, the crew
 * rides in the right seat, is saved inside its machine and never duplicated, stands idle aboard, bails out when the
 * machine falls, the machine fails without it, a withdrawn raid takes its crew too, and nothing turns on a player.
 */
public class RaiderCrewGameTests {
	@GameTest(maxTicks = 40)
	public void raidMachinesAreCrewedFromTheirOwnParty(GameTestHelper h) {
		floor(h);
		ServerLevel level = h.getLevel();
		BlockPos gather = h.absolutePos(new BlockPos(4, 2, 4));
		BlockPos objective = h.absolutePos(new BlockPos(6, 2, 6));
		RaiderRaids.Raid raid = RaiderRaids.start(level, objective, gather, 5, RandomSource.create(5));
		if (JugcraftSeasons.thanksgivingTruce()) {
			h.assertTrue(raid == null, "No raid starts during the Thanksgiving truce");
			h.succeed();
			return;
		}
		h.assertTrue(raid != null, "The raid starts");
		int expected = 0;
		for (int n : RaiderRaids.PARTY[4]) {
			expected += n;
		}
		h.assertTrue(raid.members().size() == expected && raid.total() == expected,
				"The party is the size PARTY says, crew included: " + raid.members().size() + " of " + expected);
		h.runAfterDelay(2, () -> {
			List<Mob> machines = new ArrayList<>();
			int aboard = 0;
			for (var id : raid.members()) {
				Entity member = level.getEntity(id);
				h.assertTrue(member != null, "Every member is in the world");
				if (RaiderCrew.isMachine(member)) {
					machines.add((Mob) member);
				}
				if (RaiderCrew.aboard(member)) {
					aboard++;
				}
			}
			h.assertTrue(machines.size() == RaiderRaids.PARTY[4][3] + RaiderRaids.PARTY[4][4],
					"Two blimps and a walker come: " + machines.size());
			h.assertTrue(aboard == machines.size(), "Exactly one crew member aboard each machine: " + aboard);
			for (Mob machine : machines) {
				RaiderInfantry crew = RaiderCrew.crew(machine);
				h.assertTrue(crew != null && machine.getPassengers().size() == 1, "Each machine has its one crew member");
				h.assertTrue(raid.id().equals(crew.raid()) && raid.members().contains(crew.getUUID()), "The crew belongs to the raid");
				h.assertTrue(crew.getType() == JugcraftRaiders.GRUNT || crew.getType() == JugcraftRaiders.GUNNER,
						"Crew come from the grunts and gunners: " + crew.getType());
				Vec3 seat = machine.position().add((machine instanceof RaiderWalker ? RaiderCrew.WALKER_SEAT : RaiderCrew.BLIMP_SEAT)
						.yRot(-machine.getYRot() * net.minecraft.util.Mth.DEG_TO_RAD));
				h.assertTrue(crew.position().distanceTo(seat) < 0.3, "The crew sits in its seat: " + crew.position() + " vs " + seat);
				h.assertTrue(machine instanceof RaiderWalker walker ? walker.crewed() : ((RaiderBlimp) machine).crewed(), "and the machine knows it is crewed");
			}
			// Withdrawing takes the machines and their crews: nobody is left behind.
			List<Entity> everyone = new ArrayList<>();
			for (var id : raid.members()) {
				everyone.add(level.getEntity(id));
			}
			RaiderRaids.withdraw(level, raid);
			for (Entity member : everyone) {
				h.assertTrue(member.isRemoved(), "Withdrawn with its raid: " + member);
			}
			h.succeed();
		});
	}

	@GameTest(maxTicks = 120)
	public void aMachineThatLosesItsCrewGoesDownButAnUncrewedOneKeepsGoing(GameTestHelper h) {
		floor(h);
		ServerLevel level = h.getLevel();
		RaiderWalker crewed = walker(h, new BlockPos(2, 2, 2));
		RaiderInfantry driver = grunt(h, new BlockPos(2, 2, 2));
		h.assertTrue(RaiderCrew.board(crewed, driver), "The driver boards");
		h.assertFalse(RaiderCrew.board(crewed, grunt(h, new BlockPos(3, 2, 3))), "The seat holds one");
		RaiderWalker legacy = walker(h, new BlockPos(6, 2, 6));
		driver.hurtServer(level, level.damageSources().generic(), 1000.0F);
		h.assertTrue(!driver.isAlive(), "The driver falls");
		h.runAfterDelay(RaiderCrew.ABANDON_TICKS + 20, () -> {
			h.assertTrue(crewed.isDeadOrDying() || crewed.isRemoved(), "A walker without its driver goes down");
			h.assertTrue(legacy.isAlive() && !legacy.crewed(), "A walker that never had a crew walks on as before");
			legacy.discard();
			h.succeed();
		});
	}

	@GameTest(maxTicks = 40)
	public void theCrewBailsOutWhenItsMachineFalls(GameTestHelper h) {
		floor(h);
		ServerLevel level = h.getLevel();
		RaiderBlimp blimp = JugcraftRaiders.BLIMP.create(level, EntitySpawnReason.COMMAND);
		BlockPos air = h.absolutePos(new BlockPos(4, 12, 4));
		blimp.snapTo(air.getX() + 0.5, air.getY(), air.getZ() + 0.5, 0, 0);
		level.addFreshEntity(blimp);
		RaiderInfantry pilot = grunt(h, new BlockPos(4, 2, 4));
		h.assertTrue(RaiderCrew.board(blimp, pilot), "The pilot boards");
		RaiderWalker walker = walker(h, new BlockPos(1, 2, 7));
		RaiderInfantry driver = grunt(h, new BlockPos(1, 2, 7));
		h.assertTrue(RaiderCrew.board(walker, driver), "The driver boards");
		blimp.hurtServer(level, level.damageSources().generic(), 1000.0F);
		walker.hurtServer(level, level.damageSources().generic(), 10000.0F);
		h.assertTrue(!pilot.isPassenger() && pilot.isAlive() && pilot.hasEffect(MobEffects.SLOW_FALLING),
				"The blimp's pilot bails out and drifts down");
		h.assertTrue(!driver.isPassenger() && driver.isAlive(), "The walker's driver climbs out to fight on foot");
		pilot.discard();
		driver.discard();
		h.succeed();
	}

	@GameTest(maxTicks = 100)
	public void theCrewIsSavedInsideItsMachineAndLoadingNeverMakesMore(GameTestHelper h) {
		floor(h);
		ServerLevel level = h.getLevel();
		RaiderWalker walker = walker(h, new BlockPos(2, 2, 2));
		RaiderInfantry driver = grunt(h, new BlockPos(2, 2, 2));
		RaiderCrew.board(walker, driver);
		TagValueOutput out = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
		walker.saveWithoutId(out);
		CompoundTag saved = out.buildResult();
		h.assertTrue(saved.getListOrEmpty("Passengers").size() == 1, "The driver is saved inside the walker: " + saved.getListOrEmpty("Passengers").size());
		h.assertTrue(saved.getBooleanOr("crewed", false), "with the walker marked crewed");
		// A copy loaded without its passengers (as if they were lost) never conjures a new crew: it goes down instead.
		RaiderWalker copy = JugcraftRaiders.WALKER.create(level, EntitySpawnReason.LOAD);
		copy.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), saved));
		walker.discard();
		driver.discard();
		BlockPos at = h.absolutePos(new BlockPos(6, 2, 6));
		copy.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0, 0);
		level.addFreshEntity(copy);
		h.assertTrue(copy.crewed() && !copy.isVehicle(), "The loaded walker remembers its crew and has none");
		h.runAfterDelay(RaiderCrew.ABANDON_TICKS + 20, () -> {
			h.assertTrue(copy.isDeadOrDying() || copy.isRemoved(), "so it goes down rather than doubling its crew");
			h.assertTrue(level.getEntitiesOfClass(RaiderInfantry.class, copy.getBoundingBox().inflate(4), RaiderInfantry::isAlive).isEmpty(),
					"No new crew member appeared");
			h.succeed();
		});
	}

	@GameTest(maxTicks = 80)
	public void aboardTheCrewStandsIdleFacesItsMachineAndNoMachineTurnsOnAPlayer(GameTestHelper h) {
		floor(h);
		ServerLevel level = h.getLevel();
		RaiderWalker walker = walker(h, new BlockPos(3, 2, 3));
		walker.setNoAi(true);
		walker.setYRot(90.0F);
		walker.setYBodyRot(90.0F);
		RaiderInfantry gunner = JugcraftRaiders.GUNNER.create(level, EntitySpawnReason.COMMAND);
		BlockPos at = h.absolutePos(new BlockPos(3, 2, 3));
		gunner.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0, 0);
		level.addFreshEntity(gunner);
		h.assertTrue(RaiderCrew.board(walker, gunner), "The gunner boards");
		Townsfolk guard = JugcraftTown.TOWNSFOLK.create(level, EntitySpawnReason.COMMAND);
		BlockPos post = h.absolutePos(new BlockPos(6, 2, 6));
		guard.snapTo(post.getX() + 0.5, post.getY(), post.getZ() + 0.5, 0, 0);
		guard.setUp(new TownData.Spot(-1, "guard", null, "Test guard", "guard_blue", BlockPos.ZERO), post);
		guard.setNoAi(true);
		level.addFreshEntity(guard);
		ServerPlayer player = h.makeMockServerPlayerInLevel();
		player.setGameMode(GameType.SURVIVAL);
		h.assertFalse(walker.canAttack(player) || gunner.canAttack(player), "Raider machines and crews never turn on a player");
		h.assertTrue(walker.canAttack(guard) && gunner.canAttack(guard), "only on the city's guards");
		gunner.setTarget(guard);
		h.runAfterDelay(40, () -> {
			h.assertTrue(gunner.isPassenger() && gunner.getVehicle() == walker, "Still aboard");
			h.assertTrue(gunner.getGoalSelector().getAvailableGoals().stream()
					.noneMatch(w -> w.isRunning() && w.getGoal() instanceof RaiderCrew.Afoot), "Its own fighting and marching stand idle aboard");
			h.assertTrue(Math.abs(net.minecraft.util.Mth.wrapDegrees(gunner.yBodyRot - walker.yBodyRot)) < 1.0F,
					"It faces the way its walker does: " + gunner.yBodyRot + " vs " + walker.yBodyRot);
			h.assertTrue(Math.abs(net.minecraft.util.Mth.wrapDegrees(gunner.getYHeadRot() - walker.yBodyRot)) <= 75.5F,
					"Its head turns no further than 75 degrees");
			gunner.discard();
			walker.discard();
			guard.discard();
			h.succeed();
		});
	}

	@GameTest(maxTicks = 20)
	public void aSummonedMachineBringsItsOwnCrew(GameTestHelper h) {
		floor(h);
		ServerLevel level = h.getLevel();
		BlockPos at = h.absolutePos(new BlockPos(4, 2, 4));
		// As the summon command does: make it, finish its spawn, add it with its passengers.
		RaiderWalker summoned = JugcraftRaiders.WALKER.create(level, EntitySpawnReason.COMMAND);
		summoned.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0, 0);
		summoned.finalizeSpawn(level, level.getCurrentDifficultyAt(at), EntitySpawnReason.COMMAND, null);
		level.addFreshEntityWithPassengers(summoned);
		RaiderInfantry crew = RaiderCrew.crew(summoned);
		h.assertTrue(crew != null && summoned.crewed() && level.getEntity(crew.getUUID()) == crew,
				"A walker summoned by a command comes with its driver, in the world once");
		h.assertTrue(crew.raid() == null, "who belongs to no raid, as the walker does not");
		// A walker that already has a rider brings no second one.
		summoned.finalizeSpawn(level, level.getCurrentDifficultyAt(at), EntitySpawnReason.COMMAND, null);
		h.assertTrue(summoned.getPassengers().size() == 1, "Finishing a spawn again adds no second crew member");
		crew.discard();
		summoned.discard();
		h.succeed();
	}

	// ---------------------------------------------------------------- helpers

	private static RaiderWalker walker(GameTestHelper h, BlockPos rel) {
		RaiderWalker walker = JugcraftRaiders.WALKER.create(h.getLevel(), EntitySpawnReason.COMMAND);
		BlockPos at = h.absolutePos(rel);
		walker.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0, 0);
		walker.setPersistenceRequired();
		h.getLevel().addFreshEntity(walker);
		return walker;
	}

	private static RaiderInfantry grunt(GameTestHelper h, BlockPos rel) {
		RaiderInfantry grunt = JugcraftRaiders.GRUNT.create(h.getLevel(), EntitySpawnReason.COMMAND);
		BlockPos at = h.absolutePos(rel);
		grunt.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0, 0);
		grunt.setPersistenceRequired();
		h.getLevel().addFreshEntity(grunt);
		return grunt;
	}

	private static void floor(GameTestHelper h) {
		for (int x = 0; x < 8; x++) {
			for (int z = 0; z < 8; z++) {
				h.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
			}
		}
	}
}
