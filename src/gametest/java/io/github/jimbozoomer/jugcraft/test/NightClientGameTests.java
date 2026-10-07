package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.agriculture.CarvedPumpkinBlock;
import io.github.jimbozoomer.jugcraft.agriculture.CarvingTemplates;
import io.github.jimbozoomer.jugcraft.agriculture.HalloweenSeason;
import io.github.jimbozoomer.jugcraft.agriculture.HarvestMoon;
import io.github.jimbozoomer.jugcraft.agriculture.HeadlessHorseman;
import io.github.jimbozoomer.jugcraft.agriculture.JugcraftAgriculture;
import io.github.jimbozoomer.jugcraft.agriculture.PumpkinCarving;
import io.github.jimbozoomer.jugcraft.agriculture.ScarecrowBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.TallCrop;
import io.github.jimbozoomer.jugcraft.agriculture.TallCropBlock;
import io.github.jimbozoomer.jugcraft.agriculture.TallDecorationBlock;
import io.github.jimbozoomer.jugcraft.agriculture.ThrowMarker;
import io.github.jimbozoomer.jugcraft.agriculture.TrebuchetBlock;
import io.github.jimbozoomer.jugcraft.agriculture.TrebuchetBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.WillOWisp;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Locale;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

/**
 * Client game test for Halloween nights: a cornfield, three trebuchets (loaded, ready, just thrown) with a landing
 * marker, Wisps in Jars and Horseman's Lanterns standing and hanging, an armor stand in the Horseman's Cloak, and lit
 * carvings on a scarecrow; photographed at noon, then at midnight on Halloween (the event on a fixed server clock)
 * with posed will-o'-wisps over the corn and the Headless Horseman (CI job {@code client}). The creatures are posed
 * (no AI); the server tests check how they behave.
 */
public class NightClientGameTests implements FabricClientGameTest {
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
			server.runCommand("fill %d %d %d %d %d %d minecraft:grass_block".formatted(x - 10, y - 3, z - 20, x + 34, y - 1, z + 10));
			server.runCommand("fill %d %d %d %d %d %d minecraft:air".formatted(x - 10, y, z - 20, x + 34, y + 12, z + 10));
			context.waitTicks(10);
			server.runOnServer(minecraft -> {
				HalloweenSeason.setMode(HalloweenSeason.Mode.AUTO);
				HalloweenSeason.setWindow(HalloweenSeason.DEFAULT_START, HalloweenSeason.DEFAULT_END, ZoneOffset.UTC,
						Clock.fixed(Instant.parse("2026-10-31T20:00:00Z"), ZoneOffset.UTC));
				build(minecraft.overworld(), origin);
			});
			context.waitTicks(60);
			singleplayer.getConnection().waitForChunksRender();

			shoot(context, singleplayer, x + 14, y + 5, z + 6, 180, 18, "jugcraft_halloween_nights");
			shoot(context, singleplayer, x + 13, y + 1, z - 3, 180, 14, "jugcraft_trebuchets");
			shoot(context, singleplayer, x + 25, y + 1, z - 4, 180, 12, "jugcraft_wisp_jars_and_lanterns");

			server.runCommand("time set midnight");
			server.runOnServer(minecraft -> creatures(minecraft.overworld(), origin));
			context.waitTicks(20);
			shoot(context, singleplayer, x + 14, y + 5, z + 6, 180, 18, "jugcraft_halloween_nights_night");
			shoot(context, singleplayer, x + 3, y + 2, z - 4, 180, 12, "jugcraft_wisps");
			shoot(context, singleplayer, x + 20, y + 2, z - 9, 180, 8, "jugcraft_headless_horseman");
			shoot(context, singleplayer, x + 25, y + 1, z - 4, 180, 12, "jugcraft_wisp_jars_night");
			System.out.println("[nights test] Harvest Moon on the client: " + HarvestMoon.clientActive + ", wisps: "
					+ context.computeOnClient(client -> client.level.getEntitiesOfClass(WillOWisp.class,
							client.player.getBoundingBox().inflate(64.0)).size()));
			shoot(context, singleplayer, x + 8, y + 1, z - 7, 180, 10, "jugcraft_harvest_moon_sparks");
			server.runOnServer(minecraft -> HalloweenSeason.reset());
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

	private static BlockState state(String id) {
		return JugcraftAgriculture.block(id).defaultBlockState();
	}

