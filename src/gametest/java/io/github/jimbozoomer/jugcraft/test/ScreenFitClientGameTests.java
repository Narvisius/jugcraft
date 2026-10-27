package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.agriculture.CarvingFace;
import io.github.jimbozoomer.jugcraft.aircraft.AircraftMenu;
import io.github.jimbozoomer.jugcraft.aircraft.AircraftSpec;
import io.github.jimbozoomer.jugcraft.agriculture.CarvingTemplates;
import io.github.jimbozoomer.jugcraft.agriculture.CookingPotMenu;
import io.github.jimbozoomer.jugcraft.agriculture.FarmStandMenu;
import io.github.jimbozoomer.jugcraft.agriculture.OpenCarvingPayload;
import io.github.jimbozoomer.jugcraft.agriculture.PumpkinCarving;
import io.github.jimbozoomer.jugcraft.chemistry.FluidRecipes;
import io.github.jimbozoomer.jugcraft.client.AircraftScreen;
import io.github.jimbozoomer.jugcraft.client.AtmScreen;
import io.github.jimbozoomer.jugcraft.client.ArtisanScreen;
import io.github.jimbozoomer.jugcraft.client.CarvingScreen;
import io.github.jimbozoomer.jugcraft.client.CivilWorkshopScreen;
import io.github.jimbozoomer.jugcraft.client.CompactReactorScreen;
import io.github.jimbozoomer.jugcraft.client.CaveWorkshopScreen;
import io.github.jimbozoomer.jugcraft.client.CookingPotScreen;
import io.github.jimbozoomer.jugcraft.client.DroneTerminalScreen;
import io.github.jimbozoomer.jugcraft.client.ExchangeGuideScreen;
import io.github.jimbozoomer.jugcraft.client.FarmStandScreen;
import io.github.jimbozoomer.jugcraft.client.FormMachineScreen;
import io.github.jimbozoomer.jugcraft.client.IndustrialHeatScreen;
import io.github.jimbozoomer.jugcraft.client.LampwrightBenchScreen;
import io.github.jimbozoomer.jugcraft.client.LogicControllerScreen;
import io.github.jimbozoomer.jugcraft.client.MachineRecipeScreen;
import io.github.jimbozoomer.jugcraft.client.MachineScreen;
import io.github.jimbozoomer.jugcraft.client.MarketChestScreen;
import io.github.jimbozoomer.jugcraft.client.MatterWorkshopScreen;
import io.github.jimbozoomer.jugcraft.client.ProspectorScreen;
import io.github.jimbozoomer.jugcraft.client.ReactorConsoleScreen;
import io.github.jimbozoomer.jugcraft.client.RemoteShutdownScreen;
import io.github.jimbozoomer.jugcraft.client.RocketPadScreen;
import io.github.jimbozoomer.jugcraft.client.RosterScreen;
import io.github.jimbozoomer.jugcraft.client.ScreenKit;
import io.github.jimbozoomer.jugcraft.client.ShopScreen;
import io.github.jimbozoomer.jugcraft.client.StorageTankScreen;
import io.github.jimbozoomer.jugcraft.client.TowerScreen;
import io.github.jimbozoomer.jugcraft.client.jimtafacts.ComputerScreen;
import io.github.jimbozoomer.jugcraft.client.jimtafacts.GateParametersScreen;
import io.github.jimbozoomer.jugcraft.client.jimtafacts.IndustrialFluidServiceScreen;
import io.github.jimbozoomer.jugcraft.client.jimtafacts.IndustrialLatheScreen;
import io.github.jimbozoomer.jugcraft.client.jimtafacts.IndustrialTransformerScreen;
import io.github.jimbozoomer.jugcraft.client.jimtafacts.JimtaScribingScreen;
import io.github.jimbozoomer.jugcraft.client.jimtafacts.PersonalWorkshopScreen;
import io.github.jimbozoomer.jugcraft.client.jimtafacts.WorkshopChuteScreen;
import io.github.jimbozoomer.jugcraft.client.jimtafacts.WorkshopEngineScreen;
import io.github.jimbozoomer.jugcraft.client.jimtafacts.WorkshopTableScreen;
import io.github.jimbozoomer.jugcraft.concordance.LampwrightBenchMenu;
import io.github.jimbozoomer.jugcraft.control.LogicControllerMenu;
import io.github.jimbozoomer.jugcraft.finance.MarketChestMenu;
import io.github.jimbozoomer.jugcraft.jimtafacts.computers.ComputerTerminal;
import io.github.jimbozoomer.jugcraft.jimtafacts.industry.CivilWorkshops;
import io.github.jimbozoomer.jugcraft.jimtafacts.industry.CompactReactor;
import io.github.jimbozoomer.jugcraft.jimtafacts.industry.IndustrialFluidServices;
import io.github.jimbozoomer.jugcraft.jimtafacts.industry.IndustrialGateParameters;
import io.github.jimbozoomer.jugcraft.jimtafacts.industry.IndustrialHeat;
import io.github.jimbozoomer.jugcraft.jimtafacts.industry.IndustrialLathe;
import io.github.jimbozoomer.jugcraft.jimtafacts.industry.IndustrialTransformers;
import io.github.jimbozoomer.jugcraft.jimtafacts.industry.MatterWorkshops;
import io.github.jimbozoomer.jugcraft.jimtafacts.industry.PersonalWorkshops;
import io.github.jimbozoomer.jugcraft.jimtafacts.industry.WorkshopChute;
import io.github.jimbozoomer.jugcraft.jimtafacts.industry.WorkshopEngines;
import io.github.jimbozoomer.jugcraft.jimtafacts.industry.WorkshopTables;
import io.github.jimbozoomer.jugcraft.jimtafacts.spellbooks.JimtaScribing;
import io.github.jimbozoomer.jugcraft.machine.JugcraftMachines;
import io.github.jimbozoomer.jugcraft.machine.MachineKind;
import io.github.jimbozoomer.jugcraft.machine.MachineMenu;
import io.github.jimbozoomer.jugcraft.machine.form.FormMachineMenu;
import io.github.jimbozoomer.jugcraft.machine.form.JugcraftForms;
import io.github.jimbozoomer.jugcraft.machine.form.MachineForm;
import io.github.jimbozoomer.jugcraft.menagerie.CaveWorkshop;
import io.github.jimbozoomer.jugcraft.prospecting.OreSurvey;
import io.github.jimbozoomer.jugcraft.rocketry.RocketPadMenu;
import io.github.jimbozoomer.jugcraft.town.AtmMenu;
import io.github.jimbozoomer.jugcraft.town.ShopMenu;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/**
 * Every machine screen fits the smallest GUI Minecraft's automatic scale gives: a 640 x 480 window at scale 2, which is
 * 320 x 240 GUI pixels (docs/features/screen-fit-check.md). With the window at that size it opens every machine kind's
 * screen, every form's, a form's recipe page and the other machine screens, on stand-in menus (no block or server round
 * trip: only their size and layout are judged here; other tests show them working), the workshop benches and the Concordance's
 * roster, checks that each one's panel ({@link ScreenKit.Fitted}) lies inside the window, and photographs a few at that
 * size. Then it puts the window back.
 * trip: only their size and layout are judged here; other tests show them working) and the workshop benches, checks that each one's panel
 * ({@link ScreenKit.Fitted}) lies inside the window, and photographs a few at that size. The workshop, aircraft and
 * furniture screens (docs/features/new-screen-styles.md) are checked too: the robot programming table reads the block it
 * stands on to choose its layout, so that block is placed above the player first (and so do the personal shops, in the
 * trade look: docs/features/trade-look.md). Then it puts the window back.
 */
