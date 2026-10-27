package io.github.jimbozoomer.jugcraft.test;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.agriculture.TrickOrTreat;
import io.github.jimbozoomer.jugcraft.client.EncyclopediaClient;
import io.github.jimbozoomer.jugcraft.client.EncyclopediaInventoryButton;
import io.github.jimbozoomer.jugcraft.client.EncyclopediaScreen;
import io.github.jimbozoomer.jugcraft.compat.jade.ConcordanceDataProvider;
import io.github.jimbozoomer.jugcraft.concordance.Alchemy;
import io.github.jimbozoomer.jugcraft.concordance.CircleAnchorBlockEntity;
import io.github.jimbozoomer.jugcraft.concordance.ComposeText;
import io.github.jimbozoomer.jugcraft.concordance.ConcordanceData;
import io.github.jimbozoomer.jugcraft.concordance.ConcordanceProgress;
import io.github.jimbozoomer.jugcraft.concordance.CrucibleBlockEntity;
import io.github.jimbozoomer.jugcraft.concordance.JugcraftConcordance;
import io.github.jimbozoomer.jugcraft.concordance.LeyPylonBlockEntity;
import io.github.jimbozoomer.jugcraft.concordance.RateGate;
import io.github.jimbozoomer.jugcraft.concordance.alchemy.Band;
import io.github.jimbozoomer.jugcraft.concordance.alchemy.Formula;
import io.github.jimbozoomer.jugcraft.concordance.diagnostic.Diagnostic;
import io.github.jimbozoomer.jugcraft.concordance.garden.Garden;
import io.github.jimbozoomer.jugcraft.concordance.garden.OrganismCropBlock;
import io.github.jimbozoomer.jugcraft.concordance.garden.VerdantBedBlock;
import io.github.jimbozoomer.jugcraft.concordance.garden.VerdantBedBlockEntity;
import io.github.jimbozoomer.jugcraft.concordance.ritual.Readiness;
import io.github.jimbozoomer.jugcraft.concordance.ritual.StructurePattern;
import io.github.jimbozoomer.jugcraft.concordance.rules.Evidence;
import io.github.jimbozoomer.jugcraft.concordance.rules.ResearchState;
import io.github.jimbozoomer.jugcraft.energy.CreativeEnergyCellBlock;
import io.github.jimbozoomer.jugcraft.fluid.ElectricPumpBlockEntity;
import io.github.jimbozoomer.jugcraft.fluid.JugcraftFluids;
import io.github.jimbozoomer.jugcraft.guide.encyclopedia.EncyclopediaPayload;
import io.github.jimbozoomer.jugcraft.lair.JugcraftLairs;
import io.github.jimbozoomer.jugcraft.lair.Lair;
import io.github.jimbozoomer.jugcraft.lair.LairDossiers;
import io.github.jimbozoomer.jugcraft.lair.tyrant.CinderTyrantLoot;
import io.github.jimbozoomer.jugcraft.lair.yeti.YetiKingLoot;
import io.github.jimbozoomer.jugcraft.machine.JugcraftMachines;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.fabricmc.fabric.api.client.creativetab.v1.FabricCreativeModeInventoryScreen;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerConnection;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerContext;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractContainerWidget;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;

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
 * <li><b>A crucible</b> (Concordance Chunk C, C-01). Following the Kiln formula over a lit campfire, it waits for its
 * magma cream: the reason the server encodes reads back on the client in words, with no key or id showing. Jade shows
 * it when loaded ({@code jugcraft_concordance_crucible_jade}); the Inspect page, opened looking at it by a master
 * carrying an Assay Glass, says it with the rest ({@code jugcraft_encyclopedia_inspect_crucible}, and at GUI scale 4
 * {@code jugcraft_encyclopedia_inspect_crucible_scale4}). Since C-02, set to a dirty Stillroot Poultice (bone meal
 * stirred in with the Mendvetch), the formula's preview names what fouled its Verdance
 * ({@code jugcraft_encyclopedia_inspect_poultice}).</li>
 * <li><b>A Crucible Stand</b> (Concordance Chunk C, C-05): a small workshop as a player builds it, a crucible following the
 * Kiln formula on a stand fed through a copper cable and an Electric Pump piping its water. Lit for the hot stir
 * ({@code jugcraft_concordance_crucible_stand_workshop}); the crucible's Inspect page names the stand heating it, with no
 * key or id; Jade on the stand when loaded ({@code jugcraft_concordance_crucible_stand_jade}).</li>
 * <li><b>Readiness</b> (Concordance Chunk C, C-03): an anchor two glowstone short, beside a pylon short of a step and one
 * short of the run; the Inspect page shows the server's lines as decoded and worded on this client, and the stage table
 * ({@code jugcraft_encyclopedia_inspect_readiness}); then, walled and roofed so it is dark at any hour (the player given
 * night vision, which changes only what the client draws, so the shot is not black), the Lumen Vigil gathering, in Jade
 * ({@code jugcraft_concordance_anchor_gathering_jade}).</li>
 * <li><b>Root and Rivet's foundation</b> (Concordance Chunk C, C-04): the Rooted Thallite Foundation held, in the hand and
 * the hotbar, so its icon is seen in game ({@code jugcraft_rooted_thallite_foundation}; ITEM_ICONS step 7).</li>
 * <li><b>A garden</b> (Concordance Chunk C, C-07): a Sunpetal grown ripe in a lit Verdant Bed, with random ticks held
 * still so only the test grows it; Jade on the ripe crop shows its bed's last verdict, as it grew
 * ({@code jugcraft_concordance_ripe_crop_jade}); the Inspect page, opened looking at the bed, judges each factor in words,
 * the light as sky and lamps ({@code jugcraft_encyclopedia_inspect_bed}).</li>
 * <li><b>Expeditions and keys</b> ({@code jugcraft_encyclopedia_expedition}, {@code jugcraft_encyclopedia_keys}), and the
 * screen at GUI scale 4 and in a small window ({@code jugcraft_encyclopedia_scale4}, {@code jugcraft_encyclopedia_small}).
 * Since Concordance Chunk B (B-11), with the Frost Horn and the Kiln Seal carried and both keepers beaten, Expeditions
 * scrolled to the Glacier Hall's and the Cinder Kiln's dossiers ({@code jugcraft_encyclopedia_expedition_glacier_hall},
 * {@code jugcraft_encyclopedia_expedition_cinder_kiln}).</li>
 * </ul>
 */
