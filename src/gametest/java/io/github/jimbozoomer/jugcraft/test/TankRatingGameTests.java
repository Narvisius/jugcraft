package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.blueprint.Blueprint;
import io.github.jimbozoomer.jugcraft.nuclear.StorageTankOutletBlock;
import io.github.jimbozoomer.jugcraft.nuclear.StorageTankPlateBlock;
import io.github.jimbozoomer.jugcraft.nuclear.StorageTankPortBlock;
import io.github.jimbozoomer.jugcraft.nuclear.StorageTankPortBlockEntity;
import io.github.jimbozoomer.jugcraft.nuclear.StorageTanks;
import io.github.jimbozoomer.jugcraft.nuclear.TankKitItem;
import io.github.jimbozoomer.jugcraft.nuclear.TankProducts;
import io.github.jimbozoomer.jugcraft.nuclear.TankRating;
import io.github.jimbozoomer.jugcraft.nuclear.TankUpgrades;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

/**
 * The storage tank ratings (Justin, Oct 7; docs/features/nuclear.md, "Storage tank ratings"): each liquid's label
 * (kind, fire diamond, UN number); a tank takes only what its rating allows, at the inlet as at the hatch; a flame arrester
 * or rubber lining re-rates an empty tank (repainted, its vent swapped) and is refused while it holds anything; a name tag
 * titles it; a kit places a whole tank that forms at once, refuses where something stands in the way, and the cryogenic
 * kit's tank takes liquid oxygen and nothing else.
 */
public class TankRatingGameTests {
	private static final String ARENA = "jugcraft-test:drone_tower";

	private static Fluid fluid(String id) {
		return BuiltInRegistries.FLUID.getValue(Identifier.parse(id));
	}

	private static long insert(Storage<FluidVariant> storage, Fluid fluid, long amount) {
		try (Transaction t = Transaction.openOuter()) {
			long moved = storage.insert(FluidVariant.of(fluid), amount, t);
			t.commit();
			return moved;
		}
	}

	private static long extract(Storage<FluidVariant> storage, Fluid fluid, long amount) {
		try (Transaction t = Transaction.openOuter()) {
			long moved = storage.extract(FluidVariant.of(fluid), amount, t);
			t.commit();
			return moved;
		}
	}

	/** The small square tank of StorageTankGameTests, outlet north at (3, 1, 0), inlet east at (6, 1, 3), a vent on the roof. */
	private static StorageTankPortBlockEntity tank(GameTestHelper helper, TankRating rating) {
		BlockState plate = StorageTanks.PLATE.defaultBlockState();
		for (int x = 0; x <= 7; x++) {
			for (int z = 0; z <= 7; z++) {
				helper.setBlock(new BlockPos(x, 0, z), Blocks.SMOOTH_STONE.defaultBlockState());
			}
		}
		for (int x = 1; x <= 5; x++) {
			for (int z = 1; z <= 5; z++) {
				helper.setBlock(new BlockPos(x, 4, z), plate);
				for (int y = 1; y <= 3; y++) {
					if (x == 1 || x == 5 || z == 1 || z == 5) {
						helper.setBlock(new BlockPos(x, y, z), plate);
					}
				}
			}
		}
		helper.setBlock(new BlockPos(3, 5, 3), StorageTanks.BLOCKS.get("storage_tank_vent"));
		helper.setBlock(new BlockPos(3, 1, 0), StorageTanks.OUTLET.defaultBlockState().setValue(StorageTankPortBlock.FACING, Direction.NORTH)
				.setValue(StorageTankOutletBlock.RATING, rating));
		helper.setBlock(new BlockPos(6, 1, 3), StorageTanks.INLET.defaultBlockState().setValue(StorageTankPortBlock.FACING, Direction.EAST));
		StorageTankPortBlockEntity outlet = (StorageTankPortBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(new BlockPos(3, 1, 0)));
		outlet.check(helper.getLevel());
		helper.assertTrue(outlet.formed(), "the tank forms");
		return outlet;
	}

	private static Storage<FluidVariant> inlet(GameTestHelper helper) {
		return FluidStorage.SIDED.find(helper.getLevel(), helper.absolutePos(new BlockPos(6, 1, 3)), Direction.EAST);
	}

