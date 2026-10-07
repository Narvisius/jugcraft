package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.agriculture.Crow;
import io.github.jimbozoomer.jugcraft.agriculture.Crows;
import io.github.jimbozoomer.jugcraft.agriculture.JugcraftAgriculture;
import io.github.jimbozoomer.jugcraft.agriculture.ScarecrowBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.Scarecrows;
import io.github.jimbozoomer.jugcraft.agriculture.TallDecorationBlock;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.AABB;

/**
 * In-game tests for crows and working scarecrows: how far a scarecrow guards (bare, wearing a head, wearing a lit one,
 * and how high), a crow pecking a ripe crop back three stages (and flying down to it with its own AI), leaving guarded
 * crops alone and taking flight when a scarecrow goes up, fleeing a player (a sneaking one gets closer) and a blow,
 * flying off at nightfall, the spawner finding a field and bringing a flock, and the feathers it drops.
 *
 * <p>Crows are stepped by hand with the time of day passed in, as the shared world clock may read night. Every crow a
 * test makes is persistent while it runs (so none despawns early) and gone before it ends, so none raids another
 * test's crops.
 */
public class CrowGameTests {
	private static final int RIPE = 7;

	/** A crow that always thinks it is day: its own AI, but not the shared world clock. */
	private static final class DayCrow extends Crow {
		DayCrow(ServerLevel level) {
			super(JugcraftAgriculture.CROW, level);
		}

		@Override
		protected void customServerAiStep(ServerLevel level) {
			step(level, true);
		}
	}

	private static void floor(GameTestHelper helper) {
		for (int x = 0; x <= 7; x++) {
			for (int z = 0; z <= 7; z++) {
				helper.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
			}
		}
	}

	/** Ripe wheat on farmland at {@code pos}. */
	private static void ripeWheat(GameTestHelper helper, BlockPos pos) {
		helper.setBlock(pos.below(), Blocks.FARMLAND);
		helper.setBlock(pos, ((CropBlock) Blocks.WHEAT).getStateForAge(RIPE));
	}

	private static int age(GameTestHelper helper, BlockPos pos) {
		return helper.getBlockState(pos).getBlock() instanceof CropBlock crop ? crop.getAge(helper.getBlockState(pos)) : -1;
	}

	/** A scarecrow standing on {@code ground}; returns its upper half, which wears the head. */
	private static ScarecrowBlockEntity scarecrow(GameTestHelper helper, BlockPos ground) {
		helper.setBlock(ground.above(), JugcraftAgriculture.block("scarecrow").defaultBlockState().setValue(TallDecorationBlock.HALF, DoubleBlockHalf.LOWER));
		helper.setBlock(ground.above(2), JugcraftAgriculture.block("scarecrow").defaultBlockState().setValue(TallDecorationBlock.HALF, DoubleBlockHalf.UPPER));
		return helper.getBlockEntity(ground.above(2), ScarecrowBlockEntity.class);
	}

	/** A crow sitting on the block at {@code pos}, with no AI of its own: the test steps it by hand. */
	private static Crow crow(GameTestHelper helper, BlockPos pos) {
		Crow crow = helper.spawnWithNoFreeWill(JugcraftAgriculture.CROW, pos);
		crow.setNoAi(true);
		crow.setPersistenceRequired();
		return crow;
	}