	private static void build(ServerLevel level, BlockPos origin) {
		int x = origin.getX();
		int y = origin.getY();
		int z = origin.getZ();
		// A ripe cornfield for the wisps, a scarecrow with a lit head, and lit carvings before it (Harvest Moon sparks).
		TallCropBlock corn = JugcraftAgriculture.TALL_CROPS.get(TallCrop.CORN);
		for (int dx = 0; dx <= 6; dx++) {
			for (int dz = -16; dz <= -12; dz++) {
				BlockPos pos = origin.offset(dx, 0, dz);
				set(level, pos.below(), Blocks.FARMLAND.defaultBlockState().setValue(BlockStateProperties.MOISTURE, 7));
				for (int section = 0; section < 3; section++) {
					set(level, pos.above(section), corn.defaultBlockState().setValue(TallCropBlock.AGE, TallCropBlock.MAX_AGE)
							.setValue(TallCropBlock.SECTION, section));
				}
			}
		}
		BlockPos scarecrow = new BlockPos(x + 8, y, z - 10);
		BlockState standing = state("scarecrow").setValue(TallDecorationBlock.FACING, Direction.SOUTH);
		set(level, scarecrow, standing);
		set(level, scarecrow.above(), standing.setValue(TallDecorationBlock.HALF, DoubleBlockHalf.UPPER));
		// It wears a lit hand-carved pumpkin with the Classic face.
		ItemStack head = new ItemStack(JugcraftAgriculture.item("hand_carved_pumpkin"));
		head.set(JugcraftAgriculture.CARVING, PumpkinCarving.BLANK.withFace(0, CarvingTemplates.ALL.get(0).face()));
		head.set(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY.with(CarvedPumpkinBlock.LIT, true));
		if (level.getBlockEntity(scarecrow.above()) instanceof ScarecrowBlockEntity worn) {
			worn.setHead(head);
		}
		for (int dx : new int[] {6, 10}) {
			set(level, new BlockPos(x + dx, y, z - 9), state("hand_carved_pumpkin").setValue(CarvedPumpkinBlock.FACING, Direction.SOUTH)
					.setValue(CarvedPumpkinBlock.LIT, true));
		}

		// Three trebuchets, throwing north: loaded, ready and just thrown; a landing marker out in front.
		TrebuchetBlock.Arm[] arms = {TrebuchetBlock.Arm.LOADED, TrebuchetBlock.Arm.READY, TrebuchetBlock.Arm.RELEASED};
		for (int i = 0; i < arms.length; i++) {
			BlockPos pos = new BlockPos(x + 10 + i * 3, y, z - 8);
			set(level, pos, state("trebuchet").setValue(TrebuchetBlock.FACING, Direction.NORTH).setValue(TrebuchetBlock.ARM, arms[i]));
			if (arms[i] == TrebuchetBlock.Arm.LOADED && level.getBlockEntity(pos) instanceof TrebuchetBlockEntity trebuchet) {
				trebuchet.load(new ItemStack(Items.PUMPKIN));
			}
		}
		ThrowMarker marker = JugcraftAgriculture.THROW_MARKER.create(level, EntitySpawnReason.COMMAND);
		if (marker != null) {
			marker.snapTo(x + 14.5, y, z - 13.5, 0.0F, 0.0F);
			marker.setCustomName(Component.translatable("entity.jugcraft.throw_marker.distance", "52.3"));
			marker.setCustomNameVisible(true);
			level.addFreshEntity(marker);
		}

		// Wisps in Jars and the Horseman's Lantern, standing on posts and hanging from a beam.
		for (int dx = 20; dx <= 28; dx += 2) {
			set(level, new BlockPos(x + dx, y, z - 8), Blocks.SPRUCE_FENCE.defaultBlockState());
		}
		for (int dx = 21; dx <= 27; dx++) {
			set(level, new BlockPos(x + dx, y + 2, z - 8), Blocks.SPRUCE_PLANKS.defaultBlockState());
		}
		set(level, new BlockPos(x + 20, y + 1, z - 8), state("wisp_in_a_jar"));
		set(level, new BlockPos(x + 22, y + 1, z - 8), state("horseman_lantern"));
		set(level, new BlockPos(x + 23, y + 1, z - 8), state("wisp_in_a_jar").setValue(LanternBlock.HANGING, true));
		set(level, new BlockPos(x + 25, y + 1, z - 8), state("horseman_lantern").setValue(LanternBlock.HANGING, true));
		set(level, new BlockPos(x + 26, y, z - 7), state("wisp_in_a_jar"));
		Entity stand = BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.withDefaultNamespace("armor_stand")).create(level, EntitySpawnReason.COMMAND);
		if (stand instanceof LivingEntity dummy) {
			dummy.setItemSlot(EquipmentSlot.CHEST, new ItemStack(JugcraftAgriculture.item("horseman_cloak")));
			dummy.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.JACK_O_LANTERN));
			dummy.snapTo(x + 30.5, y, z - 7.5, 0.0F, 0.0F);
			level.addFreshEntity(dummy);
		}
	}

	/** At midnight: will-o'-wisps over the corn and the Headless Horseman, posed side-on between two of his lanterns. */
	private static void creatures(ServerLevel level, BlockPos origin) {
		int x = origin.getX();
		int y = origin.getY();
		int z = origin.getZ();
		double[][] wisps = {{1.5, 3.6, -12.5}, {3.5, 4.2, -14.0}, {5.2, 3.4, -11.8}};
		for (double[] at : wisps) {
			WillOWisp wisp = JugcraftAgriculture.WILL_O_WISP.create(level, EntitySpawnReason.COMMAND);
			if (wisp != null) {
				wisp.setNoAi(true);
				wisp.snapTo(x + at[0], y + at[1], z + at[2], 0.0F, 0.0F);
				level.addFreshEntity(wisp);
			}
		}
		for (int dx : new int[] {17, 24}) {
			set(level, new BlockPos(x + dx, y, z - 14), state("horseman_lantern"));
		}
		HeadlessHorseman horseman = JugcraftAgriculture.HEADLESS_HORSEMAN.create(level, EntitySpawnReason.COMMAND);
		if (horseman != null) {
			horseman.setNoAi(true);
			horseman.setPersistenceRequired();
			horseman.snapTo(x + 20.5, y, z - 15.5, 90.0F, 0.0F);
			horseman.setYHeadRot(90.0F);
			horseman.setYBodyRot(90.0F);
			horseman.setHome(new BlockPos(x + 20, y, z - 15));
			level.addFreshEntity(horseman);
		}
	}
}
