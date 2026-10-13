package io.github.jimbozoomer.jugcraft.test;

import com.mojang.serialization.JsonOps;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.guns.GunItem;
import io.github.jimbozoomer.jugcraft.guns.GunShots;
import io.github.jimbozoomer.jugcraft.guns.GunSpec;
import io.github.jimbozoomer.jugcraft.guns.JugcraftGuns;
import java.util.List;
import java.util.Optional;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/**
 * In-game tests for the guns (slices 1 to 4, docs/features/guns.md), worked through the server's own entry points (the ones
 * the trigger and reload payloads call): GunShots.fire and GunShots.reload. A shot hurts what it is aimed at by the
 * gun's damage and spends a round; an empty gun, an empty hand and a too-fast trigger do not fire; a creative player
 * spends nothing; a magazine reload takes the gun's reload time and loads from the inventory; a shell-at-a-time reload
 * loads one round per shell's time, and a shot or a switch of item cuts it short; a shotgun's pellets land together;
 * bullets count as projectiles; the iron set's pistol and SMG land their damage, and the Haymaker loads only the shells
 * there are; the lever rifles land theirs and load a round at a time, and the Coach Gun's pellets land together; a
 * muzzle-loader lands its one heavy shot and reloads a paper cartridge, and the Bellmouth's balls land together; the
 * hand guns of slice 8 land theirs, the Bulldog loads its one round in its reload time and the Marshal a round at a time;
 * the service arms of slice 8B land theirs, the Sentry's magazine reloads and the Garrison fires as fast as it may.
 * Attachments (slice 5) fit and come off in a crafting grid, change a gun's numbers, and the server fires and loads by
 * them; the scopes (slice 7) fit the guns the owner made to take one. Shooters face south (+z) and aim at their
 * target's middle.
 */
public class GunsGameTests {
	static final String ARENA = "jugcraft-test:arms_arena";

	/** Every gun and round is registered, each gun as a GunItem with its numbers, and every gun's round exists. */
	@GameTest
	public void everyGunIsRegistered(GameTestHelper helper) {
		JugcraftGuns.SPECS.forEach((name, spec) -> {
			helper.assertTrue(JugcraftGuns.GUNS.get(name) != null && JugcraftGuns.GUNS.get(name).spec() == spec,
					name + " is not registered with its numbers");
			helper.assertTrue(JugcraftGuns.ammo(spec) != null, name + " fires " + spec.ammo() + ", which is not registered");
			helper.assertTrue(new ItemStack(JugcraftGuns.GUNS.get(name)).getMaxStackSize() == 1, name + " stacks");
		});
		helper.assertTrue(JugcraftGuns.GUNS.size() == 54 && JugcraftGuns.ROUNDS.size() == 4, "Not fifty-four guns and four rounds");
		helper.assertTrue(JugcraftGuns.ATTACHMENT_ITEMS.keySet().equals(JugcraftGuns.ATTACHMENTS.keySet())
				&& JugcraftGuns.ATTACHMENTS.size() == 20, "Not twenty attachments, each with its item");
		JugcraftGuns.ACCEPTS.forEach((gun, takes) -> helper.assertTrue(JugcraftGuns.GUNS.containsKey(gun)
				&& JugcraftGuns.ATTACHMENTS.keySet().containsAll(takes), gun + " takes an unknown attachment"));
		helper.succeed();
	}

	/** A Patchwork Carbine's shot hurts the pig it is aimed at by the carbine's damage and spends a round. */
	@GameTest(structure = ARENA, maxTicks = 20)
	public void carbineShotHurtsWhatItAimsAt(GameTestHelper helper) {
		floor(helper);
		Mob pig = pig(helper, new BlockPos(1, 2, 8));
		ServerPlayer shooter = shooter(helper, "patchwork_carbine", 10, pig, GameType.SURVIVAL);
		float max = pig.getHealth();
		helper.assertTrue(GunShots.fire(shooter), "The loaded carbine did not fire");
		GunSpec spec = JugcraftGuns.SPECS.get("patchwork_carbine");
		helper.assertTrue(Math.abs(max - pig.getHealth() - spec.damage()) < 1.0E-3F,
				"The shot took " + (max - pig.getHealth()) + " from the pig, not " + spec.damage());
		helper.assertTrue(GunItem.loaded(shooter.getMainHandItem()) == 9, "The shot did not spend one round");
		helper.succeed();
	}

