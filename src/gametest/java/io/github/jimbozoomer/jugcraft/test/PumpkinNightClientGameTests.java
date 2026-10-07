package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.agriculture.CarvedPumpkinBlock;
import io.github.jimbozoomer.jugcraft.agriculture.CarvedPumpkinBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.CarvingTemplates;
import io.github.jimbozoomer.jugcraft.agriculture.FarmStandBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.HarvestEffigyBlock;
import io.github.jimbozoomer.jugcraft.agriculture.HarvestEffigyBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.JugcraftAgriculture;
import io.github.jimbozoomer.jugcraft.agriculture.MultiDecorationBlock;
import io.github.jimbozoomer.jugcraft.agriculture.PumpkinCarving;
import io.github.jimbozoomer.jugcraft.agriculture.SingingPumpkinBlock;
import io.github.jimbozoomer.jugcraft.agriculture.StringLightHookBlock;
import io.github.jimbozoomer.jugcraft.agriculture.StringLightHookBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.StringLightsItem;
import io.github.jimbozoomer.jugcraft.agriculture.TallDecorationBlock;
import java.util.List;
import java.util.Locale;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.Vec3;

/**
 * Client game test for Pumpkin Night. To the west a Farm Stand stocked with Red Kuri and Kabocha pumpkins, apples,
 * carrots, white pumpkins and corn, each crate priced, with the heirlooms in a row beside it and hand-carved ones on hay
 * bales; in the middle a Harvest Effigy in a purple cloak wearing a carved Red Kuri, corn shocks round him and Effigy
 * Ashes by his feet; to the east the four Singing Pumpkins on hay bales, with a Pumpkin Vine Garland and an Autumn Leaf
 * Garland strung on fence posts behind them. By day, the choir caught singing; then at midnight the effigy blazing, the
 * garlands' bulbs lit and the carved heirlooms glowing their own colours. CI job {@code client}.
 */
public class PumpkinNightClientGameTests implements FabricClientGameTest {
	private static final int BURN_SHOT = 240;

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
			server.runCommand("difficulty peaceful");
			server.runCommand("gamerule minecraft:send_command_feedback false");
			server.runCommand("gamerule minecraft:spawn_mobs false");
			server.runCommand("gamerule minecraft:advance_time false");
			server.runCommand("fill %d %d %d %d %d %d minecraft:stone".formatted(x - 16, y - 4, z - 16, x + 16, y - 4, z + 9));
			server.runCommand("fill %d %d %d %d %d %d minecraft:grass_block".formatted(x - 16, y - 3, z - 16, x + 16, y - 1, z + 9));
			server.runCommand("fill %d %d %d %d %d %d minecraft:air".formatted(x - 16, y, z - 16, x + 16, y + 12, z + 9));
			context.waitTicks(10);
			server.runOnServer(minecraft -> build(minecraft.overworld(), origin, minecraft.getPlayerList().getPlayers().get(0)));
			context.waitTicks(40);
			singleplayer.getConnection().waitForChunksRender();

			shoot(context, singleplayer, x, y + 8, z + 7, 180, 32, "jugcraft_pumpkin_night");
			watchFrom(context, singleplayer, origin, new Vec3(-9.0, 1.6, -4.0), 180.0F, 14.0F, false, "jugcraft_pumpkin_night_farm_stand");
			watchFrom(context, singleplayer, origin, new Vec3(8.5, 1.3, -4.5), 180.0F, 6.0F, true, "jugcraft_pumpkin_night_choir");
			server.runCommand("time set 18000");
			server.runOnServer(minecraft -> nightfall(minecraft.overworld(), origin));
			context.waitTicks(BURN_SHOT);
			watchFrom(context, singleplayer, origin, new Vec3(0.5, 2.2, -2.5), 180.0F, 4.0F, false, "jugcraft_pumpkin_night_effigy_burning");
			watchFrom(context, singleplayer, origin, new Vec3(9.0, 2.0, -4.5), 180.0F, 8.0F, false, "jugcraft_pumpkin_night_garlands_night");
			watchFrom(context, singleplayer, origin, new Vec3(-9.0, 1.8, -4.0), 180.0F, 14.0F, false, "jugcraft_pumpkin_night_farm_stand_night");
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

