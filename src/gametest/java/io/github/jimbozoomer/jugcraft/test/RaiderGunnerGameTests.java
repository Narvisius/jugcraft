package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.guns.GunItem;
import io.github.jimbozoomer.jugcraft.guns.JugcraftGuns;
import io.github.jimbozoomer.jugcraft.guns.MobGuns;
import io.github.jimbozoomer.jugcraft.raiders.JugcraftRaiders;
import io.github.jimbozoomer.jugcraft.raiders.RaiderInfantry;
import io.github.jimbozoomer.jugcraft.raiders.RaiderRaids;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.AABB;

/**
 * In-game tests for the raider gunners (slice 10F, docs/features/guns.md), the guns in mobs' hands ({@link MobGuns}): a
 * gunner carries one of the service arms, fires the gun's own bullets at what it hunts at half their damage, spares
 * raiders, reloads when its magazine is empty, and its gun drops empty.
 */
public class RaiderGunnerGameTests {
	/**
	 * The gunner is registered and carries one of the arms, each of them carried by some gunners; it marches in raids in
	 * place of some of the grunts, so every party is as large as before; and its loot table loads.
	 */
	@GameTest
	public void raiderGunnersAreRegistered(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		RaiderInfantry gunner = JugcraftRaiders.GUNNER.create(level, EntitySpawnReason.COMMAND);
		helper.assertTrue(gunner != null && gunner.role() == RaiderInfantry.Role.GUNNER && gunner.gunfire() != null,
				"The gunner is registered, with its gunfire");
		helper.assertTrue(gunner.getMainHandItem().getItem() instanceof GunItem gun && MobGuns.ARMS.stream().anyMatch(arm -> arm.gun().equals(gun.name())),
				"A new gunner carries one of the service arms, not " + gunner.getMainHandItem());
		Set<String> carried = new HashSet<>();
		RandomSource random = RandomSource.create(10L);
		for (int i = 0; i < 100; i++) {
			carried.add(((GunItem) MobGuns.arm(random).getItem()).name());
		}
		helper.assertTrue(carried.size() == MobGuns.ARMS.size(), "A hundred gunners carry every arm between them, not only " + carried);
		for (int raid = 1; raid <= RaiderRaids.MAX_LEVEL; raid++) {
			int[] party = RaiderRaids.PARTY[raid - 1];
			helper.assertTrue(party[5] == (raid + 1) / 2 && party[0] + party[5] == 2 + raid,
					"A level " + raid + " raid's gunners take the places of some of its grunts");
		}
		helper.assertTrue(level.getServer().reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE,
				Jugcraft.id("entities/raider_gunner"))) != LootTable.EMPTY, "The gunner's loot table loads");
		helper.succeed();
	}

	/** A gunner with a Garrison Rifle shoots the pig it hunts, nine blocks off, half the gun's damage a hit. */
	@GameTest(structure = GunsGameTests.ARENA, maxTicks = 200)
	public void raiderGunnerShootsWhatItHunts(GameTestHelper helper) {
		GunsGameTests.floor(helper);
		Mob pig = sturdyPig(helper, new BlockPos(8, 2, 12));
		RaiderInfantry gunner = gunner(helper, new BlockPos(8, 2, 3), "garrison_rifle", pig);
		int capacity = JugcraftGuns.SPECS.get("garrison_rifle").capacity();
		float hit = JugcraftGuns.SPECS.get("garrison_rifle").damage() * MobGuns.DAMAGE;
		helper.succeedWhen(() -> {
			float lost = pig.getMaxHealth() - pig.getHealth();
			helper.assertTrue(lost > 0.0F, "The gunner has not hit the pig");
			helper.assertTrue(Math.abs(lost / hit - Math.round(lost / hit)) < 1.0E-3F, "The pig lost " + lost + ", not hits of " + hit);
			helper.assertTrue(gunner.gunfire().rounds() < capacity, "Its magazine still holds " + gunner.gunfire().rounds());
		});
	}

	/** A gunner's bullets pass through the grunt standing between it and the pig, and hit the pig. */
	@GameTest(structure = GunsGameTests.ARENA, maxTicks = 200)
	public void raiderGunnersSpareRaiders(GameTestHelper helper) {
		GunsGameTests.floor(helper);
		Mob pig = sturdyPig(helper, new BlockPos(8, 2, 12));
		RaiderInfantry grunt = helper.spawnWithNoFreeWill(JugcraftRaiders.GRUNT, new BlockPos(8, 2, 7));
		gunner(helper, new BlockPos(8, 2, 2), "sentry_pistol", pig);
		helper.succeedWhen(() -> {
			helper.assertTrue(pig.getHealth() < pig.getMaxHealth(), "The gunner has not hit the pig past the grunt");
			helper.assertTrue(grunt.getHealth() == grunt.getMaxHealth(), "The gunner's bullets hurt the grunt");
		});
	}

	/** A gunner with a Breacher fires its six shells at the pig, then reloads. */
	@GameTest(structure = GunsGameTests.ARENA, maxTicks = 400)
	public void raiderGunnerReloadsWhenEmpty(GameTestHelper helper) {
		GunsGameTests.floor(helper);
		Mob pig = sturdyPig(helper, new BlockPos(8, 2, 8));
		RaiderInfantry gunner = gunner(helper, new BlockPos(8, 2, 3), "breacher", pig);
		helper.succeedWhen(() -> {
			MobGuns.FireGoal gunfire = gunner.gunfire();
			helper.assertTrue(pig.getHealth() < pig.getMaxHealth(), "The gunner has not hit the pig");
			helper.assertTrue(gunfire.reloading() && gunfire.rounds() == 0, "The gunner is not reloading an empty magazine: "
					+ gunfire.rounds() + " rounds");
		});
	}

	/**
	 * Killed by a player, a gunner drops its gun (here certainly) with nothing loaded, and only the rounds its own gun
	 * fires, if any.
	 */
	@GameTest(structure = GunsGameTests.ARENA)
	public void raiderGunnersGunDropsEmpty(GameTestHelper helper) {
		GunsGameTests.floor(helper);
		ServerLevel level = helper.getLevel();
		RaiderInfantry gunner = helper.spawnWithNoFreeWill(JugcraftRaiders.GUNNER, new BlockPos(8, 2, 8));
		gunner.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(JugcraftGuns.GUNS.get("sentry_pistol")));
		gunner.setDropChance(EquipmentSlot.MAINHAND, 1.0F);
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		gunner.hurtServer(level, level.damageSources().playerAttack(player), 1000.0F);
		helper.assertTrue(gunner.isDeadOrDying(), "The gunner did not fall");
		List<ItemEntity> drops = level.getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(new BlockPos(8, 2, 8))).inflate(3.0));
		ItemStack gun = drops.stream().map(ItemEntity::getItem).filter(stack -> stack.getItem() instanceof GunItem).findFirst()
				.orElse(ItemStack.EMPTY);
		helper.assertTrue(gun.is(JugcraftGuns.GUNS.get("sentry_pistol")) && GunItem.loaded(gun) == 0,
				"The gunner dropped " + gun + ", not an empty Sentry Pistol");
		int light = count(drops, "light_round");
		int others = count(drops, "rifle_round") + count(drops, "buckshot_shell");
		helper.assertTrue(light <= 3 && others == 0, "It dropped " + light + " Light Rounds and " + others + " other rounds");
		helper.succeed();
	}

	/** A raider gunner with this gun, hunting {@code target}. */
	private static RaiderInfantry gunner(GameTestHelper helper, BlockPos pos, String gun, LivingEntity target) {
		RaiderInfantry gunner = helper.spawn(JugcraftRaiders.GUNNER, pos);
		gunner.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(JugcraftGuns.GUNS.get(gun)));
		gunner.setTarget(target);
		return gunner;
	}

	/** A still pig that takes many hits. */
	private static Mob sturdyPig(GameTestHelper helper, BlockPos pos) {
		Mob pig = GunsGameTests.pig(helper, pos);
		pig.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000.0);
		pig.setHealth(1000.0F);
		return pig;
	}

	/** How many of this round lie among the drops. */
	private static int count(List<ItemEntity> drops, String round) {
		Item item = BuiltInRegistries.ITEM.getValue(Jugcraft.id(round));
		return drops.stream().map(ItemEntity::getItem).filter(stack -> stack.is(item)).mapToInt(ItemStack::getCount).sum();
	}
}
