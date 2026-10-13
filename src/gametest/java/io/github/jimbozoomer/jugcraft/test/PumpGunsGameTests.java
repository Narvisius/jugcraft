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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/**
 * In-game tests for the pump shotguns (slice 9D, docs/features/guns.md), through the server's entry points as
 * {@link GunsGameTests} does: the Sledge, the Highwayman and the Throttle fire eight pellets a shot from Buckshot
 * Shells and are loaded a shell at a time. Shooters face south (+z).
 */
public class PumpGunsGameTests {
	/** The pump shotguns, in the order the creative tab shows them. */
	static final List<String> GUNS = List.of("sledge", "highwayman", "throttle");

	/**
	 * Each fires eight pellets a shot from Buckshot Shells, a pull of the trigger at a time, and is loaded a shell at a
	 * time; each one's recipe loads. The Sledge hits hardest a shot; the Highwayman holds the most, reaches furthest and
	 * is the steadiest aimed; the Throttle fires quickest. The Highwayman and the Throttle, made with sights to swap, take
	 * the three scopes, the Sledge none; the Throttle, made with a pistol grip and no barrel parts, takes only the stocks
	 * (in its grip's place) and the scopes, the others the barrel attachments, the light grip and the bayonets too.
	 */
	@GameTest
	public void pumpShotgunsAreRegistered(GameTestHelper helper) {
		for (String name : GUNS) {
			GunSpec spec = JugcraftGuns.SPECS.get(name);
			GunItem gun = JugcraftGuns.GUNS.get(name);
			helper.assertTrue(spec != null && gun != null && gun.spec() == spec, name + " is not registered with its numbers");
			helper.assertTrue(!spec.auto() && spec.pellets() == 8 && spec.byShell() && spec.ammo().equals("buckshot_shell")
					&& JugcraftGuns.shot(gun).equals("bullet"), name + " is not a shotgun of eight pellets loaded a shell at a time");
			helper.assertTrue(helper.getLevel().recipeAccess().byKey(ResourceKey.create(Registries.RECIPE, Jugcraft.id(name))).isPresent(),
					"The " + name + " recipe does not load");
			List<String> takes = JugcraftGuns.ACCEPTS.get(name);
			boolean scoped = !name.equals("sledge");
			boolean barrel = !name.equals("throttle");
			for (String scope : List.of("long_scope", "medium_scope", "reflex_sight")) {
				helper.assertTrue(takes.contains(scope) == scoped, "The " + name + (scoped ? " does not take" : " takes") + " a " + scope);
			}
			for (String part : List.of("silencer", "extended_barrel", "light_grip", "iron_bayonet")) {
				helper.assertTrue(takes.contains(part) == barrel, "The " + name + (barrel ? " does not take" : " takes") + " a " + part);
			}
			helper.assertTrue(takes.containsAll(List.of("light_stock", "weighted_stock", "wooden_stock")),
					"The " + name + " does not take the three stocks");
		}
		GunSpec sledge = JugcraftGuns.SPECS.get("sledge");
		GunSpec highwayman = JugcraftGuns.SPECS.get("highwayman");
		GunSpec throttle = JugcraftGuns.SPECS.get("throttle");
		helper.assertTrue(sledge.damage() > highwayman.damage() && sledge.damage() > throttle.damage(), "The Sledge does not hit hardest");
		helper.assertTrue(highwayman.capacity() > sledge.capacity() && highwayman.capacity() > throttle.capacity()
				&& highwayman.range() > sledge.range() && highwayman.range() > throttle.range()
				&& highwayman.aimSpread() < sledge.aimSpread() && highwayman.aimSpread() < throttle.aimSpread(),
				"The Highwayman does not hold the most, reach furthest and aim steadiest");
		helper.assertTrue(throttle.interval() < sledge.interval() && throttle.interval() < highwayman.interval(),
				"The Throttle does not fire quickest");
		helper.succeed();
	}