	@GameTest(maxTicks = 20)
	public void labelsAndRatings(GameTestHelper helper) {
		if (TestFocus.skip("nuclear")) {
			helper.succeed();
			return;
		}
		TankProducts.Product diesel = TankProducts.of(fluid("jugcraft:diesel"));
		helper.assertTrue(diesel.kind() == TankProducts.Kind.FLAMMABLE && diesel.health() == 1 && diesel.fire() == 2 && diesel.instability() == 0
				&& diesel.un().equals("1202") && diesel.diamond(), "diesel: flammable, 1-2-0, UN 1202");
		TankProducts.Product acid = TankProducts.of(fluid("jugcraft:sulfuric_acid"));
		helper.assertTrue(acid.kind() == TankProducts.Kind.CORROSIVE && acid.special().equals("W") && acid.un().equals("1830"),
				"sulfuric acid: corrosive, W, UN 1830");
		helper.assertTrue(TankProducts.of(fluid("jugcraft:nitric_acid")).special().equals("OX"), "nitric acid is an oxidizer");
		TankProducts.Product lox = TankProducts.of(fluid("jugcraft:liquid_oxygen"));
		helper.assertTrue(lox.kind() == TankProducts.Kind.CRYOGENIC && lox.special().equals("OX") && lox.health() == 3, "liquid oxygen: cryogenic, 3-0-0 OX");
		helper.assertTrue(!TankProducts.of(Fluids.WATER).diamond(), "water shows no diamond");
		helper.assertTrue(TankProducts.of(Fluids.LAVA).kind() == TankProducts.Kind.REFUSED, "lava goes in no storage tank");
		helper.assertTrue(TankProducts.of(fluid("jugcraft:hydrogen")).kind() == TankProducts.Kind.REFUSED, "nor does a gas");
		helper.assertTrue(TankRating.FLAMMABLE.takes(TankProducts.Kind.GENERAL) && TankRating.CORROSIVE.takes(TankProducts.Kind.GENERAL),
				"rated tanks also take general liquids");
		helper.assertTrue(!TankRating.CRYOGENIC.takes(TankProducts.Kind.GENERAL), "a cryogenic tank takes only cryogenic liquids");
		helper.assertTrue(!TankRating.GENERAL.takes(TankProducts.Kind.FLAMMABLE) && !TankRating.FLAMMABLE.takes(TankProducts.Kind.CORROSIVE),
				"each class needs its own rating");
		Component refused = TankUpgrades.refusal(TankRating.GENERAL, FluidVariant.of(fluid("jugcraft:diesel")));
		helper.assertTrue(refused != null && refused.getString().contains("flammable"), "the refusal names the rating needed: " + refused);
		helper.succeed();
	}

	@GameTest(maxTicks = 40)
	public void tankTakesOnlyWhatItIsRatedFor(GameTestHelper helper) {
		if (TestFocus.skip("nuclear")) {
			helper.succeed();
			return;
		}
		StorageTankPortBlockEntity outlet = tank(helper, TankRating.GENERAL);
		helper.assertBlockProperty(new BlockPos(3, 1, 1), StorageTankPlateBlock.RATING, TankRating.GENERAL);
		Storage<FluidVariant> in = inlet(helper);
		helper.assertTrue(insert(in, fluid("jugcraft:diesel"), FluidConstants.BUCKET) == 0, "a general tank refuses diesel");
		helper.assertTrue(insert(in, Fluids.WATER, FluidConstants.BUCKET) == FluidConstants.BUCKET, "and takes water");
		helper.assertTrue(TankProducts.of(outlet.label()).equals(TankProducts.of(Fluids.WATER)), "its label is water's");
		// Emptied, it keeps the label.
		Storage<FluidVariant> out = FluidStorage.SIDED.find(helper.getLevel(), helper.absolutePos(new BlockPos(3, 1, 0)), Direction.NORTH);
		helper.assertTrue(extract(out, Fluids.WATER, FluidConstants.BUCKET) == FluidConstants.BUCKET, "the water drains out");
		helper.assertTrue(outlet.label().equals(FluidVariant.of(Fluids.WATER)), "an empty tank keeps its last product's label");
		helper.assertTrue(insert(in, fluid("jugcraft:sulfuric_acid"), FluidConstants.BUCKET) == 0, "and refuses acid");
		helper.succeed();
	}

