package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.mixin.AttackStrengthAccessor;
import io.github.jimbozoomer.jugcraft.weapons.JugcraftArms;
import io.github.jimbozoomer.jugcraft.weapons.TwoHanded;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * In-game tests for Arms IV (batch 47): each new kind's trait as the server works it. The wielder is a mock player
 * facing south (+z) with a full attack charge; the foes are still husks with no armor.
 */
public class ArmsIVGameTests {
	private static final int CHARGED = 100;
	private static final float BLOW = 4.0F;

	/** A labrys's ordinary blow strikes only its arc in front; ahead, behind and both sides are about it. */
	@GameTest
	public void aLabrysOrdinaryBlowKeepsToItsArc(GameTestHelper helper) {
		floor(helper);
		ServerPlayer player = wielder(helper, "steel_labrys", new BlockPos(4, 2, 3));
		Mob ahead = husk(helper, new BlockPos(4, 2, 5));
		List<Mob> about = List.of(husk(helper, new BlockPos(4, 2, 1)), husk(helper, new BlockPos(2, 2, 3)),
				husk(helper, new BlockPos(6, 2, 3)));
		List<LivingEntity> struck = TwoHanded.strike(player, player.getMainHandItem(), JugcraftArms.TWO_HANDED.get("labrys"), false, CHARGED);
		Jugcraft.LOGGER.info("[arms iv] labrys ordinary blow struck {} of 4", struck.size());
		helper.assertTrue(struck.equals(List.of(ahead)) && about.stream().allMatch(foe -> foe.getHealth() == foe.getMaxHealth()),
				"The ordinary blow struck " + struck);
		helper.succeed();
	}

	/** A labrys's finishing blow whirls: it strikes every foe about the wielder, behind and to both sides too. */
	@GameTest
	public void aLabrysFinishingBlowWhirlsRightRound(GameTestHelper helper) {
		floor(helper);
		ServerPlayer player = wielder(helper, "steel_labrys", new BlockPos(4, 2, 3));
		List<Mob> foes = List.of(husk(helper, new BlockPos(4, 2, 5)), husk(helper, new BlockPos(4, 2, 1)),
				husk(helper, new BlockPos(2, 2, 3)), husk(helper, new BlockPos(6, 2, 3)));
		List<LivingEntity> struck = TwoHanded.strike(player, player.getMainHandItem(), JugcraftArms.TWO_HANDED.get("labrys"), true, CHARGED);
		Jugcraft.LOGGER.info("[arms iv] labrys whirl struck {} of 4", struck.size());
		helper.assertTrue(struck.size() == 4 && foes.stream().allMatch(foe -> foe.getHealth() < foe.getMaxHealth()),
				"The whirl struck " + struck.size() + " of the 4 foes about it");
		helper.succeed();
	}

	/** A battleblade's hit wears every piece of the foe's armor by SUNDER more. */
	@GameTest
	public void aBattlebladeSundersArmor(GameTestHelper helper) {
		floor(helper);
		ServerPlayer player = wielder(helper, "bronze_battleblade", new BlockPos(4, 2, 3));
		Mob foe = husk(helper, new BlockPos(4, 2, 5));
		ItemStack helmet = new ItemStack(Items.IRON_HELMET);
		ItemStack chest = new ItemStack(Items.IRON_CHESTPLATE);
		foe.setItemSlot(EquipmentSlot.HEAD, helmet);
		foe.setItemSlot(EquipmentSlot.CHEST, chest);
		ItemStack blade = player.getMainHandItem();
		blade.getItem().hurtEnemy(blade, foe, player);
		Jugcraft.LOGGER.info("[arms iv] battleblade: helmet wear {}, chestplate wear {}", foe.getItemBySlot(EquipmentSlot.HEAD).getDamageValue(),
				foe.getItemBySlot(EquipmentSlot.CHEST).getDamageValue());
		helper.assertTrue(foe.getItemBySlot(EquipmentSlot.HEAD).getDamageValue() == JugcraftArms.SUNDER
				&& foe.getItemBySlot(EquipmentSlot.CHEST).getDamageValue() == JugcraftArms.SUNDER, "The battleblade did not sunder the armor");
		helper.succeed();
	}

	/** A war fork adds BRACE of a blow against a foe coming at its wielder, and nothing against one standing or going. */
	@GameTest
	public void aWarForkBracesAgainstACharge(GameTestHelper helper) {
		floor(helper);
		Mob wielder = husk(helper, new BlockPos(6, 2, 3));
		Mob coming = husk(helper, new BlockPos(2, 2, 3));
		Mob going = husk(helper, new BlockPos(2, 2, 5));
		Mob standing = husk(helper, new BlockPos(2, 2, 1));
		coming.xo = coming.getX() - 0.3;
		going.xo = going.getX() + 0.3;
		standing.xo = standing.getX();
		ItemStack fork = arm("bronze_war_fork");
		float onComing = fork.getItem().getAttackDamageBonus(coming, BLOW, helper.getLevel().damageSources().mobAttack(wielder));
		float onGoing = fork.getItem().getAttackDamageBonus(going, BLOW, helper.getLevel().damageSources().mobAttack(wielder));
		float onStanding = fork.getItem().getAttackDamageBonus(standing, BLOW, helper.getLevel().damageSources().mobAttack(wielder));
		Jugcraft.LOGGER.info("[arms iv] war fork on a blow of {}: coming {}, going {}, standing {}", BLOW, onComing, onGoing, onStanding);
		helper.assertTrue(near(onComing, BLOW * JugcraftArms.BRACE) && near(onGoing, 0.0F) && near(onStanding, 0.0F),
				"The war fork gained " + onComing + ", " + onGoing + ", " + onStanding);
		helper.succeed();
	}

