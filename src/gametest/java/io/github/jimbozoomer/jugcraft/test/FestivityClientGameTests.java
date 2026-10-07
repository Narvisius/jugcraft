package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.agriculture.CandleSkullBlock;
import io.github.jimbozoomer.jugcraft.agriculture.CarvedPumpkinBlock;
import io.github.jimbozoomer.jugcraft.agriculture.CarvedPumpkinBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.CarvingTemplates;
import io.github.jimbozoomer.jugcraft.agriculture.GravestoneBlock;
import io.github.jimbozoomer.jugcraft.agriculture.GravestoneBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.HangingGhostBlock;
import io.github.jimbozoomer.jugcraft.agriculture.JudgingStandBlock;
import io.github.jimbozoomer.jugcraft.agriculture.JugcraftAgriculture;
import io.github.jimbozoomer.jugcraft.agriculture.PumpkinCarving;
import java.util.List;
import java.util.Locale;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Client game test for the Halloween festivities: a little graveyard (the three gravestones engraved, Candle
 * Skulls lit between them), an arch hung with ghosts and Spun Cobwebs, a Judging Stand with a lit hand-carved
 * pumpkin, the four sweets on a table, three costumed mobs and the Peddler; photographed by day and at midnight
 * (CI job {@code client}). The mobs are posed (no AI); a server test checks who dresses up and when.
 */
public class FestivityClientGameTests implements FabricClientGameTest {
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
			server.runCommand("fill %d %d %d %d %d %d minecraft:grass_block".formatted(x - 10, y - 3, z - 16, x + 30, y - 1, z + 10));
			server.runCommand("fill %d %d %d %d %d %d minecraft:air".formatted(x - 10, y, z - 16, x + 30, y + 12, z + 10));
			context.waitTicks(10);
			server.runOnServer(minecraft -> build(minecraft.overworld(), origin));
			context.waitTicks(60);
			singleplayer.getConnection().waitForChunksRender();
			BlockPos first = new BlockPos(x + 2, y, z - 8);
			System.out.println("[festivities test] engraving on the client: " + context.computeOnClient(client ->
					client.level.getBlockEntity(first) instanceof GravestoneBlockEntity stone ? stone.text() : "(no gravestone)"));

			shoot(context, singleplayer, x + 9, y + 5, z + 6, 180, 18, "jugcraft_halloween_festivities");
			shoot(context, singleplayer, x + 5, y, z - 4, 180, 16, "jugcraft_gravestones");
			shoot(context, singleplayer, x + 2, y, z - 6, 180, 20, "jugcraft_gravestone_engraving");
			shoot(context, singleplayer, x + 21, y, z - 4, 180, 14, "jugcraft_judging_stand");
			server.runCommand("time set midnight");
			shoot(context, singleplayer, x + 9, y + 5, z + 6, 180, 18, "jugcraft_halloween_festivities_night");
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

	private static BlockState facingSouth(String id) {
		return JugcraftAgriculture.block(id).defaultBlockState().setValue(GravestoneBlock.FACING, Direction.SOUTH);
	}

