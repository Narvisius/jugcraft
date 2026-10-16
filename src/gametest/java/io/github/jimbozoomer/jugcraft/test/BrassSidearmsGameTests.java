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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;

/**
 * In-game tests for slice 14A's guns (docs/features/guns.md), through the server's entry points as
 * {@link GunsGameTests} does: the Holler Pistol, a heavy pistol held in both hands and fed from a magazine ahead of its
 * trigger; the Minuet Pistol, a light target pistol held in one hand; and the Half-Crown Revolver, a break-top revolver
 * loaded a round at a time, held in one hand, or in both as a carbine with a stock fitted. Shooters face south (+z).
 */
public class BrassSidearmsGameTests {
	/** The slice's guns, in the order the creative tab shows them. */
	static final List<String> GUNS = List.of("holler_pistol", "minuet_pistol", "half_crown_revolver");
	/** The barrel attachments. */
	static final List<String> BARREL = List.of("silencer", "baffled_silencer", "muzzle_brake", "extended_barrel");
	/** The stocks. */
	static final List<String> STOCKS = List.of("light_stock", "weighted_stock", "wooden_stock");
	/** The sights a gun's owner-made rail takes. */
	static final List<String> SIGHTS = List.of("long_scope", "medium_scope", "reflex_sight", "laser_sight");

	/**
	 * Each is registered with its numbers and its recipe loads, one pull a shot, firing bullets. The Holler Pistol fires
	 * rifle rounds, ten a magazine, held in both hands, and hits harder than any other pistol; it takes the barrel
	 * attachments, both magazines, the stocks, the grips, the bayonets and every sight but the Long Scope. The Minuet
	 * Pistol fires light rounds, ten a magazine, held in one hand, and aimed it strays less than any other gun held in
	 * one hand; it takes the barrel attachments, the Extended Magazine and the four sights. The Half-Crown Revolver fires
	 * rifle rounds, six loaded a round at a time, held in one hand but in both as a carbine, and takes what the Parley
	 * Revolver takes.
	 */
	@GameTest
	public void brassSidearmsAreRegistered(GameTestHelper helper) {
		for (String name : GUNS) {
			GunSpec spec = JugcraftGuns.SPECS.get(name);
			GunItem gun = JugcraftGuns.GUNS.get(name);
			helper.assertTrue(spec != null && gun != null && gun.spec() == spec, name + " is not registered with its numbers");
			helper.assertTrue(!spec.auto() && !JugcraftGuns.ignites(gun) && !JugcraftGuns.levitates(gun), name + " is not one pull a shot");
			helper.assertTrue(JugcraftGuns.shot(gun).equals("bullet"), name + " does not fire bullets");
			helper.assertTrue(helper.getLevel().recipeAccess().byKey(ResourceKey.create(Registries.RECIPE, Jugcraft.id(name))).isPresent(),
					"The " + name + " recipe does not load");
		}
		GunSpec holler = JugcraftGuns.SPECS.get("holler_pistol");
		GunSpec minuet = JugcraftGuns.SPECS.get("minuet_pistol");
		GunSpec crown = JugcraftGuns.SPECS.get("half_crown_revolver");
		helper.assertTrue(holler.ammo().equals("rifle_round") && holler.capacity() == 10 && !holler.byShell()
				&& !JugcraftGuns.ONE_HANDED.contains("holler_pistol"),
				"The Holler Pistol is not a magazine of ten rifle rounds held in both hands");
		helper.assertTrue(minuet.ammo().equals("light_round") && minuet.capacity() == 10 && !minuet.byShell()
				&& JugcraftGuns.ONE_HANDED.contains("minuet_pistol") && !JugcraftGuns.CARBINES.contains("minuet_pistol"),
				"The Minuet Pistol is not a magazine of ten light rounds held in one hand");
		helper.assertTrue(crown.ammo().equals("rifle_round") && crown.capacity() == 6 && crown.byShell()
				&& JugcraftGuns.ONE_HANDED.contains("half_crown_revolver") && JugcraftGuns.CARBINES.contains("half_crown_revolver"),
				"The Half-Crown Revolver is not a one-handed revolver of six rifle rounds loaded one at a time that a stock makes a carbine");
		JugcraftGuns.SPECS.forEach((name, spec) -> {
			helper.assertTrue(name.equals("holler_pistol") || !name.endsWith("_pistol") || spec.damage() < holler.damage(),
					"The " + name + " hits as hard as the Holler Pistol");
			helper.assertTrue(name.equals("minuet_pistol") || !JugcraftGuns.ONE_HANDED.contains(name) || spec.aimSpread() > minuet.aimSpread(),
					"Aimed, the " + name + " strays as little as the Minuet Pistol");
		});
		List<String> hollerTakes = JugcraftGuns.ACCEPTS.get("holler_pistol");
		helper.assertTrue(hollerTakes.containsAll(BARREL) && hollerTakes.containsAll(STOCKS)
				&& hollerTakes.containsAll(List.of("extended_magazine", "speed_magazine", "light_grip", "tactical_grip"))
				&& hollerTakes.containsAll(FrontierGunsIVGameTests.BAYONETS)
				&& hollerTakes.containsAll(List.of("medium_scope", "reflex_sight", "laser_sight")) && !hollerTakes.contains("long_scope")
				&& hollerTakes.size() == 18, "The Holler Pistol takes " + hollerTakes);
		List<String> minuetTakes = JugcraftGuns.ACCEPTS.get("minuet_pistol");
		helper.assertTrue(minuetTakes.containsAll(BARREL) && minuetTakes.contains("extended_magazine") && minuetTakes.containsAll(SIGHTS)
				&& minuetTakes.size() == 9, "The Minuet Pistol takes " + minuetTakes);
		List<String> crownTakes = JugcraftGuns.ACCEPTS.get("half_crown_revolver");
		helper.assertTrue(crownTakes.equals(JugcraftGuns.ACCEPTS.get("parley_revolver")), "The Half-Crown Revolver takes " + crownTakes);
		helper.succeed();
	}

