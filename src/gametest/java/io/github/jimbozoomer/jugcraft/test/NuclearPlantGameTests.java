package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.blueprint.Blueprint;
import io.github.jimbozoomer.jugcraft.blueprint.BuildSite;
import io.github.jimbozoomer.jugcraft.nuclear.CoolingTowerBlockEntity;
import io.github.jimbozoomer.jugcraft.nuclear.DamperBlock;
import io.github.jimbozoomer.jugcraft.nuclear.EquipmentBlockEntity;
import io.github.jimbozoomer.jugcraft.nuclear.EquipmentKind;
import io.github.jimbozoomer.jugcraft.nuclear.FramedGlassBlock;
import io.github.jimbozoomer.jugcraft.nuclear.HvacRoom;
import io.github.jimbozoomer.jugcraft.nuclear.JugcraftNuclear;
import io.github.jimbozoomer.jugcraft.nuclear.PlantInterior;
import io.github.jimbozoomer.jugcraft.nuclear.PlantBuilder;
import io.github.jimbozoomer.jugcraft.nuclear.ReactorCore;
import io.github.jimbozoomer.jugcraft.nuclear.ReactorUnit;
import io.github.jimbozoomer.jugcraft.nuclear.RelayRoom;
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
	private static final String[][] PLANTS = {{"nuclear_small_modular", "118096"}, {"nuclear_medium_pwr", "448419"}, {"nuclear_large_epr", "383337"}};

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
		// Reserve a separate site: the relay-room test also builds a full plant at its origin + (5000, 0, -5000).
		// Relative test origins are close enough for those hundreds-of-blocks-wide buildings to overlap.
		BlockPos largeStake = new BlockPos(8_000, origin.getY(), -8_000);
		PlantBuilder.Result large = PlantBuilder.build(level, largePrint, largeStake, Rotation.NONE, null, true);
		surveyAtFullPower(helper, large, "large plant", largePrint, largeStake);
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
		helper.assertTrue(!cores.isEmpty(), plant + ": reactors found at " + built.vessels() + "; blocks=" + built.vessels().stream().map(p -> level.getBlockState(p).toString() + "/" + level.getBlockEntity(p)).toList());
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
		// Shut, the doors bar the way at the line (their slot 0.6 px behind it), 1.67 high: too high to jump.
		AABB doors = level.getBlockState(entry).getCollisionShape(level, entry).bounds();
		helper.assertTrue(doors.minZ < 0.01 && doors.maxZ < 0.1 && doors.maxY >= 1.6 && doors.minX <= 0 && doors.maxX >= 1,
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

	/**
	 * Round 25 (Justin: "make sure they actually hold charge in game, I want it to actually work"): in a built medium
	 * plant the main transformer feeds the control building's two load centres by its buried cable (and no other), each
	 * load centre's DC MCC breaker feeds its train's motor control centre, whose CHGR 1 and CHGR 2 breakers each feed one
	 * of the train's battery chargers (round 26), the chargers charge the eight station battery racks (kept even by their
	 * switchboards: round 26, #9), and opening the
	 * breakers stops it while everything keeps its charge; each vital inverter's regulating transformer reads its MCC's
	 * REG XF breaker (round 26, 8d). The pieces are ticked by hand, in
	 * the order the server ticks them.
	 */
	@GameTest(maxTicks = 40)
	public void mediumPlantPowerReachesTheBatteries(GameTestHelper helper) {
		if (TestFocus.skip("nuclear")) {
			helper.succeed();
			return;
		}
		var level = helper.getLevel();
		// Where a breaker's door is, as the game finds it from a click (the main, an upper and a lower feeder).
		helper.assertTrue(io.github.jimbozoomer.jugcraft.nuclear.Switchgear.breakerAt(2.48, 0.9, 0.1) == 0, "main breaker's door");
		double w = (7.96 - 2.98) / 5;
		helper.assertTrue(io.github.jimbozoomer.jugcraft.nuclear.Switchgear.breakerAt(2.98 + 2.5 * w, 0.6, 0.1) == 6, "DC MCC breaker's door");
		helper.assertTrue(io.github.jimbozoomer.jugcraft.nuclear.Switchgear.breakerAt(2.98 + 2.5 * w, 1.5, 0.1) == 5, "INVERTER breaker's door");
		helper.assertTrue(io.github.jimbozoomer.jugcraft.nuclear.Switchgear.breakerAt(1.0, 1.0, 0.1) == -1, "the transformer section has no breaker");
		// The motor control centre's unit doors (round 26): the main, CHGR 1, DC AHU; the incoming cover has none.
		helper.assertTrue(io.github.jimbozoomer.jugcraft.nuclear.MotorControlCenter.breakerAt(0.4, 1.5, 0.05) == 0, "MCC main breaker's door");
		helper.assertTrue(io.github.jimbozoomer.jugcraft.nuclear.MotorControlCenter.breakerAt(2.4, 1.6, 0.05) == 4, "MCC CHGR 1 door");
		helper.assertTrue(io.github.jimbozoomer.jugcraft.nuclear.MotorControlCenter.breakerAt(3.4, 1.0, 0.05) == 7, "MCC DC AHU door");
		helper.assertTrue(io.github.jimbozoomer.jugcraft.nuclear.MotorControlCenter.breakerAt(0.4, 0.5, 0.05) == -1, "the incoming cover has no breaker");
		BlockPos stake = helper.absolutePos(BlockPos.ZERO).offset(-5_000, 0, -5_000);
		Blueprint print = Blueprint.get("nuclear_medium_pwr", false);
		PlantBuilder.build(level, print, stake, Rotation.NONE, null, true);
		java.util.List<EquipmentBlockEntity> gear = new java.util.ArrayList<>(), mccs = new java.util.ArrayList<>();
		java.util.List<io.github.jimbozoomer.jugcraft.nuclear.StationBatteryBlockEntity> banks = new java.util.ArrayList<>();
		java.util.List<io.github.jimbozoomer.jugcraft.nuclear.LivePanelBlockEntity> chargers = new java.util.ArrayList<>();
		java.util.List<io.github.jimbozoomer.jugcraft.nuclear.LivePanelBlockEntity> inverters = new java.util.ArrayList<>();
		java.util.List<io.github.jimbozoomer.jugcraft.nuclear.LivePanelBlockEntity> regXfs = new java.util.ArrayList<>();
		java.util.List<io.github.jimbozoomer.jugcraft.nuclear.LivePanelBlockEntity> vitals = new java.util.ArrayList<>();
		java.util.List<io.github.jimbozoomer.jugcraft.nuclear.LivePanelBlockEntity> detectors = new java.util.ArrayList<>(), boards = new java.util.ArrayList<>();
		java.util.List<io.github.jimbozoomer.jugcraft.nuclear.LivePanelBlockEntity> fans = new java.util.ArrayList<>(), monitors = new java.util.ArrayList<>();
		java.util.List<BlockPos> eyewashes = new java.util.ArrayList<>();
		EquipmentBlockEntity transformer = null;
		for (int i = 0; i < print.size(); i++) {
			BlockState state = print.state(i, Rotation.NONE);
			BlockPos pos = stake.offset(print.offset(i, Rotation.NONE));
			if (state.getBlock() instanceof io.github.jimbozoomer.jugcraft.nuclear.EquipmentAnchorBlock anchor) {
				if (level.getBlockEntity(pos) instanceof EquipmentBlockEntity e) {
					if (anchor.kind == EquipmentKind.SWITCHGEAR_LINEUP) {
						gear.add(e);
					} else if (anchor.kind == EquipmentKind.MCC_LINEUP) {
						mccs.add(e);
					} else if (anchor.kind == EquipmentKind.MAIN_TRANSFORMER) {
						transformer = e;
					}
				}
			} else if (state.getBlock() instanceof io.github.jimbozoomer.jugcraft.nuclear.StationBatteryBlock) {
				var bank = io.github.jimbozoomer.jugcraft.nuclear.StationBatteryBlock.battery(level, pos);
				if (bank != null && !banks.contains(bank)) {
					banks.add(bank);
				}
			} else if (state.is(PlantInterior.BLOCKS.get("vital_inverter"))
					&& level.getBlockEntity(pos) instanceof io.github.jimbozoomer.jugcraft.nuclear.LivePanelBlockEntity inv) {
				inverters.add(inv);
			} else if (state.is(PlantInterior.BLOCKS.get("reg_transformer"))
					&& level.getBlockEntity(pos) instanceof io.github.jimbozoomer.jugcraft.nuclear.LivePanelBlockEntity xf) {
				regXfs.add(xf);
			} else if (state.is(PlantInterior.BLOCKS.get("vital_panel"))
					&& level.getBlockEntity(pos) instanceof io.github.jimbozoomer.jugcraft.nuclear.LivePanelBlockEntity vb) {
				vitals.add(vb);
			} else if (state.is(PlantInterior.BLOCKS.get("dc_ground_detector"))
					&& level.getBlockEntity(pos) instanceof io.github.jimbozoomer.jugcraft.nuclear.LivePanelBlockEntity gd) {
				detectors.add(gd);
			} else if (state.is(PlantInterior.BLOCKS.get("dc_switchboard"))
					&& level.getBlockEntity(pos) instanceof io.github.jimbozoomer.jugcraft.nuclear.LivePanelBlockEntity board) {
				boards.add(board);
			} else if (state.is(PlantInterior.BLOCKS.get("exhaust_fan")) && level.getBlockEntity(pos) instanceof io.github.jimbozoomer.jugcraft.nuclear.LivePanelBlockEntity fan) {
				fans.add(fan);
			} else if (state.is(PlantInterior.BLOCKS.get("h2_monitor")) && level.getBlockEntity(pos) instanceof io.github.jimbozoomer.jugcraft.nuclear.LivePanelBlockEntity monitor) {
				monitors.add(monitor);
			} else if (state.getBlock() instanceof io.github.jimbozoomer.jugcraft.nuclear.EyewashBlock) {
				eyewashes.add(pos);
			} else if ((state.is(PlantInterior.BLOCKS.get("edg_battery_charger")) || state.is(PlantInterior.BLOCKS.get("station_charger")))
					&& level.getBlockEntity(pos) instanceof io.github.jimbozoomer.jugcraft.nuclear.LivePanelBlockEntity panel && panel.input != null) {
				chargers.add(panel);
			}
		}
		helper.assertTrue(transformer != null && transformer.output != null, "the main transformer");
		// (Round 26, #9: two strings of two racks in each battery room.)
		helper.assertTrue(banks.size() == 8 && chargers.size() == 4, "eight station battery racks and four chargers: " + banks.size() + ", " + chargers.size());
		helper.assertTrue(mccs.size() == 2, "a motor control centre in each DC equipment room: " + mccs.size());
		// Round 26 (8g, 8d): each vital inverter finds its battery and its bypass, the regulating transformer beside it with
		// its own label (charger, inverter, transformer in a row for each number); each transformer finds its MCC and
		// takes the REG XF breaker its label says (1A-1 and 1B-1 REG XF 1, 1A-2 and 1B-2 REG XF 2), so the two by each MCC
		// are its train's, fed by its REG XF 1 and 2, and each backs the inverter that links it.
		helper.assertTrue(inverters.size() == 4 && regXfs.size() == 4, "four vital inverters and four regulating transformers: " + inverters.size()
				+ ", " + regXfs.size());
		helper.assertTrue(vitals.size() == 4, "four 120 VAC vital panels: " + vitals.size());
		helper.assertTrue(detectors.size() == 2 && boards.size() == 2, "a ground detector and a switchboard in each DC room: " + detectors.size() + ", "
				+ boards.size());
		for (int t = 0; t < 90; t++) {
			for (var inv : inverters) inv.serverTick(level);
			for (var xf : regXfs) xf.serverTick(level);
			for (var vb : vitals) vb.serverTick(level);
			for (var gd : detectors) gd.serverTick(level);
			for (var board : boards) board.serverTick(level);
		}
		// Round 26 (8f): each room's ground detector reads its train's battery, the one its switchboard is on, and is never
		// on a diesel generator; holding a test button grounds that side for as long as it is held (each use holds it
		// HOLD ticks), and the buttons are where the drawing has them.
		for (var gd : detectors) {
			int label = io.github.jimbozoomer.jugcraft.nuclear.LivePanelBlockEntity.variant(gd.getBlockState());
			var board = boards.stream().filter(b -> io.github.jimbozoomer.jugcraft.nuclear.LivePanelBlockEntity.variant(b.getBlockState()) == label).findFirst();
			helper.assertTrue(gd.battery != null && board.isPresent() && gd.battery.equals(board.get().battery) && gd.generator == null,
					"a ground detector reads its train's battery, as its switchboard does: " + gd.getBlockPos().subtract(stake).toShortString());
		}
		helper.assertTrue(io.github.jimbozoomer.jugcraft.nuclear.GroundDetectorBlock.testAt(3.9, 5.7) == 1
				&& io.github.jimbozoomer.jugcraft.nuclear.GroundDetectorBlock.testAt(10.1, 5.7) == 2
				&& io.github.jimbozoomer.jugcraft.nuclear.GroundDetectorBlock.testAt(7.0, 5.7) == 0, "the ground detector's test buttons");
		var gd0 = detectors.get(0);
		long now = level.getGameTime();
		helper.assertTrue(!gd0.testing(now) && io.github.jimbozoomer.jugcraft.nuclear.GroundDetectorBlock.press(level, gd0, 2, now) && gd0.testing(now)
				&& gd0.testSide == 2 && !io.github.jimbozoomer.jugcraft.nuclear.GroundDetectorBlock.press(level, gd0, 2, now + 4)
				&& gd0.testing(now + 4 + io.github.jimbozoomer.jugcraft.nuclear.GroundDetectorBlock.HOLD - 1)
				&& !gd0.testing(now + 4 + io.github.jimbozoomer.jugcraft.nuclear.GroundDetectorBlock.HOLD), "a held test button grounds its side while held");
		// Round 26 (#9): each switchboard has its own room's four racks, two strings in parallel, and the ground detector
		// its switchboard's (its MENU steps through them); each room's exhaust fan starts on AUTO and its selector turns
		// HAND, OFF, AUTO; each hydrogen monitor keeps to its room's fan; an eyewash pushed runs.
		for (var board : boards) {
			String at = board.getBlockPos().subtract(stake).toShortString();
			helper.assertTrue(board.racks.size() == 4 && io.github.jimbozoomer.jugcraft.nuclear.LivePanelBlockEntity.strings(level, board.racks).size() == 2,
					"a switchboard has its room's four racks, two strings: " + at + " " + board.racks.size());
			for (var gd : detectors) {
				if (io.github.jimbozoomer.jugcraft.nuclear.LivePanelBlockEntity.variant(gd.getBlockState()) == io.github.jimbozoomer.jugcraft.nuclear.LivePanelBlockEntity.variant(board.getBlockState())) {
					helper.assertTrue(gd.racks.equals(board.racks), "a ground detector reads its switchboard's racks: " + at);
				}
			}
		}
		helper.assertTrue(io.github.jimbozoomer.jugcraft.nuclear.GroundDetectorBlock.menuAt(2.5, 18.95) && !io.github.jimbozoomer.jugcraft.nuclear.GroundDetectorBlock.menuAt(6.6, 18.95), "the battery monitor's MENU key");
		helper.assertTrue(fans.size() == 2 && monitors.size() == 2 && eyewashes.size() == 2, "a fan, a hydrogen monitor and an eyewash in each battery room: "
				+ fans.size() + ", " + monitors.size() + ", " + eyewashes.size());
		helper.assertTrue(io.github.jimbozoomer.jugcraft.nuclear.ExhaustFanBlock.selectorAt(8.0, 8.2) && !io.github.jimbozoomer.jugcraft.nuclear.ExhaustFanBlock.selectorAt(8.0, 4.6), "the exhaust fan's selector");
		for (var fan : fans) {
			helper.assertTrue(fan.mode == io.github.jimbozoomer.jugcraft.nuclear.ExhaustFanBlock.AUTO, "an exhaust fan starts on AUTO");
			int[] seen = new int[3];
			for (int k = 0; k < 3; k++) {
				io.github.jimbozoomer.jugcraft.nuclear.ExhaustFanBlock.turn(level, fan);
				seen[k] = fan.mode;
			}
			helper.assertTrue(seen[0] == io.github.jimbozoomer.jugcraft.nuclear.ExhaustFanBlock.HAND && seen[1] == io.github.jimbozoomer.jugcraft.nuclear.ExhaustFanBlock.OFF && seen[2] == io.github.jimbozoomer.jugcraft.nuclear.ExhaustFanBlock.AUTO,
					"the fan's selector turns HAND, OFF, AUTO: " + java.util.Arrays.toString(seen));
		}
		// (25 October 2026: the battery rooms only look live; the monitor's fan and racks are worked out where it is drawn.)
		for (var monitor : monitors) {
			var fan = io.github.jimbozoomer.jugcraft.nuclear.LivePanelBlockEntity.batteryRoomPartner(level, monitor);
			helper.assertTrue(fan != null && fans.contains(fan)
					&& io.github.jimbozoomer.jugcraft.nuclear.LivePanelBlockEntity.batteryRoomRacks(level, monitor).size() == 4,
					"a hydrogen monitor keeps to its room's fan and battery: " + monitor.getBlockPos().subtract(stake).toShortString());
		}
		for (BlockPos ew : eyewashes) {
			var block = (io.github.jimbozoomer.jugcraft.nuclear.EyewashBlock) level.getBlockState(ew).getBlock();
			block.start(level, ew);
			helper.assertTrue(io.github.jimbozoomer.jugcraft.nuclear.EyewashBlock.runningUntil(level, ew) == level.getGameTime() + io.github.jimbozoomer.jugcraft.nuclear.EyewashBlock.RUN, "an eyewash pushed runs");
		}
		// Round 26 (8e): each inverter feeds the vital panel of its label and each panel knows it; a panel starts with its
		// main and circuits 1 to 10 closed (the spares open), which draw the inverter's 46 A; a circuit opened takes its
		// share off, the main opened all of it.
		for (var vb : vitals) {
			int label = io.github.jimbozoomer.jugcraft.nuclear.LivePanelBlockEntity.variant(vb.getBlockState());
			String at = vb.getBlockPos().subtract(stake).toShortString();
			helper.assertTrue(vb.inverter != null && level.getBlockEntity(vb.inverter) instanceof io.github.jimbozoomer.jugcraft.nuclear.LivePanelBlockEntity inv
					&& io.github.jimbozoomer.jugcraft.nuclear.LivePanelBlockEntity.variant(inv.getBlockState()) == label && vb.getBlockPos().equals(inv.panel),
					"a vital panel and the inverter of its label link each other: " + at);
			helper.assertTrue(vb.breakers == io.github.jimbozoomer.jugcraft.nuclear.VitalPanelBlock.DEFAULT_BREAKERS
					&& io.github.jimbozoomer.jugcraft.nuclear.VitalPanelBlock.load(vb.breakers) == 46, "a vital panel's breakers as built: " + at);
		}
		int full = io.github.jimbozoomer.jugcraft.nuclear.VitalPanelBlock.DEFAULT_BREAKERS;
		helper.assertTrue(io.github.jimbozoomer.jugcraft.nuclear.VitalPanelBlock.load(full & ~(1 << 3)) == 41
				&& io.github.jimbozoomer.jugcraft.nuclear.VitalPanelBlock.load(full & ~1) == 0, "a vital panel's circuits carry the load");
		for (var inv : inverters) {
			int label = io.github.jimbozoomer.jugcraft.nuclear.LivePanelBlockEntity.variant(inv.getBlockState());
			helper.assertTrue(inv.battery != null && inv.xf != null && inv.xf.distManhattan(inv.getBlockPos()) == 1
					&& level.getBlockEntity(inv.xf) instanceof io.github.jimbozoomer.jugcraft.nuclear.LivePanelBlockEntity xf
					&& io.github.jimbozoomer.jugcraft.nuclear.LivePanelBlockEntity.isTransformer(xf.getBlockState())
					&& io.github.jimbozoomer.jugcraft.nuclear.LivePanelBlockEntity.variant(xf.getBlockState()) == label,
					"an inverter links its battery and the transformer beside it, of its own label: " + inv.getBlockPos().subtract(stake).toShortString()
							+ " (label " + label + ") -> " + (inv.xf == null ? "none" : inv.xf.subtract(stake).toShortString()));
		}
		java.util.Map<BlockPos, java.util.Set<Integer>> fedBy = new java.util.HashMap<>(), trains = new java.util.HashMap<>();
		for (var xf : regXfs) {
			int label = io.github.jimbozoomer.jugcraft.nuclear.LivePanelBlockEntity.variant(xf.getBlockState());
			String at = xf.getBlockPos().subtract(stake).toShortString();
			helper.assertTrue(xf.mcc != null && xf.regXf == label % 2 + 1, "a transformer links its MCC and the REG XF its label says: " + at + " (label "
					+ label + ") REG XF " + xf.regXf);
			helper.assertTrue(xf.inverter != null && level.getBlockEntity(xf.inverter) instanceof io.github.jimbozoomer.jugcraft.nuclear.LivePanelBlockEntity inv
					&& xf.getBlockPos().equals(inv.xf), "a transformer backs the inverter that links it: " + at);
			helper.assertTrue(xf.energized(level), "with its MCC full and its REG XF closed a transformer is alive: " + at);
			fedBy.computeIfAbsent(xf.mcc, k -> new java.util.HashSet<>()).add(xf.regXf);
			trains.computeIfAbsent(xf.mcc, k -> new java.util.HashSet<>()).add(label / 2);
		}
		helper.assertTrue(fedBy.size() == 2 && fedBy.values().stream().allMatch(v -> v.equals(java.util.Set.of(1, 2)))
				&& trains.values().stream().allMatch(v -> v.size() == 1),
				"the two transformers by each MCC are its train's, fed by its REG XF 1 and 2: " + fedBy + ", trains " + trains);
		// The chain (8d): one MCC's REG XF 1 opened, the transformer it feeds goes dead and no other; then closed again.
		EquipmentBlockEntity opened = mccs.get(0);
		opened.breakers &= ~(1 << 1);
		for (var xf : regXfs) {
			boolean cut = opened.getBlockPos().equals(xf.mcc) && xf.regXf == 1;
			helper.assertTrue(xf.energized(level) != cut, "REG XF 1 open, only the transformer it feeds is dead: " + xf.getBlockPos().subtract(stake).toShortString()
					+ (cut ? " (fed from it) is alive" : " is dead"));
		}
		opened.breakers |= 1 << 1;
		long cap = io.github.jimbozoomer.jugcraft.nuclear.Switchgear.CAPACITY;
		for (EquipmentBlockEntity g : gear) {
			helper.assertTrue(g.bus != null && g.bus.getAmount() == cap, "a fuelled build's load centres start full");
		}
		for (EquipmentBlockEntity m : mccs) {
			helper.assertTrue(m.bus != null && m.bus.getAmount() == io.github.jimbozoomer.jugcraft.nuclear.MotorControlCenter.CAPACITY,
					"a fuelled build's motor control centres start full");
		}
		for (var bank : banks) {
			helper.assertTrue(bank.charge() == 1.0, "a fuelled build's batteries start full");
		}
		// 1. The transformer's output reaches the switchgear room's two load centres, and only them.
		for (EquipmentBlockEntity g : gear) {
			g.bus.setAmount(0);
		}
		for (EquipmentBlockEntity m : mccs) {
			m.bus.setAmount(0);
		}
		transformer.output.setAmount(1_000_000);
		for (int t = 0; t < 10; t++) {
			transformer.serverTick(level);
		}
		int fed = 0;
		for (EquipmentBlockEntity g : gear) {
			fed += g.bus.getAmount() > 0 ? 1 : 0;
		}
		helper.assertTrue(fed == 2, "the transformer feeds the two load centres it is cabled to: " + fed + " of " + gear.size());
		for (EquipmentBlockEntity m : mccs) {
			helper.assertTrue(m.bus.getAmount() == 0, "the transformer does not reach the motor control centres directly");
		}
		// (Round 26, 8d: with their MCCs' buses empty the regulating transformers are dead, their breakers closed or not.)
		for (var xf : regXfs) {
			helper.assertTrue(!xf.energized(level), "a regulating transformer on an empty MCC is dead: " + xf.getBlockPos().subtract(stake).toShortString());
		}
		// 2. Full load centres charge the empty batteries through the motor control centres (DC MCC breaker, MCC, its
		// CHGR 1 and CHGR 2 breakers) and the chargers.
		for (EquipmentBlockEntity g : gear) {
			g.bus.setAmount(cap);
		}
		for (var bank : banks) {
			bank.energy.setAmount(0);
		}
		Runnable tick = () -> {
			for (EquipmentBlockEntity g : gear) g.serverTick(level);
			for (EquipmentBlockEntity m : mccs) m.serverTick(level);
			for (var c : chargers) c.serverTick(level);
			for (var board : boards) board.serverTick(level);
		};
		for (int t = 0; t < 200; t++) {
			tick.run();
		}
		java.util.Set<BlockPos> linked = new java.util.HashSet<>();
		for (var c : chargers) {
			helper.assertTrue(c.battery != null && linked.add(c.battery), "each charger charges a battery of its own: " + c.battery);
		}
		// (Round 26, #9: each charger charges one rack; the switchboard keeps its four racks even, so all of them charge.)
		for (var bank : banks) {
			helper.assertTrue(bank.energy.getAmount() > 50_000, "the battery at " + bank.getBlockPos().subtract(stake).toShortString() + " charges: "
					+ bank.energy.getAmount());
		}
		for (var board : boards) {
			long lo = Long.MAX_VALUE, hi = 0;
			for (BlockPos at : board.racks) {
				long e = ((io.github.jimbozoomer.jugcraft.nuclear.StationBatteryBlockEntity) level.getBlockEntity(at)).energy.getAmount();
				lo = Math.min(lo, e);
				hi = Math.max(hi, e);
			}
			helper.assertTrue(hi - lo < 25_000, "a switchboard keeps its racks even: " + lo + " to " + hi);
		}
		for (EquipmentBlockEntity m : mccs) {
			helper.assertTrue(m.flows[0] > 0 && m.flows[4] > 0 && m.flows[6] > 0, "each MCC takes power in and feeds both its chargers: "
					+ m.flows[0] + ", " + m.flows[4] + ", " + m.flows[6]);
		}
		// 3. With the MCCs' CHGR breakers open the chargers run down their buffers and stop; the batteries keep their
		// charge.
		for (EquipmentBlockEntity m : mccs) {
			m.breakers &= ~(1 << 4 | 1 << 6);
		}
		for (int t = 0; t < 60; t++) {
			tick.run();
		}
		long[] held = banks.stream().mapToLong(b -> b.energy.getAmount()).toArray();
		for (int t = 0; t < 40; t++) {
			tick.run();
		}
		for (int k = 0; k < banks.size(); k++) {
			helper.assertTrue(banks.get(k).energy.getAmount() == held[k], "the breaker open, the battery holds: " + held[k] + " -> " + banks.get(k).energy.getAmount());
		}
		for (EquipmentBlockEntity m : mccs) {
			helper.assertTrue(m.flows[4] == 0 && m.flows[6] == 0, "an open breaker's feeder carries nothing");
		}
		helper.succeed();
	}

	/**
	 * Older rooms round 3 (Justin: the switchboard "Real 3D, breakers work", "Charger switches", "Inverter bypass switch",
	 * "Annunciator buttons", "Log desk laptop"; feeders "Cut what it feeds", equalize "24 game hours", alarms "Lit until
	 * RESET"): in a built medium plant each DC room's chargers, inverters and distribution panels find their train's
	 * switchboard, which finds them; its bus is alive on its battery; a feeder opened cuts what it feeds (an inverter onto
	 * its bypass, a distribution panel dead, the load centre's control power, the diesel's start), and closed brings it
	 * back; a charger's breakers stop it, its equalize runs out after 24 game hours; the annunciators latch, acknowledge
	 * and reset; the laptop writes the room; the clicks land where the drawings have the breakers and buttons.
	 */
	@GameTest(maxTicks = 40)
	public void mediumPlantDcRoomsWork(GameTestHelper helper) {
		if (TestFocus.skip("nuclear")) {
			helper.succeed();
			return;
		}
		var level = helper.getLevel();
		// Where the clicks land (tools/dc_room.py sb_hits, tools/plant_panels.py station_charger and vital_inverter).
		helper.assertTrue(io.github.jimbozoomer.jugcraft.nuclear.DcSwitchboardBlock.breakerAt(8.0, 10.6) == 0
				&& io.github.jimbozoomer.jugcraft.nuclear.DcSwitchboardBlock.breakerAt(20.0, 26.3) == 1
				&& io.github.jimbozoomer.jugcraft.nuclear.DcSwitchboardBlock.breakerAt(27.6, 26.3) == 2
				&& io.github.jimbozoomer.jugcraft.nuclear.DcSwitchboardBlock.breakerAt(27.6, 26.0 - 3.55 * 5 + 0.3) == 12
				&& io.github.jimbozoomer.jugcraft.nuclear.DcSwitchboardBlock.breakerAt(24.0, 20.0) == -1, "the switchboard's breakers");
		helper.assertTrue(io.github.jimbozoomer.jugcraft.nuclear.StationChargerBlock.breakerAt(4.5, 6.0) == 0
				&& io.github.jimbozoomer.jugcraft.nuclear.StationChargerBlock.breakerAt(11.5, 6.0) == 1
				&& io.github.jimbozoomer.jugcraft.nuclear.StationChargerBlock.modeAt(3.4, 10.0)
				&& io.github.jimbozoomer.jugcraft.nuclear.StationChargerBlock.buttonAt(12.1, 16.55) == 0
				&& io.github.jimbozoomer.jugcraft.nuclear.StationChargerBlock.buttonAt(12.1, 12.95) == 2
				&& io.github.jimbozoomer.jugcraft.nuclear.StationChargerBlock.buttonAt(8.0, 25.0) == -1, "the charger's breakers, MODE and buttons");
		helper.assertTrue(io.github.jimbozoomer.jugcraft.nuclear.VitalInverterBlock.breakerAt(11.5, 6.0)
				&& io.github.jimbozoomer.jugcraft.nuclear.VitalInverterBlock.bypassAt(4.0, 6.1)
				&& io.github.jimbozoomer.jugcraft.nuclear.VitalInverterBlock.buttonAt(12.1, 11.9) == 0
				&& io.github.jimbozoomer.jugcraft.nuclear.VitalInverterBlock.buttonAt(12.1, 8.7) == 2, "the inverter's breaker, bypass switch and buttons");
		BlockPos stake = helper.absolutePos(BlockPos.ZERO).offset(-9_000, 0, 15_000);
		Blueprint print = Blueprint.get("nuclear_medium_pwr", false);
		PlantBuilder.build(level, print, stake, Rotation.NONE, null, true);
		java.util.List<io.github.jimbozoomer.jugcraft.nuclear.LivePanelBlockEntity> boards = new java.util.ArrayList<>(), chargers = new java.util.ArrayList<>(),
				inverters = new java.util.ArrayList<>(), panels = new java.util.ArrayList<>(), laptops = new java.util.ArrayList<>(), others = new java.util.ArrayList<>(),
				disconnects = new java.util.ArrayList<>();
		java.util.List<EquipmentBlockEntity> gear = new java.util.ArrayList<>(), mccs = new java.util.ArrayList<>();
		for (int i = 0; i < print.size(); i++) {
			BlockState state = print.state(i, Rotation.NONE);
			BlockPos pos = stake.offset(print.offset(i, Rotation.NONE));
			if (state.getBlock() instanceof io.github.jimbozoomer.jugcraft.nuclear.EquipmentAnchorBlock anchor && level.getBlockEntity(pos) instanceof EquipmentBlockEntity e) {
				if (anchor.kind == EquipmentKind.SWITCHGEAR_LINEUP) {
					gear.add(e);
				} else if (anchor.kind == EquipmentKind.MCC_LINEUP) {
					mccs.add(e);
				}
			} else if (level.getBlockEntity(pos) instanceof io.github.jimbozoomer.jugcraft.nuclear.LivePanelBlockEntity p) {
				if (state.is(PlantInterior.BLOCKS.get("dc_switchboard"))) {
					boards.add(p);
				} else if (state.is(PlantInterior.BLOCKS.get("station_charger"))) {
					chargers.add(p);
				} else if (state.is(PlantInterior.BLOCKS.get("vital_inverter"))) {
					inverters.add(p);
				} else if (state.is(PlantInterior.BLOCKS.get("dc_dist_panel"))) {
					panels.add(p);
				} else if (state.is(PlantInterior.BLOCKS.get("dc_desk_laptop"))) {
					laptops.add(p);
				} else if (state.is(PlantInterior.BLOCKS.get("battery_disconnect"))) {
					disconnects.add(p);
				} else if (state.is(PlantInterior.BLOCKS.get("reg_transformer")) || state.is(PlantInterior.BLOCKS.get("vital_panel"))
						|| state.is(PlantInterior.BLOCKS.get("dc_ground_detector"))) {
					others.add(p);
				}
			}
		}
		helper.assertTrue(boards.size() == 2 && chargers.size() == 4 && inverters.size() == 4 && panels.size() == 4 && laptops.size() == 2,
				"two switchboards, four chargers, four inverters, four DC panels, two laptops: " + boards.size() + ", " + chargers.size() + ", "
						+ inverters.size() + ", " + panels.size() + ", " + laptops.size());
		for (EquipmentBlockEntity m : mccs) {
			m.bus.setAmount(io.github.jimbozoomer.jugcraft.nuclear.MotorControlCenter.CAPACITY);
		}
		java.util.List<io.github.jimbozoomer.jugcraft.nuclear.LivePanelBlockEntity> all = new java.util.ArrayList<>();
		all.addAll(others);
		all.addAll(chargers);
		all.addAll(inverters);
		all.addAll(panels);
		all.addAll(laptops);
		all.addAll(disconnects);
		all.addAll(boards);
		Runnable tick = () -> {
			for (var p : all) p.serverTick(level);
		};
		for (int t = 0; t < 90; t++) {
			tick.run();
		}
		for (var c : chargers) {
			// (Their supplies on: in one game tick their AC isn't counted, which it is every two seconds.)
			c.ac = true;
		}
		for (int t = 0; t < 21; t++) {
			tick.run();
		}
		// Each piece finds its train's switchboard, which has them all by number; its bus is alive.
		for (var list : java.util.List.of(chargers, inverters, panels)) {
			for (var p : list) {
				var board = io.github.jimbozoomer.jugcraft.nuclear.DcRoom.board(level, p);
				helper.assertTrue(board != null && io.github.jimbozoomer.jugcraft.nuclear.DcRoom.train(board.getBlockState())
						== io.github.jimbozoomer.jugcraft.nuclear.DcRoom.train(p.getBlockState()) && board.dcParts.contains(p.getBlockPos()),
						"a DC room piece and its train's switchboard find each other: " + p.getBlockPos().subtract(stake).toShortString());
			}
		}
		var boardA = boards.stream().filter(b -> io.github.jimbozoomer.jugcraft.nuclear.DcRoom.train(b.getBlockState()) == 0).findFirst().orElseThrow();
		for (var board : boards) {
			helper.assertTrue(board.breakers == io.github.jimbozoomer.jugcraft.nuclear.DcRoom.SB_DEFAULT
					&& (board.sbFlags & io.github.jimbozoomer.jugcraft.nuclear.DcRoom.BUS) != 0
					&& (board.sbFlags & io.github.jimbozoomer.jugcraft.nuclear.DcRoom.BATT) != 0
					&& (board.sbFlags & io.github.jimbozoomer.jugcraft.nuclear.DcRoom.CHARGING) != 0, "a switchboard as built: its bus alive on its battery "
							+ "and chargers: " + board.sbFlags);
			double v = io.github.jimbozoomer.jugcraft.nuclear.DcRoom.busVolts(level, board);
			helper.assertTrue(Math.abs(v - io.github.jimbozoomer.jugcraft.nuclear.DcRoom.FLOAT_V) < 0.5, "floating, the bus at 132 V: " + v);
		}
		var inv = inverters.stream().filter(i -> io.github.jimbozoomer.jugcraft.nuclear.LivePanelBlockEntity.variant(i.getBlockState()) == 0).findFirst().orElseThrow();
		var other = inverters.stream().filter(i -> io.github.jimbozoomer.jugcraft.nuclear.LivePanelBlockEntity.variant(i.getBlockState()) == 1).findFirst().orElseThrow();
		helper.assertTrue(io.github.jimbozoomer.jugcraft.nuclear.DcRoom.inverter(level, inv).onInv(), "inverter 1A-1 carries its load on its DC");
		// Feeder 01 opened: inverter 1A-1 loses its DC and its static switch puts the load on the bypass (1A-2 carries on);
		// INV FAIL comes in flashing, goes steady on ACK, stays lit once cleared until RESET.
		boardA.breakers &= ~(1 << io.github.jimbozoomer.jugcraft.nuclear.DcRoom.INV1);
		io.github.jimbozoomer.jugcraft.nuclear.DcRoom.tickBoard(level, boardA);
		var s1 = io.github.jimbozoomer.jugcraft.nuclear.DcRoom.inverter(level, inv);
		helper.assertTrue(!s1.dc() && !s1.onInv() && s1.onBypass() && io.github.jimbozoomer.jugcraft.nuclear.DcRoom.inverter(level, other).onInv(),
				"feeder 01 open: inverter 1A-1 on its bypass, 1A-2 on its DC");
		io.github.jimbozoomer.jugcraft.nuclear.DcRoom.tickPanel(level, inv, false);
		double now = level.getGameTime();
		int fail = io.github.jimbozoomer.jugcraft.nuclear.DcRoom.I_FAIL;
		helper.assertTrue(io.github.jimbozoomer.jugcraft.nuclear.DcRoom.window(inv, fail, now) == 2
				&& io.github.jimbozoomer.jugcraft.nuclear.DcRoom.window(inv, io.github.jimbozoomer.jugcraft.nuclear.DcRoom.I_ON_BYP, now) == 2,
				"INV FAIL and ON BYPASS come in flashing");
		io.github.jimbozoomer.jugcraft.nuclear.DcRoom.acknowledge(inv);
		helper.assertTrue(io.github.jimbozoomer.jugcraft.nuclear.DcRoom.window(inv, fail, now) == 1, "ACK: steady");
		boardA.breakers |= 1 << io.github.jimbozoomer.jugcraft.nuclear.DcRoom.INV1;
		io.github.jimbozoomer.jugcraft.nuclear.DcRoom.tickBoard(level, boardA);
		io.github.jimbozoomer.jugcraft.nuclear.DcRoom.tickPanel(level, inv, false);
		helper.assertTrue(io.github.jimbozoomer.jugcraft.nuclear.DcRoom.inverter(level, inv).onInv()
				&& io.github.jimbozoomer.jugcraft.nuclear.DcRoom.window(inv, fail, now) == 1, "feeder closed: back on the inverter, the window lit until RESET");
		helper.assertTrue(io.github.jimbozoomer.jugcraft.nuclear.DcRoom.reset(inv) >= 2 && io.github.jimbozoomer.jugcraft.nuclear.DcRoom.window(inv, fail, now) == 0,
				"RESET: the cleared windows go dark");
		inv.annTestUntil = level.getGameTime() + io.github.jimbozoomer.jugcraft.nuclear.DcRoom.TEST_TICKS;
		boolean allLit = true;
		for (int k = 0; k < io.github.jimbozoomer.jugcraft.nuclear.DcRoom.WINDOWS; k++) {
			allLit &= io.github.jimbozoomer.jugcraft.nuclear.DcRoom.window(inv, k, now) == 1;
		}
		helper.assertTrue(allLit, "TEST lights every window");
		inv.annTestUntil = 0;
		// The manual bypass switch: the load on the bypass with the inverter running.
		inv.mode = io.github.jimbozoomer.jugcraft.nuclear.DcRoom.BYPASS;
		var s2 = io.github.jimbozoomer.jugcraft.nuclear.DcRoom.inverter(level, inv);
		helper.assertTrue(s2.run() && !s2.onInv() && s2.onBypass(), "on BYPASS the load is on the bypass");
		inv.mode = io.github.jimbozoomer.jugcraft.nuclear.DcRoom.NORMAL;
		// Feeder 03: DC panel 1A-1 dead with it, alive again closed; its own main the same.
		var pnl = panels.stream().filter(p -> io.github.jimbozoomer.jugcraft.nuclear.LivePanelBlockEntity.variant(p.getBlockState()) == 0).findFirst().orElseThrow();
		helper.assertTrue(io.github.jimbozoomer.jugcraft.nuclear.DcRoom.panelLive(level, pnl), "DC panel 1A-1 alive");
		boardA.breakers &= ~(1 << io.github.jimbozoomer.jugcraft.nuclear.DcRoom.PNL1);
		helper.assertTrue(!io.github.jimbozoomer.jugcraft.nuclear.DcRoom.panelLive(level, pnl), "feeder 03 open: DC panel 1A-1 dead");
		boardA.breakers |= 1 << io.github.jimbozoomer.jugcraft.nuclear.DcRoom.PNL1;
		pnl.breakers &= ~1;
		helper.assertTrue(!io.github.jimbozoomer.jugcraft.nuclear.DcRoom.panelLive(level, pnl), "its main open: dead");
		pnl.breakers |= 1;
		// Feeder 05: load centre 1A's control power goes; feeder 07: diesel 1A won't start.
		var lcA = gear.stream().filter(g -> g.lc != null && io.github.jimbozoomer.jugcraft.nuclear.LoadCentre.tag(g).equals("1A")).findFirst().orElseThrow();
		boardA.breakers &= ~(1 << io.github.jimbozoomer.jugcraft.nuclear.DcRoom.LC_CTL | 1 << io.github.jimbozoomer.jugcraft.nuclear.DcRoom.EDG_CTL);
		io.github.jimbozoomer.jugcraft.nuclear.DcRoom.tickBoard(level, boardA);
		io.github.jimbozoomer.jugcraft.nuclear.LoadCentre.plantLoads(level, lcA, lcA.lc);
		helper.assertTrue(lcA.lc.dcOff && !lcA.lc.controlPower(1), "feeder 05 open: LC 1A has no control power");
		EquipmentBlockEntity edgA = null;
		for (var e : io.github.jimbozoomer.jugcraft.nuclear.EquipmentRegistry.of(level).anchors().entrySet()) {
			if (e.getValue().kind() == EquipmentKind.EMERGENCY_DIESEL_GENERATOR && e.getKey().distSqr(stake) < 400 * 400
					&& level.getBlockEntity(e.getKey()) instanceof EquipmentBlockEntity edg
					&& io.github.jimbozoomer.jugcraft.nuclear.DieselPanels.name(edg).equals("EDG 1A")) {
				edgA = edg;
			}
		}
		helper.assertTrue(edgA != null, "diesel 1A");
		var refused = io.github.jimbozoomer.jugcraft.nuclear.DieselPanels.refuse(edgA, true, io.github.jimbozoomer.jugcraft.nuclear.DieselPanels.REMOTE);
		helper.assertTrue(refused != null && refused.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents tc
				&& tc.getKey().equals("message.jugcraft.nuclear.edg_no_dc"), "feeder 07 open: diesel 1A won't start: " + refused);
		boardA.breakers |= 1 << io.github.jimbozoomer.jugcraft.nuclear.DcRoom.LC_CTL | 1 << io.github.jimbozoomer.jugcraft.nuclear.DcRoom.EDG_CTL;
		io.github.jimbozoomer.jugcraft.nuclear.DcRoom.tickBoard(level, boardA);
		io.github.jimbozoomer.jugcraft.nuclear.LoadCentre.plantLoads(level, lcA, lcA.lc);
		var again = io.github.jimbozoomer.jugcraft.nuclear.DieselPanels.refuse(edgA, true, io.github.jimbozoomer.jugcraft.nuclear.DieselPanels.REMOTE);
		helper.assertTrue(!lcA.lc.dcOff && lcA.lc.controlPower(1) && !(again != null && again.getContents()
				instanceof net.minecraft.network.chat.contents.TranslatableContents tc2 && tc2.getKey().equals("message.jugcraft.nuclear.edg_no_dc")),
				"feeders closed: control power back");
		// The main opened: the battery off the bus (the chargers carry it), and they charge it no more.
		boardA.breakers &= ~1;
		io.github.jimbozoomer.jugcraft.nuclear.DcRoom.tickBoard(level, boardA);
		helper.assertTrue((boardA.sbFlags & io.github.jimbozoomer.jugcraft.nuclear.DcRoom.BATT) == 0
				&& (boardA.sbFlags & io.github.jimbozoomer.jugcraft.nuclear.DcRoom.BUS) != 0, "main open: the bus on the chargers alone");
		boardA.breakers |= 1;
		io.github.jimbozoomer.jugcraft.nuclear.DcRoom.tickBoard(level, boardA);
		// Older rooms round 4: each battery room has a disconnect for each string, on its train's switchboard; one opened
		// takes its string off the bus (the other carries it), both the battery; closed again, all four racks are back.
		helper.assertTrue(disconnects.size() == 4, "a disconnect for each string: " + disconnects.size());
		for (var d : disconnects) {
			var board = io.github.jimbozoomer.jugcraft.nuclear.DcRoom.board(level, d);
			helper.assertTrue(board != null && io.github.jimbozoomer.jugcraft.nuclear.DcRoom.train(board.getBlockState())
					== io.github.jimbozoomer.jugcraft.nuclear.DcRoom.train(d.getBlockState()), "a disconnect finds its train's switchboard");
		}
		var d1 = disconnects.stream().filter(d -> io.github.jimbozoomer.jugcraft.nuclear.LivePanelBlockEntity.variant(d.getBlockState()) == 0).findFirst().orElseThrow();
		var d2 = disconnects.stream().filter(d -> io.github.jimbozoomer.jugcraft.nuclear.LivePanelBlockEntity.variant(d.getBlockState()) == 1).findFirst().orElseThrow();
		int racksOn = io.github.jimbozoomer.jugcraft.nuclear.DcRoom.onBus(level, boardA).size();
		d1.mode = io.github.jimbozoomer.jugcraft.nuclear.DcRoom.OPEN;
		io.github.jimbozoomer.jugcraft.nuclear.DcRoom.tickBoard(level, boardA);
		helper.assertTrue(racksOn == 4 && boardA.sbIso == 1 && io.github.jimbozoomer.jugcraft.nuclear.DcRoom.onBus(level, boardA).size() == 2
				&& (boardA.sbFlags & io.github.jimbozoomer.jugcraft.nuclear.DcRoom.BATT) != 0, "string 1 off the bus, string 2 carries it: " + racksOn + ", "
						+ boardA.sbIso);
		d2.mode = io.github.jimbozoomer.jugcraft.nuclear.DcRoom.OPEN;
		io.github.jimbozoomer.jugcraft.nuclear.DcRoom.tickBoard(level, boardA);
		helper.assertTrue(io.github.jimbozoomer.jugcraft.nuclear.DcRoom.onBus(level, boardA).isEmpty()
				&& (boardA.sbFlags & io.github.jimbozoomer.jugcraft.nuclear.DcRoom.BATT) == 0, "both strings off: the battery is off the bus");
		d1.mode = d2.mode = io.github.jimbozoomer.jugcraft.nuclear.DcRoom.CLOSED;
		io.github.jimbozoomer.jugcraft.nuclear.DcRoom.tickBoard(level, boardA);
		helper.assertTrue(boardA.sbIso == 0 && io.github.jimbozoomer.jugcraft.nuclear.DcRoom.onBus(level, boardA).size() == 4, "both closed: all four racks on");
		// A cell clicked reads where its number is (tools/battery_rooms.py cell_number): the upper step from the positive
		// terminal at its right end leftwards, then the lower step left to right; the second rack of a string 31 on.
		helper.assertTrue(io.github.jimbozoomer.jugcraft.nuclear.StationBatteryBlock.cellAt(2.15 + 14 * 4 + 1, 20, 0) == 1
				&& io.github.jimbozoomer.jugcraft.nuclear.StationBatteryBlock.cellAt(2.15 + 1, 20, 0) == 15
				&& io.github.jimbozoomer.jugcraft.nuclear.StationBatteryBlock.cellAt(2.15 + 1, 10, 0) == 16
				&& io.github.jimbozoomer.jugcraft.nuclear.StationBatteryBlock.cellAt(2.15 + 14 * 4 + 1, 10, 1) == 60
				&& io.github.jimbozoomer.jugcraft.nuclear.StationBatteryBlock.cellAt(1.0, 10, 0) == 0, "the cells' numbers");
		// A charger's breakers: DC open, it puts out nothing (CHGR FAIL); AC open, nothing comes in (and AC FAIL).
		var chg = chargers.stream().filter(c -> io.github.jimbozoomer.jugcraft.nuclear.LivePanelBlockEntity.variant(c.getBlockState()) == 0).findFirst().orElseThrow();
		helper.assertTrue(io.github.jimbozoomer.jugcraft.nuclear.DcRoom.delivering(chg), "charger 1A-1 puts out");
		chg.dcFlags = io.github.jimbozoomer.jugcraft.nuclear.DcRoom.DC_OPEN;
		helper.assertTrue(!io.github.jimbozoomer.jugcraft.nuclear.DcRoom.delivering(chg)
				&& (io.github.jimbozoomer.jugcraft.nuclear.DcRoom.chargerAlarms(level, chg, level.getGameTime()) >> io.github.jimbozoomer.jugcraft.nuclear.DcRoom.C_CHGR_FAIL & 1) == 1,
				"its DC breaker open: CHARGER FAIL");
		chg.dcFlags = io.github.jimbozoomer.jugcraft.nuclear.DcRoom.AC_OPEN;
		long in;
		try (net.fabricmc.fabric.api.transfer.v1.transaction.Transaction t = net.fabricmc.fabric.api.transfer.v1.transaction.Transaction.openOuter()) {
			in = chg.input.insert(500, t);
		}
		helper.assertTrue(in == 0, "its AC breaker open: nothing comes in: " + in);
		chg.dcFlags = 0;
		// MODE to EQUALIZE: 24 game hours on the timer, then back to FLOAT by itself.
		chg.mode = io.github.jimbozoomer.jugcraft.nuclear.DcRoom.EQUALIZE;
		chg.eqUntil = level.getGameTime() + io.github.jimbozoomer.jugcraft.nuclear.DcRoom.EQ_TICKS;
		helper.assertTrue(Math.abs(io.github.jimbozoomer.jugcraft.nuclear.DcRoom.eqHours(chg, level.getGameTime()) - 24) < 1e-9, "equalize: 24 hours on the timer");
		io.github.jimbozoomer.jugcraft.nuclear.DcRoom.tickBoard(level, boardA);
		helper.assertTrue(Math.abs(io.github.jimbozoomer.jugcraft.nuclear.DcRoom.busVolts(level, boardA) - io.github.jimbozoomer.jugcraft.nuclear.DcRoom.EQUAL_V) < 0.5,
				"equalizing, the bus at 139.8 V");
		chg.eqUntil = level.getGameTime() - 1;
		io.github.jimbozoomer.jugcraft.nuclear.DcRoom.tickPanel(level, chg, true);
		helper.assertTrue(chg.mode == io.github.jimbozoomer.jugcraft.nuclear.DcRoom.FLOAT, "its timer run out, it goes back to FLOAT");
		// The laptop writes its room: the bus, a line a piece, the feeders on its other pages.
		var laptop = laptops.stream().filter(l -> boardA.getBlockPos().equals(l.board)).findFirst().orElseThrow();
		java.util.Map<String, String> screen = new java.util.HashMap<>();
		io.github.jimbozoomer.jugcraft.nuclear.DcRoom.screen(level, io.github.jimbozoomer.jugcraft.nuclear.DcRoom.board(level, laptop), 0, now, screen);
		helper.assertTrue("DC BUS 1A".equals(screen.get("dcl_t")) && "CHGR 1A-1".equals(screen.get("dcl_n1")) && "ON INV".equals(screen.get("dcl_g3"))
				&& "LIVE".equals(screen.get("dcl_g7")) && screen.getOrDefault("dcl_x", "").startsWith("ALARMS "), "the laptop's room: " + screen);
		screen.clear();
		io.github.jimbozoomer.jugcraft.nuclear.DcRoom.screen(level, boardA, 2, now, screen);
		helper.assertTrue("12 SPARE".equals(screen.get("dcl_n2")) && "OPEN".equals(screen.get("dcl_w2")) && "CLOSED".equals(screen.get("dcl_g0")),
				"the laptop's feeders: " + screen);
		helper.succeed();
	}

	/**
	 * Older rooms round 5 (Justin: "Control room furniture"): the medium plant's control room has its three operators'
	 * consoles, one of each position, and the shift manager's desk; each links the room's display wall; a click on a
	 * screen steps its page (round to its first again); the clicks land on the screens where the drawing has them; a
	 * screen's page reads the plant (ControlRoomConsoles.lines).
	 */
	@GameTest(maxTicks = 40)
	public void mediumPlantControlRoomConsoles(GameTestHelper helper) {
		if (TestFocus.skip("nuclear")) {
			helper.succeed();
			return;
		}
		var level = helper.getLevel();
		helper.assertTrue(io.github.jimbozoomer.jugcraft.nuclear.ControlRoomConsoles.screenAt(false, 12.6, 18.0) == 0
				&& io.github.jimbozoomer.jugcraft.nuclear.ControlRoomConsoles.screenAt(false, 24.0, 15.0) == 1
				&& io.github.jimbozoomer.jugcraft.nuclear.ControlRoomConsoles.screenAt(false, 35.4, 21.0) == 2
				&& io.github.jimbozoomer.jugcraft.nuclear.ControlRoomConsoles.screenAt(false, 24.0, 10.0) == -1
				&& io.github.jimbozoomer.jugcraft.nuclear.ControlRoomConsoles.screenAt(true, 30.6, 18.0) == 1, "the screens where the drawing has them");
		BlockPos stake = helper.absolutePos(BlockPos.ZERO).offset(-12_000, 0, 15_000);
		Blueprint print = Blueprint.get("nuclear_medium_pwr", false);
		PlantBuilder.build(level, print, stake, Rotation.NONE, null, true);
		java.util.List<io.github.jimbozoomer.jugcraft.nuclear.LivePanelBlockEntity> consoles = new java.util.ArrayList<>();
		BlockPos wall = null;
		int racks = 0;
		for (int i = 0; i < print.size(); i++) {
			BlockState state = print.state(i, Rotation.NONE);
			BlockPos pos = stake.offset(print.offset(i, Rotation.NONE));
			if (state.getBlock() instanceof io.github.jimbozoomer.jugcraft.nuclear.EquipmentAnchorBlock anchor && anchor.kind == EquipmentKind.PLANT_DISPLAY_WALL) {
				wall = pos.immutable();
			} else if (level.getBlockEntity(pos) instanceof io.github.jimbozoomer.jugcraft.nuclear.LivePanelBlockEntity p
					&& io.github.jimbozoomer.jugcraft.nuclear.ControlRoomConsoles.isConsole(state)
					&& !io.github.jimbozoomer.jugcraft.nuclear.ControlRoomConsoles.isRsrDesk(state)) {
				consoles.add(p);
			} else if (state.is(PlantInterior.BLOCKS.get("cr_procedure_rack"))) {
				racks++;
			}
		}
		helper.assertTrue(wall != null, "the display wall");
		helper.assertTrue(consoles.size() == 4, "three consoles and the shift manager's desk: " + consoles.size());
		// (Older rooms round 7: the remote shutdown room has an AOP rack of its own.)
		helper.assertTrue(racks == 18, "eight procedure racks two blocks high and the remote shutdown room's: " + racks);
		java.util.Set<Integer> positions = new java.util.HashSet<>();
		for (var c : consoles) {
			if (!io.github.jimbozoomer.jugcraft.nuclear.ControlRoomConsoles.isSsDesk(c.getBlockState())) {
				positions.add(io.github.jimbozoomer.jugcraft.nuclear.ControlRoomConsoles.position(c.getBlockState()));
			}
			c.serverTick(level);
			helper.assertTrue(wall.equals(c.panel), "a console links the display wall: " + c.getBlockPos() + " " + c.panel);
		}
		helper.assertTrue(positions.equals(java.util.Set.of(0, 1, 2)), "one console of each position: " + positions);
		var c = consoles.get(0);
		String[] pages = io.github.jimbozoomer.jugcraft.nuclear.ControlRoomConsoles.pages(c.getBlockState());
		int first = io.github.jimbozoomer.jugcraft.nuclear.ControlRoomConsoles.page(c, 1, pages.length);
		io.github.jimbozoomer.jugcraft.nuclear.ControlRoomConsoles.step(c, 1, pages.length);
		helper.assertTrue(io.github.jimbozoomer.jugcraft.nuclear.ControlRoomConsoles.page(c, 1, pages.length) == (first + 1) % pages.length
				&& io.github.jimbozoomer.jugcraft.nuclear.ControlRoomConsoles.page(c, 0, pages.length) == 0, "a click steps only its screen");
		for (int k = 1; k < pages.length; k++) {
			io.github.jimbozoomer.jugcraft.nuclear.ControlRoomConsoles.step(c, 1, pages.length);
		}
		helper.assertTrue(io.github.jimbozoomer.jugcraft.nuclear.ControlRoomConsoles.page(c, 1, pages.length) == (first + 1) % pages.length,
				"its pages go round");
		io.github.jimbozoomer.jugcraft.nuclear.BoardPlant p = new io.github.jimbozoomer.jugcraft.nuclear.BoardPlant();
		p.present = true;
		p.loops = 4;
		var lines = io.github.jimbozoomer.jugcraft.nuclear.ControlRoomConsoles.lines("REACTOR", p,
				new io.github.jimbozoomer.jugcraft.nuclear.ControlBoard.State(), 0, 6000);
		helper.assertTrue(lines.length == io.github.jimbozoomer.jugcraft.nuclear.ControlRoomConsoles.LINES && lines[0].label().equals("REACTOR")
				&& !lines[1].value().equals("----"), "the reactor page reads the plant: " + lines[1]);
		for (String[] all : io.github.jimbozoomer.jugcraft.nuclear.ControlRoomConsoles.PAGES) {
			for (String page : all) {
				helper.assertTrue(io.github.jimbozoomer.jugcraft.nuclear.ControlRoomConsoles.lines(page, p,
						new io.github.jimbozoomer.jugcraft.nuclear.ControlBoard.State(), 0, 6000).length == 7, "every page has its lines: " + page);
			}
		}
		helper.succeed();
	}

	/**
	 * The administration building part 2 (Justin: "Admin building part 2"): the chemistry lab's status board works out its
	 * eleven results, the gamma station counts a sample; document control's eight carriages, plan files; the board room's
	 * table; a carriage's handwheel rolls it and those beside it into the aisle, which opens where it stood.
	 */
	@GameTest(maxTicks = 100)
	public void mediumPlantAdminBuildingPart2(GameTestHelper helper) {
		if (TestFocus.skip("nuclear")) {
			helper.succeed();
			return;
		}
		var level = helper.getLevel();
		BlockPos stake = helper.absolutePos(BlockPos.ZERO).offset(-24_000, 0, 17_000);
		Blueprint print = Blueprint.get("nuclear_medium_pwr", false);
		PlantBuilder.build(level, print, stake, Rotation.NONE, null, true);
		var shelving = (io.github.jimbozoomer.jugcraft.nuclear.MobileShelvingBlock) PlantInterior.BLOCKS.get("doc_mobile_shelving");
		int carriageBlocks = 0, planFiles = 0, table = 0;
		BlockPos carriage = null;
		io.github.jimbozoomer.jugcraft.nuclear.LivePanelBlockEntity board = null, gamma = null;
		for (int i = 0; i < print.size(); i++) {
			BlockState state = print.state(i, Rotation.NONE);
			BlockPos pos = stake.offset(print.offset(i, Rotation.NONE));
			if (state.is(shelving)) {
				carriageBlocks++;
				if (carriage == null && state.getValue(io.github.jimbozoomer.jugcraft.nuclear.ShelvingBlock.SEG) == 0
						&& state.getValue(io.github.jimbozoomer.jugcraft.nuclear.ShelvingBlock.HALF) == net.minecraft.world.level.block.state.properties.DoubleBlockHalf.LOWER) {
					carriage = pos.immutable();
				}
			} else if (state.is(PlantInterior.BLOCKS.get("doc_plan_file"))) {
				planFiles++;
			} else if (state.is(PlantInterior.BLOCKS.get("board_table"))) {
				table++;
			} else if (level.getBlockEntity(pos) instanceof io.github.jimbozoomer.jugcraft.nuclear.LivePanelBlockEntity p) {
				if (io.github.jimbozoomer.jugcraft.nuclear.ChemLab.isBoard(p.getBlockState())) {
					board = p;
				} else if (io.github.jimbozoomer.jugcraft.nuclear.ChemLab.isGamma(p.getBlockState())) {
					gamma = p;
				}
			}
		}
		helper.assertTrue(carriageBlocks == 64, "eight carriages of eight blocks: " + carriageBlocks);
		helper.assertTrue(planFiles == 4 && table == 24, "two plan files and the boardroom table: " + planFiles + ", " + table);
		helper.assertTrue(board != null && gamma != null, "the chemistry status board and the gamma station");
		io.github.jimbozoomer.jugcraft.nuclear.ChemLab.boardTick(board, level);
		helper.assertTrue(board.chemRows.size() == io.github.jimbozoomer.jugcraft.nuclear.ChemLab.ROWS, "the board's rows: " + board.chemRows);
		helper.assertTrue(gamma.getBlockPos().equals(board.panel), "the board links the gamma station");
		io.github.jimbozoomer.jugcraft.nuclear.ChemLab.start(gamma, level);
		helper.assertTrue(gamma.gsAct.size() == 8 && gamma.gsStart == level.getGameTime(), "the station counts a sample");
		final BlockPos turned = carriage;
		helper.assertTrue(turned != null && shelving.turn(level, turned, null), "a carriage's handwheel turns");
		helper.runAfterDelay(40, () -> {
			helper.assertTrue(!io.github.jimbozoomer.jugcraft.nuclear.MobileShelvingBlock.moving(level), "the carriages have stopped");
			helper.assertTrue(level.getBlockState(turned).isAir(), "the aisle opened where the carriage stood");
			helper.succeed();
		});
	}

	/**
	 * The administration building part 1 (Justin: "Administration building"): an EXIT sign and a pull station at the
	 * stairs on each floor and at the front doors, each floor's evacuation plan; an office computer's screen reads the
	 * plant's reactor.
	 */
	@GameTest(maxTicks = 40)
	public void mediumPlantAdminBuildingSafety(GameTestHelper helper) {
		if (TestFocus.skip("nuclear")) {
			helper.succeed();
			return;
		}
		var level = helper.getLevel();
		BlockPos stake = helper.absolutePos(BlockPos.ZERO).offset(-24_000, 0, 15_000);
		Blueprint print = Blueprint.get("nuclear_medium_pwr", false);
		PlantBuilder.build(level, print, stake, Rotation.NONE, null, true);
		int exits = 0, pulls = 0, plans = 0;
		io.github.jimbozoomer.jugcraft.nuclear.DeskScreenBlockEntity desk = null;
		java.util.Set<Integer> floors = new java.util.HashSet<>();
		for (int i = 0; i < print.size(); i++) {
			BlockState state = print.state(i, Rotation.NONE);
			BlockPos pos = stake.offset(print.offset(i, Rotation.NONE));
			if (state.is(PlantInterior.BLOCKS.get("exit_sign"))) {
				exits++;
			} else if (state.is(PlantInterior.BLOCKS.get("fire_alarm_pull"))) {
				pulls++;
			} else if (state.is(PlantInterior.BLOCKS.get("admin_evac_plan"))) {
				plans++;
				floors.add(state.getValue(((io.github.jimbozoomer.jugcraft.nuclear.MultiFurnitureBlock) state.getBlock()).variantProperty()));
			} else if (desk == null && level.getBlockEntity(pos) instanceof io.github.jimbozoomer.jugcraft.nuclear.DeskScreenBlockEntity d) {
				desk = d;
			}
		}
		helper.assertTrue(exits >= 4 && pulls >= 4, "EXIT signs and pull stations at the stairs and the front doors: " + exits + ", " + pulls);
		helper.assertTrue(plans == 6 && floors.size() == 3, "an evacuation plan (two blocks) for each floor: " + plans + " blocks, floors " + floors);
		helper.assertTrue(desk != null, "an office computer with its screen");
		desk.serverTick(level);
		helper.assertTrue(desk.status != 0, "the office screen reads the plant's reactor");
		helper.succeed();
	}

	/**
	 * The security gatehouse (Justin: "rework the security post next, the whole building everything inside of it up to
	 * new standards"): it has its alarm station console, X-ray and metal detector; the detector alarms on someone
	 * carrying an iron sword and passes someone carrying bread; the X-ray sends the sword to be searched and clears the
	 * bread; the console links the gatehouse's security post and puts a zone in alarm for a zombie inside it.
	 */
	@GameTest(maxTicks = 40)
	public void mediumPlantGatehouseSearchTrain(GameTestHelper helper) {
		if (TestFocus.skip("nuclear")) {
			helper.succeed();
			return;
		}
		var level = helper.getLevel();
		helper.assertTrue(io.github.jimbozoomer.jugcraft.nuclear.SecurityGatehouse.zone(0, -10) == 0
				&& io.github.jimbozoomer.jugcraft.nuclear.SecurityGatehouse.zone(10, 0) == 2
				&& io.github.jimbozoomer.jugcraft.nuclear.SecurityGatehouse.zone(0, 10) == 3
				&& io.github.jimbozoomer.jugcraft.nuclear.SecurityGatehouse.zone(-10, -3) == 5, "the zones round the console");
		BlockPos stake = helper.absolutePos(BlockPos.ZERO).offset(-21_000, 0, 15_000);
		Blueprint print = Blueprint.get("nuclear_medium_pwr", false);
		PlantBuilder.build(level, print, stake, Rotation.NONE, null, true);
		io.github.jimbozoomer.jugcraft.nuclear.LivePanelBlockEntity console = null, xray = null, detector = null;
		java.util.List<io.github.jimbozoomer.jugcraft.nuclear.LivePanelBlockEntity> posts = new java.util.ArrayList<>();
		for (int i = 0; i < print.size(); i++) {
			BlockState state = print.state(i, Rotation.NONE);
			BlockPos pos = stake.offset(print.offset(i, Rotation.NONE));
			if (!(level.getBlockEntity(pos) instanceof io.github.jimbozoomer.jugcraft.nuclear.LivePanelBlockEntity p)) {
				continue;
			}
			if (io.github.jimbozoomer.jugcraft.nuclear.SecurityGatehouse.isConsole(state)) {
				console = p;
			} else if (io.github.jimbozoomer.jugcraft.nuclear.SecurityGatehouse.isXray(state)) {
				xray = p;
			} else if (io.github.jimbozoomer.jugcraft.nuclear.SecurityGatehouse.isDetector(state)) {
				detector = p;
			} else if (io.github.jimbozoomer.jugcraft.nuclear.AccessControl.isStation(state)) {
				posts.add(p);
			}
		}
		helper.assertTrue(console != null && xray != null && detector != null, "the console, the X-ray and the metal detector");
		var player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
		player.getInventory().setItem(0, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_SWORD));
		helper.assertTrue(io.github.jimbozoomer.jugcraft.nuclear.SecurityGatehouse.walkedThrough(detector, level, player) && detector.wdAlarm,
				"the detector alarms on an iron sword");
		player.getInventory().setItem(0, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.BREAD));
		helper.assertTrue(!io.github.jimbozoomer.jugcraft.nuclear.SecurityGatehouse.walkedThrough(detector, level, player) && detector.wdCount == 2,
				"and passes bread");
		helper.assertTrue(io.github.jimbozoomer.jugcraft.nuclear.SecurityGatehouse.scan(xray, level,
				new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_SWORD))
				&& !io.github.jimbozoomer.jugcraft.nuclear.SecurityGatehouse.scan(xray, level, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.BREAD)),
				"the X-ray: the sword to be searched, the bread clear");
		var bags = io.github.jimbozoomer.jugcraft.nuclear.SecurityGatehouse.bags(xray);
		helper.assertTrue(bags.size() == 2 && bags.get(0).search() && !bags.get(1).search() && xray.xrCount == 2, "its screen's bags: " + xray.xrLog);
		console.serverTick(level);
		BlockPos here = console.getBlockPos();
		var nearest = posts.stream().min(java.util.Comparator.comparingDouble(p -> p.getBlockPos().distSqr(here))).orElseThrow();
		helper.assertTrue(nearest.getBlockPos().equals(console.panel), "the console links the gatehouse's post: " + console.panel);
		var zombie = net.minecraft.world.entity.EntityTypes.ZOMBIE.create(level, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
		helper.assertTrue(zombie != null, "a zombie");
		zombie.setPos(here.getX() + 0.5, here.getY(), here.getZ() - 20.5);
		level.addFreshEntity(zombie);
		helper.assertTrue((io.github.jimbozoomer.jugcraft.nuclear.SecurityGatehouse.zones(level, here) & 1) != 0, "a zombie to the north puts zone 1 in alarm");
		zombie.discard();
		helper.succeed();
	}

	/**
	 * Older rooms round 7 (Justin: "Remote shutdown room"): the room has its station, the transfer key cabinet, the
	 * communications station and its sign; the station links the remote shutdown panel, its two screens step through
	 * their own pages, and its AOP-8 page checks the plant (a cold plant: nothing done; a tripped, transferred plant:
	 * the trip and the transfer done).
	 */
	@GameTest(maxTicks = 40)
	public void mediumPlantRemoteShutdownStation(GameTestHelper helper) {
		if (TestFocus.skip("nuclear")) {
			helper.succeed();
			return;
		}
		var level = helper.getLevel();
		BlockPos stake = helper.absolutePos(BlockPos.ZERO).offset(-18_000, 0, 15_000);
		Blueprint print = Blueprint.get("nuclear_medium_pwr", false);
		PlantBuilder.build(level, print, stake, Rotation.NONE, null, true);
		java.util.List<io.github.jimbozoomer.jugcraft.nuclear.LivePanelBlockEntity> desks = new java.util.ArrayList<>();
		BlockPos rsp = null;
		int keys = 0, comms = 0, signs = 0;
		for (int i = 0; i < print.size(); i++) {
			BlockState state = print.state(i, Rotation.NONE);
			BlockPos pos = stake.offset(print.offset(i, Rotation.NONE));
			if (state.getBlock() instanceof io.github.jimbozoomer.jugcraft.nuclear.EquipmentAnchorBlock anchor && anchor.kind == EquipmentKind.REMOTE_SHUTDOWN_PANEL) {
				rsp = pos.immutable();
			} else if (level.getBlockEntity(pos) instanceof io.github.jimbozoomer.jugcraft.nuclear.LivePanelBlockEntity p
					&& io.github.jimbozoomer.jugcraft.nuclear.ControlRoomConsoles.isRsrDesk(state)) {
				desks.add(p);
			} else if (state.is(PlantInterior.BLOCKS.get("rsr_key_cabinet"))) {
				keys++;
			} else if (state.is(PlantInterior.BLOCKS.get("rsr_phone_station"))) {
				comms++;
			} else if (state.is(PlantInterior.BLOCKS.get("sign_rsr_area"))) {
				signs++;
			}
		}
		helper.assertTrue(rsp != null, "the remote shutdown panel");
		helper.assertTrue(desks.size() == 1 && keys == 1 && comms == 1 && signs == 1,
				"one station, key cabinet, communications station and sign: " + desks.size() + ", " + keys + ", " + comms + ", " + signs);
		var desk = desks.get(0);
		desk.serverTick(level);
		helper.assertTrue(rsp.equals(desk.panel), "the station links the remote shutdown panel: " + desk.panel);
		String[] pages = io.github.jimbozoomer.jugcraft.nuclear.ControlRoomConsoles.pages(desk.getBlockState());
		helper.assertTrue(pages.length == 5 && io.github.jimbozoomer.jugcraft.nuclear.ControlRoomConsoles.screens(desk.getBlockState()) == 2
				&& io.github.jimbozoomer.jugcraft.nuclear.ControlRoomConsoles.page(desk, 0, pages.length) == 0
				&& io.github.jimbozoomer.jugcraft.nuclear.ControlRoomConsoles.page(desk, 1, pages.length) == 1, "two screens on AOP-8 and HOT STANDBY");
		io.github.jimbozoomer.jugcraft.nuclear.BoardPlant p = new io.github.jimbozoomer.jugcraft.nuclear.BoardPlant();
		p.present = true;
		var s = new io.github.jimbozoomer.jugcraft.nuclear.ControlBoard.State();
		var cold = io.github.jimbozoomer.jugcraft.nuclear.ControlRoomConsoles.lines("AOP-8 EVAC", p, s, 100, 6000);
		helper.assertTrue(cold[0].value().equals("OPEN") && cold[2].value().equals("OPEN"), "nothing tripped or transferred: " + cold[0] + " " + cold[2]);
		p.scrammed = true;
		p.remote = true;
		var done = io.github.jimbozoomer.jugcraft.nuclear.ControlRoomConsoles.lines("AOP-8 EVAC", p, s, 100, 6000);
		helper.assertTrue(done[0].value().equals("DONE") && done[2].value().equals("DONE")
				&& io.github.jimbozoomer.jugcraft.nuclear.ControlRoomConsoles.done(done) >= 2, "tripped and transferred: " + done[0] + " " + done[2]);
		var hot = io.github.jimbozoomer.jugcraft.nuclear.ControlRoomConsoles.lines("HOT STANDBY", p, s, 100, 6000);
		helper.assertTrue(hot[1].value().startsWith("RSP"), "the hot standby page shows control at the panel: " + hot[1]);
		helper.succeed();
	}

	/**
	 * Older rooms round 6 (Justin: "Lobby and entrance"): the control building's entrance has its security post and the
	 * hand reader; a gate opening for someone writes the passage to the post (IN through the entry lane, OUT through the
	 * exit), who is inside follows; the duress button is where the drawing has it.
	 */
	@GameTest(maxTicks = 40)
	public void mediumPlantEntranceSecurityPost(GameTestHelper helper) {
		if (TestFocus.skip("nuclear")) {
			helper.succeed();
			return;
		}
		var level = helper.getLevel();
		helper.assertTrue(io.github.jimbozoomer.jugcraft.nuclear.AccessControl.duressAt(43.6, 13.4)
				&& !io.github.jimbozoomer.jugcraft.nuclear.AccessControl.duressAt(20.0, 13.4), "the duress button where the drawing has it");
		BlockPos stake = helper.absolutePos(BlockPos.ZERO).offset(-15_000, 0, 15_000);
		Blueprint print = Blueprint.get("nuclear_medium_pwr", false);
		PlantBuilder.build(level, print, stake, Rotation.NONE, null, true);
		java.util.List<io.github.jimbozoomer.jugcraft.nuclear.LivePanelBlockEntity> posts = new java.util.ArrayList<>();
		java.util.List<BlockPos> entry = new java.util.ArrayList<>(), exit = new java.util.ArrayList<>();
		int readers = 0;
		for (int i = 0; i < print.size(); i++) {
			BlockState state = print.state(i, Rotation.NONE);
			BlockPos pos = stake.offset(print.offset(i, Rotation.NONE));
			if (level.getBlockEntity(pos) instanceof io.github.jimbozoomer.jugcraft.nuclear.LivePanelBlockEntity p
					&& io.github.jimbozoomer.jugcraft.nuclear.AccessControl.isStation(state)) {
				posts.add(p);
			} else if (state.is(PlantInterior.BLOCKS.get("ac_hand_reader"))) {
				readers++;
			} else if (state.getBlock() instanceof io.github.jimbozoomer.jugcraft.nuclear.SpeedGateBlock
					&& state.hasProperty(io.github.jimbozoomer.jugcraft.nuclear.SpeedGateBlock.OPEN)) {
				(state.getValue(io.github.jimbozoomer.jugcraft.nuclear.SpeedGateBlock.FACING) == net.minecraft.core.Direction.SOUTH ? exit : entry).add(pos.immutable());
			}
		}
		// (The security gatehouse has a post and a hand reader of its own: the gatehouse rework.)
		helper.assertTrue(posts.size() == 2 && readers == 2, "two security posts and two hand readers: " + posts.size() + ", " + readers);
		var post = posts.get(0);
		BlockPos in = null, out = null;
		for (BlockPos g : entry) {
			if (g.distSqr(post.getBlockPos()) < 16 * 16) {
				in = g;
			}
		}
		for (BlockPos g : exit) {
			if (g.distSqr(post.getBlockPos()) < 16 * 16) {
				out = g;
			}
		}
		helper.assertTrue(in != null && out != null, "the entry and exit lanes by the post");
		var player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
		io.github.jimbozoomer.jugcraft.nuclear.AccessControl.passed(level, in, net.minecraft.core.Direction.NORTH, player);
		var all = io.github.jimbozoomer.jugcraft.nuclear.AccessControl.entries(post);
		helper.assertTrue(all.size() == 1 && all.get(0).dir().equals("IN") && all.get(0).name().equals(player.getName().getString()),
				"the entry lane writes IN: " + post.acLog);
		helper.assertTrue(io.github.jimbozoomer.jugcraft.nuclear.AccessControl.inside(post).size() == 1, "one inside");
		io.github.jimbozoomer.jugcraft.nuclear.AccessControl.passed(level, out, net.minecraft.core.Direction.SOUTH, player);
		helper.assertTrue(io.github.jimbozoomer.jugcraft.nuclear.AccessControl.inside(post).isEmpty() && post.acLog.size() == 2,
				"the exit lane writes OUT and nobody is inside: " + post.acLog);
		helper.succeed();
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

	/**
	 * Round 26, #10 (Justin: "Real cabinet types", "Unique labels", "Yes, read the reactor"): the medium plant's relay
	 * room holds 12 protection racks, 6 SSPS bays, 5 nuclear instrument racks and 29 relay panels, each kind's labels all
	 * different; ticked, every cabinet but the relay panels reads the plant's reactor (shut down, unfuelled). What they
	 * show: a full-power, a tripped, a cold and an unprotected reactor's meters, bistables, permissives and actuations
	 * (nuclear/RelayRoom.java readings).
	 */
	@GameTest(maxTicks = 40)
	public void mediumPlantRelayRoomReadsTheReactor(GameTestHelper helper) {
		if (TestFocus.skip("nuclear")) {
			helper.succeed();
			return;
		}
		var level = helper.getLevel();
		BlockPos stake = helper.absolutePos(BlockPos.ZERO).offset(5_000, 0, -5_000);
		Blueprint print = Blueprint.get("nuclear_medium_pwr", false);
		PlantBuilder.build(level, print, stake, Rotation.NONE, null, true);
		java.util.Map<Integer, java.util.List<io.github.jimbozoomer.jugcraft.nuclear.LivePanelBlockEntity>> kinds = new java.util.HashMap<>();
		for (int i = 0; i < print.size(); i++) {
			BlockState state = print.state(i, Rotation.NONE);
			int kind = RelayRoom.kind(state);
			BlockPos pos = stake.offset(print.offset(i, Rotation.NONE));
			if (kind >= 0 && level.getBlockEntity(pos) instanceof io.github.jimbozoomer.jugcraft.nuclear.LivePanelBlockEntity cabinet) {
				kinds.computeIfAbsent(kind, k -> new java.util.ArrayList<>()).add(cabinet);
			}
		}
		int[] expect = {12, 6, 5, 29};
		for (int kind = 0; kind < 4; kind++) {
			var found = kinds.getOrDefault(kind, java.util.List.of());
			java.util.Set<Integer> labels = new java.util.HashSet<>();
			for (var c : found) {
				labels.add(io.github.jimbozoomer.jugcraft.nuclear.LivePanelBlockEntity.variant(c.getBlockState()));
			}
			helper.assertTrue(found.size() == expect[kind] && labels.size() == expect[kind], "the relay room's " + RelayRoom.KEYS[kind] + "s, each its own label: "
					+ found.size() + " cabinets, " + labels.size() + " labels");
		}
		for (int kind = 0; kind < 3; kind++) {
			for (var c : kinds.get(kind)) {
				c.serverTick(level);
				helper.assertTrue(c.rxStatus == 2 && c.generator == null, "a " + RelayRoom.KEYS[kind] + " reads the plant's reactor, shut down: "
						+ c.getBlockPos().subtract(stake).toShortString() + " status " + c.rxStatus);
			}
		}
		for (var c : kinds.get(RelayRoom.RELAY)) {
			c.serverTick(level);
			helper.assertTrue(c.rxStatus == 0 && c.generator == null, "a relay panel reads nothing but its name");
		}
		// Full power, steady: Tavg 572 F, PZR 2235 psig, nothing tripped; the permissives of power lit; the source
		// range blocked; the power range at 100 %.
		RelayRoom.Reactor full = new RelayRoom.Reactor(1, 1.0, 1.0, 300, 0, 0, true, 0);
		RelayRoom.Readings tavg = RelayRoom.readings(RelayRoom.PROTECTION, 0, full), pzr = RelayRoom.readings(RelayRoom.PROTECTION, 1, full);
		helper.assertTrue(Math.abs(tavg.m0() - 42) < 0.01 && Math.abs(pzr.m0() - 66.875) < 0.01 && tavg.lamps() == 1 << 4 && pzr.lamps() == 1 << 4,
				"at full power the protection racks read Tavg 572 F and 2235 psig, nothing tripped: " + tavg + ", " + pzr);
		int logic = RelayRoom.readings(RelayRoom.SSPS, 1, full).lamps();
		helper.assertTrue((logic & 0xFFFF) == 0b10111110 && (logic & 1 << 17) != 0 && (logic & 1 << 16) == 0,
				"at power the logic bay shows P-6, P-7, P-8, P-10, P-11, P-13: " + Integer.toBinaryString(logic));
		helper.assertTrue(RelayRoom.readings(RelayRoom.NIS, 0, full).lamps() == (1 << 2 | 1 << 5)
				&& Math.abs(RelayRoom.readings(RelayRoom.NIS, 2, full).m0() - 100 / 1.2) < 0.01, "at power the source range is blocked, the power range reads 100 %");
		// Tripped: the reactor trip, turbine trip, feedwater isolation and auxiliary feed in the logic bay, the trip
		// breakers' undervoltage and shunt coils and the turbine trip relay in the output bay.
		RelayRoom.Reactor trip = new RelayRoom.Reactor(3, 0, 0.06, 290, 100, ReactorCore.A_SCRAM, true, -0.5);
		int tripped = RelayRoom.readings(RelayRoom.SSPS, 1, trip).lamps(), out = RelayRoom.readings(RelayRoom.SSPS, 2, trip).lamps();
		helper.assertTrue((tripped & (1 | 1 << 8 | 1 << 10 | 1 << 14 | 1 << 15)) == (1 | 1 << 8 | 1 << 10 | 1 << 14 | 1 << 15)
				&& (out & (1 | 1 << 11 | 1 << 14)) == (1 | 1 << 11 | 1 << 14), "a trip lights P-4, REACTOR TRIP, FW ISOLATION, AFW START, TURBINE TRIP and K601, K612, K615: "
				+ Integer.toBinaryString(tripped) + " / " + Integer.toBinaryString(out));
		// Cold and shut down: Tavg low-low, pressurizer pressure low and low (SI) tripped; the input bay shows the low
		// pressure on all four channels.
		RelayRoom.Reactor cold = new RelayRoom.Reactor(2, 0, 0, 30, 100, 0, true, 0);
		helper.assertTrue(RelayRoom.readings(RelayRoom.PROTECTION, 0, cold).lamps() == (1 << 2 | 1 << 4)
				&& RelayRoom.readings(RelayRoom.PROTECTION, 1, cold).lamps() == (1 | 1 << 3 | 1 << 4)
				&& (RelayRoom.readings(RelayRoom.SSPS, 0, cold).lamps() & 0xF) == 0xF, "cold, Tavg low-low and PZR pressure low trip on every channel");
		// The console's protection off: GENERAL WARNING.
		RelayRoom.Reactor bypassed = new RelayRoom.Reactor(1, 0.5, 0.5, 300, 30, ReactorCore.A_PROTECTION_OFF, false, 0);
		helper.assertTrue((RelayRoom.readings(RelayRoom.SSPS, 4, bypassed).lamps() & 1 << 16) != 0, "protection off lights GENERAL WARNING");
		helper.assertTrue(RelayRoom.label(RelayRoom.RELAY, 0).equals("RCP 1A 1B MOTORS") && RelayRoom.label(RelayRoom.PROTECTION, 11).equals("CH IV RACK 3"),
				"the cabinets' names by label");
		helper.succeed();
	}

	/**
	 * Round 26, #11 (Justin: "Control Room HVAC Equipment Room", all four machines, "Switch to emergency mode", "Real ducts
	 * with dampers"): the medium plant's HVAC room has its control panel, each train's radiation monitor and toxic gas
	 * analyser, the refrigerant monitor, its six dampers (each tag once) and both trains' filtration units, air handlers,
	 * chillers and pumps. Ticked on AUTO with the plant's reactor shut down it runs normally: train A's air handler,
	 * chiller and pump, its normal outside air damper open, the rest shut or on standby, no alarms. On EMERGENCY both
	 * trains go to filtered air; back on AUTO a reactor trip does the same, and the reset brings it back.
	 */
	@GameTest(maxTicks = 40)
	public void mediumPlantHvacRoomSwitchesToEmergency(GameTestHelper helper) {
		if (TestFocus.skip("nuclear")) {
			helper.succeed();
			return;
		}
		var level = helper.getLevel();
		BlockPos stake = helper.absolutePos(BlockPos.ZERO).offset(6_000, 0, -6_000);
		Blueprint print = Blueprint.get("nuclear_medium_pwr", false);
		PlantBuilder.build(level, print, stake, Rotation.NONE, null, true);
		java.util.List<io.github.jimbozoomer.jugcraft.nuclear.LivePanelBlockEntity> panels = new java.util.ArrayList<>();
		java.util.Map<Integer, java.util.Set<Integer>> monitors = new java.util.HashMap<>();
		java.util.List<BlockPos> dampers = new java.util.ArrayList<>();
		java.util.Set<Integer> tags = new java.util.HashSet<>();
		java.util.List<EquipmentBlockEntity> machines = new java.util.ArrayList<>();
		BlockPos vessel = null;
		for (int i = 0; i < print.size(); i++) {
			BlockState state = print.state(i, Rotation.NONE);
			BlockPos pos = stake.offset(print.offset(i, Rotation.NONE));
			int kind = HvacRoom.kind(state);
			if (kind >= 0 && level.getBlockEntity(pos) instanceof io.github.jimbozoomer.jugcraft.nuclear.LivePanelBlockEntity panel) {
				if (kind == HvacRoom.CONTROL) {
					panels.add(panel);
				} else {
					monitors.computeIfAbsent(kind, k -> new java.util.HashSet<>()).add(io.github.jimbozoomer.jugcraft.nuclear.LivePanelBlockEntity.variant(state));
				}
			}
			if (state.getBlock() instanceof DamperBlock) {
				dampers.add(pos);
				tags.add(state.getValue(DamperBlock.VARIANT) / 2);
			}
			if (level.getBlockEntity(pos) instanceof EquipmentBlockEntity e && e.getBlockPos().equals(pos)) {
				if (e.kind().hvac()) {
					machines.add(e);
				} else if (e.kind().reactor()) {
					vessel = pos;
				}
			}
		}
		helper.assertTrue(panels.size() == 1, "one HVAC control panel: " + panels.size());
		helper.assertTrue(monitors.getOrDefault(HvacRoom.RAD, java.util.Set.of()).size() == 2 && monitors.getOrDefault(HvacRoom.TOX, java.util.Set.of()).size() == 2
				&& monitors.getOrDefault(HvacRoom.REFRIG, java.util.Set.of()).size() == 1, "each intake's radiation monitor and toxic gas analyser (A and B), the refrigerant monitor: " + monitors);
		helper.assertTrue(dampers.size() == 6 && tags.size() == 6, "six dampers, each tag once: " + dampers.size() + " dampers, tags " + tags);
		java.util.Map<String, Integer> count = new java.util.HashMap<>();
		for (EquipmentBlockEntity m : machines) {
			count.merge(m.kind().id + HvacRoom.train(m.getBlockState()), 1, Integer::sum);
		}
		helper.assertTrue(count.size() == 8 && count.values().stream().allMatch(n -> n == 1), "each train's filtration unit, air handler, chiller and pump: " + count);
		helper.assertTrue(vessel != null && level.getBlockEntity(vessel) instanceof EquipmentBlockEntity, "the plant's reactor");
		EquipmentBlockEntity reactor = (EquipmentBlockEntity) level.getBlockEntity(vessel);
		var panel = panels.get(0);
		helper.assertTrue(panel.mode == HvacRoom.AUTO, "the panel comes on AUTO");
		// Normal: train A runs, B stands by; only CR-D-1A open.
		HvacRoom.tick(panel, level, HvacRoom.CONTROL);
		for (EquipmentBlockEntity m : machines) {
			int train = HvacRoom.train(m.getBlockState());
			boolean want = m.kind() != EquipmentKind.CR_FILTRATION_UNIT && train == 0;
			helper.assertTrue(m.running == want, "normal: " + HvacRoom.tag(HvacRoom.part(m.kind()), train) + (want ? " runs" : " stands by"));
		}
		for (BlockPos d : dampers) {
			int v = level.getBlockState(d).getValue(DamperBlock.VARIANT);
			helper.assertTrue(((v & 1) != 0) == (v / 2 == 0), "normal: " + HvacRoom.describeDamper(v));
		}
		helper.assertTrue((panel.hvAlarms & 0x1FF) == 0 && Integer.bitCount(panel.hvState >> HvacRoom.PRESENT) == 14,
				"normal: no alarms, every damper and machine found: alarms " + Integer.toBinaryString(panel.hvAlarms) + ", state " + Integer.toBinaryString(panel.hvState));
		// The selector to EMERGENCY: both trains on filtered air.
		panel.mode = HvacRoom.EMERGENCY;
		HvacRoom.tick(panel, level, HvacRoom.CONTROL);
		helper.assertTrue(machines.stream().allMatch(m -> m.running), "emergency: every machine runs");
		for (BlockPos d : dampers) {
			int v = level.getBlockState(d).getValue(DamperBlock.VARIANT);
			helper.assertTrue(((v & 1) != 0) == (v / 2 >= 2), "emergency: " + HvacRoom.describeDamper(v));
		}
		helper.assertTrue((panel.hvAlarms & (HvacRoom.EMERGENCY_ON | HvacRoom.A_EMERGENCY)) == (HvacRoom.EMERGENCY_ON | HvacRoom.A_EMERGENCY)
				&& (panel.hvAlarms & (HvacRoom.A_LOW_FLOW | HvacRoom.A_LOW_DP | HvacRoom.A_CHILLER)) == 0, "emergency: EMERG MODE lit, the room pressurized");
		// Back to AUTO: a reactor trip puts it in emergency, the reset brings it back.
		panel.mode = HvacRoom.AUTO;
		reactor.core.scrammed = true;
		HvacRoom.tick(panel, level, HvacRoom.CONTROL);
		helper.assertTrue((panel.hvAlarms & (HvacRoom.EMERGENCY_ON | HvacRoom.A_RX_TRIP)) == (HvacRoom.EMERGENCY_ON | HvacRoom.A_RX_TRIP), "AUTO: a trip goes to emergency");
		reactor.core.scrammed = false;
		HvacRoom.tick(panel, level, HvacRoom.CONTROL);
		helper.assertTrue((panel.hvAlarms & HvacRoom.EMERGENCY_ON) == 0 && machines.stream().noneMatch(m -> m.running && m.kind() == EquipmentKind.CR_FILTRATION_UNIT),
				"AUTO: reset, back to normal");
		// What the annunciator makes of a missing chiller, a dead room, high radiation at an intake.
		int noChiller = 1 << HvacRoom.PRESENT + HvacRoom.AHU | 1 << HvacRoom.AHU;
		RelayRoom.Reactor off = new RelayRoom.Reactor(2, 0, 0, 30, 100, 0, true, 0);
		helper.assertTrue((HvacRoom.alarms(off, false, false, noChiller) & HvacRoom.A_CHILLER) != 0, "an air handler running without its chiller: CHILLER TRIP");
		helper.assertTrue((HvacRoom.alarms(off, false, false, 0) & HvacRoom.A_LOW_DP) != 0, "no air handler running: CR LOW DIFF PRESS");
		helper.assertTrue(HvacRoom.emergency(HvacRoom.AUTO, off, true) && !HvacRoom.emergency(HvacRoom.NORMAL, off, true), "high radiation at an intake: emergency on AUTO, not on NORMAL");
		helper.assertTrue(HvacRoom.countRate(0, off) == HvacRoom.RAD_BACKGROUND && HvacRoom.countRate(0.05, off) > HvacRoom.RAD_HIGH,
				"the radiation monitors read background, and 50 uSv/h above their HIGH");
		helper.succeed();
	}

	/**
	 * Round 26 (#12; Justin: "Emergency class board", "Plant data video wall", "Desk screens and clocks"): the medium
	 * plant's Technical Support Center has its classification board, the video wall's three displays, the seven consoles,
	 * the communications console, the NRC desk, the four status boards, the area monitor and the frisker, each finding the
	 * board. The board reads the plant: its CR HVAC panel and both intake monitors, both 480 V load centres. With the
	 * reactor shut down there is no emergency; a trip is an UNUSUAL EVENT, the core over its high temperature alarm an
	 * ALERT (the TSC activated), each logged; the class never steps down while it lasts, and once everything has been
	 * clear for five minutes the emergency is terminated. Damaged fuel is a SITE AREA EMERGENCY, with the core at its
	 * damage temperature a GENERAL EMERGENCY.
	 */
	@GameTest(maxTicks = 40)
	public void mediumPlantTscClassifiesTheEmergency(GameTestHelper helper) {
		if (TestFocus.skip("nuclear")) {
			helper.succeed();
			return;
		}
		var level = helper.getLevel();
		BlockPos stake = helper.absolutePos(BlockPos.ZERO).offset(-6_000, 0, 6_000);
		Blueprint print = Blueprint.get("nuclear_medium_pwr", false);
		PlantBuilder.build(level, print, stake, Rotation.NONE, null, true);
		java.util.Map<Integer, java.util.List<Integer>> found = new java.util.HashMap<>();
		java.util.List<io.github.jimbozoomer.jugcraft.nuclear.LivePanelBlockEntity> panels = new java.util.ArrayList<>();
		io.github.jimbozoomer.jugcraft.nuclear.LivePanelBlockEntity hub = null;
		BlockPos vessel = null;
		for (int i = 0; i < print.size(); i++) {
			BlockState state = print.state(i, Rotation.NONE);
			BlockPos pos = stake.offset(print.offset(i, Rotation.NONE));
			int kind = io.github.jimbozoomer.jugcraft.nuclear.TscRoom.kind(state);
			if (kind >= 0 && level.getBlockEntity(pos) instanceof io.github.jimbozoomer.jugcraft.nuclear.LivePanelBlockEntity panel) {
				found.computeIfAbsent(kind, k -> new java.util.ArrayList<>()).add(io.github.jimbozoomer.jugcraft.nuclear.LivePanelBlockEntity.variant(state));
				if (kind == io.github.jimbozoomer.jugcraft.nuclear.TscRoom.HUB) {
					hub = panel;
				} else {
					panels.add(panel);
				}
			}
			if (level.getBlockEntity(pos) instanceof EquipmentBlockEntity e && e.getBlockPos().equals(pos) && e.kind().reactor()) {
				vessel = pos;
			}
		}
		java.util.Map<Integer, java.util.List<Integer>> want = java.util.Map.of(0, java.util.List.of(0), 1, java.util.List.of(0, 1, 2), 2,
				java.util.List.of(0, 1, 2, 3, 4, 5, 6), 3, java.util.List.of(0), 4, java.util.List.of(0), 5, java.util.List.of(0, 1, 2, 3), 6, java.util.List.of(0), 7,
				java.util.List.of(0));
		for (var e : want.entrySet()) {
			java.util.List<Integer> got = new java.util.ArrayList<>(found.getOrDefault(e.getKey(), java.util.List.of()));
			java.util.Collections.sort(got);
			helper.assertTrue(got.equals(e.getValue()), io.github.jimbozoomer.jugcraft.nuclear.TscRoom.KEYS[e.getKey()] + ": labels " + got + ", not " + e.getValue());
		}
		helper.assertTrue(hub != null && vessel != null, "the TSC's board and the plant's reactor");
		EquipmentBlockEntity reactor = (EquipmentBlockEntity) level.getBlockEntity(vessel);
		for (var p : panels) {
			io.github.jimbozoomer.jugcraft.nuclear.TscRoom.tick(p, level, io.github.jimbozoomer.jugcraft.nuclear.TscRoom.kind(p.getBlockState()));
			int kind = io.github.jimbozoomer.jugcraft.nuclear.TscRoom.kind(p.getBlockState());
			helper.assertTrue(kind >= io.github.jimbozoomer.jugcraft.nuclear.TscRoom.AREA || hub.getBlockPos().equals(p.panel),
					io.github.jimbozoomer.jugcraft.nuclear.TscRoom.KEYS[kind] + " at " + p.getBlockPos() + " finds the board");
		}
		// No emergency; the board finds the CR HVAC panel, both intakes and both load centres.
		io.github.jimbozoomer.jugcraft.nuclear.TscRoom.tick(hub, level, io.github.jimbozoomer.jugcraft.nuclear.TscRoom.HUB);
		int need = io.github.jimbozoomer.jugcraft.nuclear.TscRoom.B_HVAC | io.github.jimbozoomer.jugcraft.nuclear.TscRoom.B_INTAKE_A
				| io.github.jimbozoomer.jugcraft.nuclear.TscRoom.B_INTAKE_B | io.github.jimbozoomer.jugcraft.nuclear.TscRoom.B_LC_A
				| io.github.jimbozoomer.jugcraft.nuclear.TscRoom.B_LC_B;
		helper.assertTrue((hub.tsBits & need) == need, "the board finds the CR HVAC, its intakes and the load centres: " + Integer.toBinaryString(hub.tsBits));
		helper.assertTrue(hub.tsClass == 0 && hub.tsDeclared < 0, "no emergency with the reactor shut down: class " + hub.tsClass);
		// A trip: an UNUSUAL EVENT; over the high temperature alarm: an ALERT, the TSC activated.
		reactor.core.scrammed = true;
		io.github.jimbozoomer.jugcraft.nuclear.TscRoom.tick(hub, level, io.github.jimbozoomer.jugcraft.nuclear.TscRoom.HUB);
		helper.assertTrue(hub.tsClass == 1 && hub.tsDeclared >= 0 && hub.tsStart >= 0, "a trip: an UNUSUAL EVENT, class " + hub.tsClass);
		reactor.core.temperature = io.github.jimbozoomer.jugcraft.nuclear.ReactorCore.ALARM_TEMP + 5;
		io.github.jimbozoomer.jugcraft.nuclear.TscRoom.tick(hub, level, io.github.jimbozoomer.jugcraft.nuclear.TscRoom.HUB);
		helper.assertTrue(hub.tsClass == 2 && hub.tsActivated >= 0, "the core over its alarm: an ALERT, the TSC activated: class " + hub.tsClass);
		java.util.List<String> log = new java.util.ArrayList<>();
		for (long e : hub.tsEvents) {
			log.add(io.github.jimbozoomer.jugcraft.nuclear.TscRoom.EVENTS[(int) (e & 31)]);
		}
		helper.assertTrue(log.containsAll(java.util.List.of("REACTOR TRIP", "NOUE DECLARED", "HIGH CORE TEMP", "ALERT DECLARED", "TSC ACTIVATED")), "the log: " + log);
		var view = new io.github.jimbozoomer.jugcraft.nuclear.TscRoom.View(hub, level.getOverworldClockTime(), false, false, new int[] {0, 0});
		helper.assertTrue(view.csf[1] == 2 && view.priorities.contains("RESTORE CORE COOLING") && !view.brief.isEmpty() && "NONE".equals(view.par[0]),
				"core cooling challenged, its priority, the next brief, no PAR at an ALERT");
		// The reactor reset and cooled: still an ALERT until everything has been clear five minutes, then terminated.
		reactor.core.scrammed = false;
		reactor.core.temperature = 30;
		io.github.jimbozoomer.jugcraft.nuclear.TscRoom.tick(hub, level, io.github.jimbozoomer.jugcraft.nuclear.TscRoom.HUB);
		helper.assertTrue(hub.tsClass == 2, "the class doesn't step down while the event lasts");
		for (int i = 0; i < io.github.jimbozoomer.jugcraft.nuclear.TscRoom.TERMINATE / 20 + 1; i++) {
			io.github.jimbozoomer.jugcraft.nuclear.TscRoom.tick(hub, level, io.github.jimbozoomer.jugcraft.nuclear.TscRoom.HUB);
		}
		helper.assertTrue(hub.tsClass == 0 && hub.tsDeclared < 0 && (hub.tsEvents.get(hub.tsEvents.size() - 1) & 31) == io.github.jimbozoomer.jugcraft.nuclear.TscRoom.E_TERMINATED,
				"clear five minutes: the emergency terminated");
		// Damaged fuel: a SITE AREA EMERGENCY; at the damage temperature a GENERAL EMERGENCY.
		RelayRoom.Reactor damaged = new RelayRoom.Reactor(3, 0, 0, 400, 100, io.github.jimbozoomer.jugcraft.nuclear.ReactorCore.A_FUEL_DAMAGED, true, 0);
		RelayRoom.Reactor melting = new RelayRoom.Reactor(3, 0, 0, io.github.jimbozoomer.jugcraft.nuclear.ReactorCore.DAMAGE_TEMP, 100,
				io.github.jimbozoomer.jugcraft.nuclear.ReactorCore.A_FUEL_DAMAGED, true, 0);
		double[] calm = new double[io.github.jimbozoomer.jugcraft.nuclear.TscRoom.NUMS];
		helper.assertTrue(io.github.jimbozoomer.jugcraft.nuclear.TscRoom.condition(damaged, 0, calm) == 3
				&& io.github.jimbozoomer.jugcraft.nuclear.TscRoom.condition(melting, 0, calm) == 4, "damaged fuel: SAE; at its damage temperature: GE");
		helper.assertTrue("06:00".equals(io.github.jimbozoomer.jugcraft.nuclear.TscRoom.clock(0)) && "12:30".equals(io.github.jimbozoomer.jugcraft.nuclear.TscRoom.clock(6_500))
				&& "01:15".equals(io.github.jimbozoomer.jugcraft.nuclear.TscRoom.span(1_250)), "the clocks: the game's day from 06:00, an hour a 1,000 ticks");
		helper.succeed();
	}

	/**
	 * Round 26 (#13; Justin: "Work control + tagout", "Status screen + out-of-service board"): the medium plant's
	 * operations office has its out-of-service board, both work control consoles (work control, tagout), the schedule
	 * board, the master clock, the radio rack and the PPE shelf, the consoles and schedule board finding the board. As
	 * built nothing is out of service. A load centre's INVERTER feeder opened goes on the board with its clearance number
	 * and an LCO; a battery room fan switched OFF comes next with the next number; the feeder closed again leaves the
	 * board and the fan keeps its number. The plant's mode, the crew on shift.
	 */
	@GameTest(maxTicks = 40)
	public void mediumPlantOpsOfficeTracksEquipmentOut(GameTestHelper helper) {
		if (TestFocus.skip("nuclear")) {
			helper.succeed();
			return;
		}
		var level = helper.getLevel();
		BlockPos stake = helper.absolutePos(BlockPos.ZERO).offset(-6_000, 0, 9_000);
		Blueprint print = Blueprint.get("nuclear_medium_pwr", false);
		PlantBuilder.build(level, print, stake, Rotation.NONE, null, true);
		java.util.Map<Integer, java.util.List<Integer>> found = new java.util.HashMap<>();
		java.util.List<io.github.jimbozoomer.jugcraft.nuclear.LivePanelBlockEntity> linked = new java.util.ArrayList<>();
		io.github.jimbozoomer.jugcraft.nuclear.LivePanelBlockEntity hub = null;
		for (int i = 0; i < print.size(); i++) {
			BlockState state = print.state(i, Rotation.NONE);
			BlockPos pos = stake.offset(print.offset(i, Rotation.NONE));
			int kind = io.github.jimbozoomer.jugcraft.nuclear.OpsOffice.kind(state);
			if (kind >= 0 && level.getBlockEntity(pos) instanceof io.github.jimbozoomer.jugcraft.nuclear.LivePanelBlockEntity panel) {
				found.computeIfAbsent(kind, k -> new java.util.ArrayList<>()).add(io.github.jimbozoomer.jugcraft.nuclear.LivePanelBlockEntity.variant(state));
				if (kind == io.github.jimbozoomer.jugcraft.nuclear.OpsOffice.HUB) {
					hub = panel;
				} else if (kind == io.github.jimbozoomer.jugcraft.nuclear.OpsOffice.CONSOLE || kind == io.github.jimbozoomer.jugcraft.nuclear.OpsOffice.SCHEDULE) {
					linked.add(panel);
				}
			}
		}
		java.util.Map<Integer, java.util.List<Integer>> want = java.util.Map.of(0, java.util.List.of(0), 1, java.util.List.of(0, 1), 2, java.util.List.of(0),
				3, java.util.List.of(0), 4, java.util.List.of(0), 5, java.util.List.of(0));
		for (var e : want.entrySet()) {
			java.util.List<Integer> got = new java.util.ArrayList<>(found.getOrDefault(e.getKey(), java.util.List.of()));
			java.util.Collections.sort(got);
			helper.assertTrue(got.equals(e.getValue()), io.github.jimbozoomer.jugcraft.nuclear.OpsOffice.KEYS[e.getKey()] + ": labels " + got + ", not " + e.getValue());
		}
		helper.assertTrue(hub != null, "the out-of-service board");
		for (var p : linked) {
			io.github.jimbozoomer.jugcraft.nuclear.OpsOffice.tick(p, level, io.github.jimbozoomer.jugcraft.nuclear.OpsOffice.kind(p.getBlockState()));
			helper.assertTrue(hub.getBlockPos().equals(p.panel), "the panel at " + p.getBlockPos() + " finds the board");
		}
		io.github.jimbozoomer.jugcraft.nuclear.OpsOffice.tick(hub, level, io.github.jimbozoomer.jugcraft.nuclear.OpsOffice.HUB);
		helper.assertTrue(hub.opsEntries.isEmpty(), "as built nothing is out of service: " + hub.opsEntries);
		// The load centres (west to east, as the board orders them) and the battery rooms' fans.
		java.util.List<BlockPos> lcs = new java.util.ArrayList<>();
		for (var e : io.github.jimbozoomer.jugcraft.nuclear.EquipmentRegistry.of(level).anchors().entrySet()) {
			if (e.getValue().kind() == io.github.jimbozoomer.jugcraft.nuclear.EquipmentKind.SWITCHGEAR_LINEUP && e.getKey().distSqr(hub.getBlockPos()) < 192 * 192) {
				lcs.add(e.getKey());
			}
		}
		lcs.sort(java.util.Comparator.<BlockPos>comparingInt(p -> p.getX()).thenComparingInt(p -> p.getZ()).thenComparingInt(p -> p.getY()));
		helper.assertTrue(lcs.size() == 2, "both load centres: " + lcs.size());
		EquipmentBlockEntity lc = (EquipmentBlockEntity) level.getBlockEntity(lcs.get(0));
		int inverter = java.util.Arrays.asList(io.github.jimbozoomer.jugcraft.nuclear.Switchgear.NAMES).indexOf("INVERTER");
		lc.breakers &= ~(1 << inverter);
		io.github.jimbozoomer.jugcraft.nuclear.OpsOffice.tick(hub, level, io.github.jimbozoomer.jugcraft.nuclear.OpsOffice.HUB);
		var list = io.github.jimbozoomer.jugcraft.nuclear.OpsOffice.entries(hub);
		helper.assertTrue(list.size() == 1 && list.get(0).equipment().equals("480V LC 1A  FDR INVERTER") && list.get(0).lco() == 2
				&& list.get(0).number() == io.github.jimbozoomer.jugcraft.nuclear.OpsOffice.FIRST_CLEARANCE && "A".equals(list.get(0).train()),
				"the inverter feeder out, with its clearance and LCO: " + hub.opsEntries);
		io.github.jimbozoomer.jugcraft.nuclear.LivePanelBlockEntity fan = null;
		for (var p : hub.panelsAround(level, io.github.jimbozoomer.jugcraft.nuclear.PlantInterior.BLOCKS.get("exhaust_fan"), 64)) {
			fan = p;
			break;
		}
		helper.assertTrue(fan != null, "a battery room fan in reach");
		fan.mode = io.github.jimbozoomer.jugcraft.nuclear.ExhaustFanBlock.OFF;
		io.github.jimbozoomer.jugcraft.nuclear.OpsOffice.tick(hub, level, io.github.jimbozoomer.jugcraft.nuclear.OpsOffice.HUB);
		list = io.github.jimbozoomer.jugcraft.nuclear.OpsOffice.entries(hub);
		helper.assertTrue(list.size() == 2 && list.get(1).equipment().startsWith("EXH FAN") && list.get(1).number() == list.get(0).number() + 1
				&& list.get(1).lco() == 0, "the fan next, with the next number: " + hub.opsEntries);
		int fanNumber = list.get(1).number();
		lc.breakers |= 1 << inverter;
		io.github.jimbozoomer.jugcraft.nuclear.OpsOffice.tick(hub, level, io.github.jimbozoomer.jugcraft.nuclear.OpsOffice.HUB);
		list = io.github.jimbozoomer.jugcraft.nuclear.OpsOffice.entries(hub);
		helper.assertTrue(list.size() == 1 && list.get(0).number() == fanNumber, "the feeder back in service; the fan keeps its number: " + hub.opsEntries);
		fan.mode = io.github.jimbozoomer.jugcraft.nuclear.ExhaustFanBlock.AUTO;
		helper.assertTrue(io.github.jimbozoomer.jugcraft.nuclear.OpsOffice.mode(hub.reactor()).startsWith("5"), "a cold reactor: mode 5");
		int[] day = io.github.jimbozoomer.jugcraft.nuclear.OpsOffice.shift(0), night = io.github.jimbozoomer.jugcraft.nuclear.OpsOffice.shift(12_000);
		helper.assertTrue(day[0] == 0 && day[1] == 0 && night[0] == 1 && night[1] == 1, "Monday: crew A on days, B on nights");
		for (int d = 0; d < 7; d++) {
			int days = 0, nights = 0;
			for (String row : io.github.jimbozoomer.jugcraft.nuclear.OpsOffice.ROTATION) {
				days += row.charAt(d) == 'D' ? 1 : 0;
				nights += row.charAt(d) == 'N' ? 1 : 0;
			}
			helper.assertTrue(days == 1 && nights == 1, "one crew on days and one on nights each day: " + d);
		}
		helper.succeed();
	}

	/**
	 * The restroom (round 26, #14; Justin: "2 stalls, urinal, 2 sinks", every fixture working): as built, its fixtures
	 * are there; a toilet gives a roll of paper and flushes; a sink runs; the trash can throws away what is put in it
	 * and fills up; a stall door latches from inside, holds against outside, opens from inside, and a latched door with
	 * nobody in the stall unlatches.
	 */

	/**
	 * Round 31 (the diesel rooms rebuilt): each of the medium plant's two diesel generators takes its own room's
	 * starting air system (two receivers, two compressors, the dryer; labelled 1A or 1B by room), a start takes 20 psig
	 * from its receivers, and its room's panels link to it; its steps stand on half-block frames.
	 */
	@GameTest(maxTicks = 40)
	public void mediumPlantDieselRoomsStartOnAir(GameTestHelper helper) {
		if (TestFocus.skip("nuclear")) {
			helper.succeed();
			return;
		}
		var level = helper.getLevel();
		BlockPos stake = helper.absolutePos(BlockPos.ZERO).offset(-7_000, 0, 21_000);
		Blueprint print = Blueprint.get("nuclear_medium_pwr", false);
		PlantBuilder.build(level, print, stake, Rotation.NONE, null, true);
		java.util.List<EquipmentBlockEntity> edgs = new java.util.ArrayList<>();
		for (var e : io.github.jimbozoomer.jugcraft.nuclear.EquipmentRegistry.of(level).anchors().entrySet()) {
			if (e.getValue().kind() == EquipmentKind.EMERGENCY_DIESEL_GENERATOR && e.getKey().distSqr(stake) < 400 * 400
					&& level.getBlockEntity(e.getKey()) instanceof EquipmentBlockEntity edg) {
				edgs.add(edg);
			}
		}
		helper.assertTrue(edgs.size() == 2, "two diesel generators: " + edgs.size());
		java.util.Set<Integer> trains = new java.util.HashSet<>();
		for (EquipmentBlockEntity edg : edgs) {
			long t = level.getGameTime();
			for (int k = 0; k < 40; k++) {
				io.github.jimbozoomer.jugcraft.nuclear.StartingAir.tick(level, edg, t + k);
			}
			helper.assertTrue(edg.airReceivers == 2 && edg.airPieces.size() == 5, "its room's starting air: " + edg.airPieces.size() + " pieces, "
					+ edg.airReceivers + " receivers");
			int train = edg.getBlockState().getValue(io.github.jimbozoomer.jugcraft.nuclear.EquipmentAnchorBlock.VARIANT);
			trains.add(train);
			for (BlockPos piece : edg.airPieces) {
				BlockState state = level.getBlockState(piece);
				int label = state.getValue(io.github.jimbozoomer.jugcraft.nuclear.EquipmentAnchorBlock.VARIANT);
				boolean dryer = state.getBlock() instanceof io.github.jimbozoomer.jugcraft.nuclear.EquipmentAnchorBlock a && a.kind == EquipmentKind.EDG_AIR_DRYER;
				helper.assertTrue(dryer ? label == train : label / 2 == train, "its pieces are labelled for its train: " + label + " on train " + train);
			}
			// Its steps: the first down from the deck a block and a half, the last a half.
			BlockPos step = edg.getBlockPos().offset(EquipmentKind.toWorld(new BlockPos(0, 1, 12), edg.facing()));
			BlockPos last = edg.getBlockPos().offset(EquipmentKind.toWorld(new BlockPos(9, 0, 14), edg.facing()));
			helper.assertTrue(level.getBlockState(step).getValue(io.github.jimbozoomer.jugcraft.nuclear.EquipmentFrameBlock.SHAPE)
					== io.github.jimbozoomer.jugcraft.nuclear.EquipmentFrameBlock.Shape.HALF
					&& level.getBlockState(last).getValue(io.github.jimbozoomer.jugcraft.nuclear.EquipmentFrameBlock.SHAPE)
					== io.github.jimbozoomer.jugcraft.nuclear.EquipmentFrameBlock.Shape.HALF, "its steps end in half blocks");
			// A start takes its air (whether or not its day tank has fuel yet).
			io.github.jimbozoomer.jugcraft.nuclear.EquipmentPlacement.dieselSwitch(level, edg, null, true);
			helper.assertTrue(Math.abs(edg.air - 230) < 1e-6 && !edg.airFail && (edg.running || edg.fuelOut), "a start takes 20 psig: " + edg.air);
			io.github.jimbozoomer.jugcraft.nuclear.EquipmentPlacement.dieselSwitch(level, edg, null, false);
		}
		helper.assertTrue(trains.equals(java.util.Set.of(0, 1)), "one generator per train: " + trains);
		// Round 31 part 3: each room's own plant joins its generator (two keep-warm skids, the expansion tank, the cooler,
		// the resistor, the cubicle, the CO2 bank, the monorail).
		for (EquipmentBlockEntity edg : edgs) {
			long t = level.getGameTime();
			for (int k = 0; k < 40; k++) {
				io.github.jimbozoomer.jugcraft.nuclear.DieselRoom.tick(level, edg, t + k);
			}
			helper.assertTrue(edg.roomPieces.size() == 8, "its room's own plant: " + edg.roomPieces.size() + " pieces");
		}
		// The engine panels link to their own rooms' generators.
		java.util.Set<BlockPos> linked = new java.util.HashSet<>();
		for (int i = 0; i < print.size(); i++) {
			BlockState state = print.state(i, Rotation.NONE);
			BlockPos pos = stake.offset(print.offset(i, Rotation.NONE));
			if (state.is(PlantInterior.BLOCKS.get("edg_engine_panel")) && level.getBlockEntity(pos) instanceof io.github.jimbozoomer.jugcraft.nuclear.LivePanelBlockEntity panel) {
				panel.serverTick(level);
				if (panel.generator != null) {
					linked.add(panel.generator);
				}
			}
		}
		helper.assertTrue(linked.size() == 2, "each room's engine panel links to its own generator: " + linked);
		helper.succeed();
	}

	/**
	 * Diesels make real power (27 October 2026) in the medium plant: a strike trips the switchyard's lines; the reactor
	 * sees the grid lost, both safety buses shed their RES XFMR breakers and both diesels (at AUTO, their day tanks
	 * filled here) start by themselves and close onto their buses five seconds on; RES XFMR A won't close onto the dead
	 * grid; with the lines back it closes, putting bus 1A back on the grid and
	 * taking diesel A off it, still running.
	 */
	@GameTest(maxTicks = 1200)
	public void mediumPlantLosesOffsitePower(GameTestHelper helper) {
		if (TestFocus.skip("nuclear")) {
			helper.succeed();
			return;
		}
		var level = helper.getLevel();
		BlockPos stake = helper.absolutePos(BlockPos.ZERO).offset(-30_000, 0, 18_000);
		PlantBuilder.build(level, Blueprint.get("nuclear_medium_pwr", false), stake, Rotation.NONE, null, true);
		long[] phase = {0, 0};
		java.util.List<EquipmentBlockEntity> edgs = new java.util.ArrayList<>();
		EquipmentBlockEntity[] found = new EquipmentBlockEntity[2];
		helper.succeedWhen(() -> {
			long now = level.getGameTime();
			if (found[0] == null) {
				for (var e : io.github.jimbozoomer.jugcraft.nuclear.EquipmentRegistry.of(level).anchors().entrySet()) {
					if (e.getKey().distSqr(stake) < 400 * 400 && level.getBlockEntity(e.getKey()) instanceof EquipmentBlockEntity b) {
						if (b.kind().reactor()) found[0] = b;
						if (b.kind() == EquipmentKind.ELECTRICAL_BOARD) found[1] = b;
						if (b.kind() == EquipmentKind.EMERGENCY_DIESEL_GENERATOR) edgs.add(b);
					}
				}
				edgs.sort(java.util.Comparator.<EquipmentBlockEntity>comparingInt(b -> b.getBlockPos().getX()).thenComparingInt(b -> b.getBlockPos().getZ()));
			}
			EquipmentBlockEntity vessel = found[0], electrical = found[1];
			helper.assertTrue(vessel != null && electrical != null && edgs.size() == 2, "the reactor, the electrical board and two diesels");
			ReactorCore core = vessel.core;
			var sw = io.github.jimbozoomer.jugcraft.nuclear.Yards.nearest(level, vessel.getBlockPos(), io.github.jimbozoomer.jugcraft.nuclear.YardBlock.Works.SWITCHYARD);
			helper.assertTrue(vessel.unit() != null && sw != null, "its unit formed and its switchyard found");
			var es = io.github.jimbozoomer.jugcraft.nuclear.ControlBoard.refresh(level, electrical);
			var layout = io.github.jimbozoomer.jugcraft.nuclear.ControlBoard.layout(electrical.kind());
			ServerPlayer player = helper.makeMockServerPlayerInLevel();
			if (phase[0] == 0) {
				helper.assertTrue((core.power & ReactorCore.PW_GRID) != 0 && core.resOpen == 0, "built on the grid, both RES breakers closed");
				for (EquipmentBlockEntity edg : edgs) {
					var tank = io.github.jimbozoomer.jugcraft.nuclear.DieselFuel.dayTank(level, edg);
					helper.assertTrue(tank != null, "each diesel's day tank");
					try (var t = net.fabricmc.fabric.api.transfer.v1.transaction.Transaction.openOuter()) {
						tank.portStorage().insert(io.github.jimbozoomer.jugcraft.nuclear.DieselFuel.diesel(), 81_000L * 4_000, t);
						t.commit();
					}
				}
				sw.tripLines(level, 400);
				phase[0] = 1;
				phase[1] = now;
			}
			if (phase[0] == 1) {
				helper.assertTrue(now - phase[1] > 20, "a second on");
				helper.assertTrue((core.power & ReactorCore.PW_GRID) == 0 && core.resOpen == 3, "the grid lost: both buses shed their RES breakers: " + core.resOpen);
				for (EquipmentBlockEntity edg : edgs) {
					helper.assertTrue(edg.running, "each diesel started by itself (fuel out " + edg.fuelOut + ", air fail " + edg.airFail + ", "
							+ io.github.jimbozoomer.jugcraft.nuclear.DieselPanels.refuse(edg, true, io.github.jimbozoomer.jugcraft.nuclear.DieselPanels.REMOTE) + ")");
				}
				helper.assertTrue(!io.github.jimbozoomer.jugcraft.nuclear.ControlBoard.act(level, electrical, es, layout, player, "res_a", now) && core.resOpen == 3,
						"RES XFMR A won't close onto the dead grid");
				phase[0] = 2;
				phase[1] = now;
			}
			if (phase[0] == 2) {
				helper.assertTrue(now - phase[1] > 120, "the diesels' breakers closing");
				helper.assertTrue(edgs.get(0).breakerClosed(now) && edgs.get(1).breakerClosed(now), "both diesels' breakers closed");
				helper.assertTrue((core.power & ReactorCore.PW_1A) != 0 && (core.power & ReactorCore.PW_1B) != 0 && !core.plantBus(),
						"the safety buses live on their diesels, the plant's own buses dead");
				phase[0] = 3;
			}
			if (phase[0] == 3) {
				helper.assertTrue(sw.gridLive(now) && (core.power & ReactorCore.PW_GRID) != 0 && core.plantBus(), "the lines reclosed: the grid and the plant's buses back");
				helper.assertTrue(core.resOpen == 3 && edgs.get(0).breakerClosed(now), "the safety buses still on their diesels");
				helper.assertTrue(io.github.jimbozoomer.jugcraft.nuclear.ControlBoard.act(level, electrical, es, layout, player, "res_a", now) && core.resOpen == 2,
						"RES XFMR A closes");
				phase[0] = 4;
				phase[1] = now;
			}
			helper.assertTrue(now > phase[1], "a tick on");
			helper.assertTrue(!edgs.get(0).breakerClosed(now) && edgs.get(0).running && edgs.get(1).breakerClosed(now),
					"bus 1A back on the grid: diesel A off it, still running; B still carrying 1B");
			for (EquipmentBlockEntity edg : edgs) {
				io.github.jimbozoomer.jugcraft.nuclear.EquipmentPlacement.dieselSwitch(level, edg, null, false);
			}
		});
	}

	/**
	 * The control room's live boards (round 27) in the medium plant: each finds the plant's reactor, its diesel
	 * generators and its load centre; the reactor coolant board stops and starts a coolant pump, the turbine board's
	 * trip works through its guard and LATCH relatches the turbine, the generator breaker won't close without steam,
	 * and an alarm runs the annunciator's sequence with ACKNOWLEDGE and RESET.
	 */
	@GameTest(maxTicks = 400)
	public void mediumPlantBoardsLive(GameTestHelper helper) {
		if (TestFocus.skip("nuclear")) {
			helper.succeed();
			return;
		}
		var level = helper.getLevel();
		BlockPos stake = helper.absolutePos(BlockPos.ZERO).offset(-6_000, 0, 18_000);
		Blueprint print = Blueprint.get("nuclear_medium_pwr", false);
		PlantBuilder.build(level, print, stake, Rotation.NONE, null, true);
		java.util.Map<io.github.jimbozoomer.jugcraft.nuclear.EquipmentKind, io.github.jimbozoomer.jugcraft.nuclear.EquipmentBlockEntity> boards = new java.util.HashMap<>();
		for (var e : io.github.jimbozoomer.jugcraft.nuclear.EquipmentRegistry.of(level).anchors().entrySet()) {
			if (e.getValue().kind().liveBoard() && e.getKey().distSqr(stake) < 400 * 400
					&& level.getBlockEntity(e.getKey()) instanceof io.github.jimbozoomer.jugcraft.nuclear.EquipmentBlockEntity b) {
				boards.put(b.kind(), b);
			}
		}
		helper.assertTrue(boards.size() == 9, "the six live boards, the remote shutdown panel, the reactor console and (round 32) the plant display wall: "
				+ boards.keySet());
		var rcs = boards.get(io.github.jimbozoomer.jugcraft.nuclear.EquipmentKind.RCS_BOARD);
		var turbine = boards.get(io.github.jimbozoomer.jugcraft.nuclear.EquipmentKind.TURBINE_BOARD);
		var electrical = boards.get(io.github.jimbozoomer.jugcraft.nuclear.EquipmentKind.ELECTRICAL_BOARD);
		var rs = io.github.jimbozoomer.jugcraft.nuclear.ControlBoard.refresh(level, rcs);
		helper.assertTrue(rs.plant.present, "the reactor coolant board finds the reactor");
		var es = io.github.jimbozoomer.jugcraft.nuclear.ControlBoard.refresh(level, electrical);
		helper.assertTrue(es.plant.edg[0].present && es.plant.edg[1].present, "the electrical board finds both diesel generators");
		helper.assertTrue(es.plant.lc && es.plant.mcc[0] && es.plant.mcc[1], "and the load centre and both motor control centres");
		var ts = io.github.jimbozoomer.jugcraft.nuclear.ControlBoard.refresh(level, turbine);
		helper.succeedWhen(() -> {
			io.github.jimbozoomer.jugcraft.nuclear.EquipmentBlockEntity vessel = null;
			for (var e : io.github.jimbozoomer.jugcraft.nuclear.EquipmentRegistry.of(level).anchors().entrySet()) {
				if (e.getValue().kind().reactor() && e.getKey().distSqr(stake) < 400 * 400
						&& level.getBlockEntity(e.getKey()) instanceof io.github.jimbozoomer.jugcraft.nuclear.EquipmentBlockEntity v) {
					vessel = v;
				}
			}
			helper.assertTrue(vessel != null && vessel.unit() != null && vessel.core.loops == 4, "the reactor's unit formed with four loops");
			var core = vessel.core;
			net.minecraft.server.level.ServerPlayer player = helper.makeMockServerPlayerInLevel();
			long now = level.getGameTime();
			var rcsLayout = io.github.jimbozoomer.jugcraft.nuclear.ControlBoard.layout(rcs.kind());
			var turbLayout = io.github.jimbozoomer.jugcraft.nuclear.ControlBoard.layout(turbine.kind());
			var elecLayout = io.github.jimbozoomer.jugcraft.nuclear.ControlBoard.layout(electrical.kind());
			helper.assertTrue(rcsLayout.windows().size() == 90 && turbLayout.windows().size() == 108 && elecLayout.windows().size() == 102,
					"every board's windows read from its layout");
			// A coolant pump stopped and started.
			helper.assertTrue(io.github.jimbozoomer.jugcraft.nuclear.ControlBoard.act(level, rcs, rs, rcsLayout, player, "rcp3", now), "RCP 1C's switch");
			helper.assertTrue(core.rcpStopped == 4, "RCP 1C stopped: " + core.rcpStopped);
			io.github.jimbozoomer.jugcraft.nuclear.ControlBoard.act(level, rcs, rs, rcsLayout, player, "rcp3", now);
			helper.assertTrue(core.rcpStopped == 0, "and started again");
			// The turbine trip: the first use lifts the guard, the second trips; LATCH relatches it.
			io.github.jimbozoomer.jugcraft.nuclear.ControlBoard.act(level, turbine, ts, turbLayout, player, "turbine_trip", now);
			helper.assertTrue(!core.turbineTripped && ts.guardUntil > now, "the guard lifted, not yet tripped");
			io.github.jimbozoomer.jugcraft.nuclear.ControlBoard.act(level, turbine, ts, turbLayout, player, "turbine_trip", now);
			helper.assertTrue(core.turbineTripped && core.genOpen, "tripped, and its generator breaker opened");
			helper.assertTrue(io.github.jimbozoomer.jugcraft.nuclear.ControlBoard.act(level, turbine, ts, turbLayout, player, "latch", now) && !core.turbineTripped,
					"latched again");
			// The generator breaker won't close with no steam (the reactor isn't fuelled).
			helper.assertTrue(!io.github.jimbozoomer.jugcraft.nuclear.ControlBoard.act(level, electrical, es, elecLayout, player, "gen_bkr", now) && !core.syncArmed,
					"no closing without steam");
			core.genOpen = false;
			// An alarm in: the reactor protection system bypassed.
			core.protection = false;
			var s = io.github.jimbozoomer.jugcraft.nuclear.ControlBoard.refresh(level, rcs);
			int bypass = -1;
			for (int i = 0; i < rcsLayout.windows().size(); i++) {
				if (rcsLayout.windows().get(i).id().equals("rps_bypass")) {
					bypass = i;
				}
			}
			helper.assertTrue(bypass >= 0 && s.windows[bypass] == io.github.jimbozoomer.jugcraft.nuclear.ControlBoard.ALERT && s.unacked(), "the window flashes, the horn sounds");
			// Round 32: the display wall finds the reactor and shows the alarm on its banner, new, newest first.
			var wall = io.github.jimbozoomer.jugcraft.nuclear.ControlBoard.refresh(level, boards.get(io.github.jimbozoomer.jugcraft.nuclear.EquipmentKind.PLANT_DISPLAY_WALL));
			helper.assertTrue(wall.plant.present, "the display wall finds the reactor");
			boolean shown = false;
			for (int packed : wall.wallAlarms) {
				shown |= io.github.jimbozoomer.jugcraft.nuclear.DisplayWall.kind(packed) == io.github.jimbozoomer.jugcraft.nuclear.EquipmentKind.RCS_BOARD
						&& io.github.jimbozoomer.jugcraft.nuclear.DisplayWall.window(packed) == bypass
						&& io.github.jimbozoomer.jugcraft.nuclear.DisplayWall.state(packed) == io.github.jimbozoomer.jugcraft.nuclear.ControlBoard.ALERT;
			}
			helper.assertTrue(shown && wall.wallTotal >= 1 && wall.wallNew >= 1, "the bypass alarm on the wall's banner: " + wall.wallTotal + " in, " + wall.wallNew + " new");
			helper.assertTrue(io.github.jimbozoomer.jugcraft.nuclear.DisplayWall.signal(wall.plant, "dw.condensate", now)
					&& io.github.jimbozoomer.jugcraft.nuclear.DisplayWall.csf(wall.plant, 0, now) == 0, "its mimic and safety functions read the plant");
			io.github.jimbozoomer.jugcraft.nuclear.ControlBoard.act(level, rcs, s, rcsLayout, player, "ack", now);
			helper.assertTrue(s.windows[bypass] == io.github.jimbozoomer.jugcraft.nuclear.ControlBoard.ACKED && !s.unacked(), "acknowledged");
			core.protection = true;
			io.github.jimbozoomer.jugcraft.nuclear.ControlBoard.refresh(level, rcs);
			helper.assertTrue(s.windows[bypass] == io.github.jimbozoomer.jugcraft.nuclear.ControlBoard.RINGBACK, "cleared: waiting for RESET");
			io.github.jimbozoomer.jugcraft.nuclear.ControlBoard.act(level, rcs, s, rcsLayout, player, "reset", now);
			helper.assertTrue(s.windows[bypass] == io.github.jimbozoomer.jugcraft.nuclear.ControlBoard.CLEAR, "reset");
			// The desk's controls are found where the player looks: RCP 1A's switch from in front of the board.
			var c = rcsLayout.controls().get(0);
			helper.assertTrue(c.id().equals("rcp1"), "RCP 1A's switch first: " + c.id());
			// Round 28: the safety systems, steam and feedwater, and radiation boards.
			var esf = boards.get(io.github.jimbozoomer.jugcraft.nuclear.EquipmentKind.ESF_BOARD);
			var sec = boards.get(io.github.jimbozoomer.jugcraft.nuclear.EquipmentKind.SECONDARY_BOARD);
			var rad = boards.get(io.github.jimbozoomer.jugcraft.nuclear.EquipmentKind.RADIATION_BOARD);
			var esfLayout = io.github.jimbozoomer.jugcraft.nuclear.ControlBoard.layout(esf.kind());
			var secLayout = io.github.jimbozoomer.jugcraft.nuclear.ControlBoard.layout(sec.kind());
			var radLayout = io.github.jimbozoomer.jugcraft.nuclear.ControlBoard.layout(rad.kind());
			helper.assertTrue(esfLayout.windows().size() == 138 && secLayout.windows().size() == 90 && radLayout.windows().size() == 94,
					"the round 28 boards' windows and status lights");
			// The radiation board finds every monitor's spot and the HVAC panel; nothing radioactive yet: all quiet.
			var rds = io.github.jimbozoomer.jugcraft.nuclear.ControlBoard.refresh(level, rad);
			helper.assertTrue(rds.plant.radFound == 0xFFF, "every radiation monitor has its spot: " + Integer.toBinaryString(rds.plant.radFound));
			helper.assertTrue(rds.plant.hvFound, "the radiation board finds the control room's HVAC panel");
			for (int n = 1; n <= io.github.jimbozoomer.jugcraft.nuclear.BoardPlant.RAD_MONITORS; n++) {
				helper.assertTrue(!rds.plant.signal("rm" + n + ".alert", now), "RM-" + n + " quiet with an empty core: " + rds.plant.radReading(n));
			}
			// MSIV 1 shut and opened; aux feed pump A started and stopped.
			var ss = io.github.jimbozoomer.jugcraft.nuclear.ControlBoard.refresh(level, sec);
			io.github.jimbozoomer.jugcraft.nuclear.ControlBoard.act(level, sec, ss, secLayout, player, "msiv1", now);
			helper.assertTrue(core.msivClosed == 1, "MSIV 1 shut");
			io.github.jimbozoomer.jugcraft.nuclear.ControlBoard.act(level, sec, ss, secLayout, player, "msiv1", now);
			helper.assertTrue(core.msivClosed == 0, "and open again");
			io.github.jimbozoomer.jugcraft.nuclear.ControlBoard.act(level, sec, ss, secLayout, player, "afw_a", now);
			helper.assertTrue(core.afwRunning == 1, "aux feed pump A started");
			io.github.jimbozoomer.jugcraft.nuclear.ControlBoard.act(level, sec, ss, secLayout, player, "afw_a", now);
			helper.assertTrue(core.afwRunning == 0, "and stopped");
			// Manual safety injection: the guard, then full actuation; RESET a minute on.
			var fs = io.github.jimbozoomer.jugcraft.nuclear.ControlBoard.refresh(level, esf);
			helper.assertTrue(fs.plant.hvFound, "the ESF board finds the HVAC panel too");
			io.github.jimbozoomer.jugcraft.nuclear.ControlBoard.act(level, esf, fs, esfLayout, player, "si_a", now);
			helper.assertTrue(!core.siActuated && fs.guardUntil > now, "SI A's guard lifted, not yet actuated");
			io.github.jimbozoomer.jugcraft.nuclear.ControlBoard.act(level, esf, fs, esfLayout, player, "si_a", now);
			helper.assertTrue(core.siActuated && core.scrammed && core.tripCause == io.github.jimbozoomer.jugcraft.nuclear.ReactorCore.TRIP_SI
					&& core.feedIsolated && core.cntIsolated && core.afwRunning == 7, "full actuation");
			helper.assertTrue(!io.github.jimbozoomer.jugcraft.nuclear.ControlBoard.act(level, esf, fs, esfLayout, player, "si_reset_b", now + 600), "no reset before a minute");
			helper.assertTrue(io.github.jimbozoomer.jugcraft.nuclear.ControlBoard.act(level, esf, fs, esfLayout, player, "si_reset_b",
					now + io.github.jimbozoomer.jugcraft.nuclear.ReactorCore.SI_RESET) && !core.siActuated && !core.feedIsolated, "reset a minute on");
			core.afwRunning = 0;
			core.reset();
			// Round 29: the remote shutdown panel: its aux feed switch refused until a transfer key turns; then it works and
			// the steam and feedwater board's is locked out; FCV and ARV switches work; the key hands control back.
			var rsp = boards.get(io.github.jimbozoomer.jugcraft.nuclear.EquipmentKind.REMOTE_SHUTDOWN_PANEL);
			var rspLayout = io.github.jimbozoomer.jugcraft.nuclear.ControlBoard.layout(rsp.kind());
			var ps = io.github.jimbozoomer.jugcraft.nuclear.ControlBoard.refresh(level, rsp);
			helper.assertTrue(ps.plant.present && rspLayout.windows().size() == 15, "the panel finds the reactor; its annunciator");
			helper.assertTrue(!io.github.jimbozoomer.jugcraft.nuclear.ControlBoard.act(level, rsp, ps, rspLayout, player, "afw_a", now) && core.afwRunning == 0,
					"aux feed refused at the panel before the transfer");
			io.github.jimbozoomer.jugcraft.nuclear.ControlBoard.act(level, rsp, ps, rspLayout, player, "transfer", now);
			helper.assertTrue(core.remote, "a transfer key takes control");
			helper.assertTrue(io.github.jimbozoomer.jugcraft.nuclear.ControlBoard.act(level, rsp, ps, rspLayout, player, "afw_a", now) && core.afwRunning == 1,
					"aux feed pump A started from the panel");
			helper.assertTrue(!io.github.jimbozoomer.jugcraft.nuclear.ControlBoard.act(level, sec, ss, secLayout, player, "afw_a", now) && core.afwRunning == 1,
					"the steam and feedwater board is locked out");
			io.github.jimbozoomer.jugcraft.nuclear.ControlBoard.act(level, rsp, ps, rspLayout, player, "fcv1", now);
			io.github.jimbozoomer.jugcraft.nuclear.ControlBoard.act(level, rsp, ps, rspLayout, player, "arv2", now);
			helper.assertTrue(core.afwValveShut == 1 && core.arvManual == 2, "FCV 1 shut, ARV 2 open by hand");
			io.github.jimbozoomer.jugcraft.nuclear.ControlBoard.act(level, rsp, ps, rspLayout, player, "fcv1", now);
			io.github.jimbozoomer.jugcraft.nuclear.ControlBoard.act(level, rsp, ps, rspLayout, player, "arv2", now);
			io.github.jimbozoomer.jugcraft.nuclear.ControlBoard.act(level, rsp, ps, rspLayout, player, "afw_a", now);
			io.github.jimbozoomer.jugcraft.nuclear.ControlBoard.act(level, rsp, ps, rspLayout, player, "transfer", now);
			helper.assertTrue(!core.remote && core.afwValveShut == 0 && core.arvManual == 0 && core.afwRunning == 0, "all put back, control returned");
			// Round 30: the reactor control console reads the reactor it is linked to; its trip switch trips (the guard,
			// then TRIP) and, with the reactor tripped, resets (CLOSE); the lever is refused while tripped and in AUTO and
			// works in MAN; the source range can't be blocked below P-6; the protection switch bypasses and restores; with
			// the remote shutdown panel in control only the trips work.
			var con = boards.get(io.github.jimbozoomer.jugcraft.nuclear.EquipmentKind.CONTROL_CONSOLE);
			var conLayout = io.github.jimbozoomer.jugcraft.nuclear.ControlBoard.layout(con.kind());
			var cs = io.github.jimbozoomer.jugcraft.nuclear.ControlBoard.refresh(level, con);
			helper.assertTrue(cs.plant.present && conLayout.windows().size() == 45 && conLayout.controls().size() == 12,
					"the console reads its reactor; its annunciator and controls");
			io.github.jimbozoomer.jugcraft.nuclear.ControlBoard.act(level, con, cs, conLayout, player, "rx_trip_a", now);
			helper.assertTrue(!core.scrammed && cs.guardUntil > now, "the trip switch's guard lifted");
			io.github.jimbozoomer.jugcraft.nuclear.ControlBoard.act(level, con, cs, conLayout, player, "rx_trip_a", now);
			helper.assertTrue(core.scrammed && core.tripCause == io.github.jimbozoomer.jugcraft.nuclear.ReactorCore.TRIP_MANUAL, "tripped from the console");
			helper.assertTrue(!io.github.jimbozoomer.jugcraft.nuclear.ControlBoard.act(level, con, cs, conLayout, player, "rods_out", now),
					"the lever does nothing while tripped");
			io.github.jimbozoomer.jugcraft.nuclear.ControlBoard.act(level, con, cs, conLayout, player, "rx_trip_b", now);
			io.github.jimbozoomer.jugcraft.nuclear.ControlBoard.act(level, con, cs, conLayout, player, "rx_trip_b", now);
			helper.assertTrue(!core.scrammed, "turned to CLOSE: the trip reset");
			helper.assertTrue(io.github.jimbozoomer.jugcraft.nuclear.ControlBoard.act(level, con, cs, conLayout, player, "rods_out", now)
					&& core.rodTarget < core.rods && cs.lever == 1 && cs.leverUntil > now, "the lever held OUT drives the rods out");
			core.rodTarget = 100;
			io.github.jimbozoomer.jugcraft.nuclear.ControlBoard.act(level, con, cs, conLayout, player, "rod_mode", now);
			helper.assertTrue(core.rodAuto && !io.github.jimbozoomer.jugcraft.nuclear.ControlBoard.act(level, con, cs, conLayout, player, "rods_in", now),
					"in AUTO the lever does nothing");
			io.github.jimbozoomer.jugcraft.nuclear.ControlBoard.act(level, con, cs, conLayout, player, "rod_mode", now);
			helper.assertTrue(!core.rodAuto, "back to MAN");
			helper.assertTrue(!io.github.jimbozoomer.jugcraft.nuclear.ControlBoard.act(level, con, cs, conLayout, player, "sr_block", now) && !core.srBlocked,
					"no source range block below P-6");
			io.github.jimbozoomer.jugcraft.nuclear.ControlBoard.act(level, con, cs, conLayout, player, "rps", now);
			helper.assertTrue(!core.protection, "reactor protection bypassed");
			io.github.jimbozoomer.jugcraft.nuclear.ControlBoard.act(level, con, cs, conLayout, player, "rps", now);
			helper.assertTrue(core.protection, "and back to NORMAL");
			core.remote = true;
			helper.assertTrue(!io.github.jimbozoomer.jugcraft.nuclear.ControlBoard.act(level, con, cs, conLayout, player, "rod_mode", now) && !core.rodAuto,
					"locked out while the remote shutdown panel has control");
			io.github.jimbozoomer.jugcraft.nuclear.ControlBoard.act(level, con, cs, conLayout, player, "rx_trip_a", now + 200);
			io.github.jimbozoomer.jugcraft.nuclear.ControlBoard.act(level, con, cs, conLayout, player, "rx_trip_a", now + 200);
			helper.assertTrue(core.scrammed, "but it can still trip the reactor");
			core.remote = false;
			core.reset();
		});
	}
	@GameTest
	public void mediumPlantRestroomWorks(GameTestHelper helper) {
		if (TestFocus.skip("nuclear")) {
			helper.succeed();
			return;
		}
		var level = helper.getLevel();
		BlockPos stake = helper.absolutePos(BlockPos.ZERO).offset(-6_000, 0, 12_000);
		Blueprint print = Blueprint.get("nuclear_medium_pwr", false);
		PlantBuilder.build(level, print, stake, Rotation.NONE, null, true);
		java.util.Map<String, java.util.List<BlockPos>> found = new java.util.HashMap<>();
		java.util.Set<Integer> toilets = new java.util.TreeSet<>();
		for (int i = 0; i < print.size(); i++) {
			BlockState state = print.state(i, Rotation.NONE);
			String id = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath();
			if (id.startsWith("restroom_") || id.equals("sign_wash_hands")) {
				BlockPos pos = stake.offset(print.offset(i, Rotation.NONE));
				found.computeIfAbsent(id, k -> new java.util.ArrayList<>()).add(pos);
				if (state.getBlock() instanceof io.github.jimbozoomer.jugcraft.nuclear.RestroomBlocks.Toilet) {
					toilets.add(state.getValue(io.github.jimbozoomer.jugcraft.nuclear.RestroomBlocks.VARIANT));
				}
			}
		}
		java.util.Map<String, Integer> want = java.util.Map.ofEntries(java.util.Map.entry("restroom_toilet", 2), java.util.Map.entry("restroom_urinal", 1),
				java.util.Map.entry("restroom_sink", 2), java.util.Map.entry("restroom_mirror", 2), java.util.Map.entry("restroom_hand_dryer", 1),
				java.util.Map.entry("restroom_trash_can", 1), java.util.Map.entry("restroom_towel_dispenser", 1),
				java.util.Map.entry("restroom_stall_door", 2), java.util.Map.entry("restroom_stall_door_accessible", 2),
				java.util.Map.entry("restroom_stall_partition", 4), java.util.Map.entry("restroom_stall_panel", 1),
				java.util.Map.entry("restroom_stall_headrail", 3), java.util.Map.entry("restroom_urinal_screen", 1),
				java.util.Map.entry("restroom_changing_station", 1), java.util.Map.entry("restroom_floor_drain", 1),
				java.util.Map.entry("restroom_exhaust_grille", 1), java.util.Map.entry("sign_wash_hands", 1));
		for (var e : want.entrySet()) {
			int n = found.getOrDefault(e.getKey(), java.util.List.of()).size();
			helper.assertTrue(n == e.getValue(), e.getKey() + ": " + n + ", not " + e.getValue());
		}
		helper.assertTrue(toilets.equals(java.util.Set.of(0, 1)), "a standard toilet and an accessible one: " + toilets);
		helper.assertTrue(found.get("restroom_wall_tile").size() >= 20 && found.get("restroom_floor_tile").size() >= 20, "the tiled walls and floor");
		// A roll of paper off the dispenser, and a flush.
		net.minecraft.server.level.ServerPlayer player = helper.makeMockServerPlayerInLevel();
		BlockPos toilet = found.get("restroom_toilet").get(0);
		BlockState t = level.getBlockState(toilet);
		int paper = t.getValue(io.github.jimbozoomer.jugcraft.nuclear.RestroomBlocks.PAPER);
		helper.assertTrue(io.github.jimbozoomer.jugcraft.nuclear.RestroomBlocks.Toilet.takePaper(level, toilet, t, player), "paper to take");
		helper.assertTrue(level.getBlockState(toilet).getValue(io.github.jimbozoomer.jugcraft.nuclear.RestroomBlocks.PAPER) == paper - 1, "one roll fewer");
		helper.assertTrue(player.getInventory().countItem(net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(
				io.github.jimbozoomer.jugcraft.Jugcraft.id("toilet_paper_roll"))) == 1, "the player has the roll");
		io.github.jimbozoomer.jugcraft.nuclear.RestroomBlocks.Toilet.flush(level, toilet, level.getBlockState(toilet));
		helper.assertTrue(level.getBlockState(toilet).getValue(io.github.jimbozoomer.jugcraft.nuclear.RestroomBlocks.FLUSHING), "flushing");
		io.github.jimbozoomer.jugcraft.nuclear.RestroomBlocks.runFor(level, toilet, 0);
		level.getBlockState(toilet).tick(level, toilet, level.getRandom());
		helper.assertTrue(!level.getBlockState(toilet).getValue(io.github.jimbozoomer.jugcraft.nuclear.RestroomBlocks.FLUSHING), "the flush is over");
		// A sink runs.
		BlockPos sink = found.get("restroom_sink").get(0);
		((io.github.jimbozoomer.jugcraft.nuclear.RestroomBlocks.Running) level.getBlockState(sink).getBlock()).start(level, sink, level.getBlockState(sink));
		helper.assertTrue(level.getBlockState(sink).getValue(io.github.jimbozoomer.jugcraft.nuclear.RestroomBlocks.RUNNING), "the sink runs");
		// The trash can.
		BlockPos can = found.get("restroom_trash_can").get(0);
		int fill = level.getBlockState(can).getValue(io.github.jimbozoomer.jugcraft.nuclear.RestroomBlocks.FILL);
		net.minecraft.world.SimpleContainer bin = io.github.jimbozoomer.jugcraft.nuclear.RestroomBlocks.TrashCan.bin(level, can);
		bin.setItem(0, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIRT, 5));
		helper.assertTrue(io.github.jimbozoomer.jugcraft.nuclear.RestroomBlocks.TrashCan.throwAway(level, can, bin) && bin.isEmpty(), "thrown away");
		helper.assertTrue(level.getBlockState(can).getValue(io.github.jimbozoomer.jugcraft.nuclear.RestroomBlocks.FILL) == Math.min(3, fill + 1), "fuller");
		// The standard stall's door.
		BlockPos door = null;
		for (BlockPos p : found.get("restroom_stall_door")) {
			if (level.getBlockState(p).getValue(net.minecraft.world.level.block.DoorBlock.HALF) == net.minecraft.world.level.block.state.properties.DoubleBlockHalf.LOWER) {
				door = p;
			}
		}
		helper.assertTrue(door != null, "the stall door's lower half");
		io.github.jimbozoomer.jugcraft.nuclear.RestroomBlocks.StallDoor.use(level, door, level.getBlockState(door), false, player);
		helper.assertTrue(level.getBlockState(door).getValue(net.minecraft.world.level.block.DoorBlock.OPEN), "opened from outside");
		io.github.jimbozoomer.jugcraft.nuclear.RestroomBlocks.StallDoor.use(level, door, level.getBlockState(door), true, player);
		BlockState shut = level.getBlockState(door);
		helper.assertTrue(!shut.getValue(net.minecraft.world.level.block.DoorBlock.OPEN) && shut.getValue(io.github.jimbozoomer.jugcraft.nuclear.RestroomBlocks.LATCHED)
				&& level.getBlockState(door.above()).getValue(io.github.jimbozoomer.jugcraft.nuclear.RestroomBlocks.LATCHED), "closed and latched from inside");
		io.github.jimbozoomer.jugcraft.nuclear.RestroomBlocks.StallDoor.use(level, door, level.getBlockState(door), false, player);
		helper.assertTrue(!level.getBlockState(door).getValue(net.minecraft.world.level.block.DoorBlock.OPEN), "held shut against outside");
		io.github.jimbozoomer.jugcraft.nuclear.RestroomBlocks.StallDoor.use(level, door, level.getBlockState(door), true, player);
		helper.assertTrue(level.getBlockState(door).getValue(net.minecraft.world.level.block.DoorBlock.OPEN)
				&& !level.getBlockState(door).getValue(io.github.jimbozoomer.jugcraft.nuclear.RestroomBlocks.LATCHED), "unlatched and opened from inside");
		io.github.jimbozoomer.jugcraft.nuclear.RestroomBlocks.StallDoor.use(level, door, level.getBlockState(door), true, player);
		level.getBlockState(door).tick(level, door, level.getRandom());
		helper.assertTrue(!level.getBlockState(door).getValue(io.github.jimbozoomer.jugcraft.nuclear.RestroomBlocks.LATCHED), "nobody in the stall: unlatched");
		helper.succeed();
	}

	/**
	 * The plant command (Justin, 2026-10-13: "load in the current medium nuclear plant in game in front of them"): facing
	 * each way, the medium plant's stake puts the whole plant in front of the player, its near side four blocks ahead and
	 * centred across their view.
	 */
	@GameTest
	public void plantCommandBuildsInFrontOfThePlayer(GameTestHelper helper) {
		if (TestFocus.skip("nuclear")) {
			helper.succeed();
			return;
		}
		Blueprint print = Blueprint.get(PlantBuilder.PLANTS.get("medium"), false);
		helper.assertTrue(print != null, "the medium plant's blueprint");
		BlockPos at = new BlockPos(100, 64, -40);
		for (Direction facing : Direction.Plane.HORIZONTAL) {
			Rotation rotation = io.github.jimbozoomer.jugcraft.blueprint.BlueprintItem.rotationFor(facing);
			BlockPos stake = PlantBuilder.inFront(print, rotation, at, facing);
			int near = Integer.MAX_VALUE, left = Integer.MAX_VALUE, right = Integer.MIN_VALUE;
			for (int i = 0; i < print.size(); i++) {
				BlockPos p = stake.offset(print.offset(i, rotation));
				int ahead = (p.getX() - at.getX()) * facing.getStepX() + (p.getZ() - at.getZ()) * facing.getStepZ();
				Direction side = facing.getClockWise();
				int across = (p.getX() - at.getX()) * side.getStepX() + (p.getZ() - at.getZ()) * side.getStepZ();
				near = Math.min(near, ahead);
				left = Math.min(left, across);
				right = Math.max(right, across);
			}
			helper.assertTrue(near == PlantBuilder.AHEAD, facing + ": the plant's near side " + near + " blocks ahead, not " + PlantBuilder.AHEAD);
			helper.assertTrue(Math.abs(left + right) <= 1, facing + ": centred across the view (" + left + " to " + right + ")");
			helper.assertTrue(stake.getY() == at.getY(), facing + ": its ground at the player's feet");
		}
		// Anyone in creative mode may use it (Justin, 2026-10-13: "as long as you are in creative mode"): /nuclear plant is
		// in the tree without op (creative is checked when it runs); /nuclear build stays with operators.
		var nuclear = helper.getLevel().getServer().getCommands().getDispatcher().getRoot().getChild("nuclear");
		net.minecraft.commands.CommandSourceStack plain = helper.getLevel().getServer().createCommandSourceStack()
				.withPermission(net.minecraft.server.permissions.PermissionSet.NO_PERMISSIONS);
		helper.assertTrue(nuclear != null && nuclear.canUse(plain) && nuclear.getChild("plant").canUse(plain),
				"/nuclear plant is usable without op");
		helper.assertTrue(!nuclear.getChild("build").canUse(plain), "/nuclear build needs op");
		helper.succeed();
	}

	/**
	 * Punch list #15, the cable spreading room's pre-action sprinklers (Justin: "Working sprinklers", "Alarm and put it
	 * out"): a fire under a heat detector puts the releasing panel in alarm (the radiation board's FIRE CABLE SPREADING
	 * window), it releases after its delay, the head over the fire opens and puts it out (FIRE PUMP RUNNING while the
	 * water flows), the water is shut off a while after and RESET re-arms it; the pull station releases by hand.
	 */
	@GameTest(maxTicks = 20)
	public void spreadingRoomSprinklers(GameTestHelper helper) {
		if (TestFocus.skip("nuclear")) {
			helper.succeed();
			return;
		}
		ServerLevel level = helper.getLevel();
		BlockPos o = helper.absolutePos(BlockPos.ZERO).offset(9_000, 1, 9_000);
		for (int x = -2; x <= 10; x++) {
			for (int z = -2; z <= 10; z++) {
				level.setBlock(o.offset(x, -1, z), Blocks.STONE.defaultBlockState(), 3);
			}
		}
		BlockPos panelAt = o.offset(1, 0, 8), valveAt = o.offset(2, 0, 8), pullAt = o.offset(4, 1, 8);
		BlockPos detectorAt = o.offset(4, 5, 4), headAt = o.offset(4, 5, 5), fireAt = o.offset(4, 0, 4);
		level.setBlock(panelAt, PlantInterior.BLOCKS.get(io.github.jimbozoomer.jugcraft.nuclear.SpreadingRoomFire.RELEASE).defaultBlockState(), 3);
		level.setBlock(valveAt, PlantInterior.BLOCKS.get(io.github.jimbozoomer.jugcraft.nuclear.SpreadingRoomFire.VALVE).defaultBlockState(), 3);
		level.setBlock(pullAt, PlantInterior.BLOCKS.get("fire_pull_station").defaultBlockState(), 3);
		level.setBlock(detectorAt, PlantInterior.BLOCKS.get("heat_detector").defaultBlockState(), 3);
		level.setBlock(headAt, PlantInterior.BLOCKS.get("sprinkler_pipe_head").defaultBlockState(), 3);
		var panel = (io.github.jimbozoomer.jugcraft.nuclear.LivePanelBlockEntity) level.getBlockEntity(panelAt);
		helper.assertTrue(panel != null, "the releasing panel has its block entity");
		long t = level.getGameTime();
		io.github.jimbozoomer.jugcraft.nuclear.SpreadingRoomFire.tick(panel, level, t);
		helper.assertTrue(panel.csrPhase == io.github.jimbozoomer.jugcraft.nuclear.SpreadingRoomFire.NORMAL && panel.csrFlags == 0,
				"no fire: normal, its detector, head and valve found: " + panel.csrFlags);
		level.setBlock(fireAt, Blocks.FIRE.defaultBlockState(), 3);
		helper.assertTrue(level.getBlockState(fireAt).is(Blocks.FIRE), "a fire under the detector");
		io.github.jimbozoomer.jugcraft.nuclear.SpreadingRoomFire.tick(panel, level, t + 10);
		helper.assertTrue(panel.csrPhase == io.github.jimbozoomer.jugcraft.nuclear.SpreadingRoomFire.ALARM, "the detector sees it: alarm");
		io.github.jimbozoomer.jugcraft.nuclear.BoardPlant plant = new io.github.jimbozoomer.jugcraft.nuclear.BoardPlant();
		plant.csrFound = true;
		plant.csrPhase = panel.csrPhase;
		plant.csrFlags = panel.csrFlags;
		helper.assertTrue(plant.alarm("fire_csr", 0) && !plant.alarm("fire_pump", 0), "the board: FIRE CABLE SPREADING");
		long released = t + 10 + io.github.jimbozoomer.jugcraft.nuclear.SpreadingRoomFire.DELAY * 20L;
		io.github.jimbozoomer.jugcraft.nuclear.SpreadingRoomFire.tick(panel, level, released);
		helper.assertTrue(panel.csrPhase == io.github.jimbozoomer.jugcraft.nuclear.SpreadingRoomFire.RELEASED, "after its delay it releases");
		io.github.jimbozoomer.jugcraft.nuclear.SpreadingRoomFire.tick(panel, level, released + 10);
		helper.assertTrue(level.getBlockState(fireAt).isAir() && (panel.csrFlags & io.github.jimbozoomer.jugcraft.nuclear.SpreadingRoomFire.FLOWING) != 0,
				"the head over it opens and puts it out, the water flowing");
		plant.csrPhase = panel.csrPhase;
		plant.csrFlags = panel.csrFlags;
		helper.assertTrue(plant.alarm("fire_pump", 0), "the board: FIRE PUMP RUNNING");
		io.github.jimbozoomer.jugcraft.nuclear.SpreadingRoomFire.key(panel, level, 2, null);
		helper.assertTrue(panel.csrPhase == io.github.jimbozoomer.jugcraft.nuclear.SpreadingRoomFire.RELEASED, "RESET won't while the water flows");
		io.github.jimbozoomer.jugcraft.nuclear.SpreadingRoomFire.tick(panel, level,
				released + 20 + io.github.jimbozoomer.jugcraft.nuclear.SpreadingRoomFire.SETTLE * 20L);
		helper.assertTrue(panel.csrPhase == io.github.jimbozoomer.jugcraft.nuclear.SpreadingRoomFire.FIRE_OUT
				&& (panel.csrFlags & io.github.jimbozoomer.jugcraft.nuclear.SpreadingRoomFire.FLOWING) == 0, "the fire out a while: the water off");
		io.github.jimbozoomer.jugcraft.nuclear.SpreadingRoomFire.key(panel, level, 2, null);
		helper.assertTrue(panel.csrPhase == io.github.jimbozoomer.jugcraft.nuclear.SpreadingRoomFire.NORMAL, "RESET re-arms it");
		io.github.jimbozoomer.jugcraft.nuclear.SpreadingRoomFire.pull(level, pullAt, null);
		helper.assertTrue(panel.csrPhase == io.github.jimbozoomer.jugcraft.nuclear.SpreadingRoomFire.RELEASED
				&& (panel.csrFlags & io.github.jimbozoomer.jugcraft.nuclear.SpreadingRoomFire.MANUAL) != 0, "the pull station releases by hand");
		helper.succeed();
	}

	/**
	 * Round 34: the medium plant's three overview screens (the control room's east wall, the board room, the plant
	 * manager's office) each find the control room's boards and show the plant, each with its power raceway under it; a
	 * click on a building shows its page and SITE MAP goes back; a reactor trip puts the reactor's page up by itself.
	 */
	@GameTest(maxTicks = 400)
	public void mediumPlantOverviewScreens(GameTestHelper helper) {
		if (TestFocus.skip("nuclear")) {
			helper.succeed();
			return;
		}
		var level = helper.getLevel();
		BlockPos stake = helper.absolutePos(BlockPos.ZERO).offset(-12_000, 0, 18_000);
		Blueprint print = Blueprint.get("nuclear_medium_pwr", false);
		PlantBuilder.build(level, print, stake, Rotation.NONE, null, true);
		helper.succeedWhen(() -> {
			java.util.List<io.github.jimbozoomer.jugcraft.nuclear.OverviewScreenBlockEntity> screens = new java.util.ArrayList<>();
			for (int cx = (stake.getX() >> 4) - 2; cx <= (stake.getX() + 400 >> 4); cx++) {
				for (int cz = (stake.getZ() >> 4) - 2; cz <= (stake.getZ() + 400 >> 4); cz++) {
					var chunk = level.getChunkSource().getChunkNow(cx, cz);
					if (chunk == null) {
						continue;
					}
					for (var be : chunk.getBlockEntities().values()) {
						if (be instanceof io.github.jimbozoomer.jugcraft.nuclear.OverviewScreenBlockEntity s) {
							screens.add(s);
						}
					}
				}
			}
			helper.assertTrue(screens.size() == 3, "the three overview screens: " + screens.size());
			io.github.jimbozoomer.jugcraft.nuclear.EquipmentBlockEntity vessel = null, wall = null;
			for (var e : io.github.jimbozoomer.jugcraft.nuclear.EquipmentRegistry.of(level).anchors().entrySet()) {
				if (e.getKey().distSqr(stake) < 400 * 400 && level.getBlockEntity(e.getKey()) instanceof io.github.jimbozoomer.jugcraft.nuclear.EquipmentBlockEntity b) {
					if (b.kind().liveBoard()) {
						io.github.jimbozoomer.jugcraft.nuclear.ControlBoard.refresh(level, b);
					}
					if (b.kind().reactor()) {
						vessel = b;
					}
					if (b.kind() == io.github.jimbozoomer.jugcraft.nuclear.EquipmentKind.PLANT_DISPLAY_WALL) {
						wall = b;
					}
				}
			}
			helper.assertTrue(vessel != null && wall != null, "the reactor and the display wall");
			long now = level.getGameTime();
			for (var s : screens) {
				s.refresh(level);
				helper.assertTrue(s.linked && s.board.plant.present && s.page == -1, "each screen finds its boards and the reactor, the map up");
				helper.assertTrue(level.getBlockState(s.getBlockPos().below()).is(PlantInterior.BLOCKS.get("screen_raceway")), "its raceway under it");
			}
			var s = screens.get(0);
			helper.assertTrue(io.github.jimbozoomer.jugcraft.nuclear.OverviewScreen.fill("ov.b.reactor", s, now) == io.github.jimbozoomer.jugcraft.nuclear.OverviewScreen.STANDBY,
					"an empty core: the containment standby");
			helper.assertTrue(io.github.jimbozoomer.jugcraft.nuclear.OverviewScreen.signal("ov.offsite", s, now)
					&& !io.github.jimbozoomer.jugcraft.nuclear.OverviewScreen.text("ov.mode", s, now, 6000, false, false).text().isEmpty(), "the grid and the plant's mode");
			var hits = io.github.jimbozoomer.jugcraft.nuclear.OverviewScreen.layout().hits();
			var diesel = hits.get("map").stream().filter(h -> h.page().equals("diesels")).findFirst().orElseThrow();
			s.click(level, (diesel.x0() + diesel.x1()) / 2, (diesel.y0() + diesel.y1()) / 2);
			helper.assertTrue(s.page == io.github.jimbozoomer.jugcraft.nuclear.OverviewScreen.page("diesels") && !s.pageAuto, "a click on the diesel building: its page");
			var back = hits.get("diesels").stream().filter(h -> h.page().equals("map")).findFirst().orElseThrow();
			s.click(level, (back.x0() + back.x1()) / 2, (back.y0() + back.y1()) / 2);
			helper.assertTrue(s.page == -1, "SITE MAP: back to the map");
			var other = screens.get(1);
			other.refresh(level);
			vessel.core.trip(io.github.jimbozoomer.jugcraft.nuclear.ReactorCore.TRIP_MANUAL);
			io.github.jimbozoomer.jugcraft.nuclear.ControlBoard.refresh(level, wall);
			other.refresh(level);
			helper.assertTrue(io.github.jimbozoomer.jugcraft.nuclear.OverviewScreen.fill("ov.b.reactor", other, now) == io.github.jimbozoomer.jugcraft.nuclear.OverviewScreen.TRIP,
					"the trip: the containment red");
			helper.assertTrue(other.page == io.github.jimbozoomer.jugcraft.nuclear.OverviewScreen.page("reactor") && other.pageAuto,
					"and the reactor's page up by itself: " + other.page);
		});
	}
}
