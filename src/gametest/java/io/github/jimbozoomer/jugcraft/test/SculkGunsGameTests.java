package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.guns.GunItem;
import io.github.jimbozoomer.jugcraft.guns.GunShots;
import io.github.jimbozoomer.jugcraft.guns.GunSpec;
import io.github.jimbozoomer.jugcraft.guns.JugcraftGuns;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

/**
 * In-game tests for the sculk guns (slice 10D, docs/features/guns.md), through the server's entry points as
 * {@link GunsGameTests} does: the Undertone Rifle and the Murmur SMG load a magazine at a time, the Reverb a shell at a
 * time, and each lands its shot on what it aims at. Shooters face south (+z).
 */
public class SculkGunsGameTests {
	/** The sculk guns, in the order the creative tab shows them. */
	static final List<String> GUNS = List.of("undertone_rifle", "murmur_smg", "reverb");

	/**
	 * Each is registered with its numbers and its recipe loads. The Undertone Rifle fires one rifle round a pull from a
	 * magazine of twelve; the Murmur SMG fires light rounds for as long as the trigger is held, as often as any gun fires;
	 * the Reverb fires ten pellets a barrel from its two buckshot shells and is loaded a shell at a time. The Undertone
	 * Rifle takes the stocks, the Murmur SMG the magazines, the Reverb the light and tactical grips, and each the scopes
	 * and the Laser Sight.
	 */
	@GameTest
	public void sculkGunsAreRegistered(GameTestHelper helper) {
		for (String name : GUNS) {
			GunSpec spec = JugcraftGuns.SPECS.get(name);
			GunItem gun = JugcraftGuns.GUNS.get(name);
			helper.assertTrue(spec != null && gun != null && gun.spec() == spec, name + " is not registered with its numbers");
			helper.assertTrue(JugcraftGuns.shot(gun).equals("bullet"), name + " does not fire bullets");
			helper.assertTrue(helper.getLevel().recipeAccess().byKey(ResourceKey.create(Registries.RECIPE, Jugcraft.id(name))).isPresent(),
					"The " + name + " recipe does not load");
			List<String> takes = JugcraftGuns.ACCEPTS.getOrDefault(name, List.of());
			helper.assertTrue(takes.containsAll(List.of("long_scope", "medium_scope", "reflex_sight", "laser_sight")),
					"The " + name + " does not take the scopes and the Laser Sight");
		}
		GunSpec undertone = JugcraftGuns.SPECS.get("undertone_rifle");
		GunSpec murmur = JugcraftGuns.SPECS.get("murmur_smg");
		GunSpec reverb = JugcraftGuns.SPECS.get("reverb");
		helper.assertTrue(undertone.ammo().equals("rifle_round") && undertone.capacity() == 12 && !undertone.auto()
				&& undertone.pellets() == 1 && !undertone.byShell(), "The Undertone Rifle is not a rifle of twelve rounds, one a pull");
		helper.assertTrue(murmur.ammo().equals("light_round") && murmur.auto() && !murmur.byShell()
				&& JugcraftGuns.SPECS.values().stream().allMatch(other -> other.interval() >= murmur.interval()),
				"The Murmur SMG does not fire light rounds as often as any gun fires");
		helper.assertTrue(reverb.ammo().equals("buckshot_shell") && reverb.capacity() == 2 && reverb.pellets() == 10
				&& reverb.byShell() && !reverb.auto(), "The Reverb is not a double-barrel of ten pellets loaded a shell at a time");
		helper.assertTrue(JugcraftGuns.ACCEPTS.get("undertone_rifle").containsAll(List.of("light_stock", "weighted_stock", "wooden_stock")),
				"The Undertone Rifle does not take the stocks");
		helper.assertTrue(JugcraftGuns.ACCEPTS.get("murmur_smg").containsAll(List.of("extended_magazine", "speed_magazine")),
				"The Murmur SMG does not take the magazines");
		helper.assertTrue(JugcraftGuns.ACCEPTS.get("reverb").containsAll(List.of("light_grip", "tactical_grip")),
				"The Reverb does not take the grips");
		helper.succeed();
	}

