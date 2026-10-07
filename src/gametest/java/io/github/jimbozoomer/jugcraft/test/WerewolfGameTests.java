package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.agriculture.JugcraftAgriculture;
import io.github.jimbozoomer.jugcraft.agriculture.Werewolf;
import io.github.jimbozoomer.jugcraft.agriculture.WerewolfRugBlock;
import io.github.jimbozoomer.jugcraft.agriculture.Werewolves;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.loot.LootTable;

/**
 * In-game tests for full-moon werewolves (fall addition 23): silver (the dagger in hand, a silver arrow) does two and a
 * half times its damage, anything else half, and slaying one with silver earns Silver Lining; silver stops it healing;
 * it is gone soon after it finds it isn't a full-moon night; wolfsbane in hand or near wards someone off, and a warded
 * target is dropped and left alone (a player earns Not Tonight); no more come near a player than the cap; and the data
 * loads. The three kinds: each tier is tougher than the last and comes where it should; the brown werewolf hunts
 * livestock, calls its pack and flees when badly hurt; the snow werewolf's bite slows and freezes (not through leather),
 * it never freezes and runs faster on snow; the shadow werewolf steps out behind its prey, its howl darkens the night and
 * rouses the pack, and only planted wolfsbane wards it off; each drops its own pelt, and slaying a shadow werewolf earns
 * Leader of the Pack.
 */
public class WerewolfGameTests {
	private static final double EPSILON = 1.0E-4;

	private static Item item(String id) {
		return JugcraftAgriculture.item(id);
	}

