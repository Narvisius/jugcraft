package io.github.jimbozoomer.jugcraft.test;

import net.minecraft.world.phys.Vec3;
import net.minecraft.server.level.ServerPlayer;
import io.github.jimbozoomer.jugcraft.gear.JugcraftGrapple;
import io.github.jimbozoomer.jugcraft.gear.GrappleHook;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.client.HandbookScreen;
import io.github.jimbozoomer.jugcraft.client.MachineScreen;
import io.github.jimbozoomer.jugcraft.client.ProspectorScreen;
import io.github.jimbozoomer.jugcraft.energy.SimpleEnergyStorage;
import io.github.jimbozoomer.jugcraft.fluid.JugcraftFluids;
import io.github.jimbozoomer.jugcraft.kinetic.BeltPulleyBlockEntity;
import io.github.jimbozoomer.jugcraft.kinetic.DynamoBlock;
import io.github.jimbozoomer.jugcraft.kinetic.ElectricMotorBlock;
import io.github.jimbozoomer.jugcraft.kinetic.ElectricMotorBlockEntity;
import io.github.jimbozoomer.jugcraft.kinetic.HandCrankBlock;
import io.github.jimbozoomer.jugcraft.kinetic.JugcraftKinetics;
import io.github.jimbozoomer.jugcraft.kinetic.ShaftBlock;
import io.github.jimbozoomer.jugcraft.kinetic.SteamEngineBlock;
import io.github.jimbozoomer.jugcraft.logistics.ConveyorBlock;
import io.github.jimbozoomer.jugcraft.logistics.ConveyorBlockEntity;
import io.github.jimbozoomer.jugcraft.logistics.ConveyorSlopeBlock;
import io.github.jimbozoomer.jugcraft.logistics.JugcraftLogistics;
import io.github.jimbozoomer.jugcraft.machine.JugcraftMachines;
import io.github.jimbozoomer.jugcraft.machine.LargeMachineBlock;
import io.github.jimbozoomer.jugcraft.machine.MachineBlock;
import io.github.jimbozoomer.jugcraft.machine.MachineBlockEntity;
import io.github.jimbozoomer.jugcraft.machine.MachineKind;
import io.github.jimbozoomer.jugcraft.prospecting.OreSurvey;
import io.github.jimbozoomer.jugcraft.tools.ChargingStationBlock;
import io.github.jimbozoomer.jugcraft.tools.ChargingStationBlockEntity;
import io.github.jimbozoomer.jugcraft.tools.JugcraftTools;
import io.github.jimbozoomer.jugcraft.world.ArcadeCabinetBlock;
import io.github.jimbozoomer.jugcraft.world.PixelHollows;
import io.github.jimbozoomer.jugcraft.world.RetroTrader;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.client.CameraType;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.Block;
import io.github.jimbozoomer.jugcraft.chemistry.FoamSprayerItem;
import io.github.jimbozoomer.jugcraft.chemistry.ConstructionChemistry;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

/**
 * Client game tests: a real game client with real rendering (CI runs it with Mesa). It builds a
 * showroom of every machine, opens a machine screen and the Engineer's Handbook, and saves
 * screenshots, so the looks can be checked from actual game renders rather than previews.
 */
