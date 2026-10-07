package io.github.jimbozoomer.jugcraft.test;

import com.mojang.serialization.JsonOps;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.agriculture.CarvedPumpkinBlock;
import io.github.jimbozoomer.jugcraft.agriculture.CarvedPumpkinBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.CarvingTemplates;
import io.github.jimbozoomer.jugcraft.agriculture.JugcraftAgriculture;
import io.github.jimbozoomer.jugcraft.agriculture.PumpkinCarving;
import io.github.jimbozoomer.jugcraft.agriculture.PumpkinCarvings;
import io.github.jimbozoomer.jugcraft.agriculture.PumpkinCarvings.Result;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** In-game tests for pumpkin carving: the Carving Knife, the server's checks, light, and the carved item. */
public class CarvingGameTests {
	private static final BlockPos PUMPKIN = new BlockPos(2, 1, 2);
	private static final int[] CLASSIC = CarvingTemplates.ALL.get(0).face();
	private static final int[] CAT = CarvingTemplates.ALL.get(1).face();

	private static Item item(String id) {
		return JugcraftAgriculture.item(id);
	}

	private static Block block(String id) {
		return JugcraftAgriculture.block(id);
	}

	/** A survival player standing just north of the pumpkin, holding {@code held}. */
	private static ServerPlayer carver(GameTestHelper helper, ItemStack held) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setGameMode(GameType.SURVIVAL);
		BlockPos pumpkin = helper.absolutePos(PUMPKIN);
		player.setPos(pumpkin.getX() + 0.5, pumpkin.getY(), pumpkin.getZ() - 1.5);
		player.setItemInHand(InteractionHand.MAIN_HAND, held);
		return player;
	}

	private static ItemStack knife() {
		return new ItemStack(item("carving_knife"));
	}

	private static Result carve(GameTestHelper helper, ServerPlayer player, Direction side, int[] face) {
		BlockPos pumpkin = helper.absolutePos(PUMPKIN);
		PumpkinCarvings.startSession(player, pumpkin, side);
		return PumpkinCarvings.carve(player, pumpkin, side, face);
	}

	private static BlockHitResult hit(GameTestHelper helper, BlockPos pos, Direction side) {
		BlockPos absolute = helper.absolutePos(pos);
		return new BlockHitResult(Vec3.atCenterOf(absolute).relative(side, 0.5), side, absolute, false);
	}

	private static int itemsAround(GameTestHelper helper, Item item) {
		int count = 0;
		for (ItemEntity entity : helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(PUMPKIN)).inflate(3.0))) {
			if (entity.getItem().is(item)) {
				count += entity.getItem().getCount();
			}
		}
		return count;
	}

	/** One pixel changed from a face. */
	private static int[] with(int[] face, int x, int y, int depth) {
		int[] copy = face.clone();
		copy[y] = PumpkinCarving.withPixel(copy[y], x, depth);
		return copy;
	}

	/**
	 * The first carve turns a plain pumpkin into a hand-carved one facing the carved side, keeps the face,
	 * records the carver, lets the seeds out (vanilla's carving loot) and costs the knife one use.
	 */
	@GameTest
	public void knifeCarvesAPumpkin(GameTestHelper helper) {
		helper.setBlock(PUMPKIN, Blocks.PUMPKIN);
		ServerPlayer player = carver(helper, knife());
		Result result = carve(helper, player, Direction.NORTH, CLASSIC);
		helper.assertTrue(result == Result.CARVED, "Carving should succeed, got " + result);
		BlockState state = helper.getBlockState(PUMPKIN);
		helper.assertTrue(state.is(block("hand_carved_pumpkin")) && state.getValue(CarvedPumpkinBlock.FACING) == Direction.NORTH,
				"The pumpkin should now be hand-carved, facing north: " + state);
		CarvedPumpkinBlockEntity pumpkin = helper.getBlockEntity(PUMPKIN, CarvedPumpkinBlockEntity.class);
		helper.assertTrue(Arrays.equals(pumpkin.carving().face(0), CLASSIC), "The front should hold the carved face");
		helper.assertTrue(pumpkin.carverName().equals(player.getName().getString()), "The carver should be recorded");
		helper.assertTrue(state.getValue(CarvedPumpkinBlock.GLOW) == pumpkin.carving().glow() && pumpkin.carving().glow() > 0,
				"The glow should follow the design: " + state);
		helper.assertTrue(!state.getValue(CarvedPumpkinBlock.LIT) && state.getLightEmission() == 0, "No light without a torch inside");
		helper.assertTrue(player.getMainHandItem().getDamageValue() == 1, "Carving should cost the knife one use");
		helper.succeedWhen(() -> helper.assertTrue(itemsAround(helper, Items.PUMPKIN_SEEDS) == 4,
				"Carving should let out 4 seeds, found " + itemsAround(helper, Items.PUMPKIN_SEEDS)));
	}

	/** Using the knife on the side of a pumpkin opens a session for that side, which the carve then uses up. */
	@GameTest
	public void knifeOpensACarvingSession(GameTestHelper helper) {
		helper.setBlock(PUMPKIN, Blocks.PUMPKIN);
		ServerPlayer player = carver(helper, knife());
		helper.assertTrue(player.getMainHandItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit(helper, PUMPKIN, Direction.UP)))
				== InteractionResult.PASS, "The knife should not carve the top of a pumpkin");
		helper.assertTrue(player.getMainHandItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit(helper, PUMPKIN, Direction.NORTH)))
				.consumesAction(), "The knife should open the carving screen on the side of a pumpkin");
		BlockPos pumpkin = helper.absolutePos(PUMPKIN);
		helper.assertTrue(PumpkinCarvings.carve(player, pumpkin, Direction.NORTH, CLASSIC) == Result.CARVED, "The opened side should carve");
		helper.assertTrue(PumpkinCarvings.carve(player, pumpkin, Direction.NORTH, with(CLASSIC, 0, 0, PumpkinCarving.CUT)) == Result.NO_SESSION,
				"One use of the knife allows one carve");
		helper.succeed();
	}

	/** A knife cannot put skin back: carving only goes deeper, and deeper carving still works. */
	@GameTest
	public void carvingOnlyGoesDeeper(GameTestHelper helper) {
		helper.setBlock(PUMPKIN, Blocks.PUMPKIN);
		ServerPlayer player = carver(helper, knife());
		carve(helper, player, Direction.NORTH, CLASSIC);
		Result back = carve(helper, player, Direction.NORTH, with(CLASSIC, 3, 3, PumpkinCarving.SKIN));
		helper.assertTrue(back == Result.UNCARVING, "Filling a hole back in should be refused, got " + back);
		Result same = carve(helper, player, Direction.NORTH, CLASSIC);
		helper.assertTrue(same == Result.UNCHANGED, "The same face changes nothing, got " + same);
		Result deeper = carve(helper, player, Direction.NORTH, with(with(CLASSIC, 0, 15, PumpkinCarving.SHAVED), 15, 15, PumpkinCarving.CUT));
		helper.assertTrue(deeper == Result.CARVED, "Carving deeper should work, got " + deeper);
		helper.assertTrue(player.getMainHandItem().getDamageValue() == 2, "Two carvings cost two uses");
		helper.succeed();
	}

	/** Every check: the session, the knife, reach, permission, the block, the side and the face itself. */
	@GameTest
	public void carvingIsCheckedOnTheServer(GameTestHelper helper) {
		helper.setBlock(PUMPKIN, Blocks.PUMPKIN);
		BlockPos pumpkin = helper.absolutePos(PUMPKIN);
		ServerPlayer player = carver(helper, knife());
		helper.assertTrue(PumpkinCarvings.carve(player, pumpkin, Direction.NORTH, CLASSIC) == Result.NO_SESSION, "No session, no carving");
		PumpkinCarvings.startSession(player, pumpkin, Direction.NORTH);
		helper.assertTrue(PumpkinCarvings.carve(player, pumpkin, Direction.EAST, CLASSIC) == Result.NO_SESSION, "The session is for one side");
		PumpkinCarvings.startSession(player, pumpkin, Direction.NORTH);
		helper.assertTrue(PumpkinCarvings.carve(player, pumpkin.above(), Direction.NORTH, CLASSIC) == Result.NO_SESSION, "The session is for one block");

		helper.assertTrue(carve(helper, player, Direction.NORTH, new int[] {3, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}) == Result.INVALID,
				"The fourth 2-bit value means nothing");
		helper.assertTrue(carve(helper, player, Direction.NORTH, new int[15]) == Result.INVALID, "A face has 16 rows");
		helper.assertTrue(carve(helper, player, Direction.UP, CLASSIC) == Result.NOT_A_PUMPKIN, "Tops are not carved");

		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.SHEARS));
		helper.assertTrue(carve(helper, player, Direction.NORTH, CLASSIC) == Result.NO_KNIFE, "Carving needs the knife in hand");
		player.setItemInHand(InteractionHand.OFF_HAND, knife());
		helper.assertTrue(carve(helper, player, Direction.NORTH, CLASSIC) == Result.CARVED, "A knife in the other hand also carves");

		ServerPlayer far = carver(helper, knife());
		far.setPos(pumpkin.getX() + 0.5, pumpkin.getY(), pumpkin.getZ() + 12.5);
		helper.assertTrue(carve(helper, far, Direction.NORTH, CAT) == Result.TOO_FAR, "Carving needs reach");
		ServerPlayer adventurer = carver(helper, knife());
		adventurer.setGameMode(GameType.ADVENTURE);
		helper.assertTrue(carve(helper, adventurer, Direction.EAST, CAT) == Result.NOT_ALLOWED, "Adventure mode cannot carve");

		helper.setBlock(PUMPKIN, Blocks.MELON);
		helper.assertTrue(carve(helper, player, Direction.NORTH, CLASSIC) == Result.NOT_A_PUMPKIN, "Only pumpkins are carved");
		helper.succeed();
	}

	/** Each side keeps its own face, numbered from the front clockwise. */
	@GameTest
	public void sidesAreCarvedApart(GameTestHelper helper) {
		helper.setBlock(PUMPKIN, Blocks.PUMPKIN);
		ServerPlayer player = carver(helper, knife());
		carve(helper, player, Direction.NORTH, CLASSIC);
		Result side = carve(helper, player, Direction.EAST, CAT);
		helper.assertTrue(side == Result.CARVED, "The east side should carve too, got " + side);
		PumpkinCarving carving = helper.getBlockEntity(PUMPKIN, CarvedPumpkinBlockEntity.class).carving();
		helper.assertTrue(Arrays.equals(carving.face(0), CLASSIC) && Arrays.equals(carving.face(1), CAT)
				&& carving.isBlank(2) && carving.isBlank(3), "Front classic, right side cat, the rest uncarved");
		helper.assertTrue(PumpkinCarving.faceIndex(Direction.NORTH, Direction.EAST) == 1
				&& PumpkinCarving.side(Direction.SOUTH, 1) == Direction.WEST, "Faces go clockwise from the front");
		helper.succeed();
	}

	/** A torch lights a carved pumpkin with the design's glow; an empty hand takes it back; an uncarved one stays dark. */
	@GameTest
	public void torchLightsACarvedPumpkin(GameTestHelper helper) {
		helper.setBlock(PUMPKIN, Blocks.PUMPKIN);
		ServerPlayer player = carver(helper, knife());
		carve(helper, player, Direction.NORTH, CLASSIC);
		int glow = helper.getBlockEntity(PUMPKIN, CarvedPumpkinBlockEntity.class).carving().glow();
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.TORCH, 2));
		helper.useBlock(PUMPKIN, player, hit(helper, PUMPKIN, Direction.NORTH));
		BlockState lit = helper.getBlockState(PUMPKIN);
		helper.assertTrue(lit.getValue(CarvedPumpkinBlock.LIT) && lit.getLightEmission() == glow, "A torch should light it to " + glow + ": " + lit);
		helper.assertTrue(player.getMainHandItem().getCount() == 1, "Lighting uses one torch");

		player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
		helper.useBlock(PUMPKIN, player, hit(helper, PUMPKIN, Direction.NORTH));
		helper.assertTrue(!helper.getBlockState(PUMPKIN).getValue(CarvedPumpkinBlock.LIT), "An empty hand takes the torch out");
		helper.assertTrue(player.getInventory().countItem(Items.TORCH) == 1, "The torch comes back");

		BlockPos blank = PUMPKIN.east(2);
		helper.setBlock(blank, block("hand_carved_pumpkin"));
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.TORCH));
		helper.useBlock(blank, player, hit(helper, blank, Direction.NORTH));
		helper.assertTrue(!helper.getBlockState(blank).getValue(CarvedPumpkinBlock.LIT), "Nothing carved, nothing to shine through");
		helper.succeed();
	}

	/** Broken, a lit carved pumpkin drops itself with its design and its torch; placed again, it keeps both. */
	@GameTest
	public void carvedPumpkinKeepsItsDesign(GameTestHelper helper) {
		helper.setBlock(PUMPKIN, Blocks.PUMPKIN);
		ServerPlayer player = carver(helper, knife());
		carve(helper, player, Direction.NORTH, CLASSIC);
		carve(helper, player, Direction.WEST, CAT);
		helper.setBlock(PUMPKIN, helper.getBlockState(PUMPKIN).setValue(CarvedPumpkinBlock.LIT, true));
		CarvedPumpkinBlockEntity pumpkin = helper.getBlockEntity(PUMPKIN, CarvedPumpkinBlockEntity.class);
		PumpkinCarving carving = pumpkin.carving();
		List<ItemStack> drops = Block.getDrops(helper.getBlockState(PUMPKIN), helper.getLevel(), helper.absolutePos(PUMPKIN), pumpkin);
		helper.assertTrue(drops.size() == 1 && drops.get(0).is(item("hand_carved_pumpkin")), "It should drop itself: " + drops);
		ItemStack drop = drops.get(0);
		helper.assertTrue(carving.equals(drop.get(JugcraftAgriculture.CARVING)), "The item keeps the design");
		BlockItemStateProperties state = drop.get(DataComponents.BLOCK_STATE);
		helper.assertTrue(state != null && state.apply(block("hand_carved_pumpkin").defaultBlockState()).getValue(CarvedPumpkinBlock.LIT),
				"The item keeps its torch");

		BlockPos stand = new BlockPos(5, 1, 2);
		helper.setBlock(stand, Blocks.STONE);
		player.setItemInHand(InteractionHand.MAIN_HAND, drop);
		drop.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit(helper, stand, Direction.UP)));
		BlockState placed = helper.getBlockState(stand.above());
		helper.assertTrue(placed.is(block("hand_carved_pumpkin")) && placed.getValue(CarvedPumpkinBlock.LIT)
				&& placed.getValue(CarvedPumpkinBlock.GLOW) == carving.glow(), "Placed again, it is lit with the same glow: " + placed);
		helper.assertTrue(carving.equals(helper.getBlockEntity(stand.above(), CarvedPumpkinBlockEntity.class).carving()),
				"Placed again, it has the same design");
		helper.succeed();
	}

	/** With free drawing off, only the starter faces can be carved. */
	@GameTest
	public void serversCanAllowStarterFacesOnly(GameTestHelper helper) {
		helper.setBlock(PUMPKIN, Blocks.PUMPKIN);
		ServerPlayer player = carver(helper, knife());
		PumpkinCarvings.freeDraw = false;
		try {
			Result custom = carve(helper, player, Direction.NORTH, with(new int[PumpkinCarving.SIZE], 7, 7, PumpkinCarving.CUT));
			helper.assertTrue(custom == Result.NOT_A_TEMPLATE, "A free design should be refused, got " + custom);
			Result template = carve(helper, player, Direction.NORTH, CLASSIC);
			helper.assertTrue(template == Result.CARVED, "A starter face should carve, got " + template);
		} finally {
			PumpkinCarvings.freeDraw = true;
		}
		helper.succeed();
	}

	/** The design's glow, its saved form, and the starter faces. */
	@GameTest
	public void designsGlowAndSave(GameTestHelper helper) {
		helper.assertTrue(PumpkinCarving.BLANK.glow() == 0, "Nothing carved, no glow");
		int[] face = new int[PumpkinCarving.SIZE];
		face[0] = PumpkinCarving.withPixel(0, 0, PumpkinCarving.CUT);
		helper.assertTrue(PumpkinCarving.BLANK.withFace(0, face).glow() == 4, "One hole glows 4");
		int[] holes = new int[PumpkinCarving.SIZE];
		int[] shaved = new int[PumpkinCarving.SIZE];
		for (int y = 0; y < 3; y++) {
			for (int x = 0; x < PumpkinCarving.SIZE; x++) {
				holes[y] = PumpkinCarving.withPixel(holes[y], x, PumpkinCarving.CUT);
			}
		}
		for (int y = 0; y < 6; y++) {
			for (int x = 0; x < PumpkinCarving.SIZE; x++) {
				shaved[y] = PumpkinCarving.withPixel(shaved[y], x, PumpkinCarving.SHAVED);
			}
		}
		helper.assertTrue(PumpkinCarving.BLANK.withFace(0, holes).glow() == 15, "48 holes glow 15 (4 + 16, capped)");
		helper.assertTrue(PumpkinCarving.BLANK.withFace(0, shaved).glow() == 12, "96 shaved pixels glow 4 + 8");

		PumpkinCarving design = PumpkinCarving.BLANK.withFace(0, CLASSIC).withFace(2, CAT);
		PumpkinCarving loaded = PumpkinCarving.CODEC.parse(JsonOps.INSTANCE, PumpkinCarving.CODEC.encodeStart(JsonOps.INSTANCE, design).getOrThrow())
				.getOrThrow();
		helper.assertTrue(design.equals(loaded) && design.hashCode() == loaded.hashCode(), "A design saves and loads unchanged");
		helper.assertTrue(PumpkinCarving.of(new int[] {3}).isError(), "A wrong-sized design does not load");

		Set<String> ids = new HashSet<>();
		for (CarvingTemplates.Template template : CarvingTemplates.ALL) {
			helper.assertTrue(ids.add(template.id()) && PumpkinCarving.isValidFace(template.face())
					&& !PumpkinCarving.BLANK.withFace(0, template.face()).isBlank(), "Starter face " + template.id() + " should be a real face");
		}
		helper.succeed();
	}

	/** Roasted pumpkin seeds: a small snack, roasted from pumpkin seeds in a furnace, smoker or on a campfire. */
	@GameTest
	public void pumpkinSeedsRoast(GameTestHelper helper) {
		FoodProperties food = new ItemStack(item("roasted_pumpkin_seeds")).get(DataComponents.FOOD);
		helper.assertTrue(food != null && food.nutrition() == 2, "Roasted pumpkin seeds should restore 2, have " + food);
		for (String recipe : new String[] {"roasted_pumpkin_seeds", "roasted_pumpkin_seeds_from_smoking", "roasted_pumpkin_seeds_from_campfire_cooking"}) {
			helper.assertTrue(helper.getLevel().recipeAccess().byKey(ResourceKey.create(Registries.RECIPE, Jugcraft.id(recipe))).isPresent(),
					"Recipe " + recipe + " should load");
		}
		helper.succeed();
	}
}
