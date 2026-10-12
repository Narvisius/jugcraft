package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.lair.JugcraftLairs;
import io.github.jimbozoomer.jugcraft.lair.yeti.FallingIcicleEntity;
import io.github.jimbozoomer.jugcraft.lair.yeti.GlacialSpikeEntity;
import io.github.jimbozoomer.jugcraft.lair.yeti.GlacierHall;
import io.github.jimbozoomer.jugcraft.lair.yeti.JugcraftYeti;
import io.github.jimbozoomer.jugcraft.lair.yeti.YetiKingEntity;
import io.github.jimbozoomer.jugcraft.lair.yeti.YetiKingLoot;
import io.github.jimbozoomer.jugcraft.lair.yeti.YetiWhelpEntity;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/**
 * The Yeti King (docs/features/yeti-king.md), as far as a game-test server can show him: one put down on his own (with no
 * lair instance, over a hall laid out from the corner of an arena the hall's size, so his throne, his lake and its dens
 * are all loaded) waits on his throne until a player's blow wakes him, and leaps down onto his lake; his health grows
 * with the party, and the Fury of the Peaks shortens his cooldowns and widens his slam; Maul Swipe strikes in front and
 * not behind; his Ground Slam bares the drift snow round where he lands and never the trampled, and the snow drifts
 * back; an Avalanche Charge into an ice column stuns him, and stunned he takes a third more damage; his kin come out of
 * the dens, never more than four at once, and flee when he is left alone; his frost, and the Yeti Mitten against it; a
 * glacial spike throws a player up and an icicle shatters on its mark; and his loot is each participant's own. His fight
 * in the Glacier Hall itself runs in a real world, in {@link YetiKingClientGameTests}.
 *
 * <p>He fights any player within 38 blocks of his lake's centre; the arena keeps every other test's players farther off
 * than that. Each test sends him away before it ends, with his whelps, and lets the snow drift back.
 */
public class YetiKingGameTests {
	/** An arena of air the hall's size (80 x 88), so everything of his fight is loaded and ticking. */
	private static final String HALL = "jugcraft-test:yeti_hall";
	/**
	 * A new player cannot be hurt until its client has loaded the world, or for 60 ticks; a mock player has no client, so
	 * a test that hurts one waits this long first.
	 */
	private static final int LOADING_TICKS = 80;

	/** The origin of a hall laid out from the arena's corner, its lake's surface a block over the arena's floor. */
	private static BlockPos origin(GameTestHelper helper) {
		return helper.absolutePos(new BlockPos(0, -GlacierHall.LAKE, 0));
	}

	/** He waits on the throne of the hall laid out from {@code origin}. */
	private static YetiKingEntity waiting(GameTestHelper helper, BlockPos origin) {
		return YetiKingEntity.summon(helper.getLevel(), origin, null);
	}

	/** Stone at the lake's layer under the template's cells {@code x0..x1}, {@code z0..z1}, for players to stand on. */
	private static void floor(ServerLevel level, BlockPos origin, int x0, int z0, int x1, int z1) {
		for (int x = x0; x <= x1; x++) {
			for (int z = z0; z <= z1; z++) {
				level.setBlockAndUpdate(origin.offset(x, GlacierHall.LAKE, z), Blocks.STONE.defaultBlockState());
			}
		}
	}