	/** Looks from {@code at} (by an invisible armour stand); with {@code sing}, the choir sings just before the picture. */
	private static void watchFrom(ClientGameTestContext context, TestSingleplayerContext singleplayer, BlockPos origin, Vec3 at, float yaw,
			float pitch, boolean sing, String name) {
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
		if (sing) {
			server.runOnServer(minecraft -> {
				ServerLevel level = minecraft.overworld();
				for (int i = 0; i < 4; i++) {
					BlockPos pumpkin = origin.offset(7 + i, 1, -8);
					SingingPumpkinBlock.sing(level, pumpkin, level.getBlockState(pumpkin));
				}
			});
			context.waitTicks(5);
		}
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

	/** A prop of several blocks, its first block at (dx, dy, dz), facing {@code facing}. */
	private static void multi(ServerLevel level, BlockPos origin, int dx, int dy, int dz, String id, Direction facing) {
		MultiDecorationBlock prop = (MultiDecorationBlock) block(id);
		BlockPos master = origin.offset(dx, dy, dz);
		for (int part = 0; part < prop.cells().length; part++) {
			level.setBlock(prop.partPos(master, facing, part), prop.defaultBlockState().setValue(MultiDecorationBlock.FACING, facing)
					.setValue(prop.partProperty(), part), Block.UPDATE_ALL);
		}
	}

	/** A hand-carved pumpkin of one kind with a starter face on its front, facing south. */
	private static void carvedPumpkin(ServerLevel level, BlockPos pos, String id, int template) {
		PumpkinCarving carving = PumpkinCarving.BLANK.withFace(0, CarvingTemplates.ALL.get(template).face());
		level.setBlock(pos, block(id).defaultBlockState().setValue(CarvedPumpkinBlock.FACING, Direction.SOUTH)
				.setValue(CarvedPumpkinBlock.GLOW, carving.glow()), Block.UPDATE_ALL);
		if (level.getBlockEntity(pos) instanceof CarvedPumpkinBlockEntity pumpkin) {
			pumpkin.setCarving(carving, null);
		}
	}

	private static List<BlockPos> hooks(BlockPos origin) {
		return List.of(origin.offset(6, 2, -10), origin.offset(9, 2, -10), origin.offset(12, 2, -10));
	}

	private static void build(ServerLevel level, BlockPos origin, ServerPlayer owner) {
		// The farm stand on a coarse dirt yard, stocked and priced, the heirlooms beside it and carved ones on hay.
		for (int dx = -15; dx <= -5; dx++) {
			for (int dz = -12; dz <= -4; dz++) {
				put(level, origin, dx, -1, dz, Blocks.COARSE_DIRT.defaultBlockState());
			}
		}
		multi(level, origin, -10, 0, -8, JugcraftAgriculture.FARM_STAND, Direction.SOUTH);
		if (level.getBlockEntity(origin.offset(-10, 0, -8)) instanceof FarmStandBlockEntity stand) {
			stand.setOwner(owner);
			ItemStack[] goods = {new ItemStack(JugcraftAgriculture.item("red_kuri_pumpkin"), 6), new ItemStack(JugcraftAgriculture.item("kabocha_pumpkin"), 4),
					new ItemStack(Items.APPLE, 32), new ItemStack(Items.CARROT, 12), new ItemStack(JugcraftAgriculture.item("white_pumpkin"), 3),
					new ItemStack(JugcraftAgriculture.item("corn"), 16)};
			long[] prices = {3, 4, 1, 1, 6, 2};
			for (int crate = 0; crate < goods.length; crate++) {
				stand.crates().setItem(crate, goods[crate]);
				stand.setPrice(crate, prices[crate]);
			}
		}
		String[] heirlooms = {"red_kuri_pumpkin", "kabocha_pumpkin"};
		for (int i = 0; i < 4; i++) {
			put(level, origin, -14 + i, 0, -5, block(heirlooms[i % 2]).defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH));
		}
		for (int i = 0; i < 2; i++) {
			put(level, origin, -7 + i * 2, 0, -10, Blocks.HAY_BLOCK.defaultBlockState());
			carvedPumpkin(level, origin.offset(-7 + i * 2, 1, -10), "hand_carved_" + heirlooms[i], i + 1);
		}

		// The effigy in a purple cloak, a carved Red Kuri for his head, corn shocks round him and ashes by his feet.
		multi(level, origin, 0, 0, -9, JugcraftAgriculture.HARVEST_EFFIGY, Direction.SOUTH);
		HarvestEffigyBlock effigy = (HarvestEffigyBlock) block(JugcraftAgriculture.HARVEST_EFFIGY);
		for (int part = 0; part < effigy.cells().length; part++) {
			BlockPos at = effigy.partPos(origin.offset(0, 0, -9), Direction.SOUTH, part);
			level.setBlock(at, level.getBlockState(at).setValue(HarvestEffigyBlock.CLOAK, DyeColor.PURPLE), Block.UPDATE_ALL);
		}
		if (level.getBlockEntity(origin.offset(0, 0, -9)) instanceof HarvestEffigyBlockEntity works) {
			ItemStack head = new ItemStack(JugcraftAgriculture.item("hand_carved_red_kuri_pumpkin"));
			head.set(JugcraftAgriculture.CARVING, PumpkinCarving.BLANK.withFace(0, CarvingTemplates.ALL.get(0).face()));
			works.setHead(head);
		}
		for (int dx : new int[] {-3, 3}) {
			put(level, origin, dx, 0, -10, block("corn_shock").defaultBlockState());
			put(level, origin, dx, 1, -10, block("corn_shock").defaultBlockState().setValue(TallDecorationBlock.HALF, DoubleBlockHalf.UPPER));
		}
		put(level, origin, 2, 0, -7, block(JugcraftAgriculture.EFFIGY_ASHES).defaultBlockState());

		// The choir on hay bales, tuned to a chord, and the garlands on fence posts behind them.
		String[] voices = {"bass", "tenor", "alto", "soprano"};
		int[] chord = {12, 16, 19, 12};
		for (int i = 0; i < 4; i++) {
			put(level, origin, 7 + i, 0, -8, Blocks.HAY_BLOCK.defaultBlockState());
			put(level, origin, 7 + i, 1, -8, block("singing_pumpkin_" + voices[i]).defaultBlockState().setValue(SingingPumpkinBlock.FACING, Direction.SOUTH)
					.setValue(SingingPumpkinBlock.NOTE, chord[i]));
		}
		List<BlockPos> hooks = hooks(origin);
		for (BlockPos hook : hooks) {
			level.setBlock(hook.below(2), Blocks.DARK_OAK_FENCE.defaultBlockState(), Block.UPDATE_ALL);
			level.setBlock(hook.below(), Blocks.DARK_OAK_FENCE.defaultBlockState(), Block.UPDATE_ALL);
			level.setBlock(hook, block("string_light_hook").defaultBlockState().setValue(StringLightHookBlock.FACE, AttachFace.FLOOR), Block.UPDATE_ALL);
		}
		StringLightsItem.string(level, hooks.get(0), hooks.get(1), StringLightHookBlockEntity.Strand.PUMPKIN_VINE);
		StringLightsItem.string(level, hooks.get(1), hooks.get(2), StringLightHookBlockEntity.Strand.AUTUMN_LEAVES);
	}

	/** At midnight the effigy is lit, the hooks charged so the garlands' bulbs glow, and the carved heirlooms lit. */
	private static void nightfall(ServerLevel level, BlockPos origin) {
		HarvestEffigyBlock.ignite(level, origin.offset(0, 0, -9));
		for (BlockPos hook : hooks(origin)) {
			if (level.getBlockEntity(hook) instanceof StringLightHookBlockEntity entity) {
				entity.energy().setAmount(StringLightHookBlockEntity.CAPACITY);
				entity.update(level);
			}
		}
		for (int i = 0; i < 2; i++) {
			BlockPos at = origin.offset(-7 + i * 2, 1, -10);
			BlockState carved = level.getBlockState(at);
			if (carved.getBlock() instanceof CarvedPumpkinBlock) {
				level.setBlock(at, carved.setValue(CarvedPumpkinBlock.LIT, true), Block.UPDATE_ALL);
			}
		}
	}
}
