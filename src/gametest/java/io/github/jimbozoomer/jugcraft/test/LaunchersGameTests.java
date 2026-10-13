package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.guns.GunItem;
import io.github.jimbozoomer.jugcraft.guns.GunShots;
import io.github.jimbozoomer.jugcraft.guns.GunSpec;
import io.github.jimbozoomer.jugcraft.guns.JugcraftGuns;
import io.github.jimbozoomer.jugcraft.rocketry.CombatRocket;
import io.github.jimbozoomer.jugcraft.rocketry.JugcraftRocketry;
import io.github.jimbozoomer.jugcraft.weapons.GrenadeEntity;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * In-game tests for the launchers (slice 10A, docs/features/guns.md), through the server's entry points as
 * {@link GunsGameTests} does: the Earthmover and the Skylark Rifle fire the rocketry branch's High-Explosive Rockets,
 * which burst on what they hit, or in the air once their fuse (the gun's range) runs out, and break no block; the
 * Bullfrog lobs a grenade. Shooters face south (+z).
 */
public class LaunchersGameTests {
	/** The launchers, in the order the creative tab shows them. */
	static final List<String> GUNS = List.of("earthmover", "skylark_rifle", "bullfrog");
	/** The two that fire rockets. */
	static final List<String> ROCKET_GUNS = List.of("earthmover", "skylark_rifle");

	/**
	 * Each is registered with its numbers and its recipe loads. The Earthmover and the Skylark Rifle fire High-Explosive
	 * Rockets, a rocket a round, as rockets: the Skylark's fly faster and further, and it is the steadier aimed; the
	 * Earthmover's drum holds four, the Skylark one. The Bullfrog lobs grenades, of any kind, one a reload. None takes a
	 * barrel or magazine attachment (the owner made them none); the Earthmover takes the scopes, on its side rail, and
	 * the other two the stocks.
	 */
	@GameTest
	public void launchersAreRegistered(GameTestHelper helper) {
		for (String name : GUNS) {
			GunSpec spec = JugcraftGuns.SPECS.get(name);
			GunItem gun = JugcraftGuns.GUNS.get(name);
			helper.assertTrue(spec != null && gun != null && gun.spec() == spec, name + " is not registered with its numbers");
			helper.assertTrue(helper.getLevel().recipeAccess().byKey(ResourceKey.create(Registries.RECIPE, Jugcraft.id(name))).isPresent(),
					"The " + name + " recipe does not load");
			List<String> takes = JugcraftGuns.ACCEPTS.get(name);
			for (String part : List.of("silencer", "muzzle_brake", "extended_barrel", "extended_magazine", "speed_magazine")) {
				helper.assertFalse(takes.contains(part), "The " + name + " takes a " + part);
			}
		}
		for (String name : ROCKET_GUNS) {
			GunItem gun = JugcraftGuns.GUNS.get(name);
			GunSpec spec = gun.spec();
			helper.assertTrue(JugcraftGuns.shot(gun).equals(JugcraftGuns.ROCKET) && JugcraftGuns.ammo(spec) == JugcraftRocketry.HE_ROCKET
					&& JugcraftGuns.perItem(spec) == 1 && spec.pellets() == 1 && JugcraftGuns.rocketSpeed(gun) > 0.0F,
					"The " + name + " does not fire a High-Explosive Rocket a round");
		}
		GunItem earthmover = JugcraftGuns.GUNS.get("earthmover");
		GunItem skylark = JugcraftGuns.GUNS.get("skylark_rifle");
		helper.assertTrue(JugcraftGuns.rocketSpeed(skylark) > JugcraftGuns.rocketSpeed(earthmover)
				&& skylark.spec().range() > earthmover.spec().range() && skylark.spec().aimSpread() < earthmover.spec().aimSpread(),
				"The Skylark Rifle's rockets do not fly faster and further, or it is not the steadier aimed");
		helper.assertTrue(earthmover.spec().capacity() == 4 && skylark.spec().capacity() == 1,
				"The Earthmover does not hold four rockets, or the Skylark Rifle one");
		GunItem bullfrog = JugcraftGuns.GUNS.get("bullfrog");
		helper.assertTrue(JugcraftGuns.shot(bullfrog).equals(JugcraftGuns.GRENADE) && JugcraftGuns.takesGrenades(bullfrog.spec())
				&& bullfrog.spec().capacity() == 1 && JugcraftGuns.rocketSpeed(bullfrog) == 0.0F,
				"The Bullfrog does not lob grenades, one a reload");
		helper.assertTrue(JugcraftGuns.ACCEPTS.get("earthmover").containsAll(List.of("long_scope", "medium_scope", "reflex_sight")),
				"The Earthmover does not take the scopes");
		for (String name : List.of("skylark_rifle", "bullfrog")) {
			helper.assertTrue(JugcraftGuns.ACCEPTS.get(name).containsAll(List.of("light_stock", "weighted_stock", "wooden_stock")),
					"The " + name + " does not take the three stocks");
		}
		helper.succeed();
	}

	/**
	 * Side by side, ten blocks apart, the Earthmover and the Skylark Rifle each fire at a pig nine blocks off: the shot
	 * spends a round and fires one rocket of the shooter's, at the gun's rocket speed, its fuse the gun's range over that
	 * speed. Each rocket bursts on its pig, which is hurt; the shooters, out of the burst, are not, and the floor is whole.
	 */
	@GameTest(structure = GunsGameTests.ARENA, maxTicks = 60)
	public void rocketsBurstOnWhatTheyHit(GameTestHelper helper) {
		GunsGameTests.floor(helper);
		ServerLevel level = helper.getLevel();
		AABB arena = new AABB(helper.absolutePos(new BlockPos(0, 0, 0))).expandTowards(16, 6, 16);
		List<Mob> pigs = new ArrayList<>();
		List<ServerPlayer> shooters = new ArrayList<>();
		List<Float> healths = new ArrayList<>();
		for (int i = 0; i < ROCKET_GUNS.size(); i++) {
			String name = ROCKET_GUNS.get(i);
			GunItem gun = JugcraftGuns.GUNS.get(name);
			GunSpec spec = gun.spec();
			Mob pig = GunsGameTests.pig(helper, new BlockPos(2 + 10 * i, 2, 10));
			pig.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200.0);
			pig.setHealth(200.0F);
			ServerPlayer shooter = shooter(helper, name, spec.capacity(), new BlockPos(2 + 10 * i, 2, 1), pig);
			helper.assertTrue(GunShots.fire(shooter), "The loaded " + name + " did not fire");
			helper.assertTrue(GunItem.loaded(shooter.getMainHandItem()) == spec.capacity() - 1, "The " + name + "'s shot did not spend a round");
			List<CombatRocket> rockets = level.getEntitiesOfClass(CombatRocket.class, arena, rocket -> rocket.getOwner() == shooter);
			helper.assertTrue(rockets.size() == 1, "The " + name + "'s shot fired " + rockets.size() + " rockets of the shooter's, not one");
			CombatRocket rocket = rockets.getFirst();
			float speed = JugcraftGuns.rocketSpeed(gun);
			helper.assertTrue(Math.abs(rocket.getDeltaMovement().length() - speed) < speed * 0.1,
					"The " + name + "'s rocket flies at " + rocket.getDeltaMovement().length() + " blocks a tick, not " + speed);
			helper.assertTrue(rocket.fuse() == Mth.ceil(spec.range() / speed),
					"The " + name + "'s rocket's fuse is " + rocket.fuse() + " ticks, not its range's " + Mth.ceil(spec.range() / speed));
			helper.assertFalse(rocket.homing(), "The " + name + " fired a homing rocket");
			pigs.add(pig);
			shooters.add(shooter);
			healths.add(shooter.getHealth());
		}
		helper.succeedWhen(() -> {
			for (int i = 0; i < ROCKET_GUNS.size(); i++) {
				String name = ROCKET_GUNS.get(i);
				helper.assertTrue(pigs.get(i).getHealth() < 200.0F, "The " + name + "'s rocket has not burst on its pig yet");
				helper.assertTrue(shooters.get(i).getHealth() == healths.get(i), "The " + name + "'s burst nine blocks off hurt its shooter");
			}
			helper.assertTrue(level.getEntitiesOfClass(CombatRocket.class, arena).isEmpty(), "A rocket is still flying");
			for (int x = 0; x <= 15; x++) {
				for (int z = 0; z <= 15; z++) {
					helper.assertBlockPresent(Blocks.STONE, new BlockPos(x, 1, z));
				}
			}
		});
	}

	/**
	 * A rocket whose fuse runs out bursts where it is, in the air: one held still two and a half blocks over a pig, its
	 * fuse set to ten ticks (a later fuse set after does not put it off), leaves the pig whole for seven ticks; by the
	 * fourteenth it has burst, hurting the pig.
	 */
	@GameTest(structure = GunsGameTests.ARENA, maxTicks = 40)
	public void rocketBurstsWhenItsFuseRunsOut(GameTestHelper helper) {
		GunsGameTests.floor(helper);
		Mob pig = GunsGameTests.pig(helper, new BlockPos(7, 2, 7));
		pig.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200.0);
		pig.setHealth(200.0F);
		CombatRocket rocket = new CombatRocket(JugcraftRocketry.COMBAT_ROCKET, helper.getLevel());
		BlockPos at = helper.absolutePos(new BlockPos(7, 4, 7));
		rocket.setPos(at.getX() + 0.5, at.getY() + 0.5, at.getZ() + 0.5);
		rocket.setDeltaMovement(Vec3.ZERO);
		rocket.fuse(10);
		rocket.fuse(500);
		helper.assertTrue(rocket.fuse() == 10, "Setting a later fuse put the rocket's off: " + rocket.fuse() + " ticks");
		helper.getLevel().addFreshEntity(rocket);
		helper.runAfterDelay(7, () -> helper.assertTrue(rocket.isAlive() && pig.getHealth() == 200.0F,
				"The rocket burst before its fuse ran out"));
		helper.runAfterDelay(14, () -> {
			helper.assertFalse(rocket.isAlive(), "The rocket had not burst when its fuse ran out");
			helper.assertTrue(pig.getHealth() < 200.0F, "The rocket's burst over the pig did not hurt it");
			helper.succeed();
		});
	}

	/**
	 * A Bullfrog shot spends its one grenade and lobs a Grenade, owned by the shooter, that bursts on the pig seven blocks
	 * off: the pig is hurt, the shooter is out of the burst, and the floor is whole.
	 */
	@GameTest(structure = GunsGameTests.ARENA, maxTicks = 60)
	public void bullfrogLobsAGrenade(GameTestHelper helper) {
		GunsGameTests.floor(helper);
		Mob pig = GunsGameTests.pig(helper, new BlockPos(1, 2, 8));
		pig.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200.0);
		pig.setHealth(200.0F);
		ServerPlayer shooter = GunsGameTests.shooter(helper, "bullfrog", 1, pig, GameType.SURVIVAL);
		float shooterHealth = shooter.getHealth();
		helper.assertTrue(GunShots.fire(shooter), "The loaded Bullfrog did not fire");
		helper.assertTrue(GunItem.loaded(shooter.getMainHandItem()) == 0, "The shot did not spend the Bullfrog's grenade");
		AABB arena = new AABB(helper.absolutePos(new BlockPos(0, 0, 0))).expandTowards(16, 6, 16);
		helper.assertTrue(helper.getLevel().getEntitiesOfClass(GrenadeEntity.class, arena, grenade -> grenade.getOwner() == shooter).size() == 1,
				"The shot lobbed no grenade of the shooter's");
		helper.succeedWhen(() -> {
			helper.assertTrue(pig.getHealth() < 200.0F, "The grenade has not burst on the pig yet");
			helper.assertTrue(shooter.getHealth() == shooterHealth, "The burst seven blocks off hurt the shooter");
			for (int x = 0; x <= 15; x++) {
				for (int z = 0; z <= 15; z++) {
					helper.assertBlockPresent(Blocks.STONE, new BlockPos(x, 1, z));
				}
			}
		});
	}

	/**
	 * An empty Earthmover with six High-Explosive Rockets in the inventory loads a full drum of four once its reload's
	 * time is up, not before, and leaves two. Homing rockets stay the rocket launcher's: with only those, it does not
	 * start a reload.
	 */
	@GameTest(structure = GunsGameTests.ARENA, maxTicks = 100)
	public void earthmoverLoadsHighExplosiveRockets(GameTestHelper helper) {
		GunsGameTests.floor(helper);
		Mob pig = GunsGameTests.pig(helper, new BlockPos(1, 2, 8));
		ServerPlayer homing = GunsGameTests.shooter(helper, "earthmover", 0, pig, GameType.SURVIVAL);
		homing.getInventory().add(new ItemStack(JugcraftRocketry.HOMING_ROCKET, 4));
		helper.assertFalse(GunShots.reload(homing), "The Earthmover started a reload with only homing rockets");
		ServerPlayer shooter = GunsGameTests.shooter(helper, "earthmover", 0, pig, GameType.SURVIVAL);
		shooter.getInventory().add(new ItemStack(JugcraftRocketry.HE_ROCKET, 6));
		helper.assertTrue(GunShots.reload(shooter), "The empty Earthmover did not start a reload");
		GunSpec spec = JugcraftGuns.SPECS.get("earthmover");
		helper.runAfterDelay(spec.reload() - 3, () -> helper.assertTrue(GunItem.loaded(shooter.getMainHandItem()) == 0,
				"The Earthmover's drum was in before its reload's time"));
		helper.runAfterDelay(spec.reload() + 3, () -> {
			helper.assertTrue(GunItem.loaded(shooter.getMainHandItem()) == spec.capacity(), "The Earthmover holds "
					+ GunItem.loaded(shooter.getMainHandItem()) + " rockets after its reload, not " + spec.capacity());
			helper.assertTrue(GunShots.count(shooter.getInventory(), JugcraftRocketry.HE_ROCKET) == 2,
					"The reload did not take a drum's four rockets from the six");
			helper.assertFalse(GunShots.reloading(shooter), "The Earthmover was still reloading with its drum full");
			helper.succeed();
		});
	}

	/** A shooter standing at {@code at}, facing south with its look on the target's middle, holding the gun loaded so. */
	private static ServerPlayer shooter(GameTestHelper helper, String gun, int loaded, BlockPos at, Mob target) {
		ServerPlayer player = GunsGameTests.shooter(helper, gun, loaded, target, GameType.SURVIVAL);
		BlockPos pos = helper.absolutePos(at);
		player.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
		Vec3 aim = target.getBoundingBox().getCenter().subtract(player.getEyePosition());
		player.setXRot((float) Math.toDegrees(Math.atan2(-aim.y, Math.sqrt(aim.x * aim.x + aim.z * aim.z))));
		return player;
	}
}