	private static Mob posed(ServerLevel level, String id, double x, double y, double z, ItemStack head) {
		Entity entity = BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.withDefaultNamespace(id)).create(level, EntitySpawnReason.COMMAND);
		if (!(entity instanceof Mob mob)) {
			throw new AssertionError("Could not create a " + id);
		}
		mob.setNoAi(true);
		mob.setPersistenceRequired();
		mob.setItemSlot(EquipmentSlot.HEAD, head);
		mob.snapTo(x, y, z, 0.0F, 0.0F);
		mob.setYHeadRot(0.0F);
		level.addFreshEntity(mob);
		return mob;
	}

	private static void build(ServerLevel level, BlockPos origin) {
		int x = origin.getX();
		int y = origin.getY();
		int z = origin.getZ();
		// The graveyard: three engraved stones, Candle Skulls lit between them.
		List<String> stones = List.of("rounded_gravestone", "cross_gravestone", "obelisk_gravestone");
		List<String> words = List.of("Here lies Jack O'Lantern, carved too deep", "Gone with a bang", "In memory of the last pumpkin pie");
		for (int i = 0; i < stones.size(); i++) {
			BlockPos pos = new BlockPos(x + 2 + i * 3, y, z - 8);
			set(level, pos, facingSouth(stones.get(i)));
			if (level.getBlockEntity(pos) instanceof GravestoneBlockEntity stone) {
				stone.engrave(words.get(i));
			}
		}
		for (int dx : new int[] {3, 7}) {
			set(level, new BlockPos(x + dx, y, z - 8), JugcraftAgriculture.block("candle_skull").defaultBlockState()
					.setValue(CandleSkullBlock.FACING, Direction.SOUTH).setValue(CandleSkullBlock.LIT, true));
		}

		// An arch of spruce with ghosts hanging from it and cobwebs in its corners.
		for (int dy = 0; dy <= 2; dy++) {
			set(level, new BlockPos(x + 12, y + dy, z - 8), Blocks.SPRUCE_FENCE.defaultBlockState());
			set(level, new BlockPos(x + 16, y + dy, z - 8), Blocks.SPRUCE_FENCE.defaultBlockState());
		}
		for (int dx = 12; dx <= 16; dx++) {
			set(level, new BlockPos(x + dx, y + 3, z - 8), Blocks.SPRUCE_PLANKS.defaultBlockState());
		}
		for (int dx : new int[] {14, 15}) {
			set(level, new BlockPos(x + dx, y + 2, z - 8), JugcraftAgriculture.block("hanging_ghost").defaultBlockState()
					.setValue(HangingGhostBlock.FACING, Direction.SOUTH));
		}
		set(level, new BlockPos(x + 13, y + 2, z - 8), JugcraftAgriculture.block("spun_cobweb").defaultBlockState());
		set(level, new BlockPos(x + 13, y, z - 8), JugcraftAgriculture.block("spun_cobweb").defaultBlockState());

		// The carving contest: a lit hand-carved pumpkin on a Judging Stand, and the sweets on a table beside it.
		BlockPos stand = new BlockPos(x + 20, y, z - 8);
		set(level, stand, JugcraftAgriculture.block("judging_stand").defaultBlockState().setValue(JudgingStandBlock.FACING, Direction.SOUTH));
		set(level, stand.above(), JugcraftAgriculture.block("hand_carved_pumpkin").defaultBlockState()
				.setValue(CarvedPumpkinBlock.FACING, Direction.SOUTH).setValue(CarvedPumpkinBlock.LIT, true));
		if (level.getBlockEntity(stand.above()) instanceof CarvedPumpkinBlockEntity pumpkin) {
			int front = PumpkinCarving.faceIndex(Direction.SOUTH, Direction.SOUTH);
			pumpkin.setCarving(PumpkinCarving.BLANK.withFace(front, CarvingTemplates.ALL.get(1).face()), null);
		}
		set(level, new BlockPos(x + 22, y, z - 8), Blocks.SPRUCE_SLAB.defaultBlockState());
		set(level, new BlockPos(x + 23, y, z - 8), Blocks.SPRUCE_SLAB.defaultBlockState());
		List<String> sweets = List.of("glow_gum", "ghost_taffy", "fizz_rocks", "witchs_licorice");
		for (int i = 0; i < sweets.size(); i++) {
			ItemEntity sweet = new ItemEntity(level, x + 22.3 + i * 0.45, y + 0.55, z - 7.5, new ItemStack(JugcraftAgriculture.item(sweets.get(i))));
			sweet.setNoGravity(true);
			sweet.setDeltaMovement(0.0, 0.0, 0.0);
			sweet.setNeverPickUp();
			sweet.setUnlimitedLifetime();
			level.addFreshEntity(sweet);
		}

		// Costumed mobs and the Peddler, posed.
		posed(level, "zombie", x - 1.5, y, z - 7.5, new ItemStack(JugcraftAgriculture.item("witch_hat")));
		posed(level, "skeleton", x - 3.5, y, z - 7.5, new ItemStack(JugcraftAgriculture.item("scarecrow_hat")));
		posed(level, "zombie", x - 5.5, y, z - 7.5, new ItemStack(JugcraftAgriculture.item("ghost_sheet")));
		Mob peddler = posed(level, "wandering_trader", x + 25.5, y, z - 7.5, new ItemStack(JugcraftAgriculture.item("witch_hat")));
		peddler.setCustomName(Component.translatable("entity.jugcraft.halloween_peddler"));
	}
}
