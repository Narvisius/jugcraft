package io.github.jimbozoomer.jugcraft.test;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.agriculture.TrickOrTreat;
import io.github.jimbozoomer.jugcraft.client.EncyclopediaClient;
import io.github.jimbozoomer.jugcraft.client.EncyclopediaInventoryButton;
import io.github.jimbozoomer.jugcraft.client.EncyclopediaScreen;
import io.github.jimbozoomer.jugcraft.concordance.CircleAnchorBlockEntity;
import io.github.jimbozoomer.jugcraft.concordance.ConcordanceProgress;
import io.github.jimbozoomer.jugcraft.concordance.JugcraftConcordance;
import io.github.jimbozoomer.jugcraft.concordance.rules.Evidence;
import io.github.jimbozoomer.jugcraft.concordance.rules.ResearchState;
import io.github.jimbozoomer.jugcraft.guide.encyclopedia.EncyclopediaPayload;
import io.github.jimbozoomer.jugcraft.lair.JugcraftLairs;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.fabricmc.fabric.api.client.creativetab.v1.FabricCreativeModeInventoryScreen;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerConnection;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerContext;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

/**
 * Concordance Chunk A (A-04) in a real client on a dedicated server, so every encyclopedia request and answer is encoded,
 * sent and decoded. Results are logged with the prefix "[encyclopedia-check]". One client.
 * <ul>
 * <li><b>The key.</b> No other loaded key shares its default (F3 debug combinations aside, A-D7). From the game it
 * opens on Discover, where finding something that glows is next (screenshot {@code jugcraft_encyclopedia_discover_new});
 * the key again, or Esc, closes it; typing its letter in chat does not open it.</li>
 * <li><b>The inventory button.</b> It stands right of the recipe-book button, over no slot or other widget, and follows it
 * when the recipe book opens ({@code jugcraft_encyclopedia_inventory_button}, {@code jugcraft_encyclopedia_recipe_book}).
 * A real click closes the inventory, so the 2×2 grid's things come back, and opens the encyclopedia; Back opens the
 * inventory again. In creative it shows on the Survival Inventory tab only ({@code jugcraft_encyclopedia_creative_button}).</li>
 * <li><b>The route.</b> Examined in the dark, the dark step is done ({@code jugcraft_encyclopedia_discover_route}); with
 * First Light understood and a wand in hand, Spellcraft shows them ({@code jugcraft_encyclopedia_spellcraft}), and its
 * link opens the Spell Composer, whose Esc comes back. Then a Kindled Lantern and an Adept's Wand are each held, for
 * ticket A-07's runtime view ({@code jugcraft_first_loadout_kindled_lantern}, {@code jugcraft_first_loadout_adept_wand}).</li>
 * <li><b>Inspection.</b> Opened looking at a Circle Anchor, the Inspect page is the anchor's own report
 * ({@code jugcraft_encyclopedia_inspect}).</li>
 * <li><b>Expeditions and keys</b> ({@code jugcraft_encyclopedia_expedition}, {@code jugcraft_encyclopedia_keys}), and the
 * screen at GUI scale 4 and in a small window ({@code jugcraft_encyclopedia_scale4}, {@code jugcraft_encyclopedia_small}).</li>
 * </ul>
 */
