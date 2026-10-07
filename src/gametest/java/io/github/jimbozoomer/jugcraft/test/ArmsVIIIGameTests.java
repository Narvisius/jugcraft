package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.weapons.JugcraftArms;
import io.github.jimbozoomer.jugcraft.weapons.ThrownArm;
import io.github.jimbozoomer.jugcraft.weapons.ThrownArmItem;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * In-game tests for Arms VIII (batch 59): every thrown arm is registered with its kind's thrown numbers; and a mock
 * player's real throws (the item's own release, after a full wind) strike as the server works them. The javelin hits a
 * still pig for its damage and comes down as itself, worn by a throw; the chakram cuts two pigs in line on its way out
 * and again on its way back, and is caught into its thrower's inventory; the harpoon hauls its pig towards the thrower;
 * the francisca knocks a raised shield down; and a creative throw leaves the thrower's arm in hand and nothing behind.
 * Throwers face south (+z) and aim at their foe's middle.
 */
public class ArmsVIIIGameTests {
	private static final String ARENA = "jugcraft-test:arms_arena";

	/** Every thrown arm is a ThrownArmItem with its kind's thrown numbers in its metal. */
	@GameTest
	public void everyThrownArmIsRegistered(GameTestHelper helper) {
		for (JugcraftArms.Thrown thrown : JugcraftArms.THROWN) {
			String id = thrown.metal() + "_" + thrown.name();
			helper.assertTrue(JugcraftArms.ITEMS.get(id) instanceof ThrownArmItem arm && arm.kind().equals(thrown.name()) && arm.thrown() == thrown,
					id + " is not a thrown " + thrown.name() + " with " + thrown);
		}
		helper.assertTrue(JugcraftArms.THROWN.size() == 8, "There are " + JugcraftArms.THROWN.size() + " thrown arms, not 8");
		helper.succeed();
	}

	/** A steel javelin, thrown at a still pig: the pig takes its damage, and the javelin comes down, worn by one. */
	@GameTest(structure = ARENA, maxTicks = 60)
	public void javelinStrikesAndComesDown(GameTestHelper helper) {
		floor(helper);
		Mob pig = pig(helper, new BlockPos(1, 2, 7));
		ServerPlayer thrower = thrower(helper, "steel_javelin", new BlockPos(1, 2, 1), pig, GameType.SURVIVAL);
		JugcraftArms.Thrown thrown = JugcraftArms.thrown("javelin", "steel");
		release(thrower, thrown);
		helper.assertTrue(thrower.getMainHandItem().isEmpty(), "The javelin is still in hand after the throw");
		float max = pig.getMaxHealth();
		helper.succeedWhen(() -> {
			helper.assertTrue(pig.getHealth() < max, "The pig is not struck yet");
			helper.assertTrue(Math.abs(max - pig.getHealth() - thrown.damage()) < 1.0E-3F,
					"The javelin took " + (max - pig.getHealth()) + " from the pig, not " + thrown.damage());
			List<ItemEntity> landed = items(helper, "steel_javelin");
			helper.assertTrue(landed.size() == 1, "The javelin has not come down as an item (" + landed.size() + ")");
			helper.assertTrue(landed.getFirst().getItem().getDamageValue() == JugcraftArms.THROW_WEAR,
					"The javelin is worn by " + landed.getFirst().getItem().getDamageValue() + ", not " + JugcraftArms.THROW_WEAR);
		});
	}

	/** A bronze chakram cuts two pigs in line on its way out and again on its way back, and is caught again. */
	@GameTest(structure = ARENA, maxTicks = 120)
	public void chakramCutsBothWaysAndComesBack(GameTestHelper helper) {
		floor(helper);
		// Two pigs in line on a raised walk, so their middles are near the thrower's eye (and stay up when struck); the
		// corridor the chakram flies out and back along is cleared, whatever was there.
		for (int x = 0; x <= 2; x++) {
			for (int z = 3; z <= 11; z++) {
				helper.setBlock(new BlockPos(x, 2, z), Blocks.STONE);
			}
			for (int z = 0; z <= 15; z++) {
				for (int y = 3; y <= 6; y++) {
					helper.setBlock(new BlockPos(x, y, z), Blocks.AIR);
				}
			}
		}
		Mob near = pig(helper, new BlockPos(1, 3, 4));
		Mob far = pig(helper, new BlockPos(1, 3, 7));
		// Each cut knocks its pig back, which can carry it off the walk or out of the chakram's way back. Here they stand
		// fast, so the test sees the chakram's own path.
		for (Mob pig : List.of(near, far)) {
			pig.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(1.0);
		}
		ServerPlayer thrower = thrower(helper, "bronze_chakram", new BlockPos(1, 2, 0), near, GameType.SURVIVAL);
		JugcraftArms.Thrown thrown = JugcraftArms.thrown("chakram", "bronze");
		release(thrower, thrown);
		float max = near.getMaxHealth();
		helper.succeedWhen(() -> {
			helper.assertTrue(helper.getLevel().getEntitiesOfClass(ThrownArm.class, new AABB(helper.absolutePos(BlockPos.ZERO)).inflate(40)).isEmpty(),
					"The chakram is still in flight");
			boolean caught = false;
			for (int slot = 0; slot < thrower.getInventory().getContainerSize(); slot++) {
				caught |= thrower.getInventory().getItem(slot).is(JugcraftArms.ITEMS.get("bronze_chakram"));
			}
			helper.assertTrue(caught, "The chakram did not come back to its thrower");
			Jugcraft.LOGGER.info("[arms viii] chakram: near pig {} -> {}, far pig {} -> {}", max, near.getHealth(), max, far.getHealth());
			float both = 2.0F * thrown.damage();
			helper.assertTrue(Math.abs(max - near.getHealth() - both) < 1.0E-3F && Math.abs(max - far.getHealth() - both) < 1.0E-3F,
					"The chakram took " + (max - near.getHealth()) + " and " + (max - far.getHealth()) + ", not " + both + " from each pig"
							+ " (the pigs at " + where(helper, near) + " and " + where(helper, far) + ")");
		});
	}

