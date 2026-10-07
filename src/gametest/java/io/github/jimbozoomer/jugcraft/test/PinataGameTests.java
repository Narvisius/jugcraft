package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.agriculture.JugcraftAgriculture;
import io.github.jimbozoomer.jugcraft.agriculture.Pinata;
import io.github.jimbozoomer.jugcraft.agriculture.Pinatas;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** Game tests for the piñata party (fall addition 28). Run by CI's {@code runGameTest}. */
public class PinataGameTests {
	private static Item item(String id) {
		return JugcraftAgriculture.item(id);
	}

	private static ServerPlayer player(GameTestHelper helper, BlockPos standAt) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setGameMode(GameType.SURVIVAL);
		// Test players start out holding the Creative Tower Guide.
		player.getInventory().clearContent();
		BlockPos absolute = helper.absolutePos(standAt);
		player.setPos(absolute.getX() + 0.5, absolute.getY(), absolute.getZ() + 0.5);
		return player;
	}

	private static boolean earned(ServerPlayer player, String id) {
		AdvancementHolder advancement = player.level().getServer().getAdvancements().get(Jugcraft.id(id));
		return advancement != null && player.getAdvancements().getOrStartProgress(advancement).isDone();
	}

	/** A stone floor, and a stone beam at height 5 across the middle to hang piñatas from. */
	private static void hall(GameTestHelper helper) {
		for (int x = 0; x <= 7; x++) {
			for (int z = 0; z <= 7; z++) {
				helper.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
			}
			helper.setBlock(new BlockPos(x, 5, 3), Blocks.STONE);
		}
	}

	private static Pinata hung(GameTestHelper helper, BlockPos support, Pinata.Kind kind, ServerPlayer owner) {
		Pinata pinata = Pinata.hang(helper.getLevel(), helper.absolutePos(support), kind, owner);
		helper.assertTrue(pinata != null, "A " + kind.id + " piñata hangs");
		return pinata;
	}

	private static int lying(GameTestHelper helper, Item item) {
		int count = 0;
		AABB area = new AABB(helper.absolutePos(new BlockPos(0, 0, 0))).expandTowards(8, 8, 8);
		for (ItemEntity entity : helper.getLevel().getEntitiesOfClass(ItemEntity.class, area)) {
			if (entity.getItem().is(item)) {
				count += entity.getItem().getCount();
			}
		}
		return count;
	}

	/**
	 * A piñata used on the underside of a block hangs there, its foot a block and a half below; not from a block's side,
	 * nor where there is no room below. Anyone fills it a stack at a time, up to nine; the tenth won't go in.
	 */
	@GameTest
	public void itHangsAndIsFilled(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		hall(helper);
		ServerPlayer player = player(helper, new BlockPos(3, 2, 6));
		BlockPos support = helper.absolutePos(new BlockPos(3, 5, 3));
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item("star_pinata"), 2));
		player.gameMode.useItemOn(player, level, player.getMainHandItem(), InteractionHand.MAIN_HAND,
				new BlockHitResult(Vec3.atCenterOf(support).add(0.0, 0.5, -0.5), Direction.NORTH, support, false));
		helper.assertTrue(player.getMainHandItem().getCount() == 2, "Not from a block's side");
		player.gameMode.useItemOn(player, level, player.getMainHandItem(), InteractionHand.MAIN_HAND,
				new BlockHitResult(Vec3.atCenterOf(support).add(0.0, -0.5, 0.0), Direction.DOWN, support, false));
		List<Pinata> pinatas = level.getEntitiesOfClass(Pinata.class, new AABB(support).inflate(0.5, 3.0, 0.5));
		helper.assertTrue(pinatas.size() == 1 && pinatas.get(0).kind() == Pinata.Kind.STAR && pinatas.get(0).support().equals(support),
				"Used on a block's underside, a star piñata hangs from it");
		helper.assertTrue(Math.abs(pinatas.get(0).getY() - (support.getY() - Pinata.DROP)) < 1.0E-6 && player.getMainHandItem().getCount() == 1,
				"its foot a block and a half below, and one piñata is used");
		BlockPos low = helper.absolutePos(new BlockPos(6, 5, 3));
		helper.setBlock(new BlockPos(6, 4, 3), Blocks.STONE);
		player.gameMode.useItemOn(player, level, player.getMainHandItem(), InteractionHand.MAIN_HAND,
				new BlockHitResult(Vec3.atCenterOf(low).add(0.0, -0.5, 0.0), Direction.DOWN, low, false));
		helper.assertTrue(player.getMainHandItem().getCount() == 1, "Not where there is no room below");

		Pinata pinata = pinatas.get(0);
		ServerPlayer guest = player(helper, new BlockPos(2, 2, 4));
		guest.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.COOKIE, 5));
		Pinatas.use(guest, level, InteractionHand.MAIN_HAND, pinata);
		helper.assertTrue(guest.getMainHandItem().isEmpty() && pinata.contents().size() == 1 && pinata.contents().get(0).getCount() == 5,
				"Anyone puts a whole stack in");
		for (int i = 1; i < Pinata.SLOTS; i++) {
			guest.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.SWEET_BERRIES, 3));
			Pinatas.use(guest, level, InteractionHand.MAIN_HAND, pinata);
		}
		guest.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.APPLE));
		Pinatas.use(guest, level, InteractionHand.MAIN_HAND, pinata);
		helper.assertTrue(pinata.contents().size() == Pinata.SLOTS && guest.getMainHandItem().is(Items.APPLE), "Nine stacks fill it; the tenth won't go in");
		helper.succeed();
	}

	/** Its hanger, sneaking with an empty hand, takes it down, contents and all; nobody else can. */
	@GameTest
	public void itsHangerTakesItDown(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		hall(helper);
		ServerPlayer owner = player(helper, new BlockPos(3, 2, 6));
		ServerPlayer stranger = player(helper, new BlockPos(4, 2, 6));
		Pinata pinata = hung(helper, new BlockPos(3, 5, 3), Pinata.Kind.BAT, owner);
		pinata.fill(new ItemStack(Items.COOKIE, 4));
		stranger.setShiftKeyDown(true);
		Pinatas.use(stranger, level, InteractionHand.MAIN_HAND, pinata);
		stranger.setShiftKeyDown(false);
		helper.assertTrue(!pinata.isRemoved(), "A stranger can't take it down");
		owner.setShiftKeyDown(true);
		Pinatas.use(owner, level, InteractionHand.MAIN_HAND, pinata);
		owner.setShiftKeyDown(false);
		helper.assertTrue(pinata.isRemoved() && owner.getInventory().countItem(item("bat_pinata")) == 1
				&& owner.getInventory().countItem(Items.COOKIE) == 4, "Its hanger takes it down, and its cookies come back");
		helper.succeed();
	}

	/**
	 * A weak swing only rocks it; a charged one is a hit. Half its hits tear it; its last bursts it, spraying its contents
	 * out and earning Piñata Party. With the Piñata Stick each hit counts two, and bursting one blindfolded earns Blind
	 * Luck.
	 */
	@GameTest
	public void chargedHitsBurstIt(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		hall(helper);
		ServerPlayer player = player(helper, new BlockPos(3, 2, 4));
		Pinata pinata = hung(helper, new BlockPos(3, 5, 3), Pinata.Kind.PUMPKIN, player);
		pinata.fill(new ItemStack(Items.COOKIE, 6));
		pinata.fill(new ItemStack(Items.APPLE, 2));
		float full = (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE);
		helper.assertTrue(!pinata.swingAt(level, player, full * 0.2F) && pinata.hits() == 0 && pinata.swings() == 1, "A weak swing only rocks it");
		helper.assertTrue(pinata.hurtServer(level, level.damageSources().playerAttack(player), full) && pinata.hits() == 1,
				"A player's charged swing is a hit");
		helper.assertTrue(!pinata.hurtServer(level, level.damageSources().generic(), 10.0F) && pinata.hits() == 1, "Nothing else hurts it");
		for (int i = 1; i < Pinata.Kind.PUMPKIN.hits / 2; i++) {
			pinata.swingAt(level, player, full);
		}
		helper.assertTrue(pinata.torn() && !pinata.isRemoved(), "Half its hits tear it");
		while (!pinata.isRemoved()) {
			pinata.swingAt(level, player, full);
		}
		helper.assertTrue(lying(helper, Items.COOKIE) == 6 && lying(helper, Items.APPLE) == 2, "It bursts, and its contents spray out");
		helper.assertTrue(earned(player, "pinata_party") && !earned(player, "blind_luck"), "Piñata Party, but not Blind Luck");

		Pinata bat = hung(helper, new BlockPos(5, 5, 3), Pinata.Kind.BAT, player);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item(Pinatas.STICK)));
		player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(item(Pinatas.BLINDFOLD)));
		float stick = (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE);
		int swings = 0;
		while (!bat.isRemoved() && swings < 10) {
			bat.swingAt(level, player, stick);
			swings++;
		}
		helper.assertTrue(swings == Pinata.Kind.BAT.hits / Pinata.STICK_HITS, "With the stick each hit counts two: " + swings + " swings");
		helper.assertTrue(earned(player, "blind_luck"), "Bursting it blindfolded earns Blind Luck");
		helper.succeed();
	}

	/** When what it hangs from goes, it falls: it drops itself and its contents. */
	@GameTest(maxTicks = 60)
	public void itFallsWhenItsBeamGoes(GameTestHelper helper) {
		hall(helper);
		ServerPlayer player = player(helper, new BlockPos(3, 2, 6));
		Pinata pinata = hung(helper, new BlockPos(1, 5, 3), Pinata.Kind.STAR, player);
		pinata.fill(new ItemStack(Items.COOKIE, 3));
		helper.setBlock(new BlockPos(1, 5, 3), Blocks.AIR);
		helper.runAfterDelay(25, () -> {
			helper.assertTrue(pinata.isRemoved(), "It falls");
			helper.assertTrue(lying(helper, item("star_pinata")) == 1 && lying(helper, Items.COOKIE) == 3, "and drops itself and its cookies");
			helper.succeed();
		});
	}

	/** The recipes and advancements load; the Blindfold goes on the head. */
	@GameTest
	public void pinataDataLoads(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		for (String id : List.of("pumpkin_pinata", "star_pinata", "bat_pinata", Pinatas.STICK, Pinatas.BLINDFOLD)) {
			helper.assertTrue(level.recipeAccess().byKey(ResourceKey.create(Registries.RECIPE, Jugcraft.id(id))).isPresent(), id + " has a recipe");
		}
		for (String id : List.of("pinata_party", "blind_luck")) {
			helper.assertTrue(level.getServer().getAdvancements().get(Jugcraft.id(id)) != null, id + " loads");
		}
		ItemStack blindfold = new ItemStack(item(Pinatas.BLINDFOLD));
		helper.assertTrue(blindfold.get(DataComponents.EQUIPPABLE) != null && blindfold.get(DataComponents.EQUIPPABLE).slot() == EquipmentSlot.HEAD,
				"The Blindfold goes on the head");
		helper.succeed();
	}
}
