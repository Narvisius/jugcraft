package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.blueprint.Blueprint;
import io.github.jimbozoomer.jugcraft.blueprint.BuildSite;
import io.github.jimbozoomer.jugcraft.nuclear.CoolingTowerBlockEntity;
import io.github.jimbozoomer.jugcraft.nuclear.EquipmentBlockEntity;
import io.github.jimbozoomer.jugcraft.nuclear.EquipmentKind;
import io.github.jimbozoomer.jugcraft.nuclear.FramedGlassBlock;
import io.github.jimbozoomer.jugcraft.nuclear.JugcraftNuclear;
import io.github.jimbozoomer.jugcraft.nuclear.PlantInterior;
import io.github.jimbozoomer.jugcraft.nuclear.PlantBuilder;
import io.github.jimbozoomer.jugcraft.nuclear.ReactorCore;
import io.github.jimbozoomer.jugcraft.nuclear.ReactorUnit;
import io.github.jimbozoomer.jugcraft.nuclear.SpeedGateBlock;
import io.github.jimbozoomer.jugcraft.nuclear.SpeedGateBlockEntity;
import io.github.jimbozoomer.jugcraft.nuclear.SpeedGatePedestalBlock;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.block.Rotation;

/**
 * The pre-made nuclear plants (docs/features/nuclear.md, "Pre-made plants"): the plant and part blueprints load from
 * the jar at their full size, the new core designs and cooling towers behave, and a whole small modular plant built by
 * /nuclear build forms six working units with both cooling tower banks.
 */
public class NuclearPlantGameTests {
	/** Block counts the blueprints must have (the same as tools/nuclear_plants.py writes; checked again there). */
	private static final String[][] PLANTS = {{"nuclear_small_modular", "118096"}, {"nuclear_medium_pwr", "340708"}, {"nuclear_large_epr", "383209"}};

	@GameTest(maxTicks = 20)
	public void plantBlueprintsLoadWhole(GameTestHelper helper) {
		if (TestFocus.skip("nuclear")) {
			helper.succeed();
			return;
		}
		for (String[] plant : PLANTS) {
			Blueprint blueprint = Blueprint.get(plant[0], false);
			helper.assertTrue(blueprint != null, plant[0] + " loads");
			helper.assertTrue(blueprint.size() == Integer.parseInt(plant[1]), plant[0] + " has " + plant[1] + " blocks: " + blueprint.size());
			helper.assertTrue(blueprint.kind == Blueprint.Kind.COMPLETE && blueprint.category == Blueprint.Category.SET, plant[0] + " is a blue set");
			helper.assertTrue(blueprint.large() && blueprint.local() && blueprint.json.isEmpty(), plant[0] + " is a large built-in, never sent");
			helper.assertTrue(!blueprint.parts.isEmpty(), plant[0] + " lists its parts");
		}
		for (String part : new String[] {"ct_natural", "ct_natural_large", "ct_mechanical", "pwr_containment", "epr_turbine_hall"}) {
			Blueprint blueprint = Blueprint.get(part, false);
			helper.assertTrue(blueprint != null && blueprint.kind == Blueprint.Kind.PART, part + " is a green part");
		}
		// Bottom first, and a site finds its cells again.
		Blueprint tower = Blueprint.get("ct_natural", false);
		BuildSite site = new BuildSite(tower, Rotation.CLOCKWISE_90, BlockPos.ZERO);
		for (int i = 1; i < site.size(); i++) {
			helper.assertTrue(site.layerOf(i) >= site.layerOf(i - 1), "cells go bottom layer first");
		}
		helper.assertTrue(site.find(site.blockPos(site.size() / 2)) == site.size() / 2, "find gives the cell back");
		helper.succeed();
	}

