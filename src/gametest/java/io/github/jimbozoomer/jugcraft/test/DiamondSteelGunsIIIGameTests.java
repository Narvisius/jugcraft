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
 * In-game tests for slice 15C's guns (docs/features/guns.md), three more of the owner's diamond-steel guns, through the
 * server's entry points as {@link DiamondSteelGunsGameTests} does: the Hawker Rifle, whose magazine stands out of its
 * side; the Impetus Shotgun, fed from a box of six shells; and the Quill Rifle, an automatic silenced along its whole
 * length. Each is held in both hands and fed from a box magazine. Shooters face south (+z).
 */
public class DiamondSteelGunsIIIGameTests {
	/** The slice's guns, in the order the creative tab shows them. */
	static final List<String> GUNS = List.of("hawker_rifle", "impetus_shotgun", "quill_rifle");

	/**
	 * Each is registered with its numbers and its recipe loads, held in both hands, firing plain bullets a magazine at a
	 * time. The Hawker Rifle is twelve rifle rounds, a pull a shot, piercing a point of armour; it takes what the Vesper
	 * Rifle takes, the owner's model it was made over from. The Impetus Shotgun is a box of six buckshot shells, eight
	 * pellets a pull, knocking back what it hits; it takes what the Onrush SMG takes. The Quill Rifle is thirteen light
	 * rounds as long as the trigger is held, every fourth tick, piercing a point and a half of armour; it is silenced by
	 * its build, so it takes no barrel attachment, and otherwise takes what the Gallant Rifle takes.
	 */
	@GameTest
	public void diamondSteelLongGunsAreRegistered(GameTestHelper helper) {
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
		GunSpec hawker = JugcraftGuns.SPECS.get("hawker_rifle");
		helper.assertTrue(hawker.ammo().equals("rifle_round") && hawker.capacity() == 12 && !hawker.auto() && hawker.pellets() == 1,
				"The Hawker Rifle is not a magazine of twelve rifle rounds, a pull a shot");
		helper.assertTrue(JugcraftGuns.piercing(JugcraftGuns.GUNS.get("hawker_rifle")) == 1.0F
				&& JugcraftGuns.knockback(JugcraftGuns.GUNS.get("hawker_rifle")) == 0.0F, "The Hawker Rifle does not pierce a point of armour");
		helper.assertTrue(JugcraftGuns.ACCEPTS.get("hawker_rifle").equals(JugcraftGuns.ACCEPTS.get("vesper_rifle")),
				"The Hawker Rifle takes " + JugcraftGuns.ACCEPTS.get("hawker_rifle"));
		GunSpec impetus = JugcraftGuns.SPECS.get("impetus_shotgun");
		helper.assertTrue(impetus.ammo().equals("buckshot_shell") && impetus.capacity() == 6 && impetus.pellets() == 8 && !impetus.auto(),
				"The Impetus Shotgun is not a box of six buckshot shells, eight pellets a pull");
		helper.assertTrue(JugcraftGuns.knockback(JugcraftGuns.GUNS.get("impetus_shotgun")) == 0.65F
				&& JugcraftGuns.piercing(JugcraftGuns.GUNS.get("impetus_shotgun")) == 0.0F, "The Impetus Shotgun does not knock back 0.65 further");
		helper.assertTrue(JugcraftGuns.ACCEPTS.get("impetus_shotgun").equals(JugcraftGuns.ACCEPTS.get("onrush_smg")),
				"The Impetus Shotgun takes " + JugcraftGuns.ACCEPTS.get("impetus_shotgun"));
		GunSpec quill = JugcraftGuns.SPECS.get("quill_rifle");
		helper.assertTrue(quill.ammo().equals("light_round") && quill.capacity() == 13 && quill.auto() && quill.interval() == 4,
				"The Quill Rifle is not an automatic of thirteen light rounds, every fourth tick");
		helper.assertTrue(JugcraftGuns.piercing(JugcraftGuns.GUNS.get("quill_rifle")) == 1.5F && JugcraftGuns.SILENCED.contains("quill_rifle"),
				"The Quill Rifle does not pierce a point and a half of armour, silenced by its build");
		List<String> quillTakes = JugcraftGuns.ACCEPTS.get("quill_rifle");
		helper.assertTrue(quillTakes.equals(JugcraftGuns.ACCEPTS.get("gallant_rifle")) && quillTakes.stream().noneMatch(BrassSidearmsGameTests.BARREL::contains),
				"The Quill Rifle takes " + quillTakes);
		helper.succeed();
	}

