package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.gear.JugcraftGear;
import io.github.jimbozoomer.jugcraft.mixin.AttackStrengthAccessor;
import io.github.jimbozoomer.jugcraft.weapons.ArmItem;
import io.github.jimbozoomer.jugcraft.weapons.ArmShieldItem;
import io.github.jimbozoomer.jugcraft.weapons.ArmVariants;
import io.github.jimbozoomer.jugcraft.weapons.JugcraftArms;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.component.BlocksAttacks;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

/**
 * In-game tests for Arms VII (batch 56): every variant is registered as an arm of its kind with its line's perk; each
 * boon does what it says when its arm strikes, as the server works it; each style's smithing recipe and pattern recipe
 * load; each boss's loot table drops one of its two trophies; an armor set's arm, epic and as hard-wearing as a
 * trophy, has no recipe and no boss table yet (creative only until the owner settles how a set is won); and an armor
 * set's shield blocks as its base shield does. Wielders are
 * mock players facing south (+z) with a full attack charge; foes are still pigs (living, not undead, so poison and the
 * rest take) or, for Gravebane, husks.
 */
public class ArmsVIIGameTests {
	private static final String ARENA = "jugcraft-test:arms_arena";
	private static final int CHARGED = 100;

	/**
	 * Every variant is an arm of its kind, with its line's durability, enchantability and rarity, and its boon; a boss's
	 * trophy and an armor set's arm alike are epic and last twice as long as steel. Every armor set has its arm.
	 */
	@GameTest
	public void everyVariantIsAnArmOfItsKind(GameTestHelper helper) {
		int steel = JugcraftGear.STEEL.durability();
		for (String set : ArmVariants.SETS) {
			helper.assertTrue(ArmVariants.VARIANTS.stream().anyMatch(v -> v.line().equals(set)), "The " + set + " set has no arm");
		}
		for (ArmVariants.Variant variant : ArmVariants.VARIANTS) {
			Item item = ArmVariants.ITEMS.get(variant.name());
			helper.assertTrue(item instanceof ArmItem arm && arm.kind().equals(variant.kind()) && arm.boon() == variant.boon(),
					variant.name() + " is not an arm of " + variant.kind() + " with " + variant.boon());
			ItemStack stack = new ItemStack(item);
			ItemStack base = new ItemStack(JugcraftArms.ITEMS.get("steel_" + variant.kind()));
			boolean style = ArmVariants.STYLES.contains(variant.line());
			// Any other line is a boss's (a trophy) or an armor set's (its arm): both last as a trophy does.
			int durability = variant.line().equals("ironclad") ? steel * ArmVariants.IRONCLAD_DURABILITY
					: style ? base.getMaxDamage() : steel * ArmVariants.TROPHY_DURABILITY;
			helper.assertTrue(stack.getMaxDamage() == durability, variant.name() + " lasts " + stack.getMaxDamage() + ", not " + durability);
			helper.assertTrue(stack.get(DataComponents.RARITY) == (style ? Rarity.UNCOMMON : Rarity.EPIC),
					variant.name() + " is " + stack.get(DataComponents.RARITY));
			if (variant.line().equals("gilded")) {
				helper.assertTrue(stack.get(DataComponents.ENCHANTABLE).value() == ArmVariants.GILDED_ENCHANTABILITY,
						variant.name() + " is not enchantable as gold");
			}
			// Its blow is its kind's in steel.
			helper.assertTrue(attack(stack) == attack(base), variant.name() + " hits for " + attack(stack) + ", not the steel " + variant.kind() + "'s " + attack(base));
		}
		helper.succeed();
	}

