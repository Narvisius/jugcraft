package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.gear.TraitTooltips;
import java.util.List;
import java.util.function.BooleanSupplier;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/**
 * Client game test for the trait details on Shift (docs/features/trait-details.md): a runebound nodachi (five traits)
 * in the inventory, hovered with the real cursor. Shot folded (the names and "Hold Shift for details") and expanded
 * (each name with its description), and the client's English tooltip lines logged both ways (CI job {@code client}).
 * The expanded shot sets the Shift test to true rather than holding the key, so it does not depend on how the test
 * input reports a held modifier.
 */
public class TraitDetailsClientGameTests implements FabricClientGameTest {
	private static final String ITEM = "jugcraft:runebound_nodachi";

	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestSingleplayerContext singleplayer = context.worldBuilder()
				.adjustSettings(creator -> creator.setGameMode(WorldCreationUiState.SelectedGameMode.SURVIVAL)).create()) {
			singleplayer.getConnection().waitForChunksRender();
			TestServerContext server = singleplayer.getServer();
			server.runCommand("time set noon");
			server.runCommand("weather clear");
			server.runCommand("gamerule minecraft:send_command_feedback false");
			server.runCommand("item replace entity @p hotbar.0 with " + ITEM);
			context.waitTicks(10);

			// The tooltip's lines in the client's own English, folded and expanded.
			List<String> folded = context.computeOnClient(client -> lines(client, false));
			List<String> expanded = context.computeOnClient(client -> lines(client, true));
			Jugcraft.LOGGER.info("[trait details client] folded tooltip:\n{}", String.join("\n", folded));
			Jugcraft.LOGGER.info("[trait details client] expanded tooltip:\n{}", String.join("\n", expanded));
			String hint = context.computeOnClient(client -> Component.translatable("tooltip.jugcraft.hold_shift", "Shift").getString());
			check(folded.contains(hint) && !expanded.contains(hint), "The hold-Shift line is not shown folded only");
			check(expanded.size() > folded.size(), "The expanded tooltip is no longer than the folded one");
			check(folded.stream().anyMatch(line -> line.equals("Weapon Art: Iaido")), "The folded tooltip does not name the weapon art");

			// The inventory, the cursor over the nodachi in the first hotbar slot.
			context.setScreen(() -> new InventoryScreen(Minecraft.getInstance().player));
			context.waitTicks(5);
			double[] at = context.computeOnClient(client -> {
				Screen screen = client.gui.screen();
				// The survival inventory is 176 by 166 GUI units, centred; its first hotbar slot's corner is at (8, 142).
				double left = (screen.width - 176) / 2;
				double top = (screen.height - 166) / 2;
				double guiPerPixelX = MouseHandler.getScaledXPos(client.getWindow(), 1.0);
				double guiPerPixelY = MouseHandler.getScaledYPos(client.getWindow(), 1.0);
				return new double[] {(left + 8 + 8) / guiPerPixelX, (top + 142 + 8) / guiPerPixelY};
			});
			context.getInput().setCursorPos(at[0], at[1]);
			context.waitTicks(5);
			context.takeScreenshot("jugcraft_trait_details_folded");

			BooleanSupplier shift = context.computeOnClient(client -> TraitTooltips.details);
			try {
				context.runOnClient(client -> TraitTooltips.details = () -> true);
				context.waitTicks(3);
				context.takeScreenshot("jugcraft_trait_details_expanded");
			} finally {
				context.runOnClient(client -> TraitTooltips.details = shift);
			}
			context.setScreen(() -> null);
		}
	}

	/** The hotbar's first stack's tooltip lines, with descriptions shown or not, as the client renders them. */
	private static List<String> lines(Minecraft client, boolean expanded) {
		BooleanSupplier shift = TraitTooltips.details;
		try {
			TraitTooltips.details = () -> expanded;
			ItemStack stack = client.player.getInventory().getItem(0);
			return stack.getTooltipLines(Item.TooltipContext.of(client.level), client.player, TooltipFlag.NORMAL)
					.stream().map(Component::getString).toList();
		} finally {
			TraitTooltips.details = shift;
		}
	}

	private static void check(boolean ok, String message) {
		if (!ok) {
			throw new AssertionError(message);
		}
	}
}
