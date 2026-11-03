package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.guns.GunItem;
import io.github.jimbozoomer.jugcraft.guns.GunShots;
import io.github.jimbozoomer.jugcraft.guns.GunSpec;
import io.github.jimbozoomer.jugcraft.guns.JugcraftGuns;
import io.github.jimbozoomer.jugcraft.guns.Microjet;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * In-game tests for slice 16B's guns (docs/features/guns.md), three more of the owner's guns from their 23 October upload,
 * made of iron, through the server's entry points as {@link IronGunsGameTests} does: the Comet Pistol, held in one hand, and the
 * Narwhal Lance, carried at the hip and loaded one at a time, both firing Microjets that fly; and the Halibut Gun, a
 * grapeshot gun loaded a shell at a time that throws back what it hits and its shooter. Shooters face south (+z).
 */
public class IronGunsIIGameTests {
	/** The slice's guns, in the order the creative tab shows them. */
	static final List<String> GUNS = List.of("comet_pistol", "narwhal_lance", "halibut_gun");

	/**
	 * Each is registered with its numbers and its recipe loads, a pull at a time, none setting alight, lifting, bleeding
	 * or piercing armour. The Comet Pistol is six Microjets from a magazine flying 2.5 blocks a tick, held in one hand (not a
	 * carbine with a stock), taking only the magazines. The Narwhal Lance is one Microjet at a time flying 3 blocks a tick,
	 * held in both hands, taking nothing. Neither knocks back further nor pushes its shooter. The Halibut Gun is four
	 * buckshot shells of eight pellets loaded a shell at a time, as wide aimed as from the hip, knocking back 0.65 further
	 * and pushing its shooter back 0.45, held in both hands and taking the stocks, grips and bayonets but no magazine,
	 * barrel attachment or scope.
	 */
	@GameTest
	public void ironGunsIIAreRegistered(GameTestHelper helper) {
		for (String name : GUNS) {
			GunSpec spec = JugcraftGuns.SPECS.get(name);
			GunItem gun = JugcraftGuns.GUNS.get(name);
			helper.assertTrue(spec != null && gun != null && gun.spec() == spec, name + " is not registered with its numbers");
			helper.assertTrue(!JugcraftGuns.ignites(gun) && !JugcraftGuns.levitates(gun) && JugcraftGuns.bleed(gun) == null
					&& JugcraftGuns.piercing(gun) == 0.0F, name + " sets alight, lifts, bleeds or pierces");
			helper.assertTrue(!spec.auto(), name + " fires as long as the trigger is held");
			helper.assertTrue(helper.getLevel().recipeAccess().byKey(ResourceKey.create(Registries.RECIPE, Jugcraft.id(name))).isPresent(),
					"The " + name + " recipe does not load");
		}
		for (String name : List.of("comet_pistol", "narwhal_lance")) {
			GunItem gun = JugcraftGuns.GUNS.get(name);
			helper.assertTrue(JugcraftGuns.shot(gun).equals(JugcraftGuns.JET) && JugcraftGuns.SPECS.get(name).ammo().equals("microjet")
					&& JugcraftGuns.SPECS.get(name).pellets() == 1, "The " + name + " does not fire Microjets that fly, one at a time");
			helper.assertTrue(JugcraftGuns.knockback(gun) == 0.0F && JugcraftGuns.kick(gun) == 0.0F,
					"The " + name + " knocks back further, or pushes its shooter");
		}
		GunSpec comet = JugcraftGuns.SPECS.get("comet_pistol");
		helper.assertTrue(comet.capacity() == 6 && !comet.byShell() && JugcraftGuns.jetSpeed(JugcraftGuns.GUNS.get("comet_pistol")) == 2.5F,
				"The Comet Pistol is not a magazine of six Microjets flying 2.5 blocks a tick");
		helper.assertTrue(JugcraftGuns.ONE_HANDED.contains("comet_pistol") && !JugcraftGuns.CARBINES.contains("comet_pistol"),
				"The Comet Pistol is not held in one hand");
		helper.assertTrue(JugcraftGuns.ACCEPTS.get("comet_pistol").equals(List.of("extended_magazine", "speed_magazine")),
				"The Comet Pistol takes " + JugcraftGuns.ACCEPTS.get("comet_pistol"));
		GunSpec narwhal = JugcraftGuns.SPECS.get("narwhal_lance");
		helper.assertTrue(narwhal.capacity() == 1 && !narwhal.byShell() && JugcraftGuns.jetSpeed(JugcraftGuns.GUNS.get("narwhal_lance")) == 3.0F,
				"The Narwhal Lance is not one Microjet at a time flying 3 blocks a tick");
		helper.assertTrue(!JugcraftGuns.ONE_HANDED.contains("narwhal_lance") && !JugcraftGuns.ACCEPTS.containsKey("narwhal_lance"),
				"The Narwhal Lance is held in one hand, or takes an attachment");
		GunItem halibutGun = JugcraftGuns.GUNS.get("halibut_gun");
		GunSpec halibut = JugcraftGuns.SPECS.get("halibut_gun");
		helper.assertTrue(JugcraftGuns.shot(halibutGun).equals("bullet") && halibut.ammo().equals("buckshot_shell") && halibut.pellets() == 8
				&& halibut.capacity() == 4 && halibut.byShell() && halibut.aimSpread() == halibut.hipSpread(),
				"The Halibut Gun is not four shells of eight pellets loaded a shell at a time, as wide aimed as from the hip");
		helper.assertTrue(JugcraftGuns.knockback(halibutGun) == 0.65F && JugcraftGuns.kick(halibutGun) == 0.45F,
				"The Halibut Gun does not knock back 0.65 further and push its shooter back 0.45");
		List<String> halibutTakes = JugcraftGuns.ACCEPTS.get("halibut_gun");
		helper.assertTrue(halibutTakes.containsAll(BrassSidearmsGameTests.STOCKS) && halibutTakes.containsAll(FrontierGunsIVGameTests.BAYONETS)
				&& halibutTakes.containsAll(List.of("light_grip", "vertical_grip")) && !halibutTakes.contains("extended_magazine")
				&& halibutTakes.stream().noneMatch(BrassSidearmsGameTests.BARREL::contains)
				&& halibutTakes.stream().noneMatch(BrassSidearmsGameTests.SIGHTS::contains), "The Halibut Gun takes " + halibutTakes);
		helper.assertTrue(!JugcraftGuns.ONE_HANDED.contains("halibut_gun"), "The Halibut Gun is held in one hand");
		helper.succeed();
	}

