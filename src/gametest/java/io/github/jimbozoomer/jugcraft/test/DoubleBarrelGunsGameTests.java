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
 * In-game tests for the double-barrels (slice 10C, docs/features/guns.md), through the server's entry points as
 * {@link GunsGameTests} does: the Mule, the Fowler and the Culverin fire a spread of pellets, and each is loaded all at
 * once, its rounds going in as its reload ends. Shooters face south (+z).
 */
public class DoubleBarrelGunsGameTests {
	/** The double-barrels, in the order the creative tab shows them. */
	static final List<String> GUNS = List.of("mule", "fowler", "culverin");
	/** What the Fowler takes: the owner made it the Blunderbuss's grips and a part for each bayonet. */
	static final List<String> FOWLER_ATTACHMENTS = List.of("light_grip", "vertical_grip", "iron_bayonet", "steel_bayonet",
			"diamond_bayonet", "netherite_bayonet");

	/**
	 * Each fires a spread of pellets, a pull of the trigger at a time, and is loaded all at once; each one's recipe
	 * loads. The Mule holds two Buckshot Shells, the Fowler two paper cartridges and the Culverin one. The Mule loads
	 * fastest (it breaks open), the Fowler reaches furthest and takes longest to load (a ball rammed down each barrel),
	 * and the Culverin's balls hit hardest and reach least far. The Fowler takes the grips and the bayonets; the Mule and
	 * the Culverin, made with no attachment parts, take nothing.
	 */
	@GameTest
	public void doubleBarrelsAreRegistered(GameTestHelper helper) {
		for (String name : GUNS) {
			GunSpec spec = JugcraftGuns.SPECS.get(name);
			GunItem gun = JugcraftGuns.GUNS.get(name);
			helper.assertTrue(spec != null && gun != null && gun.spec() == spec, name + " is not registered with its numbers");
			helper.assertTrue(!spec.auto() && spec.pellets() > 1 && !spec.byShell() && JugcraftGuns.shot(gun).equals("bullet"),
					name + " is not a gun of pellets loaded all at once");
			helper.assertTrue(helper.getLevel().recipeAccess().byKey(ResourceKey.create(Registries.RECIPE, Jugcraft.id(name))).isPresent(),
					"The " + name + " recipe does not load");
		}
		GunSpec mule = JugcraftGuns.SPECS.get("mule");
		GunSpec fowler = JugcraftGuns.SPECS.get("fowler");
		GunSpec culverin = JugcraftGuns.SPECS.get("culverin");
		helper.assertTrue(mule.ammo().equals("buckshot_shell") && mule.capacity() == 2, "The Mule does not hold two Buckshot Shells");
		helper.assertTrue(fowler.ammo().equals("paper_cartridge") && fowler.capacity() == 2, "The Fowler does not hold two paper cartridges");
		helper.assertTrue(culverin.ammo().equals("paper_cartridge") && culverin.capacity() == 1, "The Culverin does not hold one paper cartridge");
		helper.assertTrue(mule.reload() < fowler.reload() && mule.reload() < culverin.reload(), "The Mule does not load fastest");
		helper.assertTrue(culverin.damage() > mule.damage() && culverin.damage() > fowler.damage()
				&& culverin.range() < mule.range() && culverin.range() < fowler.range(),
				"The Culverin's balls do not hit hardest and reach least far");
		helper.assertTrue(fowler.range() > mule.range() && fowler.range() > culverin.range(), "The Fowler does not reach furthest");
		helper.assertTrue(fowler.reload() > mule.reload() && fowler.reload() > culverin.reload(), "The Fowler does not take longest to load");
		helper.assertTrue(FOWLER_ATTACHMENTS.equals(JugcraftGuns.ACCEPTS.get("fowler")), "The Fowler does not take the grips and the bayonets");
		helper.assertTrue(!JugcraftGuns.ACCEPTS.containsKey("mule") && !JugcraftGuns.ACCEPTS.containsKey("culverin"),
				"The Mule or the Culverin takes an attachment");
		helper.succeed();
	}

