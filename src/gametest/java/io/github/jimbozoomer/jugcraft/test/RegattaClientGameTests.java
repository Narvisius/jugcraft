package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.agriculture.CarvingFace;
import io.github.jimbozoomer.jugcraft.agriculture.CarvingTemplates;
import io.github.jimbozoomer.jugcraft.agriculture.GiantPumpkinBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.JugcraftAgriculture;
import io.github.jimbozoomer.jugcraft.agriculture.PumpkinBoat;
import io.github.jimbozoomer.jugcraft.agriculture.PumpkinBoatData;
import io.github.jimbozoomer.jugcraft.agriculture.RegattaBuoyBlock;
import io.github.jimbozoomer.jugcraft.agriculture.RegattaFlagBlock;
import java.util.Locale;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.CameraType;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

/**
 * Client game test for the pumpkin regatta and trick-or-treating: a pond with a carved, lit Pumpkin Barge
 * (two villagers aboard) and a Pumpkin Racer among numbered buoys, the Regatta Flag on the shore, and armor
 * stands wearing the three costumes and a hand-carved pumpkin by a door with its porch light; photographed by
 * day and at midnight (CI job {@code client}). Screenshots hide the HUD, so the ghost sheet's view (a camera
 * overlay) is not photographed; a server test checks the sheet has it.
 */