	/** The EPR core makes 1.6 times the standard output with two turbines, one turbine's worth with one; a module an eighth. */
	@GameTest(maxTicks = 20)
	public void coreDesignsAndTowers(GameTestHelper helper) {
		if (TestFocus.skip("nuclear")) {
			helper.succeed();
			return;
		}
		long two = run(ReactorCore.Design.EPR, 2 * ReactorCore.RATED_JE, 0);
		long one = run(ReactorCore.Design.EPR, ReactorCore.RATED_JE, 0);
		long module = run(ReactorCore.Design.SMALL_MODULAR, ReactorCore.RATED_JE, 0);
		helper.assertTrue(Math.abs(two - ReactorCore.RATED_JE * 8 / 5) < ReactorCore.RATED_JE / 30, "EPR with two turbines: " + two);
		helper.assertTrue(one == ReactorCore.RATED_JE, "EPR with one turbine is capped: " + one);
		helper.assertTrue(Math.abs(module - ReactorCore.RATED_JE / 8) < ReactorCore.RATED_JE / 200, "module: " + module);
		// With a cooling tower taking all the heat, the condenser only asks for make-up water.
		ReactorCore core = fuelled(ReactorCore.Design.STANDARD);
		ReactorCore.Plant towered = new ReactorCore.Plant(1, true, true, ReactorCore.RATED_JE, 1);
		for (int t = 0; t < 20 * 600; t++) {
			core.tick(towered, need -> need);
		}
		helper.assertTrue(core.need <= ReactorCore.FULL_WATER * ReactorCore.MAKEUP * 1.05 && core.need > 0, "make-up only: " + core.need);
		helper.succeed();
	}

	private static ReactorCore fuelled(ReactorCore.Design design) {
		ReactorCore core = new ReactorCore();
		core.design = design;
		for (int i = 0; i < design.size(); i++) core.fuel.add(0.0);
		core.temperature = 290;
		core.rodTarget = 0;
		return core;
	}

	private static long run(ReactorCore.Design design, long limit, double tower) {
		ReactorCore core = fuelled(design);
		ReactorCore.Plant plant = new ReactorCore.Plant(1, true, true, limit, tower);
		long electric = 0;
		for (int t = 0; t < 20 * 600; t++) {
			electric = core.tick(plant, need -> need);
		}
		return electric;
	}

	/** /nuclear build of the small modular plant, far from the other tests: six modules, each a working unit, and both tower banks formed. */
	@GameTest(maxTicks = 200)
	public void smallPlantBuildsAndForms(GameTestHelper helper) {
		if (TestFocus.skip("nuclear")) {
			helper.succeed();
			return;
		}
		var level = helper.getLevel();
		BlockPos stake = helper.absolutePos(BlockPos.ZERO).offset(4_000, 0, 4_000);
		PlantBuilder.Result built = PlantBuilder.build(level, Blueprint.get("nuclear_small_modular", false), stake, Rotation.NONE, null, true);
		helper.assertTrue(built.reactors() == 6, "six modules: " + built.reactors());
		for (BlockPos vessel : built.vessels()) {
			ReactorUnit unit = ReactorUnit.find(level, vessel);
			helper.assertTrue(unit.module && unit.formed() && unit.smallTurbines.size() == 1, "module " + vessel.toShortString() + " forms: " + unit.missing());
			var v = (EquipmentBlockEntity) level.getBlockEntity(vessel);
			helper.assertTrue(v.core.fuel.size() == 37, "fuelled: " + v.core.fuel.size());
		}
		int towers = 0;
		Blueprint blueprint = Blueprint.get("nuclear_small_modular", false);
		for (int i = 0; i < blueprint.size(); i++) {
			if (blueprint.state(i, Rotation.NONE).is(JugcraftNuclear.COOLING_TOWER)) {
				BlockPos at = stake.offset(blueprint.offset(i, Rotation.NONE));
				helper.assertTrue(level.getBlockEntity(at) instanceof CoolingTowerBlockEntity tower && tower.checkNow(level), "tower bank at " + at.toShortString() + " formed");
				towers++;
			}
		}
		helper.assertTrue(towers == 2, "two tower banks: " + towers);
		helper.assertTrue(EquipmentKind.SMALL_MODULAR_REACTOR.design() == ReactorCore.Design.SMALL_MODULAR, "module design");
		surveyAtFullPower(helper, built, "small modular plant", blueprint, stake);
		helper.succeed();
	}

