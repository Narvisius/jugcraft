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
 * In-game tests for slice 14F's gun (docs/features/guns.md), the last of the owner's treated-brass guns, through the
 * server's entry points as {@link GunsGameTests} does: the Old Grange Carbine, the Grange Carbine's older make, with a
 * longer barrel, loaded from the top with a stripper clip. It is held in both hands. Shooters face south (+z).
 */
public class BrassGunsVIGameTests {
	/** The slice's guns, in the order the creative tab shows them. */
	static final List<String> GUNS = List.of("old_grange_carbine");

	/**
	 * It is registered with its numbers and its recipe loads, held in both hands. It fires a rifle round a pull from a
	 * clip of seven: slower than the Grange Carbine, harder hitting, further reaching and straighter aimed. It takes
	 * the Grange Carbine's attachments but its two magazines, having none.
	 */
	@GameTest
	public void oldGrangeCarbineIsRegistered(GameTestHelper helper) {
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
		GunSpec old = JugcraftGuns.SPECS.get("old_grange_carbine");
		GunSpec grange = JugcraftGuns.SPECS.get("grange_carbine");
		helper.assertTrue(old.ammo().equals("rifle_round") && old.capacity() == 7 && !old.auto(),
				"The Old Grange Carbine is not a clip of seven rifle rounds, a pull a shot");
		helper.assertTrue(old.interval() > grange.interval() && old.damage() > grange.damage() && old.range() > grange.range()
				&& old.aimSpread() < grange.aimSpread(), "The Old Grange Carbine does not fire slower, hit harder, reach "
				+ "further and stray less aimed than the Grange Carbine");
		List<String> takes = JugcraftGuns.ACCEPTS.get("old_grange_carbine");
		List<String> grangeTakes = JugcraftGuns.ACCEPTS.get("grange_carbine").stream()
				.filter(kind -> !kind.equals("extended_magazine") && !kind.equals("speed_magazine")).toList();
		helper.assertTrue(takes.equals(grangeTakes), "The Old Grange Carbine takes " + takes);
		helper.succeed();
	}

	/**
	 * An empty Old Grange Carbine with ten rifle rounds in the inventory loads a clip: nothing is in before its reload's
	 * time, seven rounds after it, and three are left. A shot then takes its damage from the pig. The trigger holds two
	 * shots at most ({@code GunShots.BURST}): a second pull at once fires, a third does not, and one after its interval
	 * does.
	 */
	@GameTest(structure = GunsGameTests.ARENA, maxTicks = 120)
	public void oldGrangeCarbineLoadsAClip(GameTestHelper helper) {
		GunsGameTests.floor(helper);
		Mob pig = NetherGunsGameTests.sturdyPig(helper, new BlockPos(1, 2, 4));
		GunSpec spec = JugcraftGuns.SPECS.get("old_grange_carbine");
		ServerPlayer shooter = GunsGameTests.shooter(helper, "old_grange_carbine", 0, pig, GameType.SURVIVAL);
		shooter.getInventory().setItem(1, new ItemStack(JugcraftGuns.ROUNDS.get("rifle_round"), 10));
		helper.assertTrue(GunShots.reload(shooter), "The empty Old Grange Carbine did not start to reload");
		helper.runAfterDelay(spec.reload() - 3, () -> helper.assertTrue(GunItem.loaded(shooter.getMainHandItem()) == 0,
				"The Old Grange Carbine's clip was in before its reload's time"));
		helper.runAfterDelay(spec.reload() + 2, () -> {
			helper.assertTrue(GunItem.loaded(shooter.getMainHandItem()) == 7, "The Old Grange Carbine loaded "
					+ GunItem.loaded(shooter.getMainHandItem()) + " rounds, not seven");
			helper.assertTrue(GunShots.count(shooter.getInventory(), JugcraftGuns.ROUNDS.get("rifle_round")) == 3,
					"The reload did not leave three rounds in the inventory");
			helper.assertTrue(GunShots.fire(shooter), "The loaded Old Grange Carbine did not fire");
			helper.assertTrue(Math.abs(200.0F - pig.getHealth() - spec.damage()) < 1.0E-3F,
					"The shot took " + (200.0F - pig.getHealth()) + ", not " + spec.damage());
			helper.assertTrue(GunShots.fire(shooter), "The Old Grange Carbine did not fire its second shot at once");
			helper.assertFalse(GunShots.fire(shooter), "The Old Grange Carbine fired a third shot in the same tick");
		});
		helper.runAfterDelay(spec.reload() + 3 + spec.interval(), () -> {
			helper.assertTrue(GunShots.fire(shooter), "The Old Grange Carbine did not fire after its interval");
			helper.assertTrue(GunItem.loaded(shooter.getMainHandItem()) == 4, "Three shots did not leave four rounds");
			helper.succeed();
		});
	}
}
