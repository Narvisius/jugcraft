package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.mixin.AttackStrengthAccessor;
import io.github.jimbozoomer.jugcraft.weapons.JugcraftArms;
import io.github.jimbozoomer.jugcraft.weapons.WeaponArts;
import java.util.List;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/**
 * In-game tests for Arms V (batch 48): the weapon arts as the server works them (weapons/WeaponArts), one a move, in a
 * 16-block arena. The wielder is a mock player facing south (+z); the foes are still husks with no armor, so a hit
 * takes exactly its damage off. The mock player does not tick or move by itself, so its attack damage is read back
 * rather than assumed, and where an art moves its wielder the test moves them along the way the client would.
 */
public class ArmsVGameTests {
	private static final String ARENA = "jugcraft-test:arms_arena";
	private static final int CHARGED = 100;
	private static final float HEALTH = 20.0F;

	/** The cyclone strikes every foe within its radius all round, three times, and none beyond it. */
	@GameTest(structure = ARENA, maxTicks = 60, skyAccess = true)
	public void aCycloneStrikesAllRoundThreeTimes(GameTestHelper helper) {
		floor(helper);
		ServerPlayer player = wielder(helper, "steel_twinblade", new BlockPos(8, 2, 8));
		List<Mob> near = List.of(husk(helper, new BlockPos(8, 2, 10)), husk(helper, new BlockPos(8, 2, 6)),
				husk(helper, new BlockPos(10, 2, 8)), husk(helper, new BlockPos(6, 2, 8)));
		Mob far = husk(helper, new BlockPos(8, 2, 13));
		float base = (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE);
		ItemStack stack = player.getMainHandItem();
		helper.assertTrue(WeaponArts.start(player, stack), "The cyclone did not start");
		helper.assertTrue(WeaponArts.busy(player) && player.getCooldowns().isOnCooldown(stack) && slowed(player),
				"The cyclone did not keep its wielder busy and slowed, or put the twinblade on its cooldown");
		helper.assertTrue(!WeaponArts.start(player, stack), "A second cyclone started at once");
		helper.runAfterDelay(JugcraftArms.CYCLONE_FIRST - 1, () -> helper.assertTrue(near.stream().allMatch(foe -> foe.getHealth() == HEALTH),
				"The cyclone struck before its first turn"));
		helper.runAfterDelay(JugcraftArms.ARTS.get("twinblade").ticks() + 2, () -> {
			float expected = base * JugcraftArms.CYCLONE_SHARE * JugcraftArms.CYCLONE_HITS;
			Jugcraft.LOGGER.info("[arms v] cyclone of {} a hit: near {}, far {}; busy {}", base * JugcraftArms.CYCLONE_SHARE,
					near.stream().map(Mob::getHealth).toList(), far.getHealth(), WeaponArts.busy(player));
			for (Mob foe : near) {
				helper.assertTrue(near(HEALTH - foe.getHealth(), expected), "A foe about the cyclone took " + (HEALTH - foe.getHealth())
						+ ", not " + expected);
			}
			helper.assertTrue(far.getHealth() == HEALTH, "The cyclone reached too far");
			helper.assertTrue(!WeaponArts.busy(player) && !slowed(player), "The cyclone never ended, or left its wielder slow");
			helper.succeed();
		});
	}