	/**
	 * Side by side, each fires its one shell at a pig three blocks off: the pellets land together and the shell is spent.
	 * Then each loads a full tube a shell at a time from the inventory: one shell after its opening and one shell's time,
	 * every shell after the whole reload, and no more than its tube holds.
	 */
	@GameTest(structure = GunsGameTests.ARENA, maxTicks = 200)
	public void pumpShotgunsLandAndLoad(GameTestHelper helper) {
		GunsGameTests.floor(helper);
		Item shells = JugcraftGuns.ROUNDS.get("buckshot_shell");
		int longest = 0;
		for (int i = 0; i < GUNS.size(); i++) {
			String name = GUNS.get(i);
			GunSpec spec = JugcraftGuns.SPECS.get(name);
			Mob pig = GunsGameTests.pig(helper, new BlockPos(1 + 4 * i, 2, 4));
			pig.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200.0);
			pig.setHealth(200.0F);
			ServerPlayer shooter = shooter(helper, name, new BlockPos(1 + 4 * i, 2, 1), pig);
			helper.assertTrue(GunShots.fire(shooter), "The " + name + " did not fire");
			float taken = 200.0F - pig.getHealth();
			helper.assertTrue(taken >= spec.damage() * (spec.pellets() - 2),
					"At close range the " + name + "'s pellets took only " + taken + " (one pellet is " + spec.damage() + ")");
			helper.assertTrue(GunItem.loaded(shooter.getMainHandItem()) == 0, "The " + name + " did not spend its shell");
			shooter.getInventory().add(new ItemStack(shells, spec.capacity() + 2));
			helper.assertTrue(GunShots.reload(shooter), "The " + name + "'s reload did not start");
			helper.runAfterDelay(spec.shellStart() + spec.shellEach() + 1, () -> helper.assertTrue(
					GunItem.loaded(shooter.getMainHandItem()) == 1, "The " + name + " had not one shell in after its first shell's time"));
			helper.runAfterDelay(spec.reloadTicks(spec.capacity()) + 2, () -> {
				helper.assertTrue(GunItem.loaded(shooter.getMainHandItem()) == spec.capacity(), "The " + name + " holds "
						+ GunItem.loaded(shooter.getMainHandItem()) + " shells after its reload, not " + spec.capacity());
				helper.assertTrue(GunShots.count(shooter.getInventory(), shells) == 2,
						"The " + name + "'s reload did not take its tube's shells from the inventory");
				helper.assertFalse(GunShots.reloading(shooter), "The " + name + " was still reloading with its tube full");
			});
			longest = Math.max(longest, spec.reloadTicks(spec.capacity()));
		}
		helper.runAfterDelay(longest + 4, helper::succeed);
	}

	/**
	 * Aimed, the Highwayman keeps its pellets together furthest: at a pig thirteen blocks off all eight of its pellets
	 * land (its cone, 2.5 degrees, is narrower than the pig there), while fewer of the Sledge's do (6 degrees, wider). The
	 * pigs stand a block up, level with the eye, so no pellet strays into the floor before them.
	 */
	@GameTest(structure = GunsGameTests.ARENA, maxTicks = 20)
	public void highwaymanKeepsItsPelletsTogether(GameTestHelper helper) {
		GunsGameTests.floor(helper);
		helper.setBlock(new BlockPos(1, 2, 14), Blocks.STONE);
		helper.setBlock(new BlockPos(6, 2, 14), Blocks.STONE);
		Mob far = GunsGameTests.pig(helper, new BlockPos(1, 3, 14));
		Mob other = GunsGameTests.pig(helper, new BlockPos(6, 3, 14));
		for (Mob pig : List.of(far, other)) {
			pig.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200.0);
			pig.setHealth(200.0F);
		}
		ServerPlayer highwayman = shooter(helper, "highwayman", new BlockPos(1, 2, 1), far);
		ServerPlayer sledge = shooter(helper, "sledge", new BlockPos(6, 2, 1), other);
		for (ServerPlayer shooter : List.of(highwayman, sledge)) {
			shooter.startUsingItem(InteractionHand.MAIN_HAND);
			helper.assertTrue(GunItem.aiming(shooter, shooter.getMainHandItem()), "The shooter is not aiming");
			helper.assertTrue(GunShots.fire(shooter), "The aimed shotgun did not fire");
		}
		GunSpec spec = JugcraftGuns.SPECS.get("highwayman");
		float taken = 200.0F - far.getHealth();
		helper.assertTrue(Math.abs(taken - spec.damage() * spec.pellets()) < 1.0E-3F,
				"Aimed at thirteen blocks the Highwayman's pellets took " + taken + ", not all eight's " + spec.damage() * spec.pellets());
		GunSpec wide = JugcraftGuns.SPECS.get("sledge");
		float wideTaken = 200.0F - other.getHealth();
		helper.assertTrue(wideTaken < wide.damage() * wide.pellets() - 1.0E-3F,
				"Aimed at thirteen blocks all eight of the Sledge's pellets landed too (" + wideTaken + ")");
		helper.succeed();
	}

	/** A shooter standing at {@code at}, facing south with its look on the target's middle, holding the gun with a shell in. */
	private static ServerPlayer shooter(GameTestHelper helper, String gun, BlockPos at, Mob target) {
		ServerPlayer player = GunsGameTests.shooter(helper, gun, 1, target, GameType.SURVIVAL);
		BlockPos pos = helper.absolutePos(at);
		player.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
		Vec3 aim = target.getBoundingBox().getCenter().subtract(player.getEyePosition());
		player.setXRot((float) Math.toDegrees(Math.atan2(-aim.y, Math.sqrt(aim.x * aim.x + aim.z * aim.z))));
		return player;
	}
}