	@GameTest(maxTicks = 40)
	public void emptyTankIsReRated(GameTestHelper helper) {
		if (TestFocus.skip("nuclear")) {
			helper.succeed();
			return;
		}
		StorageTankPortBlockEntity outlet = tank(helper, TankRating.GENERAL);
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		BlockPos plate = helper.absolutePos(new BlockPos(3, 2, 1));
		// A name tag titles it.
		ItemStack tag = new ItemStack(Items.NAME_TAG);
		tag.set(DataComponents.CUSTOM_NAME, Component.literal("Main Fuel Store"));
		TankUpgrades.use(tag, helper.getLevel(), plate, player);
		helper.assertTrue(outlet.title().equals("Main Fuel Store"), "titled: " + outlet.title());
		// A flame arrester on the empty tank: re-rated, repainted, its vent swapped for a pressure/vacuum vent.
		ItemStack arrester = new ItemStack(StorageTanks.FLAME_ARRESTER, 2);
		TankUpgrades.use(arrester, helper.getLevel(), plate, player);
		StorageTankPortBlockEntity tank = (StorageTankPortBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(new BlockPos(3, 1, 0)));
		helper.assertTrue(tank.rating() == TankRating.FLAMMABLE, "re-rated flammable: " + tank.rating());
		helper.assertTrue(tank.formed(), "still a tank");
		helper.assertTrue(arrester.getCount() == 1, "the arrester is used up");
		helper.assertBlockProperty(new BlockPos(3, 1, 1), StorageTankPlateBlock.RATING, TankRating.FLAMMABLE);
		helper.assertBlockPresent(StorageTanks.BLOCKS.get("storage_tank_pv_vent"), new BlockPos(3, 5, 3));
		helper.assertTrue(tank.title().equals("Main Fuel Store"), "the title stays");
		// Now it takes diesel; holding it, it can't be re-rated.
		helper.assertTrue(insert(inlet(helper), fluid("jugcraft:diesel"), FluidConstants.BUCKET) == FluidConstants.BUCKET, "it takes diesel now");
		ItemStack lining = new ItemStack(StorageTanks.RUBBER_LINING);
		TankUpgrades.use(lining, helper.getLevel(), plate, player);
		helper.assertTrue(tank.rating() == TankRating.FLAMMABLE && lining.getCount() == 1, "not re-rated while it holds diesel");
		helper.assertTrue(TankUpgrades.rerate(helper.getLevel(), tank, TankRating.CRYOGENIC) != null, "nothing becomes cryogenic");
		helper.succeed();
	}

	/** The kit's outlet, where it lands with its stake at {@code stake}. */
	private static StorageTankPortBlockEntity outletOf(GameTestHelper helper, String kit, BlockPos stake) {
		Blueprint blueprint = TankKitItem.blueprint(kit);
		helper.assertTrue(blueprint != null, kit + ": its structure is in the jar");
		for (int i = 0; i < blueprint.size(); i++) {
			if (blueprint.state(i, Rotation.NONE).getBlock() instanceof StorageTankOutletBlock) {
				return (StorageTankPortBlockEntity) helper.getLevel().getBlockEntity(stake.offset(blueprint.offset(i, Rotation.NONE)));
			}
		}
		throw new AssertionError(kit + " has no outlet");
	}

	private static void ground(GameTestHelper helper) {
		for (int x = 0; x < 44; x++) {
			for (int z = 0; z < 44; z++) {
				helper.setBlock(new BlockPos(x, 0, z), Blocks.GRASS_BLOCK.defaultBlockState());
			}
		}
	}