	/** An empty gun does not fire, and nothing but a gun fires. */
	@GameTest(structure = ARENA, maxTicks = 20)
	public void emptyGunAndEmptyHandDoNotFire(GameTestHelper helper) {
		floor(helper);
		Mob pig = pig(helper, new BlockPos(1, 2, 8));
		ServerPlayer shooter = shooter(helper, "patchwork_carbine", 0, pig, GameType.SURVIVAL);
		float max = pig.getHealth();
		helper.assertFalse(GunShots.fire(shooter), "The empty carbine fired");
		shooter.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STICK));
		helper.assertFalse(GunShots.fire(shooter), "A stick fired");
		helper.assertTrue(pig.getHealth() == max, "The pig was hurt");
		helper.succeed();
	}

	/**
	 * The trigger keeps the gun's rate: two shots may be banked (both land, despite the hit cooldown), a third at once is
	 * refused, and one interval later it fires.
	 */
	@GameTest(structure = ARENA, maxTicks = 40)
	public void triggerKeepsTheRate(GameTestHelper helper) {
		floor(helper);
		// Three blocks off, so even the Midge's hip spread (4 degrees) keeps every bullet on the pig.
		Mob pig = pig(helper, new BlockPos(1, 2, 4));
		pig.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200.0);
		pig.setHealth(200.0F);
		ServerPlayer shooter = shooter(helper, "rust_midge", 20, pig, GameType.SURVIVAL);
		helper.assertTrue(GunShots.fire(shooter) && GunShots.fire(shooter), "The first two shots were refused");
		helper.assertFalse(GunShots.fire(shooter), "A third shot in the same tick fired");
		// Both shots land: a bullet passes the half second a creature is shielded after a hit.
		float damage = JugcraftGuns.SPECS.get("rust_midge").damage();
		helper.assertTrue(Math.abs(200.0F - pig.getHealth() - 2 * damage) < 1.0E-3F,
				"Two quick shots took " + (200.0F - pig.getHealth()) + ", not " + 2 * damage);
		int interval = JugcraftGuns.SPECS.get("rust_midge").interval();
		helper.runAfterDelay(interval, () -> {
			helper.assertTrue(GunShots.fire(shooter), "The trigger did not fire again after its interval");
			helper.assertTrue(GunItem.loaded(shooter.getMainHandItem()) == 17, "Three shots did not spend three rounds");
			helper.succeed();
		});
	}

	/** A creative player fires an empty gun and spends nothing. */
	@GameTest(structure = ARENA, maxTicks = 20)
	public void creativeSpendsNothing(GameTestHelper helper) {
		floor(helper);
		Mob pig = pig(helper, new BlockPos(1, 2, 8));
		ServerPlayer shooter = shooter(helper, "patchwork_carbine", 0, pig, GameType.CREATIVE);
		float max = pig.getHealth();
		helper.assertTrue(GunShots.fire(shooter), "The creative player's carbine did not fire");
		helper.assertTrue(pig.getHealth() < max, "The creative shot missed");
		helper.assertTrue(GunItem.loaded(shooter.getMainHandItem()) == 0, "The creative shot spent a round");
		helper.succeed();
	}

	/** A magazine reload takes the gun's reload time, then fills it from the inventory's rounds. */
	@GameTest(structure = ARENA, maxTicks = 80)
	public void magazineReloadLoadsFromTheInventory(GameTestHelper helper) {
		floor(helper);
		Mob pig = pig(helper, new BlockPos(1, 2, 8));
		ServerPlayer shooter = shooter(helper, "patchwork_carbine", 2, pig, GameType.SURVIVAL);
		shooter.getInventory().add(new ItemStack(JugcraftGuns.ROUNDS.get("rifle_round"), 15));
		helper.assertTrue(GunShots.reload(shooter), "The reload did not start");
		helper.assertFalse(GunShots.reload(shooter), "A second reload started during the first");
		int ticks = JugcraftGuns.SPECS.get("patchwork_carbine").reload();
		helper.runAfterDelay(ticks - 2, () -> helper.assertTrue(GunItem.loaded(shooter.getMainHandItem()) == 2,
				"The carbine loaded before its reload time was up"));
		helper.runAfterDelay(ticks + 2, () -> {
			helper.assertTrue(GunItem.loaded(shooter.getMainHandItem()) == 10, "The carbine is not full after its reload");
			helper.assertTrue(GunShots.count(shooter.getInventory(), JugcraftGuns.ROUNDS.get("rifle_round")) == 7,
					"The reload did not take 8 rounds from the inventory");
			helper.assertFalse(GunShots.reloading(shooter), "Still reloading after the reload");
			helper.succeed();
		});
	}

	/** With no rounds to load, a reload does not start; a full gun does not reload. */
	@GameTest(structure = ARENA, maxTicks = 20)
	public void reloadNeedsRoundsAndRoom(GameTestHelper helper) {
		floor(helper);
		Mob pig = pig(helper, new BlockPos(1, 2, 8));
		ServerPlayer shooter = shooter(helper, "rust_midge", 5, pig, GameType.SURVIVAL);
		helper.assertFalse(GunShots.reload(shooter), "A reload started with no rounds to load");
		shooter.getInventory().add(new ItemStack(JugcraftGuns.ROUNDS.get("light_round"), 64));
		GunItem.setLoaded(shooter.getMainHandItem(), 20);
		helper.assertFalse(GunShots.reload(shooter), "A full gun started a reload");
		helper.succeed();
	}

	/** The Thunderpipe loads a shell after each shell's time, one at a time, and closes after the last. */
	@GameTest(structure = ARENA, maxTicks = 80)
	public void shellReloadLoadsOneAtATime(GameTestHelper helper) {
		floor(helper);
		Mob pig = pig(helper, new BlockPos(1, 2, 8));
		ServerPlayer shooter = shooter(helper, "thunderpipe", 0, pig, GameType.SURVIVAL);
		shooter.getInventory().add(new ItemStack(JugcraftGuns.ROUNDS.get("buckshot_shell"), 5));
		GunSpec spec = JugcraftGuns.SPECS.get("thunderpipe");
		helper.assertTrue(GunShots.reload(shooter), "The reload did not start");
		helper.runAfterDelay(spec.shellStart() + spec.shellEach() + 1, () -> helper.assertTrue(
				GunItem.loaded(shooter.getMainHandItem()) == 1, "One shell's time in, not one shell loaded"));
		helper.runAfterDelay(spec.reloadTicks(2) + 2, () -> {
			helper.assertTrue(GunItem.loaded(shooter.getMainHandItem()) == 2, "Both barrels are not loaded");
			helper.assertTrue(GunShots.count(shooter.getInventory(), JugcraftGuns.ROUNDS.get("buckshot_shell")) == 3,
					"The reload did not take two shells");
			helper.assertFalse(GunShots.reloading(shooter), "Still reloading after the last shell");
			helper.succeed();
		});
	}

	/** A shot cuts a shell-at-a-time reload short, and fires what is loaded. */
	@GameTest(structure = ARENA, maxTicks = 40)
	public void shotCutsAShellReloadShort(GameTestHelper helper) {
		floor(helper);
		Mob pig = pig(helper, new BlockPos(1, 2, 8));
		ServerPlayer shooter = shooter(helper, "thunderpipe", 1, pig, GameType.SURVIVAL);
		shooter.getInventory().add(new ItemStack(JugcraftGuns.ROUNDS.get("buckshot_shell"), 5));
		helper.assertTrue(GunShots.reload(shooter), "The reload did not start");
		helper.runAfterDelay(2, () -> {
			helper.assertTrue(GunShots.fire(shooter), "The loaded Thunderpipe did not fire during its reload");
			helper.assertFalse(GunShots.reloading(shooter), "The shot did not cut the reload short");
			helper.assertTrue(GunItem.loaded(shooter.getMainHandItem()) == 0, "The shot did not spend the loaded shell");
			helper.succeed();
		});
	}

	/** Switching to another item stops a reload: the gun is not loaded, and no rounds are taken. */
	@GameTest(structure = ARENA, maxTicks = 80)
	public void switchingAwayStopsAReload(GameTestHelper helper) {
		floor(helper);
		Mob pig = pig(helper, new BlockPos(1, 2, 8));
		ServerPlayer shooter = shooter(helper, "rust_midge", 0, pig, GameType.SURVIVAL);
		shooter.getInventory().add(new ItemStack(JugcraftGuns.ROUNDS.get("light_round"), 30));
		helper.assertTrue(GunShots.reload(shooter), "The reload did not start");
		ItemStack gun = shooter.getMainHandItem();
		shooter.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STICK));
		helper.runAfterDelay(JugcraftGuns.SPECS.get("rust_midge").reload() + 2, () -> {
			helper.assertFalse(GunShots.reloading(shooter), "The reload went on after switching away");
			helper.assertTrue(GunItem.loaded(gun) == 0, "The gun was loaded after switching away");
			helper.assertTrue(GunShots.count(shooter.getInventory(), JugcraftGuns.ROUNDS.get("light_round")) == 30,
					"Rounds were taken after switching away");
			helper.succeed();
		});
	}

	/** At close range a Thunderpipe's pellets land together: far more than one pellet's damage in one shot. */
	@GameTest(structure = ARENA, maxTicks = 20)
	public void pelletsLandTogether(GameTestHelper helper) {
		floor(helper);
		Mob pig = pig(helper, new BlockPos(1, 2, 4));
		pig.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100.0);
		pig.setHealth(100.0F);
		ServerPlayer shooter = shooter(helper, "thunderpipe", 2, pig, GameType.SURVIVAL);
		helper.assertTrue(GunShots.fire(shooter), "The Thunderpipe did not fire");
		GunSpec spec = JugcraftGuns.SPECS.get("thunderpipe");
		float taken = 100.0F - pig.getHealth();
		helper.assertTrue(taken >= spec.damage() * (spec.pellets() - 2),
				"At close range the pellets took only " + taken + " (one pellet is " + spec.damage() + ")");
		helper.succeed();
	}

	/**
	 * The iron set's Warden Pistol and Riveter SMG each land one shot's damage on what they aim at and spend a round (one
	 * shooter, the second gun after the first's interval).
	 */
	@GameTest(structure = ARENA, maxTicks = 40)
	public void ironSetShotsLand(GameTestHelper helper) {
		floor(helper);
		Mob pig = pig(helper, new BlockPos(1, 2, 4));
		pig.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200.0);
		pig.setHealth(200.0F);
		GunSpec pistol = JugcraftGuns.SPECS.get("warden_pistol");
		GunSpec smg = JugcraftGuns.SPECS.get("riveter_smg");
		ServerPlayer shooter = shooter(helper, "warden_pistol", pistol.capacity(), pig, GameType.SURVIVAL);
		helper.assertTrue(GunShots.fire(shooter), "The loaded Warden Pistol did not fire");
		helper.assertTrue(Math.abs(200.0F - pig.getHealth() - pistol.damage()) < 1.0E-3F,
				"The pistol took " + (200.0F - pig.getHealth()) + ", not " + pistol.damage());
		helper.assertTrue(GunItem.loaded(shooter.getMainHandItem()) == pistol.capacity() - 1, "The pistol did not spend one round");
		helper.runAfterDelay(pistol.interval() + 1, () -> {
			ItemStack stack = new ItemStack(JugcraftGuns.GUNS.get("riveter_smg"));
			GunItem.setLoaded(stack, smg.capacity());
			shooter.setItemInHand(InteractionHand.MAIN_HAND, stack);
			helper.assertTrue(GunShots.fire(shooter), "The loaded Riveter SMG did not fire");
			float expected = pistol.damage() + smg.damage();
			helper.assertTrue(Math.abs(200.0F - pig.getHealth() - expected) < 1.0E-3F,
					"After the SMG the pig had lost " + (200.0F - pig.getHealth()) + ", not " + expected);
			helper.assertTrue(GunItem.loaded(shooter.getMainHandItem()) == smg.capacity() - 1, "The SMG did not spend one round");
			helper.succeed();
		});
	}

	/**
	 * The Haymaker loads a shell at a time and stops at the shells there are (two of its five), then its pellets land
	 * together at close range.
	 */
	@GameTest(structure = ARENA, maxTicks = 120)
	public void haymakerLoadsTheShellsThereAre(GameTestHelper helper) {
		floor(helper);
		Mob pig = pig(helper, new BlockPos(1, 2, 4));
		pig.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200.0);
		pig.setHealth(200.0F);
		ServerPlayer shooter = shooter(helper, "haymaker", 0, pig, GameType.SURVIVAL);
		shooter.getInventory().add(new ItemStack(JugcraftGuns.ROUNDS.get("buckshot_shell"), 2));
		GunSpec spec = JugcraftGuns.SPECS.get("haymaker");
		helper.assertTrue(spec.byShell(), "The Haymaker does not load a shell at a time");
		helper.assertTrue(GunShots.reload(shooter), "The reload did not start");
		helper.runAfterDelay(spec.reloadTicks(2) + 2, () -> {
			helper.assertTrue(GunItem.loaded(shooter.getMainHandItem()) == 2, "The Haymaker did not load both shells");
			helper.assertTrue(GunShots.count(shooter.getInventory(), JugcraftGuns.ROUNDS.get("buckshot_shell")) == 0,
					"Shells were left in the inventory");
			helper.assertFalse(GunShots.reloading(shooter), "Still reloading with no shells left");
			helper.assertTrue(GunShots.fire(shooter), "The loaded Haymaker did not fire");
			float taken = 200.0F - pig.getHealth();
			helper.assertTrue(taken >= spec.damage() * (spec.pellets() - 2),
					"At close range the pellets took only " + taken + " (one pellet is " + spec.damage() + ")");
			helper.succeed();
		});
	}

	/**
	 * The lever rifles each land one shot's damage and spend a round (one shooter, the second rifle after the first's
	 * interval), and the Drover loads a round at a time.
	 */
	@GameTest(structure = ARENA, maxTicks = 120)
	public void leverRiflesLandAndLoadByTheRound(GameTestHelper helper) {
		floor(helper);
		Mob pig = pig(helper, new BlockPos(1, 2, 8));
		pig.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200.0);
		pig.setHealth(200.0F);
		GunSpec longhorn = JugcraftGuns.SPECS.get("longhorn_rifle");
		GunSpec drover = JugcraftGuns.SPECS.get("drover_rifle");
		ServerPlayer shooter = shooter(helper, "longhorn_rifle", 1, pig, GameType.SURVIVAL);
		helper.assertTrue(GunShots.fire(shooter), "The loaded Longhorn did not fire");
		helper.assertTrue(Math.abs(200.0F - pig.getHealth() - longhorn.damage()) < 1.0E-3F,
				"The Longhorn took " + (200.0F - pig.getHealth()) + ", not " + longhorn.damage());
		helper.assertTrue(GunItem.loaded(shooter.getMainHandItem()) == 0, "The Longhorn did not spend its round");
		helper.runAfterDelay(longhorn.interval() + 1, () -> {
			ItemStack stack = new ItemStack(JugcraftGuns.GUNS.get("drover_rifle"));
			GunItem.setLoaded(stack, 1);
			shooter.setItemInHand(InteractionHand.MAIN_HAND, stack);
			helper.assertTrue(GunShots.fire(shooter), "The loaded Drover did not fire");
			float expected = longhorn.damage() + drover.damage();
			helper.assertTrue(Math.abs(200.0F - pig.getHealth() - expected) < 1.0E-3F,
					"After the Drover the pig had lost " + (200.0F - pig.getHealth()) + ", not " + expected);
			shooter.getInventory().add(new ItemStack(JugcraftGuns.ROUNDS.get("rifle_round"), 3));
			helper.assertTrue(drover.byShell() && GunShots.reload(shooter), "The Drover's round-at-a-time reload did not start");
			helper.runAfterDelay(drover.shellStart() + drover.shellEach() + 1, () -> helper.assertTrue(
					GunItem.loaded(shooter.getMainHandItem()) == 1, "One round's time in, not one round loaded"));
		});
		helper.runAfterDelay(longhorn.interval() + 1 + drover.reloadTicks(3) + 3, () -> {
			helper.assertTrue(GunItem.loaded(shooter.getMainHandItem()) == 3, "The Drover did not load the three rounds");
			helper.assertFalse(GunShots.reloading(shooter), "Still reloading after the last round");
			helper.succeed();
		});
	}

	/** At close range the Coach Gun's pellets land together. */
	@GameTest(structure = ARENA, maxTicks = 20)
	public void coachGunPelletsLand(GameTestHelper helper) {
		floor(helper);
		Mob pig = pig(helper, new BlockPos(1, 2, 4));
		pig.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200.0);
		pig.setHealth(200.0F);
		ServerPlayer shooter = shooter(helper, "coach_gun", 2, pig, GameType.SURVIVAL);
		helper.assertTrue(GunShots.fire(shooter), "The Coach Gun did not fire");
		GunSpec spec = JugcraftGuns.SPECS.get("coach_gun");
		float taken = 200.0F - pig.getHealth();
		helper.assertTrue(taken >= spec.damage() * (spec.pellets() - 2),
				"At close range the pellets took only " + taken + " (one pellet is " + spec.damage() + ")");
		helper.assertTrue(GunItem.loaded(shooter.getMainHandItem()) == 1, "The shot did not spend one barrel");
		helper.succeed();
	}

	/** The Line Musket lands its one heavy shot, then takes its long reload to load a paper cartridge, and not before. */
	@GameTest(structure = ARENA, maxTicks = 120)
	public void musketShootsOnceAndReloadsACartridge(GameTestHelper helper) {
		floor(helper);
		Mob pig = pig(helper, new BlockPos(1, 2, 8));
		pig.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200.0);
		pig.setHealth(200.0F);
		GunSpec spec = JugcraftGuns.SPECS.get("line_musket");
		ServerPlayer shooter = shooter(helper, "line_musket", 1, pig, GameType.SURVIVAL);
		shooter.getInventory().add(new ItemStack(JugcraftGuns.ROUNDS.get("paper_cartridge"), 3));
		helper.assertTrue(GunShots.fire(shooter), "The loaded musket did not fire");
		helper.assertTrue(Math.abs(200.0F - pig.getHealth() - spec.damage()) < 1.0E-3F,
				"The musket took " + (200.0F - pig.getHealth()) + ", not " + spec.damage());
		helper.assertFalse(GunShots.fire(shooter), "The empty musket fired again");
		helper.assertTrue(GunShots.reload(shooter), "The reload did not start");
		helper.runAfterDelay(spec.reload() - 2, () -> helper.assertTrue(GunItem.loaded(shooter.getMainHandItem()) == 0,
				"The musket loaded before its reload time was up"));
		helper.runAfterDelay(spec.reload() + 2, () -> {
			helper.assertTrue(GunItem.loaded(shooter.getMainHandItem()) == 1, "The musket is not loaded after its reload");
			helper.assertTrue(GunShots.count(shooter.getInventory(), JugcraftGuns.ROUNDS.get("paper_cartridge")) == 2,
					"The reload did not take one cartridge");
			helper.succeed();
		});
	}

	/** At close range the Bellmouth's balls land together. */
	@GameTest(structure = ARENA, maxTicks = 20)
	public void bellmouthBallsLandTogether(GameTestHelper helper) {
		floor(helper);
		Mob pig = pig(helper, new BlockPos(1, 2, 3));
		pig.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200.0);
		pig.setHealth(200.0F);
		ServerPlayer shooter = shooter(helper, "bellmouth", 1, pig, GameType.SURVIVAL);
		helper.assertTrue(GunShots.fire(shooter), "The Bellmouth did not fire");
		GunSpec spec = JugcraftGuns.SPECS.get("bellmouth");
		float taken = 200.0F - pig.getHealth();
		helper.assertTrue(taken >= spec.damage() * (spec.pellets() - 3),
				"At close range the balls took only " + taken + " (one ball is " + spec.damage() + ")");
		helper.succeed();
	}

	/**
	 * Slice 8's hand guns each land one shot's damage: the Bulldog Pistol fires its one rifle round and takes its reload
	 * to load another, and not before; the Marshal Revolver loads a round at a time, only the rounds there are; then the
	 * Sapper Revolver lands its shot.
	 */
	@GameTest(structure = ARENA, maxTicks = 160)
	public void handGunsLandAndLoad(GameTestHelper helper) {
		floor(helper);
		Mob pig = pig(helper, new BlockPos(1, 2, 8));
		pig.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200.0);
		pig.setHealth(200.0F);
		GunSpec bulldog = JugcraftGuns.SPECS.get("bulldog_pistol");
		GunSpec marshal = JugcraftGuns.SPECS.get("marshal_revolver");
		GunSpec sapper = JugcraftGuns.SPECS.get("sapper_revolver");
		ServerPlayer shooter = shooter(helper, "bulldog_pistol", 1, pig, GameType.SURVIVAL);
		shooter.getInventory().add(new ItemStack(JugcraftGuns.ROUNDS.get("rifle_round"), 3));
		helper.assertTrue(GunShots.fire(shooter), "The loaded Bulldog did not fire");
		helper.assertTrue(Math.abs(200.0F - pig.getHealth() - bulldog.damage()) < 1.0E-3F,
				"The Bulldog took " + (200.0F - pig.getHealth()) + ", not " + bulldog.damage());
		helper.assertFalse(GunShots.fire(shooter), "The empty Bulldog fired again");
		helper.assertTrue(!bulldog.byShell() && GunShots.reload(shooter), "The Bulldog's reload did not start");
		helper.runAfterDelay(bulldog.reload() - 2, () -> helper.assertTrue(GunItem.loaded(shooter.getMainHandItem()) == 0,
				"The Bulldog loaded before its reload time was up"));
		int marshalAt = bulldog.reload() + 2;
		helper.runAfterDelay(marshalAt, () -> {
			helper.assertTrue(GunItem.loaded(shooter.getMainHandItem()) == 1, "The Bulldog is not loaded after its reload");
			helper.assertTrue(GunShots.count(shooter.getInventory(), JugcraftGuns.ROUNDS.get("rifle_round")) == 2,
					"The Bulldog's reload did not take one rifle round");
			ItemStack stack = new ItemStack(JugcraftGuns.GUNS.get("marshal_revolver"));
			GunItem.setLoaded(stack, 1);
			shooter.setItemInHand(InteractionHand.MAIN_HAND, stack);
			helper.assertTrue(GunShots.fire(shooter), "The loaded Marshal did not fire");
			float expected = bulldog.damage() + marshal.damage();
			helper.assertTrue(Math.abs(200.0F - pig.getHealth() - expected) < 1.0E-3F,
					"After the Marshal the pig had lost " + (200.0F - pig.getHealth()) + ", not " + expected);
			shooter.getInventory().add(new ItemStack(JugcraftGuns.ROUNDS.get("light_round"), 2));
			helper.assertTrue(marshal.byShell() && GunShots.reload(shooter), "The Marshal's round-at-a-time reload did not start");
			helper.runAfterDelay(marshal.shellStart() + marshal.shellEach() + 1, () -> helper.assertTrue(
					GunItem.loaded(shooter.getMainHandItem()) == 1, "One round's time in, not one round loaded"));
		});
		int sapperAt = marshalAt + marshal.reloadTicks(2) + 4;
		helper.runAfterDelay(sapperAt, () -> {
			helper.assertTrue(GunItem.loaded(shooter.getMainHandItem()) == 2, "The Marshal did not load the two rounds there were");
			helper.assertFalse(GunShots.reloading(shooter), "The Marshal is still reloading with no rounds left");
			ItemStack stack = new ItemStack(JugcraftGuns.GUNS.get("sapper_revolver"));
			GunItem.setLoaded(stack, 1);
			shooter.setItemInHand(InteractionHand.MAIN_HAND, stack);
			helper.assertTrue(GunShots.fire(shooter), "The loaded Sapper did not fire");
			float expected = bulldog.damage() + marshal.damage() + sapper.damage();
			helper.assertTrue(Math.abs(200.0F - pig.getHealth() - expected) < 1.0E-3F,
					"After the Sapper the pig had lost " + (200.0F - pig.getHealth()) + ", not " + expected);
			helper.succeed();
		});
	}

	/**
	 * Slice 8B's service arms: the Sentry Pistol lands its shot, and its magazine reload loads from the inventory in its
	 * reload time and not before; the Garrison Rifle's shots land one after another, as fast as its interval allows; at
	 * close range the Breacher's pellets land together.
	 */
	@GameTest(structure = ARENA, maxTicks = 100)
	public void serviceArmsLandAndLoad(GameTestHelper helper) {
		floor(helper);
		Mob pig = pig(helper, new BlockPos(1, 2, 4));
		pig.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200.0);
		pig.setHealth(200.0F);
		GunSpec sentry = JugcraftGuns.SPECS.get("sentry_pistol");
		GunSpec garrison = JugcraftGuns.SPECS.get("garrison_rifle");
		GunSpec breacher = JugcraftGuns.SPECS.get("breacher");
		ServerPlayer shooter = shooter(helper, "sentry_pistol", sentry.capacity(), pig, GameType.SURVIVAL);
		shooter.getInventory().add(new ItemStack(JugcraftGuns.ROUNDS.get("light_round"), 3));
		helper.assertTrue(GunShots.fire(shooter), "The loaded Sentry Pistol did not fire");
		helper.assertTrue(Math.abs(200.0F - pig.getHealth() - sentry.damage()) < 1.0E-3F,
				"The Sentry took " + (200.0F - pig.getHealth()) + ", not " + sentry.damage());
		helper.assertTrue(!sentry.byShell() && GunShots.reload(shooter), "The Sentry's magazine reload did not start");
		helper.runAfterDelay(sentry.reload() - 2, () -> helper.assertTrue(
				GunItem.loaded(shooter.getMainHandItem()) == sentry.capacity() - 1, "The Sentry loaded before its reload time was up"));
		int garrisonAt = sentry.reload() + 2;
		helper.runAfterDelay(garrisonAt, () -> {
			helper.assertTrue(GunItem.loaded(shooter.getMainHandItem()) == sentry.capacity(), "The Sentry is not full after its reload");
			helper.assertTrue(GunShots.count(shooter.getInventory(), JugcraftGuns.ROUNDS.get("light_round")) == 2,
					"The Sentry's reload did not take the one round it was short");
			ItemStack stack = new ItemStack(JugcraftGuns.GUNS.get("garrison_rifle"));
			GunItem.setLoaded(stack, garrison.capacity());
			shooter.setItemInHand(InteractionHand.MAIN_HAND, stack);
			helper.assertTrue(garrison.auto() && GunShots.fire(shooter), "The loaded Garrison Rifle did not fire");
		});
		int secondAt = garrisonAt + garrison.interval() + 1;
		helper.runAfterDelay(secondAt, () -> {
			helper.assertTrue(GunShots.fire(shooter), "The Garrison Rifle did not fire again after its interval");
			float expected = sentry.damage() + 2 * garrison.damage();
			helper.assertTrue(Math.abs(200.0F - pig.getHealth() - expected) < 1.0E-3F,
					"After two Garrison shots the pig had lost " + (200.0F - pig.getHealth()) + ", not " + expected);
			helper.assertTrue(GunItem.loaded(shooter.getMainHandItem()) == garrison.capacity() - 2, "The Garrison did not spend two rounds");
		});
		helper.runAfterDelay(secondAt + garrison.interval() + 1, () -> {
			ItemStack stack = new ItemStack(JugcraftGuns.GUNS.get("breacher"));
			GunItem.setLoaded(stack, 1);
			shooter.setItemInHand(InteractionHand.MAIN_HAND, stack);
			float before = pig.getHealth();
			helper.assertTrue(GunShots.fire(shooter), "The loaded Breacher did not fire");
			float taken = before - pig.getHealth();
			helper.assertTrue(taken >= breacher.damage() * (breacher.pellets() - 3),
					"At close range the Breacher's pellets took only " + taken + " (one pellet is " + breacher.damage() + ")");
			helper.succeed();
		});
	}

	/** A bullet is a projectile (Projectile Protection guards against it). */
	@GameTest
	public void bulletsAreProjectiles(GameTestHelper helper) {
		helper.assertTrue(helper.getLevel().registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(JugcraftGuns.BULLET)
				.is(DamageTypeTags.IS_PROJECTILE), "The bullet damage type is not a projectile");
		helper.succeed();
	}

	/**
	 * Attachments fit in a crafting grid and change the gun's numbers: an Extended Magazine gives the Rust Midge half as
	 * many rounds again and a slower reload, its loaded rounds kept; a Speed Magazine takes its place (the Extended
	 * Magazine staying in the grid) and quickens the reload; a Silencer fits beside it, quietens the shot and weakens it a
	 * little. A gun takes no attachment it has no part for, none it already wears, and shears take nothing off a bare gun;
	 * a gun and shears take the last attachment off, leaving it and the shears in the grid.
	 */
	@GameTest
	public void attachmentsFitAndComeOff(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		for (String recipe : List.of("gun_attachment", "gun_attachment_removal")) {
			helper.assertTrue(level.recipeAccess().byKey(ResourceKey.create(Registries.RECIPE, Jugcraft.id(recipe))).isPresent(),
					"The " + recipe + " recipe does not load");
		}
		GunSpec midge = JugcraftGuns.SPECS.get("rust_midge");
		ItemStack gun = new ItemStack(JugcraftGuns.GUNS.get("rust_midge"));
		GunItem.setLoaded(gun, 5);

		Crafted extended = craft(helper, gun, attachment("extended_magazine"));
		helper.assertTrue(GunItem.attachments(extended.result()).equals(List.of("extended_magazine")) && GunItem.loaded(extended.result()) == 5,
				"The Extended Magazine did not fit, rounds kept: " + GunItem.attachments(extended.result()));
		GunSpec spec = GunItem.spec(extended.result());
		helper.assertTrue(spec.capacity() == 30 && spec.reload() == Math.round(midge.reload() * 1.15F),
				"With the Extended Magazine: " + spec.capacity() + " rounds, " + spec.reload() + " ticks to reload");
		helper.assertTrue(extended.left().stream().allMatch(ItemStack::isEmpty), "Something stayed in the grid");

		Crafted swapped = craft(helper, extended.result(), attachment("speed_magazine"));
		spec = GunItem.spec(swapped.result());
		helper.assertTrue(GunItem.attachments(swapped.result()).equals(List.of("speed_magazine")), "The Speed Magazine did not replace it");
		helper.assertTrue(spec.capacity() == midge.capacity() && spec.reload() == Math.round(midge.reload() * 0.65F),
				"With the Speed Magazine: " + spec.capacity() + " rounds, " + spec.reload() + " ticks to reload");
		helper.assertTrue(swapped.left().get(1).is(JugcraftGuns.ATTACHMENT_ITEMS.get("extended_magazine")),
				"The Extended Magazine did not stay in the grid");

		Crafted silenced = craft(helper, swapped.result(), attachment("silencer"));
		ItemStack quiet = silenced.result();
		helper.assertTrue(GunItem.attachments(quiet).equals(List.of("speed_magazine", "silencer")), "The Silencer did not fit beside it");
		helper.assertTrue(Math.abs(GunItem.volume(quiet) - 0.35F) < 1.0E-4F && Math.abs(GunItem.spec(quiet).damage() - midge.damage() * 0.95F) < 1.0E-4F,
				"Silenced: volume " + GunItem.volume(quiet) + ", damage " + GunItem.spec(quiet).damage());

		RecipeManager.CachedCheck<CraftingInput, CraftingRecipe> crafting = RecipeManager.createCheck(RecipeType.CRAFTING);
		helper.assertFalse(crafting.getRecipeFor(grid(new ItemStack(JugcraftGuns.GUNS.get("thunderpipe")), attachment("speed_magazine")), level)
				.isPresent(), "The Thunderpipe took a magazine");
		helper.assertFalse(crafting.getRecipeFor(grid(quiet, attachment("silencer")), level).isPresent(), "A second Silencer fitted");
		helper.assertFalse(crafting.getRecipeFor(grid(gun, new ItemStack(Items.SHEARS)), level).isPresent(), "Shears worked on a bare gun");

		Crafted stripped = craft(helper, quiet, new ItemStack(Items.SHEARS));
		helper.assertTrue(GunItem.attachments(stripped.result()).equals(List.of("speed_magazine")), "The shears did not take the Silencer off");
		helper.assertTrue(stripped.left().get(0).is(JugcraftGuns.ATTACHMENT_ITEMS.get("silencer")) && stripped.left().get(1).is(Items.SHEARS),
				"The Silencer and the shears did not stay in the grid: " + stripped.left());
		helper.succeed();
	}

	/**
	 * A Rust Midge with an Extended Magazine loads 30 rounds, after its longer reload. With the magazine taken off it keeps
	 * them and fires them, and will not reload while it holds more than its own 20.
	 */
	@GameTest(structure = ARENA, maxTicks = 100)
	public void extendedMagazineLoadsMore(GameTestHelper helper) {
		floor(helper);
		Mob pig = pig(helper, new BlockPos(1, 2, 4));
		ServerPlayer shooter = shooter(helper, "rust_midge", 0, pig, GameType.SURVIVAL);
		GunItem.fit(shooter.getMainHandItem(), "extended_magazine");
		shooter.getInventory().add(new ItemStack(JugcraftGuns.ROUNDS.get("light_round"), 64));
		int own = JugcraftGuns.SPECS.get("rust_midge").reload();
		int longer = GunItem.spec(shooter.getMainHandItem()).reload();
		helper.assertTrue(longer > own + 2, "The Extended Magazine's reload is not longer");
		helper.assertTrue(GunShots.reload(shooter), "The reload did not start");
		helper.runAfterDelay(own + 2, () -> helper.assertTrue(GunItem.loaded(shooter.getMainHandItem()) == 0,
				"The Extended Magazine loaded in the Midge's own reload time"));
		helper.runAfterDelay(longer + 2, () -> {
			ItemStack stack = shooter.getMainHandItem();
			helper.assertTrue(GunItem.loaded(stack) == 30, "Loaded " + GunItem.loaded(stack) + ", not 30");
			helper.assertTrue(GunShots.count(shooter.getInventory(), JugcraftGuns.ROUNDS.get("light_round")) == 34, "Not 30 rounds taken");
			GunItem.remove(stack, "extended_magazine");
			helper.assertTrue(GunItem.loaded(stack) == 30, "The rounds went with the magazine");
			helper.assertTrue(GunShots.fire(shooter) && GunItem.loaded(stack) == 29, "It did not fire the rounds it held");
			helper.assertFalse(GunShots.reload(shooter), "It reloaded while holding more than its own magazine");
			helper.succeed();
		});
	}

	/** The server fires a silenced Rust Midge's shot with the silencer's damage. */
	@GameTest(structure = ARENA, maxTicks = 20)
	public void silencedShotIsALittleWeaker(GameTestHelper helper) {
		floor(helper);
		Mob pig = pig(helper, new BlockPos(1, 2, 4));
		pig.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200.0);
		pig.setHealth(200.0F);
		ServerPlayer shooter = shooter(helper, "rust_midge", 20, pig, GameType.SURVIVAL);
		GunItem.fit(shooter.getMainHandItem(), "silencer");
		helper.assertTrue(GunShots.fire(shooter), "The silenced Midge did not fire");
		float expected = JugcraftGuns.SPECS.get("rust_midge").damage() * 0.95F;
		helper.assertTrue(Math.abs(200.0F - pig.getHealth() - expected) < 1.0E-3F,
				"The silenced shot took " + (200.0F - pig.getHealth()) + ", not " + expected);
		helper.succeed();
	}

	/**
	 * Slice 7: a bayonet stabs. A Patchwork Carbine with an Iron Bayonet strikes a pig two blocks ahead for the bayonet's
	 * damage, and cannot stab again until its time has passed; a bare carbine cannot stab, and the bayonet does not reach
	 * a pig seven blocks off.
	 */
	@GameTest(structure = ARENA, maxTicks = 40)
	public void bayonetStabsWithinReach(GameTestHelper helper) {
		floor(helper);
		Mob near = pig(helper, new BlockPos(1, 2, 3));
		near.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200.0);
		near.setHealth(200.0F);
		ServerPlayer bare = shooter(helper, "patchwork_carbine", 10, near, GameType.SURVIVAL);
		helper.assertFalse(GunShots.stab(bare), "A carbine with no bayonet stabbed");
		helper.assertTrue(near.getHealth() == 200.0F, "A carbine with no bayonet hurt the pig");
		GunItem.fit(bare.getMainHandItem(), "iron_bayonet");
		helper.assertTrue(GunShots.stab(bare), "The carbine's Iron Bayonet did not strike the pig two blocks ahead");
		float stab = JugcraftGuns.ATTACHMENTS.get("iron_bayonet").stab();
		helper.assertTrue(Math.abs(200.0F - near.getHealth() - stab) < 1.0E-3F,
				"The stab took " + (200.0F - near.getHealth()) + ", not " + stab);
		helper.assertFalse(GunShots.stab(bare), "The bayonet stabbed again at once");
		helper.runAfterDelay(GunShots.STAB_TICKS + 1, () -> {
			Mob far = pig(helper, new BlockPos(1, 2, 8));
			ServerPlayer reaching = shooter(helper, "patchwork_carbine", 10, far, GameType.SURVIVAL);
			GunItem.fit(reaching.getMainHandItem(), "iron_bayonet");
			float health = far.getHealth();
			helper.assertFalse(GunShots.stab(reaching), "The bayonet reached a pig seven blocks off");
			helper.assertTrue(far.getHealth() == health, "The bayonet hurt a pig seven blocks off");
			helper.succeed();
		});
	}

	/**
	 * Slice 7: the guns whose attachment parts the owner drew on shared textures take them now. The Drover Rifle takes a
	 * Silencer, the Line Musket a Wooden Stock and the Coach Gun a Steel Bayonet; the Netherite Bayonet is a smithing
	 * upgrade, and its recipe loads.
	 */
	@GameTest
	public void sharedTextureGunsTakeTheirAttachments(GameTestHelper helper) {
		for (String[] pair : new String[][] {{"drover_rifle", "silencer"}, {"line_musket", "wooden_stock"}, {"coach_gun", "steel_bayonet"},
				{"duelling_pistol", "light_stock"}, {"bellmouth", "vertical_grip"}}) {
			Crafted fitted = craft(helper, new ItemStack(JugcraftGuns.GUNS.get(pair[0])), attachment(pair[1]));
			helper.assertTrue(GunItem.attachments(fitted.result()).equals(List.of(pair[1])), "The " + pair[0] + " did not take a " + pair[1]);
		}
		helper.assertTrue(helper.getLevel().recipeAccess().byKey(ResourceKey.create(Registries.RECIPE, Jugcraft.id("netherite_bayonet")))
				.isPresent(), "The Netherite Bayonet's smithing recipe does not load");
		helper.succeed();
	}

	/**
	 * Slice 7, the scopes: the guns the owner made to take one take each of the three in the optic slot, which changes the
	 * spread by the scope's numbers; a second scope takes the first one's place (the first stays in the grid); a scope fits
	 * beside an attachment in every other slot the gun has; a gun the owner made no sights for takes none. A gun keeps an
	 * attachment a slot in its save, five at most.
	 */
	@GameTest
	public void scopesFitTheGunsMadeForThem(GameTestHelper helper) {
		for (String scope : List.of("long_scope", "medium_scope", "reflex_sight")) {
			helper.assertTrue(helper.getLevel().recipeAccess().byKey(ResourceKey.create(Registries.RECIPE, Jugcraft.id(scope))).isPresent(),
					"The " + scope + " recipe does not load");
			for (String gun : List.of("longhorn_rifle", "drover_rifle", "riveter_smg")) {
				helper.assertTrue(JugcraftGuns.ACCEPTS.get(gun).contains(scope), "The " + gun + " does not take a " + scope);
			}
		}
		GunSpec longhorn = JugcraftGuns.SPECS.get("longhorn_rifle");
		Crafted scoped = craft(helper, new ItemStack(JugcraftGuns.GUNS.get("longhorn_rifle")), attachment("long_scope"));
		GunSpec spec = GunItem.spec(scoped.result());
		helper.assertTrue(GunItem.attachments(scoped.result()).equals(List.of("long_scope")), "The Long Scope did not fit the Longhorn Rifle");
		helper.assertTrue(Math.abs(spec.aimSpread() - longhorn.aimSpread() * 0.5F) < 1.0E-4F
				&& Math.abs(spec.hipSpread() - longhorn.hipSpread() * 1.25F) < 1.0E-4F,
				"With the Long Scope: spread " + spec.hipSpread() + " from the hip, " + spec.aimSpread() + " aimed");
		Crafted swapped = craft(helper, scoped.result(), attachment("medium_scope"));
		helper.assertTrue(GunItem.attachments(swapped.result()).equals(List.of("medium_scope"))
				&& swapped.left().get(1).is(JugcraftGuns.ATTACHMENT_ITEMS.get("long_scope")),
				"The Medium Scope did not take the Long Scope's place, leaving it in the grid: " + GunItem.attachments(swapped.result()));

		ItemStack riveter = new ItemStack(JugcraftGuns.GUNS.get("riveter_smg"));
		for (String other : List.of("silencer", "extended_magazine", "wooden_stock", "reflex_sight")) {
			riveter = craft(helper, riveter, attachment(other)).result();
		}
		helper.assertTrue(GunItem.attachments(riveter).equals(List.of("silencer", "extended_magazine", "wooden_stock", "reflex_sight")),
				"The Reflex Sight did not fit beside the Riveter SMG's other attachments: " + GunItem.attachments(riveter));
		helper.assertFalse(RecipeManager.createCheck(RecipeType.CRAFTING)
				.getRecipeFor(grid(new ItemStack(JugcraftGuns.GUNS.get("patchwork_carbine")), attachment("long_scope")), helper.getLevel())
				.isPresent(), "The Patchwork Carbine, made with no sights to swap, took a scope");

		List<String> fullSet = List.of("silencer", "extended_magazine", "wooden_stock", "light_grip", "long_scope");
		helper.assertTrue(JugcraftGuns.FITTED.codecOrThrow().encodeStart(JsonOps.INSTANCE, fullSet).isSuccess(),
				"A gun cannot keep an attachment in each of the five slots in its save");
		helper.succeed();
	}

	/** What a crafting grid gave, and what stayed in it. */
	private record Crafted(ItemStack result, NonNullList<ItemStack> left) {
	}

	/** Crafts a gun and another item side by side in a 2 x 1 grid (the test fails if no recipe takes them). */
	private static Crafted craft(GameTestHelper helper, ItemStack gun, ItemStack other) {
		CraftingInput input = grid(gun, other);
		Optional<RecipeHolder<CraftingRecipe>> recipe = RecipeManager.createCheck(RecipeType.CRAFTING).getRecipeFor(input, helper.getLevel());
		helper.assertTrue(recipe.isPresent(), "No recipe takes " + gun + " and " + other);
		return new Crafted(recipe.get().value().assemble(input), recipe.get().value().getRemainingItems(input));
	}

	private static CraftingInput grid(ItemStack first, ItemStack second) {
		return CraftingInput.of(2, 1, List.of(first.copy(), second.copy()));
	}

	private static ItemStack attachment(String name) {
		return new ItemStack(JugcraftGuns.ATTACHMENT_ITEMS.get(name));
	}

	/** A mock player holding a gun with this many rounds loaded, at the arena's (1, 2, 1), aimed at the target's middle. */
	/** A player at (1, 2, 1) facing south, aimed at the target's middle, with this gun loaded in the main hand. */
	static ServerPlayer shooter(GameTestHelper helper, String gun, int loaded, Mob target, GameType mode) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setGameMode(mode);
		BlockPos at = helper.absolutePos(new BlockPos(1, 2, 1));
		player.setPos(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
		Vec3 aim = target.getBoundingBox().getCenter().subtract(player.getEyePosition());
		player.setYRot(0.0F);
		player.setYHeadRot(0.0F);
		player.setXRot((float) Math.toDegrees(Math.atan2(-aim.y, Math.sqrt(aim.x * aim.x + aim.z * aim.z))));
		ItemStack stack = new ItemStack(JugcraftGuns.GUNS.get(gun));
		GunItem.setLoaded(stack, loaded);
		player.setItemInHand(InteractionHand.MAIN_HAND, stack);
		return player;
	}

	/** A stone floor at y 1 over the arena, clear air above it. */
	static void floor(GameTestHelper helper) {
		for (int x = 0; x <= 15; x++) {
			for (int z = 0; z <= 15; z++) {
				helper.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
				for (int y = 2; y <= 4; y++) {
					helper.setBlock(new BlockPos(x, y, z), Blocks.AIR);
				}
			}
		}
	}

	/**
	 * A pig that cannot walk off or be knocked back: a hit pushes a creature away (and up, once it stands on the
	 * ground), so a later shot along the first aim could pass under it.
	 */
	static Mob pig(GameTestHelper helper, BlockPos pos) {
		@SuppressWarnings("unchecked")
		EntityType<Mob> type = (EntityType<Mob>) BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.withDefaultNamespace("pig"));
		Mob pig = helper.spawnWithNoFreeWill(type, pos);
		pig.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.0);
		pig.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(1.0);
		return pig;
	}
}
