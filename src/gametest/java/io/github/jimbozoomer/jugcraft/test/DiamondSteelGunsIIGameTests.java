package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.guns.GunItem;
import io.github.jimbozoomer.jugcraft.guns.GunShots;
import io.github.jimbozoomer.jugcraft.guns.GunSpec;
import io.github.jimbozoomer.jugcraft.guns.JugcraftGuns;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

/**
 * In-game tests for slice 15B's guns (docs/features/guns.md), the next of the owner's diamond-steel guns, through the
 * server's entry points as {@link DiamondSteelGunsGameTests} does: the Kreuzer Pistol, its magazine ahead of its
 * trigger; the Psalm SMG, a machine pistol; and the Jawbreaker, a four-shot magnum. Each is held in one hand, and in
 * both with a stock fitted, as a carbine, and fed from a box magazine. Shooters face south (+z).
 */
public class DiamondSteelGunsIIGameTests {
	/** The slice's guns, in the order the creative tab shows them. */
	static final List<String> GUNS = List.of("kreuzer_pistol", "psalm_smg", "jawbreaker");
	/** The magazines the Kreuzer Pistol and the Psalm SMG take. */
	static final List<String> MAGAZINES = List.of("extended_magazine", "speed_magazine");

	/**
	 * Each is registered with its numbers and its recipe loads, held in one hand and a carbine with a stock, firing plain
	 * bullets a magazine at a time. The Kreuzer Pistol is thirteen light rounds, a pull a shot, quicker to fire than any
	 * other one-handed gun fired a pull at a time; it takes the barrels' four, the magazines and the stocks, and no sight
	 * (its sights are on its body). The Psalm SMG fires twenty-five light rounds as long as the trigger is held, every
	 * second tick, straying further aimed than the Onrush SMG; it takes what the Kreuzer Pistol takes. The Jawbreaker is
	 * four rifle rounds, a pull a shot, hitting harder than the Bulldog Pistol, the Parley Revolver and the Half-Crown
	 * Revolver; it takes the barrels' four, the stocks and the four sights, and no magazine.
	 */
	@GameTest
	public void diamondSteelSidearmsAreRegistered(GameTestHelper helper) {
		for (String name : GUNS) {
			GunSpec spec = JugcraftGuns.SPECS.get(name);
			GunItem gun = JugcraftGuns.GUNS.get(name);
			helper.assertTrue(spec != null && gun != null && gun.spec() == spec, name + " is not registered with its numbers");
			helper.assertTrue(JugcraftGuns.shot(gun).equals("bullet") && !JugcraftGuns.ignites(gun) && !JugcraftGuns.levitates(gun),
					name + " does not fire plain bullets");
			helper.assertTrue(!spec.byShell(), name + " is loaded a round at a time");
			helper.assertTrue(JugcraftGuns.ONE_HANDED.contains(name) && JugcraftGuns.CARBINES.contains(name),
					name + " is not held in one hand, and in both with a stock");
			helper.assertTrue(helper.getLevel().recipeAccess().byKey(ResourceKey.create(Registries.RECIPE, Jugcraft.id(name))).isPresent(),
					"The " + name + " recipe does not load");
		}
		GunSpec kreuzer = JugcraftGuns.SPECS.get("kreuzer_pistol");
		helper.assertTrue(kreuzer.ammo().equals("light_round") && kreuzer.capacity() == 13 && !kreuzer.auto(),
				"The Kreuzer Pistol is not a magazine of thirteen light rounds, a pull a shot");
		JugcraftGuns.SPECS.forEach((name, spec) -> helper.assertTrue(name.equals("kreuzer_pistol") || !JugcraftGuns.ONE_HANDED.contains(name)
				|| spec.auto() || spec.interval() > kreuzer.interval(), "The " + name + " fires a pull at a time as quickly as the Kreuzer Pistol"));
		GunSpec psalm = JugcraftGuns.SPECS.get("psalm_smg");
		helper.assertTrue(psalm.ammo().equals("light_round") && psalm.capacity() == 25 && psalm.auto() && psalm.interval() == 2,
				"The Psalm SMG is not an automatic of twenty-five light rounds, every second tick");
		helper.assertTrue(psalm.aimSpread() > JugcraftGuns.SPECS.get("onrush_smg").aimSpread(),
				"Aimed, the Psalm SMG strays no further than the Onrush SMG");
		for (String name : List.of("kreuzer_pistol", "psalm_smg")) {
			List<String> takes = JugcraftGuns.ACCEPTS.get(name);
			helper.assertTrue(takes.containsAll(BrassSidearmsGameTests.BARREL) && takes.containsAll(MAGAZINES)
					&& takes.containsAll(BrassSidearmsGameTests.STOCKS) && takes.size() == 9, "The " + name + " takes " + takes);
		}
		GunSpec jaw = JugcraftGuns.SPECS.get("jawbreaker");
		helper.assertTrue(jaw.ammo().equals("rifle_round") && jaw.capacity() == 4 && !jaw.auto(),
				"The Jawbreaker is not a magazine of four rifle rounds, a pull a shot");
		for (String revolver : List.of("bulldog_pistol", "parley_revolver", "half_crown_revolver")) {
			helper.assertTrue(jaw.damage() > JugcraftGuns.SPECS.get(revolver).damage(), "The Jawbreaker does not hit harder than the " + revolver);
		}
		List<String> jawTakes = JugcraftGuns.ACCEPTS.get("jawbreaker");
		helper.assertTrue(jawTakes.containsAll(BrassSidearmsGameTests.BARREL) && jawTakes.containsAll(BrassSidearmsGameTests.STOCKS)
				&& jawTakes.containsAll(BrassSidearmsGameTests.SIGHTS) && jawTakes.stream().noneMatch(MAGAZINES::contains)
				&& jawTakes.size() == 11, "The Jawbreaker takes " + jawTakes);
		helper.succeed();
	}