	/**
	 * Round 14: "make sure wherever the reactor is, it's shielded". With every core of the medium and large plants at full
	 * power, nothing measurable gets out of the reactor buildings: everywhere in the open round them (on the ground and on
	 * the roofs, within 70 blocks) reads below a tenth of a microsievert an hour over background.
	 */
	@GameTest(maxTicks = 400)
	public void reactorBuildingsKeepRadiationIn(GameTestHelper helper) {
		if (TestFocus.skip("nuclear")) {
			helper.succeed();
			return;
		}
		var level = helper.getLevel();
		BlockPos origin = helper.absolutePos(BlockPos.ZERO);
		Blueprint mediumPrint = Blueprint.get("nuclear_medium_pwr", false);
		PlantBuilder.Result medium = PlantBuilder.build(level, mediumPrint, origin.offset(-5_000, 0, 4_000), Rotation.NONE, null, true);
		surveyAtFullPower(helper, medium, "medium plant", mediumPrint, origin.offset(-5_000, 0, 4_000));
		Blueprint largePrint = Blueprint.get("nuclear_large_epr", false);
		PlantBuilder.Result large = PlantBuilder.build(level, largePrint, origin.offset(5_000, 0, -5_000), Rotation.NONE, null, true);
		surveyAtFullPower(helper, large, "large plant", largePrint, origin.offset(5_000, 0, -5_000));
		helper.succeed();
	}

