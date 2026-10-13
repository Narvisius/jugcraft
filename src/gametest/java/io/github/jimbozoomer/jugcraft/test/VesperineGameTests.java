package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.lair.vesperine.GraveThrallEntity;
import io.github.jimbozoomer.jugcraft.lair.vesperine.GriefBoltEntity;
import io.github.jimbozoomer.jugcraft.lair.vesperine.JugcraftVesperine;
import io.github.jimbozoomer.jugcraft.lair.vesperine.ReaperSkullEntity;
import io.github.jimbozoomer.jugcraft.lair.vesperine.VesperineEntity;
import io.github.jimbozoomer.jugcraft.lair.vesperine.VesperineLoot;
import io.github.jimbozoomer.jugcraft.weapons.HarvestBoon;
import java.util.List;
import java.util.UUID;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/**
 * Vesperine, the Last Reaper (docs/features/vesperine.md), as far as a game-test server can show her: one put down on
 * her own (with no lair instance) sits until a player's blow wakes her, her skulls guard her, her arc strikes in front
 * and not behind, a struck Grief Bolt turns, her servants go with her, she goes back to her seat when left alone, her
 * loot is each participant's own, and the Vesper Scythe's Harvest heals on a kill. Her fight in the Hollow Acre itself
 * (the wards, the moon, the exit) runs in a real world, in {@link VesperineClientGameTests}.
 *
 * <p>All the game tests share one world, and she fights any player within 40 blocks of her arena. So each test sends
 * its reaper away before it ends, and a test that lets her act on her own is {@link #lifted} high above the others,
 * each to its own height, where she sees only its own players and no other reaper sees them.
 */
public class VesperineGameTests {
	private static final BlockPos SEAT = new BlockPos(4, 2, 4);

	private static VesperineEntity seated(GameTestHelper helper) {
		return seated(helper, helper.absolutePos(BlockPos.ZERO));
	}

	/** She waits seated at {@link #SEAT} from {@code origin}, her arena round her seat. */
	private static VesperineEntity seated(GameTestHelper helper, BlockPos origin) {
		Vec3 at = Vec3.atBottomCenterOf(origin.offset(SEAT));
		return VesperineEntity.summon(helper.getLevel(), at, 0.0F, at, null);
	}

	private static ServerPlayer player(GameTestHelper helper, double x, double z) {
		return player(helper, helper.absolutePos(BlockPos.ZERO), x, z);
	}

