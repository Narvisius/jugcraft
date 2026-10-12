package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.deposit.Deposits;
import io.github.jimbozoomer.jugcraft.deposit.JugcraftDeposits;
import io.github.jimbozoomer.jugcraft.electronics.JugcraftElectronics;
import io.github.jimbozoomer.jugcraft.electronics.NetworkTerminalBlock;
import io.github.jimbozoomer.jugcraft.energy.EnergyNetworks;
import io.github.jimbozoomer.jugcraft.energy.EnergyStorage;
import io.github.jimbozoomer.jugcraft.energy.SimpleEnergyStorage;
import io.github.jimbozoomer.jugcraft.farming.JugcraftFarming;
import io.github.jimbozoomer.jugcraft.fluid.ElectricPumpBlockEntity;
import io.github.jimbozoomer.jugcraft.fluid.FluidFilterBlockEntity;
import io.github.jimbozoomer.jugcraft.fluid.FluidTankBlockEntity;
import io.github.jimbozoomer.jugcraft.fluid.FluidValveBlock;
import io.github.jimbozoomer.jugcraft.fluid.JugcraftFluids;
import io.github.jimbozoomer.jugcraft.fluid.StoredFluid;
import io.github.jimbozoomer.jugcraft.kinetic.BeltPulleyBlockEntity;
import io.github.jimbozoomer.jugcraft.kinetic.DynamoBlockEntity;
import io.github.jimbozoomer.jugcraft.kinetic.ElectricMotorBlock;
import io.github.jimbozoomer.jugcraft.kinetic.ElectricMotorBlockEntity;
import io.github.jimbozoomer.jugcraft.kinetic.HandCrankBlock;
import io.github.jimbozoomer.jugcraft.kinetic.HandCrankBlockEntity;
import io.github.jimbozoomer.jugcraft.kinetic.JugcraftKinetics;
import io.github.jimbozoomer.jugcraft.kinetic.ShaftBlock;
import io.github.jimbozoomer.jugcraft.kinetic.SteamEngineBlock;
import io.github.jimbozoomer.jugcraft.logistics.ConveyorBlock;
import io.github.jimbozoomer.jugcraft.logistics.ConveyorBlockEntity;
import io.github.jimbozoomer.jugcraft.logistics.ConveyorSlopeBlock;
import io.github.jimbozoomer.jugcraft.logistics.ItemSorterBlockEntity;
import io.github.jimbozoomer.jugcraft.logistics.JugcraftLogistics;
import io.github.jimbozoomer.jugcraft.logistics.PneumaticExtractorBlock;
import io.github.jimbozoomer.jugcraft.machine.EnlargedMachineBlock;
import io.github.jimbozoomer.jugcraft.machine.Footprint;
import io.github.jimbozoomer.jugcraft.machine.GeneratorFuels;
import io.github.jimbozoomer.jugcraft.machine.JugcraftMachines;
import io.github.jimbozoomer.jugcraft.machine.LargeMachineBlock;
import io.github.jimbozoomer.jugcraft.machine.MachineBlock;
import io.github.jimbozoomer.jugcraft.machine.MachineBlockEntity;
import io.github.jimbozoomer.jugcraft.machine.MachineKind;
import io.github.jimbozoomer.jugcraft.machine.MachineRecipe;
import io.github.jimbozoomer.jugcraft.machine.MachineRecipes;
import io.github.jimbozoomer.jugcraft.machine.MachineUpgrades;
import io.github.jimbozoomer.jugcraft.machine.SideConfig;
import io.github.jimbozoomer.jugcraft.prospecting.OreSurvey;
import io.github.jimbozoomer.jugcraft.storage.JugcraftStorage;
import io.github.jimbozoomer.jugcraft.tools.Chargeable;
import io.github.jimbozoomer.jugcraft.tools.ChargingStationBlock;
import io.github.jimbozoomer.jugcraft.tools.ChargingStationBlockEntity;
import io.github.jimbozoomer.jugcraft.tools.JugcraftTools;
import io.github.jimbozoomer.jugcraft.tools.MiningDrillItem;
import io.github.jimbozoomer.jugcraft.tools.RocketPackItem;
import io.github.jimbozoomer.jugcraft.tools.ToolUpgrades;
import io.github.jimbozoomer.jugcraft.gear.Exosuit;
import io.github.jimbozoomer.jugcraft.gear.ExosuitItem;
import io.github.jimbozoomer.jugcraft.gear.JugcraftExosuit;
import io.github.jimbozoomer.jugcraft.gear.JugcraftGear;
import java.util.Optional;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmithingRecipe;
import net.minecraft.world.item.crafting.SmithingRecipeInput;
import net.minecraft.world.item.equipment.ArmorType;
import io.github.jimbozoomer.jugcraft.gear.PowerBowItem;
import io.github.jimbozoomer.jugcraft.gear.PowerKatanaItem;
import io.github.jimbozoomer.jugcraft.gear.ScubaTankItem;
import java.util.List;
import net.minecraft.world.phys.Vec3;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageUtil;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.material.Fluids;

/**
 * In-game tests run by `./gradlew build` on a headless server (Fabric game test API).
 * Each builds a small setup in an empty test area and waits for the expected result.
 */
public class JugcraftGameTests {
	private static Item item(String path) {
		return BuiltInRegistries.ITEM.getValue(Jugcraft.id(path));
	}

	private static void charge(GameTestHelper helper, BlockPos relative, Direction side) {
		EnergyStorage storage = EnergyStorage.SIDED.find(helper.getLevel(), helper.absolutePos(relative), side);
		helper.assertTrue(storage instanceof SimpleEnergyStorage, "No energy storage at " + relative + " on " + side);
		((SimpleEnergyStorage) storage).setAmount(storage.getCapacity());
	}

	private static BlockState machine(MachineKind kind) {
		return JugcraftMachines.MACHINES.get(kind).defaultBlockState();
	}

	/**
	 * Batch 36: a sensor on a battery box reports on its channel through a data cable to a logic controller, whose two
	 * rules ("red above 90% → blue on", "red below 20% → blue off") switch a relay on the blue channel, with a dead
	 * band between. The sensor gives a comparator-like signal; the relay a full one.
	 */
	@GameTest(maxTicks = 200)
	public void logicControllerSwitchesRelays(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos battery = new BlockPos(1, 1, 1);
		helper.setBlock(battery, machine(MachineKind.BATTERY_BOX));
		charge(helper, battery, null); // The battery box's sided storages are wrappers; the unsided one is the real battery.
		SimpleEnergyStorage energy = (SimpleEnergyStorage) EnergyStorage.SIDED.find(level, helper.absolutePos(battery), null);
		BlockPos sensor = new BlockPos(2, 1, 1);
		helper.setBlock(sensor, io.github.jimbozoomer.jugcraft.control.JugcraftControl.SENSOR.defaultBlockState()
				.setValue(io.github.jimbozoomer.jugcraft.control.SensorBlock.FACING, Direction.EAST)
				.setValue(io.github.jimbozoomer.jugcraft.control.Channels.CHANNEL, net.minecraft.world.item.DyeColor.RED));
		helper.setBlock(new BlockPos(3, 1, 1), io.github.jimbozoomer.jugcraft.control.JugcraftControl.DATA_CABLE.defaultBlockState());
		BlockPos controllerPos = new BlockPos(4, 1, 1);
		helper.setBlock(controllerPos, io.github.jimbozoomer.jugcraft.control.JugcraftControl.LOGIC_CONTROLLER.defaultBlockState());
		helper.setBlock(new BlockPos(5, 1, 1), io.github.jimbozoomer.jugcraft.control.JugcraftControl.DATA_CABLE.defaultBlockState());
		BlockPos relay = new BlockPos(6, 1, 1);
		helper.setBlock(relay, io.github.jimbozoomer.jugcraft.control.JugcraftControl.RELAY.defaultBlockState()
				.setValue(io.github.jimbozoomer.jugcraft.control.Channels.CHANNEL, net.minecraft.world.item.DyeColor.BLUE));
		var controller = helper.getBlockEntity(controllerPos, io.github.jimbozoomer.jugcraft.control.LogicControllerBlockEntity.class);
		var red = net.minecraft.world.item.DyeColor.RED;
		var blue = net.minecraft.world.item.DyeColor.BLUE;
		controller.setRule(0, true, red, true, 90, blue, true);
		controller.setRule(1, true, red, false, 20, blue, false);
		BlockPos absController = helper.absolutePos(controllerPos);
		BlockPos beyond = helper.absolutePos(new BlockPos(7, 1, 1));
		java.util.function.BooleanSupplier relayOn = () -> helper.getBlockState(relay).getValue(io.github.jimbozoomer.jugcraft.control.RelayBlock.POWERED);

		controller.evaluate(level, absController);
		helper.assertTrue(controller.reading(red) == 100, "Red reads " + controller.reading(red));
		helper.assertTrue(relayOn.getAsBoolean(), "A full battery did not switch the relay on");
		helper.assertTrue(level.hasNeighborSignal(beyond), "The relay gives no redstone signal");
		helper.runAfterDelay(30, () -> {
			int power = helper.getBlockState(sensor).getValue(io.github.jimbozoomer.jugcraft.control.SensorBlock.POWER);
			helper.assertTrue(power == 15, "A full battery's sensor gives " + power);
			energy.setAmount(energy.getCapacity() / 2);
			controller.evaluate(level, absController);
			helper.assertTrue(relayOn.getAsBoolean(), "The relay went off inside the dead band");
			energy.setAmount(energy.getCapacity() / 10);
			controller.evaluate(level, absController);
			helper.assertTrue(!relayOn.getAsBoolean(), "A low battery did not switch the relay off");
			helper.assertTrue(!level.hasNeighborSignal(beyond), "An off relay still gives a signal");
			helper.succeed();
		});
	}

	/**
	 * Batch 37: six monitor panels form one 3x2 screen whose top-left panel links over a data cable to the logic
	 * controller; a rule sounds an alarm on its channel; a bound remote flips another channel and its relay at once.
	 */
	@GameTest(maxTicks = 100)
	public void controlRoomWorks(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos controllerPos = new BlockPos(1, 1, 1);
		helper.setBlock(controllerPos, io.github.jimbozoomer.jugcraft.control.JugcraftControl.LOGIC_CONTROLLER.defaultBlockState());
		var controller = helper.getBlockEntity(controllerPos, io.github.jimbozoomer.jugcraft.control.LogicControllerBlockEntity.class);
		// A cable run east from the controller, with an alarm, a relay and the monitor's middle bottom panel on it.
		for (int x = 2; x <= 5; x++) {
			helper.setBlock(new BlockPos(x, 1, 1), io.github.jimbozoomer.jugcraft.control.JugcraftControl.DATA_CABLE.defaultBlockState());
		}
		BlockPos alarm = new BlockPos(2, 2, 1);
		helper.setBlock(alarm, io.github.jimbozoomer.jugcraft.control.JugcraftControl.ALARM.defaultBlockState()
				.setValue(io.github.jimbozoomer.jugcraft.control.Channels.CHANNEL, net.minecraft.world.item.DyeColor.ORANGE));
		BlockPos relay = new BlockPos(3, 1, 2);
		helper.setBlock(relay, io.github.jimbozoomer.jugcraft.control.JugcraftControl.RELAY.defaultBlockState()
				.setValue(io.github.jimbozoomer.jugcraft.control.Channels.CHANNEL, net.minecraft.world.item.DyeColor.GREEN));
		// The screen, facing north, in the wall at z = 0 in front of the cable (its panels' backs touch z = 1).
		for (int x = 4; x <= 6; x++) {
			for (int y = 1; y <= 2; y++) {
				helper.setBlock(new BlockPos(x, y, 0), io.github.jimbozoomer.jugcraft.control.JugcraftControl.CONTROL_MONITOR.defaultBlockState()
						.setValue(io.github.jimbozoomer.jugcraft.control.ControlMonitorBlock.FACING, Direction.NORTH));
			}
		}
		java.util.Set<Integer> parts = new java.util.HashSet<>();
		BlockPos anchor = null;
		for (int x = 4; x <= 6; x++) {
			for (int y = 1; y <= 2; y++) {
				int part = helper.getBlockState(new BlockPos(x, y, 0)).getValue(io.github.jimbozoomer.jugcraft.control.ControlMonitorBlock.PART);
				parts.add(part);
				if (part == 1) {
					anchor = new BlockPos(x, y, 0);
				}
			}
		}
		helper.assertTrue(parts.equals(java.util.Set.of(1, 2, 3, 4, 5, 6)), "The panels did not form one screen: " + parts);
		var monitor = helper.getBlockEntity(anchor, io.github.jimbozoomer.jugcraft.control.ControlMonitorBlockEntity.class);
		helper.assertTrue(helper.absolutePos(controllerPos).equals(monitor.controller()),
				"The monitor linked to " + monitor.controller() + ", not the controller");

		// Alarm: a rule with no sensor never fires, so switch it by hand through the remote instead, then a rule.
		ItemStack remote = new ItemStack(io.github.jimbozoomer.jugcraft.control.JugcraftControl.CONTROL_REMOTE);
		remote.set(io.github.jimbozoomer.jugcraft.control.JugcraftControl.REMOTE_LINK,
				net.minecraft.core.GlobalPos.of(level.dimension(), helper.absolutePos(controllerPos)));
		remote.set(io.github.jimbozoomer.jugcraft.control.JugcraftControl.REMOTE_CHANNEL, net.minecraft.world.item.DyeColor.GREEN.ordinal());
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		Vec3 at = helper.absoluteVec(new Vec3(2.5, 1, 3.5));
		player.setPos(at.x, at.y, at.z);
		player.setItemInHand(InteractionHand.MAIN_HAND, remote);
		remote.use(level, player, InteractionHand.MAIN_HAND);
		helper.assertTrue(helper.getBlockState(relay).getValue(io.github.jimbozoomer.jugcraft.control.RelayBlock.POWERED),
				"The remote did not switch the green relay on");
		helper.assertTrue(controller.isOn(net.minecraft.world.item.DyeColor.GREEN), "The controller does not have green on");
		remote.set(io.github.jimbozoomer.jugcraft.control.JugcraftControl.REMOTE_CHANNEL, net.minecraft.world.item.DyeColor.ORANGE.ordinal());
		remote.use(level, player, InteractionHand.MAIN_HAND);
		helper.assertTrue(helper.getBlockState(alarm).getValue(io.github.jimbozoomer.jugcraft.control.AlarmBlock.POWERED),
				"The remote did not sound the orange alarm");
		remote.use(level, player, InteractionHand.MAIN_HAND);
		helper.assertTrue(!helper.getBlockState(alarm).getValue(io.github.jimbozoomer.jugcraft.control.AlarmBlock.POWERED),
				"The remote did not silence the alarm");
		helper.succeed();
	}

	/**
	 * Batch 38: the rocket workshop assembles a rocket motor from a casing, a nozzle and two solid propellant; an
	 * illumination flare's burst makes a zombie glow but not a pig; a survey rocket's wide survey finds a tin ore.
	 */
	@GameTest(maxTicks = 400)
	public void rocketryWorks(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		MachineBlockEntity workshop = processing(helper, new BlockPos(1, 1, 1), MachineKind.ROCKET_WORKSHOP,
				new ItemStack(item("rocket_casing")));
		workshop.setItem(1, new ItemStack(item("rocket_nozzle")));
		workshop.setItem(2, new ItemStack(item("solid_propellant"), 2));

		net.minecraft.world.entity.Mob zombie = helper.spawnWithNoFreeWill(net.minecraft.world.entity.EntityTypes.ZOMBIE, new BlockPos(4, 1, 4));
		net.minecraft.world.entity.Mob pig = helper.spawnWithNoFreeWill(net.minecraft.world.entity.EntityTypes.PIG, new BlockPos(6, 1, 4));
		Vec3 burst = helper.absoluteVec(new Vec3(5, 1 + io.github.jimbozoomer.jugcraft.rocketry.JugcraftRocketry.ARRIVAL_HEIGHT, 5));
		io.github.jimbozoomer.jugcraft.rocketry.JugcraftRocketry.arrive(level, null, "test",
				io.github.jimbozoomer.jugcraft.rocketry.RocketItem.Kind.ILLUMINATION, burst);
		helper.assertTrue(zombie.hasEffect(net.minecraft.world.effect.MobEffects.GLOWING), "The flare did not light up the zombie");
		helper.assertTrue(!pig.hasEffect(net.minecraft.world.effect.MobEffects.GLOWING), "The flare lit up a pig");

		// A tin ore on a sampled column (every fourth block of a chunk) under the burst.
		BlockPos column = helper.absolutePos(new BlockPos(2, 1, 6));
		BlockPos ore = new BlockPos(column.getX() & ~3, column.getY() - 1, column.getZ() & ~3);
		level.setBlockAndUpdate(ore, BuiltInRegistries.BLOCK.getValue(Jugcraft.id("tin_ore")).defaultBlockState());
		var readings = io.github.jimbozoomer.jugcraft.prospecting.OreSurvey.survey(level, ore.above(30), level.getRandom(),
				io.github.jimbozoomer.jugcraft.rocketry.JugcraftRocketry.SURVEY_RADIUS,
				io.github.jimbozoomer.jugcraft.rocketry.JugcraftRocketry.SURVEY_STRIDE);
		helper.assertTrue(readings.stream().anyMatch(r -> r.icon().getPath().equals("tin_ore")), "The survey missed the tin: " + readings);

		helper.succeedWhen(() -> {
			ItemStack motor = workshop.getItem(MachineKind.ROCKET_WORKSHOP.outputSlot());
			helper.assertTrue(motor.is(item("rocket_motor")), "Rocket workshop output is " + motor);
		});
	}

	/**
	 * Batch 39 (rocket post): a pad launches its cargo to the pad its flight plan names, using up the rocket and keeping
	 * the plan, and the cargo lands in that pad. A pad under a roof will not launch. A delivery to an area that is not
	 * loaded waits instead of landing (nothing is force-loaded).
	 */
	@GameTest(maxTicks = 300, skyAccess = true)
	public void rocketPostDelivers(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		Block padBlock = io.github.jimbozoomer.jugcraft.rocketry.JugcraftRocketry.ROCKET_PAD;
		// The launching pad needs open sky: without sky access the test area has a barrier ceiling.
		BlockPos from = helper.absolutePos(new BlockPos(1, 1, 1));
		BlockPos to = helper.absolutePos(new BlockPos(6, 1, 6));
		BlockPos roofed = helper.absolutePos(new BlockPos(1, 1, 6));
		for (BlockPos pos : List.of(from, to, roofed)) {
			level.setBlockAndUpdate(pos, padBlock.defaultBlockState());
		}
		level.setBlockAndUpdate(roofed.above(2), Blocks.STONE.defaultBlockState());
		var sender = (io.github.jimbozoomer.jugcraft.rocketry.RocketPadBlockEntity) level.getBlockEntity(from);
		var receiver = (io.github.jimbozoomer.jugcraft.rocketry.RocketPadBlockEntity) level.getBlockEntity(to);
		var blocked = (io.github.jimbozoomer.jugcraft.rocketry.RocketPadBlockEntity) level.getBlockEntity(roofed);
		ItemStack plan = new ItemStack(item("flight_plan"));
		plan.set(io.github.jimbozoomer.jugcraft.rocketry.JugcraftRocketry.FLIGHT_TARGET,
				net.minecraft.core.GlobalPos.of(level.dimension(), to));
		for (var pad : List.of(sender, blocked)) {
			pad.setItem(0, new ItemStack(Items.DIAMOND, 5));
			pad.setItem(io.github.jimbozoomer.jugcraft.rocketry.RocketPadBlockEntity.ROCKET, new ItemStack(item("delivery_rocket")));
			pad.setItem(io.github.jimbozoomer.jugcraft.rocketry.RocketPadBlockEntity.PLAN, plan.copy());
		}
		var noSky = blocked.launch(level);
		helper.assertTrue(noSky == io.github.jimbozoomer.jugcraft.rocketry.RocketPadBlockEntity.Result.NO_SKY,
				"A roofed pad launched: " + noSky);
		helper.assertTrue(!blocked.getItem(io.github.jimbozoomer.jugcraft.rocketry.RocketPadBlockEntity.ROCKET).isEmpty(),
				"A failed launch used up the rocket");
		var launched = sender.launch(level);
		helper.assertTrue(launched == io.github.jimbozoomer.jugcraft.rocketry.RocketPadBlockEntity.Result.LAUNCHED,
				"The pad did not launch: " + launched + " (sky height " + level.getHeight(
						net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, from.getX(), from.getZ()) + ", pad " + from.getY() + ")");
		helper.assertTrue(sender.getItem(0).isEmpty(), "The cargo stayed on the pad");
		helper.assertTrue(sender.getItem(io.github.jimbozoomer.jugcraft.rocketry.RocketPadBlockEntity.ROCKET).isEmpty(),
				"The rocket was not used up");
		helper.assertTrue(sender.getItem(io.github.jimbozoomer.jugcraft.rocketry.RocketPadBlockEntity.PLAN).is(item("flight_plan")),
				"The flight plan was used up");

		// A delivery to a far area nobody has loaded waits, however long it has been due.
		BlockPos far = from.offset(3_000, 0, 3_000);
		helper.assertTrue(!level.isLoaded(far), "The far target is loaded");
		io.github.jimbozoomer.jugcraft.rocketry.RocketPost.send(level.getServer(), level.dimension(), far,
				List.of(new ItemStack(Items.DIRT)), 0);
		helper.succeedWhen(() -> {
			ItemStack landed = receiver.getItem(0);
			helper.assertTrue(landed.is(Items.DIAMOND) && landed.getCount() == 5, "The receiving pad holds " + landed);
			// Both were due by now (the far one had no flight distance); only the loaded target took its delivery.
			io.github.jimbozoomer.jugcraft.rocketry.RocketPost.deliver(level.getServer());
			helper.assertTrue(io.github.jimbozoomer.jugcraft.rocketry.RocketPost.pending(level.getServer()).stream()
					.anyMatch(d -> d.target().equals(far)), "A delivery to an unloaded area landed or was lost");
		});
	}

	/**
	 * Batch 40 (zipline): a line won't string through a wall; once the wall is gone it joins two anchors, a second line
	 * from either is refused, a rider runs along it and is let off at the far anchor, and breaking an anchor takes the
	 * line down at the other end.
	 */
	@GameTest(maxTicks = 200)
	public void ziplineCarriesARider(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		Block anchorBlock = io.github.jimbozoomer.jugcraft.rocketry.JugcraftRocketry.ZIPLINE_ANCHOR;
		BlockPos from = helper.absolutePos(new BlockPos(1, 4, 1));
		BlockPos to = helper.absolutePos(new BlockPos(7, 2, 7));
		BlockPos wall = helper.absolutePos(new BlockPos(4, 3, 4));
		level.setBlockAndUpdate(from, anchorBlock.defaultBlockState());
		level.setBlockAndUpdate(to, anchorBlock.defaultBlockState());
		level.setBlockAndUpdate(wall, Blocks.STONE.defaultBlockState());
		var blocked = io.github.jimbozoomer.jugcraft.rocketry.ZiplineAnchorBlockEntity.connect(level, from, to, null);
		helper.assertTrue(blocked == io.github.jimbozoomer.jugcraft.rocketry.ZiplineAnchorBlockEntity.Result.BLOCKED,
				"A line went through a wall: " + blocked);
		level.removeBlock(wall, false);
		var strung = io.github.jimbozoomer.jugcraft.rocketry.ZiplineAnchorBlockEntity.connect(level, from, to, null);
		helper.assertTrue(strung == io.github.jimbozoomer.jugcraft.rocketry.ZiplineAnchorBlockEntity.Result.OK, "The line was not strung: " + strung);
		var again = io.github.jimbozoomer.jugcraft.rocketry.ZiplineAnchorBlockEntity.connect(level, to, from, null);
		helper.assertTrue(again == io.github.jimbozoomer.jugcraft.rocketry.ZiplineAnchorBlockEntity.Result.IN_USE,
				"A second line was strung: " + again);

		net.minecraft.world.entity.Mob rider = helper.spawnWithNoFreeWill(net.minecraft.world.entity.EntityTypes.ZOMBIE, new BlockPos(1, 1, 2));
		helper.assertTrue(io.github.jimbozoomer.jugcraft.rocketry.ZiplineRider.ride(level, from, rider), "The zombie could not ride the line");
		helper.assertTrue(rider.getVehicle() instanceof io.github.jimbozoomer.jugcraft.rocketry.ZiplineRider, "The zombie is not on the trolley");
		helper.succeedWhen(() -> {
			helper.assertTrue(!rider.isPassenger(), "Still riding at " + rider.position());
			double dx = rider.getX() - (to.getX() + 0.5);
			double dz = rider.getZ() - (to.getZ() + 0.5);
			helper.assertTrue(dx * dx + dz * dz <= 4.0, "The rider was let off away from the far anchor, at " + rider.position());
			level.removeBlock(to, false);
			var start = (io.github.jimbozoomer.jugcraft.rocketry.ZiplineAnchorBlockEntity) level.getBlockEntity(from);
			helper.assertTrue(start.link() == null, "Breaking the far anchor left the line up");
		});
	}

	/**
	 * Batch 41 (rocket launcher): a high-explosive rocket fired straight at a husk hurts it and leaves the glass beside
	 * it standing (rockets never break blocks).
	 */
	@GameTest(maxTicks = 100)
	public void heRocketHurtsButBreaksNothing(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		net.minecraft.world.entity.Mob husk = helper.spawnWithNoFreeWill(net.minecraft.world.entity.EntityTypes.HUSK, new BlockPos(6, 1, 1));
		BlockPos glass = helper.absolutePos(new BlockPos(6, 1, 2));
		level.setBlockAndUpdate(glass, Blocks.GLASS.defaultBlockState());
		launch(helper, item("he_rocket"), new Vec3(1.5, 2.0, 1.5), new Vec3(1, 0, 0), null);
		helper.succeedWhen(() -> {
			helper.assertTrue(husk.getHealth() < husk.getMaxHealth(), "The rocket did not hurt the husk");
			helper.assertTrue(level.getBlockState(glass).is(Blocks.GLASS), "The rocket broke the glass");
		});
	}

	/** Batch 41: a homing rocket fired past a husk turns and flies into it. */
	@GameTest(maxTicks = 100)
	public void homingRocketSteersIntoItsTarget(GameTestHelper helper) {
		net.minecraft.world.entity.Mob husk = helper.spawnWithNoFreeWill(net.minecraft.world.entity.EntityTypes.HUSK, new BlockPos(6, 1, 6));
		launch(helper, item("homing_rocket"), new Vec3(1.5, 2.0, 1.5), new Vec3(1, 0, 0), husk);
		helper.succeedWhen(() -> helper.assertTrue(husk.getHealth() < husk.getMaxHealth(), "The homing rocket missed the husk"));
	}

	/** Fires a rocket of {@code kind} from {@code from} (relative to the test) along {@code direction}, locked on to {@code target}. */
	private static void launch(GameTestHelper helper, Item kind, Vec3 from, Vec3 direction,
			net.minecraft.world.entity.LivingEntity target) {
		var rocket = new io.github.jimbozoomer.jugcraft.rocketry.CombatRocket(
				io.github.jimbozoomer.jugcraft.rocketry.JugcraftRocketry.COMBAT_ROCKET, helper.getLevel());
		rocket.setItem(new ItemStack(kind));
		Vec3 at = helper.absoluteVec(from);
		rocket.setPos(at.x, at.y, at.z);
		rocket.setDeltaMovement(direction.normalize().scale(io.github.jimbozoomer.jugcraft.rocketry.RocketLauncherItem.SPEED / 3.0));
		if (target != null) {
			rocket.lockOn(target);
		}
		helper.getLevel().addFreshEntity(rocket);
	}

	/**
	 * Batch 42 (booster rails): a powered booster rail fed one solid propellant (eight boosts) starts a standing
	 * minecart off along the track at speed, uses one boost and keeps the cart boosted.
	 */
	@GameTest(maxTicks = 100)
	public void boosterRailLaunchesACart(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		// Rails need solid ground: a stone bed under the track, which runs south from the booster rail.
		for (int z = 1; z <= 6; z++) {
			level.setBlockAndUpdate(helper.absolutePos(new BlockPos(1, 1, z)), Blocks.STONE.defaultBlockState());
		}
		BlockPos boosterPos = helper.absolutePos(new BlockPos(1, 2, 1));
		level.setBlockAndUpdate(boosterPos, io.github.jimbozoomer.jugcraft.rocketry.JugcraftRocketry.BOOSTER_RAIL.defaultBlockState());
		for (int z = 2; z <= 6; z++) {
			level.setBlockAndUpdate(helper.absolutePos(new BlockPos(1, 2, z)), Blocks.RAIL.defaultBlockState());
		}
		level.setBlockAndUpdate(helper.absolutePos(new BlockPos(0, 2, 1)), Blocks.REDSTONE_BLOCK.defaultBlockState());
		var rail = (io.github.jimbozoomer.jugcraft.rocketry.BoosterRailBlockEntity) level.getBlockEntity(boosterPos);
		rail.setItem(0, new ItemStack(io.github.jimbozoomer.jugcraft.rocketry.JugcraftRocketry.SOLID_PROPELLANT));
		helper.assertTrue(level.getBlockState(boosterPos).getValue(net.minecraft.world.level.block.PoweredRailBlock.POWERED),
				"The booster rail is not powered");
		var cart = helper.spawn(net.minecraft.world.entity.EntityTypes.MINECART, new Vec3(1.5, 2.1, 1.5));
		helper.succeedWhen(() -> {
			helper.assertTrue(rail.charges() == io.github.jimbozoomer.jugcraft.rocketry.BoosterRailBlockEntity.CHARGES_PER_PROPELLANT - 1,
					"The rail holds " + rail.charges() + " boosts");
			helper.assertTrue(io.github.jimbozoomer.jugcraft.rocketry.BoosterRails.boosted(cart), "The cart is not boosted");
			helper.assertTrue(cart.getDeltaMovement().horizontalDistance() > 0.3, "The cart is going " + cart.getDeltaMovement());
		});
	}

