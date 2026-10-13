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
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;

/**
 * In-game tests for the automatic weapons (slice 9B, docs/features/guns.md), through the server's entry points as
 * {@link GunsGameTests} does: the Rattler Pistol, Bronco SMG and Squall Rifle fire Light Rounds for as long as the
 * trigger is held, each shot as fast as its interval allows, and load from the inventory in their reload time. Shooters
 * face south (+z).
 */
public class AutomaticGunsGameTests {
	/** The automatic weapons, in the order the creative tab shows them. */
	static final List<String> GUNS = List.of("rattler_pistol", "bronco_smg", "squall_rifle");

	/**
	 * Each fires one bullet at a time from Light Rounds, automatically; each one's recipe loads. The Rattler and the
	 * Squall, made with sights to swap, take the three scopes; the Bronco, made with none, takes no scope.
	 */
	@GameTest
	public void automaticWeaponsFireWhileHeld(GameTestHelper helper) {
		for (String name : GUNS) {
			GunSpec spec = JugcraftGuns.SPECS.get(name);
			GunItem gun = JugcraftGuns.GUNS.get(name);
			helper.assertTrue(spec != null && gun != null && gun.spec() == spec, name + " is not registered with its numbers");
			helper.assertTrue(spec.auto() && spec.pellets() == 1 && !spec.byShell() && spec.ammo().equals("light_round")
					&& JugcraftGuns.shot(gun).equals("bullet"), name + " is not an automatic firing a Light Round at a time");
			helper.assertTrue(helper.getLevel().recipeAccess().byKey(ResourceKey.create(Registries.RECIPE, Jugcraft.id(name))).isPresent(),
					"The " + name + " recipe does not load");
			boolean scoped = !name.equals("bronco_smg");
			for (String scope : List.of("long_scope", "medium_scope", "reflex_sight")) {
				helper.assertTrue(JugcraftGuns.ACCEPTS.get(name).contains(scope) == scoped,
						"The " + name + (scoped ? " does not take" : " takes") + " a " + scope);
			}
		}
		helper.succeed();
	}

	/**
	 * Held, each fires two shots at once (the trigger's bank) and a third one interval later: all three land and spend a
	 * round each. Then the empty Squall Rifle's canister is changed: after its reload time, and not before, it holds a
	 * canister's forty rounds from the inventory.
	 */
	@GameTest(structure = GunsGameTests.ARENA, maxTicks = 200)
	public void automaticWeaponsLandAndLoad(GameTestHelper helper) {
		GunsGameTests.floor(helper);
		// Three blocks off, so even the Bronco's hip spread (5 degrees) keeps every bullet on the pig.
		Mob pig = GunsGameTests.pig(helper, new BlockPos(1, 2, 4));
		pig.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200.0);
		pig.setHealth(200.0F);
		ServerPlayer shooter = GunsGameTests.shooter(helper, "rattler_pistol", 20, pig, GameType.SURVIVAL);
		// Each gun starts once the trigger's bank has filled again (two of the slowest interval) since the last.
		int apart = 12;
		float total = 0.0F;
		for (int i = 0; i < GUNS.size(); i++) {
			String name = GUNS.get(i);
			GunSpec spec = JugcraftGuns.SPECS.get(name);
			float before = total;
			total += 3 * spec.damage();
			float after = total;
			int at = 1 + i * apart;
			helper.runAfterDelay(at, () -> {
				ItemStack stack = new ItemStack(JugcraftGuns.GUNS.get(name));
				GunItem.setLoaded(stack, spec.capacity());
				shooter.setItemInHand(InteractionHand.MAIN_HAND, stack);
				helper.assertTrue(GunShots.fire(shooter) && GunShots.fire(shooter), "The " + name + " did not fire twice at once");
				helper.assertTrue(Math.abs(200.0F - pig.getHealth() - before - 2 * spec.damage()) < 1.0E-3F,
						"After the " + name + "'s first two shots the pig had lost " + (200.0F - pig.getHealth()) + ", not " + (before + 2 * spec.damage()));
			});
			helper.runAfterDelay(at + spec.interval(), () -> {
				helper.assertTrue(GunShots.fire(shooter), "The " + name + " did not fire again after its interval");
				helper.assertTrue(Math.abs(200.0F - pig.getHealth() - after) < 1.0E-3F,
						"After the " + name + "'s three shots the pig had lost " + (200.0F - pig.getHealth()) + ", not " + after);
				helper.assertTrue(GunItem.loaded(shooter.getMainHandItem()) == spec.capacity() - 3, "The " + name + " did not spend three rounds");
			});
		}
		GunSpec squall = JugcraftGuns.SPECS.get("squall_rifle");
		Item rounds = JugcraftGuns.ROUNDS.get("light_round");
		int reloadAt = 1 + GUNS.size() * apart;
		helper.runAfterDelay(reloadAt, () -> {
			shooter.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(JugcraftGuns.GUNS.get("squall_rifle")));
			shooter.getInventory().add(new ItemStack(rounds, 45));
			helper.assertTrue(GunShots.reload(shooter), "The empty Squall's reload did not start");
		});
		helper.runAfterDelay(reloadAt + squall.reload() - 2, () -> helper.assertTrue(
				GunItem.loaded(shooter.getMainHandItem()) == 0, "The Squall loaded before its reload time was up"));
		helper.runAfterDelay(reloadAt + squall.reload() + 2, () -> {
			helper.assertTrue(GunItem.loaded(shooter.getMainHandItem()) == squall.capacity(),
					"The Squall holds " + GunItem.loaded(shooter.getMainHandItem()) + " rounds after its reload, not " + squall.capacity());
			helper.assertTrue(GunShots.count(shooter.getInventory(), rounds) == 45 - squall.capacity(),
					"The Squall's reload did not take its rounds from the inventory");
			helper.assertFalse(GunShots.reloading(shooter), "Still reloading after the Squall's reload");
			helper.succeed();
		});
	}
}