public class RegattaClientGameTests implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestSingleplayerContext singleplayer = context.worldBuilder()
				.adjustSettings(creator -> creator.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE)).create()) {
			singleplayer.getConnection().waitForChunksRender();
			BlockPos origin = context.computeOnClient(client -> BlockPos.containing(client.player.position()));
			int x = origin.getX();
			int y = origin.getY();
			int z = origin.getZ();
			TestServerContext server = singleplayer.getServer();
			server.runCommand("time set noon");
			server.runCommand("weather clear");
			server.runCommand("gamerule minecraft:send_command_feedback false");
			server.runCommand("fill %d %d %d %d %d %d minecraft:grass_block".formatted(x - 4, y - 3, z - 26, x + 34, y - 1, z + 8));
			server.runCommand("fill %d %d %d %d %d %d minecraft:air".formatted(x - 4, y, z - 26, x + 34, y + 14, z + 8));
			server.runCommand("fill %d %d %d %d %d %d minecraft:sand".formatted(x, y - 3, z - 22, x + 20, y - 3, z - 5));
			server.runCommand("fill %d %d %d %d %d %d minecraft:water".formatted(x, y - 2, z - 22, x + 20, y - 1, z - 5));
			context.waitTicks(10);
			server.runOnServer(minecraft -> build(minecraft.overworld(), origin));
			context.waitTicks(60);
			singleplayer.getConnection().waitForChunksRender();
			System.out.println("[regatta test] pumpkin boats afloat: " + server.computeOnServer(minecraft -> minecraft.overworld()
					.getEntitiesOfClass(PumpkinBoat.class, new net.minecraft.world.phys.AABB(origin).inflate(40)).size()));

			shoot(context, singleplayer, x + 10, y + 6, z + 5, 180, 24, "jugcraft_pumpkin_regatta");
			shoot(context, singleplayer, x + 7, y + 2, z - 4, 180, 14, "jugcraft_pumpkin_barge");
			shoot(context, singleplayer, x + 26, y + 1, z + 2, 180, 6, "jugcraft_costumes");
			shoot(context, singleplayer, x + 26, y, z - 3, 180, 2, "jugcraft_ghost_sheet");
			// The player in the sheet, seen from the front in third person.
			server.runCommand("item replace entity @p armor.head with jugcraft:ghost_sheet");
			context.runOnClient(client -> client.options.setCameraType(CameraType.THIRD_PERSON_FRONT));
			shoot(context, singleplayer, x + 22, y, z - 1, 180, 10, "jugcraft_ghost_sheet_worn");
			context.runOnClient(client -> client.options.setCameraType(CameraType.FIRST_PERSON));
			server.runCommand("item replace entity @p armor.head with minecraft:air");
			server.runCommand("time set midnight");
			shoot(context, singleplayer, x + 7, y + 3, z - 3, 180, 18, "jugcraft_pumpkin_barge_night");
			shoot(context, singleplayer, x + 26, y + 1, z + 2, 180, 6, "jugcraft_trick_or_treat_night");

		}
	}

	private static void shoot(ClientGameTestContext context, TestSingleplayerContext singleplayer, int x, int y, int z,
			int yaw, int pitch, String name) {
		TestServerContext server = singleplayer.getServer();
		server.runCommand("setblock %d %d %d minecraft:barrier".formatted(x, y - 1, z));
		server.runCommand(String.format(Locale.ROOT, "tp @p %.1f %d %.1f %d %d", x + 0.5, y, z + 0.5, yaw, pitch));
		context.waitTicks(40);
		singleplayer.getConnection().waitForChunksRender();
		context.takeScreenshot(name);
	}

	private static void set(ServerLevel level, BlockPos pos, BlockState state) {
		level.setBlock(pos, state, Block.UPDATE_ALL);
	}

	private static Entity create(ServerLevel level, String id) {
		Entity entity = BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.withDefaultNamespace(id)).create(level, EntitySpawnReason.COMMAND);
		if (entity == null) {
			throw new AssertionError("Could not create a " + id);
		}
		return entity;
	}

	private static PumpkinBoat boat(ServerLevel level, PumpkinBoat.Kind kind, double x, double y, double z, float yaw, PumpkinBoatData data) {
		PumpkinBoat boat = JugcraftAgriculture.boatType(kind).create(level, EntitySpawnReason.COMMAND);
		if (boat == null) {
			throw new AssertionError("Could not create a " + kind.id);
		}
		ItemStack stack = new ItemStack(JugcraftAgriculture.item(kind.id));
		stack.set(JugcraftAgriculture.PUMPKIN_BOAT, data);
		boat.setItem(stack);
		boat.snapTo(x, y, z, yaw, 0.0F);
		level.addFreshEntity(boat);
		return boat;
	}

	private static void stand(ServerLevel level, double x, double y, double z, ItemStack head) {
		LivingEntity stand = (LivingEntity) create(level, "armor_stand");
		stand.setItemSlot(EquipmentSlot.HEAD, head);
		stand.snapTo(x, y, z, 0.0F, 0.0F);
		level.addFreshEntity(stand);
	}

	private static void build(ServerLevel level, BlockPos origin) {
		int x = origin.getX();
		int y = origin.getY();
		int z = origin.getZ();
		// A carved, lit barge with two villagers aboard (a face on every side), and a racer.
		int[][] faces = new int[4][];
		for (int i = 0; i < 4; i++) {
			faces[i] = CarvingFace.scale(CarvingTemplates.ALL.get(i % CarvingTemplates.ALL.size()).face(), GiantPumpkinBlockEntity.FACE_SIZE);
		}
		PumpkinBoat barge = boat(level, PumpkinBoat.Kind.BARGE, x + 7.5, y - 0.4, z - 11.5, 180.0F, PumpkinBoatData.of(612, true, faces));
		for (int i = 0; i < 2; i++) {
			Mob villager = (Mob) create(level, "villager");
			villager.setNoAi(true);
			villager.snapTo(x + 7.5, y, z - 11.5, 0.0F, 0.0F);
			level.addFreshEntity(villager);
			villager.startRiding(barge);
		}
		boat(level, PumpkinBoat.Kind.RACER, x + 13.5, y - 0.4, z - 12.5, 150.0F, PumpkinBoatData.plain(80));

		// The course: the flag on the shore, three numbered buoys out on the pond.
		set(level, new BlockPos(x + 10, y, z - 3), JugcraftAgriculture.block("regatta_flag").defaultBlockState()
				.setValue(RegattaFlagBlock.FACING, Direction.SOUTH));
		int[][] buoys = {{3, -17}, {10, -20}, {17, -17}, {16, -8}};
		for (int i = 0; i < buoys.length; i++) {
			set(level, new BlockPos(x + buoys[i][0], y, z + buoys[i][1]), JugcraftAgriculture.block("regatta_buoy").defaultBlockState()
					.setValue(RegattaBuoyBlock.NUMBER, i + 1));
		}

		// Trick-or-treating: a front door with its porch light, and four costumes on armor stands.
		for (int dx = 23; dx <= 31; dx++) {
			for (int dy = 0; dy <= 3; dy++) {
				set(level, new BlockPos(x + dx, y + dy, z - 9), Blocks.SPRUCE_PLANKS.defaultBlockState());
			}
		}
		BlockState door = Blocks.SPRUCE_DOOR.defaultBlockState().setValue(DoorBlock.FACING, Direction.SOUTH);
		set(level, new BlockPos(x + 27, y, z - 9), door.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER));
		set(level, new BlockPos(x + 27, y + 1, z - 9), door.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
		set(level, new BlockPos(x + 25, y, z - 8), Blocks.JACK_O_LANTERN.defaultBlockState());
		stand(level, x + 24.5, y, z - 6.5, new ItemStack(JugcraftAgriculture.item("witch_hat")));
		stand(level, x + 26.5, y, z - 6.5, new ItemStack(JugcraftAgriculture.item("ghost_sheet")));
		stand(level, x + 28.5, y, z - 6.5, new ItemStack(JugcraftAgriculture.item("scarecrow_hat")));
		stand(level, x + 30.5, y, z - 6.5, new ItemStack(JugcraftAgriculture.item("hand_carved_pumpkin")));
		stand(level, x + 22.5, y, z - 6.5, new ItemStack(Items.CARVED_PUMPKIN));
	}
}
