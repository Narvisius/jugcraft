package io.github.jimbozoomer.jugcraft.test;

import com.tiviacz.travelersbackpack.attachment.AttachmentUtils;
import com.tiviacz.travelersbackpack.client.screens.BackpackScreen;
import com.tiviacz.travelersbackpack.handlers.KeybindHandler;
import com.tiviacz.travelersbackpack.init.ModDataComponents;
import com.tiviacz.travelersbackpack.inventory.BackpackWrapper;
import com.tiviacz.travelersbackpack.inventory.menu.BackpackItemMenu;
import eu.pb4.trinkets.api.TrinketsApi;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;

/** Real accessory synchronization and native screen opening for normal and extended backpack layouts. */
public class BackpackClientGameTests implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		try (var world = context.worldBuilder().create()) {
			world.getConnection().waitForChunksRender();
			if (!FabricLoader.getInstance().isModLoaded("travelersbackpack")) return;
			context.getInput().resizeWindow(1280, 900);
			context.runOnClient(client -> {
				if (!KeybindHandler.OPEN_BACKPACK.getDefaultKey().getName().equals("key.keyboard.n"))
					throw new AssertionError("Backpack Controls Reset should preserve the conflict-free N default");
				if (!KeybindHandler.OPEN_BACKPACK.saveString().equals("key.keyboard.n"))
					throw new AssertionError("Complete's backpack binding should be N");
				for (var key : client.options.keyMappings) {
					if (key != KeybindHandler.OPEN_BACKPACK && key.getCategory() != net.minecraft.client.KeyMapping.Category.DEBUG
							&& key.same(KeybindHandler.OPEN_BACKPACK)) throw new AssertionError("Backpack key conflicts with " + key.getName());
				}
			});
			for (int tier : new int[]{0, 4}) {
				world.getServer().runOnServer(server -> {
					var player = server.getPlayerList().getPlayers().getFirst();
					player.closeContainer();
					ItemStack bag = BackpackGameTests.stack("standard");
					bag.set(ModDataComponents.TIER, tier);
					bag.set(ModDataComponents.SHOW_TOOL_SLOTS, true);
					BackpackWrapper.initializeSize(bag);
					bag.set(ModDataComponents.UPGRADES, ItemContainerContents.fromItems(java.util.List.of(
							BackpackGameTests.stack("tanks_upgrade"), BackpackGameTests.stack("crafting_upgrade"))));
					var tools = java.util.List.of(Items.IRON_PICKAXE, Items.IRON_AXE, Items.IRON_SHOVEL,
							Items.IRON_HOE, Items.IRON_SWORD, Items.SHEARS);
					bag.set(ModDataComponents.TOOLS_CONTAINER, ItemContainerContents.fromItems(
							tools.subList(0, tier + 2).stream().map(ItemStack::new).toList()));
					TrinketsApi.getAttachment(player).getInventory("chest/back").setItem(0, bag);
					player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
				});
				context.waitFor(client -> AttachmentUtils.isWearingBackpack(client.player)
						&& AttachmentUtils.getWearingBackpack(client.player).getOrDefault(ModDataComponents.TIER, -1) == tier, 100);
				context.getInput().pressKey(KeybindHandler.OPEN_BACKPACK);
				context.waitFor(client -> client.gui.screen() instanceof BackpackScreen, 100);
				context.runOnClient(client -> {
					if (!(client.player.containerMenu instanceof BackpackItemMenu menu)) throw new AssertionError("Native backpack menu missing");
					var bag = menu.getWrapper();
					int slots = tier == 0 ? 27 : 99;
					if (bag.getStorageSize() != slots || bag.getToolSize() != tier + 2 || bag.getUpgradesSize() != tier + 2)
						throw new AssertionError("Approved capacities did not synchronize to the screen");
					if (!client.player.getItemBySlot(EquipmentSlot.CHEST).is(Items.IRON_CHESTPLATE))
						throw new AssertionError("Equipping a backpack displaced chest armor");
				});
				context.waitTicks(5);
				context.runOnClient(client -> client.gui.toastManager().clear());
				context.takeScreenshot("jugcraft_backpack_" + (tier == 0 ? "leather" : "netherite"));
				context.runOnClient(client -> client.player.closeContainer());
				context.waitTicks(5);
			}
		}
		dedicatedReconnect(context);
	}

	private static void dedicatedReconnect(ClientGameTestContext context) {
		try (var server = context.worldBuilder().createServer()) {
			for (int attempt = 0; attempt < 3; attempt++) {
				if (attempt > 0) InProcessServerConfigs.beforeReconnect(server);
				try (var connection = server.connect()) {
					connection.waitForChunksRender();
					if (attempt == 0) server.runOnServer(minecraft -> {
						ItemStack bag = BackpackGameTests.stack("standard");
						BackpackWrapper.initializeSize(bag);
						TrinketsApi.getAttachment(minecraft.getPlayerList().getPlayers().getFirst())
								.getInventory("chest/back").setItem(0, bag);
					});
					context.waitFor(client -> AttachmentUtils.isWearingBackpack(client.player), 100);
					context.getInput().pressKey(KeybindHandler.OPEN_BACKPACK);
					context.waitFor(client -> client.gui.screen() instanceof BackpackScreen, 100);
					context.runOnClient(client -> {
						var bag = ((BackpackItemMenu) client.player.containerMenu).getWrapper();
						if (bag.getStorageSize() != 27 || bag.getToolSize() != 2 || bag.getUpgradesSize() != 2)
							throw new AssertionError("Backpack capacities changed across dedicated reconnect");
						client.player.closeContainer();
					});
					context.waitTicks(5);
				}
			}
		}
	}
}