	/**
	 * An armor set's shield is a shield of its own shape in its set, blocks as the steel shield of its base kind does (its
	 * delay, axe cooldown and cover; both are built by JugcraftArms.shield from the same numbers), lasts twice as long as
	 * that shield, as a set's arm does against steel, and is epic.
	 */
	@GameTest
	public void setShieldsBlockAsTheirBase(GameTestHelper helper) {
		for (ArmVariants.SetShield shield : ArmVariants.SET_SHIELDS) {
			helper.assertTrue(ArmVariants.SETS.contains(shield.line()), shield.name() + " is of no armor set");
			Item item = ArmVariants.SHIELDS.get(shield.name());
			helper.assertTrue(item instanceof ArmShieldItem armShield && armShield.kind().equals(shield.shape())
					&& shield.line().equals(armShield.line()), shield.name() + " is not a " + shield.shape() + " of the " + shield.line() + " set");
			ItemStack stack = new ItemStack(item);
			ItemStack base = new ItemStack(JugcraftArms.KIT.get(ArmVariants.SET_SHIELD_METAL + "_" + shield.base()));
			BlocksAttacks blocks = stack.get(DataComponents.BLOCKS_ATTACKS);
			BlocksAttacks baseBlocks = base.get(DataComponents.BLOCKS_ATTACKS);
			helper.assertTrue(blocks != null && baseBlocks != null && blocks.blockDelaySeconds() == baseBlocks.blockDelaySeconds()
					&& blocks.disableCooldownScale() == baseBlocks.disableCooldownScale()
					&& blocks.damageReductions().equals(baseBlocks.damageReductions()),
					shield.name() + " blocks as " + blocks + ", not as its base " + baseBlocks);
			helper.assertTrue(stack.getMaxDamage() == base.getMaxDamage() * ArmVariants.TROPHY_DURABILITY,
					shield.name() + " lasts " + stack.getMaxDamage() + ", not twice its base's " + base.getMaxDamage());
			helper.assertTrue(stack.get(DataComponents.RARITY) == Rarity.EPIC, shield.name() + " is " + stack.get(DataComponents.RARITY));
		}
		helper.succeed();
	}

	/** The effect boons, struck on a still pig: each puts its effect on the foe, at its length and strength. */
	@GameTest(structure = ARENA)
	public void effectBoonsTakeOnTheFoe(GameTestHelper helper) {
		floor(helper);
		check(helper, "glacier_maul", new BlockPos(1, 2, 2), MobEffects.SLOWNESS, ArmVariants.FROST_TICKS, ArmVariants.FROST_AMPLIFIER);
		check(helper, "hagthorn", new BlockPos(3, 2, 2), MobEffects.POISON, ArmVariants.VENOM_TICKS, ArmVariants.VENOM_AMPLIFIER);
		check(helper, "gravewarden", new BlockPos(5, 2, 2), MobEffects.WITHER, ArmVariants.WITHER_TICKS, ArmVariants.WITHER_AMPLIFIER);
		check(helper, "moonfang", new BlockPos(7, 2, 2), MobEffects.WEAKNESS, ArmVariants.HOWL_TICKS, ArmVariants.HOWL_AMPLIFIER);
		check(helper, "runebound_nodachi", new BlockPos(9, 2, 2), MobEffects.GLOWING, ArmVariants.MARK_TICKS, 0);
		// An armor set's arm: the Hades Scythe withers as the Gravewarden does.
		check(helper, "hades_scythe", new BlockPos(13, 2, 2), MobEffects.WITHER, ArmVariants.WITHER_TICKS, ArmVariants.WITHER_AMPLIFIER);
		ServerPlayer player = wielder(helper, "cinderbrand", new BlockPos(11, 2, 0));
		Mob pig = pig(helper, new BlockPos(11, 2, 2));
		strike(player, pig);
		helper.assertTrue(pig.getRemainingFireTicks() == ArmVariants.EMBER_SECONDS * 20, "Ember left the pig burning " + pig.getRemainingFireTicks() + " ticks");
		helper.succeed();
	}

	/** Drain heals the wielder; Gale throws the foe up; Shock arcs to the nearest other foe within reach, and no farther. */
	@GameTest(structure = ARENA, maxTicks = 40)
	public void drainGaleAndShock(GameTestHelper helper) {
		floor(helper);
		ServerPlayer drinker = wielder(helper, "soulreaver", new BlockPos(1, 2, 0));
		drinker.setHealth(10.0F);
		strike(drinker, pig(helper, new BlockPos(1, 2, 2)));
		helper.assertTrue(drinker.getHealth() == 10.0F + ArmVariants.DRAIN_HEAL, "Drain healed the wielder to " + drinker.getHealth());
		ServerPlayer gust = wielder(helper, "stormcaller", new BlockPos(4, 2, 0));
		Mob thrown = pig(helper, new BlockPos(4, 2, 2));
		strike(gust, thrown);
		Vec3 motion = thrown.getDeltaMovement();
		helper.assertTrue(motion.y > 0.2 && motion.z > 0.0, "Gale did not throw the pig up and away: " + motion);
		ServerPlayer smith = wielder(helper, "piston_hammer", new BlockPos(8, 2, 0));
		Mob struck = pig(helper, new BlockPos(8, 2, 2));
		Mob near = pig(helper, new BlockPos(10, 2, 2));
		Mob far = pig(helper, new BlockPos(14, 2, 8));
		float before = near.getHealth();
		strike(smith, struck);
		float expected = (float) smith.getAttributeValue(Attributes.ATTACK_DAMAGE) * ArmVariants.SHOCK_SHARE;
		Jugcraft.LOGGER.info("[arms vii] shock: near pig {} -> {} (expected -{}), far pig {}", before, near.getHealth(), expected, far.getHealth());
		helper.assertTrue(Math.abs(before - near.getHealth() - expected) < 1.0E-3F, "Shock took " + (before - near.getHealth()) + " from the near pig, not " + expected);
		helper.assertTrue(far.getHealth() == far.getMaxHealth() && struck.getHealth() == struck.getMaxHealth(),
				"Shock reached a pig too far off, or struck the struck one again");
		helper.succeed();
	}

