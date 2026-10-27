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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;

/**
 * In-game tests for slice 15A's guns (docs/features/guns.md), the first of the owner's diamond-steel guns, through the
 * server's entry points as {@link GunsGameTests} does: the Vesper Rifle, a heavy marksman's rifle; the Onrush SMG, a
 * submachine gun; and the Gallant Rifle, an automatic rifle silenced the length of its barrel. All three are held in
 * both hands and fed from box magazines. Shooters face south (+z).
 */
public class DiamondSteelGunsGameTests {
	/** The slice's guns, in the order the creative tab shows them. */
	static final List<String> GUNS = List.of("vesper_rifle", "onrush_smg", "gallant_rifle");

	/**
	 * Each is registered with its numbers and its recipe loads, held in both hands, firing plain bullets a magazine at a
	 * time. The Vesper Rifle is eight rifle rounds, a pull a shot, hitting harder, reaching further and straying less aimed
	 * than the Old Grange Carbine; it takes the Long Scope, the Reflex Sight and the Laser Sight but not the Medium
	 * Scope. The Onrush SMG fires twenty-three light rounds as long as the trigger is held, every second tick, and takes
	 * the four sights. The Gallant Rifle fires seventeen rifle rounds as long as the trigger is held; silenced its length,
	 * it takes no barrel attachment, and it takes the four sights.
	 */
	@GameTest
	public void diamondSteelGunsAreRegistered(GameTestHelper helper) {
		for (String name : GUNS) {
			GunSpec spec = JugcraftGuns.SPECS.get(name);
			GunItem gun = JugcraftGuns.GUNS.get(name);
			helper.assertTrue(spec != null && gun != null && gun.spec() == spec, name + " is not registered with its numbers");
			helper.assertTrue(JugcraftGuns.shot(gun).equals("bullet") && !JugcraftGuns.ignites(gun) && !JugcraftGuns.levitates(gun),
					name + " does not fire plain bullets");
			helper.assertTrue(!spec.byShell(), name + " is loaded a round at a time");
			helper.assertTrue(!JugcraftGuns.ONE_HANDED.contains(name) && !JugcraftGuns.CARBINES.contains(name),
					name + " is not held in both hands");
			helper.assertTrue(helper.getLevel().recipeAccess().byKey(ResourceKey.create(Registries.RECIPE, Jugcraft.id(name))).isPresent(),
					"The " + name + " recipe does not load");
		}
		GunSpec vesper = JugcraftGuns.SPECS.get("vesper_rifle");
		GunSpec old = JugcraftGuns.SPECS.get("old_grange_carbine");
		helper.assertTrue(vesper.ammo().equals("rifle_round") && vesper.capacity() == 8 && !vesper.auto(),
				"The Vesper Rifle is not a magazine of eight rifle rounds, a pull a shot");
		helper.assertTrue(vesper.damage() > old.damage() && vesper.range() > old.range() && vesper.aimSpread() < old.aimSpread(),
				"The Vesper Rifle does not hit harder, reach further and stray less aimed than the Old Grange Carbine");
		List<String> vesperTakes = JugcraftGuns.ACCEPTS.get("vesper_rifle");
		helper.assertTrue(vesperTakes.containsAll(List.of("long_scope", "reflex_sight", "laser_sight")) && !vesperTakes.contains("medium_scope")
				&& vesperTakes.containsAll(BrassSidearmsGameTests.BARREL) && vesperTakes.containsAll(BrassSidearmsGameTests.STOCKS),
				"The Vesper Rifle takes " + vesperTakes);
		GunSpec onrush = JugcraftGuns.SPECS.get("onrush_smg");
		helper.assertTrue(onrush.ammo().equals("light_round") && onrush.capacity() == 23 && onrush.auto() && onrush.interval() == 2,
				"The Onrush SMG is not an automatic of twenty-three light rounds, every second tick");
		helper.assertTrue(JugcraftGuns.ACCEPTS.get("onrush_smg").containsAll(BrassSidearmsGameTests.SIGHTS),
				"The Onrush SMG takes " + JugcraftGuns.ACCEPTS.get("onrush_smg"));
		GunSpec gallant = JugcraftGuns.SPECS.get("gallant_rifle");
		helper.assertTrue(gallant.ammo().equals("rifle_round") && gallant.capacity() == 17 && gallant.auto(),
				"The Gallant Rifle is not an automatic of seventeen rifle rounds");
		List<String> gallantTakes = JugcraftGuns.ACCEPTS.get("gallant_rifle");
		helper.assertTrue(gallantTakes.stream().noneMatch(BrassSidearmsGameTests.BARREL::contains)
				&& gallantTakes.containsAll(BrassSidearmsGameTests.SIGHTS), "The Gallant Rifle takes " + gallantTakes);
		helper.succeed();
	}

