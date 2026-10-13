package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.world.ReviewWorldSettings;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;

/** The flat review world's opt-in cleanup must leave other mobs and ordinary worlds alone. */
public class ReviewWorldGameTests {
	@GameTest
	public void startupSettingsSelectOnlyNamedLocalReviewWorlds(net.minecraft.gametest.framework.GameTestHelper helper) {
		if (TestFocus.skip("tower")) { helper.succeed(); return; }
		var names = ReviewWorldSettings.parseStartupWorlds("{\"suppress_slimes_in_flat_worlds\":[\"Admin Review 6\"]}");
		helper.assertTrue(names.equals(java.util.Set.of("Admin Review 6")), "startup configuration selects only the named review world");
		helper.assertTrue(ReviewWorldSettings.parseStartupWorlds("{}").isEmpty(), "missing opt-in leaves normal worlds alone");
		boolean rejected = false;
		try { ReviewWorldSettings.parseStartupWorlds("{\"suppress_slimes_in_flat_worlds\":[\"../other-world\"]}"); }
		catch (IllegalArgumentException error) { rejected = true; }
		helper.assertTrue(rejected, "paths are not accepted as world-folder names");
		helper.succeed();
	}
	@GameTest(maxTicks = 100)
	public void flatWorldSlimesAreRemovedWithoutDropsAndStaySuppressed(net.minecraft.gametest.framework.GameTestHelper helper) {
		if (TestFocus.skip("tower")) { helper.succeed(); return; }
		var level = helper.getLevel();
		helper.assertTrue(ReviewWorldSettings.isFlat(level), "actual game-test world uses flat generation");
		ReviewWorldSettings.setSlimesSuppressed(level, false);
		var slime = EntityTypes.SLIME.create(level, EntitySpawnReason.COMMAND);
		var pig = EntityTypes.PIG.create(level, EntitySpawnReason.COMMAND);
		var pos = helper.absolutePos(new BlockPos(2, 2, 2));
		slime.setPos(pos.getX() + .5, pos.getY(), pos.getZ() + .5);
		pig.setPos(pos.getX() + .5, pos.getY(), pos.getZ() + .5);
		level.addFreshEntity(slime); level.addFreshEntity(pig);
		helper.assertFalse(slime.isRemoved(), "normal flat worlds keep their slimes by default");
		int drops = level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
				new net.minecraft.world.phys.AABB(pos).inflate(8)).size();
		try {
			helper.assertTrue(ReviewWorldSettings.setSlimesSuppressed(level, true) >= 1, "operator removes already-loaded slimes");
			helper.assertTrue(slime.isRemoved() && !pig.isRemoved(), "cleanup affects slimes only");
			var next = EntityTypes.SLIME.create(level, EntitySpawnReason.COMMAND);
			next.setPos(pos.getX() + .5, pos.getY(), pos.getZ() + .5);
			level.addFreshEntity(next);
			helper.assertTrue(next.isRemoved(), "new or reloaded slimes are discarded on the load event");
			helper.assertTrue(level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
					new net.minecraft.world.phys.AABB(pos).inflate(8)).size() == drops, "discard produces no slime drops");
			var encoded = ReviewWorldSettings.CODEC.encodeStart(NbtOps.INSTANCE, ReviewWorldSettings.get(level)).getOrThrow();
			helper.assertTrue(ReviewWorldSettings.CODEC.encodeStart(NbtOps.INSTANCE,
					ReviewWorldSettings.CODEC.parse(NbtOps.INSTANCE, encoded).getOrThrow()).getOrThrow().equals(encoded),
					"per-world setting survives saved-data codec round trip");
			var nether = level.getServer().getLevel(net.minecraft.world.level.Level.NETHER);
			if (nether != null) helper.assertFalse(ReviewWorldSettings.suppressesSlimes(nether),
					"Nether generation is not treated as the flat review dimension");
		} finally {
			ReviewWorldSettings.setSlimesSuppressed(level, false);
			pig.discard();
		}
		helper.succeed();
	}
}