	/** A kama used on grass cuts the grass and leaves about it, 3 by 3 by 3, and nothing further off; it wears by each. */
	@GameTest
	public void aKamaClearsGrassAndLeaves(GameTestHelper helper) {
		for (int x = 0; x <= 7; x++) {
			for (int z = 0; z <= 7; z++) {
				helper.setBlock(new BlockPos(x, 1, z), Blocks.GRASS_BLOCK);
			}
		}
		for (int x = 2; x <= 4; x++) {
			for (int z = 2; z <= 4; z++) {
				helper.setBlock(new BlockPos(x, 2, z), Blocks.SHORT_GRASS);
			}
		}
		helper.setBlock(new BlockPos(3, 3, 3), Blocks.OAK_LEAVES);
		helper.setBlock(new BlockPos(6, 2, 3), Blocks.SHORT_GRASS);
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setGameMode(GameType.SURVIVAL);
		BlockPos stand = helper.absolutePos(new BlockPos(3, 2, 6));
		player.setPos(stand.getX() + 0.5, stand.getY(), stand.getZ() + 0.5);
		ItemStack kama = arm("steel_kama");
		player.setItemInHand(InteractionHand.MAIN_HAND, kama);
		BlockPos center = helper.absolutePos(new BlockPos(3, 2, 3));
		InteractionResult result = player.gameMode.useItemOn(player, helper.getLevel(), kama, InteractionHand.MAIN_HAND,
				new BlockHitResult(Vec3.atCenterOf(center), Direction.UP, center, false));
		int left = 0;
		for (int x = 2; x <= 4; x++) {
			for (int z = 2; z <= 4; z++) {
				if (helper.getBlockState(new BlockPos(x, 2, z)).is(Blocks.SHORT_GRASS)) {
					left++;
				}
			}
		}
		BlockState leaves = helper.getBlockState(new BlockPos(3, 3, 3));
		boolean outside = helper.getBlockState(new BlockPos(6, 2, 3)).is(Blocks.SHORT_GRASS);
		Jugcraft.LOGGER.info("[arms iv] kama: {}, {} grass left of 9, leaves {}, the grass outside kept {}, wear {}", result, left,
				leaves, outside, kama.getDamageValue());
		helper.assertTrue(result.consumesAction() && left == 0 && leaves.isAir() && outside,
				"The kama cut wrongly: " + left + " grass left, leaves " + leaves + ", outside kept " + outside);
		helper.assertTrue(kama.getDamageValue() == 10 * JugcraftArms.CLEAR_WEAR, "The kama wore by " + kama.getDamageValue());
		helper.succeed();
	}

	/** A war pick mines stone and ore as its metal's pickaxe: bronze as iron (diamond ore, not obsidian), steel as diamond (obsidian too). */
	@GameTest
	public void aWarPickDelvesAsAPickaxe(GameTestHelper helper) {
		ItemStack bronze = arm("bronze_war_pick");
		ItemStack steel = arm("steel_war_pick");
		float stone = bronze.getDestroySpeed(Blocks.STONE.defaultBlockState());
		float log = bronze.getDestroySpeed(Blocks.OAK_LOG.defaultBlockState());
		boolean bronzeIron = bronze.isCorrectToolForDrops(Blocks.IRON_ORE.defaultBlockState());
		boolean bronzeDiamond = bronze.isCorrectToolForDrops(Blocks.DIAMOND_ORE.defaultBlockState());
		boolean bronzeObsidian = bronze.isCorrectToolForDrops(Blocks.OBSIDIAN.defaultBlockState());
		boolean steelObsidian = steel.isCorrectToolForDrops(Blocks.OBSIDIAN.defaultBlockState());
		Jugcraft.LOGGER.info("[arms iv] war pick: stone {}, log {}; bronze on iron ore {}, diamond ore {}, obsidian {}; steel on obsidian {}",
				stone, log, bronzeIron, bronzeDiamond, bronzeObsidian, steelObsidian);
		helper.assertTrue(stone > 1.0F && near(log, 1.0F), "The war pick mines stone at " + stone + " and logs at " + log);
		helper.assertTrue(bronzeIron && bronzeDiamond && !bronzeObsidian && steelObsidian, "The war picks' mining tiers are wrong");
		helper.succeed();
	}

	private static ItemStack arm(String id) {
		return new ItemStack(JugcraftArms.ITEMS.get(id));
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
		player.setItemInHand(InteractionHand.MAIN_HAND, arm(arm));
		((AttackStrengthAccessor) player).jugcraft$setAttackStrengthTicker(CHARGED);
		return player;
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

	/** A still husk with no armor. */
	private static Mob husk(GameTestHelper helper, BlockPos pos) {
		@SuppressWarnings("unchecked")
		EntityType<Mob> type = (EntityType<Mob>) BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.withDefaultNamespace("husk"));
		Mob husk = helper.spawnWithNoFreeWill(type, pos);
		husk.getAttribute(Attributes.ARMOR).setBaseValue(0.0);
		return husk;
	}
}
