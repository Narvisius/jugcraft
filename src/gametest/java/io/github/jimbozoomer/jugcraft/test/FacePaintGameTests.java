package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.agriculture.FacePaint;
import io.github.jimbozoomer.jugcraft.agriculture.FacePaintKitItem;
import io.github.jimbozoomer.jugcraft.agriculture.HalloweenSeason;
import io.github.jimbozoomer.jugcraft.agriculture.JugcraftAgriculture;
import io.github.jimbozoomer.jugcraft.agriculture.TrickOrTreat;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

/**
 * In-game tests for face paint: a Face Paint Kit paints its design on a friend at once, turns its dial when used
 * sneaking, paints its holder's own face after a held use, wears by one use a face and breaks after
 * {@value FacePaintKitItem#USES}, and paints only players; the paint stays on a dry face and washes off with the head
 * under water, and is saved; a painted face, bare-headed, is a costume for trick-or-treating.
 */
public class FacePaintGameTests {
	private static void floor(GameTestHelper helper) {
		for (int x = 0; x <= 7; x++) {
			for (int z = 0; z <= 7; z++) {
				helper.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
			}
		}
	}

	private static ServerPlayer player(GameTestHelper helper, BlockPos standAt, ItemStack held) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setGameMode(GameType.SURVIVAL);
		BlockPos absolute = helper.absolutePos(standAt);
		player.setPos(absolute.getX() + 0.5, absolute.getY(), absolute.getZ() + 0.5);
		player.setItemInHand(InteractionHand.MAIN_HAND, held);
		return player;
	}

	private static ItemStack kit() {
		return new ItemStack(JugcraftAgriculture.item("face_paint_kit"));
	}

	private static boolean earned(ServerPlayer player, String id) {
		AdvancementHolder advancement = player.level().getServer().getAdvancements().get(Jugcraft.id(id));
		return advancement != null && player.getAdvancements().getOrStartProgress(advancement).isDone();
	}

	/**
	 * Used on a friend, the kit paints its design (a skull, to start) at once, wears by one use and earns Face Painter;
	 * used sneaking it turns its dial to the next design, which then paints over the first; held to the end of its use
	 * it paints its holder's own face. It paints no zombie, and breaks after its last face.
	 */
	@GameTest(maxTicks = 20)
	public void aKitPaintsFaces(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		floor(helper);
		ServerPlayer painter = player(helper, new BlockPos(2, 2, 3), kit());
		ServerPlayer friend = player(helper, new BlockPos(4, 2, 3), ItemStack.EMPTY);
		helper.assertTrue(!FacePaint.painted(friend) && !FacePaint.inCostume(friend), "A bare face is no costume");
		InteractionResult painted = painter.interactOn(friend, InteractionHand.MAIN_HAND, friend.position());
		ItemStack kit = painter.getMainHandItem();
		helper.assertTrue(painted.consumesAction() && FacePaint.design(friend) == FacePaint.Design.SKULL && kit.getDamageValue() == 1,
				"Used on a friend, it paints a skull at once, wearing the kit by one: " + painted);
		helper.assertTrue(earned(painter, "face_painter"), "and earns Face Painter");

		painter.setShiftKeyDown(true);
		painter.gameMode.useItem(painter, level, kit, InteractionHand.MAIN_HAND);
		painter.setShiftKeyDown(false);
		helper.assertTrue(FacePaintKitItem.design(kit) == FacePaint.Design.PUMPKIN && !FacePaint.painted(painter),
				"Used sneaking, it turns its dial to a jack o'lantern and paints nothing");
		painter.interactOn(friend, InteractionHand.MAIN_HAND, friend.position());
		helper.assertTrue(FacePaint.design(friend) == FacePaint.Design.PUMPKIN && kit.getDamageValue() == 2, "which paints over the skull");

		helper.assertTrue(kit.getUseDuration(painter) == FacePaintKitItem.USE_TICKS && kit.getUseAnimation() == ItemUseAnimation.BRUSH,
				"Painting your own face takes " + FacePaintKitItem.USE_TICKS + " ticks, brushing");
		kit.finishUsingItem(level, painter);
		helper.assertTrue(FacePaint.design(painter) == FacePaint.Design.PUMPKIN && kit.getDamageValue() == 3, "and at the end paints it");

		LivingEntity zombie = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, new BlockPos(6, 2, 6));
		helper.assertTrue(kit.interactLivingEntity(painter, zombie, InteractionHand.MAIN_HAND) == InteractionResult.PASS, "It paints no zombie");
		zombie.discard();

		kit.setDamageValue(FacePaintKitItem.USES - 1);
		painter.interactOn(friend, InteractionHand.MAIN_HAND, friend.position());
		helper.assertTrue(painter.getMainHandItem().isEmpty(), "After its last face, the kit is used up");
		helper.assertTrue(level.recipeAccess().byKey(ResourceKey.create(Registries.RECIPE, Jugcraft.id("face_paint_kit"))).isPresent(),
				"The kit's recipe loads");
		helper.succeed();
	}

	/** Paint stays on a dry face and washes off with the head under water; the attachment is saved. */
	@GameTest(maxTicks = 20)
	public void paintWashesOffUnderWater(GameTestHelper helper) {
		floor(helper);
		ServerPlayer player = player(helper, new BlockPos(3, 2, 3), ItemStack.EMPTY);
		FacePaint.paint(player, FacePaint.Design.WITCH);
		helper.assertTrue(!FacePaint.washIfUnderWater(player) && FacePaint.design(player) == FacePaint.Design.WITCH, "On a dry face the paint stays");
		helper.setBlock(new BlockPos(3, 2, 3), Blocks.WATER);
		helper.setBlock(new BlockPos(3, 3, 3), Blocks.WATER);
		helper.assertTrue(FacePaint.headUnderWater(player), "Standing in water two deep, the head is under");
		helper.assertTrue(FacePaint.washIfUnderWater(player) && !FacePaint.painted(player), "and the paint washes off");
		helper.assertTrue(FacePaint.PAINT.isPersistent(), "Paint is saved with the player");
		FacePaint.Design back = FacePaint.Design.CODEC.parse(NbtOps.INSTANCE,
				FacePaint.Design.CODEC.encodeStart(NbtOps.INSTANCE, FacePaint.Design.VAMPIRE).getOrThrow()).getOrThrow();
		helper.assertTrue(back == FacePaint.Design.VAMPIRE, "and a design saved reads back the same");
		helper.succeed();
	}

	/** Bare-headed, a painted face passes trick-or-treating's costume rule (the next rule, a porch light, turns it away). */
	@GameTest(maxTicks = 20)
	public void aPaintedFaceIsACostume(GameTestHelper helper) {
		floor(helper);
		BlockPos door = new BlockPos(3, 2, 3);
		helper.setBlock(door, Blocks.OAK_DOOR.defaultBlockState().setValue(DoorBlock.FACING, Direction.SOUTH).setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER));
		helper.setBlock(door.above(), Blocks.OAK_DOOR.defaultBlockState().setValue(DoorBlock.FACING, Direction.SOUTH).setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
		ServerPlayer player = player(helper, door.south(), ItemStack.EMPTY);
		long night = TrickOrTreat.DUSK + 1000;
		try {
			HalloweenSeason.setMode(HalloweenSeason.Mode.ON);
			helper.assertTrue(player.getItemBySlot(EquipmentSlot.HEAD).isEmpty()
					&& TrickOrTreat.answer(player, helper.absolutePos(door), night) == TrickOrTreat.Result.NO_COSTUME, "Bare-headed and unpainted: no costume");
			FacePaint.paint(player, FacePaint.Design.BLACK_CAT);
			TrickOrTreat.Result painted = TrickOrTreat.answer(player, helper.absolutePos(door), night);
			helper.assertTrue(FacePaint.inCostume(player) && painted == TrickOrTreat.Result.NO_PORCH_LIGHT,
					"Painted, the costume rule passes, and only the missing porch light turns them away: " + painted);
		} finally {
			HalloweenSeason.reset();
		}
		helper.succeed();
	}
}
