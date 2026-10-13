package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.lair.DoilyLaceBlock;
import io.github.jimbozoomer.jugcraft.lair.JugcraftLairs;
import io.github.jimbozoomer.jugcraft.lair.tatterlace.BindingThreadEntity;
import io.github.jimbozoomer.jugcraft.lair.tatterlace.GoldenThimble;
import io.github.jimbozoomer.jugcraft.lair.tatterlace.JugcraftTatterlace;
import io.github.jimbozoomer.jugcraft.lair.tatterlace.LaceSnareEntity;
import io.github.jimbozoomer.jugcraft.lair.tatterlace.SpiderlingEntity;
import io.github.jimbozoomer.jugcraft.lair.tatterlace.SpindleLoft;
import io.github.jimbozoomer.jugcraft.lair.tatterlace.TatterEggSacEntity;
import io.github.jimbozoomer.jugcraft.lair.tatterlace.TatterlaceEntity;
import io.github.jimbozoomer.jugcraft.lair.tatterlace.TatterlaceLoot;
import io.github.jimbozoomer.jugcraft.weapons.StitchBoon;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Madame Tatterlace (docs/features/tatterlace.md), as far as a game-test server can show her: one put down on her own
 * (with no lair instance, over a loft laid out from an origin under the test) waits on her silk until a player's blow
 * wakes her and lowers herself onto the lace; her health grows with the party; Needlepoint strikes in front and not
 * behind; a segment of her doily drops away and is knitted back, never the ring round a spool nor the tape's foot; Taking
 * In the Seams spits her egg sacs, her Brood comes out of them and goes with her; Frenzied Stitching shortens her
 * cooldowns; her loot is each participant's own; the Needle Rapier stitches on its third hit; the Golden Thimble turns a
 * projectile aside; a snare lets go; and a struck thread snaps. Her fight in the Spindle Loft itself runs in a real world,
 * in {@link TatterlaceClientGameTests}.
 *
 * <p>All the game tests share one world, and she fights any player within 38 blocks of her doily's centre. So each test
 * sends her away before it ends, and a test that lets her act on her own is {@link #lifted} high above the others, each to
 * its own height.
 */
public class TatterlaceGameTests {
	/** Where the doily's centre is, in the test's own coordinates. */
	private static final BlockPos CENTRE = new BlockPos(4, 1, 4);
	/**
	 * A new player cannot be hurt until its client has loaded the world, or for 60 ticks; a mock player has no client, so
	 * a test that hurts one waits this long first.
	 */
	private static final int LOADING_TICKS = 80;

	/** The origin of a loft whose doily's centre lies over {@code centre} (absolute), its lace in that block. */
	private static BlockPos origin(BlockPos centre) {
		return centre.offset(-(int) Math.floor(SpindleLoft.DOILY_X), -SpindleLoft.LACE, -(int) Math.floor(SpindleLoft.DOILY_Z));
	}

	private static TatterlaceEntity waiting(GameTestHelper helper) {
		return waiting(helper, helper.absolutePos(CENTRE));
	}

	/** She waits on her silk over a doily whose centre is {@code centre} (absolute). */
	private static TatterlaceEntity waiting(GameTestHelper helper, BlockPos centre) {
		return TatterlaceEntity.summon(helper.getLevel(), origin(centre), null);
	}

	/** A survival player standing {@code x}, {@code z} from the doily's centre {@code centre} (absolute), on its floor. */
	private static ServerPlayer player(GameTestHelper helper, BlockPos centre, double x, double z) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setGameMode(GameType.SURVIVAL);
		Vec3 at = Vec3.atBottomCenterOf(centre).add(x, 0.0, z);
		player.snapTo(at.x, at.y, at.z, 0.0F, 0.0F);
		return player;
	}

	/**
	 * Where a test that lets her act on her own stands: {@code height} blocks above its structure, on a stone floor just
	 * under her lace, out of her sight of every other test's players. Each such test has its own height, at least 60
	 * blocks from the next and from the tests below; returns the doily's centre there.
	 */
	private static BlockPos lifted(GameTestHelper helper, int height) {
		return lifted(helper, height, CENTRE);
	}

	private static BlockPos lifted(GameTestHelper helper, int height, BlockPos relativeCentre) {
		ServerLevel level = helper.getLevel();
		BlockPos centre = helper.absolutePos(relativeCentre).above(height);
		// Egg sacs lie near the doily's rim. Support the entire encounter, not just its centre,
		// so the brood cannot fall out of this elevated fixture as soon as it hatches.
		int radius = (int) Math.ceil(SpindleLoft.DOILY_RADIUS) + 2;
		for (int x = -radius; x <= radius; x++) {
			for (int z = -radius; z <= radius; z++) {
				level.setBlockAndUpdate(centre.offset(x, -1, z), Blocks.STONE.defaultBlockState());
			}
		}
		return centre;
	}

	/** Checks {@code body}, then sends her away whether it passed or failed, so a failed test leaves no seamstress fighting on. */
	private static void ending(ServerLevel level, TatterlaceEntity tatterlace, Runnable body) {
		try {
			body.run();
		} finally {
			dismiss(level, tatterlace);
		}
	}

	/** Sends her away, so she fights no other test's players; her egg sacs and brood follow. */
	private static void dismiss(ServerLevel level, TatterlaceEntity tatterlace) {
		for (TatterEggSacEntity sac : tatterlace.sacs(level)) {
			sac.discard();
		}
		for (UUID id : tatterlace.brood()) {
			if (level.getEntity(id) instanceof SpiderlingEntity spiderling) {
				spiderling.discard();
			}
		}
		tatterlace.discard();
	}

	private static Item item(String id) {
		return BuiltInRegistries.ITEM.getValue(Jugcraft.id(id));
	}

	/**
	 * Waiting on her silk she takes no harm; a player's blow wakes her, her health set for the party, and she lowers
	 * herself onto her lace over {@value TatterlaceEntity#DESCEND_TICKS} ticks, to fight.
	 */
	@GameTest(maxTicks = 80)
	public void aBlowWakesHerAndSheComesDown(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos centre = lifted(helper, 60);
		TatterlaceEntity tatterlace = waiting(helper, centre);
		ServerPlayer player = player(helper, centre, 0.5, 30.0);
		boolean perched = tatterlace.phase() == TatterlaceEntity.Phase.WAITING && tatterlace.position().distanceTo(tatterlace.perch()) < 0.01;
		boolean harmless = !tatterlace.hurtServer(level, level.damageSources().playerAttack(player), 10.0F);
		boolean woken = tatterlace.phase() == TatterlaceEntity.Phase.DESCENDING && tatterlace.getHealth() == tatterlace.getMaxHealth();
		player.discard();
		helper.runAfterDelay(TatterlaceEntity.DESCEND_TICKS + 3, () -> {
			ending(level, tatterlace, () -> {
				helper.assertTrue(perched, "She did not wait on her silk");
				helper.assertTrue(harmless, "Waiting, she was harmed");
				helper.assertTrue(woken, "A blow did not wake her at full health");
				helper.assertTrue(tatterlace.phase() == TatterlaceEntity.Phase.FITTING, "She did not come down to fight: " + tatterlace.phase());
				helper.assertTrue(Math.abs(tatterlace.getY() - tatterlace.floorY()) < 0.1, "She is not on her lace but at " + tatterlace.getY());
			});
			helper.succeed();
		});
	}

	/** Her health grows by half for each player after the first, at most two and a half times; a frenzy shortens her cooldowns by a third. */
	@GameTest
	public void herHealthAndHerFrenzy(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		TatterlaceEntity tatterlace = waiting(helper);
		ending(level, tatterlace, () -> {
			helper.assertTrue(TatterlaceEntity.partyScale(1) == 1.0 && TatterlaceEntity.partyScale(2) == 1.5 && TatterlaceEntity.partyScale(4) == 2.5
					&& TatterlaceEntity.partyScale(9) == 2.5, "Her health scales with the party, at most 2.5 times");
			helper.assertTrue(tatterlace.cooldown(240) == 240, "Calm, her cooldowns are their own");
			tatterlace.startFrenzy(level);
			helper.assertTrue(tatterlace.frenzied() && tatterlace.phase() == TatterlaceEntity.Phase.FRENZY, "No Frenzied Stitching");
			helper.assertTrue(tatterlace.cooldown(240) == 160 && tatterlace.cooldown(TatterlaceEntity.GLOBAL_COOLDOWN) == 13,
					"In a frenzy her cooldowns are not a third shorter: " + tatterlace.cooldown(240));
			helper.assertTrue(Math.abs(tatterlace.getY() - tatterlace.floorY()) < 0.1, "In a frenzy she is not on her lace");
		});
		helper.succeed();
	}

	/**
	 * Needlepoint strikes a player in front of her and not one directly behind. The two wait out their loading in
	 * creative, where she pays them no heed.
	 */
	@GameTest(maxTicks = 180)
	public void needlepointStrikesInFrontNotBehind(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos centre = lifted(helper, 120);
		TatterlaceEntity tatterlace = waiting(helper, centre);
		ServerPlayer front = player(helper, centre, 0.5, 3.0);
		ServerPlayer behind = player(helper, centre, 0.5, -2.5);
		front.setGameMode(GameType.CREATIVE);
		behind.setGameMode(GameType.CREATIVE);
		helper.runAfterDelay(LOADING_TICKS, () -> {
			front.setGameMode(GameType.SURVIVAL);
			behind.setGameMode(GameType.SURVIVAL);
			tatterlace.wake(level);
			helper.runAfterDelay(TatterlaceEntity.DESCEND_TICKS + 2, () -> {
				ending(level, tatterlace, () -> {
					Vec3 middle = Vec3.atBottomCenterOf(centre);
					tatterlace.teleportTo(middle.x, tatterlace.floorY(), middle.z);
					tatterlace.setYRot(0.0F);  // facing south, towards the one in front
					int hit = tatterlace.stab(level);
					helper.assertTrue(hit == 1, "The stab struck " + hit + " players, not 1");
					helper.assertTrue(front.getHealth() < front.getMaxHealth(), "The one in front is not stabbed");
					helper.assertTrue(behind.getHealth() == behind.getMaxHealth(), "The one behind her is not spared");
				});
				helper.succeed();
			});
		});
	}

	/**
	 * The doily, by its rule: no lace in a spool's barrel; the dense band round it, which she never unravels; the tape's
	 * foot kept; and a ring round the centre that she unravels drops away and is knitted back, each cell its own pattern,
	 * {@value TatterlaceEntity#KNIT_TICKS} ticks later.
	 */
	@GameTest(maxTicks = 300)
	public void herLaceDropsAwayAndIsKnittedBack(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos centre = lifted(helper, 180);
		TatterlaceEntity tatterlace = waiting(helper, centre);
		BlockPos origin = tatterlace.origin();
		helper.assertTrue(SpindleLoft.lace(27, 23) == -1, "There is lace in the green spool's barrel");
		helper.assertTrue(SpindleLoft.lace(32, 23) == 0 && SpindleLoft.safe(32, 23), "The band round the green spool is not safe");
		helper.assertTrue(SpindleLoft.tapeFoot(40, 56) && !SpindleLoft.tapeFoot(40, 50), "The tape's foot is not where it lands");
		helper.assertTrue(SpindleLoft.lace(40, 36) == 2 && SpindleLoft.lace(40, 39) == 0, "The doily's rings are not its own");
		// A patch of the doily round its centre, its own patterns.
		List<BlockPos> cells = new ArrayList<>();
		List<BlockState> laid = new ArrayList<>();
		int cx = (int) Math.floor(SpindleLoft.DOILY_X);
		int cz = (int) Math.floor(SpindleLoft.DOILY_Z);
		for (int x = cx - 3; x <= cx + 3; x++) {
			for (int z = cz - 3; z <= cz + 3; z++) {
				BlockState lace = JugcraftLairs.DOILY_LACE.defaultBlockState().setValue(DoilyLaceBlock.PATTERN, SpindleLoft.lace(x, z));
				BlockPos cell = origin.offset(x, SpindleLoft.LACE, z);
				level.setBlockAndUpdate(cell, lace);
				cells.add(cell);
				laid.add(lace);
			}
		}
		List<BlockPos> segment = tatterlace.segment(level, true, tatterlace.centre());
		tatterlace.unravel(level, segment);
		boolean dropped = !segment.isEmpty() && segment.stream().allMatch(cell -> level.getBlockState(cell).isAir())
				&& tatterlace.unravelledSegments() == 1;
		boolean ring = segment.stream().allMatch(cell -> Math.hypot(cell.getX() + 0.5 - tatterlace.centre().x, cell.getZ() + 0.5
				- tatterlace.centre().z) < TatterlaceEntity.SEGMENT_WIDTH);
		helper.runAfterDelay(TatterlaceEntity.KNIT_TICKS + 5, () -> {
			ending(level, tatterlace, () -> {
				helper.assertTrue(dropped, "The ring of lace did not drop away");
				helper.assertTrue(ring, "She unravelled lace outside the ring");
				for (int i = 0; i < cells.size(); i++) {
					helper.assertTrue(level.getBlockState(cells.get(i)).equals(laid.get(i)), "The lace at " + cells.get(i) + " was not knitted back as it was");
				}
				helper.assertTrue(tatterlace.unravelledSegments() == 0, "A segment is still down");
			});
			helper.succeed();
		});
	}

	/**
	 * Taking In the Seams spits her egg sacs round the doily and takes her up into the threads; her Brood comes out of
	 * them; and when she falls, her sacs and her brood go with her.
	 */
	@GameTest(structure = "jugcraft-test:tatterlace_arena", padding = 48, maxTicks = 220, maxAttempts = 3, requiredSuccesses = 3)
	public void herBroodComesFromHerSacsAndGoesWithHer(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		// The structure's footprint keeps every rim sac in the test's ticking chunks.
		BlockPos centre = lifted(helper, 240, new BlockPos(24, 1, 24));
		TatterlaceEntity tatterlace = waiting(helper, centre);
		ServerPlayer player = player(helper, centre, 0.5, 5.0);
		player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 400, 4));
		tatterlace.wake(level);
		helper.runAfterDelay(TatterlaceEntity.DESCEND_TICKS + 2, () -> {
			tatterlace.startTakingIn(level);
			int sacs = tatterlace.sacs(level).size();
			helper.runAfterDelay(TatterlaceEntity.TAKE_IN_TICKS + 4, () -> {
				TatterlaceEntity.Phase hanging = tatterlace.phase();
				double up = tatterlace.getY() - tatterlace.floorY();
				tatterlace.begin(level, TatterlaceEntity.Attack.BROOD, player);
				helper.runAfterDelay(TatterlaceEntity.Attack.BROOD.windup + TatterEggSacEntity.HATCH_TICKS + 6, () -> {
					List<UUID> brood = tatterlace.brood();
					List<TatterEggSacEntity> spat = tatterlace.sacs(level);
					String hatchState = spat.stream().map(sac -> sac.blockPosition() + ": ticks=" + sac.tickCount
							+ ", hatching=" + sac.hatching()).toList().toString();
					tatterlace.hurtServer(level, level.damageSources().genericKill(), Float.MAX_VALUE);
					helper.runAfterDelay(25, () -> {
						try {
							helper.assertTrue(sacs == TatterlaceEntity.EGG_SACS, sacs + " egg sacs, not " + TatterlaceEntity.EGG_SACS);
							helper.assertTrue(hanging == TatterlaceEntity.Phase.FINAL, "She did not take to the threads: " + hanging);
							helper.assertTrue(Math.abs(up - TatterlaceEntity.HANG) < 0.6, "She does not hang " + TatterlaceEntity.HANG + " over her lace but " + up);
							helper.assertTrue(brood.size() == TatterlaceEntity.BROOD, brood.size() + " spiderlings came out, not "
									+ TatterlaceEntity.BROOD + "; sacs: " + hatchState);
							for (UUID id : brood) {
								helper.assertTrue(!(level.getEntity(id) instanceof SpiderlingEntity spiderling) || !spiderling.isAlive(), "A spiderling outlived her");
							}
							for (TatterEggSacEntity sac : spat) {
								helper.assertTrue(sac.isRemoved() || !sac.isAlive(), "An egg sac outlived her");
							}
						} finally {
							for (UUID id : brood) {
								if (level.getEntity(id) instanceof SpiderlingEntity spiderling) {
									spiderling.discard();
								}
							}
							spat.forEach(TatterEggSacEntity::discard);
							tatterlace.discard();
						}
						helper.succeed();
					});
				});
			});
		});
	}

	/**
	 * Her loot is each participant's own: everyone who struck her rolls for themselves (Gossamer Silk, and on a first
	 * kill always the Needle Rapier) into their own inventory, and a bystander who never struck her gets nothing.
	 * Afterwards Unravelled is theirs, and a later roll no longer counts as a first kill.
	 */
	@GameTest
	public void herLootIsEachParticipantsOwn(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		TatterlaceEntity tatterlace = waiting(helper);
		BlockPos centre = helper.absolutePos(CENTRE);
		ServerPlayer first = player(helper, centre, 0.5, 3.0);
		ServerPlayer second = player(helper, centre, 2.5, 3.0);
		ServerPlayer bystander = player(helper, centre, -1.5, 3.0);
		tatterlace.took(first, 10.0F);
		tatterlace.took(second, 4.0F);
		ending(level, tatterlace, () -> {
			List<ServerPlayer> participants = tatterlace.participants(level);
			helper.assertTrue(participants.contains(first) && participants.contains(second), "Both who struck her take part");
			helper.assertFalse(participants.contains(bystander), "A bystander who never struck her does not");
			helper.assertTrue(TatterlaceLoot.firstKill(first), "A newcomer has not yet won Unravelled");
			List<ItemStack> loot = TatterlaceLoot.roll(level, tatterlace, first);
			int silk = loot.stream().filter(stack -> stack.is(JugcraftTatterlace.GOSSAMER_SILK)).mapToInt(ItemStack::getCount).sum();
			helper.assertTrue(silk >= 4 && silk <= 8, silk + " Gossamer Silk, not 4 to 8");
			helper.assertTrue(loot.stream().anyMatch(stack -> stack.is(item("needle_rapier"))), "A first kill always brings the Needle Rapier");
			TatterlaceLoot.reward(level, tatterlace, participants);
			for (ServerPlayer player : List.of(first, second)) {
				helper.assertTrue(player.getInventory().countItem(JugcraftTatterlace.GOSSAMER_SILK) >= 4, "Each participant's loot went into their own inventory");
				helper.assertTrue(player.getInventory().countItem(item("needle_rapier")) == 1, "Each first kill brought its own Needle Rapier");
				helper.assertFalse(TatterlaceLoot.firstKill(player), "Unravelled is theirs now");
			}
			helper.assertTrue(bystander.getInventory().countItem(JugcraftTatterlace.GOSSAMER_SILK) == 0, "The bystander got nothing");
			helper.assertTrue(TatterlaceLoot.firstKill(bystander), "The bystander has not won Unravelled");
		});
		helper.succeed();
	}

	/** The Needle Rapier's Stitch: two hits on a foe only count; the third within the window slows it (Slowness II). */
	@GameTest
	public void theNeedleRapierStitches(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer player = player(helper, helper.absolutePos(CENTRE), 0.5, 0.5);
		ItemStack rapier = new ItemStack(item("needle_rapier"));
		player.setItemInHand(InteractionHand.MAIN_HAND, rapier);
		Mob zombie = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, new BlockPos(5, 2, 4));
		rapier.getItem().hurtEnemy(rapier, zombie, player);
		rapier.getItem().hurtEnemy(rapier, zombie, player);
		boolean counted = StitchBoon.stitches(player, zombie) == 2 && !zombie.hasEffect(MobEffects.SLOWNESS);
		rapier.getItem().hurtEnemy(rapier, zombie, player);
		MobEffectInstance slow = zombie.getEffect(MobEffects.SLOWNESS);
		helper.assertTrue(counted, "Two hits did not count as two stitches, or already slowed");
		helper.assertTrue(slow != null && slow.getAmplifier() == StitchBoon.SLOW_AMPLIFIER, "The third hit did not stitch it");
		helper.assertTrue(StitchBoon.stitches(player, zombie) == 0, "The count did not start again");
		zombie.discard();
		helper.succeed();
	}

	/** The Golden Thimble in the offhand turns aside the first projectile, then needs its cooldown: the next one strikes. */
	@GameTest(maxTicks = 120)
	public void theGoldenThimbleTurnsAsideAProjectile(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer player = player(helper, helper.absolutePos(CENTRE), 0.5, 0.5);
		player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(JugcraftTatterlace.GOLDEN_THIMBLE));
		Mob zombie = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, new BlockPos(5, 2, 4));
		helper.runAfterDelay(LOADING_TICKS, () -> {
			float health = player.getHealth();
			boolean first = player.hurtServer(level, level.damageSources().thrown(zombie, zombie), 4.0F);
			boolean spent = player.getCooldowns().isOnCooldown(player.getOffhandItem());
			boolean second = player.hurtServer(level, level.damageSources().thrown(zombie, zombie), 4.0F);
			zombie.discard();
			helper.assertFalse(first, "The first projectile struck");
			helper.assertTrue(spent, "The thimble is not cooling down");
			helper.assertTrue(second && player.getHealth() < health, "The second projectile was turned aside too");
			helper.assertTrue(GoldenThimble.COOLDOWN == 300, "The thimble's cooldown is not 15 s");
			helper.succeed();
		});
	}

	/** A snare stops a player's jump and slows them; half a second after no snare holds them they can jump again. */
	@GameTest(maxTicks = 60)
	public void aSnareLetsGo(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer player = player(helper, helper.absolutePos(CENTRE), 0.5, 0.5);
		LaceSnareEntity.snare(player, level.getGameTime());
		boolean held = LaceSnareEntity.snared(player) && player.hasEffect(MobEffects.SLOWNESS);
		helper.runAfterDelay(25, () -> {
			helper.assertTrue(held, "The snare did not hold the player");
			helper.assertFalse(LaceSnareEntity.snared(player), "The player still cannot jump");
			helper.succeed();
		});
	}

	/** A Binding Thread struck by a player snaps. */
	@GameTest
	public void aStruckThreadSnaps(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		TatterlaceEntity tatterlace = waiting(helper);
		ServerPlayer player = player(helper, helper.absolutePos(CENTRE), 0.5, 4.0);
		BindingThreadEntity thread = BindingThreadEntity.shoot(level, tatterlace, player);
		ending(level, tatterlace, () -> {
			helper.assertTrue(thread.hurtServer(level, level.damageSources().playerAttack(player), 1.0F), "A blow did not snap it");
			helper.assertTrue(thread.isRemoved(), "The thread is still there");
		});
		helper.succeed();
	}
}