public class ScreenFitClientGameTests implements FabricClientGameTest {
	/** The window Fabric's client game tests run in, put back afterwards. */
	private static final int TEST_WINDOW_WIDTH = 854;
	private static final int TEST_WINDOW_HEIGHT = 480;

	private record Catalogue(MachineForm form, List<FluidRecipes.Entry> entries) {
	}

	@Override
	public void runTest(ClientGameTestContext context) {
		if (GuideScreenshotGameTests.active()) {
			return;
		}
		try (TestSingleplayerContext singleplayer = context.worldBuilder().create()) {
			singleplayer.getConnection().waitForChunksRender();
			// The recipe page's catalogue: the processing form with the most recipes, chosen as a real form's menu would be.
			Catalogue catalogue = singleplayer.getServer().computeOnServer(server -> {
				Catalogue most = null;
				for (MachineForm form : JugcraftForms.forms()) {
					if (form.generator()) {
						continue;
					}
					List<FluidRecipes.Entry> runs = FluidRecipes.entries(server, form.family()).stream()
							.filter(entry -> form.runs(entry.recipe()) && (entry.recipe().tool().isEmpty() || !form.sockets().isEmpty()))
							.toList();
					if (most == null || runs.size() > most.entries().size()) {
						most = new Catalogue(form, runs);
					}
				}
				return most;
			});
			if (catalogue == null || catalogue.entries().isEmpty()) {
				throw new AssertionError("No processing form has any recipes to show on the recipe page");
			}
			// The programming table's menu chooses its layout from the block it stands on.
			BlockPos programming = context.computeOnClient(client -> client.player.blockPosition().above(4));
			singleplayer.getServer().runOnServer(server -> server.overworld().setBlockAndUpdate(programming,
					WorkshopTables.BLOCKS.get("programming").defaultBlockState()));
			// So do the personal shops' (docs/features/trade-look.md).
			BlockPos barter = programming.east(), vendor = programming.west();
			singleplayer.getServer().runOnServer(server -> {
				server.overworld().setBlockAndUpdate(barter, PersonalWorkshops.BLOCKS.get("barter_station").defaultBlockState());
				server.overworld().setBlockAndUpdate(vendor, PersonalWorkshops.BLOCKS.get("energy_vendor").defaultBlockState());
			});
			context.waitTicks(5);

			int scale = context.computeOnClient(client -> client.options.guiScale().get());
			context.getInput().resizeWindow(ScreenKit.SMALLEST_GUI_WIDTH * 2, ScreenKit.SMALLEST_GUI_HEIGHT * 2);
			context.runOnClient(client -> {
				client.options.guiScale().set(0);
				client.resizeGui();
			});
			context.waitTicks(2);
			List<String> faults = new ArrayList<>();
			try {
				String gui = context.computeOnClient(client -> client.getWindow().getGuiScaledWidth() + "x" + client.getWindow().getGuiScaledHeight());
				if (!gui.equals(ScreenKit.SMALLEST_GUI_WIDTH + "x" + ScreenKit.SMALLEST_GUI_HEIGHT)) {
					throw new AssertionError("The 640 x 480 window gave a " + gui + " GUI, not 320 x 240");
				}

				MachineKind[] kinds = MachineKind.values();
				for (MachineKind kind : kinds) {
					check(context, faults, "machine " + kind.id, kind == kinds[0] ? "machine_" + kind.id : null,
							inventory -> new MachineScreen(new MachineMenu(JugcraftMachines.menuType(kind), kind, 0, inventory), inventory,
									Component.translatable("block.jugcraft." + kind.id)));
				}
				for (MachineForm form : JugcraftForms.forms()) {
					check(context, faults, "form " + form.id(), form == catalogue.form() ? "form_" + form.id().getPath() : null,
							inventory -> new FormMachineScreen(new FormMachineMenu(0, inventory, form), inventory, title(form)));
				}
				// The page closes itself unless its menu is the open one, as it would be with the machine open.
				check(context, faults, "recipe page of " + catalogue.form().id(), "recipe_page", inventory -> {
					FormMachineMenu menu = new FormMachineMenu(0, inventory, catalogue.form(), catalogue.entries());
					inventory.player.containerMenu = menu;
					return new MachineRecipeScreen(new FormMachineScreen(menu, inventory, title(catalogue.form())), menu);
				});
				context.runOnClient(client -> client.player.containerMenu = client.player.inventoryMenu);

				check(context, faults, "prospector", "prospector", inventory -> new ProspectorScreen(readings()));
				check(context, faults, "rocket pad", "rocket_pad",
						inventory -> new RocketPadScreen(new RocketPadMenu(0, inventory), inventory, Component.translatable("block.jugcraft.rocket_pad")));
				check(context, faults, "logic controller", "logic_controller", inventory -> new LogicControllerScreen(
						new LogicControllerMenu(0, inventory, BlockPos.ZERO), inventory, Component.translatable("block.jugcraft.logic_controller")));
				check(context, faults, "storage tank", "storage_tank", inventory -> new StorageTankScreen(BlockPos.ZERO));
				check(context, faults, "reactor console", "reactor_console", inventory -> new ReactorConsoleScreen(BlockPos.ZERO));
				check(context, faults, "remote shutdown panel", "remote_shutdown", inventory -> new RemoteShutdownScreen(BlockPos.ZERO));
				check(context, faults, "drone depot terminal", "drone_terminal", inventory -> new DroneTerminalScreen(BlockPos.ZERO));
				check(context, faults, "drone tower", "drone_tower", inventory -> new TowerScreen(BlockPos.ZERO));
				// The blueprint table: its drafting board scales down to fit (docs/features/blueprint-table-fit.md).
				check(context, faults, "blueprint table", "blueprint_table",
						inventory -> new io.github.jimbozoomer.jugcraft.client.blueprint.BlueprintTableScreen(BlockPos.ZERO));

				// The benches in the workshop look (docs/features/bench-screens.md).
				check(context, faults, "cooking pot", "cooking_pot", inventory -> new CookingPotScreen(new CookingPotMenu(0, inventory), inventory,
						Component.translatable("container.jugcraft.cooking_pot")));
				check(context, faults, "lampwright's bench", "lampwright_bench", inventory -> new LampwrightBenchScreen(
						new LampwrightBenchMenu(0, inventory), inventory, Component.translatable("block.jugcraft.lampwright_bench")));
				check(context, faults, "farm stand", "farm_stand", inventory -> new FarmStandScreen(new FarmStandMenu(0, inventory,
						new FarmStandMenu.Opening(BlockPos.ZERO, "Steve", true)), inventory, Component.translatable("block.jugcraft.farm_stand")));
				check(context, faults, "cave survey desk", "cave_survey", inventory -> new CaveWorkshopScreen(new CaveWorkshop(0, inventory), inventory,
						Component.empty()));
				check(context, faults, "carving knife", "carving", inventory -> new CarvingScreen(new OpenCarvingPayload(BlockPos.ZERO, Direction.NORTH,
						new CarvingFace.Sized(PumpkinCarving.SIZE, CarvingTemplates.ALL.getFirst().face().clone()), true, Optional.empty())));
				// The Concordance's roster (Concordance Chunk D, D-07), before its first state: only its size and layout.
				check(context, faults, "Concordance roster", "concordance_roster", inventory -> new RosterScreen());

				// The workshop, aircraft and furniture screens (docs/features/new-screen-styles.md).
				check(context, faults, "lathe", "lathe", inventory -> new IndustrialLatheScreen(new IndustrialLathe.Menu(0, inventory, BlockPos.ZERO),
						inventory, Component.literal("Lathe")));
				check(context, faults, "transformer", null, inventory -> new IndustrialTransformerScreen(
						new IndustrialTransformers.Menu(0, inventory, BlockPos.ZERO), inventory, Component.literal("Transformer")));
				check(context, faults, "fluid service", null, inventory -> new IndustrialFluidServiceScreen(
						new IndustrialFluidServices.Menu(0, inventory, BlockPos.ZERO), inventory, Component.literal("Fluid service")));
				check(context, faults, "workshop engine", "workshop_engine", inventory -> new WorkshopEngineScreen(
						new WorkshopEngines.Menu(0, inventory, BlockPos.ZERO), inventory, Component.literal("Stirling engine")));
				check(context, faults, "workshop chute", null, inventory -> new WorkshopChuteScreen(new WorkshopChute.Menu(0, inventory, BlockPos.ZERO),
						inventory, Component.literal("Chute")));
				check(context, faults, "equipment charger", null, inventory -> new WorkshopTableScreen(
						new WorkshopTables.Menu(0, inventory, BlockPos.ZERO), inventory, Component.literal("Equipment charger")));
				check(context, faults, "robot programming table", "programming_table", inventory -> new WorkshopTableScreen(
						new WorkshopTables.Menu(0, inventory, programming), inventory, Component.literal("Programming table")));
				check(context, faults, "compact reactor", "compact_reactor", inventory -> new CompactReactorScreen(
						new CompactReactor.Menu(0, inventory, BlockPos.ZERO), inventory, Component.literal("Compact reactor")));
				check(context, faults, "computer", "computer", inventory -> new ComputerScreen(new ComputerTerminal.Menu(0, inventory), inventory,
						Component.literal("Computer")));
				check(context, faults, "gate parameters", "gate_parameters", inventory -> new GateParametersScreen(
						new IndustrialGateParameters.Menu(0, inventory), inventory, Component.literal("Gate parameters")));
				check(context, faults, "heat and kinetic generators", null, inventory -> new IndustrialHeatScreen(
						new IndustrialHeat.Menu(0, inventory, BlockPos.ZERO), inventory, Component.literal("Heat generator")));
				check(context, faults, "civil workshop", null, inventory -> new CivilWorkshopScreen(new CivilWorkshops.Menu(0, inventory, BlockPos.ZERO),
						inventory, Component.literal("Civil workshop")));
				check(context, faults, "matter workshop", null, inventory -> new MatterWorkshopScreen(
						new MatterWorkshops.Menu(0, inventory, BlockPos.ZERO), inventory, Component.literal("Matter workshop")));
				check(context, faults, "scribing desk", null, inventory -> new JimtaScribingScreen(new JimtaScribing.Menu(0, inventory), inventory,
						Component.literal("Scribing desk")));
				AircraftSpec.load();
				for (AircraftSpec spec : AircraftSpec.ALL.values()) {
					check(context, faults, "aircraft " + spec.name(), spec.name().equals("cargo_airship") ? "aircraft_" + spec.name() : null,
							inventory -> new AircraftScreen(new AircraftMenu(0, inventory, spec), inventory, Component.literal(spec.name())));
				}
				check(context, faults, "artisan's workstation", "artisan", inventory -> new ArtisanScreen(BlockPos.ZERO));

				// The trade look (docs/features/trade-look.md).
				check(context, faults, "town shop", "shop", inventory -> new ShopScreen(new ShopMenu(0, inventory, new ShopMenu.Opening("general", "", 0)),
						inventory, Component.literal("General Store")));
				check(context, faults, "Jug Teller", "atm", inventory -> new AtmScreen(new AtmMenu(0, inventory, new AtmMenu.Opening(BlockPos.ZERO,
						List.of("Alex", "Steve", "Ari", "Efe", "Kai", "Makena", "Noor", "Sunny"))), inventory, Component.literal("Jug Teller")));
				check(context, faults, "market chest", "market_chest", inventory -> new MarketChestScreen(new MarketChestMenu(0, inventory, BlockPos.ZERO),
						inventory, Component.literal("Market Chest")));
				check(context, faults, "Exchange guide", null, inventory -> new ExchangeGuideScreen(null));
				check(context, faults, "personal safe", null, inventory -> new PersonalWorkshopScreen(
						new PersonalWorkshops.Menu(0, inventory, BlockPos.ZERO), inventory, Component.literal("Personal Safe")));
				check(context, faults, "barter station", "barter_station", inventory -> new PersonalWorkshopScreen(
						new PersonalWorkshops.Menu(0, inventory, barter), inventory, Component.literal("Barter Station")));
				check(context, faults, "energy vendor", null, inventory -> new PersonalWorkshopScreen(
						new PersonalWorkshops.Menu(0, inventory, vendor), inventory, Component.literal("Energy Vendor")));
			} finally {
				context.runOnClient(client -> {
					client.player.containerMenu = client.player.inventoryMenu;
					client.gui.setScreen(null);
					client.options.guiScale().set(scale);
				});
				context.getInput().resizeWindow(TEST_WINDOW_WIDTH, TEST_WINDOW_HEIGHT);
				context.runOnClient(Minecraft::resizeGui);
				context.waitTicks(2);
			}
			if (!faults.isEmpty()) {
				throw new AssertionError(faults.size() + " screens do not fit a 320 x 240 GUI:\n" + String.join("\n", faults));
			}
		}
	}

