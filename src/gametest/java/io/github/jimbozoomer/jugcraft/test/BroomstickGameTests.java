package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.agriculture.Broomstick;
import io.github.jimbozoomer.jugcraft.agriculture.JugcraftAgriculture;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * In-game tests for the flying broomstick (fall addition 22): how it flies (forward along the look, braking, climbing,
 * its top speed, a witch hat's boost, sinking when dry); the item lays it out and seats the player, or only lays it out
 * when dry; Flying Ointment anoints it up to its limit, sneak-use takes it back with its charge, and a blow breaks a
 * riderless one into its item; the server burns its charge in the air, throws off a rider who moves it impossibly far,
 * and leaves them slow falling; and its data loads.
 */
public class BroomstickGameTests {
	private static final double EPSILON = 1.0E-9;

	private static Item item(String id) {
		return JugcraftAgriculture.item(id);
	}

	private static ServerPlayer player(GameTestHelper helper, BlockPos standAt) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setGameMode(GameType.SURVIVAL);
		BlockPos absolute = helper.absolutePos(standAt);
		player.setPos(absolute.getX() + 0.5, absolute.getY(), absolute.getZ() + 0.5);
		return player;
	}

	private static boolean earned(ServerPlayer player, String id) {
		AdvancementHolder advancement = player.level().getServer().getAdvancements().get(Jugcraft.id(id));
		return advancement != null && player.getAdvancements().getOrStartProgress(advancement).isDone();
	}

	private static int carried(ServerPlayer player, Item item) {
		int count = 0;
		for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
			ItemStack stack = player.getInventory().getItem(slot);
			if (stack.is(item)) {
				count += stack.getCount();
			}
		}
		return count;
	}

	private static ItemStack broomItem(int charge) {
		ItemStack stack = new ItemStack(item(Broomstick.ITEM));
		stack.set(JugcraftAgriculture.BROOM_CHARGE, charge);
		return stack;
	}

	private static Broomstick broomAt(GameTestHelper helper, Vec3 relative, int charge) {
		ServerLevel level = helper.getLevel();
		Broomstick broom = new Broomstick(level, helper.absoluteVec(relative), 0.0F, charge);
		level.addFreshEntity(broom);
		return broom;
	}

	/**
	 * Forward pushes it along the rider's look, up when they look up; with nothing pressed it slows; braking slows it
	 * faster; jump climbs; held forward it tops out below its top speed, which a witch hat raises by a quarter; dry, it
	 * can't speed up or climb and sinks no faster than it may.
	 */
	@GameTest
	public void theBroomFliesWhereItsRiderLooks(GameTestHelper helper) {
		Vec3 ahead = Broomstick.fly(Vec3.ZERO, 0.0F, 0.0F, 1.0F, 0.0F, false, false, false);
		helper.assertTrue(ahead.z > 0 && Math.abs(ahead.x) < EPSILON && Math.abs(ahead.y) < EPSILON, "Forward, looking south, flies south");
		Vec3 upward = Broomstick.fly(Vec3.ZERO, 90.0F, -45.0F, 1.0F, 0.0F, false, false, false);
		helper.assertTrue(upward.y > 0 && upward.x < 0 && Math.abs(upward.z) < EPSILON, "Looking up to the west, it climbs west");
		Vec3 cruising = new Vec3(0.0, 0.0, 0.4);
		Vec3 coasting = Broomstick.fly(cruising, 0.0F, 0.0F, 0.0F, 0.0F, false, false, false);
		Vec3 braking = Broomstick.fly(cruising, 0.0F, 0.0F, -1.0F, 0.0F, false, false, false);
		helper.assertTrue(coasting.z < cruising.z && braking.z < coasting.z, "It slows with nothing pressed, faster braking");
		helper.assertTrue(Broomstick.fly(Vec3.ZERO, 0.0F, 0.0F, 0.0F, 0.0F, true, false, false).y > 0, "Jump climbs");
		helper.assertTrue(Broomstick.fly(Vec3.ZERO, 0.0F, 0.0F, 0.0F, 1.0F, false, false, false).x < 0, "Right, facing south, drifts west");

		Vec3 plain = Vec3.ZERO;
		Vec3 hatted = Vec3.ZERO;
		for (int tick = 0; tick < 200; tick++) {
			plain = Broomstick.fly(plain, 0.0F, 0.0F, 1.0F, 0.0F, false, false, false);
			hatted = Broomstick.fly(hatted, 0.0F, 0.0F, 1.0F, 0.0F, false, false, true);
		}
		helper.assertTrue(plain.length() <= Broomstick.MAX_SPEED + EPSILON && plain.length() > 0.4, "Held forward, it tops out at a good speed");
		helper.assertTrue(Math.abs(hatted.length() / plain.length() - Broomstick.HAT_BONUS) < 1.0E-3, "A witch hat flies a quarter faster");

		Vec3 dry = Broomstick.fly(new Vec3(0.0, 0.3, 0.3), 0.0F, -60.0F, 1.0F, 0.0F, true, true, false);
		helper.assertTrue(dry.y < 0 && dry.y >= -Broomstick.SINK - EPSILON && dry.z < 0.3, "Dry, it can't climb or speed up, and sinks gently");
		for (int tick = 0; tick < 100; tick++) {
			dry = Broomstick.fly(dry, 0.0F, 0.0F, 1.0F, 0.0F, false, true, false);
		}
		helper.assertTrue(Math.abs(dry.y + Broomstick.SINK) < 1.0E-6, "It sinks no faster than it may");

		Vec3 from = new Vec3(0.0, 64.0, 0.0);
		double most = Broomstick.MAX_SPEED * Broomstick.CHECK_TICKS * Broomstick.TOLERANCE;
		helper.assertTrue(Broomstick.withinReason(from, from.add(most - 1.0, 0.0, 0.0), false, false), "A broom may fly its top speed");
		helper.assertTrue(!Broomstick.withinReason(from, from.add(most + 1.0, 0.0, 0.0), false, false), "but no faster");
		helper.assertTrue(Broomstick.withinReason(from, from.add(most + 1.0, 0.0, 0.0), false, true), "unless its rider wears a witch hat");
		helper.assertTrue(!Broomstick.withinReason(from, from.add(0.0, Broomstick.DRY_CLIMB + 1.0, 0.0), true, false), "A dry broom can't climb");
		helper.succeed();
	}

	/**
	 * The item, used, lays the broom out where the player stands and seats them on it, earning Up and Away; a dry broom
	 * is only laid out; a player already riding can't use one.
	 */
	@GameTest
	public void theItemSeatsItsRider(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer witch = player(helper, new BlockPos(2, 2, 2));
		witch.setItemInHand(InteractionHand.MAIN_HAND, broomItem(Broomstick.CHARGE_PER_OINTMENT));
		witch.getMainHandItem().use(level, witch, InteractionHand.MAIN_HAND);
		helper.assertTrue(witch.getVehicle() instanceof Broomstick broom && broom.charge() == Broomstick.CHARGE_PER_OINTMENT,
				"The witch is away on the broom, with its charge");
		helper.assertTrue(witch.getMainHandItem().isEmpty(), "The item is used");
		helper.assertTrue(earned(witch, "up_and_away"), "Up and Away is earned");
		witch.setItemInHand(InteractionHand.MAIN_HAND, broomItem(Broomstick.CHARGE_PER_OINTMENT));
		helper.assertTrue(witch.getMainHandItem().use(level, witch, InteractionHand.MAIN_HAND) == InteractionResult.FAIL,
				"A rider can't lay out another");

		ServerPlayer walker = player(helper, new BlockPos(5, 2, 5));
		walker.setItemInHand(InteractionHand.MAIN_HAND, broomItem(0));
		walker.getMainHandItem().use(level, walker, InteractionHand.MAIN_HAND);
		List<Broomstick> laid = level.getEntitiesOfClass(Broomstick.class, new AABB(helper.absolutePos(new BlockPos(5, 2, 5))).inflate(1.0));
		helper.assertTrue(laid.size() == 1 && laid.get(0).dry() && !walker.isPassenger(), "A dry broom is only laid out");
		helper.succeed();
	}

	/**
	 * Flying Ointment anoints a broom (its bottle comes back), up to its limit and no further; sneak-use takes it back
	 * as an item with its charge; a blow from a player breaks a riderless broom into its item, and doesn't break one
	 * being ridden.
	 */
	@GameTest
	public void anointingAndTakingItBack(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		Broomstick broom = broomAt(helper, new Vec3(2.5, 2.0, 2.5), 0);
		ServerPlayer witch = player(helper, new BlockPos(2, 2, 4));
		witch.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item("flying_ointment"), 4));
		for (int n = 1; n * Broomstick.CHARGE_PER_OINTMENT <= Broomstick.MAX_CHARGE; n++) {
			Broomstick.use(witch, level, InteractionHand.MAIN_HAND, broom);
			helper.assertTrue(broom.charge() == n * Broomstick.CHARGE_PER_OINTMENT, "Each ointment adds its charge");
		}
		helper.assertTrue(Broomstick.use(witch, level, InteractionHand.MAIN_HAND, broom) == InteractionResult.FAIL
				&& broom.charge() == Broomstick.MAX_CHARGE && witch.getMainHandItem().getCount() == 1, "A full broom takes no more");
		helper.assertTrue(carried(witch, Items.GLASS_BOTTLE) == 3, "Each ointment's bottle comes back");

		witch.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
		witch.setShiftKeyDown(true);
		Broomstick.use(witch, level, InteractionHand.MAIN_HAND, broom);
		helper.assertTrue(broom.isRemoved(), "Sneak-use takes the broom back");
		ItemStack taken = ItemStack.EMPTY;
		for (int slot = 0; slot < witch.getInventory().getContainerSize(); slot++) {
			if (witch.getInventory().getItem(slot).is(item(Broomstick.ITEM))) {
				taken = witch.getInventory().getItem(slot);
			}
		}
		helper.assertTrue(Broomstick.charge(taken) == Broomstick.MAX_CHARGE, "with its charge");

		Broomstick ridden = broomAt(helper, new Vec3(5.5, 2.0, 5.5), 600);
		ServerPlayer rider = player(helper, new BlockPos(5, 2, 5));
		rider.startRiding(ridden);
		helper.assertTrue(!ridden.hurtServer(level, level.damageSources().playerAttack(witch), 1.0F) && !ridden.isRemoved(), "A ridden broom isn't broken");
		Broomstick loose = broomAt(helper, new Vec3(2.5, 2.0, 6.5), 600);
		helper.assertTrue(loose.hurtServer(level, level.damageSources().playerAttack(witch), 1.0F) && loose.isRemoved(), "A blow breaks a riderless one");
		helper.succeedWhen(() -> {
			List<ItemEntity> dropped = level.getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(new BlockPos(2, 2, 6))).inflate(2.0),
					entity -> entity.getItem().is(item(Broomstick.ITEM)));
			helper.assertTrue(dropped.size() == 1 && Broomstick.charge(dropped.get(0).getItem()) == 600, "It drops as its item, keeping its charge");
		});
	}

	/**
	 * Ridden in the air, the broom burns a tick of charge a tick on the server until it is dry; a rider in a witch hat
	 * rides it as any other. A broom that climbs while dry (as only a client ignoring the server could make it) throws
	 * its rider off at the next check, slow falling.
	 */
	@GameTest(maxTicks = 120)
	public void theServerBurnsChargeAndChecksTheFlight(GameTestHelper helper) {
		int charge = Broomstick.CHECK_TICKS + 5;
		Broomstick broom = broomAt(helper, new Vec3(3.5, 5.0, 3.5), charge);
		ServerPlayer rider = player(helper, new BlockPos(3, 5, 3));
		rider.setItemSlot(EquipmentSlot.HEAD, new ItemStack(item("witch_hat")));
		rider.startRiding(broom);
		helper.assertTrue(Broomstick.wearsHat(rider) && rider.getVehicle() == broom, "The rider, in a witch hat, is on the broom");
		helper.runAfterDelay(Broomstick.CHECK_TICKS - 5, () -> helper.assertTrue(broom.charge() < charge && broom.charge() > 0,
				"In the air it burns its charge"));
		helper.runAfterDelay(2 * Broomstick.CHECK_TICKS + 5, () -> {
			helper.assertTrue(broom.dry() && rider.getVehicle() == broom, "Dry, it keeps its rider while it doesn't climb");
			broom.setPos(broom.position().add(0.0, Broomstick.DRY_CLIMB + 2.0, 0.0));
		});
		helper.runAfterDelay(3 * Broomstick.CHECK_TICKS + 10, () -> {
			helper.assertTrue(!rider.isPassenger(), "Climbing while dry, it throws its rider off");
			helper.assertTrue(rider.hasEffect(MobEffects.SLOW_FALLING), "who falls slowly");
			helper.succeed();
		});
	}

	/** The broom's recipe, its advancements and its entity load. */
	@GameTest
	public void broomDataLoads(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		helper.assertTrue(level.recipeAccess().byKey(ResourceKey.create(Registries.RECIPE, Jugcraft.id(Broomstick.ITEM))).isPresent(), "The broom's recipe loads");
		for (String id : List.of("up_and_away", "over_the_moon")) {
			helper.assertTrue(level.getServer().getAdvancements().get(Jugcraft.id(id)) != null, id + " loads");
		}
		helper.assertTrue(BuiltInRegistries.ENTITY_TYPE.containsKey(Jugcraft.id(Broomstick.ITEM)), "The broom's entity is registered");
		helper.assertTrue(Broomstick.charge(new ItemStack(item(Broomstick.ITEM))) == Broomstick.CHARGE_PER_OINTMENT, "A new broom holds one ointment");
		helper.succeed();
	}
}