	/**
	 * A Comet Pistol fired at a pig seven blocks off: at once there is one Microjet, the gun's, and the pig is unhurt. A
	 * tick later it has flown about 2.5 blocks; by the eighth tick it has dealt the gun's damage and gone, and a round is
	 * spent. With a Warden Pistol in the other hand it is one of two guns at once. A wall behind the pig stops a stray one.
	 */
	@GameTest(structure = GunsGameTests.ARENA, maxTicks = 40)
	public void cometPistolFiresAFlyingMicrojetFromOneHand(GameTestHelper helper) {
		GunsGameTests.floor(helper);
		wall(helper, 10);
		Mob pig = NetherGunsGameTests.sturdyPig(helper, new BlockPos(1, 2, 8));
		GunSpec spec = JugcraftGuns.SPECS.get("comet_pistol");
		ServerPlayer shooter = GunsGameTests.shooter(helper, "comet_pistol", 6, pig, GameType.SURVIVAL);
		helper.assertTrue(GunShots.fire(shooter), "The loaded Comet Pistol did not fire");
		helper.assertTrue(GunItem.loaded(shooter.getMainHandItem()) == 5, "The shot did not spend one of six rounds");
		Microjet jet = onlyJet(helper, shooter, pig, "comet_pistol");
		double launchSpeed = jet.getDeltaMovement().length();
		// Vanilla applies air drag before moving; the Microjet restores its launch velocity afterward.
		double expectedStep = launchSpeed * 0.99F;
		Vec3[] seen = new Vec3[1];
		helper.runAfterDelay(1, () -> seen[0] = jet.position());
		helper.runAfterDelay(2, () -> {
			double step = jet.position().distanceTo(seen[0]);
			helper.assertTrue(!jet.isRemoved() && Math.abs(step - expectedStep) < 1.0E-6
					&& Math.abs(jet.getDeltaMovement().length() - launchSpeed) < 1.0E-6,
					"The Comet Pistol's Microjet flew " + step + " blocks in a tick, not its drag-adjusted launch speed " + expectedStep + ", or it is gone");
		});
		helper.runAfterDelay(8, () -> {
			helper.assertTrue(jet.isRemoved(), "The Comet Pistol's Microjet flew on past the pig");
			helper.assertTrue(Math.abs(200.0F - pig.getHealth() - spec.damage()) < 1.0E-3F,
					"The Comet Pistol's Microjet took " + (200.0F - pig.getHealth()) + ", not " + spec.damage());
			shooter.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(JugcraftGuns.GUNS.get("warden_pistol")));
			helper.assertTrue(GunItem.dual(shooter), "The Comet Pistol and a Warden Pistol are not two guns at once");
			helper.succeed();
		});
	}

	/**
	 * A Narwhal Lance fired at a pig seven blocks off sends one Microjet, the gun's, which flies about 3 blocks a tick and
	 * has dealt the gun's damage by the sixth tick; the Lance is then empty and does not fire. Given two Microjets it loads
	 * one at its reload's time, not before, leaving the other, and fires again.
	 */
	@GameTest(structure = GunsGameTests.ARENA, maxTicks = 120)
	public void narwhalLanceFiresOneMicrojetAtATime(GameTestHelper helper) {
		GunsGameTests.floor(helper);
		wall(helper, 10);
		Mob pig = NetherGunsGameTests.sturdyPig(helper, new BlockPos(1, 2, 8));
		GunSpec spec = JugcraftGuns.SPECS.get("narwhal_lance");
		Item rounds = JugcraftGuns.ROUNDS.get(spec.ammo());
		ServerPlayer shooter = GunsGameTests.shooter(helper, "narwhal_lance", 1, pig, GameType.SURVIVAL);
		helper.assertTrue(GunShots.fire(shooter), "The loaded Narwhal Lance did not fire");
		Microjet jet = onlyJet(helper, shooter, pig, "narwhal_lance");
		double launchSpeed = jet.getDeltaMovement().length();
		// Vanilla applies air drag before moving; the Microjet restores its launch velocity afterward.
		double expectedStep = launchSpeed * 0.99F;
		helper.assertTrue(GunItem.loaded(shooter.getMainHandItem()) == 0, "The Narwhal Lance's shot did not empty it");
		Vec3[] seen = new Vec3[1];
		helper.runAfterDelay(1, () -> seen[0] = jet.position());
		helper.runAfterDelay(2, () -> {
			double step = jet.position().distanceTo(seen[0]);
			helper.assertTrue(!jet.isRemoved() && Math.abs(step - expectedStep) < 1.0E-6
					&& Math.abs(jet.getDeltaMovement().length() - launchSpeed) < 1.0E-6,
					"The Narwhal Lance's Microjet flew " + step + " blocks in a tick, not its drag-adjusted launch speed " + expectedStep + ", or it is gone");
		});
		helper.runAfterDelay(6, () -> {
			helper.assertTrue(jet.isRemoved(), "The Narwhal Lance's Microjet flew on past the pig");
			helper.assertTrue(Math.abs(200.0F - pig.getHealth() - spec.damage()) < 1.0E-3F,
					"The Narwhal Lance's Microjet took " + (200.0F - pig.getHealth()) + ", not " + spec.damage());
			helper.assertFalse(GunShots.fire(shooter), "The empty Narwhal Lance fired");
			shooter.getInventory().setItem(1, new ItemStack(rounds, 2));
			helper.assertTrue(GunShots.reload(shooter), "The empty Narwhal Lance did not start to reload");
		});
		helper.runAfterDelay(6 + spec.reload() - 3, () -> helper.assertTrue(GunItem.loaded(shooter.getMainHandItem()) == 0,
				"The Narwhal Lance's Microjet was in before its reload's time"));
		helper.runAfterDelay(6 + spec.reload() + 2, () -> {
			helper.assertTrue(GunItem.loaded(shooter.getMainHandItem()) == 1 && GunShots.count(shooter.getInventory(), rounds) == 1,
					"The Narwhal Lance loaded " + GunItem.loaded(shooter.getMainHandItem()) + ", not one of two Microjets");
			helper.assertTrue(GunShots.fire(shooter), "The reloaded Narwhal Lance did not fire");
			helper.succeed();
		});
	}

	/**
	 * An empty Halibut Gun with two shells loads one at its first shell's time and both by its reload's time for two,
	 * leaving none in the inventory. A pull then lands its pellets on a pig three blocks off that a hit can push, as one
	 * hit of a whole number of pellets, up to all eight; the pig is knocked away faster than a second pig shot by the
	 * Kreuzer Pistol, and the shooter is pushed back the owner's 0.45 times the level share of their look.
	 */
	@GameTest(structure = GunsGameTests.ARENA, maxTicks = 120)
	public void halibutGunLoadsAShellAtATimeAndThrowsBack(GameTestHelper helper) {
		GunsGameTests.floor(helper);
		Mob pig = pushablePig(helper, new BlockPos(1, 2, 4));
		Mob other = pushablePig(helper, new BlockPos(7, 2, 4));
		GunSpec spec = JugcraftGuns.SPECS.get("halibut_gun");
		Item shells = JugcraftGuns.ROUNDS.get(spec.ammo());
		ServerPlayer shooter = GunsGameTests.shooter(helper, "halibut_gun", 0, pig, GameType.SURVIVAL);
		ServerPlayer pistol = NetherGunsGameTests.shooter(helper, "kreuzer_pistol", new BlockPos(7, 2, 1), other);
		shooter.getInventory().setItem(1, new ItemStack(shells, 2));
		helper.assertTrue(GunShots.reload(shooter), "The empty Halibut Gun did not start to reload");
		helper.runAfterDelay(spec.shellStart() + spec.shellEach() + 1, () -> helper.assertTrue(GunItem.loaded(shooter.getMainHandItem()) == 1,
				"The Halibut Gun had not one shell in after its first shell's time"));
		helper.runAfterDelay(spec.reloadTicks(2) + 2, () -> {
			helper.assertTrue(GunItem.loaded(shooter.getMainHandItem()) == 2 && GunShots.count(shooter.getInventory(), shells) == 0,
					"The Halibut Gun holds " + GunItem.loaded(shooter.getMainHandItem()) + " shells after loading two, not 2");
			helper.assertFalse(GunShots.reloading(shooter), "The Halibut Gun was still reloading with no shells left to load");
			shooter.setDeltaMovement(Vec3.ZERO);
			pistol.setDeltaMovement(Vec3.ZERO);
			Vec3 look = shooter.getLookAngle();
			double back = 0.45 * Math.sqrt(look.x * look.x + look.z * look.z);
			helper.assertTrue(GunShots.fire(shooter) && GunShots.fire(pistol), "The Halibut Gun or the Kreuzer Pistol did not fire");
			float took = 200.0F - pig.getHealth();
			float pellets = took / spec.damage();
			helper.assertTrue(took > 0.0F && pellets <= spec.pellets() + 1.0E-3F && Math.abs(pellets - Math.round(pellets)) < 1.0E-3F,
					"The shot took " + took + ", not a whole number of " + spec.damage() + "-point pellets up to " + spec.pellets());
			double pushed = pig.getDeltaMovement().horizontalDistance();
			double nudged = other.getDeltaMovement().horizontalDistance();
			helper.assertTrue(pushed > nudged + 0.2, "The Halibut Gun's hit did not knock its pig away faster: " + pushed + " against " + nudged);
			Vec3 kicked = shooter.getDeltaMovement();
			helper.assertTrue(back > 0.35 && Math.abs(kicked.z + back) < 1.0E-6 && Math.abs(kicked.x) < 1.0E-6 && Math.abs(kicked.y) < 1.0E-6,
					"The Halibut Gun's shot moved its shooter " + kicked + ", not " + back + " back");
			helper.assertTrue(GunItem.loaded(shooter.getMainHandItem()) == 1, "The shot did not spend one of two shells");
			helper.succeed();
		});
	}

	/** The one Microjet near the shooter just after its shot: the gun's, with the gun's damage and range, the pig unhurt. */
	private static Microjet onlyJet(GameTestHelper helper, ServerPlayer shooter, Mob pig, String gun) {
		GunSpec spec = JugcraftGuns.SPECS.get(gun);
		List<Microjet> jets = helper.getLevel().getEntitiesOfClass(Microjet.class, new AABB(shooter.blockPosition()).inflate(3.0));
		helper.assertTrue(jets.size() == 1 && pig.getHealth() == 200.0F, jets.size() + " Microjets from the " + gun + ", not one, and the pig took "
				+ (200.0F - pig.getHealth()) + " at once");
		Microjet jet = jets.getFirst();
		helper.assertTrue(jet.getOwner() == shooter && jet.gun().equals(gun) && jet.damage() == spec.damage() && jet.left() == spec.range(),
				"The Microjet is not the " + gun + "'s: " + jet.gun() + ", " + jet.damage() + ", " + jet.left());
		// Projectile.getMovementToShoot adds bounded triangular spread on each axis before scaling by speed.
		double speed = JugcraftGuns.jetSpeed(JugcraftGuns.GUNS.get(gun));
		Vec3 deviation = jet.getDeltaMovement().subtract(shooter.getLookAngle().normalize().scale(speed));
		double spread = speed * 0.0172275 * spec.hipSpread() + 1.0E-6;
		helper.assertTrue(Math.abs(deviation.x) <= spread && Math.abs(deviation.y) <= spread && Math.abs(deviation.z) <= spread,
				gun + " launch velocity must use its configured speed and bounded spread: " + jet.getDeltaMovement());
		return jet;
	}

	/** A stone wall across the shooters' lane (x 0 to 2) at {@code z}, from the floor up. */
	private static void wall(GameTestHelper helper, int z) {
		for (int x = 0; x <= 2; x++) {
			for (int y = 2; y <= 4; y++) {
				helper.setBlock(new BlockPos(x, y, z), Blocks.STONE);
			}
		}
	}

	/** A sturdy pig of 200 health that a hit can push. */
	private static Mob pushablePig(GameTestHelper helper, BlockPos at) {
		Mob pig = NetherGunsGameTests.sturdyPig(helper, at);
		pig.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(0.0);
		return pig;
	}
}
