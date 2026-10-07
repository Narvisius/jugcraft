package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.agriculture.CarvedPumpkinBlock;
import io.github.jimbozoomer.jugcraft.agriculture.CarvedPumpkinBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.CarvingTemplates;
import io.github.jimbozoomer.jugcraft.agriculture.JugcraftAgriculture;
import io.github.jimbozoomer.jugcraft.agriculture.Pumpkling;
import io.github.jimbozoomer.jugcraft.agriculture.PumpkinCarving;
import io.github.jimbozoomer.jugcraft.agriculture.ScarecrowBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.Scarecrows;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * In-game tests for the Pumpkling (fall addition 25): a wisp wakes a hand-carved pumpkin with a face into a Pumpkling
 * wearing that face, owned by its waker (Little Jack), but not a blank pumpkin, nor in adventure mode; its owner (only)
 * has it sit and stand, and it comes to them from afar; it guards crops from crows as far as a scarecrow with its head;
 * a torch lights it and an empty hand takes the torch back, anyone's treats heal it, its owner's blows don't hurt it,
 * and a glass bottle settles it back into its pumpkin; slain, it drops its pumpkin, face and all; and the data loads.
 */
public class PumpklingGameTests {
	private static Item item(String id) {
		return JugcraftAgriculture.item(id);
	}

	private static Block carvedPumpkin() {
		return JugcraftAgriculture.block("hand_carved_pumpkin");
	}

	/** A face from the first carving stencil. */
	private static PumpkinCarving face() {
		return PumpkinCarving.BLANK.withFace(0, CarvingTemplates.ALL.get(0).face());
	}

	private static void floor(GameTestHelper helper) {
		for (int x = 0; x <= 7; x++) {
			for (int z = 0; z <= 7; z++) {
				helper.setBlock(new BlockPos(x, 1, z), Blocks.GRASS_BLOCK);
			}
		}
	}

