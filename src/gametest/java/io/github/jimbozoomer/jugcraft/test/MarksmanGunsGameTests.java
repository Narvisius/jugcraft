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
 * In-game tests for the marksman rifles (slice 9A, docs/features/guns.md), through the server's entry points as
 * {@link GunsGameTests} does: the Picket, Ranger and Kestrel Rifles are semi-automatic and fire a rifle round a pull,
 * aim at least as steadily and reach at least as far as any gun before them, land their damage, and load from the
 * inventory in their reload time. Shooters face south (+z).
 */
public class MarksmanGunsGameTests {
	/** The marksman rifles, in the order the creative tab shows them. */
	static final List<String> RIFLES = List.of("picket_rifle", "ranger_rifle", "kestrel_rifle");

	/**
	 * Each marksman rifle fires one bullet a pull from rifle rounds; aimed, it strays no more than the Longhorn Rifle (the
	 * steadiest gun before them) and reaches at least as far. Each one's recipe loads. The Kestrel, made with sights to
	 * swap, takes the three scopes; the Picket and the Ranger, made with none, take no scope.
	 */
	@GameTest
	public void marksmanRiflesAimTrue(GameTestHelper helper) {
		GunSpec longhorn = JugcraftGuns.SPECS.get("longhorn_rifle");
		for (String name : RIFLES) {
			GunSpec spec = JugcraftGuns.SPECS.get(name);
			GunItem gun = JugcraftGuns.GUNS.get(name);
			helper.assertTrue(spec != null && gun != null && gun.spec() == spec, name + " is not registered with its numbers");
			helper.assertTrue(!spec.auto() && spec.pellets() == 1 && !spec.byShell() && spec.ammo().equals("rifle_round")
					&& JugcraftGuns.shot(gun).equals("bullet"), name + " is not a semi-automatic rifle firing a rifle round a pull");
			helper.assertTrue(spec.aimSpread() <= longhorn.aimSpread() && spec.range() >= longhorn.range(),
					name + " aims at " + spec.aimSpread() + " degrees to " + spec.range() + " blocks, wider or shorter than the Longhorn Rifle");
			helper.assertTrue(helper.getLevel().recipeAccess().byKey(ResourceKey.create(Registries.RECIPE, Jugcraft.id(name))).isPresent(),
					"The " + name + " recipe does not load");
			boolean scoped = name.equals("kestrel_rifle");
			for (String scope : List.of("long_scope", "medium_scope", "reflex_sight")) {
				helper.assertTrue(JugcraftGuns.ACCEPTS.get(name).contains(scope) == scoped,
						"The " + name + (scoped ? " does not take" : " takes") + " a " + scope);
			}
		}
		helper.succeed();
	}

	/**
	 * One shot from each lands its damage on the pig and spends a round. The Ranger's magazine reload, from one short,
	 * loads the round it lacks after its reload time and not before; the Kestrel, empty, is loaded with a clip of eight
	 * from the inventory after its own.
	 */
	@GameTest(structure = GunsGameTests.ARENA, maxTicks = 200)
	public void marksmanRiflesLandAndLoad(GameTestHelper helper) {
		GunsGameTests.floor(helper);
		Mob pig = GunsGameTests.pig(helper, new BlockPos(1, 2, 8));
		pig.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200.0);
		pig.setHealth(200.0F);
		ServerPlayer shooter = GunsGameTests.shooter(helper, "picket_rifle", 10, pig, GameType.SURVIVAL);
		// The shots are further apart than the slowest rifle's interval: a player's trigger banks two shots at most.
		int apart = 12;
		float total = 0.0F;
		for (int i = 0; i < RIFLES.size(); i++) {
			String name = RIFLES.get(i);
			total += JugcraftGuns.SPECS.get(name).damage();
			float expected = total;
			helper.runAfterDelay(1 + i * apart, () -> shootOnce(helper, shooter, pig, name, expected));
		}
		GunSpec ranger = JugcraftGuns.SPECS.get("ranger_rifle");
		GunSpec kestrel = JugcraftGuns.SPECS.get("kestrel_rifle");
		Item rounds = JugcraftGuns.ROUNDS.get("rifle_round");
		int rangerAt = 1 + RIFLES.size() * apart;
		helper.runAfterDelay(rangerAt, () -> {
			ItemStack stack = new ItemStack(JugcraftGuns.GUNS.get("ranger_rifle"));
			GunItem.setLoaded(stack, ranger.capacity() - 1);
			shooter.setItemInHand(InteractionHand.MAIN_HAND, stack);
			shooter.getInventory().add(new ItemStack(rounds, 12));
			helper.assertTrue(GunShots.reload(shooter), "The Ranger's magazine reload did not start");
		});
		helper.runAfterDelay(rangerAt + ranger.reload() - 2, () -> helper.assertTrue(
				GunItem.loaded(shooter.getMainHandItem()) == ranger.capacity() - 1, "The Ranger loaded before its reload time was up"));
		int kestrelAt = rangerAt + ranger.reload() + 2;
		helper.runAfterDelay(kestrelAt, () -> {
			helper.assertTrue(GunItem.loaded(shooter.getMainHandItem()) == ranger.capacity(), "The Ranger is not full after its reload");
			helper.assertTrue(GunShots.count(shooter.getInventory(), rounds) == 11, "The Ranger's reload did not take the one round it lacked");
			shooter.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(JugcraftGuns.GUNS.get("kestrel_rifle")));
			helper.assertTrue(GunShots.reload(shooter), "The empty Kestrel's reload did not start");
		});
		helper.runAfterDelay(kestrelAt + kestrel.reload() - 2, () -> helper.assertTrue(
				GunItem.loaded(shooter.getMainHandItem()) == 0, "The Kestrel loaded before its reload time was up"));
		helper.runAfterDelay(kestrelAt + kestrel.reload() + 2, () -> {
			helper.assertTrue(GunItem.loaded(shooter.getMainHandItem()) == kestrel.capacity(),
					"The Kestrel holds " + GunItem.loaded(shooter.getMainHandItem()) + " rounds after its reload, not a clip of " + kestrel.capacity());
			helper.assertTrue(GunShots.count(shooter.getInventory(), rounds) == 11 - kestrel.capacity(),
					"The Kestrel's reload did not take its clip from the inventory");
			helper.assertFalse(GunShots.reloading(shooter), "Still reloading after the Kestrel's reload");
			helper.succeed();
		});
	}

	/**
	 * Puts the rifle named, fully loaded, in the shooter's hand and fires it at the pig: the pig has lost the shots' total
	 * so far, and the rifle has spent a round.
	 */
	private static void shootOnce(GameTestHelper helper, ServerPlayer shooter, Mob pig, String name, float expected) {
		GunSpec spec = JugcraftGuns.SPECS.get(name);
		ItemStack stack = new ItemStack(JugcraftGuns.GUNS.get(name));
		GunItem.setLoaded(stack, spec.capacity());
		shooter.setItemInHand(InteractionHand.MAIN_HAND, stack);
		helper.assertTrue(GunShots.fire(shooter), "The loaded " + name + " did not fire");
		helper.assertTrue(Math.abs(200.0F - pig.getHealth() - expected) < 1.0E-3F,
				"After the " + name + "'s shot the pig had lost " + (200.0F - pig.getHealth()) + ", not " + expected);
		helper.assertTrue(GunItem.loaded(shooter.getMainHandItem()) == spec.capacity() - 1, "The " + name + " did not spend a round");
	}
}