	/**
	 * Without a stock each pairs with a Minuet Pistol in the other hand, two guns at once; with a Wooden Stock fitted it is
	 * a carbine held in both hands and the other hand's gun does not fire beside it; without it again it is one-handed.
	 */
	@GameTest
	public void diamondSteelSidearmsPairUntilStocked(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		for (String name : GUNS) {
			ItemStack gun = loaded(name);
			player.setItemInHand(InteractionHand.MAIN_HAND, gun);
			player.setItemInHand(InteractionHand.OFF_HAND, loaded("minuet_pistol"));
			helper.assertTrue(GunItem.oneHanded(gun) && !GunItem.carbine(gun) && GunItem.dual(player),
					"A " + name + " without a stock and a Minuet Pistol are not two guns at once");
			GunItem.fit(gun, "wooden_stock");
			helper.assertTrue(GunItem.carbine(gun) && !GunItem.oneHanded(gun) && !GunItem.dual(player),
					"A " + name + " with a stock is not a carbine held in both hands");
			helper.assertFalse(GunShots.fire(player, InteractionHand.OFF_HAND), "The other hand's gun fired beside a " + name);
			GunItem.remove(gun, "wooden_stock");
			helper.assertTrue(GunItem.oneHanded(gun) && GunItem.dual(player), "Without its stock the " + name + " is not one-handed again");
		}
		helper.succeed();
	}

	/** An empty Kreuzer Pistol with twenty light rounds loads thirteen and leaves seven; a shot takes its damage. */
	@GameTest(structure = GunsGameTests.ARENA, maxTicks = 120)
	public void kreuzerPistolChangesItsMagazine(GameTestHelper helper) {
		DiamondSteelGunsGameTests.changesItsMagazine(helper, "kreuzer_pistol", 20);
	}

	/**
	 * An empty Psalm SMG with thirty light rounds loads twenty-five and leaves five; it fires as long as the trigger is
	 * held: two shots at once (the trigger's bank) and another each interval.
	 */
	@GameTest(structure = GunsGameTests.ARENA, maxTicks = 120)
	public void psalmSmgChangesItsMagazine(GameTestHelper helper) {
		DiamondSteelGunsGameTests.changesItsMagazine(helper, "psalm_smg", 30);
	}

	/** An empty Jawbreaker with six rifle rounds loads four and leaves two; three shots leave one. */
	@GameTest(structure = GunsGameTests.ARENA, maxTicks = 120)
	public void jawbreakerChangesItsMagazine(GameTestHelper helper) {
		DiamondSteelGunsGameTests.changesItsMagazine(helper, "jawbreaker", 6);
	}

	private static ItemStack loaded(String name) {
		ItemStack stack = new ItemStack(JugcraftGuns.GUNS.get(name));
		GunItem.setLoaded(stack, JugcraftGuns.SPECS.get(name).capacity());
		return stack;
	}
}
