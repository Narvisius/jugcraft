package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.mixin.AttackStrengthAccessor;
import io.github.jimbozoomer.jugcraft.weapons.JugcraftArms;
import io.github.jimbozoomer.jugcraft.weapons.TwoHanded;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;

/**
 * In-game tests for Arms III (batch 46): two-handed swings as the server works them (weapons/TwoHanded) and the new
 * kinds' traits. The wielder is a mock player facing south (+z) with a full attack charge; the foes are still husks
 * with no armor, so a blow takes exactly its damage off. The mock player does not tick, so its attack damage is read
 * back from it rather than assumed.
 */
public class ArmsIIIGameTests {
	/** An attack-strength ticker past any arm's delay: a full charge. */
	private static final int CHARGED = 100;
	private static final float HEALTH = 20.0F;

	/** A greatsword's blow lands at its strike tick, not before, on every foe in its arc, and on none behind or aside. */
	@GameTest(maxTicks = 60)
	public void aTwoHandedBlowLandsLateOnEveryFoeInItsArc(GameTestHelper helper) {
		floor(helper);
		ServerPlayer player = wielder(helper, "steel_greatsword", new BlockPos(4, 2, 1));
		Mob left = husk(helper, new BlockPos(3, 2, 3));
		Mob right = husk(helper, new BlockPos(5, 2, 3));
		Mob ahead = husk(helper, new BlockPos(4, 2, 4));
		Mob behind = husk(helper, new BlockPos(4, 2, 0));
		Mob aside = husk(helper, new BlockPos(7, 2, 1));
		float blow = (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE);
		JugcraftArms.Heavy heavy = JugcraftArms.TWO_HANDED.get("greatsword");
		helper.assertTrue(TwoHanded.start(player), "The greatsword's swing did not start");
		helper.assertTrue(slowed(player) && TwoHanded.swinging(player), "The wielder is not slowed while the swing is in the air");
		helper.runAfterDelay(heavy.strike() - 1, () -> helper.assertTrue(List.of(left, right, ahead).stream()
				.allMatch(foe -> foe.getHealth() == HEALTH), "The blow landed before its strike tick"));
		helper.runAfterDelay(heavy.strike() + 2, () -> {
			Jugcraft.LOGGER.info("[arms iii] greatsword blow of {}: left {}, right {}, ahead {}, behind {}, aside {}", blow,
					left.getHealth(), right.getHealth(), ahead.getHealth(), behind.getHealth(), aside.getHealth());
			for (Mob foe : List.of(left, right, ahead)) {
				helper.assertTrue(near(HEALTH - foe.getHealth(), blow), "A foe in the arc took " + (HEALTH - foe.getHealth()) + ", not " + blow);
			}
			helper.assertTrue(behind.getHealth() == HEALTH && aside.getHealth() == HEALTH, "The blow struck behind or aside");
		});
		helper.runAfterDelay(26, () -> {
			helper.assertTrue(!slowed(player) && !TwoHanded.swinging(player), "The swing never ended, or left its wielder slow");
			helper.succeed();
		});
	}