	/**
	 * Two Minuet Pistols are two guns at once and the other hand's fires. Without a stock the Half-Crown Revolver pairs
	 * too; with a Weighted Stock fitted it is a carbine held in both hands, the other hand's gun does not fire beside it,
	 * and the stock steadies its aim; without it again it is one-handed.
	 */
	@GameTest
	public void oneHandedSidearmsPairUntilStocked(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		ItemStack minuet = loaded("minuet_pistol");
		player.setItemInHand(InteractionHand.MAIN_HAND, loaded("minuet_pistol"));
		player.setItemInHand(InteractionHand.OFF_HAND, minuet);
		helper.assertTrue(GunItem.oneHanded(minuet) && GunItem.dual(player), "Two Minuet Pistols are not two guns at once");
		helper.assertTrue(GunShots.fire(player, InteractionHand.OFF_HAND), "The other hand's Minuet Pistol did not fire");

		ItemStack crown = loaded("half_crown_revolver");
		player.setItemInHand(InteractionHand.MAIN_HAND, crown);
		player.setItemInHand(InteractionHand.OFF_HAND, loaded("minuet_pistol"));
		helper.assertTrue(GunItem.oneHanded(crown) && !GunItem.carbine(crown) && GunItem.dual(player),
				"A Half-Crown Revolver without a stock and a Minuet Pistol are not two guns at once");
		GunSpec bare = GunItem.spec(crown);
		GunItem.fit(crown, "weighted_stock");
		helper.assertTrue(GunItem.carbine(crown) && !GunItem.oneHanded(crown) && !GunItem.dual(player),
				"A Half-Crown Revolver with a stock is not a carbine held in both hands");
		helper.assertFalse(GunShots.fire(player, InteractionHand.OFF_HAND), "The other hand's gun fired beside a carbine");
		GunSpec stocked = GunItem.spec(crown);
		helper.assertTrue(stocked.aimSpread() < bare.aimSpread(), "The Weighted Stock does not steady the Half-Crown Revolver's aim");
		GunItem.remove(crown, "weighted_stock");
		helper.assertTrue(GunItem.oneHanded(crown) && GunItem.dual(player), "Without its stock the Half-Crown Revolver is not one-handed again");
		helper.succeed();
	}