	/** Iaido: the dash pushes its wielder ahead; the foes it passes are cut only after it, all at once; one aside is not. */
	@GameTest(structure = ARENA, maxTicks = 60, skyAccess = true)
	public void iaidoCutsEveryFoePassedAMomentAfterTheDash(GameTestHelper helper) {
		floor(helper);
		ServerPlayer player = wielder(helper, "steel_nodachi", new BlockPos(8, 2, 2));
		Mob first = husk(helper, new BlockPos(8, 2, 5));
		Mob second = husk(helper, new BlockPos(9, 2, 8));
		Mob aside = husk(helper, new BlockPos(12, 2, 6));
		float base = (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE);
		Vec3 start = player.position();
		helper.assertTrue(WeaponArts.start(player, player.getMainHandItem()), "Iaido did not start");
		int dash = JugcraftArms.IAIDO_START;
		helper.runAfterDelay(dash, () -> helper.assertTrue(player.getDeltaMovement().z > JugcraftArms.IAIDO_SPEED * 0.9,
				"The dash did not push its wielder ahead: " + player.getDeltaMovement()));
		// The way a client would carry the wielder along: IAIDO_SPEED a tick, south.
		for (int i = 1; i <= JugcraftArms.IAIDO_DASH; i++) {
			int step = i;
			helper.runAfterDelay(dash + step - 1, () -> player.setPos(start.x, start.y, start.z + JugcraftArms.IAIDO_SPEED * step));
		}
		int cut = dash + JugcraftArms.IAIDO_DASH + JugcraftArms.IAIDO_DELAY;
		helper.runAfterDelay(cut - 1, () -> helper.assertTrue(first.getHealth() == HEALTH && second.getHealth() == HEALTH,
				"The cut landed before the dash's delay was out"));
		helper.runAfterDelay(cut + 1, () -> {
			float expected = base * JugcraftArms.IAIDO_SHARE;
			Jugcraft.LOGGER.info("[arms v] iaido cut of {}: first {}, second {}, aside {}", expected, first.getHealth(),
					second.getHealth(), aside.getHealth());
			helper.assertTrue(near(HEALTH - first.getHealth(), expected) && near(HEALTH - second.getHealth(), expected),
					"The foes passed took " + (HEALTH - first.getHealth()) + " and " + (HEALTH - second.getHealth()) + ", not " + expected);
			helper.assertTrue(aside.getHealth() == HEALTH, "Iaido cut a foe it never passed");
			helper.succeed();
		});
	}

