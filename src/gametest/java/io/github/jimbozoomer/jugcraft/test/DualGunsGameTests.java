package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.guns.GunItem;
import io.github.jimbozoomer.jugcraft.guns.GunShots;
import io.github.jimbozoomer.jugcraft.guns.GunSpec;
import io.github.jimbozoomer.jugcraft.guns.JugcraftGuns;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;

/**
 * In-game tests for two guns at once (slice 10G, docs/features/guns.md), worked through the server's own entry points
 * (the ones the trigger and reload payloads call, now with the hand): with a one-handed gun in each hand both fire, each
 * at its own rate and from its own magazine; the other hand's gun fires and reloads only beside a one-handed gun in the
 * main hand; right click aims neither; one gun reloads at a time, a magazine out stopping both and a shot of either
 * cutting a shell-at-a-time reload short. Shooters face south (+z) and aim at their target's middle, three blocks off,
 * near enough that the wider spread of two guns keeps every shot on it.
 */
public class DualGunsGameTests {
	/** The one-handed guns pair: two of them are two guns at once, and no other pair is. */
	@GameTest
	public void oneHandedGunsPair(GameTestHelper helper) {
		helper.assertTrue(JugcraftGuns.SPECS.keySet().containsAll(JugcraftGuns.ONE_HANDED) && JugcraftGuns.ONE_HANDED.size() == 14,
				"Not fourteen one-handed guns, each a gun: " + JugcraftGuns.ONE_HANDED);
		helper.assertTrue(GunItem.oneHanded(loaded("sentry_pistol", 0)) && !GunItem.oneHanded(loaded("garrison_rifle", 0))
				&& !GunItem.oneHanded(new ItemStack(Items.STICK)), "The Sentry Pistol is not one-handed, or the Garrison Rifle or a stick is");
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setItemInHand(InteractionHand.MAIN_HAND, loaded("sentry_pistol", 0));
		player.setItemInHand(InteractionHand.OFF_HAND, loaded("warden_pistol", 0));
		helper.assertTrue(GunItem.dual(player), "A pistol in each hand is not two guns at once");
		player.setItemInHand(InteractionHand.MAIN_HAND, loaded("garrison_rifle", 0));
		helper.assertFalse(GunItem.dual(player), "A rifle and a pistol are two guns at once");
		player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
		helper.assertFalse(GunItem.dual(player), "A pistol in the other hand alone is two guns at once");
		helper.succeed();
	}

	/** With a gun in each hand right click aims neither (it is the other gun's trigger); with one pistol it aims. */
	@GameTest
	public void twoGunsAreNotAimed(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		ItemStack sentry = loaded("sentry_pistol", 8);
		player.setItemInHand(InteractionHand.MAIN_HAND, sentry);
		player.setItemInHand(InteractionHand.OFF_HAND, loaded("warden_pistol", 12));
		helper.assertTrue(sentry.use(level, player, InteractionHand.MAIN_HAND) == InteractionResult.PASS && !player.isUsingItem(),
				"Right click with a gun in each hand aimed the main one");
		helper.assertTrue(player.getOffhandItem().use(level, player, InteractionHand.OFF_HAND) == InteractionResult.PASS
				&& !player.isUsingItem(), "Right click with a gun in each hand aimed the other one");
		player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
		helper.assertTrue(sentry.use(level, player, InteractionHand.MAIN_HAND).consumesAction() && GunItem.aiming(player, sentry),
				"Right click with one pistol did not aim it");
		player.stopUsingItem();
		helper.succeed();
	}

	/**
	 * Both guns fire, each at its own rate: each may bank two shots, and a third in the same tick is refused for each;
	 * every shot lands, and each gun spends its own rounds.
	 */
	@GameTest(structure = GunsGameTests.ARENA, maxTicks = 20)
	public void bothGunsFireEachAtItsRate(GameTestHelper helper) {
		GunsGameTests.floor(helper);
		Mob pig = sturdyPig(helper);
		ServerPlayer shooter = GunsGameTests.shooter(helper, "sentry_pistol", 8, pig, GameType.SURVIVAL);
		ItemStack warden = loaded("warden_pistol", 12);
		shooter.setItemInHand(InteractionHand.OFF_HAND, warden);
		helper.assertTrue(GunShots.fire(shooter) && GunShots.fire(shooter), "The Sentry Pistol's first two shots were refused");
		helper.assertFalse(GunShots.fire(shooter), "The Sentry Pistol fired a third shot in the same tick");
		helper.assertTrue(GunShots.fire(shooter, InteractionHand.OFF_HAND) && GunShots.fire(shooter, InteractionHand.OFF_HAND),
				"The Warden Pistol's first two shots, from the other hand, were refused");
		helper.assertFalse(GunShots.fire(shooter, InteractionHand.OFF_HAND), "The Warden Pistol fired a third shot in the same tick");
		float expected = 2 * spec("sentry_pistol").damage() + 2 * spec("warden_pistol").damage();
		helper.assertTrue(Math.abs(200.0F - pig.getHealth() - expected) < 1.0E-3F,
				"Four shots took " + (200.0F - pig.getHealth()) + " from the pig, not " + expected);
		helper.assertTrue(GunItem.loaded(shooter.getMainHandItem()) == 6 && GunItem.loaded(warden) == 10,
				"Each gun did not spend two rounds of its own: " + GunItem.loaded(shooter.getMainHandItem()) + " and " + GunItem.loaded(warden));
		helper.succeed();
	}