	/** A steel harpoon strikes a pig 8 blocks off and hauls it most of the way in. */
	@GameTest(structure = ARENA, maxTicks = 60)
	public void harpoonHaulsItsCatch(GameTestHelper helper) {
		floor(helper);
		Mob pig = pig(helper, new BlockPos(1, 2, 9));
		ServerPlayer thrower = thrower(helper, "steel_harpoon", new BlockPos(1, 2, 1), pig, GameType.SURVIVAL);
		JugcraftArms.Thrown thrown = JugcraftArms.thrown("harpoon", "steel");
		double start = pig.getZ();
		release(thrower, thrown);
		float max = pig.getMaxHealth();
		helper.succeedWhen(() -> {
			helper.assertTrue(Math.abs(max - pig.getHealth() - thrown.damage()) < 1.0E-3F, "The harpoon took " + (max - pig.getHealth()) + ", not " + thrown.damage());
			helper.assertTrue(pig.getZ() < start - 2.0, "The harpoon has hauled the pig only from " + start + " to " + pig.getZ());
		});
	}

	/** A francisca's blow on a raised shield knocks it down: the shield goes on cooldown and is lowered. */
	@GameTest(structure = ARENA)
	public void franciscaKnocksAShieldDown(GameTestHelper helper) {
		floor(helper);
		ServerPlayer bearer = helper.makeMockServerPlayerInLevel();
		bearer.setGameMode(GameType.SURVIVAL);
		ItemStack shield = new ItemStack(Items.SHIELD);
		bearer.setItemInHand(InteractionHand.OFF_HAND, shield);
		bearer.startUsingItem(InteractionHand.OFF_HAND);
		helper.assertTrue(bearer.isUsingItem(), "The shield is not raised");
		ThrownArm.disableBlocking(helper.getLevel(), bearer);
		helper.assertTrue(bearer.getCooldowns().isOnCooldown(bearer.getOffhandItem()), "The shield is not on cooldown");
		helper.assertTrue(!bearer.isUsingItem(), "The shield is still raised");
		helper.succeed();
	}

	/** A creative player's throw strikes as any other, but the arm stays in hand and nothing comes down. */
	@GameTest(structure = ARENA, maxTicks = 60)
	public void creativeThrowLeavesNothing(GameTestHelper helper) {
		floor(helper);
		Mob pig = pig(helper, new BlockPos(1, 2, 7));
		ServerPlayer thrower = thrower(helper, "bronze_javelin", new BlockPos(1, 2, 1), pig, GameType.CREATIVE);
		release(thrower, JugcraftArms.thrown("javelin", "bronze"));
		helper.assertTrue(thrower.getMainHandItem().is(JugcraftArms.ITEMS.get("bronze_javelin")), "The creative thrower's javelin is gone");
		float max = pig.getMaxHealth();
		helper.succeedWhen(() -> {
			helper.assertTrue(pig.getHealth() < max, "The pig is not struck yet");
			helper.assertTrue(items(helper, "bronze_javelin").isEmpty(), "A creative throw left a javelin behind");
		});
	}

	/** Lets go of the use key after a full wind: the arm's own release, as a real throw. */
	private static void release(ServerPlayer thrower, JugcraftArms.Thrown thrown) {
		ItemStack stack = thrower.getMainHandItem();
		thrower.startUsingItem(InteractionHand.MAIN_HAND);
		stack.getItem().releaseUsing(stack, thrower.level(), thrower, stack.getUseDuration(thrower) - thrown.wind());
	}

	/** A mock player holding `arm`, at `pos`, aiming at `target`'s middle. */
	private static ServerPlayer thrower(GameTestHelper helper, String arm, BlockPos pos, Mob target, GameType mode) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setGameMode(mode);
		BlockPos at = helper.absolutePos(pos);
		player.setPos(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
		Vec3 aim = target.getBoundingBox().getCenter().subtract(player.getEyePosition());
		player.setYRot(0.0F);
		player.setYHeadRot(0.0F);
		player.setXRot((float) Math.toDegrees(Math.atan2(-aim.y, Math.sqrt(aim.x * aim.x + aim.z * aim.z))));
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(JugcraftArms.ITEMS.get(arm)));
		return player;
	}

	/** Where a mob is, relative to the test's origin, for a failure message. */
	private static String where(GameTestHelper helper, Mob mob) {
		BlockPos origin = helper.absolutePos(BlockPos.ZERO);
		return String.format(java.util.Locale.ROOT, "%.2f %.2f %.2f", mob.getX() - origin.getX(), mob.getY() - origin.getY(), mob.getZ() - origin.getZ());
	}

	private static List<ItemEntity> items(GameTestHelper helper, String arm) {
		return helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(BlockPos.ZERO)).inflate(40),
				item -> item.getItem().is(JugcraftArms.ITEMS.get(arm)));
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
		@SuppressWarnings("unchecked")
		EntityType<Mob> type = (EntityType<Mob>) BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.withDefaultNamespace("pig"));
		Mob pig = helper.spawnWithNoFreeWill(type, pos);
		pig.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.0);
		return pig;
	}
}
