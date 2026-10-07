package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.agriculture.BoneThroneBlock;
import io.github.jimbozoomer.jugcraft.agriculture.CoffinBlock;
import io.github.jimbozoomer.jugcraft.agriculture.CoffinWardrobeBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.ColossalRibBlock;
import io.github.jimbozoomer.jugcraft.agriculture.ColossalSkullBlock;
import io.github.jimbozoomer.jugcraft.agriculture.GargoyleRainspoutBlock;
import io.github.jimbozoomer.jugcraft.agriculture.GargoyleSentinelBlock;
import io.github.jimbozoomer.jugcraft.agriculture.IronBoundCoffinBlock;
import io.github.jimbozoomer.jugcraft.agriculture.IronBoundCoffinBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.JugcraftAgriculture;
import io.github.jimbozoomer.jugcraft.agriculture.LongDecorationBlock;
import io.github.jimbozoomer.jugcraft.agriculture.SarcophagusBlock;
import io.github.jimbozoomer.jugcraft.agriculture.TallDecorationBlock;
import io.github.jimbozoomer.jugcraft.agriculture.VertebraFloorLampBlock;
import java.util.Locale;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChiseledBookShelfBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.entity.ChiseledBookShelfBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.Vec3;

/**
 * Client game test for the Crypt and the Ossuary. To the west a roofless crypt of deepslate: three sarcophagi along the
 * back wall carved with a knight, a lady and a skull (the deepslate one open), an Iron-Bound Coffin locked with its
 * padlock and another open on its velvet, and a Coffin Wardrobe dressed in mismatched armour. In the middle an ossuary
 * parlour before an ossuary wall: the Bone Throne between two Ribcage Bookcases full of books, a Skull Footstool, and
 * Vertebra Floor Lamps. To the east, a giant's bones before a church wall: the Colossal Skull with its jaw dropped, three
 * pairs of Colossal Ribs arched over a spine of Colossal Vertebrae, a Colossal Femur and a Giant Bone Hand, watched by a
 * Gargoyle Sentinel turned toward a pumpkin-headed zombie; Gargoyle Rainspouts over cauldrons and Chimera Finials on
 * the wall's buttresses. By day, close up, in a thunderstorm, and at night in the parlour (the throne's eyes lit) and by the
 * colossus (its sockets glowing, the sentinel's eyes on the zombie). CI job {@code client}.
 */
