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
	private static final String[][] PLANTS = {{"nuclear_small_modular", "118096"}, {"nuclear_medium_pwr", "341680"}, {"nuclear_large_epr", "383209"}};

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
		helper.assertTrue(wrong == 0, plant + ": " + build);
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
			// The diesel starter chargers belong to the EDG circuit, not these four station-battery feeds.
			} else if (state.is(PlantInterior.BLOCKS.get("station_charger"))
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
			for (var fan : fans) fan.serverTick(level);
			for (var monitor : monitors) monitor.serverTick(level);
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
		for (var monitor : monitors) {
			helper.assertTrue(monitor.panel != null && fans.stream().anyMatch(f -> f.getBlockPos().equals(monitor.panel)) && monitor.racks.size() == 4,
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
			for (var bank : banks) bank.serverTick(level);
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
		helper.succeed();
	}
}