	/**
	 * An empty Half-Crown Revolver with three rifle rounds in the inventory loads them a round at a time: halfway
	 * through the second round's time only the first is in, and all three once its reload ends, none left over. A shot
	 * then spends one and takes its damage from the pig.
	 */
	@GameTest(structure = GunsGameTests.ARENA, maxTicks = 160)
	public void halfCrownRevolverLoadsARoundAtATime(GameTestHelper helper) {
		GunsGameTests.floor(helper);
		Mob pig = sturdyPig(helper, new BlockPos(1, 2, 4));
		GunSpec spec = JugcraftGuns.SPECS.get("half_crown_revolver");
		ServerPlayer shooter = GunsGameTests.shooter(helper, "half_crown_revolver", 0, pig, GameType.SURVIVAL);
		shooter.getInventory().setItem(1, new ItemStack(JugcraftGuns.ROUNDS.get("rifle_round"), 3));
		helper.assertTrue(GunShots.reload(shooter), "The empty Half-Crown Revolver did not start to reload");
		helper.runAfterDelay(spec.shellStart() + spec.shellEach() + spec.shellEach() / 2, () -> helper.assertTrue(
				GunItem.loaded(shooter.getMainHandItem()) == 1, "Halfway through loading its second round the Half-Crown Revolver held "
						+ GunItem.loaded(shooter.getMainHandItem()) + ", not one"));
		helper.runAfterDelay(spec.reloadTicks(3) + 3, () -> {
			helper.assertTrue(GunItem.loaded(shooter.getMainHandItem()) == 3, "The Half-Crown Revolver loaded "
					+ GunItem.loaded(shooter.getMainHandItem()) + " rounds from three, not three");
			helper.assertTrue(GunShots.count(shooter.getInventory(), JugcraftGuns.ROUNDS.get("rifle_round")) == 0,
					"The reload left rounds in the inventory");
			helper.assertTrue(GunShots.fire(shooter), "The loaded Half-Crown Revolver did not fire");
			helper.assertTrue(GunItem.loaded(shooter.getMainHandItem()) == 2, "The shot did not spend a round");
			helper.assertTrue(Math.abs(200.0F - pig.getHealth() - spec.damage()) < 1.0E-3F,
					"The shot took " + (200.0F - pig.getHealth()) + ", not " + spec.damage());
			helper.succeed();
		});
	}

	/**
	 * An empty Holler Pistol with fifteen rifle rounds in the inventory changes its magazine: nothing is in before its
	 * reload's time, ten rounds after it, and five are left. A shot then takes its damage from the pig.
	 */
	@GameTest(structure = GunsGameTests.ARENA, maxTicks = 120)
	public void hollerPistolChangesItsMagazine(GameTestHelper helper) {
		GunsGameTests.floor(helper);
		Mob pig = sturdyPig(helper, new BlockPos(1, 2, 4));
		GunSpec spec = JugcraftGuns.SPECS.get("holler_pistol");
		ServerPlayer shooter = GunsGameTests.shooter(helper, "holler_pistol", 0, pig, GameType.SURVIVAL);
		shooter.getInventory().setItem(1, new ItemStack(JugcraftGuns.ROUNDS.get("rifle_round"), 15));
		helper.assertTrue(GunShots.reload(shooter), "The empty Holler Pistol did not start to reload");
		helper.runAfterDelay(spec.reload() - 3, () -> helper.assertTrue(GunItem.loaded(shooter.getMainHandItem()) == 0,
				"The Holler Pistol's magazine was in before its reload's time"));
		helper.runAfterDelay(spec.reload() + 2, () -> {
			helper.assertTrue(GunItem.loaded(shooter.getMainHandItem()) == 10, "The Holler Pistol loaded "
					+ GunItem.loaded(shooter.getMainHandItem()) + " rounds, not ten");
			helper.assertTrue(GunShots.count(shooter.getInventory(), JugcraftGuns.ROUNDS.get("rifle_round")) == 5,
					"The reload did not leave five rounds in the inventory");
			helper.assertTrue(GunShots.fire(shooter), "The loaded Holler Pistol did not fire");
			helper.assertTrue(Math.abs(200.0F - pig.getHealth() - spec.damage()) < 1.0E-3F,
					"The shot took " + (200.0F - pig.getHealth()) + ", not " + spec.damage());
			helper.succeed();
		});
	}

	/** A full gun of this slice's. */
	private static ItemStack loaded(String name) {
		ItemStack stack = new ItemStack(JugcraftGuns.GUNS.get(name));
		GunItem.setLoaded(stack, JugcraftGuns.SPECS.get(name).capacity());
		return stack;
	}

	/** A still pig of 200 health, so that no shot kills it. */
	private static Mob sturdyPig(GameTestHelper helper, BlockPos pos) {
		Mob pig = GunsGameTests.pig(helper, pos);
		pig.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200.0);
		pig.setHealth(200.0F);
		return pig;
	}
}