	/** Opens the screen, notes a fault if its panel leaves the window, and photographs it when {@code shot} is given. */
	private static void check(ClientGameTestContext context, List<String> faults, String name, String shot, Function<Inventory, Screen> screen) {
		context.runOnClient(client -> client.gui.setScreen(screen.apply(client.player.getInventory())));
		String fault = context.computeOnClient(client -> {
			Screen open = client.gui.screen();
			if (!(open instanceof ScreenKit.Fitted fitted)) {
				return name + ": " + (open == null ? "the screen closed itself" : open.getClass().getSimpleName() + " does not report its panel");
			}
			ScreenRectangle area = fitted.panelArea();
			if (area.left() < 0 || area.top() < 0 || area.right() > open.width || area.bottom() > open.height) {
				return name + ": its " + area.width() + " x " + area.height() + " panel at " + area.left() + ", " + area.top()
						+ " leaves the " + open.width + " x " + open.height + " GUI";
			}
			return "";
		});
		if (!fault.isEmpty()) {
			faults.add(fault);
		}
		if (shot != null) {
			context.waitTicks(3);
			context.takeScreenshot("jugcraft_fit_" + shot);
		}
	}

	private static Component title(MachineForm form) {
		return Component.translatable("block." + form.id().getNamespace() + "." + form.id().getPath());
	}

	/** Fourteen readings: two more than the glass shows, so the "fainter" line shows too. */
	private static List<OreSurvey.Reading> readings() {
		String[] ores = {"iron_ore", "copper_ore", "coal_ore", "gold_ore", "redstone_ore", "lapis_ore", "diamond_ore", "emerald_ore",
				"deepslate_iron_ore", "deepslate_copper_ore", "deepslate_gold_ore", "nether_quartz_ore", "nether_gold_ore", "ancient_debris"};
		List<OreSurvey.Reading> readings = new ArrayList<>();
		for (int i = 0; i < ores.length; i++) {
			readings.add(new OreSurvey.Reading(Identifier.withDefaultNamespace(ores[i]), Math.max(1, 5 - i / 3), i % 3));
		}
		return readings;
	}
}