	/** The second blow of a greatsword's combo, within the combo window, is the finishing blow: FINISHER times as strong. */
	@GameTest(maxTicks = 80)
	public void aComboEndsInAFinishingBlow(GameTestHelper helper) {
		floor(helper);
		ServerPlayer player = wielder(helper, "steel_greatsword", new BlockPos(4, 2, 1));
		Mob foe = husk(helper, new BlockPos(4, 2, 3));
		foe.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100.0);
		foe.setHealth(100.0F);
		float blow = (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE);
		helper.assertTrue(TwoHanded.start(player), "The first swing did not start");
		helper.runAfterDelay(22, () -> {
			helper.assertTrue(near(100.0F - foe.getHealth(), blow), "The first blow took " + (100.0F - foe.getHealth()));
			charge(player);
			helper.assertTrue(TwoHanded.start(player), "The second swing did not start");
		});
		helper.runAfterDelay(22 + JugcraftArms.TWO_HANDED.get("greatsword").strike() + 2, () -> {
			float second = 100.0F - foe.getHealth() - blow;
			Jugcraft.LOGGER.info("[arms iii] greatsword combo: {} then {}", blow, second);
			helper.assertTrue(near(second, blow * JugcraftArms.FINISHER), "The finishing blow took " + second);
			helper.succeed();
		});
	}

	/** A shield in the off hand leaves no hand for the grip: the swing does not start. A torch does not stop it. */
	@GameTest
	public void aShieldInTheOffHandStopsATwoHandedSwing(GameTestHelper helper) {
		floor(helper);
		ServerPlayer player = wielder(helper, "bronze_war_hammer", new BlockPos(4, 2, 1));
		player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.SHIELD));
		boolean shielded = TwoHanded.start(player);
		player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.TORCH));
		boolean torch = TwoHanded.start(player);
		Jugcraft.LOGGER.info("[arms iii] war hammer swings with a shield: {}, with a torch: {}", shielded, torch);
		helper.assertTrue(!shielded && torch, "A shield did not stop the swing, or a torch did");
		helper.succeed();
	}

	/** Switching away from the arm before the blow lands cancels the swing. */
	@GameTest(maxTicks = 40)
	public void switchingAwayCancelsTheSwing(GameTestHelper helper) {
		floor(helper);
		ServerPlayer player = wielder(helper, "steel_glaive", new BlockPos(4, 2, 1));
		Mob foe = husk(helper, new BlockPos(4, 2, 3));
		helper.assertTrue(TwoHanded.start(player), "The glaive's swing did not start");
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STICK));
		helper.runAfterDelay(JugcraftArms.TWO_HANDED.get("glaive").strike() + 3, () -> {
			helper.assertTrue(foe.getHealth() == HEALTH && !TwoHanded.swinging(player) && !slowed(player),
					"The swing went on after the glaive was put away");
			helper.succeed();
		});
	}

	/** A maul's finishing blow shakes the ground: a foe close behind takes a share and is slowed; one further off is not. */
	@GameTest
	public void aMaulsFinishingBlowShakesTheGround(GameTestHelper helper) {
		floor(helper);
		// In the middle of the test's floor, so the quake reaches nothing of a neighbouring test.
		ServerPlayer player = wielder(helper, "steel_maul", new BlockPos(4, 2, 3));
		Mob ahead = husk(helper, new BlockPos(4, 2, 5));
		Mob close = husk(helper, new BlockPos(4, 2, 2));
		Mob far = husk(helper, new BlockPos(0, 2, 3));
		float blow = (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE) * JugcraftArms.FINISHER;
		List<LivingEntity> struck = TwoHanded.strike(player, player.getMainHandItem(), JugcraftArms.TWO_HANDED.get("maul"), true, CHARGED);
		MobEffectInstance slowed = close.getEffect(MobEffects.SLOWNESS);
		Jugcraft.LOGGER.info("[arms iii] maul finishing blow of {}: struck {}, ahead {}, close {} ({}), far {}", blow, struck.size(),
				ahead.getHealth(), close.getHealth(), slowed, far.getHealth());
		helper.assertTrue(struck.equals(List.of(ahead)) && near(HEALTH - ahead.getHealth(), blow), "The cleave struck " + struck);
		helper.assertTrue(near(HEALTH - close.getHealth(), blow * JugcraftArms.QUAKE_SHARE), "The quake took " + (HEALTH - close.getHealth()));
		helper.assertTrue(slowed != null && slowed.getAmplifier() == JugcraftArms.QUAKE_AMPLIFIER, "The quake left " + slowed);
		helper.assertTrue(far.getHealth() == HEALTH && far.getEffect(MobEffects.SLOWNESS) == null, "The quake reached too far");
		helper.succeed();
	}

	/** An executioner's sword adds EXECUTE of a blow against a foe at or below EXECUTE_HEALTH, and nothing against a hale one. */
	@GameTest
	public void anExecutionerFinishesTheWounded(GameTestHelper helper) {
		floor(helper);
		ServerPlayer player = wielder(helper, "bronze_executioner", new BlockPos(4, 2, 1));
		Mob hale = husk(helper, new BlockPos(3, 2, 4));
		Mob wounded = husk(helper, new BlockPos(5, 2, 4));
		wounded.setHealth(HEALTH * JugcraftArms.EXECUTE_HEALTH);
		ItemStack sword = player.getMainHandItem();
		float onHale = sword.getItem().getAttackDamageBonus(hale, 4.0F, helper.getLevel().damageSources().playerAttack(player));
		float onWounded = sword.getItem().getAttackDamageBonus(wounded, 4.0F, helper.getLevel().damageSources().playerAttack(player));
		Jugcraft.LOGGER.info("[arms iii] executioner on a blow of 4: hale {}, wounded {}", onHale, onWounded);
		helper.assertTrue(near(onHale, 0.0F) && near(onWounded, 4.0F * JugcraftArms.EXECUTE), "The executioner gained " + onHale
				+ " and " + onWounded);
		helper.succeed();
	}

	/** A bill's blow hooks: it drags a rider from the saddle and pulls it towards the wielder. */
	@GameTest
	public void aBillHooksRidersOutOfTheSaddle(GameTestHelper helper) {
		floor(helper);
		ServerPlayer player = wielder(helper, "steel_bill", new BlockPos(4, 2, 1));
		Mob horse = mob(helper, "horse", new BlockPos(4, 2, 4));
		Mob rider = husk(helper, new BlockPos(4, 2, 4));
		helper.assertTrue(rider.startRiding(horse, true, true), "The husk would not mount the horse");
		List<LivingEntity> struck = TwoHanded.strike(player, player.getMainHandItem(), JugcraftArms.TWO_HANDED.get("bill"), false, CHARGED);
		Jugcraft.LOGGER.info("[arms iii] bill: struck {}, rider riding {}, rider moving {}", struck.size(), rider.isPassenger(),
				rider.getDeltaMovement());
		helper.assertTrue(struck.contains(rider), "The bill did not reach the rider");
		helper.assertTrue(!rider.isPassenger(), "The rider kept the saddle");
		helper.assertTrue(rider.getDeltaMovement().z < 0.0, "The rider was not pulled towards the wielder: " + rider.getDeltaMovement());
		helper.succeed();
	}

	/** A mock player facing south (+z) at pos, in survival, the arm in hand and a full attack charge. */
	private static ServerPlayer wielder(GameTestHelper helper, String arm, BlockPos pos) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setGameMode(GameType.SURVIVAL);
		BlockPos at = helper.absolutePos(pos);
		player.setPos(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
		player.setYRot(0.0F);
		player.setXRot(0.0F);
		player.setYHeadRot(0.0F);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(JugcraftArms.ITEMS.get(arm)));
		charge(player);
		return player;
	}

	private static void charge(ServerPlayer player) {
		((AttackStrengthAccessor) player).jugcraft$setAttackStrengthTicker(CHARGED);
	}

	private static boolean slowed(ServerPlayer player) {
		return player.getAttribute(Attributes.MOVEMENT_SPEED).hasModifier(TwoHanded.SLOW);
	}

	private static boolean near(float a, float b) {
		return Math.abs(a - b) < 1.0E-3F;
	}

	private static void floor(GameTestHelper helper) {
		for (int x = 0; x <= 7; x++) {
			for (int z = 0; z <= 7; z++) {
				helper.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
			}
		}
	}

	private static Mob mob(GameTestHelper helper, String id, BlockPos pos) {
		@SuppressWarnings("unchecked")
		EntityType<Mob> type = (EntityType<Mob>) BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.withDefaultNamespace(id));
		return helper.spawnWithNoFreeWill(type, pos);
	}

	/** A still husk with no armor, at full health. */
	private static Mob husk(GameTestHelper helper, BlockPos pos) {
		Mob husk = mob(helper, "husk", pos);
		husk.getAttribute(Attributes.ARMOR).setBaseValue(0.0);
		husk.setHealth(HEALTH);
		return husk;
	}
}