public class EncyclopediaClientGameTests implements FabricClientGameTest {
	private static final int WAIT = 200;

	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestDedicatedServerContext server = context.worldBuilder().createServer()) {
			try (TestDedicatedServerConnection connection = server.connect()) {
				connection.waitForChunksRender();
				server.runCommand("time set noon");
				server.runCommand("weather clear");
				server.runCommand("gamerule minecraft:spawn_mobs false");
				server.runCommand("gamemode survival @a");
				context.waitTicks(20);
				theKey(context);
				theInventoryButton(context, server);
				theRoute(context, server);
				inspection(context, server);
				expeditionsAndKeys(context, server);
				creative(context, server);
				sizes(context);
			}
		}
	}

	private static void theKey(ClientGameTestContext context) {
		KeyMapping key = EncyclopediaClient.key();
		String clash = context.computeOnClient(client -> Arrays.stream(client.options.keyMappings)
				.filter(other -> other != key && other.getCategory() != KeyMapping.Category.DEBUG && other.getDefaultKey().equals(key.getDefaultKey()))
				.map(KeyMapping::getName).toList().toString());
		// Every loaded default outside the debug keys, so one run shows which keys a new default could still take.
		String taken = context.computeOnClient(client -> Arrays.stream(client.options.keyMappings)
				.filter(other -> other.getCategory() != KeyMapping.Category.DEBUG && !other.getDefaultKey().equals(InputConstants.UNKNOWN))
				.map(other -> other.getDefaultKey().getName() + "=" + other.getName()).sorted().toList().toString());
		Jugcraft.LOGGER.info("[encyclopedia-check] loaded defaults: {}", taken);
		Jugcraft.LOGGER.info("[encyclopedia-check] the default key {}; other keys with that default: {}", key.getDefaultKey().getName(), clash);
		check(!key.isUnbound() && clash.equals("[]"), "The encyclopedia's default key should be bound and shared with no other key: " + clash);

		context.getInput().pressKey(key);
		context.waitForScreen(EncyclopediaScreen.class);
		context.waitFor(client -> EncyclopediaClient.navigator().received(), WAIT);
		context.waitTicks(5);
		String page = context.computeOnClient(client -> ((EncyclopediaScreen) client.gui.screen()).page());
		EncyclopediaPayload.StepState find = step(context, "find");
		Jugcraft.LOGGER.info("[encyclopedia-check] opened by its key: page {}, find {}", page, find);
		check(page.equals("discover") && find == EncyclopediaPayload.StepState.NEXT, "A newcomer should open on Discover with find next: " + page + " " + find);
		context.takeScreenshot("jugcraft_encyclopedia_discover_new");

		context.getInput().pressKey(key);
		context.waitForScreen(null);
		context.getInput().pressKey(key);
		context.waitForScreen(EncyclopediaScreen.class);
		context.getInput().pressKey(InputConstants.KEY_ESCAPE);
		context.waitForScreen(null);

		// Typing the key's letter in chat types it; the encyclopedia does not open.
		context.getInput().pressKey(options -> options.keyChat);
		context.waitForScreen(ChatScreen.class);
		context.getInput().typeChars("b");
		context.waitTicks(5);
		check(context.computeOnClient(client -> client.gui.screen() instanceof ChatScreen), "Typing in chat should not open the encyclopedia");
		context.getInput().pressKey(InputConstants.KEY_ESCAPE);
		context.waitForScreen(null);
	}

	private static void theInventoryButton(ClientGameTestContext context, TestDedicatedServerContext server) {
		// Four planks in the 2x2 grid: closing the inventory as Esc does gives them back.
		server.runOnServer(minecraft -> {
			ServerPlayer player = first(minecraft);
			player.getInventory().clearContent();
			for (int slot = 1; slot <= 4; slot++) {
				player.inventoryMenu.getSlot(slot).set(new ItemStack(Items.OAK_PLANKS));
			}
			player.inventoryMenu.broadcastChanges();
		});
		context.waitTicks(5);
		context.getInput().pressKey(options -> options.keyInventory);
		context.waitForScreen(InventoryScreen.class);
		context.waitTicks(5);
		String placed = context.computeOnClient(EncyclopediaClientGameTests::placement);
		Jugcraft.LOGGER.info("[encyclopedia-check] the inventory button: {}", placed);
		check(placed.startsWith("ok"), "The inventory button should stand right of the recipe-book button over nothing else: " + placed);
		context.takeScreenshot("jugcraft_encyclopedia_inventory_button");

		// The recipe book opened: the button follows its button.
		context.runOnClient(client -> ((Button) EncyclopediaInventoryButton.anchor(client.gui.screen())).onPress(new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT, 0)));
		context.waitTicks(5);
		String moved = context.computeOnClient(EncyclopediaClientGameTests::placement);
		Jugcraft.LOGGER.info("[encyclopedia-check] with the recipe book open: {}", moved);
		check(moved.startsWith("ok"), "With the recipe book open the button should follow its button: " + moved);
		context.takeScreenshot("jugcraft_encyclopedia_recipe_book");
		context.runOnClient(client -> ((Button) EncyclopediaInventoryButton.anchor(client.gui.screen())).onPress(new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT, 0)));
		context.waitTicks(5);

		// A real click on it.
		double[] at = context.computeOnClient(client -> {
			AbstractWidget button = EncyclopediaInventoryButton.button(client.gui.screen());
			int scale = client.getWindow().getGuiScale();
			return new double[] {(button.getX() + button.getWidth() / 2.0) * scale, (button.getY() + button.getHeight() / 2.0) * scale};
		});
		context.getInput().setCursorPos(at[0], at[1]);
		context.getInput().pressMouse(InputConstants.MOUSE_BUTTON_LEFT);
		context.waitForScreen(EncyclopediaScreen.class);
		context.waitTicks(10);
		String returned = server.computeOnServer(minecraft -> {
			ServerPlayer player = first(minecraft);
			int grid = 0;
			for (int slot = 1; slot <= 4; slot++) {
				grid += player.inventoryMenu.getSlot(slot).getItem().getCount();
			}
			return grid + " in the grid, " + player.getInventory().countItem(Items.OAK_PLANKS) + " in the inventory";
		});
		Jugcraft.LOGGER.info("[encyclopedia-check] after the button: {}", returned);
		check(returned.equals("0 in the grid, 4 in the inventory"), "The button should close the inventory as Esc does: " + returned);
		check(context.computeOnClient(client -> ((EncyclopediaScreen) client.gui.screen()).back().inventory()), "Opened from the inventory, it goes back there");
		context.getInput().pressKey(InputConstants.KEY_ESCAPE);
		context.waitForScreen(InventoryScreen.class);
		context.getInput().pressKey(InputConstants.KEY_ESCAPE);
		context.waitForScreen(null);
	}

	/** Where the button stands on the open inventory screen: "ok ..." when it is beside its anchor and over nothing else. */
	private static String placement(Minecraft client) {
		Screen screen = client.gui.screen();
		AbstractWidget button = EncyclopediaInventoryButton.button(screen);
		AbstractWidget anchor = EncyclopediaInventoryButton.anchor(screen);
		if (button == null || !button.visible) {
			return "no button";
		}
		List<String> overlaps = overlaps(screen, button, anchor == null ? 0 : anchor.getX() - 104, (screen.height - 166) / 2);
		if (anchor != null && (button.getX() != anchor.getX() + anchor.getWidth() + EncyclopediaInventoryButton.GAP || button.getY() != anchor.getY())) {
			overlaps.add("not beside the recipe-book button at " + anchor.getX() + "," + anchor.getY());
		}
		return (overlaps.isEmpty() ? "ok " : "bad ") + button.getX() + "," + button.getY() + " " + overlaps;
	}

	/** Everything the button covers: a slot of the menu (the panel at left, top) or another visible widget. */
	private static List<String> overlaps(Screen screen, AbstractWidget button, int left, int top) {
		List<String> out = new ArrayList<>();
		if (screen instanceof AbstractContainerScreen<?> container) {
			for (Slot slot : container.getMenu().slots) {
				if (slot.isActive() && intersects(button, left + slot.x, top + slot.y, 16, 16)) {
					out.add("slot " + slot.index);
				}
			}
		}
		for (AbstractWidget other : Screens.getWidgets(screen)) {
			if (other != button && other.visible && intersects(button, other.getX(), other.getY(), other.getWidth(), other.getHeight())) {
				out.add(other.getClass().getSimpleName() + " " + other.getMessage().getString());
			}
		}
		return out;
	}

	private static boolean intersects(AbstractWidget button, int x, int y, int width, int height) {
		return button.getX() < x + width && x < button.getX() + button.getWidth() && button.getY() < y + height && y < button.getY() + button.getHeight();
	}

	private static void theRoute(ClientGameTestContext context, TestDedicatedServerContext server) {
		server.runOnServer(minecraft -> ConcordanceProgress.record(first(minecraft), new Evidence.Examined("minecraft:amethyst_shard", 0)));
		open(context);
		EncyclopediaPayload.StepState dark = step(context, "dark");
		Jugcraft.LOGGER.info("[encyclopedia-check] examined in the dark: dark {}, understand {}", dark, step(context, "understand"));
		check(dark == EncyclopediaPayload.StepState.DONE, "Examined in the dark, the dark step should be done: " + dark);
		context.takeScreenshot("jugcraft_encyclopedia_discover_route");

		server.runOnServer(minecraft -> {
			ServerPlayer player = first(minecraft);
			ConcordanceProgress.grant(player, "jugcraft:first_light", ResearchState.UNDERSTOOD);
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(JugcraftConcordance.INITIATE_WAND));
		});
		context.waitTicks(15);
		context.getInput().pressKey(InputConstants.KEY_R);
		answered(context);
		EncyclopediaPayload.StepState cast = step(context, "cast");
		check(cast == EncyclopediaPayload.StepState.NEXT, "Understood with a wand in hand, casting comes next: " + cast);
		context.runOnClient(client -> {
			EncyclopediaClient.navigator().go("spellcraft");
			EncyclopediaClient.changed();
		});
		context.waitTicks(5);
		context.takeScreenshot("jugcraft_encyclopedia_spellcraft");

		// The composer's link opens the Spell Composer (a screen or, with GuiLib, its workspace); its Esc comes back here.
		context.runOnClient(client -> {
			AbstractWidget link = ((EncyclopediaScreen) client.gui.screen()).find(Component.translatable("encyclopedia.jugcraft.link.composer"));
			((Button) link).onPress(new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT, 0));
		});
		context.waitFor(client -> client.gui.screen() != null && !(client.gui.screen() instanceof EncyclopediaScreen), WAIT);
		context.waitTicks(5);
		context.getInput().pressKey(InputConstants.KEY_ESCAPE);
		context.waitForScreen(EncyclopediaScreen.class);
		context.getInput().pressKey(InputConstants.KEY_ESCAPE);
		context.waitForScreen(null);

		// Ticket A-07's runtime view of two first-loadout pieces only a working or a ritual makes: each in hand, as the
		// first loadout's record cites them. The wand goes back in hand after.
		for (String held : List.of("kindled_lantern", "adept_wand")) {
			server.runOnServer(minecraft -> first(minecraft).setItemInHand(InteractionHand.MAIN_HAND,
					new ItemStack(held.equals("kindled_lantern") ? JugcraftConcordance.KINDLED_LANTERN : JugcraftConcordance.ADEPT_WAND)));
			context.waitTicks(10);
			context.takeScreenshot("jugcraft_first_loadout_" + held);
		}
		server.runOnServer(minecraft -> first(minecraft).setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(JugcraftConcordance.INITIATE_WAND)));
		context.waitTicks(5);
	}

	private static void inspection(ClientGameTestContext context, TestDedicatedServerContext server) {
		BlockPos stand = context.computeOnClient(client -> client.player.blockPosition());
		BlockPos anchor = stand.offset(0, 0, 3);
		server.runOnServer(minecraft -> {
			ServerLevel level = minecraft.overworld();
			for (int dz = 1; dz <= 2; dz++) {
				level.setBlockAndUpdate(stand.offset(0, 0, dz), Blocks.AIR.defaultBlockState());
				level.setBlockAndUpdate(stand.offset(0, 1, dz), Blocks.AIR.defaultBlockState());
			}
			level.setBlockAndUpdate(anchor, JugcraftConcordance.CIRCLE_ANCHOR.defaultBlockState());
		});
		context.waitTicks(10);
		context.getInput().lookAt(anchor);
		context.waitTicks(5);
		open(context);
		String page = context.computeOnClient(client -> ((EncyclopediaScreen) client.gui.screen()).page());
		List<String> report = server.computeOnServer(minecraft -> {
			ServerLevel level = minecraft.overworld();
			List<String> lines = new ArrayList<>();
			((CircleAnchorBlockEntity) level.getBlockEntity(anchor)).status(level, line -> lines.add(line.getString()));
			return lines;
		});
		List<String> shown = context.computeOnClient(client -> {
			EncyclopediaPayload.Page current = EncyclopediaClient.navigator().current();
			return current == null ? List.<String>of() : current.rows().stream().skip(1).limit(report.size()).map(row -> row.text().getString()).toList();
		});
		Jugcraft.LOGGER.info("[encyclopedia-check] looking at an anchor: page {}, lines {}", page, shown);
		check(page.equals("inspect") && shown.equals(report), "Looking at an anchor, the Inspect page should be its own report: " + shown + " vs " + report);
		context.takeScreenshot("jugcraft_encyclopedia_inspect");
		context.getInput().pressKey(InputConstants.KEY_ESCAPE);
		context.waitForScreen(null);
		context.getInput().lookAt(0.0F, 30.0F);
		context.waitTicks(5);
	}

	private static void expeditionsAndKeys(ClientGameTestContext context, TestDedicatedServerContext server) {
		server.runOnServer(minecraft -> {
			ServerPlayer player = first(minecraft);
			player.getInventory().add(new ItemStack(JugcraftLairs.DEATH_KNELL));
			TrickOrTreat.award(player, "the_last_harvest");
		});
		context.waitTicks(10);
		open(context);
		context.waitFor(client -> EncyclopediaClient.navigator().pages().stream().anyMatch(page -> page.id().equals("expedition")), WAIT);
		context.runOnClient(client -> {
			EncyclopediaClient.navigator().go("expedition");
			EncyclopediaClient.changed();
		});
		context.waitTicks(5);
		context.takeScreenshot("jugcraft_encyclopedia_expedition");

		context.runOnClient(client -> {
			EncyclopediaClient.navigator().go("keys");
			EncyclopediaClient.changed();
		});
		context.waitTicks(5);
		List<String> missing = context.computeOnClient(client -> {
			List<String> lines = EncyclopediaClient.navigator().current().rows().stream().map(row -> row.text().getString()).toList();
			List<String> out = new ArrayList<>();
			for (KeyMapping mapping : client.options.keyMappings) {
				String name = Component.translatable(mapping.getName()).getString();
				if (mapping.getName().startsWith("key.jugcraft.") && lines.stream().noneMatch(line -> line.startsWith(name + ":"))) {
					out.add(mapping.getName());
				}
			}
			return out;
		});
		Jugcraft.LOGGER.info("[encyclopedia-check] Jugcraft keys missing from the Keys page: {}", missing);
		check(missing.isEmpty(), "The Keys page should list every Jugcraft key: missing " + missing);
		context.takeScreenshot("jugcraft_encyclopedia_keys");
		context.getInput().pressKey(InputConstants.KEY_ESCAPE);
		context.waitForScreen(null);
	}

	private static void creative(ClientGameTestContext context, TestDedicatedServerContext server) {
		server.runCommand("gamemode creative @a");
		context.waitTicks(10);
		context.getInput().pressKey(options -> options.keyInventory);
		context.waitForScreen(CreativeModeInventoryScreen.class);
		context.runOnClient(client -> ((FabricCreativeModeInventoryScreen) client.gui.screen()).setSelectedTab(tab(CreativeModeTab.Type.INVENTORY)));
		context.waitTicks(5);
		String placed = context.computeOnClient(client -> {
			Screen screen = client.gui.screen();
			AbstractWidget button = EncyclopediaInventoryButton.button(screen);
			if (button == null || !button.visible) {
				return "no button";
			}
			List<String> overlaps = overlaps(screen, button, (screen.width - 195) / 2, (screen.height - 136) / 2);
			return (overlaps.isEmpty() ? "ok " : "bad ") + button.getX() + "," + button.getY() + " " + overlaps;
		});
		Jugcraft.LOGGER.info("[encyclopedia-check] the creative Survival Inventory tab: {}", placed);
		check(placed.startsWith("ok"), "On the creative Survival Inventory tab the button should show over nothing else: " + placed);
		context.takeScreenshot("jugcraft_encyclopedia_creative_button");
		context.runOnClient(client -> ((FabricCreativeModeInventoryScreen) client.gui.screen()).setSelectedTab(CreativeModeTabs.getDefaultTab()));
		context.waitTicks(5);
		boolean hidden = context.computeOnClient(client -> {
			AbstractWidget button = EncyclopediaInventoryButton.button(client.gui.screen());
			return button != null && !button.visible;
		});
		check(hidden, "On any other creative tab the button should be hidden");
		context.getInput().pressKey(InputConstants.KEY_ESCAPE);
		context.waitForScreen(null);
		server.runCommand("gamemode survival @a");
		context.waitTicks(10);
	}

	private static void sizes(ClientGameTestContext context) {
		open(context);
		int scale = context.computeOnClient(client -> client.options.guiScale().get());
		context.runOnClient(client -> {
			client.options.guiScale().set(4);
			client.resizeGui();
		});
		context.waitTicks(10);
		context.takeScreenshot("jugcraft_encyclopedia_scale4");
		context.runOnClient(client -> {
			client.options.guiScale().set(scale);
			client.resizeGui();
		});
		// Last, a small window (the runner restores the window's size after each test).
		context.getInput().resizeWindow(640, 360);
		context.waitTicks(10);
		context.takeScreenshot("jugcraft_encyclopedia_small");
		context.setScreen(() -> null);
	}

	/** Opens the encyclopedia with its key and waits for the answer to that opening. */
	private static void open(ClientGameTestContext context) {
		context.getInput().pressKey(EncyclopediaClient.key());
		context.waitForScreen(EncyclopediaScreen.class);
		answered(context);
		context.waitTicks(15);
	}

	/**
	 * Waits for the server's answer to the client's latest request, not the pages it already holds (a page that was
	 * already there, such as Spellcraft once First Light is begun, says nothing about whether the answer has come).
	 */
	private static void answered(ClientGameTestContext context) {
		int asked = context.computeOnClient(client -> EncyclopediaClient.sequence());
		context.waitFor(client -> EncyclopediaClient.navigator().applied() >= asked, WAIT);
	}

	/** Where a step of the route stands on the Discover page as the client has it. */
	private static EncyclopediaPayload.StepState step(ClientGameTestContext context, String id) {
		return context.computeOnClient(client -> EncyclopediaClient.navigator().pages().stream().filter(page -> page.id().equals("discover"))
				.flatMap(page -> page.rows().stream())
				.filter(row -> row.kindOf() == EncyclopediaPayload.Kind.STEP && row.text().getContents() instanceof TranslatableContents key
						&& (key.getKey().equals("encyclopedia.jugcraft.route." + id) || key.getKey().startsWith("encyclopedia.jugcraft.route." + id + ".")))
				.map(EncyclopediaPayload.Row::stateOf).findFirst().orElse(null));
	}

	private static CreativeModeTab tab(CreativeModeTab.Type type) {
		return CreativeModeTabs.allTabs().stream().filter(tab -> tab.getType() == type).findFirst().orElseThrow();
	}

	private static ServerPlayer first(MinecraftServer minecraft) {
		return minecraft.getPlayerList().getPlayers().get(0);
	}

	private static void check(boolean condition, String message) {
		if (!condition) {
			throw new AssertionError(message);
		}
	}
}