	private static ServerPlayer player(GameTestHelper helper, BlockPos standAt, ItemStack held) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setGameMode(GameType.SURVIVAL);
		BlockPos absolute = helper.absolutePos(standAt);
		player.setPos(absolute.getX() + 0.5, absolute.getY(), absolute.getZ() + 0.5);
		player.setItemInHand(InteractionHand.MAIN_HAND, held);
		return player;
	}

	@SuppressWarnings("unchecked")
	private static EntityType<Villager> villager() {
		return (EntityType<Villager>) BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.withDefaultNamespace("villager"));
	}

	private static boolean earned(ServerPlayer player, String id) {
		AdvancementHolder advancement = player.level().getServer().getAdvancements().get(Jugcraft.id(id));
		return advancement != null && player.getAdvancements().getOrStartProgress(advancement).isDone();
	}

	/** A werewolf with no armour, so damage taken is damage dealt. */
	private static Werewolf werewolf(GameTestHelper helper, BlockPos at) {
		Werewolf werewolf = helper.spawn(JugcraftAgriculture.WEREWOLF, at);
		werewolf.getAttribute(Attributes.ARMOR).setBaseValue(0.0);
		werewolf.setNoAi(true);
		return werewolf;
	}

	/** A werewolf of {@code kind} at full health, without AI, so only the test moves it or changes its target. */
	private static Werewolf werewolf(GameTestHelper helper, BlockPos at, Werewolf.Kind kind) {
		Werewolf werewolf = helper.spawn(JugcraftAgriculture.WEREWOLF, at);
		werewolf.setKind(kind);
		werewolf.setHealth(werewolf.getMaxHealth());
		werewolf.setNoAi(true);
		return werewolf;
	}

	private static float lost(Werewolf werewolf) {
		return werewolf.getMaxHealth() - werewolf.getHealth();
	}

	private static void floor(GameTestHelper helper) {
		for (int x = 0; x <= 7; x++) {
			for (int z = 0; z <= 7; z++) {
				helper.setBlock(new BlockPos(x, 1, z), Blocks.GRASS_BLOCK);
			}
		}
	}

	/**
	 * Eight damage from an iron sword does four to a werewolf, from the silver dagger twenty; a plain arrow four, a silver
	 * arrow twenty. Silver keeps it from healing. Slain with silver, it earns Silver Lining.
	 */
	@GameTest
	public void silverHurtsWerewolves(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		floor(helper);
		ServerPlayer hunter = player(helper, new BlockPos(1, 2, 1), new ItemStack(Items.IRON_SWORD));
		Werewolf ironStruck = werewolf(helper, new BlockPos(2, 2, 4));
		ironStruck.hurtServer(level, level.damageSources().playerAttack(hunter), 8.0F);
		helper.assertTrue(Math.abs(lost(ironStruck) - 8.0F * Werewolf.HIDE_FACTOR) < EPSILON, "Iron does half its damage, not " + lost(ironStruck));
		helper.assertTrue(!ironStruck.silverWounded(), "Iron doesn't stop it healing");

		hunter.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item("silver_dagger")));
		Werewolf silverStruck = werewolf(helper, new BlockPos(4, 2, 4));
		silverStruck.hurtServer(level, level.damageSources().playerAttack(hunter), 8.0F);
		helper.assertTrue(Math.abs(lost(silverStruck) - 8.0F * Werewolf.SILVER_FACTOR) < EPSILON, "Silver does two and a half times, not "
				+ lost(silverStruck));
		helper.assertTrue(silverStruck.silverWounded(), "Silver stops it healing");

		Werewolf shot = werewolf(helper, new BlockPos(6, 2, 4));
		Arrow plain = new Arrow(level, hunter, new ItemStack(Items.ARROW), new ItemStack(Items.BOW));
		shot.hurtServer(level, level.damageSources().arrow(plain, hunter), 8.0F);
		helper.assertTrue(Math.abs(lost(shot) - 8.0F * Werewolf.HIDE_FACTOR) < EPSILON, "A plain arrow does half");
		Werewolf silverShot = werewolf(helper, new BlockPos(6, 2, 6));
		Arrow silver = new Arrow(level, hunter, new ItemStack(item(Werewolves.SILVER_ARROW)), new ItemStack(Items.BOW));
		silverShot.hurtServer(level, level.damageSources().arrow(silver, hunter), 8.0F);
		helper.assertTrue(Math.abs(lost(silverShot) - 8.0F * Werewolf.SILVER_FACTOR) < EPSILON, "A silver arrow does two and a half times");

		silverStruck.hurtServer(level, level.damageSources().playerAttack(hunter), 100.0F);
		helper.assertTrue(silverStruck.isDeadOrDying(), "Silver slays it");
		helper.assertTrue(earned(hunter, "silver_lining"), "Silver Lining is earned");
		helper.succeed();
	}

	/** A werewolf finds within a second whether it is a full-moon night; if it isn't, it is gone. */
	@GameTest(maxTicks = 80)
	public void werewolvesTurnBackWhenTheNightEnds(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		floor(helper);
		boolean fullMoon = Werewolves.fullMoon(level);
		Werewolf werewolf = helper.spawn(JugcraftAgriculture.WEREWOLF, new BlockPos(3, 2, 3));
		werewolf.setNoAi(true);
		helper.runAfterDelay(45, () -> {
			helper.assertTrue(werewolf.isRemoved() != fullMoon, fullMoon ? "On a full-moon night it stays" : "By day it is gone");
			helper.succeed();
		});
	}

	/**
	 * A player with wolfsbane in hand is warded, as is one standing near planted wolfsbane, and not one further off; a
	 * werewolf keeps hunting a villager with nothing, and drops one holding a sprig and leaves them alone; a warded
	 * player is left alone and earns Not Tonight.
	 */
	@GameTest
	public void wolfsbaneWardsWerewolvesOff(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		floor(helper);
		ServerPlayer holder = player(helper, new BlockPos(1, 2, 1), new ItemStack(Werewolves.wolfsbane()));
		helper.assertTrue(Werewolves.warded(level, holder), "Wolfsbane in hand wards");
		ServerPlayer bare = player(helper, new BlockPos(7, 2, 7), ItemStack.EMPTY);
		helper.assertTrue(!Werewolves.warded(level, bare), "An empty hand with no wolfsbane near doesn't");
		helper.setBlock(new BlockPos(0, 2, 7), JugcraftAgriculture.block(Werewolves.WOLFSBANE));
		helper.assertTrue(Werewolves.wardNear(level, helper.absolutePos(new BlockPos(5, 2, 7))), "Planted wolfsbane wards five blocks off");
		helper.assertTrue(!Werewolves.wardNear(level, helper.absolutePos(new BlockPos(0, 2, 7)).east(Werewolves.WARD_REACH + 1)),
				"but not beyond its reach");

		// A villager can be hunted at once, where a player who has only just appeared can't be yet. Without AI, so only
		// its own ward check (not a target goal) changes its target.
		Villager unwarded = helper.spawnWithNoFreeWill(villager(), new BlockPos(7, 2, 5));
		Villager sprigged = helper.spawnWithNoFreeWill(villager(), new BlockPos(1, 2, 3));
		sprigged.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Werewolves.wolfsbane()));
		Werewolf werewolf = werewolf(helper, new BlockPos(3, 2, 3));
		werewolf.setTarget(unwarded);
		helper.assertTrue(werewolf.getTarget() == unwarded, "It hunts a villager");
		helper.assertTrue(!werewolf.checkWard(level) && werewolf.getTarget() == unwarded, "It keeps hunting someone unwarded");
		werewolf.setTarget(sprigged);
		helper.assertTrue(werewolf.checkWard(level), "Its ward check finds wolfsbane on its target");
		helper.assertTrue(werewolf.getTarget() == null && werewolf.shuns(sprigged) && !werewolf.canAttack(sprigged),
				"It drops a warded target and leaves them alone");
		werewolf.wardedOff(holder);
		helper.assertTrue(werewolf.shuns(holder) && !werewolf.canAttack(holder), "It leaves a warded player alone");
		helper.assertTrue(earned(holder, "wolfsbane_ward"), "Not Tonight is earned");
		helper.succeed();
	}

	/** No more werewolves come near a player than the cap allows; they step out onto woodland floor, not stone. */
	@GameTest
	public void werewolvesKeepToTheirCap(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		floor(helper);
		for (int n = 0; n < Werewolves.NEAR_CAP; n++) {
			werewolf(helper, new BlockPos(2 + 2 * n, 2, 2));
		}
		helper.assertTrue(Werewolves.trySpawn(level, helper.absolutePos(new BlockPos(3, 2, 3)), level.getRandom()) == 0, "None come past the cap");
		helper.setBlock(new BlockPos(6, 1, 6), Blocks.STONE);
		helper.assertTrue(Werewolves.woodlandFloor(level, helper.absolutePos(new BlockPos(3, 2, 6))), "Grass is woodland floor");
		helper.assertTrue(!Werewolves.woodlandFloor(level, helper.absolutePos(new BlockPos(6, 2, 6))), "Stone isn't");
		helper.assertTrue(Werewolves.ground(level, helper.absolutePos(new BlockPos(3, 2, 3)).getX(), helper.absolutePos(new BlockPos(3, 2, 3)).getZ())
				== null || level.getBiome(helper.absolutePos(new BlockPos(3, 2, 3))).is(Werewolves.HAUNTS), "Only werewolf country is ground for one");
		helper.succeed();
	}

	/**
	 * Each kind is tougher than the one before (health, damage, armour, size and tier), and its attributes are its
	 * kind's; an unknown kind reads as brown. Snowy woods bring snow werewolves, the shadow werewolf's haunts bring it half
	 * the time and elsewhere rarely; the biome tags hold the right biomes.
	 */
	@GameTest
	public void werewolfKindsRiseInDanger(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		floor(helper);
		Werewolf.Kind[] kinds = Werewolf.Kind.values();
		helper.assertTrue(kinds.length == 3 && kinds[0] == Werewolf.Kind.BROWN && kinds[2] == Werewolf.Kind.SHADOW, "Three kinds, brown to shadow");
		Werewolf previous = null;
		for (int n = 0; n < kinds.length; n++) {
			Werewolf.Kind kind = kinds[n];
			Werewolf werewolf = werewolf(helper, new BlockPos(1 + 3 * n, 2, 3), kind);
			helper.assertTrue(werewolf.kind() == kind && kind.tier == n + 1, kind + " is tier " + (n + 1));
			helper.assertTrue(Math.abs(werewolf.getMaxHealth() - kind.health) < EPSILON && Math.abs(werewolf.getHealth() - kind.health) < EPSILON,
					kind + " has its kind's health");
			helper.assertTrue(Math.abs(werewolf.getAttributeValue(Attributes.ATTACK_DAMAGE) - kind.damage) < EPSILON
					&& Math.abs(werewolf.getAttributeValue(Attributes.ARMOR) - kind.armor) < EPSILON
					&& Math.abs(werewolf.getAttributeValue(Attributes.SCALE) - kind.scale) < EPSILON, kind + " has its kind's damage, armour and size");
			if (previous != null) {
				helper.assertTrue(werewolf.getMaxHealth() > previous.getMaxHealth()
						&& werewolf.getAttributeValue(Attributes.ATTACK_DAMAGE) > previous.getAttributeValue(Attributes.ATTACK_DAMAGE)
						&& werewolf.getAttributeValue(Attributes.ARMOR) > previous.getAttributeValue(Attributes.ARMOR)
						&& werewolf.getAttributeValue(Attributes.SCALE) > previous.getAttributeValue(Attributes.SCALE), kind + " is more dangerous than "
						+ previous.kind());
			}
			previous = werewolf;
		}
		helper.assertTrue(Werewolf.Kind.byId("snow") == Werewolf.Kind.SNOW && Werewolf.Kind.byId("none") == Werewolf.Kind.BROWN,
				"Kinds read back by name, an unknown one as brown");

		helper.assertTrue(Werewolves.kindFor(false, false, 0.5F) == Werewolf.Kind.BROWN, "In the woods, a brown werewolf");
		helper.assertTrue(Werewolves.kindFor(true, false, 0.5F) == Werewolf.Kind.SNOW, "In snowy woods, a snow werewolf");
		helper.assertTrue(Werewolves.kindFor(false, false, Werewolves.SHADOW_CHANCE / 2) == Werewolf.Kind.SHADOW
				&& Werewolves.kindFor(true, false, Werewolves.SHADOW_CHANCE / 2) == Werewolf.Kind.SHADOW, "Now and then a shadow werewolf anywhere");
		helper.assertTrue(Werewolves.kindFor(false, true, Werewolves.SHADOW_HAUNT_CHANCE - 0.01F) == Werewolf.Kind.SHADOW
				&& Werewolves.kindFor(false, true, Werewolves.SHADOW_HAUNT_CHANCE + 0.01F) == Werewolf.Kind.BROWN,
				"In its haunts, a shadow werewolf half the time");
		Registry<Biome> biomes = level.registryAccess().lookupOrThrow(Registries.BIOME);
		helper.assertTrue(biomes.getOrThrow(Biomes.SNOWY_TAIGA).is(Werewolves.SNOW_HAUNTS) && biomes.getOrThrow(Biomes.SNOWY_TAIGA).is(Werewolves.HAUNTS)
				&& !biomes.getOrThrow(Biomes.FOREST).is(Werewolves.SNOW_HAUNTS), "Snowy taiga is snow werewolf country, a forest isn't");
		helper.assertTrue(biomes.getOrThrow(Biomes.DARK_FOREST).is(Werewolves.SHADOW_HAUNTS) && biomes.getOrThrow(Biomes.DARK_FOREST).is(Werewolves.HAUNTS)
				&& !biomes.getOrThrow(Biomes.TAIGA).is(Werewolves.SHADOW_HAUNTS), "A dark forest haunts shadow werewolves, a taiga doesn't");
		helper.succeed();
	}

	/**
	 * A brown werewolf goes after livestock it can see; its howl over its prey calls brown werewolves near that aren't
	 * hunting, not a snow werewolf; a snow werewolf leaves livestock alone.
	 */
	@GameTest
	public void brownWerewolvesHuntAsAPack(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		floor(helper);
		Mob sheep = helper.spawnWithNoFreeWill(EntityTypes.SHEEP, new BlockPos(6, 2, 6));
		Werewolf hunter = werewolf(helper, new BlockPos(3, 2, 6), Werewolf.Kind.BROWN);
		Werewolf packmate = werewolf(helper, new BlockPos(1, 2, 1), Werewolf.Kind.BROWN);
		Werewolf snow = werewolf(helper, new BlockPos(6, 2, 1), Werewolf.Kind.SNOW);
		helper.assertTrue(snow.huntPrey(level) == null && snow.getTarget() == null, "A snow werewolf leaves livestock alone");
		helper.assertTrue(hunter.huntPrey(level) == sheep && hunter.getTarget() == sheep, "A brown werewolf goes after a sheep");
		helper.assertTrue(hunter.callPack(level, sheep) >= 1, "Its howl calls the pack");
		helper.assertTrue(packmate.getTarget() == sheep, "A brown werewolf near joins the hunt");
		helper.assertTrue(snow.getTarget() == null, "A snow werewolf doesn't answer a brown one's call");
		helper.succeed();
	}

	/** Below a quarter of its health a brown werewolf drops its prey and flees until healed to half; a snow werewolf doesn't. */
	@GameTest
	public void brownWerewolvesFleeWhenHurt(GameTestHelper helper) {
		floor(helper);
		Villager villager = helper.spawnWithNoFreeWill(villager(), new BlockPos(6, 2, 6));
		Werewolf brown = werewolf(helper, new BlockPos(2, 2, 2), Werewolf.Kind.BROWN);
		brown.setTarget(villager);
		helper.assertTrue(!brown.checkFlight() && brown.getTarget() == villager, "Unhurt, it hunts on");
		brown.setHealth(brown.getMaxHealth() * (Werewolf.FLEE_BELOW - 0.05F));
		helper.assertTrue(brown.checkFlight() && brown.fleeing() && brown.getTarget() == null && !brown.canAttack(villager),
				"Badly hurt, it drops its prey and flees");
		brown.setHealth(brown.getMaxHealth() * (Werewolf.FLEE_UNTIL - 0.05F));
		helper.assertTrue(brown.checkFlight(), "Not yet healed enough, it keeps fleeing");
		brown.setHealth(brown.getMaxHealth() * Werewolf.FLEE_UNTIL);
		helper.assertTrue(!brown.checkFlight() && brown.canAttack(villager), "Healed to half, it hunts again");
		Werewolf snow = werewolf(helper, new BlockPos(2, 2, 5), Werewolf.Kind.SNOW);
		snow.setHealth(1.0F);
		helper.assertTrue(!snow.checkFlight() && !snow.fleeing(), "A snow werewolf fights on");
		helper.succeed();
	}

	/**
	 * A snow werewolf's bite slows and chills (not through leather); it never freezes, a brown one can; on snow it runs a
	 * quarter faster, and a brown one doesn't.
	 */
	@GameTest
	public void snowWerewolvesBiteWithFrost(GameTestHelper helper) {
		floor(helper);
		Villager bitten = helper.spawnWithNoFreeWill(villager(), new BlockPos(6, 2, 6));
		Villager booted = helper.spawnWithNoFreeWill(villager(), new BlockPos(6, 2, 2));
		booted.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.LEATHER_BOOTS));
		Werewolf snow = werewolf(helper, new BlockPos(2, 2, 4), Werewolf.Kind.SNOW);
		snow.frostbite(bitten);
		helper.assertTrue(bitten.hasEffect(MobEffects.SLOWNESS), "Its bite slows");
		helper.assertTrue(bitten.getTicksFrozen() == Werewolf.FROSTBITE_CHILL, "Its bite chills, " + bitten.getTicksFrozen());
		snow.frostbite(booted);
		helper.assertTrue(booted.hasEffect(MobEffects.SLOWNESS) && booted.getTicksFrozen() == 0, "Leather keeps the chill out, not the slowing");
		Werewolf brown = werewolf(helper, new BlockPos(4, 2, 4), Werewolf.Kind.BROWN);
		helper.assertTrue(!snow.canFreeze() && brown.canFreeze(), "A snow werewolf never freezes, a brown one can");

		double base = snow.getAttributeValue(Attributes.MOVEMENT_SPEED);
		snow.stride(true);
		helper.assertTrue(Math.abs(snow.getAttributeValue(Attributes.MOVEMENT_SPEED) - base * (1.0 + Werewolf.SNOW_STRIDE)) < EPSILON,
				"On snow it runs a quarter faster");
		snow.stride(false);
		helper.assertTrue(Math.abs(snow.getAttributeValue(Attributes.MOVEMENT_SPEED) - base) < EPSILON, "Off snow, its own pace");
		double brownBase = brown.getAttributeValue(Attributes.MOVEMENT_SPEED);
		brown.stride(true);
		helper.assertTrue(Math.abs(brown.getAttributeValue(Attributes.MOVEMENT_SPEED) - brownBase) < EPSILON, "A brown werewolf doesn't");
		helper.setBlock(new BlockPos(4, 2, 1), Blocks.SNOW);
		helper.assertTrue(werewolf(helper, new BlockPos(4, 2, 1), Werewolf.Kind.SNOW).onSnow() && !brown.onSnow(), "Snow underfoot is snow");
		helper.succeed();
	}

	/**
	 * A shadow werewolf steps out of the shadows behind prey six or more blocks off, onto the ground; not when its prey is
	 * close, and a brown werewolf never does.
	 */
	@GameTest
	public void shadowWerewolvesStepOutBehindTheirPrey(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		floor(helper);
		Villager prey = helper.spawnWithNoFreeWill(villager(), new BlockPos(6, 2, 6));
		// Facing south (+z), so behind it is north.
		prey.setYRot(0.0F);
		prey.setYHeadRot(0.0F);
		Werewolf brown = werewolf(helper, new BlockPos(1, 2, 1), Werewolf.Kind.BROWN);
		brown.setTarget(prey);
		helper.assertTrue(!brown.shadowStep(level), "A brown werewolf can't");
		Werewolf shadow = werewolf(helper, new BlockPos(0, 2, 1), Werewolf.Kind.SHADOW);
		shadow.setTarget(prey);
		helper.assertTrue(shadow.distanceTo(prey) >= Werewolf.SHADOW_STEP_MIN, "Its prey is far off");
		helper.assertTrue(shadow.shadowStep(level), "It steps out of the shadows");
		helper.assertTrue(shadow.distanceTo(prey) < 3.5F && shadow.getZ() < prey.getZ(), "behind its prey, " + shadow.distanceTo(prey) + " off");
		helper.assertTrue(Math.abs(shadow.getY() - prey.getY()) < 1.0E-3, "on the ground");
		helper.assertTrue(!shadow.shadowStep(level), "Close by, it doesn't need to");
		helper.succeed();
	}

	/**
	 * A shadow werewolf's howl darkens the night for players near and sends the werewolves near into a frenzy, after its
	 * prey.
	 */
	@GameTest
	public void shadowWerewolfHowlRousesThePack(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		floor(helper);
		ServerPlayer near = player(helper, new BlockPos(1, 2, 6), ItemStack.EMPTY);
		Villager prey = helper.spawnWithNoFreeWill(villager(), new BlockPos(6, 2, 6));
		Werewolf shadow = werewolf(helper, new BlockPos(2, 2, 2), Werewolf.Kind.SHADOW);
		Werewolf brown = werewolf(helper, new BlockPos(5, 2, 2), Werewolf.Kind.BROWN);
		shadow.setTarget(prey);
		helper.assertTrue(shadow.alphaHowl(level) >= 1, "Its howl rouses the werewolves near");
		helper.assertTrue(near.hasEffect(MobEffects.DARKNESS), "and darkens the night for a player near");
		helper.assertTrue(brown.hasEffect(MobEffects.STRENGTH) && brown.hasEffect(MobEffects.SPEED), "A werewolf near is in a frenzy");
		helper.assertTrue(brown.getTarget() == prey, "and goes after the alpha's prey");
		helper.assertTrue(!shadow.hasEffect(MobEffects.STRENGTH), "The alpha doesn't rouse itself");
		helper.succeed();
	}

	/**
	 * A sprig of wolfsbane in hand wards a brown werewolf off but not a shadow werewolf; wolfsbane planted by its prey wards
	 * off both.
	 */
	@GameTest
	public void onlyPlantedWolfsbaneWardsShadowWerewolves(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		floor(helper);
		Villager sprigged = helper.spawnWithNoFreeWill(villager(), new BlockPos(1, 2, 6));
		sprigged.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Werewolves.wolfsbane()));
		Werewolf brown = werewolf(helper, new BlockPos(1, 2, 1), Werewolf.Kind.BROWN);
		brown.setTarget(sprigged);
		helper.assertTrue(brown.checkWard(level) && brown.getTarget() == null, "A sprig wards a brown werewolf off");
		Werewolf shadow = werewolf(helper, new BlockPos(4, 2, 1), Werewolf.Kind.SHADOW);
		shadow.setTarget(sprigged);
		helper.assertTrue(!shadow.checkWard(level) && shadow.getTarget() == sprigged, "but not a shadow werewolf");
		helper.setBlock(new BlockPos(0, 2, 7), JugcraftAgriculture.block(Werewolves.WOLFSBANE));
		helper.assertTrue(shadow.checkWard(level) && shadow.getTarget() == null && shadow.shuns(sprigged), "Planted wolfsbane wards it off");
		helper.succeed();
	}

	/** Each kind drops its own pelt; slaying a shadow werewolf earns Leader of the Pack. */
	@GameTest
	public void eachKindDropsItsOwnPelt(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		floor(helper);
		int n = 0;
		for (Werewolf.Kind kind : Werewolf.Kind.values()) {
			Werewolf werewolf = werewolf(helper, new BlockPos(1 + 3 * n++, 2, 1), kind);
			List<ItemStack> pelts = werewolf.dropPelt(level, level.damageSources().generic());
			helper.assertTrue(pelts.size() == 1 && pelts.get(0).is(item(kind.pelt)) && pelts.get(0).getCount() == 1, kind + " drops its pelt");
		}
		ServerPlayer hunter = player(helper, new BlockPos(4, 2, 6), new ItemStack(Items.IRON_SWORD));
		Werewolf brown = werewolf(helper, new BlockPos(2, 2, 6), Werewolf.Kind.BROWN);
		brown.hurtServer(level, level.damageSources().playerAttack(hunter), 1000.0F);
		helper.assertTrue(brown.isDeadOrDying() && !earned(hunter, "leader_of_the_pack"), "Slaying a brown werewolf isn't leading the pack");
		Werewolf shadow = werewolf(helper, new BlockPos(6, 2, 6), Werewolf.Kind.SHADOW);
		shadow.hurtServer(level, level.damageSources().playerAttack(hunter), 1000.0F);
		helper.assertTrue(shadow.isDeadOrDying(), "Iron can slay a shadow werewolf, with enough of it");
		helper.assertTrue(earned(hunter, "leader_of_the_pack"), "Slaying one earns Leader of the Pack");
		helper.succeed();
	}

	/** The silver dagger, silver arrows, the rug and leather from the pelt have recipes; the drops, flower and tags load. */
	@GameTest
	public void werewolfDataLoads(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		List<String> recipes = new ArrayList<>(List.of("silver_dagger", Werewolves.SILVER_ARROW));
		List<String> tables = new ArrayList<>(List.of("entities/werewolf", "blocks/wolfsbane", "blocks/potted_wolfsbane"));
		for (Werewolf.Kind kind : Werewolf.Kind.values()) {
			recipes.addAll(List.of(kind.rug, "leather_from_" + kind.pelt));
			tables.addAll(List.of("blocks/" + kind.rug, "entities/werewolf/" + kind.id));
			helper.assertTrue(JugcraftAgriculture.block(kind.rug) instanceof WerewolfRugBlock, kind + " has a rug");
		}
		for (String id : recipes) {
			helper.assertTrue(level.recipeAccess().byKey(ResourceKey.create(Registries.RECIPE, Jugcraft.id(id))).isPresent(), id + " has a recipe");
		}
		for (String table : tables) {
			helper.assertTrue(level.getServer().reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE, Jugcraft.id(table)))
					!= LootTable.EMPTY, table + " loads");
		}
		helper.assertTrue(Werewolf.isPrey(EntityTypes.SHEEP) && !Werewolf.isPrey(villager()), "Sheep are a brown werewolf's prey");
		for (String id : List.of("silver_lining", "wolfsbane_ward", "leader_of_the_pack")) {
			helper.assertTrue(level.getServer().getAdvancements().get(Jugcraft.id(id)) != null, id + " loads");
		}
		helper.assertTrue(new ItemStack(item(Werewolves.SILVER_ARROW)).is(ItemTags.ARROWS), "A silver arrow is an arrow to a bow");
		helper.assertTrue(new ItemStack(item("silver_dagger")).is(Werewolf.SILVER_WEAPONS), "The dagger is a silver weapon");
		helper.succeed();
	}
}