	/**
	 * The other hand's gun fires and reloads only beside a one-handed gun: not beside a rifle, nor alone; beside a pistol
	 * it fires.
	 */
	@GameTest(structure = GunsGameTests.ARENA, maxTicks = 20)
	public void otherHandFiresOnlyBesideAOneHandedGun(GameTestHelper helper) {
		GunsGameTests.floor(helper);
		Mob pig = sturdyPig(helper);
		ServerPlayer shooter = GunsGameTests.shooter(helper, "garrison_rifle", 30, pig, GameType.SURVIVAL);
		ItemStack sentry = loaded("sentry_pistol", 4);
		shooter.setItemInHand(InteractionHand.OFF_HAND, sentry);
		shooter.getInventory().add(new ItemStack(JugcraftGuns.ROUNDS.get("light_round"), 16));
		helper.assertFalse(GunShots.fire(shooter, InteractionHand.OFF_HAND), "The pistol beside a rifle fired");
		helper.assertFalse(GunShots.reload(shooter, InteractionHand.OFF_HAND), "The pistol beside a rifle began to reload");
		shooter.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
		helper.assertFalse(GunShots.fire(shooter, InteractionHand.OFF_HAND), "The pistol in the other hand alone fired");
		shooter.setItemInHand(InteractionHand.MAIN_HAND, loaded("bulldog_pistol", 1));
		helper.assertTrue(GunShots.fire(shooter, InteractionHand.OFF_HAND), "The pistol beside a pistol did not fire");
		helper.assertTrue(Math.abs(200.0F - pig.getHealth() - spec("sentry_pistol").damage()) < 1.0E-3F && GunItem.loaded(sentry) == 3,
				"The pistol beside a pistol did not land one shot and spend its round");
		helper.succeed();
	}

	/**
	 * One gun reloads at a time: while the main gun's magazine is out, the other gun neither fires nor begins its own
	 * reload; once the first is loaded the other reloads in its own time, each from the same rounds.
	 */
	@GameTest(structure = GunsGameTests.ARENA, maxTicks = 140)
	public void oneGunReloadsAtATime(GameTestHelper helper) {
		GunsGameTests.floor(helper);
		Mob pig = sturdyPig(helper);
		ServerPlayer shooter = GunsGameTests.shooter(helper, "sentry_pistol", 0, pig, GameType.SURVIVAL);
		ItemStack warden = loaded("warden_pistol", 1);
		shooter.setItemInHand(InteractionHand.OFF_HAND, warden);
		shooter.getInventory().add(new ItemStack(JugcraftGuns.ROUNDS.get("light_round"), 32));
		GunSpec sentry = spec("sentry_pistol");
		GunSpec wardenSpec = spec("warden_pistol");
		helper.assertTrue(GunShots.reload(shooter), "The Sentry Pistol's reload did not start");
		helper.assertFalse(GunShots.reload(shooter, InteractionHand.OFF_HAND), "The other gun began to reload during the first's");
		helper.assertFalse(GunShots.fire(shooter, InteractionHand.OFF_HAND), "The other gun fired while a magazine was out");
		helper.runAfterDelay(sentry.reload() + 2, () -> {
			helper.assertTrue(GunItem.loaded(shooter.getMainHandItem()) == sentry.capacity(), "The Sentry Pistol is not full after its reload");
			helper.assertTrue(GunItem.loaded(warden) == 1, "The other gun was loaded with the first");
			helper.assertTrue(GunShots.reload(shooter, InteractionHand.OFF_HAND), "The other gun's reload did not start after the first's");
		});
		helper.runAfterDelay(sentry.reload() + wardenSpec.reload() + 6, () -> {
			helper.assertTrue(GunItem.loaded(warden) == wardenSpec.capacity(), "The other gun is not full after its reload");
			int left = GunShots.count(shooter.getInventory(), JugcraftGuns.ROUNDS.get("light_round"));
			helper.assertTrue(left == 32 - sentry.capacity() - (wardenSpec.capacity() - 1), "The two reloads left " + left + " Light Rounds");
			helper.assertFalse(GunShots.reloading(shooter), "Still reloading after both reloads");
			helper.succeed();
		});
	}