	private static ServerPlayer player(GameTestHelper helper, BlockPos standAt, GameType mode) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setGameMode(mode);
		BlockPos absolute = helper.absolutePos(standAt);
		player.setPos(absolute.getX() + 0.5, absolute.getY(), absolute.getZ() + 0.5);
		return player;
	}

	private static boolean earned(ServerPlayer player, String id) {
		AdvancementHolder advancement = player.level().getServer().getAdvancements().get(Jugcraft.id(id));
		return advancement != null && player.getAdvancements().getOrStartProgress(advancement).isDone();
	}

	/** A hand-carved pumpkin facing south with {@code carving} cut in it, lit or not. */
	private static void carved(GameTestHelper helper, BlockPos at, PumpkinCarving carving, boolean lit) {
		helper.setBlock(at, carvedPumpkin().defaultBlockState().setValue(CarvedPumpkinBlock.FACING, Direction.SOUTH)
				.setValue(CarvedPumpkinBlock.GLOW, carving.glow()).setValue(CarvedPumpkinBlock.LIT, lit));
		if (helper.getLevel().getBlockEntity(helper.absolutePos(at)) instanceof CarvedPumpkinBlockEntity pumpkin) {
			pumpkin.setCarving(carving, null);
		}
	}

	/** {@code player} uses what they hold on the block at {@code at}, as a right click does. */
	private static void use(GameTestHelper helper, ServerPlayer player, BlockPos at) {
		BlockPos absolute = helper.absolutePos(at);
		BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(absolute), Direction.NORTH, absolute, false);
		player.gameMode.useItemOn(player, helper.getLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND, hit);
	}

	/** A Pumpkling owned by {@code owner}, wearing the stencil's face, lit or not. */
	private static Pumpkling pumpkling(GameTestHelper helper, BlockPos at, ServerPlayer owner, boolean lit) {
		Pumpkling pumpkling = helper.spawn(JugcraftAgriculture.PUMPKLING, at);
		pumpkling.setOwner(owner.getUUID());
		BlockState state = carvedPumpkin().defaultBlockState().setValue(CarvedPumpkinBlock.LIT, lit);
		pumpkling.setHead(Pumpkling.headOf(state, face()));
		return pumpkling;
	}

	private static InteractionResult interact(ServerPlayer player, Pumpkling pumpkling, boolean sneaking) {
		player.setShiftKeyDown(sneaking);
		InteractionResult result = player.interactOn(pumpkling, InteractionHand.MAIN_HAND, pumpkling.position());
		player.setShiftKeyDown(false);
		return result;
	}

	/**
	 * A Wisp in a Jar used on a lit hand-carved pumpkin with a face wakes it: the block is gone and a Pumpkling stands
	 * there, owned by its waker, wearing the same face, lit; the jar is a glass bottle again; Little Jack is earned.
	 * Ectoplasm does nothing to a blank pumpkin, nor for a player in adventure mode.
	 */
	@GameTest
	public void aSparkWakesACarvedPumpkin(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		floor(helper);
		carved(helper, new BlockPos(3, 2, 3), face(), true);
		ServerPlayer waker = player(helper, new BlockPos(3, 2, 1), GameType.SURVIVAL);
		waker.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item("wisp_in_a_jar")));
		use(helper, waker, new BlockPos(3, 2, 3));
		helper.assertBlockPresent(Blocks.AIR, new BlockPos(3, 2, 3));
		List<Pumpkling> woken = level.getEntitiesOfClass(Pumpkling.class, new AABB(helper.absolutePos(new BlockPos(3, 2, 3))).inflate(1.5));
		helper.assertTrue(woken.size() == 1, "One Pumpkling wakes, not " + woken.size());
		Pumpkling pumpkling = woken.get(0);
		helper.assertTrue(pumpkling.ownedBy(waker), "It belongs to its waker");
		helper.assertTrue(pumpkling.head().is(carvedPumpkin().asItem()) && pumpkling.head().getOrDefault(JugcraftAgriculture.CARVING, PumpkinCarving.BLANK).equals(face())
				&& pumpkling.lit(), "It wears the pumpkin's face, lit as it was");
		helper.assertTrue(waker.getMainHandItem().is(Items.GLASS_BOTTLE), "The jar is a glass bottle again");
		helper.assertTrue(earned(waker, "little_jack"), "Little Jack is earned");

		carved(helper, new BlockPos(6, 2, 3), PumpkinCarving.BLANK, false);
		waker.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item("ectoplasm")));
		use(helper, waker, new BlockPos(6, 2, 3));
		helper.assertBlockPresent(carvedPumpkin(), new BlockPos(6, 2, 3));
		helper.assertTrue(waker.getMainHandItem().is(item("ectoplasm")), "A blank pumpkin doesn't wake");

		carved(helper, new BlockPos(1, 2, 6), face(), false);
		ServerPlayer visitor = player(helper, new BlockPos(1, 2, 4), GameType.ADVENTURE);
		visitor.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item("ectoplasm")));
		use(helper, visitor, new BlockPos(1, 2, 6));
		helper.assertBlockPresent(carvedPumpkin(), new BlockPos(1, 2, 6));
		helper.assertTrue(visitor.getMainHandItem().is(item("ectoplasm")), "Nor does one for a player in adventure mode");
		helper.succeed();
	}

	/** Its owner, with an empty hand, has it sit and get up; a stranger can't. It comes to its owner from afar. */
	@GameTest
	public void itSitsAndComesToItsOwner(GameTestHelper helper) {
		floor(helper);
		// Both within reach (3 blocks) of it.
		ServerPlayer owner = player(helper, new BlockPos(2, 2, 1), GameType.SURVIVAL);
		ServerPlayer stranger = player(helper, new BlockPos(1, 2, 3), GameType.SURVIVAL);
		Pumpkling pumpkling = pumpkling(helper, new BlockPos(1, 2, 1), owner, false);
		// Test players start out holding the Creative Tower Guide: empty their hands.
		owner.getInventory().clearContent();
		stranger.getInventory().clearContent();
		InteractionResult first = interact(owner, pumpkling, false);
		boolean sat = pumpkling.sitting();
		InteractionResult strangers = interact(stranger, pumpkling, false);
		boolean stillSat = pumpkling.sitting();
		InteractionResult second = interact(owner, pumpkling, false);
		boolean stood = !pumpkling.sitting();
		String report = "owner " + first + " -> " + sat + "; stranger " + strangers + " -> " + stillSat + "; owner " + second + " -> " + !stood
				+ " (alive " + pumpkling.isAlive() + ", owned by the owner " + pumpkling.ownedBy(owner) + ", by the stranger " + pumpkling.ownedBy(stranger)
				+ ", the owner's hand " + owner.getMainHandItem() + ", eyes " + owner.getEyePosition().distanceTo(pumpkling.position()) + " off)";
		helper.assertTrue(sat && stillSat && stood, "Its owner has it sit and get up; a stranger can't: " + report);
		BlockPos far = helper.absolutePos(new BlockPos(6, 2, 6));
		owner.setPos(far.getX() + 0.5, far.getY(), far.getZ() + 0.5);
		helper.assertTrue(pumpkling.comeToOwner(), "It finds a spot by its owner");
		double away = pumpkling.distanceTo(owner);
		helper.assertTrue(away <= 4.5, "and comes to them: " + away + " blocks off");
		helper.succeed();
	}

	/** Unlit, it guards crops as far as a scarecrow with a carved head; lit, as one with a lit head. */
	@GameTest
	public void itGuardsCropsFromCrows(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		floor(helper);
		ServerPlayer owner = player(helper, new BlockPos(7, 2, 7), GameType.SURVIVAL);
		Pumpkling pumpkling = pumpkling(helper, new BlockPos(1, 2, 1), owner, false);
		helper.assertTrue(pumpkling.guardRadius() == Scarecrows.HEADED, "Unlit, it guards " + Scarecrows.HEADED + " blocks");
		helper.assertTrue(Scarecrows.guarded(level, helper.absolutePos(new BlockPos(6, 2, 5))), "A crop six blocks off is guarded");
		pumpkling.setLit(true, false);
		helper.assertTrue(pumpkling.guardRadius() == Scarecrows.LIT, "Lit, it guards " + Scarecrows.LIT + " blocks");
		helper.succeed();
	}

	/**
	 * Only its owner lights it: a torch lights it (used up), another torch takes the torch back out, a soul torch lights it
	 * blue. Anyone's treat heals it. Its owner's blows don't hurt it. Sneaking with a glass bottle, its owner settles it
	 * back into its pumpkin, which drops with its face, and the bottle fills with its spark.
	 */
	@GameTest
	public void lightingTreatsAndSettling(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		floor(helper);
		ServerPlayer owner = player(helper, new BlockPos(2, 2, 2), GameType.SURVIVAL);
		ServerPlayer stranger = player(helper, new BlockPos(6, 2, 2), GameType.SURVIVAL);
		Pumpkling pumpkling = pumpkling(helper, new BlockPos(4, 2, 4), owner, false);

		stranger.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.TORCH));
		interact(stranger, pumpkling, false);
		helper.assertTrue(!pumpkling.lit(), "A stranger can't light it");
		owner.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.TORCH));
		interact(owner, pumpkling, false);
		helper.assertTrue(pumpkling.lit() && owner.getMainHandItem().isEmpty(), "Its owner's torch lights it");
		owner.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.TORCH));
		interact(owner, pumpkling, false);
		helper.assertTrue(!pumpkling.lit() && owner.getInventory().countItem(Items.TORCH) == 2, "A torch takes its torch back");
		owner.getInventory().clearContent();
		interact(owner, pumpkling, false);
		helper.assertTrue(!pumpkling.lit() && pumpkling.sitting(), "An empty hand has it sit instead");
		interact(owner, pumpkling, false);
		owner.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.SOUL_TORCH));
		interact(owner, pumpkling, false);
		helper.assertTrue(pumpkling.lit() && ScarecrowBlockEntity.soul(pumpkling.head()), "A soul torch lights it blue");

		pumpkling.setHealth(6.0F);
		stranger.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.PUMPKIN_SEEDS, 2));
		interact(stranger, pumpkling, false);
		helper.assertTrue(pumpkling.getHealth() == 6.0F + Pumpkling.TREAT_HEAL && stranger.getMainHandItem().getCount() == 1,
				"Anyone's pumpkin seeds heal it: " + pumpkling.getHealth());
		helper.assertTrue(!pumpkling.hurtServer(level, level.damageSources().playerAttack(owner), 4.0F) && pumpkling.getHealth() == 6.0F + Pumpkling.TREAT_HEAL,
				"Its owner's blows don't hurt it");

		owner.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.GLASS_BOTTLE));
		interact(owner, pumpkling, true);
		helper.assertTrue(pumpkling.isRemoved(), "A glass bottle settles it");
		helper.assertTrue(owner.getMainHandItem().is(item("ectoplasm")), "and fills with its spark");
		helper.assertTrue(droppedPumpkin(helper, new BlockPos(4, 2, 4)), "Its pumpkin drops, face and all");
		helper.succeed();
	}

	/** Slain by anyone but its owner, it drops its carved pumpkin with its face. */
	@GameTest
	public void slainItDropsItsPumpkin(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		floor(helper);
		ServerPlayer owner = player(helper, new BlockPos(1, 2, 1), GameType.SURVIVAL);
		ServerPlayer stranger = player(helper, new BlockPos(6, 2, 6), GameType.SURVIVAL);
		Pumpkling pumpkling = pumpkling(helper, new BlockPos(4, 2, 4), owner, true);
		pumpkling.hurtServer(level, level.damageSources().playerAttack(stranger), 100.0F);
		helper.assertTrue(pumpkling.isDeadOrDying(), "A stranger's blow slays it");
		helper.assertTrue(droppedPumpkin(helper, new BlockPos(4, 2, 4)), "It drops its pumpkin, face and all");
		helper.succeed();
	}

	private static boolean droppedPumpkin(GameTestHelper helper, BlockPos near) {
		return !helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(near)).inflate(2.0),
				drop -> drop.getItem().is(carvedPumpkin().asItem()) && drop.getItem().getOrDefault(JugcraftAgriculture.CARVING, PumpkinCarving.BLANK).equals(face()))
				.isEmpty();
	}

	/** The advancement loads; a wisp and ectoplasm are sparks; pumpkin seeds and pie are treats. */
	@GameTest
	public void pumpklingDataLoads(GameTestHelper helper) {
		helper.assertTrue(helper.getLevel().getServer().getAdvancements().get(Jugcraft.id("little_jack")) != null, "Little Jack loads");
		helper.assertTrue(new ItemStack(item("wisp_in_a_jar")).is(Pumpkling.SPARKS) && new ItemStack(item("ectoplasm")).is(Pumpkling.SPARKS),
				"A wisp and ectoplasm are sparks");
		helper.assertTrue(new ItemStack(Items.PUMPKIN_SEEDS).is(Pumpkling.TREATS) && new ItemStack(Items.PUMPKIN_PIE).is(Pumpkling.TREATS),
				"Pumpkin seeds and pie are treats");
		helper.succeed();
	}
}