	/** An empty Hawker Rifle with twenty rifle rounds loads twelve and leaves eight; a shot takes its damage. */
	@GameTest(structure = GunsGameTests.ARENA, maxTicks = 120)
	public void hawkerRifleChangesItsMagazine(GameTestHelper helper) {
		DiamondSteelGunsGameTests.changesItsMagazine(helper, "hawker_rifle", 20);
	}

	/**
	 * An empty Quill Rifle with twenty light rounds loads thirteen and leaves seven; it fires as long as the trigger is
	 * held: two shots at once (the trigger's bank) and another each interval.
	 */
	@GameTest(structure = GunsGameTests.ARENA, maxTicks = 120)
	public void quillRifleChangesItsMagazine(GameTestHelper helper) {
		DiamondSteelGunsGameTests.changesItsMagazine(helper, "quill_rifle", 20);
	}

	/**
	 * An empty Impetus Shotgun with nine shells loads its box of six at its reload's time, not before, and leaves three; a
	 * pull lands its pellets on the pig three blocks off as one hit of a whole number of pellets, up to all eight, and
	 * two pulls at once both fire (the trigger's bank).
	 */
	@GameTest(structure = GunsGameTests.ARENA, maxTicks = 120)
	public void impetusShotgunChangesItsBox(GameTestHelper helper) {
		GunsGameTests.floor(helper);
		Mob pig = NetherGunsGameTests.sturdyPig(helper, new BlockPos(1, 2, 4));
		GunSpec spec = JugcraftGuns.SPECS.get("impetus_shotgun");
		ServerPlayer shooter = GunsGameTests.shooter(helper, "impetus_shotgun", 0, pig, GameType.SURVIVAL);
		shooter.getInventory().setItem(1, new ItemStack(JugcraftGuns.ROUNDS.get(spec.ammo()), 9));
		helper.assertTrue(GunShots.reload(shooter), "The empty Impetus Shotgun did not start to reload");
		helper.runAfterDelay(spec.reload() - 3, () -> helper.assertTrue(GunItem.loaded(shooter.getMainHandItem()) == 0,
				"The Impetus Shotgun's box was in before its reload's time"));
		helper.runAfterDelay(spec.reload() + 2, () -> {
			helper.assertTrue(GunItem.loaded(shooter.getMainHandItem()) == 6, "The Impetus Shotgun loaded "
					+ GunItem.loaded(shooter.getMainHandItem()) + " shells, not 6");
			helper.assertTrue(GunShots.count(shooter.getInventory(), JugcraftGuns.ROUNDS.get(spec.ammo())) == 3,
					"The reload did not leave three shells in the inventory");
			helper.assertTrue(GunShots.fire(shooter), "The loaded Impetus Shotgun did not fire");
			float took = 200.0F - pig.getHealth();
			float pellets = took / spec.damage();
			helper.assertTrue(took > 0.0F && pellets <= spec.pellets() + 1.0E-3F && Math.abs(pellets - Math.round(pellets)) < 1.0E-3F,
					"The shot took " + took + ", not a whole number of " + spec.damage() + "-point pellets up to " + spec.pellets());
			helper.assertTrue(GunShots.fire(shooter), "The Impetus Shotgun did not fire its second shell at once");
			helper.assertTrue(GunItem.loaded(shooter.getMainHandItem()) == 4, "Two pulls did not leave four shells");
			helper.succeed();
		});
	}
}