	@GameTest(structure = ARENA, maxTicks = 60, skyAccess = true)
	public void kitsPlaceWholeTanks(GameTestHelper helper) {
		if (TestFocus.skip("nuclear")) {
			helper.succeed();
			return;
		}
		ground(helper);
		// Tall grass where the flammable tank goes: a kit clears it.
		helper.setBlock(new BlockPos(10, 1, 6), Blocks.SHORT_GRASS);
		BlockPos stake = helper.absolutePos(new BlockPos(10, 1, 14));
		Component refused = TankKitItem.place(helper.getLevel(), "flammable_medium", stake, Rotation.NONE, null);
		helper.assertTrue(refused == null, "the medium flammable kit is placed: " + refused);
		StorageTankPortBlockEntity tank = outletOf(helper, "flammable_medium", stake);
		helper.assertTrue(tank != null && tank.formed(), "its tank forms at once");
		helper.assertTrue(tank.rating() == TankRating.FLAMMABLE, "rated flammable");
		helper.assertTrue(tank.fluid.capacity() == 1184 * FluidConstants.BUCKET, "1,184 buckets: " + tank.fluid.capacity() / FluidConstants.BUCKET);
		helper.assertTrue(tank.stencilFace() == Direction.SOUTH, "the label faces whoever placed it: " + tank.stencilFace());
		helper.assertBlockPresent(Blocks.GRASS_BLOCK, new BlockPos(10, 0, 14));
		// Again on the same spot: something is in the way.
		helper.assertTrue(TankKitItem.place(helper.getLevel(), "flammable_medium", stake, Rotation.NONE, null) != null, "refused where a tank stands");
		// The cryogenic tank, on its legs: liquid oxygen in, water refused.
		BlockPos cryo = helper.absolutePos(new BlockPos(30, 1, 14));
		helper.assertTrue(TankKitItem.place(helper.getLevel(), "cryogenic", cryo, Rotation.NONE, null) == null, "the cryogenic kit is placed");
		StorageTankPortBlockEntity cold = outletOf(helper, "cryogenic", cryo);
		helper.assertTrue(cold.formed() && cold.rating() == TankRating.CRYOGENIC, "the cryogenic tank forms, rated cryogenic");
		Storage<FluidVariant> out = FluidStorage.SIDED.find(helper.getLevel(), cold.getBlockPos(), cold.getBlockState().getValue(StorageTankPortBlock.FACING));
		helper.assertTrue(out != null, "its outlet takes a pipe");
		helper.assertTrue(insert(cold.fluid, Fluids.WATER, FluidConstants.BUCKET) == 0, "it refuses water");
		helper.assertTrue(insert(cold.fluid, fluid("jugcraft:liquid_oxygen"), FluidConstants.BUCKET) == FluidConstants.BUCKET, "it takes liquid oxygen");
		// A corrosive kit, turned: its safety shower stands, two blocks tall.
		BlockPos acid = helper.absolutePos(new BlockPos(16, 1, 30));
		helper.assertTrue(TankKitItem.place(helper.getLevel(), "corrosive_small", acid, Rotation.CLOCKWISE_90, null) == null, "the corrosive kit is placed turned");
		Blueprint bp = TankKitItem.blueprint("corrosive_small");
		boolean shower = false;
		for (int i = 0; i < bp.size(); i++) {
			BlockPos p = acid.offset(bp.offset(i, Rotation.CLOCKWISE_90));
			if (helper.getLevel().getBlockState(p).is(StorageTanks.BLOCKS.get("safety_shower"))) {
				shower = helper.getLevel().getBlockState(p.above()).is(StorageTanks.BLOCKS.get("safety_shower_head"));
			}
			if (bp.state(i, Rotation.CLOCKWISE_90).getBlock() instanceof StorageTankOutletBlock) {
				StorageTankPortBlockEntity a = (StorageTankPortBlockEntity) helper.getLevel().getBlockEntity(p);
				helper.assertTrue(a.formed() && a.rating() == TankRating.CORROSIVE, "the corrosive tank forms, rated corrosive");
			}
		}
		helper.assertTrue(shower, "the safety shower stands with its head on it");
		helper.succeed();
	}

	@GameTest(maxTicks = 20)
	public void kitItemsHaveTheirParts(GameTestHelper helper) {
		if (TestFocus.skip("nuclear")) {
			helper.succeed();
			return;
		}
		for (String kit : new String[] {"general_small", "general_medium", "general_large", "flammable_small", "flammable_medium",
				"flammable_large", "corrosive_small", "corrosive_medium", "corrosive_large", "cryogenic"}) {
			helper.assertTrue(StorageTanks.ITEMS.get("tank_kit_" + kit) instanceof TankKitItem item && item.kit.equals(kit), kit + " is a kit item");
			Blueprint blueprint = TankKitItem.blueprint(kit);
			helper.assertTrue(blueprint != null && blueprint.anchorY() == 1, kit + ": stands on the ground");
			int outlets = 0;
			TankRating rating = null;
			for (int i = 0; i < blueprint.size(); i++) {
				BlockState s = blueprint.state(i, Rotation.NONE);
				if (s.getBlock() instanceof StorageTankOutletBlock) {
					outlets++;
					rating = s.getValue(StorageTankOutletBlock.RATING);
				}
			}
			helper.assertTrue(outlets == 1, kit + ": one outlet");
			helper.assertTrue(rating == ((TankKitItem) StorageTanks.ITEMS.get("tank_kit_" + kit)).rating, kit + ": the outlet's rating is the kit's");
		}
		helper.succeed();
	}
}