	/** A survival player standing {@code x}, {@code z} from {@code origin}, on its floor. */
	private static ServerPlayer player(GameTestHelper helper, BlockPos origin, double x, double z) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setGameMode(GameType.SURVIVAL);
		Vec3 at = Vec3.atBottomCenterOf(origin.offset(0, 2, 0)).add(x, 0.0, z);
		player.snapTo(at.x, at.y, at.z, 0.0F, 0.0F);
		return player;
	}

	/**
	 * Where a test that lets her act on her own stands: {@code height} blocks above its structure, on a stone floor (for
	 * her thralls), out of her sight of every other test's players. Each such test has its own height, at least 50
	 * blocks from the next and from the tests below.
	 */
	private static BlockPos lifted(GameTestHelper helper, int height) {
		ServerLevel level = helper.getLevel();
		BlockPos origin = helper.absolutePos(BlockPos.ZERO).above(height);
		for (int x = -4; x <= 14; x++) {
			for (int z = -4; z <= 14; z++) {
				level.setBlockAndUpdate(origin.offset(x, 1, z), Blocks.STONE.defaultBlockState());
			}
		}
		return origin;
	}

	/**
	 * A new player cannot be hurt until its client has loaded the world, or for 60 ticks; a mock player has no client,
	 * so a test that hurts one waits this long first.
	 */
	private static final int LOADING_TICKS = 80;

	/** Checks {@code body}, then sends her away whether it passed or failed, so a failed test leaves no reaper fighting on. */
	private static void ending(ServerLevel level, VesperineEntity vesperine, Runnable body) {
		try {
			body.run();
		} finally {
			dismiss(level, vesperine);
		}
	}

	/** Sends her away, so she fights no other test's players; her skulls and thralls follow. */
	private static void dismiss(ServerLevel level, VesperineEntity vesperine) {
		for (ReaperSkullEntity skull : vesperine.skulls(level)) {
			skull.discard();
		}
		for (UUID id : vesperine.thralls()) {
			if (level.getEntity(id) instanceof GraveThrallEntity thrall) {
				thrall.discard();
			}
		}
		vesperine.discard();
	}

	private static Item item(String id) {
		return BuiltInRegistries.ITEM.getValue(Jugcraft.id(id));
	}

	/** Seated, she takes no harm; a player's blow wakes her, her bar shows, and both skulls are at her shoulders. */
	@GameTest
	public void aBlowWakesHerAndDoesNoHarm(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		VesperineEntity vesperine = seated(helper);
		ServerPlayer player = player(helper, 4.5, 7.5);
		ending(level, vesperine, () -> {
			helper.assertTrue(vesperine.phase() == VesperineEntity.Phase.SEATED, "She waits seated");
			helper.assertTrue(vesperine.skullsAlive(level) == 2, "Dirge and Requiem are with her");
			helper.assertFalse(vesperine.hurtServer(level, level.damageSources().playerAttack(player), 10.0F), "A seated reaper takes no harm");
			// Waking sets her health for the players near her arena, so it may grow; the blow takes none of it.
			helper.assertTrue(vesperine.getHealth() == vesperine.getMaxHealth(), "She rises at full health");
			helper.assertTrue(vesperine.phase() == VesperineEntity.Phase.RISING, "The blow wakes her: she rises");
			for (ReaperSkullEntity skull : vesperine.skulls(level)) {
				helper.assertTrue(skull.position().distanceTo(vesperine.shoulder(skull.left())) < 0.5, "A skull floats at her shoulder");
			}
		});
		helper.succeed();
	}

	/** While both skulls live she takes half a blow; with one, all of it; her health grows with the party, within bounds. */
	@GameTest(maxTicks = 40)
	public void herSkullsGuardHer(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		VesperineEntity vesperine = seated(helper, lifted(helper, 60));
		boolean halved = vesperine.guards() == 2 && vesperine.taken(10.0F) == 5.0F;
		vesperine.skulls(level).getFirst().discard();
		helper.runAfterDelay(3, () -> {
			ending(level, vesperine, () -> {
				helper.assertTrue(halved, "Both skulls: half damage");
				helper.assertTrue(VesperineEntity.partyScale(1) == 1.0 && VesperineEntity.partyScale(2) == 1.5
						&& VesperineEntity.partyScale(4) == 2.5 && VesperineEntity.partyScale(9) == 2.5, "Her health scales with the party, at most 2.5 times");
				helper.assertTrue(VesperineEntity.slamDamage(0.0) == 16.0F && VesperineEntity.slamDamage(5.0) == 8.0F
						&& VesperineEntity.slamDamage(10.0) == 0.0F, "The slam falls off to nothing at 10 blocks");
				helper.assertTrue(vesperine.guards() == 1, "One skull left");
				helper.assertTrue(vesperine.taken(10.0F) == 10.0F, "One skull: she takes the whole blow");
			});
			helper.succeed();
		});
	}

	/**
	 * The Reaping Arc strikes a player in front of her (14 damage and Wither) and not one directly behind. The two wait
	 * out their loading in creative, where she pays them no heed, before she wakes.
	 */
	@GameTest(maxTicks = 180)
	public void theReapingArcStrikesInFrontNotBehind(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos origin = lifted(helper, 110);
		VesperineEntity vesperine = seated(helper, origin);
		ServerPlayer front = player(helper, origin, 4.5, 7.0);
		ServerPlayer behind = player(helper, origin, 4.5, 2.0);
		front.setGameMode(GameType.CREATIVE);
		behind.setGameMode(GameType.CREATIVE);
		helper.runAfterDelay(LOADING_TICKS, () -> {
			front.setGameMode(GameType.SURVIVAL);
			behind.setGameMode(GameType.SURVIVAL);
			vesperine.wake(level);
			helper.runAfterDelay(VesperineEntity.RISE_TICKS + 2, () -> {
				ending(level, vesperine, () -> {
					vesperine.setYRot(0.0F);  // facing south, towards the one in front
					int hit = vesperine.reapingArc(level);
					helper.assertTrue(hit == 1, "The arc struck " + hit + " players, not 1");
					helper.assertTrue(front.getHealth() < front.getMaxHealth() && front.hasEffect(MobEffects.WITHER),
							"The one in front is reaped and withers");
					helper.assertTrue(behind.getHealth() == behind.getMaxHealth(), "The one behind her is spared");
				});
				helper.succeed();
			});
		});
	}

	/** A Grief Bolt struck by a player turns back towards its skull; a turned bolt cannot be struck again. */
	@GameTest
	public void aStruckGriefBoltTurns(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		VesperineEntity vesperine = seated(helper);
		ServerPlayer player = player(helper, 4.5, 7.5);
		ReaperSkullEntity skull = vesperine.skulls(level).getFirst();
		GriefBoltEntity bolt = GriefBoltEntity.loose(level, skull, player);
		ending(level, vesperine, () -> {
			helper.assertFalse(bolt.reflected(), "A loosed bolt flies at the player");
			helper.assertTrue(bolt.hurtServer(level, level.damageSources().playerAttack(player), 1.0F), "A player's blow turns it");
			helper.assertTrue(bolt.reflected(), "It flies back");
			helper.assertFalse(bolt.hurtServer(level, level.damageSources().playerAttack(player), 1.0F), "It turns only once");
			bolt.discard();
		});
		helper.succeed();
	}

	/** Her Grave Call brings up three thralls; when she falls they crumble, and her skulls go with her. */
	@GameTest(maxTicks = 160)
	public void herServantsGoWithHer(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos origin = lifted(helper, 160);
		VesperineEntity vesperine = seated(helper, origin);
		ServerPlayer player = player(helper, origin, 4.5, 7.5);
		player.addEffect(new net.minecraft.world.effect.MobEffectInstance(MobEffects.RESISTANCE, 400, 4));
		vesperine.wake(level);
		helper.runAfterDelay(VesperineEntity.RISE_TICKS + 2, () -> {
			vesperine.begin(level, VesperineEntity.Attack.GRAVE_CALL, player);
			helper.runAfterDelay(VesperineEntity.Attack.GRAVE_CALL.windup + 4, () -> {
				List<UUID> thralls = vesperine.thralls();
				List<ReaperSkullEntity> skulls = vesperine.skulls(level);
				if (thralls.size() != VesperineEntity.CALL_THRALLS) {
					dismiss(level, vesperine);
				}
				helper.assertTrue(thralls.size() == VesperineEntity.CALL_THRALLS, thralls.size() + " thralls rose, not " + VesperineEntity.CALL_THRALLS);
				vesperine.hurtServer(level, level.damageSources().genericKill(), Float.MAX_VALUE);
				helper.runAfterDelay(3, () -> {
					try {
						for (UUID id : thralls) {
							helper.assertTrue(!(level.getEntity(id) instanceof GraveThrallEntity thrall) || !thrall.isAlive(), "A thrall outlived her");
						}
						for (ReaperSkullEntity skull : skulls) {
							helper.assertTrue(skull.isRemoved() || !skull.isAlive(), "A skull outlived her");
						}
					} finally {
						// Checked first, so this sends away only what wrongly outlived her.
						for (UUID id : thralls) {
							if (level.getEntity(id) instanceof GraveThrallEntity thrall) {
								thrall.discard();
							}
						}
						skulls.forEach(ReaperSkullEntity::discard);
					}
					helper.succeed();
				});
			});
		});
	}

	/** Left alone after she rises, she goes back to her seat, healed. */
	@GameTest(maxTicks = 320)
	public void leftAloneSheReturnsToHerSeat(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		VesperineEntity vesperine = seated(helper, lifted(helper, 210));
		Vec3 seat = vesperine.position();
		vesperine.wake(level);
		helper.runAfterDelay(VesperineEntity.RISE_TICKS + 2, () -> {
			vesperine.setHealth(vesperine.getMaxHealth() * 0.6F);
			helper.runAfterDelay(VesperineEntity.ABANDON_TICKS + 10, () -> {
				ending(level, vesperine, () -> {
					helper.assertTrue(vesperine.phase() == VesperineEntity.Phase.SEATED, "With nobody to fight she sits again, not " + vesperine.phase());
					helper.assertTrue(vesperine.getHealth() == vesperine.getMaxHealth(), "She is healed");
					helper.assertTrue(vesperine.position().distanceTo(seat) < 0.5, "She is back on her seat");
				});
				helper.succeed();
			});
		});
	}

	/**
	 * Her loot is each participant's own: everyone who struck her rolls for themselves (Reaper's Shade, and on a first
	 * kill always the Vesper Scythe) into their own inventory, and a bystander who never struck her gets nothing.
	 * Afterwards The Last Harvest is theirs, and a later roll no longer counts as a first kill.
	 */
	@GameTest
	public void herLootIsEachParticipantsOwn(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		VesperineEntity vesperine = seated(helper);
		ServerPlayer first = player(helper, 4.5, 7.5);
		ServerPlayer second = player(helper, 6.5, 7.5);
		ServerPlayer bystander = player(helper, 2.5, 7.5);
		vesperine.took(first, 10.0F);
		vesperine.took(second, 4.0F);
		ending(level, vesperine, () -> {
			List<ServerPlayer> participants = vesperine.participants(level);
			helper.assertTrue(participants.contains(first) && participants.contains(second), "Both who struck her take part");
			helper.assertFalse(participants.contains(bystander), "A bystander who never struck her does not");
			helper.assertTrue(VesperineLoot.firstKill(first), "A newcomer has not yet won The Last Harvest");
			List<ItemStack> loot = VesperineLoot.roll(level, vesperine, first);
			int shades = loot.stream().filter(stack -> stack.is(JugcraftVesperine.REAPER_SHADE)).mapToInt(ItemStack::getCount).sum();
			helper.assertTrue(shades >= 3 && shades <= 6, shades + " Reaper's Shade, not 3 to 6");
			helper.assertTrue(loot.stream().anyMatch(stack -> stack.is(item("vesper_scythe"))), "A first kill always brings the Vesper Scythe");
			VesperineLoot.reward(level, vesperine, participants);
			for (ServerPlayer player : List.of(first, second)) {
				helper.assertTrue(player.getInventory().countItem(JugcraftVesperine.REAPER_SHADE) >= 3, "Each participant's loot went into their own inventory");
				helper.assertTrue(player.getInventory().countItem(item("vesper_scythe")) == 1, "Each first kill brought its own Vesper Scythe");
				helper.assertFalse(VesperineLoot.firstKill(player), "The Last Harvest is theirs now");
			}
			helper.assertTrue(bystander.getInventory().countItem(JugcraftVesperine.REAPER_SHADE) == 0, "The bystander got nothing");
			helper.assertTrue(VesperineLoot.firstKill(bystander), "The bystander has not won The Last Harvest");
		});
		helper.succeed();
	}

	/** The Vesper Scythe's Harvest: a kill with it heals its wielder two hearts. */
	@GameTest
	public void aKillWithTheVesperScytheHeals(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer player = player(helper, 4.5, 4.5);
		ItemStack scythe = new ItemStack(item("vesper_scythe"));
		player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, scythe);
		player.setHealth(10.0F);
		Mob zombie = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, new BlockPos(5, 2, 4));
		zombie.hurtServer(level, level.damageSources().playerAttack(player), 1000.0F);
		scythe.getItem().hurtEnemy(scythe, zombie, player);
		helper.assertTrue(player.getHealth() == 10.0F + HarvestBoon.HEAL, "A kill heals " + HarvestBoon.HEAL + ", health is " + player.getHealth());
		helper.assertFalse(HarvestBoon.charged(player), "One kill does not yet charge the crescent");
		helper.succeed();
	}
}