	/**
	 * The leap: it springs from the platform; where it lands, three blocks lower, a foe close by takes more than one at
	 * the edge (the falloff), both get the drop's bonus and are thrown up, and one beyond the radius is spared.
	 */
	@GameTest(structure = ARENA, maxTicks = 60, skyAccess = true)
	public void aLeapSlamsHardestAtItsCentreAndFromAHeight(GameTestHelper helper) {
		floor(helper);
		for (int x = 3; x <= 5; x++) {
			for (int y = 2; y <= 4; y++) {
				for (int z = 7; z <= 9; z++) {
					helper.setBlock(new BlockPos(x, y, z), Blocks.STONE);
				}
			}
		}
		ServerPlayer player = wielder(helper, "steel_earthbreaker", new BlockPos(4, 5, 8));
		Mob close = husk(helper, new BlockPos(10, 2, 8));
		Mob edge = husk(helper, new BlockPos(12, 2, 8));
		Mob far = husk(helper, new BlockPos(14, 2, 12));
		float base = (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE);
		// Where the husks stand before the slam throws them.
		Vec3 closeAt = close.position();
		Vec3 edgeAt = edge.position();
		helper.assertTrue(WeaponArts.start(player, player.getMainHandItem()), "The leap did not start from the platform");
		helper.assertTrue(near((float) player.getDeltaMovement().y, JugcraftArms.LEAP_UP), "The leap did not spring: " + player.getDeltaMovement());
		Vec3 air = Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(9, 5, 8)));
		Vec3 landing = Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(9, 2, 8)));
		helper.runAfterDelay(1, () -> player.setPos(air.x, air.y, air.z));
		helper.runAfterDelay(3, () -> player.setPos(landing.x, landing.y, landing.z));
		helper.runAfterDelay(JugcraftArms.LEAP_MIN_AIR + 2, () -> {
			double drop = 3.0;
			float closeShare = share(closeAt, landing, drop);
			float edgeShare = share(edgeAt, landing, drop);
			Jugcraft.LOGGER.info("[arms v] leap slam, {} blocks down: close {} (share {}), edge {} (share {}), far {}; close thrown up {}",
					drop, close.getHealth(), closeShare, edge.getHealth(), edgeShare, far.getHealth(), close.getDeltaMovement().y);
			helper.assertTrue(near(HEALTH - close.getHealth(), base * closeShare) && near(HEALTH - edge.getHealth(), base * edgeShare),
					"The slam took " + (HEALTH - close.getHealth()) + " and " + (HEALTH - edge.getHealth()) + ", not " + base * closeShare
							+ " and " + base * edgeShare);
			helper.assertTrue(closeShare > edgeShare && close.getDeltaMovement().y > 0.0, "The slam had no falloff, or threw nothing up");
			helper.assertTrue(far.getHealth() == HEALTH, "The slam reached too far");
			helper.assertTrue(!WeaponArts.active(player), "The leap went on after landing");
			helper.succeed();
		});
	}

	/** Every jab of the flurry lands in full, one on another, then the finish: FLURRY_JABS shares and the finish's. */
	@GameTest(structure = ARENA, maxTicks = 60, skyAccess = true)
	public void aFlurryLandsEveryJab(GameTestHelper helper) {
		floor(helper);
		ServerPlayer player = wielder(helper, "steel_katar", new BlockPos(8, 2, 4));
		Mob foe = husk(helper, new BlockPos(8, 2, 6));
		Mob behind = husk(helper, new BlockPos(8, 2, 1));
		float base = (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE);
		helper.assertTrue(WeaponArts.start(player, player.getMainHandItem()), "The flurry did not start");
		helper.runAfterDelay(JugcraftArms.ARTS.get("katar").ticks() + 2, () -> {
			float expected = base * (JugcraftArms.FLURRY_JABS * JugcraftArms.FLURRY_SHARE + JugcraftArms.FLURRY_FINISH);
			Jugcraft.LOGGER.info("[arms v] flurry: took {} of an expected {}; behind {}", HEALTH - foe.getHealth(), expected,
					behind.getHealth());
			helper.assertTrue(near(HEALTH - foe.getHealth(), expected), "The flurry took " + (HEALTH - foe.getHealth()) + ", not " + expected);
			helper.assertTrue(behind.getHealth() == HEALTH, "The flurry struck behind its wielder");
			helper.succeed();
		});
	}

	/** The crescent runs through two foes in line (the second for less) and is stopped by a wall before a third. */
	@GameTest(structure = ARENA, maxTicks = 60, skyAccess = true)
	public void aCrescentRunsThroughFoesUntilAWall(GameTestHelper helper) {
		floor(helper);
		for (int x = 4; x <= 12; x++) {
			for (int y = 2; y <= 4; y++) {
				helper.setBlock(new BlockPos(x, y, 11), Blocks.STONE);
			}
		}
		ServerPlayer player = wielder(helper, "steel_moonblade", new BlockPos(8, 2, 1));
		Mob first = husk(helper, new BlockPos(8, 2, 4));
		Mob second = husk(helper, new BlockPos(8, 2, 8));
		Mob aside = husk(helper, new BlockPos(13, 2, 5));
		Mob walled = husk(helper, new BlockPos(8, 2, 13));
		float base = (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE);
		helper.assertTrue(WeaponArts.start(player, player.getMainHandItem()), "The crescent did not start");
		helper.runAfterDelay(JugcraftArms.CRESCENT_RELEASE + JugcraftArms.CRESCENT_TICKS + 2, () -> {
			float one = base * JugcraftArms.CRESCENT_SHARE;
			float two = one * (1.0F - JugcraftArms.CRESCENT_FADE);
			Jugcraft.LOGGER.info("[arms v] crescent: first {}, second {} (expected {} and {}), aside {}, behind the wall {}",
					HEALTH - first.getHealth(), HEALTH - second.getHealth(), one, two, aside.getHealth(), walled.getHealth());
			helper.assertTrue(near(HEALTH - first.getHealth(), one) && near(HEALTH - second.getHealth(), two),
					"The wave took " + (HEALTH - first.getHealth()) + " and " + (HEALTH - second.getHealth()));
			helper.assertTrue(aside.getHealth() == HEALTH && walled.getHealth() == HEALTH, "The wave struck aside or through the wall");
			helper.succeed();
		});
	}

	/**
	 * The chain catches only the first foe in line: one seven blocks off is struck, hauled in within the sickle's reach
	 * and reaped; one close by is struck and reaped where it stands. The foe behind the close one is spared.
	 */
	@GameTest(structure = ARENA, maxTicks = 60, skyAccess = true)
	public void aChainLashHaulsInTheFirstFoeAndReapsIt(GameTestHelper helper) {
		floor(helper);
		ServerPlayer thrower = wielder(helper, "steel_kusarigama", new BlockPos(11, 2, 1));
		Mob distant = husk(helper, new BlockPos(11, 2, 8));
		ServerPlayer reaper = wielder(helper, "steel_kusarigama", new BlockPos(4, 2, 1));
		Mob close = husk(helper, new BlockPos(4, 2, 3));
		Mob shielded = husk(helper, new BlockPos(4, 2, 6));
		float base = (float) thrower.getAttributeValue(Attributes.ATTACK_DAMAGE);
		double startZ = distant.getZ();
		helper.assertTrue(WeaponArts.start(thrower, thrower.getMainHandItem()) && WeaponArts.start(reaper, reaper.getMainHandItem()),
				"The chain lashes did not start");
		helper.runAfterDelay(JugcraftArms.LASH_THROW, () -> {
			Vec3 pull = distant.getDeltaMovement();
			Jugcraft.LOGGER.info("[arms v] chain lash: the distant husk took {}, pulled at {}", HEALTH - distant.getHealth(), pull);
			helper.assertTrue(near(HEALTH - distant.getHealth(), base * JugcraftArms.LASH_SHARE), "The lash took " + (HEALTH - distant.getHealth()));
			helper.assertTrue(pull.z < -0.3, "The chain did not haul the foe in: " + pull);
		});
		helper.runAfterDelay(JugcraftArms.ARTS.get("kusarigama").ticks() + 2, () -> {
			float both = base * (JugcraftArms.LASH_SHARE + JugcraftArms.LASH_REAP_SHARE);
			double hauled = startZ - distant.getZ();
			Jugcraft.LOGGER.info("[arms v] chain lash and reap: close {}, behind it {}, distant {} (hauled {} blocks in)",
					HEALTH - close.getHealth(), shielded.getHealth(), HEALTH - distant.getHealth(), hauled);
			helper.assertTrue(near(HEALTH - close.getHealth(), both), "The close foe took " + (HEALTH - close.getHealth()));
			helper.assertTrue(near(HEALTH - distant.getHealth(), both) && hauled > 1.0,
					"The distant foe took " + (HEALTH - distant.getHealth()) + ", hauled " + hauled + " blocks in");
			helper.assertTrue(shielded.getHealth() == HEALTH, "The chain went through the first foe");
			helper.succeed();
		});
	}

	/**
	 * The rules round an art: a two-handed arm's needs a free off hand (a one-handed one's does not); the leap needs
	 * the ground; while busy the wielder's plain hit is refused; once free again plain hits go through although the
	 * art is still on its cooldown.
	 */
	@GameTest(structure = ARENA, maxTicks = 60, skyAccess = true)
	public void anArtKeepsItsRules(GameTestHelper helper) {
		floor(helper);
		ServerPlayer blade = wielder(helper, "bronze_moonblade", new BlockPos(2, 2, 2));
		blade.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.SHIELD));
		boolean shielded = WeaponArts.start(blade, blade.getMainHandItem());
		ServerPlayer hammer = wielder(helper, "bronze_earthbreaker", new BlockPos(13, 5, 2));
		boolean midAir = WeaponArts.start(hammer, hammer.getMainHandItem());
		ServerPlayer fist = wielder(helper, "bronze_katar", new BlockPos(8, 2, 8));
		fist.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.SHIELD));
		Mob foe = husk(helper, new BlockPos(8, 2, 10));
		boolean katar = WeaponArts.start(fist, fist.getMainHandItem());
		InteractionResult during = AttackEntityCallback.EVENT.invoker().interact(fist, helper.getLevel(), InteractionHand.MAIN_HAND, foe, null);
		Jugcraft.LOGGER.info("[arms v] rules: moonblade with a shield {}, leap from mid-air {}, katar with a shield {}, plain hit while busy {}",
				shielded, midAir, katar, during);
		helper.assertTrue(!shielded && !midAir && katar, "An art started where it should not, or not where it should");
		helper.assertTrue(during == InteractionResult.FAIL, "A plain hit went through while busy with an art: " + during);
		helper.runAfterDelay(JugcraftArms.ARTS.get("katar").ticks() + 2, () -> {
			InteractionResult after = AttackEntityCallback.EVENT.invoker().interact(fist, helper.getLevel(), InteractionHand.MAIN_HAND, foe, null);
			boolean cooling = fist.getCooldowns().isOnCooldown(fist.getMainHandItem());
			Jugcraft.LOGGER.info("[arms v] rules: plain hit after the art {}, art on cooldown {}", after, cooling);
			helper.assertTrue(after == InteractionResult.PASS && cooling, "Plain hits were held back by the art's cooldown, or it had none");
			helper.succeed();
		});
	}

	/** The slam's share for a foe standing at `at`: LEAP_SHARE falling off towards the edge, plus LEAP_PER_BLOCK a block of drop. */
	private static float share(Vec3 at, Vec3 landing, double drop) {
		double distance = Math.sqrt(Math.pow(at.x - landing.x, 2) + Math.pow(at.z - landing.z, 2));
		double falloff = 1.0 - (1.0 - JugcraftArms.LEAP_EDGE) * Math.min(1.0, distance / JugcraftArms.LEAP_RADIUS);
		return (float) (JugcraftArms.LEAP_SHARE * falloff + JugcraftArms.LEAP_PER_BLOCK * drop);
	}

	/** A mock player facing south (+z) standing at pos, in survival, the arm in hand and a full attack charge. */
	private static ServerPlayer wielder(GameTestHelper helper, String arm, BlockPos pos) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setGameMode(GameType.SURVIVAL);
		BlockPos at = helper.absolutePos(pos);
		player.setPos(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
		player.setYRot(0.0F);
		player.setXRot(0.0F);
		player.setYHeadRot(0.0F);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(JugcraftArms.ITEMS.get(arm)));
		((AttackStrengthAccessor) player).jugcraft$setAttackStrengthTicker(CHARGED);
		return player;
	}

	private static boolean slowed(ServerPlayer player) {
		return player.getAttribute(Attributes.MOVEMENT_SPEED).hasModifier(WeaponArts.SLOW);
	}

	private static boolean near(float a, float b) {
		return Math.abs(a - b) < 1.0E-3F;
	}

	private static void floor(GameTestHelper helper) {
		for (int x = 0; x <= 15; x++) {
			for (int z = 0; z <= 15; z++) {
				helper.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
			}
		}
	}

	/** A still husk with no armor. */
	private static Mob husk(GameTestHelper helper, BlockPos pos) {
		@SuppressWarnings("unchecked")
		EntityType<Mob> type = (EntityType<Mob>) BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.withDefaultNamespace("husk"));
		Mob husk = helper.spawnWithNoFreeWill(type, pos);
		husk.getAttribute(Attributes.ARMOR).setBaseValue(0.0);
		return husk;
	}
}