	/** A shot of the other gun cuts the main gun's round-at-a-time reload short, as a shot of its own would. */
	@GameTest(structure = GunsGameTests.ARENA, maxTicks = 40)
	public void eitherGunsShotCutsAShellReloadShort(GameTestHelper helper) {
		GunsGameTests.floor(helper);
		Mob pig = sturdyPig(helper);
		ServerPlayer shooter = GunsGameTests.shooter(helper, "marshal_revolver", 2, pig, GameType.SURVIVAL);
		ItemStack sentry = loaded("sentry_pistol", 8);
		shooter.setItemInHand(InteractionHand.OFF_HAND, sentry);
		shooter.getInventory().add(new ItemStack(JugcraftGuns.ROUNDS.get("light_round"), 10));
		helper.assertTrue(spec("marshal_revolver").byShell() && GunShots.reload(shooter),
				"The Marshal Revolver's round-at-a-time reload did not start");
		helper.runAfterDelay(2, () -> {
			helper.assertTrue(GunShots.fire(shooter, InteractionHand.OFF_HAND), "The other gun did not fire during the Marshal's reload");
			helper.assertFalse(GunShots.reloading(shooter), "The other gun's shot did not cut the Marshal's reload short");
			helper.assertTrue(GunItem.loaded(sentry) == 7 && GunItem.loaded(shooter.getMainHandItem()) == 2,
					"The shot did not spend the other gun's round alone");
			helper.succeed();
		});
	}

	/** The other gun's reload stops when there are no longer two guns: the gun is not loaded, and no rounds are taken. */
	@GameTest(structure = GunsGameTests.ARENA, maxTicks = 80)
	public void otherGunsReloadStopsWithoutTwoGuns(GameTestHelper helper) {
		GunsGameTests.floor(helper);
		Mob pig = sturdyPig(helper);
		ServerPlayer shooter = GunsGameTests.shooter(helper, "sentry_pistol", 8, pig, GameType.SURVIVAL);
		ItemStack warden = loaded("warden_pistol", 0);
		shooter.setItemInHand(InteractionHand.OFF_HAND, warden);
		shooter.getInventory().add(new ItemStack(JugcraftGuns.ROUNDS.get("light_round"), 20));
		helper.assertTrue(GunShots.reload(shooter, InteractionHand.OFF_HAND), "The other gun's reload did not start");
		shooter.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STICK));
		helper.runAfterDelay(spec("warden_pistol").reload() + 2, () -> {
			helper.assertFalse(GunShots.reloading(shooter), "The other gun's reload went on with a stick in the main hand");
			helper.assertTrue(GunItem.loaded(warden) == 0
					&& GunShots.count(shooter.getInventory(), JugcraftGuns.ROUNDS.get("light_round")) == 20,
					"The other gun was loaded, or rounds taken, after there were no longer two guns");
			helper.succeed();
		});
	}

	/** An energy pistol in the other hand fires its beam, which lands its damage. */
	@GameTest(structure = GunsGameTests.ARENA, maxTicks = 20)
	public void otherHandsBeamPistolFires(GameTestHelper helper) {
		GunsGameTests.floor(helper);
		Mob pig = sturdyPig(helper);
		ServerPlayer shooter = GunsGameTests.shooter(helper, "sentry_pistol", 8, pig, GameType.SURVIVAL);
		ItemStack beam = loaded("beam_pistol", 2);
		shooter.setItemInHand(InteractionHand.OFF_HAND, beam);
		helper.assertTrue(GunShots.fire(shooter, InteractionHand.OFF_HAND), "The Beam Pistol in the other hand did not fire");
		helper.assertTrue(Math.abs(200.0F - pig.getHealth() - spec("beam_pistol").damage()) < 1.0E-3F && GunItem.loaded(beam) == 1,
				"The Beam Pistol's beam took " + (200.0F - pig.getHealth()) + ", not " + spec("beam_pistol").damage());
		helper.succeed();
	}

	/** A gun with this many rounds loaded. */
	private static ItemStack loaded(String gun, int rounds) {
		ItemStack stack = new ItemStack(JugcraftGuns.GUNS.get(gun));
		GunItem.setLoaded(stack, rounds);
		return stack;
	}

	private static GunSpec spec(String gun) {
		return JugcraftGuns.SPECS.get(gun);
	}

	/** A still pig with 200 health, three blocks in front of the shooter. */
	private static Mob sturdyPig(GameTestHelper helper) {
		Mob pig = GunsGameTests.pig(helper, new BlockPos(1, 2, 4));
		pig.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200.0);
		pig.setHealth(200.0F);
		return pig;
	}
}