public class EncyclopediaClientGameTests implements FabricClientGameTest {
	private static final int WAIT = 200;
	/** A Kiln Draught's formula: magma cream stirred hot twice. */
	private static final String KILN_FORMULA = "water 1; add minecraft:magma_cream jugcraft:raw; stir hot; stir hot";
	/** A dirty Stillroot Poultice's formula (C-02): bone meal stirred in with the Mendvetch fouls its Verdance. */
	private static final String DIRTY_POULTICE = "water 1; add jugcraft:mendvetch jugcraft:raw; add jugcraft:mendvetch jugcraft:raw; "
			+ "add minecraft:bone_meal jugcraft:raw; stir warm; stir warm; stir warm; stir warm";

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
				crucibleInspection(context, server);
				crucibleStandWorkshop(context, server);
				anchorReadiness(context, server);
				rootedFoundation(context, server);
				gardenInspection(context, server);
				expeditionsAndKeys(context, server);
				creative(context, server);
				sizes(context);
			}
		}
	}

	private static void theKey(ClientGameTestContext context) {
		KeyMapping key = EncyclopediaClient.key();
		String clash = context.computeOnClient(client -> Arrays.stream(client.options.keyMappings)
				.filter(other -> other != key && other.getCategory() != KeyMapping.Category.DEBUG && other.same(key))
				.map(KeyMapping::getName).toList().toString());
		// Every loaded default outside the debug keys, so one run shows which keys a new default could still take.
		String taken = context.computeOnClient(client -> Arrays.stream(client.options.keyMappings)
				.filter(other -> other.getCategory() != KeyMapping.Category.DEBUG && !other.getDefaultKey().equals(InputConstants.UNKNOWN))
				.map(other -> other.getDefaultKey().getName() + "=" + other.getName()).sorted().toList().toString());
		Jugcraft.LOGGER.info("[encyclopedia-check] loaded defaults: {}", taken);
		// Complete supplies options.txt overrides (Traveler's Backpack uses N instead of its upstream B).
		Jugcraft.LOGGER.info("[encyclopedia-check] the configured key {}; other keys with that binding: {}", key.saveString(), clash);
		check(!key.isUnbound() && clash.equals("[]"), "The encyclopedia's configured key should be bound and shared with no other key: " + clash);

		context.getInput().pressKey(key);
		context.waitForScreen(EncyclopediaScreen.class);
		context.waitFor(client -> EncyclopediaClient.navigator().received(), WAIT);
		context.waitTicks(5);
		String page = context.computeOnClient(client -> ((EncyclopediaScreen) client.gui.screen()).page());
		EncyclopediaPayload.StepState find = step(context, "find");
		Jugcraft.LOGGER.info("[encyclopedia-check] opened by its key: page {}, find {}", page, find);
		check(page.equals("discover") && find == EncyclopediaPayload.StepState.NEXT, "A newcomer should open on Discover with find next: " + page + " " + find);
		context.runOnClient(client -> client.gui.toastManager().clear());
                context.waitTicks(2);
                context.takeScreenshot("jugcraft_encyclopedia_discover_new");
        check(context.computeOnClient(client -> ((EncyclopediaScreen) client.gui.screen()).isHome()), "The guide should open on the simple contents spread");
        context.clickScreenButton("ui.jugcraft.book.discover");
        context.waitTicks(3);
        check(context.computeOnClient(client -> !((EncyclopediaScreen) client.gui.screen()).isHome()), "First steps should open the topic directory");
        context.takeScreenshot("jugcraft_field_guide_first_steps");

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
		context.runOnClient(client -> client.gui.toastManager().clear());
                context.waitTicks(2);
                context.takeScreenshot("jugcraft_encyclopedia_inventory_button");

		// The recipe book opened: the button follows its button.
		context.runOnClient(client -> ((Button) EncyclopediaInventoryButton.anchor(client.gui.screen())).onPress(new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT, 0)));
		context.waitTicks(5);
		String moved = context.computeOnClient(EncyclopediaClientGameTests::placement);
		Jugcraft.LOGGER.info("[encyclopedia-check] with the recipe book open: {}", moved);
		check(moved.startsWith("ok"), "With the recipe book open the button should follow its button: " + moved);
		context.runOnClient(client -> client.gui.toastManager().clear());
                context.waitTicks(2);
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
		context.runOnClient(client -> client.gui.toastManager().clear());
                context.waitTicks(2);
                context.takeScreenshot("jugcraft_encyclopedia_discover_route");

		server.runOnServer(minecraft -> {
			ServerPlayer player = first(minecraft);
			ConcordanceProgress.grant(player, "jugcraft:first_light", ResearchState.UNDERSTOOD);
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(JugcraftConcordance.INITIATE_WAND));
		});
		context.waitTicks(15);
		context.getInput().pressKey(InputConstants.KEY_F6);
		answered(context);
		EncyclopediaPayload.StepState cast = step(context, "cast");
		check(cast == EncyclopediaPayload.StepState.NEXT, "Understood with a wand in hand, casting comes next: " + cast);
		context.runOnClient(client -> {
			((EncyclopediaScreen) client.gui.screen()).showPage("spellcraft");
			EncyclopediaClient.changed();
		});
		context.waitTicks(5);
		context.runOnClient(client -> client.gui.toastManager().clear());
                context.waitTicks(2);
                context.takeScreenshot("jugcraft_encyclopedia_spellcraft");

		// The composer's link opens the Spell Composer (a screen or, with GuiLib, its workspace); its Esc comes back here.
		context.runOnClient(client -> {
			((EncyclopediaScreen) client.gui.screen()).showSectionContaining(Component.translatable("encyclopedia.jugcraft.link.composer"));
            });
            context.waitTicks(3);
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
			context.runOnClient(client -> client.gui.toastManager().clear());
                context.waitTicks(2);
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
		context.runOnClient(client -> client.gui.toastManager().clear());
                context.waitTicks(2);
                context.takeScreenshot("jugcraft_encyclopedia_inspect");
		context.getInput().pressKey(InputConstants.KEY_ESCAPE);
		context.waitForScreen(null);
		context.getInput().lookAt(0.0F, 30.0F);
		context.waitTicks(5);
	}

	private static void crucibleInspection(ClientGameTestContext context, TestDedicatedServerContext server) {
		BlockPos stand = context.computeOnClient(client -> client.player.blockPosition());
		BlockPos fire = stand.offset(0, 0, 2);
		BlockPos pot = fire.above();
		boolean set = server.computeOnServer(minecraft -> {
			ServerLevel level = minecraft.overworld();
			for (int dz = 1; dz <= 2; dz++) {
				level.setBlockAndUpdate(stand.offset(0, 0, dz), Blocks.AIR.defaultBlockState());
				level.setBlockAndUpdate(stand.offset(0, 1, dz), Blocks.AIR.defaultBlockState());
			}
			level.setBlockAndUpdate(fire, Blocks.CAMPFIRE.defaultBlockState());
			level.setBlockAndUpdate(pot, JugcraftConcordance.CRUCIBLE.defaultBlockState());
			ServerPlayer player = first(minecraft);
			ConcordanceProgress.grant(player, "jugcraft:first_light", ResearchState.UNDERSTOOD);
			ConcordanceProgress.grant(player, Alchemy.RESEARCH, ResearchState.MASTERED);
			player.getInventory().add(new ItemStack(JugcraftConcordance.ASSAY_GLASS));
			if (!(level.getBlockEntity(pot) instanceof CrucibleBlockEntity crucible)) {
				return false;
			}
			// Set as the formula item sets it, without its checks: no permission rule of this server can refuse it.
			crucible.follow(Formula.parse(KILN_FORMULA));
			try (Transaction transaction = Transaction.openOuter()) {
				crucible.water.insert(FluidVariant.of(Fluids.WATER), FluidConstants.BOTTLE * 2, transaction);
				transaction.commit();
			}
			return crucible.program() != null;
		});
		check(set, "The crucible should follow the Kiln formula");
		// Its first pulse pours a part; its second finds no magma cream in its buffer.
		for (int wait = 0; wait < WAIT && !server.computeOnServer(minecraft -> minecraft.overworld().getBlockEntity(pot) instanceof CrucibleBlockEntity crucible
				&& crucible.step() == 1 && crucible.waiting() != null); wait++) {
			context.waitTick();
		}
		String encoded = server.computeOnServer(minecraft -> minecraft.overworld().getBlockEntity(pot) instanceof CrucibleBlockEntity crucible
				&& crucible.waiting() != null ? crucible.waiting().encode() : "");
		String worded = context.computeOnClient(client -> {
			Diagnostic reason = Diagnostic.decode(encoded);
			return reason == null ? "" : ComposeText.show(reason.text()).getString();
		});
		String cream = context.computeOnClient(client -> I18n.get("item.minecraft.magma_cream"));
		Jugcraft.LOGGER.info("[encyclopedia-check] the crucible waits: sent {}, worded on the client \"{}\"", encoded, worded);
		check(!worded.isEmpty() && worded.contains(cream) && !worded.contains("jugcraft.") && !worded.contains("jugcraft:"),
				"The crucible's reason should read back in words naming the magma cream, with no key or id: \"" + worded + "\" from " + encoded);

		context.getInput().lookAt(pot);
		context.waitTicks(20);
		if (FabricLoader.getInstance().isModLoaded("jade")) {
			context.runOnClient(client -> client.gui.toastManager().clear());   // the research's toasts
			context.waitTicks(5);
			context.takeScreenshot("jugcraft_concordance_crucible_jade");
		} else {
			Jugcraft.LOGGER.info("[encyclopedia-check] Jade is not loaded: no shot of its crucible lines");
		}

		open(context);
		String page = context.computeOnClient(client -> ((EncyclopediaScreen) client.gui.screen()).page());
		List<String> rows = context.computeOnClient(client -> {
			EncyclopediaPayload.Page current = EncyclopediaClient.navigator().current();
			return current == null ? List.<String>of() : current.rows().stream().map(row -> row.text().getString()).toList();
		});
		Jugcraft.LOGGER.info("[encyclopedia-check] looking at a crucible: page {}, rows {}", page, rows);
		check(page.equals("inspect") && rows.stream().anyMatch(row -> row.contains(worded))
				&& rows.stream().noneMatch(row -> row.contains("jugcraft.") || row.contains("jugcraft:")),
				"Looking at a crucible, the Inspect page should give its reason, with no key or id: " + rows);
		context.runOnClient(client -> client.gui.toastManager().clear());
		context.waitTicks(5);
		context.takeScreenshot("jugcraft_encyclopedia_inspect_crucible");
		int scale = context.computeOnClient(client -> client.options.guiScale().get());
		context.runOnClient(client -> {
			client.options.guiScale().set(4);
			client.resizeGui();
			client.gui.toastManager().clear();
		});
		context.waitTicks(10);
		context.takeScreenshot("jugcraft_encyclopedia_inspect_crucible_scale4");
		context.runOnClient(client -> {
			client.options.guiScale().set(scale);
			client.resizeGui();
		});
		context.getInput().pressKey(InputConstants.KEY_ESCAPE);
		context.waitForScreen(null);

		// C-02 (C-P7): set to a dirty Stillroot Poultice, the formula's preview names what fouled its Verdance.
		boolean dirty = server.computeOnServer(minecraft -> {
			if (!(minecraft.overworld().getBlockEntity(pot) instanceof CrucibleBlockEntity crucible)) {
				return false;
			}
			crucible.follow(Formula.parse(DIRTY_POULTICE));
			return crucible.program() != null;
		});
		check(dirty, "The crucible should follow the dirty poultice's formula");
		open(context);
		List<String> preview = context.computeOnClient(client -> {
			EncyclopediaPayload.Page current = EncyclopediaClient.navigator().current();
			List<String> found = new ArrayList<>();
			boolean under = false;
			for (EncyclopediaPayload.Row row : current == null ? List.<EncyclopediaPayload.Row>of() : current.rows()) {
				if (row.kindOf() == EncyclopediaPayload.Kind.HEADING) {
					under = holds(row.text(), "compose.jugcraft.alchemy.inspect.preview");
				} else if (under && holds(row.text(), "compose.jugcraft.alchemy.fouled")) {
					found.add(row.text().getString());
				}
			}
			return found;
		});
		Jugcraft.LOGGER.info("[encyclopedia-check] a dirty poultice's preview: fouled {}", preview);
		check(!preview.isEmpty() && preview.stream().noneMatch(row -> row.contains("jugcraft.") || row.contains("jugcraft:")),
				"The dirty poultice's preview should say what fouled its Verdance, in words: " + preview);
		context.runOnClient(client -> client.gui.toastManager().clear());
		context.waitTicks(5);
		context.takeScreenshot("jugcraft_encyclopedia_inspect_poultice");
		context.getInput().pressKey(InputConstants.KEY_ESCAPE);
		context.waitForScreen(null);
		context.getInput().lookAt(0.0F, 30.0F);
		// The crucible and its fire taken away again (no bottle went in, so nothing drops) and the Assay Glass put back. The
		// Alembic Arts stay mastered, so the later Expeditions shots carry the codex links that research opens (the Winter
		// Brine's and the Kiln Draught's).
		server.runOnServer(minecraft -> {
			ServerLevel level = minecraft.overworld();
			level.setBlockAndUpdate(pot, Blocks.AIR.defaultBlockState());
			level.setBlockAndUpdate(fire, Blocks.AIR.defaultBlockState());
			Inventory inventory = first(minecraft).getInventory();
			for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
				if (inventory.getItem(slot).is(JugcraftConcordance.ASSAY_GLASS)) {
					inventory.setItem(slot, ItemStack.EMPTY);
				}
			}
		});
		context.waitTicks(5);
	}

	/**
	 * Concordance Chunk C, C-05: a small workshop as a player builds it, at the ordinary GUI scale: a crucible following the
	 * Kiln formula on a Crucible Stand fed by a Creative Energy Cell through a copper cable, and an Electric Pump on a spring
	 * piping into its tank. The crucible's Inspect page names the stand heating it; Jade on the stand, if loaded, is shot too.
	 */
	private static void crucibleStandWorkshop(ClientGameTestContext context, TestDedicatedServerContext server) {
		BlockPos feet = context.computeOnClient(client -> client.player.blockPosition());
		BlockPos base = feet.offset(0, 0, 3);
		BlockPos pot = base.above();
		BlockPos cable = base.west();
		BlockPos cell = cable.west();
		BlockPos spring = base.east(2);
		BlockPos pump = spring.above();
		List<BlockPos> walls = List.of(spring.west(), spring.east(), spring.north(), spring.south());
		boolean built = server.computeOnServer(minecraft -> {
			ServerLevel level = minecraft.overworld();
			for (int dx = -2; dx <= 3; dx++) {
				for (int dy = 0; dy <= 2; dy++) {
					for (int dz = 2; dz <= 4; dz++) {
						level.setBlockAndUpdate(feet.offset(dx, dy, dz), Blocks.AIR.defaultBlockState());
					}
				}
			}
			walls.forEach(wall -> level.setBlockAndUpdate(wall, Blocks.STONE.defaultBlockState()));
			level.setBlockAndUpdate(spring, Blocks.WATER.defaultBlockState());
			level.setBlockAndUpdate(cell, CreativeEnergyCellBlock.BLOCK.defaultBlockState());
			level.setBlockAndUpdate(cable, JugcraftMachines.COPPER_CABLE.defaultBlockState());
			level.setBlockAndUpdate(base, JugcraftConcordance.CRUCIBLE_STAND.defaultBlockState());
			level.setBlockAndUpdate(pot, JugcraftConcordance.CRUCIBLE.defaultBlockState());
			level.setBlockAndUpdate(pump, JugcraftFluids.ELECTRIC_PUMP.defaultBlockState());
			level.setBlockAndUpdate(pump.west(), JugcraftFluids.BRONZE_FLUID_PIPE.defaultBlockState());
			if (!(level.getBlockEntity(pump) instanceof ElectricPumpBlockEntity pumping) || !(level.getBlockEntity(pot) instanceof CrucibleBlockEntity crucible)) {
				return false;
			}
			pumping.energy().setAmount(ElectricPumpBlockEntity.ENERGY_CAPACITY);
			crucible.follow(Formula.parse(KILN_FORMULA)); // no permission rule of this server can refuse it
			crucible.setItem(0, new ItemStack(Items.MAGMA_CREAM));
			crucible.setItem(CrucibleBlockEntity.BOTTLE_SLOT, new ItemStack(Items.GLASS_BOTTLE));
			return true;
		});
		check(built, "The workshop should be built");
		for (int wait = 0; wait < WAIT && !server.computeOnServer(minecraft -> minecraft.overworld().getBlockEntity(pot) instanceof CrucibleBlockEntity crucible
				&& crucible.band() == Band.HOT && crucible.parts() > 0); wait++) {
			context.waitTick();
		}
		boolean lit = server.computeOnServer(minecraft -> minecraft.overworld().getBlockState(base).getValue(BlockStateProperties.LIT));
		Jugcraft.LOGGER.info("[encyclopedia-check] the workshop's stand is lit: {}", lit);
		check(lit, "The stand should be lit, heating the crucible for its hot stir");
		context.getInput().lookAt(pot);
		context.waitTicks(20);
		context.runOnClient(client -> client.gui.toastManager().clear());
		context.waitTicks(5);
		context.takeScreenshot("jugcraft_concordance_crucible_stand_workshop");
		String standName = context.computeOnClient(client -> I18n.get("block.jugcraft.crucible_stand"));
		open(context);
		List<String> rows = context.computeOnClient(client -> {
			EncyclopediaPayload.Page current = EncyclopediaClient.navigator().current();
			return current == null ? List.<String>of() : current.rows().stream().map(row -> row.text().getString()).toList();
		});
		Jugcraft.LOGGER.info("[encyclopedia-check] looking at a crucible on a stand: rows {}", rows);
		check(rows.stream().anyMatch(row -> row.contains(standName)) && rows.stream().noneMatch(row -> row.contains("jugcraft.") || row.contains("jugcraft:")),
				"The crucible's Inspect page should name the Crucible Stand heating it, with no key or id: " + rows);
		context.getInput().pressKey(InputConstants.KEY_ESCAPE);
		context.waitForScreen(null);
		context.getInput().lookAt(base);
		context.waitTicks(20);
		if (FabricLoader.getInstance().isModLoaded("jade")) {
			context.runOnClient(client -> client.gui.toastManager().clear());
			context.waitTicks(5);
			context.takeScreenshot("jugcraft_concordance_crucible_stand_jade");
		} else {
			Jugcraft.LOGGER.info("[encyclopedia-check] Jade is not loaded: no shot of the stand's lines");
		}
		context.getInput().lookAt(0.0F, 30.0F);
		server.runOnServer(minecraft -> {
			ServerLevel level = minecraft.overworld();
			if (level.getBlockEntity(pot) instanceof CrucibleBlockEntity crucible) {
				crucible.clearContent(); // nothing drops
			}
			for (BlockPos at : List.of(pot, base, cable, cell, pump.west(), pump, spring, walls.get(0), walls.get(1), walls.get(2), walls.get(3))) {
				level.setBlockAndUpdate(at, Blocks.AIR.defaultBlockState());
			}
		});
		context.waitTicks(5);
	}

	/**
	 * C-03: an anchor two glowstone short, beside a pylon short of a step and one short of the whole run. The server's
	 * readiness lines, encoded as Jade carries a diagnostic, read back on this client in words with no key or id, and the
	 * Inspect page shows each with the rite's stage table. Then, walled and roofed so the vigil's light check holds at any
	 * hour, the Lumen Vigil gathers, and Jade's snapshot shows who it needs, what it has spent and what it holds.
	 */
	private static void anchorReadiness(ClientGameTestContext context, TestDedicatedServerContext server) {
		BlockPos stand = context.computeOnClient(client -> client.player.blockPosition());
		BlockPos anchor = stand.offset(1, 0, 2); // the player stands at (-1, 0, -2) from it: inside the ring, on no part
		List<String> encoded = server.computeOnServer(minecraft -> {
			ServerLevel level = minecraft.overworld();
			for (int dx = -4; dx <= 4; dx++) {
				for (int dz = -4; dz <= 4; dz++) {
					level.setBlockAndUpdate(anchor.offset(dx, -1, dz), Blocks.STONE.defaultBlockState());
					for (int dy = 0; dy <= 3; dy++) {
						level.setBlockAndUpdate(anchor.offset(dx, dy, dz), Blocks.AIR.defaultBlockState());
					}
				}
			}
			level.setBlockAndUpdate(anchor, JugcraftConcordance.CIRCLE_ANCHOR.defaultBlockState());
			int channel = 0;
			for (StructurePattern.Part part : ConcordanceData.rules().structure("jugcraft:lesser_circle").parts()) {
				BlockPos at = anchor.offset(part.offset().x(), part.offset().y(), part.offset().z());
				if (part.role() == StructurePattern.Role.CHANNEL) {
					level.setBlockAndUpdate(at, JugcraftConcordance.LEY_PYLON.defaultBlockState());
					((LeyPylonBlockEntity) level.getBlockEntity(at)).setLey(level, channel == 0 ? 1 : channel == 1 ? 9 : 64);
					channel++;
				} else if (part.role() == StructurePattern.Role.BOUNDARY) {
					level.setBlockAndUpdate(at, JugcraftConcordance.WARDING_STONE.defaultBlockState());
				}
			}
			ServerPlayer player = first(minecraft);
			ConcordanceProgress.grant(player, "jugcraft:first_light", ResearchState.UNDERSTOOD);
			ConcordanceProgress.grant(player, "jugcraft:circle_lore", ResearchState.UNDERSTOOD);
			ConcordanceProgress.setFocus(player, 20);
			CircleAnchorBlockEntity circle = (CircleAnchorBlockEntity) level.getBlockEntity(anchor);
			for (ItemStack stack : List.of(new ItemStack(JugcraftConcordance.INITIATE_WAND), new ItemStack(Items.AMETHYST_SHARD, 4),
					new ItemStack(Items.GOLD_INGOT, 2), new ItemStack(Items.GLOWSTONE_DUST, 2))) {
				circle.offer(player, stack);
			}
			Readiness readiness = circle.readiness(level, player);
			return readiness == null ? List.<String>of() : readiness.lines().stream().map(line -> line.diagnostic().encode()).toList();
		});
		check(encoded.size() == 3, "The anchor should lack two glowstone dust, a step at one pylon and the run at another: " + encoded);
		context.waitTicks(10);
		context.getInput().lookAt(anchor);
		context.waitTicks(5);
		open(context);
		String page = context.computeOnClient(client -> ((EncyclopediaScreen) client.gui.screen()).page());
		List<String> worded = context.computeOnClient(client -> encoded.stream().map(each -> {
			Diagnostic diagnostic = Diagnostic.decode(each);
			return diagnostic == null ? "" : ComposeText.show(diagnostic.text()).getString();
		}).toList());
		List<String> shown = context.computeOnClient(client -> {
			EncyclopediaPayload.Page current = EncyclopediaClient.navigator().current();
			return current == null ? List.<String>of() : current.rows().stream().map(row -> row.text().getString()).toList();
		});
		boolean table = context.computeOnClient(client -> {
			EncyclopediaPayload.Page current = EncyclopediaClient.navigator().current();
			return current != null && current.rows().stream().anyMatch(row -> row.text().getContents() instanceof TranslatableContents key
					&& key.getKey().equals("compose.jugcraft.ritual.stage.backlash"));
		});
		Jugcraft.LOGGER.info("[encyclopedia-check] an anchor's readiness: page {}, lines {}, stage table {}", page, worded, table);
		check(page.equals("inspect") && worded.stream().noneMatch(String::isEmpty) && shown.containsAll(worded) && table
				&& worded.stream().noneMatch(line -> line.contains("jugcraft.") || line.contains("jugcraft:")),
				"The Inspect page should list the anchor's shortfalls in words, with no key or id, and its table: " + shown);
		context.runOnClient(client -> client.gui.toastManager().clear());
		context.waitTicks(5);
		context.takeScreenshot("jugcraft_encyclopedia_inspect_readiness");
		context.getInput().pressKey(InputConstants.KEY_ESCAPE);
		context.waitForScreen(null);
		// The vigil needs light 7 or less above the anchor: walls and a roof make it dark whatever the sky gives at midnight.
		server.runOnServer(minecraft -> {
			ServerLevel level = minecraft.overworld();
			for (int dx = -4; dx <= 4; dx++) {
				for (int dz = -4; dz <= 4; dz++) {
					level.setBlockAndUpdate(anchor.offset(dx, 3, dz), Blocks.STONE.defaultBlockState());
					for (int dy = 0; dy <= 2 && (Math.abs(dx) == 4 || Math.abs(dz) == 4); dy++) {
						level.setBlockAndUpdate(anchor.offset(dx, dy, dz), Blocks.STONE.defaultBlockState());
					}
				}
			}
			ServerPlayer player = first(minecraft);
			// Night vision changes only what the client draws, not the light the server's vigil checks: the shot is not black.
			player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 600));
			CircleAnchorBlockEntity circle = (CircleAnchorBlockEntity) level.getBlockEntity(anchor);
			circle.use(player, level, true); // the attunement's offerings back
			for (StructurePattern.Part part : ConcordanceData.rules().structure("jugcraft:lesser_circle").channels()) {
				((LeyPylonBlockEntity) level.getBlockEntity(anchor.offset(part.offset().x(), part.offset().y(), part.offset().z()))).setLey(level, 64);
			}
			circle.offer(player, new ItemStack(Items.GLOW_BERRIES, 4));
		});
		server.runCommand("time set midnight");
		context.waitTicks(10);
		String phase = server.computeOnServer(minecraft -> {
			ServerLevel level = minecraft.overworld();
			ServerPlayer player = first(minecraft);
			RateGate.forget(player.getUUID());
			CircleAnchorBlockEntity circle = (CircleAnchorBlockEntity) level.getBlockEntity(anchor);
			circle.use(player, level, false);
			return circle.phase().id + " at light " + level.getMaxLocalRawBrightness(anchor.above());
		});
		Jugcraft.LOGGER.info("[encyclopedia-check] the Lumen Vigil begun in the dark: {}", phase);
		check(phase.startsWith("gathering"), "The vigil should gather: " + phase);
		if (FabricLoader.getInstance().isModLoaded("jade")) {
			String jade = server.computeOnServer(minecraft -> {
				CompoundTag shownTag = ConcordanceDataProvider.snapshot((CircleAnchorBlockEntity) minecraft.overworld().getBlockEntity(anchor), minecraft.overworld());
				boolean right = shownTag.getIntOr("needed", -1) == 2 && shownTag.getIntOr("joined", -1) == 1 && shownTag.getIntOr("spent_focus", -1) == 4
						&& shownTag.getIntOr("reserved", -1) == 4 && shownTag.getStringOr("offering0", "").equals("minecraft:glow_berries|4");
				return (right ? "ok " : "bad ") + shownTag;
			});
			Jugcraft.LOGGER.info("[encyclopedia-check] a gathering anchor in Jade: {}", jade);
			check(jade.startsWith("ok"), "Jade's snapshot of a gathering anchor: " + jade);
			context.getInput().lookAt(anchor);
			context.waitTicks(20);
			context.runOnClient(client -> client.gui.toastManager().clear());
			context.waitTicks(5);
			context.takeScreenshot("jugcraft_concordance_anchor_gathering_jade");
		} else {
			Jugcraft.LOGGER.info("[encyclopedia-check] Jade is absent: no shot of a gathering anchor");
		}
		server.runOnServer(minecraft -> {
			ServerLevel level = minecraft.overworld();
			ServerPlayer player = first(minecraft);
			CircleAnchorBlockEntity circle = (CircleAnchorBlockEntity) level.getBlockEntity(anchor);
			circle.use(player, level, true); // the leader calls it off; the berries stay
			circle.use(player, level, true); // and takes them back, so nothing drops below
			player.removeEffect(MobEffects.NIGHT_VISION);
			for (int dx = -4; dx <= 4; dx++) {
				for (int dz = -4; dz <= 4; dz++) {
					for (int dy = 0; dy <= 3; dy++) {
						level.setBlockAndUpdate(anchor.offset(dx, dy, dz), Blocks.AIR.defaultBlockState());
					}
				}
			}
		});
		server.runCommand("time set noon");
		context.getInput().lookAt(0.0F, 30.0F);
		context.waitTicks(5);
	}

	/** Concordance Chunk C, C-04: the Rooted Thallite Foundation in the hand and the hotbar, its icon seen in game (ITEM_ICONS step 7). */
	private static void rootedFoundation(ClientGameTestContext context, TestDedicatedServerContext server) {
		server.runOnServer(minecraft -> first(minecraft).setItemInHand(InteractionHand.MAIN_HAND,
				new ItemStack(JugcraftConcordance.ROOTED_THALLITE_FOUNDATION)));
		context.waitTicks(10);
		context.takeScreenshot("jugcraft_rooted_thallite_foundation");
		server.runOnServer(minecraft -> first(minecraft).setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(JugcraftConcordance.INITIATE_WAND)));
		context.waitTicks(5);
	}

	/**
	 * Concordance Chunk C, C-07 (C-P54, C-P55): a Sunpetal grown ripe in a lit Verdant Bed. The client holds its bed's last
	 * verdict, which Jade shows on the ripe crop as it grew; the Inspect page, opened looking at the bed, judges each factor
	 * in words, the light as sky and lamps. Random ticks are held still meanwhile, so only this test grows the crop. C-08
	 * (C-P59): its bed strained 2, the client is told the strain, Jade shows "recently harvested" and the page's disturbance
	 * row names the strain counted against its limit.
	 */
	private static void gardenInspection(ClientGameTestContext context, TestDedicatedServerContext server) {
		server.runCommand("gamerule minecraft:random_tick_speed 0");
		BlockPos stand = context.computeOnClient(client -> client.player.blockPosition());
		BlockPos bedAt = stand.offset(0, 0, 2);
		BlockPos plant = bedAt.above();
		BlockPos lamp = plant.east();
		server.runOnServer(minecraft -> {
			ServerLevel level = minecraft.overworld();
			for (int dx = 0; dx <= 1; dx++) {
				for (int dz = 1; dz <= 2; dz++) {
					level.setBlockAndUpdate(stand.offset(dx, 0, dz), Blocks.AIR.defaultBlockState());
					level.setBlockAndUpdate(stand.offset(dx, 1, dz), Blocks.AIR.defaultBlockState());
				}
			}
			level.setBlockAndUpdate(bedAt, Garden.VERDANT_BED.defaultBlockState().setValue(VerdantBedBlock.MOISTURE, VerdantBedBlock.WET));
			level.setBlockAndUpdate(plant, Garden.SUNPETAL_CROP.defaultBlockState());
			level.setBlockAndUpdate(lamp, Blocks.GLOWSTONE.defaultBlockState());
			if (level.getBlockEntity(bedAt) instanceof VerdantBedBlockEntity bed) {
				bed.awaken(first(minecraft).getUUID());
				bed.setNutrients(10);
			}
		});
		context.waitTicks(20); // the light engine
		String grown = server.computeOnServer(minecraft -> {
			ServerLevel level = minecraft.overworld();
			for (int step = 0; step < OrganismCropBlock.MAX_AGE; step++) {
				BlockState state = level.getBlockState(plant);
				if (!(state.getBlock() instanceof OrganismCropBlock crop) || crop.grow(level, plant, state, level.getRandom(), true) != OrganismCropBlock.Growth.GREW) {
					return "";
				}
			}
			return level.getBlockEntity(bedAt) instanceof VerdantBedBlockEntity bed ? bed.growth() : "";
		});
		check(!grown.isEmpty(), "The sunpetal should grow ripe in its lit bed, its bed noting its pace");
		context.waitTicks(10);
		String seen = context.computeOnClient(client -> client.level.getBlockEntity(bedAt) instanceof VerdantBedBlockEntity bed
				&& client.level.getBlockState(plant).getBlock() instanceof OrganismCropBlock crop && crop.isMaxAge(client.level.getBlockState(plant)) ? bed.growth() : "");
		Jugcraft.LOGGER.info("[encyclopedia-check] a ripe sunpetal: its bed noted {}, the client holds {}", grown, seen);
		check(seen.equals(grown), "The client should hold the ripe crop's last verdict, which Jade now shows: " + seen + " vs " + grown);
		server.runOnServer(minecraft -> {
			if (minecraft.overworld().getBlockEntity(bedAt) instanceof VerdantBedBlockEntity bed) {
				bed.setStrain(2); // Concordance Chunk C (C-08): so Jade and the page show "recently harvested"
				bed.setGraft("jugcraft:dewlit"); // C-09: so Jade and the page name the graft
			}
		});
		context.waitTicks(5);
		int strain = context.computeOnClient(client -> client.level.getBlockEntity(bedAt) instanceof VerdantBedBlockEntity bed ? bed.strainNow() : -1);
		check(strain == 2, "The client should be told the bed's strain (the update tag): " + strain);
		String graft = context.computeOnClient(client -> client.level.getBlockEntity(bedAt) instanceof VerdantBedBlockEntity bed ? bed.graft() + "|" + bed.graftFor() : "");
		check(graft.equals("jugcraft:dewlit|jugcraft:sunpetal"), "The client should be told the graft and its host: " + graft);
		context.getInput().lookAt(plant);
		context.waitTicks(20);
		if (FabricLoader.getInstance().isModLoaded("jade")) {
			context.runOnClient(client -> client.gui.toastManager().clear());
			context.waitTicks(5);
			context.takeScreenshot("jugcraft_concordance_ripe_crop_jade");
		} else {
			Jugcraft.LOGGER.info("[encyclopedia-check] Jade is not loaded: no shot of a ripe crop's lines");
		}
		context.getInput().lookAt(bedAt);
		context.waitTicks(5);
		open(context);
		String page = context.computeOnClient(client -> ((EncyclopediaScreen) client.gui.screen()).page());
		List<String> rows = context.computeOnClient(client -> {
			EncyclopediaPayload.Page current = EncyclopediaClient.navigator().current();
			return current == null ? List.<String>of() : current.rows().stream().map(row -> row.text().getString()).toList();
		});
		boolean judged = context.computeOnClient(client -> {
			EncyclopediaPayload.Page current = EncyclopediaClient.navigator().current();
			List<EncyclopediaPayload.Row> found = current == null ? List.of() : current.rows();
			return found.stream().anyMatch(row -> holds(row.text(), "compose.jugcraft.ecology.inspect.light"))
					&& found.stream().anyMatch(row -> holds(row.text(), "compose.jugcraft.ecology.inspect.diversity"))
					&& found.stream().anyMatch(row -> holds(row.text(), "compose.jugcraft.ecology.inspect.verdict"))
					&& found.stream().anyMatch(row -> holds(row.text(), "compose.jugcraft.ecology.inspect.disturbance_strained"))
					&& found.stream().anyMatch(row -> holds(row.text(), "compose.jugcraft.ecology.graft"));
		});
		Jugcraft.LOGGER.info("[encyclopedia-check] looking at a bed: page {}, rows {}", page, rows);
		check(page.equals("inspect") && judged && rows.stream().noneMatch(row -> row.contains("jugcraft.") || row.contains("jugcraft:")),
				"Looking at a bed, the Inspect page should judge each factor in words, the light as sky and lamps, and name its strain and graft: " + rows);
		context.runOnClient(client -> client.gui.toastManager().clear());
		context.waitTicks(5);
		context.takeScreenshot("jugcraft_encyclopedia_inspect_bed");
		context.getInput().pressKey(InputConstants.KEY_ESCAPE);
		context.waitForScreen(null);
		context.getInput().lookAt(0.0F, 30.0F);
		server.runOnServer(minecraft -> {
			ServerLevel level = minecraft.overworld();
			level.setBlockAndUpdate(plant, Blocks.AIR.defaultBlockState());
			level.setBlockAndUpdate(lamp, Blocks.AIR.defaultBlockState());
			level.setBlockAndUpdate(bedAt, Blocks.AIR.defaultBlockState());
		});
		server.runCommand("gamerule minecraft:random_tick_speed 3");
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
			((EncyclopediaScreen) client.gui.screen()).showPage("expedition");
			EncyclopediaClient.changed();
		});
		context.waitTicks(5);
		context.runOnClient(client -> client.gui.toastManager().clear());
                context.waitTicks(2);
                context.takeScreenshot("jugcraft_encyclopedia_expedition");

		// Concordance Chunk B (B-11): the Glacier Hall's and the Cinder Kiln's dossiers (their tokens carried, their keepers
		// beaten), each opened as its own section of the book (each dossier starts with a heading named after its lair)
		// and scrolled to the top of the page's body.
		server.runOnServer(minecraft -> {
			ServerPlayer player = first(minecraft);
			player.getInventory().add(new ItemStack(JugcraftLairs.FROST_HORN));
			player.getInventory().add(new ItemStack(JugcraftLairs.KILN_SEAL));
			TrickOrTreat.award(player, YetiKingLoot.ADVANCEMENT);
			TrickOrTreat.award(player, CinderTyrantLoot.ADVANCEMENT);
		});
		context.waitTicks(10);
		context.getInput().pressKey(InputConstants.KEY_ESCAPE);
		context.waitForScreen(null);
		open(context);
		context.runOnClient(client -> {
			((EncyclopediaScreen) client.gui.screen()).showPage("expedition");
			EncyclopediaClient.changed();
		});
		context.waitTicks(5);
		for (Lair lair : List.of(Lair.GLACIER_HALL, Lair.CINDER_KILN)) {
			context.runOnClient(client -> ((EncyclopediaScreen) client.gui.screen()).showSectionContaining(LairDossiers.name(lair)));
			context.waitTicks(3);
			String placed = context.computeOnClient(client -> scrollTo(client, LairDossiers.name(lair)));
			Jugcraft.LOGGER.info("[encyclopedia-check] the {} dossier on the Expeditions page: {}", lair.id, placed);
			check(placed.startsWith("ok"), "The Expeditions page does not show the " + lair.id + " dossier: " + placed);
			context.runOnClient(client -> client.gui.toastManager().clear());   // the two advancements' toasts
			context.waitTicks(5);
			context.takeScreenshot("jugcraft_encyclopedia_expedition_" + lair.id);
		}

		context.runOnClient(client -> {
			((EncyclopediaScreen) client.gui.screen()).showPage("keys");
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
		context.runOnClient(client -> client.gui.toastManager().clear());
                context.waitTicks(2);
                context.takeScreenshot("jugcraft_encyclopedia_keys");
		context.getInput().pressKey(InputConstants.KEY_ESCAPE);
		context.waitForScreen(null);
	}

	/** Scrolls the open encyclopedia's body so the row showing {@code heading} is at its top: "ok" and where it stands, or what is missing. */
	private static String scrollTo(Minecraft client, Component heading) {
		if (!(client.gui.screen() instanceof EncyclopediaScreen screen)) {
			return "no encyclopedia open";
		}
		AbstractWidget row = screen.find(heading);
		if (row == null) {
			return "no row \"" + heading.getString() + "\"";
		}
		AbstractContainerWidget body = screen.children().stream().filter(AbstractContainerWidget.class::isInstance)
				.map(AbstractContainerWidget.class::cast).filter(area -> area.children().contains(row)).findFirst().orElse(null);
		if (body == null) {
			return "no scrolling body holds \"" + heading.getString() + "\"";
		}
		body.setScrollAmount(body.scrollAmount() + row.getY() - body.getY());
		boolean shown = row.getY() >= body.getY() && row.getY() < body.getY() + body.getHeight();
		return (shown ? "ok" : "hidden") + " at " + (row.getY() - body.getY()) + " of " + body.getHeight() + ", scrolled " + body.scrollAmount();
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
		context.runOnClient(client -> client.gui.toastManager().clear());
                context.waitTicks(2);
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
		context.runOnClient(client -> client.gui.toastManager().clear());
                context.waitTicks(2);
                context.takeScreenshot("jugcraft_encyclopedia_scale4");
		context.runOnClient(client -> {
			client.options.guiScale().set(scale);
			client.resizeGui();
		});
		// Last, a small window (the runner restores the window's size after each test).
		context.getInput().resizeWindow(640, 360);
		context.waitTicks(10);
		context.runOnClient(client -> client.gui.toastManager().clear());
                context.waitTicks(2);
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

	/** Whether the translation key {@code key} is in {@code component}, its arguments or its siblings. */
	private static boolean holds(Component component, String key) {
		if (component.getContents() instanceof TranslatableContents translatable) {
			if (translatable.getKey().equals(key)) {
				return true;
			}
			for (Object arg : translatable.getArgs()) {
				if (arg instanceof Component inner && holds(inner, key)) {
					return true;
				}
			}
		}
		return component.getSiblings().stream().anyMatch(sibling -> holds(sibling, key));
	}

	private static void check(boolean condition, String message) {
		if (!condition) {
			throw new AssertionError(message);
		}
	}
}
