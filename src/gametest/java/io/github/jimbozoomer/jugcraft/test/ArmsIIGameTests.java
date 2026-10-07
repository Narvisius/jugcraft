package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.weapons.JugcraftArms;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * In-game tests for Arms II (batch 45): each kind's trait as the server works it. The blow bonuses are what the item adds
 * to a blow of 4 from a husk (the number Player.attack asks the held item for); the flail's daze is its hit hook; the
 * scythe reaps a wheat field for a mock player; the battle axe mines wood as an axe.
 */
public class ArmsIIGameTests {
	private static final float BLOW = 4.0F;

	/** A dagger adds half a blow from behind the target's body, and nothing from in front. */
	@GameTest
	public void aDaggerStabsHarderFromBehind(GameTestHelper helper) {
		floor(helper);
		ItemStack dagger = arm("steel_dagger");
		Mob target = husk(helper, new BlockPos(3, 2, 3), 0.0F);
		Mob behind = husk(helper, new BlockPos(3, 2, 1), 0.0F);
		Mob front = husk(helper, new BlockPos(3, 2, 5), 180.0F);
		float back = bonus(helper, dagger, target, behind);
		float face = bonus(helper, dagger, target, front);
		Jugcraft.LOGGER.info("[arms ii] dagger: a blow of {} gains {} from behind, {} from in front", BLOW, back, face);
		helper.assertTrue(near(back, BLOW * JugcraftArms.BACKSTAB), "A backstab gained " + back);
		helper.assertTrue(near(face, 0.0F), "A stab from in front gained " + face);
		helper.succeed();
	}

	/** A sabre adds SADDLE from a horse's back, and nothing on foot. */
	@GameTest
	public void aSabreCutsHarderFromTheSaddle(GameTestHelper helper) {
		floor(helper);
		ItemStack sabre = arm("bronze_sabre");
		Mob target = husk(helper, new BlockPos(1, 2, 1), 0.0F);
		Mob rider = husk(helper, new BlockPos(4, 2, 4), 0.0F);
		Mob horse = mob(helper, "horse", new BlockPos(4, 2, 4));
		helper.assertTrue(rider.startRiding(horse, true, true), "The husk would not mount the horse");
		Mob walker = husk(helper, new BlockPos(1, 2, 4), 0.0F);
		float mounted = bonus(helper, sabre, target, rider);
		float afoot = bonus(helper, sabre, target, walker);
		Jugcraft.LOGGER.info("[arms ii] sabre: {} from the saddle, {} on foot", mounted, afoot);
		helper.assertTrue(near(mounted, JugcraftArms.SADDLE), "A cut from the saddle gained " + mounted);
		helper.assertTrue(near(afoot, 0.0F), "A cut on foot gained " + afoot);
		helper.succeed();
	}

	/** An estoc adds ARMOR_PIERCE per point of armor, up to ARMOR_PIERCE_MAX, and nothing against no armor. */
	@GameTest
	public void anEstocPiercesArmor(GameTestHelper helper) {
		floor(helper);
		ItemStack estoc = arm("steel_estoc");
		Mob attacker = husk(helper, new BlockPos(1, 2, 1), 0.0F);
		Mob bare = husk(helper, new BlockPos(1, 2, 4), 0.0F);
		Mob mailed = husk(helper, new BlockPos(4, 2, 4), 0.0F);
		mailed.getAttribute(Attributes.ARMOR).setBaseValue(10.0);
		Mob plated = husk(helper, new BlockPos(6, 2, 4), 0.0F);
		plated.getAttribute(Attributes.ARMOR).setBaseValue(30.0);
		float none = bonus(helper, estoc, bare, attacker);
		float ten = bonus(helper, estoc, mailed, attacker);
		float thirty = bonus(helper, estoc, plated, attacker);
		Jugcraft.LOGGER.info("[arms ii] estoc: armor 0 gains {}, 10 gains {}, 30 gains {}", none, ten, thirty);
		helper.assertTrue(near(none, 0.0F), "No armor gained " + none);
		helper.assertTrue(near(ten, 10 * JugcraftArms.ARMOR_PIERCE), "Armor 10 gained " + ten);
		helper.assertTrue(near(thirty, JugcraftArms.ARMOR_PIERCE_MAX), "Armor 30 gained " + thirty);
		helper.succeed();
	}