	private static ServerPlayer player(GameTestHelper helper, BlockPos standAt) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setGameMode(GameType.SURVIVAL);
		BlockPos absolute = helper.absolutePos(standAt);
		player.setPos(absolute.getX() + 0.5, absolute.getY(), absolute.getZ() + 0.5);
		return player;
	}

	private static boolean guarded(GameTestHelper helper, BlockPos pos) {
		return Scarecrows.guarded(helper.getLevel(), helper.absolutePos(pos));
	}

	// ---------------------------------------------------------------- scarecrows

	/** A bare scarecrow guards four blocks across; a pumpkin head eight; a jack o'lantern twelve; six up or down. */
	@GameTest(maxTicks = 20)
	public void scarecrowsGuardFartherDressed(GameTestHelper helper) {
		floor(helper);
		ScarecrowBlockEntity scarecrow = scarecrow(helper, new BlockPos(0, 1, 0));
		BlockPos upper = new BlockPos(0, 3, 0);
		helper.assertTrue(Scarecrows.radius(scarecrow) == Scarecrows.BARE, "Bare: " + Scarecrows.radius(scarecrow));
		helper.assertTrue(guarded(helper, new BlockPos(0, 2, 4)) && guarded(helper, new BlockPos(2, 2, 3)), "Four blocks off is guarded");
		helper.assertFalse(guarded(helper, new BlockPos(0, 2, 5)) || guarded(helper, new BlockPos(4, 2, 4)), "Five is not");
		helper.assertTrue(guarded(helper, upper.offset(0, Scarecrows.HEIGHT, 2)) && guarded(helper, upper.offset(0, -Scarecrows.HEIGHT, 2)),
				"Six blocks up or down is guarded");
		helper.assertFalse(guarded(helper, upper.offset(0, Scarecrows.HEIGHT + 1, 2)), "Seven up is not");

		scarecrow.setHead(new ItemStack(Items.CARVED_PUMPKIN));
		helper.assertTrue(Scarecrows.radius(scarecrow) == Scarecrows.HEADED, "With a head: " + Scarecrows.radius(scarecrow));
		helper.assertTrue(guarded(helper, new BlockPos(0, 2, 8)) && guarded(helper, new BlockPos(4, 2, 4)), "Eight blocks off is guarded");
		helper.assertFalse(guarded(helper, new BlockPos(5, 2, 7)), "Nine is not");

		scarecrow.setHead(new ItemStack(Items.JACK_O_LANTERN));
		helper.assertTrue(Scarecrows.radius(scarecrow) == Scarecrows.LIT, "With a lit head: " + Scarecrows.radius(scarecrow));
		helper.assertTrue(guarded(helper, new BlockPos(5, 2, 7)) && guarded(helper, new BlockPos(7, 2, 7)), "Ten blocks off is guarded");
		helper.succeed();
	}

	// ---------------------------------------------------------------- raiding

	/**
	 * Sent after a ripe crop, a crow lands, pecks for two seconds (the crop still ripe halfway) and sets it back three
	 * stages, then rests: it won't raid again for a while, nor go after an unripe crop.
	 */
	@GameTest(maxTicks = 20)
	public void aCrowPecksARipeCropBack(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		floor(helper);
		BlockPos crop = new BlockPos(4, 2, 4);
		ripeWheat(helper, crop);
		Crow crow = crow(helper, crop);
		helper.assertTrue(crow.raid(level, helper.absolutePos(crop)), "It goes for the ripe wheat");
		crow.step(level, true);
		helper.assertTrue(crow.pecking(), "On the crop, it pecks");
		for (int i = 0; i < Crow.PECK_TICKS / 2; i++) {
			crow.step(level, true);
		}
		helper.assertTrue(age(helper, crop) == RIPE && crow.pecking(), "Halfway, the wheat is still ripe");
		for (int i = 0; i < Crow.PECK_TICKS / 2; i++) {
			crow.step(level, true);
		}
		helper.assertTrue(age(helper, crop) == RIPE - Crow.SETBACK, "Pecked back three stages: age " + age(helper, crop));
		helper.assertTrue(!crow.pecking() && crow.crop() == null, "and it is done");
		helper.assertFalse(crow.raid(level, helper.absolutePos(crop)), "It won't go for unripe wheat");
		crow.discard();
		helper.succeed();
	}

	/** With its own AI (by day), a crow above a field flies down to the ripe crop it was sent after and pecks it back. */
	@GameTest(maxTicks = 200)
	public void aCrowFliesDownToPeck(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		floor(helper);
		BlockPos crop = new BlockPos(4, 2, 4);
		ripeWheat(helper, crop);
		DayCrow crow = new DayCrow(level);
		crow.setPersistenceRequired();
		BlockPos above = helper.absolutePos(crop.above(3));
		crow.snapTo(above.getX() + 0.5, above.getY(), above.getZ() + 0.5, 0.0F, 0.0F);
		level.addFreshEntity(crow);
		helper.assertTrue(crow.raid(level, helper.absolutePos(crop)), "It goes for the ripe wheat");
		helper.succeedWhen(() -> {
			helper.assertTrue(age(helper, crop) == RIPE - Crow.SETBACK, "Waiting for it to fly down and peck: at " + crow.position());
			crow.discard();
		});
	}

	/** A scarecrow going up next to the crop it is pecking sends a crow off, the crop spared; no crow goes after it then. */
	@GameTest(maxTicks = 20)
	public void scarecrowsKeepCrowsOff(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		floor(helper);
		BlockPos crop = new BlockPos(6, 2, 6);
		ripeWheat(helper, crop);
		Crow crow = crow(helper, crop);
		helper.assertTrue(crow.raid(level, helper.absolutePos(crop)), "Unguarded, it goes for the wheat");
		crow.step(level, true);
		helper.assertTrue(crow.pecking(), "and pecks");
		scarecrow(helper, new BlockPos(4, 1, 6));
		for (int i = 0; i < Crow.LOOK_TICKS && !crow.fleeing(); i++) {
			crow.step(level, true);
		}
		helper.assertTrue(crow.fleeing() && !crow.pecking() && crow.crop() == null, "A scarecrow goes up: within half a second it takes flight");
		for (int i = 0; i < Crow.PECK_TICKS; i++) {
			crow.step(level, true);
		}
		helper.assertTrue(age(helper, crop) == RIPE, "The wheat is spared");
		Crow another = crow(helper, crop);
		helper.assertFalse(another.raid(level, helper.absolutePos(crop)), "No crow goes after a guarded crop");
		crow.discard();
		another.discard();
		helper.succeed();
	}

	/** A player within six blocks scares a pecking crow off; a sneaking one gets to two and a half; a blow does too. */
	@GameTest(maxTicks = 20)
	public void crowsFleePlayersAndBlows(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		floor(helper);
		BlockPos crop = new BlockPos(5, 2, 4);
		ripeWheat(helper, crop);
		Crow crow = crow(helper, crop);
		ServerPlayer player = player(helper, new BlockPos(1, 2, 4));
		player.setShiftKeyDown(true);
		crow.raid(level, helper.absolutePos(crop));
		crow.step(level, true);
		helper.assertTrue(crow.pecking() && !crow.fleeing(), "A sneaking player four blocks off doesn't scare it");
		player.setShiftKeyDown(false);
		crow.step(level, true);
		helper.assertTrue(crow.fleeing() && !crow.pecking() && crow.crop() == null, "Standing up, the player scares it off");
		helper.assertTrue(crow.getDeltaMovement().x > 0.0, "and it flies away from them: " + crow.getDeltaMovement());
		helper.assertTrue(age(helper, crop) == RIPE, "The wheat is spared");

		player.setPos(player.getX(), player.getY() + 40.0, player.getZ());
		Crow struck = crow(helper, crop);
		struck.raid(level, helper.absolutePos(crop));
		struck.step(level, true);
		helper.assertTrue(struck.pecking(), "With no one near, another crow pecks");
		struck.hurtServer(level, level.damageSources().playerAttack(player), 1.0F);
		helper.assertTrue(struck.isAlive() && struck.fleeing() && !struck.pecking(), "A blow sends it off");
		crow.discard();
		struck.discard();
		helper.succeed();
	}

	/** At nightfall a crow gives up its crop and climbs away, gone once high over the ground. */
	@GameTest(maxTicks = 20)
	public void crowsLeaveAtNightfall(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		floor(helper);
		BlockPos crop = new BlockPos(3, 2, 3);
		ripeWheat(helper, crop);
		Crow crow = crow(helper, crop);
		crow.raid(level, helper.absolutePos(crop));
		crow.step(level, true);
		crow.step(level, false);
		helper.assertTrue(!crow.isRemoved() && !crow.pecking() && crow.crop() == null && crow.getDeltaMovement().y > 0.0,
				"At nightfall it leaves the crop and climbs: " + crow.getDeltaMovement());
		BlockPos at = crow.blockPosition();
		int ground = level.getHeight(Heightmap.Types.MOTION_BLOCKING, at.getX(), at.getZ());
		crow.setPos(crow.getX(), ground + Crow.LEAVE_HEIGHT + 1.0, crow.getZ());
		crow.step(level, false);
		helper.assertTrue(crow.isRemoved(), "High over the ground, it is gone");
		helper.assertTrue(age(helper, crop) == RIPE, "The wheat is untouched");
		crow.discard();
		helper.succeed();
	}

	// ---------------------------------------------------------------- spawning and drops

	/**
	 * The spawner finds a ripe field open to the sky, and not one under a roof (crows look down from the sky); it brings a
	 * flock of two or three crows, in the sky above the field.
	 */
	@GameTest(maxTicks = 20)
	public void aFlockComesToARipeField(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		for (int x = 0; x <= 7; x++) {
			for (int z = 0; z <= 7; z++) {
				ripeWheat(helper, new BlockPos(x, 2, z));
				// The test's own barrier ceiling would be a roof over the field: lift it.
				for (int y = 3; y <= 24; y++) {
					if (helper.getBlockState(new BlockPos(x, y, z)).is(Blocks.BARRIER)) {
						helper.setBlock(new BlockPos(x, y, z), Blocks.AIR);
					}
				}
			}
		}
		BlockPos centre = helper.absolutePos(new BlockPos(4, 2, 4));
		helper.assertTrue(level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, centre.getX(), centre.getZ()) == centre.getY(),
				"The field is open to the sky: the heightmap stands at " + level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, centre.getX(),
						centre.getZ()) + ", the wheat at " + centre.getY());
		RandomSource random = RandomSource.create(1031L);
		for (int x = 0; x <= 7; x++) {
			for (int z = 0; z <= 7; z++) {
				helper.setBlock(new BlockPos(x, 5, z), Blocks.GLASS);
			}
		}
		for (int i = 0; i < 4; i++) {
			BlockPos roofed = Crows.findField(level, centre, random);
			helper.assertTrue(roofed == null, "Under a glass roof crows find no field: " + roofed);
		}
		for (int x = 0; x <= 7; x++) {
			for (int z = 0; z <= 7; z++) {
				helper.setBlock(new BlockPos(x, 5, z), Blocks.AIR);
			}
		}
		BlockPos field = null;
		for (int i = 0; i < 4 && field == null; i++) {
			field = Crows.findField(level, centre, random);
		}
		helper.assertTrue(field != null && Crow.tempting(level.getBlockState(field)), "Open to the sky, it finds ripe wheat: " + field);
		AABB sky = new AABB(field).inflate(4.0, 12.0, 4.0);
		List<Crow> before = level.getEntitiesOfClass(Crow.class, sky);
		int spawned = Crows.spawnFlock(level, field, random);
		List<Crow> flock = new ArrayList<>(level.getEntitiesOfClass(Crow.class, sky));
		flock.removeAll(before);
		for (Crow crow : flock) {
			crow.discard();
		}
		helper.assertTrue(spawned >= Crows.FLOCK_MIN && spawned <= Crows.FLOCK_MAX && flock.size() == spawned,
				"A flock of two or three: " + spawned + ", found " + flock.size());
		BlockPos ground = field;
		helper.assertTrue(flock.stream().allMatch(crow -> crow.getY() >= ground.getY() + 5.0), "in the sky above it");
		helper.succeed();
	}

	/** A crow killed by a player drops up to two feathers; its loot table loads. */
	@GameTest(maxTicks = 20)
	public void crowsDropFeathers(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		floor(helper);
		ServerPlayer player = player(helper, new BlockPos(1, 2, 1));
		Crow crow = crow(helper, new BlockPos(4, 2, 4));
		crow.hurtServer(level, level.damageSources().playerAttack(player), 100.0F);
		helper.assertTrue(crow.isDeadOrDying(), "One blow kills it");
		int feathers = 0;
		for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(new BlockPos(4, 2, 4))).inflate(3.0))) {
			feathers += item.getItem().is(Items.FEATHER) ? item.getItem().getCount() : 0;
		}
		helper.assertTrue(feathers <= 2, "At most two feathers: " + feathers);
		helper.assertTrue(level.getServer().reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE, Jugcraft.id("entities/crow")))
				!= LootTable.EMPTY, "The crow's loot table loads");
		crow.discard();
		helper.succeed();
	}
}
