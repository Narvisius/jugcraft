package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.client.MachineScreen;
import io.github.jimbozoomer.jugcraft.machine.JugcraftMachines;
import io.github.jimbozoomer.jugcraft.machine.MachineBlock;
import io.github.jimbozoomer.jugcraft.machine.MachineBlockEntity;
import io.github.jimbozoomer.jugcraft.machine.MachineKind;
import io.github.jimbozoomer.jugcraft.world.ArcadeCabinetBlock;
import io.github.jimbozoomer.jugcraft.world.PixelHollows;
import io.github.jimbozoomer.jugcraft.world.RetroTrader;
import java.util.List;
import java.util.Locale;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerConnection;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.world.TestWorldSave;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.entity.EntityTypeTest;

/**
 * Multiplayer and persistence checks with a real client. First a world holding a Retro Trader, a machine with items
 * and an arcade cabinet is saved, closed and opened again. Then a real dedicated server (the game's own server and
 * network code: a singleplayer world passes packets in memory without encoding them) is started and joined: the
 * client uses a machine and trades with the Retro Trader through it, disconnects and joins again. One client only;
 * the two-client test is manual (docs/TESTING.md). Results are logged with the prefix "[server-check]".
 */
public class JugcraftServerClientGameTests implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		saveAndReopen(context);
		dedicatedServer(context);
	}

	/** The fixture survives the world being saved, closed and opened again. */
	private static void saveAndReopen(ClientGameTestContext context) {
		final BlockPos origin;
		final String before;
		final TestWorldSave save;
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			world.getConnection().waitForChunksRender();
			origin = context.computeOnClient(client -> BlockPos.containing(client.player.position()));
			world.getServer().runOnServer(server -> placeFixture(server.overworld(), origin));
			world.getServer().waitFor(server -> !traders(server.overworld()).isEmpty(), 100);
			world.getServer().runOnServer(server -> makeRetroTraders(server.overworld()));
			before = world.getServer().computeOnServer(server -> describe(server.overworld(), origin));
			save = world.getWorldSave();
		}
		try (TestSingleplayerContext world = save.open()) {
			world.getConnection().waitForChunksRender();
			// Entities load a little after their chunks.
			world.getServer().waitFor(server -> !traders(server.overworld()).isEmpty(), 200);
			String after = world.getServer().computeOnServer(server -> describe(server.overworld(), origin));
			Jugcraft.LOGGER.info("[server-check] before saving: {}", before);
			Jugcraft.LOGGER.info("[server-check] after reopening: {}", after);
			check(after.equals(before), "The world changed across save and reopen:\n  before " + before + "\n  after  " + after);
		}
	}

	/** A dedicated server: use a machine and the Retro Trader through the network, then leave and join again. */
	private static void dedicatedServer(ClientGameTestContext context) {
		try (TestDedicatedServerContext server = context.worldBuilder().createServer()) {
			final BlockPos origin;
			try (TestDedicatedServerConnection connection = server.connect()) {
				connection.waitForChunksRender();
				origin = context.computeOnClient(client -> BlockPos.containing(client.player.position()));
				server.runCommand("gamemode creative @a");
				server.runCommand("time set noon");
				server.runCommand("weather clear");
				server.runOnServer(minecraft -> placeFixture(minecraft.overworld(), origin));
				server.waitFor(minecraft -> !traders(minecraft.overworld()).isEmpty(), 100);
				server.runOnServer(minecraft -> makeRetroTraders(minecraft.overworld()));
				server.runCommand(String.format(Locale.ROOT, "tp @a %.1f %d %.1f 180 20", origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5));
				context.waitTicks(20);
				connection.waitForChunksRender();

				// The crusher's screen opens on use and shows the items the server holds.
				context.getInput().lookAt(crusher(origin));
				context.waitTick();
				context.getInput().pressKey(options -> options.keyUse);
				context.waitForScreen(MachineScreen.class);
				context.waitTicks(10);
				boolean machineSynced = context.computeOnClient(client -> client.player.containerMenu.slots.stream()
						.anyMatch(slot -> slot.getItem().is(Items.FEATHER) && slot.getItem().getCount() == 3));
				check(machineSynced, "The crusher's screen on the dedicated server does not show its 3 feathers");
				context.takeScreenshot("jugcraft_dedicated_server_machine");
				context.runOnClient(client -> client.player.closeContainer()); // as Escape does: tells the server too
				context.waitForScreen(null);

				// The Retro Trader's offers reach the client after a real right-click on him.
				context.getInput().lookAt(trader(origin));
				context.waitTick();
				context.getInput().pressKey(options -> options.keyUse);
				try {
					context.waitFor(client -> client.player.containerMenu instanceof MerchantMenu, 100);
				} catch (AssertionError timedOut) {
					String client = context.computeOnClient(minecraft -> "client player at " + minecraft.player.position() + " looking "
							+ minecraft.player.getYRot() + "/" + minecraft.player.getXRot());
					String serverSide = server.computeOnServer(minecraft -> {
						ServerPlayer player = minecraft.getPlayerList().getPlayers().get(0);
						Villager villager = traders(minecraft.overworld()).get(0);
						return "server player at " + player.position() + ", villager at " + villager.position() + ", distance "
								+ player.distanceTo(villager) + ", offers " + villager.getOffers().size() + ", trading " + villager.isTrading();
					});
					throw new AssertionError("Right-clicking the Retro Trader did not open his trades (" + client + "; " + serverSide + ")",
							timedOut);
				}
				context.waitForScreen(MerchantScreen.class);
				context.waitTicks(10);
				List<String> offers = context.computeOnClient(client -> client.player.containerMenu instanceof MerchantMenu menu
						? menu.getOffers().stream().map(offer -> offer.getResult().toString()).toList() : List.of());
				Jugcraft.LOGGER.info("[server-check] Retro Trader offers on the dedicated server: {}", offers);
				check(!offers.isEmpty() && offers.stream().allMatch(offer -> offer.contains("jugcraft:")),
						"The Retro Trader's offers did not reach the client: " + offers);
				context.takeScreenshot("jugcraft_dedicated_server_trader");
				context.runOnClient(client -> client.player.closeContainer()); // as Escape does: tells the server too
				context.waitForScreen(null);

				server.runCommand("give @a jugcraft:pixel_shard 7");
				context.waitTicks(10);
			}
			// Leave and join again: the player's inventory and the world are as they were.
			try (TestDedicatedServerConnection connection = server.connect()) {
				connection.waitForChunksRender();
				int shards = context.computeOnClient(client -> count(client.player.getInventory(), PixelHollows.PIXEL_SHARD));
				String state = server.computeOnServer(minecraft -> describe(minecraft.overworld(), origin));
				Jugcraft.LOGGER.info("[server-check] after rejoining the dedicated server: {} pixel shards; {}", shards, state);
				check(shards == 7, "After rejoining, the player has " + shards + " pixel shards instead of 7");
				check(state.contains("crusher slot 0: 3 minecraft:feather") && state.contains("jugcraft:retro_trader"),
						"After rejoining, the world is " + state);
				context.takeScreenshot("jugcraft_dedicated_server");
			}
		}
	}

	private static BlockPos crusher(BlockPos origin) {
		return origin.offset(0, 0, -2);
	}

	private static BlockPos cabinet(BlockPos origin) {
		return origin.offset(-2, 0, -2);
	}

	private static BlockPos trader(BlockPos origin) {
		return origin.offset(2, 0, -2);
	}

	/** A crusher holding 3 feathers (no recipe, so they stay), an arcade cabinet and a motionless villager. */
	private static void placeFixture(ServerLevel level, BlockPos origin) {
		BlockState crusher = JugcraftMachines.MACHINES.get(MachineKind.CRUSHER).defaultBlockState().setValue(MachineBlock.FACING, Direction.SOUTH);
		level.setBlock(crusher(origin), crusher, 3);
		if (level.getBlockEntity(crusher(origin)) instanceof MachineBlockEntity machine) {
			machine.setItem(0, new ItemStack(Items.FEATHER, 3));
		}
		BlockState cabinet = RetroTrader.ARCADE_CABINET.defaultBlockState().setValue(ArcadeCabinetBlock.FACING, Direction.SOUTH);
		level.setBlock(cabinet(origin), cabinet, 3);
		level.setBlock(cabinet(origin).above(), cabinet.setValue(ArcadeCabinetBlock.HALF, DoubleBlockHalf.UPPER), 3);
		BlockPos trader = trader(origin);
		var server = level.getServer();
		server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),
				// The block's centre: "%d.5" would put a negative coordinate half a block on the wrong side.
				String.format(Locale.ROOT, "summon minecraft:villager %.1f %d %.1f {NoAI:1b,Rotation:[0f,0f]}", trader.getX() + 0.5, trader.getY(),
						trader.getZ() + 0.5));
	}

	/** Motionless villagers, which only this test makes. */
	private static List<? extends Villager> traders(ServerLevel level) {
		return level.getEntities(EntityTypeTest.forClass(Villager.class), Villager::isNoAi);
	}

	/** Gives the motionless villagers the Retro Trader profession and makes their novice offers. */
	private static void makeRetroTraders(ServerLevel level) {
		var profession = BuiltInRegistries.VILLAGER_PROFESSION.getOrThrow(RetroTrader.PROFESSION);
		for (Villager villager : traders(level)) {
			villager.setVillagerData(villager.getVillagerData().withProfession(profession));
			villager.getOffers();
		}
	}

	/** What the fixture holds, as text to compare. */
	private static String describe(ServerLevel level, BlockPos origin) {
		StringBuilder out = new StringBuilder();
		out.append("crusher slot 0: ").append(level.getBlockEntity(crusher(origin)) instanceof MachineBlockEntity machine
				? machine.getItem(0).toString() : "no crusher");
		out.append("; cabinet: ").append(level.getBlockState(cabinet(origin))).append(" / ").append(level.getBlockState(cabinet(origin).above()));
		for (Villager villager : traders(level)) {
			out.append("; villager ").append(villager.getVillagerData().profession().getRegisteredName()).append(" offers ")
					.append(villager.getOffers().stream().map(offer -> offer.getResult().toString()).toList());
		}
		return out.toString();
	}

	private static int count(Inventory inventory, Item item) {
		int total = 0;
		for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
			ItemStack stack = inventory.getItem(slot);
			if (stack.is(item)) {
				total += stack.getCount();
			}
		}
		return total;
	}

	private static void check(boolean condition, String message) {
		if (!condition) {
			throw new AssertionError(message);
		}
	}
}
