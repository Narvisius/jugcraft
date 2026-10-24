package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.chemistry.FluidRecipes;
import io.github.jimbozoomer.jugcraft.client.DroneTerminalScreen;
import io.github.jimbozoomer.jugcraft.client.FormMachineScreen;
import io.github.jimbozoomer.jugcraft.client.LogicControllerScreen;
import io.github.jimbozoomer.jugcraft.client.MachineRecipeScreen;
import io.github.jimbozoomer.jugcraft.client.MachineScreen;
import io.github.jimbozoomer.jugcraft.client.ProspectorScreen;
import io.github.jimbozoomer.jugcraft.client.ReactorConsoleScreen;
import io.github.jimbozoomer.jugcraft.client.RemoteShutdownScreen;
import io.github.jimbozoomer.jugcraft.client.RocketPadScreen;
import io.github.jimbozoomer.jugcraft.client.ScreenKit;
import io.github.jimbozoomer.jugcraft.client.StorageTankScreen;
import io.github.jimbozoomer.jugcraft.client.TowerScreen;
import io.github.jimbozoomer.jugcraft.control.LogicControllerMenu;
import io.github.jimbozoomer.jugcraft.machine.JugcraftMachines;
import io.github.jimbozoomer.jugcraft.machine.MachineKind;
import io.github.jimbozoomer.jugcraft.machine.MachineMenu;
import io.github.jimbozoomer.jugcraft.machine.form.FormMachineMenu;
import io.github.jimbozoomer.jugcraft.machine.form.JugcraftForms;
import io.github.jimbozoomer.jugcraft.machine.form.MachineForm;
import io.github.jimbozoomer.jugcraft.prospecting.OreSurvey;
import io.github.jimbozoomer.jugcraft.rocketry.RocketPadMenu;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/**
 * Every machine screen fits the smallest GUI Minecraft's automatic scale gives: a 640 x 480 window at scale 2, which is
 * 320 x 240 GUI pixels (docs/features/screen-fit-check.md). With the window at that size it opens every machine kind's
 * screen, every form's, a form's recipe page and the other machine screens, on stand-in menus (no block or server round
 * trip: only their size and layout are judged here; other tests show them working), checks that each one's panel
 * ({@link ScreenKit.Fitted}) lies inside the window, and photographs a few at that size. Then it puts the window back.
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