	/** An empty Vesper Rifle with ten rifle rounds loads eight and leaves two; a shot takes its damage. */
	@GameTest(structure = GunsGameTests.ARENA, maxTicks = 120)
	public void vesperRifleChangesItsMagazine(GameTestHelper helper) {
		changesItsMagazine(helper, "vesper_rifle", 10);
	}

	/**
	 * An empty Onrush SMG with thirty light rounds loads twenty-three and leaves seven; it fires as long as the trigger is
	 * held: two shots at once (the trigger's bank, {@code GunShots.BURST}) and another each interval.
	 */
	@GameTest(structure = GunsGameTests.ARENA, maxTicks = 120)
	public void onrushSmgChangesItsMagazine(GameTestHelper helper) {
		changesItsMagazine(helper, "onrush_smg", 30);
	}

	/** An empty Gallant Rifle with twenty rifle rounds loads seventeen and leaves three; it fires as the Onrush SMG does. */
	@GameTest(structure = GunsGameTests.ARENA, maxTicks = 120)
	public void gallantRifleChangesItsMagazine(GameTestHelper helper) {
		changesItsMagazine(helper, "gallant_rifle", 20);
	}

	/**
	 * An empty gun with {@code rounds} of its ammunition in the inventory: nothing is in before its reload's time, a full
	 * magazine after it, and the rest left. Then two pulls at once both fire (the trigger's bank), the first taking its
	 * damage from the pig, a third does not, and one after the gun's interval does: three rounds spent.
	 */
	static void changesItsMagazine(GameTestHelper helper, String name, int rounds) {
		GunsGameTests.floor(helper);
		Mob pig = NetherGunsGameTests.sturdyPig(helper, new BlockPos(1, 2, 4));
		GunSpec spec = JugcraftGuns.SPECS.get(name);
		ServerPlayer shooter = GunsGameTests.shooter(helper, name, 0, pig, GameType.SURVIVAL);
		shooter.getInventory().setItem(1, new ItemStack(JugcraftGuns.ROUNDS.get(spec.ammo()), rounds));
		helper.assertTrue(GunShots.reload(shooter), "The empty " + name + " did not start to reload");
		helper.runAfterDelay(spec.reload() - 3, () -> helper.assertTrue(GunItem.loaded(shooter.getMainHandItem()) == 0,
				"The " + name + "'s magazine was in before its reload's time"));
		helper.runAfterDelay(spec.reload() + 2, () -> {
			helper.assertTrue(GunItem.loaded(shooter.getMainHandItem()) == spec.capacity(), "The " + name + " loaded "
					+ GunItem.loaded(shooter.getMainHandItem()) + " rounds, not " + spec.capacity());
			helper.assertTrue(GunShots.count(shooter.getInventory(), JugcraftGuns.ROUNDS.get(spec.ammo())) == rounds - spec.capacity(),
					"The reload did not leave " + (rounds - spec.capacity()) + " rounds in the inventory");
			helper.assertTrue(GunShots.fire(shooter), "The loaded " + name + " did not fire");
			helper.assertTrue(Math.abs(200.0F - pig.getHealth() - spec.damage()) < 1.0E-3F,
					"The shot took " + (200.0F - pig.getHealth()) + ", not " + spec.damage());
			helper.assertTrue(GunShots.fire(shooter), "The " + name + " did not fire its second shot at once");
			helper.assertFalse(GunShots.fire(shooter), "The " + name + " fired a third shot in the same tick");
		});
		helper.runAfterDelay(spec.reload() + 3 + spec.interval(), () -> {
			helper.assertTrue(GunShots.fire(shooter), "The " + name + " did not fire after its interval");
			helper.assertTrue(GunItem.loaded(shooter.getMainHandItem()) == spec.capacity() - 3,
					"Three shots did not leave " + (spec.capacity() - 3) + " rounds");
			helper.succeed();
		});
	}
}