public class CryptAndOssuaryClientGameTests implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestSingleplayerContext singleplayer = context.worldBuilder()
				.adjustSettings(creator -> creator.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE)).create()) {
			singleplayer.getConnection().waitForChunksRender();
			context.runOnClient(client -> {
				if (!client.gui.hud.isHidden()) {
					client.gui.hud.toggle();
				}
			});
			BlockPos origin = context.computeOnClient(client -> BlockPos.containing(client.player.position()));
			int x = origin.getX();
			int y = origin.getY();
			int z = origin.getZ();
			TestServerContext server = singleplayer.getServer();
			server.runCommand("time set 6000");
			server.runCommand("weather clear");
			server.runCommand("difficulty easy");
			server.runCommand("gamerule minecraft:send_command_feedback false");
			server.runCommand("gamerule minecraft:spawn_mobs false");
			server.runCommand("gamerule minecraft:advance_time false");
			server.runCommand("fill %d %d %d %d %d %d minecraft:grass_block".formatted(x - 16, y - 3, z - 16, x + 16, y - 1, z + 9));
			server.runCommand("fill %d %d %d %d %d %d minecraft:air".formatted(x - 16, y, z - 16, x + 16, y + 12, z + 9));
			// Rain must fall here whatever the world's biome.
			server.runCommand("fillbiome %d %d %d %d %d %d minecraft:plains".formatted(x - 16, y - 4, z - 16, x + 16, y + 16, z + 9));
			context.waitTicks(10);
			server.runOnServer(minecraft -> build(minecraft.overworld(), origin));
			context.waitTicks(40);
			singleplayer.getConnection().waitForChunksRender();

			shoot(context, singleplayer, x, y + 7, z + 7, 180, 30, "jugcraft_crypt_and_ossuary");
			watchFrom(context, singleplayer, origin, new Vec3(-3.0, 4.0, -1.0), 128.0F, 28.0F, "jugcraft_crypt_and_ossuary_crypt");
			watchFrom(context, singleplayer, origin, new Vec3(-8.0, 2.2, -1.5), 118.0F, 25.0F, "jugcraft_crypt_and_ossuary_wardrobe");
			watchFrom(context, singleplayer, origin, new Vec3(2.5, 1.6, -4.0), 180.0F, 14.0F, "jugcraft_crypt_and_ossuary_parlour");
			watchFrom(context, singleplayer, origin, new Vec3(6.5, 3.0, -1.0), 210.0F, 16.0F, "jugcraft_crypt_and_ossuary_colossus");
			server.runCommand("weather thunder");
			context.waitTicks(200);
			watchFrom(context, singleplayer, origin, new Vec3(10.5, 4.0, -5.5), 180.0F, 8.0F, "jugcraft_crypt_and_ossuary_gargoyles");
			server.runCommand("weather clear");
			server.runCommand("time set 18000");
			server.runOnServer(minecraft -> nightfall(minecraft.overworld(), origin));
			context.waitTicks(40);
			watchFrom(context, singleplayer, origin, new Vec3(2.5, 1.6, -4.0), 180.0F, 14.0F, "jugcraft_crypt_and_ossuary_night");
			watchFrom(context, singleplayer, origin, new Vec3(6.5, 3.0, -1.0), 210.0F, 16.0F, "jugcraft_crypt_and_ossuary_colossus_night");
			server.runCommand("time set noon");
			server.runCommand("gamerule minecraft:advance_time true");
		}
	}

	private static void shoot(ClientGameTestContext context, TestSingleplayerContext singleplayer, int x, int y, int z, int yaw, int pitch,
			String name) {
		TestServerContext server = singleplayer.getServer();
		server.runCommand("fill %d %d %d %d %d %d minecraft:barrier replace minecraft:air".formatted(x, y - 1, z, x, y - 1, z));
		server.runCommand(String.format(Locale.ROOT, "tp @p %.1f %d %.1f %d %d", x + 0.5, y, z + 0.5, yaw, pitch));
		context.waitTicks(30);
		singleplayer.getConnection().waitForChunksRender();
		context.takeScreenshot(name);
	}

	private static void watchFrom(ClientGameTestContext context, TestSingleplayerContext singleplayer, BlockPos origin, Vec3 at, float yaw,
			float pitch, String name) {
		TestServerContext server = singleplayer.getServer();
		int id = server.computeOnServer(minecraft -> {
			ServerLevel level = minecraft.overworld();
			ArmorStand stand = new ArmorStand(level, origin.getX() + at.x, origin.getY() + at.y, origin.getZ() + at.z);
			stand.snapTo(stand.getX(), stand.getY(), stand.getZ(), yaw, pitch);
			stand.setYHeadRot(yaw);
			stand.setInvisible(true);
			stand.setNoGravity(true);
			level.addFreshEntity(stand);
			return stand.getId();
		});
		context.waitTicks(5);
		context.runOnClient(client -> {
			Entity stand = client.level.getEntity(id);
			if (stand != null) {
				client.setCameraEntity(stand);
			}
		});
		context.waitTicks(10);
		singleplayer.getConnection().waitForChunksRender();
		context.takeScreenshot(name);
		context.runOnClient(client -> client.setCameraEntity(client.player));
		server.runOnServer(minecraft -> {
			Entity stand = minecraft.overworld().getEntity(id);
			if (stand != null) {
				stand.discard();
			}
		});
	}

	private static Block block(String id) {
		return JugcraftAgriculture.block(id);
	}

	private static void put(ServerLevel level, BlockPos origin, int dx, int dy, int dz, BlockState state) {
		level.setBlock(origin.offset(dx, dy, dz), state, Block.UPDATE_ALL);
	}

	/** A two-block-tall prop, lower half at (dx, dy, dz). */
	private static void tall(ServerLevel level, BlockPos origin, int dx, int dy, int dz, BlockState lower) {
		put(level, origin, dx, dy, dz, lower.setValue(TallDecorationBlock.HALF, DoubleBlockHalf.LOWER));
		put(level, origin, dx, dy + 1, dz, lower.setValue(TallDecorationBlock.HALF, DoubleBlockHalf.UPPER));
	}

	/** A two-block-long prop, foot at (dx, dy, dz) and head beyond it toward {@code facing}. */
	private static void lying(ServerLevel level, BlockPos origin, int dx, int dz, BlockState state, Direction facing) {
		BlockState foot = state.setValue(HorizontalDirectionalBlock.FACING, facing).setValue(LongDecorationBlock.PART, BedPart.FOOT);
		put(level, origin, dx, 0, dz, foot);
		put(level, origin, dx + facing.getStepX(), 0, dz + facing.getStepZ(), foot.setValue(LongDecorationBlock.PART, BedPart.HEAD));
	}

	private static BlockState coffin(boolean open, Direction facing, BedPart part) {
		return block(JugcraftAgriculture.IRON_BOUND_COFFIN).defaultBlockState().setValue(CoffinBlock.FACING, facing).setValue(CoffinBlock.PART, part)
				.setValue(CoffinBlock.OPEN, open);
	}

	private static void build(ServerLevel level, BlockPos origin) {
		// The crypt: a deepslate tile floor, back and west walls of cracked deepslate bricks, roofless.
		for (int dx = -14; dx <= -2; dx++) {
			for (int dz = -12; dz <= -1; dz++) {
				put(level, origin, dx, -1, dz, Blocks.DEEPSLATE_TILES.defaultBlockState());
			}
			for (int dy = 0; dy < 4; dy++) {
				put(level, origin, dx, dy, -12, (dx + dy) % 3 == 0 ? Blocks.CRACKED_DEEPSLATE_BRICKS.defaultBlockState() : Blocks.DEEPSLATE_BRICKS.defaultBlockState());
			}
		}
		for (int dz = -12; dz <= -1; dz++) {
			for (int dy = 0; dy < 4; dy++) {
				put(level, origin, -14, dy, dz, (dz + dy) % 3 == 0 ? Blocks.CRACKED_DEEPSLATE_BRICKS.defaultBlockState() : Blocks.DEEPSLATE_BRICKS.defaultBlockState());
			}
		}
		// Three sarcophagi along the back wall, heads to it, carved with a knight, a lady and a skull; the deepslate one open.
		lying(level, origin, -12, -9, block("stone_brick_sarcophagus").defaultBlockState().setValue(SarcophagusBlock.LID, SarcophagusBlock.Effigy.KNIGHT),
				Direction.NORTH);
		lying(level, origin, -9, -9, block("deepslate_sarcophagus").defaultBlockState().setValue(SarcophagusBlock.LID, SarcophagusBlock.Effigy.LADY)
				.setValue(SarcophagusBlock.OPEN, true), Direction.NORTH);
		lying(level, origin, -6, -9, block("blackstone_sarcophagus").defaultBlockState().setValue(SarcophagusBlock.LID, SarcophagusBlock.Effigy.SKULL),
				Direction.NORTH);
		// Two Iron-Bound Coffins across the crypt, heads west: one locked, one open on its velvet.
		put(level, origin, -8, 0, -5, coffin(false, Direction.WEST, BedPart.FOOT));
		put(level, origin, -9, 0, -5, coffin(false, Direction.WEST, BedPart.HEAD));
		if (level.getBlockEntity(origin.offset(-9, 0, -5)) instanceof IronBoundCoffinBlockEntity locked) {
			IronBoundCoffinBlock.setLock(level, origin.offset(-9, 0, -5), locked, 4242);
		}
		put(level, origin, -11, 0, -2, coffin(true, Direction.WEST, BedPart.FOOT));
		put(level, origin, -12, 0, -2, coffin(true, Direction.WEST, BedPart.HEAD));
		// The Coffin Wardrobe on the west wall, dressed in odd armour.
		tall(level, origin, -13, 0, -6, block(JugcraftAgriculture.COFFIN_WARDROBE).defaultBlockState().setValue(TallDecorationBlock.FACING, Direction.EAST));
		if (level.getBlockEntity(origin.offset(-13, 0, -6)) instanceof CoffinWardrobeBlockEntity wardrobe) {
			wardrobe.set(EquipmentSlot.HEAD, new ItemStack(Items.GOLDEN_HELMET));
			wardrobe.set(EquipmentSlot.CHEST, new ItemStack(Items.NETHERITE_CHESTPLATE));
			wardrobe.set(EquipmentSlot.LEGS, new ItemStack(Items.CHAINMAIL_LEGGINGS));
			wardrobe.set(EquipmentSlot.FEET, new ItemStack(Items.IRON_BOOTS));
		}
		tall(level, origin, -3, 0, -11, block(JugcraftAgriculture.VERTEBRA_LAMP).defaultBlockState().setValue(VertebraFloorLampBlock.LIT, true));

		// The ossuary parlour: a polished blackstone floor before an ossuary wall.
		for (int dx = -1; dx <= 5; dx++) {
			for (int dz = -12; dz <= -5; dz++) {
				put(level, origin, dx, -1, dz, Blocks.POLISHED_BLACKSTONE.defaultBlockState());
			}
			for (int dy = 0; dy < 4; dy++) {
				put(level, origin, dx, dy, -12, block(JugcraftAgriculture.OSSUARY_WALL).defaultBlockState()
						.setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH));
			}
		}
		for (int dz = -10; dz <= -5; dz++) {
			put(level, origin, 2, -1, dz, BuiltInRegistries.BLOCK.getValue(Identifier.withDefaultNamespace("red_wool")).defaultBlockState());
		}
		tall(level, origin, 2, 0, -11, block(JugcraftAgriculture.BONE_THRONE).defaultBlockState().setValue(TallDecorationBlock.FACING, Direction.SOUTH));
		for (int dx : new int[] {0, 4}) {
			for (int dy = 0; dy < 2; dy++) {
				put(level, origin, dx, dy, -11, block(JugcraftAgriculture.RIBCAGE_BOOKCASE).defaultBlockState()
						.setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH));
				if (level.getBlockEntity(origin.offset(dx, dy, -11)) instanceof ChiseledBookShelfBlockEntity shelf) {
					for (int slot = 0; slot < ChiseledBookShelfBlock.SLOT_OCCUPIED_PROPERTIES.size(); slot++) {
						if ((slot + dx + dy) % 4 != 3) {
							shelf.setItem(slot, new ItemStack(slot % 3 == 0 ? Items.ENCHANTED_BOOK : Items.BOOK));
						}
					}
				}
			}
		}
		put(level, origin, 2, 0, -8, block(JugcraftAgriculture.SKULL_FOOTSTOOL).defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH));
		tall(level, origin, -1, 0, -9, block(JugcraftAgriculture.VERTEBRA_LAMP).defaultBlockState().setValue(VertebraFloorLampBlock.LIT, true));
		tall(level, origin, 5, 0, -9, block(JugcraftAgriculture.VERTEBRA_LAMP).defaultBlockState().setValue(VertebraFloorLampBlock.LIT, true));

		// The church wall to the north-east, with buttresses capped by Chimera Finials and two Gargoyle Rainspouts over cauldrons.
		for (int dx = 6; dx <= 15; dx++) {
			for (int dy = 0; dy < 5; dy++) {
				put(level, origin, dx, dy, -15, (dx + dy) % 4 == 0 ? Blocks.MOSSY_STONE_BRICKS.defaultBlockState() : Blocks.STONE_BRICKS.defaultBlockState());
			}
			put(level, origin, dx, 5, -15, Blocks.STONE_BRICK_WALL.defaultBlockState());
		}
		for (int dx : new int[] {7, 11, 15}) {
			for (int dy = 0; dy < 6; dy++) {
				put(level, origin, dx, dy, -14, Blocks.CHISELED_STONE_BRICKS.defaultBlockState());
			}
			put(level, origin, dx, 6, -14, block(JugcraftAgriculture.CHIMERA_FINIAL).defaultBlockState()
					.setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH));
		}
		for (int dx : new int[] {9, 13}) {
			put(level, origin, dx, 3, -14, block(JugcraftAgriculture.GARGOYLE_RAINSPOUT).defaultBlockState()
					.setValue(GargoyleRainspoutBlock.FACING, Direction.SOUTH));
			put(level, origin, dx, 0, -13, Blocks.CAULDRON.defaultBlockState());
		}
		// The buried giant before it: the skull, jaw dropped; ribs arched over a spine; a femur and a hand; bones scattered.
		ColossalSkullBlock skull = (ColossalSkullBlock) block(JugcraftAgriculture.COLOSSAL_SKULL);
		BlockPos master = origin.offset(7, 0, -8);
		for (int part = 0; part < skull.cells().length; part++) {
			level.setBlock(skull.partPos(master, Direction.SOUTH, part), skull.defaultBlockState().setValue(ColossalSkullBlock.FACING, Direction.SOUTH)
					.setValue(ColossalSkullBlock.PART, part).setValue(ColossalSkullBlock.POWERED, true), Block.UPDATE_ALL);
		}
		BlockState rib = block(JugcraftAgriculture.COLOSSAL_RIB).defaultBlockState().setValue(ColossalRibBlock.JOINED, true);
		for (int dx = 11; dx <= 13; dx++) {
			tall(level, origin, dx, 0, -11, rib.setValue(TallDecorationBlock.FACING, Direction.SOUTH));
			tall(level, origin, dx, 0, -9, rib.setValue(TallDecorationBlock.FACING, Direction.NORTH));
			put(level, origin, dx, 0, -10, block(JugcraftAgriculture.COLOSSAL_VERTEBRA).defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.X));
		}
		put(level, origin, 10, 0, -10, block(JugcraftAgriculture.COLOSSAL_VERTEBRA).defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.X));
		lying(level, origin, 8, -5, block(JugcraftAgriculture.COLOSSAL_FEMUR).defaultBlockState(), Direction.EAST);
		tall(level, origin, 13, 0, -5, block(JugcraftAgriculture.BONE_HAND).defaultBlockState().setValue(TallDecorationBlock.FACING, Direction.WEST));
		for (int[] spot : new int[][] {{6, -6}, {11, -7}, {14, -2}, {10, -12}}) {
			put(level, origin, spot[0], 0, spot[1], block(JugcraftAgriculture.BONE_PILE).defaultBlockState());
		}
		for (int[] spot : new int[][] {{6, -10}, {7, -4}, {10, -6}, {12, -7}, {14, -9}, {9, -2}, {11, -3}, {8, -12}}) {
			put(level, origin, spot[0], -1, spot[1], (spot[0] + spot[1]) % 2 == 0 ? Blocks.COARSE_DIRT.defaultBlockState() : Blocks.PODZOL.defaultBlockState());
		}
		// The Gargoyle Sentinel on the path, turned toward a pumpkin-headed zombie by the femur.
		put(level, origin, 10, 0, -2, block(JugcraftAgriculture.GARGOYLE_SENTINEL).defaultBlockState().setValue(GargoyleSentinelBlock.FACING, Direction.WEST));
		Zombie zombie = EntityTypes.ZOMBIE.create(level, EntitySpawnReason.COMMAND);
		if (zombie != null) {
			zombie.snapTo(origin.getX() + 6.5, origin.getY(), origin.getZ() - 2.5, 90.0F, 0.0F);
			zombie.setNoAi(true);
			zombie.setPersistenceRequired();
			zombie.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.CARVED_PUMPKIN));
			level.addFreshEntity(zombie);
		}
	}

	/** At night someone has just risen from the throne: its eyes still burn. */
	private static void nightfall(ServerLevel level, BlockPos origin) {
		BlockPos throne = origin.offset(2, 0, -11);
		for (BlockPos pos : new BlockPos[] {throne, throne.above()}) {
			BlockState state = level.getBlockState(pos);
			if (state.getBlock() instanceof BoneThroneBlock) {
				level.setBlock(pos, state.setValue(BoneThroneBlock.LIT, true), Block.UPDATE_ALL);
			}
		}
	}
}