public class JugcraftClientGameTests implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		if (TestFocus.skip("machines")) {
			return;
		}
		if (GuideScreenshotGameTests.active()) {
			return;
		}
		try (TestSingleplayerContext singleplayer = context.worldBuilder()
				.adjustSettings(creator -> creator.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE)).create()) {
			singleplayer.getConnection().waitForChunksRender();
			BlockPos origin = context.computeOnClient(client -> BlockPos.containing(client.player.position()));
			int x = origin.getX();
			int y = origin.getY();
			int z = origin.getZ();
			TestServerContext server = singleplayer.getServer();
			// No command feedback in chat, so it does not cover the screenshots (the rule's name differs across
			// versions; whichever does not exist just fails).
			server.runCommand("gamerule sendCommandFeedback false");
			server.runCommand("gamerule minecraft:send_command_feedback false");
			server.runCommand("time set noon");
			server.runCommand("weather clear");
			// A floor under the showroom and the scenes, with the air above it cleared. Set block by block on the
			// server: a fill command cannot reach chunks that are not loaded yet, and the multi-block row runs far
			// beyond the player.
			int end = x + 20 + largeRowLength() + 8;
			server.runOnServer(minecraft -> clearFloor(minecraft.overworld(), new BlockPos(x - 26, y, z - 10), new BlockPos(end, y + 14, z + 12)));
			server.runOnServer(minecraft -> buildShowroom(minecraft.overworld(), new BlockPos(x, y, z - 5)));

			// Hide the HUD, hand and chat so the screenshots show only the machines. In 26.3 this is the "toggle GUI"
			// key mapping (F1 by default); pressing the raw F1 key code did not reach it.
			context.getInput().pressKey(options -> options.keyToggleGui);

			// One-block machines, facing the camera, in two halves.
			server.runCommand("tp @p %d %d %d 180 25".formatted(x - 4, y, z - 2));
			context.waitTicks(40);
			singleplayer.getConnection().waitForChunksRender();
			context.takeScreenshot("jugcraft_machines_1");
			server.runCommand("tp @p %d %d %d 180 25".formatted(x + 3, y, z - 2));
			context.waitTicks(20);
			singleplayer.getConnection().waitForChunksRender();
			context.takeScreenshot("jugcraft_machines_2");

			// A running kinetic line: steam engine, shafts, a gearbox with a hand crank, a dynamo and a crusher.
			// Its own row behind the multi-block camera (the showroom rows are at z - 5).
			server.runOnServer(minecraft -> buildKineticLine(minecraft.overworld(), new BlockPos(x, y, z + 2)));
			server.runCommand("tp @p %d %d %d 180 35".formatted(x + 2, y + 2, z + 7));
			context.waitTicks(60);
			singleplayer.getConnection().waitForChunksRender();
			context.takeScreenshot("jugcraft_kinetics");

			// An electric motor driving a pulley, belted up to a second pulley that drives a crusher through a shaft.
			server.runOnServer(minecraft -> buildBeltLine(minecraft.overworld(), new BlockPos(x + 12, y, z + 2)));
			server.runCommand("tp @p %d %d %d 180 5".formatted(x + 14, y + 1, z + 7));
			context.waitTicks(60);
			singleplayer.getConnection().waitForChunksRender();
			context.takeScreenshot("jugcraft_belts");

			// A conveyor line with items riding it, a splitter at the end and a chest on each of its outputs.
			server.runOnServer(minecraft -> buildConveyorLine(minecraft.overworld(), new BlockPos(x + 6, y, z + 2)));
			server.runCommand("tp @p %d %d %d 180 40".formatted(x + 8, y + 2, z + 5));
			context.waitTicks(10);
			singleplayer.getConnection().waitForChunksRender();
			context.takeScreenshot("jugcraft_conveyors");

			// Conveyor slopes: items climb onto a raised conveyor and come back down.
			server.runOnServer(minecraft -> buildSlopeLine(minecraft.overworld(), new BlockPos(x + 5, y, z + 4)));
			server.runCommand("tp @p %d %d %d 150 25".formatted(x + 11, y + 2, z + 8));
			context.waitTicks(12);
			singleplayer.getConnection().waitForChunksRender();
			context.takeScreenshot("jugcraft_conveyor_slopes");

			// Powered tools: three charging stations holding the drill, the chainsaw and the rocket pack.
			server.runOnServer(minecraft -> buildToolStations(minecraft.overworld(), new BlockPos(x - 8, y, z + 2)));
			server.runCommand("tp @p %d %d %d 180 10".formatted(x - 6, y, z + 6));
			context.waitTicks(20);
			singleplayer.getConnection().waitForChunksRender();
			context.takeScreenshot("jugcraft_charging_stations");

			// The drill in hand (first person, with the hotbar showing the three tools), then the worn rocket pack
			// from behind.
			server.runCommand("item replace entity @p hotbar.0 with jugcraft:mining_drill");
			server.runCommand("item replace entity @p hotbar.1 with jugcraft:chainsaw");
			server.runCommand("item replace entity @p hotbar.2 with jugcraft:rocket_pack");
			server.runCommand("item replace entity @p armor.chest with jugcraft:rocket_pack");
			server.runCommand("tp @p %d %d %d 180 20".formatted(x - 6, y, z + 5));
			// Show the HUD again (the first hotbar slot, the drill, is selected in a new world).
			context.getInput().pressKey(options -> options.keyToggleGui);
			context.waitTicks(20);
			context.takeScreenshot("jugcraft_drill_in_hand");
			context.runOnClient(client -> client.options.setCameraType(CameraType.THIRD_PERSON_BACK));
			context.waitTicks(20);
			context.takeScreenshot("jugcraft_rocket_pack_worn");
			// Batch 25: steel armor (the 3D knight armor, docs/features/knight-armor.md) with a steel paxel in hand, from the
			// front; then bronze, from the front and from behind. KnightArmorClientGameTests shoots both from every side.
			server.runCommand("item replace entity @p armor.head with jugcraft:steel_helmet");
			server.runCommand("item replace entity @p armor.chest with jugcraft:steel_chestplate");
			server.runCommand("item replace entity @p armor.legs with jugcraft:steel_leggings");
			server.runCommand("item replace entity @p armor.feet with jugcraft:steel_boots");
			server.runCommand("item replace entity @p hotbar.0 with jugcraft:steel_paxel");
			context.runOnClient(client -> client.options.setCameraType(CameraType.THIRD_PERSON_FRONT));
			context.waitTicks(20);
			context.takeScreenshot("jugcraft_steel_armor_worn");
			server.runCommand("item replace entity @p armor.head with jugcraft:bronze_helmet");
			server.runCommand("item replace entity @p armor.chest with jugcraft:bronze_chestplate");
			server.runCommand("item replace entity @p armor.legs with jugcraft:bronze_leggings");
			server.runCommand("item replace entity @p armor.feet with jugcraft:bronze_boots");
			context.waitTicks(20);
			context.takeScreenshot("jugcraft_bronze_armor_worn");
			context.runOnClient(client -> client.options.setCameraType(CameraType.THIRD_PERSON_BACK));
			context.waitTicks(20);
			context.takeScreenshot("jugcraft_bronze_armor_back");
			// Batch 27: scuba mask and tank, free runners and the power katana, from the front; then the sixteen plastic
			// blocks in a wall, two high.
			server.runCommand("item replace entity @p armor.head with jugcraft:scuba_mask");
			server.runCommand("item replace entity @p armor.chest with jugcraft:scuba_tank");
			server.runCommand("item replace entity @p armor.legs with minecraft:air");
			server.runCommand("item replace entity @p armor.feet with jugcraft:free_runners");
			server.runCommand("item replace entity @p hotbar.0 with jugcraft:power_katana");
			context.runOnClient(client -> client.options.setCameraType(CameraType.THIRD_PERSON_FRONT));
			context.waitTicks(20);
			context.takeScreenshot("jugcraft_scuba_gear_worn");
			// Batch 28: the powered exosuit, Vanguard from the front and behind, then Ronin with its katana.
			for (String[] slot : new String[][] {{"head", "helmet"}, {"chest", "chestplate"}, {"legs", "leggings"},
					{"feet", "boots"}}) {
				server.runCommand("item replace entity @p armor.%s with jugcraft:exosuit_%s".formatted(slot[0], slot[1]));
			}
			server.runCommand("item replace entity @p hotbar.0 with jugcraft:power_katana");
			context.waitTicks(20);
			context.takeScreenshot("jugcraft_exosuit_vanguard");
			context.runOnClient(client -> client.options.setCameraType(CameraType.THIRD_PERSON_BACK));
			context.waitTicks(20);
			context.takeScreenshot("jugcraft_exosuit_vanguard_back");
			for (String[] slot : new String[][] {{"head", "helmet"}, {"chest", "chestplate"}, {"legs", "leggings"},
					{"feet", "boots"}}) {
				server.runCommand("item replace entity @p armor.%s with jugcraft:ronin_exosuit_%s".formatted(slot[0], slot[1]));
			}
			server.runCommand("item replace entity @p hotbar.0 with jugcraft:ronin_katana");
			context.runOnClient(client -> client.options.setCameraType(CameraType.THIRD_PERSON_FRONT));
			context.waitTicks(20);
			context.takeScreenshot("jugcraft_exosuit_ronin");
			// Batch 30: the pneumatic grapple in hand, its hook hanging out on its slack line beside the player.
			server.runCommand("item replace entity @p hotbar.0 with jugcraft:pneumatic_grapple");
			server.runOnServer(minecraft -> {
				ServerPlayer owner = minecraft.getPlayerList().getPlayers().get(0);
				Vec3 look = owner.getLookAngle();
				GrappleHook hook = new GrappleHook(minecraft.overworld(), owner, new ItemStack(JugcraftGrapple.PNEUMATIC_GRAPPLE));
				// On the player's right, which the front camera shows on the left, clear of the advancement toasts.
				hook.setPos(owner.getX() + look.x * 1.5 - look.z * 1.6, owner.getY() + 1.4, owner.getZ() + look.z * 1.5 + look.x * 1.6);
				hook.setNoGravity(true);
				hook.setDeltaMovement(Vec3.ZERO);
				minecraft.overworld().addFreshEntity(hook);
			});
			context.waitTicks(10);
			context.takeScreenshot("jugcraft_pneumatic_grapple");
			server.runCommand("kill @e[type=jugcraft:grapple_hook]");
			context.runOnClient(client -> client.options.setCameraType(CameraType.FIRST_PERSON));
			String[] plastics = {"white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray", "light_gray",
					"cyan", "purple", "blue", "brown", "green", "red", "black"};
			for (int i = 0; i < plastics.length; i++) {
				server.runCommand("fill %d %d %d %d %d %d jugcraft:%s_plastic".formatted(x - 14 + i, y, z + 12, x - 14 + i, y + 1,
						z + 12, plastics[i]));
			}
			server.runCommand("tp @p %d %d %d 180 10".formatted(x - 6, y, z + 18));
			context.waitTicks(20);
			singleplayer.getConnection().waitForChunksRender();
			context.takeScreenshot("jugcraft_plastic_blocks");
			// Batch 32: a trench bridged with sprayed construction foam, a foam blob set against a wall, and concrete and
			// blast-proof concrete with their slabs and stairs; the foam sprayer in hand.
			server.runCommand("item replace entity @p hotbar.0 with jugcraft:foam_sprayer");
			server.runOnServer(minecraft -> {
				ServerLevel level = minecraft.overworld();
				BlockPos base = new BlockPos(x - 24, y, z + 7);
				for (int dx = 2; dx <= 4; dx++) {
					for (int dz = -1; dz <= 1; dz++) {
						for (int dy = 1; dy <= 3; dy++) {
							level.setBlockAndUpdate(base.offset(dx, -dy, dz), Blocks.AIR.defaultBlockState());
						}
						level.setBlockAndUpdate(base.offset(dx, -4, dz), Blocks.WATER.defaultBlockState());
					}
				}
				ServerPlayer player = minecraft.getPlayerList().getPlayers().get(0);
				for (BlockPos pos : FoamSprayerItem.fill(level, player, base.offset(3, -1, 0), ConstructionChemistry.SPRAY_BLOCKS)) {
					level.setBlockAndUpdate(pos, ConstructionChemistry.CONSTRUCTION_FOAM.defaultBlockState());
				}
				// The drone tower's Steel Armor Plate and Hazard Plating end the row, so their clean 32 px plates show beside
				// the blast-proof concrete; a hazard block caps the stacked armour, the way the tower rims its armour pads.
				String[] row = {"concrete", "concrete_slab", "concrete_stairs", "blastproof_concrete", "blastproof_concrete_slab",
						"blastproof_concrete_stairs", "steel_armor_plate", "steel_armor_plate_slab", "steel_armor_plate_stairs",
						"hazard_plating", "hazard_plating_slab"};
				for (int i = 0; i < row.length; i++) {
					Block block = BuiltInRegistries.BLOCK.getValue(Jugcraft.id(row[i]));
					level.setBlockAndUpdate(base.offset(6 + i, 0, -1), block.defaultBlockState());
				}
				level.setBlockAndUpdate(base.offset(6, 1, -1), ConstructionChemistry.BLASTPROOF_CONCRETE.defaultBlockState());
				level.setBlockAndUpdate(base.offset(9, 1, -1), ConstructionChemistry.CONSTRUCTION_FOAM.defaultBlockState());
				level.setBlockAndUpdate(base.offset(12, 1, -1),
						BuiltInRegistries.BLOCK.getValue(Jugcraft.id("steel_armor_plate")).defaultBlockState());
				level.setBlockAndUpdate(base.offset(12, 2, -1),
						BuiltInRegistries.BLOCK.getValue(Jugcraft.id("hazard_plating")).defaultBlockState());
			});
			// Back from the scene and a little to the left, looking slightly down; wait for the advancement toasts to go.
			server.runCommand("tp @p %d %d %d 190 22".formatted(x - 17, y + 1, z + 12));
			context.waitTicks(140);
			singleplayer.getConnection().waitForChunksRender();
			context.takeScreenshot("jugcraft_foam_sprayer");
			context.runOnClient(client -> client.options.setCameraType(CameraType.FIRST_PERSON));
			context.getInput().pressKey(options -> options.keyToggleGui);
			server.runCommand("clear @p");

			// Power gear in the electric look: a line of glowing cables from a solar panel through a battery box, a
			// capacitor bank and a charging station to an electric motor turning a dynamo, and an electric pump.
			server.runOnServer(minecraft -> buildPowerGear(minecraft.overworld(), new BlockPos(x - 24, y, z + 2)));
			server.runCommand("tp @p %d %d %d 180 18".formatted(x - 17, y + 2, z + 9));
			context.waitTicks(40);
			singleplayer.getConnection().waitForChunksRender();
			context.takeScreenshot("jugcraft_power_gear");

			// Batch 37: a control room. Two battery boxes with sensors (red, blue), a logic controller and a relay on a
			// data cable, an alarm sounding, and a 3x2 control monitor showing the channels; the remote in hand.
			server.runCommand("item replace entity @p hotbar.0 with jugcraft:control_remote");
			server.runOnServer(minecraft -> buildControlRoom(minecraft.overworld(), new BlockPos(x - 24, y, z - 10)));
			server.runCommand("tp @p %d %d %d 180 6".formatted(x - 21, y + 1, z - 3));
			context.waitTicks(120);
			singleplayer.getConnection().waitForChunksRender();
			context.takeScreenshot("jugcraft_control_room");
			server.runCommand("clear @p");

			// Rocketry (batches 38-43), where the control room was: a wall of item frames with the rockets, the rocket
			// workshop, cryogenic liquefier and a rocket pad, a zipline strung from the top of the wall, and a powered
			// booster rail line with a minecart; the rocket launcher in hand.
			server.runCommand("fill %d %d %d %d %d %d minecraft:air".formatted(x - 26, y, z - 10, x - 10, y + 6, z + 1));
			server.runOnServer(minecraft -> buildRocketry(minecraft.overworld(), new BlockPos(x - 24, y, z - 10)));
			String[] rockets = {"survey_rocket", "signal_flare", "delivery_rocket", "line_rocket", "he_rocket", "homing_rocket",
					"kerosene_tank", "lox_tank", "rocket_motor", "flight_plan"};
			for (int i = 0; i < rockets.length; i++) {
				server.runCommand("summon minecraft:item_frame %d %d %d {Facing:3b,Fixed:1b,Item:{id:\"jugcraft:%s\",count:1}}"
						.formatted(x - 23 + i % 5 * 2, y + 1 + i / 5, z - 9, rockets[i]));
			}
			server.runCommand("item replace entity @p hotbar.0 with jugcraft:rocket_launcher");
			server.runCommand("tp @p %d %d %d 180 10".formatted(x - 18, y + 1, z - 2));
			context.waitTicks(80);
			singleplayer.getConnection().waitForChunksRender();
			context.takeScreenshot("jugcraft_rocketry");
			server.runCommand("clear @p");

			// Dieselworks (batch 45), in the same spot: a riveted wall with portholes and a dome-plate cornice, a
			// grating catwalk on I-beams and amber cage lamps.
			server.runCommand("kill @e[type=minecraft:item_frame]");
			server.runCommand("kill @e[type=minecraft:minecart]");
			server.runCommand("fill %d %d %d %d %d %d minecraft:air".formatted(x - 26, y, z - 10, x - 10, y + 6, z + 1));
			server.runOnServer(minecraft -> buildDieselworks(minecraft.overworld(), new BlockPos(x - 24, y, z - 10)));
			server.runCommand("tp @p %d %d %d 180 4".formatted(x - 19, y + 1, z - 2));
			context.waitTicks(40);
			singleplayer.getConnection().waitForChunksRender();
			context.takeScreenshot("jugcraft_dieselworks");

			// The zeppelin (batch 46), moored over the Dieselworks wall, seen from further back.
			server.runOnServer(minecraft -> {
				ServerLevel overworld = minecraft.overworld();
				io.github.jimbozoomer.jugcraft.airship.Zeppelin zeppelin = new io.github.jimbozoomer.jugcraft.airship.Zeppelin(
						io.github.jimbozoomer.jugcraft.airship.JugcraftAirships.ZEPPELIN, overworld);
				zeppelin.snapTo(x - 18.5, y + 7, z - 14.5, 90.0F, 0.0F);
				zeppelin.setFuel(io.github.jimbozoomer.jugcraft.airship.Zeppelin.FUEL_TANK);
				overworld.addFreshEntity(zeppelin);
			});
			server.runCommand("tp @p %d %d %d 180 -12".formatted(x - 19, y + 2, z + 9));
			context.waitTicks(40);
			singleplayer.getConnection().waitForChunksRender();
			context.takeScreenshot("jugcraft_zeppelin");

			// The Diesel Walker (batch 47), standing in front of the Dieselworks wall and facing the camera.
			server.runOnServer(minecraft -> {
				ServerLevel overworld = minecraft.overworld();
				io.github.jimbozoomer.jugcraft.walker.DieselWalker walker = new io.github.jimbozoomer.jugcraft.walker.DieselWalker(
						io.github.jimbozoomer.jugcraft.walker.JugcraftWalkers.DIESEL_WALKER, overworld);
				walker.snapTo(x - 18.5, y + 1, z - 3.5, 0.0F, 0.0F);
				overworld.addFreshEntity(walker);
			});
			server.runCommand("tp @p %d %d %d 180 8".formatted(x - 19, y + 2, z + 4));
			context.waitTicks(40);
			singleplayer.getConnection().waitForChunksRender();
			context.takeScreenshot("jugcraft_diesel_walker");

			// Kaiserworks (batch 48), in the same spot: an imperial station front of marble columns, black lacquer,
			// leaded glass, a gilt frieze and the crest, on station tiles with gas lamps.
			server.runCommand("kill @e[type=jugcraft:diesel_walker]");
			server.runCommand("kill @e[type=jugcraft:zeppelin]");
			server.runCommand("fill %d %d %d %d %d %d minecraft:air".formatted(x - 26, y, z - 10, x - 10, y + 8, z + 1));
			server.runOnServer(minecraft -> buildKaiserworks(minecraft.overworld(), new BlockPos(x - 24, y, z - 10)));
			server.runCommand("tp @p %d %d %d 180 6".formatted(x - 19, y + 2, z - 1));
			context.waitTicks(40);
			singleplayer.getConnection().waitForChunksRender();
			context.takeScreenshot("jugcraft_kaiserworks");

			// The Landship (batch 49), on the station tiles in front of the Kaiserworks front, turned to show its side.
			server.runOnServer(minecraft -> {
				ServerLevel overworld = minecraft.overworld();
				io.github.jimbozoomer.jugcraft.landship.Landship landship = new io.github.jimbozoomer.jugcraft.landship.Landship(
						io.github.jimbozoomer.jugcraft.landship.JugcraftLandships.LANDSHIP, overworld);
				landship.snapTo(x - 18.5, y, z - 6.5, -60.0F, 0.0F);
				overworld.addFreshEntity(landship);
			});
			server.runCommand("tp @p %d %d %d 180 12".formatted(x - 19, y + 3, z + 2));
			context.waitTicks(40);
			singleplayer.getConnection().waitForChunksRender();
			context.takeScreenshot("jugcraft_landship");

			// Trench works (batch 50), in the same spot: a revetted trench with duckboards, a sandbag parapet, barbed wire
			// out front, a field telephone and a searchlight sweeping across.
			server.runCommand("kill @e[type=jugcraft:landship]");
			server.runCommand("fill %d %d %d %d %d %d minecraft:air".formatted(x - 26, y, z - 10, x - 10, y + 8, z + 1));
			server.runOnServer(minecraft -> buildTrench(minecraft.overworld(), new BlockPos(x - 24, y, z - 10)));
			server.runCommand("tp @p %d %d %d 180 20".formatted(x - 19, y + 3, z));
			context.waitTicks(40);
			singleplayer.getConnection().waitForChunksRender();
			context.takeScreenshot("jugcraft_trench_works");

			// Big guns (batch 51), behind the trench: the siege mortar, the self-propelled howitzer, a flak gun and an
			// observation balloon hanging above, seen from further back.
			server.runOnServer(minecraft -> {
				ServerLevel overworld = minecraft.overworld();
				io.github.jimbozoomer.jugcraft.artillery.SiegeMortar mortar = new io.github.jimbozoomer.jugcraft.artillery.SiegeMortar(
						io.github.jimbozoomer.jugcraft.artillery.JugcraftArtillery.SIEGE_MORTAR, overworld);
				mortar.snapTo(x - 23.5, y, z - 3.5, 0.0F, 0.0F);
				mortar.face(160.0F);
				overworld.addFreshEntity(mortar);
				io.github.jimbozoomer.jugcraft.artillery.SelfPropelledHowitzer howitzer = new io.github.jimbozoomer.jugcraft.artillery.SelfPropelledHowitzer(
						io.github.jimbozoomer.jugcraft.artillery.JugcraftArtillery.HOWITZER, overworld);
				howitzer.snapTo(x - 15.5, y, z - 2.5, 0.0F, 0.0F);
				howitzer.face(150.0F);
				overworld.addFreshEntity(howitzer);
				io.github.jimbozoomer.jugcraft.artillery.FlakGun flak = new io.github.jimbozoomer.jugcraft.artillery.FlakGun(
						io.github.jimbozoomer.jugcraft.artillery.JugcraftArtillery.FLAK_GUN, overworld);
				flak.snapTo(x - 19.5, y, z + 0.5, 0.0F, 0.0F);
				flak.face(200.0F);
				overworld.addFreshEntity(flak);
				io.github.jimbozoomer.jugcraft.artillery.ObservationBalloon balloon = new io.github.jimbozoomer.jugcraft.artillery.ObservationBalloon(
						io.github.jimbozoomer.jugcraft.artillery.JugcraftArtillery.BALLOON, overworld);
				balloon.snapTo(x - 12.5, y + 3, z - 12.5, 90.0F, 0.0F);
				overworld.addFreshEntity(balloon);
			});
			server.runCommand("tp @p %d %d %d 200 15".formatted(x - 21, y + 5, z + 9));
			context.waitTicks(60);
			singleplayer.getConnection().waitForChunksRender();
			context.takeScreenshot("jugcraft_big_guns");

			// Tower guns (batch 54), each on its own stone-brick tower in a row: the Grand Mortar, the Bastion Mortar, the
			// Fortress Rifle, the Bastion Autocannon and the Triple Battery, seen from behind and above. They stand well west
			// of everything else (from x - 80 to x - 44), so the machine rows that later views open stay untouched.
			for (String type : new String[] {"siege_mortar", "self_propelled_howitzer", "flak_gun", "observation_balloon"}) {
				server.runCommand("kill @e[type=jugcraft:" + type + "]");
			}
			server.runCommand("fill %d %d %d %d %d %d minecraft:air".formatted(x - 80, y, z - 12, x - 44, y + 12, z + 2));
			server.runOnServer(minecraft -> {
				ServerLevel overworld = minecraft.overworld();
				String[] guns = {"grand_mortar", "bastion_mortar", "fortress_rifle", "bastion_autocannon", "triple_battery"};
				int[] heights = {6, 4, 5, 3, 4};
				float[] facing = {160.0F, 200.0F, 150.0F, 210.0F, 170.0F};
				int cx = x - 76;
				for (int i = 0; i < guns.length; i++) {
					var type = io.github.jimbozoomer.jugcraft.artillery.JugcraftTowerGuns.type(guns[i]);
					int half = io.github.jimbozoomer.jugcraft.artillery.JugcraftTowerGuns.specOf(type).footprint() / 2;
					cx += half;
					for (int dx = -half; dx <= half; dx++) {
						for (int dz = -half; dz <= half; dz++) {
							for (int dy = 0; dy < heights[i]; dy++) {
								overworld.setBlockAndUpdate(new BlockPos(cx + dx, y + dy, z - 6 + dz), Blocks.STONE_BRICKS.defaultBlockState());
							}
						}
					}
					io.github.jimbozoomer.jugcraft.artillery.TowerGun gun = new io.github.jimbozoomer.jugcraft.artillery.TowerGun(type, overworld);
					gun.snapTo(cx + 0.5, y + heights[i], z - 6 + 0.5, 0.0F, 0.0F);
					gun.face(facing[i]);
					overworld.addFreshEntity(gun);
					cx += half + 2;
				}
			});
			server.runCommand("tp @p %d %d %d 180 22".formatted(x - 62, y + 11, z + 14));
			context.waitTicks(60);
			singleplayer.getConnection().waitForChunksRender();
			context.takeScreenshot("jugcraft_tower_guns");

			// Fortifications (batch 55), west of the tower guns: a 7x7 bastion-concrete tower carrying a Grand Mortar inside a
			// ring of parapets, a steel ladder and a blast door on its south face, an ammo hoist up its east side feeding a
			// stocked ready rack, and a bastion wall in front.
			server.runCommand("fill %d %d %d %d %d %d minecraft:air".formatted(x - 112, y, z - 14, x - 84, y + 14, z + 10));
			server.runOnServer(minecraft -> {
				ServerLevel overworld = minecraft.overworld();
				var blocks = io.github.jimbozoomer.jugcraft.building.Fortifications.BLOCKS;
				int cx = x - 98;
				int cz = z - 6;
				int h = 6;
				for (int dx = -3; dx <= 3; dx++) {
					for (int dz = -3; dz <= 3; dz++) {
						for (int dy = 0; dy < h; dy++) {
							overworld.setBlockAndUpdate(new BlockPos(cx + dx, y + dy, cz + dz), blocks.get("bastion_concrete").defaultBlockState());
						}
						if (Math.abs(dx) == 3 && Math.abs(dz) == 3) {
							// Corner merlons, turned so each stands on its outside corner.
							net.minecraft.core.Direction corner = dz < 0 ? (dx < 0 ? net.minecraft.core.Direction.NORTH : net.minecraft.core.Direction.EAST)
									: (dx > 0 ? net.minecraft.core.Direction.SOUTH : net.minecraft.core.Direction.WEST);
							overworld.setBlockAndUpdate(new BlockPos(cx + dx, y + h, cz + dz), blocks.get("bastion_parapet_corner").defaultBlockState()
									.setValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING, corner));
						} else if (Math.abs(dx) == 3 || Math.abs(dz) == 3) {
							net.minecraft.core.Direction out = Math.abs(dz) == 3 ? (dz > 0 ? net.minecraft.core.Direction.SOUTH : net.minecraft.core.Direction.NORTH)
									: (dx > 0 ? net.minecraft.core.Direction.EAST : net.minecraft.core.Direction.WEST);
							overworld.setBlockAndUpdate(new BlockPos(cx + dx, y + h, cz + dz), blocks.get("bastion_parapet").defaultBlockState()
									.setValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING, out));
						}
					}
				}
				for (int dy = 0; dy < h; dy++) {
					overworld.setBlockAndUpdate(new BlockPos(cx + 2, y + dy, cz + 4), blocks.get("steel_ladder").defaultBlockState()
							.setValue(net.minecraft.world.level.block.LadderBlock.FACING, net.minecraft.core.Direction.SOUTH));
				}
				var door = blocks.get("blast_door").defaultBlockState().setValue(net.minecraft.world.level.block.DoorBlock.FACING,
						net.minecraft.core.Direction.SOUTH);
				overworld.setBlock(new BlockPos(cx - 1, y, cz + 3), door.setValue(net.minecraft.world.level.block.DoorBlock.HALF,
						net.minecraft.world.level.block.state.properties.DoubleBlockHalf.LOWER), 3);
				overworld.setBlock(new BlockPos(cx - 1, y + 1, cz + 3), door.setValue(net.minecraft.world.level.block.DoorBlock.HALF,
						net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER), 3);
				for (int dy = 0; dy <= h; dy++) {
					overworld.setBlockAndUpdate(new BlockPos(cx + 4, y + dy, cz), blocks.get("ammo_hoist").defaultBlockState());
				}
				BlockPos rackPos = new BlockPos(cx + 4, y + h + 1, cz);
				overworld.setBlockAndUpdate(rackPos, blocks.get("ready_rack").defaultBlockState()
						.setValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING, net.minecraft.core.Direction.SOUTH));
				if (overworld.getBlockEntity(rackPos) instanceof io.github.jimbozoomer.jugcraft.building.ReadyRackBlock.Entity rack) {
					for (int slot = 0; slot < rack.shells.getContainerSize(); slot++) {
						rack.shells.setItem(slot, new ItemStack(io.github.jimbozoomer.jugcraft.artillery.JugcraftTowerGuns.GREAT_SHELL_ITEM, 4));
					}
				}
				// Gun slits in the tower's south face, and the wall in front with a sliding gate in its middle.
				for (int dx = 1; dx <= 2; dx++) {
					overworld.setBlockAndUpdate(new BlockPos(cx + dx, y + 3, cz + 3), blocks.get("bastion_embrasure").defaultBlockState()
							.setValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING, net.minecraft.core.Direction.SOUTH));
				}
				for (int dx = -6; dx <= 6; dx++) {
					if (Math.abs(dx) <= 1) {
						for (int dy = 0; dy < 2; dy++) {
							overworld.setBlockAndUpdate(new BlockPos(cx + dx, y + dy, cz + 7), blocks.get("sliding_gate").defaultBlockState()
									.setValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING, net.minecraft.core.Direction.SOUTH));
						}
					} else {
						overworld.setBlockAndUpdate(new BlockPos(cx + dx, y, cz + 7), blocks.get("bastion_concrete_wall").defaultBlockState());
					}
				}
				var type = io.github.jimbozoomer.jugcraft.artillery.JugcraftTowerGuns.type("grand_mortar");
				io.github.jimbozoomer.jugcraft.artillery.TowerGun gun = new io.github.jimbozoomer.jugcraft.artillery.TowerGun(type, overworld);
				gun.snapTo(cx + 0.5, y + h, cz + 0.5, 0.0F, 0.0F);
				gun.face(200.0F);
				overworld.addFreshEntity(gun);
			});
			server.runCommand("tp @p %d %d %d 168 6".formatted(x - 95, y + 6, z + 18));
			context.waitTicks(60);
			singleplayer.getConnection().waitForChunksRender();
			context.takeScreenshot("jugcraft_fortifications");

			// Bunker and trench interiors (batch 59), where fire control stands next: a dugout under a corrugated iron roof on
			// timber shoring, lit by hanging bunker lamps, with a map table, a lit field kitchen with a cooking pot on it, a
			// bunk, a gas curtain let down in the back doorway and one rolled up in the side doorway; outside to the west, a
			// trench periscope looking over a sandbag parapet. Fire control's fill clears it all again.
			server.runCommand("fill %d %d %d %d %d %d minecraft:air".formatted(x - 144, y, z - 14, x - 114, y + 10, z + 10));
			server.runOnServer(minecraft -> buildBunker(minecraft.overworld(), new BlockPos(x - 136, y, z - 8)));
			// Three blocks in front of the open south side, looking in and a little down, so the interior fills the picture.
			server.runCommand("tp @p %d %d %d 180 14".formatted(x - 132, y, z - 1));
			context.waitTicks(40);
			singleplayer.getConnection().waitForChunksRender();
			context.takeScreenshot("jugcraft_bunker");

			// Fire control (batch 56), west of the fortifications: a fire control table on converge directing three Bastion
			// Mortars on stone-brick plinths, each with a stocked ready rack, all laid on the table's target to the north.
			server.runCommand("fill %d %d %d %d %d %d minecraft:air".formatted(x - 144, y, z - 14, x - 114, y + 10, z + 10));
			server.runOnServer(minecraft -> {
				ServerLevel overworld = minecraft.overworld();
				BlockPos tablePos = new BlockPos(x - 129, y, z - 2);
				overworld.setBlockAndUpdate(tablePos, io.github.jimbozoomer.jugcraft.building.FireControl.TABLE.defaultBlockState()
						.setValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING, net.minecraft.core.Direction.NORTH)
						.setValue(io.github.jimbozoomer.jugcraft.building.FireControlTableBlock.MODE,
								io.github.jimbozoomer.jugcraft.building.FireControlTableBlock.Mode.CONVERGE));
				if (!(overworld.getBlockEntity(tablePos) instanceof io.github.jimbozoomer.jugcraft.building.FireControlTableBlock.Entity table)) {
					return;
				}
				table.setTarget(new BlockPos(x - 129, y, z - 90));
				var type = io.github.jimbozoomer.jugcraft.artillery.JugcraftTowerGuns.type("bastion_mortar");
				int[][] plinths = {{x - 137, z - 8}, {x - 129, z - 10}, {x - 121, z - 8}};
				for (int[] at : plinths) {
					for (int dx = -1; dx <= 1; dx++) {
						for (int dz = -1; dz <= 1; dz++) {
							for (int dy = 0; dy < 2; dy++) {
								overworld.setBlockAndUpdate(new BlockPos(at[0] + dx, y + dy, at[1] + dz), Blocks.STONE_BRICKS.defaultBlockState());
							}
						}
					}
					BlockPos rackPos = new BlockPos(at[0] + 2, y + 2, at[1] + 1);
					overworld.setBlockAndUpdate(new BlockPos(at[0] + 2, y + 1, at[1] + 1), Blocks.STONE_BRICKS.defaultBlockState());
					overworld.setBlockAndUpdate(new BlockPos(at[0] + 2, y, at[1] + 1), Blocks.STONE_BRICKS.defaultBlockState());
					overworld.setBlockAndUpdate(rackPos, io.github.jimbozoomer.jugcraft.building.Fortifications.BLOCKS.get("ready_rack").defaultBlockState()
							.setValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING, net.minecraft.core.Direction.SOUTH));
					if (overworld.getBlockEntity(rackPos) instanceof io.github.jimbozoomer.jugcraft.building.ReadyRackBlock.Entity rack) {
						rack.shells.addItem(new ItemStack(io.github.jimbozoomer.jugcraft.artillery.JugcraftArtillery.HEAVY_SHELL_ITEM, 12));
					}
					io.github.jimbozoomer.jugcraft.artillery.TowerGun gun = new io.github.jimbozoomer.jugcraft.artillery.TowerGun(type, overworld);
					gun.snapTo(at[0] + 0.5, y + 2, at[1] + 0.5, 0.0F, 0.0F);
					gun.face(150.0F);
					overworld.addFreshEntity(gun);
					table.link(gun.getUUID());
					gun.linkDirector(overworld, tablePos, table);
				}
			});
			server.runCommand("tp @p %d %d %d 180 15".formatted(x - 129, y + 4, z + 5));
			context.waitTicks(100);
			singleplayer.getConnection().waitForChunksRender();
			context.takeScreenshot("jugcraft_fire_control");

			// The raider faction (batch 57), west of fire control: a grunt, a grenadier and an officer in front, a raider
			// walker behind them and a blimp overhead, all standing still (no AI) facing the camera.
			server.runCommand("fill %d %d %d %d %d %d minecraft:air".formatted(x - 176, y, z - 16, x - 148, y + 14, z + 10));
			server.runOnServer(minecraft -> {
				ServerLevel overworld = minecraft.overworld();
				var types = new net.minecraft.world.entity.EntityType<?>[] {io.github.jimbozoomer.jugcraft.raiders.JugcraftRaiders.GRUNT,
						io.github.jimbozoomer.jugcraft.raiders.JugcraftRaiders.OFFICER, io.github.jimbozoomer.jugcraft.raiders.JugcraftRaiders.GRENADIER,
						io.github.jimbozoomer.jugcraft.raiders.JugcraftRaiders.WALKER, io.github.jimbozoomer.jugcraft.raiders.JugcraftRaiders.BLIMP};
				double[][] at = {{x - 164.5, y, z - 2.5}, {x - 162.5, y, z - 3.5}, {x - 160.5, y, z - 2.5}, {x - 162.5, y, z - 8.5},
						{x - 158.5, y + 7, z - 12.5}};
				float[] yaw = {-10.0F, 0.0F, 10.0F, -5.0F, 30.0F};
				for (int i = 0; i < types.length; i++) {
					if (types[i].create(overworld, net.minecraft.world.entity.EntitySpawnReason.COMMAND) instanceof net.minecraft.world.entity.Mob mob) {
						mob.snapTo(at[i][0], at[i][1], at[i][2], yaw[i], 0.0F);
						mob.setYBodyRot(yaw[i]);
						mob.setYHeadRot(yaw[i]);
						mob.setNoAi(true);
						mob.setPersistenceRequired();
						overworld.addFreshEntity(mob);
					}
				}
			});
			server.runCommand("tp @p %d %d %d 180 -6".formatted(x - 162, y + 2, z + 4));
			context.waitTicks(40);
			singleplayer.getConnection().waitForChunksRender();
			context.takeScreenshot("jugcraft_raiders");

			// The Armoured Walker (batch 58, the owner's Blender model) beside the raiders' walker in their paint.
			server.runOnServer(minecraft -> {
				ServerLevel overworld = minecraft.overworld();
				io.github.jimbozoomer.jugcraft.walker.ArmouredWalker walker = new io.github.jimbozoomer.jugcraft.walker.ArmouredWalker(
						io.github.jimbozoomer.jugcraft.walker.JugcraftWalkers.ARMOURED_WALKER, overworld);
				walker.snapTo(x - 167.5, y, z - 6.5, -20.0F, 0.0F);
				overworld.addFreshEntity(walker);
			});
			server.runCommand("tp @p %d %d %d 200 2".formatted(x - 170, y + 3, z + 2));
			context.waitTicks(30);
			singleplayer.getConnection().waitForChunksRender();
			context.takeScreenshot("jugcraft_armoured_walker");
			server.runCommand("kill @e[type=jugcraft:armoured_walker]");

			// The Howitzer Walker (the owner's model of 9 October 2026) where the Armoured Walker stood, its gun at rest.
			server.runOnServer(minecraft -> {
				ServerLevel overworld = minecraft.overworld();
				io.github.jimbozoomer.jugcraft.walker.HowitzerWalker walker = new io.github.jimbozoomer.jugcraft.walker.HowitzerWalker(
						io.github.jimbozoomer.jugcraft.walker.JugcraftWalkers.HOWITZER_WALKER, overworld);
				walker.snapTo(x - 167.5, y, z - 6.5, -20.0F, 0.0F);
				overworld.addFreshEntity(walker);
			});
			context.waitTicks(30);
			singleplayer.getConnection().waitForChunksRender();
			context.takeScreenshot("jugcraft_howitzer_walker");
			server.runCommand("kill @e[type=jugcraft:howitzer_walker]");
			for (String raider : new String[] {"raider_grunt", "raider_grenadier", "raider_officer", "raider_walker", "raider_blimp"}) {
				server.runCommand("kill @e[type=jugcraft:" + raider + "]");
			}

			// Multi-block machines, ten blocks away, in views twelve blocks apart along the row (the wind turbine is
			// nine tall; the oil machines are at the far end).
			int views = (largeRowLength() + 11) / 12;
			for (int view = 0; view < views; view++) {
				server.runCommand("tp @p %d %d %d 180 8".formatted(x + 24 + view * 12, y + 3, z + 5));
				context.waitTicks(20);
				singleplayer.getConnection().waitForChunksRender();
				context.takeScreenshot("jugcraft_multiblocks_" + (view + 1));
			}

			// A machine screen: walk up to the crusher and use it.
			BlockPos crusher = new BlockPos(x + largeOffset(MachineKind.CRUSHER), y, z - 5);
			server.runCommand("tp @p %d %d %d 180 30".formatted(crusher.getX(), y, z - 3));
			context.waitTicks(10);
			context.getInput().lookAt(crusher);
			context.waitTick();
			context.getInput().pressKey(options -> options.keyUse);
			context.waitForScreen(MachineScreen.class);
			context.takeScreenshot("jugcraft_machine_screen");
			// Its sides view: the faces in the status pane, the key in the help box.
			context.runOnClient(client -> ((MachineScreen) client.gui.screen()).showSides(true));
			context.waitTicks(2);
			context.takeScreenshot("jugcraft_machine_screen_sides");
			context.setScreen(() -> null);

			// One screen in each of the other looks (docs/features/machine-screens.md): the electric battery box, the
			// advanced circuit assembler, the lab's chemical reactor and the industrial distillation tower, whose screen
			// holds the most outputs (five tanks and a slot, batch 24). The crusher above is the workshop look.
			for (MachineKind kind : List.of(MachineKind.BATTERY_BOX, MachineKind.CIRCUIT_ASSEMBLER, MachineKind.CHEMICAL_REACTOR,
					MachineKind.DISTILLATION_TOWER)) {
				BlockPos machine = kind.isLarge() ? new BlockPos(x + largeOffset(kind), y, z - 5)
						: new BlockPos(x - 7 + singleIndex(kind), y, z - 5);
				server.runCommand("tp @p %d %d %d 180 30".formatted(machine.getX(), y, z - 3));
				context.waitTicks(10);
				context.getInput().lookAt(machine);
				context.waitTick();
				context.getInput().pressKey(options -> options.keyUse);
				context.waitForScreen(MachineScreen.class);
				context.waitTicks(25);
				context.takeScreenshot("jugcraft_machine_screen_" + kind.id);
				context.setScreen(() -> null);
			}

			// The auto-crafter's screen, with a stick pattern in its grid.
			BlockPos crafter = new BlockPos(x - 7 + singleIndex(MachineKind.AUTO_CRAFTER), y, z - 5);
			server.runOnServer(minecraft -> {
				if (minecraft.overworld().getBlockEntity(crafter) instanceof MachineBlockEntity machine) {
					machine.setItem(0, new ItemStack(Items.OAK_PLANKS, 12));
					machine.setItem(3, new ItemStack(Items.OAK_PLANKS, 12));
				}
			});
			server.runCommand("tp @p %d %d %d 180 30".formatted(crafter.getX(), y, z - 3));
			context.waitTicks(10);
			context.getInput().lookAt(crafter);
			context.waitTick();
			context.getInput().pressKey(options -> options.keyUse);
			context.waitForScreen(MachineScreen.class);
			context.waitTicks(20);
			context.takeScreenshot("jugcraft_auto_crafter_screen");
			context.setScreen(() -> null);

			// Two of the other machine screens in their looks, on the floor in front of the auto-crafter: the rocket pad
			// (industrial), with a rocket and some cargo but no flight plan yet, then the logic controller (advanced),
			// with two rules set.
			BlockPos front = crafter.south();
			server.runOnServer(minecraft -> {
				minecraft.overworld().setBlockAndUpdate(front, io.github.jimbozoomer.jugcraft.rocketry.JugcraftRocketry.ROCKET_PAD.defaultBlockState());
				if (minecraft.overworld().getBlockEntity(front) instanceof io.github.jimbozoomer.jugcraft.rocketry.RocketPadBlockEntity rocketPad) {
					rocketPad.setItem(io.github.jimbozoomer.jugcraft.rocketry.RocketPadBlockEntity.ROCKET,
							new ItemStack(io.github.jimbozoomer.jugcraft.rocketry.JugcraftRocketry.DELIVERY_ROCKET));
					rocketPad.setItem(0, new ItemStack(Items.IRON_INGOT, 32));
					rocketPad.setItem(1, new ItemStack(Items.REDSTONE, 16));
				}
			});
			context.waitTicks(5);
			context.getInput().lookAt(front);
			context.waitTick();
			context.getInput().pressKey(options -> options.keyUse);
			context.waitForScreen(io.github.jimbozoomer.jugcraft.client.RocketPadScreen.class);
			context.waitTicks(5);
			context.takeScreenshot("jugcraft_rocket_pad_screen");
			context.setScreen(() -> null);
			server.runOnServer(minecraft -> {
				// Emptied first, so its cargo is not dropped in front of the later shots.
				if (minecraft.overworld().getBlockEntity(front) instanceof net.minecraft.world.Container pad) {
					pad.clearContent();
				}
				minecraft.overworld().setBlockAndUpdate(front, io.github.jimbozoomer.jugcraft.control.JugcraftControl.LOGIC_CONTROLLER.defaultBlockState());
				if (minecraft.overworld().getBlockEntity(front) instanceof io.github.jimbozoomer.jugcraft.control.LogicControllerBlockEntity controller) {
					controller.setRule(0, true, net.minecraft.world.item.DyeColor.BLUE, false, 20, net.minecraft.world.item.DyeColor.RED, true);
					controller.setRule(1, true, net.minecraft.world.item.DyeColor.YELLOW, true, 80, net.minecraft.world.item.DyeColor.LIME, false);
				}
			});
			context.waitTicks(5);
			context.getInput().lookAt(front);
			context.waitTick();
			context.getInput().pressKey(options -> options.keyUse);
			context.waitForScreen(io.github.jimbozoomer.jugcraft.client.LogicControllerScreen.class);
			context.waitTicks(5);
			context.takeScreenshot("jugcraft_logic_controller_screen");
			context.setScreen(() -> null);
			server.runOnServer(minecraft -> minecraft.overworld().setBlockAndUpdate(front, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState()));

			// The Engineer's Handbook: the first page and the crusher's page.
			context.setScreen(HandbookScreen::new);
			context.waitTicks(2);
			context.takeScreenshot("jugcraft_handbook");
			// The Progression chapter's first stage, and a machine page (Processing: the crusher).
			context.setScreen(() -> new HandbookScreen(0, 1));
			context.waitTicks(2);
			context.takeScreenshot("jugcraft_handbook_progression");
			context.setScreen(() -> new HandbookScreen(4, 1));
			context.waitTicks(2);
			context.takeScreenshot("jugcraft_handbook_machine_page");
			context.setScreen(() -> null);

			// The prospector: a real survey of the superflat ground under the showroom (it is only a few
			// blocks deep), with ores buried in its dirt so the screen has readings to show; wait for the valve-tube bars to warm up.
			server.runCommand("fill %d %d %d %d %d %d minecraft:iron_ore".formatted(x - 12, y - 3, z - 12, x + 12, y - 3, z + 12));
			server.runCommand("fill %d %d %d %d %d %d minecraft:copper_ore".formatted(x - 6, y - 2, z - 6, x + 6, y - 2, z + 6));
			server.runCommand("setblock %d %d %d minecraft:diamond_ore".formatted(x + 1, y - 2, z + 1));
			AtomicReference<List<OreSurvey.Reading>> readings = new AtomicReference<>(List.of());
			server.runOnServer(minecraft -> readings.set(OreSurvey.survey(minecraft.overworld(), new BlockPos(x, y, z),
					minecraft.overworld().getRandom())));
			context.setScreen(() -> new ProspectorScreen(readings.get()));
			context.waitTicks(60);
			context.takeScreenshot("jugcraft_prospector");
			context.setScreen(() -> null);

			// Pixel Hollows blocks, then the Retro Trader at his cabinet, on a fresh floor south of everything else.
			int pz = z + 16;
			server.runCommand("fill %d %d %d %d %d %d minecraft:smooth_stone".formatted(x - 12, y - 1, z + 12, x + 64, y - 1, z + 40));
			server.runCommand("fill %d %d %d %d %d %d minecraft:air".formatted(x - 12, y, z + 12, x + 64, y + 14, z + 40));
			server.runOnServer(minecraft -> buildPixelHollowsShowroom(minecraft.overworld(), new BlockPos(x, y, pz)));
			server.runCommand("tp @p %d %d %d 180 25".formatted(x, y + 1, pz + 6));
			context.waitTicks(20);
			singleplayer.getConnection().waitForChunksRender();
			context.takeScreenshot("jugcraft_pixel_hollows_blocks");

			// Block centres as numbers ("%d.5" would put a negative coordinate half a block the wrong way).
			server.runCommand("summon minecraft:villager %s %d %s {NoAI:1b,Silent:1b,Rotation:[0f,0f]}".formatted(x + 10.5, y, pz + 0.5));
			server.runOnServer(minecraft -> makeRetroTraders(minecraft.overworld()));
			server.runCommand("tp @p %s %d %s 180 5".formatted(x + 10.5, y, pz + 3.2));
			context.waitTicks(20);
			singleplayer.getConnection().waitForChunksRender();
			context.takeScreenshot("jugcraft_retro_trader");

			// The Retro Game Shop as a village places it (its floor replaces the ground layer).
			server.runCommand("place template jugcraft:village/plains/retro_game_shop %d %d %d".formatted(x - 8, y - 1, z + 24));
			// A village turns the street jigsaw at the doorstep into its final state (air); a bare template keeps it.
			server.runCommand("setblock %d %d %d minecraft:air".formatted(x - 4, y, z + 31));
			server.runCommand("tp @p %d %d %d 180 8".formatted(x - 4, y + 1, z + 39));
			context.waitTicks(20);
			singleplayer.getConnection().waitForChunksRender();
			context.takeScreenshot("jugcraft_retro_game_shop");

			// The shop in a real village: a plains village generated far from the scenes (as /place structure does, once its
			// area is loaded), seen from the street in front of the shop's door.
			int vx = x + 640;
			int vz = z + 640;
			String villageArea = "%d %d %d %d".formatted(vx - 112, vz - 112, vx + 112, vz + 112);
			server.runCommand("forceload add " + villageArea);
			server.waitFor(minecraft -> areaLoaded(minecraft.overworld(), vx, vz, 112), 1200);
			server.runCommand("place structure minecraft:village_plains %d %d %d".formatted(vx, y, vz));
			server.runCommand(server.computeOnServer(minecraft -> shopCamera(minecraft.overworld(), new BlockPos(vx, y, vz), 112)));
			context.waitTicks(40);
			singleplayer.getConnection().waitForChunksRender();
			context.takeScreenshot("jugcraft_retro_game_shop_village");
			server.runCommand("forceload remove " + villageArea);

			// Inside a Pixel Hollows cave: a carved cavity in one chunk, its biome set to the Pixel Hollows, lined by the
			// biome's own circuitstone feature placed around the walls, with crystals on its floor and ceiling; seen in
			// spectator mode with night vision.
			int cx = Math.floorDiv(x + 48, 16) * 16;
			int cz = Math.floorDiv(z + 16, 16) * 16;
			server.runOnServer(minecraft -> carveCave(minecraft.overworld(), new BlockPos(cx, y, cz)));
			server.runCommand("fillbiome %d %d %d %d %d %d jugcraft:pixel_hollows".formatted(cx, y - 1, cz, cx + 15, y + 12, cz + 15));
			int[][] blobs = {{2, 2, 2}, {13, 3, 3}, {2, 8, 13}, {13, 9, 12}, {8, 0, 8}, {8, 10, 8}, {3, 5, 8}, {13, 6, 8}, {8, 4, 2},
					{8, 5, 15}, {4, 1, 12}, {12, 1, 4}, {5, 9, 4}, {11, 9, 13}};
			for (int[] blob : blobs) {
				server.runCommand("place feature jugcraft:pixel_hollows_lining %d %d %d".formatted(cx + blob[0], y + blob[1], cz + blob[2]));
			}
			server.runOnServer(minecraft -> growCrystals(minecraft.overworld(), new BlockPos(cx, y, cz)));
			server.runCommand("gamemode spectator @p");
			server.runCommand("effect give @p minecraft:night_vision infinite 0 true");
			server.runCommand("tp @p %d %d %d 180 15".formatted(cx + 8, y + 4, cz + 13));
			context.waitTicks(40);
			singleplayer.getConnection().waitForChunksRender();
			context.takeScreenshot("jugcraft_pixel_hollows_cave");
		}
	}

	/**
	 * Circuitstone, polished circuitstone, bricks and a pixel lamp in a row facing the camera, crystal clusters on top
	 * and on a small circuitstone wall, and the arcade cabinet at the end.
	 */
	private static void buildPixelHollowsShowroom(ServerLevel level, BlockPos row) {
		Block[] blocks = {PixelHollows.CIRCUITSTONE, PixelHollows.POLISHED_CIRCUITSTONE, PixelHollows.CIRCUITSTONE_BRICKS, PixelHollows.PIXEL_LAMP};
		BlockState cluster = PixelHollows.PIXEL_CRYSTAL_CLUSTER.defaultBlockState();
		for (int i = 0; i < blocks.length; i++) {
			level.setBlock(row.offset(-6 + i * 2, 0, 0), blocks[i].defaultBlockState(), 3);
		}
		level.setBlock(row.offset(-6, 1, 0), cluster.setValue(AmethystClusterBlock.FACING, Direction.UP), 3);
		for (int dx = 1; dx <= 3; dx++) {
			for (int dy = 0; dy <= 2; dy++) {
				level.setBlock(row.offset(dx, dy, -1), PixelHollows.CIRCUITSTONE.defaultBlockState(), 3);
			}
		}
		level.setBlock(row.offset(1, 1, 0), cluster.setValue(AmethystClusterBlock.FACING, Direction.SOUTH), 3);
		level.setBlock(row.offset(3, 0, 0), cluster.setValue(AmethystClusterBlock.FACING, Direction.SOUTH), 3);
		level.setBlock(row.offset(2, 3, -1), cluster.setValue(AmethystClusterBlock.FACING, Direction.UP), 3);
		BlockState cabinet = RetroTrader.ARCADE_CABINET.defaultBlockState().setValue(ArcadeCabinetBlock.FACING, Direction.SOUTH);
		level.setBlock(row.offset(6, 0, 0), cabinet, 3);
		level.setBlock(row.offset(6, 1, 0), cabinet.setValue(ArcadeCabinetBlock.HALF, DoubleBlockHalf.UPPER), 3);
		level.setBlock(row.offset(11, 0, 0), cabinet, 3);
		level.setBlock(row.offset(11, 1, 0), cabinet.setValue(ArcadeCabinetBlock.HALF, DoubleBlockHalf.UPPER), 3);
	}

	private static boolean areaLoaded(ServerLevel level, int x, int z, int reach) {
		for (int cx = (x - reach) >> 4; cx <= (x + reach) >> 4; cx++) {
			for (int cz = (z - reach) >> 4; cz <= (z + reach) >> 4; cz++) {
				if (!level.getChunkSource().hasChunk(cx, cz)) {
					return false;
				}
			}
		}
		return true;
	}

	/**
	 * A teleport command that puts the camera on the street in front of the village's Retro Game Shop, looking at its
	 * door: the shop is found by its arcade cabinet, and its door is the nearest oak door to the cabinet.
	 */
	private static String shopCamera(ServerLevel level, BlockPos centre, int reach) {
		BlockPos cabinet = null;
		for (BlockPos pos : BlockPos.betweenClosed(centre.offset(-reach, -12, -reach), centre.offset(reach, 24, reach))) {
			BlockState state = level.getBlockState(pos);
			if (state.is(RetroTrader.ARCADE_CABINET) && state.getValue(ArcadeCabinetBlock.HALF) == DoubleBlockHalf.LOWER) {
				cabinet = pos.immutable();
				break;
			}
		}
		if (cabinet == null) {
			throw new AssertionError("The generated plains village has no Retro Game Shop");
		}
		BlockPos door = null;
		for (BlockPos pos : BlockPos.betweenClosed(cabinet.offset(-10, -2, -10), cabinet.offset(10, 2, 10))) {
			BlockState state = level.getBlockState(pos);
			if (state.is(Blocks.OAK_DOOR) && state.getValue(DoorBlock.HALF) == DoubleBlockHalf.LOWER
					&& (door == null || pos.distSqr(cabinet) < door.distSqr(cabinet))) {
				door = pos.immutable();
			}
		}
		if (door == null) {
			throw new AssertionError("The Retro Game Shop at " + cabinet + " has no door");
		}
		// From above the street, looking down at the doorstep: the storefront and how it meets the village path.
		Direction outside = level.getBlockState(door).getValue(DoorBlock.FACING).getOpposite();
		BlockPos eye = door.relative(outside, 7).above(6);
		BlockPos doorstep = door.relative(outside);
		Jugcraft.LOGGER.info("[pixel-hollows] Retro Game Shop in a generated plains village: cabinet {}, door {}, doorstep {} ({}), beyond {}",
				cabinet, door, doorstep, level.getBlockState(doorstep.below()), level.getBlockState(doorstep.relative(outside).below()));
		return String.format(Locale.ROOT, "tp @p %.1f %d %.1f facing %.1f %.1f %.1f", eye.getX() + 0.5, eye.getY(), eye.getZ() + 0.5,
				doorstep.getX() + 0.5, doorstep.getY() + 1.0, doorstep.getZ() + 0.5);
	}

	/** Gives every motionless (NoAI) villager the Retro Trader profession, at apprentice level. */
	private static void makeRetroTraders(ServerLevel level) {
		var trader = BuiltInRegistries.VILLAGER_PROFESSION.getOrThrow(RetroTrader.PROFESSION);
		for (Villager villager : level.getEntities(EntityTypeTest.forClass(Villager.class), villager -> villager.isNoAi())) {
			villager.setVillagerData(villager.getVillagerData().withProfession(trader).withLevel(2));
		}
	}

	/** Clusters on about one in six of the cave's open floors and ceilings (worldgen places them the same way). */
	private static void growCrystals(ServerLevel level, BlockPos corner) {
		RandomSource random = RandomSource.create(7L);
		for (int dx = 0; dx < 16; dx++) {
			for (int dz = 0; dz < 16; dz++) {
				for (int dy = 0; dy <= 10; dy++) {
					BlockPos pos = corner.offset(dx, dy, dz);
					if (!level.getBlockState(pos).isAir() || random.nextInt(6) != 0) {
						continue;
					}
					Direction facing = level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP) ? Direction.UP
							: level.getBlockState(pos.above()).isFaceSturdy(level, pos.above(), Direction.DOWN) ? Direction.DOWN : null;
					if (facing != null) {
						level.setBlock(pos, PixelHollows.PIXEL_CRYSTAL_CLUSTER.defaultBlockState().setValue(AmethystClusterBlock.FACING, facing), 2);
					}
				}
			}
		}
	}

	/** A deepslate block filling one chunk, 13 high, hollowed into a few joined round chambers. */
	private static void carveCave(ServerLevel level, BlockPos corner) {
		int[][] chambers = {{8, 5, 8, 6}, {4, 4, 11, 4}, {12, 6, 4, 4}, {8, 3, 13, 3}};
		for (int dx = 0; dx < 16; dx++) {
			for (int dy = -1; dy <= 11; dy++) {
				for (int dz = 0; dz < 16; dz++) {
					boolean hollow = false;
					for (int[] c : chambers) {
						int ax = dx - c[0];
						int ay = dy - c[1];
						int az = dz - c[2];
						hollow |= dy >= 0 && dy <= 10 && ax * ax + ay * ay + az * az < c[3] * c[3];
					}
					level.setBlock(corner.offset(dx, dy, dz), hollow ? Blocks.CAVE_AIR.defaultBlockState() : Blocks.DEEPSLATE.defaultBlockState(), 2);
				}
			}
		}
	}

	/**
	 * Engine at {@code start} facing west (its back drives east), two shafts, a gearbox with a hand crank
	 * on top, a dynamo beyond it and a crusher on the gearbox's north side (behind it, seen from the camera).
	 */
	private static void buildKineticLine(ServerLevel level, BlockPos start) {
		level.setBlock(start.below(), Blocks.WATER.defaultBlockState(), 3);
		level.setBlock(start, JugcraftKinetics.STEAM_ENGINE.defaultBlockState().setValue(SteamEngineBlock.FACING, Direction.WEST), 3);
		Storage<ItemVariant> fuel = ItemStorage.SIDED.find(level, start, Direction.UP);
		try (Transaction transaction = Transaction.openOuter()) {
			fuel.insert(ItemVariant.of(Items.COAL), 8, transaction);
			transaction.commit();
		}
		BlockState shaft = JugcraftKinetics.IRON_SHAFT.defaultBlockState().setValue(ShaftBlock.AXIS, Direction.Axis.X);
		level.setBlock(start.east(), shaft, 3);
		level.setBlock(start.east(2), shaft, 3);
		BlockPos gearbox = start.east(3);
		level.setBlock(gearbox, JugcraftKinetics.BRASS_GEARBOX.defaultBlockState(), 3);
		level.setBlock(gearbox.above(), JugcraftKinetics.HAND_CRANK.defaultBlockState().setValue(HandCrankBlock.FACING, Direction.DOWN), 3);
		level.setBlock(gearbox.east(), JugcraftKinetics.DYNAMO.defaultBlockState().setValue(DynamoBlock.FACING, Direction.SOUTH), 3);
		level.setBlock(gearbox.north(), JugcraftMachines.MACHINES.get(MachineKind.CRUSHER).defaultBlockState()
				.setValue(MachineBlock.FACING, Direction.SOUTH), 3);
	}

	/**
	 * A charged electric motor facing east into a pulley; that pulley is belted to one three blocks above it, which
	 * drives a crusher through a shaft. All run along the x axis.
	 */
	private static void buildBeltLine(ServerLevel level, BlockPos start) {
		level.setBlock(start, JugcraftKinetics.ELECTRIC_MOTOR.defaultBlockState().setValue(ElectricMotorBlock.FACING, Direction.EAST), 3);
		if (level.getBlockEntity(start) instanceof ElectricMotorBlockEntity motor) {
			motor.energy().setAmount(ElectricMotorBlockEntity.CAPACITY);
		}
		BlockState pulley = JugcraftKinetics.BELT_PULLEY.defaultBlockState().setValue(ShaftBlock.AXIS, Direction.Axis.X);
		BlockPos lower = start.east();
		BlockPos upper = lower.above(3);
		level.setBlock(lower, pulley, 3);
		level.setBlock(upper, pulley, 3);
		BeltPulleyBlockEntity.connect(level, lower, upper);
		level.setBlock(upper.east(), JugcraftKinetics.IRON_SHAFT.defaultBlockState().setValue(ShaftBlock.AXIS, Direction.Axis.X), 3);
		level.setBlock(upper.east(2), JugcraftMachines.MACHINES.get(MachineKind.CRUSHER).defaultBlockState()
				.setValue(MachineBlock.FACING, Direction.SOUTH), 3);
	}

	/**
	 * Three conveyors running east with items on them, into a splitter with chests on its three outputs; a charged
	 * motor at the back drives them all.
	 */
	private static void buildConveyorLine(ServerLevel level, BlockPos start) {
		ItemStack[] cargo = {new ItemStack(Items.IRON_INGOT), new ItemStack(Items.COBBLESTONE), new ItemStack(Items.COAL),
				new ItemStack(Items.OAK_LOG)};
		for (int i = 1; i <= 4; i++) {
			BlockPos pos = start.east(i);
			level.setBlock(pos, (i == 4 ? JugcraftLogistics.CONVEYOR_SPLITTER : JugcraftLogistics.CONVEYOR).defaultBlockState()
					.setValue(ConveyorBlock.FACING, Direction.EAST), 3);
			if (i < 4 && level.getBlockEntity(pos) instanceof ConveyorBlockEntity conveyor) {
				for (int slot = 0; slot < 3; slot++) {
					conveyor.accept(cargo[(i + slot) % cargo.length].copy(), slot * 0.3F);
				}
			}
		}
		BlockPos splitter = start.east(4);
		for (BlockPos chest : new BlockPos[] {splitter.north(), splitter.east(), splitter.south()}) {
			level.setBlock(chest, Blocks.CHEST.defaultBlockState(), 3);
		}
		level.setBlock(start, JugcraftKinetics.ELECTRIC_MOTOR.defaultBlockState().setValue(ElectricMotorBlock.FACING, Direction.EAST), 3);
		if (level.getBlockEntity(start) instanceof ElectricMotorBlockEntity motor) {
			motor.energy().setAmount(ElectricMotorBlockEntity.CAPACITY);
		}
	}

	/**
	 * A motor driving a conveyor east into an up slope, a raised conveyor, a down slope and a chest, with items on the
	 * first conveyor and both slopes.
	 */
	private static void buildSlopeLine(ServerLevel level, BlockPos start) {
		BlockState flat = JugcraftLogistics.CONVEYOR.defaultBlockState().setValue(ConveyorBlock.FACING, Direction.EAST);
		BlockState slope = JugcraftLogistics.CONVEYOR_SLOPE.defaultBlockState().setValue(ConveyorBlock.FACING, Direction.EAST);
		level.setBlock(start.east(1), flat, 3);
		level.setBlock(start.east(2), slope.setValue(ConveyorSlopeBlock.ASCENDING, true), 3);
		level.setBlock(start.east(3).above(), flat, 3);
		level.setBlock(start.east(4), slope.setValue(ConveyorSlopeBlock.ASCENDING, false), 3);
		level.setBlock(start.east(5), Blocks.CHEST.defaultBlockState(), 3);
		ItemStack[] cargo = {new ItemStack(Items.IRON_INGOT), new ItemStack(Items.OAK_LOG), new ItemStack(Items.COAL)};
		for (BlockPos pos : new BlockPos[] {start.east(1), start.east(2), start.east(3).above(), start.east(4)}) {
			if (level.getBlockEntity(pos) instanceof ConveyorBlockEntity conveyor) {
				for (int i = 0; i < 2; i++) {
					conveyor.accept(cargo[(pos.getX() + i) % cargo.length].copy(), 0.1F + i * 0.45F);
				}
			}
		}
		level.setBlock(start, JugcraftKinetics.ELECTRIC_MOTOR.defaultBlockState().setValue(ElectricMotorBlock.FACING, Direction.EAST), 3);
		if (level.getBlockEntity(start) instanceof ElectricMotorBlockEntity motor) {
			motor.energy().setAmount(ElectricMotorBlockEntity.CAPACITY);
		}
	}

	/** Three charging stations in a row, facing south, holding the mining drill, the chainsaw and the rocket pack. */
	private static void buildToolStations(ServerLevel level, BlockPos start) {
		Item[] tools = {JugcraftTools.MINING_DRILL, JugcraftTools.CHAINSAW, JugcraftTools.ROCKET_PACK};
		for (int i = 0; i < tools.length; i++) {
			BlockPos lower = start.east(i * 2);
			BlockState state = JugcraftTools.CHARGING_STATION.defaultBlockState().setValue(ChargingStationBlock.FACING, Direction.SOUTH);
			level.setBlock(lower, state, 3);
			level.setBlock(lower.above(), state.setValue(ChargingStationBlock.HALF, DoubleBlockHalf.UPPER), 3);
			if (level.getBlockEntity(lower) instanceof ChargingStationBlockEntity station) {
				station.setTool(new ItemStack(tools[i]));
			}
		}
	}

	/** Position of a one-block machine in the showroom row (multi-block machines skipped). */
	private static int singleIndex(MachineKind target) {
		int index = 0;
		for (MachineKind kind : MachineKind.values()) {
			if (kind == target) {
				return index;
			}
			if (!kind.isLarge()) {
				index++;
			}
		}
		throw new IllegalArgumentException(target.id);
	}

	/** Smooth stone one below {@code from}'s level across the area, and air from there up to {@code to}. */
	private static void clearFloor(ServerLevel level, BlockPos from, BlockPos to) {
		BlockState floor = Blocks.SMOOTH_STONE.defaultBlockState();
		BlockState air = Blocks.AIR.defaultBlockState();
		for (int bx = from.getX(); bx <= to.getX(); bx++) {
			for (int bz = from.getZ(); bz <= to.getZ(); bz++) {
				level.setBlock(new BlockPos(bx, from.getY() - 1, bz), floor, 2);
				for (int by = from.getY(); by <= to.getY(); by++) {
					BlockPos pos = new BlockPos(bx, by, bz);
					if (!level.getBlockState(pos).isAir()) {
						level.setBlock(pos, air, 2);
					}
				}
			}
		}
	}

	/** Blocks a multi-block machine takes across its front (it extends to its right, +x when facing south). */
	private static int width(MachineKind kind) {
		return 1 + kind.footprint().offsets().stream().mapToInt(offset -> Math.abs(offset.getX())).max().orElse(0);
	}

	/** How far east of the showroom's origin a multi-block machine's master stands (see buildShowroom). */
	private static int largeOffset(MachineKind target) {
		int offset = 20;
		for (MachineKind kind : MachineKind.values()) {
			if (kind == target) {
				return offset;
			}
			if (kind.isLarge()) {
				offset += width(kind) + 2;
			}
		}
		throw new IllegalArgumentException(target.id);
	}

	/** Length of the multi-block row: each machine's width plus a two-block gap. */
	private static int largeRowLength() {
		int length = 0;
		for (MachineKind kind : MachineKind.values()) {
			if (kind.isLarge()) {
				length += width(kind) + 2;
			}
		}
		return length;
	}

	/**
	 * Power gear in the electric look, along x from {@code start}: a solar panel, a charged battery box, a capacitor
	 * bank and a charging station holding a drill, joined by copper, silver and aluminum cables (one rising over the
	 * bank), then a charged electric motor turning a shaft into a dynamo, and an electric pump. All face south.
	 */
	/** Batch 37: the control room scene, along the back of the cleared floor (base: the west end of its cable). */
	private static void buildControlRoom(ServerLevel level, BlockPos base) {
		BlockState cable = io.github.jimbozoomer.jugcraft.control.JugcraftControl.DATA_CABLE.defaultBlockState();
		for (int dx = 1; dx <= 5; dx++) {
			level.setBlock(base.offset(dx, 1, 0), cable, 3);
		}
		for (int dx = 6; dx <= 9; dx++) {
			level.setBlock(base.offset(dx, 0, 0), cable, 3);
		}
		BlockPos controllerPos = base.offset(6, 1, 0);
		level.setBlock(controllerPos, io.github.jimbozoomer.jugcraft.control.JugcraftControl.LOGIC_CONTROLLER.defaultBlockState()
				.setValue(io.github.jimbozoomer.jugcraft.control.LogicControllerBlock.FACING, Direction.SOUTH), 3);
		var red = net.minecraft.world.item.DyeColor.RED;
		var blue = net.minecraft.world.item.DyeColor.BLUE;
		int[][] batteries = {{7, 70}, {9, 30}};
		for (int i = 0; i < batteries.length; i++) {
			BlockPos battery = base.offset(batteries[i][0], 0, 2);
			level.setBlock(battery, JugcraftMachines.MACHINES.get(MachineKind.BATTERY_BOX).defaultBlockState()
					.setValue(MachineBlock.FACING, Direction.SOUTH), 3);
			if (level.getBlockEntity(battery) instanceof MachineBlockEntity box && box.energyFor(null) instanceof SimpleEnergyStorage energy) {
				energy.setAmount(energy.getCapacity() * batteries[i][1] / 100);
			}
			level.setBlock(battery.north(), io.github.jimbozoomer.jugcraft.control.JugcraftControl.SENSOR.defaultBlockState()
					.setValue(io.github.jimbozoomer.jugcraft.control.SensorBlock.FACING, Direction.NORTH)
					.setValue(io.github.jimbozoomer.jugcraft.control.Channels.CHANNEL, i == 0 ? red : blue), 3);
		}
		level.setBlock(base.offset(8, 0, 1), io.github.jimbozoomer.jugcraft.control.JugcraftControl.RELAY.defaultBlockState()
				.setValue(io.github.jimbozoomer.jugcraft.control.Channels.CHANNEL, net.minecraft.world.item.DyeColor.GREEN), 3);
		level.setBlock(base.offset(1, 2, 0), io.github.jimbozoomer.jugcraft.control.JugcraftControl.ALARM.defaultBlockState()
				.setValue(io.github.jimbozoomer.jugcraft.control.Channels.CHANNEL, net.minecraft.world.item.DyeColor.ORANGE), 3);
		// The monitor last, so its panels find the cable behind them when they form.
		for (int dx = 2; dx <= 4; dx++) {
			for (int dy = 1; dy <= 2; dy++) {
				level.setBlock(base.offset(dx, dy, 1), io.github.jimbozoomer.jugcraft.control.JugcraftControl.CONTROL_MONITOR.defaultBlockState()
						.setValue(io.github.jimbozoomer.jugcraft.control.ControlMonitorBlock.FACING, Direction.SOUTH), 3);
			}
		}
		for (BlockPos pos : BlockPos.betweenClosed(base, base.offset(9, 2, 0))) {
			if (level.getBlockState(pos).is(io.github.jimbozoomer.jugcraft.control.JugcraftControl.DATA_CABLE)) {
				level.setBlock(pos, Block.updateFromNeighbourShapes(level.getBlockState(pos), level, pos), 3);
			}
		}
		if (level.getBlockEntity(controllerPos) instanceof io.github.jimbozoomer.jugcraft.control.LogicControllerBlockEntity controller) {
			controller.setRule(0, true, red, true, 50, net.minecraft.world.item.DyeColor.GREEN, true);
			controller.setRule(1, true, blue, false, 20, net.minecraft.world.item.DyeColor.ORANGE, true);
			controller.evaluate(level, controllerPos);
			controller.toggle(level, net.minecraft.world.item.DyeColor.ORANGE);
		}
	}

	/** Batches 38-43: the rocketry scene (base: the west end of the back wall). */
	private static void buildRocketry(ServerLevel level, BlockPos base) {
		for (int dx = 0; dx <= 10; dx++) {
			for (int dy = 0; dy <= 2; dy++) {
				level.setBlock(base.offset(dx, dy, 0), Blocks.SMOOTH_STONE.defaultBlockState(), 3);
			}
		}
		level.setBlock(base.offset(1, 0, 3), JugcraftMachines.MACHINES.get(MachineKind.ROCKET_WORKSHOP).defaultBlockState()
				.setValue(MachineBlock.FACING, Direction.SOUTH), 3);
		level.setBlock(base.offset(3, 0, 3), JugcraftMachines.MACHINES.get(MachineKind.CRYOGENIC_LIQUEFIER).defaultBlockState()
				.setValue(MachineBlock.FACING, Direction.SOUTH), 3);
		level.setBlock(base.offset(5, 0, 3), io.github.jimbozoomer.jugcraft.rocketry.JugcraftRocketry.ROCKET_PAD.defaultBlockState(), 3);
		// A zipline from the top of the wall down to an anchor near the camera.
		BlockPos top = base.offset(10, 3, 0);
		BlockPos low = base.offset(13, 0, 4);
		BlockState anchor = io.github.jimbozoomer.jugcraft.rocketry.JugcraftRocketry.ZIPLINE_ANCHOR.defaultBlockState();
		level.setBlock(top, anchor, 3);
		level.setBlock(low, anchor, 3);
		io.github.jimbozoomer.jugcraft.rocketry.ZiplineAnchorBlockEntity.connect(level, top, low, null);
		// A powered booster rail line along the front, with a minecart on it.
		BlockState rail = io.github.jimbozoomer.jugcraft.rocketry.JugcraftRocketry.BOOSTER_RAIL.defaultBlockState()
				.setValue(net.minecraft.world.level.block.PoweredRailBlock.SHAPE, net.minecraft.world.level.block.state.properties.RailShape.EAST_WEST);
		for (int dx = 1; dx <= 8; dx++) {
			level.setBlock(base.offset(dx, 0, 5), rail, 3);
		}
		level.setBlock(base.offset(0, 0, 5), Blocks.REDSTONE_BLOCK.defaultBlockState(), 3);
		var cart = net.minecraft.world.entity.EntityTypes.MINECART.create(level, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
		if (cart != null) {
			BlockPos at = base.offset(5, 0, 5);
			cart.snapTo(at.getX() + 0.5, at.getY() + 0.1, at.getZ() + 0.5, 90, 0);
			level.addFreshEntity(cart);
		}
	}

	/** Batch 45: the Dieselworks showcase (base: the west end of the wall). */
	private static void buildTrench(ServerLevel level, BlockPos base) {
		java.util.function.Function<String, BlockState> block = id ->
				io.github.jimbozoomer.jugcraft.building.Trenchworks.BLOCKS.get(id).defaultBlockState();
		for (int dx = 0; dx <= 10; dx++) {
			for (int dy = 0; dy <= 1; dy++) {
				level.setBlock(base.offset(dx, dy, 0), block.apply("timber_revetment"), 3);
				level.setBlock(base.offset(dx, dy, 3), block.apply("sandbags"), 3);
			}
			level.setBlock(base.offset(dx, 2, 3), block.apply(dx % 2 == 0 ? "sandbags" : "sandbags_slab"), 3);
			for (int dz = 1; dz <= 2; dz++) {
				level.setBlock(base.offset(dx, 0, dz), block.apply("duckboard"), 3);
			}
			for (int dz = 5; dz <= 6; dz++) {
				if ((dx + dz) % 3 != 0) {
					level.setBlock(base.offset(dx, 0, dz), block.apply("barbed_wire"), 3);
				}
			}
		}
		level.setBlock(base.offset(2, 2, 0), block.apply("field_telephone")
				.setValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING, net.minecraft.core.Direction.SOUTH), 3);
		level.setBlock(base.offset(7, 3, 3), block.apply("searchlight")
				.setValue(io.github.jimbozoomer.jugcraft.building.SearchlightBlock.YAW, 5)
				.setValue(io.github.jimbozoomer.jugcraft.building.SearchlightBlock.TILT, 1), 3);
	}

	/**
	 * Batch 59: the bunker scene (base: the dugout's back west corner, at floor level). The dugout runs eight blocks east
	 * and four south, open at the south for the camera.
	 */
	private static void buildBunker(ServerLevel level, BlockPos base) {
		var blocks = io.github.jimbozoomer.jugcraft.building.Bunkerworks.BLOCKS;
		java.util.function.Function<String, BlockState> block = id -> blocks.get(id).defaultBlockState();
		var facing = net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING;
		var half = net.minecraft.world.level.block.DoublePlantBlock.HALF;
		var lower = net.minecraft.world.level.block.state.properties.DoubleBlockHalf.LOWER;
		var upper = net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER;
		var sandbags = io.github.jimbozoomer.jugcraft.building.Trenchworks.BLOCKS;
		for (int dx = 0; dx <= 8; dx++) {
			for (int dz = 0; dz <= 4; dz++) {
				// Back wall of corrugated iron with a doorway in its middle, sandbag side walls and the roof.
				for (int dy = 0; dy <= 1; dy++) {
					if (dz == 0 && dx != 4) {
						level.setBlock(base.offset(dx, dy, dz), block.apply("corrugated_iron"), 3);
					} else if ((dx == 0 || dx == 8) && !(dx == 8 && dz == 2)) {
						level.setBlock(base.offset(dx, dy, dz), sandbags.get("sandbags").defaultBlockState(), 3);
					}
				}
				level.setBlock(base.offset(dx, 2, dz), dz == 4
						? block.apply("timber_shoring").setValue(net.minecraft.world.level.block.RotatedPillarBlock.AXIS, net.minecraft.core.Direction.Axis.X)
						: block.apply("corrugated_iron"), 3);
				level.setBlock(base.offset(dx, 3, dz), block.apply("corrugated_iron_slab"), 3);
				if (dx > 0 && dx < 8 && dz > 0) {
					level.setBlock(base.offset(dx, 0, dz), sandbags.get("duckboard").defaultBlockState(), 3);
				}
			}
		}
		for (int dx : new int[] {1, 7}) {
			for (int dy = 0; dy <= 1; dy++) {
				level.setBlock(base.offset(dx, dy, 4), block.apply("timber_shoring"), 3);
			}
		}
		for (int dx : new int[] {3, 6}) {
			level.setBlock(base.offset(dx, 1, 2), block.apply("bunker_lamp").setValue(net.minecraft.world.level.block.LanternBlock.HANGING, true), 3);
		}
		level.setBlock(base.offset(3, 0, 2), block.apply("map_table").setValue(facing, net.minecraft.core.Direction.SOUTH), 3);
		BlockPos stove = base.offset(6, 0, 1);
		level.setBlock(stove, block.apply("field_kitchen").setValue(facing, net.minecraft.core.Direction.SOUTH), 3);
		level.setBlock(stove.above(), io.github.jimbozoomer.jugcraft.agriculture.JugcraftAgriculture.block("cooking_pot").defaultBlockState(), 3);
		if (level.getBlockEntity(stove) instanceof io.github.jimbozoomer.jugcraft.building.FieldKitchenBlock.Entity kitchen) {
			try (net.fabricmc.fabric.api.transfer.v1.transaction.Transaction transaction =
					net.fabricmc.fabric.api.transfer.v1.transaction.Transaction.openOuter()) {
				kitchen.fuel.insert(net.fabricmc.fabric.api.transfer.v1.item.ItemVariant.of(net.minecraft.world.item.Items.COAL), 1, transaction);
				transaction.commit();
			}
		}
		if (level.getBlockEntity(stove.above()) instanceof io.github.jimbozoomer.jugcraft.agriculture.CookingPotBlockEntity pot) {
			pot.setItem(0, new ItemStack(net.minecraft.world.item.Items.BOWL));
			pot.setItem(1, new ItemStack(net.minecraft.world.item.Items.BEEF));
			pot.setItem(2, new ItemStack(net.minecraft.world.item.Items.POTATO));
			pot.setItem(3, new ItemStack(net.minecraft.world.item.Items.CARROT));
		}
		BlockState bunk = block.apply("bunker_bunk").setValue(facing, net.minecraft.core.Direction.SOUTH);
		level.setBlock(base.offset(1, 0, 1), bunk.setValue(half, lower), 3);
		level.setBlock(base.offset(1, 1, 1), bunk.setValue(half, upper), 3);
		BlockState hanging = block.apply("gas_curtain").setValue(facing, net.minecraft.core.Direction.SOUTH);
		level.setBlock(base.offset(4, 0, 0), hanging.setValue(half, lower), 3);
		level.setBlock(base.offset(4, 1, 0), hanging.setValue(half, upper), 3);
		BlockState rolled = block.apply("gas_curtain").setValue(facing, net.minecraft.core.Direction.WEST)
				.setValue(io.github.jimbozoomer.jugcraft.building.GasCurtainBlock.ROLLED, true);
		level.setBlock(base.offset(8, 0, 2), rolled.setValue(half, lower), 3);
		level.setBlock(base.offset(8, 1, 2), rolled.setValue(half, upper), 3);
		// The trench periscope west of the dugout, behind a sandbag parapet one and a half blocks high.
		for (int dx = -3; dx <= -1; dx++) {
			level.setBlock(base.offset(dx, 0, 1), sandbags.get("sandbags").defaultBlockState(), 3);
			level.setBlock(base.offset(dx, 1, 1), sandbags.get("sandbags_slab").defaultBlockState(), 3);
		}
		BlockState periscope = block.apply("trench_periscope").setValue(facing, net.minecraft.core.Direction.NORTH);
		level.setBlock(base.offset(-2, 0, 2), periscope.setValue(half, lower), 3);
		level.setBlock(base.offset(-2, 1, 2), periscope.setValue(half, upper), 3);
	}

	private static void buildKaiserworks(ServerLevel level, BlockPos base) {
		java.util.function.Function<String, BlockState> block = id ->
				io.github.jimbozoomer.jugcraft.building.Kaiserworks.BLOCKS.get(id).defaultBlockState();
		for (int dx = 0; dx <= 10; dx++) {
			for (int dy = 0; dy <= 5; dy++) {
				boolean window = (dx == 2 || dx == 3 || dx == 7 || dx == 8) && dy >= 1 && dy <= 3;
				String id = dx % 5 == 0 ? "fluted_marble_column"
						: dy == 0 ? "gilt_trimmed_plate" : window ? "leaded_glass" : dy == 4 ? "gilt_frieze"
						: dy == 5 ? "riveted_black_plate" : "black_lacquer_plate";
				level.setBlock(base.offset(dx, dy, 0), block.apply(id), 3);
			}
			level.setBlock(base.offset(dx, 6, 0), block.apply("polished_brass_plate_slab"), 3);
			for (int dz = 1; dz <= 5; dz++) {
				level.setBlock(base.offset(dx, -1, dz), block.apply("station_tiles"), 3);
			}
			level.setBlock(base.offset(dx, 0, 5), block.apply("wrought_iron_lattice_slab"), 3);
		}
		level.setBlock(base.offset(5, 6, 0), block.apply("imperial_crest")
				.setValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING, net.minecraft.core.Direction.SOUTH), 3);
		for (int dx : new int[] {1, 9}) {
			level.setBlock(base.offset(dx, 0, 2), block.apply("black_iron_column"), 3);
			level.setBlock(base.offset(dx, 1, 2), block.apply("imperial_gas_lamp"), 3);
		}
		for (int dx = 4; dx <= 6; dx++) {
			level.setBlock(base.offset(dx, -1, 1), block.apply("polished_marble"), 3);
			level.setBlock(base.offset(dx, 0, 1), block.apply("polished_marble_stairs"), 3);
		}
	}

	private static void buildDieselworks(ServerLevel level, BlockPos base) {
		java.util.function.Function<String, BlockState> block = id ->
				io.github.jimbozoomer.jugcraft.building.Dieselworks.BLOCKS.get(id).defaultBlockState();
		for (int dx = 0; dx <= 10; dx++) {
			for (int dy = 0; dy <= 4; dy++) {
				String id = dx % 5 == 0 ? (dx == 5 ? "ribbed_patina_pillar" : "ribbed_rust_pillar")
						: dy <= 1 ? "riveted_rust_plate" : dy == 2 ? "riveted_band_block"
						: dy == 4 ? "copper_dome_plate"
						: dx == 2 || dx == 8 ? "porthole_window" : dx == 3 || dx == 7 ? "perforated_patina_plate" : "patina_plate";
				level.setBlock(base.offset(dx, dy, 0), block.apply(id), 3);
			}
			level.setBlock(base.offset(dx, 5, 0), block.apply("red_iron_plate_slab"), 3);
			for (int dz = 1; dz <= 5; dz++) {
				level.setBlock(base.offset(dx, -1, dz), block.apply(dz == 1 ? "skid_iron_block" : "rust_plate"), 3);
			}
			// The catwalk: grating slabs two up, on I-beams at each end and in the middle.
			level.setBlock(base.offset(dx, 2, 2), block.apply("rust_grating_slab"), 3);
		}
		for (int dx : new int[] {0, 5, 10}) {
			level.setBlock(base.offset(dx, 0, 2), block.apply("steel_i_beam"), 3);
			level.setBlock(base.offset(dx, 1, 2), block.apply("steel_i_beam"), 3);
		}
		for (int dx : new int[] {2, 8}) {
			level.setBlock(base.offset(dx, 3, 2), block.apply("amber_cage_lamp"), 3);
		}
		level.setBlock(base.offset(4, 0, 4), block.apply("amber_cage_lamp"), 3);
		// In the front row, all inside the screenshot: the steel plates' slabs and stairs, then a red iron stair. The slab
		// comes first, so the middle I-beam behind it stays in view.
		String[] front = {"rust_plate_slab", "rust_plate_stairs", "riveted_rust_plate_stairs", "red_iron_plate_stairs"};
		for (int i = 0; i < front.length; i++) {
			level.setBlock(base.offset(5 + i, 0, 4), block.apply(front[i]), 3);
		}
		level.setBlock(base.offset(3, 0, 4), block.apply("riveted_rust_plate_slab"), 3);
		level.setBlock(base.offset(1, 0, 4), block.apply("rust_grating"), 3);
		level.setBlock(base.offset(2, 0, 4), block.apply("copper_dome_plate_stairs"), 3);
	}

	private static void buildPowerGear(ServerLevel level, BlockPos start) {
		level.setBlock(start, JugcraftMachines.MACHINES.get(MachineKind.SOLAR_PANEL).defaultBlockState()
				.setValue(MachineBlock.FACING, Direction.SOUTH), 3);
		BlockPos battery = start.east(2);
		level.setBlock(battery, JugcraftMachines.MACHINES.get(MachineKind.BATTERY_BOX).defaultBlockState()
				.setValue(MachineBlock.FACING, Direction.SOUTH), 3);
		if (level.getBlockEntity(battery) instanceof MachineBlockEntity box && box.energyFor(null) instanceof SimpleEnergyStorage energy) {
			energy.setAmount(energy.getCapacity());
		}
		BlockPos bank = start.east(4);
		MachineBlock bankBlock = JugcraftMachines.MACHINES.get(MachineKind.CAPACITOR_BANK);
		BlockState bankState = bankBlock.defaultBlockState().setValue(MachineBlock.FACING, Direction.SOUTH);
		level.setBlock(bank, bankState, 3);
		((LargeMachineBlock) bankBlock).setPlacedBy(level, bank, bankState, null, ItemStack.EMPTY);
		BlockPos station = start.east(7);
		BlockState stationState = JugcraftTools.CHARGING_STATION.defaultBlockState().setValue(ChargingStationBlock.FACING, Direction.SOUTH);
		level.setBlock(station, stationState, 3);
		level.setBlock(station.above(), stationState.setValue(ChargingStationBlock.HALF, DoubleBlockHalf.UPPER), 3);
		if (level.getBlockEntity(station) instanceof ChargingStationBlockEntity holder) {
			holder.setTool(new ItemStack(JugcraftTools.MINING_DRILL));
		}
		BlockPos motor = start.east(9);
		level.setBlock(motor, JugcraftKinetics.ELECTRIC_MOTOR.defaultBlockState().setValue(ElectricMotorBlock.FACING, Direction.EAST), 3);
		if (level.getBlockEntity(motor) instanceof ElectricMotorBlockEntity motorEntity) {
			motorEntity.energy().setAmount(ElectricMotorBlockEntity.CAPACITY);
		}
		level.setBlock(motor.east(), JugcraftKinetics.IRON_SHAFT.defaultBlockState().setValue(ShaftBlock.AXIS, Direction.Axis.X), 3);
		level.setBlock(motor.east(2), JugcraftKinetics.DYNAMO.defaultBlockState().setValue(DynamoBlock.FACING, Direction.SOUTH), 3);
		level.setBlock(start.east(13), JugcraftFluids.ELECTRIC_PUMP.defaultBlockState(), 3);
		// Cables last: copper over the battery, silver across the top of the bank, aluminum to the rest.
		List<BlockPos> copper = List.of(start.east(), start.east(3), start.east(3).above(), start.east(3).above(2));
		List<BlockPos> silver = List.of(start.east(4).above(2), start.east(5).above(2), start.east(6).above(2), start.east(6).above());
		List<BlockPos> aluminum = List.of(start.east(6), start.east(8), start.east(12));
		placeCables(level, JugcraftMachines.COPPER_CABLE, copper);
		placeCables(level, JugcraftMachines.SILVER_CABLE, silver);
		placeCables(level, JugcraftMachines.ALUMINUM_CABLE, aluminum);
		for (List<BlockPos> line : List.of(copper, silver, aluminum)) {
			for (BlockPos pos : line) {
				level.setBlock(pos, Block.updateFromNeighbourShapes(level.getBlockState(pos), level, pos), 3);
			}
		}
	}

	private static void placeCables(ServerLevel level, Block cable, List<BlockPos> positions) {
		for (BlockPos pos : positions) {
			level.setBlock(pos, cable.defaultBlockState(), 3);
		}
	}

	/**
	 * One-block machines side by side (in MachineKind order, from x - 7), all facing south towards the
	 * camera; multi-block machines in a second group 20 blocks east, each its own width plus two apart.
	 */
	private static void buildShowroom(ServerLevel level, BlockPos row) {
		int large = 20;
		for (MachineKind kind : MachineKind.values()) {
			MachineBlock block = JugcraftMachines.MACHINES.get(kind);
			BlockState state = block.defaultBlockState().setValue(MachineBlock.FACING, Direction.SOUTH);
			if (kind.isLarge()) {
				BlockPos pos = row.offset(large, 0, 0);
				state = ((LargeMachineBlock) block).formed(state);
				level.setBlock(pos, state, 3);
				((LargeMachineBlock) block).setPlacedBy(level, pos, state, null, ItemStack.EMPTY);
				large += width(kind) + 2;
			} else {
				level.setBlock(row.offset(-7 + singleIndex(kind), 0, 0), state, 3);
			}
		}
	}
}