	/** Gravebane strikes the undead harder (a husk) and the living (a pig) no harder; Tide strikes a foe in water harder. */
	@GameTest(structure = ARENA, maxTicks = 40)
	public void gravebaneAndTide(GameTestHelper helper) {
		floor(helper);
		ServerPlayer player = wielder(helper, "bonecarved_dagger", new BlockPos(1, 2, 0));
		ItemStack dagger = player.getMainHandItem();
		Mob husk = husk(helper, new BlockPos(1, 2, 2));
		Mob pig = pig(helper, new BlockPos(3, 2, 2));
		float blow = 6.0F;
		// Against the plain steel dagger's own bonus (its backstab still counts from behind), so only the boon is measured.
		Item plain = JugcraftArms.ITEMS.get("steel_dagger");
		DamageSource source = helper.getLevel().damageSources().playerAttack(player);
		float onHusk = dagger.getItem().getAttackDamageBonus(husk, blow, source) - plain.getAttackDamageBonus(husk, blow, source);
		float onPig = dagger.getItem().getAttackDamageBonus(pig, blow, source) - plain.getAttackDamageBonus(pig, blow, source);
		helper.assertTrue(Math.abs(onHusk - blow * ArmVariants.GRAVEBANE) < 1.0E-4F && onPig == 0.0F,
				"Gravebane added " + onHusk + " on a husk and " + onPig + " on a pig");
		ItemStack fork = new ItemStack(ArmVariants.ITEMS.get("tidebreaker"));
		Mob dry = pig(helper, new BlockPos(6, 2, 2));
		BlockPos pool = new BlockPos(9, 2, 2);
		helper.setBlock(pool, Blocks.WATER);
		Mob wet = pig(helper, pool);
		helper.runAfterDelay(3, () -> {
			Item plainFork = JugcraftArms.ITEMS.get("steel_war_fork");
			float onWet = fork.getItem().getAttackDamageBonus(wet, blow, source) - plainFork.getAttackDamageBonus(wet, blow, source);
			float onDry = fork.getItem().getAttackDamageBonus(dry, blow, source) - plainFork.getAttackDamageBonus(dry, blow, source);
			Jugcraft.LOGGER.info("[arms vii] tide: wet {} (in water {}), dry {}", onWet, wet.isInWaterOrRain(), onDry);
			helper.assertTrue(Math.abs(onWet - blow * ArmVariants.TIDE) < 1.0E-4F && onDry == 0.0F,
					"Tide added " + onWet + " in water and " + onDry + " on dry land");
			helper.succeed();
		});
	}

