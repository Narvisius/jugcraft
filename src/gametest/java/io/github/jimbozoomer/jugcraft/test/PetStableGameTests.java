package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.pets.PetRoster;
import io.github.jimbozoomer.jugcraft.pets.Pets;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;

/**
 * What the companion stable screen shows (client/PetInventoryScreen, docs/features/pet-inventory-nov2.md): a stored or
 * knocked-out pet's record keeps its health as it went in, and the knockout's recovery is still the server's minute.
 */
public class PetStableGameTests {
	@GameTest(maxTicks = 40)
	public void storedPetsKeepTheirHealthForTheStable(GameTestHelper h) {
		var p = h.makeMockServerPlayerInLevel();
		var pos = h.absolutePos(new BlockPos(1, 2, 1));
		var wolf = EntityTypes.WOLF.create(h.getLevel(), EntitySpawnReason.COMMAND);
		wolf.tame(p);
		wolf.setNoAi(true);
		wolf.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0, 0);
		h.getLevel().addFreshEntity(wolf);
		var entry = Pets.enroll(wolf);
		wolf.setHealth(7.0F);
		float max = wolf.getMaxHealth();
		h.assertTrue(Pets.store(wolf, entry, false), "Recalled");
		h.assertTrue(entry.getFloatOr("health", -1) == 7.0F && entry.getFloatOr("max_health", -1) == max,
				"The record keeps the health it went in with: " + entry.getFloatOr("health", -1) + " / " + entry.getFloatOr("max_health", -1));

		var cat = EntityTypes.CAT.create(h.getLevel(), EntitySpawnReason.COMMAND);
		cat.tame(p);
		cat.setNoAi(true);
		cat.snapTo(pos.getX() + 2.5, pos.getY(), pos.getZ() + 0.5, 0, 0);
		h.getLevel().addFreshEntity(cat);
		cat.hurtServer(h.getLevel(), cat.damageSources().generic(), 10000);
		var record = PetRoster.get(h.getLevel().getServer()).entries.get(cat.getUUID());
		h.assertTrue(record != null && record.getStringOr("state", "").equals("KNOCKED_OUT"), "Knocked out, not killed");
		h.assertTrue(record.getFloatOr("health", -1) == record.getFloatOr("max_health", -2) * 0.5F,
				"A knock-out goes into the stable at half health: " + record.getFloatOr("health", -1));
		long left = record.getLongOr("ready", 0) - h.getLevel().getServer().overworld().getGameTime();
		h.assertTrue(left > 1100 && left <= 1200, "The recovery is the server's minute: " + left + " ticks");
		h.assertTrue(Pets.action(p, cat.getUUID().toString(), "summon").contains("recovering"), "and the server refuses an early deploy");
		h.succeed();
	}
}