	/**
	 * Side by side, each fires at a pig three blocks off: its pellets land together and a round is spent; the Mule and
	 * the Fowler then fire their second barrel. Each then loads from the inventory: halfway through its reload nothing is
	 * in yet, and as it ends every round is, no more than it holds.
	 */
	@GameTest(structure = GunsGameTests.ARENA, maxTicks = 200)
	public void doubleBarrelsFireBothBarrelsAndLoad(GameTestHelper helper) {
		GunsGameTests.floor(helper);
		int longest = 0;
		for (int i = 0; i < GUNS.size(); i++) {
			String name = GUNS.get(i);
			GunSpec spec = JugcraftGuns.SPECS.get(name);
			Item rounds = JugcraftGuns.ROUNDS.get(spec.ammo());
			Mob pig = GunsGameTests.pig(helper, new BlockPos(1 + 4 * i, 2, 4));
			pig.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200.0);
			pig.setHealth(200.0F);
			ServerPlayer shooter = shooter(helper, name, new BlockPos(1 + 4 * i, 2, 1), pig, spec.capacity());
			helper.assertTrue(GunShots.fire(shooter), "The " + name + " did not fire");
			float taken = 200.0F - pig.getHealth();
			helper.assertTrue(taken >= spec.damage() * (spec.pellets() - 2),
					"At close range the " + name + "'s pellets took only " + taken + " (one pellet is " + spec.damage() + ")");
			helper.assertTrue(GunItem.loaded(shooter.getMainHandItem()) == spec.capacity() - 1, "The " + name + " did not spend a round");
			int load = spec.interval() + 1;
			if (spec.capacity() == 2) {
				helper.runAfterDelay(load, () -> {
					helper.assertTrue(GunShots.fire(shooter), "The " + name + " did not fire its second barrel");
					helper.assertTrue(GunItem.loaded(shooter.getMainHandItem()) == 0, "The " + name + " did not spend its second round");
				});
				load += spec.interval() + 1;
			}
			helper.runAfterDelay(load, () -> {
				shooter.getInventory().add(new ItemStack(rounds, spec.capacity() + 2));
				helper.assertTrue(GunShots.reload(shooter), "The " + name + "'s reload did not start");
			});
			helper.runAfterDelay(load + spec.reload() / 2, () -> helper.assertTrue(GunItem.loaded(shooter.getMainHandItem()) == 0,
					"The " + name + " had a round in halfway through its reload"));
			helper.runAfterDelay(load + spec.reload() + 2, () -> {
				helper.assertTrue(GunItem.loaded(shooter.getMainHandItem()) == spec.capacity(), "The " + name + " holds "
						+ GunItem.loaded(shooter.getMainHandItem()) + " rounds after its reload, not " + spec.capacity());
				helper.assertTrue(GunShots.count(shooter.getInventory(), rounds) == 2,
						"The " + name + "'s reload did not take its rounds from the inventory");
				helper.assertFalse(GunShots.reloading(shooter), "The " + name + " was still reloading, full");
			});
			longest = Math.max(longest, load + spec.reload() + 2);
		}
		helper.runAfterDelay(longest + 2, helper::succeed);
	}

	/** A shooter standing at {@code at}, facing south with its look on the target's middle, holding the gun loaded. */
	private static ServerPlayer shooter(GameTestHelper helper, String gun, BlockPos at, Mob target, int loaded) {
		ServerPlayer player = GunsGameTests.shooter(helper, gun, loaded, target, GameType.SURVIVAL);
		BlockPos pos = helper.absolutePos(at);
		player.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
		Vec3 aim = target.getBoundingBox().getCenter().subtract(player.getEyePosition());
		player.setXRot((float) Math.toDegrees(Math.atan2(-aim.y, Math.sqrt(aim.x * aim.x + aim.z * aim.z))));
		return player;
	}
}