	/** A pike adds half a blow against a rider and against its mount, and nothing against a foe on foot. */
	@GameTest
	public void aPikeHitsRidersAndMountsHarder(GameTestHelper helper) {
		floor(helper);
		ItemStack pike = arm("bronze_pike");
		Mob attacker = husk(helper, new BlockPos(1, 2, 1), 0.0F);
		Mob rider = husk(helper, new BlockPos(4, 2, 4), 0.0F);
		Mob horse = mob(helper, "horse", new BlockPos(4, 2, 4));
		helper.assertTrue(rider.startRiding(horse, true, true), "The husk would not mount the horse");
		Mob walker = husk(helper, new BlockPos(1, 2, 5), 0.0F);
		float onRider = bonus(helper, pike, rider, attacker);
		float onMount = bonus(helper, pike, horse, attacker);
		float onWalker = bonus(helper, pike, walker, attacker);
		Jugcraft.LOGGER.info("[arms ii] pike: rider {}, mount {}, on foot {}", onRider, onMount, onWalker);
		helper.assertTrue(near(onRider, BLOW * JugcraftArms.RIDERS), "A rider gained " + onRider);
		helper.assertTrue(near(onMount, BLOW * JugcraftArms.RIDERS), "A mount gained " + onMount);
		helper.assertTrue(near(onWalker, 0.0F), "A foe on foot gained " + onWalker);
		helper.succeed();
	}

	/** A flail's hit slows the foe: Slowness at DAZE_AMPLIFIER for DAZE_TICKS. */
	@GameTest
	public void aFlailDazes(GameTestHelper helper) {
		floor(helper);
		ItemStack flail = arm("steel_flail");
		Mob attacker = husk(helper, new BlockPos(1, 2, 1), 0.0F);
		Mob target = husk(helper, new BlockPos(1, 2, 3), 180.0F);
		flail.getItem().hurtEnemy(flail, target, attacker);
		MobEffectInstance daze = target.getEffect(MobEffects.SLOWNESS);
		Jugcraft.LOGGER.info("[arms ii] flail: the foe has {}", daze);
		helper.assertTrue(daze != null && daze.getAmplifier() == JugcraftArms.DAZE_AMPLIFIER
				&& daze.getDuration() == JugcraftArms.DAZE_TICKS, "The flail's hit left " + daze);
		helper.succeed();
	}

	/** A battle axe mines wood as an axe does; a war hammer does not. */
	@GameTest
	public void aBattleAxeChopsWood(GameTestHelper helper) {
		BlockState log = Blocks.OAK_LOG.defaultBlockState();
		float axe = arm("bronze_battle_axe").getDestroySpeed(log);
		float hammer = arm("bronze_war_hammer").getDestroySpeed(log);
		float stone = arm("bronze_battle_axe").getDestroySpeed(Blocks.STONE.defaultBlockState());
		Jugcraft.LOGGER.info("[arms ii] battle axe on a log {}, on stone {}; war hammer on a log {}", axe, stone, hammer);
		helper.assertTrue(axe > 1.0F, "The battle axe mines a log at " + axe);
		helper.assertTrue(near(hammer, 1.0F) && near(stone, 1.0F), "Something else mines too fast: hammer " + hammer
				+ ", stone " + stone);
		helper.succeed();
	}