	/**
	 * Each core of a built plant set to full power, then the open ground and roofs round it surveyed: at each spot, eye
	 * height above the topmost block of the column (read block by block, so a roof or dome always counts).
	 */
	private static void surveyAtFullPower(GameTestHelper helper, PlantBuilder.Result built, String plant, Blueprint blueprint, BlockPos stake) {
		var level = helper.getLevel();
		// First, that the build put down what the blueprint says (equipment bodies aside), so a leak is a design fault.
		int wrong = 0;
		StringBuilder missing = new StringBuilder();
		for (int i = 0; i < blueprint.size(); i++) {
			var want = blueprint.state(i, Rotation.NONE);
			if (want.getBlock() instanceof io.github.jimbozoomer.jugcraft.nuclear.EquipmentAnchorBlock) {
				continue;
			}
			BlockPos pos = stake.offset(blueprint.offset(i, Rotation.NONE));
			var have = level.getBlockState(pos);
			if (have.getBlock() != want.getBlock() && level.getBlockEntity(pos) == null) {
				if (wrong++ < 6) {
					missing.append(" ").append(pos.subtract(stake).toShortString()).append(" ")
							.append(net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(want.getBlock()).getPath()).append("->")
							.append(net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(have.getBlock()).getPath());
				}
			}
		}
		String build = wrong + " blocks differ from the blueprint after the build" + (wrong == 0 ? "" : ":" + missing);
		io.github.jimbozoomer.jugcraft.Jugcraft.LOGGER.info("{}: {}", plant, build);
		java.util.List<BlockPos> cores = new java.util.ArrayList<>();
		for (BlockPos vessel : built.vessels()) {
			if (level.getBlockEntity(vessel) instanceof EquipmentBlockEntity v) {
				cores.add(v.coreMiddle());
				io.github.jimbozoomer.jugcraft.nuclear.RadiationSources.set(level, v.coreMiddle(), EquipmentBlockEntity.CORE_RADIATION * 1.07,
						io.github.jimbozoomer.jugcraft.nuclear.RadiationKind.NUCLEAR);
			}
		}
		helper.assertTrue(!cores.isEmpty(), plant + ": reactors found");
		double worst = 0;
		BlockPos where = null;
		int points = 0;
		try {
			for (BlockPos core : cores) {
				for (int dx = -70; dx <= 70; dx += 3) {
					for (int dz = -70; dz <= 70; dz += 3) {
						if (dx * dx + dz * dz > 70 * 70) {
							continue;
						}
						int x = core.getX() + dx, z = core.getZ() + dz;
						int y = Math.min(level.getMaxY(), core.getY() + 100);
						while (y > level.getMinY() && level.getBlockState(new BlockPos(x, y - 1, z)).isAir()) {
							y--;
						}
						net.minecraft.world.phys.Vec3 eye = new net.minecraft.world.phys.Vec3(x + 0.5, y + 1.6, z + 0.5);
						double rate = io.github.jimbozoomer.jugcraft.nuclear.Radiation.rateAt(level, eye);
						points++;
						if (rate > worst) {
							worst = rate;
							where = BlockPos.containing(eye);
						}
					}
				}
			}
		} finally {
			for (BlockPos core : cores) {
				io.github.jimbozoomer.jugcraft.nuclear.RadiationSources.clear(level, core);
			}
		}
		if (worst >= 1e-4) {
			// Say where it gets out: the column at the spot, and what the line from the nearest core crosses.
			StringBuilder why = new StringBuilder();
			BlockPos near = cores.get(0);
			for (BlockPos core : cores) {
				if (core.distSqr(where) < near.distSqr(where)) {
					near = core;
				}
			}
			why.append(" | core ").append(near.toShortString()).append(" | column:");
			int found = 0;
			for (int yy = level.getMaxY(); yy >= level.getMinY() && found < 8; yy--) {
				var st = level.getBlockState(new BlockPos(where.getX(), yy, where.getZ()));
				if (!st.isAir()) {
					why.append(" ").append(yy).append("=").append(net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(st.getBlock()).getPath());
					found++;
				}
			}
			final BlockPos own = near;
			// Sources here (cores at full power, radioactive blocks) were cleared above; trace the nearest core's line.
			why.append(" | line: ").append(String.join("; ", io.github.jimbozoomer.jugcraft.nuclear.Shielding.trace(
					io.github.jimbozoomer.jugcraft.nuclear.RadiationField.states(level), net.minecraft.world.phys.Vec3.atCenterOf(near),
					net.minecraft.world.phys.Vec3.atCenterOf(where).add(0, 0.1, 0), (cell, st) -> cell.equals(own), 30)));
			why.append(" | ").append(build).append(" | stake ").append(stake.toShortString()).append(" | heightmap ")
					.append(level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, where.getX(), where.getZ()))
					.append(" | world y ").append(level.getMinY()).append("..").append(level.getMaxY());
			helper.assertTrue(false, plant + " at full power: " + (worst * 1000) + " uSv/h in the open at " + where
					+ " (" + (int) Math.sqrt(where.distSqr(near)) + " blocks from a core); want under 0.1" + why);
		}
		io.github.jimbozoomer.jugcraft.Jugcraft.LOGGER.info("{} at full power: worst {} uSv/h over {} points in the open", plant, worst * 1000, points);
	}

	// ---------------------------------------------------------------- framed glass (FramedGlassBlock)

	private static BlockPos framed(GameTestHelper helper, int x, int y, int z, Direction facing) {
		BlockPos at = helper.absolutePos(new BlockPos(x, y, z));
		FramedGlassBlock glass = (FramedGlassBlock) PlantInterior.BLOCKS.get("framed_glass");
		helper.getLevel().setBlock(at, glass.connected(glass.defaultBlockState().setValue(FramedGlassBlock.FACING, facing),
				helper.getLevel(), at), Block.UPDATE_ALL);
		return at;
	}

	private static BlockState at(GameTestHelper helper, BlockPos absolute) {
		return helper.getLevel().getBlockState(absolute);
	}

	private static void door(GameTestHelper helper, int x, int y, int z, Direction facing) {
		Block door = PlantInterior.BLOCKS.get("glass_door");
		BlockState lower = door.defaultBlockState().setValue(DoorBlock.FACING, facing);
		helper.setBlock(new BlockPos(x, y, z), lower.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER));
		helper.setBlock(new BlockPos(x, y + 1, z), lower.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
	}

	/** Placed beside a glass door it takes the door's plane, whichever way its placer faces, and lines up with its leaf. */
	@GameTest(maxTicks = 20)
	public void framedGlassMeetsTheDoorFlush(GameTestHelper helper) {
		if (TestFocus.skip("nuclear")) {
			helper.succeed();
			return;
		}
		for (int x = 0; x < 5; x++) {
			for (int z = 0; z < 5; z++) {
				helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE.defaultBlockState());
			}
		}
		door(helper, 1, 1, 2, Direction.NORTH);
		// Placed from the other side (looking south), just east of the door.
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setGameMode(GameType.CREATIVE);
		BlockPos target = helper.absolutePos(new BlockPos(2, 1, 2));
		player.setPos(target.getX() + 0.5, target.getY(), target.getZ() - 2.5);
		player.setYRot(0F);   // looking south
		ItemStack stack = new ItemStack(PlantInterior.BLOCKS.get("framed_glass"));
		player.setItemInHand(InteractionHand.MAIN_HAND, stack);
		BlockPos floor = target.below();
		BlockPlaceContext place = new BlockPlaceContext(player, InteractionHand.MAIN_HAND, stack,
				new BlockHitResult(Vec3.atCenterOf(floor).relative(Direction.UP, 0.5), Direction.UP, floor, false));
		helper.assertTrue(place.getHorizontalDirection() == Direction.SOUTH, "the placer looks south: " + place.getHorizontalDirection());
		BlockState glass = PlantInterior.BLOCKS.get("framed_glass").getStateForPlacement(place);
		helper.assertTrue(glass.getValue(FramedGlassBlock.FACING) == Direction.NORTH, "takes the door's facing: " + glass);
		helper.assertTrue(glass.getValue(FramedGlassBlock.LEFT) == FramedGlassBlock.End.JAMB, "a jamb against the door: " + glass);
		helper.getLevel().setBlock(target, glass, Block.UPDATE_ALL);
		// The panel and the closed leaf take the same slice of their blocks.
		AABB pane = glass.getShape(helper.getLevel(), target).bounds();
		BlockPos doorAt = helper.absolutePos(new BlockPos(1, 1, 2));
		AABB leaf = at(helper, doorAt).getShape(helper.getLevel(), doorAt).bounds();
		helper.assertTrue(Math.abs(pane.minZ - leaf.minZ) < 1e-6 && Math.abs(pane.maxZ - leaf.maxZ) < 1e-6,
				"flush with the door's leaf: glass z " + pane.minZ + ".." + pane.maxZ + ", leaf z " + leaf.minZ + ".." + leaf.maxZ);
		// Every facing: the panel sits where a door facing that way has its leaf.
		for (Direction facing : Direction.Plane.HORIZONTAL) {
			BlockState g = glass.setValue(FramedGlassBlock.FACING, facing);
			BlockState d = at(helper, doorAt).setValue(DoorBlock.FACING, facing);
			AABB a = g.getShape(helper.getLevel(), target).bounds();
			AABB b = d.getShape(helper.getLevel(), doorAt).bounds();
			helper.assertTrue(a.equals(b), facing + ": glass " + a + ", door " + b);
		}
		helper.succeed();
	}

	/** The glass runs on unbroken; frames only at ends, corners, top and bottom, and round a door set in it. */
	@GameTest(maxTicks = 20)
	public void framedGlassFramesOnlyWhereItNeedsSupport(GameTestHelper helper) {
		if (TestFocus.skip("nuclear")) {
			helper.succeed();
			return;
		}
		// A run along x (facing north) three wide and three high with a door in the middle and glass over it.
		door(helper, 2, 1, 1, Direction.NORTH);
		BlockPos[][] run = new BlockPos[5][4];
		for (int y = 1; y <= 3; y++) {
			for (int x = 1; x <= 3; x++) {
				if (x == 2 && y < 3) {
					continue;
				}
				run[x][y] = framed(helper, x, y, 1, Direction.NORTH);
			}
		}
		// Joined up as the game does when neighbours change: settle once more from the finished arrangement.
		for (int y = 1; y <= 3; y++) {
			for (int x = 1; x <= 3; x++) {
				if (run[x][y] != null) {
					FramedGlassBlock block = (FramedGlassBlock) at(helper, run[x][y]).getBlock();
					helper.getLevel().setBlock(run[x][y], block.connected(at(helper, run[x][y]), helper.getLevel(), run[x][y]), Block.UPDATE_ALL);
				}
			}
		}
		BlockState left = at(helper, run[1][1]), over = at(helper, run[2][3]), besideOver = at(helper, run[1][3]), mid = at(helper, run[1][2]);
		helper.assertTrue(left.getValue(FramedGlassBlock.RIGHT) == FramedGlassBlock.End.JAMB, "a jamb beside the door: " + left);
		helper.assertTrue(left.getValue(FramedGlassBlock.BOTTOM) == FramedGlassBlock.Bottom.SILL && left.getValue(FramedGlassBlock.UP), "a sill, glass going on up: " + left);
		helper.assertTrue(mid.getValue(FramedGlassBlock.BOTTOM) == FramedGlassBlock.Bottom.GLASS && mid.getValue(FramedGlassBlock.UP), "no rails between stacked glass: " + mid);
		helper.assertTrue(over.getValue(FramedGlassBlock.BOTTOM) == FramedGlassBlock.Bottom.DOOR, "the head over the door: " + over);
		helper.assertTrue(over.getValue(FramedGlassBlock.LEFT) == FramedGlassBlock.End.JOIN && over.getValue(FramedGlassBlock.RIGHT) == FramedGlassBlock.End.JOIN,
				"glass carries on either side over the door: " + over);
		helper.assertTrue(besideOver.getValue(FramedGlassBlock.RIGHT) == FramedGlassBlock.End.HEAD, "the door head's corner beside it: " + besideOver);
		helper.assertTrue(!besideOver.getValue(FramedGlassBlock.UP), "a head along the top: " + besideOver);
		// A corner: a run going north from the west end, facing east (its panel on the west edge, in line with the
		// first run's west end): the first run's end block turns the corner, the new run carries the glass into it.
		BlockPos turn = framed(helper, 1, 1, 0, Direction.EAST);
		BlockPos end = run[1][1];
		FramedGlassBlock block = (FramedGlassBlock) at(helper, end).getBlock();
		helper.getLevel().setBlock(end, block.connected(at(helper, end), helper.getLevel(), end), Block.UPDATE_ALL);
		helper.getLevel().setBlock(turn, block.connected(at(helper, turn), helper.getLevel(), turn), Block.UPDATE_ALL);
		helper.assertTrue(at(helper, end).getValue(FramedGlassBlock.LEFT) == FramedGlassBlock.End.CORNER, "a corner post: " + at(helper, end));
		helper.assertTrue(at(helper, turn).getValue(FramedGlassBlock.RIGHT) == FramedGlassBlock.End.JOIN, "the turned run carries on into it: " + at(helper, turn));
		helper.succeed();
	}

	// ------------------------------------------------------------------ speed gates

	private static void stoneFloor(GameTestHelper helper) {
		for (int x = 0; x < 7; x++) {
			for (int z = 0; z < 7; z++) {
				helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE.defaultBlockState());
			}
		}
	}

	/** A speed gate placed as a player would, looking {@code facing} at (x, 1, z): the lane block and its pedestals. */
	private static BlockPos speedGate(GameTestHelper helper, int x, int z, Direction facing) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setGameMode(GameType.CREATIVE);
		BlockPos target = helper.absolutePos(new BlockPos(x, 1, z));
		player.setPos(target.getX() + 0.5 - facing.getStepX() * 3, target.getY(), target.getZ() + 0.5 - facing.getStepZ() * 3);
		player.setYRot(facing.toYRot());
		Block gate = PlantInterior.BLOCKS.get("speed_gate");
		ItemStack stack = new ItemStack(gate);
		player.setItemInHand(InteractionHand.MAIN_HAND, stack);
		BlockPos floor = target.below();
		BlockPlaceContext place = new BlockPlaceContext(player, InteractionHand.MAIN_HAND, stack,
				new BlockHitResult(Vec3.atCenterOf(floor).relative(Direction.UP, 0.5), Direction.UP, floor, false));
		BlockState state = gate.getStateForPlacement(place);
		helper.assertTrue(state != null && state.getValue(SpeedGateBlock.FACING) == facing, "a speed gate fits at " + x + ", " + z + " facing " + facing);
		helper.getLevel().setBlock(target, state, Block.UPDATE_ALL);
		gate.setPlacedBy(helper.getLevel(), target, state, player, stack);
		player.setGameMode(GameType.SPECTATOR);   // out of the gates' sight
		return target;
	}

	private static boolean open(GameTestHelper helper, BlockPos lane) {
		return helper.getLevel().getBlockState(lane).getValue(SpeedGateBlock.OPEN);
	}

	/** An entry lane and an exit lane side by side, as at the plants: three pedestals, the middle one shared. */
	@GameTest(maxTicks = 20)
	public void speedGatesShareTheirMiddlePedestal(GameTestHelper helper) {
		if (TestFocus.skip("nuclear")) {
			helper.succeed();
			return;
		}
		stoneFloor(helper);
		BlockPos entry = speedGate(helper, 2, 3, Direction.NORTH);
		// The exit lane fits beside it, its pedestal on that side already standing.
		BlockPos exit = speedGate(helper, 4, 2, Direction.SOUTH);
		Block pedestal = PlantInterior.BLOCKS.get("speed_gate_pedestal");
		for (int x : new int[] {1, 3, 5}) {
			for (int z = 2; z <= 3; z++) {
				BlockState at = helper.getBlockState(new BlockPos(x, 1, z));
				helper.assertTrue(at.is(pedestal) && at.getValue(SpeedGatePedestalBlock.AXIS) == Direction.Axis.Z
						&& at.getValue(SpeedGatePedestalBlock.HALF) == (z == 3 ? SpeedGatePedestalBlock.Half.B : SpeedGatePedestalBlock.Half.A),
						"a pedestal along the lanes at " + x + ", " + z + ": " + at);
			}
		}
		helper.assertTrue(helper.getBlockState(new BlockPos(2, 1, 2)).isAir() && helper.getBlockState(new BlockPos(4, 1, 3)).isAir(),
				"the way through each lane is left clear");
		// Their doors take the shared pedestal's two slots, each lane's in its own block, wherever the pair stands.
		ServerLevel level = helper.getLevel();
		helper.assertTrue(SpeedGateBlock.slotSign(level, entry, Direction.NORTH) != SpeedGateBlock.slotSign(level, exit, Direction.SOUTH),
				"the two lanes' doors run in different slots");
		helper.assertTrue(SpeedGateBlock.ownSlot(level, entry, Direction.NORTH) && SpeedGateBlock.ownSlot(level, exit, Direction.SOUTH),
				"each lane's doors run in its own block");
		// Shut, the doors bar the way where they are drawn (2 px behind the line), too high to jump.
		AABB doors = level.getBlockState(entry).getCollisionShape(level, entry).bounds();
		helper.assertTrue(doors.minZ > 0.05 && doors.maxZ < 0.2 && doors.maxY >= 1.5 && doors.minX <= 0 && doors.maxX >= 1,
				"the entry lane's shut doors: " + doors);
		helper.assertTrue(level.getBlockState(entry).setValue(SpeedGateBlock.OPEN, true).getCollisionShape(level, entry).isEmpty(),
				"open, nothing in the way");
		// Built turned (a blueprint placed another way round), each pedestal is still the one its lane wants there.
		for (Rotation rotation : Rotation.values()) {
			for (BlockPos cell : SpeedGateBlock.pedestals(entry, Direction.NORTH)) {
				BlockState turned = level.getBlockState(cell).rotate(rotation);
				BlockState want = SpeedGateBlock.pedestalState(cell.rotate(rotation), entry.rotate(rotation), rotation.rotate(Direction.NORTH));
				helper.assertTrue(turned.equals(want), rotation + ": " + turned + ", wanted " + want);
			}
		}
		helper.succeed();
	}

	/** The doors open for someone walking up to them the way through, stay open while they pass, and shut behind them. */
	@GameTest(maxTicks = 100)
	public void speedGateOpensOnlyTheWayThrough(GameTestHelper helper) {
		if (TestFocus.skip("nuclear")) {
			helper.succeed();
			return;
		}
		stoneFloor(helper);
		BlockPos lane = speedGate(helper, 2, 3, Direction.NORTH);
		ServerPlayer walker = helper.makeMockServerPlayerInLevel();
		walker.setGameMode(GameType.SURVIVAL);
		// The line is the lane block's north edge. Coming the wrong way, up to the doors from the north: they stay shut.
		walker.setPos(lane.getX() + 0.5, lane.getY(), lane.getZ() - 0.6);
		helper.runAfterDelay(10, () -> {
			helper.assertTrue(!open(helper, lane), "shut to someone coming the wrong way");
			walker.setPos(lane.getX() + 0.5, lane.getY(), lane.getZ() + 0.6);
		});
		helper.runAfterDelay(20, () -> {
			helper.assertTrue(open(helper, lane), "open for someone walking up the lane to them");
			walker.setPos(lane.getX() + 0.5, lane.getY(), lane.getZ() - 0.3);
		});
		helper.runAfterDelay(20 + SpeedGateBlockEntity.CLOSE_AFTER + 10, () -> {
			helper.assertTrue(open(helper, lane), "still open while someone is between the pedestals");
			walker.setPos(lane.getX() + 0.5, lane.getY(), lane.getZ() - 3.5);
		});
		helper.runAfterDelay(40 + 2 * SpeedGateBlockEntity.CLOSE_AFTER + 10, () -> {
			helper.assertTrue(!open(helper, lane), "shut again once the lane is clear");
			walker.setGameMode(GameType.SPECTATOR);
			helper.succeed();
		});
	}

	/** Taking a lane away takes the pedestals only it used; breaking a pedestal takes its lane, which drops one gate. */
	@GameTest(maxTicks = 20)
	public void speedGateComesDownWhole(GameTestHelper helper) {
		if (TestFocus.skip("nuclear")) {
			helper.succeed();
			return;
		}
		stoneFloor(helper);
		BlockPos entry = speedGate(helper, 2, 3, Direction.NORTH);
		BlockPos exit = speedGate(helper, 4, 2, Direction.SOUTH);
		ServerLevel level = helper.getLevel();
		Block pedestal = PlantInterior.BLOCKS.get("speed_gate_pedestal");
		level.destroyBlock(exit, false);
		for (int z = 2; z <= 3; z++) {
			helper.assertTrue(helper.getBlockState(new BlockPos(1, 1, z)).is(pedestal) && helper.getBlockState(new BlockPos(3, 1, z)).is(pedestal),
					"the entry lane keeps its pedestals, the shared one too (z " + z + ")");
			helper.assertTrue(helper.getBlockState(new BlockPos(5, 1, z)).isAir(), "the exit lane's own pedestal goes with it (z " + z + ")");
		}
		level.destroyBlock(helper.absolutePos(new BlockPos(1, 1, 2)), true);
		helper.assertTrue(level.getBlockState(entry).isAir(), "breaking a pedestal takes the lane it serves");
		for (int x : new int[] {1, 3}) {
			for (int z = 2; z <= 3; z++) {
				helper.assertTrue(helper.getBlockState(new BlockPos(x, 1, z)).isAir(), "and its other pedestals (" + x + ", " + z + ")");
			}
		}
		int gates = 0, others = 0;
		for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, new AABB(entry).inflate(4))) {
			if (item.getItem().is(PlantInterior.BLOCKS.get("speed_gate").asItem())) {
				gates += item.getItem().getCount();
			} else {
				others++;
			}
		}
		helper.assertTrue(gates == 1 && others == 0, "one speed gate dropped and nothing else: " + gates + " gates, " + others + " others");
		helper.succeed();
	}
}