	/** A survival player standing {@code dx}, {@code dz} from his lake's centre, on it. */
	private static ServerPlayer player(GameTestHelper helper, YetiKingEntity king, double dx, double dz) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setGameMode(GameType.SURVIVAL);
		Vec3 at = king.centre().add(dx, 0.0, dz);
		player.snapTo(at.x, at.y, at.z, 0.0F, 0.0F);
		return player;
	}

	/** Checks {@code body}, then sends him away whether it passed or failed, so a failed test leaves no king fighting on. */
	private static void ending(ServerLevel level, YetiKingEntity king, Runnable body) {
		try {
			body.run();
		} finally {
			for (UUID id : king.whelps()) {
				if (level.getEntity(id) instanceof YetiWhelpEntity whelp) {
					whelp.discard();
				}
			}
			king.driftAll(level);
			king.discard();
		}
	}

	/**
	 * Waiting on his throne he takes no harm; a player's blow wakes him at full health, and over {@value
	 * YetiKingEntity#WAKE_TICKS} ticks he roars and leaps down onto his lake below the dais, to hunt.
	 */
	@GameTest(structure = HALL, padding = 8, maxTicks = 120)
	public void aBlowWakesHimAndHeLeapsDown(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos origin = origin(helper);
		YetiKingEntity king = waiting(helper, origin);
		floor(level, origin, 37, 34, 42, 39);
		ServerPlayer player = player(helper, king, 0.5, 0.5);
		boolean throned = king.phase() == YetiKingEntity.Phase.WAITING && king.position().distanceTo(king.throne()) < 0.01;
		boolean harmless = !king.hurtServer(level, level.damageSources().playerAttack(player), 10.0F);
		boolean woken = king.phase() == YetiKingEntity.Phase.WAKING && king.getHealth() == king.getMaxHealth();
		helper.runAfterDelay(YetiKingEntity.WAKE_TICKS + 3, () -> {
			ending(level, king, () -> {
				player.discard();
				Vec3 below = GlacierHall.below().add(origin.getX(), origin.getY(), origin.getZ());
				helper.assertTrue(throned, "He did not wait on his throne");
				helper.assertTrue(harmless, "Waiting, he was harmed");
				helper.assertTrue(woken, "A blow did not wake him at full health");
				helper.assertTrue(king.phase() == YetiKingEntity.Phase.HUNT, "He did not come down to hunt: " + king.phase());
				helper.assertTrue(Math.abs(king.getY() - king.floorY()) < 0.1, "He is not on his lake but at " + king.getY());
				helper.assertTrue(Math.hypot(king.getX() - below.x, king.getZ() - below.z) < 2.0, "He did not land below his dais but at " + king.position());
			});
			helper.succeed();
		});
	}

	/**
	 * His health grows by half for each player after the first, at most two and a half times; the Fury of the Peaks makes
	 * every cooldown a third shorter and his slam bare a wider ring.
	 */
	@GameTest(structure = HALL, padding = 8)
	public void hisHealthAndHisFury(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		YetiKingEntity king = waiting(helper, origin(helper));
		ending(level, king, () -> {
			helper.assertTrue(YetiKingEntity.partyScale(1) == 1.0 && YetiKingEntity.partyScale(2) == 1.5 && YetiKingEntity.partyScale(4) == 2.5
					&& YetiKingEntity.partyScale(9) == 2.5, "His health scales with the party, at most 2.5 times");
			helper.assertTrue(king.cooldown(240) == 240 && king.ring() == YetiKingEntity.RING, "Calm, his cooldowns and his ring are their own");
			king.startFury(level);
			helper.assertTrue(king.furious() && king.phase() == YetiKingEntity.Phase.FURY, "No Fury of the Peaks");
			helper.assertTrue(king.cooldown(240) == 160 && king.cooldown(YetiKingEntity.GLOBAL_COOLDOWN) == 13,
					"In his fury his cooldowns are not a third shorter: " + king.cooldown(240));
			helper.assertTrue(king.ring() == YetiKingEntity.FURY_RING, "In his fury his slam does not bare a wider ring");
		});
		helper.succeed();
	}

	/**
	 * Maul Swipe strikes a player in front of him and not one directly behind. The two wait out their loading in
	 * creative, where he pays them no heed.
	 */
	@GameTest(structure = HALL, padding = 8, maxTicks = 180)
	public void hisSwipeStrikesInFrontNotBehind(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos origin = origin(helper);
		YetiKingEntity king = waiting(helper, origin);
		floor(level, origin, 35, 31, 44, 42);
		ServerPlayer front = player(helper, king, 0.5, 3.0);
		ServerPlayer behind = player(helper, king, 0.5, -3.0);
		front.setGameMode(GameType.CREATIVE);
		behind.setGameMode(GameType.CREATIVE);
		helper.runAfterDelay(LOADING_TICKS, () -> {
			front.setGameMode(GameType.SURVIVAL);
			behind.setGameMode(GameType.SURVIVAL);
			ending(level, king, () -> {
				Vec3 centre = king.centre();
				king.teleportTo(centre.x, king.floorY(), centre.z);
				king.setYRot(0.0F);  // facing south, towards the one in front
				int hit = king.swipe(level);
				helper.assertTrue(hit == 1, "The swipe struck " + hit + " players, not 1");
				helper.assertTrue(front.getHealth() < front.getMaxHealth(), "The one in front is not struck");
				helper.assertTrue(behind.getHealth() == behind.getMaxHealth(), "The one behind him is not spared");
			});
			helper.succeed();
		});
	}

	/**
	 * His Ground Slam blasts the drift snow bare to glare ice within {@value YetiKingEntity#RING} blocks of where he lands,
	 * never the trampled snow nor the snow beyond; {@value YetiKingEntity#GLARE_TICKS} ticks later it has drifted back.
	 */
	@GameTest(structure = HALL, padding = 8, maxTicks = 300)
	public void hisSlamBaresTheDriftSnowAndItDriftsBack(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		YetiKingEntity king = waiting(helper, origin(helper));
		Vec3 at = king.centre();
		BlockPos middle = BlockPos.containing(at).below();
		// Drift snow round the lake's centre, a row of trampled snow across it.
		for (int x = -9; x <= 9; x++) {
			for (int z = -9; z <= 9; z++) {
				level.setBlockAndUpdate(middle.offset(x, 0, z), (z == 0 ? JugcraftLairs.TRAMPLED_SNOW : JugcraftLairs.DRIFT_SNOW).defaultBlockState());
			}
		}
		List<BlockPos> cells = king.bare(level, at, YetiKingEntity.RING);
		boolean bared = !cells.isEmpty() && cells.stream().allMatch(cell -> level.getBlockState(cell).is(JugcraftLairs.GLARE_ICE));
		boolean inRing = cells.stream().allMatch(cell -> Math.hypot(cell.getX() + 0.5 - at.x, cell.getZ() + 0.5 - at.z) <= YetiKingEntity.RING);
		boolean trampledKept = IntStream.rangeClosed(-9, 9).allMatch(x -> level.getBlockState(middle.offset(x, 0, 0)).is(JugcraftLairs.TRAMPLED_SNOW));
		boolean beyondKept = level.getBlockState(middle.offset(9, 0, 9)).is(JugcraftLairs.DRIFT_SNOW);
		int patches = king.baredPatches();
		helper.runAfterDelay(YetiKingEntity.GLARE_TICKS + 5, () -> {
			ending(level, king, () -> {
				helper.assertTrue(bared, "The drift snow round where he lands was not bared to glare ice");
				helper.assertTrue(inRing, "He bared snow outside his ring");
				helper.assertTrue(trampledKept, "He bared the trampled snow");
				helper.assertTrue(beyondKept, "He bared the snow beyond his ring");
				helper.assertTrue(patches == 1, patches + " bare patches, not 1");
				helper.assertTrue(cells.stream().allMatch(cell -> level.getBlockState(cell).is(JugcraftLairs.DRIFT_SNOW)),
						"The snow did not drift back over the bare ice");
				helper.assertTrue(king.baredPatches() == 0, "A patch is still bare");
			});
			helper.succeed();
		});
	}

	/**
	 * An Avalanche Charge at a foe beyond an ice column stops at the column: he is stunned for {@value
	 * YetiKingEntity#STUN_TICKS} ticks, and meanwhile takes a third more damage.
	 */
	@GameTest(structure = HALL, padding = 8, maxTicks = 160)
	public void aChargeIntoAColumnStunsHim(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos origin = origin(helper);
		YetiKingEntity king = waiting(helper, origin);
		// The north-west column stands at (26.5, 23.5): he charges north at a foe beyond it.
		floor(level, origin, 24, 10, 29, 14);
		ServerPlayer foe = helper.makeMockServerPlayerInLevel();
		foe.setGameMode(GameType.SURVIVAL);
		foe.snapTo(origin.getX() + 26.5, king.floorY(), origin.getZ() + 12.0, 0.0F, 0.0F);
		king.wake(level);
		helper.runAfterDelay(YetiKingEntity.WAKE_TICKS + 2, () -> {
			YetiKingEntity.Phase hunting = king.phase();
			king.teleportTo(origin.getX() + 26.5, king.floorY(), origin.getZ() + 33.0);
			king.begin(level, YetiKingEntity.Attack.AVALANCHE_CHARGE, foe);
			helper.runAfterDelay(YetiKingEntity.Attack.AVALANCHE_CHARGE.windup + 12, () -> {
				ending(level, king, () -> {
					helper.assertTrue(hunting == YetiKingEntity.Phase.HUNT, "He was not hunting: " + hunting);
					helper.assertTrue(king.stunnedTicks() > 0 && king.action() == YetiKingEntity.Action.STUNNED,
							"Charging into the column did not stun him: " + king.action());
					double fromColumn = Math.hypot(king.getX() - origin.getX() - 26.5, king.getZ() - origin.getZ() - 23.5);
					helper.assertTrue(fromColumn > GlacierHall.COLUMN_RADIUS && king.getZ() - origin.getZ() > 23.5,
							"He ran through the column: " + fromColumn + " from it");
					float before = king.getHealth();
					king.hurtServer(level, level.damageSources().indirectMagic(foe, foe), 6.0F);
					float taken = before - king.getHealth();
					helper.assertTrue(Math.abs(taken - 6.0 * YetiKingEntity.STUN_TAKEN) < 0.01, "Stunned, he took " + taken + " of 6, not a third more");
					foe.discard();
				});
				helper.succeed();
			});
		});
	}

	/**
	 * His kin climb out of the dens in the side walls, never more than {@value YetiKingEntity#MAX_WHELPS} at once; left
	 * alone he climbs back onto his throne, healed, and they flee into the snow.
	 */
	@GameTest(structure = HALL, padding = 8, maxTicks = 60)
	public void hisKinComeFromTheDensAndFleeWhenHeIsLeftAlone(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos origin = origin(helper);
		YetiKingEntity king = waiting(helper, origin);
		floor(level, origin, 3, 34, 8, 39);
		floor(level, origin, 71, 34, 76, 39);
		king.wake(level);
		int called = king.callKin(level, YetiKingEntity.KIN);
		int more = king.callKin(level, 6);
		List<UUID> kin = king.whelps();
		List<Vec3> dens = GlacierHall.dens().stream().map(den -> den.add(origin.getX(), origin.getY(), origin.getZ())).toList();
		boolean atDens = kin.stream().allMatch(id -> level.getEntity(id) instanceof YetiWhelpEntity whelp
				&& dens.stream().anyMatch(den -> whelp.position().distanceTo(den) < 1.5));
		king.setHealth(king.getMaxHealth() * 0.5F);
		king.reset(level);
		helper.runAfterDelay(3, () -> {
			ending(level, king, () -> {
				helper.assertTrue(called == YetiKingEntity.KIN, called + " whelps answered his call, not " + YetiKingEntity.KIN);
				helper.assertTrue(more == YetiKingEntity.MAX_WHELPS - YetiKingEntity.KIN && kin.size() == YetiKingEntity.MAX_WHELPS,
						"More than " + YetiKingEntity.MAX_WHELPS + " whelps came at once: " + kin.size());
				helper.assertTrue(atDens, "A whelp did not come out of a den");
				for (UUID id : kin) {
					helper.assertTrue(!(level.getEntity(id) instanceof YetiWhelpEntity whelp) || whelp.isRemoved(), "A whelp did not flee");
				}
				helper.assertTrue(king.phase() == YetiKingEntity.Phase.WAITING && king.getHealth() == king.getMaxHealth(),
						"Left alone he is not back on his throne, healed");
				helper.assertTrue(king.position().distanceTo(king.throne()) < 0.1, "He is not on his throne but at " + king.position());
			});
			helper.succeed();
		});
	}

	/**
	 * His frost: the King's Roar's blizzard chills a player to just short of freezing them through, his Frost Breath
	 * stacks past it; a Yeti Mitten in the offhand keeps its wearer warm, whatever frost they gather.
	 */
	@GameTest(maxTicks = 20)
	public void hisFrostAndTheYetiMitten(GameTestHelper helper) {
		ServerPlayer bare = helper.makeMockServerPlayerInLevel();
		ServerPlayer warm = helper.makeMockServerPlayerInLevel();
		bare.setGameMode(GameType.SURVIVAL);
		warm.setGameMode(GameType.SURVIVAL);
		warm.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(JugcraftYeti.YETI_MITTEN));
		YetiKingEntity.chill(bare, YetiKingEntity.BLIZZARD_FROST, false);
		YetiKingEntity.chill(warm, YetiKingEntity.BLIZZARD_FROST, false);
		int blizzard = bare.getTicksFrozen();
		boolean shortOfFreezing = !bare.isFullyFrozen();
		int warmBlizzard = warm.getTicksFrozen();
		YetiKingEntity.chill(bare, bare.getTicksRequiredToFreeze(), true);
		boolean frozenThrough = bare.isFullyFrozen();
		warm.setTicksFrozen(100);
		helper.runAfterDelay(2, () -> {
			helper.assertTrue(blizzard == YetiKingEntity.BLIZZARD_FROST && shortOfFreezing, "The blizzard did not chill to just short of freezing: " + blizzard);
			helper.assertTrue(frozenThrough, "His breath's frost did not stack past freezing");
			helper.assertTrue(warmBlizzard == 0, "The mitten's wearer was chilled");
			helper.assertTrue(warm.getTicksFrozen() == 0, "The mitten did not keep its wearer warm");
			bare.discard();
			warm.discard();
			helper.succeed();
		});
	}

	/**
	 * A glacial spike bursting up under a player strikes them and throws them up; an icicle shaken from the vault falls
	 * onto its mark and shatters, striking the player standing there.
	 */
	@GameTest(structure = HALL, padding = 8, maxTicks = 140)
	public void aSpikeThrowsAPlayerUpAndAnIcicleShatters(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos origin = origin(helper);
		YetiKingEntity king = waiting(helper, origin);
		floor(level, origin, 32, 34, 42, 39);
		ServerPlayer struck = player(helper, king, 0.5, 0.5);
		ServerPlayer under = player(helper, king, -4.5, 0.5);
		// They wait out their loading in creative, where he pays them no heed; once they can be struck he may wake, but
		// waking he strikes no one until the icicle has long since landed.
		struck.setGameMode(GameType.CREATIVE);
		under.setGameMode(GameType.CREATIVE);
		helper.runAfterDelay(LOADING_TICKS, () -> {
			struck.setGameMode(GameType.SURVIVAL);
			under.setGameMode(GameType.SURVIVAL);
			float before = struck.getHealth();
			GlacialSpikeEntity.burst(level, king, struck.position());
			boolean thrown = struck.getHealth() < before && struck.getDeltaMovement().y > 0.0;
			float underBefore = under.getHealth();
			FallingIcicleEntity icicle = FallingIcicleEntity.drop(level, king, under.position());
			helper.runAfterDelay((int) Math.ceil(FallingIcicleEntity.HEIGHT / FallingIcicleEntity.FALL_SPEED) + 4, () -> {
				ending(level, king, () -> {
					helper.assertTrue(thrown, "The spike did not strike the player and throw them up");
					helper.assertTrue(icicle.isRemoved(), "The icicle did not shatter");
					helper.assertTrue(under.getHealth() < underBefore, "The icicle did not strike the player under it");
					struck.discard();
					under.discard();
				});
				helper.succeed();
			});
		});
	}

	/**
	 * His loot is each participant's own: everyone who struck him rolls for themselves (Yeti Fur, and on a first kill
	 * always one of his trophies) into their own inventory, and a bystander who never struck him gets nothing. Afterwards
	 * Abominable is theirs, and a later roll no longer counts as a first kill.
	 */
	@GameTest(structure = HALL, padding = 8)
	public void hisLootIsEachParticipantsOwn(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos origin = origin(helper);
		YetiKingEntity king = waiting(helper, origin);
		floor(level, origin, 36, 34, 43, 41);
		ServerPlayer first = player(helper, king, 0.5, 3.0);
		ServerPlayer second = player(helper, king, 2.5, 3.0);
		ServerPlayer bystander = player(helper, king, -1.5, 3.0);
		king.took(first, 10.0F);
		king.took(second, 4.0F);
		ending(level, king, () -> {
			List<ServerPlayer> participants = king.participants(level);
			helper.assertTrue(participants.contains(first) && participants.contains(second), "Both who struck him take part");
			helper.assertFalse(participants.contains(bystander), "A bystander who never struck him does not");
			helper.assertTrue(YetiKingLoot.firstKill(first), "A newcomer has not yet won Abominable");
			List<ItemStack> loot = YetiKingLoot.roll(level, king, first);
			int fur = loot.stream().filter(stack -> stack.is(JugcraftYeti.YETI_FUR)).mapToInt(ItemStack::getCount).sum();
			helper.assertTrue(fur >= 4 && fur <= 8, fur + " Yeti Fur, not 4 to 8");
			helper.assertTrue(loot.stream().filter(YetiKingLoot::trophy).count() == 1, "A first kill always brings one of his trophies");
			YetiKingLoot.reward(level, king, participants);
			for (ServerPlayer player : List.of(first, second)) {
				helper.assertTrue(player.getInventory().countItem(JugcraftYeti.YETI_FUR) >= 4, "Each participant's loot went into their own inventory");
				int trophies = player.getInventory().countItem(JugcraftYeti.glacierMaul()) + player.getInventory().countItem(JugcraftYeti.rimeclaw());
				helper.assertTrue(trophies == 1, "Each first kill brought its own trophy, not " + trophies);
				helper.assertFalse(YetiKingLoot.firstKill(player), "Abominable is theirs now");
			}
			helper.assertTrue(bystander.getInventory().countItem(JugcraftYeti.YETI_FUR) == 0, "The bystander got nothing");
			helper.assertTrue(YetiKingLoot.firstKill(bystander), "The bystander has not won Abominable");
			for (ServerPlayer player : List.of(first, second, bystander)) {
				player.discard();
			}
		});
		helper.succeed();
	}
}
