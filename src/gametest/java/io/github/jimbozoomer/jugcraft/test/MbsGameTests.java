package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.blueprint.Blueprint;
import io.github.jimbozoomer.jugcraft.machine.EnlargedMachineBlock;
import io.github.jimbozoomer.jugcraft.machine.JugcraftMachines;
import io.github.jimbozoomer.jugcraft.machine.MachineBlock;
import io.github.jimbozoomer.jugcraft.nuclear.MultiFurnitureBlock;
import io.github.jimbozoomer.jugcraft.nuclear.PlantBuilder;
import io.github.jimbozoomer.jugcraft.nuclear.PlantInterior;
import io.github.jimbozoomer.jugcraft.nuclear.StorageTankOutletBlock;
import io.github.jimbozoomer.jugcraft.nuclear.StorageTankPlateBlock;
import io.github.jimbozoomer.jugcraft.nuclear.StorageTankPortBlockEntity;
import io.github.jimbozoomer.jugcraft.nuclear.StorageTanks;
import io.github.jimbozoomer.jugcraft.nuclear.TankKitItem;
import io.github.jimbozoomer.jugcraft.placement.StructureGhost;
import io.github.jimbozoomer.jugcraft.placement.StructureReach;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * MBS placed from one item come down whole (Justin, October 8 2026: "once you break an MBS, it just drops the entire
 * MBS for you"; he chose the kit back with its liquid lost, and hand-built structures left as they are), and show what
 * they would place before they are placed (the preview's data: TankKitItem.formed, StructureGhost).
 */
public class MbsGameTests {
	private static final String ARENA = "jugcraft-test:drone_tower";

	private static void ground(GameTestHelper helper) {
		for (int x = 0; x < 44; x++) {
			for (int z = 0; z < 44; z++) {
				helper.setBlock(new BlockPos(x, 0, z), Blocks.GRASS_BLOCK.defaultBlockState());
			}
		}
	}

	private static ServerPlayer player(GameTestHelper helper, BlockPos near, GameType mode) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setGameMode(mode);
		player.setPos(near.getX() + 0.5, near.getY(), near.getZ() + 0.5);
		player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
		return player;
	}

	private static int dropped(GameTestHelper helper, BlockPos around, double radius, Item item) {
		return helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(around).inflate(radius)).stream()
				.filter(e -> item == null || e.getItem().is(item)).mapToInt(e -> e.getItem().getCount()).sum();
	}

	private static void clearItems(GameTestHelper helper, BlockPos around) {
		helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(around).inflate(40)).forEach(ItemEntity::discard);
	}

	private static StorageTankPortBlockEntity outlet(GameTestHelper helper, Blueprint blueprint, BlockPos stake, Rotation rotation) {
		for (int i = 0; i < blueprint.size(); i++) {
			if (blueprint.state(i, rotation).getBlock() instanceof StorageTankOutletBlock) {
				return (StorageTankPortBlockEntity) helper.getLevel().getBlockEntity(stake.offset(blueprint.offset(i, rotation)));
			}
		}
		throw new AssertionError("no outlet");
	}

	/** A plate on the shell's side, at the height of the player standing by the tank. */
	private static BlockPos wallPlate(GameTestHelper helper, Blueprint blueprint, BlockPos stake, Rotation rotation) {
		for (int i = 0; i < blueprint.size(); i++) {
			if (blueprint.state(i, rotation).getBlock() instanceof StorageTankPlateBlock && blueprint.offsetY(i) == 2) {
				return stake.offset(blueprint.offset(i, rotation));
			}
		}
		throw new AssertionError("no wall plate");
	}

	@GameTest(structure = ARENA, maxTicks = 100, skyAccess = true)
	public void kitTankComesDownWhole(GameTestHelper helper) {
		if (TestFocus.skip("blueprints")) {
			helper.succeed();
			return;
		}
		ground(helper);
		String kit = "general_small";
		Blueprint blueprint = TankKitItem.blueprint(kit);
		BlockPos stake = helper.absolutePos(new BlockPos(12, 1, 14));
		helper.assertTrue(TankKitItem.place(helper.getLevel(), kit, stake, Rotation.NONE, null) == null, "the kit is placed");
		StorageTankPortBlockEntity tank = outlet(helper, blueprint, stake, Rotation.NONE);
		helper.assertTrue(tank.formed() && kit.equals(tank.kit()), "the tank forms and remembers its kit: " + tank.kit());
		try (Transaction t = Transaction.openOuter()) {
			tank.fluid.insert(FluidVariant.of(Fluids.WATER), 20 * FluidConstants.BUCKET, t);
			t.commit();
		}
		helper.assertTrue(tank.fluid.amount > 0, "water in the tank");
		BlockPos plate = wallPlate(helper, blueprint, stake, Rotation.NONE);
		clearItems(helper, stake);
		ServerPlayer player = player(helper, plate.south(2), GameType.SURVIVAL);
		player.gameMode.destroyBlock(plate);
		// Every block the kit laid is gone; the ground under the foundation is grass again.
		int left = 0, grass = 0;
		for (int i = 0; i < blueprint.size(); i++) {
			BlockPos p = stake.offset(blueprint.offset(i, Rotation.NONE));
			BlockState there = helper.getLevel().getBlockState(p);
			if (blueprint.offsetY(i) < 0) {
				grass += there.is(Blocks.GRASS_BLOCK) ? 1 : 0;
				helper.assertTrue(there.is(Blocks.GRASS_BLOCK), "the ground at " + p + " is " + there + ", not grass again");
			} else if (!there.isAir()) {
				left++;
			}
		}
		helper.assertTrue(left == 0, left + " blocks of the tank are left standing");
		helper.assertTrue(grass > 0, "the foundation was over grass");
		Item kitItem = StorageTanks.ITEMS.get("tank_kit_" + kit);
		helper.assertTrue(dropped(helper, plate, 3, kitItem) == 1, "one kit dropped: " + dropped(helper, plate, 3, kitItem));
		helper.assertTrue(dropped(helper, stake, 30, null) == 1, "nothing else dropped (no plates, no outlet with its water): "
				+ dropped(helper, stake, 30, null));
		helper.succeed();
	}

	@GameTest(structure = ARENA, maxTicks = 40, skyAccess = true)
	public void kitRefusedWhereSomethingStandsInside(GameTestHelper helper) {
		if (TestFocus.skip("blueprints")) {
			helper.succeed();
			return;
		}
		ground(helper);
		String kit = "general_medium";
		BlockPos stake = helper.absolutePos(new BlockPos(12, 1, 14));
		TankKitItem.Formed formed = TankKitItem.formed(kit, Rotation.NONE);
		helper.assertTrue(formed != null && !formed.inside().isEmpty(), "the kit has a space inside");
		// A stone where the middle of the tank's space would be: in the way, like the shell.
		BlockPos middle = stake.offset(formed.inside().stream().filter(p -> p.getY() == 2).findFirst().orElseThrow());
		helper.getLevel().setBlock(middle, Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
		helper.assertTrue(TankKitItem.blocked(helper.getLevel(), kit, stake, Rotation.NONE).contains(middle), "the stone inside is in the way");
		helper.assertTrue(TankKitItem.place(helper.getLevel(), kit, stake, Rotation.NONE, null) != null, "the kit is refused");
		helper.succeed();
	}

	@GameTest(structure = ARENA, maxTicks = 100, skyAccess = true)
	public void creativeBreaksWholeTankWithoutDrop(GameTestHelper helper) {
		if (TestFocus.skip("blueprints")) {
			helper.succeed();
			return;
		}
		ground(helper);
		String kit = "flammable_small";
		Blueprint blueprint = TankKitItem.blueprint(kit);
		BlockPos stake = helper.absolutePos(new BlockPos(12, 1, 14));
		helper.assertTrue(TankKitItem.place(helper.getLevel(), kit, stake, Rotation.CLOCKWISE_90, null) == null, "the kit is placed turned");
		clearItems(helper, stake);
		// The ladder this time.
		BlockPos ladder = null;
		for (int i = 0; i < blueprint.size(); i++) {
			BlockPos p = stake.offset(blueprint.offset(i, Rotation.CLOCKWISE_90));
			if (helper.getLevel().getBlockState(p).getBlock().getDescriptionId().endsWith(".caged_ladder")) {
				ladder = p;
				break;
			}
		}
		helper.assertTrue(ladder != null, "the tank has a ladder");
		ServerPlayer player = player(helper, ladder.east(2), GameType.CREATIVE);
		player.gameMode.destroyBlock(ladder);
		int left = 0;
		for (int i = 0; i < blueprint.size(); i++) {
			BlockPos p = stake.offset(blueprint.offset(i, Rotation.CLOCKWISE_90));
			if (blueprint.offsetY(i) >= 0 && !helper.getLevel().getBlockState(p).isAir()) {
				left++;
			}
		}
		helper.assertTrue(left == 0, left + " blocks of the tank are left standing");
		helper.assertTrue(dropped(helper, stake, 30, null) == 0, "nothing drops in creative");
		helper.succeed();
	}

	@GameTest(structure = ARENA, maxTicks = 100, skyAccess = true)
	public void damagedKitTankBreaksBlockByBlock(GameTestHelper helper) {
		if (TestFocus.skip("blueprints")) {
			helper.succeed();
			return;
		}
		ground(helper);
		String kit = "general_small";
		Blueprint blueprint = TankKitItem.blueprint(kit);
		BlockPos stake = helper.absolutePos(new BlockPos(12, 1, 14));
		helper.assertTrue(TankKitItem.place(helper.getLevel(), kit, stake, Rotation.NONE, null) == null, "the kit is placed");
		StorageTankPortBlockEntity tank = outlet(helper, blueprint, stake, Rotation.NONE);
		// Something (an explosion) has taken a railing off the roof.
		BlockPos railing = null;
		for (int i = 0; i < blueprint.size(); i++) {
			BlockPos p = stake.offset(blueprint.offset(i, Rotation.NONE));
			if (helper.getLevel().getBlockState(p).getBlock().getDescriptionId().endsWith(".railing")) {
				railing = p;
			}
		}
		helper.assertTrue(railing != null, "the tank has railing");
		helper.getLevel().setBlock(railing, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
		clearItems(helper, stake);
		BlockPos plate = wallPlate(helper, blueprint, stake, Rotation.NONE);
		ServerPlayer player = player(helper, plate.south(2), GameType.SURVIVAL);
		player.gameMode.destroyBlock(plate);
		helper.assertTrue(helper.getLevel().getBlockState(plate).isAir(), "the plate broke");
		helper.assertTrue(tank.kit() == null, "the damaged tank forgot its kit");
		helper.assertTrue(helper.getLevel().getBlockState(plate.above()).getBlock() instanceof StorageTankPlateBlock, "the rest still stands");
		helper.assertTrue(dropped(helper, stake, 30, StorageTanks.ITEMS.get("tank_kit_" + kit)) == 0, "no kit back from a damaged tank");
		helper.succeed();
	}

	@GameTest(structure = ARENA, maxTicks = 100, skyAccess = true)
	public void olderKitTanksAreRecognised(GameTestHelper helper) {
		if (TestFocus.skip("blueprints")) {
			helper.succeed();
			return;
		}
		ground(helper);
		// Laid as a kit laid it before tanks remembered their kits: the blocks alone.
		String kit = "corrosive_medium";
		Blueprint blueprint = TankKitItem.blueprint(kit);
		BlockPos stake = helper.absolutePos(new BlockPos(14, 1, 16));
		PlantBuilder.build(helper.getLevel(), blueprint, stake, Rotation.COUNTERCLOCKWISE_90, null, false);
		StorageTankPortBlockEntity tank = outlet(helper, blueprint, stake, Rotation.COUNTERCLOCKWISE_90);
		helper.assertTrue(tank.formed(), "it forms");
		helper.assertTrue(kit.equals(tank.kit()), "it is recognised as the " + kit + " kit's: " + tank.kit());
		clearItems(helper, stake);
		BlockPos plate = wallPlate(helper, blueprint, stake, Rotation.COUNTERCLOCKWISE_90);
		ServerPlayer player = player(helper, plate.west(2), GameType.SURVIVAL);
		player.gameMode.destroyBlock(plate);
		int left = 0;
		for (int i = 0; i < blueprint.size(); i++) {
			BlockPos p = stake.offset(blueprint.offset(i, Rotation.COUNTERCLOCKWISE_90));
			BlockState there = helper.getLevel().getBlockState(p);
			if (blueprint.offsetY(i) < 0) {
				helper.assertTrue(there.is(Blocks.GRASS_BLOCK) || there.is(Blocks.DIRT), "the ground comes back as earth: " + there);
			} else if (!there.isAir()) {
				left++;
			}
		}
		helper.assertTrue(left == 0, left + " blocks of the tank are left standing");
		helper.assertTrue(dropped(helper, plate, 3, StorageTanks.ITEMS.get("tank_kit_" + kit)) == 1, "its kit dropped");
		helper.succeed();
	}

	@GameTest(structure = ARENA, maxTicks = 40)
	public void furnitureGoesWholeHoweverItBreaks(GameTestHelper helper) {
		if (TestFocus.skip("blueprints")) {
			helper.succeed();
			return;
		}
		ServerPlayer player = player(helper, helper.absolutePos(new BlockPos(10, 1, 6)), GameType.SURVIVAL);
		MultiFurnitureBlock screen = (MultiFurnitureBlock) PlantInterior.BLOCKS.get("status_screen");
		ItemStack stack = new ItemStack(screen);
		player.setItemInHand(InteractionHand.MAIN_HAND, stack);
		player.setYRot(180);   // looking north
		BlockPos floor = helper.absolutePos(new BlockPos(10, 0, 10));
		helper.getLevel().setBlock(floor, Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
		BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(floor).relative(Direction.UP, 0.5), Direction.UP, floor, false);
		BlockPlaceContext place = new BlockPlaceContext(player, InteractionHand.MAIN_HAND, stack, hit);
		// The preview's copy: all six blocks, and it fits.
		StructureGhost ghost = screen.ghost(place);
		helper.assertTrue(ghost.cells().size() == 6 && ghost.fits(), "the screen's copy: " + ghost.cells().size() + " blocks, fits " + ghost.fits());
		// Placed, then one block blown away (no player): the rest goes with it, one screen drops.
		BlockPos origin = place.getClickedPos();
		BlockState state = screen.getStateForPlacement(place);
		helper.assertTrue(state != null, "it can be placed");
		helper.getLevel().setBlock(origin, state, Block.UPDATE_ALL);
		screen.setPlacedBy(helper.getLevel(), origin, state, player, stack);
		for (StructureGhost.Cell cell : ghost.cells()) {
			helper.assertTrue(helper.getLevel().getBlockState(cell.pos()).is(screen), "placed at " + cell.pos());
		}
		clearItems(helper, origin);
		BlockPos corner = ghost.cells().get(ghost.cells().size() - 1).pos();
		helper.getLevel().destroyBlock(corner, true);
		for (StructureGhost.Cell cell : ghost.cells()) {
			helper.assertTrue(helper.getLevel().getBlockState(cell.pos()).isAir(), "a part is left at " + cell.pos());
		}
		helper.assertTrue(dropped(helper, origin, 4, screen.asItem()) == 1, "one screen drops: " + dropped(helper, origin, 4, screen.asItem()));
		// Something in the way: the copy marks the cell.
		helper.getLevel().setBlock(origin.above(), Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
		StructureGhost blocked = screen.ghost(new BlockPlaceContext(player, InteractionHand.MAIN_HAND, stack, hit));
		helper.assertTrue(!blocked.fits() && blocked.blocked().contains(origin.above()), "the stone in the way is marked: " + blocked.blocked());
		helper.succeed();
	}

	@GameTest(maxTicks = 20)
	public void previewsShowTheRealThing(GameTestHelper helper) {
		if (TestFocus.skip("blueprints")) {
			helper.succeed();
			return;
		}
		// A kit's copy is the tank formed: painted plates in the kit's rating, a roof, the space inside left out.
		TankKitItem.Formed formed = TankKitItem.formed("flammable_medium", Rotation.CLOCKWISE_180);
		helper.assertTrue(formed != null && !formed.inside().isEmpty(), "the medium flammable kit's copy");
		int roof = 0, stripe = 0, primer = 0;
		for (Blueprint.Cell cell : formed.cells()) {
			if (cell.state().getBlock() instanceof StorageTankPlateBlock) {
				if (!cell.state().getValue(StorageTankPlateBlock.FORMED)) {
					primer++;
				}
				roof += cell.state().getValue(StorageTankPlateBlock.BAND) == StorageTankPlateBlock.Band.ROOF ? 1 : 0;
				stripe += cell.state().getValue(StorageTankPlateBlock.BAND) == StorageTankPlateBlock.Band.STRIPE ? 1 : 0;
			}
			helper.assertFalse(formed.inside().contains(cell.offset()), "a block in the space inside");
		}
		helper.assertTrue(primer == 0 && roof > 0 && stripe > 0, "plates painted: " + primer + " in primer, " + roof + " roof, " + stripe + " stripe");
		// Machines that grew into multi-blocks reach as far as their full size says.
		int enlarged = 0;
		for (MachineBlock machine : JugcraftMachines.MACHINES.values()) {
			if (machine instanceof EnlargedMachineBlock big) {
				int side = StructureReach.longestSide(new ItemStack(big), false);
				helper.assertTrue(side > 1, machine.kind() + ": an enlarged machine has a placing reach (" + side + ")");
				enlarged++;
			}
		}
		helper.assertTrue(enlarged > 0, "there are enlarged machines");
		helper.succeed();
	}
}