	/**
	 * Every style variant's smithing recipe and every pattern's recipe load; each boss's table drops only its trophies;
	 * an armor set's arm has no recipe, and its set no boss table, yet.
	 */
	@GameTest(maxTicks = 20)
	public void recipesAndTrophyTablesLoad(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		List<String> missing = new ArrayList<>();
		for (ArmVariants.Variant variant : ArmVariants.VARIANTS) {
			if (ArmVariants.STYLES.contains(variant.line())
					&& level.recipeAccess().byKey(ResourceKey.create(Registries.RECIPE, Jugcraft.id(variant.name()))).isEmpty()) {
				missing.add(variant.name());
			}
		}
		for (String pattern : ArmVariants.PATTERN_NAMES) {
			if (level.recipeAccess().byKey(ResourceKey.create(Registries.RECIPE, Jugcraft.id(pattern))).isEmpty()) {
				missing.add(pattern);
			}
		}
		helper.assertTrue(missing.isEmpty(), "These recipes did not load: " + missing);
		// An armor set's arm is creative only until the owner settles how a set is won: no recipe, no boss table.
		for (ArmVariants.Variant variant : ArmVariants.VARIANTS) {
			if (!ArmVariants.SETS.contains(variant.line())) {
				continue;
			}
			helper.assertTrue(level.recipeAccess().byKey(ResourceKey.create(Registries.RECIPE, Jugcraft.id(variant.name()))).isEmpty(),
					variant.name() + ", an armor set's arm, has a recipe");
			LootTable table = level.getServer().reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE,
					Jugcraft.id("bosses/" + variant.line())));
			helper.assertTrue(table == LootTable.EMPTY, "The " + variant.line() + " armor set has a boss's trophy table");
		}
		ServerPlayer player = wielder(helper, null, new BlockPos(1, 2, 1));
		Set<String> bosses = new HashSet<>();
		ArmVariants.VARIANTS.stream().filter(v -> !ArmVariants.STYLES.contains(v.line()) && !ArmVariants.SETS.contains(v.line()))
				.forEach(v -> bosses.add(v.line()));
		for (String boss : bosses) {
			LootTable table = level.getServer().reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE,
					Jugcraft.id("bosses/" + boss)));
			helper.assertTrue(table != LootTable.EMPTY, "The " + boss + "'s trophy table did not load");
			Set<String> dropped = new HashSet<>();
			for (int roll = 0; roll < 40; roll++) {
				LootParams params = new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, player.position())
						.withParameter(LootContextParams.THIS_ENTITY, player).create(LootContextParamSets.GIFT);
				for (ItemStack drop : table.getRandomItems(params)) {
					dropped.add(BuiltInRegistries.ITEM.getKey(drop.getItem()).getPath());
				}
			}
			Set<String> trophies = new HashSet<>();
			ArmVariants.VARIANTS.stream().filter(v -> v.line().equals(boss)).forEach(v -> trophies.add(v.name()));
			helper.assertTrue(dropped.equals(trophies), "The " + boss + " dropped " + dropped + ", not its trophies " + trophies);
		}
		helper.succeed();
	}

	private static void check(GameTestHelper helper, String arm, BlockPos at, net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> effect,
			int ticks, int amplifier) {
		ServerPlayer player = wielder(helper, arm, at.north(2));
		Mob pig = pig(helper, at);
		strike(player, pig);
		MobEffectInstance got = pig.getEffect(effect);
		helper.assertTrue(got != null && got.getDuration() == ticks && got.getAmplifier() == amplifier,
				arm + " put " + got + " on the pig, not " + effect.getRegisteredName() + " " + amplifier + " for " + ticks);
	}

	/** The wielder's arm strikes the foe, as a landed blow calls it (its kind's trait and its boon). */
	private static void strike(ServerPlayer player, Mob foe) {
		ItemStack stack = player.getMainHandItem();
		stack.getItem().hurtEnemy(stack, foe, player);
	}

	/** The attack damage an arm's own modifiers add. */
	private static double attack(ItemStack stack) {
		return stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY).modifiers().stream()
				.filter(entry -> entry.attribute().equals(Attributes.ATTACK_DAMAGE)).mapToDouble(entry -> entry.modifier().amount()).sum();
	}

	private static ServerPlayer wielder(GameTestHelper helper, String arm, BlockPos pos) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setGameMode(GameType.SURVIVAL);
		BlockPos at = helper.absolutePos(pos);
		player.setPos(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
		player.setYRot(0.0F);
		player.setXRot(0.0F);
		player.setYHeadRot(0.0F);
		if (arm != null) {
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ArmVariants.ITEMS.get(arm)));
		}
		((AttackStrengthAccessor) player).jugcraft$setAttackStrengthTicker(CHARGED);
		return player;
	}

	private static void floor(GameTestHelper helper) {
		for (int x = 0; x <= 15; x++) {
			for (int z = 0; z <= 15; z++) {
				helper.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
			}
		}
	}

	/** A pig that cannot walk off (pushes still move it). */
	private static Mob pig(GameTestHelper helper, BlockPos pos) {
		Mob pig = helper.spawnWithNoFreeWill(entity("pig"), pos);
		pig.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.0);
		return pig;
	}

	/** A still husk with no armor (undead). */
	private static Mob husk(GameTestHelper helper, BlockPos pos) {
		Mob husk = helper.spawnWithNoFreeWill(entity("husk"), pos);
		husk.getAttribute(Attributes.ARMOR).setBaseValue(0.0);
		return husk;
	}

	@SuppressWarnings("unchecked")
	private static EntityType<Mob> entity(String name) {
		return (EntityType<Mob>) BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.withDefaultNamespace(name));
	}
}