	/**
	 * A scythe used on the farmland under a ripe crop reaps every ripe crop within REAP_RADIUS and replants it: the ripe
	 * wheat around the middle comes back at age 0, a young one and a ripe one outside the reach are left alone, the
	 * harvest lies on the ground, and the scythe wears by one for each.
	 */
	@GameTest
	public void aScytheReapsAndReplants(GameTestHelper helper) {
		floor(helper);
		BlockState ripe = Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE, 7);
		BlockState young = Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE, 3);
		for (int x = 1; x <= 5; x++) {
			for (int z = 1; z <= 3; z++) {
				helper.setBlock(new BlockPos(x, 1, z), Blocks.FARMLAND);
				helper.setBlock(new BlockPos(x, 2, z), x <= 3 ? ripe : Blocks.AIR.defaultBlockState());
			}
		}
		helper.setBlock(new BlockPos(3, 2, 3), young);
		helper.setBlock(new BlockPos(5, 2, 2), ripe);
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setGameMode(GameType.SURVIVAL);
		BlockPos stand = helper.absolutePos(new BlockPos(2, 2, 5));
		player.setPos(stand.getX() + 0.5, stand.getY(), stand.getZ() + 0.5);
		ItemStack scythe = arm("steel_scythe");
		player.setItemInHand(InteractionHand.MAIN_HAND, scythe);
		BlockPos farmland = helper.absolutePos(new BlockPos(2, 1, 2));
		InteractionResult result = player.gameMode.useItemOn(player, helper.getLevel(), scythe, InteractionHand.MAIN_HAND,
				new BlockHitResult(Vec3.atCenterOf(farmland).relative(Direction.UP, 0.5), Direction.UP, farmland, false));
		int replanted = 0;
		for (int x = 1; x <= 3; x++) {
			for (int z = 1; z <= 3; z++) {
				BlockState state = helper.getBlockState(new BlockPos(x, 2, z));
				if (state.is(Blocks.WHEAT) && state.getValue(CropBlock.AGE) == 0) {
					replanted++;
				}
			}
		}
		int youngAge = helper.getBlockState(new BlockPos(3, 2, 3)).getValue(CropBlock.AGE);
		int outsideAge = helper.getBlockState(new BlockPos(5, 2, 2)).getValue(CropBlock.AGE);
		ServerLevel level = helper.getLevel();
		int wheat = level.getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(BlockPos.ZERO)).inflate(8.0)).stream()
				.filter(item -> item.getItem().is(Items.WHEAT)).mapToInt(item -> item.getItem().getCount()).sum();
		Jugcraft.LOGGER.info("[arms ii] scythe: {}, {} replanted, young age {}, outside age {}, {} wheat dropped, wear {}",
				result, replanted, youngAge, outsideAge, wheat, scythe.getDamageValue());
		helper.assertTrue(result.consumesAction(), "Using the scythe on the field did nothing: " + result);
		helper.assertTrue(replanted == 8, replanted + " of the 8 ripe crops were reaped and replanted");
		helper.assertTrue(youngAge == 3 && outsideAge == 7, "The scythe touched a young crop or one out of reach");
		helper.assertTrue(wheat == 8, wheat + " wheat lie on the ground, not 8");
		helper.assertTrue(scythe.getDamageValue() == 8 * JugcraftArms.REAP_WEAR, "The scythe wore by " + scythe.getDamageValue());
		helper.succeed();
	}

	private static ItemStack arm(String id) {
		return new ItemStack(JugcraftArms.ITEMS.get(id));
	}

	private static float bonus(GameTestHelper helper, ItemStack stack, LivingEntity target, LivingEntity attacker) {
		return stack.getItem().getAttackDamageBonus(target, BLOW, helper.getLevel().damageSources().mobAttack(attacker));
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

	/** A still husk with no armor, facing south (yaw 0) or north (180). */
	private static Mob husk(GameTestHelper helper, BlockPos pos, float yaw) {
		Mob husk = mob(helper, "husk", pos);
		husk.getAttribute(Attributes.ARMOR).setBaseValue(0.0);
		husk.setYRot(yaw);
		husk.setYHeadRot(yaw);
		husk.setYBodyRot(yaw);
		return husk;
	}
}