	/**
	 * Side by side, each fires its last round at a pig three blocks off: the rifle's and the machine gun's one shot takes
	 * one round's damage, the Reverb's pellets land together. Each then loads from the inventory: the two magazines have
	 * nothing in halfway through their reload and are full as it ends; the Reverb has one shell in after its first shell's
	 * time and both after its reload. Two rounds are left in the inventory each time.
	 */
	@GameTest(structure = GunsGameTests.ARENA, maxTicks = 200)
	public void sculkGunsFireAndLoad(GameTestHelper helper) {
		GunsGameTests.floor(helper);
		int longest = 0;
		for (int i = 0; i < GUNS.size(); i++) {
			String name = GUNS.get(i);
			GunSpec spec = JugcraftGuns.SPECS.get(name);
			Item rounds = JugcraftGuns.ROUNDS.get(spec.ammo());
			Mob pig = GunsGameTests.pig(helper, new BlockPos(1 + 4 * i, 2, 4));
			pig.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200.0);
			pig.setHealth(200.0F);
			ServerPlayer shooter = shooter(helper, name, new BlockPos(1 + 4 * i, 2, 1), pig);
			helper.assertTrue(GunShots.fire(shooter), "The " + name + " did not fire");
			float taken = 200.0F - pig.getHealth();
			if (spec.pellets() > 1) {
				helper.assertTrue(taken >= spec.damage() * (spec.pellets() - 2),
						"At close range the " + name + "'s pellets took only " + taken + " (one pellet is " + spec.damage() + ")");
			} else {
				helper.assertTrue(Math.abs(taken - spec.damage()) < 1.0E-3F, "The " + name + "'s shot took " + taken + ", not " + spec.damage());
			}
			helper.assertTrue(GunItem.loaded(shooter.getMainHandItem()) == 0, "The " + name + " did not spend its round");
			shooter.getInventory().add(new ItemStack(rounds, spec.capacity() + 2));
			helper.assertTrue(GunShots.reload(shooter), "The " + name + "'s reload did not start");
			int ticks = spec.reloadTicks(spec.capacity());
			if (spec.byShell()) {
				helper.runAfterDelay(spec.shellStart() + spec.shellEach() + 1, () -> helper.assertTrue(
						GunItem.loaded(shooter.getMainHandItem()) == 1, "The " + name + " had not one shell in after its first shell's time"));
			} else {
				helper.runAfterDelay(ticks / 2, () -> helper.assertTrue(GunItem.loaded(shooter.getMainHandItem()) == 0,
						"The " + name + " had a round in halfway through its reload"));
			}
			helper.runAfterDelay(ticks + 2, () -> {
				helper.assertTrue(GunItem.loaded(shooter.getMainHandItem()) == spec.capacity(), "The " + name + " holds "
						+ GunItem.loaded(shooter.getMainHandItem()) + " rounds after its reload, not " + spec.capacity());
				helper.assertTrue(GunShots.count(shooter.getInventory(), rounds) == 2,
						"The " + name + "'s reload did not take its rounds from the inventory");
				helper.assertFalse(GunShots.reloading(shooter), "The " + name + " was still reloading, full");
			});
			longest = Math.max(longest, ticks + 2);
		}
		helper.runAfterDelay(longest + 2, helper::succeed);
	}

	/** A shooter standing at {@code at}, facing south with its look on the target's middle, its last round loaded. */
	private static ServerPlayer shooter(GameTestHelper helper, String gun, BlockPos at, Mob target) {
		ServerPlayer player = GunsGameTests.shooter(helper, gun, 1, target, GameType.SURVIVAL);
		BlockPos pos = helper.absolutePos(at);
		player.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
		Vec3 aim = target.getBoundingBox().getCenter().subtract(player.getEyePosition());
		player.setXRot((float) Math.toDegrees(Math.atan2(-aim.y, Math.sqrt(aim.x * aim.x + aim.z * aim.z))));
		return player;
	}
}