	/** Ores drop their raw material to a plain pickaxe, more with Fortune; only Silk Touch takes the ore block itself. */
	@GameTest
	public void oresNeedSilkTouchToDropThemselves(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
		BlockState ore = BuiltInRegistries.BLOCK.getValue(Jugcraft.id("tin_ore")).defaultBlockState();
		List<ItemStack> plain = Block.getDrops(ore, level, pos, null, null, new ItemStack(Items.IRON_PICKAXE));
		helper.assertTrue(plain.size() == 1 && plain.get(0).is(item("raw_tin")), "Tin ore gave a plain pickaxe " + plain);
		ItemStack silk = new ItemStack(Items.IRON_PICKAXE);
		silk.enchant(level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SILK_TOUCH), 1);
		List<ItemStack> silkDrops = Block.getDrops(ore, level, pos, null, null, silk);
		helper.assertTrue(silkDrops.size() == 1 && silkDrops.get(0).is(item("tin_ore")), "Tin ore gave Silk Touch " + silkDrops);
		// Fortune III multiplies raw ore by 1-4 (vanilla's ore formula); 64 breaks all giving one has odds of 0.4^64.
		ItemStack fortune = new ItemStack(Items.IRON_PICKAXE);
		fortune.enchant(level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.FORTUNE), 3);
		int most = 0;
		for (int i = 0; i < 64; i++) {
			List<ItemStack> drops = Block.getDrops(ore, level, pos, null, null, fortune);
			helper.assertTrue(drops.size() == 1 && drops.get(0).is(item("raw_tin")) && drops.get(0).getCount() <= 4,
					"Tin ore gave Fortune III " + drops);
			most = Math.max(most, drops.get(0).getCount());
		}
		helper.assertTrue(most > 1, "Fortune III never gave more than one raw tin");
		helper.succeed();
	}

	/** A double asphalt slab drops two slabs, a single one drops one (26.3's slab loot form). */
	@GameTest
	public void doubleSlabsDropTwo(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
		BlockState slab = BuiltInRegistries.BLOCK.getValue(Jugcraft.id("asphalt_slab")).defaultBlockState();
		List<ItemStack> single = Block.getDrops(slab.setValue(SlabBlock.TYPE, SlabType.BOTTOM), level, pos, null, null,
				new ItemStack(Items.IRON_PICKAXE));
		List<ItemStack> twin = Block.getDrops(slab.setValue(SlabBlock.TYPE, SlabType.DOUBLE), level, pos, null, null,
				new ItemStack(Items.IRON_PICKAXE));
		helper.assertTrue(single.size() == 1 && single.get(0).getCount() == 1, "A single slab dropped " + single);
		helper.assertTrue(twin.size() == 1 && twin.get(0).getCount() == 2, "A double slab dropped " + twin);
		helper.succeed();
	}

	/** Recipes are data-driven: the crushing recipe for tin ore loads from data/jugcraft/recipe/crushing. */
	@GameTest
	public void crushingRecipesLoad(GameTestHelper helper) {
		MachineRecipe recipe = MachineRecipes.find(helper.getLevel(), MachineKind.CRUSHER, new ItemStack(item("tin_ore")))
				.orElseThrow(() -> helper.assertionException("No crushing recipe for tin ore"));
		ItemStack output = recipe.output().create();
		helper.assertTrue(output.is(item("raw_tin")) && output.getCount() == 2, "Tin ore should crush into 2 raw tin, got " + output);
		helper.succeed();
	}

	/** A powered crusher turns one ore into two raw ore. */
	@GameTest(maxTicks = 400)
	public void crusherDoublesOre(GameTestHelper helper) {
		BlockPos pos = new BlockPos(2, 1, 2);
		helper.setBlock(pos, machine(MachineKind.CRUSHER));
		charge(helper, pos, Direction.UP);
		MachineBlockEntity crusher = helper.getBlockEntity(pos, MachineBlockEntity.class);
		crusher.setItem(0, new ItemStack(item("tin_ore")));
		helper.succeedWhen(() -> {
			ItemStack output = crusher.getItem(MachineKind.CRUSHER.outputSlot());
			helper.assertTrue(output.is(item("raw_tin")) && output.getCount() == 2, "Crusher output is " + output);
		});
	}

	/**
	 * Batch 44: a placed crusher fills its two by three by two footprint and crushes ore fed in through its top back
	 * block; a compact crusher (one standing since before batch 44, which loads with the default state) is still one
	 * block and still crushes.
	 */
	@GameTest(maxTicks = 400)
	public void enlargedCrusherFormsAndOldCopiesKeepWorking(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		EnlargedMachineBlock block = (EnlargedMachineBlock) JugcraftMachines.MACHINES.get(MachineKind.CRUSHER);
		BlockPos master = new BlockPos(4, 1, 2);
		helper.setBlock(master, block.formed(block.defaultBlockState().setValue(MachineBlock.FACING, Direction.NORTH)));
		block.setPlacedBy(level, helper.absolutePos(master), helper.getBlockState(master), null, ItemStack.EMPTY);
		Footprint footprint = MachineKind.CRUSHER.footprint();
		helper.assertTrue(footprint.size() == 12, "The crusher should fill 12 blocks, not " + footprint.size());
		for (int part = 0; part < footprint.size(); part++) {
			BlockState at = level.getBlockState(footprint.partPos(helper.absolutePos(master), Direction.NORTH, part));
			helper.assertTrue(at.is(block) && !at.getValue(EnlargedMachineBlock.COMPACT) && at.getValue(LargeMachineBlock.PART) == part,
					"Part " + part + " is " + at);
		}
		BlockPos top = footprint.partPos(helper.absolutePos(master), Direction.NORTH, footprint.size() - 1);
		MachineBlockEntity crusher = helper.getBlockEntity(master, MachineBlockEntity.class);
		helper.assertTrue(block.getContainer(level.getBlockState(top), level, top) == crusher,
				"The crusher's top back block should reach its slots");
		charge(helper, master, Direction.UP);
		crusher.setItem(0, new ItemStack(item("tin_ore")));

		BlockPos old = new BlockPos(1, 1, 2);
		helper.setBlock(old, machine(MachineKind.CRUSHER));
		helper.assertTrue(helper.getBlockState(old).getValue(EnlargedMachineBlock.COMPACT), "A default crusher should be compact");
		helper.assertTrue(block.footprint(helper.getBlockState(old)).size() == 1, "A compact crusher should be one block");
		helper.assertBlockPresent(Blocks.AIR, old.above());
		charge(helper, old, Direction.UP);
		MachineBlockEntity oldCrusher = helper.getBlockEntity(old, MachineBlockEntity.class);
		oldCrusher.setItem(0, new ItemStack(item("tin_ore")));
		helper.succeedWhen(() -> {
			for (MachineBlockEntity machine : List.of(crusher, oldCrusher)) {
				ItemStack output = machine.getItem(MachineKind.CRUSHER.outputSlot());
				helper.assertTrue(output.is(item("raw_tin")) && output.getCount() == 2, "Crusher output is " + output);
			}
		});
	}

	/** Breaking any block of an enlarged machine removes all of it and drops the one item. */
	@GameTest
	public void breakingAnEnlargedMachineRemovesItAll(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		EnlargedMachineBlock block = (EnlargedMachineBlock) JugcraftMachines.MACHINES.get(MachineKind.SAWMILL);
		BlockPos master = new BlockPos(4, 1, 1);
		helper.setBlock(master, block.formed(block.defaultBlockState().setValue(MachineBlock.FACING, Direction.NORTH)));
		block.setPlacedBy(level, helper.absolutePos(master), helper.getBlockState(master), null, ItemStack.EMPTY);
		Footprint footprint = MachineKind.SAWMILL.footprint();
		BlockPos far = footprint.partPos(helper.absolutePos(master), Direction.NORTH, footprint.size() - 1);
		helper.assertTrue(level.getBlockState(far).is(block), "The sawmill's far block is missing");
		level.destroyBlock(far, true);
		for (int part = 0; part < footprint.size(); part++) {
			BlockPos at = footprint.partPos(helper.absolutePos(master), Direction.NORTH, part);
			helper.assertTrue(level.getBlockState(at).isAir(), "Part " + part + " of the sawmill is still there");
		}
		helper.succeed();
	}

	/**
	 * Batch 45: every Dieselworks block is registered and placeable; the grating doesn't hide what is behind it, the I-beam is
	 * not a full cube, and the amber cage lamp gives light.
	 */
	@GameTest
	public void dieselworksBlocksPlace(GameTestHelper helper) {
		var blocks = io.github.jimbozoomer.jugcraft.building.Dieselworks.BLOCKS;
		helper.assertTrue(blocks.size() == 25, "Expected 25 Dieselworks blocks, got " + blocks.size());
		BlockPos at = new BlockPos(1, 1, 1);
		for (var entry : blocks.entrySet()) {
			helper.setBlock(at, entry.getValue().defaultBlockState());
			helper.assertTrue(helper.getBlockState(at).is(entry.getValue()), entry.getKey() + " did not place");
		}
		ServerLevel level = helper.getLevel();
		BlockPos abs = helper.absolutePos(at);
		helper.setBlock(at, blocks.get("rust_grating").defaultBlockState());
		helper.assertFalse(helper.getBlockState(at).isViewBlocking(level, abs, new net.minecraft.world.phys.AABB(abs)),
				"Rust grating should be see-through");
		helper.setBlock(at, blocks.get("steel_i_beam").defaultBlockState());
		helper.assertFalse(Block.isShapeFullBlock(helper.getBlockState(at).getShape(level, abs)), "An I-beam is not a full cube");
		helper.setBlock(at, blocks.get("amber_cage_lamp").defaultBlockState());
		helper.assertTrue(helper.getBlockState(at).getLightEmission() == io.github.jimbozoomer.jugcraft.building.Dieselworks.LAMP_LIGHT,
				"The amber cage lamp should give light");
		helper.succeed();
	}

	/**
	 * Batch 46: in open air a fuelled zeppelin left alone hovers, one with an empty tank sinks, and a piloted one climbs
	 * and flies forward when its pilot holds forward and jump, burning fuel.
	 */
	@GameTest(structure = "jugcraft-test:drone_tower", maxTicks = 200, skyAccess = true)
	public void zeppelinHoversSinksAndFlies(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		var type = io.github.jimbozoomer.jugcraft.airship.JugcraftAirships.ZEPPELIN;
		io.github.jimbozoomer.jugcraft.airship.Zeppelin empty = helper.spawn(type, new Vec3(10.5, 10, 10.5));
		io.github.jimbozoomer.jugcraft.airship.Zeppelin parked = helper.spawn(type, new Vec3(10.5, 10, 32.5));
		io.github.jimbozoomer.jugcraft.airship.Zeppelin flown = helper.spawn(type, new Vec3(32.5, 10, 20.5));
		parked.setFuel(1000);
		flown.setFuel(1000);
		ServerPlayer pilot = helper.makeMockServerPlayerInLevel();
		pilot.setPos(flown.getX(), flown.getY(), flown.getZ());
		helper.assertTrue(pilot.startRiding(flown, true, true), "The pilot could not board");
		double emptyY = empty.getY();
		double parkedY = parked.getY();
		Vec3 start = flown.position();
		helper.onEachTick(() -> flown.steer(pilot, 1, 0, 1));
		helper.runAfterDelay(40, () -> {
			helper.assertTrue(empty.getY() < emptyY - 0.5, "An empty zeppelin should sink, but went from " + emptyY + " to " + empty.getY());
			helper.assertTrue(Math.abs(parked.getY() - parkedY) < 0.05, "A fuelled zeppelin left alone should hover");
			helper.assertTrue(flown.getY() > start.y + 2, "Holding jump should climb: " + start.y + " to " + flown.getY());
			helper.assertTrue(flown.position().subtract(start).horizontalDistance() > 1, "Holding forward should fly forward");
			helper.assertTrue(flown.fuel() < 1000, "Flying should burn fuel");
			helper.succeed();
		});
	}

	/** Batch 46: a zeppelin's fuel and cargo are saved with it. */
	@GameTest
	public void zeppelinKeepsFuelAndCargo(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		var type = io.github.jimbozoomer.jugcraft.airship.JugcraftAirships.ZEPPELIN;
		io.github.jimbozoomer.jugcraft.airship.Zeppelin zeppelin = type.create(level, net.minecraft.world.entity.EntitySpawnReason.LOAD);
		zeppelin.setFuel(3000);
		zeppelin.cargo().setItem(4, new ItemStack(Items.COAL, 17));
		net.minecraft.world.level.storage.TagValueOutput out = net.minecraft.world.level.storage.TagValueOutput.createWithContext(
				net.minecraft.util.ProblemReporter.DISCARDING, level.registryAccess());
		zeppelin.saveWithoutId(out);
		io.github.jimbozoomer.jugcraft.airship.Zeppelin copy = type.create(level, net.minecraft.world.entity.EntitySpawnReason.LOAD);
		copy.load(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING,
				level.registryAccess(), out.buildResult()));
		helper.assertTrue(copy.fuel() == 3000, "Fuel should be saved, got " + copy.fuel());
		helper.assertTrue(copy.cargo().getItem(4).is(Items.COAL) && copy.cargo().getItem(4).getCount() == 17, "Cargo should be saved");
		helper.succeed();
	}

	/**
	 * Batch 48: every Kaiserworks block is registered and placeable; the lattice and leaded glass don't hide what is
	 * behind them, the crest turns to face its placer and the gas lamp gives light.
	 */
	@GameTest
	public void kaiserworksBlocksPlace(GameTestHelper helper) {
		var blocks = io.github.jimbozoomer.jugcraft.building.Kaiserworks.BLOCKS;
		helper.assertTrue(blocks.size() == 26, "Expected 26 Kaiserworks blocks, got " + blocks.size());
		BlockPos at = new BlockPos(1, 1, 1);
		for (var entry : blocks.entrySet()) {
			helper.setBlock(at, entry.getValue().defaultBlockState());
			helper.assertTrue(helper.getBlockState(at).is(entry.getValue()), entry.getKey() + " did not place");
		}
		ServerLevel level = helper.getLevel();
		BlockPos abs = helper.absolutePos(at);
		for (String id : new String[] {"wrought_iron_lattice", "leaded_glass"}) {
			helper.setBlock(at, blocks.get(id).defaultBlockState());
			helper.assertFalse(helper.getBlockState(at).isViewBlocking(level, abs, new net.minecraft.world.phys.AABB(abs)),
					id + " should be see-through");
		}
		helper.setBlock(at, blocks.get("imperial_crest").defaultBlockState()
				.setValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING, net.minecraft.core.Direction.EAST));
		helper.assertTrue(helper.getBlockState(at).getValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING)
				== net.minecraft.core.Direction.EAST, "The crest should keep the way it faces");
		helper.setBlock(at, blocks.get("imperial_gas_lamp").defaultBlockState());
		helper.assertTrue(helper.getBlockState(at).getLightEmission() == io.github.jimbozoomer.jugcraft.building.Kaiserworks.LAMP_LIGHT,
				"The gas lamp should give light");
		helper.succeed();
	}

	/** Batch 47: lays a stone floor under the empty depot structure for the Diesel Walker tests. */
	private static void walkerFloor(GameTestHelper helper) {
		for (int x = 0; x < 16; x++) {
			for (int z = 0; z < 16; z++) {
				helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
			}
		}
	}

	private static io.github.jimbozoomer.jugcraft.walker.DieselWalker pilotedWalker(GameTestHelper helper, Vec3 at, ServerPlayer pilot) {
		io.github.jimbozoomer.jugcraft.walker.DieselWalker walker = helper.spawn(
				io.github.jimbozoomer.jugcraft.walker.JugcraftWalkers.DIESEL_WALKER, at);
		walker.setFuel(1000);
		pilot.setPos(walker.getX(), walker.getY(), walker.getZ());
		helper.assertTrue(pilot.startRiding(walker, true, true), "The pilot could not climb in");
		return walker;
	}

	/** Batch 47: a fuelled Diesel Walker walks forward for its pilot and burns fuel; an empty one stands still. */
	@GameTest(structure = "jugcraft-test:drone_depot", maxTicks = 200)
	public void dieselWalkerWalksOnFuel(GameTestHelper helper) {
		walkerFloor(helper);
		ServerPlayer pilot = helper.makeMockServerPlayerInLevel();
		ServerPlayer stranded = helper.makeMockServerPlayerInLevel();
		io.github.jimbozoomer.jugcraft.walker.DieselWalker walker = pilotedWalker(helper, new Vec3(3.5, 1, 2.5), pilot);
		io.github.jimbozoomer.jugcraft.walker.DieselWalker empty = pilotedWalker(helper, new Vec3(11.5, 1, 2.5), stranded);
		empty.setFuel(0);
		Vec3 start = walker.position();
		Vec3 emptyStart = empty.position();
		helper.onEachTick(() -> {
			walker.steer(pilot, 1, 0, 0, 0, 0);
			empty.steer(stranded, 1, 0, 0, 0, 0);
		});
		helper.runAfterDelay(40, () -> {
			double walked = walker.position().subtract(start).horizontalDistance();
			helper.assertTrue(walked > 3, "Holding forward should walk it forward, but it moved " + walked);
			helper.assertTrue(walker.fuel() < 1000, "Walking should burn fuel");
			helper.assertTrue(empty.position().subtract(emptyStart).horizontalDistance() < 0.1, "An empty walker should not move");
			helper.succeed();
		});
	}

	/** Batch 47: holding use drills the block the pilot looks at, and it drops as if they broke it. */
	@GameTest(structure = "jugcraft-test:drone_depot", maxTicks = 200)
	public void dieselWalkerDrillsWhatThePilotLooksAt(GameTestHelper helper) {
		walkerFloor(helper);
		ServerPlayer pilot = helper.makeMockServerPlayerInLevel();
		io.github.jimbozoomer.jugcraft.walker.DieselWalker walker = pilotedWalker(helper, new Vec3(7.5, 1, 3.5), pilot);
		BlockPos rock = new BlockPos(7, 2, 7);
		helper.setBlock(rock, Blocks.COBBLESTONE);
		Vec3 target = Vec3.atCenterOf(helper.absolutePos(rock));
		helper.onEachTick(() -> {
			pilot.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, target);
			walker.steer(pilot, 0, 0, 0, 1, 0);
		});
		helper.succeedWhen(() -> {
			helper.assertBlockPresent(Blocks.AIR, rock);
			helper.assertItemEntityPresent(Items.COBBLESTONE);
			helper.assertTrue(walker.fuel() < 1000, "Drilling should burn fuel");
		});
	}

	/** Batch 47: the fist hits what stands in front of the walker. */
	@GameTest(structure = "jugcraft-test:drone_depot", maxTicks = 100)
	public void dieselWalkerPunches(GameTestHelper helper) {
		walkerFloor(helper);
		ServerPlayer pilot = helper.makeMockServerPlayerInLevel();
		io.github.jimbozoomer.jugcraft.walker.DieselWalker walker = pilotedWalker(helper, new Vec3(7.5, 1, 3.5), pilot);
		net.minecraft.world.entity.Mob pig = helper.spawnWithNoFreeWill(net.minecraft.world.entity.EntityTypes.PIG, new BlockPos(7, 1, 5));
		helper.runAfterDelay(5, () -> walker.steer(pilot, 0, 0, 0, 0, 1));
		helper.succeedWhen(() -> helper.assertTrue(!pig.isAlive() || pig.getHealth() < pig.getMaxHealth(), "The fist should hit the pig"));
	}

	private static io.github.jimbozoomer.jugcraft.walker.ArmouredWalker pilotedArmouredWalker(GameTestHelper helper, Vec3 at, ServerPlayer pilot) {
		io.github.jimbozoomer.jugcraft.walker.ArmouredWalker walker = helper.spawn(
				io.github.jimbozoomer.jugcraft.walker.JugcraftWalkers.ARMOURED_WALKER, at);
		walker.setFuel(1000);
		pilot.setPos(walker.getX(), walker.getY(), walker.getZ());
		helper.assertTrue(pilot.startRiding(walker, true, true), "The pilot could not climb in");
		return walker;
	}

	/** Batch 58: the Armoured Walker walks on fuel, as the Diesel Walker does. */
	@GameTest(structure = "jugcraft-test:drone_depot", maxTicks = 200)
	public void armouredWalkerWalksOnFuel(GameTestHelper helper) {
		walkerFloor(helper);
		ServerPlayer pilot = helper.makeMockServerPlayerInLevel();
		var walker = pilotedArmouredWalker(helper, new Vec3(7.5, 1, 2.5), pilot);
		Vec3 start = walker.position();
		helper.onEachTick(() -> walker.steer(pilot, 1, 0, 0, 0, 0));
		helper.runAfterDelay(40, () -> {
			double walked = walker.position().subtract(start).horizontalDistance();
			helper.assertTrue(walked > 3, "Holding forward should walk it forward, but it moved " + walked);
			helper.assertTrue(walker.fuel() < 1000, "Walking should burn fuel");
			helper.succeed();
		});
	}

	/**
	 * Batch 58: holding use fires the hull cannon (a Heavy Shell toward where the pilot looks), at most once every
	 * {@value io.github.jimbozoomer.jugcraft.walker.ArmouredWalker#CANNON_COOLDOWN} ticks.
	 */
	@GameTest(structure = "jugcraft-test:drone_tower", maxTicks = 120, skyAccess = true)
	public void armouredWalkerFiresItsCannon(GameTestHelper helper) {
		raiderFloor(helper);
		ServerPlayer pilot = helper.makeMockServerPlayerInLevel();
		var walker = pilotedArmouredWalker(helper, new Vec3(20.5, 1, 6.5), pilot);
		pilot.getInventory().add(new ItemStack(io.github.jimbozoomer.jugcraft.artillery.JugcraftArtillery.HEAVY_SHELL_ITEM, 4));
		Vec3 aim = helper.absoluteVec(new Vec3(20.5, 4, 40.5));
		net.minecraft.world.phys.AABB area = new net.minecraft.world.phys.AABB(helper.absolutePos(BlockPos.ZERO)).expandTowards(44, 26, 44);
		java.util.Set<java.util.UUID> shells = new java.util.HashSet<>();
		helper.onEachTick(() -> {
			pilot.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, aim);
			walker.steer(pilot, 0, 0, 0, 1, 0);
			for (var shell : helper.getLevel().getEntitiesOfClass(io.github.jimbozoomer.jugcraft.artillery.ArtilleryShell.class, area)) {
				if (shells.add(shell.getUUID())) {
					helper.assertTrue(shell.getDeltaMovement().z > 1.0, "The shell should fly where the pilot looks, not " + shell.getDeltaMovement());
				}
			}
		});
		helper.runAfterDelay(60, () -> {
			helper.assertTrue(shells.size() == 2, "Sixty ticks of holding use should fire twice (once every 40), not " + shells.size());
			helper.succeed();
		});
	}

	/** Batch 58: attack rams the piston into what stands in front, throwing it. */
	@GameTest(structure = "jugcraft-test:drone_depot", maxTicks = 100)
	public void armouredWalkerRams(GameTestHelper helper) {
		walkerFloor(helper);
		ServerPlayer pilot = helper.makeMockServerPlayerInLevel();
		var walker = pilotedArmouredWalker(helper, new Vec3(7.5, 1, 3.5), pilot);
		net.minecraft.world.entity.Mob pig = helper.spawnWithNoFreeWill(net.minecraft.world.entity.EntityTypes.PIG, new BlockPos(7, 1, 6));
		helper.runAfterDelay(5, () -> walker.steer(pilot, 0, 0, 0, 0, 1));
		helper.succeedWhen(() -> helper.assertTrue(!pig.isAlive() || pig.getHealth() < pig.getMaxHealth(), "The ram should hit the pig"));
	}

	/** Batch 47: a Diesel Walker's fuel is saved with it. */
	@GameTest
	public void dieselWalkerKeepsFuel(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		var type = io.github.jimbozoomer.jugcraft.walker.JugcraftWalkers.DIESEL_WALKER;
		io.github.jimbozoomer.jugcraft.walker.DieselWalker walker = type.create(level, net.minecraft.world.entity.EntitySpawnReason.LOAD);
		walker.setFuel(2500);
		net.minecraft.world.level.storage.TagValueOutput out = net.minecraft.world.level.storage.TagValueOutput.createWithContext(
				net.minecraft.util.ProblemReporter.DISCARDING, level.registryAccess());
		walker.saveWithoutId(out);
		io.github.jimbozoomer.jugcraft.walker.DieselWalker copy = type.create(level, net.minecraft.world.entity.EntitySpawnReason.LOAD);
		copy.load(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING,
				level.registryAccess(), out.buildResult()));
		helper.assertTrue(copy.fuel() == 2500, "Fuel should be saved, got " + copy.fuel());
		helper.succeed();
	}

	private static io.github.jimbozoomer.jugcraft.landship.Landship drivenLandship(GameTestHelper helper, Vec3 at, ServerPlayer driver) {
		io.github.jimbozoomer.jugcraft.landship.Landship landship = helper.spawn(
				io.github.jimbozoomer.jugcraft.landship.JugcraftLandships.LANDSHIP, at);
		landship.setFuel(1000);
		driver.setPos(landship.getX(), landship.getY(), landship.getZ());
		helper.assertTrue(driver.startRiding(landship, true, true), "The driver could not climb aboard");
		return landship;
	}

	/** Batch 49: a fuelled Landship drives forward for its driver and burns fuel; an empty one stands still. */
	@GameTest(structure = "jugcraft-test:drone_depot", maxTicks = 200)
	public void landshipDrivesOnFuel(GameTestHelper helper) {
		walkerFloor(helper);
		ServerPlayer driver = helper.makeMockServerPlayerInLevel();
		ServerPlayer stranded = helper.makeMockServerPlayerInLevel();
		io.github.jimbozoomer.jugcraft.landship.Landship landship = drivenLandship(helper, new Vec3(4.5, 1, 2.5), driver);
		io.github.jimbozoomer.jugcraft.landship.Landship empty = drivenLandship(helper, new Vec3(11.5, 1, 2.5), stranded);
		empty.setFuel(0);
		Vec3 start = landship.position();
		Vec3 emptyStart = empty.position();
		helper.onEachTick(() -> {
			landship.steer(driver, 1, 0, 0, 0);
			empty.steer(stranded, 1, 0, 0, 0);
		});
		helper.runAfterDelay(40, () -> {
			double driven = landship.position().subtract(start).horizontalDistance();
			helper.assertTrue(driven > 3, "Holding forward should drive it forward, but it moved " + driven);
			helper.assertTrue(landship.fuel() < 1000, "Driving should burn fuel");
			helper.assertTrue(empty.position().subtract(emptyStart).horizontalDistance() < 0.1, "An empty landship should not move");
			helper.succeed();
		});
	}

	/**
	 * Batch 49: the cannon fires a shell that uses one from the driver's inventory and bursts against a wall, hurting
	 * the pig in front of it and leaving every block of the wall standing.
	 */
	@GameTest(structure = "jugcraft-test:drone_depot", maxTicks = 200)
	public void landshipCannonHurtsButNeverBreaksBlocks(GameTestHelper helper) {
		walkerFloor(helper);
		for (int x = 0; x < 16; x++) {
			for (int y = 1; y <= 5; y++) {
				helper.setBlock(new BlockPos(x, y, 14), Blocks.STONE);
			}
		}
		ServerPlayer driver = helper.makeMockServerPlayerInLevel();
		driver.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
		driver.getInventory().add(new ItemStack(io.github.jimbozoomer.jugcraft.landship.JugcraftLandships.CANNON_SHELL, 2));
		io.github.jimbozoomer.jugcraft.landship.Landship landship = drivenLandship(helper, new Vec3(7.5, 1, 3.5), driver);
		net.minecraft.world.entity.Mob pig = helper.spawnWithNoFreeWill(net.minecraft.world.entity.EntityTypes.PIG, new BlockPos(7, 1, 13));
		int[] ticks = {0};
		helper.onEachTick(() -> {
			driver.setYRot(0.0F);
			driver.setXRot(0.0F);
			if (++ticks[0] > 5) {
				landship.steer(driver, 0, 0, 1, 0);
			}
		});
		helper.succeedWhen(() -> {
			helper.assertTrue(!pig.isAlive() || pig.getHealth() < pig.getMaxHealth(), "The shell's burst should hurt the pig");
			for (int x = 0; x < 16; x++) {
				for (int y = 1; y <= 5; y++) {
					helper.assertBlockPresent(Blocks.STONE, new BlockPos(x, y, 14));
				}
			}
			int shells = driver.getInventory().countItem(io.github.jimbozoomer.jugcraft.landship.JugcraftLandships.CANNON_SHELL);
			helper.assertTrue(shells == 1, "One shot should use one shell, " + shells + " left");
		});
	}

	/** Batch 50: every trench block places; the duckboard is low, the searchlight shines and the wire lets things through. */
	@GameTest
	public void trenchBlocksPlace(GameTestHelper helper) {
		var blocks = io.github.jimbozoomer.jugcraft.building.Trenchworks.BLOCKS;
		helper.assertTrue(blocks.size() == 10, "Expected 10 trench blocks, got " + blocks.size());
		BlockPos at = new BlockPos(1, 1, 1);
		for (var entry : blocks.entrySet()) {
			helper.setBlock(at, entry.getValue().defaultBlockState());
			helper.assertTrue(helper.getBlockState(at).is(entry.getValue()), entry.getKey() + " did not place");
		}
		ServerLevel level = helper.getLevel();
		BlockPos abs = helper.absolutePos(at);
		helper.setBlock(at, blocks.get("duckboard").defaultBlockState());
		helper.assertTrue(helper.getBlockState(at).getShape(level, abs).max(net.minecraft.core.Direction.Axis.Y) < 0.2,
				"A duckboard is a low walkway");
		helper.setBlock(at, blocks.get("barbed_wire").defaultBlockState());
		helper.assertTrue(helper.getBlockState(at).getCollisionShape(level, abs).isEmpty(), "Barbed wire is walked into, not on");
		helper.setBlock(at, blocks.get("searchlight").defaultBlockState());
		helper.assertTrue(helper.getBlockState(at).getLightEmission() == io.github.jimbozoomer.jugcraft.building.Trenchworks.SEARCHLIGHT_LIGHT,
				"A searchlight should shine");
		helper.succeed();
	}

	/** Batch 50: a pig pushed through barbed wire is cut. */
	@GameTest(maxTicks = 100)
	public void barbedWireCuts(GameTestHelper helper) {
		for (int x = 1; x <= 6; x++) {
			helper.setBlock(new BlockPos(x, 1, 2), io.github.jimbozoomer.jugcraft.building.Trenchworks.BLOCKS.get("barbed_wire"));
		}
		net.minecraft.world.entity.Mob pig = helper.spawnWithNoFreeWill(net.minecraft.world.entity.EntityTypes.PIG, new BlockPos(1, 1, 2));
		helper.onEachTick(() -> pig.setDeltaMovement(0.15, pig.getDeltaMovement().y, 0));
		helper.succeedWhen(() -> helper.assertTrue(pig.getHealth() < pig.getMaxHealth(), "The wire should cut the pig"));
	}

	/**
	 * Batch 50: powering a field telephone's back makes another on its channel ring and light a lamp in front of it; one
	 * on a different channel stays quiet.
	 */
	@GameTest(maxTicks = 100)
	public void fieldTelephonesRing(GameTestHelper helper) {
		Block phone = io.github.jimbozoomer.jugcraft.building.Trenchworks.BLOCKS.get("field_telephone");
		BlockState north = phone.defaultBlockState();
		helper.setBlock(new BlockPos(1, 1, 2), north);
		helper.setBlock(new BlockPos(4, 1, 2), north);
		helper.setBlock(new BlockPos(7, 1, 2), north.setValue(io.github.jimbozoomer.jugcraft.control.Channels.CHANNEL,
				net.minecraft.world.item.DyeColor.RED));
		helper.setBlock(new BlockPos(4, 1, 1), Blocks.REDSTONE_LAMP);
		helper.setBlock(new BlockPos(7, 1, 1), Blocks.REDSTONE_LAMP);
		helper.runAfterDelay(5, () -> helper.setBlock(new BlockPos(1, 1, 3), Blocks.REDSTONE_BLOCK));
		helper.succeedWhen(() -> {
			helper.assertTrue(helper.getBlockState(new BlockPos(4, 1, 2)).getValue(io.github.jimbozoomer.jugcraft.building.FieldTelephoneBlock.RINGING),
					"The telephone on the same channel should ring");
			helper.assertTrue(helper.getBlockState(new BlockPos(4, 1, 1)).getValue(net.minecraft.world.level.block.RedstoneLampBlock.LIT),
					"A ringing telephone should light the lamp in front of it");
			helper.assertFalse(helper.getBlockState(new BlockPos(7, 1, 2)).getValue(io.github.jimbozoomer.jugcraft.building.FieldTelephoneBlock.RINGING),
					"A telephone on another channel should stay quiet");
			helper.assertFalse(helper.getBlockState(new BlockPos(7, 1, 1)).getValue(net.minecraft.world.level.block.RedstoneLampBlock.LIT),
					"Its lamp should stay dark");
		});
	}

	/** Batch 49: a Landship's fuel is saved with it. */
	@GameTest
	public void landshipKeepsFuel(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		var type = io.github.jimbozoomer.jugcraft.landship.JugcraftLandships.LANDSHIP;
		io.github.jimbozoomer.jugcraft.landship.Landship landship = type.create(level, net.minecraft.world.entity.EntitySpawnReason.LOAD);
		landship.setFuel(4500);
		net.minecraft.world.level.storage.TagValueOutput out = net.minecraft.world.level.storage.TagValueOutput.createWithContext(
				net.minecraft.util.ProblemReporter.DISCARDING, level.registryAccess());
		landship.saveWithoutId(out);
		io.github.jimbozoomer.jugcraft.landship.Landship copy = type.create(level, net.minecraft.world.entity.EntitySpawnReason.LOAD);
		copy.load(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING,
				level.registryAccess(), out.buildResult()));
		helper.assertTrue(copy.fuel() == 4500, "Fuel should be saved, got " + copy.fuel());
		helper.succeed();
	}

	/** Batch 51: the ballistics solver's elevations land shells where they were aimed, on both arcs. */
	@GameTest
	public void ballisticsLandWhereAimed(GameTestHelper helper) {
		double speed = io.github.jimbozoomer.jugcraft.artillery.JugcraftArtillery.HEAVY_SPEED;
		double gravity = io.github.jimbozoomer.jugcraft.artillery.JugcraftArtillery.HEAVY_GRAVITY;
		for (double distance : new double[] {30, 60, 100}) {
			for (double rise : new double[] {0, -4, 6}) {
				for (boolean high : new boolean[] {false, true}) {
					Float pitch = io.github.jimbozoomer.jugcraft.artillery.Ballistics.solve(speed, gravity, distance, rise, high, -5.0F, 85.0F);
					helper.assertTrue(pitch != null, "No elevation for " + distance + " blocks (rise " + rise + ", high " + high + ")");
					double landed = io.github.jimbozoomer.jugcraft.artillery.Ballistics.range(speed, gravity, pitch, rise);
					helper.assertTrue(Math.abs(landed - distance) < 1.0, "Aimed at " + distance + " but landed at " + landed);
					helper.assertTrue(high == pitch > 45.0F, "The " + (high ? "high" : "low") + " arc chose " + pitch + " degrees");
				}
			}
		}
		helper.assertTrue(io.github.jimbozoomer.jugcraft.artillery.Ballistics.solve(speed, gravity, 400, 0, true, 45.0F, 85.0F) == null,
				"400 blocks should be out of reach");
		helper.succeed();
	}

	/**
	 * Batch 51: a siege mortar, crewed by a gunner with a marked target 30 blocks away, turns, lobs a Heavy Shell (using
	 * one) and hurts the pig at the target, without breaking the floor.
	 */
	@GameTest(structure = "jugcraft-test:drone_tower", maxTicks = 600, skyAccess = true)
	public void siegeMortarShellsTheMarkedTarget(GameTestHelper helper) {
		for (int x = 0; x < 44; x++) {
			for (int z = 0; z < 44; z++) {
				helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
			}
		}
		var type = io.github.jimbozoomer.jugcraft.artillery.JugcraftArtillery.SIEGE_MORTAR;
		io.github.jimbozoomer.jugcraft.artillery.SiegeMortar mortar = helper.spawn(type, new Vec3(6.5, 1, 6.5));
		mortar.face(0.0F);
		ServerPlayer gunner = helper.makeMockServerPlayerInLevel();
		gunner.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
		gunner.getInventory().add(new ItemStack(io.github.jimbozoomer.jugcraft.artillery.JugcraftArtillery.HEAVY_SHELL_ITEM, 3));
		gunner.setPos(mortar.getX(), mortar.getY(), mortar.getZ());
		helper.assertTrue(gunner.startRiding(mortar, true, true), "The gunner could not climb aboard");
		BlockPos target = new BlockPos(28, 1, 27);
		net.minecraft.world.entity.Mob pig = helper.spawnWithNoFreeWill(net.minecraft.world.entity.EntityTypes.PIG, target);
		io.github.jimbozoomer.jugcraft.artillery.Spotting.mark(gunner, helper.absolutePos(target.below()));
		helper.onEachTick(() -> mortar.steer(gunner, 0, 0, 1));
		helper.succeedWhen(() -> {
			helper.assertTrue(!pig.isAlive() || pig.getHealth() < pig.getMaxHealth(), "The shell should hurt the pig at the target");
			helper.assertBlockPresent(Blocks.STONE, target.below());
			int left = gunner.getInventory().countItem(io.github.jimbozoomer.jugcraft.artillery.JugcraftArtillery.HEAVY_SHELL_ITEM);
			helper.assertTrue(left < 3, "Firing should use a Heavy Shell");
		});
	}

	/** Batch 51: a flak shell fired up past a hovering bat bursts beside it and hurts it. */
	@GameTest(structure = "jugcraft-test:drone_tower", maxTicks = 100, skyAccess = true)
	public void flakBurstsBesideFlyers(GameTestHelper helper) {
		net.minecraft.world.entity.Mob bat = helper.spawnWithNoFreeWill(net.minecraft.world.entity.EntityTypes.BAT, new BlockPos(20, 16, 20));
		bat.setNoGravity(true);
		var shell = new io.github.jimbozoomer.jugcraft.artillery.ArtilleryShell(
				io.github.jimbozoomer.jugcraft.artillery.JugcraftArtillery.FLAK_SHELL, helper.getLevel());
		Vec3 start = helper.absoluteVec(new Vec3(21.5, 4, 20.5));
		shell.setPos(start);
		shell.shoot(0, 1, 0, (float) io.github.jimbozoomer.jugcraft.artillery.JugcraftArtillery.FLAK_SPEED, 0.0F);
		helper.getLevel().addFreshEntity(shell);
		helper.succeedWhen(() -> helper.assertTrue(!bat.isAlive() || bat.getHealth() < bat.getMaxHealth(),
				"The flak burst should hurt the bat"));
	}

	/** Batch 51: an observation balloon winches up while ridden and its anchor is saved. */
	@GameTest(structure = "jugcraft-test:drone_tower", maxTicks = 300, skyAccess = true)
	public void observationBalloonRisesWithItsSpotter(GameTestHelper helper) {
		for (int x = 0; x < 6; x++) {
			for (int z = 0; z < 6; z++) {
				helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
			}
		}
		var type = io.github.jimbozoomer.jugcraft.artillery.JugcraftArtillery.BALLOON;
		io.github.jimbozoomer.jugcraft.artillery.ObservationBalloon balloon = helper.spawn(type, new Vec3(2.5, 1, 2.5));
		ServerPlayer spotter = helper.makeMockServerPlayerInLevel();
		spotter.setPos(balloon.getX(), balloon.getY(), balloon.getZ());
		double start = balloon.getY();
		helper.runAfterDelay(2, () -> helper.assertTrue(spotter.startRiding(balloon, true, true), "The spotter could not climb in"));
		helper.runAfterDelay(160, () -> {
			helper.assertTrue(balloon.getY() > start + 8, "A ridden balloon should rise, but went from " + start + " to " + balloon.getY());
			helper.assertTrue(Math.abs(balloon.anchorY() - start) < 0.01, "The anchor should stay where the balloon was placed");
			helper.succeed();
		});
	}

	/** Batch 51: a self-propelled howitzer's fuel and aim are saved with it. */
	@GameTest
	public void howitzerKeepsFuelAndAim(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		var type = io.github.jimbozoomer.jugcraft.artillery.JugcraftArtillery.HOWITZER;
		io.github.jimbozoomer.jugcraft.artillery.SelfPropelledHowitzer gun = type.create(level, net.minecraft.world.entity.EntitySpawnReason.LOAD);
		gun.setFuel(3500);
		gun.face(90.0F);
		net.minecraft.world.level.storage.TagValueOutput out = net.minecraft.world.level.storage.TagValueOutput.createWithContext(
				net.minecraft.util.ProblemReporter.DISCARDING, level.registryAccess());
		gun.saveWithoutId(out);
		io.github.jimbozoomer.jugcraft.artillery.SelfPropelledHowitzer copy = type.create(level, net.minecraft.world.entity.EntitySpawnReason.LOAD);
		copy.load(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING,
				level.registryAccess(), out.buildResult()));
		helper.assertTrue(copy.fuel() == 3500, "Fuel should be saved, got " + copy.fuel());
		helper.assertTrue(Math.abs(copy.aimYaw(1.0F) - 90.0F) < 0.01, "The gun's aim should be saved, got " + copy.aimYaw(1.0F));
		helper.succeed();
	}

	/**
	 * Batch 54: each tower gun's entity fits inside its footprint, and it stands only where a solid top covers the
	 * whole footprint (a 5x5 top with one block missing will not do).
	 */
	@GameTest
	public void towerGunsNeedAFullTop(GameTestHelper helper) {
		for (var spec : io.github.jimbozoomer.jugcraft.artillery.JugcraftTowerGuns.SPECS) {
			float width = io.github.jimbozoomer.jugcraft.artillery.JugcraftTowerGuns.type(spec.id()).getWidth();
			helper.assertTrue(width < spec.footprint() && width > spec.footprint() - 1,
					spec.id() + " is " + width + " wide for a " + spec.footprint() + "x" + spec.footprint() + " top");
		}
		for (int x = 0; x < 5; x++) {
			for (int z = 0; z < 5; z++) {
				helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE_BRICKS);
			}
		}
		BlockPos centre = helper.absolutePos(new BlockPos(2, 1, 2));
		ServerLevel level = helper.getLevel();
		helper.assertTrue(io.github.jimbozoomer.jugcraft.artillery.TowerGun.supported(level, centre, 5), "A full 5x5 top should hold a 5x5 gun");
		helper.assertTrue(io.github.jimbozoomer.jugcraft.artillery.TowerGun.supported(level, centre, 3), "A 5x5 top should hold a 3x3 gun");
		helper.setBlock(new BlockPos(4, 0, 4), Blocks.AIR);
		helper.assertFalse(io.github.jimbozoomer.jugcraft.artillery.TowerGun.supported(level, centre, 5), "A 5x5 top missing a corner should not");
		helper.assertTrue(io.github.jimbozoomer.jugcraft.artillery.TowerGun.supported(level, centre, 3), "The 3x3 middle is still whole");
		helper.succeed();
	}

	/**
	 * Batch 54: a Grand Mortar, crewed by a gunner with a marked target about 33 blocks away, lobs a Great Shell (using
	 * one) and hurts the pig at the target, without breaking the floor.
	 */
	@GameTest(structure = "jugcraft-test:drone_tower", maxTicks = 700, skyAccess = true)
	public void grandMortarShellsTheMarkedTarget(GameTestHelper helper) {
		for (int x = 0; x < 44; x++) {
			for (int z = 0; z < 44; z++) {
				helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
			}
		}
		var type = io.github.jimbozoomer.jugcraft.artillery.JugcraftTowerGuns.type("grand_mortar");
		io.github.jimbozoomer.jugcraft.artillery.TowerGun mortar = helper.spawn(type, new Vec3(6.5, 1, 6.5));
		mortar.face(0.0F);
		ServerPlayer gunner = helper.makeMockServerPlayerInLevel();
		gunner.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
		gunner.getInventory().add(new ItemStack(io.github.jimbozoomer.jugcraft.artillery.JugcraftTowerGuns.GREAT_SHELL_ITEM, 2));
		gunner.setPos(mortar.getX(), mortar.getY(), mortar.getZ());
		helper.assertTrue(gunner.startRiding(mortar, true, true), "The gunner could not climb aboard");
		BlockPos target = new BlockPos(30, 1, 29);
		net.minecraft.world.entity.Mob pig = helper.spawnWithNoFreeWill(net.minecraft.world.entity.EntityTypes.PIG, target);
		io.github.jimbozoomer.jugcraft.artillery.Spotting.mark(gunner, helper.absolutePos(target.below()));
		helper.onEachTick(() -> mortar.steer(gunner, 0, 0, 1));
		helper.succeedWhen(() -> {
			helper.assertTrue(!pig.isAlive() || pig.getHealth() < pig.getMaxHealth(), "The Great Shell should hurt the pig at the target");
			helper.assertBlockPresent(Blocks.STONE, target.below());
			int left = gunner.getInventory().countItem(io.github.jimbozoomer.jugcraft.artillery.JugcraftTowerGuns.GREAT_SHELL_ITEM);
			helper.assertTrue(left < 2, "Firing should use a Great Shell");
		});
	}

	/**
	 * Batch 54: one press on a Triple Battery fires a salvo of three Heavy Shells (one from each barrel, using three)
	 * and then it reloads.
	 */
	@GameTest(structure = "jugcraft-test:drone_tower", maxTicks = 200, skyAccess = true)
	public void tripleBatteryFiresASalvo(GameTestHelper helper) {
		for (int x = 0; x < 5; x++) {
			for (int z = 0; z < 5; z++) {
				helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
			}
		}
		var type = io.github.jimbozoomer.jugcraft.artillery.JugcraftTowerGuns.type("triple_battery");
		io.github.jimbozoomer.jugcraft.artillery.TowerGun battery = helper.spawn(type, new Vec3(2.5, 1, 2.5));
		battery.face(0.0F);
		ServerPlayer gunner = helper.makeMockServerPlayerInLevel();
		gunner.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
		gunner.getInventory().add(new ItemStack(io.github.jimbozoomer.jugcraft.artillery.JugcraftArtillery.HEAVY_SHELL_ITEM, 5));
		gunner.setPos(battery.getX(), battery.getY(), battery.getZ());
		helper.assertTrue(gunner.startRiding(battery, true, true), "The gunner could not climb aboard");
		// The gunner's own mark straight ahead, in range: other tests' marks (which outlive their players) could
		// otherwise draw the gun's aim away, as a gun follows the nearest spotter's mark when its gunner has none.
		io.github.jimbozoomer.jugcraft.artillery.Spotting.mark(gunner, helper.absolutePos(new BlockPos(2, 0, 30)));
		// One press, held for a moment: the gun fires once it has turned onto the gunner's line.
		for (int tick = 1; tick <= 40; tick++) {
			int pressed = tick <= 30 ? 1 : 0;
			helper.runAfterDelay(tick, () -> battery.steer(gunner, 0, 0, pressed));
		}
		helper.runAfterDelay(80, () -> {
			int left = gunner.getInventory().countItem(io.github.jimbozoomer.jugcraft.artillery.JugcraftArtillery.HEAVY_SHELL_ITEM);
			try {
				helper.assertTrue(left == 2, "A salvo should use three Heavy Shells, leaving 2, but left " + left
						+ "; gun ticks=" + battery.tickCount + ", crewed=" + (battery.getFirstPassenger() == gunner)
						+ ", aim=" + battery.aimYaw(1.0F) + "/" + battery.aimPitch());
				helper.succeed();
			} finally {
				io.github.jimbozoomer.jugcraft.artillery.Spotting.clear(gunner);
			}
		});
	}

	/**
	 * Batch 55: every fortification block places; the parapet has its crenel (a gap you can stand in), the wall joins
	 * walls, the ladder is climbable and the hoist and rack have their block entities.
	 */
	@GameTest
	public void fortificationBlocksPlace(GameTestHelper helper) {
		int x = 0;
		for (var entry : io.github.jimbozoomer.jugcraft.building.Fortifications.BLOCKS.entrySet()) {
			if (entry.getKey().equals("blast_door") || entry.getKey().equals("steel_ladder")) {
				continue;
			}
			BlockPos pos = new BlockPos(x++ % 8, 1, x / 8 * 2);
			helper.setBlock(pos, entry.getValue());
			helper.assertBlockPresent(entry.getValue(), pos);
		}
		ServerLevel level = helper.getLevel();
		BlockPos parapet = new BlockPos(0, 3, 4);
		helper.setBlock(parapet, io.github.jimbozoomer.jugcraft.building.Fortifications.BLOCKS.get("bastion_parapet"));
		var shape = helper.getBlockState(parapet).getShape(level, helper.absolutePos(parapet));
		helper.assertTrue(shape.max(Direction.Axis.Y) == 1.0 && !Block.isShapeFullBlock(shape), "A parapet is full height but not a full cube");
		helper.assertTrue(io.github.jimbozoomer.jugcraft.building.Fortifications.BLOCKS.get("bastion_concrete_wall").defaultBlockState().is(net.minecraft.tags.BlockTags.WALLS),
				"The bastion wall should be in #minecraft:walls");
		helper.assertTrue(io.github.jimbozoomer.jugcraft.building.Fortifications.BLOCKS.get("steel_ladder").defaultBlockState().is(net.minecraft.tags.BlockTags.CLIMBABLE),
				"The steel ladder should be climbable");
		BlockPos hoist = new BlockPos(2, 3, 4);
		helper.setBlock(hoist, io.github.jimbozoomer.jugcraft.building.Fortifications.BLOCKS.get("ammo_hoist"));
		helper.getBlockEntity(hoist, io.github.jimbozoomer.jugcraft.building.AmmoHoistBlock.Entity.class);
		BlockPos rack = new BlockPos(4, 3, 4);
		helper.setBlock(rack, io.github.jimbozoomer.jugcraft.building.Fortifications.BLOCKS.get("ready_rack"));
		helper.getBlockEntity(rack, io.github.jimbozoomer.jugcraft.building.ReadyRackBlock.Entity.class);
		helper.succeed();
	}

	/** Batch 55: a blast door will not open by hand (its set is iron's) but opens on a redstone signal. */
	@GameTest
	public void blastDoorOpensOnlyByRedstone(GameTestHelper helper) {
		var door = (net.minecraft.world.level.block.DoorBlock) io.github.jimbozoomer.jugcraft.building.Fortifications.BLOCKS.get("blast_door");
		helper.assertFalse(door.type().canOpenByHand(), "A blast door must not open by hand");
		BlockPos lower = new BlockPos(2, 1, 2);
		helper.setBlock(lower, door.defaultBlockState().setValue(net.minecraft.world.level.block.DoorBlock.HALF,
				net.minecraft.world.level.block.state.properties.DoubleBlockHalf.LOWER));
		helper.setBlock(lower.above(), door.defaultBlockState().setValue(net.minecraft.world.level.block.DoorBlock.HALF,
				net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER));
		helper.assertBlockProperty(lower, net.minecraft.world.level.block.DoorBlock.OPEN, false);
		helper.setBlock(lower.east(), Blocks.REDSTONE_BLOCK);
		helper.succeedWhen(() -> helper.assertBlockProperty(lower, net.minecraft.world.level.block.DoorBlock.OPEN, true));
	}

	/**
	 * Batch 55: shells loaded into the bottom of a four-high ammo hoist climb the shaft into the ready rack on top, and a
	 * hopper cannot take them back out of the shaft.
	 */
	@GameTest(maxTicks = 300)
	public void ammoHoistLiftsShellsToTheRack(GameTestHelper helper) {
		for (int y = 1; y <= 4; y++) {
			helper.setBlock(new BlockPos(2, y, 2), io.github.jimbozoomer.jugcraft.building.Fortifications.BLOCKS.get("ammo_hoist"));
		}
		BlockPos rackPos = new BlockPos(2, 5, 2);
		helper.setBlock(rackPos, io.github.jimbozoomer.jugcraft.building.Fortifications.BLOCKS.get("ready_rack"));
		helper.assertBlockProperty(new BlockPos(2, 4, 2), io.github.jimbozoomer.jugcraft.building.AmmoHoistBlock.TOP, true);
		helper.assertBlockProperty(new BlockPos(2, 1, 2), io.github.jimbozoomer.jugcraft.building.AmmoHoistBlock.TOP, false);
		ServerLevel level = helper.getLevel();
		var bottom = ItemStorage.SIDED.find(level, helper.absolutePos(new BlockPos(2, 1, 2)), Direction.DOWN);
		helper.assertTrue(bottom != null && bottom.supportsInsertion() && !bottom.supportsExtraction(),
				"A hoist should take items in but never give them up to a hopper");
		ItemVariant shell = ItemVariant.of(io.github.jimbozoomer.jugcraft.artillery.JugcraftArtillery.HEAVY_SHELL_ITEM);
		try (Transaction transaction = Transaction.openOuter()) {
			helper.assertTrue(bottom.insert(shell, 8, transaction) == 8, "The bottom hoist should take 8 shells");
			transaction.commit();
		}
		var rack = helper.getBlockEntity(rackPos, io.github.jimbozoomer.jugcraft.building.ReadyRackBlock.Entity.class);
		helper.succeedWhen(() -> {
			helper.assertTrue(rack.count() == 8, "The rack on top should hold all 8 shells, but holds " + rack.count());
			helper.assertBlockProperty(rackPos, io.github.jimbozoomer.jugcraft.building.ReadyRackBlock.FILL, 1);
		});
	}

	/**
	 * Batch 55: a gunner with no shells on them fires a Triple Battery's salvo from the ready rack beside it, and the
	 * rack loses three shells.
	 */
	@GameTest(structure = "jugcraft-test:drone_tower", maxTicks = 200, skyAccess = true)
	public void gunsDrawShellsFromReadyRacks(GameTestHelper helper) {
		for (int x = 0; x < 5; x++) {
			for (int z = 0; z < 5; z++) {
				helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
			}
		}
		BlockPos rackPos = new BlockPos(5, 1, 2);
		helper.setBlock(rackPos, io.github.jimbozoomer.jugcraft.building.Fortifications.BLOCKS.get("ready_rack"));
		var rack = helper.getBlockEntity(rackPos, io.github.jimbozoomer.jugcraft.building.ReadyRackBlock.Entity.class);
		rack.shells.addItem(new ItemStack(io.github.jimbozoomer.jugcraft.artillery.JugcraftArtillery.HEAVY_SHELL_ITEM, 5));
		var type = io.github.jimbozoomer.jugcraft.artillery.JugcraftTowerGuns.type("triple_battery");
		io.github.jimbozoomer.jugcraft.artillery.TowerGun battery = helper.spawn(type, new Vec3(2.5, 1, 2.5));
		battery.face(0.0F);
		ServerPlayer gunner = helper.makeMockServerPlayerInLevel();
		gunner.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
		gunner.setPos(battery.getX(), battery.getY(), battery.getZ());
		helper.assertTrue(gunner.startRiding(battery, true, true), "The gunner could not climb aboard");
		// The gunner's own mark straight ahead, in range: other tests' marks (which outlive their players) could
		// otherwise draw the gun's aim away, as a gun follows the nearest spotter's mark when its gunner has none.
		io.github.jimbozoomer.jugcraft.artillery.Spotting.mark(gunner, helper.absolutePos(new BlockPos(2, 0, 30)));
		for (int tick = 1; tick <= 40; tick++) {
			int pressed = tick <= 30 ? 1 : 0;
			helper.runAfterDelay(tick, () -> battery.steer(gunner, 0, 0, pressed));
		}
		helper.runAfterDelay(80, () -> {
			helper.assertTrue(rack.count() == 2, "The salvo should take three shells from the rack, leaving 2, but left " + rack.count());
			helper.succeed();
		});
	}

	/**
	 * Batch 56 helper: a Triple Battery on a stone pad, a ready rack of five Heavy Shells beside it and a fire control
	 * table facing south (down the range) with the battery linked to it through the wire, in {@code mode}. Returns the
	 * battery; the rack is at (5, 1, 2) and the table at (8, 1, 2).
	 */
	private static io.github.jimbozoomer.jugcraft.artillery.TowerGun linkedBattery(GameTestHelper helper,
			io.github.jimbozoomer.jugcraft.building.FireControlTableBlock.Mode mode) {
		for (int x = 0; x < 5; x++) {
			for (int z = 0; z < 5; z++) {
				helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
			}
		}
		BlockPos rackPos = new BlockPos(5, 1, 2);
		helper.setBlock(rackPos, io.github.jimbozoomer.jugcraft.building.Fortifications.BLOCKS.get("ready_rack"));
		helper.getBlockEntity(rackPos, io.github.jimbozoomer.jugcraft.building.ReadyRackBlock.Entity.class).shells
				.addItem(new ItemStack(io.github.jimbozoomer.jugcraft.artillery.JugcraftArtillery.HEAVY_SHELL_ITEM, 5));
		BlockPos tablePos = new BlockPos(8, 1, 2);
		helper.setBlock(tablePos, io.github.jimbozoomer.jugcraft.building.FireControl.TABLE.defaultBlockState()
				.setValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING, Direction.SOUTH)
				.setValue(io.github.jimbozoomer.jugcraft.building.FireControlTableBlock.MODE, mode));
		var type = io.github.jimbozoomer.jugcraft.artillery.JugcraftTowerGuns.type("triple_battery");
		io.github.jimbozoomer.jugcraft.artillery.TowerGun battery = helper.spawn(type, new Vec3(2.5, 1, 2.5));
		battery.face(0.0F);
		ServerPlayer layer = helper.makeMockServerPlayerInLevel();
		io.github.jimbozoomer.jugcraft.building.FireControl.startLink(layer, helper.getLevel(), helper.absolutePos(tablePos));
		io.github.jimbozoomer.jugcraft.building.FireControl.link(layer, battery);
		layer.discard();
		return battery;
	}

	/**
	 * Batch 56: linking through the wire, the link limit, a parallel sheaf's spacing, and that the wire on a linked gun
	 * unlinks it.
	 */
	@GameTest(structure = "jugcraft-test:drone_tower", skyAccess = true)
	public void fireControlTableLinksGuns(GameTestHelper helper) {
		var battery = linkedBattery(helper, io.github.jimbozoomer.jugcraft.building.FireControlTableBlock.Mode.PARALLEL);
		BlockPos tablePos = new BlockPos(8, 1, 2);
		var table = helper.getBlockEntity(tablePos, io.github.jimbozoomer.jugcraft.building.FireControlTableBlock.Entity.class);
		helper.assertTrue(table.linked(battery.getUUID()), "The wire should have linked the battery to the table");
		helper.assertTrue(helper.absolutePos(tablePos).equals(battery.director()), "The battery should know its table");
		for (int i = 1; i < io.github.jimbozoomer.jugcraft.building.FireControl.MAX_GUNS; i++) {
			helper.assertTrue(table.link(java.util.UUID.randomUUID()), "A table should take " + io.github.jimbozoomer.jugcraft.building.FireControl.MAX_GUNS + " guns");
		}
		helper.assertFalse(table.link(java.util.UUID.randomUUID()), "A table should refuse a gun past its limit");
		// Three guns on a parallel sheaf straight down the range lay on points six blocks apart across it.
		while (table.links().size() > 3) {
			table.unlink(table.links().get(table.links().size() - 1));
		}
		BlockPos target = helper.absolutePos(new BlockPos(8, 0, 30));
		table.setTarget(target);
		Vec3 first = table.aimPoint(table.links().get(0));
		Vec3 middle = table.aimPoint(table.links().get(1));
		Vec3 last = table.aimPoint(table.links().get(2));
		helper.assertTrue(middle.distanceTo(Vec3.atCenterOf(target).add(0, 0.5, 0)) < 1.0E-6, "The middle gun should lay on the target");
		helper.assertTrue(Math.abs(first.distanceTo(middle) - io.github.jimbozoomer.jugcraft.building.FireControl.SHEAF_SPACING) < 1.0E-6
				&& Math.abs(last.distanceTo(middle) - io.github.jimbozoomer.jugcraft.building.FireControl.SHEAF_SPACING) < 1.0E-6
				&& Math.abs(first.z - last.z) < 1.0E-6, "A parallel sheaf should spread across the line of fire, six blocks apart");
		ServerPlayer layer = helper.makeMockServerPlayerInLevel();
		io.github.jimbozoomer.jugcraft.building.FireControl.startLink(layer, helper.getLevel(), helper.absolutePos(tablePos));
		io.github.jimbozoomer.jugcraft.building.FireControl.link(layer, battery);
		helper.assertFalse(table.linked(battery.getUUID()), "The wire on a linked gun should unlink it");
		helper.assertTrue(battery.director() == null, "An unlinked gun should forget its table");
		helper.succeed();
	}

	/**
	 * Batch 56: an uncrewed battery linked to a table on converge lays on the table's target but holds fire until a
	 * redstone pulse into the table, then fires exactly one salvo from the ready rack; the table's comparator reads it as
	 * ready while it waits.
	 */
	@GameTest(structure = "jugcraft-test:drone_tower", maxTicks = 260, skyAccess = true)
	public void fireControlSalvoOnPulse(GameTestHelper helper) {
		linkedBattery(helper, io.github.jimbozoomer.jugcraft.building.FireControlTableBlock.Mode.CONVERGE);
		BlockPos tablePos = new BlockPos(8, 1, 2);
		var table = helper.getBlockEntity(tablePos, io.github.jimbozoomer.jugcraft.building.FireControlTableBlock.Entity.class);
		table.setTarget(helper.absolutePos(new BlockPos(2, 0, 30)));
		var rack = helper.getBlockEntity(new BlockPos(5, 1, 2), io.github.jimbozoomer.jugcraft.building.ReadyRackBlock.Entity.class);
		helper.runAfterDelay(70, () -> {
			helper.assertTrue(rack.count() == 5, "A laid gun must not fire before the table orders it, but the rack holds " + rack.count());
			helper.assertTrue(table.readyCount(helper.getLevel().getGameTime()) == 1, "The laid, loaded battery should report ready");
			ServerLevel level = helper.getLevel();
			BlockState state = level.getBlockState(helper.absolutePos(tablePos));
			helper.assertTrue(state.getAnalogOutputSignal(level, helper.absolutePos(tablePos), Direction.NORTH) == 1,
					"The table's comparator should read one ready gun");
			helper.setBlock(new BlockPos(9, 1, 2), Blocks.REDSTONE_BLOCK);
		});
		helper.runAfterDelay(160, () -> helper.assertTrue(rack.count() == 2,
				"One pulse should fire one salvo of three shells, leaving 2, but left " + rack.count()));
		helper.runAfterDelay(240, () -> {
			helper.assertTrue(rack.count() == 2, "A held signal must not fire again, but the rack holds " + rack.count());
			helper.succeed();
		});
	}

	/**
	 * Batch 56: on sentry an uncrewed battery ignores a hostile mob outside the table's 90 degree sector and one with a
	 * player beside it, and lays on and fires at one inside the sector.
	 */
	@GameTest(structure = "jugcraft-test:drone_tower", maxTicks = 300, skyAccess = true)
	public void fireControlSentryKeepsToItsSector(GameTestHelper helper) {
		var battery = linkedBattery(helper, io.github.jimbozoomer.jugcraft.building.FireControlTableBlock.Mode.SENTRY);
		var table = helper.getBlockEntity(new BlockPos(8, 1, 2), io.github.jimbozoomer.jugcraft.building.FireControlTableBlock.Entity.class);
		table.setSectorWidth(90);
		var rack = helper.getBlockEntity(new BlockPos(5, 1, 2), io.github.jimbozoomer.jugcraft.building.ReadyRackBlock.Entity.class);
		for (int x = 30; x <= 32; x++) {
			for (int z = 1; z <= 3; z++) {
				helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
			}
		}
		// East of the table: outside a sector facing south.
		var outside = helper.spawnWithNoFreeWill(net.minecraft.world.entity.EntityTypes.HUSK, new Vec3(31.5, 1, 2.5));
		// South, in the sector, but with a player standing beside it.
		for (int x = 1; x <= 3; x++) {
			for (int z = 27; z <= 29; z++) {
				helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
			}
		}
		var guarded = helper.spawnWithNoFreeWill(net.minecraft.world.entity.EntityTypes.HUSK, new Vec3(2.5, 1, 28.5));
		ServerPlayer friend = helper.makeMockServerPlayerInLevel();
		friend.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
		friend.setPos(helper.absoluteVec(new Vec3(4.5, 1, 28.5)));
		for (int tick = 5; tick <= 80; tick += 5) {
			helper.runAfterDelay(tick, () -> {
				var target = battery.sentryTarget();
				helper.assertFalse(target == outside, "A sentry must not engage a mob outside its sector");
				helper.assertFalse(target == guarded, "A sentry must not engage a mob with a player beside it");
			});
		}
		helper.runAfterDelay(85, () -> {
			helper.assertTrue(rack.count() == 5, "Nothing should have been fired yet, but the rack holds " + rack.count());
			friend.discard();
			guarded.discard();
			outside.discard();
			for (int x = 1; x <= 3; x++) {
				for (int z = 31; z <= 33; z++) {
					helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
				}
			}
			// The first salvo is enough to pass, whether or not it kills the husk.
			helper.spawnWithNoFreeWill(net.minecraft.world.entity.EntityTypes.HUSK, new Vec3(2.5, 1, 32.5));
		});
		helper.runAfterDelay(280, () -> {
			helper.assertTrue(rack.count() < 5, "A sentry should fire at a hostile mob in its sector, but the rack still holds " + rack.count());
			helper.succeed();
		});
	}

	/** Batch 57 helper: a stone floor over the whole of a drone_tower test's 44 x 44 ground. */
	private static void raiderFloor(GameTestHelper helper) {
		for (int x = 0; x < 44; x++) {
			for (int z = 0; z < 44; z++) {
				helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
			}
		}
	}

	private static void killOutright(GameTestHelper helper, net.minecraft.world.entity.LivingEntity mob) {
		mob.hurtServer(helper.getLevel(), helper.getLevel().damageSources().genericKill(), 10000.0F);
	}

	/**
	 * Batch 57: every raider is a hostile mob (so sentries and town guards fight it), and a raider's blast hurts the pig
	 * beside it but spares the raider standing just as close.
	 */
	@GameTest(structure = "jugcraft-test:drone_tower", skyAccess = true)
	public void raidersAreHostileAndSpareEachOther(GameTestHelper helper) {
		raiderFloor(helper);
		var grunt = helper.spawnWithNoFreeWill(io.github.jimbozoomer.jugcraft.raiders.JugcraftRaiders.GRUNT, new Vec3(10.5, 1, 10.5));
		var walker = helper.spawnWithNoFreeWill(io.github.jimbozoomer.jugcraft.raiders.JugcraftRaiders.WALKER, new Vec3(20.5, 1, 20.5));
		var blimp = helper.spawnWithNoFreeWill(io.github.jimbozoomer.jugcraft.raiders.JugcraftRaiders.BLIMP, new Vec3(30.5, 8, 30.5));
		for (Object raider : new Object[] {grunt, walker, blimp}) {
			helper.assertTrue(raider instanceof net.minecraft.world.entity.monster.Enemy, raider + " should be a hostile mob");
			helper.assertTrue(raider instanceof io.github.jimbozoomer.jugcraft.raiders.Raider, raider + " should be a raider");
		}
		var pig = helper.spawnWithNoFreeWill(net.minecraft.world.entity.EntityTypes.PIG, new Vec3(12.5, 1, 10.5));
		Vec3 center = helper.absoluteVec(new Vec3(11.5, 1.5, 10.5));
		io.github.jimbozoomer.jugcraft.weapons.Blast.detonate(helper.getLevel(), center, null, null,
				io.github.jimbozoomer.jugcraft.raiders.JugcraftRaiders.BOMB_RADIUS, io.github.jimbozoomer.jugcraft.raiders.JugcraftRaiders.BOMB_DAMAGE,
				target -> target instanceof io.github.jimbozoomer.jugcraft.raiders.Raider);
		helper.assertTrue(grunt.getHealth() == grunt.getMaxHealth(), "A raider's blast should spare the raider");
		helper.assertTrue(pig.getHealth() < pig.getMaxHealth(), "A raider's blast should hurt the pig");
		helper.succeed();
	}

	/**
	 * Batch 57: a level 1 raid gathers its party (two grunts, a grenadier, an officer and, slice 10F of the guns, a
	 * gunner) bound for its objective; when every raider has fallen the raid is won and the world's raid level goes up by
	 * one (to at most five).
	 */
	@GameTest(structure = "jugcraft-test:drone_tower", maxTicks = 200, skyAccess = true)
	public void raidIsWonWhenEveryRaiderFalls(GameTestHelper helper) {
		raiderFloor(helper);
		ServerLevel level = helper.getLevel();
		int before = io.github.jimbozoomer.jugcraft.raiders.RaiderRaids.raidLevel(level);
		BlockPos objective = helper.absolutePos(new BlockPos(5, 1, 5));
		var raid = io.github.jimbozoomer.jugcraft.raiders.RaiderRaids.start(level, objective, helper.absolutePos(new BlockPos(35, 1, 35)), 1,
				level.getRandom());
		helper.assertTrue(raid.total() == 5 && raid.members().size() == 5, "A level 1 raid should bring five raiders, not " + raid.total());
		int[] kinds = new int[io.github.jimbozoomer.jugcraft.raiders.RaiderInfantry.Role.values().length];
		for (java.util.UUID id : raid.members()) {
			if (level.getEntity(id) instanceof io.github.jimbozoomer.jugcraft.raiders.RaiderInfantry raider) {
				kinds[raider.role().ordinal()]++;
				helper.assertTrue(objective.equals(raider.objective()) && raid.id().equals(raider.raid()), "Each raider should know its raid");
			}
		}
		helper.assertTrue(kinds[0] == 2 && kinds[1] == 1 && kinds[2] == 1 && kinds[3] == 1,
				"Two grunts, a grenadier, an officer and a gunner, not " + java.util.Arrays.toString(kinds));
		helper.assertTrue(io.github.jimbozoomer.jugcraft.raiders.RaiderRaids.active(level, raid.id()), "The raid should be under way");
		helper.runAfterDelay(5, () -> {
			for (java.util.UUID id : raid.members()) {
				if (level.getEntity(id) instanceof net.minecraft.world.entity.LivingEntity raider) {
					killOutright(helper, raider);
				}
			}
		});
		helper.succeedWhen(() -> {
			helper.assertFalse(io.github.jimbozoomer.jugcraft.raiders.RaiderRaids.active(level, raid.id()), "With every raider fallen the raid should end");
			int after = io.github.jimbozoomer.jugcraft.raiders.RaiderRaids.raidLevel(level);
			helper.assertTrue(after >= Math.min(5, before + 1), "A won raid should raise the raid level from " + before + ", not leave it at " + after);
		});
	}

	/**
	 * Batch 57: a raid that withdraws takes its loaded raiders with it, and a raider whose raid has ended (it was
	 * elsewhere when the raid withdrew) leaves at its next check.
	 */
	@GameTest(structure = "jugcraft-test:drone_tower", maxTicks = 300, skyAccess = true)
	public void raidersWithdrawWithTheirRaid(GameTestHelper helper) {
		raiderFloor(helper);
		ServerLevel level = helper.getLevel();
		var raid = io.github.jimbozoomer.jugcraft.raiders.RaiderRaids.start(level, helper.absolutePos(new BlockPos(5, 1, 5)),
				helper.absolutePos(new BlockPos(30, 1, 30)), 1, level.getRandom());
		var members = raid.members();
		io.github.jimbozoomer.jugcraft.raiders.RaiderRaids.withdraw(level, raid);
		helper.assertFalse(io.github.jimbozoomer.jugcraft.raiders.RaiderRaids.active(level, raid.id()), "A withdrawn raid should be over");
		for (java.util.UUID id : members) {
			helper.assertTrue(level.getEntity(id) == null, "Its raiders should leave with it");
		}
		var straggler = helper.spawn(io.github.jimbozoomer.jugcraft.raiders.JugcraftRaiders.GRUNT, new Vec3(20.5, 1, 20.5));
		straggler.joinRaid(java.util.UUID.randomUUID(), helper.absolutePos(new BlockPos(5, 1, 5)));
		helper.succeedWhen(() -> helper.assertTrue(straggler.isRemoved(), "A raider whose raid is over should leave"));
	}

	/**
	 * Batch 57: an officer rallies the raiders near them (Speed and Strength); when the officer falls, the raiders lose
	 * heart (Weakness).
	 */
	@GameTest(structure = "jugcraft-test:drone_tower", maxTicks = 200, skyAccess = true)
	public void officerRalliesAndTheirFallRoutsTheRest(GameTestHelper helper) {
		raiderFloor(helper);
		var officer = helper.spawn(io.github.jimbozoomer.jugcraft.raiders.JugcraftRaiders.OFFICER, new Vec3(20.5, 1, 20.5));
		var grunt = helper.spawn(io.github.jimbozoomer.jugcraft.raiders.JugcraftRaiders.GRUNT, new Vec3(23.5, 1, 20.5));
		helper.runAfterDelay(io.github.jimbozoomer.jugcraft.raiders.JugcraftRaiders.RALLY_TICKS + 5, () -> {
			helper.assertTrue(grunt.hasEffect(net.minecraft.world.effect.MobEffects.SPEED)
					&& grunt.hasEffect(net.minecraft.world.effect.MobEffects.STRENGTH), "The officer should rally the grunt");
			killOutright(helper, officer);
			helper.assertTrue(grunt.hasEffect(net.minecraft.world.effect.MobEffects.WEAKNESS), "The officer's fall should rout the grunt");
			helper.assertFalse(grunt.hasEffect(net.minecraft.world.effect.MobEffects.STRENGTH), "A routed raider loses the rally's strength");
			helper.succeed();
		});
	}

	/** Batch 57: a grenadier's throw puts a grenade in the air toward its target. */
	@GameTest(structure = "jugcraft-test:drone_tower", maxTicks = 100, skyAccess = true)
	public void grenadierLobsGrenades(GameTestHelper helper) {
		raiderFloor(helper);
		var grenadier = helper.spawn(io.github.jimbozoomer.jugcraft.raiders.JugcraftRaiders.GRENADIER, new Vec3(10.5, 1, 20.5));
		var pig = helper.spawnWithNoFreeWill(net.minecraft.world.entity.EntityTypes.PIG, new Vec3(22.5, 1, 20.5));
		grenadier.performRangedAttack(pig, 1.0F);
		net.minecraft.world.phys.AABB area = new net.minecraft.world.phys.AABB(helper.absolutePos(BlockPos.ZERO)).expandTowards(44, 26, 44);
		var bombs = helper.getLevel().getEntitiesOfClass(io.github.jimbozoomer.jugcraft.raiders.RaiderBomb.class, area);
		helper.assertTrue(bombs.size() == 1 && !bombs.get(0).heavy(), "A throw should put one grenade in the air, not " + bombs.size());
		helper.assertTrue(bombs.get(0).getDeltaMovement().x > 0, "The grenade should fly toward the pig");
		// Too close to throw at, it clubs instead.
		pig.teleportTo(grenadier.getX() + 1.5, grenadier.getY(), grenadier.getZ());
		grenadier.performRangedAttack(pig, 1.0F);
		helper.assertTrue(helper.getLevel().getEntitiesOfClass(io.github.jimbozoomer.jugcraft.raiders.RaiderBomb.class, area).size() == 1,
				"A grenadier should not throw at something closer than " + io.github.jimbozoomer.jugcraft.raiders.JugcraftRaiders.GRENADE_MIN_RANGE + " blocks");
		helper.succeed();
	}

	/**
	 * Batch 57: raiders find and go for the people they hunt by themselves: a grunt hunts a townsperson down to striking
	 * distance, and a grenadier lobs grenades at them. (A townsperson stands in for a player: the test server's mock
	 * player is always in creative mode, and no hostile mob targets a creative player. Townsfolk cannot be hurt, so the
	 * test checks the grunt closes in rather than that the blow lands; the fist itself is vanilla's.)
	 */
	@GameTest(structure = "jugcraft-test:drone_tower", maxTicks = 400, skyAccess = true)
	public void raidersHuntOnTheirOwn(GameTestHelper helper) {
		raiderFloor(helper);
		var victim = helper.spawnWithNoFreeWill(io.github.jimbozoomer.jugcraft.town.JugcraftTown.TOWNSFOLK, new Vec3(20.5, 1, 20.5));
		var grunt = helper.spawn(io.github.jimbozoomer.jugcraft.raiders.JugcraftRaiders.GRUNT, new Vec3(16.5, 1, 20.5));
		var grenadier = helper.spawn(io.github.jimbozoomer.jugcraft.raiders.JugcraftRaiders.GRENADIER, new Vec3(20.5, 1, 8.5));
		net.minecraft.world.phys.AABB area = new net.minecraft.world.phys.AABB(helper.absolutePos(BlockPos.ZERO)).expandTowards(44, 26, 44);
		boolean[] threw = {false};
		boolean[] closed = {false};
		helper.onEachTick(() -> {
			threw[0] |= !helper.getLevel().getEntitiesOfClass(io.github.jimbozoomer.jugcraft.raiders.RaiderBomb.class, area).isEmpty();
			closed[0] |= grunt.getTarget() == victim && grunt.distanceTo(victim) < 2.0F;
		});
		helper.succeedWhen(() -> {
			helper.assertTrue(grunt.getTarget() == victim, "The grunt should hunt the townsperson, not " + grunt.getTarget());
			helper.assertTrue(closed[0], "The grunt should close to striking distance, but is " + grunt.distanceTo(victim) + " away");
			helper.assertTrue(threw[0], "The grenadier should have thrown at the townsperson (it hunts " + grenadier.getTarget() + ")");
		});
	}

	/** Batch 57: a blimp climbs to cruise well above what it hunts. */
	@GameTest(structure = "jugcraft-test:drone_tower", maxTicks = 300, skyAccess = true)
	public void blimpCruisesOverItsQuarry(GameTestHelper helper) {
		raiderFloor(helper);
		var blimp = helper.spawn(io.github.jimbozoomer.jugcraft.raiders.JugcraftRaiders.BLIMP, new Vec3(30.5, 2, 30.5));
		var pig = helper.spawnWithNoFreeWill(net.minecraft.world.entity.EntityTypes.PIG, new Vec3(20.5, 1, 20.5));
		double start = blimp.getY();
		helper.succeedWhen(() -> {
			if (blimp.getTarget() == null) {
				blimp.setTarget(pig);
			}
			helper.assertTrue(blimp.getY() > start + 8, "The blimp should climb toward its cruising height: at " + blimp.getY()
					+ " from " + start + ", hunting " + blimp.getTarget() + ", moving " + blimp.getDeltaMovement() + ", removed " + blimp.isRemoved());
		});
	}

	/** Coal generator -> copper cables -> electric furnace smelts raw iron. */
	@GameTest(maxTicks = 600)
	public void cablesCarryPower(GameTestHelper helper) {
		BlockPos generator = new BlockPos(1, 1, 3);
		BlockPos furnace = new BlockPos(5, 1, 3);
		helper.setBlock(generator, machine(MachineKind.COAL_GENERATOR));
		for (int x = 2; x <= 4; x++) {
			helper.setBlock(new BlockPos(x, 1, 3), JugcraftMachines.COPPER_CABLE);
		}
		helper.setBlock(furnace, machine(MachineKind.ELECTRIC_FURNACE));
		helper.getBlockEntity(generator, MachineBlockEntity.class).setItem(0, new ItemStack(Items.COAL));
		MachineBlockEntity electricFurnace = helper.getBlockEntity(furnace, MachineBlockEntity.class);
		electricFurnace.setItem(0, new ItemStack(Items.RAW_IRON));
		helper.succeedWhen(() -> {
			ItemStack output = electricFurnace.getItem(MachineKind.ELECTRIC_FURNACE.outputSlot());
			helper.assertTrue(output.is(Items.IRON_INGOT), "Electric furnace output is " + output);
		});
	}

	/** An electric pump on water pushes it through bronze pipes into a tank. */
	@GameTest(maxTicks = 200)
	public void pumpFillsTankThroughPipes(GameTestHelper helper) {
		helper.setBlock(new BlockPos(1, 1, 3), Blocks.WATER);
		BlockPos pump = new BlockPos(1, 2, 3);
		helper.setBlock(pump, JugcraftFluids.ELECTRIC_PUMP);
		helper.getBlockEntity(pump, ElectricPumpBlockEntity.class).energy().setAmount(ElectricPumpBlockEntity.ENERGY_CAPACITY);
		for (int x = 2; x <= 3; x++) {
			helper.setBlock(new BlockPos(x, 2, 3), JugcraftFluids.BRONZE_FLUID_PIPE);
		}
		BlockPos tank = new BlockPos(4, 2, 3);
		helper.setBlock(tank, JugcraftFluids.FLUID_TANK);
		FluidTankBlockEntity tankEntity = helper.getBlockEntity(tank, FluidTankBlockEntity.class);
		helper.succeedWhen(() -> helper.assertTrue(tankEntity.storage.amount > 0, "Tank is still empty"));
	}

	/** A powered fluid valve stops a pump's water short of the tank; once the signal goes, the water gets through. */
	@GameTest(maxTicks = 300)
	public void fluidValveClosesOnRedstone(GameTestHelper helper) {
		helper.setBlock(new BlockPos(1, 1, 3), Blocks.WATER);
		BlockPos pump = new BlockPos(1, 2, 3);
		helper.setBlock(pump, JugcraftFluids.ELECTRIC_PUMP);
		helper.getBlockEntity(pump, ElectricPumpBlockEntity.class).energy().setAmount(ElectricPumpBlockEntity.ENERGY_CAPACITY);
		helper.setBlock(new BlockPos(2, 2, 3), JugcraftFluids.STEEL_FLUID_PIPE);
		BlockPos valve = new BlockPos(3, 2, 3);
		helper.setBlock(valve, JugcraftFluids.FLUID_VALVE);
		BlockPos signal = valve.above();
		helper.setBlock(signal, Blocks.REDSTONE_BLOCK);
		helper.setBlock(new BlockPos(4, 2, 3), JugcraftFluids.STEEL_FLUID_PIPE);
		BlockPos tank = new BlockPos(5, 2, 3);
		helper.setBlock(tank, JugcraftFluids.FLUID_TANK);
		FluidTankBlockEntity tankEntity = helper.getBlockEntity(tank, FluidTankBlockEntity.class);
		java.util.concurrent.atomic.AtomicBoolean opened = new java.util.concurrent.atomic.AtomicBoolean();
		helper.runAfterDelay(60, () -> {
			helper.assertTrue(helper.getBlockState(valve).getValue(FluidValveBlock.POWERED), "The valve is not closed");
			helper.assertTrue(tankEntity.storage.amount == 0, "Water got past the closed valve");
			helper.setBlock(signal, Blocks.AIR);
			opened.set(true);
		});
		helper.succeedWhen(() -> {
			helper.assertTrue(opened.get(), "The valve has not been opened yet");
			helper.assertTrue(tankEntity.storage.amount > 0, "The tank is still empty with the valve open");
		});
	}

	/**
	 * A fluid filter lets nothing into the tank it touches until it is set, and then only its fluid; a tank on an
	 * ordinary pipe of the same line fills all along.
	 */
	@GameTest(maxTicks = 300)
	public void fluidFilterLetsOnlyItsFluidOut(GameTestHelper helper) {
		helper.setBlock(new BlockPos(1, 1, 3), Blocks.WATER);
		BlockPos pump = new BlockPos(1, 2, 3);
		helper.setBlock(pump, JugcraftFluids.ELECTRIC_PUMP);
		helper.getBlockEntity(pump, ElectricPumpBlockEntity.class).energy().setAmount(ElectricPumpBlockEntity.ENERGY_CAPACITY);
		helper.setBlock(new BlockPos(2, 2, 3), JugcraftFluids.STEEL_FLUID_PIPE);
		BlockPos filter = new BlockPos(3, 2, 3);
		helper.setBlock(filter, JugcraftFluids.FLUID_FILTER);
		BlockPos open = new BlockPos(2, 2, 2);
		BlockPos filtered = new BlockPos(4, 2, 3);
		helper.setBlock(open, JugcraftFluids.FLUID_TANK);
		helper.setBlock(filtered, JugcraftFluids.FLUID_TANK);
		FluidTankBlockEntity openTank = helper.getBlockEntity(open, FluidTankBlockEntity.class);
		FluidTankBlockEntity filteredTank = helper.getBlockEntity(filtered, FluidTankBlockEntity.class);
		FluidFilterBlockEntity filterEntity = helper.getBlockEntity(filter, FluidFilterBlockEntity.class);
		java.util.concurrent.atomic.AtomicBoolean set = new java.util.concurrent.atomic.AtomicBoolean();
		helper.runAfterDelay(40, () -> {
			helper.assertTrue(openTank.storage.amount > 0, "The tank on the ordinary pipe is empty");
			helper.assertTrue(filteredTank.storage.amount == 0, "An unset filter let water out");
			filterEntity.setFilter(FluidVariant.of(Fluids.LAVA));
		});
		helper.runAfterDelay(80, () -> {
			helper.assertTrue(filteredTank.storage.amount == 0, "A lava filter let water out");
			filterEntity.setFilter(FluidVariant.of(Fluids.WATER));
			set.set(true);
		});
		helper.succeedWhen(() -> {
			helper.assertTrue(set.get(), "The filter is not set to water yet");
			helper.assertTrue(filteredTank.storage.amount > 0, "A water filter let no water out");
		});
	}

	/** The crop harvester harvests a ripe wheat crop in its field, keeps the wheat, and plants one of the seeds again. */
	@GameTest(maxTicks = 200)
	public void cropHarvesterHarvestsAndReplants(GameTestHelper helper) {
		BlockPos master = new BlockPos(4, 1, 0);
		LargeMachineBlock block = (LargeMachineBlock) JugcraftMachines.MACHINES.get(MachineKind.CROP_HARVESTER);
		helper.setBlock(master, block.defaultBlockState().setValue(MachineBlock.FACING, Direction.SOUTH));
		block.setPlacedBy(helper.getLevel(), helper.absolutePos(master), helper.getBlockState(master), null, ItemStack.EMPTY);
		charge(helper, master.above(), Direction.UP);
		MachineBlockEntity harvester = helper.getBlockEntity(master, MachineBlockEntity.class);
		// The field starts the block in front (south); a ripe crop two blocks out and one to the side.
		BlockPos crop = new BlockPos(3, 1, 2);
		helper.setBlock(crop.below(), Blocks.FARMLAND);
		helper.setBlock(crop, ((CropBlock) Blocks.WHEAT).getStateForAge(7));
		helper.succeedWhen(() -> {
			BlockState replanted = helper.getBlockState(crop);
			helper.assertTrue(replanted.is(Blocks.WHEAT) && ((CropBlock) Blocks.WHEAT).getAge(replanted) == 0,
					"The crop is now " + replanted);
			boolean wheat = false;
			for (int slot = 0; slot < MachineKind.CROP_HARVESTER.slots; slot++) {
				wheat |= harvester.getItem(slot).is(Items.WHEAT);
			}
			helper.assertTrue(wheat, "The harvester kept no wheat");
		});
	}

	/**
	 * Cotton: the seeds plant the crop on farmland, a ripe crop drops cotton and seeds (an unripe one only a seed), cotton counts as a crop for
	 * fertilizer, and sifting coarse dirt can turn up the seeds.
	 */
	@GameTest
	public void cottonGrowsFromSeedsAndDropsCotton(GameTestHelper helper) {
		BlockPos crop = new BlockPos(2, 2, 2);
		helper.setBlock(crop.below(), Blocks.FARMLAND);
		helper.setBlock(crop, JugcraftFarming.COTTON_CROP);
		helper.assertTrue(helper.getBlockState(crop).is(BlockTags.CROPS), "Cotton is not in minecraft:crops");
		CropBlock cotton = (CropBlock) JugcraftFarming.COTTON_CROP;
		helper.assertTrue(cotton.asItem() == JugcraftFarming.COTTON_SEEDS, "Cotton seeds do not plant cotton");
		BlockState ripe = cotton.getStateForAge(cotton.getMaxAge());
		helper.setBlock(crop, ripe);
		List<ItemStack> drops = Block.getDrops(ripe, helper.getLevel(), helper.absolutePos(crop), null);
		helper.assertTrue(drops.stream().anyMatch(stack -> stack.is(JugcraftFarming.COTTON)), "A ripe crop dropped " + drops);
		helper.assertTrue(drops.stream().anyMatch(stack -> stack.is(JugcraftFarming.COTTON_SEEDS)), "A ripe crop dropped " + drops);
		List<ItemStack> unripe = Block.getDrops(cotton.getStateForAge(3), helper.getLevel(), helper.absolutePos(crop), null);
		helper.assertTrue(unripe.stream().noneMatch(stack -> stack.is(JugcraftFarming.COTTON))
				&& unripe.stream().anyMatch(stack -> stack.is(JugcraftFarming.COTTON_SEEDS)), "An unripe crop dropped " + unripe);
		MachineRecipe sifting = MachineRecipes.find(helper.getLevel(), MachineKind.SIEVE, new ItemStack(Items.COARSE_DIRT))
				.orElseThrow(() -> helper.assertionException("No sifting recipe for coarse dirt"));
		helper.assertTrue(sifting.byproducts().stream().anyMatch(b -> b.result().create().is(JugcraftFarming.COTTON_SEEDS)),
				"Sifting coarse dirt never gives cotton seeds");
		helper.succeed();
	}

	/** The 3x2x6 alloy smelter: places all 36 blocks, takes power only at its socket, makes bronze. */
	@GameTest(maxTicks = 400)
	public void alloySmelterMakesBronze(GameTestHelper helper) {
		BlockPos master = new BlockPos(6, 1, 3);
		LargeMachineBlock block = (LargeMachineBlock) JugcraftMachines.MACHINES.get(MachineKind.ALLOY_SMELTER);
		BlockState state = block.defaultBlockState().setValue(MachineBlock.FACING, Direction.NORTH);
		helper.setBlock(master, state);
		block.setPlacedBy(helper.getLevel(), helper.absolutePos(master), helper.getBlockState(master), null, ItemStack.EMPTY);
		Footprint footprint = MachineKind.ALLOY_SMELTER.footprint();
		for (int part = 0; part < footprint.size(); part++) {
			BlockPos at = footprint.partPos(helper.absolutePos(master), Direction.NORTH, part);
			helper.assertTrue(helper.getLevel().getBlockState(at).is(block), "Part " + part + " is missing");
		}
		helper.assertTrue(EnergyStorage.SIDED.find(helper.getLevel(), helper.absolutePos(master), Direction.NORTH) == null,
				"The alloy smelter must not take power at its front");
		// The socket is on the outer (west) side of the lower right front block, two to the right of the master.
		charge(helper, master.west(2), Direction.WEST);
		MachineBlockEntity smelter = helper.getBlockEntity(master, MachineBlockEntity.class);
		smelter.setItem(0, new ItemStack(Items.COPPER_INGOT, 3));
		smelter.setItem(1, new ItemStack(item("tin_ingot")));
		helper.succeedWhen(() -> {
			ItemStack output = smelter.getItem(MachineKind.ALLOY_SMELTER.outputSlot());
			helper.assertTrue(output.is(item("bronze_ingot")) && output.getCount() == 4, "Alloy smelter output is " + output);
		});
	}

	/**
	 * Glass chemistry (batch 16): tincal drops 1-3 borax; sand and borax melt into borosilicate glass, drawn into
	 * optical fibre; iron and borax make ferroboron, which doubles the magnets a rare earth oxide gives; fibre can stand
	 * in for gold in a processor.
	 */
	@GameTest
	public void glassChemistryRecipes(GameTestHelper helper) {
		BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
		BlockState tincal = BuiltInRegistries.BLOCK.getValue(Jugcraft.id("tincal")).defaultBlockState();
		for (int i = 0; i < 8; i++) {
			int borax = Block.getDrops(tincal, helper.getLevel(), pos, null).stream().filter(s -> s.is(item("borax")))
					.mapToInt(ItemStack::getCount).sum();
			helper.assertTrue(borax >= 1 && borax <= 3, "Tincal dropped " + borax + " borax");
		}
		assertMulti(helper, MachineKind.ALLOY_SMELTER, List.of(new ItemStack(Items.SAND, 2), new ItemStack(item("borax"))),
				item("borosilicate_glass"), 2);
		assertMulti(helper, MachineKind.ALLOY_SMELTER, List.of(new ItemStack(Items.IRON_INGOT), new ItemStack(item("borax"))),
				item("ferroboron"), 1);
		assertMulti(helper, MachineKind.ALLOY_SMELTER, List.of(new ItemStack(item("rare_earth_oxide")),
				new ItemStack(item("ferroboron"))), item("neodymium_magnet"), 2);
		MachineRecipe fibre = MachineRecipes.find(helper.getLevel(), MachineKind.WIRE_DRAWER, new ItemStack(item("borosilicate_glass")))
				.orElseThrow(() -> helper.assertionException("No drawing recipe for borosilicate glass"));
		ItemStack drawn = fibre.output().create();
		helper.assertTrue(drawn.is(item("optical_fibre")) && drawn.getCount() == 4, "Glass draws into " + drawn);
		assertMulti(helper, MachineKind.CIRCUIT_ASSEMBLER, List.of(new ItemStack(item("microchip"), 4),
				new ItemStack(item("advanced_circuit")), new ItemStack(item("optical_fibre"), 2)), item("processor"), 1);
		helper.succeed();
	}

	private static void assertMulti(GameTestHelper helper, MachineKind kind, List<ItemStack> inputs, Item result, int count) {
		ItemStack out = MachineRecipes.findMulti(helper.getLevel(), kind, inputs)
				.orElseThrow(() -> helper.assertionException("No " + kind.id + " recipe for " + inputs))
				.recipe().output().create();
		helper.assertTrue(out.is(result) && out.getCount() == count, kind.id + " makes " + out + " from " + inputs);
	}

	/**
	 * The arc furnace pulls a silicon boule from 4 silicon and a phosphate (batch 24, from the old crystal grower); the
	 * sawmill cuts it into 8 wafers. Its one-ingredient recipes still work alongside.
	 */
	@GameTest(maxTicks = 600)
	public void arcFurnacePullsABoule(GameTestHelper helper) {
		for (int x = 2; x <= 4; x++) {
			for (int y = 1; y <= 3; y++) {
				for (int z = 1; z <= 3; z++) {
					helper.setBlock(new BlockPos(x, y, z), JugcraftMachines.ARC_FURNACE_CASING);
				}
			}
		}
		BlockPos controller = new BlockPos(3, 2, 1);
		helper.setBlock(controller, machine(MachineKind.ARC_FURNACE).setValue(MachineBlock.FACING, Direction.NORTH));
		charge(helper, controller, Direction.NORTH);
		MachineBlockEntity furnace = helper.getBlockEntity(controller, MachineBlockEntity.class);
		furnace.setItem(0, new ItemStack(item("phosphate")));
		furnace.setItem(1, new ItemStack(item("silicon"), 4));
		assertMulti(helper, MachineKind.ARC_FURNACE, List.of(new ItemStack(Items.QUARTZ)), item("silicon"), 2);
		MachineRecipe wafers = MachineRecipes.find(helper.getLevel(), MachineKind.SAWMILL, new ItemStack(item("silicon_boule")))
				.orElseThrow(() -> helper.assertionException("No sawing recipe for a silicon boule"));
		ItemStack sawn = wafers.output().create();
		helper.assertTrue(sawn.is(item("silicon_wafer")) && sawn.getCount() == 8, "A boule saws into " + sawn);
		helper.succeedWhen(() -> {
			ItemStack output = furnace.getItem(MachineKind.ARC_FURNACE.outputSlot());
			helper.assertTrue(output.is(item("silicon_boule")), "Arc furnace output is " + output);
			helper.assertTrue(furnace.getItem(0).isEmpty() && furnace.getItem(1).isEmpty(), "The inputs were not used up");
		});
	}

	/** The circuit assembler bonds four microchips to an advanced circuit with gold: a processor. */
	@GameTest(maxTicks = 600)
	public void circuitAssemblerMakesAProcessor(GameTestHelper helper) {
		MachineBlockEntity assembler = processing(helper, new BlockPos(2, 1, 2), MachineKind.CIRCUIT_ASSEMBLER,
				new ItemStack(item("microchip"), 4));
		assembler.setItem(1, new ItemStack(item("advanced_circuit")));
		assembler.setItem(2, new ItemStack(Items.GOLD_INGOT));
		helper.succeedWhen(() -> {
			ItemStack output = assembler.getItem(MachineKind.CIRCUIT_ASSEMBLER.outputSlot());
			helper.assertTrue(output.is(item("processor")), "Circuit assembler output is " + output);
		});
	}

	/**
	 * The network terminal reads the network it is cabled to: four cables, a battery box and a capacitor bank whose two
	 * lower blocks both touch the cables. The bank counts once.
	 */
	@GameTest
	public void networkTerminalReadsItsNetwork(GameTestHelper helper) {
		BlockPos terminal = new BlockPos(1, 1, 2);
		helper.setBlock(terminal, JugcraftElectronics.NETWORK_TERMINAL);
		helper.assertTrue(NetworkTerminalBlock.read(helper.getLevel(), helper.absolutePos(terminal)) == null,
				"A terminal with no cable read a network");
		for (BlockPos cable : List.of(new BlockPos(2, 1, 2), new BlockPos(2, 1, 3), new BlockPos(3, 1, 3), new BlockPos(4, 1, 3))) {
			helper.setBlock(cable, JugcraftMachines.COPPER_CABLE);
		}
		helper.setBlock(new BlockPos(2, 1, 1), machine(MachineKind.BATTERY_BOX));
		((SimpleEnergyStorage) helper.getBlockEntity(new BlockPos(2, 1, 1), MachineBlockEntity.class).energyFor(null)).setAmount(100_000);
		MachineBlockEntity bank = large(helper, new BlockPos(4, 1, 4), MachineKind.CAPACITOR_BANK);
		((SimpleEnergyStorage) bank.energyFor(null)).setAmount(1_000_000);
		NetworkTerminalBlock.Reading reading = NetworkTerminalBlock.read(helper.getLevel(), helper.absolutePos(terminal));
		helper.assertTrue(reading != null, "The terminal found no network");
		helper.assertTrue(reading.cables() == 4, "Cables: " + reading.cables());
		helper.assertTrue(reading.devices() == 2, "Devices: " + reading.devices());
		helper.assertTrue(reading.stored() == 1_100_000, "Stored: " + reading.stored());
		helper.assertTrue(reading.capacity() == 4_400_000, "Capacity: " + reading.capacity());
		helper.succeed();
	}

	/**
	 * Loot tables load in Minecraft 26.x's format: the charging station drops once (from its lower half only) and salt
	 * ore drops two to four salt. With the older keys, which 26.x ignores, both halves dropped and the ore dropped one.
	 */
	@GameTest
	public void lootTablesKeepTheirConditionsAndCounts(GameTestHelper helper) {
		BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
		BlockState lower = JugcraftTools.CHARGING_STATION.defaultBlockState();
		BlockState upper = lower.setValue(ChargingStationBlock.HALF, DoubleBlockHalf.UPPER);
		helper.assertTrue(Block.getDrops(lower, helper.getLevel(), pos, null).size() == 1, "The lower half did not drop the station");
		helper.assertTrue(Block.getDrops(upper, helper.getLevel(), pos, null).isEmpty(), "The upper half dropped a second station");
		BlockState saltOre = BuiltInRegistries.BLOCK.getValue(Jugcraft.id("salt_ore")).defaultBlockState();
		for (int i = 0; i < 8; i++) {
			int salt = Block.getDrops(saltOre, helper.getLevel(), pos, null).stream().mapToInt(ItemStack::getCount).sum();
			helper.assertTrue(salt >= 2 && salt <= 4, "Salt ore dropped " + salt + " salt");
		}
		helper.succeed();
	}

	/**
	 * A broken tank drops with its fluid and a placed one takes it back: a tinplate tank with five buckets of water
	 * and a steel tank broken from any of its blocks both keep what they held, and an empty tank's item carries nothing.
	 */
	@GameTest(maxTicks = 40)
	public void tanksKeepTheirFluidWhenBroken(GameTestHelper helper) {
		BlockPos tank = new BlockPos(1, 1, 1);
		helper.setBlock(tank, JugcraftFluids.FLUID_TANK);
		FluidTankBlockEntity tankEntity = helper.getBlockEntity(tank, FluidTankBlockEntity.class);
		List<ItemStack> empty = Block.getDrops(helper.getBlockState(tank), helper.getLevel(), helper.absolutePos(tank), tankEntity);
		helper.assertTrue(empty.size() == 1 && !empty.get(0).has(JugcraftFluids.STORED_FLUID), "An empty tank dropped with fluid");
		try (Transaction transaction = Transaction.openOuter()) {
			tankEntity.storage.insert(FluidVariant.of(Fluids.WATER), 5 * FluidConstants.BUCKET, transaction);
			transaction.commit();
		}
		List<ItemStack> drops = Block.getDrops(helper.getBlockState(tank), helper.getLevel(), helper.absolutePos(tank), tankEntity);
		helper.assertTrue(drops.size() == 1, "The tank dropped " + drops.size() + " stacks");
		StoredFluid stored = drops.get(0).get(JugcraftFluids.STORED_FLUID);
		helper.assertTrue(stored != null && stored.variant().isOf(Fluids.WATER) && stored.amount() == 5 * FluidConstants.BUCKET,
				"The tank's item does not carry its water: " + stored);
		BlockPos placed = new BlockPos(3, 1, 1);
		helper.setBlock(placed, JugcraftFluids.FLUID_TANK);
		FluidTankBlockEntity placedEntity = helper.getBlockEntity(placed, FluidTankBlockEntity.class);
		placedEntity.applyComponentsFromItemStack(drops.get(0));
		helper.assertTrue(placedEntity.storage.variant.isOf(Fluids.WATER) && placedEntity.storage.amount == 5 * FluidConstants.BUCKET,
				"The placed tank did not take back its water");

		BlockPos base = new BlockPos(5, 1, 1);
		LargeMachineBlock steel = (LargeMachineBlock) JugcraftMachines.MACHINES.get(MachineKind.STEEL_TANK);
		helper.setBlock(base, steel.defaultBlockState());
		steel.setPlacedBy(helper.getLevel(), helper.absolutePos(base), helper.getBlockState(base), null, ItemStack.EMPTY);
		MachineBlockEntity steelEntity = helper.getBlockEntity(base, MachineBlockEntity.class);
		try (Transaction transaction = Transaction.openOuter()) {
			steelEntity.reservoir().insert(FluidVariant.of(Fluids.LAVA), 20 * FluidConstants.BUCKET, transaction);
			transaction.commit();
		}
		List<ItemStack> steelDrops = Block.getDrops(helper.getBlockState(base), helper.getLevel(), helper.absolutePos(base), steelEntity);
		StoredFluid lava = steelDrops.isEmpty() ? null : steelDrops.get(0).get(JugcraftFluids.STORED_FLUID);
		helper.assertTrue(steelDrops.size() == 1 && lava != null && lava.variant().isOf(Fluids.LAVA) && lava.amount() == 20 * FluidConstants.BUCKET,
				"The steel tank's item does not carry its lava: " + steelDrops);
		// Break another block of the 2x2 tank (not the master, which holds the fluid): the whole tank drops once.
		BlockPos other = null;
		for (BlockPos near : BlockPos.betweenClosed(base.offset(-1, 0, -1), base.offset(1, 0, 1))) {
			if (!near.equals(base) && helper.getBlockState(near).is(steel)) {
				other = near.immutable();
			}
		}
		helper.assertTrue(other != null, "The steel tank did not form");
		BlockPos broken = other;
		helper.destroyBlock(broken);
		helper.succeedWhen(() -> {
			helper.assertBlockNotPresent(steel, base);
			helper.assertBlockNotPresent(steel, broken);
			int tanks = helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
					new net.minecraft.world.phys.AABB(helper.absolutePos(base)).inflate(3),
					item -> item.getItem().has(JugcraftFluids.STORED_FLUID) && item.getItem().get(JugcraftFluids.STORED_FLUID).amount() == 20 * FluidConstants.BUCKET).size();
			helper.assertTrue(tanks == 1, "Breaking the steel tank's top dropped " + tanks + " full tanks");
		});
	}

	/** Breaking any block of a multi-block machine removes the whole machine (here the nine-block wind turbine). */
	@GameTest(maxTicks = 40)
	public void breakingOnePartRemovesTheMachine(GameTestHelper helper) {
		BlockPos base = new BlockPos(3, 1, 3);
		LargeMachineBlock block = (LargeMachineBlock) JugcraftMachines.MACHINES.get(MachineKind.WIND_TURBINE);
		helper.setBlock(base, block.defaultBlockState());
		block.setPlacedBy(helper.getLevel(), helper.absolutePos(base), helper.getBlockState(base), null, ItemStack.EMPTY);
		helper.assertBlockPresent(block, base.above(8));
		helper.destroyBlock(base.above(4));
		helper.succeedWhen(() -> {
			for (int y = 0; y < 9; y++) {
				helper.assertBlockNotPresent(block, base.above(y));
			}
		});
	}

	private static int count(ChestBlockEntity chest, Item item) {
		int total = 0;
		for (int slot = 0; slot < chest.getContainerSize(); slot++) {
			ItemStack stack = chest.getItem(slot);
			if (stack.is(item)) {
				total += stack.getCount();
			}
		}
		return total;
	}

	private static ChestBlockEntity chest(GameTestHelper helper, BlockPos pos) {
		helper.setBlock(pos, Blocks.CHEST);
		return helper.getBlockEntity(pos, ChestBlockEntity.class);
	}

	private static void extractor(GameTestHelper helper, BlockPos pos, Direction intake) {
		helper.setBlock(pos, JugcraftLogistics.PNEUMATIC_EXTRACTOR.defaultBlockState().setValue(PneumaticExtractorBlock.FACING, intake));
	}

	/** Chest -> extractor -> brass item pipes -> chest. */
	@GameTest(maxTicks = 200)
	public void extractorMovesItemsThroughPipes(GameTestHelper helper) {
		ChestBlockEntity source = chest(helper, new BlockPos(1, 1, 3));
		source.setItem(0, new ItemStack(Items.IRON_INGOT, 5));
		extractor(helper, new BlockPos(2, 1, 3), Direction.WEST);
		for (int x = 3; x <= 4; x++) {
			helper.setBlock(new BlockPos(x, 1, 3), JugcraftLogistics.BRASS_ITEM_PIPE);
		}
		ChestBlockEntity target = chest(helper, new BlockPos(5, 1, 3));
		helper.succeedWhen(() -> helper.assertTrue(count(target, Items.IRON_INGOT) == 5,
				"Target chest has " + count(target, Items.IRON_INGOT) + " iron"));
	}

	/** A sorter set to iron takes the iron; everything else goes to the plain chest on the same pipes. */
	@GameTest(maxTicks = 300)
	public void sorterRoutesMatchingItems(GameTestHelper helper) {
		ChestBlockEntity source = chest(helper, new BlockPos(1, 1, 1));
		source.setItem(0, new ItemStack(Items.IRON_INGOT, 4));
		source.setItem(1, new ItemStack(Items.GOLD_INGOT, 4));
		extractor(helper, new BlockPos(2, 1, 1), Direction.WEST);
		for (int x = 3; x <= 5; x++) {
			helper.setBlock(new BlockPos(x, 1, 1), JugcraftLogistics.BRASS_ITEM_PIPE);
		}
		BlockPos sorter = new BlockPos(4, 1, 2);
		helper.setBlock(sorter, JugcraftLogistics.ITEM_SORTER.defaultBlockState().setValue(PneumaticExtractorBlock.FACING, Direction.SOUTH));
		helper.getBlockEntity(sorter, ItemSorterBlockEntity.class).setItem(0, new ItemStack(Items.IRON_INGOT));
		ChestBlockEntity sorted = chest(helper, new BlockPos(4, 1, 3));
		ChestBlockEntity overflow = chest(helper, new BlockPos(6, 1, 1));
		helper.succeedWhen(() -> {
			helper.assertTrue(count(sorted, Items.IRON_INGOT) == 4 && count(sorted, Items.GOLD_INGOT) == 0,
					"Sorted chest: " + count(sorted, Items.IRON_INGOT) + " iron, " + count(sorted, Items.GOLD_INGOT) + " gold");
			helper.assertTrue(count(overflow, Items.GOLD_INGOT) == 4 && count(overflow, Items.IRON_INGOT) == 0,
					"Overflow chest: " + count(overflow, Items.GOLD_INGOT) + " gold, " + count(overflow, Items.IRON_INGOT) + " iron");
		});
	}

	/** A crusher set to eject pushes its results into the chest below (its default output face). */
	@GameTest(maxTicks = 400)
	public void machineEjectsIntoChest(GameTestHelper helper) {
		ChestBlockEntity below = chest(helper, new BlockPos(2, 1, 2));
		BlockPos pos = new BlockPos(2, 2, 2);
		helper.setBlock(pos, machine(MachineKind.CRUSHER));
		charge(helper, pos, Direction.UP);
		MachineBlockEntity crusher = helper.getBlockEntity(pos, MachineBlockEntity.class);
		helper.assertTrue(crusher.clickSideButton(SideConfig.EJECT_BUTTON), "Eject button was refused");
		crusher.setItem(0, new ItemStack(item("tin_ore")));
		helper.succeedWhen(() -> helper.assertTrue(count(below, item("raw_tin")) == 2,
				"Chest below has " + count(below, item("raw_tin")) + " raw tin"));
	}

	// ------------------------------------------------------------------ ore processing

	/** A powered single-input machine processes one item; returns the machine for checking. */
	private static MachineBlockEntity processing(GameTestHelper helper, BlockPos pos, MachineKind kind, ItemStack input) {
		helper.setBlock(pos, machine(kind));
		charge(helper, pos, Direction.UP);
		MachineBlockEntity machine = helper.getBlockEntity(pos, MachineBlockEntity.class);
		machine.setItem(0, input);
		return machine;
	}

	/** Byproducts load from data: pulverizing tin ore sometimes yields tungsten dust. */
	@GameTest
	public void pulverizerByproductsLoad(GameTestHelper helper) {
		MachineRecipe recipe = MachineRecipes.find(helper.getLevel(), MachineKind.PULVERIZER, new ItemStack(item("tin_ore")))
				.orElseThrow(() -> helper.assertionException("No pulverizing recipe for tin ore"));
		helper.assertTrue(recipe.byproducts().size() == 1, "Expected one byproduct, got " + recipe.byproducts());
		MachineRecipe.Byproduct byproduct = recipe.byproducts().getFirst();
		helper.assertTrue(byproduct.result().create().is(item("tungsten_dust")) && byproduct.chance() > 0.0F,
				"Tin ore byproduct is " + byproduct);
		helper.succeed();
	}

	/** The pulverizer grinds one ore into two dusts. */
	@GameTest(maxTicks = 400)
	public void pulverizerGrindsOre(GameTestHelper helper) {
		MachineBlockEntity pulverizer = processing(helper, new BlockPos(2, 1, 2), MachineKind.PULVERIZER, new ItemStack(item("tin_ore")));
		helper.succeedWhen(() -> {
			ItemStack output = pulverizer.getItem(MachineKind.PULVERIZER.outputSlot());
			helper.assertTrue(output.is(item("tin_dust")) && output.getCount() == 2, "Pulverizer output is " + output);
		});
	}

	/** The ore washer, fed by a water source below, turns one ore into three washed ores. */
	@GameTest(maxTicks = 500)
	public void oreWasherTriplesOre(GameTestHelper helper) {
		helper.setBlock(new BlockPos(2, 1, 2), Blocks.WATER);
		MachineBlockEntity washer = processing(helper, new BlockPos(2, 2, 2), MachineKind.ORE_WASHER, new ItemStack(item("tin_ore")));
		helper.succeedWhen(() -> {
			ItemStack output = washer.getItem(MachineKind.ORE_WASHER.outputSlot());
			helper.assertTrue(output.is(item("washed_tin_ore")) && output.getCount() == 3, "Ore washer output is " + output);
		});
	}

	/** Without water the ore washer waits instead of washing. */
	@GameTest(maxTicks = 300)
	public void oreWasherNeedsWater(GameTestHelper helper) {
		MachineBlockEntity washer = processing(helper, new BlockPos(2, 1, 2), MachineKind.ORE_WASHER, new ItemStack(item("tin_ore")));
		helper.runAtTickTime(260, () -> {
			helper.assertTrue(washer.getItem(MachineKind.ORE_WASHER.outputSlot()).isEmpty(), "A dry ore washer made something");
			helper.succeed();
		});
	}

	/**
	 * Batch 33: the hydroponic bay grows wheat seeds into wheat on nutrient solution, gives the seed back and uses 100 mB
	 * a harvest; with no solution it grows nothing.
	 */
	@GameTest(maxTicks = 900)
	public void hydroponicBayGrowsOnNutrients(GameTestHelper helper) {
		MachineBlockEntity bay = processing(helper, new BlockPos(2, 1, 2), MachineKind.HYDROPONIC_BAY, new ItemStack(Items.WHEAT_SEEDS));
		MachineBlockEntity dry = processing(helper, new BlockPos(5, 1, 2), MachineKind.HYDROPONIC_BAY, new ItemStack(Items.WHEAT_SEEDS));
		Storage<FluidVariant> inlet = FluidStorage.SIDED.find(helper.getLevel(), helper.absolutePos(new BlockPos(2, 1, 2)), Direction.UP);
		try (Transaction transaction = Transaction.openOuter()) {
			long accepted = inlet.insert(FluidVariant.of(io.github.jimbozoomer.jugcraft.chemistry.PetroFluids.NUTRIENT_SOLUTION.source()),
					FluidConstants.BUCKET, transaction);
			helper.assertTrue(accepted == FluidConstants.BUCKET, "The bay took " + accepted / 81 + " mB of nutrient solution");
			long water = inlet.insert(FluidVariant.of(net.minecraft.world.level.material.Fluids.WATER), FluidConstants.BUCKET, transaction);
			helper.assertTrue(water == 0, "The bay took plain water");
			transaction.commit();
		}
		helper.succeedWhen(() -> {
			ItemStack wheat = bay.getItem(MachineKind.HYDROPONIC_BAY.outputSlot());
			helper.assertTrue(wheat.is(Items.WHEAT) && wheat.getCount() >= 2, "Hydroponic bay output is " + wheat);
			boolean seedBack = false;
			for (int slot = 0; slot < bay.getContainerSize(); slot++) {
				seedBack |= slot != 0 && bay.getItem(slot).is(Items.WHEAT_SEEDS);
			}
			helper.assertTrue(seedBack, "The seed did not come back");
			helper.assertTrue(dry.getItem(MachineKind.HYDROPONIC_BAY.outputSlot()).isEmpty(), "A dry bay grew something");
		});
	}

	/** Fills a machine's tank with {@code mb} of sulfuric acid through its top. */
	private static void acid(GameTestHelper helper, BlockPos pos, long mb) {
		Storage<FluidVariant> inlet = FluidStorage.SIDED.find(helper.getLevel(), helper.absolutePos(pos), Direction.UP);
		try (Transaction transaction = Transaction.openOuter()) {
			long accepted = inlet.insert(FluidVariant.of(io.github.jimbozoomer.jugcraft.chemistry.PetroFluids.SULFURIC_ACID.source()),
					mb * 81, transaction);
			helper.assertTrue(accepted == mb * 81, "The bath took " + accepted / 81 + " mB of acid");
			transaction.commit();
		}
	}

	/**
	 * The electroplating bath: nickel makes a worn iron sword half as durable again and repairs it; silver gives it Smite
	 * III; a nickel-plated sword refuses gold; without acid nothing happens. Gold-plated armor counts as gold for piglins.
	 */
	@GameTest(maxTicks = 600)
	public void electroplatingPlatesAndRepairs(GameTestHelper helper) {
		ItemStack worn = new ItemStack(Items.IRON_SWORD);
		worn.setDamageValue(200);
		int ironMax = worn.getMaxDamage();
		MachineBlockEntity nickel = processing(helper, new BlockPos(1, 1, 1), MachineKind.ELECTROPLATING_BATH, worn.copy());
		nickel.setItem(1, new ItemStack(item("nickel_ingot")));
		acid(helper, new BlockPos(1, 1, 1), 1000);
		MachineBlockEntity silver = processing(helper, new BlockPos(3, 1, 1), MachineKind.ELECTROPLATING_BATH, worn.copy());
		silver.setItem(1, new ItemStack(item("silver_ingot")));
		acid(helper, new BlockPos(3, 1, 1), 1000);
		ItemStack plated = worn.copy();
		plated.set(io.github.jimbozoomer.jugcraft.machine.Electroplating.PLATING, "nickel");
		MachineBlockEntity refused = processing(helper, new BlockPos(5, 1, 1), MachineKind.ELECTROPLATING_BATH, plated);
		refused.setItem(1, new ItemStack(Items.GOLD_INGOT));
		acid(helper, new BlockPos(5, 1, 1), 1000);
		MachineBlockEntity dry = processing(helper, new BlockPos(7, 1, 1), MachineKind.ELECTROPLATING_BATH, worn.copy());
		dry.setItem(1, new ItemStack(item("nickel_ingot")));
		helper.assertFalse(nickel.canPlaceItem(0, new ItemStack(Items.DIRT)), "The bath took dirt to plate");
		helper.assertFalse(nickel.canPlaceItem(1, new ItemStack(Items.IRON_INGOT)), "The bath took iron to plate with");

		var stand = helper.spawn(net.minecraft.world.entity.EntityTypes.ARMOR_STAND, new BlockPos(1, 2, 4));
		ItemStack chest = new ItemStack(Items.IRON_CHESTPLATE);
		stand.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST, chest);
		helper.assertFalse(net.minecraft.world.entity.monster.piglin.PiglinAi.isWearingSafeArmor(stand),
				"Plain iron armor counts as gold");
		chest.set(io.github.jimbozoomer.jugcraft.machine.Electroplating.PLATING, "gold");
		stand.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST, chest);
		helper.assertTrue(net.minecraft.world.entity.monster.piglin.PiglinAi.isWearingSafeArmor(stand),
				"Gold-plated armor does not count as gold");

		var smite = helper.getLevel().registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT)
				.getOrThrow(net.minecraft.world.item.enchantment.Enchantments.SMITE);
		helper.succeedWhen(() -> {
			int out = MachineKind.ELECTROPLATING_BATH.outputSlot();
			ItemStack n = nickel.getItem(out);
			helper.assertTrue(n.is(Items.IRON_SWORD) && n.getDamageValue() == 0 && n.getMaxDamage() == ironMax * 3 / 2
					&& "nickel".equals(n.get(io.github.jimbozoomer.jugcraft.machine.Electroplating.PLATING)),
					"Nickel bath output is " + n + " (max damage " + n.getMaxDamage() + ")");
			helper.assertTrue(nickel.getItem(0).isEmpty() && nickel.getItem(1).isEmpty(), "The nickel bath kept its inputs");
			ItemStack s = silver.getItem(out);
			helper.assertTrue(s.is(Items.IRON_SWORD) && s.getDamageValue() == 0
					&& net.minecraft.world.item.enchantment.EnchantmentHelper.getItemEnchantmentLevel(smite, s) == 3,
					"Silver bath output is " + s);
			helper.assertTrue(refused.getItem(out).isEmpty() && refused.getItem(0).is(Items.IRON_SWORD)
					&& refused.getItem(1).is(Items.GOLD_INGOT), "A nickel-plated sword took gold");
			helper.assertTrue(dry.getItem(out).isEmpty() && dry.getItem(0).is(Items.IRON_SWORD), "A bath without acid plated");
		});
	}

	/** Dust smelts back into an ingot in the electric furnace. */
	@GameTest(maxTicks = 300)
	public void dustSmeltsIntoIngot(GameTestHelper helper) {
		MachineBlockEntity furnace = processing(helper, new BlockPos(2, 1, 2), MachineKind.ELECTRIC_FURNACE, new ItemStack(item("tin_dust")));
		helper.succeedWhen(() -> {
			ItemStack output = furnace.getItem(MachineKind.ELECTRIC_FURNACE.outputSlot());
			helper.assertTrue(output.is(item("tin_ingot")), "Electric furnace output is " + output);
		});
	}

	/** The sawmill cuts a log into six planks. */
	@GameTest(maxTicks = 300)
	public void sawmillCutsLogs(GameTestHelper helper) {
		MachineBlockEntity sawmill = processing(helper, new BlockPos(2, 1, 2), MachineKind.SAWMILL, new ItemStack(Items.OAK_LOG));
		helper.succeedWhen(() -> {
			ItemStack output = sawmill.getItem(MachineKind.SAWMILL.outputSlot());
			helper.assertTrue(output.is(Items.OAK_PLANKS) && output.getCount() == 6, "Sawmill output is " + output);
		});
	}

	/** The sieve sifts gravel into flint. */
	@GameTest(maxTicks = 300)
	public void sieveSiftsGravel(GameTestHelper helper) {
		MachineBlockEntity sieve = processing(helper, new BlockPos(2, 1, 2), MachineKind.SIEVE, new ItemStack(Items.GRAVEL));
		helper.succeedWhen(() -> {
			ItemStack output = sieve.getItem(MachineKind.SIEVE.outputSlot());
			helper.assertTrue(output.is(Items.FLINT), "Sieve output is " + output);
		});
	}

	// ------------------------------------------------------------------ steel tier

	/** Places a multi-block machine facing north and builds all its parts, as a player placing it would. */
	private static MachineBlockEntity large(GameTestHelper helper, BlockPos master, MachineKind kind) {
		LargeMachineBlock block = (LargeMachineBlock) JugcraftMachines.MACHINES.get(kind);
		helper.setBlock(master, block.defaultBlockState().setValue(MachineBlock.FACING, Direction.NORTH));
		block.setPlacedBy(helper.getLevel(), helper.absolutePos(master), helper.getBlockState(master), null, ItemStack.EMPTY);
		return helper.getBlockEntity(master, MachineBlockEntity.class);
	}

	/** The coke oven bakes coal into coke with no power at all. */
	@GameTest(maxTicks = 800)
	public void cokeOvenBakesCoke(GameTestHelper helper) {
		MachineBlockEntity oven = large(helper, new BlockPos(2, 1, 2), MachineKind.COKE_OVEN);
		oven.setItem(0, new ItemStack(Items.COAL));
		helper.succeedWhen(() -> {
			ItemStack output = oven.getItem(MachineKind.COKE_OVEN.outputSlot());
			helper.assertTrue(output.is(item("coke")), "Coke oven output is " + output);
		});
	}

	/** The steel foundry refines one iron ingot with one coke into one steel ingot, unpowered. */
	@GameTest(maxTicks = 600)
	public void steelFoundryMakesSteel(GameTestHelper helper) {
		MachineBlockEntity foundry = large(helper, new BlockPos(2, 1, 2), MachineKind.STEEL_FOUNDRY);
		foundry.setItem(0, new ItemStack(Items.IRON_INGOT));
		foundry.setItem(1, new ItemStack(item("coke")));
		helper.succeedWhen(() -> {
			ItemStack output = foundry.getItem(MachineKind.STEEL_FOUNDRY.outputSlot());
			helper.assertTrue(output.is(item("steel_ingot")), "Steel foundry output is " + output);
		});
	}

	/** Cables draw a connection only where power really goes in: never to unpowered machines, only to a socket. */
	@GameTest(maxTicks = 20)
	public void cablesConnectOnlyWherePowerGoesIn(GameTestHelper helper) {
		// Cables first, so placing the machines updates their connections.
		BlockPos besideOven = new BlockPos(3, 1, 1);
		BlockPos besideFront = new BlockPos(7, 1, 2);
		BlockPos besideSocket = new BlockPos(4, 1, 3);
		for (BlockPos cable : new BlockPos[] {besideOven, besideFront, besideSocket}) {
			helper.setBlock(cable, JugcraftMachines.COPPER_CABLE);
		}
		// The 2x2 coke oven fills x 1..2, z 1..2 from its master at (2,1,1).
		large(helper, new BlockPos(2, 1, 1), MachineKind.COKE_OVEN);
		// Alloy smelter at (7,1,3), 3 wide to the west: its front faces north towards (7,1,2); its socket block is
		// (5,1,3), with the socket facing west towards the cable at (4,1,3).
		large(helper, new BlockPos(7, 1, 3), MachineKind.ALLOY_SMELTER);
		helper.assertTrue(EnergyStorage.SIDED.find(helper.getLevel(), helper.absolutePos(new BlockPos(2, 1, 1)), Direction.WEST) == null,
				"An unpowered machine must not expose energy");
		helper.assertTrue(!helper.getBlockState(besideOven).getValue(PipeBlock.PROPERTY_BY_DIRECTION.get(Direction.WEST)),
				"A cable must not connect to the coke oven");
		helper.assertTrue(!helper.getBlockState(besideFront).getValue(PipeBlock.PROPERTY_BY_DIRECTION.get(Direction.SOUTH)),
				"A cable must not connect to the alloy smelter's front");
		helper.assertTrue(helper.getBlockState(besideSocket).getValue(PipeBlock.PROPERTY_BY_DIRECTION.get(Direction.EAST)),
				"A cable must connect to the alloy smelter's power socket");
		helper.succeed();
	}

	// ------------------------------------------------------------------ machine control

	/** Four speed upgrades make the crusher three times as fast (160 ticks -> 54). */
	@GameTest(maxTicks = 120)
	public void speedUpgradesShortenProcessing(GameTestHelper helper) {
		MachineBlockEntity crusher = processing(helper, new BlockPos(2, 1, 2), MachineKind.CRUSHER, new ItemStack(item("tin_ore")));
		crusher.setItem(MachineKind.CRUSHER.slots, new ItemStack(MachineUpgrades.SPEED, 4));
		helper.runAtTickTime(80, () -> {
			ItemStack output = crusher.getItem(MachineKind.CRUSHER.outputSlot());
			helper.assertTrue(output.is(item("raw_tin")), "An upgraded crusher should be done by tick 80, output is " + output);
			helper.succeed();
		});
	}

	/** Four efficiency upgrades cut the energy one operation uses to about 41%. */
	@GameTest(maxTicks = 400)
	public void efficiencyUpgradesSaveEnergy(GameTestHelper helper) {
		MachineBlockEntity crusher = processing(helper, new BlockPos(2, 1, 2), MachineKind.CRUSHER, new ItemStack(item("tin_ore")));
		crusher.setItem(MachineKind.CRUSHER.slots + 1, new ItemStack(MachineUpgrades.EFFICIENCY, 4));
		EnergyStorage energy = EnergyStorage.SIDED.find(helper.getLevel(), helper.absolutePos(new BlockPos(2, 1, 2)), Direction.UP);
		long start = energy.getAmount();
		helper.succeedWhen(() -> {
			helper.assertTrue(!crusher.getItem(MachineKind.CRUSHER.outputSlot()).isEmpty(), "Not done yet");
			long used = start - energy.getAmount();
			// Unupgraded: 16 JE/t x 160 ticks = 2,560 JE. Upgraded: 7 JE/t x 160 = 1,120 JE.
			helper.assertTrue(used <= 1_200, "Used " + used + " JE for one operation");
		});
	}

	/** In "high" redstone mode a machine waits until it receives a signal. */
	@GameTest(maxTicks = 500)
	public void redstoneHighWaitsForSignal(GameTestHelper helper) {
		BlockPos pos = new BlockPos(2, 1, 2);
		MachineBlockEntity crusher = processing(helper, pos, MachineKind.CRUSHER, new ItemStack(item("tin_ore")));
		crusher.clickSideButton(SideConfig.REDSTONE_BUTTON);
		helper.assertTrue(crusher.sides().redstone() == SideConfig.Redstone.HIGH, "One click should select HIGH");
		helper.runAtTickTime(200, () -> {
			helper.assertTrue(crusher.getItem(MachineKind.CRUSHER.outputSlot()).isEmpty(), "Ran without a redstone signal");
			helper.setBlock(pos.east(), Blocks.REDSTONE_BLOCK);
		});
		helper.succeedWhen(() -> {
			ItemStack output = crusher.getItem(MachineKind.CRUSHER.outputSlot());
			helper.assertTrue(output.is(item("raw_tin")), "Crusher output is " + output);
		});
	}

	/** Comparators read a battery's charge; upgrade slots are never offered to hoppers or pipes. */
	@GameTest
	public void comparatorAndUpgradeSlots(GameTestHelper helper) {
		BlockPos batteryPos = new BlockPos(1, 1, 1);
		helper.setBlock(batteryPos, machine(MachineKind.BATTERY_BOX));
		MachineBlockEntity battery = helper.getBlockEntity(batteryPos, MachineBlockEntity.class);
		helper.assertTrue(battery.comparatorSignal() == 0, "Empty battery signal is " + battery.comparatorSignal());
		charge(helper, batteryPos, null); // The battery box's sided storages are wrappers; the unsided one is the real battery.
		helper.assertTrue(battery.comparatorSignal() == 15, "Full battery signal is " + battery.comparatorSignal());

		BlockPos crusherPos = new BlockPos(3, 1, 3);
		helper.setBlock(crusherPos, machine(MachineKind.CRUSHER));
		MachineBlockEntity crusher = helper.getBlockEntity(crusherPos, MachineBlockEntity.class);
		for (Direction side : Direction.values()) {
			for (int slot : crusher.getSlotsForFace(side)) {
				helper.assertTrue(slot < MachineKind.CRUSHER.slots, "Upgrade slot " + slot + " exposed on " + side);
			}
		}
		helper.assertTrue(!crusher.canPlaceItem(MachineKind.CRUSHER.slots, new ItemStack(Items.COBBLESTONE)),
				"Upgrade slots must only take upgrades");
		helper.succeed();
	}

	// ------------------------------------------------------------------ transmitter tiers

	/** Pushes once from a large source through the given cables into an arc furnace; returns the JE moved. */
	private static long pushThrough(GameTestHelper helper, int z, net.minecraft.world.level.block.Block... cables) {
		for (int i = 0; i < cables.length; i++) {
			helper.setBlock(new BlockPos(2 + i, 1, z), cables[i]);
		}
		BlockPos furnace = new BlockPos(2 + cables.length, 1, z);
		helper.setBlock(furnace, machine(MachineKind.ARC_FURNACE));
		SimpleEnergyStorage source = new SimpleEnergyStorage(100_000, 0, 100_000, () -> {
		});
		source.setAmount(100_000);
		return EnergyNetworks.pushToNeighbors(helper.getLevel(), helper.absolutePos(new BlockPos(1, 1, z)), source, 100_000,
				java.util.List.of(Direction.EAST));
	}

	/** Silver cable carries 1,024 JE/t, copper 256; a mixed network runs at its slowest cable. */
	@GameTest
	public void cableTiersSetTheRate(GameTestHelper helper) {
		long silver = pushThrough(helper, 1, JugcraftMachines.SILVER_CABLE, JugcraftMachines.SILVER_CABLE);
		long copper = pushThrough(helper, 3, JugcraftMachines.COPPER_CABLE, JugcraftMachines.COPPER_CABLE);
		long mixed = pushThrough(helper, 5, JugcraftMachines.SILVER_CABLE, JugcraftMachines.COPPER_CABLE);
		helper.assertTrue(silver > 256 && silver <= 1_024, "Silver network moved " + silver);
		helper.assertTrue(copper == 256, "Copper network moved " + copper);
		helper.assertTrue(mixed == 256, "Mixed network moved " + mixed);
		helper.succeed();
	}

	/** The high-pressure extractor moves 32 items every 4 ticks; by tick 10 the brass one could move at most 16. */
	@GameTest(maxTicks = 40)
	public void highPressureExtractorIsFaster(GameTestHelper helper) {
		ChestBlockEntity source = chest(helper, new BlockPos(1, 1, 2));
		source.setItem(0, new ItemStack(Items.COBBLESTONE, 64));
		helper.setBlock(new BlockPos(2, 1, 2), JugcraftLogistics.HIGH_PRESSURE_EXTRACTOR.defaultBlockState()
				.setValue(PneumaticExtractorBlock.FACING, Direction.WEST));
		ChestBlockEntity target = chest(helper, new BlockPos(3, 1, 2));
		helper.runAtTickTime(10, () -> {
			int moved = count(target, Items.COBBLESTONE);
			helper.assertTrue(moved >= 32, "High-pressure extractor moved " + moved + " by tick 10");
			helper.succeed();
		});
	}

	// ------------------------------------------------------------------ storage

	/** The 2x2 capacitor bank holds 4,000,000 JE, takes power at its sides and gives it out of its front. */
	@GameTest
	public void capacitorBankOutputsFromItsFront(GameTestHelper helper) {
		BlockPos master = new BlockPos(4, 1, 3);
		large(helper, master, MachineKind.CAPACITOR_BANK);
		EnergyStorage front = EnergyStorage.SIDED.find(helper.getLevel(), helper.absolutePos(master.above()), Direction.NORTH);
		EnergyStorage side = EnergyStorage.SIDED.find(helper.getLevel(), helper.absolutePos(master.west()), Direction.WEST);
		helper.assertTrue(front != null && front.supportsExtraction() && !front.supportsInsertion(), "Front must only give power");
		helper.assertTrue(side != null && side.supportsInsertion() && !side.supportsExtraction(), "Sides must only take power");
		helper.assertTrue(front.getCapacity() == 4_000_000, "Capacity is " + front.getCapacity());
		helper.succeed();
	}

	/** The 3x2 lithium battery bank holds 32,000,000 JE and gives up to 16,384 JE/t out of all six front sockets. */
	@GameTest
	public void lithiumBatteryBankOutputsFromItsFront(GameTestHelper helper) {
		BlockPos master = new BlockPos(5, 1, 3);
		large(helper, master, MachineKind.LITHIUM_BATTERY_BANK);
		EnergyStorage front = EnergyStorage.SIDED.find(helper.getLevel(), helper.absolutePos(master.west().above()), Direction.NORTH);
		EnergyStorage side = EnergyStorage.SIDED.find(helper.getLevel(), helper.absolutePos(master.west(2)), Direction.WEST);
		EnergyStorage back = EnergyStorage.SIDED.find(helper.getLevel(), helper.absolutePos(master.above()), Direction.SOUTH);
		helper.assertTrue(front != null && front.supportsExtraction() && !front.supportsInsertion(), "Front must only give power");
		helper.assertTrue(side != null && side.supportsInsertion() && !side.supportsExtraction(), "Sides must only take power");
		helper.assertTrue(back != null && back.supportsInsertion(), "The back must take power");
		helper.assertTrue(front.getCapacity() == 32_000_000, "Capacity is " + front.getCapacity());
		helper.succeed();
	}

	/** The steel tank holds exactly 128 buckets of one fluid. */
	@GameTest
	public void steelTankHolds128Buckets(GameTestHelper helper) {
		BlockPos master = new BlockPos(4, 1, 2);
		large(helper, master, MachineKind.STEEL_TANK);
		Storage<FluidVariant> tank = FluidStorage.SIDED.find(helper.getLevel(), helper.absolutePos(master.south()), Direction.UP);
		helper.assertTrue(tank != null, "No fluid storage on the tank's back block");
		try (Transaction transaction = Transaction.openOuter()) {
			long inserted = tank.insert(FluidVariant.of(Fluids.WATER), 200 * FluidConstants.BUCKET, transaction);
			helper.assertTrue(inserted == 128 * FluidConstants.BUCKET, "Inserted " + inserted / FluidConstants.BUCKET + " buckets");
			long lava = tank.insert(FluidVariant.of(Fluids.LAVA), FluidConstants.BUCKET, transaction);
			helper.assertTrue(lava == 0, "A water tank accepted lava");
			transaction.commit();
		}
		helper.succeed();
	}

	/** A crate holds 32 stacks of one item type, refuses others, and shows its fill on a comparator. */
	@GameTest
	public void crateHoldsOneItemType(GameTestHelper helper) {
		BlockPos pos = new BlockPos(2, 1, 2);
		helper.setBlock(pos, JugcraftStorage.ITEM_CRATE);
		Storage<ItemVariant> crate = ItemStorage.SIDED.find(helper.getLevel(), helper.absolutePos(pos), Direction.UP);
		try (Transaction transaction = Transaction.openOuter()) {
			long cobble = crate.insert(ItemVariant.of(Items.COBBLESTONE), 5_000, transaction);
			long dirt = crate.insert(ItemVariant.of(Items.DIRT), 10, transaction);
			helper.assertTrue(cobble == 32 * 64, "Crate took " + cobble + " cobblestone");
			helper.assertTrue(dirt == 0, "Crate took a second item type");
			transaction.commit();
		}
		helper.assertTrue(StorageUtil.getRedstoneSignal(crate) == 15, "A full crate should signal 15");
		helper.succeed();
	}

	/**
	 * Ores drop their raw material, not themselves, and storage blocks drop themselves. Before the
	 * loot tables used the 26.x keys, Minecraft ignored their conditions and every ore dropped itself.
	 */
	@GameTest
	public void oresDropRawMaterial(GameTestHelper helper) {
		assertDrops(helper, new BlockPos(1, 1, 1), "tin_ore", "raw_tin", 1, 1);
		assertDrops(helper, new BlockPos(3, 1, 1), "deepslate_zinc_ore", "raw_zinc", 1, 1);
		assertDrops(helper, new BlockPos(5, 1, 1), "salt_ore", "salt", 2, 4);
		assertDrops(helper, new BlockPos(1, 1, 3), "oil_sand", "bitumen", 1, 2);
		assertDrops(helper, new BlockPos(3, 1, 3), "tin_block", "tin_block", 1, 1);
		helper.succeed();
	}

	/**
	 * The Charging Station drops once, from its lower half. Its top half's "lower half only" condition used
	 * the pre-26.x loot keys, which Minecraft ignored, so breaking the top half dropped two stations.
	 */
	@GameTest
	public void chargingStationDropsOnce(GameTestHelper helper) {
		BlockPos lower = new BlockPos(2, 1, 2);
		BlockState state = JugcraftTools.CHARGING_STATION.defaultBlockState();
		helper.setBlock(lower, state);
		helper.setBlock(lower.above(), state.setValue(ChargingStationBlock.HALF, DoubleBlockHalf.UPPER));
		List<ItemStack> top = Block.getDrops(helper.getBlockState(lower.above()), helper.getLevel(), helper.absolutePos(lower.above()), null);
		List<ItemStack> bottom = Block.getDrops(helper.getBlockState(lower), helper.getLevel(), helper.absolutePos(lower), null);
		helper.assertTrue(top.isEmpty(), "The top half should drop nothing, dropped " + top);
		helper.assertTrue(bottom.size() == 1 && bottom.get(0).is(JugcraftTools.CHARGING_STATION.asItem()) && bottom.get(0).getCount() == 1,
				"The lower half should drop one station, dropped " + bottom);
		helper.succeed();
	}

	/** Breaking {@code block} with no tool drops only {@code drop}, between {@code min} and {@code max} of it. */
	private static void assertDrops(GameTestHelper helper, BlockPos pos, String block, String drop, int min, int max) {
		helper.setBlock(pos, BuiltInRegistries.BLOCK.getValue(Jugcraft.id(block)));
		List<ItemStack> drops = Block.getDrops(helper.getBlockState(pos), helper.getLevel(), helper.absolutePos(pos), null);
		helper.assertTrue(drops.size() == 1 && drops.get(0).is(item(drop)) && drops.get(0).getCount() >= min
				&& drops.get(0).getCount() <= max, block + " should drop " + min + "-" + max + " " + drop + ", dropped " + drops);
	}

	// ------------------------------------------------------------------ mining & prospecting

	/** The prospector's survey reports ore placed nearby, with a signal of 1-5 and a depth band, and no positions. */
	@GameTest
	public void surveyFindsNearbyOre(GameTestHelper helper) {
		for (int x = 1; x <= 4; x++) {
			for (int z = 1; z <= 4; z++) {
				helper.setBlock(new BlockPos(x, 1, z), Blocks.COPPER_ORE);
			}
		}
		List<OreSurvey.Reading> readings = OreSurvey.survey(helper.getLevel(), helper.absolutePos(new BlockPos(2, 2, 2)),
				helper.getLevel().getRandom());
		OreSurvey.Reading copper = readings.stream()
				.filter(reading -> reading.icon().equals(Identifier.parse("minecraft:copper_ore"))).findFirst().orElse(null);
		helper.assertTrue(copper != null, "No copper reading in " + readings);
		helper.assertTrue(copper.signal() >= 1 && copper.signal() <= 5, "Signal out of range: " + copper.signal());
		helper.assertTrue(copper.depth() >= 0 && copper.depth() <= 2, "Depth band out of range: " + copper.depth());
		helper.succeed();
	}

	/**
	 * The ore drill mines ore blocks in the layer below it, puts them in its result slots, fills stone ore
	 * holes with stone and deepslate ore holes with deepslate, and leaves other blocks alone.
	 */
	@GameTest(maxTicks = 400)
	public void oreDrillMinesOreBelow(GameTestHelper helper) {
		BlockPos master = new BlockPos(4, 2, 4);
		BlockPos iron = new BlockPos(5, 1, 3);
		BlockPos deepIron = new BlockPos(2, 1, 6);
		BlockPos dirt = new BlockPos(4, 1, 4);
		helper.setBlock(iron, Blocks.IRON_ORE);
		helper.setBlock(deepIron, Blocks.DEEPSLATE_IRON_ORE);
		helper.setBlock(dirt, Blocks.DIRT);
		MachineBlockEntity drill = large(helper, master, MachineKind.ORE_DRILL);
		charge(helper, master, Direction.EAST);
		helper.succeedWhen(() -> {
			helper.assertBlockPresent(Blocks.STONE, iron);
			helper.assertBlockPresent(Blocks.DEEPSLATE, deepIron);
			helper.assertBlockPresent(Blocks.DIRT, dirt);
			int ore = 0;
			int deep = 0;
			for (int slot = 0; slot < MachineKind.ORE_DRILL.slots; slot++) {
				ItemStack stack = drill.getItem(slot);
				ore += stack.is(Items.IRON_ORE) ? stack.getCount() : 0;
				deep += stack.is(Items.DEEPSLATE_IRON_ORE) ? stack.getCount() : 0;
			}
			helper.assertTrue(ore == 1 && deep == 1, "Drill holds " + ore + " iron ore and " + deep + " deepslate iron ore");
		});
	}

	/**
	 * A deposit drill standing on surface deposits takes one of each kind per cycle from those under it and one block
	 * round it, into a chest beside it, and leaves stone where each ran out; a deposit outside its reach stays full.
	 * Two cycles empty an iron and a coal deposit with 2 left each; mining them one after the other would take four,
	 * longer than the test allows. Picks get nothing from a deposit block.
	 */
	@GameTest(maxTicks = 1000)
	public void depositDrillEmptiesDepositsIntoAChest(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos master = new BlockPos(4, 2, 4);
		// The drill covers x 2..4, z 4..6 (it faces north); it reaches x 1..5, z 3..7, at y 1 and below.
		BlockPos iron = new BlockPos(3, 1, 5);
		BlockPos coal = new BlockPos(5, 1, 7);
		BlockPos outside = new BlockPos(0, 1, 5);
		helper.setBlock(iron, JugcraftDeposits.BLOCKS.get("iron_deposit"));
		helper.setBlock(coal, JugcraftDeposits.BLOCKS.get("coal_deposit"));
		helper.setBlock(outside, JugcraftDeposits.BLOCKS.get("iron_deposit"));
		helper.assertTrue(Block.getDrops(helper.getBlockState(iron), level, helper.absolutePos(iron), null).isEmpty(),
				"A deposit block drops something when broken");
		// Run the two deposits nearly dry so the test finishes quickly: 2 iron and 2 coal left.
		helper.assertTrue(Deposits.extract(level, helper.absolutePos(iron), Deposits.CAPACITY - 2) == Deposits.CAPACITY - 2,
				"Could not draw down the iron deposit");
		Deposits.extract(level, helper.absolutePos(coal), Deposits.CAPACITY - 2);
		helper.assertTrue(Deposits.remaining(level, helper.absolutePos(coal)) == 2, "The coal deposit does not hold 2");
		helper.setBlock(new BlockPos(4, 2, 3), Blocks.CHEST);
		large(helper, master, MachineKind.DEPOSIT_DRILL);
		charge(helper, master, Direction.EAST);
		helper.succeedWhen(() -> {
			helper.assertBlockPresent(Blocks.STONE, iron);
			helper.assertBlockPresent(Blocks.STONE, coal);
			helper.assertBlockPresent(JugcraftDeposits.BLOCKS.get("iron_deposit"), outside);
			helper.assertTrue(Deposits.remaining(level, helper.absolutePos(outside)) == Deposits.CAPACITY,
					"The deposit out of reach was drawn on");
			ChestBlockEntity chest = helper.getBlockEntity(new BlockPos(4, 2, 3), ChestBlockEntity.class);
			int rawIron = count(chest, Items.RAW_IRON);
			int coalItems = count(chest, Items.COAL);
			helper.assertTrue(rawIron == 2 && coalItems == 2, "The chest holds " + rawIron + " raw iron and " + coalItems + " coal");
		});
	}

	/**
	 * The iron deposit's worldgen feature, placed on a two-layer stone floor as the surface heightmap would place it
	 * (on the first air block), turns a disk of the top layer into iron deposit and leaves the layer below as stone.
	 */
	@GameTest
	public void depositFeatureReplacesTheTopLayer(GameTestHelper helper) {
		for (int x = 0; x < 8; x++) {
			for (int z = 0; z < 8; z++) {
				helper.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
				helper.setBlock(new BlockPos(x, 2, z), Blocks.STONE);
			}
		}
		ServerLevel level = helper.getLevel();
		PlacedFeature placed = level.registryAccess().lookupOrThrow(Registries.PLACED_FEATURE)
				.getOrThrow(ResourceKey.create(Registries.PLACED_FEATURE, Jugcraft.id("iron_deposit"))).value();
		// Without its placement rules (rarity, heightmap, biome), the feature goes exactly where it is put.
		new PlacedFeature(placed.feature(), List.of()).place(level, level.getChunkSource().getGenerator(),
				RandomSource.create(42), helper.absolutePos(new BlockPos(4, 3, 4)));
		Block deposit = JugcraftDeposits.BLOCKS.get("iron_deposit");
		helper.assertBlockPresent(deposit, new BlockPos(4, 2, 4));
		helper.assertBlockPresent(Blocks.STONE, new BlockPos(4, 1, 4));
		int top = 0;
		for (int x = 0; x < 8; x++) {
			for (int z = 0; z < 8; z++) {
				top += helper.getBlockState(new BlockPos(x, 2, z)).is(deposit) ? 1 : 0;
				helper.assertBlockNotPresent(deposit, new BlockPos(x, 1, z));
			}
		}
		helper.assertTrue(top >= 9, "Only " + top + " deposit blocks were placed");
		helper.succeed();
	}

	/** Breaking a deposit forgets what was taken from it, so a deposit placed there again is full. */
	@GameTest
	public void brokenDepositIsForgotten(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos pos = new BlockPos(1, 1, 1);
		helper.setBlock(pos, JugcraftDeposits.BLOCKS.get("tin_deposit"));
		Deposits.extract(level, helper.absolutePos(pos), 400);
		helper.assertTrue(Deposits.remaining(level, helper.absolutePos(pos)) == Deposits.CAPACITY - 400, "Nothing was taken");
		helper.setBlock(pos, Blocks.AIR);
		helper.setBlock(pos, JugcraftDeposits.BLOCKS.get("tin_deposit"));
		helper.assertTrue(Deposits.remaining(level, helper.absolutePos(pos)) == Deposits.CAPACITY,
				"A new deposit block starts with " + Deposits.remaining(level, helper.absolutePos(pos)));
		helper.succeed();
	}

	// ------------------------------------------------------------------ renewables

	/**
	 * A cobblestone generator with water below it and lava above it (walled in so neither can spread) makes
	 * cobblestone; one with only water makes none.
	 */
	@GameTest(maxTicks = 200)
	public void cobblestoneGeneratorNeedsWaterAndLava(GameTestHelper helper) {
		BlockPos both = new BlockPos(2, 1, 2);
		BlockPos waterOnly = new BlockPos(6, 1, 2);
		for (BlockPos pos : List.of(both, waterOnly)) {
			helper.setBlock(pos, machine(MachineKind.COBBLESTONE_GENERATOR));
			charge(helper, pos, Direction.NORTH);
			helper.setBlock(pos.below(), Blocks.WATER);
		}
		for (Direction side : Direction.Plane.HORIZONTAL) {
			helper.setBlock(both.above().relative(side), Blocks.GLASS);
		}
		helper.setBlock(both.above(2), Blocks.GLASS);
		helper.setBlock(both.above(), Blocks.LAVA);
		MachineBlockEntity generator = helper.getBlockEntity(both, MachineBlockEntity.class);
		MachineBlockEntity idle = helper.getBlockEntity(waterOnly, MachineBlockEntity.class);
		helper.runAtTickTime(150, () -> {
			ItemStack made = generator.getItem(MachineKind.COBBLESTONE_GENERATOR.outputSlot());
			helper.assertTrue(made.is(Items.COBBLESTONE) && made.getCount() >= 3, "Generator made " + made);
			helper.assertTrue(idle.getItem(0).isEmpty(), "A generator without lava made " + idle.getItem(0));
			helper.succeed();
		});
	}

	/** The tree farm grows an oak sapling into six oak logs and gives the sapling back. */
	@GameTest(maxTicks = 600)
	public void treeFarmGrowsLogs(GameTestHelper helper) {
		MachineBlockEntity farm = processing(helper, new BlockPos(2, 1, 2), MachineKind.TREE_FARM, new ItemStack(Items.OAK_SAPLING));
		helper.succeedWhen(() -> {
			ItemStack logs = farm.getItem(MachineKind.TREE_FARM.outputSlot());
			helper.assertTrue(logs.is(Items.OAK_LOG) && logs.getCount() == 6, "Tree farm output is " + logs);
			ItemStack sapling = farm.getItem(MachineKind.TREE_FARM.outputSlot() + 1);
			helper.assertTrue(sapling.is(Items.OAK_SAPLING), "The sapling did not come back: " + sapling);
		});
	}

	/** Water falling past the wheel side of a water wheel turns it; the wheel stores power. */
	@GameTest(maxTicks = 200)
	public void waterWheelTurnsInFlowingWater(GameTestHelper helper) {
		BlockPos master = new BlockPos(3, 1, 2);
		MachineBlockEntity wheel = large(helper, master, MachineKind.WATER_WHEEL);
		// Facing north, the wheel is on the west side. A source above that column falls past both blocks.
		helper.setBlock(master.west().above(2), Blocks.WATER);
		helper.succeedWhen(() -> {
			long stored = wheel.energyFor(null).getAmount();
			helper.assertTrue(stored >= 1_000, "Water wheel stored only " + stored + " JE");
		});
	}

	// ------------------------------------------------------------------ kinetic power

	/** A steam engine facing west (its back, the output, is east) with coal and a water source below it. */
	private static BlockPos steamEngine(GameTestHelper helper, BlockPos pos) {
		helper.setBlock(pos.below(), Blocks.WATER);
		helper.setBlock(pos, JugcraftKinetics.STEAM_ENGINE.defaultBlockState().setValue(SteamEngineBlock.FACING, Direction.WEST));
		Storage<ItemVariant> fuel = ItemStorage.SIDED.find(helper.getLevel(), helper.absolutePos(pos), Direction.UP);
		helper.assertTrue(fuel != null, "The steam engine takes no fuel");
		try (Transaction transaction = Transaction.openOuter()) {
			helper.assertTrue(fuel.insert(ItemVariant.of(Items.COAL), 4, transaction) == 4, "The steam engine refused coal");
			transaction.commit();
		}
		return pos;
	}

	/** A steam engine turns a shaft line that runs a crusher with no JE at all; the shafts show as turning. */
	@GameTest(maxTicks = 400)
	public void steamEngineDrivesCrusherThroughShafts(GameTestHelper helper) {
		BlockPos engine = steamEngine(helper, new BlockPos(1, 1, 2));
		BlockState shaft = JugcraftKinetics.IRON_SHAFT.defaultBlockState().setValue(ShaftBlock.AXIS, Direction.Axis.X);
		helper.setBlock(engine.east(), shaft);
		helper.setBlock(engine.east(2), shaft);
		BlockPos crusherPos = engine.east(3);
		helper.setBlock(crusherPos, machine(MachineKind.CRUSHER));
		MachineBlockEntity crusher = helper.getBlockEntity(crusherPos, MachineBlockEntity.class);
		crusher.setItem(0, new ItemStack(item("tin_ore")));
		helper.succeedWhen(() -> {
			helper.assertTrue(helper.getBlockState(engine.east()).getValue(ShaftBlock.TURNING), "The shaft is not turning");
			helper.assertTrue(crusher.getItem(MachineKind.CRUSHER.outputSlot()).is(item("raw_tin")),
					"Crusher output is " + crusher.getItem(MachineKind.CRUSHER.outputSlot()));
		});
	}

	/** A gearbox splits one engine between a dynamo (which makes JE) and a machine on another side. */
	@GameTest(maxTicks = 200)
	public void gearboxBranchesToDynamoAndMachine(GameTestHelper helper) {
		BlockPos engine = steamEngine(helper, new BlockPos(1, 1, 2));
		BlockPos gearbox = engine.east();
		helper.setBlock(gearbox, JugcraftKinetics.BRASS_GEARBOX);
		helper.setBlock(gearbox.east(), JugcraftKinetics.DYNAMO);
		helper.setBlock(gearbox.south(), machine(MachineKind.ELECTRIC_FURNACE));
		DynamoBlockEntity dynamo = helper.getBlockEntity(gearbox.east(), DynamoBlockEntity.class);
		MachineBlockEntity furnace = helper.getBlockEntity(gearbox.south(), MachineBlockEntity.class);
		helper.succeedWhen(() -> {
			helper.assertTrue(dynamo.energy().getAmount() > 0, "The dynamo made no JE");
			helper.assertTrue(furnace.energyFor(null).getAmount() > 0, "The furnace got no power");
		});
	}

	/** A hand crank on top of a dynamo charges it while it turns. */
	@GameTest(maxTicks = 100)
	public void handCrankChargesDynamo(GameTestHelper helper) {
		BlockPos dynamoPos = new BlockPos(2, 1, 2);
		helper.setBlock(dynamoPos, JugcraftKinetics.DYNAMO);
		helper.setBlock(dynamoPos.above(), JugcraftKinetics.HAND_CRANK.defaultBlockState().setValue(HandCrankBlock.FACING, Direction.DOWN));
		helper.getBlockEntity(dynamoPos.above(), HandCrankBlockEntity.class).addTurns(HandCrankBlockEntity.TICKS_PER_CRANK);
		DynamoBlockEntity dynamo = helper.getBlockEntity(dynamoPos, DynamoBlockEntity.class);
		helper.succeedWhen(() -> helper.assertTrue(dynamo.energy().getAmount() >= 100,
				"The dynamo holds only " + dynamo.energy().getAmount() + " JE"));
	}

	/** A charged electric motor facing a crusher runs it on rotation alone, using its own JE. */
	@GameTest(maxTicks = 300)
	public void electricMotorDrivesCrusher(GameTestHelper helper) {
		BlockPos motorPos = new BlockPos(1, 1, 2);
		helper.setBlock(motorPos, JugcraftKinetics.ELECTRIC_MOTOR.defaultBlockState().setValue(ElectricMotorBlock.FACING, Direction.EAST));
		ElectricMotorBlockEntity motor = helper.getBlockEntity(motorPos, ElectricMotorBlockEntity.class);
		motor.energy().setAmount(ElectricMotorBlockEntity.CAPACITY);
		helper.setBlock(motorPos.east(), machine(MachineKind.CRUSHER));
		MachineBlockEntity crusher = helper.getBlockEntity(motorPos.east(), MachineBlockEntity.class);
		crusher.setItem(0, new ItemStack(item("tin_ore")));
		helper.succeedWhen(() -> {
			helper.assertTrue(crusher.getItem(MachineKind.CRUSHER.outputSlot()).is(item("raw_tin")),
					"Crusher output is " + crusher.getItem(MachineKind.CRUSHER.outputSlot()));
			helper.assertTrue(motor.energy().getAmount() < ElectricMotorBlockEntity.CAPACITY, "The motor used no JE");
		});
	}

	/**
	 * A magnet motor driving a magnet dynamo, which feeds the motor back: both run at the magnet rates, but at 95%
	 * each way the pair loses power every round and never gains any.
	 */
	@GameTest(maxTicks = 200)
	public void magnetMotorAndDynamoLoopLosesPower(GameTestHelper helper) {
		BlockPos motorPos = new BlockPos(1, 1, 2);
		helper.setBlock(motorPos, JugcraftKinetics.MAGNET_MOTOR.defaultBlockState().setValue(ElectricMotorBlock.FACING, Direction.EAST));
		helper.setBlock(motorPos.east(), JugcraftKinetics.MAGNET_DYNAMO);
		ElectricMotorBlockEntity motor = helper.getBlockEntity(motorPos, ElectricMotorBlockEntity.class);
		DynamoBlockEntity dynamo = helper.getBlockEntity(motorPos.east(), DynamoBlockEntity.class);
		helper.assertTrue(motor.stats() == ElectricMotorBlockEntity.MAGNET, "The magnet motor has copper stats");
		helper.assertTrue(dynamo.stats() == DynamoBlockEntity.MAGNET, "The magnet dynamo has copper stats");
		long start = ElectricMotorBlockEntity.MAGNET.capacity();
		motor.energy().setAmount(start);
		helper.runAfterDelay(100, () -> {
			long total = motor.energy().getAmount() + dynamo.energy().getAmount();
			helper.assertTrue(total < start, "The pair holds " + total + " JE of " + start);
			// At least 100 ticks of the motor's full 384 KE/t went round, losing about a tenth of each pass.
			helper.assertTrue(start - total >= 100L * 384 * 5 / 100, "Only " + (start - total) + " JE was lost");
			helper.succeed();
		});
	}

	/** A belt between two pulleys carries a hand crank's rotation to a dynamo under the other pulley. */
	@GameTest(maxTicks = 200)
	public void beltCarriesRotation(GameTestHelper helper) {
		BlockState pulley = JugcraftKinetics.BELT_PULLEY.defaultBlockState().setValue(ShaftBlock.AXIS, Direction.Axis.Y);
		BlockPos first = new BlockPos(1, 2, 2);
		BlockPos second = new BlockPos(5, 2, 2);
		helper.setBlock(first, pulley);
		helper.setBlock(second, pulley);
		helper.setBlock(second.below(), JugcraftKinetics.DYNAMO);
		BlockPos a = helper.absolutePos(first);
		BlockPos b = helper.absolutePos(second);
		helper.assertTrue(BeltPulleyBlockEntity.cannotLink(helper.getLevel(), a, b) == null,
				"The pulleys cannot link: " + BeltPulleyBlockEntity.cannotLink(helper.getLevel(), a, b));
		BeltPulleyBlockEntity.connect(helper.getLevel(), a, b);
		helper.setBlock(first.above(), JugcraftKinetics.HAND_CRANK.defaultBlockState().setValue(HandCrankBlock.FACING, Direction.DOWN));
		helper.getBlockEntity(first.above(), HandCrankBlockEntity.class).addTurns(HandCrankBlockEntity.TICKS_PER_CRANK);
		DynamoBlockEntity dynamo = helper.getBlockEntity(second.below(), DynamoBlockEntity.class);
		helper.succeedWhen(() -> helper.assertTrue(dynamo.energy().getAmount() >= 100,
				"The dynamo behind the belt holds only " + dynamo.energy().getAmount() + " JE"));
	}

	/** Pulleys on different levels of their axis, or too far apart, refuse a belt. */
	@GameTest
	public void beltRefusesBadPulleys(GameTestHelper helper) {
		BlockState pulley = JugcraftKinetics.BELT_PULLEY.defaultBlockState().setValue(ShaftBlock.AXIS, Direction.Axis.Y);
		helper.setBlock(new BlockPos(1, 1, 1), pulley);
		helper.setBlock(new BlockPos(3, 2, 1), pulley);
		String reason = BeltPulleyBlockEntity.cannotLink(helper.getLevel(), helper.absolutePos(new BlockPos(1, 1, 1)),
				helper.absolutePos(new BlockPos(3, 2, 1)));
		helper.assertTrue("axis".equals(reason), "Pulleys at different heights linked: " + reason);
		helper.succeed();
	}

	// ------------------------------------------------------------------ conveyors

	/** A charged electric motor at {@code pos} facing east, driving whatever is east of it. */
	private static void motorFacingEast(GameTestHelper helper, BlockPos pos) {
		helper.setBlock(pos, JugcraftKinetics.ELECTRIC_MOTOR.defaultBlockState().setValue(ElectricMotorBlock.FACING, Direction.EAST));
		helper.getBlockEntity(pos, ElectricMotorBlockEntity.class).energy().setAmount(ElectricMotorBlockEntity.CAPACITY);
	}

	private static ConveyorBlockEntity conveyor(GameTestHelper helper, BlockPos pos, Direction facing, boolean splitter) {
		helper.setBlock(pos, (splitter ? JugcraftLogistics.CONVEYOR_SPLITTER : JugcraftLogistics.CONVEYOR).defaultBlockState()
				.setValue(ConveyorBlock.FACING, facing));
		return helper.getBlockEntity(pos, ConveyorBlockEntity.class);
	}

	/** Items put in at the back of a driven two-conveyor line (as a pipe would) end up in the chest at its end. */
	@GameTest(maxTicks = 200)
	public void conveyorCarriesItemsIntoChest(GameTestHelper helper) {
		conveyor(helper, new BlockPos(2, 1, 2), Direction.EAST, false);
		conveyor(helper, new BlockPos(3, 1, 2), Direction.EAST, false);
		ChestBlockEntity chest = chest(helper, new BlockPos(4, 1, 2));
		motorFacingEast(helper, new BlockPos(1, 1, 2));
		Storage<ItemVariant> belt = ItemStorage.SIDED.find(helper.getLevel(), helper.absolutePos(new BlockPos(2, 1, 2)), Direction.UP);
		helper.assertTrue(belt != null, "The conveyor takes no items");
		try (Transaction transaction = Transaction.openOuter()) {
			helper.assertTrue(belt.insert(ItemVariant.of(Items.COBBLESTONE), 16, transaction) == 16, "The conveyor refused cobblestone");
			transaction.commit();
		}
		helper.succeedWhen(() -> helper.assertTrue(count(chest, Items.COBBLESTONE) == 16,
				"The chest holds " + count(chest, Items.COBBLESTONE) + " cobblestone"));
	}

	/** Without a drive, items stay on the belt. */
	@GameTest(maxTicks = 100)
	public void conveyorNeedsRotation(GameTestHelper helper) {
		ConveyorBlockEntity belt = conveyor(helper, new BlockPos(2, 1, 2), Direction.EAST, false);
		ChestBlockEntity chest = chest(helper, new BlockPos(3, 1, 2));
		helper.assertTrue(belt.accept(new ItemStack(Items.COBBLESTONE), 0.5F), "The conveyor refused an item");
		helper.runAtTickTime(60, () -> {
			helper.assertTrue(count(chest, Items.COBBLESTONE) == 0, "An undriven conveyor moved an item");
			helper.assertTrue(belt.items().size() == 1, "The item left the belt");
			helper.succeed();
		});
	}

	/** An item entity dropped on a driven conveyor is picked up and carried off. */
	@GameTest(maxTicks = 200)
	public void conveyorPicksUpDroppedItems(GameTestHelper helper) {
		conveyor(helper, new BlockPos(2, 1, 2), Direction.EAST, false);
		ChestBlockEntity chest = chest(helper, new BlockPos(3, 1, 2));
		motorFacingEast(helper, new BlockPos(1, 1, 2));
		helper.spawnItem(Items.IRON_INGOT, 2.5F, 1.5F, 2.5F);
		helper.succeedWhen(() -> helper.assertTrue(count(chest, Items.IRON_INGOT) == 1, "The dropped ingot did not arrive"));
	}

	/** A splitter sends one stack left, one straight on and one right. */
	@GameTest(maxTicks = 200)
	public void splitterTakesTurns(GameTestHelper helper) {
		ConveyorBlockEntity splitter = conveyor(helper, new BlockPos(2, 1, 2), Direction.EAST, true);
		ChestBlockEntity left = chest(helper, new BlockPos(2, 1, 1));
		ChestBlockEntity ahead = chest(helper, new BlockPos(3, 1, 2));
		ChestBlockEntity right = chest(helper, new BlockPos(2, 1, 3));
		for (float progress : new float[] {0.75F, 0.5F, 0.25F}) {
			helper.assertTrue(splitter.accept(new ItemStack(Items.COBBLESTONE), progress), "The splitter refused an item");
		}
		motorFacingEast(helper, new BlockPos(1, 1, 2));
		helper.succeedWhen(() -> {
			for (ChestBlockEntity chest : List.of(left, ahead, right)) {
				helper.assertTrue(count(chest, Items.COBBLESTONE) == 1, "Split unevenly: " + count(left, Items.COBBLESTONE) + " left, "
						+ count(ahead, Items.COBBLESTONE) + " ahead, " + count(right, Items.COBBLESTONE) + " right");
			}
		});
	}

	/** Items climb an up slope onto a raised conveyor, come down a down slope and land in a chest. */
	@GameTest(maxTicks = 300)
	public void conveyorSlopesGoUpAndDown(GameTestHelper helper) {
		conveyor(helper, new BlockPos(2, 1, 2), Direction.EAST, false);
		helper.setBlock(new BlockPos(3, 1, 2), JugcraftLogistics.CONVEYOR_SLOPE.defaultBlockState()
				.setValue(ConveyorBlock.FACING, Direction.EAST).setValue(ConveyorSlopeBlock.ASCENDING, true));
		conveyor(helper, new BlockPos(4, 2, 2), Direction.EAST, false);
		helper.setBlock(new BlockPos(5, 1, 2), JugcraftLogistics.CONVEYOR_SLOPE.defaultBlockState()
				.setValue(ConveyorBlock.FACING, Direction.EAST).setValue(ConveyorSlopeBlock.ASCENDING, false));
		ChestBlockEntity chest = chest(helper, new BlockPos(6, 1, 2));
		motorFacingEast(helper, new BlockPos(1, 1, 2));
		helper.getBlockEntity(new BlockPos(2, 1, 2), ConveyorBlockEntity.class).accept(new ItemStack(Items.COBBLESTONE, 8), 0);
		helper.succeedWhen(() -> helper.assertTrue(count(chest, Items.COBBLESTONE) == 8,
				"The chest holds " + count(chest, Items.COBBLESTONE) + " cobblestone"));
	}

	// ------------------------------------------------------------------ powered tools

	private static ItemStack charged(Item item) {
		ItemStack stack = new ItemStack(item);
		Chargeable.setEnergy(stack, Chargeable.capacity(stack));
		return stack;
	}

	/** A survival mock player at {@code pos}, facing south (+z), holding {@code tool}. */
	private static ServerPlayer miner(GameTestHelper helper, ItemStack tool) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setGameMode(GameType.SURVIVAL);
		player.setYRot(0);
		player.setXRot(0);
		player.setItemInHand(InteractionHand.MAIN_HAND, tool);
		return player;
	}

	/** A charging station on cables' power fills the tool on its cradle and lights up. */
	@GameTest(maxTicks = 100)
	public void chargingStationChargesTool(GameTestHelper helper) {
		BlockPos lower = new BlockPos(2, 1, 2);
		BlockState state = JugcraftTools.CHARGING_STATION.defaultBlockState();
		helper.setBlock(lower, state);
		helper.setBlock(lower.above(), state.setValue(ChargingStationBlock.HALF, DoubleBlockHalf.UPPER));
		charge(helper, lower.above(), Direction.WEST);
		ChargingStationBlockEntity station = helper.getBlockEntity(lower, ChargingStationBlockEntity.class);
		station.setTool(new ItemStack(JugcraftTools.MINING_DRILL));
		helper.succeedWhen(() -> {
			long energy = Chargeable.energy(station.tool());
			helper.assertTrue(energy >= ChargingStationBlockEntity.CHARGE_RATE * 10, "The drill holds only " + energy + " JE");
			helper.assertTrue(helper.getBlockState(lower).getValue(ChargingStationBlock.LIT), "The station is not lit while charging");
		});
	}

	/**
	 * Batch 25 gear. A paxel mines stone, logs and dirt fast. Bronze (iron tier) reaches diamond ore but not obsidian;
	 * steel (diamond tier) reaches both. Paxels last three times as long as the tier's tools. The armor goes in the right slots.
	 */
	@GameTest
	public void paxelsAndBronzeAndSteelGear(GameTestHelper helper) {
		ItemStack bronze = new ItemStack(item("bronze_paxel"));
		ItemStack steel = new ItemStack(item("steel_paxel"));
		for (BlockState state : List.of(Blocks.STONE.defaultBlockState(), Blocks.OAK_LOG.defaultBlockState(),
				Blocks.DIRT.defaultBlockState())) {
			helper.assertTrue(bronze.getDestroySpeed(state) > 1.0F, "A bronze paxel mines " + state + " like a hand");
		}
		// Iron tier (bronze) reaches diamond ore but not obsidian; diamond tier (steel) reaches both.
		helper.assertTrue(bronze.isCorrectToolForDrops(Blocks.DIAMOND_ORE.defaultBlockState()), "Bronze cannot get diamond ore");
		helper.assertTrue(!bronze.isCorrectToolForDrops(Blocks.OBSIDIAN.defaultBlockState()), "Bronze gets obsidian");
		helper.assertTrue(steel.isCorrectToolForDrops(Blocks.OBSIDIAN.defaultBlockState()), "A steel paxel cannot get obsidian");
		helper.assertTrue(new ItemStack(item("steel_pickaxe")).isCorrectToolForDrops(Blocks.OBSIDIAN.defaultBlockState()),
				"A steel pickaxe cannot get obsidian");
		helper.assertTrue(bronze.getMaxDamage() == 3 * new ItemStack(item("bronze_pickaxe")).getMaxDamage(),
				"A bronze paxel lasts " + bronze.getMaxDamage());
		helper.assertTrue(new ItemStack(item("diamond_paxel")).getMaxDamage() == 3 * new ItemStack(Items.DIAMOND_PICKAXE).getMaxDamage(),
				"A diamond paxel is not three diamond pickaxes' worth");
		helper.assertTrue(new ItemStack(item("netherite_paxel")).has(DataComponents.DAMAGE_RESISTANT), "The netherite paxel burns");
		for (String[] piece : new String[][] {{"helmet", "HEAD"}, {"chestplate", "CHEST"}, {"leggings", "LEGS"}, {"boots", "FEET"}}) {
			for (String metal : List.of("bronze", "steel")) {
				ItemStack stack = new ItemStack(item(metal + "_" + piece[0]));
				var equippable = stack.get(DataComponents.EQUIPPABLE);
				helper.assertTrue(equippable != null && equippable.slot() == EquipmentSlot.valueOf(piece[1]),
						metal + " " + piece[0] + " does not go in " + piece[1]);
			}
		}
		helper.succeed();
	}

	/**
	 * Batch 27: under water, a scuba tank worn with the mask tops the wearer's air up for 1 mB of oxygen; without the
	 * mask it does nothing.
	 */
	@GameTest
	public void scubaTankKeepsAirUnderWater(GameTestHelper helper) {
		for (int y = 1; y <= 3; y++) {
			helper.setBlock(new BlockPos(2, y, 2), Blocks.WATER);
		}
		ServerPlayer diver = helper.makeMockServerPlayerInLevel();
		Vec3 at = helper.absoluteVec(new Vec3(2.5, 1.0, 2.5));
		diver.setPos(at.x, at.y, at.z);
		ItemStack tank = new ItemStack(JugcraftGear.SCUBA_TANK);
		ScubaTankItem.setOxygen(tank, ScubaTankItem.CAPACITY);
		diver.setItemSlot(EquipmentSlot.CHEST, tank);
		diver.setItemSlot(EquipmentSlot.HEAD, new ItemStack(JugcraftGear.SCUBA_MASK));
		diver.baseTick();
		helper.assertTrue(diver.isEyeInFluid(FluidTags.WATER), "The diver's eyes are not under water");
		diver.setAirSupply(10);
		tank.getItem().inventoryTick(tank, helper.getLevel(), diver, EquipmentSlot.CHEST);
		helper.assertTrue(diver.getAirSupply() == diver.getMaxAirSupply(), "Air is " + diver.getAirSupply());
		helper.assertTrue(ScubaTankItem.oxygen(tank) == ScubaTankItem.CAPACITY - ScubaTankItem.OXYGEN_PER_TICK,
				"The tank holds " + ScubaTankItem.oxygen(tank) + " mB");
		diver.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
		diver.setAirSupply(10);
		tank.getItem().inventoryTick(tank, helper.getLevel(), diver, EquipmentSlot.CHEST);
		helper.assertTrue(diver.getAirSupply() == 10, "The tank gave air without the mask");
		helper.succeed();
	}

	/** Batch 27: free runners take away all fall damage and step up half a block more, on top of their armor. */
	@GameTest
	public void freeRunnersCancelFallDamage(GameTestHelper helper) {
		ItemAttributeModifiers modifiers = new ItemStack(JugcraftGear.FREE_RUNNERS).get(DataComponents.ATTRIBUTE_MODIFIERS);
		helper.assertTrue(modifiers != null, "Free runners have no attribute modifiers");
		helper.assertTrue(modifiers.modifiers().stream().anyMatch(entry -> entry.attribute().equals(Attributes.FALL_DAMAGE_MULTIPLIER)
				&& entry.modifier().amount() == JugcraftGear.RUNNERS_FALL_DAMAGE), "No fall damage modifier");
		helper.assertTrue(modifiers.modifiers().stream().anyMatch(entry -> entry.attribute().equals(Attributes.STEP_HEIGHT)
				&& entry.modifier().amount() == JugcraftGear.RUNNERS_STEP_HEIGHT), "No step height modifier");
		helper.assertTrue(modifiers.modifiers().stream().anyMatch(entry -> entry.attribute().equals(Attributes.ARMOR)),
				"Free runners lost their armor");
		helper.succeed();
	}

	/** Batch 27: a charged power katana hits at full strength and pays 1,000 JE a hit; an empty one hits for 1. */
	@GameTest
	public void powerKatanaRunsOnCharge(GameTestHelper helper) {
		ServerPlayer player = miner(helper, new ItemStack(JugcraftGear.POWER_KATANA));
		float bonus = JugcraftGear.POWER_KATANA.getAttackDamageBonus(player, 11.0F,
				helper.getLevel().damageSources().playerAttack(player));
		helper.assertTrue(bonus == -10.0F, "An empty katana's bonus is " + bonus);
		player.setItemInHand(InteractionHand.MAIN_HAND, charged(JugcraftGear.POWER_KATANA));
		bonus = JugcraftGear.POWER_KATANA.getAttackDamageBonus(player, 11.0F, helper.getLevel().damageSources().playerAttack(player));
		helper.assertTrue(bonus == 0.0F, "A charged katana's bonus is " + bonus);
		ItemStack katana = player.getMainHandItem();
		katana.getItem().postHurtEnemy(katana, player, player);
		helper.assertTrue(Chargeable.energy(katana) == PowerKatanaItem.CAPACITY - PowerKatanaItem.ENERGY_PER_HIT,
				"The katana holds " + Chargeable.energy(katana) + " JE");
		helper.succeed();
	}

	/**
	 * Batch 27: a charged power bow fires an energy arrow with no arrows in the inventory, for 500 JE, and the arrow
	 * cannot be picked up; an empty one with no arrows fires nothing.
	 */
	@GameTest
	public void powerBowFiresOnCharge(GameTestHelper helper) {
		ServerPlayer player = miner(helper, charged(JugcraftGear.POWER_BOW));
		// Stand inside the test area, where the chunk is loaded, so the arrow is added to the world.
		Vec3 at = helper.absoluteVec(new Vec3(2.5, 1.0, 2.5));
		player.setPos(at.x, at.y, at.z);
		ItemStack bow = player.getMainHandItem();
		int drawn = bow.getUseDuration(player) - 20;
		helper.assertTrue(JugcraftGear.POWER_BOW.releaseUsing(bow, helper.getLevel(), player, drawn), "The charged bow did not fire");
		List<Arrow> arrows = helper.getLevel().getEntitiesOfClass(Arrow.class, player.getBoundingBox().inflate(8));
		helper.assertTrue(arrows.size() == 1, arrows.size() + " arrows");
		helper.assertTrue(arrows.get(0).pickup == Arrow.Pickup.CREATIVE_ONLY, "The energy arrow can be picked up");
		helper.assertTrue(Chargeable.energy(bow) == PowerBowItem.CAPACITY - PowerBowItem.ENERGY_PER_SHOT,
				"The bow holds " + Chargeable.energy(bow) + " JE");
		ItemStack empty = new ItemStack(JugcraftGear.POWER_BOW);
		player.setItemInHand(InteractionHand.MAIN_HAND, empty);
		helper.assertTrue(!JugcraftGear.POWER_BOW.releaseUsing(empty, helper.getLevel(), player, drawn), "The empty bow fired");
		helper.assertTrue(helper.getLevel().getEntitiesOfClass(Arrow.class, player.getBoundingBox().inflate(8)).size() == 1,
				"The empty bow made an arrow");
		helper.succeed();
	}

	private static EquipmentSlot slot(ArmorType type) {
		return switch (type) {
			case HELMET -> EquipmentSlot.HEAD;
			case CHESTPLATE -> EquipmentSlot.CHEST;
			case LEGGINGS -> EquipmentSlot.LEGS;
			default -> EquipmentSlot.FEET;
		};
	}

	/**
	 * Batch 28: a charged exosuit gives more speed, a full-block step, no fall damage and a shield, and its leggings
	 * and boots pay a JE a tick; flat pieces give none of it, and nothing is left behind.
	 */
	@GameTest
	public void exosuitPowersRunOnCharge(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		for (ArmorType type : JugcraftExosuit.PIECES) {
			player.setItemSlot(slot(type), charged(JugcraftExosuit.piece(ExosuitItem.Style.VANGUARD, type)));
		}
		Exosuit.tick(player);
		helper.assertTrue(player.getAttribute(Attributes.MOVEMENT_SPEED).hasModifier(Exosuit.SPEED), "No speed bonus");
		helper.assertTrue(player.getAttribute(Attributes.STEP_HEIGHT).getValue() >= 1.0,
				"Step height " + player.getAttribute(Attributes.STEP_HEIGHT).getValue());
		helper.assertTrue(player.getAttribute(Attributes.FALL_DAMAGE_MULTIPLIER).getValue() == 0.0, "Fall damage still counts");
		helper.assertTrue(player.getMaxAbsorption() >= Exosuit.SHIELD_POINTS, "Shield holds " + player.getMaxAbsorption());
		for (EquipmentSlot paid : new EquipmentSlot[] {EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
			ItemStack piece = player.getItemBySlot(paid);
			helper.assertTrue(Chargeable.energy(piece) == Chargeable.capacity(piece) - 1,
					"The " + paid + " piece holds " + Chargeable.energy(piece) + " JE");
		}
		for (ArmorType type : JugcraftExosuit.PIECES) {
			player.setItemSlot(slot(type), new ItemStack(JugcraftExosuit.piece(ExosuitItem.Style.VANGUARD, type)));
		}
		Exosuit.tick(player);
		helper.assertTrue(!player.getAttribute(Attributes.MOVEMENT_SPEED).hasModifier(Exosuit.SPEED), "Flat leggings still speed");
		helper.assertTrue(!player.getAttribute(Attributes.STEP_HEIGHT).hasModifier(Exosuit.STEP), "Flat boots still step");
		helper.assertTrue(!player.getAttribute(Attributes.FALL_DAMAGE_MULTIPLIER).hasModifier(Exosuit.FALL), "Flat boots still cushion");
		helper.assertTrue(player.getMaxAbsorption() == 0.0F, "A flat chestplate still shields");
		helper.succeed();
	}

	/** Batch 28: the chestplate's shield regrows absorption for 4,000 JE a point. */
	@GameTest(maxTicks = 100)
	public void exosuitShieldRegrows(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setItemSlot(EquipmentSlot.CHEST, charged(JugcraftExosuit.piece(ExosuitItem.Style.RONIN, ArmorType.CHESTPLATE)));
		player.setAbsorptionAmount(0.0F);
		helper.succeedWhen(() -> {
			Exosuit.tick(player);
			ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
			helper.assertTrue(player.getAbsorptionAmount() >= 1.0F, "No shield yet");
			helper.assertTrue(Chargeable.energy(chest) <= Chargeable.capacity(chest) - Exosuit.SHIELD_PER_POINT,
					"The shield cost " + (Chargeable.capacity(chest) - Chargeable.energy(chest)) + " JE");
		});
	}

	/** Batch 28: the exosuit chestplate flies like the rocket pack, on its own charge. */
	@GameTest
	public void exosuitChestplateIsAJetpack(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setItemSlot(EquipmentSlot.CHEST, charged(JugcraftExosuit.piece(ExosuitItem.Style.VANGUARD, ArmorType.CHESTPLATE)));
		helper.assertTrue(RocketPackItem.canThrust(player), "The chestplate cannot thrust");
		player.fallDistance = 10;
		RocketPackItem.thrust(player);
		ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
		helper.assertTrue(Chargeable.energy(chest) == Chargeable.capacity(chest) - RocketPackItem.ENERGY_PER_TICK,
				"The chestplate holds " + Chargeable.energy(chest) + " JE");
		helper.assertTrue(player.fallDistance == 0, "The fall was not cancelled");
		player.setItemSlot(EquipmentSlot.CHEST, charged(JugcraftExosuit.piece(ExosuitItem.Style.VANGUARD, ArmorType.LEGGINGS)));
		helper.assertTrue(!RocketPackItem.canThrust(player), "Leggings worn on the chest fly");
		helper.succeed();
	}

	/** Batch 28: the Ronin livery repaints an exosuit piece at the smithing table and keeps its charge. */
	@GameTest
	public void liveryRepaintsAndKeepsCharge(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ItemStack base = new ItemStack(JugcraftExosuit.piece(ExosuitItem.Style.VANGUARD, ArmorType.HELMET));
		Chargeable.setEnergy(base, 123_456);
		SmithingRecipeInput input = new SmithingRecipeInput(new ItemStack(JugcraftExosuit.RONIN_LIVERY), base,
				// Looked up by ID: the dye has no Items constant in 26.3.
				new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse("minecraft:red_dye"))));
		Optional<RecipeHolder<SmithingRecipe>> recipe = level.recipeAccess().getRecipeFor(RecipeType.SMITHING, input, level);
		helper.assertTrue(recipe.isPresent(), "No livery recipe for the helmet");
		ItemStack out = recipe.get().value().assemble(input);
		helper.assertTrue(out.is(JugcraftExosuit.piece(ExosuitItem.Style.RONIN, ArmorType.HELMET)), "Repainted into " + out);
		helper.assertTrue(Chargeable.energy(out) == 123_456, "The repainted helmet holds " + Chargeable.energy(out) + " JE");
		helper.succeed();
	}

	/** An empty drill mines like a bare hand and gets no ore drops; a charged one is fast and correct. */
	@GameTest
	public void emptyDrillIsSlow(GameTestHelper helper) {
		BlockState stone = Blocks.STONE.defaultBlockState();
		ItemStack empty = new ItemStack(JugcraftTools.MINING_DRILL);
		ItemStack full = charged(JugcraftTools.MINING_DRILL);
		helper.assertTrue(empty.getDestroySpeed(stone) == 1.0F, "An empty drill mines at " + empty.getDestroySpeed(stone));
		helper.assertTrue(!empty.isCorrectToolForDrops(Blocks.IRON_ORE.defaultBlockState()), "An empty drill gets ore drops");
		helper.assertTrue(full.getDestroySpeed(stone) > 8.0F, "A charged drill mines at only " + full.getDestroySpeed(stone));
		helper.assertTrue(full.isCorrectToolForDrops(Blocks.DIAMOND_ORE.defaultBlockState()), "A charged drill cannot mine diamond ore");
		helper.succeed();
	}

	/** In 3x3 mode, breaking one block of a wall breaks the square around it, for the energy of nine blocks. */
	@GameTest
	public void drillMinesThreeByThree(GameTestHelper helper) {
		for (int x = 1; x <= 3; x++) {
			for (int y = 1; y <= 3; y++) {
				helper.setBlock(new BlockPos(x, y, 3), Blocks.STONE);
			}
		}
		ItemStack drill = charged(JugcraftTools.MINING_DRILL);
		drill.set(JugcraftTools.DRILL_MODE, MiningDrillItem.AREA);
		ServerPlayer player = miner(helper, drill);
		helper.assertTrue(player.gameMode.destroyBlock(helper.absolutePos(new BlockPos(2, 2, 3))), "The drill broke nothing");
		for (int x = 1; x <= 3; x++) {
			for (int y = 1; y <= 3; y++) {
				helper.assertBlockPresent(Blocks.AIR, new BlockPos(x, y, 3));
			}
		}
		long used = JugcraftTools.DRILL_CAPACITY - Chargeable.energy(player.getMainHandItem());
		helper.assertTrue(used == 9 * JugcraftTools.DRILL_ENERGY_PER_BLOCK, "The drill used " + used + " JE for nine blocks");
		helper.succeed();
	}

	/** Cutting the bottom log with the chainsaw fells the whole tree, branches too. */
	@GameTest
	public void chainsawFellsTree(GameTestHelper helper) {
		for (int y = 1; y <= 5; y++) {
			helper.setBlock(new BlockPos(2, y, 2), Blocks.OAK_LOG);
		}
		helper.setBlock(new BlockPos(3, 4, 3), Blocks.OAK_LOG);
		ServerPlayer player = miner(helper, charged(JugcraftTools.CHAINSAW));
		player.gameMode.destroyBlock(helper.absolutePos(new BlockPos(2, 1, 2)));
		for (int y = 1; y <= 5; y++) {
			helper.assertBlockPresent(Blocks.AIR, new BlockPos(2, y, 2));
		}
		helper.assertBlockPresent(Blocks.AIR, new BlockPos(3, 4, 3));
		helper.succeed();
	}

	/** A tick of rocket thrust costs its JE and cancels a fall. */
	@GameTest
	public void rocketPackThrustUsesEnergy(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setItemSlot(EquipmentSlot.CHEST, charged(JugcraftTools.ROCKET_PACK));
		player.fallDistance = 10;
		RocketPackItem.thrust(player);
		long left = Chargeable.energy(player.getItemBySlot(EquipmentSlot.CHEST));
		helper.assertTrue(left == RocketPackItem.CAPACITY - RocketPackItem.ENERGY_PER_TICK, "The pack holds " + left + " JE");
		helper.assertTrue(player.fallDistance == 0, "The fall was not cancelled");
		helper.succeed();
	}

	/** A capacity module doubles what a drill holds; a second triples it; a third does not fit. */
	@GameTest
	public void capacityModulesAddCharge(GameTestHelper helper) {
		ItemStack drill = new ItemStack(JugcraftTools.MINING_DRILL);
		for (int i = 1; i <= 2; i++) {
			helper.assertTrue(ToolUpgrades.fit(helper.getLevel(), drill, ToolUpgrades.Kind.CAPACITY) == ToolUpgrades.Result.FITTED,
					"Capacity module " + i + " did not fit");
		}
		helper.assertTrue(ToolUpgrades.fit(helper.getLevel(), drill, ToolUpgrades.Kind.CAPACITY) == ToolUpgrades.Result.FULL,
				"A third capacity module fitted");
		Chargeable.setEnergy(drill, Long.MAX_VALUE);
		helper.assertTrue(Chargeable.energy(drill) == 3 * JugcraftTools.DRILL_CAPACITY, "The drill holds " + Chargeable.energy(drill));
		helper.succeed();
	}

	/** Modules fit only the tools they are for, and silk touch and fortune exclude each other. */
	@GameTest
	public void modulesFitTheRightTools(GameTestHelper helper) {
		ItemStack chainsaw = new ItemStack(JugcraftTools.CHAINSAW);
		helper.assertTrue(ToolUpgrades.fit(helper.getLevel(), chainsaw, ToolUpgrades.Kind.RANGE) == ToolUpgrades.Result.WRONG_TOOL,
				"A range module fitted the chainsaw");
		ItemStack drill = new ItemStack(JugcraftTools.MINING_DRILL);
		helper.assertTrue(ToolUpgrades.fit(helper.getLevel(), drill, ToolUpgrades.Kind.SILK_TOUCH) == ToolUpgrades.Result.FITTED,
				"Silk touch did not fit the drill");
		helper.assertTrue(ToolUpgrades.fit(helper.getLevel(), drill, ToolUpgrades.Kind.FORTUNE) == ToolUpgrades.Result.CONFLICT,
				"Fortune fitted alongside silk touch");
		helper.assertTrue(!drill.getEnchantments().isEmpty(), "The drill has no enchantment after a silk touch module");
		helper.assertTrue(ToolUpgrades.fit(helper.getLevel(), new ItemStack(JugcraftTools.ROCKET_PACK), ToolUpgrades.Kind.OVERCLOCK)
				== ToolUpgrades.Result.WRONG_TOOL, "An overclock module fitted the rocket pack");
		helper.succeed();
	}

	/** With a range module, the drill's area mode breaks a 5x5 square for the energy of 25 blocks. */
	@GameTest
	public void rangeModuleMinesFiveByFive(GameTestHelper helper) {
		for (int x = 0; x <= 4; x++) {
			for (int y = 1; y <= 5; y++) {
				helper.setBlock(new BlockPos(x, y, 3), Blocks.STONE);
			}
		}
		ItemStack drill = charged(JugcraftTools.MINING_DRILL);
		drill.set(JugcraftTools.DRILL_MODE, MiningDrillItem.AREA);
		ToolUpgrades.fit(helper.getLevel(), drill, ToolUpgrades.Kind.RANGE);
		ServerPlayer player = miner(helper, drill);
		player.gameMode.destroyBlock(helper.absolutePos(new BlockPos(2, 3, 3)));
		for (int x = 0; x <= 4; x++) {
			for (int y = 1; y <= 5; y++) {
				helper.assertBlockPresent(Blocks.AIR, new BlockPos(x, y, 3));
			}
		}
		long used = JugcraftTools.DRILL_CAPACITY - Chargeable.energy(player.getMainHandItem());
		helper.assertTrue(used == 25 * JugcraftTools.DRILL_ENERGY_PER_BLOCK, "The drill used " + used + " JE for 25 blocks");
		helper.succeed();
	}

	/** An overclock module speeds the drill up by half and doubles its JE per block. */
	@GameTest
	public void overclockModuleSpeedsUp(GameTestHelper helper) {
		ItemStack plain = charged(JugcraftTools.MINING_DRILL);
		ItemStack fast = charged(JugcraftTools.MINING_DRILL);
		ToolUpgrades.fit(helper.getLevel(), fast, ToolUpgrades.Kind.OVERCLOCK);
		BlockState stone = Blocks.STONE.defaultBlockState();
		float ratio = fast.getDestroySpeed(stone) / plain.getDestroySpeed(stone);
		helper.assertTrue(Math.abs(ratio - 1.5F) < 0.01F, "Overclocked speed ratio " + ratio);
		helper.assertTrue(((MiningDrillItem) fast.getItem()).energyPerBlock(fast) == 2 * JugcraftTools.DRILL_ENERGY_PER_BLOCK,
				"Overclocked JE per block " + ((MiningDrillItem) fast.getItem()).energyPerBlock(fast));
		helper.succeed();
	}

	/** Charcoal burns three quarters as long as coal in generators (tree farm wood power, slightly weakened). */
	@GameTest
	public void charcoalBurnsShorterThanCoal(GameTestHelper helper) {
		int coal = GeneratorFuels.burnTicks(new ItemStack(Items.COAL));
		int charcoal = GeneratorFuels.burnTicks(new ItemStack(Items.CHARCOAL));
		helper.assertTrue(coal == 1600 && charcoal == 1200, "Coal burns " + coal + " ticks and charcoal " + charcoal);
		helper.succeed();
	}

	// ------------------------------------------------------------------ advancements

	/** The quest line loads: its first and last steps exist, and the rocket pack step leads back to the root. */
	@GameTest
	public void advancementTreeLoads(GameTestHelper helper) {
		var advancements = helper.getLevel().getServer().getAdvancements();
		for (String id : List.of("root", "bronze", "steel", "charging_station", "upgrade", "rocket_pack")) {
			helper.assertTrue(advancements.get(Jugcraft.id(id)) != null, "Advancement jugcraft:" + id + " did not load");
		}
		var step = advancements.get(Jugcraft.id("rocket_pack"));
		int depth = 0;
		while (step != null && step.value().parent().isPresent() && depth < 20) {
			step = advancements.get(step.value().parent().get());
			depth++;
		}
		helper.assertTrue(step != null && step.id().equals(Jugcraft.id("root")), "The rocket pack step does not lead back to the root");
		helper.succeed();
	}

	// ------------------------------------------------------------------ auto-crafter

	private static MachineBlockEntity autoCrafter(GameTestHelper helper, BlockPos pos) {
		helper.setBlock(pos, machine(MachineKind.AUTO_CRAFTER));
		charge(helper, pos, Direction.UP);
		return helper.getBlockEntity(pos, MachineBlockEntity.class);
	}

	/** Planks laid out as a stick recipe craft until one plank is left in each slot as the pattern. */
	@GameTest(maxTicks = 300)
	public void autoCrafterKeepsItsPattern(GameTestHelper helper) {
		MachineBlockEntity crafter = autoCrafter(helper, new BlockPos(2, 1, 2));
		crafter.setItem(0, new ItemStack(Items.OAK_PLANKS, 3));
		crafter.setItem(3, new ItemStack(Items.OAK_PLANKS, 3));
		helper.succeedWhen(() -> {
			ItemStack sticks = crafter.getItem(MachineKind.AUTO_CRAFTER.outputSlot());
			helper.assertTrue(sticks.is(Items.STICK) && sticks.getCount() == 8, "Crafted " + sticks);
			helper.assertTrue(crafter.getItem(0).getCount() == 1 && crafter.getItem(3).getCount() == 1,
					"The pattern was not kept: " + crafter.getItem(0) + ", " + crafter.getItem(3));
		});
	}

	/** Container remainders (the glass bottles from a honey block) go to the remainder slot. */
	@GameTest(maxTicks = 200)
	public void autoCrafterKeepsRemainders(GameTestHelper helper) {
		MachineBlockEntity crafter = autoCrafter(helper, new BlockPos(2, 1, 2));
		for (int slot : new int[] {0, 1, 3, 4}) {
			crafter.setItem(slot, new ItemStack(Items.HONEY_BOTTLE, 2));
		}
		helper.succeedWhen(() -> {
			helper.assertTrue(crafter.getItem(MachineKind.AUTO_CRAFTER.outputSlot()).is(Items.HONEY_BLOCK), "No honey block");
			ItemStack bottles = crafter.getItem(MachineKind.AUTO_CRAFTER.outputSlot() + 1);
			helper.assertTrue(bottles.is(Items.GLASS_BOTTLE) && bottles.getCount() == 4, "Remainder slot holds " + bottles);
		});
	}

	/** Pipes and hoppers only top up grid slots that already hold that item; they never set the pattern. */
	@GameTest
	public void autoCrafterAutomationFollowsPattern(GameTestHelper helper) {
		MachineBlockEntity crafter = autoCrafter(helper, new BlockPos(2, 1, 2));
		crafter.setItem(4, new ItemStack(Items.OAK_PLANKS, 1));
		Storage<ItemVariant> storage = ItemStorage.SIDED.find(helper.getLevel(), helper.absolutePos(new BlockPos(2, 1, 2)), Direction.UP);
		try (Transaction transaction = Transaction.openOuter()) {
			long planks = storage.insert(ItemVariant.of(Items.OAK_PLANKS), 10, transaction);
			long cobble = storage.insert(ItemVariant.of(Items.COBBLESTONE), 10, transaction);
			helper.assertTrue(planks == 10, "Accepted " + planks + " planks into the pattern slot");
			helper.assertTrue(cobble == 0, "Accepted cobblestone into an empty grid slot");
			transaction.abort();
		}
		helper.succeed();
	}

	/**
	 * Raider extras: grunts marching on an objective behind a wall they cannot path round prop siege ladders up it, which
	 * are climbable and drop nothing.
	 */
	@GameTest(structure = "jugcraft-test:drone_tower", maxTicks = 600, skyAccess = true)
	public void siegeLaddersGoUpWalls(GameTestHelper helper) {
		raiderFloor(helper);
		// A walled yard round the objective: closed on every side, since the test world's ground runs on past the
		// floor and an open-ended wall is simply walked round.
		for (int i = 12; i <= 28; i++) {
			for (int y = 1; y <= 5; y++) {
				helper.setBlock(new BlockPos(i, y, 2), Blocks.STONE_BRICKS);
				helper.setBlock(new BlockPos(i, y, 18), Blocks.STONE_BRICKS);
				helper.setBlock(new BlockPos(12, y, i - 10), Blocks.STONE_BRICKS);
				helper.setBlock(new BlockPos(28, y, i - 10), Blocks.STONE_BRICKS);
			}
		}
		ServerLevel level = helper.getLevel();
		var raid = io.github.jimbozoomer.jugcraft.raiders.RaiderRaids.start(level, helper.absolutePos(new BlockPos(20, 1, 10)),
				helper.absolutePos(new BlockPos(20, 1, 34)), 1, level.getRandom());
		helper.succeedWhen(() -> {
			boolean ladder = false;
			for (int x = 10; x <= 30 && !ladder; x++) {
				for (int z = 0; z <= 20 && !ladder; z++) {
					for (int y = 1; y <= 5 && !ladder; y++) {
						ladder = io.github.jimbozoomer.jugcraft.raiders.SiegeLadderBlock.is(helper.getBlockState(new BlockPos(x, y, z)));
					}
				}
			}
			if (!ladder) {
				// Say where the raid's infantry are and what they are doing, so a failure shows why.
				StringBuilder seen = new StringBuilder();
				for (var raider : level.getEntitiesOfClass(io.github.jimbozoomer.jugcraft.raiders.RaiderInfantry.class,
						new net.minecraft.world.phys.AABB(helper.absolutePos(BlockPos.ZERO)).inflate(64))) {
					BlockPos rel = raider.blockPosition().subtract(helper.absolutePos(BlockPos.ZERO));
					seen.append(' ').append(net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(raider.getType()).getPath()).append('@').append(rel.getX()).append(',').append(rel.getY()).append(',')
							.append(rel.getZ()).append(raider.getTarget() != null ? " target=" + net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(raider.getTarget().getType()).getPath() : "")
							.append(raider.getNavigation().isDone() ? " idle" : " moving").append(';');
				}
				helper.assertTrue(false, "A grunt stuck at the wall should have propped a siege ladder against it; raiders:" + seen);
			}
			helper.assertTrue(io.github.jimbozoomer.jugcraft.raiders.JugcraftRaiders.SIEGE_LADDER.defaultBlockState()
					.is(net.minecraft.tags.BlockTags.CLIMBABLE), "A siege ladder should be climbable");
			io.github.jimbozoomer.jugcraft.raiders.RaiderRaids.withdraw(level, raid);
		});
	}

	/**
	 * Raider extras: the Raider War Horn calls a raid at the world's raid level on the blower's base, or refuses (and is
	 * kept) while another raid is under way.
	 */
	@GameTest(structure = "jugcraft-test:drone_tower", maxTicks = 100, skyAccess = true)
	public void raidHornCallsARaid(GameTestHelper helper) {
		raiderFloor(helper);
		ServerLevel level = helper.getLevel();
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setPos(helper.absoluteVec(new Vec3(20.5, 1, 20.5)));
		player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(io.github.jimbozoomer.jugcraft.raiders.JugcraftRaiders.RAID_HORN));
		int before = io.github.jimbozoomer.jugcraft.raiders.RaiderRaids.raids(level).size();
		var result = io.github.jimbozoomer.jugcraft.raiders.JugcraftRaiders.RAID_HORN.use(level, player, net.minecraft.world.InteractionHand.MAIN_HAND);
		var raids = io.github.jimbozoomer.jugcraft.raiders.RaiderRaids.raids(level);
		if (before > 0) {
			// Another test's raid is under way: the horn must refuse and start nothing.
			helper.assertTrue(result == net.minecraft.world.InteractionResult.FAIL && raids.size() == before, "The horn should refuse while a raid is under way");
		} else if (result == net.minecraft.world.InteractionResult.SUCCESS) {
			helper.assertTrue(raids.size() == 1, "Blowing the horn should start one raid");
			var raid = raids.get(0);
			helper.assertTrue(raid.objective().equals(player.blockPosition()) && raid.level() == io.github.jimbozoomer.jugcraft.raiders.RaiderRaids.raidLevel(level),
					"It should come for the blower's base at the world's raid level");
			io.github.jimbozoomer.jugcraft.raiders.RaiderRaids.withdraw(level, raid);
		} else {
			// Nowhere loaded and open for the party to gather (the test area's surroundings): refused, nothing started.
			helper.assertTrue(raids.isEmpty(), "A refused horn should start nothing");
		}
		helper.succeed();
	}

	/**
	 * Raider extras: a raider camp on flat ground has its sandbag ring, campfire, tents and a supply barrel with its loot
	 * table, held by a garrison of four raiders who never despawn; on uneven ground none is built.
	 */
	@GameTest(structure = "jugcraft-test:drone_tower", skyAccess = true)
	public void raiderCampHoldsItsGround(GameTestHelper helper) {
		raiderFloor(helper);
		ServerLevel level = helper.getLevel();
		BlockPos origin = helper.absolutePos(new BlockPos(14, 1, 14));
		helper.assertTrue(io.github.jimbozoomer.jugcraft.raiders.RaiderCamps.build(level, origin, level.getRandom()),
				"A camp should be built on flat ground");
		helper.assertBlockPresent(Blocks.CAMPFIRE, new BlockPos(14, 1, 14));
		helper.assertBlockPresent(Blocks.BARREL, new BlockPos(17, 1, 16));
		var barrel = helper.getBlockEntity(new BlockPos(17, 1, 16), net.minecraft.world.level.block.entity.BarrelBlockEntity.class);
		helper.assertTrue(io.github.jimbozoomer.jugcraft.raiders.RaiderCamps.LOOT.equals(barrel.getLootTable()), "The barrel should hold the camp's loot");
		var raiders = level.getEntitiesOfClass(io.github.jimbozoomer.jugcraft.raiders.RaiderInfantry.class,
				new net.minecraft.world.phys.AABB(origin).inflate(8));
		helper.assertTrue(raiders.size() == 4 && raiders.stream().allMatch(net.minecraft.world.entity.Mob::isPersistenceRequired),
				"Four raiders should hold the camp, for good, not " + raiders.size());
		// A tall step in the ground: no camp.
		for (int x = 32; x < 44; x++) {
			for (int z = 30; z < 44; z++) {
				for (int y = 1; y <= 4; y++) {
					helper.setBlock(new BlockPos(x, y, z), Blocks.STONE);
				}
			}
		}
		helper.assertFalse(io.github.jimbozoomer.jugcraft.raiders.RaiderCamps.build(level, helper.absolutePos(new BlockPos(32, 1, 32)),
				level.getRandom()), "No camp on uneven ground");
		for (var raider : raiders) {
			raider.discard();
		}
		helper.succeed();
	}

	/**
	 * Fire control extras: a creeping barrage fires its first salvo on the target, then each salvo after steps the point
	 * {@value io.github.jimbozoomer.jugcraft.building.FireControl#CREEP_STEP} blocks further down range, starting again after
	 * {@value io.github.jimbozoomer.jugcraft.building.FireControl#CREEP_STEPS} steps; setting a new target starts it afresh.
	 */
	@GameTest(structure = "jugcraft-test:drone_tower", skyAccess = true)
	public void creepingBarrageWalksDownRange(GameTestHelper helper) {
		var battery = linkedBattery(helper, io.github.jimbozoomer.jugcraft.building.FireControlTableBlock.Mode.CREEPING);
		var table = helper.getBlockEntity(new BlockPos(8, 1, 2), io.github.jimbozoomer.jugcraft.building.FireControlTableBlock.Entity.class);
		var mode = io.github.jimbozoomer.jugcraft.building.FireControlTableBlock.Mode.CREEPING;
		table.setTarget(helper.absolutePos(new BlockPos(8, 0, 20)));
		Vec3 start = table.aimPoint(battery.getUUID());
		int step = io.github.jimbozoomer.jugcraft.building.FireControl.CREEP_STEP;
		int steps = io.github.jimbozoomer.jugcraft.building.FireControl.CREEP_STEPS;
		helper.assertTrue(table.fire(mode), "A creeping barrage with a target should fire");
		helper.assertTrue(table.aimPoint(battery.getUUID()).distanceTo(start) < 1.0E-6, "The first salvo lands on the target itself");
		helper.assertTrue(table.fire(mode), "and the second fires too");
		Vec3 next = table.aimPoint(battery.getUUID());
		helper.assertTrue(Math.abs(next.z - start.z - step) < 1.0E-6 && Math.abs(next.x - start.x) < 1.0E-6,
				"The second salvo should land " + step + " blocks further down range, not at " + next.subtract(start));
		for (int i = 2; i <= steps; i++) {
			table.fire(mode);
		}
		helper.assertTrue(table.aimPoint(battery.getUUID()).distanceTo(start) < 1.0E-6, "After " + steps + " steps it starts again");
		table.fire(mode);
		table.setTarget(helper.absolutePos(new BlockPos(8, 0, 20)));
		helper.assertTrue(table.creep() == 0 && table.aimPoint(battery.getUUID()).distanceTo(start) < 1.0E-6,
				"Setting the target again starts the barrage afresh");
		helper.succeed();
	}

	/**
	 * Fire control extras: a gunner aboard a linked gun, with no mark of their own, has the gun laid on the table's point
	 * (whichever way they look); they still choose when it fires.
	 */
	@GameTest(structure = "jugcraft-test:drone_tower", maxTicks = 200, skyAccess = true)
	public void crewedGunFollowsTheTable(GameTestHelper helper) {
		var battery = linkedBattery(helper, io.github.jimbozoomer.jugcraft.building.FireControlTableBlock.Mode.CONVERGE);
		var table = helper.getBlockEntity(new BlockPos(8, 1, 2), io.github.jimbozoomer.jugcraft.building.FireControlTableBlock.Entity.class);
		// Off to the side of the battery's start, so it has to turn to it.
		BlockPos target = helper.absolutePos(new BlockPos(30, 0, 30));
		table.setTarget(target);
		ServerPlayer gunner = helper.makeMockServerPlayerInLevel();
		gunner.setPos(battery.getX(), battery.getY(), battery.getZ());
		helper.assertTrue(gunner.startRiding(battery, true, true), "The gunner could not climb aboard");
		io.github.jimbozoomer.jugcraft.artillery.Spotting.clear(gunner);
		helper.onEachTick(() -> gunner.setYRot(90.0F));
		helper.succeedWhen(() -> {
			Vec3 to = Vec3.atCenterOf(target).subtract(battery.position());
			float wanted = (float) Math.toDegrees(Math.atan2(-to.x, to.z));
			float off = Math.abs(net.minecraft.util.Mth.wrapDegrees(battery.aimYaw(0) - wanted));
			helper.assertTrue(off < 4.0F, "The crewed gun should turn to the table's target (yaw " + wanted + "), not " + battery.aimYaw(0));
		});
	}

	/**
	 * Fortification extras: the bunker door opens by hand; two sliding gates side by side open together on one signal and
	 * leave only a post to bump into; the embrasure has a slit through it; the parapet corner's merlon turns with it.
	 */
	@GameTest(maxTicks = 60)
	public void fortificationExtras(GameTestHelper helper) {
		var blocks = io.github.jimbozoomer.jugcraft.building.Fortifications.BLOCKS;
		var door = (net.minecraft.world.level.block.DoorBlock) blocks.get("bunker_door");
		helper.assertTrue(door.type().canOpenByHand(), "A bunker door should open by hand");
		ServerLevel level = helper.getLevel();
		BlockPos embrasure = new BlockPos(5, 1, 5);
		helper.setBlock(embrasure, blocks.get("bastion_embrasure"));
		var shape = helper.getBlockState(embrasure).getShape(level, helper.absolutePos(embrasure));
		helper.assertTrue(!net.minecraft.world.phys.shapes.Shapes.joinIsNotEmpty(shape,
				net.minecraft.world.phys.shapes.Shapes.box(0.4, 0.6, 0.0, 0.6, 0.65, 1.0), net.minecraft.world.phys.shapes.BooleanOp.AND),
				"An embrasure should have a slit right through it");
		var corner = blocks.get("bastion_parapet_corner").defaultBlockState();
		var north = corner.setValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING, Direction.NORTH);
		var south = corner.setValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING, Direction.SOUTH);
		helper.setBlock(new BlockPos(1, 1, 5), north);
		helper.setBlock(new BlockPos(3, 1, 5), south);
		var corner_nw = net.minecraft.world.phys.shapes.Shapes.box(0.1, 0.7, 0.1, 0.3, 0.9, 0.3);
		helper.assertTrue(net.minecraft.world.phys.shapes.Shapes.joinIsNotEmpty(helper.getBlockState(new BlockPos(1, 1, 5))
				.getShape(level, helper.absolutePos(new BlockPos(1, 1, 5))), corner_nw, net.minecraft.world.phys.shapes.BooleanOp.AND)
				&& !net.minecraft.world.phys.shapes.Shapes.joinIsNotEmpty(helper.getBlockState(new BlockPos(3, 1, 5))
						.getShape(level, helper.absolutePos(new BlockPos(3, 1, 5))), corner_nw, net.minecraft.world.phys.shapes.BooleanOp.AND),
				"The corner's merlon should stand at the north-west facing north, and turn with it");
		BlockPos left = new BlockPos(2, 1, 2);
		BlockPos right = new BlockPos(3, 1, 2);
		helper.setBlock(left, blocks.get("sliding_gate"));
		helper.setBlock(right, blocks.get("sliding_gate"));
		helper.assertBlockProperty(left, io.github.jimbozoomer.jugcraft.building.SlidingGateBlock.OPEN, false);
		helper.setBlock(new BlockPos(1, 1, 2), Blocks.REDSTONE_BLOCK);
		helper.succeedWhen(() -> {
			helper.assertBlockProperty(left, io.github.jimbozoomer.jugcraft.building.SlidingGateBlock.OPEN, true);
			helper.assertBlockProperty(right, io.github.jimbozoomer.jugcraft.building.SlidingGateBlock.OPEN, true);
			var open = helper.getBlockState(right).getCollisionShape(level, helper.absolutePos(right));
			helper.assertTrue(open.bounds().getXsize() < 0.2, "An open gate should leave only its post");
		});
	}

	/** Batch 59: a stone floor at y = 0 from (0, 0) to (x1, z1), for blocks that stand only on a solid top. */
	private static void bunkerFloor(GameTestHelper helper, int x1, int z1) {
		for (int x = 0; x <= x1; x++) {
			for (int z = 0; z <= z1; z++) {
				helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
			}
		}
	}

	/**
	 * Bunker interiors (batch 59): every block places; the periscope, curtain and bunk stand two blocks tall (placing the
	 * lower half puts up the upper one), the lamp hangs from a ceiling, the shoring is a pillar and the field kitchen has
	 * its block entity.
	 */
	@GameTest
	public void bunkerBlocksPlace(GameTestHelper helper) {
		var blocks = io.github.jimbozoomer.jugcraft.building.Bunkerworks.BLOCKS;
		ServerLevel level = helper.getLevel();
		bunkerFloor(helper, 7, 7);
		int x = 0;
		for (String id : List.of("map_table", "field_kitchen", "corrugated_iron", "corrugated_iron_slab", "corrugated_iron_stairs",
				"timber_shoring", "bunker_lamp")) {
			BlockPos pos = new BlockPos(x++, 1, 0);
			helper.setBlock(pos, blocks.get(id));
			helper.assertBlockPresent(blocks.get(id), pos);
		}
		helper.getBlockEntity(new BlockPos(1, 1, 0), io.github.jimbozoomer.jugcraft.building.FieldKitchenBlock.Entity.class);
		x = 0;
		for (String id : List.of("trench_periscope", "gas_curtain", "bunker_bunk")) {
			BlockPos lower = new BlockPos(x, 1, 3);
			x += 2;
			Block block = blocks.get(id);
			BlockState state = block.defaultBlockState().setValue(net.minecraft.world.level.block.DoublePlantBlock.HALF, DoubleBlockHalf.LOWER);
			helper.setBlock(lower, state);
			block.setPlacedBy(level, helper.absolutePos(lower), state, null, ItemStack.EMPTY);
			helper.assertBlockPresent(block, lower.above());
			helper.assertBlockProperty(lower.above(), net.minecraft.world.level.block.DoublePlantBlock.HALF, DoubleBlockHalf.UPPER);
		}
		BlockPos lamp = new BlockPos(6, 2, 6);
		helper.setBlock(lamp.above(), Blocks.STONE);
		helper.setBlock(lamp, blocks.get("bunker_lamp").defaultBlockState().setValue(net.minecraft.world.level.block.LanternBlock.HANGING, true));
		helper.assertTrue(helper.getBlockState(lamp).canSurvive(level, helper.absolutePos(lamp)), "A bunker lamp should hang from a ceiling");
		helper.assertTrue(helper.getBlockState(lamp).getLightEmission() == io.github.jimbozoomer.jugcraft.building.Bunkerworks.LAMP_LIGHT,
				"A bunker lamp should shine");
		BlockPos beam = new BlockPos(0, 1, 6);
		helper.setBlock(beam, blocks.get("timber_shoring").defaultBlockState()
				.setValue(net.minecraft.world.level.block.RotatedPillarBlock.AXIS, Direction.Axis.X));
		helper.assertBlockProperty(beam, net.minecraft.world.level.block.RotatedPillarBlock.AXIS, Direction.Axis.X);
		helper.succeed();
	}

	/**
	 * Batch 59: a trench periscope looking south counts the three zombies in front of it (not the one behind it) for a
	 * comparator, and a player looking through it marks the nearest one, as a Range Finder would.
	 */
	@GameTest(maxTicks = 120)
	public void periscopeCountsAndMarks(GameTestHelper helper) {
		Block periscope = io.github.jimbozoomer.jugcraft.building.Bunkerworks.BLOCKS.get("trench_periscope");
		BlockState state = periscope.defaultBlockState()
				.setValue(io.github.jimbozoomer.jugcraft.agriculture.TallDecorationBlock.FACING, Direction.SOUTH);
		BlockPos lower = new BlockPos(1, 1, 1);
		bunkerFloor(helper, 7, 7);
		helper.setBlock(lower, state.setValue(net.minecraft.world.level.block.DoublePlantBlock.HALF, DoubleBlockHalf.LOWER));
		helper.setBlock(lower.above(), state.setValue(net.minecraft.world.level.block.DoublePlantBlock.HALF, DoubleBlockHalf.UPPER));
		net.minecraft.world.entity.Mob nearest = helper.spawnWithNoFreeWill(net.minecraft.world.entity.EntityTypes.ZOMBIE, new BlockPos(1, 1, 5));
		helper.spawnWithNoFreeWill(net.minecraft.world.entity.EntityTypes.ZOMBIE, new BlockPos(3, 1, 6));
		helper.spawnWithNoFreeWill(net.minecraft.world.entity.EntityTypes.ZOMBIE, new BlockPos(0, 1, 7));
		helper.spawnWithNoFreeWill(net.minecraft.world.entity.EntityTypes.ZOMBIE, new BlockPos(2, 1, 0));
		helper.runAfterDelay(io.github.jimbozoomer.jugcraft.building.Bunkerworks.PERISCOPE_INTERVAL + 10, () -> {
			ServerLevel level = helper.getLevel();
			helper.assertBlockProperty(lower, io.github.jimbozoomer.jugcraft.building.TrenchPeriscopeBlock.SEEN, 3);
			helper.assertTrue(helper.getBlockState(lower).getAnalogOutputSignal(level, helper.absolutePos(lower), Direction.NORTH) == 3,
					"A comparator should read the three zombies in front, not the one behind");
			ServerPlayer spotter = helper.makeMockServerPlayerInLevel();
			helper.useBlock(lower.above(), spotter);
			BlockPos mark = io.github.jimbozoomer.jugcraft.artillery.Spotting.own(level, spotter);
			helper.assertTrue(nearest.blockPosition().equals(mark), "Looking through it should mark the nearest zombie, "
					+ nearest.blockPosition() + ", not " + mark);
			spotter.setShiftKeyDown(true);
			helper.useBlock(lower, spotter);
			helper.assertTrue(io.github.jimbozoomer.jugcraft.artillery.Spotting.own(level, spotter) == null, "Sneak-using it should clear the mark");
			spotter.discard();
			helper.succeed();
		});
	}

	/**
	 * Batch 59: a chlorine cloud on one side of a hanging gas curtain does not hurt a pig on the other side; rolled up (by
	 * using it, both halves at once), the next pulse does.
	 */
	@GameTest(maxTicks = 100)
	public void gasCurtainStopsChlorine(GameTestHelper helper) {
		Block curtain = io.github.jimbozoomer.jugcraft.building.Bunkerworks.BLOCKS.get("gas_curtain");
		BlockState state = curtain.defaultBlockState()
				.setValue(io.github.jimbozoomer.jugcraft.agriculture.TallDecorationBlock.FACING, Direction.EAST);
		BlockPos lower = new BlockPos(4, 1, 3);
		bunkerFloor(helper, 7, 7);
		helper.setBlock(lower, state.setValue(net.minecraft.world.level.block.DoublePlantBlock.HALF, DoubleBlockHalf.LOWER));
		helper.setBlock(lower.above(), state.setValue(net.minecraft.world.level.block.DoublePlantBlock.HALF, DoubleBlockHalf.UPPER));
		net.minecraft.world.entity.Mob pig = helper.spawnWithNoFreeWill(net.minecraft.world.entity.EntityTypes.PIG, new BlockPos(5, 1, 3));
		io.github.jimbozoomer.jugcraft.weapons.ChemicalCloud.spawn(helper.getLevel(), helper.absoluteVec(new Vec3(3.0, 1.5, 3.5)),
				io.github.jimbozoomer.jugcraft.weapons.ChemicalCloud.Kind.CHLORINE, null);
		// After the cloud's first two pulses the pig behind the hanging curtain is unhurt; then the curtain is rolled up.
		int pulse = io.github.jimbozoomer.jugcraft.weapons.ChemicalCloud.PULSE;
		helper.runAfterDelay(pulse * 2 + 5, () -> {
			helper.assertTrue(pig.getHealth() == pig.getMaxHealth(), "The curtain should keep the chlorine off the pig: " + pig.getHealth());
			ServerPlayer player = helper.makeMockServerPlayerInLevel();
			helper.useBlock(lower, player);
			player.discard();
			helper.assertBlockProperty(lower, io.github.jimbozoomer.jugcraft.building.GasCurtainBlock.ROLLED, true);
			helper.assertBlockProperty(lower.above(), io.github.jimbozoomer.jugcraft.building.GasCurtainBlock.ROLLED, true);
		});
		// Two pulses later, with the curtain rolled up, the chlorine has reached it.
		helper.runAfterDelay(pulse * 4 + 5, () -> {
			helper.assertTrue(pig.getHealth() < pig.getMaxHealth(), "With the curtain rolled up the chlorine should reach the pig");
			helper.succeed();
		});
	}

	/**
	 * Batch 59: a field kitchen with coal and a Cooking Pot on top lights and cooks Trench Stew from beef, a potato, a carrot
	 * and a bowl, burning one coal; one with coal but no pot stays cold and keeps its coal.
	 */
	@GameTest(maxTicks = 420)
	public void fieldKitchenHeatsThePot(GameTestHelper helper) {
		Block kitchen = io.github.jimbozoomer.jugcraft.building.Bunkerworks.BLOCKS.get("field_kitchen");
		BlockPos withPot = new BlockPos(1, 1, 1);
		BlockPos bare = new BlockPos(4, 1, 1);
		helper.setBlock(withPot, kitchen);
		helper.setBlock(bare, kitchen);
		var cooking = helper.getBlockEntity(withPot, io.github.jimbozoomer.jugcraft.building.FieldKitchenBlock.Entity.class);
		var cold = helper.getBlockEntity(bare, io.github.jimbozoomer.jugcraft.building.FieldKitchenBlock.Entity.class);
		for (var stove : List.of(cooking, cold)) {
			try (Transaction transaction = Transaction.openOuter()) {
				helper.assertTrue(stove.fuel.insert(ItemVariant.of(Items.COAL), 2, transaction) == 2, "A field kitchen should take coal");
				helper.assertTrue(stove.fuel.insert(ItemVariant.of(Items.DIRT), 1, transaction) == 0, "A field kitchen should refuse dirt");
				transaction.commit();
			}
		}
		helper.setBlock(withPot.above(), io.github.jimbozoomer.jugcraft.agriculture.JugcraftAgriculture.block("cooking_pot"));
		var pot = helper.getBlockEntity(withPot.above(), io.github.jimbozoomer.jugcraft.agriculture.CookingPotBlockEntity.class);
		pot.setItem(0, new ItemStack(Items.BOWL));
		pot.setItem(1, new ItemStack(Items.BEEF));
		pot.setItem(2, new ItemStack(Items.POTATO));
		pot.setItem(3, new ItemStack(Items.CARROT));
		helper.succeedWhen(() -> {
			ItemStack made = pot.getItem(io.github.jimbozoomer.jugcraft.agriculture.CookingPotBlockEntity.RESULT);
			helper.assertTrue(made.is(io.github.jimbozoomer.jugcraft.agriculture.JugcraftAgriculture.item("trench_stew")),
					"The pot on the field kitchen should cook Trench Stew, found " + made);
			helper.assertBlockProperty(withPot, io.github.jimbozoomer.jugcraft.building.FieldKitchenBlock.LIT, true);
			helper.assertTrue(cooking.fuelCount() == 1, "One coal should be burning, one left, but " + cooking.fuelCount() + " left");
			helper.assertBlockProperty(bare, io.github.jimbozoomer.jugcraft.building.FieldKitchenBlock.LIT, false);
			helper.assertTrue(cold.fuelCount() == 2 && cold.burn() == 0, "With no pot on top a field kitchen must not start its coal");
		});
	}

	/**
	 * Batch 59: with a mark plotted next to it, sneak-using a map table hands that mark to the fire control table within
	 * four blocks of it.
	 */
	@GameTest
	public void mapTablePlotsTargetsToFireControl(GameTestHelper helper) {
		BlockPos mapPos = new BlockPos(1, 1, 1);
		helper.setBlock(mapPos, io.github.jimbozoomer.jugcraft.building.Bunkerworks.BLOCKS.get("map_table"));
		BlockPos tablePos = new BlockPos(4, 1, 1);
		helper.setBlock(tablePos, io.github.jimbozoomer.jugcraft.building.FireControl.TABLE);
		ServerPlayer spotter = helper.makeMockServerPlayerInLevel();
		// Right beside the map table, so no other test's mark can be nearer.
		BlockPos target = helper.absolutePos(new BlockPos(1, 1, 2));
		io.github.jimbozoomer.jugcraft.artillery.Spotting.mark(spotter, target);
		ServerPlayer officer = helper.makeMockServerPlayerInLevel();
		officer.setShiftKeyDown(true);
		helper.useBlock(mapPos, officer);
		var table = helper.getBlockEntity(tablePos, io.github.jimbozoomer.jugcraft.building.FireControlTableBlock.Entity.class);
		helper.assertTrue(target.equals(table.target()), "The map table should lay the fire control table on the plotted mark, not "
				+ table.target());
		io.github.jimbozoomer.jugcraft.artillery.Spotting.clear(spotter);
		spotter.discard();
		officer.discard();
		helper.succeed();
	}
}
